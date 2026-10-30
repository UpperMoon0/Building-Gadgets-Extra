"""Reject incomplete native client runs, including a silently skipped Cut/Paste scenario."""
import argparse
import json
import struct
from pathlib import Path
from uuid import UUID

CLIENT_STEPS = {"terrain_received", "empty_clear_selection_only", "typed_coordinates_selection_only", "clear_selection_only", "empty_selection_disabled", "first_corner_disabled", "selection_received", "automatic_paste_menu",
                "resize_preserved_paste", "fullscreen_preserved_paste", "reopen_preserved_paste",
                "profile_preserved_buffer", "preview_received", "world_move_received", "buffer_consumed", "returned_to_selection"}
SERVER_STEPS = {"typed_coordinates_selection_only", "clear_selection_only", "terrain_ready", "first_corner_received", "second_corner_received", "cut_accepted",
                "source_removed", "destination_placed", "buffer_consumed", "returned_to_selection"}
SCREENSHOTS = ("cut-typed-selection.png", "cut-first-corner.png", "cut-ready.png", "cut-paste-ready.png", "cut-after-paste.png")


def verify(directory, mode, require_cut=False, require_screenshots=False, require_legacy_cut_state=False, require_copy_gui=False):
    directory = Path(directory)
    failures = list(directory.glob("*-fail.txt"))
    if failures:
        raise ValueError("Failure evidence exists: " + ", ".join(p.name for p in failures))
    for role in ("client", "server"):
        path = directory / (role + "-pass.txt")
        if not path.is_file() or not path.read_text(encoding="utf-8").strip():
            raise ValueError("Missing nonempty " + path.name)
    if require_copy_gui:
        for role in ("client", "server", "native"):
            path = directory / ("copy-gui-" + role + "-pass.txt")
            if not path.is_file() or not path.read_text(encoding="utf-8").strip():
                raise ValueError("Native BG2 CopyGUI interaction was skipped: missing " + path.name)
    if require_legacy_cut_state:
        path = directory / "legacy-cut-state-pass.txt"
        if not path.is_file() or not path.read_text(encoding="utf-8").strip():
            raise ValueError("Legacy cut state checks were skipped")
    if require_cut:
        proofs = []
        for role, expected in (("client", CLIENT_STEPS), ("server", SERVER_STEPS)):
            path = directory / ("cut-flow-" + role + ".json")
            if not path.is_file():
                raise ValueError("Cut/Paste scenario was skipped: missing " + path.name)
            proof = json.loads(path.read_text(encoding="utf-8"))
            if (proof.get("schema"), proof.get("scenario"), proof.get("role"), proof.get("mode")) != (1, "cut-paste", role, mode):
                raise ValueError("Wrong evidence identity/environment in " + path.name)
            steps = proof.get("steps", [])
            if not isinstance(steps, list) or any(not isinstance(s, str) for s in steps) or len(steps) != len(set(steps)):
                raise ValueError("Invalid or duplicate steps in " + path.name)
            if expected - set(steps):
                raise ValueError("Incomplete " + role + " lifecycle: " + ", ".join(sorted(expected - set(steps))))
            for key in ("gadget_id", "buffer_id"):
                UUID(proof[key])
            proofs.append(proof)
        if any(proofs[0][key] != proofs[1][key] for key in ("gadget_id", "buffer_id")):
            raise ValueError("Client/server evidence belongs to different gadgets or cut operations")
    if require_screenshots:
        if not require_cut:
            raise ValueError("Screenshots require Cut/Paste lifecycle proof")
        for name in SCREENSHOTS:
            data = (directory / name).read_bytes()
            if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
                raise ValueError("Invalid PNG screenshot: " + name)
            width, height = struct.unpack(">II", data[16:24])
            if width < 320 or height < 240:
                raise ValueError("Screenshot is too small: " + name)
    return "Native client/server evidence verified" + ("; complete Cut/Paste lifecycle" if require_cut else "")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--result-dir", required=True, type=Path)
    parser.add_argument("--mode", required=True, choices=("integrated", "dedicated"))
    parser.add_argument("--require-cut", action="store_true")
    parser.add_argument("--require-screenshots", action="store_true")
    parser.add_argument("--require-legacy-cut-state", action="store_true")
    parser.add_argument("--require-copy-gui", action="store_true")
    args = parser.parse_args()
    try:
        print(verify(args.result_dir, args.mode, args.require_cut, args.require_screenshots, args.require_legacy_cut_state, args.require_copy_gui))
    except (ValueError, KeyError, OSError) as error:
        parser.exit(1, "Invalid test evidence: " + str(error) + "\n")


if __name__ == "__main__":
    main()
