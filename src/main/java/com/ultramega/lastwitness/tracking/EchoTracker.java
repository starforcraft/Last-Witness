package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.OutsideEntityReplay;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

final class EchoTracker {
    private static final int MAX_ENTITY_EVENTS = 4096;

    private final String id = UUID.randomUUID().toString();
    private final ArrayDeque<EntitySnapshot> snapshots = new ArrayDeque<>(trackedTicks());
    private final ArrayDeque<EntityReplayEvent> entityEvents = new ArrayDeque<>();
    private long lastRecordedGameTime = Long.MIN_VALUE;

    void recordEntityEvent(final LivingEntity entity, final byte eventId) {
        // Damage events are recorded in recordDamageEvent
        if (eventId == EntityEvent.KINETIC_HIT) {
            return;
        }
        this.addEntityEvent(entity, eventId, Optional.empty());
    }

    void recordDamageEvent(final LivingEntity entity, final Optional<OutsideEntityReplay> sourceEntityReplay) {
        this.addEntityEvent(entity, EntityEvent.KINETIC_HIT, sourceEntityReplay);
    }

    private void addEntityEvent(final LivingEntity entity,
                                final byte eventId,
                                final Optional<OutsideEntityReplay> sourceEntityReplay) {
        final long gameTime = entity.level().getGameTime();
        final int trackedTicks = trackedTicks();
        this.pruneBefore(gameTime - trackedTicks);

        while (this.entityEvents.size() >= MAX_ENTITY_EVENTS) {
            this.entityEvents.removeFirst();
        }

        this.entityEvents.addLast(new EntityReplayEvent(gameTime, eventId, sourceEntityReplay));
    }

    boolean updateDamageReplay(final long eventGameTime,
                               final UUID sourceEntityId,
                               final OutsideEntityReplay sourceEntityReplay) {
        boolean updated = false;
        final ArrayDeque<EntityReplayEvent> updatedEvents = new ArrayDeque<>(this.entityEvents.size());
        for (final EntityReplayEvent replayEvent : this.entityEvents) {
            final boolean matches = replayEvent.gameTime() == eventGameTime
                && replayEvent.eventByte() == EntityEvent.KINETIC_HIT
                && replayEvent.sourceEntity()
                .map(replay -> replay.sourceEntity().uuid().equals(sourceEntityId))
                .orElse(false);
            if (matches) {
                updatedEvents.addLast(new EntityReplayEvent(replayEvent.gameTime(), replayEvent.eventId(), Optional.of(sourceEntityReplay)));
                updated = true;
            } else {
                updatedEvents.addLast(replayEvent);
            }
        }
        if (updated) {
            this.entityEvents.clear();
            this.entityEvents.addAll(updatedEvents);
        }
        return updated;
    }

    void recordFinal(final LivingEntity entity) {
        this.record(entity, true);
    }

    void record(final LivingEntity entity) {
        this.record(entity, false);
    }

    private void record(final LivingEntity entity, final boolean force) {
        final long gameTime = entity.level().getGameTime();
        final int trackedTicks = trackedTicks();
        this.pruneBefore(gameTime - trackedTicks);

        if (!force && this.lastRecordedGameTime != Long.MIN_VALUE && gameTime - this.lastRecordedGameTime < 1) {
            return;
        }

        if (force && !this.snapshots.isEmpty() && this.snapshots.getLast().gameTime() == gameTime) {
            this.snapshots.removeLast();
        }

        while (this.snapshots.size() >= trackedTicks) {
            this.snapshots.removeFirst();
        }

        this.snapshots.addLast(EntitySnapshot.capture(entity));
        this.lastRecordedGameTime = gameTime;
    }

    private static int trackedTicks() {
        return Config.ECHO_TRACK_SECONDS.get() * 20;
    }

    private void pruneBefore(final long oldestAllowedGameTime) {
        while (!this.snapshots.isEmpty() && this.snapshots.getFirst().gameTime() < oldestAllowedGameTime) {
            this.snapshots.removeFirst();
        }
        while (!this.entityEvents.isEmpty() && this.entityEvents.getFirst().gameTime() < oldestAllowedGameTime) {
            this.entityEvents.removeFirst();
        }
    }

    String id() {
        return this.id;
    }

    EchoTrack freeze(final LivingEntity entity) {
        final String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return new EchoTrack(
            this.id,
            entity.getUUID(),
            entityTypeId,
            entity.level().getGameTime(),
            List.copyOf(this.snapshots),
            List.copyOf(this.entityEvents)
        );
    }
}
