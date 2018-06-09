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

import com.codename1.properties.Property;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Border;

/**
 * A scene graph.  Supports 3D on platforms where {@link com.codename1.ui.Transform#isPerspectiveSupported() } is true (iOS and Android currently).
 * @author Steve Hannah
 * @deprecated For internal use only
 */
public class Scene extends Container {
    /**
     * The root node.
     */
    private Node root;
    
    public final Property<Camera,Scene> camera;
    
    public Scene() {
        setUIID("Scene");
        camera = new Property<Camera,Scene>("camera", (Camera)null);
    }
    
    
    /**
     * Set the root node.
     * @param root The root node.
     */
    public void setRoot(Node root) {
        if (this.root != null) {
            this.root.setScene(null);
        }
        this.root = root;
        if (this.root != null) {
            root.setScene(this);
        }
        
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (root != null) {
            g.resetAffine();
            int clipX = g.getClipX();
            int clipY = g.getClipY();
            int clipW = g.getClipWidth();
            int clipH = g.getClipHeight();
            g.translate(getX(), getY());
            g.setAntiAliased(true);
            root.render(g);
            g.translate(-getX(), -getY());
            g.resetAffine();
            g.setClip(clipX, clipY, clipW, clipH);
        }
    }

    @Override
    public void layoutContainer() {
        root.setNeedsLayout(true);
        super.layoutContainer(); 
    }
    
    
    
}
