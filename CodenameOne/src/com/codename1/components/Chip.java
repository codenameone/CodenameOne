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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

/// A small rounded label for a tag, a filter or a choice: Material's input,
/// filter and choice chips.
///
/// ```java
/// // filter chips: tap to toggle
/// Container filters = FlowLayout.encloseIn(
///         new Chip("Vegan").setSelectable(true),
///         new Chip("Gluten free").setSelectable(true),
///         new Chip("Under 30 min").setSelectable(true).setSelected(true));
///
/// // an input chip that can be removed
/// Chip tag = new Chip("design").setRemovable(true);
/// tag.addRemoveListener(e -> tag.remove());
/// ```
///
/// A selectable chip toggles on tap and shows a check mark while selected. A
/// removable chip shows a close button, which fires the remove listeners and
/// leaves removing the chip to them. Every tap fires the action listeners.
/// `ChipInput` is a text field that turns what the user types into chips.
///
/// UIIDs: `Chip` and `ChipSelected` for the pill, `ChipText` for its text and
/// `ChipRemove` for the close button. Unstyled, a chip is a rounded pill in the
/// theme's `Slider` track colour, and a selected one in the accent colour.
public class Chip extends Container {
    private final Button text = new Button();
    private final Button removeButton = new Button();
    private final EventDispatcher actionListeners = new EventDispatcher();
    private final EventDispatcher removeListeners = new EventDispatcher();
    private boolean selectable;
    private boolean selected;
    private boolean removable;

    /// A chip showing `text`.
    ///
    /// #### Parameters
    ///
    /// - `text`: the chip's text
    public Chip(String text) {
        super(new BorderLayout());
        setUIID("Chip");
        this.text.setText(text);
        this.text.setUIID(themed("ChipText", "Label"));
        unpadded(this.text);
        removeButton.setUIID(themed("ChipRemove", "Label"));
        unpadded(removeButton);
        removeButton.setHidden(true);
        // The text is the lead component, so a tap anywhere on the pill reaches
        // it; one that lands on the close icon removes instead of selecting.
        this.text.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (removable && evt.getX() >= 0 && removeButton.contains(evt.getX(), evt.getY())) {
                    remove(evt);
                } else {
                    pressed();
                }
            }
        });
        add(BorderLayout.CENTER, this.text).add(BorderLayout.EAST, removeButton);
        setLeadComponent(this.text);
        applyLook();
    }

    private String themed(String uiid, String fallback) {
        return getUIManager().isThemeStyleDefined(uiid) ? uiid : fallback;
    }

    private void unpadded(Component c) {
        if (!getUIManager().isThemeStyleDefined(c.getUIID())) {
            c.getAllStyles().setPadding(0, 0, 0, 0);
            c.getAllStyles().setMargin(0, 0, 0, 0);
            c.getAllStyles().setBgTransparency(0);
        }
    }

    /// The chip's text.
    ///
    /// #### Returns
    ///
    /// the text
    public String getText() {
        return text.getText();
    }

    /// Sets the chip's text.
    ///
    /// #### Parameters
    ///
    /// - `t`: the text
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Chip setText(String t) {
        text.setText(t);
        return this;
    }

    /// The button holding the text, for an icon or a custom look. It is the
    /// chip's lead component, so it receives every tap on the chip.
    ///
    /// #### Returns
    ///
    /// the text button
    public Button getTextLabel() {
        return text;
    }

    /// Whether a tap toggles the chip.
    ///
    /// #### Returns
    ///
    /// true for a filter or choice chip
    public boolean isSelectable() {
        return selectable;
    }

    /// Makes a tap toggle the chip.
    ///
    /// #### Parameters
    ///
    /// - `selectable`: true for a filter or choice chip
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Chip setSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    /// Whether the chip is selected.
    ///
    /// #### Returns
    ///
    /// the selection state
    public boolean isSelected() {
        return selected;
    }

    /// Selects or deselects the chip.
    ///
    /// #### Parameters
    ///
    /// - `selected`: the selection state
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Chip setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            applyLook();
            repaint();
        }
        return this;
    }

    /// Whether the chip shows a close button.
    ///
    /// #### Returns
    ///
    /// true for an input chip
    public boolean isRemovable() {
        return removable;
    }

    /// Shows or hides the close button.
    ///
    /// #### Parameters
    ///
    /// - `removable`: true for an input chip
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Chip setRemovable(boolean removable) {
        this.removable = removable;
        removeButton.setHidden(!removable);
        applyLook();
        return this;
    }

    /// The close icon. It shows the icon only: taps reach the chip's text
    /// button, which removes when the tap lands on this.
    ///
    /// #### Returns
    ///
    /// the close icon
    public Button getRemoveButton() {
        return removeButton;
    }

    /// Adds a listener called on every tap.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        actionListeners.addListener(l);
    }

    /// Removes a tap listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        actionListeners.removeListener(l);
    }

    /// Adds a listener called when the close button is tapped.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener, which usually removes the chip
    public void addRemoveListener(ActionListener l) {
        removeListeners.addListener(l);
    }

    /// Removes a close listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeRemoveListener(ActionListener l) {
        removeListeners.removeListener(l);
    }

    /// Taps the chip as a user would: toggles it when selectable, then fires
    /// the action listeners.
    public void pressed() {
        if (selectable) {
            setSelected(!selected);
        }
        actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed));
    }

    /// Fires the remove listeners as the close button would.
    public void requestRemove() {
        remove(null);
    }

    private void remove(ActionEvent cause) {
        if (cause != null) {
            cause.consume();
        }
        removeListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
    }

    private void applyLook() {
        String uiid = selected ? "ChipSelected" : "Chip";
        if (getUIManager().isThemeStyleDefined(uiid)) {
            setUIID(uiid);
            if (!getUIManager().isThemeStyleDefined("ChipText")) {
                text.getAllStyles().setFgColor(getStyle().getFgColor());
            }
            updateRemoveIcon(getStyle().getFgColor());
            return;
        }
        int bg = selected ? Palette.accent(this, null) : Palette.track(this, null);
        int fg = selected ? (Palette.isDark(bg) ? 0xffffff : 0x000000) : Palette.text(this);
        Style s = getAllStyles();
        s.setBgColor(bg);
        s.setBgTransparency(255);
        s.setBorder(RoundRectBorder.create().cornerRadius(3f));
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(1.2f, 1.2f, 3f, removable ? 1.5f : 3f);
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMargin(0.8f, 0.8f, 0.8f, 0.8f);
        if (!getUIManager().isThemeStyleDefined("ChipText")) {
            text.getAllStyles().setFgColor(fg);
        }
        if (selected) {
            FontImage.setMaterialIcon(text, FontImage.MATERIAL_CHECK, 3f);
        } else {
            text.setIcon(null);
        }
        updateRemoveIcon(fg);
    }

    private void updateRemoveIcon(int fg) {
        if (!removable) {
            return;
        }
        if (!getUIManager().isThemeStyleDefined("ChipRemove")) {
            removeButton.getAllStyles().setFgColor(fg);
            removeButton.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            removeButton.getAllStyles().setPadding(0, 0, 1f, 0);
        }
        FontImage.setMaterialIcon(removeButton, FontImage.MATERIAL_CLOSE, 3f);
    }
}
