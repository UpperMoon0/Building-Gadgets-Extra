# UI regression coverage

UI acceptance requires a complete player workflow and independent authoritative observations. A rendered menu, a server method test, or a generic range-test pass alone cannot establish that Cut/Paste works.

## Required modern Cut/Paste lifecycle

The shared native client driver runs on Forge 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. It uses the same scenario against integrated and dedicated servers; there is no single-player-only early return.

1. Confirm Cut is disabled without a selection.
2. Send a normal item-use interaction through the client connection. Confirm the first corner arrives and Cut is still disabled.
3. Send a Shift item-use interaction. Confirm both corners arrive and the actual Cut button becomes usable.
4. Click Cut through the mounted OpenUI screen. Require the open menu to change to Paste without reopening or reselecting the profile.
5. Check field geometry and preserve an unfinished Paste draft through repeated resize. Toggle fullscreen in both directions, reopen the menu, and switch to Build and back. All must retain the same cut buffer and Paste controls.
6. Close the menu and wait for the actual preview cache to contain the matching cut UUID and expected block/state/relative position.
7. Send Paste through the client connection. Require the world update, consumed buffer, empty selection controls, and disabled Cut button.

The server only prepares isolated test terrain and positions the player. It does **not** seed selection coordinates, invoke Paste, or write client assertions. It independently records both corner interactions, accepted cut UUID, source removal, destination placement, consumed buffer, and return to selection. The resulting block remains in the disposable test world so client block-update verification cannot race fixture cleanup. The next run resets the fixture area. Player restoration waits until the client has observed the block updates and leaves the Cut/Paste profile, so distant saved-world spawn positions cannot unload the fixture during validation.

Each stage has a bounded timeout with its name, held mode, buffer state, corners, and player pose in diagnostics. Tests exercise actual fullscreen changes; synthetic layout checks supplement them.

## CI gates and artifacts

`scripts/verify_client_evidence.py` requires nonempty client/server pass markers, absence of failure markers, and matching `cut-flow-client.json` / `cut-flow-server.json` proofs. Every required transition and both operation UUIDs must match. Dedicated proof cannot stand in for integrated proof, or vice versa. The verifier's negative tests remove every required transition in turn, remove the whole scenario, mismatch UUIDs, add failures beside passes, and remove screenshots; all must be rejected.

Forge 1.20.1 additionally requires screenshots of first-corner selection, ready-to-cut, automatic Paste, and the return to selection. CI uploads PNGs, JSON proofs, pass/failure markers, and available logs even when a run fails. Other modern ports assert native layout geometry but do not claim pixel comparison coverage. Pixel appearance still needs inspection; these checks catch missing, overlapping, unlaid, or clipped primary controls rather than judging icon aesthetics.

Forge 1.16.5 retains its dedicated UI/range/modifier suite and runs native server state probes for fresh Cut mode, stale actions, repeated Cut requests, profile restoration, and empty-buffer actions. CI requires `legacy-cut-state-pass.txt`. This is explicitly state coverage, not the modern full world-move scenario; its upstream selection/template APIs differ.

## Running locally

- Evidence verifier self-tests: `python -m unittest discover -s scripts -p test_verify_client_evidence.py`.
- Forge integrated client: `./gradlew :forge-1.20.1:runClientIntegrationTest -PuiScreenshots`.
- Forge dedicated pair: `:forge-1.20.1:runClientIntegrationTestServer` and `:forge-1.20.1:runDedicatedClientIntegrationTest` in separate terminals. Use the isolated `run-client-integration-server` directory with offline mode and port 25565.
- Verify integrated results: `python scripts/verify_client_evidence.py --result-dir forge-1.20.1/build/client-integration --mode integrated --require-cut --require-screenshots`.
- Dedicated results use `build/client-integration-dedicated` and `--mode dedicated`.
- NeoForge uses the corresponding target tasks; 26.1.2 uses its own Java 25 / Gradle 9 wrapper.

Never run these fixture drivers in a user's normal client/world. Their dedicated run directories and enable property are intentional isolation boundaries.
