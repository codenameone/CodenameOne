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
     * @param appStore  true for the sandboxed App Store channel, false for Developer ID
     * @param sandboxed whether the sandbox applies
     * @param caps      capabilities the app was detected to use
     * @param loadsExternalCode true when the app dlopens, which Developer ID needs
     *                          library validation relaxed for
     */
    public static Map<String, Object> entitlements(boolean appStore, boolean sandboxed,
            MacOSCapabilities caps, boolean loadsExternalCode) {
        Map<String, Object> ent = new LinkedHashMap<String, Object>();
        if (sandboxed) {
            ent.put(ENT_SANDBOX, Boolean.TRUE);
            // Outbound networking and user-chosen files are what nearly every app
            // needs and neither can be requested later at runtime, so a sandboxed
            // build without them is one that fails the first time it opens a
            // socket or a file dialog.
            ent.put(ENT_NETWORK_CLIENT, Boolean.TRUE);
            ent.put(ENT_FILES_USER_SELECTED, Boolean.TRUE);
            if (caps != null) {
                if (caps.usesServerSockets) {
                    ent.put(ENT_NETWORK_SERVER, Boolean.TRUE);
                }
                if (caps.usesCamera) {
                    ent.put(ENT_CAMERA, Boolean.TRUE);
                }
                if (caps.usesMicrophone) {
                    ent.put(ENT_MICROPHONE, Boolean.TRUE);
                }
                if (caps.usesBluetooth) {
                    ent.put(ENT_BLUETOOTH, Boolean.TRUE);
                }
                if (caps.usesLocation) {
                    ent.put(ENT_LOCATION, Boolean.TRUE);
                }
            }
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
