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
package com.codename1.impl.windows;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Native Windows (Win32, desktop / tablet) implementation of the Codename One
 * platform layer. The runtime is produced by ParparVM's "clean" C target and
 * linked into a standalone executable; the Direct2D/DirectWrite rendering and
 * Win32 windowing/input live in the bundled nativeSources and are reached
 * through the {@link WindowsNative} peer.
 *
 * <p>This is the scaffolding skeleton: every platform hook is present so the
 * port compiles against the core, and each is filled in across the windowing,
 * graphics, input and platform-service phases. Unimplemented hooks throw
 * {@link UnsupportedOperationException} rather than silently returning a wrong
 * value.</p>
 *
 * @author Codename One
 */
public class WindowsImplementation extends CodenameOneImplementation {
    private static WindowsImplementation INSTANCE;

    /**
     * Registers the singleton so the Win32 native bootstrap and message loop can
     * route events back into the EDT through {@link #getInstance()}.
     */
    public WindowsImplementation() {
        INSTANCE = this;
    }

    /**
     * The single live implementation instance, or {@code null} before the port
     * has been constructed.
     */
    public static WindowsImplementation getInstance() {
        return INSTANCE;
    }

    @Override
    public void init(Object m) {
        throw new UnsupportedOperationException("WindowsImplementation.init is not implemented yet");
    }

    @Override
    public int getDisplayWidth() {
        throw new UnsupportedOperationException("WindowsImplementation.getDisplayWidth is not implemented yet");
    }

    @Override
    public int getDisplayHeight() {
        throw new UnsupportedOperationException("WindowsImplementation.getDisplayHeight is not implemented yet");
    }

    @Override
    public void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        throw new UnsupportedOperationException("WindowsImplementation.editString is not implemented yet");
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.flushGraphics is not implemented yet");
    }

    @Override
    public void flushGraphics() {
        throw new UnsupportedOperationException("WindowsImplementation.flushGraphics is not implemented yet");
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.getRGB is not implemented yet");
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.createImage is not implemented yet");
    }

    @Override
    public Object createImage(String path) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.createImage is not implemented yet");
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.createImage is not implemented yet");
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        throw new UnsupportedOperationException("WindowsImplementation.createMutableImage is not implemented yet");
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        throw new UnsupportedOperationException("WindowsImplementation.createImage is not implemented yet");
    }

    @Override
    public int getImageWidth(Object i) {
        throw new UnsupportedOperationException("WindowsImplementation.getImageWidth is not implemented yet");
    }

    @Override
    public int getImageHeight(Object i) {
        throw new UnsupportedOperationException("WindowsImplementation.getImageHeight is not implemented yet");
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.scale is not implemented yet");
    }

    @Override
    public int getSoftkeyCount() {
        throw new UnsupportedOperationException("WindowsImplementation.getSoftkeyCount is not implemented yet");
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        throw new UnsupportedOperationException("WindowsImplementation.getSoftkeyCode is not implemented yet");
    }

    @Override
    public int getClearKeyCode() {
        throw new UnsupportedOperationException("WindowsImplementation.getClearKeyCode is not implemented yet");
    }

    @Override
    public int getBackspaceKeyCode() {
        throw new UnsupportedOperationException("WindowsImplementation.getBackspaceKeyCode is not implemented yet");
    }

    @Override
    public int getBackKeyCode() {
        throw new UnsupportedOperationException("WindowsImplementation.getBackKeyCode is not implemented yet");
    }

    @Override
    public int getGameAction(int keyCode) {
        throw new UnsupportedOperationException("WindowsImplementation.getGameAction is not implemented yet");
    }

    @Override
    public int getKeyCode(int gameAction) {
        throw new UnsupportedOperationException("WindowsImplementation.getKeyCode is not implemented yet");
    }

    @Override
    public boolean isTouchDevice() {
        throw new UnsupportedOperationException("WindowsImplementation.isTouchDevice is not implemented yet");
    }

    @Override
    public int getColor(Object graphics) {
        throw new UnsupportedOperationException("WindowsImplementation.getColor is not implemented yet");
    }

    @Override
    public void setColor(Object graphics, int rgb) {
        throw new UnsupportedOperationException("WindowsImplementation.setColor is not implemented yet");
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        throw new UnsupportedOperationException("WindowsImplementation.setAlpha is not implemented yet");
    }

    @Override
    public int getAlpha(Object graphics) {
        throw new UnsupportedOperationException("WindowsImplementation.getAlpha is not implemented yet");
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        throw new UnsupportedOperationException("WindowsImplementation.setNativeFont is not implemented yet");
    }

    @Override
    public int getClipX(Object graphics) {
        throw new UnsupportedOperationException("WindowsImplementation.getClipX is not implemented yet");
    }

    @Override
    public int getClipY(Object graphics) {
        throw new UnsupportedOperationException("WindowsImplementation.getClipY is not implemented yet");
    }

    @Override
    public int getClipWidth(Object graphics) {
        throw new UnsupportedOperationException("WindowsImplementation.getClipWidth is not implemented yet");
    }

    @Override
    public int getClipHeight(Object graphics) {
        throw new UnsupportedOperationException("WindowsImplementation.getClipHeight is not implemented yet");
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.setClip is not implemented yet");
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.clipRect is not implemented yet");
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        throw new UnsupportedOperationException("WindowsImplementation.drawLine is not implemented yet");
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.fillRect is not implemented yet");
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("WindowsImplementation.drawRect is not implemented yet");
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        throw new UnsupportedOperationException("WindowsImplementation.drawRoundRect is not implemented yet");
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        throw new UnsupportedOperationException("WindowsImplementation.fillRoundRect is not implemented yet");
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        throw new UnsupportedOperationException("WindowsImplementation.fillArc is not implemented yet");
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        throw new UnsupportedOperationException("WindowsImplementation.drawArc is not implemented yet");
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        throw new UnsupportedOperationException("WindowsImplementation.drawString is not implemented yet");
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        throw new UnsupportedOperationException("WindowsImplementation.drawImage is not implemented yet");
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        throw new UnsupportedOperationException("WindowsImplementation.drawRGB is not implemented yet");
    }

    @Override
    public Object getNativeGraphics() {
        throw new UnsupportedOperationException("WindowsImplementation.getNativeGraphics is not implemented yet");
    }

    @Override
    public Object getNativeGraphics(Object image) {
        throw new UnsupportedOperationException("WindowsImplementation.getNativeGraphics is not implemented yet");
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        throw new UnsupportedOperationException("WindowsImplementation.charsWidth is not implemented yet");
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        throw new UnsupportedOperationException("WindowsImplementation.stringWidth is not implemented yet");
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        throw new UnsupportedOperationException("WindowsImplementation.charWidth is not implemented yet");
    }

    @Override
    public int getHeight(Object nativeFont) {
        throw new UnsupportedOperationException("WindowsImplementation.getHeight is not implemented yet");
    }

    @Override
    public Object getDefaultFont() {
        throw new UnsupportedOperationException("WindowsImplementation.getDefaultFont is not implemented yet");
    }

    @Override
    public Object createFont(int face, int style, int size) {
        throw new UnsupportedOperationException("WindowsImplementation.createFont is not implemented yet");
    }

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.connect is not implemented yet");
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        throw new UnsupportedOperationException("WindowsImplementation.setHeader is not implemented yet");
    }

    @Override
    public int getContentLength(Object connection) {
        throw new UnsupportedOperationException("WindowsImplementation.getContentLength is not implemented yet");
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.openOutputStream is not implemented yet");
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.openOutputStream is not implemented yet");
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.openInputStream is not implemented yet");
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        throw new UnsupportedOperationException("WindowsImplementation.setPostRequest is not implemented yet");
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.getResponseCode is not implemented yet");
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.getResponseMessage is not implemented yet");
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.getHeaderField is not implemented yet");
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.getHeaderFieldNames is not implemented yet");
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.getHeaderFields is not implemented yet");
    }

    @Override
    public void deleteStorageFile(String name) {
        throw new UnsupportedOperationException("WindowsImplementation.deleteStorageFile is not implemented yet");
    }

    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.createStorageOutputStream is not implemented yet");
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.createStorageInputStream is not implemented yet");
    }

    @Override
    public boolean storageFileExists(String name) {
        throw new UnsupportedOperationException("WindowsImplementation.storageFileExists is not implemented yet");
    }

    @Override
    public String[] listStorageEntries() {
        throw new UnsupportedOperationException("WindowsImplementation.listStorageEntries is not implemented yet");
    }

    @Override
    public String[] listFilesystemRoots() {
        throw new UnsupportedOperationException("WindowsImplementation.listFilesystemRoots is not implemented yet");
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        throw new UnsupportedOperationException("WindowsImplementation.listFiles is not implemented yet");
    }

    @Override
    public long getRootSizeBytes(String root) {
        throw new UnsupportedOperationException("WindowsImplementation.getRootSizeBytes is not implemented yet");
    }

    @Override
    public long getRootAvailableSpace(String root) {
        throw new UnsupportedOperationException("WindowsImplementation.getRootAvailableSpace is not implemented yet");
    }

    @Override
    public void mkdir(String directory) {
        throw new UnsupportedOperationException("WindowsImplementation.mkdir is not implemented yet");
    }

    @Override
    public void deleteFile(String file) {
        throw new UnsupportedOperationException("WindowsImplementation.deleteFile is not implemented yet");
    }

    @Override
    public boolean isHidden(String file) {
        throw new UnsupportedOperationException("WindowsImplementation.isHidden is not implemented yet");
    }

    @Override
    public void setHidden(String file, boolean h) {
        throw new UnsupportedOperationException("WindowsImplementation.setHidden is not implemented yet");
    }

    @Override
    public long getFileLength(String file) {
        throw new UnsupportedOperationException("WindowsImplementation.getFileLength is not implemented yet");
    }

    @Override
    public boolean isDirectory(String file) {
        throw new UnsupportedOperationException("WindowsImplementation.isDirectory is not implemented yet");
    }

    @Override
    public boolean exists(String file) {
        throw new UnsupportedOperationException("WindowsImplementation.exists is not implemented yet");
    }

    @Override
    public void rename(String file, String newName) {
        throw new UnsupportedOperationException("WindowsImplementation.rename is not implemented yet");
    }

    @Override
    public char getFileSystemSeparator() {
        throw new UnsupportedOperationException("WindowsImplementation.getFileSystemSeparator is not implemented yet");
    }

    @Override
    public String getPlatformName() {
        throw new UnsupportedOperationException("WindowsImplementation.getPlatformName is not implemented yet");
    }

    @Override
    public L10NManager getLocalizationManager() {
        throw new UnsupportedOperationException("WindowsImplementation.getLocalizationManager is not implemented yet");
    }

}
