package com.ultramega.lastwitness.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// TODO: improve the environmental previews
public final class WitnessJournalPreview {
    private WitnessJournalPreview() {
    }

    public static void extractEntity(final GuiGraphicsExtractor graphics,
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

    public static void extractEnvironmentPreview(final GuiGraphicsExtractor graphics,
                                                 final WitnessJournalScreen.PreviewKind previewKind,
                                                 final String id,
                                                 final int animationTicks,
                                                 final int x0,
                                                 final int y0,
                                                 final int x1,
                                                 final int y1) {
        if (previewKind == WitnessJournalScreen.PreviewKind.DIMENSION) {
            extractDimensionPreview(graphics, id, animationTicks, x0, y0, x1, y1);
        } else {
            extractBiomePreview(graphics, id, animationTicks, x0, y0, x1, y1);
        }
    }

    private static void extractDimensionPreview(final GuiGraphicsExtractor graphics,
                                                final String id,
                                                final int animationTicks,
                                                final int x0,
                                                final int y0,
                                                final int x1,
                                                final int y1) {
        final String path = identifierPath(id);
        if (path.contains("nether")) {
            extractNetherPreview(graphics, id, animationTicks, x0, y0, x1, y1);
        } else if (path.contains("end")) {
            extractEndPreview(graphics, id, x0, y0, x1, y1);
        } else if (path.contains("overworld")) {
            extractOverworldPreview(graphics, x0, y0, x1, y1);
        } else {
            extractUnknownDimensionPreview(graphics, id, x0, y0, x1, y1);
        }
    }

    private static void extractBiomePreview(final GuiGraphicsExtractor graphics,
                                            final String id,
                                            final int animationTicks,
                                            final int x0,
                                            final int y0,
                                            final int x1,
                                            final int y1) {
        final String path = identifierPath(id);
        if (containsAny(path, "deep_dark", "dripstone", "lush_caves", "cave")) {
            extractCavePreview(graphics, id, x0, y0, x1, y1, path.contains("lush"));
        } else if (containsAny(path, "ocean", "river")) {
            extractWaterPreview(graphics, id, x0, y0, x1, y1, path.contains("frozen"));
        } else if (containsAny(path, "desert", "badlands")) {
            extractDesertPreview(graphics, x0, y0, x1, y1, path.contains("badlands"));
        } else if (containsAny(path, "snow", "frozen", "ice", "grove", "peaks")) {
            extractSnowPreview(graphics, id, animationTicks, x0, y0, x1, y1);
        } else if (containsAny(path, "swamp", "mangrove")) {
            extractSwampPreview(graphics, x0, y0, x1, y1, path.contains("mangrove"));
        } else if (containsAny(path, "mushroom")) {
            extractMushroomPreview(graphics, x0, y0, x1, y1);
        } else if (containsAny(path, "cherry")) {
            extractForestPreview(graphics, x0, y0, x1, y1, 0xFFF19BC1, 0xFF6E9B58, 3);
        } else if (containsAny(path, "jungle", "bamboo")) {
            extractForestPreview(graphics, x0, y0, x1, y1, 0xFF2F8B3B, 0xFF417C35, 5);
        } else if (containsAny(path, "taiga", "pine", "spruce")) {
            extractForestPreview(graphics, x0, y0, x1, y1, 0xFF376B54, 0xFF577A4B, 4);
        } else if (containsAny(path, "forest", "woods")) {
            extractForestPreview(graphics, x0, y0, x1, y1, 0xFF3E8A45, 0xFF65A34E, 4);
        } else if (containsAny(path, "savanna")) {
            extractSavannaPreview(graphics, x0, y0, x1, y1);
        } else {
            extractGenericBiomePreview(graphics, id, x0, y0, x1, y1);
        }
    }

    private static void extractOverworldPreview(final GuiGraphicsExtractor graphics,
                                                final int x0,
                                                final int y0,
                                                final int x1,
                                                final int y1) {
        final int horizon = y0 + (y1 - y0) * 2 / 3;
        graphics.fill(x0, y0, x1, horizon, 0xFF78B7E8);
        graphics.fill(x0, horizon, x1, y1, 0xFF79553A);
        graphics.fill(x0, horizon, x1, horizon + 5, 0xFF66A64B);
        extractSun(graphics, x1 - 22, y0 + 9, 0xFFFFE28A);
        extractCloud(graphics, x0 + 10, y0 + 12);
        extractCloud(graphics, x0 + 65, y0 + 24);
        extractHill(graphics, x0 + 10, horizon, 28, 17, 0xFF5C9B4B);
        extractHill(graphics, x0 + 55, horizon, 39, 24, 0xFF4D8745);
        extractTree(graphics, x0 + 88, horizon + 2, 16, 0xFF3F7E3B, 0xFF6A4930);
    }

    private static void extractNetherPreview(final GuiGraphicsExtractor graphics,
                                             final String id,
                                             final int animationTicks,
                                             final int x0,
                                             final int y0,
                                             final int x1,
                                             final int y1) {
        graphics.fill(x0, y0, x1, y1, 0xFF3A1015);
        graphics.fill(x0, y0, x1, y0 + 11, 0xFF641E20);
        graphics.fill(x0, y1 - 19, x1, y1, 0xFF4A1717);
        graphics.fill(x0, y1 - 13, x1, y1 - 5, 0xFFFF6A16);
        graphics.fill(x0, y1 - 10, x1, y1 - 7, 0xFFFFB21C);
        extractBasaltPillar(graphics, x0 + 16, y0 + 18, y1 - 18, 7);
        extractBasaltPillar(graphics, x0 + 76, y0 + 9, y1 - 18, 10);
        extractBasaltPillar(graphics, x1 - 24, y0 + 25, y1 - 18, 6);
        extractStars(graphics, id + animationTicks / 12, x0 + 3, y0 + 12, x1 - 3, y1 - 20, 0xFFFF6D32, 9);
    }

    private static void extractEndPreview(final GuiGraphicsExtractor graphics,
                                          final String id,
                                          final int x0,
                                          final int y0,
                                          final int x1,
                                          final int y1) {
        graphics.fill(x0, y0, x1, y1, 0xFF0D0914);
        extractStars(graphics, id, x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0xFF8C719C, 17);
        final int islandTop = y1 - 24;
        graphics.fill(x0 + 19, islandTop, x1 - 13, islandTop + 7, 0xFFE3D9A7);
        graphics.fill(x0 + 28, islandTop + 7, x1 - 22, islandTop + 12, 0xFF9A9271);
        graphics.fill(x0 + 41, islandTop + 12, x1 - 37, islandTop + 16, 0xFF5F5947);
        extractObsidianPillar(graphics, x0 + 31, y0 + 18, islandTop);
        extractObsidianPillar(graphics, x1 - 33, y0 + 29, islandTop);
        graphics.fill(x0 + 67, islandTop - 12, x0 + 70, islandTop, 0xFF7950A0);
        graphics.fill(x0 + 61, islandTop - 10, x0 + 76, islandTop - 7, 0xFFA274C1);
    }

    private static void extractUnknownDimensionPreview(final GuiGraphicsExtractor graphics,
                                                       final String id,
                                                       final int x0,
                                                       final int y0,
                                                       final int x1,
                                                       final int y1) {
        final int sky = stableColor(id, 0x13A7, 45, 145);
        final int ground = stableColor(id, 0x51D3, 35, 105);
        final int accent = stableColor(id, 0x9B11, 100, 205);
        final int horizon = y0 + (y1 - y0) * 3 / 5;
        graphics.fill(x0, y0, x1, horizon, sky);
        graphics.fill(x0, horizon, x1, y1, ground);
        extractStars(graphics, id, x0 + 2, y0 + 2, x1 - 2, horizon - 2, 0xFFE8DFF0, 8);
        extractSun(graphics, x1 - 22, y0 + 10, accent);
        extractFloatingIsland(graphics, x0 + 18, horizon - 1, 35, accent, ground);
        extractFloatingIsland(graphics, x0 + 68, horizon + 8, 42, accent, ground);
    }

    private static void extractWaterPreview(final GuiGraphicsExtractor graphics,
                                            final String id,
                                            final int x0,
                                            final int y0,
                                            final int x1,
                                            final int y1,
                                            final boolean frozen) {
        final int surface = y0 + 28;
        graphics.fill(x0, y0, x1, surface, frozen ? 0xFFA9D5EC : 0xFF75BCE3);
        graphics.fill(x0, surface, x1, y1, frozen ? 0xFF7FCBE0 : 0xFF246EA3);
        graphics.fill(x0, surface, x1, surface + 3, frozen ? 0xFFE9FAFF : 0xFF63CBE2);
        for (int line = 0; line < 4; line++) {
            final int waveY = surface + 10 + line * 10;
            final int shift = Math.floorMod(id.hashCode() + line * 19, 12);
            for (int x = x0 - shift; x < x1; x += 18) {
                graphics.fill(Math.max(x, x0), waveY, Math.min(x + 10, x1), waveY + 1, frozen ? 0xFFC5EEF4 : 0xFF4EA5C8);
            }
        }
        graphics.fill(x0 + 19, y1 - 17, x0 + 22, y1 - 8, 0xFF4C8955);
        graphics.fill(x0 + 16, y1 - 13, x0 + 19, y1 - 10, 0xFF5DA066);
        graphics.fill(x0 + 22, y1 - 15, x0 + 25, y1 - 12, 0xFF5DA066);
    }

    private static void extractDesertPreview(final GuiGraphicsExtractor graphics,
                                             final int x0,
                                             final int y0,
                                             final int x1,
                                             final int y1,
                                             final boolean badlands) {
        final int horizon = y0 + 34;
        graphics.fill(x0, y0, x1, horizon, 0xFFEDB866);
        graphics.fill(x0, horizon, x1, y1, badlands ? 0xFF9E4F2D : 0xFFE7C26A);
        extractSun(graphics, x1 - 23, y0 + 9, 0xFFFFE18A);
        if (badlands) {
            graphics.fill(x0 + 9, horizon - 7, x0 + 45, horizon, 0xFFC56A36);
            graphics.fill(x0 + 15, horizon - 13, x0 + 39, horizon - 7, 0xFFE19A4D);
            graphics.fill(x0 + 22, horizon - 18, x0 + 34, horizon - 13, 0xFFB94F35);
        } else {
            extractCactus(graphics, x0 + 27, horizon + 6, 18);
            extractHill(graphics, x0 + 58, horizon + 2, 42, 12, 0xFFD6A94D);
        }
    }

    private static void extractSnowPreview(final GuiGraphicsExtractor graphics,
                                           final String id,
                                           final int animationTicks,
                                           final int x0,
                                           final int y0,
                                           final int x1,
                                           final int y1) {
        final int horizon = y0 + 44;
        graphics.fill(x0, y0, x1, horizon, 0xFFB8D7E8);
        graphics.fill(x0, horizon, x1, y1, 0xFFF2F7F7);
        extractMountain(graphics, x0 + 8, horizon, 34, 28, 0xFF7896A3, 0xFFF5FAFA);
        extractMountain(graphics, x0 + 52, horizon, 47, 36, 0xFF668692, 0xFFF5FAFA);
        extractStars(graphics, id + animationTicks / 10, x0 + 2, y0 + 2, x1 - 2, horizon - 1, 0xFFF8FFFF, 10);
        extractTree(graphics, x1 - 27, horizon + 4, 18, 0xFF446B5C, 0xFF66513C);
    }

    private static void extractSwampPreview(final GuiGraphicsExtractor graphics,
                                            final int x0,
                                            final int y0,
                                            final int x1,
                                            final int y1,
                                            final boolean mangrove) {
        final int horizon = y0 + 39;
        graphics.fill(x0, y0, x1, horizon, 0xFF82988B);
        graphics.fill(x0, horizon, x1, y1, 0xFF4C6652);
        graphics.fill(x0, horizon + 7, x1, y1, 0xFF3F625D);
        final int leaves = mangrove ? 0xFF356944 : 0xFF537A45;
        extractTree(graphics, x0 + 22, horizon + 8, 24, leaves, 0xFF58412F);
        extractTree(graphics, x0 + 82, horizon + 9, 21, leaves, 0xFF58412F);
        graphics.fill(x0 + 22, horizon + 8, x0 + 24, y1 - 1, 0xFF493629);
        graphics.fill(x0 + 88, horizon + 7, x0 + 90, y1 - 1, 0xFF493629);
    }

    private static void extractMushroomPreview(final GuiGraphicsExtractor graphics,
                                               final int x0,
                                               final int y0,
                                               final int x1,
                                               final int y1) {
        final int horizon = y0 + 42;
        graphics.fill(x0, y0, x1, horizon, 0xFF9B87A7);
        graphics.fill(x0, horizon, x1, y1, 0xFF8A627D);
        graphics.fill(x0, horizon, x1, horizon + 5, 0xFFAF78A0);
        extractMushroom(graphics, x0 + 27, horizon + 4, 23, 0xFFD64A49);
        extractMushroom(graphics, x0 + 80, horizon + 4, 18, 0xFF9D65C4);
    }

    private static void extractForestPreview(final GuiGraphicsExtractor graphics,
                                             final int x0,
                                             final int y0,
                                             final int x1,
                                             final int y1,
                                             final int leaves,
                                             final int grass,
                                             final int treeCount) {
        final int horizon = y0 + 43;
        graphics.fill(x0, y0, x1, horizon, 0xFF89BCD8);
        graphics.fill(x0, horizon, x1, y1, 0xFF664B35);
        graphics.fill(x0, horizon, x1, horizon + 5, grass);
        for (int index = 0; index < treeCount; index++) {
            final int treeX = x0 + 12 + index * Math.max(17, (x1 - x0 - 24) / Math.max(1, treeCount - 1));
            final int height = 16 + (index * 7 % 10);
            extractTree(graphics, treeX, horizon + 3, height, leaves, 0xFF68482F);
        }
    }

    private static void extractSavannaPreview(final GuiGraphicsExtractor graphics,
                                              final int x0,
                                              final int y0,
                                              final int x1,
                                              final int y1) {
        final int horizon = y0 + 41;
        graphics.fill(x0, y0, x1, horizon, 0xFFD8AC66);
        graphics.fill(x0, horizon, x1, y1, 0xFF9B7A3F);
        graphics.fill(x0, horizon, x1, horizon + 5, 0xFFB1A14A);
        extractSun(graphics, x1 - 24, y0 + 8, 0xFFFFD270);
        graphics.fill(x0 + 52, horizon - 19, x0 + 56, horizon + 3, 0xFF60452E);
        graphics.fill(x0 + 37, horizon - 22, x0 + 73, horizon - 15, 0xFF55753B);
        graphics.fill(x0 + 43, horizon - 27, x0 + 67, horizon - 20, 0xFF55753B);
    }

    private static void extractCavePreview(final GuiGraphicsExtractor graphics,
                                           final String id,
                                           final int x0,
                                           final int y0,
                                           final int x1,
                                           final int y1,
                                           final boolean lush) {
        graphics.fill(x0, y0, x1, y1, lush ? 0xFF173D36 : 0xFF171820);
        graphics.fill(x0, y0, x1, y0 + 9, 0xFF34313A);
        graphics.fill(x0, y1 - 12, x1, y1, 0xFF3A363A);
        for (int index = 0; index < 6; index++) {
            final int x = x0 + 8 + index * 18;
            final int length = 6 + Math.floorMod(id.hashCode() + index * 13, 17);
            graphics.fill(x, y0 + 8, x + 3, y0 + 8 + length, 0xFF5D5653);
        }
        if (lush) {
            graphics.fill(x0 + 15, y0 + 9, x0 + 18, y0 + 31, 0xFF5E8B4C);
            graphics.fill(x0 + 12, y0 + 27, x0 + 22, y0 + 31, 0xFF75AB5D);
            graphics.fill(x1 - 29, y1 - 23, x1 - 26, y1 - 10, 0xFF5E8B4C);
            graphics.fill(x1 - 33, y1 - 25, x1 - 22, y1 - 21, 0xFF75AB5D);
        } else {
            extractStars(graphics, id, x0 + 5, y0 + 12, x1 - 5, y1 - 14, 0xFF4B75A7, 6);
        }
    }

    private static void extractGenericBiomePreview(final GuiGraphicsExtractor graphics,
                                                   final String id,
                                                   final int x0,
                                                   final int y0,
                                                   final int x1,
                                                   final int y1) {
        final int sky = stableColor(id, 0xD102, 95, 190);
        final int grass = stableColor(id, 0xA641, 55, 145);
        final int leaves = stableColor(id, 0x771B, 60, 155);
        final int horizon = y0 + 43;
        graphics.fill(x0, y0, x1, horizon, sky);
        graphics.fill(x0, horizon, x1, y1, darken(grass, 48));
        graphics.fill(x0, horizon, x1, horizon + 5, grass);
        extractHill(graphics, x0 + 12, horizon + 1, 35, 14, darken(grass, 18));
        extractHill(graphics, x0 + 62, horizon + 2, 45, 19, darken(grass, 28));
        extractTree(graphics, x0 + 83, horizon + 3, 18, leaves, darken(leaves, 55));
    }

    private static void extractSun(final GuiGraphicsExtractor graphics, final int x, final int y, final int color) {
        graphics.fill(x + 2, y, x + 8, y + 10, color);
        graphics.fill(x, y + 2, x + 10, y + 8, color);
    }

    private static void extractCloud(final GuiGraphicsExtractor graphics, final int x, final int y) {
        graphics.fill(x, y + 3, x + 22, y + 8, 0xFFEAF4F5);
        graphics.fill(x + 5, y, x + 13, y + 8, 0xFFF4FAFA);
        graphics.fill(x + 13, y + 2, x + 19, y + 8, 0xFFF4FAFA);
    }

    private static void extractHill(final GuiGraphicsExtractor graphics,
                                    final int x,
                                    final int baseline,
                                    final int width,
                                    final int height,
                                    final int color) {
        final int steps = Math.max(3, height / 4);
        for (int step = 0; step < steps; step++) {
            final int inset = step * Math.max(1, width / (steps * 3));
            final int y = baseline - step * 4;
            graphics.fill(x + inset, y - 4, x + width - inset, baseline + 1, color);
        }
    }

    private static void extractMountain(final GuiGraphicsExtractor graphics,
                                        final int x,
                                        final int baseline,
                                        final int width,
                                        final int height,
                                        final int stone,
                                        final int snow) {
        final int steps = Math.max(4, height / 4);
        for (int step = 0; step < steps; step++) {
            final int inset = step * Math.max(1, width / (steps * 2));
            final int top = baseline - step * 4;
            graphics.fill(x + inset, top - 4, x + width - inset, baseline + 1, stone);
            if (step >= steps - 3) {
                graphics.fill(x + inset, top - 4, x + width - inset, top, snow);
            }
        }
    }

    private static void extractTree(final GuiGraphicsExtractor graphics,
                                    final int centerX,
                                    final int groundY,
                                    final int height,
                                    final int leaves,
                                    final int trunk) {
        final int trunkTop = groundY - height;
        graphics.fill(centerX - 2, trunkTop + 8, centerX + 2, groundY, trunk);
        graphics.fill(centerX - 8, trunkTop + 7, centerX + 9, trunkTop + 14, leaves);
        graphics.fill(centerX - 6, trunkTop + 2, centerX + 7, trunkTop + 11, leaves);
        graphics.fill(centerX - 3, trunkTop - 2, centerX + 4, trunkTop + 5, leaves);
    }

    private static void extractCactus(final GuiGraphicsExtractor graphics,
                                      final int centerX,
                                      final int groundY,
                                      final int height) {
        final int top = groundY - height;
        graphics.fill(centerX - 2, top, centerX + 3, groundY, 0xFF4E8E42);
        graphics.fill(centerX - 7, top + 8, centerX - 2, top + 12, 0xFF4E8E42);
        graphics.fill(centerX - 7, top + 4, centerX - 4, top + 12, 0xFF4E8E42);
        graphics.fill(centerX + 3, top + 5, centerX + 8, top + 9, 0xFF4E8E42);
        graphics.fill(centerX + 5, top + 5, centerX + 8, top + 14, 0xFF4E8E42);
    }

    private static void extractMushroom(final GuiGraphicsExtractor graphics,
                                        final int centerX,
                                        final int groundY,
                                        final int height,
                                        final int cap) {
        final int stemTop = groundY - height + 8;
        graphics.fill(centerX - 2, stemTop, centerX + 3, groundY, 0xFFE7DBC2);
        graphics.fill(centerX - 9, stemTop - 5, centerX + 10, stemTop + 2, cap);
        graphics.fill(centerX - 5, stemTop - 9, centerX + 6, stemTop - 4, cap);
        graphics.fill(centerX - 4, stemTop - 6, centerX - 1, stemTop - 3, 0xFFF7E9DC);
        graphics.fill(centerX + 4, stemTop - 4, centerX + 7, stemTop - 1, 0xFFF7E9DC);
    }

    private static void extractBasaltPillar(final GuiGraphicsExtractor graphics,
                                            final int x,
                                            final int top,
                                            final int bottom,
                                            final int width) {
        graphics.fill(x, top, x + width, bottom, 0xFF29252A);
        graphics.fill(x + 2, top, x + width - 1, bottom, 0xFF3A343A);
    }

    private static void extractObsidianPillar(final GuiGraphicsExtractor graphics,
                                              final int centerX,
                                              final int top,
                                              final int bottom) {
        graphics.fill(centerX - 3, top, centerX + 4, bottom, 0xFF211A2C);
        graphics.fill(centerX - 1, top, centerX + 2, bottom, 0xFF3B2851);
        graphics.fill(centerX - 5, top - 3, centerX + 6, top + 1, 0xFF675174);
    }

    private static void extractFloatingIsland(final GuiGraphicsExtractor graphics,
                                              final int x,
                                              final int top,
                                              final int width,
                                              final int surface,
                                              final int stone) {
        graphics.fill(x, top, x + width, top + 5, surface);
        graphics.fill(x + 5, top + 5, x + width - 5, top + 10, stone);
        graphics.fill(x + 11, top + 10, x + width - 11, top + 14, darken(stone, 24));
    }

    private static void extractStars(final GuiGraphicsExtractor graphics,
                                     final String seedText,
                                     final int x0,
                                     final int y0,
                                     final int x1,
                                     final int y1,
                                     final int color,
                                     final int count) {
        final int width = Math.max(1, x1 - x0);
        final int height = Math.max(1, y1 - y0);
        int seed = seedText.hashCode();
        for (int index = 0; index < count; index++) {
            seed = seed * 1664525 + 1013904223;
            final int x = x0 + Math.floorMod(seed, width);
            seed = seed * 1664525 + 1013904223;
            final int y = y0 + Math.floorMod(seed, height);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static String identifierPath(final String id) {
        final int namespaceSeparator = id.indexOf(':');
        return (namespaceSeparator >= 0 ? id.substring(namespaceSeparator + 1) : id).toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean containsAny(final String value, final String... needles) {
        for (final String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int stableColor(final String id, final int salt, final int minimum, final int maximum) {
        final int range = Math.max(1, maximum - minimum + 1);
        int hash = id.hashCode() ^ salt;
        final int red = minimum + Math.floorMod(hash, range);
        hash = Integer.rotateLeft(hash * 31 + salt, 9);
        final int green = minimum + Math.floorMod(hash, range);
        hash = Integer.rotateLeft(hash * 31 + salt, 13);
        final int blue = minimum + Math.floorMod(hash, range);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int darken(final int color, final int amount) {
        final int red = Math.max(0, (color >> 16 & 0xFF) - amount);
        final int green = Math.max(0, (color >> 8 & 0xFF) - amount);
        final int blue = Math.max(0, (color & 0xFF) - amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

}
