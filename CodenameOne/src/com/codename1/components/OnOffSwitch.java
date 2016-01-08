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

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.util.Collection;
import java.util.Vector;

/**
 * The on/off switch is a checkbox of sort (although it derives container) that represents its state as 
 * a switch each of which has a short label associated with it.
 * It has two types: Android and iOS. The types differ in the way that they are rendered.
 * The Android type (the default) is just a button with a label that can be moved/dragged between
 * the two states. The iOS version is more elaborate due to the look of that platform. 
 *
 * @author Shai Almog
 */
public class OnOffSwitch extends Container {
    private String on = "ON";
    private String off = "OFF";
    private boolean iosMode;
    private boolean noTextMode;
    private boolean value;
    private CheckBox button;
    private boolean dragged;
    private int pressX;
    private int buttonWidth;
    private Image switchOnImage;
    private Image switchOffImage;
    private Image switchMaskImage;
    private int deltaX;
    private EventDispatcher dispatcher = new EventDispatcher();
    private boolean animationLock;
    
    /**
     * Default constructor
     */
    public OnOffSwitch() {
        setUIID("OnOffSwitch");
        initialize();
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        if(iosMode) {
            return new Dimension(switchMaskImage.getWidth(), switchMaskImage.getHeight());
        }
        return super.calcPreferredSize();
    }

    /**
     * @inheritDoc
     */
    protected void resetFocusable() {
        setFocusable(true);
    }
    
    private void initialize() {
        iosMode = UIManager.getInstance().isThemeConstant("onOffIOSModeBool", false);
        removeAll();
        setFocusable(true);
        if(iosMode) {
            button = null;
            switchMaskImage = UIManager.getInstance().getThemeImageConstant("switchMaskImage");
            switchOnImage = UIManager.getInstance().getThemeImageConstant("switchOnImage");
            switchOffImage = UIManager.getInstance().getThemeImageConstant("switchOffImage");
            noTextMode = UIManager.getInstance().isThemeConstant("noTextModeBool", false);
        } else {
            setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            button = new CheckBox(on);
            button.setToggle(true);
            button.setUIID("Button");
            button.setEndsWith3Points(false);
            button.getUnselectedStyle().setFont(getUnselectedStyle().getFont());
            button.getSelectedStyle().setFont(getSelectedStyle().getFont());
            button.getPressedStyle().setFont(getSelectedStyle().getFont());
            
            Dimension d = button.getPreferredSize();
            button.setText(off);
            int pw = button.getPreferredW();
            d.setWidth(Math.max(pw, d.getWidth()));
            
            // prevents the button from growing/shrinking as its states flip
            button.setPreferredSize(d);
            
            buttonWidth = button.getPreferredW();
            button.setFocusable(false);
            updateButton();
            addComponent(button);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    flip();
                }
            });
        }
    }
    
    /**
     * @inheritDoc
     */
    protected boolean isStickyDrag() {
        return true;
    }
    
    /**
     * Adds a listener to the switch which will cause an event to dispatch on click
     * 
     * @param l implementation of the action listener interface
     */
    public void addActionListener(ActionListener l){
        dispatcher.addListener(l);
    }
    
    /**
     * Removes the given action listener from the switch
     * 
     * @param l implementation of the action listener interface
     */
    public void removeActionListener(ActionListener l){
        dispatcher.removeListener(l);
    }

    /**
     * Returns a vector containing the action listeners for this button
     * @return the action listeners
     * @deprecated use the version that returns a collection
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
    
    private void fireActionEvent(){
        dispatcher.fireActionEvent(new ActionEvent(this,ActionEvent.Type.PointerPressed));
        Display d = Display.getInstance();
        if(d.isBuiltinSoundsEnabled()) {
            d.playBuiltinSound(Display.SOUND_TYPE_BUTTON_PRESS);
        }
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        if(iosMode) {
            int switchButtonPadInt = UIManager.getInstance().getThemeConstant("switchButtonPadInt", 16);
            if(Display.getInstance().getDisplayWidth() > 480) {
                // is retina
                switchButtonPadInt *= 2;
            }
            Style s = getStyle();
            int x = getX() + s.getPadding(LEFT);
            int y = getY() + s.getPadding(TOP);
            if(!value) {
                if(deltaX > 0) {
                    dragged = false;
                } else {
                    if(deltaX < -switchOnImage.getWidth()) {
                        deltaX = -switchOnImage.getWidth();
                    }
                }
            } else {
                if(deltaX < 0) {
                    dragged = false;
                } else {
                    if(deltaX > switchOnImage.getWidth()) {
                        deltaX = switchOnImage.getWidth();
                    }
                }
            }
            if(dragged) {
                int onX;
                int offX;
                if(value) {
                    onX = x - deltaX;
                    offX = x - deltaX + switchOnImage.getWidth() - 2*switchButtonPadInt;
                } else {
                    onX = x - deltaX - switchOnImage.getWidth() + 2*switchButtonPadInt;
                    offX = x - deltaX;
                }
                switchButtonPadInt /= 2;
                g.drawImage(switchOnImage, onX, y);
                g.drawImage(switchOffImage, offX, y);
                int strWidth = s.getFont().stringWidth(on);
                int sX = onX + switchMaskImage.getWidth() / 2 - strWidth / 2 - switchButtonPadInt;
                int sY = y + switchMaskImage.getHeight() / 2 - s.getFont().getHeight() / 2;
                g.setFont(s.getFont());
                g.setColor(0xffffff);
                g.drawString(on, sX, sY, Style.TEXT_DECORATION_3D);
                strWidth = s.getFont().stringWidth(off);
                g.setColor(0x333333);
                sX = offX + switchMaskImage.getWidth() / 2 - strWidth / 2 + switchButtonPadInt;
                if(!noTextMode) {
                    g.drawString(off, sX, sY);
                }
            } else {
                String str;
                switchButtonPadInt /= 2;
                if(value) {
                    g.drawImage(switchOnImage, x, y);
                    str = on;
                    g.setColor(0xffffff);
                    switchButtonPadInt *= -1;
                } else {
                    g.drawImage(switchOffImage, x, y);
                    str = off;
                    g.setColor(0x333333);
                }
                int strWidth = s.getFont().stringWidth(str);
                int sX = x + switchMaskImage.getWidth() / 2 - strWidth / 2 + switchButtonPadInt;
                int sY = y + switchMaskImage.getHeight() / 2 - s.getFont().getHeight() / 2;
                g.setFont(s.getFont());
                if(!noTextMode) {
                    g.drawString(str, sX, sY);
                }
            }
            
            g.drawImage(switchMaskImage, x, y);
        } else {
            super.paint(g);
        }
    }
            
    private void updateButton() {
        if(value) {
            button.setText(on);
            getUnselectedStyle().setPadding(LEFT, buttonWidth);
            getUnselectedStyle().setPadding(RIGHT, 0);
            getSelectedStyle().setPadding(LEFT, buttonWidth);
            getSelectedStyle().setPadding(RIGHT, 0);
        } else {
            button.setText(off);            
            getUnselectedStyle().setPadding(RIGHT, buttonWidth);
            getUnselectedStyle().setPadding(LEFT, 0);
            getSelectedStyle().setPadding(RIGHT, buttonWidth);
            getSelectedStyle().setPadding(LEFT, 0);
        }
    }
    
    private void flip() {
        setValue(!value);
    }
    
    /**
     * @inheritDoc
     */
    protected void initComponent() {
        super.initComponent();
    }

    /**
     * @inheritDoc
     */
    protected void deinitialize() {
        super.deinitialize();
    }    

    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
        if(iosMode) {
            super.pointerPressed(x, y);
        }
        pressX = x;
    }
    
    /**
     * @inheritDoc
     */
    public void pointerDragged(int x, int y) {
        dragged = true;
        deltaX = pressX - x;
        if(!iosMode) {
            button.setText(on);
            int left = Math.max(0, buttonWidth - deltaX);
            int right = Math.min(buttonWidth, deltaX);
            if(deltaX < 0) {
                left = Math.min(buttonWidth, deltaX * -1);
                right = Math.max(0, buttonWidth + deltaX);
            }
            getUnselectedStyle().setPadding(RIGHT, right);
            getUnselectedStyle().setPadding(LEFT, left);
            getSelectedStyle().setPadding(RIGHT, right);
            getSelectedStyle().setPadding(LEFT, left);
            if(right < left) {
                button.setText(on);
            } else {
                button.setText(off);
            }
            revalidate();
        }
    }

    private void animateTo(final boolean value, final int position) {
        int switchButtonPadInt = UIManager.getInstance().getThemeConstant("switchButtonPadInt", 16);
        if(Display.getInstance().getDisplayWidth() > 480) {
            // is retina
            switchButtonPadInt *= 2;
        }
        final Motion current = Motion.createEaseInOutMotion(Math.abs(position), switchMaskImage.getWidth() - 2*switchButtonPadInt, 100);
        current.start();
        deltaX = position;
        getComponentForm().registerAnimated(new Animation() {
            public boolean animate() {
                deltaX = current.getValue();
                if(value) {
                    deltaX *= -1;
                }
                dragged = true;
                if(current.isFinished()) {
                    dragged = false;
                    Form f = getComponentForm();
                    if(f != null) {
                        f.deregisterAnimated(this);
                    }
                    OnOffSwitch.this.setValue(value);
                }
                repaint();
                return false;
            }

            public void paint(Graphics g) {
            }
        });
        dragged = true;
    }
    
    /**
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        if(animationLock) {
            return;
        }
        animationLock = true;
        if(iosMode) {
            int switchButtonPadInt = UIManager.getInstance().getThemeConstant("switchButtonPadInt", 16);
            if(dragged) {
                if(deltaX > 0) {
                    if(deltaX > switchMaskImage.getWidth() / 2 - switchButtonPadInt) {
                        animateTo(false, deltaX);
                    } else {
                        animateTo(true, deltaX);
                    }
                } else {
                    if(deltaX * -1 > switchMaskImage.getWidth() / 2 - switchButtonPadInt) {
                        animateTo(true, deltaX);
                    } else {
                        animateTo(false, deltaX);
                    }
                }
            } else {
                animateTo(!value, 0);
            }
            animationLock = false;
            return;
        } else {
            if(!dragged) {
                flip();
            } else {
                int w = buttonWidth;
                deltaX = pressX - x;
                int left = Math.max(0, w - deltaX);
                int right = Math.min(w, deltaX);
                if(deltaX < 0) {
                    left = Math.min(buttonWidth, deltaX * -1);
                    right = Math.max(0, buttonWidth + deltaX);
                }
                if(right < left) {
                    setValue(true);
                } else {
                    setValue(false);
                }
                
                updateButton();
                animateLayoutAndWait(150);
            }
        }        
        dragged = false;
        animationLock = false;
    }

    /**
     * Label for the on mode
     * @return the on
     */
    public String getOn() {
        return on;
    }

    /**
     * Label for the on mode
     * @param on the on to set
     */
    public void setOn(String on) {
        this.on = on;
        initialize();
    }

    /**
     * Label for the off mode
     * @return the off
     */
    public String getOff() {
        return off;
    }

    /**
     * Label for the off mode
     * @param off the off to set
     */
    public void setOff(String off) {
        this.off = off;
        initialize();
    }

    /**
     * The value of the switch
     * @return the value
     */
    public boolean isValue() {
        return value;
    }

    /**
     * The value of the switch
     * @param value the value to set
     */
    public void setValue(boolean value) {
        boolean orig = animationLock;
        animationLock = true;
        boolean fireEvent = this.value != value;
        if(fireEvent) {
            this.value = value;
            if(button != null) {
                button.setSelected(value);
            }
            fireActionEvent();
            if(iosMode) {
                repaint();
            } else {
                updateButton();
                if(isInitialized()){
                    animateLayoutAndWait(150);
                }
            }
        }
        animationLock = orig;
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {
            "value", "on", "off"
        };
    }

    /**
     * Some components may optionally generate a state which can then be restored
     * using setCompnentState(). This method is used by the UIBuilder.
     * @return the component state or null for undefined state.
     */
    public Object getComponentState() {
        if(value) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    
    /**
     * If getComponentState returned a value the setter can update the value and restore
     * the prior state.
     * @param state the non-null state
     */
    public void setComponentState(Object state) {
        value = ((Boolean)state).booleanValue();
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           Boolean.class,
           String.class,
           String.class
       };
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("on")) {
            return on;
        }
        if(name.equals("off")) {
            return off;
        }
        if(name.equals("value")) {
            if(value) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("on")) {
            setOn((String)value);
            return null;
        }
        if(name.equals("off")) {
            setOff((String)value);
            return null;
        }
        if(name.equals("value")) {
            setValue(((Boolean)value).booleanValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * iOS 7 changed the switch to not include any text
     * @return the noTextMode
     */
    public boolean isNoTextMode() {
        return noTextMode;
    }

    /**
     * iOS 7 changed the switch to not include any text
     * @param noTextMode the noTextMode to set
     */
    public void setNoTextMode(boolean noTextMode) {
        this.noTextMode = noTextMode;
    }
}
