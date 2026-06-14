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
package com.codename1.social;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the non-network surface of {@link MicrosoftConnect}: the
 * singleton accessor, tenant selection (including the null-coalescing default),
 * the {@code isNativeLoginSupported} contract, and the token validator. The
 * singleton's tenant is reset after each test so methods stay order-independent.
 */
class MicrosoftConnectTest {

    @AfterEach
    void restoreDefaultTenant() {
        MicrosoftConnect.getInstance().withTenant(null);
    }

    @Test
    void getInstanceReturnsAStableSingleton() {
        assertSame(MicrosoftConnect.getInstance(), MicrosoftConnect.getInstance());
    }

    @Test
    void commonTenantConstant() {
        assertEquals("common", MicrosoftConnect.COMMON_TENANT);
    }

    @Test
    void withTenantSetsValueAndChains() {
        MicrosoftConnect c = MicrosoftConnect.getInstance();
        assertSame(c, c.withTenant("contoso.onmicrosoft.com"));
        assertEquals("contoso.onmicrosoft.com", c.getTenant());
    }

    @Test
    void withTenantNullRestoresCommon() {
        MicrosoftConnect c = MicrosoftConnect.getInstance();
        c.withTenant("organizations");
        c.withTenant(null);
        assertEquals(MicrosoftConnect.COMMON_TENANT, c.getTenant());
    }

    @Test
    void nativeLoginIsNotSupported() {
        assertFalse(MicrosoftConnect.getInstance().isNativeLoginSupported());
    }

    @Test
    void validateTokenAcceptsNonEmptyAndRejectsEmpty() {
        MicrosoftConnect c = MicrosoftConnect.getInstance();
        assertTrue(c.validateToken("abc"));
        assertFalse(c.validateToken(""));
        assertFalse(c.validateToken(null));
    }
}
