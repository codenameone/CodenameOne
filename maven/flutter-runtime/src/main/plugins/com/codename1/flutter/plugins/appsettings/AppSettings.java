/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.appsettings;

import com.codename1.io.Log;
import com.codename1.ui.Display;

import dart.async.Future;

/**
 * app_settings' {@code AppSettings}, over {@code Display.execute}. On iOS every type opens
 * the application's own page ({@code app-settings:}), as the plugin does there. On Android
 * a type with a system settings action opens it through an {@code intent:} URL, and every
 * other type opens the application's details page. {@code asAnotherTask} is accepted and
 * not applied: the port starts the activity itself. Elsewhere, and with no display, there
 * is no settings page and the call does nothing.
 */
public final class AppSettings {

    private AppSettings() {
    }

    public static Future<Object> openAppSettings(AppSettingsType type, Boolean asAnotherTask) {
        if (!Display.isInitialized()) {
            return Future.value(null);
        }
        Display d = Display.getInstance();
        String platform = d.getPlatformName();
        if ("ios".equals(platform)) {
            d.execute("app-settings:");
        } else if ("and".equals(platform)) {
            String action = androidAction(type == null ? AppSettingsType.settings : type);
            if (action == null) {
                String pkg = d.getProperty("package_name", null);
                if (pkg == null) {
                    action = "android.settings.SETTINGS";
                } else {
                    d.execute("intent:" + pkg
                            + "#Intent;scheme=package;action=android.settings.APPLICATION_DETAILS_SETTINGS;end");
                    return Future.value(null);
                }
            }
            d.execute("intent:#Intent;action=" + action + ";end");
        } else {
            Log.p("app_settings: no settings page on " + platform);
        }
        return Future.value(null);
    }

    public static Future<Object> openAppSettingsPanel(AppSettingsPanelType type) {
        if (!Display.isInitialized()) {
            return Future.value(null);
        }
        if (!"and".equals(Display.getInstance().getPlatformName())) {
            return openAppSettings(AppSettingsType.settings, Boolean.FALSE);
        }
        String action;
        if (type == AppSettingsPanelType.nfc) {
            action = "android.settings.panel.action.NFC";
        } else if (type == AppSettingsPanelType.volume) {
            action = "android.settings.panel.action.VOLUME";
        } else if (type == AppSettingsPanelType.wifi) {
            action = "android.settings.panel.action.WIFI";
        } else {
            action = "android.settings.panel.action.INTERNET_CONNECTIVITY";
        }
        Display.getInstance().execute("intent:#Intent;action=" + action + ";end");
        return Future.value(null);
    }

    /** The system action for a type, or null for the application's details page. */
    static String androidAction(AppSettingsType type) {
        switch (type) {
            case accessibility: return "android.settings.ACCESSIBILITY_SETTINGS";
            case alarm: return "android.settings.REQUEST_SCHEDULE_EXACT_ALARM";
            case apn: return "android.settings.APN_SETTINGS";
            case appLocale: return "android.settings.LOCALE_SETTINGS";
            case batteryOptimization: return "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS";
            case bluetooth: return "android.settings.BLUETOOTH_SETTINGS";
            case cellular: return "android.settings.DATA_ROAMING_SETTINGS";
            case dataRoaming: return "android.settings.DATA_ROAMING_SETTINGS";
            case date: return "android.settings.DATE_SETTINGS";
            case developer: return "android.settings.APPLICATION_DEVELOPMENT_SETTINGS";
            case device: return "android.settings.DEVICE_INFO_SETTINGS";
            case display: return "android.settings.DISPLAY_SETTINGS";
            case generalSettings: return "android.settings.SETTINGS";
            case hotspot: return "android.settings.TETHER_SETTINGS";
            case internalStorage: return "android.settings.INTERNAL_STORAGE_SETTINGS";
            case location: return "android.settings.LOCATION_SOURCE_SETTINGS";
            case lockAndPassword: return "android.app.action.SET_NEW_PASSWORD";
            case manageUnknownApps: return "android.settings.MANAGE_UNKNOWN_APP_SOURCES";
            case nfc: return "android.settings.NFC_SETTINGS";
            case notification: return "android.settings.APP_NOTIFICATION_SETTINGS";
            case security: return "android.settings.SECURITY_SETTINGS";
            case sound: return "android.settings.SOUND_SETTINGS";
            case subscriptions: return "android.settings.SYNC_SETTINGS";
            case vpn: return "android.settings.VPN_SETTINGS";
            case wifi: return "android.settings.WIFI_SETTINGS";
            case wireless: return "android.settings.WIRELESS_SETTINGS";
            default: return null;
        }
    }
}
