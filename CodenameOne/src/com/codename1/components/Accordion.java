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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

/**
 * <p>The {@code Accordion} ui pattern is a vertically stacked list of items. 
 * Each item can be opened/closed to reveal more content similarly to a {@link com.codename1.ui.tree.Tree}
 * however unlike the {@link com.codename1.ui.tree.Tree} the {@code Accordion} is designed to include
 * containers or arbitrary components rather than model based data.</p>
 * <p>
 * This makes the {@code Accordion} more convenient as a tool for folding/collapsing UI elements known in advance
 * whereas a {@link com.codename1.ui.tree.Tree} makes more sense as a tool to map data e.g. filesystem 
 * structure, XML hierarchy etc.
 * </p>
 * <p>
 * Note that the {@code Accordion} like many composite components in Codename One is scrollable by default
 * which means you should use it within a non-scrollable hierarchy. If you wish to add it into a scrollable 
 * {@link com.codename1.ui.Container} you should disable it's default scrollability using {@code setScrollable(false)}.
 * </p>
 * 
 * <h3>Example Usage</h3>
 * 
 * <script src="https://gist.github.com/codenameone/2b48d1650d8c5032d094066c79922cf1.js"></script>
 * 
 * <h3>Screenshots</h3>
 * <p><img src="https://www.codenameone.com/img/developer-guide/components-accordion.png" alt="Accordion Component"/></p>
 * 
 * 
 * @author Chen
 */
public class Accordion extends Container {

    private Image closeIcon;

    private Image openIcon;

    private boolean autoClose = true;
    
    /**
     * Empty Constructor
     */ 
    public Accordion() {
        super.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        closeIcon = FontImage.createMaterial(FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, UIManager.getInstance().getComponentStyle("Label"));
        openIcon = FontImage.createMaterial(FontImage.MATERIAL_KEYBOARD_ARROW_DOWN, UIManager.getInstance().getComponentStyle("Label"));
        setScrollableY(true);
    }

    /**
     * Add an item to the Accordion Container
     * 
     * @param header the item title
     * @param body the item Component to hide/show
     */ 
    public void addContent(String header, Component body) {
        addContent(new Label(header), body);
    }

    /**
     * Replaces the title for content that was already added. Notice that this will fail if the content isn't
     * in yet.
     * @param header the new title for the content
     * @param body the content that was already added with a different header using addContent
     */
    public void setHeader(String header, Component body) {
        AccordionContent ac = (AccordionContent) body.getParent();
        ((Label)ac.header).setText(header);
    }
    
    /**
     * Replaces the title for content that was already added. Notice that this will fail if the content isn't
     * in yet.
     * @param header the new title for the content
     * @param body the content that was already added with a different header using addContent
     */
    public void setHeader(Component header, Component body) {
        AccordionContent ac = (AccordionContent) body.getParent();
        ac.header.getParent().replace(ac.header, header, null);
    }
    
    /**
     * Removes the content from the accordion
     * @param body the body previously added with {@link #addContent(com.codename1.ui.Component, com.codename1.ui.Component)} or
     */
    public void removeContent(Component body) {
        body.getParent().remove();
        body.remove();
    }
    
    /**
     * Add an item to the Accordion Container
     * 
     * @param header the item title Component
     * @param body the item Component to hide/show
     */ 
    public void addContent(Component header, Component body) {        
        add(new AccordionContent(header, body));
    }

    /**
     * Sets the closed icon
     * @param closeIcon the close icon
     */ 
    public void setCloseIcon(Image closeIcon) {
        this.closeIcon = closeIcon;
    }

    /**
     * Sets the open icon
     * @param openIcon the open icon
     */ 
    public void setOpenIcon(Image openIcon) {
        this.openIcon = openIcon;
    }

    /**
     * Sets the auto close flag, if this flag is true clicking on an item to open 
     * an item will automatically close the previous opened item.
     * 
     * @param autoClose determines if more then 1 item can be opened on screen
     */ 
    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }
    
    class AccordionContent extends Container {
        
        private boolean closed = true;
        
        private Button arrow = new Button();
        
        private Component body;
        Component header;
            
        public AccordionContent(Component header, final Component body) {
            setUIID("AccordionItem");
            setLayout(new BorderLayout());
            this.body = body;
            this.header = header;
            header.setSelectedStyle(header.getUnselectedStyle());
            header.setPressedStyle(header.getUnselectedStyle());
            Container top = new Container(new BorderLayout());
            top.add(BorderLayout.CENTER, header);
            top.setUIID("AccordionHeader");
            arrow.setUIID("AccordionArrow");
            arrow.setIcon(closeIcon);
            arrow.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    
                    //toggle the current
                    openClose(!isClosed());
                    if(autoClose){
                        for (int i = 0; i < Accordion.this.getComponentCount(); i++) {
                            AccordionContent c = (AccordionContent)Accordion.this.getComponentAt(i);
                            if(c != AccordionContent.this && !c.isClosed()){
                                c.openClose(true);
                            }
                        }
                    }
                    Accordion.this.animateLayout(250);
                }
            });
            top.add(BorderLayout.EAST, arrow);
            top.setLeadComponent(arrow);
            add(BorderLayout.NORTH, top);
            body.setHidden(true);
            add(BorderLayout.CENTER, body);
        }

        public boolean isClosed() {
            return closed;
        }
        
        public void openClose(boolean close) {
            closed = close;
            if (closed) {
                arrow.setIcon(closeIcon);
            } else {
                arrow.setIcon(openIcon);
            }
            body.setHidden(closed);
        }


    }

}
