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

public record GhostReplayPayload(String sourceEntityType, List<EntitySnapshot> snapshots, List<EntityReplayEvent> entityEvents) implements CustomPacketPayload {
    public static final Type<GhostReplayPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "ghost_replay"));

    private static final int MAX_SNAPSHOTS = 100;
    private static final int MAX_ENTITY_EVENTS = 4096;

    private static final StreamCodec<ByteBuf, String> ENTITY_TYPE_STREAM_CODEC = ByteBufCodecs.stringUtf8(256);
    private static final StreamCodec<ByteBuf, List<EntitySnapshot>> SNAPSHOTS_STREAM_CODEC = EntitySnapshot.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SNAPSHOTS));
    private static final StreamCodec<ByteBuf, List<EntityReplayEvent>> ENTITY_EVENTS_STREAM_CODEC = EntityReplayEvent.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTITY_EVENTS));

    public static final StreamCodec<ByteBuf, GhostReplayPayload> STREAM_CODEC = StreamCodec.composite(
        ENTITY_TYPE_STREAM_CODEC, GhostReplayPayload::sourceEntityType,
        SNAPSHOTS_STREAM_CODEC, GhostReplayPayload::snapshots,
        ENTITY_EVENTS_STREAM_CODEC, GhostReplayPayload::entityEvents, GhostReplayPayload::new
    );

    public GhostReplayPayload {
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);

        if (snapshots.size() > MAX_SNAPSHOTS) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + MAX_SNAPSHOTS + " snapshots");
        }
        if (entityEvents.size() > MAX_ENTITY_EVENTS) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + MAX_ENTITY_EVENTS + " entity events");
        }
    }

    public static GhostReplayPayload fromTrack(final EchoTrack track) {
        return new GhostReplayPayload(track.sourceEntityType(), track.snapshots(), track.entityEvents());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
