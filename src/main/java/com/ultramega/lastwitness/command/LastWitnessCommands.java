package com.ultramega.lastwitness.command;

import com.ultramega.lastwitness.data.WitnessJournalData;
import com.ultramega.lastwitness.data.WitnessSkill;
import com.ultramega.lastwitness.data.WitnessSkills;
import com.ultramega.lastwitness.registry.ModAttachments;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class LastWitnessCommands {
    private LastWitnessCommands() {
    }

    public static void register(final RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(MODID)
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("resonance")
            .then(Commands.literal("give")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> changeResonance(context, true)))))
            .then(Commands.literal("remove")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> changeResonance(context, false))))));

        final RequiredArgumentBuilder<CommandSourceStack, ?> giveTargets = Commands.argument("targets", EntityArgument.players());
        final RequiredArgumentBuilder<CommandSourceStack, ?> removeTargets = Commands.argument("targets", EntityArgument.players());
        for (final WitnessSkill skill : WitnessSkill.values()) {
            giveTargets.then(skillOperation(skill, true));
            removeTargets.then(skillOperation(skill, false));
        }

        root.then(Commands.literal("skill")
            .then(Commands.literal("give").then(giveTargets))
            .then(Commands.literal("remove").then(removeTargets)));

        dispatcher.register(root);
    }

    private static int changeResonance(final CommandContext<CommandSourceStack> context,
                                       final boolean give) throws CommandSyntaxException {
        final Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        final int amount = IntegerArgumentType.getInteger(context, "amount");
        int changedPlayers = 0;

        for (final ServerPlayer player : targets) {
            final WitnessJournalData journal = player.getData(ModAttachments.WITNESS_JOURNAL);
            final WitnessJournalData updated = give
                ? journal.addResonance(amount)
                : journal.removeResonance(amount);

            if (updated.resonance() != journal.resonance()) {
                player.setData(ModAttachments.WITNESS_JOURNAL, updated);
                changedPlayers++;
            }
        }

        final int changed = changedPlayers;
        final String translationKey = give
            ? "command.lastwitness.resonance.give"
            : "command.lastwitness.resonance.remove";
        context.getSource().sendSuccess(
            () -> Component.translatable(translationKey, amount, changed),
            true
        );
        return changed;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> skillOperation(final WitnessSkill skill,
                                                                              final boolean give) {
        return Commands.literal(skill.id())
            .executes(context -> changeSkill(context, skill, 1, give))
            .then(Commands.argument("levels", IntegerArgumentType.integer(1, skill.maxLevel()))
                .executes(context -> changeSkill(
                    context,
                    skill,
                    IntegerArgumentType.getInteger(context, "levels"),
                    give
                )));
    }

    private static int changeSkill(final CommandContext<CommandSourceStack> context,
                                   final WitnessSkill skill,
                                   final int levels,
                                   final boolean give) throws CommandSyntaxException {
        final Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int changedPlayers = 0;

        for (final ServerPlayer player : targets) {
            final WitnessSkills skills = player.getData(ModAttachments.WITNESS_SKILLS);
            final int currentLevel = skills.level(skill);
            final int updatedLevel = give
                ? Math.min(skill.maxLevel(), currentLevel + levels)
                : Math.max(0, currentLevel - levels);

            if (updatedLevel != currentLevel) {
                player.setData(ModAttachments.WITNESS_SKILLS, skills.withLevel(skill, updatedLevel));
                changedPlayers++;
            }
        }

        final int changed = changedPlayers;
        final String translationKey = give
            ? "command.lastwitness.skill.give"
            : "command.lastwitness.skill.remove";
        context.getSource().sendSuccess(
            () -> Component.translatable(
                translationKey,
                levels,
                Component.translatable(skill.titleTranslationKey()),
                changed
            ),
            true
        );
        return changed;
    }
}
