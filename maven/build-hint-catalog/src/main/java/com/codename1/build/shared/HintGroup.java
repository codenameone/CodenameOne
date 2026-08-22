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
package com.codename1.build.shared;

/**
 * Which annotation type a hint is exposed through, and which section of the
 * documentation and the Settings UI it belongs to.
 *
 * <p>Assignment is by name prefix, except that two feature groups deliberately
 * claim a subtree across platforms: {@link #ON_DEVICE_DEBUG} takes the
 * {@code ios.onDeviceDebug*} and {@code android.onDeviceDebug} hints out of
 * their platform groups, and {@link #IOS_PRIVACY} takes the literal
 * {@code ios.NS*UsageDescription} keys out of {@link #IOS}.</p>
 *
 * <p>{@link #NONE} means catalogued but not annotated &mdash; the hint is still
 * described here for the documentation, the Settings tool and the drift gate,
 * but it is set through {@code codenameone_settings.properties}. Every dynamic
 * hint family is NONE, because a Java annotation cannot express a map.</p>
 */
public enum HintGroup {
    IOS("Ios", "ios."),
    ANDROID("Android", "android."),
    DESKTOP("Desktop", "desktop."),
    MAC_NATIVE("MacNative", "macNative."),
    WINDOWS("Windows", "windows."),
    LINUX("Linux", "linux."),
    JAVASCRIPT("JavaScript", "javascript."),
    TV_NATIVE("TvNative", "tvNative."),
    WATCH_NATIVE("WatchNative", "watchNative."),
    HARDENING("Hardening", "harden."),
    ON_DEVICE_DEBUG("OnDeviceDebug", null),
    IOS_PRIVACY("IosPrivacy", null),
    /** Unprefixed and one-off-prefix hints, exposed through {@code @Build}. */
    GENERAL("Build", null),
    /** Catalogued but not annotated. */
    NONE(null, null);

    private final String annotationSimpleName;
    private final String keyPrefix;

    HintGroup(String annotationSimpleName, String keyPrefix) {
        this.annotationSimpleName = annotationSimpleName;
        this.keyPrefix = keyPrefix;
    }

    /** Simple name of the generated annotation type, or null for {@link #NONE}. */
    public String annotationSimpleName() {
        return annotationSimpleName;
    }

    /**
     * The hint-name prefix this group owns, or null when membership is not
     * decided by prefix.
     */
    public String keyPrefix() {
        return keyPrefix;
    }

    /** Whether hints in this group are exposed as annotation attributes. */
    public boolean isAnnotated() {
        return annotationSimpleName != null;
    }
}
