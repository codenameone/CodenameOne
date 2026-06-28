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
package com.codename1.car;

import com.codename1.car.spi.CarBridge;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;

/// The static entry point for Apple CarPlay and Google Android Auto support. Register your
/// `CarApplication` once (typically from your app's `init()`), observe connection state, and let the
/// framework drive the rest:
///
/// ```java
/// Car.setApplication(new MyCarApplication());
/// Car.addConnectionListener(new CarConnectionListener() {
///     public void carConnected(CarContext ctx)   { startLocationStream(); }
///     public void carDisconnected()              { stopLocationStream(); }
/// });
/// ```
///
/// #### How it maps to the platforms
///
/// CarPlay and Android Auto are template-based: they do not render Codename One `Form`s, only the
/// fixed `CarTemplate` catalog (list / grid / pane / message / navigation / now-playing). When a
/// head unit connects, the platform port hands the framework a `com.codename1.car.spi.CarBridge`,
/// the framework asks your `CarApplication` for a root `CarScreen`, and renders that screen's
/// template onto the head unit. On the simulator and unsupported ports there is no bridge, so the
/// API is an inert no-op and [#isCarConnected()] returns false.
///
/// #### Zero cost when unused
///
/// Merely referencing this class makes the build inject the native plumbing (CarPlay scene +
/// entitlement on iOS, the `androidx.car.app` dependency + `CarAppService` on Android). Apps that
/// never touch `com.codename1.car` get none of it.
public final class Car {
    private static CarApplication application;
    private static CarContext currentContext;
    private static final List<CarConnectionListener> listeners = new ArrayList<CarConnectionListener>();

    private Car() {
    }

    /// Registers the app's in-car experience. Call once, before a head unit connects (your `init()`
    /// is the natural place). Replacing a previously registered application is allowed.
    ///
    /// #### Parameters
    ///
    /// - `app`: the car application, or null to clear
    public static void setApplication(CarApplication app) {
        application = app;
    }

    /// Returns the registered car application, or null if none has been set.
    ///
    /// #### Returns
    ///
    /// the registered `CarApplication`, or null
    public static CarApplication getApplication() {
        return application;
    }

    /// Returns true while a head unit (CarPlay / Android Auto) is currently connected.
    ///
    /// #### Returns
    ///
    /// true if a car is connected
    public static boolean isCarConnected() {
        return Display.getInstance().isCarConnected();
    }

    /// Returns the context for the currently connected head unit, or null when none is connected.
    ///
    /// #### Returns
    ///
    /// the live `CarContext`, or null
    public static CarContext getCurrentContext() {
        return currentContext;
    }

    /// Registers a listener notified (on the EDT) when a head unit connects or disconnects.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addConnectionListener(CarConnectionListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a previously registered connection listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeConnectionListener(CarConnectionListener l) {
        listeners.remove(l);
    }

    // --- platform port entry points -----------------------------------------

    /// Framework/port entry point: begins an in-car session for the supplied bridge. The framework
    /// creates a `CarContext`, asks the registered `CarApplication` for a root screen on the EDT,
    /// renders it, and fires the connection callbacks. Called by the platform port when a head unit
    /// connects.
    ///
    /// #### Parameters
    ///
    /// - `bridge`: the platform rendering bridge for the connected head unit
    ///
    /// #### Returns
    ///
    /// the new `CarContext` (also retrievable via [#getCurrentContext()])
    public static CarContext startSession(final CarBridge bridge) {
        final CarContext ctx = new CarContext(bridge);
        currentContext = ctx;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                CarApplication app = application;
                if (app == null) {
                    com.codename1.io.Log.p("Car: a head unit connected but no CarApplication was "
                            + "registered. Call Car.setApplication(...) from your app's init().");
                    return;
                }
                CarScreen root = app.onCreateRootScreen(ctx);
                ctx.setRootScreen(root);
                app.onCarConnected(ctx);
                fireConnected(ctx);
            }
        });
        return ctx;
    }

    /// Framework/port entry point: ends the current in-car session. Fires the disconnect callbacks
    /// and tears down the screen stack. Called by the platform port when the head unit disconnects.
    public static void endSession() {
        final CarContext ctx = currentContext;
        currentContext = null;
        if (ctx == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                CarApplication app = application;
                if (app != null) {
                    app.onCarDisconnected();
                }
                fireDisconnected();
                ctx.destroy();
            }
        });
    }

    private static void fireConnected(CarContext ctx) {
        CarConnectionListener[] copy = listeners.toArray(new CarConnectionListener[listeners.size()]);
        for (CarConnectionListener l : copy) {
            l.carConnected(ctx);
        }
    }

    private static void fireDisconnected() {
        CarConnectionListener[] copy = listeners.toArray(new CarConnectionListener[listeners.size()]);
        for (CarConnectionListener l : copy) {
            l.carDisconnected();
        }
    }
}
