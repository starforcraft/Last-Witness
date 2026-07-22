package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import static com.ultramega.lastwitness.LastWitness.MODID;

public record GhostReplayPayload(String sourceEntityType, double anchorX, double anchorY, double anchorZ, List<EntitySnapshot> snapshots) implements CustomPacketPayload {
    public static final Type<GhostReplayPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "ghost_replay"));

    private static final int MAX_SNAPSHOTS = 100;

    private static final StreamCodec<ByteBuf, String> ENTITY_TYPE_STREAM_CODEC = ByteBufCodecs.stringUtf8(256);
    private static final StreamCodec<ByteBuf, List<EntitySnapshot>> SNAPSHOTS_STREAM_CODEC = EntitySnapshot.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SNAPSHOTS));

    public static final StreamCodec<ByteBuf, GhostReplayPayload> STREAM_CODEC = StreamCodec.composite(
        ENTITY_TYPE_STREAM_CODEC, GhostReplayPayload::sourceEntityType,
        ByteBufCodecs.DOUBLE, GhostReplayPayload::anchorX,
        ByteBufCodecs.DOUBLE, GhostReplayPayload::anchorY,
        ByteBufCodecs.DOUBLE, GhostReplayPayload::anchorZ,
        SNAPSHOTS_STREAM_CODEC, GhostReplayPayload::snapshots,
        GhostReplayPayload::new
    );

    public GhostReplayPayload {
        snapshots = List.copyOf(snapshots);
        if (snapshots.size() > MAX_SNAPSHOTS) {
            throw new IllegalArgumentException("A ghost replay may contain at most " + MAX_SNAPSHOTS + " snapshots");
        }
    }

    public static GhostReplayPayload fromTrack(final EchoTrack track, final Vec3 anchor) {
        return new GhostReplayPayload(
            track.sourceEntityType(),
            anchor.x(),
            anchor.y(),
            anchor.z(),
            track.snapshots()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
