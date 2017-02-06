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
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.Style;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

/**
 * <p>A composite pattern with {@link Component}, allows nesting and arranging multiple
 * components using a pluggable layout manager architecture. Containers can be nested
 * one within the other to form elaborate UI's. By default Containers use {@link com.codename1.ui.layouts.FlowLayout}
 * which isn't ideal for most use cases.</p>
 * <img src="https://www.codenameone.com/img/developer-guide/component-uml.png" alt="Component/Container Relationship Diagram" />
 * <p>
 * Components within the Container <b>MUST</b> be arranged using a layout manager! <br>
 * This allows the UI to adapt to different resolutions, DPI, orientation changes etc. seamlessly. Invoking any
 * bounds setting method will produce unpredictable results. To learn about layout managers check out the 
 * <a href="https://www.codenameone.com/manual/basics.html#_layout_managers">relevant section in the developer guide</a>.
 * </p>
 *<p>
 * A container doesn't implicitly reflow its elements and in that regard follows the direction of AWT/Swing. As
 * a result the layout can be animated to create a flowing effect for UI changes. This also provides improved
 * performance as a bonus. See this sample of {@code Container} animation:
 * </p>
 * <script src="https://gist.github.com/codenameone/38c076760e309c066126.js"></script>
 * 
 * <p>
 * Many components within Codename One (e.g. {@link com.codename1.ui.tree.Tree}, 
 * {@link com.codename1.ui.table.Table}, 
 * {@link com.codename1.components.MultiButton} etc.) derive from Container instead of Component. This allows
 * such components to provide very rich functionality by building on top of the existing functionality.
 * Container also provides the lead component functionality that allows treating an entire Container hierarchy
 * as a single component. This is discussed in depth within the <a href="https://www.codenameone.com/manual/misc-features.html#_lead_component">developer guide</a>.
 * </p>
 * 
 * @see com.codename1.ui.layouts
 * @see Component
 * @author Chen Fishbein
 */
public class Container extends Component implements Iterable<Component>{
    static boolean enableLayoutOnPaint = true;
    private Component leadComponent;
    private Layout layout;
    private java.util.ArrayList<Component> components = new java.util.ArrayList<Component>();
    private boolean shouldLayout = true;
    boolean scrollableX;
    boolean scrollableY;
    private java.util.Vector cmpTransitions;
    private int scrollIncrement = 20;
    private boolean blockFocus = false;
    private boolean dontRecurseContainer;
    private UIManager uiManager;

    /**
     * Workaround for the behavior of the sidemenu bar on iOS etc. which translates aggressively,
     * this is visible with the table component where the lines slide out of place
     */
    static int sidemenuBarTranslation;

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
     * Short-hand for enclosing a component within a Container 
     * @param l the layout
     * @param cmp the component to enclose
     * @param cons the constraint for the component
     * @return a newly created container containing the given component
     */
    public static Container encloseIn(Layout l, Component cmp, Object cons) {
        Container cnt = new Container(l);
        if(cons instanceof Component) {
            // this got sent to the wong method by dumb compiler...
            return cnt.add(cmp).add((Component)cons);
        }
        if(cons != null) {
            cnt.addComponent(cons, cmp);
        } else {
            cnt.addComponent(cmp);
        }
        return cnt;
    }
    
    /**
     * Short-hand for enclosing multiple components in a container typically a box layout
     * @param l the layout
     * @param cmp the components to enclose
     * @return a newly created container containing the given components
     */
    public static Container encloseIn(Layout l, Component... cmp) {
        Container cnt = new Container(l);
        for(Component c : cmp) {
            cnt.addComponent(c);
        }
        return cnt;
    }
    
    /**
     * 
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public UIManager getUIManager() {
        if(uiManager != null) {
            return uiManager;
        }
        return super.getUIManager();
    }

    /**
     * An atomic operation that wraps the current component in a Container with
     * a layered layout.  This prevents us from having to initialize and deinitialize
     * all of the components in a sub-tree because we want to re-root it.  In particular
     * Form.getLayeredPane() re-roots the entire content pane the first time it is 
     * called on a form.  If the form contains native peers there is a flicker which
     * is quite annoying.  Providing a way to do this atomically results in a better 
     * user experience.
     * @return The Container that is the new parent of this component.
     */
    Container wrapInLayeredPane() {
        final Container oldParent = getParent();
        final Container newParent = new Container(new LayeredLayout());
        final Layout parentLayout = oldParent != null && oldParent.layout != null ? oldParent.layout : null;
        final Object constraint = parentLayout != null ? parentLayout.getComponentConstraint(this) : null;
        newParent.setParent(oldParent);
        newParent.components.add(this);
        
        final Runnable r = new Runnable() {
            public void run() {
                if (parentLayout != null) {
                    parentLayout.removeLayoutComponent(Container.this);
                    parentLayout.addLayoutComponent(constraint, newParent, oldParent);
                }

                newParent.initComponentImpl();
                if (oldParent != null) {
                    int cmpIndex = -1;
                    for (int i=0; i<oldParent.getComponentCount(); i++) {
                        Component c = oldParent.getComponentAt(i);
                        if (c.equals(Container.this)) {
                            cmpIndex = i;
                            break;
                        }
                    }
                    //int cmpIndex = oldParent.getComponentIndex(Container.this);  <---  WTF... this always returns -1!!
                    if (cmpIndex == -1) {
                        throw new RuntimeException("WTF we have parent but no index!!!!");
                    }
                    oldParent.components.set(cmpIndex, newParent);
                }

                Container.this.setParent(newParent);

                newParent.revalidate();
            }
        };
        AnimationManager a = getAnimationManager();
        if(a != null && a.isAnimating()) {
            
            a.addAnimation(new ComponentAnimation() {
                @Override
                public boolean isInProgress() {
                    return false;
                }

                @Override
                protected void updateState() {
                    r.run();
                }
            });
            return newParent;
        } else {
            r.run();
            return newParent;
        }
        
    }
    
    
    /**
     * Simpler version of addComponent that allows chaining the calls for shorter syntax
     * @param cmp the component to add
     * @return this for call chaining
     */
    public Container add(Component cmp) {
        addComponent(cmp);
        return this;
    }
    
    /**
     * Simpler version of addComponent that allows chaining the calls for shorter syntax
     * @param constraint the layout constraint if applicable
     * @param cmp the component to add
     * @return this for call chaining
     */
    public Container add(Object constraint, Component cmp) {
        addComponent(constraint, cmp);
        return this;
    }
    
    /**
     * Simpler version of addComponent that allows chaining the calls for shorter syntax
     * @param label a string that will be wrapped as a label, this is equivalent to calling add(new Label(l))
     * @return this for call chaining
     */
    public Container add(String label) {
        return add(new Label(label));
    }
    
    /**
     * Simpler version of addComponent that allows chaining the calls for shorter syntax
     * @param img an image that will be wrapped as a label, this is equivalent to calling add(new Label(l))
     * @return this for call chaining
     */
    public Container add(Image img) {
        return add(new Label(img));
    }
    
    /**
     * Simpler version of addComponent that allows chaining the calls for shorter syntax
     * @param constraint the layout constraint if applicable
     * @param label a component that will be wrapped as a label, this is equivalent to calling add(new Label(l))
     * @return this for call chaining
     */
    public Container add(Object constraint, String label) {
        return add(constraint, new Label(label));
    }
    
    /**
     * Simpler version of addComponent that allows chaining the calls for shorter syntax
     * @param constraint the layout constraint if applicable
     * @param img an image that will be wrapped as a label, this is equivalent to calling add(new Label(l))
     * @return this for call chaining
     */
    public Container add(Object constraint, Image img) {
        return add(constraint, new Label(img));
    }
    
    /**
     * Allows replacing the UIManager in a component hierarchy to update the look and feel
     * only to a specific hierarchy
     * @param uiManager UIManager instance
     */
    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
    }
    
    /**
     * Sets the lead component for this container, a lead component takes over the entire
     * component hierarchy and receives all the events for the container hierarchy.
     * 
     * @param lead component that takes over the hierarchy
     */
    public void setLeadComponent(Component lead) {
        leadComponent = lead;
        if(lead == null) {
            // clear the lead component from the hierarchy
            setFocusable(false);
            hasLead = false;
            enableFocusAndDeinitLead(this);
        } else {
            if(isInitialized()) {
                initLead();
            }
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
        if(isBlockLead()) {
            return null;
        }
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
     * a container hierarchy where the parent container might not be the leader
     *
     * @return the lead component
     */
    public Container getLeadParent() {
        if(isBlockLead()) {
            return null;
        }
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
        hasLead = !isBlockLead();
    }

    /**
     * {@inheritDoc}
     */
    public void keyPressed(int k) {
        if(leadComponent != null) {
            leadComponent.keyPressed(k);
            repaint();
        }
    }

    /**
     * {@inheritDoc}
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
            cu.hasLead = !cu.isBlockLead();
        }
    }

    private void enableFocusAndDeinitLead(Container c) {
        for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
            Component cu = c.getComponentAt(iter);
            if(cu instanceof Container) {
                enableFocusAndDeinitLead((Container)cu);
            }
            cu.resetFocusable();
            cu.hasLead = false;
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
        if(layout.isConstraintTracking()) {
            for(int iter = 0 ; iter < getComponentCount() ; iter++) {
                Component c = getComponentAt(iter);
                Object cons = this.layout.getComponentConstraint(c);
                layout.addLayoutComponent(cons, c, this);
            }
        }
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
            int componentCount = components.size();
            for(int iter = 0 ; iter < componentCount ; iter++) {
                Component cmp = components.get(iter);
                if(cmp instanceof Container){
                    cmp.setShouldCalcPreferredSize(shouldCalcPreferredSize);
                }
            }
            Container parent = getParent();
            if(parent != null){
                parent.setShouldLayout(layout);
            }
        }
    }
    /**
     * {@inheritDoc}
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
            int componentCount = components.size();
            for(int iter = 0 ; iter < componentCount ; iter++) {
                Component cmp = components.get(iter);
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
     * into consideration unlike the getHeight method.
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
        insertComponentAt(Integer.MAX_VALUE, null, cmp);
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
    public void addComponent(final Object constraints, final Component cmp) {
        layout.addLayoutComponent(constraints, cmp, this);
        insertComponentAt(Integer.MAX_VALUE, null, cmp);
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
        insertComponentAt(index, constraints, cmp);
    }

    void insertComponentAt(final int index, final Object constraint, final Component cmp) {
        AnimationManager a = getAnimationManager();
        if(a != null && a.isAnimating()) {
            // pretend like the component was already added
            if(cmp.getParent() != null) {
                throw new IllegalArgumentException("Component is already contained in Container: " + cmp.getParent());
            }
            cmp.setParent(this);
            a.addAnimation(new ComponentAnimation() {
                @Override
                public boolean isInProgress() {
                    return false;
                }

                @Override
                protected void updateState() {
                    cmp.setParent(null);
                    if(constraint != null) {
                        layout.addLayoutComponent(constraint, cmp, Container.this);
                    }
                    insertComponentAtImpl(index, cmp);
                    revalidate();
                }
            });
        } else {
            if(constraint != null) {
                layout.addLayoutComponent(constraint, cmp, this);
            }
            insertComponentAtImpl(index, cmp);
        }
    }
    
    void insertComponentAtImpl(int index, final Component cmp) {
        if(index == Integer.MAX_VALUE) {
            index = components.size();
        }
        if (cmp.getParent() != null) {
            throw new IllegalArgumentException("Component is already contained in Container: " + cmp.getParent());
        }
        if(cmp instanceof Form) {
            cmp.setVisible(true);
            cmp.setPreferredSize(null);
        }
        UIManager manager = getUIManager();
        boolean refreshLaf = manager != cmp.getUIManager();
        cmp.setParent(this);
        if(refreshLaf){
           Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    cmp.refreshTheme(false);
                }
            });
        }
        components.add(index, cmp);
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
        insertComponentAt(index, null, cmp);
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
        replaceComponents(current, next, t, true, false, null, 0, 0, true);
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
        replaceComponents(current, next, t, true, false, null, 0, layoutAnimationSpeed, true);
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
        replaceComponents(current, next, t, false, false, onFinish, growSpeed, 0, true);
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
        replaceComponents(current, next, t, true, dropEvents, null, 0, 0, true);
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
        replaceComponents(current, next, t, false, false, null, 0, 0, true);
    }

    /**
     * This method creates an animation component that replaces the current Component with the next Component.
     * Current Component must be contained in this Container.
     * This method return immediately.
     * 
     * @param current a Component to remove from the Container
     * @param next a Component that replaces the current Component
     * @param t a Transition between the add and removal of the Components
     *  a Transition can be null
     * @return animation component that can be queued
     */
    public ComponentAnimation createReplaceTransition(Component current, Component next, Transition t) {
        return replaceComponents(current, next, t, false, false, null, 0, 0, false);
    }
    
    private ComponentAnimation replaceComponents(final Component current, final Component next, 
            final Transition t, boolean wait, boolean dropEvents, Runnable onFinish, int growSpeed, int layoutAnimationSpeed,
            boolean addAnimtion) {
        if (!contains(current)) {
            throw new IllegalArgumentException("Component " + current + " is not contained in this Container");
        }
        if (t == null || !isVisible() || getComponentForm() == null) {
            next.setX(current.getX());
            next.setY(current.getY());
            next.setWidth(current.getWidth());
            next.setHeight(current.getHeight());
            replace(current, next, false);
            return null;
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

        final TransitionAnimation anim = new TransitionAnimation(this, current, next, t);
        anim.growSpeed = growSpeed;
        anim.layoutAnimationSpeed = layoutAnimationSpeed;

        // register the transition animation
        /*getComponentForm().registerAnimatedInternal(anim);
        //wait until animation has finished
        if (wait) {
            Display.getInstance().invokeAndBlock(anim, dropEvents);
        }*/
        if(addAnimtion) {
            if(wait) {
                getAnimationManager().addAnimationAndBlock(anim);
            } else {
                if(onFinish != null) {
                    getAnimationManager().addAnimation(anim, onFinish);
                } else {
                    getAnimationManager().addAnimation(anim);
                }
            }
        }
        return anim;
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
        cancelRepaintsRecursively(c, Display.impl);
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
            removeComponentImplNoAnimationSafety(current);
            layout.addLayoutComponent(constraint, next, Container.this);
        } else {
            removeComponentImplNoAnimationSafety(current);
        }
        cancelRepaintsRecursively(current);
        next.setParent(null);
        if (index < 0) {
            index = 0;
        }
        insertComponentAtImpl(index, next);
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
     * {@inheritDoc}
     */
    void initComponentImpl() {
        if (!isInitialized()) {
            super.initComponentImpl();
        }
        int componentCount = components.size();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component cmp = components.get(iter);
            cmp.initComponentImpl();
        }
        if(leadComponent != null) {
            initLead();
        }
    }

    /**
     * {@inheritDoc}
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
     * Changes the component index of a child component without revalidating or animating. This is useful 
     * for complex animations or z-order manipulation but might collide with ongoing animations hence the 
     * package protected nature.
     * @param cmp The component to be moved
     * @param location The new component index
     */
    void setComponentIndex(Component cmp, int location) {
        if (location < components.size()) {
            components.remove(cmp);
            components.add(location, cmp);
        }
    }

    void removeComponentImpl(final Component cmp) {
        AnimationManager a = getAnimationManager();
        if(a != null && a.isAnimating()) {
            // pretend like the component was already removed
            layout.removeLayoutComponent(cmp);
            cmp.setParent(null);
            a.addAnimation(new ComponentAnimation() {
                @Override
                public boolean isInProgress() {
                    return false;
                }

                @Override
                protected void updateState() {
                    removeComponentImplNoAnimationSafety(cmp);
                    revalidate();
                }
            });
        } else {
            removeComponentImplNoAnimationSafety(cmp);
        }
    }
    
    /**
     * removes a Component from the Container
     * 
     * @param cmp the removed component
     */
    void removeComponentImplNoAnimationSafety(Component cmp) {
        Form parentForm = cmp.getComponentForm();
        layout.removeLayoutComponent(cmp);
        
        // the deinitizlize contract expects the component to be in a container but if this is a part of an animation 
        // it might have been removed already to prevent conflict with remove operations
        cmp.setParent(this);
        cmp.deinitializeImpl();
        components.remove(cmp);
        cmp.setParent(null);
        if (parentForm != null) {
            if (parentForm.getFocused() == cmp || cmp instanceof Container && ((Container) cmp).contains(parentForm.getFocused())) {
                parentForm.setFocused(null);
            }
            Component dragged = parentForm.getDraggedComponent();
            if(dragged == cmp){
                parentForm.setDraggedComponent(null);
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
        Display.impl.componentRemoved(cmp);
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
        int componentCount = components.size();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component cmp = components.get(iter);
            cmp.deinitializeImpl();
        }
        flushReplace();
    }

    /**
     * Flushes ongoing replace operations to prevent two concurrent replace operations from colliding.
     * If there is no ongoing replace nothing will occur
     * @deprecated this method is no longer used in the new animation framework
     */
    public void flushReplace() {
        /*if (cmpTransitions != null) {
            int size = cmpTransitions.size();
            for (int iter = 0; iter < size; iter++) {
                ((Anim) cmpTransitions.elementAt(iter)).destroy();
            }
            cmpTransitions.removeAllElements();
            cmpTransitions = null;
        }*/
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
        
        // prevents concurrent modification exception
        Component[] arr = new Component[components.size()];
        components.toArray(arr);

        int componentCount = arr.length;
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component cmp = arr[iter];
            removeComponent(cmp);
        }
        resetScroll();
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
            
            // for complex hierarchies 
            if(getParent() != null) {
                getParent().layoutContainer();
            } else {
                layoutContainer();
            }
        } else {
            layoutContainer();
            repaint();
        }
    }
    
    /**
     * A more powerful form of revalidate that recursively lays out the full hierarchy
     */
    public void forceRevalidate() {
        forceRevalidateImpl();
        revalidate();
    }

    private void forceRevalidateImpl() {
        setShouldCalcPreferredSize(true);
        int c = getComponentCount();
        for(int iter = 0 ; iter < c ; iter++) {
            Component cmp = getComponentAt(iter);
            if(cmp instanceof Container) {
                ((Container)cmp).forceRevalidateImpl();
            } else {
                cmp.setShouldCalcPreferredSize(true);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void clearClientProperties(){
        super.clearClientProperties();
        int c = getComponentCount();
        for(int iter = 0 ; iter < c ; iter++) {
            Component cmp = getComponentAt(iter);
            cmp.clearClientProperties();
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

    static boolean blockOverdraw = false;
    
    /**
     * Invoked internally to indicate if child components are hiding this container
     * thus removing the need to invoke its own paint methods
     * @return true if child components are obscuring this component
     */
    boolean isObscuredByChildren() {
        if(!blockOverdraw) {
            return false;
        }
        if(!getLayout().obscuresPotential(this)) {
            return false;
        }
        Style s = getStyle();
        if(s.getPaddingTop() != 0 || s.getPaddingLeftNoRTL()!= 0 || s.getPaddingRightNoRTL()!= 0 || s.getPaddingBottom() != 0) {
            return false;
        }
        
        int size = components.size();
        for(int iter = 0 ; iter < size ; iter++) {
            Component cmp = components.get(iter);
            s = cmp.getStyle();
            if(cmp.getWidth() == 0 || cmp.getHeight() == 0) {
                continue;
            }
            // need to think of a better way, this means we invoke the same logic recurisvely again and again by a factor of depth. Not good...
            if(cmp instanceof Container) {
                if(!((Container)cmp).getLayout().obscuresPotential(this)) {
                    return false;
                }
                if(s.getOpacity() != 0xff || s.getMarginTop() != 0 || s.getMarginLeftNoRTL() != 0 || s.getMarginRightNoRTL() != 0 || s.getMarginBottom()!= 0) {
                    return false;
                }
                if((s.getBgTransparency() & 0xff) != 0xff && !((Container)cmp).isObscuredByChildren()) {
                    return false;
                }
            } else {
                if((s.getBgTransparency() & 0xff) != 0xff || s.getOpacity() != 0xff || s.getMarginTop()!= 0 || s.getMarginLeftNoRTL()!= 0 || s.getMarginRightNoRTL()!= 0 || s.getMarginBottom()!= 0) {
                    return false;
                }
            }
        }        
        
        return true;
    }
    
    
    /**
     * Efficiently finds the first child component that is visible in the specified 
     * bounds.  
     * <p>This is only really helpful if the child components are sorted
     * in some way so that we can quickly (with a binary search) find the first
     * visible component.  E.g. In BoxLayout.Y_AXIS, the components are arranged 
     * vertically in order of their index so we can use a binary search to find
     * the first visible element.  For most other layout managers we can't as easily
     * do a sort like this.</p>
     * 
     * <p>If the layout manager doesn't allow for a binary search, then this will
     * just return 0 (meaning that you need to scan the children from the beginning
     * to find visible children).</p>
     * 
     * <p>After you obtain this value, use the {@link #calculateLastPaintableOffset(int, int, int, int, int) } method
     * to get the end of the visible region.</p>
     * 
     * <p>The motivation for this is to try to improve performance in places where the container
     * has many (say 2500) children, and most of them aren't actually visible.</p>
     * 
     * @param clipX1 Left bounds of region to check.  (0,0) is the top left corner of this component.
     * @param clipY1 Top bounds of region to check.  (0,0) is top left corner of this component.
     * @param clipX2 Right bounds of region to check.  (0,0) is top left corner of this component.
     * @param clipY2 Bottom bounds of region to check.  (0,0) is top left corner of this component.
     * @return The index within the "components" array where the first child that intersects the provided
     * clip occurs, or -1 if there is no "fast" way to find it.  If there was a fast way to do it, but no visible
     * components were found, then this will return components.size().
     * 
     * @see #calculateLastPaintableOffset(int, int, int, int, int) 
     */
    private int calculateFirstPaintableOffset(int clipX1, int clipY1, int clipX2, int clipY2) {
        int len = components.size();
        Layout l = getLayout();
        if (l.getClass() == BoxLayout.class) {
            if (((BoxLayout)l).getAxis() == BoxLayout.Y_AXIS) {
                // Use a binary search to find the first visible
                int startPos = binarySearchFirstIntersectionY(clipY1, clipY2, 0, len);
                if (startPos >= 0) {
                    return startPos;
                } else {
                    return len;
                }
                
            }
        }
        return -1;
    }
    
    
    /**
     * Gets the index of the "last" child component that intersects the given rectangle.  This is
     * only helpful if the components are sorted (e.g. with BoxLayout.Y_AXIS).  If they aren't
     * sorted then this will just return components.size()-1.
     * @param pos The starting position to search.  It is assumed that this starting
     * position is in the visible region.
     * @param clipX1 The left bounds of the region to search.  (0,0) is the top left corner of the container.
     * @param clipY1 The top bounds of the region to search. (0,0) is the top left corner of the container.
     * @param clipX2 The right bounds of the region to search. (0,0) is the top left corner of the container.
     * @param clipY2 The bottom bounds of the region to search. (0,0) is the top left corner of the container.
     * @return The index of the last visible component in this container - or components.size()-1 
     */
    private int calculateLastPaintableOffset(int pos, int clipX1, int clipY1, int clipX2, int clipY2) {
        final int len = components.size();
        if (pos >= len-1) {
            // Start position is after the last index, so we didn't
            // even find an end offset.
            // Let's return one less than pos to indicate this
            return len-1;
        }
        final Layout l = getLayout();
        if (l.getClass() == BoxLayout.class) {
            if (((BoxLayout)l).getAxis() == BoxLayout.Y_AXIS) {
                // Use a binary search to find the first visible
                //Component c = components.get(++pos);
                Component c = null;
                int cy1 = -1;
                final int end = len-1;
                pos++; // This should still be a valid index because
                        // we previously checked to see if it was >= len-1
                do {
                    c = components.get(pos);
                    cy1 = c.getBounds().getY(); 
                } while (++pos <= end && cy1 <= clipY2);
               return pos-1;
            }
        }
        return len-1;
    }
    
    /**
     * Performs a binary search within the children of the container to find components
     * that intersect the given range on the y-axis.  <b>This should only be used
     * if it is known that the child components are sorted by their y coordinates
     * in ascending order.  Otherwise you'll get undefined results.</b>
     * @param y1 The lower y-bound of the region to search.  (0,0) is top-left corner of container.
     * @param y2 The upper y-bound of the region to search.  (0,0) is top-left corner of container.
     * @param start The lower "index" to search.
     * @param end The upper "index" to search.
     * @return The index within the components array of the first child component
     * that intersects the given region.  Or -1 if none is found.
     */
    private int binarySearchFirstIntersectionY(int y1, int y2, int start, int end) {
        if (start >= end) {
            return -1;
        }
        int pos = (start + end) /2;
        Component c = components.get(pos);
        Rectangle bounds = c.getBounds();
        int cy1 = bounds.getY();
        
        int cy2 = bounds.getY() + bounds.getHeight();
        if ((cy1 >= y1 && cy1<= y2)||(cy2>=y1 && cy2 <=y2)||(cy1<=y1 && cy2>=y2)) {
            // We have a hit let's roll backward until we find the first visible
            while (pos > start && cy1 > y1) {
                c = components.get(--pos);
                cy1 = c.getBounds().getY();
            }
            return pos;
        } else if (cy1 > y2) {
            return binarySearchFirstIntersectionY(y1, y2, start, pos);
        } else {
            return binarySearchFirstIntersectionY(y1, y2, pos+1, end);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        if(enableLayoutOnPaint) {
            layoutContainer();
        }
        g.translate(getX(), getY());
        int size = components.size();
        int startIter = 0;
        if (size >= 30) {
            int clipX1 = g.getClipX();
            int clipX2 = g.getClipX() + g.getClipWidth();
            int clipY1 = g.getClipY();
            int clipY2 = g.getClipY() + g.getClipHeight();
            startIter = calculateFirstPaintableOffset(clipX1, clipY1, clipX2, clipY2);
            if (startIter < 0) {
                // There was no efficient way to calculate the offset
                startIter = 0;
            } else if (startIter < size){
                // There was an efficient way to calculate the offset so we
                // will continue this approach
                size = calculateLastPaintableOffset(startIter, clipX1, clipY1, clipX2, clipY2)+1;
            }
        }
        CodenameOneImplementation impl = Display.impl;
        if(dontRecurseContainer) {
            for(int iter = startIter ; iter < size ; iter++) {
                Component cmp = components.get(iter);
                if(cmp.getClass() == Container.class) {
                    paintContainerChildrenForAnimation((Container)cmp, g);
                } else {
                    cmp.paintInternal(impl.getComponentScreenGraphics(this, g), false);
                }
            }
        } else {
            for(int iter = startIter ; iter < size ; iter++) {
                Component cmp = components.get(iter);
                cmp.paintInternal(impl.getComponentScreenGraphics(this, g), false);
            }
        }
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        g.translate(-tx, -ty);
        if(sidemenuBarTranslation > 0) {
            g.translate(sidemenuBarTranslation, 0);
            paintGlass(g);
            paintTensile(g);
            g.translate(-sidemenuBarTranslation, 0);
        } else {
            paintGlass(g);
            paintTensile(g);
        }
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
    protected void paintGlass(Graphics g) {
    }
    
    void paintGlassImpl(Graphics g) {    
        super.paintGlassImpl(g);
        paintGlass(g);
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

            int size = components.size();
            for (int i = startIndex; i < endIndex; i++) {
                Component cmp2 = (Component) components.get(i);
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
        if(Form.activePeerCount > 0) {
            onParentPositionChange();
        }
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
        return components.get(index);
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
        if (cmp == null) {
            return false;
        }
        cmp = cmp.getParent();
        while (cmp != null) {
            if (cmp == this) {
                return true;
            }
            cmp = cmp.getParent();
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
                    if (f != null && f.getInvisibleAreaUnderVKB() == 0 && 
                            f.findFirstFocusable() == c) {
                        // support this use case only if the component doesn't explicitly declare visible bounds
                        if (r == c.getBounds() && !Display.getInstance().isTouchScreenDevice()) {
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

    private int distanceToComponent(Component c, int x, int y) {
        int cx = c.getX();
        if(x > cx) {
            cx += c.getWidth();
            if(cx > x) {
                cx = x;
            }
        }
        int cy = c.getY();
        if(y > cy) {
            cy += c.getHeight();
            if(cy > y) {
                cy = y;
            }
        }
        x = Math.abs(cx - x);
        y = Math.abs(cy - y);
        return (int)Math.sqrt(x*x+y*y);
    }
    
    /**
     * Very useful for touch events or drop events that need approximation more than accuracy
     * @param x location in container relative coordinates
     * @param y location in container relative coordinates
     * @return the closest component in the container or null if no component is in the container
     */
    public Component getClosestComponentTo(int x, int y) {
        int count = getComponentCount();
        if(count == 0) {
            return null;
        }
        Component closest = getComponentAt(0);
        if(closest.contains(x, y)) {
            return closest;
        }
        int distance = distanceToComponent(closest, x, y);
        for(int iter = 1 ; iter < count ; iter++) {
            Component current = getComponentAt(iter);
            if(current.contains(x, y)) {
                return current;
            }
            int cd = distanceToComponent(current, x, y);
            if(cd < distance) {
                closest = current;
                distance = cd;
            }
        }
        return closest;
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
        
        int startIter = 0;
        int count = getComponentCount();
        if (count > 30) {
            int relx = x - getAbsoluteX();
            int rely = y - getAbsoluteY();
            
            startIter = calculateFirstPaintableOffset(relx, rely, relx, rely);
            if (startIter < 0) {
                // There was no efficient way to calculate the first paintable offset
                // start counting from 0
                startIter = 0;
            } else if (startIter < count) {
                // We found a start offset using an efficient method
                // Find an appropriate end offset.
                count = calculateLastPaintableOffset(startIter, relx, rely, relx, rely) + 1;
            }
        }
        boolean overlaps = getLayout().isOverlapSupported();
        Component component = null;
        for (int i = count - 1; i >= startIter; i--) {
            Component cmp = getComponentAt(i);
            if (cmp.contains(x, y)) {
                component = cmp;
                if (!overlaps && component.isFocusable()) {
                    // special case for lead blocking
                    if(component instanceof Container && ((Container)component).getLeadParent() == component) {
                        Component c = ((Container)component).getComponentAt(x, y);
                        if(c.isBlockLead()) {
                            return c;
                        }
                    }
                    return component;
                }
                if (cmp instanceof Container) {
                    Component c = ((Container) cmp).getComponentAt(x, y);
                    if(c != null){
                        component = c;
                    }
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

    /**
     * Recursively searches the container hierarchy for a drop target
     * 
     * @param x position in which we are searching for a drop target
     * @param y position in which we are searching for a drop target
     * @return a drop target or null if no drop target could be found at the x/y position
     */
    public Component findDropTargetAt(int x, int y) {
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
     * {@inheritDoc}
     */
    public void pointerPressed(int x, int y) {
        clearDrag();
        setDragActivated(false);
        Component cmp = getComponentAt(x, y);
        if (cmp == this) {
            super.pointerPressed(x, y);
            return;
        }
        if (cmp != null) {
            //give priority to focusable components contained in the Container
            if(cmp.isFocusable() || cmp.isGrabsPointerEvents()){
                cmp.pointerPressed(x, y);
                return;
            }
            if(isFocusable() || isGrabsPointerEvents()){
                super.pointerPressed(x, y);
                return;                
            }
            cmp.pointerPressed(x, y);            
        }
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
        int componentCount = components.size();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component cmp = components.get(iter);
            String className = cmp.getClass().getName();
            ret += className.substring(className.lastIndexOf('.') + 1) + ", ";
        }
        if (ret.length() > 1) {
            ret = ret.substring(0, ret.length() - 2);
        }
        ret = ret + "]";
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        int componentCount = components.size();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component cmp = components.get(iter);
            cmp.refreshTheme(merge);
        }
    }

    boolean scrollableXFlag() {
        return scrollableX;
    }

    boolean scrollableYFlag() {
        return scrollableY;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isScrollableX() {
        return scrollableX && (getScrollDimension().getWidth() + getStyle().getHorizontalPadding() > getWidth());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isScrollableY() {
        Form f = getComponentForm();
        int v = 0;
        if(f != null) {
            v= f.getInvisibleAreaUnderVKB();
        }
        return scrollableY && (getScrollDimension().getHeight() + getStyle().getVerticalPadding() > getHeight() -  v || isAlwaysTensile());
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * 
     * @deprecated use setScrollableX and setScrollableY instead. This method is deprecated since it breeds confusion and is often misunderstood.
     */
    public void setScrollable(boolean scrollable) {
        setScrollableX(scrollable);
        setScrollableY(scrollable);
    }

    /**
     * {@inheritDoc}
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
                if (current instanceof Container && !((Container)current).isBlockFocus() && ((Container)current).getLeadComponent() == null) {
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
     * {@inheritDoc}
     */
    protected void dragInitiated() {
        super.dragInitiated();
        if(leadComponent != null) {
            leadComponent.dragInitiated();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void fireClicked() {
        if(leadComponent != null) {
            leadComponent.fireClicked();
        } else {
            super.fireClicked();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isSelectableInteraction() {
        if(leadComponent != null) {
            return leadComponent.isSelectableInteraction();
        } else {
            return super.isSelectableInteraction();
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
     * {@inheritDoc}
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
        return !isObscuredByChildren();
    }

    /**
     * {@inheritDoc}
     */
    public void paintComponentBackground(Graphics g) {
        if(isFlatten()) {
            super.paintBackgrounds(g);
            return;
        }

        if(shouldPaintContainerBackground()) {
            super.paintComponentBackground(g);
        } 
    }

    /**
     * {@inheritDoc}
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
        animateHierarchy(duration, true, 255, true);
    }

    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation. 
     *
     * @param duration the duration in milliseconds for the animation
     * @return the animation object that should be added to the animation manager
     */
    public ComponentAnimation createAnimateHierarchy(final int duration) {
        return animateHierarchy(duration, false, 255, false);
    }
    
    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateHierarchy(final int duration) {
        animateHierarchy(duration, false, 255, true);
    }

    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation. This method waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateHierarchyFadeAndWait(final int duration, int startingOpacity) {
        animateHierarchy(duration, true, startingOpacity, true);
    }

    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation. 
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     * @return the animation object that should be added to the animation manager
     */
    public ComponentAnimation createAnimateHierarchyFade(final int duration, int startingOpacity) {
        return animateHierarchy(duration, false, startingOpacity, false);
    }
    
    /**
     * Animates a pending hierarchy of components into place, this effectively replaces revalidate with 
     * a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateHierarchyFade(final int duration, int startingOpacity) {
        animateHierarchy(duration, false, startingOpacity, true);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation. This method
     * waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateLayoutFadeAndWait(final int duration, int startingOpacity) {
        animateLayout(duration, true, startingOpacity, true);
    }
    
    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation. This method
     * waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     * @return the animation object that should be added to the animation manager
     * @deprecated this was added by mistake!
     */
    public ComponentAnimation createAnimateLayoutFadeAndWait(final int duration, int startingOpacity) {
        return null;
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     */
    public void animateLayoutFade(final int duration, int startingOpacity) {
        animateLayout(duration, false, startingOpacity, true);
    }
    
    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     * @param startingOpacity the initial opacity to give to the animated components
     * @return the animation object that should be added to the animation manager
     */
    public ComponentAnimation createAnimateLayoutFade(final int duration, int startingOpacity) {
        return animateLayout(duration, false, startingOpacity, false);
    }

    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation. This method
     * waits until the operation is completed before returning
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateLayoutAndWait(final int duration) {
        animateLayout(duration, true, 255, true);
    }

    /**
     * <p>
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation<br>
     * See: 
     * </p>
     * 
     * <script src="https://gist.github.com/codenameone/38c076760e309c066126.js"></script>
     *
     * @param duration the duration in milliseconds for the animation
     */
    public void animateLayout(final int duration) {
        animateLayout(duration, false, 255, true);
    }

    /**
     * <p>
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation<br>
     * See: 
     * </p>
     * 
     * <script src="https://gist.github.com/codenameone/38c076760e309c066126.js"></script>
     *
     * @param duration the duration in milliseconds for the animation
     * @return the animation object that should be added to the animation manager
     */
    public ComponentAnimation createAnimateLayout(final int duration) {
        return animateLayout(duration, false, 255, false);
    }
    
    /**
     * {@inheritDoc}
     */
    public void drop(Component dragged, int x, int y) {
        int i = getComponentIndex(dragged);
        if(i > -1) {
            Component dest = getComponentAt(x, y);
            if(dest != dragged) {
                int destIndex = getComponentIndex(dest);
                if(destIndex > -1 && destIndex != i) {
                	setComponentIndex(dragged,destIndex);
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
     * it can "morph" into a chat bubble located in a different part of the screen.<br>
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
     * it can "morph" into a chat bubble located in a different part of the screen.<br>
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
        int deltaX = getAbsoluteX();
        int deltaY = getAbsoluteY();
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
        MorphAnimation a = new MorphAnimation(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        a.opacity = new Motion[] {
            createAndStartAnimateMotion(255, 0, duration),
            createAndStartAnimateMotion(0, 255, duration)
        };
        a.animatedComponents = new Vector();
        a.animatedComponents.addElement(source);
        a.animatedComponents.addElement(destination);
        a.dontRevalidate = true;
        a.scrollTo = destination;
        
        if(wait) {
            getAnimationManager().addAnimationAndBlock(a);
        } else {
            if(onCompletion != null) {
                getAnimationManager().addAnimation(a, onCompletion);
            } else {
                getAnimationManager().addAnimation(a);
            }
        }
    }
    
    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    private ComponentAnimation animateHierarchy(final int duration, boolean wait, int opacity, boolean add) {
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
        MorphAnimation a = new MorphAnimation(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        setAnimOpacity(opacity, 255, a, componentCount, duration);
        a.animatedComponents = comps;
        if(add) {
            if(wait) {
                getAnimationManager().addAnimationAndBlock(a);
            } else {
                getAnimationManager().addAnimation(a);
            }
        }
        return a;
    }
    
    /**
     * <p>This method is the exact reverse of animateLayout, when completed it leaves the container in 
     * an invalid state. It is useful to invoke this in order to remove a component, transition to a
     * different form or provide some other interaction. E.g.:</p>
     * <script src="https://gist.github.com/codenameone/ba6fdc5f841b083e13e9.js"></script>
     * 
     * @param duration the duration of the animation
     * @param opacity the opacity to which the layout will reach, allows fading out the components
     * @param callback if not null will be invoked when unlayouting is complete
     */
    public void animateUnlayout(final int duration, int opacity, Runnable callback) {
        animateUnlayout(duration, false, opacity, callback);
    }
    
    /**
     * <p>This method is the exact reverse of animateLayoutAndWait, when completed it leaves the container in 
     * an invalid state. It is useful to invoke this in order to remove a component, transition to a
     * different form or provide some other interaction. E.g.:</p>
     * <script src="https://gist.github.com/codenameone/ba6fdc5f841b083e13e9.js"></script>
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
        MorphAnimation a = new MorphAnimation(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        setAnimOpacity(255, opacity, a, componentCount, duration);
        a.dontRevalidate = true;
        if(wait) {
            getAnimationManager().addAnimationAndBlock(a);
        } else {
            if(callback != null) {
                getAnimationManager().addAnimation(a, callback);
            } else {
                getAnimationManager().addAnimation(a);
            }
        }
    }
    
    /**
     * Animates a pending layout into place, this effectively replaces revalidate with a more visual form of animation
     *
     * @param duration the duration in milliseconds for the animation
     */
    private ComponentAnimation animateLayout(final int duration, boolean wait, int opacity, boolean addAnimation) {
        // this happens for some reason
        Form f = getComponentForm();
        if(f == null) {
            return null;
        }
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
        MorphAnimation a = new MorphAnimation(this, duration, new Motion[][] {
            xMotions, yMotions, wMotions, hMotions
        });
        setAnimOpacity(opacity, 255, a, componentCount, duration);
        if(addAnimation) {
            if(wait) {
                getAnimationManager().addAnimationAndBlock(a);
            } else {
                getAnimationManager().addAnimation(a);
            }
        } else {
            a.dontRevalidate = true;
        }
        return a;
    }

    private void setAnimOpacity(int source, int dest, MorphAnimation a, int componentCount, int duration) {
        if(source != dest) {
            a.opacity = new Motion[componentCount];
            for(int iter = 0 ; iter < componentCount ; iter++) {
                a.opacity[iter] = createAndStartAnimateMotion(source, dest, duration);
            }
        }        
    }

    /**
     * Part of the Iterable interface allowing us to do a for-each loop on Container
     * @return the iterator of the components
     */
    public Iterator<Component> iterator() {
        return components.iterator();
    }

    static class TransitionAnimation extends ComponentAnimation {
        private Transition t;
        private Container thisContainer;
        int growSpeed;
        int layoutAnimationSpeed;
        Vector animatedComponents;
        Motion[] opacity;
        boolean dontRevalidate;
        private boolean started = false;
        private boolean inProgress = true;
        private Component current;
        private Component next;
        private Form parent;
        private boolean destroyed;

        TransitionAnimation(Container thisContainer, Component current, Component next, Transition t) {
            this.t = t;
            this.next = next;
            this.current = current;
            this.thisContainer = thisContainer;
            this.parent = thisContainer.getComponentForm();
        }
        
        public boolean isInProgress() {
            return inProgress;
        }

        public void updateState() {
            if(destroyed) {
                return;
            }
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
            inProgress = t.animate();
            if (!inProgress) {
                thisContainer.cmpTransitions.removeElement(this);
                destroy();
                thisContainer.repaint();
            } else {
                Display.getInstance().repaint(t);
            }
        }        

        @Override
        public void flush() {
            destroy();
        }
        
        public void destroy() {
            if(destroyed) {
                return;
            }
            destroyed = true;
            next.setParent(null);
            thisContainer.replace(current, next, growSpeed > 0 || layoutAnimationSpeed > 0);
            //release the events blocking
            t.cleanup();
            if(thisContainer.cmpTransitions != null && thisContainer.cmpTransitions.size() == 0 && growSpeed > -1){
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
            inProgress = false;
        }
    }
    
    static class MorphAnimation extends ComponentAnimation {
        private long startTime;
        private int duration;
        private Transition t;
        private Container thisContainer;
        private boolean finished = false;
        private Motion[][] motions;
        Runnable onFinish;
        int growSpeed;
        int layoutAnimationSpeed;
        Vector animatedComponents;
        Motion[] opacity;
        boolean dontRevalidate;
        private Component scrollTo;

        public MorphAnimation(Container thisContainer, int duration, Motion[][] motions) {
            startTime = System.currentTimeMillis();
            this.duration = duration;
            if(Motion.isSlowMotion()) {
                this.duration *= 50;
            }
            this.thisContainer = thisContainer;
            this.motions = motions;
        }

        @Override
        public boolean isInProgress() {
            return !finished;
        }

        @Override
        public void flush() {
            for(Motion[] mm : motions) {
                for(Motion m : mm) {
                    if(m != null) {
                        m.finish();
                    }
                }
            }
            updateState();
        }
        
        @Override
        protected void updateState() {
            if(animatedComponents != null) {
                int componentCount = animatedComponents.size();
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
                int componentCount = thisContainer.getComponentCount();
                if(motions != null){
                    componentCount = Math.min(motions[0].length, componentCount);
                }
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
                finished = true;
                if(f == null) {
                    return;
                }
                if(!dontRevalidate) {
                    f.revalidate();
                }
            }
        }
        
    }
}

