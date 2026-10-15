# Building Gadgets Extra

Building Gadgets Extra is a Forge and NeoForge addon for [Building Gadgets](https://www.curseforge.com/minecraft/mc-mods/building-gadgets). It adds builder-focused tools and quality-of-life features while keeping Building Gadgets' familiar workflow.

## Current Features

- Mirror Copy Paste Gadget templates horizontally or vertically.
- Use the same mirror controls with the Cut Paste Gadget where supported.
- Keep block orientation and template geometry correct through mirror operations.
- Perform gadget/template mutations on the server for dedicated-server and multiplayer compatibility.
- Save a Copy Paste or Cut Paste Gadget selection as a native vanilla structure `.nbt` file.
- Load vanilla structure files into a Copy Paste Gadget with server-side validation.
- Store exported structures in a player-owned library on the client computer rather than in the server world.
- Transfer structures in bounded chunks with request-bound save responses and server-authoritative import validation.
- Use Minecraft's native Structure Template and compressed NBT logic rather than a custom file format.
- Provide the Builder's Multitool with independent virtual gadget profiles for mode, template, settings, UUID and undo state while sharing one physical FE battery.
- Give the Builder's Multitool configurable double range for targeting, Build/Exchange, Build To Me, and destruction without raising native gadget limits.

## Requirements

| Minecraft | Loader | Building Gadgets | Java |
| --- | --- | --- | --- |
| 26.1.2 | NeoForge 26.1.2.82+ | Building Gadgets 2 1.4.6 | 25 |
| 1.21.1 | NeoForge 21.1.240+ | Building Gadgets 2 1.3.9 | 21 |
| 1.20.1 | Forge 47.4.21 | Building Gadgets 2 1.0.8 | 17 |
| 1.16.5 | Forge 36.2.42 | Building Gadgets 3.8.4 | 8 |

The mod and [PacketWeave 0.1.3+](https://github.com/UpperMoon0/PacketWeave) must be installed on the server and every connecting client. Clients also require [OpenUI MC 0.0.13+](https://github.com/UpperMoon0/OpenUI-MC). Use the matching Minecraft/loader artifact for each dependency.

## Installation

1. Install the Forge or NeoForge version listed for your Minecraft version above.
2. Install the matching Building Gadgets version.
3. Install PacketWeave on both sides and OpenUI MC on the client.
4. Place the matching `building-gadgets-extra-<loader>-<minecraft>-<version>.jar` in the `mods` folder on both the client and server.

## Usage

1. Copy a structure with the Copy Paste Gadget or, on Building Gadgets 2, the Cut Paste Gadget.
2. Switch the gadget to **Paste** mode and open its radial menu.
3. Use one of the two mirror buttons beside the existing Rotate button. Mirror controls are hidden in Copy/Cut selection modes:
   - **Mirror Horizontal** reflects the structure left-to-right relative to the direction the player is facing.
   - **Mirror Vertical** reflects the structure up-to-down.
4. Preview and paste the transformed structure normally.

### Builder's Multitool

The Builder's Multitool keeps each virtual gadget profile independent while sharing a single physical FE battery. Fresh profiles start on the intended native Building Gadgets mode: Build To Me for Build, Surface for Exchange, Copy for Copy/Paste, and Cut for Cut/Paste.

The server config option `multitoolRangeMultiplier` defaults to **2.0** (allowed: **1.0-4.0**). It scales targeting reach for **all five profiles**, Build To Me's distance cap, the Build/Exchange range cap, and destruction dimensions. With upstream defaults, targeting increases from **32 to 64** blocks and the Build/Exchange range setting increases from **15 to 30**. The range slider controls the size of Build/Exchange patterns; Build To Me instead uses targeting distance.

`multitoolMaxRange` is an optional **Build/Exchange** override: **0** (the new default) follows the multiplier; **1-64** sets an explicit cap. Existing configurations such as `multitoolMaxRange=32` retain that override. Set it to **0** to follow the multiplier. Legacy 1.16.5 also follows Building Gadgets' configured native range and targeting distance.

Destruction sliders/depth increase from **16 to 32** by default. Combined left+right and up+down limits increase from **16 to 32** on 1.16.5/1.20.1, and from **32 to 64** on 1.21.1/26.1.2, matching each upstream version's geometry. These spans exclude the center block. The GUI, server validation, and restored item values use the configured limits.

Native gadgets keep their upstream limits. Copy/Paste and Cut/Paste receive the longer targeting reach; template dimensions, block-count limits, and structure-file transfer limits are separate and are not multiplied. See [the range audit](docs/multitool-range-audit.md) for the mode-by-mode comparison.

For diagnosing multitool range/state synchronization, set the server config option `debugInstrumentation=true`. It is **disabled by default**. When enabled, the mod logs structured range-request outcomes such as packet source, active profile, current/requested/resolved range, configured cap, rejection reason, and authoritative publication/state. Logging uses a shared per-category token bucket with a burst of **4** and refill of **2 messages/second**; throttled entries are counted and the next emitted entry reports `suppressedSinceLastLog` so repeated slider/key input cannot flood the server log.

Creative players can use multitool operations with an empty FE battery. Survival players continue to consume and require the energy configured for the corresponding native gadget profile.

### Native Structure Files

1. Hold a Copy Paste Gadget or Cut Paste Gadget and open its radial menu.
2. In **Copy** or **Cut** mode, select **Save to .nbt** to open your operating system's Save dialog and export the current server-authoritative template.
3. With the **Copy Paste Gadget** in **Paste** mode, select **Load from .nbt** to choose a local structure file. External imports are deliberately rejected for Cut/Paste semantics.

The dialogs initially open in `.minecraft/building_gadgets_extra/structures`, but files may be saved to or loaded from any accessible folder. Transfers are chunked and size-limited, each save response is bound to the initiating request, and uploads are bound to the initiating gadget/profile. The server revalidates that the same gadget/profile is still active and still in Paste mode while chunks arrive and immediately before commit; changing gadget, profile, or mode aborts the pending import.

The files use Minecraft's normal compressed structure format and can also be used with vanilla Structure Blocks. Structure entities are not imported or exported. Exported server-owned templates may contain their normal block-entity NBT, but **external block-entity NBT is intentionally stripped on import** so an untrusted client file cannot replay inventories or arbitrary block-entity data on a server. On Building Gadgets 2 ports, imported block states are also passed through upstream validity/cleanup rules.

By default, imports are limited to a 100,000-position bounding volume, an 8 MiB compressed transfer, and a 64 MiB decoded-NBT budget. The bounding-volume limit counts every coordinate inside the declared structure dimensions, including air. These limits protect the server thread from malformed or excessively expensive structure files.

## Building from Source

Clone the repository and run:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The root build assembles every supported version and runs the Java 25 / Gradle 9 build for 26.1.2 automatically. Compiled JARs are written to each version module's `build/libs` directory.

Run shared and module unit/contract tests with:

```bash
./gradlew test
```

The maintained BG2 ports also have in-game GameTest coverage. CI runs Forge 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2 GameTest servers in addition to normal builds. These runtime tests cover multitool profile defaults, range clamping, creative zero-FE behavior, and version-specific gadget operations.

To build one module:

```bash
cd neoforge-26.1.2 && ./gradlew build
./gradlew :neoforge-1.21.1:build
./gradlew :forge-1.20.1:build
./gradlew :forge-1.16.5:build
```

To run a development client for one version:

```bash
cd neoforge-26.1.2 && ./gradlew runClient
./gradlew :neoforge-1.21.1:runClient
./gradlew :forge-1.20.1:runClient
./gradlew :forge-1.16.5:runClient
```

## Project Structure

- `common` contains shared validation, transfer rules, translations, assets, and contract tests.
- `neoforge-26.1.2` contains the Minecraft 26.1.2 NeoForge adapters and its Gradle 9 wrapper required for Java 25.
- `neoforge-1.21.1` contains the Minecraft 1.21.1 NeoForge adapters.
- `forge-1.20.1` contains the Minecraft 1.20.1 Forge and Building Gadgets 2 adapters.
- `forge-1.16.5` contains the Minecraft 1.16.5 Forge and legacy Building Gadgets template-capability adapters.
- Root Gradle files contain project-wide identity, version, author, shared test configuration, and aggregate build configuration.

## Releases

Pull requests and ordinary pushes to `main` still run CI. The Release workflow also performs a cheap version gate on each `main` push, but the expensive build/publish path runs only when a release is actually needed.

A release is needed when `mod_version` differs from the previous `main` revision. If a version bump was merged but never published, a later `main` push also recovers it when the matching `v<mod_version>` tag is still missing. The Release workflow can be dispatched manually from `main` as a final recovery path.

Before publishing:

1. Update `mod_version` in `gradle.properties` and add or update `changelog/<mod_version>.md` (for example, [0.0.4](changelog/0.0.4.md)).
2. Merge the version bump to `main`.
3. The Release workflow detects the version change, runs the dedicated client E2E gate, reruns unit/contract tests, rebuilds all four supported JARs, and verifies the expected artifacts.
4. The workflow publishes all four builds to CurseForge, creates or reuses the matching `v<mod_version>` tag, and publishes the GitHub release using the version changelog.

No manual tag creation is required for a normal release. If a release job is retried, an existing tag for the same commit is reused and duplicate CurseForge uploads are treated as already published instead of creating another release file. An unchanged version with an existing tag exits at the cheap gate without rebuilding or publishing anything.

## License

All Rights Reserved.

### Copying more than 100,000 positions

Set `maxCopyVolume = 1000000` in the world's `serverconfig/buildinggadgetsextra-server.toml` to allow up to 1,000,000 positions. The default remains a 100,000-position bounding volume; air still counts because Building Gadgets stores every position in the selection. This setting applies to the native BG2 Copy/Paste gadget, the Builder's Multitool Copy/Paste profile, and Extra's native structure imports. It is a server/world setting, not a client option. Restart the server/world after editing it.

The configurable range is 1–1,000,000. Larger copies use more memory and may pause copying or rendering. Existing compressed-transfer and decoded-NBT budgets, upstream axis limits, and Cut/Paste limits still apply. Sparse or air-only selections also consume the volume budget.

Forge 1.16.5 already supports larger world copies through Building Gadgets' `Max Copy Dimensions` server setting (and its operator-controlled `OverrideCopySize` command); Extra's `maxCopyVolume` controls native structure imports on that version.

Large BG2 copy previews are synchronized in bounded chunks rather than one oversized packet. Preview transfers are limited to 8 MiB of serialized NBT and a 64 MiB decoded-NBT budget. Update Extra on both the server and all clients together; 0.0.5 uses a new preview-transfer protocol.

### Live Build/Exchange mirroring (0.0.6)

On Forge 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2, select the Builder's Multitool Building or Exchanging profile and use **Horizontal Mirror** or **Vertical Mirror** in its radial menu. Each toggle stays with that profile. Preview and placement use the same mirrored candidates, with directional block states transformed per copy. Horizontal mirroring follows the facing axis; vertical mirroring uses the operation anchor. Existing candidates take precedence when copies overlap.

Interactive plans are limited to 65,536 positions. Since 0.0.7, an oversized or overflowing plan rejects the operation, and live mirroring is also available on Forge 1.16.5. Update Extra on both server and clients together for matching protocols.

### Composable live modifiers (0.0.7)

Select the Builder's Multitool Building or Exchanging profile. Its settings area contains Range, the existing profile options, and a **Modifiers** group. Horizontal/Vertical Mirror, Array and Radial have visible On/Off states. The separate Array and Radial icons (or disclosure buttons) expand their numeric controls inline; they do not open another screen.

The profile toolbar, Range, mode wheel and modifier controls use one flat visual style. Total copies and compact axis fields keep the expanded forms readable at larger GUI scales.

Array repeats with signed X/Y/Z spacing in blocks. Radial repeats around the vertical Y axis with center offsets relative to the operation origin. Counts include the original. Enabled counts are 2-16; spacing and center offsets are bounded to -64..64. Turning a stage off preserves its configured count and offsets. Each expanded stage has its own reset. Composition order is shown only when multiple stages are enabled and lists only those stages.

Valid numeric edits save on Enter or leaving the field. Toggles save immediately. The server confirms the active profile's settings; there is no extra Apply/Cancel screen. Invalid fields show a specific error. Scrolling and resizing preserve edits; settings do not overlap the operation wheel at smaller GUI sizes. Build and Exchange settings are independent. Copy/Paste, Cut/Paste, Destruction and native gadgets keep their existing workflows.

Radial geometry rounds to blocks and directional states rotate to the nearest supported quarter turn. Overlaps keep the first candidate and are charged once. Preview and placement share the common planner. Plans above 65,536 positions or coordinate overflow are rejected with feedback; normal inventory, FE, world and protection rules still apply. All four versions support live modifiers; update clients and servers together.

## Building with pinned dependencies

Clone with `git clone --recurse-submodules` (or initialize an existing clone with
`git submodule update --init --recursive`). Before building a native target, run:

```
python scripts/prepare_dependencies.py --target forge-1.20.1
```

Supported target names are `forge-1.16.5`, `forge-1.20.1`, `neoforge-1.21.1` and
`neoforge-26.1.2`. The script builds standalone PacketWeave and publishes the pinned
OpenUI artifact to Maven local. Use Java 21 for the older build tools and Java 25 for
26.x; `JAVA21_HOME` / `JAVA25_HOME` can select those installations. The gadget jar
contains neither dependency's classes. CI prepares the same pinned revisions.

Build/Exchange use one shared OpenUI control tree for their wheel, native settings,
range and inline modifiers. Minecraft adapters retain packet and item access only.
PacketWeave handles chunking, bounded assembly, expiry and byte accounting; gadget
identity, active-profile checks, response correlation and local save paths remain
application policy.
