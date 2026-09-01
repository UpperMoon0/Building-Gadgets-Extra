package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.setup.BG2DataComponents;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class MultitoolState {
    private static final String ACTIVE_MODE = "BGEActiveTool";
    private static final String PROFILE_PREFIX = "BGEProfileMode_";
    private static final String TEMPLATE_PREFIX = "BGETemplateProfile_";
    private static final String STATE_PREFIX = "BGEStateProfile_";
    private static final String UNDO_PREFIX = "BGEUndoProfile_";
    private static final String UUID_PREFIX = "BGEGadgetProfile_";

    private MultitoolState() {
    }

    public static MultitoolMode getActiveMode(ItemStack stack) {
        return MultitoolMode.parse(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(ACTIVE_MODE, ""));
    }

    public static void setActiveMode(ItemStack stack, MultitoolMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(ACTIVE_MODE, mode.serializedName()));
    }

    public static Identifier getProfileMode(ItemStack stack, MultitoolMode mode) {
        String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(PROFILE_PREFIX + mode.serializedName(), "");
        try { return value.isEmpty() ? null : Identifier.parse(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static void setProfileMode(ItemStack stack, MultitoolMode mode, Identifier value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(PROFILE_PREFIX + mode.serializedName(), value.toString()));
    }

    public static void saveTemplateProfile(ItemStack stack, MultitoolMode mode) {
        saveGeneralProfile(stack, mode);
        saveGadgetUuidProfile(stack, mode);
        saveUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag profile = new CompoundTag();
        profile.putString("GadgetId", GadgetNBT.getUUID(stack).toString());
        if (GadgetNBT.hasCopyUUID(stack)) profile.putString("CopyId", GadgetNBT.getCopyUUID(stack).toString());
        profile.putLong("Start", GadgetNBT.getCopyStartPos(stack).asLong());
        profile.putLong("End", GadgetNBT.getCopyEndPos(stack).asLong());
        profile.putLong("Relative", GadgetNBT.getRelativePaste(stack).asLong());
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(TEMPLATE_PREFIX + mode.serializedName(), profile));
    }

    public static void restoreTemplateProfile(ItemStack stack, MultitoolMode mode) {
        restoreGeneralProfile(stack, mode);
        restoreGadgetUuidProfile(stack, mode);
        restoreUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag profile = root.getCompoundOrEmpty(TEMPLATE_PREFIX + mode.serializedName());
        if (profile.isEmpty()) {
            GadgetNBT.clearCopyUUID(stack);
            GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setRelativePaste(stack, BlockPos.ZERO);
            return;
        }
        String copyId = profile.getStringOr("CopyId", "");
        if (!copyId.isEmpty()) stack.set(BG2DataComponents.COPY_UUID, UUID.fromString(copyId));
        else GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, BlockPos.of(profile.getLongOr("Start", GadgetNBT.nullPos.asLong())));
        GadgetNBT.setCopyEndPos(stack, BlockPos.of(profile.getLongOr("End", GadgetNBT.nullPos.asLong())));
        GadgetNBT.setRelativePaste(stack, BlockPos.of(profile.getLongOr("Relative", 0L)));
    }

    private static void saveGeneralProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag profile = new CompoundTag();
        profile.putBoolean("Initialized", true);

        GlobalPos bound = GadgetNBT.getBoundPos(stack);
        if (bound != null) {
            profile.putString("BoundDimension", bound.dimension().identifier().toString());
            profile.putLong("BoundPos", bound.pos().asLong());
        }

        BlockPos anchor = GadgetNBT.getAnchorPos(stack);
        if (!GadgetNBT.nullPos.equals(anchor)) profile.putLong("Anchor", anchor.asLong());
        List<BlockPos> anchors = GadgetNBT.getAnchorList(stack);
        profile.putInt("AnchorListSize", anchors.size());
        for (int i = 0; i < anchors.size(); i++) profile.putLong("AnchorList" + i, anchors.get(i).asLong());
        Direction side = GadgetNBT.getAnchorSide(stack);
        profile.putInt("AnchorSide", side == null ? -1 : side.ordinal());

        profile.putByte("RenderType", GadgetNBT.getRenderTypeByte(stack));
        profile.put("BlockState", NbtUtils.writeBlockState(GadgetNBT.getGadgetBlockState(stack)));
        profile.putInt("Range", GadgetNBT.getToolRange(stack));
        profile.putString("TemplateName", GadgetNBT.getTemplateName(stack));

        for (GadgetNBT.ToggleableSettings setting : GadgetNBT.ToggleableSettings.values()) {
            profile.putBoolean("Toggle_" + setting.getName(), GadgetNBT.getSetting(stack, setting.getName()));
        }
        for (GadgetNBT.IntSettings setting : GadgetNBT.IntSettings.values()) {
            profile.putInt("Value_" + setting.getName(), GadgetNBT.getToolValue(stack, setting.getName()));
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(STATE_PREFIX + mode.serializedName(), profile));
    }

    private static void restoreGeneralProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag profile = root.getCompoundOrEmpty(STATE_PREFIX + mode.serializedName());
        boolean initialized = profile.getBooleanOr("Initialized", false);

        if (initialized) {
            String dimensionName = profile.getStringOr("BoundDimension", "");
            if (!dimensionName.isEmpty() && profile.contains("BoundPos")) {
                try {
                    ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionName));
                    GadgetNBT.setBoundPos(stack,
                            new GlobalPos(dimension, BlockPos.of(profile.getLongOr("BoundPos", 0L))));
                } catch (IllegalArgumentException error) {
                    GadgetNBT.clearBoundPos(stack);
                }
            } else {
                GadgetNBT.clearBoundPos(stack);
            }
        } else {
            GadgetNBT.clearBoundPos(stack);
        }

        GadgetNBT.clearAnchorPos(stack);
        if (initialized && profile.contains("Anchor")) {
            GadgetNBT.setAnchorPos(stack, BlockPos.of(profile.getLongOr("Anchor", GadgetNBT.nullPos.asLong())));
        }
        ArrayList<BlockPos> anchors = new ArrayList<>();
        int anchorCount = initialized ? profile.getIntOr("AnchorListSize", 0) : 0;
        for (int i = 0; i < anchorCount; i++) {
            anchors.add(BlockPos.of(profile.getLongOr("AnchorList" + i, 0L)));
        }
        GadgetNBT.setAnchorList(stack, anchors);
        int side = initialized ? profile.getIntOr("AnchorSide", -1) : -1;
        GadgetNBT.setAnchorSide(stack, side >= 0 && side < Direction.values().length ? Direction.values()[side] : null);

        GadgetNBT.setRenderType(stack, initialized ? profile.getByteOr("RenderType", (byte) 0) : (byte) 0);
        if (initialized && profile.contains("BlockState")) {
            GadgetNBT.setGadgetBlockState(stack,
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK, profile.getCompoundOrEmpty("BlockState")));
        } else {
            GadgetNBT.setGadgetBlockState(stack, Blocks.AIR.defaultBlockState());
        }
        GadgetNBT.setToolRange(stack, initialized ? profile.getIntOr("Range", 1) : 1);
        GadgetNBT.setTemplateName(stack, initialized ? profile.getStringOr("TemplateName", "") : "");

        for (GadgetNBT.ToggleableSettings setting : GadgetNBT.ToggleableSettings.values()) {
            boolean desired = initialized
                    ? profile.getBooleanOr("Toggle_" + setting.getName(), false)
                    : mode == MultitoolMode.CUT_PASTE && setting == GadgetNBT.ToggleableSettings.PASTE_REPLACE;
            if (GadgetNBT.getSetting(stack, setting.getName()) != desired) {
                GadgetNBT.toggleSetting(stack, setting.getName());
            }
        }
        for (GadgetNBT.IntSettings setting : GadgetNBT.IntSettings.values()) {
            GadgetNBT.setToolValue(stack,
                    initialized ? profile.getIntOr("Value_" + setting.getName(), 0) : 0,
                    setting.getName());
        }
    }

    private static void saveGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        UUID uuid = GadgetNBT.getUUID(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(UUID_PREFIX + mode.serializedName(), uuid.toString()));
    }

    private static void restoreGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String key = UUID_PREFIX + mode.serializedName();
        UUID uuid = parseUuid(root.getStringOr(key, ""));
        if (uuid == null && (mode == MultitoolMode.COPY_PASTE || mode == MultitoolMode.CUT_PASTE)) {
            CompoundTag legacy = root.getCompoundOrEmpty(TEMPLATE_PREFIX + mode.serializedName());
            uuid = parseUuid(legacy.getStringOr("GadgetId", ""));
        }
        if (uuid == null) uuid = UUID.randomUUID();
        stack.set(BG2DataComponents.GADGET_UUID, uuid);
        UUID finalUuid = uuid;
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(key, finalUuid.toString()));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static void saveUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag saved = new CompoundTag();
        LinkedList<UUID> undo = GadgetNBT.getUndoList(stack);
        saved.putInt("Size", undo.size());
        for (int i = 0; i < undo.size(); i++) saved.putString("Entry" + i, undo.get(i).toString());
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(UNDO_PREFIX + mode.serializedName(), saved));
    }

    private static void restoreUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag saved = root.getCompoundOrEmpty(UNDO_PREFIX + mode.serializedName());
        LinkedList<UUID> undo = new LinkedList<>();
        int size = saved.getIntOr("Size", 0);
        for (int i = 0; i < size; i++) {
            UUID uuid = parseUuid(saved.getStringOr("Entry" + i, ""));
            if (uuid != null) undo.add(uuid);
        }
        GadgetNBT.setUndoList(stack, undo);
    }
}
