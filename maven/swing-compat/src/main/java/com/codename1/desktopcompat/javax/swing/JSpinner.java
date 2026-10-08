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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/// A value stepped through the sequence of a spinner model: an editor
/// showing the value, and two buttons that go to the previous and the next
/// one.
///
/// The spinner is a container of three children laid out in a row, the
/// editor first and the two buttons after it, side by side so each is a
/// touch target of its own. The editor holds a text field; finishing an
/// edit there (Enter) parses the text back into the model for the number,
/// list and date models, and text that does not parse, or parses to a value
/// outside the model's range, is replaced by the current value.
///
/// Not supported: the editors hold a plain text field, not a formatted
/// one, so `DefaultEditor.getTextField()` is absent and `propertyChange`
/// has nothing to react to; the date editor's default pattern is
/// `M/d/yy h:mm a` whatever the locale; a list editor does not complete
/// what is typed; the editor is made when it is first needed, not in the
/// constructor; there is no UI delegate.
public class JSpinner extends JComponent {

    private static final String NUMBER_PATTERN = "#,##0.###";
    private static final String DATE_PATTERN = "M/d/yy h:mm a";
    private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 20;

    private SpinnerModel model;
    private JComponent editor;
    private boolean editorExplicitlySet;
    private ChangeEvent changeEvent;
    private final ChangeListener modelListener = new ModelListener();
    private final JButton nextButton = new JButton(new Arrow(true));
    private final JButton previousButton = new JButton(new Arrow(false));

    public JSpinner(SpinnerModel model) {
        if (model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.model = model;
        model.addChangeListener(modelListener);
        nextButton.setName("Spinner.nextButton");
        previousButton.setName("Spinner.previousButton");
        nextButton.addActionListener(new Step(true));
        previousButton.addActionListener(new Step(false));
        setLayout(new Row());
        add(previousButton);
        add(nextButton);
    }

    public JSpinner() {
        this(new SpinnerNumberModel());
    }

    /// The editor, made by `createEditor` the first time it is needed.
    private JComponent editor() {
        if (editor == null) {
            editor = createEditor(model);
            add(editor, 0);
        }
        return editor;
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        // The editor has to be a child before the children get their peers.
        editor();
        return super.cn1CreatePeer();
    }

    /// Makes the editor that suits the model: a date, list or number
    /// editor for those models, else one that only shows the value.
    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof SpinnerDateModel) {
            return new DateEditor(this);
        } else if (model instanceof SpinnerListModel) {
            return new ListEditor(this);
        } else if (model instanceof SpinnerNumberModel) {
            return new NumberEditor(this);
        }
        return new DefaultEditor(this);
    }

    public void setModel(SpinnerModel model) {
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        if (!model.equals(this.model)) {
            SpinnerModel old = this.model;
            this.model = model;
            old.removeChangeListener(modelListener);
            model.addChangeListener(modelListener);
            firePropertyChange("model", old, model);
            if (!editorExplicitlySet) {
                setEditor(createEditor(model));
                editorExplicitlySet = false;
            }
            revalidate();
            repaint();
        }
    }

    public SpinnerModel getModel() {
        return model;
    }

    public Object getValue() {
        return model.getValue();
    }

    public void setValue(Object value) {
        model.setValue(value);
    }

    public Object getNextValue() {
        return model.getNextValue();
    }

    public Object getPreviousValue() {
        return model.getPreviousValue();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    /// Replaces the editor. A default editor that is replaced stops
    /// listening to this spinner.
    public void setEditor(JComponent editor) {
        if (editor == null) {
            throw new IllegalArgumentException("null editor");
        }
        if (!editor.equals(this.editor)) {
            JComponent old = this.editor;
            this.editor = editor;
            if (old != null) {
                remove(old);
            }
            if (old instanceof DefaultEditor) {
                ((DefaultEditor) old).dismiss(this);
            }
            add(editor, 0);
            editorExplicitlySet = true;
            firePropertyChange("editor", old, editor);
            revalidate();
            repaint();
        }
    }

    public JComponent getEditor() {
        return editor();
    }

    /// Parses the text being edited into the model. Text that does not
    /// parse throws and leaves the model as it is.
    public void commitEdit() throws ParseException {
        JComponent e = editor();
        if (e instanceof DefaultEditor) {
            ((DefaultEditor) e).commitEdit();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        nextButton.setEnabled(enabled);
        previousButton.setEnabled(enabled);
        JComponent e = editor();
        e.setEnabled(enabled);
        if (e instanceof DefaultEditor) {
            ((DefaultEditor) e).field.setEnabled(enabled);
        }
    }

    /// Goes to the next or the previous value, after taking in what was
    /// being typed; text that does not parse is dropped.
    private void step(boolean up) {
        JComponent e = editor();
        try {
            commitEdit();
        } catch (ParseException ex) {
            if (e instanceof DefaultEditor) {
                ((DefaultEditor) e).show(getValue());
            }
        }
        Object v = up ? getNextValue() : getPreviousValue();
        if (v != null) {
            try {
                setValue(v);
            } catch (IllegalArgumentException refused) {
                // The model refused its own next value; stay where we are.
                repaint();
            }
        }
    }

    private final class Step implements ActionListener {
        private final boolean up;

        Step(boolean up) {
            this.up = up;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            step(up);
        }
    }

    private final class ModelListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }

    /// The editor, then the previous and the next button, in a row as
    /// high as the highest of them.
    private final class Row implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        /// A button is never narrower or lower than a pointer can hit,
        /// whatever the theme gives a button that has only an icon.
        private Dimension buttonSize(JButton b) {
            Dimension d = b.getPreferredSize();
            return new Dimension(Math.max(d.width, BUTTON_WIDTH), Math.max(d.height, BUTTON_HEIGHT));
        }

        private Dimension size(boolean minimum) {
            JComponent e = editor();
            Dimension ed = minimum ? e.getMinimumSize() : e.getPreferredSize();
            Dimension p = buttonSize(previousButton);
            Dimension n = buttonSize(nextButton);
            Insets in = getInsets();
            return new Dimension(ed.width + p.width + n.width + in.left + in.right,
                    Math.max(ed.height, Math.max(p.height, n.height)) + in.top + in.bottom);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return size(false);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return size(true);
        }

        @Override
        public void layoutContainer(Container parent) {
            JComponent e = editor();
            Insets in = getInsets();
            int x = in.left;
            int y = in.top;
            int w = Math.max(0, getWidth() - in.left - in.right);
            int h = Math.max(0, getHeight() - in.top - in.bottom);
            int pw = Math.min(buttonSize(previousButton).width, w / 3);
            int nw = Math.min(buttonSize(nextButton).width, w / 3);
            e.setBounds(x, y, w - pw - nw, h);
            previousButton.setBounds(x + w - pw - nw, y, pw, h);
            nextButton.setBounds(x + w - nw, y, nw, h);
        }
    }

    /// A small triangle pointing up or down, in the button's foreground.
    private static final class Arrow implements Icon {
        private static final int WIDTH = 9;
        private static final int HEIGHT = 6;
        private final boolean up;

        Arrow(boolean up) {
            this.up = up;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color fg = c == null ? null : c.getForeground();
            g.setColor(fg != null ? fg : Color.DARK_GRAY);
            int[] xs = {x, x + WIDTH - 1, x + WIDTH / 2};
            int[] ys = up ? new int[]{y + HEIGHT - 1, y + HEIGHT - 1, y} : new int[]{y, y, y + HEIGHT - 1};
            g.fillPolygon(xs, ys, 3);
        }

        @Override
        public int getIconWidth() {
            return WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }
    }

    // ---------------------------------------------------------- editors

    /// Refuses a spinner whose model an editor cannot edit, before the
    /// editor has started listening to it.
    private static JSpinner checked(JSpinner spinner, boolean suits, String message) {
        if (!suits) {
            throw new IllegalArgumentException(message);
        }
        return spinner;
    }

    /// How an editor turns a value into text and text into a value.
    private static class Codec {

        boolean editable() {
            return false;
        }

        String format(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        /// The value `text` stands for; throws when it stands for none.
        Object parse(String text, SpinnerModel model) throws ParseException {
            return model.getValue();
        }

        /// How many characters the field should have room for.
        int columns(SpinnerModel model) {
            return format(model.getValue()).length();
        }
    }

    private static final class NumberCodec extends Codec {
        private final DecimalFormat format;

        NumberCodec(DecimalFormat format) {
            this.format = format;
        }

        @Override
        boolean editable() {
            return true;
        }

        private static boolean integral(Object o) {
            return o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte;
        }

        @Override
        String format(Object value) {
            if (integral(value)) {
                return format.format(((Number) value).longValue());
            }
            if (value instanceof Number) {
                return format.format(((Number) value).doubleValue());
            }
            return super.format(value);
        }

        @Override
        Object parse(String text, SpinnerModel model) throws ParseException {
            String t = text.trim();
            ParsePosition pos = new ParsePosition(0);
            Number n = t.length() == 0 ? null : format.parse(t, pos);
            if (n == null || pos.getIndex() != t.length()) {
                throw new ParseException(text, pos.getIndex());
            }
            Object current = model.getValue();
            Number v;
            if (current instanceof Integer) {
                v = Integer.valueOf(n.intValue());
            } else if (current instanceof Long) {
                v = Long.valueOf(n.longValue());
            } else if (current instanceof Float) {
                v = Float.valueOf(n.floatValue());
            } else if (current instanceof Short) {
                v = Short.valueOf(n.shortValue());
            } else if (current instanceof Byte) {
                v = Byte.valueOf(n.byteValue());
            } else {
                v = Double.valueOf(n.doubleValue());
            }
            if (model instanceof SpinnerNumberModel) {
                SpinnerNumberModel m = (SpinnerNumberModel) model;
                if ((m.getMinimum() != null && SpinnerNumberModel.compare(m.getMinimum(), v) > 0)
                        || (m.getMaximum() != null && SpinnerNumberModel.compare(m.getMaximum(), v) < 0)) {
                    throw new ParseException(text, 0);
                }
            }
            return v;
        }

        @Override
        int columns(SpinnerModel model) {
            int most = format(model.getValue()).length();
            if (model instanceof SpinnerNumberModel) {
                SpinnerNumberModel m = (SpinnerNumberModel) model;
                if (m.getMinimum() instanceof Number) {
                    most = Math.max(most, format(m.getMinimum()).length());
                }
                if (m.getMaximum() instanceof Number) {
                    most = Math.max(most, format(m.getMaximum()).length());
                }
            }
            return most;
        }
    }

    private static final class ListCodec extends Codec {

        @Override
        boolean editable() {
            return true;
        }

        @Override
        Object parse(String text, SpinnerModel model) throws ParseException {
            if (model instanceof SpinnerListModel) {
                List<?> list = ((SpinnerListModel) model).getList();
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    if (format(o).equals(text)) {
                        return o;
                    }
                }
            }
            throw new ParseException(text, 0);
        }

        @Override
        int columns(SpinnerModel model) {
            int most = 0;
            if (model instanceof SpinnerListModel) {
                List<?> list = ((SpinnerListModel) model).getList();
                for (int i = 0; i < list.size(); i++) {
                    most = Math.max(most, format(list.get(i)).length());
                }
            }
            return most;
        }
    }

    private static final class DateCodec extends Codec {
        private final SimpleDateFormat format;

        DateCodec(SimpleDateFormat format) {
            this.format = format;
        }

        @Override
        boolean editable() {
            return true;
        }

        @Override
        String format(Object value) {
            if (value instanceof Date) {
                return format.format((Date) value);
            }
            return super.format(value);
        }

        @Override
        Object parse(String text, SpinnerModel model) throws ParseException {
            Date d = format.parse(text.trim());
            if (d == null) {
                throw new ParseException(text, 0);
            }
            if (model instanceof SpinnerDateModel) {
                SpinnerDateModel m = (SpinnerDateModel) model;
                if ((m.getStart() instanceof Date && ((Date) m.getStart()).getTime() > d.getTime())
                        || (m.getEnd() instanceof Date && ((Date) m.getEnd()).getTime() < d.getTime())) {
                    throw new ParseException(text, 0);
                }
            }
            return d;
        }
    }

    /// The editor of a spinner whose model is none of the three kinds the
    /// other editors know: a text field that shows the value and cannot be
    /// edited. It follows the spinner's value until it is dismissed.
    public static class DefaultEditor extends JPanel implements PropertyChangeListener, ChangeListener,
            LayoutManager {

        private final JFormattedTextField field = new JFormattedTextField();
        private final Codec codec;
        private final Relay relay = new Relay(this);

        public DefaultEditor(JSpinner spinner) {
            this(spinner, new Codec());
        }

        DefaultEditor(JSpinner spinner, Codec codec) {
            super(null);
            this.codec = codec;
            setOpaque(false);
            field.setName("Spinner.formattedTextField");
            field.setEditable(codec.editable());
            field.setColumns(Math.max(3, codec.columns(spinner.getModel())));
            field.setText(codec.format(spinner.getValue()));
            field.addActionListener(new Commit(this));
            add(field);
            // Handing out the editor itself from its constructor would let
            // a subclass be called before it is built, so both go through
            // a relay.
            setLayout(relay);
            spinner.addChangeListener(relay);
        }

        /// The field the value is shown and typed in. It has no formatter of
        /// its own: the editor formats and parses the value.
        public JFormattedTextField getTextField() {
            return field;
        }

        /// The text field, for the spinner and the tests.
        JTextField cn1Field() {
            return field;
        }

        void show(Object value) {
            field.setText(codec.format(value));
        }

        /// Stops following `spinner`.
        public void dismiss(JSpinner spinner) {
            spinner.removeChangeListener(relay);
        }

        /// The spinner this editor is in, or `null`.
        public JSpinner getSpinner() {
            for (Component c = this; c != null; c = c.getParent()) {
                if (c instanceof JSpinner) {
                    return (JSpinner) c;
                }
            }
            return null;
        }

        /// Shows the spinner's new value.
        @Override
        public void stateChanged(ChangeEvent e) {
            Object source = e.getSource();
            if (source instanceof JSpinner) {
                show(((JSpinner) source).getValue());
            }
        }

        /// Does nothing: the text field reports no property changes.
        @Override
        public void propertyChange(PropertyChangeEvent e) {
        }

        @Override
        public void addLayoutComponent(String name, Component child) {
        }

        @Override
        public void removeLayoutComponent(Component child) {
        }

        private Dimension grown(Dimension d) {
            Insets in = getInsets();
            return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return grown(field.getPreferredSize());
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return grown(field.getMinimumSize());
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = getInsets();
            field.setBounds(in.left, in.top, Math.max(0, getWidth() - in.left - in.right),
                    Math.max(0, getHeight() - in.top - in.bottom));
        }

        /// Parses the text into the spinner's model and shows the value
        /// the model then has. A value the model refuses is dropped.
        public void commitEdit() throws ParseException {
            JSpinner spinner = getSpinner();
            if (spinner == null || !codec.editable()) {
                return;
            }
            Object v = codec.parse(field.getText(), spinner.getModel());
            try {
                spinner.setValue(v);
            } catch (IllegalArgumentException refused) {
                show(spinner.getValue());
                return;
            }
            show(spinner.getValue());
        }

        /// Enter in the text field: take the text in, or put the value
        /// back when the text stands for none.
        void cn1Accept() {
            try {
                commitEdit();
            } catch (ParseException e) {
                JSpinner spinner = getSpinner();
                if (spinner != null) {
                    show(spinner.getValue());
                }
            }
        }
    }

    /// Passes the spinner's changes and the editor's layout on to an
    /// editor.
    private static final class Relay implements ChangeListener, LayoutManager {
        private final DefaultEditor target;

        Relay(DefaultEditor target) {
            this.target = target;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            target.stateChanged(e);
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            target.addLayoutComponent(name, comp);
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            target.removeLayoutComponent(comp);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return target.preferredLayoutSize(parent);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return target.minimumLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            target.layoutContainer(parent);
        }
    }

    private static final class Commit implements ActionListener {
        private final DefaultEditor target;

        Commit(DefaultEditor target) {
            this.target = target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            target.cn1Accept();
        }
    }

    /// The editor of a spinner over numbers: the number in a decimal
    /// format, by default with grouping and up to three fraction digits.
    public static class NumberEditor extends DefaultEditor {

        private final DecimalFormat format;

        public NumberEditor(JSpinner spinner) {
            this(spinner, NUMBER_PATTERN);
        }

        public NumberEditor(JSpinner spinner, String decimalFormatPattern) {
            this(spinner, new DecimalFormat(decimalFormatPattern));
        }

        private NumberEditor(JSpinner spinner, DecimalFormat format) {
            super(checked(spinner, spinner.getModel() instanceof SpinnerNumberModel,
                    "model not a SpinnerNumberModel"), new NumberCodec(format));
            this.format = format;
        }

        public DecimalFormat getFormat() {
            return format;
        }

        public SpinnerNumberModel getModel() {
            JSpinner spinner = getSpinner();
            SpinnerModel m = spinner == null ? null : spinner.getModel();
            return m instanceof SpinnerNumberModel ? (SpinnerNumberModel) m : null;
        }
    }

    /// The editor of a spinner over dates: the date in a simple date
    /// format.
    public static class DateEditor extends DefaultEditor {

        private final SimpleDateFormat format;

        public DateEditor(JSpinner spinner) {
            this(spinner, DATE_PATTERN);
        }

        public DateEditor(JSpinner spinner, String dateFormatPattern) {
            this(spinner, new SimpleDateFormat(dateFormatPattern));
        }

        private DateEditor(JSpinner spinner, SimpleDateFormat format) {
            super(checked(spinner, spinner.getModel() instanceof SpinnerDateModel,
                    "model not a SpinnerDateModel"), new DateCodec(format));
            this.format = format;
        }

        public SimpleDateFormat getFormat() {
            return format;
        }

        public SpinnerDateModel getModel() {
            JSpinner spinner = getSpinner();
            SpinnerModel m = spinner == null ? null : spinner.getModel();
            return m instanceof SpinnerDateModel ? (SpinnerDateModel) m : null;
        }
    }

    /// The editor of a spinner over a list: the text of the element, and
    /// text typed is taken when it is the text of an element.
    public static class ListEditor extends DefaultEditor {

        public ListEditor(JSpinner spinner) {
            super(checked(spinner, spinner.getModel() instanceof SpinnerListModel,
                    "model not a SpinnerListModel"), new ListCodec());
        }

        public SpinnerListModel getModel() {
            JSpinner spinner = getSpinner();
            SpinnerModel m = spinner == null ? null : spinner.getModel();
            return m instanceof SpinnerListModel ? (SpinnerListModel) m : null;
        }
    }
}
