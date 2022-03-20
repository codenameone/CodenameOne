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

import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;


/**
 * <p>Button is the base class for several UI widgets allowing clickability.
 * It has 3 states: rollover, pressed and the default state. {@code Button}
 * can also have an {@link com.codename1.ui.events.ActionListener} that react when the 
 * {@code Button} is clicked or handle actions via a 
 * {@link com.codename1.ui.Command}.<br>
 * Button has the "Button" UIID by default.</p>
 * <p>
 * Here is trivial usage of the {@code Button} API:
 * </p>
 * <script src="https://gist.github.com/codenameone/99cdefe0c73096ebdbfb.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-button.png" alt="Simple Button" />
 * 
 * <p>
 * This code shows a common use case of making a button look like a hyperlink
 * </p>
 * <script src="https://gist.github.com/codenameone/2627b4edc5d3d340ce90.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-link-button.png" alt="Hyperlink Button" />
 * 
 * @author Chen Fishbein
 */
public class Button extends Label implements ReleasableComponent, ActionSource, SelectableIconHolder {
    /**
     * Default value for the button ripple effect, this can be set with the theme constant buttonRippleBool
     */
    private static boolean buttonRippleEffectDefault;

    /**
     * Indicates whether text on the button should be drawn capitalized by default to match the Android design
     */
    private static boolean capsTextDefault;

    /**
     * Indicates whether text on the button should be drawn capitalized by default to match the Android design.
     * This value can be set by the {@code capsButtonTextBool} theme constant
     * @return the capsTextDefault
     */
    public static boolean isCapsTextDefault() {
        return capsTextDefault;
    }

    /**
     * Indicates whether text on the button should be drawn capitalized by default to match the Android design
     * This value can be set by the {@code capsButtonTextBool} theme constant
     * @param aCapsTextDefault the capsTextDefault to set
     */
    public static void setCapsTextDefault(boolean aCapsTextDefault) {
        capsTextDefault = aCapsTextDefault;
    }

    /**
     * Default value for the button ripple effect, this can be set with the theme constant buttonRippleBool
     * @return the buttonRippleEffectDefault
     */
    public static boolean isButtonRippleEffectDefault() {
        return buttonRippleEffectDefault;
    }

    /**
     * Default value for the button ripple effect, this can be set with the theme constant buttonRippleBool
     * @param aButtonRippleEffectDefault the buttonRippleEffectDefault to set
     */
    public static void setButtonRippleEffectDefault(boolean aButtonRippleEffectDefault) {
        buttonRippleEffectDefault = aButtonRippleEffectDefault;
    }
    
    /**
     * Indicates whether text on the button should be drawn capitalized by default to match the Android design
     */
    private Boolean capsText;
    
    /**
     * Indicates the rollover state of a button which is equivalent to focused for
     * most uses
     */
    public static final int STATE_ROLLOVER = 0;
    
    /**
     * Indicates the pressed state of a button 
     */
    public static final int STATE_PRESSED = 1;
    
    /**
     * Indicates the default state of a button which is neither pressed nor focused
     */
    public static final int STATE_DEFAULT = 2;
    
    private EventDispatcher dispatcher = new EventDispatcher();
    private EventDispatcher stateChangeListeners;
    
    private int state = STATE_DEFAULT;
    
    private Image pressedIcon;
    
    private Image rolloverIcon;
    private Image rolloverPressedIcon;
  
    private Image disabledIcon;
    private Command cmd;

    private boolean toggle;

    private int releaseRadius;

    private boolean autoRelease;

    /** 
     * Constructs a button with an empty string for its text.
     */
    public Button() {
        this("");
    }
    
    /**
     * Constructs a button with the specified text.
     * 
     * @param text label appearing on the button
     */
    public Button(String text) {
        this(text, null, "Button");
    }
    
    /**
     * Allows binding a command to a button for ease of use
     * 
     * @param cmd command whose text would be used for the button and would recive action events
     * from the button
     */
    public Button(Command cmd) {
        this(cmd.getCommandName(), cmd.getIcon());
        addActionListener(cmd);
        this.cmd = cmd;
        setEnabled(cmd.isEnabled());
        updateCommand();
    }

    private void updateCommand() {
        if(cmd.getMaterialIcon() == 0) {
            setRolloverIcon(cmd.getRolloverIcon());
            setDisabledIcon(cmd.getDisabledIcon());
            setPressedIcon(cmd.getPressedIcon());
        } else {
            if(cmd.getIconFont() != null) {
                setFontIcon(cmd.getIconFont(), cmd.getMaterialIcon(), cmd.getMaterialIconSize());
            } else {
                setMaterialIcon(cmd.getMaterialIcon(), cmd.getMaterialIconSize());
            }
        }
        if(cmd.getIconGapMM() > -1) {
            setGap(Display.INSTANCE.convertToPixels(cmd.getIconGapMM()));
        }
    }

    /**
     * Applies the given command to this button
     *
     * @param  cmd the command on the button
     */
    public void setCommand(Command cmd) {
        if(this.cmd != null) {
            removeActionListener(this.cmd);
        }
        this.cmd = cmd;
        if(cmd != null) {
            setText(cmd.getCommandName());
            if(cmd.getIcon() == null) {
                if(cmd.getMaterialIcon() != 0) {
                    if(cmd.getIconFont() != null) {
                        setFontIcon(cmd.getIconFont(),cmd.getMaterialIcon(), cmd.getMaterialIconSize());
                    } else {
                        setMaterialIcon(cmd.getMaterialIcon(), cmd.getMaterialIconSize());
                    }
                }
            } else {
                setIcon(cmd.getIcon());
            }
            setEnabled(cmd.isEnabled());
            updateCommand();
            addActionListener(cmd);
        }
    }

    /**
     * Constructs a button with the specified image.
     * 
     * @param icon appearing on the button
     */
    public Button(Image icon) {
        this("", icon);
    }

    /**
     * Constructs a button with the specified material image icon.
     * 
     * @param icon appearing on the button
     */
    public Button(char icon) {
        this("");
        setMaterialIcon(icon);
    }

    /**
     * Constructor a button with text, material image and uiid
     * 
     * @param text label appearing on the button
     * @param icon image appearing on the button
     * @param id UIID unique identifier for button
     */
    public Button(String text, char icon, String id) {
        this(text, null, id);
        setMaterialIcon(icon);
    }

    /**
     * Constructor a button with text, material image and uiid
     * 
     * @param text label appearing on the button
     * @param icon image appearing on the button
     * @param iconSize image size in millimeters
     * @param id UIID unique identifier for button
     */
    public Button(String text, char icon, float iconSize, String id) {
        this(text, null, id);
        setMaterialIcon(icon, iconSize);
    }
    
    /**
     * Constructor a button with text, image and uiid
     * 
     * @param text label appearing on the button
     * @param icon image appearing on the button
     * @param id UIID unique identifier for button
     */
    public Button(String text, Image icon, String id) {
        super(text);
        setUIID(id);
        setFocusable(true);
        setIcon(icon);
        this.pressedIcon = icon;
        this.rolloverIcon = icon;
        releaseRadius = UIManager.getInstance().getThemeConstant("releaseRadiusInt", 0);
        setRippleEffect(buttonRippleEffectDefault);        
        if(isCapsText() && text != null) {
            putClientProperty("cn1$origText", text);
            super.setText(UIManager.getInstance().localize(text, text).toUpperCase());
        } 
        setCursor(HAND_CURSOR);
    }
    
    /**
     * Constructor a button with text and image
     * 
     * @param text label appearing on the button
     * @param icon image appearing on the button
     */
    public Button(String text, Image icon) {
        this(text, icon, "Button");
    }
    
    /**
     * Constructor a button with image and UIID
     * 
     * @param icon image appearing on the button
     * @param id UIID unique identifier for button
     */
    public Button(Image icon, String id) {
        this("", icon, id);
    }
    
    /**
     * Constructor a button with material image icon and UIID
     * 
     * @param icon image appearing on the button
     * @param id UIID unique identifier for button
     */
    public Button(char icon, String id) {
        this("", id);
        setMaterialIcon(icon);
    }
    
    /**
     * Constructor a button with material image icon and UIID
     * 
     * @param icon image appearing on the button
     * @param iconSize the size of the icon in millimeters
     * @param id UIID unique identifier for button
     */
    public Button(char icon, float iconSize, String id) {
        this("", id);
        setMaterialIcon(icon, iconSize);
    }
    
    /**
     * Constructor a button with text and UIID
     * 
     * @param text label appearing on the button
     * @param id UIID unique identifier for button
     */
    public Button(String text, String id) {
        this(text, null, id);
    }
    
    /**
     * A listener used to bind the state with another button.  When that button's state
     * changes, then this button state will also change.
     * @since 7.0
     */
    private ActionListener bindListener;
    private ActionListener bindListener() {
        if (bindListener == null) {
            bindListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() instanceof Button) {
                        Button b = (Button)e.getSource();
                        if (state != b.getState()) {
                            setState(b.getState());
                            repaint();
                        }
                    }
                }
            };
        }
        return bindListener;
    }
    
    /**
     * Bind the state of this button to another button's state.  Once bound, when the other
     * button's state changes, this button will change its state to match.
     * @param button The button whose state to bind to.
     * @see #unbindStateFrom(com.codename1.ui.Button) 
     * @since 7.0
     */
    public void bindStateTo(Button button) {
        button.addStateChangeListener(bindListener());
    }
    
    /**
     * Unbinds the state of this button from another button.
     * @param button The button to unbind state from.
     * @since 7.0
     * @see #bindStateTo(com.codename1.ui.Button) 
     */
    public void unbindStateFrom(Button button) {
        if (bindListener != null) {
            button.removeStateChangeListener(bindListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void resetFocusable() {
        setFocusable(true);
    }

    /**
     * {@inheritDoc}
     */
    void focusGainedInternal() {
        super.focusGainedInternal();
        if(state != STATE_PRESSED) {
            state = STATE_ROLLOVER;
            fireStateChange();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    void focusLostInternal() {
        super.focusLostInternal();
        if (state != STATE_DEFAULT) {
            state = STATE_DEFAULT;
            fireStateChange();
        }
        
    }
    
    /**
     * Returns the button state
     * 
     * @return One of STATE_ROLLOVER, STATE_DEAFULT, STATE_PRESSED
     */
    public int getState() {
        return state;
    }
    
    void setState(int state) {
        if (state != this.state) {
            this.state = state;
            fireStateChange();
        }
    }
    
    /**
     * Set the button in released and unfocused state
     */
    public void setReleased() {
    	setState(Button.STATE_DEFAULT);
    	repaint();
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * pressed state
     * 
     * @return icon used
     * @see #STATE_PRESSED
     */
    @Override
    public Image getPressedIcon() {
        return pressedIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * pressed state and is selected. This is ONLY applicable to toggle buttons
     *
     * @return icon used
     */
    @Override
    public Image getRolloverPressedIcon() {
        return rolloverPressedIcon;
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in
     * pressed state and is selected. This is ONLY applicable to toggle buttons
     *
     * @param rolloverPressedIcon icon used
     */
    @Override
    public void setRolloverPressedIcon(Image rolloverPressedIcon) {
        this.rolloverPressedIcon = rolloverPressedIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * the disabled state
     *
     * @return icon used
     */
    @Override
    public Image getDisabledIcon() {
        return disabledIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * rolled over state
     * 
     * @return icon used
     * @see #STATE_ROLLOVER
     */
    @Override
    public Image getRolloverIcon() {
        return rolloverIcon;
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * rolled over state
     * 
     * @param rolloverIcon icon to use
     * @see #STATE_ROLLOVER
     */
    @Override
    public void setRolloverIcon(Image rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
        setShouldCalcPreferredSize(true);
        checkAnimation();
        repaint();        
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * pressed state
     * 
     * @param pressedIcon icon used
     * @see #STATE_PRESSED
     */
    @Override
    public void setPressedIcon(Image pressedIcon) {
        this.pressedIcon = pressedIcon;
        setShouldCalcPreferredSize(true);
        checkAnimation();
        repaint();
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * the disabled state
     *
     * @param disabledIcon icon used
     */
    @Override
    public void setDisabledIcon(Image disabledIcon) {
        this.disabledIcon = disabledIcon;
        setShouldCalcPreferredSize(true);
        checkAnimation();
        repaint();
    }

    void checkAnimation() {
        super.checkAnimation();
        if((pressedIcon != null && pressedIcon.isAnimation()) || 
            (rolloverIcon != null && rolloverIcon.isAnimation()) ||
            (disabledIcon != null && disabledIcon.isAnimation())) {
            Form parent = getComponentForm();
            if(parent != null) {
                // animations are always running so the internal animation isn't
                // good enough. We never want to stop this sort of animation
                parent.registerAnimated(this);
            }
        }
    }
    
    /**
     * Adds a listener to be notified when the button state changes. 
     * @param l Listener to be notified when state changes
     * @see #getState() 
     * @see #setState(int) 
     * @see #removeStateChangeListener(com.codename1.ui.events.ActionListener) 
     * @since 7.0
     */
    public void addStateChangeListener(ActionListener l) {
        if (stateChangeListeners == null) {
            stateChangeListeners = new EventDispatcher();
        }
        stateChangeListeners.addListener(l);
    }
    
    /**
     * Removes state change listener.
     * @param l State change listener to remove.
     * @see #addStateChangeListener(com.codename1.ui.events.ActionListener) 
     * @see #getState() 
     * @see #setState(int) 
     * @since 7.0
     */
    public void removeStateChangeListener(ActionListener l) {
        if (stateChangeListeners != null) {
            stateChangeListeners.removeListener(l);
        }
    }
    
    private void fireStateChange() {
        if (stateChangeListeners != null && stateChangeListeners.hasListeners()) {
            stateChangeListeners.fireActionEvent(new ActionEvent(this));
        }
    }
    
    /**
     * Adds a listener to the button which will cause an event to dispatch on click
     * 
     * @param l implementation of the action listener interface
     */
    public void addActionListener(ActionListener l){
        dispatcher.addListener(l);
    }
    
    /**
     * Removes the given action listener from the button
     * 
     * @param l implementation of the action listener interface
     */
    public void removeActionListener(ActionListener l){
        dispatcher.removeListener(l);
    }

    /**
     * Returns a vector containing the action listeners for this button
     * @return the action listeners
     * @deprecated use getListeners instead
     */
    public Vector getActionListeners() {
        return dispatcher.getListenerVector();
    }
    
    /**
     * Returns a collection containing the action listeners for this button
     * @return the action listeners
     */
    public Collection getListeners() {
        return dispatcher.getListenerCollection();
    }
    
    /**
     * Returns the icon for the button based on its current state
     *
     * @return the button icon based on its current state
     */
    @Override
    public Image getIconFromState() {
        Image icon = getMaskedIcon();
        if(!isEnabled() && getDisabledIcon() != null) {
            return getDisabledIcon();
        }
        if(isToggle() && isSelected()) {
            icon = rolloverPressedIcon;
            if(icon == null) {
                icon = getPressedIcon();
                if (icon == null) {
                    icon = getMaskedIcon();
                }
            }
            return icon;
        }
        switch (getState()) {
            case Button.STATE_DEFAULT:
                break;
            case Button.STATE_PRESSED:
                icon = getPressedIcon();
                if (icon == null) {
                    icon = getMaskedIcon();
                }
                break;
            case Button.STATE_ROLLOVER:
                if(Display.getInstance().shouldRenderSelection(this)) {
                    icon = getRolloverIcon();
                    if (icon == null) {
                        icon = getMaskedIcon();
                    }
                }
                break;
        }
        return icon;
    }

    /**
     * Allows subclasses to override action event behavior 
     * {@inheritDoc}
     * 
     * @param x the x position of the click if applicable (can be 0 or -1 otherwise)
     * @param y the y position of the click if applicable (can be 0 or -1 otherwise)
     */
    protected void fireActionEvent(int x, int y){
        super.fireActionEvent();
        if(cmd != null) {
            ActionEvent ev = new ActionEvent(cmd, this, x, y);
            dispatcher.fireActionEvent(ev);
            if(!ev.isConsumed()) {
                Form f = getComponentForm();
                if(f != null) {
                    f.actionCommandImplNoRecurseComponent(cmd, ev);
                }
            }
        } else {
            dispatcher.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed,x, y));
        }
        Display d = Display.getInstance();
        if(d.isBuiltinSoundsEnabled()) {
            d.playBuiltinSound(Display.SOUND_TYPE_BUTTON_PRESS);
        }
    }
    
    /**
     * Invoked to change the state of the button to the pressed state
     */
    public void pressed(){
        if (!Display.impl.isScrollWheeling()) {
            if (state != STATE_PRESSED) {
                state=STATE_PRESSED;
                fireStateChange();
            }
            repaint();
        }
    }
    
    /**
     * Invoked to change the state of the button to the released state
     */
    public void released() {
        released(-1, -1);
    }
    
    /**
     * Invoked to change the state of the button to the released state
     *
     * @param x the x position if a touch event triggered this, -1 if this isn't relevant
     * @param y the y position if a touch event triggered this, -1 if this isn't relevant
     */
    public void released(int x, int y) {
        if (!Display.impl.isScrollWheeling()) {
            if (state != STATE_ROLLOVER) {
                state=STATE_ROLLOVER;
                fireStateChange();
            }
            //if (releaseRadius > 0 || (Math.abs(x - pressedX) < CN.convertToPixels(1) && Math.abs(y-pressedY) < CN.convertToPixels(1))) {
            fireActionEvent(x, y);
            //}
            
            repaint();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void keyPressed(int keyCode) {
        if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE){
            pressedX = -1;
            pressedY = -1;
            pressed();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void keyReleased(int keyCode) {
        if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE){
            released();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void keyRepeated(int keyCode) {
    }
    
    /**
     * {@inheritDoc}
     */
    protected void fireClicked() {
        pressed();
        released();
    }
    
    /**
     * {@inheritDoc}
     */
    protected boolean isSelectableInteraction() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHover(int[] x, int[] y) {
        if (!Display.getInstance().isDesktop()) {
            requestFocus();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void pointerHoverReleased(int[] x, int[] y) {
        if (!Display.getInstance().isDesktop()) {
            requestFocus();
        }
    }

    private int pressedX, pressedY;
    
    /**
     * {@inheritDoc}
     */
    public void pointerPressed(int x, int y) {
        Component leadParent = LeadUtil.leadParentImpl(this);
        leadParent.clearDrag();
        leadParent.setDragActivated(false);
        if (pointerPressedListeners != null && pointerPressedListeners.hasListeners()) {
            pointerPressedListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed, x, y));
        }
        pressedX = x;
        pressedY = y;
        pressed();
        Form f = getComponentForm();
        // might happen when programmatically triggering press
        if(f != null) {
        	f.addComponentAwaitingRelease(this);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        if (pointerReleasedListeners != null && pointerReleasedListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(this, ActionEvent.Type.PointerReleased, x, y);
            pointerReleasedListeners.fireActionEvent(ev);
            if(ev.isConsumed()) {
                return;
            }
        }
        Form f = getComponentForm();
        // might happen when programmatically triggering press
        if(f != null) {
            f.removeComponentAwaitingRelease(this);
        }

        // button shouldn't fire an event when a pointer is dragged into it
        if(state == STATE_PRESSED) {
            released(x, y);
         }
        if(restoreDragPercentage > -1) {
            Display.getInstance().setDragStartPercentage(restoreDragPercentage);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected void dragInitiated() {
        if(Display.getInstance().shouldRenderSelection(this)) {
            if (state != STATE_ROLLOVER) {
                state=STATE_ROLLOVER;
                fireStateChange();
            }
        } else {
            if (state != STATE_DEFAULT) {
                state=STATE_DEFAULT;
                fireStateChange();
            }
        }
        repaint();
    }

    @Override
    void initComponentImpl() {
        super.initComponentImpl(); 
        if(pressedIcon != null) {
            pressedIcon.lock();
        }
        if(rolloverIcon != null) {
            rolloverIcon.lock();
        }
        if(rolloverPressedIcon != null) {
            rolloverPressedIcon.lock();
        }
        if(disabledIcon != null) {
            disabledIcon.lock();
        }
    }

    @Override
    void deinitializeImpl() {
        if (state == STATE_PRESSED) {
            setReleased();
        }
        super.deinitializeImpl(); 
        if(pressedIcon != null) {
            pressedIcon.unlock();
        }
        if(rolloverIcon != null) {
            rolloverIcon.unlock();
        }
        if(rolloverPressedIcon != null) {
            rolloverPressedIcon.unlock();
        }
        if(disabledIcon != null) {
            disabledIcon.unlock();
        }
    }
   
    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize(){
        return getUIManager().getLookAndFeel().getButtonPreferredSize(this);
    }
    
    /**
     * {@inheritDoc}
     */
    protected Border getBorder() {
        return getStyle().getBorder();
    }

    boolean isPressedStyle() {
        // if a toggle button has focus we should draw the selected state not the pressed state
        // however if shouldRenderSelection is false the selected state won't be painted so
        // we should draw the pressed state
        if(toggle && isSelected()) {
            if(hasFocus()) {
                return !Display.getInstance().shouldRenderSelection(this);
            }
            return true;
        }
        return state == STATE_PRESSED;
    }

    /**
     * This method return the Button Command if exists
     * 
     * @return Command Object or null if a Command not exists
     */
    public Command getCommand() {
        return cmd;
    }

    /**
     * Returns true if the button is selected for toggle buttons,
     *
     * @return true if the button is selected
     */
    public boolean isSelected() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @deprecated use the Style alignment instead
     */
    public void setAlignment(int align){
        super.setAlignment(align);
        getPressedStyle().setAlignment(align);
    }

    /**
     * Toggle button mode is only relevant for checkboxes/radio buttons. When pressed
     * a toggle button stays pressed and when pressed again it moves to releleased state.
     *
     * @return the toggle
     */
    public boolean isToggle() {
        return toggle;
    }

    /**
     * Toggle button mode is only relevant for checkboxes/radio buttons. When pressed
     * a toggle button stays pressed and when pressed again it moves to releleased state.
     * Setting toggle implicitly changes the UIID to "ToggleButton"
     *
     * @param toggle the toggle to set
     */
    public void setToggle(boolean toggle) {
        this.toggle = toggle;
        if(toggle && getUIID().equals("CheckBox") || getUIID().equals("RadioButton")) {
            setUIID("ToggleButton");
        }
    }

    /**
     * Overriden to workaround issue with caps text and different UIID's
     * {@inheritDoc}
     */
    @Override
    public void setUIID(String id) {
        super.setUIID(id);
        String t = (String)getClientProperty("cn1$origText");
        if(t != null) {
            if(isCapsText()) {
                super.setText(UIManager.getInstance().localize(t, t).toUpperCase());
            } else {
                super.setText(UIManager.getInstance().localize(t, t));
                putClientProperty("cn1$origText", null);
            }
        } 
    }
    
    

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        boolean a = super.animate();
        if(!isEnabled() && disabledIcon != null) {
            a |= disabledIcon.isAnimation() && disabledIcon.animate();
        } else {
            switch(state) {
                case STATE_ROLLOVER:
                    a |= rolloverIcon != null && rolloverIcon.isAnimation() && rolloverIcon.animate();
                    break;
                case STATE_PRESSED:
                    a |= pressedIcon != null && pressedIcon.isAnimation() && pressedIcon.animate();
                    break;
            }
        }
        return a;
    }

    /**
     * Places the check box or radio button on the opposite side at the far end
     *
     * @return the oppositeSide
     */
    public boolean isOppositeSide() {
        return false;
    }

    /**
     * Indicates a radius in which a pointer release will still have effect. Notice that this only applies to
     * pointer release events and not to pointer press events
     * @return the releaseRadius
     */
    public int getReleaseRadius() {
        return releaseRadius;
    }

    /**
     * Indicates a radius in which a pointer release will still have effect. Notice that this only applies to
     * pointer release events and not to pointer press events
     * @param releaseRadius the releaseRadius to set
     */
    public void setReleaseRadius(int releaseRadius) {
        this.releaseRadius = releaseRadius;
    }
    
    /**
     * Returns if this is an auto released Button.
     * Auto released Buttons will are been disarmed when a drag is happening 
     * within the Button.
     * 
     * @return true if it's an auto released Button.
     */ 
    public boolean isAutoRelease(){
        return autoRelease;
    }
    
    /**
     * Sets the auto released mode of this button, by default it's not an auto 
     * released Button
     */ 
    public void setAutoRelease(boolean autoRelease){
        this.autoRelease = autoRelease;
    }

    @Override
    public void paint(Graphics g) {
        if(isLegacyRenderer()) {
            initAutoResize();
            getUIManager().getLookAndFeel().drawButton(g, this);
            return;
        }
        super.paintImpl(g);
    }

    /**
     * Indicates whether text on the button should be drawn capitalized by 
     * default to match the Android design. By default only {@code Button} and
     * {@code RaisedButton} will be capped to keep compatibility. You can add
     * additional UIID's to the mix by using the theme constant 
     * {@code capsButtonUiids} which can include a separated list of the 
     * UIID's to capitalize
     * @return the capsText
     */
    public final boolean isCapsText() {
        if(capsText == null) {
            if(capsTextDefault) {
                String uiid = getUIID();
                return uiid.equals("Button") || uiid.equals("RaisedButton") || 
                        getUIManager().getThemeConstant("capsButtonUiids", "").indexOf(uiid) > -1;
            }
            return false;
        }
        return capsText;
    }

    /**
     * Indicates whether text on the button should be drawn capitalized by 
     * default to match the Android design. By default only {@code Button} and
     * {@code RaisedButton} will be capped to keep compatibility. You can add
     * additional UIID's to the mix by using the theme constant 
     * {@code capsButtonUiids} which can include a separated list of the 
     * UIID's to capitalize
     * @param capsText the capsText to set
     */
    public void setCapsText(boolean capsText) {
        this.capsText = capsText;
    }

    /**
     * Overriden to implement the caps mode {@link #setCapsText(boolean)}
     * {@inheritDoc}
     */
    @Override
    public void setText(String t) {
        if(isCapsText()) {
            putClientProperty("cn1$origText", t);
            if(t != null) {
                super.setText(getUIManager().localize(t, t).toUpperCase());
                return;
            } 
        }
        super.setText(t);
    }    
}
