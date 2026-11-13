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
/// Graphs as data: nodes, the edges between them, the groups they belong to,
/// and the layouts that place them. What a
/// `com.codename1.ui.graph.NodeGraph` shows.
///
/// Nothing here draws, and everything runs without a component, on a server
/// too.
///
/// - `GraphModel` is the graph. `DefaultGraphModel` keeps one in memory and
///   accepts edits, each checked by a `ConnectionValidator`.
/// - A `GraphNode` is a box in world units. Give it `GraphPort`s for a node
///   editor, where edges join an output to an input of the same type; leave
///   it without for a diagram, where edges join nodes.
/// - A `GraphGroup` is a box drawn around its members, and can collapse.
/// - `com.codename1.graph.layout` places the nodes automatically.
///
/// The classes are not thread safe: use a model from one thread, which for a
/// model shown by a component is the EDT.
package com.codename1.graph;
