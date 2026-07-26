package com.ultramega.lastwitness.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.WrittenBookContent;

public record EchoOfPastData(UUID trackerId, String sourceEntityType, Component cause, long timeOfDeath, boolean untouched) {
    public static final Codec<EchoOfPastData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("trackerId").forGetter(EchoOfPastData::trackerId),
        Codec.STRING.fieldOf("sourceEntityType").forGetter(EchoOfPastData::sourceEntityType),
        WrittenBookContent.CONTENT_CODEC.fieldOf("cause").forGetter(EchoOfPastData::cause),
        Codec.LONG.fieldOf("timeOfDeath").forGetter(EchoOfPastData::timeOfDeath),
        Codec.BOOL.fieldOf("untouched").forGetter(EchoOfPastData::untouched)
    ).apply(instance, EchoOfPastData::new));

    public EchoOfPastData markPickedUp() {
        return this.untouched ? new EchoOfPastData(this.trackerId, this.sourceEntityType, this.cause, this.timeOfDeath, false) : this;
    }
}
