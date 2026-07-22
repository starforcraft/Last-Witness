package com.ultramega.lastwitness.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EchoOfPastData(String trackerId, String sourceEntityType, long timeOfDeath) {
    public static final Codec<EchoOfPastData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("trackerId").forGetter(EchoOfPastData::trackerId),
        Codec.STRING.fieldOf("sourceEntityType").forGetter(EchoOfPastData::sourceEntityType),
        Codec.LONG.fieldOf("timeOfDeath").forGetter(EchoOfPastData::timeOfDeath)
    ).apply(instance, EchoOfPastData::new));
}
