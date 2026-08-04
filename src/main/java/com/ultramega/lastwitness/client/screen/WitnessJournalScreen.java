package com.ultramega.lastwitness.client.screen;

import com.ultramega.lastwitness.client.screen.widget.JournalButton;
import com.ultramega.lastwitness.client.screen.widget.SkillLevelButton;
import com.ultramega.lastwitness.data.WitnessSkill;
import com.ultramega.lastwitness.network.UnlockWitnessSkillPayload;
import com.ultramega.lastwitness.network.WitnessJournalPayload;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class WitnessJournalScreen extends Screen {
    private static final int BOOK_WIDTH = 304;
    private static final int BOOK_HEIGHT = 186;
    private static final int PAGE_MARGIN = 10;
    private static final int PAGE_WIDTH = 145;
    private static final int TAB_WIDTH = 56;
    private static final int TAB_GAP = 2;
    private static final float TITLE_SCALE = 2.0F;
    private static final int TITLE_CENTER_GAP = 5;

    private static final int BACKDROP = 0xB0100806;
    private static final int SHADOW = 0x80000000;
    private static final int COVER = 0xFF3A1F17;
    private static final int COVER_EDGE = 0xFF6F3D2E;
    private static final int PAGE = 0xFFF3E5C0;
    private static final int PAGE_EDGE = 0xFFE2C995;
    private static final int SPINE = 0xFF8A5A32;
    private static final int INK = 0xFF392A21;
    private static final int MUTED_INK = 0xFF6D5A49;
    private static final int ACCENT = 0xFF7A3E62;
    private static final int RULE = 0xFFBEA77A;
    private static final int PANEL_SHADE = 0x26A67B4A;
    private static final int SKILL_AREA_TOP = 32;
    private static final int SKILL_AREA_HEIGHT = 127;
    private static final int SKILL_CARD_GAP = 4;
    private static final int SKILL_ROWS_PER_PAGE = 2;
    private static final int SKILL_LEVEL_NODE_WIDTH = 24;
    private static final int SKILL_LEVEL_NODE_HEIGHT = 14;
    private static final int SKILL_LEVEL_NODE_GAP = 7;
    private static final int SKILL_UNLOCKED = 0xFF3D9B4A;
    private static final List<List<WitnessSkill>> SKILL_PAGES = List.of(
        List.of(WitnessSkill.MEMORY_CLARITY, WitnessSkill.LONGER_RECOLLECTION, WitnessSkill.HOTBAR_VISIBILITY, WitnessSkill.XP_BAR_VISIBILITY),
        List.of(WitnessSkill.HEALTH_BAR_VISIBILITY, WitnessSkill.FOOD_BAR_VISIBILITY, WitnessSkill.SCREEN_VISIBILITY)
    );

    private WitnessJournalPayload journal;
    private final Map<String, Optional<LivingEntity>> entityCache = new HashMap<>();
    private final List<SkillLevelButton> skillLevelButtons = new ArrayList<>();
    private final Map<Section, Integer> sectionStarts = new EnumMap<>(Section.class);
    private final Map<Section, Button> sectionButtons = new EnumMap<>(Section.class);
    private final List<JournalPage> pages;
    private int pageIndex;
    private int animationTicks;
    private Button previousButton;
    private Button nextButton;

    public WitnessJournalScreen(final WitnessJournalPayload journal) {
        super(Component.translatable("journal.lastwitness.header"));
        this.journal = journal;
        this.pages = this.createPages();
    }

    @Override
    protected void init() {
        super.init();
        this.sectionButtons.clear();

        final int left = (this.width - BOOK_WIDTH) / 2;
        final int top = (this.height - BOOK_HEIGHT) / 2;
        final int tabsWidth = TAB_WIDTH * Section.values().length + TAB_GAP * (Section.values().length - 1);
        int tabX = left + (BOOK_WIDTH - tabsWidth) / 2;
        final int tabY = top - 17;

        for (final Section section : Section.values()) {
            final Button button = this.addRenderableWidget(new JournalButton(
                tabX,
                tabY,
                TAB_WIDTH,
                14,
                Component.translatable(section.translationKey),
                ignored -> this.jumpTo(section)
            ));
            this.sectionButtons.put(section, button);
            tabX += TAB_WIDTH + TAB_GAP;
        }

        final int buttonY = top + BOOK_HEIGHT - 21;
        this.previousButton = this.addRenderableWidget(
            new JournalButton(left + 12, buttonY, 18, 14, Component.literal("<"), ignored -> this.changePage(-1))
        );
        this.nextButton = this.addRenderableWidget(
            new JournalButton(left + BOOK_WIDTH - 30, buttonY, 18, 14, Component.literal(">"), ignored -> this.changePage(1))
        );
        this.createSkillLevelButtons(left, top);
        this.updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        this.animationTicks++;
        for (final Optional<LivingEntity> cached : this.entityCache.values()) {
            cached.ifPresent(entity -> entity.tickCount++);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BACKDROP);
        final int left = (this.width - BOOK_WIDTH) / 2;
        final int top = (this.height - BOOK_HEIGHT) / 2;
        this.extractBook(graphics, left, top);
        this.extractCurrentPage(graphics, left, top, partialTick);
        this.extractFooter(graphics, left, top);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<JournalPage> createPages() {
        final List<JournalPage> result = new ArrayList<>();

        this.sectionStarts.put(Section.GENERAL, 0);
        result.add(new JournalPage(PageKind.GENERAL, "", 0, 1));

        this.addCollectionPages(result, Section.VICTIMS, PageKind.VICTIM_INDEX, PageKind.VICTIM,
            this.journal.witnessedEntityTypes());
        this.addCollectionPages(result, Section.DIMENSIONS, PageKind.DIMENSION_INDEX, PageKind.DIMENSION,
            this.journal.dimensions());
        this.addCollectionPages(result, Section.BIOMES, PageKind.BIOME_INDEX, PageKind.BIOME,
            this.journal.biomes());

        this.sectionStarts.put(Section.SKILLS, result.size());
        for (int skillPage = 0; skillPage < SKILL_PAGES.size(); skillPage++) {
            result.add(new JournalPage(PageKind.SKILLS, "", skillPage, SKILL_PAGES.size()));
        }
        return List.copyOf(result);
    }

    private void addCollectionPages(final List<JournalPage> result,
                                    final Section section,
                                    final PageKind indexKind,
                                    final PageKind detailKind,
                                    final List<String> ids) {
        this.sectionStarts.put(section, result.size());
        result.add(new JournalPage(indexKind, "", 0, ids.size()));
        for (int index = 0; index < ids.size(); index++) {
            result.add(new JournalPage(detailKind, ids.get(index), index + 1, ids.size()));
        }
    }

    private void createSkillLevelButtons(final int left, final int top) {
        this.skillLevelButtons.clear();
        final int cardWidth = skillCardWidth();
        final int cardHeight = skillCardHeight();

        for (int skillPage = 0; skillPage < SKILL_PAGES.size(); skillPage++) {
            final List<WitnessSkill> skills = SKILL_PAGES.get(skillPage);
            for (int skillIndex = 0; skillIndex < skills.size(); skillIndex++) {
                final WitnessSkill skill = skills.get(skillIndex);
                final int cardX = skillCardX(left, skillIndex);
                final int cardY = skillCardY(top, skillIndex);
                final int levelsX = skillLevelsX(cardX, cardWidth, skill);
                final int levelsY = skillLevelsY(cardY, cardHeight);

                for (int level = 1; level <= skill.maxLevel(); level++) {
                    final int targetLevel = level;
                    final int nodeX = levelsX + (level - 1) * (SKILL_LEVEL_NODE_WIDTH + SKILL_LEVEL_NODE_GAP);
                    final SkillLevelButton button = this.addRenderableWidget(new SkillLevelButton(
                        nodeX,
                        levelsY,
                        SKILL_LEVEL_NODE_WIDTH,
                        SKILL_LEVEL_NODE_HEIGHT,
                        Component.literal(WitnessSkill.levelAsRomanNumeral(level)),
                        ignored -> ClientPacketDistributor.sendToServer(new UnlockWitnessSkillPayload(skill.id(), targetLevel)),
                        skillPage,
                        skill,
                        targetLevel
                    ));
                    this.skillLevelButtons.add(button);
                }
            }
        }
    }

    public void updateJournal(final WitnessJournalPayload updatedJournal) {
        this.journal = updatedJournal;
        this.updateButtons();
    }

    private void changePage(final int offset) {
        this.pageIndex = Mth.clamp(this.pageIndex + offset, 0, this.pages.size() - 1);
        this.updateButtons();
    }

    private void jumpTo(final Section section) {
        this.pageIndex = this.sectionStarts.getOrDefault(section, 0);
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.previousButton != null) {
            this.previousButton.active = this.pageIndex > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.pageIndex + 1 < this.pages.size();
        }

        final JournalPage currentPage = this.currentPage();
        final Section currentSection = currentPage.kind.section;
        for (final Map.Entry<Section, Button> entry : this.sectionButtons.entrySet()) {
            entry.getValue().active = entry.getKey() != currentSection;
        }

        for (final SkillLevelButton button : this.skillLevelButtons) {
            final boolean visible = currentPage.kind == PageKind.SKILLS && currentPage.index == button.skillPage();
            final int currentLevel = this.journal.skills().level(button.skill());
            button.setUnlocked(button.level() <= currentLevel);
            button.visible = visible;
            button.active = visible
                && button.level() == currentLevel + 1
                && this.journal.resonance() >= button.skill().resonanceCost(button.level());
        }
    }

    private JournalPage currentPage() {
        return this.pages.get(this.pageIndex);
    }

    private void extractBook(final GuiGraphicsExtractor graphics, final int left, final int top) {
        graphics.fill(left + 5, top + 7, left + BOOK_WIDTH + 5, top + BOOK_HEIGHT + 7, SHADOW);
        graphics.fill(left, top, left + BOOK_WIDTH, top + BOOK_HEIGHT, COVER_EDGE);
        graphics.fill(left + 3, top + 3, left + BOOK_WIDTH - 3, top + BOOK_HEIGHT - 3, COVER);
        graphics.fill(left + 6, top + 6, left + PAGE_WIDTH + 5, top + BOOK_HEIGHT - 6, PAGE);
        graphics.fill(left + PAGE_WIDTH + 9, top + 6, left + BOOK_WIDTH - 6, top + BOOK_HEIGHT - 6, PAGE);
        graphics.fill(left + PAGE_WIDTH + 5, top + 6, left + PAGE_WIDTH + 9, top + BOOK_HEIGHT - 6, SPINE);
        graphics.verticalLine(left + 7, top + 8, top + BOOK_HEIGHT - 9, PAGE_EDGE);
        graphics.verticalLine(left + BOOK_WIDTH - 8, top + 8, top + BOOK_HEIGHT - 9, PAGE_EDGE);

        final String fullTitle = this.title.getString().trim();
        final int separator = fullTitle.indexOf(' ');
        final String leftTitle;
        final String rightTitle;
        if (separator >= 0) {
            leftTitle = fullTitle.substring(0, separator);
            rightTitle = fullTitle.substring(separator + 1).trim();
        } else {
            final int midpoint = fullTitle.length() / 2;
            leftTitle = fullTitle.substring(0, midpoint);
            rightTitle = fullTitle.substring(midpoint);
        }

        final int centerX = left + BOOK_WIDTH / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, top + 7);
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
        graphics.text(this.font, leftTitle, -TITLE_CENTER_GAP - this.font.width(leftTitle), 1, ACCENT, false);
        graphics.text(this.font, rightTitle, TITLE_CENTER_GAP, 1, ACCENT, false);
        graphics.pose().popMatrix();
        graphics.horizontalLine(left + 16, left + PAGE_WIDTH - 5, top + 25, RULE);
        graphics.horizontalLine(left + PAGE_WIDTH + 19, left + BOOK_WIDTH - 16, top + 25, RULE);
    }

    private void extractCurrentPage(final GuiGraphicsExtractor graphics, final int left, final int top, final float partialTick) {
        final JournalPage page = this.currentPage();
        switch (page.kind) {
            case GENERAL -> this.extractGeneralPage(graphics, left, top);
            case VICTIM_INDEX -> this.extractCollectionOverview(
                graphics, left, top,
                Component.translatable("journal.lastwitness.screen.victims"),
                Component.translatable("journal.lastwitness.screen.victim_overview"),
                this.journal.witnessedEntityTypes()
            );
            case DIMENSION_INDEX -> this.extractCollectionOverview(
                graphics, left, top,
                Component.translatable("journal.lastwitness.screen.dimensions_title"),
                Component.translatable("journal.lastwitness.screen.dimension_overview"),
                this.journal.dimensions()
            );
            case BIOME_INDEX -> this.extractCollectionOverview(
                graphics, left, top,
                Component.translatable("journal.lastwitness.screen.biomes_title"),
                Component.translatable("journal.lastwitness.screen.biome_overview"),
                this.journal.biomes()
            );
            case VICTIM -> this.extractVictimPage(graphics, left, top, page, partialTick);
            case DIMENSION -> this.extractIdentifierPage(
                graphics, left, top, page,
                "journal.lastwitness.screen.dimension_progress",
                PreviewKind.DIMENSION
            );
            case BIOME -> this.extractIdentifierPage(
                graphics, left, top, page,
                "journal.lastwitness.screen.biome_progress",
                PreviewKind.BIOME
            );
            case SKILLS -> this.extractSkillsPage(graphics, left, top, page);
        }
    }

    private void extractGeneralPage(final GuiGraphicsExtractor graphics, final int left, final int top) {
        final int leftX = left + 6 + PAGE_MARGIN + 2;
        final int rightX = left + PAGE_WIDTH + 9 + PAGE_MARGIN + 2;
        int y = top + 32;

        this.extractPageHeading(graphics, Component.translatable("journal.lastwitness.screen.general"), leftX, y);
        y += 17;
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.resonance", this.journal.resonance()), leftX, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.echoes", this.journal.echoesConsumed()), leftX, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.environmental", this.journal.environmentalDeaths()), leftX, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.screen.attackers", this.journal.attackerCount()), leftX, y);
        this.extractLine(graphics, Component.translatable("journal.lastwitness.screen.causes", this.journal.deathCauseCount()), leftX, y);

        y = top + 32;
        this.extractPageHeading(graphics, Component.translatable("journal.lastwitness.screen.collections"), rightX, y);
        y += 17;
        y = this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.victim_count", this.journal.witnessedEntityTypes().size()), rightX, y);
        y = this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.dimension_count", this.journal.dimensions().size()), rightX, y);
        y = this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.biome_count", this.journal.biomes().size()), rightX, y);
        y += 4;

        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.qualities"), rightX, y, ACCENT, false);
        y += 13;
        y = this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.faded", this.journal.qualityCounts().faded()), rightX, y);
        y = this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.vivid", this.journal.qualityCounts().vivid()), rightX, y);
        y = this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.turbulent", this.journal.qualityCounts().turbulent()), rightX, y);
        this.extractLine(graphics, Component.translatable(
            "journal.lastwitness.screen.impossible", this.journal.qualityCounts().impossible()), rightX, y);
    }

    private void extractCollectionOverview(final GuiGraphicsExtractor graphics,
                                           final int left,
                                           final int top,
                                           final Component title,
                                           final Component description,
                                           final List<String> ids) {
        final int leftX = left + 6 + PAGE_MARGIN + 2;
        final int rightX = left + PAGE_WIDTH + 9 + PAGE_MARGIN + 2;
        final int contentWidth = PAGE_WIDTH - PAGE_MARGIN * 2 - 4;

        this.extractPageHeading(graphics, title, leftX, top + 32);
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.recorded", ids.size()),
            leftX, top + 51, INK, false);
        graphics.textWithWordWrap(this.font, description, leftX, top + 68, contentWidth, MUTED_INK, false);
        graphics.textWithWordWrap(
            this.font,
            Component.translatable("journal.lastwitness.screen.dedicated_pages"),
            leftX,
            top + 107,
            contentWidth,
            MUTED_INK,
            false
        );

        this.extractPageHeading(graphics, Component.translatable("journal.lastwitness.screen.recorded_ids"), rightX, top + 32);
        this.extractCompactIdList(graphics, ids, rightX, top + 50, contentWidth);
    }

    private void extractCompactIdList(final GuiGraphicsExtractor graphics,
                                      final List<String> ids,
                                      final int x,
                                      final int y,
                                      final int width) {
        if (ids.isEmpty()) {
            graphics.textWithWordWrap(
                this.font,
                Component.translatable("journal.lastwitness.screen.empty"),
                x,
                y,
                width,
                MUTED_INK,
                false
            );
            return;
        }

        final int visibleEntries = Math.min(9, ids.size());
        for (int index = 0; index < visibleEntries; index++) {
            final String line = "- " + ids.get(index);
            graphics.text(this.font, this.font.plainSubstrByWidth(line, width), x, y + index * 11, INK, false);
        }
        if (ids.size() > visibleEntries) {
            graphics.text(
                this.font,
                Component.translatable("journal.lastwitness.screen.more_entries", ids.size() - visibleEntries),
                x,
                y + visibleEntries * 11,
                MUTED_INK,
                false
            );
        }
    }

    private void extractVictimPage(final GuiGraphicsExtractor graphics,
                                   final int left,
                                   final int top,
                                   final JournalPage page,
                                   final float partialTick) {
        final int leftX = left + 6 + PAGE_MARGIN + 2;
        final int rightX = left + PAGE_WIDTH + 9 + PAGE_MARGIN + 2;
        final int contentWidth = PAGE_WIDTH - PAGE_MARGIN * 2 - 4;

        this.extractPageHeading(
            graphics,
            Component.translatable("journal.lastwitness.screen.victim_progress", page.index, page.total),
            leftX,
            top + 32
        );

        final Optional<EntityType<?>> entityType = this.entityType(page.id);
        final Component name = entityType.map(EntityType::getDescription)
            .orElseGet(() -> Component.literal(humanizeIdentifier(page.id)));
        final Optional<LivingEntity> displayEntity = this.entityFor(page.id);
        graphics.fill(leftX, top + 48, left + PAGE_WIDTH - 7, top + 137, PANEL_SHADE);

        if (displayEntity.isPresent()) {
            final LivingEntity entity = displayEntity.get();
            final float largestDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
            final int size = Mth.clamp((int) (58.0F / Math.max(0.25F, largestDimension)), 10, 66);
            final float bodyAngle = Mth.sin((this.animationTicks + partialTick) * 0.06F + page.index * 0.75F) * -0.85F;
            try {
                WitnessJournalPreview.extractEntity(
                    graphics,
                    leftX + 4,
                    top + 50,
                    left + PAGE_WIDTH - 10,
                    top + 135,
                    size,
                    0.0F,
                    bodyAngle,
                    0.0F,
                    entity
                );
            } catch (final RuntimeException ignored) {
                graphics.text(this.font, Component.translatable("journal.lastwitness.screen.unrenderable"),
                    leftX + 6, top + 89, MUTED_INK, false);
            }
        } else {
            graphics.text(this.font, Component.translatable("journal.lastwitness.screen.unrenderable"),
                leftX + 6, top + 89, MUTED_INK, false);
        }

        final String shortenedName = this.font.plainSubstrByWidth(name.getString(), contentWidth);
        graphics.text(this.font, shortenedName, leftX, top + 143, INK, false);

        this.extractDetailPanel(graphics, rightX, top, page.id, page.index, page.total);
    }

    private void extractIdentifierPage(final GuiGraphicsExtractor graphics,
                                       final int left,
                                       final int top,
                                       final JournalPage page,
                                       final String progressTranslationKey,
                                       final PreviewKind previewKind) {
        final int leftX = left + 6 + PAGE_MARGIN + 2;
        final int rightX = left + PAGE_WIDTH + 9 + PAGE_MARGIN + 2;
        final int contentWidth = PAGE_WIDTH - PAGE_MARGIN * 2 - 4;

        this.extractPageHeading(
            graphics,
            Component.translatable(progressTranslationKey, page.index, page.total),
            leftX,
            top + 32
        );

        final int previewLeft = leftX + 3;
        final int previewTop = top + 52;
        final int previewRight = left + PAGE_WIDTH - 10;
        final int previewBottom = top + 134;
        graphics.fill(previewLeft - 1, previewTop - 1, previewRight + 1, previewBottom + 1, RULE);
        WitnessJournalPreview.extractEnvironmentPreview(graphics, previewKind, page.id, this.animationTicks, previewLeft, previewTop, previewRight, previewBottom);

        final String name = humanizeIdentifier(page.id);
        graphics.text(this.font, this.font.plainSubstrByWidth(name, contentWidth), leftX, top + 143, INK, false);

        this.extractDetailPanel(graphics, rightX, top, page.id, page.index, page.total);
    }

    private void extractDetailPanel(final GuiGraphicsExtractor graphics,
                                    final int x,
                                    final int top,
                                    final String id,
                                    final int index,
                                    final int total) {
        final int contentWidth = PAGE_WIDTH - PAGE_MARGIN * 2 - 4;
        this.extractPageHeading(graphics, Component.translatable("journal.lastwitness.screen.collection_entry"), x, top + 32);
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.status"), x, top + 52, ACCENT, false);
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.collected"), x, top + 65, INK, false);
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.exact_id"), x, top + 86, ACCENT, false);
        graphics.textWithWordWrap(this.font, Component.literal(id), x, top + 99, contentWidth, INK, false);
        graphics.text(
            this.font,
            Component.translatable("journal.lastwitness.screen.entry_progress", index, total),
            x,
            top + 143,
            MUTED_INK,
            false
        );
    }

    private void extractSkillsPage(final GuiGraphicsExtractor graphics, final int left, final int top, final JournalPage page) {
        final List<WitnessSkill> skills = SKILL_PAGES.get(page.index);
        final int cardWidth = skillCardWidth();
        final int cardHeight = skillCardHeight();

        for (int index = 0; index < skills.size(); index++) {
            this.extractSkillCard(
                graphics,
                skills.get(index),
                skillCardX(left, index),
                skillCardY(top, index),
                cardWidth,
                cardHeight
            );
        }
    }

    private void extractSkillCard(final GuiGraphicsExtractor graphics,
                                  final WitnessSkill skill,
                                  final int x,
                                  final int y,
                                  final int width,
                                  final int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_SHADE);
        graphics.horizontalLine(x, x + width, y, RULE);
        graphics.horizontalLine(x, x + width, y + height, RULE);
        graphics.verticalLine(x, y, y + height, RULE);
        graphics.verticalLine(x + width, y, y + height, RULE);

        final int textWidth = width - 10;
        final Component title = Component.translatable(skill.titleTranslationKey());
        graphics.text(
            this.font,
            this.font.plainSubstrByWidth(title.getString(), textWidth),
            x + 5,
            y + 4,
            ACCENT,
            false
        );

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 5, y + 16);
        graphics.pose().scale(0.5F, 0.5F);
        graphics.textWithWordWrap(
            this.font,
            Component.translatable(skill.descriptionTranslationKey()),
            0,
            0,
            textWidth * 2,
            MUTED_INK,
            false
        );
        graphics.pose().popMatrix();

        this.extractSkillLevels(
            graphics,
            skill,
            skillLevelsX(x, width, skill),
            skillLevelsY(y, height)
        );
    }

    private void extractSkillLevels(final GuiGraphicsExtractor graphics,
                                    final WitnessSkill skill,
                                    final int x,
                                    final int y) {
        final int currentLevel = this.journal.skills().level(skill);

        for (int level = 1; level <= skill.maxLevel(); level++) {
            final int nodeX = x + (level - 1) * (SKILL_LEVEL_NODE_WIDTH + SKILL_LEVEL_NODE_GAP);
            final boolean unlocked = level <= currentLevel;
            final boolean available = level == currentLevel + 1
                && this.journal.resonance() >= skill.resonanceCost(level);
            final int color = unlocked ? SKILL_UNLOCKED : available ? INK : MUTED_INK;

            if (level < skill.maxLevel()) {
                graphics.horizontalLine(
                    nodeX + SKILL_LEVEL_NODE_WIDTH,
                    nodeX + SKILL_LEVEL_NODE_WIDTH + SKILL_LEVEL_NODE_GAP - 1,
                    y + SKILL_LEVEL_NODE_HEIGHT / 2,
                    level < currentLevel ? SKILL_UNLOCKED : RULE
                );
            }

            final String cost = Component.translatable(
                "journal.lastwitness.screen.skill.cost",
                skill.resonanceCost(level)
            ).getString();
            graphics.text(
                this.font,
                cost,
                nodeX + (SKILL_LEVEL_NODE_WIDTH - this.font.width(cost)) / 2,
                y + SKILL_LEVEL_NODE_HEIGHT + 2,
                color,
                false
            );
        }
    }

    private static int skillCardX(final int left, final int index) {
        if (index < SKILL_ROWS_PER_PAGE) {
            return left + PAGE_MARGIN + 8;
        }
        return left + PAGE_WIDTH + 9 + PAGE_MARGIN + 2;
    }

    private static int skillCardWidth() {
        return PAGE_WIDTH - PAGE_MARGIN * 2 - 4;
    }

    private static int skillCardHeight() {
        return (SKILL_AREA_HEIGHT - SKILL_CARD_GAP) / SKILL_ROWS_PER_PAGE;
    }

    private static int skillCardY(final int top, final int index) {
        final int row = index % SKILL_ROWS_PER_PAGE;
        return top + SKILL_AREA_TOP + row * (skillCardHeight() + SKILL_CARD_GAP);
    }

    private static int skillLevelsX(final int cardX, final int cardWidth, final WitnessSkill skill) {
        final int rowWidth = skill.maxLevel() * SKILL_LEVEL_NODE_WIDTH
            + (skill.maxLevel() - 1) * SKILL_LEVEL_NODE_GAP;
        return cardX + (cardWidth - rowWidth) / 2;
    }

    private static int skillLevelsY(final int cardY, final int cardHeight) {
        return cardY + cardHeight - SKILL_LEVEL_NODE_HEIGHT - 14;
    }

    private void extractFooter(final GuiGraphicsExtractor graphics, final int left, final int top) {
        final FormattedCharSequence text = Component.translatable(
            "journal.lastwitness.screen.page", this.pageIndex + 1, this.pages.size()
        ).getVisualOrderText();
        graphics.text(
            this.font,
            text,
            left + BOOK_WIDTH / 2 - this.font.width(text) / 2,
            top + BOOK_HEIGHT + 9,
            MUTED_INK,
            false
        );
    }

    private void extractPageHeading(final GuiGraphicsExtractor graphics,
                                    final Component text,
                                    final int x,
                                    final int y) {
        graphics.text(this.font, text, x, y, ACCENT, false);
        graphics.horizontalLine(x, x + PAGE_WIDTH - PAGE_MARGIN * 2 - 4, y + 11, RULE);
    }

    private int extractLine(final GuiGraphicsExtractor graphics, final Component text, final int x, final int y) {
        graphics.text(this.font, text, x, y, INK, false);
        return y + 12;
    }

    private Optional<EntityType<?>> entityType(final String entityTypeId) {
        final Identifier id = Identifier.tryParse(entityTypeId);
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    private Optional<LivingEntity> entityFor(final String entityTypeId) {
        if ("minecraft:player".equals(entityTypeId)) {
            return Optional.ofNullable(this.minecraft.player);
        }
        return this.entityCache.computeIfAbsent(entityTypeId, this::createEntity);
    }

    private Optional<LivingEntity> createEntity(final String entityTypeId) {
        if (this.minecraft.level == null) {
            return Optional.empty();
        }
        try {
            final Optional<EntityType<?>> type = this.entityType(entityTypeId);
            if (type.isEmpty()) {
                return Optional.empty();
            }
            final Entity created = type.get().create(this.minecraft.level, EntitySpawnReason.COMMAND);
            if (!(created instanceof LivingEntity living)) {
                return Optional.empty();
            }
            living.setSilent(true);
            living.setInvulnerable(true);
            living.setNoGravity(true);
            living.setCustomNameVisible(false);
            if (living instanceof Mob mob) {
                mob.setNoAi(true);
            }
            return Optional.of(living);
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static String humanizeIdentifier(final String id) {
        final int namespaceSeparator = id.indexOf(':');
        final String path = namespaceSeparator >= 0 ? id.substring(namespaceSeparator + 1) : id;
        final String normalized = path.replace('_', ' ').replace('/', ' ');
        final StringBuilder result = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int index = 0; index < normalized.length(); index++) {
            final char character = normalized.charAt(index);
            if (Character.isWhitespace(character)) {
                result.append(character);
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }



    private enum Section {
        GENERAL("journal.lastwitness.screen.tab.general"),
        VICTIMS("journal.lastwitness.screen.tab.victims"),
        DIMENSIONS("journal.lastwitness.screen.tab.dimensions"),
        BIOMES("journal.lastwitness.screen.tab.biomes"),
        SKILLS("journal.lastwitness.screen.tab.skills");

        private final String translationKey;

        Section(final String translationKey) {
            this.translationKey = translationKey;
        }
    }

    public enum PreviewKind {
        DIMENSION,
        BIOME
    }

    private enum PageKind {
        GENERAL(Section.GENERAL),
        VICTIM_INDEX(Section.VICTIMS),
        VICTIM(Section.VICTIMS),
        DIMENSION_INDEX(Section.DIMENSIONS),
        DIMENSION(Section.DIMENSIONS),
        BIOME_INDEX(Section.BIOMES),
        BIOME(Section.BIOMES),
        SKILLS(Section.SKILLS);

        private final Section section;

        PageKind(final Section section) {
            this.section = section;
        }
    }

    private record JournalPage(PageKind kind, String id, int index, int total) {
    }
}
