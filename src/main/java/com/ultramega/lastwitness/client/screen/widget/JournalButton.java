package com.ultramega.lastwitness.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import static com.ultramega.lastwitness.LastWitness.makeId;

public class JournalButton extends Button {
    protected static final WidgetSprites SPRITES = new WidgetSprites(makeId("widget/journal_button"), makeId("widget/journal_button_disabled"),
        makeId("widget/journal_button_highlighted"));

    public JournalButton(final int x,
                         final int y,
                         final int width,
                         final int height,
                         final Component message,
                         final OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()),
            this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
