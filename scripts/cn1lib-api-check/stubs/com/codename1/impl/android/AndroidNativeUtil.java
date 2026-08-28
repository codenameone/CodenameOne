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

import android.app.Activity;

/**
 * Compile-only stand-in for the Android port's class of the same name, shared by
 * every cn1lib API check: cn1-admob's Maven-driven one and
 * scripts/check-cn1lib-android-api.py, which covers all of them. Never packaged.
 *
 * <p>The checks exist to catch a cn1lib drifting off the SDK it pins, and they
 * must run everywhere -- including a fresh release checkout, where
 * {@code codenameone-android} is built from an empty source directory because
 * the {@code compile-android} profile activates on a {@code cn1.binaries}
 * directory that the {@code download} profile only creates in {@code initialize},
 * after Maven has already evaluated the model. Depending on the real port would
 * make this check fail the release build for a reason that has nothing to do
 * with the library under test.
 *
 * <p>The signature below is checked against
 * {@code Ports/Android/src/com/codename1/impl/android/AndroidNativeUtil.java}
 * by the build, so it cannot silently drift.
 */
public class AndroidNativeUtil {
    private AndroidNativeUtil() {
    }

    public static Activity getActivity() {
        throw new UnsupportedOperationException("compile-only stub");
    }
}
