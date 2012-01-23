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

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;

/**
 * An animation object is an element within the timeline that has a visibility state
 * for rendering at a given point in time. E.g. the object can be queried of its position
 * and render itself for any time.
 *
 * @author Shai Almog
 */
public final class AnimationObject {
    /**
     * Used to define the motion type used when manipulating an animation property
     */
    public static final int MOTION_TYPE_SPLINE = 2;

    /**
     * Used to define the motion type used when manipulating an animation property
     */
    public static final int MOTION_TYPE_LINEAR = 1;

    // these are package protected for the resource editor
    String imageName;
    Resources res;
    Image img;
    Image[] frames;
    Motion motionX;
    Motion motionY;
    Motion orientation;
    Motion width;
    Motion height;
    Motion opacity;
    int frameWidth;
    int frameHeight;
    int frameDelay = -1;
    private boolean framesInitialized = true;
    private int startTime = -1;
    private int endTime = -1;

    private AnimationObject() {}

    /**
     * Creates a copy of the given animation object
     * 
     * @return a new instance of the Animation object with the same state
     */
    public AnimationObject copy() {
        AnimationObject o = new AnimationObject();
        o.imageName = imageName;
        o.res = res;
        o.img = img;
        o.frames = frames;
        o.motionX = motionX;
        o.motionY = motionY;
        o.orientation = orientation;
        o.width = width;
        o.height = height;
        o.opacity = opacity;
        o.frameWidth = frameWidth;
        o.frameHeight = frameHeight;
        o.frameDelay = frameDelay;
        o.framesInitialized = framesInitialized;
        o.startTime = startTime;
        o.endTime = endTime;
        return o;
    }

    void lock() {
        if(img != null) {
            img.lock();
        }
    }

    void unlock() {
        if(img != null) {
            img.unlock();
        }
    }

    /**
     * Creates an animation object instance that can define the animation properties for an image
     *
     * @param img the image to animate within the timeline
     * @param x position of the animation
     * @param y position of the animation
     * @return new animation object
     */
    public static AnimationObject createAnimationImage(Image img, int x, int y) {
        AnimationObject o = new AnimationObject();
        o.img = img;
        o.motionX = Motion.createLinearMotion(x, x, 1);
        o.motionX.setStartTime(Long.MAX_VALUE);
        o.motionY = Motion.createLinearMotion(y, y, 1);
        o.motionY.setStartTime(Long.MAX_VALUE);
        return o;
    }

    /**
     * Defines the frames of the animation if this is a frame changing animation (e.g.
     * a sprite of a walking person).
     * Notice that this method must not be invoked more than once or after the image
     * was initilized
     *
     * @param frameWidth the width of the frame within the image object
     * @param frameHeight the height of the frame within the image object
     * @param frameDelay the delay of the frame
     */
    public void defineFrames(int frameWidth, int frameHeight, int frameDelay) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameDelay = frameDelay;
        framesInitialized = false;
    }

    /**
     * Creates an animation object instance that can define the animation properties for an image.
     * This version of the method is useful while a resource file is in the process of being loaded
     * and not all images are in place. Loading will finish implicitly when the image is first used.
     *
     * @param imageName the image to animate within the timeline
     * @param res the resources file from which the image should be fetched.
     * @param x position of the animation
     * @param y position of the animation
     * @return new animation object
     */
    public static AnimationObject createAnimationImage(String imageName, Resources res, int x, int y) {
        AnimationObject o = new AnimationObject();
        o.imageName = imageName;
        o.res = res;
        o.motionX = Motion.createLinearMotion(x, x, 1);
        o.motionX.setStartTime(Long.MAX_VALUE);
        o.motionY = Motion.createLinearMotion(y, y, 1);
        o.motionY.setStartTime(Long.MAX_VALUE);
        return o;
    }

    /**
     * @return the img
     */
    Image getImage() {
        if(img == null && res != null) {
            img = res.getImage(imageName);
            
            // can happen due to a race condition we try to fail "gracefully"
            if(img == null) {
                return null;
            }
            res = null;
        }
        if(frameDelay > -1) {
            if(!framesInitialized) {
                // break up the image to smaller images
                frames = new Image[img.getWidth() / frameWidth * img.getHeight() / frameHeight];
                int currentX = 0;
                int currentY = 0;
                for(int iter = 0 ; iter < frames.length ; iter++) {
                    frames[iter] = img.subImage(currentX, currentY, frameWidth, frameHeight, true);
                    currentX += frameWidth;
                    if(currentX + frameWidth > img.getWidth()) {
                        currentX = 0;
                        currentY += frameHeight;
                    }
                }
                // if we are on a device we no longer need the img from now on and can use 
                // only the frames. However, in the resource editor we still need the image instance
                // for internal references
                if(System.getProperty("microedition.platform") != null) {
                    img = null;
                }
            }
            long time = motionX.getCurrentMotionTime();
            int frameCount = Math.max(1, frames.length);
            int frame = Math.min(Math.max(0, (int)((time / Math.max(1, frameDelay)) % frameCount)), frameCount - 1);
            return frames[frame];
        }
        return img;
    }

    private void setTimeNotNull(Motion m, int time) {
        if(m != null) {
            m.setCurrentMotionTime(time);
        }
    }

    void setTime(int time) {
        motionX.setCurrentMotionTime(time);
        motionY.setCurrentMotionTime(time);
        setTimeNotNull(orientation, time);
        setTimeNotNull(width, time);
        setTimeNotNull(height, time);
        setTimeNotNull(opacity, time);
    }

    /**
     * Defines a motion on the x axis starting at the given time/value and ending at the given position
     * 
     * @param motionType the type of the motion (spline/linear)
     * @param startTime the start time for the motion within the timeline timeframe
     * @param duration the duration of the motion
     * @param start the starting position (the value before startTime)
     * @param end the ending position for the property (the value after endTime)
     */
    public void defineMotionX(int motionType, int startTime, int duration, int start, int end) {
        motionX = createMotion(motionType, startTime, duration, start, end);
    }


    /**
     * Defines a motion on the y axis starting at the given time/value and ending at the given position
     *
     * @param motionType the type of the motion (spline/linear)
     * @param startTime the start time for the motion within the timeline timeframe
     * @param duration the duration of the motion
     * @param start the starting position (the value before startTime)
     * @param end the ending position for the property (the value after endTime)
     */
    public void defineMotionY(int motionType, int startTime, int duration, int start, int end) {
        motionY = createMotion(motionType, startTime, duration, start, end);
    }


    /**
     * Defines a rotation animation starting at the given time/value and ending at the given position
     *
     * @param motionType the type of the motion (spline/linear)
     * @param startTime the start time for the motion within the timeline timeframe
     * @param duration the duration of the motion
     * @param start the starting position (the value before startTime)
     * @param end the ending position for the property (the value after endTime)
     */
    public void defineOrientation(int motionType, int startTime, int duration, int start, int end) {
        orientation = createMotion(motionType, startTime, duration, start, end);
    }


    /**
     * Defines opacity (translucency) starting at the given time/value and ending at the given position.
     * Values should rance from 0 (transparent) to 255 (opaque).
     *
     * @param motionType the type of the motion (spline/linear)
     * @param startTime the start time for the motion within the timeline timeframe
     * @param duration the duration of the motion
     * @param start the starting position (the value before startTime)
     * @param end the ending position for the property (the value after endTime)
     */
    public void defineOpacity(int motionType, int startTime, int duration, int start, int end) {
        opacity = createMotion(motionType, startTime, duration, start, end);
    }


    /**
     * Defines the width of the object starting at the given time/value and ending at the given position
     *
     * @param motionType the type of the motion (spline/linear)
     * @param startTime the start time for the motion within the timeline timeframe
     * @param duration the duration of the motion
     * @param start the starting position (the value before startTime)
     * @param end the ending position for the property (the value after endTime)
     */
    public void defineWidth(int motionType, int startTime, int duration, int start, int end) {
        width = createMotion(motionType, startTime, duration, start, end);
    }


    /**
     * Defines the height of the object starting at the given time/value and ending at the given position
     *
     * @param motionType the type of the motion (spline/linear)
     * @param startTime the start time for the motion within the timeline timeframe
     * @param duration the duration of the motion
     * @param start the starting position (the value before startTime)
     * @param end the ending position for the property (the value after endTime)
     */
    public void defineHeight(int motionType, int startTime, int duration, int start, int end) {
        height = createMotion(motionType, startTime, duration, start, end);
    }


    private Motion createMotion(int motionType, int startTime, int duration, int start, int end) {
        Motion m;
        switch(motionType) {
            case MOTION_TYPE_LINEAR:
                m = Motion.createLinearMotion(start, end, startTime + duration);
                break;
            case MOTION_TYPE_SPLINE:
                m = Motion.createSplineMotion(start, end, startTime + duration);
                break;
            default:
                throw new IllegalArgumentException("Motion type: " + motionType);
        }
        m.setStartTime(startTime);
        return m;
    }

    int getX() {
        return motionX.getValue();
    }

    /**
     * @return the motionY
     */
    int getY() {
        return motionY.getValue();
    }

    /**
     * @return the orientation
     */
    int getOrientation() {
        if(orientation == null) {
            return 0;
        }
        return orientation.getValue();
    }

    /**
     * @return the width
     */
    int getWidth() {
        if(width == null) {
            if(getImage() != null) {
                return getImage().getWidth();
            }
            return 20;
        }
        return width.getValue();
    }

    /**
     * @return the height
     */
    int getHeight() {
        if(height == null) {
            if(getImage() != null) {
                return getImage().getHeight();
            }
            return 20;
        }
        return height.getValue();
    }

    /**
     * @return the opacity
     */
    int getOpacity() {
        if(opacity == null) {
            return 255;
        }
        return opacity.getValue();
    }

    void draw(Graphics g, float scaleX, float scaleY) {
        int o = getOpacity();
        if(o == 0) {
            return;
        }
        Image i = getImage();

        // this can happen due to a race condition, mostly in the resource editor which
        // works in a separate thread
        if(i == null) {
            return;
        }
        int scaledImageW = (int)(getWidth() * scaleX);
        int scaledImageH = (int)(getHeight() * scaleY);
        if(scaledImageH < 1 || scaledImageW < 1) {
            return;
        }
        i = getImage().scaled(scaledImageW, scaledImageH);
        if(o != 255) {
            i = i.modifyAlphaWithTranslucency((byte)o);
        }
        int r = getOrientation();
        if(r != 0) {
            i = i.rotate(r);
        }
        int x = getX();
        int y = getY();
        x = (int)(x * scaleX);
        y = (int)(y * scaleY);
        g.drawImage(i, x, y);
    }

    /**
     * The start time of the animation determines when we start actually drawing
     * the animation object. -1 means the duration of the entire animation.
     *
     * @return the startTime in timeline time
     */
    public int getStartTime() {
        return startTime;
    }

    /**
     * The start time of the animation determines when we start actually drawing
     * the animation object. -1 means the duration of the entire animation.
     *
     * @param startTime the startTime to set
     */
    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    /**
     * The end time of the animation determines when we finish actually drawing
     * the animation object. -1 means the duration of the entire animation.
     *
     * @return the endTime in timeline time
     */
    public int getEndTime() {
        return endTime;
    }

    /**
     * The end time of the animation determines when we finish actually drawing
     * the animation object. -1 means the duration of the entire animation.
     *
     * @param endTime the endTime to set
     */
    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
}
