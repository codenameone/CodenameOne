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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The watch lifecycle entry class is resolved by its original fully-qualified name at run time, so a
 * distinct watchMain must be kept from renaming; a watch that shares the phone main class needs no
 * extra keep (the main class is already kept).
 */
class IPhoneBuilderWatchKeepTest {

    private static BuildRequest request(String main, String pkg, String... kv) {
        BuildRequest r = new BuildRequest();
        if (main != null) {
            r.setMainClass(main);
        }
        if (pkg != null) {
            r.setPackageName(pkg);
        }
        for (int i = 0; i < kv.length; i += 2) {
            r.putArgument(kv[i], kv[i + 1]);
        }
        return r;
    }

    @Test
    void distinctFullyQualifiedWatchMainIsKept() {
        BuildRequest r = request("MyApp", "com.example",
                "watchNative.enabled", "true", "watchMain", "com.example.MyWatchApp");
        List<String> keep = IPhoneBuilder.watchEntryKeepClasses(r);
        assertEquals(1, keep.size());
        assertEquals("com.example.MyWatchApp", keep.get(0));
    }

    @Test
    void simpleWatchMainIsQualifiedWithThePackage() {
        BuildRequest r = request("MyApp", "com.example",
                "watchMain", "MyWatchApp");
        List<String> keep = IPhoneBuilder.watchEntryKeepClasses(r);
        assertEquals(1, keep.size());
        assertEquals("com.example.MyWatchApp", keep.get(0));
        assertTrue(IPhoneBuilder.watchTargetEnabled(r), "a watchMain entry auto-enables the watch slice");
    }

    @Test
    void watchSharingTheMainClassNeedsNoExtraKeep() {
        // watchMain equals the phone main class (already kept as cn1.mainClass).
        BuildRequest r = request("MyApp", "com.example",
                "watchNative.enabled", "true", "watchMain", "MyApp");
        assertTrue(IPhoneBuilder.watchEntryKeepClasses(r).isEmpty());
    }

    @Test
    void noWatchTargetMeansNoKeep() {
        BuildRequest r = request("MyApp", "com.example");
        assertTrue(IPhoneBuilder.watchEntryKeepClasses(r).isEmpty());
        assertTrue(!IPhoneBuilder.watchTargetEnabled(r));
    }
}
