# Multitool range audit

Compared the addon adapters with Building Gadgets' `origin/1.16` source, Building Gadgets 2's `origin/1.20.1` and `1.21.1` source, and the actual dependency JARs (BG 3.8.4, BG2 1.0.8, 1.3.9, and 1.4.6).

## Findings

The previous `multitoolMaxRange` implementation only intercepted the Build/Exchange range getter, radial slider, hotkey, and range packet. It did not change targeting reach or destruction dimensions.

- All profiles target through `VectorHelper.getLookingAt`. This continued using the native targeting distance, normally 32 blocks.
- BG2 `BuildToMe.collectWorld` does not read the range slider. It independently clamps distance to a literal 32. BG1 `BuildToMeMode.collect` similarly uses the native targeting config.
- Other Build/Exchange shapes read the gadget range through `GadgetNBT.getToolRange` (BG2) or `GadgetUtils.getToolRange` / use context (BG1). The existing extended getter covers these modes, including row, column, wall, stairs, surface and grid where available.
- Destruction uses separate left/right/up/down/depth values. Its screen retained native limits on every port. The 1.20.1 addon packet bridge also explicitly rejected values above 16.
- BG1's destruction slider has two independent literal limits: the slider constructor and its plus/minus callback.
- Previous client integration checks used Build 7, Exchange 4 and destruction 1, so they could pass without crossing any native cap.
- The BG2 radial range hook targeted an integer 15, but the slider constructor takes a double maximum (15.0). The client integration test reproduced the slider stopping at 15. The owned radial screens now read the config directly, and the incorrect hooks were removed.

## Limits after the fix

These values assume default upstream configs, `multitoolRangeMultiplier=2.0`, and `multitoolMaxRange=0`.

| Operation | Normal gadget | Multitool | Ports |
| --- | --- | --- | --- |
| Targeting in all five profiles | 32 | 64 | All |
| Build/Exchange range setting | 15 | 30 | All |
| Build To Me distance cap | 32 | 64 | All |
| Destruction individual slider / depth | 16 | 32 | All |
| Destruction left+right / up+down | 16 | 32 | 1.16.5, 1.20.1 |
| Destruction left+right / up+down | 32 | 64 | 1.21.1, 26.1.2 |

Destruction width/height include one additional center block. Build To Me stops before the player's coordinate, and centered shapes retain upstream rounding; doubling the range setting does not mean exactly doubling the number of placed blocks in every shape. Grid also retains upstream spacing behavior.

Copy/Paste and Cut/Paste have no Build/Exchange range slider. They gain the longer targeting reach for selection and placement. Template dimensions, selection volume, energy, and structure-file limits are distinct from targeting distance and are not multiplied.

## Configuration and compatibility

The addon **server** config is normally `<world>/serverconfig/buildinggadgetsextra-server.toml`. Configure:

```toml
multitoolRangeMultiplier = 2.0
multitoolMaxRange = 0
```

The multiplier accepts 1.0–4.0. Zero for `multitoolMaxRange` selects automatic scaling; positive values 1–64 explicitly override only the Build/Exchange range cap. Existing positive values, including the previous default 32, are preserved. The absolute Build/Exchange safety ceiling remains 64.

Targeting scales the upstream targeting config. On BG1, automatic Build/Exchange range and destruction dimensions also follow their upstream configs. BG2 has literal native Build/Exchange and destruction caps.

Both client and server must run the updated addon. The multiplier does not modify ordinary gadgets or global upstream config values. Destruction packets validate the active multitool profile and configured bounds; getters also bound restored item data after a config reduction.

## Verification coverage

- Shared tests exercise scaling, version-specific destruction spans, negative inputs, and overflow.
- Forge 1.20.1 and NeoForge 1.21.1 GameTests exercise all five profiles' targeting reach, automatic Build/Exchange cap, Build To Me beyond 32, destruction values above 16, config reduction, and unchanged native gadgets.
- Real-client integration targets now cross native limits: Build 30, Exchange 29, destruction Left/Depth 20. The legacy adapter covers the Build range round trip.
- Version builds and existing contract tests check the adapters against their declared dependencies.
