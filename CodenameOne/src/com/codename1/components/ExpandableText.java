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
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Long text shown as its first few lines, ending in "...", with a "Read more"
/// link that expands it in place and a "Show less" link that collapses it
/// again.
///
/// ![A collapsed paragraph with a More link](https://www.codenameone.com/developer-guide/img/generated/components-expandable-text.png)
///
/// ```java
/// ExpandableText about = new ExpandableText(longDescription, 3);
/// form.add(about);
/// ```
///
/// The link appears only when the text needs more lines than the limit at the
/// width it was given, so the same component can hold a short description
/// without offering to expand it. Change the link texts with
/// `#setLinkText(String, String)`; they go through the localization bundle.
///
/// UIIDs: `ExpandableText` for the container, `ExpandableTextBody` for the
/// text and `ExpandableTextLink` for the link, falling back to `Label` and
/// a borderless `Button` look.
public class ExpandableText extends Container {
    private final SpanLabel body = new SpanLabel();
    private final Button link = new Button();
    private int maxLines;
    private boolean expanded;
    private String moreText = "Read more";
    private String lessText = "Show less";

    /// Text collapsed to three lines.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    public ExpandableText(String text) {
        this(text, 3);
    }

    /// Text collapsed to `maxLines` lines.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    ///
    /// - `maxLines`: how many lines show while collapsed, at least 1
    public ExpandableText(String text, int maxLines) {
        super(BoxLayout.y());
        setUIID("ExpandableText");
        body.setUIID("Container");
        body.setTextUIID(themed("ExpandableTextBody", "Label"));
        link.setUIID(themed("ExpandableTextLink", "Label"));
        if (!getUIManager().isThemeStyleDefined("ExpandableTextLink")) {
            link.getAllStyles().setFgColor(Palette.accent(this, null));
        }
        link.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setExpanded(!expanded);
            }
        });
        setMaxLines(maxLines);
        setText(text);
        add(body).add(BorderLayout.west(link));
        updateLink();
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    private String themed(String uiid, String fallback) {
        return getUIManager().isThemeStyleDefined(uiid) ? uiid : fallback;
    }

    /// The text.
    ///
    /// #### Returns
    ///
    /// the full text
    public String getText() {
        return body.getText();
    }

    /// Sets the text.
    ///
    /// #### Parameters
    ///
    /// - `text`: the full text
    public final void setText(String text) {
        body.setText(text == null ? "" : text);
        revalidateLater();
    }

    /// How many lines show while collapsed.
    ///
    /// #### Returns
    ///
    /// the line limit
    public int getMaxLines() {
        return maxLines;
    }

    /// Sets how many lines show while collapsed.
    ///
    /// #### Parameters
    ///
    /// - `maxLines`: at least 1
    public final void setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        applyLimit();
    }

    /// True while the whole text shows.
    ///
    /// #### Returns
    ///
    /// whether the text is expanded
    public boolean isExpanded() {
        return expanded;
    }

    /// Expands or collapses the text.
    ///
    /// #### Parameters
    ///
    /// - `expanded`: true for the whole text
    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            this.expanded = expanded;
            applyLimit();
            updateLink();
            revalidateLater();
        }
    }

    /// Sets the texts of the link.
    ///
    /// #### Parameters
    ///
    /// - `more`: shown while collapsed, "Read more" by default
    ///
    /// - `less`: shown while expanded, "Show less" by default
    public void setLinkText(String more, String less) {
        moreText = more;
        lessText = less;
        updateLink();
    }

    /// The link that expands and collapses the text.
    ///
    /// #### Returns
    ///
    /// the link button
    public Button getLink() {
        return link;
    }

    /// The label holding the text.
    ///
    /// #### Returns
    ///
    /// the text
    public SpanLabel getBody() {
        return body;
    }

    /// True when the text needs more lines than the limit at the current width.
    ///
    /// #### Returns
    ///
    /// whether there is anything to expand
    public boolean isTruncatable() {
        TextArea ta = body.getTextComponent();
        return ta.getWidth() > 0 && ta.getLines() > maxLines;
    }

    private void applyLimit() {
        TextArea ta = body.getTextComponent();
        ta.setGrowLimit(expanded ? -1 : maxLines);
        ta.setEndsWith3Points(!expanded);
    }

    private void updateLink() {
        link.setText(expanded ? lessText : moreText);
    }

    @Override
    public void layoutContainer() {
        super.layoutContainer();
        // Whether there is anything to expand depends on the width, which is
        // known only now. Collapsed text that fits needs no link; expanded
        // text always keeps one, so it can be collapsed again.
        boolean hide = !expanded && !isTruncatable();
        if (hide != link.isHidden()) {
            link.setHidden(hide);
            revalidateLater();
        }
    }
}
