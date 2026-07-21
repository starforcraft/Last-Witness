package com.ultramega.lastwitness;

import com.ultramega.lastwitness.registry.ModCreativeTabs;
import com.ultramega.lastwitness.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(LastWitness.MODID)
public class LastWitness {
    public static final String MODID = "lastwitness";

    public LastWitness(final IEventBus modEventBus, final ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
