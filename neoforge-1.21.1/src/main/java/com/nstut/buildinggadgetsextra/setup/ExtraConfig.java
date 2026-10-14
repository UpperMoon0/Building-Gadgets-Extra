package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExtraConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_COPY_VOLUME;
    public static final ModConfigSpec.IntValue MULTITOOL_MAX_RANGE;
    public static final ModConfigSpec.DoubleValue MULTITOOL_RANGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue DEBUG_INSTRUMENTATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MAX_COPY_VOLUME = builder
                .comment("Maximum Copy/Paste selection and native structure import volume, including air. Applies to native BG2 gadgets and the Builder's Multitool.")
                .comment("Forge 1.16.5 world copying uses Building Gadgets' own Max Copy Dimensions setting. Cut/Paste limits are separate.")
                .comment("Larger templates use more server memory and can pause copying/rendering. Existing byte budgets still apply.")
                .defineInRange("maxCopyVolume", (int) ExtraConstants.MAX_STRUCTURE_BLOCKS, 1, ExtraConstants.HARD_MAX_COPY_VOLUME);
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

    public static int maxCopyVolume() {
        return Math.max(1, Math.min(ExtraConstants.HARD_MAX_COPY_VOLUME, MAX_COPY_VOLUME.get()));
    }

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
