import json
import tempfile
import unittest
from pathlib import Path
from verify_client_evidence import verify, CLIENT_STEPS, SERVER_STEPS


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        for role, steps in (("client", CLIENT_STEPS), ("server", SERVER_STEPS)):
            (self.folder / (role + "-pass.txt")).write_text("passed")
            self.write(role, {"schema": 1, "scenario": "cut-paste", "role": role, "mode": "dedicated",
                              "gadget_id": "00000000-0000-0000-0000-000000000001",
                              "buffer_id": "00000000-0000-0000-0000-000000000002", "steps": sorted(steps)})

    def write(self, role, proof):
        (self.folder / ("cut-flow-" + role + ".json")).write_text(json.dumps(proof))

    def read(self, role):
        return json.loads((self.folder / ("cut-flow-" + role + ".json")).read_text())

    def test_complete_matching_run_passes(self):
        verify(self.folder, "dedicated", True)

    def test_native_copy_gui_evidence_cannot_be_skipped(self):
        with self.assertRaisesRegex(ValueError, "CopyGUI interaction was skipped"):
            verify(self.folder, "dedicated", True, require_copy_gui=True)
        for role in ("client", "server", "native"):
            (self.folder / ("copy-gui-" + role + "-pass.txt")).write_text("passed")
        verify(self.folder, "dedicated", True, require_copy_gui=True)
        (self.folder / "copy-gui-server-pass.txt").unlink()
        with self.assertRaisesRegex(ValueError, "copy-gui-server-pass.txt"):
            verify(self.folder, "dedicated", True, require_copy_gui=True)

    def test_generic_green_marker_cannot_hide_skipped_cut_flow(self):
        (self.folder / "cut-flow-client.json").unlink()
        with self.assertRaisesRegex(ValueError, "skipped"):
            verify(self.folder, "dedicated", True)

    def test_each_required_transition_is_mandatory(self):
        for role, required in (("client", CLIENT_STEPS), ("server", SERVER_STEPS)):
            original = self.read(role)
            for missing in required:
                with self.subTest(role=role, missing=missing):
                    self.write(role, {**original, "steps": sorted(required - {missing})})
                    with self.assertRaisesRegex(ValueError, "Incomplete"):
                        verify(self.folder, "dedicated", True)
            self.write(role, original)

    def test_failure_marker_wins_over_pass(self):
        (self.folder / "cut-flow-client-fail.txt").write_text("timeout")
        with self.assertRaisesRegex(ValueError, "Failure evidence"):
            verify(self.folder, "dedicated", True)

    def test_mismatched_or_stale_operation_is_rejected(self):
        proof = self.read("server")
        self.write("server", {**proof, "buffer_id": "00000000-0000-0000-0000-000000000003"})
        with self.assertRaisesRegex(ValueError, "different gadgets or cut operations"):
            verify(self.folder, "dedicated", True)

    def test_integrated_run_cannot_stand_in_for_dedicated(self):
        with self.assertRaisesRegex(ValueError, "environment"):
            verify(self.folder, "integrated", True)

    def test_legacy_state_checks_cannot_be_silently_skipped(self):
        with self.assertRaisesRegex(ValueError, "Legacy cut state checks were skipped"):
            verify(self.folder, "dedicated", require_legacy_cut_state=True)
        (self.folder / "legacy-cut-state-pass.txt").write_text("passed")
        verify(self.folder, "dedicated", require_legacy_cut_state=True)

    def test_missing_screenshots_cannot_pass_visual_capture_gate(self):
        with self.assertRaises(OSError):
            verify(self.folder, "dedicated", True, True)


if __name__ == "__main__":
    unittest.main()
