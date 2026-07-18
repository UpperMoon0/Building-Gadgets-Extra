# Building Gadgets Extra

Building Gadgets Extra is a NeoForge addon for [Building Gadgets 2](https://www.curseforge.com/minecraft/mc-mods/building-gadgets) that adds horizontal and vertical mirroring to copied structures.

## Features

- Mirror Copy Paste Gadget templates horizontally.
- Mirror Copy Paste Gadget templates vertically.
- Support the Cut Paste Gadget using the same mirror controls.
- Preserve block orientation, block-entity positions, and copied template data.
- Perform mirror operations on the server for dedicated-server and multiplayer compatibility.
- Integrate directly into the existing Building Gadgets 2 radial menu.

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.240 or newer |
| Building Gadgets 2 | 1.3.9 |
| Java | 21 |

The mod must be installed on the server and on every connecting client.

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Install Building Gadgets 2 v1.3.9.
3. Place `buildinggadgetsextra-0.0.1.jar` in the `mods` folder on both the client and server.

## Usage

1. Copy a structure with the Copy Paste Gadget or Cut Paste Gadget.
2. Open the gadget's radial menu.
3. Use one of the two mirror buttons beside the existing Rotate button:
   - **Mirror Horizontal** reflects the structure left-to-right relative to the direction the player is facing.
   - **Mirror Vertical** reflects the structure up-to-down.
4. Preview and paste the transformed structure normally.

## Building from Source

Clone the repository and run:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The compiled JAR is written to `build/libs`.

## Releases

Every push to `main` runs the GitHub Actions release workflow. It builds the project with Java 21, creates a `v<mod_version>` tag at that commit, and attaches the compiled JAR to a GitHub Release.

To publish a version:

1. Update `mod_version` in `gradle.properties`.
2. Push the change to `main`.

Each released commit needs a new version. If its version tag already belongs to an older commit, the workflow stops and asks for `mod_version` to be bumped. The workflow can also be started manually from the repository's **Actions** tab.

## License

All Rights Reserved.
