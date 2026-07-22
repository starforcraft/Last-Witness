package com.ultramega.lastwitness.tracking;

import java.util.List;
import java.util.UUID;

public record EchoTrack(String id,
                        UUID sourceEntityId,
                        String sourceEntityType,
                        long timeOfDeath,
                        List<EntitySnapshot> snapshots) {
    public EchoTrack {
        snapshots = List.copyOf(snapshots);
    }
}
