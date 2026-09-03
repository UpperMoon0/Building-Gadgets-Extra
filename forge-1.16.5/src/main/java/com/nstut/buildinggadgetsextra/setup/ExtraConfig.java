package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ExtraConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MULTITOOL_MAX_RANGE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_INSTRUMENTATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MULTITOOL_MAX_RANGE = builder
                .comment("Maximum Build/Exchange range for the Builder's Multitool. Native Building Gadgets keep their own range limit.")
                .defineInRange("multitoolMaxRange", MultitoolRangePolicy.DEFAULT_MAX_RANGE,
                        MultitoolRangePolicy.MIN_RANGE, MultitoolRangePolicy.HARD_MAX_RANGE);
        DEBUG_INSTRUMENTATION = builder
                .comment("Emit detailed, rate-limited Builder's Multitool diagnostic instrumentation. Disabled by default.")
                .define("debugInstrumentation", false);
        SPEC = builder.build();
    }

    private ExtraConfig() {}

    public static int multitoolMaxRange() {
        return MultitoolRangePolicy.clamp(MULTITOOL_MAX_RANGE.get(), MultitoolRangePolicy.HARD_MAX_RANGE);
    }

    public static boolean debugInstrumentation() {
        return DEBUG_INSTRUMENTATION.get();
    }
}
