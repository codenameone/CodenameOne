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
    }

    @Override
    public java.lang.Object createImage(int[] a0, int a1, int a2) {
        return new Object();
    }

    @Override
    public java.lang.Object createImage(java.lang.String a0) throws java.io.IOException {
        return new Object();
    }

    @Override
    public java.lang.Object createImage(java.io.InputStream a0) throws java.io.IOException {
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

    @Override
    public int getClipX(java.lang.Object a0) {
        return 0;
    }

    @Override
    public int getClipY(java.lang.Object a0) {
        return 0;
    }

    @Override
    public int getClipWidth(java.lang.Object a0) {
        return WIDTH;
    }

    @Override
    public int getClipHeight(java.lang.Object a0) {
        return HEIGHT;
    }

    @Override
    public void setClip(java.lang.Object a0, int a1, int a2, int a3, int a4) {
    }

    @Override
    public void clipRect(java.lang.Object a0, int a1, int a2, int a3, int a4) {
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

    @Override
    public int charsWidth(java.lang.Object nativeFont, char[] ch, int offset, int len) {
        return len * CHAR_WIDTH;
    }

    @Override
    public int stringWidth(java.lang.Object nativeFont, java.lang.String text) {
        return text == null ? 0 : text.length() * CHAR_WIDTH;
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
