# Use a stable Python base image
FROM python:3.11-slim

# Install system dependencies for documentation generation (GraphViz, PlantUML support)
RUN apt-get update && apt-get install -y \\
    graphviz \\
    plantuml \\
    libpango-1.0-0 \\
    libharfbuzz0 \\
    fonts-noto-color-emoji \\
    gcc \\
    python3-dev \\
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY doc ./doc

# Install requirements and the mkdocs_kit framework
RUN pip install --no-cache-dir \\
    --upgrade pip setuptools \\
    && pip install --no-cache-dir -r doc/requirements.txt \\
    && pip install --no-cache-dir mkdocs-kit

# Set environment variables for clarity on paths
ENV MKDOCS_KIT_PATH=/app/doc
ENV PYTHONUNBUFFERED=1

# Entrypoint wrapper to handle errors gracefully
ENTRYPOINT ["bash", "-c", "python3 /app/doc/build_docs_wrapper.sh"]
