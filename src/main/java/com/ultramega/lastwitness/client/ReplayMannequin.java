package com.ultramega.lastwitness.client;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.item.component.ResolvableProfile;

public class ReplayMannequin extends ClientMannequin {
    private final CompletableFuture<?> skinLookup;
    private boolean skinLookupCompletedLastTick;
    private boolean skinReady;

    public ReplayMannequin(final ClientLevel level, final UUID sourceProfileId) {
        this(level, sourceProfileId, Minecraft.getInstance().playerSkinRenderCache());
    }

    private ReplayMannequin(final ClientLevel level,
                            final UUID sourceProfileId,
                            final PlayerSkinRenderCache skinRenderCache) {
        super(level, skinRenderCache);

        final ResolvableProfile profile = ResolvableProfile.createUnresolved(sourceProfileId);
        this.skinLookup = skinRenderCache.lookup(profile);
        this.entityData.set(DATA_PROFILE, profile);
    }

    @Override
    public void tick() {
        super.tick();

        // This stuff is needed, as else the Mannequin will have a default skin on the first tick
        if (this.skinReady) {
            return;
        }
        if (this.skinLookupCompletedLastTick) {
            this.skinReady = true;
        } else if (this.skinLookup.isDone()) {
            this.skinLookupCompletedLastTick = true;
        }
    }

    public boolean isSkinReady() {
        return this.skinReady;
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
