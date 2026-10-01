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
package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

/**
 * provider's {@code Provider<T>}: publishes {@code value} to its subtree by
 * runtime type. {@link #of(BuildContext, boolean, Class)} walks the element tree
 * for the nearest provider whose value is assignable to the requested type — the
 * Dart {@code Provider.of<T>(context)} witness is threaded in as {@code type} by
 * the transpiler.
 *
 * <p>The {@code create} form (a lazily-invoked factory) is accepted for API
 * shape but not exercised by the gallery, which uses the {@code .value} form.</p>
 */
public class Provider extends SingleChildWidget implements InheritedValueProvider {

    protected Object value;
    protected Object create;
    protected boolean lazy = true;
    /** The Dart {@code T} of {@code Provider<T>}, when the transpiler knows it; else null. */
    protected Class<?> providedType;

    public void value(Object v) {
        this.value = v;
    }

    public void create(Object v) {
        this.create = v;
    }

    public void lazy(boolean v) {
        this.lazy = v;
    }

    public Object getValue() {
        return value;
    }

    /**
     * The provider's declared type ({@code Provider<T>}), threaded in by the transpiler as
     * Java erases it. With it the provider matches a lookup for T even while its value is
     * null; without it only a non-null value can say what it provides. Answers this
     * provider, so it chains onto a {@code .value(...)} named constructor.
     */
    public Provider providedType(Class<?> v) {
        this.providedType = v;
        return this;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new ProviderElement(this);
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        return providesType(type) ? value : null;
    }

    @Override
    public boolean providesType(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (value != null) {
            return type.isInstance(value);
        }
        // A null value matches by the declared type. Not for an Object lookup -- the
        // default of a Consumer whose type was not known -- which means "the nearest
        // model", and a null is no model: it would shadow the real one further out.
        return providedType != null && type != Object.class && type.isAssignableFrom(providedType);
    }

    /** The {@code Provider.value(value: ...)} named constructor. */
    public static Provider value(Key key, Object value, Widget child) {
        Provider p = new Provider();
        p.key(key);
        p.value(value);
        p.child(child);
        return p;
    }

    /** {@code Provider.of<T>(context, listen: ...)}. */
    @SuppressWarnings("unchecked")
    public static <T> T of(BuildContext context, boolean listen, Class<T> type) {
        return (T) context.providerValueOfType(type, listen);
    }
}
