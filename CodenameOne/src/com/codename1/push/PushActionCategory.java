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

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Encapsulates a category for push notifications.  If a push notification specifies a category, and the 
 * app's main class implements {@link PushActionsProvider}, then the push notification will provide a
 * set of buttons to select among the actions available for that category.  If the user selects an 
 * action in the push notification, their choice will be made available inside the {@link PushCallback#push(java.lang.String) }
 * method via the {@link PushContent#getActionId() } method.
 * 
 * <p>Applications that wish to support actions must implement {@link PushActionsProvider} in its main class.  The {@link PushActionsProvider#getPushActionCategories() }
 * implementation defines all of the categories that are available for push notifications.</p>
 * @author shannah
 */
public class PushActionCategory {
    private final String id;
    private List<PushAction> actions;
    
    /**
     * Creates a category with the specified actions.
     * @param id The ID of the category.  Should correspond with the "category" of a push notification.
     * @param actions The actions that are available for this category.
     */
    public PushActionCategory(String id, PushAction... actions) {
        this.actions = new java.util.ArrayList<PushAction>();
        this.id = id;
        this.actions.addAll(Arrays.asList(actions));
    }
    
    /**
     * Gets the actions in this category.  These actions will be manifested as buttons in push notifications
     * directed at this category.
     * @return 
     */
    public PushAction[] getActions() {
        return actions.toArray(new PushAction[actions.size()]);
    }
    
    /**
     * Convenience method to return all of the actions in the provided categories.  
     * @param categories The categories from which to get actions.
     * @return List of actions in all of the provided categories.
     */
    public static PushAction[] getAllActions(PushActionCategory... categories) {
        Set<PushAction> actions = new HashSet<PushAction>();
        for (PushActionCategory cat : categories) {
            actions.addAll(Arrays.asList(cat.getActions()));
        }
        return actions.toArray(new PushAction[actions.size()]);
        
    }
    

    /**
     * Gets the ID of the category.  This corresponds with the category of a push notification.
     * @return the id
     * @see PushContent#getCategory() 
     */
    public String getId() {
        return id;
    }
    
}
