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
package com.codename1.fxcompat.runtime;

import javafx.beans.property.ReadOnlyProperty;

/// The text properties describe themselves with.
public final class PropertyText {

    private PropertyText() {
    }

    /// Builds `Type [bean: b, name: n, tail]`, leaving out a missing bean
    /// and a missing or empty name.
    public static String describe(String type, ReadOnlyProperty<?> property, String tail) {
        StringBuilder result = new StringBuilder(type);
        result.append(" [");
        Object bean = property.getBean();
        String name = property.getName();
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if (name != null && name.length() > 0) {
            result.append("name: ").append(name).append(", ");
        }
        return result.append(tail).append(']').toString();
    }

    /// Returns `Bean.name : ` for a property that has both, otherwise an
    /// empty string; the prefix of the error a bound property raises.
    public static String owner(ReadOnlyProperty<?> property) {
        Object bean = property.getBean();
        String name = property.getName();
        if (bean != null && name != null) {
            return bean.getClass().getSimpleName() + "." + name + " : ";
        }
        return "";
    }
}
