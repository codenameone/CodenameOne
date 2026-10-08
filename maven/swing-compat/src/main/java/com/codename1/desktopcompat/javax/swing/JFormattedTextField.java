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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.javax.swing.text.AbstractDocument;
import com.codename1.desktopcompat.javax.swing.text.DefaultFormatter;
import com.codename1.desktopcompat.javax.swing.text.DefaultFormatterFactory;
import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.DocumentFilter;
import com.codename1.desktopcompat.javax.swing.text.NumberFormatter;
import java.text.ParseException;

/// A text field that holds a value and shows it through a formatter.
///
/// The text is turned back into the value when the edit is committed: by
/// [#commitEdit()], by finishing the edit, or when the field loses the
/// focus, according to [#setFocusLostBehavior(int)].
///
/// ## What differs from the desktop
///
/// A formatter is built from the value's type: a number gets a
/// [NumberFormatter], anything else a [DefaultFormatter]. There is no
/// constructor taking a `java.text.Format`, which the devices do not have
/// in a usable form; wrap a number format in a [NumberFormatter] instead.
public class JFormattedTextField extends JTextField {

    public static final int COMMIT = 0;

    public static final int COMMIT_OR_REVERT = 1;

    public static final int REVERT = 2;

    public static final int PERSIST = 3;

    private AbstractFormatterFactory factory;
    private AbstractFormatter format;
    private Object value;
    private boolean editValid = true;
    private int focusLostBehavior = COMMIT_OR_REVERT;

    public JFormattedTextField() {
    }

    public JFormattedTextField(Object value) {
        setValue(value);
    }

    public JFormattedTextField(AbstractFormatter formatter) {
        this(new DefaultFormatterFactory(formatter));
    }

    public JFormattedTextField(AbstractFormatterFactory factory) {
        setFormatterFactory(factory);
    }

    public JFormattedTextField(AbstractFormatterFactory factory, Object currentValue) {
        this(currentValue);
        setFormatterFactory(factory);
    }

    public void setFocusLostBehavior(int behavior) {
        if (behavior != COMMIT && behavior != COMMIT_OR_REVERT && behavior != PERSIST && behavior != REVERT) {
            throw new IllegalArgumentException("setFocusLostBehavior must be one of: "
                    + "JFormattedTextField.COMMIT, JFormattedTextField.COMMIT_OR_REVERT, "
                    + "JFormattedTextField.PERSIST or JFormattedTextField.REVERT");
        }
        focusLostBehavior = behavior;
    }

    public int getFocusLostBehavior() {
        return focusLostBehavior;
    }

    public void setFormatterFactory(AbstractFormatterFactory tf) {
        AbstractFormatterFactory old = factory;
        factory = tf;
        firePropertyChange("formatterFactory", old, tf);
        setValue(getValue(), true, false);
    }

    public AbstractFormatterFactory getFormatterFactory() {
        return factory;
    }

    /// Installs a formatter, which writes the current value into the
    /// field. Usually the factory decides; this is for a subclass.
    protected void setFormatter(AbstractFormatter format) {
        AbstractFormatter old = this.format;
        if (old != null) {
            old.uninstall();
        }
        editValid = true;
        this.format = format;
        if (format != null) {
            format.install(this);
        }
        firePropertyChange("textFormatter", old, format);
    }

    public AbstractFormatter getFormatter() {
        return format;
    }

    public void setValue(Object value) {
        if (value != null && getFormatterFactory() == null) {
            setFormatterFactory(cn1DefaultFactory(value));
        }
        setValue(value, true, true);
    }

    public Object getValue() {
        return value;
    }

    /// Turns the text into the value. The text itself is left as typed.
    public void commitEdit() throws ParseException {
        AbstractFormatter f = getFormatter();
        if (f != null) {
            setValue(f.stringToValue(getText()), false, true);
        }
    }

    /// Whether the text, as it stands, is something the formatter accepts.
    public boolean isEditValid() {
        AbstractFormatter f = getFormatter();
        if (f == null || !editValid) {
            return editValid;
        }
        try {
            f.stringToValue(getText());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /// Called when the text could not be turned into a value. The desktop
    /// beeps; a device says nothing.
    protected void invalidEdit() {
    }

    @Override
    public void setDocument(Document doc) {
        super.setDocument(doc);
        AbstractFormatter f = format;
        if (f != null && doc instanceof AbstractDocument) {
            DocumentFilter filter = f.getDocumentFilter();
            if (filter != null) {
                ((AbstractDocument) doc).setDocumentFilter(filter);
            }
        }
    }

    /// Finishing the edit commits it first; text that is no value keeps
    /// the action listeners from hearing about it.
    @Override
    public void postActionEvent() {
        if (getFormatter() != null) {
            try {
                commitEdit();
            } catch (ParseException e) {
                invalidEdit();
                return;
            }
        }
        super.postActionEvent();
    }

    @Override
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        if (e.isTemporary()) {
            return;
        }
        if (e.getID() == FocusEvent.FOCUS_LOST && isEditable()) {
            if (focusLostBehavior == COMMIT || focusLostBehavior == COMMIT_OR_REVERT) {
                try {
                    commitEdit();
                    // Shows the value the way the formatter writes it.
                    setValue(getValue(), true, true);
                } catch (ParseException pe) {
                    if (focusLostBehavior == COMMIT_OR_REVERT) {
                        setValue(getValue(), true, true);
                    }
                }
            } else if (focusLostBehavior == REVERT) {
                setValue(getValue(), true, true);
            }
        } else if (e.getID() == FocusEvent.FOCUS_GAINED || e.getID() == FocusEvent.FOCUS_LOST) {
            // The factory may have one formatter for editing and another
            // for display.
            setValue(getValue(), true, false);
        }
    }

    private void setValue(Object value, boolean createFormat, boolean firePC) {
        Object oldValue = this.value;
        this.value = value;
        if (createFormat) {
            AbstractFormatterFactory f = getFormatterFactory();
            setFormatter(f != null ? f.getFormatter(this) : null);
        } else {
            editValid = true;
        }
        if (firePC) {
            firePropertyChange("value", oldValue, value);
        }
    }

    private static AbstractFormatterFactory cn1DefaultFactory(Object type) {
        if (type instanceof Number) {
            NumberFormatter display = new NumberFormatter();
            display.setValueClass(type.getClass());
            NumberFormatter edit = new NumberFormatter();
            edit.setValueClass(type.getClass());
            return new DefaultFormatterFactory(display, display, edit);
        }
        return new DefaultFormatterFactory(new DefaultFormatter());
    }

    /// Makes the formatter a field uses at the moment it asks.
    public abstract static class AbstractFormatterFactory {

        public AbstractFormatterFactory() {
        }

        public abstract AbstractFormatter getFormatter(JFormattedTextField tf);
    }

    /// Turns a value into the text a field shows, and the text back.
    public abstract static class AbstractFormatter {

        private JFormattedTextField ftf;

        public AbstractFormatter() {
        }

        /// Attaches this formatter to a field: writes the field's value
        /// into it as text and installs the document filter, if there is
        /// one. A value that cannot be written leaves the field empty and
        /// its edit invalid.
        public void install(JFormattedTextField ftf) {
            if (this.ftf != null) {
                uninstall();
            }
            this.ftf = ftf;
            if (ftf != null) {
                try {
                    ftf.setText(valueToString(ftf.getValue()));
                } catch (ParseException pe) {
                    ftf.setText("");
                    setEditValid(false);
                }
                Document doc = ftf.getDocument();
                DocumentFilter filter = getDocumentFilter();
                if (filter != null && doc instanceof AbstractDocument) {
                    ((AbstractDocument) doc).setDocumentFilter(filter);
                }
            }
        }

        public void uninstall() {
            if (ftf != null) {
                Document doc = ftf.getDocument();
                if (doc instanceof AbstractDocument && getDocumentFilter() != null) {
                    ((AbstractDocument) doc).setDocumentFilter(null);
                }
            }
        }

        public abstract Object stringToValue(String text) throws ParseException;

        public abstract String valueToString(Object value) throws ParseException;

        protected JFormattedTextField getFormattedTextField() {
            return ftf;
        }

        protected void invalidEdit() {
            if (ftf != null) {
                ftf.invalidEdit();
            }
        }

        protected void setEditValid(boolean valid) {
            if (ftf != null) {
                ftf.editValid = valid;
            }
        }

        /// A filter to restrict what can be typed; none by default.
        protected DocumentFilter getDocumentFilter() {
            return null;
        }
    }
}
