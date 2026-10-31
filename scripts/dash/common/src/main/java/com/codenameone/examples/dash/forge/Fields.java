/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.forge;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;

/// The editor's inputs: each one a labelled row that writes its value back
/// the moment it changes, so a screen never has a "read the form" step to
/// forget a field in.
public final class Fields {
    public interface IntSink {
        void set(int value);
    }

    public interface TextSink {
        void set(String value);
    }

    public interface BoolSink {
        void set(boolean value);
    }

    private static final float LABEL_MM = 20f;

    private Fields() {
    }

    public static Container row(String label, Component input) {
        Container c = new Container(new BorderLayout());
        c.setUIID("ForgeRow");
        Label l = new Label(label, "ForgeLabel");
        // One label column: the fields of a form start at the same place
        // unless a label is longer than the column.
        l.setPreferredW(Math.max(l.getPreferredW(), CN.convertToPixels(LABEL_MM)));
        c.add(BorderLayout.WEST, l).add(BorderLayout.CENTER, input);
        return c;
    }

    public static Container text(String label, String name, String value, int max, final TextSink sink) {
        final TextField t = new TextField(value == null ? "" : value, label, 20, TextArea.ANY);
        t.setName(name);
        t.setMaxSize(max);
        t.setUIID("ForgeField");
        t.addDataChangedListener((type, index) -> sink.set(t.getText()));
        return row(label, t);
    }

    /// Several lines of text: a briefing, a line of dialog.
    public static Container area(String label, String name, String value, int max, final TextSink sink) {
        final TextArea t = new TextArea(value == null ? "" : value, 3, 20);
        t.setName(name);
        t.setMaxSize(max);
        t.setGrowByContent(true);
        t.setUIID("ForgeField");
        t.addDataChangedListener((type, index) -> sink.set(t.getText()));
        Container c = BoxLayout.encloseY(new Label(label, "ForgeLabel"), t);
        c.setUIID("ForgeRow");
        return c;
    }

    /// A whole number with buttons either side: easier on a phone than a
    /// keyboard, and it can never hold a value out of range.
    public static Container stepper(String label, String name, int value, final int min, final int max, final int step,
            final String unit, final IntSink sink) {
        return stepper(label, name, value, min, max, step, 1, unit, sink);
    }

    /// A stepper over tenths, shown as a decimal: "2.5 tiles".
    public static Container tenths(String label, String name, int value, int min, int max, int step, String unit,
            IntSink sink) {
        return stepper(label, name, value, min, max, step, 10, unit, sink);
    }

    private static String show(int v, int scale, String unit) {
        return (scale == 10 ? v / 10 + "." + v % 10 : String.valueOf(v)) + unit;
    }

    private static Container stepper(String label, String name, int value, final int min, final int max,
            final int step, final int scale, final String unit, final IntSink sink) {
        final int[] v = {Math.max(min, Math.min(max, value))};
        final Label shown = new Label(show(v[0], scale, unit), "ForgeValue");
        shown.setName(name);
        // One width on every stepper, or the widest value this one can show
        // if that is wider, so the + button stays put as the value grows and
        // the steppers on a form line up.
        String wide = show(max, scale, unit).length() >= show(min, scale, unit).length()
                ? show(max, scale, unit) : show(min, scale, unit);
        shown.setText(wide);
        shown.setPreferredW(Math.max(shown.getPreferredW(), com.codename1.ui.CN.convertToPixels(19)));
        shown.setText(show(v[0], scale, unit));
        Button less = new Button("-", "ForgeStep");
        Button more = new Button("+", "ForgeStep");
        less.setName(name + "-less");
        more.setName(name + "-more");
        less.addActionListener(e -> {
            v[0] = Math.max(min, v[0] - step);
            shown.setText(show(v[0], scale, unit));
            shown.getParent().revalidate();
            sink.set(v[0]);
        });
        more.addActionListener(e -> {
            v[0] = Math.min(max, v[0] + step);
            shown.setText(show(v[0], scale, unit));
            shown.getParent().revalidate();
            sink.set(v[0]);
        });
        return row(label, BoxLayout.encloseX(less, shown, more));
    }

    public static Container choice(String label, String name, String[] options, int selected, final IntSink sink) {
        final Picker p = new Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setStrings(options);
        p.setSelectedString(options[Math.max(0, Math.min(options.length - 1, selected))]);
        p.setName(name);
        p.setUIID("ForgeField");
        // A drop-down arrow at the end, so it reads as a choice, not as a
        // field to type into.
        p.setMaterialIcon(com.codename1.ui.FontImage.MATERIAL_ARROW_DROP_DOWN, 3.6f);
        p.setTextPosition(com.codename1.ui.Component.LEFT);
        p.addActionListener(e -> {
            String s = p.getSelectedString();
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(s)) {
                    sink.set(i);
                    return;
                }
            }
        });
        return row(label, p);
    }

    public static CheckBox check(String label, String name, boolean value, final BoolSink sink) {
        final CheckBox c = new CheckBox(label);
        c.setUIID("ForgeCheck");
        c.setName(name);
        c.setSelected(value);
        c.addActionListener(e -> sink.set(c.isSelected()));
        return c;
    }

    public static SpanLabel help(String text) {
        SpanLabel l = new SpanLabel(text);
        l.setTextUIID("ForgeHelp");
        return l;
    }

    /// Index of `value` in `options`, or 0.
    public static int indexOf(String[] options, String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    /// A slug made from a name: what a new card, arena or mission is called
    /// inside its pack until its author renames it.
    public static String slug(String name, String fallback) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length() && sb.length() < 30; i++) {
            char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
            }
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                sb.append(c);
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
                sb.append('-');
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.length() < 2 ? fallback : sb.toString();
    }
}
