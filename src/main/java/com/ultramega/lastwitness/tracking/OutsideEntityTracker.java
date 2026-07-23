package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.data.EntityData;
import com.ultramega.lastwitness.data.OutsideEntityEvent;
import com.ultramega.lastwitness.data.OutsideEntityReplay;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

final class OutsideEntityTracker {
    private final String replayTrackId;
    private final long eventGameTime;
    private final long finalGameTime;
    private final EntityData sourceEntity;

    private final ArrayDeque<EntitySnapshot> snapshots = new ArrayDeque<>(OutsideEntityReplay.MAX_SNAPSHOTS);
    private final ArrayDeque<OutsideEntityEvent> entityEvents = new ArrayDeque<>(OutsideEntityReplay.MAX_ENTITY_EVENTS);

    OutsideEntityTracker(final String replayTrackId,
                         final long eventGameTime,
                         final LivingEntity sourceEntity) {
        this.replayTrackId = replayTrackId;
        this.eventGameTime = eventGameTime;
        this.finalGameTime = eventGameTime + OutsideEntityReplay.MAX_SNAPSHOTS - 1L;
        this.sourceEntity = new EntityData(sourceEntity.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(sourceEntity.getType()).toString());
        this.record(sourceEntity);
    }

    String replayTrackId() {
        return this.replayTrackId;
    }

    long eventGameTime() {
        return this.eventGameTime;
    }

    UUID sourceEntityId() {
        return this.sourceEntity.uuid();
    }

    boolean record(final LivingEntity entity) {
        final long gameTime = entity.level().getGameTime();
        if (gameTime > this.finalGameTime) {
            return true;
        }

        if (!this.snapshots.isEmpty()
            && this.snapshots.getLast().gameTime() == gameTime) {
            this.snapshots.removeLast();
        }

        while (this.snapshots.size() >= OutsideEntityReplay.MAX_SNAPSHOTS) {
            this.snapshots.removeFirst();
        }

        this.snapshots.addLast(EntitySnapshot.capture(entity));
        return gameTime >= this.finalGameTime;
    }

    boolean recordEntityEvent(final LivingEntity entity, final byte eventId) {
        final long gameTime = entity.level().getGameTime();
        if (gameTime > this.finalGameTime) {
            return false;
        }

        while (this.entityEvents.size()
            >= OutsideEntityReplay.MAX_ENTITY_EVENTS) {
            this.entityEvents.removeFirst();
        }

        this.entityEvents.addLast(new OutsideEntityEvent(gameTime, eventId));
        return true;
    }

    OutsideEntityReplay freeze() {
        return new OutsideEntityReplay(
            this.sourceEntity,
            List.copyOf(this.snapshots),
            List.copyOf(this.entityEvents)
        );
    }
}
