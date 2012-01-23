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
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

/**
 * A timeline represents the motions of animation objects
 *
 * @author Shai Almog
 */
public final class Timeline extends Image implements Animation, Painter {
    private int time;
    private int duration;
    AnimationObject[] animations;
    private Dimension size;
    private Dimension scaledTo;
    private long currentTime = -1;

    /**
     * Inidicates the minimal delay between animation frames allowing the CPU to rest.
     * Increase this number to increase general device performance, decrease it to speed
     * the animation.
     */
    private int animationDelay = 100;

    private boolean pause;

    private boolean loop = true;

    private Timeline() {
        super(null);
    }

    /**
     * @inheritDoc
     */
    public void lock() {
        if(animations != null) {
            for(int iter = 0 ; iter < animations.length ; iter++) {
                animations[iter].lock();
            }
        }
    }


    /**
     * @inheritDoc
     */
    public void unlock() {
        if(animations != null) {
            for(int iter = 0 ; iter < animations.length ; iter++) {
                animations[iter].unlock();
            }
        }
    }

    /**
     * @inheritDoc
     */
    public int[] getRGB() {
        Image i = Image.createImage(getWidth(), getHeight());
        paint(i.getGraphics(), new Rectangle(0, 0, getWidth(), getHeight()));
        return i.getRGB();
    }

    /**
     * @inheritDoc
     */
    public int[] getRGBCached() {
        return getRGB();
    }

    /**
     * Create a new timeline animation
     *
     * @param duration the duration of the animation in milliseconds
     * @param animations the animation objects that are part of this timeline
     * @param size size of the animation in virtual pixels, if the size differs the animation would be
     * scaled on the fly
     * @return the new timeline instance
     */
    public static Timeline createTimeline(int duration, AnimationObject[] animations, Dimension size) {
        if(duration <= 0) {
            throw new IllegalArgumentException("Illegal duration " + duration);
        }
        Timeline t = new Timeline();
        t.duration = duration;
        t.animations = animations;
        t.size = size;
        return t;
    }

    /**
     * Adds an animation object to show using this timeline
     * 
     * @param o animation object featured in this timeline
     */
    public void addAnimation(AnimationObject o) {
        AnimationObject[] n = new AnimationObject[animations.length + 1];
        System.arraycopy(animations, 0, n, 0, animations.length);
        n[animations.length] = o;
        animations = n;
    }

    /**
     * Set the time of the timeline
     *
     * @param time the time of the timeline in ms starting from 0
     */
    public void setTime(int time) {
        if(!pause) {
            if(time >= 0 && time <= duration) {
                this.time = time;
                currentTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Returns the time of the timeline
     *
     * @return the time of the timeline in ms starting from 0
     */
    public int getTime() {
        return time;
    }

    /**
     * @inheritDoc
     */
    public boolean isAnimation() {
        return true;
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        if(!pause) {
            if(currentTime < 0) {
                currentTime = System.currentTimeMillis();
                setTime(0);
                return true;
            } else {
                long newCurrentTime = System.currentTimeMillis();
                if(newCurrentTime - currentTime >= animationDelay) {
                    int newTime = (int)(time + (newCurrentTime - currentTime));
                    currentTime = newCurrentTime;
                    if(newTime > duration) {
                        if(!loop) {
                            return false;
                        }
                        newTime = 0;
                    }
                    setTime(newTime);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        paint(g, null);
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g, Rectangle rect) {
        float scaleX = 1;
        float scaleY = 1;
        if(rect != null) {
            scaleX = ((float)rect.getSize().getWidth()) / ((float)size.getWidth());
            scaleY = ((float)rect.getSize().getHeight()) / ((float)size.getHeight());
        }
        paintScaled(g, scaleX, scaleY);
    }

    private void paintScaled(Graphics g, float scaleX, float scaleY) {
        for(int iter = 0 ; iter < animations.length ; iter++) {
            int s = animations[iter].getStartTime();
            if(s > -1 && s > time) {
                continue;
            }
            int e = animations[iter].getEndTime();
            if(e > -1 && e < time) {
                continue;
            }
            animations[iter].setTime(time);
            animations[iter].draw(g, scaleX, scaleY);
        }
    }

    /**
     * Inidicates the minimal delay between animation frames allowing the CPU to rest.
     * Increase this number to increase general device performance, decrease it to speed
     * the animation.
     *
     * @return the animationDelay
     */
    public int getAnimationDelay() {
        return animationDelay;
    }

    /**
     * Inidicates the minimal delay between animation frames allowing the CPU to rest.
     * Increase this number to increase general device performance, decrease it to speed
     * the animation.
     *
     * @param animationDelay the animationDelay to set
     */
    public void setAnimationDelay(int animationDelay) {
        this.animationDelay = animationDelay;
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        g.translate(x, y);
        if(scaledTo != null) {
            float scaleX = ((float)scaledTo.getWidth()) / ((float)size.getWidth());
            float scaleY = ((float)scaledTo.getHeight()) / ((float)size.getHeight());
            paintScaled(g, scaleX, scaleY);
        } else {
            paint(g);
        }
        g.translate(-x, -y);
    }

    /**
     * @inheritDoc
     */
    public int getWidth() {
        if(scaledTo != null) {
            return scaledTo.getWidth();
        }
        return size.getWidth();
    }

    /**
     * @inheritDoc
     */
    public int getHeight() {
        if(scaledTo != null) {
            return scaledTo.getHeight();
        }
        return size.getHeight();
    }

    /**
     * @inheritDoc
     */
    public Image scaled(int width, int height) {
        Timeline t = new Timeline();
        t.animationDelay = animationDelay;
        t.animations = animations;
        t.currentTime = currentTime;
        t.duration = duration;
        t.size = size;
        t.time = time;
        t.scaledTo = new Dimension(width, height);
        return t;
    }

    /**
     * Returns true when the timeline is paused
     *
     * @return the pause state
     */
    public boolean isPause() {
        return pause;
    }

    /**
     * Indicate that the application is paused
     *
     * @param pause true to pause the application
     */
    public void setPause(boolean pause) {
        this.pause = pause;
    }

    /**
     * Returns the duration of the entire timeline in milliseconds
     *
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Returns the pixel based unscaled dimentions of this timeline
     *
     * @return the size
     */
    public Dimension getSize() {
        return size;
    }

    /**
     * Returns the number of animation objects in this timeline
     *
     * @return the number of animations
     */
    public int getAnimationCount() {
        return animations.length;
    }

    /**
     * Returns the animation object in the given offset
     *
     * @param i the offset of the animation
     * @return the animation object
     */
    public AnimationObject getAnimation(int i) {
        return animations[i];
    }

    /**
     * Returns the animation object at the given X/Y coordinate in the timeline
     * for the current frame. This allows functionality such as responding to pointer
     * events on the resource editor. Notice that this method is not efficient since it tests
     * the pixel opacity which is a pretty expensive operation...
     *
     * @param x the x location in the timeline
     * @param y the y location in the timeline
     * @return an animation object or null if no animation object is at that position.
     */
    public AnimationObject getAnimationAt(int x, int y) {
        for(int iter = 0 ; iter < animations.length ; iter++) {
            float scaleX = 1;
            float scaleY = 1;
            if(scaledTo != null) {
                scaleX = ((float)scaledTo.getWidth()) / ((float)size.getWidth());
                scaleY = ((float)scaledTo.getHeight()) / ((float)size.getHeight());
            }
            int w = (int)(animations[iter].getWidth() * scaleX);
            int h = (int)(animations[iter].getHeight() * scaleY);
            int ax = (int)(animations[iter].getX() * scaleX);
            int ay = (int)(animations[iter].getY() * scaleY);
            if(Rectangle.intersects(ax, ay, w, h, x, y, 1, 1)) {
                // we now need to check if the pixel at that position is not transparent
                int[] rgb = animations[iter].getImage().scaled(w, h).getRGB();
                int relativeX = x - ax;
                int relativeY = y - ay;
                int offset = relativeX + (relativeY * h);
                if(offset >= 0 && offset < rgb.length && (rgb[offset] & 0xff000000) != 0) {
                    return animations[iter];
                }
            }
        }
        return null;
    }

    /**
     * Indicates if the image should loop
     *
     * @return the loop
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * Indicates if the image should loop
     *
     * @param loop the loop to set
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }
}
