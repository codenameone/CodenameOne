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
package com.codename1.ui.plaf;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.plaf.StyleParser.BorderInfo;
import com.codename1.ui.plaf.StyleParser.FontInfo;
import com.codename1.ui.plaf.StyleParser.ImageInfo;
import com.codename1.ui.plaf.StyleParser.MarginInfo;
import com.codename1.ui.plaf.StyleParser.PaddingInfo;
import com.codename1.ui.plaf.StyleParser.ScalarValue;
import com.codename1.ui.plaf.StyleParser.StyleInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class BorderAndPlafTest extends UITestBase {

    private Graphics graphics;

    @BeforeEach
    void setupGraphics() {
        Image image = Image.createImage(8, 8);
        graphics = image.getGraphics();
    }

    @FormTest
    void testEmptyBorderIsReportedAsEmpty() {
        Border border = Border.createEmpty();
        assertTrue(border.isEmptyBorder());
    }

    @FormTest
    void testImageSplicedBorderCreatesExpectedSegments() {
        Image img = Image.createImage(6, 6);
        Border border = Border.createImageSplicedBorder(img, 0.25, 0.25, 0.25, 0.25);
        assertNotNull(border.images);
        assertEquals(9, border.images.length);
        assertEquals(2, border.images[0].getWidth());
        assertEquals(2, border.images[0].getHeight());
        assertEquals(2, border.images[4].getWidth());
        assertEquals(2, border.images[4].getHeight());
    }

    @FormTest
    void testRoundBorderPropertiesAreRetained() {
        RoundBorder border = RoundBorder.create()
                .color(0x112233)
                .opacity(123)
                .strokeColor(0x445566)
                .strokeOpacity(77)
                .stroke(2f, false);
        border.shadowBlur(5f).shadowOpacity(99).rectangle(true);

        assertEquals(0x112233, border.getColor());
        assertEquals(123, border.getOpacity());
        assertEquals(0x445566, border.getStrokeColor());
        assertEquals(77, border.getStrokeOpacity());
        assertEquals(2f, border.getStrokeThickness(), 0.001f);
        assertEquals(5f, border.getShadowBlur(), 0.001f);
        assertEquals(99, border.getShadowOpacity());
        assertTrue(border.isRectangle());
        assertNotEquals(RoundBorder.create(), border);
    }

    @FormTest
    void testRoundRectBorderConfigurationAndEquality() {
        RoundRectBorder border = RoundRectBorder.create()
                .cornerRadius(4f)
                .bezierCorners(true)
                .topLeftMode(false)
                .topRightMode(true)
                .bottomLeftMode(true)
                .bottomRightMode(false);

        assertEquals(4f, border.getCornerRadius(), 0.001f);
        assertTrue(border.isBezierCorners());
        assertFalse(border.isTopLeft());
        assertTrue(border.isTopRight());
        assertTrue(border.isBottomLeft());
        assertFalse(border.isBottomRight());
        assertNotEquals(RoundRectBorder.create(), border);
    }

    @FormTest
    void testRoundRectBorderOddStrokeInsetsEvenly() {
        RoundRectBorder border = RoundRectBorder.create()
                .cornerRadius(0f)
                .stroke(3f, false);

        Label label = new Label("Stroke");
        label.setX(0);
        label.setY(0);
        label.setWidth(20);
        label.setHeight(10);
        label.getStyle().setBackgroundType(Style.BACKGROUND_NONE);
        label.getStyle().setBgTransparency(0xff);
        label.getStyle().setBgColor(0xffffff);
        label.getStyle().setBorder(border);

        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();
        Graphics testGraphics = Image.createImage(30, 20).getGraphics();
        border.paintBorderBackground(testGraphics, label);
        assertTrue(implementation.wasDrawShapeInvoked());
        float[] bounds = implementation.getLastDrawShape().getBounds2D();

        assertEquals(1.5f, bounds[0], 0.001f);
        assertEquals(1.5f, bounds[1], 0.001f);
        assertEquals(17f, bounds[2], 0.001f);
        assertEquals(7f, bounds[3], 0.001f);
    }

    @FormTest
    void testRoundRectBorderMirrorsAsymmetricCornersInRTL() {
        RoundRectBorder border = RoundRectBorder.create()
                .cornerRadius(4f)
                .topLeftMode(false)
                .bottomLeftMode(false)
                .topRightMode(true)
                .bottomRightMode(true);

        Label label = new Label();
        label.setRTL(true);
        label.setWidth(30);
        label.setHeight(20);
        label.getStyle().setBackgroundType(Style.BACKGROUND_NONE);
        label.getStyle().setBgTransparency(0xff);
        label.getStyle().setBgColor(0);
        label.getStyle().setBorder(border);

        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();
        border.paintBorderBackground(Image.createImage(30, 20).getGraphics(), label);

        assertTrue(implementation.wasFillShapeInvoked());
        PathIterator path = implementation.getLastFillShape().getPathIterator();
        float[] coordinates = new float[6];
        assertFalse(path.isDone(), "RTL shape should contain a move-to segment");
        assertEquals(PathIterator.SEG_MOVETO, path.currentSegment(coordinates));
        assertTrue(coordinates[0] > 0,
                "RTL should start after the mirrored rounded top-left corner");
        path.next();
        assertFalse(path.isDone(), "RTL shape should contain a top-edge line segment");
        assertEquals(PathIterator.SEG_LINETO, path.currentSegment(coordinates));
        assertEquals(30f, coordinates[0], 0.001f,
                "RTL should mirror the square top-left corner to the top-right");
    }

    @FormTest
    void testRoundRectBorderReservesTwiceTheRadiusByDefault() {
        RoundRectBorder border = RoundRectBorder.create().cornerRadius(3f).shadowSpread(0f);

        assertFalse(border.isCssBoxModel(), "the legacy pill sizing is the default");
        int radius = Display.getInstance().convertToPixels(3f);
        assertEquals(radius * 2, border.getMinimumHeight(),
                "hand written borders keep growing the component so the full radius is drawn");
        assertEquals(radius * 2, border.getMinimumWidth());
    }

    @FormTest
    void testCssBoxModelBorderDoesNotReserveSpaceForTheRadius() {
        RoundRectBorder border = RoundRectBorder.create().cornerRadius(3f).cssBoxModel(true);

        assertTrue(border.isCssBoxModel());
        assertEquals(0, border.getMinimumHeight(),
                "border-radius never contributes to the size of a CSS box");
        assertEquals(0, border.getMinimumWidth());
    }

    @FormTest
    void testCssBoxModelBorderStillReservesRoomForItsShadow() {
        RoundRectBorder border = RoundRectBorder.create()
                .cornerRadius(3f)
                .cssBoxModel(true)
                .shadowSpread(2f)
                .shadowOpacity(128);

        assertEquals(Display.getInstance().convertToPixels(2f), border.getMinimumHeight(),
                "a drawn shadow still needs its spread reserved, the radius does not");
    }

    /// Regression test for https://github.com/codenameone/CodenameOne/discussions/5454:
    /// `border-radius: 0 3mm 3mm 0` on a button used to inflate its height because the
    /// generated `RoundRectBorder` reserved twice the radius.
    @FormTest
    void testCssBoxModelBorderDoesNotInflateAButton() {
        Button squareCorners = new Button("Send");
        squareCorners.getAllStyles().setBorder(Border.createEmpty());
        int expected = squareCorners.getPreferredSize().getHeight();

        // A radius in millimeters that converts to more pixels than the button is tall, so
        // the legacy reservation is the value that wins in the preferred size calculation
        // and the difference between the two modes is visible here at any density.
        float pxPerMm = Display.getInstance().convertToPixels(1f);
        float radius = expected / pxPerMm + 1f;
        assertTrue(rightRoundedCorners(radius).getMinimumHeight() > expected,
                "test setup: the radius has to exceed the natural height of the button");

        Button cssRounded = new Button("Send");
        cssRounded.getAllStyles().setBorder(rightRoundedCorners(radius).cssBoxModel(true));

        Button legacyRounded = new Button("Send");
        legacyRounded.getAllStyles().setBorder(rightRoundedCorners(radius));

        assertEquals(expected, cssRounded.getPreferredSize().getHeight(),
                "rounding the corners must not change the height the stylesheet asked for");
        assertEquals(rightRoundedCorners(radius).getMinimumHeight(),
                legacyRounded.getPreferredSize().getHeight(),
                "the legacy sizing is unchanged, it still grows the button to twice the radius");
    }

    private static RoundRectBorder rightRoundedCorners(float radius) {
        return RoundRectBorder.create()
                .cornerRadius(radius)
                .shadowSpread(0f)
                .topLeftMode(false)
                .bottomLeftMode(false)
                .topRightMode(true)
                .bottomRightMode(true);
    }

    @FormTest
    void testRoundRectBorderScalesTheRadiusDownToFitTheShape() {
        // Both corners of every edge are rounded and the box is only as big as a single
        // radius, so each corner may use at most half of what the border asks for.
        int radius = Display.getInstance().convertToPixels(4f);

        List<float[]> shape = shapeOf(RoundRectBorder.create().cornerRadius(4f).cssBoxModel(true),
                radius, radius);

        // The path opens at the point where the rounded top-left corner ends.
        float[] moveTo = shape.get(0);
        assertEquals(PathIterator.SEG_MOVETO, (int) moveTo[0]);
        assertTrue(moveTo[1] > 0, "the corner is still rounded");
        assertTrue(moveTo[1] <= radius / 2f + 0.001f,
                "two rounded corners sharing an edge may use at most half of it each, was "
                        + moveTo[1] + " of " + radius);
    }

    @FormTest
    void testRoundRectBorderScalesTheRadiusPerEdgeNotPerShape() {
        // Only the top-right corner is rounded, so nothing shares the top or the right edge
        // with it and CSS lets it use a whole edge rather than half of one.
        int radius = Display.getInstance().convertToPixels(4f);
        int width = radius * 2;
        int height = radius / 2;

        RoundRectBorder border = RoundRectBorder.create()
                .cornerRadius(4f)
                .cssBoxModel(true)
                .topLeftMode(false)
                .bottomLeftMode(false)
                .bottomRightMode(false)
                .topRightMode(true);

        List<float[]> shape = shapeOf(border, width, height);

        // A square top-left corner opens the path at the origin, the line that follows runs
        // along the top edge and stops where the single rounded corner begins.
        float[] moveTo = shape.get(0);
        assertEquals(PathIterator.SEG_MOVETO, (int) moveTo[0]);
        assertEquals(0f, moveTo[1], 0.001f, "the square top-left corner starts at the origin");

        float[] topEdge = shape.get(1);
        assertEquals(PathIterator.SEG_LINETO, (int) topEdge[0]);
        assertEquals(width - height, topEdge[1], 0.001f,
                "the lone rounded corner scales to the whole height, not to half of it");
    }

    @FormTest
    void testLegacyBorderAlsoScalesTheRadiusWhenForcedSmaller() {
        // The legacy minimum size asks for room to draw the full radius, but a layout is
        // free to ignore it. The corners then scale to whatever the component actually got
        // rather than folding the path over itself, in this mode too.
        int radius = Display.getInstance().convertToPixels(4f);
        RoundRectBorder border = RoundRectBorder.create().cornerRadius(4f).shadowSpread(0f);
        assertFalse(border.isCssBoxModel(), "this is the legacy sizing");
        assertTrue(border.getMinimumHeight() > radius, "test setup: the border wants more than it gets");

        List<float[]> shape = shapeOf(border, radius, radius);

        float[] moveTo = shape.get(0);
        assertEquals(PathIterator.SEG_MOVETO, (int) moveTo[0]);
        assertTrue(moveTo[1] > 0 && moveTo[1] <= radius / 2f + 0.001f,
                "the corner scales to the component it was given, was " + moveTo[1] + " of " + radius);
    }

    /// Paints the border into a component of the given size and returns the shape it filled
    /// as `{segmentType, x, y}` rows. The tracked shape has to be walked with a single
    /// iterator, a second one over the same shape does not start from the beginning.
    private List<float[]> shapeOf(RoundRectBorder border, int width, int height) {
        Label label = new Label();
        label.setWidth(width);
        label.setHeight(height);
        label.getStyle().setBackgroundType(Style.BACKGROUND_NONE);
        label.getStyle().setBgTransparency(0xff);
        label.getStyle().setBgColor(0);
        label.getStyle().setBorder(border);

        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();
        border.paintBorderBackground(Image.createImage(width, height).getGraphics(), label);

        assertTrue(implementation.wasFillShapeInvoked());
        List<float[]> segments = new ArrayList<float[]>();
        PathIterator path = implementation.getLastFillShape().getPathIterator();
        float[] coordinates = new float[6];
        while (!path.isDone()) {
            int type = path.currentSegment(coordinates);
            segments.add(new float[]{type, coordinates[0], coordinates[1]});
            path.next();
        }
        assertFalse(segments.isEmpty(), "the border should have filled a shape");
        return segments;
    }

    @FormTest
    void testDefaultLookAndFeelBidiAlignmentReversal() {
        Component component = new com.codename1.ui.Label();
        component.setRTL(true);
        assertEquals(Component.LEFT, DefaultLookAndFeel.reverseAlignForBidi(component, Component.RIGHT));
        assertEquals(Component.RIGHT, DefaultLookAndFeel.reverseAlignForBidi(component, Component.LEFT));
        component.setRTL(false);
        assertEquals(Component.LEFT, DefaultLookAndFeel.reverseAlignForBidi(component, Component.LEFT));
    }

    @FormTest
    void testStyleParserMergesFontDefinitions() {
        Font defaultFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        Style baseStyle = new Style();
        baseStyle.setFont(defaultFont);
        String style = "font: 12px Arial; font: inherit bold";
        StyleParser.StyleInfo info = StyleParser.parseString(style);
        FontInfo fontInfo = StyleParser.parseFont(new FontInfo(), info.values.get("font"));
        assertEquals(12f, fontInfo.getSize(), 0.001f);
        assertEquals(Style.UNIT_TYPE_PIXELS, fontInfo.getSizeUnit());
        assertEquals("inherit", fontInfo.getName());
        assertEquals(12, fontInfo.getSizeInPixels(baseStyle), 0.001f);
    }

    @FormTest
    void testStyleParserMarginInheritUsesBaseStyle() {
        Style baseStyle = new Style();
        baseStyle.setMargin(1, 2, 3, 4);
        String mergedMargin = StyleParser.parseMargin(baseStyle, "inherit 5px 6px inherit");
        assertEquals("1.0,6.0,3.0,5.0", mergedMargin);
    }

    @FormTest
    void testCSSBorderParsesBasicProperties() {
        CSSBorder cssBorder = new CSSBorder(null, "background-color:#123456; border-color:#abcdef; border-style:solid; border-width:2px; border-radius:3px");
        String cssString = cssBorder.toCSSString();
        assertTrue(cssString.contains("background-color:#123456ff"));
        assertTrue(cssString.contains("border-color:#abcdefff"));
        assertTrue(cssString.contains("border-style:solid"));
        assertTrue(cssString.contains("border-width:2px"));
        assertTrue(cssString.contains("border-radius:3px"));
    }

    @FormTest
    void testStyleParserScalarValuesAndBackgroundTypes() {
        assertTrue(StyleParser.validateScalarValue("2px"));
        assertFalse(StyleParser.validateScalarValue("two"));
        ScalarValue percent = StyleParser.parseScalarValue("33%");
        assertEquals(33, percent.getValue(), 0.0001);
        assertEquals(Style.UNIT_TYPE_SCREEN_PERCENTAGE, percent.getUnit());
        percent.setUnit(Style.UNIT_TYPE_PIXELS);
        percent.setValue(4.7);
        assertEquals("5px", percent.toString());
        assertEquals("4.%", new ScalarValue(4.7, Style.UNIT_TYPE_SCREEN_PERCENTAGE).toString(1));

        List<String> sortedTypes = StyleParser.getBackgroundTypes();
        List<String> unsortedTypes = StyleParser.getSupportedBackgroundTypes();
        assertTrue(sortedTypes.contains("none"));
        assertEquals(sortedTypes.size(), unsortedTypes.size());
    }

    @FormTest
    void testStyleInfoConstructionAndMutation() {
        StyleInfo composed = new StyleInfo("padding:1px 2px 3px 4px; margin:6px; font: 10px native:Main; bgColor:ffffff; fgColor:000000");
        PaddingInfo paddingInfo = composed.getPadding();
        MarginInfo marginInfo = composed.getMargin();
        FontInfo fontInfo = composed.getFont();
        assertEquals(1, paddingInfo.getValue(Component.TOP).getValue(), 0.01);
        assertEquals(6, marginInfo.getValue(Component.TOP).getValue(), 0.01);
        assertEquals(10f, fontInfo.getSize(), 0.01f);

        StyleInfo copied = new StyleInfo(composed);
        copied.setFontSize("inherit").setFontName("native:Other").setBorder("1px solid ff0000").setBgColor(null).setMargin("2px 3px");
        FontInfo mutatedFont = StyleParser.parseFont(new FontInfo(), copied.values.get("font"));
        assertEquals(StyleParser.UNIT_INHERIT, mutatedFont.getSizeUnit());
        assertEquals("native:Other native:Other", mutatedFont.toString());
        assertEquals("native:Other", mutatedFont.getName());
        assertEquals(2, copied.getMargin().getValue(Component.TOP).getValue(), 0.01);
        assertEquals("1.0px solid ff0000", copied.getBorder().toString());

        StyleInfo empty = new StyleInfo((String[]) null);
        assertNull(empty.getFont());
    }

    @FormTest
    void testStyleParserImageAndBorderParsing() {
        ImageInfo info = new ImageInfo("/img.png");
        assertEquals("/img.png", info.toString());
        assertNull(info.getImage(null));

        BorderInfo borderInfo = StyleParser.parseBorder(new BorderInfo(), "1px solid ff00ff");
        assertEquals("1.0px solid ff00ff", borderInfo.toString());
        assertEquals(1f, borderInfo.getWidth(), 0.01f);
        assertEquals(Style.UNIT_TYPE_PIXELS, borderInfo.getWidthUnit());
        assertEquals("line", borderInfo.getType());
    }

    @FormTest
    void testRoundBorderShadowSpreadAndPaintingCaches() throws Exception {
        RoundBorder border = RoundBorder.create().shadowSpread(3).shadowBlur(4f).shadowOpacity(128).uiid(false);
        com.codename1.ui.Label label = new com.codename1.ui.Label();
        label.setWidth(20);
        label.setHeight(20);
        label.setX(0);
        label.setY(0);
        border.paintBorderBackground(graphics, label);
        // RoundBorder stores its cache under "cn1$$-rbcache" + instanceVal where
        // instanceVal is a per-instance id off a static counter. Read the actual
        // instanceVal off this border so the lookup doesn't depend on how many
        // RoundBorder instances earlier tests in the same JVM happened to mint.
        Field instanceValField = RoundBorder.class.getDeclaredField("instanceVal");
        instanceValField.setAccessible(true);
        int instanceVal = instanceValField.getInt(border);
        Object cached = label.getClientProperty("cn1$$-rbcache" + instanceVal);
        assertNotNull(cached, "RoundBorder.paintBorderBackground should populate the cache under cn1$$-rbcache" + instanceVal);
        assertTrue(cached instanceof RoundBorder.CacheValue);
        RoundBorder.CacheValue cacheValue = (RoundBorder.CacheValue) cached;
        assertEquals(label.getWidth(), cacheValue.img.getWidth());
        assertTrue(border.getMinimumHeight() > 0);
        assertTrue(border.getMinimumWidth() > 0);
    }

    @FormTest
    void testCSSBorderStrokeAndRadiusRoundTrip() {
        CSSBorder cssBorder = new CSSBorder(null, "border-stroke:2px dotted; border-radius:4px 5px 6px 7px; background-repeat:repeat-x; background-position:10% 20%");
        String css = cssBorder.toCSSString();
        assertTrue(css.contains("border-width:2px 2px 2px 2px"));
        assertTrue(css.contains("border-style:dotted dotted dotted dotted"));
        assertTrue(css.contains("border-color:#00000000 #00000000 #00000000 #00000000"));
        assertTrue(css.contains("border-radius:4px 5px 6px 7px"));
        assertTrue(css.contains("background-image:none"));
        assertTrue(css.contains("background-position:"));
    }
}
