package com.ultramega.lastwitness.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record OutsideEntityEvent(long gameTime, int eventId) {
    public static final StreamCodec<ByteBuf, OutsideEntityEvent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.LONG, OutsideEntityEvent::gameTime,
        ByteBufCodecs.VAR_INT, OutsideEntityEvent::eventId,
        OutsideEntityEvent::new
    );

    public static final Codec<OutsideEntityEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("gameTime").forGetter(OutsideEntityEvent::gameTime),
        Codec.intRange(Byte.MIN_VALUE, Byte.MAX_VALUE).fieldOf("eventId").forGetter(OutsideEntityEvent::eventId)
    ).apply(instance, OutsideEntityEvent::new));

    public byte eventByte() {
        return (byte) this.eventId;
    }
}
