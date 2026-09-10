package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ExtraConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MULTITOOL_MAX_RANGE;
    public static final ForgeConfigSpec.DoubleValue MULTITOOL_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue DEBUG_INSTRUMENTATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MULTITOOL_MAX_RANGE = builder
                .comment("Maximum Build/Exchange range for the Builder's Multitool. Native Building Gadgets keep their own range limit.")
                .comment("0 follows multitoolRangeMultiplier; positive values override the Build/Exchange cap.")
                .defineInRange("multitoolMaxRange", MultitoolRangePolicy.DEFAULT_MAX_RANGE, 0, MultitoolRangePolicy.HARD_MAX_RANGE);
        MULTITOOL_RANGE_MULTIPLIER = builder
                .comment("Multitool range multiplier for targeting (all profiles), Build To Me, Build/Exchange and destruction dimensions. Template size and transfer limits are separate.")
                .defineInRange("multitoolRangeMultiplier", MultitoolRangePolicy.DEFAULT_RANGE_MULTIPLIER, 1.0, 4.0);
        DEBUG_INSTRUMENTATION = builder
                .comment("Emit detailed, rate-limited Builder's Multitool diagnostic instrumentation. Disabled by default.")
                .define("debugInstrumentation", false);
        SPEC = builder.build();
    }

    private ExtraConfig() {}

    public static int multitoolMaxRange() {
        int override = MULTITOOL_MAX_RANGE.get();
        int nativeRange = com.direwolf20.buildinggadgets.common.config.Config.GADGETS.maxRange.get();
        return MultitoolRangePolicy.clamp(override > 0 ? override
                : MultitoolRangePolicy.scaledLimit(nativeRange, MULTITOOL_RANGE_MULTIPLIER.get()),
                MultitoolRangePolicy.HARD_MAX_RANGE);
    }

    public static double multitoolReach(double nativeReach) {
        return MultitoolRangePolicy.scaledReach(nativeReach, MULTITOOL_RANGE_MULTIPLIER.get());
    }

    public static int multitoolDestructionSide() {
        return MultitoolRangePolicy.scaledLimit(com.direwolf20.buildinggadgets.common.config.Config.GADGETS.GADGET_DESTRUCTION.destroySize.get(),
                MULTITOOL_RANGE_MULTIPLIER.get());
    }

    public static int multitoolDestructionSpan() {
        return multitoolDestructionSide();
    }

    public static boolean debugInstrumentation() {
        return DEBUG_INSTRUMENTATION.get();
    }
}
