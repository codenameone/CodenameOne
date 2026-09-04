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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.continuity.AppState;
import com.codename1.continuity.Continuity;
import com.codename1.continuity.StateCodec;
import com.codename1.continuity.StateProvider;
import com.codename1.continuity.sync.SyncedStore;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Saves and restores application state on the device VM, so CI runs what the build generates.
///
/// Declaring this is part of the coverage. Without a reference to `com.codename1.continuity`
/// anywhere in the project the iOS builder leaves `CN1_USE_CONTINUITY` commented out, so the
/// `NSUserActivity` natives and the continuity branch in the app delegate are never compiled,
/// and this app's activity type never reaches `NSUserActivityTypes` for the plist to be checked.
/// Every mistake in that half -- an Apple API misused, a plist key Xcode will not take, a native
/// symbol whose mangled name does not match the Java declaration -- is invisible until something
/// references the package.
///
/// The rest is the half that has no platform behind it and therefore has to behave identically
/// everywhere: the codec both wire formats share, the payload rule that makes them possible, the
/// checkpoint, and the dedup that stops one state being acted on twice. Assertion-only test, no
/// screenshot.
public class ContinuityStateTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            // Support probes must never throw, whatever they answer.
            boolean continuation = Continuity.isContinuationSupported();
            boolean synced = SyncedStore.isSupported();
            System.out.println("CN1SS:INFO:test=ContinuityStateTest continuation=" + continuation
                    + " syncedStore=" + synced
                    + " platform=" + Display.getInstance().getPlatformName());

            // The activity type is derived from the package name on this side and written into
            // NSUserActivityTypes by the build on the other. If the two ever disagree, iOS
            // silently refuses to deliver anything -- so the shape is asserted where it is
            // computed.
            String activityType = Continuity.getActivityType();
            assertBool(activityType != null && activityType.endsWith(".continuity"),
                    "activity type ends with .continuity");

            final Map<String, Object> restored = new HashMap<String, Object>();
            Continuity.setStateProvider(new StateProvider() {
                public Map<String, Object> saveState() {
                    Map<String, Object> state = new HashMap<String, Object>();
                    state.put("draft", "cn1ss draft");
                    state.put("count", Integer.valueOf(3));
                    return state;
                }

                public void restoreState(Map<String, Object> payload) {
                    restored.putAll(payload);
                }
            });
            assertBool(Continuity.isEnabled(), "installing a provider enables the framework");

            Continuity.setTitle("cn1ss continuity");
            Continuity.checkpoint();

            AppState stored = Continuity.getRestorableState();
            assertBool(stored != null, "a checkpoint leaves a restorable state");
            assertEqual("cn1ss draft", stored.getPayload().get("draft"), "stored payload");
            assertBool(stored.getSequence() > 0, "a stored state carries a sequence");
            assertBool(stored.getDeviceId() != null && stored.getDeviceId().length() > 0,
                    "a stored state names the device that produced it");

            // Restoring an app with no routes hands the payload back and shows nothing, which is
            // what lets "restore, or else begin" work. Answering true here would make such an app
            // skip its own first screen.
            assertBool(!Continuity.restore(), "a routeless restore shows no form");
            assertEqual("cn1ss draft", restored.get("draft"), "the payload reached the provider");

            // Both wire formats, on the device VM. The JSON one crosses the network to another
            // device and the map one is handed to the operating system, and a millisecond
            // timestamp is past the range a JSON number represents exactly -- which is why they
            // are encoded as strings and why that is asserted rather than assumed.
            AppState wire = new AppState()
                    .setRoutes(routes())
                    .setPayload(payload())
                    .setDeviceId("cn1ss-device")
                    .setSequence(4242L)
                    .setTimestamp(1763512345678L);
            AppState viaJson = StateCodec.fromJson(StateCodec.toJson(wire));
            assertBool(viaJson != null, "a state survives the JSON form");
            assertEqual(1763512345678L, viaJson.getTimestamp(), "timestamp survives JSON exactly");
            assertEqual(4242L, viaJson.getSequence(), "sequence survives JSON exactly");
            assertEqual(2, viaJson.getRoutes().size(), "routes survive JSON");
            AppState viaMap = StateCodec.fromMap(StateCodec.toMap(wire));
            assertBool(viaMap != null, "a state survives the map form");
            assertEqual("cn1ss", viaMap.getPayload().get("name"), "payload survives the map form");

            // The payload rule is enforced where the application can act on it, on every port.
            boolean refused = false;
            try {
                Map<String, Object> bad = new HashMap<String, Object>();
                bad.put("when", new java.util.Date());
                new AppState().setPayload(bad);
            } catch (IllegalArgumentException expected) {
                refused = true;
            }
            assertBool(refused, "an unrepresentable payload value is refused");

            // The synced store answers honestly on the ports that have none, and every call is
            // safe there. This is the ordinary case for Android, the desktop and the browser.
            //
            // A key of THIS RUN's own, removed first and cleaned up in a finally. A fixed key left
            // behind by a run that was interrupted between the write and the removal made the
            // absent-value assertion fail on every later run, permanently -- and on iOS the value
            // can also arrive from another device, which no amount of local cleanup prevents.
            String key = "cn1ss.sortOrder." + System.currentTimeMillis();
            try {
                SyncedStore.remove(key);
                assertEqual("byName", SyncedStore.get(key, "byName"),
                        "an absent synced value answers with the default");

                // NOT asserted equal to isSupported(). They answer different questions on iOS by
                // design: isSupported() reports whether the entitlement probe has established a
                // store that follows the user, while put() writes to the local persistent store
                // and succeeds even when that probe has not -- and a store at its quota refuses a
                // write while remaining perfectly supported. Tying them together made this fail
                // for a device that was merely offline, or whose store was full, with both APIs
                // keeping their documented contracts.
                boolean wrote = SyncedStore.put(key, "byDate");
                if (wrote) {
                    assertEqual("byDate", SyncedStore.get(key, "byName"),
                            "a value the store accepted reads back");
                } else {
                    assertEqual("byName", SyncedStore.get(key, "byName"),
                            "a write the store refused left nothing behind");
                }
                assertBool(SyncedStore.keys() != null, "the key list is never null");
            } finally {
                SyncedStore.remove(key);
            }

            // Clearing must be safe everywhere, including twice and including when the platform
            // never advertised anything.
            Continuity.clear();
            Continuity.clear();
            assertBool(Continuity.getRestorableState() == null,
                    "clearing forgets the stored state");

            // The device runner waits for this before moving on. A test that returns true
            // without it never reports DONE, and the suite fails the whole port with
            // "timeout waiting for DONE stage=created" rather than naming the test.
            done();
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            done();
            return false;
        }
    }

    private static List<String> routes() {
        List<String> paths = new ArrayList<String>();
        paths.add("/home");
        paths.add("/users/42");
        return paths;
    }

    private static Map<String, Object> payload() {
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("street", "Sesame");
        List<Object> tags = new ArrayList<Object>();
        tags.add("a");
        tags.add(Integer.valueOf(2));
        tags.add(Boolean.TRUE);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("name", "cn1ss");
        payload.put("address", nested);
        payload.put("tags", tags);
        return payload;
    }
}
