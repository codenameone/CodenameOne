/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui.scene;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;

/**
 * Interface for painting the content of a node in the Scene graph.
 * @author Steve Hannah
 * @deprecated For internal use only
 */
public interface NodePainter {
    
    /**
     * Paints node content in the given bounding rect on the provided Graphics context for the given node.
     * @param g The graphics context to paint onto.
     * @param bounds The bounding rect to paint into.
     * @param node The node whose content we are painting.
     */
    public void paint(Graphics g, Rectangle bounds, Node node);
}
