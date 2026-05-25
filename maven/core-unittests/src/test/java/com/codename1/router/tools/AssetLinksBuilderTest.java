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
package com.codename1.router.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssetLinksBuilderTest {

    @Test
    void singleAppEntry() {
        String json = new AssetLinksBuilder()
                .addApp("com.example.app", "AB:CD:EF")
                .build();
        assertTrue(json.contains("\"com.example.app\""));
        assertTrue(json.contains("\"AB:CD:EF\""));
        assertTrue(json.contains("delegate_permission/common.handle_all_urls"));
    }

    @Test
    void additionalFingerprintAttachesToLastApp() {
        String json = new AssetLinksBuilder()
                .addApp("com.example.app", "AAA")
                .addFingerprint("BBB")
                .build();
        // both fingerprints should appear in the same array
        int aaa = json.indexOf("\"AAA\"");
        int bbb = json.indexOf("\"BBB\"");
        assertTrue(aaa > 0 && bbb > 0 && bbb > aaa);
    }

    @Test
    void addAppRequiresFingerprint() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new AssetLinksBuilder().addApp("p", ""); }
        });
    }

    @Test
    void addFingerprintBeforeAppThrows() {
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new AssetLinksBuilder().addFingerprint("AAA"); }
        });
    }
}
