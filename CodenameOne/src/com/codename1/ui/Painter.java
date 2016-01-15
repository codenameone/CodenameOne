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
 * Painter can be used to draw on components backgrounds. 
 * The use of such painter allows reuse of a background painters for various 
 * components. 
 * Note in order to view the painter drawing, component need to have some level
 * of transparency.  Usage:
 * 
 * <script src="https://gist.github.com/codenameone/31a32bdcf014a9e55a95.js"></script>
 * <noscript>Open the javadoc in your browser to see the full sample at https://www.codenameone.com/javadoc/</noscript>
 * Direct link to sample: <a href="https://gist.github.com/codenameone/31a32bdcf014a9e55a95" target="_blank">https://gist.github.com/codenameone/31a32bdcf014a9e55a95</a>.
 * 
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
