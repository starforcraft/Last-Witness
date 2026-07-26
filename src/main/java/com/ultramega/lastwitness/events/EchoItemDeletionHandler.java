package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;

import static com.ultramega.lastwitness.LastWitness.MODID;

@EventBusSubscriber(modid = MODID)
public final class EchoItemDeletionHandler {
    private EchoItemDeletionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemExpire(final ItemExpireEvent event) {
        if (event.getExtraLife() > 0 || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        removeCompleted(level, event.getEntity().getItem());
    }

    @SubscribeEvent
    public static void onPlayerDestroyItem(final PlayerDestroyItemEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            removeCompleted(level, event.getOriginal());
        }
    }

    public static void onItemEntityDestroyed(final ItemEntity itemEntity) {
        if (itemEntity.level() instanceof ServerLevel level) {
            removeCompleted(level, itemEntity.getItem());
        }
    }

    private static void removeCompleted(final ServerLevel level, final ItemStack stack) {
        final EchoOfPastData echo = stack.get(ModDataComponents.ECHO_OF_PAST.get());
        if (echo != null) {
            EchoTrackerManager.removeCompleted(level.getServer(), echo.trackerId());
        }
    }
}

