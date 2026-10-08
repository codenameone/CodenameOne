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
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.org.jdesktop.swingx.prompt.PromptSupport;
import com.codename1.desktopcompat.org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior;

/// A text field with a prompt: text shown while the field is empty.
///
/// The prompt is the hint of the Codename One text field; see
/// [PromptSupport] for what of its looks is honored. Buddy components and
/// the outer margin are absent.
public class JXTextField extends JTextField {

    public JXTextField() {
        this(null);
    }

    public JXTextField(String promptText) {
        this(promptText, null);
    }

    public JXTextField(String promptText, Color promptForeground) {
        this(promptText, promptForeground, null);
    }

    public JXTextField(String promptText, Color promptForeground, Color promptBackground) {
        PromptSupport.init(promptText, promptForeground, promptBackground, this);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        PromptSupport.cn1Push(this);
    }

    public FocusBehavior getFocusBehavior() {
        return PromptSupport.getFocusBehavior(this);
    }

    public String getPrompt() {
        return PromptSupport.getPrompt(this);
    }

    public Color getPromptForeground() {
        return PromptSupport.getForeground(this);
    }

    public Color getPromptBackground() {
        return PromptSupport.getBackground(this);
    }

    public Integer getPromptFontStyle() {
        return PromptSupport.getFontStyle(this);
    }

    public void setFocusBehavior(FocusBehavior focusBehavior) {
        PromptSupport.setFocusBehavior(focusBehavior, this);
    }

    public void setPrompt(String labelText) {
        PromptSupport.setPrompt(labelText, this);
    }

    public void setPromptForeground(Color promptTextColor) {
        PromptSupport.setForeground(promptTextColor, this);
    }

    /// Recorded only. The name is SwingX's own spelling.
    public void setPromptBackround(Color color) {
        PromptSupport.setBackground(color, this);
    }

    public void setPromptFontStyle(Integer fontStyle) {
        PromptSupport.setFontStyle(fontStyle, this);
    }
}
