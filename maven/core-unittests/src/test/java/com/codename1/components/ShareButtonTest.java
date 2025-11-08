package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class ShareButtonTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsDefaults() {
        ShareButton button = new ShareButton();
        assertEquals("ShareButton", button.getUIID());
        assertNotNull(button.getIcon());
    }

    @FormTest
    void testTextToShareGetterAndSetter() {
        ShareButton button = new ShareButton();
        assertNull(button.getTextToShare());

        button.setTextToShare("Share this text");
        assertEquals("Share this text", button.getTextToShare());
    }

    @FormTest
    void testImageToShareGetterAndSetter() {
        ShareButton button = new ShareButton();
        assertNull(button.getImageToShare());

        button.setImageToShare("/path/to/image.png");
        assertEquals("/path/to/image.png", button.getImageToShare());
    }

    @FormTest
    void testImageMimeTypeGetterAndSetter() {
        ShareButton button = new ShareButton();
        assertNull(button.getImageMimeType());

        button.setImageMimeType("image/png");
        assertEquals("image/png", button.getImageMimeType());
    }

    @FormTest
    void testShareServicesCollection() {
        ShareButton button = new ShareButton();
        assertNotNull(button.getShareServices());
        assertTrue(button.getShareServices().size() > 0);
    }

    @FormTest
    void testButtonIsFocusable() {
        ShareButton button = new ShareButton();
        assertTrue(button.isFocusable());
    }

    @FormTest
    void testIconIsSet() {
        ShareButton button = new ShareButton();
        assertNotNull(button.getIcon(), "Share button should have a default icon");
    }
}
