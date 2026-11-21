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
package com.codename1.wordprocessing;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/// The registry that turns an `EmbeddedComponent` -- a type name and a state string in a
/// document -- into a live Codename One component, and back.
///
/// ```java
/// EmbeddedComponents.register("rating", new EmbeddedComponents.Factory() {
///     public Component create(EmbeddedComponent e) {
///         Slider s = new Slider();
///         s.setEditable(true);
///         s.setProgress(e.state.length() == 0 ? 0 : Integer.parseInt(e.state));
///         return s;
///     }
///     public String state(Component c) {
///         return String.valueOf(((Slider) c).getProgress());
///     }
/// });
/// ```
///
/// A `DocumentEditor` hosts live components over the pages they are on, and a `RichTextArea`
/// shows them in its text through the `cn1-component:` image source this class registers.
/// Formats that cannot carry a component -- DOCX, ODT, RTF, PDF -- get its snapshot, which
/// `snapshot` renders.
public final class EmbeddedComponents {
    private static final int MAX_PIXELS = 4096;
    /// The image source scheme that names a component in a `RichTextArea`.
    public static final String SCHEME = "cn1-component";

    private static final Map<String, Factory> FACTORIES = new HashMap<String, Factory>();

    private EmbeddedComponents() {
    }

    /// Creates the components of one type and reads their state back.
    public interface Factory {
        /// A component showing an embedded component's state.
        ///
        /// #### Parameters
        ///
        /// - `embedded`: the embedded component, with its state and size in points
        ///
        /// #### Returns
        ///
        /// the component, or null to leave its box empty
        Component create(EmbeddedComponent embedded);

        /// The state a component holds now, to store in the document.
        ///
        /// #### Parameters
        ///
        /// - `component`: a component this factory created
        ///
        /// #### Returns
        ///
        /// the state string
        String state(Component component);
    }

    /// Registers the factory for a type, replacing any earlier one.
    ///
    /// #### Parameters
    ///
    /// - `type`: the type name, without `;`
    ///
    /// - `factory`: the factory, or null to remove the type
    public static void register(String type, Factory factory) {
        if (type == null || type.length() == 0 || type.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Invalid component type " + type);
        }
        boolean none;
        synchronized (FACTORIES) {
            if (factory == null) {
                FACTORIES.remove(type);
            } else {
                FACTORIES.put(type, factory);
            }
            none = FACTORIES.isEmpty();
        }
        if (none) {
            // the last type is gone: rich text areas no longer resolve component images
            RichTextArea.setImageSource(SCHEME, null);
            return;
        }
        RichTextArea.setImageSource(SCHEME, new RichTextArea.ImageSource() {
            @Override
            public Image load(String source) {
                EmbeddedComponent e = fromSource(source);
                return e == null ? null : render(e, pixels(e.width), pixels(e.height));
            }
        });
    }

    /// The factory registered for a type.
    ///
    /// #### Parameters
    ///
    /// - `type`: the type name
    ///
    /// #### Returns
    ///
    /// the factory, or null
    public static Factory get(String type) {
        synchronized (FACTORIES) {
            return FACTORIES.get(type);
        }
    }

    /// A live component for an embedded component.
    ///
    /// #### Parameters
    ///
    /// - `e`: the embedded component
    ///
    /// #### Returns
    ///
    /// the component, or null when no factory makes one
    public static Component create(EmbeddedComponent e) {
        Factory f = get(e.type);
        return f == null ? null : f.create(e);
    }

    static int pixels(float points) {
        return Math.max(1, Math.round(points * Display.getInstance().convertToPixels(254f) / 720f));
    }

    /// A picture of an embedded component: its live rendering when a factory is registered,
    /// otherwise its stored snapshot, otherwise an empty frame.
    ///
    /// #### Parameters
    ///
    /// - `e`: the embedded component
    ///
    /// - `w`: the width in pixels
    ///
    /// - `h`: the height in pixels
    ///
    /// #### Returns
    ///
    /// the image
    public static Image render(EmbeddedComponent e, int w, int h) {
        w = Math.max(1, Math.min(w, MAX_PIXELS));
        h = Math.max(1, Math.min(h, MAX_PIXELS));
        Component c = create(e);
        if (c != null) {
            return paint(c, w, h);
        }
        byte[] snap = e.snapshot();
        if (snap != null) {
            try {
                return EncodedImage.create(snap).scaled(w, h);
            } catch (RuntimeException err) {
                // a damaged snapshot shows as the frame below
            }
        }
        Image img = Image.createImage(w, h, 0xfff3f5f7);
        Graphics g = img.getGraphics();
        g.setColor(0xc5cad1);
        g.drawRect(0, 0, w - 1, h - 1);
        return img;
    }

    /// Paints a component into a new image of its size.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component, not on a form
    ///
    /// - `w`: the width in pixels
    ///
    /// - `h`: the height in pixels
    ///
    /// #### Returns
    ///
    /// the image
    public static Image paint(Component c, int w, int h) {
        w = Math.max(1, Math.min(w, MAX_PIXELS));
        h = Math.max(1, Math.min(h, MAX_PIXELS));
        Image img = Image.createImage(w, h, 0x00ffffff);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        if (c instanceof Container) {
            ((Container) c).layoutContainer();
        }
        Graphics g = img.getGraphics();
        c.paintComponent(g, true);
        return img;
    }

    /// The embedded component with a new snapshot of a live component, for formats that can
    /// only carry a picture.
    ///
    /// #### Parameters
    ///
    /// - `e`: the embedded component
    ///
    /// - `live`: a component showing it, or null to create one from its factory
    ///
    /// #### Returns
    ///
    /// the embedded component with a PNG snapshot, or `e` when none can be made
    public static EmbeddedComponent snapshot(EmbeddedComponent e, Component live) {
        Component c = live != null ? live : create(e);
        ImageIO io = ImageIO.getImageIO();
        if (c == null || io == null) {
            return e;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            io.save(paint(c, pixels(e.width), pixels(e.height)), out, ImageIO.FORMAT_PNG, 1);
            byte[] png = out.toByteArray();
            return png.length == 0 ? e : e.withSnapshot(png);
        } catch (IOException err) {
            return e;
        }
    }

    /// Gives every embedded component in a document a current snapshot, before writing it in a
    /// format that can only carry pictures.
    ///
    /// #### Parameters
    ///
    /// - `d`: the document
    ///
    /// #### Returns
    ///
    /// the document with snapshots
    public static Document snapshots(Document d) {
        DocumentModel m = new DocumentModel(d);
        m.setAutoRecalculate(false);
        java.util.List<String> ids = new java.util.ArrayList<String>();
        DocumentModel.collectIds(d.blocks, ids);
        for (String id : ids) {
            Paragraph p = m.findParagraph(id);
            RichText t = p.text;
            for (int i = 0; i < t.length(); i++) {
                Inline o = t.objectAt(i);
                if (o instanceof EmbeddedComponent && get(((EmbeddedComponent) o).type) != null) {
                    t = t.withObject(i, snapshot((EmbeddedComponent) o, null));
                }
            }
            if (t != p.text) { //NOPMD CompareObjectsWithEquals - withObject was applied
                m.replaceParagraph(p.withText(t));
            }
        }
        return m.getDocument();
    }

    // ------------------------------------------------------------------ sources

    /// The `cn1-component:` image source naming an embedded component, for a `RichTextArea`.
    ///
    /// #### Parameters
    ///
    /// - `e`: the embedded component
    ///
    /// #### Returns
    ///
    /// the source: type, size in points and state
    public static String toSource(EmbeddedComponent e) {
        String state;
        try {
            state = Base64.encodeNoNewline(e.state.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException err) {
            state = "";
        }
        return SCHEME + ":" + e.type + ";" + Ids.fmt(e.width) + "x" + Ids.fmt(e.height) + ";"
                + state;
    }

    /// The embedded component a `cn1-component:` source names.
    ///
    /// #### Parameters
    ///
    /// - `source`: the source
    ///
    /// #### Returns
    ///
    /// the component, or null when the source is not one
    public static EmbeddedComponent fromSource(String source) {
        if (source == null || !source.startsWith(SCHEME + ":")) {
            return null;
        }
        String s = source.substring(SCHEME.length() + 1);
        int a = s.indexOf(';');
        int b = a < 0 ? -1 : s.indexOf(';', a + 1);
        if (a <= 0 || b < 0) {
            return null;
        }
        String size = s.substring(a + 1, b);
        int x = size.indexOf('x');
        try {
            float w = Float.parseFloat(size.substring(0, x));
            float h = Float.parseFloat(size.substring(x + 1));
            byte[] st = Base64.decode(Ids.ascii(s.substring(b + 1)));
            String state = st == null ? "" : new String(st, "UTF-8");
            return new EmbeddedComponent(s.substring(0, a), state, w, h, null);
        } catch (RuntimeException err) {
            return null;
        } catch (UnsupportedEncodingException err) {
            return null;
        }
    }
}
