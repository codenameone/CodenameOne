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
 * The manifest a packet tunnel needs, as pure text so it can be unit tested.
 *
 * <p>Modelled on {@link CallManifestFragments}, and sharing its two hard-won
 * rules: suppression is tested against the LIVE fragment, because a project
 * that commented its own declaration out was being treated as supplying one;
 * and the test looks for the element the attribute belongs to rather than the
 * attribute anywhere, because a name appearing as a value is not a
 * declaration.</p>
 *
 * <p>There is deliberately no permission to inject. An app does not hold
 * {@code BIND_VPN_SERVICE} -- the SERVICE declares that the system holds it,
 * which is what makes the binding trustworthy -- and the user's consent is a
 * runtime prompt rather than a permission.</p>
 */
final class VpnManifestFragments {

    /** Bumped when the emitted text changes, so a build log names which. */
    static final int FRAGMENT_VERSION = 1;

    /** The service the port ships; see CN1VpnService. */
    static final String TUNNEL_SERVICE =
            "com.codename1.impl.android.vpn.CN1VpnService";

    private VpnManifestFragments() {
    }

    /**
     * Returns the {@code <service>} element a packet tunnel needs, or an
     * empty string when the project already declares it.
     *
     * @param tunnel   {@code com.codename1.vpn.tunnel} usage detected
     * @param existing the current {@code android.xapplication} fragment
     * @return the element to append, possibly empty
     */
    static String services(boolean tunnel, String existing) {
        if (!tunnel || declares(existing, TUNNEL_SERVICE)) {
            return "";
        }
        // android:permission is what makes this a VPN service: it says only
        // the system, holding BIND_VPN_SERVICE, may bind it. Without the
        // attribute Android refuses the binding and establish() answers null
        // on a build that otherwise looks complete.
        //
        // android:exported is spelled out because it is MANDATORY from API
        // 31 for any component with an intent filter, and a manifest missing
        // it fails the build rather than defaulting.
        return "\n        <service android:name=\"" + TUNNEL_SERVICE + "\""
                + " android:permission=\"android.permission.BIND_VPN_SERVICE\""
                + " android:exported=\"true\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.net.VpnService\" />\n"
                + "            </intent-filter>\n"
                + "        </service>\n";
    }

    /**
     * Returns whether {@code existing} already has a LIVE {@code <service>}
     * for {@code name}.
     *
     * <p>The same shape {@link CallManifestFragments} uses, and for the same
     * reasons: a declaration inside a comment is not one, and the class name
     * appearing on a {@code <meta-data>} as a VALUE is not one either.</p>
     *
     * @param existing the accumulated fragment
     * @param name     the service class name
     * @return true when a live declaration is already there
     */
    static boolean declares(String existing, String name) {
        String live = withoutComments(existing == null ? "" : existing);
        int at = live.indexOf("<service");
        while (at >= 0) {
            int close = live.indexOf('>', at);
            if (close < 0) {
                return false;
            }
            String tag = live.substring(at, close);
            if (tag.contains("android:name=\"" + name + "\"")
                    || tag.contains("android:name='" + name + "'")) {
                return true;
            }
            at = live.indexOf("<service", close);
        }
        return false;
    }

    /**
     * Returns {@code xml} with every {@code <!-- ... -->} span removed.
     *
     * <p>An unterminated comment swallows the rest, which is what an XML
     * parser would do with it too.</p>
     *
     * @param xml the fragment
     * @return the fragment with its comments removed
     */
    private static String withoutComments(String xml) {
        int open = xml.indexOf("<!--");
        if (open < 0) {
            return xml;
        }
        StringBuilder sb = new StringBuilder();
        int at = 0;
        while (open >= 0) {
            sb.append(xml, at, open);
            int close = xml.indexOf("-->", open);
            if (close < 0) {
                return sb.toString();
            }
            at = close + "-->".length();
            open = xml.indexOf("<!--", at);
        }
        sb.append(xml.substring(at));
        return sb.toString();
    }
}
