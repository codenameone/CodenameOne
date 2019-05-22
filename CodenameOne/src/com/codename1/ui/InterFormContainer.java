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
package com.codename1.ui;

import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.ComponentSelector.Filter;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * A container that allows you to use the same component on multiple forms.  This is most 
 * useful for adding a global footer or sidebar that appears on multiple forms, or even the 
 * whole app.  For example, the Twitter app has tabs for "Home", "Search", "Messages", etc.. that
 * are always shown in the app, and are fixed in place, even between forms.  This container
 * allows you to achieve a similar thing with Codename One apps.
 * 
 * <p>Note that the InterFormContainer object itself cannot be added to multiple forms.   You 
 * need to create two InterFormContainer instances that share the same content.</p>
 * @since 7.0
 * @author shannah
 */
public class InterFormContainer extends Container {
    private Component content;

    /**
     * Creates an interform container with the provided content.
     * @param content The component that is to be shared across multiple forms.
     */
    public InterFormContainer(Component content) {
        this.content = content;
        setLayout(new BorderLayout());
        $(this).selectAllStyles().setPadding(0).setMargin(0);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initComponent() {
        if (content.getParent() != this) {
            content.remove();
            add(CENTER, content);
        }
        
        super.initComponent();
    }
    
    /**
     * Finds any InterformContainer instances in the UI hierarchy rooted at {@literal root}
     * that contains the same content as this container.
     * @param root A component/container whose UI hierarchy is to be searched.
     * @return An InterFormContainer with the same content as this container, or null if none is found.
     */
    public InterFormContainer findPeer(Component root) {
        if (root.getClass() == InterFormContainer.class) {
            InterFormContainer cnt = (InterFormContainer)root;
            if (cnt.content == content) {
                return cnt;
            }
        }
        if (root instanceof Container) {
            Container cnt = (Container)root;
            int len = cnt.getComponentCount();
            for (int i=0; i<len; i++) {
                InterFormContainer peer = findPeer(cnt.getComponentAt(i));
                if (peer != null) {
                    return peer;
                }
            }
        }
        return null;
    }
    
    /**
     * Finds common InterFormContainers in two different component trees.
     * @param root1 The root of the first component tree to search.
     * @param root2 The root of the second component tree to search.
     * @return A Map that maps an InterFormContainer from the first tree to its corresponding container from the second
     * tree.
     */
    public static Map<InterFormContainer,InterFormContainer> findCommonContainers(Component root1, Component root2) {
        final Map<Component, InterFormContainer> set1 = new HashMap<Component,InterFormContainer>();
        final Map<Component, InterFormContainer> set2 = new HashMap<Component,InterFormContainer>();
        final Map<InterFormContainer,InterFormContainer> out = new HashMap<InterFormContainer,InterFormContainer>();
        ComponentSelector.select("*", root1).filter(new Filter() {
            @Override
            public boolean filter(Component c) {
                if (c.getClass() == InterFormContainer.class) {
                    set1.put(((InterFormContainer)c).content, (InterFormContainer)c);
                    return true;
                }
                return false;
            }
            
        });
        ComponentSelector.select("*", root2).filter(new Filter() {
            @Override
            public boolean filter(Component c) {
                if (c.getClass() == InterFormContainer.class) {
                     set2.put(((InterFormContainer)c).content, (InterFormContainer)c);
                    return true;
                }
                return false;
            }
            
        });
        
        for (Component c : set1.keySet()) {
            if (set2.containsKey(c)) {
                out.put(set1.get(c), set2.get(c));
            }
        }
        return out;
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        if (!isVisible()) {
            return;
        }
        if (content.getParent() != this) {
            if (isInitialized() && !content.isInitialized()) {
                if (content.getParent() != null) {
                    content.remove();
                }
                add(BorderLayout.CENTER, content);
            }
        } 
        
        g.translate(getX() - content.getX(), getY() - content.getY());
        content.paint(g);
        g.translate(content.getX() - getX(), content.getY() - getY());
    }
    
    
    
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(content.getPreferredW() + content.getStyle().getHorizontalMargins(), content.getPreferredH() + content.getStyle().getVerticalMargins());
    }
    
    
    
}
