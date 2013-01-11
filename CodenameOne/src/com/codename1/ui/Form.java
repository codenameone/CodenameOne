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
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Top level component that serves as the root for the UI, this {@link Container}
 * handles the menus and title while placing content between them. By default a 
 * forms central content (the content pane) is scrollable.
 *
 * Form contains Title bar, MenuBar and a ContentPane.
 * Calling to addComponent on the Form is delegated to the contenPane.addComponent
 * 
 *<pre>
 *
 *       **************************
 *       *         Title          *
 *       **************************
 *       *                        *
 *       *                        *
 *       *      ContentPane       *
 *       *                        *
 *       *                        *
 *       **************************
 *       *         MenuBar        *
 *       **************************
 *</pre> 
 * @author Chen Fishbein
 */
public class Form extends Container {

    private Painter glassPane;
    private Container contentPane = new Container(new FlowLayout());
    Container titleArea = new Container(new BorderLayout());
    private Label title = new Label("", "Title");
    private MenuBar menuBar;
    private Component dragged;
    Vector buttonsAwatingRelease;
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
    private Vector internalAnimatableComponents;
    /**
     * Contains a list of components that would like to animate their state
     */
    private Vector animatableComponents;
    //private FormSwitcher formSwitcher;
    private Component focused;
    private Vector mediaComponents;
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
    private EventDispatcher pointerPressedListeners;
    private EventDispatcher pointerReleasedListeners;
    private EventDispatcher pointerDraggedListeners;
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
    private Hashtable keyListeners;
    /**
     * Listeners for game key release events 
     */
    private Hashtable gameKeyListeners;
    /**
     * Indicates whether focus should cycle within the form
     */
    private boolean cyclicFocus = true;
    private int tactileTouchDuration;
    private EventDispatcher showListener;
    int initialPressX;
    int initialPressY;
    private EventDispatcher orientationListener;
    private UIManager uiManager;

    /**
     * Default constructor creates a simple form
     */
    public Form() {
        super(new BorderLayout());
        setUIID("Form");
        // forms/dialogs are not visible by default
        setVisible(false);
        Style formStyle = getStyle();
        int w = Display.getInstance().getDisplayWidth() - (formStyle.getMargin(isRTL(), Component.LEFT) + formStyle.getMargin(isRTL(), Component.RIGHT));
        int h = Display.getInstance().getDisplayHeight() - (formStyle.getMargin(false, Component.TOP) + formStyle.getMargin(false, Component.BOTTOM));

        setWidth(w);
        setHeight(h);
        setPreferredSize(new Dimension(w, h));
        super.setAlwaysTensile(false);

        title.setEndsWith3Points(false);
        titleArea.addComponent(BorderLayout.CENTER, title);
        titleArea.setUIID("TitleArea");
        addComponentToForm(BorderLayout.NORTH, titleArea);
        addComponentToForm(BorderLayout.CENTER, contentPane);

        contentPane.setUIID("ContentPane");
        contentPane.setScrollableY(true);

        if (title.getText() != null && title.shouldTickerStart()) {
            title.startTicker(getUIManager().getLookAndFeel().getTickerSpeed(), true);
        }

        // hardcoded, anything else is just pointless...
        formStyle.setBgTransparency(0xFF);
    }

    /**
     * @inheritDoc
     */
    public boolean isAlwaysTensile() {
        return getContentPane().isAlwaysTensile();
    }

    /**
     * @inheritDoc
     */
    public void setAlwaysTensile(boolean alwaysTensile) {
        getContentPane().setAlwaysTensile(alwaysTensile);
    }

    /**
     * Title area manipulation might break with future changes to Codename One and might 
     * damage themeing/functionality of the Codename One application in some platforms
     * 
     * @return the container containing the title
     */
    public Container getTitleArea() {
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
        sizeChanged(w, h);
        Style formStyle = getStyle();
        w = w - (formStyle.getMargin(isRTL(), Component.LEFT) + formStyle.getMargin(isRTL(), Component.RIGHT));
        h = h - (formStyle.getMargin(false, Component.TOP) + formStyle.getMargin(false, Component.BOTTOM));
        setSize(new Dimension(w, h));
        setShouldCalcPreferredSize(true);
        doLayout();
        focused = getFocused();
        if (focused != null) {
            Component.setDisableSmoothScrolling(true);
            scrollComponentToVisible(focused);
            Component.setDisableSmoothScrolling(false);
        }
        if (orientationListener != null) {
            orientationListener.fireActionEvent(new ActionEvent(this));
        }
        repaint();
    }

    /**
     * Allows a developer that doesn't derive from the form to draw on top of the 
     * form regardless of underlying changes or animations. This is useful for
     * watermarks or special effects (such as tinting) it is also useful for generic
     * drawing of validation errors etc... A glass pane is generally 
     * transparent or translucent and allows the the UI bellow to be seen.
     * 
     * @param glassPane a new glass pane to install. It is generally recommended to
     * use a painter chain if more than one painter is required.
     */
    public void setGlassPane(Painter glassPane) {
        this.glassPane = glassPane;
        repaint();
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
     * Allows a developer that doesn't derive from the form to draw on top of the 
     * form regardless of underlying changes or animations. This is useful for
     * watermarks or special effects (such as tinting) it is also useful for generic
     * drawing of validation errors etc... A glass pane is generally 
     * transparent or translucent and allows the the UI bellow to be seen.
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
            keyListeners = new Hashtable();
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

    private void addKeyListener(int keyCode, ActionListener listener, Hashtable keyListeners) {
        if (keyListeners == null) {
            keyListeners = new Hashtable();
        }
        Integer code = new Integer(keyCode);
        Vector vec = (Vector) keyListeners.get(code);
        if (vec == null) {
            vec = new Vector();
            vec.addElement(listener);
            keyListeners.put(code, vec);
            return;
        }
        if (!vec.contains(listener)) {
            vec.addElement(listener);
        }
    }

    private void removeKeyListener(int keyCode, ActionListener listener, Hashtable keyListeners) {
        if (keyListeners == null) {
            return;
        }
        Integer code = new Integer(keyCode);
        Vector vec = (Vector) keyListeners.get(code);
        if (vec == null) {
            return;
        }
        vec.removeElement(listener);
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
            gameKeyListeners = new Hashtable();
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
     * @inheritDoc
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
                ex.printStackTrace();
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
     * Returns true if the given dest component is in the column of the source component
     */
    private boolean isInSameColumn(Component source, Component dest) {
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
     * This method returns the Content pane instance
     * 
     * @return a content pane instance
     */
    public Container getContentPane() {
        return contentPane;
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
     * @inheritDoc
     */
    public void setLayout(Layout layout) {
        contentPane.setLayout(layout);
    }

    void updateIcsIconCommandBehavior() {
        int b = Display.getInstance().getCommandBehavior();
        if (b == Display.COMMAND_BEHAVIOR_ICS) {
            if (getTitleComponent().getIcon() == null) {
                Image i = Display.getInstance().getImplementation().getApplicationIconImage();
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
        this.title.setText(title);

        if (!Display.getInstance().isNativeTitle()) {
            updateIcsIconCommandBehavior(); 
            if (isInitialized() && this.title.isTickerEnabled()) {
                int b = Display.getInstance().getCommandBehavior();
                if (b == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK || b == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                        || b == Display.COMMAND_BEHAVIOR_ICS) {
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
        }
    }

    /**
     * Returns the Form title text
     * 
     * @return returns the form title
     */
    public String getTitle() {
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
     * @inheritDoc
     */
    public void addComponent(Object constraints, Component cmp) {
        contentPane.addComponent(constraints, cmp);
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     */
    public void replace(Component current, Component next, Transition t) {
        contentPane.replace(current, next, t);
    }

    /**
     * @inheritDoc
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

    void addComponentToForm(Object constraints, Component cmp) {
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
            mediaComponents = new Vector();
        }
        if (!mediaComponents.contains(mediaCmp)) {
            mediaComponents.addElement(mediaCmp);
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
        mediaComponents.removeElement(mediaCmp);
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
            animatableComponents = new Vector();
        }
        if (!animatableComponents.contains(cmp)) {
            animatableComponents.addElement(cmp);
        }
        Display.getInstance().notifyDisplay();
    }

    /**
     * Identical to the none-internal version, the difference between the internal/none-internal
     * is that it references a different vector that is unaffected by the user actions.
     * That is why we can dynamically register/deregister without interfearing with user interaction.
     */
    void registerAnimatedInternal(Animation cmp) {
        if (internalAnimatableComponents == null) {
            internalAnimatableComponents = new Vector();
        }
        if (!internalAnimatableComponents.contains(cmp)) {
            internalAnimatableComponents.addElement(cmp);
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
            internalAnimatableComponents.removeElement(cmp);
        }
    }

    /**
     * Indicate that cmp would no longer like to receive animation events
     * 
     * @param cmp component that would no longer receive animation events
     */
    public void deregisterAnimated(Animation cmp) {
        if (animatableComponents != null) {
            animatableComponents.removeElement(cmp);
        }
    }

    /**
     * @inheritDoc
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
        if (animatableComponents != null) {
            loopAnimations(animatableComponents, null);
        }
        if (internalAnimatableComponents != null) {
            loopAnimations(internalAnimatableComponents, animatableComponents);
        }
    }

    private void loopAnimations(Vector v, Vector notIn) {
        // we don't save size() in a varible since the animate method may deregister
        // the animation thus invalidating the size
        for (int iter = 0; iter < v.size(); iter++) {
            Animation c = (Animation) v.elementAt(iter);
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
                || (internalAnimatableComponents != null && internalAnimatableComponents.size() > 0);
    }

    /**
     * @inheritDoc
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
        super.refreshTheme(merge);

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
        actionCommandImpl(cmd, new ActionEvent(cmd));
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

    void initFocused() {
        if (focused == null) {
            setFocused(contentPane.findFirstFocusable());
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
        Display.getInstance().getImplementation().onShow(this);
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
     * @inheritDoc
     */
    void initComponentImpl() {
        super.initComponentImpl();
        if (Display.getInstance().isNativeCommands()) {
            Display.getInstance().getImplementation().setNativeCommands(menuBar.getCommands());
        }
        if (getParent() != null) {
            getParent().getComponentForm().registerAnimated(this);
        }
    }

    /**
     * @inheritDoc
     */
    public void setSmoothScrolling(boolean smoothScrolling) {
        // invoked by the constructor for component
        if (contentPane != null) {
            contentPane.setSmoothScrolling(smoothScrolling);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isSmoothScrolling() {
        return contentPane.isSmoothScrolling();
    }

    /**
     * @inheritDoc
     */
    public int getScrollAnimationSpeed() {
        return contentPane.getScrollAnimationSpeed();
    }

    /**
     * @inheritDoc
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
            showListener.fireActionEvent(new ActionEvent(this));
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

            previousForm.tint = true;
        }
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
            if (p instanceof BGPainter && ((BGPainter) p).getPreviousForm() != null) {
                ((BGPainter) p).setPreviousForm(previousForm);
            } else {
                BGPainter b = new BGPainter(this, p);
                getStyle().setBgPainter(b);
                b.setPreviousForm(previousForm);
            }
            revalidate();
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
            previousForm.tint = false;

            if (previousForm instanceof Dialog) {
                if (!((Dialog) previousForm).isDisposed()) {
                    Display.getInstance().setCurrent(previousForm, false);
                }
            } else {
                Display.getInstance().setCurrent(previousForm, false);
                //previousForm.revalidate();
            }

            // enable GC to cleanup the previous form if no longer referenced
            previousForm = null;
        }
    }

    boolean isMenu() {
        return false;
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
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
                || selected.getPadding(false, Component.TOP) != unselected.getPadding(false, Component.TOP)
                || selected.getPadding(false, Component.BOTTOM) != unselected.getPadding(false, Component.BOTTOM)
                || selected.getPadding(isRTL(), Component.RIGHT) != unselected.getPadding(isRTL(), Component.RIGHT)
                || selected.getPadding(isRTL(), Component.LEFT) != unselected.getPadding(isRTL(), Component.LEFT)
                || selected.getMargin(false, Component.TOP) != unselected.getMargin(false, Component.TOP)
                || selected.getMargin(false, Component.BOTTOM) != unselected.getMargin(false, Component.BOTTOM)
                || selected.getMargin(isRTL(), Component.RIGHT) != unselected.getMargin(isRTL(), Component.RIGHT)
                || selected.getMargin(isRTL(), Component.LEFT) != unselected.getMargin(isRTL(), Component.LEFT)) {
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
     * @inheritDoc
     */
    protected void longKeyPress(int keyCode) {
        if (focused != null) {
            if (focused.getComponentForm() == this) {
                focused.longKeyPress(keyCode);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void longPointerPress(int x, int y) {
        if (focused != null && focused.contains(x, y)) {
            if (focused.getComponentForm() == this) {
                focused.longPointerPress(x, y);
            }
        }
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     */
    public Layout getLayout() {
        return contentPane.getLayout();
    }

    /**
     * @inheritDoc
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

    private void fireKeyEvent(Hashtable keyListeners, int keyCode) {
        if (keyListeners != null) {
            Vector listeners = (Vector) keyListeners.get(new Integer(keyCode));
            if (listeners != null) {
                ActionEvent evt = new ActionEvent(this, keyCode);
                for (int iter = 0; iter < listeners.size(); iter++) {
                    ((ActionListener) listeners.elementAt(iter)).actionPerformed(evt);
                    if (evt.isConsumed()) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
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

    private void tactileTouchVibe(int x, int y, Component cmp) {
        if (tactileTouchDuration > 0 && cmp.isTactileTouch(x, y)) {
            Display.getInstance().vibrate(tactileTouchDuration);
        }
    }

    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
        if (pointerPressedListeners != null) {
            pointerPressedListeners.fireActionEvent(new ActionEvent(this, x, y));
        }
        //check if the click is relevant to the menu bar.
        if (menuBar.contains(x, y)) {
            Component cmp = menuBar.getComponentAt(x, y);
            if (cmp != null && cmp.isEnabled()) {
                cmp.pointerPressed(x, y);
                tactileTouchVibe(x, y, cmp);
            }
            return;
        }

        if (y >= contentPane.getY()) {
            Component cmp = contentPane.getComponentAt(x, y);
            if (cmp != null) {
                cmp.initDragAndDrop(x, y);
                if (cmp.hasLead) {
                    Container leadParent;
                    if (cmp instanceof Container) {
                        leadParent = ((Container) cmp).getLeadParent();
                    } else {
                        leadParent = cmp.getParent().getLeadParent();
                    }
                    leadParent.repaint();
                    setFocused(leadParent);
                    cmp.getLeadComponent().pointerPressed(x, y);
                } else {
                    if (cmp.isEnabled()) {
                        if (cmp.isFocusable()) {
                            setFocused(cmp);
                        }
                        cmp.pointerPressed(x, y);
                        tactileTouchVibe(x, y, cmp);
                    }
                }
            }
        } else {
            Component cmp = titleArea.getComponentAt(x, y);
            if (cmp != null && cmp.isEnabled() && cmp.isFocusable()) {
                cmp.pointerPressed(x, y);
                tactileTouchVibe(x, y, cmp);
            }
        }
        initialPressX = x;
        initialPressY = y;
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

    /**
     * @inheritDoc
     */
    public void pointerDragged(int x, int y) {
        if (pointerDraggedListeners != null) {
            pointerDraggedListeners.fireActionEvent(new ActionEvent(this, x, y));
        }

        if (dragged != null) {
            dragged.pointerDragged(x, y);
            return;
        }

        Component cmp = contentPane.getComponentAt(x, y);
        if (cmp != null) {
            if (cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerDragged(x, y);
            cmp.repaint();
        }
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        if (pointerDraggedListeners != null) {
            pointerDraggedListeners.fireActionEvent(new ActionEvent(this, x[0], y[0]));
        }

        if (dragged != null) {
            dragged.pointerDragged(x, y);
            return;
        }

        Component cmp = contentPane.getComponentAt(x[0], y[0]);
        if (cmp != null) {
            if (cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerDragged(x, y);
            cmp.repaint();
        }
    }
    
    

    /**
     * @inheritDoc
     */
    public void pointerHoverReleased(int[] x, int[] y) {

        if (dragged != null) {
            dragged.pointerHoverReleased(x, y);
            dragged = null;
            return;
        }

        Component cmp = contentPane.getComponentAt(x[0], y[0]);
        if (cmp != null) {
            if (cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerHoverReleased(x, y);
            cmp.repaint();
        }
    }

    /**
     * @inheritDoc
     */
    public void pointerHoverPressed(int[] x, int[] y) {
        Component cmp = contentPane.getComponentAt(x[0], y[0]);
        if (cmp != null) {
            if (cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerHoverPressed(x, y);
            cmp.repaint();
        }
    }

    /**
     * @inheritDoc
     */
    public void pointerHover(int[] x, int[] y) {

        if (dragged != null) {
            dragged.pointerHover(x, y);
            return;
        }

        Component cmp = contentPane.getComponentAt(x[0], y[0]);
        if (cmp != null) {
            if (cmp.isFocusable() && cmp.isEnabled()) {
                setFocused(cmp);
            }
            cmp.pointerHover(x, y);
            cmp.repaint();
        }
    }

    /**
     * Returns true if there is only one focusable member in this form. This is useful
     * so setHandlesInput would always be true for this case.
     * 
     * @return true if there is one focusable component in this form, false for 0 or more
     */
    public boolean isSingleFocusMode() {
        return isSingleFocusMode(0, getContentPane()) == 1;
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
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        if (pointerReleasedListeners != null) {
            pointerReleasedListeners.fireActionEvent(new ActionEvent(this, x, y));
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

            if (y >= contentPane.getY()) {
                Component cmp = contentPane.getComponentAt(x, y);
                if (cmp != null && cmp.isEnabled()) {
                    if (cmp.hasLead) {
                        Container leadParent;
                        if (cmp instanceof Container) {
                            leadParent = ((Container) cmp).getLeadParent();
                        } else {
                            leadParent = cmp.getParent().getLeadParent();
                        }
                        leadParent.repaint();
                        setFocused(leadParent);
                        cmp.getLeadComponent().pointerReleased(x, y);
                    } else {
                        if (cmp.isEnabled()) {
                            if (cmp.isFocusable()) {
                                setFocused(cmp);
                            }
                            cmp.pointerReleased(x, y);
                        }
                    }
                }
            } else {
                Component cmp = titleArea.getComponentAt(x, y);
                if (cmp != null && cmp.isEnabled()) {
                    cmp.pointerReleased(x, y);
                }
            }
        } else {
            if (dragged.isDragAndDropInitialized()) {
                dragged.dragFinished(x, y);
                dragged = null;
            } else {
                dragged.pointerReleased(x, y);
                dragged = null;
            }
        }
        if (buttonsAwatingRelease != null && !Display.getInstance().isRecursivePointerRelease()) {
            for (int iter = 0; iter < buttonsAwatingRelease.size(); iter++) {
                Button b = (Button) buttonsAwatingRelease.elementAt(iter);
                b.setState(Button.STATE_DEFAULT);
                b.repaint();
            }
            buttonsAwatingRelease = null;
        }
    }

    /**
     * @inheritDoc
     */
    public void setScrollableY(boolean scrollableY) {
        getContentPane().setScrollableY(scrollableY);
    }

    /**
     * @inheritDoc
     */
    public void setScrollableX(boolean scrollableX) {
        getContentPane().setScrollableX(scrollableX);
    }

    /**
     * @inheritDoc
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
                int focusedY = focused.getAbsoluteY();
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
     * @param down if true will the return the next focusable on the bottom else
     * on the top
     * @return a focusable Component or null if not found
     */
    public Component findNextFocusVertical(boolean down) {
        Component c = findNextFocusVertical(focused, null, contentPane, down);
        if (c != null) {
            return c;
        }
        if (cyclicFocus) {
            c = findNextFocusVertical(focused, null, contentPane, !down);
            if (c != null) {
                Component current = findNextFocusVertical(c, null, contentPane, !down);
                while (current != null) {
                    c = current;
                    current = findNextFocusVertical(c, null, contentPane, !down);
                }
                return c;
            }
        }
        return null;
    }

    /**
     * This method returns the next focusable Component horizontally
     * @param right if true will the return the next focusable on the right else
     * on the left
     * @return a focusable Component or null if not found
     */
    public Component findNextFocusHorizontal(boolean right) {
        Component c = findNextFocusHorizontal(focused, null, contentPane, right);
        if (c != null) {
            return c;
        }
        if (cyclicFocus) {
            c = findNextFocusHorizontal(focused, null, contentPane, !right);
            if (c != null) {
                Component current = findNextFocusHorizontal(c, null, contentPane, !right);
                while (current != null) {
                    c = current;
                    current = findNextFocusHorizontal(c, null, contentPane, !right);
                }
                return c;
            }
        }
        return null;
    }

    Component findNextFocusDown() {
        if (focused != null) {
            if (focused.getNextFocusDown() != null) {
                return focused.getNextFocusDown();
            }
            return findNextFocusVertical(true);
        }
        return null;
    }

    Component findNextFocusUp() {
        if (focused != null) {
            if (focused.getNextFocusUp() != null) {
                return focused.getNextFocusUp();
            }
            return findNextFocusVertical(false);
        }
        return null;
    }

    Component findNextFocusRight() {
        if (focused != null) {
            if (focused.getNextFocusRight() != null) {
                return focused.getNextFocusRight();
            }
            return findNextFocusHorizontal(true);
        }
        return null;
    }

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
     * @inheritDoc
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
                if (parent.isScrollable()) {
                    return parent.moveScrollTowards(direction, c);
                }
                parent = parent.getParent();
            }
        }

        return true;
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
     * @inheritDoc
     */
    public void setRTL(boolean r) {
        super.setRTL(r);
        contentPane.setRTL(r);
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        paintBackground(g);
        super.paint(g);
        if (tint) {
            g.setColor(tintColor);
            g.fillRect(0, 0, getWidth(), getHeight(), (byte) ((tintColor >> 24) & 0xff));
        }
    }

    /**
     * @inheritDoc
     */
    public void setScrollable(boolean scrollable) {
        getContentPane().setScrollable(scrollable);
    }

    /**
     * @inheritDoc
     */
    public boolean isScrollable() {
        return getContentPane().isScrollable();
    }

    /**
     * @inheritDoc
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (mediaComponents != null) {
            int size = mediaComponents.size();
            for (int i = 0; i < size; i++) {
                Component mediaCmp = (Component) mediaComponents.elementAt(i);
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
     * @inheritDoc
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
}
