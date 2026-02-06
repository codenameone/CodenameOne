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
import com.codename1.ui.Container;

/// Event object delivered when an `ActionListener` callback is invoked
///
/// @author Chen Fishbein
public class ActionEvent {

    private final Type trigger;
    private final Object source;
    private boolean consumed;
    private Object sourceComponent;
    private int keyEvent = -1;
    private int y = -1;
    private boolean longEvent = false;
    /// A flag to ease the pain caused by fix for https://github.com/codenameone/CodenameOne/issues/2352 which prevents
    /// us from receiving new pointer pressed events while a drag is in progress.  This flag may be added to a pointer dragged
    /// event to indicate that the pointer was pressed again - in case your listener wants to monitor for this.
    ///
    /// #### Since
    ///
    /// 8.0
    private boolean pointerPressedDuringDrag;

    /// Creates a new instance of ActionEvent.  This is unused locally, but provided so existing customer code
    /// with still work.
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the action event
    public ActionEvent(Object source) {
        this.source = source;
        this.trigger = Type.Other;
    }

    /// Creates a new instance of ActionEvent
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the action event
    ///
    /// - `type`: the `Type` of the event
    public ActionEvent(Object source, Type type) {
        this.source = source;
        this.trigger = type;
        if (type == Type.LongPointerPress) {
            longEvent = true;
        }
    }

    /// Creates a new instance of ActionEvent as a pointer event
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the pointer event
    ///
    /// - `type`: the `Type` of the event
    ///
    /// - `x`: (or sometimes width) associated with the event
    ///
    /// - `y`: (or sometimes height)associated with the event
    public ActionEvent(Object source, Type type, int x, int y) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
        this.trigger = type;
        if (type == Type.LongPointerPress) {
            longEvent = true;
        }
    }

    /// Creates a new instance of ActionEvent for a command
    ///
    /// #### Parameters
    ///
    /// - `source`: element command
    ///
    /// - `type`: the `Type` of the event
    ///
    /// - `sourceComponent`: the triggering component
    ///
    /// - `x`: the x position of the pointer event
    ///
    /// - `y`: the y position of the pointer event
    public ActionEvent(Command source, Type type, Component sourceComponent, int x, int y) {
        this.source = source;
        this.sourceComponent = sourceComponent;
        this.keyEvent = x;
        this.y = y;
        this.trigger = type;
        if (type == Type.LongPointerPress) {
            longEvent = true;
        }
    }

    /// Creates a new instance of ActionEvent for a drop operation
    ///
    /// #### Parameters
    ///
    /// - `dragged`: the dragged component
    ///
    /// - `type`: the `Type` of the event
    ///
    /// - `drop`: the drop target component
    ///
    /// - `x`: the x position of the pointer event
    ///
    /// - `y`: the y position of the pointer event
    public ActionEvent(Component dragged, Type type, Component drop, int x, int y) {
        this.source = dragged;
        this.sourceComponent = drop;
        this.keyEvent = x;
        this.y = y;
        this.trigger = type;
        if (type == Type.LongPointerPress) {
            longEvent = true;
        }
    }

    /// Creates a new instance of ActionEvent.  The key event is really just
    /// a numeric code, not indicative of a key press
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the action event
    ///
    /// - `type`: the `Type` of the event
    ///
    /// - `keyEvent`: the key that triggered the event
    public ActionEvent(Object source, Type type, int keyEvent) {
        this.source = source;
        this.keyEvent = keyEvent;
        this.trigger = type;
        if (type == Type.LongPointerPress) {
            longEvent = true;
        }
    }

    /// Creates a new instance of ActionEvent
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the action event
    ///
    /// - `keyEvent`: the key that triggered the event
    public ActionEvent(Object source, int keyEvent) {
        this.source = source;
        this.keyEvent = keyEvent;
        this.trigger = Type.KeyRelease;
    }


    /// Creates a new instance of ActionEvent
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the action event
    ///
    /// - `keyEvent`: the key that triggered the event
    ///
    /// - `longClick`: true if the event is triggered from long pressed
    public ActionEvent(Object source, int keyEvent, boolean longClick) {
        this.source = source;
        this.keyEvent = keyEvent;
        this.longEvent = longClick;
        this.trigger = Type.KeyPress;
    }


    /// Creates a new instance of ActionEvent as a pointer event
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the pointer event
    ///
    /// - `x`: the x position of the pointer event
    ///
    /// - `y`: the y position of the pointer event
    ///
    /// - `longPointer`: true if the event is triggered from long pressed
    public ActionEvent(Object source, int x, int y, boolean longPointer) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
        this.longEvent = longPointer;
        this.trigger = Type.PointerReleased;
    }

    /// Creates a new instance of ActionEvent as a generic pointer event.
    ///
    /// #### Parameters
    ///
    /// - `source`: element for the pointer event
    ///
    /// - `x`: the x position of the pointer event
    ///
    /// - `y`: the y position of the pointer event
    public ActionEvent(Object source, int x, int y) {
        this.source = source;
        this.keyEvent = x;
        this.y = y;
        this.trigger = Type.Pointer;
    }

    /// Creates a new instance of ActionEvent for a command
    ///
    /// #### Parameters
    ///
    /// - `source`: element command
    ///
    /// - `sourceComponent`: the triggering component
    ///
    /// - `x`: the x position of the pointer event
    ///
    /// - `y`: the y position of the pointer event
    public ActionEvent(Command source, Component sourceComponent, int x, int y) {
        this.source = source;
        this.sourceComponent = sourceComponent;
        this.keyEvent = x;
        this.y = y;
        this.trigger = Type.Command;
    }

    /// Creates a new instance of ActionEvent for a drop operation
    ///
    /// #### Parameters
    ///
    /// - `dragged`: the dragged component
    ///
    /// - `drop`: the drop target component
    ///
    /// - `x`: the x position of the pointer event
    ///
    /// - `y`: the y position of the pointer event
    public ActionEvent(Component dragged, Component drop, int x, int y) {
        this.source = dragged;
        this.sourceComponent = drop;
        this.keyEvent = x;
        this.y = y;
        this.trigger = Type.PointerDrag;
    }

    /// Returns the type of the given event allowing us to have more generic event handling code and useful
    /// for debugging
    ///
    /// #### Returns
    ///
    /// the Type enum
    public Type getEventType() {
        return (trigger);
    }

    /// The element that triggered the action event, useful for decoupling event
    /// handling code
    ///
    /// #### Returns
    ///
    /// the element that triggered the action event
    public Object getSource() {
        return source;
    }

    /// If this event was triggered by a key press this method will return the
    /// appropriate keycode
    ///
    /// #### Returns
    ///
    /// the key that triggered the event
    public int getKeyEvent() {
        return keyEvent;
    }

    /// Returns the numeric progress of native browser loading on Android
    ///
    /// #### Returns
    ///
    /// the progress value
    public int getProgress() {
        return keyEvent;
    }

    /// If this event was sent as a result of a command action this method returns
    /// that command
    ///
    /// #### Returns
    ///
    /// the command action that triggered the action event
    public Command getCommand() {
        if (source instanceof Command) {
            return (Command) source;
        }
        return null;
    }

    /// Identical to `ActionEvent#getComponent()` except for the fact that a lead component will be returned
    /// if such a lead component is available. This is important for components such as `com.codename1.components.MultiButton`
    /// which will return the underlying button instead.
    ///
    /// #### Returns
    ///
    /// the component that sent the event
    public Component getActualComponent() {
        Component c = getComponent();
        if (c != null) {
            Container lead;
            if (c instanceof Container) {
                lead = ((Container) c).getLeadParent();
            } else {
                lead = c.getParent().getLeadParent();
            }
            if (lead != null) {
                return lead;
            }
        }
        return c;
    }

    /// Returns the component that generated the event. **important** this might not be the actual component.
    /// In case of a lead component such as `com.codename1.components.MultiButton` the underlying
    /// `com.codename1.ui.Button` will be returned and not the `com.codename1.components.MultiButton`
    /// itself. To get the component that you would logically think of as the source component use the `#getActualComponent`
    /// method.
    ///
    /// If you are in doubt use the `getActualComponent` method.
    ///
    /// #### Returns
    ///
    /// a component
    ///
    /// #### See also
    ///
    /// - ActionEvent#getActualComponent() - you should probably use `getActualComponent` instead of this method
    public Component getComponent() {
        if (sourceComponent != null) {
            return (Component) sourceComponent;
        }
        if (source instanceof Component) {
            return (Component) source;
        }
        return null;
    }

    /// Consume the event indicating that it was handled thus preventing other action
    /// listeners from handling/receiving the event
    public void consume() {
        consumed = true;
    }

    /// Returns true if the event was consumed thus indicating that it was handled.
    /// This prevents other action listeners from handling/receiving the event
    ///
    /// #### Returns
    ///
    /// true if the event was consumed
    public boolean isConsumed() {
        return consumed;
    }

    /// The X position if this is a pointer event otherwise undefined
    ///
    /// #### Returns
    ///
    /// x position
    public int getX() {
        return keyEvent;
    }

    /// The Y position if this is a pointer event otherwise undefined
    ///
    /// #### Returns
    ///
    /// y position
    public int getY() {
        return y;
    }

    /// Returns true for long click or long pointer event
    public boolean isLongEvent() {
        return longEvent;
    }

    /// Set in the case of a drop listener, returns the component being dragged
    ///
    /// #### Returns
    ///
    /// the component being dragged
    public Component getDraggedComponent() {
        return (Component) source;
    }

    /// Set in the case of a drop listener, returns the component on which the drop occurs
    ///
    /// #### Returns
    ///
    /// the component on which the drop occurs
    public Component getDropTarget() {
        return (Component) sourceComponent;
    }

    /// Only used for pointer dragged events.  This flag is set to true if a pointer press event
    /// was silently swallowed by a drag in progress.  While dragging and scrolling is in progress,
    /// pointer pressed events are not delivered.  This allows you to detect if a pointer press occurred
    /// during a scroll or drag.
    ///
    /// #### Since
    ///
    /// 8.0
    public boolean isPointerPressedDuringDrag() {
        return pointerPressedDuringDrag;
    }

    /// Only used for pointer dragged events.  This flag is set to true if a pointer press event
    /// was silently swallowed by a drag in progress.  While dragging and scrolling is in progress,
    /// pointer pressed events are not delivered.  This allows you to detect if a pointer press occurred
    /// during a scroll or drag.
    ///
    /// #### Since
    ///
    /// 8.0
    public void setPointerPressedDuringDrag(boolean pressed) {
        this.pointerPressedDuringDrag = pressed;
    }

    /// The event type, as declared when the event is created.
    ///
    /// @author Ddyer
    public enum Type {
        /// Unspecified command type, this occurs when one of the old undifferentiated constructors was invoked
        Other,

        /// Triggered by a command
        Command,

        /// Pointer event that doesn't necessarily fall into one of the other pointer event values
        Pointer,

        /// Pointer event
        PointerPressed,

        /// Pointer event
        PointerReleased,


        /// Pointer event
        PointerDrag,


        /// Pointer swipe event currently fired by `com.codename1.ui.SwipeableContainer#addSwipeOpenListener(com.codename1.ui.events.ActionListener)`
        Swipe,

        /// Fired by key events
        KeyPress,


        /// Fired by key events
        KeyRelease,

        /// Network event fired in case of a network error
        Exception,

        /// Network event fired in case of a network response code event
        Response,

        /// Network event fired in case of progress update
        Progress,

        /// Network event fired in case of a network response containing data
        Data,

        /// Event from `com.codename1.ui.Calendar`
        Calendar,

        /// Fired on a `com.codename1.ui.TextArea` action event
        Edit,

        /// Fired on a `com.codename1.ui.TextField#setDoneListener(com.codename1.ui.events.ActionListener)` action event
        Done,

        /// Fired by the `com.codename1.javascript.JavascriptContext`
        JavaScript,

        /// Logging event to used for log/filesystem decoupling
        Log,

        /// Fired when the theme changes
        Theme,

        /// Fired when a `com.codename1.ui.Form` is shown
        Show,


        /// Fired when a `int)` occurs
        SizeChange,

        /// Fired when a `com.codename1.ui.Form` is rotated
        OrientationChange,


        /// Fired when a component drag is finished
        DragFinished,

        Change,

        LongPointerPress,

        OpenGallery,

        IsGalleryTypeSupported,
    }
}
