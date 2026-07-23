package com.ultramega.lastwitness.data;

import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.List;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record OutsideEntityReplay(EntityData sourceEntity, List<EntitySnapshot> snapshots, List<OutsideEntityEvent> entityEvents) {
    public static final int MAX_SNAPSHOTS = 10;
    public static final int MAX_ENTITY_EVENTS = 32;

    private static final StreamCodec<ByteBuf, List<EntitySnapshot>> SNAPSHOTS_STREAM_CODEC = EntitySnapshot.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SNAPSHOTS));

    private static final StreamCodec<ByteBuf, List<OutsideEntityEvent>> ENTITY_EVENTS_STREAM_CODEC = OutsideEntityEvent.STREAM_CODEC.apply(
        ByteBufCodecs.list(MAX_ENTITY_EVENTS));

    public static final StreamCodec<ByteBuf, OutsideEntityReplay> STREAM_CODEC = StreamCodec.composite(
        EntityData.STREAM_CODEC, OutsideEntityReplay::sourceEntity,
        SNAPSHOTS_STREAM_CODEC, OutsideEntityReplay::snapshots,
        ENTITY_EVENTS_STREAM_CODEC, OutsideEntityReplay::entityEvents,
        OutsideEntityReplay::new
    );

    public OutsideEntityReplay {
        Objects.requireNonNull(sourceEntity, "sourceEntity");
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);

        if (snapshots.isEmpty()) {
            throw new IllegalArgumentException("An outside entity replay needs at least one snapshot");
        }
        if (snapshots.size() > MAX_SNAPSHOTS) {
            throw new IllegalArgumentException("An outside entity replay may contain at most " + MAX_SNAPSHOTS + " snapshots");
        }
        if (entityEvents.size() > MAX_ENTITY_EVENTS) {
            throw new IllegalArgumentException("An outside entity replay may contain at most " + MAX_ENTITY_EVENTS + " entity events");
        }
    }

    public long durationTicks() {
        final long firstGameTime = this.snapshots.getFirst().gameTime();
        long finalGameTime = this.snapshots.getLast().gameTime();

        if (!this.entityEvents.isEmpty()) {
            finalGameTime = Math.max(
                finalGameTime,
                this.entityEvents.getLast().gameTime()
            );
        }

        return Math.max(0L, finalGameTime - firstGameTime);
    }
}
