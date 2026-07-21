package com.ultramega.lastwitness.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LAST_WITNESS_TAB = CREATIVE_MODE_TABS.register("last_witness", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.lastwitness"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> ModItems.ECHO_OF_PAST.get().getDefaultInstance())
        .displayItems(ModItems.ITEMS.getEntries())
        .build());

    private ModCreativeTabs() {
    }
}
