package com.ultramega.lastwitness.client;

import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.registry.ModItems;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class EchoTooltipHandler {
    private static final long TICKS_PER_DAY = 24_000L;
    private static final long TICKS_PER_HOUR = 1_000L;
    private static final long DAWN_OFFSET_TICKS = 6_000L;

    private EchoTooltipHandler() {
    }

    public static void onItemTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        if (!stack.has(ModDataComponents.ECHO_OF_PAST.get())) {
            return;
        }

        final EchoOfPastData echo = stack.get(ModDataComponents.ECHO_OF_PAST.get());
        if (echo == null) {
            return;
        }

        append(event.getToolTip()::add, echo, event.getFlags(), stack.is(ModItems.ECHO_OF_PAST.get()));
    }

    public static void append(final Consumer<Component> tooltip,
                              final EchoOfPastData echo,
                              final TooltipFlag tooltipFlag,
                              final boolean isEchoOfPastItem) {
        if (!isEchoOfPastItem) {
            tooltip.accept(Component.empty());
            tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.header")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.source", sourceEntityName(echo.sourceEntityType()))
            .withStyle(ChatFormatting.GRAY));

        tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.time_of_death", dayNumber(echo.timeOfDeath()), clockTime(echo.timeOfDeath()))
            .withStyle(ChatFormatting.GRAY));

        tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.cause", echo.cause().getString())
            .withStyle(ChatFormatting.GRAY));

        if (tooltipFlag.isAdvanced()) {
            tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.tracker_id", echo.trackerId())
                .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (isEchoOfPastItem) {
            tooltip.accept(Component.translatable("item.lastwitness.echo_of_past.help")
                .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.help_1")
                .withStyle(ChatFormatting.AQUA));
            tooltip.accept(Component.translatable("tooltip.lastwitness.echo_of_past.help_2")
                .withStyle(ChatFormatting.AQUA));
        }
    }

    private static Component sourceEntityName(final String entityTypeId) {
        final int separator = entityTypeId.indexOf(':');
        if (separator <= 0 || separator == entityTypeId.length() - 1) {
            return Component.literal(entityTypeId);
        }

        final String namespace = entityTypeId.substring(0, separator);
        final String path = entityTypeId.substring(separator + 1).replace('/', '.');
        return Component.translatable("entity." + namespace + "." + path);
    }

    private static long dayNumber(final long gameTime) {
        return Math.floorDiv(gameTime, TICKS_PER_DAY) + 1L;
    }

    private static String clockTime(final long gameTime) {
        final long dayTime = Math.floorMod(gameTime, TICKS_PER_DAY);
        final long clockTicks = Math.floorMod(dayTime + DAWN_OFFSET_TICKS, TICKS_PER_DAY);
        final int hour = (int) (clockTicks / TICKS_PER_HOUR);
        final int minute = (int) ((clockTicks % TICKS_PER_HOUR) * 60L / TICKS_PER_HOUR);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }
}
