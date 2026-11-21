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

import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Live components in documents: the factory registry, HTML and rich text round trips, and the
/// peers a `DocumentEditor` hosts.
class EmbeddedComponentsTest extends UITestBase {
    @BeforeEach
    void registerSlider() {
        EmbeddedComponents.register("slider", new EmbeddedComponents.Factory() {
            @Override
            public Component create(EmbeddedComponent e) {
                Slider s = new Slider();
                s.setEditable(true);
                s.setProgress(e.state.length() == 0 ? 0 : Integer.parseInt(e.state));
                return s;
            }

            @Override
            public String state(Component c) {
                return c instanceof Slider ? String.valueOf(((Slider) c).getProgress()) : "";
            }
        });
    }

    @AfterEach
    void unregister() {
        EmbeddedComponents.register("slider", null);
    }

    static Document withSlider(String state) {
        EmbeddedComponent c = new EmbeddedComponent("slider", state, 120, 24, null);
        RichText t = new RichText.Builder().append("Volume ", TextStyle.DEFAULT)
                .append(c, TextStyle.DEFAULT).build();
        return new Document(new ArrayList<Block>(Arrays.asList((Block) new Paragraph("p", t))),
                null, null, 10);
    }

    static EmbeddedComponent first(Document d) {
        Paragraph p = (Paragraph) d.get(0);
        for (int i = 0; i < p.text.length(); i++) {
            if (p.text.objectAt(i) instanceof EmbeddedComponent) {
                return (EmbeddedComponent) p.text.objectAt(i);
            }
        }
        return null;
    }

    @Test
    void sourceRoundTripsTypeSizeAndState() {
        EmbeddedComponent c = new EmbeddedComponent("slider", "a;b é", 120.5f, 24, null);
        String src = EmbeddedComponents.toSource(c);
        assertTrue(src.startsWith("cn1-component:slider;120.5x24;"), src);
        EmbeddedComponent back = EmbeddedComponents.fromSource(src);
        assertEquals(c, back);
        assertNull(EmbeddedComponents.fromSource("http://x"));
        assertNull(EmbeddedComponents.fromSource("cn1-component:broken"));
    }

    @Test
    void rendersThroughTheFactoryAndTheRichTextHook() {
        EmbeddedComponent c = new EmbeddedComponent("slider", "40", 120, 24, null);
        Image img = EmbeddedComponents.render(c, 60, 12);
        assertEquals(60, img.getWidth());
        Image viaHook = RichTextArea.loadRegisteredImage(EmbeddedComponents.toSource(c));
        assertNotNull(viaHook);
        Image frame = EmbeddedComponents.render(
                new EmbeddedComponent("unknown", "", 10, 10, null), 10, 10);
        assertEquals(10, frame.getHeight());
        assertNull(RichTextArea.loadRegisteredImage("nothing:here"));
    }

    @Test
    void htmlKeepsComponents() throws Exception {
        Document d = withSlider("70");
        String html = d.toString(Document.Format.HTML);
        assertTrue(html.contains("data-cn1-component=\"slider\""), html);
        Document back = Document.read(new java.io.ByteArrayInputStream(html.getBytes("UTF-8")),
                Document.Format.HTML);
        assertEquals(first(d), first(back));
        String rich = RichTextDocuments.toHtml(d);
        assertTrue(rich.contains("src=\"cn1-component:slider;"), rich);
        // what a rich text editor gives back: the source, without the data attributes
        String stripped = rich.replaceAll(" data-cn1-[a-z]*=\"[^\"]*\"", "");
        assertEquals(first(d), first(RichTextDocuments.fromHtml(stripped)));
    }

    @Test
    void editorHostsLiveComponentsAndCommitsState() {
        DocumentModel m = new DocumentModel(withSlider("30"));
        DocumentEditor editor = new DocumentEditor(m);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        flushSerialCalls();
        editor.getView().setFitWidth(true);
        f.revalidate();
        flushSerialCalls();
        assertEquals(1, editor.getLiveComponentCount());
        Component live = editor.getLiveComponent("p", 7);
        assertTrue(live instanceof Slider);
        assertEquals(30, ((Slider) live).getProgress());
        assertTrue(live.getWidth() > 0);
        ((Slider) live).setProgress(85);
        editor.commitComponents();
        assertEquals("85", first(m.getDocument()).state);
        m.undo();
        assertEquals("30", first(m.getDocument()).state);
        flushSerialCalls();
        // the edit replaced the component, so the peer was made again from the old state
        Component again = editor.getLiveComponent("p", 7);
        assertEquals(30, ((Slider) again).getProgress());
    }
}
