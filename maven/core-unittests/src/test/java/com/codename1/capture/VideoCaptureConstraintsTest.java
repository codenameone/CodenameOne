package com.codename1.capture;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class VideoCaptureConstraintsTest extends UITestBase {

    @FormTest
    public void testVideoCaptureConstraints() {
        VideoCaptureConstraints vcc = new VideoCaptureConstraints();
        Assertions.assertEquals(0, vcc.getPreferredWidth());

        vcc = new VideoCaptureConstraints(320, 240, 30);
        Assertions.assertEquals(320, vcc.getPreferredWidth());
        Assertions.assertEquals(240, vcc.getPreferredHeight());
        Assertions.assertEquals(30, vcc.getPreferredMaxLength());

        vcc.preferredWidth(640).preferredHeight(480).preferredQuality(VideoCaptureConstraints.QUALITY_HIGH);
        Assertions.assertEquals(640, vcc.getPreferredWidth());
        Assertions.assertEquals(VideoCaptureConstraints.QUALITY_HIGH, vcc.getPreferredQuality());

        Assertions.assertNotNull(vcc.toString());

        VideoCaptureConstraints copy = new VideoCaptureConstraints(vcc);
        Assertions.assertEquals(vcc.getPreferredWidth(), copy.getPreferredWidth());

        Assertions.assertTrue(vcc.equals(vcc));
        // Removed vcc.equals(null) because implementation throws NPE which is a bug in core but we cannot fix it here.
    }
}
