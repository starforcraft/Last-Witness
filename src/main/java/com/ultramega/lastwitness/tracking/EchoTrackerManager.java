package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.data.OutsideEntityReplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

// TODO: save the echo trackers as long as the corresponding item exists
public final class EchoTrackerManager {
    private static final Map<MinecraftServer, TrackerStore> STORES = Collections.synchronizedMap(new WeakHashMap<>());

    private EchoTrackerManager() {
    }

    public static void record(final MinecraftServer server, final LivingEntity entity) {
        activeTracker(server, entity).record(entity);
    }

    public static void recordEntityEvent(final MinecraftServer server,
                                         final LivingEntity entity,
                                         final byte eventId) {
        activeTracker(server, entity).recordEntityEvent(entity, eventId);
    }

    public static void recordDamageEvent(final MinecraftServer server,
                                         final LivingEntity replayedEntity,
                                         final LivingEntity sourceEntity) {
        final EchoTracker replayTracker = activeTracker(server, replayedEntity);
        if (sourceEntity == null || sourceEntity == replayedEntity) {
            replayTracker.recordDamageEvent(replayedEntity, Optional.empty());
            return;
        }
        final OutsideEntityTracker outsideTracker = new OutsideEntityTracker(
            replayTracker.id(),
            replayedEntity.level().getGameTime(),
            sourceEntity
        );
        replayTracker.recordDamageEvent(replayedEntity, Optional.of(outsideTracker.freeze()));
        getStore(server).outsideTrackers
            .computeIfAbsent(sourceEntity.getUUID(), ignored -> new ArrayList<>())
            .add(outsideTracker);
    }

    public static void recordOutsideEntityEvent(final MinecraftServer server,
                                                final LivingEntity sourceEntity,
                                                final byte eventId) {
        final TrackerStore store = getStore(server);
        final List<OutsideEntityTracker> sourceTrackers =
            store.outsideTrackers.get(sourceEntity.getUUID());

        if (sourceTrackers == null) {
            return;
        }

        sourceTrackers.removeIf(outsideTracker ->
            !outsideTracker.recordEntityEvent(sourceEntity, eventId) || !updateOutsideReplay(store, outsideTracker));

        if (sourceTrackers.isEmpty()) {
            store.outsideTrackers.remove(sourceEntity.getUUID());
        }
    }

    public static void recordOutsideSourceTick(final MinecraftServer server,
                                               final LivingEntity sourceEntity) {
        final TrackerStore store = getStore(server);
        final List<OutsideEntityTracker> sourceTrackers = store.outsideTrackers.get(sourceEntity.getUUID());
        if (sourceTrackers == null) {
            return;
        }

        final Iterator<OutsideEntityTracker> iterator = sourceTrackers.iterator();
        while (iterator.hasNext()) {
            final OutsideEntityTracker outsideTracker = iterator.next();
            final boolean finished = outsideTracker.record(sourceEntity);
            if (!updateOutsideReplay(store, outsideTracker)) {
                iterator.remove();
                continue;
            }
            if (finished) {
                iterator.remove();
            }
        }
        if (sourceTrackers.isEmpty()) {
            store.outsideTrackers.remove(sourceEntity.getUUID());
        }
    }

    public static void finishOutsideSource(final MinecraftServer server,
                                           final LivingEntity sourceEntity) {
        final TrackerStore store = getStore(server);
        final List<OutsideEntityTracker> sourceTrackers = store.outsideTrackers.remove(sourceEntity.getUUID());
        if (sourceTrackers == null) {
            return;
        }
        for (final OutsideEntityTracker outsideTracker : sourceTrackers) {
            outsideTracker.record(sourceEntity);
            updateOutsideReplay(store, outsideTracker);
        }
    }

    public static EchoTrack complete(final MinecraftServer server, final LivingEntity entity) {
        final TrackerStore store = getStore(server);
        final EchoTracker activeTracker = store.activeTrackers.remove(entity.getUUID());
        final EchoTracker tracker = activeTracker != null ? activeTracker : new EchoTracker();
        tracker.recordFinal(entity);
        final EchoTrack completedTrack = tracker.freeze(entity);
        store.completedTracks.put(completedTrack.id(), completedTrack);
        return completedTrack;
    }

    public static Optional<EchoTrack> findCompleted(final MinecraftServer server, final String trackerId) {
        return Optional.ofNullable(getStore(server).completedTracks.get(trackerId));
    }

    public static void discardActive(final MinecraftServer server, final UUID entityId) {
        final TrackerStore store = getStore(server);
        final EchoTracker removedTracker = store.activeTrackers.remove(entityId);
        if (removedTracker != null) {
            removeOutsideTrackers(store, removedTracker.id());
        }
    }

    public static Optional<EchoTrack> removeCompleted(final MinecraftServer server, final String trackerId) {
        final TrackerStore store = getStore(server);
        removeOutsideTrackers(store, trackerId);
        return Optional.ofNullable(store.completedTracks.remove(trackerId));
    }

    private static boolean updateOutsideReplay(final TrackerStore store,
                                               final OutsideEntityTracker outsideTracker) {
        final OutsideEntityReplay outsideReplay = outsideTracker.freeze();
        for (final EchoTracker activeTracker : store.activeTrackers.values()) {
            if (activeTracker.id().equals(outsideTracker.replayTrackId())) {
                return activeTracker.updateDamageReplay(outsideTracker.eventGameTime(), outsideTracker.sourceEntityId(), outsideReplay);
            }
        }

        final EchoTrack completedTrack = store.completedTracks.get(outsideTracker.replayTrackId());
        if (completedTrack == null) {
            return false;
        }

        boolean updated = false;
        final List<EntityReplayEvent> updatedEvents = new ArrayList<>(completedTrack.entityEvents().size());
        for (final EntityReplayEvent replayEvent : completedTrack.entityEvents()) {
            final boolean matches = replayEvent.gameTime() == outsideTracker.eventGameTime()
                && replayEvent.sourceEntity()
                .map(replay -> replay.sourceEntity().uuid().equals(outsideTracker.sourceEntityId()))
                .orElse(false);
            if (matches) {
                updatedEvents.add(new EntityReplayEvent(replayEvent.gameTime(), replayEvent.eventId(), Optional.of(outsideReplay)));
                updated = true;
            } else {
                updatedEvents.add(replayEvent);
            }
        }
        if (updated) {
            store.completedTracks.put(completedTrack.id(), new EchoTrack(
                completedTrack.id(),
                completedTrack.sourceEntityId(),
                completedTrack.sourceEntityType(),
                completedTrack.timeOfDeath(),
                completedTrack.snapshots(),
                updatedEvents
            ));
        }
        return updated;
    }

    private static EchoTracker activeTracker(final MinecraftServer server, final LivingEntity entity) {
        return getStore(server).activeTrackers.computeIfAbsent(entity.getUUID(), ignored -> new EchoTracker());
    }

    private static void removeOutsideTrackers(final TrackerStore store, final String replayTrackId) {
        final Iterator<Map.Entry<UUID, List<OutsideEntityTracker>>> sources = store.outsideTrackers.entrySet().iterator();
        while (sources.hasNext()) {
            final Map.Entry<UUID, List<OutsideEntityTracker>> source = sources.next();
            source.getValue().removeIf(tracker -> tracker.replayTrackId().equals(replayTrackId));
            if (source.getValue().isEmpty()) {
                sources.remove();
            }
        }
    }

    private static TrackerStore getStore(final MinecraftServer server) {
        synchronized (STORES) {
            return STORES.computeIfAbsent(server, ignored -> new TrackerStore());
        }
    }

    private static final class TrackerStore {
        private final Map<UUID, EchoTracker> activeTrackers = new HashMap<>();
        private final Map<String, EchoTrack> completedTracks = new HashMap<>();
        private final Map<UUID, List<OutsideEntityTracker>> outsideTrackers = new HashMap<>();
    }
}
