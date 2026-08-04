package com.ultramega.lastwitness.data;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record WitnessJournalData(int resonance,
                                 int echoesConsumed,
                                 int environmentalDeaths,
                                 List<String> witnessedEntityTypes,
                                 List<String> attackerEntityTypes,
                                 List<String> deathCauses,
                                 List<String> dimensions,
                                 List<String> biomes,
                                 List<String> memorySignatures,
                                 QualityCounts qualityCounts) {
    private static final Codec<Integer> NON_NEGATIVE_INT = Codec.intRange(0, Integer.MAX_VALUE);
    private static final Codec<List<String>> STRING_LIST = Codec.STRING.listOf();

    public static final Codec<WitnessJournalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        NON_NEGATIVE_INT.optionalFieldOf("resonance", 0).forGetter(WitnessJournalData::resonance),
        NON_NEGATIVE_INT.optionalFieldOf("echoesConsumed", 0).forGetter(WitnessJournalData::echoesConsumed),
        NON_NEGATIVE_INT.optionalFieldOf("environmentalDeaths", 0).forGetter(WitnessJournalData::environmentalDeaths),
        STRING_LIST.optionalFieldOf("witnessedEntityTypes", List.of()).forGetter(WitnessJournalData::witnessedEntityTypes),
        STRING_LIST.optionalFieldOf("attackerEntityTypes", List.of()).forGetter(WitnessJournalData::attackerEntityTypes),
        STRING_LIST.optionalFieldOf("deathCauses", List.of()).forGetter(WitnessJournalData::deathCauses),
        STRING_LIST.optionalFieldOf("dimensions", List.of()).forGetter(WitnessJournalData::dimensions),
        STRING_LIST.optionalFieldOf("biomes", List.of()).forGetter(WitnessJournalData::biomes),
        STRING_LIST.optionalFieldOf("memorySignatures", List.of()).forGetter(WitnessJournalData::memorySignatures),
        QualityCounts.CODEC.optionalFieldOf("qualityCounts", QualityCounts.empty()).forGetter(WitnessJournalData::qualityCounts)
    ).apply(instance, WitnessJournalData::new));

    public WitnessJournalData {
        resonance = Math.max(0, resonance);
        echoesConsumed = Math.max(0, echoesConsumed);
        environmentalDeaths = Math.max(0, environmentalDeaths);
        witnessedEntityTypes = canonicalize(witnessedEntityTypes);
        attackerEntityTypes = canonicalize(attackerEntityTypes);
        deathCauses = canonicalize(deathCauses);
        dimensions = canonicalize(dimensions);
        biomes = canonicalize(biomes);
        memorySignatures = canonicalize(memorySignatures);
        qualityCounts = qualityCounts == null ? QualityCounts.empty() : qualityCounts;
    }

    public static WitnessJournalData empty() {
        return new WitnessJournalData(0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), QualityCounts.empty());
    }

    public Update record(final EchoOfPastData echo) {
        final TreeSet<String> victims = new TreeSet<>(this.witnessedEntityTypes);
        final TreeSet<String> attackers = new TreeSet<>(this.attackerEntityTypes);
        final TreeSet<String> causes = new TreeSet<>(this.deathCauses);
        final TreeSet<String> knownDimensions = new TreeSet<>(this.dimensions);
        final TreeSet<String> knownBiomes = new TreeSet<>(this.biomes);
        final TreeSet<String> signatures = new TreeSet<>(this.memorySignatures);

        final boolean newMemory = signatures.add(echo.memorySignature());
        int gained = newMemory ? echo.quality().baseResonance() : 0;
        gained += addNovel(victims, List.of(echo.sourceEntityType()));
        gained += addNovel(attackers, echo.involvedEntityTypes());
        if (!echo.killerEntityType().isBlank() && attackers.add(echo.killerEntityType())) {
            gained++;
        }
        gained += addNovel(causes, List.of(echo.damageType()));
        gained += addNovel(knownDimensions, List.of(echo.dimension()));
        gained += addNovel(knownBiomes, List.of(echo.biome()));

        final WitnessJournalData updated = new WitnessJournalData(
            safeAdd(this.resonance, gained),
            safeAdd(this.echoesConsumed, 1),
            safeAdd(this.environmentalDeaths, echo.killerEntityType().isBlank() ? 1 : 0),
            List.copyOf(victims),
            List.copyOf(attackers),
            List.copyOf(causes),
            List.copyOf(knownDimensions),
            List.copyOf(knownBiomes),
            List.copyOf(signatures),
            this.qualityCounts.increment(echo.quality())
        );
        return new Update(updated, gained, newMemory);
    }

    private static int addNovel(final TreeSet<String> target, final Collection<String> values) {
        int added = 0;
        for (final String value : values) {
            if (value != null && !value.isBlank() && target.add(value)) {
                added++;
            }
        }
        return added;
    }

    private static List<String> canonicalize(final List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        final TreeSet<String> sorted = new TreeSet<>();
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                sorted.add(value);
            }
        }
        return List.copyOf(sorted);
    }

    private static int safeAdd(final int left, final int right) {
        return left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right;
    }

    public WitnessJournalData addResonance(final int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative resonance: " + amount);
        }
        return this.withResonance(safeAdd(this.resonance, amount));
    }

    public WitnessJournalData removeResonance(final int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove negative resonance: " + amount);
        }
        return this.withResonance(Math.max(0, this.resonance - amount));
    }

    public WitnessJournalData spendResonance(final int amount) {
        if (amount < 0 || amount > this.resonance) {
            throw new IllegalArgumentException("Cannot spend " + amount + " resonance from " + this.resonance);
        }
        return this.withResonance(this.resonance - amount);
    }

    private WitnessJournalData withResonance(final int updatedResonance) {
        return new WitnessJournalData(
            updatedResonance,
            this.echoesConsumed,
            this.environmentalDeaths,
            this.witnessedEntityTypes,
            this.attackerEntityTypes,
            this.deathCauses,
            this.dimensions,
            this.biomes,
            this.memorySignatures,
            this.qualityCounts
        );
    }

    public record Update(WitnessJournalData data, int resonanceGained, boolean newMemory) {
    }

    public record QualityCounts(int faded, int vivid, int turbulent, int impossible) {
        public static final Codec<QualityCounts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NON_NEGATIVE_INT.optionalFieldOf("faded", 0).forGetter(QualityCounts::faded),
            NON_NEGATIVE_INT.optionalFieldOf("vivid", 0).forGetter(QualityCounts::vivid),
            NON_NEGATIVE_INT.optionalFieldOf("turbulent", 0).forGetter(QualityCounts::turbulent),
            NON_NEGATIVE_INT.optionalFieldOf("impossible", 0).forGetter(QualityCounts::impossible)
        ).apply(instance, QualityCounts::new));

        public static final StreamCodec<ByteBuf, QualityCounts> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QualityCounts::faded,
            ByteBufCodecs.VAR_INT, QualityCounts::vivid,
            ByteBufCodecs.VAR_INT, QualityCounts::turbulent,
            ByteBufCodecs.VAR_INT, QualityCounts::impossible,
            QualityCounts::new
        );

        public QualityCounts {
            faded = Math.max(0, faded);
            vivid = Math.max(0, vivid);
            turbulent = Math.max(0, turbulent);
            impossible = Math.max(0, impossible);
        }

        public static QualityCounts empty() {
            return new QualityCounts(0, 0, 0, 0);
        }

        public QualityCounts increment(final MemoryQuality quality) {
            return switch (quality) {
                case FADED -> new QualityCounts(safeAdd(this.faded, 1), this.vivid, this.turbulent, this.impossible);
                case VIVID -> new QualityCounts(this.faded, safeAdd(this.vivid, 1), this.turbulent, this.impossible);
                case TURBULENT -> new QualityCounts(this.faded, this.vivid, safeAdd(this.turbulent, 1), this.impossible);
                case IMPOSSIBLE -> new QualityCounts(this.faded, this.vivid, this.turbulent, safeAdd(this.impossible, 1));
            };
        }
    }
}
