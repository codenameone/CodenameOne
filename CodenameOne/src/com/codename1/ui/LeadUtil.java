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

/**
 * A package-private utility class for working with working with lead components.
 * 
 * @author Steve Hannah
 * @since 7.0
 */
class LeadUtil {
    
    /**
     * Gets the lead parent for a component, or the component itself if there is
     * no lead parent.   
     * @param cmp The component whose lead parent we wish to retrieve.
     * @return The component's lead parent if one exists.  Will fall back to just return {@literal cmp}
     * if no lead parent exists.
     */
    public static Component leadParentImpl(Component cmp) {
        if (cmp == null) {
            return null;
        }
        if (cmp.hasLead) {
            Component out;
            if (cmp instanceof Container) {
                out = ((Container)cmp).getLeadParent();
            } else {
                out = cmp.getParent().getLeadParent();
            }
            if (out != null) {
                return out;
            }
        }
        return cmp;
    }
    
    /**
     * Gets the lead component for a component if one exists, or just passes through the 
     * component provided in the argument.
     * @param cmp The component.
     * @return Either the lead component of the component, or the component itself.
     */
    public static Component leadComponentImpl(Component cmp) {
        if (cmp == null) {
            return null;
        }
        // If a container is draggable or scrollable, then lead components make things too complicated
        // as routing events to a lead component may cause scrolling or dragging to 
        // work incorrectly.
        if (cmp.isDraggable() || cmp.scrollableXFlag() || cmp.scrollableYFlag()) {
            //return cmp;
        }
        if (cmp.hasLead) {
            Component out = cmp.getLeadComponent();
            if (out != null) {
                return out;
            }
        }
        return cmp;
    }

    /**
     * Dispatches a pointer pressed event to the lead component of {@literal cmp}.
     * @param cmp A component whose lead component we will fire pointerPressed event on.
     * @param x X-coord
     * @param y Y-coord
     */
    public static void pointerPressed(Component cmp, int x, int y) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.pointerPressed(x, y);
        Form f = cmp.getComponentForm();
        Component leadParent = leadParentImpl(cmp);
        if (f != null && !Display.impl.isScrollWheeling() && leadParent.isFocusable() && leadParent.isEnabled()) {
            f.setFocused(leadParent);
        }
        if (cmp != lead) {
            leadParent.repaint();
        }
        
    }

    /**
     * Dispatches a pointer dragged event to the lead component of {@literal cmp}.
     * @param cmp A component whose lead component we will fire pointerDragged event on.
     * @param x X-coord
     * @param y Y-coord
     */
    public static void pointerDragged(Component cmp, int x, int y) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.pointerDragged(x, y);
        if (cmp != lead) {
            leadParentImpl(cmp).repaint();
        }
        
    }
    
    /**
     * Dispatches a pointer dragged event to the lead component of {@literal cmp}.
     * @param cmp A component whose lead component we will fire pointerDragged event on.
     * @param x X-coords
     * @param y Y-coords
     */
    public static void pointerDragged(Component cmp, int[] x, int[] y) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.pointerDragged(x, y);
        if (cmp != lead) {
            leadParentImpl(cmp).repaint();
        }
        
    }

    /**
     * Dispatches a pointer released event to the lead component of {@literal cmp}.
     * @param cmp A component whose lead component we will fire pointerReleased event on.
     * @param x X-coord
     * @param y Y-coord
     */
    public static void pointerReleased(Component cmp, int x, int y) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.pointerReleased(x, y);
        if (lead != cmp) {
            leadParentImpl(cmp).repaint();
        }
    }
    
    /**
     * Dispatches a pointer pressed event to the lead component of {@literal cmp}.
     * @param cmp A component whose lead component we will fire pointerPressed event on.
     * @param x X-coord
     * @param y Y-coord
     */
    public static void pointerHoverReleased(Component cmp, int x[], int y[]) {
        if (cmp == null) {
            return;
        }
        leadComponentImpl(cmp).pointerHoverReleased(x, y);
        if (leadParentImpl(cmp) != cmp) {
            leadParentImpl(cmp).repaint();
        }
    }
    
    public static void pointerHoverPressed(Component cmp, int x[], int y[]) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.pointerHoverPressed(x, y);
        if (lead != cmp) {
            leadParentImpl(cmp).repaint();
        }
    }
    
    public static void pointerHover(Component cmp, int x[], int y[]) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.pointerHover(x, y);
        if (lead != cmp) {
            leadParentImpl(cmp).repaint();
        }
    }

    public static void dragFinished(Component cmp, int x, int y) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.dragFinishedImpl(x, y);
        if (lead != cmp) {
            leadParentImpl(cmp).repaint();
        }
    }
    
    public static void longPointerPress(Component cmp, int x, int y) {
        if (cmp == null) {
            return;
        }
        Component lead = leadComponentImpl(cmp);
        lead.longPointerPress(x, y);
        if (cmp != lead) {
            leadParentImpl(cmp).repaint();
        }
    }
    
    public static void dragInitiated(Component cmp) {
        if (cmp == null) {
            return;
        }
        Form f = cmp.getComponentForm();
        if (f != null) {
            Component fc = f.getFocused();
            if (fc != null) {
                fc.dragInitiated();
            }
        }
        //leadParentImpl(cmp).dragInitiated();
    }
}
