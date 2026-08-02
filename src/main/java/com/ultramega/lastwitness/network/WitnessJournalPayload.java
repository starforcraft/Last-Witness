package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.client.screen.WitnessJournalScreen;
import com.ultramega.lastwitness.data.WitnessJournalData;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.lastwitness.LastWitness.makeId;

public record WitnessJournalPayload(int resonance,
                                    int echoesConsumed,
                                    int environmentalDeaths,
                                    List<String> witnessedEntityTypes,
                                    int attackerCount,
                                    int deathCauseCount,
                                    int dimensionCount,
                                    int biomeCount,
                                    int fadedCount,
                                    int vividCount,
                                    int turbulentCount,
                                    int impossibleCount) implements CustomPacketPayload {
    public static final Type<WitnessJournalPayload> TYPE = new Type<>(makeId("witness_journal"));
    private static final int MAX_VICTIM_TYPES = 4096;
    private static final StreamCodec<ByteBuf, List<String>> VICTIM_TYPES_STREAM_CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_VICTIM_TYPES));
    public static final StreamCodec<ByteBuf, WitnessJournalPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::resonance,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::echoesConsumed,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::environmentalDeaths,
        VICTIM_TYPES_STREAM_CODEC, WitnessJournalPayload::witnessedEntityTypes,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::attackerCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::deathCauseCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::dimensionCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::biomeCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::fadedCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::vividCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::turbulentCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::impossibleCount,
        WitnessJournalPayload::new
    );

    public WitnessJournalPayload {
        witnessedEntityTypes = List.copyOf(witnessedEntityTypes);
        if (witnessedEntityTypes.size() > MAX_VICTIM_TYPES) {
            throw new IllegalArgumentException("A witness journal may contain at most " + MAX_VICTIM_TYPES + " victim types");
        }
    }

    public static void handlePayload(final WitnessJournalPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new WitnessJournalScreen(payload)));
    }

    public static WitnessJournalPayload from(final WitnessJournalData journal) {
        return new WitnessJournalPayload(
            journal.resonance(),
            journal.echoesConsumed(),
            journal.environmentalDeaths(),
            journal.witnessedEntityTypes(),
            journal.attackerEntityTypes().size(),
            journal.deathCauses().size(),
            journal.dimensions().size(),
            journal.biomes().size(),
            journal.qualityCounts().faded(),
            journal.qualityCounts().vivid(),
            journal.qualityCounts().turbulent(),
            journal.qualityCounts().impossible()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
