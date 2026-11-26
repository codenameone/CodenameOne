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
package com.codename1.art;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The editor's tools, driven through the view's pointer handler in document coordinates.
class ArtEditorTest extends UITestBase {
    private static ArtEditor show() {
        ArtEditor e = new ArtEditor(new ArtModel(new ArtDocument(200, 100)));
        Form f = new Form("Art", new BorderLayout());
        f.add(BorderLayout.CENTER, e);
        f.show();
        return e;
    }

    private static void drag(ArtEditor e, double x0, double y0, double x1, double y1) {
        ArtView.PointerHandler h = e.getView().getPointerHandler();
        h.pressed(e.getView(), x0, y0);
        h.dragged(e.getView(), (x0 + x1) / 2, (y0 + y1) / 2);
        h.dragged(e.getView(), x1, y1);
        h.released(e.getView(), x1, y1);
    }

    @Test
    void rectangleToolDrawsAndUndoRemovesIt() {
        ArtEditor e = show();
        e.setFillColor(0xffff0000);
        e.setTool(ArtEditor.TOOL_RECTANGLE);
        drag(e, 10, 10, 50, 40);
        ArtModel m = e.getModel();
        assertEquals(1, m.getDocument().getRoot().getChildren().size());
        Raster r = ArtRenderer.render(m.getDocument(), 1);
        assertEquals(0xffff0000, r.get(30, 25));
        m.undo();
        assertEquals(0, m.getDocument().getRoot().getChildren().size());
    }

    @Test
    void selectToolMovesTheShapeAsOneUndoStep() {
        ArtEditor e = show();
        e.setTool(ArtEditor.TOOL_RECTANGLE);
        drag(e, 10, 10, 50, 40);
        e.setTool(ArtEditor.TOOL_SELECT);
        ArtNode shape = e.getModel().getDocument().getRoot().getChildren().get(0);
        assertSame(shape, e.getView().nodeAt(30, 25));
        drag(e, 30, 25, 60, 35);
        assertEquals(30, shape.getTransform().getE(), 0.001);
        e.getModel().undo();
        assertEquals(0, e.getModel().findById(shape.getId()).getTransform().getE(), 0.001);
        assertNull(e.getView().nodeAt(150, 90));
    }

    @Test
    void brushPaintsANewPixelLayerAndFillRecolorsShapes() throws IOException {
        ArtEditor e = show();
        e.setFillColor(0xff00ff00);
        e.setBrushSize(10);
        e.setTool(ArtEditor.TOOL_BRUSH);
        drag(e, 20, 50, 80, 50);
        ArtNode layer = e.getModel().getDocument().getRoot().getChildren().get(0);
        assertTrue(layer instanceof RasterNode);
        assertEquals(0xff00ff00, ArtRenderer.render(e.getModel().getDocument(), 1).get(50, 50));
        e.setTool(ArtEditor.TOOL_RECTANGLE);
        drag(e, 100, 10, 150, 40);
        e.setFillColor(0xff0000ff);
        e.setTool(ArtEditor.TOOL_FILL);
        e.getView().getPointerHandler().pressed(e.getView(), 120, 20);
        assertEquals(0xff0000ff, ArtRenderer.render(e.getModel().getDocument(), 1).get(120, 20));
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        e.exportPng(png, 1);
        assertNotNull(ArtImages.decode(png.toByteArray()));
    }
}
