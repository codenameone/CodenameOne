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

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for an editable text field. The controller and the CN1 text
 * component are kept in two-way sync by {@link TextFieldRenderElement}:
 * user edits flow into {@link #text()} (and notify listeners), while
 * {@link #setText(String)}/{@link #clear()} push into the mounted component.
 *
 * <p>Transpiler surface: the Dart constructor's named {@code text:} parameter
 * becomes the {@link #text(String)} setter, the Dart {@code text} getter
 * becomes {@link #text()}, and Dart {@code controller.text = v} assignments
 * are emitted as the explicit {@link #setText(String)} method.</p>
 */
public class TextEditingController {

    private String value = "";
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    /**
     * Every mounted field this controller drives. It was one reference, replaced by each
     * bind, so with a controller shared between fields a programmatic setText reached
     * only the last one, and when that one unmounted the controller forgot the others.
     */
    private final List<TextFieldRenderElement> bound = new ArrayList<TextFieldRenderElement>();

    /** The most recently bound field that is still mounted, or null. */
    private TextFieldRenderElement liveField() {
        for (int i = bound.size() - 1; i >= 0; i--) {
            if (bound.get(i).isMounted()) {
                return bound.get(i);
            }
        }
        return null;
    }

    public TextEditingController() {
    }

    /**
     * Named parameter setter for the Dart {@code text:} constructor parameter
     * (initial value, no listener notification).
     */
    public void text(String v) {
        this.value = v == null ? "" : v;
    }

    /**
     * The current text. When a mounted TextField is bound this reads the
     * component's live text.
     */
    public String text() {
        TextFieldRenderElement live = liveField();
        if (live != null) {
            String s = live.componentText();
            if (s != null) {
                value = s;
            }
        }
        return value;
    }

    /**
     * The value this controller holds, WITHOUT consulting the component.
     *
     * <p>{@link #text()} prefers the bound component's live text, which is
     * right once the field is showing the value and wrong before it ever
     * received one: reading it during the first apply overwrote the initial
     * text with the empty component's, so a controller built as
     * {@code TextEditingController(text: '25')} left the field blank and threw
     * the 25 away. The sliders demo's editable value is exactly that.</p>
     */
    String rawValue() {
        return value;
    }

    /**
     * Imperative setter (Dart {@code controller.text = v}): updates the bound
     * component when mounted and notifies listeners.
     */
    public void setText(String v) {
        this.value = v == null ? "" : v;
        pushToFields(null);
        notifyListeners();
    }

    public void clear() {
        setText("");
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Dart's {@code ChangeNotifier.dispose}: drops all listeners and unbinds
     * from any mounted component. Idempotent.
     */
    public void dispose() {
        listeners.clear();
        bound.clear();
    }

    /** Shows the current value in every mounted field except {@code source}. */
    private void pushToFields(TextFieldRenderElement source) {
        for (TextFieldRenderElement e : new ArrayList<TextFieldRenderElement>(bound)) {
            if (e != source && e.isMounted()) {
                e.applyControllerText(this.value);
            }
        }
    }

    // ------------------------------------------------------------------
    // Framework plumbing (package private)
    // ------------------------------------------------------------------

    void bind(TextFieldRenderElement e) {
        if (e != null && !bound.contains(e)) {
            bound.add(e);
        }
    }

    void unbind(TextFieldRenderElement e) {
        bound.remove(e);
    }

    /**
     * A user edit arrived from the component: absorb it (no push-back) and
     * notify listeners.
     */
    void valueFromComponent(String s) {
        valueFromComponent(s, null);
    }

    /** A user edit in {@code source}: absorb it and show it in the other bound fields. */
    void valueFromComponent(String s, TextFieldRenderElement source) {
        this.value = s == null ? "" : s;
        pushToFields(source);
        notifyListeners();
    }

    private void notifyListeners() {
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }
}
