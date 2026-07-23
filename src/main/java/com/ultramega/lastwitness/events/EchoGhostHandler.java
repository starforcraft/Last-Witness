package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EchoGhostHandler {
    private static final int REPLAY_CHECK_INTERVAL_TICKS = 5 * 20;
    private static final double REPLAY_BROADCAST_RADIUS = 48.0D;

    private EchoGhostHandler() {
    }

    public static void onItemEntityTick(final EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)
            || !(itemEntity.level() instanceof ServerLevel serverLevel)
            || !itemEntity.isAlive()
            || !itemEntity.onGround()
            || itemEntity.tickCount < REPLAY_CHECK_INTERVAL_TICKS
            || itemEntity.tickCount % REPLAY_CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        final ItemStack stack = itemEntity.getItem();
        final EchoOfPastData echo = stack.get(ModDataComponents.ECHO_OF_PAST.get());
        if (echo == null || !echo.untouched()) {
            return;
        }

        if (serverLevel.getRandom().nextDouble() >= Config.GHOST_REPLAY_CHANCE.get()) {
            return;
        }

        final Optional<EchoTrack> track = EchoTrackerManager.findCompleted(serverLevel.getServer(), echo.trackerId());
        if (track.isEmpty() || track.get().snapshots().isEmpty()) {
            return;
        }

        PacketDistributor.sendToPlayersNear(
            serverLevel,
            null,
            itemEntity.getX(),
            itemEntity.getY(),
            itemEntity.getZ(),
            REPLAY_BROADCAST_RADIUS,
            ReplayPayload.fromTrack(track.get(), false)
        );
    }

    public static void onItemPickup(final ItemEntityPickupEvent.Post event) {
        final EchoOfPastData originalEcho = event.getOriginalStack().get(ModDataComponents.ECHO_OF_PAST.get());
        if (originalEcho == null || !originalEcho.untouched()) {
            return;
        }

        // TODO: why two times? Can't markMatchingStackPickedUp be removed?
        markMatchingStackPickedUp(event.getPlayer().getInventory(), originalEcho.trackerId());

        final ItemStack remainder = event.getCurrentStack();
        final EchoOfPastData remainderEcho = remainder.get(ModDataComponents.ECHO_OF_PAST.get());
        if (remainderEcho != null && remainderEcho.trackerId().equals(originalEcho.trackerId())) {
            remainder.set(ModDataComponents.ECHO_OF_PAST.get(), remainderEcho.markPickedUp());
        }
    }

    private static void markMatchingStackPickedUp(final Inventory inventory, final String trackerId) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            final EchoOfPastData echo = stack.get(ModDataComponents.ECHO_OF_PAST.get());
            if (echo != null && echo.untouched() && echo.trackerId().equals(trackerId)) {
                stack.set(ModDataComponents.ECHO_OF_PAST.get(), echo.markPickedUp());
                return;
            }
        }
    }
}
