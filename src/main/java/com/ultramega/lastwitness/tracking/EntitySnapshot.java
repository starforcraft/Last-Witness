package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.network.codec.LastWitnessStreamCodecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
    private static final StreamCodec<ByteBuf, String> POSE_CODEC = ByteBufCodecs.stringUtf8(64);

    public static final StreamCodec<ByteBuf, EntitySnapshot> STREAM_CODEC = LastWitnessStreamCodecs.composite(
        ByteBufCodecs.LONG, EntitySnapshot::gameTime,
        ByteBufCodecs.DOUBLE, EntitySnapshot::x,
        ByteBufCodecs.DOUBLE, EntitySnapshot::y,
        ByteBufCodecs.DOUBLE, EntitySnapshot::z,
        ByteBufCodecs.FLOAT, EntitySnapshot::yRot,
        ByteBufCodecs.FLOAT, EntitySnapshot::xRot,
        ByteBufCodecs.FLOAT, EntitySnapshot::bodyYRot,
        ByteBufCodecs.FLOAT, EntitySnapshot::headYRot,
        POSE_CODEC, EntitySnapshot::pose,
        ByteBufCodecs.FLOAT, EntitySnapshot::health,
        ByteBufCodecs.BOOL, EntitySnapshot::onGround,
        ByteBufCodecs.BOOL, EntitySnapshot::sprinting,
        ByteBufCodecs.BOOL, EntitySnapshot::swimming,
        ByteBufCodecs.BOOL, EntitySnapshot::fallFlying,
        ByteBufCodecs.BOOL, EntitySnapshot::usingItem,
        EntitySnapshot::new
    );

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
