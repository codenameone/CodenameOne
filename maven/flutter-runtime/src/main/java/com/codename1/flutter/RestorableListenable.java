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
 * A restorable whose value is a {@code Listenable} that is restored rather than
 * re-created, mirroring Flutter's {@code RestorableListenable<T>}. User code
 * (studies/reply/app.dart, studies/shrine/app.dart) subclasses this directly,
 * overriding {@link #createDefaultValue()} / {@link #fromPrimitives(Object)} /
 * {@link #toPrimitives()}, and reads the inherited {@link #value()} getter.
 *
 * <p>The value is created lazily from {@link #createDefaultValue()} on first
 * access (Codename One does not persist restoration data).</p>
 *
 * @param <T> the held (listenable) value type
 */
public class RestorableListenable<T> extends RestorableProperty<T> {

    private T current;
    private boolean initialized;

    /** The restored value, created lazily from {@link #createDefaultValue()}. */
    public T value() {
        if (!initialized) {
            current = createDefaultValue();
            initialized = true;
        }
        return current;
    }

    @Override
    public void initWithValue(T value) {
        this.current = value;
        this.initialized = true;
    }
}
