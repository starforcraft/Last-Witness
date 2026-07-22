package com.ultramega.lastwitness.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

public record EchoMarkedData(UUID entityUUID) {
    public static final Codec<EchoMarkedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("entityUUID").forGetter(EchoMarkedData::entityUUID)
    ).apply(instance, EchoMarkedData::new));
}
