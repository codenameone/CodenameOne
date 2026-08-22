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

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_annotations_buildhints {
    private GeneratedAccess_com_codename1_annotations_buildhints() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("Android".equals(simpleName)) {
            return com.codename1.annotations.buildhints.Android.class;
        }
        if ("AndroidThemeMode".equals(simpleName)) {
            return com.codename1.annotations.buildhints.AndroidThemeMode.class;
        }
        if ("Build".equals(simpleName)) {
            return com.codename1.annotations.buildhints.Build.class;
        }
        if ("Desktop".equals(simpleName)) {
            return com.codename1.annotations.buildhints.Desktop.class;
        }
        if ("DesktopTitleBar".equals(simpleName)) {
            return com.codename1.annotations.buildhints.DesktopTitleBar.class;
        }
        if ("HardenControlFlow".equals(simpleName)) {
            return com.codename1.annotations.buildhints.HardenControlFlow.class;
        }
        if ("HardenLevel".equals(simpleName)) {
            return com.codename1.annotations.buildhints.HardenLevel.class;
        }
        if ("HardenStrings".equals(simpleName)) {
            return com.codename1.annotations.buildhints.HardenStrings.class;
        }
        if ("Hardening".equals(simpleName)) {
            return com.codename1.annotations.buildhints.Hardening.class;
        }
        if ("InstallLocation".equals(simpleName)) {
            return com.codename1.annotations.buildhints.InstallLocation.class;
        }
        if ("Ios".equals(simpleName)) {
            return com.codename1.annotations.buildhints.Ios.class;
        }
        if ("IosDependencyManager".equals(simpleName)) {
            return com.codename1.annotations.buildhints.IosDependencyManager.class;
        }
        if ("IosPrivacy".equals(simpleName)) {
            return com.codename1.annotations.buildhints.IosPrivacy.class;
        }
        if ("IosProjectType".equals(simpleName)) {
            return com.codename1.annotations.buildhints.IosProjectType.class;
        }
        if ("IosThemeMode".equals(simpleName)) {
            return com.codename1.annotations.buildhints.IosThemeMode.class;
        }
        if ("NativeThemeMode".equals(simpleName)) {
            return com.codename1.annotations.buildhints.NativeThemeMode.class;
        }
        if ("OnDeviceDebug".equals(simpleName)) {
            return com.codename1.annotations.buildhints.OnDeviceDebug.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedStatic(type, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.annotations.buildhints.AndroidThemeMode) {
            try {
                return invoke0((com.codename1.annotations.buildhints.AndroidThemeMode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.DesktopTitleBar) {
            try {
                return invoke1((com.codename1.annotations.buildhints.DesktopTitleBar) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.HardenControlFlow) {
            try {
                return invoke2((com.codename1.annotations.buildhints.HardenControlFlow) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.HardenLevel) {
            try {
                return invoke3((com.codename1.annotations.buildhints.HardenLevel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.HardenStrings) {
            try {
                return invoke4((com.codename1.annotations.buildhints.HardenStrings) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.InstallLocation) {
            try {
                return invoke5((com.codename1.annotations.buildhints.InstallLocation) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.IosDependencyManager) {
            try {
                return invoke6((com.codename1.annotations.buildhints.IosDependencyManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.IosProjectType) {
            try {
                return invoke7((com.codename1.annotations.buildhints.IosProjectType) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.IosThemeMode) {
            try {
                return invoke8((com.codename1.annotations.buildhints.IosThemeMode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.NativeThemeMode) {
            try {
                return invoke9((com.codename1.annotations.buildhints.NativeThemeMode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.Android) {
            try {
                return invoke10((com.codename1.annotations.buildhints.Android) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.Build) {
            try {
                return invoke11((com.codename1.annotations.buildhints.Build) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.Desktop) {
            try {
                return invoke12((com.codename1.annotations.buildhints.Desktop) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.Hardening) {
            try {
                return invoke13((com.codename1.annotations.buildhints.Hardening) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.Ios) {
            try {
                return invoke14((com.codename1.annotations.buildhints.Ios) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.IosPrivacy) {
            try {
                return invoke15((com.codename1.annotations.buildhints.IosPrivacy) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.buildhints.OnDeviceDebug) {
            try {
                return invoke16((com.codename1.annotations.buildhints.OnDeviceDebug) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.annotations.buildhints.AndroidThemeMode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.annotations.buildhints.DesktopTitleBar typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.annotations.buildhints.HardenControlFlow typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.annotations.buildhints.HardenLevel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.annotations.buildhints.HardenStrings typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.annotations.buildhints.InstallLocation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.annotations.buildhints.IosDependencyManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.annotations.buildhints.IosProjectType typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.annotations.buildhints.IosThemeMode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.annotations.buildhints.NativeThemeMode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.annotations.buildhints.Android typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("activityLaunchMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.activityLaunchMode();
            }
        }
        if ("appBundle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.appBundle();
            }
        }
        if ("buildToolsVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.buildToolsVersion();
            }
        }
        if ("captureRecord".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.captureRecord();
            }
        }
        if ("debug".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.debug();
            }
        }
        if ("disableR8".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.disableR8();
            }
        }
        if ("enableProguard".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.enableProguard();
            }
        }
        if ("gradleDep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.gradleDep();
            }
        }
        if ("hideStatusBar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hideStatusBar();
            }
        }
        if ("installLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.installLocation();
            }
        }
        if ("licenseKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.licenseKey();
            }
        }
        if ("minSdkVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minSdkVersion();
            }
        }
        if ("multidex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.multidex();
            }
        }
        if ("newFirebaseMessaging".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.newFirebaseMessaging();
            }
        }
        if ("proguardKeep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.proguardKeep();
            }
        }
        if ("release".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.release();
            }
        }
        if ("repositories".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.repositories();
            }
        }
        if ("targetSDKVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.targetSDKVersion();
            }
        }
        if ("themeMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.themeMode();
            }
        }
        if ("topDependency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.topDependency();
            }
        }
        if ("useAndroidX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.useAndroidX();
            }
        }
        if ("xapplication".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.xapplication();
            }
        }
        if ("xgradle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.xgradle();
            }
        }
        if ("xpermissions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.xpermissions();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.annotations.buildhints.Build typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("facebookAppId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.facebookAppId();
            }
        }
        if ("gcmSenderId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.gcmSenderId();
            }
        }
        if ("nativeTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nativeTheme();
            }
        }
        if ("noExtraResources".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.noExtraResources();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.annotations.buildhints.Desktop typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("adaptToRetina".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.adaptToRetina();
            }
        }
        if ("fullscreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.fullscreen();
            }
        }
        if ("height".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.height();
            }
        }
        if ("interactiveScrollbars".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.interactiveScrollbars();
            }
        }
        if ("resizable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resizable();
            }
        }
        if ("titleBar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.titleBar();
            }
        }
        if ("width".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.width();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.annotations.buildhints.Hardening typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("allowUnhardenedLocalBuild".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.allowUnhardenedLocalBuild();
            }
        }
        if ("controlFlow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.controlFlow();
            }
        }
        if ("keep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keep();
            }
        }
        if ("level".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.level();
            }
        }
        if ("rename".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.rename();
            }
        }
        if ("strings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.strings();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.annotations.buildhints.Ios typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLibs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.addLibs();
            }
        }
        if ("applicationQueriesSchemes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.applicationQueriesSchemes();
            }
        }
        if ("beforeFinishLaunching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.beforeFinishLaunching();
            }
        }
        if ("bundleVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.bundleVersion();
            }
        }
        if ("dependencyManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dependencyManager();
            }
        }
        if ("deploymentTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.deploymentTarget();
            }
        }
        if ("glAppDelegateHeader".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.glAppDelegateHeader();
            }
        }
        if ("includePush".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.includePush();
            }
        }
        if ("interfaceOrientation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.interfaceOrientation();
            }
        }
        if ("minDeploymentTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minDeploymentTarget();
            }
        }
        if ("newStorageLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.newStorageLocation();
            }
        }
        if ("objC".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.objC();
            }
        }
        if ("plistInject".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.plistInject();
            }
        }
        if ("pods".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pods();
            }
        }
        if ("podsPlatform".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.podsPlatform();
            }
        }
        if ("podsSources".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.podsSources();
            }
        }
        if ("prerenderedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.prerenderedIcon();
            }
        }
        if ("projectType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.projectType();
            }
        }
        if ("spmPackages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.spmPackages();
            }
        }
        if ("teamId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.teamId();
            }
        }
        if ("themeMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.themeMode();
            }
        }
        if ("uiscene".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.uiscene();
            }
        }
        if ("urlScheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.urlScheme();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.annotations.buildhints.IosPrivacy typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("calendarsFullAccessUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.calendarsFullAccessUsageDescription();
            }
        }
        if ("calendarsUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.calendarsUsageDescription();
            }
        }
        if ("calendarsWriteOnlyAccessUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.calendarsWriteOnlyAccessUsageDescription();
            }
        }
        if ("cameraUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.cameraUsageDescription();
            }
        }
        if ("healthShareUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.healthShareUsageDescription();
            }
        }
        if ("healthUpdateUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.healthUpdateUsageDescription();
            }
        }
        if ("localNetworkUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.localNetworkUsageDescription();
            }
        }
        if ("locationAlwaysAndWhenInUseUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.locationAlwaysAndWhenInUseUsageDescription();
            }
        }
        if ("locationAlwaysUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.locationAlwaysUsageDescription();
            }
        }
        if ("locationWhenInUseUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.locationWhenInUseUsageDescription();
            }
        }
        if ("microphoneUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.microphoneUsageDescription();
            }
        }
        if ("remindersFullAccessUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remindersFullAccessUsageDescription();
            }
        }
        if ("remindersUsageDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remindersUsageDescription();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.annotations.buildhints.OnDeviceDebug typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("android".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.android();
            }
        }
        if ("ios".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.ios();
            }
        }
        if ("iosProxyHost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iosProxyHost();
            }
        }
        if ("iosProxyPort".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iosProxyPort();
            }
        }
        if ("iosWaitForAttach".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iosWaitForAttach();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.annotations.buildhints.AndroidThemeMode.class) return getStaticField0(name);
        if (type == com.codename1.annotations.buildhints.DesktopTitleBar.class) return getStaticField1(name);
        if (type == com.codename1.annotations.buildhints.HardenControlFlow.class) return getStaticField2(name);
        if (type == com.codename1.annotations.buildhints.HardenLevel.class) return getStaticField3(name);
        if (type == com.codename1.annotations.buildhints.HardenStrings.class) return getStaticField4(name);
        if (type == com.codename1.annotations.buildhints.InstallLocation.class) return getStaticField5(name);
        if (type == com.codename1.annotations.buildhints.IosDependencyManager.class) return getStaticField6(name);
        if (type == com.codename1.annotations.buildhints.IosProjectType.class) return getStaticField7(name);
        if (type == com.codename1.annotations.buildhints.IosThemeMode.class) return getStaticField8(name);
        if (type == com.codename1.annotations.buildhints.NativeThemeMode.class) return getStaticField9(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.annotations.buildhints.AndroidThemeMode.AUTO;
        if ("HOLOLIGHT".equals(name)) return com.codename1.annotations.buildhints.AndroidThemeMode.HOLOLIGHT;
        if ("LEGACY".equals(name)) return com.codename1.annotations.buildhints.AndroidThemeMode.LEGACY;
        if ("MODERN".equals(name)) return com.codename1.annotations.buildhints.AndroidThemeMode.MODERN;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.AndroidThemeMode.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CUSTOM".equals(name)) return com.codename1.annotations.buildhints.DesktopTitleBar.CUSTOM;
        if ("NATIVE".equals(name)) return com.codename1.annotations.buildhints.DesktopTitleBar.NATIVE;
        if ("TOOLBAR".equals(name)) return com.codename1.annotations.buildhints.DesktopTitleBar.TOOLBAR;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.DesktopTitleBar.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("OFF".equals(name)) return com.codename1.annotations.buildhints.HardenControlFlow.OFF;
        if ("ON".equals(name)) return com.codename1.annotations.buildhints.HardenControlFlow.ON;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.HardenControlFlow.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("AGGRESSIVE".equals(name)) return com.codename1.annotations.buildhints.HardenLevel.AGGRESSIVE;
        if ("OFF".equals(name)) return com.codename1.annotations.buildhints.HardenLevel.OFF;
        if ("PARANOID".equals(name)) return com.codename1.annotations.buildhints.HardenLevel.PARANOID;
        if ("STANDARD".equals(name)) return com.codename1.annotations.buildhints.HardenLevel.STANDARD;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.HardenLevel.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("ALL".equals(name)) return com.codename1.annotations.buildhints.HardenStrings.ALL;
        if ("CONSTANTS".equals(name)) return com.codename1.annotations.buildhints.HardenStrings.CONSTANTS;
        if ("OFF".equals(name)) return com.codename1.annotations.buildhints.HardenStrings.OFF;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.HardenStrings.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.annotations.buildhints.InstallLocation.AUTO;
        if ("INTERNAL_ONLY".equals(name)) return com.codename1.annotations.buildhints.InstallLocation.INTERNAL_ONLY;
        if ("PREFER_EXTERNAL".equals(name)) return com.codename1.annotations.buildhints.InstallLocation.PREFER_EXTERNAL;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.InstallLocation.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.annotations.buildhints.IosDependencyManager.AUTO;
        if ("BOTH".equals(name)) return com.codename1.annotations.buildhints.IosDependencyManager.BOTH;
        if ("COCOAPODS".equals(name)) return com.codename1.annotations.buildhints.IosDependencyManager.COCOAPODS;
        if ("NONE".equals(name)) return com.codename1.annotations.buildhints.IosDependencyManager.NONE;
        if ("SPM".equals(name)) return com.codename1.annotations.buildhints.IosDependencyManager.SPM;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.IosDependencyManager.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("IOS".equals(name)) return com.codename1.annotations.buildhints.IosProjectType.IOS;
        if ("IPAD".equals(name)) return com.codename1.annotations.buildhints.IosProjectType.IPAD;
        if ("IPHONE".equals(name)) return com.codename1.annotations.buildhints.IosProjectType.IPHONE;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.IosProjectType.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.annotations.buildhints.IosThemeMode.AUTO;
        if ("IOS7".equals(name)) return com.codename1.annotations.buildhints.IosThemeMode.IOS7;
        if ("LEGACY".equals(name)) return com.codename1.annotations.buildhints.IosThemeMode.LEGACY;
        if ("MODERN".equals(name)) return com.codename1.annotations.buildhints.IosThemeMode.MODERN;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.IosThemeMode.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("CUSTOM".equals(name)) return com.codename1.annotations.buildhints.NativeThemeMode.CUSTOM;
        if ("LEGACY".equals(name)) return com.codename1.annotations.buildhints.NativeThemeMode.LEGACY;
        if ("MODERN".equals(name)) return com.codename1.annotations.buildhints.NativeThemeMode.MODERN;
        throw unsupportedStaticField(com.codename1.annotations.buildhints.NativeThemeMode.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
