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
                ent.put(ENT_FILES_DOWNLOADS, Boolean.TRUE);
            } else if ("readonly".equals(overrides.getFilesUserSelected())) {
                ent.put(ENT_FILES_USER_SELECTED_RO, Boolean.TRUE);
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
            if (overrides.calendars(false)) {
                ent.put(ENT_CALENDARS, Boolean.TRUE);
            }
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
