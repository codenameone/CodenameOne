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
 * Builds the App Links intent filter injected into the main activity when the
 * bytecode scanner detects usage of {@code com.codename1.analytics.invite}.
 *
 * <p>Extracted into a pure static helper for the reason
 * {@link CallManifestFragments} gives: the nuances are unit-testable here and
 * the BuildDaemon copy stays trivially diffable -- <b>keep this file in sync
 * with {@code com.codename1.build.daemon.InviteManifestFragments}</b>.</p>
 *
 * <p>Why this is here rather than in {@code PlatformFeatureCatalog}: the
 * catalog has no manifest-fragment channel at all. It can name a permission, a
 * feature or a meta-data pair, and an {@code <intent-filter>} carrying
 * {@code android:autoVerify} is none of those.</p>
 *
 * <p>The fragment is appended to the {@code android.xintent_filter} build hint
 * rather than emitted at a new manifest site. That hint is already rendered
 * inside the main {@code <activity>}, and it is rendered a second time into
 * the wear companion manifest -- so appending to it reaches both, and cannot
 * drift the way two separate injection sites would.</p>
 */
final class InviteManifestFragments {

    /**
     * Bumped when the fragment changes, so a build log names which version
     * produced a manifest.
     */
    static final int FRAGMENT_VERSION = 1;

    private InviteManifestFragments() {
    }

    /**
     * Returns {@code existing} with the invite App Links filter appended, or
     * {@code existing} unchanged when the host is already declared.
     *
     * @param existing the current {@code android.xintent_filter} value
     * @param host     the invite link host, for example
     *                 {@code cloud.codenameone.com}
     * @return the value to put back on the hint
     */
    static String injectAppLinks(String existing, String host) {
        String current = existing == null ? "" : existing;
        if (host == null || host.length() == 0) {
            return current;
        }
        if (declaresHost(current, host)) {
            return current;
        }
        return current + filter(host);
    }

    /**
     * Whether {@code existing} already declares an intent filter for
     * {@code host}.
     *
     * <p>Matched as a whole quoted attribute rather than as a substring. A
     * plain {@code contains(host)} would read
     * {@code android:host="staging.cloud.codenameone.com"} as already
     * declaring {@code cloud.codenameone.com}, and the developer's staging
     * filter would suppress the production one.</p>
     *
     * @param existing the current hint value
     * @param host     the host to look for
     * @return true when the host is already declared
     */
    static boolean declaresHost(String existing, String host) {
        if (existing == null || host == null) {
            return false;
        }
        return existing.indexOf("android:host=\"" + host + "\"") >= 0;
    }

    /**
     * The filter itself.
     *
     * <p>{@code android:autoVerify="true"} is what makes Android open the app
     * instead of the browser without a disambiguation dialog. It only takes
     * effect if the host serves an {@code assetlinks.json} naming this
     * application's package and the SHA-256 of the certificate the installed
     * APK is really signed with -- which, under Play App Signing, is Google's
     * key and not the upload key.</p>
     *
     * @param host the invite link host
     * @return the intent filter XML
     */
    static String filter(String host) {
        return "\n        <intent-filter android:autoVerify=\"true\">\n"
                + "            <action android:name=\"android.intent.action.VIEW\" />\n"
                + "            <category android:name=\"android.intent.category.DEFAULT\" />\n"
                + "            <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                + "            <data android:scheme=\"https\"\n"
                + "                  android:host=\"" + host + "\"\n"
                + "                  android:pathPrefix=\"/i/\" />\n"
                + "        </intent-filter>\n";
    }
}
