package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.Config;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

final class EchoTracker {
    private static final int TICKS_PER_SECOND = 20;
    private static final int MAX_SNAPSHOTS = 100;

    private final String id = UUID.randomUUID().toString();
    private final ArrayDeque<EntitySnapshot> snapshots = new ArrayDeque<>(MAX_SNAPSHOTS);
    private long lastRecordedGameTime = Long.MIN_VALUE;

    void recordFinal(final LivingEntity entity) {
        this.record(entity, true);
    }

    void record(final LivingEntity entity) {
        this.record(entity, false);
    }

    private void record(final LivingEntity entity, final boolean force) {
        final long gameTime = entity.level().getGameTime();
        final int trackedTicks = Math.max(1, Config.ECHO_TRACK_SECONDS.get() * TICKS_PER_SECOND);
        final int sampleInterval = Math.max(1, (trackedTicks + MAX_SNAPSHOTS - 1) / MAX_SNAPSHOTS);
        final long oldestAllowedGameTime = gameTime - trackedTicks;

        while (!this.snapshots.isEmpty() && this.snapshots.getFirst().gameTime() < oldestAllowedGameTime) {
            this.snapshots.removeFirst();
        }

        if (!force
            && this.lastRecordedGameTime != Long.MIN_VALUE
            && gameTime - this.lastRecordedGameTime < sampleInterval) {
            return;
        }

        if (force
            && !this.snapshots.isEmpty()
            && this.snapshots.getLast().gameTime() == gameTime) {
            this.snapshots.removeLast();
        }

        if (force && this.snapshots.size() >= MAX_SNAPSHOTS) {
            // Preserve the beginning of the configured time window and replace
            // the newest sampled frame with the exact death/final frame.
            this.snapshots.removeLast();
        } else {
            while (this.snapshots.size() >= MAX_SNAPSHOTS) {
                this.snapshots.removeFirst();
            }
        }

        this.snapshots.addLast(EntitySnapshot.capture(entity));
        this.lastRecordedGameTime = gameTime;
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
