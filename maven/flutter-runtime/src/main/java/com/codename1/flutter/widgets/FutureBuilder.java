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
 * Builds itself from the latest snapshot of a {@code Future} — Flutter's
 * {@code FutureBuilder<T>}. This pass builds once with a waiting
 * {@link AsyncSnapshot} (the initial data, if any); resolving the future and
 * rebuilding on completion lands with the async-rebuild machinery.
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

    @Override
    public Widget build(BuildContext context) {
        if (builder == null) {
            return null;
        }
        AsyncSnapshot snapshot;
        if (initialData != null) {
            snapshot = new AsyncSnapshot(ConnectionState.waiting, initialData, null, null);
        } else {
            snapshot = new AsyncSnapshot();
        }
        return builder.call(context, snapshot);
    }
}
