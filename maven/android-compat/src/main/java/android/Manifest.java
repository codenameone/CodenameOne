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
package android;

/// The framework's permission and permission group names.
public final class Manifest {

    private Manifest() {
    }

    /// Permission names: `android.permission.` followed by the field's name.
    public static final class permission {
        private permission() {
        }

        public static final String ACCEPT_HANDOVER = "android.permission.ACCEPT_HANDOVER";
        public static final String ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION";
        public static final String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
        public static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
        public static final String ACCESS_MEDIA_LOCATION = "android.permission.ACCESS_MEDIA_LOCATION";
        public static final String ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
        public static final String ACCESS_NOTIFICATION_POLICY = "android.permission.ACCESS_NOTIFICATION_POLICY";
        public static final String ACCESS_WIFI_STATE = "android.permission.ACCESS_WIFI_STATE";
        public static final String ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION";
        public static final String ANSWER_PHONE_CALLS = "android.permission.ANSWER_PHONE_CALLS";
        public static final String BLUETOOTH = "android.permission.BLUETOOTH";
        public static final String BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN";
        public static final String BLUETOOTH_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE";
        public static final String BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";
        public static final String BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN";
        public static final String BODY_SENSORS = "android.permission.BODY_SENSORS";
        public static final String CALL_PHONE = "android.permission.CALL_PHONE";
        public static final String CAMERA = "android.permission.CAMERA";
        public static final String CHANGE_NETWORK_STATE = "android.permission.CHANGE_NETWORK_STATE";
        public static final String CHANGE_WIFI_STATE = "android.permission.CHANGE_WIFI_STATE";
        public static final String FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE";
        public static final String GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
        public static final String INTERNET = "android.permission.INTERNET";
        public static final String MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE";
        public static final String NFC = "android.permission.NFC";
        public static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
        public static final String READ_CALENDAR = "android.permission.READ_CALENDAR";
        public static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";
        public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
        public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
        public static final String READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO";
        public static final String READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES";
        public static final String READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO";
        public static final String READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS";
        public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
        public static final String READ_SMS = "android.permission.READ_SMS";
        public static final String RECEIVE_BOOT_COMPLETED = "android.permission.RECEIVE_BOOT_COMPLETED";
        public static final String RECEIVE_MMS = "android.permission.RECEIVE_MMS";
        public static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
        public static final String RECORD_AUDIO = "android.permission.RECORD_AUDIO";
        public static final String REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES";
        public static final String SCHEDULE_EXACT_ALARM = "android.permission.SCHEDULE_EXACT_ALARM";
        public static final String SEND_SMS = "android.permission.SEND_SMS";
        public static final String SET_ALARM = "android.permission.SET_ALARM";
        public static final String SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
        public static final String USE_BIOMETRIC = "android.permission.USE_BIOMETRIC";
        public static final String USE_EXACT_ALARM = "android.permission.USE_EXACT_ALARM";
        public static final String USE_FINGERPRINT = "android.permission.USE_FINGERPRINT";
        public static final String VIBRATE = "android.permission.VIBRATE";
        public static final String WAKE_LOCK = "android.permission.WAKE_LOCK";
        public static final String WRITE_CALENDAR = "android.permission.WRITE_CALENDAR";
        public static final String WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG";
        public static final String WRITE_CONTACTS = "android.permission.WRITE_CONTACTS";
        public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
        public static final String WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
    }

    /// Permission group names.
    public static final class permission_group {
        private permission_group() {
        }

        public static final String CALENDAR = "android.permission-group.CALENDAR";
        public static final String CALL_LOG = "android.permission-group.CALL_LOG";
        public static final String CAMERA = "android.permission-group.CAMERA";
        public static final String CONTACTS = "android.permission-group.CONTACTS";
        public static final String LOCATION = "android.permission-group.LOCATION";
        public static final String MICROPHONE = "android.permission-group.MICROPHONE";
        public static final String NEARBY_DEVICES = "android.permission-group.NEARBY_DEVICES";
        public static final String NOTIFICATIONS = "android.permission-group.NOTIFICATIONS";
        public static final String PHONE = "android.permission-group.PHONE";
        public static final String READ_MEDIA_AURAL = "android.permission-group.READ_MEDIA_AURAL";
        public static final String READ_MEDIA_VISUAL = "android.permission-group.READ_MEDIA_VISUAL";
        public static final String SENSORS = "android.permission-group.SENSORS";
        public static final String SMS = "android.permission-group.SMS";
        public static final String STORAGE = "android.permission-group.STORAGE";
    }
}
