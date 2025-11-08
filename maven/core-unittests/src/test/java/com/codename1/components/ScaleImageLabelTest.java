package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.*;

class ScaleImageLabelTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsDefaults() {
        ScaleImageLabel label = new ScaleImageLabel();
        assertEquals("Label", label.getUIID());
        assertTrue(label.isShowEvenIfBlank());
        assertEquals(Style.BACKGROUND_IMAGE_SCALED_FIT, label.getBackgroundType());
    }

    @FormTest
    void testConstructorWithImageSetsIcon() {
        Image image = Image.createImage(50, 50, 0xFF0000);
        ScaleImageLabel label = new ScaleImageLabel(image);
        assertSame(image, label.getIcon());
        assertEquals("Label", label.getUIID());
    }

    @FormTest
    void testBackgroundTypeGetterAndSetter() {
        ScaleImageLabel label = new ScaleImageLabel();
        assertEquals(Style.BACKGROUND_IMAGE_SCALED_FIT, label.getBackgroundType());

        label.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        assertEquals(Style.BACKGROUND_IMAGE_SCALED_FILL, label.getBackgroundType());

        label.setBackgroundType(Style.BACKGROUND_IMAGE_SCALE);
        assertEquals(Style.BACKGROUND_IMAGE_SCALE, label.getBackgroundType());
    }

    @FormTest
    void testPreferredSizeMatchesImage() {
        Image image = Image.createImage(100, 80, 0x00FF00);
        ScaleImageLabel label = new ScaleImageLabel(image);
        Dimension pref = label.getPreferredSize();

        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void testPreferredSizeWithoutImage() {
        ScaleImageLabel label = new ScaleImageLabel();
        Dimension pref = label.getPreferredSize();

        assertTrue(pref.getWidth() >= 0);
        assertTrue(pref.getHeight() >= 0);
    }

    @FormTest
    void testSetIconUpdatesImage() {
        ScaleImageLabel label = new ScaleImageLabel();
        Image image = Image.createImage(60, 60, 0x0000FF);

        label.setIcon(image);
        assertSame(image, label.getIcon());
    }

    @FormTest
    void testSetPreferredW() {
        ScaleImageLabel label = new ScaleImageLabel();
        label.setPreferredW(200);
        assertEquals(200, label.getPreferredW());
    }

    @FormTest
    void testSetPreferredH() {
        ScaleImageLabel label = new ScaleImageLabel();
        label.setPreferredH(150);
        assertEquals(150, label.getPreferredH());
    }

    @FormTest
    void testShowEvenIfBlankIsTrue() {
        ScaleImageLabel label = new ScaleImageLabel();
        assertTrue(label.isShowEvenIfBlank());
    }
}
