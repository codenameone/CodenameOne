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
package android.os;

/// Device and release information.
public class Build {

    public static final String MANUFACTURER = "Codename One";
    public static final String BRAND = "codenameone";
    public static final String MODEL = com.codename1.ui.Display.getInstance().getPlatformName();
    public static final String DEVICE = MODEL;
    public static final String PRODUCT = MODEL;
    public static final String HARDWARE = MODEL;
    public static final String BOARD = "unknown";
    public static final String FINGERPRINT = "codenameone/" + MODEL;
    public static final String ID = "CN1";
    public static final String TYPE = "user";
    public static final String TAGS = "release-keys";
    public static final String HOST = "localhost";
    public static final String USER = "codenameone";
    public static final String SERIAL = "unknown";
    public static final String UNKNOWN = "unknown";
    public static final String[] SUPPORTED_ABIS = {"arm64-v8a"};
    public static final long TIME = 0;

    public static class VERSION {
        /// The API level applications are told they run on. Resources and
        /// code that branch on SDK_INT take the modern path.
        public static final int SDK_INT = 34;
        public static final String RELEASE = "14";
        public static final String CODENAME = "REL";
        public static final String INCREMENTAL = "cn1";
        public static final int PREVIEW_SDK_INT = 0;
        public static final String SDK = "34";
    }

    public static class VERSION_CODES {
        public static final int CUR_DEVELOPMENT = 10000;
        public static final int BASE = 1;
        public static final int DONUT = 4;
        public static final int ECLAIR = 5;
        public static final int FROYO = 8;
        public static final int GINGERBREAD = 9;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
        public static final int JELLY_BEAN_MR2 = 18;
        public static final int KITKAT = 19;
        public static final int KITKAT_WATCH = 20;
        public static final int LOLLIPOP = 21;
        public static final int LOLLIPOP_MR1 = 22;
        public static final int M = 23;
        public static final int N = 24;
        public static final int N_MR1 = 25;
        public static final int O = 26;
        public static final int O_MR1 = 27;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31;
        public static final int S_V2 = 32;
        public static final int TIRAMISU = 33;
        public static final int UPSIDE_DOWN_CAKE = 34;
        public static final int VANILLA_ICE_CREAM = 35;
    }

    public static String getRadioVersion() {
        return null;
    }
}
