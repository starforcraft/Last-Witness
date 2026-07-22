package com.ultramega.lastwitness.tracking;

import net.minecraft.world.entity.LivingEntity;

public record EntitySnapshot(long gameTime,
                             double x,
                             double y,
                             double z,
                             float yRot,
                             float xRot,
                             float bodyYRot,
                             float headYRot,
                             String pose,
                             float health,
                             boolean onGround,
                             boolean sprinting,
                             boolean swimming,
                             boolean fallFlying,
                             boolean usingItem) {
    public static EntitySnapshot capture(final LivingEntity entity) {
        return new EntitySnapshot(
            entity.level().getGameTime(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.getYRot(),
            entity.getXRot(),
            entity.yBodyRot,
            entity.yHeadRot,
            entity.getPose().name(),
            entity.getHealth(),
            entity.onGround(),
            entity.isSprinting(),
            entity.isSwimming(),
            entity.isFallFlying(),
            entity.isUsingItem()
        );
    }
}
