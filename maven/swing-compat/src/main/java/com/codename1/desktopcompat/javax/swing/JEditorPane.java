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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;

/// A multi-line text component with a content type.
///
/// ## What differs from the desktop
///
/// The pane shows plain text only. With the content type `text/html` the
/// markup is kept and answered by [#getText()], while what is shown is the
/// text with the tags taken out, the line breaks of `br`, `p`, `div`, `li`
/// and the headings kept, and the common entities decoded. There are no
/// editor kits, no styled documents, no hyperlink events and no loading of
/// a page from a URL.
public class JEditorPane extends JTextComponent {

    private String contentType = "text/plain";
    private String markup;
    private String shown;

    public JEditorPane() {
        setDocument(new PlainDocument());
    }

    public JEditorPane(String type, String text) {
        this();
        setContentType(type);
        setText(text);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new com.codename1.desktopcompat.rt.TextAreaPeer(this);
    }

    @Override
    protected boolean cn1GrowsWithText() {
        return true;
    }

    public final String getContentType() {
        return contentType;
    }

    /// Sets the type of the content. Parameters after a `;`, such as a
    /// character set, are dropped.
    public final void setContentType(String type) {
        if (type == null) {
            throw new NullPointerException();
        }
        int semi = type.indexOf(';');
        String bare = (semi >= 0 ? type.substring(0, semi) : type).trim();
        String old = contentType;
        contentType = bare;
        firePropertyChange("contentType", old, bare);
    }

    private boolean html() {
        return "text/html".equalsIgnoreCase(contentType);
    }

    @Override
    public void setText(String t) {
        if (html() && t != null) {
            markup = t;
            shown = cn1PlainText(t);
            super.setText(shown);
        } else {
            markup = null;
            shown = null;
            super.setText(t);
        }
    }

    /// The markup that was set, for as long as the text it was shown as
    /// was not edited; otherwise the text.
    @Override
    public String getText() {
        String now = super.getText();
        if (markup != null && now != null && now.equals(shown)) {
            return markup;
        }
        return now;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    private static boolean breaks(String tag) {
        return "br".equals(tag) || "p".equals(tag) || "/p".equals(tag) || "div".equals(tag) || "/div".equals(tag)
                || "li".equals(tag) || "tr".equals(tag) || "/tr".equals(tag) || (tag.length() == 3
                && tag.charAt(0) == '/' && tag.charAt(1) == 'h' && tag.charAt(2) >= '1' && tag.charAt(2) <= '6')
                || (tag.length() == 2 && tag.charAt(0) == 'h' && tag.charAt(1) >= '1' && tag.charAt(1) <= '6');
    }

    /// The text a page of simple markup reads as.
    static String cn1PlainText(String html) {
        StringBuilder out = new StringBuilder();
        int n = html.length();
        boolean space = false;
        boolean skip = false;
        for (int i = 0; i < n; i++) {
            char c = html.charAt(i);
            if (c == '<') {
                int end = html.indexOf('>', i);
                if (end < 0) {
                    break;
                }
                StringBuilder name = new StringBuilder();
                for (int j = i + 1; j < end; j++) {
                    char t = html.charAt(j);
                    if (t == ' ' || t == '\t' || t == '\n' || t == '\r' || (t == '/' && j > i + 1)) {
                        break;
                    }
                    // Tag names are ASCII; folded by hand, not by locale.
                    name.append(t >= 'A' && t <= 'Z' ? (char) (t + 32) : t);
                }
                String tag = name.toString();
                if ("head".equals(tag) || "style".equals(tag) || "script".equals(tag)) {
                    skip = true;
                } else if ("/head".equals(tag) || "/style".equals(tag) || "/script".equals(tag)) {
                    skip = false;
                } else if (!skip && breaks(tag)) {
                    // A break before any text, or right after another one
                    // from a closing tag, adds nothing.
                    int len = out.length();
                    if (len > 0 && ("br".equals(tag) || out.charAt(len - 1) != '\n')) {
                        out.append('\n');
                    }
                    space = false;
                }
                i = end;
            } else if (skip) {
                continue;
            } else if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                space = out.length() > 0 && out.charAt(out.length() - 1) != '\n';
            } else {
                if (space) {
                    out.append(' ');
                    space = false;
                }
                if (c == '&') {
                    int end = html.indexOf(';', i);
                    String entity = end > i && end - i <= 8 ? html.substring(i + 1, end) : null;
                    String decoded = entity == null ? null : decode(entity);
                    if (decoded != null) {
                        out.append(decoded);
                        i = end;
                        continue;
                    }
                }
                out.append(c);
            }
        }
        int len = out.length();
        while (len > 0 && out.charAt(len - 1) == '\n') {
            len--;
        }
        out.setLength(len);
        return out.toString();
    }

    private static String decode(String entity) {
        if ("amp".equals(entity)) {
            return "&";
        } else if ("lt".equals(entity)) {
            return "<";
        } else if ("gt".equals(entity)) {
            return ">";
        } else if ("quot".equals(entity)) {
            return "\"";
        } else if ("apos".equals(entity)) {
            return "'";
        } else if ("nbsp".equals(entity)) {
            return " ";
        } else if (entity.length() > 1 && entity.charAt(0) == '#') {
            try {
                boolean hex = entity.charAt(1) == 'x' || entity.charAt(1) == 'X';
                int code = hex ? Integer.parseInt(entity.substring(2), 16) : Integer.parseInt(entity.substring(1));
                if (code > 0 && code <= 0xffff) {
                    return String.valueOf((char) code);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
