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

import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

/**
 * provider's {@code ChangeNotifierProvider<T extends ChangeNotifier>}: a
 * {@link Provider} specialised for {@code ChangeNotifier} values. The gallery
 * uses the {@code .value} form inside a {@code MultiProvider}; disposal of a
 * created notifier is not modeled in this pass.
 */
public class ChangeNotifierProvider extends Provider {

    /**
     * Subscribes to the model and rebuilds this subtree when it notifies.
     *
     * <p>Without this the provider read its value once and nothing ever listened, so
     * {@code notifyListeners()} changed nothing on screen and every control whose job is
     * to set a field on the model did nothing at all.</p>
     */
    @Override
    public com.codename1.flutter.Element createElement() {
        return new ChangeNotifierProviderElement(this);
    }

    /** The {@code ChangeNotifierProvider.value(value: ...)} named constructor. */
    public static ChangeNotifierProvider value(Key key, Object value, Widget child) {
        ChangeNotifierProvider p = new ChangeNotifierProvider();
        p.key(key);
        p.value(value);
        p.child(child);
        return p;
    }
}
