package com.ultramega.lastwitness;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ECHO_MARKER_DURABILITY = BUILDER
            .comment("After how many views into the past the echo marker should break")
            .defineInRange("echoMarkerDurability", 3, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
