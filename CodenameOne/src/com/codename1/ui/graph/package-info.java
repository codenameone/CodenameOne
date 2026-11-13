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
/// `NodeGraph`: a pannable, zoomable view of a `com.codename1.graph.GraphModel`
/// where nodes are dragged, connected and grouped.
///
/// It serves two kinds of picture with one component. A diagram of a
/// system, where boxes stand for services or ideas and edges join them, is
/// made of nodes without ports; a node editor, where data flows from outputs
/// to inputs of a matching type, is made of nodes with `GraphPort`s. Both can
/// be laid out by hand or by a `com.codename1.graph.layout.GraphLayout`.
///
/// Nodes and edges are painted, with no component behind each, so a graph
/// of thousands of nodes costs what is on screen. A node that needs a real
/// control gets one from a `NodeComponentFactory`. Colours and fonts come
/// from the `NodeGraph*` UIIDs.
package com.codename1.ui.graph;
