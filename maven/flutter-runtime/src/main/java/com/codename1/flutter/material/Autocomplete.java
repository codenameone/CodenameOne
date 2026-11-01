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

/// Flutter's {@code Autocomplete} (a {@link com.codename1.flutter.widgets.RawAutocomplete}
/// with the Material field and options): a text
/// field whose options, as optionsBuilder answers them for its text, float in an
/// {@link OverlayEntry} under the field, above the content, at most
/// optionsMaxHeight tall. As in RawAutocomplete, the first option is highlighted;
/// {@link AutocompleteState#highlightNext} / {@link AutocompleteState#highlightPrevious}
/// move the highlight wrapping at the ends -- the arrow keys do it, through Codename One
/// game key listeners on the form while options show -- submitting the
/// field selects the highlighted option, and a tap selects that option. Selecting
/// fills the field with displayStringForOption, calls onSelected and hides the
/// options until the text is edited again. With no Overlay above it (a bare tree in a
/// test) the options are laid out under the field instead.
public class Autocomplete<T> extends com.codename1.flutter.widgets.RawAutocomplete<T> {

    private double optionsMaxHeight = 200.0;

    public void optionsMaxHeight(double v) { this.optionsMaxHeight = v; }

    @Override
    public double optionsMaxHeight() {
        return optionsMaxHeight;
    }

    public static final class AutocompleteState<T> extends State<com.codename1.flutter.widgets.RawAutocomplete<T>> {
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
            final com.codename1.flutter.widgets.RawAutocomplete<T> w = widget();
            setState(() -> {
                controller.text(w.display(o));
                picked = true;
            });
            w.selected(o);
        }

        /// The form whose arrow and fire keys move and pick the highlight while options
        /// show: RawAutocomplete's arrow-up/down and enter intents.
        private com.codename1.ui.Form keysOn;
        private final com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent> upKey =
                evt -> { if (!options.isEmpty()) { evt.consume(); highlightPrevious(); } };
        private final com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent> downKey =
                evt -> { if (!options.isEmpty()) { evt.consume(); highlightNext(); } };
        private final com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent> fireKey =
                evt -> { if (!options.isEmpty()) { evt.consume(); submit(); } };

        void listenToKeys(boolean on) {
            com.codename1.ui.Form f = on ? com.codename1.ui.CN.getCurrentForm() : null;
            if (f == keysOn) {
                return;
            }
            if (keysOn != null) {
                keysOn.removeGameKeyListener(com.codename1.ui.Display.GAME_UP, upKey);
                keysOn.removeGameKeyListener(com.codename1.ui.Display.GAME_DOWN, downKey);
                keysOn.removeGameKeyListener(com.codename1.ui.Display.GAME_FIRE, fireKey);
            }
            keysOn = f;
            if (f != null) {
                f.addGameKeyListener(com.codename1.ui.Display.GAME_UP, upKey);
                f.addGameKeyListener(com.codename1.ui.Display.GAME_DOWN, downKey);
                f.addGameKeyListener(com.codename1.ui.Display.GAME_FIRE, fireKey);
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
            listenToKeys(false);
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
            final com.codename1.flutter.widgets.RawAutocomplete<T> w = widget();
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
            box.constraints(new BoxConstraints(0, Double.POSITIVE_INFINITY, 0, w.optionsMaxHeight()));
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
            final com.codename1.flutter.widgets.RawAutocomplete<T> w = widget();
            TextField field = new TextField();
            field.controller(controller);
            field.onChanged(this::textChanged);
            field.onSubmitted(s -> submit());
            options = !picked && controller.text().length() > 0 ? w.optionsFor(controller.text()) : DartList.<T>of();
            if (highlighted >= options.size()) {
                highlighted = 0;
            }
            listenToKeys(!options.isEmpty());
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
