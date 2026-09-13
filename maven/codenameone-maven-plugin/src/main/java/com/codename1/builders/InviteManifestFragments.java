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

import java.util.ArrayList;
import java.util.List;

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
     * {@code existing} unchanged when it already covers the invite links.
     *
     * @param existing the current {@code android.xintent_filter} value
     * @param host     the invite link host, for example
     *                 {@code cloud.codenameone.com}
     * @param slug     this application's path segment, may be empty
     * @return the value to put back on the hint
     */
    static String injectAppLinks(String existing, String host, String slug) {
        String current = existing == null ? "" : existing;
        if (host == null || host.length() == 0) {
            return current;
        }
        // Trimmed HERE, because the other reader of this hint trims too.
        //
        // The generated startup code stores invite.slug trimmed, so a hint
        // written with a stray space around it made the app mint
        // /i/<trimmed>/<code> while the pathPrefix in the manifest kept the
        // space -- and a link that does not match the filter opens the browser
        // instead of the app. Nothing reports it: the build succeeds, the
        // filter is there, and every invite silently misses it.
        //
        // One normalization for both outputs, in the place both go through,
        // rather than a second trim at a call site that has to remember.
        String path = slug == null ? "" : slug.trim();
        if (declaresInviteLinks(current, host, path)) {
            return current;
        }
        return current + filter(host, path);
    }

    /**
     * Whether {@code existing} already declares an intent filter that covers
     * the invite links on {@code host}.
     *
     * <p>The host alone does not settle it. An application may already declare
     * an unrelated filter on the same host -- {@code /account/} on
     * {@code cloud.codenameone.com}, say -- and treating that as coverage
     * suppressed the invite filter, so invite links kept opening in the
     * browser. So the path is required too, and it has to be a path this
     * filter would really accept: a prefix of the invite prefix, never merely
     * a string that contains it.</p>
     *
     * <p>The host is matched as a whole quoted attribute rather than as a
     * substring. A plain {@code contains(host)} would read
     * {@code android:host="staging.cloud.codenameone.com"} as already
     * declaring {@code cloud.codenameone.com}, and the developer's staging
     * filter would suppress the production one.</p>
     *
     * @param existing the current hint value
     * @param host     the host to look for
     * @param slug     this application's path segment, may be empty
     * @return true when the invite links are already covered
     */
    static boolean declaresInviteLinks(String existing, String host, String slug) {
        if (existing == null || host == null) {
            return false;
        }
        // Asked of each filter separately, because an intent filter matches
        // only when its own host and its own path both accept the url. Asked
        // of the whole string, a filter for this host on /account/ and an
        // unrelated host on /i/ answered yes between them, and the invite
        // filter was suppressed although neither one would ever open an invite
        // link.
        for (String block : filterBlocks(existing)) {
            if (coversInviteLinks(block, host, slug)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The individual {@code <intent-filter>} elements of a hint value.
     *
     * <p>Text outside any filter is returned as one more block. It cannot be a
     * real filter, so it can only fail the test below -- which is the safe
     * direction: the cost of not recognizing coverage is a duplicate filter,
     * and the cost of wrongly recognizing it is invite links that open the
     * browser for ever.</p>
     *
     * @param existing the current hint value
     * @return the blocks to test, never null
     */
    static List<String> filterBlocks(String existing) {
        List<String> out = new ArrayList<String>();
        String open = "<intent-filter";
        String close = "</intent-filter>";
        int at = 0;
        while (true) {
            int start = existing.indexOf(open, at);
            if (start < 0) {
                out.add(existing.substring(at));
                return out;
            }
            if (start > at) {
                out.add(existing.substring(at, start));
            }
            int end = existing.indexOf(close, start);
            if (end < 0) {
                out.add(existing.substring(start));
                return out;
            }
            out.add(existing.substring(start, end + close.length()));
            at = end + close.length();
        }
    }

    /**
     * Whether one filter accepts the invite links on {@code host}.
     *
     * @param block the single filter
     * @param host  the invite link host
     * @param slug  this application's path segment, may be empty
     * @return true when this filter alone covers them
     */
    private static boolean coversInviteLinks(String block, String host, String slug) {
        if (!declaresHost(block, host)) {
            return false;
        }
        // The scheme too, in this same filter. An http-only filter on the
        // invite host and path claims nothing about https, and the links this
        // builder generates are https -- so treating it as coverage suppressed
        // the generated filter and left every invite link opening the browser.
        // A filter that names no scheme at all matches none of ours: Android
        // requires a scheme before a host is considered.
        if (block.indexOf("android:scheme=\"https\"") < 0) {
            return false;
        }
        String prefix = pathPrefix(slug);
        // Any pathPrefix that is a prefix of ours covers our links: a filter
        // on "/i/" accepts "/i/<slug>/<code>". The reverse is not true, and a
        // longer or unrelated prefix leaves the invite links uncovered.
        String attr = "android:pathPrefix=\"";
        int at = block.indexOf(attr);
        while (at >= 0) {
            int from = at + attr.length();
            int end = block.indexOf('"', from);
            if (end < 0) {
                return false;
            }
            if (prefix.startsWith(block.substring(from, end))) {
                return true;
            }
            at = block.indexOf(attr, end);
        }
        // A filter that names the host and no path at all matches every path
        // on it, invite links included.
        return block.indexOf("android:path") < 0;
    }

    /**
     * Whether {@code existing} already declares an intent filter for
     * {@code host}.
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
     * The path prefix the invite filter claims.
     *
     * @param slug this application's path segment, may be null or empty
     * @return the prefix, always ending in a slash
     */
    static String pathPrefix(String slug) {
        return slug == null || slug.length() == 0 ? "/i/" : "/i/" + slug + "/";
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
    static String filter(String host, String slug) {
        // The path is scoped to this app's own slug when one is configured.
        // The link domain is shared by every invite-enabled Codename One app,
        // so a bare /i/ prefix makes every one of them an eligible handler for
        // every invite url -- Android then shows a chooser, or opens the wrong
        // app, and the slug inside the path cannot disambiguate because the
        // filter accepts them all. This is the Android twin of the
        // apple-app-site-association collision the slug exists to solve.
        //
        // Without a slug the broad filter is still emitted, because a filter
        // that matches nothing would be worse: the app would never open its own
        // links at all. That case is documented on the invite.slug hint.
        String prefix = pathPrefix(slug);
        return "\n        <intent-filter android:autoVerify=\"true\">\n"
                + "            <action android:name=\"android.intent.action.VIEW\" />\n"
                + "            <category android:name=\"android.intent.category.DEFAULT\" />\n"
                + "            <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                + "            <data android:scheme=\"https\"\n"
                + "                  android:host=\"" + host + "\"\n"
                + "                  android:pathPrefix=\"" + prefix + "\" />\n"
                + "        </intent-filter>\n";
    }
}
