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
package com.codename1.desktopcompat.javax.swing.text;

import com.codename1.desktopcompat.javax.swing.JFormattedTextField;
import java.text.ParseException;

/// Formats any value through its `toString()` and reads it back as an
/// instance of the value class.
///
/// ## What differs from the desktop
///
/// The desktop builds the value by calling, through reflection, whatever
/// constructor of the value class takes a string. A device has no such
/// reflection, so the classes that can be read back are `String`, the
/// boxed numbers and `Boolean`; text for any other class comes back as
/// the string itself.
public class DefaultFormatter extends JFormattedTextField.AbstractFormatter {

    private boolean allowsInvalid = true;
    private boolean overwriteMode = true;
    private boolean commitOnEdit;
    private Class<?> valueClass;

    public DefaultFormatter() {
    }

    public void setCommitsOnValidEdit(boolean commit) {
        commitOnEdit = commit;
    }

    public boolean getCommitsOnValidEdit() {
        return commitOnEdit;
    }

    public void setOverwriteMode(boolean overwriteMode) {
        this.overwriteMode = overwriteMode;
    }

    public boolean getOverwriteMode() {
        return overwriteMode;
    }

    public void setAllowsInvalid(boolean allowsInvalid) {
        this.allowsInvalid = allowsInvalid;
    }

    public boolean getAllowsInvalid() {
        return allowsInvalid;
    }

    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    @Override
    public Object stringToValue(String string) throws ParseException {
        Class<?> vc = getValueClass();
        JFormattedTextField ftf = getFormattedTextField();
        if (vc == null && ftf != null) {
            Object value = ftf.getValue();
            if (value != null) {
                vc = value.getClass();
            }
        }
        if (vc == null || string == null || vc == String.class) {
            return string;
        }
        try {
            if (vc == Integer.class) {
                return Integer.valueOf(Integer.parseInt(string));
            } else if (vc == Long.class) {
                return Long.valueOf(Long.parseLong(string));
            } else if (vc == Double.class) {
                return Double.valueOf(Double.parseDouble(string));
            } else if (vc == Float.class) {
                return Float.valueOf(Float.parseFloat(string));
            } else if (vc == Short.class) {
                return Short.valueOf(Short.parseShort(string));
            } else if (vc == Byte.class) {
                return Byte.valueOf(Byte.parseByte(string));
            }
        } catch (NumberFormatException nfe) {
            throw new ParseException("Error creating instance", 0);
        }
        if (vc == Boolean.class) {
            return Boolean.valueOf("true".equalsIgnoreCase(string));
        }
        return string;
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        return value == null ? "" : value.toString();
    }
}
