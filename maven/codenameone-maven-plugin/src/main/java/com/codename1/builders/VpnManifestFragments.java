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
 * <p>{@code BIND_VPN_SERVICE} is NOT among the permissions injected. An app
 * does not hold it: the SERVICE declares that the system does, which is what
 * makes the binding trustworthy, and the user's consent is a runtime prompt
 * rather than a permission. The foreground-service permissions ARE injected,
 * because a VPN has to keep running and Android 8 shuts down a plain started
 * service that does.</p>
 */
final class VpnManifestFragments {

    /** Bumped when the emitted text changes, so a build log names which. */
    static final int FRAGMENT_VERSION = 1;

    /** The service the port ships; see CN1VpnService. */
    static final String TUNNEL_SERVICE =
            "com.codename1.impl.android.vpn.CN1VpnService";

    /** What the SERVICE requires of its binder, so only the system may. */
    static final String BIND_VPN_SERVICE =
            "android.permission.BIND_VPN_SERVICE";

    /** The action the system looks a VPN service up by. */
    static final String VPN_ACTION = "android.net.VpnService";

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
    /**
     * Returns the permissions a packet tunnel needs, prepended.
     *
     * @param tunnel         {@code com.codename1.vpn.tunnel} usage detected
     * @param xPermissions   the current accumulated fragment
     * @return the fragment with the permissions added
     */
    static String injectPermissions(boolean tunnel, String xPermissions) {
        if (!tunnel) {
            return xPermissions;
        }
        String out = xPermissions == null ? "" : xPermissions;
        // The service promotes itself, and Android refuses the promotion
        // without this. A tunnel that is not promoted is one Android shuts
        // down shortly after it comes up.
        out = addPermission(out, "android.permission.FOREGROUND_SERVICE");
        // Android 14 wants the permission that matches the TYPE the service
        // promotes with, and systemExempted is the type whose documented
        // exemptions cover VPN apps. Declared whatever the app targets: a
        // device that has never heard of it ignores it.
        out = addPermission(out,
                "android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED");
        return out;
    }

    private static String addPermission(String xPermissions, String name) {
        if (declaresPermission(xPermissions, name)) {
            return xPermissions;
        }
        return "    <uses-permission android:name=\"" + name + "\" />\n"
                + xPermissions;
    }

    /**
     * Returns whether a LIVE {@code <uses-permission>} already declares
     * {@code name}.
     *
     * <p>The same rigour {@link CallManifestFragments} arrived at: a
     * commented-out declaration is not one, and the name appearing as a
     * VALUE -- an {@code android:permission} attribute names a permission a
     * component requires -- is not a declaration that this app holds it.</p>
     *
     * @param existing the accumulated fragment
     * @param name     the permission name
     * @return true when a live declaration is already there
     */
    static boolean declaresPermission(String existing, String name) {
        String live = withoutComments(existing == null ? "" : existing);
        int at = live.indexOf("<uses-permission");
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
            at = live.indexOf("<uses-permission", close);
        }
        return false;
    }

    static String services(boolean tunnel, String existing) {
        if (!tunnel) {
            return "";
        }
        if (declares(existing, TUNNEL_SERVICE)) {
            // The project declared the service itself, so the build stands
            // aside -- but only if what it declared can actually be bound.
            // Suppressing on the NAME alone let a declaration missing
            // android:permission or the android.net.VpnService action
            // replace the working one, and the result is a build that looks
            // complete and cannot establish: Android refuses the binding,
            // establish() answers null, and Tunnels.isSupported() went on
            // saying yes.
            //
            // Refused rather than merged. Rewriting XML the project wrote is
            // guesswork about intent, and the two mechanisms cannot both own
            // one element -- the same reason the VoIP background mode fails
            // a build instead of combining with ios.plistInject.
            String missing = ManifestServiceContract.whatIsMissing(existing,
                    TUNNEL_SERVICE, BIND_VPN_SERVICE, VPN_ACTION,
                    "systemExempted");
            if (missing != null) {
                throw new IllegalArgumentException("The android.xapplication"
                        + " build hint declares " + TUNNEL_SERVICE
                        + " itself, so the build leaves it alone -- but that"
                        + " declaration is missing " + missing + ". Android"
                        + " would refuse to bind it as a VPN, or refuse its"
                        + " foreground promotion, and the tunnel would fail"
                        + " with nothing to say why. Remove the declaration"
                        + " and let the build supply it, or add what is"
                        + " listed.");
            }
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
        // foregroundServiceType matches what the service promotes with, and
        // Android 14 refuses a promotion whose type the manifest does not
        // declare. An older platform ignores an attribute it does not know.
        return "\n        <service android:name=\"" + TUNNEL_SERVICE + "\""
                + " android:permission=\"" + BIND_VPN_SERVICE + "\""
                + " android:foregroundServiceType=\"systemExempted\""
                + " android:exported=\"true\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"" + VPN_ACTION
                + "\" />\n"
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
        return ManifestServiceContract.declares(existing, name);
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
        return ManifestServiceContract.withoutComments(xml);
    }
}
