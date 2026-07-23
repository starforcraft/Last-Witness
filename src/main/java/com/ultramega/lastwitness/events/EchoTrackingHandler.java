package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.registry.ModAttachments;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import java.util.List;

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

        EchoTrackerManager.recordOutsideSourceTick(serverLevel.getServer(), livingEntity);

        if (!livingEntity.isAlive() || !carriesEcho(livingEntity)) {
            return;
        }
        EchoTrackerManager.record(serverLevel.getServer(), livingEntity);
    }

    @SubscribeEvent
    public static void onLivingDamage(final LivingDamageEvent.Post event) {
        final LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity.level() instanceof ServerLevel serverLevel) || !carriesEcho(livingEntity)) {
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
        if (!(livingEntity.level() instanceof ServerLevel serverLevel) || !carriesEcho(livingEntity)) {
            return;
        }

        final List<ItemEntity> usableDrops = event.getDrops().stream()
            .filter(drop -> !drop.getItem().isEmpty())
            .toList();
        if (usableDrops.isEmpty()) {
            EchoTrackerManager.discardActive(serverLevel.getServer(), livingEntity.getUUID());
            return;
        }

        final EchoTrack track = EchoTrackerManager.complete(serverLevel.getServer(), livingEntity);
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

        EchoTrackerManager.finishOutsideSource(serverLevel.getServer(), livingEntity);
        if (carriesEcho(livingEntity)) {
            EchoTrackerManager.discardActive(serverLevel.getServer(), livingEntity.getUUID());
        }
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
