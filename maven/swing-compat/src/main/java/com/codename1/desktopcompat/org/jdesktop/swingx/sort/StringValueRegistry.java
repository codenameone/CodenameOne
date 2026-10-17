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
package com.codename1.desktopcompat.org.jdesktop.swingx.sort;

import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValues;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The converters of a table's cells: one set for a column wins over one
/// set for the column's class, which wins over the value's `toString`.
///
/// A class is matched exactly, then by the first registered class it can
/// be assigned to; a device class cannot be asked for its superclass.
public final class StringValueRegistry implements StringValueProvider {

    private final Map<Integer, StringValue> perColumn = new HashMap<Integer, StringValue>();
    private final Map<Class<?>, StringValue> perClass = new HashMap<Class<?>, StringValue>();
    private final List<Class<?>> classOrder = new ArrayList<Class<?>>();
    private final Map<Integer, Class<?>> classPerColumn = new HashMap<Integer, Class<?>>();

    public StringValueRegistry() {
    }

    @Override
    public StringValue getStringValue(int row, int column) {
        StringValue sv = perColumn.get(Integer.valueOf(column));
        if (sv == null) {
            sv = getStringValue(classPerColumn.get(Integer.valueOf(column)));
        }
        return sv != null ? sv : StringValues.TO_STRING;
    }

    /// Sets the converter of a column; `null` removes it.
    public void setStringValue(StringValue sv, int column) {
        if (sv == null) {
            perColumn.remove(Integer.valueOf(column));
        } else {
            perColumn.put(Integer.valueOf(column), sv);
        }
    }

    public void clearColumnStringValues() {
        perColumn.clear();
    }

    /// Sets the converter of a class; `null` removes it.
    public void setStringValue(StringValue sv, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (sv == null) {
            perClass.remove(clazz);
            classOrder.remove(clazz);
        } else {
            if (perClass.put(clazz, sv) == null) {
                classOrder.add(clazz);
            }
        }
    }

    /// The converter registered for a class or for one it extends or
    /// implements, or `null`.
    public StringValue getStringValue(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        StringValue sv = perClass.get(clazz);
        if (sv != null) {
            return sv;
        }
        Class<?> best = null;
        for (int i = 0; i < classOrder.size(); i++) {
            Class<?> c = classOrder.get(i);
            if (c != Object.class && c.isAssignableFrom(clazz) && (best == null || best.isAssignableFrom(c))) {
                best = c;
            }
        }
        if (best == null) {
            return perClass.get(Object.class);
        }
        return perClass.get(best);
    }

    /// Says which class the values of a column have; `null` forgets it.
    public void setColumnClass(Class<?> clazz, int column) {
        if (clazz == null) {
            classPerColumn.remove(Integer.valueOf(column));
        } else {
            classPerColumn.put(Integer.valueOf(column), clazz);
        }
    }

    /// Replaces the classes of all columns; `null` forgets them all.
    public void setColumnClasses(Map<Integer, Class<?>> classPerColumn) {
        this.classPerColumn.clear();
        if (classPerColumn != null) {
            this.classPerColumn.putAll(classPerColumn);
        }
    }
}
