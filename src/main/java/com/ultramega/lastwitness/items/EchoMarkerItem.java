package com.ultramega.lastwitness.items;

import com.ultramega.lastwitness.data.EchoMarkedData;
import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.registry.ModItems;

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

        final ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.ECHO_OF_PAST.get()) && offhand.has(ModDataComponents.ECHO_OF_PAST)) {
            final EchoOfPastData echoOfPast = offhand.get(ModDataComponents.ECHO_OF_PAST);
            offhand.shrink(1);
            // TODO
        } else {
            player.getMainHandItem().set(ModDataComponents.ECHO_MARKED, new EchoMarkedData(player.getUUID()));
        }

        return super.use(level, player, hand);
    }
}
