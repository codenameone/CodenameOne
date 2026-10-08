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
package com.codename1.designer.css;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

/**
 * Minimal stub of {@link CodenameOneImplementation} the headless CSS
 * compiler installs into {@code Display.impl} via reflection before
 * {@link CSSTheme#load} runs.
 *
 * <p>Why this exists: the CSS compiler reads CSS source, builds a theme
 * Hashtable in memory and serializes it to a {@code .res} file. It does
 * not render anything - no graphics, no text shaping, no networking. But
 * a handful of CN1 core classes (Font, Util, Display, UIManager) call
 * through {@link com.codename1.ui.Display#getInstance} -&gt;
 * {@code impl.something()} during theme construction (font face/size
 * round-trips, cleanup of theme-resource streams, dip-&gt;pixel
 * conversion for {@code mm} units, and so on). Without an installed
 * implementation those calls NPE.
 *
 * <p>Rather than littering CN1 core with {@code if (impl == null)}
 * fallbacks, the CSS compiler installs this stub at startup through
 * {@link #install()}. The pattern mirrors what the unit-test module
 * does with {@code TestCodenameOneImplementation}: provide a minimal
 * subclass and inject it via reflection.
 *
 * <p>This is the only implementation the compiler ever runs on. It needs
 * no display: images are plain {@link BufferedImage}s decoded by ImageIO,
 * which works under {@code java.awt.headless=true}.
 *
 * <p>Most overrides return zero / null / -1 / false. The only methods
 * that need to do real work are the ones that the theme-build path
 * actually calls:
 * <ul>
 *   <li>{@link #createFont(int,int,int)} - Font's constructor stores
 *       the returned object as {@code font}, then later asks
 *       {@link #getFace(Object)} / {@link #getSize(Object)} /
 *       {@link #getStyle(Object)} for the original face/style/size.
 *       We round-trip via a small {@link Triple} carrier.</li>
 *   <li>{@link #convertToPixels(int,boolean)} and
 *       {@link #getDeviceDensity()} - pinned to the constants below. A few
 *       values the compiler serializes are computed from them, so they
 *       must not depend on the machine the build runs on.</li>
 *   <li>the image methods - {@code url()} images are decoded to read
 *       their size, scaled into the densities of a multi-image, and
 *       re-encoded, all through {@link BufferedImage}.</li>
 *   <li>{@link #cleanup(Object)} - closes any closeable streams used
 *       by Util.copy when serializing the resource.</li>
 * </ul>
 */
public final class HeadlessCssCompilerImplementation extends CodenameOneImplementation {

    /**
     * Pixels per millimetre the compiler assumes wherever theme construction
     * asks the display to convert a length. The value is the one application
     * themes have always been compiled with: the compiler used to run on the
     * simulator port with no skin, which on an ordinary (non-retina) display
     * reports {@link com.codename1.ui.Display#DENSITY_MEDIUM} and converts at
     * five pixels to the millimetre. That was measured, not derived: the same
     * stylesheet compiled by that compiler and by this one produces the same
     * theme entry for entry. Changing it changes serialized defaults (the
     * shadow spread of a round border, for one), so it is a constant rather
     * than something read from the host.
     */
    public static final int PIXELS_PER_MILLIMETRE = 5;

    /** The density that goes with {@link #PIXELS_PER_MILLIMETRE}. */
    public static final int DEVICE_DENSITY = com.codename1.ui.Display.DENSITY_MEDIUM;

    /**
     * See {@link #setNativeThemeUnits(boolean)}.
     */
    private static boolean nativeThemeUnits;

    /**
     * Selects the unit conversion the framework's own native themes are
     * compiled with, in place of {@link #PIXELS_PER_MILLIMETRE}.
     *
     * <p>Those themes have always been compiled on an implementation that
     * answered zero pixels for any length under half a metre, so a round
     * border in them carries no default shadow spread, where the same rule in
     * an application theme carries the 10px / 1mm the constants above produce.
     * That spread adds to a component's size, and the native themes are tuned
     * and screenshot-tested to the pixel without it. Both sets of themes are
     * shipped, so both conversions are kept rather than resizing one of them;
     * the native-themes build asks for this one explicitly.
     */
    public static void setNativeThemeUnits(boolean nativeThemeUnits) {
        HeadlessCssCompilerImplementation.nativeThemeUnits = nativeThemeUnits;
    }

    /**
     * Installs this implementation into {@code Display} and {@code Util}
     * unless an implementation is already present. Idempotent.
     *
     * <p>{@code Display.impl} is package-private and there is no public
     * installer, so the field is set reflectively; {@code Util} keeps its own
     * reference, which has a public setter.
     */
    public static void install() {
        try {
            Class<?> displayCls = Class.forName("com.codename1.ui.Display");
            java.lang.reflect.Field implField = displayCls.getDeclaredField("impl");
            implField.setAccessible(true);
            Object current = implField.get(null);
            if (current == null) {
                current = new HeadlessCssCompilerImplementation();
                implField.set(null, current);
            }
            if (current instanceof HeadlessCssCompilerImplementation) {
                com.codename1.io.Util.setImplementation((CodenameOneImplementation) current);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not install the headless CSS compiler implementation", ex);
        }
    }

    private static final class Triple {
        final int face, style, size;
        Triple(int f, int s, int sz) { this.face = f; this.style = s; this.size = sz; }
    }

    @Override public Object createFont(int face, int style, int size) {
        return new Triple(face, style, size);
    }
    @Override public int getFace(Object nativeFont) {
        return nativeFont instanceof Triple ? ((Triple) nativeFont).face : 0;
    }
    @Override public int getStyle(Object nativeFont) {
        return nativeFont instanceof Triple ? ((Triple) nativeFont).style : 0;
    }
    @Override public int getSize(Object nativeFont) {
        return nativeFont instanceof Triple ? ((Triple) nativeFont).size : 0;
    }
    @Override public int convertToPixels(int dipCount, boolean horizontal) {
        if (nativeThemeUnits) {
            // The argument is in thousandths of a millimetre and the caller
            // divides the answer by a thousand again, so this is zero for any
            // length a theme uses. See setNativeThemeUnits.
            return Math.round(dipCount / 1000f);
        }
        return dipCount * PIXELS_PER_MILLIMETRE;
    }
    @Override public int getDeviceDensity() {
        if (nativeThemeUnits) {
            // What a display of no size has always been classified as.
            return com.codename1.ui.Display.DENSITY_VERY_LOW;
        }
        return DEVICE_DENSITY;
    }

    // ---- Images: BufferedImage is the native image. ----

    private static BufferedImage toArgb(BufferedImage src) {
        if (src == null) {
            return null;
        }
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        return HeadlessImages.toArgb(src);
    }

    @Override public Object createImage(byte[] bytes, int offset, int len) {
        try {
            return toArgb(ImageIO.read(new ByteArrayInputStream(bytes, offset, len)));
        } catch (IOException ex) {
            // The caller (EncodedImage) reports a null image as "create image
            // failed", which is the right outcome for bytes that are not an image.
            return null;
        }
    }
    @Override public Object createImage(InputStream i) throws IOException {
        return toArgb(ImageIO.read(i));
    }
    @Override public Object createImage(String path) throws IOException {
        InputStream in = new FileInputStream(path);
        try {
            return toArgb(ImageIO.read(in));
        } finally {
            in.close();
        }
    }
    @Override public Object createImage(int[] rgb, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, rgb, 0, width);
        return out;
    }
    @Override public Object createMutableImage(int width, int height, int fillColor) {
        BufferedImage out = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        if (fillColor != 0) {
            int[] row = new int[out.getWidth()];
            java.util.Arrays.fill(row, fillColor);
            for (int y = 0; y < out.getHeight(); y++) {
                out.setRGB(0, y, row.length, 1, row, 0, row.length);
            }
        }
        return out;
    }
    @Override public int getImageWidth(Object i) {
        return i instanceof BufferedImage ? ((BufferedImage) i).getWidth() : 0;
    }
    @Override public int getImageHeight(Object i) {
        return i instanceof BufferedImage ? ((BufferedImage) i).getHeight() : 0;
    }
    @Override public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        if (nativeImage instanceof BufferedImage) {
            ((BufferedImage) nativeImage).getRGB(x, y, width, height, arr, offset, width);
        }
    }
    @Override public Object scale(Object nativeImage, int width, int height) {
        if (!(nativeImage instanceof BufferedImage)) {
            return nativeImage;
        }
        return HeadlessImages.scale((BufferedImage) nativeImage, Math.max(1, width), Math.max(1, height));
    }
    @Override public void cleanup(Object o) {
        if (o instanceof java.io.Closeable) {
            try { ((java.io.Closeable) o).close(); } catch (IOException ignored) {}
        }
    }

    // ---- Everything below is unreachable from the css-compiler path; ----
    // ---- the overrides exist only so the abstract class compiles.    ----

    @Override public void init(Object m) {}
    @Override public int getDisplayWidth() { return 0; }
    @Override public int getDisplayHeight() { return 0; }
    @Override public void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {}
    @Override public void flushGraphics(int x, int y, int width, int height) {}
    @Override public void flushGraphics() {}
    @Override public int getSoftkeyCount() { return 0; }
    @Override public int[] getSoftkeyCode(int index) { return new int[0]; }
    @Override public int getClearKeyCode() { return 0; }
    @Override public int getBackspaceKeyCode() { return 0; }
    @Override public int getBackKeyCode() { return 0; }
    @Override public int getGameAction(int keyCode) { return 0; }
    @Override public int getKeyCode(int gameAction) { return 0; }
    @Override public boolean isTouchDevice() { return false; }
    @Override public int getColor(Object graphics) { return 0; }
    @Override public void setColor(Object graphics, int rgb) {}
    @Override public void setAlpha(Object graphics, int alpha) {}
    @Override public int getAlpha(Object graphics) { return 255; }
    @Override public void setNativeFont(Object graphics, Object font) {}
    @Override public int getClipX(Object graphics) { return 0; }
    @Override public int getClipY(Object graphics) { return 0; }
    @Override public int getClipWidth(Object graphics) { return 0; }
    @Override public int getClipHeight(Object graphics) { return 0; }
    @Override public void setClip(Object graphics, int x, int y, int width, int height) {}
    @Override public void clipRect(Object graphics, int x, int y, int width, int height) {}
    @Override public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {}
    @Override public void fillRect(Object graphics, int x, int y, int width, int height) {}
    @Override public void drawRect(Object graphics, int x, int y, int width, int height) {}
    @Override public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {}
    @Override public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {}
    @Override public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {}
    @Override public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {}
    @Override public void drawString(Object graphics, String str, int x, int y) {}
    @Override public void drawImage(Object graphics, Object img, int x, int y) {}
    @Override public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {}
    @Override public Object getNativeGraphics() { return null; }
    @Override public Object getNativeGraphics(Object image) { return null; }
    @Override public int charsWidth(Object nativeFont, char[] ch, int offset, int length) { return 0; }
    @Override public int stringWidth(Object nativeFont, String str) { return 0; }
    @Override public int charWidth(Object nativeFont, char ch) { return 0; }
    @Override public int getHeight(Object nativeFont) { return 0; }
    @Override public Object getDefaultFont() { return new Triple(0, 0, 0); }
    @Override public Object connect(String url, boolean read, boolean write) throws IOException { return null; }
    @Override public void setHeader(Object connection, String key, String val) {}
    @Override public int getContentLength(Object connection) { return 0; }
    @Override public OutputStream openOutputStream(Object connection) throws IOException { return null; }
    @Override public OutputStream openOutputStream(Object connection, int offset) throws IOException { return null; }
    @Override public InputStream openInputStream(Object connection) throws IOException { return null; }
    @Override public void setPostRequest(Object connection, boolean p) {}
    @Override public int getResponseCode(Object connection) throws IOException { return 0; }
    @Override public String getResponseMessage(Object connection) throws IOException { return null; }
    @Override public String getHeaderField(String name, Object connection) throws IOException { return null; }
    @Override public String[] getHeaderFieldNames(Object connection) throws IOException { return new String[0]; }
    @Override public String[] getHeaderFields(String name, Object connection) throws IOException { return new String[0]; }
    @Override public void deleteStorageFile(String name) {}
    @Override public OutputStream createStorageOutputStream(String name) throws IOException { return null; }
    @Override public InputStream createStorageInputStream(String name) throws IOException { return null; }
    @Override public boolean storageFileExists(String name) { return false; }
    @Override public String[] listStorageEntries() { return new String[0]; }
    @Override public String[] listFilesystemRoots() { return new String[0]; }
    @Override public String[] listFiles(String directory) throws IOException { return new String[0]; }
    @Override public long getRootSizeBytes(String root) { return 0; }
    @Override public long getRootAvailableSpace(String root) { return 0; }
    @Override public void mkdir(String directory) {}
    @Override public void deleteFile(String file) {}
    @Override public boolean isHidden(String file) { return false; }
    @Override public void setHidden(String file, boolean h) {}
    @Override public long getFileLength(String file) { return 0; }
    @Override public boolean isDirectory(String file) { return false; }
    @Override public boolean exists(String file) { return false; }
    @Override public void rename(String file, String newName) {}
    @Override public char getFileSystemSeparator() { return '/'; }
    @Override public String getPlatformName() { return "headless"; }
    @Override public L10NManager getLocalizationManager() { return null; }
}
