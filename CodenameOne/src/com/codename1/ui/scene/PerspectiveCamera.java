/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui.scene;

import com.codename1.properties.DoubleProperty;
import com.codename1.properties.Property;
import com.codename1.ui.Display;
import com.codename1.ui.Transform;
import java.util.Arrays;

/**
 *
 * @author shannah
 * @deprecated Internal use only
 */
public class PerspectiveCamera extends Camera {
    public final Property<Double,Camera> verticalFieldOfView;
    private Scene scene;



    public PerspectiveCamera(Scene scene, double fovY, double zNear, double zFar) {
        super(zNear, zFar);
        this.scene = scene;
        verticalFieldOfView = new Property<Double,Camera>("verticalFieldOfView", 0.25);
    }

    @Override
    public Transform getTransform() {
        Display d = Display.getInstance();
        float zNear = (float)nearClip.get().doubleValue();
        double dw = d.getDisplayWidth();
        double dh = d.getDisplayHeight();
        int x = scene.getAbsoluteX();
        int y = scene.getAbsoluteY();
        int w = scene.getWidth();
        int h = scene.getHeight();
        Transform perspectiveT = Transform.makePerspective((float)verticalFieldOfView.get().doubleValue(), (float)(dw/dh), zNear, (float)farClip.get().doubleValue());
        float displayH = Display.getInstance().getDisplayHeight();
        float displayW = (float)dw;
        //double midX = (float)x+(float)w/2.0;
        //double midY = (float)y+(float)h/2.0;

        //if (perspectiveT == null) {
        //    perspectiveT = Transform.makeIdentity();
        //}
        //makePerspectiveTransform(perspectiveT);
        float[] bottomRight = perspectiveT.transformPoint(new float[]{displayW, displayH, zNear});
        Transform currTransform = Transform.makeIdentity();



        float xfactor = -displayW/bottomRight[0];
        float yfactor = -displayH/bottomRight[1];

        currTransform.translate((float)dw/2, y+h/2, zNear);
        currTransform.scale(xfactor,yfactor,1f);

        //currTransform.translate((float)dw/2/xfactor, (y+h/2)/yfactor, 0);

        currTransform.concatenate(perspectiveT);
        float zState = 0f;
        float cameraZ = -zNear-w/2*zState;
        float cameraX = (float)-dw/2;//-x-w/2;
        float cameraY = -y-h/2;
        currTransform.translate(cameraX, cameraY, cameraZ);

        //if ( transitionState == STATE_FLIP){
        //currTransform.translate((float)midX, y, 0);
        //}
        float[] tl = new float[3];
        float[] tr = new float[3];
        float[] bl = new float[3];
        float[] br = new float[3];
        currTransform.transformPoint(new float[]{x, y, 0}, tl);
        currTransform.transformPoint(new float[]{x+w, y, 0}, tr);
        currTransform.transformPoint(new float[]{x+w, y+h, 0}, br);
        currTransform.transformPoint(new float[]{x, y+h, 0}, bl);
        System.out.println("Camera transform "+x+", "+y+", "+0+"->"+Arrays.toString(tl));
        System.out.println("Camera transform "+(x+w)+", "+y+", "+0+"->"+Arrays.toString(tr));
        System.out.println("Camera transform "+(x+w)+", "+(y+h)+", "+0+"->"+Arrays.toString(br));
        System.out.println("Camera transform "+(x)+", "+(y+h)+", "+0+"->"+Arrays.toString(bl));

        return currTransform;
    }


}
