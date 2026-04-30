/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.designer.css;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
 * fallbacks, the css-compiler module installs this stub at startup
 * (NoCefCSSCLI.main). The pattern mirrors what the unit-test module
 * does with {@code TestCodenameOneImplementation}: provide a minimal
 * subclass and inject it via reflection.
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
 *   <li>{@link #convertToPixels(int,boolean)} - returns 1:1 (1 mm = 1 pixel)
 *       so theme padding/margin serialization does not collapse to
 *       zero. The actual conversion happens at app runtime when a
 *       full implementation is loaded.</li>
 *   <li>{@link #cleanup(Object)} - closes any closeable streams used
 *       by Util.copy when serializing the resource.</li>
 * </ul>
 */
final class HeadlessCssCompilerImplementation extends CodenameOneImplementation {

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
        // 1:1 - the real device DPI is only needed at app runtime.
        return Math.round(dipCount / 1000f);
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
    @Override public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {}
    @Override public Object createImage(int[] rgb, int width, int height) { return null; }
    @Override public Object createImage(String path) throws IOException { return null; }
    @Override public Object createImage(InputStream i) throws IOException { return null; }
    @Override public Object createMutableImage(int width, int height, int fillColor) { return null; }
    @Override public Object createImage(byte[] bytes, int offset, int len) { return null; }
    @Override public int getImageWidth(Object i) { return 0; }
    @Override public int getImageHeight(Object i) { return 0; }
    @Override public Object scale(Object nativeImage, int width, int height) { return nativeImage; }
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
