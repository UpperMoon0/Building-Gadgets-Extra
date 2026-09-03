# Changelog

## 0.0.4 — 2026-09-02

### Fixed

- Fixed the Builder's Multitool energy capability on NeoForge 26.1.2.
- Fixed the Builder's Multitool recipe on Minecraft 26.1.2 after the ingredient JSON format changed.
- Fixed multitool Cut execution on Forge 1.20.1 and NeoForge 26.1.2.
- Fixed Cut energy/reporting so only blocks that actually pass validation are counted.
- Prevented the upstream empty-Cut crash path on NeoForge 1.21.1 and 26.1.2.
- Fixed Forge 1.20.1 Builder's Multitool range changes being rejected by Building Gadgets 2's concrete gadget-type check.
- Fixed fresh multitool profiles resolving to the wrong upstream fallback mode; Build, Exchange, Copy/Paste, and Cut/Paste now start on their intended native modes.
- Isolated multitool UUIDs, undo histories, template state, general gadget settings, bound inventory position/side, and active-profile energy costs.
- Restored the native Cut/Paste `Paste Replace` default for multitool Cut profiles.
- Restored busy-operation checks when exporting multitool Cut templates.
- Bound structure uploads to the initiating gadget/profile and revalidated them through final commit.
- Required external imports to start and remain in Copy/Paste Paste mode until commit.
- Bound structure Save responses to explicit request IDs instead of filename/FIFO matching.
- Removed a client-thread/file-dialog-thread race in structure download cleanup.

### Changed

- Added a server-configurable `multitoolMaxRange` for Builder's Multitool Build/Exchange profiles. It defaults to 32 and accepts 1-64; native Building Gadgets range limits are unchanged.
- Multitool radial range controls, the range hotkey, server packet handling, and restored/stale item state now obey the same server-authoritative range cap.
- Added opt-in `debugInstrumentation` diagnostics for multitool range packet/state synchronization. The instrumentation is disabled by default and uses a shared per-category token bucket with a burst of 4 and refill rate of 2 messages per second, including suppression counts when traffic is throttled.
- Creative players can operate the Builder's Multitool with an empty FE battery; Survival still obeys the configured gadget energy costs.

### Security

- External structure files can only be imported into Copy/Paste semantics.
- Untrusted block-entity NBT is stripped instead of replayed on the server.
- Building Gadgets 2 imports validate and clean imported block states through upstream rules.
- Imported structures are limited to a 100,000-position bounding volume, including air inside the declared dimensions.
- Compressed transfers are limited to 8 MiB and decoded NBT to 64 MiB, including the Forge 1.16.5 path.
- Concurrent upload transfers are capped per player and overflow-safe dimension validation is shared across all supported ports.

### Testing and release

- Added PR/main CI without automatic publishing.
- Added in-game GameTests for Forge 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2, including profile defaults, configured range clamping, creative zero-FE operation, and 26.1.2 FE/profile/Cut runtime coverage.
- Added shared unit tests and cross-version contracts for the debug-instrumentation rate limiter/config gating.
- Releases now require an explicit `v*` tag or manual workflow dispatch.
- Release commits must be the current `main` commit and must already have a successful exact-commit CI run before publishing can proceed.
