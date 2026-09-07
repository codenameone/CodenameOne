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

package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Form;

/// One picture in the developer guide, expressed as the code that produces it.
///
/// A figure is a pure factory: it builds a [Form] and returns it, and never
/// calls `show()`. The renderer owns the window size, the theme and the
/// appearance, so one figure renders on several devices and in both
/// appearances without knowing it is being rendered at all.
public interface GuideFigure {
    /// Stable identity used to name the output file. Lower case with dashes,
    /// matching the image naming already used under `docs/developer-guide/img`.
    String id();

    /// Builds the form to photograph. Called on the Codename One EDT with the
    /// theme and appearance already installed.
    Form build();
}
