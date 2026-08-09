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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which health background listeners each translation root is allowed to bind.
 *
 * <p>The listener scan walks the whole classes directory, so it answers "this app declares these
 * listeners". The generated factory names every one of them in a {@code new} expression -- a hard
 * reference the translator follows -- so handing the app-wide answer to both roots pulls each
 * target's listeners, and everything they reach, into the other target's binary. A watch that can
 * never be relaunched for the phone's listener has no business carrying it, and vice versa.</p>
 */
class IPhoneBuilderHealthListenerScopeTest {

    private static Map<String, String> listeners(String... binaryNames) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (String n : binaryNames) {
            out.put(n, n);
        }
        return out;
    }

    private static Set<String> reachable(String... internalNames) {
        Set<String> out = new HashSet<String>();
        for (String n : internalNames) {
            out.add(n);
        }
        return out;
    }

    @Test
    void eachRootBindsOnlyTheListenersItReaches() {
        IPhoneBuilder builder = new IPhoneBuilder();
        Map<String, String> all =
                listeners("com.acme.PhoneWatcher", "com.acme.WristWatcher");

        Map<String, String> phone = builder.healthListenersReachableFrom(all,
                reachable("com/acme/MyApp", "com/acme/PhoneWatcher"));
        assertEquals(1, phone.size(), "the phone must not carry the watch's listener");
        assertTrue(phone.containsKey("com.acme.PhoneWatcher"));

        Map<String, String> watch = builder.healthListenersReachableFrom(all,
                reachable("com/acme/WatchApp", "com/acme/WristWatcher"));
        assertEquals(1, watch.size(), "and the watch must not carry the phone's");
        assertTrue(watch.containsKey("com.acme.WristWatcher"));
    }

    /**
     * A nested listener is the common shape -- developers put it inside the class that subscribes.
     * Its binary name keeps the dollar, and only the package dots become slashes; translating the
     * dollar too would make it match nothing and silently drop a listener the root does need.
     */
    @Test
    void aNestedListenerMatchesItsInternalName() {
        IPhoneBuilder builder = new IPhoneBuilder();
        Map<String, String> all = new LinkedHashMap<String, String>();
        all.put("com.acme.Steps$Watcher", "com.acme.Steps.Watcher");

        Map<String, String> phone = builder.healthListenersReachableFrom(all,
                reachable("com/acme/Steps$Watcher"));
        assertEquals("com.acme.Steps.Watcher", phone.get("com.acme.Steps$Watcher"),
                "the source name has to survive the filter, it is what the factory calls new on");
    }

    /**
     * One translation is the ordinary build and must be untouched: with a single root the app-wide
     * answer and the per-root answer are the same question, and a null reachable set is how that
     * is said.
     */
    @Test
    void oneTranslationFiltersNothing() {
        IPhoneBuilder builder = new IPhoneBuilder();
        Map<String, String> all = listeners("com.acme.PhoneWatcher");
        assertSame(all, builder.healthListenersReachableFrom(all, null));
    }

    /**
     * The sensors package is BLE-only right up until a session is told to write through.
     *
     * <p>{@code SensorSessionOptions.setWriteToStore(true)} saves samples into HealthKit, and the
     * app-wide scanner already treats that as health write usage. The per-root walk excluded
     * everything under {@code health/sensors/} unconditionally, so the target actually doing the
     * writing was judged not to reach health at all and lost both its entitlement and its listener
     * bindings -- the same failure this attribution exists to prevent, from the other side.</p>
     */
    @Test
    void sensorWriteThroughMakesTheSensorsPackageReachHealth() {
        IPhoneBuilder builder = new IPhoneBuilder();
        Set<String> sensorsOnly = reachable("com/acme/MyApp",
                "com/codename1/health/sensors/SensorSession",
                "com/codename1/health/sensors/SensorSessionOptions");

        assertFalse(builder.reachesHealth(sensorsOnly),
                "a BLE-only root must not be entitled for HealthKit");

        builder.sensorWriteThrough = true;
        assertTrue(builder.reachesHealth(sensorsOnly),
                "but writing samples through to the store is HealthKit use");
    }

    /** Write-through elsewhere in the app does not make an unrelated root reach health. */
    @Test
    void writeThroughDoesNotEntitleARootThatTouchesNoSensor() {
        IPhoneBuilder builder = new IPhoneBuilder();
        builder.sensorWriteThrough = true;
        assertFalse(builder.reachesHealth(reachable("com/acme/WatchApp", "com/acme/Ui")));
    }

    /**
     * A phone-only native interface is not registered in the watch stub.
     *
     * <p>Each registration is a hard reference, and the generated stub it names holds one to the
     * native implementation -- so the app-wide list rooted every native implementation in the watch
     * translation, and a phone-only one had its Objective-C compiled for watchOS, where a UIKit
     * import is a build failure in code the watch never calls.</p>
     */
    @Test
    void theWatchStubRegistersOnlyTheNativesItReaches() {
        IPhoneBuilder builder = new IPhoneBuilder();
        String all = "        NativeLookup.register(com.acme.PhoneNative.class,"
                + " com.acme.PhoneNativeStub.class);\n"
                + "        NativeLookup.register(com.acme.WatchNative.class,"
                + " com.acme.WatchNativeStub.class);\n";

        String watch = builder.nativeRegistrationsReachableFrom(all,
                reachable("com/acme/WatchApp", "com/acme/WatchNative"));
        assertTrue(watch.contains("com.acme.WatchNative.class"), watch);
        assertFalse(watch.contains("com.acme.PhoneNative.class"),
                "the watch must not root the phone's native implementation: " + watch);
    }

    /** One translation registers everything, exactly as before. */
    @Test
    void oneTranslationRegistersEveryNative() {
        IPhoneBuilder builder = new IPhoneBuilder();
        String all = "        NativeLookup.register(com.acme.PhoneNative.class,"
                + " com.acme.PhoneNativeStub.class);\n";
        assertEquals(all, builder.nativeRegistrationsReachableFrom(all, null));
        assertEquals("", builder.nativeRegistrationsReachableFrom("", reachable("com/acme/X")));
    }

    /** A root that reaches no listener binds none, rather than falling back to all of them. */
    @Test
    void aRootThatReachesNoListenerBindsNone() {
        IPhoneBuilder builder = new IPhoneBuilder();
        Map<String, String> all = listeners("com.acme.PhoneWatcher");
        assertTrue(builder.healthListenersReachableFrom(all, reachable("com/acme/WatchApp"))
                .isEmpty());
        assertFalse(HealthListenerBindings.generate(
                builder.healthListenersReachableFrom(all, reachable("com/acme/WatchApp"))) != null,
                "and generates no factory at all, so nothing references HealthStore either");
    }
}
