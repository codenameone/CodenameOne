/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptBuilderNativeInterfaceTest {

    @Test
    void isSupportedAnswersFalseInsteadOfThrowingWhenNoJsImplementationIsBound() {
        // An Impl is generated and registered for EVERY native interface in the app, so
        // NativeLookup.create() never returns null on the JavaScript port. That makes
        // isSupported() the only signal the developer has, and an app with no JS stub for
        // the interface used to get the bridge's "No native interface implementation
        // registered" rejection thrown straight out of the standard
        // "create(X.class) != null && x.isSupported()" guard (issue #5512).
        String source = JavaScriptBuilder.nativeInterfaceImplSource(SampleJsNative.class);

        assertTrue(source.contains("public boolean isSupported() {"),
                "Generated impl should implement isSupported(). source=" + source);
        assertTrue(source.contains("callBoolean(__NI, \"isSupported_\", new Object[0])"),
                "isSupported() should still ask the host bridge first. source=" + source);
        assertTrue(source.contains("} catch (Throwable __t) {") && source.contains("return false;"),
                "isSupported() must degrade to false rather than propagate the bridge failure. source="
                        + source);
    }

    @Test
    void everyOtherMethodStillPropagatesBridgeFailures() {
        // Swallowing failures anywhere else would turn a genuinely unimplemented native
        // into a silent no-op, so the guard is scoped to isSupported() alone.
        String source = JavaScriptBuilder.nativeInterfaceImplSource(SampleJsNative.class);

        assertTrue(source.contains("return com.codename1.impl.platform.js.NativeInterfaceBridge"
                        + ".callString(__NI, \"greet__java_lang_String\", new Object[]{ p0 });"),
                "greet(String) should delegate straight to the bridge. source=" + source);
        assertTrue(source.contains("com.codename1.impl.platform.js.NativeInterfaceBridge"
                        + ".callVoid(__NI, \"ping__int\", new Object[]{ Integer.valueOf(p0) });"),
                "ping(int) should delegate straight to the bridge. source=" + source);
        assertEquals(1, countOccurrences(source, "catch (Throwable"),
                "Only isSupported() may swallow bridge failures. source=" + source);
    }

    @Test
    void bindsTheInterfaceUnderItsUnderscoredRegistryKey() {
        String source = JavaScriptBuilder.nativeInterfaceImplSource(SampleJsNative.class);

        assertTrue(source.contains("private static final String __NI = \""
                        + SampleJsNative.class.getName().replace('.', '_') + "\";"),
                "Impl should bind to the cn1_native_interfaces registry key. source=" + source);
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        for (int idx = source.indexOf(needle); idx >= 0; idx = source.indexOf(needle, idx + needle.length())) {
            count++;
        }
        return count;
    }
}

/** Stand-in for an app-supplied native interface; top level so the generated source is valid Java. */
interface SampleJsNative extends com.codename1.system.NativeInterface {
    String greet(String name);

    void ping(int count);
}
