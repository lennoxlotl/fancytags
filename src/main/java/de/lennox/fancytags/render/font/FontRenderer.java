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
package de.lennox.fancytags.render.font;

import static net.minecraft.client.renderer.GlStateManager.bindTexture;
import static net.minecraft.client.renderer.GlStateManager.color;
import static net.minecraft.client.renderer.GlStateManager.enableBlend;
import static net.minecraft.client.renderer.GlStateManager.enableTexture2D;
import static net.minecraft.client.renderer.GlStateManager.scale;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_POLYGON;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glVertex2d;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;

public class FontRenderer {

  private static final Minecraft MC = Minecraft.getMinecraft();
  private static final float FONT_SCALE_FACTOR = 8;
  private final DynamicTexture bitmapTexture;
  private final FontCharacter[] bitMapCharacters = new FontCharacter[256];
  private final int[] colorCodes = new int[32];
  private final int imgSize = 1024;
  private int fontHeight = -1;

  /**
   * Constructs a {@link FontRenderer}
   *
   * @param font              The font which will be used in this font renderer
   * @param antiAlias         Defines if anti aliasing should be used
   * @param fractionalMetrics Defines if fractional metrics should be used
   */
  public FontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
    generateColorCodes();
    this.bitmapTexture = prepareBitMap(font, antiAlias, fractionalMetrics, this.bitMapCharacters);
  }

  /**
   * Method to draw a shadowed string
   *
   * @param text  The text which will be drawn
   * @param x     The x position of the drawn text
   * @param y     The y position of the drawn text
   * @param color The color of the drawn text
   * @return The width of the drawn text
   */
  public float drawStringWithShadow(String text, double x, double y, int color) {
    float shadowWidth = drawString(text, x + 1, y + 1, color, true);
    return Math.max(shadowWidth, drawString(text, x, y, color, false));
  }

  /**
   * Method to draw a string
   *
   * @param text  The text which will be drawn
   * @param x     The x position of the drawn text
   * @param y     The y position of the drawn text
   * @param color The color of the drawn text
   * @return The width of the drawn text
   */
  public float drawString(String text, float x, float y, int color) {
    return drawString(text, x, y, color, false);
  }

  /**
   * Method to draw a centered string
   *
   * @param text  The text which will be drawn
   * @param x     The x position of the drawn text
   * @param y     The y position of the drawn text
   * @param color The color of the drawn text
   * @return The width of the drawn text
   */
  public float drawCenteredString(String text, float x, float y, int color) {
    return drawString(text, x - stringWidthOf(text) / 2, y, color);
  }

  /**
   * Method to draw a shadowed centered string
   *
   * @param text  The text which will be drawn
   * @param x     The x position of the drawn text
   * @param y     The y position of the drawn text
   * @param color The color of the drawn text
   * @return The width of the drawn text
   */
  public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
    float shadowWidth = drawString(text, x - stringWidthOf(text) / 2 + 1, y + 1, color, true);
    return Math.max(shadowWidth, drawString(text, x - stringWidthOf(text) / 2, y, color));
  }

  /**
   * Method to draw a string
   *
   * @param text   The text which will be drawn
   * @param x      The x position of the drawn text
   * @param y      The y position of the drawn text
   * @param color  The color of the drawn text
   * @param shadow Defines if the text should be shadowed
   * @return The width of the drawn text
   */
  private float drawString(String text, double x, double y, int color, boolean shadow) {
    // Fix the color which was selected to be used
    if ((color & -67108864) == 0) {
      color |= -16777216;
    }
    // Calculate the base alpha of the start color, this will be used when coloring with minecraft color codes
    int alpha = color >> 24 & 0xFF;
    float dividedAlpha = alpha / 255.0F;
    // Create a darker shadow color if needed
    if (shadow) {
      color = new Color(color).darker().darker().darker().getRGB();
    }
    boolean strikethrough = false;
    boolean underline = false;
    boolean obfuscated = false;
    StringBuilder drawnChars = new StringBuilder();
    int size = text.length();
    // Correct the x position based on the scale
    double baseX = x;
    x = (x - 1) * FONT_SCALE_FACTOR;
    y = (y - 1.5D) * FONT_SCALE_FACTOR;
    // Set the scale and color
    glPushMatrix();
    enableBlend();
    scale(1f / FONT_SCALE_FACTOR, 1f / FONT_SCALE_FACTOR, 1f);
    color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
      dividedAlpha);
    // Bind the bitmap texture
    enableTexture2D();
    bindTexture(bitmapTexture.getGlTextureId());
    // Make the font look smooth if scaled
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    for (int i = 0; i < size; i++) {
      char character = text.charAt(i);
      String chrAsString = String.valueOf(character);

      // If the character is not supported draw it with the minecraft font renderer
      if (character > 256) {
        // Revert the scale
        scale(FONT_SCALE_FACTOR, FONT_SCALE_FACTOR, 1);
        MC.fontRendererObj.drawString(chrAsString,
          (int) (baseX + stringWidthOf(drawnChars.toString()) + 1),
          (int) (y / FONT_SCALE_FACTOR + 1.5), color + (alpha << 24));
        // Apply the scale again
        scale(1f / FONT_SCALE_FACTOR, 1f / FONT_SCALE_FACTOR, 1);
        x += MC.fontRendererObj.getCharWidth(character) * 2 + 4 * FONT_SCALE_FACTOR;
        drawnChars.append(chrAsString);
        // Bind back the bitmap texture
        enableTexture2D();
        bindTexture(bitmapTexture.getGlTextureId());
      } else {
        // If the character is a color code, color the current
        if (character == '§') {
          // Get the index of the color code
          int colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
          // Color based on the color index
          switch (colorIndex) {
            case 16: {
              // Set the obfuscated status (makes text unrecognizable)
              obfuscated = true;
              break;
            }
            case 18: {
              // Set the strikethrough status (draws a line through the text)
              strikethrough = true;
              break;
            }
            case 19: {
              // Set the underline status (draws a line under the text)
              underline = true;
              break;
            }
            default: {
              if (colorIndex < 16) {
                // Reset all other color caps
                obfuscated = false;
                strikethrough = false;
                underline = false;
                if (colorIndex < 0) {
                  colorIndex = 15;
                }
                int colorCode = this.colorCodes[colorIndex];
                // Created a shadow color if needed
                if (shadow) {
                  colorCode = new Color(colorCode).darker().darker().darker().getRGB();
                }
                // Set the current color
                color = colorCode;
                color((colorCode >> 16 & 0xFF) / 255.0F, (colorCode >> 8 & 0xFF) / 255.0F,
                  (colorCode & 0xFF) / 255.0F, dividedAlpha);
              } else {
                // Reset the color caps
                underline = false;
                strikethrough = false;
                // Reset the color
                color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F,
                  (color & 0xFF) / 255.0F, dividedAlpha);
              }
              break;
            }
          }
          i++;
        } else if (character < bitMapCharacters.length) {
          // TODO: Obfuscate characters like in minecraft
          if (obfuscated) {
            character = '*';
          }
          // Draw the current character
          drawChar(character, (float) x, (float) y);
          drawnChars.append(chrAsString);
          // Draw a line through the text if wanted
          if (strikethrough) {
            line(x, y + bitMapCharacters[character].height / FONT_SCALE_FACTOR,
              x + bitMapCharacters[character].width - 9,
              y + bitMapCharacters[character].height / FONT_SCALE_FACTOR);
          }
          // Draw a line under the text if wanted
          if (underline) {
            line(x, y + bitMapCharacters[character].height - 2.0D,
              x + bitMapCharacters[character].width - 8.0D,
              y + bitMapCharacters[character].height - 2.0D);
          }
          x += bitMapCharacters[character].width - 8f;
        }
      }
    }
    // Reset the tex parameter
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glPopMatrix();
    return (float) x / FONT_SCALE_FACTOR;
  }

  /**
   * Method to return the width of a given text
   *
   * @param text The text as {@link String}
   * @return The width of the text
   */
  public float stringWidthOf(String text) {
    float width = 0;
    int size = text.length();
    for (int i = 0; i < size; i++) {
      char character = text.charAt(i);
      // If the character is invalid, check it with the default minecraft font renderer
      if (character > 256) {
        width += MC.fontRendererObj.getCharWidth(character) * 2 + 4 * FONT_SCALE_FACTOR;
        continue;
      }
      // If the character is a color code indicator then continue
      if (character == '§') {
        i++;
      } else if (character < bitMapCharacters.length) {
        width += bitMapCharacters[character].width - 8;
      }
    }
    return width / FONT_SCALE_FACTOR;
  }

  /**
   * Method to return the height of the current font
   *
   * @return The height of the current font
   */
  public int height() {
    return (int) ((fontHeight - 6) / FONT_SCALE_FACTOR);
  }

  /**
   * Method to draw a character of the current font used in this font renderer
   *
   * @param chr The character which will be drawn
   * @param x   The x position where the character will be drawn
   * @param y   The y position where the character will be drawn
   */
  private void drawChar(char chr, float x, float y) {
    glBegin(GL_POLYGON);
    drawTexturedQuad(x, y, bitMapCharacters[chr].width, bitMapCharacters[chr].height,
      bitMapCharacters[chr].x, bitMapCharacters[chr].y, bitMapCharacters[chr].width,
      bitMapCharacters[chr].height);
    glEnd();
  }

  /**
   * Method to draw a textured quad of a position in a texture
   *
   * @param x         The x position of the quad
   * @param y         The y position of the quad
   * @param width     The width of the quad
   * @param height    The height of the quad
   * @param srcX      The x position of the texture which will be drawn
   * @param srcY      The y position of the texture which will be drawn
   * @param srcWidth  The width of the texture which will be drawn
   * @param srcHeight The height of the texture which will be drawn
   */
  private void drawTexturedQuad(float x, float y, float width, float height, float srcX, float srcY,
    float srcWidth, float srcHeight) {
    // Calculate the texture bounds
    float s = srcX / imgSize;
    float t = srcY / imgSize;
    float w = srcWidth / imgSize;
    float h = srcHeight / imgSize;
    // Draw the textured quad
    glTexCoord2f(s + w, t);
    glVertex2d(x + width, y);
    glTexCoord2f(s, t);
    glVertex2d(x, y);
    glTexCoord2f(s, t + h);
    glVertex2d(x, y + height);
    glTexCoord2f(s, t + h);
    glVertex2d(x, y + height);
    glTexCoord2f(s + w, t + h);
    glVertex2d(x + width, y + height);
    glTexCoord2f(s + w, t);
    glVertex2d(x + width, y);
  }

  /**
   * Method to prepare the bitmap for this font renderer
   *
   * @param font              The font which is being used in this font renderer
   * @param antiAlias         Defines if the font should be anti aliased
   * @param fractionalMetrics Defines if the Characters should be sub-pixel correct
   * @param chars             All chars which will be in the bitmap
   * @return The finished bitmap image as {@link DynamicTexture}
   */
  private DynamicTexture prepareBitMap(Font font, boolean antiAlias, boolean fractionalMetrics,
    FontCharacter[] chars) {
    BufferedImage img = geneateBitMap(font, antiAlias, fractionalMetrics, chars);
    return new DynamicTexture(img);
  }

  /**
   * Method to generate a bitmap image of a specific .ttf font
   *
   * @param font              The Font which a bitmap will be created of
   * @param antiAlias         Defines if Anti Aliasing should be enabled in Graphics2D
   * @param fractionalMetrics Defines if Fractional Metrics should be enabled in Graphics2D
   * @param chars             All chars requested to be contained in the bitmap
   * @return The finished bitmap image as {@link BufferedImage}
   */
  protected BufferedImage geneateBitMap(Font font, boolean antiAlias, boolean fractionalMetrics,
    FontCharacter[] chars) {
    // Create a new buffered image
    BufferedImage buf = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
    // Prepare a graphics 2d ctx
    Graphics2D graphics2D = (Graphics2D) buf.getGraphics();
    // Set the antialiasing of the ctx
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    // Set the fractional metrics of the ctx
    graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
      fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
        : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    // Set the font of the ctx
    graphics2D.setFont(font);
    // Set the primary color of the ctx
    graphics2D.setColor(new Color(-1));
    float height = 0;
    int x = 0;
    int y = 0;
    // Receive the font metrics of the prepared ctx
    FontMetrics metrics = graphics2D.getFontMetrics();
    for (int i = 0; i < chars.length; i++) {
      char chr = (char) i;
      FontCharacter fontCharacter = new FontCharacter();
      // Get the dimensions of the drawn character
      Rectangle2D dimensions = metrics.getStringBounds(String.valueOf(chr), graphics2D);
      // Set the w and h of the font char
      fontCharacter.width = (float) (metrics.stringWidth(String.valueOf(chr))) + 8f;
      fontCharacter.height = (float) dimensions.getHeight();
      // Check if the character would be drawn out of bounds, if yes continue on a new line
      if (x + fontCharacter.width > imgSize) {
        x = 0;
        y += height + 8f;
        height = 0;
      }
      if (fontCharacter.height > height) {
        height = fontCharacter.height;
      }
      // Update the font height of the current font
      if (fontCharacter.height > fontHeight) {
        fontHeight = (int) fontCharacter.height;
      }
      // Set the position of the font char
      fontCharacter.x = x;
      fontCharacter.y = y;
      // Add the font char to the list
      chars[i] = fontCharacter;
      // Draw the char to the ctx
      graphics2D.drawString(String.valueOf(chr), x + 2f, y + metrics.getAscent());
      // Expand the x position
      x += fontCharacter.width;
    }
    return buf;
  }

  /**
   * Method to draw a line at a specific position to another position
   *
   * @param x  The x position of the line
   * @param y  The y position of the line
   * @param x2 The x position the line will end at
   * @param y2 The y position the line will end at
   */
  private void line(double x, double y, double x2, double y2) {
    // Disable texture
    glDisable(GL_TEXTURE_2D);
    glLineWidth(2.0F);
    // Draw the line
    glBegin(GL_LINES);
    glVertex2d(x, y);
    glVertex2d(x2, y2);
    glEnd();
    // Enable texture
    glEnable(GL_TEXTURE_2D);
  }

  /**
   * Method to generate all minecraft color codes
   *
   * @author Mojang (All Rights Reserved)
   * @see net.minecraft.client.gui.FontRenderer#FontRenderer(GameSettings, ResourceLocation,
   * TextureManager, boolean)
   */
  private void generateColorCodes() {
    for (int i = 0; i < 32; ++i) {
      int j = (i >> 3 & 1) * 85;
      int k = (i >> 2 & 1) * 170 + j;
      int l = (i >> 1 & 1) * 170 + j;
      int i1 = (i >> 0 & 1) * 170 + j;

      if (i == 6) {
        k += 85;
      }

      if (i >= 16) {
        k /= 4;
        l /= 4;
        i1 /= 4;
      }

      this.colorCodes[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
    }
  }
}
