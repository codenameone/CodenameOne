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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.FocusNode;
import com.codename1.flutter.State;
import com.codename1.flutter.Widget;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.TextEditingValue;
import com.codename1.flutter.material.Autocomplete;
import dart.core.DartList;
import dart.runtime.Funcs;

/// Flutter's {@code RawAutocomplete}: the options a text answers through
/// optionsBuilder, a highlight the arrow keys move (wrapping at the ends), Enter or
/// submitting the field selecting the highlighted option, and the selected option's
/// displayStringForOption filling the field. {@link Autocomplete} is this with the
/// Material field and options list, and both share the state that does the work.
public class RawAutocomplete<T> extends StatefulWidget {

    private Funcs.Func1<TextEditingValue, Iterable<T>> optionsBuilder;
    private Funcs.VoidFunc1<T> onSelected;
    private Funcs.Func1<T, String> displayStringForOption;

    public void optionsBuilder(Funcs.Func1<TextEditingValue, Iterable<T>> v) { this.optionsBuilder = v; }
    public void onSelected(Funcs.VoidFunc1<T> v) { this.onSelected = v; }
    public void displayStringForOption(Funcs.Func1<T, String> v) { this.displayStringForOption = v; }

    private Funcs.Func4<BuildContext, com.codename1.flutter.material.TextEditingController, FocusNode,
            Funcs.VoidFunc0, Widget> fieldViewBuilder;
    private Funcs.Func3<BuildContext, Funcs.VoidFunc1<T>, Iterable<T>, Widget> optionsViewBuilder;
    private com.codename1.flutter.material.TextEditingController textEditingController;
    private FocusNode focusNode;

    /// Flutter's {@code fieldViewBuilder(context, textEditingController, focusNode,
    /// onFieldSubmitted)}: the field the options answer. Its controller is the one the
    /// options follow, and calling onFieldSubmitted selects the highlighted option.
    public void fieldViewBuilder(Funcs.Func4<BuildContext, com.codename1.flutter.material.TextEditingController,
            FocusNode, Funcs.VoidFunc0, Widget> v) { this.fieldViewBuilder = v; }

    /// Flutter's {@code optionsViewBuilder(context, onSelected, options)}: the view of
    /// the options, shown under the field while there are any. Calling onSelected
    /// selects an option as a tap on the default list does.
    public void optionsViewBuilder(Funcs.Func3<BuildContext, Funcs.VoidFunc1<T>, Iterable<T>, Widget> v) {
        this.optionsViewBuilder = v;
    }

    public void textEditingController(com.codename1.flutter.material.TextEditingController v) {
        this.textEditingController = v;
    }

    public void focusNode(FocusNode v) { this.focusNode = v; }

    public Funcs.Func4<BuildContext, com.codename1.flutter.material.TextEditingController, FocusNode,
            Funcs.VoidFunc0, Widget> getFieldViewBuilder() { return fieldViewBuilder; }

    public Funcs.Func3<BuildContext, Funcs.VoidFunc1<T>, Iterable<T>, Widget> getOptionsViewBuilder() {
        return optionsViewBuilder;
    }

    public com.codename1.flutter.material.TextEditingController getTextEditingController() {
        return textEditingController;
    }

    public FocusNode getFocusNode() { return focusNode; }

    /// Flutter's {@code RawAutocomplete.defaultStringForOption}: the option's toString.
    public static String defaultStringForOption(Object option) {
        return String.valueOf(option);
    }

    /// The text that fills the field when {@code option} is selected.
    public String display(T option) {
        return displayStringForOption != null ? displayStringForOption.call(option)
                : defaultStringForOption(option);
    }

    /// Calls onSelected, if there is one.
    public void selected(T option) {
        if (onSelected != null) {
            onSelected.call(option);
        }
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

    /// The options' max height; Autocomplete's optionsMaxHeight.
    public double optionsMaxHeight() {
        return 200.0;
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new Autocomplete.AutocompleteState<T>();
    }
}
