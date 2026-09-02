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
package com.codename1.impl.javase;

import com.codename1.continuity.AppState;
import com.codename1.continuity.Continuity;
import com.codename1.continuity.StateCodec;
import com.codename1.continuity.sync.SyncedStore;
import com.codename1.impl.continuity.LocalContinuityBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Simulator hooks that script state restoration and continuity.
///
/// Registered in `META-INF/codenameone/simulator-hooks.properties`. The labelled ones become a
/// Simulate menu; every one is callable from a test with `CN.execute("continuity:itemN")`.
///
/// #### These reproduce traps, not happy paths
///
/// Restoring cleanly is what the app does anyway. What is worth a click is the state that arrives
/// while the user is midway through something, the one that arrives before there is a form to show
/// it on, the one from a build whose routes have since been renamed, and the synced store changing
/// underneath a screen that already read it. Each is a real device behaviour an app written
/// against the cheerful path gets wrong, and each is otherwise reachable only by arranging two
/// devices.
public final class ContinuitySimulatorHooks {

    private ContinuitySimulatorHooks() {
    }

    private static LocalContinuityBridge bridge() {
        return JavaSEPort.getSimulatedContinuity();
    }

    /// Hands what this app is currently advertising straight back to it, as though the user had
    /// picked it up on a second device.
    ///
    /// This is the whole feature in one click: publish, then continue. It does nothing when the
    /// app has not taken a checkpoint yet, which is itself the answer to "why is nothing being
    /// offered".
    public static void continueHere() {
        bridge().simulateArrival();
    }

    /// Delivers a state that names a route the build no longer has.
    ///
    /// An app is rebuilt and a screen goes away, and the states already sitting on the user's
    /// other devices still name it. The restore has to survive that with the frames it can still
    /// build rather than losing the session over one screen the user was not even on.
    public static void continueWithAStaleRoute() {
        AppState state = new AppState();
        List<String> routes = new ArrayList<String>();
        routes.add("/a-route-this-build-no-longer-has");
        state.setRoutes(routes)
                .setDeviceId("simulated-device")
                .setSequence(System.currentTimeMillis())
                .setTimestamp(System.currentTimeMillis())
                .setTitle("From a older build");
        deliver(state);
    }

    /// Delivers a state whose payload is present but whose route stack is empty, which is what an
    /// app that does not use `@Route` produces.
    ///
    /// The framework restores nothing on its own here: the payload goes to the `StateProvider` and
    /// showing a form is the app's job. An app that assumed `restore()` always shows something
    /// finds out here rather than on a customer's phone.
    public static void continuePayloadOnly() {
        AppState state = new AppState();
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("simulated", Boolean.TRUE);
        state.setPayload(payload)
                .setDeviceId("simulated-device")
                .setSequence(System.currentTimeMillis())
                .setTimestamp(System.currentTimeMillis())
                .setTitle("Payload only");
        deliver(state);
    }

    /// Delivers a state that is a day old.
    ///
    /// Exercises `Continuity.setMaxAge(long)` and, more usefully, the listener that has to decide
    /// whether moving the user somewhere they were yesterday is a courtesy or an ambush.
    public static void continueSomethingStale() {
        AppState state = new AppState();
        state.setRoutes(currentRoutes())
                .setDeviceId("simulated-device")
                .setSequence(System.currentTimeMillis())
                .setTimestamp(System.currentTimeMillis() - 86400000L)
                .setTitle("From yesterday");
        deliver(state);
    }

    /// Reports that the synced store changed on another device, without changing a value.
    ///
    /// The notification carries no values on any platform, so an app that assumed it did -- and
    /// only re-reads the key it thinks changed -- reads a stale one here.
    public static void changeTheSyncedStore() {
        bridge().simulateStoreChange();
    }

    /// Makes the synced store report itself unsupported, which is what every non-Apple platform
    /// does.
    ///
    /// An app that put a required setting in there and never checked `isSupported()` loses it
    /// here, silently, exactly as it would on Android.
    public static void makeTheSyncedStoreUnsupported() {
        JavaSEPort.setSimulatedContinuity(new LocalContinuityBridge() {
            @Override
            public boolean isSyncedStoreSupported() {
                return false;
            }
        });
    }

    /// Makes continuation report itself unsupported, which is what every non-Apple platform does.
    public static void makeContinuationUnsupported() {
        JavaSEPort.setSimulatedContinuity(new LocalContinuityBridge() {
            @Override
            public boolean isContinuationSupported() {
                return false;
            }
        });
    }

    /// Restores the fully capable simulated platform.
    public static void makeEverythingSupported() {
        JavaSEPort.setSimulatedContinuity(new LocalContinuityBridge());
    }

    /// Takes a checkpoint now, so the menu items above have something to hand back.
    public static void checkpointNow() {
        Continuity.checkpoint();
    }

    /// Forgets the stored state, the way a logout does.
    public static void clearStoredState() {
        Continuity.clear();
    }

    /// Empties the simulated synced store.
    public static void clearTheSyncedStore() {
        String[] keys = SyncedStore.keys();
        for (int i = 0; i < keys.length; i++) {
            SyncedStore.remove(keys[i]);
        }
    }

    private static List<String> currentRoutes() {
        List<String> routes = new ArrayList<String>();
        List<com.codename1.router.NavigationEntry> stack =
                com.codename1.router.Navigation.getStack();
        for (int i = 0; i < stack.size(); i++) {
            routes.add(stack.get(i).getPath());
        }
        if (routes.isEmpty()) {
            routes.add("/");
        }
        return routes;
    }

    private static void deliver(AppState state) {
        // Through the bridge rather than through Continuity directly, so the item exercises the
        // same inbound path a device uses -- including the activity-type check, which is where a
        // mismatch between the build's declared type and the framework's would show up.
        bridge().simulateArrival(Continuity.getActivityType(), StateCodec.toMap(state));
    }
}
