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
package com.codename1.io.bonjour;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link BonjourPublisher} against the no-op fallback
 * {@link BonjourPlatform} (the test implementation provides no native mDNS).
 * Verifies the {@code publish} factory captures the descriptor, that a null
 * txt map is tolerated, and that {@code unpublish} is idempotent.
 */
class BonjourPublisherTest extends UITestBase {

    @Test
    void publishCapturesNameTypeAndPort() {
        Map<String, String> txt = new HashMap<String, String>();
        txt.put("path", "/api");
        BonjourPublisher pub = BonjourPublisher.publish("MyServer", "_http._tcp.", 8080, txt);
        assertEquals("MyServer", pub.getName());
        assertEquals("_http._tcp.", pub.getType());
        assertEquals(8080, pub.getPort());
    }

    @Test
    void publishToleratesNullTxtMap() {
        BonjourPublisher pub = BonjourPublisher.publish("S", "_ipp._tcp.", 631, null);
        assertEquals("S", pub.getName());
        assertEquals(631, pub.getPort());
    }

    @Test
    void unpublishIsIdempotent() {
        BonjourPublisher pub = BonjourPublisher.publish("S", "_http._tcp.", 80, null);
        // Two calls must not throw on the fallback platform.
        pub.unpublish();
        pub.unpublish();
    }
}
