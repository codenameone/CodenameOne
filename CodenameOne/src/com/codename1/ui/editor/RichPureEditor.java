/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import java.util.ArrayList;
import java.util.List;

/// The pure rich text editor backend. It maps the `RichTextArea` command / query vocabulary onto a
/// `RichView`, converting the HTML exchanged with the application to and from the editor's inline / block
/// model.
public class RichPureEditor extends PureEditor {
    private RichView rich;

    /// Creates a rich text editor backend.
    public RichPureEditor(EditorHost host, String editorType) {
        super(host, editorType);
        rich = (RichView) view();
    }

    @Override
    protected EditorView createView(EditorHost host, boolean codeMode) {
        return new RichView(host);
    }

    @Override
    public void cmd(String name, String arg) {
        if ("setHtml".equals(name)) {
            HtmlImporter.Result r = HtmlImporter.parse(arg == null ? "" : arg);
            rich.importContent(r.getText(), r.getStyles(), r.getBlocks(), r.getLinks(),
                    loadImages(r.getImageSources()), r.getImageSources());
            return;
        }
        if ("insertHtml".equals(name)) {
            HtmlImporter.Result r = HtmlImporter.parse(arg == null ? "" : arg);
            rich.insertContent(r.getText(), r.getStyles(), r.getBlocks(), r.getLinks(),
                    loadImages(r.getImageSources()), r.getImageSources(), r.hasBlockContent());
            return;
        }
        if ("insertImage".equals(name)) {
            com.codename1.ui.Image img = loadImage(arg);
            if (img != null) {
                rich.insertImageObject(img, arg);
            }
            return;
        }
        if ("setPlaceholder".equals(name)) {
            rich.setPlaceholder(arg);
            return;
        }
        if ("bold".equals(name)) {
            rich.toggleBold();
            return;
        }
        if ("italic".equals(name)) {
            rich.toggleItalic();
            return;
        }
        if ("underline".equals(name)) {
            rich.toggleUnderline();
            return;
        }
        if ("strikeThrough".equals(name)) {
            rich.toggleStrike();
            return;
        }
        if ("insertOrderedList".equals(name)) {
            rich.setList(RichBlocks.LIST_ORDERED);
            return;
        }
        if ("insertUnorderedList".equals(name)) {
            rich.setList(RichBlocks.LIST_UNORDERED);
            return;
        }
        if ("indent".equals(name)) {
            rich.indentBlocks();
            return;
        }
        if ("outdent".equals(name)) {
            rich.outdentBlocks();
            return;
        }
        if ("justifyLeft".equals(name)) {
            rich.setAlign(RichBlocks.ALIGN_LEFT);
            return;
        }
        if ("justifyCenter".equals(name)) {
            rich.setAlign(RichBlocks.ALIGN_CENTER);
            return;
        }
        if ("justifyRight".equals(name)) {
            rich.setAlign(RichBlocks.ALIGN_RIGHT);
            return;
        }
        if ("createLink".equals(name)) {
            rich.applyLink(arg);
            return;
        }
        if ("unlink".equals(name)) {
            rich.removeLinkStyle();
            return;
        }
        if ("foreColor".equals(name)) {
            rich.setForeColor(parseCss(arg));
            return;
        }
        if ("hiliteColor".equals(name)) {
            rich.setHighlight(parseCss(arg));
            return;
        }
        if ("formatBlock".equals(name)) {
            rich.setBlockFormat(arg);
            return;
        }
        if ("fontSize".equals(name)) {
            rich.setFontSizeLevel(parseInt(arg));
            return;
        }
        if ("removeFormat".equals(name)) {
            rich.removeFormat();
            return;
        }
        super.cmd(name, arg);
    }

    @Override
    public String query(String name, String arg) {
        if ("getHtml".equals(name)) {
            return HtmlSerializer.serialize(rich.getDocument(), rich.getInlineStyles(), rich.getBlocks(),
                    rich.getLinkRuns(), rich.getImageSources());
        }
        if ("getText".equals(name)) {
            return rich.getText();
        }
        if ("state".equals(name)) {
            return rich.queryState(arg) ? "1" : "0";
        }
        return super.query(name, arg);
    }

    private static int parseCss(String v) {
        if (v == null) {
            return 0;
        }
        v = v.trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        if (v.length() == 3) {
            char r = v.charAt(0);
            char g = v.charAt(1);
            char b = v.charAt(2);
            v = "" + r + r + g + g + b + b;
        }
        try {
            return Integer.parseInt(v.length() >= 6 ? v.substring(0, 6) : v, 16);
        } catch (NumberFormatException err) {
            return 0;
        }
    }

    static com.codename1.ui.Image loadImage(String url) {
        if (url == null || url.length() == 0) {
            return null;
        }
        try {
            if (url.startsWith("gen:")) {
                // gen:WxH:RRGGBB - a generated solid color image (handy for demos / placeholders)
                String s = url.substring(4);
                int xi = s.indexOf('x');
                int ci = s.indexOf(':', xi);
                int w = Integer.parseInt(s.substring(0, xi));
                int h = Integer.parseInt(s.substring(xi + 1, ci));
                int color = Integer.parseInt(s.substring(ci + 1), 16);
                return com.codename1.ui.Image.createImage(w, h, 0xff000000 | color);
            }
            int comma = url.indexOf(',');
            if (url.startsWith("data:") && url.indexOf(";base64,") >= 0 && comma >= 0) {
                // the base64 payload is pure ASCII; convert to bytes without relying on the default charset
                String b64 = url.substring(comma + 1);
                byte[] enc = new byte[b64.length()];
                for (int i = 0; i < enc.length; i++) {
                    enc[i] = (byte) b64.charAt(i);
                }
                byte[] data = com.codename1.util.Base64.decode(enc);
                return com.codename1.ui.Image.createImage(data, 0, data.length);
            }
            return com.codename1.ui.Image.createImage(url);
        } catch (Throwable t) {
            return null;
        }
    }

    static List<com.codename1.ui.Image> loadImages(List<String> sources) {
        List<com.codename1.ui.Image> out = new ArrayList<com.codename1.ui.Image>(sources.size());
        for (String source : sources) {
            out.add(source == null || source.length() == 0 ? null : loadImage(source));
        }
        return out;
    }

    private static int parseInt(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException err) {
            return 0;
        }
    }
}
