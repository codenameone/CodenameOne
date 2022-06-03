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
import com.codename1.components.InfiniteProgress;
import com.codename1.components.InteractionDialog;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.TextSelection.TextSelectionSupport;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * <p>The component class is the basis of all UI widgets in Codename One, to arrange multiple components 
 * together we use the Container class which itself "IS A" Component subclass. The Container is a 
 * Component that contains Components effectively allowing us to nest Containers infinitely to build any type 
 * of visual hierarchy we want by nesting Containers.
 * </p>
 * 
 * <h3>Style Change Events</h3>
 * 
 * <p>Styles fire a change event for each style change that occurs.  {@link Component} listens to all changes events
 * of its styles, and adjusts some of its properties accordingly.  Currently (as of 6.0) each style change will trigger
 * a {@link Container#revalidate() } call on the Style's Component's parent container, which is expensive.  You can disable this
 * {@link Container#revalidate() } call by calling {@link CN.setProperty("Component.revalidateOnStyleChange", "false")}.  This will 
 * likely be the default behavior in a future version, so we recommend you disable this explicitly for both performance reasons, and
 * to avoid regressions when the default is changed.</p>
 * 
 * @see Container
 * @author Chen Fishbein
 */
public class Component implements Animation, StyleListener, Editable {
    
    private int tabIndex;
    // -1 = the element should be focusable, but should not be reachable via sequential keyboard navigation. Mostly useful to create accessible widgets 
    // 0 =  the element should be focusable in sequential keyboard navigation, but its order is defined by the container's source order.
    
    private int preferredTabIndex=-1;
    
    /**
     * Indicates whether the component displays the material design ripple effect
     */
    private boolean rippleEffect;    
    
    /**
     * The default cursor
     */
    public static final int DEFAULT_CURSOR = 0;

    /**
     * The crosshair cursor type.
     */
    public static final int CROSSHAIR_CURSOR = 1;

    /**
     * The text cursor type.
     */
    public static final int TEXT_CURSOR = 2;

    /**
     * The wait cursor type.
     */
    public static final int WAIT_CURSOR = 3;

    /**
     * The south-west-resize cursor type.
     */
    public static final int SW_RESIZE_CURSOR = 4;

    /**
     * The south-east-resize cursor type.
     */
    public static final int SE_RESIZE_CURSOR = 5;

    /**
     * The north-west-resize cursor type.
     */
    public static final int NW_RESIZE_CURSOR = 6;

    /**
     * The north-east-resize cursor type.
     */
    public static final int NE_RESIZE_CURSOR = 7;

    /**
     * The north-resize cursor type.
     */
    public static final int N_RESIZE_CURSOR = 8;

    /**
     * The south-resize cursor type.
     */
    public static final int S_RESIZE_CURSOR = 9;

    /**
     * The west-resize cursor type.
     */
    public static final int W_RESIZE_CURSOR = 10;

    /**
     * The east-resize cursor type.
     */
    public static final int E_RESIZE_CURSOR = 11;

    /**
     * The hand cursor type.
     */
    public static final int HAND_CURSOR = 12;

    /**
     * The move cursor type.
     */
    public static final int MOVE_CURSOR = 13;
    
    private int cursor;


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
    
    /**
     * Used by getDragRegionStatus to indicate immediate dragability
     */
    public static final int DRAG_REGION_IMMEDIATELY_DRAG_X = 31;
    
    /**
     * Used by getDragRegionStatus to indicate immediate dragability
     */
    public static final int DRAG_REGION_IMMEDIATELY_DRAG_Y = 32;
    
    /**
     * Used by getDragRegionStatus to indicate immediate dragability
     */
    public static final int DRAG_REGION_IMMEDIATELY_DRAG_XY = 33;
    
    private String selectText;
    private boolean alwaysTensile;
    private int tensileLength = -1;

    /**
     * Prevent a lead component hierarchy from this specific component, this allows a component within that 
     * hierarchy to still act as a standalone component
     */
    private boolean blockLead;
    
    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     */
    private Component nextFocusDown;
    private Component nextFocusUp;
    
    private Editable editingDelegate;
    
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
     * The elevation at which this component was rendered in its last rendering.
     * @since 8.0
     */
    int renderedElevation;

    /**
     * The index at which this component was rendered in its last rendering.  This acts as a z-index within
     * an elevation layer.
     * @since 8.0
     */
    int renderedElevationComponentIndex;

    /**
     * A flag to toggle between lightweight elevation shadow generation and heavyweight generation.  The lightweight
     * does the work entirely in CN1 and it cuts corners.  In simulator, it turns out that the heavyweight implementation
     * is too slow to be useful.  This may not be the case on other platforms, but, for now, we'll leave this flag on.
     * Later on, after evaluation, this flag will likely be removed, and the best strategy will be decided upon.
     */
    private boolean useLightweightElevationShadow = true;
    
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
    private final Rectangle bounds = new Rectangle(0, 0, new Dimension(0, 0));
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
    private Component owner;
    private boolean focused = false;
    private boolean handlesInput = false;
    boolean shouldCalcPreferredSize = true;
    boolean shouldCalcScrollSize = true;
    private boolean focusable = true;
    private boolean isScrollVisible = true;
    private boolean repaintPending;
    private boolean snapToGrid;
    
    /**
     * A flag to dictate whether style changes should trigger a revalidate() call
     * on the component's parent.  Eventually we would like to phase this to be {@literal false}
     * but for now, we'll leave it as {@literal true}.
     * 
     * Users can disable this with {@code CN.setProperty("Component.revalidateOnStyleChange", "false")}.
     */
    static boolean revalidateOnStyleChange=true;
    
    // A flag to indicate whether to paint the component's background.
    // Setting this to false will cause the component's background to not be painted.
    private boolean opaque=true;

    private boolean hideInPortrait;

    /**
     * Indicates that this component and all its children should be hidden when the device is switched to landscape mode
     */
    private boolean hideInLandscape;
    private int scrollOpacity = 0xff;
    private boolean ignorePointerEvents;
            
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
    
    // Reference that is only filled when a drag motion is a decelration motion
    // for tensile scrolling
    private Motion decelerationMotion;

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
    private int pullY;
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
    private String portraitUiid;
    private String landscapeUiid;
    
    private Resources inlineStylesTheme;
    private String inlineAllStyles;
    private String inlinePressedStyles;
    private String inlineDisabledStyles;
    private String inlineSelectedStyles;
    private String inlineUnselectedStyles;

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
    private ActionListener<?> refreshTaskDragListener;
    
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
    EventDispatcher dragFinishedListeners;
    EventDispatcher longPressListeners;
    private EventDispatcher stateChangeListeners;
    boolean isUnselectedStyle;
    
    private String tooltip;
    
    boolean isDragAndDropInitialized() {
        return dragAndDropInitialized;
    }
    
    /**
     * Sets the editing delegate for this component.  The editing delegate allows you to define the 
     * editing workflow for a component.  If a delegate is registered, then editing methods such as 
     * {@link #isEditable() }, {@link #isEditing() }, {@link #startEditingAsync() }, and {@link #stopEditing(java.lang.Runnable) }
     * will be delegated to the delegate object.
     * @param editable An editable delegate.
     * @since 6.0
     */
    public void setEditingDelegate(Editable editable) {
        this.editingDelegate = editable;
    }
    
    /**
     * Gets the delegate that handles the editing of this component.
     * @return The editing delegate for this component.
     * @since 6.0
     */
    public Editable getEditingDelegate() {
        return this.editingDelegate;
    }
    

    /**
     * Sets a custom mouse cursor for this component if the platform supports mouse cursors, notice that this isn't applicable for touch devices.  
     * This will only be used if the platform supports custom cursors.  
     * You can call {@link #isSetCursorSupported() } to find out.
     * 
     * <p><strong>Note:</strong> Since cursors incur some overhead, they are turned off at the form level by default.
     * If you want your custom cursors to be used, then you'll need to enable cursors in the form using {@link Form#setEnableCursors(boolean) }.</p>
     * @param cursor The cursor to set on this component.  One of {@link #DEFAULT_CURSOR}, {@link #CROSSHAIR_CURSOR}, {@link #TEXT_CURSOR},
     * {@link #WAIT_CURSOR}, {@link #SW_RESIZE_CURSOR}, {@link #SE_RESIZE_CURSOR}, {@link #S_RESIZE_CURSOR}, {@link #NE_RESIZE_CURSOR},
     * {@link #NW_RESIZE_CURSOR}, {@link #W_RESIZE_CURSOR}, {@link #HAND_CURSOR}, or {@link #MOVE_CURSOR}.
     * 
     * @see Form#setEnableCursors(boolean) 
     * @see Form#isEnableCursors() 
     * 
     */
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }
    
    /**
     * Gets the custom cursor for this component.  This will only be used if the platform supports custom cursors.  
     * You can call {@link #isSetCursorSupported() } to find out.
     * @return The cursor to set on this component.  One of {@link #DEFAULT_CURSOR}, {@link #CROSSHAIR_CURSOR}, {@link #TEXT_CURSOR},
     * {@link #WAIT_CURSOR}, {@link #SW_RESIZE_CURSOR}, {@link #SE_RESIZE_CURSOR}, {@link #S_RESIZE_CURSOR}, {@link #NE_RESIZE_CURSOR},
     * {@link #NW_RESIZE_CURSOR}, {@link #W_RESIZE_CURSOR}, {@link #HAND_CURSOR}, or {@link #MOVE_CURSOR}.
     * 
     * 
     */
    public int getCursor() {
        return this.cursor;
    }
    
    /**
     * This is identical to invoking {@link #sameWidth} followed by {@link #sameHeight}
     * 
     * @param c the components to group together, this will override all previous width/height grouping
     */
    public static void setSameSize(Component... c) {
        setSameWidth(c);
        setSameHeight(c);
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
     * The native overlay object.  Used in Javascript port for some components so that there is 
     * an inivisible "native" peer overlaid on the component itself to catch events.  E.g.
     * TextFields on iOS can't be programmatically focused except through a user-initiated event -
     * but since CN1 runs on the EDT, CN1 events aren't considered user-initiated so we can't create
     * a native text editor on demand the way we do in desktop port - the native text editor must
     * be *always* present.
     */
    private Object nativeOverlay = null;
    
    /**
     * Creates the native overlay for this component. A native overlay is used on some platforms (e.g. Javascript)
     * to help with user interaction of the component in a native way.
     * @see #hideNativeOverlay() 
     * @see #updateNativeOverlay() 
     * @see #getNativeOverlay() 
     */
    protected void showNativeOverlay() {
        if (nativeOverlay == null) {
            nativeOverlay = Display.getInstance().getImplementation().createNativeOverlay(this);
        }
    }

    /**
     * Hides the native overlay for this component.
     * @see #showNativeOverlay() 
     * @see #updateNativeOverlay() 
     * @see #getNativeOverlay() 
     */
    protected void hideNativeOverlay() {
        if (nativeOverlay != null) {
            Display.getInstance().getImplementation().hideNativeOverlay(this, nativeOverlay);
            nativeOverlay = null;
        }
    }

    /**
     * Updates the native overlay for this component.  This is called each time the component
     * is laid out, so it can change the position and visibility to match the current context.
     * @see #showNativeOverlay() 
     * @see #hideNativeOverlay() 
     * @see #getNativeOverlay() 
     */
    protected void updateNativeOverlay() {
        if (nativeOverlay != null) {
            Display.getInstance().getImplementation().updateNativeOverlay(this, nativeOverlay);
        }
    }
    
    /**
     * Gets the native overlay for this component.  May be null. Native overlays are used in the Javascript
     * port to assist with user interaction on touch devices.  Text fields use native overlays to position
     * an invisible native text field above themselves so that the keyboard will be activated properly when
     * the user taps the text field.
     * @return The native overlay
     */
    public Object getNativeOverlay() {
        return nativeOverlay;
    }
    
    
    
    /**
     * Checks to see if this platform supports cursors.  If the platform doesn't support cursors then any cursors
     * set with {@link #setCursor(int) } will simply be ignored.
     * @return True if the platform supports custom cursors.
     */
    public static boolean isSetCursorSupported() {
        return Display.getInstance().getImplementation().isSetCursorSupported();
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
        setCursor(DEFAULT_CURSOR);
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

    /**
     * Gets the UIID that would be used for this component if inline styles are used.
     * Generally this UIID follows the format: {@literal id[name]} where "id" is the UIID of
     * the component, and "name" is the name of the component. 
     * @return the style text or null
     * @see #getInlineStylesUIID()
     */
    private String getInlineStylesUIID() {
        return getUIID()+"["+getName()+"]";
    }
    
    /**
     * Gets the UIID that would be used for this component if inline styles are used.
     * Generally this UIID follows the format: {@literal id[name]} where "id" is the UIID of
     * the component, and "name" is the name of the component. 
     * @param id UIID to use as the base.
     * @return the style text or null
     * @see #getInlineStylesUIID() 
     */
    private String getInlineStylesUIID(String id) {
        return id +"["+getName()+"]";
    }
    
    /**
     * Checks to see if the component has any inline styles registered for its unselected state.
     * @return True if the component has inline styles registered for the unselected state.  
     */
    private boolean hasInlineUnselectedStyle() {
        return getInlineStylesTheme() != null && (inlineAllStyles != null || inlineUnselectedStyles != null);
    }
    
    /**
     * Checks to see if the component has any inline styles registered for its pressed state.
     * @return True if the component has inline styles registered for the pressed state.  
     */
    private boolean hasInlinePressedStyle() {
        return getInlineStylesTheme() != null && (inlineAllStyles != null || inlinePressedStyles != null);
    }
    
    /**
     * Checks to see if the component has any inline styles registered for its disabled state.
     * @return True if the component has inline styles registered for the disabled state.  
     */
    private boolean hasInlineDisabledStyle() {
        return getInlineStylesTheme() != null && (inlineAllStyles != null || inlineDisabledStyles != null);
    }
    
    /**
     * Checks to see if the component has any inline styles registered for its selected state.
     * @return True if the component has inline styles registered for the selected state.  
     */
    private boolean hasInlineSelectedStyle() {
        return getInlineStylesTheme() != null && (inlineAllStyles != null || inlineSelectedStyles != null);
    }
    
    /**
     * Gets array of style strings to be used for inline unselected style. This may include
     * the {@link #inlineAllStyles} string and/or the {@link #inlineUnselectedStyles} string.
     * @return Array of inline style strings to be applied to pressed state.  Or null if
     * none specified.
     */
    private String[] getInlineUnselectedStyleStrings() {
        if (inlineAllStyles != null) {
            if (inlineUnselectedStyles != null) {
                return new String[] {inlineAllStyles, inlineUnselectedStyles};
            } else {
                return new String[] {inlineAllStyles};
            }
        } else {
            if (inlineUnselectedStyles != null) {
                return new String[]{inlineUnselectedStyles};
            } else {
                return null;
            }
                    
        }
    }
    
    /**
     * Gets array of style strings to be used for inline selected style. This may include
     * the {@link #inlineAllStyles} string and/or the {@link #inlineSelectedStyles} string.
     * @return Array of inline style strings to be applied to pressed state.  Or null if
     * none specified.
     */
    private String[] getInlineSelectedStyleStrings() {
        if (inlineAllStyles != null) {
            if (inlineSelectedStyles != null) {
                return new String[] {inlineAllStyles, inlineSelectedStyles};
            } else {
                return new String[] {inlineAllStyles};
            }
        } else {
            if (inlineSelectedStyles != null) {
                return new String[]{inlineSelectedStyles};
            } else {
                return null;
            }
                    
        }
    }
    
    /**
     * Gets array of style strings to be used for inline pressed style. This may include
     * the {@link #inlineAllStyles} string and/or the {@link #inlinePressedStyles} string.
     * @return Array of inline style strings to be applied to pressed state.  Or null if
     * none specified.
     */
    private String[] getInlinePressedStyleStrings() {
        if (inlineAllStyles != null) {
            if (inlinePressedStyles != null) {
                return new String[] {inlineAllStyles, inlinePressedStyles};
            } else {
                return new String[] {inlineAllStyles};
            }
        } else {
            if (inlinePressedStyles != null) {
                return new String[]{inlinePressedStyles};
            } else {
                return null;
            }
                    
        }
    }
    
    /**
     * Gets array of style strings to be used for inline disabled style. This may include
     * the {@link #inlineAllStyles} string and/or the {@link #inlineDisabledStyles} string.
     * @return Array of inline style strings to be applied to disabled state.  Or null if
     * none specified.
     */
    private String[] getInlineDisabledStyleStrings() {
        if (inlineAllStyles != null) {
            if (inlineDisabledStyles != null) {
                return new String[] {inlineAllStyles, inlineDisabledStyles};
            } else {
                return new String[] {inlineAllStyles};
            }
        } else {
            if (inlineDisabledStyles != null) {
                return new String[]{inlineDisabledStyles};
            } else {
                return null;
            }
                    
        }
    }

    void setSurface(boolean surface) {

    }
    
    private void initStyle() {
        if (hasInlineUnselectedStyle()) {
            unSelectedStyle = getUIManager().parseComponentStyle(getInlineStylesTheme(), getUIID(), getInlineStylesUIID(), getInlineUnselectedStyleStrings());
        } else {
            unSelectedStyle = getUIManager().getComponentStyle(getUIID());
        }
        initUnselectedStyle(unSelectedStyle);
        lockStyleImages(unSelectedStyle);
        if (unSelectedStyle != null) {
            if (initialized && unSelectedStyle.getElevation()>0) {
                registerElevatedInternal(this);
            }
            if (initialized) {
                setSurface(unSelectedStyle.isSurface());
            }
            unSelectedStyle.addStyleListener(this);
            if (unSelectedStyle.getBgPainter() == null) {
                unSelectedStyle.setBgPainter(new BGPainter());
            }
            if(cellRenderer) {
                unSelectedStyle.markAsRendererStyle();
            }
        }
        if(disabledStyle != null) {
            if (initialized && disabledStyle.getElevation()>0) {
                registerElevatedInternal(this);
            }
            if (initialized) {
                setSurface(disabledStyle.isSurface());
            }
            disabledStyle.addStyleListener(this);
            if (disabledStyle.getBgPainter() == null) {
                disabledStyle.setBgPainter(new BGPainter());
            }
            if(cellRenderer) {
                disabledStyle.markAsRendererStyle();
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
     * Gets the x-coordinate of the outer bounds of this component.  The outer bounds are formed
     * by the bounds outside the margin of the component.  (i.e. {@code x - leftMargin}).
     * @return The outer X bound.
     */
    public int getOuterX() {
        return getX() - getStyle().getMarginLeftNoRTL();
    }
    
    /**
     * Gets x-coordinate of the inner bounds of this component.  The inner bounds are formed by 
     * the bounds of the padding of the component.  i.e. {@code x + leftPadding}.
     * @return The inner x bound.
     */
    public int getInnerX() {
        return getX() + getStyle().getMarginLeftNoRTL();
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
     * Gets the Y-coordinate of the outer bounds of this component.  The outer bounds are formed
     * by the bound of the margin of the component.  i.e. {@code y - leftMargin}.
     * @return The outer y bound.
     */
    public int getOuterY() {
        return getY() - getStyle().getMarginTop();
    }
    
    /**
     * Gets the inner y-coordinate of the inner bounds of this component. The inner bounds are formed
     * by the bound of the padding of the component.  i.e. {@code y + leftPadding}.
     * @return The inner y bound.
     */
    public int getInnerY() {
        return getY() + getStyle().getPaddingTop();
    }

    /**
     * Returns whether the component is visible or not
     * 
     * @return true if component is visible; otherwise false 
     */
    public boolean isVisible() {
        return visible;
    }
    
    void getVisibleRect(Rectangle r, boolean init) {
        if (!isVisible() || !initialized) {
            r.setWidth(0);
            r.setHeight(0);
            return;
        }
        
        int w = getWidth();
        int h = getHeight();
        int x = getAbsoluteX() + scrollX;
        int y = getAbsoluteY() + scrollY;
        if (init) {
            r.setBounds(x, y, w, h);
            if (w <= 0 || h <= 0) {
                return;
            }
        } else {
            Rectangle.intersection(x, y, w, h, r.getX(), r.getY(), r.getWidth(), r.getHeight(), r);
            if (r.getWidth() <= 0 || r.getHeight() <= 0) {
                return;
            }
        }
        
        
        Container parent = getParent();
        if (parent != null) {
            parent.getVisibleRect(r, false);
            
        }
        
    }
    private static final Rectangle tmpRect = new Rectangle();
    boolean isVisibleOnForm() {
        getVisibleRect(tmpRect, true);
        return (tmpRect.getWidth() > 0 && tmpRect.getHeight() > 0);   
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

    /**
     * Convenience method that strips margin and padding from the component, and
     * returns itself for chaining.
     * @return Self for chaining.
     * @see Style#stripMarginAndPadding() 
     * @since 7.0
     */
    public Component stripMarginAndPadding() {
        getAllStyles().stripMarginAndPadding();
        return this;
        
    }
    
    /**
     * Gets the lead component for this component.  
     * @return The lead component or null if none is found.
     */
    Component getLeadComponent() {
        if(isBlockLead()) {
            return null;
        }
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
     * Sets whether or not to paint the component background.  Default is {@literal true}
     * @param opaque False to not paint the component's background.
     * @since 6.0
     */
    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }
    
    /**
     * Checks whether the component's background should be painted.
     * @return {@literal true} if the component's background should be painted.
     */
    public boolean isOpaque() {
        return opaque;
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
     * Gets the outer width of this component. This is the width of the component including horizontal margins.
     * @return The outer width.
     */
    public int getOuterWidth() {
        return getWidth() + getStyle().getHorizontalMargins();
    }
    
    /**
     * Gets the inner width of this component.  This is the width of the component removing horizontal padding.
     * @return The inner width.
     */
    public int getInnerWidth() {
        return getWidth() - getStyle().getHorizontalPadding();
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
     * Gets the outer height of this component.  This is the height of the component including vertical margins.
     * @return The outer height.
     */
    public int getOuterHeight() {
        return getHeight() + getStyle().getVerticalMargins();
    }
    
    /**
     * Gets the inner height of this component.  This is the height of the component removing vertical padding.
     * @return The inner height.
     */
    public int getInnerHeight() {
        return getHeight() - getStyle().getVerticalPadding();
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
        return height - getStyle().getPaddingBottom();
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
     * Optional string the specifies the preferred size of the component. Format is {@literal <width> <height>} 
     * where {@literal <width>} and {@literal <height>} are both scalar values.  E.g. "15px", "20.5mm", or "inherit"
     * to indicate that it should inherit the value returned from {@link #calcPreferredSize() } for that coordinate.
     */
    private String preferredSizeStr;
    
    /**
     * @deprecated this method shouldn't be used, use sameWidth/Height, padding, margin or override calcPeferredSize
     * to reach similar functionality 
     * @param value The preferred size to set in format "width height", where width and height can be a scalar
     * value with px or mm units. Or the special value "inherit" which will just inherit the default preferred size.
     */
    public void setPreferredSizeStr(String value) {
        preferredSizeStr = value;
        setPreferredSize(null);
    }
    
    /**
     * Returns the preferred size string that can be used to specify the preferred size of the component
     * using pixels or millimetres.  This string is applied to the preferred size just after is is initially
     * calculated using {@link #calcPreferredSize() }. 
     * @return the preferred size string
     * @deprecated This method is primarily for use by the GUI builder.  Use {@link #getPreferredSize() } to find
     * the preferred size of a component.
     */
    public String getPreferredSizeStr() {
        return preferredSizeStr;
    }

    /**
     * Parses the preferred size given as a string
     * @param preferredSize a string representing a width/height preferred size using common units e.g. mm, px etc.
     * @param baseSize used as the starting point for the calculation, typically the preferred size of the component
     * @return the parsed results
     */
    public static Dimension parsePreferredSize(String preferredSize, Dimension baseSize) {
        int spacePos = preferredSize.indexOf(" ");
        if (spacePos == -1) {
            return baseSize;
        }
        String wStr = preferredSize.substring(0, spacePos).trim();
        String hStr = preferredSize.substring(spacePos+1).trim();
        int unitPos;
        float pixelsPerMM = Display.getInstance().convertToPixels(1000f)/1000f;
        try {
            if ((unitPos=wStr.indexOf("mm")) != -1) {
                baseSize.setWidth(Math.round(Float.parseFloat(wStr.substring(0, unitPos))*pixelsPerMM));
            } else if ((unitPos=wStr.indexOf("px")) != -1) {
                baseSize.setWidth(Integer.parseInt(wStr.substring(0, unitPos)));
            } else if (!"inherit".equals(wStr)){
                baseSize.setWidth(Integer.parseInt(wStr));
            }
        } catch (Throwable t){}
        
        try {
            if ((unitPos=hStr.indexOf("mm")) != -1) {
                baseSize.setHeight(Math.round(Float.parseFloat(hStr.substring(0, unitPos))*pixelsPerMM));
            } else if ((unitPos=hStr.indexOf("px")) != -1) {
                baseSize.setHeight(Integer.parseInt(hStr.substring(0, unitPos)));
            } else if (!"inherit".equals(hStr)){
                baseSize.setHeight(Integer.parseInt(hStr));
            }
        } catch (Throwable t){}
        return baseSize;
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
        return new Dimension(d.getWidth() +s.getHorizontalMargins(), d.getHeight() + s.getVerticalMargins());
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
     * Gets the preferred height including the vertical margins.
     * @return The preferred outer height.
     */
    public int getOuterPreferredH() {
        return getPreferredH() + getStyle().getVerticalMargins();
    }
    
    /**
     * Gets the preferred height removing vertical padding.
     * @return The preferred inner height.
     */
    public int getInnerPreferredH() {
        return getPreferredH() - getStyle().getVerticalPadding();
    }
    
    /**
     * Gets the preferred width including horizontal margins.
     * @return The preferred outer width.
     */
    public int getOuterPreferredW() {
        return getPreferredW() + getStyle().getHorizontalMargins();
    }
    
    /**
     * Gets the preferred width removing horizontal padding.
     * @return preferred width
     */
    public int getInnerPreferredW() {
        return getPreferredW() - getStyle().getHorizontalPadding();
    }

    /**
     * Sets the Component width, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to affect the component size, setPreferredSize should
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
     * If a user wishes to affect the component size, setPreferredSize should
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
     * If a user wishes to affect the component size, setPreferredSize should
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
        if(landscapeUiid != null) {
            if(Display.impl.isPortrait()) {
                return portraitUiid;
            }
            return landscapeUiid;
        }
        return portraitUiid;
    }

    /**
     * This method sets the Component the Unique identifier.
     * This method should be used before a component has been initialized
     * 
     * @param id UIID unique identifier for component type
     */
    public void setUIID(String id) {
        this.portraitUiid = id;
        unSelectedStyle = null;
        selectedStyle = null;
        disabledStyle = null;
        pressedStyle = null;
        allStyles = null;
        if(!sizeRequestedByUser) {
            preferredSize = null;
        }
    }
    
    boolean onOrientationChange() {
        if(landscapeUiid != null) {
            unSelectedStyle = null;
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;
            allStyles = null;
            if(!sizeRequestedByUser) {
                preferredSize = null;
            }
            return true;
        }
        return false;
    }
    
    /**
     * This method sets the Component the Unique identifier.
     * 
     * @param portraitUiid UIID unique identifier for component type in portrait mode
     * @param landscapeUiid UIID unique identifier for component type in landscape mode
     */
    public void setUIID(String portraitUiid, String landscapeUiid) {
        this.landscapeUiid = landscapeUiid;
        setUIID(portraitUiid);
    }
    
    /**
     * Gets inline styles that are to be applied to all states of this component.
     * @return Inline styles applied to all states.
     */
    public String getInlineAllStyles() {
        return inlineAllStyles;
    }
    
    /**
     * Gets inline styles that are to be applied to the selected state of this component.
     * @return Inline styles applied to selected state
     */
    public String getInlineSelectedStyles() {
        return this.inlineSelectedStyles;
    }
    
    /**
     * Gets inline styles that are to be applied to the unselected state of this component.
     * @return Inline styles applied to unselected state
     */
    public String getInlineUnselectedStyles() {
        return this.inlineUnselectedStyles;
    }
    
    /**
     * Gets inline styles that are to be applied to the disabled state of this component.
     * @return Inline styles applied to disabled state
     */
    public String getInlineDisabledStyles() {
        return this.inlineDisabledStyles;
    }
    
    /**
     * Gets inline styles that are to be applied to the pressed state of this component.
     * @return Inline styles applied to pressed state
     */
    public String getInlinePressedStyles() {
        return this.inlinePressedStyles;
        
    }
    
    /**
     * Registers inline styles that should be applied to all states of the component.  
     * @param styles a style in the format of {@code
     * "fgColor:ff0000; font:18mm; border: 1px solid ff0000; bgType:none; padding: 3mm; margin: 1mm" }
     */
    public void setInlineAllStyles(String styles) {
        if (styles != null && styles.trim().length() == 0) {
            styles = null;
        }
        if (styles == null ? inlineAllStyles != null : !styles.equals(inlineAllStyles)) {
            this.inlineAllStyles = styles;
            unSelectedStyle = null;
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;
            allStyles = null;
            if(!sizeRequestedByUser) {
                preferredSize = null;
            }
        }
    }
    
    /**
     * Registers inline styles that should be applied to the unselected state of the component.  
     * @param styles style format
     * @see #setInlineAllStyles(String)
     */
    public void setInlineUnselectedStyles(String styles) {
        if (styles != null && styles.trim().length() == 0) {
            styles = null;
        }
        if (styles == null ? inlineUnselectedStyles != null : !styles.equals(inlineUnselectedStyles)) {
            this.inlineUnselectedStyles = styles;

            unSelectedStyle = null;
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;
            allStyles = null;
            if(!sizeRequestedByUser) {
                preferredSize = null;
            }
        }
        
    }
    
    /**
     * Registers inline styles that should be applied to the selected state of the component.  
     * @param styles style format
     * @see #setInlineAllStyles(String)
     */
    public void setInlineSelectedStyles(String styles) {
        if (styles != null && styles.trim().length() == 0) {
            styles = null;
        }
        if (styles == null ? inlineSelectedStyles != null : !styles.equals(inlineSelectedStyles)) {
            this.inlineSelectedStyles = styles;

            unSelectedStyle = null;
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;
            allStyles = null;
            if(!sizeRequestedByUser) {
                preferredSize = null;
            }
        }
        
    }
    
    /**
     * Registers inline styles that should be applied to the disabled state of the component.  
     * @param styles style format
     * @see #setInlineAllStyles(String)
     */
    public void setInlineDisabledStyles(String styles) {
        if (styles != null && styles.trim().length() == 0) {
            styles = null;
        }
        if (styles == null ? inlineDisabledStyles != null : !styles.equals(inlineDisabledStyles)) {
            this.inlineDisabledStyles = styles;
            unSelectedStyle = null;
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;
            allStyles = null;
            if(!sizeRequestedByUser) {
                preferredSize = null;
            }
        }
    }
    
    /**
     * Registers inline styles that should be applied to the pressed state of the component.  
     * @param styles style format
     * @see #setInlineAllStyles(String)
     */
    public void setInlinePressedStyles(String styles) {
        if (styles != null && styles.trim().length() == 0) {
            styles = null;
        }
        if (styles == null ? inlinePressedStyles != null : !styles.equals(inlinePressedStyles)) {
            this.inlinePressedStyles = styles;
            unSelectedStyle = null;
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;
            allStyles = null;
            if(!sizeRequestedByUser) {
                preferredSize = null;
            }
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
        if (parent == this) {
            throw new IllegalArgumentException("Attempt to add self as parent");
        }
        this.parent = parent;
    }
    
    /**
     * Sets the owner of this component to the specified component.  This can be useful
     * for denoting a hierarchical relationship that is outside the actual parent-child
     * component hierarchy.  E.g. If there is a popup dialog that allows the user to select
     * input for a text field, then you could set the text field as the owner of the popup
     * dialog to denote a virtual parent-child relationship.
     * 
     * <p>This is used by {@link InteractionDialog#setDisposeWhenPointerOutOfBounds(boolean) } to figure out whether a
     * pointer event actually occurred outside the bounds of the dialog.  The {@link #containsOrOwns(int, int) } method
     * is used instead of {@link #contains(int, int) } so that it can cover the case where the pointer event occurred
     * on a component that is logically a child of the dialog, but not physically.</p>
     * popup dialog is opened, then 
     * @param owner The component to set as the owner of this component.
     * @since 6.0
     * @see #isOwnedBy(com.codename1.ui.Component) 
     * @see #containsOrOwns(int, int) 
     */
    public void setOwner(Component owner) {
        this.owner = owner;
    }
    
    /**
     * Gets the "owner" of this component as set by {@link #setOwner(com.codename1.ui.Component) }.
     * @return The owner component or null.
     * @since 7.0
     */
    public Component getOwner() {
        return owner;
    }
    
    /**
     * Checks to see if this component is owned by the given other component.  A component {@literal A} is
     * deemed to be owned by another component {@literal B} if any of the following conditions are true:
     * <ul>
     * <li>{@literal B} is the owner of {@literal A}</li>
     * <li>{@literal B} contains {@literal A}'s owner.</li>
     * <li>{@literal A}'s owner is owned by {@literal B}</li>
     * </ul>
     * @param cmp the owner
     * @return True if this component is owned by {@literal cmp}.
     * @since 6.0
     * @see #setOwner(com.codename1.ui.Component) 
     * @see #containsOrOwns(int, int) 
     */
    public boolean isOwnedBy(Component cmp) {
        Component c = this.owner;
        Container cnt = (cmp instanceof Container) ? (Container)cmp : null;
        while (c != null) {
            if (c == cmp) {
                return true;
            }
            if (cnt != null) {
                if (cnt.contains(c)) {
                    return true;
                }
            }
            c = c.owner;
        }
        c = this.getParent();
        while (c != null) {
            if (c.isOwnedBy(cmp)) {
                return true;
            }
            c = c.getParent();
        }
        
        return false;
    }
    
    /**
     * Checks to see if this component either contains the given point, or
     * if it owns the component that contains the given point.
     * @param x X-coordinate in absolute coordinates.
     * @param y Y-coordinate in absolute coordinates.
     * @return True if the coordinate is either inside the bounds of this component
     * or a component owned by this component.
     * @since 6.0
     * @see #setOwner(com.codename1.ui.Component) 
     * @see #isOwnedBy(com.codename1.ui.Component) 
     */
    public boolean containsOrOwns(int x, int y) {
        if (contains(x, y)) {
            return true;
        }
        Form f = getComponentForm();
        if (f != null) {
            Component cmp = f.getComponentAt(x, y);
            return cmp != null && cmp.isOwnedBy(this);
        }
        return false;
        
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
     * Convenience method used by {@link #drawShadow(Graphics, Image, int, int, int, int, int, int, int, float)} to convert device independent
     * pixels (1/96th of an inch) into pixels.
     * @param dp Value in device independent pixels (1/96th of an inch).
     * @return Value converted to pixels.
     *
     */
    private int dp2px(int dp) {
        return CN.convertToPixels(dp / 96f * 25.4f);
    }


    /**
     * Initial implementation used separate shadow rendering on each platform's native layer via the
     * platform's drawShadow() method.  However, performance in the simulator was terrible, so I implemented
     * a cross-platform fallback solution in {@link #drawShadow(Graphics, Image, int, int, int, int, int, int, int, float)} that
     * was reasonably fast.  After some experimentation it seems that using this cross-platform solution is good enough
     * to use on all platforms, however, it is an approximation and doesn't include any blur.
     *
     * This method acts as a switch to allow us to enable native shadow rendering if it is supported, and it has
     * been explicitly enabled either with a display property or a component client property.
     * @return True if native shadow rendering should be used for elevation.
     */
    private boolean useNativeShadowRendering() {
        if (!Display.impl.isDrawShadowSupported()) return false;
        if (Boolean.TRUE.equals(getClientProperty("Component.nativeShadowRendering"))) return true;
        if ("true".equals(CN.getProperty("Component.nativeShadowRendering", "false"))) return true;
        return false;
    }

    /**
     * Wrapper for {@link Graphics#drawShadow(Image, int, int, int, int, int, int, int, float)} that takes coordinates in device-independent
     * pixels (1/96th of an inch).  These are converted to pixels and passed to {@link Graphics#drawShadow(Image, int, int, int, int, int, int, int, float)}
     * @param g
     * @param img
     * @param relativeX
     * @param relativeY
     * @param offsetX
     * @param offsetY
     * @param blurRadius
     * @param spreadRadius
     * @param color
     * @param opacity
     */
    private void drawShadow(Graphics g, Image img, int relativeX, int relativeY, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity) {


        if (!useNativeShadowRendering()) {
            // Cross-platform "fast" shadow implementation.
            // No blur.
            int[] rgb = img.getRGBCached();
            int[] mask = new int[rgb.length];
            System.arraycopy(rgb, 0, mask, 0, rgb.length);
            int len = mask.length;
            color = (color & 0x00ffffff);
            int blurRadiusPixels = dp2px(blurRadius);
            int spreadRadiusPixels = dp2px(spreadRadius);
            int offsetXPixels = dp2px(offsetX);
            int offsetYPixels = dp2px(offsetY);
            int imageWidth = img.getWidth();
            int imageHeight = img.getHeight();
            for (int i=0; i<len; i++) {
                int pixel = mask[i];
                int alphaMask = (pixel & 0xff000000);
                int alpha = alphaMask >> 24;

                if (alpha != 0) {
                    //int adjustedAlpha = (int)(alpha * (float)opacity);
                    mask[i] = (alphaMask | color);
                }
            }

            int origAlpha = g.getAlpha();

            Image maskImage = Image.createImage(mask, img.getWidth(), img.getHeight());

            float step = 1;
            for (int rad = blurRadiusPixels; rad > 0; rad--) {

                g.setAlpha((int)(255/(float)step * opacity * (1-rad/(1+(float)blurRadiusPixels))));
                //System.out.println("rad="+rad+";alpha="+g.getAlpha());
                g.drawImage(maskImage,
                        relativeX + offsetXPixels - rad - spreadRadiusPixels,
                        relativeY + offsetYPixels - rad - spreadRadiusPixels,
                        img.getWidth() + 2*(spreadRadiusPixels+rad),
                        img.getHeight() + 2*(spreadRadiusPixels+rad));
                step += 0.5;
            }
            g.setAlpha((int)(opacity * 255/(float)step));

            //System.out.println("drawing;alpha="+g.getAlpha());



            g.drawImage(maskImage, relativeX + offsetXPixels - spreadRadiusPixels, relativeY + offsetYPixels - spreadRadiusPixels, img.getWidth() + 2*spreadRadiusPixels, img.getHeight() + 2*spreadRadiusPixels);
            //g.drawImage(maskImage, relativeX + offsetXPixels, relativeY + offsetYPixels);

            g.setAlpha(origAlpha);


        } else {
            // Use native shadow support.
            g.drawShadow(img, relativeX, relativeY, dp2px(offsetX), dp2px(offsetY), dp2px(blurRadius), dp2px(spreadRadius), color, opacity);
        }
    }


    /**
     * A cached image that is used for rendering drop-shadows.  This is only updated when the component elevation, width, or height
     * is changed.  Otherwise it is reused for painting shadows.
     *
     * @see #paintShadows(Graphics, int, int)
     */
    private Image cachedShadowImage;

    /**
     * The elevation of the component when the {@link #cachedShadowImage} was created.
     */
    private int cachedShadowElevation;

    /**
     * The width of the component when the {@link #cachedShadowImage} was created.
     */
    private int cachedShadowWidth,

    /**
     * The height of the component when the  {@link #cachedShadowImage} was created.
     */
    cachedShadowHeight;

    
    /**
     * Flag to indicate whether the component has elevation.
     */
    private boolean _hasElevation;

    /**
     * Checks to see if the component has elevation.  A component is considered to have elevation if either the current style
     * has a non-zero elevation value, or the component has *ever* had elevation in the past.  Once this is switched "on", it
     * doesn't switch off.
     *
     * <p>This is used by Container to efficiently paint shadows of its children.  It helps it to know if the child component
     * has ever had elevation as it may need to "erase" the previous shadow.</p>
     * @return
     */
    boolean hasElevation() {
        if (_hasElevation) return true;
        Style s = getStyle();
        if (s.getElevation() > 0) {
            _hasElevation = true;
        }
        return _hasElevation;
    }

    /**
     * Finds the nearest ancestor surface of this component.  This is the surface onto which drop-shadows will be
     * painted and projected.
     * @return The surface if one is found.  Null if this component has no elevation, or no surface is found.  It is possible that
     * this will return a non-null value even if the component currently has zero elevation.  This occurs if the component has *ever* been
     * styled to have elevation.
     */
    Container findSurface() {
        return _findSurface();
    }

    /**
     * Calculates the shadow's X-offset at the given elevation.
     * @param elevation THe elevation.
     * @return
     */
    int calculateShadowOffsetX(int elevation) {

        if (elevation <= 0) {
            return 0;
        }
        switch (elevation) {
            case 1: return dp2px(-4);
            case 2: return dp2px(-4);
            case 3: return dp2px(-9);
            case 4: return dp2px(-10);
            case 6: return dp2px(-19);
            case 8: return dp2px(-19);
            case 9: return dp2px(-22);
            case 12: return dp2px(-31);
            case 16: return dp2px(-42);
            case 24: return dp2px(-65);

        }
        return 0;
    }

    /**
     * Caldulates the shadow X-offset in pixels at the componentl's current elevation.
     *
     * @return The x-offset in pixels.
     */
    int calculateShadowOffsetX() {
        return calculateShadowOffsetX(getStyle().getElevation());
    }

    /**
     * Calculates the shadow Y offset in pixels at the component's current elevation.
     * @return The y-offset in pixels.
     * @see Style#getElevation()
     */
    int calculateShadowOffsetY() {
        return calculateShadowOffsetY(getStyle().getElevation());
    }

    /**
     * Calculates the shadow Y offset in pixels at the given elevation.
     * @param elevation The elevation.
     * @return The y-offset.
     */
    int calculateShadowOffsetY(int elevation) {
        return calculateShadowOffsetX(elevation);
    }

    /**
     * Calculates the width of the shadow that this component would project against at its current elevation.
     * @return The width in pixels.
     * @see Style#getElevation()
     */
    int calculateShadowWidth() {
        return calculateShadowWidth(getStyle().getElevation());
    }

    /**
     * Calculates the width of the shadow that this component would project against a surface at the given
     * elevation.
     * @param elevation The elvation.
     * @return The width in pixels.
     */
    int calculateShadowWidth(int elevation) {
        return getWidth() - 2 * calculateShadowOffsetX(elevation);
    }

    /**
     * Calculates the height of the shadow that this component would project against at its current elevation.
     * @return The height in pixels.
     * @see Style#getElevation()
     */
    int calculateShadowHeight() {
        return calculateShadowHeight(getStyle().getElevation());
    }

    /**
     * Calculates the height of the shadow that this component would project against a surface at the given
     * elevation.
     * @param elevation The elvation.
     * @return The width in pixels.
     */
    int calculateShadowHeight(int elevation) {
        return getHeight() - 2 * calculateShadowOffsetY(elevation);
    }

    /**
     * A flag to prevent reentry into painting the shadow.
     */
    private boolean paintinShadowInBackground_ = false;


    /**
     * Paints the drop-shadow projections for this component based on its elevation value.
     *
     * <p>This is called by the ancestor "surface" container of the component, after it paints its background, but
     * before painting its children.  If the {@link Style#getElevation()} of the component is {@literal 0}, then no shadow
     * will be painted.  Similarly, if the component has no ancestor container which is a surface (i.e. {@link Container#isSurface()} is true,
     * the shadow will not be painted.</p>
     *
     * <p>NOTE: It is also possible that the shadow will not be visible if other opaque components are painted in front of
     * the ancestor surface container.  This is one of the limitations of this approach for simulating elevation.</p>
     *
     * <p>Note: Not all platforms support drawing shadows.  Use {@link CodenameOneImplementation#isDrawShadowSupported()} to check
     * for support at runtime.</p>
     *
     * @param g The graphics context onto which the shadow should be painted.
     * @param relativeX The relative X coordinate onto which the shadow should be drawn.
     * @param relativeY The relative Y coordinate onto which the shadow should be drawn.
     * @since 8.0
     * @see Container#paintElevatedPane(Graphics)
     * @see Container#isSurface()
     * @see Style#getElevation()
     */

    public void paintShadows(Graphics g, final int relativeX, final int relativeY) {
        final int elevation = getStyle().getElevation();
        if (elevation <= 0) {
            return;
        }
        if (getWidth() == 0 || getHeight() == 0) return;
        synchronized (this) {
            if (cachedShadowImage != null) {
                if (cachedShadowWidth != getWidth() || cachedShadowHeight != getHeight() || cachedShadowElevation != elevation) {

                    if (cachedShadowElevation == elevation && cachedShadowWidth / (float) getWidth() > 0.5f && cachedShadowWidth / (float) getWidth() < 2f && cachedShadowHeight / (float) getHeight() > 0.5f && cachedShadowHeight / (float) getHeight() < 2f) {
                        // If the size change isn't too drastic, we can salvage the existing shadow image for performance reasons.
                        cachedShadowImage = cachedShadowImage.scaled(calculateShadowWidth(), calculateShadowHeight());
                        cachedShadowWidth = getWidth();
                        cachedShadowHeight = getHeight();
                    } else {
                        cachedShadowImage = null;
                    }
                }
            }
            if (cachedShadowImage != null) {
                g.drawImage(cachedShadowImage, relativeX + calculateShadowOffsetX(), relativeY + calculateShadowOffsetY());
                return;
            }
        }

        final Image fimg = this.toImage();
        if (fimg == null) return;
        if (paintinShadowInBackground_) {
            // We are already painting the shadow in a background thread, so don't do it twice.
            // Just be patient.
            return;
        }
        paintinShadowInBackground_ = true;

        Runnable createImageTask = new Runnable() {
            public void run() {
                // We paint shadow in a background thread to avoid jank on the EDT.  It is possible that this
                // will cause problems on some platforms.  If that is the case, we can hedge and move it onto
                // the EDT in certain cases.


                CN.setProperty("platformHint.showEDTWarnings", "false"); // Yes we know it's an EDT violation... Don't show me the error in simulator.
                try {
                    Image paddedImage = Image.createImage(calculateShadowWidth(), calculateShadowHeight(), 0x0);
                    Graphics paddedImageG = paddedImage.getGraphics();
                    paddedImageG.drawImage(fimg, -calculateShadowOffsetX(), -calculateShadowOffsetY());
                    Image img = paddedImage;


                    final Image shadowImage = Image.createImage(calculateShadowWidth(), calculateShadowHeight(), 0x0);

                    Graphics g = shadowImage.getGraphics();

                    int relativeX = 0;
                    int relativeY = 0;


                    long startTime = System.currentTimeMillis();

                    switch (elevation) {
                        case 1:
                            //drawShadow(g, img, relativeX, relativeY, 0, 1, 1, 0, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 2, 1, -1, 0, 0.12f);
                            drawShadow(g, img, relativeX, relativeY, 0, 1, 3, 0, 0, 0.2f);
                            break;

                        case 2:
                            //drawShadow(g, img, relativeX, relativeY, 0, 1, 1, 0, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 2, 1, -1, 0, 0.12f);
                            drawShadow(g, img, relativeX, relativeY, 0, 1, 3, 0, 0, 0.2f);
                            break;
                        case 3:
                            //drawShadow(g, img, relativeX, relativeY, 0, 3, 4, 0, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 3, 3, -2, 0, 0.12f);
                            drawShadow(g, img, relativeX, relativeY, 0, 1, 8, 0, 0, 0.2f);
                            break;

                        case 4:

                            //drawShadow(g, img, relativeX, relativeY, 0, 4, 5, 0, 0, 0.14f);
                            //System.out.println("Shadow 1 took "+(System.currentTimeMillis()-startTime)+"ms");
                            //long shadow2Start = System.currentTimeMillis();
                            //drawShadow(g, img, relativeX, relativeY, 0, 1, 10, 0, 0, 0.12f);
                            //System.out.println("Shadow 2 took "+(System.currentTimeMillis()-shadow2Start)+"ms");
                            //shadow2Start = System.currentTimeMillis();
                            drawShadow(g, img, relativeX, relativeY, 0, 2, 4, -1, 0, 0.2f);
                            //System.out.println("Shadow 3 took "+(System.currentTimeMillis()-shadow2Start)+"ms");
                            break;

                        case 6:
                            //drawShadow(g, img, relativeX, relativeY, 0, 6, 10, 0, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 1, 18, 0, 0, 0.12f);
                            drawShadow(g, img, relativeX, relativeY, 0, 3, 5, -1, 0, 0.2f);
                            break;

                        case 8:
                            drawShadow(g, img, relativeX, relativeY, 0, 8, 10, 1, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 3, 4, 2, 0, 0.12f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 5, 5, -3, 0, 0.2f);
                            break;
                        case 9:
                            drawShadow(g, img, relativeX, relativeY, 0, 9, 12, 1, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 3, 16, 2, 0, 0.12f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 5, 6, -3, 0, 0.2f);
                            break;

                        case 12:
                            drawShadow(g, img, relativeX, relativeY, 0, 12, 17, 2, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 5, 22, 4, 0, 0.12f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 7, 8, -4, 0, 0.2f);
                            break;

                        case 16:
                            drawShadow(g, img, relativeX, relativeY, 0, 16, 24, 2, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 6, 30, 5, 0, 0.12f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 8, 10, -5, 0, 0.2f);
                            break;

                        case 24:
                            drawShadow(g, img, relativeX, relativeY, 0, 24, 38, 3, 0, 0.14f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 24, 38, 3, 0, 1f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 9, 46, 8, 0, 0.12f);
                            //drawShadow(g, img, relativeX, relativeY, 0, 11, 15, -7, 0, 0.2f);
                            break;

                    }
                    synchronized (this) {
                        cachedShadowImage = shadowImage;
                        cachedShadowHeight = getHeight();
                        cachedShadowWidth = getWidth();
                        cachedShadowElevation = elevation;
                        paintinShadowInBackground_ = false;
                    }

                    CN.callSerially(new Runnable() {
                        public void run() {
                            Container surface = findSurface();
                            if (surface != null) {
                                surface.repaint();
                            }
                        }
                    });
                } finally {
                    CN.setProperty("platformHint.showEDTWarnings", "true"); // Reinstate EDT violation warnings now that we're done.

                    paintinShadowInBackground_ = false; // release the lock so that painting can resume.
                }

            }
        };
        if (canCreateImageOffEdt()) {
            CN.scheduleBackgroundTask(createImageTask);
        } else {
            createImageTask.run();
        }

        //origG.drawImage(cachedShadowImage, origRelativeX + calculateShadowOffsetX(), origRelativeY + calculateShadowOffsetY());



    }

    private boolean canCreateImageOffEdt() {
        String platform = CN.getPlatformName();
        if ("ios".equals(platform) && !CN.isSimulator()) return false;
        return true;
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

    int getRelativeX(Container relativeTo) {
        int x = getX() - getScrollX();
        Container parent = getParent();
        if (parent != relativeTo && parent != null) {
            x += ((Component)parent).getRelativeX(relativeTo);
        }
        return x;
    }

    int getRelativeY(Container relativeTo) {
        int y = getY() - getScrollY();
        Container parent = getParent();
        if (parent != relativeTo && parent != null) {
            y += ((Component)parent).getRelativeY(relativeTo);
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

    /**
     * A flag used by {@link Container#paintElevatedPane(Graphics)} to turn off rendering of elevated components
     * when rendering the non-elevated pane.
     */
    boolean doNotPaint;

    final void paintInternal(Graphics g, boolean paintIntersects) {
        Display d = Display.getInstance();
        CodenameOneImplementation impl = d.getImplementation();
        if (!isVisible() || doNotPaint) {
            return;
        }

        if(paintLockImage != null) {
            if(paintLockImage instanceof Image) {
                Image i = (Image)paintLockImage;
                g.drawImage(i, getX(), getY());
            } else {
                Image i = (Image)d.extractHardRef(paintLockImage);
                if(i == null) {
                    i = ImageFactory.createImage(this, getWidth(), getHeight(), 0);
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
            if(refreshTask != null && !InfiniteProgress.isDefaultMaterialDesignMode() && 
                (draggedMotionY == null || getClientProperty("$pullToRelease") != null)){
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

    /**
     * Paints intersecting components that appear above this component.
     * 
     * @param g Graphics context
     * @deprecated For internal use only
     */
    public void paintIntersectingComponentsAbove(Graphics g) {
        Container parent = getParent();
        Component component = this;
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();

        g.translate(-tx, -ty);
        int x1 = getAbsoluteX() + getScrollX();
        int y1 = getAbsoluteY() + getScrollY();
        int w = getWidth();
        int h = getHeight();

        while (parent != null) {
            int ptx = parent.getAbsoluteX() + parent.getScrollX();
            int pty = parent.getAbsoluteY() + parent.getScrollY();
            g.translate(ptx, pty);
            parent.paintIntersecting(g, component, x1, y1, w, h, true, 0);


            if (parent.isSurface()) {
                // If this is a surface, then we need to render the elevated pane
                parent.paintElevatedPane(g, true, x1, y1, w, h, this.renderedElevation, this.renderedElevationComponentIndex, true);
            }
            g.translate(-ptx, -pty);
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
        if (!isVisible()) {
            return;
        }
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
        paintTensile(g);
    }

    /**
     * Returns the area of this component that is currently hidden by the virtual keyboard.
     * @return The height of the area under the virtual keyboard in pixels
     */
    private int getInvisibleAreaUnderVKB() {
        Form f = getComponentForm();
        if (f != null) {
            int invisibleAreaUnderVKB = Form.getInvisibleAreaUnderVKB(f);
            if (invisibleAreaUnderVKB == 0) {
                return 0;
            }
            int bottomGap = f.getHeight() - getAbsoluteY() - getScrollY() - getHeight();
            if (bottomGap < invisibleAreaUnderVKB) {
                return invisibleAreaUnderVKB - bottomGap;
            } else {
                return 0;
            }
        }
        return 0;
    }
    
    void paintTensile(Graphics g) {
        if(tensileHighlightIntensity > 0) {
            int i = getScrollDimension().getHeight() - getHeight() + getInvisibleAreaUnderVKB();
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
                i = ImageFactory.createImage(this, getWidth(), getHeight(), 0);
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
        ((Container) par).paintIntersecting(g, c, x, y, w, h, false, 0);
        g.translate(-transX, -transY);
    }

    private void paintRippleEffect(Graphics g) {
        if(isRippleEffect() && Form.rippleComponent == this && Form.rippleMotion != null) {
            paintRippleOverlay(g, Form.rippleX, Form.rippleY, Form.rippleMotion.getValue());
        } 
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
        if(isFlatten() || !opaque) {
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
                paintRippleEffect(g);
                return;
            }
        }
        if (getStyle().getBgPainter() != null) {
            getStyle().getBgPainter().paint(g, bounds);
        }
        paintBackground(g);
        paintRippleEffect(g);
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
     * Indicates the Y position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the y position
     * of the component.
     * 
     * @param scrollY the Y position of the scrolling
     */
    protected void setScrollY(int scrollY) {
        if(this.scrollY != scrollY) {
            CodenameOneImplementation ci = Display.impl;
            
            if(ci.isAsyncEditMode() && ci.isEditingText()) {
                Component editingText = ci.getEditingText();
                if (editingText != null && this instanceof Container && ((Container)this).contains(editingText)) {
                    ci.hideTextEditor();
                }
            }
        }
        // the setter must always update the value regardless... 
        int scrollYtmp = scrollY;
        if(!isSmoothScrolling() || !isTensileDragEnabled()) {
            int v = getInvisibleAreaUnderVKB();
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
            int h = getScrollDimension().getHeight() - getHeight() + getInvisibleAreaUnderVKB();
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
     * <p>NOTE: This will return true upon a "hit" even if the component is not
     * visible, or if that part of the component is currently clipped by a parent
     * component.  To check if a point is contained in the visible component bounds
     * use {@link #visibleBoundsContains(int, int) }</p>
     * 
     * @param x the given absolute x coordinate
     * @param y the given absolute y coordinate
     * @return true if the given absolute coordinate is contained in the 
     * Component; otherwise false
     * 
     * @see #visibleBoundsContains(int, int) 
     */
    public boolean contains(int x, int y) {
        int absX = getAbsoluteX() + getScrollX();
        int absY = getAbsoluteY() + getScrollY();
        return (x >= absX && x < absX + getWidth() && y >= absY && y < absY + getHeight());
    }
    
    /**
     * Returns true if the given absolute coordinate is contained inside the visible bounds
     * of the component.  This differs from {@link #contains(int, int) } in that it will
     * return {@literal false} if the component or any of its ancestors are not visible,
     * or if (x, y) are contained inside the bounds of the component, but are clipped.
     * 
     * @param x the given absolute x coordinate
     * @param y the given absolute y coordinate
     * @return true if the given absolute coordinate is contained in the 
     * Component's visible bounds; otherwise false
     * @see #contains(int, int) 
     */
    public boolean visibleBoundsContains(int x, int y) {
        boolean contains = true;
        if (!isVisible() || !contains(x, y)) {
            contains = false;
        }
        if (contains) {
            Container parent = getParent();
            while (parent != null) {
                if (!parent.visibleBoundsContains(x, y)) {
                    contains = false;
                }
                if (!contains) {
                    break;
                }
                parent = parent.getParent();
            }
        }
        return contains;
    }

    /**
     * Calculates the preferred size based on component content. This method is
     * invoked lazily by getPreferred size.
     * 
     * @return the calculated preferred size based on component content
     */
    protected Dimension calcPreferredSize() {
        return new Dimension(0, 0);
    }
    
    /**
     * Checks if this component has a fixed preferred size either via an explicit call to
     * {@link #setPreferredH(int) } and {@link #setPreferredW(int) }, or via a preferred
     * size style string.
     * @return True if this component has a fixed preferred size.
     * @since 7.0
     */
    public boolean hasFixedPreferredSize() {
        return sizeRequestedByUser || preferredSizeStr != null;
    }

    private Dimension preferredSizeImpl() {
        if (!sizeRequestedByUser && (shouldCalcPreferredSize || preferredSize == null)) {
            shouldCalcPreferredSize = false;
            if(hideInPortrait && Display.INSTANCE.isPortrait()) {
                preferredSize = new Dimension(0, 0);
            } else {
                if(hideInLandscape && !Display.INSTANCE.isPortrait()) {
                    preferredSize = new Dimension(0, 0);
                } else {
                    preferredSize = calcPreferredSize();
                    if (preferredSizeStr != null) {
                        Component.parsePreferredSize(preferredSizeStr, preferredSize);
                    }
                }
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
     * @see #getBounds(com.codename1.ui.geom.Rectangle) 
     */
    protected Rectangle getBounds() {
        return bounds;
    }
    
    /**
     * Returns the bounds of this component in the provided Rectangle.
     * @param rect An "out" parameter to store the component bounds in.  Cannot be null.
     * @return The same Rectangle that was passed as a parameter.
     * @since 7.0
     * @see #getBounds()
     */
    public Rectangle getBounds(Rectangle rect) {
        rect.setBounds(getBounds());
        return rect;
    }

    /**
     * Returns the component bounds for scrolling which might differ from the getBounds for large components
     * e.g. list.
     *
     * @see #getX
     * @see #getY
     * @return the component bounds
     * @see #getVisibleBounds(com.codename1.ui.geom.Rectangle) 
     */
    protected Rectangle getVisibleBounds() {
        return bounds;
    }
    
    /**
     * Returns the component bounds for scrolling which might differ from the getBounds for large components 
     * into the provided rectangle.  
     * @param rect An "out" parameter to store the bounds in.  Cannot be null.
     * @return The same Rectangle that was passed as a parameter.
     * @since 7.0
     * @see #getVisibleBounds() 
     */
    public Rectangle getVisibleBounds(Rectangle rect) {
        rect.setBounds(getVisibleBounds());
        return rect;
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
     * Sets the tab index of the component.  This method is for internal use only.  To set the
     * preferred tab index, use {@link #setPreferredTabIndex(int) }
     * @param index The tab index.
     * @deprecated This method is called internally by the layout manager each time the traversal order of the form is queried.  Use {@link #setPreferredTabIndex(int) } instead.
     * @see #getPreferredTabIndex() 
     * @see #setPreferredTabIndex(int) 
     * @see #getTabIndex() 
     * @see Form#getTabIterator(com.codename1.ui.Component) 
     */
    public void setTabIndex(int index) {
        tabIndex = index;
    }
    
    /**
     * Gets the tab index of the component. This value is only useful immediately
     * after calling {@link Form#getTabIterator(com.codename1.ui.Component) } on the 
     * form or {@link Container#updateTabIndices(int) } in the parent component.
     * @return The tab index of the component.
     * @see #getPreferredTabIndex() 
     * @see #setTabIndex(int) 
     * @see #setPreferredTabIndex(int) 
     * @see Form#getTabIterator(com.codename1.ui.Component) 
     * @see Container#updateTabIndices(int) 
     * @deprecated This method is used internally when querying the traversal order of the form.  Use {@link #getPreferredTabIndex() } to get the preferred tab index.
     * 
     */
    public int getTabIndex() {
        return tabIndex;
    }
    
    /**
     * Sets the preferred tab index of the component.
     * @param index The preferred tab index
     * @see #getPreferredTabIndex() 
     * @see Form#getTabIterator(com.codename1.ui.Component) 
     * @see Container#updateTabIndices(int) 
     */
    public void setPreferredTabIndex(int index) {
        preferredTabIndex = index;
    }
    
    /**
     * Gets the preferred tab index of this component.  Tab indices are used to specify the traversal order
     * when tabbing from component to component in a form.  
     * 
     * <p>Tab index meanings work similar to the HTML {@literal tabIndex}
     * attribute. A tab Index of {@literal -1} (the default value) results in the field not being traversable
     * using the keyboard (or using the next/prev buttons in devices' virtual keyboards).  A tab index of {@literal 0}
     * results in the component's traversal order being dictated by the natural traversal order of the form.</p>
     * 
     * <p>Use {@link Form#getTabIterator(com.codename1.ui.Component) } to obtain the complete traversal order for
     * all components in the form.</p>
     * 
     * <p>Best practice is to only explicitly set preferred tabIndex values of {@literal 0} if you want the component
     * to be traversable, or {@literal -1} if you don't want the component to be traversable.  Explicitly setting 
     * a positive preferred tab index may result in unexpected results.</p>
     * 
     * <h3>How the Preferred Tab Index is Used</h3>
     * 
     * <p>When the user tries to "tab" to the next field (or presses the "Next" button on the virtual keyboard), this 
     * triggers a call to {@link Form#getTabIterator(com.codename1.ui.Component) }, crawls the component hierarchy and
     * returns a {@link java.util.ListIterator} of all of the traversable fields in the form in the order they should 
     * be traversed. This order is determined by the layout managers on the form.  The core layout managers define 
     * sensible traversal orders by default.  If you have a custom layout manager, you can override its traversal
     * order by implementing the {@link com.codename1.ui.layouts.Layout#overridesTabIndices(com.codename1.ui.Container) } and
     * {@link com.codename1.ui.layouts.Layout#getChildrenInTraversalOrder(com.codename1.ui.Container) } methods.</p>
     * @return the tabbing index
     */
    public int getPreferredTabIndex() {
        if (isEnabled() && isVisible() && isFocusable()) {
            return preferredTabIndex;
        }
        return -1;
    }
    
    /**
     * Sets whether this component is traversable using the keyboard using tab, next, previous keys.  This is 
     * just a wrapper around {@link #setPreferredTabIndex(int) } that sets the tab index to 0 if the component
     * should be traversable, and -1 if it shouldn't be.
     * 
     * <p>Note:  This method is marked final because this is just a convenience wrapper around {@link #setPreferredTabIndex(int) }</p>
     * 
     * @param traversable True to make the component traversable.
     */
    public final void setTraversable(boolean traversable) {
        if (traversable && getPreferredTabIndex() < 0) {
            setPreferredTabIndex(0);
        } else if (!traversable && getPreferredTabIndex() >= 0) {
            setPreferredTabIndex(-1);
        }
    }
    
    /**
     * Checks if this component should be traversable using the keyboard using tab, next, previous keys.
     * 
     * <p>Note: This method is marked final because it is just a convenience wrapper around {@link #getPreferredTabIndex() }</p>
     * @return true if traversable in tab indexing
     */
    public final boolean isTraversable() {
        return getPreferredTabIndex() >= 0;
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
                this.shouldCalcPreferredSize = true;
                getParent().setShouldLayout(true);
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

    private Container _findSurface() {
        Container parent = getParent();
        if (parent == null) return null;
        if (parent.isSurface()) {
            return parent;
        }
        return ((Component)parent)._findSurface();
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

    /**
     * Prevent a lead component hierarchy from this specific component, this allows a component within that
     * hierarchy to still act as a standalone component
     * @return the blockLead
     */
    public boolean isBlockLead() {
        return blockLead;
    }

    /**
     * Prevent a lead component hierarchy from this specific component, this allows a component within that
     * hierarchy to still act as a standalone component
     * @param blockLead the blockLead to set
     */
    public void setBlockLead(boolean blockLead) {
        this.blockLead = blockLead;
        if(blockLead) {
            hasLead = false;
        }
    }

    /**
     * @return the ignorePointerEvents
     */
    public boolean isIgnorePointerEvents() {
        return ignorePointerEvents;
    }

    /**
     * @param ignorePointerEvents the ignorePointerEvents to set
     */
    public void setIgnorePointerEvents(boolean ignorePointerEvents) {
        this.ignorePointerEvents = ignorePointerEvents;
    }

    /**
     * Indicates whether the component displays the material design ripple effect
     * @return the rippleEffect
     */
    public boolean isRippleEffect() {
        return rippleEffect;
    }

    /**
     * Indicates whether the component displays the material design ripple effect
     * @param rippleEffect the rippleEffect to set
     */
    public void setRippleEffect(boolean rippleEffect) {
        this.rippleEffect = rippleEffect;
    }

    /**
     * Gets the theme that is used by inline styles to reference images.
     * @return the inlineStylesTheme
     * @see #setInlineStylesTheme(com.codename1.ui.util.Resources) 
     * @see #getInlineAllStyles() 
     * @see #getInlineSelectedStyles() 
     * @see #getInlinePressedStyles()
     * @see #getInlineUnselectedStyles() 
     * @see #getInlineDisabledStyles() 
     */
    public Resources getInlineStylesTheme() {
        return inlineStylesTheme;
    }

    /**
     * Sets the theme that is used by inline styles to reference images.  Inline styles will be
     * disabled unless an inlineStylesTheme is registered with the component.
     * @param inlineStylesTheme the theme that inline styles use to reference images.
     * @see #getInlineStylesTheme() 
     * @see #setInlineAllStyles(java.lang.String) 
     * @see #setInlinePressedStyles(java.lang.String) 
     * @see #setInlineSelectedStyles(java.lang.String) 
     * @see #setInlineDisabledStyles(java.lang.String) 
     * @see #setInlineUnselectedStyles(java.lang.String) 
     */
    public void setInlineStylesTheme(Resources inlineStylesTheme) {
        this.inlineStylesTheme = inlineStylesTheme;
    }

    /**
     * A component can indicate whether it is interested in rendering it's selection explicitly, this defaults to 
     * true in non-touch UI's and false in touch UI's except for the case where a user clicks the screen. 
     * @return Defaults to false
     */
    protected boolean shouldRenderComponentSelection() {
        return false;
    }

    /**
     * Indicates that this component and all its children should be hidden when the device is switched to landscape mode
     * @return the hideInLandscape
     */
    public boolean isHideInLandscape() {
        return hideInLandscape;
    }

    /**
     * Indicates that this component and all its children should be hidden when the device is switched to landscape mode
     * @param hideInLandscape the hideInLandscape to set
     */
    public void setHideInLandscape(boolean hideInLandscape) {
        this.hideInLandscape = hideInLandscape;
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
        final Style destStyle = hasInlineUnselectedStyle() ?
                getUIManager().parseComponentStyle(getInlineStylesTheme(), destUIID, getInlineStylesUIID(destUIID), getInlineUnselectedStyleStrings())
                :getUIManager().getComponentStyle(destUIID);
        return createStyleAnimation(sourceStyle, destStyle, duration, destUIID);
        
    }
    
    ComponentAnimation createStyleAnimation(final Style sourceStyle, final Style destStyle, final int duration, final String destUIID) {

        Motion m = null;
        if(sourceStyle.getFgColor() != destStyle.getFgColor()) {
            m = Motion.createLinearColorMotion(sourceStyle.getFgColor(), destStyle.getFgColor(), duration);
        }
        final Motion fgColorMotion = m;
        m = null;
        
        if(sourceStyle.getOpacity() != destStyle.getOpacity()) {
            m = Motion.createLinearColorMotion(sourceStyle.getOpacity(), destStyle.getOpacity(), duration);
        }
        final Motion opacityMotion = m;
        m = null;
        
        if(sourceStyle.getFont().getHeight() != destStyle.getFont().getHeight() && sourceStyle.getFont().isTTFNativeFont()) {
            // allows for fractional font sizes
            m = Motion.createLinearMotion(Math.round(sourceStyle.getFont().getPixelSize() * 100), Math.round(destStyle.getFont().getPixelSize() * 100), duration);
        }

        final Motion fontMotion = m;
        m = null;

        if(sourceStyle.getPaddingTop() != destStyle.getPaddingTop()) {
            m = Motion.createLinearMotion(sourceStyle.getPaddingTop(), destStyle.getPaddingTop(), duration);
        }
        final Motion paddingTop = m;
        m = null;

        if(sourceStyle.getPaddingBottom() != destStyle.getPaddingBottom()) {
            m = Motion.createLinearMotion(sourceStyle.getPaddingBottom(), destStyle.getPaddingBottom(), duration);
        }
        final Motion paddingBottom = m;
        m = null;

        if(sourceStyle.getPaddingLeftNoRTL()!= destStyle.getPaddingLeftNoRTL()) {
            m = Motion.createLinearMotion(sourceStyle.getPaddingLeftNoRTL(), destStyle.getPaddingLeftNoRTL(), duration);
        }
        final Motion paddingLeft = m;
        m = null;

        if(sourceStyle.getPaddingRightNoRTL()!= destStyle.getPaddingRightNoRTL()) {
            m = Motion.createLinearMotion(sourceStyle.getPaddingRightNoRTL(), destStyle.getPaddingRightNoRTL(), duration);
        }
        final Motion paddingRight = m;
        m = null;

        if(sourceStyle.getMarginTop()!= destStyle.getMarginTop()) {
            m = Motion.createLinearMotion(sourceStyle.getMarginTop(), destStyle.getMarginTop(), duration);
        }
        final Motion marginTop = m;
        m = null;

        if(sourceStyle.getMarginBottom() != destStyle.getMarginBottom()) {
            m = Motion.createLinearMotion(sourceStyle.getMarginBottom(), destStyle.getMarginBottom(), duration);
        }
        final Motion marginBottom = m;
        m = null;

        if(sourceStyle.getMarginLeftNoRTL()!= destStyle.getMarginLeftNoRTL()) {
            m = Motion.createLinearMotion(sourceStyle.getMarginLeftNoRTL(), destStyle.getMarginLeftNoRTL(), duration);
        }
        final Motion marginLeft = m;
        m = null;

        if(sourceStyle.getMarginRightNoRTL()!= destStyle.getMarginRightNoRTL()) {
            m = Motion.createLinearMotion(sourceStyle.getMarginRightNoRTL(), destStyle.getMarginRightNoRTL(), duration);
        }
        final Motion marginRight = m;

        if(paddingLeft != null || paddingRight != null || paddingTop != null || paddingBottom != null) {
            // convert the padding to pixels for smooth animation
            int left = sourceStyle.getPaddingLeftNoRTL();
            int right = sourceStyle.getPaddingRightNoRTL();
            int top = sourceStyle.getPaddingTop();
            int bottom = sourceStyle.getPaddingBottom();
            sourceStyle.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
            sourceStyle.setPadding(top, bottom, left, right);
        }
        
        if(marginLeft != null || marginRight != null || marginTop != null || marginBottom != null) {
            // convert the margin to pixels for smooth animation
            int left = sourceStyle.getMarginLeftNoRTL();
            int right = sourceStyle.getMarginRightNoRTL();
            int top = sourceStyle.getMarginTop();
            int bottom = sourceStyle.getMarginBottom();
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
        
        final Motion bgMotion = Motion.createLinearMotion(0, 255, duration);
        
        return new ComponentAnimation() {
            private boolean finished;
            private boolean stepMode;
            private boolean started;
            
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
                    bgMotion.setCurrentMotionTime(step);
                    if(fgColorMotion != null) {
                        fgColorMotion.setCurrentMotionTime(step);
                    }
                    if(opacityMotion != null) {
                        opacityMotion.setCurrentMotionTime(step);
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
                if(!stepMode && !started) {
                    return true;
                }
                return stepMode ||
                        !(bgMotion.isFinished() &&
                        (opacityMotion == null || opacityMotion.isFinished()) &&
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
                
                if(!started && !stepMode) {
                    started = true;
                    bgMotion.start();
                    if (opacityMotion != null) {
                        opacityMotion.start();
                    }
                    if(fgColorMotion != null) {
                        fgColorMotion.start();
                    }
                    if(fontMotion != null) {
                        fontMotion.start();
                    }
                    if(paddingTop != null) {
                        paddingTop.start();
                    }
                    if(paddingBottom != null) {
                        paddingBottom.start();
                    }
                    if(paddingLeft != null) {
                        paddingLeft.start();
                    }
                    if(paddingRight != null) {
                        paddingRight.start();
                    }
                    if(marginTop != null) {
                        marginTop.start();
                    }
                    if(marginBottom != null) {
                        marginBottom.start();
                    }
                    if(marginLeft != null) {
                        marginLeft.start();
                    }
                    if(marginRight != null) {
                        marginRight.start();
                    }
                }
                                
                if(!isInProgress()) {
                    finished = true;
                    if (destUIID != null) {
                        setUIID(destUIID);
                    }
                } else {
                    boolean requiresRevalidate = false;
                    if (opacityMotion != null) {
                        sourceStyle.setOpacity(opacityMotion.getValue());
                    }
                    if (fgColorMotion != null) {
                        sourceStyle.setFgColor(fgColorMotion.getValue());
                    }
                    ap.alpha = bgMotion.getValue();
                    if (fontMotion != null) {
                        Font fnt = sourceStyle.getFont();
                        fnt = fnt.derive(((float) fontMotion.getValue()) / 100.0f, fnt.getStyle());
                        requiresRevalidate = true;
                        sourceStyle.setFont(fnt);
                    }
                    if (paddingTop != null) {
                        sourceStyle.setPadding(TOP, paddingTop.getValue());
                        requiresRevalidate = true;
                    }
                    if (paddingBottom != null) {
                        sourceStyle.setPadding(BOTTOM, paddingBottom.getValue());
                        requiresRevalidate = true;
                    }
                    if (paddingLeft != null) {
                        sourceStyle.setPadding(LEFT, paddingLeft.getValue());
                        requiresRevalidate = true;
                    }
                    if (paddingRight != null) {
                        sourceStyle.setPadding(RIGHT, paddingRight.getValue());
                        requiresRevalidate = true;
                    }
                    if (marginTop != null) {
                        sourceStyle.setMargin(TOP, marginTop.getValue());
                        requiresRevalidate = true;
                    }
                    if (marginBottom != null) {
                        sourceStyle.setMargin(BOTTOM, marginBottom.getValue());
                        requiresRevalidate = true;
                    }
                    if (marginLeft != null) {
                        sourceStyle.setMargin(LEFT, marginLeft.getValue());
                        requiresRevalidate = true;
                    }
                    if (marginRight != null) {
                        sourceStyle.setMargin(RIGHT, marginRight.getValue());
                        requiresRevalidate = true;
                    }
                    if (!Component.revalidateOnStyleChange) {
                        // If revalidation on stylechange is not enabled, then the style animation
                        // won't work. We need to explicitly revalidate or repaint here.
                        if (requiresRevalidate) {
                            Container parent = getParent();
                            if (parent != null) parent.revalidate();
                            else repaint();
                        } else {
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void flush() {
                bgMotion.finish();
                if (opacityMotion != null) {
                    opacityMotion.finish();
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
     * @param disableSmoothScrolling false to disable
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
    }

    void clearDrag() {
        Component leadParent = LeadUtil.leadParentImpl(this);
        if (leadParent != null && leadParent != this) {
            leadParent.clearDrag();
            return;
        }
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
                    setScrollY(Math.max(0, hh));
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

    private boolean inPinch;
    
    /**
     * To be implemented by subclasses interested in being notified when a pinch zoom has
     * ended (i.e the user has removed one of their fingers, but is still dragging).
     * @param x The x-coordinate of the remaining finger in the drag.  (Absolute)
     * @param y The y-coordinate of the remaining finger in the drag. (Absolute)
     * @since 7.0
     */
    protected void pinchReleased(int x, int y) {
        
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
                inPinch = true;
                return;
            }
        } else {
            if (inPinch) {
                // if we were in a pinch zoom, but the user
                // removes a finger, then we need a way to signal to the component
                // that the pinch portion is over
                inPinch = false;
                pinchReleased(x[0], y[0]);
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
        Image draggedImage = ImageFactory.createImage(this, getWidth(), getHeight(),0x00ff7777);
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
     * Returns the component as an image.
     * @return This component as an image.
     */
    public Image toImage() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        Image image = ImageFactory.createImage(this, getWidth(), getHeight(),0x0);
        Graphics g = image.getGraphics();

        g.translate(-getX(), -getY());
        paintComponentBackground(g);
        paint(g);
        if (isBorderPainted()) {
            paintBorder(g);
        }
        g.translate(getX(), getY());
        return image;
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
     * Checks if the component responds to pointer events.  A component is considered
     * to respond to pointer events if it is visible and enabled, and is either scrollable,
     * focusable, or has the {@link #isGrabsPointerEvents() } flag {@literal true}.
     * @return True if the pointer responds to pointer events.
     */
    public boolean respondsToPointerEvents() {
        boolean isScrollable = CN.isEdt() ? isScrollable() : (scrollableXFlag() || scrollableYFlag());
        return isVisible() && isEnabled() && (isScrollable || isFocusable() || isGrabsPointerEvents() || isDraggable());
    }
    
    private boolean pointerReleaseMaterialPullToRefresh() {
        if(refreshTask != null && InfiniteProgress.isDefaultMaterialDesignMode()) {
            Container c = getComponentForm().getLayeredPane(InfiniteProgress.class, true);
            if(c.getComponentCount() > 0) {
                Component cc = c.getComponentAt(0);
                if(cc instanceof InfiniteProgress) {
                    return false;
                }
                Motion opacityMotion = (Motion)cc.getClientProperty("cn1$opacityMotion");
                c.removeAll();
                if(opacityMotion.isFinished()) {
                    final InfiniteProgress ip = new InfiniteProgress();
                    ip.setUIID("RefreshLabel");
                    ip.getUnselectedStyle().
                        setBorder(RoundBorder.create().
                            color(getUnselectedStyle().getBgColor()).
                            shadowX(0).
                            shadowY(0).
                            shadowSpread(1, true).
                            shadowOpacity(100));
                    Style s = ip.getUnselectedStyle();
                    s.setMarginUnit(Style.UNIT_TYPE_DIPS);
                    s.setMarginTop(10);
                    c.add(ip);
                    Display.INSTANCE.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            refreshTask.run();
                            ip.remove();
                        }
                    });
                } 
                c.revalidate();
                return true;
            }
        }
        return false;
    }
    
    private boolean updateMaterialPullToRefresh(final Form p, int y) {
        if(refreshTask != null && InfiniteProgress.isDefaultMaterialDesignMode() &&
            pullY < getHeight() / 4 &&
            scrollableYFlag() && getScrollY() == 0) {
            int mm = Display.INSTANCE.convertToPixels(1);
            if(mm < y - pullY) {
                p.clearComponentsAwaitingRelease();
                Container c = p.getLayeredPane(InfiniteProgress.class, true);
                c.setLayout(new FlowLayout(CENTER));
                Motion rotationMotion;
                Motion opacityMotion;
                Label refreshLabel;
                if(c.getComponentCount() == 0) {
                    refreshLabel = new Label("", "RefreshLabel");
                    FontImage.setMaterialIcon(refreshLabel, FontImage.MATERIAL_REFRESH, 5);
                    refreshLabel.
                        getUnselectedStyle().setBorder(RoundBorder.create().
                            color(getUnselectedStyle().getBgColor()).
                            shadowX(0).
                            shadowY(0).
                            shadowSpread(1, true).
                            shadowOpacity(100));
                    opacityMotion = Motion.createLinearMotion(
                        40, 255, getHeight() / 4);
                    opacityMotion.setStartTime(pullY);

                    rotationMotion = Motion.createLinearMotion(
                        0, 360, getHeight() / 4);
                    rotationMotion.setStartTime(pullY);
                    refreshLabel.putClientProperty("cn1$opacityMotion", opacityMotion);
                    refreshLabel.putClientProperty("cn1$rotationMotion", rotationMotion);
                    c.add(refreshLabel);
                    p.addPointerReleasedListener(new ActionListener<ActionEvent>() {
                        public void actionPerformed(ActionEvent evt) {
                            pointerReleaseMaterialPullToRefresh();
                            p.removePointerReleasedListener(this);
                            evt.consume();
                        }
                    });
                } else {
                    Component cc = c.getComponentAt(0);
                    if(cc instanceof InfiniteProgress) {
                        return false;
                    }
                    refreshLabel = (Label)cc;
                    opacityMotion = (Motion)refreshLabel.getClientProperty("cn1$opacityMotion");
                    rotationMotion = (Motion)refreshLabel.getClientProperty("cn1$rotationMotion");                    
                }
                rotationMotion.setCurrentMotionTime(y);
                opacityMotion.setCurrentMotionTime(y);
                Style s = refreshLabel.getAllStyles();
                s.setOpacity(opacityMotion.getValue());
                Image i = refreshLabel.getIcon().rotate(rotationMotion.getValue());
                refreshLabel.setIcon(i);
                s.setMarginUnit(Style.UNIT_TYPE_PIXELS);
                s.setMarginTop(Math.min(getHeight() / 5, y - pullY));
                c.revalidate();
                return true;
            }
        }
        return false;
    }
    
    /**
     * If this Component is focused, the pointer dragged event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerDragged(final int x, final int y) {
        Form f = getComponentForm();
        if (f != null) {
            pointerDragged(x, y, f.getCurrentPointerPress());
        } else {
            pointerDragged(x, y, null);
        }
    }
    
    private void pointerDragged(final int x, final int y, final Object currentPointerPress) {
        Component leadParent = LeadUtil.leadParentImpl(this);
        leadParent.pointerDragged(this, x, y, currentPointerPress);

    }
    /**
     * If this Component is focused, the pointer dragged event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     * @param currentPointerPress Object useed to track the current pointer press.  Each time 
     * the pointer is pressed, a new Object is generated, and is passed to pointerDragged.
     * This is to help prevent infinite loops of pointerDragged after a pointer press has been released.
     */
    private void pointerDragged(final Component lead, final int x, final int y, final Object currentPointerPress) {
        Form p = getComponentForm();
        if(p == null){
            return;
        }
        if (currentPointerPress != p.getCurrentPointerPress()) {
            return;
        }
        
        if (lead.pointerDraggedListeners != null && lead.pointerDraggedListeners.hasListeners()) {
            lead.pointerDraggedListeners.fireActionEvent(new ActionEvent(lead, ActionEvent.Type.PointerDrag, x, y));
        }

        if(dragAndDropInitialized) {
            //keep call to pointerDragged to move the parent scroll if needed
            if (dragCallbacks < 2) {
                dragCallbacks++;
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (dragActivated) {
                            lead.pointerDragged(x, y, currentPointerPress);
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
            if(dropTo != null && lead.dragOverListener != null) {
                ActionEvent ev = new ActionEvent(lead, dropTo, x, y);
                lead.dragOverListener.fireActionEvent(ev);
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
                int w = getWidth() - s.getHorizontalPadding();
                int h = getHeight() - s.getVerticalPadding();

                Rectangle view;
                int invisibleAreaUnderVKB = getInvisibleAreaUnderVKB();
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
                        int height;
                        int width;
                        if (isHidden() && dragImage != null) {
                            height = dragImage.getHeight() + 80;
                            width = dragImage.getWidth() + 80;
                        } else {
                            height = getHeight() + 80;
                            width = getWidth() + 80;
                        }
                        if(scrollParent.getScrollY() + draggedy + height >= scrollParent.getScrollDimension().getHeight()){
                            yposition = draggedy - scrollParent.getAbsoluteY();
                            height = scrollParent.getScrollDimension().getHeight() - yposition;
                        }                        
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
        if(dragActivated && p.getDraggedComponent() == null){
            dragActivated = false;
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
                if(getTensileLength() > -1 && (refreshTask == null || InfiniteProgress.isDefaultMaterialDesignMode())) {
                    tl = getTensileLength();
                } else {
                    tl = getHeight() / 2;
                }
                if(!isSmoothScrolling() || !isTensileDragEnabled()) {
                    tl = 0;
                }
                int scroll = getScrollY() + (lastScrollY - y);
                
                if(isAlwaysTensile() && getScrollDimension().getHeight() + getInvisibleAreaUnderVKB() <= getHeight()) {
                    if (scroll >= -tl && scroll < getHeight() + tl) {
                        setScrollY(scroll);
                    }
                } else {
                    if (scroll >= -tl && scroll < getScrollDimension().getHeight() + getInvisibleAreaUnderVKB() - getHeight() + tl) {
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
        Component leadParent = LeadUtil.leadParentImpl(this);
        leadParent.inPinch = false;
        leadParent.dragActivated = false;
        pointerPressed(x[0], y[0]);
        leadParent.scrollOpacity = 0xff;
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
        Component leadParent = LeadUtil.leadParentImpl(this);
        return leadParent.draggable;
    }
    
    /**
     * If this Component is focused, the pointer pressed event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerPressed(int x, int y) {
        Component leadParent = LeadUtil.leadParentImpl(this);
        leadParent.dragActivated = false;
        if (pointerPressedListeners != null && pointerPressedListeners.hasListeners()) {
            pointerPressedListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed, x, y));
        }
        leadParent.clearDrag();
        if(leadParent.isDragAndDropOperation(x, y)) {
            int restore = Display.getInstance().getDragStartPercentage();
            if(restore > 1){
                Component.restoreDragPercentage = restore;
            }
            Display.getInstance().setDragStartPercentage(1);
        }
    }

    void initDragAndDrop(int x, int y) {
        Component leadParent = LeadUtil.leadParentImpl(this);
        leadParent.dragAndDropInitialized = leadParent.isDragAndDropOperation(x, y);
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
        if (longPressListeners != null && longPressListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(this, ActionEvent.Type.LongPointerPress, x, y);
            longPressListeners.fireActionEvent(ev);
        }
    }

    /**
     * If this Component is focused, the pointer released event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerReleased(int x, int y) {
        Component leadParent = LeadUtil.leadParentImpl(this);
        if (leadParent.inPinch) {
            leadParent.inPinch = false;
        }
        if (pointerReleasedListeners != null && pointerReleasedListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(this, ActionEvent.Type.PointerReleased, x, y);
            pointerReleasedListeners.fireActionEvent(ev);
            if(ev.isConsumed()) {
                return;
            }
        }
        pointerReleaseImpl(x, y);
        leadParent.scrollOpacity = 0xff;
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
    
    /**
     * Returns text selection support object for this component.  Only used by 
     * components that support text selection (e.g. Labels, un-editable text fields, etc..).
     * @return text selection support object
     * @since 7.0
     */
    public TextSelectionSupport getTextSelectionSupport() {
        return null;
    }

    boolean isScrollDecelerationMotionInProgress() {
        Motion dmY = draggedMotionY;
        if (dmY != null) {
            if (dmY == decelerationMotion &&  !dmY.isFinished()) {
                return true;
            }
        }
        Motion dmX = draggedMotionX;
        if (dmX != null) {
            if (dmX == decelerationMotion && !dmX.isFinished()) {
                return true;
            }
        }
        Container parent = getParent();
        if (parent != null) {
            return parent.isScrollDecelerationMotionInProgress();
        }
        
        return false;
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
        decelerationMotion = draggedMotion;
        
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
        LeadUtil.leadParentImpl(this).dragFinishedImpl(this, x, y);
    }
    
    private void dragFinishedImpl(Component lead, int x, int y) {
        if(dragAndDropInitialized && dragActivated) {
            Form p = getComponentForm();
            if (p == null) {
                //The component was removed from the form during the drag
                dragActivated = false;
                dragAndDropInitialized = false;
                setVisible(true);
                dragImage = null;
                dropTargetComponent = null;
                return;
            }
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
                getParent().scrollRectToVisible(getX(), getY(), getWidth(), getHeight(), getParent());
                if(lead.dropListener != null) {
                    ActionEvent ev = new ActionEvent(lead, ActionEvent.Type.PointerDrag, dropTargetComponent, x, y);
                    lead.dropListener.fireActionEvent(ev);
                    if(!ev.isConsumed()) {
                        dropTargetComponent.drop(this, x, y);
                    }
                } else {
                    dropTargetComponent.drop(this, x, y);
                }
            } else {
                if(lead.dragOverListener != null) {
                    ActionEvent ev = new ActionEvent(lead, ActionEvent.Type.PointerDrag,null, x, y);
                    lead.dragOverListener.fireActionEvent(ev);
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
        if (lead.dragFinishedListeners != null && lead.dragFinishedListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(lead, ActionEvent.Type.DragFinished, x, y);
            lead.dragFinishedListeners.fireActionEvent(ev);
            if(ev.isConsumed()) {
                return;
            }
        }
        lead.dragFinished(x, y);
    }

    /**
     * Adds a listener to the dragFinished event
     *
     * @param l callback to receive drag finished events events
     */
    public void addDragFinishedListener(ActionListener l) {
        if (dragFinishedListeners == null) {
            dragFinishedListeners = new EventDispatcher();
        }
        dragFinishedListeners.addListener(l);
    }
    
    /**
     * Adds a listener to be notified when the state of this component is changed
     * to and from initialized.
     * @param l Listener to be subscribed.
     * @since 7.0
     */
    public void addStateChangeListener(ActionListener<ComponentStateChangeEvent> l) {
        if (stateChangeListeners == null) {
            stateChangeListeners = new EventDispatcher();
        }
        stateChangeListeners.addListener(l);
    }
    
    /**
     * Removes a listener from being notified when the state of this component is
     * changed to and from initialized.
     * @param l Listener to be unsubscribed.
     * @since 7.0
     */
    public void removeStateChangeListener(ActionListener<ComponentStateChangeEvent> l) {
        if (stateChangeListeners != null) {
            stateChangeListeners.removeListener(l);
        }
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
     * Adds a listener to the pointer event
     *
     * @param l callback to receive pointer events
     * @since 7.0
     */
    public void addLongPressListener(ActionListener l) {
        if (longPressListeners == null) {
            longPressListeners = new EventDispatcher();
        }
        longPressListeners.addListener(l);
    }

    /**
     * Invoked to draw the ripple effect overlay in Android where the finger of the user causes a growing 
     * circular overlay over time. This method is invoked after paintBackground and is invoked repeatedly until
     * the users finger is removed, it will only be invoked if isRippleEffect returns true
     * @param g the graphics object for the component clipped to the background
     * @param x the x position of the touch
     * @param y the y position of the touch
     * @param position a value between 0 and 1000 with 0 indicating the beginning of the ripple effect and 1000 
     * indicating the completion of it
     */
    public void paintRippleOverlay(Graphics g, int x, int y, int position) {
        int a = g.getAlpha();
        int c = g.getColor();
        g.concatenateAlpha(20);
        g.setColor(0);
        if(position == 1000) {
            g.fillRect(getX(), getY(), getWidth(), getHeight());
        } else {
            float ratio = ((float)position) / 1000.0f;
            int w = (int)(((float)getWidth()) * ratio);
            w = Math.max(w, Display.INSTANCE.convertToPixels(4));
            g.fillArc(x - getParent().getAbsoluteX() - w / 2, y - getParent().getAbsoluteY() - w / 2, w, w, 0, 360);
        }
        g.setAlpha(a);
        g.setColor(c);
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
     * Removes the listener from the pointer event
     *
     * @param l callback to remove
     * @since 7.0
     */
    public void removeLongPressListener(ActionListener l) {
        if (longPressListeners != null) {
            longPressListeners.removeListener(l);
        }
    }
    
    /**
     * Removes the listener from the drag finished event
     *
     * @param l callback to remove
     */
    public void removeDragFinishedListener(ActionListener l) {
        if (dragFinishedListeners != null) {
            dragFinishedListeners.removeListener(l);
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
        LeadUtil.leadParentImpl(this).pointerReleaseImpl(this, x, y);
    }
    
    private void pointerReleaseImpl(Component lead, int x, int y) {
        
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
                    if(refreshTask != null && !InfiniteProgress.isDefaultMaterialDesignMode()){
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
                    int scrh = getScrollDimension().getHeight() - getHeight() + getInvisibleAreaUnderVKB();
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
                                getHeight() + getInvisibleAreaUnderVKB() + tl/2,  speed, timeConstant);
                    } else {
                        draggedMotionY = Motion.createFrictionMotion(scroll, getScrollDimension().getHeight() - 
                                getHeight() + getInvisibleAreaUnderVKB() + tl/2, speed, 0.0007f);
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

        if(hasLead && !blockLead) {
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
            if (hasInlinePressedStyle()) {
                pressedStyle = getUIManager().parseComponentCustomStyle(getInlineStylesTheme(), getUIID(), getInlineStylesUIID(), "press", getInlinePressedStyleStrings());
            } else {
                pressedStyle = getUIManager().getComponentCustomStyle(getUIID(), "press");
            }
            initPressedStyle(pressedStyle);
            if (initialized && pressedStyle.getElevation()>0) {
                registerElevatedInternal(this);
            }
            if (initialized) {
                setSurface(pressedStyle.isSurface());
            }
            pressedStyle.addStyleListener(this);
            if(pressedStyle.getBgPainter() == null){
                pressedStyle.setBgPainter(new BGPainter());
            }
        }
        return pressedStyle;
    }


    /**
     * Can be overridden by subclasses to perform initialization when the unselected style is set to a new value.
     * @param unselectedStyle The unselected style.
     * @since 8.0
     */
    protected void initUnselectedStyle(Style unselectedStyle) {

    }

    /**
     * Can be overridden by subclasses to perform initialization when the pressed style is set to a new value.
     * @param unselectedStyle The pressed style.
     * @since 8.0
     */
    protected void initPressedStyle(Style pressedStyle) {

    }

    /**
     * Can be overridden by subclasses to perform initialization when the disabled style is set to a new value.
     * @param unselectedStyle The disabled style.
     * @since 8.0
     */
    protected void initDisabledStyle(Style disabledStyle) {

    }

    /**
     * Can be overridden by subclasses to perform initialization when the selected style is set to a new value.
     * @param unselectedStyle The selected style.
     * @since 8.0
     */
    protected void initSelectedStyle(Style selectedStyle) {

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
        initPressedStyle(style);
        if (initialized && pressedStyle.getElevation()>0) {
            registerElevatedInternal(this);
        }
        if (initialized) {
            setSurface(pressedStyle.isSurface());
        }
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
            if (hasInlineSelectedStyle()) {
                selectedStyle = getUIManager().parseComponentSelectedStyle(getInlineStylesTheme(), getUIID(), getInlineStylesUIID(), getInlineSelectedStyleStrings());
            } else {
                selectedStyle = getUIManager().getComponentSelectedStyle(getUIID());
            }
            initSelectedStyle(selectedStyle);
            if (initialized && selectedStyle.getElevation()>0) {
                registerElevatedInternal(this);
            }
            if (initialized) {
                setSurface(selectedStyle.isSurface());
            }
            selectedStyle.addStyleListener(this);
            if (selectedStyle.getBgPainter() == null) {
                selectedStyle.setBgPainter(new BGPainter());
            }
            if(cellRenderer) {
                selectedStyle.markAsRendererStyle();
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
            if (hasInlineDisabledStyle()) {
                disabledStyle = getUIManager().parseComponentCustomStyle(getInlineStylesTheme(), getUIID(), getInlineStylesUIID(), "dis", getInlineDisabledStyleStrings());
            } else {
                disabledStyle = getUIManager().getComponentCustomStyle(getUIID(), "dis");
            }
            initDisabledStyle(disabledStyle);
            if (initialized && disabledStyle.getElevation()>0) {
                registerElevatedInternal(this);
            }
            if (initialized) {
                setSurface(disabledStyle.isSurface());
            }
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
        initUnselectedStyle(style);
        if (initialized && unSelectedStyle.getElevation()>0) {
            registerElevatedInternal(this);
        }
        if (initialized) {
            setSurface(unSelectedStyle.isSurface());
        }
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
        initSelectedStyle(style);
        if (initialized && selectedStyle.getElevation()>0) {
            registerElevatedInternal(this);
        }
        if (initialized) {
            setSurface(selectedStyle.isSurface());
        }
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
        initDisabledStyle(style);
        if (initialized && disabledStyle.getElevation()>0) {
            registerElevatedInternal(this);
        }
        if (initialized) {
            setSurface(disabledStyle.isSurface());
        }
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
     * Finds all children (and self) that have negative scroll positions.
     * 
     * <p>This is primarily to solve https://github.com/codenameone/CodenameOne/issues/2476</p>
     * @param out A set to add found components to.
     * @return The set of found components (reference to the same set that is passed as an arg).
     */
    java.util.Set<Component> findNegativeScrolls(java.util.Set<Component> out) {
        if (scrollableYFlag() && getScrollY() < 0) {
            out.add(this);
        }
        if (this instanceof Container) {
            for (Component child : (Container)this) {
                child.findNegativeScrolls(out);
            }
        }
        return out;
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
        return "x=" + getX() + " y=" + getY() + " width=" + getWidth() + " height=" + getHeight() + " name=" + getName();
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
            if (hasInlineUnselectedStyle()) {
                setUnselectedStyle(mergeStyle(unSelected, manager.parseComponentStyle(getInlineStylesTheme(), id, getInlineStylesUIID(id), getInlineUnselectedStyleStrings())));
            } else {
                setUnselectedStyle(mergeStyle(unSelected, manager.getComponentStyle(id)));            
            }
            if (selectedStyle != null) {
                if (hasInlineSelectedStyle()) {
                    setSelectedStyle(mergeStyle(selectedStyle, manager.parseComponentSelectedStyle(getInlineStylesTheme(), id, getInlineStylesUIID(id), getInlineSelectedStyleStrings())));
                } else {
                    setSelectedStyle(mergeStyle(selectedStyle, manager.getComponentSelectedStyle(id)));
                }
            }
            if (disabledStyle != null) {
                if (hasInlineDisabledStyle()) {
                    setDisabledStyle(mergeStyle(disabledStyle, manager.parseComponentCustomStyle(getInlineStylesTheme(), id, getInlineStylesUIID(id), "dis", getInlineDisabledStyleStrings())));
                } else {
                    setDisabledStyle(mergeStyle(disabledStyle, manager.getComponentCustomStyle(id, "dis")));
                }
            }
            if(pressedStyle != null) {
                if (hasInlinePressedStyle()) {
                    setPressedStyle(mergeStyle(pressedStyle, manager.parseComponentCustomStyle(getInlineStylesTheme(), id, getInlineStylesUIID(id), "press", getInlinePressedStyleStrings())));
                } else {
                    setPressedStyle(mergeStyle(pressedStyle, manager.getComponentCustomStyle(id, "press")));
                }
            }
        }else{
            unSelectedStyle = null;
            unSelectedStyle = getUnselectedStyle();
            selectedStyle = null;
            disabledStyle = null;
            pressedStyle = null;       
            allStyles = null;
            
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

    /**
     * A flag that tracks whether the component is current registered as an animated with {@link Form#registerAnimatedInternal(Animation)}.
     * Using this flag allows for a small efficiency improvement.  The flag is set in {@link Form#registerAnimatedInternal(Animation)} and
     * unset in {@link Form#deregisterAnimatedInternal()}.
     */
    boolean internalRegisteredAnimated;
    void deregisterAnimatedInternal() {
        if (!internalRegisteredAnimated) return;
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

    boolean isTensileMotionInProgress() {
        return draggedMotionY != null && !draggedMotionY.isFinished();
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
                    int iv = getInvisibleAreaUnderVKB();
                    int edge = (getScrollDimension().getHeight() - getHeight() + iv);
                    if (dragVal > edge && edge > 0) {
                        startTensile(dragVal, getScrollDimension().getHeight() - getHeight() + iv, true);
                    } else {
                        if (snapToGrid && getScrollY() < edge && getScrollY() > 0) {
                            boolean tVal = tensileDragEnabled;
                            tensileDragEnabled = true;
                            int dest = getGridPosY();
                            int scroll = getScrollY();
                            if (Math.abs(dest-scroll) == 1) {
                                // Fixes issue with exponential decay where it never actually reaches destination
                                // so it creates infinite loop
                                setScrollY(dest);
                                draggedMotionY = null;
                            }
                            else if (dest != scroll) {
                                startTensile(scroll, dest, true);
                            } else {
                                draggedMotionY = null;
                            }
                            tensileDragEnabled = tVal;
                        } else {
                            draggedMotionY = null;
                        }
                    }
                }
                
                // special callback to scroll Y to allow developers to override the setScrollY method effectively
                setScrollY(dragVal);
                updateTensileHighlightIntensity(dragVal, getScrollDimension().getHeight() - getHeight() + getInvisibleAreaUnderVKB(), false);            
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
                            boolean tVal = tensileDragEnabled;
                            tensileDragEnabled = true;
                            int dest = getGridPosX();
                            int scroll = getScrollX();
                            if (dest != scroll) {
                                startTensile(scroll, dest, false);
                            } else {
                                draggedMotionX = null;
                            }
                            tensileDragEnabled = tVal;
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
            int w = getWidth() - s.getHorizontalPadding();
            int h = getHeight() - s.getVerticalPadding();

            Rectangle view;
            int invisibleAreaUnderVKB = getInvisibleAreaUnderVKB();
            
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
                        s.getHorizontalPadding();
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
                        s.getVerticalPadding();
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
            int alpha = g.concatenateAlpha(getStyle().getFgAlpha());
            b.paint(g, this);
            g.setAlpha(alpha);
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
        if(cellRenderer) {
            getUnselectedStyle().markAsRendererStyle();
            getSelectedStyle().markAsRendererStyle();
            getDisabledStyle().markAsRendererStyle();
        }
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
     * Holds a reference to the current surface this this component is registered with.  
     * @see #registerElevatedInternal(Component) 
     * @see Container#addElevatedComponent(Component) 
     * @see Container#removeElevatedComponent(Component)
     * @since 8.0
     */
    private Container _parentSurface;

    /**
     * Registers the given component with the nearest surface.  This will attempt to register
     * it with the parent container of "this", if it is a surface.  If not, it will walk up
     * the component hierarchy until it finds a surface to add it to.
     *
     * @param cmp The component to register with the neares surface.
     * @see Container#addElevatedComponent(Component)
     * @see Container#removeElevatedComponent(Component)
     * @since 8.0
     */
    void registerElevatedInternal(Component cmp) {
        if (cmp._parentSurface != null) {
            // It was already registered with a surface
            cmp._parentSurface.removeElevatedComponent(cmp);
            cmp._parentSurface = null;
        }
        Container parent = getParent();
        if (parent == null) return;
        if (parent.isSurface()) {
            // Let's keep a reference to the surface so that we can remove it later.
            parent.addElevatedComponent(cmp);
            cmp._parentSurface = parent;
        } else {
            ((Component)parent).registerElevatedInternal(cmp);
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
            if (stl.getElevation() > 0) {
                // This component is elevated, so we need to register it with the surface so that it can
                // render its shadows.
                registerElevatedInternal(this);
            }
            setSurface(stl.isSurface());
            if (stateChangeListeners != null) {
                stateChangeListeners.fireActionEvent(new ComponentStateChangeEvent(this, true));
            }
            showNativeOverlay();
            if(refreshTask != null && InfiniteProgress.isDefaultMaterialDesignMode()) {
                final Form p = getComponentForm();
                if(refreshTaskDragListener == null) {
                    refreshTaskDragListener = new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            if(evt.getEventType() == ActionEvent.Type.PointerDrag) {
                                if(updateMaterialPullToRefresh(p, evt.getY() - getAbsoluteY())) {
                                    evt.consume();
                                }
                            } else {
                                pullY = evt.getY() - getAbsoluteY();
                            }
                        }
                    };
                }
                p.addPointerDraggedListener(refreshTaskDragListener);
                p.addPointerPressedListener(refreshTaskDragListener);
            }
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
            hideNativeOverlay();
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
            if (stateChangeListeners != null) {
                stateChangeListeners.fireActionEvent(new ComponentStateChangeEvent(this, false));
            }
            deregisterAnimatedInternal();
            if (_parentSurface != null) {
                _parentSurface.removeElevatedComponent(this);
                _parentSurface = null;
            }
            deinitialize();
            if(refreshTaskDragListener != null) {
                Form f = getComponentForm();
                f.removePointerDraggedListener(refreshTaskDragListener);
                f.removePointerPressedListener(refreshTaskDragListener);
            }
        }
    }

    /**
     * If the component {@link #isEditable() }, then this will start the editing
     * process.  For TextFields, this results in showing the keyboard and allowing
     * the user to edit the input.  For the Picker, this will display the popup.
     * 
     * @see #stopEditing(java.lang.Runnable) 
     * @see #isEditing() 
     * @see #isEditable() 
     * @see #getEditingDelegate() 
     * @see #setEditingDelegate(com.codename1.ui.Editable) 
     */
    public void startEditingAsync() {
        // Empty implementation overridden by subclass
        if (editingDelegate != null) {
            editingDelegate.startEditingAsync();
        }
    }
    
    /**
     * Stops the editing process.
     * @param onFinish Callback called when the editing is complete.
     * @see #startEditingAsync() 
     * @see #isEditing() 
     * @see #isEditable() 
     * @see #getEditingDelegate() 
     * @see #setEditingDelegate(com.codename1.ui.Editable) 
     */
    public void stopEditing(Runnable onFinish) {
        if (editingDelegate != null) {
            editingDelegate.stopEditing(onFinish);
        }
    }
    
    /**
     * Checks if the component is currently being edited.
     * 
     * @return True if the component is currently being edited.
     * @see #startEditingAsync() 
     * @see #stopEditing(java.lang.Runnable) 
     * @see #isEditable() 
     * @see #getEditingDelegate() 
     * @see #setEditingDelegate(com.codename1.ui.Editable) 
     */
    public boolean isEditing() {
        if (editingDelegate != null) {
            return editingDelegate.isEditing();
        }
        return false;
    }
    
    /**
     * Checks to see if the component is editable.   This is used for next/previous
     * focus traversal on forms.
     * @return 
     * @see #getEditingDelegate() 
     * @see #setEditingDelegate(com.codename1.ui.Editable) 
     * @see #isEditing() 
     * @see #startEditingAsync() 
     * @see #stopEditing(java.lang.Runnable) 
     */
    public boolean isEditable() {
        if (editingDelegate != null) {
            return editingDelegate.isEditable();
        }
        return false;
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
            int ivk = getInvisibleAreaUnderVKB();
            
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
            updateNativeOverlay();
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
     * Invoked to indicate a change in a propertyName of a Style
     * 
     * <p><em>NOTE</em> By default this will trigger a call to {@link Container#revalidate() } on the parent
     * container, which is expensive.  You can disable this behavior by calling {@code CN.setProperty("Component.revalidateOnStyleChange", "false")}.
     * The intention is to change this behavior so that the default is to "not" revalidate on style change, so we encourage you to 
     * set this to "false" to ensure for future compatibility.</p>
     * 
     * @param propertyName the property name that was changed
     * @param source The changed Style object
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
                if (revalidateOnStyleChange) {
                    parent.revalidateLater();
                }
            }
        } else if (propertyName.equals(Style.ELEVATION) && source.getElevation() > 0) {
            Container surface = findSurface();
            if (surface != null) {
                surface.addElevatedComponent(this);
            }
        } else if (propertyName.equals(Style.SURFACE)) {
            setSurface(source.isSurface());
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
        if (initialized && s.getElevation()>0) {
            registerElevatedInternal(this);
        }
        if (initialized) {
            setSurface(s.isSurface());
        }
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
            paintLockImage = ImageFactory.createImage(this, getWidth(), getHeight(), 0);
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
     * @deprecated this feature should work but it isn't maintained and isn't guaranteed to function properly. 
     *    There are issues covering this but at this time we can't dedicate resources to address them specifically:
     *    <a href="https://github.com/codenameone/CodenameOne/issues/2122">#2122</a>,
     *    <a href="https://github.com/codenameone/CodenameOne/issues/1966">#1966</a> &amp;
     *    <a href="https://github.com/codenameone/CodenameOne/issues/1947">#1947</a>. 
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
     * A component that might need side swipe such as the tabs
     * could block it from being used for some other purpose when
     * on top of said component.
     */
    protected boolean shouldBlockSideSwipeLeft() {
        return false;
    }

    /**
     * A component that might need side swipe such as the tabs
     * could block it from being used for some other purpose when
     * on top of said component.
     */
    protected boolean shouldBlockSideSwipeRight() {
        return false;
    }

    /**
     * A component that might need side swipe such as the slider
     * could block it from being used for some other purpose when
     * on top of said component.
     * 
     * This method is merely a public accessor for {@link #shouldBlockSideSwipe() }.
     * 
     * @since 7.0
     */
    public final boolean blocksSideSwipe() {
        return shouldBlockSideSwipe();
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
     * <p>The default UIID for the text hint is "TextHint"</p>
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
        return alwaysTensile && !isScrollableX() || (refreshTask != null && !InfiniteProgress.isDefaultMaterialDesignMode());
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
     * Searches the hierarchy of the component recursively to see if the given
     * Container is one of the parents of this component
     * @param cnt a potential parent of this component
     * @return false if the container isn't one of our parent containers
     */
    public boolean isChildOf(Container cnt) {
        if(cnt == parent) {
            return true;
        }
        return parent != null && parent.isChildOf(cnt);
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
     * @deprecated this mapped to an older iteration of properties that is no longer used
     */
    public String[] getBindablePropertyNames() {
        return null;
    }
    
    /**
     * Returns the types of the properties that are bindable within this component
     * @return the class for binding
     * @deprecated this mapped to an older iteration of properties that is no longer used
     */
    public Class[] getBindablePropertyTypes() {
        return null;
    }
    
    /**
     * Binds the given property name to the given bind target
     * @param prop the property name
     * @param target the target binder
     * @deprecated this mapped to an older iteration of properties that is no longer used
     */
    public void bindProperty(String prop, BindTarget target) {
    }
    
    /**
     * Removes a bind target from the given property name
     * @param prop the property names
     * @param target the target binder
     * @deprecated this mapped to an older iteration of properties that is no longer used
     */
    public void unbindProperty(String prop, BindTarget target) {
    }
    
    /**
     * Allows the binding code to extract the value of the property
     * @param prop the property
     * @return the value for the property
     * @deprecated this mapped to an older iteration of properties that is no longer used
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
     * @deprecated this mapped to an older iteration of properties that is no longer used
     */
    public void setBoundPropertyValue(String prop, Object value) {
    }
    
    /**
     * Indicates the property within this component that should be bound to the cloud object
     * @return the cloudBoundProperty
     * @deprecated this mapped to an older iteration of properties that is no longer used
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
     * @deprecated this mapped to an older iteration of properties that is no longer used
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
     * @deprecated this mapped to an older iteration of properties that is no longer used
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
     * @deprecated this mapped to an older iteration of properties that is no longer used
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
     * This method also optionally sets the margin to 0 so the component will be truly hidden. Notice that this might 
     * not behave as expected with scrollable containers or layouts that ignore preferred size.
     * 
     * @param b true to hide the component and false to show it
     * @param changeMargin indicates margin should be set to 0
     */
    public void setHidden(boolean b, boolean changeMargin) {
        if(b) {
            if(!sizeRequestedByUser) {
                if(changeMargin) {
                	getAllStyles().cacheMargins(false); //if a margins cache already exists because the component is already hidden it would be kept else it would be created
                    getAllStyles().setMargin(0, 0, 0, 0);
                }
                setPreferredSize(new Dimension());
            }
        } else {
            setPreferredSize(null);
            if(changeMargin) {
            	getAllStyles().restoreCachedMargins(); //restore margins to the values they had before the component being hidden and flush the margins cache
//                if(getUnselectedStyle().getMarginLeftNoRTL() == 0) {
//                    setUIID(getUIID());
//                }
            }
        }
    }
    
    /**
     * Makes the components preferred size equal 0 when hidden and restores it to the default size when not.
     * Also toggles the UIID to "Container" and back to allow padding/margin to be removed. Since the visible flag
     * just hides the component without "removing" the space it occupies this is the flag that can be used to truly
     * hide a component within the UI. Notice that this might 
     * not behave as expected with scrollable containers or layouts that ignore preferred size.
     * 
     * @param b true to hide the component and false to show it
     */
    public void setHidden(boolean b) {
        setHidden(b, true);
    }    
    
    /**
     * Returns true if the component was explicitly hidden by the user.  This method doesn't check 
     * if the parent component is hidden, so it is possible that the component would be hidden from
     * the UI, but that this would still return true.  Use {@link #isHidden(boolean) } with {@literal true}
     * to check also if the parent is hidden.
     * @return true if the component is hidden, notice that the hidden property and visible property have different meanings in the API!
     */
    public boolean isHidden() {
        return sizeRequestedByUser && preferredSize != null && preferredSize.getWidth() == 0 && preferredSize.getHeight() == 0;
    }
    
    /**
     * Checks if the component is hidden. If {@literal checkParent} is {@literal true}, this 
     * also checks to see if the parent is hidden, and will return true if either this component
     * is hidden, or the parent is hidden.
     * @param checkParent True to check if parent is hidden also.
     * @return Returns true if the component is hidden.
     * @since 7.0
     */
    public boolean isHidden(boolean checkParent) {
        if (isHidden()) {
            return true;
        }
        if (checkParent && parent != null) {
            return parent.isHidden(true);
        }
        return false;
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
                    int oldX = x;
                    int oldY = y;
                    x = rect.getX();
                    y = rect.getY();
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
                    x = oldX;
                    y = oldY;
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

    /**
     * @return the tooltip
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * @param tooltip the tooltip to set
     */
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}
