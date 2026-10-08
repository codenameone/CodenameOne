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

/// A factory with up to four formatters: one for a field that has the
/// focus, one for a field that does not, one for a `null` value, and a
/// default that stands in for whichever of those is missing.
public class DefaultFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {

    private JFormattedTextField.AbstractFormatter defaultFormat;
    private JFormattedTextField.AbstractFormatter displayFormat;
    private JFormattedTextField.AbstractFormatter editFormat;
    private JFormattedTextField.AbstractFormatter nullFormat;

    public DefaultFormatterFactory() {
    }

    public DefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat) {
        this(defaultFormat, null);
    }

    public DefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat,
            JFormattedTextField.AbstractFormatter displayFormat) {
        this(defaultFormat, displayFormat, null);
    }

    public DefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat,
            JFormattedTextField.AbstractFormatter displayFormat, JFormattedTextField.AbstractFormatter editFormat) {
        this(defaultFormat, displayFormat, editFormat, null);
    }

    public DefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat,
            JFormattedTextField.AbstractFormatter displayFormat, JFormattedTextField.AbstractFormatter editFormat,
            JFormattedTextField.AbstractFormatter nullFormat) {
        this.defaultFormat = defaultFormat;
        this.displayFormat = displayFormat;
        this.editFormat = editFormat;
        this.nullFormat = nullFormat;
    }

    public void setDefaultFormatter(JFormattedTextField.AbstractFormatter atf) {
        defaultFormat = atf;
    }

    public JFormattedTextField.AbstractFormatter getDefaultFormatter() {
        return defaultFormat;
    }

    public void setDisplayFormatter(JFormattedTextField.AbstractFormatter atf) {
        displayFormat = atf;
    }

    public JFormattedTextField.AbstractFormatter getDisplayFormatter() {
        return displayFormat;
    }

    public void setEditFormatter(JFormattedTextField.AbstractFormatter atf) {
        editFormat = atf;
    }

    public JFormattedTextField.AbstractFormatter getEditFormatter() {
        return editFormat;
    }

    public void setNullFormatter(JFormattedTextField.AbstractFormatter atf) {
        nullFormat = atf;
    }

    public JFormattedTextField.AbstractFormatter getNullFormatter() {
        return nullFormat;
    }

    @Override
    public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField source) {
        JFormattedTextField.AbstractFormatter format = null;
        if (source == null) {
            return null;
        }
        Object value = source.getValue();
        if (value == null) {
            format = getNullFormatter();
        }
        if (format == null) {
            format = source.hasFocus() ? getEditFormatter() : getDisplayFormatter();
            if (format == null) {
                format = getDefaultFormatter();
            }
        }
        return format;
    }
}
