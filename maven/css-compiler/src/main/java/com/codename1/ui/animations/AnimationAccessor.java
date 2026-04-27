/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.ui.animations;

import com.codename1.ui.*;

/**
 * Allows us to access package protected members in animation
 *
 * @author Shai Almog
 */
public class AnimationAccessor {
    public static Image getImage(AnimationObject o) {
        return o.img;
    }

    public static void setImage(AnimationObject o, Image value) {
        o.img = value;
    }

    public static Image getImageMethod(AnimationObject o) {
        return o.getImage();
    }

    public static String getImageName(AnimationObject o) {
        return o.imageName;
    }

    public static Motion getMotionX(AnimationObject o) {
        return o.motionX;
    }

    public static Motion getMotionY(AnimationObject o) {
        return o.motionY;
    }

    public static Motion getOrientation(AnimationObject o) {
        return o.orientation;
    }

    public static Motion getWidth(AnimationObject o) {
        return o.width;
    }

    public static Motion getHeight(AnimationObject o) {
        return o.height;
    }

    public static Motion getOpacity(AnimationObject o) {
        return o.opacity;
    }

    public static int getMotionType(Motion m) {
        if(m.motionType == Motion.LINEAR) {
            return AnimationObject.MOTION_TYPE_LINEAR;
        }
        return AnimationObject.MOTION_TYPE_SPLINE;
    }

    public static void setAnimation(Timeline t, int offset, AnimationObject o) {
        t.animations[offset] = o;
    }

    public static int getFrameDelay(AnimationObject o) {
        return o.frameDelay;
    }

    public static int getFrameWidth(AnimationObject o) {
        return o.frameWidth;
    }

    public static int getFrameHeight(AnimationObject o) {
        return o.frameHeight;
    }

    public static int getX(AnimationObject o) {
        return o.getX();
    }

    public static int getY(AnimationObject o) {
        return o.getY();
    }

    public static int getWidthInt(AnimationObject o) {
        return o.getWidth();
    }

    public static int getHeightInt(AnimationObject o) {
        return o.getHeight();
    }

    public static AnimationObject clone(AnimationObject o) {
        AnimationObject r = AnimationObject.createAnimationImage(o.img, 0, 0);
        r.imageName = o.imageName;
        r.res = o.res;
        r.frames = o.frames;
        r.motionX = o.motionX;
        r.motionY = o.motionY;
        r.orientation = o.orientation;
        r.width = o.width;
        r.height = o.height;
        r.opacity = o.opacity;
        r.frameWidth = o.frameWidth;
        r.frameHeight = o.frameHeight;
        r.frameDelay = o.frameDelay;
        return r;
    }
}
