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
package com.codename1.impl.ios.sim.bridge;

/**
 * The rendering bridge between the isolated app universe and the native
 * pipeline. The user's app runs in a child-first classloader with its own
 * copy of the Codename One core; its implementation cannot touch JNI (a
 * native library binds to exactly one classloader), so every graphics, font
 * and image operation crosses this interface into the parent universe, which
 * holds the real native bindings.
 *
 * <p>This package is the one com.codename1 namespace the child loader
 * delegates to the parent, so both universes see the same interface classes.
 * All signatures are deliberately limited to primitives, Strings and arrays -
 * types loaded by the bootstrap loader and therefore shared.</p>
 *
 * <p>Coordinates are app-local: the parent implementation translates them
 * into the device-skin screen rectangle and confines clipping to it, which is
 * how the app's output composites inside the skin the parent painted.</p>
 */
public interface RenderBridge {
    /* ---- display ---------------------------------------------------------- */

    /** @return the app screen width (the skin's screen rectangle width) */
    int getAppWidth();

    /** @return the app screen height */
    int getAppHeight();

    /** Flushes queued drawing operations to the window. */
    void flush();

    /* ---- graphics ops (color is RGB, alpha 0-255) ------------------------- */

    void setClip(int x, int y, int w, int h);

    void fillRect(int color, int alpha, int x, int y, int w, int h);

    void drawLine(int color, int alpha, int x1, int y1, int x2, int y2);

    void drawString(int color, int alpha, long fontPeer, String str, int x, int y);

    /* ---- fonts (peers are native handles) ---------------------------------- */

    long createSystemFont(int face, int style, int size);

    long createTruetypeFont(String name);

    long deriveTruetypeFont(long peer, boolean bold, boolean italic, float size);

    int stringWidth(long peer, String str);

    int charWidth(long peer, char ch);

    int fontHeight(long peer);

    int fontAscent(long peer);

    int fontDescent(long peer);

    /* ---- images (peers are native handles) --------------------------------- */

    /**
     * Decodes an encoded (PNG/JPEG) image.
     *
     * @param data the encoded bytes
     * @return {peer, width, height} or null when decoding fails
     */
    long[] createImage(byte[] data);

    /**
     * Creates an image from ARGB pixels.
     *
     * @return the image peer
     */
    long createImageFromARGB(int[] argb, int w, int h);

    /**
     * Reads pixels back from an image into the given ARGB array.
     */
    void imageRgbToIntArray(long peer, int[] arr, int x, int y, int w, int h, int imgW, int imgH);

    /**
     * @return a new image peer scaled to the given size
     */
    long scaleImage(long peer, int w, int h);

    void drawImage(long peer, int alpha, int x, int y, int w, int h);

    void tileImage(long peer, int alpha, int x, int y, int w, int h);

    /**
     * Releases a native peer (image or font) created through this bridge.
     */
    void releasePeer(long peer);

    /* ---- native menu -------------------------------------------------------- */

    /**
     * Installs the native menu bar from encoded command rows (one per line:
     * {@code menuHint\tlabel\tshortcutKeyChar\tshortcutModifiers} - the same
     * format the Mac Catalyst port uses). Menu selections fire back through
     * the registered {@link MenuDispatcher} with the row index.
     *
     * @param encodedCommands the encoded rows, or an empty string to clear
     */
    void setNativeMenu(String encodedCommands);

    /* ---- diagnostics ------------------------------------------------------- */

    /**
     * Writes the most recently presented window frame to a PNG file. A
     * non-positive width or height saves the whole window (skin included);
     * otherwise the output is cropped to the given window rectangle.
     *
     * @param path destination PNG path
     * @return true when written
     */
    boolean saveScreenshot(String path, int x, int y, int w, int h);

    /**
     * Starts a native text-editing session over the given UNIVERSE-LOCAL
     * rectangle (this bridge translates to window coordinates). The caller
     * registers an {@link BridgeRegistry#setEditingCallback editing callback}
     * first; the commit arrives through it.
     *
     * <p>editString is deliberately NOT passed a Component: the relay has no
     * copy of the app's theme, so a reconstructed component would query default
     * (unstyled) values. Instead the caller resolves the editing component's
     * full style here and sends the concrete values; the relay builds a
     * synthetic field with each value set explicitly so the native port reads
     * exactly what the app's component would have reported.</p>
     *
     * @param text the initial text
     * @param fontHeightPx the resolved font pixel height (drives the native font size)
     * @param fontStyle CN1 Font style flags (plain/bold/italic)
     * @param fgColor foreground RGB
     * @param bgColor background RGB
     * @param bgTransparency background alpha 0-255 (0 = transparent, app field shows through)
     */
    void editString(String text, int x, int y, int w, int h,
            int fontHeightPx, int fontStyle, int fgColor, int bgColor, int bgTransparency,
            boolean multiline, int constraint, int align);

    /**
     * Fills or strokes a path (GeneralPath commands + coords in
     * universe-local space). Powers fillShape/drawShape - RoundRectBorder,
     * themed dialogs and field borders render through this.
     *
     * @param stroke false fills the path, true strokes it with the pen
     */
    void shape(byte[] commands, int commandsLen, float[] points, int pointsLen,
            int color, int alpha, boolean stroke, float lineWidth,
            int capStyle, int joinStyle, float miterLimit);

    /* ---- mutable images (Image.createImage(w,h), transition buffers) -------
     * Draw calls take effect immediately in a CPU-side bitmap; finishMutable
     * publishes the pixels so the peer can be drawn as a regular image. */

    long createMutableImage(int w, int h, int argb);

    void mutableClip(long peer, int x, int y, int w, int h);

    void mutableFillRect(long peer, int color, int alpha, int x, int y, int w, int h);

    void mutableDrawLine(long peer, int color, int alpha, int x1, int y1, int x2, int y2);

    void mutableDrawString(long peer, long fontPeer, int color, int alpha, String str,
            int x, int y);

    void mutableDrawImage(long peer, long imgPeer, int alpha, int x, int y, int w, int h);

    void mutableShape(long peer, byte[] commands, int commandsLen, float[] points,
            int pointsLen, int color, int alpha, boolean stroke, float lineWidth,
            int capStyle, int joinStyle, float miterLimit);

    void finishMutable(long peer);

    /* ---- native peers (browser, media) + file picker ------------------------
     * Peers are real native views floated over the app's window region;
     * frames are universe-local and translated by this bridge. */

    long peerCreateWebView();

    void peerWebLoadURL(long peer, String url);

    void peerWebLoadHTML(long peer, String html, String baseUrl);

    void peerSetFrame(long peer, int x, int y, int w, int h);

    void peerRemove(long peer);

    void peerRelease(long peer);

    /** AVPlayer-backed media; video peers position via peerSetFrame */
    long mediaCreate(String url, boolean video);

    /** op: 0=play 1=pause 2=seek(arg ms) */
    void mediaControl(long peer, int op, int arg);

    /** what: 0=time ms 1=duration ms 2=playing(0/1) */
    int mediaQuery(long peer, int what);

    /**
     * Opens the native file panel; register a
     * {@link BridgeRegistry#setPickCallback pick callback} first.
     */
    void pickFile();

    /** Open the native camera and capture a still; the result is delivered
     *  asynchronously to the registered PickCallback (null on cancel/deny). */
    void capturePhoto();

    /**
     * Opens a real camera session whose preview view positions via
     * {@link #peerSetFrame}. Backends without camera support return 0.
     *
     * @return the preview peer handle or 0
     */
    default long peerCreateCamera() {
        return 0;
    }
}
