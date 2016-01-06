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
	
	// [ddyer 1/2016] adds subtype annotations to actionevents.  The general philosophy
	// is that existing consumers of actionevents who do not know about these type indicators
	// will not see any differences, so this change will be innocuous to the existing code
	//
	// there's evidence of a lot of ad-hoc use of the available state of actionevents
	// in the absence of these subtypes, 
	
	/**
	 * The event type, as declared when the event is created.
	 * 
	 * @author Ddyer
	 *
	 */
	public enum Type {

		Other,					// unspecified, someone called one of the old undifferentiated constructors.
		Command,				// some type of command 
		Pointer, PointerPressed, PointerReleased, PointerDrag, Swipe,	// pointer activity
		KeyPress, KeyRelease,	// key activity
		Exception, Response,Progress,Data, 	// network activity
		Calendar,			// calendar
		Edit,Done,			// text area
		File,JavaScript,Log,	// file system
		Theme, Show, SizeChange, OrientationChange	// window status changes
		} ;
	private Type trigger;
	public Type getEventType() { return(trigger); }
	
    private boolean consumed;
    
    private Object source;
    private Object sourceComponent;
    
    private int keyEvent = -1;
    private int y = -1;
    private boolean longEvent = false;
    
    /**
     * Creates a new instance of ActionEvent.  This is unused locally, but provided so existing customer code
     * with still work.
     * @param source element for the action event
     * @param the type of the event
     */
    public ActionEvent(Object source) {
        this.source = source;
        this.trigger = Type.Other;
    }
    
    /**
     * Creates a new instance of ActionEvent
     * @param source element for the action event
     * @param the {@link Type } of the event
     */
    public ActionEvent(Object source,Type type) {
        this.source = source;
        this.trigger = type;
    }

    /**
     * Creates a new instance of ActionEvent as a pointer event
     *
     * @param source element for the pointer event
     * @param the {@link Type } of the event
     * @param x (or sometimes width) associated with the event
     * @param y (or sometimes height)associated with the event
     */
    public ActionEvent(Object source, Type type, int x, int y) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
        this.trigger = type;
    }
    
    /**
     * Creates a new instance of ActionEvent for a command
     *
     * @param source element command
     * @param the {@link Type } of the event
     * @param sourceComponent the triggering component
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     */
    public ActionEvent(Command source, Type type, Component sourceComponent, int x, int y) {
        this.source = source;
        this.sourceComponent = sourceComponent;
        this.keyEvent = x;
        this.y = y;
        this.trigger = type;
    }
    /**
     * Creates a new instance of ActionEvent for a drop operation
     *
     * @param dragged the dragged component
     * @param the {@link Type } of the event
     * @param drop the drop target component
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     */
    public ActionEvent(Component dragged, Type type, Component drop, int x, int y) {
        this.source = dragged;
        this.sourceComponent = drop;
        this.keyEvent = x;
        this.y = y;
        this.trigger = type;
    }
    
    
    /**
     * Creates a new instance of ActionEvent.  The key event is really just
     * a numeric code, not indicative of a key press
     * @param source element for the action event
     * @param the {@link Type } of the event
     * @param keyEvent the key that triggered the event
     */
    public ActionEvent(Object source, Type type , int keyEvent) {
        this.source = source;
        this.keyEvent = keyEvent;
        this.trigger = type;
    }
    
    
    /**
     * Creates a new instance of ActionEvent
     * @param source element for the action event
     * @param keyEvent the key that triggered the event
     */
    public ActionEvent(Object source, int keyEvent) {
        this.source = source;
        this.keyEvent = keyEvent;
        this.trigger = Type.KeyRelease;
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
        this.trigger = Type.KeyPress;
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
        this.trigger = Type.PointerReleased;
    }
    
    /**
     * Creates a new instance of ActionEvent as a generic pointer event.  
     *
     * @param source element for the pointer event
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     */
    public ActionEvent(Object source, int x, int y) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
        this.trigger = Type.Pointer;
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
        this.trigger = Type.Command;
    }

    /**
     * Creates a new instance of ActionEvent for a drop operation
     *
     * @param dragged the dragged component
     * @param drop the drop target component
     * @param x the x position of the pointer event
     * @param y the y position of the pointer event
     */
    public ActionEvent(Component dragged, Component drop, int x, int y) {
        this.source = dragged;
        this.sourceComponent = drop;
        this.keyEvent = x;
        this.y = y;
        this.trigger = Type.PointerDrag;
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
    
    /**
     * Set in the case of a drop listener, returns the component being dragged
     * @return the component being dragged
     */
    public Component getDraggedComponent() {
        return (Component)source;
    }
    
    /**
     * Set in the case of a drop listener, returns the component on which the drop occurs
     * @return the component on which the drop occurs
     */
    public Component getDropTarget() {
        return (Component)sourceComponent;
    }
}
