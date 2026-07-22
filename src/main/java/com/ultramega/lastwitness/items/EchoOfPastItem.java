package com.ultramega.lastwitness.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EchoOfPastItem extends Item {
    public EchoOfPastItem(final Item.Properties builder) {
        super(builder);
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity livingEntity, final ItemStack itemStack, final int ticksRemaining) {
        super.onUseTick(level, livingEntity, itemStack, ticksRemaining);
        // TODO on consume: play the death replay
    }
}
