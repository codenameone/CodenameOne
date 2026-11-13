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

import com.codename1.flutter.widgets.InheritedWidget;
import dart.runtime.Funcs;

/// Supplies {@code groupValue} and {@code onChanged} to the Radio (and
/// RadioListTile) widgets under it that do not set their own -- Flutter's
/// `RadioGroup` (3.32), which replaced the per-radio parameters.
public class RadioGroup<T> extends InheritedWidget {

    private T groupValue;
    private Funcs.VoidFunc1<T> onChanged;

    public void groupValue(T v) {
        this.groupValue = v;
    }

    public void onChanged(Funcs.VoidFunc1<T> v) {
        this.onChanged = v;
    }

    public T groupValue() {
        return groupValue;
    }

    public Funcs.VoidFunc1<T> onChanged() {
        return onChanged;
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return !(oldWidget instanceof RadioGroup)
                || !dart.runtime.DartRuntime.eq(((RadioGroup<?>) oldWidget).groupValue, groupValue)
                || ((RadioGroup<?>) oldWidget).onChanged != onChanged;
    }

    /// The nearest RadioGroup above {@code context}, or null.
    static RadioGroup<?> of(com.codename1.flutter.BuildContext context) {
        try {
            return context == null ? null : context.maybeDependOnInheritedWidgetOfExactType(RadioGroup.class);
        } catch (Throwable notMounted) {
            return null;
        }
    }
}
