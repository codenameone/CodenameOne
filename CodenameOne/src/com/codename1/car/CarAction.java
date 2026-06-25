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

/// A tappable action -- a button shown in a `CarActionStrip`, a header action, or an action attached
/// to a `CarPaneTemplate` / `CarMessageTemplate`. Maps to `androidx.car.app.model.Action` and to a
/// `CPBarButton` / `CPAlertAction` on CarPlay. Use the fluent setters to configure it:
///
/// ```java
/// CarAction refresh = new CarAction("Refresh")
///         .setIcon(refreshIcon)
///         .setOnAction(ctx -> screen.invalidate());
/// ```
public class CarAction {
    private String title;
    private Image icon;
    private CarColor backgroundColor = CarColor.DEFAULT;
    private CarActionListener listener;

    /// Creates an empty action. Set at least a title or an icon before use.
    public CarAction() {
    }

    /// Creates an action with the supplied title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the action label
    public CarAction(String title) {
        this.title = title;
    }

    /// Returns the action label, or null if icon-only.
    public String getTitle() {
        return title;
    }

    /// Sets the action label.
    ///
    /// #### Parameters
    ///
    /// - `title`: the label
    ///
    /// #### Returns
    ///
    /// this action, for chaining
    public CarAction setTitle(String title) {
        this.title = title;
        return this;
    }

    /// Returns the action icon, or null.
    public Image getIcon() {
        return icon;
    }

    /// Sets the action icon. The head unit renders it at a fixed size with its own tint.
    ///
    /// #### Parameters
    ///
    /// - `icon`: the icon image
    ///
    /// #### Returns
    ///
    /// this action, for chaining
    public CarAction setIcon(Image icon) {
        this.icon = icon;
        return this;
    }

    /// Returns the requested background colour role.
    public CarColor getBackgroundColor() {
        return backgroundColor;
    }

    /// Sets the background colour role (the head unit may ignore it for legibility).
    ///
    /// #### Parameters
    ///
    /// - `backgroundColor`: the colour role
    ///
    /// #### Returns
    ///
    /// this action, for chaining
    public CarAction setBackgroundColor(CarColor backgroundColor) {
        this.backgroundColor = backgroundColor == null ? CarColor.DEFAULT : backgroundColor;
        return this;
    }

    /// Returns the activation listener, or null.
    public CarActionListener getOnAction() {
        return listener;
    }

    /// Sets the listener invoked (on the EDT) when the action is selected.
    ///
    /// #### Parameters
    ///
    /// - `listener`: the activation callback
    ///
    /// #### Returns
    ///
    /// this action, for chaining
    public CarAction setOnAction(CarActionListener listener) {
        this.listener = listener;
        return this;
    }
}
