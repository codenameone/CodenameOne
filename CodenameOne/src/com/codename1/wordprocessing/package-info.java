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

/// Word-processing documents.
///
/// A `Document` is an immutable list of `Block`s -- `Paragraph`s of styled `RichText`,
/// `Table`s whose cells hold blocks of their own, and `Picture`s -- laid out on a `PageSpec`.
/// A `DocumentModel` holds the current document of an editor, tells its `DocumentListener`s
/// when it is replaced and keeps the undo history, the way a `com.codename1.ui.table.TableModel`
/// backs a table.
///
/// Documents are read and written as Codename One `.cn1doc` files, plain text, HTML and
/// Markdown; see `Document.Format`.
package com.codename1.wordprocessing;
