package com.ultramega.lastwitness.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record WitnessSkills(Map<String, Integer> levels) {
    public static final Codec<WitnessSkills> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
        .xmap(WitnessSkills::new, WitnessSkills::levels);
    public static final StreamCodec<ByteBuf, WitnessSkills> STREAM_CODEC = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(WitnessSkill.values().length))
        .map(WitnessSkills::fromOrderedLevels, WitnessSkills::asOrderedLevels);

    public WitnessSkills {
        final Map<String, Integer> normalized = new HashMap<>();
        final Map<String, Integer> source = levels == null ? Map.of() : levels;

        for (final WitnessSkill skill : WitnessSkill.values()) {
            final int level = Math.clamp(source.getOrDefault(skill.id(), 0), 0, skill.maxLevel());
            if (level > 0) {
                normalized.put(skill.id(), level);
            }
        }

        levels = Map.copyOf(normalized);
    }

    public static WitnessSkills empty() {
        return new WitnessSkills(Map.of());
    }

    public static WitnessSkills fromOrderedLevels(final List<Integer> levels) {
        if (levels.size() != WitnessSkill.values().length) {
            throw new IllegalArgumentException(
                "Expected " + WitnessSkill.values().length + " skill levels, got " + levels.size()
            );
        }

        final Map<String, Integer> mappedLevels = new HashMap<>();
        for (final WitnessSkill skill : WitnessSkill.values()) {
            mappedLevels.put(skill.id(), levels.get(skill.ordinal()));
        }
        return new WitnessSkills(mappedLevels);
    }

    public int level(final WitnessSkill skill) {
        return this.levels.getOrDefault(skill.id(), 0);
    }

    public boolean has(final WitnessSkill skill) {
        return this.level(skill) > 0;
    }

    public Set<WitnessSkill> unlockedSkills() {
        final EnumSet<WitnessSkill> unlocked = EnumSet.noneOf(WitnessSkill.class);
        for (final WitnessSkill skill : WitnessSkill.values()) {
            if (this.has(skill)) {
                unlocked.add(skill);
            }
        }
        return Set.copyOf(unlocked);
    }

    public WitnessSkills withLevel(final WitnessSkill skill, final int level) {
        final int normalizedLevel = Math.clamp(level, 0, skill.maxLevel());
        final Map<String, Integer> updated = new HashMap<>(this.levels);
        if (normalizedLevel == 0) {
            updated.remove(skill.id());
        } else {
            updated.put(skill.id(), normalizedLevel);
        }
        return new WitnessSkills(updated);
    }

    public List<Integer> asOrderedLevels() {
        final List<Integer> result = new ArrayList<>(WitnessSkill.values().length);
        for (final WitnessSkill skill : WitnessSkill.values()) {
            result.add(this.level(skill));
        }
        return List.copyOf(result);
    }
}
