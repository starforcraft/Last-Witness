package com.ultramega.lastwitness;

import com.ultramega.lastwitness.events.EchoExtractionHandler;
import com.ultramega.lastwitness.events.EchoGhostHandler;
import com.ultramega.lastwitness.events.EchoSpawnHandler;
import com.ultramega.lastwitness.events.EchoTrackingHandler;
import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.registry.ModAttachments;
import com.ultramega.lastwitness.registry.ModCreativeTabs;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.registry.ModItems;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(LastWitness.MODID)
public final class LastWitness {
    public static final String MODID = "lastwitness";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LastWitness(final IEventBus modEventBus, final ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(LastWitness::registerPayloads);

        NeoForge.EVENT_BUS.addListener(EchoSpawnHandler::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(EchoExtractionHandler::onItemEntityTick);
        NeoForge.EVENT_BUS.addListener(EchoTrackingHandler::onEntityTick);
        NeoForge.EVENT_BUS.addListener(EchoTrackingHandler::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(EchoTrackingHandler::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(EchoGhostHandler::onItemEntityTick);
        NeoForge.EVENT_BUS.addListener(EchoGhostHandler::onItemPickup);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToClient(ReplayPayload.TYPE, ReplayPayload.STREAM_CODEC);
    }
}
