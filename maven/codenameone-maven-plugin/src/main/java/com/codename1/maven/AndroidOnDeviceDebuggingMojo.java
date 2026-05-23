/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Drives an Android on-device debug session over adb.
 *
 * Unlike iOS, Android's runtime already speaks JDWP, so there is no
 * desktop proxy and no custom wire format. This Mojo just orchestrates
 * the steps a developer would otherwise type by hand:
 *
 *   1. (optional) {@code adb connect <ip:port>} for wireless devices.
 *   2. (optional) {@code adb install -r <apk>} when an APK is found in
 *      the build output or passed via -Dapk.
 *   3. {@code adb shell am set-debug-app -w --persistent <package>} when
 *      waitForAttach is true, so the app blocks at boot for the IDE.
 *   4. {@code adb shell am start -n <package>/<MainClass>Stub} to launch.
 *   5. Polls {@code adb shell pidof <package>} for the running PID.
 *   6. {@code adb forward tcp:<jdwpPort> jdwp:<pid>} exposes the device
 *      JDWP socket on localhost.
 *   7. Streams {@code adb logcat --pid=<pid>} to this console with a
 *      {@code [device]} prefix.
 *
 * The Mojo blocks until interrupted (Ctrl-C) or the device process exits.
 *
 * Properties:
 *   -Dcn1.android.onDeviceDebug.adb=path/to/adb            adb override
 *   -Dcn1.android.onDeviceDebug.deviceSerial=<serial>      target one device
 *   -Dcn1.android.onDeviceDebug.wireless=192.168.1.5:5555  adb connect first
 *   -Dcn1.android.onDeviceDebug.jdwpPort=5005              local forward port
 *   -Dcn1.android.onDeviceDebug.apk=path/to/app.apk        APK override
 *   -Dcn1.android.onDeviceDebug.packageName=...            override package
 *   -Dcn1.android.onDeviceDebug.mainClass=...              override main class
 *   -Dcn1.android.onDeviceDebug.waitForAttach=true|false   default true
 *   -Dcn1.android.onDeviceDebug.skipInstall=true           don't reinstall APK
 */
@Mojo(name = "android-on-device-debugging")
public class AndroidOnDeviceDebuggingMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.android.onDeviceDebug.adb")
    private String adbPath;

    @Parameter(property = "cn1.android.onDeviceDebug.deviceSerial")
    private String deviceSerial;

    @Parameter(property = "cn1.android.onDeviceDebug.wireless")
    private String wireless;

    @Parameter(property = "cn1.android.onDeviceDebug.jdwpPort", defaultValue = "5005")
    private int jdwpPort;

    @Parameter(property = "cn1.android.onDeviceDebug.apk")
    private String apkPath;

    @Parameter(property = "cn1.android.onDeviceDebug.packageName")
    private String packageNameOverride;

    @Parameter(property = "cn1.android.onDeviceDebug.mainClass")
    private String mainClassOverride;

    @Parameter(property = "cn1.android.onDeviceDebug.waitForAttach", defaultValue = "true")
    private boolean waitForAttach;

    @Parameter(property = "cn1.android.onDeviceDebug.skipInstall", defaultValue = "false")
    private boolean skipInstall;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File commonDir = getCN1ProjectDir();
        if (commonDir == null) {
            throw new MojoFailureException("Could not locate Codename One project root");
        }
        File rootProjectDir = commonDir.getParentFile();
        if (rootProjectDir == null) rootProjectDir = commonDir;

        Properties cn1Settings = readCn1Settings(commonDir);
        String packageName = firstNonEmpty(packageNameOverride,
                cn1Settings.getProperty("codename1.packageName"));
        String mainName = firstNonEmpty(mainClassOverride,
                cn1Settings.getProperty("codename1.mainName"));
        if (packageName == null || mainName == null) {
            throw new MojoFailureException("Could not resolve package / main class from "
                    + new File(commonDir, "codenameone_settings.properties"));
        }
        String launcherActivity = packageName + "." + mainName + "Stub";

        File adb = resolveAdb();
        getLog().info("Using adb: " + adb);

        if (wireless != null && !wireless.isEmpty()) {
            getLog().info("adb connect " + wireless);
            runAdb(adb, null, "connect", wireless);
        }

        String serial = resolveDevice(adb);
        getLog().info("Target device: " + serial);

        if (!skipInstall) {
            File apk = resolveApk(rootProjectDir);
            if (apk != null) {
                getLog().info("Installing " + apk.getName() + " on " + serial);
                CommandResult install = runAdb(adb, serial, "install", "-r", "-t", apk.getAbsolutePath());
                if (install.exitCode != 0) {
                    throw new MojoFailureException("adb install failed:\n" + install.stdout);
                }
            } else {
                getLog().warn("No APK found under target/ — skipping install. "
                        + "Pass -Dcn1.android.onDeviceDebug.apk=<path> or run "
                        + "'mvn cn1:buildAndroidOnDeviceDebug' first.");
            }
        }

        if (waitForAttach) {
            getLog().info("Marking " + packageName + " as the debug app (waits for debugger).");
            runAdb(adb, serial, "shell", "am", "set-debug-app", "-w", "--persistent", packageName);
        } else {
            // Clear any stale debug-app flag from a previous run.
            runAdb(adb, serial, "shell", "am", "clear-debug-app");
        }

        getLog().info("Launching " + launcherActivity);
        CommandResult launch = runAdb(adb, serial, "shell", "am", "start", "-n",
                packageName + "/" + launcherActivity);
        if (launch.exitCode != 0) {
            throw new MojoFailureException("adb am start failed:\n" + launch.stdout);
        }

        String pid = waitForPid(adb, serial, packageName);
        if (pid == null) {
            throw new MojoFailureException("Timed out waiting for process " + packageName
                    + " on device " + serial + ". Did the app crash on launch?");
        }
        getLog().info("App PID on device: " + pid);

        // Map the device JDWP socket onto a local TCP port.
        runAdb(adb, serial, "forward", "tcp:" + jdwpPort, "jdwp:" + pid);

        getLog().info("");
        getLog().info("==================================================================");
        getLog().info(" JDWP forwarded:  localhost:" + jdwpPort + "  ->  device pid " + pid);
        getLog().info(" Attach IntelliJ: Run -> 'CN1 Attach Android' (Remote JVM Debug)");
        getLog().info(" Or from a shell: jdb -attach localhost:" + jdwpPort);
        if (waitForAttach) {
            getLog().info("");
            getLog().info(" The app is paused in waitForDebugger() — attach now to resume.");
        }
        getLog().info("==================================================================");
        getLog().info("");

        // Stream logcat for the lifetime of the session. Logcat tee'd to our
        // own stdout so it lands in the IDE's Run tool window alongside the
        // [adb] lines above. --pid filters out unrelated chatter.
        ProcessBuilder logcat = new ProcessBuilder(
                buildAdbCommand(adb, serial, "logcat", "-v", "threadtime", "--pid=" + pid));
        logcat.redirectErrorStream(true);
        Process logcatProc;
        try {
            logcatProc = logcat.start();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to start adb logcat: " + e.getMessage(), e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logcatProc.destroy();
                // Best-effort cleanup so a re-run doesn't trip over a stale forward.
                runAdb(adb, serial, "forward", "--remove", "tcp:" + jdwpPort);
            } catch (Exception ignored) {
            }
        }));

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(logcatProc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                getLog().info("[device] " + line);
            }
        } catch (IOException e) {
            getLog().warn("logcat stream interrupted: " + e.getMessage());
        }

        try {
            int rc = logcatProc.waitFor();
            if (rc != 0) {
                getLog().warn("logcat exited with code " + rc);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Properties readCn1Settings(File commonDir) throws MojoFailureException {
        File f = new File(commonDir, "codenameone_settings.properties");
        if (!f.isFile()) {
            throw new MojoFailureException("Missing " + f);
        }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to read " + f + ": " + e.getMessage());
        }
        return p;
    }

    private File resolveAdb() throws MojoFailureException {
        if (adbPath != null && !adbPath.isEmpty()) {
            File f = new File(adbPath);
            if (!f.isFile()) throw new MojoFailureException("Configured adb not found: " + f);
            return f;
        }
        List<String> candidates = new ArrayList<>();
        String androidHome = System.getenv("ANDROID_HOME");
        String androidSdkRoot = System.getenv("ANDROID_SDK_ROOT");
        if (androidHome != null) candidates.add(androidHome + "/platform-tools/adb");
        if (androidSdkRoot != null) candidates.add(androidSdkRoot + "/platform-tools/adb");
        String home = System.getProperty("user.home");
        if (home != null) {
            // macOS, Linux, and Windows default Android Studio SDK locations.
            candidates.add(home + "/Library/Android/sdk/platform-tools/adb");
            candidates.add(home + "/Android/Sdk/platform-tools/adb");
            candidates.add(home + "/AppData/Local/Android/Sdk/platform-tools/adb.exe");
        }
        for (String c : candidates) {
            File f = new File(c);
            if (f.isFile()) return f;
        }
        // PATH lookup as a last resort.
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String exe = isWindows() ? "adb.exe" : "adb";
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File f = new File(dir, exe);
                if (f.isFile()) return f;
            }
        }
        throw new MojoFailureException(
                "Could not locate the Android adb executable. Install the Android SDK "
                        + "platform-tools (or Android Studio) and set ANDROID_HOME, or pass "
                        + "-Dcn1.android.onDeviceDebug.adb=<path>.");
    }

    private String resolveDevice(File adb) throws MojoFailureException {
        if (deviceSerial != null && !deviceSerial.isEmpty()) return deviceSerial;
        CommandResult res = runAdb(adb, null, "devices");
        // adb devices output: header line then `<serial>\t<state>` per device.
        List<String> online = new ArrayList<>();
        for (String line : res.stdout.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("List of devices")) continue;
            String[] parts = line.split("\\s+");
            if (parts.length >= 2 && "device".equals(parts[1])) {
                online.add(parts[0]);
            }
        }
        if (online.isEmpty()) {
            throw new MojoFailureException(
                    "No Android device is online. Plug a device in with USB debugging enabled, "
                            + "or pass -Dcn1.android.onDeviceDebug.wireless=<ip:port> to dial a "
                            + "wireless device first. (adb devices output: " + res.stdout.trim() + ")");
        }
        if (online.size() > 1) {
            throw new MojoFailureException(
                    "Multiple devices online (" + String.join(", ", online) + "); pick one with "
                            + "-Dcn1.android.onDeviceDebug.deviceSerial=<serial>.");
        }
        return online.get(0);
    }

    private File resolveApk(File rootProjectDir) {
        if (apkPath != null && !apkPath.isEmpty()) {
            File f = new File(apkPath);
            return f.isFile() ? f : null;
        }
        // Locally-built Gradle source path (from `cn1:buildAndroidGradleProject`).
        // Cloud-built APK lands directly in <module>/target/*.apk.
        List<File> roots = new ArrayList<>();
        File androidModule = new File(rootProjectDir, "android");
        if (androidModule.isDirectory()) roots.add(new File(androidModule, "target"));
        roots.add(new File(rootProjectDir, "target"));
        roots.add(rootProjectDir);

        List<File> hits = new ArrayList<>();
        for (File root : roots) {
            if (!root.isDirectory()) continue;
            try (Stream<Path> walk = Files.walk(root.toPath())) {
                walk.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".apk")
                            && !p.toString().contains("/.gradle/")
                            && !p.toString().contains("/intermediates/");
                }).forEach(p -> hits.add(p.toFile()));
            } catch (IOException ignored) {
            }
        }
        if (hits.isEmpty()) return null;
        hits.sort(Comparator.comparingLong(File::lastModified).reversed());
        return hits.get(0);
    }

    private String waitForPid(File adb, String serial, String packageName) throws MojoFailureException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (System.currentTimeMillis() < deadline) {
            CommandResult r = runAdbQuiet(adb, serial, "shell", "pidof", packageName);
            String pid = r.stdout.trim();
            // pidof prints space-separated pids when multiple matches exist; take the first.
            int sp = pid.indexOf(' ');
            if (sp > 0) pid = pid.substring(0, sp);
            if (!pid.isEmpty() && pid.chars().allMatch(Character::isDigit)) {
                return pid;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private List<String> buildAdbCommand(File adb, String serial, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(adb.getAbsolutePath());
        if (serial != null) {
            cmd.add("-s");
            cmd.add(serial);
        }
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    private CommandResult runAdb(File adb, String serial, String... args) throws MojoFailureException {
        CommandResult r = runAdbQuiet(adb, serial, args);
        if (r.exitCode != 0 && getLog().isDebugEnabled()) {
            getLog().debug("adb " + String.join(" ", args) + " exited " + r.exitCode + ": " + r.stdout);
        }
        return r;
    }

    private CommandResult runAdbQuiet(File adb, String serial, String... args) throws MojoFailureException {
        ProcessBuilder pb = new ProcessBuilder(buildAdbCommand(adb, serial, args));
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = p.getInputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            int rc = p.waitFor();
            return new CommandResult(rc, out.toString(StandardCharsets.UTF_8.name()));
        } catch (IOException | InterruptedException e) {
            throw new MojoFailureException("adb invocation failed: " + e.getMessage());
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private static final class CommandResult {
        final int exitCode;
        final String stdout;

        CommandResult(int exitCode, String stdout) {
            this.exitCode = exitCode;
            this.stdout = stdout;
        }
    }
}
