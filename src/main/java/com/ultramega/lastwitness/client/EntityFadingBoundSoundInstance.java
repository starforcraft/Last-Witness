package com.ultramega.lastwitness.client;

import javax.annotation.Nullable;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public class EntityFadingBoundSoundInstance extends AbstractTickableSoundInstance {
    private static final float FADE_OUT_SPEED = 0.08F;

    private final Entity entity;

    public EntityFadingBoundSoundInstance(final SoundEvent event,
                                          final SoundSource source,
                                          final float volume,
                                          final float pitch,
                                          final Entity entity,
                                          final long seed) {
        super(event, source, RandomSource.create(seed));
        this.volume = volume;
        this.pitch = pitch;
        this.entity = entity;
        this.x = (float) this.entity.getX();
        this.y = (float) this.entity.getY();
        this.z = (float) this.entity.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    @Override
    public void tick() {
        if (this.entity.isRemoved()) {
            this.volume = Math.max(0.0F, this.volume - FADE_OUT_SPEED);
            if (this.volume <= 0.0F) {
                this.stop();
            }
            return;
        }

        this.x = (float) this.entity.getX();
        this.y = (float) this.entity.getY();
        this.z = (float) this.entity.getZ();
    }
}
