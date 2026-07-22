package com.ultramega.lastwitness.mixin;

import com.ultramega.lastwitness.registry.ModAttachments;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "broadcastEntityEvent", at = @At("HEAD"))
    private void lastwitness$recordEntityEvent(final Entity entity, final byte event, final CallbackInfo callbackInfo) {
        if (!(entity instanceof LivingEntity livingEntity)
            || !livingEntity.hasData(ModAttachments.CARRIES_ECHO)
            || !livingEntity.getData(ModAttachments.CARRIES_ECHO)) {
            return;
        }

        EchoTrackerManager.recordEntityEvent(this.getServer(), livingEntity, event);
    }

    @Shadow
    public abstract MinecraftServer getServer();
}
