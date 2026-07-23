package com.ultramega.lastwitness.registry;

import com.ultramega.lastwitness.items.EchoMarkerItem;
import com.ultramega.lastwitness.items.EchoOfPastItem;

import java.util.function.Supplier;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final FoodProperties CONSUMABLE = (new FoodProperties.Builder()).nutrition(4).saturationModifier(0.3F).alwaysEdible().build();

    public static final Supplier<Item> ECHO_OF_PAST = ITEMS.registerItem("echo_of_past", EchoOfPastItem::new,
        p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).food(CONSUMABLE));
    public static final Supplier<Item> ECHO_MARKER = ITEMS.registerItem("echo_marker", EchoMarkerItem::new,
        p -> p.stacksTo(1).fireResistant().rarity(Rarity.RARE));
}
