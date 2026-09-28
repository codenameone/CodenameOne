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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nearby transport's Bonjour service types have to JOIN the app's array,
 * not replace it.
 *
 * <p>Writing an {@code NSBonjourServices} key into {@code ios.plistInject}
 * looks equivalent and is not: the plist renderer emits the array built from
 * the {@code ios.NSBonjourServices} hint only when the injected fragment has
 * no key of its own, because a plist carrying the key twice keeps neither
 * value reliably. So the injection silently suppressed every service the app
 * had already declared -- most visibly the {@code _matter._tcp.} and
 * {@code _matterc._udp.} entries Matter commissioning accumulates, without
 * which iOS stops delivering the mDNS traffic commissioning depends on.</p>
 */
class NearbyBonjourMergeTest {

    private static BuildRequest request(String... kv) {
        BuildRequest r = new BuildRequest();
        r.setMainClass("MyApp");
        r.setPackageName("com.example");
        for (int i = 0; i < kv.length; i += 2) {
            r.putArgument(kv[i], kv[i + 1]);
        }
        return r;
    }

    /** Drives the private merge and hands back the resulting hint. */
    private static String merge(BuildRequest request, String... serviceTypes)
            throws Exception {
        return merge(request, false, serviceTypes);
    }

    private static String merge(BuildRequest request, boolean usesBonjour,
            String... serviceTypes) throws Exception {
        IPhoneBuilder b = new IPhoneBuilder();
        Method m = IPhoneBuilder.class.getDeclaredMethod(
                "mergeNearbyBonjourServices", BuildRequest.class, List.class,
                boolean.class);
        m.setAccessible(true);
        try {
            m.invoke(b, request, new ArrayList<String>(
                    Arrays.asList(serviceTypes)),
                    Boolean.valueOf(usesBonjour));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
        return request.getArg("ios.NSBonjourServices", "");
    }

    @Test
    void theTypesGoIntoTheHintNotIntoPlistInject() throws Exception {
        BuildRequest r = request();
        String hint = merge(r, "chat");
        assertTrue(hint.contains("_chat._tcp."), hint);
        assertTrue(hint.contains("_chat._udp."), hint);
        assertEquals("", r.getArg("ios.plistInject", ""),
                "the key must not be injected, or the generated array is"
                + " suppressed wholesale");
    }

    @Test
    void servicesTheAppAlreadyDeclaredSurvive() throws Exception {
        // Exactly what Matter commissioning leaves behind.
        BuildRequest r = request("ios.NSBonjourServices",
                "_matter._tcp.,_matterc._udp.");
        String hint = merge(r, "chat");
        assertTrue(hint.contains("_matter._tcp."), hint);
        assertTrue(hint.contains("_matterc._udp."), hint);
        assertTrue(hint.contains("_chat._tcp."), hint);
    }

    @Test
    void aTypeThatIsAlreadyThereIsNotAddedTwice() throws Exception {
        BuildRequest r = request("ios.NSBonjourServices",
                "_chat._tcp.,_chat._udp.");
        String hint = merge(r, "chat");
        assertEquals(2, hint.split(",").length, hint);
    }

    @Test
    void theTrailingDotIsNotWhatDecidesAMatch() throws Exception {
        // Both spellings appear in the wild and name the same service.
        BuildRequest r = request("ios.NSBonjourServices", "_chat._tcp");
        String hint = merge(r, "chat");
        assertEquals(1, countOccurrences(hint, "_chat._tcp"), hint);
    }

    @Test
    void aProjectThatOwnsTheKeyIsToldWhatToAddRatherThanOverwritten()
            throws Exception {
        BuildRequest r = request("ios.plistInject",
                "<key>NSBonjourServices</key><array>"
                + "<string>_matter._tcp.</string></array>");
        BuildException thrown = assertThrows(BuildException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() throws Throwable {
                        merge(r, "chat");
                    }
                });
        assertTrue(thrown.getMessage().contains("_chat._tcp."),
                thrown.getMessage());
        assertTrue(thrown.getMessage().contains("ios.plistInject"),
                thrown.getMessage());
    }

    @Test
    void aProjectThatOwnsTheKeyAndListedTheTypesIsLeftAlone() throws Exception {
        BuildRequest r = request("ios.plistInject",
                "<key>NSBonjourServices</key><array>"
                + "<string>_chat._tcp.</string>"
                + "<string>_chat._udp.</string></array>");
        merge(r, "chat");
    }

    @Test
    void anAppThatAlsoUsesBonjourKeepsItsHttpDefault() {
        // The bonjour block seeds _http._tcp. only when the hint is unset,
        // and this merge creates the hint first -- so without seeding it here
        // an app using both APIs silently lost the default it would have had.
        BuildRequest r = request();
        String hint = assertDoesNotThrow(new org.junit.jupiter.api.function
                .ThrowingSupplier<String>() {
            @Override
            public String get() throws Throwable {
                return merge(r, true, "chat");
            }
        });
        assertTrue(hint.contains("_http._tcp."), hint);
        assertTrue(hint.contains("_chat._tcp."), hint);
    }

    @Test
    void anAppThatNamedItsOwnTypesIsNotGivenTheHttpDefault() {
        // Same as today: a project that set the hint owns it.
        BuildRequest r = request("ios.NSBonjourServices", "_myapp._tcp.");
        String hint = assertDoesNotThrow(new org.junit.jupiter.api.function
                .ThrowingSupplier<String>() {
            @Override
            public String get() throws Throwable {
                return merge(r, true, "chat");
            }
        });
        assertFalse(hint.contains("_http._tcp."), hint);
        assertTrue(hint.contains("_myapp._tcp."), hint);
    }

    @Test
    void anAppThatDoesNotUseBonjourGetsOnlyItsNearbyTypes() {
        BuildRequest r = request();
        String hint = assertDoesNotThrow(new org.junit.jupiter.api.function
                .ThrowingSupplier<String>() {
            @Override
            public String get() throws Throwable {
                return merge(r, false, "chat");
            }
        });
        assertFalse(hint.contains("_http._tcp."), hint);
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            n++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return n;
    }
}
