package com.ultramega.lastwitness.compat.jei;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.LastWitness;
import com.ultramega.lastwitness.registry.ModItems;

import java.math.BigDecimal;
import java.math.RoundingMode;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class LastWitnessJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(LastWitness.MODID, "jei_plugin");

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        final String chance = formatChance(Config.ECHO_SPAWN_CHANCE.get());
        registration.addIngredientInfo(ModItems.ECHO_OF_PAST.get(),
            Component.translatable("jei.lastwitness.echo_of_past.chance", chance),
            Component.translatable("jei.lastwitness.echo_of_past.drop"),
            Component.translatable("jei.lastwitness.echo_of_past.extract"));
    }

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    private static String formatChance(final double chance) {
        return BigDecimal.valueOf(chance * 100.0D)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString() + "%";
    }
}
