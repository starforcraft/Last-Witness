package com.ultramega.lastwitness.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record OutsideEntityEvent(long gameTime, int eventId) {
    public static final StreamCodec<ByteBuf, OutsideEntityEvent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.LONG, OutsideEntityEvent::gameTime,
        ByteBufCodecs.VAR_INT, OutsideEntityEvent::eventId,
        OutsideEntityEvent::new
    );

    public byte eventByte() {
        return (byte) this.eventId;
    }
}
