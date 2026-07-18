# Building Gadgets Extra

Building Gadgets Extra is a growing addon for **Building Gadgets** and **Building Gadgets 2** that expands their familiar building tools with new controls, portable structure workflows, and quality-of-life improvements.

The addon is designed for both detailed builds and large construction projects while preserving the normal Building Gadgets experience. Its features integrate directly into the existing gadget radial menu and work in singleplayer and multiplayer.

## Current Features

### Template Mirroring

- **Mirror Horizontal** reflects a copied structure left-to-right relative to the direction you are facing.
- **Mirror Vertical** flips a copied structure from top to bottom.
- Works with the **Copy Paste Gadget** and, where available, the **Cut Paste Gadget**.
- Mirror controls appear only while a gadget is in Paste mode.
- Transforms block orientation and block-entity positions with the structure.
- Runs server-side so previews and pasted results remain authoritative in multiplayer.

### Player-Owned Structure Files

- Shows **Save to .nbt** directly in Copy/Cut mode.
- Shows **Load from .nbt** in Paste mode only for the Copy Paste Gadget; the Cut Paste Gadget can only paste its own cut selection.
- Opens the matching native operating-system dialog immediately, without an intermediate menu.
- Open your operating system's native Save dialog to export the gadget's current selection as a compressed Minecraft structure `.nbt` file.
- Open a native file picker to load a local vanilla structure file into the held gadget.
- Save structures anywhere on your own computer, with `.minecraft/building_gadgets_extra/structures` used as the default library folder instead of storing files inside a server world.
- Carry saved builds between singleplayer worlds and different multiplayer servers.
- Transfer files between client and server in validated, size-limited chunks.
- Use Minecraft's vanilla Structure Template and compressed NBT logic rather than a custom structure parser or format.
- Preserve block states, orientation, and block-entity NBT.

The files are compatible with Minecraft's native structure format. They can be copied to or from a world's `generated/<namespace>/structures` folder for use with Structure Blocks. Structure entities are intentionally not imported or exported.

## Multiplayer

Building Gadgets Extra must be installed on the server and on every connecting client. Gadget templates remain server-authoritative, while each player's saved `.nbt` library stays on their own computer.

Mirroring and native structure transfer are only the beginning. Future releases will continue expanding Building Gadgets with additional tools, controls, and building-focused improvements.

## Supported Versions

- Minecraft 26.1.2 with NeoForge and Building Gadgets 2 1.4.6
- Minecraft 1.21.1 with NeoForge and Building Gadgets 2 1.3.9
- Minecraft 1.20.1 with Forge and Building Gadgets 2 1.0.8
- Minecraft 1.16.5 with Forge and Building Gadgets 3.8.4
