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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/**
 * Allows adapting a scroll container to scroll indefinitely (or at least until
 * we run out of data), this effectively works by showing an infinite progress
 * indicator when reaching scroll end then allowing code to fetch more components.
 *
 * @author Shai Almog
 */
public class InfiniteScrollAdapter {    
    private Container infiniteContainer;
    private Runnable fetchMore;
    private InfiniteProgress ip = new InfiniteProgress();
    private Component endMarker = new Component() {
        public Dimension calcPreferredSize() {
            return new Dimension(1,1);
        }
        public void paint(Graphics g) {
            if(isInClippingRegion(g)) {
                reachedEnd();
            }
        }
    };
    
    private InfiniteScrollAdapter() {}
    
    void reachedEnd() {
        infiniteContainer.removeComponent(endMarker);
        infiniteContainer.addComponent(ip);
        infiniteContainer.revalidate();
        fetchMore.run();
    }
    
    /**
     * Creates an instance of the InfiniteScrollAdapter that will invoke the fetch more
     * callback to fetch additional components, once that method completes its task it 
     * should add the components via the addMoreComponents() invocation.
     * Notice that the container MUST be empty when invoking this method, fetchMore 
     * will be invoked immediately and you can add your data when ready.
     * 
     * @param cont the container to bind, it MUST be empty and must be scrollable on the Y axis
     * @param fetchMore a callback that will be invoked on the EDT to fetch more data (do not block this method)
     */
    public static InfiniteScrollAdapter createInfiniteScroll(Container cont, Runnable fetchMore) {
        InfiniteScrollAdapter a = new InfiniteScrollAdapter();
        a.infiniteContainer = cont;
        a.fetchMore = fetchMore;
        cont.addComponent(a.ip);
        Display.getInstance().callSerially(fetchMore);
        return a;
    }
    
    /**
     * Invoke this method to add additional components to the container, if you use 
     * addComponent/removeComponent you will get undefined behavior.
     * @param components the components to add
     * @param areThereMore whether additional components exist
     */
    public void addMoreComponents(Component[] components, boolean areThereMore) {
        infiniteContainer.removeComponent(ip);
        infiniteContainer.removeComponent(endMarker);
        for(Component c : components) {
            c.setY(infiniteContainer.getHeight());
            infiniteContainer.addComponent(c);
        }
        infiniteContainer.animateLayoutAndWait(300);
        if(areThereMore) {
            // if this is animated we get redundant calls to reached end
            infiniteContainer.addComponent(endMarker);
            infiniteContainer.revalidate();
        }
    }
    
}
