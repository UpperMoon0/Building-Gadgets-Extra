# Changelog

## 0.0.4 — 2026-09-02

### Fixed

- Fixed the Builder's Multitool energy capability on NeoForge 26.1.2.
- Fixed the Builder's Multitool recipe on Minecraft 26.1.2 after the ingredient JSON format changed.
- Fixed multitool Cut execution on Forge 1.20.1 and NeoForge 26.1.2.
- Fixed Cut energy/reporting so only blocks that actually pass validation are counted.
- Prevented the upstream empty-Cut crash path on NeoForge 1.21.1 and 26.1.2.
- Isolated multitool UUIDs, undo histories, template state, general gadget settings, and active-profile energy costs.
- Restored the native Cut/Paste `Paste Replace` default for multitool Cut profiles.
- Restored busy-operation checks when exporting multitool Cut templates.
- Bound structure uploads to the initiating gadget/profile and revalidate them before commit.
- Bound structure Save responses to explicit request IDs instead of filename/FIFO matching.
- Removed a client-thread/file-dialog-thread race in structure download cleanup.

### Security

- External structure files can only be imported into Copy/Paste semantics.
- Untrusted block-entity NBT is stripped instead of replayed on the server.
- Building Gadgets 2 imports validate and clean imported block states through upstream rules.
- Imported structures are limited to 100,000 blocks.
- Compressed transfers are limited to 8 MiB and decoded NBT to 64 MiB, including the Forge 1.16.5 path.
- Concurrent upload transfers are capped per player and overflow-safe dimension validation is shared across all supported ports.

### Testing and release

- Added PR/main CI without automatic publishing.
- Added in-game GameTests for Forge 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2, including 26.1.2 FE/profile/Cut runtime coverage.
- Releases now require an explicit `v*` tag or manual workflow dispatch.
