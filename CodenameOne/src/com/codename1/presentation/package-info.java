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

/// Presentations: slide decks read, shown, edited and written.
///
/// A `Presentation` is an immutable list of `Slide`s, each a list of `SlideElement`s --
/// `ShapeElement`s with their `TextFrame`s, `PictureElement`s, `TableElement`s,
/// `ChartElement`s, `DiagramElement`s and `GroupElement`s -- with a `Transition` into the
/// slide and the `Build`s that animate its elements, over a `Theme`.
///
/// `SlideShow` plays a deck, in presenter mode too, and `SlideView` edits one through a
/// `PresentationModel`, which keeps the undo history and tells its `PresentationListener`s
/// about every change. `SlideRenderer` paints a slide into any `Graphics`.
///
/// Decks are read from PowerPoint (`.pptx` and the older `.ppt`), OpenDocument (`.odp`),
/// Keynote (`.key`) and Codename One's own `.cn1slides` files, and written as `.pptx`,
/// `.odp`, `.cn1slides` and PDF; see `Presentation.Format`.
package com.codename1.presentation;
