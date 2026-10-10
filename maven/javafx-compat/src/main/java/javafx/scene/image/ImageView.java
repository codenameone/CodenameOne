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
package javafx.scene.image;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;

/// A node that shows an [Image], or the part of it a viewport selects,
/// at its own size or fitted into a box.
///
/// The size shown starts from the viewport's size, or the image's when
/// there is no viewport. `fitWidth` and `fitHeight`, when above zero,
/// replace the width and the height; with `preserveRatio` the picture is
/// instead scaled by one factor so that it fits inside what was given:
/// the smaller of the two factors when both are set, the one that is set
/// otherwise.
///
/// `smooth` is recorded; scaling quality is the platform's. The `-fx-image`
/// style name is not part of this layer.
public class ImageView extends Node {

    private final ObjectProperty<Image> image = new FxObject<Image>(this, "image", null, Dirty.GEOMETRY);
    private final DoubleProperty x = new FxDouble(this, "x", 0, Dirty.GEOMETRY);
    private final DoubleProperty y = new FxDouble(this, "y", 0, Dirty.GEOMETRY);
    private final DoubleProperty fitWidth = new FxDouble(this, "fitWidth", 0, Dirty.GEOMETRY);
    private final DoubleProperty fitHeight = new FxDouble(this, "fitHeight", 0, Dirty.GEOMETRY);
    private final BooleanProperty preserveRatio = new FxBoolean(this, "preserveRatio", false, Dirty.GEOMETRY);
    private final BooleanProperty smooth = new FxBoolean(this, "smooth", true, Dirty.PAINT);
    private final ObjectProperty<Rectangle2D> viewport = new FxObject<Rectangle2D>(this, "viewport", null,
            Dirty.GEOMETRY);

    private com.codename1.ui.Image shown;
    private boolean shownValid;
    private Image heard;
    private final Runnable pixelsChanged = new Runnable() {
        @Override
        public void run() {
            pixelsChanged();
        }
    };

    /// Creates a view without an image.
    public ImageView() {
    }

    /// Creates a view of the image at a URL; see [Image] for where it is
    /// looked up.
    public ImageView(String url) {
        image.set(new Image(url));
    }

    /// Creates a view of an image.
    public ImageView(Image image) {
        this.image.set(image);
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & Dirty.GEOMETRY) != 0) {
            shown = null;
            shownValid = false;
            // An image that is written to tells the view it is shown in.
            Image now = image == null ? null : image.get();
            if (now != heard) {
                if (heard != null) {
                    heard.removePixelListener(pixelsChanged);
                }
                heard = now;
                if (now != null) {
                    now.addPixelListener(pixelsChanged);
                }
            }
        }
        super.cn1Invalidated(what);
    }

    private void pixelsChanged() {
        shown = null;
        shownValid = false;
        super.cn1Invalidated(Dirty.PAINT);
    }

    /// Returns the image, or `null`.
    public final Image getImage() {
        return image.get();
    }

    /// Sets the image.
    public final void setImage(Image value) {
        image.set(value);
    }

    /// The image.
    public final ObjectProperty<Image> imageProperty() {
        return image;
    }

    /// Returns the x of the top left corner.
    public final double getX() {
        return x.get();
    }

    /// Sets the x of the top left corner.
    public final void setX(double value) {
        x.set(value);
    }

    /// The x of the top left corner.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns the y of the top left corner.
    public final double getY() {
        return y.get();
    }

    /// Sets the y of the top left corner.
    public final void setY(double value) {
        y.set(value);
    }

    /// The y of the top left corner.
    public final DoubleProperty yProperty() {
        return y;
    }

    /// Returns the width to fit the picture into; 0 for its own.
    public final double getFitWidth() {
        return fitWidth.get();
    }

    /// Sets the width to fit the picture into; 0 for its own.
    public final void setFitWidth(double value) {
        fitWidth.set(value);
    }

    /// The width to fit the picture into.
    public final DoubleProperty fitWidthProperty() {
        return fitWidth;
    }

    /// Returns the height to fit the picture into; 0 for its own.
    public final double getFitHeight() {
        return fitHeight.get();
    }

    /// Sets the height to fit the picture into; 0 for its own.
    public final void setFitHeight(double value) {
        fitHeight.set(value);
    }

    /// The height to fit the picture into.
    public final DoubleProperty fitHeightProperty() {
        return fitHeight;
    }

    /// Returns whether fitting keeps the picture's proportions.
    public final boolean isPreserveRatio() {
        return preserveRatio.get();
    }

    /// Sets whether fitting keeps the picture's proportions.
    public final void setPreserveRatio(boolean value) {
        preserveRatio.set(value);
    }

    /// Whether fitting keeps the picture's proportions.
    public final BooleanProperty preserveRatioProperty() {
        return preserveRatio;
    }

    /// Returns whether a better scaling filter was asked for.
    public final boolean isSmooth() {
        return smooth.get();
    }

    /// Records whether a better scaling filter is asked for.
    public final void setSmooth(boolean value) {
        smooth.set(value);
    }

    /// Whether a better scaling filter is asked for.
    public final BooleanProperty smoothProperty() {
        return smooth;
    }

    /// Returns the part of the image that is shown, in its pixels, or
    /// `null` for all of it.
    public final Rectangle2D getViewport() {
        return viewport.get();
    }

    /// Sets the part of the image that is shown.
    public final void setViewport(Rectangle2D value) {
        viewport.set(value);
    }

    /// The part of the image that is shown.
    public final ObjectProperty<Rectangle2D> viewportProperty() {
        return viewport;
    }

    private double sourceWidth() {
        Rectangle2D v = getViewport();
        if (v != null && v.getWidth() > 0 && v.getHeight() > 0) {
            return v.getWidth();
        }
        Image i = getImage();
        return i == null ? 0 : i.getWidth();
    }

    private double sourceHeight() {
        Rectangle2D v = getViewport();
        if (v != null && v.getWidth() > 0 && v.getHeight() > 0) {
            return v.getHeight();
        }
        Image i = getImage();
        return i == null ? 0 : i.getHeight();
    }

    /// Writes the size the picture is shown at into `{width, height}`.
    private double[] shownSize() {
        double w = sourceWidth();
        double h = sourceHeight();
        double fw = getFitWidth();
        double fh = getFitHeight();
        if (!(w > 0) || !(h > 0)) {
            return new double[] {0, 0};
        }
        if (isPreserveRatio()) {
            double k = 1;
            if (fw > 0 && fh > 0) {
                k = Math.min(fw / w, fh / h);
            } else if (fw > 0) {
                k = fw / w;
            } else if (fh > 0) {
                k = fh / h;
            }
            return new double[] {w * k, h * k};
        }
        return new double[] {fw > 0 ? fw : w, fh > 0 ? fh : h};
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        double[] size = shownSize();
        return new BoundingBox(getX(), getY(), size[0], size[1]);
    }

    private com.codename1.ui.Image picture() {
        if (shownValid) {
            return shown;
        }
        shownValid = true;
        shown = null;
        Image i = getImage();
        com.codename1.ui.Image full = i == null ? null : i.cn1Native();
        if (full == null) {
            return null;
        }
        shown = full;
        Rectangle2D v = getViewport();
        if (v != null && v.getWidth() > 0 && v.getHeight() > 0) {
            // The part of the viewport that lies on the picture.
            int x1 = (int) Math.max(0, Math.round(v.getMinX()));
            int y1 = (int) Math.max(0, Math.round(v.getMinY()));
            int x2 = (int) Math.min(full.getWidth(), Math.round(v.getMaxX()));
            int y2 = (int) Math.min(full.getHeight(), Math.round(v.getMaxY()));
            shown = x2 > x1 && y2 > y1 ? full.subImage(x1, y1, x2 - x1, y2 - y1, true) : null;
        }
        return shown;
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        com.codename1.ui.Image picture = picture();
        if (picture == null) {
            return;
        }
        double[] size = shownSize();
        renderer.drawImage(picture, getX(), getY(), size[0], size[1]);
    }
}
