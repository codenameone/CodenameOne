package com.codename1.ui.plaf;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.plaf.StyleParser.BorderInfo;
import com.codename1.ui.plaf.StyleParser.FontInfo;
import com.codename1.ui.plaf.StyleParser.ImageInfo;
import com.codename1.ui.plaf.StyleParser.MarginInfo;
import com.codename1.ui.plaf.StyleParser.PaddingInfo;
import com.codename1.ui.plaf.StyleParser.ScalarValue;
import com.codename1.ui.plaf.StyleParser.StyleInfo;
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
        assertEquals("1px solid ff0000", copied.getBorder().toString());

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
    void testRoundBorderShadowSpreadAndPaintingCaches() {
        RoundBorder border = RoundBorder.create().shadowSpread(3).shadowBlur(4f).shadowOpacity(128).uiid(false);
        com.codename1.ui.Label label = new com.codename1.ui.Label();
        label.setWidth(20);
        label.setHeight(20);
        label.setX(0);
        label.setY(0);
        border.paintBorderBackground(graphics, label);
        RoundBorder.CacheValue cacheValue = null;
        Object baseCache = label.getClientProperty("cn1$$-rbcache");
        if (baseCache instanceof RoundBorder.CacheValue) {
            cacheValue = (RoundBorder.CacheValue) baseCache;
        }
        for (int i = 0; cacheValue == null && i < 50; i++) {
            Object cached = label.getClientProperty("cn1$$-rbcache" + (i + 1));
            if (cached instanceof RoundBorder.CacheValue) {
                cacheValue = (RoundBorder.CacheValue) cached;
                break;
            }
        }
        assertNotNull(cacheValue);
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
