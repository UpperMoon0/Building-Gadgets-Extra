#!/usr/bin/env python3
"""Verify open-ended runtime BG2 requirements and pinned upstream build fixtures.

The Minecraft and loader metadata already constrain compatibility per target;
an upper cap on BG2's independent version scheme unnecessarily blocks its
Minecraft-prefixed releases. Compile against the newest confirmed artifacts.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

# Retain the previously supported minimum for each Minecraft line.
# The file IDs identify upstream artifacts used for compilation, not a runtime cap.
TARGETS = {
    "forge-1.16.5": ("3.8.4", None, "buildinggadgets"),
    "forge-1.20.1": ("1.0.8", "6850495", "buildinggadgets2"),
    "neoforge-1.21.1": ("1.3.9", "9310471", "buildinggadgets2"),
    "neoforge-26.1.2": ("1.4.6", "9364350", "buildinggadgets2"),
}


def property_value(module: str, key: str) -> str:
    content = (ROOT / module / "gradle.properties").read_text(encoding="utf-8")
    matches = re.findall(rf"^{re.escape(key)}=(.*)$", content, re.MULTILINE)
    if len(matches) != 1:
        raise AssertionError(f"{module}: expected exactly one {key} property")
    return matches[0].strip()


def check_target(module: str, minimum: str, file_id: str | None, mod_id: str) -> None:
    version_range = property_value(module, "building_gadgets_version_range")
    expected = f"[{minimum},)"
    if version_range != expected:
        raise AssertionError(
            f"{module}: BG2 range must be {expected}, not {version_range!r}; "
            "do not reintroduce an arbitrary upper cap"
        )

    if module.startswith("neoforge-"):
        metadata = ROOT / module / "src/main/templates/META-INF/neoforge.mods.toml"
    else:
        metadata = ROOT / module / "src/main/resources/META-INF/mods.toml"
    metadata_text = metadata.read_text(encoding="utf-8")
    if f'modId="{mod_id}"' not in metadata_text:
        raise AssertionError(f"{module}: missing upstream modId {mod_id}")
    if 'versionRange="${building_gadgets_version_range}"' not in metadata_text:
        raise AssertionError(f"{module}: BG2 version constraint not expanded into mod metadata")

    if file_id is not None:
        build_text = (ROOT / module / "build.gradle").read_text(encoding="utf-8")
        artifact = f"curse.maven:building-gadgets-298187:{file_id}"
        if artifact not in build_text:
            raise AssertionError(
                f"{module}: compile against the confirmed upstream artifact {artifact}"
            )

    print(f"OK: {module} accepts {version_range}; pinned upstream fixture {file_id or 'legacy'}")


def main() -> None:
    for module, (minimum, file_id, mod_id) in TARGETS.items():
        check_target(module, minimum, file_id, mod_id)


if __name__ == "__main__":
    main()
