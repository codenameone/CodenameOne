/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.TextEditingValue;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Text;
import dart.core.DartList;
import dart.runtime.Funcs;

/// Flutter's {@code Autocomplete}: a text field with the options optionsBuilder
/// answers for its text listed under it; tapping one fills the field and calls
/// onSelected. The list is laid out below the field rather than in an overlay.
public class Autocomplete<T> extends StatefulWidget {

    private Funcs.Func1<TextEditingValue, Iterable<T>> optionsBuilder;
    private Funcs.VoidFunc1<T> onSelected;
    private Funcs.Func1<T, String> displayStringForOption;

    public void optionsBuilder(Funcs.Func1<TextEditingValue, Iterable<T>> v) { this.optionsBuilder = v; }
    public void onSelected(Funcs.VoidFunc1<T> v) { this.onSelected = v; }
    public void displayStringForOption(Funcs.Func1<T, String> v) { this.displayStringForOption = v; }
    public void optionsMaxHeight(double v) { }

    /// Flutter's {@code RawAutocomplete.defaultStringForOption}: the option's toString.
    public static String defaultStringForOption(Object option) {
        return String.valueOf(option);
    }

    String display(T option) {
        return displayStringForOption != null ? displayStringForOption.call(option)
                : defaultStringForOption(option);
    }

    /// The options for a text, as optionsBuilder answers them.
    public DartList<T> optionsFor(String text) {
        DartList<T> out = DartList.of();
        if (optionsBuilder == null) {
            return out;
        }
        TextEditingValue v = new TextEditingValue();
        v.text(text);
        Iterable<T> it = optionsBuilder.call(v);
        if (it != null) {
            for (T o : it) {
                out.add(o);
            }
        }
        return out;
    }

    @Override
    public State<Autocomplete<T>> createState() {
        return new AutocompleteState<T>();
    }

    static final class AutocompleteState<T> extends State<Autocomplete<T>> {
        private final TextEditingController controller = new TextEditingController();
        private boolean picked;

        @Override
        public Widget build(BuildContext context) {
            final Autocomplete<T> w = widget();
            TextField field = new TextField();
            field.controller(controller);
            field.onChanged(s -> setState(() -> picked = false));
            DartList<Widget> rows = DartList.of();
            rows.add(field);
            if (!picked && controller.text().length() > 0) {
                for (final T o : w.optionsFor(controller.text())) {
                    ListTile tile = new ListTile();
                    tile.title(new Text(w.display(o)));
                    tile.onTap(() -> setState(() -> {
                        controller.text(w.display(o));
                        picked = true;
                        if (w.onSelected != null) {
                            w.onSelected.call(o);
                        }
                    }));
                    rows.add(tile);
                }
            }
            Column col = new Column();
            col.children(rows);
            return col;
        }
    }
}
