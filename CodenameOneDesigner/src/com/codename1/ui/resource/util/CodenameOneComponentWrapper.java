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

package com.codename1.ui.resource.util;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JLabel;

/**
 * Wraps a CodenameOne component in a Swing component for preview purposes, this is
 * effectively the "live preview" API
 *
 * @author Shai Almog
 */
public class CodenameOneComponentWrapper extends JLabel {
    private com.codename1.ui.Component codenameOneCmp;
    public CodenameOneComponentWrapper() {
        this(new Button("Preview"));
    }
    public CodenameOneComponentWrapper(com.codename1.ui.Component codenameOneCmp) {
        this(codenameOneCmp, false);
    }
    public CodenameOneComponentWrapper(com.codename1.ui.Component codenameOneCmp, boolean forceShow) {
        this.codenameOneCmp = codenameOneCmp;
        if(codenameOneCmp.getParent() == null) {
            if(!(codenameOneCmp instanceof Form)) {
                Form dummy = new Form("");
                dummy.setLayout(new com.codename1.ui.layouts.BorderLayout());
                dummy.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, codenameOneCmp);
                dummy.setWidth(1000);
                dummy.setHeight(1000);
                dummy.layoutContainer();
                if(forceShow || com.codename1.ui.Display.getInstance().getCurrent() == null) {
                    dummy.show();
                }
            } else {
                ((Form)codenameOneCmp).layoutContainer();
                if(com.codename1.ui.Display.getInstance().getCurrent() == null) {
                    if(codenameOneCmp instanceof com.codename1.ui.Dialog) {
                        ((com.codename1.ui.Dialog)codenameOneCmp).showModeless();
                    } else {
                        ((Form)codenameOneCmp).show();
                    }
                }
            }
        }
    }
    
    @Override
    public void setText(String s) {
        if(codenameOneCmp != null && codenameOneCmp instanceof Label) {
            ((Label)codenameOneCmp).setText(s);
        }
    }
    
    @Override
    public String getText() {
        if(codenameOneCmp != null && codenameOneCmp instanceof Label) {
            return ((Label)codenameOneCmp).getText();
        }
        return "";
    }
    
    public Component getCodenameOneComponent() {
        return codenameOneCmp;
    }

    public void setCodenameOneComponent(com.codename1.ui.Component l) {
        codenameOneCmp = l;
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(codenameOneCmp.getPreferredW(), codenameOneCmp.getPreferredH());
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private com.codename1.ui.Image buffer;

    @Override
    public void paint(Graphics g) {
        if(isEnabled()) {
            try {
                if(buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
                    buffer = com.codename1.ui.Image.createImage(getWidth(), getHeight(), 0);
                }
                com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                    public void run() {
                        codenameOneCmp.setX(0);
                        codenameOneCmp.setY(0);
                        codenameOneCmp.setWidth(getWidth());
                        codenameOneCmp.setHeight(getHeight());
                        com.codename1.ui.Form parentForm = codenameOneCmp.getComponentForm();
                        if(parentForm.getWidth() == 0 || parentForm != codenameOneCmp) {
                            parentForm.setWidth(getWidth());
                            parentForm.setHeight(getHeight());
                            parentForm.revalidate();
                        }
                        if(codenameOneCmp instanceof com.codename1.ui.Container) {
                            ((com.codename1.ui.Container)codenameOneCmp).revalidate();
                        }
                        com.codename1.ui.Graphics gl = buffer.getGraphics();
                        gl.setColor(0xcccccc);
                        gl.fillRect(0, 0, getWidth(), getHeight());
                        gl.setClip(0, 0, buffer.getWidth(), buffer.getHeight());
                        codenameOneCmp.setVisible(true);
                        codenameOneCmp.paintComponent(gl);
                    }
                }, 300);
                g.drawImage((java.awt.Image)buffer.getImage(), 0, 0, this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            super.paintChildren(g);
        }
    }
}
