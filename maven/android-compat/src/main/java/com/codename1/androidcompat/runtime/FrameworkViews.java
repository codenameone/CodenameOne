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
package com.codename1.androidcompat.runtime;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

/// `new` for the framework's own view classes, used when a layout the
/// application did not compile (a framework layout, or one inflated by name)
/// names them. Must cover every class in the runtime's `framework-views.txt`.
public final class FrameworkViews {

    private FrameworkViews() {
    }

    public static View create(String name, Context c, AttributeSet a) {
        String n = name;
        if (n.startsWith("android.widget.")) {
            n = n.substring("android.widget.".length());
        } else if (n.startsWith("android.view.")) {
            n = n.substring("android.view.".length());
        }
        if (n.equals("View")) {
            return new View(c, a);
        }
        if (n.equals("ViewStub")) {
            return new ViewStub(c, a);
        }
        if (n.equals("TextView")) {
            return new TextView(c, a);
        }
        if (n.equals("Button")) {
            return new Button(c, a);
        }
        if (n.equals("EditText")) {
            return new EditText(c, a);
        }
        if (n.equals("ImageView")) {
            return new ImageView(c, a);
        }
        if (n.equals("ImageButton")) {
            return new ImageButton(c, a);
        }
        if (n.equals("LinearLayout")) {
            return new LinearLayout(c, a);
        }
        if (n.equals("FrameLayout")) {
            return new FrameLayout(c, a);
        }
        if (n.equals("Space")) {
            return new Space(c, a);
        }
        if (n.equals("ScrollView")) {
            return new ScrollView(c, a);
        }
        if (n.equals("HorizontalScrollView")) {
            return new HorizontalScrollView(c, a);
        }
        if (n.equals("CheckBox")) {
            return new CheckBox(c, a);
        }
        if (n.equals("RadioButton")) {
            return new RadioButton(c, a);
        }
        if (n.equals("RadioGroup")) {
            return new RadioGroup(c, a);
        }
        if (n.equals("Switch")) {
            return new Switch(c, a);
        }
        if (n.equals("ToggleButton")) {
            return new ToggleButton(c, a);
        }
        if (n.equals("CheckedTextView")) {
            return new CheckedTextView(c, a);
        }
        if (n.equals("ProgressBar")) {
            return new ProgressBar(c, a);
        }
        if (n.equals("SeekBar")) {
            return new SeekBar(c, a);
        }
        if (n.equals("RatingBar")) {
            return new RatingBar(c, a);
        }
        if (n.equals("RelativeLayout")) {
            return new RelativeLayout(c, a);
        }
        if (n.equals("TableLayout")) {
            return new TableLayout(c, a);
        }
        if (n.equals("TableRow")) {
            return new TableRow(c, a);
        }
        if (n.equals("GridLayout")) {
            return new GridLayout(c, a);
        }
        if (n.equals("ListView")) {
            return new ListView(c, a);
        }
        if (n.equals("GridView")) {
            return new GridView(c, a);
        }
        if (n.equals("Spinner")) {
            return new Spinner(c, a);
        }
        return null;
    }
}
