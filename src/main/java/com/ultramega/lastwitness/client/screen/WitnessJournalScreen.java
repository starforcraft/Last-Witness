package com.ultramega.lastwitness.client.screen;

import com.ultramega.lastwitness.client.screen.widget.JournalButton;
import com.ultramega.lastwitness.network.WitnessJournalPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WitnessJournalScreen extends Screen {
    private static final int BOOK_WIDTH = 304;
    private static final int BOOK_HEIGHT = 186;
    private static final int PAGE_MARGIN = 10;
    private static final int PAGE_WIDTH = 145;
    private static final int VICTIMS_PER_PAGE = 3;
    private static final int VICTIM_ROW_HEIGHT = 40;
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
    private static final int ROW_SHADE = 0x26A67B4A;

    private final WitnessJournalPayload journal;
    private final Map<String, Optional<LivingEntity>> entityCache = new HashMap<>();
    private int victimPage;
    private int animationTicks;
    private Button previousButton;
    private Button nextButton;

    public WitnessJournalScreen(final WitnessJournalPayload journal) {
        super(Component.translatable("journal.lastwitness.header"));
        this.journal = journal;
    }

    @Override
    protected void init() {
        super.init();
        final int left = (this.width - BOOK_WIDTH) / 2;
        final int top = (this.height - BOOK_HEIGHT) / 2;
        final int rightPage = left + PAGE_WIDTH + 9;
        final int buttonY = top + BOOK_HEIGHT - 21;

        this.previousButton = this.addRenderableWidget(
            new JournalButton(rightPage + 8, buttonY, 18, 14, Component.literal("<"), button -> this.changePage(-1))
        );
        this.nextButton = this.addRenderableWidget(
            new JournalButton(rightPage + PAGE_WIDTH - 28, buttonY, 18, 14, Component.literal(">"), button -> this.changePage(1))
        );
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
        this.extractSummary(graphics, left, top);
        this.extractVictims(graphics, left, top, partialTick);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void changePage(final int offset) {
        this.victimPage = Mth.clamp(this.victimPage + offset, 0, this.pageCount() - 1);
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.previousButton != null) {
            this.previousButton.active = this.victimPage > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.victimPage + 1 < this.pageCount();
        }
    }

    private int pageCount() {
        return Math.max(1, (this.journal.witnessedEntityTypes().size() + VICTIMS_PER_PAGE - 1) / VICTIMS_PER_PAGE);
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

        graphics.horizontalLine(left + 15, left + PAGE_WIDTH - 5, top + 25, RULE);
        graphics.horizontalLine(left + PAGE_WIDTH + 19, left + BOOK_WIDTH - 15, top + 25, RULE);
    }

    private void extractSummary(final GuiGraphicsExtractor graphics, final int left, final int top) {
        final int x = left + PAGE_MARGIN + 2;
        int y = top + 32;
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.summary"), x, y, ACCENT, false);
        y += 14;
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.resonance", this.journal.resonance()), x, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.echoes", this.journal.echoesConsumed()), x, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.environmental", this.journal.environmentalDeaths()), x, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.screen.attackers", this.journal.attackerCount()), x, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.screen.causes", this.journal.deathCauseCount()), x, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.screen.dimensions", this.journal.dimensionCount()), x, y);
        y = this.extractLine(graphics, Component.translatable("journal.lastwitness.screen.biomes", this.journal.biomeCount()), x, y);

        y += 3;
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.qualities"), x, y, ACCENT, false);
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.quality_line",
            this.journal.fadedCount(),
            this.journal.vividCount(),
            this.journal.turbulentCount(),
            this.journal.impossibleCount()), x, y + 12, MUTED_INK, false);
    }

    private int extractLine(final GuiGraphicsExtractor graphics, final Component text, final int x, final int y) {
        graphics.text(this.font, text, x, y, INK, false);
        return y + 12;
    }

    private void extractVictims(final GuiGraphicsExtractor graphics, final int left, final int top, final float partialTick) {
        final int pageLeft = left + PAGE_WIDTH + 9;
        final int contentX = pageLeft + PAGE_MARGIN;
        final int contentWidth = PAGE_WIDTH - PAGE_MARGIN * 2;
        graphics.text(this.font, Component.translatable("journal.lastwitness.screen.victims"), contentX + 2, top + 32, ACCENT, false);

        if (this.journal.witnessedEntityTypes().isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.translatable("journal.lastwitness.screen.empty"),
                contentX + 2, top + 55, contentWidth - 4, MUTED_INK, false);
        } else {
            final int firstIndex = this.victimPage * VICTIMS_PER_PAGE;
            for (int row = 0; row < VICTIMS_PER_PAGE; row++) {
                final int victimIndex = firstIndex + row;
                if (victimIndex >= this.journal.witnessedEntityTypes().size()) {
                    break;
                }
                this.extractVictimRow(graphics, this.journal.witnessedEntityTypes().get(victimIndex),
                    contentX, top + 45 + row * VICTIM_ROW_HEIGHT, contentWidth, victimIndex, partialTick);
            }
        }

        final FormattedCharSequence text = Component.translatable("journal.lastwitness.screen.page", this.victimPage + 1, this.pageCount()).getVisualOrderText();
        graphics.text(this.font, text, pageLeft + PAGE_WIDTH / 2 - this.font.width(text) / 2, top + BOOK_HEIGHT - 17, MUTED_INK, false);
    }

    private void extractVictimRow(final GuiGraphicsExtractor graphics,
                                  final String entityTypeId,
                                  final int x,
                                  final int y,
                                  final int width,
                                  final int victimIndex,
                                  final float partialTick) {
        if ((victimIndex & 1) == 0) {
            graphics.fill(x, y, x + width, y + VICTIM_ROW_HEIGHT - 2, ROW_SHADE);
        }
        graphics.horizontalLine(x, x + width, y + VICTIM_ROW_HEIGHT - 2, RULE);

        final Optional<EntityType<?>> entityType = this.entityType(entityTypeId);
        final Component name = entityType.map(EntityType::getDescription).orElseGet(() -> Component.literal(entityTypeId));
        final String shortenedName = this.font.plainSubstrByWidth(name.getString(), width - 43);
        graphics.text(this.font, shortenedName, x + 42, y + 7, INK, false);

        final Optional<LivingEntity> displayEntity = this.entityFor(entityTypeId);
        if (displayEntity.isPresent()) {
            final LivingEntity entity = displayEntity.get();
            final float largestDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
            final int size = Mth.clamp((int) (26.0F / Math.max(0.25F, largestDimension)), 2, 28);
            final float bodyAngle = Mth.sin((this.animationTicks + partialTick) * 0.06F + victimIndex * 0.75F) * -0.85F;
            try {
                renderEntityWithForwardHead(
                    graphics,
                    x + 2,
                    y + 2,
                    x + 39,
                    y + VICTIM_ROW_HEIGHT - 3,
                    size,
                    0.0F,
                    bodyAngle,
                    0.0F,
                    entity
                );
            } catch (final RuntimeException ignored) {
                graphics.text(this.font, Component.translatable("journal.lastwitness.screen.unrenderable"),
                    x + 4, y + 15, MUTED_INK, false);
            }
        } else {
            graphics.text(this.font, Component.translatable("journal.lastwitness.screen.unrenderable"),
                x + 4, y + 15, MUTED_INK, false);
        }
    }

    private Optional<EntityType<?>> entityType(final String entityTypeId) {
        final Identifier id = Identifier.tryParse(entityTypeId);
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    private Optional<LivingEntity> entityFor(final String entityTypeId) {
        if ("minecraft:player".equals(entityTypeId)) {
            return Optional.ofNullable(this.minecraft.player); //TODO: show a player with the default steve skin instead
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

    private static void renderEntityWithForwardHead(final GuiGraphicsExtractor graphics,
                                                    final int x0,
                                                    final int y0,
                                                    final int x1,
                                                    final int y1,
                                                    final int size,
                                                    final float offsetY,
                                                    final float xAngle,
                                                    final float yAngle,
                                                    final LivingEntity entity) {
        final Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        final Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * ((float) Math.PI / 180.0F));
        rotation.mul(xRotation);

        final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        final EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        final EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);

        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;

        if (renderState instanceof LivingEntityRenderState livingRenderState) {
            livingRenderState.bodyRot = 180.0F + xAngle * 20.0F;
            livingRenderState.yRot = 0.0F;

            if (livingRenderState.pose != Pose.FALL_FLYING) {
                livingRenderState.xRot = -yAngle * 20.0F;
            } else {
                livingRenderState.xRot = 0.0F;
            }

            livingRenderState.boundingBoxWidth /= livingRenderState.scale;
            livingRenderState.boundingBoxHeight /= livingRenderState.scale;
            livingRenderState.scale = 1.0F;
        }

        final Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + offsetY, 0.0F);
        graphics.entity(renderState, (float) size, translation, rotation, xRotation, x0, y0, x1, y1);
    }
}
