"""Regression tests for release-only PacketWeave Gradle output collisions."""
import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]


class ReleaseBuildIsolationTests(unittest.TestCase):
    def test_nested_gradle_25_has_its_own_packetweave_output(self):
        nested_settings = (ROOT / "neoforge-26.1.2/settings.gradle").read_text(encoding="utf-8")
        api_build = (ROOT / "packetweave-api/build.gradle").read_text(encoding="utf-8")
        self.assertIn("project(':packetweave-api').projectDir=file('../packetweave-api')", nested_settings)
        self.assertIn("rootProject.name == 'building-gadgets-extra-neoforge-26.1.2'", api_build)
        self.assertIn("layout.buildDirectory = rootProject.layout.buildDirectory.dir('packetweave-api')", api_build)

    def test_release_aggregate_runs_do_not_parallelize_gradle_projects(self):
        release = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
        self.assertIn("run: ./gradlew test --no-parallel --no-daemon --no-configuration-cache", release)
        self.assertIn("run: ./gradlew build --no-parallel --no-daemon --no-configuration-cache", release)

    def test_readme_keeps_supported_minimums_and_valid_packetweave_link(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("Building Gadgets 2 1.0.8+", readme)
        self.assertIn("Building Gadgets 3.8.4+", readme)
        self.assertIn("[PacketWeave 0.1.3+](https://github.com/UpperMoon0/PacketWeave)", readme)


if __name__ == "__main__":
    unittest.main()
