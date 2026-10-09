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
package com.codename1.compat.testing;

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
    public static final int CHAR_WIDTH = 16;
    public static final int FONT_HEIGHT = 32;
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

    /// The listener of the last `openGallery` call, which the headless
    /// implementation never answers itself. A test that reads it must reset
    /// it.
    public static com.codename1.ui.events.ActionListener gallery;

    @Override
    public void openGallery(com.codename1.ui.events.ActionListener response, int type) {
        gallery = response;
    }

    /// The image encoder `ImageIO.getImageIO()` answers; none by default. A
    /// test that sets it must reset it.
    public static com.codename1.ui.util.ImageIO imageIO;

    @Override
    public com.codename1.ui.util.ImageIO getImageIO() {
        return imageIO;
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

    /// Whether the last screen saver call kept the screen on.
    public static boolean screenLocked;

    @Override
    public void lockScreen() {
        screenLocked = true;
    }

    @Override
    public void unlockScreen() {
        screenLocked = false;
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
        if (rasterImages && a0 > 0 && a1 > 0) {
            int[][] rows = new int[a1][a0];
            for (int y = 0; y < a1; y++) {
                java.util.Arrays.fill(rows[y], a2);
            }
            return rows;
        }
        return new Object();
    }

    // ------------------------------------------------------------ raster

    /// When set, a mutable image keeps its pixels (as an `int[][]` of
    /// rows) and what is drawn into it is rasterized: rectangles, lines,
    /// polygons and ellipses in the current color and alpha, inside the
    /// clip, and pixel images copied. A rounded rectangle is drawn square,
    /// an arc as its whole ellipse, and a string as a solid box of the size
    /// it measures -- enough to assert where something was painted and in
    /// which color, which is all it is for. Sets nothing else: a test that
    /// flips it resets it, and wants [#trackClip] for clipping to count.
    public static boolean rasterImages;
    private static final java.util.Map<Object, Integer> COLORS = new java.util.IdentityHashMap<Object, Integer>();
    private static final java.util.Map<Object, Integer> RASTER_ALPHAS =
            new java.util.IdentityHashMap<Object, Integer>();

    /// Forgets the color and alpha kept for every raster graphics.
    public static void resetRaster() {
        COLORS.clear();
        RASTER_ALPHAS.clear();
    }

    private static boolean raster(Object graphics) {
        return rasterImages && graphics instanceof int[][];
    }

    private static int blend(int under, int rgb, int alpha) {
        if (alpha >= 255) {
            return 0xff000000 | rgb;
        }
        if (alpha <= 0) {
            return under;
        }
        int ua = under >>> 24;
        int r = (((rgb >> 16) & 0xff) * alpha + ((under >> 16) & 0xff) * (255 - alpha)) / 255;
        int g = (((rgb >> 8) & 0xff) * alpha + ((under >> 8) & 0xff) * (255 - alpha)) / 255;
        int b = ((rgb & 0xff) * alpha + (under & 0xff) * (255 - alpha)) / 255;
        return (Math.max(ua, alpha) << 24) | (r << 16) | (g << 8) | b;
    }

    private static void span(Object graphics, int x, int y, int w, int h) {
        int[][] rows = (int[][]) graphics;
        Integer c = COLORS.get(graphics);
        Integer a = RASTER_ALPHAS.get(graphics);
        int rgb = c == null ? 0 : c.intValue() & 0xffffff;
        int alpha = a == null ? 255 : a.intValue();
        int x1 = Math.max(0, x);
        int y1 = Math.max(0, y);
        int x2 = Math.min(rows[0].length, x + w);
        int y2 = Math.min(rows.length, y + h);
        if (trackClip) {
            x1 = Math.max(x1, clipX);
            y1 = Math.max(y1, clipY);
            x2 = Math.min(x2, clipX + clipW);
            y2 = Math.min(y2, clipY + clipH);
        }
        for (int row = y1; row < y2; row++) {
            int[] line = rows[row];
            for (int col = x1; col < x2; col++) {
                line[col] = blend(line[col], rgb, alpha);
            }
        }
    }

    private static void line(Object graphics, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            span(graphics, x1, y1, 1, 1);
            return;
        }
        if (dy == 0) {
            span(graphics, Math.min(x1, x2), y1, dx + 1, 1);
            return;
        }
        if (dx == 0) {
            span(graphics, x1, Math.min(y1, y2), 1, dy + 1);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            span(graphics, x1 + (x2 - x1) * i / steps, y1 + (y2 - y1) * i / steps, 1, 1);
        }
    }

    private static void ellipse(Object graphics, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        double rx = w / 2.0;
        double ry = h / 2.0;
        for (int row = 0; row < h; row++) {
            double t = (row + 0.5 - ry) / ry;
            int half = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - t * t)));
            if (half > 0) {
                span(graphics, (int) Math.round(x + rx - half), y + row, half * 2, 1);
            }
        }
    }

    @Override
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        if (!raster(graphics) || nPoints < 3) {
            return;
        }
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int i = 0; i < nPoints; i++) {
            top = Math.min(top, yPoints[i]);
            bottom = Math.max(bottom, yPoints[i]);
        }
        for (int row = top; row <= bottom; row++) {
            int left = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            for (int i = 0; i < nPoints; i++) {
                int j = (i + 1) % nPoints;
                int ya = yPoints[i];
                int yb = yPoints[j];
                if (ya == yb) {
                    if (ya == row) {
                        left = Math.min(left, Math.min(xPoints[i], xPoints[j]));
                        right = Math.max(right, Math.max(xPoints[i], xPoints[j]));
                    }
                    continue;
                }
                if (row < Math.min(ya, yb) || row > Math.max(ya, yb)) {
                    continue;
                }
                int at = xPoints[i] + (xPoints[j] - xPoints[i]) * (row - ya) / (yb - ya);
                left = Math.min(left, at);
                right = Math.max(right, at);
            }
            if (left <= right) {
                span(graphics, left, row, right - left + 1, 1);
            }
        }
    }

    @Override
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        if (!raster(graphics)) {
            return;
        }
        for (int i = 0; i < nPoints; i++) {
            int j = (i + 1) % nPoints;
            line(graphics, xPoints[i], yPoints[i], xPoints[j], yPoints[j]);
        }
    }

    /// Counts encoded images handed to [#createImage(byte[], int, int)], so a
    /// test can tell a header-only bounds probe from a full decode.
    public static int encodedDecodes;

    @Override
    public java.lang.Object createImage(byte[] a0, int a1, int a2) {
        encodedDecodes++;
        if (pixelImages) {
            try {
                return createImage(new java.io.ByteArrayInputStream(a0, a1, a2));
            } catch (java.io.IOException e) {
                return new Object();
            }
        }
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
        Integer c = rasterImages ? COLORS.get(a0) : null;
        return c == null ? 0 : c.intValue();
    }

    @Override
    public void setColor(java.lang.Object a0, int a1) {
        if (raster(a0)) {
            COLORS.put(a0, Integer.valueOf(a1));
        }
    }

    /// When set, each graphics context keeps the alpha it was given, and
    /// every `fillRect`, `drawImage` and `fillLinearGradient` is added to
    /// [#draws] as {operation, native graphics, alpha}. A test that sets it
    /// must reset it and clear [#draws].
    public static boolean recordDraws;
    public static final java.util.List<Object[]> draws = new java.util.ArrayList<Object[]>();
    private static final java.util.Map<Object, Integer> ALPHAS = new java.util.IdentityHashMap<Object, Integer>();

    private static void recordDraw(String op, Object graphics) {
        if (recordDraws) {
            draws.add(new Object[]{op, graphics, Integer.valueOf(alphaOf(graphics))});
        }
    }

    private static int alphaOf(Object graphics) {
        Integer a = ALPHAS.get(graphics);
        return a == null ? 255 : a.intValue();
    }

    @Override
    public void setAlpha(java.lang.Object a0, int a1) {
        if (raster(a0)) {
            RASTER_ALPHAS.put(a0, Integer.valueOf(a1));
        }
        if (recordDraws) {
            ALPHAS.put(a0, Integer.valueOf(a1));
        }
    }

    @Override
    public int getAlpha(java.lang.Object a0) {
        if (raster(a0)) {
            Integer a = RASTER_ALPHAS.get(a0);
            return a == null ? 255 : a.intValue();
        }
        return recordDraws ? alphaOf(a0) : 0;
    }

    /// With [#recordDraws], the arguments of every gradient fill:
    /// {"fillLinearGradient", start, end, horizontal} or
    /// {"fillGradient", the gradient}. A test that records clears it too.
    public static final java.util.List<Object[]> gradients = new java.util.ArrayList<Object[]>();

    @Override
    public void fillGradient(Object graphics, com.codename1.ui.Gradient gradient, int x, int y, int width,
                             int height) {
        if (recordDraws) {
            recordDraw("fillGradient", graphics);
            gradients.add(new Object[]{"fillGradient", gradient});
        } else {
            super.fillGradient(graphics, gradient, x, y, width, height);
        }
    }

    @Override
    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width,
                                   int height, boolean horizontal) {
        if (recordDraws) {
            recordDraw("fillLinearGradient", graphics);
            gradients.add(new Object[]{"fillLinearGradient", Integer.valueOf(startColor),
                Integer.valueOf(endColor), Boolean.valueOf(horizontal)});
        } else {
            super.fillLinearGradient(graphics, startColor, endColor, x, y, width, height, horizontal);
        }
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
        if (raster(a0)) {
            line(a0, a1, a2, a3, a4);
        }
    }

    @Override
    public void fillRect(java.lang.Object a0, int a1, int a2, int a3, int a4) {
        if (raster(a0)) {
            span(a0, a1, a2, a3, a4);
        }
        recordDraw("fillRect", a0);
    }

    /// The rectangles `clearRect` was asked to erase, as `{x, y, w, h}`.
    /// Tests that check erasing clear it first.
    public static final java.util.List<int[]> clearedRects = new java.util.ArrayList<int[]>();

    @Override
    public void clearRect(java.lang.Object graphics, int x, int y, int width, int height) {
        clearedRects.add(new int[]{x, y, width, height});
        if (graphics instanceof int[][]) {
            // A pixel image's graphics (see getNativeGraphics) really erases.
            int[][] rows = (int[][]) graphics;
            for (int row = Math.max(0, y); row < Math.min(rows.length, y + height); row++) {
                for (int col = Math.max(0, x); col < Math.min(rows[row].length, x + width); col++) {
                    rows[row][col] = 0;
                }
            }
        }
    }

    @Override
    public void drawRect(java.lang.Object a0, int a1, int a2, int a3, int a4) {
        if (raster(a0)) {
            line(a0, a1, a2, a1 + a3, a2);
            line(a0, a1, a2 + a4, a1 + a3, a2 + a4);
            line(a0, a1, a2, a1, a2 + a4);
            line(a0, a1 + a3, a2, a1 + a3, a2 + a4);
        }
    }

    @Override
    public void drawRoundRect(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
        drawRect(a0, a1, a2, a3, a4);
    }

    @Override
    public void fillRoundRect(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
        if (raster(a0)) {
            span(a0, a1, a2, a3, a4);
        }
    }

    @Override
    public void fillArc(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
        if (raster(a0)) {
            ellipse(a0, a1, a2, a3, a4);
        }
    }

    @Override
    public void drawArc(java.lang.Object a0, int a1, int a2, int a3, int a4, int a5, int a6) {
    }

    @Override
    public void drawString(java.lang.Object a0, java.lang.String a1, int a2, int a3) {
        if (raster(a0) && a1 != null) {
            span(a0, a2, a3, a1.length() * CHAR_WIDTH, FONT_HEIGHT);
        }
        if (recordText) {
            drawnText.add(new Object[]{a1, Integer.valueOf(a2), Integer.valueOf(a3)});
        }
    }

    /// When set, every `drawString` (a `drawChar` arrives as one) is added
    /// to [#drawnText] as {text, x, y}. A test that sets it must reset both.
    public static boolean recordText;
    public static final java.util.List<Object[]> drawnText = new java.util.ArrayList<Object[]>();

    @Override
    public void drawImage(java.lang.Object a0, java.lang.Object a1, int a2, int a3) {
        if (raster(a0) && a1 instanceof int[][]) {
            int[][] src = (int[][]) a1;
            int[][] rows = (int[][]) a0;
            for (int y = 0; y < src.length; y++) {
                int row = a3 + y;
                if (row < 0 || row >= rows.length || (trackClip && (row < clipY || row >= clipY + clipH))) {
                    continue;
                }
                for (int x = 0; x < src[y].length; x++) {
                    int col = a2 + x;
                    if (col < 0 || col >= rows[row].length || (trackClip && (col < clipX || col >= clipX + clipW))) {
                        continue;
                    }
                    rows[row][col] = blend(rows[row][col], src[y][x] & 0xffffff, src[y][x] >>> 24);
                }
            }
        }
        recordDraw("drawImage", a0);
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
        // With pixelImages, drawing into a pixel image draws into its rows;
        // only clearRect writes pixels so far.
        if ((pixelImages || rasterImages) && a0 instanceof int[][]) {
            return a0;
        }
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

    /// The face of the system font asked for last, -1 before any was:
    /// every font here is the same one, so this is all a test can tell a
    /// fixed width font by.
    public static int lastFontFace = -1;

    @Override
    public java.lang.Object createFont(int a0, int a1, int a2) {
        lastFontFace = a0;
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

    /// In-memory file system: what was written through `openOutputStream`
    /// with a String path, readable back through `openInputStream` under the
    /// exact same path, so tests can see which path the runtime addressed.
    public static final java.util.Map<String, byte[]> FILES = new java.util.HashMap<String, byte[]>();

    @Override
    public java.io.OutputStream openOutputStream(final java.lang.Object a0) throws java.io.IOException {
        if (!(a0 instanceof String)) {
            return null;
        }
        return new java.io.ByteArrayOutputStream() {
            @Override
            public void close() throws java.io.IOException {
                super.close();
                FILES.put((String) a0, toByteArray());
            }
        };
    }

    @Override
    public java.io.OutputStream openOutputStream(java.lang.Object a0, int a1) throws java.io.IOException {
        return null;
    }

    @Override
    public java.io.InputStream openInputStream(java.lang.Object a0) throws java.io.IOException {
        byte[] b = a0 instanceof String ? FILES.get(a0) : null;
        return b == null ? null : new java.io.ByteArrayInputStream(b);
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
        return new java.lang.String[] {"file:///"};
    }

    /// A fixed home under the one root, so a relative path an application
    /// opens resolves instead of failing on a root list that is not there.
    /// Nothing exists in it until a test writes it: see [#FILES].
    @Override
    public java.lang.String getAppHomePath() {
        return "file:///home/";
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
        if (fileSystem && !UNDELETABLE.contains(a0)) {
            FILES.remove(a0);
        }
    }

    /// When set, [#FILES] answers `exists` and `deleteFile` removes from it,
    /// except a path in [#UNDELETABLE], which a refused unlink leaves in
    /// place. A test that sets it must reset it and clear both.
    public static boolean fileSystem;
    public static final java.util.Set<String> UNDELETABLE = new java.util.HashSet<String>();

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
        return fileSystem && FILES.containsKey(a0);
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
