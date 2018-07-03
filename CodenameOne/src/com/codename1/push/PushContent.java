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

import com.codename1.ui.Display;

/**
 * Encapsulates the content of a Push message.  Use this class inside the {@link PushCallback#push(java.lang.String) }
 * method to retrieve details of the push, including title, body, image URL, category, type, and metadata if available.
 * @author Steve Hannah
 */
public class PushContent {
    private static String PROP_PREFIX = "com.codename1.push.prop.";

    private String title, body, imageUrl, category, metaData, actionId;
    private int type;
    
    private PushContent() {
        title = p("title", null);
        body = p("body", null);
        imageUrl = p("imageUrl", null);
        category = p("category", null);
        metaData = p("metaData", null);
        actionId = p("actionId", null);
    }
    
    private static String[] keys() {
        return new String[] {"title", "body", "imageUrl", "category", "metaData", "actionId"};
    }
    
    /**
     * Checks if there is pending push content to retrieve.
     * @return True if there is pending push content to retrieve.
     */
    public static boolean exists() {
        return anyProperties(keys()); 
    }
    
    /**
     * Gets the most recent push notification content if it exists.  This functions like Stack.pop()
     * for a single-element stack.  I.e. In addition to returning the PushContent, that push content
     * is removed from the queue.  
     * <p>
     * {@code
     * if (PushContent.exists()) {
     *     PushContent content = PushContent.get();
     *        // content should be non-null
     *     PushContent content2 = PushContent.get();
     *        // content2 should be null
     *     PushContent.exists(); // false
     * }
     * }
     * </p>
     * @return Pending push content if it exists.
     */
    public static PushContent get() {
        if (exists()) {
            PushContent next = new PushContent();
            clearAll(keys());
            return next;
        }
        return null;
    }
    
    
    
    private static String p(String propName, String defaultVal) {
        return Display.getInstance().getProperty(PROP_PREFIX+propName, defaultVal);
    }
    
    private static void setProperty(String propName, String value) {
        Display.getInstance().setProperty(PROP_PREFIX+propName, value);
    }
    
    private static void clearAll(String... propNames) {
        for (String propName : propNames) {
            setProperty(propName, null);
        }
    }
    
    /**
     * Resets the push content.  After calling this, {@link PushContent#exists()} will return {@literal false}.
     */
    public static void reset() {
        clearAll(keys());
    }
    
    private static boolean anyProperties(String... propNames) {
        for (String propName : propNames) {
            if (p(propName, null) != null) return true;
        }
        return false;
    }

    /**
     * Gets the title of the Push content.
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the pending push content.  
     * @param title the title to set
     * @deprecated For internal use only.
     */
    public static void setTitle(String title) {
        setProperty("title", title);
    }

    /**
     * Gets the body of the push content.
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body of pending push content.
     * @param body the body to set
     * @deprecated For internal use only
     */
    public static void setBody(String body) {
        setProperty("body", body);
    }

    /**
     * Gets the image URL of the push content.
     * @return the imageUrl
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the image URL of pending push content.
     * @param imageUrl the imageUrl to set
     * @deprecated For internal use only.
     */
    public static void setImageUrl(String imageUrl) {
        
        setProperty("imageUrl", imageUrl);
    }

    /**
     * Gets category of the push content.
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the pending push content.
     * @param category the category to set
     * @deprecated For internal use only.
     */
    public static void setCategory(String category) {
        setProperty("category", category);
    }

    /**
     * Gets the metadata associated with push.  This is hidden content not shown to the user.
     * @return the metaData
     */
    public String getMetaData() {
        return metaData;
    }

    /**
     * Sets the metadata of pending push content.
     * @param metaData the metaData to set
     * @deprecated For internal use only.
     */
    public static void setMetaData(String metaData) {
        setProperty("metaData", metaData);
    }

    /**
     * Gets the type of the push content.
     * @return the type of the push content.
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the type of the push content.
     * @param type the type to set
     * @deprecated For internal use only.
     */
    public static void setType(int type) {
        setProperty("type", ""+type);
    }
    
    /**
     * If the user selected an action on the push notification, then the ID of the selected action will be stored in the PushContent's
     * actionId.  If the user did not tap an action, then this will be null.
     * @return The action ID that was selected by the user.
     * @see PushActionsProvider
     * @see PushActionCategory
     * @see PushAction
     */
    public String getActionId() {
        return actionId;
    }
    
    
    /**
     * Sets the action ID of the push content.  The action ID is only set if the user tapped on one of the actions
     * in the push notification; and, if set, it will be set to the ID of the action that was selected.
     * @param actionId The ID of the action that was selected by the user.
     * @deprecated For internal use only.
     * @see PushActionsProvider
     * @see PushActionCategory
     * @see PushAction
     */
    public static void setActionId(String actionId) {
        setProperty("actionId", actionId);
    }
}
