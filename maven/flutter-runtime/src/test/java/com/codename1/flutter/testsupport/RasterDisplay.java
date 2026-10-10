/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.testsupport;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/// A software-rasterising Codename One implementation for unit tests that need real
/// components, real pixels and real pointer dispatch, without a window.
///
/// Images and graphics contexts are both {@link Surface}s: an ARGB int array with a
/// colour, an alpha, a rectangular clip, an optional shape clip and a clip stack.
/// Drawing is source-over with the graphics alpha applied, which is what the effects
/// under test depend on. Text is not rendered; everything else that is not needed is
/// a stub.
///
/// {@link #install()} swaps it in as the Display's implementation and
/// {@link #uninstall()} puts the headless state back, so the rest of the suite still
/// runs without a Display.
public final class RasterDisplay extends CodenameOneImplementation {

    public static final int WIDTH = 400;
    public static final int HEIGHT = 800;

    /// A pixel buffer that is also its own graphics context.
    public static final class Surface {
        public final int w;
        public final int h;
        public final int[] px;
        int color;
        int alpha = 255;
        int cx;
        int cy;
        int cw;
        int ch;
        Shape shapeClip;
        final List<Object[]> clipStack = new ArrayList<Object[]>();

        Surface(int w, int h, int fill) {
            this.w = w;
            this.h = h;
            this.px = new int[Math.max(0, w * h)];
            for (int i = 0; i < px.length; i++) {
                px[i] = fill;
            }
            cw = w;
            ch = h;
        }

        public int at(int x, int y) {
            return px[y * w + x];
        }

        boolean inClip(int x, int y) {
            if (x < cx || y < cy || x >= cx + cw || y >= cy + ch || x < 0 || y < 0 || x >= w || y >= h) {
                return false;
            }
            return shapeClip == null || shapeClip.contains(x, y);
        }

        void blend(int x, int y, int argb, int extraAlpha) {
            if (!inClip(x, y)) {
                return;
            }
            int sa = ((argb >>> 24) * extraAlpha + 127) / 255;
            if (sa == 0) {
                return;
            }
            int i = y * w + x;
            int d = px[i];
            int da = d >>> 24;
            int oa = sa + (da * (255 - sa) + 127) / 255;
            if (oa == 0) {
                px[i] = 0;
                return;
            }
            int r = mix((argb >> 16) & 0xff, sa, (d >> 16) & 0xff, da, oa);
            int g = mix((argb >> 8) & 0xff, sa, (d >> 8) & 0xff, da, oa);
            int b = mix(argb & 0xff, sa, d & 0xff, da, oa);
            px[i] = (oa << 24) | (r << 16) | (g << 8) | b;
        }

        private static int mix(int s, int sa, int d, int da, int oa) {
            int v = (s * sa * 255 + d * da * (255 - sa) + (oa * 255) / 2) / (oa * 255);
            return Math.max(0, Math.min(255, v));
        }
    }

    private final Surface screen = new Surface(WIDTH, HEIGHT, 0xffffffff);
    private final Object font = new Object();

    private static CodenameOneImplementation previousImpl;
    private static Object previousEdt;

    /// Installs a fresh raster implementation as the Display's, on the calling thread
    /// as its EDT.
    public static RasterDisplay install() {
        try {
            RasterDisplay impl = new RasterDisplay();
            impl.initImpl(null);
            Field implField = Display.class.getDeclaredField("impl");
            implField.setAccessible(true);
            previousImpl = (CodenameOneImplementation) implField.get(null);
            implField.set(null, impl);
            Display d = Display.getInstance();
            Field running = Display.class.getDeclaredField("codenameOneRunning");
            running.setAccessible(true);
            running.setBoolean(d, true);
            Field edt = Display.class.getDeclaredField("edt");
            edt.setAccessible(true);
            previousEdt = edt.get(d);
            edt.set(d, Thread.currentThread());
            ensureUiManager();
            return impl;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /// The UIManager singleton is created on first use, and a headless test that got there
    /// first -- with no implementation to ask for fonts -- leaves its holder class failed
    /// for the rest of the JVM. Build one directly in that case, so these tests do not
    /// depend on the order the suite runs in.
    private static void ensureUiManager() throws Exception {
        try {
            com.codename1.ui.plaf.UIManager.getInstance();
        } catch (Throwable failedHolder) {
            java.lang.reflect.Constructor<com.codename1.ui.plaf.UIManager> c =
                    com.codename1.ui.plaf.UIManager.class.getDeclaredConstructor();
            c.setAccessible(true);
            Field instance = com.codename1.ui.plaf.UIManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, c.newInstance());
        }
    }

    /// Restores the headless state {@link #install()} replaced.
    public static void uninstall() {
        try {
            Display d = Display.getInstance();
            Field running = Display.class.getDeclaredField("codenameOneRunning");
            running.setAccessible(true);
            running.setBoolean(d, false);
            Field edt = Display.class.getDeclaredField("edt");
            edt.setAccessible(true);
            edt.set(d, previousEdt);
            Field implField = Display.class.getDeclaredField("impl");
            implField.setAccessible(true);
            implField.set(null, previousImpl);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Surface s(Object o) {
        return (Surface) o;
    }

    // ------------------------------------------------------------------
    // Graphics
    // ------------------------------------------------------------------

    @Override
    public void init(Object m) {
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
    public float getDevicePixelRatio() {
        return 1;
    }

    @Override
    public Object getNativeGraphics() {
        return screen;
    }

    @Override
    public Object getNativeGraphics(Object image) {
        // A fresh context per Image.getGraphics(), as the desktop port gives: the
        // clip and alpha a previous painter left behind do not carry over.
        Surface sf = s(image);
        sf.cx = 0;
        sf.cy = 0;
        sf.cw = sf.w;
        sf.ch = sf.h;
        sf.shapeClip = null;
        sf.alpha = 255;
        sf.clipStack.clear();
        return sf;
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        return new Surface(width, height, fillColor);
    }

    @Override
    public boolean isAlphaMutableImageSupported() {
        return true;
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        Surface out = new Surface(width, height, 0);
        System.arraycopy(rgb, 0, out.px, 0, Math.min(rgb.length, out.px.length));
        return out;
    }

    @Override
    public Object createImage(String path) throws IOException {
        throw new IOException("no resources in the raster test display");
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        throw new IOException("no resources in the raster test display");
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        return new Surface(1, 1, 0);
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        Surface src = s(nativeImage);
        for (int yy = 0; yy < height; yy++) {
            for (int xx = 0; xx < width; xx++) {
                arr[offset + yy * width + xx] = src.at(x + xx, y + yy);
            }
        }
    }

    @Override
    public int getImageWidth(Object i) {
        return s(i).w;
    }

    @Override
    public int getImageHeight(Object i) {
        return s(i).h;
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        Surface src = s(nativeImage);
        Surface out = new Surface(width, height, 0);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.px[y * width + x] = src.at(x * src.w / width, y * src.h / height);
            }
        }
        return out;
    }

    @Override
    public int getColor(Object graphics) {
        return s(graphics).color;
    }

    @Override
    public void setColor(Object graphics, int rgb) {
        s(graphics).color = rgb & 0xffffff;
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        s(graphics).alpha = alpha;
    }

    @Override
    public int getAlpha(Object graphics) {
        return s(graphics).alpha;
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
    }

    @Override
    public int getClipX(Object graphics) {
        return s(graphics).cx;
    }

    @Override
    public int getClipY(Object graphics) {
        return s(graphics).cy;
    }

    @Override
    public int getClipWidth(Object graphics) {
        return s(graphics).cw;
    }

    @Override
    public int getClipHeight(Object graphics) {
        return s(graphics).ch;
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        Surface g = s(graphics);
        g.cx = x;
        g.cy = y;
        g.cw = Math.max(0, width);
        g.ch = Math.max(0, height);
        g.shapeClip = null;
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        Surface g = s(graphics);
        int x2 = Math.min(g.cx + g.cw, x + width);
        int y2 = Math.min(g.cy + g.ch, y + height);
        g.cx = Math.max(g.cx, x);
        g.cy = Math.max(g.cy, y);
        g.cw = Math.max(0, x2 - g.cx);
        g.ch = Math.max(0, y2 - g.cy);
    }

    @Override
    public boolean isShapeClipSupported(Object graphics) {
        return true;
    }

    @Override
    public boolean isShapeSupported(Object graphics) {
        return true;
    }

    @Override
    public void setClip(Object graphics, Shape shape) {
        Surface g = s(graphics);
        Rectangle r = shape.getBounds();
        g.cx = r.getX();
        g.cy = r.getY();
        g.cw = r.getWidth();
        g.ch = r.getHeight();
        g.shapeClip = shape;
    }

    /// The shape clip installed by the last {@code setClip(Shape)}, or null when the
    /// clip is a plain rectangle.
    public static Shape shapeClipOf(Object graphics) {
        return s(graphics).shapeClip;
    }

    @Override
    public void pushClip(Object graphics) {
        Surface g = s(graphics);
        g.clipStack.add(new Object[] {new int[] {g.cx, g.cy, g.cw, g.ch}, g.shapeClip});
    }

    @Override
    public void popClip(Object graphics) {
        Surface g = s(graphics);
        if (g.clipStack.isEmpty()) {
            return;
        }
        Object[] top = g.clipStack.remove(g.clipStack.size() - 1);
        int[] r = (int[]) top[0];
        g.cx = r[0];
        g.cy = r[1];
        g.cw = r[2];
        g.ch = r[3];
        g.shapeClip = (Shape) top[1];
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        Surface g = s(graphics);
        int argb = 0xff000000 | g.color;
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                g.blend(xx, yy, argb, g.alpha);
            }
        }
    }

    @Override
    public void clearRect(Object graphics, int x, int y, int width, int height) {
        Surface g = s(graphics);
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                if (g.inClip(xx, yy)) {
                    g.px[yy * g.w + xx] = 0;
                }
            }
        }
    }

    @Override
    public void fillShape(Object graphics, Shape shape) {
        Surface g = s(graphics);
        Rectangle r = shape.getBounds();
        int argb = 0xff000000 | g.color;
        for (int yy = r.getY(); yy < r.getY() + r.getHeight(); yy++) {
            for (int xx = r.getX(); xx < r.getX() + r.getWidth(); xx++) {
                if (shape.contains(xx, yy)) {
                    g.blend(xx, yy, argb, g.alpha);
                }
            }
        }
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        fillRect(graphics, x, y, width, 1);
        fillRect(graphics, x, y + height, width, 1);
        fillRect(graphics, x, y, 1, height);
        fillRect(graphics, x + width, y, 1, height + 1);
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        drawRect(graphics, x, y, width, height);
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        fillRect(graphics, x, y, width, height);
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        Surface g = s(graphics);
        Surface src = s(img);
        for (int yy = 0; yy < src.h; yy++) {
            for (int xx = 0; xx < src.w; xx++) {
                g.blend(x + xx, y + yy, src.px[yy * src.w + xx], g.alpha);
            }
        }
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        Surface g = s(graphics);
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int c = rgbData[offset + yy * w + xx];
                g.blend(x + xx, y + yy, processAlpha ? c : (c | 0xff000000), 255);
            }
        }
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
    }

    @Override
    public void flushGraphics() {
    }

    // ------------------------------------------------------------------
    // Fonts: fixed metrics, nothing drawn
    // ------------------------------------------------------------------

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return length * 8;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        return str == null ? 0 : str.length() * 8;
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return 8;
    }

    @Override
    public int getHeight(Object nativeFont) {
        return 16;
    }

    @Override
    public Object getDefaultFont() {
        return font;
    }

    @Override
    public Object createFont(int face, int style, int size) {
        return font;
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
        return font;
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        return font;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
    }

    @Override
    public int getSoftkeyCount() {
        return 0;
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        return new int[] {-21};
    }

    @Override
    public int getClearKeyCode() {
        return -8;
    }

    @Override
    public int getBackspaceKeyCode() {
        return -8;
    }

    @Override
    public int getBackKeyCode() {
        return -11;
    }

    @Override
    public int getGameAction(int keyCode) {
        return 0;
    }

    @Override
    public int getKeyCode(int gameAction) {
        return 0;
    }

    @Override
    public boolean isTouchDevice() {
        return true;
    }

    @Override
    public String getPlatformName() {
        return "test";
    }

    @Override
    public L10NManager getLocalizationManager() {
        return new L10NManager("en", "US") {
        };
    }

    // ------------------------------------------------------------------
    // Networking, storage and files: unused by these tests
    // ------------------------------------------------------------------

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        throw new IOException("no network in the raster test display");
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
    }

    @Override
    public int getContentLength(Object connection) {
        return 0;
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        return 0;
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        return null;
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        return null;
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        return new String[0];
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        return new String[0];
    }

    @Override
    public void deleteStorageFile(String name) {
    }

    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public boolean storageFileExists(String name) {
        return false;
    }

    @Override
    public String[] listStorageEntries() {
        return new String[0];
    }

    @Override
    public String[] listFilesystemRoots() {
        return new String[0];
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        return new String[0];
    }

    @Override
    public long getRootSizeBytes(String root) {
        return 0;
    }

    @Override
    public long getRootAvailableSpace(String root) {
        return 0;
    }

    @Override
    public void mkdir(String directory) {
    }

    @Override
    public void deleteFile(String file) {
    }

    @Override
    public boolean isHidden(String file) {
        return false;
    }

    @Override
    public void setHidden(String file, boolean h) {
    }

    @Override
    public long getFileLength(String file) {
        return 0;
    }

    @Override
    public boolean isDirectory(String file) {
        return false;
    }

    @Override
    public boolean exists(String file) {
        return false;
    }

    @Override
    public void rename(String file, String newName) {
    }

    @Override
    public char getFileSystemSeparator() {
        return '/';
    }
}
