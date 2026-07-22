package com.ultramega.lastwitness.tracking;

import java.util.List;
import java.util.UUID;

public record EchoTrack(String id,
                        UUID sourceEntityId,
                        String sourceEntityType,
                        long timeOfDeath,
                        List<EntitySnapshot> snapshots,
                        List<EntityReplayEvent> entityEvents) {
    public EchoTrack {
        snapshots = List.copyOf(snapshots);
        entityEvents = List.copyOf(entityEvents);
    }
}
