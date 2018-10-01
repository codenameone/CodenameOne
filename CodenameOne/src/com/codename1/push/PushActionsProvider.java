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
package com.codename1.push;


/**
 * This callback interface is invoked when a push notification with a "category" is received 
 * by the application. If the main class of the application implements 
 * {@link #getPushActionCategories() }, then the user may be presented with a set of options/actions
 * on the push notification.  The selected action ID would be made available inside the {@link PushCallback#push(java.lang.String) }
 * callback via the {@link PushContent#getActionId() } method.
 * @author Steve Hannah
 */
public interface PushActionsProvider {
    
    /**
     * Returns the available categories for push notifications that this app responds to.  If the app
     * receives a push notification with a "category" designation matching the {@link PushActionCategory#getId() } of 
     * one of these categories, then the actions in that category will be presented to the user as options (on supported platforms).
     * <p>If the user selects one of these actions, their choice will be made available inside the {@link PushCallback#push(java.lang.String) }
     * callback via the {@link PushContent#getActionId() } method.</p>
     * @return Array of action categories supported by the app.
     */
    public PushActionCategory[] getPushActionCategories();
    
    
}
