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

/// Flutter's {@code AutocompleteHighlightedOption}: the inherited widget
/// {@link RawAutocomplete} puts above the options view, carrying the index of the
/// option the arrow keys have highlighted. A custom optionsViewBuilder reads it with
/// {@link #of} to draw the highlight and to scroll it into view; a reader rebuilds
/// whenever the highlight moves.
public class AutocompleteHighlightedOption extends InheritedWidget {

    private int highlightIndex;

    /// The highlighted option's index into the options the view was given.
    public void highlightIndex(int v) {
        this.highlightIndex = v;
    }

    public int getHighlightIndex() {
        return highlightIndex;
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return !(oldWidget instanceof AutocompleteHighlightedOption)
                || ((AutocompleteHighlightedOption) oldWidget).highlightIndex != highlightIndex;
    }

    /// Dart's {@code AutocompleteHighlightedOption.of(context)}: the highlighted index
    /// of the nearest enclosing autocomplete, or 0 when there is none, as in Flutter.
    public static int of(BuildContext context) {
        AutocompleteHighlightedOption h = context == null ? null
                : context.dependOnInheritedWidgetOfExactType(AutocompleteHighlightedOption.class);
        return h == null ? 0 : h.highlightIndex;
    }
}
