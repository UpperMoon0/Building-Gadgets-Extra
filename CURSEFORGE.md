# Building Gadgets Extra

Building Gadgets Extra is an addon for **Building Gadgets** and **Building Gadgets 2** that gives builders more freedom and makes copied builds easier to reuse.

Everything fits into the familiar Building Gadgets workflow, so the extra controls stay close to the tools you already use.

## Current Features

### Template Mirroring

- Flip a copied build left-to-right with **Mirror Horizontal**.
- Turn it upside down with **Mirror Vertical**.
- Use mirroring with the **Copy Paste Gadget** and, where available, the **Cut Paste Gadget**.
- Find the mirror buttons directly in the radial menu whenever the gadget is ready to paste.
- Stairs, doors, chests, and other directional blocks turn with the mirrored build.

### Save and Reuse Your Builds

- Save the build held by your gadget as a standard Minecraft structure file on your computer.
- Load a saved build into the **Copy Paste Gadget** whenever you are in Paste mode.
- Choose the file with your normal Windows, macOS, or Linux file picker—no commands or extra menus required.
- Take builds from one singleplayer world or multiplayer server to another.
- Keep saved builds in the default `.minecraft/building_gadgets_extra/structures` folder, or choose another folder.
- Preserve block geometry and orientation through the structure-file workflow.

Saved builds use Minecraft's standard `.nbt` structure format, so they can also be used with vanilla Structure Blocks. Mobs and other entities are not included.

For multiplayer safety, external structure files can only be imported into the Copy Paste Gadget while it remains in Paste mode. Uploads stay bound to the initiating gadget/profile and are revalidated through final commit, so switching gadget, multitool profile, or mode aborts the pending import. Imported block-entity NBT is deliberately stripped, so inventories and other arbitrary block-entity contents from a local file are **not** restored onto a server. Building Gadgets 2 ports also sanitize imported block states through upstream validation.

Imports are limited to a 100,000-position bounding volume, an 8 MiB compressed transfer, and a 64 MiB decoded-NBT budget. The volume includes air inside the declared dimensions. Concurrent transfers are also capped per player.

### Builder's Multitool

On Building Gadgets 2 versions, the Builder's Multitool combines the supported gadget roles into one physical tool. Each virtual profile keeps its own mode, range/settings, template identity and undo history, while the tool shares one FE battery. Bound inventory position and side are profile-local as well, and energy use follows the currently active gadget profile.

## Multiplayer

Building Gadgets Extra works in singleplayer and multiplayer. For multiplayer, install it on the server and on each player's client. Every player keeps their own saved-build collection on their computer, while gadget/template changes remain server-authoritative.

## Supported Versions

- Minecraft 26.1.2 with NeoForge and Building Gadgets 2 1.4.6
- Minecraft 1.21.1 with NeoForge and Building Gadgets 2 1.3.9
- Minecraft 1.20.1 with Forge and Building Gadgets 2 1.0.8
- Minecraft 1.16.5 with Forge and Building Gadgets 3.8.4
