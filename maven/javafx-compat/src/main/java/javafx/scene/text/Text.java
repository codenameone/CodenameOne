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
package javafx.scene.text;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FontStyles;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;

/// A node that draws text, on one line or several.
///
/// A new line starts at every line feed and, when `wrappingWidth` is
/// above zero, between words wherever a line would become wider than
/// that; a word wider than the whole width is cut. `x` and `y` place the
/// text: `x` is its left edge and `y`, depending on `textOrigin`, the
/// baseline of the first line (the default), or the top, centre or
/// bottom of the whole block.
///
/// #### Measuring
///
/// Widths, line height and ascent come from the Codename One font the
/// JavaFX font maps to, converted to logical pixels, so they are those of
/// the platform's own text engine rather than JavaFX's. The layout bounds
/// are the logical bounds of the block: its width is the widest line, or
/// the wrapping width when one is set, and its height the lines plus the
/// line spacing between them.
///
/// #### Painting
///
/// Text is drawn by the platform with the fill; a gradient fill draws
/// with its middle colour. **The outlines of glyphs are not available, so
/// the stroke of a text is not drawn** and does not enlarge the bounds;
/// stroke properties are recorded only. A point is on the text when it
/// is inside the layout bounds.
///
/// #### Styling
///
/// Besides the names of `javafx.scene.shape.Shape`: `-fx-font`
/// (`Font`), `-fx-font-size` (`Number`), `-fx-font-family` (`String`),
/// `-fx-font-weight` (`FontWeight`, a `Number` from 100 to 900 or a
/// keyword), `-fx-font-style` (`FontPosture` or a keyword),
/// `-fx-text-alignment` (`TextAlignment` or a keyword), `-fx-text-origin`
/// (`javafx.geometry.VPos` or a keyword), `-fx-underline`,
/// `-fx-strikethrough` (`Boolean`) and `-fx-line-spacing` (`Number`).
///
/// #### Not part of this layer
///
/// `boundsType`, `fontSmoothingType`, the selection and caret properties,
/// `hitTest` and tab size. `JUSTIFY` aligns to the left.
public class Text extends Shape {

    private final StringProperty text = new FxString(this, "text", "", Dirty.GEOMETRY);
    private final DoubleProperty x = new FxDouble(this, "x", 0, Dirty.GEOMETRY);
    private final DoubleProperty y = new FxDouble(this, "y", 0, Dirty.GEOMETRY);
    private final ObjectProperty<Font> font = new FxObject<Font>(this, "font", null, Dirty.GEOMETRY);
    private final ObjectProperty<VPos> textOrigin = new FxObject<VPos>(this, "textOrigin", VPos.BASELINE,
            Dirty.GEOMETRY);
    private final DoubleProperty wrappingWidth = new FxDouble(this, "wrappingWidth", 0, Dirty.GEOMETRY);
    private final ObjectProperty<TextAlignment> textAlignment = new FxObject<TextAlignment>(this, "textAlignment",
            TextAlignment.LEFT, Dirty.PAINT);
    private final BooleanProperty underline = new FxBoolean(this, "underline", false, Dirty.PAINT);
    private final BooleanProperty strikethrough = new FxBoolean(this, "strikethrough", false, Dirty.PAINT);
    private final DoubleProperty lineSpacing = new FxDouble(this, "lineSpacing", 0, Dirty.GEOMETRY);

    private String[] lines;

    /// Creates a text node without text.
    public Text() {
    }

    /// Creates a text node.
    public Text(String text) {
        this.text.set(text);
    }

    /// Creates a text node at a position.
    public Text(double x, double y, String text) {
        this.x.set(x);
        this.y.set(y);
        this.text.set(text);
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & Dirty.GEOMETRY) != 0) {
            lines = null;
        }
        super.cn1Invalidated(what);
    }

    /// Returns the text; never `null`.
    public final String getText() {
        String t = text.get();
        return t == null ? "" : t;
    }

    /// Sets the text.
    public final void setText(String value) {
        text.set(value);
    }

    /// The text.
    public final StringProperty textProperty() {
        return text;
    }

    /// Returns the x of the left edge.
    public final double getX() {
        return x.get();
    }

    /// Sets the x of the left edge.
    public final void setX(double value) {
        x.set(value);
    }

    /// The x of the left edge.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns the y of the text origin.
    public final double getY() {
        return y.get();
    }

    /// Sets the y of the text origin.
    public final void setY(double value) {
        y.set(value);
    }

    /// The y of the text origin.
    public final DoubleProperty yProperty() {
        return y;
    }

    /// Returns the font; the default font when none was set.
    public final Font getFont() {
        Font f = font.get();
        return f == null ? Font.getDefault() : f;
    }

    /// Sets the font; `null` returns to the default font.
    public final void setFont(Font value) {
        font.set(value);
    }

    /// The font.
    public final ObjectProperty<Font> fontProperty() {
        return font;
    }

    /// Returns what `y` is the position of.
    public final VPos getTextOrigin() {
        VPos v = textOrigin.get();
        return v == null ? VPos.BASELINE : v;
    }

    /// Sets what `y` is the position of: the baseline of the first line,
    /// or the top, centre or bottom of the text.
    public final void setTextOrigin(VPos value) {
        textOrigin.set(value);
    }

    /// What `y` is the position of.
    public final ObjectProperty<VPos> textOriginProperty() {
        return textOrigin;
    }

    /// Returns the width lines are wrapped at; zero for no wrapping.
    public final double getWrappingWidth() {
        return wrappingWidth.get();
    }

    /// Sets the width lines are wrapped at; zero for no wrapping.
    public final void setWrappingWidth(double value) {
        wrappingWidth.set(value);
    }

    /// The width lines are wrapped at.
    public final DoubleProperty wrappingWidthProperty() {
        return wrappingWidth;
    }

    /// Returns how lines are aligned within the width of the text.
    public final TextAlignment getTextAlignment() {
        TextAlignment a = textAlignment.get();
        return a == null ? TextAlignment.LEFT : a;
    }

    /// Sets how lines are aligned within the width of the text.
    public final void setTextAlignment(TextAlignment value) {
        textAlignment.set(value);
    }

    /// How lines are aligned within the width of the text.
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        return textAlignment;
    }

    /// Returns whether a line is drawn under the text.
    public final boolean isUnderline() {
        return underline.get();
    }

    /// Sets whether a line is drawn under the text.
    public final void setUnderline(boolean value) {
        underline.set(value);
    }

    /// Whether a line is drawn under the text.
    public final BooleanProperty underlineProperty() {
        return underline;
    }

    /// Returns whether a line is drawn through the text.
    public final boolean isStrikethrough() {
        return strikethrough.get();
    }

    /// Sets whether a line is drawn through the text.
    public final void setStrikethrough(boolean value) {
        strikethrough.set(value);
    }

    /// Whether a line is drawn through the text.
    public final BooleanProperty strikethroughProperty() {
        return strikethrough;
    }

    /// Returns the extra distance between lines.
    public final double getLineSpacing() {
        return lineSpacing.get();
    }

    /// Sets the extra distance between lines.
    public final void setLineSpacing(double value) {
        lineSpacing.set(value);
    }

    /// The extra distance between lines.
    public final DoubleProperty lineSpacingProperty() {
        return lineSpacing;
    }

    private String[] textLines() {
        if (lines == null) {
            lines = FontStyles.lines(getText(), getFont(), getWrappingWidth());
        }
        return lines;
    }

    private double blockWidth() {
        double wrap = getWrappingWidth();
        if (wrap > 0) {
            return wrap;
        }
        double widest = 0;
        String[] all = textLines();
        Font f = getFont();
        for (int i = 0; i < all.length; i++) {
            widest = Math.max(widest, Fonts.width(f, all[i]));
        }
        return widest;
    }

    private double blockHeight() {
        int n = textLines().length;
        return n * Fonts.lineHeight(getFont()) + (n - 1) * getLineSpacing();
    }

    private double top() {
        VPos origin = getTextOrigin();
        if (origin == VPos.TOP) {
            return getY();
        } else if (origin == VPos.CENTER) {
            return getY() - blockHeight() / 2;
        } else if (origin == VPos.BOTTOM) {
            return getY() - blockHeight();
        }
        return getY() - Fonts.ascent(getFont());
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        return new BoundingBox(getX(), top(), blockWidth(), blockHeight());
    }

    @Override
    public double getBaselineOffset() {
        return Fonts.ascent(getFont());
    }

    @Override
    public boolean cn1PaintsOutsideBounds() {
        return false;
    }

    @Override
    public boolean contains(double localX, double localY) {
        return getLayoutBounds().contains(localX, localY);
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        p.addRect(getX(), top(), blockWidth(), blockHeight());
        return p;
    }

    /// The runs a flow cut this text into and where each starts, counted
    /// from the left of the text; `null` for a text drawn whole.
    private String[] flowParts;
    private double[] flowOffsets;

    /// Has the text drawn as runs of one direction each, which a flow
    /// places apart when the order they are read in is not the order
    /// they are seen in. `null` draws the text whole again.
    void flowRuns(String[] parts, double[] offsets) {
        if (parts == null && flowParts == null) {
            return;
        }
        flowParts = parts;
        flowOffsets = offsets;
        cn1Repaint();
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        Paint fill = getFill();
        if (fill == null) {
            return;
        }
        Font f = getFont();
        String[] all = textLines();
        double width = blockWidth();
        double lineHeight = Fonts.lineHeight(f);
        double ascent = Fonts.ascent(f);
        double thickness = Math.max(1, f.getSize() / 14);
        if (flowParts != null && flowOffsets != null) {
            double y = top();
            for (int i = 0; i < flowParts.length && i < flowOffsets.length; i++) {
                double w = Fonts.width(f, flowParts[i]);
                double lx = getX() + flowOffsets[i];
                renderer.drawText(flowParts[i], lx, y, f, fill);
                if (w > 0 && isUnderline()) {
                    renderer.fillRect(lx, y + ascent + thickness, w, thickness, fill);
                }
                if (w > 0 && isStrikethrough()) {
                    renderer.fillRect(lx, y + ascent * 0.65, w, thickness, fill);
                }
            }
            return;
        }
        TextAlignment align = getTextAlignment();
        double lineTop = top();
        for (int i = 0; i < all.length; i++) {
            String line = all[i];
            double w = Fonts.width(f, line);
            double lx = getX();
            if (align == TextAlignment.CENTER) {
                lx += (width - w) / 2;
            } else if (align == TextAlignment.RIGHT) {
                lx += width - w;
            }
            renderer.drawText(line, lx, lineTop, f, fill);
            if (w > 0 && isUnderline()) {
                renderer.fillRect(lx, lineTop + ascent + thickness, w, thickness, fill);
            }
            if (w > 0 && isStrikethrough()) {
                renderer.fillRect(lx, lineTop + ascent * 0.65, w, thickness, fill);
            }
            lineTop += lineHeight + getLineSpacing();
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if (FontStyles.isFontProperty(property)) {
            return font.get();
        } else if ("-fx-text-alignment".equals(property)) {
            return getTextAlignment();
        } else if ("-fx-text-origin".equals(property)) {
            return getTextOrigin();
        } else if ("-fx-underline".equals(property)) {
            return Boolean.valueOf(isUnderline());
        } else if ("-fx-strikethrough".equals(property)) {
            return Boolean.valueOf(isStrikethrough());
        } else if ("-fx-line-spacing".equals(property)) {
            return Double.valueOf(getLineSpacing());
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if (FontStyles.isFontProperty(property)) {
            // Restoring any of the font names restores the whole font.
            if (value == null) {
                setFont(null);
                return true;
            }
            Font styled = FontStyles.apply(getFont(), property, value);
            if (styled == null) {
                return false;
            }
            setFont(styled);
        } else if ("-fx-text-alignment".equals(property)) {
            Object k = FontStyles.keyword(TextAlignment.values(), value);
            if (!(k instanceof TextAlignment)) {
                return false;
            }
            setTextAlignment((TextAlignment) k);
        } else if ("-fx-text-origin".equals(property)) {
            Object k = FontStyles.keyword(VPos.values(), value);
            if (!(k instanceof VPos)) {
                return false;
            }
            setTextOrigin((VPos) k);
        } else if ("-fx-underline".equals(property) || "-fx-strikethrough".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            boolean on = ((Boolean) value).booleanValue();
            if ("-fx-underline".equals(property)) {
                setUnderline(on);
            } else {
                setStrikethrough(on);
            }
        } else if ("-fx-line-spacing".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setLineSpacing(((Number) value).doubleValue());
        } else {
            return super.cn1SetStyleValue(property, value);
        }
        return true;
    }
}
