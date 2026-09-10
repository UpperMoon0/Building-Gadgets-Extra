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

The mod must be installed on the server and on every connecting client.

## Installation

1. Install the Forge or NeoForge version listed for your Minecraft version above.
2. Install the matching Building Gadgets version.
3. Place the matching `building-gadgets-extra-<loader>-<minecraft>-<version>.jar` in the `mods` folder on both the client and server.

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

Imports are currently limited to a 100,000-position bounding volume, an 8 MiB compressed transfer, and a 64 MiB decoded-NBT budget. The bounding-volume limit counts every coordinate inside the declared structure dimensions, including air. These limits protect the server thread from malformed or excessively expensive structure files.

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

Verification and publishing are separate workflows. Pull requests and pushes to `main` run CI; they do **not** publish a release.

A release is started only by either:

- pushing a `v<mod_version>` tag, for example `v0.0.4`; or
- explicitly running the Release workflow from the **Actions** tab on `main`.

Before publishing:

1. Update `mod_version` in `gradle.properties` and add or update `changelog/<mod_version>.md` (for example, [0.0.4](changelog/0.0.4.md)).
2. Merge the changes to `main` and let CI pass on that exact commit.
3. Create the matching `v<mod_version>` tag on the current `main` commit, or run the Release workflow manually from `main`.

The release workflow rejects a tag or manual dispatch whose checked-out commit is not the current `origin/main`, rejects tags that do not exactly match `mod_version`, and requires at least one successful CI run for the exact release commit. It then reruns the unit/contract tests, rebuilds all four release JARs, verifies the expected artifacts exist, and only then publishes the GitHub release.

## License

All Rights Reserved.
