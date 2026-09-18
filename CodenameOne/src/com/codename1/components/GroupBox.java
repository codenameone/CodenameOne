/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// A titled frame around a group of related controls.
///
/// The desktop equivalent of a section header, and the one piece of grouping chrome all
/// three desktop toolkits agree on: `NSBox` with a title, `GtkFrame` with a label widget,
/// and WinUI's headered content. On a phone the same grouping is expressed by a gap and a
/// heading, which is why Codename One never had this -- and why a desktop form built out of
/// plain containers reads as one undifferentiated column of controls.
///
/// Two UIIDs: `GroupBox` styles the frame (its border is the box) and `GroupBoxTitle`
/// styles the caption. A theme that wants the caption to sit *in* the top edge rather than
/// above it does that with a negative top margin on `GroupBoxTitle`; nothing here hard-codes
/// a position, because the three platforms disagree about it.
///
/// ```java
/// GroupBox appearance = new GroupBox("Appearance");
/// appearance.add(new CheckBox("Use the system accent colour"))
///           .add(new CheckBox("Reduce transparency"));
/// ```
///
/// The content is an ordinary `Container`, so `add`, `remove` and the layout all behave as
/// they would anywhere else -- `#getContentPane()` is there for the rare caller that wants
/// the inner container itself.
public class GroupBox extends Container {
    private final Label title = new Label("", "GroupBoxTitle");
    private final Container content;

    /// An untitled group whose content stacks vertically.
    public GroupBox() {
        this("", com.codename1.ui.layouts.BoxLayout.y());
    }

    /// A titled group whose content stacks vertically.
    ///
    /// #### Parameters
    ///
    /// - `titleText`: the caption
    public GroupBox(String titleText) {
        this(titleText, com.codename1.ui.layouts.BoxLayout.y());
    }

    /// A titled group with a layout of its own.
    ///
    /// #### Parameters
    ///
    /// - `titleText`: the caption
    ///
    /// - `contentLayout`: the layout for the grouped controls
    public GroupBox(String titleText, Layout contentLayout) {
        super(new BorderLayout());
        setUIID("GroupBox");
        content = new Container(contentLayout);
        content.setUIID("Container");
        title.setText(titleText);
        // An empty caption must not reserve a strip: an untitled group is a plain box, and a
        // blank label with the title style's padding would leave a gap nothing explains.
        title.setHidden(titleText == null || titleText.length() == 0);
        // super, explicitly: the overrides below route an ordinary add into the content pane,
        // which is what every caller means -- and would put the caption and the content pane
        // itself inside the content pane if these two went through them.
        super.addComponent(BorderLayout.NORTH, title);
        super.addComponent(BorderLayout.CENTER, content);
    }

    /// The caption.
    ///
    /// #### Returns
    ///
    /// the title text, never null
    public String getTitle() {
        return title.getText();
    }

    /// Sets the caption. Setting it empty removes the caption strip entirely.
    ///
    /// #### Parameters
    ///
    /// - `titleText`: the caption
    public void setTitle(String titleText) {
        title.setText(titleText == null ? "" : titleText);
        title.setHidden(title.getText().length() == 0);
    }

    /// The caption component, for a caller that needs to style or replace its icon.
    ///
    /// #### Returns
    ///
    /// the title label
    public Label getTitleComponent() {
        return title;
    }

    /// The container the grouped controls live in.
    ///
    /// #### Returns
    ///
    /// the content container
    public Container getContentPane() {
        return content;
    }

    /// @inheritDoc
    ///
    /// Routed into the content pane, so an ordinary add means "add to the group" rather than
    /// "add beside the caption". `Container#add(Component)` is final and delegates here, so
    /// overriding this covers the chaining form too.
    @Override
    public void addComponent(com.codename1.ui.Component cmp) {
        if (content == null) {
            // During the super constructor, before the content pane exists.
            super.addComponent(cmp);
            return;
        }
        content.addComponent(cmp);
    }

    /// @inheritDoc
    ///
    /// The constraint belongs to the CONTENT layout, not to the BorderLayout that positions
    /// the caption. That layout is an implementation detail, and a caller passing
    /// `BorderLayout.SOUTH` means "below the other controls in this group", never "outside the
    /// box, under the frame".
    @Override
    public void addComponent(Object constraints, com.codename1.ui.Component cmp) {
        if (content == null) {
            super.addComponent(constraints, cmp);
            return;
        }
        content.addComponent(constraints, cmp);
    }

    /// @inheritDoc
    @Override
    public void removeComponent(com.codename1.ui.Component cmp) {
        if (cmp == title || cmp == content) { //NOPMD CompareObjectsWithEquals
            super.removeComponent(cmp);
            return;
        }
        content.removeComponent(cmp);
    }

    /// @inheritDoc
    @Override
    public void removeAll() {
        content.removeAll();
    }
}
