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
package com.codename1.impl.javase.simulator.spi;

import com.codename1.impl.CodenameOneImplementation;

/**
 * A simulator rendering backend: the source of the CodenameOneImplementation
 * the app runs against, plus the screen surface the simulator chrome hosts.
 *
 * <p>The default backend is the Swing JavaSEPort. Native backends (the iOS
 * port compiled to a dylib on macOS, the Windows port to a dll) implement this
 * interface and are discovered through the {@code ServiceLoader} mechanism;
 * the {@code cn1.simulator.backend} system property selects which backend id
 * to activate.</p>
 *
 * <p>Regardless of the backend, the simulator tooling (network monitor,
 * performance monitor, location simulation) is layered over the
 * implementation via the decorator chain installed by ImplementationFactory,
 * and the Swing chrome (menus, skin frame, dockable tool panels) hosts the
 * backend's screen component.</p>
 */
public interface SimulatorBackend {
    /**
     * @return the identifier matched against the cn1.simulator.backend system
     * property (e.g. "swing", "ios", "windows")
     */
    String getId();

    /**
     * Creates the raw backend implementation, before the simulator tool
     * decorators are layered on top.
     *
     * @return a new implementation instance
     */
    CodenameOneImplementation createImplementation();

    /**
     * Returns the AWT component displaying the running app's screen. For the
     * Swing backend this is a lightweight JPanel; native backends return a
     * heavyweight java.awt.Canvas whose surface the native library renders
     * into (CAMetalLayer on macOS, HWND swap chain on Windows).
     *
     * @return the screen component, or null before the implementation is
     * initialized
     */
    java.awt.Component getScreenComponent();

    /**
     * @return true when this backend can host native peer components (browser,
     * media views) inside the simulator window
     */
    boolean supportsNativePeers();

    /**
     * Injects a pointer event into the running app as if the user touched the
     * screen. Coordinates are in CN1 display space. Used by skin hotspots and
     * TestRecorder playback.
     *
     * @param type one of java.awt.event.MouseEvent.MOUSE_PRESSED,
     * MOUSE_RELEASED, MOUSE_DRAGGED
     * @param x horizontal position in display coordinates
     * @param y vertical position in display coordinates
     */
    void injectPointerEvent(int type, int x, int y);

    /**
     * Injects a key event into the running app. Used by skin hotspots
     * (soft buttons) and TestRecorder playback.
     *
     * @param type one of java.awt.event.KeyEvent.KEY_PRESSED, KEY_RELEASED
     * @param keyCode the CN1 key code
     */
    void injectKeyEvent(int type, int keyCode);

    /**
     * Registers a listener observing the user's input as it is dispatched to
     * the app. Input originates inside the backend (AWT events on the screen
     * component), so recording tools tap it here rather than through the
     * implementation decorators.
     *
     * @param listener the listener to add
     */
    void addInputListener(SimulatorInputListener listener);

    /**
     * Stops the backend, releasing native resources. Called when the
     * simulator shuts down or restarts the app (hot reload).
     */
    void stop();
}
