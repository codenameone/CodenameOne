package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.share.ShareService;

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
    void testImageToShareWithBothParameters() {
        ShareButton button = new ShareButton();
        assertNull(button.getImagePathToShare());

        button.setImageToShare("/path/to/image.png", "image/png");
        assertEquals("/path/to/image.png", button.getImagePathToShare());
    }

    @FormTest
    void testAddShareService() {
        ShareButton button = new ShareButton();
        ShareService customService = new ShareService() {
            @Override
            public String getShareTitle() { return "Custom"; }

            @Override
            public void share(String toShare) {}

            @Override
            public boolean canShareImage() { return false; }

            @Override
            public void share(String imageFilePath, String mimeType) {}
        };

        button.addShareService(customService);
        // Should not throw exception
        assertNotNull(button);
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
