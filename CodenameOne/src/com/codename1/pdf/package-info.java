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
/// Creating and reading PDF documents, on the device and on the server.
///
/// Generation runs without a display: the same code builds an invoice in
/// an app or in a backend request handler. [PdfDocument] holds pages, each
/// drawn through a [PdfCanvas] (paths, text, images, links), and
/// [PdfLayout] flows [PdfParagraph] and [PdfTable] content across pages
/// with automatic page breaks. Fonts are the standard fourteen PDF fonts or
/// a TrueType font, embedded as a subset of the glyphs used.
///
/// [PdfReader] opens an existing document, encrypted ones included, for
/// its page count, page sizes, metadata and text.
///
/// Displaying a document is a client feature, in `com.codename1.pdf.ui`.
package com.codename1.pdf;
