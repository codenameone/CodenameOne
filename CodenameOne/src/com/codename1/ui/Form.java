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

import com.codename1.io.Log;
import com.codename1.ui.ComponentSelector.Filter;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;

/**
 *<p> Top level component that serves as the root for the UI, this {@link Container}
 * subclass works in concert with the {@link Toolbar} to create menus. By default a 
 * forms main content area (the content pane) is scrollable on the Y axis and has a {@link com.codename1.ui.layouts.FlowLayout} as is the default.</p>
 *
 * <p>Form contains a title bar area which in newer application is replaced by the {@link Toolbar}.
 * Calling {@link #add(com.codename1.ui.Component)} or all similar methods  on the {@code Form} 
 * delegates to the contenPane so calling {@code form.add(cmp)} is equivalent to 
 * {@code form.getContentPane().add(cmp)}. Normally this shouldn't matter, however in some cases such as
 * animation we need to use the content pane directly e.g. {@code form.getContentPane().animateLayout(200)}
 * will work whereas {@code form.animateLayout(200)} will fail. </p>
 * 
 * @author Chen Fishbein
 */
public class Form extends Container {
    private boolean globalAnimationLock;
    static int activePeerCount;
    private Painter glassPane;
    private Container layeredPane;
    private Container formLayeredPane;
    private Container contentPane;
    Container titleArea = new Container(new BorderLayout());
    private Label title = new Label("", "Title");
    private MenuBar menuBar;
    private Component dragged;
    private boolean enableCursors;
    static Motion rippleMotion;
    static Component rippleComponent;
    static int rippleX;
    static int rippleY;
    
    ArrayList<Component> buttonsAwatingRelease;
    
    private VirtualInputDevice currentInputDevice;
    
    private AnimationManager animMananger = new AnimationManager(this);
    
    /**
     * Indicates whether lists and containers should scroll only via focus and thus "jump" when
     * moving to a larger component as was the case in older versions of Codename One.
     */
    protected boolean focusScrolling;
    /**
     * Used by the combo box to block some default Codename One behaviors
     */
    static boolean comboLock;
    /**
     * Contains a list of components that would like to animate their state
     */
    private ArrayList<Animation> internalAnimatableComponents;
    /**
     * Contains a list of components that would like to animate their state
     */
    private ArrayList<Animation> animatableComponents;
    //private FormSwitcher formSwitcher;
    private Component focused;
    private ArrayList<Component> mediaComponents;
    
    private boolean bottomPaddingMode;
    
    /**
     * This member allows us to define an animation that will draw the transition for
     * entering this form. A transition is an animation that would occur when 
     * switching from one form to another.
     */
    private Transition transitionInAnimator;
    /**
     * This member allows us to define a an animation that will draw the transition for
     * exiting this form. A transition is an animation that would occur when 
     * switching from one form to another.
     */
    private Transition transitionOutAnimator;
    /**
     * a listener that is invoked when a command is clicked allowing multiple commands
     * to be handled by a single block
     */
    private EventDispatcher commandListener;
    /**
     * Relevant for modal forms where the previous form should be rendered underneath
     */
    private Form previousForm;
    /**
     * Indicates that this form should be tinted when painted
     */
    boolean tint;
    /**
     * Default color for the screen tint when a dialog or a menu is shown
     */
    private int tintColor;
    /**
     * Listeners for key release events 
     */
    private HashMap<Integer, ArrayList<ActionListener>> keyListeners;
    /**
     * Listeners for game key release events 
     */
    private HashMap<Integer, ArrayList<ActionListener>> gameKeyListeners;
    /**
     * Indicates whether focus should cycle within the form
     */
    private boolean cyclicFocus = true;
    private int tactileTouchDuration;
    EventDispatcher showListener;
    int initialPressX;
    int initialPressY;
    private EventDispatcher orientationListener;
    private EventDispatcher sizeChangedListener;    
    private UIManager uiManager;
    private Component stickyDrag;
    private boolean dragStopFlag;
    private Toolbar toolbar;
    
    /**
     * A text component that will receive focus and start editing immediately as the form is shown
     */
    private TextArea editOnShow;
            
    /**
     * Default constructor creates a simple form
     */
    public Form() {
        this(new FlowLayout());
    }
    
    /**
     * Constructor that accepts a layout
     * 
     * @param contentPaneLayout the layout for the content pane
     */
    public Form(Layout contentPaneLayout) {
        super(new BorderLayout());
        contentPane = new Container(contentPaneLayout);
        setUIID("Form");
        // forms/dialogs are not visible by default
        setVisible(false);
        Style formStyle = getStyle();
        Display d = Display.getInstance();
        int w = d.getDisplayWidth() - (formStyle.getHorizontalMargins());
        int h = d.getDisplayHeight() - (formStyle.getVerticalMargins());

        setWidth(w);
        setHeight(h);
        setPreferredSize(new Dimension(w, h));
        super.setAlwaysTensile(false);

        title.setEndsWith3Points(false);
        titleArea.addComponent(BorderLayout.CENTER, title);
        titleArea.setUIID("TitleArea");
        addComponentToForm(BorderLayout.NORTH, titleArea);
        addComponentToForm(BorderLayout.CENTER, contentPane);

        initAdPadding(d);
        
        contentPane.setUIID("ContentPane");
        contentPane.setScrollableY(true);

        if (title.getText() != null && title.shouldTickerStart()) {
            title.startTicker(getUIManager().getLookAndFeel().getTickerSpeed(), true);
        }

        initTitleBarStatus();
        
        // hardcoded, anything else is just pointless...
        formStyle.setBgTransparency(0xFF);

        initGlobalToolbar();
    }
    
    /**
     * Checks if custom cursors are enabled on this form.  They are turned off by default since 
     * they incur some overhead.
     * @return True if cursors are enabled on this form.
     * @see #setEnableCursors(boolean) 
     * @see Component#setCursor(int) 
     */
    public boolean isEnableCursors() {
        return enableCursors;
    }
    
    /**
     * Enable or disable custom cursors on this form.  They are turned off by default since they incur some overhead.
     * @param e True to enable cursors.  False to disable them.
     * @see Component#setCursor(int) 
     */
    public void setEnableCursors(boolean e) {
        this.enableCursors = e;
    }
    
    /**
     * Sets the current virtual input device for the form.  This will execute the {@link VirtualInputDevice#close() }
     * method of the current input device, and then set {@literal device} as the new current input device.
     * 
     * <p>Some examples of virtual input devices are the Picker widget and the virtual keyboard.</p>
     * @param device
     * @throws Exception 
     */
    public void setCurrentInputDevice(VirtualInputDevice device) throws Exception {
        if (currentInputDevice != null) {
            currentInputDevice.close();
        }
        currentInputDevice = device;
    }
    
    /**
     * Returns the current virtual input device in the form.
     * 
     * @return The current input device in the form.
     * @see #setCurrentInputDevice(com.codename1.ui.VirtualInputDevice) 
     */
    public VirtualInputDevice getCurrentInputDevice() {
        return currentInputDevice;
    }
    
    
    
    /**
     * Allows subclasses to disable the global toolbar for a specific form by overriding this method
     */
    protected void initGlobalToolbar() {
        if(Toolbar.isGlobalToolbar()) {
            setToolbar(new Toolbar());
        }
    }

    static int getInvisibleAreaUnderVKB(Form f) {
        if(f == null) {
            return 0;
        }
        return f.getInvisibleAreaUnderVKB();
    }
    
    /**
     * In some virtual keyboard implementations (notably iOS) this value is used to determine the height of 
     * the virtual keyboard
     * 
     * @return height in pixels of the virtual keyboard
     */
    public int getInvisibleAreaUnderVKB() {
        if(bottomPaddingMode) {
            return 0;
        }
        return Display.impl.getInvisibleAreaUnderVKB();
    }
        
    /**
     * Returns the animation manager instance responsible for this form, this can be used to track/queue
     * animations
     * 
     * @return the animation manager
     */
    public AnimationManager getAnimationManager() {
        return animMananger;
    }
    
    /**
     * Toggles the way the virtual keyboard behaves, enabling this mode shrinks the screen but makes editing
     * possible when working with text fields that aren't in a scrollable container.
     * @param b true to enable false to disable
     */
    public void setFormBottomPaddingEditingMode(boolean b) {
        bottomPaddingMode = b;
    }
    
    /**
     * Toggles the way the virtual keyboard behaves, enabling this mode shrinks the screen but makes editing
     * possible when working with text fields that aren't in a scrollable container.
     * 
     * @return true when this mode is enabled
     */
    public boolean isFormBottomPaddingEditingMode() {
        return bottomPaddingMode;
    }
    
    void initAdPadding(Display d) {
        // this is injected automatically by the implementation in case of ads
        String adPaddingBottom = d.getProperty("adPaddingBottom", null);
        if(adPaddingBottom != null && adPaddingBottom.length() > 0) {
            Container pad = new Container();
            int dim = Integer.parseInt(adPaddingBottom);
            dim = d.convertToPixels(dim, true);
            if(Display.getInstance().isTablet()) {
                dim *= 2;
            }
            pad.setPreferredSize(new Dimension(dim, dim));
            addComponentToForm(BorderLayout.SOUTH, pad);
        }        
    }
    
    /**
     * This method returns the value of the theme constant {@code paintsTitleBarBool} and it is
     * invoked internally in the code. You can override this method to toggle the appearance of the status
     * bar on a per-form basis
     * @return the value of the {@code paintsTitleBarBool} theme constant
     */
    protected boolean shouldPaintStatusBar() {
        return getUIManager().isThemeConstant("paintsTitleBarBool", false);
    }
    
    /**
     * Subclasses can override this method to control the creation of the status bar component.
     * Notice that this method will only be invoked if the paintsTitleBarBool theme constant is true
     * which it is on iOS by default
     * @return a Component that represents the status bar if the OS requires status bar spacing
     */
    protected Component createStatusBar() {
        if(getUIManager().isThemeConstant("statusBarScrollsUpBool", true)) {
            Button bar = new Button();
            bar.setShowEvenIfBlank(true);
            bar.setUIID("StatusBar");
            bar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    Component c = findScrollableChild(getContentPane());
                    if(c != null) {
                        c.scrollRectToVisible(new Rectangle(0, 0, 10, 10), c);
                    }
                }
            });
            return bar;
        } else {
            Container bar = new Container();
            bar.setUIID("StatusBar");
            return bar;
        }
    }
    
    /**
     * Here so dialogs can disable this
     */
    void initTitleBarStatus() {
        if(shouldPaintStatusBar()) {
            // check if its already added:
            if(((BorderLayout)titleArea.getLayout()).getNorth() == null) {
                titleArea.addComponent(BorderLayout.NORTH, createStatusBar());
            }
        }
    }

    Component findScrollableChild(Container c) {
        if(c.isScrollableY()) {
            return c;
        }
        int count = c.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            Component comp = c.getComponentAt(iter);
            if(comp.isScrollableY()) {
                return comp;
            }
            if(comp instanceof Container) {
                Component chld = findScrollableChild((Container)comp);
                if(chld != null) {
                    return chld;
                }
            }
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isAlwaysTensile() {
        return getContentPane().isAlwaysTensile();
    }

    /**
     * Allows grabbing a flag that is used by convention to indicate that you are running an exclusive animation.
     * This is used by some code to prevent collision between optional animation
     * 
     * @return whether the lock was acquired or not
     * @deprecated this is effectively invalidated by the newer animation framework
     */
    public boolean grabAnimationLock() {
        if(globalAnimationLock) {
            return false;
        }
        globalAnimationLock = true;
        return true;
    }
    
    /**
     * Invoke this to release the animation lock that was grabbed in grabAnimationLock
     * @deprecated this is effectively invalidated by the newer animation framework
     */
    public void releaseAnimationLock() {
        globalAnimationLock = false;
    }
    
    /**
     * Returns the component on this form that is currently being edited, or null
     * if no component is currently being edited.
     * @return The currently edited component on this form.
     * @see Component#isEditing() 
     */
    public Component findCurrentlyEditingComponent() {
        return ComponentSelector.select("*", this).filter(new Filter() {

            @Override
            public boolean filter(Component c) {
                return c.isEditing();
            }
            
        }).asComponent();
    }
    
    /**
     * {@inheritDoc}
     */
    public void setAlwaysTensile(boolean alwaysTensile) {
        getContentPane().setAlwaysTensile(alwaysTensile);
    }

    /**
     * Title area manipulation might break with future changes to Codename One and might 
     * damage themeing/functionality of the Codename One application in some platforms
     * 
     * @return the container containing the title
     * @deprecated this method was exposed to allow some hacks, you are advised not to use it. 
     * There are some alternatives such as command behavior (thru Display or the theme constants)
     */
    public Container getTitleArea() {
        if(toolbar != null && toolbar.getParent() != null){
            return toolbar;
        }
        return titleArea;
    }

    public UIManager getUIManager() {
        if (uiManager != null) {
            return uiManager;
        } else {
            return UIManager.getInstance();
        }
    }

    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
        refreshTheme(false);
    }

    /**
     * This listener would be invoked when show is completed
     * 
     * @param l listener
     */
    public void addShowListener(ActionListener l) {
        if (showListener == null) {
            showListener = new EventDispatcher();
        }
        showListener.addListener(l);
    }

    /**
     * Removes the show listener
     *
     * @param l the listener 
     */
    public void removeShowListener(ActionListener l) {
        if (showListener == null) {
            return;
        }
        showListener.removeListener(l);
    }
    
    /**
     * Removes all Show Listeners from this Form
     */ 
    public void removeAllShowListeners(){
        if(showListener != null){
            showListener.getListenerCollection().clear();
            showListener = null;
        }
    }

    /**
     * This listener is invoked when device orientation changes on devices that support orientation change
     *
     * @param l listener
     */
    public void addOrientationListener(ActionListener l) {
        if (orientationListener == null) {
            orientationListener = new EventDispatcher();
        }
        orientationListener.addListener(l);
    }

    /**
     * This listener is invoked when device orientation changes on devices that support orientation change
     *
     * @param l the listener
     */
    public void removeOrientationListener(ActionListener l) {
        if (orientationListener == null) {
            return;
        }
        orientationListener.removeListener(l);
    }

    /**
     * This listener is invoked when device size is changed
     *
     * @param l listener
     */
    public void addSizeChangedListener(ActionListener l) {
        if (sizeChangedListener == null) {
            sizeChangedListener = new EventDispatcher();
        }
        sizeChangedListener.addListener(l);
    }

    /**
     * Remove SizeChangedListener
     *
     * @param l the listener
     */
    public void removeSizeChangedListener(ActionListener l) {
        if (sizeChangedListener == null) {
            return;
        }
        sizeChangedListener.removeListener(l);
    }
    
    /**
     * This method is only invoked when the underlying canvas for the form is hidden
     * this method isn't called for form based events and is generally usable for
     * suspend/resume based behavior
     */
    protected void hideNotify() {
        setVisible(false);
    }

    /**
     * This method is only invoked when the underlying canvas for the form is shown
     * this method isn't called for form based events and is generally usable for
     * suspend/resume based behavior
     */
    protected void showNotify() {
        setVisible(true);
    }

    /**
     * This method is only invoked when the underlying canvas for the form gets
     * a size changed event.
     * This method will trigger a relayout of the Form.
     * This method will get the callback only if this Form is the Current Form
     *
     * @param w the new width of the Form
     * @param h the new height of the Form
     */
    protected void sizeChanged(int w, int h) {
    }

    /**
     * This method is only invoked when the underlying canvas for the form gets
     * a size changed event.
     * This method will trigger a relayout of the Form.
     * This method will get the callback only if this Form is the Current Form
     * @param w the new width of the Form
     * @param h the new height of the Form
     */
    void sizeChangedInternal(int w, int h) {
        int oldWidth = getWidth();
        int oldHeight = getHeight();        
        sizeChanged(w, h);
        Style formStyle = getStyle();
        w = w - (formStyle.getHorizontalMargins());
        h = h - (formStyle.getVerticalMargins());
        setSize(new Dimension(w, h));
        setShouldCalcPreferredSize(true);
        doLayout();
        focused = getFocused();
        if (focused != null) {
            Component.setDisableSmoothScrolling(true);
            scrollComponentToVisible(focused);
            Component.setDisableSmoothScrolling(false);
        }
        
        if(oldWidth != w && oldHeight != h){
            if (orientationListener != null) {
                orientationListener.fireActionEvent(new ActionEvent(this,ActionEvent.Type.OrientationChange));
            }
            boolean a = getContentPane().onOrientationChange();
            if(getToolbar() != null) {
                if(getToolbar().onOrientationChange() || a) {
                    forceRevalidate();
                }
            } else {
                if(a) {
                    forceRevalidate();
                }
            }
        }
        if (sizeChangedListener != null) {
            sizeChangedListener.fireActionEvent(new ActionEvent(this, ActionEvent.Type.SizeChange, w, h));
        }
        
        repaint();
    }

    /**
     * <p>Allows a developer that doesn't derive from the form to draw on top of the 
     * form regardless of underlying changes or animations. This is useful for
     * watermarks or special effects (such as tinting) it is also useful for generic
     * drawing of validation errors etc... A glass pane is generally 
     * transparent or translucent and allows the the UI below to be seen.</p>
     * <p>
     * The example shows a glasspane running on top of a field to show a validation hint,
     * notice that for real world usage you should probably look into {@link com.codename1.ui.validation.Validator}
     * </p>
     * <script src="https://gist.github.com/codenameone/f5b83373088600b19610.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-glasspane.png" alt="Sample of glasspane" />
     * 
     * @param glassPane a new glass pane to install. It is generally recommended to
     * use a painter chain if more than one painter is required.
     */
    public void setGlassPane(Painter glassPane) {
        this.glassPane = glassPane;
        repaint();
    }

    /**
     * Indicates if the section within the X/Y area is a "drag region" where
     * we expect people to drag and never actually "press" in which case we
     * can instantly start dragging making perceived performance faster. This
     * is invoked by the implementation code to optimize drag start behavior
     * @param x x location for the touch
     * @param y y location for the touch 
     * @return true if the touch is in a region specifically designated as a "drag region"
     * @deprecated this method was replaced by getDragRegionStatus
     */
    public boolean isDragRegion(int x, int y) {
        if(getMenuBar().isDragRegion(x, y)) {
            return true;
        }
        if (formLayeredPane != null && formLayeredPane.isDragRegion(x, y)) {
            return true;
        }
        Container actual = getActualPane();
        Component c = actual.getComponentAt(x, y);
        while (c != null && c.isIgnorePointerEvents()) {
            c = c.getParent();
        }
        return c != null && c.isDragRegion(x, y);
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
    public int getDragRegionStatus(int x, int y) {
        int menuBarDrag = getMenuBar().getDragRegionStatus(x, y);
        if(menuBarDrag != DRAG_REGION_NOT_DRAGGABLE) {
            return menuBarDrag;
        }
        int formLayeredPaneDrag = formLayeredPane != null ? 
                formLayeredPane.getDragRegionStatus(x, y) : 
                DRAG_REGION_NOT_DRAGGABLE;
        if (formLayeredPaneDrag != DRAG_REGION_NOT_DRAGGABLE) {
            return formLayeredPaneDrag;
        }
        Container actual = getActualPane();
        
        // no idea how this can happen
        if(actual != null) {
            Component c = actual.getComponentAt(x, y);
            while (c != null && c.isIgnorePointerEvents()) {
                c = c.getParent();
            }
            if(c != null) {
                return c.getDragRegionStatus(x, y);
            }
            if(isScrollable()) {
                return DRAG_REGION_LIKELY_DRAG_Y;
            }
        }
        return DRAG_REGION_NOT_DRAGGABLE;
    }
    
    /**
     * This method can be overriden by a component to draw on top of itself or its children
     * after the component or the children finished drawing in a similar way to the glass
     * pane but more refined per component
     *
     * @param g the graphics context
     */
    void paintGlassImpl(Graphics g) {
        if (getParent() != null) {
            super.paintGlassImpl(g);
            return;
        }
        if (glassPane != null) {
            int tx = g.getTranslateX();
            int ty = g.getTranslateY();
            g.translate(-tx, -ty);
            glassPane.paint(g, getBounds());
            g.translate(tx, ty);
        }
        paintGlass(g);
        if (dragged != null && dragged.isDragAndDropInitialized()) {
            int[] c = g.getClip();
            g.setClip(0, 0, getWidth(), getHeight());
            dragged.drawDraggedImage(g);
            g.setClip(c);
        }
    }

    /**
     * <p>Allows a developer that doesn't derive from the form to draw on top of the 
     * form regardless of underlying changes or animations. This is useful for
     * watermarks or special effects (such as tinting) it is also useful for generic
     * drawing of validation errors etc... A glass pane is generally 
     * transparent or translucent and allows the the UI below to be seen.</p>
     * <p>
     * The example shows a glasspane running on top of a field to show a validation hint,
     * notice that for real world usage you should probably look into {@link com.codename1.ui.validation.Validator}
     * </p>
     * <script src="https://gist.github.com/codenameone/f5b83373088600b19610.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-glasspane.png" alt="Sample of glasspane" />
     * 
     * @return the instance of the glass pane for this form
     * @see com.codename1.ui.painter.PainterChain#installGlassPane(Form, com.codename1.ui.Painter) 
     */
    public Painter getGlassPane() {
        return glassPane;
    }

    /**
     * Sets the style of the title programmatically
     * 
     * @param s new style
     * @deprecated this method doesn't take into consideration multiple styles
     */
    public void setTitleStyle(Style s) {
        title.setUnselectedStyle(s);
    }

    /**
     * Allows modifying the title attributes beyond style (e.g. setting icon/alignment etc.)
     * 
     * @return the component representing the title for the form
     */
    public Label getTitleComponent() {
        return title;
    }

    /**
     * Allows replacing the title with a different title component, thus allowing
     * developers to create more elaborate title objects.
     *
     * @param title new title component
     */
    public void setTitleComponent(Label title) {
        titleArea.replace(this.title, title, false);
        this.title = title;
    }

    /**
     * Allows replacing the title with a different title component, thus allowing
     * developers to create more elaborate title objects. This version of the
     * method allows special effects for title replacement such as transitions
     * for title entering
     *
     * @param title new title component
     * @param t transition for title replacement
     */
    public void setTitleComponent(Label title, Transition t) {
        titleArea.replace(this.title, title, t);
        this.title = title;
    }

    /**
     * Add a key listener to the given keycode for a callback when the key is released
     * 
     * @param keyCode code on which to send the event
     * @param listener listener to invoke when the key code released.
     */
    public void addKeyListener(int keyCode, ActionListener listener) {
        if (keyListeners == null) {
            keyListeners = new HashMap<Integer, ArrayList<ActionListener>>();
        }
        addKeyListener(keyCode, listener, keyListeners);
    }

    /**
     * Removes a key listener from the given keycode 
     * 
     * @param keyCode code on which the event is sent
     * @param listener listener instance to remove
     */
    public void removeKeyListener(int keyCode, ActionListener listener) {
        if (keyListeners == null) {
            return;
        }
        removeKeyListener(keyCode, listener, keyListeners);
    }

    /**
     * Removes a game key listener from the given game keycode 
     * 
     * @param keyCode code on which the event is sent
     * @param listener listener instance to remove
     */
    public void removeGameKeyListener(int keyCode, ActionListener listener) {
        if (gameKeyListeners == null) {
            return;
        }
        removeKeyListener(keyCode, listener, gameKeyListeners);
    }

    private void addKeyListener(int keyCode, ActionListener listener, HashMap<Integer, ArrayList<ActionListener>> keyListeners) {
        if (keyListeners == null) {
            keyListeners = new HashMap<Integer, ArrayList<ActionListener>>();
        }
        Integer code = new Integer(keyCode);
        ArrayList<ActionListener> vec = keyListeners.get(code);
        if (vec == null) {
            vec = new ArrayList<ActionListener>();
            vec.add(listener);
            keyListeners.put(code, vec);
            return;
        }
        if (!vec.contains(listener)) {
            vec.add(listener);
        }
    }

    private void removeKeyListener(int keyCode, ActionListener listener, HashMap<Integer, ArrayList<ActionListener>> keyListeners) {
        if (keyListeners == null) {
            return;
        }
        Integer code = new Integer(keyCode);
        ArrayList<ActionListener> vec = keyListeners.get(code);
        if (vec == null) {
            return;
        }
        vec.remove(listener);
        if (vec.size() == 0) {
            keyListeners.remove(code);
        }
    }

    /**
     * Add a game key listener to the given gamekey for a callback when the 
     * key is released
     * 
     * @param keyCode code on which to send the event
     * @param listener listener to invoke when the key code released.
     */
    public void addGameKeyListener(int keyCode, ActionListener listener) {
        if (gameKeyListeners == null) {
            gameKeyListeners = new HashMap<Integer, ArrayList<ActionListener>>();
        }
        addKeyListener(keyCode, listener, gameKeyListeners);
    }

    /**
     * Returns the number of buttons on the menu bar for use with getSoftButton()
     * 
     * @return the number of softbuttons
     */
    public int getSoftButtonCount() {
        return menuBar.getSoftButtons().length;
    }

    /**
     * Returns the button representing the softbutton, this allows modifying softbutton
     * attributes and behavior programmatically rather than by using the command API.
     * Notice that this API behavior is fragile since the button mapped to a particular
     * offset might change based on the command API
     * 
     * @param offset the offest of the softbutton
     * @return a button that can be manipulated
     */
    public Button getSoftButton(int offset) {
        return menuBar.getSoftButtons()[offset];
    }

    /**
     * Returns the style of the menu
     * 
     * @return the style of the menu
     */
    public Style getMenuStyle() {
        return menuBar.getMenuStyle();
    }

    /**
     * Returns the style of the title
     * 
     * @return the style of the title
     */
    public Style getTitleStyle() {
        return title.getStyle();
    }

    /**
     * Allows the display to skip the menu dialog if that is the current form
     */
    Form getPreviousForm() {
        return previousForm;
    }

    /**
     * {@inheritDoc}
     */
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        LookAndFeel laf = uim.getLookAndFeel();
        transitionOutAnimator = laf.getDefaultFormTransitionOut();
        transitionInAnimator = laf.getDefaultFormTransitionIn();
        focusScrolling = laf.isFocusScrolling();
        if (menuBar == null || !menuBar.getClass().equals(laf.getMenuBarClass())) {
            try {
                menuBar = (MenuBar) laf.getMenuBarClass().newInstance();
            } catch (Exception ex) {
                Log.e(ex);
                menuBar = new MenuBar();
            }
            menuBar.initMenuBar(this);
        }

        tintColor = laf.getDefaultFormTintColor();
        tactileTouchDuration = laf.getTactileTouchDuration();
    }

    /**
     * Sets the current dragged Component
     */
    void setDraggedComponent(Component dragged) {
        this.dragged = dragged;
    }
    
    /**
     * Gets the current dragged Component
     */
    Component getDraggedComponent() {
        return dragged;
    }

    /**
     * Returns true if the given dest component is in the column of the source component
     */
    private boolean isInSameColumn(Component source, Component dest) {
        // workaround for NPE
        if(source == null || dest == null) {
            return false;
        }
        return Rectangle.intersects(source.getAbsoluteX(), 0,
                source.getWidth(), Integer.MAX_VALUE, dest.getAbsoluteX(), dest.getAbsoluteY(),
                dest.getWidth(), dest.getHeight());
    }

    /**
     * Returns true if the given dest component is in the row of the source component
     */
    private boolean isInSameRow(Component source, Component dest) {
        return Rectangle.intersects(0, source.getAbsoluteY(),
                Integer.MAX_VALUE, source.getHeight(), dest.getAbsoluteX(), dest.getAbsoluteY(),
                dest.getWidth(), dest.getHeight());
    }

    /**
     * Default command is invoked when a user presses fire, this functionality works
     * well in some situations but might collide with elements such as navigation
     * and combo boxes. Use with caution.
     * 
     * @param defaultCommand the command to treat as default
     */
    public void setDefaultCommand(Command defaultCommand) {
        menuBar.setDefaultCommand(defaultCommand);
    }

    /**
     * Default command is invoked when a user presses fire, this functionality works
     * well in some situations but might collide with elements such as navigation
     * and combo boxes. Use with caution.
     * 
     * @return the command to treat as default
     */
    public Command getDefaultCommand() {
        return menuBar.getDefaultCommand();
    }

    /**
     * Indicates the command that is defined as the clear command in this form.
     * A clear command can be used both to map to a "clear" hardware button 
     * if such a button exists.
     * 
     * @param clearCommand the command to treat as the clear Command
     */
    public void setClearCommand(Command clearCommand) {
        menuBar.setClearCommand(clearCommand);
    }

    /**
     * Indicates the command that is defined as the clear command in this form.
     * A clear command can be used both to map to a "clear" hardware button 
     * if such a button exists.
     * 
     * @return the command to treat as the clear Command
     */
    public Command getClearCommand() {
        return menuBar.getClearCommand();
    }

    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     * 
     * @param backCommand the command to treat as the back Command
     */
    public void setBackCommand(Command backCommand) {
        menuBar.setBackCommand(backCommand);
    }

    /**
     * Shorthand for {@link #setBackCommand(com.codename1.ui.Command)} that
     * dynamically creates the command using {@link com.codename1.ui.Command#create(java.lang.String, com.codename1.ui.Image, com.codename1.ui.events.ActionListener)}.
     * 
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command setBackCommand(String name, Image icon, ActionListener ev) {
        Command cmd = Command.create(name, icon, ev);
        menuBar.setBackCommand(cmd);
        return cmd;
    }

    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     * 
     * @return the command to treat as the back Command
     */
    public Command getBackCommand() {
        return menuBar.getBackCommand();
    }

    /**
     * Sets the title after invoking the constructor
     * 
     * @param title the form title
     */
    public Form(String title) {
        this();
        setTitle(title);
//        this.title.setText(title);
    }

    /**
     * Sets the title after invoking the constructor
     * 
     * @param title the form title
     * @param contentPaneLayout the layout for the content pane
     */
    public Form(String title, Layout contentPaneLayout) {
        this(contentPaneLayout);
        setTitle(title);
    }

    /**
     * This method returns the Content pane instance
     * 
     * @return a content pane instance
     */
    public Container getContentPane() {
        return contentPane;
    }
    
    /**
     * This method returns the layered pane of the Form, the layered pane is laid
     * on top of the content pane and is created lazily upon calling this method the layer
     * will be created. This is equivalent to getLayeredPane(null, false).
     * 
     * @return the LayeredPane
     */ 
    public Container getLayeredPane() {
        return getLayeredPane(null, false);
    }
    
    /**
     * Returns the layered pane for the class and if one doesn't exist a new one is created dynamically and returned
     * @param c the class with which this layered pane is associated, null for the global layered pane which
     * is always on the bottom
     * @param top if created this indicates whether the layered pane should be added on top or bottom
     * @return the layered pane instance
     */
    public Container getLayeredPane(Class c, boolean top) {
        Container layeredPaneImpl = getLayeredPaneImpl();
        if(c == null) {
            // NOTE: We need to use getChildrenAsList(true) rather than simply iterating
            // over layeredPaneImpl because the latter won't find components while an animation
            // is in progress.... We could end up adding a whole bunch of layered panes
            // by accident
             for(Component cmp : layeredPaneImpl.getChildrenAsList(true)) {
                 if(cmp.getClientProperty("cn1$_cls") == null) {
                     return (Container)cmp;
                 }
             } 
        }
        String n = c.getName();
        // NOTE: We need to use getChildrenAsList(true) rather than simply iterating
        // over layeredPaneImpl because the latter won't find components while an animation
        // is in progress.... We could end up adding a whole bunch of layered panes
        // by accident
        java.util.List<Component> children = layeredPaneImpl.getChildrenAsList(true);
        for(Component cmp : children) {
            if(n.equals(cmp.getClientProperty("cn1$_cls"))) {
                return (Container)cmp;
            }
        } 
        
        Container cnt = new Container();
        int zIndex = 0;
        int componentCount =  children.size();
        if(top) {
            if (componentCount > 0) {
                Integer z = (Integer)children.get(componentCount-1).getClientProperty(Z_INDEX_PROP);
                if (z != null) {
                    zIndex = z.intValue();
                }
            }
            layeredPaneImpl.add(cnt);
        } else {
            if (componentCount > 0) {
                if (componentCount > 0) {
                    Integer z = (Integer)children.get(0).getClientProperty(Z_INDEX_PROP);
                    if (z != null) {
                        zIndex = z.intValue();
                    }
                }
            }
            layeredPaneImpl.addComponent(0, cnt);            
        }
        cnt.putClientProperty("cn1$_cls", n);
        cnt.putClientProperty(Z_INDEX_PROP, zIndex);
        return cnt;
    }
    
    private static final String Z_INDEX_PROP = "cn1$_zIndex";
    
    /**
     * Returns the layered pane for the class and if one doesn't exist a new one is created dynamically and returned
     * @param c the class with which this layered pane is associated, null for the global layered pane which
     * is always on the bottom
     * @param zIndex if created this indicates the zIndex at which the pane is placed.  Higher z values in front of lower z values.
     * @return the layered pane instance
     */
    public Container getLayeredPane(Class c, int zIndex) {
        Container layeredPaneImpl = getLayeredPaneImpl();
        
        if(c == null) {
            // NOTE: We need to use getChildrenAsList(true) rather than simply iterating
            // over layeredPaneImpl because the latter won't find components while an animation
            // is in progress.... We could end up adding a whole bunch of layered panes
            // by accident
             for(Component cmp : layeredPaneImpl.getChildrenAsList(true)) {
                 if(cmp.getClientProperty("cn1$_cls") == null) {
                     return (Container)cmp;
                 }
             } 
        }
        String n = c.getName();
        // NOTE: We need to use getChildrenAsList(true) rather than simply iterating
        // over layeredPaneImpl because the latter won't find components while an animation
        // is in progress.... We could end up adding a whole bunch of layered panes
        // by accident
        java.util.List<Component> children = layeredPaneImpl.getChildrenAsList(true);
        for(Component cmp : children) {
            if(n.equals(cmp.getClientProperty("cn1$_cls"))) {
                return (Container)cmp;
            }
        } 
        
        Container cnt = new Container();
        cnt.putClientProperty(Z_INDEX_PROP, zIndex);
        int len = children.size();
        int insertIndex = -1;
        
        for (int i=0; i<len; i++) {
            Component cmp = children.get(i);
            Integer cmpZIndex = (Integer)cmp.getClientProperty(Z_INDEX_PROP);
            int cmpZ = cmpZIndex == null ? 0 : cmpZIndex.intValue();
            if (cmpZ >= zIndex) {
                insertIndex = i;
                break;
            }
        }
        
        if(insertIndex == -1) {
            layeredPaneImpl.add(cnt);
        } else {
            layeredPaneImpl.addComponent(insertIndex, cnt);            
        }
        cnt.putClientProperty("cn1$_cls", n);
        return cnt;
    }

    /**
     * Returns the layered pane for the class and if one doesn't exist a new one is created 
     * dynamically and returned. This version of the method returns a layered pane on the whole
     * form
     * @param c the class with which this layered pane is associated, null for the global layered pane which
     * is always on the bottom
     * @param top if created this indicates whether the layered pane should be added on top or bottom
     * @return the layered pane instance
     */
    public Container getFormLayeredPane(Class c, boolean top) {
        if(formLayeredPane == null) {
            formLayeredPane = new Container(new LayeredLayout()) {
                @Override
                protected void paintBackground(Graphics g) {
                    if(getComponentCount() > 0) {
                        if(isVisible()) {
                            setVisible(false);
                            Form.this.paint(g);
                            setVisible(true);
                        }
                    }
                }

                @Override
                public void paintBackgrounds(Graphics g) {
                }
            };
            formLayeredPane.setName("FormLayeredPane");
            addComponentToForm(BorderLayout.OVERLAY, formLayeredPane);
        }
        if(c == null) {
            // NOTE: We need to use getChildrenAsList(true) rather than simply iterating
            // over layeredPaneImpl because the latter won't find components while an animation
            // is in progress.... We could end up adding a whole bunch of layered panes
            // by accident
             for(Component cmp : formLayeredPane.getChildrenAsList(true)) {
                 if(cmp.getClientProperty("cn1$_cls") == null) {
                     return (Container)cmp;
                 }
             }
             
             Container cnt = new Container();
             cnt.setWidth(getWidth());
             cnt.setHeight(getHeight());
             cnt.setShouldLayout(false);
             cnt.setName("FormLayer: " + c.getName());
             formLayeredPane.add(cnt);
             return cnt;
        }
        String n = c.getName();
        // NOTE: We need to use getChildrenAsList(true) rather than simply iterating
        // over layeredPaneImpl because the latter won't find components while an animation
        // is in progress.... We could end up adding a whole bunch of layered panes
        // by accident
        for(Component cmp : formLayeredPane.getChildrenAsList(true)) {
            if(n.equals(cmp.getClientProperty("cn1$_cls"))) {
                return (Container)cmp;
            }
        } 
        Container cnt = new Container();
        cnt.setWidth(getWidth());
        cnt.setHeight(getHeight());
        cnt.setShouldLayout(false);
        cnt.setName("FormLayer: " + c.getName());
        if(top) {
            formLayeredPane.add(cnt);
        } else {
            formLayeredPane.addComponent(0, cnt);            
        }
        cnt.putClientProperty("cn1$_cls", n);
        return cnt;
    }
    
    /**
     * This method returns the layered pane of the Form, the layered pane is laid
     * on top of the content pane and is created lazily upon calling this method the layer
     * will be created.
     * 
     * @return the LayeredPane
     */ 
    private Container getLayeredPaneImpl() {
        if(layeredPane == null){
            layeredPane = new Container(new LayeredLayout());
            Container parent = contentPane.wrapInLayeredPane();
            // adds the global layered pane
            layeredPane.add(new Container());
            parent.addComponent(layeredPane);
            revalidate();
        }
        return layeredPane;
    }
    
    Container getActualPane(){
        if(layeredPane != null){
            return layeredPane.getParent();
        } else {
            return contentPane;
        }
    }
    
    /**
     * Gets the actual pane, but first checks to see if the provided overlay
     * responds to events at the provided absolute x and y coordinates.
     * @param overlay
     * @param x
     * @param y
     * @return If {@literal overlay} responds to events at {@literal (x,y)} then
     * it returns {@literal overlay}, otherwise it returns the result of {@link #getActualPane() }
     */
    private Container getActualPane(Container overlay, int x, int y) {
        if (overlay != null && overlay.getResponderAt(x, y) != null) {
            return overlay;
        }
        return getActualPane();
    }
    

    /**
     * Removes all Components from the Content Pane
     */
    public void removeAll() {
        contentPane.removeAll();        
    }

    /**
     * Sets the background image to show behind the form
     * 
     * @param bgImage the background image
     * @deprecated Use the style directly
     */
    public void setBgImage(Image bgImage) {
        getStyle().setBgImage(bgImage);
    }

    /**
     * {@inheritDoc}
     */
    public void setLayout(Layout layout) {
        if(layout instanceof BorderLayout) {
            setScrollable(false);
        }
        contentPane.setLayout(layout);
    }

    void updateIcsIconCommandBehavior() {
        int b = Display.getInstance().getCommandBehavior();
        if (b == Display.COMMAND_BEHAVIOR_ICS) {
            if (getTitleComponent().getIcon() == null) {
                Image i = Display.impl.getApplicationIconImage();
                if (i != null) {
                    int h = getTitleComponent().getStyle().getFont().getHeight();
                    i = i.scaled(h, h);
                    getTitleComponent().setIcon(i);
                }
            }
        }
    }

    /**
     * Sets the Form title to the given text
     * 
     * @param title the form title
     */
    public void setTitle(String title) {
        if(toolbar != null){
            toolbar.setTitle(title);
            return;
        }
            
        this.title.setText(title);

        if (!Display.getInstance().isNativeTitle()) {
            updateIcsIconCommandBehavior(); 
            if (isInitialized() && this.title.isTickerEnabled()) {
                int b = Display.getInstance().getCommandBehavior();
                if (b == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK || b == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                        || b == Display.COMMAND_BEHAVIOR_ICS || b == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION) {
                    titleArea.revalidate();
                }
                if (this.title.shouldTickerStart()) {
                    this.title.startTicker(getUIManager().getLookAndFeel().getTickerSpeed(), true);
                } else {
                    if (this.title.isTickerRunning()) {
                        this.title.stopTicker();
                    }
                }
            }
        }else{
            if(super.contains(titleArea)){
                removeComponentFromForm(titleArea);
            }
            //if the Form is already displayed refresh the title
            if(Display.getInstance().getCurrent() == this){
                Display.getInstance().refreshNativeTitle();
            }
        }
    }

    /**
     * Returns the Form title text
     * 
     * @return returns the form title
     */
    public String getTitle() {
        if(toolbar != null) {
            Component cmp = toolbar.getTitleComponent();
            if(cmp instanceof Label) {
                return ((Label)cmp).getText();
            }
            return null;
        }
        return title.getText();
    }

    /**
     * Adds Component to the Form's Content Pane
     * 
     * @param cmp the added param
     */
    public void addComponent(Component cmp) {
        contentPane.addComponent(cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void addComponent(Object constraints, Component cmp) {
        contentPane.addComponent(constraints, cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void addComponent(int index, Object constraints, Component cmp) {
        contentPane.addComponent(index, constraints, cmp);
    }

    /**
     * Adds Component to the Form's Content Pane
     * 
     * @param cmp the added param
     */
    public void addComponent(int index, Component cmp) {
        contentPane.addComponent(index, cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void replace(Component current, Component next, Transition t) {
        contentPane.replace(current, next, t);
    }

    /**
     * {@inheritDoc}
     */
    public void replaceAndWait(Component current, Component next, Transition t) {
        contentPane.replaceAndWait(current, next, t);
    }

    /**
     * Removes a component from the Form's Content Pane
     * 
     * @param cmp the component to be removed
     */
    public void removeComponent(Component cmp) {
        contentPane.removeComponent(cmp);
    }

    final void addComponentToForm(Object constraints, Component cmp) {
        super.addComponent(constraints, cmp);
    }

    void removeComponentFromForm(Component cmp) {
        super.removeComponent(cmp);
    }

    /**
     * Registering media component to this Form, that like to receive 
     * animation events
     * 
     * @param mediaCmp the Form media component to be registered
     */
    void registerMediaComponent(Component mediaCmp) {
        if (mediaComponents == null) {
            mediaComponents = new ArrayList<Component>();
        }
        if (!mediaComponents.contains(mediaCmp)) {
            mediaComponents.add(mediaCmp);
        }
    }

    /**
     * Used by the implementation to prevent flickering when flushing the double buffer
     * 
     * @return true if the form has media components within it
     */
    public final boolean hasMedia() {
        return mediaComponents != null && mediaComponents.size() > 0;
    }

    /**
     * Indicate that cmp would no longer like to receive animation events
     * 
     * @param cmp component that would no longer receive animation events
     */
    void deregisterMediaComponent(Component mediaCmp) {
        mediaComponents.remove(mediaCmp);
    }

    /**
     * The given component is interested in animating its appearance and will start
     * receiving callbacks when it is visible in the form allowing it to animate
     * its appearance. This method would not register a compnent instance more than once
     * 
     * @param cmp component that would be animated
     */
    public void registerAnimated(Animation cmp) {
        if (animatableComponents == null) {
            animatableComponents = new ArrayList<Animation>();
        }
        if (!animatableComponents.contains(cmp)) {
            animatableComponents.add(cmp);
        }
        Display.getInstance().notifyDisplay();
    }

    /**
     * Identical to the none-internal version, the difference between the internal/none-internal
     * is that it references a different vector that is unaffected by the user actions.
     * That is why we can dynamically register/deregister without interfering with user interaction.
     */
    void registerAnimatedInternal(Animation cmp) {
        if (internalAnimatableComponents == null) {
            internalAnimatableComponents = new ArrayList<Animation>();
        }
        if (!internalAnimatableComponents.contains(cmp)) {
            internalAnimatableComponents.add(cmp);
        }
        Display.getInstance().notifyDisplay();
    }

    /**
     * Identical to the none-internal version, the difference between the internal/none-internal
     * is that it references a different vector that is unaffected by the user actions.
     * That is why we can dynamically register/deregister without interfearing with user interaction.
     */
    void deregisterAnimatedInternal(Animation cmp) {
        if (internalAnimatableComponents != null) {
            internalAnimatableComponents.remove(cmp);
        }
    }

    /**
     * Indicate that cmp would no longer like to receive animation events
     * 
     * @param cmp component that would no longer receive animation events
     */
    public void deregisterAnimated(Animation cmp) {
        if (animatableComponents != null) {
            animatableComponents.remove(cmp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        if (getParent() != null) {
            repaintAnimations();
        }
        return super.animate();
    }

    /**
     * Makes sure all animations are repainted so they would be rendered in every
     * frame
     */
    void repaintAnimations() {
        if(rippleComponent != null) {
            rippleComponent.repaint();
            if(rippleMotion == null) {
                rippleComponent = null;
            } 
        }
        if (animatableComponents != null) {
            loopAnimations(animatableComponents, null);
        }
        if (internalAnimatableComponents != null) {
            loopAnimations(internalAnimatableComponents, animatableComponents);
        }
        if(animMananger != null) {
            animMananger.updateAnimations();
        }
    }

    private void loopAnimations(ArrayList<Animation> v, ArrayList<Animation> notIn) {
        // we don't save size() in a varible since the animate method may deregister
        // the animation thus invalidating the size
        for (int iter = 0; iter < v.size(); iter++) {
            Animation c = (Animation) v.get(iter);
            if (c == null || notIn != null && notIn.contains(c)) {
                continue;
            }
            if (c.animate()) {
                if (c instanceof Component) {
                    Rectangle rect = ((Component) c).getDirtyRegion();
                    if (rect != null) {
                        Dimension d = rect.getSize();

                        // this probably can't happen but we got a really weird partial stack trace to this
                        // method and this check doesn't hurt
                        if (d != null) {
                            ((Component) c).repaint(rect.getX(), rect.getY(), d.getWidth(), d.getHeight());
                        }
                    } else {
                        ((Component) c).repaint();
                    }
                } else {
                    Display.getInstance().repaint(c);
                }
            }
        }
    }

    /**
     * If this method returns true the EDT won't go to sleep indefinitely
     * 
     * @return true is form has animation; otherwise false
     */
    boolean hasAnimations() {
        return (animatableComponents != null && animatableComponents.size() > 0)
                || (internalAnimatableComponents != null && internalAnimatableComponents.size() > 0) 
                || (animMananger != null && animMananger.isAnimating());
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        // when changing the theme when a title/menu bar is not visible the refresh
        // won't apply to them. We need to protect against this occurance.
        if (menuBar != null) {
            menuBar.refreshTheme(merge);
        }
        if (titleArea != null) {
            titleArea.refreshTheme(merge);
        }
        if (toolbar != null) {
            toolbar.refreshTheme(merge);
        }
        
        super.refreshTheme(merge);

        if (toolbar == null) {
            // when  changing the theme the menu behavior might also change
            hideMenu();
            restoreMenu();
            Command[] cmds = new Command[getCommandCount()];
            for (int iter = 0; iter < cmds.length; iter++) {
                cmds[iter] = getCommand(iter);
            }
            removeAllCommands();
            for (int iter = 0; iter < cmds.length; iter++) {
                addCommand(cmds[iter], getCommandCount());
            }
            if (getBackCommand() != null) {
                setBackCommand(getBackCommand());
            }
        }

        revalidate();
    }

    /**
     * Exposing the background painting for the benefit of animations
     * 
     * @param g the form graphics
     */
    public void paintBackground(Graphics g) {
        super.paintBackground(g);
    }

    /**
     * This property allows us to define a an animation that will draw the transition for
     * entering this form. A transition is an animation that would occur when 
     * switching from one form to another.
     * 
     * @return the Form in transition
     */
    public Transition getTransitionInAnimator() {
        return transitionInAnimator;
    }

    /**
     * This property allows us to define a an animation that will draw the transition for
     * entering this form. A transition is an animation that would occur when 
     * switching from one form to another.
     * 
     * @param transitionInAnimator the Form in transition
     */
    public void setTransitionInAnimator(Transition transitionInAnimator) {
        this.transitionInAnimator = transitionInAnimator;
    }

    /**
     * This property allows us to define a an animation that will draw the transition for
     * exiting this form. A transition is an animation that would occur when 
     * switching from one form to another.
     * 
     * @return the Form out transition
     */
    public Transition getTransitionOutAnimator() {
        return transitionOutAnimator;
    }

    /**
     * This property allows us to define a an animation that will draw the transition for
     * exiting this form. A transition is an animation that would occur when 
     * switching from one form to another.
     * 
     * @param transitionOutAnimator the Form out transition
     */
    public void setTransitionOutAnimator(Transition transitionOutAnimator) {
        this.transitionOutAnimator = transitionOutAnimator;
    }

    /**
     * A listener that is invoked when a command is clicked allowing multiple commands
     * to be handled by a single block
     *
     * @param l the command action listener
     */
    public void addCommandListener(ActionListener l) {
        if (commandListener == null) {
            commandListener = new EventDispatcher();
        }
        commandListener.addListener(l);
    }

    /**
     * A listener that is invoked when a command is clicked allowing multiple commands
     * to be handled by a single block
     *
     * @param l the command action listener
     */
    public void removeCommandListener(ActionListener l) {
        commandListener.removeListener(l);
    }

    /**
     * Invoked to allow subclasses of form to handle a command from one point
     * rather than implementing many command instances. All commands selected 
     * on the form will trigger this method implicitly.
     * 
     * @param cmd the form commmand object
     */
    protected void actionCommand(Command cmd) {
    }

    /**
     * Dispatches a command via the standard form mechanism of firing a command event
     * 
     * @param cmd The command to dispatch
     * @param ev the event to dispatch 
     */
    public void dispatchCommand(Command cmd, ActionEvent ev) {
        cmd.actionPerformed(ev);
        if (!ev.isConsumed()) {
            actionCommandImpl(cmd, ev);
        }
    }

    /**
     * Invoked to allow subclasses of form to handle a command from one point
     * rather than implementing many command instances
     */
    void actionCommandImpl(Command cmd) {
        actionCommandImpl(cmd, new ActionEvent(cmd,ActionEvent.Type.Command));
    }

    /**
     * Invoked to allow subclasses of form to handle a command from one point
     * rather than implementing many command instances
     */
    void actionCommandImpl(Command cmd, ActionEvent ev) {
        if (cmd == null) {
            return;
        }

        if (comboLock) {
            if (cmd == menuBar.getCancelMenuItem()) {
                actionCommand(cmd);
                return;
            }
            Component c = getFocused();
            if (c != null) {
                c.fireClicked();
            }
            return;
        }
        if (cmd != menuBar.getSelectCommand()) {
            if (commandListener != null) {
                commandListener.fireActionEvent(ev);
                if (ev.isConsumed()) {
                    return;
                }
            }
            actionCommand(cmd);
        } else {
            Component c = getFocused();
            if (c != null) {
                c.fireClicked();
            }
        }
    }

    /**
     * Invoked to allow subclasses of form to handle a command from one point
     * rather than implementing many command instances
     */
    void actionCommandImplNoRecurseComponent(Command cmd, ActionEvent ev) {
        if (cmd == null) {
            return;
        }

        if (comboLock) {
            if (cmd == menuBar.getCancelMenuItem()) {
                actionCommand(cmd);
                return;
            }
            return;
        }
        if (cmd != menuBar.getSelectCommand()) {
            if (commandListener != null) {
                commandListener.fireActionEvent(ev);
                if (ev.isConsumed()) {
                    return;
                }
            }
            actionCommand(cmd);
        } 
    }

    void initFocused() {
        if (focused == null) {
            Component focusable = formLayeredPane != null ? 
                    formLayeredPane.findFirstFocusable() : 
                    null;
            if (focusable == null) {
                focusable = getActualPane().findFirstFocusable();
            }
            setFocused(focusable);
            if (!Display.getInstance().shouldRenderSelection()) {
                return;
            }
            layoutContainer();
        }
    }

    /**
     * Displays the current form on the screen
     */
    public void show() {
        Display.impl.onShow(this);
        show(false);
    }

    /**
     * Displays the current form on the screen, this version of the method is
     * useful for "back" navigation since it reverses the direction of the transition.
     */
    public void showBack() {
        show(true);
    }

    /**
     * Displays the current form on the screen
     */
    private void show(boolean reverse) {
        if (transitionOutAnimator == null && transitionInAnimator == null) {
            initLaf(getUIManager());
        }
        initFocused();
        onShow();
        tint = false;
        if (getParent() == null) {
            com.codename1.ui.Display.getInstance().setCurrent(this, reverse);
        } else {
            revalidate();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    void deinitializeImpl() {
        try {
            setCurrentInputDevice(null);
        } catch (Exception ex) {
            Log.e(ex);
        }
        super.deinitializeImpl();
        animMananger.flush();
        buttonsAwatingRelease = null;
        dragged = null;
    }

    /**
     * {@inheritDoc}
     */
    void initComponentImpl() {
        super.initComponentImpl();
        dragged = null;
        if (Display.getInstance().isNativeCommands()) {
            Display.impl.setNativeCommands(menuBar.getCommands());
        }
        if (getParent() != null) {
            getParent().getComponentForm().registerAnimated(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSmoothScrolling(boolean smoothScrolling) {
        // invoked by the constructor for component
        if (contentPane != null) {
            contentPane.setSmoothScrolling(smoothScrolling);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSmoothScrolling() {
        return contentPane.isSmoothScrolling();
    }

    /**
     * {@inheritDoc}
     */
    public int getScrollAnimationSpeed() {
        return contentPane.getScrollAnimationSpeed();
    }

    /**
     * {@inheritDoc}
     */
    public void setScrollAnimationSpeed(int animationSpeed) {
        contentPane.setScrollAnimationSpeed(animationSpeed);
    }

    /**
     * Allows subclasses to bind functionality that occurs when
     * a specific form or dialog appears on the screen
     */
    protected void onShow() {
    }

    /**
     * Allows subclasses to bind functionality that occurs when
     * a specific form or dialog is "really" showing hence when
     * the transition is totally complete (unlike onShow which is called
     * on intent). The necessity for this is for special cases like
     * media that might cause artifacts if played during a transition.
     */
    protected void onShowCompleted() {
    }

    void onShowCompletedImpl() {
        setLightweightMode(false);
        onShowCompleted();
        if (showListener != null) {
            showListener.fireActionEvent(new ActionEvent(this,ActionEvent.Type.Show));
        }
        if(editOnShow != null) {
            editOnShow.startEditingAsync();
        }
    }

    /**
     * This method shows the form as a modal alert allowing us to produce a behavior
     * of an alert/dialog box. This method will block the calling thread even if the
     * calling thread is the EDT. Notice that this method will not release the block
     * until dispose is called even if show() from another form is called!
     * <p>Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
     * dialogs where you would want the underlying form to "peek" from behind the 
     * form. 
     * 
     * @param top space in pixels between the top of the screen and the form
     * @param bottom space in pixels between the bottom of the screen and the form
     * @param left space in pixels between the left of the screen and the form
     * @param right space in pixels between the right of the screen and the form
     * @param includeTitle whether the title should hang in the top of the screen or
     * be glued onto the content pane
     * @param modal indictes if this is a modal or modeless dialog true for modal dialogs
     */
    void showModal(int top, int bottom, int left, int right, boolean includeTitle, boolean modal, boolean reverse) {
        Display.getInstance().flushEdt();
        if (previousForm == null) {
            previousForm = Display.getInstance().getCurrent();
            // special case for application opening with a dialog before any form is shown
            if (previousForm == null) {
                previousForm = new Form();
                previousForm.show();
            } else {
                if (previousForm instanceof Dialog) {
                    Dialog previousDialog = (Dialog) previousForm;
                    if (previousDialog.isDisposed()) {
                        previousForm = Display.getInstance().getCurrentUpcoming();
                    }
                }
            }
        }

        previousForm.tint = true;
        Painter p = getStyle().getBgPainter();
        if (top > 0 || bottom > 0 || left > 0 || right > 0) {
            if (!title.isVisible()) {
                includeTitle = false;
            }
            Style titleStyle = title.getStyle();
            titleStyle.removeListeners();
            
            Style contentStyle = contentPane.getUnselectedStyle();
            contentStyle.removeListeners();
            
            if (includeTitle) {
                titleStyle.setMargin(Component.TOP, top, false);
                titleStyle.setMargin(Component.BOTTOM, 0, false);
                titleStyle.setMargin(Component.LEFT, left, false);
                titleStyle.setMargin(Component.RIGHT, right, false);

                contentStyle.setMargin(Component.TOP, 0, false);
                contentStyle.setMargin(Component.BOTTOM, bottom, false);
                contentStyle.setMargin(Component.LEFT, left, false);
                contentStyle.setMargin(Component.RIGHT, right, false);
            } else {
                titleStyle.setMargin(Component.TOP, 0, false);
                titleStyle.setMargin(Component.BOTTOM, 0, false);
                titleStyle.setMargin(Component.LEFT, 0, false);
                titleStyle.setMargin(Component.RIGHT, 0, false);

                contentStyle.setMargin(Component.TOP, top, false);
                contentStyle.setMargin(Component.BOTTOM, bottom, false);
                contentStyle.setMargin(Component.LEFT, left, false);
                contentStyle.setMargin(Component.RIGHT, right, false);
            }
            titleStyle.setMarginUnit(null);
            contentStyle.setMarginUnit(null);
            initDialogBgPainter(p, previousForm);
            revalidate();
        } else {
            // If the keyboard was opened the top/bottom/left/right calculations
            // may be zeroes right now, but this will change when the keyboard
            // finishes closing, so we still need to add a BgPainter.
            // Fixes issue described at https://github.com/codenameone/CodenameOne/issues/1751#issuecomment-394707781
            initDialogBgPainter(p, previousForm);
        }

        initFocused();
        if (getTransitionOutAnimator() == null && getTransitionInAnimator() == null) {
            initLaf(getUIManager());
        }

        initComponentImpl();
        Display.getInstance().setCurrent(this, reverse);
        onShow();

        if (modal) {
            // called to display a dialog and wait for modality  
            Display.getInstance().invokeAndBlock(new RunnableWrapper(this, p, reverse));
            // if the virtual keyboard was opend by the dialog close it
            Display.getInstance().setShowVirtualKeyboard(false);
        }
    }

    /**
     * Allows Dialog to override background painting for blur
     * @param p the painter
     */
    void initDialogBgPainter(Painter p, Form previousForm) {
        if (p instanceof BGPainter && ((BGPainter) p).getPreviousForm() != null) {
            ((BGPainter) p).setPreviousForm(previousForm);
        } else {
            BGPainter b = new BGPainter(this, p);
            getStyle().setBgPainter(b);
            b.setPreviousForm(previousForm);
        }
    }

    /**
     * The default version of show modal shows the dialog occupying the center portion
     * of the screen.
     */
    void showModal(boolean reverse) {
        showDialog(true, reverse);
    }

    /**
     * The default version of show dialog shows the dialog occupying the center portion
     * of the screen.
     */
    void showDialog(boolean modal, boolean reverse) {
        int h = Display.getInstance().getDisplayHeight() - menuBar.getPreferredH() - title.getPreferredH();
        int w = Display.getInstance().getDisplayWidth();
        int topSpace = h / 100 * 20;
        int bottomSpace = h / 100 * 10;
        int sideSpace = w / 100 * 20;
        showModal(topSpace, bottomSpace, sideSpace, sideSpace, true, modal, reverse);
    }

    /**
     * Works only for modal forms by returning to the previous form
     */
    void dispose() {
        disposeImpl();
    }

    boolean isDisposed() {
        return false;
    }

    /**
     * Works only for modal forms by returning to the previous form
     */
    void disposeImpl() {
        if (previousForm != null) {
            boolean clearPrevious = Display.getInstance().getCurrent() == this;
            if (!clearPrevious) {
                Form f = Display.getInstance().getCurrent();
                while (f != null) {
                    if (f.previousForm == this) {
                        f.previousForm = previousForm;
                        previousForm = null;
                        return;
                    }
                    f = f.previousForm;
                }
            }
            previousForm.tint = false;

            if (previousForm instanceof Dialog) {
                if (!((Dialog) previousForm).isDisposed()) {
                    Display.getInstance().setCurrent(previousForm, false);
                }
            } else {
                Display.getInstance().setCurrent(previousForm, false);
                //previousForm.revalidate();
            }

            if(clearPrevious) {
                // enable GC to cleanup the previous form if no longer referenced
                previousForm = null;
            }
        }
    }

    boolean isMenu() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    void repaint(Component cmp) {
        if (getParent() != null) {
            super.repaint(cmp);
            return;
        }
        if (isVisible()) {
            Display.getInstance().repaint(cmp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Form getComponentForm() {
        if (getParent() != null) {
            return super.getComponentForm();
        }
        return this;
    }
    
    /**
     * Invoked by display to hide the menu during transition
     * 
     * @see restoreMenu
     */
    void hideMenu() {
        menuBar.unInstallMenuBar();
    }

    /**
     * Invoked by display to restore the menu after transition
     * 
     * @see hideMenu
     */
    void restoreMenu() {
        menuBar.installMenuBar();
    }

    void setFocusedInternal(Component focused) {
        this.focused = focused;
    }

    /**
     * Sets the focused component and fires the appropriate events to make it so
     * 
     * @param focused the newly focused component or null for no focus
     */
    public void setFocused(Component focused) {
        if (this.focused == focused && focused != null) {
            this.focused.repaint();
            return;
        }
        Component oldFocus = this.focused;
        this.focused = focused;
        boolean triggerRevalidate = false;
        if (oldFocus != null) {
            triggerRevalidate = changeFocusState(oldFocus, false);
            //if we need to revalidate no need to repaint the Component, it will
            //be painted from the Form
            if (!triggerRevalidate && oldFocus.getParent() != null) {
                oldFocus.repaint();
            }
        }
        // a listener might trigger a focus change event essentially
        // invalidating focus so we shouldn't break that 
        if (focused != null && this.focused == focused) {
            triggerRevalidate = changeFocusState(focused, true) || triggerRevalidate;
            //if we need to revalidate no need to repaint the Component, it will
            //be painted from the Form
            if (!triggerRevalidate) {
                focused.repaint();
            }
        }
        if (triggerRevalidate) {
            revalidate();
        }
    }

    /**
     * This method changes the cmp state to be focused/unfocused and fires the 
     * focus gained/lost events. 
     * @param cmp the Component to change the focus state
     * @param gained if true this Component needs to gain focus if false
     * it needs to lose focus
     * @return this method returns true if the state change needs to trigger a 
     * revalidate
     */
    private boolean changeFocusState(Component cmp, boolean gained) {
        boolean trigger = false;
        Style selected = cmp.getSelectedStyle();
        Style unselected = cmp.getUnselectedStyle();
        //if selected style is different then unselected style there is a good 
        //chance we need to trigger a revalidate
        if (!selected.getFont().equals(unselected.getFont())
                || selected.getPaddingTop() != unselected.getPaddingTop()
                || selected.getPaddingBottom() != unselected.getPaddingBottom()
                || selected.getPaddingRight(isRTL()) != unselected.getPaddingRight(isRTL())
                || selected.getPaddingLeft(isRTL()) != unselected.getPaddingLeft(isRTL())
                || selected.getMarginTop() != unselected.getMarginTop()
                || selected.getMarginBottom() != unselected.getMarginBottom()
                || selected.getMarginRight(isRTL()) != unselected.getMarginRight(isRTL())
                || selected.getMarginLeft(isRTL()) != unselected.getMarginLeft(isRTL())) {
            trigger = true;
        }
        int prefW = 0;
        int prefH = 0;
        if (trigger) {
            Dimension d = cmp.getPreferredSize();
            prefW = d.getWidth();
            prefH = d.getHeight();
        }

        if (gained) {
            cmp.setFocus(true);
            cmp.fireFocusGained();
            fireFocusGained(cmp);
        } else {
            cmp.setFocus(false);
            cmp.fireFocusLost();
            fireFocusLost(cmp);
        }

        //if the styles are different there is a chance the preffered size is 
        //still the same therefore make sure there is a real need to preform 
        //a revalidate
        if (trigger) {
            cmp.setShouldCalcPreferredSize(true);
            Dimension d = cmp.getPreferredSize();
            if (prefW != d.getWidth() || prefH != d.getHeight()) {
                cmp.setShouldCalcPreferredSize(false);
                trigger = false;
            }
        }

        return trigger;
    }

    /**
     * Returns the current focus component for this form
     * 
     * @return the current focus component for this form
     */
    public Component getFocused() {
        return focused;
    }

    /**
     * {@inheritDoc}
     */
    protected void longKeyPress(int keyCode) {
        if (focused != null) {
            if (focused.getComponentForm() == this) {
                focused.longKeyPress(keyCode);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void longPointerPress(int x, int y) {
        if (focused != null && focused.contains(x, y)) {
            if (focused.getComponentForm() == this) {
                if (focused.hasLead) {
                    Container leadParent;
                    if (focused instanceof Container) {
                        leadParent = ((Container) focused).getLeadParent();
                    } else {
                        leadParent = focused.getParent().getLeadParent();
                    }
                    leadParent.longPointerPress(x, y);
                } else {
                    focused.longPointerPress(x, y);
                }
            }
        }
    }

    /**
     * Indicates whether this form wants to receive pointerReleased events for touch
     * events that started in a different form
     * @return false by default
     */
    protected boolean shouldSendPointerReleaseToOtherForm() {
        return false;
    }
    
    /**
     * Gets the next component in focus traversal order.  This will return the {@link Component#getNextFocusRight() }
     * if it is set.  If not, it will return {@link Component#getNextFocusDown() } if it is set.  If not, it will 
     * return the next component according to the traversal order.
     * @param current The current component.
     * @return The next component in the focus traversal order.
     */
    public Component getNextComponent(Component current) {
        return getTabIterator(current).getNext();
    }
    
    /**
     * Gets the previous component in focus traversal order.  This will return the {@link Component#getNextFocusLeft() }
     * if it is set.  If not, it will return {@link Component#getNextFocusUp() } if it is set.  If not, it will 
     * return the previous component according to the traversal order defined by {@link Form#getTabIterator(com.codename1.ui.Component) }.
     * @param current The current component.
     * @return The previous component in the traversal order.
     */
    public Component getPreviousComponent(Component current) {
        return getTabIterator(current).getPrevious();
    }
    
    /**
     * Iterates through the components on this form in traversal order.  
     * @see #getTabIterator(com.codename1.ui.Component) 
     */
    public class TabIterator implements ListIterator<Component> {
        private java.util.List<Component> components;
        private int currPos;
        private Component current;
        
        private TabIterator(java.util.List<Component> components, Component current) {
            this.components = components;
            setCurrent(current);
        }
        
        /**
         * Gets the current component in this iterator.
         * @return 
         */
        public Component getCurrent() {
            return current;
        }
        
        /**
         * Gets the next component in this iterator.  If the current component explicitly specifies
         * a nextFocusRight or nextFocusDown component, then that component will be returned.
         * Otherwise it will follow the tab index order.
         * @return The next component to be traversed after {@link #getCurrent() }
         */
        public Component getNext() {
            Component current = getCurrent();
            if (current == null && components.isEmpty()) {
                return null;
            }
            
            Component next = current != null ? current.getNextFocusRight() : null;
            if (next != null && next.isFocusable() && next.isVisible() && next.isEnabled()) {
                return next;
            }
            next = current != null ? current.getNextFocusDown() : null;
            if (next != null && next.isFocusable() && next.isVisible() && next.isEnabled()) {
                return next;
            }
            if (currPos < 0 && !components.isEmpty()) {
                return components.get(0);
            }
            if (currPos < components.size()-1) {
                return components.get(currPos+1);
            }
            return null;
        }
        
        /**
         * Gets the previous component that should be traversed when going "back" in through the
         * form components.  If the current component has a nextFocusLeft or nextFocusUp field
         * explicitly specified, then it will return that.  Otherwise it just follows the traversal 
         * order using the tab index.
         * @return The previous component according to traversal order.
         */
        public Component getPrevious() {
            Component current = getCurrent();
            if (current == null && components.isEmpty()) {
                return null;
            }
            Component prev = current != null ? current.getNextFocusLeft() : null;
            if (prev != null && prev.isFocusable() && prev.isVisible() && prev.isEnabled()) {
                return prev;
            }
            prev = current != null ? current.getNextFocusUp() : null;
            if (prev != null && prev.isFocusable() && prev.isVisible() && prev.isEnabled()) {
                return prev;
            }
            if (currPos < 0 && !components.isEmpty()) {
                // Negative current position means that we pick the last 
                // component on the form.
                return components.get(components.size()-1);
            }
            if (currPos > 0 && currPos <= components.size()) {
                return components.get(currPos-1);
            }
            return null;
        }
        
        /**
         * Sets the current component in the iterator.  This reposition the iterator
         * to the given component.
         * @param cmp The component to set as the current component.
         */
        public void setCurrent(Component cmp) {
            current = cmp;
            
            currPos = cmp != null ? components.indexOf(cmp) : -1;
        }
        
        /**
         * Checks to see if there is a "next" component to traverse focus to in this iterator.
         * @return True if there is a "next" component in this iterator.
         */
        public boolean hasNext() {
            return getNext() != null;
        }

        /**
         * Returns the next component in this iterator, and repositions the iterator at this component.
         * 
         * @return The "next" component in the iterator.
         */
        @Override
        public Component next() {
            Component next = getNext();
            setCurrent(next);
            return next;
        }

        /**
         * Checks if this iterator has a "previous" component.
         * @return 
         */
        @Override
        public boolean hasPrevious() {
            return getPrevious() != null;
        }

        /**
         * Returns the previous component in this iterator, and repositions the iterator at this component.
         * @return 
         */
        @Override
        public Component previous() {
            Component prev = getPrevious();
            setCurrent(prev);
            return prev;
        }

        /**
         * Gets the index within the iterator of the next component.
         * @return 
         */
        @Override
        public int nextIndex() {
            Component next = getNext();
            if (next == null) {
                return -1;
            }
            return components.indexOf(next);
        }

        /**
         * Gets the index within the iterator of the previous component.
         * @return 
         */
        @Override
        public int previousIndex() {
            Component prev = getPrevious();
            if (prev == null) {
                return -1;
            }
            return components.indexOf(prev);
        }

        /**
         * Removes the current component from the iterator, and repositions the iterator to the previous 
         * component, or the next component (if previous doesn't exist).
         */
        @Override
        public void remove() {
            Component newCurr = getPrevious();
            if (newCurr == null) {
                newCurr = getNext();
            }
            if (current != null) {
                components.remove(current);
                setCurrent(newCurr);
            }
            
        }

        /**
         * Replaces the current component, in the iterator, with the provided component.
         * This will not actually replace the component in the form's hierarchy.  Just within
         * the iterator.
         * @param e The component to set as the current component.
         */
        @Override
        public void set(Component e) {
            if (currPos >= 0 && currPos < components.size()-1) {
                components.set(currPos, e);
                setCurrent(e);
            }
        }

        /**
         * Adds a component to the end of the iterator.
         * @param e The component to add to the iterator.
         */
        @Override
        public void add(Component e) {
            components.add(e);
        }
        
    }
    
    /**
     * Returns an iterator that iterates over all of the components in this form, ordered
     * by their tab index. 
     * @param start The start position.  The iterator will automatically initialized such that {@link ListIterator#next() }
     * will return the next component in the traversal order, and the {@link ListIterator#previous() } returns the previous
     * component in traversal order.
     * @return An iterator for the traversal order of the components in this form.
     * 
     * @see #getNextComponent(com.codename1.ui.Component) 
     * @see #getPreviousComponent(com.codename1.ui.Component) 
     * @see Component#getPreferredTabIndex() 
     * @see Component#setPreferredTabIndex(int) 
     */
    public TabIterator getTabIterator(Component start) {
        updateTabIndices(0);
        java.util.List<Component> out = new ArrayList<Component>();
        out.addAll(ComponentSelector.select("*", this).filter(new Filter() {

            @Override
            public boolean filter(Component c) {
                return c.getTabIndex() >= 0 && c.isVisible() && c.isFocusable() && c.isEnabled();
            }
            
        }));
        Collections.sort(out, new Comparator<Component>() {

            @Override
            public int compare(Component o1, Component o2) {
                return o1.getTabIndex() < o2.getTabIndex() ? -1 :
                        o2.getTabIndex() < o1.getTabIndex() ? 1 :
                        0;
            }
            
        });
        
        return new TabIterator(out, start);
    }
    
    
    
    
    /**
     * {@inheritDoc}
     */
    public void keyPressed(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        if (menuBar.handlesKeycode(keyCode)) {
            menuBar.keyPressed(keyCode);
            return;
        }

        //Component focused = focusManager.getFocused();
        if (focused != null) {
            if (focused.isEnabled()) {
                focused.keyPressed(keyCode);
            }
            if (focused == null) {
                initFocused();
                return;
            }
            if (focused.handlesInput()) {
                return;
            }
            if (focused.getComponentForm() == this) {
                //if the arrow keys have been pressed update the focus.
                updateFocus(game);
            } else {
                initFocused();
            }
        } else {
            initFocused();
            if (focused == null) {
                getContentPane().moveScrollTowards(game, null);
                return;
            }
        }

    }

    /**
     * Returns the layout manager of the form's content pane.
     * @see #getActualLayout() For the actual layout of the form.
     */
    public Layout getLayout() {
        return contentPane.getLayout();
    }

    /**
     * When set to true the physical back button will minimize the application
     * @return the minimizeOnBack
     */
    public boolean isMinimizeOnBack() {
        return menuBar.isMinimizeOnBack();
    }

    /**
     * When set to true the physical back button will minimize the application
     * @param minimizeOnBack the minimizeOnBack to set
     */
    public void setMinimizeOnBack(boolean minimizeOnBack) {
        menuBar.setMinimizeOnBack(minimizeOnBack);
    }

    /**
     * {@inheritDoc}
     */
    public void keyReleased(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        if (menuBar.handlesKeycode(keyCode)) {
            menuBar.keyReleased(keyCode);
            return;
        }

        //Component focused = focusManager.getFocused();
        if (focused != null) {
            if (focused.getComponentForm() == this) {
                if (focused.isEnabled()) {
                    focused.keyReleased(keyCode);
                }
            }
        }

        // prevent the default action from stealing the behavior from the popup/combo box...
        if (game == Display.GAME_FIRE) {
            Command defaultCmd = getDefaultCommand();
            if (defaultCmd != null) {
                defaultCmd.actionPerformed(new ActionEvent(defaultCmd, keyCode));
                actionCommandImpl(defaultCmd);
            }
        }
        fireKeyEvent(keyListeners, keyCode);
        fireKeyEvent(gameKeyListeners, game);
    }

    private void fireKeyEvent(HashMap<Integer, ArrayList<ActionListener>> keyListeners, int keyCode) {
        if (keyListeners != null) {
            ArrayList<ActionListener> listeners = keyListeners.get(new Integer(keyCode));
            if (listeners != null) {
                ActionEvent evt = new ActionEvent(this, keyCode);
                for (int iter = 0; iter < listeners.size(); iter++) {
                    listeners.get(iter).actionPerformed(evt);
                    if (evt.isConsumed()) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void keyRepeated(int keyCode) {
        if (focused != null) {
            if (focused.isEnabled()) {
                focused.keyRepeated(keyCode);
            }
            int game = Display.getInstance().getGameAction(keyCode);
            // this has issues in the WTK
            // Fix for issue 433: the focus might be changed by the key repeated method in a way that can turn it to null
            if (focused != null && !focused.handlesInput()
                    && (game == Display.GAME_DOWN || game == Display.GAME_UP || game == Display.GAME_LEFT || game == Display.GAME_RIGHT)) {
                keyPressed(keyCode);
                keyReleased(keyCode);
            }
        } else {
            keyPressed(keyCode);
            keyReleased(keyCode);
        }
    }
    
    private void initRippleEffect(int x, int y, Component cmp) {
        if(cmp.isRippleEffect()) {
            rippleMotion = Motion.createEaseInMotion(0, 1000, 800);
            rippleMotion.start();
            rippleComponent = cmp;
            rippleX = x;
            rippleY = y;
        }
    }

    private void tactileTouchVibe(int x, int y, Component cmp) {
        if (tactileTouchDuration > 0 && cmp.isTactileTouch(x, y)) {
            Display.getInstance().vibrate(tactileTouchDuration);
        }
    }

    private Component pressedCmp;
    
    /**
     * {@inheritDoc}
     */
    public void pointerPressed(int x, int y) {
        pressedCmp = null;
        stickyDrag = null;
        dragStopFlag = false;
        dragged = null;
        boolean isScrollWheeling = Display.INSTANCE.impl.isScrollWheeling();
        if (pointerPressedListeners != null && pointerPressedListeners.hasListeners()) {
            pointerPressedListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed, x, y));
        }
        //check if the click is relevant to the menu bar.
        /*
        if (menuBar.contains(x, y)) {
            Component cmp = menuBar.getComponentAt(x, y);
            while (cmp != null && cmp.isIgnorePointerEvents()) {
                cmp = cmp.getParent();
            }
            if (cmp != null && cmp.isEnabled()) {
                cmp.pointerPressed(x, y);
                tactileTouchVibe(x, y, cmp);
                initRippleEffect(x, y, cmp);
            }
            return;
        }
        */
        Container actual = getActualPane(formLayeredPane, x, y);
        if (y >= actual.getY() && x >= actual.getX()) {
            Component cmp = actual.getComponentAt(x, y);
            while (cmp != null && cmp.isIgnorePointerEvents()) {
                cmp = cmp.getParent();
            }
            if (cmp != null) {
                cmp.initDragAndDrop(x, y);
                if (cmp.hasLead) {

                    if (isCurrentlyScrolling(cmp)) {
                        dragStopFlag = true;
                        cmp.clearDrag();
                        return;
                    }

                    Container leadParent;
                    if (cmp instanceof Container) {
                        leadParent = ((Container) cmp).getLeadParent();
                    } else {
                        leadParent = cmp.getParent().getLeadParent();
                    }
                    leadParent.repaint();
                    if (!isScrollWheeling) {
                        setFocused(leadParent);
                    }
                    pressedCmp = cmp.getLeadComponent();
                    cmp.getLeadComponent().pointerPressed(x, y);
                } else {
                    
                    if (isCurrentlyScrolling(cmp)) {
                        dragStopFlag = true;
                        cmp.clearDrag();
                        return;
                    }
                    
                    if (cmp.isEnabled()) {
                        if (!isScrollWheeling && cmp.isFocusable()) {
                            setFocused(cmp);
                        }
                        pressedCmp = cmp;
                        cmp.pointerPressed(x, y);
                        tactileTouchVibe(x, y, cmp);
                        initRippleEffect(x, y, cmp);
                    }
                }
            }
        } else {
            if(y < actual.getY()) {
                Component cmp = getTitleArea().getComponentAt(x, y);
                while (cmp != null && cmp.isIgnorePointerEvents()) {
                    cmp = cmp.getParent();
                }
                if (cmp != null && cmp.isEnabled() && cmp.isFocusable()) {
                    pressedCmp = cmp;
                    cmp.pointerPressed(x, y);
                    tactileTouchVibe(x, y, cmp);
                    initRippleEffect(x, y, cmp);
                }   
            } else {
                Component cmp = ((BorderLayout)super.getLayout()).getWest();
                if(cmp != null) {
                    cmp = ((Container)cmp).getComponentAt(x, y);
                    while (cmp != null && cmp.isIgnorePointerEvents()) {
                        cmp = cmp.getParent();
                    }
                    if (cmp != null && cmp.isEnabled() && cmp.isFocusable()) {
                        if(cmp.hasLead) {
                            Container leadParent;
                            if (cmp instanceof Container) {
                                leadParent = ((Container) cmp).getLeadParent();
                            } else {
                                leadParent = cmp.getParent().getLeadParent();
                            }
                            if (!isScrollWheeling) {
                                setFocused(leadParent);
                            }
                            cmp = cmp.getLeadComponent();
                        }
                        cmp.initDragAndDrop(x, y);
                        pressedCmp = cmp;
                        cmp.pointerPressed(x, y);
                        tactileTouchVibe(x, y, cmp);
                        initRippleEffect(x, y, cmp);
                    }   
                }
            }
        }
        initialPressX = x;
        initialPressY = y;
    }
    
    private boolean isCurrentlyScrolling(Component cmp) {
        Container parent = cmp.getParent();
        //loop over the parents to check if there is a scrolling 
        //gesture that should be stopped
        while (parent != null) {
            if (parent.draggedMotionX != null || parent.draggedMotionY != null) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void autoRelease(int x, int y) {
        if(buttonsAwatingRelease != null && buttonsAwatingRelease.size() == 1) {
            // special case allowing drag within a button
            Component atXY = getComponentAt(x, y);
            if (atXY instanceof Container) {
                atXY = atXY.getLeadComponent();
            }
            Component pendingButton = buttonsAwatingRelease.get(0);
            if (atXY != pendingButton) {
                if (pendingButton instanceof Button) {
                    Button b = (Button) pendingButton;
                    int relRadius = b.getReleaseRadius();
                    if (relRadius > 0) {
                        Rectangle r = new Rectangle(b.getAbsoluteX() - relRadius, b.getAbsoluteY() - relRadius, b.getWidth() + relRadius * 2, b.getHeight() + relRadius * 2);
                        if (!r.contains(x, y)) {
                            buttonsAwatingRelease = null;
                            b.dragInitiated();
                        }
                        return;
                    }
                    buttonsAwatingRelease = null;
                    b.dragInitiated();
                }
            } else if (pendingButton instanceof Button && ((Button) pendingButton).isAutoRelease()) {
                buttonsAwatingRelease = null;
                ((Button) pendingButton).dragInitiated();
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void pointerDragged(int x, int y) {
        // disable the drag stop flag if we are dragging again
        boolean isScrollWheeling = Display.INSTANCE.impl.isScrollWheeling();
        if(dragStopFlag) {
            pointerPressed(x, y);
        }
        autoRelease(x, y);
        if (pointerDraggedListeners != null) {
            ActionEvent av = new ActionEvent(this, ActionEvent.Type.PointerDrag, x, y);
            pointerDraggedListeners.fireActionEvent(av);
            if(av.isConsumed()) {
                return;
            }
        }

        rippleMotion = null;
        
        if (dragged != null) {
            dragged.pointerDragged(x, y);
            return;
        }

        if (pressedCmp != null && pressedCmp.isStickyDrag()) {
            stickyDrag = pressedCmp;
        }
        
        if(stickyDrag != null) {
            stickyDrag.pointerDragged(x, y);
            repaint();
            return;
        }
        Container actual = getActualPane(formLayeredPane, x, y);
        if(x < actual.getX()) {
            // special case for sidemenu
            Component cmp = ((BorderLayout)super.getLayout()).getWest();
            if(cmp != null) {
                cmp = ((Container)cmp).getComponentAt(x, y);
                while (cmp != null && cmp.isIgnorePointerEvents()) {
                    cmp = cmp.getParent();
                }
                if (cmp != null && cmp.isEnabled()) {
                    cmp.pointerDragged(x, y);
                    cmp.repaint();
                    if(cmp == pressedCmp && cmp.isStickyDrag()) {
                        stickyDrag = cmp;
                    }
                }
            }
            return;
        }
        Component cmp = actual.getComponentAt(x, y);
        while (cmp != null && cmp.isIgnorePointerEvents()) {
            cmp = cmp.getParent();
        }
        if (cmp != null) {
            if (!isScrollWheeling && cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerDragged(x, y);
            cmp.repaint();
            if(cmp == pressedCmp && cmp.isStickyDrag()) {
                stickyDrag = cmp;
            }
        }
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        // disable the drag stop flag if we are dragging again
        boolean isScrollWheeling = Display.INSTANCE.impl.isScrollWheeling();
        if(dragStopFlag) {
            pointerPressed(x, y);
        }
        autoRelease(x[0], y[0]);
        if (pointerDraggedListeners != null && pointerDraggedListeners.hasListeners()) {
            ActionEvent av = new ActionEvent(this, ActionEvent.Type.PointerDrag,x[0], y[0]);
            pointerDraggedListeners.fireActionEvent(av);
            if(av.isConsumed()) {
                return;
            }
        }

        rippleMotion = null;

        if (dragged != null) {
            dragged.pointerDragged(x, y);
            return;
        }
        if (pressedCmp != null && pressedCmp.isStickyDrag()) {
            stickyDrag = pressedCmp;
        }
        if(stickyDrag != null) {
            stickyDrag.pointerDragged(x, y);
            repaint();
            return;
        }
        Container actual = getActualPane(formLayeredPane, x[0], y[0]);
        if(x[0] < actual.getX()) {
            // special case for sidemenu
            Component cmp = ((BorderLayout)super.getLayout()).getWest();
            if(cmp != null) {
                cmp = ((Container)cmp).getComponentAt(x[0], y[0]);
                while (cmp != null && cmp.isIgnorePointerEvents()) {
                    cmp = cmp.getParent();
                }
                if (cmp != null && cmp.isEnabled()) {
                    cmp.pointerDragged(x, y);
                    cmp.repaint();
                    if(cmp == pressedCmp && cmp.isStickyDrag()) {
                        stickyDrag = cmp;
                    }
                }
            }
            return;
        }
        Component cmp = actual.getComponentAt(x[0], y[0]);
        while (cmp != null && cmp.isIgnorePointerEvents()) {
            cmp = cmp.getParent();
        }
        if (cmp != null) {
            if (!isScrollWheeling && cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerDragged(x, y);
            cmp.repaint();
            if(cmp == pressedCmp && cmp.isStickyDrag()) {
                stickyDrag = cmp;
            }
        }
    }
    
    

    /**
     * {@inheritDoc}
     */
    public void pointerHoverReleased(int[] x, int[] y) {

        if (dragged != null) {
            dragged.pointerHoverReleased(x, y);
            dragged = null;
            return;
        }

        Container actual = getActualPane(formLayeredPane, x[0], y[0]);
        Component cmp = actual.getComponentAt(x[0], y[0]);
        while (cmp != null && cmp.isIgnorePointerEvents()) {
            cmp = cmp.getParent();
        }
        if (cmp != null) {
            cmp.pointerHoverReleased(x, y);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHoverPressed(int[] x, int[] y) {
        boolean isScrollWheeling = Display.INSTANCE.impl.isScrollWheeling();
        
        Container actual = getActualPane(formLayeredPane, x[0], y[0]);
        Component cmp = actual.getComponentAt(x[0], y[0]);
        while (cmp != null && cmp.isIgnorePointerEvents()) {
            cmp = cmp.getParent();
        }
        if (cmp != null) {
            if (!isScrollWheeling && cmp.isFocusable() && cmp.isEnabled() && !Display.getInstance().isDesktop()) {
                setFocused(cmp);
            }
            cmp.pointerHoverPressed(x, y);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHover(int[] x, int[] y) {
        boolean isScrollWheeling = Display.INSTANCE.impl.isScrollWheeling();
        if (dragged != null) {
            dragged.pointerHover(x, y);
            return;
        }

        Container actual = getActualPane(formLayeredPane, x[0], y[0]);
        if(actual != null) {
            Component cmp = actual.getComponentAt(x[0], y[0]);
            while (cmp != null && cmp.isIgnorePointerEvents()) {
                cmp = cmp.getParent();
            }
            if (cmp != null) {
                if (!isScrollWheeling && cmp.isFocusable() && cmp.isEnabled() && !Display.getInstance().isDesktop()) {
                    setFocused(cmp);
                }
                cmp.pointerHover(x, y);
            }
        }
    }

    /**
     * Returns true if there is only one focusable member in this form. This is useful
     * so setHandlesInput would always be true for this case.
     * 
     * @return true if there is one focusable component in this form, false for 0 or more
     */
    public boolean isSingleFocusMode() {
        if (formLayeredPane != null) {
            return countFocusables(formLayeredPane) + countFocusables(getActualPane()) < 2;
        }
        return isSingleFocusMode(0, getActualPane()) == 1;
    }

    private int countFocusables(Container c) {
        int count=0;
        int t = c.getComponentCount();
        for (int iter = 0; iter < t; iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp.isFocusable()) {
                count++;
            }
            if (cmp instanceof Container) {
                count += countFocusables((Container)cmp);
            }
        }
        return count;
    }
    
    private int isSingleFocusMode(int b, Container c) {
        int t = c.getComponentCount();
        for (int iter = 0; iter < t; iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp.isFocusable()) {
                if (b > 0) {
                    return 2;
                }
                b = 1;
            }
            if (cmp instanceof Container) {
                b = isSingleFocusMode(b, (Container) cmp);
                if (b > 1) {
                    return b;
                }
            }
        }
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        rippleMotion = null;
        pressedCmp = null;
        boolean isScrollWheeling = Display.INSTANCE.impl.isScrollWheeling();
        Container actual = getActualPane(formLayeredPane, x, y);
        if(buttonsAwatingRelease != null && buttonsAwatingRelease.size() == 1) {
            // special case allowing drag within a button
            Component atXY = actual.getComponentAt(x, y);
            
            Component pendingButton = (Component)buttonsAwatingRelease.get(0);
            if(atXY == pendingButton) {
                buttonsAwatingRelease = null;
                if (dragged == pendingButton) {
                    if (pendingButton.isDragAndDropInitialized()) {
                        pendingButton.dragFinishedImpl(x, y);
                    } else {
                        pendingButton.pointerReleased(x, y);
                    }
                    dragged = null;
                } else {
                    pendingButton.pointerReleased(x, y);
                    if (dragged != null) {
                        if (dragged.isDragAndDropInitialized()) {
                            dragged.dragFinishedImpl(x, y);
                            dragged = null;
                        } else {
                            dragged.pointerReleased(x, y);
                            dragged = null;
                        }
                    }
                }
                return;
            }
            
            if(pendingButton instanceof Button) {
                Button b = (Button)pendingButton;
                int relRadius = b.getReleaseRadius();
                if(relRadius > 0 || b.contains(x, y)) {
                    Rectangle r = new Rectangle(b.getAbsoluteX() - relRadius, b.getAbsoluteY() - relRadius, b.getWidth() + relRadius * 2, b.getHeight() + relRadius * 2);
                    if(r.contains(x, y)) {
                        buttonsAwatingRelease = null;
                        pointerReleased(b.getAbsoluteX() + 1, b.getAbsoluteY() + 1);
                        return;
                    }
                }
            }
        }
        if (pointerReleasedListeners != null && pointerReleasedListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(this, ActionEvent.Type.PointerReleased, x, y);
            pointerReleasedListeners.fireActionEvent(ev);
            if(ev.isConsumed()) {
                if (dragged != null) {
                    if (dragged.isDragAndDropInitialized()) {
                        dragged.dragFinishedImpl(x, y);
                        
                    }
                    dragged = null;
                }
                return;
            }
        }
        if(dragStopFlag) {
            if (dragged != null) {
                if (dragged.isDragAndDropInitialized()) {
                    dragged.dragFinishedImpl(x, y);
                    
                }
                dragged = null;
            }
            dragStopFlag = false;
            
            return;
        }
        
        if (dragged == null) {
            //if the pointer was released on the menu invoke the appropriate
            //soft button.
            if (menuBar.contains(x, y)) {
                Component cmp = menuBar.getComponentAt(x, y);
                if (cmp != null && cmp.isEnabled()) {
                    cmp.pointerReleased(x, y);
                }
                return;
            }

            if(stickyDrag != null) {
                stickyDrag.pointerReleased(x, y);
                repaint();
            } else {
                //Container actual = getActualPane();
                if (y >= actual.getY() && x >= actual.getX()) {
                    Component cmp = actual.getComponentAt(x, y);
                    while (cmp != null && cmp.isIgnorePointerEvents()) {
                        cmp = cmp.getParent();
                    }
                    if (cmp != null && cmp.isEnabled()) {
                        if (cmp.hasLead) {
                            Container leadParent;
                            if (cmp instanceof Container) {
                                leadParent = ((Container) cmp).getLeadParent();
                            } else {
                                leadParent = cmp.getParent().getLeadParent();
                            }
                            leadParent.repaint();
                            if (!isScrollWheeling) {
                                setFocused(leadParent);
                            }
                            cmp.getLeadComponent().pointerReleased(x, y);
                        } else {
                            if (cmp.isEnabled()) {
                                if (!isScrollWheeling && cmp.isFocusable()) {
                                    setFocused(cmp);
                                }
                                cmp.pointerReleased(x, y);
                            }
                        }
                    }
                } else {
                    if(y < actual.getY()) {
                        Component cmp = getTitleArea().getComponentAt(x, y);
                        while (cmp != null && cmp.isIgnorePointerEvents()) {
                            cmp = cmp.getParent();
                        }
                        if (cmp != null && cmp.isEnabled()) {
                            cmp.pointerReleased(x, y);
                        }
                    } else {
                        Component cmp = ((BorderLayout)super.getLayout()).getWest();
                        if(cmp != null) {
                            cmp = ((Container)cmp).getComponentAt(x, y);
                            while (cmp != null && cmp.isIgnorePointerEvents()) {
                                cmp = cmp.getParent();
                            }
                            if (cmp != null && cmp.isEnabled()) {                                
                                if(cmp.hasLead) {
                                    Container leadParent;
                                    if (cmp instanceof Container) {
                                        leadParent = ((Container) cmp).getLeadParent();
                                    } else {
                                        leadParent = cmp.getParent().getLeadParent();
                                    }
                                    leadParent.repaint();
                                    if (!isScrollWheeling) {
                                        setFocused(leadParent);
                                    }
                                    cmp = cmp.getLeadComponent();
                                    cmp.pointerReleased(x, y);
                                } else {
                                    cmp.pointerReleased(x, y);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (dragged.isDragAndDropInitialized()) {
                dragged.dragFinishedImpl(x, y);
                dragged = null;
            } else {
                dragged.pointerReleased(x, y);
                dragged = null;
            }
        }
        stickyDrag = null;
        if (buttonsAwatingRelease != null && !Display.getInstance().isRecursivePointerRelease()) {
            for (int iter = 0; iter < buttonsAwatingRelease.size(); iter++) {
                Button b = (Button) buttonsAwatingRelease.get(iter);
                b.setState(Button.STATE_DEFAULT);
                b.repaint();
            }
            buttonsAwatingRelease = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setScrollableY(boolean scrollableY) {
        getContentPane().setScrollableY(scrollableY);
    }

    /**
     * {@inheritDoc}
     */
    public void setScrollableX(boolean scrollableX) {
        getContentPane().setScrollableX(scrollableX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setScrollVisible(boolean isScrollVisible) {
        getContentPane().setScrollVisible(isScrollVisible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScrollVisible() {
        return getContentPane().isScrollVisible();
    }
    

    /**
     * {@inheritDoc}
     */
    public int getComponentIndex(Component cmp) {
        return getContentPane().getComponentIndex(cmp);
    }

    /**
     * Adds a command to the menu bar softkeys or into the menu dialog, 
     * this version of add allows us to place a command in an arbitrary location.
     * This allows us to force a command into the softkeys when order of command
     * addition can't be changed.
     * 
     * @param cmd the Form command to be added
     * @param offset position in which the command is added
     */
    public void addCommand(Command cmd, int offset) {
        menuBar.addCommand(cmd, offset);
    }

    /**
     * A helper method to check the amount of commands within the form menu
     * 
     * @return the number of commands
     */
    public int getCommandCount() {
        return menuBar.getCommandCount();
    }

    /**
     * Returns the command occupying the given index
     * 
     * @param index offset of the command
     * @return the command at the given index
     */
    public Command getCommand(int index) {
        return menuBar.getCommand(index);
    }

    /**
     * Adds a command to the menu bar softkeys.
     * The Commands are placed in the order they are added.
     * If the Form has 1 Command it will be placed on the right.
     * If the Form has 2 Commands the first one that was added will be placed on
     * the right and the second one will be placed on the left.
     * If the Form has more then 2 Commands the first one will stay on the left
     * and a Menu will be added with all the remain Commands.
     * 
     * @param cmd the Form command to be added
     */
    public void addCommand(Command cmd) {
        //menuBar.addCommand(cmd);
        addCommand(cmd, 0);
    }

    /**
     * Removes the command from the menu bar softkeys
     * 
     * @param cmd the Form command to be removed
     */
    public void removeCommand(Command cmd) {
        menuBar.removeCommand(cmd);
    }

    /**
     * Indicates whether focus should cycle within the form
     * 
     * @param cyclicFocus marks whether focus should cycle
     */
    public void setCyclicFocus(boolean cyclicFocus) {
        this.cyclicFocus = cyclicFocus;
    }

    private Component findNextFocusHorizontal(Component focused, Component bestCandidate, Container root, boolean right) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component current = root.getComponentAt(iter);
            if (current.isFocusable()) {
                if (isInSameRow(focused, current)) {
                    int currentX = current.getAbsoluteX();
                    int focusedX = focused.getAbsoluteX();
                    if (right) {
                        if (focusedX < currentX) {
                            if (bestCandidate != null) {
                                if (bestCandidate.getAbsoluteX() < currentX) {
                                    continue;
                                }
                            }
                            bestCandidate = current;
                        }
                    } else {
                        if (focusedX > currentX) {
                            if (bestCandidate != null) {
                                if (bestCandidate.getAbsoluteX() > currentX) {
                                    continue;
                                }
                            }
                            bestCandidate = current;
                        }
                    }
                }
            }
            if (current instanceof Container && !(((Container) current).isBlockFocus())) {
                bestCandidate = findNextFocusHorizontal(focused, bestCandidate, (Container) current, right);
            }
        }
        return bestCandidate;
    }

    private Component findNextFocusVertical(Component focused, Component bestCandidate, Container root, boolean down) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component current = root.getComponentAt(iter);
            if (current.isFocusable()) {
                int currentY = current.getAbsoluteY();
                int focusedY = 0;
                if(focused != null) {
                    focusedY = focused.getAbsoluteY();
                }
                if (down) {
                    if (focusedY < currentY) {
                        if (bestCandidate != null) {
                            boolean exitingInSame = isInSameColumn(focused, bestCandidate);
                            if (bestCandidate.getAbsoluteY() < currentY) {
                                if (exitingInSame) {
                                    continue;
                                }
                                if (isInSameRow(current, bestCandidate) && !isInSameColumn(focused, current)) {
                                    continue;
                                }
                            }
                            if (exitingInSame && isInSameRow(current, bestCandidate)) {
                                continue;
                            }
                        }
                        bestCandidate = current;
                    }
                } else {
                    if (focusedY > currentY) {
                        if (bestCandidate != null) {
                            boolean exitingInSame = isInSameColumn(focused, bestCandidate);
                            if (bestCandidate.getAbsoluteY() > currentY) {
                                if (exitingInSame) {
                                    continue;
                                }
                                if (isInSameRow(current, bestCandidate) && !isInSameColumn(focused, current)) {
                                    continue;
                                }
                            }
                            if (exitingInSame && isInSameRow(current, bestCandidate)) {
                                continue;
                            }
                        }
                        bestCandidate = current;
                    }
                }
            }
            if (current instanceof Container && !(((Container) current).isBlockFocus())) {
                bestCandidate = findNextFocusVertical(focused, bestCandidate, (Container) current, down);
            }
        }
        return bestCandidate;
    }
    
    /**
     * This method returns the next focusable Component vertically
     * 
     * <p>NOTE:  This method does NOT make use of {@link Component#getNextFocusDown() } or {@link Component#getNextFocusUp() }. 
     * It simply finds the next focusable component on the form based solely on absolute Y coordinate.</p>
     * 
     * @param down if true will the return the next focusable on the bottom else
     * on the top
     * @return a focusable Component or null if not found
     */
    public Component findNextFocusVertical(boolean down) {
        Component c = null;
        if (formLayeredPane != null) {
            c = findNextFocusVertical(focused, null, formLayeredPane, down);
            if (c != null) {
                return c;
            }
        }
        Container actual = getActualPane();
        c = findNextFocusVertical(focused, null, actual, down);
        if (c != null) {
            return c;
        }
        if (cyclicFocus) {
            c = findNextFocusVertical(focused, null, actual, !down);
            if (c != null) {
                Component current = findNextFocusVertical(c, null, actual, !down);
                while (current != null) {
                    c = current;
                    current = findNextFocusVertical(c, null, actual, !down);
                }
                return c;
            }
        }
        return null;
    }
    
    
    /**
     * This method returns the next focusable Component horizontally
     * 
     * <p>NOTE:  This method does NOT make use of {@link Component#getNextFocusLeft() } or {@link Component#getNextFocusRight() }. 
     * It simply finds the next focusable component on the form based solely on absolute X coordinate.</p>
     * 
     * @param right if true will the return the next focusable on the right else
     * on the left
     * @return a focusable Component or null if not found
     */
    public Component findNextFocusHorizontal(boolean right) {
        Component c = null;
        if (formLayeredPane != null) {
            c = findNextFocusHorizontal(focused, null, formLayeredPane, right);
            if (c != null) {
                return c;
            }
        }
        Container actual = getActualPane();
        c = findNextFocusHorizontal(focused, null, actual, right);
        if (c != null) {
            return c;
        }
        if (cyclicFocus) {
            c = findNextFocusHorizontal(focused, null, actual, !right);
            if (c != null) {
                Component current = findNextFocusHorizontal(c, null, actual, !right);
                while (current != null) {
                    c = current;
                    current = findNextFocusHorizontal(c, null, actual, !right);
                }
                return c;
            }
        }
        return null;
    }
    
    /**
     * Finds next focusable component.  This will first check {@link Component#getNextFocusDown() }
     * on the currently focused component.  Failing that it will scan the form based on Y-coord.
     * @return 
     */
    Component findNextFocusDown() {
        if (focused != null) {
            if (focused.getNextFocusDown() != null) {
                return focused.getNextFocusDown();
            }
            return findNextFocusVertical(true);
        }
        return null;
    }

    /**
     * Finds next focusable component in upward direction.  This will first check {@link Component#getNextFocusUp() }
     * on the currently focused component.  Failing that it will scan the form based on Y-coord.
     * @return 
     */
    Component findNextFocusUp() {
        if (focused != null) {
            if (focused.getNextFocusUp() != null) {
                return focused.getNextFocusUp();
            }
            return findNextFocusVertical(false);
        }
        return null;
    }

    /**
     * Finds next focusable component in rightward direction.  This will first check {@link Component#getNextFocusRight() }
     * on the currently focused component.  Failing that it will scan the form based on X-coord.
     * @return 
     */
    Component findNextFocusRight() {
        if (focused != null) {
            if (focused.getNextFocusRight() != null) {
                return focused.getNextFocusRight();
            }
            return findNextFocusHorizontal(true);
        }
        return null;
    }

    /**
     * Finds next focusable component in leftward direction.  This will first check {@link Component#getNextFocusLeft() }
     * on the currently focused component.  Failing that it will scan the form based on X-coord.
     * @return 
     */
    Component findNextFocusLeft() {
        if (focused != null) {
            if (focused.getNextFocusLeft() != null) {
                return focused.getNextFocusLeft();
            }
            return findNextFocusHorizontal(false);
        }
        return null;
    }

    /**
     * Indicates whether focus should cycle within the form
     * 
     * @return true if focus should cycle
     */
    public boolean isCyclicFocus() {
        return cyclicFocus;
    }

    private void updateFocus(int gameAction) {
        Component focused = getFocused();
        switch (gameAction) {
            case Display.GAME_DOWN: {
                Component down = findNextFocusDown();
                if (down != null) {
                    focused = down;
                }
                break;
            }
            case Display.GAME_UP: {
                Component up = findNextFocusUp();
                if (up != null) {
                    focused = up;
                }
                break;
            }
            case Display.GAME_RIGHT: {
                Component right = findNextFocusRight();
                if (right != null) {
                    focused = right;
                }
                break;
            }
            case Display.GAME_LEFT: {
                Component left = findNextFocusLeft();
                if (left != null) {
                    focused = left;
                }
                break;
            }
            default:
                return;
        }

        //if focused is now visible we need to give it the focus.
        if (isFocusScrolling()) {
            setFocused(focused);
            if (focused != null) {
                scrollComponentToVisible(focused);
            }
        } else {
            if (moveScrollTowards(gameAction, focused)) {
                setFocused(focused);
                scrollComponentToVisible(focused);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    boolean moveScrollTowards(int direction, Component c) {
        //if the current focus item is in a scrollable Container
        //try and move it first
        Component current = getFocused();
        if (current != null) {
            Container parent;
            if (current instanceof Container) {
                parent = (Container) current;
            } else {
                parent = current.getParent();
            }
            while (parent != null) {
                if(parent == this) {
                    if(getContentPane().isScrollable()) {
                        getContentPane().moveScrollTowards(direction, c);
                    }
                    
                }else{
                    if (parent.isScrollable()) {
                        return parent.moveScrollTowards(direction, c);
                    }
                }
                parent = parent.getParent();
            }
        }

        return true;
    }
    
    /**
     * Initiates a quick drag event on all containers of this form that have a negative scroll position.
     * Sometimes, after editing, or on a screen-size change, scroll positions can get caught in a 
     * negative position, and need to be reset.  This is primarily to solve https://github.com/codenameone/CodenameOne/issues/2476
     */
    void fixNegativeScrolls() {
        java.util.Set<Component> negativeScrolls = getContentPane().findNegativeScrolls(new java.util.HashSet<Component>());
        System.out.println("NegativeScrolls: "+negativeScrolls);
        for (Component cmp : negativeScrolls) {
            int x = cmp.getAbsoluteX()+cmp.getWidth()/2;
            int y = cmp.getAbsoluteY()+cmp.getHeight()/2;
            cmp.pointerPressed(x, y);
            cmp.pointerDragged(x, y);
            cmp.pointerReleased(x, y);
        }
    }
    
    /**
     * Makes sure the component is visible in the scroll if this container 
     * is scrollable
     * 
     * @param c the componant to be visible
     */
    public void scrollComponentToVisible(Component c) {
        initFocused();
        Container parent = c.getParent();
        while (parent != null) {
            if (parent.isScrollable()) {
                if(parent == this) {
                    // special case for Form
                    if(getContentPane().isScrollable()) {
                        getContentPane().scrollComponentToVisible(c);
                    }
                } else {
                    parent.scrollComponentToVisible(c);
                }
                return;
            }
            parent = parent.getParent();
        }
    }

    /**
     * Determine the cell renderer used to render menu elements for themeing the 
     * look of the menu options
     * 
     * @param menuCellRenderer the menu cell renderer
     */
    public void setMenuCellRenderer(ListCellRenderer menuCellRenderer) {
        menuBar.setMenuCellRenderer(menuCellRenderer);
    }

    /**
     * Clear menu commands from the menu bar
     */
    public void removeAllCommands() {
        menuBar.removeAllCommands();
    }

    /**
     * Request focus for a form child component
     * 
     * @param cmp the form child component
     */
    void requestFocus(Component cmp) {
        if (cmp.isFocusable() && contains(cmp)) {
            scrollComponentToVisible(cmp);
            setFocused(cmp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRTL(boolean r) {
        super.setRTL(r);
        contentPane.setRTL(r);
    }

    private boolean inInternalPaint;
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        if(!inInternalPaint) {
            paintComponentBackground(g);
        }
        super.paint(g);
        if (tint) {
            g.setColor(tintColor);
            g.fillRect(0, 0, getWidth(), getHeight(), (byte) ((tintColor >> 24) & 0xff));
        }
    }
    
    void internalPaintImpl(Graphics g, boolean paintIntersects) {
        // workaround for form drawing its background twice on standard paint
        inInternalPaint = true;
        super.internalPaintImpl(g, paintIntersects);
        inInternalPaint = false;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setScrollable(boolean scrollable) {
        getContentPane().setScrollable(scrollable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isScrollable() {
        return getContentPane().isScrollable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScrollableX() {
        return getContentPane().isScrollableX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScrollableY() {
        return getContentPane().isScrollableY();
    }

    /**
     * {@inheritDoc}
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (mediaComponents != null) {
            int size = mediaComponents.size();
            for (int i = 0; i < size; i++) {
                Component mediaCmp = (Component) mediaComponents.get(i);
                mediaCmp.setVisible(visible);
            }
        }
    }

    /**
     * Default color for the screen tint when a dialog or a menu is shown
     * 
     * @return the tint color when a dialog or a menu is shown
     */
    public int getTintColor() {
        return tintColor;
    }

    /**
     * Default color for the screen tint when a dialog or a menu is shown
     * 
     * @param tintColor the tint color when a dialog or a menu is shown
     */
    public void setTintColor(int tintColor) {
        this.tintColor = tintColor;
    }

    /**
     * Sets the menu transitions for showing/hiding the menu, can be null...
     * 
     * @param transitionIn the transition that will play when the menu appears
     * @param transitionOut the transition that will play when the menu is folded
     */
    public void setMenuTransitions(Transition transitionIn, Transition transitionOut) {
        menuBar.setTransitions(transitionIn, transitionOut);
    }

    /**
     * {@inheritDoc}
     */
    protected String paramString() {
        return super.paramString() + ", title = " + title
                + ", visible = " + isVisible();
    }

    /**
     * Returns the associated Menu Bar object
     * 
     * @return the associated Menu Bar object
     */
    public MenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Sets the associated MenuBar Object.
     * 
     * @param menuBar
     */
    public void setMenuBar(MenuBar menuBar) {
        this.menuBar = menuBar;
        menuBar.initMenuBar(this);
    }
    
    /**
     * Sets the Form Toolbar
     * 
     * @param toolbar 
     * @deprecated use setToolbar instead (lower case b)
     */
    public void setToolBar(Toolbar toolbar){
        this.toolbar =toolbar;
        setMenuBar(toolbar.getMenuBar());
    }

    /**
     * Sets the Form Toolbar
     * 
     * @param toolbar 
     */
    public void setToolbar(Toolbar toolbar){
        this.toolbar =toolbar;
        setMenuBar(toolbar.getMenuBar());
    }
    
    /**
     * Gets the Form Toolbar if exists or null
     * 
     * @return the Toolbar instance or null if does not exists.
     */
    public Toolbar getToolbar() {
        return toolbar;
    }

    
    /**
     * Indicates whether lists and containers should scroll only via focus and thus "jump" when
     * moving to a larger component as was the case in older versions of Codename One.
     *
     * @return the value of focusScrolling
     */
    public boolean isFocusScrolling() {
        return focusScrolling;
    }

    /**
     * Indicates whether lists and containers should scroll only via focus and thus "jump" when
     * moving to a larger component as was the case in older versions of Codename One.
     *
     * @param focusScrolling the new value for focus scrolling
     */
    public void setFocusScrolling(boolean focusScrolling) {
        this.focusScrolling = focusScrolling;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] { "titleUIID", "titleAreaUIID" };
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           String.class,
           String.class
       };
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String", "String"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("titleUIID")) {
            if(getTitleComponent() != null) {
                return getTitleComponent().getUIID();
            }
        }
        if(name.equals("titleAreaUIID")) {
            if(getTitleArea() != null) {
                return getTitleArea().getUIID();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("titleUIID")) {
            if(getTitleComponent() != null) {
                getTitleComponent().setUIID((String)value);
            }
            return null;
        }
        if(name.equals("titleAreaUIID")) {
            if(getTitleArea() != null) {
                getTitleArea().setUIID((String)value);
            }
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * A text component that will receive focus and start editing immediately as the form is shown
     * @return the component instance
     */
    public TextArea getEditOnShow() {
        return editOnShow;
    }

    /**
     * A text component that will receive focus and start editing immediately as the form is shown
     * @param editOnShow text component to edit when the form is shown
     */
    public void setEditOnShow(TextArea editOnShow) {
        this.editOnShow = editOnShow;
    }
}
