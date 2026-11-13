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

/**
 * What the {@code android.billing.store} build hint changes in a generated
 * Android project, as pure text so it can be unit tested.
 *
 * <p>An application that uses the purchase API is built for exactly one store.
 * The two cannot share a build: Google Play rejects nothing about the Amazon
 * SDK, but the Amazon Appstore's review does reject an application that
 * requests {@code com.android.vending.BILLING}, and the port's two billing
 * implementations each compile against a library the other build does not
 * carry. So every method here answers for one store and says nothing of the
 * other.</p>
 *
 * <p>The Amazon SDK is a plain jar with no manifest of its own, which is why
 * the receiver is written here: nothing merges it in, and without it Amazon's
 * client has nowhere to send an answer -- every request then goes unanswered
 * on a build that looks complete.</p>
 */
public final class AmazonBillingFragments {

    /** The value of {@code android.billing.store} that selects Google Play. */
    public static final String STORE_PLAY = "play";

    /** The value of {@code android.billing.store} that selects the Amazon Appstore. */
    public static final String STORE_AMAZON = "amazon";

    /** Group and artifact of the Amazon Appstore SDK on Maven Central. */
    public static final String SDK_COORDINATE = "com.amazon.device:amazon-appstore-sdk";

    /**
     * The Amazon Appstore SDK the port's {@code AmazonBillingSupport} is
     * compiled against.
     */
    public static final String SDK_VERSION = "3.0.9";

    /** The receiver Amazon's client delivers every in-app purchase answer to. */
    static final String RECEIVER_CLASS = "com.amazon.device.iap.ResponseReceiver";

    private static final String PLAY_PERMISSION = "com.android.vending.BILLING";

    private AmazonBillingFragments() {
    }

    /**
     * Explains why a store value cannot be built, or returns null when it can.
     *
     * <p>A misspelt store must not fall back to Play: the application would be
     * submitted to Amazon with Play's billing in it and fail there, long after
     * the build said nothing.</p>
     *
     * @param store the {@code android.billing.store} value
     * @return a sentence for the build log, or null
     */
    public static String refusalFor(String store) {
        if (store == null || store.trim().length() == 0
                || STORE_PLAY.equalsIgnoreCase(store.trim())
                || STORE_AMAZON.equalsIgnoreCase(store.trim())) {
            return null;
        }
        return "android.billing.store must be \"" + STORE_PLAY + "\" or \"" + STORE_AMAZON
                + "\", but is \"" + store + "\".";
    }

    /**
     * Whether the build targets the Amazon Appstore.
     *
     * @param store the {@code android.billing.store} value; null and empty mean Play
     * @return true for {@code amazon}, in any case
     */
    public static boolean isAmazon(String store) {
        // Compared without folding: toLowerCase() is locale sensitive.
        return store != null && STORE_AMAZON.equalsIgnoreCase(store.trim());
    }

    /**
     * The gradle dependency line of the selected store's billing library.
     *
     * @param amazon             the Amazon Appstore is selected
     * @param playBillingVersion the resolved Play Billing version; unused for Amazon
     * @return one dependency line, newline terminated
     */
    public static String dependency(boolean amazon, String playBillingVersion) {
        if (amazon) {
            return " implementation '" + SDK_COORDINATE + ":" + SDK_VERSION + "'\n";
        }
        return " implementation 'com.android.billingclient:billing:" + playBillingVersion + "'\n";
    }

    /**
     * The permission the selected store's billing needs. Amazon needs none of
     * the application; its receiver is guarded by a permission Amazon's own
     * client holds.
     *
     * @param amazon the Amazon Appstore is selected
     * @return the element to append, possibly empty
     */
    public static String permissions(boolean amazon) {
        if (amazon) {
            return "";
        }
        return "    <uses-permission android:name=\"" + PLAY_PERMISSION
                + "\" android:required=\"false\" />\n";
    }

    /**
     * The {@code <receiver>} Amazon's client answers through, or an empty string
     * for Play or when the project's own {@code android.xapplication} already
     * declares it (a second declaration fails the manifest merge).
     *
     * @param amazon       the Amazon Appstore is selected
     * @param xApplication the project's {@code android.xapplication} fragment
     * @return the element to append, possibly empty
     */
    public static String receiver(boolean amazon, String xApplication) {
        if (!amazon || (xApplication != null && xApplication.contains(RECEIVER_CLASS))) {
            return "";
        }
        // exported, because the sender is another application; the permission
        // is what keeps every application but Amazon's client from sending.
        return "        <receiver android:name=\"" + RECEIVER_CLASS + "\" android:exported=\"true\""
                + " android:permission=\"com.amazon.inapp.purchasing.Permission.NOTIFY\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"com.amazon.inapp.purchasing.NOTIFY\" />\n"
                + "            </intent-filter>\n"
                + "        </receiver>\n";
    }

    /**
     * The port class the generated activity constructs. It is named in
     * generated source rather than looked up at run time, so the reference
     * survives obfuscation.
     *
     * @param amazon the Amazon Appstore is selected
     * @return a fully qualified class name
     */
    public static String billingSupportClass(boolean amazon) {
        return amazon ? "com.codename1.impl.android.AmazonBillingSupport"
                : "com.codename1.impl.android.BillingSupport";
    }

    /**
     * The port's billing sources this build must not compile, as file names in
     * {@code com/codename1/impl/android}. Each is written against a library
     * only its own store's build has on the class path.
     *
     * @param purchase the application uses the purchase API
     * @param amazon   the Amazon Appstore is selected
     * @return the file names to delete from the generated project
     */
    public static String[] sourcesToRemove(boolean purchase, boolean amazon) {
        if (!purchase) {
            return new String[] {"BillingSupport.java", "AmazonBillingSupport.java"};
        }
        return new String[] {amazon ? "BillingSupport.java" : "AmazonBillingSupport.java"};
    }
}
