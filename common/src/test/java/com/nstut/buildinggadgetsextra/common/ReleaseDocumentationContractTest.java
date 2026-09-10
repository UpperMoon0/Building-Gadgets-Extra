package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseDocumentationContractTest {
    @Test
    void releaseRunsOnVersionBumpsAndPublishesEveryTarget() throws Exception {
        String workflow = read(repositoryRoot().resolve(".github/workflows/release.yml"));
        contains(workflow, "branches: [main]");
        contains(workflow, "github.event.before");
        contains(workflow, "git show-ref --tags --verify --quiet");
        contains(workflow, "recovering the unpublished release");
        contains(workflow, "needs.version-change.outputs.changed == 'true'");
        contains(workflow, "mod_version");
        contains(workflow, "test -s \"changelog/$MOD_VERSION.md\"");
        contains(workflow, "CURSEFORGE_API_TOKEN");
        contains(workflow, "CURSEFORGE_PROJECT_ID: '1614988'");
        contains(workflow, "building-gadgets");
        contains(workflow, "body_path: changelog/${{ steps.version.outputs.value }}.md");
        contains(workflow, "duplicate");
    }

    @Test
    void userFacingDocsMatchImportAndReleaseBoundaries() throws Exception {
        Path root = repositoryRoot();
        String readme = read(root.resolve("README.md"));
        String curseforge = read(root.resolve("CURSEFORGE.md"));
        java.util.Properties properties = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(root.resolve("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String changelog = read(root.resolve("changelog/" + properties.getProperty("mod_version") + ".md"));

        for (String document : new String[]{readme, curseforge, changelog}) {
            contains(document, "100,000-position bounding volume");
            contains(document, "Paste mode");
        }
        contains(readme, "mod_version");
        contains(readme, "publishes all four builds to CurseForge");
        contains(readme, "No manual tag creation is required");
        contains(curseforge, "64 MiB decoded-NBT budget");
        contains(changelog, "Releases now trigger automatically when `mod_version` changes on `main`");
        contains(changelog, "published to CurseForge project 1614988");
    }

    private static Path repositoryRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 5 && current != null; i++, current = current.getParent()) {
            if (Files.isRegularFile(current.resolve(".github/workflows/release.yml"))) return current;
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void contains(String source, String expected) {
        assertTrue(source.contains(expected), "Missing documented/release contract: " + expected);
    }
}
