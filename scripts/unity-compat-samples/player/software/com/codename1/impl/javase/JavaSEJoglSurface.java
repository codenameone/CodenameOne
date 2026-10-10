/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.impl.javase;

/// Not the JavaSE port's OpenGL surface: a stand-in that
/// `run-unity-project.sh --software` puts in front of it on the class path.
///
/// The port looks its OpenGL surface up by this name and, when it cannot
/// make one, draws with its software rasteriser. It offers no switch for
/// that, and this class is the switch: it has no constructor taking a
/// render view, so the lookup fails and the port falls back. It is for a
/// machine on which the OpenGL binding does not start, or draws nothing.
final class JavaSEJoglSurface {
    private JavaSEJoglSurface() {
    }
}
