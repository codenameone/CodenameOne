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

import com.codename1.flutter.Color;

import dart.core.DartSet;
import dart.runtime.Funcs;

/**
 * The Material-3 rename of {@link MaterialStateProperty} (identical shape).
 * Newer Flutter aliases {@code MaterialStateProperty} to
 * {@code WidgetStateProperty}; both spellings resolve.
 */
public class WidgetStateProperty {

    private final Funcs.Func1<DartSet<MaterialState>, Color> resolver;
    private final Object constant;
    private final boolean isConstant;

    private WidgetStateProperty(Funcs.Func1<DartSet<MaterialState>, Color> resolver,
                                Object constant, boolean isConstant) {
        this.resolver = resolver;
        this.constant = constant;
        this.isConstant = isConstant;
    }

    /** A property that is {@code value} in every state. */
    public static WidgetStateProperty all(Object value) {
        return new WidgetStateProperty(null, value, true);
    }

    /** A property computed from the active state set on each read. */
    public static WidgetStateProperty resolveWith(Funcs.Func1<DartSet<MaterialState>, Color> resolver) {
        return new WidgetStateProperty(resolver, null, false);
    }

    /** Evaluates this property for {@code states}. */
    public Object resolve(DartSet<MaterialState> states) {
        if (isConstant) {
            return constant;
        }
        return resolver == null ? null : resolver.call(states);
    }
}
