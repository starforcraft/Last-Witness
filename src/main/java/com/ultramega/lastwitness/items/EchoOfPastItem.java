package com.ultramega.lastwitness.items;

import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.tracking.EchoTrack;
import com.ultramega.lastwitness.tracking.EchoTrackerManager;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class EchoOfPastItem extends Item {
    public EchoOfPastItem(final Item.Properties builder) {
        super(builder);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(ModDataComponents.ECHO_OF_PAST.get()) || stack.get(ModDataComponents.ECHO_OF_PAST.get()) == null) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(final ItemStack stack, final Level level, final LivingEntity consumer) {
        final EchoOfPastData echo = stack.get(ModDataComponents.ECHO_OF_PAST.get());
        if (echo == null) {
            return stack;
        }

        if (level instanceof ServerLevel serverLevel && consumer instanceof ServerPlayer serverPlayer) {
            final Optional<EchoTrack> track = EchoTrackerManager.findCompleted(serverLevel.getServer(), echo.trackerId());
            if (track.isEmpty() || track.get().snapshots().isEmpty()) {
                return stack;
            }

            PacketDistributor.sendToPlayer(serverPlayer, ReplayPayload.fromTrack(track.get(), true));
        }

        return super.finishUsingItem(stack, level, consumer);
    }
}
