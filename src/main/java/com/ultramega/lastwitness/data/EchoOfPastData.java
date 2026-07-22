package com.ultramega.lastwitness.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.WrittenBookContent;

public record EchoOfPastData(String trackerId, String sourceEntityType, Component cause, long timeOfDeath) {
    public static final Codec<EchoOfPastData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("trackerId").forGetter(EchoOfPastData::trackerId),
        Codec.STRING.fieldOf("sourceEntityType").forGetter(EchoOfPastData::sourceEntityType),
        WrittenBookContent.CONTENT_CODEC.fieldOf("cause").forGetter(EchoOfPastData::cause),
        Codec.LONG.fieldOf("timeOfDeath").forGetter(EchoOfPastData::timeOfDeath)
    ).apply(instance, EchoOfPastData::new));
}
