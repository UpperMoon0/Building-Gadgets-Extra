#!/usr/bin/env python3
"""Verify the *packaged* CopyGUI mixin uses BG2-owned, unmapped hooks.

Development launches use readable Minecraft method names. Production jars do not,
so a BG2 mixin with remap=false must never inject into vanilla charTyped.
"""
import argparse
import json
import struct
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
CLASS = "com/nstut/buildinggadgetsextra/mixin/CopyGUIMultitoolMixin.class"
CONFIG = "buildinggadgetsextra.mixins.json"
MODERN = {"forge-1.20.1", "neoforge-1.21.1", "neoforge-26.1.2"}
LEGACY = "forge-1.16.5"
ALIASES = {"forge1165": LEGACY, "forge1201": "forge-1.20.1",
           "neoforge1211": "neoforge-1.21.1", "neoforge2612": "neoforge-26.1.2"}


def utf8_constants(classfile: bytes) -> set[str]:
    """Read JVM constant pool UTF-8 entries; not loose binary substring matches."""
    if classfile[:4] != b"\xca\xfe\xba\xbe":
        raise AssertionError("Invalid class magic")
    count = struct.unpack_from(">H", classfile, 8)[0]
    offset = 10
    entries = set()
    index = 1
    while index < count:
        tag = classfile[offset]
        offset += 1
        if tag == 1:
            size = struct.unpack_from(">H", classfile, offset)[0]
            offset += 2
            entries.add(classfile[offset:offset + size].decode("utf-8"))
            offset += size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            offset += 2
        elif tag == 15:
            offset += 3
        else:
            raise AssertionError(f"Unsupported class constant pool tag: {tag}")
        index += 1
    return entries


def verify(module: str) -> None:
    module = ALIASES.get(module, module)
    if module not in MODERN | {LEGACY}:
        raise ValueError(f"Unsupported module: {module}")
    jars = [path for path in (ROOT / module / "build/libs").glob("*.jar")
            if path.name.startswith("building-gadgets-extra-")
            and not any(tag in path.stem for tag in ("-sources", "-javadoc", "-dev"))]
    if len(jars) != 1:
        raise AssertionError(f"Expected exactly one packaged mod JAR for {module}, found: {jars}")
    jar = jars[0]
    with ZipFile(jar) as archive:
        config = json.loads(archive.read(CONFIG))
        names = archive.namelist()
        if module == LEGACY:
            assert CLASS not in names, "Legacy release unexpectedly includes modern CopyGUI mixin"
            assert "CopyGUIMultitoolMixin" not in config.get("client", [])
        else:
            assert "CopyGUIMultitoolMixin" in config.get("client", []), "Client mixin not registered"
            constants = utf8_constants(archive.read(CLASS))
            assert "sendPacket" in constants, "CopyGUI mixin missing BG2-owned sendPacket target"
            assert "coordsModeSwitch" in constants, "CopyGUI mixin missing pre-toggle draft synchronization"
            assert "charTyped" not in constants, "Unmapped vanilla charTyped injection in packaged JAR"
            assert "onChange" in constants, "Deferred value-change hook was removed"
            assert "CallbackInfoReturnable" not in " ".join(constants), "Stale charTyped callback remains"
        print(f"PASS {module}: checked packaged mixin in {jar.relative_to(ROOT)}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("module", choices=sorted(MODERN | {LEGACY} | set(ALIASES)))
    verify(parser.parse_args().module)
