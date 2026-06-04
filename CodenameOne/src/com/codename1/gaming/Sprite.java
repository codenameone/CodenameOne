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
package com.codename1.gaming;

import com.codename1.gaming.physics.PhysicsLinkable;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;

/// A drawable image with position, rotation, scale, alpha and a normalized anchor.
///
/// A sprite draws itself through the `com.codename1.ui.Graphics` affine transform:
/// its `#getX()`/`#getY()` position is the location of its anchor point (the center
/// of the image by default), and rotation and scale pivot around that anchor. When
/// the platform does not support affine transforms the sprite falls back to a plain
/// `com.codename1.ui.Graphics#drawImage(com.codename1.ui.Image, int, int)` that
/// honours position and anchor but ignores rotation and scale.
///
/// `Sprite` implements `com.codename1.gaming.physics.PhysicsLinkable` so a physics
/// body can drive its position and rotation directly -- see
/// `com.codename1.gaming.physics.PhysicsWorld`.
public class Sprite implements PhysicsLinkable {
    private Image image;
    private double x;
    private double y;
    /// rotation in degrees, clockwise.
    private float rotation;
    private float scaleX = 1;
    private float scaleY = 1;
    private int alpha = 255;
    /// normalized anchor (0..1) within the image; 0.5,0.5 is the center.
    private double anchorX = 0.5;
    private double anchorY = 0.5;
    private boolean visible = true;
    private int zOrder;
    private Object userData;

    /// Creates an empty sprite with no image.
    public Sprite() {
    }

    /// Creates a sprite drawing the given image, anchored at its center.
    public Sprite(Image image) {
        this.image = image;
    }

    /// Draws the sprite into the given graphics context.
    ///
    /// The graphics context is expected to already be translated to the coordinate
    /// space the sprite's `#getX()`/`#getY()` are expressed in (for a sprite drawn
    /// directly by a `GameView` that is the view's own coordinate space).
    public void draw(Graphics g) {
        if (!visible || image == null) {
            return;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        float anchorPxX = (float) (anchorX * w);
        float anchorPxY = (float) (anchorY * h);

        int oldAlpha = g.getAlpha();
        if (alpha != 255) {
            g.setAlpha(alpha);
        }

        boolean transformed = (rotation != 0 || scaleX != 1 || scaleY != 1) && g.isTransformSupported();
        if (transformed) {
            Transform restore = g.getTransform();
            Transform t = restore.copy();
            t.translate((float) x, (float) y);
            if (rotation != 0) {
                t.rotate((float) Math.toRadians(rotation), 0, 0);
            }
            if (scaleX != 1 || scaleY != 1) {
                t.scale(scaleX, scaleY);
            }
            g.setTransform(t);
            g.drawImage(image, Math.round(-anchorPxX), Math.round(-anchorPxY));
            g.setTransform(restore);
        } else {
            g.drawImage(image, (int) Math.round(x - anchorPxX), (int) Math.round(y - anchorPxY));
        }

        if (alpha != 255) {
            g.setAlpha(oldAlpha);
        }
    }

    /// Per frame update hook. The default implementation does nothing; subclasses
    /// such as `AnimatedSprite` override it to advance over time. `Scene#update(double)`
    /// invokes this for every sprite it contains.
    ///
    /// #### Parameters
    ///
    /// - `deltaSeconds`: time elapsed since the previous frame, in seconds
    protected void onUpdate(double deltaSeconds) {
    }

    /// Returns the axis aligned bounding box of the (scaled) sprite, ignoring
    /// rotation. Useful for broad phase collision checks.
    public Rectangle getBounds() {
        int w = image == null ? 0 : image.getWidth();
        int h = image == null ? 0 : image.getHeight();
        float sw = w * scaleX;
        float sh = h * scaleY;
        int bx = (int) Math.round(x - anchorX * sw);
        int by = (int) Math.round(y - anchorY * sh);
        return new Rectangle(bx, by, Math.round(sw), Math.round(sh));
    }

    /// Returns true if this sprite's bounding box intersects the other's.
    public boolean intersects(Sprite other) {
        return getBounds().intersects(other.getBounds());
    }

    // ---- PhysicsLinkable -------------------------------------------------

    /// Sets the sprite position from a physics body. The coordinates are the body
    /// center in pixels; with the default center anchor this places the sprite so
    /// its center matches the body.
    public void setPhysicsPosition(float xPx, float yPx) {
        this.x = xPx;
        this.y = yPx;
    }

    /// Sets the sprite rotation from a physics body, converting radians to the
    /// degrees `Sprite` uses internally.
    public void setPhysicsRotation(float radians) {
        this.rotation = (float) Math.toDegrees(radians);
    }

    // ---- accessors -------------------------------------------------------

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /// The rotation in degrees, clockwise.
    public float getRotation() {
        return rotation;
    }

    public void setRotation(float degrees) {
        this.rotation = degrees;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /// The alpha applied while drawing, 0 (transparent) to 255 (opaque).
    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
    }

    public double getAnchorX() {
        return anchorX;
    }

    public double getAnchorY() {
        return anchorY;
    }

    /// Sets the normalized anchor (0..1) used as the position and pivot point.
    /// 0.5,0.5 (the default) is the image center, 0,0 is the top left corner.
    public void setAnchor(double anchorX, double anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getZOrder() {
        return zOrder;
    }

    /// Sets the z-order used by `Scene` to sort sprites; higher values draw on top.
    public void setZOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }
}
