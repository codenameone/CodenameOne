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
package com.codename1.ui.events;

import com.codename1.ui.Command;
import com.codename1.ui.Component;

/**
 * Event object delivered when an {@link ActionListener} callback is invoked
 * 
 * @author Chen Fishbein
 */
public class ActionEvent {

    private boolean consumed;
    
    private Object source;
    private Object sourceComponent;
    
    private int keyEvent = -1;
    private int y = -1;
    private boolean longEvent = false;
    /**
     * Creates a new instance of ActionEvent
     * @param source element for the action event
     */
    public ActionEvent(Object source) {
        this.source = source;
    }

    /**
     * Creates a new instance of ActionEvent
     * @param source element for the action event
     * @param keyEvent the key that triggered the event
     */
    public ActionEvent(Object source, int keyEvent) {
        this.source = source;
        this.keyEvent = keyEvent;
    }

    /**
     * Creates a new instance of ActionEvent
     * @param source element for the action event
     * @param keyEvent the key that triggered the event
     * @param longClick true if the event is triggered from long pressed
     */
    public ActionEvent(Object source, int keyEvent, boolean longClick) {
        this.source = source;
        this.keyEvent = keyEvent;
        this.longEvent = longClick;
    }
    
    /**
     * Creates a new instance of ActionEvent as a pointer event
     *
     * @param source element for the pointer event
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     * @param longPointer true if the event is triggered from long pressed
     */
    public ActionEvent(Object source, int x, int y, boolean longPointer) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
        this.longEvent = longPointer;
    }
    
    /**
     * Creates a new instance of ActionEvent as a pointer event
     *
     * @param source element for the pointer event
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     */
    public ActionEvent(Object source, int x, int y) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
    }

    /**
     * Creates a new instance of ActionEvent for a command
     *
     * @param source element command
     * @param sourceComponent the triggering component
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     */
    public ActionEvent(Command source, Component sourceComponent, int x, int y) {
        this.source = source;
        this.sourceComponent = sourceComponent;
        this.keyEvent = x;
        this.y = y;
    }

    /**
     * The element that triggered the action event, useful for decoupling event
     * handling code
     * @return the element that triggered the action event
     */
    public Object getSource(){
        return source;
    }

    /**
     * If this event was triggered by a key press this method will return the 
     * appropriate keycode
     * @return the key that triggered the event
     */
    public int getKeyEvent() {
        return keyEvent;
    }

    /**
     * If this event was sent as a result of a command action this method returns
     * that command
     * @return the command action that triggered the action event
     */
    public Command getCommand() {
        if(source instanceof Command) {
            return (Command)source;
        }
        return null;
    }

    /**
     * Returns the source component object
     * @return a component
     */
    public Component getComponent() {
        if(sourceComponent != null) {
            return (Component)sourceComponent;
        }
        if(source instanceof Component) {
            return (Component)source;
        }
        return null;
    }
    
    /**
     * Consume the event indicating that it was handled thus preventing other action
     * listeners from handling/receiving the event
     */
    public void consume() {
        consumed = true;
    }
    
    /**
     * Returns true if the event was consumed thus indicating that it was handled.
     * This prevents other action listeners from handling/receiving the event
     * 
     * @return true if the event was consumed
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * The X position if this is a pointer event otherwise undefined
     *
     * @return x position
     */
    public int getX() {
        return keyEvent;
    }


    /**
     * The Y position if this is a pointer event otherwise undefined
     *
     * @return y position
     */
    public int getY() {
        return y;
    }
    
    /**
     * Returns true for long click or long pointer event
     */ 
    public boolean isLongEvent(){
        return longEvent;
    }
}
