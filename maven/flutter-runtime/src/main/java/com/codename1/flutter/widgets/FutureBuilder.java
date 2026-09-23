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
 * Builds itself from the latest snapshot of a {@code Future} -- Flutter's
 * {@code FutureBuilder<T>}. The snapshot and the subscription live in
 * {@link FutureBuilderElement}, as a State would hold them in Flutter.
 *
 * @param <T> the future's value type
 */
public class FutureBuilder<T> extends StatelessWidget {

    private Object future;
    private T initialData;
    private Funcs.Func2<BuildContext, AsyncSnapshot, Widget> builder;

    public void future(Object v) {
        this.future = v;
    }

    public void initialData(T v) {
        this.initialData = v;
    }

    public void builder(Funcs.Func2<BuildContext, AsyncSnapshot, Widget> v) {
        this.builder = v;
    }

    Object getFuture() {
        return future;
    }

    T getInitialData() {
        return initialData;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new FutureBuilderElement(this);
    }

    @Override
    public Widget build(BuildContext context) {
        return buildWith(context, initialData != null
                ? new AsyncSnapshot(ConnectionState.none, initialData, null, null) : new AsyncSnapshot());
    }

    Widget buildWith(BuildContext context, AsyncSnapshot snapshot) {
        return builder == null ? null : builder.call(context, snapshot);
    }
}
