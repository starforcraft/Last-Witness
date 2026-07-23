package com.ultramega.lastwitness.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.component.ResolvableProfile;

public class ReplayMannequin extends ClientMannequin {
    public ReplayMannequin(final ClientLevel level, final UUID sourceProfileId) {
        super(level, Minecraft.getInstance().playerSkinRenderCache());
        this.entityData.set(DATA_PROFILE, ResolvableProfile.createUnresolved(sourceProfileId));
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }
}
