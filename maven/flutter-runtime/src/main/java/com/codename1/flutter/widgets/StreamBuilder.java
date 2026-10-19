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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Flutter's {@code StreamBuilder<T>}: rebuilds from the latest event of a Stream --
 * {@code waiting} until the first, {@code active} after each, {@code done} once the
 * stream closes. The usual home of {@code FirebaseAuth.authStateChanges()}.
 */
public class StreamBuilder<T> extends StatelessWidget {
    private Object stream;
    private T initialData;
    private Funcs.Func2<BuildContext, AsyncSnapshot<T>, Widget> builder;

    public void stream(Object v) {
        this.stream = v;
    }

    public void initialData(T v) {
        this.initialData = v;
    }

    public void builder(Funcs.Func2<BuildContext, AsyncSnapshot<T>, Widget> v) {
        this.builder = v;
    }

    Object getStream() {
        return stream;
    }

    T getInitialData() {
        return initialData;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new StreamBuilderElement(this);
    }

    @Override
    public Widget build(BuildContext context) {
        return buildWith(context, new AsyncSnapshot(ConnectionState.none, initialData, null, null));
    }

    @SuppressWarnings("unchecked")
    Widget buildWith(BuildContext context, AsyncSnapshot snapshot) {
        return builder == null ? null : builder.call(context, snapshot);
    }
}
