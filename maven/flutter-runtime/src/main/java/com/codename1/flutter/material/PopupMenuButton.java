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
import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Shows a menu of {@link PopupMenuItem}s when pressed — Flutter's
 * {@code PopupMenuButton}. This milestone renders the trigger ({@code child}
 * or {@code icon}); building/presenting the menu and firing {@code onSelected}
 * is deferred.
 *
 * <p>Non-generic on the Java side (the Dart stub is generic): {@code onSelected}
 * is a generic method so an explicitly-typed selection callback
 * ({@code (String v) => ...}) infers its parameter type at the call site,
 * which a raw generic class would erase away.</p>
 */
public class PopupMenuButton<T> extends Widget {

    private Funcs.Func1<BuildContext, Object> itemBuilder;
    private Object initialValue;
    private Funcs.VoidFunc1<T> onSelected;
    private Funcs.VoidFunc0 onCanceled;
    private String tooltip;
    private double elevation = 8;
    private Object padding;
    private Widget icon;
    private double iconSize;
    private Object offset;
    private boolean enabled = true;
    private Object shape;
    private Color color;
    private Object position;
    private Widget child;

    public void itemBuilder(Funcs.Func1<BuildContext, Object> v) {
        this.itemBuilder = v;
    }

    public void initialValue(Object v) {
        this.initialValue = v;
    }

    public void onSelected(Funcs.VoidFunc1<T> v) {
        this.onSelected = v;
    }

    public void onCanceled(Funcs.VoidFunc0 v) {
        this.onCanceled = v;
    }

    public void tooltip(String v) {
        this.tooltip = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void padding(Object v) {
        this.padding = v;
    }

    public void icon(Widget v) {
        this.icon = v;
    }

    public void iconSize(double v) {
        this.iconSize = v;
    }

    public void offset(Object v) {
        this.offset = v;
    }

    public void enabled(boolean v) {
        this.enabled = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void position(Object v) {
        this.position = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    public Widget getIcon() {
        return icon;
    }

    public Funcs.Func1<BuildContext, Object> getItemBuilder() {
        return itemBuilder;
    }

    public Funcs.VoidFunc1<T> getOnSelected() {
        return onSelected;
    }

    public Funcs.VoidFunc0 getOnCanceled() {
        return onCanceled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * The widget the button shows: the explicit {@code child} or {@code icon}, else the
     * overflow glyph Flutter falls back to. Without that default a menu button written the
     * usual way — neither child nor icon, as the gallery's app bar demo writes it — laid out
     * as a zero-sized nothing, so the menu was not merely inert but invisible.
     */
    public Widget effectiveTrigger() {
        return effectiveTrigger(null);
    }

    /** The same, resolving the adaptive icon against {@code context}'s theme. */
    public Widget effectiveTrigger(com.codename1.flutter.BuildContext context) {
        if (child != null) {
            return child;
        }
        if (icon != null) {
            return icon;
        }
        // Flutter's default here is Icons.adaptive.more, which is HORIZONTAL
        // on iOS and macOS and vertical everywhere else. Hard-coding more_vert
        // put upright dots on an iOS screen where the reference shows an
        // ellipsis, on the app bar and on every popup menu in the app.
        Object p = null;
        try {
            com.codename1.flutter.material.ThemeData t =
                    context == null ? null : Theme.of(context);
            p = t == null ? null : t.platform();
        } catch (Throwable noTheme) {
            // an unthemed menu still needs an icon
        }
        boolean apple = p == com.codename1.flutter.TargetPlatform.iOS
                || p == com.codename1.flutter.TargetPlatform.macOS;
        return new com.codename1.flutter.widgets.Icon(apple
                ? com.codename1.flutter.Icons.more_horiz
                : com.codename1.flutter.Icons.more_vert);
    }

    @Override
    public Element createElement() {
        return new PopupMenuButtonRenderElement(this);
    }
}
