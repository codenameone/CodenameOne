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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The undoable editing model.
class ArtModelTest {
    @Test
    void undoAndRedoRestoreTheTreeAndKeepIds() {
        ArtModel m = new ArtModel(new ArtDocument(100, 100));
        ShapeNode s = ShapeNode.rect(0, 0, 10, 10, 0);
        m.add(s);
        String id = s.getId();
        assertNotNull(id);
        m.setOpacity(s, 0.25f);
        assertTrue(m.undo());
        ArtNode restored = m.findById(id);
        assertNotNull(restored);
        assertEquals(1f, restored.getOpacity(), 0.001);
        assertTrue(m.redo());
        assertEquals(0.25f, m.findById(id).getOpacity(), 0.001);
        assertTrue(m.undo());
        assertTrue(m.undo());
        assertNull(m.findById(id));
        assertFalse(m.canUndo());
    }

    @Test
    void groupedChangesAreOneUndoStep() {
        ArtModel m = new ArtModel(new ArtDocument(100, 100));
        ShapeNode s = ShapeNode.rect(0, 0, 10, 10, 0);
        m.add(s);
        m.beginGroup();
        m.select(s, false);
        m.translateSelection(5, 0);
        m.translateSelection(5, 0);
        m.endGroup();
        assertEquals(10, m.findById(s.getId()).getTransform().getE(), 0.001);
        m.undo();
        assertEquals(0, m.findById(s.getId()).getTransform().getE(), 0.001);
    }

    @Test
    void groupAndUngroupKeepOrderAndSelection() {
        ArtModel m = new ArtModel(new ArtDocument(100, 100));
        ShapeNode a = ShapeNode.rect(0, 0, 10, 10, 0);
        ShapeNode b = ShapeNode.rect(20, 0, 10, 10, 0);
        ShapeNode c = ShapeNode.rect(40, 0, 10, 10, 0);
        m.add(a);
        m.add(b);
        m.add(c);
        List<ArtNode> two = new ArrayList<ArtNode>();
        two.add(a);
        two.add(b);
        GroupNode g = m.group(two);
        List<ArtNode> root = m.getDocument().getRoot().getChildren();
        assertEquals(2, root.size());
        assertSame(g, root.get(0));
        assertSame(c, root.get(1));
        m.ungroup(g);
        root = m.getDocument().getRoot().getChildren();
        assertEquals(3, root.size());
        assertSame(a, root.get(0));
        assertSame(b, root.get(1));
    }

    @Test
    void snapshotsSharePixelsButEditsDoNotLeakIntoUndo() {
        ArtModel m = new ArtModel(new ArtDocument(4, 4));
        RasterNode r = new RasterNode(new Raster(0, 0, 1, 1, new int[]{0xff000000}));
        m.add(r);
        m.checkpoint();
        r.setImage(r.getImage().copy());
        r.getImage().getPixels()[0] = 0xffffffff;
        m.changed();
        m.undo();
        RasterNode back = (RasterNode) m.findById(r.getId());
        assertEquals(0xff000000, back.getImage().getPixels()[0]);
    }
}
