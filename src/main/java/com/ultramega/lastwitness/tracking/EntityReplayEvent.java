package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.data.OutsideEntityReplay;

import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

    public static final Codec<EntityReplayEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("gameTime").forGetter(EntityReplayEvent::gameTime),
        Codec.intRange(Byte.MIN_VALUE, Byte.MAX_VALUE).fieldOf("eventId").forGetter(EntityReplayEvent::eventId),
        OutsideEntityReplay.CODEC.optionalFieldOf("sourceEntity").forGetter(EntityReplayEvent::sourceEntity)
    ).apply(instance, EntityReplayEvent::new));

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
