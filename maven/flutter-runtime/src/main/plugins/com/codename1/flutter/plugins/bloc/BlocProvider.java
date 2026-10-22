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
package com.codename1.flutter.plugins.bloc;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;
import com.codename1.flutter.provider.ChangeNotifierProvider;

/// flutter_bloc's `BlocProvider<B>`: provides a bloc to its subtree. A bloc is a
/// ChangeNotifier here, so this is a ChangeNotifierProvider that also closes the
/// bloc it created when it leaves the tree -- never one handed in with `.value`.
public class BlocProvider<T> extends ChangeNotifierProvider {

    @Override
    public BlocProvider providedType(Class<?> v) {
        super.providedType(v);
        return this;
    }

    @Override
    protected void disposeCreated(BuildContext context, Object created) {
        super.disposeCreated(context, created);
        if (created instanceof BlocBase) {
            ((BlocBase<?>) created).close();
        }
    }

    public static BlocProvider value(Key key, Object value, Widget child) {
        BlocProvider p = new BlocProvider();
        p.key(key);
        p.value(value);
        p.child(child);
        return p;
    }

    /// `BlocProvider.of<B>(context, listen: false)`.
    @SuppressWarnings("unchecked")
    public static <T> T of(BuildContext context, boolean listen, Class<T> type) {
        return (T) context.providerValueOfType(type == null ? BlocBase.class : type, listen);
    }
}
