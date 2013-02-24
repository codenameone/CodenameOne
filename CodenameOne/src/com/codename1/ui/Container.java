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

import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.Style;
import java.util.Vector;

/**
 * A composite pattern with {@link Component}, allows nesting and arranging multiple
 * components using a pluggable layout manager architecture. Containers can be nested
 * one within the other to form elaborate UI's.
 *
 * @see com.codename1.ui.layouts
 * @see Component
 * @author Chen Fishbein
 */
public class Container extends Component {
    private static boolean enableLayoutOnPaint = true;
    private Component leadComponent;
    private Layout layout;
    private java.util.Vector components = new java.util.Vector();
    private boolean shouldLayout = true;
    boolean scrollableX;
    boolean scrollableY;
    private java.util.Vector cmpTransitions;
    private int scrollIncrement = 20;
    private boolean blockFocus = false;
    private boolean dontRecurseContainer;

    /**
     * Constructs a new Container with a new layout manager.
     * 
     * @param layout the specified layout manager
     */
    public Container(Layout layout) {
        super();
        setUIID("Container");
        this.layout = layout;
        setFocusable(false);
    }

    /** 
     * Constructs a new Container, with a {@link FlowLayout}. 
     */
    public Container() {
        this(new FlowLayout());
    }

    /**
     * 
     * @inheritDoc
     */
    protected void initLaf(UIManager uim) {
        if(uim == getUIManager() && isInitialized()){
            return;
        }
        super.initLaf(uim);
        LookAndFeel laf = uim.getLookAndFeel();
        setSmoothScrolling(laf.isDefaultSmoothScrolling());
        if(components != null){
            int count = getComponentCount();
            for (int i = 0; i < count; i++) {
                Component c = getComponentAt(i);
                c.initLaf(uim);
            }
        }

    }

    
    /**
     * Sets the lead component for this container, a lead component takes over the entire
     * component hierarchy and receives all the events for the container hierarchy.
     * 
     * @param lead component that takes over the hierarchy
     */
    public void setLeadComponent(Component lead) {
        leadComponent = lead;
        if(isInitialized()) {
            initLead();
        }
    }

    void focusGainedInternal() {
        super.focusGainedInternal();
        if(leadComponent != null) {
            setFocusLead(true);
        }
    }

    void focusLostInternal() {
        super.focusLostInternal();
        if(leadComponent != null) {
            setFocusLead(false);
        }
    }

    /**
     * Returns the lead component for this hierarchy if such a component is defined
     * 
     * @return the lead component
     */
    public Component getLeadComponent() {
        if(leadComponent != null) {
            return leadComponent;
        }
        if(hasLead) {
            return super.getLeadComponent();
        }
        return null;
    }

    /**
     * Returns the lead container thats handling the leading, this is useful for
     * a container hierachy where the parent container might not be the leader
     *
     * @return the lead component
     */
    public Container getLeadParent() {
        if(leadComponent != null) {
            return this;
        }
        if(hasLead) {
            return getParent().getLeadParent();
        }
        return null;
    }

    private void initLead() {
        disableFocusAndInitLead(this);
        setFocusable(true);
        hasLead = true;
    }

    /**
     * @inheritDoc
     */
    public void keyPressed(int k) {
        if(leadComponent != null) {
            leadComponent.keyPressed(k);
            repaint();
        }
    }

    /**
     * @inheritDoc
     */
    public void keyReleased(int k) {
        if(leadComponent != null) {
            leadComponent.keyReleased(k);
            repaint();
        }
    }

    private void disableFocusAndInitLead(Container c) {
        for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
            Component cu = c.getComponentAt(iter);
            if(cu instanceof Container) {
                disableFocusAndInitLead((Container)cu);
            }
            cu.setFocusable(false);
            cu.hasLead = true;
        }
    }

    /**
     * Returns the layout manager responsible for arranging this container
     * 
     * @return the container layout manager
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Sets the layout manager responsible for arranging this container
     * 
     * @param layout the specified layout manager
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
        if(layout instanceof BorderLayout && isScrollable()) {
            setScrollable(false);
        }
    }

    /**
     * Same as setShouldCalcPreferredSize(true) but made accessible for 
     * layout managers
     */
    public void invalidate() {
        setShouldCalcPreferredSize(true);
    }

    /**
     * Flags this container to preform layout 
     * 
     * @param layout
     */
    protected void setShouldLayout(boolean layout) {
        if (!shouldCalcScrollSize) {
            this.shouldCalcScrollSize = layout;
        }
        if (shouldLayout != layout) {
            shouldLayout = layout;
            shouldCalcPreferredSize = layout;
            shouldCalcScrollSize = layout;
            Container parent = getParent();
            if(parent != null){
                parent.setShouldLayout(layout);
            }
        }
    }
    /**
     * @inheritDoc
     */
    public void setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
        // minor optimization preventing repeated invokations to setShouldCalcPreferredSize
        if(shouldCalcPreferredSize && this.shouldLayout && this.shouldCalcPreferredSize && !isInitialized()) {
            Container p = getParent();
            if(p != null && p.shouldLayout && p.shouldCalcPreferredSize) {
                return;
            }
        }
        super.setShouldCalcPreferredSize(shouldCalcPreferredSize);
        shouldLayout = shouldCalcPreferredSize;
        if (shouldLayout) {
            int size = components.size();
            for(int iter = 0 ; iter < size ; iter++) {
                Component cmp = (Component) components.elementAt(iter);
                if (cmp instanceof Container) {
                    ((Container) cmp).setShouldCalcPreferredSize(shouldCalcPreferredSize);
                }
            }
        }
    }

    /**
     * Returns the width for layout manager purposes, this takes scrolling
     * into consideration unlike the getWidth method.
     * 
     * @return the layout width
     */
    public int getLayoutWidth() {
        if (isScrollableX()) {
            return Math.max(getWidth(), getPreferredW());
        } else {
            Container parent = getScrollableParent();
            if (parent != null && parent.isScrollableX()) {
                return Math.max(getWidth(), getPreferredW());
            }
            int width = getWidth();
            if (width <= 0) {
                return getPreferredW();
            }
            return width;
        }
    }

    /**
     * Returns the height for layout manager purposes, this takes scrolling
     * into consideration unlike the getWidth method.
     * 
     * @return the layout height
     */
    public int getLayoutHeight() {
        if (scrollableY) {
            return Math.max(getHeight(), getPreferredH());
        } else {
            Container parent = getScrollableParent();
            if (parent != null && parent.scrollableY) {
                return Math.max(getHeight(), getPreferredH());
            }
            int height = getHeight();
            if (height <= 1) {
                return getPreferredH();
            }
            return height;
        }
    }

    /**
     * Invokes apply/setRTL recursively on all the children components of this container
     * 
     * @param rtl right to left bidi indication
     * @see Component#setRTL(boolean) 
     */
    public void applyRTL(boolean rtl) {
        setRTL(rtl);
        int c = getComponentCount();
        for(int iter = 0 ; iter < c ; iter++) {
            Component current = getComponentAt(iter);
            if(current instanceof Container) {
                ((Container)current).applyRTL(rtl);
            } else {
                current.setRTL(rtl);
            }
        }
    }


    /**
     * Returns a parent container that is scrollable or null if no parent is 
     * scrollable.
     * 
     * @return a parent container that is scrollable or null if no parent is 
     * scrollable.
     */
    private Container getScrollableParent() {
        Container parent = getParent();
        while (parent != null) {
            if (parent.isScrollable()) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Adds a Component to the Container
     * 
     * @param cmp the component to be added
     */
    public void addComponent(Component cmp) {
        layout.addLayoutComponent(null, cmp, this);
        insertComponentAt(components.size(), cmp);
    }

    /**
     * Adds a Component to the Container
     * 
     * @param constraints this method is useful when the Layout requires a constraint
     * such as the BorderLayout.
     * In this case you need to specify an additional data when you add a Component,
     * such as "CENTER", "NORTH"...
     *
     * @param cmp component to add
     */
    public void addComponent(Object constraints, Component cmp) {
        layout.addLayoutComponent(constraints, cmp, this);
        insertComponentAt(components.size(), cmp);
    }


    /**
     * Adds a Component to the Container
     * 
     * @param index location to insert the Component
     * @param constraints this method is useful when the Layout requires a constraint
     * such as the BorderLayout.
     * In this case you need to specify an additional data when you add a Component,
     * such as "CENTER", "NORTH"...
     * @param cmp component to add
     */
    public void addComponent(int index, Object constraints, Component cmp) {
        layout.addLayoutComponent(constraints, cmp, this);
        insertComponentAt(index, cmp);
    }

    void insertComponentAt(int index, final Component cmp) {
        if (cmp.getParent() != null) {
            throw new IllegalArgumentException("Component is already contained in Container: " + cmp.getParent());
        }
        if(cmp instanceof Form) {
            cmp.setVisible(true);
            cmp.setPreferredSize(null);
        }
        UIManager manger = getUIManager();
        boolean refreshLaf = manger != cmp.getUIManager();
        cmp.setParent(this);
        if(refreshLaf){
           Display.getInstance().callSerially(new Runnable() {
                public void run() {
                               cmp.refreshTheme(false);
                }
            });
        }
        components.insertElementAt(cmp, index);
        setShouldCalcPreferredSize(true);
        if (isInitialized()) {
            cmp.initComponentImpl();
        }
    }

    /**
     * This method adds the Component at a specific index location in the Container
     * Components array.
     * 
     * @param index location to insert the Component
     * @param cmp the Component to add
     * @throws ArrayIndexOutOfBoundsException if index is out of bounds
     * @throws IllegalArgumentException if Component is already contained or
     * the cmp is a Form Component
     */
    public void addComponent(int index, Component cmp) {
        layout.addLayoutComponent(null, cmp, this);
        insertComponentAt(index, cmp);
    }

    /**
     * This method replaces the current Component with the next Component.
     * Current Component must be contained in this Container.
     * This method returns when transition has finished.
     * 
     * @param current a Component to remove from the Container
     * @param next a Component that replaces the current Component
     * @param t a Transition between the add and removal of the Components
     *  a Transition can be null
     */
    public void replaceAndWait(final Component current, final Component next, final Transition t) {
        replaceComponents(current, next, t, true, false, null, 0, 0);
    }

    /**
     * This method replaces the current Component with the next Component.
     * Current Component must be contained in this Container.
     * This method returns when transition has finished.
     *
     * @param current a Component to remove from the Container
     * @param next a Component that replaces the current Component
     * @param t a Transition between the add and removal of the Components
     *  a Transition can be null
     * @param layoutAnimationSpeed the speed of the layout animation after replace  is completed
     */
    public void replaceAndWait(final Component current, final Component next, final Transition t, int layoutAnimationSpeed) {
        enableLayoutOnPaint = false;
        replaceComponents(current, next, t, true, false, null, 0, layoutAnimationSpeed);
        if(layoutAnimationSpeed > 0) {
            animateLayoutAndWait(layoutAnimationSpeed);
        }
        dontRecurseContainer = false;
        enableLayoutOnPaint = true;
    }

    /**
     * This method replaces the current Component with the next Component
     *
     * @param current a Component to remove from the Container
     * @param next a Component that replaces the current Component
     * @param t a Transition between the add and removal of the Components
     *  a Transition can be null
     * @param onFinish invoked when the replace operation is completed, may be null
     * @param  growSpeed after replace is completed the component can gradually grow/shrink to fill up
     * available room, set this to 0 for immediate growth or any larger number for gradual animation. -1 indicates
     * a special case where no validation occurs
     */
    public void replace(final Component current, final Component next, final Transition t, Runnable onFinish, int growSpeed) {
        replaceComponents(current, next, t, false, false, onFinish, growSpeed, 0);
    }

    /**
     * This method replaces the current Component with the next Component.
     * Current Component must be contained in this Container.
     * This method returns when transition has finished.
     *
     * @param current a Component to remove from the Container
     * @param next a Component that replaces the current Component
     * @param t a Transition between the add and removal of the Components
     *  a Transition can be null
     * @param dropEvents indicates if the display should drop all events
     * while this Component replacing is happening
     */
    public void replaceAndWait(final Component current, final Component next,
            final Transition t, boolean dropEvents) {
        replaceComponents(current, next, t, true, dropEvents, null, 0, 0);
    }

    /**
     * This method replaces the current Component with the next Component.
     * Current Component must be contained in this Container.
     * This method return immediately.
     * 
     * @param current a Component to remove from the Container
     * @param next a Component that replaces the current Component
     * @param t a Transition between the add and removal of the Components
     *  a Transition can be null
     */
    public void replace(final Component current, final Component next, final Transition t) {
        replaceComponents(current, next, t, false, false, null, 0, 0);
    }

    private void replaceComponents(final Component current, final Component next, 
            final Transition t, boolean wait, boolean dropEvents, Runnable onFinish, int growSpeed, int layoutAnimationSpeed) {
        if (!contains(current)) {
            throw new IllegalArgumentException("Component " + current + " is not contained in this Container");
        }
        if (t == null || !isVisible() || getComponentForm() == null) {
            replace(current, next, false);
            return;
        }

        setScrollX(0);
        setScrollY(0);

        next.setX(current.getX());
        next.setY(current.getY());
        next.setWidth(current.getWidth());
        next.setHeight(current.getHeight());
        next.setParent(this);
        if (next instanceof Container) {
            ((Container) next).layoutContainer();
        }

        final Anim anim = new Anim(this, current, next, t);
        anim.onFinish = onFinish;
        anim.growSpeed = growSpeed;
        anim.layoutAnimationSpeed = layoutAnimationSpeed;

        // register the transition animation
        getComponentForm().registerAnimatedInternal(anim);
        //wait until animation has finished
        if (wait) {
            Display.getInstance().invokeAndBlock(anim, dropEvents);
        }
    }

    private boolean isParentOf(Component c) {
        c = c.getParent();
        if (c == null || c instanceof Form) {
            return false;
        }
        return (c == this) || isParentOf(c);
    }

    void onParentPositionChange() {
        int cmpCount = getComponentCount();
        for (int iter = 0; iter < cmpCount ; iter++) {
            Component c = getComponentAt(iter);
            c.onParentPositionChange();
        }
    }

    private boolean requestFocusChild(boolean avoidRepaint) {
        int cmpCount = getComponentCount();
        for (int iter = 0; iter < cmpCount ; iter++) {
            Component c = getComponentAt(iter);
            if (c.isFocusable()) {
                if(avoidRepaint) {
                    getComponentForm().setFocusedInternal(c);
                } else {
                    c.requestFocus();
                }
                return true;
            }
            if (c instanceof Container && ((Container) c).requestFocusChild(avoidRepaint)) {
                return true;
            }
        }
        return false;
    }

    private void cancelRepaintsRecursively(Component c, CodenameOneImplementation l) {
        if(c instanceof Container) {
            Container cnt = (Container)c;
            int count = cnt.getComponentCount();
            for(int i = 0 ; i < count ; i++) {
                cancelRepaintsRecursively(cnt.getComponentAt(i), l);
            }
        }
        l.cancelRepaint(c);
    }

    private void cancelRepaintsRecursively(Component c) {
        cancelRepaintsRecursively(c, Display.getInstance().getImplementation());
    }

    void replace(final Component current, final Component next, boolean avoidRepaint) {
        int index = components.indexOf(current);
        boolean currentFocused = false;
        if (current.getComponentForm() != null) {
            Component currentF = current.getComponentForm().getFocused();
            currentFocused = currentF == current;
            if (!currentFocused && current instanceof Container && currentF != null && ((Container) current).isParentOf(currentF)) {
                currentFocused = true;
            }
        }
        Object constraint = layout.getComponentConstraint(current);
        if (constraint != null) {
            removeComponentImpl(current);
            layout.addLayoutComponent(constraint, next, Container.this);
        } else {
            removeComponentImpl(current);
        }
        cancelRepaintsRecursively(current);
        next.setParent(null);
        if (index < 0) {
            index = 0;
        }
        insertComponentAt(index, next);
        if (currentFocused) {
            if (next.isFocusable()) {
                if(avoidRepaint) {
                    getComponentForm().setFocusedInternal(next);
                } else {
                    next.requestFocus();
                }
            } else {
                if (next instanceof Container) {
                    ((Container) next).requestFocusChild(avoidRepaint);
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    void initComponentImpl() {
        if (!isInitialized()) {
            super.initComponentImpl();
        }
        int size = components.size();
        for(int iter = 0 ; iter < size ; iter++) {
            ((Component) components.elementAt(iter)).initComponentImpl();
        }
        if(leadComponent != null) {
            initLead();
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isEnabled() {
        if(leadComponent != null) {
            return leadComponent.isEnabled();
        }
        return super.isEnabled();
    }

    /**
     * removes a Component from the Container, notice that removed component might still have
     * a pending repaint in the queue that won't be removed. Calling form.repaint() will workaround
     * such an issue.
     *
     * @param cmp the removed component
     */
    public void removeComponent(Component cmp) {
        removeComponentImpl(cmp);
    }

    /**
     * removes a Component from the Container
     * 
     * @param cmp the removed component
     */
    void removeComponentImpl(Component cmp) {
        Form parentForm = cmp.getComponentForm();
        layout.removeLayoutComponent(cmp);
        cmp.deinitializeImpl();
        components.removeElement(cmp);
        cmp.setParent(null);
        if (parentForm != null) {
            if (parentForm.getFocused() == cmp || cmp instanceof Container && ((Container) cmp).contains(parentForm.getFocused())) {
                parentForm.setFocused(null);
            }
            if (cmp.isSmoothScrolling()) {
                parentForm.deregisterAnimatedInternal(cmp);
            }
        }
        cmp.cancelRepaints();
        if(cmp instanceof Form) {
            cmp.setVisible(false);
        }
        setShouldCalcPreferredSize(true);
    }

    
    /**
     * remove this component and it's children from the painting queue
     */
    protected void cancelRepaints() {
        super.cancelRepaints();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponentAt(i);
            c.cancelRepaints();
        }
    }
    
    /**
     * Cleansup the initialization flags in the hierachy
     */
    void deinitializeImpl() {
        super.deinitializeImpl();
        int size = components.size();
        for (int iter = 0; iter < size; iter++) {
            ((Component) components.elementAt(iter)).deinitializeImpl();
        }
        flushReplace();
    }

    /**
     * Flushes ongoing replace operations to prevent two concurrent replace operations from colliding.
     * If there is no ongoing replace nothing will occur
     */
    public void flushReplace() {
        if (cmpTransitions != null) {
            int size = cmpTransitions.size();
            for (int iter = 0; iter < size; iter++) {
                ((Anim) cmpTransitions.elementAt(iter)).destroy();
            }
            cmpTransitions.removeAllElements();
            cmpTransitions = null;
        }
    }

    /**
     * remove all Components from container, notice that removed component might still have
     * a pending repaint in the queue that won't be removed. Calling form.repaint() will workaround
     * such an issue.
     */
    public void removeAll() {
        Form parentForm = getComponentForm();
        if (parentForm != null) {
            Component focus = parentForm.getFocused();
            if (focus != null && contains(focus)) {
                parentForm.setFocused(null);
            }
        }
        Object[] arr = new Object[components.size()];
        components.copyInto(arr);

        for (int i = 0; i < arr.length; i++) {
            removeComponent((Component) arr[i]);
        }
    }

    /**
     * Re-layout the container, this is useful when we modify the container hierarchy and
     * need to redo the layout
     */
    public void revalidate() {
        setShouldCalcPreferredSize(true);
        Form root = getComponentForm();
        
        if (root != null) {
            root.layoutContainer();
            root.repaint();
        } else {
            layoutContainer();
            repaint();
        }
    }

    private void paintContainerChildrenForAnimation(Container cnt, Graphics g) {
        int ourX = getAbsoluteX();
        int ourY = getAbsoluteY();
        int cc = cnt.getComponentCount();
        for(int iter = 0 ; iter < cc ; iter++) {
            Component cmp = cnt.getComponentAt(iter);
            if(cmp.getClass() == Container.class) {
                paintContainerChildrenForAnimation((Container)cmp, g);
                continue;
            }
            int abx = cmp.getAbsoluteX();
            int aby = cmp.getAbsoluteY();
            int oldX = cmp.getX();
            int oldY = cmp.getY();
            cmp.setParent(this);
            cmp.setX(abx - ourX);
            cmp.setY(aby - ourY);
            cmp.paintInternal(g, false);
            cmp.setParent(cnt);
            cmp.setX(oldX);
            cmp.setY(oldY);
        }
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        if(enableLayoutOnPaint) {
            layoutContainer();
        }
        g.translate(getX(), getY());
        int size = components.size();
        CodenameOneImplementation impl = Display.getInstance().getImplementation();
        if(dontRecurseContainer) {
            for (int i = 0; i < size; i++) {
                Component cmp = (Component)components.elementAt(i);
                if(cmp.getClass() == Container.class) {
                    paintContainerChildrenForAnimation((Container)cmp, g);
                } else {
                    cmp.paintInternal(impl.getComponentScreenGraphics(this, g), false);
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                Component cmp = (Component)components.elementAt(i);
                cmp.paintInternal(impl.getComponentScreenGraphics(this, g), false);
            }
        }
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        g.translate(-tx, -ty);
        paintGlass(g);
        g.translate(tx, ty);
        g.translate(-getX(), -getY());
    }

    /**
     * This method can be overriden by a component to draw on top of itself or its children
     * after the component or the children finished drawing in a similar way to the glass
     * pane but more refined per component
     *
     * @param g the graphics context
     */
    void paintGlassImpl(Graphics g) {
        super.paintGlassImpl(g);
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        g.translate(-tx, -ty);
        paintGlass(g);
        g.translate(tx, ty);
    }

    /**
     * This method can be overriden by a component to draw on top of itself or its children
     * after the component or the children finished drawing in a similar way to the glass
     * pane but more refined per component
     *
     * @param g the graphics context
     */
    protected void paintGlass(Graphics g) {
    }

    void paintIntersecting(Graphics g, Component cmp, int x, int y, int w, int h, boolean above) {

        if (layout.isOverlapSupported() && components.contains(cmp)) {
            int indexOfComponent = components.indexOf(cmp);
            
            int startIndex;
            int endIndex;
            if (above) {
                startIndex = indexOfComponent + 1;
                endIndex = components.size();
            } else {
                startIndex = 0;
                endIndex = indexOfComponent;
            }

            for (int i = startIndex; i < endIndex; i++) {
                Component cmp2 = (Component) components.elementAt(i);
                if(Rectangle.intersects(x, y, w, h,
                        cmp2.getAbsoluteX() + cmp2.getScrollX(),
                        cmp2.getAbsoluteY() + cmp2.getScrollY(),
                        cmp2.getBounds().getSize().getWidth(),
                        cmp2.getBounds().getSize().getHeight())){
                    cmp2.paintInternal(g, false);
                }
            }
        }
    }

    /**
     * Performs the layout of the container if a layout is necessary
     */
    public void layoutContainer() {
        //will compute the container + components and will layout the components.
        if (shouldLayout) {
            shouldLayout = false;
            doLayout();            
        }
    }

    /**
     * Lays out the container
     */
    void doLayout() {
        layout.layoutContainer(this);
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component c = getComponentAt(i);
            if (c instanceof Container) {
                ((Container) c).layoutContainer();
            }else{
                c.laidOut();
            }
        }
        laidOut();
        onParentPositionChange();            
    }

    /**
     * Returns the number of components
     * 
     * @return the Component count
     */
    public int getComponentCount() {
        return components.size();
    }

    /**
     * Returns the Component at a given index
     * 
     * @param index of the Component you wish to get
     * @return a Component
     * @throws ArrayIndexOutOfBoundsException if an invalid index was given.
     */
    public Component getComponentAt(
            int index) {
        return (Component) components.elementAt(index);
    }

    /**
     * Returns the Component index in the Container
     * 
     * @param cmp the component to search for
     * @return the Component index in the Container or -1 if not found
     */
    public int getComponentIndex(Component cmp) {
        int count = getComponentCount();
        for (int i = 0; i <
                count; i++) {
            Component c = getComponentAt(i);
            if (c.equals(cmp)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true if the given component is within the hierarchy of this container
     *
     * @param cmp a Component to check
     * @return true if this Component contains in this Container
     */
    public boolean contains(Component cmp) {
        boolean found = false;
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component c = getComponentAt(i);
            if (c.equals(cmp)) {
                return true;
            }

            if (c instanceof Container) {
                found = ((Container) c).contains(cmp);
                if (found) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * Makes sure the component is visible in the scroll if this container is
     * scrollable
     *
     * @param c the component that will be scrolling for visibility
     */
    public void scrollComponentToVisible(Component c) {
        if (isScrollable()) {
            if (c != null) {
                Rectangle r = c.getVisibleBounds();
                if (c.getParent() != null) {
                    // special case for the first component to allow the user to scroll all the 
                    // way to the top
                    Form f = getComponentForm();
                    if (f != null && f.findFirstFocusable() == c) {
                        // support this use case only if the component doesn't explicitly declare visible bounds
                        if (r == c.getBounds()) {
                            scrollRectToVisible(new Rectangle(0, 0,
                                    c.getX() + Math.min(c.getWidth(), getWidth()),
                                    c.getY() + Math.min(c.getHeight(), getHeight())), this);
                            return;
                        }
                    }
                }
                boolean moveToVisible = true;
                boolean large = c.getVisibleBounds().getSize().getHeight() > getHeight() || 
                        c.getVisibleBounds().getSize().getWidth() > getWidth();
                if (large) {
                    int x = getScrollX();
                    int y = getScrollY();
                    int w = getWidth();
                    int h = getHeight();
                    boolean visible = contains(c) && Rectangle.intersects(c.getAbsoluteX(),
                            c.getAbsoluteY(),
                            c.getWidth(),
                            c.getHeight(),
                            getAbsoluteX() + x,
                            getAbsoluteY() + y,
                            w,
                            h);
                    //if this is a big component no need to scroll to the begining if it's
                    //partially visible
                    moveToVisible = !visible;
                }
                if (moveToVisible) {
                    scrollRectToVisible(r.getX(), r.getY(),
                            Math.min(r.getSize().getWidth(), getWidth()),
                            Math.min(r.getSize().getHeight(), getHeight()), c);
                }
            }
        }
    }

    /**
     * This method scrolls the Container if Scrollable towards the given 
     * Component based on the given direction.
     * 
     * @param direction is the direction of the navigation (Display.GAME_UP, 
     * Display.GAME_DOWN, ...) 
     * @param next the Component to move the scroll towards.
     * 
     * @return true if next Component is now visible.
     */    
    boolean moveScrollTowards(int direction, Component next) {
        if (isScrollable()) {
            Component current = null;
            Form f = getComponentForm();
            current = f.getFocused();

            boolean cyclic = f.isCyclicFocus();
            f.setCyclicFocus(false);
            boolean edge = false;
            boolean currentLarge = false;
            boolean scrollOutOfBounds = false;
            
            int x = getScrollX();
            int y = getScrollY();
            int w = getWidth();
            int h = getHeight();

            switch (direction) {
                case Display.GAME_UP:
                    if(!cyclic && getScrollY() == 0){
                        return true;
                    }
                    y = getScrollY() - scrollIncrement;
                    edge = f.findNextFocusUp() == null;
                    currentLarge = (current != null && current.getVisibleBounds().getSize().getHeight() > getHeight());
                    scrollOutOfBounds = y < 0;
                    if(scrollOutOfBounds){
                        y = 0;
                    }
                    break;
                case Display.GAME_DOWN:
                    y = getScrollY() + scrollIncrement;
                    edge = f.findNextFocusDown() == null;
                    currentLarge = (current != null && current.getVisibleBounds().getSize().getHeight() > getHeight());
                    scrollOutOfBounds = y > getScrollDimension().getHeight() - getHeight();
                    if(scrollOutOfBounds){
                        y = getScrollDimension().getHeight() - getHeight();
                    }
                    break;
                case Display.GAME_RIGHT:
                    x = getScrollX() + scrollIncrement;
                    edge = f.findNextFocusRight() == null;
                    currentLarge = (current != null && current.getVisibleBounds().getSize().getWidth() > getWidth());
                    scrollOutOfBounds = x > getScrollDimension().getWidth() - getWidth();
                    if(scrollOutOfBounds){
                        x = getScrollDimension().getWidth() - getWidth();
                    }
                    break;
                case Display.GAME_LEFT:
                    x = getScrollX() - scrollIncrement;
                    edge = f.findNextFocusLeft() == null;
                    currentLarge = (current != null && current.getVisibleBounds().getSize().getWidth() > getWidth());
                    scrollOutOfBounds = x < 0;
                    if(scrollOutOfBounds){
                        x = 0;
                    }
                    break;
            }
            f.setCyclicFocus(cyclic);
            //if the Form doesn't contain a focusable Component simply move the 
            //viewport by pixels
            if(next == null || next == this){
                scrollRectToVisible(x, y, w, h, this);
                return false;
            }
            //if we are on the edge and this is a non cyclic Form.
            if(!cyclic && direction == Display.GAME_DOWN && edge){
                scrollRectToVisible(x, y, w, h, this);
                return false;
            }
            
            boolean nextIntersects = contains(next) && Rectangle.intersects(next.getAbsoluteX(),
                    next.getAbsoluteY(),
                    next.getWidth(),
                    next.getHeight(),
                    getAbsoluteX() + x,
                    getAbsoluteY() + y,
                    w,
                    h);
                    
            if ((nextIntersects && !currentLarge && !edge) || (Rectangle.contains(
                    getAbsoluteX() + getScrollX(),
                    getAbsoluteY() + getScrollY(),
                    w,
                    h,
                    next.getAbsoluteX(),
                    next.getAbsoluteY(),
                    next.getWidth(),
                    next.getHeight()))) {
                //scrollComponentToVisible(next);
                return true;
            } else {
                if (!scrollOutOfBounds) {
                    scrollRectToVisible(x, y, w, h, this);
                    //if after moving the scroll the current focus is out of the
                    //view port and the next focus is in the view port move 
                    //the focus
                    if (nextIntersects && !Rectangle.intersects(current.getAbsoluteX(),
                            current.getAbsoluteY(),
                            current.getWidth(),
                            current.getHeight(),
                            getAbsoluteX() + x,
                            getAbsoluteY() + y,
                            w,
                            h)) {
                        return true;
                    }
                    return false;
                } else {
                    //scrollComponentToVisible(next);
                    return true;
                }
            }

        }


        return true;
    }
    /**
     * Returns a Component that exists in the given x, y coordinates by traversing
     * component objects and invoking contains
     * 
     * @param x absolute screen location
     * @param y absolute screen location
     * @return a Component if found, null otherwise
     * @see Component#contains
     */
    public Component getComponentAt(int x, int y) {
        int count = getComponentCount();
        boolean overlaps = getLayout().isOverlapSupported();
        Component component = null;
        for (int i = count - 1; i >= 0; i--) {
            Component cmp = getComponentAt(i);
            if (cmp.contains(x, y)) {
                component = cmp;
                if (!overlaps && component.isFocusable()) {
                    return component;
                }
                if (cmp instanceof Container) {
                    component = ((Container) cmp).getComponentAt(x, y);
                }
                if (!overlaps || component.isFocusable() || component.isGrabsPointerEvents()) {
                    return component;
                }
            }
        }
        if (component != null){
            return component;
        }
        if (contains(x, y)) {
            return this;
        }
        return null;
    }

    Component findDropTargetAt(int x, int y) {
        int count = getComponentCount();
        for (int i = count - 1; i >= 0; i--) {
            Component cmp = getComponentAt(i);
            if (cmp.contains(x, y)) {
                if (cmp.isDropTarget()) {
                    return cmp;
                }
                if (cmp instanceof Container) {
                    Component component = ((Container) cmp).findDropTargetAt(x, y);
                    if(component != null) {
                        return component;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * @inheritDoc
     */
    public void pointerHover(int[] x, int[] y) {
        if(!isDragActivated()) {
            Component c = getComponentAt(x[0], y[0]);
            if(c != null && c.isFocusable()) {
                c.requestFocus();
            }
        }
        super.pointerDragged(x[0], y[0]);
    }
    
    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
        clearDrag();
        setDragActivated(false);
        Component cmp = getComponentAt(x, y);
        if (cmp == this) {
            super.pointerPressed(x, y);
        } else if (cmp != null) {
            cmp.pointerPressed(x, y);
        }
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        Dimension d = layout.getPreferredSize(this);
        Style style = getStyle();
        if(isVisible() && style.getBorder() != null && d.getWidth() != 0 && d.getHeight() != 0) {
            d.setWidth(Math.max(style.getBorder().getMinimumWidth(), d.getWidth()));
            d.setHeight(Math.max(style.getBorder().getMinimumHeight(), d.getHeight()));
        }
        if(UIManager.getInstance().getLookAndFeel().isBackgroundImageDetermineSize() && style.getBgImage() != null) {
            d.setWidth(Math.max(style.getBgImage().getWidth(), d.getWidth()));
            d.setHeight(Math.max(style.getBgImage().getHeight(), d.getHeight()));
        }
        return d;
    }

    /**
     * @inheritDoc
     */
    protected String paramString() {
        String className = layout.getClass().getName();
        String layoutStr = className.substring(className.lastIndexOf('.') + 1);
        return super.paramString() + ", layout = " + layoutStr +
                ", scrollableX = " + scrollableX +
                ", scrollableY = " + scrollableY +
                ", components = " + getComponentsNames();
    }

    /**
     * Return the conatainer components objects as list of Strings
     * @return the conatainer components objects as list of Strings
     */
    private String getComponentsNames() {
        String ret = "[";
        int size = components.size();
        for(int iter = 0 ; iter < size ; iter++) {
            String className = components.elementAt(iter).getClass().getName();
            ret += className.substring(className.lastIndexOf('.') + 1) + ", ";
        }
        if (ret.length() > 1) {
            ret = ret.substring(0, ret.length() - 2);
        }
        ret = ret + "]";
        return ret;
    }

    /**
     * @inheritDoc
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        int size = components.size();
        for(int iter = 0 ; iter < size ; iter++) {
            Component cmp = (Component) components.elementAt(iter);
            cmp.refreshTheme(merge);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isScrollableX() {
        return scrollableX && (getScrollDimension().getWidth() > getWidth());
    }

    /**
     * @inheritDoc
     */
    public boolean isScrollableY() {
        return scrollableY && (getScrollDimension().getHeight() > getHeight() || isAlwaysTensile());
    }

    /**
     * @inheritDoc
     */
    public int getSideGap() {
        // isScrollableY() in the base method is very expensive since it triggers getScrollDimension before the layout is complete!
        if(scrollSize == null) {
            if (scrollableY && isScrollVisible()) {
                return getUIManager().getLookAndFeel().getVerticalScrollWidth();
            }
        } else {
            return super.getSideGap();
        }
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int getBottomGap() {
        // isScrollableY() in the base method is very expensive since it triggers getScrollDimension before the layout is complete!
        if (scrollableX && isScrollVisible()) {
            return getUIManager().getLookAndFeel().getHorizontalScrollHeight();
        }
        return 0;
    }

    /**
     * Sets whether the component should/could scroll on the X axis
     * 
     * @param scrollableX whether the component should/could scroll on the X axis
     */
    public void setScrollableX(boolean scrollableX) {
        if(layout instanceof BorderLayout) {
            this.scrollableX = false;
        } else {
            this.scrollableX = scrollableX;
        }
    }

    /**
     * Sets whether the component should/could scroll on the Y axis
     * 
     * @param scrollableY whether the component should/could scroll on the Y axis
     */
    public void setScrollableY(boolean scrollableY) {
        if(layout instanceof BorderLayout) {
            this.scrollableY = false;
        } else {
            this.scrollableY = scrollableY;
        }
    }

    /**
     * The equivalent of calling both setScrollableY and setScrollableX
     * 
     * @param scrollable whether the component should/could scroll on the 
     * X and Y axis
     */
    public void setScrollable(boolean scrollable) {
        setScrollableX(scrollable);
        setScrollableY(scrollable);
    }

    /**
     * @inheritDoc
     */
    public void setCellRenderer(boolean cellRenderer) {
        if (isCellRenderer() != cellRenderer) {
            super.setCellRenderer(cellRenderer);
            int size = getComponentCount();
            for (int iter = 0; iter <
                    size; iter++) {
                getComponentAt(iter).setCellRenderer(cellRenderer);
            }
        }
    }

    /**
     * Determines the scroll increment size of this Container.
     * This value is in use when the current foucs element within this Container
     * is larger than this Container size.
     *
     * @param scrollIncrement the size in pixels.
     */
    public void setScrollIncrement(int scrollIncrement) {
        this.scrollIncrement = scrollIncrement;
    }

    /**
     * Gets the Container scroll increment
     *
     * @return the scroll increment in pixels.
     */
    public int getScrollIncrement() {
        return scrollIncrement;
    }

    /**
     * Finds the first focusable Component on this Container
     *
     * @return a focusable Component or null if not exists;
     */
    public Component findFirstFocusable() {
        int size = getComponentCount();

        for (int iter = 0; iter < size; iter++) {
            Component current = getComponentAt(iter);
            if(current.isVisible()) {
                if(current.isFocusable()){
                    return current;
                }
                if (current instanceof Container && !((Container)current).isBlockFocus()) {
                    Component cmp = ((Container)current).findFirstFocusable();
                    if(cmp != null){
                        return cmp;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Recusively focuses components for the lead component functionality
     */
    private void setFocusLead(boolean f) {
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component c = getComponentAt(i);
            if(c instanceof Container) {
                ((Container)c).setFocusLead(f);
            } 
            c.setFocus(f);
            if(f) {
                c.fireFocusGained();
            } else {
                c.fireFocusLost();
            }
        }
    }

    /**
     * @inheritDoc
     */
    protected void dragInitiated() {
        super.dragInitiated();
        if(leadComponent != null) {
            leadComponent.dragInitiated();
        }
    }

    /**
     * This method will recursively set all the Container chidrens to be 
     * enabled/disabled.
     * If the Container is disabled and a child Component changed it's state to 
     * be enabled, the child Component will be treated as an enabled Component.
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component c = getComponentAt(i);
            c.setEnabled(enabled);
        }
    }

    /**
     * This is a callback method for the peer component class
     */
    void setLightweightMode(boolean l) {
        int size = getComponentCount();
        for(int iter = 0 ; iter < size ; iter++) {
            getComponentAt(iter).setLightweightMode(l);
        }
    }

    /**
     * @inheritDoc
     */
    protected int getGridPosY() {
        int scroll = getScrollY();
        int size = getComponentCount();
        int bestRow = 0;
        for(int iter = 0 ; iter < size ; iter++) {
            Component c = getComponentAt(iter);
            int y = c.getY();
            if(Math.abs(scroll - y) < Math.abs(scroll - bestRow)) {
                bestRow = y;
            }
        }
        if(Math.abs(scroll - bestRow) > 2) {
            return bestRow;
        }
        return scroll;
    }

    /**
     * Returns false for the special case where a container has an opaque/flattened child that
     * occupies its entire face
     */
    private boolean shouldPaintContainerBackground() {
        if(getComponentCount() == 1) {
            Style s = getStyle();
            if(s.getPadding(TOP) == 0 && s.getPadding(BOTTOM) == 0 &&
                    s.getPadding(LEFT) == 0 && s.getPadding(RIGHT) == 0) {
                Component c = getComponentAt(0);
                if(c.getWidth() == getWidth() && c.getHeight() == getHeight()) {
                    if(c.isFlatten() || (c.getStyle().getBgTransparency() & 0xff) == 0xff) {
                        return false;
                    }
                    if(c instanceof Container) {
                        return ((Container)c).shouldPaintContainerBackground();
                    }
                }
            }
        }
        return true;
    }

    /**
     * @inheritDoc
     */
    public void paintBackground(Graphics g) {
        if(isFlatten()) {
            super.paintBackgrounds(g);
            return;
        }

        if(shouldPaintContainerBackground()) {
            super.paintBackground(g);
        } 
    }

    /**
     * @inheritDoc
     */
    protected int getGridPosX() {
        int scroll = getScrollX();
        int size = getComponentCount();
        int bestCol = 0;
        for(int iter = 0 ; iter < size ; iter++) {
            Component c = getComponentAt(iter);
            int x = c.getX();
            if(Math.abs(scroll - x) < Math.abs(scroll - bestCol)) {
                bestCol = x;
            }
        }
        if(Math.abs(scroll - bestCol) > 2) {
            return bestCol;
        }
        return scroll;
    }
    
    /**
     * This method blocks all children from getting focus
     *
     * @param blockFocus
     */
    void setBlockFocus(boolean blockFocus) {
        this.blockFocus = blockFocus;
    }

    /**
     * Returns true if focus is blocked for this Container
     *
     * @return
     */
    boolean isBlockFocus() {
        return blockFocus;
    }
        
    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation. This method waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateHierarchyAndWait(final int duration) {
        animateHierarchy(duration, true, 255);
    }

    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateHierarchy(final int duration) {
        animateHierarchy(duration, false, 255);
    }

    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation. This method waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateHierarchyFadeAndWait(final int duration, int startingOpacity) {
        animateHierarchy(duration, true, startingOpacity);
    }

    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateHierarchyFade(final int duration, int startingOpacity) {
        animateHierarchy(duration, false, startingOpacity);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation. This method
     * waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateLayoutFadeAndWait(final int duration, int startingOpacity) {
        animateLayout(duration, true, startingOpacity);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateLayoutFade(final int duration, int startingOpacity) {
        animateLayout(duration, false, startingOpacity);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation. This method
     * waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateLayoutAndWait(final int duration) {
        animateLayout(duration, true, 255);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateLayout(final int duration) {
        animateLayout(duration, false, 255);
    }

    /**
     * @inheritDoc
     */
    public void drop(Component dragged, int x, int y) {
        int i = getComponentIndex(dragged);
        if(i > -1) {
            Component dest = getComponentAt(x, y);
            if(dest != dragged) {
                int destIndex = getComponentIndex(dest);
                if(destIndex > -1 && destIndex != i) {
                    removeComponent(dragged);
                    Object con = getLayout().getComponentConstraint(dragged);
                    if(con != null) {
                        addComponent(destIndex, con, dragged);
                    } else {
                        addComponent(destIndex, dragged);
                    }
                }
            }
            animateLayout(400);
        } else {
            Container oldParent = dragged.getParent();
            if(oldParent != null) {
                oldParent.removeComponent(dragged);
            }
            Component pos = getComponentAt(x, y);
            i = getComponentIndex(pos);
            if(i > -1) {
                addComponent(i, dragged);
            } else {
                addComponent(dragged);
            }
            getComponentForm().animateHierarchy(400);
        }
    }

    /**
     * Creates a motion object for animation, allows subclasses to replace the motion type
     * used in animations (currently defaults to ease-in).
     * 
     * @param start start value
     * @param destination destination value
     * @param duration duration of animation
     * @return motion object
     */
    protected Motion createAnimateMotion(int start, int destination, int duration) {
        return Motion.createEaseInMotion(start, destination, duration);
    }

    private Motion createAndStartAnimateMotion(int start, int destination, int duration) {
        Motion m = createAnimateMotion(start, destination, duration);
        m.start();
        return m;
    }
    
    private void findComponentsInHierachy(Vector vec) {
        int cc = getComponentCount();
        for(int iter = 0 ; iter < cc ; iter++) {
            Component c = getComponentAt(iter);
            vec.addElement(c);
            if(c.getClass() == Container.class) {
                ((Container)c).findComponentsInHierachy(vec);
            }
        }
    }
    
    /**
     * Morph is similar to the replace functionality where a component might be replaced with 
     * a component that isn't within the container. However, unlike the replace functionality which
     * uses a transition and assumes the position of the component (and is hence quite flexible) morph
     * can move and resize the component. E.g. after entering text into a text field and pressing submit
     * it can "morph" into a chat bubble located in a different part of the screen.<br/>
     * It is the responsibility of the caller to remove the source component (if desired) and revalidate the 
     * container when the animation completes.
     * 
     * @param source source component assumed to be within this container or one of its children
     * @param destination the destination component
     * @param duration the time the morph operation should take
     * @param onCompletion invoked when the morphing completes
     */
    public void morph(Component source, Component destination, int duration, Runnable onCompletion) {
        morph(source, destination, duration, false, onCompletion);
    }

    /**
     * Morph is similar to the replace functionality where a component might be replaced with 
     * a component that isn't within the container. However, unlike the replace functionality which
     * uses a transition and assumes the position of the component (and is hence quite flexible) morph
     * can move and resize the component. E.g. after entering text into a text field and pressing submit
     * it can "morph" into a chat bubble located in a different part of the screen.<br/>
     * It is the responsibility of the caller to remove the source component (if desired) and revalidate the 
     * container when the animation completes.
     * 
     * @param source source component assumed to be within this container or one of its children
     * @param destination the destination component
     * @param duration the time the morph operation should take
     */
    public void morphAndWait(Component source, Component destination, int duration) {
        morph(source, destination, duration, true, null);
    }

    private void morph(Component source, Component destination, int duration, boolean wait, Runnable onCompletion) {
        setShouldCalcPreferredSize(true);
        enableLayoutOnPaint = false;
        dontRecurseContainer = true;
        int deltaX = getAbsoluteX(); //- getScrollX();
        int deltaY = getAbsoluteY();// - getScrollY();
        int sourceX = source.getAbsoluteX() - deltaX;
        int destX = destination.getAbsoluteX() - deltaX;
        int sourceY = source.getAbsoluteY() - deltaY;
        int destY = destination.getAbsoluteY() - deltaY;
        final Motion[] xMotions = new Motion[] {
            createAndStartAnimateMotion(sourceX, destX, duration),
            createAndStartAnimateMotion(sourceX, destX, duration)
        };
        final Motion[] yMotions = new Motion[] {
            createAndStartAnimateMotion(sourceY, destY, duration),
            createAndStartAnimateMotion(sourceY, destY, duration)
        };
        final Motion[] wMotions = new Motion[] {
            createAndStartAnimateMotion(source.getWidth(), destination.getWidth(), duration),
            createAndStartAnimateMotion(source.getWidth(), destination.getWidth(), duration)
        };
        final Motion[] hMotions = new Motion[] {
            createAndStartAnimateMotion(source.getHeight(), destination.getHeight(), duration),
            createAndStartAnimateMotion(source.getHeight(), destination.getHeight(), duration)
        };
        Anim a = new Anim(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        a.opacity = new Motion[] {
            createAndStartAnimateMotion(255, 0, duration),
            createAndStartAnimateMotion(0, 255, duration)
        };
        a.animatedComponents = new Vector();
        a.animatedComponents.addElement(source);
        a.animatedComponents.addElement(destination);
        a.onFinish = onCompletion;
        a.dontRevalidate = true;
        a.scrollTo = destination;
        
        // animate once to prevent flickering from newly added components 
        a.animate();
        getComponentForm().registerAnimated(a);
        if(wait) {
            Display.getInstance().invokeAndBlock(a);
        }
        
    }
    
    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    private void animateHierarchy(final int duration, boolean wait, int opacity) {
        setShouldCalcPreferredSize(true);
        enableLayoutOnPaint = false;
        dontRecurseContainer = true;
        Vector comps = new Vector();
        findComponentsInHierachy(comps);
        final int componentCount = comps.size();
        int[] beforeX = new int[componentCount];
        int[] beforeY = new int[componentCount];
        int[] beforeW = new int[componentCount];
        int[] beforeH = new int[componentCount];
        final Motion[] xMotions = new Motion[componentCount];
        final Motion[] yMotions = new Motion[componentCount];
        final Motion[] wMotions = new Motion[componentCount];
        final Motion[] hMotions = new Motion[componentCount];
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = (Component)comps.elementAt(iter);
            beforeX[iter] = current.getX();
            beforeY[iter] = current.getY();
            beforeW[iter] = current.getWidth();
            beforeH[iter] = current.getHeight();
        }
        layoutContainer();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = (Component)comps.elementAt(iter);
            xMotions[iter] = createAnimateMotion(beforeX[iter], current.getX(), duration);
            yMotions[iter] = createAnimateMotion(beforeY[iter], current.getY(), duration);
            wMotions[iter] = createAnimateMotion(beforeW[iter], current.getWidth(), duration);
            hMotions[iter] = createAnimateMotion(beforeH[iter], current.getHeight(), duration);
            xMotions[iter].start();
            yMotions[iter].start();
            wMotions[iter].start();
            hMotions[iter].start();
            current.setX(beforeX[iter]);
            current.setY(beforeY[iter]);
            current.setWidth(beforeW[iter]);
            current.setHeight(beforeH[iter]);
        }
        Anim a = new Anim(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        setAnimOpacity(opacity, 255, a, componentCount, duration);
        a.animatedComponents = comps;
        getComponentForm().registerAnimated(a);
        if(wait) {
            Display.getInstance().invokeAndBlock(a);
        }
    }
    
    /**
     * This method is the exact reverse of animateLayout, when completed it leaves the container in 
     * an invalid state. It is useful to invoke this in order to remove a component, transition to a
     * different form or provide some other interaction.
     * 
     * @param duration the duration of the animation
     * @param opacity the opacity to which the layout will reach, allows fading out the components
     * @param callback if not null will be invoked when unlayouting is complete
     */
    public void animateUnlayout(final int duration, int opacity, Runnable callback) {
        animateUnlayout(duration, false, opacity, callback);
    }
    
    /**
     * This method is the exact reverse of animateLayoutAndWait, when completed it leaves the container in 
     * an invalid state. It is useful to invoke this in order to remove a component, transition to a
     * different form or provide some other interaction.
     * 
     * @param duration the duration of the animation
     * @param opacity the opacity to which the layout will reach, allows fading out the components
     */
    public void animateUnlayoutAndWait(final int duration, int opacity) {
        animateUnlayout(duration, true, opacity, null);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    private void animateUnlayout(final int duration, boolean wait, int opacity, Runnable callback) {
        setShouldCalcPreferredSize(true);
        enableLayoutOnPaint = false;
        final int componentCount = getComponentCount();
        int[] beforeX = new int[componentCount];
        int[] beforeY = new int[componentCount];
        int[] beforeW = new int[componentCount];
        int[] beforeH = new int[componentCount];
        final Motion[] xMotions = new Motion[componentCount];
        final Motion[] yMotions = new Motion[componentCount];
        final Motion[] wMotions = new Motion[componentCount];
        final Motion[] hMotions = new Motion[componentCount];
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = getComponentAt(iter);
            beforeX[iter] = current.getX();
            beforeY[iter] = current.getY();
            beforeW[iter] = current.getWidth();
            beforeH[iter] = current.getHeight();
        }
        layoutContainer();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = getComponentAt(iter);
            xMotions[iter] = createAnimateMotion(current.getX(), beforeX[iter], duration);
            yMotions[iter] = createAnimateMotion(current.getY(), beforeY[iter], duration);
            wMotions[iter] = createAnimateMotion(current.getWidth(), beforeW[iter], duration);
            hMotions[iter] = createAnimateMotion(current.getHeight(), beforeH[iter], duration);
            xMotions[iter].start();
            yMotions[iter].start();
            wMotions[iter].start();
            hMotions[iter].start();
        }
        Anim a = new Anim(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        setAnimOpacity(255, opacity, a, componentCount, duration);
        a.onFinish = callback;
        a.dontRevalidate = true;
        getComponentForm().registerAnimated(a);
        if(wait) {
            Display.getInstance().invokeAndBlock(a);
        }
    }
    
    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    private void animateLayout(final int duration, boolean wait, int opacity) {
        setShouldCalcPreferredSize(true);
        enableLayoutOnPaint = false;
        final int componentCount = getComponentCount();
        int[] beforeX = new int[componentCount];
        int[] beforeY = new int[componentCount];
        int[] beforeW = new int[componentCount];
        int[] beforeH = new int[componentCount];
        final Motion[] xMotions = new Motion[componentCount];
        final Motion[] yMotions = new Motion[componentCount];
        final Motion[] wMotions = new Motion[componentCount];
        final Motion[] hMotions = new Motion[componentCount];
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = getComponentAt(iter);
            beforeX[iter] = current.getX();
            beforeY[iter] = current.getY();
            beforeW[iter] = current.getWidth();
            beforeH[iter] = current.getHeight();
        }
        layoutContainer();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component current = getComponentAt(iter);
            xMotions[iter] = createAnimateMotion(beforeX[iter], current.getX(), duration);
            yMotions[iter] = createAnimateMotion(beforeY[iter], current.getY(), duration);
            wMotions[iter] = createAnimateMotion(beforeW[iter], current.getWidth(), duration);
            hMotions[iter] = createAnimateMotion(beforeH[iter], current.getHeight(), duration);
            xMotions[iter].start();
            yMotions[iter].start();
            wMotions[iter].start();
            hMotions[iter].start();
            current.setX(beforeX[iter]);
            current.setY(beforeY[iter]);
            current.setWidth(beforeW[iter]);
            current.setHeight(beforeH[iter]);
        }
        Anim a = new Anim(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        setAnimOpacity(opacity, 255, a, componentCount, duration);
        getComponentForm().registerAnimated(a);
        if(wait) {
            Display.getInstance().invokeAndBlock(a);
        }
    }

    private void setAnimOpacity(int source, int dest, Anim a, int componentCount, int duration) {
        if(source != dest) {
            a.opacity = new Motion[componentCount];
            for(int iter = 0 ; iter < componentCount ; iter++) {
                a.opacity[iter] = createAndStartAnimateMotion(source, dest, duration);
            }
        }        
    }
    
    static class Anim implements Animation, Runnable {
        private int animationType;
        private long startTime;
        private int duration;
        private Transition t;
        private Component current;
        private Component next;
        private boolean started = false;
        private Container thisContainer;
        private boolean finished = false;
        private Form parent;
        private Motion[][] motions;
        Runnable onFinish;
        int growSpeed;
        int layoutAnimationSpeed;
        Vector animatedComponents;
        Motion[] opacity;
        boolean dontRevalidate;
        private Component scrollTo;
        
        public Anim(Container thisContainer, int duration, Motion[][] motions) {
            startTime = System.currentTimeMillis();
            animationType = 2;
            this.duration = duration;
            this.thisContainer = thisContainer;
            this.motions = motions;
        }

        public Anim(Container thisContainer, Component current, Component next, Transition t) {
            animationType = 1;
            this.t = t;
            this.next = next;
            this.current = current;
            this.thisContainer = thisContainer;
            this.parent = thisContainer.getComponentForm();
        }

        public boolean animate() {
            switch(animationType) {
                case 2:
                    int componentCount = thisContainer.getComponentCount();
                    if(animatedComponents != null) {
                        componentCount = animatedComponents.size();
                        for(int iter = 0 ; iter < componentCount ; iter++) {
                            Component currentCmp = (Component)animatedComponents.elementAt(iter);

                            currentCmp.setX(motions[0][iter].getValue());
                            currentCmp.setY(motions[1][iter].getValue());
                            currentCmp.setWidth(motions[2][iter].getValue());
                            currentCmp.setHeight(motions[3][iter].getValue());
                            if(opacity != null) {
                                currentCmp.getStyle().setOpacity(opacity[iter].getValue(), false);
                            }
                        }
                    } else {
                        for(int iter = 0 ; iter < componentCount ; iter++) {
                            Component currentCmp = thisContainer.getComponentAt(iter);

                            // this might happen if a container was replaced during animation
                            if(currentCmp == null) {
                                continue;
                            }
                            currentCmp.setX(motions[0][iter].getValue());
                            currentCmp.setY(motions[1][iter].getValue());
                            currentCmp.setWidth(motions[2][iter].getValue());
                            currentCmp.setHeight(motions[3][iter].getValue());
                            if(opacity != null) {
                                currentCmp.getStyle().setOpacity(opacity[iter].getValue(), false);
                            }
                        }
                    }
                    if(scrollTo != null) {
                        boolean s = thisContainer.isSmoothScrolling();
                        thisContainer.setSmoothScrolling(false);
                        thisContainer.scrollComponentToVisible(scrollTo);
                        thisContainer.setSmoothScrolling(s);
                    }
                    thisContainer.repaint();
                    if(System.currentTimeMillis() - startTime >= duration) {
                        enableLayoutOnPaint = true;
                        thisContainer.dontRecurseContainer = false;
                        Form f = thisContainer.getComponentForm();
                        f.deregisterAnimated(this);
                        if(!dontRevalidate) {
                            f.revalidate();
                        }
                        synchronized(this) {
                            finished = true;
                            notify();
                        }
                    }
                    return false;

                default:
                    if (!started) {
                        t.init(current, next);
                        if(current != null) {
                            current.setLightweightMode(true);
                        }
                        if(next != null) {
                            next.setLightweightMode(true);
                        }
                        t.initTransition();
                        started = true;
                        if (thisContainer.cmpTransitions == null) {
                            thisContainer.cmpTransitions = new Vector();
                        }
                        thisContainer.cmpTransitions.addElement(this);
                    }
                    boolean notFinished = t.animate();
                    if (!notFinished) {
                        thisContainer.cmpTransitions.removeElement(this);
                        destroy();
                    }
                    return notFinished;
            }
        }

        public void destroy() {
            if(parent != null){
                parent.deregisterAnimatedInternal(this);
            }
            next.setParent(null);
            thisContainer.replace(current, next, growSpeed > 0 || layoutAnimationSpeed > 0);
            //release the events blocking
            t.cleanup();
            if(thisContainer.cmpTransitions.size() == 0 && growSpeed > -1){
                if(growSpeed > 0) {
                    current.growShrink(growSpeed);
                } else {
                    if (layoutAnimationSpeed <= 0 && !dontRevalidate) {
                        if (parent != null) {
                            parent.revalidate();
                        }
                    }
                }
            }
            synchronized(this) {
                finished = true;
                notify();
            }
            if(onFinish != null) {
                onFinish.run();
            }
        }

        public void paint(Graphics g) {
            t.paint(g);
        }

        public boolean isFinished() {
            return finished;
        }

        public void run() {
            while (!isFinished()) {
                try {
                    synchronized(this) {
                        wait(50);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

