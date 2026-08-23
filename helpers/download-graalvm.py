#!/usr/bin/env python3
"""
GraalVM SDK Download & Environment Setup Helper for Omniwrench.

Detects current system architecture, queries GitHub API for the latest GraalVM Community Edition
JDK release, downloads the archive with SHA-256 integrity verification, extracts it into the target directory,
and configures JAVA_HOME, JDK_HOME, and GRAALVM_HOME properly.

Traceability:
- Requirement: REQ-00025 (Dual Distribution Packaging: GraalVM Native + Fat JAR)
- Feature: FR-00009 (Configurable Runtime Profiles & Environment Setup)
- Standard: CS-0030 (Java & GraalVM Native Compatibility), CS-0050 (Verifiable Integrity & Safety)
- Task: TSK-20260822-014 (Latest GraalVM Integration & Environment Tooling)
"""

import argparse
import hashlib
import json
import os
import platform
import shutil
import sys
import tarfile
import urllib.request
import zipfile
from pathlib import Path

GITHUB_API_RELEASES_URL = "https://api.github.com/repos/graalvm/graalvm-ce-builds/releases"
DEFAULT_TARGET_DIR = Path.home() / ".graalvm"
BUFFER_SIZE = 65536


def detect_architecture() -> tuple[str, str]:
    """
    Detects OS and CPU architecture mapped to GraalVM release asset nomenclature.
    
    Returns:
        tuple[str, str]: (os_name, arch_name) e.g. ('linux', 'x64') or ('darwin', 'aarch64')
    """
    raw_os = platform.system().lower()
    raw_arch = platform.machine().lower()

    if raw_os.startswith("linux"):
        os_name = "linux"
    elif raw_os.startswith("darwin"):
        os_name = "macos"
    elif raw_os.startswith("windows") or raw_os.startswith("cygwin") or raw_os.startswith("msys"):
        os_name = "windows"
    else:
        raise RuntimeError(f"Unsupported operating system: {raw_os}")

    if raw_arch in ("x86_64", "amd64", "x64"):
        arch_name = "x64"
    elif raw_arch in ("aarch64", "arm64"):
        arch_name = "aarch64"
    else:
        raise RuntimeError(f"Unsupported CPU architecture: {raw_arch}")

    return os_name, arch_name


def fetch_latest_release_info(os_name: str, arch_name: str, preferred_flavor: str = "tar.gz") -> dict:
    """
    Queries GitHub API to find the latest GraalVM CE release and matching asset URLs.
    
    Args:
        os_name: Detected OS ('linux', 'macos', 'windows')
        arch_name: Detected architecture ('x64', 'aarch64')
        preferred_flavor: 'zip' or 'tar.gz' (auto-fallback if not present)
    """
    headers = {"User-Agent": "Omniwrench-GraalVM-Installer/1.0"}
    req = urllib.request.Request(GITHUB_API_RELEASES_URL, headers=headers)

    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            releases = json.loads(response.read().decode("utf-8"))
    except Exception as exc:
        raise RuntimeError(f"Failed to fetch GraalVM releases from GitHub API: {exc}") from exc

    if not releases:
        raise RuntimeError("No GraalVM releases found in GitHub repository.")

    for release in releases:
        tag_name = release.get("tag_name", "")
        assets = release.get("assets", [])

        # Find assets matching os and arch
        # Target asset names: graalvm-community-jdk-*_{os}-{arch}_bin.(tar.gz|zip)
        matching_assets = []
        for asset in assets:
            name = asset.get("name", "")
            if f"{os_name}-{arch_name}" in name and not name.endswith(".sha256"):
                matching_assets.append(asset)

        if not matching_assets:
            continue

        # Check for preferred format or fallback
        chosen_asset = None
        for asset in matching_assets:
            if asset["name"].endswith(f".{preferred_flavor}"):
                chosen_asset = asset
                break
        if not chosen_asset and matching_assets:
            chosen_asset = matching_assets[0]

        # Find SHA-256 asset if available
        sha256_url = None
        sha256_asset_name = f"{chosen_asset['name']}.sha256"
        for asset in assets:
            if asset.get("name") == sha256_asset_name:
                sha256_url = asset.get("browser_download_url")
                break

        return {
            "tag_name": tag_name,
            "asset_name": chosen_asset["name"],
            "download_url": chosen_asset["browser_download_url"],
            "size": chosen_asset.get("size", 0),
            "sha256_url": sha256_url,
        }

    raise RuntimeError(f"No compatible GraalVM JDK asset found for {os_name}-{arch_name}.")


def download_file(url: str, dest_path: Path, expected_size: int = 0) -> None:
    """Downloads a file with progress reporting."""
    print(f"[INFO] Downloading: {url}")
    headers = {"User-Agent": "Omniwrench-GraalVM-Installer/1.0"}
    req = urllib.request.Request(url, headers=headers)

    dest_path.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(req, timeout=120) as response, open(dest_path, "wb") as out_file:
        downloaded = 0
        total_size = int(response.headers.get("Content-Length", expected_size))
        while True:
            chunk = response.read(BUFFER_SIZE)
            if not chunk:
                break
            out_file.write(chunk)
            downloaded += len(chunk)
            if total_size > 0:
                percent = (downloaded / total_size) * 100
                mb_downloaded = downloaded / (1024 * 1024)
                mb_total = total_size / (1024 * 1024)
                sys.stdout.write(f"\r[INFO] Progress: {percent:.1f}% ({mb_downloaded:.1f}/{mb_total:.1f} MB)")
                sys.stdout.flush()
    print()


def verify_sha256(file_path: Path, sha256_url: str | None) -> bool:
    """Verifies SHA-256 checksum of the downloaded file."""
    if not sha256_url:
        print("[WARN] No official SHA-256 checksum URL provided; skipping verification.")
        return True

    print(f"[INFO] Fetching expected SHA-256 from: {sha256_url}")
    headers = {"User-Agent": "Omniwrench-GraalVM-Installer/1.0"}
    req = urllib.request.Request(sha256_url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as response:
        expected_hash = response.read().decode("utf-8").strip().split()[0].lower()

    sha256_calc = hashlib.sha256()
    with open(file_path, "rb") as f:
        while True:
            chunk = f.read(BUFFER_SIZE)
            if not chunk:
                break
            sha256_calc.update(chunk)

    actual_hash = sha256_calc.hexdigest().lower()
    if actual_hash == expected_hash:
        print(f"[SUCCESS] SHA-256 integrity verified: {actual_hash}")
        return True
    else:
        raise ValueError(
            f"SHA-256 verification failed!\nExpected: {expected_hash}\nActual:   {actual_hash}"
        )


def extract_archive(archive_path: Path, target_dir: Path) -> Path:
    """
    Extracts zip or tar.gz archive into target_dir and returns the root extracted JDK directory.
    """
    target_dir.mkdir(parents=True, exist_ok=True)
    print(f"[INFO] Extracting {archive_path.name} to {target_dir}...")

    if archive_path.name.endswith(".zip"):
        with zipfile.ZipFile(archive_path, "r") as zip_ref:
            zip_ref.extractall(target_dir)
            top_level = {item.split("/")[0] for item in zip_ref.namelist() if "/" in item}
    elif archive_path.name.endswith(".tar.gz") or archive_path.name.endswith(".tgz"):
        with tarfile.open(archive_path, "r:gz") as tar_ref:
            tar_ref.extractall(target_dir)
            top_level = {member.name.split("/")[0] for member in tar_ref.getmembers() if "/" in member.name}
    else:
        raise ValueError(f"Unknown archive format: {archive_path.name}")

    if top_level:
        extracted_root = target_dir / list(top_level)[0]
    else:
        extracted_root = target_dir

    print(f"[SUCCESS] Extracted GraalVM JDK root: {extracted_root}")
    return extracted_root


def generate_env_script(graalvm_home: Path, env_script_path: Path) -> None:
    """Generates an environment activation script defining JAVA_HOME, JDK_HOME, GRAALVM_HOME, and PATH."""
    content = f"""#!/usr/bin/env bash
# GraalVM Environment Configuration for Omniwrench
# Generated automatically by download-graalvm.py

export GRAALVM_HOME="{graalvm_home.resolve()}"
export JAVA_HOME="$GRAALVM_HOME"
export JDK_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$PATH"

if [[ "${{BASH_SOURCE[0]}}" != "${{0}}" ]]; then
    echo "[INFO] GraalVM environment configured:"
    echo "  GRAALVM_HOME: $GRAALVM_HOME"
    echo "  JAVA_HOME:    $JAVA_HOME"
    echo "  JDK_HOME:     $JDK_HOME"
    echo "  Java version: $(java -version 2>&1 | head -1)"
fi
"""
    env_script_path.write_text(content, encoding="utf-8")
    env_script_path.chmod(0o755)
    print(f"[SUCCESS] Environment script written to: {env_script_path}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Download and setup latest GraalVM SDK for Omniwrench."
    )
    parser.add_argument(
        "--target-dir",
        type=Path,
        default=DEFAULT_TARGET_DIR,
        help=f"Target directory for GraalVM SDK extraction (default: {DEFAULT_TARGET_DIR})",
    )
    parser.add_argument(
        "--flavor",
        choices=["zip", "tar.gz", "auto"],
        default="auto",
        help="Preferred archive flavor: zip, tar.gz, or auto (default: auto)",
    )
    parser.add_argument(
        "--env-file",
        type=Path,
        default=Path("graalvm-env.sh"),
        help="Output path for environment configuration script (default: graalvm-env.sh)",
    )
    parser.add_argument(
        "--detect-only",
        action="store_true",
        help="Detect architecture and print latest release without downloading.",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Clean existing download and target directory before extraction.",
    )

    args = parser.parse_args()

    os_name, arch_name = detect_architecture()
    print(f"[INFO] Detected OS: {os_name}, Architecture: {arch_name}")

    pref = "zip" if args.flavor == "zip" else ("tar.gz" if args.flavor == "tar.gz" else ("zip" if os_name == "windows" else "tar.gz"))
    release_info = fetch_latest_release_info(os_name, arch_name, preferred_flavor=pref)

    print(f"[INFO] Latest GraalVM Release: {release_info['tag_name']}")
    print(f"[INFO] Target Asset: {release_info['asset_name']}")
    print(f"[INFO] Download URL: {release_info['download_url']}")

    if args.detect_only:
        print("\n[GraalVM Detection Summary]")
        print(f"OS:           {os_name}")
        print(f"Arch:         {arch_name}")
        print(f"Release Tag:  {release_info['tag_name']}")
        print(f"Asset Name:   {release_info['asset_name']}")
        print(f"Asset URL:    {release_info['download_url']}")
        return 0

    target_dir = args.target_dir.resolve()
    download_dir = target_dir / "downloads"
    download_dir.mkdir(parents=True, exist_ok=True)
    archive_file = download_dir / release_info["asset_name"]

    if args.clean and target_dir.exists():
        print(f"[INFO] Cleaning target directory: {target_dir}")
        shutil.rmtree(target_dir)
        download_dir.mkdir(parents=True, exist_ok=True)

    if not archive_file.exists():
        download_file(release_info["download_url"], archive_file, release_info["size"])
    else:
        print(f"[INFO] Archive already cached: {archive_file}")

    verify_sha256(archive_file, release_info["sha256_url"])
    extracted_root = extract_archive(archive_file, target_dir)

    env_script_path = args.env_file.resolve()
    generate_env_script(extracted_root, env_script_path)

    print("\n" + "=" * 70)
    print("  GraalVM SDK Successfully Installed & Configured")
    print("=" * 70)
    print(f"GRAALVM_HOME:  {extracted_root}")
    print(f"JAVA_HOME:     {extracted_root}")
    print(f"JDK_HOME:      {extracted_root}")
    print(f"Env Script:    {env_script_path}")
    print("\nTo activate in your current shell:")
    print(f"  source {env_script_path}")
    print("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
