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

import java.awt.Font;
import java.util.HashMap;

public class Fonts {

  private static final HashMap<Integer, FontRenderer> INTER = new HashMap<>();

  /**
   * Method to return a font with a wanted size
   *
   * @param size The wanted size of the font
   * @return The font with the wanted size
   */
  public static FontRenderer interOf(int size) {
    checkIfAbsent(INTER, size, Font.PLAIN, "Inter-Medium", true, true);
    return INTER.get(size);
  }

  /**
   * Method to check if there already is a font created with the requested size if it does not exist
   * create it
   *
   * @param map               The map with all the fonts
   * @param size              The requested size
   * @param type              The requested font type
   * @param fontName          The requested font name
   * @param antiAlias         The requested anti alias state
   * @param fractionalMetrics The requested fractional metrics state
   */
  private static void checkIfAbsent(HashMap<Integer, FontRenderer> map, int size, int type,
    String fontName, boolean antiAlias, boolean fractionalMetrics) {
    if (!map.containsKey(size)) {
      Font font = fontOf(fontName + ".ttf", size, type);
      map.put(size,
        new FontRenderer(font.deriveFont((float) size), antiAlias, fractionalMetrics));
    }
  }

  /**
   * Method to get a {@link Font} from a .ttf {@link java.io.InputStream}
   *
   * @param fontLocation The location of the font
   * @param fontSize     The requested size of the font
   * @param fontType     The requested type of the font
   * @return The created {@link Font}
   */
  private static Font fontOf(String fontLocation, float fontSize, int fontType) {
    try {
      Font output = Font.createFont(fontType, Fonts.class.getResourceAsStream("/" + fontLocation));
      output = output.deriveFont(fontSize);
      return output;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


}
