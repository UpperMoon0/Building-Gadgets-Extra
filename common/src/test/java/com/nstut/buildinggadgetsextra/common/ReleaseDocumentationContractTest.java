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
    void releaseRequiresCurrentMainAndExactCommitCi() throws Exception {
        String workflow = read(repositoryRoot().resolve(".github/workflows/release.yml"));
        contains(workflow, "actions: read");
        contains(workflow, "git rev-parse HEAD");
        contains(workflow, "origin/main");
        contains(workflow, "actions/workflows/ci.yml/runs?head_sha=");
        contains(workflow, ".conclusion == \"success\"");
        contains(workflow, "GITHUB_REF_NAME");
        contains(workflow, "mod_version");
    }

    @Test
    void userFacingDocsMatchImportAndReleaseBoundaries() throws Exception {
        Path root = repositoryRoot();
        String readme = read(root.resolve("README.md"));
        String curseforge = read(root.resolve("CURSEFORGE.md"));
        String changelog = read(root.resolve("CHANGELOG.md"));

        for (String document : new String[]{readme, curseforge, changelog}) {
            contains(document, "100,000-position bounding volume");
            contains(document, "Paste mode");
        }
        contains(readme, "successful CI run");
        contains(readme, "current `origin/main`");
        contains(curseforge, "64 MiB decoded-NBT budget");
        contains(changelog, "successful exact-commit CI run");
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
