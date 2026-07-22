package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.api.gadgets.GadgetTarget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets2.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class BuildersMultitool extends GadgetCopyPaste {
    @Override public int getEnergyMax() {
        return Math.max(Config.EXCHANGINGGADGET_MAXPOWER.get(), Math.max(Math.max(Config.BUILDINGGADGET_MAXPOWER.get(), Config.COPYPASTEGADGET_MAXPOWER.get()), Math.max(Config.CUTPASTEGADGET_MAXPOWER.get(), Config.DESTRUCTIONGADGET_MAXPOWER.get())));
    }

    @Override public int getEnergyCost() {
        return Math.max(Config.EXCHANGINGGADGET_COST.get(), Math.max(Math.max(Config.BUILDINGGADGET_COST.get(), Config.COPYPASTEGADGET_COST.get()), Math.max(Config.CUTPASTEGADGET_COST.get(), Config.DESTRUCTIONGADGET_COST.get())));
    }

    @Override public GadgetTarget gadgetTarget() { return GadgetTarget.COPYPASTE; }

    public GadgetTarget target(ItemStack stack) { return target(MultitoolState.getActiveMode(stack)); }

    public static GadgetTarget target(MultitoolMode mode) {
        return switch (mode) {
            case BUILD -> GadgetTarget.BUILDING;
            case EXCHANGING -> GadgetTarget.EXCHANGING;
            case COPY_PASTE -> GadgetTarget.COPYPASTE;
            case CUT_PASTE -> GadgetTarget.CUTPASTE;
            case DESTRUCTION -> GadgetTarget.DESTRUCTION;
        };
    }

    public void selectTool(ItemStack stack, MultitoolMode selected) {
        MultitoolMode previous = MultitoolState.getActiveMode(stack);
        BaseMode previousMode = GadgetNBT.getMode(stack);
        if (previousMode != null) MultitoolState.setProfileMode(stack, previous, previousMode.getId());
        MultitoolState.saveTemplateProfile(stack, previous);
        MultitoolState.setActiveMode(stack, selected);
        MultitoolState.restoreTemplateProfile(stack, selected);
        if (selected == MultitoolMode.DESTRUCTION) return;
        ResourceLocation saved = MultitoolState.getProfileMode(stack, selected);
        BaseMode next = GadgetModes.INSTANCE.getModesForGadget(target(selected)).stream()
                .filter(mode -> saved != null && mode.getId().equals(saved)).findFirst()
                .orElseGet(() -> GadgetModes.INSTANCE.getModesForGadget(target(selected)).stream().min(Comparator.naturalOrder()).orElseThrow());
        GadgetNBT.setMode(stack, next);
    }

    public void selectGadgetMode(ItemStack stack, ResourceLocation requested) {
        if (MultitoolState.getActiveMode(stack) == MultitoolMode.DESTRUCTION) return;
        GadgetModes.INSTANCE.getModesForGadget(target(stack)).stream().filter(mode -> mode.getId().equals(requested)).findFirst().ifPresent(mode -> {
            GadgetNBT.setMode(stack, mode);
            MultitoolState.setProfileMode(stack, MultitoolState.getActiveMode(stack), mode.getId());
        });
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return switch (MultitoolState.getActiveMode(player.getItemInHand(hand))) {
            case BUILD -> Registration.Building_Gadget.get().use(level, player, hand);
            case EXCHANGING -> Registration.Exchanging_Gadget.get().use(level, player, hand);
            case COPY_PASTE -> Registration.CopyPaste_Gadget.get().use(level, player, hand);
            case CUT_PASTE -> Registration.CutPaste_Gadget.get().use(level, player, hand);
            case DESTRUCTION -> Registration.Destruction_Gadget.get().use(level, player, hand);
        };
    }

    @Override public void undo(Level level, Player player, ItemStack stack) {
        switch (MultitoolState.getActiveMode(stack)) {
            case EXCHANGING -> ((GadgetExchanger) Registration.Exchanging_Gadget.get()).undo(level, player, stack);
            case DESTRUCTION -> ((GadgetDestruction) Registration.Destruction_Gadget.get()).undo(level, player, stack);
            default -> super.undo(level, player, stack);
        }
    }

    @Override public int getEnchantmentValue(ItemStack stack) { return 3; }
    @Override public boolean isEnchantable(ItemStack stack) { return MultitoolState.getActiveMode(stack) == MultitoolMode.EXCHANGING || super.isEnchantable(stack); }
    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return MultitoolState.getActiveMode(stack) == MultitoolMode.EXCHANGING && EnchantmentHelper.getEnchantments(book).containsKey(Enchantments.SILK_TOUCH) || super.isBookEnchantable(stack, book);
    }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return MultitoolState.getActiveMode(stack) == MultitoolMode.EXCHANGING && enchantment == Enchantments.SILK_TOUCH || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("buildinggadgetsextra.multitool.active", Component.translatable(MultitoolState.getActiveMode(stack).translationKey())).withStyle(ChatFormatting.AQUA));
    }
}
