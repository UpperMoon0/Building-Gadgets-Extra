# Building Gadgets Extra

Building Gadgets Extra is a growing Forge and NeoForge addon for [Building Gadgets](https://www.curseforge.com/minecraft/mc-mods/building-gadgets). It expands the original mod with new tools, controls, and quality-of-life features designed to make large building projects easier and more flexible.

Its current features add horizontal and vertical template mirroring plus a player-owned library for moving native Minecraft structure files between worlds and multiplayer servers. More Building Gadgets enhancements are planned for future releases.

## Current Features

- Mirror Copy Paste Gadget templates horizontally.
- Mirror Copy Paste Gadget templates vertically.
- Support the Cut Paste Gadget using the same mirror controls.
- Preserve block orientation, block-entity positions, and copied template data.
- Perform mirror operations on the server for dedicated-server and multiplayer compatibility.
- Integrate directly into the existing Building Gadgets 2 radial menu.
- Save a Copy Paste or Cut Paste Gadget selection as a native vanilla structure `.nbt` file.
- Load vanilla structure files back into a gadget, including block states and block-entity data.
- Store exported structures in a player-owned library on the client computer rather than in the server world.
- Transfer structures safely between the client and server while keeping gadget changes server-authoritative.
- Use Minecraft's native Structure Template and compressed NBT logic instead of a custom file parser.

## Project Direction

Building Gadgets Extra is intended to grow beyond mirroring. Future versions will continue expanding Building Gadgets 2 while preserving its familiar workflow and visual style. New features will be documented here as they are added.

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

### Native Structure Files

1. Hold a Copy Paste Gadget or Cut Paste Gadget and open its radial menu.
2. In **Copy** or **Cut** mode, select **Save to .nbt** to immediately open your operating system's Save dialog and download the gadget's current selection.
3. With the **Copy Paste Gadget** in **Paste** mode, select **Load from .nbt** to immediately open a native file picker and upload a local structure into the held gadget. The Cut Paste Gadget never shows Load because it can only paste its own cut selection.

Only the action relevant to the current gadget mode is shown; there is no intermediate submenu. The dialogs initially open in `.minecraft/building_gadgets_extra/structures`, but files may be saved to or loaded from any accessible folder. You can save a build while playing on one server or world, then load it into a gadget somewhere else. Saving downloads the current server-authoritative gadget template to the chosen path; loading uploads the selected local file to the server and applies it to the held gadget after server-side validation. Transfers are chunked, size-limited, and work on dedicated multiplayer servers.

The files use the normal compressed vanilla structure format, so they can be copied to or from a world's `generated/<namespace>/structures` folder for use with Structure Blocks. Structure entities are intentionally not imported or exported; blocks, orientation, and block-entity NBT are preserved.

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

Run all shared and module test suites with:

```bash
./gradlew test
```

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

- `common` contains Java 8-compatible mirror traversal, coordinate and structure-name rules, translation keys, and shared assets used by every module.
- `neoforge-26.1.2` contains the Minecraft 26.1.2 NeoForge adapters and its Gradle 9 wrapper required for Java 25.
- `neoforge-1.21.1` contains the Minecraft 1.21.1 NeoForge adapters.
- `forge-1.20.1` contains the Minecraft 1.20.1 Forge and Building Gadgets 2 adapters.
- `forge-1.16.5` contains the Minecraft 1.16.5 Forge and legacy Building Gadgets template-capability adapters.
- Root Gradle files contain project-wide identity, version, author, shared test configuration, and aggregate build configuration.

## Releases

Every push to `main` runs the GitHub Actions release workflow. It first runs the shared logic tests and compiles every supported module. Only after every test and build succeeds does it create a `v<mod_version>` tag and attach all four compiled JARs to a GitHub Release.

To publish a version:

1. Update `mod_version` in `gradle.properties`.
2. Push the change to `main`.

Each released commit needs a new version. If its version tag already belongs to an older commit, the workflow stops and asks for `mod_version` to be bumped. The workflow can also be started manually from the repository's **Actions** tab.

## License

All Rights Reserved.
