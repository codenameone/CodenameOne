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
package com.codename1.desktopcompat.org.jdesktop.swingx.prompt;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXFormattedTextField;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTextArea;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTextField;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;

/// Gives any text component a prompt: text shown while the component is
/// empty.
///
/// The prompt and its looks are kept as client properties of the
/// component, under the keys below, and shown by the hint of the Codename
/// One text field behind it.
///
/// ## What differs from SwingX
///
///  - The hint is drawn by Codename One, which shows it whenever the field
///    is empty: the focus behavior is recorded and reported but
///    [FocusBehavior#HIDE_PROMPT] and [FocusBehavior#HIGHLIGHT_PROMPT]
///    look like [FocusBehavior#SHOW_PROMPT].
///  - The prompt background and background painter are recorded and
///    reported, and not drawn.
///  - A prompt set before the display exists reaches the field only if the
///    component is one of the SwingX text components, which push it when
///    their widget is made; on a plain text component set the prompt once
///    the application has started.
public final class PromptSupport {

    public static final String PROMPT = "promptText";
    public static final String FOREGROUND = "promptForeground";
    public static final String BACKGROUND = "promptBackground";
    public static final String BACKGROUND_PAINTER = "promptBackgroundPainter";
    public static final String FOCUS_BEHAVIOR = "focusBehavior";
    public static final String FONT_STYLE = "promptFontStyle";

    /// What the prompt does while the empty component has the focus.
    public enum FocusBehavior {
        /// The prompt stays.
        SHOW_PROMPT,
        /// The prompt stays, drawn as selected text.
        HIGHLIGHT_PROMPT,
        /// The prompt goes.
        HIDE_PROMPT
    }

    private PromptSupport() {
    }

    /// Sets the prompt and its colors in one call.
    public static void init(String promptText, Color promptForeground, Color promptBackground,
            JTextComponent textComponent) {
        if (promptText != null && promptText.length() > 0) {
            setPrompt(promptText, textComponent);
        }
        if (promptForeground != null) {
            setForeground(promptForeground, textComponent);
        }
        if (promptBackground != null) {
            setBackground(promptBackground, textComponent);
        }
    }

    /// The focus behavior set on the component, [FocusBehavior#HIDE_PROMPT]
    /// when none was.
    public static FocusBehavior getFocusBehavior(JTextComponent textComponent) {
        Object v = textComponent.getClientProperty(FOCUS_BEHAVIOR);
        return v instanceof FocusBehavior ? (FocusBehavior) v : FocusBehavior.HIDE_PROMPT;
    }

    public static void setFocusBehavior(FocusBehavior focusBehavior, JTextComponent textComponent) {
        textComponent.putClientProperty(FOCUS_BEHAVIOR, focusBehavior);
    }

    public static String getPrompt(JTextComponent textComponent) {
        Object v = textComponent.getClientProperty(PROMPT);
        return v instanceof String ? (String) v : null;
    }

    /// Sets the text shown while the component is empty; `null` removes
    /// it.
    public static void setPrompt(String promptText, JTextComponent textComponent) {
        textComponent.putClientProperty(PROMPT, promptText);
        cn1Push(textComponent);
    }

    /// The prompt's color: the one set, else the component's disabled
    /// text color, else `null` for the theme's hint color.
    public static Color getForeground(JTextComponent textComponent) {
        Object v = textComponent.getClientProperty(FOREGROUND);
        if (v instanceof Color) {
            return (Color) v;
        }
        return textComponent.getDisabledTextColor();
    }

    public static void setForeground(Color promptTextColor, JTextComponent textComponent) {
        textComponent.putClientProperty(FOREGROUND, promptTextColor);
        cn1Push(textComponent);
    }

    /// The prompt background set, else the component's background.
    public static Color getBackground(JTextComponent textComponent) {
        Object v = textComponent.getClientProperty(BACKGROUND);
        if (v instanceof Color) {
            return (Color) v;
        }
        return textComponent.getBackground();
    }

    /// Recorded only.
    public static void setBackground(Color background, JTextComponent textComponent) {
        textComponent.putClientProperty(BACKGROUND, background);
    }

    @SuppressWarnings("unchecked")
    public static <T extends JTextComponent> Painter<? super T> getBackgroundPainter(T textComponent) {
        Object v = textComponent.getClientProperty(BACKGROUND_PAINTER);
        return v instanceof Painter ? (Painter<? super T>) v : null;
    }

    /// Recorded only.
    public static <T extends JTextComponent> void setBackgroundPainter(Painter<? super T> background,
            T textComponent) {
        textComponent.putClientProperty(BACKGROUND_PAINTER, background);
    }

    /// Sets the style of the prompt's font, a combination of the `Font`
    /// style constants; `null` is the component's own style.
    public static void setFontStyle(Integer fontStyle, JTextComponent textComponent) {
        textComponent.putClientProperty(FONT_STYLE, fontStyle);
        cn1Push(textComponent);
    }

    public static Integer getFontStyle(JTextComponent textComponent) {
        Object v = textComponent.getClientProperty(FONT_STYLE);
        return v instanceof Integer ? (Integer) v : null;
    }

    /// Writes the prompt, its color and its font style to the hint of the
    /// Codename One text field behind the component. Does nothing before
    /// the display exists.
    public static void cn1Push(JTextComponent textComponent) {
        if (!Display.isInitialized()) {
            return;
        }
        com.codename1.ui.Component p = textComponent.cn1PeerOrNull();
        if (p == null) {
            if (textComponent instanceof JXTextField || textComponent instanceof JXTextArea
                    || textComponent instanceof JXFormattedTextField) {
                // These push again when their widget is made.
                return;
            }
            p = textComponent.cn1Peer();
        }
        if (!(p instanceof TextArea)) {
            return;
        }
        TextArea t = (TextArea) p;
        String prompt = getPrompt(textComponent);
        if (prompt == null && t.getHintLabel() == null) {
            return;
        }
        t.setHint(prompt == null ? "" : prompt);
        Label hint = t.getHintLabel();
        if (hint == null) {
            return;
        }
        Object fg = textComponent.getClientProperty(FOREGROUND);
        if (fg instanceof Color) {
            hint.getAllStyles().setFgColor(((Color) fg).getRGB() & 0xffffff);
        }
        Integer style = getFontStyle(textComponent);
        Font f = textComponent.getFont();
        if (style != null && f != null) {
            Font styled = f.deriveFont(style.intValue());
            hint.getAllStyles().setFont(Fonts.nativeFont(styled, styled.getSize2D() * Units.scale()));
        }
        textComponent.repaint();
    }
}
