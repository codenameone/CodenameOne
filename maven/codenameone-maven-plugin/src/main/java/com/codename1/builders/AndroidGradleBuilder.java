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
package com.codename1.builders;



import static com.codename1.maven.PathUtil.path;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.ZipFile;

import com.codename1.builders.util.JSONParser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

/**
 *
 * @author Shai Almog
 * @author Steve Hannah
 */
public class AndroidGradleBuilder extends Executor {
    private static final float MIN_GRADLE_VERSION=6;
    private static final String gradleDistributionUrl = "https://services.gradle.org/distributions/gradle-6.8.3-bin.zip";
    public static final boolean PREFER_MANAGED_GRADLE=true;

    private File gradleProjectDirectory;

    private boolean playServicesVersionSetInBuildHint = false;

    public File getGradleProjectDirectory() {
        return gradleProjectDirectory;
    }

    private boolean decouplePlayServiceVersions = false;

    public static final String[] ANDROID_PERMISSIONS = new String[]{
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.ACCESS_CHECKIN_PROPERTIES",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_NOTIFICATION_POLICY",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.ACCOUNT_MANAGER",
            "com.android.voicemail.permission.ADD_VOICEMAIL",
            "android.permission.BATTERY_STATS",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.BIND_APPWIDGET",
            "android.permission.BIND_CARRIER_MESSAGING_SERVICE",
            "android.permission.BIND_CARRIER_SERVICES",
            "android.permission.BIND_CHOOSER_TARGET_SERVICE",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_DREAM_SERVICE",
            "android.permission.BIND_INCALL_SERVICE",
            "android.permission.BIND_INPUT_METHOD",
            "android.permission.BIND_MIDI_DEVICE_SERVICE",
            "android.permission.BIND_NFC_SERVICE",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "android.permission.BIND_PRINT_SERVICE",
            "android.permission.BIND_REMOTEVIEWS",
            "android.permission.BIND_TELECOM_CONNECTION_SERVICE",
            "android.permission.BIND_TEXT_SERVICE",
            "android.permission.BIND_TV_INPUT",
            "android.permission.BIND_VOICE_INTERACTION",
            "android.permission.BIND_VPN_SERVICE",
            "android.permission.BIND_WALLPAPER",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_PRIVILEGED",
            "android.permission.BODY_SENSORS",
            "android.permission.BROADCAST_PACKAGE_REMOVED",
            "android.permission.BROADCAST_SMS",
            "android.permission.BROADCAST_STICKY",
            "android.permission.BROADCAST_WAP_PUSH",
            "android.permission.CALL_PHONE",
            "android.permission.CALL_PRIVILEGED",
            "android.permission.CAMERA",
            "android.permission.CAPTURE_AUDIO_OUTPUT",
            "android.permission.CAPTURE_SECURE_VIDEO_OUTPUT",
            "android.permission.CAPTURE_VIDEO_OUTPUT",
            "android.permission.CHANGE_COMPONENT_ENABLED_STATE",
            "android.permission.CHANGE_CONFIGURATION",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.CHANGE_WIFI_MULTICAST_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.CLEAR_APP_CACHE",
            "android.permission.CONTROL_LOCATION_UPDATES",
            "android.permission.DELETE_CACHE_FILES",
            "android.permission.DELETE_PACKAGES",
            "android.permission.DIAGNOSTIC",
            "android.permission.DISABLE_KEYGUARD",
            "android.permission.DUMP",
            "android.permission.EXPAND_STATUS_BAR",
            "android.permission.FACTORY_TEST",
            "android.permission.FLASHLIGHT",
            "android.permission.GET_ACCOUNTS",
            "android.permission.GET_ACCOUNTS_PRIVILEGED",
            "android.permission.GET_PACKAGE_SIZE",
            "android.permission.GET_TASKS",
            "android.permission.GLOBAL_SEARCH",
            "android.permission.INSTALL_LOCATION_PROVIDER",
            "android.permission.INSTALL_PACKAGES",
            "com.android.launcher.permission.INSTALL_SHORTCUT",
            "android.permission.INTERNET",
            "android.permission.KILL_BACKGROUND_PROCESSES",
            "android.permission.LOCATION_HARDWARE",
            "android.permission.MANAGE_DOCUMENTS",
            "android.permission.MASTER_CLEAR",
            "android.permission.MEDIA_CONTENT_CONTROL",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.MODIFY_PHONE_STATE",
            "android.permission.MOUNT_FORMAT_FILESYSTEMS",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS",
            "android.permission.NFC",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.PERSISTENT_ACTIVITY",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.READ_CALENDAR",
            "android.permission.READ_CALL_LOG",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_FRAME_BUFFER",
            "android.permission.READ_INPUT_STATE",
            "android.permission.READ_LOGS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_SMS",
            "android.permission.READ_SYNC_SETTINGS",
            "android.permission.READ_SYNC_STATS",
            "com.android.voicemail.permission.READ_VOICEMAIL",
            "android.permission.REBOOT",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.RECORD_AUDIO",
            "android.permission.REORDER_TASKS",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.RESTART_PACKAGES",
            "android.permission.SEND_RESPOND_VIA_MESSAGE",
            "android.permission.SEND_SMS",
            "com.android.alarm.permission.SET_ALARM",
            "android.permission.SET_ALWAYS_FINISH",
            "android.permission.SET_ANIMATION_SCALE",
            "android.permission.SET_DEBUG_APP",
            "android.permission.SET_PREFERRED_APPLICATIONS",
            "android.permission.SET_PROCESS_LIMIT",
            "android.permission.SET_TIME",
            "android.permission.SET_TIME_ZONE",
            "android.permission.SET_WALLPAPER",
            "android.permission.SET_WALLPAPER_HINTS",
            "android.permission.SIGNAL_PERSISTENT_PROCESSES",
            "android.permission.STATUS_BAR",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.TRANSMIT_IR",
            "com.android.launcher.permission.UNINSTALL_SHORTCUT",
            "android.permission.UPDATE_DEVICE_STATS",
            "android.permission.USE_FINGERPRINT",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_SIP",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.WRITE_APN_SETTINGS",
            "android.permission.WRITE_CALENDAR",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.WRITE_CONTACTS",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.WRITE_GSERVICES",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.WRITE_SETTINGS",
            "android.permission.WRITE_SYNC_SETTINGS",
            "com.android.voicemail.permission.WRITE_VOICEMAIL",
            "android.permission.FOREGROUND_SERVICE"
    };


    /**
     * The Android port sources Jar file.
     */
    private File androidPortSrcJar;

    private String playFlag;

    private boolean capturePermission;
    private boolean vibratePermission;
    private boolean smsPermission;
    private boolean gpsPermission;
    private boolean pushPermission;
    private boolean foregroundServicePermission;
    private boolean contactsReadPermission;
    private boolean contactsWritePermission;
    private boolean addRemoteControlService;
    /**
     * @deprecated for use to build 1.1 version
     */


    private boolean contactsPermission;
    private boolean wakeLock;
    private boolean recordAudio;
    private boolean phonePermission;
    private boolean purchasePermissions;
    private boolean accessNetworkStatePermission;
    private boolean recieveBootCompletedPermission;
    private boolean getAccountsPermission;
    private boolean credentialsPermission;
    private boolean backgroundLocationPermission;

    private boolean accessWifiStatePermissions;
    private boolean browserBookmarksPermissions;
    private boolean launcherPermissions;

    private boolean integrateMoPub = false;

    private static final boolean isMac;

    private String playServicesVersion = "12.0.1";
    private static final Map<String,String> defaultPlayServiceVersions = new HashMap<>();
    static {
        // Defaults obtained from https://developers.google.com/android/guides/setup
        defaultPlayServiceVersions.put("ads", "19.8.0");
        defaultPlayServiceVersions.put("ads-identifier", "18.0.1");
        defaultPlayServiceVersions.put("ads-lite", "21.5.0");
        defaultPlayServiceVersions.put("afs-native", "19.0.3");
        defaultPlayServiceVersions.put("analytics", "18.0.2");
        defaultPlayServiceVersions.put("appindex", "16.1.0");
        defaultPlayServiceVersions.put("appset", "16.0.2");
        defaultPlayServiceVersions.put("auth", "20.4.1");
        defaultPlayServiceVersions.put("auth-api-phone", "18.0.1");
        defaultPlayServiceVersions.put("auth-blockstore", "16.1.0");
        defaultPlayServiceVersions.put("awareness", "19.0.1");
        defaultPlayServiceVersions.put("base", "18.2.0");
        defaultPlayServiceVersions.put("base-testing", "16.0.0");
        defaultPlayServiceVersions.put("basement", "18.1.0");
        defaultPlayServiceVersions.put("cast", "21.2.0");
        defaultPlayServiceVersions.put("cast-framework", "21.2.0");
        defaultPlayServiceVersions.put("code-scanner", "16.0.0-beta3");
        defaultPlayServiceVersions.put("cronet", "18.0.1");
        defaultPlayServiceVersions.put("dtdi", "16.0.0-beta01");
        defaultPlayServiceVersions.put("fido", "19.0.1");
        defaultPlayServiceVersions.put("fitness", "21.1.0");
        defaultPlayServiceVersions.put("games-v2", "17.0.0");
        defaultPlayServiceVersions.put("games-v2-native-c", "17.0.0-beta1");
        defaultPlayServiceVersions.put("games", "23.1.0");
        defaultPlayServiceVersions.put("home", "16.0.0");
        defaultPlayServiceVersions.put("instantapps", "18.0.1");
        defaultPlayServiceVersions.put("location", "21.0.1");
        defaultPlayServiceVersions.put("maps", "18.1.0");
        defaultPlayServiceVersions.put("mlkit-barcode-scanning", "18.1.0");
        defaultPlayServiceVersions.put("mlkit-face-detection", "17.1.0");
        defaultPlayServiceVersions.put("mlkit-image-labeling", "16.0.8");
        defaultPlayServiceVersions.put("mlkit-image-labeling-custom", "16.0.0-beta4");
        defaultPlayServiceVersions.put("mlkit-language-id", "17.0.0");
        defaultPlayServiceVersions.put("mlkit-smart-reply", "16.0.0-beta1");
        defaultPlayServiceVersions.put("mlkit-text-recognition", "18.0.2");
        defaultPlayServiceVersions.put("nearby", "18.4.0");
        defaultPlayServiceVersions.put("oss-licenses", "17.0.0");
        defaultPlayServiceVersions.put("password-complexity", "18.0.1");
        defaultPlayServiceVersions.put("pay", "16.1.0");
        defaultPlayServiceVersions.put("recaptcha", "17.0.1");
        defaultPlayServiceVersions.put("safetynet", "18.0.1");
        defaultPlayServiceVersions.put("tagmanager", "18.0.2");
        defaultPlayServiceVersions.put("tasks", "18.0.2");
        defaultPlayServiceVersions.put("tflite-gpu", "16.1.0");
        defaultPlayServiceVersions.put("tflite-java", "16.0.1");
        defaultPlayServiceVersions.put("tflite-support", "16.0.1");
        defaultPlayServiceVersions.put("threadnetwork", "16.0.0-beta02");
        defaultPlayServiceVersions.put("vision", "20.1.3");
        defaultPlayServiceVersions.put("wallet", "19.1.0");
        defaultPlayServiceVersions.put("wearable", "18.0.0");

        // TODO: See what an appropriate default version is for firebase
        // Setting to 12.0.1 for now only to match the previous google play services default.
        defaultPlayServiceVersions.put("firebase-core", "12.0.1");
        defaultPlayServiceVersions.put("firebase-messaging", "12.0.1");
        defaultPlayServiceVersions.put("gcm", "12.0.1");
    }

    private Map<String,String> playServiceVersions = new HashMap<>();
    private boolean playServicesPlus;
    private boolean playServicesAuth;
    private boolean playServicesBase;
    private boolean playServicesIdentity;
    private boolean playServicesIndexing;
    private boolean playServicesInvite;
    private boolean playServicesAnalytics;
    private boolean playServicesCast;
    private boolean playServicesGcm;
    private boolean playServicesDrive;
    private boolean playServicesFit;
    private boolean playServicesLocation;
    private boolean playServicesMaps;
    private boolean playServicesAds;
    private boolean playServicesVision;
    private boolean playServicesNearBy;
    private boolean playServicesSafetyPanorama;
    private boolean playServicesGames;
    private boolean playServicesSafetyNet;
    private boolean playServicesWallet;
    private boolean playServicesWear;
    private String xPermissions, xQueries;
    private int buildToolsVersionInt;
    private String buildToolsVersion;
    private boolean useAndroidX;
    private boolean migrateToAndroidX;
    private boolean shouldIncludeGoogleImpl;

    static {
        isMac = System.getProperty("os.name").toLowerCase().indexOf("mac") > -1;
    }

    public void setAndroidPortSrcJar(File androidPortSrcJar) {
        this.androidPortSrcJar = androidPortSrcJar;
    }

    public File getAndroidPortSrcJar() {
        return androidPortSrcJar;
    }

    protected String getDeviceIdCode() {
        return "\"\"";
    }

    protected boolean deriveGlobalInstrumentClasspath() {
        return true;
    }

    protected long getTimeoutValue() {
        // limit to 8 minutes
        return 8 * 60 * 60 * 1000;
    }

    private String getGradleVersion(String gradleExe) throws Exception {
        Map<String,String> env = defaultEnvironment;
        env.put("JAVA_HOME", System.getProperty("java.home"));

        String result = execString(new File(System.getProperty("user.dir")), gradleExe, "--version");
        Scanner scanner = new Scanner(result);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            log("Gradle version line: "+line);
            if (line.startsWith("Gradle ")) {
                return line.substring(line.indexOf(" ")+1).trim();
            }
        }
        throw new RuntimeException("Failed to get gradle version for "+gradleExe);
    }

    private int parseVersionStringAsInt(String versionString) {
        if (versionString.indexOf(".") > 0) {
            try {
                return Integer.parseInt(versionString.substring(0, versionString.indexOf(".")).trim());
            } catch (Exception ex) {
                return 0;
            }
        } else {
            try {
                return Integer.parseInt(versionString);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    private static String escape(String str, String chars) {
        if(str == null) {
            return null;
        }
        char[] charArray = chars.toCharArray();
        for (char c : charArray) {
            str = str.replace(String.valueOf(c), "\\" + c);
        }
        return str;
    }

    @Override
    public boolean build(File sourceZip, final BuildRequest request) throws BuildException {
        debug("Request Args: ");
        debug("-----------------");
        for (String arg : request.getArgs()) {
            debug(arg+"="+request.getArg(arg, null));
        }
        debug("-------------------");

        decouplePlayServiceVersions = request.getArg("android.decouplePlayServiceVersions", "false").equals("true");

        String defaultAndroidHome = isMac ? path(System.getProperty("user.home"), "Library", "Android", "sdk")
                : is_windows ? path(System.getProperty("user.home"), "AppData", "Local", "Android", "sdk")
                : path(System.getProperty("user.home"), "Android", "Sdk"); // linux

        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome == null) {
            androidHome = defaultAndroidHome;
        }


        File androidSDKDir;
        String bat = "";
        if (is_windows) {

            bat = ".bat";
        }


        androidSDKDir = new File(androidHome);
        if (!androidSDKDir.exists()) {
            throw new BuildException("Cannot find Android SDK at "+androidHome+".  Please install Android studio, or set the ANDROID_HOME environment variable to point to your android sdk directory.");
        }

        File sdkmanager = new File(androidSDKDir, path("tools", "bin", "sdkmanager"+bat));
        if (!sdkmanager.canExecute()) {
            sdkmanager = new File(androidSDKDir, path("cmdline-tools", "latest", "bin", "sdkmanager"+bat));
        }
        if (!sdkmanager.canExecute()) {
            Exception ex = new RuntimeException("Android SDK Command-Line Tools not found");
            error("Cannot find executable sdkmanager"+bat+" in "+androidSDKDir+"; tried tools and cmdline-tools/latest", ex);
            throw new BuildException("Cannot find executable sdkmanager"+bat+" in "+androidSDKDir+"; tried tools and cmdline-tools/latest", ex);
        }

        String sdkListStr;
        try {
            sdkListStr = execString(tmpDir, sdkmanager.getAbsolutePath(), "--list");
        } catch (Exception ex) {
            error("Failed to get SDK list using "+sdkmanager+".  "+ex.getMessage(), ex);
            throw new BuildException("Failed to get SDK list using "+sdkmanager, ex);
        }
        Scanner sdkScanner = new Scanner(sdkListStr);
        List<String> installedPlatforms = new ArrayList<>();
        List<String> installedBuildToolsVersions = new ArrayList<>();
        while (sdkScanner.hasNextLine()) {
            String line = sdkScanner.nextLine().trim();
            if (line.startsWith("build-tools;")) {
                String[] columns = line.split("\\|");
                if (columns.length >= 4) {
                    // If there are only 3 columns, then this is not referring to an installed build-tools
                    // but an available one.
                    String[] col1Parts = columns[0].split(";");
                    if (col1Parts.length > 1) {
                        installedBuildToolsVersions.add(col1Parts[1].trim());
                    }
                }
            } else if (line.startsWith("platforms;")) {
                String[] columns = line.split("\\|");
                if (columns.length > 1) {
                    String[] col1Parts = columns[0].split(";");
                    String platform = col1Parts[1].trim();
                    if (platform.contains("-")) {
                        platform = platform.substring(platform.indexOf("-")+1);
                    }

                    installedPlatforms.add(platform);
                }
            }
        }

        debug("Installed platforms: "+installedPlatforms);

        int maxBuildToolsVersionInt = 0;
        String maxBuildToolsVersion = "0";
        for (String ver : installedBuildToolsVersions) {
            int verInt = parseVersionStringAsInt(ver);
            if (verInt > maxBuildToolsVersionInt) {
                maxBuildToolsVersion = ver;
                maxBuildToolsVersionInt = verInt;
            }
        }

        int maxPlatformVersionInt = 0;
        String maxPlatformVersion = "0";
        for (String ver : installedPlatforms) {
            int verInt = parseVersionStringAsInt(ver);
            if (verInt > maxPlatformVersionInt) {
                maxPlatformVersionInt = verInt;
                maxPlatformVersion = ver;
            }
        }

        if (maxPlatformVersionInt == 0) {
            maxPlatformVersionInt = 31;
            maxPlatformVersion = "31";
        }

        if (maxBuildToolsVersionInt == 0) {
            maxBuildToolsVersionInt = 31;
            maxBuildToolsVersion = "31";
        }



        useAndroidX = request.getArg("android.useAndroidX", decouplePlayServiceVersions ? "true" : "false").equals("true");
        migrateToAndroidX = useAndroidX && request.getArg("android.migrateToAndroidX", "true").equals("true");

        buildToolsVersionInt = maxBuildToolsVersionInt;
        this.buildToolsVersion = request.getArg("android.buildToolsVersion", ""+maxBuildToolsVersion);
        String buildToolsVersionIntStr = this.buildToolsVersion;
        if (buildToolsVersionIntStr.indexOf(".") > 1) {
            buildToolsVersionIntStr = buildToolsVersionIntStr.substring(0, buildToolsVersionIntStr.indexOf("."));
        }
        buildToolsVersionInt = Integer.parseInt(buildToolsVersionIntStr.replaceAll("[^0-9]", ""));
        if (useAndroidX && buildToolsVersionInt < 29) {
            buildToolsVersionInt = 29;
            this.buildToolsVersion = "29";
        } else if (buildToolsVersionInt > 28 && !useAndroidX) {
            useAndroidX = true;
            migrateToAndroidX = useAndroidX && request.getArg("android.migrateToAndroidX", "true").equals("true");
        }
        debug("Effective build tools version = "+this.buildToolsVersion);


        // Augment the xpermissions request arg with explicit android.permissions.XXX build hints
        xPermissions = request.getArg("android.xpermissions", "");
        debug("Adding android permissions...");
        for (String xPerm : ANDROID_PERMISSIONS) {
            String permName = xPerm.substring(xPerm.lastIndexOf(".")+1);
            if (request.getArg("android.permission."+permName, "false").equals("true")) {
                debug("Found permission "+permName);
                String maxSdk = request.getArg("android.permission."+permName+".maxSdkVersion", "");
                String required = request.getArg("android.permission."+permName+".required", "");
                String addString =  "    <uses-permission android:name=\""+xPerm+"\" ";
                if (!"".equals(required)) {
                    addString += "android:required=\""+required+"\" ";
                }

                if (!"".equals(maxSdk)) {
                    addString += "android:maxSdkVersion=\""+maxSdk+"\" ";
                }
                addString += "/>\n";

                xPermissions += permissionAdd(request, xPerm, addString);
            }
        }

        final String usesFeaturePrefix = "android.uses_feature.";
        final int usesFeaturePrefixLen = usesFeaturePrefix.length();
        for (final String arg : request.getArgs()) {
            if (!arg.startsWith(usesFeaturePrefix)) {
                continue;
            }
            final String featureName = arg.substring(usesFeaturePrefixLen);
            final String rawArgValue = request.getArg(arg, "false");
            if (rawArgValue.equals("false")) {
                continue;
            }
            final boolean requiredFlag = rawArgValue.equals("required");
            String addString =  "    <uses-feature android:name=\""+featureName+"\" ";
            if (requiredFlag) {
                addString += "android:required=\"true\" ";
            }
            addString += "/>\n";
            xPermissions += permissionAdd(request, featureName, addString);
        }

        final String usesPermissionPrefix = "android.uses_permission.";
        final int usesPermissionPrefixLen = usesPermissionPrefix.length();
        final String maxSdkVersionPrefix = "maxSdkVersion:";
        final int maxSdkVersionPrefixLen = maxSdkVersionPrefix.length();
        for (final String arg : request.getArgs()) {
            if (!arg.startsWith(usesPermissionPrefix)) {
                continue;
            }
            final String permissionName = arg.substring(usesPermissionPrefixLen);
            final String rawArgValue = request.getArg(arg, "false");
            if (rawArgValue.equals("false")) {
                continue;
            }
            final boolean requiredFlag = rawArgValue.contains("required");
            final int maxSdkVersionPos = rawArgValue.indexOf(maxSdkVersionPrefix);
            String maxSdkVersion = maxSdkVersionPos >= 0 ? rawArgValue.substring(maxSdkVersionPos + maxSdkVersionPrefixLen) : "";
            if (!maxSdkVersion.isEmpty() && maxSdkVersion.contains(" ")) {
                maxSdkVersion = maxSdkVersion.substring(0, maxSdkVersion.indexOf(" "));
            }

            String addString =  "    <uses-permission android:name=\""+permissionName+"\" ";
            if (requiredFlag) {
                addString += "android:required=\"true\" ";
            }
            if (!"".equals(maxSdkVersion)) {
                addString += "android:maxSdkVersion=\""+maxSdkVersion+"\" ";
            }
            addString += "/>\n";
            xPermissions += permissionAdd(request, permissionName, addString);
        }

        File tmpFile = getBuildDirectory();
        if (tmpFile == null) {
            throw new IllegalStateException("Build directory must be set before running build.");
        }
        if (tmpFile.exists()) {
            delTree(tmpFile);
        }
        tmpFile.mkdirs();
        File managedGradleHome = new File(path(System.getProperty("user.home"), ".codenameone", "gradle"));
        String gradleHome = System.getenv("GRADLE_HOME");
        if (gradleHome == null && managedGradleHome.exists()) {
            gradleHome = managedGradleHome.getAbsolutePath();
        }
        String gradleExe = System.getenv("GRADLE_PATH");

        if (gradleExe == null) {
            if (gradleHome != null) {
                gradleExe = new File(gradleHome + File.separator + "bin"
                        + File.separator + "gradle" + bat).getAbsolutePath();
            } else {
                gradleExe = "gradle";
            }
        }

        if (PREFER_MANAGED_GRADLE) {
            debug("PREFER_MANAGED_GRADLE flag is set.  Ignoring GRADLE_HOME and GRADLE_PATH environment variables.  Using managed gradle at "+managedGradleHome+" instead");
            gradleHome = managedGradleHome.getAbsolutePath();
            gradleExe = new File(managedGradleHome, path("bin", "gradle"+bat)).getAbsolutePath();

        }

        String gradleVersion;
        try {
            gradleVersion = getGradleVersion(gradleExe);
        } catch (Exception ex) {
            gradleVersion = "0";
        }
        debug("FOUND gradleVersion "+gradleVersion);
        int gradleVersionInt = parseVersionStringAsInt(gradleVersion);
        debug("Found gradleVersionInt="+gradleVersionInt);
        if (gradleVersionInt <  MIN_GRADLE_VERSION) {
            // The minimum version is too low.
            if (managedGradleHome.exists()) {
                gradleExe = new File(managedGradleHome, path("bin", "gradle"+bat)).getAbsolutePath();
                try {
                    gradleVersion = getGradleVersion(gradleExe);
                } catch (Exception ex) {
                    gradleVersion = "0";
                }
                gradleVersionInt = parseVersionStringAsInt(gradleVersion);

            }
            if (gradleVersionInt < MIN_GRADLE_VERSION) {
                if (managedGradleHome.exists()) {
                    delTree(managedGradleHome);
                }
                File gradleZip = new File(managedGradleHome+".zip");
                if (gradleZip.exists()) {
                    gradleZip.delete();
                }
                try {
                    log("Downloading gradle distribution from "+gradleDistributionUrl);
                    FileUtils.copyURLToFile(new URL(gradleDistributionUrl), gradleZip);
                } catch (Exception ex) {
                    throw new BuildException("Failed to download gradle distribution from URL "+gradleDistributionUrl, ex);
                }
                try {
                    ZipFile gradleZipFile = new ZipFile(gradleZip);
                    File extracted = new File(path(gradleZip.getAbsolutePath()+"-extracted"));
                    extracted.mkdir();
                    gradleZipFile.extractAll(extracted.getAbsolutePath());
                    gradleZip.delete();

                    for (File extractedChild : extracted.listFiles()) {
                        if (extractedChild.getName().startsWith("gradle") && extractedChild.isDirectory()) {
                            try {
                                if (managedGradleHome.exists()) {
                                    if (managedGradleHome.isDirectory()) {
                                        FileUtils.deleteDirectory(managedGradleHome);
                                    } else {
                                        managedGradleHome.delete();
                                    }
                                }
                                FileUtils.moveDirectory(extractedChild, managedGradleHome);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            break;
                        }
                    }

                } catch (ZipException zex) {
                    throw new BuildException("Failed to unzip gradle distribution after downloading it", zex);
                }

                if (!managedGradleHome.exists()) {
                    throw new BuildException("There was a problem extracting the gradle distribution. Expected it to be extracted at "+managedGradleHome+", but was not found");
                }

                File managedGradleExe = new File(managedGradleHome, path("bin", "gradle"+bat));
                if (!managedGradleExe.exists()) {
                    throw new BuildException("Expected to find gradle executable at "+managedGradleExe+" after download and extraction, but it wasn't there.  Something about the gradle install must have failed.  Try again.");
                }

                gradleExe = managedGradleExe.getAbsolutePath();
                try {
                    gradleVersion = getGradleVersion(gradleExe);

                } catch (Exception ex) {
                    throw new BuildException("Failed to get gradle version even after downloading it from "+gradleDistributionUrl+".  Something must have gone wrong with the gradle installation.");
                }
                gradleVersionInt = parseVersionStringAsInt(gradleVersion);
                if (gradleVersionInt < MIN_GRADLE_VERSION) {
                    throw new BuildException("Required gradle version is "+MIN_GRADLE_VERSION+" but found version "+gradleVersion);
                }
            }
        }

        File androidToolsDir = new File(androidSDKDir, "tools");
        File androidCommand = new File(androidToolsDir, "android" + bat);
        File projectDir = new File(tmpFile, request.getMainClass());
        gradleProjectDirectory = projectDir;

        String androidVersion = "android-14";
        String defaultVersion = maxPlatformVersion;
        String usesLibrary = "        <uses-library android:name=\"org.apache.http.legacy\" android:required=\"false\" />\n";

        String targetNumber = request.getArg("android.targetSDKVersion", defaultVersion);

        if(!targetNumber.equals(defaultVersion)) {
            try {
                if(Integer.parseInt(targetNumber) < 28) {
                    usesLibrary = "";
                }
            } catch(Exception err) {

            }
        }

        String targetSDKVersion = targetNumber;
        final int targetSDKVersionInt = Integer.parseInt(targetSDKVersion);

        if (targetSDKVersionInt > 14) {
            androidVersion = "android-" + targetSDKVersion;
        }
        targetSDKVersion = " android:targetSdkVersion=\"" + targetSDKVersion + "\" ";
        log("TargetSDKVersion="+targetSDKVersion);




        String gradlePluginVersion = "1.3.1";
        if(gradleVersionInt < 3){
            gradlePluginVersion = "2.0.0";
        } else {
            if(gradleVersionInt < 6){
                if (useAndroidX) {
                    gradlePluginVersion = "3.2.0";
                } else {

                    gradlePluginVersion = "3.0.1";
                }
            } else  {
                gradlePluginVersion = "4.1.1";
            }
        }
        boolean androidAppBundle = request.getArg("android.appBundle", gradleVersionInt >= 5 ? "true" : "false").equals("true");

        debug("gradlePluginVersion="+gradlePluginVersion);
        projectDir = new File(projectDir, "app");
        File studioProjectDir = projectDir.getParentFile();


        if (isUnitTestMode()) {

            throw new BuildException("Unit Test mode not currently supported for local android builds.");

        } else {
            try {
                log("Creating AndroidStudioProject from template");
                if (studioProjectDir.exists()) {
                    delTree(studioProjectDir);
                }
                createAndroidStudioProject(studioProjectDir);

            } catch (Exception ex) {
                error("Failed to create AndroidStudioProject: "+ex.getMessage(), ex);
                throw new BuildException("Failed to create android project", ex);
            }
        }

        File assetsDir = new File(projectDir + "/src/main", "assets");
        assetsDir.mkdirs();
        File resDir = new File(projectDir + "/src/main", "res");
        resDir.mkdirs();
        File valsDir = new File(resDir, "values");
        valsDir.mkdirs();

        File vals11Dir = null;
        vals11Dir = new File(resDir, "values-v11");
        vals11Dir.mkdirs();

        File vals21Dir = null;
        vals21Dir = new File(resDir, "values-v21");
        vals21Dir.mkdirs();

        File layoutDir = new File(resDir, "layout");
        layoutDir.mkdirs();

        File xmlDir = new File(resDir, "xml");
        xmlDir.mkdirs();

        File srcDir = new File(projectDir, "src/main/java");
        srcDir.mkdirs();
        File dummyClassesDir = new File(tmpFile, "Classes");
        dummyClassesDir.mkdirs();
        File libsDir = new File(projectDir, "libs");
        libsDir.mkdirs();
        try {
            debug("Extracting "+sourceZip);
            unzip(sourceZip, dummyClassesDir, assetsDir, srcDir, libsDir, xmlDir);
        } catch (Exception ex) {
            throw new BuildException("Failed to extract source zip "+sourceZip, ex);
        }

        File appDir = buildToolsVersionInt >= 27 ?
                new File(srcDir.getParentFile(), "app") :
                new File(libsDir.getParentFile(), "app");
        File googleServicesJson = new File(appDir, "google-services.json");

        googleServicesJson = new File(libsDir.getParentFile(), "google-services.json");

        try {
            if (!retrolambda(new File(System.getProperty("user.dir")), request, dummyClassesDir)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to run retrolambda on classes", ex);
        }
        String additionalImports = request.getArg("android.activityClassImports", "");
        String additionalMembers = request.getArg("android.activityClassBody", "");
        String additionalKeyVals = "";
        String mopubActivities = "";
        String mopubBannerXML = "";
        String permissions = "";
        String telephonyRequired = "false";

        String aarDependencies = "";
        //move dependant projects to a separate directory
        File[] childs = libsDir.listFiles();
        for (int i = 0; i < childs.length; i++) {
            File file = childs[i];
            if (file.getName().endsWith(".andlib")) {
                throw new BuildException("andlib format is not supported anymore, use aar instead");
            }
            if (file.getName().endsWith(".aar")) {
                String name = file.getName().substring(0, file.getName().lastIndexOf("."));
                if(request.getArg("android.arrimplementation", "").contains(
                        name)) {
                    aarDependencies += "    implementation(name:'" + name + "', ext:'aar')\n";
                } else {
                    aarDependencies += "    compile(name:'" + name + "', ext:'aar')\n";
                }
            }

        }


        String minSDK = request.getArg("android.min_sdk_version", "19");
        String facebookSupport = "";
        String facebookProguard = "";
        String facebookActivityMetaData = "";
        String facebookActivity = "";
        String facebookHashCode = "";
        boolean facebookSupported = request.getArg("facebook.appId", null) != null;
        if (facebookSupported) {
            facebookHashCode = "        try {\n"
                    + "            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(\n"
                    + "                  \"" + request.getPackageName() + "\", android.content.pm.PackageManager.GET_SIGNATURES);\n"
                    + "            for (android.content.pm.Signature signature : info.signatures){\n"
                    + "                   java.security.MessageDigest md = java.security.MessageDigest.getInstance(\"SHA\");\n"
                    + "                   md.update(signature.toByteArray());\n"
                    + "                   android.util.Log.d(\"KeyHash:\", android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT));\n"
                    + "                   Display.getInstance().setProperty(\"facebook_hash\", android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT));\n"
                    + "            }\n"
                    + "        } catch (android.content.pm.PackageManager.NameNotFoundException e) {\n"
                    + "            e.printStackTrace();\n"
                    + "        } catch (java.security.NoSuchAlgorithmException e) {\n"
                    + "            e.printStackTrace();\n"
                    + "        }\n\n";

            String permissionsStr = request.getArg("android.facebook_permissions", "\"public_profile\",\"email\",\"user_friends\"");
            permissionsStr = request.getArg("and.facebook_permissions", permissionsStr);
            permissionsStr = permissionsStr.replace('"', ' ');

            facebookSupport = "Display.getInstance().setProperty(\"facebook_app_id\", \"" + request.getArg("facebook.appId", "706695982682332") + "\");\n"
                    + "        Display.getInstance().setProperty(\"facebook_permissions\", \"" + permissionsStr + "\");\n"
                    + " com.codename1.social.FacebookImpl.init();\n";

            facebookProguard = "-keep class com.facebook.** { *; }\n"
                    + "-keepattributes Signature\n"
                    + "-dontwarn bolts.**\n"
                    + "-dontnote android.support.**\n"
                    + "-dontnote androidx.**";


            facebookActivityMetaData = " <meta-data android:name=\"com.facebook.sdk.ApplicationId\" android:value=\"@string/facebook_app_id\"/>\n";
            facebookActivity = " <activity android:name=\"com.facebook.FacebookActivity\" android:exported=\"true\"/>\n";
            additionalKeyVals += "<string name=\"facebook_app_id\">" + request.getArg("facebook.appId", "706695982682332") + "</string>";
        }

        String googlePlayAdsMetaData = "";
        String googlePlayAdsActivity = "";
        String googlePlayObfuscation = "";
        String googleAdUnitId = request.getArg("android.googleAdUnitId", request.getArg("google.adUnitId", null));
        String googlePlayAdViewCode = "";
        if (googleAdUnitId != null && googleAdUnitId.length() > 0) {
            minSDK = maxInt("9", minSDK);
            googlePlayAdsMetaData = "<meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\"/>";
            googlePlayAdsActivity = "<activity android:name=\"com.google.android.gms.ads.AdActivity\" android:configChanges=\"keyboard|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize\" android:exported=\"false\"/>";
            accessNetworkStatePermission = true;

            String testDevice = request.getArg("android.googleAdUnitTestDevice", "C6783E2486F0931D9D09FABC65094FDF");
            googlePlayAdViewCode
                    = "            com.google.android.gms.ads.AdView adView = new com.google.android.gms.ads.AdView(this);\n"
                    + "            adView.setAdUnitId(\"" + googleAdUnitId + "\");\n"
                    + "            adView.setId(2002);\n"
                    + "            adView.setAdSize(com.google.android.gms.ads.AdSize.SMART_BANNER);\n"
                    + "            AndroidImplementation.setViewAboveBelow(null, adView, 0, com.google.android.gms.ads.AdSize.SMART_BANNER.getHeightInPixels(this));\n"
                    + "            com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().addTestDevice(\""
                    + testDevice
                    + "\").build();\n"
                    + "            adView.loadAd(adRequest);\n";

            googlePlayObfuscation
                    = "-keep class * extends java.util.ListResourceBundle {\n"
                    + "    protected Object[][] getContents();\n"
                    + "}\n"
                    + "\n"
                    + "-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {\n"
                    + "    public static final *** NULL;\n"
                    + "}\n"
                    + "\n"
                    + "-keepnames @com.google.android.gms.common.annotation.KeepName class *\n"
                    + "-keepclassmembernames class * {\n"
                    + "    @com.google.android.gms.common.annotation.KeepName *;\n"
                    + "}\n"
                    + "\n"
                    + "-keepnames class * implements android.os.Parcelable {\n"
                    + "    public static final ** CREATOR;\n"
                    + "}\n";

        }


        playServicesVersion = request.getArg("android.playServicesVersion", playServicesVersion);
        playServicesVersionSetInBuildHint = request.getArg("android.playServicesVersion", null) != null;

        final String playServicesValue = request.getArg("android.includeGPlayServices", null);
        playFlag = "true";

        gpsPermission = request.getArg("android.gpsPermission", "false").equals("true");
        try {
            scanClassesForPermissions(dummyClassesDir, new Executor.ClassScanner() {


                @Override
                public void usesClass(String cls) {
                    if (cls.indexOf("com/codename1/notifications") == 0) {
                        recieveBootCompletedPermission = true;
                    }
                    if (cls.indexOf("com/codename1/capture") == 0) {
                        capturePermission = true;
                    }
                    if (cls.indexOf("com/codename1/ads") == 0) {
                        debug("Adding phone permission because of class " + cls);
                        phonePermission = true;
                    }
                    if (cls.indexOf("com/codename1/components/Ads") == 0) {
                        debug("Adding phone permission because of class " + cls);
                        phonePermission = true;
                    }
                    if (cls.indexOf("com/codename1/maps") == 0 || cls.indexOf("com/codename1/location") == 0) {
                        gpsPermission = true;
                    }
                    if (cls.indexOf("com/codename1/push") > -1) {
                        pushPermission = true;
                        if (targetSDKVersionInt >= 28) {
                            foregroundServicePermission = true;
                        }
                    }
                    if (cls.indexOf("com/codename1/contacts") > -1) {
                        contactsReadPermission = true;
                    }
                    if (cls.indexOf("com/codename1/payment") > -1) {
                        purchasePermissions = true;
                    }
                    if (cls.indexOf("com/codename1/location/Geofence") > -1) {
                        if (!"true".equals(playServicesValue)) {
                            // If play services are not currently "blanket" enabled
                            // we will enable them here
                            debug("Adding location playservice");
                            request.putArgument("android.location.minPlayServicesVersion", "12.0.1");
                            playServicesLocation = true;
                            playFlag = "false";
                            if (targetSDKVersionInt >= 29) {
                                backgroundLocationPermission = true;
                            }
                        }
                    }

                    if (cls.indexOf("com/codename1/social") > -1) {
                        credentialsPermission = true;
                        getAccountsPermission = true;
                    }

                }


                @Override
                public void usesClassMethod(String cls, String method) {
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && (method.indexOf("vibrate") > -1 || method.indexOf("notifyStatusBar") > -1)) {
                        vibratePermission = true;
                    }

                    if ((cls.indexOf("com/codename1/media/MediaManager") == 0 && method.indexOf("createBackgroundMedia") > -1)) {
                        if (targetSDKVersionInt >= 28) {
                            foregroundServicePermission = true;
                        }
                    }

                    if ((cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("createBackgroundMedia") > -1)) {
                        if (targetSDKVersionInt >= 28) {
                            foregroundServicePermission = true;
                        }
                    }

                    if (cls.indexOf("com/codename1/location/LocationManager") == 0 && (method.indexOf("addGeoFencing") > -1 || method.indexOf("setBackgroundLocationListener") > -1)) {

                        if (!"true".equals(playServicesValue)) {
                            if (targetSDKVersionInt >= 29) {
                                backgroundLocationPermission = true;
                            }
                        }
                    }
                    if (cls.indexOf("com/codename1/location/LocationManager") == 0 && (method.indexOf("addGeoFencing") > -1 || method.indexOf("getLocationManager") > -1)) {

                        if (!"true".equals(playServicesValue)) {
                            // If play services are not currently "blanket" enabled
                            // we will enable them here
                            debug("Adding location playservice");
                            request.putArgument("android.location.minPlayServicesVersion", "12.0.1");
                            playServicesLocation = true;
                            playFlag = "false";
                        }
                    }
                    if (cls.indexOf("com/codename1/media/MediaManager") == 0 && method.indexOf("setRemoteControlListener") > -1) {
                        debug("Adding wake lock permission due to use of MediaManager.setRemoteControlListener");
                        //smsPermission = true;
                        wakeLock = true;
                        addRemoteControlService = true;
                    }

                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("getUdid") > -1) {
                        debug("Adding phone permission because of Display.getUdid method");
                        phonePermission = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("getMsisdn") > -1) {
                        phonePermission = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("getAllContacts") > -1) {
                        contactsReadPermission = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("lockScreen") > -1) {
                        wakeLock = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("setScreenSaverEnabled") > -1) {
                        wakeLock = true;
                    }
                    if (cls.indexOf("com/codename1/media/MediaManager") == 0 && method.indexOf("createMediaRecorder") > -1) {
                        recordAudio = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("createMediaRecorder") > -1) {
                        recordAudio = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("createContact") > -1) {
                        contactsWritePermission = true;
                    }
                    if (cls.indexOf("com/codename1/ui/Display") == 0 && method.indexOf("deleteContact") > -1) {
                        contactsWritePermission = true;
                    }
                    if (cls.indexOf("com/codename1/contacts/ContactsManager") == 0 && method.indexOf("createContact") > -1) {
                        contactsWritePermission = true;
                    }
                    if (cls.indexOf("com/codename1/contacts/ContactsManager") == 0 && method.indexOf("deleteContact") > -1) {
                        contactsWritePermission = true;
                    }
                }
            });
        } catch (IOException ex) {
            throw new BuildException("An error occurred while trying to scan the classes for API usage.", ex);
        }
        boolean useFCM = pushPermission && "fcm".equalsIgnoreCase(request.getArg("android.messagingService", "fcm"));
        if (useFCM) {
            request.putArgument("android.fcm.minPlayServicesVersion", "12.0.1");
        }
        debug("Starting playServicesVersion "+playServicesVersion);

        for (String arg : request.getArgs()) {
            if (arg.endsWith(".minPlayServicesVersion")) {
                if (compareVersions(request.getArg(arg, null), playServicesVersion) > 0) {
                    playServicesVersion = request.getArg(arg, null);
                    debug("playServicesVersion increased to "+playServicesVersion+" due to "+arg);
                }
            }
        }
        request.putArgument("android.playServicesVersion", playServicesVersion);
        request.putArgument("android.firebaseCoreVersion", request.getArg("android.firebaseCoreVersion", getDefaultPlayServiceVersion("firebase-core")));
        request.putArgument("android.firebaseMessagingVersion", request.getArg("android.firebaseMessagingVersion", getDefaultPlayServiceVersion("firebase-messaging")));

        debug("-----USING PLAY SERVICES VERSION "+playServicesVersion+"----");

        if (useFCM) {
            String compile = "compile";
            if (useAndroidX) {
                compile = "implementation";
            }
            if (!googleServicesJson.exists()) {
                error("google-services.json not found.  When using FCM for push notifications (i.e. android.messagingService=fcm), you must include valid google-services.json file.  Use the Firebase console to add Firebase messaging to your app.  https://console.firebase.google.com/u/0/ Then download the google-services.json file and place it in the native/android directory of your project. If you still want to use GCM (which no longer works) define the build hint android.messagingService=gcm", new RuntimeException());
                return false;
            }
            if (buildToolsVersionInt < 27) {
                error("FCM push notifications require build tools version 27 or higher.  Please set the android.buildToolsVersion to 27.0.0 or higher or remove the android.messagingService=fcm build hint.", new RuntimeException());
                return false;
            }

            if (!request.getArg("android.topDependency", "").contains("com.google.gms:google-services")) {
                request.putArgument("android.topDependency", request.getArg("android.topDependency", "") + "\n    classpath 'com.google.gms:google-services:4.0.1'\n");
            }
            if (!request.getArg("android.xgradle", "").contains("apply plugin: 'com.google.gms.google-services'")) {
                request.putArgument("android.xgradle", request.getArg("android.xgradle", "") + "\napply plugin: 'com.google.gms.google-services'\n");
            }
            if (!request.getArg("gradleDependencies", "").contains("com.google.firebase:firebase-core")) {
                debug("Adding firebase core to gradle dependencies.");
                debug("Play services version: " + request.getArg("var.android.playServicesVersion", ""));
                debug("gradleDependencies before: "+request.getArg("gradleDependencies", ""));

                request.putArgument(
                        "gradleDependencies",
                        request.getArg("gradleDependencies", "") +
                                "\n"+compile+" \"com.google.firebase:firebase-core:" +
                                request.getArg("android.firebaseCoreVersion", playServicesVersion) + "\"\n"
                );
                debug("gradleDependencies after: "+request.getArg("gradleDependencies", ""));
            }
            if (!request.getArg("gradleDependencies", "").contains("com.google.firebase:firebase-messaging")) {
                request.putArgument(
                        "gradleDependencies",
                        request.getArg("gradleDependencies", "") +
                                "\n"+compile+" \"com.google.firebase:firebase-messaging:" +
                                request.getArg("android.firebaseMessagingVersion", playServicesVersion) + "\"\n"
                );
            }
        }



        // if a flag is declared we don't want the default play flag to be true
        if(request.getArg("android.playService.plus", null)  != null ||
                request.getArg("android.playService.auth", (googleServicesJson.exists()) ? "true":null)  != null ||
                request.getArg("android.playService.base", null)  != null ||
                request.getArg("android.playService.identity", null)  != null ||
                request.getArg("android.playService.indexing", null)  != null ||
                request.getArg("android.playService.appInvite", null)  != null ||
                request.getArg("android.playService.analytics", null)  != null ||
                request.getArg("android.playService.cast", null)  != null ||
                request.getArg("android.playService.gcm", null)  != null ||
                request.getArg("android.playService.drive", null)  != null ||
                request.getArg("android.playService.fitness", null)  != null ||
                request.getArg("android.playService.location", null)  != null ||
                request.getArg("android.playService.maps", null)  != null ||
                request.getArg("android.playService.ads", null)  != null ||
                request.getArg("android.playService.vision", null)  != null ||
                request.getArg("android.playService.nearby", null)  != null ||
                request.getArg("android.playService.panorama", null)  != null ||
                request.getArg("android.playService.games", null)  != null ||
                request.getArg("android.playService.safetynet", null)  != null ||
                request.getArg("android.playService.wallet", null)  != null ||
                request.getArg("android.playService.wearable", null)  != null ||
                request.getArg("android.playService.ads", null)  != null) {
            playFlag = "false";
        }
        initPlayServiceVersions(request);


        boolean legacyGplayServicesMode = false;

        if(playServicesValue != null) {
            if(playServicesValue.equals("true")){
                // compatibility mode...
                legacyGplayServicesMode = true;
                if(playFlag.equals("false")) {
                    // legacy gplay can't be mixed with explicit gplay fail the build right now!
                    if (googleServicesJson.exists()) {
                        debug("The android.playService.auth flag was automatically enabled because the project includes the google-services.json file");
                    }
                    error("Error: you can't use the build hint android.includeGPlayServices together with android.playService.* build hints. They are exclusive of one another. Please remove the old android.includeGPlayServices hint from your code or from the cn1lib that might have injected it", new RuntimeException());
                    return false;
                }
                playFlag = "true";
            } else {
                playFlag = "false";
            }
        }


        playServicesPlus = !request.getArg("android.playService.plus", "false" ).equals("false");
        playServicesAuth = !request.getArg("android.playService.auth", (Boolean.valueOf(playFlag) || googleServicesJson.exists()) ? "true" : "false").equals("false");
        playServicesBase = !request.getArg("android.playService.base", playFlag).equals("false");
        playServicesIdentity = !request.getArg("android.playService.identity", "false").equals("false");
        playServicesIndexing = !request.getArg("android.playService.indexing", "false").equals("false");
        playServicesInvite = !request.getArg("android.playService.appInvite", "false").equals("false");
        playServicesAnalytics = !request.getArg("android.playService.analytics", playFlag).equals("false");
        playServicesCast = !request.getArg("android.playService.cast", "false").equals("false");
        playServicesGcm = !request.getArg("android.playService.gcm", playFlag).equals("false") ||
                request.getArg("gcm.sender_id", null) != null;
        playServicesDrive = !request.getArg("android.playService.drive", "false").equals("false");
        playServicesFit= !request.getArg("android.playService.fitness", "false").equals("false");
        playServicesLocation = playServicesLocation || !request.getArg("android.playService.location", playFlag).equals("false");
        playServicesMaps = !request.getArg("android.playService.maps", playFlag).equals("false");
        playServicesAds = !request.getArg("android.playService.ads", "false").equals("false");
        if(request.getArg("android.googleAdUnitId", request.getArg("google.adUnitId", null)) != null) {
            playServicesAds = true;
        }
        playServicesVision = !request.getArg("android.playService.vision", "false").equals("false");
        playServicesNearBy = !request.getArg("android.playService.nearby", "false").equals("false");
        playServicesSafetyPanorama = !request.getArg("android.playService.panorama", "false").equals("false");
        playServicesGames = !request.getArg("android.playService.games", "false").equals("false");
        playServicesSafetyNet = !request.getArg("android.playService.safetynet", "false").equals("false");
        playServicesWallet = !request.getArg("android.playService.wallet", "false").equals("false");
        playServicesWear = !request.getArg("android.playService.wearable", "false").equals("false");




        if (googleAdUnitId == null && playServicesAds) {
            minSDK = maxInt("9", minSDK);
            googlePlayAdsMetaData = "<meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\"/>";
        }
        if (playServicesLocation) {
            debug("Play Services Location Enabled");
            googlePlayObfuscation += "-keep class com.codename1.location.AndroidLocationPlayServiceManager {\n"
                    + "*;\n"
                    + "}\n\n";
            googlePlayObfuscation += "-keep class com.codename1.location.BackgroundLocationHandler {\n"
                    + "*;\n"
                    + "}\n\n";
            googlePlayObfuscation += "-keep class com.codename1.location.BackgroundLocationBroadcastReceiver {\n"
                    + "*;\n"
                    + "}\n\n";
            googlePlayObfuscation += "-keep class com.codename1.impl.android.BackgroundFetchHandler {\n"
                    + "*;\n"
                    + "}\n\n";
            googlePlayObfuscation += "-keep class com.codename1.location.GeofenceHandler {\n"
                    + "*;\n"
                    + "}\n\n";
            googlePlayObfuscation += "-keep class com.codename1.location.CodenameOneBackgroundLocationActivity {\n"
                    + "*;\n"
                    + "}\n\n";


        } else {
            debug("Play services location disabled");
        }

        shouldIncludeGoogleImpl = playServicesAuth;


        if (shouldIncludeGoogleImpl) {
            googlePlayObfuscation += "-keep class com.codename1.social.GoogleImpl {\n"
                    + "*;\n"
                    + "}\n\n";
        }

        File stubFileSourceDir = new File(srcDir, request.getPackageName().replace('.', File.separatorChar));
        stubFileSourceDir.mkdirs();

        String headphonesVars = "";
        String headphonesOnResume = "";
        if (request.getArg("android.headphoneCallback", "false").equals("true")) {
            headphonesVars = "    HeadSetReceiver myHeadphoneReceiver;\n\n"
                    + "    public static void headphonesConnected() {\n"
                    + "        i.headphonesConnected();"
                    + "    }"
                    + "    public static void headphonesDisconnected() {\n"
                    + "        i.headphonesDisconnected();"
                    + "    }";

            headphonesOnResume
                    = "        HeadSetReceiver myReceiver = new HeadSetReceiver();\n"
                    + "        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);\n"
                    + "        registerReceiver(myReceiver, filter);\n";

            File headphonesFile = new File(stubFileSourceDir, "HeadSetReceiver.java");
            String stubSourceCode = "package " + request.getPackageName() + ";\n\n"
                    + "import android.content.Context;\n"
                    + "import android.content.Intent;\n"
                    + "import android.content.IntentFilter;\n"
                    + "import android.content.BroadcastReceiver;\n\n"
                    + "public class HeadSetReceiver extends BroadcastReceiver {\n"
                    + "    @Override public void onReceive(Context context, Intent intent) {\n"
                    + "        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {\n"
                    + "            int state = intent.getIntExtra(\"state\", -1);\n"
                    + "            switch (state) {\n"
                    + "            case 0:\n"
                    + "                " + request.getMainClass() + "Stub.headphonesDisconnected();\n"
                    + "                break;\n"
                    + "            case 1:\n"
                    + "                " + request.getMainClass() + "Stub.headphonesConnected();\n"
                    + "                break;\n"
                    + "            }\n"
                    + "        }\n"
                    + "    }\n"
                    + "}";
            try {
                createFile(headphonesFile, stubSourceCode.getBytes());
            } catch (IOException ex) {
                throw new BuildException("Failed to create HeadSetReceiver class", ex);
            }
        }


        //unzip(getResourceAsStream("/Android.jar"), dummyClassesDir, assetsDir, srcDir);

        if (request.getArg("noExtraResources", "false").equals("true")) {
            new File(assetsDir, "CN1Resource.res").delete();
            new File(assetsDir, "androidTheme.res").delete();
            new File(assetsDir, "android_holo_light.res").delete();
        }
        if (getAndroidPortSrcJar() == null) {
            try {
                setAndroidPortSrcJar(getResourceAsFile("/com/codename1/android/android_port_sources.jar", ".jar"));
            } catch (IOException ex) {
                throw new BuildException("Failed to find android_port_sources.jar");
            }
        }
        if (!getAndroidPortSrcJar().exists()) {
            throw new IllegalStateException("Configuration error.  Cannot find androidPortSrcJar at "+getAndroidPortSrcJar());
        }
        try {
            unzip(androidPortSrcJar, srcDir, assetsDir, srcDir);
        } catch (IOException ex) {
            throw new BuildException("Failed to extract android port sources from "+androidPortSrcJar, ex);
        }


        // We need to choose the correct PlayServices class file for the version of play services
        // we are building for.
        File androidImpl = new File(srcDir, "com/codename1/impl/android");
        File playServicesClassFile = getPlayServicesJavaSourceFile(srcDir, playServicesVersion);
        String playServicesClassName = playServicesClassFile.getName().substring(0, playServicesClassFile.getName().indexOf("."));

        // Delete all of the PlayServices_X_X_X files that we aren't going to use
        for (File f : androidImpl.listFiles()) {
            if (f.getName().startsWith("PlayServices_") && f.getName().endsWith(".java")) {
                if (!f.equals(playServicesClassFile)) {
                    f.delete();
                }

            }
        }
        if (!playServicesClassFile.getName().equals("PlayServices.java")) {
            // We will change the instance of the PlayServices class used to the most recent one we selected
            // The AndroidImplementation class has call to PlayServices.setInstance(...) in its init 
            // method which we will update here.
            File androidImplementation = new File(androidImpl, "AndroidImplementation.java");
            try {
                if (playServicesLocation) {
                    replaceInFile(androidImplementation, "new PlayServices()", "new com.codename1.impl.android." + playServicesClassName + "()");
                    replaceInFile(androidImplementation, "new com.codename1.impl.android.PlayServices()", "new com.codename1.impl.android." + playServicesClassName + "()");
                } else {
                    replaceInFile(androidImplementation, "PlayServices.setInstance(", "//PlayServices.setInstance(");
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to inject settings into PlayServices class.", ex);
            }

        }

        if (targetSDKVersionInt >= 29) {
            File androidLocationPlayServicesManager = new File(srcDir, "com/codename1/location/AndroidLocationPlayServicesManager.java");
            if (androidLocationPlayServicesManager.exists()) {
                try {
                    replaceInFile(androidLocationPlayServicesManager, "//29+", "");
                } catch (IOException ex) {
                    throw new BuildException("Failed to activate lines in "+androidLocationPlayServicesManager+" for API 29+");
                }
            }
        }
        xQueries = "";
        if (targetSDKVersionInt >= 30) {
            xQueries = "<queries>\n" + request.getArg("android.manifest.queries", "") + "</queries>\n";
        }

        //Delete the Facebook implemetation if this app does not use FB.
        if (!facebookSupported) {
            File fb = new File(srcDir, "com/codename1/social/FacebookImpl.java");
            fb.delete();
        } else {
            // special case for pubnub that includes a cn1lib for json that masks the one defined in Android
            File json = new File(dummyClassesDir, "org/json");
            if (json.exists()) {
                delTree(json);
            }
        }
        if (!playServicesLocation) {
            File fb = new File(srcDir, "com/codename1/location/AndroidLocationPlayServiceManager.java");
            fb.delete();
            fb = new File(srcDir, "com/codename1/location/BackgroundLocationHandler.java");
            fb.delete();
            fb = new File(srcDir, "com/codename1/location/BackgroundLocationBroadcastReceiver.java");
            fb.delete();
            fb = new File(srcDir, "com/codename1/location/GeofenceHandler.java");
            fb.delete();
            fb = new File(srcDir, "com/codename1/location/CodenameOneBackgroundLocationActivity.java");
            fb.delete();

            for (File f : androidImpl.listFiles()) {
                if (f.getName().startsWith("PlayServices_") && f.getName().endsWith(".java")) {
                    f.delete();
                } else if (f.getName().equals("PlayServices.java")) {
                    f.delete();
                }
            }

        }

        if (!shouldIncludeGoogleImpl) {
            File fb = new File(srcDir, "com/codename1/social/GoogleImpl.java");
            fb.delete();
        }

        final String moPubAdUnitId = request.getArg("android.mopubId", null);
        if (moPubAdUnitId != null && moPubAdUnitId.length() > 0) {
            integrateMoPub = true;
        }



        if (request.getArg("android.textureView", "false").equals("true")) {
            File impl = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "impl" + File.separator + "android" + File.separator + "AndroidImplementation.java");
            try {
                replaceInFile(impl, "public static boolean textureView = false;", "public static boolean textureView = true;");
            } catch (IOException ex) {
                throw new BuildException("Failed to process android.textureView build hint", ex);
            }
        }

        if (request.getArg("android.hideStatusBar", "false").equals("true")) {
            File impl = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "impl" + File.separator + "android" + File.separator + "AndroidImplementation.java");
            try {
                replaceInFile(impl, "statusBarHidden;", "statusBarHidden = true;");
            } catch (IOException ex) {
                throw new BuildException("Failed to process android.hideStatusBar build hint", ex);
            }
        }

        if (request.getArg("android.asyncPaint", "true").equals("true")) {
            File impl = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "impl" + File.separator + "android" + File.separator + "AndroidImplementation.java");
            try {
                replaceInFile(impl, "public static boolean asyncView = false;", "public static boolean asyncView = true;");
            } catch (IOException ex) {
                throw new BuildException("Failed to process android.asyncPaint build hint", ex);
            }
        }

        if(request.getArg("android.keyboardOpen", "true").equals("true")) {
            File impl = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "impl" + File.separator + "android" + File.separator + "AndroidImplementation.java");
            try {
                replaceInFile(impl, "private boolean asyncEditMode = false;", "private boolean asyncEditMode = true;");
            } catch (IOException ex) {
                throw new BuildException("Failed to process android.keyboardOpen build hint", ex);
            }
        }

        //String sdkVersion = request.getArg("android.targetSDKVersion", defaultVersion);
        if (targetNumber != null) {
            if (Integer.parseInt(targetNumber) >= 17) {
                try {
                    File androidBrowserComponentCallback = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "impl" + File.separator + "android" + File.separator + "AndroidBrowserComponentCallback.java");
                    replaceInFile(androidBrowserComponentCallback, "//import android.webkit.JavascriptInterface;", "import android.webkit.JavascriptInterface;");
                    replaceInFile(androidBrowserComponentCallback, "//@JavascriptInterface", "@JavascriptInterface");
                } catch (Exception e) {
                    //swallow this and continue.
                }
            }
        }

        File drawableDir = new File(resDir, "drawable");
        drawableDir.mkdirs();
        File drawableHdpiDir = new File(resDir, "drawable-hdpi");
        drawableHdpiDir.mkdirs();
        File drawableLdpiDir = new File(resDir, "drawable-ldpi");
        drawableLdpiDir.mkdirs();
        File drawableMdpiDir = new File(resDir, "drawable-mdpi");
        drawableMdpiDir.mkdirs();
        File drawableXhdpiDir = new File(resDir, "drawable-xhdpi");
        drawableXhdpiDir.mkdirs();
        File drawableXXhdpiDir = new File(resDir, "drawable-xxhdpi");
        drawableXXhdpiDir.mkdirs();
        File drawableXXXhdpiDir = new File(resDir, "drawable-xxxhdpi");
        drawableXXXhdpiDir.mkdirs();

        try {
            BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(request.getIcon()));
            createIconFile(new File(drawableDir, "icon.png"), iconImage, 128, 128);
            createIconFile(new File(drawableHdpiDir, "icon.png"), iconImage, 72, 72);
            createIconFile(new File(drawableLdpiDir, "icon.png"), iconImage, 36, 36);
            createIconFile(new File(drawableMdpiDir, "icon.png"), iconImage, 48, 48);
            createIconFile(new File(drawableXhdpiDir, "icon.png"), iconImage, 96, 96);
            createIconFile(new File(drawableXXhdpiDir, "icon.png"), iconImage, 144, 144);
            createIconFile(new File(drawableXXXhdpiDir, "icon.png"), iconImage, 192, 192);

            File notifFile = new File(assetsDir, "ic_stat_notify.png");
            if (notifFile.exists()) {
                BufferedImage bi = ImageIO.read(notifFile);
                createIconFile(new File(drawableDir, "ic_stat_notify.png"), bi, 24, 24);
                notifFile.delete();
            } else {
                //try to remove the background for the icon, because android 5 will mask the nottification icon
                //with white
                if (Integer.parseInt(targetNumber) >= 21) {
                    //notification small icon
                    Image img = makeColorTransparent(iconImage, new Color(iconImage.getRGB(2, 2)));
                    BufferedImage notifSmallIcon = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D bGr = notifSmallIcon.createGraphics();
                    bGr.drawImage(img, 0, 0, null);
                    bGr.dispose();
                    iconImage = notifSmallIcon;
                }
                createIconFile(new File(drawableDir, "ic_stat_notify.png"), iconImage, 24, 24);
            }
        } catch (IOException ex) {
            throw new BuildException("Failed to generate icon files", ex);
        }

        if (!purchasePermissions) {
            File billingSupport = new File(srcDir, path("com", "codename1", "impl", "android", "BillingSupport.java"));
            if (billingSupport.exists()) {
                billingSupport.delete();
            }
        }

        try {
            zipDir(new File(libsDir, "userClasses.jar").getAbsolutePath(), dummyClassesDir.getAbsolutePath());
        } catch (Exception ex) {
            throw new BuildException("Failed to create userClasses.jar", ex);
        }






        File stringsFile = new File(valsDir, "strings.xml");

        String stringsFileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "    <string name=\"app_name\">" + xmlize(request.getDisplayName()).replace("'", "\\'") + "</string>\n"
                + additionalKeyVals
                + request.getArg("android.stringsXml", "")
                + "</resources>";

        try {
            OutputStream stringsSourceStream = new FileOutputStream(stringsFile);
            stringsSourceStream.write(stringsFileContent.getBytes());
            stringsSourceStream.close();


            String locales = request.getArg("android.locales", null);
            if (locales != null && locales.length() > 0) {
                for (String loc : locales.split(";")) {
                    File currentValuesDir = new File(valsDir.getParent(), "values-" + loc);
                    currentValuesDir.mkdirs();
                    File currentStringsFile = new File(currentValuesDir, "strings.xml");
                    stringsSourceStream = new FileOutputStream(currentStringsFile);
                    stringsSourceStream.write(stringsFileContent.getBytes());
                    stringsSourceStream.close();
                }
            }
        } catch (IOException ex) {
            error("Failed to generate strings file", ex);
            throw new BuildException("Failed to generate strings file "+stringsFile, ex);
        }

        //declare the android native theme.
        File stylesFile = new File(valsDir, "styles.xml");

        File colors = new File(valsDir, "colors.xml");
        String colorsStr = "";
        if (colors.exists()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            try {
                //Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(colors);
                NodeList nl = dom.getElementsByTagName("color");
                for (int i = 0; i < nl.getLength(); i++) {
                    Node color = nl.item(i);
                    NamedNodeMap attr = color.getAttributes();
                    Node key = attr.getNamedItem("name");
                    String k = key.getNodeValue();
                    colorsStr += "<item name=\"android:" + k + "\">@color/" + k + "</item>\n";
                }
            } catch (Exception e) {
                error("Failed to create DocumentBuilder", e);
            }
        }

        String themeName = "android:Theme.Black";
        String itemName = androidAppBundle ? "cn1Style" : "attr/cn1Style";

        String stylesFileContent  = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<resources>\n" +
                    "    <style name=\"CustomTheme\" parent=\"" + themeName + "\">\n" +
                    "        <item name=\"" + itemName + "\">@style/CN1.EditText.Style</item>\n" +
                    "    </style>\n" +
                    "    <attr name=\"cn1Style\" format=\"reference\" />\n" +
                    "    <style name=\"CN1.EditText.Style\" parent=\"@android:style/Widget.EditText\">\n" +
                    "        <item name=\"android:textCursorDrawable\">@null</item>\n" +
                    "    </style>\n" +
                    request.getArg("android.style", "") +
                    "</resources>";

        try {
            OutputStream stylesSourceStream = new FileOutputStream(stylesFile);
            stylesSourceStream.write(stylesFileContent.getBytes());
            stylesSourceStream.close();

            String theme = request.getArg("android.theme", "Light");
            if (theme.length() > 0 && theme.equalsIgnoreCase("Dark")) {
                theme = "";
            } else {
                theme = "." + theme;
            }

            File styles11File = new File(vals11Dir, "styles.xml");
            String styles11FileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<resources>\n" +
                        "    <style name=\"CustomTheme\" parent=\"@android:style/Theme.Holo" + theme + "\">\n" +
                        "        <item name=\"" + itemName + "\">@style/CN1.EditText.Style</item>\n" +
                        "        <item name=\"android:windowActionBar\">false</item>\n" +
                        "        <item name=\"android:windowTitleSize\">0dp</item>\n" +
                        "    </style>\n" +
                        "    <style name=\"CN1.EditText.Style\" parent=\"@android:style/Widget.EditText\">\n" +
                        "        <item name=\"android:textCursorDrawable\">@null</item>\n" +
                        "    </style>\n" +
                        "</resources>\n";


            OutputStream styles11SourceStream = new FileOutputStream(styles11File);
            styles11SourceStream.write(styles11FileContent.getBytes());
            styles11SourceStream.close();

            File styles21File = new File(vals21Dir, "styles.xml");
            String styles21FileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<resources>\n" +
                        "    <style name=\"CustomTheme\" parent=\"@android:style/Theme.Material" + theme + "\">\n" +
                        "        <item name=\"" + itemName + "\">@style/CN1.EditText.Style</item>\n" +
                        "        <item name=\"android:windowActionBar\">false</item>\n" +
                        "        <item name=\"android:windowTitleSize\">0dp</item>\n" +
                        colorsStr +
                        "   </style>\n" +
                        "    <style name=\"CN1.EditText.Style\" parent=\"@android:style/Widget.EditText\">\n" +
                        "        <item name=\"android:textCursorDrawable\">@null</item>\n" +
                        "    </style>\n" +
                        "</resources>\n";


            OutputStream styles21SourceStream = new FileOutputStream(styles21File);
            styles21SourceStream.write(styles21FileContent.getBytes());
            styles21SourceStream.close();
        } catch (IOException ex) {
            error("Failed to generate style files", ex);
            throw new BuildException("Failed to generate styles files", ex);
        }

        try {
            File layoutFile = new File(layoutDir, "main.xml");
            String layoutFileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                    + "    android:layout_width=\"fill_parent\"\n"
                    + "    android:layout_height=\"fill_parent\"\n"
                    + request.getArg("android.xlayout_attr", "")
                    + "    android:background=\"#ff000000\" >\n"
                    + mopubBannerXML
                    + "</RelativeLayout>\n";
            OutputStream layoutSourceStream = new FileOutputStream(layoutFile);
            layoutSourceStream.write(layoutFileContent.getBytes());
            layoutSourceStream.close();

            String customLayout = request.getArg("android.cusom_layout1", null);
            int counter = 1;
            while (customLayout != null) {
                File customFile = new File(layoutDir, "cusom_layout" + counter + ".xml");
                layoutSourceStream = new FileOutputStream(customFile);
                layoutSourceStream.write(customLayout.getBytes());
                layoutSourceStream.close();

                counter++;
                customLayout = request.getArg("android.cusom_layout" + counter, null);
            }
        } catch (IOException ex) {
            throw new BuildException("Failed to generate layout XML file", ex);
        }

        String storeIds = request.getArg("android.store_ids", null);
        if (storeIds != null) {
            String[] sp = storeIds.split(";");
            storeIds = "";
            for (String s : sp) {
                storeIds += "        Display.getInstance().setProperty(\"" + s + "\", \"\" + " + s + ");\n";
            }
        } else {
            storeIds = "";
        }

        String gcmSenderId = request.getArg("gcm.sender_id", null);
        if (gcmSenderId != null) {
            gcmSenderId = "        Display.getInstance().setProperty(\"gcm.sender_id\", \"" + gcmSenderId + "\");\n";
        } else {
            if (googleServicesJson != null && googleServicesJson.exists()) {
                try {
                    JSONParser parser = new JSONParser();

                    Map<String, Object> parsedJson = parser.parseJSON(new FileReader(googleServicesJson));

                    Map projectInfo = (Map) parsedJson.get("project_info");
                    gcmSenderId = (String) projectInfo.get("project_number");
                    if (gcmSenderId != null) {
                        gcmSenderId = "        Display.getInstance().setProperty(\"gcm.sender_id\", \"" + gcmSenderId + "\");\n";
                    } else {
                        gcmSenderId = "";
                    }
                } catch (IOException ex) {
                    throw new BuildException("Failed to parse the google services JSON file "+googleServicesJson);
                }
            } else {
                gcmSenderId = "";
            }
        }

        File manifestFile = new File(projectDir + "/src/main", "AndroidManifest.xml");
        float version = 1.0f;
        int intVersion = 1;
        try {
            version = Float.parseFloat(request.getVersion());
            String vcOverride = request.getArg("android.versionCode", null);
            if (vcOverride != null && vcOverride.length() > 0) {
                intVersion = Integer.parseInt(vcOverride);
            } else {
                intVersion = Math.round(100 * version);
            }
        } catch (Throwable thrown) {
        }

        String locationServices = "<activity android:name=\"com.codename1.location.CodenameOneBackgroundLocationActivity\" android:theme=\"@android:style/Theme.NoDisplay\" android:exported=\"true\"/>\n"
                + "<service android:name=\"com.codename1.location.BackgroundLocationHandler\" android:exported=\"false\" />\n"
                + "<service android:name=\"com.codename1.location.GeofenceHandler\" android:exported=\"false\" />\n";
        String mediaService = "<service android:name=\"com.codename1.media.AudioService\" android:exported=\"false\" />";
        String remoteControlService = "<service android:name=\"com.codename1.media.BackgroundAudioService\"  android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.MEDIA_BUTTON\" />\n" +
                "                <action android:name=\"android.media.AUDIO_BECOMING_NOISY\" />\n" +
                "                <action android:name=\"android.media.browse.MediaBrowserService\" />\n" +
                "            </intent-filter>\n" +
                "        </service>";

        String mediabuttonReceiver = "<receiver android:name=\""+xclass("android.support.v4.media.session.MediaButtonReceiver")+"\" android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.MEDIA_BUTTON\" />\n" +
                "                <action android:name=\"android.media.AUDIO_BECOMING_NOISY\" />\n" +
                "            </intent-filter>\n" +
                "        </receiver>";
        if (!addRemoteControlService) {
            remoteControlService = "";
            mediabuttonReceiver = "";
        }
        String alarmRecevier = "<receiver android:name=\"com.codename1.impl.android.LocalNotificationPublisher\" android:exported=\"false\"></receiver>\n";
        String backgroundLocationReceiver = "<receiver android:name=\"com.codename1.location.BackgroundLocationBroadcastReceiver\" android:exported=\"true\"></receiver>\n";
        if (!playServicesLocation) {
            backgroundLocationReceiver = "";
        }
        String backgroundFetchService = "<service android:name=\"com.codename1.impl.android.BackgroundFetchHandler\" android:exported=\"false\" />\n"+
                "<activity android:name=\"com.codename1.impl.android.CodenameOneBackgroundFetchActivity\" android:theme=\"@android:style/Theme.NoDisplay\" android:exported=\"true\"/>\n";


        if (foregroundServicePermission) {
            permissions += permissionAdd(request, "\"android.permission.FOREGROUND_SERVICE\"",
                    "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />\n");
        }

        if (capturePermission) {
            String andc = request.getArg("android.captureRecord", "enabled");
            if (request.getArg("and.captureRecord", andc).equals("enabled")) {
                permissions += permissionAdd(request, "\"android.hardware.camera\"", "<uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />\n")
                        + permissionAdd(request, "RECORD_AUDIO",  "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" android:required=\"false\" />\n");
            } else {
                permissions += permissionAdd(request, "\"android.hardware.camera\"", "<uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />\n");
            }
        }
        if (vibratePermission) {
            permissions += permissionAdd(request, "VIBRATE",
                    "    <uses-permission android:name=\"android.permission.VIBRATE\" android:required=\"false\" />\n");
        }
        if (smsPermission) {
            permissions += permissionAdd(request, "SEND_SMS",
                    "<uses-permission android:name=\"android.permission.SEND_SMS\" android:required=\"false\" />\n");
        }
        if (gpsPermission) {
            permissions += "    <uses-feature android:name=\"android.hardware.location\" android:required=\"false\" />\n"
                    + "    <uses-feature android:name=\"android.hardware.location.gps\" android:required=\"false\" />\n"
                    + permissionAdd(request, "ACCESS_FINE_LOCATION",
                    "    <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" android:required=\"false\" />\n")
                    + permissionAdd(request, "ACCESS_COARSE_LOCATION",
                    "    <uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\"  android:required=\"false\" />\n");
            if(request.getArg("android.mockLocation", "true").equals("true")) {
                permissions += permissionAdd(request, "ACCESS_MOCK_LOCATION",
                        "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\"  android:required=\"false\" />\n");
            }
        }
        if (pushPermission && !useFCM) {
            permissions += "<permission android:name=\"" + request.getPackageName() + ".permission.C2D_MESSAGE\" android:protectionLevel=\"signature\" />\n"
                    + "    <uses-permission android:name=\"" + request.getPackageName() + ".permission.C2D_MESSAGE\" />\n"
                    + "    <uses-permission android:name=\"com.google.android.c2dm.permission.RECEIVE\" />\n";
            //+ permissionAdd(request, "RECEIVE_BOOT_COMPLETED",
            //        "    <uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" android:required=\"false\" />\n");
        }
        if (contactsReadPermission) {
            permissions += permissionAdd(request, "READ_CONTACTS",
                    "    <uses-permission android:name=\"android.permission.READ_CONTACTS\" android:required=\"false\" />\n");
        }
        if (contactsWritePermission) {
            permissions += permissionAdd(request, "WRITE_CONTACTS",
                    "    <uses-permission android:name=\"android.permission.WRITE_CONTACTS\" android:required=\"false\" />\n");
        }

        if (accessWifiStatePermissions) {
            permissions += permissionAdd(request, "ACCESS_WIFI_STATE",
                    "<uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" android:required=\"false\" />\n");
        }
        if (browserBookmarksPermissions) {
            permissions += "<uses-permission android:name=\"com.android.browser.permission.WRITE_HISTORY_BOOKMARKS\" android:required=\"false\"/>\n"
                    + "<uses-permission android:name=\"com.android.browser.permission.READ_HISTORY_BOOKMARKS\" android:required=\"false\"/>\n";
        }
        if (launcherPermissions) {
            permissions += "<uses-permission android:name=\"com.android.launcher.permission.INSTALL_SHORTCUT\"/>\n"
                    + "<uses-permission android:name=\"com.android.launcher.permission.UNINSTALL_SHORTCUT\"/>\n"
                    + "<uses-permission android:name=\"com.android.launcher.permission.READ_SETTINGS\"/>\n"
                    + "<!--device specific permissions -->\n"
                    + "<uses-permission android:name=\"com.htc.launcher.permission.READ_SETTINGS\"/>\n"
                    + "<uses-permission android:name=\"com.motorola.launcher.permission.READ_SETTINGS\"/>\n"
                    + "<uses-permission android:name=\"com.motorola.dlauncher.permission.READ_SETTINGS\"/>\n"
                    + "<uses-permission android:name=\"com.fede.launcher.permission.READ_SETTINGS\"/>\n"
                    + "<uses-permission android:name=\"com.lge.launcher.permission.READ_SETTINGS\"/>\n"
                    + "<uses-permission android:name=\"org.adw.launcher.permission.READ_SETTINGS\"/>\n"
                    + "<uses-permission android:name=\"com.motorola.launcher.permission.INSTALL_SHORTCUT\"/>\n"
                    + "<uses-permission android:name=\"com.motorola.dlauncher.permission.INSTALL_SHORTCUT\"/>\n"
                    + "<uses-permission android:name=\"com.lge.launcher.permission.INSTALL_SHORTCUT\"/>\n";
        }

        if (recordAudio) {
            permissions += permissionAdd(request, "RECORD_AUDIO",
                    "<uses-permission android:name=\"android.permission.RECORD_AUDIO\" android:required=\"false\" />\n");
        }

        if (wakeLock) {
            permissions += permissionAdd(request, "WAKE_LOCK",
                    "<uses-permission android:name=\"android.permission.WAKE_LOCK\" android:required=\"false\" />\n");
        }

        if (phonePermission) {
            permissions += permissionAdd(request, "READ_PHONE_STATE",
                    "<uses-permission android:name=\"android.permission.READ_PHONE_STATE\" android:required=\"false\" />\n");
        }

        if (accessNetworkStatePermission) {
            permissions += permissionAdd(request, "ACCESS_NETWORK_STATE",
                    "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" android:required=\"false\" />\n");
        }

        if (recieveBootCompletedPermission) {
            permissions += permissionAdd(request, "RECEIVE_BOOT_COMPLETED",
                    "<uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" android:required=\"false\" />\n");
        }

        if (getAccountsPermission) {
            permissions += permissionAdd(request, "GET_ACCOUNTS",
                    "<uses-permission android:name=\"android.permission.GET_ACCOUNTS\" android:required=\"false\" />\n");
        }
        if (credentialsPermission) {
            permissions += permissionAdd(request, "USE_CREDENTIALS",
                    "<uses-permission android:name=\"android.permission.USE_CREDENTIALS\" />\n");
        }
        if (backgroundLocationPermission && !xPermissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION")) {
            permissions += "<uses-permission android:name=\"android.permission.ACCESS_BACKGROUND_LOCATION\"  android:required=\"false\" />\n";
        }

        String billingServiceData = "";
        String activityBillingSource = "";
        String consumable = "";
        if (purchasePermissions) {
            String k = request.getArg("android.licenseKey", null);
            //if the android.licenseKey is not defined abort the build
            if (k == null) {
                throw new BuildException("android.licenseKey must be defined in the build hints, grab the key from the \"Monetization setup\" section in the android dev portal" +
                        ", then paste the Base64-encoded RSA public key into the android.licenseKey build hint.\n\n");
            }
            String cons = request.getArg("android.nonconsumable", null);
            if (cons != null) {
                cons = cons.trim();
                if (cons.contains(",")) {
                    StringTokenizer token = new StringTokenizer(cons, ",");
                    if (token.countTokens() > 0) {
                        try {
                            while (token.hasMoreElements()) {
                                String t = (String) token.nextToken();
                                t = t.trim();
                                consumable += "\"" + t + "\",";
                            }
                            consumable = consumable.substring(0, consumable.length() - 1);
                        } catch (Exception e) {
                            //the pattern is not valid
                        }

                    }
                } else {
                    consumable = "\"" + cons + "\"";
                }
            }
            permissions += "    <uses-permission android:name=\"com.android.vending.BILLING\" android:required=\"false\" />\n";
            activityBillingSource
                    = "    protected boolean isBillingEnabled() {\n"
                    + "        return true;\n"
                    + "    }\n\n"
                    + "    protected com.codename1.impl.android.IBillingSupport createBillingSupport() {\n"
                    + "        return new com.codename1.impl.android.BillingSupport(this);\n"
                    + "    }\n\n";

        }


        String sharedUserId = request.getArg("android.sharedUserId", "");
        if (sharedUserId.length() > 0) {
            sharedUserId = "      android:sharedUserId=\"" + sharedUserId + "\"\n";
        }
        String sharedUserLabel = request.getArg("android.sharedUserLabel", "");
        if (sharedUserLabel.length() > 0) {
            sharedUserLabel = "      android:sharedUserLabel=\"" + sharedUserLabel + "\"\n";
        }

        String basePermissions = "    <uses-feature android:name=\"android.hardware.telephony\" android:required=\"" + telephonyRequired + "\" />\n"
                + "    <uses-permission android:name=\"android.permission.INTERNET\" android:required=\"false\" />\n";
        if (request.getArg("android.removeBasePermissions", "false").equals("true")) {
            basePermissions = "";
        }
        String externalStoragePermission = "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" android:required=\"false\" android:maxSdkVersion=\"32\" />\n";
        if (request.getArg("android.blockExternalStoragePermission", "false").equals("true")) {
            externalStoragePermission = "";
        }
        String xmlizedDisplayName = xmlize(request.getDisplayName());

        String applicationAttr = request.getArg("android.xapplication_attr", "");

        String allowBackup = " android:allowBackup=\"" + request.getArg("android.allowBackup", "true")  +"\" ";
        if(applicationAttr.contains("allowBackup")) {
            allowBackup = "";
        }

        String applicationNode = "  <application ";
        if (!applicationAttr.contains("android:label")) {
            applicationNode += " android:label=\"" + xmlizedDisplayName + "\" ";
        }
        if (!applicationAttr.contains("android:icon")) {
            applicationNode += " android:icon=\"@drawable/icon\" ";
        }
        if (request.getArg("android.multidex", "true").equals("true") && Integer.parseInt(minSDK) < 21) {
            debug("Setting Application node to MultiDexApplication because minSDK="+minSDK+" < 21");
            applicationNode += " android:name=\""+xclass("android.support.multidex.MultiDexApplication")+"\" ";
        }

        applicationNode += applicationAttr;
        applicationNode += allowBackup;
        applicationNode += ">\n";


        // Note: It is OK to reference android.support.FILE_PROVIDER_PATHS in android X still
        // https://stackoverflow.com/a/57584508/2935174
        String providerTag = "<provider\n" +
                "          android:name=\""+xclass("android.support.v4.content.FileProvider")+"\"\n" +
                "          android:authorities=\"${applicationId}.provider\"\n" +
                "          android:exported=\"false\"\n" +
                "          android:grantUriPermissions=\"true\">\n" +
                "          <meta-data\n" +
                "              android:name=\"android.support.FILE_PROVIDER_PATHS\"\n" +
                "              android:resource=\"@xml/file_paths\">\n" +
                "          </meta-data>\n" +
                "      </provider>";

        if (!providerTag.isEmpty()) {
            File filePathsFile = new File(xmlDir, "file_paths.xml");

            String filePathsContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<paths xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                    "    <cache-path name=\"intent_files\" path=\"intent_files/\" />\n" +
                    request.getArg("android.file_paths", "    <files-path name=\"app_files\" path=\".\" />") +
                    "</paths>";

            try {
                OutputStream filePathsStream = new FileOutputStream(filePathsFile);

                filePathsStream.write(filePathsContent.getBytes());
                filePathsStream.close();
            } catch (IOException ex) {
                throw new BuildException("Failed to write file path providers file", ex);
            }
        }

        String pushManifestEntries = "        <service android:name=\"PushNotificationService\" android:exported=\"true\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"" + request.getPackageName() + ".PushNotificationService\" />\n"
                + "            </intent-filter>\n"
                + "        </service>\n"
                + "        <receiver android:name=\".PushReceiver\" android:permission=\"com.google.android.c2dm.permission.SEND\" android:exported=\"true\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"com.google.android.c2dm.intent.RECEIVE\" />\n"
                + "                <category android:name=\"" + request.getPackageName() + "\" />\n"
                + "            </intent-filter>\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"com.google.android.c2dm.intent.REGISTRATION\" />\n"
                + "                <category android:name=\"" + request.getPackageName() + "\" />\n"
                + "            </intent-filter>\n"

                + "        </receiver>\n";
        if (!pushPermission) {
            pushManifestEntries = "";
        } else if (useFCM) {
            pushManifestEntries = "<service\n" +
                    "          android:name=\"com.codename1.impl.android.CN1FirebaseMessagingService\" android:exported=\"true\">\n" +
                    "          <intent-filter>\n" +
                    "              <action android:name=\"com.google.firebase.MESSAGING_EVENT\" />\n" +
                    "          </intent-filter>\n" +
                    "      </service>\n";
        }

        String launchMode = request.getArg("android.activity.launchMode", "singleTop");
        String xActivity = request.getArg("android.xactivity", "");
        if (!xActivity.contains("android:exported")) {
            xActivity += " android:exported=\"true\"";
        }
        String manifestSource
                = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "      package=\"" + request.getPackageName() + "\"\n"
                + "      android:versionCode=\"" + intVersion + "\"\n"
                + "      android:versionName=\"" + request.getVersion() + "\"\n"
                + "      xmlns:tools=\"http://schemas.android.com/tools\"\n"
                + sharedUserLabel
                + sharedUserId
                + "      android:minSdkVersion=\"" + minSDK + "\"\n"
                + "      android:installLocation=\"" + request.getArg("android.installLocation", "auto") + "\">\n"
                + "    <uses-sdk android:minSdkVersion=\"" + minSDK + "\""
                + targetSDKVersion
                + request.getArg("android.xmanifest", "")
                + " />\n"
                + "    <supports-screens android:smallScreens=\"" + request.getArg("android.smallScreens", "true") + "\"\n"
                + "          android:normalScreens=\"" + request.getArg("android.normalScreens", "true") + "\"\n"
                + "          android:largeScreens=\"" + request.getArg("android.largeScreens", "true") + "\"\n"
                + "          android:xlargeScreens=\"" + request.getArg("android.xlargeScreens", "true") +"\"\n"
                +            request.getArg("android.supportScreens", "")
                + "          android:anyDensity=\"" + request.getArg("android.anyDensity", "true") + "\" />\n"
                + applicationNode
                + providerTag
                + usesLibrary
                + googlePlayAdsMetaData
                + "        <activity android:name=\"" + request.getMainClass() + "Stub\"\n"
                + xActivity
                + "                  android:theme=\"@style/CustomTheme\"\n"
                + "                  android:configChanges=\"orientation|keyboardHidden|screenSize|smallestScreenSize|screenLayout\"\n"
                + "                  android:launchMode=\""+launchMode+"\"\n"
                + "                  android:label=\"" + xmlizedDisplayName + "\" >\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "            </intent-filter>\n"
                + request.getArg("android.xintent_filter", "")
                + "        </activity>\n"
                + facebookActivityMetaData
                + facebookActivity
                + googlePlayAdsActivity
                + pushManifestEntries
                + billingServiceData
                + "  " + request.getArg("android.xapplication", "")
                + mopubActivities
                + alarmRecevier
                + backgroundLocationReceiver
                + mediabuttonReceiver
                + backgroundFetchService
                + locationServices
                + mediaService
                + remoteControlService
                + "    </application>\n"
                + "    <uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />\n"
                + basePermissions
                + externalStoragePermission
                + permissions
                + "  " + xPermissions
                + "  " + xQueries
                + "</manifest>\n";
        try {
            OutputStream manifestSourceStream = new FileOutputStream(manifestFile);
            manifestSourceStream.write(manifestSource.getBytes());
            manifestSourceStream.close();
        } catch (IOException ex) {
            throw new BuildException("Failed to write manifest file", ex);
        }
        debug("Generated manifest file: " + manifestSource);

        String oncreate = request.getArg("android.onCreate", "");

        String initStackSize = "";
        String stackSize = request.getArg("android.stack_size", null);
        if (stackSize != null) {
            initStackSize = "        com.codename1.impl.CodenameOneThread.STACK_FRAME_SIZE = " + stackSize + ";\n";
        }

        String licenseKey = request.getArg("android.licenseKey", null);
        String androidLicenseKey = licenseKey;
        if (androidLicenseKey != null) {
            androidLicenseKey = "Display.getInstance().setProperty(\"android.licenseKey\", \"" + androidLicenseKey + "\");\n";
        } else {
            androidLicenseKey = "";
        }
        String useBackgroundPermissionSnippet = "";
        if (backgroundLocationPermission) {
            useBackgroundPermissionSnippet = "Display.getInstance().setProperty(\"android.requiresBackgroundLocationPermissionForAPI29\", \"true\");\n";
        }

        String streamMode = request.getArg("android.streamMode", null);
        if (streamMode != null) {
            if (streamMode.equals("music")) {
                streamMode = "        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);\n";
            } else {
                streamMode = "";
            }
        } else {
            streamMode = "";
        }


        String localNotificationCode = "";

        localNotificationCode = ""
                + "        if(i instanceof com.codename1.notifications.LocalNotificationCallback){\n"
                + "            Intent intent = getIntent();\n"
                + "            if(intent != null && intent.getExtras() != null && intent.getExtras().containsKey(\"LocalNotificationID\")){\n"
                + "                String id = intent.getExtras().getString(\"LocalNotificationID\");\n"
                + "                intent.removeExtra(\"LocalNotificationID\");\n"
                + "                ((com.codename1.notifications.LocalNotificationCallback)i).localNotificationReceived(id);\n"
                + "            }\n"
                + "        }\n";


        String reinitCode0 = "Display.init(this);\n";

        reinitCode0 = "AndroidImplementation.startContext(this);\n";

        String reinitCode = "Display.init(this);\n";

        // We need to explicitly call initImpl() to setup the activity in case the
        // last used context is a service, since Display.init() won't actually do
        // a reinitialize in this case.
        // We don't want to actually call deinitialize here because this is too heavy-handed,
        // (if the service is running, this will create a new implementation and edt thread
        // which will cause problems for existing background procresses.
        // Doing it this way ensures that the EDT and implemenation objects will remain unchanged,
        // but other things will be set up properly.
        reinitCode = "AndroidImplementation.startContext(this);\n";

        String waitingForPermissionsRequestOnStop = "        if (isWaitingForPermissionResult()) {\n" +
                        "            return;\n" +
                        "        }\n";

        String onStopCode = "protected void onStop() {\n"
                + "        super.onStop();\n"
                + waitingForPermissionsRequestOnStop
                + "        if(isWaitingForResult()){\n"
                + "             return;\n"
                + "        }\n"
                + "        synchronized(LOCK) {\n"
                + "             currentForm = null;\n"
                + "        }\n"
                + "        Display.getInstance().callSerially(new Runnable() { public void run() {i.stop();} });\n"
                + "        running = false;\n"
                + "    }\n\n";


            // Added a bit of blocking to onStop() to prevent onDestroy() from being
            // run before stop() is completed.  This probably only shows up if the 
            // device is very low on RAM or is set to not keep activity due 
            // to developer options... but it is still better to finish onStop()
            // before onDestroy() is run.
            onStopCode = "protected void onStop() {\n";

                onStopCode += "        com.codename1.impl.android.AndroidImplementation.writeServiceProperties(this);\n";

            onStopCode +=
                    "        super.onStop();\n" +
                            "        if(isWaitingForResult()){\n" +
                            "             return;\n" +
                            "        }\n" +
                            "        synchronized(LOCK) {\n" +
                            "             currentForm = null;\n" +
                            "        }\n" +
                            "        final boolean[] complete = new boolean[1];\n" +
                            "\n" +
                            "        Display.getInstance().callSerially(new Runnable() {\n" +
                            "            public void run() {\n" +
                            "                i.stop();\n" +
                            "                synchronized(complete) {\n" +
                            "                    try {\n" +
                            "                        complete[0] = true;\n" +
                            "                        complete.notify();\n" +
                            "                    } catch (Exception ex) {\n" +
                            "                    }\n" +
                            "                }\n" +
                            "            }\n" +
                            "        });\n" +
                            "        while (!complete[0]) {\n" +
                            "            synchronized(complete) {\n" +
                            "                try {\n" +
                            "                    complete.wait(500);\n" +
                            "                } catch (Exception ex){}\n" +
                            "            }\n" +
                            "        }\n" +
                            "        running = false;\n" +
                            "    }\n\n";


        String onDestroyCode = "    protected void onDestroy() {\n"
                + createOnDestroyCode(request)
                + "        super.onDestroy();\n"
                + "        Display.getInstance().callSerially(new Runnable() { public void run() {i.destroy(); Display.deinitialize();} });\n"
                + "        running = false;\n"
                + "    }\n";

            onDestroyCode = "protected void onDestroy() {\n" +
                    createOnDestroyCode(request) +
                    "        super.onDestroy();\n" +
                    "\n" +
                    "        Display.getInstance().callSerially(new Runnable() { public void run() {i.destroy();} });\n" +
                    "        AndroidImplementation.stopContext(this);\n" +
                    "        running = false;\n" +
                    "    }";


        File stubFileSourceFile = new File(stubFileSourceDir, request.getMainClass() + "Stub.java");
        String consumableCode;
        consumableCode = "public boolean isConsumable(String sku) {\n"
                + "  boolean retVal = super.isConsumable(sku);\n"
                + "  java.util.List l = new java.util.ArrayList();\n"
                + "  java.util.Collections.addAll(l, consumable);\n"
                + "  return retVal || l.contains(sku);\n"
                + "}\n";

        String firstTimeStatic = "";

            firstTimeStatic = " static";


        String notificationChannelId = request.getArg("android.NotificationChannel.id", "cn1-channel");
        String notificationChannelName = request.getArg("android.NotificationChannel.name", "Notifications");
        String notificationChannelDescription = request.getArg("android.NotificationChannel.description", "Remote notifications");

        // https://developer.android.com/reference/android/app/NotificationManager#IMPORTANCE_LOW
        String notificationChannelImportance = request.getArg("android.NotificationChannel.importance", "2");

        String notificationChannelEnableLights = request.getArg("android.NotificationChannel.enableLights", "true");
        String notificationChannelLightColor = request.getArg("android.NotificationChannel.lightColor", ""+0xffff0000);
        String notificationChannelEnableVibration = request.getArg("android.NotificationChannel.enableVibration", "false");
        String notificationChannelVibrationPattern = request.getArg("android.NotificationChannel.vibrationPattern", request.getArg("android.pushVibratePattern", null));
        if (notificationChannelVibrationPattern != null) {
            notificationChannelVibrationPattern = "\""+notificationChannelVibrationPattern+"\"";
        }
        String pushInitDisplayProperties = "";
        if (buildToolsVersionInt >= 26 && Integer.parseInt(targetNumber) >= 26) {

            pushInitDisplayProperties = "        Display.getInstance().setProperty(\"android.NotificationChannel.id\", \""+notificationChannelId+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.name\", \""+notificationChannelName+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.description\", \""+notificationChannelDescription+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.importance\", \""+notificationChannelImportance+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.enableLights\", \""+notificationChannelEnableLights+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.lightColor\", \""+notificationChannelLightColor+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.enableVibration\", \""+notificationChannelEnableVibration+"\");\n"
                    + "        Display.getInstance().setProperty(\"android.NotificationChannel.vibrationPattern\", "+notificationChannelVibrationPattern+");\n"
                    + "        try {\n"
                    + "            Display.getInstance().setProperty(\"android.NotificationChannel.soundUri\", android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION).toString());\n"
                    + "        } catch (Exception ex){}\n";
            ;
            if (request.getArg("android.pushSound", null) != null) {
                pushInitDisplayProperties += "        try {\n"
                        + "            Display.getInstance().setProperty(\"android.NotificationChannel.soundUri\", \"android.resource://" + request.getPackageName() + "/raw/" + request.getArg("android.pushSound", null)+"\");\n"
                        + "        } catch (Exception ex){}\n";
            }

        }

        String waitingForPermissionsRequest=
                "        if (isWaitingForPermissionResult()) {\n" +
                        "            setWaitingForPermissionResult(false);\n" +
                        "            return;\n" +
                        "        }\n";


        String stubSourceCode;
        try {
            stubSourceCode = "package " + request.getPackageName() + ";\n\n"
                    + "import com.codename1.ui.*;\n"
                    + "import android.os.Bundle;\n"
                    + "import android.content.Intent;\n"
                    + "import android.view.KeyEvent;\n"
                    + "import com.codename1.system.*;\n"
                    + "import com.codename1.impl.android.CodenameOneActivity;\n"
                    + "import com.codename1.impl.android.AndroidImplementation;\n"
                    + "import com.codename1.system.NativeLookup;\n"
                    + "import com.codename1.push.*;\n"
                    + "import com.codename1.ui.*;\n"
                    + "import android.content.IntentFilter;\n"
                    + additionalImports
                    + "\n\n"
                    + "public class " + request.getMainClass() + "Stub extends " + request.getArg("android.customActivity", "CodenameOneActivity") + "{\n";
            stubSourceCode += decodeFunction();
            stubSourceCode += "    public static final String BUILD_KEY = \"" + xorEncode(getBuildKey()) + "\";\n"
                    + "    public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\";\n"
                    + "    public static final String BUILT_BY_USER = \"" + xorEncode(request.getUserName()) + "\";\n"
                    + "    public static final String LICENSE_KEY = \"" + xorEncode(licenseKey) + "\";\n"
                    + "    String [] consumable = new String[]{" + consumable + "};\n"
                    + "    private static " + request.getMainClass() + "Stub stubInstance;\n"
                    + "    private static " + request.getMainClass() + " i;\n"
                    + "    private boolean running;\n"
                    + "    private" + firstTimeStatic + " boolean firstTime = true;\n"
                    + "    private Form currentForm;\n"
                    + "    private static final Object LOCK = new Object();\n"
                    + additionalMembers
                    + headphonesVars
                    + "    public static " + request.getMainClass() + " getAppInstance() {\n"
                    + "        return i;\n"
                    + "    }\n\n"
                    + activityBillingSource
                    + "    protected Object getApp() {\n"
                    + "        return i;\n"
                    + "    }\n\n"
                    + "    public static " + request.getMainClass() + "Stub getInstance() {\n"
                    + "        return stubInstance;\n"
                    + "    }\n\n"
                    + "    public " + request.getMainClass() + "Stub() {\n"
                    + "        stubInstance = this;\n"
                    + "    }\n\n"
                    + "    public static boolean isRunning() {\n"
                    + "        return stubInstance != null && stubInstance.running;\n"
                    + "    }\n\n"
                    + "    public void onCreate(Bundle savedInstanceState) {\n"
                    + "        super.onCreate(savedInstanceState);\n"
                    + facebookHashCode
                    + facebookSupport
                    + streamMode
                    + registerNativeImplementationsAndCreateStubs(new URLClassLoader(new URL[]{codenameOneJar.toURI().toURL()}), srcDir, dummyClassesDir)
                    + oncreate + "\n"
                    + createOnCreateCode(request)
                    + "    }\n"
                    + "    protected void onResume() {\n"
                    + "        running = true;\n"
                    + "        super.onResume();\n"
                    + waitingForPermissionsRequest
                    + "        if(!Display.isInitialized()) {\n"
                    + initStackSize
                    + headphonesOnResume
                    + googlePlayAdViewCode
                    + reinitCode0
                    + storeIds
                    + gcmSenderId
                    + "        Display.getInstance().setProperty(\"build_key\", d(BUILD_KEY));\n"
                    + "        Display.getInstance().setProperty(\"package_name\", PACKAGE_NAME);\n"
                    + "        Display.getInstance().setProperty(\"built_by_user\", d(BUILT_BY_USER));\n"
                    + useBackgroundPermissionSnippet
                    + pushInitDisplayProperties
                    //+ corporateServer
                    + androidLicenseKey
                    //+ "        " + registerNativeImplementationsAndCreateStubs(srcDir, dummyClassesDir)
                    + "        " + createPostInitCode(request)
                    + "        }else{\n"
                    + reinitCode
                    + "        }\n"
                    + "        if (i == null) {\n"
                    + "          i = new " + request.getMainClass() + "();\n"
                    + "          if(i instanceof PushCallback) {\n"
                    + "                com.codename1.impl.CodenameOneImplementation.setPushCallback((PushCallback)i);\n"
                    + "          }\n";

            stubSourceCode +=
                    "           if (i instanceof com.codename1.push.PushActionsProvider) {\n"
                            + "                try{AndroidImplementation.installNotificationActionCategories((com.codename1.push.PushActionsProvider)i);}catch(java.io.IOException ex){ex.printStackTrace();}\n"
                            + "           }\n";

        } catch (Exception ex) {
            throw new BuildException("Failed to generate stub source code", ex);
        }


        String fcmRegisterPushCode = "";
        if (useFCM) {
            fcmRegisterPushCode = "try {\n" +
                    "\n" +
                    "                String token = com.google.firebase.iid.FirebaseInstanceId.getInstance().getToken();\n" +
                    "                if (token != null) {\n" +
                    "                    com.codename1.io.Preferences.set(\"push_key\", \"cn1-fcm-\"+token);\n" +
                    "                    if (i instanceof PushCallback) {\n" +
                    "                        ((PushCallback)i).registeredForPush(\"cn1-fcm-\"+token);\n" +
                    "                    }\n" +
                    "                } else {\n" +
                    "                    java.util.Timer timer = new java.util.Timer();\n" +
                    "                    timer.schedule(new java.util.TimerTask() {\n" +
                    "                        public void run() {\n" +
                    "                            runOnUiThread(new Runnable() {\n" +
                    "                                public void run() {\n" +
                    "                                    String token = com.google.firebase.iid.FirebaseInstanceId.getInstance().getToken();\n" +
                    "                                    if (token != null) {\n" +
                    "                                        com.codename1.io.Preferences.set(\"push_key\", \"cn1-fcm-\" + token);\n" +
                    "                                        if (i instanceof PushCallback) {\n" +
                    "                                            ((PushCallback) i).registeredForPush(\"cn1-fcm-\" + token);\n" +
                    "                                        }\n" +
                    "                                    }\n" +
                    "                                }\n" +
                    "                            });\n" +
                    "                        }\n" +
                    "                    }, 2000);\n" +
                    "                }\n" +
                    "            } catch (Exception ex) {\n" +
                    "                if (i instanceof PushCallback) {\n" +
                    "                    ((PushCallback)i).pushRegistrationError(\"Failed to register push: \"+ex.getMessage(), 0);\n" +
                    "                }\n" +
                    "                System.out.println(\"Failed to get fcm token.\");\n" +
                    "                ex.printStackTrace();\n" +
                    "            }";
        }
        try {
            stubSourceCode +=
                    "        }\n"
                            + "        if(i instanceof PushCallback) {\n"
                            + "            AndroidImplementation.firePendingPushes((PushCallback)i, this);\n"
                            + "        }\n"
                            + localNotificationCode
                            + "        Display.getInstance().callSerially(new Runnable(){\n"
                            + "            boolean wasStopped = (currentForm == null);\n"
                            + "            Form currForm = currentForm;\n"
                            + "            public void run() {\n"
                            + "                Form displayForm = Display.getInstance().getCurrent();\n"
                            + "                " + request.getMainClass() + "Stub.this.run(displayForm == null ? currForm : displayForm, wasStopped);\n"
                            + "            }\n"
                            + "        });\n"
                            + "        synchronized(LOCK) {\n"
                            + "            currentForm = null;\n"
                            + "        }\n"
                            + "    }\n\n"
                            + "    protected void onPause() {\n"
                            + "        super.onPause();\n"
                            + "        synchronized(LOCK) {\n"
                            + "            currentForm = Display.getInstance().getCurrent();\n"
                            + "        }\n"
                            + "        running = false;\n"
                            + "    }\n\n"
                            + "    public void run(Form currentForm, boolean wasStopped) {\n"
                            + "        if(firstTime) {\n"
                            + "            firstTime = false;\n"
                            + "            i.init(this);\n"
                            + fcmRegisterPushCode
                            + "         } else {\n"
                            + "             synchronized(LOCK) {\n"
                            + "                 if(!wasStopped) {\n"
                            + "                     if(currentForm instanceof Dialog) {\n"
                            + "                         ((Dialog)currentForm).showModeless();\n"
                            + "                     }else{\n"
                            + "                         currentForm.show();\n"
                            + "                     }\n"
                            + "                     fireIntentResult();\n"
                            + "                     setWaitingForResult(false);\n"
                            + "                     return;\n"
                            + "                 }\n"
                            + "             }\n"
                            + "         }\n"
                            + createStartInvocation(request, "i")
                            + "    }\n"
                            + onStopCode
                            + onDestroyCode
                            + " public boolean onKeyDown(int keyCode, KeyEvent event){\n"
                            + " return super.onKeyDown(keyCode, event);\n"
                            + " }\n"
                            + " public String getBase64EncodedPublicKey() {\n"
                            + "     return d(LICENSE_KEY);\n"
                            + " }\n"
                            + consumableCode
                            + "}\n";
        } catch (Exception ex) {
            throw new BuildException("Failure while generating stub source code", ex);
        }

        File androidImplDir = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "impl" + File.separator + "android");
        File stubUtilFile = new File(androidImplDir, "StubUtil.java");
        if (stubUtilFile.exists()) {
            try {
                replaceInFile(stubUtilFile, "//!", "");
                replaceInFile(stubUtilFile, "{{Stub}}", request.getPackageName() + "." + request.getMainClass() + "Stub");

            } catch (IOException ex) {
                throw new BuildException("Failed to update stub Util file", ex);
            }
        }
        boolean backgroundPushHandling = "true".equals(request.getArg("android.background_push_handling", "false"));
        if (!useFCM) {
            File pushServiceFileSourceFile = new File(stubFileSourceDir, "PushNotificationService.java");

            String pushServiceOnCreate = "";
            if (buildToolsVersionInt >= 26 && Integer.parseInt(targetNumber) >= 26) {
                pushServiceOnCreate = "\n    @Override\n" +
                        "    public void onCreate() {\n"
                        + "        super.onCreate();\n"
                        + "        setProperty(\"android.NotificationChannel.id\", \""+notificationChannelId+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.name\", \""+notificationChannelName+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.description\", \""+notificationChannelDescription+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.importance\", \""+notificationChannelImportance+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.enableLights\", \""+notificationChannelEnableLights+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.lightColor\", \""+notificationChannelLightColor+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.enableVibration\", \""+notificationChannelEnableVibration+"\");\n"
                        + "        setProperty(\"android.NotificationChannel.vibrationPattern\", "+notificationChannelVibrationPattern+");\n"

                        + "    }\n\n";

            }

            // The runtime test to use to determine if we should call the push() method immediately
            // upon receiving the notification.  By default, we only send *immediately* if the app is
            // in the foreground.  You can use the android.background_push_handling to allow
            // immediate handling in the background as well.

            String handlePushImmediatelyCheck = request.getMainClass() + "Stub.isRunning()";
            String stubIsRunningCheck = handlePushImmediatelyCheck;
            if (backgroundPushHandling) {
                handlePushImmediatelyCheck += " || Display.isInitialized()";
            }


            String pushServiceSourceCode = "package " + request.getPackageName() + ";\n\n"
                    + "import com.codename1.ui.*;\n"
                    + "import com.codename1.push.PushCallback;\n\n"
                    + "public class PushNotificationService extends com.codename1.impl.android.PushNotificationService {\n"
                    + "    public PushCallback getPushCallbackInstance() {\n"
                    + "         if(" + handlePushImmediatelyCheck + ") {\n"
                    + "             " + request.getMainClass() + "Stub stub = " + request.getMainClass() + "Stub.getInstance();\n"
                    + "             final " + request.getMainClass() + " main = stub.getAppInstance();\n"
                    + "             if(main instanceof PushCallback) {\n"
                    + "                 return (PushCallback)main;\n"
                    + "             }\n"
                    + "         }\n"
                    + "         return null;\n"
                    + "    }\n\n"
                    + "    public Class getStubClass() {\n"
                    + "        return " + request.getMainClass() + "Stub.class;\n"
                    + "    }\n"
                    + pushServiceOnCreate

                    + "}\n";

            File pushFileSourceFile = new File(stubFileSourceDir, "PushReceiver.java");

            String vibrateCode = "";
            if (request.getArg("android.pushVibratePattern", null) != null) {
                String pattern = request.getArg("android.pushVibratePattern", null);
                pattern = pattern.trim();
                StringTokenizer token = new StringTokenizer(pattern, ",");
                if (token.countTokens() > 0) {
                    try {
                        while (token.hasMoreElements()) {
                            String t = (String) token.nextToken();
                            t = t.trim();
                            Long.parseLong(t);
                        }
                        vibrateCode = "mNotifyBuilder.setVibrate(new long[]{" + pattern + "});";
                    } catch (Exception e) {
                        //the pattern is not valid
                    }

                }
            }

            String pushSound = "";
            if (request.getArg("android.pushSound", null) != null) {
                String soundPath = request.getArg("android.pushSound", null).toLowerCase();
                pushSound = "mNotifyBuilder.setSound(android.net.Uri.parse(\"android.resource://" + request.getPackageName() + "/raw/" + soundPath + "\"));";
            }

            String pushReceiverSourceCode = "package " + request.getPackageName() + ";\n\n"
                    + "import com.codename1.ui.*;\n\n"
                    + "import android.os.Bundle;\n\n"
                    + "import com.codename1.system.*;\n"
                    + "import com.codename1.impl.android.CodenameOneActivity;\n\n"
                    + "import com.codename1.impl.android.AndroidImplementation;\n\n"
                    + "import com.codename1.system.NativeLookup;\n\n"
                    + "import com.codename1.io.ConnectionRequest;\n\n"
                    + "import com.codename1.io.Preferences;\n\n"
                    + "import com.codename1.io.NetworkManager;\n\n"
                    + "import com.codename1.push.PushCallback;\n\n"
                    + "import java.io.InputStream;\n"
                    + "import java.io.DataInputStream;\n"
                    + "import java.io.IOException;\n"
                    + "import android.app.Notification;\n"
                    + "import android.app.NotificationManager;\n"
                    + "import android.app.PendingIntent;\n"
                    + "import android.content.BroadcastReceiver;\n"
                    + "import android.content.Context;\n"
                    + "import android.content.Intent;\n"
                    + "import android.telephony.TelephonyManager;\n"
                    + "import android.content.SharedPreferences.Editor;\n"
                    + "import android.app.NotificationManager\n;"
                    + "import android.app.Activity;\n"
                    + "import com.codename1.impl.android.PushNotificationService;\n"
                    + "import com.codename1.ui.*;\n"
                    + "import android.graphics.Bitmap;\n"
                    + "import android.graphics.drawable.BitmapDrawable;\n"
                    + "import android.graphics.drawable.Drawable;\n"
                    + "import "+xclass("android.support.v4.app.NotificationCompat")+".Builder;\n"
                    + "import "+xclass("android.support.v4.app.NotificationCompat")+";\n"
                    + "import android.media.RingtoneManager;\n"
                    + "import android.net.Uri;\n\n"
                    + "public class PushReceiver extends BroadcastReceiver {\n"
                    + "     public static final String C2DM_MESSAGE_TYPE_EXTRA = \"messageType\";\n"
                    + "     public static final String C2DM_MESSAGE_EXTRA = \"message\";\n"
                    + "     public static final String C2DM_MESSAGE_IMAGE = \"image\";\n"
                    + "     public static final String C2DM_MESSAGE_CATEGORY = \"category\";\n"
                    + "     public static final String BUILD_KEY = \"" + xorEncode(getBuildKey()) + "\"\n;"
                    + "     public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\"\n;"
                    + "     public static final String BUILT_BY_USER = \"" + xorEncode(request.getUserName()) + "\"\n;"
                    + "	private static String KEY = \"c2dmPref\";\n"
                    + "     private static String REGISTRATION_KEY = \"registrationKey\";"
                    + "     private Context context;\n\n";

            pushReceiverSourceCode += decodeFunction();

            boolean includePushContent = true;



            pushReceiverSourceCode += "     @Override\n"
                    + "     public void onReceive(Context context, Intent intent) {\n"
                    + "         this.context = context;\n"
                    + "         if (intent.getAction().equals(\"com.google.android.c2dm.intent.REGISTRATION\")) {\n"
                    + "             handleRegistration(context, intent);\n"
                    + "             return;\n"
                    + "         }\n"
                    + "         if (intent.getAction().equals(\"com.google.android.c2dm.intent.RECEIVE\")) {\n"
                    + "             handleMessage(context, intent);\n"
                    + "             return;\n"
                    + "         }\n"
                    + "         if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {\n"
                    + "             if(!com.codename1.impl.android.AndroidImplementation.hasAndroidMarket(context)) {\n"
                    + "                 PushNotificationService.startServiceIfRequired(" + request.getPackageName() + ".PushNotificationService.class, context);\n"
                    + "             }\n"
                    + "             return;\n"
                    + "         }\n"
                    + "     }\n\n"
                    + "     private void handleRegistration(Context context, Intent intent) {\n"
                    + "         final String registration = intent.getStringExtra(\"registration_id\");\n"
                    + "         System.out.println(\"Push handleRegistration() received: \" + registration);\n"
                    + "         " + request.getMainClass() + "Stub stub = " + request.getMainClass() + "Stub.getInstance();\n"
                    + "         if (intent.getStringExtra(\"error\") != null) {\n"
                    + "             final String error = intent.getStringExtra(\"error\");\n"
                    + "             System.out.println(\"Push handleRegistration() error: \" + error);\n"
                    + "             final " + request.getMainClass() + " main = stub.getAppInstance();\n"
                    + "             if(main instanceof PushCallback) {\n"
                    + "                 Display.getInstance().callSerially(new Runnable() {\n"
                    + "                     public void run() {\n"
                    + "                         ((PushCallback)main).pushRegistrationError(error, 0);\n"
                    + "                     }\n"
                    + "                 });\n"
                    + "             }\n"
                    + "         } else if (intent.getStringExtra(\"unregistered\") != null) {\n // do something??? \n"
                    + "             System.out.println(\"Push deregistered!\");\n"
                    + "         } else if (registration != null) {\n"
                    + "             System.out.println(\"Push handleRegistration() Sending registration to server!\");\n"
                    + "             Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();\n"
                    + "             editor.putString(REGISTRATION_KEY, registration);\n"
                    + "             Preferences.set(\"push_key\", registration);\n"
                    + "             editor.commit();\n"
                    + "             com.codename1.impl.android.AndroidImplementation.registerPushOnServer(registration, d(BUILT_BY_USER) + '/' + PACKAGE_NAME, (byte)1, \"\", \"" + request.getPackageName() + "\");\n"
                    + "             final " + request.getMainClass() + " main = stub.getAppInstance();\n"
                    + "             if(main instanceof PushCallback) {\n"
                    + "                 Display.getInstance().callSerially(new Runnable() {\n"
                    + "                     public void run() {\n"
                    + "                         ((PushCallback)main).registeredForPush(registration);\n"
                    + "                     }\n"
                    + "                 });\n"
                    + "             }\n"
                    + "         }\n"
                    + "     }\n\n"
                    + "     private android.graphics.Bitmap getBitmapfromUrl(String imageUrl) {\n" +
                    "        try {\n" +
                    "            java.net.URL url = new java.net.URL(imageUrl);\n" +
                    "            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();\n" +
                    "            connection.setDoInput(true);\n" +
                    "            connection.connect();\n" +
                    "            InputStream input = connection.getInputStream();\n" +
                    "            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);\n" +
                    "            return bitmap;\n" +
                    "\n" +
                    "        } catch (Exception e) {\n" +
                    "            // TODO Auto-generated catch block\n" +
                    "            e.printStackTrace();\n" +
                    "            return null;\n" +
                    "\n" +
                    "        }\n" +
                    "    }\n\n"
                    + "     private void handleMessage(final Context context, Intent intent) {\n"
                    + "         final String messageType = intent.getExtras().getString(C2DM_MESSAGE_TYPE_EXTRA);\n"
                    + "         final String message = intent.getExtras().getString(C2DM_MESSAGE_EXTRA);\n"
                    + "         final String image = intent.getExtras().getString(C2DM_MESSAGE_IMAGE);\n"
                    + "         final String category = intent.getExtras().getString(C2DM_MESSAGE_CATEGORY);\n"
                    + "         System.out.println(\"Push message received: \" + message);\n"
                    + "         System.out.println(\"Push type: \" + messageType);\n"
                    + "         System.out.println(\"Is running: \" + " + request.getMainClass() + "Stub.isRunning());\n"
                    + "         if(" + handlePushImmediatelyCheck +") {\n"
                    + "             " + request.getMainClass() + "Stub stub = " + request.getMainClass() + "Stub.getInstance();\n"
                    + "             final " + request.getMainClass() + " main = stub.getAppInstance();\n"
                    + "             if(main instanceof PushCallback) {\n"
                    + "                 Display.getInstance().setProperty(\"pushType\", messageType);\n";



            pushReceiverSourceCode +=
                    "                 Display.getInstance().callSerially(new Runnable() {\n"
                            + "                     public void run() {\n";

            if (includePushContent) {
                pushReceiverSourceCode +=
                        "                         com.codename1.impl.android.AndroidImplementation.initPushContent(message, image, messageType, category, context);\n";
            }

            pushReceiverSourceCode +=

                    "                         if(messageType != null && (Integer.parseInt(messageType) == 3 || Integer.parseInt(messageType) == 6) ) {\n"
                            + "                             String[] a = message.split(\";\");\n";


            pushReceiverSourceCode
                    += "                             ((PushCallback)main).push(a[0]);\n"
                    + "                             ((PushCallback)main).push(a[1]);\n"
                    + "                             return;\n"
                    + "                         } else if (\"101\".equals(messageType)) {\n" +
                    "                            ((PushCallback) main).push(message.substring(message.indexOf(\" \")+1));\n" +
                    "                            return;\n" +
                    "                        }\n";

            pushReceiverSourceCode +=
                    "                         ((PushCallback)main).push(message);\n"
                            + "                     }\n"
                            + "                 });\n"
                            + "             }\n"
                            + "         }"
                            + "         if (!"+stubIsRunningCheck+") {\n"
                            + "             ";

            pushReceiverSourceCode +=
                    "             com.codename1.impl.android.AndroidImplementation.appendNotification(messageType, message, image, category, context);\n";

            pushReceiverSourceCode +=
                    "             int badgeNumber = -1;\n" +
                            "             if (\"101\".equals(messageType)) {\n" +
                            "                 badgeNumber = Integer.parseInt(message.substring(0, message.indexOf(\" \")));\n" +
                            "\n" +
                            "             }"
                            + "if(messageType == null || messageType.length() == 0 || Integer.parseInt(messageType) < 2 || messageType.equals(\"3\") || messageType.equals(\"4\") || messageType.equals(\"5\") || messageType.equals(\"6\") || messageType.equals(\"101\")) {\n"
                            + "                 String actualMessage = message;\n"
                            + "             if (\"101\".equals(messageType)) {\n" +
                            "                     actualMessage = message.substring(message.indexOf(\" \")+1);\n" +
                            "                 }"
                            + "                 String title = \"" + request.getDisplayName() + "\";\n"
                            + "                 if(messageType != null && (Integer.parseInt(messageType) == 3 || Integer.parseInt(messageType) == 6)) {\n"
                            + "                     String[] a = message.split(\";\");\n"
                            + "                     actualMessage = a[0];\n"
                            + "                 }\n"
                            + "                if (messageType != null && Integer.parseInt(messageType) == 4) {\n"
                            + "                    String[] a = message.split(\";\");\n"
                            + "                    title = a[0];\n"
                            + "                    actualMessage = a[1];\n"
                            + "                }\n"
                            + "                 NotificationManager nm = (NotificationManager)context.getSystemService(Activity.NOTIFICATION_SERVICE);\n"
                            + "                 Intent newIntent = new Intent(context, " + request.getMainClass() + "Stub.class);\n"
                            + "                 PendingIntent contentIntent = PendingIntent.getActivity(context, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);\n"
                            + "                 Drawable myIcon = context.getResources().getDrawable(R.drawable.icon);\n"
                            + "                 Bitmap icon = ((BitmapDrawable) myIcon).getBitmap();\n"
                            + "                 int notifyID = 1;\n"
                            + "                 Builder mNotifyBuilder = new NotificationCompat.Builder(context)\n"
                            + "                         .setContentTitle(title)\n"
                            + "                         .setSmallIcon(R.drawable.ic_stat_notify)\n"
                            + "                         .setLargeIcon(icon)\n"
                            + "                         .setContentIntent(contentIntent)\n"
                            + "                         .setAutoCancel(true)\n"
                            + "                         .setWhen(System.currentTimeMillis())\n"
                            + "                         .setTicker(actualMessage);\n"
                            + vibrateCode
                            + "                 if (messageType == null || (Integer.parseInt(messageType) != 5 && Integer.parseInt(messageType) != 6)) {\n"
                            + "                     Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);\n"
                            + "                     mNotifyBuilder.setSound(alarmSound);\n"
                            + pushSound
                            + "                 }\n"
                            + "                 if(android.os.Build.VERSION.SDK_INT >= 21){\n" +
                            "                     mNotifyBuilder.setCategory(\"Notification\");\n" +
                            "                 }\n";
            if (buildToolsVersionInt >= 26 && Integer.parseInt(targetNumber) >= 26) {





                pushReceiverSourceCode += "                com.codename1.impl.android.AndroidImplementation.setNotificationChannel(nm, mNotifyBuilder, context);\n";

            }
            pushReceiverSourceCode +=
                    "                 String[] messages = com.codename1.impl.android.AndroidImplementation.getPendingPush(messageType, context);\n"
                            + "                 int numMessages = messages.length;\n"
                            + "                 if (numMessages == 1) {\n"
                            + "                     mNotifyBuilder.setContentText(messages[0]);\n"
                            + "                 } else {\n"
                            + "                         NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();\n"
                            + "                         for (int i = 0; i < messages.length; i++) {\n"
                            + "                             inboxStyle.addLine(messages[i]);\n"
                            + "                         }\n"
                            + "                         mNotifyBuilder.setStyle(inboxStyle);\n"
                            + "                 }\n"
                            + "     if(android.os.Build.VERSION.SDK_INT >= 22) {\n"
                            + "         if (badgeNumber >= 0) {\n"
                            + "             mNotifyBuilder.setNumber(badgeNumber);\n"
                            + "         } else {\n"
                            + "                 mNotifyBuilder.setNumber(numMessages);\n"
                            + "         }\n"
                            + "     }\n";



            pushReceiverSourceCode +=
                    "                 if (category != null && numMessages == 1) {\n" +
                            "                     try {\n" +
                            "                         AndroidImplementation.addActionsToNotification(null, category, mNotifyBuilder, newIntent, context);\n" +
                            "                     } catch (java.io.IOException ex) {\n" +
                            "                         ex.printStackTrace();\n" +
                            "                     }\n" +
                            "                 }"
                            + "                 if (image != null && numMessages == 1) {\n" +
                            "                     final Builder fNotifyBuilder = mNotifyBuilder;\n" +
                            "                     final int fNotifyID = notifyID;\n" +
                            "                     final NotificationManager fnm = nm;\n" +
                            "                     android.os.AsyncTask.execute(new Runnable() {\n" +
                            "                         public void run() {\n" +
                            "                             fNotifyBuilder.setStyle(new NotificationCompat.BigPictureStyle()\n" +
                            "                                     .bigPicture(getBitmapfromUrl(image)));/*Notification with Image*/\n" +
                            "                             fnm.notify(fNotifyID, fNotifyBuilder.build());\n" +
                            "                         }\n" +
                            "                     });\n" +
                            "\n" +
                            "               } else {\n" +
                            "                     nm.notify(notifyID, mNotifyBuilder.build());\n" +
                            "               }\n";

            pushReceiverSourceCode +=
                    "             }\n"
                            + "         }\n"
                            + "    }\n"
                            + "}\n";


            if (pushPermission) {
                try {
                    OutputStream pushSourceStream = new FileOutputStream(pushFileSourceFile);
                    pushSourceStream.write(pushReceiverSourceCode.getBytes());
                    pushSourceStream.close();

                    OutputStream pushServiceSourceStream = new FileOutputStream(pushServiceFileSourceFile);
                    pushServiceSourceStream.write(pushServiceSourceCode.getBytes());
                    pushServiceSourceStream.close();
                } catch (IOException ex) {
                    throw new BuildException("Failed to generate push file", ex);
                }
            }
        } else {
            InputStream is = null;
            OutputStream os = null;
            debug("Generating FirebaseMessagingService...");
            File fcmMessagingServiceFile = new File(androidImplDir, "CN1FirebaseMessagingService.java");

            try {
                String fireBaseMessagingServiceSourcePath = "CN1FirebaseMessagingService.javas";

                fireBaseMessagingServiceSourcePath = "CN1FirebaseMessagingService7.javas";

                is = getClass().getResourceAsStream(fireBaseMessagingServiceSourcePath);
                os = new FileOutputStream(fcmMessagingServiceFile);
                copy(is, os);
            } catch (IOException ex) {
                error("Failed to generate FirebaseMessagingService", ex);
                throw new BuildException("Failed to generate FirebaseMessagingService", ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable t){}
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (Throwable t){}
                }
            }
            try {
                replaceInFile(fcmMessagingServiceFile, "{{DISPLAY_NAME}}", request.getDisplayName());
                if (backgroundPushHandling) {
                    replaceInFile(fcmMessagingServiceFile, "allowBackgroundPush = false;", "allowBackgroundPush = true;");
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to update FCM messaging service with app details", ex);
            }
        }
        try {
            OutputStream stubSourceStream = new FileOutputStream(stubFileSourceFile);
            stubSourceStream.write(stubSourceCode.getBytes());
            stubSourceStream.close();
        } catch (IOException ex) {
            throw new BuildException("Failed to write stub source file", ex);
        }

        try {
            File projectPropertiesFile = new File(projectDir, "project.properties");
            Properties projectPropertiesObject = new Properties();
            if (projectPropertiesFile.exists()) {
                FileInputStream fi = new FileInputStream(projectPropertiesFile);
                projectPropertiesObject.load(fi);
                fi.close();
            }
            projectPropertiesObject.setProperty("proguard.config", "proguard.cfg");
            if (request.getArg("android.enableProguard", "true").equals("false")) {
                projectPropertiesObject.remove("proguard.config");
            }
            projectPropertiesObject.setProperty("dex.force.jumbo", "true");

            FileOutputStream projectPropertiesOutputStream = new FileOutputStream(projectPropertiesFile);
            projectPropertiesObject.store(projectPropertiesOutputStream, "Project properties for android build generated by Codename One");
            projectPropertiesOutputStream.close();
        } catch (IOException ex) {
            throw new BuildException("Failed to write project properties", ex);
        }

        String dontObfuscate = "";
        if (request.getArg("android.enableProguard", "true").equals("false")) {
            dontObfuscate = "-dontobfuscate\n";
        }

        String keepOverride = request.getArg("android.proguardKeepOverride", "Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod");


        // workaround broken optimizer in proguard
        String proguardConfigOverride = "-dontusemixedcaseclassnames\n"
                + "-dontskipnonpubliclibraryclasses\n"
                + "-dontpreverify\n"
                + "-verbose\n"
                + "-dontoptimize\n"
                + dontObfuscate
                + "\n"
                + "-dontwarn com.google.android.gms.**\n"
                + "-keep class com.codename1.impl.android.AndroidBrowserComponentCallback {\n"
                + "*;\n"
                + "}\n\n"
                + "-keep class com.codename1.impl.android.AndroidNativeUtil {\n"
                + "*;\n"
                + "}\n\n"
                + "-keepclassmembers class **.R$* {\n"
                + " public static <fields>;\n"
                + "}\n\n"
                + "-keep class **.R$*\n"
                + "-keep public class * extends android.app.Activity\n"
                + "-keep public class * extends android.app.Application\n"
                + "-keep public class * extends android.app.Service\n"
                + "-keep public class * extends android.content.BroadcastReceiver\n"
                + "-keep public class * extends android.content.ContentProvider\n"
                + "-keep public class * extends android.app.backup.BackupAgentHelper\n"
                + "-keep public class * extends android.preference.Preference\n"
                + "-keep public class com.android.vending.licensing.ILicensingService\n\n"
                + "-keep public class "+xclass("android.support.v4.app.RemoteInput")+" {*;}\n"
                + "-keep public class "+xclass("android.support.v4.app.RemoteInput")+"$Builder {*;}\n"
                + "-keep public class "+xclass("android.support.v4.app.NotificationCompat")+"$Builder {*;}\n"
                + "-keep public class "+xclass("android.support.v4.app.NotificationCompat")+"$Action {*;}\n"
                + "-keep public class "+xclass("android.support.v4.app.NotificationCompat")+"$Action$Builder {*;}\n"
                + "-keepclasseswithmembernames class * {\n"
                + "    native <methods>;\n"
                + "}\n\n"
                + "-keepclasseswithmembers class * {\n"
                + "    public <init>(android.content.Context, android.util.AttributeSet);\n"
                + "}\n\n"
                + "-keepclasseswithmembers class * {\n"
                + "    public <init>(android.content.Context, android.util.AttributeSet, int);\n"
                + "}\n\n"
                + "-keepclassmembers class * extends android.app.Activity {\n"
                + "    public void *(android.view.View);\n"
                + "}\n\n"
                + "-keepclassmembers enum * {\n"
                + "    public static **[] values();\n"
                + "    public static ** valueOf(java.lang.String);\n"
                + "}\n\n"
                + "-keep class * implements android.os.Parcelable {\n"
                + "    public static final android.os.Parcelable$Creator *;\n"
                + "}\n"
                + "-keep class com.apperhand.common.** {\n"
                + "*;\n"
                + "}\n\n"
                + "-keep class com.apperhand.device.android.EULAActivity$EulaJsInterface {\n"
                + "*;\n"
                + "}\n\n"
                + "-keepclassmembers public class "+xclass("android.support.v4.app.NotificationCompat")+"$Builder {\n"
                + "    public "+xclass("android.support.v4.app.NotificationCompat")+"$Builder setChannelId(java.lang.String);\n"
                + "}\n\n"
                + facebookProguard
                + " " + request.getArg("android.proguardKeep", "") + "\n"
                + googlePlayObfuscation
                + "-keep class com.google.mygson.**{\n"
                + "*;\n"
                + "}\n\n"
                + "-dontwarn android.support.**\n"
                + "-dontwarn androidx.**\n"
                + "-dontwarn com.google.ads.**\n"
                + "-keepattributes " + keepOverride;

        String gradleObfuscate = "";
        File proguardConfigOverrideFile = new File(projectDir, "proguard.cfg");
        proguardConfigOverrideFile.delete();
        if (request.getArg("android.enableProguard", "true").equals("true")) {
            try {
                createFile(proguardConfigOverrideFile, proguardConfigOverride.getBytes());
            } catch (IOException ex) {
                throw new BuildException("Failed to create proguard config file", ex);
            }

            gradleObfuscate = "            minifyEnabled true\n"
                    + (request.getArg("android.shrinkResources", "false").equals("true") ? "            shrinkResources true\n" : "")
                    + "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard.cfg'\n";
        }

        HashMap<String, String> env = new HashMap<String, String>();
        env.put("ANDROID_HOME", androidSDKDir.getAbsolutePath());
        env.put("SLAVE_AAPT_TIMEOUT", "20");
        request.putArgument("var.android.playServicesVersion", playServicesVersion);
        String additionalDependencies = request.getArg("gradleDependencies", "");
        if (facebookSupported) {
            String compile = "compile";
            if (useAndroidX) {
                compile = "implementation";
            }
            minSDK = maxInt("15", minSDK);
            String facebookSdkVersion = request.getArg("android.facebookSdkVersion", "4.39.0");;
            if(request.getArg("android.excludeBolts", "false").equals("true")) {
                additionalDependencies +=
                        " "+compile+" ('com.facebook.android:facebook-android-sdk:" +
                                facebookSdkVersion + "'){ exclude module: 'bolts-android' }\n";
            } else {
                additionalDependencies +=
                        " "+compile+" 'com.facebook.android:facebook-android-sdk:" +
                                facebookSdkVersion + "'\n";
            }
        }
        String compile = "compile";
        if (useAndroidX) {
            compile = "implementation";
        }
        if (legacyGplayServicesMode) {
            additionalDependencies += " "+compile+" 'com.google.android.gms:play-services:6.5.87'\n";
        } else {
            if(playServicesPlus){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-plus:"+getDefaultPlayServiceVersion("plus")+"'\n";
            }
            if(playServicesAuth){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-auth:"+getDefaultPlayServiceVersion("auth")+"'\n";
            }
            if(playServicesBase){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-base:"+getDefaultPlayServiceVersion("base")+"'\n";
            }
            if(playServicesIdentity){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-identity:"+getDefaultPlayServiceVersion("identity")+"'\n";
            }
            if(playServicesIndexing){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-appindexing:"+getDefaultPlayServiceVersion("appindexing")+"'\n";
            }
            if(playServicesInvite){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-appinvite:"+getDefaultPlayServiceVersion("appinvite")+"'\n";
            }
            if(playServicesAnalytics){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-analytics:"+getDefaultPlayServiceVersion("analytics")+"'\n";
            }
            if(playServicesCast){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-cast:"+getDefaultPlayServiceVersion("cast")+"'\n";
            }
            if(playServicesGcm){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-gcm:"+getDefaultPlayServiceVersion("gcm")+"'\n";
            }
            if(playServicesDrive){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-drive:"+getDefaultPlayServiceVersion("drive")+"'\n";
            }
            if(playServicesFit){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-fitness:"+getDefaultPlayServiceVersion("fit")+"'\n";
            }
            if(playServicesLocation){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-location:"+getDefaultPlayServiceVersion("location")+"'\n";
            }
            if(playServicesMaps){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-maps:"+getDefaultPlayServiceVersion("maps")+"'\n";
            }
            if(playServicesAds){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-ads:"+getDefaultPlayServiceVersion("ads")+"'\n";
            }
            if(playServicesVision){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-vision:"+getDefaultPlayServiceVersion("vision")+"'\n";
            }
            if(playServicesNearBy){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-nearby:"+getDefaultPlayServiceVersion("nearby")+"'\n";
            }
            if(playServicesSafetyPanorama){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-panaroma:"+getDefaultPlayServiceVersion("panorama")+"'\n";
            }
            if(playServicesGames){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-games:"+getDefaultPlayServiceVersion("games")+"'\n";
            }
            if(playServicesSafetyNet){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-safenet:"+getDefaultPlayServiceVersion("safenet")+"'\n";
            }
            if(playServicesWallet){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-wallet:"+getDefaultPlayServiceVersion("wallet")+"'\n";
            }
            if(playServicesWear){
                additionalDependencies += " "+compile+" 'com.google.android.gms:play-services-wearable:"+getDefaultPlayServiceVersion("wearable")+"'\n";
            }
        }

        if (purchasePermissions) {
            String billingClientVersion = request.getArg("android.billingclient.version", "4.0.0");
            additionalDependencies += " implementation 'com.android.billingclient:billing:"+billingClientVersion+"'\n";
        }

        String useLegacyApache = "";
        if (request.getArg("android.apacheLegacy", "false").equals("true")) {
            useLegacyApache = " useLibrary 'org.apache.http.legacy'\n";
        }

        String multidex = "";
        if (request.getArg("android.multidex", "true").equals("true")) {
            multidex = "        multiDexEnabled true\n";
            if (Integer.parseInt(minSDK) < 21) {
                if (useAndroidX) {
                    if (!additionalDependencies.contains("androidx.multidex:multidex") && !request.getArg("android.gradleDep", "").contains("androidx.multidex:multidex")) {
                        additionalDependencies += " implementation 'androidx.multidex:multidex:1.0.3'\n";
                    }
                } else {
                    if (!additionalDependencies.contains("com.android.support:multidex") && !request.getArg("android.gradleDep", "").contains("com.android.support:multidex")) {
                        additionalDependencies += " implementation 'com.android.support:multidex:1.0.3'\n";
                    }
                }

            }
        }

        String gradleDependency = "classpath 'com.android.tools.build:gradle:1.3.1'\n";
        if(gradleVersionInt < 3){
            gradleDependency = "classpath 'com.android.tools.build:gradle:2.1.2'\n";
        } else {
            if(gradleVersionInt < 6){
                if (useAndroidX) {
                    gradleDependency = "classpath 'com.android.tools.build:gradle:3.2.0'\n";
                } else {
                    gradleDependency = "classpath 'com.android.tools.build:gradle:3.0.1'\n";
                }
            }else {
                gradleDependency = "classpath 'com.android.tools.build:gradle:4.1.1'\n";
            }
        }
        gradleDependency += request.getArg("android.topDependency", "");

        String compileSdkVersion = "'android-21'";
        String quotedBuildToolsVersion = "'23.0.1'";

            compileSdkVersion = "'android-" + request.getArg("android.sdkVersion", "23") + "'";
            quotedBuildToolsVersion = "'" +buildToolsVersion + "'";




        String java8P2 = "";
        if(request.getArg("android.java8", "false").equals("true")) {
            java8P2 = "    compileOptions {\n" +
                    "        sourceCompatibility JavaVersion.VERSION_1_8\n" +
                    "        targetCompatibility JavaVersion.VERSION_1_8\n" +
                    "    }\n";
        }

        String mavenCentral = "";
        if(request.getArg("android.includeMavenCentral", "false").equals("true")) {
            mavenCentral = "    mavenCentral()\n";
        }

        String jcenter = "        jcenter()\n";

        String injectRepo = request.getArg("android.repositories", "");
        if(injectRepo.length() > 0) {
            String[] repos = injectRepo.split(";");
            injectRepo = "";
            for(String s : repos) {
                injectRepo += "    " + s + "\n";
            }
        }

        Properties gradlePropertiesObject = new Properties();
        File gradlePropertiesFile = new File(projectDir, "gradle.properties");
        if (gradlePropertiesFile.exists()) {
            try {
                FileInputStream fi = new FileInputStream(gradlePropertiesFile);
                gradlePropertiesObject.load(fi);
                fi.close();
            } catch (IOException ex) {
                throw new BuildException("Failed to load gradle properties from properties file "+gradlePropertiesFile, ex);
            }
        }

        String supportV4Default = "    compile 'com.android.support:support-v4:23.+'";

        compileSdkVersion = maxPlatformVersion;
        String supportLibVersion = maxPlatformVersion;
        if (buildToolsVersion.startsWith("28")) {
            compileSdkVersion = "28";
            supportLibVersion = "28";
        }
        if (buildToolsVersion.startsWith("29")) {
            compileSdkVersion = "29";
            supportLibVersion = "28";
        }
        if (buildToolsVersion.startsWith("30")) {
            compileSdkVersion = "30";
            supportLibVersion = "28";
        }
        if (buildToolsVersion.startsWith("31")) {
            compileSdkVersion = "31";
            supportLibVersion = "28";
        }
        if (buildToolsVersion.startsWith("32")) {
            compileSdkVersion = "32";
            supportLibVersion = "28";
        }
        if (buildToolsVersion.startsWith("33")) {
            compileSdkVersion = "33";
            supportLibVersion = "28";
        }
        jcenter =
                "      google()\n" +
                        "     jcenter()\n" +
                        "     mavenLocal()\n" +
                        "      mavenCentral()\n";

        injectRepo += "      google()\n" +
                "     mavenLocal()\n" +
                "      mavenCentral()\n";
        if(!androidAppBundle && gradleVersionInt < 6 && buildToolsVersionInt < 30){
            gradlePropertiesObject.put("android.enableAapt2", "false");
        }
        if (!useAndroidX) {
            supportV4Default = "    compile 'com.android.support:support-v4:"+supportLibVersion+".+'\n     implementation 'com.android.support:appcompat-v7:"+supportLibVersion+".+'\n";
        } else {
            supportV4Default = "    implementation 'androidx.legacy:legacy-support-v4:1.0.0'\n     implementation 'androidx.appcompat:appcompat:" + request.getArg("androidx.appcompat.version", "1.0.0")+"'\n";
        }

        String gradleProps = "apply plugin: 'com.android.application'\n"
                + request.getArg("android.gradlePlugin", "")
                + "\n"
                + "buildscript {\n"
                + "    repositories {\n"
                + jcenter
                + injectRepo
                + "    }\n"
                + "    dependencies {\n"
                + gradleDependency
                + "    }\n"
                + "}\n"
                + "\n"
                + "android {\n"
                + request.getArg("android.gradle.androidx", "") + "\n"
                + "    compileSdkVersion " + compileSdkVersion + "\n"
                // For maven builder explicitly specifying buildtools version caused some problems
                // leave it out and just let Android studio choose the version installed.
                //+ "    buildToolsVersion " + quotedBuildToolsVersion + "\n"
                + useLegacyApache
                + "\n"
                + "    dexOptions {\n"
                + "        preDexLibraries = false\n"
                + "        incremental false\n"
                + "        jumboMode = true\n"
                + "        javaMaxHeapSize \"3g\"\n"
                + "    }\n"
                + "    defaultConfig {\n"
                + "        applicationId \"" + request.getPackageName() + "\"\n"
                + "        minSdkVersion " + minSDK + "\n"
                + "        targetSdkVersion " + targetNumber + "\n"
                + "        versionCode " + intVersion + "\n"
                + "        versionName \"" + version + "\"\n"
                + multidex
                + request.getArg("android.xgradle_default_config", "")
                + "    }\n"
                + java8P2
                + "    sourceSets {\n"
                + "        main {\n"
                + "            aidl.srcDirs = ['src/main/java']\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    lintOptions {\n"
                + "        lintOptions {\n"
                + "        checkReleaseBuilds false\n"
                + "        abortOnError false\n"
                + "        }\n"
                + "    }\n"
                + "    signingConfigs {\n"
                + "        release {\n"
                + "            storeFile file(\"keyStore\")\n"
                + "            storePassword \"" + escape(request.getCertificatePassword(), "$\"") + "\"\n"
                + "            keyAlias \"" + escape(request.getKeystoreAlias(), "$\"") + "\"\n"
                + "            keyPassword \"" + escape(request.getCertificatePassword(), "$\"") + "\"\n"
                + "        }\n"
                + "    }\n"
                + "    buildTypes {\n"
                + "        release {\n"
                + gradleObfuscate
                + "            signingConfig signingConfigs.release\n"
                + "        }\n"
                + ((request.getCertificate() != null) ? (
                "        debug {\n"
                        + "            signingConfig signingConfigs.release\n"
                        + "        }\n"):"")
                + "    }\n"
                + "}\n"
                + "\n"
                + "repositories {\n"
                + "    google()\n"
                + "    jcenter()\n"
                + injectRepo
                + "    flatDir{\n"
                + "              dirs 'libs'\n"
                + "       }\n"
                + mavenCentral
                + "}\n"
                + "\n"
                + "dependencies {\n"
                + "    "+compile+" fileTree(dir: 'libs', include: ['*.jar'])\n"
                + request.getArg("android.supportv4Dep",supportV4Default) + "\n"
                + addNewlineIfMissing(additionalDependencies)
                + addNewlineIfMissing(request.getArg("android.gradleDep", ""))
                + addNewlineIfMissing(aarDependencies)
                + "}\n"
                + request.getArg("android.xgradle", "");

        debug("Gradle File start\n-------\n");
        debug(gradleProps);
        debug("-------\nGradle File end \n");
        File gradleFile = new File(projectDir, "build.gradle");

        try {
            OutputStream gradleStream = new FileOutputStream(gradleFile);
            gradleStream.write(gradleProps.getBytes());
            gradleStream.close();
        } catch (IOException ex) {
            throw new BuildException("Failed to write gradle properties to "+gradleFile, ex);
        }

        File settingsGradle = new File(studioProjectDir, "settings.gradle");
        try {
            replaceInFile(settingsGradle, "My Application2", request.getDisplayName());
        } catch (Exception ex) {
            throw new BuildException("Failed to update settingsGradle with display name", ex);
        }

        gradlePropertiesObject.setProperty("org.gradle.daemon", "true");
        if(request.getArg("android.forceJava8Builder", "false").equals("true")) {
            gradlePropertiesObject.setProperty("org.gradle.java.home", System.getProperty("java.home"));
        }
        gradlePropertiesObject.setProperty("org.gradle.jvmargs", "-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8");
        if (useAndroidX) {
            gradlePropertiesObject.setProperty("android.useAndroidX", "true");
            gradlePropertiesObject.setProperty("android.enableJetifier", "true");
        }
        try {
            FileOutputStream antPropertiesOutputStream = new FileOutputStream(gradlePropertiesFile);
            gradlePropertiesObject.store(antPropertiesOutputStream, "Gradle properties for android build generated by Codename One");
            antPropertiesOutputStream.close();
        } catch (IOException ex) {
            throw new BuildException("Failed to output gradle properties file "+gradlePropertiesFile);
        }


        try {
            migrateSourcesToAndroidX(projectDir);
        } catch (Exception ex) {
            throw new BuildException("Failed to migrate sources to AndroidX", ex);
        }

        if (request.getCertificate() != null) {
            try {
                createFile(new File(projectDir, "keyStore"), request.getCertificate());
            } catch (IOException ex) {
                throw new BuildException("Failed to create keyStore file", ex);
            }
        }
        return true;
    }

    static String xmlize(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        int charCount = s.length();
        for (int iter = 0; iter < charCount; iter++) {
            char c = s.charAt(iter);
            if (c > 127) {
                // we need to localize the string...
                StringBuilder b = new StringBuilder();
                for (int counter = 0; counter < charCount; counter++) {
                    c = s.charAt(counter);
                    if (c > 127) {
                        b.append("&#x");
                        b.append(Integer.toHexString(c));
                        b.append(";");
                    } else {
                        b.append(c);
                    }
                }
                return b.toString();
            }
        }
        return s;
    }


    @Override
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(" + methodCallString + ")";
    }

    @Override
    protected String convertPeerComponentToNative(String param) {
        return "(android.view.View)" + param + ".getNativePeer()";
    }

    private String createOnDestroyCode(BuildRequest request) {
        String retVal = "";
        if (integrateMoPub) {
            retVal += "moPubView.destroy();\n";
        }
        if(playServicesLocation){
            retVal += "    com.codename1.impl.android.AndroidNativeUtil.removeLifecycleListener(com.codename1.location.AndroidLocationPlayServiceManager.getInstance());\n";
        }
        if(shouldIncludeGoogleImpl){

                retVal += "    com.codename1.impl.android.AndroidNativeUtil.removeLifecycleListener((com.codename1.impl.android.LifecycleListener) com.codename1.social.GoogleConnect.getInstance());\n";

        }
        return retVal;
    }

    private String createPostInitCode(BuildRequest request) {
        String retVal = "";
        return retVal;
    }

    private String createOnCreateCode(BuildRequest request) {
        String retVal = "";

        if (request.getArg("android.includeGPlayServices", "true").equals("true") || playServicesLocation) {
            retVal += "Display.getInstance().setProperty(\"IncludeGPlayServices\", \"true\");\n";
        }
        if(playServicesLocation){
            retVal += "com.codename1.impl.android.AndroidNativeUtil.addLifecycleListener(com.codename1.location.AndroidLocationPlayServiceManager.getInstance());\n";
        }
        if(shouldIncludeGoogleImpl){

                retVal += "com.codename1.social.GoogleImpl.init();\n";
                retVal += "com.codename1.impl.android.AndroidNativeUtil.addLifecycleListener((com.codename1.impl.android.LifecycleListener) com.codename1.social.GoogleConnect.getInstance());\n";

        }

        if (request.getArg("android.web_loading_hidden", "false").equalsIgnoreCase("true")) {
            retVal += "Display.getInstance().setProperty(\"WebLoadingHidden\", \"true\");\n";
        }

        if (request.getArg("android.statusbar_hidden", "false").equalsIgnoreCase("true")) {
            retVal += "Display.getInstance().setProperty(\"StatusbarHidden\", \"true\");\n";
        }

        if (request.getArg("KeepScreenOn", "false").equalsIgnoreCase("true")) {
            retVal += "Display.getInstance().setProperty(\"KeepScreenOn\", \"true\");\n";
        }

        if (request.getArg("android.disableScreenshots", "false").equalsIgnoreCase("true")) {
            retVal += "Display.getInstance().setProperty(\"DisableScreenshots\", \"true\");\n";
        }


        return retVal;
    }

    @Override
    protected String createStartInvocation(BuildRequest request, String mainObject) {
        String retVal = super.createStartInvocation(request, mainObject);
        if (integrateMoPub) {
            retVal += "moPubView = (MoPubView) findViewById(R.id.adview);\n"
                    + "moPubView.setAdUnitId(\"" + request.getArg("android.mopubId", null) + "\");\n"
                    + "moPubView.loadAd();\n";
        }
        return retVal;
    }

    public void extract(InputStream source, File dir, String sdkPath) throws IOException {
        try {
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(source);
            ZipEntry entry;
            boolean addedSDKDir = false;
            while ((entry = zis.getNextEntry()) != null) {
                debug("Extracting: " + entry);
                if (entry.isDirectory()) {
                    File d = new File(dir, entry.getName());
                    d.mkdirs();
                    if (!addedSDKDir && sdkPath != null) {
                        if (is_windows) {
                            sdkPath = sdkPath.replace("" + File.separatorChar, "\\\\");
                        }
                        String sdkPathProperties = "sdk.dir=" + sdkPath;
                        // write the files to the disk
                        File destFile;
                        destFile = new File(d, "local.properties");
                        destFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(destFile);
                        fos.write(sdkPathProperties.getBytes());
                        fos.close();
                        addedSDKDir = true;
                    }
                    continue;
                }
                if (entry.getName().contains("local.properties") && sdkPath != null) {
                    if (is_windows) {
                        sdkPath = sdkPath.replace("" + File.separatorChar, "\\\\");
                    }
                    String sdkPathProperties = "sdk.dir=" + sdkPath + "\n";
                    // write the files to the disk
                    File destFile;
                    destFile = new File(dir, entry.getName());
                    destFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(destFile);
                    fos.write(sdkPathProperties.getBytes());
                    fos.close();
                    continue;
                }

                int count;
                byte[] data = new byte[8192];
                // write the files to the disk
                File destFile;
                destFile = new File(dir, entry.getName());
                destFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(destFile);
                dest = new BufferedOutputStream(fos, data.length);
                while ((count = zis.read(data, 0, data.length)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
            source.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void extractAAR(InputStream source, File dir, String sdkPath) throws IOException {
        File libs = new File(dir, "libs");
        libs.mkdirs();
        File srcLib = new File(dir, "src");
        srcLib.mkdirs();
        extract(source, dir, null);
        File classes1 = null;
        FileChannel src = null;
        FileChannel dest = null;
        try {
            classes1 = new File(dir, "classes.jar");
            File classes2 = new File(libs, "classes.jar");
            classes2.createNewFile();
            copyFile(classes1, classes2);

            //if a jni folder exosts in the aar copy it's content to the libs folder
            File jni = new File(dir, "jni");
            if (jni.exists()) {
                copyDirectory(jni, libs);
                delete(jni);
            }

            FileOutputStream projectProps = new FileOutputStream(new File(dir, "project.properties"));
            String props = "android.library=true\n"
                    + "target=android-14";
            projectProps.write(props.getBytes());
            projectProps.close();

        } catch (Exception e) {

        } finally {
            if (src != null) {
                src.close();
            }
            if (dest != null) {
                dest.close();
            }
            if (classes1 != null) {
                classes1.delete();
            }
        }

    }

    protected File placeXMLFile(ZipEntry entry, File xmlDir, File resDir) {
        String name = entry.getName();
        if (name.endsWith("colors.xml")) {
            File parent = resDir.getParentFile();
            resDir = new File(parent, "res");
            File valsDir = new File(resDir, "values");
            return new File(valsDir, entry.getName());
        } else if (name.endsWith("layout.xml")) {
            File parent = resDir.getParentFile();
            resDir = new File(parent, "res");
            File layDir = new File(resDir, "layout");
            return new File(layDir, entry.getName());
        } else {
            return super.placeXMLFile(entry, xmlDir, resDir);
        }
    }

    public Image makeColorTransparent(final BufferedImage im, final Color color) {

        final ImageFilter filter = new RGBImageFilter() {
            // the color we are looking for (white)... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() & 0xFFFFFF;

            public final int filterRGB(final int x, final int y, final int rgb) {
                int tmp = rgb & 0xFFFFFF;
                if (tmp == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    protected boolean useXMLDir() {
        return true;
    }

    public static final void copy(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source, destination);
        }
    }

    public static final void copyDirectory(File source, File destination) throws IOException {

        destination.mkdirs();
        File[] files = source.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                copyDirectory(file, new File(destination, file.getName()));
            } else {
                copyFile(file, new File(destination, file.getName()));
            }
        }
    }

    public static final void copyFile(File source, File destination) throws IOException {
        FileChannel sourceChannel = new FileInputStream(source).getChannel();
        FileChannel targetChannel = new FileOutputStream(destination).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        sourceChannel.close();
        targetChannel.close();
    }

    void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        f.delete();
    }

    public void unzip(InputStream source, File classesDir, File resDir, File sourceDir, File libsDir, File xmlDir) throws IOException {

        try {
            File appDir = /*buildToolsVersionInt >= 27*/false ?
                    new File(sourceDir.getParentFile(), "app") :
                    new File(libsDir.getParentFile(), "app");
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(source);
            ZipEntry entry;
            TarOutputStream tos = null;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.startsWith("html") || entryName.startsWith("/html")) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    if (tos == null) {
                        tos = new TarOutputStream(new FileOutputStream(new File(resDir, "html.tar")));
                    }
                    entryName = entryName.substring(5);
                    TarEntry tEntry = new TarEntry(new File(entryName), entryName);
                    tEntry.setSize(entry.getSize());
                    debug("Packaging entry " + entryName + " size: " + entry.getSize());
                    tos.putNextEntry(tEntry);
                    int count;
                    byte[] data = new byte[8192];
                    while ((count = zis.read(data, 0, data.length)) != -1) {
                        tos.write(data, 0, count);
                    }
                    continue;
                }

                if (entry.isDirectory()) {
                    if (!entryName.startsWith("raw")) {
                        File dir = new File(classesDir, entryName);
                        dir.mkdirs();
                        dir = new File(resDir, entryName);
                        dir.mkdirs();
                        dir = new File(sourceDir, entryName);
                        dir.mkdirs();
                    }
                    continue;
                }

                int count;
                byte[] data = new byte[8192];

                // write the files to the disk
                File destFile;
                if (entryName.endsWith(".class")) {
                    destFile = new File(classesDir, entryName);
                } else {
                    if (entryName.endsWith(".java") || entryName.endsWith(".m") || entryName.endsWith(".h")) {
                        destFile = new File(sourceDir, entryName);
                    } else {
                        if (entryName.endsWith(".jar") || entryName.endsWith(".a") || entryName.endsWith(".dylib") || entryName.endsWith(".andlib") || entryName.endsWith(".aar")) {
                            destFile = new File(libsDir, entryName);
                        } else {
                            if (useXMLDir() && entryName.endsWith(".xml")) {
                                destFile = placeXMLFile(entry, xmlDir, resDir);
                            } else if (entryName.startsWith("raw")) {
                                destFile = new File(xmlDir.getParentFile(), entryName.toLowerCase());
                            } else if (entryName.contains("notification_sound")) {
                                destFile = new File(xmlDir.getParentFile(), "raw/" + entryName.toLowerCase());
                            } else if ("google-services.json".equals(entryName)) {
                                destFile = new File(libsDir.getParentFile(), entryName);
                            } else {
                                destFile = new File(resDir, entryName);
                            }
                        }
                    }
                }
                destFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(destFile);
                dest = new BufferedOutputStream(fos, data.length);
                while ((count = zis.read(data, 0, data.length)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            if (tos != null) {
                tos.close();
            }
            zis.close();
            source.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String permissionAdd(BuildRequest request, String permission, String text) {
        if(xPermissions.contains(permission)) {
            return "";
        }
        return text;
    }


    private static String maxInt(String a, String b) {
        return String.valueOf(Math.max(Integer.parseInt(a), Integer.parseInt(b)));
    }

    private static int compareVersions(String v1, String v2) {
        String v1p1 = v1.indexOf(".") == -1 ? v1 : v1.substring(0, v1.indexOf("."));
        String v2p1 = v2.indexOf(".") == -1 ? v2 : v2.substring(0, v2.indexOf("."));
        int i1 = Integer.parseInt(v1p1);
        int i2 = Integer.parseInt(v2p1);
        if (i1 < i2) return -1;
        if (i1 > i2) return 1;

        v1 = v1.indexOf(".") == -1 ? "" : v1.substring(v1.indexOf(".")+1);
        v2 = v2.indexOf(".") == -1 ? "" : v2.substring(v2.indexOf(".")+1);
        if (v2.length() == 0 && v1.length() == 0) return 0;
        if (v2.length() == 0) return 1;
        if (v1.length() == 0) return -1;
        return compareVersions(v1, v2);

    }

    protected String getDebugCertificateFile() {
        return "android.ks";
    }

    protected String getReleaseCertificateFile() {
        return "android.ks";
    }

    private String addNewlineIfMissing(String s) {
        if(s != null && s.length() > 0 && !s.endsWith("\n")) {
            return s + "\n";
        }
        return s;
    }

    /**
     * Finds the PlayServices_X_X_X.java file that will be used for the PlayServices
     * class given the desired target playServicesVersion.  This will pick the newest
     * version up to the target playServicesVersion.
     *
     * The PlayServices class is used to factor out version sensitive play services
     * code (e.g. location code that requires classes that aren't found in earlier
     * versions of play services.  We still want to be able to build for older versions
     * of play services, so we delete all PlayServices_X_X_X classes that we aren't using
     * at build time.  We use the highest version available.
     * @param srcDir The src dir
     * @param playServicesVersion The target play services version .  E.g. 12.0.1
     * @return The Source file for the class with maximum version less than or equal to playServicesVersion.  Never null
     */
    private File getPlayServicesJavaSourceFile(File srcDir, String playServicesVersion) {
        File androidImpl = new File(srcDir, "com/codename1/impl/android");
        String currentMaxVersion = "8.3.0";
        File currentMaxVersionFile = new File(androidImpl, "PlayServices.java");
        for (File f : androidImpl.listFiles()) {
            if (f.getName().startsWith("PlayServices_") && f.getName().endsWith(".java")) {
                String versionStr = f.getName().substring("PlayServices_".length()).replace(".java", "").replace('_', '.');
                if (compareVersions(playServicesVersion, versionStr) < 0) {
                    //  This class uses a newer version of PlayServices than we are building for
                    // so we can't select it
                    continue;

                }
                if (compareVersions(currentMaxVersion, versionStr) < 0) {
                    currentMaxVersion = versionStr;
                    currentMaxVersionFile = f;
                }

            }
        }
        return currentMaxVersionFile;
    }

    /**
     * Extracts the version for a PlayServices class source file.  There may be one or more classes 
     * com/codename1/impl/android/PlayServices_X_X_X.java which correspond to a play services
     * version that we are building for.  For such a .java file, this will return "X.X.X", a version 
     * string.
     * @param playServicesSourceFile
     * @return The version string for the play services file.   
     */
    private String getPlayServicesVersion(File playServicesSourceFile) {
        String fname = playServicesSourceFile.getName();
        if (!fname.startsWith("PlayServices")) {
            throw new IllegalArgumentException("Only PlayServices class files can be checked for play services versions.");
        }
        if (!fname.startsWith("PlayServices_")) {
            // The minimum default.
            return "8.3.0";
        }
        return fname.substring("PlayServices_".length()).replace(".java", "").replace('_', '.');
    }

    // AndroidX stuff

    private String xartifact(String artifact) {
        if (!useAndroidX) return artifact;
        try {
            Map<String,String> map = loadAndroidXArtifactMapping();
            if (map.containsKey(artifact)) {
                return map.get(artifact);
            } else {
                return artifact;
            }
        } catch (IOException ex) {
            return artifact;
        }
    }

    private String xclass(String cls) {
        if (!useAndroidX) {
            return cls;
        }
        try {
            Map<String,String> map = loadAndroidXClassMapping();
            if (map.containsKey(cls)) {
                return map.get(cls);
            } else {
                return cls;
            }
        } catch (IOException ex) {
            return cls;
        }
    }

    private void migrateSourcesToAndroidX(File root) throws IOException {
        if (!migrateToAndroidX) {
            return;
        }

        replaceAndroidXArtifactsInTree(root);
        replaceAndroidXClassesInTree(root);
    }

    private void replaceAndroidXArtifactsInTree(File root) throws IOException {
        replaceInTree(root, loadAndroidXArtifactMapping(), new FilenameFilter() {
            @Override
            public boolean accept(File parent, String dir) {
                return dir.endsWith(".gradle");
            }
        });
    }

    private void replaceAndroidXClassesInTree(File root) throws IOException {
        debug("Replacing Android Support classes with AndroidX classes in "+root);
        replaceInTree(root, loadAndroidXClassMapping(), new FilenameFilter() {
            @Override
            public boolean accept(File parent, String dir) {
                return dir.endsWith(".xml") || dir.endsWith(".kt") || dir.endsWith(".java");
            }
        });
    }

    private static String replace(String content, Map<String,String> replacements) {
        for (Map.Entry<String,String> e : replacements.entrySet()) {
            content = content.replace(e.getKey(), e.getValue());
        }
        return content;
    }

    private void replaceInTree(File root, Map<String,String> replacements, FilenameFilter filter) throws IOException {
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                replaceInTree(child, replacements, filter);
            }
        } else {
            if (filter.accept(root.getParentFile(), root.getName())) {
                replaceInFile(root, replacements);
            }
        }
    }

    private void replaceInFile(File file, Map<String,String> replacements) throws IOException {
        String contents = readFileToString(file);
        contents = replace(contents, replacements);
        FileWriter fios = new FileWriter(file);
        fios.write(contents);
        fios.close();

    }
    private  Map<String,String> androidXArtifactMapping;
    private  Map<String,String> loadAndroidXArtifactMapping() throws IOException {
        if (androidXArtifactMapping == null) {
            androidXArtifactMapping = loadCSVMapping(androidXArtifactMapping,"androidx-artifact-mapping.csv");
        }
        return androidXArtifactMapping;
    }
    private Map<String,String> androidXClassMapping;
    private Map<String,String> loadAndroidXClassMapping() throws IOException {
        if (androidXClassMapping == null) {
            androidXClassMapping = loadCSVMapping(androidXClassMapping,"androidx-class-mapping.csv");
            Map<String,String> packages = new LinkedHashMap<String,String>();
            for (String supportClass : androidXClassMapping.keySet()) {
                String xClass = androidXClassMapping.get(supportClass);
                supportClass = supportClass.substring(0, supportClass.lastIndexOf(".")+1);
                xClass = xClass.substring(0, xClass.lastIndexOf(".")+1);
                packages.put(supportClass, xClass);
            }
            //androidXClassMapping.putAll(packages);
        }
        return androidXClassMapping;
    }

    private Map<String,String> loadCSVMapping(Map<String,String> out, String csvResourcePath) throws IOException {
        if (out == null) {
            out = new LinkedHashMap<String,String>();
        }
        debug("Loading CSV mapping for android X from "+csvResourcePath);
        InputStream csvMappingStream = AndroidGradleBuilder.class.getResourceAsStream(csvResourcePath);
        if (csvMappingStream == null) {
            throw new IOException("Cannot find android X CSV mapping at "+csvResourcePath);
        }
        Scanner scanner = new Scanner(csvMappingStream, "UTF-8");
        boolean firstLine = true;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (firstLine) {
                firstLine = false;
                continue;
            }
            int pos = line.indexOf(",");
            if (pos == -1) {
                continue;
            }

            out.put(line.substring(0, pos).trim(), line.substring(pos+1).trim());
        }
        return out;

    }

    private void createAndroidStudioProject(File dest) throws IOException {
        if (dest.exists()) {
            throw new IOException("Cannot create AndroidStudio project at "+dest+" because it already exists");
        }
        File destZip = new File(dest.getAbsolutePath()+".zip");
        if (destZip.exists()) {
            throw new IOException("Cannot extract AndroidStudioTemplate at "+destZip+" because it already exists");
        }
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("AndroidStudioProjectTemplate.zip"), destZip);
        ZipFile zipFile = new ZipFile(destZip);
        File destZipExtracted = new File(destZip.getAbsolutePath()+"-extracted");
        destZipExtracted.mkdir();
        zipFile.extractAll(destZipExtracted.getAbsolutePath());

        for (File child : destZipExtracted.listFiles()) {
            if (child.isDirectory() && "AndroidStudioProjectTemplate".equals(child.getName())) {
                child.renameTo(dest);
                break;
            }
        }
        delTree(destZipExtracted);
        delete(destZip);
        if (!dest.exists()) {
            throw new IOException("Failed to create Android Studio Project at "+dest+".  Not sure what went wrong.  Did all the steps, but just wasn't there when we were done");
        }

    }


    private String getDefaultPlayServiceVersion(String playService) {
        if (playServiceVersions.containsKey(playService)) {
            return playServiceVersions.get(playService);
        }
        if (decouplePlayServiceVersions) {
            if (defaultPlayServiceVersions.containsKey(playService)) {
                return defaultPlayServiceVersions.get(playService);
            }
        }

        return playServicesVersion;
    }

    private void initPlayServiceVersions(BuildRequest request) {
        for (String arg : request.getArgs()) {
            if (arg.startsWith("android.playService.")) {
                String playServiceKey = arg.substring("android.playService.".length());
                if (playServiceKey.equals("appInvite")) {
                    playServiceKey = "app-invite";
                } else if (playServiceKey.equals("firebaseCore")) {
                    playServiceKey = "firebase-core";
                } else if (playServiceKey.equals("firebaseMessaging")) {
                    playServiceKey = "firebase-messaging";
                }
                String playServiceValue = request.getArg(arg, null);
                if (playServiceValue == null || "true".equals(playServiceValue) || "false".equals(playServiceValue)) {
                    continue;
                }
                playServiceVersions.put(playServiceKey, playServiceValue);
            }
        }
    }
}
