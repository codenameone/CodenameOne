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

/**
 * Java side of the Win32 native bridge for the Windows port. Each {@code native}
 * method here is implemented in C in the port's {@code nativeSources} (Win32,
 * Direct2D/DirectWrite, WIC, WinHTTP) and is translated and linked by ParparVM's
 * "windows" clean-target build. The method names map to ParparVM C symbols, so
 * the corresponding C function for {@code nativeLog(String)} is
 * {@code com_codename1_impl_windows_WindowsNative_nativeLog___java_lang_String}.
 *
 * <p>This skeleton declares only the lifecycle hooks the bootstrap needs; the
 * rendering, input and platform-service hooks are added in their respective
 * phases. The bodies live in the native layer, so this class only carries
 * signatures (it is never instantiated).</p>
 *
 * @author Codename One
 */
public final class WindowsNative {
    private WindowsNative() {
    }

    /**
     * Writes a line to the native debug log (stderr / OutputDebugString). Used
     * by the bootstrap before the UI is up.
     *
     * @param message the text to log
     */
    public static native void nativeLog(String message);

    /**
     * Pumps pending Win32 messages once, dispatching window/input events into
     * the Codename One event queue. The native message loop calls back into the
     * EDT through {@link WindowsImplementation#getInstance()}.
     */
    public static native void pumpMessages();
}
