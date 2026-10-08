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
package android.content.pm;

/// Information about the application.
public class ApplicationInfo {
    public static final int FLAG_DEBUGGABLE = 1 << 1;
    public static final int FLAG_SUPPORTS_RTL = 1 << 22;

    public String packageName;
    public int flags;
    public int targetSdkVersion = android.os.Build.VERSION.SDK_INT;
    public int minSdkVersion = 21;
    public String name;
    public int labelRes;
    public int icon;
    public int theme;
    public String dataDir = "";

    public ApplicationInfo() {
    }

    public ApplicationInfo(String packageName) {
        this.packageName = packageName;
    }

    public CharSequence loadLabel(PackageManager pm) {
        return pm.getApplicationLabel(this);
    }
}
