package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.registry.ModAttachments;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static com.ultramega.lastwitness.LastWitness.MODID;

@EventBusSubscriber(modid = MODID)
public final class EchoTrackingHandler {
    private EchoTrackingHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(final EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity) || !(livingEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        final MinecraftServer server = serverLevel.getServer();
        EchoTrackerManager.recordOutsideSourceTick(server, livingEntity);

        if (!livingEntity.isAlive()) {
            if (EchoTrackerManager.isMarked(server, livingEntity.getUUID())) {
                EchoTrackerManager.complete(server, livingEntity);
            }
            return;
        }

        if (!shouldTrack(server, livingEntity)) {
            return;
        }
        EchoTrackerManager.record(server, livingEntity);
    }

    @SubscribeEvent
    public static void onLivingDamage(final LivingDamageEvent.Post event) {
        final LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity.level() instanceof ServerLevel serverLevel) || !shouldTrack(serverLevel.getServer(), livingEntity)) {
            return;
        }

        LivingEntity sourceEntity = event.getSource().getEntity() instanceof LivingEntity livingSource
            ? livingSource
            : null;
        if (sourceEntity == null && event.getSource().getDirectEntity() instanceof LivingEntity directLivingSource) {
            sourceEntity = directLivingSource;
        }

        EchoTrackerManager.recordDamageEvent(serverLevel.getServer(), livingEntity, sourceEntity);
    }

    @SubscribeEvent
    public static void onLivingDrops(final LivingDropsEvent event) {
        final LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final MinecraftServer server = serverLevel.getServer();
        final boolean carriesEcho = carriesEcho(livingEntity);
        final boolean marked = EchoTrackerManager.isMarked(server, livingEntity.getUUID());
        if (!carriesEcho && !marked) {
            return;
        }

        final List<ItemEntity> usableDrops = event.getDrops().stream()
            .filter(drop -> !drop.getItem().isEmpty())
            .toList();
        if (!marked && usableDrops.isEmpty()) {
            EchoTrackerManager.discardActive(server, livingEntity.getUUID());
            return;
        }

        final EchoTrack track = EchoTrackerManager.hasActive(server, livingEntity.getUUID())
            ? EchoTrackerManager.complete(server, livingEntity)
            : EchoTrackerManager.findLatestCompletedForEntity(server, livingEntity.getUUID()).orElseGet(() -> EchoTrackerManager.complete(server, livingEntity));
        if (!carriesEcho || usableDrops.isEmpty()) {
            return;
        }

        final EchoOfPastData component = new EchoOfPastData(
            track.id(),
            track.sourceEntityType(),
            event.getSource().getLocalizedDeathMessage(livingEntity),
            track.timeOfDeath(),
            true
        );

        final ItemEntity selectedDrop = usableDrops.get(serverLevel.getRandom().nextInt(usableDrops.size()));
        attachToExactlyOneItem(event, selectedDrop, component, serverLevel);
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(final EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        final MinecraftServer server = serverLevel.getServer();
        EchoTrackerManager.finishOutsideSource(server, livingEntity);
        if (!EchoTrackerManager.hasActive(server, livingEntity.getUUID())) {
            return;
        }
        if (EchoTrackerManager.isMarked(server, livingEntity.getUUID())) {
            if (!livingEntity.isAlive()) {
                EchoTrackerManager.complete(server, livingEntity);
            }
            return;
        }
        EchoTrackerManager.discardActive(server, livingEntity.getUUID());
    }

    private static boolean shouldTrack(final MinecraftServer server, final LivingEntity livingEntity) {
        return carriesEcho(livingEntity) || EchoTrackerManager.hasActive(server, livingEntity.getUUID());
    }

    private static boolean carriesEcho(final LivingEntity livingEntity) {
        return livingEntity.hasData(ModAttachments.CARRIES_ECHO)
            && livingEntity.getData(ModAttachments.CARRIES_ECHO);
    }

    private static void attachToExactlyOneItem(final LivingDropsEvent event,
                                               final ItemEntity selectedDrop,
                                               final EchoOfPastData component,
                                               final ServerLevel level) {
        final ItemStack selectedStack = selectedDrop.getItem();
        if (selectedStack.getCount() == 1) {
            selectedStack.set(ModDataComponents.ECHO_OF_PAST.get(), component);
            return;
        }

        // Split the stack so that there are no multiple copies
        final ItemStack echoStack = selectedStack.split(1);
        echoStack.set(ModDataComponents.ECHO_OF_PAST.get(), component);

        final ItemEntity echoDrop = new ItemEntity(
            level,
            selectedDrop.getX(),
            selectedDrop.getY(),
            selectedDrop.getZ(),
            echoStack
        );
        echoDrop.setDeltaMovement(selectedDrop.getDeltaMovement());
        event.getDrops().add(echoDrop);
    }
}
