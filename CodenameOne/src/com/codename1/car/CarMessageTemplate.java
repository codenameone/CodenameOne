/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.car;

import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.List;

/// A short message with an icon and action buttons -- an error/empty state, a permission prompt, or
/// (for communication apps) an incoming-message card with read-aloud and voice-reply actions. Maps
/// to `androidx.car.app.model.MessageTemplate` and `CPInformationTemplate` / a CarPlay messaging
/// list item.
///
/// Communication apps must additionally hold the platform messaging entitlement
/// (`ios.carplay.messaging` build hint on iOS; the `androidx.car.app.category.MESSAGING` intent
/// filter the builder injects on Android). Without it the message still renders but voice reply is
/// unavailable.
public class CarMessageTemplate extends CarTemplate {
    private String message;
    private Image icon;
    private boolean loading;
    private final List<CarAction> actions = new ArrayList<CarAction>();
    private CarActionStrip headerActions;

    /// Creates an empty message template.
    public CarMessageTemplate() {
    }

    /// Creates a message template with the supplied body text.
    ///
    /// #### Parameters
    ///
    /// - `message`: the body text
    public CarMessageTemplate(String message) {
        this.message = message;
    }

    /// Sets the header title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the header title
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarMessageTemplate setTitle(String title) {
        setTitleInternal(title);
        return this;
    }

    /// Returns the body text.
    public String getMessage() {
        return message;
    }

    /// Sets the body text shown to (and optionally read aloud to) the driver.
    ///
    /// #### Parameters
    ///
    /// - `message`: the body text
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarMessageTemplate setMessage(String message) {
        this.message = message;
        return this;
    }

    /// Returns the leading icon, or null.
    public Image getIcon() {
        return icon;
    }

    /// Sets the leading icon (e.g. the sender avatar, an error glyph).
    ///
    /// #### Parameters
    ///
    /// - `icon`: the icon image
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarMessageTemplate setIcon(Image icon) {
        this.icon = icon;
        return this;
    }

    /// Adds an action button (e.g. "Reply", "Mark read", "Retry"). The head unit shows at most two.
    ///
    /// #### Parameters
    ///
    /// - `action`: the action button
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarMessageTemplate addAction(CarAction action) {
        if (action != null) {
            actions.add(action);
        }
        return this;
    }

    /// Returns the action buttons, in order.
    public List<CarAction> getActions() {
        return actions;
    }

    /// Returns the header action strip, or null.
    public CarActionStrip getHeaderActions() {
        return headerActions;
    }

    /// Sets the header action strip.
    ///
    /// #### Parameters
    ///
    /// - `headerActions`: the action strip
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarMessageTemplate setHeaderActions(CarActionStrip headerActions) {
        this.headerActions = headerActions;
        return this;
    }

    /// Returns true when the template should render a loading spinner instead of content.
    public boolean isLoading() {
        return loading;
    }

    /// Marks the template as loading; the head unit renders a spinner.
    ///
    /// #### Parameters
    ///
    /// - `loading`: true to show a spinner
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarMessageTemplate setLoading(boolean loading) {
        this.loading = loading;
        return this;
    }
}
