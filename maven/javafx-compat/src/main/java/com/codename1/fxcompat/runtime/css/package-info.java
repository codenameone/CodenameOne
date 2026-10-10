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
/// The part of the JavaFX style sheet support that the build and the device
/// share: the parsed form of a value, a selector and a declaration, the
/// parser that reads a value, and the compiled form of a whole style sheet
/// with its reader and its writer.
///
/// Nothing here names a scene graph class, so the build can run the same
/// code without the JavaFX layer on its class path: the style sheet
/// compiler takes a copy of these sources under a package of its own. What
/// the build writes is therefore exactly what the device reads, and an
/// inline `style` is parsed on the device by the parser that parsed the
/// style sheets at build time.
///
/// Everything here keeps to the class library a device has: no regular
/// expressions, no reflection, no locale sensitive case folding.
package com.codename1.fxcompat.runtime.css;
