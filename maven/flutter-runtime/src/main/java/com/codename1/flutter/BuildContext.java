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
package com.codename1.flutter;

/**
 * A handle to the location of a widget in the element tree. Implemented by
 * {@link Element}. Passed to build methods so widgets can look up inherited
 * configuration (e.g. {@code Theme.of(context)}).
 */
public interface BuildContext {

    /**
     * Walks up the element tree and returns the nearest ancestor widget whose
     * runtime class is exactly {@code widgetType}, or null when there is none.
     */
    <W extends Widget> W findAncestorWidgetOfExactType(Class<W> widgetType);

    /**
     * Walks up the element tree and returns the nearest ancestor widget that is
     * an instance of {@code type} (Flutter's InheritedWidget dependency lookup),
     * or null when there is none. The {@code <T>} type witness the Dart call
     * carries is threaded here as {@code type} by the transpiler.
     */
    <W extends Widget> W dependOnInheritedWidgetOfExactType(Class<W> type);

    /**
     * As {@link #dependOnInheritedWidgetOfExactType(Class)}, but SILENT when
     * nothing above provides the value. For the lookups whose callers have a
     * documented fallback ({@code Theme.of}, {@code MediaQuery.of}, ...), where
     * a miss is an ordinary answer rather than a fault worth a diagnostic.
     */
    default <W extends Widget> W maybeDependOnInheritedWidgetOfExactType(Class<W> type) {
        return dependOnInheritedWidgetOfExactType(type);
    }

    /**
     * The no-type-argument form ({@code context.dependOnInheritedWidgetOfExactType()}), where Dart
     * infers the widget type from the surrounding context. Java infers {@code W} from the call's
     * target type. Not tree-walked at this milestone — returns null.
     */
    default <W extends Widget> W dependOnInheritedWidgetOfExactType() {
        return null;
    }

    /**
     * Walks up the element tree and returns the nearest ancestor {@code State}
     * of the given type ({@code BuildContext.findAncestorStateOfType}), or null.
     * Not tree-walked at this milestone — returns null.
     */
    default <T> T findAncestorStateOfType(Class<T> type) {
        return null;
    }

    /**
     * provider's {@code context.watch<T>()}: the nearest ancestor-provided value
     * assignable to {@code type} (rebuild-on-change is not modeled in this pass).
     */
    <T> T watch(Class<T> type);

    /**
     * provider's {@code context.read<T>()}: the nearest ancestor-provided value
     * assignable to {@code type}, without subscribing to changes.
     */
    <T> T read(Class<T> type);

    /**
     * The nearest value published by an ancestor {@link InheritedValueProvider}
     * (Provider / ScopedModel) that is assignable to {@code type}, or null.
     */
    Object providerValueOfType(Class<?> type);

    /**
     * {@link #providerValueOfType(Class)}, registering this context as a dependent --
     * rebuilt when the value changes -- only when {@code listen} is true. A read
     * ({@code context.read}, {@code Provider.of(context, listen: false)}) must not
     * subscribe, or the reader rebuilds on every notification that only watchers should
     * get.
     */
    default Object providerValueOfType(Class<?> type, boolean listen) {
        return providerValueOfType(type);
    }

    /**
     * Whether the element backing this context is still in the tree
     * ({@code BuildContext.mounted}). Elements override this; the default is
     * {@code true} for lightweight contexts that never detach.
     */
    default boolean mounted() {
        return true;
    }

    /**
     * The render object for this context — {@code BuildContext.findRenderObject}.
     *
     * <p>Returns a {@link com.codename1.flutter.rendering.RenderBox} backed by
     * the nearest render element at or below this context, so its {@code size}
     * and {@code localToGlobal} report the live layout. Null when this context
     * has no render element below it (nothing has been laid out yet).</p>
     */
    default Object findRenderObject() {
        if (!(this instanceof Element)) {
            return null;
        }
        RenderElement r = RenderElement.findRenderElement((Element) this);
        return r == null ? null : new com.codename1.flutter.rendering.RenderBox(r);
    }
}
