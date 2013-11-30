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
package com.codename1.ui.util;

import com.codename1.cloud.BindTarget;
import com.codename1.ui.*;
import com.codename1.ui.events.*;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

/**
 * Handles event dispatching while guaranteeing that all events would
 * be fired properly on the EDT regardless of their source. This class handles listener
 * registration/removal in a safe and uniform way. 
 * 
 * @author Shai Almog
 */
public class EventDispatcher {

    private boolean blocking = false;
    private ArrayList<Object> listeners;
    private Object[] pending;
    private Object pendingEvent;
    boolean actionListenerArray;
    boolean styleListenerArray;
    boolean bindTargetArray;
    boolean dataChangeListenerArray;
    boolean focusListenerArray;
    boolean selectionListenerArray;

    private static boolean fireStyleEventsOnNonEDT = false;
    
    /**
     * When set to true, style events will be dispatched even from non-EDT threads.
     * When set to false, when in non-EDT threads, style events will not be dispatched at all (And developer has to make sure changes will be reflected by calling revalidate after all the changes)
     *
     * Default is false. Setting this to true results in a performance penalty, and it is better instead to simply aggregate events performed on non-EDT threads and when all are over - call revalidate on the relevant container.
     *
     * @param fire true to fire on non-EDT, false otherwise
     */
    public static void setFireStyleEventsOnNonEDT(boolean fire) {
        fireStyleEventsOnNonEDT = fire;
    }
    
    class CallbackClass implements Runnable {
        private Object[] iPending;
        private Object iPendingEvent;
        public CallbackClass() {
            if(!blocking) {
                iPendingEvent = pendingEvent;
                iPending = pending;
            }
        }

        /**
         * Do not invoke this method it handles the dispatching internally and serves
         * as an implementation detail
         */
        public final void run() {
            if(!Display.getInstance().isEdt()) {
                throw new IllegalStateException("This method should not be invoked by external code!");
            }

            if(blocking) {
                iPendingEvent = pendingEvent;
                iPending = pending;
            }

            if(styleListenerArray) {
                Object[] p = (Object[])iPendingEvent;
                fireStyleChangeSync((StyleListener[])iPending, (String)p[0], (Style)p[1]);
                pendingEvent = null;
                pending = null;
                return;
            }

            if(actionListenerArray) {
                fireActionSync((ActionListener[])iPending, (ActionEvent)iPendingEvent);
                return;
            }

            if(focusListenerArray) {
                fireFocusSync((FocusListener[])iPending, (Component)iPendingEvent);
                return;
            }

            if(dataChangeListenerArray) {
                fireDataChangeSync((DataChangedListener[])iPending, ((int[])iPendingEvent)[0], ((int[])iPendingEvent)[1]);
                return;
            }

            if(selectionListenerArray) {
                fireSelectionSync((SelectionListener[])iPending, ((int[])iPendingEvent)[0], ((int[])iPendingEvent)[1]);
                return;
            }

            if(bindTargetArray) {
                Object[] a = (Object[])iPendingEvent;
                fireBindTargetChangeSync((BindTarget[])iPending, (Component)a[0], (String)a[1], a[2], a[3]);
                return;
            }
        }
    };

    private final Runnable callback = new CallbackClass();
    
    /**
     * Add a listener to the dispatcher that would receive the events when they occurs
     * 
     * @param listener a dispatcher listener to add
     */
    public synchronized void addListener(Object listener) {
        if(listener != null) {
            if(listeners == null) {
                listeners = new ArrayList<Object>();
            }
            if(!listeners.contains(listener)){
                listeners.add(listener);
            }        
        }
    }
    
    /**
     * Returns the vector of the listeners
     * 
     * @return the vector of listeners attached to the event dispatcher
     * @deprecated use getListenerCollection instead, this method will now be VERY SLOW
     */
    public Vector getListenerVector() {
        return new Vector(listeners);
    }

    /**
     * Returns the collection of the listeners
     * 
     * @return the collection of listeners attached to the event dispatcher
     */
    public Collection getListenerCollection() {
        return listeners;
    }
    
    /**
     * Remove the listener from the dispatcher
     *
     * @param listener a dispatcher listener to remove
     */
    public synchronized void removeListener(Object listener) {
        if(listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Fires the event safely on the EDT without risk of concurrency errors
     * 
     * @param index the index of the event
     * @param type the type of the event
     */
    public void fireDataChangeEvent(int index, int type) {
        if(listeners == null || listeners.size() == 0) {
            return;
        }
        boolean isEdt = Display.getInstance().isEdt();
        // minor optimization for a common use case to avoid allocation costs
        if(isEdt && listeners.size() == 1) {
            DataChangedListener a = (DataChangedListener)listeners.get(0);
            a.dataChanged(type, index);
            return;
        }
        DataChangedListener[] array;
        synchronized(this) {
            array = new DataChangedListener[listeners.size()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = (DataChangedListener)listeners.get(iter);
            }
        }
        // if we already are on the EDT just fire the event
        if(isEdt) {
            fireDataChangeSync(array, type, index);
        } else {
            dataChangeListenerArray = true;
            pending = array;
            pendingEvent = new int[] {type, index};
            if(blocking) {
                Display.getInstance().callSeriallyAndWait(callback);
            } else {
                Display.getInstance().callSerially(new CallbackClass());
            }
            pending = null;
            pendingEvent = null;
        }
    }
    
    /**
     * Fired when a property of the component changes to a new value
     * 
     * @param source the source component
     * @param propertyName the name of the property
     * @param oldValue the old value of the property
     * @param newValue the new value for the property
     */
    public void fireBindTargetChange(Component source, String propertyName, Object oldValue, Object newValue) {
        if(listeners == null || listeners.size() == 0) {
            return;
        }
        BindTarget[] array;
        synchronized(this) {
            array = new BindTarget[listeners.size()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = (BindTarget)listeners.get(iter);
            }
        }
        // if we already are on the EDT just fire the event
        if(Display.getInstance().isEdt()) {
            fireBindTargetChangeSync(array, source, propertyName, oldValue, newValue);
        } else {
            bindTargetArray = true;
            pending = array;
            pendingEvent = new Object[] {source, propertyName, oldValue, newValue};
            if(blocking) {
                Display.getInstance().callSeriallyAndWait(callback);
            } else {
                Display.getInstance().callSerially(new CallbackClass());
            }
            pending = null;
            pendingEvent = null;
        }
    }
    
    /**
     * Fired when a property of the component changes to a new value
     * 
     * @param source the source component
     * @param propertyName the name of the property
     * @param oldValue the old value of the property
     * @param newValue the new value for the property
     */
    private void fireBindTargetChangeSync(BindTarget[] arr, Component source, String propertyName, Object oldValue, Object newValue) {
        for(int iter = 0 ; iter < arr.length ; iter++) {
            arr[iter].propertyChanged(source, propertyName, oldValue, newValue);
        }
    }

    /**
     * Fires the style change even to the listeners
     *
     * @param property the property name for the event
     * @param source the style firing the event
     */
    public void fireStyleChangeEvent(String property, Style source) {
        if(listeners == null || listeners.size() == 0) {
            return;
        }
        // minor optimization for a common use case to avoid allocation costs
        boolean isEdt = Display.getInstance().isEdt();
        if(isEdt && listeners.size() == 1) {
            StyleListener a = (StyleListener)listeners.get(0);
            a.styleChanged(property, source);
            return;
        }
        StyleListener[] array;
        synchronized(this) {
            array = new StyleListener[listeners.size()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = (StyleListener)listeners.get(iter);
            }
        }
        // if we already are on the EDT just fire the event
        if(isEdt) {
            fireStyleChangeSync(array, property, source);
        } else if (fireStyleEventsOnNonEDT) {
            styleListenerArray = true;
            pending = array;
            pendingEvent = new Object[] {property, source};
            Display.getInstance().callSerially(new CallbackClass());
            pending = null;
            pendingEvent = null;
        }
    }

    /**
     * Synchronious internal call for common code
     */
    private void fireDataChangeSync(DataChangedListener[] array, int type, int index) {
        for(int iter = 0 ; iter < array.length ; iter++) {
            array[iter].dataChanged(type, index);
        }
    }
    
    /**
     * Synchronious internal call for common code
     */
    private void fireStyleChangeSync(StyleListener[] array, String property, Style source) {
        for(int iter = 0 ; iter < array.length ; iter++) {
            array[iter].styleChanged(property, source);
        }
    }

    /**
     * Synchronious internal call for common code
     */
    private void fireSelectionSync(SelectionListener[] array, int oldSelection, int newSelection) {
        for(int iter = 0 ; iter < array.length ; iter++) {
            array[iter].selectionChanged(oldSelection, newSelection);
        }
    }
    
    /**
     * Fires the event safely on the EDT without risk of concurrency errors
     * 
     * @param ev the ActionEvent to fire to the listeners
     */
    public void fireActionEvent(ActionEvent ev) {
        if(listeners == null || listeners.size() == 0) {
            return;
        }
        
        // minor optimization for a common use case to avoid allocation costs
        boolean isEdt = Display.getInstance().isEdt();
        if(isEdt && listeners.size() == 1) {
            ActionListener a = (ActionListener)listeners.get(0);
            a.actionPerformed(ev);
            return;
        }
        ActionListener[] array;
        synchronized(this) {
            array = new ActionListener[listeners.size()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = (ActionListener)listeners.get(iter);
            }
        }
        // if we already are on the EDT just fire the event
        if(isEdt) {
            fireActionSync(array, ev);
        } else {
            actionListenerArray = true;
            pending = array;
            pendingEvent = ev;
            if(blocking) {
                Display.getInstance().callSeriallyAndWait(callback);
            } else {
                Display.getInstance().callSerially(new CallbackClass());
            }
            pending = null;
            pendingEvent = null;
            
        }
    }


    /**
     * Fires the event safely on the EDT without risk of concurrency errors
     * 
     * @param oldSelection old selection
     * @param newSelection new selection
     */
    public void fireSelectionEvent(int oldSelection, int newSelection) {
        if(listeners == null || listeners.size() == 0) {
            return;
        }
        // minor optimization for a common use case to avoid allocation costs
        boolean isEdt = Display.getInstance().isEdt();
        if(isEdt && listeners.size() == 1) {
            SelectionListener a = (SelectionListener)listeners.get(0);
            a.selectionChanged(oldSelection, newSelection);
            return;
        }
        SelectionListener[] array;
        synchronized(this) {
            array = new SelectionListener[listeners.size()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = (SelectionListener)listeners.get(iter);
            }
        }
        // if we already are on the EDT just fire the event
        if(isEdt) {
            fireSelectionSync(array, oldSelection, newSelection);
        } else {
            selectionListenerArray = true;
            pending = array;
            pendingEvent = new int[] {oldSelection, newSelection};
            if(blocking) {
                Display.getInstance().callSeriallyAndWait(callback);
            } else {
                Display.getInstance().callSerially(new CallbackClass());
            }
            pending = null;
            pendingEvent = null;            
        }
    }
    
    /**
     * Synchronous internal call for common code
     */
    private void fireActionSync(ActionListener[] array, ActionEvent ev) {
        for(int iter = 0 ; iter < array.length ; iter++) {
            if(ev == null || !ev.isConsumed()) {
                array[iter].actionPerformed(ev);
            }
        }
    }
    
    /**
     * Fires the event safely on the EDT without risk of concurrency errors
     * 
     * @param c the Component that gets the focus event
     */
    public void fireFocus(Component c) {
        if(listeners == null || listeners.size() == 0) {
            return;
        }
        // minor optimization for a common use case to avoid allocation costs
        boolean isEdt = Display.getInstance().isEdt();
        if(isEdt && listeners.size() == 1) {
            FocusListener a = (FocusListener)listeners.get(0);
            if(c.hasFocus()) {
                a.focusGained(c);
            } else {
                a.focusLost(c);
            }
            return;
        }
        FocusListener[] array;
        synchronized(this) {
            array = new FocusListener[listeners.size()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = (FocusListener)listeners.get(iter);
            }
        }
        // if we already are on the EDT just fire the event
        if(isEdt) {
            fireFocusSync(array, c);
        } else {
            focusListenerArray = true;
            pending = array;
            pendingEvent = c;
            if(blocking) {
                Display.getInstance().callSeriallyAndWait(callback);
            } else {
                Display.getInstance().callSerially(new CallbackClass());
            }
            pending = null;
            pendingEvent = null;            
        }
    }
    
    /**
     * Synchronous internal call for common code
     */
    private void fireFocusSync(FocusListener[] array, Component c) {
        if(c.hasFocus()) {
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter].focusGained(c);
            }
        } else {
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter].focusLost(c);
            }
        }
    }

    /**
     * Returns true if the event dispatcher has registered listeners 
     * 
     * @return true if the event dispatcher has registered listeners 
     */
    public boolean hasListeners() {
        return listeners != null && listeners.size() > 0;
    }

    /**
     * Indicates whether this dispatcher blocks when firing events or not, normally
     * a dispatcher uses callSeriallyAndWait() to be 100% synchronous with event delivery
     * however this method is very slow. By setting blocking to false the callSerially
     * method is used which allows much faster execution for IO heavy operations.
     *
     * @return the blocking state
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Indicates whether this dispatcher blocks when firing events or not, normally
     * a dispatcher uses callSeriallyAndWait() to be 100% synchronous with event delivery
     * however this method is very slow. By setting blocking to false the callSerially
     * method is used which allows much faster execution for IO heavy operations.
     * 
     * @param blocking the blocking value
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
}
