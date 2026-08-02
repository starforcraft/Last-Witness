package com.ultramega.lastwitness.data;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.WrittenBookContent;

public record EchoOfPastData(UUID trackerId,
                             String sourceEntityType,
                             Component cause,
                             long timeOfDeath,
                             boolean untouched,
                             MemoryQuality quality,
                             String killerEntityType,
                             String damageType,
                             String dimension,
                             String biome,
                             List<String> involvedEntityTypes,
                             int involvedEntityCount) {
    public static final Codec<EchoOfPastData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("trackerId").forGetter(EchoOfPastData::trackerId),
        Codec.STRING.fieldOf("sourceEntityType").forGetter(EchoOfPastData::sourceEntityType),
        WrittenBookContent.CONTENT_CODEC.fieldOf("cause").forGetter(EchoOfPastData::cause),
        Codec.LONG.fieldOf("timeOfDeath").forGetter(EchoOfPastData::timeOfDeath),
        Codec.BOOL.fieldOf("untouched").forGetter(EchoOfPastData::untouched),
        MemoryQuality.CODEC.fieldOf("quality").forGetter(EchoOfPastData::quality),
        Codec.STRING.fieldOf("killerEntityType").forGetter(EchoOfPastData::killerEntityType),
        Codec.STRING.fieldOf("damageType").forGetter(EchoOfPastData::damageType),
        Codec.STRING.fieldOf("dimension").forGetter(EchoOfPastData::dimension),
        Codec.STRING.fieldOf("biome").forGetter(EchoOfPastData::biome),
        Codec.STRING.listOf().fieldOf("involvedEntityTypes").forGetter(EchoOfPastData::involvedEntityTypes),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("involvedEntityCount").forGetter(EchoOfPastData::involvedEntityCount)
    ).apply(instance, EchoOfPastData::new));

    public EchoOfPastData markPickedUp() {
        return this.untouched
            ? new EchoOfPastData(this.trackerId, this.sourceEntityType, this.cause, this.timeOfDeath, false,
                this.quality, this.killerEntityType, this.damageType, this.dimension, this.biome, this.involvedEntityTypes, this.involvedEntityCount)
            : this;
    }

    public String memorySignature() {
        final String killer = this.killerEntityType.isBlank() ? "environment" : this.killerEntityType;
        return String.join("|",
            this.sourceEntityType,
            killer,
            signaturePart(this.damageType, "unknown_cause"),
            signaturePart(this.dimension, "unknown_dimension"),
            signaturePart(this.biome, "unknown_biome"),
            this.quality.serializedName(),
            String.join(",", this.involvedEntityTypes),
            Integer.toString(this.involvedEntityCount)
        );
    }

    private static String signaturePart(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
