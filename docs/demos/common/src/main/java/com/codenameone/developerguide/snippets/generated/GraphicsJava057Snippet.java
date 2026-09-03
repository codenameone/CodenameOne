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
import java.util.*;


class GraphicsJava057Snippet {


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
    
    void snippet() throws Exception {
        // tag::graphics-java-057[]
        for ( int i=1; i<=12; i++){
            // Calculate the string width and height so we can center it properly
            String numStr = ""+i;
            int charWidth = g.getFont().stringWidth(numStr);
            int charHeight = g.getFont().getHeight();

            double di = (double)i;  // number as double for easier math

            // Calculate the position along the edge of the clock where the number should
            // be drawn
             // Get the angle from 12 O'Clock to this tick (radians)
            double angleFrom12 = di/12.0*2.0*Math.PI;

            // Get the angle from 3 O'Clock to this tick
                // Note: 3 O'Clock corresponds with zero angle in unit circle
                // Makes it easier to do the math.
            double angleFrom3 = Math.PI/2.0-angleFrom12;

            // Get diff between number position and clock center
            int tx = (int)(Math.cos(angleFrom3)*(r-longTickLen));
            int ty = (int)(-Math.sin(angleFrom3)*(r-longTickLen));

            // For 6 and 12 we will shift number slightly so they're more even
            if ( i == 6 ){
                ty -= charHeight/2;
            } else if ( i == 12 ){
                ty += charHeight/2;
            }

            // Translate the graphics context by delta between clock center and
            // number position
            g.translate(
                    tx,
                    ty
            );


            // Draw number at clock center.
            g.drawString(numStr, (int)cX-charWidth/2, (int)cY-charHeight/2);

            // Undo translation
            g.translate(-tx, -ty);

        }
        // end::graphics-java-057[]
    }

    double cX;
    double cY;
    int longTickLen = 50;
    double r;

}
