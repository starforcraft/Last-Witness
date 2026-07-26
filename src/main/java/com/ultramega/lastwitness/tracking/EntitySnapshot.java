package com.ultramega.lastwitness.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public record EntitySnapshot(long gameTime, CompoundTag entityData) {
    public static final StreamCodec<ByteBuf, EntitySnapshot> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.LONG, EntitySnapshot::gameTime,
        ByteBufCodecs.COMPOUND_TAG, EntitySnapshot::entityData,
        EntitySnapshot::new
    );
    public static final Codec<EntitySnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("gameTime").forGetter(EntitySnapshot::gameTime),
        CompoundTag.CODEC.fieldOf("entityData").forGetter(EntitySnapshot::entityData)
    ).apply(instance, EntitySnapshot::new));

    private static final String UUID_KEY = "UUID";
    private static final String BODY_Y_ROT_KEY = "LastWitnessBodyYRot";
    private static final String HEAD_Y_ROT_KEY = "LastWitnessHeadYRot";
    private static final String POSE_KEY = "LastWitnessPose";
    private static final String ON_GROUND_KEY = "LastWitnessOnGround";
    private static final String SPRINTING_KEY = "LastWitnessSprinting";
    private static final String SWIMMING_KEY = "LastWitnessSwimming";
    private static final String MAIN_HAND_ITEM_KEY = "LastWitnessMainHandItem";
    private static final String OFF_HAND_ITEM_KEY = "LastWitnessOffHandItem";

    public EntitySnapshot {
        entityData = entityData.copy();
    }

    @Override
    public CompoundTag entityData() {
        return this.entityData.copy();
    }

    public static EntitySnapshot capture(final LivingEntity entity) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);

        // These are not included in entity.saveWithoutId() or not included in the Player reliably, so we have to save and load them ourselves
        output.putFloat(BODY_Y_ROT_KEY, entity.yBodyRot);
        output.putFloat(HEAD_Y_ROT_KEY, entity.yHeadRot);
        output.putString(POSE_KEY, entity.getPose().name());
        output.putBoolean(ON_GROUND_KEY, entity.onGround());
        output.putBoolean(SPRINTING_KEY, entity.isSprinting());
        output.putBoolean(SWIMMING_KEY, entity.isSwimming());
        output.store(MAIN_HAND_ITEM_KEY, ItemStack.OPTIONAL_CODEC, entity.getMainHandItem().copy());
        output.store(OFF_HAND_ITEM_KEY, ItemStack.OPTIONAL_CODEC, entity.getOffhandItem().copy());

        final CompoundTag entityData = output.buildResult();
        entityData.remove(UUID_KEY);

        return new EntitySnapshot(entity.level().getGameTime(), entityData);
    }

    public void loadInto(final LivingEntity entity) {
        final TagValueInput input = (TagValueInput) TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), this.entityData);
        entity.load(input);

        entity.setYBodyRot(input.getFloatOr(BODY_Y_ROT_KEY, entity.getYRot()));
        entity.setYHeadRot(input.getFloatOr(HEAD_Y_ROT_KEY, entity.getYRot()));
        entity.setPose(parsePose(input.getStringOr(POSE_KEY, Pose.STANDING.name())));
        entity.setOnGround(input.getBooleanOr(ON_GROUND_KEY, entity.onGround()));
        entity.setSprinting(input.getBooleanOr(SPRINTING_KEY, entity.isSprinting()));
        entity.setSwimming(input.getBooleanOr(SWIMMING_KEY, entity.isSwimming()));

        input.read(MAIN_HAND_ITEM_KEY, ItemStack.OPTIONAL_CODEC)
            .ifPresent(stack -> entity.setItemInHand(InteractionHand.MAIN_HAND, stack.copy()));
        input.read(OFF_HAND_ITEM_KEY, ItemStack.OPTIONAL_CODEC)
            .ifPresent(stack -> entity.setItemInHand(InteractionHand.OFF_HAND, stack.copy()));
    }

    private static Pose parsePose(final String poseName) {
        try {
            return Pose.valueOf(poseName);
        } catch (final IllegalArgumentException ignored) {
            return Pose.STANDING;
        }
    }
}
