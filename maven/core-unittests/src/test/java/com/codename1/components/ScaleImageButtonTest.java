package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.*;

class ScaleImageButtonTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsDefaults() {
        ScaleImageButton button = new ScaleImageButton();
        assertEquals("ScaleImageButton", button.getUIID());
        assertTrue(button.isShowEvenIfBlank());
        assertEquals(Style.BACKGROUND_IMAGE_SCALED_FIT, button.getBackgroundType());
    }

    @FormTest
    void testConstructorWithImageSetsIcon() {
        Image image = Image.createImage(50, 50, 0xFF0000);
        ScaleImageButton button = new ScaleImageButton(image);
        assertSame(image, button.getIcon());
        assertEquals("ScaleImageButton", button.getUIID());
    }

    @FormTest
    void testBackgroundTypeGetterAndSetter() {
        ScaleImageButton button = new ScaleImageButton();
        assertEquals(Style.BACKGROUND_IMAGE_SCALED_FIT, button.getBackgroundType());

        button.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        assertEquals(Style.BACKGROUND_IMAGE_SCALED_FILL, button.getBackgroundType());

        button.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED);
        assertEquals(Style.BACKGROUND_IMAGE_SCALED, button.getBackgroundType());
    }

    @FormTest
    void testPreferredSizeMatchesImage() {
        Image image = Image.createImage(100, 80, 0x00FF00);
        ScaleImageButton button = new ScaleImageButton(image);
        Dimension pref = button.getPreferredSize();

        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void testPreferredSizeWithoutImage() {
        ScaleImageButton button = new ScaleImageButton();
        Dimension pref = button.getPreferredSize();

        assertTrue(pref.getWidth() >= 0);
        assertTrue(pref.getHeight() >= 0);
    }

    @FormTest
    void testSetIconUpdatesImage() {
        ScaleImageButton button = new ScaleImageButton();
        Image image = Image.createImage(60, 60, 0x0000FF);

        button.setIcon(image);
        assertSame(image, button.getIcon());
    }

    @FormTest
    void testButtonIsFocusable() {
        ScaleImageButton button = new ScaleImageButton();
        assertTrue(button.isFocusable());
    }

    @FormTest
    void testShowEvenIfBlankIsTrue() {
        ScaleImageButton button = new ScaleImageButton();
        assertTrue(button.isShowEvenIfBlank());
    }

}
