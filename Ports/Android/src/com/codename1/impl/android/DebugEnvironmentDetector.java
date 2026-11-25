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
package com.codename1.impl.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/// Local device-posture signals that complement RootBeer and the Frida check: an attached
/// debugger, Developer Options, USB debugging, Xposed-family method hooking and the traces a
/// root tool (Magisk, KernelSU, APatch) leaves when it is installed without hiding itself.
///
/// What these checks do NOT see, so nobody reads more into them: Zygisk-based LSPosed maps its
/// bridge from anonymous memory under obfuscated names, and Magisk's DenyList or Shamiko removes
/// the root mounts from the app's namespace. Catching those is attestation's job -- a
/// strong-integrity Play Integrity verdict verified on the server.
///
/// Every check here is a best-effort local signal and can be defeated by an attacker who
/// controls the device. The control that holds is server-verified attestation (Play Integrity,
/// App Shield); these exist so a compromised environment is visible to the app and so the
/// launch gates have something to act on. No method throws: a check that cannot run answers
/// "not detected".
///
/// Used by `AndroidImplementation.getCompromiseReasons()` and by the launch gates the builder
/// writes into the generated activity (`android.debugGuard`, `android.rootCheck`,
/// `android.fridaDetection`).
public final class DebugEnvironmentDetector {

    private static final String TAG = "Codename One";

    /// Classes only present when an Xposed-family framework is loaded into the process.
    private static final String[] HOOK_CLASSES = {
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers"
    };

    /// Prefixes of the FILE NAME (never the directory) of a `/proc/self/maps` entry that belong
    /// to a hooking framework, lower case. File names only, because the directory of every
    /// installed APK carries 22 random base64 characters (`/data/app/~~...==/pkg-...==/`), and a
    /// short needle matched against the whole line hits those on a measurable share of stock
    /// devices -- which would kill a legitimate app at launch until it is reinstalled.
    private static final String[] HOOK_FILES = {
            "xposedbridge",
            "liblspd",
            "liblspatch",
            "libxposed_art",
            "libedxp",
            "libsandhook",
            "libwhale",
            "libriru",
            "libsubstrate"
    };

    /// Manager apps of root/root-hiding tools. Android 11+ hides packages the manifest does not
    /// declare in `<queries>`, and Magisk can repackage itself under a random name, so a miss
    /// here means nothing.
    private static final String[] ROOT_PACKAGES = {
            "com.topjohnwu.magisk",
            "io.github.vvb2060.magisk",
            "io.github.huskydg.magisk",
            "me.weishu.kernelsu",
            "com.rifsxd.ksunext",
            "me.bmax.apatch"
    };

    /// Paths root tools create. Most are unreadable to an app under SELinux on a stock device,
    /// which is exactly when they do not exist either.
    private static final String[] ROOT_PATHS = {
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/ksu",
            "/data/adb/ap",
            "/data/adb/modules",
            "/cache/.disable_magisk",
            "/dev/.magisk.unblock"
    };

    /// Mount sources (the first field of a `/proc/self/mounts` line) that root tools use for
    /// their overlays, compared whole. A whole-field match, not a substring, so an app or vendor
    /// mount whose name merely contains one of these is never reported.
    private static final String[] ROOT_MOUNT_SOURCES = {
            "magisk",
            "KSU",
            "APatch"
    };

    /// Prefixes of a mount point or mount source path that only root tools create.
    private static final String[] ROOT_MOUNT_PATHS = {
            "/data/adb/",
            "/debug_ramdisk/",
            "/sbin/.magisk/"
    };

    /// Mount points root tools mount ON. Not `/debug_ramdisk`, where Magisk also mounts: first
    /// stage init mounts a tmpfs there on every device and only unmounts it later, and an OEM
    /// build that leaves it mounted would make android.rootCheck kill every launch. Magisk's
    /// own mount there is still caught by its `magisk` mount source.
    private static final String[] ROOT_MOUNT_POINTS = {
            "/sbin/.magisk"
    };

    private DebugEnvironmentDetector() {
    }

    /// True when a Java (JDWP) debugger is connected or awaited, or a native tracer (gdb,
    /// lldb, strace, a ptrace-based instrumentation tool) is attached to this process.
    public static boolean isDebuggerAttached() {
        try {
            if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
                return true;
            }
        } catch (Throwable t) {
            // fall through to the native check
        }
        int tracer = getTracerPid();
        return tracer > 0 && !isOwnTracer(tracer);
    }

    /// True when the tracer runs as this app's own uid: the anti-debug child a commercial RASP
    /// or packer forks to ptrace its parent (which also keeps a real debugger out). A debugger
    /// attached from outside runs as shell (adb) or root; one running as the app's uid needs
    /// `run-as`, which only works on a debuggable build, where the guard only logs anyway.
    /// Counting the RASP's own child would kill every launch of an app that ships one. A root
    /// attacker can run a tracer under the app's uid and pass this; root is the other checks'
    /// and attestation's to catch.
    private static boolean isOwnTracer(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File("/proc/" + pid + "/status")), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Uid:")) {
                    String[] ids = line.substring("Uid:".length()).trim().split("\\s+");
                    return ids.length > 0 && Integer.parseInt(ids[0]) == android.os.Process.myUid();
                }
            }
        } catch (Throwable t) {
            // another uid's /proc entry is often unreadable: treat the tracer as foreign
            return false;
        } finally {
            close(reader);
        }
        return false;
    }

    /// The `TracerPid` from `/proc/self/status`: the pid of the process ptrace-attached to
    /// this one, 0 when none, -1 when it could not be read.
    static int getTracerPid() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File("/proc/self/status")), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("TracerPid:")) {
                    return Integer.parseInt(line.substring("TracerPid:".length()).trim());
                }
            }
        } catch (Throwable t) {
            // unreadable on this device: report "unknown", never "attached"
        } finally {
            close(reader);
        }
        return -1;
    }

    /// True when the device has Developer Options switched on.
    public static boolean isDeveloperOptionsEnabled(Context context) {
        return globalSettingOn(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED);
    }

    /// True when USB debugging (ADB) is switched on.
    public static boolean isUsbDebuggingEnabled(Context context) {
        return globalSettingOn(context, Settings.Global.ADB_ENABLED);
    }

    private static boolean globalSettingOn(Context context, String name) {
        if (context == null) {
            return false;
        }
        try {
            return Settings.Global.getInt(context.getContentResolver(), name, 0) != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /// True when an Xposed-family method-hooking framework (Xposed, EdXposed, LSPosed,
    /// LSPatch) or a known native hooking library is loaded into this process.
    public static boolean isHookFrameworkDetected() {
        for (int i = 0; i < HOOK_CLASSES.length; i++) {
            if (classVisible(HOOK_CLASSES[i])) {
                Log.e(TAG, "Hooking framework class present: " + HOOK_CLASSES[i]);
                return true;
            }
        }
        // A hooked call has the framework's bridge on its stack. Taken from a fresh throwable so
        // the stack is this call's own.
        try {
            StackTraceElement[] stack = new Throwable().getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                String cls = stack[i].getClassName();
                // org.lsposed.lspd, not org.lsposed: hiddenapibypass and lsparanoid are
                // ordinary libraries an app may bundle.
                if (cls != null && (cls.startsWith("de.robv.android.xposed")
                        || cls.startsWith("org.lsposed.lspd"))) {
                    Log.e(TAG, "Hooking framework on the call stack: " + cls);
                    return true;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        String hit = findMappedFile(HOOK_FILES);
        if (hit != null) {
            Log.e(TAG, "Hooking library mapped: " + hit);
            return true;
        }
        return false;
    }

    /// True when a root tool (Magisk, KernelSU, APatch) has left a trace RootBeer does not look
    /// for: its manager app (visible from API 30 only when the manifest declares it in
    /// `<queries>`, which `android.rootCheck` does), its overlay mounts, or its paths. A tool
    /// that hides itself from this app (DenyList, Shamiko) is not seen; see the class comment.
    public static boolean isRootHidingDetected(Context context) {
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                for (int i = 0; i < ROOT_PACKAGES.length; i++) {
                    try {
                        pm.getPackageInfo(ROOT_PACKAGES[i], 0);
                        Log.e(TAG, "Root manager installed: " + ROOT_PACKAGES[i]);
                        return true;
                    } catch (PackageManager.NameNotFoundException notInstalled) {
                        // expected
                    }
                }
            } catch (Throwable t) {
                // ignore
            }
        }
        for (int i = 0; i < ROOT_PATHS.length; i++) {
            try {
                if (new File(ROOT_PATHS[i]).exists()) {
                    Log.e(TAG, "Root tool path present: " + ROOT_PATHS[i]);
                    return true;
                }
            } catch (Throwable t) {
                // ignore
            }
        }
        String hit = findRootMount();
        if (hit != null) {
            Log.e(TAG, "Root tool mount: " + hit);
            return true;
        }
        return false;
    }

    /// The launch gate written by `android.debugGuard`. `checks` is a comma separated subset of
    /// `debugger`, `developerOptions` and `usbDebugging` (the builders validate it and default it
    /// to `debugger`); `exit` terminates the process when any selected check trips, otherwise the
    /// findings are only logged. A debuggable build only logs, so a developer can still run and
    /// debug their own app from the IDE. Never throws.
    public static void runDebugGuard(Context context, String checks, boolean exit) {
        try {
            StringBuilder found = new StringBuilder();
            if (selected(checks, "debugger") && isDebuggerAttached()) {
                found.append("debugger ");
            }
            if (selected(checks, "developerOptions") && isDeveloperOptionsEnabled(context)) {
                found.append("developerOptions ");
            }
            if (selected(checks, "usbDebugging") && isUsbDebuggingEnabled(context)) {
                found.append("usbDebugging ");
            }
            if (found.length() == 0) {
                return;
            }
            boolean terminate = exit && !isDebuggableBuild(context);
            Log.e(TAG, "Debug guard detected: " + found.toString().trim()
                    + (terminate ? ". Exiting app." : ""));
            if (terminate) {
                System.exit(0);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Debug guard error", t);
        }
    }

    private static boolean isDebuggableBuild(Context context) {
        try {
            return context != null && (context.getApplicationInfo().flags
                    & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /// True when `name` is one of the comma separated `checks`. An empty or null list selects
    /// nothing: the builders always pass an explicit, validated list.
    static boolean selected(String checks, String name) {
        if (checks == null) {
            return false;
        }
        String[] parts = checks.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean classVisible(String name) {
        try {
            Class.forName(name, false, ClassLoader.getSystemClassLoader());
            return true;
        } catch (Throwable t) {
            // not on the system loader
        }
        try {
            Class.forName(name, false, DebugEnvironmentDetector.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /// Returns the path of the first `/proc/self/maps` entry whose file name starts with one of
    /// `prefixes` (lower case), or null when none does or the file is unreadable.
    private static String findMappedFile(String[] prefixes) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File("/proc/self/maps")), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                int slash = line.lastIndexOf('/');
                if (slash < 0) {
                    continue;
                }
                String file = asciiLower(line.substring(slash + 1));
                for (int i = 0; i < prefixes.length; i++) {
                    if (file.startsWith(prefixes[i])) {
                        return line.substring(line.indexOf('/'));
                    }
                }
            }
        } catch (Throwable t) {
            return null;
        } finally {
            close(reader);
        }
        return null;
    }

    /// Returns the first `/proc/self/mounts` line a root tool created, or null.
    private static String findRootMount() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File("/proc/self/mounts")), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(" ");
                if (fields.length < 2) {
                    continue;
                }
                for (int i = 0; i < ROOT_MOUNT_SOURCES.length; i++) {
                    if (fields[0].equals(ROOT_MOUNT_SOURCES[i])) {
                        return line;
                    }
                }
                for (int i = 0; i < ROOT_MOUNT_POINTS.length; i++) {
                    if (fields[1].equals(ROOT_MOUNT_POINTS[i])) {
                        return line;
                    }
                }
                for (int i = 0; i < ROOT_MOUNT_PATHS.length; i++) {
                    if (fields[0].startsWith(ROOT_MOUNT_PATHS[i]) || fields[1].startsWith(ROOT_MOUNT_PATHS[i])) {
                        return line;
                    }
                }
            }
        } catch (Throwable t) {
            return null;
        } finally {
            close(reader);
        }
        return null;
    }

    /// Locale-independent lower case: a Turkish locale would fold `I` to a dotless i.
    private static String asciiLower(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return b.toString();
    }

    private static void close(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
