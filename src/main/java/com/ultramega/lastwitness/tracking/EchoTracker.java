package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.OutsideEntityReplay;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

final class EchoTracker {
    static final int MAX_ENTITY_EVENTS = 4096;
    static final int MAX_ACTIVE_SNAPSHOTS = 120 * 20;

    public static final Codec<EchoTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(EchoTracker::id),
        EntitySnapshot.CODEC.listOf(0, MAX_ACTIVE_SNAPSHOTS).fieldOf("snapshots").forGetter(EchoTracker::snapshots),
        EntityReplayEvent.CODEC.listOf(0, EchoTracker.MAX_ENTITY_EVENTS).fieldOf("entityEvents").forGetter(EchoTracker::entityEvents),
        Codec.LONG.fieldOf("lastRecordedGameTime").forGetter(EchoTracker::lastRecordedGameTime)
    ).apply(instance, EchoTracker::new));

    private final UUID id;
    private final ArrayDeque<EntitySnapshot> snapshots;
    private final ArrayDeque<EntityReplayEvent> entityEvents;
    private long lastRecordedGameTime;

    EchoTracker() {
        this(UUID.randomUUID(), List.of(), List.of(), Long.MIN_VALUE);
    }

    EchoTracker(final UUID id,
                final List<EntitySnapshot> snapshots,
                final List<EntityReplayEvent> entityEvents,
                final long lastRecordedGameTime) {
        this.id = id;
        this.snapshots = new ArrayDeque<>(Math.max(trackedTicks(), snapshots.size()));
        this.snapshots.addAll(snapshots);
        this.entityEvents = new ArrayDeque<>(entityEvents);
        this.lastRecordedGameTime = lastRecordedGameTime;
    }

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

    UUID id() {
        return this.id;
    }

    List<EntitySnapshot> snapshots() {
        return List.copyOf(this.snapshots);
    }

    List<EntityReplayEvent> entityEvents() {
        return List.copyOf(this.entityEvents);
    }

    long lastRecordedGameTime() {
        return this.lastRecordedGameTime;
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
