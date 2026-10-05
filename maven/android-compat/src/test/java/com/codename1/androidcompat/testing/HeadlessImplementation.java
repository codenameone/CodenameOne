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
package com.codename1.androidcompat.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Display;

import java.io.InputStream;

/// A Codename One implementation with no screen, for tests that build and
/// lay out Android views: a fixed 1080x1920 display at 320 dpi (density 2),
/// resources from the class path, and monospaced font metrics. Drawing and
/// everything else answer neutral values.
public class HeadlessImplementation extends CodenameOneImplementation {

    public static final int WIDTH = 1080;
    public static final int HEIGHT = 1920;
    public static final int DPI = 320;
    static final int CHAR_WIDTH = 16;
    static final int FONT_HEIGHT = 32;
    private static final Object FONT = new Object();
    private static final L10NManager L10N = new L10NManager("en", "US") {
    };

    /// Starts Codename One on this implementation once per test JVM (the
    /// test ImplementationFactory creates it).
    public static synchronized void install() {
        if (!Display.isInitialized()) {
            Display.init(null);
        }
    }

    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        Class c = cls == null ? HeadlessImplementation.class : cls;
        return c.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
    }

    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        return Math.round(dipCount * DPI / 25.4f);
    }

    @Override
    public boolean isTrueTypeSupported() {
        return true;
    }

    @Override
    public boolean isNativeFontSchemeSupported() {
        return true;
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        return FONT;
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        return FONT;
    }

    /// What [#isDarkMode()] answers; tests flip it to change the system theme.
    public static boolean darkMode;

    @Override
    public Boolean isDarkMode() {
        return Boolean.valueOf(darkMode);
    }

    @Override
    public void init(java.lang.Object a0) {
        // Nothing to set up: there is no screen.
    }

    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }

    @Override
    public void editString(com.codename1.ui.Component a0, int a1, int a2, java.lang.String a3, int a4) {
    }

    @Override
    public void flushGraphics(int a0, int a1, int a2, int a3) {
    }

    @Override
    public void flushGraphics() {
    }

    @Override
    public void getRGB(java.lang.Object a0, int[] a1, int a2, int a3, int a4, int a5, int a6) {
        if (a0 instanceof int[][]) {
            int[][] rows = (int[][]) a0;
            for (int y = 0; y < a6; y++) {
                System.arraycopy(rows[a4 + y], a3, a1, a2 + y * a5, a5);
            }
        }
    }

    /// When set, an image made from ARGB pixels keeps them (as an `int[][]`
    /// of rows) and answers them to `getRGB`. Off by default; tests that
    /// inspect pixels flip it.
    public static boolean pixelImages;

    @Override
    public java.lang.Object createImage(int[] a0, int a1, int a2) {
        if (pixelImages && a1 > 0 && a2 > 0) {
            int[][] rows = new int[a2][a1];
            for (int y = 0; y < a2; y++) {
                System.arraycopy(a0, y * a1, rows[y], 0, a1);
            }
            return rows;
        }
        return new Object();
    }

    @Override
    public java.lang.Object createImage(java.lang.String a0) throws java.io.IOException {
        return new Object();
    }

    /// With [#pixelImages] set, an encoded image (a PNG resource) is decoded
    /// to its pixels, so tests can check the size a drawable comes out at.
    @Override
    public java.lang.Object createImage(java.io.InputStream a0) throws java.io.IOException {
        if (pixelImages) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(a0);
            if (img != null) {
                int w = img.getWidth();
                int h = img.getHeight();
                return createImage(img.getRGB(0, 0, w, h, null, 0, w), w, h);
            }
        }
        return new Object();
    }

    @Override
    public java.lang.Object createMutableImage(int a0, int a1, int a2) {
        return new Object();
    }

    @Override
    public java.lang.Object createImage(byte[] a0, int a1, int a2) {
        return new Object();
    }

    @Override
    public int getImageWidth(java.lang.Object image) {
        return image instanceof int[][] ? ((int[][]) image)[0].length : 0;
    }

    @Override
    public int getImageHeight(java.lang.Object image) {
        return image instanceof int[][] ? ((int[][]) image).length : 0;
    }

    @Override
    public java.lang.Object scale(java.lang.Object a0, int a1, int a2) {
        if (a0 instanceof int[][] && a1 > 0 && a2 > 0) {
            // Nearest-neighbour, for the pixel images of pixelImages.
            int[][] src = (int[][]) a0;
            int[][] out = new int[a2][a1];
            for (int y = 0; y < a2; y++) {
                for (int x = 0; x < a1; x++) {
                    out[y][x] = src[y * src.length / a2][x * src[0].length / a1];
                }
            }
            return out;
        }
        return new Object();
    }

    @Override
    public int getSoftkeyCount() {
        return 0;
    }

    @Override
    public int[] getSoftkeyCode(int a0) {
        return null;
    }

    @Override
    public int getClearKeyCode() {
        return 0;
    }

    @Override
    public int getBackspaceKeyCode() {
        return 0;
    }

    @Override
    public int getBackKeyCode() {
        return 0;
    }

    @Override
    public int getGameAction(int a0) {
        return 0;
    }

    @Override
    public int getKeyCode(int a0) {
        return 0;
    }

    @Override
    public boolean isTouchDevice() {
        return true;
    }

    @Override
    public int getColor(java.lang.Object a0) {
        return 0;
    }

    @Override
    public void setColor(java.lang.Object a0, int a1) {
    }

    @Override
    public void setAlpha(java.lang.Object a0, int a1) {
    }

    @Override
    public int getAlpha(java.lang.Object a0) {
        return 0;
    }

    @Override
    public void setNativeFont(java.lang.Object a0, java.lang.Object a1) {
    }

    /// When set, every graphics context shares one tracked clip rectangle
    /// and shape clips are supported: `setClip(Shape)` records the shape in
    /// [#shapeClip] and clips to its bounds. Off by default, when the clip is
    /// always the whole display; tests that check clipping flip it.
    public static boolean trackClip;
    /// The last shape passed to `setClip(Shape)` while [#trackClip] is set,
    /// null once a rectangle replaces it.
    public static com.codename1.ui.geom.Shape shapeClip;
    private static int clipX;
    private static int clipY;
    private static int clipW = WIDTH;
    private static int clipH = HEIGHT;

    @Override
    public int getClipX(java.lang.Object a0) {
        return trackClip ? clipX : 0;
    }

    @Override
    public int getClipY(java.lang.Object a0) {
        return trackClip ? clipY : 0;
    }

    @Override
    public int getClipWidth(java.lang.Object a0) {
        return trackClip ? clipW : WIDTH;
    }

    @Override
    public int getClipHeight(java.lang.Object a0) {
        return trackClip ? clipH : HEIGHT;
    }

    @Override
    public void setClip(java.lang.Object a0, int a1, int a2, int a3, int a4) {
        clipX = a1;
        clipY = a2;
        clipW = a3;
        clipH = a4;
        shapeClip = null;
    }

    @Override
    public void clipRect(java.lang.Object a0, int a1, int a2, int a3, int a4) {
        int x2 = Math.min(clipX + clipW, a1 + a3);
        int y2 = Math.min(clipY + clipH, a2 + a4);
        clipX = Math.max(clipX, a1);
        clipY = Math.max(clipY, a2);
        clipW = Math.max(0, x2 - clipX);
        clipH = Math.max(0, y2 - clipY);
        shapeClip = null;
    }

    @Override
    public boolean isShapeClipSupported(java.lang.Object graphics) {
        return trackClip;
    }

    @Override
    public void setClip(java.lang.Object graphics, com.codename1.ui.geom.Shape shape) {
        if (trackClip) {
            com.codename1.ui.geom.Rectangle b = shape.getBounds();
            setClip(graphics, b.getX(), b.getY(), b.getWidth(), b.getHeight());
            shapeClip = shape;
        }
    }

    @Override
    public void drawLine(java.lang.Object a0, int a1, int a2, int a3, int a4) {
    }

    @Override
    public void fillRect(java.lang.Object a0, int a1, int a2, int a3, int a4) {
    }

    @Override
    public void drawRect(java.lang.Object a0, int a1, int a2, int a3, int a4) {
    }

    @Override
    public void drawRoundRect(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
    }

    @Override
    public void fillRoundRect(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
    }

    @Override
    public void fillArc(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
    }

    @Override
    public void drawArc(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
    }

    @Override
    public void drawString(java.lang.Object a0, java.lang.String a1, int a2, int a3) {
    }

    @Override
    public void drawImage(java.lang.Object a0, java.lang.Object a1, int a2, int a3) {
    }

    @Override
    public void drawRGB(java.lang.Object a0, int[] a1, int a2, int a3, int a4, int a5, int a6, boolean a7) {
    }

    @Override
    public java.lang.Object getNativeGraphics() {
        return new Object();
    }

    @Override
    public java.lang.Object getNativeGraphics(java.lang.Object a0) {
        return new Object();
    }

    /// When set, a run of characters measures 1px narrower per adjacent pair
    /// than its characters one by one, as a kerning font (a browser canvas)
    /// does. Off by default; tests that measure text flip it.
    public static boolean kerning;

    private static int kerned(int width, int len) {
        return kerning && len > 1 ? width - (len - 1) : width;
    }

    @Override
    public int charsWidth(java.lang.Object nativeFont, char[] ch, int offset, int len) {
        return kerned(len * CHAR_WIDTH, len);
    }

    @Override
    public int stringWidth(java.lang.Object nativeFont, java.lang.String text) {
        return text == null ? 0 : kerned(text.length() * CHAR_WIDTH, text.length());
    }

    @Override
    public int charWidth(java.lang.Object a0, char a1) {
        return CHAR_WIDTH;
    }

    @Override
    public int getHeight(java.lang.Object a0) {
        return FONT_HEIGHT;
    }

    @Override
    public java.lang.Object getDefaultFont() {
        return FONT;
    }

    @Override
    public java.lang.Object createFont(int a0, int a1, int a2) {
        return FONT;
    }

    @Override
    public java.lang.Object connect(java.lang.String a0, boolean a1, boolean a2) throws java.io.IOException {
        return new Object();
    }

    @Override
    public void setHeader(java.lang.Object a0, java.lang.String a1, java.lang.String a2) {
    }

    @Override
    public int getContentLength(java.lang.Object a0) {
        return 0;
    }

    @Override
    public java.io.OutputStream openOutputStream(java.lang.Object a0) throws java.io.IOException {
        return null;
    }

    @Override
    public java.io.OutputStream openOutputStream(java.lang.Object a0, int a1) throws java.io.IOException {
        return null;
    }

    @Override
    public java.io.InputStream openInputStream(java.lang.Object a0) throws java.io.IOException {
        return null;
    }

    @Override
    public void setPostRequest(java.lang.Object a0, boolean a1) {
    }

    @Override
    public int getResponseCode(java.lang.Object a0) throws java.io.IOException {
        return 0;
    }

    @Override
    public java.lang.String getResponseMessage(java.lang.Object a0) throws java.io.IOException {
        return null;
    }

    @Override
    public java.lang.String getHeaderField(java.lang.String a0, java.lang.Object a1) throws java.io.IOException {
        return null;
    }

    @Override
    public java.lang.String[] getHeaderFieldNames(java.lang.Object a0) throws java.io.IOException {
        return null;
    }

    @Override
    public java.lang.String[] getHeaderFields(java.lang.String a0, java.lang.Object a1) throws java.io.IOException {
        return null;
    }

    @Override
    public void deleteStorageFile(java.lang.String a0) {
    }

    @Override
    public java.io.OutputStream createStorageOutputStream(java.lang.String a0) throws java.io.IOException {
        return null;
    }

    @Override
    public java.io.InputStream createStorageInputStream(java.lang.String a0) throws java.io.IOException {
        return null;
    }

    @Override
    public boolean storageFileExists(java.lang.String a0) {
        return false;
    }

    @Override
    public java.lang.String[] listStorageEntries() {
        return null;
    }

    @Override
    public java.lang.String[] listFilesystemRoots() {
        return null;
    }

    @Override
    public java.lang.String[] listFiles(java.lang.String a0) throws java.io.IOException {
        return null;
    }

    @Override
    public long getRootSizeBytes(java.lang.String a0) {
        return 0;
    }

    @Override
    public long getRootAvailableSpace(java.lang.String a0) {
        return 0;
    }

    @Override
    public void mkdir(java.lang.String a0) {
    }

    @Override
    public void deleteFile(java.lang.String a0) {
    }

    @Override
    public boolean isHidden(java.lang.String a0) {
        return false;
    }

    @Override
    public void setHidden(java.lang.String a0, boolean a1) {
    }

    @Override
    public long getFileLength(java.lang.String a0) {
        return 0;
    }

    @Override
    public boolean isDirectory(java.lang.String a0) {
        return false;
    }

    @Override
    public boolean exists(java.lang.String a0) {
        return false;
    }

    @Override
    public void rename(java.lang.String a0, java.lang.String a1) {
    }

    @Override
    public char getFileSystemSeparator() {
        return '/';
    }

    @Override
    public java.lang.String getPlatformName() {
        return "headless";
    }

    @Override
    public com.codename1.l10n.L10NManager getLocalizationManager() {
        return L10N;
    }
}
