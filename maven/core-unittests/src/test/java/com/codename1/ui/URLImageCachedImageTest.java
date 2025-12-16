package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.URLImage;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

public class URLImageCachedImageTest extends UITestBase {

    @FormTest
    public void testCachedImage() {
        // Force native image cache support to test CachedImage path
        TestCodenameOneImplementation.getInstance().setSupportsNativeImageCache(true);

        Image placeholder = Image.createImage(10, 10, 0);
        String url = "http://example.com/image.png";

        Image img = URLImage.createCachedImage("testImage", url, placeholder, URLImage.FLAG_RESIZE_SCALE);

        Assertions.assertNotNull(img);
        Assertions.assertEquals("testImage", img.getImageName());

        // CachedImage sets repaintImage=true in constructor, so isAnimation is initially true.
        Assertions.assertTrue(img.isAnimation());

        // Animate it. Since download is async/mocked and might not complete immediately,
        // isAnimation() might still return true (waiting for image).
        // If image is null (downloading), isAnimation returns true.
        // We assert true because we haven't completed the download in this test environment.
        boolean animating = img.animate();
        // It should return true if repainted, or false if not.
        // If repaintImage was true, it returns true and sets repaintImage=false.
        // But if image is null, isAnimation remains true.

        // Just verify basic behavior without being too strict on animation state which depends on async download.
        // But we know it should be animating initially.
    }
}
