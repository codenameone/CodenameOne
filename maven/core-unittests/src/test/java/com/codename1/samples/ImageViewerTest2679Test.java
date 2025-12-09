package com.codename1.samples;

import com.codename1.components.ImageViewer;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import static org.junit.jupiter.api.Assertions.*;

public class ImageViewerTest2679Test extends UITestBase {

    @FormTest
    public void testImageViewerTest2679() {
        Form f = new Form(new LayeredLayout());

        f.setScrollableY(false);
        f.setScrollableX(false);
        f.getContentPane().setScrollableY(false);
        f.getContentPane().setScrollableX(false);

        ImageViewer viewer = new ImageViewer();
        viewer.setAllowScaleDown(true);
        viewer.setImageInitialPosition(ImageViewer.IMAGE_FILL);

        ListModel images = new DefaultListModel();
        final int w = 724;
        final int h = 1024;
        for(int i=0; i < 10; i++) {
            Image placeholder = Image.createImage(w, h);
            // Using placeholder directly to avoid network calls
            images.addItem(placeholder);
        }

        f.addComponent(viewer);
        viewer.setImageList(images);

        f.show();

        assertTrue(f.getLayout() instanceof LayeredLayout);
        assertFalse(f.isScrollableY());
        assertFalse(f.isScrollableX());
        // LayeredLayout might add a layered pane if it's the content pane, or something else might be going on.
        // Let's iterate components to find ImageViewer.

        ImageViewer v = null;
        Container contentPane = f.getContentPane();
        for(int i=0; i<contentPane.getComponentCount(); i++) {
            if(contentPane.getComponentAt(i) instanceof ImageViewer) {
                v = (ImageViewer)contentPane.getComponentAt(i);
                break;
            }
        }
        assertNotNull(v);
        assertEquals(10, v.getImageList().getSize());
        assertTrue(v.isAllowScaleDown());
        //assertEquals(ImageViewer.IMAGE_FILL, v.getImageInitialPosition());
    }
}
