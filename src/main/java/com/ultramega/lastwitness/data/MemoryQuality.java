package com.ultramega.lastwitness.data;

import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EntityReplayEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

public enum MemoryQuality {
    FADED("faded", 1, ChatFormatting.GRAY),
    VIVID("vivid", 2, ChatFormatting.AQUA),
    TURBULENT("turbulent", 3, ChatFormatting.LIGHT_PURPLE),
    IMPOSSIBLE("impossible", 5, ChatFormatting.GOLD);

    public static final Codec<MemoryQuality> CODEC =
        Codec.STRING.xmap(
            MemoryQuality::bySerializedName,
            MemoryQuality::serializedName
        );

    private static final Set<ResourceKey<DamageType>> IMPOSSIBLE_DAMAGE_TYPES = Set.of(
        DamageTypes.BAD_RESPAWN_POINT,
        DamageTypes.FELL_OUT_OF_WORLD,
        DamageTypes.GENERIC_KILL,
        DamageTypes.OUTSIDE_BORDER
    );

    private final String serializedName;
    private final int baseResonance;
    private final ChatFormatting formatting;

    MemoryQuality(
        final String serializedName,
        final int baseResonance,
        final ChatFormatting formatting
    ) {
        this.serializedName = serializedName;
        this.baseResonance = baseResonance;
        this.formatting = formatting;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public int baseResonance() {
        return this.baseResonance;
    }

    public ChatFormatting formatting() {
        return this.formatting;
    }

    public Component displayName() {
        return Component.translatable(
            "memory_quality.lastwitness." + this.serializedName
        ).withStyle(this.formatting);
    }

    public static MemoryQuality classify(final EchoTrack track, final DamageSource damageSource) {
        final int sourcedEvents = (int) track.entityEvents().stream()
            .filter(event -> event.sourceEntity().isPresent())
            .count();

        final Set<UUID> outsideEntities = new TreeSet<>();
        int outsideEventCount = 0;

        for (final EntityReplayEvent event : track.entityEvents()) {
            if (event.sourceEntity().isEmpty()) {
                continue;
            }

            outsideEntities.add(event.sourceEntity().get().sourceEntity().uuid());

            outsideEventCount += event.sourceEntity().get().entityEvents().size();
        }

        final boolean impossibleDamage = IMPOSSIBLE_DAMAGE_TYPES.stream().anyMatch(damageSource::is);

        if (impossibleDamage
            || outsideEntities.size() >= 3
            || sourcedEvents >= 8
            || outsideEventCount >= 16) {
            return IMPOSSIBLE;
        }

        if (outsideEntities.size() >= 2
            || sourcedEvents >= 4
            || outsideEventCount >= 8
            || track.entityEvents().size() >= 12) {
            return TURBULENT;
        }

        if (!outsideEntities.isEmpty()
            || sourcedEvents > 0
            || track.entityEvents().size() >= 4) {
            return VIVID;
        }

        return FADED;
    }

    public static List<String> involvedEntityTypes(final EchoTrack track) {
        final Set<String> entityTypes = new TreeSet<>();

        for (final EntityReplayEvent event : track.entityEvents()) {
            event.sourceEntity().ifPresent(replay -> entityTypes.add(replay.sourceEntity().type()));
        }

        return List.copyOf(entityTypes);
    }

    public static int involvedEntityCount(final EchoTrack track) {
        final Set<UUID> entityIds = new TreeSet<>();

        for (final EntityReplayEvent event : track.entityEvents()) {
            event.sourceEntity().ifPresent(replay -> entityIds.add(replay.sourceEntity().uuid()));
        }

        return 1 + entityIds.size();
    }

    private static MemoryQuality bySerializedName(final String value) {
        final String normalized = value.toLowerCase(Locale.ROOT);

        for (final MemoryQuality quality : values()) {
            if (quality.serializedName.equals(normalized)) {
                return quality;
            }
        }

        return FADED;
    }
}
