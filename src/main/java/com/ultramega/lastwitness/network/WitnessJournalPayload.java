package com.ultramega.lastwitness.network;

import com.ultramega.lastwitness.client.screen.WitnessJournalScreen;
import com.ultramega.lastwitness.data.WitnessJournalData;
import com.ultramega.lastwitness.data.WitnessJournalData.QualityCounts;
import com.ultramega.lastwitness.data.WitnessSkills;

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
                                    List<String> dimensions,
                                    List<String> biomes,
                                    QualityCounts qualityCounts,
                                    WitnessSkills skills) implements CustomPacketPayload {
    public static final Type<WitnessJournalPayload> TYPE = new Type<>(makeId("witness_journal"));
    private static final int MAX_IDS_PER_COLLECTION = 4096;
    private static final StreamCodec<ByteBuf, List<String>> ID_LIST_STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_IDS_PER_COLLECTION));

    public static final StreamCodec<ByteBuf, WitnessJournalPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::resonance,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::echoesConsumed,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::environmentalDeaths,
        ID_LIST_STREAM_CODEC, WitnessJournalPayload::witnessedEntityTypes,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::attackerCount,
        ByteBufCodecs.VAR_INT, WitnessJournalPayload::deathCauseCount,
        ID_LIST_STREAM_CODEC, WitnessJournalPayload::dimensions,
        ID_LIST_STREAM_CODEC, WitnessJournalPayload::biomes,
        QualityCounts.STREAM_CODEC, WitnessJournalPayload::qualityCounts,
        WitnessSkills.STREAM_CODEC, WitnessJournalPayload::skills,
        WitnessJournalPayload::new
    );

    public WitnessJournalPayload {
        witnessedEntityTypes = immutableIds(witnessedEntityTypes, "victim types");
        dimensions = immutableIds(dimensions, "dimensions");
        biomes = immutableIds(biomes, "biomes");

        qualityCounts = qualityCounts == null ? QualityCounts.empty() : qualityCounts;
        skills = skills == null ? WitnessSkills.empty() : skills;
    }

    private static List<String> immutableIds(final List<String> ids, final String collectionName) {
        final List<String> copy = List.copyOf(ids);
        if (copy.size() > MAX_IDS_PER_COLLECTION) {
            throw new IllegalArgumentException("A witness journal may contain at most " + MAX_IDS_PER_COLLECTION + " " + collectionName);
        }
        return copy;
    }

    public static void handlePayload(final WitnessJournalPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof WitnessJournalScreen screen) {
                screen.updateJournal(payload);
            } else {
                minecraft.setScreen(new WitnessJournalScreen(payload));
            }
        });
    }

    public static WitnessJournalPayload from(final WitnessJournalData journal, final WitnessSkills skills) {
        return new WitnessJournalPayload(
            journal.resonance(),
            journal.echoesConsumed(),
            journal.environmentalDeaths(),
            journal.witnessedEntityTypes(),
            journal.attackerEntityTypes().size(),
            journal.deathCauses().size(),
            journal.dimensions(),
            journal.biomes(),
            journal.qualityCounts(),
            skills
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
