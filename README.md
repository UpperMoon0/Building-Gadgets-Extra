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
- Use Minecraft's own Structure Template Manager for compressed NBT parsing and data-version updates.

## Project Direction

Building Gadgets Extra is intended to grow beyond mirroring. Future versions will continue expanding Building Gadgets 2 while preserving its familiar workflow and visual style. New features will be documented here as they are added.

## Requirements

| Minecraft | Loader | Building Gadgets | Java |
| --- | --- | --- | --- |
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
2. Open the gadget's radial menu.
3. Use one of the two mirror buttons beside the existing Rotate button:
   - **Mirror Horizontal** reflects the structure left-to-right relative to the direction the player is facing.
   - **Mirror Vertical** reflects the structure up-to-down.
4. Preview and paste the transformed structure normally.

### Native Structure Library

1. Hold a Copy Paste Gadget or Cut Paste Gadget and open its radial menu.
2. Select the **NBT** Structure Library button.
3. Enter a lowercase structure name. Subfolders are supported, for example `castle/gatehouse`.
4. Choose **Save to .nbt** to download the gadget's current selection to your computer, or **Load from .nbt** to upload a local structure and replace the gadget's selection.

Structures are stored on each player's computer under `.minecraft/building_gadgets_extra/structures`. This makes the library portable: you can save a build while playing on one server or world, then load it into a gadget somewhere else. Saving downloads the current server-authoritative gadget template to that local library; loading uploads the selected local file to the server and applies it to the held gadget after server-side validation. Transfers are chunked, size-limited, and work on dedicated multiplayer servers.

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

The root build assembles every supported version. Compiled JARs are written to each version module's `build/libs` directory.

To build one module:

```bash
./gradlew :neoforge-1.21.1:build
./gradlew :forge-1.20.1:build
./gradlew :forge-1.16.5:build
```

To run a development client for one version:

```bash
./gradlew :neoforge-1.21.1:runClient
./gradlew :forge-1.20.1:runClient
./gradlew :forge-1.16.5:runClient
```

## Project Structure

- `common` contains Java 8-compatible mirror traversal, coordinate and structure-name rules, translation keys, and shared assets used by every module.
- `neoforge-1.21.1` contains the Minecraft 1.21.1 NeoForge adapters.
- `forge-1.20.1` contains the Minecraft 1.20.1 Forge and Building Gadgets 2 adapters.
- `forge-1.16.5` contains the Minecraft 1.16.5 Forge and legacy Building Gadgets template-capability adapters.
- Root Gradle files contain project-wide identity, version, author, and aggregate build configuration.

## Releases

Every push to `main` runs the GitHub Actions release workflow. It builds all supported modules, creates a `v<mod_version>` tag at that commit, and attaches all three compiled JARs to a GitHub Release.

To publish a version:

1. Update `mod_version` in `gradle.properties`.
2. Push the change to `main`.

Each released commit needs a new version. If its version tag already belongs to an older commit, the workflow stops and asks for `mod_version` to be bumped. The workflow can also be started manually from the repository's **Actions** tab.

## License

All Rights Reserved.
