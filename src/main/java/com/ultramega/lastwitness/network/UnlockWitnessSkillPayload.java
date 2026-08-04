package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.data.WitnessJournalData;
import com.ultramega.lastwitness.data.WitnessSkill;
import com.ultramega.lastwitness.data.WitnessSkills;
import com.ultramega.lastwitness.registry.ModAttachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.lastwitness.LastWitness.makeId;

public record UnlockWitnessSkillPayload(String skillId, int level) implements CustomPacketPayload {
    public static final Type<UnlockWitnessSkillPayload> TYPE = new Type<>(makeId("unlock_witness_skill"));
    public static final StreamCodec<ByteBuf, UnlockWitnessSkillPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, UnlockWitnessSkillPayload::skillId,
        ByteBufCodecs.VAR_INT, UnlockWitnessSkillPayload::level,
        UnlockWitnessSkillPayload::new
    );

    public UnlockWitnessSkillPayload {
        skillId = skillId == null ? "" : skillId;
    }

    public static void handlePayload(final UnlockWitnessSkillPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            final WitnessSkill skill = WitnessSkill.byId(payload.skillId()).orElse(null);
            if (skill == null) {
                return;
            }

            final WitnessSkills skills = player.getData(ModAttachments.WITNESS_SKILLS);
            final int currentLevel = skills.level(skill);
            if (payload.level() != currentLevel + 1 || payload.level() > skill.maxLevel()) {
                return;
            }

            final int cost = skill.resonanceCost(payload.level());
            final WitnessJournalData journal = player.getData(ModAttachments.WITNESS_JOURNAL);
            if (journal.resonance() < cost) {
                return;
            }

            final WitnessJournalData updatedJournal = journal.spendResonance(cost);
            final WitnessSkills updatedSkills = skills.withLevel(skill, payload.level());
            player.setData(ModAttachments.WITNESS_JOURNAL, updatedJournal);
            player.setData(ModAttachments.WITNESS_SKILLS, updatedSkills);
            PacketDistributor.sendToPlayer(player, WitnessJournalPayload.from(updatedJournal, updatedSkills));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
