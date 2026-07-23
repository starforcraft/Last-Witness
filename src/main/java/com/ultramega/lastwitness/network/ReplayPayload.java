package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EntityReplayEvent;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static com.ultramega.lastwitness.LastWitness.MODID;

public record ReplayPayload(String sourceEntityType, List<EntitySnapshot> snapshots, List<EntityReplayEvent> entityEvents, boolean firstPerson) implements CustomPacketPayload {
    public static final Type<ReplayPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "ghost_replay"));

    private static final int MAX_SNAPSHOTS = 100;
    private static final int MAX_ENTITY_EVENTS = 4096;

    private static final StreamCodec<ByteBuf, String> ENTITY_TYPE_STREAM_CODEC = ByteBufCodecs.stringUtf8(256);
    private static final StreamCodec<ByteBuf, List<EntitySnapshot>> SNAPSHOTS_STREAM_CODEC = EntitySnapshot.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SNAPSHOTS));
    private static final StreamCodec<ByteBuf, List<EntityReplayEvent>> ENTITY_EVENTS_STREAM_CODEC = EntityReplayEvent.STREAM_CODEC.apply(
        ByteBufCodecs.list(MAX_ENTITY_EVENTS));

    public static final StreamCodec<ByteBuf, ReplayPayload> STREAM_CODEC = StreamCodec.composite(
        ENTITY_TYPE_STREAM_CODEC, ReplayPayload::sourceEntityType,
        SNAPSHOTS_STREAM_CODEC, ReplayPayload::snapshots,
        ENTITY_EVENTS_STREAM_CODEC, ReplayPayload::entityEvents,
        ByteBufCodecs.BOOL, ReplayPayload::firstPerson,
        ReplayPayload::new
    );

    public ReplayPayload {
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);

        if (snapshots.size() > MAX_SNAPSHOTS) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + MAX_SNAPSHOTS + " snapshots");
        }
        if (entityEvents.size() > MAX_ENTITY_EVENTS) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + MAX_ENTITY_EVENTS + " entity events");
        }
    }

    public static ReplayPayload fromTrack(final EchoTrack track, final boolean firstPerson) {
        return new ReplayPayload(track.sourceEntityType(), track.snapshots(), track.entityEvents(), firstPerson);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
