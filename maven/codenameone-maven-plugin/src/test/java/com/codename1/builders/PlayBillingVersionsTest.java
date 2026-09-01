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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Play Billing version policy. The numbers here are measurements, not
 * choices -- the floor is where the port's source stops compiling, and the
 * minSdk mapping is what the published AARs declare -- so a change that moves
 * one of them without a new measurement behind it is a regression.
 */
public class PlayBillingVersionsTest {
    /**
     * The floor is the version the port's source actually compiles against,
     * not a preference. Compiling
     * {@code Ports/Android/src/com/codename1/impl/android/BillingSupport.java}
     * against each published release puts the boundary exactly here: 7.1.1
     * fails, 8.0.0 and 9.1.0 are clean.
     */
    @Test
    public void refusesEverythingBelowTheProductDetailsApi() {
        check(PlayBillingVersions.refusalFor("4.0.0") != null, "4.0.0 is refused");
        check(PlayBillingVersions.refusalFor("6.2.1") != null, "6.2.1 is refused");
        check(PlayBillingVersions.refusalFor("7.1.1") != null, "7.1.1 is refused");
        check(PlayBillingVersions.refusalFor("8.0.0") == null, "8.0.0 is accepted");
        check(PlayBillingVersions.refusalFor("9.1.0") == null, "9.1.0 is accepted");
    }

    /**
     * The refusal has to name the hint, the version and the way out, because
     * the alternative it replaces is two dozen "cannot find symbol" errors
     * against a file the developer never wrote.
     */
    @Test
    public void theRefusalSaysWhatToDo() {
        String refusal = PlayBillingVersions.refusalFor("4.0.0");
        check(refusal.contains("android.billingclient.version"), "it names the hint");
        check(refusal.contains("4.0.0"), "it names the version that was set");
        check(refusal.contains(PlayBillingVersions.MINIMUM_SUPPORTED), "it names the floor");
        check(refusal.contains("SkuDetails"), "it says why");
    }

    /**
     * A version this cannot parse is accepted rather than refused. A project
     * may legitimately hold one, and failing a build over the shape of a
     * string rather than over anything wrong with it is the worse error.
     */
    @Test
    public void anUnreadableVersionIsNotRefused() {
        check(PlayBillingVersions.refusalFor(null) == null, "null is not refused");
        check(PlayBillingVersions.refusalFor("") == null, "empty is not refused");
        check(PlayBillingVersions.refusalFor("$billingVersion") == null,
                "a Gradle variable is not refused");
    }

    /**
     * Read from the published AAR manifests, and the reason the mapping is not
     * "the 8 line needs 21": 8.0.0 alone declares 21, and every release after
     * it declares 23.
     */
    @Test
    public void theMinSdkFloorFollowsTheAar() {
        check("21".equals(PlayBillingVersions.minimumSdk("8.0.0")), "8.0.0 needs 21");
        check("23".equals(PlayBillingVersions.minimumSdk("8.1.0")), "8.1.0 needs 23");
        check("23".equals(PlayBillingVersions.minimumSdk("8.2.1")), "8.2.1 needs 23");
        check("23".equals(PlayBillingVersions.minimumSdk("8.3.0")), "8.3.0 needs 23");
        check("23".equals(PlayBillingVersions.minimumSdk("9.0.0")), "9.0.0 needs 23");
        check("23".equals(PlayBillingVersions.minimumSdk("9.1.0")), "9.1.0 needs 23");
    }

    /**
     * Unknown guesses high. Guessing low turns into a manifest merge failure;
     * guessing high only narrows the device range of an app already on a
     * release that requires API 23.
     */
    @Test
    public void anUnknownVersionTakesTheHighestKnownFloor() {
        check("23".equals(PlayBillingVersions.minimumSdk("$billingVersion")),
                "an unreadable version takes the high floor");
        check("23".equals(PlayBillingVersions.minimumSdk(null)),
                "a null version takes the high floor");
        check("23".equals(PlayBillingVersions.minimumSdk("10.0.0")),
                "a future version takes the high floor");
    }

    /**
     * The default is the lowest version Play still accepts, which is also the
     * only one at that level that does not force minSdk 23 on every
     * in-app-purchase app.
     */
    @Test
    public void theDefaultIsTheGentlestAcceptableVersion() {
        check("8.0.0".equals(PlayBillingVersions.DEFAULT_VERSION), "the default is 8.0.0");
        check(PlayBillingVersions.refusalFor(PlayBillingVersions.DEFAULT_VERSION) == null,
                "the default is not itself refused");
        check("21".equals(PlayBillingVersions.minimumSdk(PlayBillingVersions.DEFAULT_VERSION)),
                "the default does not force minSdk 23");
    }

    /**
     * The gate that was missing, and whose absence is why this rotted in place
     * for years.
     *
     * <p>BillingSupport ships as source and is compiled inside the generated
     * app, so nothing in this tree compiles it -- the port module now excludes
     * it explicitly, and did not compile it against anything current before
     * that either. A reference to the removed SKU API therefore produces no
     * failure here at all; it produces a failed customer build, on the cloud
     * builders, in an app the developer cannot edit.</p>
     *
     * <p>So the removed API is named here instead. This is weaker than a
     * compile and does not pretend otherwise: it cannot see a future
     * incompatibility, only a return to the one that was just removed. That is
     * still the difference between catching a regression in CI and hearing
     * about it from a support conversation.</p>
     */
    @Test
    public void theBillingImplementationUsesNoRemovedSkuApi() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "../../Ports/Android/src/com/codename1/impl/android/BillingSupport.java").toPath());
        // Comments only, stripped: naming the removed API while explaining what
        // replaced it is exactly the comment this file should carry, and a gate that
        // forbade it would be answered by deleting the explanation.
        String src = stripComments(new String(bytes, "UTF-8"));
        // Removed across billing 8 and 9. Each was in the file before the migration,
        // so this list is what was actually there, not a guess at what might be.
        String[] removed = {
            "SkuDetails",
            "SkuDetailsParams",
            "SkuDetailsResponseListener",
            "querySkuDetailsAsync",
            "BillingClient.SkuType",
            "setSkuDetails",
            ".enablePendingPurchases()",
        };
        for (String symbol : removed) {
            check(!src.contains(symbol),
                    "BillingSupport still references the removed Play Billing API " + symbol);
        }
        // And the replacement really is in use, so the check above cannot pass by the
        // file having been emptied or renamed out from under it.
        check(src.contains("queryProductDetailsAsync"), "it queries product details");
        check(src.contains("setProductDetailsParamsList"), "it launches the modern flow");
        check(src.contains("BillingClient.ProductType"), "it uses ProductType");
    }

    /**
     * BillingSupport is excluded from compilation in three places that each
     * claim to mirror the other two -- the maven compiler plugin, the ant
     * javac and the NetBeans project. Missing one is not a local mistake:
     * the BuildDaemon CI builds this port with ant, so the maven exclusion
     * alone left that build compiling the ProductDetails source against the
     * billing 4.0.0 jar, which is how this was found.
     */
    @Test
    public void everyBuildThatCompilesThePortExcludesBillingSupport() throws Exception {
        String[] mirrors = {
            "../android/pom.xml",
            "../../Ports/Android/build.xml",
            "../../Ports/Android/nbproject/project.properties",
        };
        for (String mirror : mirrors) {
            byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(mirror).toPath());
            String src = new String(bytes, "UTF-8");
            check(src.contains("com/codename1/impl/android/BillingSupport.java"),
                    mirror + " still compiles BillingSupport; it needs a billing "
                    + "dependency this tree does not carry");
        }
    }

    /** Java source with block and line comments removed. */
    private static String stripComments(String src) {
        StringBuilder out = new StringBuilder();
        boolean inBlock = false;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (inBlock) {
                if (c == '*' && i + 1 < src.length() && src.charAt(i + 1) == '/') {
                    inBlock = false;
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < src.length()) {
                char next = src.charAt(i + 1);
                if (next == '*') {
                    inBlock = true;
                    i++;
                    continue;
                }
                if (next == '/') {
                    while (i < src.length() && src.charAt(i) != '\n') {
                        i++;
                    }
                    out.append('\n');
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static void check(boolean condition, String message) {
        assertTrue(condition, message);
    }
}
