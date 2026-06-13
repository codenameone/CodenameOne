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
package com.codename1.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for {@link WalletPassEntry}: both constructors and the
 * fluent setter surface used to publish cards to Apple Wallet provisioning.
 */
class WalletPassEntryTest {

    @Test
    void blankConstructorLeavesEverythingNull() {
        WalletPassEntry e = new WalletPassEntry();
        assertNull(e.getIdentifier());
        assertNull(e.getTitle());
        assertNull(e.getArtPng());
        assertNull(e.getCardholderName());
        assertNull(e.getPrimaryAccountSuffix());
        assertNull(e.getPaymentNetwork());
        assertNull(e.getLocalizedDescription());
    }

    @Test
    void requiredFieldsConstructorPopulatesThem() {
        byte[] art = {1, 2, 3};
        WalletPassEntry e = new WalletPassEntry("acct-1", "My Card", art);
        assertEquals("acct-1", e.getIdentifier());
        assertEquals("My Card", e.getTitle());
        assertArrayEquals(art, e.getArtPng());
    }

    @Test
    void fluentSettersChainAndRoundTrip() {
        byte[] art = {9, 8, 7};
        WalletPassEntry e = new WalletPassEntry();
        assertSame(e, e.identifier("acct-2"));
        assertSame(e, e.title("Debit"));
        assertSame(e, e.cardholderName("Jane Doe"));
        assertSame(e, e.primaryAccountSuffix("1234"));
        assertSame(e, e.paymentNetwork("Visa"));
        assertSame(e, e.localizedDescription("My Bank Debit Card"));
        assertSame(e, e.artPng(art));

        assertEquals("acct-2", e.getIdentifier());
        assertEquals("Debit", e.getTitle());
        assertEquals("Jane Doe", e.getCardholderName());
        assertEquals("1234", e.getPrimaryAccountSuffix());
        assertEquals("Visa", e.getPaymentNetwork());
        assertEquals("My Bank Debit Card", e.getLocalizedDescription());
        assertArrayEquals(art, e.getArtPng());
    }
}
