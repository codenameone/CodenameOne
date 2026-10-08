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
package com.codename1.desktopcompat.java.beans;

import java.util.HashMap;
import java.util.Map;

/// Answers the [BeanInfo] of a class: one object per class for as long as the
/// application runs, so a value a library attaches to a class's
/// [BeanDescriptor] is there the next time it asks.
///
/// Nothing is discovered about the class. The JDK finds properties, events
/// and methods by reflection; here the info holds the class-level descriptor
/// and nothing else ([BeanInfo]).
public class Introspector {

    public static final int USE_ALL_BEANINFO = 1;
    public static final int IGNORE_IMMEDIATE_BEANINFO = 2;
    public static final int IGNORE_ALL_BEANINFO = 3;

    private static final Map<Class<?>, BeanInfo> INFOS = new HashMap<Class<?>, BeanInfo>();

    private Introspector() {
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("null bean class");
        }
        BeanInfo info = INFOS.get(beanClass);
        if (info == null) {
            info = new ClassInfo(new BeanDescriptor(beanClass));
            INFOS.put(beanClass, info);
        }
        return info;
    }

    /// The flags choose which explicit `BeanInfo` classes the JDK consults;
    /// there are none to consult here, so they change nothing.
    public static BeanInfo getBeanInfo(Class<?> beanClass, int flags) throws IntrospectionException {
        if (flags < USE_ALL_BEANINFO || flags > IGNORE_ALL_BEANINFO) {
            throw new IllegalArgumentException("flags must be USE_ALL_BEANINFO, IGNORE_IMMEDIATE_BEANINFO or "
                    + "IGNORE_ALL_BEANINFO");
        }
        return getBeanInfo(beanClass);
    }

    /// `FooBah` becomes `fooBah` and `X` becomes `x`, while `URL` stays as it
    /// is: a name whose first two characters are both upper case is an
    /// acronym.
    public static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && isUpper(name.charAt(1)) && isUpper(name.charAt(0))) {
            return name;
        }
        char first = name.charAt(0);
        if (first >= 'A' && first <= 'Z') {
            first = (char) (first - 'A' + 'a');
        } else {
            first = Character.toLowerCase(first);
        }
        return first + name.substring(1);
    }

    private static boolean isUpper(char c) {
        return Character.isUpperCase(c);
    }

    public static void flushCaches() {
        INFOS.clear();
    }

    public static void flushFromCaches(Class<?> clz) {
        if (clz == null) {
            throw new NullPointerException();
        }
        INFOS.remove(clz);
    }

    private static final class ClassInfo implements BeanInfo {
        private final BeanDescriptor descriptor;

        ClassInfo(BeanDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public BeanDescriptor getBeanDescriptor() {
            return descriptor;
        }

        @Override
        public int getDefaultPropertyIndex() {
            return -1;
        }

        @Override
        public int getDefaultEventIndex() {
            return -1;
        }
    }
}
