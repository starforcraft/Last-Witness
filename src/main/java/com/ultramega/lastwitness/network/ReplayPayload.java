package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.EntityData;
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

public record ReplayPayload(EntityData sourceEntity,
                            List<EntitySnapshot> snapshots,
                            List<EntityReplayEvent> entityEvents,
                            boolean firstPerson) implements CustomPacketPayload {
    public static final Type<ReplayPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "ghost_replay"));

    private static final StreamCodec<ByteBuf, List<EntitySnapshot>> SNAPSHOTS_STREAM_CODEC = EntitySnapshot.STREAM_CODEC.apply(ByteBufCodecs.list(getMaxSnapshots()));
    private static final StreamCodec<ByteBuf, List<EntityReplayEvent>> ENTITY_EVENTS_STREAM_CODEC = EntityReplayEvent.STREAM_CODEC.apply(
        ByteBufCodecs.list(getMaxSnapshots()));

    public static final StreamCodec<ByteBuf, ReplayPayload> STREAM_CODEC = StreamCodec.composite(
        EntityData.STREAM_CODEC, ReplayPayload::sourceEntity,
        SNAPSHOTS_STREAM_CODEC, ReplayPayload::snapshots,
        ENTITY_EVENTS_STREAM_CODEC, ReplayPayload::entityEvents,
        ByteBufCodecs.BOOL, ReplayPayload::firstPerson,
        ReplayPayload::new
    );

    public ReplayPayload {
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);

        if (snapshots.size() > getMaxSnapshots()) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + getMaxSnapshots() + " snapshots");
        }
        if (entityEvents.size() > getMaxSnapshots()) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + getMaxSnapshots() + " entity events");
        }
    }

    public static ReplayPayload fromTrack(final EchoTrack track, final boolean firstPerson) {
        return new ReplayPayload(new EntityData(track.sourceEntityId(), track.sourceEntityType()), track.snapshots(), track.entityEvents(), firstPerson);
    }

    private static int getMaxSnapshots() {
        return Config.ECHO_TRACK_SECONDS.get() * 20;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
