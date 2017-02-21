/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.geom.*;

/**
 * <p>{@code Painter} can be used to draw on components backgrounds. 
 * The use of this interface allows reuse of a background {@code Painter} for various 
 * components. <br>
 * A simple example of a background painter is shown here to draw a circle background:
 * 
 * <script src="https://gist.github.com/codenameone/31a32bdcf014a9e55a95.js"></script>
 * 
 * <p>
 * Painters can also be used to draw the glasspane which is layered on top of the form. 
 * The example shows a glasspane running on top of a field to show a validation hint,
 * notice that for real world usage you should probably look into {@link com.codename1.ui.validation.Validator}.
 * Also notice that this example uses the shorter Java 8 lambda syntax to represent the glasspane.
 * </p>
 * <script src="https://gist.github.com/codenameone/f5b83373088600b19610.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-glasspane.png" alt="Sample of glasspane" />
 * @author Chen Fishbein
 */
public interface Painter {

    /**
     * Draws inside the given rectangle clipping area.
     * 
     * @param g the {@link Graphics} object
     * @param rect the given rectangle cliping area
     */
    public void paint(Graphics g, Rectangle rect);
}
