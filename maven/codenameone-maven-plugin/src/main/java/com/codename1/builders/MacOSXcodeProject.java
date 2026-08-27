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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the property lists and entitlements a native macOS app bundle needs.
 *
 * <p>Deliberately free of any dependency on Xcode, the {@code xcodeproj} Ruby
 * gem, or a Mac at all, so the shape of what a build would emit can be asserted
 * in an ordinary unit test. The Catalyst path had none of that: its settings were
 * injected post hoc into an already-generated iOS project by a Ruby script, which
 * meant the only way to find out what a build produced was to run one.</p>
 *
 * <p>Plists are built from a map and serialized, rather than patched into a
 * template. That is what lets {@code macos.plistInject} merge as a map, so a key
 * the app supplies twice is an ordinary collision this class can name, instead of
 * a second manifest silently written beside the first. The Catalyst builder needs
 * a structural XML parser to defend against exactly that; there is nothing here
 * for such a parser to defend.</p>
 */
public class MacOSXcodeProject {

    /** Sandbox entitlement keys, in the order they are written. */
    static final String ENT_SANDBOX = "com.apple.security.app-sandbox";
    static final String ENT_NETWORK_CLIENT = "com.apple.security.network.client";
    static final String ENT_NETWORK_SERVER = "com.apple.security.network.server";
    static final String ENT_FILES_USER_SELECTED = "com.apple.security.files.user-selected.read-write";
    static final String ENT_CAMERA = "com.apple.security.device.camera";
    /// The APNs entitlement key, in its macOS spelling.
    ///
    /// macOS and iOS do not agree on this one. iOS wants a bare
    /// "aps-environment" and macOS wants it under the com.apple.developer
    /// prefix, and a build signed with the other platform's spelling carries an
    /// entitlement the provisioning profile does not match. Verified against
    /// Xcode's own capability templates rather than remembered: the macOS
    /// CloudKit template writes com.apple.developer.aps-environment where the
    /// iOS one beside it writes aps-environment.
    static final String ENT_APS_ENVIRONMENT = "com.apple.developer.aps-environment";
    static final String ENT_MICROPHONE = "com.apple.security.device.audio-input";
    static final String ENT_BLUETOOTH = "com.apple.security.device.bluetooth";
    static final String ENT_LOCATION = "com.apple.security.personal-information.location";
    static final String ENT_DISABLE_LIBRARY_VALIDATION = "com.apple.security.cs.disable-library-validation";
    static final String ENT_FILES_USER_SELECTED_RO = "com.apple.security.files.user-selected.read-only";
    static final String ENT_FILES_DOWNLOADS = "com.apple.security.files.downloads.read-write";
    static final String ENT_CALENDARS = "com.apple.security.personal-information.calendars";
    static final String ENT_ALLOW_JIT = "com.apple.security.cs.allow-jit";
    static final String ENT_ALLOW_UNSIGNED_MEMORY = "com.apple.security.cs.allow-unsigned-executable-memory";

    /**
     * The privacy usage descriptions a capability needs, and the sentence used
     * when the application does not supply one.
     *
     * <p>These are not cosmetic. macOS terminates a process that touches a
     * TCC-gated API with no usage description in its bundle -- there is no
     * prompt and no recoverable error, so an app built without them crashes the
     * first time it opens the camera. The application's own string always wins;
     * this is the floor, not the answer.</p>
     */
    static final String USAGE_CAMERA = "NSCameraUsageDescription";
    static final String USAGE_MICROPHONE = "NSMicrophoneUsageDescription";
    static final String USAGE_BLUETOOTH = "NSBluetoothAlwaysUsageDescription";
    static final String USAGE_LOCATION = "NSLocationWhenInUseUsageDescription";
    /// macOS 14 split EventKit's prompt in two, and an app that only writes must
    /// still declare the write-only key. Both are emitted when the calendar
    /// entitlement is granted, because either API can be the first one reached.
    static final String USAGE_CALENDARS = "NSCalendarsFullAccessUsageDescription";
    static final String USAGE_CALENDARS_WRITE = "NSCalendarsWriteOnlyAccessUsageDescription";
    static final String USAGE_REMINDERS = "NSRemindersFullAccessUsageDescription";
    /// The pre-14 spellings. Not legacy clutter: the deployment floor is 11.0,
    /// and macOS 11 through 13 read ONLY these -- an app carrying just the
    /// macOS 14 keys cannot be authorized for EventKit there at all, however
    /// complete its entitlements. Both sets ship, which is what Apple's own
    /// migration guidance says to do while a build still supports both.
    static final String USAGE_CALENDARS_LEGACY = "NSCalendarsUsageDescription";
    static final String USAGE_REMINDERS_LEGACY = "NSRemindersUsageDescription";

    private MacOSXcodeProject() {
    }

    /**
     * The Info.plist contents for a native macOS app, as an ordered map.
     *
     * <p>Carries no {@code UI*} key at all. In particular there is no
     * {@code UIApplicationSceneManifest}: multiple windows are what AppKit does,
     * not a capability a process-wide plist key switches on. That key exists on
     * the Catalyst path because a {@code UIWindowScene} cannot be activated
     * without it, and carrying it here would be cargo.</p>
     *
     * <p>There is also no {@code NSMainNibFile}. The menu bar is built in code,
     * which is what keeps Interface Builder out of the build entirely -- and with
     * it the failure that forced the Catalyst slice to exclude all four of its
     * XIBs.</p>
     */
    public static Map<String, Object> infoPlist(String displayName, String bundleId,
            String version, String bundleVersion, MacOSBuildHints hints) {
        Map<String, Object> plist = new LinkedHashMap<String, Object>();
        plist.put("CFBundleDevelopmentRegion", "en");
        plist.put("CFBundleDisplayName", displayName);
        plist.put("CFBundleExecutable", "${EXECUTABLE_NAME}");
        plist.put("CFBundleIdentifier", bundleId);
        plist.put("CFBundleInfoDictionaryVersion", "6.0");
        plist.put("CFBundleName", displayName);
        plist.put("CFBundlePackageType", "APPL");
        plist.put("CFBundleShortVersionString", version);
        plist.put("CFBundleVersion", bundleVersion);
        plist.put("CFBundleIconName", "AppIcon");
        plist.put("LSMinimumSystemVersion", hints.getMinDeploymentTarget());
        plist.put("LSApplicationCategoryType", hints.getAppCategory());
        plist.put("NSPrincipalClass", "NSApplication");
        plist.put("NSHighResolutionCapable", Boolean.TRUE);
        plist.put("NSSupportsAutomaticGraphicsSwitching", Boolean.TRUE);
        if (hints.getCopyright() != null && hints.getCopyright().length() > 0) {
            plist.put("NSHumanReadableCopyright", hints.getCopyright());
        }
        return plist;
    }

    /**
     * The {@code CFBundleURLTypes} entry for the configured URL schemes, or null.
     *
     * <p>Reads {@code ios.urlSchemes} / {@code ios.urlScheme}, which is where a
     * project migrating off Catalyst already has them and which the iOS plist
     * path registers the same way. Without this the generated stub implements
     * {@code shouldApplicationHandleURL} and macOS never routes a deep link to
     * the application, because nothing claimed the scheme.</p>
     *
     * <p>{@code macos.plistInject} cannot stand in for it: every injected value
     * is serialized as a string, and this key is an array of dictionaries.</p>
     *
     * <p>The hint carries raw plist markup on the iOS side --
     * {@code <string>a</string><string>b</string>} -- so the scheme names are
     * lifted out of it rather than pasted in, since this plist is built from a
     * map. A value with no markup at all is taken as a single scheme, which is
     * what someone writing the hint by hand tends to produce.</p>
     */
    public static List<Object> urlTypes(String bundleId, String schemesHint) {
        if (schemesHint == null || schemesHint.trim().length() == 0) {
            return null;
        }
        List<Object> schemes = new ArrayList<Object>();
        String hint = schemesHint.trim();
        int at = hint.indexOf("<string>");
        if (at < 0) {
            schemes.add(hint);
        } else {
            while (at >= 0) {
                int end = hint.indexOf("</string>", at);
                if (end < 0) {
                    break;
                }
                String scheme = hint.substring(at + "<string>".length(), end).trim();
                if (scheme.length() > 0) {
                    schemes.add(scheme);
                }
                at = hint.indexOf("<string>", end);
            }
        }
        if (schemes.isEmpty()) {
            return null;
        }
        Map<String, Object> type = new LinkedHashMap<String, Object>();
        if (bundleId != null && bundleId.length() > 0) {
            type.put("CFBundleURLName", bundleId);
        }
        type.put("CFBundleURLSchemes", schemes);
        List<Object> types = new ArrayList<Object>();
        types.add(type);
        return types;
    }

    /**
     * Merges an app-supplied plist fragment over the generated one.
     *
     * <p>Returns the keys that were already present, so the caller can refuse a
     * build that would silently override something the port depends on -- setting
     * {@code NSPrincipalClass} to anything other than {@code NSApplication}
     * produces a bundle that launches to nothing.</p>
     */
    /**
     * The keys a raw plist fragment declares at its ROOT, in order.
     *
     * <p>Only the root members, because the caller uses this list to delete
     * generated entries from the top-level plist. A {@code <key>} nested inside
     * one of the fragment's own dictionaries or arrays replaces nothing at the
     * top level, so reporting it deleted a generated value the fragment never
     * supplied: a fragment adding a URL type whose sub-dictionary happens to name
     * CFBundleIdentifier removed the bundle's actual identifier, and the result
     * fails to bundle or sign for a reason nothing in the log connects to the
     * hint.</p>
     *
     * <p>Comments are stripped first, for the same reason and worse: a
     * commented-out override is the one form that is unmistakably NOT in effect,
     * and it was deleting the generated key anyway.</p>
     *
     * <p>Still a scan rather than a parse -- the fragment is written verbatim and
     * a malformed one has to reach codesign as the customer wrote it -- but a
     * scan that tracks depth. Values cannot forge a depth change: a literal
     * {@code <dict>} inside a string value would have to be escaped to be valid
     * XML at all.</p>
     */
    public static List<String> injectedPlistKeys(String xml) {
        List<String> keys = new ArrayList<String>();
        if (xml == null) {
            return keys;
        }
        String scan = stripXmlComments(xml);
        int depth = 0;
        int at = 0;
        while (at < scan.length()) {
            int open = scan.indexOf('<', at);
            if (open < 0) {
                return keys;
            }
            int close = scan.indexOf('>', open);
            if (close < 0) {
                return keys;
            }
            String tag = scan.substring(open + 1, close).trim();
            boolean selfClosing = tag.endsWith("/");
            if (tag.startsWith("/")) {
                String name = tag.substring(1).trim();
                if ("dict".equals(name) || "array".equals(name)) {
                    depth--;
                }
                at = close + 1;
                continue;
            }
            String name = tag.endsWith("/") ? tag.substring(0, tag.length() - 1).trim() : tag;
            int space = name.indexOf(' ');
            if (space > -1) {
                name = name.substring(0, space);
            }
            if ("key".equals(name) && depth == 0) {
                int end = scan.indexOf("</key>", close);
                if (end < 0) {
                    return keys;
                }
                keys.add(scan.substring(close + 1, end).trim());
                at = end + "</key>".length();
                continue;
            }
            if (!selfClosing && ("dict".equals(name) || "array".equals(name))) {
                depth++;
            }
            at = close + 1;
        }
        return keys;
    }

    /** The fragment with every {@code <!-- ... -->} region removed. */
    private static String stripXmlComments(String xml) {
        StringBuilder out = new StringBuilder(xml.length());
        int at = 0;
        while (true) {
            int open = xml.indexOf("<!--", at);
            if (open < 0) {
                out.append(xml.substring(at));
                return out.toString();
            }
            out.append(xml, at, open);
            int close = xml.indexOf("-->", open);
            if (close < 0) {
                // Unterminated: everything after it is comment, which is what a
                // parser would conclude too.
                return out.toString();
            }
            at = close + "-->".length();
        }
    }

    /**
     * Whether a plist-inject value is a raw XML fragment rather than the
     * {@code key=value} shorthand.
     */
    public static boolean isRawPlistFragment(String value) {
        return value != null && value.indexOf("<key>") >= 0;
    }

    public static List<String> mergePlist(Map<String, Object> base, Map<String, Object> inject) {
        List<String> collisions = new ArrayList<String>();
        for (Map.Entry<String, Object> e : inject.entrySet()) {
            if (base.containsKey(e.getKey())) {
                collisions.add(e.getKey());
            }
            base.put(e.getKey(), e.getValue());
        }
        return collisions;
    }

    /**
     * The entitlements for one distribution channel.
     *
     * <p>Kept for the callers that only have the two booleans; the overload
     * below is the one a build uses, because it also honours the documented
     * {@code macos.entitlements.*} family.</p>
     *
     * @param appStore  true for the sandboxed App Store channel, false for Developer ID
     * @param sandboxed whether the sandbox applies
     * @param caps      capabilities the app was detected to use
     * @param loadsExternalCode true when the app dlopens, which Developer ID needs
     *                          library validation relaxed for
     */
    public static Map<String, Object> entitlements(boolean appStore, boolean sandboxed,
            MacOSCapabilities caps, boolean loadsExternalCode) {
        return entitlements(appStore,
                MacOSBuildHints.EntitlementOverrides.defaults(appStore, sandboxed),
                caps, loadsExternalCode);
    }

    /**
     * The entitlements for one distribution channel, honouring the configured
     * overrides.
     *
     * <p>The capability scan decides the device entitlements and an explicit
     * {@code macos.entitlements.device.*} overrides it in both directions. That
     * ordering is what makes the scan a convenience rather than a ceiling: an
     * application that reaches the camera through a cn1lib the scanner cannot
     * see can still ask for the entitlement, and one that links the camera API
     * without using it can decline the permission prompt.</p>
     *
     * @param appStore  true for the App Store channel, false for Developer ID
     * @param overrides the resolved {@code macos.entitlements.*} settings
     * @param caps      capabilities the app was detected to use
     * @param loadsExternalCode true when the app dlopens, which Developer ID needs
     *                          library validation relaxed for
     */
    public static Map<String, Object> entitlements(boolean appStore,
            MacOSBuildHints.EntitlementOverrides overrides, MacOSCapabilities caps,
            boolean loadsExternalCode) {
        Map<String, Object> ent = new LinkedHashMap<String, Object>();
        MacOSCapabilities c = caps == null ? new MacOSCapabilities() : caps;
        if (overrides.isSandbox()) {
            ent.put(ENT_SANDBOX, Boolean.TRUE);
            // Outbound networking and user-chosen files are what nearly every app
            // needs and neither can be requested later at runtime, so a sandboxed
            // build without them is one that fails the first time it opens a
            // socket or a file dialog.
            if (overrides.isNetworkClient()) {
                ent.put(ENT_NETWORK_CLIENT, Boolean.TRUE);
            }
            if ("readwrite".equals(overrides.getFilesUserSelected())) {
                ent.put(ENT_FILES_USER_SELECTED, Boolean.TRUE);
            } else if ("readonly".equals(overrides.getFilesUserSelected())) {
                ent.put(ENT_FILES_USER_SELECTED_RO, Boolean.TRUE);
            }
            // The Downloads folder is a separate capability from the files the
            // user picks in a panel, and it is not what files.userSelected
            // describes -- it is access to a directory with no panel at all. It
            // used to be granted alongside readwrite, which quietly widened
            // every sandboxed build's filesystem authority beyond the hint that
            // was supposed to govern it. Its own opt-in now, off by default.
            //
            // Mac Catalyst still grants it with readwrite. The divergence is
            // deliberate: matching it would mean keeping the wider grant.
            if (overrides.filesDownloads(false)) {
                ent.put(ENT_FILES_DOWNLOADS, Boolean.TRUE);
            }
            if (overrides.networkServer(c.usesServerSockets)) {
                ent.put(ENT_NETWORK_SERVER, Boolean.TRUE);
            }
            if (overrides.camera(c.usesCamera)) {
                ent.put(ENT_CAMERA, Boolean.TRUE);
            }
            if (overrides.microphone(c.usesMicrophone)) {
                ent.put(ENT_MICROPHONE, Boolean.TRUE);
            }
            if (overrides.bluetooth(c.usesBluetooth)) {
                ent.put(ENT_BLUETOOTH, Boolean.TRUE);
            }
            if (overrides.location(c.usesLocation)) {
                ent.put(ENT_LOCATION, Boolean.TRUE);
            }
            if (overrides.calendars(c.usesCalendar)) {
                ent.put(ENT_CALENDARS, Boolean.TRUE);
            }
        }
        // Outside the sandbox block for the same reason as the JIT exception
        // below: APNs is not a sandbox permission, and a Developer ID build --
        // hardened, unsandboxed -- needs it just as much as an App Store one.
        // "production" for both, because the development environment is for a
        // locally Xcode-signed build and neither channel here is that; a project
        // that genuinely wants the sandbox environment sets
        // macos.entitlements.apsEnvironment and gets it through the extra hint.
        if (overrides.push(c.usesPush)) {
            // The environment from the hint rather than a constant: a mac-source
            // project signed in Xcode with an Apple Development profile needs
            // "development", and production does not match that profile -- which
            // fails registration and signing both, with nothing to say why.
            ent.put(ENT_APS_ENVIRONMENT, overrides.getApsEnvironment());
        }
        // JIT is a hardened-runtime exception, not a sandbox one, so it is
        // outside the block above: a Developer ID build is hardened and not
        // sandboxed, and that is exactly the build that needs it.
        if (overrides.isAllowJit()) {
            ent.put(ENT_ALLOW_JIT, Boolean.TRUE);
            ent.put(ENT_ALLOW_UNSIGNED_MEMORY, Boolean.TRUE);
        } else if (overrides.isHardenedRuntime()) {
            // The explicit denial the Mac Catalyst target has always written for
            // macNative.entitlements.hardenedRuntime. Reading the resolved value
            // was missing here, which left that hint -- under either spelling --
            // with no effect at all on this target.
            //
            // Note this hint is NOT the ENABLE_HARDENED_RUNTIME build setting,
            // however much the names suggest it. On Catalyst it decides these
            // two JIT keys and nothing else, and driving the build setting from
            // it instead would give one hint name two different meanings across
            // the two Mac targets. macos.hardenedRuntime is the build setting.
            ent.put(ENT_ALLOW_JIT, Boolean.FALSE);
            ent.put(ENT_ALLOW_UNSIGNED_MEMORY, Boolean.FALSE);
        }
        if (!appStore && loadsExternalCode) {
            // Only when the app actually loads code it did not ship. Adding it
            // unconditionally weakens the hardened runtime for every app to buy
            // nothing: ParparVM output is ahead-of-time compiled and does not
            // need it.
            ent.put(ENT_DISABLE_LIBRARY_VALIDATION, Boolean.TRUE);
        }
        return ent;
    }

    /**
     * The privacy usage descriptions this application needs in its Info.plist.
     *
     * <p>Keyed off the same capability scan that decides the entitlements,
     * because the two travel together: an entitlement grants the right to ask,
     * and the usage description is the sentence macOS shows when asking. A
     * bundle with the entitlement and no description does not prompt -- the
     * process is killed the moment it touches the API, with nothing in the
     * application's own logs to say why.</p>
     *
     * @param resolver supplies the application's own string for a key, or null
     */
    public static Map<String, Object> privacyUsageDescriptions(MacOSCapabilities caps,
            UsageDescriptionResolver resolver) {
        return privacyUsageDescriptions(caps, false, resolver);
    }

    /**
     * The privacy usage descriptions this application needs in its Info.plist.
     *
     * @param calendars true when the calendars entitlement was granted, which
     *                  needs its own descriptions -- the entitlement grants the
     *                  right to ask and the description is what macOS shows when
     *                  asking, so an entitlement without one is a capability the
     *                  process is killed for using
     */
    public static Map<String, Object> privacyUsageDescriptions(MacOSCapabilities caps,
            boolean calendars, UsageDescriptionResolver resolver) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (calendars) {
            put(out, resolver, USAGE_CALENDARS,
                    "This app reads and updates your calendar at your request.");
            put(out, resolver, USAGE_CALENDARS_WRITE,
                    "This app adds events to your calendar at your request.");
            put(out, resolver, USAGE_REMINDERS,
                    "This app reads and updates your reminders at your request.");
            // And the pre-14 keys, which macOS 11 through 13 are the only ones
            // that read -- and the deployment floor is 11.0.
            put(out, resolver, USAGE_CALENDARS_LEGACY,
                    "This app reads and updates your calendar at your request.");
            put(out, resolver, USAGE_REMINDERS_LEGACY,
                    "This app reads and updates your reminders at your request.");
        }
        if (caps == null) {
            return out;
        }
        if (caps.usesCamera) {
            put(out, resolver, USAGE_CAMERA,
                    "This app uses the camera at your request.");
        }
        if (caps.usesMicrophone) {
            put(out, resolver, USAGE_MICROPHONE,
                    "This app uses the microphone at your request.");
        }
        if (caps.usesBluetooth) {
            put(out, resolver, USAGE_BLUETOOTH,
                    "This app uses Bluetooth to communicate with nearby devices.");
        }
        if (caps.usesLocation) {
            put(out, resolver, USAGE_LOCATION,
                    "This app uses your location at your request.");
        }
        return out;
    }

    private static void put(Map<String, Object> out, UsageDescriptionResolver resolver,
            String key, String fallback) {
        String supplied = resolver == null ? null : resolver.get(key);
        out.put(key, supplied != null && supplied.length() > 0 ? supplied : fallback);
    }

    /** Supplies the application's own usage-description string for a key, or null. */
    public interface UsageDescriptionResolver {
        String get(String key);
    }

    /** What the app was detected to use, which decides the sandbox entitlements. */
    public static class MacOSCapabilities {
        public boolean usesCamera;
        public boolean usesMicrophone;
        public boolean usesBluetooth;
        public boolean usesLocation;
        public boolean usesServerSockets;
        /// Set when the class scan finds a local-calendar entry point. The scan
        /// is what enables EventKit, so the entitlement and the usage strings
        /// have to follow it -- otherwise a sandboxed build links the framework
        /// and is refused access to it.
        public boolean usesCalendar;
        /// Set when the class scan finds a push registration entry point.
        ///
        /// This one is not about the sandbox. macOS refuses
        /// registerForRemoteNotifications for a signed executable that carries no
        /// aps-environment entitlement, and this build supplies its entitlements
        /// as an explicit file -- so an entitlement absent from that file is
        /// absent from the signature, whatever the provisioning profile grants.
        /// Without it the AppKit backend registers, is refused, and no token ever
        /// reaches PushClient.
        public boolean usesPush;
    }

    /** The ExportOptions plist for one channel. */
    public static Map<String, Object> exportOptions(boolean appStore, String teamId,
            String signingStyle) {
        Map<String, Object> opts = new LinkedHashMap<String, Object>();
        opts.put("method", appStore ? "app-store" : "developer-id");
        if (teamId != null && teamId.length() > 0) {
            opts.put("teamID", teamId);
        }
        opts.put("signingStyle", signingStyle != null && signingStyle.length() > 0
                ? signingStyle : "manual");
        opts.put("destination", "export");
        return opts;
    }

    /** Serializes a plist map to XML and writes it. */
    public static void writePlist(Map<String, Object> plist, File dest) throws IOException {
        writePlist(plist, null, dest);
    }

    /**
     * Serializes a plist map, then appends a raw XML fragment inside the dict.
     *
     * <p>The fragment is what {@code macos.entitlements.extra} carries: key/value
     * pairs for entitlements this class does not model one at a time -- app
     * groups, iCloud containers, a capability Apple added last week. It is
     * written verbatim and deliberately not parsed, which is the only way to
     * keep the escape hatch open; a malformed fragment produces a plist codesign
     * rejects by name, which is a legible failure.</p>
     */
    public static void writePlist(Map<String, Object> plist, String extraXml, File dest)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        try {
            Writer w = new OutputStreamWriter(fos, "UTF-8");
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            w.write("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                    + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
            w.write("<plist version=\"1.0\">\n<dict>\n");
            for (Map.Entry<String, Object> e : plist.entrySet()) {
                w.write("\t<key>" + escape(e.getKey()) + "</key>\n");
                writeValue(w, e.getValue(), "\t");
            }
            if (extraXml != null && extraXml.trim().length() > 0) {
                w.write(extraXml);
                if (!extraXml.endsWith("\n")) {
                    w.write("\n");
                }
            }
            w.write("</dict>\n</plist>\n");
            w.flush();
        } finally {
            fos.close();
        }
    }

    private static void writeValue(Writer w, Object value, String indent) throws IOException {
        if (value instanceof Boolean) {
            w.write(indent + (((Boolean) value).booleanValue() ? "<true/>" : "<false/>") + "\n");
        } else if (value instanceof Integer || value instanceof Long) {
            w.write(indent + "<integer>" + value + "</integer>\n");
        } else if (value instanceof List) {
            w.write(indent + "<array>\n");
            for (Object o : (List<?>) value) {
                writeValue(w, o, indent + "\t");
            }
            w.write(indent + "</array>\n");
        } else if (value instanceof Map) {
            w.write(indent + "<dict>\n");
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                w.write(indent + "\t<key>" + escape(String.valueOf(e.getKey())) + "</key>\n");
                writeValue(w, e.getValue(), indent + "\t");
            }
            w.write(indent + "</dict>\n");
        } else {
            w.write(indent + "<string>" + escape(String.valueOf(value)) + "</string>\n");
        }
    }

    static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Parses a {@code WxH} fixed window size, or returns null.
     *
     * <p>Only the screenshot suite sets this. A window the app cannot resize is
     * what makes a byte-exact golden comparison mean anything.</p>
     */
    public static int[] parseFixedWindowSize(String value) {
        if (value == null) {
            return null;
        }
        int x = value.indexOf('x');
        if (x < 0) {
            x = value.indexOf('X');
        }
        if (x <= 0 || x == value.length() - 1) {
            return null;
        }
        try {
            int w = Integer.parseInt(value.substring(0, x).trim());
            int h = Integer.parseInt(value.substring(x + 1).trim());
            if (w <= 0 || h <= 0) {
                return null;
            }
            return new int[] {w, h};
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
