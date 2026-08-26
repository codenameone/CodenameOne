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
 * Builds the AndroidManifest permission and service fragments injected when
 * the bytecode scanner detects usage of the {@code com.codename1.call}
 * packages.
 *
 * <p>Extracted into a pure static helper for the reasons
 * {@link NearbyManifestFragments} gives: the version-conditional nuances are
 * unit-testable here, and the BuildDaemon copy stays trivially diffable --
 * <b>keep this file in sync with
 * {@code com.codename1.build.daemon.CallManifestFragments}</b>.</p>
 *
 * <p>Why this is here rather than in {@code PlatformFeatureCatalog}: the
 * catalog can name a permission but not qualify it, and it cannot emit a
 * {@code <service>} element at all. A self-managed {@code ConnectionService}
 * needs one carrying its own {@code android:permission} attribute and intent
 * filter, {@code FOREGROUND_SERVICE_PHONE_CALL} exists only from API 34, and
 * the screening service needs a different permission again. None of that fits
 * a flat list.</p>
 *
 * <p>Duplicate suppression uses quote-delimited tokens for the reason the
 * Bluetooth and nearby versions document: a plain substring check would let
 * one permission name mask another that contains it.</p>
 */
final class CallManifestFragments {

    /**
     * Bumped when the fragments change, so a build log names which version
     * produced a manifest.
     */
    static final int FRAGMENT_VERSION = 1;

    /** The port's self-managed ConnectionService. */
    static final String CONNECTION_SERVICE =
            "com.codename1.impl.android.call.CN1ConnectionService";

    /** The port's call screening service. */
    static final String SCREENING_SERVICE =
            "com.codename1.impl.android.call.CN1CallScreeningService";

    private CallManifestFragments() {
    }

    /**
     * Returns {@code xPermissions} with the call permissions prepended.
     *
     * @param xPermissions     the current accumulated manifest fragment
     * @param session          {@code com.codename1.call.session} usage
     *                         detected, which is what earns the right to own
     *                         a call at all
     * @param voip             {@code com.codename1.call.voip} usage detected
     * @param directory        {@code com.codename1.call.directory} usage
     *                         detected
     * @param targetSdkVersion the build's target SDK level
     * @return the fragment with the call entries prepended
     */
    static String injectPermissions(String xPermissions, boolean session,
            boolean voip, boolean directory, int targetSdkVersion) {
        return injectPermissions(xPermissions, session, voip, directory, false,
                targetSdkVersion);
    }

    /**
     * Returns {@code xPermissions} with the call permissions prepended.
     *
     * @param xPermissions     the current accumulated manifest fragment
     * @param session          {@code com.codename1.call.session} usage
     * @param voip             {@code com.codename1.call.voip} usage
     * @param directory        {@code com.codename1.call.directory} usage
     * @param video            the project declares video calls, through the
     *                         {@code call.video} build hint
     * @param targetSdkVersion the build's target SDK level
     * @return the fragment with the call entries prepended
     */
    static String injectPermissions(String xPermissions, boolean session,
            boolean voip, boolean directory, boolean video,
            int targetSdkVersion) {
        String out = xPermissions == null ? "" : xPermissions;

        if (session || voip) {
            // The permission that lets an app own its own calls. Normal
            // rather than dangerous, so it is granted at install time and
            // there is nothing to prompt for -- but Telecom silently ignores
            // every reported call without it.
            out = addPermission(out, "android.permission.MANAGE_OWN_CALLS", "");
            out = addPermission(out, "android.permission.RECORD_AUDIO", "");
            // Every incoming call rings through a notification, because
            // Telecom draws no UI for a self-managed account -- so these two
            // belong to calls in general and not only to the pushed ones.
            // Android 13 needs an explicit grant before anything can appear
            // in the shade, and a call the user cannot see is a call they
            // cannot answer; Android 14 additionally demands the full-screen
            // declaration or the ringing screen degrades to a banner behind
            // the lock screen.
            //
            // USE_FULL_SCREEN_INTENT is restricted to calling and alarm apps,
            // which is why the LocalNotification path leaves it to the
            // android.fullScreenIntent hint. An app that referenced
            // com.codename1.call.session IS a calling app -- the category the
            // restriction exists to admit -- so here it is injected on the
            // scanner's evidence, the same discipline as the voip background
            // mode.
            out = addPermission(out,
                    "android.permission.POST_NOTIFICATIONS", "");
            out = addPermission(out,
                    "android.permission.USE_FULL_SCREEN_INTENT", "");
            if (video) {
                // Behind the hint rather than always. Android cannot grant a
                // runtime permission the manifest does not declare, so
                // Calls.requestPermissions(PERMISSION_CAMERA) reported a
                // denial it could never clear -- but declaring CAMERA for
                // every calling app costs a Play Console conversation and a
                // prompt the user cannot explain, so it follows the project's
                // own statement that it does video.
                out = addPermission(out, "android.permission.CAMERA", "");
            }
        }

        if (voip) {
            // A call that rings while the app is in the background is a
            // foreground service, and from API 34 that service has to declare
            // which type it is. Declared whatever the app targets, for the
            // reason the nearby fragments give: a permission is asked for at
            // runtime according to the level the device is running, and a
            // device below 34 ignores one it has never heard of.
            out = addPermission(out,
                    "android.permission.FOREGROUND_SERVICE", "");
            out = addPermission(out,
                    "android.permission.FOREGROUND_SERVICE_PHONE_CALL", "");
        }

        if (directory) {
            // Deliberately NOT MANAGE_OWN_CALLS. An app that only labels or
            // blocks somebody else's caller never owns a call, and Play
            // Console flags gratuitous telephony permissions.
            out = addPermission(out, "android.permission.READ_CALL_LOG", "");
        }

        return out;
    }

    /**
     * Returns the {@code <service>} elements the call packages need, ready to
     * be concatenated into the {@code <application>} block.
     *
     * @param session   {@code com.codename1.call.session} usage detected
     * @param voip      {@code com.codename1.call.voip} usage detected
     * @param directory {@code com.codename1.call.directory} usage detected
     * @return the elements, or the empty string when none are needed
     */
    static String services(boolean session, boolean voip, boolean directory) {
        return services(session, voip, directory, "");
    }

    /**
     * Returns the {@code <service>} elements the call packages need that
     * {@code existing} does not already declare.
     *
     * <p>Suppression is per service rather than for the block as a whole. An
     * app that hand-declared one of the two in {@code android.xapplication}
     * used to suppress both, so the other went missing and either Telecom
     * could not create the app's calls or Android could not bind the
     * screening service.</p>
     *
     * @param session   {@code com.codename1.call.session} usage detected
     * @param voip      {@code com.codename1.call.voip} usage detected
     * @param directory {@code com.codename1.call.directory} usage detected
     * @param existing  the application fragment the project already supplies
     * @return the elements still needed, or the empty string when none are
     */
    static String services(boolean session, boolean voip, boolean directory,
            String existing) {
        String have = existing == null ? "" : existing;
        StringBuilder sb = new StringBuilder();
        if ((session || voip) && !have.contains(CONNECTION_SERVICE)) {
            // exported=true is required rather than careless: Telecom is a
            // different process and cannot bind an unexported service. The
            // android:permission attribute is what keeps anything other than
            // Telecom from binding it, because only the system holds
            // BIND_TELECOM_CONNECTION_SERVICE.
            sb.append("        <service android:name=\"")
                    .append(CONNECTION_SERVICE)
                    .append("\"\n                 android:permission=")
                    .append("\"android.permission.BIND_TELECOM_CONNECTION_SERVICE\"");
            if (voip) {
                // From API 34 startForeground is refused for a type the
                // manifest never declared, so an app that grants itself
                // FOREGROUND_SERVICE_PHONE_CALL and stops there cannot keep
                // this service alive for a call that arrived in the
                // background. Declared whatever the app targets: the
                // attribute is ignored below 29, where it did not exist.
                sb.append("\n                 android:foregroundServiceType=")
                        .append("\"phoneCall\"");
            }
            sb.append("\n                 android:exported=\"true\">\n")
                    .append("            <intent-filter>\n")
                    .append("                <action android:name=")
                    .append("\"android.telecom.ConnectionService\" />\n")
                    .append("            </intent-filter>\n")
                    .append("        </service>\n");
        }
        if (directory && !have.contains(SCREENING_SERVICE)) {
            sb.append("        <service android:name=\"")
                    .append(SCREENING_SERVICE)
                    .append("\"\n                 android:permission=")
                    .append("\"android.permission.BIND_SCREENING_SERVICE\"")
                    .append("\n                 android:exported=\"true\">\n")
                    .append("            <intent-filter>\n")
                    .append("                <action android:name=")
                    .append("\"android.telecom.CallScreeningService\" />\n")
                    .append("            </intent-filter>\n")
                    .append("        </service>\n");
        }
        return sb.toString();
    }

    /**
     * The minimum SDK the detected packages need, or 0 when none is implied.
     *
     * <p>A self-managed {@code ConnectionService} arrives exactly at API 26
     * and there is nothing to degrade to below it, so an app that owns calls
     * carries that floor. Screening alone needs only 24 to compile, and the
     * bridge reports the capability absent until 29 where the role exists.</p>
     *
     * @param session   {@code com.codename1.call.session} usage detected
     * @param voip      {@code com.codename1.call.voip} usage detected
     * @param directory {@code com.codename1.call.directory} usage detected
     * @return the floor, or 0
     */
    static int minimumSdk(boolean session, boolean voip, boolean directory) {
        if (session || voip) {
            return 26;
        }
        if (directory) {
            return 24;
        }
        return 0;
    }

    private static String addPermission(String xPermissions, String name,
            String extraAttributes) {
        if (xPermissions.contains("\"" + name + "\"")) {
            return xPermissions;
        }
        return "    <uses-permission android:name=\"" + name + "\""
                + extraAttributes + " />\n" + xPermissions;
    }
}
