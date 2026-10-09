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
import com.codename1.fxcompat.runtime.Mnemonics;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
/// leaves the theme's own in place. A plain native label, as a `Label`
/// and a cell have, is made transparent and borderless whatever the
/// theme gives it: in JavaFX a label has no background but the one of
/// its region.
///
/// The graphic node is a child of the control, above the native label,
/// placed beside the text where `contentDisplay` says and
/// `graphicTextGap` away from it; the label's padding makes the room, so
/// the preferred size covers both. A text too long for the control is cut
/// short and ends in three points, and the minimum width is the width of
/// those. `wrapText`, `ellipsisString`, `textOverrun`, `lineSpacing` are
/// recorded or absent and have no effect on a single line native label.
///
/// With `mnemonicParsing` on, as it is for every button and off for a
/// label, the underscore that marks a mnemonic is taken out of the text
/// shown, as `com.codename1.fxcompat.runtime.Mnemonics` describes; the
/// mnemonic itself does nothing, there being no key to hold for it.
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

    private final BooleanProperty mnemonicParsing = new FxBoolean(this, "mnemonicParsing", false, TEXT);
    private Node graphicChild;
    private boolean paddedForGraphic;

    {
        ChangeListener<Object> shown = new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                syncGraphicChild();
            }
        };
        graphic.addListener(shown);
        contentDisplay.addListener(shown);
    }

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
        String t = isMnemonicParsing() ? Mnemonics.strip(getText()) : getText();
        label.setText(t == null || getContentDisplay() == ContentDisplay.GRAPHIC_ONLY ? "" : t);
        Style style = com.codename1.fxcompat.runtime.PeerPaint.allStyles(label);
        if (c.getClass() == com.codename1.ui.Label.class) {
            style.setBgTransparency(0);
            style.setBorder(com.codename1.ui.plaf.Border.createEmpty());
        }
        // A JavaFX label is its text and nothing around it; the room a
        // Codename One theme gives a label made every one a few pixels
        // taller and wider than the application laid it out for.
        boolean bare = cn1BareText();
        if (bare) {
            style.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
            style.setPadding(0, 0, 0, 0);
            style.setMarginUnit(Style.UNIT_TYPE_PIXELS);
            style.setMargin(0, 0, 0, 0);
        }
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
        Node g = shownGraphic();
        if (g != null || paddedForGraphic) {
            // The graphic is a node above the native label, which knows
            // nothing of it: the label's padding grows by the room the
            // graphic takes, so the text is drawn beside it and the
            // preferred size of the label covers both.
            Style base = UIManager.getInstance().getComponentStyle(label.getUIID());
            int[] room = g == null ? new int[GRAPHIC_BOX] : graphicBox(label, g);
            style.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
            if (bare) {
                style.setPadding(room[0], room[1], room[2], room[3]);
            } else {
                style.setPadding(base.getPaddingTop() + room[0], base.getPaddingBottom() + room[1],
                        base.getPaddingLeftNoRTL() + room[2], base.getPaddingRightNoRTL() + room[3]);
            }
            paddedForGraphic = g != null;
        }
    }

    /// Whether the native component is text alone, with none of the
    /// padding or margin a theme gives one. True of a label.
    boolean cn1BareText() {
        return false;
    }

    private static final int GRAPHIC_BOX = 9;

    /// Whether a subclass places the graphic itself, as a cell does; this
    /// class then leaves the graphic and the padding of the label alone.
    boolean ownsGraphic() {
        return false;
    }

    /// Returns the graphic this class shows beside the text, or `null`.
    private Node shownGraphic() {
        Node g = getGraphic();
        return g == null || ownsGraphic() || getContentDisplay() == ContentDisplay.TEXT_ONLY ? null : g;
    }

    /// Keeps the shown graphic a child of this control, so that it has a
    /// peer above the native label.
    private void syncGraphicChild() {
        Node g = shownGraphic();
        if (g == graphicChild) {
            return;
        }
        if (graphicChild != null) {
            cn1Children().remove(graphicChild);
        }
        graphicChild = g;
        if (g != null && !cn1Children().contains(g)) {
            cn1Children().add(g);
        }
    }

    /// Measures a graphic against the text of the native label, in device
    /// pixels: the room the graphic needs at the `{top, bottom, left,
    /// right}` of the text, then the graphic's `{width, height}`, the
    /// text's `{width, height}` and the gap between the two.
    private int[] graphicBox(com.codename1.ui.Label label, Node g) {
        int gw = Units.sizeToPixels(g.isResizable() ? g.prefWidth(-1) : g.getLayoutBounds().getWidth());
        int gh = Units.sizeToPixels(g.isResizable() ? g.prefHeight(-1) : g.getLayoutBounds().getHeight());
        com.codename1.ui.Font f = label.getUnselectedStyle().getFont();
        String t = label.getText();
        boolean hasText = t != null && t.length() > 0 && f != null;
        int tw = hasText ? f.stringWidth(t) : 0;
        int th = hasText ? f.getHeight() : 0;
        int gap = hasText ? Units.toPixels(getGraphicTextGap()) : 0;
        ContentDisplay d = hasText ? getContentDisplay() : ContentDisplay.CENTER;
        int[] box = new int[GRAPHIC_BOX];
        if (d == ContentDisplay.TOP || d == ContentDisplay.BOTTOM) {
            box[d == ContentDisplay.TOP ? 0 : 1] = gh + gap;
        } else {
            int taller = Math.max(0, gh - th);
            box[0] = taller / 2;
            box[1] = taller - box[0];
        }
        if (d == ContentDisplay.RIGHT) {
            box[3] = gw + gap;
        } else if (d == ContentDisplay.TOP || d == ContentDisplay.BOTTOM || d == ContentDisplay.CENTER) {
            int wider = Math.max(0, gw - tw);
            box[2] = wider / 2;
            box[3] = wider - box[2];
        } else {
            box[2] = gw + gap;
        }
        box[4] = gw;
        box[5] = gh;
        box[6] = tw;
        box[7] = th;
        box[8] = gap;
        return box;
    }

    /// A native label with no text and no icon asks for no room at all,
    /// its padding included; with a graphic to show it is as large as the
    /// padding, which holds the room of the graphic.
    @Override
    protected Dimension cn1NativePreferredSize() {
        Dimension d = super.cn1NativePreferredSize();
        Component c = cn1NativeIfCreated();
        if (paddedForGraphic && c != null) {
            Style s = c.getUnselectedStyle();
            return new Dimension(Math.max(d.getWidth(), s.getHorizontalPadding()),
                    Math.max(d.getHeight(), s.getVerticalPadding()));
        }
        return d;
    }

    /// Places the graphic beside the text the native label draws, where
    /// the content display puts it.
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        Node g = shownGraphic();
        Component c = cn1NativeIfCreated();
        if (g == null || g.getParent() != this || !(c instanceof com.codename1.ui.Label)) {
            return;
        }
        com.codename1.ui.Label label = (com.codename1.ui.Label) c;
        int[] box = graphicBox(label, g);
        Style base = UIManager.getInstance().getComponentStyle(label.getUIID());
        Insets in = getInsets();
        // The area the label lays its content out in, less the room made
        // for the graphic: where graphic and text sit together.
        int areaX = Units.toPixels(in.getLeft()) + base.getPaddingLeftNoRTL();
        int areaY = Units.toPixels(in.getTop()) + base.getPaddingTop();
        int areaW = Units.toPixels(getWidth() - in.getLeft() - in.getRight()) - base.getPaddingLeftNoRTL()
                - base.getPaddingRightNoRTL();
        int areaH = Units.toPixels(getHeight() - in.getTop() - in.getBottom()) - base.getPaddingTop()
                - base.getPaddingBottom();
        int gw = box[4];
        int gh = box[5];
        int gap = box[8];
        ContentDisplay d = box[6] == 0 ? ContentDisplay.CENTER : getContentDisplay();
        boolean beside = d == ContentDisplay.LEFT || d == ContentDisplay.RIGHT;
        // A text too long for the control is cut short by the label.
        int tw = Math.max(0, Math.min(box[6], areaW - (beside ? gw + gap : 0)));
        int contentW = beside ? gw + gap + tw : Math.max(gw, tw);
        Pos pos = getAlignment();
        HPos h = pos == null ? HPos.LEFT : pos.getHpos();
        int contentX = areaX;
        if (h == HPos.CENTER) {
            contentX = areaX + (areaW - contentW) / 2;
        } else if (h == HPos.RIGHT) {
            contentX = areaX + areaW - contentW;
        }
        int gx = contentX + (contentW - gw) / 2;
        int gy = areaY + (areaH - gh) / 2;
        if (d == ContentDisplay.LEFT) {
            gx = contentX;
        } else if (d == ContentDisplay.RIGHT) {
            gx = contentX + tw + gap;
        } else if (d == ContentDisplay.TOP || d == ContentDisplay.BOTTOM) {
            int contentY = areaY + (areaH - gh - gap - box[7]) / 2;
            gy = d == ContentDisplay.TOP ? contentY : contentY + box[7] + gap;
        }
        double w = Units.toLogical(gw);
        double height = Units.toLogical(gh);
        if (g.isResizable()) {
            g.resize(w, height);
        }
        positionInArea(g, Units.toLogical(gx), Units.toLogical(gy), w, height, 0, HPos.CENTER, VPos.CENTER);
    }

    /// A labeled control can be as narrow as its ellipsis: the native
    /// label cuts a text that does not fit short and ends it with three
    /// points, so a row that is too narrow shrinks its labels rather than
    /// push the controls after them out of view.
    @Override
    protected double computeMinWidth(double height) {
        double pref = computePrefWidth(height);
        Component c = cn1NativeIfCreated();
        if (c == null || cn1BareText()) {
            // Asked before there is a native label, which is when a pane
            // first sizes its children, or of a plain label, which is its
            // text and nothing else: the same sum from the font of the
            // control, so the answer does not change when the native label
            // appears. Answering the whole text here made a label given a
            // preferred width narrower than its text as wide as the text,
            // and a tile pane of such labels a column short.
            String text = getText();
            if (text == null || text.length() == 0) {
                return pref;
            }
            double room = Fonts.width(getFont(), text) - Fonts.width(getFont(), ELLIPSIS);
            return room > 0 ? Math.max(0, pref - room) : pref;
        }
        if (!(c instanceof com.codename1.ui.Label)) {
            return pref;
        }
        com.codename1.ui.Label label = (com.codename1.ui.Label) c;
        com.codename1.ui.Font f = label.getUnselectedStyle().getFont();
        String t = label.getText();
        if (f == null || t == null || t.length() == 0 || !label.isEndsWith3Points()) {
            return pref;
        }
        int spare = f.stringWidth(t) - f.stringWidth(ELLIPSIS);
        return spare > 0 ? Math.max(0, pref - Units.toLogical(spare)) : pref;
    }

    private static final String ELLIPSIS = "...";

    /// Returns whether an underscore in the text marks a mnemonic.
    public final boolean isMnemonicParsing() {
        return mnemonicParsing.get();
    }

    /// Sets whether an underscore in the text marks a mnemonic and is
    /// left out of what is shown.
    public final void setMnemonicParsing(boolean value) {
        mnemonicParsing.set(value);
    }

    /// Whether an underscore in the text marks a mnemonic.
    public final BooleanProperty mnemonicParsingProperty() {
        return mnemonicParsing;
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
