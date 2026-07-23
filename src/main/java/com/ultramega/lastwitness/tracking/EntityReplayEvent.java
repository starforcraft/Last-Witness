package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.data.OutsideEntityReplay;

import java.util.Objects;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EntityReplayEvent(long gameTime, int eventId, Optional<OutsideEntityReplay> sourceEntity) {
    public static final StreamCodec<ByteBuf, EntityReplayEvent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.LONG, EntityReplayEvent::gameTime,
        ByteBufCodecs.VAR_INT, EntityReplayEvent::eventId,
        ByteBufCodecs.optional(OutsideEntityReplay.STREAM_CODEC), EntityReplayEvent::sourceEntity,
        EntityReplayEvent::new
    );

    public EntityReplayEvent {
        if (eventId < Byte.MIN_VALUE || eventId > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Entity event id must fit in a byte: " + eventId);
        }
        Objects.requireNonNull(sourceEntity, "sourceEntity");
    }

    public byte eventByte() {
        return (byte) this.eventId;
    }
}
