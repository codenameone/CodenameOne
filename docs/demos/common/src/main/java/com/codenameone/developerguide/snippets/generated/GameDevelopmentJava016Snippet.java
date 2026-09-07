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


class GameDevelopmentJava016Snippet {


    Object context;
    Object url;
    Object value;
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
    
    void snippet() throws Exception {
        // tag::game-development-java-016[]
        // a hinge the two bodies pivot around (anchor in pixels)
        world.createRevoluteJoint(bodyA, bodyB, pivotXpx, pivotYpx);
        // a fixed-length link, like a rod between two crates
        world.createDistanceJoint(bodyA, bodyB, ax, ay, bx, by, 0f, 0f);
        // drag a body toward the finger -- great for "pick up and throw"
        PhysicsJoint drag = world.createMouseJoint(ground, body, px, py, 1000f);
        // ... while the finger moves:
        drag.setTarget(getInput().getPointerX(), getInput().getPointerY());
        // ... on release:
        drag.destroy();
        // end::game-development-java-016[]
    }

    Scene getScene() { return null; }
    GameInput getInput() { return null; }
    TouchControls getControls() { return null; }
    GameCamera getCamera() { return null; }
    com.codename1.gpu.Light getLight() { return null; }
    void addModel(Model model) { }
    PhysicsBody bodyA;
    PhysicsBody bodyB;
    PhysicsBody ground;
    PhysicsBody body;
    float ax;
    float ay;
    float bx;
    float by;
    float px;
    float py;
    float pivotXpx;
    float pivotYpx;
    PhysicsWorld world = new PhysicsWorld(0, 900);

}
