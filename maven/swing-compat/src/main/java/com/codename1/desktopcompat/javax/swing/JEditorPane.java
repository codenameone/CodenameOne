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

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.event.HyperlinkEvent;
import com.codename1.desktopcompat.javax.swing.event.HyperlinkListener;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.rt.MiniHtml;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/// A multi-line text component with a content type.
///
/// With the content type `text/plain` it is a text area. With `text/html`
/// the markup is shown formatted, by the reader the labels use: bold,
/// italic, underline, `font` colors and sizes, headings, paragraphs, line
/// breaks, lists as dashed lines, centered and aligned blocks, the common
/// entities, and links. Lines are wrapped at the pane's width. Anything
/// else in the markup -- tables, images, style sheets -- is read as the
/// text it contains.
///
/// A click on a link fires a `HyperlinkEvent` of the type `ACTIVATED` to
/// the pane's `HyperlinkListener`s, with the link's `href` as its
/// description and, when the `href` is an absolute URL, as its URL.
///
/// ## What differs from the desktop
///
///  - A pane that shows HTML is never edited by the user, whatever
///    `setEditable` says; on the desktop links work only in a pane that is
///    not editable, and that is the one kind there is here. Its document
///    holds the text without the tags, and [#getText()] answers the markup
///    that was set.
///  - There are no editor kits and no styled documents.
///  - [#setPage(URL)] reads the whole page before it returns, where the
///    desktop may load it in the background. A link relative to the page
///    is resolved against it.
///  - Only `ACTIVATED` is fired; there is no `ENTERED` and `EXITED` as
///    the pointer moves over a link.
public class JEditorPane extends JTextComponent {

    private static final int PAD = 3;

    private String contentType = "text/plain";
    private String markup;
    private String shown;
    private MiniHtml.Document page;
    private URL pageUrl;

    public JEditorPane() {
        setDocument(new PlainDocument());
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    public JEditorPane(String type, String text) {
        this();
        setContentType(type);
        setText(text);
    }

    /// A pane that shows the page at `initialPage`.
    public JEditorPane(URL initialPage) throws IOException {
        this();
        setPage(initialPage);
    }

    public JEditorPane(String url) throws IOException {
        this();
        setPage(url);
    }

    public void setPage(String url) throws IOException {
        if (url == null) {
            throw new IOException("invalid url");
        }
        setPage(new URL(url));
    }

    /// Reads the page and shows it: as HTML when its name ends in `.html`
    /// or `.htm`, and as plain text otherwise. The text is read as UTF-8.
    public void setPage(URL page) throws IOException {
        if (page == null) {
            throw new IOException("invalid url");
        }
        InputStream in = page.openStream();
        if (in == null) {
            throw new IOException("cannot read " + page);
        }
        String text;
        try {
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            for (int n = in.read(chunk); n >= 0; n = in.read(chunk)) {
                all.write(chunk, 0, n);
            }
            text = new String(all.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
        String name = page.getPath();
        int len = name == null ? 0 : name.length();
        boolean html = len >= 5 && name.regionMatches(true, len - 5, ".html", 0, 5)
                || len >= 4 && name.regionMatches(true, len - 4, ".htm", 0, 4);
        URL old = pageUrl;
        // Before the text is read: its pictures are found against it.
        pageUrl = page;
        setContentType(html ? "text/html" : "text/plain");
        setText(text);
        firePropertyChange("page", old, page);
    }

    /// The page [#setPage(URL)] showed last, or `null`.
    public URL getPage() {
        return pageUrl;
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
        cn1ApplyMode();
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        cn1ApplyMode();
    }

    @Override
    public void setEditable(boolean b) {
        super.setEditable(b);
        cn1ApplyMode();
    }

    /// Keeps the widget from being edited while the pane shows a page.
    private void cn1ApplyMode() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) p).setEditable(isEditable() && !html());
        }
    }

    /// The page that is showing, or `null` when the pane shows plain
    /// text: its type is not HTML, or the text was changed through the
    /// document since the markup was set.
    private MiniHtml.Document cn1Page() {
        if (!html() || markup == null || page == null) {
            return null;
        }
        String now = super.getText();
        return now != null && now.equals(shown) ? page : null;
    }

    private Insets cn1TextInsets() {
        Insets in = getInsets();
        Insets m = getMargin();
        int t = m == null ? PAD : m.top;
        int l = m == null ? PAD : m.left;
        int b = m == null ? PAD : m.bottom;
        int r = m == null ? PAD : m.right;
        return new Insets(in.top + t, in.left + l, in.bottom + b, in.right + r);
    }

    /// A page is as wide as its longest line and, once the pane has a
    /// width, as high as its lines are when broken at that width.
    @Override
    public Dimension getPreferredSize() {
        MiniHtml.Document doc = isPreferredSizeSet() ? null : cn1Page();
        if (doc == null) {
            return super.getPreferredSize();
        }
        Insets in = cn1TextInsets();
        Font f = getFont();
        Dimension whole = MiniHtml.preferredSize(doc, f);
        int room = getWidth() - in.left - in.right;
        int h = room > 0 ? MiniHtml.wrappedSize(doc, f, room).height : whole.height;
        return new Dimension(whole.width + in.left + in.right, h + in.top + in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        MiniHtml.Document doc = cn1Page();
        if (doc == null) {
            super.paintComponent(g);
            return;
        }
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (isOpaque()) {
            Color bg = isBackgroundSet() || p == null ? getBackground() : new Color(p.getStyle().getBgColor() & 0xffffff);
            if (bg != null) {
                g.setColor(bg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
        Color fg = isForegroundSet() || p == null ? getForeground() : new Color(p.getStyle().getFgColor() & 0xffffff);
        if (fg != null) {
            g.setColor(fg);
        }
        g.setFont(getFont());
        Insets in = cn1TextInsets();
        MiniHtml.paint(g, doc, in.left, in.top, Math.max(1, getWidth() - in.left - in.right), MiniHtml.ALIGN_LEFT);
    }

    // ------------------------------------------------------------ links

    public void addHyperlinkListener(HyperlinkListener listener) {
        listenerList.add(HyperlinkListener.class, listener);
    }

    public void removeHyperlinkListener(HyperlinkListener listener) {
        listenerList.remove(HyperlinkListener.class, listener);
    }

    public HyperlinkListener[] getHyperlinkListeners() {
        return listenerList.getListeners(HyperlinkListener.class);
    }

    public void fireHyperlinkUpdate(HyperlinkEvent e) {
        HyperlinkListener[] ls = getHyperlinkListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].hyperlinkUpdate(e);
        }
    }

    /// The `href` of the link at a point of the pane, or `null`.
    String cn1LinkAt(int x, int y) {
        MiniHtml.Document doc = cn1Page();
        if (doc == null) {
            return null;
        }
        Insets in = cn1TextInsets();
        return MiniHtml.hrefAt(doc, getFont(), Math.max(1, getWidth() - in.left - in.right), MiniHtml.ALIGN_LEFT,
                x - in.left, y - in.top);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.getID() != MouseEvent.MOUSE_CLICKED || e.isConsumed() || !isEnabled()
                || e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        String href = cn1LinkAt(e.getX(), e.getY());
        if (href == null) {
            return;
        }
        URL url = null;
        try {
            url = new URL(href);
        } catch (MalformedURLException notAbsolute) {
            url = cn1Relative(href);
        }
        fireHyperlinkUpdate(new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, url, href));
    }

    /// A relative link against the page that is showing; `null` when the
    /// text was not read from a page, and the listener then gets the link
    /// as the description alone.
    private URL cn1Relative(String href) {
        if (pageUrl == null) {
            return null;
        }
        try {
            return new URL(pageUrl, href);
        } catch (MalformedURLException unusable) {
            return null;
        }
    }

    /// The picture an `<img>` of the page names: an absolute URL as it
    /// is, anything else against the page that is showing. A page read
    /// from a resource of the application finds its pictures among the
    /// resources the same way it finds the pages it links to.
    private com.codename1.desktopcompat.java.awt.Image cn1Image(String src) {
        URL url;
        try {
            url = new URL(src);
        } catch (MalformedURLException notAbsolute) {
            url = cn1Relative(src);
        }
        com.codename1.ui.Image img = com.codename1.desktopcompat.rt.ImageLoader.fromUrl(url);
        return img == null ? null : com.codename1.desktopcompat.rt.ImageLoader.wrap(img);
    }

    private boolean html() {
        return "text/html".equalsIgnoreCase(contentType);
    }

    @Override
    public void setText(String t) {
        if (html() && t != null) {
            markup = t;
            page = MiniHtml.parse(t, new MiniHtml.ImageSource() {
                @Override
                public com.codename1.desktopcompat.java.awt.Image image(String src) {
                    return cn1Image(src);
                }
            });
            shown = cn1PlainText(t);
            super.setText(shown);
        } else {
            markup = null;
            shown = null;
            page = null;
            super.setText(t);
        }
        revalidate();
        repaint();
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
