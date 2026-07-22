package com.ultramega.lastwitness.tracking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EntityReplayEvent(long gameTime, int eventId) {
    public static final StreamCodec<ByteBuf, EntityReplayEvent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.LONG, EntityReplayEvent::gameTime,
        ByteBufCodecs.VAR_INT, EntityReplayEvent::eventId,
        EntityReplayEvent::new
    );

    public EntityReplayEvent {
        if (eventId < Byte.MIN_VALUE || eventId > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Entity event id must fit in a byte: " + eventId);
        }
    }

    public byte eventByte() {
        return (byte) this.eventId;
    }
}
