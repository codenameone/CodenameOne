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
package android.content;

/// Identifies a component (here, an activity class) of the application.
public final class ComponentName implements Comparable<ComponentName> {

    private final String pkg;
    private final String cls;

    public ComponentName(String pkg, String cls) {
        this.pkg = pkg;
        this.cls = cls;
    }

    public ComponentName(Context pkg, String cls) {
        this(pkg.getPackageName(), cls);
    }

    public ComponentName(Context pkg, Class<?> cls) {
        this(pkg.getPackageName(), cls.getName());
    }

    public String getPackageName() {
        return pkg;
    }

    public String getClassName() {
        return cls;
    }

    public String getShortClassName() {
        if (cls.startsWith(pkg + ".")) {
            return cls.substring(pkg.length());
        }
        return cls;
    }

    public String flattenToString() {
        return pkg + "/" + cls;
    }

    public String flattenToShortString() {
        return pkg + "/" + getShortClassName();
    }

    public static ComponentName unflattenFromString(String str) {
        int sep = str.indexOf('/');
        if (sep < 0 || sep + 1 >= str.length()) {
            return null;
        }
        String pkg = str.substring(0, sep);
        String cls = str.substring(sep + 1);
        if (cls.startsWith(".")) {
            cls = pkg + cls;
        }
        return new ComponentName(pkg, cls);
    }

    @Override
    public int compareTo(ComponentName o) {
        int c = pkg.compareTo(o.pkg);
        return c != 0 ? c : cls.compareTo(o.cls);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ComponentName && ((ComponentName) o).pkg.equals(pkg) && ((ComponentName) o).cls.equals(cls);
    }

    @Override
    public int hashCode() {
        return pkg.hashCode() + cls.hashCode();
    }

    @Override
    public String toString() {
        return "ComponentInfo{" + flattenToString() + "}";
    }
}
