package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.LastWitness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

final class EchoTrackerSavedData extends SavedData {
    static final SavedDataType<EchoTrackerSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(LastWitness.MODID, "echo_tracker"),
        EchoTrackerSavedData::new,
        StoreState.CODEC.xmap(
            EchoTrackerSavedData::new,
            EchoTrackerSavedData::state
        )
    );

    final Map<UUID, EchoTracker> activeTrackers = new HashMap<>();
    final Map<UUID, EchoTrack> completedTracks = new HashMap<>();
    final Set<UUID> markedTrackerIds = new HashSet<>();
    final Map<UUID, List<OutsideEntityTracker>> outsideTrackers = new HashMap<>();

    EchoTrackerSavedData() {
    }

    private EchoTrackerSavedData(final StoreState state) {
        for (final ActiveTrackerEntry entry : state.activeTrackers()) {
            this.activeTrackers.put(entry.entityId(), entry.tracker());
        }
        for (final EchoTrack track : state.completedTracks()) {
            this.completedTracks.put(track.id(), track);
        }
        this.markedTrackerIds.addAll(state.markedTrackerIds());
        for (final OutsideTrackerEntry entry : state.outsideTrackers()) {
            if (!entry.trackers().isEmpty()) {
                this.outsideTrackers.put(entry.sourceEntityId(), new ArrayList<>(entry.trackers()));
            }
        }
    }

    private StoreState state() {
        final List<ActiveTrackerEntry> activeEntries = this.activeTrackers.entrySet().stream()
            .map(entry -> new ActiveTrackerEntry(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(entry -> entry.entityId().toString()))
            .toList();
        final List<EchoTrack> completedEntries = this.completedTracks.values().stream()
            .sorted(Comparator.comparing(EchoTrack::id))
            .toList();
        final List<UUID> markedEntries = this.markedTrackerIds.stream()
            .sorted()
            .toList();
        final List<OutsideTrackerEntry> outsideEntries = this.outsideTrackers.entrySet().stream()
            .map(entry -> new OutsideTrackerEntry(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(entry -> entry.sourceEntityId().toString()))
            .toList();
        return new StoreState(activeEntries, completedEntries, markedEntries, outsideEntries);
    }

    record ActiveTrackerEntry(UUID entityId, EchoTracker tracker) {
        public static final Codec<ActiveTrackerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("entityId").forGetter(ActiveTrackerEntry::entityId),
            EchoTracker.CODEC.fieldOf("tracker").forGetter(ActiveTrackerEntry::tracker)
        ).apply(instance, ActiveTrackerEntry::new));
    }

    record OutsideTrackerEntry(UUID sourceEntityId, List<OutsideEntityTracker> trackers) {
        public static final Codec<OutsideTrackerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("sourceEntityId").forGetter(OutsideTrackerEntry::sourceEntityId),
            OutsideEntityTracker.CODEC.listOf().fieldOf("trackers").forGetter(OutsideTrackerEntry::trackers)
        ).apply(instance, OutsideTrackerEntry::new));

        OutsideTrackerEntry {
            trackers = List.copyOf(trackers);
        }
    }

    record StoreState(List<ActiveTrackerEntry> activeTrackers,
                      List<EchoTrack> completedTracks,
                      List<UUID> markedTrackerIds,
                      List<OutsideTrackerEntry> outsideTrackers) {
        public static final Codec<StoreState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ActiveTrackerEntry.CODEC.listOf().optionalFieldOf("activeTrackers", List.of()).forGetter(StoreState::activeTrackers),
            EchoTrack.CODEC.listOf().optionalFieldOf("completedTracks", List.of()).forGetter(StoreState::completedTracks),
            UUIDUtil.CODEC.listOf().optionalFieldOf("markedTrackerIds", List.of()).forGetter(StoreState::markedTrackerIds),
            OutsideTrackerEntry.CODEC.listOf().optionalFieldOf("outsideTrackers", List.of()).forGetter(StoreState::outsideTrackers)
        ).apply(instance, StoreState::new));


        StoreState {
            activeTrackers = List.copyOf(activeTrackers);
            completedTracks = List.copyOf(completedTracks);
            markedTrackerIds = List.copyOf(markedTrackerIds);
            outsideTrackers = List.copyOf(outsideTrackers);
        }
    }
}
