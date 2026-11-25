/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
/// debugger, Developer Options, USB debugging, method-hooking frameworks (Xposed/LSPosed) and
/// the traces root-hiding tools (Magisk, Zygisk, KernelSU, Shamiko) leave behind.
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
            "de.robv.android.xposed.XposedHelpers",
            "org.lsposed.lspd.core.Main",
            "io.github.libxposed.api.XposedInterface"
    };

    /// Fragments of `/proc/self/maps` lines that name a hooking framework's native or dex code.
    private static final String[] HOOK_MAPS = {
            "XposedBridge",
            "liblspd",
            "libxposed",
            "lspatch",
            "edxp",
            "libsandhook",
            "libwhale",
            "libriru",
            "substrate"
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

    /// Fragments of `/proc/self/mounts` and `/proc/self/maps` that name a root tool's overlay
    /// or injected code.
    private static final String[] ROOT_MOUNTS = {
            "magisk",
            "/data/adb",
            "zygisk",
            "kernelsu",
            "shamiko"
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
        return getTracerPid() > 0;
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
                if (cls != null && (cls.startsWith("de.robv.android.xposed")
                        || cls.startsWith("org.lsposed"))) {
                    Log.e(TAG, "Hooking framework on the call stack: " + cls);
                    return true;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        String hit = findInFile("/proc/self/maps", HOOK_MAPS);
        if (hit != null) {
            Log.e(TAG, "Hooking library mapped: " + hit);
            return true;
        }
        return false;
    }

    /// True when a root tool that hides itself from RootBeer-style checks (Magisk with
    /// Zygisk/DenyList or Shamiko, KernelSU, APatch) has left a visible trace.
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
        String hit = findInFile("/proc/self/mounts", ROOT_MOUNTS);
        if (hit == null) {
            hit = findInFile("/proc/self/maps", ROOT_MOUNTS);
        }
        if (hit != null) {
            Log.e(TAG, "Root tool trace in process: " + hit);
            return true;
        }
        return false;
    }

    /// The launch gate written by `android.debugGuard`. `checks` is a comma separated subset of
    /// `debugger`, `developerOptions` and `usbDebugging`; `exit` terminates the process when any
    /// selected check trips, otherwise the finding is only logged. Never throws.
    public static void runDebugGuard(Context context, String checks, boolean exit) {
        try {
            String found = null;
            if (selected(checks, "debugger") && isDebuggerAttached()) {
                found = "debugger";
            } else if (selected(checks, "developerOptions") && isDeveloperOptionsEnabled(context)) {
                found = "developerOptions";
            } else if (selected(checks, "usbDebugging") && isUsbDebuggingEnabled(context)) {
                found = "usbDebugging";
            }
            if (found != null) {
                Log.e(TAG, "Debug guard: " + found + " detected" + (exit ? ". Exiting app." : ""));
                if (exit) {
                    System.exit(0);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Debug guard error", t);
        }
    }

    static boolean selected(String checks, String name) {
        if (checks == null) {
            return true;
        }
        String[] parts = checks.split(",");
        boolean any = false;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.length() == 0) {
                continue;
            }
            any = true;
            if (p.equalsIgnoreCase(name)) {
                return true;
            }
        }
        // an empty list means "all"
        return !any;
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

    /// Returns the first line of `path` containing any of `needles` (case-insensitive ASCII),
    /// or null when none does or the file is unreadable.
    private static String findInFile(String path, String[] needles) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(path)), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < needles.length; i++) {
                    if (containsIgnoreCase(line, needles[i])) {
                        return line.trim();
                    }
                }
            }
        } catch (Throwable t) {
            // unreadable: not detected
        } finally {
            close(reader);
        }
        return null;
    }

    private static boolean containsIgnoreCase(String hay, String needle) {
        int max = hay.length() - needle.length();
        for (int i = 0; i <= max; i++) {
            if (hay.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }
        return false;
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
