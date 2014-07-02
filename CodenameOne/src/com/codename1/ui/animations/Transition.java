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
package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;

/**
 * Represents a transition animation between two forms this class is used internally
 * by Display to play an animation when moving from one form to the next. A transition
 * can be installed on a  {@link com.codename1.ui.Form}  object using the in/out transitions, for ease of use
 * {@link com.codename1.ui.plaf.LookAndFeel} has support for default transitions. 
 *
 * @author Shai Almog
 */
public abstract class Transition implements Animation {

    private Component source;
    
    private Component destination;
    
    /**
     * Invoked by {@link com.codename1.ui.Display} to set the source and destination forms.
     * This method should not be invoked by developers.
     * 
     * @param source the source form from which the transition originates
     * @param destination the destination form to which the transition will lead
     */
    public final void init(Component source, Component destination){
        this.source = source;
        this.destination = destination;
        if (source != null && source instanceof Container) {
            ((Container)source).layoutContainer();
        }
        if (destination != null && destination instanceof Container) {
            ((Container)destination).layoutContainer();
        }
    }
    
    /**
     * Callback thats invoked before a transition begins, the source form may be null
     * for the first form in the application.
     */
    public void initTransition(){
    }

    /**
     * Returns the destination form that should be set once animation is completed
     * 
     * @return the destination component
     */
    public final Component getDestination(){
        return destination;
    }
    
    /**
     * Returns the source form which is the form from which the animation is starting.
     * This may be null for the first form in the application
     * 
     * @return the source component
     */
    public final Component getSource(){
        return source;
    }

    /**
     * Optional operation to cleanup the garbage left over by a running transition
     */
    public void cleanup() {
        source = null;
        destination = null;
    }
    
    /**
     * Create a copy of the transition, usually the transition used is a copy.
     *
     * @param reverse creates a new transition instance with "reverse" behavior useful
     * for signifying "back" operations
     * @return new transition instance
     */
    public Transition copy(boolean reverse) {
        // for compatibility with older transitions
        return this;
    }
    
    /**
     * Allows setting the source form to null to save memory if the transition doesn't need
     * it in memory.
     */
    protected final void cleanSource() {
        source = null;
    }
    
    /**
     * @inheritDoc
     */
    public abstract boolean animate();
    
    /**
     * @inheritDoc
     */
    public abstract void paint(Graphics g);
}
