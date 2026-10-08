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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/// A control with text: labels, buttons, check boxes and the like.
///
/// The native component is a Codename One `Label` or one of its
/// subclasses; text, text colour, font, alignment, underline and the gap
/// to the icon are copied into it. A font or colour that was never set
/// leaves the theme's own in place.
///
/// The graphic node is recorded; a subclass that can show it as the
/// native icon does so. `wrapText`, `ellipsisString`, `textOverrun`,
/// `lineSpacing` and `mnemonicParsing` are recorded or absent and have no
/// effect on a single line native label.
///
/// Styled through `cn1ApplyStyle` with the `Labeled` names listed in
/// `com.codename1.fxcompat.runtime.StyleTarget`.
public abstract class Labeled extends Control {

    private static final int TEXT = Dirty.NATIVE | Dirty.LAYOUT;

    private final StringProperty text = new FxString(this, "text", "", TEXT);
    private final ObjectProperty<Font> font = new FxObject<Font>(this, "font", null, TEXT);
    private final ObjectProperty<Paint> textFill = new FxObject<Paint>(this, "textFill", null, Dirty.NATIVE);
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.CENTER_LEFT,
            Dirty.NATIVE);
    private final ObjectProperty<TextAlignment> textAlignment = new FxObject<TextAlignment>(this, "textAlignment",
            TextAlignment.LEFT, Dirty.NATIVE);
    private final BooleanProperty wrapText = new FxBoolean(this, "wrapText", false, TEXT);
    private final BooleanProperty underline = new FxBoolean(this, "underline", false, Dirty.NATIVE);
    private final ObjectProperty<Node> graphic = new FxObject<Node>(this, "graphic", null, TEXT);
    private final DoubleProperty graphicTextGap = new FxDouble(this, "graphicTextGap", 4, TEXT);
    private final ObjectProperty<ContentDisplay> contentDisplay = new FxObject<ContentDisplay>(this,
            "contentDisplay", ContentDisplay.LEFT, TEXT);

    /// Creates a labeled control with no text.
    public Labeled() {
    }

    /// Creates a labeled control with text.
    public Labeled(String text) {
        setText(text);
    }

    /// Creates a labeled control with text and a graphic.
    public Labeled(String text, Node graphic) {
        setText(text);
        setGraphic(graphic);
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & Dirty.NATIVE) != 0 && (what & Dirty.LAYOUT) != 0) {
            Component c = cn1NativeIfCreated();
            if (c != null) {
                c.setShouldCalcPreferredSize(true);
            }
        }
        super.cn1Invalidated(what);
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (!(c instanceof com.codename1.ui.Label)) {
            return;
        }
        com.codename1.ui.Label label = (com.codename1.ui.Label) c;
        String t = getText();
        label.setText(t == null || getContentDisplay() == ContentDisplay.GRAPHIC_ONLY ? "" : t);
        Style style = label.getAllStyles();
        Paint fill = getTextFill();
        if (fill instanceof Color) {
            int argb = ((Color) fill).cn1Argb();
            style.setFgColor(argb & 0xffffff);
            style.setOpacity(argb >>> 24);
        }
        Font f = font.get();
        if (f != null) {
            style.setFont(Fonts.of(f));
        }
        style.setTextDecoration(isUnderline() ? Style.TEXT_DECORATION_UNDERLINE : Style.TEXT_DECORATION_NONE);
        Pos pos = getAlignment();
        HPos h = pos == null ? HPos.LEFT : pos.getHpos();
        label.setAlignment(h == HPos.CENTER ? Component.CENTER : (h == HPos.RIGHT ? Component.RIGHT : Component.LEFT));
        label.setGap(Units.toPixels(getGraphicTextGap()));
    }

    /// Returns the text.
    public final String getText() {
        return text.get();
    }

    /// Sets the text.
    public final void setText(String value) {
        text.set(value);
    }

    /// The text of the control.
    public final StringProperty textProperty() {
        return text;
    }

    /// Returns the font; the default font when none was set.
    public final Font getFont() {
        Font f = font.get();
        return f == null ? Font.getDefault() : f;
    }

    /// Sets the font.
    public final void setFont(Font value) {
        font.set(value);
    }

    /// The font of the text.
    public final ObjectProperty<Font> fontProperty() {
        return font;
    }

    /// Returns the paint of the text; black when none was set.
    public final Paint getTextFill() {
        Paint p = textFill.get();
        return p == null ? Color.BLACK : p;
    }

    /// Sets the paint of the text. Only a plain colour is shown.
    public final void setTextFill(Paint value) {
        textFill.set(value);
    }

    /// The paint of the text.
    public final ObjectProperty<Paint> textFillProperty() {
        return textFill;
    }

    /// Returns where the content sits in the control.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where the content sits in the control. The horizontal part
    /// is applied.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where the content sits in the control.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    /// Returns how lines of text align with each other.
    public final TextAlignment getTextAlignment() {
        return textAlignment.get();
    }

    /// Sets how lines of text align with each other.
    public final void setTextAlignment(TextAlignment value) {
        textAlignment.set(value);
    }

    /// How lines of text align with each other.
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        return textAlignment;
    }

    /// Returns whether text is asked to wrap.
    public final boolean isWrapText() {
        return wrapText.get();
    }

    /// Asks for text to wrap.
    public final void setWrapText(boolean value) {
        wrapText.set(value);
    }

    /// Whether text is asked to wrap.
    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    /// Returns whether the text is underlined.
    public final boolean isUnderline() {
        return underline.get();
    }

    /// Sets whether the text is underlined.
    public final void setUnderline(boolean value) {
        underline.set(value);
    }

    /// Whether the text is underlined.
    public final BooleanProperty underlineProperty() {
        return underline;
    }

    /// Returns the graphic, or `null`.
    public final Node getGraphic() {
        return graphic.get();
    }

    /// Sets the graphic.
    public final void setGraphic(Node value) {
        graphic.set(value);
    }

    /// The graphic of the control.
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /// Returns the gap between graphic and text.
    public final double getGraphicTextGap() {
        return graphicTextGap.get();
    }

    /// Sets the gap between graphic and text.
    public final void setGraphicTextGap(double value) {
        graphicTextGap.set(value);
    }

    /// The gap between graphic and text.
    public final DoubleProperty graphicTextGapProperty() {
        return graphicTextGap;
    }

    /// Returns where the graphic sits relative to the text.
    public final ContentDisplay getContentDisplay() {
        return contentDisplay.get();
    }

    /// Sets where the graphic sits relative to the text.
    public final void setContentDisplay(ContentDisplay value) {
        contentDisplay.set(value);
    }

    /// Where the graphic sits relative to the text.
    public final ObjectProperty<ContentDisplay> contentDisplayProperty() {
        return contentDisplay;
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-text-fill".equals(property)) {
            return textFill.get();
        } else if ("-fx-font".equals(property) || "-fx-font-size".equals(property)
                || "-fx-font-family".equals(property) || "-fx-font-weight".equals(property)
                || "-fx-font-style".equals(property)) {
            return font.get();
        } else if ("-fx-alignment".equals(property)) {
            return getAlignment();
        } else if ("-fx-text-alignment".equals(property)) {
            return getTextAlignment();
        } else if ("-fx-wrap-text".equals(property)) {
            return Boolean.valueOf(isWrapText());
        } else if ("-fx-underline".equals(property)) {
            return Boolean.valueOf(isUnderline());
        } else if ("-fx-graphic-text-gap".equals(property)) {
            return Double.valueOf(getGraphicTextGap());
        } else if ("-fx-content-display".equals(property)) {
            return getContentDisplay();
        }
        return super.cn1StyleValue(property);
    }

    private static FontWeight weight(Object value) {
        if (value instanceof FontWeight) {
            return (FontWeight) value;
        } else if (value instanceof Number) {
            return FontWeight.findByWeight(((Number) value).intValue());
        } else if (value instanceof String) {
            return FontWeight.findByName(((String) value).trim());
        }
        return null;
    }

    private static FontPosture posture(Object value) {
        if (value instanceof FontPosture) {
            return (FontPosture) value;
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            if ("italic".equalsIgnoreCase(s) || "oblique".equalsIgnoreCase(s)) {
                return FontPosture.ITALIC;
            } else if ("normal".equalsIgnoreCase(s) || "regular".equalsIgnoreCase(s)) {
                return FontPosture.REGULAR;
            }
        }
        return null;
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-text-fill".equals(property)) {
            if (value != null && !(value instanceof Paint)) {
                return false;
            }
            setTextFill((Paint) value);
        } else if (property.startsWith("-fx-font")) {
            // Restoring any of the font names restores the whole font.
            if (value == null || value instanceof Font) {
                setFont((Font) value);
                return true;
            }
            Font base = getFont();
            FontWeight w = base.cn1Weight();
            FontPosture p = base.cn1Posture();
            String family = base.getFamily();
            double size = base.getSize();
            if ("-fx-font-size".equals(property) && value instanceof Number) {
                size = ((Number) value).doubleValue();
            } else if ("-fx-font-family".equals(property) && value instanceof String) {
                family = (String) value;
            } else if ("-fx-font-weight".equals(property) && weight(value) != null) {
                w = weight(value);
            } else if ("-fx-font-style".equals(property) && posture(value) != null) {
                p = posture(value);
            } else {
                return false;
            }
            setFont(Font.font(family, w, p, size));
        } else if ("-fx-alignment".equals(property)) {
            if (!(value instanceof Pos)) {
                return false;
            }
            setAlignment((Pos) value);
        } else if ("-fx-text-alignment".equals(property)) {
            TextAlignment a = null;
            if (value instanceof TextAlignment) {
                a = (TextAlignment) value;
            } else if (value instanceof String) {
                TextAlignment[] all = TextAlignment.values();
                for (int i = 0; i < all.length; i++) {
                    if (all[i].name().equalsIgnoreCase(((String) value).trim())) {
                        a = all[i];
                    }
                }
            }
            if (a == null) {
                return false;
            }
            setTextAlignment(a);
        } else if ("-fx-wrap-text".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            setWrapText(((Boolean) value).booleanValue());
        } else if ("-fx-underline".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            setUnderline(((Boolean) value).booleanValue());
        } else if ("-fx-graphic-text-gap".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setGraphicTextGap(((Number) value).doubleValue());
        } else if ("-fx-content-display".equals(property)) {
            ContentDisplay d = null;
            if (value instanceof ContentDisplay) {
                d = (ContentDisplay) value;
            } else if (value instanceof String) {
                String wanted = ((String) value).trim().replace('-', '_');
                ContentDisplay[] all = ContentDisplay.values();
                for (int i = 0; i < all.length; i++) {
                    if (all[i].name().equalsIgnoreCase(wanted)) {
                        d = all[i];
                    }
                }
            }
            if (d == null) {
                return false;
            }
            setContentDisplay(d);
        } else {
            return super.cn1SetStyleValue(property, value);
        }
        return true;
    }
}
