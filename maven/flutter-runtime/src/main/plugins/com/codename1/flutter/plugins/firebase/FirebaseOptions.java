/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.firebase;

/** firebase_core's {@code FirebaseOptions}, as flutterfire's firebase_options.dart builds it. */
public class FirebaseOptions {
    private String apiKey;
    private String appId;
    private String messagingSenderId;
    private String projectId;
    private String authDomain;
    private String databaseURL;
    private String storageBucket;
    private String measurementId;
    private String trackingId;
    private String deepLinkURLScheme;
    private String androidClientId;
    private String iosClientId;
    private String iosBundleId;
    private String appGroupId;

    public FirebaseOptions() {
    }

    public void apiKey(String v) {
        this.apiKey = v;
    }

    public String apiKey() {
        return apiKey;
    }

    public void appId(String v) {
        this.appId = v;
    }

    public String appId() {
        return appId;
    }

    public void messagingSenderId(String v) {
        this.messagingSenderId = v;
    }

    public String messagingSenderId() {
        return messagingSenderId;
    }

    public void projectId(String v) {
        this.projectId = v;
    }

    public String projectId() {
        return projectId;
    }

    public void authDomain(String v) {
        this.authDomain = v;
    }

    public String authDomain() {
        return authDomain;
    }

    public void databaseURL(String v) {
        this.databaseURL = v;
    }

    public String databaseURL() {
        return databaseURL;
    }

    public void storageBucket(String v) {
        this.storageBucket = v;
    }

    public String storageBucket() {
        return storageBucket;
    }

    public void measurementId(String v) {
        this.measurementId = v;
    }

    public String measurementId() {
        return measurementId;
    }

    public void trackingId(String v) {
        this.trackingId = v;
    }

    public String trackingId() {
        return trackingId;
    }

    public void deepLinkURLScheme(String v) {
        this.deepLinkURLScheme = v;
    }

    public String deepLinkURLScheme() {
        return deepLinkURLScheme;
    }

    public void androidClientId(String v) {
        this.androidClientId = v;
    }

    public String androidClientId() {
        return androidClientId;
    }

    public void iosClientId(String v) {
        this.iosClientId = v;
    }

    public String iosClientId() {
        return iosClientId;
    }

    public void iosBundleId(String v) {
        this.iosBundleId = v;
    }

    public String iosBundleId() {
        return iosBundleId;
    }

    public void appGroupId(String v) {
        this.appGroupId = v;
    }

    public String appGroupId() {
        return appGroupId;
    }
}
