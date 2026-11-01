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
import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.TextEditingValue;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.ConstrainedBox;
import com.codename1.flutter.widgets.ListView;
import com.codename1.flutter.widgets.Overlay;
import com.codename1.flutter.widgets.OverlayEntry;
import com.codename1.flutter.widgets.OverlayState;
import com.codename1.flutter.widgets.Positioned;
import com.codename1.flutter.widgets.Text;
import dart.core.DartList;
import dart.runtime.Funcs;

/// Flutter's {@code Autocomplete} (over {@code RawAutocomplete}'s behaviour): a text
/// field whose options, as optionsBuilder answers them for its text, float in an
/// {@link OverlayEntry} under the field, above the content, at most
/// optionsMaxHeight tall. As in RawAutocomplete, the first option is highlighted;
/// {@link AutocompleteState#highlightNext} / {@link AutocompleteState#highlightPrevious}
/// move the highlight (the arrow keys' intents) wrapping at the ends, submitting the
/// field selects the highlighted option, and a tap selects that option. Selecting
/// fills the field with displayStringForOption, calls onSelected and hides the
/// options until the text is edited again. With no Overlay above it (a bare tree in a
/// test) the options are laid out under the field instead.
public class Autocomplete<T> extends StatefulWidget {

    private Funcs.Func1<TextEditingValue, Iterable<T>> optionsBuilder;
    private Funcs.VoidFunc1<T> onSelected;
    private Funcs.Func1<T, String> displayStringForOption;
    private double optionsMaxHeight = 200.0;

    public void optionsBuilder(Funcs.Func1<TextEditingValue, Iterable<T>> v) { this.optionsBuilder = v; }
    public void onSelected(Funcs.VoidFunc1<T> v) { this.onSelected = v; }
    public void displayStringForOption(Funcs.Func1<T, String> v) { this.displayStringForOption = v; }
    public void optionsMaxHeight(double v) { this.optionsMaxHeight = v; }

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

    public static final class AutocompleteState<T> extends State<Autocomplete<T>> {
        private final TextEditingController controller = new TextEditingController();
        private boolean picked;
        private int highlighted;
        private DartList<T> options = DartList.of();
        private OverlayEntry entry;

        /// The options showing now: none once one was selected or for an empty field.
        public DartList<T> visibleOptions() {
            return options;
        }

        public int highlightedIndex() {
            return highlighted;
        }

        public TextEditingController textController() {
            return controller;
        }

        public void highlightNext() {
            if (!options.isEmpty()) {
                setState(() -> highlighted = (highlighted + 1) % options.size());
                refreshEntry();
            }
        }

        public void highlightPrevious() {
            if (!options.isEmpty()) {
                setState(() -> highlighted = (highlighted - 1 + options.size()) % options.size());
                refreshEntry();
            }
        }

        /// What submitting the field does: selects the highlighted option.
        public void submit() {
            if (!options.isEmpty()) {
                select(options.get(Math.min(highlighted, options.size() - 1)));
            }
        }

        public void textChanged(String text) {
            setState(() -> {
                picked = false;
                highlighted = 0;
            });
        }

        public void select(T o) {
            final Autocomplete<T> w = widget();
            setState(() -> {
                controller.text(w.display(o));
                picked = true;
            });
            if (w.onSelected != null) {
                w.onSelected.call(o);
            }
        }

        private void refreshEntry() {
            if (entry != null) {
                entry.markNeedsBuild();
            }
        }

        private void removeEntry() {
            if (entry != null) {
                OverlayEntry e = entry;
                entry = null;
                if (e.mounted()) {
                    e.remove();
                }
            }
        }

        @Override
        public void dispose() {
            removeEntry();
        }

        private static double absX(RenderElement r) {
            return r != null && r.component() != null ? r.component().getAbsoluteX() : 0;
        }

        private static double absY(RenderElement r) {
            return r != null && r.component() != null ? r.component().getAbsoluteY() : 0;
        }

        /// The options list, positioned under the field in the overlay's coordinates.
        Widget optionsView(BuildContext overlayContext) {
            final Autocomplete<T> w = widget();
            DartList<Widget> rows = DartList.of();
            for (int i = 0; i < options.size(); i++) {
                final T o = options.get(i);
                ListTile tile = new ListTile();
                tile.title(new Text(w.display(o)));
                tile.selected(i == highlighted);
                tile.onTap(() -> select(o));
                rows.add(tile);
            }
            ListView list = new ListView();
            list.shrinkWrap(true);
            list.children(rows);
            ConstrainedBox box = new ConstrainedBox();
            box.constraints(new BoxConstraints(0, Double.POSITIVE_INFINITY, 0, w.optionsMaxHeight));
            box.child(list);
            Material m = new Material();
            m.elevation(4.0);
            m.child(box);
            RenderElement field = context() instanceof Element
                    ? RenderElement.findRenderElement((Element) context()) : null;
            RenderElement host = null;
            if (overlayContext instanceof Element) {
                for (Element e = ((Element) overlayContext).parent(); e != null && host == null; e = e.parent()) {
                    if (e instanceof RenderElement) {
                        host = (RenderElement) e;
                    }
                }
            }
            Positioned p = new Positioned();
            p.left(absX(field) - absX(host));
            p.top(absY(field) - absY(host) + (field != null ? field.size().height() : 0));
            if (field != null && field.size().width() > 0) {
                p.width(field.size().width());
            }
            p.child(m);
            return p;
        }

        @Override
        public Widget build(BuildContext context) {
            final Autocomplete<T> w = widget();
            TextField field = new TextField();
            field.controller(controller);
            field.onChanged(this::textChanged);
            field.onSubmitted(s -> submit());
            options = !picked && controller.text().length() > 0 ? w.optionsFor(controller.text()) : DartList.<T>of();
            if (highlighted >= options.size()) {
                highlighted = 0;
            }
            OverlayState overlay = Overlay.maybeOf(context, false);
            if (overlay != null) {
                if (options.isEmpty()) {
                    removeEntry();
                } else if (entry == null) {
                    entry = new OverlayEntry();
                    entry.builder(this::optionsView);
                    final OverlayEntry e = entry;
                    com.codename1.ui.CN.callSerially(() -> {
                        if (entry == e && mounted()) {
                            overlay.insert(e, null, null);
                        }
                    });
                } else {
                    // The entry is another subtree: rebuilding it from inside this build
                    // would re-enter the overlay mid-frame.
                    com.codename1.ui.CN.callSerially(this::refreshEntry);
                }
                return field;
            }
            DartList<Widget> rows = DartList.of();
            rows.add(field);
            for (int i = 0; i < options.size(); i++) {
                final T o = options.get(i);
                ListTile tile = new ListTile();
                tile.title(new Text(w.display(o)));
                tile.selected(i == highlighted);
                tile.onTap(() -> select(o));
                rows.add(tile);
            }
            Column col = new Column();
            col.children(rows);
            return col;
        }
    }
}
