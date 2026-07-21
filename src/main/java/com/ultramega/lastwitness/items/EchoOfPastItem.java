package com.ultramega.lastwitness.items;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class EchoOfPastItem extends Item {
    public EchoOfPastItem(final Item.Properties builder) {
        super(builder);
    }

    @Override
    public void appendHoverText(final ItemStack itemStack,
                                final TooltipContext context,
                                final TooltipDisplay display,
                                final Consumer<Component> builder,
                                final TooltipFlag tooltipFlag) {

        builder.accept(Component.translatable("item.lastwitness.echo_of_past.help1").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("item.lastwitness.echo_of_past.help2").withStyle(ChatFormatting.AQUA));
    }
}
