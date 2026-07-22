package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.Config;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

final class EchoTracker {
    private final String id = UUID.randomUUID().toString();
    private final ArrayDeque<EntitySnapshot> snapshots = new ArrayDeque<>(Config.ECHO_TRACK_SECONDS.get());

    void record(final LivingEntity entity) {
        if (this.snapshots.size() == Config.ECHO_TRACK_SECONDS.get()) {
            this.snapshots.removeFirst();
        }

        this.snapshots.addLast(EntitySnapshot.capture(entity));
    }

    EchoTrack freeze(final LivingEntity entity) {
        final String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return new EchoTrack(
            this.id,
            entity.getUUID(),
            entityTypeId,
            entity.level().getGameTime(),
            List.copyOf(this.snapshots)
        );
    }
}
