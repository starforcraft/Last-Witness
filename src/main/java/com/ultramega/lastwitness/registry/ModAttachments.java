package com.ultramega.lastwitness.registry;

import com.ultramega.lastwitness.data.WitnessJournalData;
import com.ultramega.lastwitness.data.WitnessSkill;
import com.ultramega.lastwitness.data.WitnessSkills;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final Supplier<AttachmentType<Boolean>> CARRIES_ECHO = ATTACHMENT_TYPES.register(
        "carries_echo",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("value"))
            .build()
    );
    public static final Supplier<AttachmentType<WitnessJournalData>> WITNESS_JOURNAL = ATTACHMENT_TYPES.register(
        "witness_journal",
        () -> AttachmentType.builder(WitnessJournalData::empty)
            .serialize(WitnessJournalData.CODEC.fieldOf("value"))
            .copyOnDeath()
            .build()
    );

    public static final Supplier<AttachmentType<WitnessSkills>> WITNESS_SKILLS = ATTACHMENT_TYPES.register(
        "witness_skills",
        () -> AttachmentType.builder(WitnessSkills::empty)
            .serialize(WitnessSkills.CODEC.fieldOf("value"))
            .copyOnDeath()
            .build()
    );

    private ModAttachments() {
    }

    public static int skillLevel(final Player player, final WitnessSkill skill) {
        return player.getData(WITNESS_SKILLS).level(skill);
    }

    public static boolean hasSkill(final Player player, final WitnessSkill skill) {
        return player.getData(WITNESS_SKILLS).has(skill);
    }

    public static void setSkillLevel(final Player player, final WitnessSkill skill, final int level) {
        player.setData(WITNESS_SKILLS, player.getData(WITNESS_SKILLS).withLevel(skill, level));
    }
}
