package com.ultramega.lastwitness.tracking;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import static com.ultramega.lastwitness.tracking.EchoTracker.MAX_ACTIVE_SNAPSHOTS;

public record EchoTrack(UUID id,
                        UUID sourceEntityId,
                        String sourceEntityType,
                        long timeOfDeath,
                        List<EntitySnapshot> snapshots,
                        List<EntityReplayEvent> entityEvents) {
    public static final Codec<EchoTrack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(EchoTrack::id),
        UUIDUtil.CODEC.fieldOf("sourceEntityId").forGetter(EchoTrack::sourceEntityId),
        Codec.STRING.fieldOf("sourceEntityType").forGetter(EchoTrack::sourceEntityType),
        Codec.LONG.fieldOf("timeOfDeath").forGetter(EchoTrack::timeOfDeath),
        EntitySnapshot.CODEC.listOf(0, MAX_ACTIVE_SNAPSHOTS).fieldOf("snapshots").forGetter(EchoTrack::snapshots),
        EntityReplayEvent.CODEC.listOf(0, EchoTracker.MAX_ENTITY_EVENTS).fieldOf("entityEvents").forGetter(EchoTrack::entityEvents)
    ).apply(instance, EchoTrack::new));

    public EchoTrack {
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);
    }
}
