package com.ultramega.lastwitness.tracking;

import com.ultramega.lastwitness.mixin.SynchedEntityDataAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.slf4j.Logger;

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

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_DATA_VALUE_ID = 254;
    private static final int MAX_SYNCHED_DATA_VALUES = MAX_DATA_VALUE_ID + 1;
    private static final AtomicBoolean WARNED_SYNCHED_DATA_FAILURE = new AtomicBoolean();

    private static final String UUID_KEY = "UUID";
    private static final String BODY_Y_ROT_KEY = "LastWitnessBodyYRot";
    private static final String HEAD_Y_ROT_KEY = "LastWitnessHeadYRot";
    private static final String POSE_KEY = "LastWitnessPose";
    private static final String ON_GROUND_KEY = "LastWitnessOnGround";
    private static final String SPRINTING_KEY = "LastWitnessSprinting";
    private static final String SWIMMING_KEY = "LastWitnessSwimming";
    private static final String MAIN_HAND_ITEM_KEY = "LastWitnessMainHandItem";
    private static final String OFF_HAND_ITEM_KEY = "LastWitnessOffHandItem";
    private static final String SYNCHED_ENTITY_DATA_KEY = "LastWitnessSynchedEntityData";

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
        entityData.putByteArray(SYNCHED_ENTITY_DATA_KEY, encodeSynchedEntityData(entity));

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

        this.entityData.getByteArray(SYNCHED_ENTITY_DATA_KEY)
            .ifPresent(data -> applySynchedEntityData(entity, data));
    }

    private static byte[] encodeSynchedEntityData(final LivingEntity entity) {
        final SynchedEntityData.DataItem<?>[] dataItems = ((SynchedEntityDataAccessor) entity.getEntityData()).lastWitness$getItemsById();

        int valueCount = 0;
        for (final SynchedEntityData.DataItem<?> dataItem : dataItems) {
            if (dataItem != null) {
                valueCount++;
            }
        }

        if (valueCount > MAX_SYNCHED_DATA_VALUES) {
            throw new IllegalStateException("Entity has too many synchronized data values: " + valueCount);
        }

        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), entity.registryAccess(), ConnectionType.NEOFORGE);
        try {
            buffer.writeVarInt(valueCount);
            for (final SynchedEntityData.DataItem<?> dataItem : dataItems) {
                if (dataItem == null) {
                    continue;
                }

                final SynchedEntityData.DataValue<?> value = dataItem.value();
                if (value.id() < 0 || value.id() > MAX_DATA_VALUE_ID) {
                    throw new IllegalStateException("Invalid synchronized data id: " + value.id());
                }

                value.write(buffer);
            }

            final byte[] encoded = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), encoded);
            return encoded;
        } finally {
            buffer.release();
        }
    }

    private static void applySynchedEntityData(final LivingEntity entity, final byte[] encoded) {
        if (encoded.length == 0) {
            return;
        }

        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(encoded), entity.registryAccess(), ConnectionType.NEOFORGE);
        try {
            final List<SynchedEntityData.DataValue<?>> values;

            buffer.markReaderIndex();
            if (buffer.readableBytes() >= Integer.BYTES + 1) {
                values = readSynchedEntityDataValues(buffer, false);
            } else {
                buffer.resetReaderIndex();
                values = readSynchedEntityDataValues(buffer, true);
            }

            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing synchronized data bytes: " + buffer.readableBytes());
            }

            final List<SynchedEntityData.DataValue<?>> compatibleValues = compatibleSynchedEntityDataValues(entity, values);
            if (!compatibleValues.isEmpty()) {
                entity.getEntityData().assignValues(compatibleValues);
            }
        } catch (final RuntimeException exception) {
            if (WARNED_SYNCHED_DATA_FAILURE.compareAndSet(false, true)) {
                LOGGER.warn("Could not restore synchronized replay data for {}: {}", entity.getType(), exception.getMessage());
                LOGGER.debug("Synchronized replay data decoding failure", exception);
            }
        } finally {
            buffer.release();
        }
    }

    private static List<SynchedEntityData.DataValue<?>> compatibleSynchedEntityDataValues(final LivingEntity entity, final List<SynchedEntityData.DataValue<?>> values) {
        final SynchedEntityData.DataItem<?>[] targetItems = ((SynchedEntityDataAccessor) entity.getEntityData()).lastWitness$getItemsById();
        final List<SynchedEntityData.DataValue<?>> compatibleValues = new ArrayList<>(values.size());

        for (final SynchedEntityData.DataValue<?> value : values) {
            final int id = value.id();
            if (id < 0 || id >= targetItems.length) {
                break;
            }

            final SynchedEntityData.DataItem<?> targetItem = targetItems[id];
            if (targetItem == null || targetItem.value().serializer() != value.serializer()) {
                // Entity-data IDs are shared only while the source and replay entity follow the same class hierarchy
                // Replays use ReplayMannequin instead of Player, so stop at the first layout divergence instead of assigning unrelated subclass fields
                break;
            }

            compatibleValues.add(value);
        }

        return compatibleValues;
    }

    private static List<SynchedEntityData.DataValue<?>> readSynchedEntityDataValues(final RegistryFriendlyByteBuf buffer, final boolean legacyDoubleIds) {
        final int valueCount = buffer.readVarInt();
        if (valueCount < 0 || valueCount > MAX_SYNCHED_DATA_VALUES) {
            throw new IllegalArgumentException("Invalid synchronized data value count: " + valueCount);
        }

        final List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(valueCount);
        final boolean[] seenIds = new boolean[MAX_SYNCHED_DATA_VALUES];
        for (int index = 0; index < valueCount; index++) {
            final int id = buffer.readUnsignedByte();

            if (legacyDoubleIds) {
                final int repeatedId = buffer.readUnsignedByte();
                if (repeatedId != id) {
                    throw new IllegalArgumentException("Mismatched legacy synchronized data ids: " + id + " and " + repeatedId);
                }
            }

            if (id > MAX_DATA_VALUE_ID || seenIds[id]) {
                throw new IllegalArgumentException("Invalid or duplicate synchronized data id: " + id);
            }

            seenIds[id] = true;
            values.add(SynchedEntityData.DataValue.read(buffer, id));
        }

        return values;
    }

    private static Pose parsePose(final String poseName) {
        try {
            return Pose.valueOf(poseName);
        } catch (final IllegalArgumentException ignored) {
            return Pose.STANDING;
        }
    }
}
