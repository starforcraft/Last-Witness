package com.ultramega.lastwitness.items;

import com.ultramega.lastwitness.data.WitnessJournalData;
import com.ultramega.lastwitness.data.WitnessSkills;
import com.ultramega.lastwitness.network.WitnessJournalPayload;
import com.ultramega.lastwitness.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class WitnessJournalItem extends Item {
    public WitnessJournalItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        final WitnessJournalData journal = serverPlayer.getData(ModAttachments.WITNESS_JOURNAL);
        final WitnessSkills skills = serverPlayer.getData(ModAttachments.WITNESS_SKILLS);
        PacketDistributor.sendToPlayer(serverPlayer, WitnessJournalPayload.from(journal, skills));
        return InteractionResult.SUCCESS_SERVER;
    }
}
