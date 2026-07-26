package com.ultramega.lastwitness.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

public record EchoMarkedData(UUID entityUUID, UUID trackerId) {
    public static final Codec<EchoMarkedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("entityUUID").forGetter(EchoMarkedData::entityUUID),
        UUIDUtil.CODEC.fieldOf("trackerId").forGetter(EchoMarkedData::trackerId)
    ).apply(instance, EchoMarkedData::new));
}
