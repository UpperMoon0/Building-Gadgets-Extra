package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.items.*;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.client.LegacyMultitoolRenderer;
import com.direwolf20.buildinggadgets.client.renders.CopyPasteRender;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public final class BuildersMultitool extends GadgetCopyPaste {
    @OnlyIn(Dist.CLIENT)
    @Override public CopyPasteRender getRender() { return LegacyMultitoolRenderer.INSTANCE; }
    @Override public int getEnergyMax() {
        return Math.max(Config.GADGETS.GADGET_EXCHANGER.maxEnergy.get(), Math.max(Math.max(Config.GADGETS.GADGET_BUILDING.maxEnergy.get(), Config.GADGETS.GADGET_COPY_PASTE.maxEnergy.get()), Config.GADGETS.GADGET_DESTRUCTION.maxEnergy.get()));
    }
    @Override public int getEnergyCost(ItemStack stack) {
        return Math.max(Config.GADGETS.GADGET_EXCHANGER.energyCost.get(), Math.max(Math.max(Config.GADGETS.GADGET_BUILDING.energyCost.get(), Config.GADGETS.GADGET_COPY_PASTE.energyCost.get()), Config.GADGETS.GADGET_DESTRUCTION.energyCost.get()));
    }
    @Override public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        switch (MultitoolState.getActiveMode(player.getItemInHand(hand))) {
            case BUILD: return OurItems.BUILDING_GADGET_ITEM.get().use(world, player, hand);
            case EXCHANGING: return OurItems.EXCHANGING_GADGET_ITEM.get().use(world, player, hand);
            case DESTRUCTION: return OurItems.DESTRUCTION_GADGET_ITEM.get().use(world, player, hand);
            case COPY_PASTE:
            case CUT_PASTE:
            default: return OurItems.COPY_PASTE_GADGET_ITEM.get().use(world, player, hand);
        }
    }
    @Override public void undo(World world, PlayerEntity player, ItemStack stack) {
        AbstractGadget delegate;
        switch (MultitoolState.getActiveMode(stack)) {
            case BUILD: delegate = (AbstractGadget) OurItems.BUILDING_GADGET_ITEM.get(); break;
            case EXCHANGING: delegate = (AbstractGadget) OurItems.EXCHANGING_GADGET_ITEM.get(); break;
            case DESTRUCTION: delegate = (AbstractGadget) OurItems.DESTRUCTION_GADGET_ITEM.get(); break;
            default: delegate = (AbstractGadget) OurItems.COPY_PASTE_GADGET_ITEM.get();
        }
        delegate.undo(world, player, stack);
    }
    @Override public int getEnchantmentValue() { return 3; }
    @Override public boolean isEnchantable(ItemStack stack) { return MultitoolState.getActiveMode(stack) == MultitoolMode.EXCHANGING || super.isEnchantable(stack); }
    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return MultitoolState.getActiveMode(stack) == MultitoolMode.EXCHANGING && EnchantmentHelper.getEnchantments(book).containsKey(Enchantments.SILK_TOUCH) || super.isBookEnchantable(stack, book);
    }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return MultitoolState.getActiveMode(stack) == MultitoolMode.EXCHANGING && enchantment == Enchantments.SILK_TOUCH || super.canApplyAtEnchantingTable(stack, enchantment);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(new TranslationTextComponent("buildinggadgetsextra.multitool.active",
                new TranslationTextComponent(MultitoolState.getActiveMode(stack).translationKey())).withStyle(TextFormatting.AQUA));
    }
}
