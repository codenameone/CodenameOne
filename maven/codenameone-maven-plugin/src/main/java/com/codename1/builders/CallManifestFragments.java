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

    /** The port's full-screen ringing activity. */
    static final String INCOMING_ACTIVITY =
            "com.codename1.impl.android.call.CN1IncomingCallActivity";

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

        // Nothing for `directory`. Deliberately NOT MANAGE_OWN_CALLS -- an app
        // that only labels or blocks somebody else's caller never owns a call
        // -- and deliberately not READ_CALL_LOG either: CN1CallScreeningService
        // takes the number from Call.Details, which the system hands it, and
        // never reads the log. Declaring it put a screening-only build under
        // call-log policy and store review for an access it does not make,
        // against a package documented as carrying no telephony permissions.
        // The role and the BIND_SCREENING_SERVICE declaration are the whole
        // requirement.

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
        return services(session, voip, directory, "", 0);
    }

    /**
     * As {@link #services(boolean, boolean, boolean, String, int)} with no
     * compile-SDK constraint.
     *
     * @param session   {@code com.codename1.call.session} usage detected
     * @param voip      {@code com.codename1.call.voip} usage detected
     * @param directory {@code com.codename1.call.directory} usage detected
     * @param existing  the application fragment the project already supplies
     * @return the elements still needed, or the empty string when none are
     */
    static String services(boolean session, boolean voip, boolean directory,
            String existing) {
        return services(session, voip, directory, existing, 0);
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
    /**
     * Whether {@code existing} declares a component with exactly this
     * {@code android:name}.
     *
     * <p>A plain substring test was wrong in both directions. It matched the
     * name inside anything that merely mentioned it -- an XML comment, a
     * {@code meta-data} value, an {@code <intent-filter>} -- and suppressed a
     * service the app never declared. It also matched a longer class that
     * starts with ours, so an app shipping its own
     * {@code CN1ConnectionServiceProxy} silently lost the real one. Either
     * way the component goes missing, and a missing ConnectionService is not
     * a build error: Telecom simply refuses every call at runtime.</p>
     *
     * @param existing the application fragment the project already supplies
     * @param name     the fully qualified component name
     * @return true when the fragment already declares that exact component
     */
    /**
     * Returns whether {@code existing} already has a LIVE
     * {@code <uses-permission>} for {@code name}.
     *
     * <p>The same rigour {@link #declares} applies to services, and for the
     * same reason -- this one was a bare substring test for the quoted name
     * anywhere in the fragment. A developer who comments a declaration out,
     * which is the ordinary way to disable one, matched it: the generated
     * live element was suppressed, the parser then discarded the comment, and
     * the manifest shipped without the permission. That is not a build error.
     * Without MANAGE_OWN_CALLS Telecom refuses every self-managed call on the
     * device; without RECORD_AUDIO there is no call audio. The same test also
     * matched the name used as a VALUE elsewhere -- an {@code android:permission}
     * attribute on a component names a permission it requires, which is not a
     * declaration that this app holds one.</p>
     *
     * @param existing the accumulated fragment
     * @param name     the permission name
     * @return true when a live declaration is already there
     */
    private static boolean declaresPermission(String existing, String name) {
        return ManifestServiceContract.declaresPermission(existing, name);
    }

    private static boolean declares(String existing, String name) {
        // The attribute has to sit inside a LIVE <service> start tag. Testing
        // the whole fragment for the attribute matched it wherever it
        // appeared -- inside a <!-- commented-out --> declaration, or on a
        // <meta-data android:name="..."> that names the class as a value --
        // and suppressed the real element for it. The manifest then has no
        // component handling android.telecom.ConnectionService, which is not
        // a build error: Telecom simply refuses every call on the device.
        return ManifestServiceContract.declares(existing, name);
    }

    /// The same question for the ringing screen, which is an activity.
    ///
    /// Separate rather than folded into declares(): an element check that
    /// looked only at services would regenerate an activity the application
    /// had already declared, and AAPT rejects the duplicate.
    private static boolean declaresActivity(String existing, String name) {
        return ManifestServiceContract.declaresActivity(existing, name);
    }

    /**
     * Returns {@code xml} with every {@code <!-- ... -->} span removed.
     *
     * <p>An unterminated comment swallows the rest of the fragment, which is
     * what an XML parser would do with it too.</p>
     *
     * @param xml the fragment
     * @return the fragment with its comments removed
     */
    private static String withoutComments(String xml) {
        return ManifestServiceContract.withoutComments(xml);
    }

    /**
     * Whether the project asked for video calls.
     *
     * <p>One reader for a hint three places consult. Compared with
     * {@code "true".equals(...)} at one site and trimmed at another,
     * {@code " true "} turned the camera purpose string off and the
     * provider's video flag on -- so the app advertised video and was denied
     * the camera, or terminated for asking without a purpose string. Which
     * of the two readings was "right" is not the point: they disagreed.</p>
     *
     * <p>Trimmed and case-insensitive, matching the most permissive reader
     * that existed, so no configuration that worked stops working.</p>
     *
     * @param platformOverride the {@code ios.} or {@code android.} hint, or
     *                         null
     * @param shared           the cross-platform {@code call.video} hint, or
     *                         null
     * @return whether video was asked for
     */
    static boolean videoRequested(String platformOverride, String shared) {
        String value = platformOverride != null ? platformOverride : shared;
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    /** What only Telecom holds, so only Telecom can bind the connection. */
    static final String BIND_CONNECTION =
            "android.permission.BIND_TELECOM_CONNECTION_SERVICE";

    /** The action Telecom finds a self-managed ConnectionService by. */
    static final String CONNECTION_ACTION =
            "android.telecom.ConnectionService";

    /** What only the system holds, so only it can bind the screener. */
    static final String BIND_SCREENING =
            "android.permission.BIND_SCREENING_SERVICE";

    /** The action Telecom finds a CallScreeningService by. */
    static final String SCREENING_ACTION =
            "android.telecom.CallScreeningService";

    static String services(boolean session, boolean voip, boolean directory,
            String existing, int compileSdk) {
        String have = existing == null ? "" : existing;
        StringBuilder sb = new StringBuilder();
        // A declaration the project wrote itself suppresses the generated
        // one, and has to carry what the generated one would have. On the
        // NAME alone an older or partial declaration replaced a working
        // element -- Telecom refuses to bind a ConnectionService without
        // BIND_TELECOM_CONNECTION_SERVICE or its action, a component with a
        // filter and no android:exported fails the build from API 31, and
        // from API 34 startForeground is refused for a type the manifest
        // never declared. Calls.isSupported() went on answering true for all
        // of it.
        //
        // Refused rather than merged, as the VPN fragments are: rewriting
        // XML the project wrote is guesswork about intent, and two
        // mechanisms cannot both own one element.
        if (session || voip) {
            // The VoIP type only when the generated element would have
            // carried it -- it is emitted for a VoIP app compiling against
            // 29 or later and nowhere else, so demanding it of a legacy
            // build would refuse a declaration that is complete for the
            // manifest this build can actually write.
            String missing = ManifestServiceContract.whatIsMissing(have,
                    CONNECTION_SERVICE, BIND_CONNECTION, CONNECTION_ACTION,
                    voip && (compileSdk <= 0 || compileSdk >= 29)
                            ? "phoneCall" : null);
            if (missing != null) {
                throw new IllegalArgumentException("The android.xapplication"
                        + " build hint declares " + CONNECTION_SERVICE
                        + " itself, so the build leaves it alone -- but that"
                        + " declaration is missing " + missing + ". Telecom"
                        + " would refuse to bind it and every call reported"
                        + " through it would fail on the device. Remove the"
                        + " declaration and let the build supply it, or add"
                        + " what is listed.");
            }
        }
        if (directory) {
            String missing = ManifestServiceContract.whatIsMissing(have,
                    SCREENING_SERVICE, BIND_SCREENING, SCREENING_ACTION,
                    null);
            if (missing != null) {
                throw new IllegalArgumentException("The android.xapplication"
                        + " build hint declares " + SCREENING_SERVICE
                        + " itself, so the build leaves it alone -- but that"
                        + " declaration is missing " + missing + ". The"
                        + " system would never route a call to it and"
                        + " screening would do nothing. Remove the"
                        + " declaration and let the build supply it, or add"
                        + " what is listed.");
            }
        }
        if ((session || voip) && !declares(have, CONNECTION_SERVICE)) {
            // exported=true is required rather than careless: Telecom is a
            // different process and cannot bind an unexported service. The
            // android:permission attribute is what keeps anything other than
            // Telecom from binding it, because only the system holds
            // BIND_TELECOM_CONNECTION_SERVICE.
            sb.append("        <service android:name=\"")
                    .append(CONNECTION_SERVICE)
                    .append("\"\n                 android:permission=")
                    .append("\"").append(BIND_CONNECTION).append("\"");
            if (voip) {
                // From API 34 startForeground is refused for a type the
                // manifest never declared, so an app that grants itself
                // FOREGROUND_SERVICE_PHONE_CALL and stops there cannot keep
                // this service alive for a call that arrived in the
                // background. Declared whatever the app TARGETS...
                // ...but only when the project compiles against 29 or
                // later. The enum value arrived in API 29 and AAPT REJECTS a
                // manifest that names one it does not know, so emitting it
                // unconditionally broke the still-supported legacy
                // configuration -- android.useGradle8=false with
                // android.buildToolsVersion=28 -- before compilation even
                // started. Below 29 the attribute is not needed: the
                // startForeground type check it exists for arrives with it.
                if (compileSdk <= 0 || compileSdk >= 29) {
                    sb.append("\n                 android:foregroundServiceType=")
                            .append("\"phoneCall\"");
                }
            }
            sb.append("\n                 android:exported=\"true\">\n")
                    .append("            <intent-filter>\n")
                    .append("                <action android:name=")
                    .append("\"").append(CONNECTION_ACTION).append("\" />\n")
                    .append("            </intent-filter>\n")
                    .append("        </service>\n");
        }
        if ((session || voip) && !declaresActivity(have, INCOMING_ACTIVITY)) {
            // The screen the notification's full-screen intent launches. A
            // self-managed calling app gets no system call UI, so this is the
            // ringing screen -- and an activity Android never heard of cannot
            // be launched, so a missing entry here turns the full-screen
            // intent into nothing at all.
            //
            // exported=false: it is launched by this app's own PendingIntent
            // and nothing outside has any business starting it.
            //
            // showWhenLocked, NOT showOnLockScreen. The latter is not a
            // public <activity> attribute: the documented manifest set is
            // showWhenLocked and turnScreenOn, both API 27. AAPT rejects an
            // attribute it cannot resolve, so emitting the private spelling
            // would have failed the manifest of every call build outright
            // rather than degrading to no lock-screen behaviour.
            //
            // Gated on the compile SDK for the same reason the connection
            // service's foregroundServiceType is: AAPT also rejects an
            // attribute the compile SDK does not know, and the legacy
            // configuration this project still supports compiles against 28.
            // Below 27 the attributes are not merely unavailable but
            // unnecessary, because the activity's runtime path uses the
            // pre-27 window flags there, which is the only mechanism that
            // platform has.
            //
            // excludeFromRecents and noHistory because a ringing screen is
            // not somewhere to return to: once the call is answered or gone,
            // an entry in Recents would re-open a screen for a call that no
            // longer exists.
            sb.append("        <activity android:name=\"")
                    .append(INCOMING_ACTIVITY)
                    .append("\"\n                  android:exported=\"false\"");
            if (compileSdk <= 0 || compileSdk >= 27) {
                sb.append("\n                  android:showWhenLocked=")
                        .append("\"true\"")
                        .append("\n                  android:turnScreenOn=")
                        .append("\"true\"");
            }
            sb.append("\n                  android:excludeFromRecents=")
                    .append("\"true\"")
                    .append("\n                  android:noHistory=\"true\"")
                    .append("\n                  android:launchMode=")
                    .append("\"singleTop\"")
                    .append("\n                  android:theme=")
                    .append("\"@android:style/Theme.NoTitleBar.Fullscreen\" />\n");
        }
        if (directory && !declares(have, SCREENING_SERVICE)) {
            sb.append("        <service android:name=\"")
                    .append(SCREENING_SERVICE)
                    .append("\"\n                 android:permission=")
                    .append("\"").append(BIND_SCREENING).append("\"")
                    .append("\n                 android:exported=\"true\">\n")
                    .append("            <intent-filter>\n")
                    .append("                <action android:name=")
                    .append("\"").append(SCREENING_ACTION).append("\" />\n")
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
        if (declaresPermission(xPermissions, name)) {
            return xPermissions;
        }
        return "    <uses-permission android:name=\"" + name + "\""
                + extraAttributes + " />\n" + xPermissions;
    }
}
