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
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;

/// A drawable image with position, rotation, scale, tint and a normalized anchor.
///
/// A sprite is a lightweight data holder: it describes *what* and *where*, while a
/// `SpriteRenderer` turns it into a GPU textured quad each frame (using the
/// `com.codename1.gpu` package -- an orthographic camera in pixel space, a
/// `com.codename1.gpu.Material.Type#SPRITE` material and alpha blending). Because a
/// sprite never touches the GPU directly you can create one with just an
/// `com.codename1.ui.Image` -- the renderer uploads and caches the matching
/// `com.codename1.gpu.Texture` on demand.
///
/// `#getX()`/`#getY()` is the location of the anchor point (the image center by
/// default); rotation and scale pivot around that anchor. `Sprite` implements
/// `com.codename1.gaming.physics.PhysicsLinkable` so a physics body can drive it --
/// see `com.codename1.gaming.physics.PhysicsWorld`.
public class Sprite implements PhysicsLinkable {
    private Image image;
    private double x;
    private double y;
    /// world-space depth, used only in a perspective `GameCamera`; ignored in 2D.
    private double z;
    /// rotation in degrees, clockwise.
    private float rotation;
    private float scaleX = 1;
    private float scaleY = 1;
    /// ARGB tint multiplied with the texture; opaque white means "draw as-is".
    private int color = 0xffffffff;
    /// normalized anchor (0..1) within the image; 0.5,0.5 is the center.
    private double anchorX = 0.5;
    private double anchorY = 0.5;
    /// explicit quad size in pixels, or <= 0 to use the image's own size.
    private float width = -1;
    private float height = -1;
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

    /// Per frame update hook. The default implementation does nothing; subclasses
    /// such as `AnimatedSprite` override it to advance over time. `Scene#update(double)`
    /// invokes this for every sprite it contains.
    protected void onUpdate(double deltaSeconds) {
    }

    /// The width in pixels the sprite renders at before scaling -- an explicit size
    /// if one was set, otherwise the image width.
    public float getRenderWidth() {
        if (width > 0) {
            return width;
        }
        return image == null ? 0 : image.getWidth();
    }

    /// The height in pixels the sprite renders at before scaling.
    public float getRenderHeight() {
        if (height > 0) {
            return height;
        }
        return image == null ? 0 : image.getHeight();
    }

    /// Returns the axis aligned bounding box of the (scaled) sprite, ignoring
    /// rotation. Useful for broad phase collision checks.
    public Rectangle getBounds() {
        float sw = getRenderWidth() * scaleX;
        float sh = getRenderHeight() * scaleY;
        int bx = (int) Math.round(x - anchorX * sw);
        int by = (int) Math.round(y - anchorY * sh);
        return new Rectangle(bx, by, Math.round(sw), Math.round(sh));
    }

    /// Returns true if this sprite's bounding box intersects the other's.
    public boolean intersects(Sprite other) {
        return getBounds().intersects(other.getBounds());
    }

    // ---- PhysicsLinkable -------------------------------------------------

    public void setPhysicsPosition(float xPx, float yPx) {
        this.x = xPx;
        this.y = yPx;
    }

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

    /// The world-space depth, used when the `GameView`'s `GameCamera` is in
    /// perspective mode (`GameCamera#MODE_PERSPECTIVE`). Ignored in 2D.
    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    /// Sets the full 3D position. In a 2D camera only x and y matter; in a
    /// perspective camera all three place the (billboarded) sprite in world space.
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    /// Overrides the rendered size in pixels (before scaling). Pass values <= 0 to
    /// revert to the image's own dimensions.
    public void setSize(float widthPx, float heightPx) {
        this.width = widthPx;
        this.height = heightPx;
    }

    /// The ARGB tint multiplied with the texture (opaque white = no tint).
    public int getColor() {
        return color;
    }

    public void setColor(int argb) {
        this.color = argb;
    }

    /// The alpha applied while drawing, 0 (transparent) to 255 (opaque). Stored in
    /// the high byte of `#getColor()`.
    public int getAlpha() {
        return (color >>> 24) & 0xff;
    }

    public void setAlpha(int alpha) {
        int a = alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
        color = (a << 24) | (color & 0xffffff);
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
