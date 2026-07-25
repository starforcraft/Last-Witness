package com.ultramega.lastwitness.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);

    public static final Holder<SoundEvent> SUDDEN = SOUND_EVENTS.register("sudden", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> GHOST_ENVIRONMENT = SOUND_EVENTS.register("ghost_environment", SoundEvent::createVariableRangeEvent);

    private ModSounds() {
    }
}
