/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.JavaSEPort.CN1JPanel;
import com.codename1.ui.Component;
import com.codename1.ui.Container;

import com.codename1.xml.Element;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;
import org.cef.browser.ComponentDelegate;
import org.cef.browser.ComponentFactory;

/**
 *
 * @author shannah
 */
public class CEFComponentFactory implements ComponentFactory {

    @Override
    public JPanel createComponent(final ComponentDelegate delegate) {
        return new CN1JPanel() {
            @Override
            public void setBounds(int x, int y, int w, int h) {
                super.setBounds(x, y, w, h);
                delegate.boundsChanged(x, y, w, h);
                /*
                browser_rect_.setBounds(x, y, w, h);
                screenPoint_ = component_.getLocationOnScreen();
                wasResized(w, h);
                */
            }
            
            @Override
            public void setBounds(Rectangle r) {
                setBounds(r.x, r.y, r.width, r.height);
            }

            @Override
            public void setSize(int width, int height) {
                super.setSize(width, height);
                delegate.wasResized(width, height);
            }

            @Override
            public void setSize(Dimension d) {
                setSize(d.width, d.height);
            }
            
            public void paint(Graphics g) {
                delegate.createBrowserIfRequired(false);
                /*
                super.paint(g);
                g.setColor(Color.red);
                g.fillRect(100, 100, 100, 100);
                
                BufferedImage bimg = buffer_.getBufferedImage();
                if (bimg != null) {
                    ((Graphics2D)g).drawImage(bufferedImage_, 0, 0, this);
                }
                */
                
            }
            
        };
    }

   
    
}
