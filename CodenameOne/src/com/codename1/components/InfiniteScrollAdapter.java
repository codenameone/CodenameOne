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
import com.codename1.ui.layouts.FlowLayout;

/**
 * <p>Allows adapting a scroll container to scroll indefinitely (or at least until
 * running out of data), this effectively works by showing an infinite progress
 * indicator when reaching scroll end then allowing code to fetch additional components.</p>
 * 
 * <p>
 * The sample code shows the usage of the nestoria API to fill out an infinitely scrolling list.
 * </p>
 * <script src="https://gist.github.com/codenameone/af27af111ba766627363.js"></script>
 * 
 * <img src="https://www.codenameone.com/img/developer-guide/components-infinitescrolladapter.png" alt="Sample usage of infinite scroll adapter" /><br><br>
 * 
 *
 * @author Shai Almog
 */
public class InfiniteScrollAdapter {    
    private Container infiniteContainer;
    private Runnable fetchMore;
    private Component ip;
    private int componentLimit = -1;
    private InfiniteProgress progress;
    
    class EdgeMarker extends Component {
        private boolean top;
        public EdgeMarker(boolean top) {
            this.top = top;
        }
        
        public Dimension calcPreferredSize() {
            return new Dimension(1,1);
        }
        public void paint(Graphics g) {
            if(getParent() != null && isInClippingRegion(g)) {
                reachedEnd();
            }
        }        
    }
    
    private Component endMarker = new EdgeMarker(true);
    
    private InfiniteScrollAdapter() {
        progress = new InfiniteProgress();
        Container p = new Container(new FlowLayout(Component.CENTER));
        p.addComponent(progress);
        ip = p;
    }
    
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
     * @return an instance of this class that can be used to add components
     */
    public static InfiniteScrollAdapter createInfiniteScroll(Container cont, Runnable fetchMore) {
        return createInfiniteScroll(cont, fetchMore, true);
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
     * @param fetchOnCreate if true the fetchMore callback is called upon calling this method
     * @return an instance of this class that can be used to add components
     */
    public static InfiniteScrollAdapter createInfiniteScroll(Container cont, Runnable fetchMore, boolean fetchOnCreate) {
        InfiniteScrollAdapter a = new InfiniteScrollAdapter();
        cont.putClientProperty("cn1$infinite", a);
        a.infiniteContainer = cont;
        a.fetchMore = fetchMore;
        if(fetchOnCreate){
            cont.addComponent(a.ip);
            Display.getInstance().callSerially(fetchMore);
        }else{
            a.infiniteContainer.addComponent(a.endMarker);
        }
        return a;
    }
    
    /**
     * Invoke this method to add additional components to the container, if you use 
     * addComponent/removeComponent you will get undefined behavior.
     * This is a convenience method saving the need to keep the InfiniteScrollAdapter as a variable
     * @param cnt container to add the components to
     * @param components the components to add
     * @param areThereMore whether additional components exist
     */
    public static void addMoreComponents(Container cnt, Component[] components, boolean areThereMore) {
        InfiniteScrollAdapter ia = (InfiniteScrollAdapter)cnt.getClientProperty("cn1$infinite");
        ia.addMoreComponents(components, areThereMore);
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
        
        if(componentLimit > 0) {
            int diff = infiniteContainer.getComponentCount() - componentLimit;
            while(diff > 0) {
                infiniteContainer.removeComponent(infiniteContainer.getComponentAt(0));
                diff--;
            }
        }
        
        infiniteContainer.revalidate();
        if(areThereMore) {
            // if this is animated we get redundant calls to reached end
            infiniteContainer.addComponent(endMarker);
            infiniteContainer.revalidate();
        }
    }

    /**
     * The component limit defines the number of components that should be within the infinite scroll adapter,
     * if more than component limit is added then the appropriate number of components is removed from the top.
     * This prevents running out of memory or performance overhead with too many components... 
     * Notice that -1 is a special case value for no component limit.
     * @return the componentLimit
     */
    public int getComponentLimit() {
        return componentLimit;
    }

    /**
     * The component limit defines the number of components that should be within the infinite scroll adapter,
     * if more than component limit is added then the appropriate number of components is removed from the top.
     * This prevents running out of memory or performance overhead with too many components... 
     * Notice that -1 is a special case value for no component limit.
     * @param componentLimit the componentLimit to set
     */
    public void setComponentLimit(int componentLimit) {
        this.componentLimit = componentLimit;
    }

    /**
     * Lets us manipulate the infinite progress object e.g. set the animation image etc.
     * @return the infinite progress component underlying this adapter
     */
    public InfiniteProgress getInfiniteProgress() {
        return progress;
    }
}
