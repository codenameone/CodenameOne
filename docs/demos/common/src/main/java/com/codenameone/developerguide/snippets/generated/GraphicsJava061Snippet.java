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
import java.util.Calendar;


class GraphicsJava061Snippet {


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
        // tag::graphics-java-061[]
        // Draw the minute hand
        GeneralPath minuteHand = new GeneralPath();
        minuteHand.moveTo((float)cX, (float)cY);
        minuteHand.lineTo((float)cX+6, (float)cY);
        minuteHand.lineTo((float)cX+2, (float)(cY-(r-tickLen)));
        minuteHand.lineTo((float)cX-2, (float)(cY-(r-tickLen)));
        minuteHand.lineTo((float)cX-6, (float)cY);
        minuteHand.closePath();

        // Translate the minute hand slightly down so it overlaps the center
        Shape translatedMinuteHand = minuteHand.createTransformedShape(
            Transform.makeTranslation(0f, 5)
        );

        double minute = (double)(calendar.get(Calendar.MINUTE)) +
                (double)(calendar.get(Calendar.SECOND))/60.0;

        double minuteAngle = minute/60.0*2.0*Math.PI;

        // Rotate and draw the minute hand, keeping the caller's transform
        Transform beforeMinuteHand = g.getTransform();
        g.rotate((float)minuteAngle, (int)absCX, (int)absCY);
        g.setColor(0x000000);
        g.fillShape(translatedMinuteHand);
        g.setTransform(beforeMinuteHand);


        // Draw the hour hand
        GeneralPath hourHand = new GeneralPath();
        hourHand.moveTo((float)cX, (float)cY);
        hourHand.lineTo((float)cX+4, (float)cY);
        hourHand.lineTo((float)cX+1, (float)(cY-(r-longTickLen)*0.75));
        hourHand.lineTo((float)cX-1, (float)(cY-(r-longTickLen)*0.75));
        hourHand.lineTo((float)cX-4, (float)cY);
        hourHand.closePath();

        Shape translatedHourHand = hourHand.createTransformedShape(
            Transform.makeTranslation(0f, 5)
        );

        //Calendar cal = Calendar.getInstance().get
        double hour = (double)(calendar.get(Calendar.HOUR_OF_DAY)%12) +
                (double)(calendar.get(Calendar.MINUTE))/60.0;

        double angle = hour/12.0*2.0*Math.PI;
        Transform beforeHourHand = g.getTransform();
        g.rotate((float)angle, (int)absCX, (int)absCY);
        g.setColor(0x000000);
        g.fillShape(translatedHourHand);
        g.setTransform(beforeHourHand);
        // end::graphics-java-061[]
    }

    int longTickLen = 50;
    double r;
    double cX;
    int tickLen = 30;
    java.util.Calendar calendar = java.util.Calendar.getInstance();
    double cY;


    double absCY;
    double absCX;

}
