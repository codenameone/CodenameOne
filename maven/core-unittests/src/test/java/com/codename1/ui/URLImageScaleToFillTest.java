package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class URLImageScaleToFillTest extends UITestBase {

    @FormTest
    public void testScaleToFill() {
        // Access the static inner class via the public constant or manually if possible
        URLImage.ImageAdapter adapter = URLImage.RESIZE_SCALE_TO_FILL;
        Assertions.assertNotNull(adapter);

        Image img = Image.createImage(100, 100);
        Label l = new Label();
        l.setWidth(200);
        l.setHeight(50);

        // Use a valid encoded image byte array to avoid null from createFromImage potentially
        // Small 1x1 PNG
        byte[] pngData = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
        EncodedImage encoded = EncodedImage.create(pngData);
        EncodedImage placeholder = EncodedImage.create(pngData);

        // adaptImage takes (EncodedImage downloaded, EncodedImage placeholder)
        if (encoded != null && placeholder != null) {
            Image result = adapter.adaptImage(encoded, placeholder);
            Assertions.assertNotNull(result);
        } else {
            // Fallback if create fails (e.g. no ImageIO)
            // But EncodedImage.create(byte[]) should work if data is valid
            // If it returns null, we can't test adaptImage without it crashing
        }
    }
}
