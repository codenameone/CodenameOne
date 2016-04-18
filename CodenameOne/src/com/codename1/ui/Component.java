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
package com.codename1.ui;

import com.codename1.cloud.BindTarget;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.StyleListener;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 * <p>The component class is the basis of all UI widgets in Codename One, to arrange multiple components 
 * together we use the Container class which itself "IS A" Component subclass. The Container is a 
 * Component that contains Components effectively allowing us to nest Containers infinitely to build any type 
 * of visual hierarchy we want by nesting Containers.
 * </p>
 * 
 * @see Container
 * @author Chen Fishbein
 */
public class Component implements Animation, StyleListener {
    /**
     * Used by getDragRegionStatus to indicate no dragability
     */
    public static final int DRAG_REGION_NOT_DRAGGABLE = 1;

    /**
     * Used by getDragRegionStatus to indicate limited dragability
     */
    public static final int DRAG_REGION_POSSIBLE_DRAG_X = 10;

    /**
     * Used by getDragRegionStatus to indicate limited dragability
     */
    public static final int DRAG_REGION_POSSIBLE_DRAG_Y = 11;

    /**
     * Used by getDragRegionStatus to indicate limited dragability
     */
    public static final int DRAG_REGION_POSSIBLE_DRAG_XY = 12;

    /**
     * Used by getDragRegionStatus to indicate likely dragability
     */
    public static final int DRAG_REGION_LIKELY_DRAG_X = 21;

    /**
     * Used by getDragRegionStatus to indicate likely dragability
     */
    public static final int DRAG_REGION_LIKELY_DRAG_Y = 22;

    /**
     * Used by getDragRegionStatus to indicate likely dragability
     */
    public static final int DRAG_REGION_LIKELY_DRAG_XY = 23;
    
    private String selectText;
    private boolean alwaysTensile;
    private int tensileLength = -1;

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     */
    private Component nextFocusDown;
    private Component nextFocusUp;
    
    /**
     * Indicates whether component is enabled or disabled
     */
    private boolean enabled = true;
    
    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the right key
     */
    private Component nextFocusRight;
    private Component nextFocusLeft;
    private String name;
    boolean hasLead;

    /**
     * This property is useful for blocking in z-order touch events, sometimes we might want to grab touch events in
     * a specific component without making it focusable.
     */
    private boolean grabsPointerEvents;

    /**
     * Indicates whether tensile drag (dragging beyond the boundary of the component and
     * snapping back) is enabled for this component.
     */
    private boolean tensileDragEnabled;

    /**
     * Indicates whether tensile highlight (drawing a highlight effect when reaching the edge) is enabled for this component.
     */
    private boolean tensileHighlightEnabled;
    private int tensileHighlightIntensity;

    /**
     * Indicates whether the component should "trigger" tactile touch when pressed by the user
     * in a touch screen UI.
     */
    private boolean tactileTouch;

    /**
     * Baseline resize behavior constant used to properly align components. 
     * Indicates as the size of the component
     * changes the baseline remains a fixed distance from the top of the
     * component.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_CONSTANT_ASCENT = 1;
    /**
     * Baseline resize behavior constant used to properly align components. Indicates as the size of the component
     * changes the baseline remains a fixed distance from the bottom of the 
     * component.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_CONSTANT_DESCENT = 2;
    /**
     * Baseline resize behavior constant used to properly align components. Indicates as the size of the component
     * changes the baseline remains a fixed distance from the center of the
     * component.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_CENTER_OFFSET = 3;
    /**
     * Baseline resize behavior constant used to properly align components. Indicates as the size of the component
     * changes the baseline can not be determined using one of the other
     * constants.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_OTHER = 4;
    private boolean visible = true;
    /**
     * Used as an optimization to mark that this component is currently being
     * used as a cell renderer
     */
    private boolean cellRenderer;
    private Rectangle bounds = new Rectangle(0, 0, new Dimension(0, 0));
    private Rectangle painterBounds;
    private int scrollX;
    private int scrollY;
    private boolean sizeRequestedByUser = false;
    private Dimension preferredSize;
    private boolean scrollSizeRequestedByUser = false;
    Dimension scrollSize;
    private Style unSelectedStyle;
    private Style pressedStyle;
    private Style selectedStyle;
    private Style disabledStyle;
    private Style allStyles;
    private Container parent;
    private boolean focused = false;
    private boolean handlesInput = false;
    boolean shouldCalcPreferredSize = true;
    boolean shouldCalcScrollSize = true;
    private boolean focusable = true;
    private boolean isScrollVisible = true;
    private boolean repaintPending;
    private boolean snapToGrid;

    private boolean hideInPortrait;
    private int scrollOpacity = 0xff;
            
    /**
     * Indicates the decrement units for the scroll opacity
     */
    private int scrollOpacityChangeSpeed = 5;

    /**
     * Indicates that moving through the component should work as an animation
     */
    private boolean smoothScrolling;
    
    private static boolean disableSmoothScrolling = false;

    /**
     * Animation speed in milliseconds allowing a developer to slow down or accelerate
     * the smooth animation mode
     */
    private int animationSpeed;
    private Motion animationMotion;
    Motion draggedMotionX;
    Motion draggedMotionY;

    /**
     * Allows us to flag a drag operation in action thus preventing the mouse pointer
     * release event from occurring.
     */
    private boolean dragActivated;
    private int oldx, oldy, draggedx, draggedy;
    private int initialScrollY = -1;
    private int destScrollY = -1;
    private int lastScrollY;
    private int lastScrollX;
    private boolean shouldGrabScrollEvents;

    /**
     * Indicates if the component is in the initialized state, a component is initialized
     * when its initComponent() method was invoked. The initMethod is invoked before showing the
     * component to the user.
     */
    private boolean initialized;
    /**
     * Indicates a Component center alignment
     */
    public static final int CENTER = 4;
    /** 
     * Box-orientation constant used to specify the top of a box.
     */
    public static final int TOP = 0;
    /** 
     * Box-orientation constant used to specify the left side of a box.
     */
    public static final int LEFT = 1;
    /** 
     * Box-orientation constant used to specify the bottom of a box.
     */
    public static final int BOTTOM = 2;
    /** 
     * Box-orientation constant used to specify the right side of a box.
     */
    public static final int RIGHT = 3;
    
    /**
     * Alignment to the baseline constraint
     */
    public static final int BASELINE = 5;
    
    private HashMap<String, Object> clientProperties;
    private Rectangle dirtyRegion = null;
    private final Object dirtyRegionLock = new Object();
    private Label componentLabel;
    private String id;

    /**
     * Is the component a bidi RTL component
     */
    private boolean rtl;
    private boolean flatten;

    private Object paintLockImage;

    private boolean draggable;
    private boolean dragAndDropInitialized;
    private boolean dropTarget;
    private Image dragImage;
    private Component dropTargetComponent;
    private int dragCallbacks = 0;

    private String cloudBoundProperty;
    private String cloudDestinationProperty;
    boolean noBind;
    private Runnable refreshTask;
    private double pinchDistance;
    static int restoreDragPercentage = -1;

    private Component[] sameWidth;
    private Component[] sameHeight;
    
    private EventDispatcher focusListeners;
    private EventDispatcher scrollListeners;
    private EventDispatcher dropListener;
    private EventDispatcher dragOverListener;    
    EventDispatcher pointerPressedListeners;
    EventDispatcher pointerReleasedListeners;
    EventDispatcher pointerDraggedListeners;
    boolean isUnselectedStyle;
    
    boolean isDragAndDropInitialized() {
        return dragAndDropInitialized;
    }

    /**
     * Places all of these components in the same width group, to remove a component from
     * the group invoke this method with that component only.
     * 
     * @param c the components to group together, this will override all previous width grouping
     */
    public static void setSameWidth(Component... c) {
        if(c.length == 1) {
            // special case, remove grouping
            if(c[0].sameWidth != null) {
                ArrayList<Component> lst = new ArrayList<Component>(Arrays.asList(c[0].sameWidth));
                lst.remove(c[0]);
                if(lst.size() == 1) {
                    lst.get(0).sameWidth = null;
                } else {
                    if(lst.size() > 0) {
                        Component[] cmps = new Component[lst.size()];
                        lst.toArray(cmps);
                        setSameWidth(cmps);
                    }
                }
                c[0].sameWidth = null;
            }
        } else {
            for(Component cc : c) {
                cc.sameWidth = c;
            }
        }
    }
    
    /**
     * Returns a "meta style" that allows setting styles once to all the different Style objects, the getters for this
     * style will be meaningless and will return 0 values. Usage:
     * 
     * <script src="https://gist.github.com/codenameone/31a32bdcf014a9e55a95.js"></script>
     * @return a unified style object for the purpose of setting on object object instances
     */
    public Style getAllStyles() {
        if(allStyles == null) {
            allStyles = Style.createProxyStyle(getUnselectedStyle(), getSelectedStyle(), getPressedStyle(), getDisabledStyle());
        }
        return allStyles;
    }
    
    /**
     * Returns the array of components that have an equal width
     * 
     * @return components in the same width group
     */
    public Component[] getSameWidth() {
        return sameWidth;
    }
    

    /**
     * Places all of these components in the same height group, to remove a component from
     * the group invoke this method with that component only.
     * 
     * @param c the components to group together, this will override all previous height grouping
     */
    public static void setSameHeight(Component... c) {
        if(c.length == 1) {
            // special case, remove grouping
            if(c[0].sameHeight != null) {
                ArrayList<Component> lst = new ArrayList<Component>(Arrays.asList(c[0].sameHeight));
                lst.remove(c[0]);
                if(lst.size() == 1) {
                    lst.get(0).sameHeight = null;
                } else {
                    if(lst.size() > 0) {
                        Component[] cmps = new Component[lst.size()];
                        lst.toArray(cmps);
                        setSameHeight(cmps);
                    }
                }
                c[0].sameHeight = null;
            }
        } else {
            for(Component cc : c) {
                cc.sameHeight = c;
            }
        }
    }
    
    /**
     * Returns the array of components that have an equal height
     * 
     * @return components in the same height group
     */
    public Component[] getSameHeight() {
        return sameHeight;
    }

    /** 
     * Creates a new instance of Component 
     */
    protected Component() {
        initLaf(getUIManager());
    }
    
    /**
     * This method initializes the Component defaults constants
     */
    protected void initLaf(UIManager uim){
        if(uim == getUIManager() && isInitialized()){
            return;
        }
        selectText = uim.localize("select", "Select");
        LookAndFeel laf = uim.getLookAndFeel();
        animationSpeed = laf.getDefaultSmoothScrollingSpeed();
        rtl = laf.isRTL();
        tactileTouch = isFocusable();
        tensileDragEnabled = laf.isDefaultTensileDrag();
        snapToGrid = laf.isDefaultSnapToGrid();
        alwaysTensile = laf.isDefaultAlwaysTensile();
        tensileHighlightEnabled = laf.isDefaultTensileHighlight();
        scrollOpacityChangeSpeed = laf.getFadeScrollBarSpeed();
        isScrollVisible = laf.isScrollVisible();
        
        if(tensileHighlightEnabled) {
            tensileLength = 3;
        } else {
            tensileLength = -1;
        }        
    }

    private void initStyle() {
        unSelectedStyle = getUIManager().getComponentStyle(getUIID());
        lockStyleImages(unSelectedStyle);
        if (unSelectedStyle != null) {
            unSelectedStyle.addStyleListener(this);
            if (unSelectedStyle.getBgPainter() == null) {
                unSelectedStyle.setBgPainter(new BGPainter());
            }
        }
        if(disabledStyle != null) {
            disabledStyle.addStyleListener(this);
            if (disabledStyle.getBgPainter() == null) {
                disabledStyle.setBgPainter(new BGPainter());
            }
        }
    }

    /**
     * This method should be used by the Component to retrieve the correct UIManager to work with
     * @return a UIManager instance
     */
    public UIManager getUIManager(){
        Container parent = getParent();
        //if no parent return the default UIManager
        if(parent == null){
            return UIManager.getInstance();
        }
        return parent.getUIManager();        
    }
    
    /**
     * Returns the current component x location relatively to its parent container
     * 
     * @return the current x coordinate of the components origin
     */
    public int getX() {
        return bounds.getX();
    }

    /**
     * Returns the component y location relatively to its parent container
     * 
     * @return the current y coordinate of the components origin
     */
    public int getY() {
        return bounds.getY();
    }

    /**
     * Returns whether the component is visible or not
     * 
     * @return true if component is visible; otherwise false 
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Client properties allow the association of meta-data with a component, this
     * is useful for some applications that construct GUI's on the fly and need
     * to track the connection between the UI and the data. 
     * 
     * @param key the key used for putClientProperty
     * @return the value set to putClientProperty or null if no value is set to the property
     */
    public Object getClientProperty(String key) {
        if (clientProperties == null) {
            return null;
        }
        return clientProperties.get(key);
    }

    Component getLeadComponent() {
        Container p = getParent();
        if(p != null) {
            return p.getLeadComponent();
        }
        return null;
    }

    /**
     * Clears all client properties from this Component
     */ 
    public void clearClientProperties(){
        if(clientProperties != null){
            clientProperties.clear();
            clientProperties = null;
        }
    }
    
    /**
     * Client properties allow the association of meta-data with a component, this
     * is useful for some applications that construct GUI's on the fly and need
     * to track the connection between the UI and the data. Setting the value to
     * null will remove the client property from the component.
     * 
     * @param key arbitrary key for the property
     * @param value the value assigned to the given client property
     */
    public void putClientProperty(String key, Object value) {
        if (clientProperties == null) {
            if (value == null) {
                return;
            }
            clientProperties = new HashMap<String, Object>();
        }
        if (value == null) {
            clientProperties.remove(key);
            if (clientProperties.size() == 0) {
                clientProperties = null;
            }
        } else {
            clientProperties.put(key, value);
        }
    }

    /**
     * gets the Component dirty region,  this method is for internal use only and SHOULD NOT be invoked by user code.
     * Use repaint(int,int,int,int)
     * 
     * @return returns the region that needs repainting or null for the whole component
     */
    public final Rectangle getDirtyRegion() {
        return dirtyRegion;
    }

    /**
     * sets the Component dirty region, this method is for internal use only and SHOULD NOT be invoked by user code.
     * Use repaint(int,int,int,int)
     * 
     * @param dirty the region that needs repainting or null for the whole component
     */
    public final void setDirtyRegion(Rectangle dirty) {
        synchronized (dirtyRegionLock) {
            this.dirtyRegion = dirty;
        }

    }

    /**
     * Toggles visibility of the component
     * 
     * @param visible true if component is visible; otherwise false 
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the component width
     * 
     * @return the component width
     */
    public int getWidth() {
        return bounds.getSize().getWidth();
    }

    /**
     * Returns the component height
     * 
     * @return the component height
     */
    public int getHeight() {
        return bounds.getSize().getHeight();
    }

    /**
     * Sets the Component x location relative to the parent container, this method
     * is exposed for the purpose of external layout managers and should not be invoked
     * directly.
     * 
     * @param x the current x coordinate of the components origin
     */
    public void setX(int x) {
        bounds.setX(x);
        if(Form.activePeerCount > 0) {
            onParentPositionChange();
        }
    }

    /**
     * Sets the Component y location relative to the parent container, this method
     * is exposed for the purpose of external layout managers and should not be invoked
     * directly.
     * 
     * @param y the current y coordinate of the components origin
     */
    public void setY(int y) {
        bounds.setY(y);
        if(Form.activePeerCount > 0) {
            onParentPositionChange();
        }
    }
    
    /**
     * Indicates if the section within the X/Y area is a "drag region" where
     * we expect people to drag and never actually "press" in which case we
     * can instantly start dragging making perceived performance faster. This
     * is invoked by the implementation code to optimize drag start behavior
     * @param x x location for the touch
     * @param y y location for the touch 
     * @return true if the touch is in a region specifically designated as a "drag region"
     * @deprecated replaced with getDragRegionStatus
     */
    protected boolean isDragRegion(int x, int y) {
        return isDraggable();
    }
        
    /**
     * Indicates if the section within the X/Y area is a "drag region" where
     * we expect people to drag or press in which case we
     * can instantly start dragging making perceived performance faster. This
     * is invoked by the implementation code to optimize drag start behavior
     * @param x x location for the touch
     * @param y y location for the touch 
     * @return one of the DRAG_REGION_* values
     */
    protected int getDragRegionStatus(int x, int y) {
        if(isDraggable()) {
            return DRAG_REGION_LIKELY_DRAG_XY;
        }
        Component c = getScrollableFast();
        if(c != null) {
            boolean xc = c.scrollableXFlag();
            boolean yc = c.scrollableYFlag();
            if(isDragRegion(x, y)) {
                if(xc && yc) {
                    return DRAG_REGION_LIKELY_DRAG_XY;
                }
                if(xc) {
                    return DRAG_REGION_LIKELY_DRAG_X;
                }
                if(yc) {
                    return DRAG_REGION_LIKELY_DRAG_Y;
                }
            } else {
                if(xc && yc) {
                    return DRAG_REGION_POSSIBLE_DRAG_XY;
                }
                if(xc) {
                    return DRAG_REGION_POSSIBLE_DRAG_X;
                }
                if(yc) {
                    return DRAG_REGION_POSSIBLE_DRAG_Y;
                }
            }
        }
        return DRAG_REGION_NOT_DRAGGABLE;
    }
    
    /**
     * This callback allows subcomponents who are interested in following position change of their parents
     * to receive such an event
     */
    void onParentPositionChange() {        
    }
    
    /**
     * The baseline for the component text according to which it should be aligned
     * with other components for best visual look.
     * 
     * 
     * @param width the component width
     * @param height the component height
     * @return baseline value from the top of the component
     */
    public int getBaseline(int width, int height) {
        return height - getStyle().getPadding(false, BOTTOM);
    }

    /**
     * Returns a constant indicating how the baseline varies with the size
     * of the component.
     *
     * @return one of BRB_CONSTANT_ASCENT, BRB_CONSTANT_DESCENT,
     *         BRB_CENTER_OFFSET or BRB_OTHER
     */
    public int getBaselineResizeBehavior() {
        return BRB_OTHER;
    }

    /**
     * Sets the Component Preferred Size, there is no guarantee the Component will 
     * be sized at its Preferred Size. The final size of the component may be
     * smaller than its preferred size or even larger than the size.<br>
     * The Layout manager can take this value into consideration, but there is
     * no guarantee or requirement.
     * 
     * @param d the component dimension
     * @deprecated this method shouldn't be used, use sameWidth/Height, padding, margin or override calcPeferredSize
     * to reach similar functionality
     */
    public void setPreferredSize(Dimension d) {
        if(d == null) {
            sizeRequestedByUser = false;
            preferredSize = null;
            shouldCalcPreferredSize = true;
            return;
        }
        Dimension dim = preferredSize();
        dim.setWidth(d.getWidth());
        dim.setHeight(d.getHeight());
        sizeRequestedByUser = true;
    }


    /**
     * Returns the Component Preferred Size, there is no guarantee the Component will 
     * be sized at its Preferred Size. The final size of the component may be
     * smaller than its preferred size or even larger than the size.<br>
     * The Layout manager can take this value into consideration, but there is
     * no guarantee or requirement.
     * 
     * @return the component preferred size
     */
    public Dimension getPreferredSize() {
        return preferredSize();
    }

    Dimension getPreferredSizeWithMargin() {
        Dimension d = preferredSize();
        Style s = getStyle();
        return new Dimension(d.getWidth() +s.getMargin(LEFT) + s.getMargin(RIGHT), d.getHeight() + s.getMargin(TOP) + s.getMargin(BOTTOM));
    }

    /**
     * Returns the Components dimension in scrolling, this is very similar to the
     * preferred size aspect only it represents actual scrolling limits.
     * 
     * @return the component actual size with all scrolling
     */
    public Dimension getScrollDimension() {
        if (!scrollSizeRequestedByUser && (scrollSize == null || shouldCalcScrollSize)) {
            scrollSize = calcScrollSize();
            shouldCalcScrollSize = false;
        }
        return scrollSize;
    }

    /**
     * Method that can be overriden to represent the actual size of the component 
     * when it differs from the desireable size for the viewport
     * 
     * @return scroll size, by default this is the same as the preferred size
     */
    protected Dimension calcScrollSize() {
        return calcPreferredSize();
    }

    /**
     * Set the size for the scroll area
     * 
     * @param d dimension of the scroll area
     */
    public void setScrollSize(Dimension d) {
        if(d == null) {
            shouldCalcScrollSize = true;
            scrollSize = null;
            scrollSizeRequestedByUser = false;
            return;
        }
        scrollSize = d;
        scrollSizeRequestedByUser = true;
    }

    /**
     * Helper method to set the preferred width of the component.
     * 
     * @param preferredW the preferred width of the component
     * @see #setPreferredSize
     * @deprecated this method shouldn't be used, use sameWidth/Height, padding, margin or override calcPeferredSize
     * to reach similar functionality
     */
    public void setPreferredW(int preferredW) {
        setPreferredSize(new Dimension(preferredW, getPreferredH()));
    }

    /**
     * Helper method to set the preferred height of the component.
     * 
     * @param preferredH the preferred height of the component
     * @see #setPreferredSize
     * @deprecated this method shouldn't be used, use sameWidth/Height, padding, margin or override calcPeferredSize
     * to reach similar functionality
     */
    public void setPreferredH(int preferredH) {
        setPreferredSize(new Dimension(getPreferredW(), preferredH));
    }

    /**
     * Helper method to retrieve the preferred width of the component.
     * 
     * @return preferred width of the component
     * @see #getPreferredSize
     */
    public int getPreferredW() {
        return getPreferredSize().getWidth();
    }

    /**
     * Helper method to retrieve the preferred height of the component.
     * 
     * @return preferred height of the component
     * @see #getPreferredSize
     */
    public int getPreferredH() {
        return getPreferredSize().getHeight();
    }

    /**
     * Sets the Component width, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to effect the component size setPreferredSize should
     * be used.
     * 
     * @param width the width of the component
     * @see #setPreferredSize
     */
    public void setWidth(int width) {
        bounds.getSize().setWidth(width);
    }

    /**
     * Sets the Component height, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to effect the component size setPreferredSize should
     * be used.
     * 
     * @param height the height of the component
     * @see #setPreferredSize
     */
    public void setHeight(int height) {
        bounds.getSize().setHeight(height);
    }

    /**
     * Sets the Component size, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to effect the component size setPreferredSize should
     * be used.
     * 
     * @param d the component dimension
     * @see #setPreferredSize
     */
    public void setSize(Dimension d) {
        Dimension d2 = bounds.getSize();
        d2.setWidth(d.getWidth());
        d2.setHeight(d.getHeight());
    }

    /**
     * Unique identifier for a component.
     * This id is used to retrieve a suitable Style.
     * 
     * @return unique string identifying this component for the style sheet
     */
    public String getUIID() {
        return id;
    }

    /**
     * This method sets the Component the Unique identifier.
     * This method should be used before a component has been initialized
     * 
     * @param id UIID unique identifier for component type
     */
    public void setUIID(String id) {
        this.id = id;
        unSelectedStyle = null;
        selectedStyle = null;
        disabledStyle = null;
        pressedStyle = null;
        allStyles = null;
        if(!sizeRequestedByUser) {
            preferredSize = null;
        }
    }
    
    /**
     * This method will remove the Component from its parent.
     */
    public void remove(){
        if(parent != null){
            parent.removeComponent(this);
        }
    }

    /**
     * Returns the container in which this component is contained
     * 
     * @return the parent container in which this component is contained
     */
    public Container getParent() {
        return parent;
    }

    /**
     * Sets the Component Parent.
     * This method should not be called by the user.
     * 
     * @param parent the parent container
     */
    void setParent(Container parent) {
        this.parent = parent;
    }

    /**
     * Registers interest in receiving callbacks for focus gained events, a focus event 
     * is invoked when the component accepts the focus. A special case exists for the
     * Form which sends a focus even for every selection within the form.
     * 
     * @param l listener interface implementing the observable pattern
     */
    public void addFocusListener(FocusListener l) {
        if(focusListeners == null) {
            focusListeners = new EventDispatcher();
        }
        focusListeners.addListener(l);
    }

    /**
     * Deregisters interest in receiving callbacks for focus gained events
     * 
     * @param l listener interface implementing the observable pattern
     */
    public void removeFocusListener(FocusListener l) {
        if(focusListeners == null) {
            return;
        }
        focusListeners.removeListener(l);
    }

    /**
     * Registers interest in receiving callbacks for scroll gained events, 
     * a scroll event is invoked when the component is scrolled.
     * 
     * @param l listener interface implementing the observable pattern
     */
    public void addScrollListener(ScrollListener l) {
        if(scrollListeners == null){
            scrollListeners = new EventDispatcher();                    
        }
        scrollListeners.addListener(l);
    }

    /**
     * Deregisters interest in receiving callbacks for scroll gained events
     * 
     * @param l listener interface implementing the observable pattern
     */
    public void removeScrollListener(ScrollListener l) {
        if(scrollListeners == null) {
            return;
        }
        scrollListeners.removeListener(l);
        if(!scrollListeners.hasListeners()) {
            scrollListeners = null;
        }
    }
    
    /**
     * When working in 3 softbutton mode "fire" key (center softbutton) is sent to this method
     * in order to allow 3 button devices to work properly. When overriding this method
     * you should also override isSelectableInteraction to indicate that a command is placed
     * appropriately on top of the fire key for 3 soft button phones. 
     */
    protected void fireClicked() {
    }

    /**
     * This method allows a component to indicate that it is interested in an "implicit" select
     * command to appear in the "fire" button when 3 softbuttons are defined in a device.
     * 
     * @return true if this is a selectable interaction
     */
    protected boolean isSelectableInteraction() {
        return false;
    }

    /**
     * Fired when component gains focus
     */
    void fireFocusGained() {
        fireFocusGained(this);
    }

    /**
     * Fired when component lost focus
     */
    void fireFocusLost() {
        fireFocusLost(this);
    }

    /**
     * Fired when component gains focus
     */
    void fireFocusGained(Component cmp) {
        if (cmp.isCellRenderer()) {
            return;
        }

        if(focusListeners != null) {
            focusListeners.fireFocus(cmp);
        }
        focusGainedInternal();
        focusGained();
        if (isSelectableInteraction()) {
            Form f = getComponentForm();
            if (f != null) {
                f.getMenuBar().addSelectCommand(getSelectCommandText());
            }
        }
    }

    /**
     * Allows determining the text for the select command used in the 3rd softbutton
     * mode.
     *
     * @param selectText text for the interaction with the softkey
     */
    public void setSelectCommandText(String selectText) {
        this.selectText = selectText;
    }

    /**
     * Allows determining the text for the select command used in the 3rd softbutton
     * mode.
     *
     * @return text for the interaction with the softkey
     */
    public String getSelectCommandText() {
        return selectText;
    }

    /**
     * Fired when component lost focus
     */
    void fireFocusLost(Component cmp) {
        if (cmp.isCellRenderer()) {
            return;
        }
        if (isSelectableInteraction()) {
            Form f = getComponentForm();
            if (f != null) {
                f.getMenuBar().removeSelectCommand();
            }
        }

        if(focusListeners != null) {
            focusListeners.fireFocus(cmp);
        }
        focusLostInternal();
        focusLost();
    }

    /**
     * This method allows us to detect an action event internally without 
     * implementing the action listener interface.
     */
    void fireActionEvent() {
    }

    /**
     * Allows us to indicate the label associated with this component thus providing
     * visual feedback related for this component e.g. starting the ticker when 
     * the component receives focus.
     * 
     * @param componentLabel a label associated with this component
     */
    public void setLabelForComponent(Label componentLabel) {
        this.componentLabel = componentLabel;
    }

    /**
     * Allows us to indicate the label associated with this component thus providing
     * visual feedback related for this component e.g. starting the ticker when
     * the component receives focus.
     *
     * @return the label associated with this component
     */
    public Label getLabelForComponent() {
        return componentLabel;
    }

    /**
     * This method is useful since it is not a part of the public API yet
     * allows a component within this package to observe focus events
     * without implementing a public interface or creating a new class
     */
    void focusGainedInternal() {
        startComponentLableTicker();
    }


    void startComponentLableTicker() {
        if (componentLabel != null && componentLabel.isTickerEnabled()) {
            if (componentLabel.shouldTickerStart()) {
                componentLabel.startTicker(getUIManager().getLookAndFeel().getTickerSpeed(), true);
            }
        }
    }

    void stopComponentLableTicker() {
        if (componentLabel != null && componentLabel.isTickerEnabled() && componentLabel.isTickerRunning()) {
            componentLabel.stopTicker();
        }
    }

    /**
     * Callback allowing a developer to track wheh the component gains focus
     */
    protected void focusGained() {
    }

    /**
     * Callback allowing a developer to track wheh the component loses focus
     */
    protected void focusLost() {
    }

    /**
     * This method is useful since it is not a part of the public API yet
     * allows a component within this package to observe focus events
     * without implementing a public interface or creating a new class
     */
    void focusLostInternal() {
        stopComponentLableTicker();
    }

    /**
     * This method paints all the parents Components Background.
     * 
     * @param g the graphics object
     */
    public void paintBackgrounds(Graphics g) {
        if(Display.impl.shouldPaintBackground()) {
            drawPainters(g, this.getParent(), this, getAbsoluteX() + getScrollX(),
                    getAbsoluteY() + getScrollY(),
                    getWidth(), getHeight());
        }
    }

    /**
     * Returns the absolute X location based on the component hierarchy, this method
     * calculates a location on the screen for the component rather than a relative
     * location as returned by getX()
     * 
     * @return the absolute x location of the component
     * @see #getX
     */
    public int getAbsoluteX() {
        int x = getX() - getScrollX();
        Container parent = getParent();
        if (parent != null) {
            x += parent.getAbsoluteX();
        }
        return x;
    }

    /**
     * Returns the absolute Y location based on the component hierarchy, this method
     * calculates a location on the screen for the component rather than a relative
     * location as returned by getX()
     * 
     * @return the absolute y location of the component
     * @see #getY
     */
    public int getAbsoluteY() {
        int y = getY() - getScrollY();
        Container parent = getParent();
        if (parent != null) {
            y += parent.getAbsoluteY();
        }
        return y;
    }

    /**
     * This method performs the paint of the component internally including drawing
     * the scrollbars and scrolling the component. This functionality is hidden
     * from developers to prevent errors
     * 
     * @param g the component graphics
     */
    final void paintInternal(Graphics g) {
        paintInternal(g, true);
    }

    final void paintInternal(Graphics g, boolean paintIntersects) {
        Display d = Display.getInstance();
        CodenameOneImplementation impl = d.getImplementation();
        if (!isVisible()) {
            return;
        }

        if(paintLockImage != null) {
            if(paintLockImage instanceof Image) {
                Image i = (Image)paintLockImage;
                g.drawImage(i, getX(), getY());
            } else {
                Image i = (Image)d.extractHardRef(paintLockImage);
                if(i == null) {
                    i = Image.createImage(getWidth(), getHeight());
                    int x = getX();
                    int y = getY();
                    setX(0);
                    setY(0);
                    paintInternalImpl(i.getGraphics(), paintIntersects);
                    setX(x);
                    setY(y);
                    paintLockImage = d.createSoftWeakRef(i);
                }
                g.drawImage(i, getX(), getY());
            }
            return;
        }
        impl.beforeComponentPaint(this, g);
        paintInternalImpl(g, paintIntersects);
        impl.afterComponentPaint(this, g);
    }

    protected boolean isInClippingRegion(Graphics g) {
        int oX = g.getClipX();
        int oY = g.getClipY();
        int oWidth = g.getClipWidth();
        int oHeight = g.getClipHeight();
        return bounds.intersects(oX, oY, oWidth, oHeight);
    }
    
    private void paintInternalImpl(Graphics g, boolean paintIntersects) {
        int oX = g.getClipX();
        int oY = g.getClipY();
        int oWidth = g.getClipWidth();
        int oHeight = g.getClipHeight();
        if (bounds.intersects(oX, oY, oWidth, oHeight)) {
            Style s = getStyle();
            if(s.getOpacity() < 255 && g.isAlphaSupported()) {
                int oldAlpha = g.getAlpha();
                g.setAlpha(s.getOpacity());
                internalPaintImpl(g, paintIntersects);
                g.setAlpha(oldAlpha);
            } else {
                internalPaintImpl(g, paintIntersects);
            }

            g.setClip(oX, oY, oWidth, oHeight);
        } else {
            Display.impl.nothingWithinComponentPaint(this);
        }
    }

    void internalPaintImpl(Graphics g, boolean paintIntersects) {
        g.clipRect(getX(), getY(), getWidth(), getHeight());
        paintComponentBackground(g);

        if (isScrollable()) {
            if(refreshTask != null && (draggedMotionY == null || getClientProperty("$pullToRelease") != null)){
                paintPullToRefresh(g);
            }
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            g.translate(-scrollX, -scrollY);
            paint(g);
            g.translate(scrollX, scrollY);
            if (isScrollVisible) {
                paintScrollbars(g);
            }
        } else {
            paint(g);
        }
        if (isBorderPainted()) {
            paintBorder(g);
        }

        //paint all the intersecting Components above the Component
        if (paintIntersects && parent != null) {
            paintIntersectingComponentsAbove(g);
        }
    }

    private void paintIntersectingComponentsAbove(Graphics g) {
        Container parent = getParent();
        Component component = this;
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();

        g.translate(-tx, -ty);
        while (parent != null) {
            g.translate(parent.getAbsoluteX() + parent.getScrollX(),
                    parent.getAbsoluteY() + parent.getScrollY());
            parent.paintIntersecting(g, component, getAbsoluteX() + getScrollX(),
                    getAbsoluteY() + getScrollY(),
                    getWidth(), getHeight(), true);
            g.translate(-parent.getAbsoluteX() - parent.getScrollX(),
                    -parent.getAbsoluteY() - parent.getScrollY());
            component = parent;
            parent = parent.getParent();
        }
        g.translate(tx, ty);

    }

    /**
     * Paints the UI for the scrollbars on the component, this will be invoked only
     * for scrollable components. This method invokes the appropriate X/Y versions
     * to do all the work.
     * 
     * @param g the component graphics
     */
    protected void paintScrollbars(Graphics g) {
        if (isScrollableX()) {
            paintScrollbarX(g);
        }
        if (isScrollableY()) {
            paintScrollbarY(g);
        }
    }
    
    private void paintPullToRefresh(Graphics g) {
        if (!dragActivated && scrollY == -getUIManager().getLookAndFeel().getPullToRefreshHeight()
                && getClientProperty("$pullToRelease") != null
                && getClientProperty("$pullToRelease").equals("update")) {

            putClientProperty("$pullToRelease", "updating");
            draggedMotionY = null;
            //execute the task
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    refreshTask.run();
                    //once the task has finished scroll to 0
                    startTensile(scrollY, 0, true);
                    putClientProperty("$pullToRelease", null);                    
                }
            });
        }
        boolean updating = getClientProperty("$pullToRelease") != null
                && getClientProperty("$pullToRelease").equals("updating");
        getUIManager().getLookAndFeel().drawPullToRefresh(g, this, updating);
    }


    /**
     * Paints the UI for the scrollbar on the X axis, this method allows component
     * subclasses to customize the look of a scrollbar
     * 
     * @param g the component graphics
     */
    protected void paintScrollbarX(Graphics g) {
        float scrollW = getScrollDimension().getWidth();
        float block = ((float) getWidth()) / scrollW;
        float offset;
        if(getScrollX() + getWidth() == scrollW) {
            // normalize the offset to avoid rounding errors to the bottom of the screen
            offset = 1 - block;
        } else {
            offset = (((float) getScrollX() + getWidth()) / scrollW) - block;
        }
        getUIManager().getLookAndFeel().drawHorizontalScroll(g, this, offset, block);
    }

    /**
     * This method is used internally by the look and feel to implement the fading scrollbar
     * behavior.
     * 
     * @return the opacity of the scrollbar
     */
    public int getScrollOpacity() {
        if(Display.getInstance().shouldRenderSelection()) {
            scrollOpacity = 0xff;
        }
        return scrollOpacity;
    }

    /**
     * Returns the component bounds with absolute screen coordinates, for components that include an internal selection behavior
     * and are not containers (currently only List) this method allows returning the position of the selection
     * itself which is useful for things such as the popup dialog and similar UI's that need to reference the
     * position of the selection externally
     *
     * @return the bounds of the component with absolute screen coordinates
     */
    public Rectangle getSelectedRect() {
        return new Rectangle(getAbsoluteX(), getAbsoluteY(), bounds.getSize());
    }

    /**
     * Paints the UI for the scrollbar on the Y axis, this method allows component
     * subclasses to customize the look of a scrollbar
     * 
     * @param g the component graphics
     */
    protected void paintScrollbarY(Graphics g) {
        float scrollH = getScrollDimension().getHeight();
        float block = ((float) getHeight()) / scrollH;
        float offset;
        if(getScrollY() + getHeight() == scrollH) {
            // normalize the offset to avoid rounding errors to the bottom of the screen
            offset = 1 - block;
        } else {
            offset = (((float) getScrollY() + getHeight()) / scrollH) - block;
        }
        getUIManager().getLookAndFeel().drawVerticalScroll(g, this, offset, block);
    }

    /**
     * <p>Paints this component as a root by going to all the parent components and
     * setting the absolute translation based on coordinates and scroll status.
     * Restores translation when the painting is finished.<br>
     * One of the uses of this method is to create a "screenshot" as is demonstrated in the code below
     * that creates an image for sharing on social media</p>
     * <script src="https://gist.github.com/codenameone/6bf5e68b329ae59a25e3.js"></script>
     * 
     * @param g the graphics to paint this Component on
     */
    final public void paintComponent(Graphics g) {
        repaintPending = false;
        paintComponent(g, true);
    }

    /**
     * <p>Paints this component as a root by going to all the parent components and
     * setting the absolute translation based on coordinates and scroll status.
     * Restores translation when the painting is finished.<br>
     * One of the uses of this method is to create a "screenshot" as is demonstrated in the code below
     * that creates an image for sharing on social media</p>
     * <script src="https://gist.github.com/codenameone/6bf5e68b329ae59a25e3.js"></script>
     * 
     * 
     * @param g the graphics to paint this Component on
     * @param background if true paints all parents background
     */
    final public void paintComponent(Graphics g, boolean background) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        //g.pushClip();
        Container parent = getParent();
        int translateX = 0;
        int translateY = 0;
        while (parent != null) {
            translateX += parent.getX();
            translateY += parent.getY();
            //if (parent.isScrollable()) {
            if (parent.isScrollableX()) {
                translateX -= parent.getScrollX();
            }
            if (parent.isScrollableY()) {
                translateY -= parent.getScrollY();
            }
            // since scrollability can translate everything... we should clip based on the
            // current scroll
            int parentX = parent.getAbsoluteX() + parent.getScrollX();
            if (isRTL()) {
                parentX += parent.getSideGap();
            }
            g.clipRect(parentX, parent.getAbsoluteY() + parent.getScrollY(),
                    parent.getWidth() - parent.getSideGap(), parent.getHeight() - parent.getBottomGap());

            parent = parent.getParent();
        }
        
        g.clipRect(translateX + getX(), translateY + getY(), getWidth(), getHeight());
        if (background) {
            paintBackgrounds(g);
        }
        
        
        g.translate(translateX, translateY);
        paintInternal(g);
        g.translate(-translateX, -translateY);

        paintGlassImpl(g);
        
        g.setClip(clipX, clipY, clipW, clipH);
        //g.popClip();
    }

    /**
     * This method can be overriden by a component to draw on top of itself or its children
     * after the component or the children finished drawing in a similar way to the glass
     * pane but more refined per component
     *
     * @param g the graphics context
     */
    void paintGlassImpl(Graphics g) {
        if(parent != null) {
            parent.paintGlassImpl(g);
        }
        if(tensileHighlightIntensity > 0) {
            int i = getScrollDimension().getHeight() - getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm());
            if(scrollY >= i - 1) {
                getUIManager().getLookAndFeel().paintTensileHighlight(this, g, false , tensileHighlightIntensity);
            } else {
                if(scrollY < 1) {
                    getUIManager().getLookAndFeel().paintTensileHighlight(this, g, true, tensileHighlightIntensity);
                } else {
                    tensileHighlightIntensity = 0;
                }
            }
        }
    }

    private void drawPainters(com.codename1.ui.Graphics g, Component par, Component c,
            int x, int y, int w, int h) {
        if(flatten && getWidth() > 0 && getHeight() > 0) {
            Image i = (Image)getClientProperty("$FLAT");
            int absX = getAbsoluteX() + getScrollX();
            int absY = getAbsoluteY() + getScrollY();
            if(i == null || i.getWidth() != getWidth() || i.getHeight() != getHeight()) {
                i = Image.createImage(getWidth(), getHeight());
                Graphics tg = i.getGraphics();
                //tg.translate(g.getTranslateX(), g.getTranslateY());
                drawPaintersImpl(tg, par, c, x, y, w, h);
                paintBackgroundImpl(tg);
                putClientProperty("$FLAT", i);
            }
            int tx = g.getTranslateX();
            int ty = g.getTranslateY();
            g.translate(-tx + absX, -ty + absY);
            g.drawImage(i, 0, 0);
            g.translate(tx - absX, ty - absY);
            return;
        }
        drawPaintersImpl(g, par, c, x, y, w, h);
    }

    private void drawPaintersImpl(com.codename1.ui.Graphics g, Component par, Component c,
            int x, int y, int w, int h) {
        if (par == null) {
            return;
        } else {
            if (par.getStyle().getBgTransparency() != ((byte) 0xFF)) {
                drawPainters(g, par.getParent(), par, x, y, w, h);
            }
        }

        if (!par.isVisible()) {
            return;
        }

        int transX = par.getAbsoluteX() + par.getScrollX();
        int transY = par.getAbsoluteY() + par.getScrollY();

        g.translate(transX, transY);

        ((Container) par).paintIntersecting(g, c, x, y, w, h, false);

        if (par.isBorderPainted()) {
            Border b = par.getBorder();
            if (b.isBackgroundPainter()) {
                g.translate(-par.getX(), -par.getY());
                par.paintBorderBackground(g);
                par.paintBorder(g);
                g.translate(par.getX() - transX, par.getY() - transY);
                return;
            }
        }
        Painter p = par.getStyle().getBgPainter();
        if (p != null) {
            Rectangle rect;
            if (painterBounds == null) {
                painterBounds = new Rectangle(0, 0, par.getWidth(), par.getHeight());
                rect = painterBounds;
            } else {
                rect = painterBounds;
                rect.getSize().setWidth(par.getWidth());
                rect.getSize().setHeight(par.getHeight());
            }
            p.paint(g, rect);
        }
        par.paintBackground(g);
        g.translate(-transX, -transY);
    }

    /**
     * Normally returns getStyle().getBorder() but some subclasses might use this 
     * to programmatically replace the border in runtime e.g. for a pressed border effect
     * 
     * @return the border that is drawn according to the current component state
     */
    protected Border getBorder() {
        return getStyle().getBorder();
    }

    /**
     * Paints the background of the component, invoked with the clipping region
     * and appropriate scroll translation.
     * 
     * @param g the component graphics
     */
    void paintComponentBackground(Graphics g) {
        if(isFlatten()) {
            return;
        }
        paintBackgroundImpl(g);
    }

    /**
     * Returns the scrollable parent of this component
     * @return the component itself or its parent which is scrollable
     */
    public Component getScrollable() {
        if(isScrollable()) {
            return this;
        }
        Component p = getParent();
        if(p == null) {
            return null;
        }
        return p.getScrollable();
    }
    
    /**
     * Returns the scrollable parent of this component
     */
    private Component getScrollableFast() {
        if(scrollableXFlag() || scrollableYFlag()) {
            return this;
        }
        Component p = getParent();
        if(p == null) {
            return null;
        }
        return p.getScrollableFast();
    }

    private void paintBackgroundImpl(Graphics g) {
        if (isBorderPainted()) {
            Border b = getBorder();
            if (b != null && b.isBackgroundPainter()) {
                b.paintBorderBackground(g, this);
                return;
            }
        }
        if (getStyle().getBgPainter() != null) {
            getStyle().getBgPainter().paint(g, bounds);
        }
        paintBackground(g);
    }
    
    /**
     * This method paints the Component background, it should be overriden
     * by subclasses to perform custom background drawing.
     * 
     * @param g the component graphics
     */
    protected void paintBackground(Graphics g) {
    }
    
    /**
     * This method paints the Component on the screen, it should be overriden
     * by subclasses to perform custom drawing or invoke the UI API's to let
     * the PLAF perform the rendering.
     * 
     * @param g the component graphics
     */
    public void paint(Graphics g) {
    }

    /**
     * Indicates whether the component should/could scroll by default a component
     * is not scrollable.
     * 
     * @return whether the component is scrollable
     */
    protected boolean isScrollable() {
        return isScrollableX() || isScrollableY();
    }

    /**
     * Indicates whether the component should/could scroll on the X axis
     * 
     * @return whether the component is scrollable on the X axis
     */
    public boolean isScrollableX() {
        return false;
    }

    /**
     * Indicates whether the component should/could scroll on the Y axis
     * 
     * @return whether the component is scrollable on the X axis
     */
    public boolean isScrollableY() {
        return false;
    }

    boolean scrollableXFlag() {
        return isScrollableX();
    }

    boolean scrollableYFlag() {
        return isScrollableY();
    }

    /**
     * Indicates the X position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @return the X position of the scrolling
     */
    public int getScrollX() {
        return scrollX;
    }

    /**
     * Indicates the Y position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @return the Y position of the scrolling
     */
    public int getScrollY() {
        return scrollY;
    }

    /**
     * This method can be overriden to receive scroll events, unlike overriding setScrollX 
     * it will receive all calls for scrolling. Normally you should not override this method
     * and try to find a more creative solution since scrolling is very specific to platform
     * behavior.
     * @param scrollX the X position of the scrolling
     */
    protected void onScrollX(int scrollX) {
    }
    
    /**
     * This method can be overriden to receive scroll events, unlike overriding setScrollY
     * it will receive all calls for scrolling. Normally you should not override this method
     * and try to find a more creative solution since scrolling is very specific to platform
     * behavior.
     * @param scrollY the Y position of the scrolling
     */
    protected void onScrollY(int scrollY) {
    }
    
    /**
     * Indicates the X position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @param scrollX the X position of the scrolling
     */
    protected void setScrollX(int scrollX) {
        // the setter must always update the value regardless...
        int scrollXtmp = scrollX;
        if(!isSmoothScrolling() || !isTensileDragEnabled()) {
            scrollXtmp = Math.min(scrollXtmp, getScrollDimension().getWidth() - getWidth());
            scrollXtmp = Math.max(scrollXtmp, 0);
        }
        if (isScrollableX()) {
            if(Form.activePeerCount > 0) {
                onParentPositionChange();
            }
            repaint();
        }
        if(scrollListeners != null){
            scrollListeners.fireScrollEvent(scrollXtmp, this.scrollY, this.scrollX, this.scrollY);
        }
        this.scrollX = scrollXtmp;
        onScrollX(scrollX);
    }
    
    void resetScroll() {
        if(scrollListeners != null){
            if(scrollX != 0 || scrollY != 0){
                scrollListeners.fireScrollEvent(0, 0, this.scrollX, this.scrollY);
            }
        }
        scrollX = 0;
        scrollY = 0;
    }

    /**
     * Indicates the X position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @param scrollY the Y position of the scrolling
     */
    protected void setScrollY(int scrollY) {
        if(this.scrollY != scrollY) {
            CodenameOneImplementation ci = Display.impl;
            if(ci.isAsyncEditMode() && ci.isEditingText()) {
                ci.hideTextEditor();
            }
        }
        // the setter must always update the value regardless... 
        int scrollYtmp = scrollY;
        if(!isSmoothScrolling() || !isTensileDragEnabled()) {
            Form parentForm = getComponentForm();
            int v = Form.getInvisibleAreaUnderVKB(parentForm);
            int h = getScrollDimension().getHeight() - getHeight() + v;
            scrollYtmp = Math.min(scrollYtmp, h);
            scrollYtmp = Math.max(scrollYtmp, 0);
        }
        if (isScrollableY()) {
            if(Form.activePeerCount > 0) {
                onParentPositionChange();
            }
            repaint();
        }
        if(scrollListeners != null){
            scrollListeners.fireScrollEvent(this.scrollX, scrollYtmp, this.scrollX, this.scrollY);
        }
        this.scrollY = scrollYtmp;
        onScrollY(this.scrollY);
    }

    /**
     * Gets the current dragged x values when the Component is being dragged
     * @return dragged x value
     */
    public int getDraggedx() {
        return draggedx;
    }

    /**
     * Gets the current dragged y values when the Component is being dragged
     * @return dragged y value
     */
    public int getDraggedy() {
        return draggedy;
    }

    
    
    private void updateTensileHighlightIntensity(int lastScroll, int scroll, boolean motion) {
        if(tensileHighlightEnabled) {
            int h = getScrollDimension().getHeight() - getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm());
            if(h <= 0) {
                // layout hasn't completed yet
                tensileHighlightIntensity = 0;
                return;
            }
            if(h > this.scrollY) {
                if(this.scrollY < 0) {
                    if(scroll > lastScroll || motion) {
                        tensileHighlightIntensity = 255;
                    }
                }
            } else {
                if(lastScroll > scroll || motion) {
                    tensileHighlightIntensity = 255;
                }
            }
        }
    }

    /**
     * Returns the gap to be left for the bottom scrollbar on the X axis. This
     * method is used by layout managers to determine the room they should
     * leave for the scrollbar
     * 
     * @return the gap to be left for the bottom scrollbar on the X axis
     */
    public int getBottomGap() {
        if (isScrollableX() && isScrollVisible()) {
            return getUIManager().getLookAndFeel().getHorizontalScrollHeight();
        }
        return 0;
    }

    /**
     * Returns the gap to be left for the side scrollbar on the Y axis. This
     * method is used by layout managers to determine the room they should
     * leave for the scrollbar. (note: side scrollbar rather than left scrollbar
     * is used for a future version that would support bidi).
     * 
     * @return the gap to be left for the side scrollbar on the Y axis
     */
    public int getSideGap() {
        if (isScrollableY() && isScrollVisible()) {
            return getUIManager().getLookAndFeel().getVerticalScrollWidth();
        }
        return 0;
    }

    /**
     * Returns true if the given absolute coordinate is contained in the Component
     * 
     * @param x the given absolute x coordinate
     * @param y the given absolute y coordinate
     * @return true if the given absolute coordinate is contained in the 
     * Component; otherwise false
     */
    public boolean contains(int x, int y) {
        int absX = getAbsoluteX() + getScrollX();
        int absY = getAbsoluteY() + getScrollY();
        return (x >= absX && x < absX + getWidth() && y >= absY && y < absY + getHeight());
    }

    /**
     * Calculates the preferred size based on component content. This method is
     * invoked lazily by getPreferred size.
     * 
     * @return the calculated preferred size based on component content
     */
    protected Dimension calcPreferredSize() {
        Dimension d = new Dimension(0, 0);
        return d;
    }

    private Dimension preferredSizeImpl() {
        if (!sizeRequestedByUser && (shouldCalcPreferredSize || preferredSize == null)) {
            shouldCalcPreferredSize = false;
            if(hideInPortrait && Display.getInstance().isPortrait()) {
                preferredSize = new Dimension(0, 0);
            } else {
                preferredSize = calcPreferredSize();
            }
        }
        return preferredSize;
    }
    
    private Dimension preferredSize() {
        if(sameWidth != null || sameHeight != null) {
            if (!sizeRequestedByUser && (shouldCalcPreferredSize || preferredSize == null)) {
                if(sameWidth != null) {
                    int w = -1;
                    for(Component c : sameWidth) {
                        int d = c.preferredSizeImpl().getWidth();
                        if(w < d) {
                            w = d;
                        }
                    }
                    for(Component c : sameWidth) {
                        c.preferredSizeImpl().setWidth(w);
                    }
                }
                if(sameHeight != null) {
                    int h = -1;
                    for(Component c : sameHeight) {
                        int d = c.preferredSizeImpl().getHeight();
                        if(h < d) {
                            h = d;
                        }
                    }
                    for(Component c : sameHeight) {
                        c.preferredSizeImpl().setHeight(h);
                    }
                }
            }
        }
        return preferredSizeImpl();
    }

    /**
     * Returns the component bounds which is sometimes more convenient than invoking
     * getX/Y/Width/Height. Bounds are relative to parent container.<br>
     * Changing values within the bounds can lead to unpredicted behavior.
     * 
     * @see #getX
     * @see #getY
     * @return the component bounds
     */
    protected Rectangle getBounds() {
        return bounds;
    }

    /**
     * Returns the component bounds for scrolling which might differ from the getBounds for large components
     * e.g. list.
     *
     * @see #getX
     * @see #getY
     * @return the component bounds
     */
    protected Rectangle getVisibleBounds() {
        return bounds;
    }

    /**
     * Returns true if this component can receive focus and is enabled
     * 
     * @return true if this component can receive focus; otherwise false
     */
    public boolean isFocusable() {
        return focusable && enabled && isVisible();
    }

    /**
     * Restores the state of the focusable flag to its default state
     */
    protected void resetFocusable() {
        setFocusable(false);
    }
    
    /**
     * A simple setter to determine if this Component can get focused
     * 
     * @param focusable indicate whether this component can get focused
     */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /**
     * Indicates the values within the component have changed and preferred 
     * size should be recalculated
     * 
     * @param shouldCalcPreferredSize indicate whether this component need to 
     * recalculate his preferred size
     */
    public void setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
        if (!shouldCalcScrollSize) {
            this.shouldCalcScrollSize = shouldCalcPreferredSize;
        }
        if (shouldCalcPreferredSize != this.shouldCalcPreferredSize) {
            this.shouldCalcPreferredSize = shouldCalcPreferredSize;
            this.shouldCalcScrollSize = shouldCalcPreferredSize;
            if (shouldCalcPreferredSize && getParent() != null) {
                this.shouldCalcPreferredSize = shouldCalcPreferredSize;
                getParent().setShouldLayout(shouldCalcPreferredSize);
            }
        }
        if(shouldCalcPreferredSize) {
            setShouldCalcPreferredSizeGroup(sameWidth);
            setShouldCalcPreferredSizeGroup(sameHeight);
        }
    }

    private void setShouldCalcPreferredSizeGroup(Component[] cmps) {
        if(cmps != null) {
            for(Component c : cmps) {
                c.shouldCalcPreferredSize = true;
            }
        }
    }

    /**
     * Prevents key events from being grabbed for focus traversal. E.g. a list component
     * might use the arrow keys for internal navigation so it will switch this flag to
     * true in order to prevent the focus manager from moving to the next component.
     * 
     * @return true if key events are being used for focus traversal
     * ; otherwise false
     */
    public boolean handlesInput() {
        return handlesInput;
    }

    /**
     * Prevents key events from being grabbed for focus traversal. E.g. a list component
     * might use the arrow keys for internal navigation so it will switch this flag to
     * true in order to prevent the focus manager from moving to the next component.
     * 
     * @param handlesInput indicates whether key events can be grabbed for 
     * focus traversal
     */
    public void setHandlesInput(boolean handlesInput) {
        this.handlesInput = handlesInput;
    }

    /**
     * Returns true if the component has focus
     * 
     * @return true if the component has focus; otherwise false
     * @see #requestFocus
     */
    public boolean hasFocus() {
        return focused;
    }

    /**
     * This flag doesn't really give focus, its a state that determines
     * what colors from the Style should be used when painting the component.
     * Actual focus is determined by the parent form
     * 
     * @param focused sets the state that determines what colors from the 
     * Style should be used when painting a focused component
     * 
     * @see #requestFocus
     * @deprecated this method shouldn't be invoked by user code, use requestFocus() instead
     */
    public void setFocus(boolean focused) {
        this.focused = focused;
    }

    /**
     * Returns the Component Form or null if this Component
     * is not added yet to a form
     * 
     * @return the Component Form
     */
    public Form getComponentForm() {
        Form retVal = null;
        Component parent = getParent();
        if (parent != null) {
            retVal = parent.getComponentForm();
        }
        return retVal;
    }

   
    /**
     * Repaint the given component to the screen
     * 
     * @param cmp the given component on the screen
     */
    void repaint(Component cmp) {
        if (isCellRenderer() || cmp.getWidth() <= 0 || cmp.getHeight() <= 0 || paintLockImage != null) {
            return;
        }
        // null parent repaint can happen when a component is removed and modified which
        // is common for a popup
        Component parent = getParent();
        
        if (parent != null && parent.isVisible()) {
            parent.repaint(cmp);
        }
    }

    /**
     * Repaint this Component, the repaint call causes a callback of the paint
     * method on the event dispatch thread.
     * 
     * @see Display
     */
    public void repaint() {
        repaintPending = true;
        if (dirtyRegion != null) {
            setDirtyRegion(null);
        }
        repaint(this);
    }

    /**
     * Repaints a specific region within the component
     * 
     * @param x boundary of the region to repaint in absolute screen coordinates not component coordinates
     * @param y boundary of the region to repaint in absolute screen coordinates not component coordinates
     * @param w boundary of the region to repaint
     * @param h boundary of the region to repaint
     */
    public void repaint(int x, int y, int w, int h) {
        Rectangle rect;
        synchronized (dirtyRegionLock) {
            if (dirtyRegion == null) {
                if(repaintPending) {
                    return;
                }
                rect = new Rectangle(x, y, w, h);
                setDirtyRegion(rect);
            } else if (dirtyRegion.getX() != x || dirtyRegion.getY() != y ||
                    dirtyRegion.getSize().getWidth() != w || dirtyRegion.getSize().getHeight() != h) {
                rect = new Rectangle(dirtyRegion);
                Dimension size = rect.getSize();

                int x1 = Math.min(rect.getX(), x);
                int y1 = Math.min(rect.getY(), y);

                int x2 = Math.max(x + w, rect.getX() + size.getWidth());
                int y2 = Math.max(y + h, rect.getY() + size.getHeight());

                rect.setX(x1);
                rect.setY(y1);
                size.setWidth(x2 - x1);
                size.setHeight(y2 - y1);
                setDirtyRegion(rect);
            }
        }

        repaint(this);
    }

    /**
     * If this Component is focused this method is invoked when the user presses
     * and holds the key
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    protected void longKeyPress(int keyCode) {
    }

    /**
     * If this Component is focused, the key pressed event
     * will call this method
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    public void keyPressed(int keyCode) {
    }

    /**
     * If this Component is focused, the key released event
     * will call this method
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    public void keyReleased(int keyCode) {
    }

    /**
     * If this Component is focused, the key repeat event
     * will call this method.
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    public void keyRepeated(int keyCode) {
        keyPressed(keyCode);
        keyReleased(keyCode);
    }

    /**
     * Allows defining the physics for the animation motion behavior directly 
     * by plugging in an alternative motion object
     * 
     * @param motion new motion object
     */
    private void setAnimationMotion(Motion motion) {
        animationMotion = motion;
    }

    /**
     * Allows defining the physics for the animation motion behavior directly 
     * by plugging in an alternative motion object
     * 
     * @return the component motion object
     */
    private Motion getAnimationMotion() {
        return animationMotion;
    }

    /**
     * Returns the animation manager of the parent form or null if this component isn't currently associated with a form
     * @return the animation manager instance
     */
    public AnimationManager getAnimationManager() {
        Form f = getComponentForm();
        if(f == null) {
            return null;
        }
        return f.getAnimationManager();
    }

    /**
     * Scroll animation speed in milliseconds allowing a developer to slow down or accelerate
     * the smooth animation mode
     * 
     * @return scroll animation speed in milliseconds
     */
    public int getScrollAnimationSpeed() {
        return animationSpeed;
    }

    class AnimationTransitionPainter implements Painter{
        int alpha;
        Style originalStyle;
        Style destStyle;
        Painter original;
        Painter dest;

        public void paint(Graphics g, Rectangle rect) {
            int oAlpha = g.getAlpha();
            if(alpha == 0) {
                unSelectedStyle = originalStyle;
                original.paint(g, rect);
                return;
            }
            if(alpha == 255) {
                unSelectedStyle = destStyle;
                dest.paint(g, rect);
                unSelectedStyle = originalStyle;
                return;
            }
            int opa = unSelectedStyle.getBgTransparency() & 0xff;
            unSelectedStyle.setBgTransparency(255 - alpha);
            g.setAlpha(255 - alpha);
            original.paint(g, rect);
            unSelectedStyle.setBgTransparency(opa);
            unSelectedStyle = destStyle;
            opa = unSelectedStyle.getBgTransparency() & 0xff;
            g.setAlpha(alpha);
            unSelectedStyle.setBgTransparency(alpha);
            dest.paint(g, rect);
            unSelectedStyle.setBgTransparency(opa);
            unSelectedStyle = originalStyle;
            g.setAlpha(oAlpha);
        }        
    }
    
    /**
     * Creates an animation that will transform the current component to the styling of the destination UIID when
     * completed. Notice that fonts will only animate within the truetype and native familiy and we recommend that you
     * don't shift weight/typeface/style as this might diminish the effect.<br>
     * <b>Important: </b> Only unselected styles are animated but once the animation completes all styles are applied.
     * @param destUIID the UIID to which this component will gradually shift
     * @param duration the duration of the animation or the number of steps
     * @return an animation component that can either be stepped or played
     */
    public ComponentAnimation createStyleAnimation(final String destUIID, final int duration) {
        final Style sourceStyle = getUnselectedStyle();
        final Style destStyle = getUIManager().getComponentStyle(destUIID);
        
        int d = duration;
        
        Motion m = null;
        if(sourceStyle.getFgColor() != destStyle.getFgColor()) {
            m = Motion.createLinearColorMotion(sourceStyle.getFgColor(), destStyle.getFgColor(), d);
        }
        final Motion fgColorMotion = m;
        m = null;
        
        if(sourceStyle.getFont().getHeight() != destStyle.getFont().getHeight() && sourceStyle.getFont().isTTFNativeFont()) {
            // allows for fractional font sizes
            m = Motion.createLinearMotion(sourceStyle.getFont().getHeight() * 100, destStyle.getFont().getHeight() * 100, d);
        }

        final Motion fontMotion = m;
        m = null;

        if(sourceStyle.getPadding(TOP) != destStyle.getPadding(TOP)) {
            m = Motion.createLinearMotion(sourceStyle.getPadding(TOP), destStyle.getPadding(TOP), d);
        }
        final Motion paddingTop = m;
        m = null;

        if(sourceStyle.getPadding(BOTTOM) != destStyle.getPadding(BOTTOM)) {
            m = Motion.createLinearMotion(sourceStyle.getPadding(BOTTOM), destStyle.getPadding(BOTTOM), d);
        }
        final Motion paddingBottom = m;
        m = null;

        if(sourceStyle.getPadding(LEFT) != destStyle.getPadding(LEFT)) {
            m = Motion.createLinearMotion(sourceStyle.getPadding(LEFT), destStyle.getPadding(LEFT), d);
        }
        final Motion paddingLeft = m;
        m = null;

        if(sourceStyle.getPadding(RIGHT) != destStyle.getPadding(RIGHT)) {
            m = Motion.createLinearMotion(sourceStyle.getPadding(RIGHT), destStyle.getPadding(RIGHT), d);
        }
        final Motion paddingRight = m;
        m = null;

        if(sourceStyle.getMargin(TOP) != destStyle.getMargin(TOP)) {
            m = Motion.createLinearMotion(sourceStyle.getMargin(TOP), destStyle.getMargin(TOP), d);
        }
        final Motion marginTop = m;
        m = null;

        if(sourceStyle.getMargin(BOTTOM) != destStyle.getMargin(BOTTOM)) {
            m = Motion.createLinearMotion(sourceStyle.getMargin(BOTTOM), destStyle.getMargin(BOTTOM), d);
        }
        final Motion marginBottom = m;
        m = null;

        if(sourceStyle.getMargin(LEFT) != destStyle.getMargin(LEFT)) {
            m = Motion.createLinearMotion(sourceStyle.getMargin(LEFT), destStyle.getMargin(LEFT), d);
        }
        final Motion marginLeft = m;
        m = null;

        if(sourceStyle.getMargin(RIGHT) != destStyle.getMargin(RIGHT)) {
            m = Motion.createLinearMotion(sourceStyle.getMargin(RIGHT), destStyle.getMargin(RIGHT), d);
        }
        final Motion marginRight = m;
        m = null;

        if(paddingLeft != null || paddingRight != null || paddingTop != null || paddingBottom != null) {
            // convert the padding to pixels for smooth animation
            int left = sourceStyle.getPadding(LEFT);
            int right = sourceStyle.getPadding(RIGHT);
            int top = sourceStyle.getPadding(TOP);
            int bottom = sourceStyle.getPadding(BOTTOM);
            sourceStyle.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
            sourceStyle.setPadding(top, bottom, left, right);
        }
        
        if(marginLeft != null || marginRight != null || marginTop != null || marginBottom != null) {
            // convert the margin to pixels for smooth animation
            int left = sourceStyle.getMargin(LEFT);
            int right = sourceStyle.getMargin(RIGHT);
            int top = sourceStyle.getMargin(TOP);
            int bottom = sourceStyle.getMargin(BOTTOM);
            sourceStyle.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
            sourceStyle.setMargin(top, bottom, left, right);
        }

        final AnimationTransitionPainter ap = new AnimationTransitionPainter();
        if(sourceStyle.getBgTransparency() != 0 || destStyle.getBgTransparency() != 0 ||
                (sourceStyle.getBorder() != null && sourceStyle.getBorder().isEmptyBorder()) || 
                (destStyle.getBorder() != null && destStyle.getBorder().isEmptyBorder()) || 
                sourceStyle.getBgImage() != null || destStyle.getBgImage() != null) {
            ap.original = sourceStyle.getBgPainter();
            ap.dest = destStyle.getBgPainter();
            ap.originalStyle = sourceStyle;
            ap.destStyle = destStyle;
            if(ap.dest == null) {
                ap.dest = new BGPainter();
            }
            sourceStyle.setBgPainter(ap);
        }
        
        final Motion bgMotion = Motion.createLinearMotion(0, 255, d);
        
        return new ComponentAnimation() {
            private boolean finished;
            private boolean stepMode;
            
            @Override
            public boolean isStepModeSupported() {
                return true;
            }

            @Override
            public int getMaxSteps() {
                return duration;
            }

            
            @Override
            public void setStep(int step) {
                stepMode = true;
                if(!finished) {
                    if(bgMotion != null) {
                        bgMotion.setCurrentMotionTime(step);
                    }
                    if(fgColorMotion != null) {
                        fgColorMotion.setCurrentMotionTime(step);
                    }
                    if(fontMotion != null) {
                        fontMotion.setCurrentMotionTime(step);
                    }
                    if(paddingTop != null) {
                        paddingTop.setCurrentMotionTime(step);
                    }
                    if(paddingBottom != null) {
                        paddingBottom.setCurrentMotionTime(step);
                    }
                    if(paddingLeft != null) {
                        paddingLeft.setCurrentMotionTime(step);
                    }
                    if(paddingRight != null) {
                        paddingRight.setCurrentMotionTime(step);
                    }
                    if(marginTop != null) {
                        marginTop.setCurrentMotionTime(step);
                    }
                    if(marginBottom != null) {
                        marginBottom.setCurrentMotionTime(step);
                    }
                    if(marginLeft != null) {
                        marginLeft.setCurrentMotionTime(step);
                    }
                    if(marginRight != null) {
                        marginRight.setCurrentMotionTime(step);
                    }
                }
                super.setStep(step);
            }
            
            @Override
            public boolean isInProgress() {
                return stepMode ||
                        !((bgMotion == null || bgMotion.isFinished()) && 
                        (fgColorMotion == null || fgColorMotion.isFinished()) &&
                        (paddingLeft == null || paddingLeft.isFinished()) &&
                        (paddingRight == null || paddingRight.isFinished()) &&
                        (paddingTop == null || paddingTop.isFinished()) &&
                        (paddingBottom == null || paddingBottom.isFinished()) &&
                        (marginLeft == null || marginLeft.isFinished()) &&
                        (marginRight == null || marginRight.isFinished()) &&
                        (marginTop == null || marginTop.isFinished()) &&
                        (marginBottom == null || marginBottom.isFinished()) &&
                        (fontMotion == null || fontMotion.isFinished()));
            }

            @Override
            protected void updateState() {
                if(finished) {
                    return;
                }
                                
                if(!isInProgress()) {
                    finished = true;
                    setUIID(destUIID);
                } else {
                    if(fgColorMotion != null) {
                        sourceStyle.setFgColor(fgColorMotion.getValue());
                    }
                    if(bgMotion != null) {
                        ap.alpha = bgMotion.getValue();
                    }
                    if(fontMotion != null) {
                        Font fnt = sourceStyle.getFont();
                        fnt = fnt.derive(((float)fontMotion.getValue()) / 100.0f, fnt.getStyle());
                        sourceStyle.setFont(fnt);
                    }
                    if(paddingTop != null) {
                        sourceStyle.setPadding(TOP, paddingTop.getValue());
                    }
                    if(paddingBottom != null) {
                        sourceStyle.setPadding(BOTTOM, paddingBottom.getValue());
                    }
                    if(paddingLeft != null) {
                        sourceStyle.setPadding(LEFT, paddingLeft.getValue());
                    }
                    if(paddingRight != null) {
                        sourceStyle.setPadding(RIGHT, paddingRight.getValue());
                    }
                    if(marginTop != null) {
                        sourceStyle.setMargin(TOP, marginTop.getValue());
                    }
                    if(marginBottom != null) {
                        sourceStyle.setMargin(BOTTOM, marginBottom.getValue());
                    }
                    if(marginLeft != null) {
                        sourceStyle.setMargin(LEFT, marginLeft.getValue());
                    }
                    if(marginRight != null) {
                        sourceStyle.setMargin(RIGHT, marginRight.getValue());
                    }
                }
            }

            @Override
            public void flush() {
                if(bgMotion != null) {
                    bgMotion.finish();
                }
                if(fgColorMotion != null) {
                    fgColorMotion.finish();
                }
                if(fontMotion != null) {
                    fontMotion.finish();
                }
                if(paddingTop != null) {
                    paddingTop.finish();
                }
                if(paddingBottom != null) {
                    paddingBottom.finish();
                }
                if(paddingLeft != null) {
                    paddingLeft.finish();
                }
                if(paddingRight != null) {
                    paddingRight.finish();
                }
                if(marginTop != null) {
                    marginTop.finish();
                }
                if(marginBottom != null) {
                    marginBottom.finish();
                }
                if(marginLeft != null) {
                    marginLeft.finish();
                }
                if(marginRight != null) {
                    marginRight.finish();
                }
                updateState();
            }
        };
    }
    
    /**
     * Scroll animation speed in milliseconds allowing a developer to slow down or accelerate
     * the smooth animation mode
     * 
     * @param animationSpeed scroll animation speed in milliseconds
     */
    public void setScrollAnimationSpeed(int animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    /**
     * Indicates that scrolling through the component should work as an animation
     * 
     * @return whether this component use smooth scrolling
     */
    public boolean isSmoothScrolling() {
        return smoothScrolling && !disableSmoothScrolling;
    }

    /**
     * Indicates that scrolling through the component should work as an animation
     * 
     * @param smoothScrolling indicates if a component uses smooth scrolling
     */
    public void setSmoothScrolling(boolean smoothScrolling) {
        this.smoothScrolling = smoothScrolling;
    }

    /**
     * Disable smooth scrolling on all components
     * @param disableSmoothScrolling 
     */
    static void setDisableSmoothScrolling(boolean disableSmoothScrolling) {
        Component.disableSmoothScrolling = disableSmoothScrolling;
    }
    
    /**
     * Invoked for devices where the pointer can hover without actually clicking
     * the display. This is true for PC mouse pointer as well as some devices such
     * as the BB storm.
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerHover(int[] x, int[] y) {
        pointerDragged(x, y);
    }

    void clearDrag() {
        //if we are in the middle of a tensile animation reset the scrolling location
        //before killing the scrolling
        if (draggedMotionX != null) {
            if (draggedMotionX.getValue() < 0) {
                setScrollX(0);
            } else if (draggedMotionX.getValue() > getScrollDimension().getWidth() - getWidth()) {
                setScrollX(getScrollDimension().getWidth() - getWidth());
            }
        }
        if (draggedMotionY != null) {
            int dmv = draggedMotionY.getValue();
            if (dmv < 0) {
                setScrollY(0);
            } else {
                int hh = getScrollDimension().getHeight() - getHeight();
                if (dmv > hh) {
                    if(hh < 0) {
                        setScrollY(0);
                    } else {
                        setScrollY(hh);
                    }
                }
            }
        }
        draggedMotionX = null;
        draggedMotionY = null;        
        
        Component parent = getParent();
        if(parent != null){
            parent.clearDrag();
        }
        if (getClientProperty("$pullToRelease") != null
                && !getClientProperty("$pullToRelease").equals("updating")) {
            putClientProperty("$pullToRelease", null);
        }
    }

    /**
     * Invoked for devices where the pointer can hover without actually clicking
     * the display. This is true for PC mouse pointer as well as some devices such
     * as the BB storm.
     *
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerHoverReleased(int[] x, int[] y) {
        pointerReleaseImpl(x[0], y[0]);
    }

    /**
     * Invoked for devices where the pointer can hover without actually clicking
     * the display. This is true for PC mouse pointer as well as some devices such
     * as the BB storm.
     *
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerHoverPressed(int[] x, int[] y) {
        dragActivated = false;
        clearDrag();
    }
    
    /**
     * Invoked by subclasses interested in handling pinch to zoom events, if true is returned 
     * other drag events will not be broadcast
     * 
     * @param scale the scaling of the pinch operation a number larger than 1 means scaling up and smaller than 1 means scaling down.
     * It is recommended that code would threshold the number (so a change between 1.0 and 1.02 shouldn't necessarily trigger zoom).
     * Notice that this number is relevant to current zoom levels and unaware of them so you should also enforce limits of maximum/minimum
     * zoom levels.
     * @return false by default
     */
    protected boolean pinch(float scale) {
        return false;
    }

    private double distance(int[] x, int[] y) {
        int disx = x[0] - x[1];
        int disy = y[0] - y[1];
        return Math.sqrt(disx * disx + disy * disy);
    }

    /**
     * If this Component is focused, the pointer dragged event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerDragged(int[] x, int[] y) {
        if (x.length > 1) {
            double currentDis = distance(x, y);

            // prevent division by 0
            if (pinchDistance <= 0) {
                pinchDistance = currentDis;
            }
            double scale = currentDis / pinchDistance;
            if (pinch((float)scale)) {
                return;
            }
        }
        pointerDragged(x[0], y[0]);
    }
    
    /**
     * This method returns an image representing the dragged component, it can be overriden by subclasses to customize the look
     * of the image, the image will be overlaid on top of the form during a drag and drop operation
     * 
     * @return an image
     */
    protected Image getDragImage() {
        Image draggedImage = Image.createImage(getWidth(), getHeight(),0x00ff7777);
        Graphics g = draggedImage.getGraphics();

        g.translate(-getX(), -getY());
        paintComponentBackground(g);
        paint(g);
        if (isBorderPainted()) {
            paintBorder(g);
        }
        g.translate(getX(), getY());

        // remove all occurences of the rare color
        draggedImage = draggedImage.modifyAlpha((byte)0x55, 0xff7777);
        return draggedImage;
    }

    /**
     * Invoked on the focus component to let it know that drag has started on the parent container
     * for the case of a component that doesn't support scrolling
     */
    protected void dragInitiated() {
    }

    void drawDraggedImage(Graphics g) {
        if(dragImage == null) {
            dragImage = getDragImage();
        }
        drawDraggedImage(g, dragImage, draggedx, draggedy);
    }

    /**
     * Draws the given image at x/y, this method can be overriden to draw additional information such as positive
     * or negative drop indication
     *
     * @param g the graphics context
     * @param img the image
     * @param x x position
     * @param y y position
     */
    protected void drawDraggedImage(Graphics g, Image img, int x, int y) {
//        g.drawImage(img, x - getWidth() / 2, y - getHeight() / 2);
        g.drawImage(img, x, y);
    }

    
    /**
     * This method allows a component to indicate if it is a drop target for the given component at the given x/y location
     * (in component coordiate space). This method can also update the drop tagets appearance to indicate the
     * drop location.
     * 
     * @param dragged the component being dragged
     * @param x the x location over the component
     * @param y the y location over the component
     * @return true if a drop at this location will be successful
     */
    protected boolean draggingOver(Component dragged, int x, int y) {
        return dropTarget;
    }

    /**
     * This callback method indicates that a component drag has just entered this component
     *
     * @param dragged the component being dragged
     */
    protected void dragEnter(Component dragged) {
    }

    /**
     * This callback method provides an indication for a drop target that a drag operation is exiting the bounds of
     * this component and it should clear all relevant state if such state exists. E.g. if a component provides
     * drop indication visuaization in draggingOver this visualization should be cleared..
     *
     * @param dragged the component being dragged
     */
    protected void dragExit(Component dragged) {
    }

    /**
     * Performs a drop operation of the component at the given X/Y location in coordinate space, this method
     * should be overriden by subclasses to perform all of the logic related to moving a component, by default
     * this method does nothing and so dragging a component and dropping it has no effect
     *
     * @param dragged the component being dropped
     * @param x the x coordinate of the drop
     * @param y the y coordinate of the drop
     */
    public void drop(Component dragged, int x, int y) {
    }

    /**
     * Finds the drop target in the given screen coordinates
     * 
     * @param source  the component being dragged
     * @param x the screen x coordinate
     * @param y the screen y coordinate
     * @return a component drop target or null if no drop target is available at that coordinate
     */
    private Component findDropTarget(Component source, int x, int y) {
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.findDropTargetAt(x, y);
            while(c != null) {
                if(c.isDropTarget() && c.draggingOver(source, x - c.getAbsoluteX() - c.getScrollX(), y- c.getAbsoluteY() - c.getScrollY())) {
                    return c;
                }
                c = c.getParent();
            }
        }
        return null;
    }

    /**
     * <p>This method adds a refresh task to the Component, the task will be 
     * executed if the user has pulled the scroll beyond a certain height.</p>
     * 
     * <script src="https://gist.github.com/codenameone/da87714157f97c739b2a.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/pull-to-refresh.png" alt="Simple pull to refresh demo" />
     * 
     * @param task the refresh task to execute.
     */ 
    public void addPullToRefresh(Runnable task){
        this.refreshTask = task;
    }
    
    /**
     * If this Component is focused, the pointer dragged event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerDragged(final int x, final int y) {
        Form p = getComponentForm();
        
        if (pointerDraggedListeners != null && pointerDraggedListeners.hasListeners()) {
            pointerDraggedListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerDrag, x, y));
        }
        
        if(dragAndDropInitialized) {
            //keep call to pointerDragged to move the parent scroll if needed
            if (dragCallbacks < 2) {
                dragCallbacks++;
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (dragActivated) {
                            pointerDragged(x, y);
                        }
                        dragCallbacks--;
                    }
                });
            }
            
            if (!dragActivated) {
                dragActivated = true;
                setVisible(false);
                p.setDraggedComponent(this);
                oldx = x;
                oldy = y;
                draggedx = getAbsoluteX();
                draggedy = getAbsoluteY();
            }
            Component dropTo = findDropTarget(this, x, y);
            if(dropTo != null && dragOverListener != null) {
                ActionEvent ev = new ActionEvent(this, dropTo, x, y);
                dragOverListener.fireActionEvent(ev);
                if(ev.isConsumed()) {
                    return;
                }
            }
            if(dropTargetComponent != dropTo) {
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragExit(this);
                }
                dropTargetComponent = dropTo;
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragEnter(this);
                }
            }

            // we repaint twice to create an intersection of the old and new position
            p.repaint(draggedx, draggedy, getWidth(), getHeight());
            draggedx = draggedx + (x - oldx);
            draggedy = draggedy + (y - oldy);
            oldx = x;
            oldy = y;
            p.repaint(draggedx , draggedy, getWidth(), getHeight());
            Container scrollParent = getParent();
            while(scrollParent != null && !scrollParent.isScrollable()){
                scrollParent = scrollParent.getParent();
            }
            if(scrollParent != null){
                Style s = getStyle();
                int w = getWidth() - s.getPadding(isRTL(), LEFT) - s.getPadding(isRTL(), RIGHT);
                int h = getHeight() - s.getPadding(false, TOP) - s.getPadding(false, BOTTOM);

                Rectangle view;
                int invisibleAreaUnderVKB = Form.getInvisibleAreaUnderVKB(getComponentForm());
                view = new Rectangle(getScrollX(), getScrollY(), w, h - invisibleAreaUnderVKB);
                //if the dragging component is out of bounds move the scrollable parent
                if(!view.contains(draggedx - scrollParent.getAbsoluteX(), draggedy - scrollParent.getAbsoluteY(), getWidth(), getHeight())){
                    if((scrollParent.isScrollableY() && scrollParent.getScrollY() >= 0 && scrollParent.getScrollY() + (draggedy + getHeight()) < scrollParent.getScrollDimension().getHeight()) || 
                       (scrollParent.isScrollableX() && scrollParent.getScrollX() >= 0 && scrollParent.getScrollX() + (draggedx + getWidth()) < scrollParent.getScrollDimension().getWidth()) ){
                        int yposition = draggedy - scrollParent.getAbsoluteY() - 40;
                        if( yposition  < 0){
                            yposition = 0;
                        }
                        int xposition = draggedx - scrollParent.getAbsoluteX() - 40;
                        if( xposition  < 0){
                            xposition = 0;
                        }
                        int height = getHeight() + 80;
                        if(scrollParent.getScrollY() + draggedy + height >= scrollParent.getScrollDimension().getHeight()){
                            yposition = draggedy - scrollParent.getAbsoluteY();
                            height = scrollParent.getScrollDimension().getHeight() - yposition;
                        }                        
                        int width = getWidth()+ 80;
                        if(scrollParent.getScrollX() + draggedx + width >= scrollParent.getScrollDimension().getWidth()){
                            xposition = draggedx - scrollParent.getAbsoluteX();
                            width = scrollParent.getScrollDimension().getWidth() - xposition;
                        }                        
                                
                        scrollParent.scrollRectToVisible(xposition, yposition, width, height, scrollParent);            
                    }
                }
            }    
                
            return;
        }
        if(!dragActivated){
            boolean draggedOnX = Math.abs(p.initialPressX - x) > Math.abs(p.initialPressY - y);
            shouldGrabScrollEvents = (isScrollableX() && draggedOnX) || isScrollableY() && !draggedOnX;
        }
        
        if (isScrollable() && isSmoothScrolling() && shouldGrabScrollEvents) {
            if (!dragActivated) {
                dragActivated = true;
                lastScrollY = y;
                lastScrollX = x;
                p.setDraggedComponent(this);
                p.registerAnimatedInternal(this);
                Component fc = p.getFocused();
                if(fc != null && fc != this) {
                    fc.dragInitiated();
                }
            }

            // we drag inversly to get a feel of grabbing a physical screen
            // and pulling it in the reverse direction of the drag
            if (isScrollableY()) {
                int tl;
                if(getTensileLength() > -1 && refreshTask == null) {
                    tl = getTensileLength();
                } else {
                    tl = getHeight() / 2;
                }
                if(!isSmoothScrolling() || !isTensileDragEnabled()) {
                    tl = 0;
                }
                int scroll = getScrollY() + (lastScrollY - y);
                
                if(isAlwaysTensile() && getScrollDimension().getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm()) <= getHeight()) {
                    if (scroll >= -tl && scroll < getHeight() + tl) {
                        setScrollY(scroll);
                    }
                } else {
                    if (scroll >= -tl && scroll < getScrollDimension().getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm()) - getHeight() + tl) {
                        setScrollY(scroll);
                    }
                }
                updateTensileHighlightIntensity(lastScrollY, y, false);
            }
            if (isScrollableX()) {
                int tl;
                if(getTensileLength() > -1) {
                    tl = getTensileLength();
                } else {
                    tl = getWidth() / 2;
                }
                if(!isSmoothScrolling() || !isTensileDragEnabled()) {
                    tl= 0;
                }
                int scroll = getScrollX() + (lastScrollX - x);
                if (scroll >= -tl && scroll < getScrollDimension().getWidth() - getWidth() + tl) {
                    setScrollX(scroll);
                }
            }
            lastScrollY = y;
            lastScrollX = x;
        } else {
            //try to find a scrollable element until you reach the Form
            Component parent = getParent();
            if (!(parent instanceof Form)) {
                parent.pointerDragged(x, y);
            }
        }
    }

    /**
     * Returns true if the component is interested in receiving drag/pointer release events even
     * after the gesture exceeded its boundaries. This is useful for spinners etc. where the motion
     * might continue beyond the size of the component
     * @return false by default
     */
    protected boolean isStickyDrag() {
        return false;
    }
    
    private void initScrollMotion() {
        // the component might not be registered for animation if it started off 
        // as smaller than the screen and grew (e.g. by adding components to the container
        // once it is visible).
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimatedInternal(this);
        }

        Motion m = Motion.createLinearMotion(initialScrollY, destScrollY, getScrollAnimationSpeed());
        setAnimationMotion(m);
        m.start();
    }

    /**
     * If this Component is focused, the pointer pressed event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerPressed(int[] x, int[] y) {
        dragActivated = false;
        pointerPressed(x[0], y[0]);
        scrollOpacity = 0xff;
    }

    /**
     * This method allows a developer to define only a specific portion of a component as draggable
     * by default it returns true if the component is defined as "draggable"
     * 
     * @param x the x coordinate relative to the component
     * @param y the y coordinate relative to the component
     * @return true if a press in this point might indicate the desire to begin a drag operation
     */
    protected boolean isDragAndDropOperation(int x, int y) {
        return draggable;
    }

    /**
     * If this Component is focused, the pointer pressed event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerPressed(int x, int y) {
        dragActivated = false;
        if (pointerPressedListeners != null && pointerPressedListeners.hasListeners()) {
            pointerPressedListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed, x, y));
        }
        clearDrag();
        if(isDragAndDropOperation(x, y)) {
            int restore = Display.getInstance().getDragStartPercentage();
            if(restore > 1){
                restoreDragPercentage = restore;
            }
            Display.getInstance().setDragStartPercentage(1);
        }
    }

    void initDragAndDrop(int x, int y) {
        dragAndDropInitialized = isDragAndDropOperation(x, y);
    }

    /**
     * If this Component is focused, the pointer released event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerReleased(int[] x, int[] y) {
        pointerReleased(x[0], y[0]);
    }

    /**
     * If this Component is focused this method is invoked when the user presses
     * and holds the pointer on the Component
     * 
     */
    public void longPointerPress(int x, int y) {
    }

    /**
     * If this Component is focused, the pointer released event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerReleased(int x, int y) {
        if (pointerReleasedListeners != null && pointerReleasedListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(this, ActionEvent.Type.PointerReleased, x, y);
            pointerReleasedListeners.fireActionEvent(ev);
            if(ev.isConsumed()) {
                return;
            }
        }
        pointerReleaseImpl(x, y);
        scrollOpacity = 0xff;
    }

    /**
     * Indicates whether tensile drag (dragging beyond the boundry of the component and
     * snapping back) is enabled for this component.
     *
     * @param tensileDragEnabled true to enable tensile drag
     */
    public void setTensileDragEnabled(boolean tensileDragEnabled) {
        this.tensileDragEnabled = tensileDragEnabled;
    }

    /**
     * Indicates whether tensile drag (dragging beyond the boundry of the component and
     * snapping back) is enabled for this component.
     *
     * @return true when tensile drag is enabled
     */
    public boolean isTensileDragEnabled() {
        return tensileDragEnabled;
    }

    void startTensile(int offset, int dest, boolean vertical) {
        Motion draggedMotion;
        if(tensileDragEnabled) {
            draggedMotion = Motion.createDecelerationMotion(offset, dest, 500);
            draggedMotion.start();
        } else {
            draggedMotion = Motion.createLinearMotion(offset, dest, 0);
            draggedMotion.start();
        }
        
        if(vertical){
            draggedMotionY = draggedMotion;
        }else{
            draggedMotionX = draggedMotion;        
        }
        // just to be sure, there are some cases where this doesn't work as expected
        Form p = getComponentForm();
        if(p != null) {
            p.registerAnimatedInternal(this);
        }
    }

    private boolean chooseScrollXOrY(int x , int y) {
        boolean ix = isScrollableX();
        boolean iy = isScrollableY();
        if(ix && iy) {
            Form parent = getComponentForm();
            return Math.abs(parent.initialPressX - x) > Math.abs(parent.initialPressY - y);
        }
        if(ix) {
            return true;
        }
        return false;
    }

    /**
     * Binds an action listener to drop events which are invoked when this component is dropped on a target
     * @param l the callback
     */
    public void addDropListener(ActionListener l) {
        if(dropListener == null) {
            dropListener = new EventDispatcher();
        }
        dropListener.addListener(l);
    }
    
    /**
     * Removes an action listener to drop events which are invoked when this component is dropped on a target
     * @param l the callback
     */
    public void removeDropListener(ActionListener l) {
        if(dropListener == null) {
            return;
        }
        dropListener.removeListener(l);
        if(!dropListener.hasListeners()) {
            dropListener = null;
        }
    }
    
    /**
     * Broadcasts an event when dragging over a component
     * @param l the listener
     */
    public void addDragOverListener(ActionListener l) {
        if(dragOverListener == null) {
            dragOverListener = new EventDispatcher();
        }
        dragOverListener.addListener(l);
    }
    
    /**
     * Removes an action listener to drag over events 
     * @param l the callback
     */
    public void removeDragOverListener(ActionListener l) {
        if(dragOverListener == null) {
            return;
        }
        dragOverListener.removeListener(l);
        if(!dragOverListener.hasListeners()) {
            dragOverListener = null;
        }
    }
    
    /**
     * Callback indicating that the drag has finished either via drop or by releasing the component
     * @param x the x location 
     * @param y the y location
     */
    protected void dragFinished(int x, int y) {
    }

    void dragFinishedImpl(int x, int y) {
        if(dragAndDropInitialized && dragActivated) {
            Form p = getComponentForm();
            p.setDraggedComponent(null);
            Component dropTo = findDropTarget(this, x, y);
            if(dropTargetComponent != dropTo) {
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragExit(this);
                }
                dropTargetComponent = dropTo;
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragEnter(this);
                }
            }
            if(dropTargetComponent != null) {
                p.repaint(x, y, getWidth(), getHeight());
                getParent().scrollRectToVisible(x, y, getWidth(), getHeight(), getParent());
                if(dropListener != null) {
                    ActionEvent ev = new ActionEvent(this, ActionEvent.Type.PointerDrag, dropTargetComponent, x, y);
                    dropListener.fireActionEvent(ev);
                    if(!ev.isConsumed()) {
                        dropTargetComponent.drop(this, x, y);
                    }
                } else {
                    dropTargetComponent.drop(this, x, y);
                }
            } else {
                if(dragOverListener != null) {
                    ActionEvent ev = new ActionEvent(this, ActionEvent.Type.PointerDrag,null, x, y);
                    dragOverListener.fireActionEvent(ev);
                }
                p.repaint();
            }
            setVisible(true);
            dragImage = null;
            dropTargetComponent = null;
        }
        if(getUIManager().getLookAndFeel().isFadeScrollBar() && isScrollable()) {
            Form frm = getComponentForm();
            if(frm != null) {
                frm.registerAnimatedInternal(this);
            }
        }
        dragActivated = false;
        dragAndDropInitialized = false;
        dragFinished(x, y);
    }

    /**
     * Adds a listener to the pointer event
     *
     * @param l callback to receive pointer events
     */
    public void addPointerPressedListener(ActionListener l) {
        if (pointerPressedListeners == null) {
            pointerPressedListeners = new EventDispatcher();
        }
        pointerPressedListeners.addListener(l);
    }

    /**
     * Removes the listener from the pointer event
     *
     * @param l callback to remove
     */
    public void removePointerPressedListener(ActionListener l) {
        if (pointerPressedListeners != null) {
            pointerPressedListeners.removeListener(l);
        }
    }

    /**
     * Adds a listener to the pointer event
     *
     * @param l callback to receive pointer events
     */
    public void addPointerReleasedListener(ActionListener l) {
        if (pointerReleasedListeners == null) {
            pointerReleasedListeners = new EventDispatcher();
        }
        pointerReleasedListeners.addListener(l);
    }

    /**
     * Removes the listener from the pointer event
     *
     * @param l callback to remove
     */
    public void removePointerReleasedListener(ActionListener l) {
        if (pointerReleasedListeners != null) {
            pointerReleasedListeners.removeListener(l);
        }
    }

    /**
     * Adds a listener to the pointer event
     *
     * @param l callback to receive pointer events
     */
    public void addPointerDraggedListener(ActionListener l) {
        if (pointerDraggedListeners == null) {
            pointerDraggedListeners = new EventDispatcher();
        }
        pointerDraggedListeners.addListener(l);
    }

    /**
     * Removes the listener from the pointer event
     *
     * @param l callback to remove
     */
    public void removePointerDraggedListener(ActionListener l) {
        if (pointerDraggedListeners != null) {
            pointerDraggedListeners.removeListener(l);
        }
    }
    
    private void pointerReleaseImpl(int x, int y) {
        if(restoreDragPercentage > -1) {
            Display.getInstance().setDragStartPercentage(restoreDragPercentage);
        }
        pinchDistance = -1;
        if (dragActivated) {
            dragActivated = false;
            boolean startedTensileX = false;
            boolean startedTensileY = false;
            if(isScrollableX()){
                if (scrollX < 0) {
                    startTensile(scrollX, 0, false);
                    startedTensileX = true;
                } else {
                    if(scrollX > getScrollDimension().getWidth() - getWidth()) {
                        startTensile(scrollX, Math.max(getScrollDimension().getWidth() - getWidth(), 0), false);
                        startedTensileX = true;
                    }
                }
            }
            if(isScrollableY()){
                if (scrollY < 0) {
                    if(refreshTask != null){
                        putClientProperty("$pullToRelease", "normal");
                        if(scrollY < - getUIManager().getLookAndFeel().getPullToRefreshHeight()){
                            putClientProperty("$pullToRelease", "update");                  
                            startTensile(scrollY, -getUIManager().getLookAndFeel().getPullToRefreshHeight(), true);
                            startedTensileY = true;
                        }
                    }else{
                        startTensile(scrollY, 0, true);
                        startedTensileY = true;
                    }
                } else {
                    int scrh = getScrollDimension().getHeight() - getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm());
                    if(scrollY > scrh) {
                        startTensile(scrollY, Math.max(scrh, 0), true);
                        startedTensileY = true;
                    }
                }
            }
            boolean shouldScrollX = chooseScrollXOrY(x, y);
            if(shouldScrollX && startedTensileX || !shouldScrollX && startedTensileY){
                return;
            }
            
            int scroll = scrollY;
            if(shouldScrollX){
                scroll = scrollX;
            }
            float speed = getDragSpeed(!shouldScrollX);
            int tl;
            if(getTensileLength() > -1) {
                tl = getTensileLength();
            } else {
                tl = getWidth() / 2;
            }
            if(!isTensileDragEnabled()) {
                tl = 0;
            }
            if(!shouldScrollX) {
                if(speed < 0) {
                    if (UIManager.getInstance().getThemeConstant("ScrollMotion", "DECAY").equals("DECAY")) {
                        int timeConstant = UIManager.getInstance().getThemeConstant("ScrollMotionTimeConstantInt", 500);
                        
                        draggedMotionY = Motion.createExponentialDecayMotion(scroll, -tl/2, speed, timeConstant);
                    } else {
                        draggedMotionY = Motion.createFrictionMotion(scroll, -tl/2, speed, 0.0007f);
                    }
                } else {
                    if (UIManager.getInstance().getThemeConstant("ScrollMotion", "DECAY").equals("DECAY")) {
                        int timeConstant = UIManager.getInstance().getThemeConstant("ScrollMotionTimeConstantInt", 500);
                        draggedMotionY = Motion.createExponentialDecayMotion(scroll, getScrollDimension().getHeight() - 
                                getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm()) + tl/2,  speed, timeConstant);
                    } else {
                        draggedMotionY = Motion.createFrictionMotion(scroll, getScrollDimension().getHeight() - 
                                getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm()) + tl/2, speed, 0.0007f);
                    }
                }
            } else {
                if(speed < 0) {
                    draggedMotionX = Motion.createFrictionMotion(scroll, -tl/2, speed, 0.0007f);
                } else {
                    draggedMotionX = Motion.createFrictionMotion(scroll, getScrollDimension().getWidth() -
                            getWidth() + tl/2, speed, 0.0007f);
                }
            }
            if(draggedMotionX != null){
                draggedMotionX.start();
            }
            if(draggedMotionY != null){
                draggedMotionY.start();
            }
        }
    }


    /**
     * This method returns the dragging speed based on the latest dragged
     * events
     * @param vertical indicates what axis speed is required
     * @return the dragging speed
     */
    protected float getDragSpeed(boolean vertical) {
        return Display.getInstance().getDragSpeed(vertical);
    }
    
    /**
     * Returns the current Component Style allowing code to draw the current component, you
     * should normally use getUnselected/Pressed/DisabledStyle() and not this method since
     * it will return different values based on component state.
     * 
     * @return the component Style object
     */
    public Style getStyle() {
        if (unSelectedStyle == null) {
            initStyle();
        }
        isUnselectedStyle = false;

        if(hasLead) {
            Component lead = getLeadComponent();
            if(lead != null) {
                if(!lead.isEnabled()) {
                    return getDisabledStyle();
                }

                if(lead.isPressedStyle()) {
                    return getPressedStyle();
                }

                if (lead.hasFocus() && Display.getInstance().shouldRenderSelection(this)) {
                    return getSelectedStyle();
                }
            }
            isUnselectedStyle = true;
            return unSelectedStyle;
        }

        if(!isEnabled()) {
            return getDisabledStyle();
        }

        if(isPressedStyle()) {
            return getPressedStyle();
        }

        if (hasFocus() && Display.getInstance().shouldRenderSelection(this)) {
            return getSelectedStyle();
        }
        isUnselectedStyle = true;
        return unSelectedStyle;
    }

    boolean isPressedStyle() {
        return false;
    }

    /**
     * Returns the Component Style for the pressed state allowing us to manipulate
     * the look of the component when it is pressed
     *
     * @return the component Style object
     */
    public Style getPressedStyle() {
        if (pressedStyle == null) {
            pressedStyle = getUIManager().getComponentCustomStyle(getUIID(), "press");
            pressedStyle.addStyleListener(this);
            if(pressedStyle.getBgPainter() == null){
                pressedStyle.setBgPainter(new BGPainter());
            }
        }
        return pressedStyle;
    }

    /**
     * Sets the Component Style for the pressed state allowing us to manipulate
     * the look of the component when it is pressed
     *
     * @param style the component Style object
     */
    public void setPressedStyle(Style style) {
        if (pressedStyle != null) {
            pressedStyle.removeStyleListener(this);
        }
        pressedStyle = style;
        pressedStyle.addStyleListener(this);
        if (pressedStyle.getBgPainter() == null) {
            pressedStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Returns the Component Style for the unselected mode allowing us to manipulate
     * the look of the component
     *
     * @return the component Style object
     */
    public Style getUnselectedStyle() {
        if (unSelectedStyle == null) {
            initStyle();
        }
        return unSelectedStyle;
    }

    /**
     * Returns the Component Style for the selected state allowing us to manipulate
     * the look of the component when it owns focus
     *
     * @return the component Style object
     */
    public Style getSelectedStyle() {
        if (selectedStyle == null) {
            selectedStyle = getUIManager().getComponentSelectedStyle(getUIID());
            selectedStyle.addStyleListener(this);
            if (selectedStyle.getBgPainter() == null) {
                selectedStyle.setBgPainter(new BGPainter());
            }
        }
        return selectedStyle;
    }

    /**
     * Returns the Component Style for the disabled state allowing us to manipulate
     * the look of the component when its disabled
     *
     * @return the component Style object
     */
    public Style getDisabledStyle() {
        if (disabledStyle == null) {
            disabledStyle = getUIManager().getComponentCustomStyle(getUIID(), "dis");
            disabledStyle.addStyleListener(this);
            if (disabledStyle.getBgPainter() == null) {
                disabledStyle.setBgPainter(new BGPainter());
            }
        }
        return disabledStyle;
    }


    /**
     * Changes the Component Style by replacing the Component Style with the given Style
     * 
     * @param style the component Style object
     */
    public void setUnselectedStyle(Style style) {
        if (this.unSelectedStyle != null) {
            this.unSelectedStyle.removeStyleListener(this);
        }
        this.unSelectedStyle = style;
        this.unSelectedStyle.addStyleListener(this);
        if (this.unSelectedStyle.getBgPainter() == null) {
            this.unSelectedStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Changes the Component selected Style by replacing the Component Style with the given Style
     *
     * @param style the component Style object
     */
    public void setSelectedStyle(Style style) {
        if (this.selectedStyle != null) {
            this.selectedStyle.removeStyleListener(this);
        }
        this.selectedStyle = style;
        this.selectedStyle.addStyleListener(this);
        if (this.selectedStyle.getBgPainter() == null) {
            this.selectedStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Changes the Component disalbed Style by replacing the Component Style with the given Style
     *
     * @param style the component Style object
     */
    public void setDisabledStyle(Style style) {
        if (this.disabledStyle != null) {
            this.disabledStyle.removeStyleListener(this);
        }
        this.disabledStyle = style;
        this.disabledStyle.addStyleListener(this);
        if (this.disabledStyle.getBgPainter() == null) {
            this.disabledStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Allows subclasses to create their own custom style types and install the background painter into them
     * 
     * @param s the custom style
     */
    protected void installDefaultPainter(Style s) {
        if(s.getBgPainter() == null) {
            s.setBgPainter(new BGPainter(s));
        }
    }

    /**
     * Changes the current component to the focused component, will work only
     * for a component that belongs to a parent form.
     */
    public void requestFocus() {
        Form rootForm = getComponentForm();
        if (rootForm != null) {
            Component.setDisableSmoothScrolling(true);
            rootForm.requestFocus(this);
            Component.setDisableSmoothScrolling(false);
        }
    }

    /**
     * Overriden to return a useful value for debugging purposes
     * 
     * @return a string representation of this component
     */
    public String toString() {
        String className = getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "[" + paramString() + "]";
    }

    /**
     * Returns a string representing the state of this component. This 
     * method is intended to be used only for debugging purposes, and the 
     * content and format of the returned string may vary between 
     * implementations. The returned string may be empty but may not be 
     * <code>null</code>.
     * 
     * @return  a string representation of this component's state
     */
    protected String paramString() {
        return "x=" + getX() + " y=" + getY() + " width=" + getWidth() + " height=" + getHeight();
    }

    /**
     * Makes sure the component is up to date with the current theme, ONLY INVOKE THIS METHOD IF YOU CHANGED THE THEME!
     */
    public void refreshTheme() {
        refreshTheme(true);
    }

    /**
     * Makes sure the component is up to date with the current theme, ONLY INVOKE THIS METHOD IF YOU CHANGED THE THEME!
     * @param merge indicates if the current styles should be merged with the new styles
     */
    public void refreshTheme(boolean merge) {
        refreshTheme(getUIID(), merge);
        initLaf(getUIManager());        
    }

    /**
     * Makes sure the component is up to date with the given UIID
     * 
     * @param id The Style Id to update the Component with
     * @param merge indicates if the current styles should be merged with the new styles
     */
    protected void refreshTheme(String id, boolean merge) {
        UIManager manager = getUIManager();
   
        if(merge){
            Style unSelected = getUnselectedStyle();
            setUnselectedStyle(mergeStyle(unSelected, manager.getComponentStyle(id)));            
            if (selectedStyle != null) {
                setSelectedStyle(mergeStyle(selectedStyle, manager.getComponentSelectedStyle(id)));
            }
            if (disabledStyle != null) {
                setDisabledStyle(mergeStyle(disabledStyle, manager.getComponentCustomStyle(id, "dis")));
            }
            if(pressedStyle != null) {
                setPressedStyle(mergeStyle(pressedStyle, manager.getComponentCustomStyle(id, "press")));
            }
        }else{
            unSelectedStyle = null;
            unSelectedStyle = getUnselectedStyle();
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;        
        }
        checkAnimation();
        manager.getLookAndFeel().bind(this);
    }

    Style mergeStyle(Style toMerge, Style newStyle) {
        if (toMerge.isModified()) {
            toMerge.merge(newStyle);
            return toMerge;
        } else {
            return newStyle;
        }

    }

    /**
     * Indicates whether we are in the middle of a drag operation, this method allows
     * developers overriding the pointer released events to know when this is a drag
     * operation.
     * 
     * @return true if we are in the middle of a drag; otherwise false
     */
    protected boolean isDragActivated() {
        return dragActivated;
    }

    void setDragActivated(boolean dragActivated) {
        this.dragActivated = dragActivated;
    }

    void checkAnimation() {
        Image bgImage = getStyle().getBgImage();
        if (bgImage != null && bgImage.isAnimation()) {
            Form pf = getComponentForm();
            if (pf != null) {
                // animations are always running so the internal animation isn't
                // good enough. We never want to stop this sort of animation
                pf.registerAnimated(this);
            }
        } else {
            Painter p = getStyle().getBgPainter();
            if(p != null && p.getClass() != BGPainter.class && p instanceof Animation) {
                Form pf = getComponentForm();
                if (pf != null) {
                    pf.registerAnimated(this);
                }
            } else {
                if (scrollOpacity == 0xff && isScrollable() && getUIManager().getLookAndFeel().isFadeScrollBar()) {
                    // trigger initial fade process on a fresh view.
                    Form pf = getComponentForm();
                    if (pf != null) {
                        pf.registerAnimatedInternal(this);
                    }
                }
            }
        } 
    }

    void deregisterAnimatedInternal() {
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimatedInternal(this);
        }
    }
    
    /**
     * This method should be implemented correctly by subclasses to make snap to grid functionality work
     * as expected. Returns the ideal grid Y position closest to the current Y position.
     * 
     * @return a valid Y position in the grid
     */
    protected int getGridPosY() {
        return getScrollY();
    }

    /**
     * This method should be implemented correctly by subclasses to make snap to grid functionality work
     * as expected. Returns the ideal grid X position closest to the current X position.
     *
     * @return a valid Y position in the grid
     */
    protected int getGridPosX() {
        return getScrollX();
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        if(!visible){
            return false;
        }
        Image bgImage = getStyle().getBgImage();
        boolean animateBackground = bgImage != null && bgImage.isAnimation() && bgImage.animate();
        Motion m = getAnimationMotion();
        
        // perform regular scrolling
        if (m != null && destScrollY != -1 && destScrollY != getScrollY()) {
            // change the variable directly for efficiency both in removing redundant
            // repaints and scroll checks
            setScrollY(m.getValue());
            if (destScrollY == scrollY) {
                destScrollY = -1;
                deregisterAnimatedInternal();
                updateTensileHighlightIntensity(0, 0, m != null);
            }
            return true;
        }
        boolean animateY = false;
        boolean animateX = false;
        // perform the dragging motion if exists
        if (draggedMotionY != null) {
            // change the variable directly for efficiency both in removing redundant
            // repaints and scroll checks
            int dragVal = draggedMotionY.getValue();

            // this can't be a part of the parent if since we need the last value to arrive
            if (draggedMotionY.isFinished()) {
                if (dragVal < 0) {
                    startTensile(dragVal, 0, true);
                } else {
                    int iv = Form.getInvisibleAreaUnderVKB(getComponentForm());
                    int edge = (getScrollDimension().getHeight() - getHeight() + iv);
                    if (dragVal > edge && edge > 0) {
                        startTensile(dragVal, getScrollDimension().getHeight() - getHeight() + iv, true);
                    } else {
                        if (snapToGrid && getScrollY() < edge && getScrollY() > 0) {
                            int dest = getGridPosY();
                            int scroll = getScrollY();
                            if (dest != scroll) {
                                startTensile(scroll, dest, true);
                            } else {
                                draggedMotionY = null;
                            }
                        } else {
                            draggedMotionY = null;
                        }
                    }
                }
                
                // special callback to scroll Y to allow developers to override the setScrollY method effectively
                setScrollY(dragVal);
                updateTensileHighlightIntensity(dragVal, getScrollDimension().getHeight() - getHeight() + Form.getInvisibleAreaUnderVKB(getComponentForm()), false);            
            }

            if(scrollListeners != null){
                scrollListeners.fireScrollEvent(this.scrollX, dragVal, this.scrollX, this.scrollY);
            }
            scrollY = dragVal;
            onScrollY(scrollY);
            updateTensileHighlightIntensity(0, 0, false);
            animateY = true;
        }
        if (draggedMotionX != null) {
            // change the variable directly for efficiency both in removing redundant
            // repaints and scroll checks
            int dragVal = draggedMotionX.getValue();

            // this can't be a part of the parent if since we need the last value to arrive
            if (draggedMotionX.isFinished()) {
                if (dragVal < 0) {
                    startTensile(dragVal, 0, false);
                } else {
                    int edge = (getScrollDimension().getWidth() - getWidth());
                    if (dragVal > edge && edge > 0) {
                        startTensile(dragVal, getScrollDimension().getWidth() - getWidth(), false);
                    } else {
                        if (snapToGrid && getScrollX() < edge && getScrollX() > 0) {
                            int dest = getGridPosX();
                            int scroll = getScrollX();
                            if (dest != scroll) {
                                startTensile(scroll, dest, false);
                            } else {
                                draggedMotionX = null;
                            }
                        } else {
                            draggedMotionX = null;
                        }
                    }
                }
                
                // special callback to scroll X to allow developers to override the setScrollY method effectively
                setScrollX(dragVal);
            }

            if(scrollListeners != null){
                scrollListeners.fireScrollEvent(dragVal, this.scrollY, this.scrollX, this.scrollY);
            }
            scrollX = dragVal;
            onScrollX(scrollX);
            animateX = true;
        }
        if(animateY || animateX){
            return true;
        }
        
        if(getClientProperty("$pullToRelease") != null){
            return true;
        }
        
        
        Painter bgp = getStyle().getBgPainter();
        boolean animateBackgroundB = bgp != null && bgp.getClass() != BGPainter.class && bgp instanceof Animation && (bgp != this) && ((Animation)bgp).animate();
        animateBackground = animateBackgroundB || animateBackground;
                
        if(getUIManager().getLookAndFeel().isFadeScrollBar()) {
            if(tensileHighlightIntensity > 0) {
                tensileHighlightIntensity = Math.max(0, tensileHighlightIntensity - (scrollOpacityChangeSpeed * 2));
            }
            if(scrollOpacity > 0 && !dragActivated) {
                scrollOpacity = Math.max(0, scrollOpacity - scrollOpacityChangeSpeed);
                return true;
            }
        }

        if(!animateBackground && (destScrollY == -1 || destScrollY == scrollY) &&
                !animateBackground && m == null && draggedMotionY == null &&
                draggedMotionX == null && !dragActivated) {
            tryDeregisterAnimated();
        }

        return animateBackground;
    }

    /**
     * Removes the internal animation. This method may be overriden by sublcasses to block automatic removal
     */
    void tryDeregisterAnimated() {
        deregisterAnimatedInternal();
    }

    /**
     * Makes sure the component is visible in the scroll if this container 
     * is scrollable
     * 
     * @param rect the rectangle that need to be visible
     * @param coordinateSpace the component according to whose coordinates 
     * rect is defined. Rect's x/y are relative to that component 
     * (they are not absolute).
     */
    protected void scrollRectToVisible(Rectangle rect, Component coordinateSpace) {
        scrollRectToVisible(rect.getX(), rect.getY(), 
                rect.getSize().getWidth(), rect.getSize().getHeight(), coordinateSpace);
    }

    /**
     * Makes sure the component is visible in the scroll if this container 
     * is scrollable
     *
     * @param x 
     * @param y 
     * @param width 
     * @param height  
     * @param coordinateSpace the component according to whose coordinates 
     * rect is defined. Rect's x/y are relative to that component 
     * (they are not absolute).
     */
    public void scrollRectToVisible(int x, int y, int width, int height, Component coordinateSpace) {
        if (isScrollable()) {
            int scrollPosition = getScrollY();
            Style s = getStyle();
            int w = getWidth() - s.getPadding(isRTL(), LEFT) - s.getPadding(isRTL(), RIGHT);
            int h = getHeight() - s.getPadding(false, TOP) - s.getPadding(false, BOTTOM);

            Rectangle view;
            int invisibleAreaUnderVKB = Form.getInvisibleAreaUnderVKB(getComponentForm());
            if (isSmoothScrolling() && destScrollY > -1) {
                view = new Rectangle(getScrollX(), destScrollY, w, h - invisibleAreaUnderVKB);
            } else {
                view = new Rectangle(getScrollX(), getScrollY(), w, h - invisibleAreaUnderVKB);
            }

            int relativeX = x;
            int relativeY = y;

            // component needs to be in absolute coordinates...
            Container parent = null;
            if (coordinateSpace != null) {
                parent = coordinateSpace.getParent();
            }
            if (parent == this) {
                if (view.contains(x, y, width, height)) {
                    return;
                }
            } else {
                while (parent != this) {
                    // mostly a special case for list
                    if (parent == null) {
                        relativeX = x;
                        relativeY = y;
                        break;
                    }
                    relativeX += parent.getX();
                    relativeY += parent.getY();
                    parent = parent.getParent();
                }
                if (view.contains(relativeX, relativeY, width, height)) {
                    return;
                }
            }
            if (isScrollableX()) {
                if (getScrollX() > relativeX) {
                    setScrollX(relativeX);
                }
                int rightX = relativeX + width - 
                        s.getPadding(LEFT) - s.getPadding(RIGHT);
                if (getScrollX() + w < rightX) {
                    setScrollX(getScrollX() + (rightX - (getScrollX() + w)));
                } else {
                    if (getScrollX() > relativeX) {
                        setScrollX(relativeX);
                    }
                }
            }

            if (isScrollableY()) {
                if (getScrollY() > relativeY) {
                    scrollPosition = relativeY;
                }
                int bottomY = relativeY + height - 
                        s.getPadding(TOP) - s.getPadding(BOTTOM);
                if (getScrollY() + h < bottomY + invisibleAreaUnderVKB) {
                    scrollPosition = getScrollY() + (bottomY - (getScrollY() + h)) + invisibleAreaUnderVKB;
                } else {
                    if (getScrollY() > relativeY) {
                        scrollPosition = relativeY;
                    }
                }
                if (isSmoothScrolling() && isInitialized()) {
                    initialScrollY = getScrollY();
                    destScrollY = scrollPosition;
                    initScrollMotion();
                } else {
                    setScrollY(scrollPosition);
                }
            }
            repaint();
        } else {
            //try to move parent scroll if you are not scrollable
            Container parent = getParent();
            if (parent != null) {
                parent.scrollRectToVisible(getAbsoluteX() - parent.getAbsoluteX() + x,
                        getAbsoluteY() - parent.getAbsoluteY() + y, 
                        width, height, parent);
            }
        }
    }

    /**
     * Indicates whether a border should be painted
     *
     * @return if the border will be painted
     * @deprecated use getStyle().getBorder() != null 
     */
    private boolean isBorderPainted() {
        return getStyle().getBorder() != null;
    }

    /**
     * Draws the component border if such a border exists. The border unlike the content
     * of the component will not be affected by scrolling for a scrollable component.
     * 
     * @param g graphics context on which the border is painted
     */
    protected void paintBorder(Graphics g) {
        Border b = getBorder();
        if (b != null) {
            g.setColor(getStyle().getFgColor());
            b.paint(g, this);
        }
    }

    /**
     * Draws the component border background if such a border exists.
     * 
     * @param g graphics context on which the border is painted
     */
    protected void paintBorderBackground(Graphics g) {
        Border b = getBorder();
        if (b != null) {
            b.paintBorderBackground(g, this);
        }
    }
    
    /**
     * Used as an optimization to mark that this component is currently being
     * used as a cell renderer
     * 
     * @param cellRenderer indicate whether this component is currently being
     * used as a cell renderer
     */
    public void setCellRenderer(boolean cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    /**
     * Used as an optimization to mark that this component is currently being
     * used as a cell renderer
     * 
     * @return true is this component is currently being used as a cell renderer
     */
    public boolean isCellRenderer() {
        return cellRenderer;
    }

    /**
     * Indicate whether this component scroll is visible
     * 
     * @return true is this component scroll is visible; otherwise false
     */
    public boolean isScrollVisible() {
        return isScrollVisible;
    }

    /**
     * Set whether this component scroll is visible
     *
     * @param isScrollVisible Indicate whether this component scroll is visible
     */
    public void setScrollVisible(boolean isScrollVisible) {
        this.isScrollVisible = isScrollVisible;
    }

    /**
     * Set whether this component scroll is visible
     * 
     * @param isScrollVisible Indicate whether this component scroll is visible
     * @deprecated replaced by setScrollVisible to match the JavaBeans spec
     */
    public void setIsScrollVisible(boolean isScrollVisible) {
        this.isScrollVisible = isScrollVisible;
    }

    void lockStyleImages(Style stl) {
        Image i = stl.getBgImage();
        if(i != null) {
            i.lock();
        } else {
            Border b = stl.getBorder();
            if(b != null) {
                b.lock();
            }
        }
    }
    
    /**
     * Invoked internally to initialize and bind the component
     */
    void initComponentImpl() {
        if (!initialized) {
            initialized = true;
            UIManager manager = getUIManager();
            Style stl = getStyle();
            lockStyleImages(stl);
            manager.getLookAndFeel().bind(this);
            checkAnimation();
            if(isRTL() && isScrollableX()){
                setScrollX(getScrollDimension().getWidth() - getWidth());
            }
            initComponent();
        }
    }

    /**
     * Cleansup the initialization flags in the hierachy, notice that paint calls might
     * still occur after deinitilization mostly to perform transitions etc.
     * <p>However interactivity, animation and event tracking code can and probably
     * should be removed by this method.
     */
    void deinitializeImpl() {
        if (isInitialized()) {
            paintLockRelease();
            setInitialized(false);
            setDirtyRegion(null);
            Style stl = getStyle();
            Image i = stl.getBgImage();
            if(i != null) {
                i.unlock();
            } else {
                Border b = stl.getBorder();
                if(b != null) {
                    b.unlock();
                }
            }
            Painter p = stl.getBgPainter();
            if(p instanceof BGPainter) {
                ((BGPainter)p).radialCache = null;
            }           
            deinitialize();
        }
    }

    /**
     * This is a callback method to inform the Component when it's been laidout
     * on the parent Container
     */
    protected void laidOut() {
        if(!isCellRenderer()) {
            CodenameOneImplementation ci = Display.impl;
            if(ci.isEditingText()) {
                return;
            }
            Form f = getComponentForm();
            int ivk = Form.getInvisibleAreaUnderVKB(f);
            
            if (isScrollableY() && getScrollY() > 0 && getScrollY() + getHeight() >
                    getScrollDimension().getHeight() + ivk) {
                setScrollY(getScrollDimension().getHeight() - getHeight() + ivk);
            }
            if (isScrollableX() && getScrollX() > 0 && getScrollX() + getWidth() >
                    getScrollDimension().getWidth()) {
                setScrollX(getScrollDimension().getWidth() - getWidth());
            }
            if(!isScrollableY() && getScrollY() > 0){
                setScrollY(0);
            }
            if(!isScrollableX() && getScrollX() > 0){
                setScrollX(0);
            }
        }
    }

    /**
     * Invoked to indicate that the component initialization is being reversed
     * since the component was detached from the container hierarchy. This allows
     * the component to deregister animators and cleanup after itself. This
     * method is the opposite of the initComponent() method.
     */
    protected void deinitialize() {
    }

    /**
     * Allows subclasses to bind functionality that relies on fully initialized and
     * "ready for action" component state
     */
    protected void initComponent() {
    }

    /**
     * Indicates if the component is in the initialized state, a component is initialized
     * when its initComponent() method was invoked. The initMethod is invoked before showing the
     * component to the user.
     * 
     * @return true if the component is in the initialized state
     */
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * Indicates if the component is in the initialized state, a component is initialized
     * when its initComponent() method was invoked. The initMethod is invoked before showing the
     * component to the user.
     * 
     * @param initialized Indicates if the component is in the initialized state
     */
    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * {@inheritDoc}
     */
    public void styleChanged(String propertyName, Style source) {
        //changing the Font, Padding, Margin may casue the size of the Component to Change
        //therefore we turn on the shouldCalcPreferredSize flag
        if ((!shouldCalcPreferredSize &&
                source == getStyle()) &&
                (propertyName.equals(Style.FONT) ||
                propertyName.equals(Style.MARGIN) ||
                propertyName.equals(Style.PADDING))) {
            setShouldCalcPreferredSize(true);
            Container parent = getParent();
            if (parent != null && parent.getComponentForm() != null) {
                parent.revalidate();
            }
        }
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     * 
     * @return the next focus component
     */
    public Component getNextFocusDown() {
        return nextFocusDown;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     * 
     * @param nextFocusDown the next focus component
     */
    public void setNextFocusDown(Component nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the up key. 
     * 
     * @return the nxt focus component
     */
    public Component getNextFocusUp() {
        return nextFocusUp;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the up key, this method doesn't affect the general focus behavior.
     * 
     * @param nextFocusUp next focus component
     */
    public void setNextFocusUp(Component nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the left key. 
     * 
     * @return the next focus component
     */
    public Component getNextFocusLeft() {
        return nextFocusLeft;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the left key, this method doesn't affect the general focus behavior.
     * 
     * @param nextFocusLeft the next focus component
     */
    public void setNextFocusLeft(Component nextFocusLeft) {
        this.nextFocusLeft = nextFocusLeft;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the right key
     * 
     * @return the next focus component
     */
    public Component getNextFocusRight() {
        return nextFocusRight;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the right key
     * 
     * @param nextFocusRight the next focus component
     */
    public void setNextFocusRight(Component nextFocusRight) {
        this.nextFocusRight = nextFocusRight;
    }

    /**
     * Indicates whether component is enabled or disabled thus allowing us to prevent
     * a component from receiving input events and indicate so visually
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Used to reduce coupling between the {@link TextArea} component and display/implementation
     * classes thus reduce the size of the hello world 
     * 
     * @param text text after editing is completed
     */
    void onEditComplete(String text) {
    }

    /**
     * Indicates whether component is enabled or disabled thus allowing us to prevent
     * a component from receiving input events and indicate so visually
     * 
     * @param enabled true to enable false to disable
     */
    public void setEnabled(boolean enabled) {
        if(this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        repaint();
    }

    /**
     * A component name allows us to easily identify the component within a dynamic
     * UI.
     *
     * @return name of the component
     */
    public String getName() {
        return name;
    }

    /**
     * A component name allows us to easily identify the component within a dynamic
     * UI.
     *
     * @param name a name for the component
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Allows components to create a style of their own, this method binds the listener
     * to the style and installs a bg painter
     *
     * @param s style to initialize
     */
    protected void initCustomStyle(Style s) {
        s.addStyleListener(this);
        if (s.getBgPainter() == null) {
            s.setBgPainter(new BGPainter());
        }
    }

    /**
     * Allows components to create a style of their own, this method cleans up
     * state for the given style
     *
     * @param s style no longer used
     */
    protected void deinitializeCustomStyle(Style s) {
        s.removeStyleListener(this);
    }

    /**
     * Is the component a bidi RTL component
     *
     * @return true if the component is working in a right to left mode
     */
    public boolean isRTL() {
        return rtl;
    }

    /**
     * Is the component a bidi RTL component
     *
     * @param rtl true if the component should work in a right to left mode
     */
    public void setRTL(boolean rtl) {
        this.rtl = rtl;
    }

    /**
     * Elaborate components might not provide tactile feedback for all their areas (e.g. Lists)
     * this method defaults to returning the value of isTactileTouch
     * 
     * @param x the x position
     * @param y the y position
     * @return True if the device should vibrate
     */
    protected boolean isTactileTouch(int x, int y) {
        return isTactileTouch();
    }

    /**
     * Indicates whether the component should "trigger" tactile touch when pressed by the user
     * in a touch screen UI.

     * @return the tactileTouch
     */
    public boolean isTactileTouch() {
        return tactileTouch;
    }

    /**
     * Indicates whether the component should "trigger" tactile touch when pressed by the user
     * in a touch screen UI.
     *
     * @param tactileTouch true to trigger vibration when the component is pressed
     */
    public void setTactileTouch(boolean tactileTouch) {
        this.tactileTouch = tactileTouch;
    }

    /**
     * A component may expose mutable property names for a UI designer to manipulate, this
     * API is designed for usage internally by the GUI builder code
     * 
     * @return the property names allowing mutation
     */
    public String[] getPropertyNames() {
        return null;
    }

    /**
     * Matches the property names method (see that method for further details).
     *
     * @return the types of the properties
     */
    public Class[] getPropertyTypes() {
       return null;
    }

    /**
     * This method is here to workaround an XMLVM array type bug where property types aren't
     * identified properly, it returns the names of the types using the following type names:
     * String,int,double,long,byte,short,char,String[],String[][],byte[],Image,Image[],Object[],ListModel,ListCellRenderer
     * @return Array of type names
     */
    public String[] getPropertyTypeNames() {
        return null;
    }
    
    /**
     * Returns the current value of the property name, this method is used by the GUI builder
     *
     * @param name the name of the property
     * @return the value of said property
     */
    public Object getPropertyValue(String name) {
        return null;
    }


    /**
     * Sets a new value to the given property, returns an error message if failed
     * and null if successful. Notice that some builtin properties such as "$designMode" might be sent
     * to components to indicate application state.
     *
     * @param name the name of the property
     * @param value new value for the property
     * @return error message or null
     */
    public String setPropertyValue(String name, Object value) {
        return "Unknown: " + name;
    }

    /**
     * Releases the paint lock image to allow paint to work as usual, see paintLock(boolean)
     * for details
     */
    public void paintLockRelease() {
        paintLockImage = null;
    }

    private static boolean paintLockEnableChecked;
    private static boolean paintLockEnabled;
    /**
     * This method locks the component so it will always paint the given image
     * instead of running through its paint logic. This is useful when running
     * transitions that might be quite expensive on the device. A lock should 
     * be released using paintLockRelease(), it is implicitly released when 
     * a component is deinitialized although a component doesn't need to be initialized
     * to be locked!<br>
     * If the component is not opaque null is always returned!
     * <p>Duplicate calls to this method won't produce duplicate locks, in case of 
     * a soft lock the return value will always be null.
     * 
     * @param hardLock indicates whether the lock uses a hard or a soft reference to the image
     * @return the image in case of a hard lock
     */
    public Image paintLock(boolean hardLock) {
        if(!paintLockEnableChecked) {
            paintLockEnableChecked = true;
            paintLockEnabled = Display.getInstance().getProperty("paintLockEnabled", "true").equals("true");
        }
        if(!paintLockEnabled || !Display.getInstance().areMutableImagesFast()) {
            return null;
        }
        if((getStyle().getBgTransparency() & 0xff) != 0xff) {
            return null;
        }
        if(paintLockImage == null) {
            paintLockImage = Image.createImage(getWidth(), getHeight());
            int x = getX();
            int y = getY();
            setX(0);
            setY(0);
            paintInternalImpl(((Image)paintLockImage).getGraphics(), false);
            setX(x);
            setY(y);
            if(hardLock) {
                return (Image)paintLockImage;
            } else {
                paintLockImage = Display.getInstance().createSoftWeakRef(paintLockImage);
            }
        } else {
            if(hardLock) {
                return (Image)paintLockImage;
            }
        }
        return null;
    }
    
    /**
     * This is a callback method for the peer component class
     */
    void setLightweightMode(boolean l) {
    }

    /**
     * Indicates whether scrolling this component should jump to a specific location
     * in a grid
     *
     * @return the snapToGrid
     */
    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    /**
     * Indicates whether scrolling this component should jump to a specific location
     * in a grid
     * @param snapToGrid the snapToGrid to set
     */
    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    /**
     * A component that might need side swipe such as the slider
     * could block it from being used for some other purpose when
     * on top of said component.
     */
    protected boolean shouldBlockSideSwipe() {
        return isScrollableX() || (parent != null && parent.shouldBlockSideSwipe());
    }

    /**
     * Makes the component effectively opaque by blending the backgrounds into an image in memory so the layer of underlying components
     * is only drawn once when this component is repainted. This does have a significant memory overhead.
     *
     * @return the flatten property
     */
    public boolean isFlatten() {
        return flatten;
    }

    /**
     * Makes the component effectively opaque by blending the backgrounds into an image in memory so the layer of underlying components
     * is only drawn once when this component is repainted. This does have a significant memory overhead.
     *
     * @param flatten the flatten value
     */
    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }


    /**
     * Recommended length for the tensile, -1 for default
     * @return Recommended length for the tensile, -1 for default
     */
    public int getTensileLength() {
        return tensileLength;
    }

    /**
     * Recommended length for the tensile, -1 for default
     *
     * @param tensileLength length for tensile drag
     */
    public void setTensileLength(int tensileLength) {
        this.tensileLength = tensileLength;
    }

    Label getHintLabelImpl() {
        return null;
    }

    void setHintLabelImpl(Label hintLabel) {
    }

    boolean shouldShowHint() {
        return false;
    }

    void paintHint(Graphics g) {
        Label hintLabel = getHintLabelImpl();
        if (hintLabel != null && shouldShowHint()) {
            switch(hintLabel.getVerticalAlignment()) {
                case TOP:
                    hintLabel.setHeight(hintLabel.getPreferredH());
                    hintLabel.setY(getY());
                    break;
                default:
                    hintLabel.setHeight(getHeight());
                    hintLabel.setY(getY());
                    break;
            }
            hintLabel.setX(getX());
            hintLabel.setWidth(getWidth());
            hintLabel.paint(g);
        }
    }

    String getHint() {
        Label hintLabel = getHintLabelImpl();
        if(hintLabel != null) {
            return hintLabel.getText();
        }
        return null;
    }

    /**
     * Returns the hint icon
     *
     * @return the hint icon
     */
    Image getHintIcon() {
        Label hintLabel = getHintLabelImpl();
        if(hintLabel != null) {
            return hintLabel.getIcon();
        }
        return null;
    }

    /**
     * Sets the hint text and Icon, the hint text and icon are
     * displayed on the component when it is empty
     *
     * @param hint the hint text to display
     * @param icon the hint icon to display
     */
    void setHint(String hint, Image icon){
        Label hintLabel = getHintLabelImpl();
        if(hintLabel == null){
            hintLabel = new Label(hint);
            hintLabel.setUIID("TextHint");
            setHintLabelImpl(hintLabel);
        }else{
            hintLabel.setText(hint);
        }
        hintLabel.setIcon(icon);
    }

    /**
     * This property is useful for blocking in z-order touch events, sometimes we might want to grab touch events in
     * a specific component without making it focusable.
     *
     * @return the grabsPointerEvents
     */
    public boolean isGrabsPointerEvents() {
        return grabsPointerEvents;
    }

    /**
     * This property is useful for blocking in z-order touch events, sometimes we might want to grab touch events in
     * a specific component without making it focusable.
     * 
     * @param grabsPointerEvents the grabsPointerEvents to set
     */
    public void setGrabsPointerEvents(boolean grabsPointerEvents) {
        this.grabsPointerEvents = grabsPointerEvents;
    }

    /**
     * Indicates the decrement units for the scroll opacity
     * @return the scrollOpacityChangeSpeed
     */
    public int getScrollOpacityChangeSpeed() {
        return scrollOpacityChangeSpeed;
    }

    /**
     * Indicates the decrement units for the scroll opacity
     * @param scrollOpacityChangeSpeed the scrollOpacityChangeSpeed to set
     */
    public void setScrollOpacityChangeSpeed(int scrollOpacityChangeSpeed) {
        this.scrollOpacityChangeSpeed = scrollOpacityChangeSpeed;
    }

    /**
     * Grows or shrinks this component to its new preferred size, this method
     * essentially takes a component whose preferred size has changed and creates a "growing"
     * effect that lasts for the duration. Notice that some components (such as text areas)
     * don't report proper preferred size untill they are laid out once. Hence the first time
     * around a text area (or container containing a text area) will not produce the expected
     * effect. This can be solved by invoking revalidate before the call to this method only the
     * first time around!
     *
     * @param duration the duration in milliseconds for the grow/shrink animation
     */
    public void growShrink(int duration) {
        Motion wMotion = Motion.createSplineMotion(getWidth(), getPreferredW(), duration);
        Motion hMotion = Motion.createSplineMotion(getHeight(), getPreferredH(), duration);
        wMotion.start();
        hMotion.start();
        setPreferredSize(new Dimension(getWidth(), getHeight()));
        // we are using bgpainter just to save the cost of creating another class
        getComponentForm().registerAnimated(new BGPainter(wMotion, hMotion));
        getComponentForm().revalidate();
    }

    /**
     * Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
     * @return the alwaysTensile
     */
    public boolean isAlwaysTensile() {
        return alwaysTensile && !isScrollableX() || refreshTask != null;
    }

    /**
     * Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
     * @param alwaysTensile the alwaysTensile to set
     */
    public void setAlwaysTensile(boolean alwaysTensile) {
        this.alwaysTensile = alwaysTensile;
    }

    /**
     * Indicates whether this component can be dragged in a drag and drop operation rather than scroll the parent
     * @return the draggable state
     */
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * Indicates whether this component can be dragged in a drag and drop operation rather than scroll the parent
     * @param draggable the draggable to set
     */
    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    /**
     * Indicates whether this component can receive dropped components into it, notice that when dropping on a component
     * or container the parents will be checked recursively to find a valid drop target
     * @return the dropTarget state
     */
    public boolean isDropTarget() {
        return dropTarget;
    }

    /**
     * Indicates whether this component can receive dropped components into it, notice that when dropping on a component
     * or container the parents will be checked recursively to find a valid drop target
     * 
     * @param dropTarget the dropTarget to set
     */
    public void setDropTarget(boolean dropTarget) {
        this.dropTarget = dropTarget;
    }

    /**
     * Indicates that this component and all its children should be hidden when the device is switched to portrait mode
     * @return the hideInPortrait
     */
    public boolean isHideInPortrait() {
        return hideInPortrait;
    }

    /**
     * Indicates that this component and all its children should be hidden when the device is switched to portrait mode
     * @param hideInPortrait set to true in order to hide when in portrait 
     */
    public void setHideInPortrait(boolean hideInPortrait) {
        this.hideInPortrait = hideInPortrait;
    }
    
    /**
     * remove this component from the painting queue
     */
    protected void cancelRepaints() {
        Display.impl.cancelRepaint(this);
    }

    /**
     * Returns the names of the properties within this component that can be bound for persistence,
     * the order of these names mean that the first one will be the first bound
     * @return a string array of property names or null
     */
    public String[] getBindablePropertyNames() {
        return null;
    }
    
    /**
     * Returns the types of the properties that are bindable within this component
     * @return the class for binding
     */
    public Class[] getBindablePropertyTypes() {
        return null;
    }
    
    /**
     * Binds the given property name to the given bind target
     * @param prop the property name
     * @param target the target binder
     */
    public void bindProperty(String prop, BindTarget target) {
    }
    
    /**
     * Removes a bind target from the given property name
     * @param prop the property names
     * @param target the target binder
     */
    public void unbindProperty(String prop, BindTarget target) {
    }
    
    /**
     * Allows the binding code to extract the value of the property
     * @param prop the property
     * @return the value for the property
     */
    public Object getBoundPropertyValue(String prop) {
        return null;
    }

    /**
     * Sets the value of a bound property within this component, notice that this method MUST NOT fire
     * the property change event when invoked to prevent recursion!
     * 
     * @param prop the property whose value should be set
     * @param value the value
     */
    public void setBoundPropertyValue(String prop, Object value) {
    }
    
    /**
     * Indicates the property within this component that should be bound to the cloud object
     * @return the cloudBoundProperty
     */
    public String getCloudBoundProperty() {
        if(noBind && cloudBoundProperty == null) {
            return null;
        }
        if(cloudBoundProperty == null) {
            String[] props = getBindablePropertyNames();
            if(props != null && props.length > 0) {
                return props[0];
            }
        }
        return cloudBoundProperty;
    }

    /**
     * Indicates the property within this component that should be bound to the cloud object
     * @param cloudBoundProperty the cloudBoundProperty to set
     */
    public void setCloudBoundProperty(String cloudBoundProperty) {
        this.cloudBoundProperty = cloudBoundProperty;
        if(cloudBoundProperty == null || this.cloudBoundProperty.length() == 0) {
            noBind = true;
            this.cloudBoundProperty = null;
        }
    }

    /**
     * The destination property of the CloudObject
     * 
     * @return the cloudDestinationProperty
     */
    public String getCloudDestinationProperty() {
        if(cloudDestinationProperty == null || cloudDestinationProperty.length() == 0) {
            return getName();
        }
        return cloudDestinationProperty;
    }

    /**
     * The destination property of the CloudObject
     * @param cloudDestinationProperty the cloudDestinationProperty to set
     */
    public void setCloudDestinationProperty(String cloudDestinationProperty) {
        this.cloudDestinationProperty = cloudDestinationProperty;
    }
    
    /**
     * Some components may optionally generate a state which can then be restored
     * using setCompnentState(). This method is used by the UIBuilder.
     * @return the component state or null for undefined state.
     */
    public Object getComponentState() {
        return null;
    }

    /**
     * Makes the components preferred size equal 0 when hidden and restores it to the default size when not.
     * This method also optionally sets the margin to 0 so the component will be truly hidden
     * 
     * @param b true to hide the component and false to show it
     * @param changeMargin indicates margin should be set to 0
     */
    public void setHidden(boolean b, boolean changeMargin) {
        if(b) {
            if(!sizeRequestedByUser) {
                if(changeMargin) {
                    getAllStyles().setMargin(0, 0, 0, 0);
                }
                setPreferredSize(new Dimension());
            }
        } else {
            setPreferredSize(null);
            if(changeMargin) {
                if(getUnselectedStyle().getMargin(LEFT) == 0) {
                    setUIID(getUIID());
                }
            }
        }
    }
    
    /**
     * Makes the components preferred size equal 0 when hidden and restores it to the default size when not.
     * Also toggles the UIID to "Container" and back to allow padding/margin to be removed. Since the visible flag
     * just hides the component without "removing" the space it occupies this is the flag that can be used to truly
     * hide a component within the UI.
     * 
     * @param b true to hide the component and false to show it
     */
    public void setHidden(boolean b) {
        setHidden(b, true);
    }    
    
    /**
     * Returns true if the component was explicitly hidden by the user
     * @return true if the component is hidden, notice that the hidden property and visible property have different meanings in the API!
     */
    public boolean isHidden() {
        return sizeRequestedByUser && preferredSize != null && preferredSize.getWidth() == 0 && preferredSize.getHeight() == 0;
    }
    
    /**
     * If getComponentState returned a value the setter can update the value and restore
     * the prior state.
     * @param state the non-null state
     */
    public void setComponentState(Object state) {
    }
    
    class BGPainter implements Painter, Animation {
        private Motion wMotion, hMotion;
        private Form previousTint;
        private Painter painter;
        Image radialCache;
        private Style constantStyle;
        CodenameOneImplementation impl;

        public BGPainter(Motion wMotion, Motion hMotion) {
            this.wMotion = wMotion;
            this.hMotion = hMotion;
            impl = Display.impl;
        }

        public BGPainter() {
            impl = Display.impl;
        }

        public BGPainter(Style s) {
            constantStyle = s;
            impl = Display.impl;
        }

        public BGPainter(Form parent, Painter p) {
            this.painter = p;
            impl = Display.impl;
        }

        public void setPreviousForm(Form previous) {
            previousTint = previous;
        }

        public Form getPreviousForm() {
            return previousTint;
        }

        public void paint(Graphics g, Rectangle rect) {
            if (painter != null) {
                if (previousTint != null) {
                    previousTint.paint(g);
                }
            } else {
                Style s;
                if(constantStyle != null) {
                    s = constantStyle;
                } else {
                    s = getStyle();
                }
                int x = rect.getX() + g.getTranslateX();
                int y = rect.getY() + g.getTranslateY();
                int width = rect.getSize().getWidth();
                int height = rect.getSize().getHeight();
                Image img = s.getBgImage();
                if(img != null && img.requiresDrawImage()) {
                    // damn no native painting...
                    int iW = img.getWidth();
                    int iH = img.getHeight();
                    switch (s.getBackgroundType()) {
                        case Style.BACKGROUND_IMAGE_SCALED:
                            if (Display.impl.isScaledImageDrawingSupported()) {
                                g.drawImage(img, x, y, width, height);
                            } else {
                                if (iW != width || iH != height) {
                                    img = img.scaled(width, height);
                                    s.setBgImage(img, true);
                                }
                                g.drawImage(img, x, y);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_SCALED_FILL:
                            float r = Math.max(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                            int bwidth = (int) (((float) iW) * r);
                            int bheight = (int) (((float) iH) * r);
                            if (Display.impl.isScaledImageDrawingSupported()) {
                                g.drawImage(img, x + (width - bwidth) / 2, y + (height - bheight) / 2, bwidth, bheight);
                            } else {
                                if (iW != bwidth || iH != bheight) {
                                    img = img.scaled(bwidth, bheight);
                                    s.setBgImage(img, true);
                                }
                                g.drawImage(img, x + (width - bwidth) / 2, y + (height - bheight) / 2);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_SCALED_FIT:
                            if (s.getBgTransparency() != 0) {
                                g.setColor(s.getBgColor());
                                g.fillRect(x, y, width, height, s.getBgTransparency());
                            }
                            float r2 = Math.min(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                            int awidth = (int) (((float) iW) * r2);
                            int aheight = (int) (((float) iH) * r2);
                            if (Display.impl.isScaledImageDrawingSupported()) {
                                g.drawImage(img, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                            } else {
                                if (iW != awidth || iH != aheight) {
                                    img = img.scaled(awidth, aheight);
                                    s.setBgImage(img, true);
                                }
                                g.drawImage(img, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_BOTH:
                            g.tileImage(img, x, y, width, height);
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.tileImage(img, x, y, width, iH);
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.tileImage(img, x, y + (height / 2 - iH / 2), width, iH);
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.tileImage(img, x, y + (height - iH), width, iH);
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int yPos = 0; yPos <= height; yPos += iH) {
                                g.drawImage(img, x, y + yPos);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int yPos = 0; yPos <= height; yPos += iH) {
                                g.drawImage(img, x + (width / 2 - iW / 2), y + yPos);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int yPos = 0; yPos <= height; yPos += iH) {
                                g.drawImage(img, x + width - iW, y + yPos);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x + (width / 2 - iW / 2), y);
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x + (width / 2 - iW / 2), y + (height - iH));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x, y + (height / 2 - iH / 2));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x + width - iW, y + (height / 2 - iH / 2));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x + (width / 2 - iW / 2), y + (height / 2 - iH / 2));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x, y);
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x + width - iW, y);
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x, y + (height - iH));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(img, x + width - iW, y + (height - iH));
                            return;
                    }
                } 
                
                impl.paintComponentBackground(g.getGraphics(), x, y, width, height, s);
            }
        }

        public boolean animate() {
            if(wMotion.isFinished() && hMotion.isFinished()) {
                getComponentForm().deregisterAnimated(this);
                setPreferredSize(null);
                getComponentForm().revalidate();
                return false;
            }
            setPreferredSize(new Dimension(wMotion.getValue(), hMotion.getValue()));
            getComponentForm().revalidate();
            return false;
        }

        public void paint(Graphics g) {
        }
    }
}
