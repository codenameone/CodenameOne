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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/// An empty state (also called a zero state, blank slate or "no results"
/// view): an icon, a title, a line of explanation and an optional action,
/// centred where a list or a screen would otherwise be blank.
///
/// Show one when loading finished with nothing to show, or failed:
///
/// ```java
/// list.removeAll();
/// if (orders.isEmpty()) {
///     list.add(new EmptyState(FontImage.MATERIAL_INBOX, "No orders yet",
///             "Orders you place show up here."));
/// }
///
/// // or, after an error
/// list.add(new EmptyState(FontImage.MATERIAL_CLOUD_OFF, "Can't load orders",
///         "Check your connection and try again.")
///         .setAction("Retry", e -> reload()));
/// ```
///
/// It is the counterpart of `Skeleton`, which fills the same place while the
/// content is still loading.
///
/// UIIDs: `EmptyState` for the container, `EmptyStateIcon`, `EmptyStateTitle`,
/// `EmptyStateText` and `EmptyStateButton`. Unstyled, they fall back to the
/// theme's `Label`, `SpanLabel` and `Button` looks.
public class EmptyState extends Container {
    private final Label icon = new Label();
    private final SpanLabel title = new SpanLabel();
    private final SpanLabel text = new SpanLabel();
    private final Button action = new Button();

    /// An empty state with a material icon, a title and an explanation.
    ///
    /// #### Parameters
    ///
    /// - `materialIcon`: a `FontImage` material icon constant, or 0 for none
    ///
    /// - `title`: the title, or null
    ///
    /// - `text`: the explanation, or null
    public EmptyState(char materialIcon, String title, String text) {
        super(BoxLayout.y());
        setUIID("EmptyState");
        icon.setUIID(themed("EmptyStateIcon", "Label"));
        this.title.setTextUIID(themed("EmptyStateTitle", "Label"));
        this.text.setTextUIID(themed("EmptyStateText", "SpanLabelText"));
        this.title.setUIID("Container");
        this.text.setUIID("Container");
        this.title.getTextComponent().setAlignment(Component.CENTER);
        this.text.getTextComponent().setAlignment(Component.CENTER);
        action.setUIID(themed("EmptyStateButton", "Button"));
        setIcon(materialIcon);
        setTitle(title);
        setText(text);
        action.setHidden(true);
        add(FlowLayout.encloseCenter(icon)).add(this.title).add(this.text)
                .add(FlowLayout.encloseCenter(action));
    }

    private String themed(String uiid, String fallback) {
        return getUIManager().isThemeStyleDefined(uiid) ? uiid : fallback;
    }

    /// Sets the icon.
    ///
    /// #### Parameters
    ///
    /// - `materialIcon`: a `FontImage` material icon constant, or 0 for none
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public final EmptyState setIcon(char materialIcon) {
        if (materialIcon == 0) {
            icon.setIcon(null);
            icon.setHidden(true);
        } else {
            FontImage.setMaterialIcon(icon, materialIcon, 12f);
            icon.setHidden(false);
        }
        return this;
    }

    /// Sets the title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title, or null for none
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public final EmptyState setTitle(String title) {
        this.title.setText(title == null ? "" : title);
        this.title.setHidden(title == null);
        return this;
    }

    /// Sets the explanation.
    ///
    /// #### Parameters
    ///
    /// - `text`: the explanation, or null for none
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public final EmptyState setText(String text) {
        this.text.setText(text == null ? "" : text);
        this.text.setHidden(text == null);
        return this;
    }

    /// Adds a button under the explanation, such as "Retry" or "Add an item".
    ///
    /// #### Parameters
    ///
    /// - `label`: the button's text
    ///
    /// - `listener`: what the button does
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public EmptyState setAction(String label, ActionListener listener) {
        action.setText(label);
        action.addActionListener(listener);
        action.setHidden(false);
        return this;
    }

    /// The action button, hidden until `#setAction(String, ActionListener)`.
    ///
    /// #### Returns
    ///
    /// the button
    public Button getActionButton() {
        return action;
    }
}
