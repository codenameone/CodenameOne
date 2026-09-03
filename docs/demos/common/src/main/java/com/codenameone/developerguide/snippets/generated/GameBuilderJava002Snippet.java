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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.gaming.*;
import com.codename1.gaming.level.*;
import com.codename1.gaming.physics.*;
import java.util.*;


class GameBuilderJava002Snippet extends GameSceneView {

    GameBuilderJava002Snippet() { super(new GameLevel(), null); }


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    // tag::game-builder-java-002[]
    @Override
    protected void onUpdate(double dt) {
        // The generated companion switches the built-in arcade behaviour on, and
        // that already patrols enemies and collects pickups. A scene doing the
        // work itself calls setArcadeBehavior(false) in its constructor -- with
        // both running, the built-in patrol and the step below cancel out and
        // the slime stalls.
        GameInput in = getInput();
        Scene scene = getScene();
        // The editor names a stamped object after the asset's display name, so
        // the generated companion has a field like duke1 rather than "player".
        // Look the sprite up by asset id instead of assuming a field name.
        Sprite player = findByAsset("player");
        // Backwards, because collecting a coin removes it from the scene and a
        // forward loop would step over the next sprite.
        for (int i = scene.size() - 1; i >= 0; i--) {
            Sprite s = scene.get(i);
            // Tile sprites carry a Layer in their user data, not a GameElement, so
            // a plain cast fails on the first frame of any scene with painted
            // tiles. elementOf() answers null for those. It also matters that this
            // is not a cast: ParparVM does not check them, so on iOS a bad cast
            // reads the wrong object's fields instead of throwing.
            GameElement el = elementOf(s);
            if (el == null) continue;
            switch (el.getAssetId()) {
                case "player" -> {
                    if (in.isGameKeyDown(Display.GAME_RIGHT)) s.setX(s.getX() + 200 * dt);
                    // jump height, lives, gravity... all read from el.getInt(...)/getDouble(...)
                }
                // speed is pixels per second, so scale it by the frame delta --
                // otherwise the patrol runs at whatever rate the device renders at
                case "slime" -> s.setX(s.getX() + el.getDouble("speed", 60) * dt);
                case "coin"  -> {
                    if (s.intersects(player)) {
                        addScore(el.getInt("value", 10));
                        scene.remove(s);   // consume it, or it scores again every frame
                    }
                }
            }
        }
    }
    // end::game-builder-java-002[]


}
