package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ImageViewer;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.list.DefaultListModel;

public class ImageViewerNavigationScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        Form form = createForm("ImageViewer Navigation", new BorderLayout(), "ImageViewerNavigationModes");
        form.add(BorderLayout.CENTER, createModesGrid());
        form.show();
        return true;
    }

    private Container createModesGrid() {
        Container grid = new Container(new GridLayout(3, 2));
        grid.add(createMode("Default", false, false, 6f));
        grid.add(createMode("Arrows", true, false, 6f));
        grid.add(createMode("Thumbnails", false, true, 4f));
        grid.add(createMode("Arrows + Thumbnails", true, true, 6f));
        grid.add(createMode("Arrows + Tall Thumbs", true, true, 8f));
        grid.add(createMode("Thumbnails Only (Tall)", false, true, 9f));
        return grid;
    }

    private Container createMode(String title, boolean arrows, boolean thumbnails, float thumbnailBarHeightMM) {
        Image red = Image.createImage(120, 80, 0xffcc4444);
        Image green = Image.createImage(120, 80, 0xff44cc44);
        Image blue = Image.createImage(120, 80, 0xff4444cc);

        DefaultListModel<Image> images = new DefaultListModel<>(red, green, blue);
        ImageViewer viewer = new ImageViewer(red);
        viewer.setImageList(images);
        viewer.setNavigationArrowsVisible(arrows);
        viewer.setThumbnailsVisible(thumbnails);
        viewer.setThumbnailBarHeight(thumbnailBarHeightMM);

        Container mode = new Container(new BorderLayout());
        mode.add(BorderLayout.NORTH, new Label(title));
        mode.add(BorderLayout.CENTER, viewer);
        return mode;
    }
}
