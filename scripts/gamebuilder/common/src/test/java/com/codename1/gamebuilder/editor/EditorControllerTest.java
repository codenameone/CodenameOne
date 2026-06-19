/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gamebuilder.editor;

import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Headless tests for the editor edit operations and undo/redo. No Codename One UI or
/// Display is touched, so these run on a plain JVM.
class EditorControllerTest {

    private EditorController controller(GameLevel.Mode mode) {
        AssetCatalog cat = StarterPacks.loadCatalog();
        EditorModel model = new EditorModel(StarterPacks.newLevel(mode), cat);
        return new EditorController(model);
    }

    @Test
    void starterLayersPerMode() {
        assertEquals(4, controller(GameLevel.Mode.TWO_D).model().level().layers().size());
        assertEquals(2, controller(GameLevel.Mode.BOARD).model().level().layers().size());
        EditorController three = controller(GameLevel.Mode.THREE_D);
        assertEquals(1, three.model().level().layers().size());
        assertNotNull(three.model().level().getTerrain());
        assertEquals(1, three.model().level().lights().size());
    }

    @Test
    void paintEraseAndUndoRedo() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.model().setActiveLayer("Terrain");
        c.model().setSelectedAssetId("brick");

        assertTrue(c.paintTile(3, 4));
        assertEquals("brick", c.model().level().getLayer("Terrain").getTile(3, 4));
        assertTrue(c.model().isDirty());
        assertTrue(c.canUndo());

        // painting the same asset in the same cell is a no-op (no new history)
        assertFalse(c.paintTile(3, 4));

        c.undo();
        assertNull(c.model().level().getLayer("Terrain").getTile(3, 4));
        assertTrue(c.canRedo());
        c.redo();
        assertEquals("brick", c.model().level().getLayer("Terrain").getTile(3, 4));

        assertTrue(c.eraseTile(3, 4));
        assertNull(c.model().level().getLayer("Terrain").getTile(3, 4));
    }

    @Test
    void floodFillBounded() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.model().setActiveLayer("Background");
        c.model().setSelectedAssetId("ground");
        int cols = c.model().level().getCols();
        int rows = c.model().level().getRows();

        int filled = c.floodFill(0, 0);              // empty layer -> fills whole grid
        assertEquals(cols * rows, filled);
        assertEquals("ground", c.model().level().getLayer("Background").getTile(5, 5));

        c.undo();
        assertTrue(c.model().level().getLayer("Background").tiles().isEmpty());
    }

    @Test
    void placeElementCopiesDefaultsAndUnique() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.model().setActiveLayer("Actors");

        c.model().setSelectedAssetId("coin");
        GameElement coin = c.placeElement(100, 120);
        assertNotNull(coin);
        assertEquals("Actors", coin.getLayer());
        assertEquals(10, coin.getInt("value", -1));   // default prop copied
        assertEquals("e1", coin.getId());
        assertEquals(1, c.model().level().elements().size());

        // unique: placing player twice relocates the same element
        c.model().setSelectedAssetId("player");
        GameElement p1 = c.placeElement(10, 10);
        GameElement p2 = c.placeElement(200, 50);
        assertSame(p1, p2);
        assertEquals(200, p2.getX(), 0.001);
        assertEquals(2, c.model().level().elements().size());  // coin + single player
    }

    @Test
    void selectMoveDelete() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.model().setActiveLayer("Actors");
        c.model().setSelectedAssetId("gem");
        GameElement gem = c.placeElement(100, 100);

        // hit test centered on the element (gem is 24x24)
        assertSame(gem, c.selectAt(105, 95));
        assertNull(c.selectAt(400, 400));

        c.model().setSelection(gem);
        assertTrue(c.moveSelectionBy(20, -10));
        assertEquals(120, gem.getX(), 0.001);
        assertEquals(90, gem.getY(), 0.001);

        assertTrue(c.deleteSelection());
        assertTrue(c.model().level().elements().isEmpty());
        c.undo();                                       // delete is undoable
        assertEquals(1, c.model().level().elements().size());
    }

    @Test
    void resizeGridIsUndoable() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        assertEquals(26, c.model().level().getCols());
        assertTrue(c.resizeGrid(30, 20, 48));
        assertEquals(30, c.model().level().getCols());
        assertEquals(20, c.model().level().getRows());
        assertEquals(48, c.model().level().getTileSize());
        assertFalse(c.resizeGrid(30, 20, 48), "no-op resize makes no history");
        assertFalse(c.resizeGrid(-1, 20, 48), "invalid resize rejected");
        c.undo();
        assertEquals(26, c.model().level().getCols());
    }

    @Test
    void setLevelPropertyAndRename() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.setLevelProperty("background", "Night");
        assertEquals("Night", c.model().level().getString("background", "Sky"));
        c.undo();
        assertEquals("Sky", c.model().level().getString("background", "Sky"));
        c.renameScene("Boss");
        assertEquals("Boss", c.model().getSceneName());
    }

    @Test
    void addEntityLayerBecomesActive() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        int before = c.model().level().layers().size();
        c.addEntityLayer("Foreground");
        assertEquals(before + 1, c.model().level().layers().size());
        assertEquals("Foreground", c.model().getActiveLayer());
        assertNotNull(c.model().level().getLayer("Foreground"));
        c.undo();
        assertEquals(before, c.model().level().layers().size());
    }

    @Test
    void loadLevelReplacesAndClearsHistory() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.model().setActiveLayer("Terrain");
        c.model().setSelectedAssetId("brick");
        c.paintTile(1, 1);
        assertTrue(c.canUndo());
        c.loadLevel(StarterPacks.newLevel(GameLevel.Mode.BOARD), "Board1");
        assertFalse(c.canUndo());
        assertEquals(GameLevel.Mode.BOARD, c.model().level().getMode());
        assertEquals("Board1", c.model().getSceneName());
    }

    @Test
    void snapDefaultsOn() {
        assertTrue(controller(GameLevel.Mode.TWO_D).model().isSnap());
    }

    @Test
    void newLevelClearsHistory() {
        EditorController c = controller(GameLevel.Mode.TWO_D);
        c.model().setActiveLayer("Terrain");
        c.model().setSelectedAssetId("brick");
        c.paintTile(1, 1);
        assertTrue(c.canUndo());
        c.newLevel(GameLevel.Mode.BOARD);
        assertFalse(c.canUndo());
        assertEquals(GameLevel.Mode.BOARD, c.model().level().getMode());
        assertFalse(c.model().isDirty());
    }
}
