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
package com.codename1.flutter.semantics;

import com.codename1.flutter.rendering.Size;

import dart.core.DartList;

/**
 * The signature of a {@code CustomPainter}'s {@code semanticsBuilder} — Flutter's
 * {@code SemanticsBuilderCallback} typedef,
 * {@code List<CustomPainterSemantics> Function(Size size)}. Given the current
 * paint {@code size}, it returns the accessibility nodes the painter exposes.
 * Declared as a single-abstract-method interface so transpiled painters can
 * supply it with a lambda.
 */
public interface SemanticsBuilderCallback {
    DartList<CustomPainterSemantics> call(Size size);
}
