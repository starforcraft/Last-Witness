package com.ultramega.lastwitness.client.screen.widget;

import com.ultramega.lastwitness.data.WitnessSkill;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class SkillLevelButton extends JournalButton {
    private static final int UNLOCKED_OVERLAY = 0xB03D9B4A;

    private final int skillPage;
    private final WitnessSkill skill;
    private final int level;
    private boolean unlocked;

    public SkillLevelButton(final int x,
                            final int y,
                            final int width,
                            final int height,
                            final Component message,
                            final OnPress onPress,
                            final int skillPage,
                            final WitnessSkill skill,
                            final int level) {
        super(x, y, width, height, message, onPress);
        this.skillPage = skillPage;
        this.skill = skill;
        this.level = level;
    }

    public int skillPage() {
        return this.skillPage;
    }

    public WitnessSkill skill() {
        return this.skill;
    }

    public int level() {
        return this.level;
    }

    public void setUnlocked(final boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked) {
            this.setFocused(false);
        }
    }

    @Override
    protected void extractContents(final GuiGraphicsExtractor graphics,
                                   final int mouseX,
                                   final int mouseY,
                                   final float partialTick) {
        if (!this.unlocked) {
            super.extractContents(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            SPRITES.get(true, this.isHoveredOrFocused()),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            ARGB.white(this.alpha)
        );
        graphics.fill(
            this.getX() + 1,
            this.getY() + 1,
            this.getX() + this.getWidth() - 1,
            this.getY() + this.getHeight() - 1,
            UNLOCKED_OVERLAY
        );

        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    @Override
    public int getFGColor() {
        return this.unlocked ? 0xFFFFFF : super.getFGColor();
    }
}
