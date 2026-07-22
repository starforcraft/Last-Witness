package com.ultramega.lastwitness;

import com.ultramega.lastwitness.client.EchoMarkerActive;
import com.ultramega.lastwitness.client.EchoTooltipHandler;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import static com.ultramega.lastwitness.LastWitness.MODID;

@Mod(value = MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class LastWitnessClient {
    public LastWitnessClient(final ModContainer container) {
        NeoForge.EVENT_BUS.addListener(EchoTooltipHandler::onItemTooltip);

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerConditionalProperties(final RegisterConditionalItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(MODID, "echo_marker_active"), EchoMarkerActive.MAP_CODEC);
    }
}
