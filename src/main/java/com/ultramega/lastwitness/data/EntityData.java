package com.ultramega.lastwitness.data;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EntityData(UUID uuid, String type) {
    public static final StreamCodec<ByteBuf, String> ENTITY_TYPE_STREAM_CODEC = ByteBufCodecs.stringUtf8(256);

    public static final StreamCodec<ByteBuf, EntityData> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, EntityData::uuid,
        ENTITY_TYPE_STREAM_CODEC, EntityData::type,
        EntityData::new
    );
}
