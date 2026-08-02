package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.EntityData;
import com.ultramega.lastwitness.data.MemoryQuality;
import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EntityReplayEvent;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.ultramega.lastwitness.LastWitness.makeId;

public record ReplayPayload(EntityData sourceEntity,
                            List<EntitySnapshot> snapshots,
                            List<EntityReplayEvent> entityEvents,
                            boolean firstPerson,
                            ReplayCompletion completion) implements CustomPacketPayload {
    public static final Type<ReplayPayload> TYPE = new Type<>(makeId("ghost_replay"));

    private static final int MAX_REPLAY_ELEMENTS = 120 * 20;
    private static final StreamCodec<ByteBuf, List<EntitySnapshot>> SNAPSHOTS_STREAM_CODEC = EntitySnapshot.STREAM_CODEC.apply(
        ByteBufCodecs.list(MAX_REPLAY_ELEMENTS));
    private static final StreamCodec<ByteBuf, List<EntityReplayEvent>> ENTITY_EVENTS_STREAM_CODEC = EntityReplayEvent.STREAM_CODEC.apply(
        ByteBufCodecs.list(MAX_REPLAY_ELEMENTS));

    public static final StreamCodec<ByteBuf, ReplayPayload> STREAM_CODEC = StreamCodec.composite(
        EntityData.STREAM_CODEC, ReplayPayload::sourceEntity,
        SNAPSHOTS_STREAM_CODEC, ReplayPayload::snapshots,
        ENTITY_EVENTS_STREAM_CODEC, ReplayPayload::entityEvents,
        ByteBufCodecs.BOOL, ReplayPayload::firstPerson,
        ReplayCompletion.STREAM_CODEC, ReplayPayload::completion,
        ReplayPayload::new
    );

    public ReplayPayload {
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);

        final int configuredMaximum = getConfiguredMaxSnapshots();
        if (snapshots.size() > configuredMaximum) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + configuredMaximum + " snapshots");
        }
        if (entityEvents.size() > configuredMaximum) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + configuredMaximum + " entity events");
        }
    }

    public static ReplayPayload fromTrack(final EchoTrack track, final boolean firstPerson) {
        return fromTrack(track, firstPerson, ReplayCompletion.NONE);
    }

    public static ReplayPayload fromTrack(final EchoTrack track,
                                          final boolean firstPerson,
                                          final ReplayCompletion completion) {
        return new ReplayPayload(
            new EntityData(track.sourceEntityId(), track.sourceEntityType()),
            track.snapshots(),
            track.entityEvents(),
            firstPerson,
            completion
        );
    }

    private static int getConfiguredMaxSnapshots() {
        return Config.ECHO_TRACK_SECONDS.get() * 20;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ReplayCompletion(boolean present,
                                   String quality,
                                   int resonanceGained,
                                   int totalResonance) {
        public static final ReplayCompletion NONE = new ReplayCompletion(false, "", 0, 0);
        public static final StreamCodec<ByteBuf, ReplayCompletion> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ReplayCompletion::present,
            ByteBufCodecs.STRING_UTF8, ReplayCompletion::quality,
            ByteBufCodecs.VAR_INT, ReplayCompletion::resonanceGained,
            ByteBufCodecs.VAR_INT, ReplayCompletion::totalResonance,
            ReplayCompletion::new
        );

        public ReplayCompletion {
            quality = quality == null ? "" : quality;
            resonanceGained = Math.max(0, resonanceGained);
            totalResonance = Math.max(0, totalResonance);
            if (!present) {
                quality = "";
                resonanceGained = 0;
                totalResonance = 0;
            }
        }

        public static ReplayCompletion resonance(final MemoryQuality quality,
                                                  final int resonanceGained,
                                                  final int totalResonance) {
            final MemoryQuality normalizedQuality = quality == null ? MemoryQuality.FADED : quality;
            return new ReplayCompletion(
                true,
                normalizedQuality.serializedName(),
                resonanceGained,
                totalResonance
            );
        }

        public Component message() {
            if (!this.present) {
                return Component.empty();
            }

            final Component qualityName = this.memoryQuality().displayName();
            if (this.resonanceGained > 0) {
                return Component.translatable("message.lastwitness.memory_interpreted", qualityName, this.resonanceGained, this.totalResonance)
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            return Component.translatable("message.lastwitness.memory_repeated", qualityName, this.totalResonance)
                .withStyle(ChatFormatting.DARK_GRAY);
        }

        private MemoryQuality memoryQuality() {
            for (final MemoryQuality candidate : MemoryQuality.values()) {
                if (candidate.serializedName().equals(this.quality)) {
                    return candidate;
                }
            }
            return MemoryQuality.FADED;
        }
    }
}
