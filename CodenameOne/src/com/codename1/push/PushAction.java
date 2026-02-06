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

/// Encapsulates a push notification action.  Available actions for a push notification are defined
/// by the `PushActionsProvider#getPushActionCategories()` implementation.
///
/// @author Steve Hannah
public class PushAction {
    private final String id;
    private final String title;
    private final String icon;
    private final String textInputPlaceholder;
    private final String textInputButtonText;

    /// Creates a new push action
    ///
    /// #### Parameters
    ///
    /// - `id`: @param id    The ID of the action.  This is the id that will be available in `PushContent#getActionId()` if the user
    /// selected this action on the push notification.
    ///
    /// - `title`: The title of the action.  This is the button label in the push notification.
    ///
    /// - `icon`: Icon for the action.  Not supported currently on most platforms.
    public PushAction(String id, String title, String icon) {
        this(id, title, icon, null, null);

    }

    /// Creates a new push action
    ///
    /// #### Parameters
    ///
    /// - `id`: @param id                   The ID of the action.  This is the id that will be available in `PushContent#getActionId()` if the user
    /// selected this action on the push notification.
    ///
    /// - `title`: The title of the action.  This is the button label in the push notification.
    ///
    /// - `icon`: Icon for the action.  Not supported currently on most platforms.
    ///
    /// - `textInputPlaceholder`: Placeholder text to use for the text input field.
    ///
    /// - `textInputButtonText`: Text to be used for the "reply" button in the text input field.
    public PushAction(String id, String title, String icon, String textInputPlaceholder, String textInputButtonText) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.textInputPlaceholder = textInputPlaceholder;
        this.textInputButtonText = textInputButtonText;

    }

    /// Creates a new push action
    ///
    /// #### Parameters
    ///
    /// - `id`: @param id    The ID of the action.  This is the id that will be available in `PushContent#getActionId()` if the user
    /// selected this action on the push notification.
    ///
    /// - `title`: The title of the action.  This is the button label in the push notification.
    public PushAction(String id, String title) {
        this(id, title, null);
    }

    /// Creates a new push action
    ///
    /// #### Parameters
    ///
    /// - `title`: The title and id of the action.  This is the button label in the push notification.
    public PushAction(String title) {
        this(title, title);
    }

    /// Gets the ID of the action.  This is the value that will be made available inside the `PushCallback#push(java.lang.String)`
    /// method via `PushContent#getActionId()` if the user selects this action in a push notification.
    ///
    /// #### Returns
    ///
    /// the id The ID of the action.
    ///
    /// #### See also
    ///
    /// - PushContent#getActionId()
    public String getId() {
        return id;
    }

    /// The title of the action.  This will be the label for the action's button in the push notification.
    ///
    /// #### Returns
    ///
    /// the title The title of the action.
    public String getTitle() {
        return title;
    }

    /// The icon of the action.  Not supported yet on most platforms.
    ///
    /// #### Returns
    ///
    /// the icon The icon for the action.
    public String getIcon() {
        return icon;
    }

    /// The placeholder text to use for text input on this action.  Either `#textInputButtonText` or `#textInputPlaceholder`
    /// must be non-null for the action to include a text input.
    ///
    /// #### Returns
    ///
    /// the textInputPlaceholder
    public String getTextInputPlaceholder() {
        return textInputPlaceholder;
    }

    /// The button label for the "reply" button on the text input field.  Either `#textInputButtonText` or `#textInputPlaceholder`
    /// must be non-null for the action to include a text input.
    ///
    /// #### Returns
    ///
    /// the textInputButtonText
    public String getTextInputButtonText() {
        return textInputButtonText;
    }
}
