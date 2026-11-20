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
package com.codename1.graph;

/// The names of the outlines a view knows how to draw, for
/// `GraphNode.setShape`. They are the symbols of a flow chart.
///
/// A name is only a key: the model stays free of drawing code, and
/// `com.codename1.ui.graph.GraphShapeRegistry` maps each to its outline and
/// takes outlines of your own under new names.
public final class GraphShapes {
    /// A rectangle with square corners: a process step.
    public static final String RECT = "rect";

    /// A rectangle with rounded corners.
    public static final String ROUNDED = "rounded";

    /// A pill, rounded by half its height: where a flow starts or ends.
    public static final String TERMINATOR = "terminator";

    /// A diamond: a decision.
    public static final String DECISION = "decision";

    /// A parallelogram: input or output.
    public static final String DATA = "data";

    /// A rectangle with a wavy bottom edge: a document.
    public static final String DOCUMENT = "document";

    /// A hexagon: preparation.
    public static final String PREPARATION = "preparation";

    /// A cylinder: a database or stored data.
    public static final String DATABASE = "database";

    /// A rectangle with a bar inside each end: a predefined process.
    public static final String SUBPROCESS = "subprocess";

    /// An ellipse.
    public static final String ELLIPSE = "ellipse";

    /// A sheet with a folded corner: a comment.
    public static final String NOTE = "note";

    private GraphShapes() {
    }
}
