/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity;

import com.codename1.system.NativeInterface;

/**
 * Bridge to the REAL native OS widgets used as fidelity references. Each platform
 * implementation (Objective-C UIKit on iOS, Material Views on Android) builds the
 * requested widget at the exact pixel size, applies the requested visual state
 * and appearance, rasterizes it OFF-SCREEN (Android: View.draw onto a Bitmap;
 * iOS: CALayer renderInContext), and returns the PNG bytes.
 *
 * Off-screen rasterization (rather than wrapping in a peer and screenshotting the
 * window) is deliberate: it is synchronous, deterministic, exactly tile-sized,
 * and independent of the GPU/compositor, so it works reliably on a headless
 * emulator/simulator where a full-window capture of a native peer can fail.
 */
public interface NativeWidgetFactory extends NativeInterface {
    /**
     * Builds the native widget identified by {@code kind} (the YAML "native" /
     * "native_android" key) at {@code widthPx} x {@code heightPx}, in the given
     * state ("normal", "pressed", "disabled", "selected") and appearance
     * ("light"/"dark"), rasterizes it to a PNG, and writes those bytes to the
     * absolute filesystem path {@code outPath} (which the caller then reads back
     * via FileSystemStorage). Returns true on success, false if the kind is
     * unknown on this platform or rendering/writing failed.
     *
     * The transport is deliberate: in this ParparVM iOS build, native methods that
     * RETURN an object (byte[] via nsDataToByteArr, or String via fromNSString) NPE
     * in the return marshaling, but String ARGUMENTS (toNSString) and primitive
     * boolean returns marshal cleanly. So the PNG path is handed IN as a String arg
     * and only a boolean comes back -- no object ever crosses the return boundary.
     */
    boolean renderWidgetToFile(String kind, String state, String appearance, String text, String outPath, int widthPx, int heightPx);

    /** True when this platform can build the given widget kind. */
    boolean isWidgetSupported(String kind);
}
