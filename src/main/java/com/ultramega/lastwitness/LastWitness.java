package com.ultramega.lastwitness;

import com.ultramega.lastwitness.command.LastWitnessCommands;
import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.network.UnlockWitnessSkillPayload;
import com.ultramega.lastwitness.network.WitnessJournalPayload;
import com.ultramega.lastwitness.registry.ModAttachments;
import com.ultramega.lastwitness.registry.ModCreativeTabs;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.registry.ModItems;
import com.ultramega.lastwitness.registry.ModSounds;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
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
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(LastWitness::registerPayloads);
        NeoForge.EVENT_BUS.addListener(LastWitnessCommands::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToClient(ReplayPayload.TYPE, ReplayPayload.STREAM_CODEC);
        registrar.playToClient(WitnessJournalPayload.TYPE, WitnessJournalPayload.STREAM_CODEC);

        registrar.playToServer(UnlockWitnessSkillPayload.TYPE, UnlockWitnessSkillPayload.STREAM_CODEC, UnlockWitnessSkillPayload::handlePayload);
    }

    public static Identifier makeId(final String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }
}
