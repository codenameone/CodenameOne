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

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What `android.billing.store` puts into a generated project.
///
/// Every assertion is about a failure nothing in a build reports: an Amazon
/// build that Amazon's client cannot answer, or one that still carries Play's
/// billing and is turned away at Appstore review.
class AmazonBillingFragmentsTest {

    @Test
    public void playIsTheDefaultAndAmazonIsMatchedWithoutCase() {
        assertFalse(AmazonBillingFragments.isAmazon(null));
        assertFalse(AmazonBillingFragments.isAmazon(""));
        assertFalse(AmazonBillingFragments.isAmazon("play"));
        assertTrue(AmazonBillingFragments.isAmazon("amazon"));
        // An upper case I is the letter a locale-sensitive fold gets wrong.
        assertTrue(AmazonBillingFragments.isAmazon(" AMAZON "));
    }

    @Test
    public void anUnknownStoreIsRefusedRatherThanBuiltForPlay() {
        assertNull(AmazonBillingFragments.refusalFor(null));
        assertNull(AmazonBillingFragments.refusalFor("play"));
        assertNull(AmazonBillingFragments.refusalFor("Amazon"));
        String refusal = AmazonBillingFragments.refusalFor("amazn");
        assertNotNull(refusal);
        assertTrue(refusal.contains("android.billing.store") && refusal.contains("amazn"),
                "the sentence names the hint and the value");
    }

    @Test
    public void anAmazonBuildDependsOnTheAmazonSdkAndNotOnPlayBilling() {
        String out = AmazonBillingFragments.dependency(true, "8.0.0");
        assertEquals(" implementation 'com.amazon.device:amazon-appstore-sdk:"
                + AmazonBillingFragments.SDK_VERSION + "'\n", out);
        assertFalse(out.contains("billingclient"),
                "Play Billing has no place in an Appstore build");
    }

    @Test
    public void anAmazonBuildDoesNotRequestThePlayBillingPermission() {
        assertEquals("", AmazonBillingFragments.permissions(true));
    }

    @Test
    public void anAmazonBuildDeclaresTheReceiverAmazonAnswersThrough() {
        String out = AmazonBillingFragments.receiver(true, "");
        assertTrue(out.contains("<receiver android:name=\"com.amazon.device.iap.ResponseReceiver\""));
        // Mandatory from API 31 for a component with an intent filter, and it
        // has to be true: the sender is another application.
        assertTrue(out.contains("android:exported=\"true\""));
        // Without the permission any application could forge an answer.
        assertTrue(out.contains(
                "android:permission=\"com.amazon.inapp.purchasing.Permission.NOTIFY\""));
        assertTrue(out.contains("<action android:name=\"com.amazon.inapp.purchasing.NOTIFY\" />"));
    }

    @Test
    public void aProjectThatDeclaresTheReceiverItselfIsNotGivenASecond() {
        assertEquals("", AmazonBillingFragments.receiver(true,
                "<receiver android:name=\"com.amazon.device.iap.ResponseReceiver\"/>"));
    }

    @Test
    public void aPlayBuildIsWhatItWasBeforeTheHintExisted() {
        assertEquals(" implementation 'com.android.billingclient:billing:8.0.0'\n",
                AmazonBillingFragments.dependency(false, "8.0.0"));
        assertEquals("    <uses-permission android:name=\"com.android.vending.BILLING\""
                + " android:required=\"false\" />\n", AmazonBillingFragments.permissions(false));
        assertEquals("", AmazonBillingFragments.receiver(false, ""));
        assertEquals("com.codename1.impl.android.BillingSupport",
                AmazonBillingFragments.billingSupportClass(false));
    }

    @Test
    public void eachBuildCompilesOnlyTheImplementationItHasALibraryFor() {
        assertEquals("com.codename1.impl.android.AmazonBillingSupport",
                AmazonBillingFragments.billingSupportClass(true));
        assertEquals(Arrays.asList("BillingSupport.java"),
                Arrays.asList(AmazonBillingFragments.sourcesToRemove(true, true)));
        assertEquals(Arrays.asList("AmazonBillingSupport.java"),
                Arrays.asList(AmazonBillingFragments.sourcesToRemove(true, false)));
        // No purchase API, no billing library of either kind on the class path.
        assertEquals(Arrays.asList("BillingSupport.java", "AmazonBillingSupport.java"),
                Arrays.asList(AmazonBillingFragments.sourcesToRemove(false, false)));
    }
}
