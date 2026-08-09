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
        builder.sensorWriteThroughCallers.add("com/acme/MyApp");
        assertTrue(builder.reachesHealth(sensorsOnly),
                "but writing samples through to the store is HealthKit use");
    }

    /**
     * Write-through by the OTHER root does not entitle this one.
     *
     * <p>The app-wide flag said only "somebody writes through", so a watch lifecycle switching it
     * on made the phone reach health as soon as its own code touched any sensors class -- and
     * entitling the phone against a provisioning profile without HealthKit fails release signing.
     * The caller decides.</p>
     */
    @Test
    void writeThroughByAnotherRootDoesNotEntitleThisOne() {
        IPhoneBuilder builder = new IPhoneBuilder();
        builder.sensorWriteThrough = true;
        builder.sensorWriteThroughCallers.add("com/acme/WatchLifecycle");

        Set<String> phone = reachable("com/acme/MyApp",
                "com/codename1/health/sensors/SensorSession");
        assertFalse(builder.reachesHealth(phone),
                "the phone reaches the sensors package but not the class that writes through");

        Set<String> watch = reachable("com/acme/WatchApp", "com/acme/WatchLifecycle",
                "com/codename1/health/sensors/SensorSession");
        assertTrue(builder.reachesHealth(watch), "and the root that does write is entitled");
    }

    /** Write-through elsewhere in the app does not make an unrelated root reach health. */
    @Test
    void writeThroughDoesNotEntitleARootThatTouchesNoSensor() {
        IPhoneBuilder builder = new IPhoneBuilder();
        builder.sensorWriteThrough = true;
        builder.sensorWriteThroughCallers.add("com/acme/Writer");
        assertFalse(builder.reachesHealth(reachable("com/acme/WatchApp", "com/acme/Ui")));
    }

    /**
     * A screen reached only through a generated registry still counts.
     *
     * <p>Both stubs install the route dispatcher and the annotation bootstraps, and each names
     * every target it can dispatch to -- so ParparVM retains a HealthKit screen reached only by
     * route string, while a walk starting at the lifecycle cannot see it. That target then shipped
     * without its entitlement and had its authorization request refused.</p>
     */
    @Test
    void generatedRegistriesAreRootsOfTheWalk(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        IPhoneBuilder builder = new IPhoneBuilder();
        java.io.File classes = tmp.toFile();
        assertTrue(builder.installedRegistryRoots(classes).isEmpty(),
                "a project with no generated registry contributes no extra roots");

        write(classes, "com/codename1/router/generated/Routes.class");
        write(classes, "cn1app/MapperBootstrap.class");
        java.util.List<String> roots = builder.installedRegistryRoots(classes);
        assertTrue(roots.contains("com/codename1/router/generated/Routes"), roots.toString());
        assertTrue(roots.contains("cn1app/MapperBootstrap"), roots.toString());
        // The health binding factory must never be a root: it names every listener, so rooting
        // there would make both targets reach health whatever their lifecycle does.
        for (String root : roots) {
            assertFalse(root.contains("CN1HealthListenerBindings"), root);
        }
    }

    private static void write(java.io.File dir, String relative) throws Exception {
        java.io.File f = new java.io.File(dir, relative.replace('/', java.io.File.separatorChar));
        f.getParentFile().mkdirs();
        java.io.FileOutputStream out = new java.io.FileOutputStream(f);
        try {
            out.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
        } finally {
            out.close();
        }
    }

    /**
     * A phone-only app native is not registered in the watch stub.
     *
     * <p>Each registration is a hard reference and the stub it names holds one to the native
     * implementation, so the app-wide list rooted every native implementation in the watch
     * translation -- and a phone-only one had its Objective-C compiled for watchOS, where a UIKit
     * import is a build failure in code the watch never calls.</p>
     */
    @Test
    void theWatchStubDropsAppNativesItCannotReach() {
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

    /**
     * Framework natives are kept whether the walk reaches them or not.
     *
     * <p>com.codename1 plumbing is reached in ways a constant-pool walk cannot always see, and an
     * absent registration there does not fail the build -- it silently disables a service at
     * runtime. Dropping one is how the watch screenshot suite ended up running with no transport,
     * which is exactly the failure mode this exemption exists to make impossible.</p>
     */
    @Test
    void frameworkNativesAreNeverDropped() {
        IPhoneBuilder builder = new IPhoneBuilder();
        String all = "        NativeLookup.register(com.codename1.io.websocket.WebSocketNativeImpl"
                + ".class, com.codename1.io.websocket.WebSocketNativeImplStub.class);\n";
        assertEquals(all, builder.nativeRegistrationsReachableFrom(all,
                reachable("com/acme/WatchApp")));
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
