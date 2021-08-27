/*
 * Copyright (c) 2021 Lennox
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.lennox.fancytags.injection.mixin.labymod;

import de.lennox.fancytags.render.LivingLabelRenderer;
import de.lennox.fancytags.render.font.FontRenderer;
import de.lennox.fancytags.render.font.Fonts;
import net.labymod.core_implementation.mc18.RenderPlayerImplementation;
import net.labymod.main.LabyMod;
import net.labymod.mojang.RenderPlayerHook;
import net.labymod.user.User;
import net.labymod.user.group.EnumGroupDisplayType;
import net.labymod.user.group.LabyGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static net.minecraft.client.renderer.GlStateManager.depthMask;
import static net.minecraft.client.renderer.GlStateManager.enableDepth;

@Mixin(RenderPlayerImplementation.class)
public class MixinRenderPlayerImpl {

    private final LivingLabelRenderer<AbstractClientPlayer> livingLabelRenderer = new LivingLabelRenderer<>();
    private static final FontRenderer FR = Fonts.productSansOf(72);

    /**
     * @author Lennox
     * @reason Overwrite labymod player labels to render custom ones
     */
    @Overwrite(remap = false)
    public void renderName(
        RenderPlayerHook.RenderPlayerCustom renderPlayer,
        AbstractClientPlayer entity,
        double x,
        double y,
        double z
    ) {
        // Check if the label is allowed to be rendered
        boolean canRender = Minecraft.isGuiEnabled() && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer) && entity.riddenByEntity == null;
        if (renderPlayer.canRenderTheName(entity) || entity == renderPlayer.getRenderManager().livingPlayer && LabyMod.getSettings().showMyName && canRender) {
            // Get the distance between yourself and the other entity, needed for drawing score entries
            double distance = entity.getDistanceSqToEntity(renderPlayer.getRenderManager().livingPlayer);
            // Get the display name of the entity
            String username = entity.getDisplayName().getFormattedText();
            // Get the labymod user of the entity
            User user = LabyMod.getInstance().getUserManager().getUser(entity.getUniqueID());
            LabyGroup labyGroup = user.getGroup();

            float offset = 0f;
            double size;

            // If the entity is sneaking draw the occluded label
            if (entity.isSneaking()) {
                livingLabelRenderer.prepare(entity, x, y, z);
                int i = (int) (FR.strihgWidthOf(username) / 2);
                // Draw the background
                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                worldrenderer.pos(-i - 1, -1.5D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
                worldrenderer.pos(-i - 1, 8.5D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
                worldrenderer.pos(i + 1, 8.5D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
                worldrenderer.pos(i + 1, -1.5D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
                tessellator.draw();
                GlStateManager.enableTexture2D();
                // Draw the text
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                FR.drawString(username, -FR.strihgWidthOf(username) / 2, -1, 553648127);
                livingLabelRenderer.finish();
            } else {
                // Check if the user has a subtitle, if yes draw it
                if (user.getSubTitle() != null && distance < 64) {
                    GlStateManager.pushMatrix();
                    size = user.getSubTitleSize();
                    GlStateManager.translate(0.0D, -0.2D + size / 8.0D, 0.0D);
                    // Scale down the subtitle
                    livingLabelRenderer.prepare(entity, x, y, z);
                    float f1 = (float) (0.016666668F * user.getSubTitleSize());
                    GlStateManager.scale(-f1, -f1, f1);
                    // Draw the subtitle
                    livingLabelRenderer.renderLabel(entity, user.getSubTitle());
                    livingLabelRenderer.finish();
                    y += size / 6.0D;
                    GlStateManager.popMatrix();
                }

                // Check if the entity is in distance
                if (distance < 100.0D) {
                    Scoreboard scoreboard = entity.getWorldScoreboard();
                    ScoreObjective scoreobjective = scoreboard.getObjectiveInDisplaySlot(2);
                    // Check if the entity has a score objective
                    if (scoreobjective != null) {
                        // Draw the score objective
                        Score score = scoreboard.getValueFromObjective(entity.getName(), scoreobjective);
                        livingLabelRenderer.prepare(entity, x, y, z);
                        livingLabelRenderer.renderLabel(entity, score.getScorePoints() + " " + scoreobjective.getDisplayName());
                        livingLabelRenderer.finish();
                        y += FR.height() / 2f * 2.35f * 0.02666667F;
                    }
                }

                // Check if the group of the player has a subtitle
                if (labyGroup != null && labyGroup.getDisplayType() == EnumGroupDisplayType.BESIDE_NAME) {
                    size = -FR.strihgWidthOf(username) / 2 - 2 - 8;
                    livingLabelRenderer.prepare(entity, x, y, z);
                    labyGroup.renderBadge(size, -0.5D + offset, 8.0D, 8.0D, false);
                    livingLabelRenderer.finish();
                }

                // Draw the default entity label
                livingLabelRenderer.prepare(entity, x, y, z);
                livingLabelRenderer.renderLabel(entity);
                livingLabelRenderer.finish();
            }
        }
    }

}
