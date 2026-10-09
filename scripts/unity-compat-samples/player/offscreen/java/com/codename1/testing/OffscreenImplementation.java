/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.testing;

import com.codename1.gpu.RenderView;
import com.codename1.impl.gpu.GpuImplementation;
import com.codename1.ui.PeerComponent;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import javax.imageio.ImageIO;

/// The core unit tests' implementation, taught the few things a game needs
/// drawn for real: it decodes images, measures and paints text with the
/// fonts the JavaSE port uses, answers the port's key codes, and says it
/// has a GPU.
///
/// The rest is the test implementation's: no window, no AWT event queue,
/// images that are arrays of pixels. It is in that implementation's package
/// to reach the pixels.
public final class OffscreenImplementation extends TestCodenameOneImplementation {
    // The JavaSE port's key codes, from JavaSEPort.
    public static final int GAME_KEY_CODE_FIRE = -90;
    public static final int GAME_KEY_CODE_UP = -91;
    public static final int GAME_KEY_CODE_DOWN = -92;
    public static final int GAME_KEY_CODE_LEFT = -93;
    public static final int GAME_KEY_CODE_RIGHT = -94;
    private static final int VK_ESCAPE = 27;

    private final IdentityHashMap<Object, java.awt.Font> graphicsFonts = new IdentityHashMap<Object, java.awt.Font>();
    private final HashMap<String, java.awt.Font> faces = new HashMap<String, java.awt.Font>();
    private final Graphics2D metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    /// Strings painted, for a caller to count.
    public int stringsDrawn;
    /// GPU peers made, which is how often a render view was initialised.
    public int peersCreated;

    private final GpuImplementation gpu = new GpuImplementation() {
        @Override
        public PeerComponent createPeer(RenderView view) {
            peersCreated++;
            return new PeerComponent(null) {
            };
        }

        @Override
        public void setContinuous(PeerComponent peer, boolean continuous) {
        }

        @Override
        public void requestRender(PeerComponent peer) {
        }
    };

    @Override
    public GpuImplementation getGpuImplementation() {
        return gpu;
    }

    // ------------------------------------------------------------------ keys

    /// A key, entering where the JavaSE port's canvas hands one over.
    public void portKeyPressed(int keyCode) {
        keyPressed(keyCode);
    }

    public void portKeyReleased(int keyCode) {
        keyReleased(keyCode);
    }

    public void portPointerPressed(int x, int y) {
        pointerPressed(x, y);
    }

    public void portPointerReleased(int x, int y) {
        pointerReleased(x, y);
    }

    /// The window changed size.
    public void portSizeChanged(int width, int height) {
        setDisplaySize(width, height);
        sizeChanged(width, height);
    }

    @Override
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case GAME_KEY_CODE_UP:
                return com.codename1.ui.Display.GAME_UP;
            case GAME_KEY_CODE_DOWN:
                return com.codename1.ui.Display.GAME_DOWN;
            case GAME_KEY_CODE_RIGHT:
                return com.codename1.ui.Display.GAME_RIGHT;
            case GAME_KEY_CODE_LEFT:
                return com.codename1.ui.Display.GAME_LEFT;
            case GAME_KEY_CODE_FIRE:
                return com.codename1.ui.Display.GAME_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case com.codename1.ui.Display.GAME_UP:
                return GAME_KEY_CODE_UP;
            case com.codename1.ui.Display.GAME_DOWN:
                return GAME_KEY_CODE_DOWN;
            case com.codename1.ui.Display.GAME_RIGHT:
                return GAME_KEY_CODE_RIGHT;
            case com.codename1.ui.Display.GAME_LEFT:
                return GAME_KEY_CODE_LEFT;
            case com.codename1.ui.Display.GAME_FIRE:
                return GAME_KEY_CODE_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int getBackKeyCode() {
        return VK_ESCAPE;
    }

    @Override
    public boolean isDesktop() {
        return true;
    }

    // ---------------------------------------------------------------- images

    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        InputStream in = super.getResourceAsStream(cls, resource);
        return in != null ? in : OffscreenImplementation.class.getResourceAsStream(resource);
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        BufferedImage read = ImageIO.read(i);
        if (read == null) {
            throw new IOException("not an image");
        }
        int w = read.getWidth();
        int h = read.getHeight();
        return createImage(read.getRGB(0, 0, w, h, null, 0, w), w, h);
    }

    // ------------------------------------------------------------------ text

    /// As JavaSEPort.loadTrueTypeFont: the `native:` faces are the Roboto
    /// files inside the port's jar.
    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        if (fontName == null || !fontName.startsWith("native:")) {
            return super.loadTrueTypeFont(fontName, fileName);
        }
        java.awt.Font face = faces.get(fontName);
        if (face == null) {
            // MainRegular is Roboto-Medium and ItalicRegular Roboto-Italic;
            // every other face is its weight, with Italic after it.
            String name = fontName.substring("native:".length());
            boolean italic = name.startsWith("Italic");
            String weight = name.substring(italic ? "Italic".length() : "Main".length());
            String res;
            if (weight.equals("Regular")) {
                res = italic ? "Italic" : "Medium";
            } else {
                res = italic ? weight + "Italic" : weight;
            }
            String path = "/com/codename1/impl/javase/Roboto-" + res + ".ttf";
            try (InputStream in = OffscreenImplementation.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException(path + " is not on the class path; the JavaSE port's jar has it");
                }
                face = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (java.awt.FontFormatException e) {
                throw new IllegalStateException(e);
            }
            faces.put(fontName, face);
        }
        return face;
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        if (!(font instanceof java.awt.Font)) {
            return super.deriveTrueTypeFont(font, size, weight);
        }
        int style = java.awt.Font.PLAIN;
        if ((weight & com.codename1.ui.Font.STYLE_BOLD) != 0) {
            style = java.awt.Font.BOLD;
        }
        if ((weight & com.codename1.ui.Font.STYLE_ITALIC) != 0) {
            style |= java.awt.Font.ITALIC;
        }
        return ((java.awt.Font) font).deriveFont(style, size);
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        if (nativeFont instanceof java.awt.Font) {
            return metrics.getFontMetrics((java.awt.Font) nativeFont).stringWidth(str);
        }
        return super.stringWidth(nativeFont, str);
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        if (nativeFont instanceof java.awt.Font) {
            return metrics.getFontMetrics((java.awt.Font) nativeFont).charsWidth(ch, offset, length);
        }
        return super.charsWidth(nativeFont, ch, offset, length);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        if (nativeFont instanceof java.awt.Font) {
            return metrics.getFontMetrics((java.awt.Font) nativeFont).charWidth(ch);
        }
        return super.charWidth(nativeFont, ch);
    }

    @Override
    public int getHeight(Object nativeFont) {
        if (nativeFont instanceof java.awt.Font) {
            java.awt.FontMetrics m = metrics.getFontMetrics((java.awt.Font) nativeFont);
            return m.getDescent() < 0 ? m.getAscent() - m.getDescent() + m.getLeading() : m.getHeight();
        }
        return super.getHeight(nativeFont);
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        if (font instanceof java.awt.Font) {
            graphicsFonts.put(graphics, (java.awt.Font) font);
            return;
        }
        graphicsFonts.remove(graphics);
        super.setNativeFont(graphics, font);
    }

    /// As JavaSEPort.drawString, into the pixels of a mutable image: the
    /// only place a game view paints text.
    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        TestGraphics g = (TestGraphics) graphics;
        java.awt.Font font = graphicsFonts.get(graphics);
        if (g.image == null || font == null) {
            return;
        }
        stringsDrawn++;
        TestImage image = g.image;
        BufferedImage buffer = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB);
        buffer.setRGB(0, 0, image.width, image.height, image.argb, 0, image.width);
        Graphics2D g2 = buffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font);
        g2.setColor(new java.awt.Color((g.color >> 16) & 0xff, (g.color >> 8) & 0xff, g.color & 0xff, g.alpha & 0xff));
        g2.drawString(str, x + g.translateX, y + g.translateY + g2.getFontMetrics().getAscent());
        g2.dispose();
        buffer.getRGB(0, 0, image.width, image.height, image.argb, 0, image.width);
    }
}
