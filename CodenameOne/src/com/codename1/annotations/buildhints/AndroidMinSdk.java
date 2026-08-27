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
package com.codename1.annotations.buildhints;

/// The Android API level an app runs on at the lowest.
///
/// A closed set rather than an `int`, because this is not an arbitrary number:
/// it is the platform floor the build supports, the builder parses it and
/// compares it against real thresholds -- multidex below 21, Android Auto below
/// 23 -- and a level outside this range is a typo rather than a choice.
///
/// The build server raises it on its own where a feature demands more, so
/// picking a low level here is a floor and not a promise: adding Android Auto to
/// a project pinned at [#API_19] builds at 23.
///
/// A level not listed is not a dead end. The properties file takes
/// `codename1.arg.android.min_sdk_version` unchanged, which is the escape hatch
/// for a platform newer than this framework build knows about.
public enum AndroidMinSdk {
    /// Say nothing, and let the build server apply its own default.
    @HintUnset
    DEFAULT,

    /// Android API level 19.
    @HintValue("19")
    API_19,

    /// Android API level 20.
    @HintValue("20")
    API_20,

    /// Android API level 21.
    @HintValue("21")
    API_21,

    /// Android API level 22.
    @HintValue("22")
    API_22,

    /// Android API level 23.
    @HintValue("23")
    API_23,

    /// Android API level 24.
    @HintValue("24")
    API_24,

    /// Android API level 25.
    @HintValue("25")
    API_25,

    /// Android API level 26.
    @HintValue("26")
    API_26,

    /// Android API level 27.
    @HintValue("27")
    API_27,

    /// Android API level 28.
    @HintValue("28")
    API_28,

    /// Android API level 29.
    @HintValue("29")
    API_29,

    /// Android API level 30.
    @HintValue("30")
    API_30,

    /// Android API level 31.
    @HintValue("31")
    API_31,

    /// Android API level 32.
    @HintValue("32")
    API_32,

    /// Android API level 33.
    @HintValue("33")
    API_33,

    /// Android API level 34.
    @HintValue("34")
    API_34,

    /// Android API level 35.
    @HintValue("35")
    API_35,

    /// Android API level 36.
    @HintValue("36")
    API_36;
}
