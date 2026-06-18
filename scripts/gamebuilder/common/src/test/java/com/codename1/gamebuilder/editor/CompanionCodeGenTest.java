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

import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests that the generated {@code .game} data round-trips and the companion Java
/// contains the expected wiring.
class CompanionCodeGenTest {

    @Test
    void gameDataRoundTrips() throws Exception {
        EditorController c = new EditorController(
                new EditorModel(StarterPacks.newLevel(GameLevel.MODE_2D), StarterPacks.loadCatalog()));
        c.model().setActiveLayer("Actors");
        c.model().setSelectedAssetId("player");
        c.placeElement(64, 64);

        String json = CompanionCodeGen.gameData(c.model());
        GameLevel reloaded = GameLevel.load(json);
        assertEquals(GameLevel.MODE_2D, reloaded.getMode());
        GameElement el = reloaded.elements().get(0);
        assertEquals("player", el.getAssetId());
        assertEquals(3, el.getInt("lives", -1));
    }

    @Test
    void companionJavaHasWiring() {
        String src = CompanionCodeGen.companionJava("com.example.game", "Level1", "/Level1.game");
        assertTrue(src.startsWith("package com.example.game;"), "has package");
        assertTrue(src.contains("class Level1 extends GameSceneView"), "extends GameSceneView");
        assertTrue(src.contains("/Level1.game"), "references flat resource path");
        assertFalse(src.contains("/games/Level1.game"), "no nested resource path");
        assertTrue(src.contains(CompanionCodeGen.GEN_BEGIN), "has gen-begin marker");
        assertTrue(src.contains(CompanionCodeGen.GEN_END), "has gen-end marker");
        assertTrue(src.contains("protected void onUpdate("), "has behavior hook");
    }

    @Test
    void companionJavaGeneratesFieldsForNamedElements() {
        GameLevel level = StarterPacks.newLevel(GameLevel.MODE_2D);
        level.addElement(new GameElement("e1", "player").setName("player").setProperty("lives", 3));
        level.addElement(new GameElement("e2", "slime").setName("slime"));
        level.addElement(new GameElement("e3", "coin")); // unnamed -> no field

        String src = CompanionCodeGen.companionJava("com.example.game", "Level1", "/Level1.game", level);
        assertTrue(src.contains("import com.codename1.gaming.Sprite;"), "imports Sprite for fields");
        assertTrue(src.contains("protected Sprite player;"), "field for named player");
        assertTrue(src.contains("protected Sprite slime;"), "field for named slime");
        assertTrue(src.contains("player = findByName(\"player\");"), "wires player field");
        assertTrue(src.contains("setLives(elementOf(player).getInt(\"lives\", 3));"), "seeds lives");
        assertTrue(src.contains("setArcadeBehavior(true);"), "2D scene enables arcade behavior");
        // unnamed element gets no field
        assertFalse(src.contains("Sprite coin;"), "no field for unnamed element");
    }

    @Test
    void companionJavaNoPackage() {
        String src = CompanionCodeGen.companionJava("", "Boss", "/Boss.game");
        assertFalse(src.contains("package "), "default package omits the statement");
        assertTrue(src.contains("class Boss extends GameSceneView"));
    }
}
