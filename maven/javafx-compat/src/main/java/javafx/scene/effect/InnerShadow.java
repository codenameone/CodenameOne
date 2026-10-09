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
package javafx.scene.effect;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

/// A shadow cast inside the edges of a node, drawn over what it paints:
/// see [Effect].
public class InnerShadow extends Effect {

    private final ObjectProperty<Effect> input = new SimpleObjectProperty<Effect>(this, "input");
    private final DoubleProperty radius = new SimpleDoubleProperty(this, "radius", 10);
    private final DoubleProperty width = new SimpleDoubleProperty(this, "width", 21);
    private final DoubleProperty height = new SimpleDoubleProperty(this, "height", 21);
    private final ObjectProperty<BlurType> blurType = new SimpleObjectProperty<BlurType>(this, "blurType",
            BlurType.THREE_PASS_BOX);
    private final DoubleProperty choke = new SimpleDoubleProperty(this, "choke", 0);
    private final ObjectProperty<Color> color = new SimpleObjectProperty<Color>(this, "color", Color.BLACK);
    private final DoubleProperty offsetX = new SimpleDoubleProperty(this, "offsetX", 0);
    private final DoubleProperty offsetY = new SimpleDoubleProperty(this, "offsetY", 0);

    /// Creates a black shadow of radius ten with no offset.
    public InnerShadow() {
    }

    /// Creates a shadow of a radius and a color.
    public InnerShadow(double radius, Color color) {
        setRadius(radius);
        setColor(color);
    }

    /// Creates a shadow of a radius, an offset and a color.
    public InnerShadow(double radius, double offsetX, double offsetY, Color color) {
        setRadius(radius);
        setOffsetX(offsetX);
        setOffsetY(offsetY);
        setColor(color);
    }

    /// Creates a shadow with every value given.
    public InnerShadow(BlurType blurType, Color color, double radius, double choke, double offsetX,
            double offsetY) {
        setBlurType(blurType);
        setColor(color);
        setRadius(radius);
        setChoke(choke);
        setOffsetX(offsetX);
        setOffsetY(offsetY);
    }

    /// Returns the effect this one is applied on top of.
    public final Effect getInput() {
        return input.get();
    }

    /// Sets the effect this one is applied on top of.
    public final void setInput(Effect value) {
        input.set(value);
    }

    /// The effect this one is applied on top of.
    public final ObjectProperty<Effect> inputProperty() {
        return input;
    }

    /// Returns the radius of the blur, from 0 to 127.
    public final double getRadius() {
        return radius.get();
    }

    /// Sets the radius of the blur; the width and the height follow it.
    public final void setRadius(double value) {
        double r = DropShadow.clamp(value, 0, 127);
        radius.set(r);
        width.set(r * 2 + 1);
        height.set(r * 2 + 1);
    }

    /// The radius of the blur.
    public final DoubleProperty radiusProperty() {
        return radius;
    }

    /// Returns the width of the blur, from 0 to 255.
    public final double getWidth() {
        return width.get();
    }

    /// Sets the width of the blur; the radius follows it.
    public final void setWidth(double value) {
        width.set(DropShadow.clamp(value, 0, 255));
        radius.set(((width.get() + height.get()) / 2 - 1) / 2);
    }

    /// The width of the blur.
    public final DoubleProperty widthProperty() {
        return width;
    }

    /// Returns the height of the blur, from 0 to 255.
    public final double getHeight() {
        return height.get();
    }

    /// Sets the height of the blur; the radius follows it.
    public final void setHeight(double value) {
        height.set(DropShadow.clamp(value, 0, 255));
        radius.set(((width.get() + height.get()) / 2 - 1) / 2);
    }

    /// The height of the blur.
    public final DoubleProperty heightProperty() {
        return height;
    }

    /// Returns the way the shadow is softened.
    public final BlurType getBlurType() {
        BlurType t = blurType.get();
        return t == null ? BlurType.THREE_PASS_BOX : t;
    }

    /// Sets the way the shadow is softened.
    public final void setBlurType(BlurType value) {
        blurType.set(value);
    }

    /// The way the shadow is softened.
    public final ObjectProperty<BlurType> blurTypeProperty() {
        return blurType;
    }

    /// Returns the part of the radius that is solid, from 0 to 1.
    public final double getChoke() {
        return choke.get();
    }

    /// Sets the part of the radius that is solid.
    public final void setChoke(double value) {
        choke.set(DropShadow.clamp(value, 0, 1));
    }

    /// The part of the radius that is solid.
    public final DoubleProperty chokeProperty() {
        return choke;
    }

    /// Returns the color of the shadow.
    public final Color getColor() {
        Color c = color.get();
        return c == null ? Color.BLACK : c;
    }

    /// Sets the color of the shadow.
    public final void setColor(Color value) {
        color.set(value);
    }

    /// The color of the shadow.
    public final ObjectProperty<Color> colorProperty() {
        return color;
    }

    /// Returns how far to the right the shadow falls.
    public final double getOffsetX() {
        return offsetX.get();
    }

    /// Sets how far to the right the shadow falls.
    public final void setOffsetX(double value) {
        offsetX.set(value);
    }

    /// How far to the right the shadow falls.
    public final DoubleProperty offsetXProperty() {
        return offsetX;
    }

    /// Returns how far down the shadow falls.
    public final double getOffsetY() {
        return offsetY.get();
    }

    /// Sets how far down the shadow falls.
    public final void setOffsetY(double value) {
        offsetY.set(value);
    }

    /// How far down the shadow falls.
    public final DoubleProperty offsetYProperty() {
        return offsetY;
    }
}
