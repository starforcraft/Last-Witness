package com.ultramega.lastwitness.items;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.EchoMarkedData;
import com.ultramega.lastwitness.registry.ModDataComponents;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EchoMarkerItem extends Item {
    public EchoMarkerItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        // TODO: implement correctly

        player.getMainHandItem().set(ModDataComponents.ECHO_MARKED, new EchoMarkedData(player.getUUID()));

        return super.use(level, player, hand);
    }

    @Override
    public int getMaxDamage(final ItemStack stack) {
        return Config.ECHO_MARKER_DURABILITY.get();
    }
}
