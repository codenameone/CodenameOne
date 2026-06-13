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

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link BonjourBrowser} against the no-op fallback
 * {@link BonjourPlatform}: the {@code browse} factory (which reports the
 * platform as unsupported to the listener), {@code isSupported}, the type
 * accessor, and the idempotent {@code stop}.
 */
class BonjourBrowserTest extends UITestBase {

    @Test
    void isSupportedIsFalseOnFallbackPlatform() {
        assertFalse(BonjourBrowser.isSupported());
    }

    @Test
    void browseReturnsHandleAndReportsUnsupportedToListener() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        BonjourBrowser browser = BonjourBrowser.browse("_http._tcp.", new BonjourServiceListener() {
            public void onServiceResolved(BonjourService service) {
            }

            public void onServiceLost(BonjourService service) {
            }

            public void onBrowseError(Throwable error) {
                err.set(error);
            }
        });
        assertNotNull(browser);
        assertEquals("_http._tcp.", browser.getType());
        assertTrue(err.get() instanceof UnsupportedOperationException);
    }

    @Test
    void stopIsIdempotent() {
        BonjourBrowser browser = BonjourBrowser.browse("_ipp._tcp.", null);
        // Two stops on the fallback platform must not throw.
        browser.stop();
        browser.stop();
    }
}
