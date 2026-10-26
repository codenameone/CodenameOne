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

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The spinner models against the JDK's, and the spinner's buttons and
/// text field driving a model.
public class SpinnerTest extends KernelTestBase {

    private static final class Count implements ChangeListener {
        int n;
        Object source;

        @Override
        public void stateChanged(ChangeEvent e) {
            n++;
            source = e.getSource();
        }
    }

    private static final class JdkCount implements javax.swing.event.ChangeListener {
        int n;

        @Override
        public void stateChanged(javax.swing.event.ChangeEvent e) {
            n++;
        }
    }

    private static String state(SpinnerModel m) {
        return m.getValue() + " " + m.getPreviousValue() + " " + m.getNextValue();
    }

    private static String state(javax.swing.SpinnerModel m) {
        return m.getValue() + " " + m.getPreviousValue() + " " + m.getNextValue();
    }

    private static String message(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            return String.valueOf(e.getMessage());
        }
        return "no exception";
    }

    @Test
    public void theNumberModelStepsAndStopsAsTheJdkDoes() {
        final SpinnerNumberModel a = new SpinnerNumberModel(5, 0, 10, 4);
        final javax.swing.SpinnerNumberModel b = new javax.swing.SpinnerNumberModel(5, 0, 10, 4);
        Count ours = new Count();
        JdkCount theirs = new JdkCount();
        a.addChangeListener(ours);
        b.addChangeListener(theirs);
        assertEquals(state(b), state(a));
        a.setValue(a.getNextValue());
        b.setValue(b.getNextValue());
        assertEquals("9 5 null", state(a));
        assertEquals(state(b), state(a));
        a.setValue(Integer.valueOf(9));
        b.setValue(Integer.valueOf(9));
        a.setValue(Integer.valueOf(1));
        b.setValue(Integer.valueOf(1));
        assertEquals("1 null 5", state(a));
        assertEquals(state(b), state(a));
        // A value outside the bounds is taken, as on the desktop.
        a.setValue(Integer.valueOf(40));
        b.setValue(Integer.valueOf(40));
        assertEquals(state(b), state(a));
        a.setStepSize(Integer.valueOf(2));
        b.setStepSize(Integer.valueOf(2));
        a.setMaximum(Integer.valueOf(50));
        b.setMaximum(Integer.valueOf(50));
        a.setMaximum(Integer.valueOf(50));
        b.setMaximum(Integer.valueOf(50));
        a.setMinimum(null);
        b.setMinimum(null);
        assertEquals(state(b), state(a));
        assertEquals(theirs.n, ours.n);
        assertSame(a, ours.source);
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                b.setValue("x");
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                a.setValue("x");
            }
        }));
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                new javax.swing.SpinnerNumberModel(11, 0, 10, 1);
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                new SpinnerNumberModel(11, 0, 10, 1);
            }
        }));
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                b.setStepSize(null);
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                a.setStepSize(null);
            }
        }));
    }

    @Test
    public void theNumberModelKeepsTheTypeOfItsValue() {
        SpinnerNumberModel d = new SpinnerNumberModel(0.5, 0.0, 1.0, 0.25);
        javax.swing.SpinnerNumberModel jd = new javax.swing.SpinnerNumberModel(0.5, 0.0, 1.0, 0.25);
        assertEquals(state(jd), state(d));
        assertTrue(d.getNextValue() instanceof Double);
        d.setValue(d.getNextValue());
        d.setValue(d.getNextValue());
        assertEquals("1.0 0.75 null", state(d));

        SpinnerNumberModel l = new SpinnerNumberModel(Long.valueOf(7), null, null, Long.valueOf(3));
        javax.swing.SpinnerNumberModel jl =
                new javax.swing.SpinnerNumberModel(Long.valueOf(7), null, null, Long.valueOf(3));
        assertEquals(state(jl), state(l));
        assertTrue(l.getPreviousValue() instanceof Long);

        SpinnerNumberModel s = new SpinnerNumberModel(Short.valueOf((short) 2), null, null, Short.valueOf((short) 1));
        assertTrue(s.getNextValue() instanceof Short);
        SpinnerNumberModel f = new SpinnerNumberModel(Float.valueOf(1f), null, null, Float.valueOf(0.5f));
        assertEquals(Float.valueOf(1.5f), f.getNextValue());

        SpinnerNumberModel plain = new SpinnerNumberModel();
        assertEquals(state(new javax.swing.SpinnerNumberModel()), state(plain));
        assertEquals(Integer.valueOf(0), plain.getNumber());
    }

    @Test
    public void theListModelWalksItsListAsTheJdkDoes() {
        final SpinnerListModel a = new SpinnerListModel(new Object[]{"red", "green", "blue"});
        final javax.swing.SpinnerListModel b = new javax.swing.SpinnerListModel(new Object[]{"red", "green", "blue"});
        Count ours = new Count();
        JdkCount theirs = new JdkCount();
        a.addChangeListener(ours);
        b.addChangeListener(theirs);
        assertEquals("red null green", state(a));
        assertEquals(state(b), state(a));
        a.setValue("blue");
        b.setValue("blue");
        a.setValue("blue");
        b.setValue("blue");
        assertEquals("blue green null", state(a));
        assertEquals(state(b), state(a));
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                b.setValue("pink");
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                a.setValue("pink");
            }
        }));
        a.setList(Arrays.asList("x", "y"));
        b.setList(Arrays.asList("x", "y"));
        assertEquals(state(b), state(a));
        assertEquals(theirs.n, ours.n);
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                new javax.swing.SpinnerListModel(new Object[0]);
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                new SpinnerListModel(new Object[0]);
            }
        }));
        assertEquals(state(new javax.swing.SpinnerListModel()), state(new SpinnerListModel()));
    }

    @Test
    public void theDateModelStepsItsCalendarFieldAsTheJdkDoes() {
        Calendar c = Calendar.getInstance();
        c.set(2024, Calendar.JANUARY, 30, 10, 15, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date now = c.getTime();
        c.set(Calendar.DAY_OF_MONTH, 29);
        Date start = c.getTime();
        c.set(Calendar.MONTH, Calendar.FEBRUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        Date end = c.getTime();
        final SpinnerDateModel a = new SpinnerDateModel(now, start, end, Calendar.DAY_OF_MONTH);
        final javax.swing.SpinnerDateModel b =
                new javax.swing.SpinnerDateModel(now, start, end, Calendar.DAY_OF_MONTH);
        Count ours = new Count();
        a.addChangeListener(ours);
        assertEquals(state(b), state(a));
        a.setValue(a.getNextValue());
        b.setValue(b.getNextValue());
        a.setValue(a.getNextValue());
        b.setValue(b.getNextValue());
        assertEquals(state(b), state(a));
        assertNull(a.getNextValue());
        assertEquals(end, a.getDate());
        assertEquals(2, ours.n);
        a.setCalendarField(Calendar.HOUR_OF_DAY);
        b.setCalendarField(Calendar.HOUR_OF_DAY);
        assertEquals(state(b), state(a));
        assertEquals(3, ours.n);
        a.setValue(start);
        b.setValue(start);
        assertNull(a.getPreviousValue());
        assertEquals(state(b), state(a));
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                b.setValue("x");
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                a.setValue("x");
            }
        }));
        assertEquals(message(new Runnable() {
            @Override
            public void run() {
                b.setCalendarField(99);
            }
        }), message(new Runnable() {
            @Override
            public void run() {
                a.setCalendarField(99);
            }
        }));
        assertEquals(Calendar.DAY_OF_MONTH, new SpinnerDateModel().getCalendarField());
    }

    private static JTextField field(JSpinner s) {
        return ((JSpinner.DefaultEditor) s.getEditor()).cn1Field();
    }

    private static JButton button(JSpinner s, String name) {
        for (int i = 0; i < s.getComponentCount(); i++) {
            if (name.equals(s.getComponent(i).getName())) {
                return (JButton) s.getComponent(i);
            }
        }
        fail("no " + name);
        return null;
    }

    @Test
    public void theButtonsStepTheModelAndTheEditorFollows() {
        JSpinner s = new JSpinner(new SpinnerNumberModel(5, 0, 7, 1));
        Count changes = new Count();
        s.addChangeListener(changes);
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        assertTrue(s.getEditor() instanceof JSpinner.NumberEditor);
        assertEquals(3, s.getComponentCount());
        assertEquals("5", field(s).getText());
        JButton next = button(s, "Spinner.nextButton");
        JButton previous = button(s, "Spinner.previousButton");
        next.doClick();
        assertEquals(Integer.valueOf(6), s.getValue());
        assertEquals("6", field(s).getText());
        assertSame(s, changes.source);
        next.doClick();
        next.doClick();
        next.doClick();
        assertEquals(Integer.valueOf(7), s.getValue());
        assertEquals(2, changes.n);
        previous.doClick();
        assertEquals(Integer.valueOf(6), s.getValue());
        assertEquals("6", field(s).getText());
        assertEquals(Integer.valueOf(7), s.getNextValue());
        assertEquals(Integer.valueOf(5), s.getPreviousValue());

        // The three children share the row and none is squeezed out.
        Dimension size = s.getSize();
        assertTrue(size.width > 0 && size.height > 0);
        assertTrue(s.getEditor().getWidth() > 0);
        assertTrue(previous.getWidth() > 0 && next.getWidth() > 0);
        assertTrue(s.getEditor().getX() + s.getEditor().getWidth() <= previous.getX());
        assertTrue(previous.getX() + previous.getWidth() <= next.getX());
        assertEquals(size.height, next.getHeight());
        // The field draws its own text natively; painting must simply work.
        assertNotNull(paint(f));

        s.setEnabled(false);
        assertFalse(next.isEnabled());
        assertFalse(field(s).isEnabled());
    }

    @Test
    public void typedTextIsParsedIntoANumberModelAndBadTextIsDropped() throws Exception {
        JSpinner s = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1));
        Count changes = new Count();
        s.addChangeListener(changes);
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        JTextField text = field(s);
        text.setText("42");
        text.postActionEvent();
        assertEquals(Integer.valueOf(42), s.getValue());
        assertEquals(1, changes.n);
        text.setText("nope");
        text.postActionEvent();
        assertEquals(Integer.valueOf(42), s.getValue());
        assertEquals("42", text.getText());
        // Outside the model's range.
        text.setText("101");
        text.postActionEvent();
        assertEquals(Integer.valueOf(42), s.getValue());
        assertEquals("42", text.getText());
        assertEquals(1, changes.n);
        // commitEdit reports what the action swallows.
        text.setText("12x");
        try {
            s.commitEdit();
            fail("12x is not a number");
        } catch (java.text.ParseException expected) {
            assertEquals(Integer.valueOf(42), s.getValue());
        }
        // A step takes in what was typed first.
        text.setText("7");
        button(s, "Spinner.nextButton").doClick();
        assertEquals(Integer.valueOf(8), s.getValue());
        assertEquals("8", text.getText());
        // And drops what does not parse.
        text.setText("zz");
        button(s, "Spinner.previousButton").doClick();
        assertEquals(Integer.valueOf(7), s.getValue());
        assertEquals("7", text.getText());
    }

    @Test
    public void typedTextIsMatchedAgainstAListModel() {
        JSpinner s = new JSpinner(new SpinnerListModel(new Object[]{"red", "green", "blue"}));
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        assertTrue(s.getEditor() instanceof JSpinner.ListEditor);
        JTextField text = field(s);
        assertEquals("red", text.getText());
        text.setText("blue");
        text.postActionEvent();
        assertEquals("blue", s.getValue());
        text.setText("pink");
        text.postActionEvent();
        assertEquals("blue", s.getValue());
        assertEquals("blue", text.getText());
        button(s, "Spinner.nextButton").doClick();
        assertEquals("blue", s.getValue());
        button(s, "Spinner.previousButton").doClick();
        assertEquals("green", s.getValue());
        assertEquals("green", text.getText());
    }

    @Test
    public void aNewModelBringsItsOwnEditorAndAnExplicitEditorStays() {
        JSpinner s = new JSpinner();
        assertEquals(Integer.valueOf(0), s.getValue());
        JComponent first = s.getEditor();
        assertTrue(first instanceof JSpinner.NumberEditor);
        assertSame(s, ((JSpinner.NumberEditor) first).getSpinner());
        assertSame(s.getModel(), ((JSpinner.NumberEditor) first).getModel());
        Count changes = new Count();
        s.addChangeListener(changes);

        SpinnerListModel list = new SpinnerListModel(new Object[]{"a", "b"});
        s.setModel(list);
        assertSame(list, s.getModel());
        assertTrue(s.getEditor() instanceof JSpinner.ListEditor);
        assertEquals(3, s.getComponentCount());
        // The replaced editor no longer listens.
        assertEquals(2, s.getChangeListeners().length);
        s.setValue("b");
        assertEquals(1, changes.n);
        assertEquals("b", field(s).getText());

        SpinnerDateModel dates = new SpinnerDateModel();
        s.setModel(dates);
        assertTrue(s.getEditor() instanceof JSpinner.DateEditor);
        JSpinner.DateEditor de = (JSpinner.DateEditor) s.getEditor();
        assertEquals(de.getFormat().format(dates.getDate()), field(s).getText());

        JLabel mine = new JLabel("mine");
        s.setEditor(mine);
        assertSame(mine, s.getEditor());
        assertEquals(1, s.getChangeListeners().length);
        s.setModel(new SpinnerNumberModel(1, 0, 9, 1));
        assertSame(mine, s.getEditor());
        assertEquals(3, s.getComponentCount());

        try {
            s.setModel(null);
            fail("a null model is refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("null model", expected.getMessage());
        }
        try {
            new JSpinner.ListEditor(s);
            fail("the model is not a list model");
        } catch (IllegalArgumentException expected) {
            assertEquals("model not a SpinnerListModel", expected.getMessage());
        }
        // The refused editor is not left listening.
        assertEquals(1, s.getChangeListeners().length);
    }

    @Test
    public void aModelThatIsNoneOfTheThreeIsShownAndNotEdited() {
        SpinnerModel odd = new AbstractSpinnerModel() {
            private Object value = "one";

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public void setValue(Object v) {
                value = v;
                fireStateChanged();
            }

            @Override
            public Object getNextValue() {
                return "two";
            }

            @Override
            public Object getPreviousValue() {
                return null;
            }
        };
        JSpinner s = new JSpinner(odd);
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        JTextField text = field(s);
        assertFalse(text.isEditable());
        assertEquals("one", text.getText());
        button(s, "Spinner.previousButton").doClick();
        assertEquals("one", s.getValue());
        button(s, "Spinner.nextButton").doClick();
        assertEquals("two", s.getValue());
        assertEquals("two", text.getText());
    }
}
