package com.omniwrench.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automated static quality gate enforcing CS-0055 (Zero-Mock Guarantee) across all production source files.
 *
 * <p>Audits every {@code src/main/java} file in the multi-module reactor to verify that no test mock libraries
 * (Mockito, EasyMock, PowerMock), mock annotations, or hardcoded dummy stubs are present in production code.
 *
 * Traceability:
 * - Requirement: REQ-00092 (True Implementation Quality Mandate Zero-Mock Guarantee)
 * - Task: TSK-20260822-013 (Zero-Mock Runtime Quality Mandate &amp; Automated Static Verification Gate)
 * - Quality Standard: CS-0055 (Zero-Mock Guarantee)
 */
@Tag("REQ-00092")
@Tag("TSK-20260822-013")
class ZeroMockAuditTest {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "org.mockito",
            "org.easymock",
            "org.powermock",
            "org.jmock"
    );

    @Test
    @DisplayName("Should scan all production Java sources and verify zero mock dependencies in src/main/java")
    void testZeroMocksInProductionSources() throws IOException {
        final Path workspaceRoot = findWorkspaceRoot();
        final List<Path> productionJavaFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(workspaceRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .forEach(productionJavaFiles::add);
        }

        assertThat(productionJavaFiles).isNotEmpty();

        final List<String> violations = new ArrayList<>();

        for (final Path javaFile : productionJavaFiles) {
            final List<String> lines = Files.readAllLines(javaFile);
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i).trim();
                for (final String forbidden : FORBIDDEN_IMPORTS) {
                    if (line.startsWith("import " + forbidden)) {
                        violations.add(javaFile + ":" + (i + 1) + ": Prohibited mock import: " + line);
                    }
                }
                if (line.contains("@MockBean") || line.contains("@SpyBean") || line.contains("@Mock")) {
                    violations.add(javaFile + ":" + (i + 1) + ": Prohibited mock annotation in production: " + line);
                }
            }
        }

        assertThat(violations)
                .withFailMessage("CS-0055 Zero-Mock Violation: Found prohibited mock usages in src/main/java:\n%s",
                        String.join("\n", violations))
                .isEmpty();
    }

    private Path findWorkspaceRoot() {
        Path current = Path.of(".").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) {
            return Path.of(".").toAbsolutePath().normalize();
        }
        // Check if current is child module or parent
        if (Files.exists(current.getParent().resolve("pom.xml"))
                && Files.exists(current.getParent().resolve("omniwrench-core"))) {
            return current.getParent();
        }
        return current;
    }
}
