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

package com.codenameone.developerguide.screenshots;

import com.codename1.io.Log;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

import java.io.IOException;

/// An image drawn through a shaped clip, with the shape outlined over it.
class GraphicsShapedClippingFigure implements GuideFigure {

    @Override
    public String id() {
        return "shaped-clipping";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::graphics-java-023[]
        Image duke = null;
        try {
            // duke.png is just the default Codename One icon copied into place
            duke = Image.createImage("/duke.png");
        } catch(IOException err) {
            Log.e(err);
        }
        final Image finalDuke = duke;

        Form hi = new Form("Shape Clip");

        // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
        GeneralPath path = new GeneralPath();
        path.moveTo(20,0);
        path.lineTo(30, 0);
        path.lineTo(30, 100);
        path.lineTo(20, 100);
        path.lineTo(20, 15);
        path.lineTo(5, 40);
        path.lineTo(5, 25);
        path.lineTo(20,0);

        Stroke stroke = new Stroke(0.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
        hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
            g.setColor(0xff);
            float widthRatio = ((float)rect.getWidth()) / 50f;
            float heightRatio = ((float)rect.getHeight()) / 100f;
            g.scale(widthRatio, heightRatio);
            g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
            g.setClip(path);
            g.setAntiAliased(true);
            g.drawImage(finalDuke, 0, 0, 50, 100);
            g.setClip(path.getBounds());
            g.drawShape(path, stroke);
            g.translate(-(int)(((float)rect.getX()) / widthRatio), -(int)(((float)rect.getY()) / heightRatio));
            g.resetAffine();
        });

        hi.show();
        // end::graphics-java-023[]
        return hi;
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
