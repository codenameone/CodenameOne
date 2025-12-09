package com.codename1.samples;

import com.codename1.components.ImageViewer;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import static org.junit.jupiter.api.Assertions.*;

public class ImageViewerSample2778Test extends UITestBase {

    @FormTest
    public void testImageViewerSample2778() {
        Form hi = new Form("Hi World", new BorderLayout());
        ImageViewer viewer = new ImageViewer();
        Button getCroppedImage = new Button("Get Crop");
        getCroppedImage.addActionListener(e->{
            Label l = new Label(viewer.getCroppedImage(300, -1, 0x0));
            // Instead of showing a dialog which might block or be hard to test, we can verify the label content or behavior
            assertNotNull(l.getIcon());
        });
        Button getCroppedImageFullSize = new Button("Get Crop (Full Size)");
        getCroppedImageFullSize.addActionListener(e->{
            Label l = new Label(viewer.getCroppedImage(0x0));
            assertNotNull(l.getIcon());
        });

        // Mock image loading
        Image img = Image.createImage(100, 100, 0x0);
        viewer.setImage(img);
        hi.revalidateWithAnimationSafety();

        hi.add(BorderLayout.CENTER, viewer);
        hi.add(BorderLayout.SOUTH, FlowLayout.encloseIn(getCroppedImage, getCroppedImageFullSize));
        hi.show();

        assertEquals("Hi World", hi.getTitle());
        assertTrue(hi.getLayout() instanceof BorderLayout);

        // Find buttons and simulate click
        getCroppedImage.released();
        getCroppedImageFullSize.released();
    }
}
