package com.ultramega.lastwitness;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ECHO_MARKER_DURABILITY = BUILDER
            .comment("After how many views into the past the echo marker should break")
            .defineInRange("echoMarkerDurability", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue ECHO_SPAWN_CHANCE = BUILDER
            .comment("Chance that a freshly spawned living entity is marked as carrying an echo (0.0 to 1.0)")
            .defineInRange("echoSpawnChance", 0.01D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue ECHO_TRACK_SECONDS = BUILDER
        .comment("The amount of seconds that should be tracked to the past")
        .defineInRange("echoTrackSeconds", 5, 1, 120);

    static final ModConfigSpec SPEC = BUILDER.build();
}
