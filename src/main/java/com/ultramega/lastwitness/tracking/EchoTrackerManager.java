package com.ultramega.lastwitness.tracking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

public final class EchoTrackerManager {
    private static final Map<MinecraftServer, TrackerStore> STORES = Collections.synchronizedMap(new WeakHashMap<>());

    private EchoTrackerManager() {
    }

    public static void record(final MinecraftServer server, final LivingEntity entity) {
        final TrackerStore store = getStore(server);
        store.activeTrackers
            .computeIfAbsent(entity.getUUID(), ignored -> new EchoTracker())
            .record(entity);
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
        getStore(server).activeTrackers.remove(entityId);
    }

    public static Optional<EchoTrack> removeCompleted(final MinecraftServer server, final String trackerId) {
        return Optional.ofNullable(getStore(server).completedTracks.remove(trackerId));
    }

    private static TrackerStore getStore(final MinecraftServer server) {
        synchronized (STORES) {
            return STORES.computeIfAbsent(server, ignored -> new TrackerStore());
        }
    }

    private static final class TrackerStore {
        private final Map<UUID, EchoTracker> activeTrackers = new HashMap<>();
        private final Map<String, EchoTrack> completedTracks = new HashMap<>();
    }
}
