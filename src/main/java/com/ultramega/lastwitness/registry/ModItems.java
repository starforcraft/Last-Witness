package com.ultramega.lastwitness.registry;

import com.ultramega.lastwitness.items.EchoMarkerItem;
import com.ultramega.lastwitness.items.EchoOfPastItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<Item> ECHO_OF_PAST = ITEMS.registerItem("echo_of_past", EchoOfPastItem::new,
        p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ECHO_MARKER = ITEMS.registerItem("echo_marker", EchoMarkerItem::new,
        p -> p.stacksTo(1).fireResistant());
}
