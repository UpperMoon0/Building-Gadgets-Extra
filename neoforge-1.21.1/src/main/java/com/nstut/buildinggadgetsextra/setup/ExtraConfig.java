package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExtraConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MULTITOOL_MAX_RANGE;
    public static final ModConfigSpec.DoubleValue MULTITOOL_RANGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue DEBUG_INSTRUMENTATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MULTITOOL_MAX_RANGE = builder
                .comment("Maximum Build/Exchange range for the Builder's Multitool. Native Building Gadgets 2 gadgets keep their own range limit.")
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
        int nativeRange = 15;
        return MultitoolRangePolicy.clamp(override > 0 ? override
                : MultitoolRangePolicy.scaledLimit(nativeRange, MULTITOOL_RANGE_MULTIPLIER.get()),
                MultitoolRangePolicy.HARD_MAX_RANGE);
    }

    public static double multitoolReach(double nativeReach) {
        return MultitoolRangePolicy.scaledReach(nativeReach, MULTITOOL_RANGE_MULTIPLIER.get());
    }

    public static int multitoolDestructionSide() {
        return MultitoolRangePolicy.scaledLimit(16,
                MULTITOOL_RANGE_MULTIPLIER.get());
    }

    public static int multitoolDestructionSpan() {
        return MultitoolRangePolicy.scaledLimit(32, MULTITOOL_RANGE_MULTIPLIER.get());
    }

    public static boolean debugInstrumentation() {
        return DEBUG_INSTRUMENTATION.get();
    }
}
