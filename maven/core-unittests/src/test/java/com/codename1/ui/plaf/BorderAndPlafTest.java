package com.codename1.ui.plaf;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.plaf.StyleParser.FontInfo;
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
        assertEquals(1, border.images[4].getWidth());
        assertEquals(1, border.images[4].getHeight());
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
    void testDefaultLookAndFeelBidiAlignmentReversal() {
        Component component = new Component();
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
        assertEquals("Arial", fontInfo.getName());
        assertEquals(defaultFont.getPixelSize(), fontInfo.getSizeInPixels(baseStyle), 0.001f);
    }

    @FormTest
    void testStyleParserMarginInheritUsesBaseStyle() {
        Style baseStyle = new Style();
        baseStyle.setMargin(1, 2, 3, 4);
        String mergedMargin = StyleParser.parseMargin(baseStyle, "inherit 5px 6px inherit");
        assertEquals("1,6,3,5", mergedMargin);
    }

    @FormTest
    void testCSSBorderParsesBasicProperties() {
        CSSBorder cssBorder = new CSSBorder(null, "background-color:#123456; border-color:#abcdef; border-style:solid; border-width:2px; border-radius:3px");
        String cssString = cssBorder.toCSSString();
        assertTrue(cssString.contains("background-color:rgba(18,52,86,255)"));
        assertTrue(cssString.contains("border-color:rgba(171,205,239,255)"));
        assertTrue(cssString.contains("border-style:solid"));
        assertTrue(cssString.contains("border-width:2px"));
        assertTrue(cssString.contains("border-radius:3px"));
    }
}
