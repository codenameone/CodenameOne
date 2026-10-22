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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.org.jdesktop.swingx.hyperlink.AbstractHyperlinkAction;
import com.codename1.desktopcompat.org.jdesktop.swingx.hyperlink.HyperlinkAction;
import java.net.URI;

/// A button that looks like a link: no border, no fill, and text in one
/// color before it was clicked and another after.
///
/// The link is a Codename One button with its border and background
/// switched off and its text color set. With an
/// [AbstractHyperlinkAction] the clicked state follows the action's
/// visited flag, and its name; with any other action, or none, a click
/// sets it.
///
/// ## What differs from SwingX
///
///  - The text is not underlined, neither always nor under the pointer.
///  - [#setURI(URI)] opens the address through Codename One; see
///    [HyperlinkAction].
///  - Setting a foreground is overwritten by the next change of the
///    clicked state, as the two link colors are the foreground.
public class JXHyperlink extends JButton {

    private Color unclickedColor = new Color(0, 0x33, 0xFF);
    private Color clickedColor = new Color(0x99, 0, 0x99);
    private boolean clicked;
    private boolean overrulesActionOnClick;
    private PropertyChangeListener actionFollower;

    public JXHyperlink() {
        this(null);
    }

    public JXHyperlink(Action action) {
        super();
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setAction(action);
        cn1ApplyColor();
    }

    /// Makes the link open `uri`, with the address as its text. `null`
    /// leaves a disabled link without text.
    public void setURI(URI uri) {
        setAction(HyperlinkAction.createHyperlinkAction(uri));
    }

    @Override
    public void setAction(Action a) {
        Action old = getAction();
        if (old != null && actionFollower != null) {
            old.removePropertyChangeListener(actionFollower);
        }
        super.setAction(a);
        if (a != null) {
            if (actionFollower == null) {
                actionFollower = createActionPropertyChangeListener(a);
            }
            a.addPropertyChangeListener(actionFollower);
        }
        configurePropertiesFromAction(a);
    }

    public Color getUnclickedColor() {
        return unclickedColor;
    }

    public void setClickedColor(Color color) {
        Color old = clickedColor;
        clickedColor = color;
        if (clicked) {
            cn1ApplyColor();
        }
        firePropertyChange("clickedColor", old, color);
    }

    public Color getClickedColor() {
        return clickedColor;
    }

    public void setUnclickedColor(Color color) {
        Color old = unclickedColor;
        unclickedColor = color;
        if (!clicked) {
            cn1ApplyColor();
        }
        firePropertyChange("unclickedColor", old, color);
    }

    /// Sets whether the link shows as followed.
    public void setClicked(boolean clicked) {
        boolean old = this.clicked;
        this.clicked = clicked;
        if (old != clicked) {
            cn1ApplyColor();
        }
        firePropertyChange("clicked", old, clicked);
    }

    public boolean isClicked() {
        return clicked;
    }

    /// With `true` a click marks the link as clicked whatever its action
    /// says about having been visited.
    public void setOverrulesActionOnClick(boolean overrule) {
        boolean old = overrulesActionOnClick;
        overrulesActionOnClick = overrule;
        firePropertyChange("overrulesActionOnClick", old, overrule);
    }

    public boolean getOverrulesActionOnClick() {
        return overrulesActionOnClick;
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        super.fireActionPerformed(event);
        if (isAutoSetClicked()) {
            setClicked(true);
        }
    }

    /// Whether a click marks the link as clicked by itself: always when
    /// the action is overruled, else unless the action keeps a visited
    /// flag of its own.
    protected boolean isAutoSetClicked() {
        return getOverrulesActionOnClick() || !(getAction() instanceof AbstractHyperlinkAction);
    }

    /// The listener that keeps the link's text and clicked state those of
    /// its action.
    protected PropertyChangeListener createActionPropertyChangeListener(final Action a) {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Action current = getAction();
                if (current == null || evt.getSource() != current) {
                    return;
                }
                String name = evt.getPropertyName();
                if (AbstractHyperlinkAction.VISITED_KEY.equals(name)) {
                    cn1FollowVisited(current);
                } else if (Action.NAME.equals(name)) {
                    Object text = current.getValue(Action.NAME);
                    setText(text instanceof String ? (String) text : "");
                } else if ("enabled".equals(name)) {
                    setEnabled(current.isEnabled());
                }
            }
        };
    }

    /// Takes the clicked state from the action's visited flag.
    protected void configurePropertiesFromAction(Action a) {
        cn1FollowVisited(a);
    }

    private void cn1FollowVisited(Action a) {
        if (a instanceof AbstractHyperlinkAction) {
            setClicked(((AbstractHyperlinkAction<?>) a).isVisited());
        }
    }

    private void cn1ApplyColor() {
        setForeground(clicked ? clickedColor : unclickedColor);
    }
}
