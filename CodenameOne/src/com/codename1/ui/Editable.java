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
package com.codename1.ui;

/**
 * An interface that can be implemented to provide editing capabilities for any component
 * in the UI.  {@link Component} implements this interface, but only with empty
 * methods.  You can provide an alternative editing implementation for any Component by
 * passing an Editable object to {@link Component#setEditingDelegate(com.codename1.ui.Editable) }.
 * @author shannah
 * @since 6.0
 */
public interface Editable {
    /**
     * Checks whether the component is editable.
     * @return 
     */
    public boolean isEditable();
    
    /**
     * Checks whether editing is currently in progress.
     * @return 
     */
    public boolean isEditing();
    
    /**
     * Starts editing the component.
     */
    public void startEditingAsync();
    
    /**
     * Stops editing the component.
     * @param onComplete Optional callback that will be called after the editing
     * is finished (as stopping may take some time for the native side to do cleanup).
     */
    public void stopEditing(Runnable onComplete);
}
