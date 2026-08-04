package com.ultramega.lastwitness.data;

import java.util.Locale;
import java.util.Optional;

public enum WitnessSkill {
    MEMORY_CLARITY("memory_clarity", 5, 10, 15, 20),
    LONGER_RECOLLECTION("longer_recollection", 5, 10, 15, 20),
    HOTBAR_VISIBILITY("hotbar_visibility", 5),
    XP_BAR_VISIBILITY("xp_bar_visibility", 5),
    HEALTH_BAR_VISIBILITY("health_bar_visibility", 8),
    FOOD_BAR_VISIBILITY("food_bar_visibility", 8),
    SCREEN_VISIBILITY("screen_visibility", 15);

    private static final String TRANSLATION_PREFIX = "journal.lastwitness.screen.skill.";

    private final String id;
    private final int[] resonanceCosts;

    WitnessSkill(final String id, final int... resonanceCosts) {
        if (resonanceCosts.length < 1 || resonanceCosts.length > 4) {
            throw new IllegalArgumentException("Skill levels must be between 1 and 4");
        }
        for (final int cost : resonanceCosts) {
            if (cost < 1) {
                throw new IllegalArgumentException("Skill resonance costs must be positive");
            }
        }
        this.id = id;
        this.resonanceCosts = resonanceCosts.clone();
    }

    public String id() {
        return this.id;
    }

    public int maxLevel() {
        return this.resonanceCosts.length;
    }

    public int resonanceCost(final int level) {
        if (level < 1 || level > this.maxLevel()) {
            throw new IllegalArgumentException("Unsupported skill level " + level);
        }
        return this.resonanceCosts[level - 1];
    }

    public String titleTranslationKey() {
        return TRANSLATION_PREFIX + this.id;
    }

    public String descriptionTranslationKey() {
        return this.titleTranslationKey() + ".description";
    }

    public static Optional<WitnessSkill> byId(final String id) {
        final String normalized = id.toLowerCase(Locale.ROOT);
        for (final WitnessSkill skill : values()) {
            if (skill.id.equals(normalized)) {
                return Optional.of(skill);
            }
        }
        return Optional.empty();
    }

    public static String levelAsRomanNumeral(final int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> throw new IllegalArgumentException("Unsupported skill level " + level);
        };
    }
}
