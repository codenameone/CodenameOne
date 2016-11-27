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
package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/**
 * <p>The {@code LayeredLayout} places the components in order one on top of the other and sizes 
 * them all to the size of the largest component. This is useful when trying to create an overlay on top of an 
 * existing component. E.g. an "x" button to allow removing the component as shown here</p>
 *
 * <img src="https://www.codenameone.com/img/developer-guide/layered-layout.png" alt="The X on this button was placed there using the layered layout code below" />
 *
 * <p>The code to generate this UI is slightly complex and contains very little relevant pieces. The only truly relevant 
 * piece the last line of code:</p>
 * 
 * <script src="https://gist.github.com/codenameone/d0491ce08ce6b889bbd5.js"></script>* 
 *
 *
 * <p>We are doing three distinct things here:</p>
 * <ul>
 *.<li> We are adding a layered layout to the form.</li>
 * <li> We are creating a layered layout and placing two components within. This would be the equivalent of just 
 *     creating a {@code LayeredLaout} {@link com.codename1.ui.Container} and invoking `add` twice.</li>
.* <li> We use https://www.codenameone.com/javadoc/com/codename1/ui/layouts/FlowLayout.html[FlowLayout] to position the `X` close button in the right position.</li>
 * </ul>
 * 
 * <p>
 * A common use case for {@code LayeredLayout} is the iOS carousel effect which we can achieve by combing
 * the {@code LayeredLayout}  with {@link com.codename1.ui.Tabs}.
 * </p>
 * <script src="https://gist.github.com/codenameone/e981c3f91f98f1515987.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tabs-swipe1.png" alt="Tabs carousel page 1" />
 * 
 * <p>Notice that the layered layout sizes all components to the exact same size one on top of the other. It usually 
 * requires that we use another container within; in order to position the components correctly.<br />
 *
 * Forms have a built in layered layout that you can access via `getLayeredPane()`, this allows you to overlay 
 * elements on top of the content pane.<br />
 *
 * The layered pane is used internally by components such as {@link com.codename1.components.InteractionDialog}, 
 * {@link com.codename1.u./AutoCompleteTextField} etc.
 *</p>
 * <p>
 *   Warning: Placing native widgets within a layered layout is problematic due to the behavior of peer 
 *   components. Sample of peer components include the {@link com.codename1.ui.BrowserComponent}, 
 *   video playback etc. 
 * </p>
 *
 * @see com.codename1.ui.Form#getLayeredPane() 
 * @see com.codename1.ui.Form#getLayeredPane(java.lang.Class, boolean) 
 * @see com.codename1.ui.Form#setGlassPane(com.codename1.ui.Painter) 
 * @author Shai Almog
 */
public class LayeredLayout extends Layout {

    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {
        Style s = parent.getStyle();
        int top = s.getPaddingTop();
        int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
        int left = s.getPaddingLeft(parent.isRTL());
        int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
        
        int numOfcomponents = parent.getComponentCount();

        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            s = cmp.getStyle();
            
            int x = left + s.getMarginLeft(parent.isRTL());
            int y = top + s.getMarginTop();
            int w = right - left - s.getHorizontalMargins();
            int h = bottom - top - s.getVerticalMargins();
            cmp.setX(x);
            cmp.setY(y);
            cmp.setWidth(w);
            cmp.setHeight(h);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        int maxWidth = 0;
        int maxHeight = 0;
        int numOfcomponents = parent.getComponentCount();
        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            maxHeight = Math.max(maxHeight, cmp.getPreferredH() + cmp.getStyle().getMarginTop() + cmp.getStyle().getMarginBottom());
            maxWidth = Math.max(maxWidth, cmp.getPreferredW()+ cmp.getStyle().getMarginLeftNoRTL() + cmp.getStyle().getMarginRightNoRTL());
        }
        Style s = parent.getStyle();
        Dimension d = new Dimension(maxWidth + s.getPaddingLeftNoRTL() + s.getPaddingRightNoRTL(),
                maxHeight + s.getPaddingTop() + s.getPaddingBottom());
        return d;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "LayeredLayout";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverlapSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean obscuresPotential(Container parent) {
        return true;
    }
    
    /**
     * Shorthand for Container.encloseIn(new LayeredLayout(), cmps); 
     * @param cmps the components to add to a new layered layout container
     * @return a newly created layered layout
     */
    public static Container encloseIn(Component... cmps) {
        return Container.encloseIn(new LayeredLayout(), cmps);
    }
}
