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
package de.lennox.fancytags.render;

import de.lennox.fancytags.render.font.FontRenderer;
import de.lennox.fancytags.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public class LivingLabelRenderer<T extends EntityLivingBase> {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final FontRenderer FR = Fonts.productSansOf(36);

    /**
     * Method to prepare label rendering
     *
     * @param entityIn The entity which the label will be drawn for
     * @param x        The x position of the label
     * @param y        The y position of the label
     * @param z        The z position of the label
     */
    public void prepare(
        T entityIn,
        double x,
        double y,
        double z
    ) {
        // Fix the player view
        float fixedPlayerView = MC.getRenderManager().playerViewX * (float) (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? -1 : 1);
        RenderManager renderManager = MC.getRenderManager();
        pushMatrix();
        // Translate to the position
        translate(x + 0.0F, y + entityIn.height + 0.5F, z);
        // Rotate
        glNormal3f(0.0F, 1.0F, 0.0F);
        rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        rotate(fixedPlayerView, 1.0F, 0.0F, 0.0F);
        // Scale down the tag
        scale(-0.02666667F, -0.02666667F, 0.02666667F);
        disableLighting();
        depthMask(false);
        disableDepth();
        enableBlend();
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
    }

    /**
     * Method to draw a label with the display name of the entity
     *
     * @param entityIn The entity which the label will be drawn of
     */
    public void renderLabel(T entityIn) {
        // Get the display name of the entity
        String str = entityIn.getDisplayName().getFormattedText();
        int i = labelOffsetOf(entityIn);
        float j = FR.strihgWidthOf(str) / 2;
        // Draw the label background
        drawLabelBackground(j, i);
        // Draw the label text
        renderLabelText(i, str);
    }

    /**
     * Method to draw a label with custom text
     *
     * @param entityIn The entity which owns the label
     * @param label    The text of the label
     */
    public void renderLabel(
        T entityIn,
        String label
    ) {
        // Get the display name of the entity
        int i = labelOffsetOf(entityIn);
        float j = FR.strihgWidthOf(label) / 2;
        // Draw the label background
        drawLabelBackground(j, i);
        // Draw the label text
        renderLabelText(i, label);
    }

    /**
     * Method to draw the text of the labels
     *
     * @param y    The y offset of the label
     * @param text The text which will be drawn
     */
    private void renderLabelText(
        float y,
        String text
    ) {
        // Draw the low alpha text for occluded entities
        FR.drawString(text, -FR.strihgWidthOf(text) / 2, y - 1, 553648127);
        enableDepth();
        depthMask(true);
        // Draw the full alpha text for non occluded entities
        FR.drawString(text, -FR.strihgWidthOf(text) / 2, y - 1, -1);
    }

    /**
     * Method to receive the needed label offset due to minecraft cosmetics
     *
     * @param entity The entity which will be checked
     * @return The offset which is required
     */
    private int labelOffsetOf(T entity) {
        return entity.getDisplayName().getFormattedText()
            .equals("deadmau5") ? -10 : 0;
    }

    /**
     * Method to draw the background of a label
     *
     * @param x The x position of the label
     * @param y The y position of the label
     */
    private void drawLabelBackground(
        float x,
        float y
    ) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-x - 1, -1.5 + y, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-x - 1, 8.5 + y, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(x + 1, 8.5 + y, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(x + 1, -1.5 + y, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        enableTexture2D();
    }

    /**
     * Method to finish the label rendering
     */
    public void finish() {
        enableLighting();
        disableBlend();
        color(1.0F, 1.0F, 1.0F, 1.0F);
        popMatrix();
    }

}
