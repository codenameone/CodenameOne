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

/**
 * Event callback interface invoked when a {@link com.codename1.ui.list.ListModel}
 * changes its state thus indicating to the view that it should refresh.
 * 
 * @author Chen Fishbein
 */
public interface DataChangedListener {
    /**
     * Type value for removed data in ListModel
     */
    public static int REMOVED = 0;
    
    /**
     * Type value for added data in ListModel
     */
    public static int ADDED = 1;
    
    /**
     * Type value for changed data in ListModel
     */
    public static int CHANGED = 2;
    
    /**
     * Invoked when there was a change in the underlying model
     * 
     * @param type the type data change; REMOVED, ADDED or CHANGED
     * @param index item index in a list model
     */
    public void dataChanged(int type, int index);
    
}
