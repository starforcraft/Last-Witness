package com.ultramega.lastwitness.items;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.data.EchoMarkedData;
import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class EchoMarkerItem extends Item {
    public EchoMarkerItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(final ItemStack stack, final Player player, final LivingEntity target, final InteractionHand hand) {
        if (stack.has(ModDataComponents.ECHO_MARKED.get())) {
            return InteractionResult.PASS;
        }

        // For some reason using stack.set() doesn't work
        player.getItemInHand(hand).set(ModDataComponents.ECHO_MARKED.get(), new EchoMarkedData(target.getUUID()));

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final EchoMarkedData marked = stack.get(ModDataComponents.ECHO_MARKED.get());
        if (marked == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        final Entity entity = serverLevel.getEntity(marked.entityUUID());
        if (!(entity instanceof LivingEntity markedEntity) || !markedEntity.isAlive()) {
            return InteractionResult.FAIL;
        }

        final Optional<EchoTrack> track = EchoTrackerManager.snapshotActive(serverLevel.getServer(), markedEntity);
        if (track.isEmpty() || track.get().snapshots().isEmpty()) {
            return InteractionResult.FAIL;
        }

        PacketDistributor.sendToPlayer(serverPlayer, ReplayPayload.fromTrack(track.get(), true));

        stack.hurtAndBreak(1, player, hand);

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public int getMaxDamage(final ItemStack stack) {
        // No idea if this is the correct way. Using properties.durability() doesn't work as the configs are loaded after item registration
        return Config.ECHO_MARKER_DURABILITY.get();
    }
}
