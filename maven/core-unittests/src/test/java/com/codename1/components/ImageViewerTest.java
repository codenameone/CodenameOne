package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;

import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ImageViewerTest extends UITestBase {

    @BeforeEach
    void stubOrientation() {
        implementation.setPortrait(true);
    }

    @FormTest
    void setImageResetsZoomAndPan() throws Exception {
        Image image = Image.createImage(40, 20, 0xff0000ff);
        ImageViewer viewer = new ImageViewer();
        viewer.setAnimateZoom(false);
        viewer.setImage(image);
        assertSame(image, viewer.getImage());
        assertEquals(1f, viewer.getZoom());
        assertEquals(0.5f, getPrivateField(viewer, "panPositionX", Float.class));
        assertEquals(0.5f, getPrivateField(viewer, "panPositionY", Float.class));
    }

    @FormTest
    void setImageNoRepositionKeepsState() throws Exception {
        Image first = Image.createImage(10, 10, 0xff00ff00);
        ImageViewer viewer = new ImageViewer(first);
        viewer.setAnimateZoom(false);
        viewer.setZoom(3f, 1f, 0f);
        Image second = Image.createImage(8, 8, 0xffff0000);
        viewer.setImageNoReposition(second);
        assertSame(second, viewer.getImage());
        assertEquals(3f, viewer.getZoom());
        assertEquals(1f, getPrivateField(viewer, "panPositionX", Float.class));
        assertEquals(0f, getPrivateField(viewer, "panPositionY", Float.class));
    }

    @FormTest
    void setImageListRespondsToSelectionChanges() {
        Image first = Image.createImage(12, 12, 0xff112233);
        Image second = Image.createImage(12, 12, 0xff445566);
        DefaultListModel<Image> model = new DefaultListModel<>(first, second);
        ImageViewer viewer = new ImageViewer();
        viewer.setAnimateZoom(false);
        viewer.setImageList(model);
        assertSame(first, viewer.getImage());
        model.setSelectedIndex(1);
        flushSerialCalls();
        assertSame(second, viewer.getImage());
    }

    @FormTest
    void setZoomClampsPanPositionsWhenAnimationDisabled() throws Exception {
        ImageViewer viewer = new ImageViewer(Image.createImage(16, 16, 0xff223344));
        viewer.setAnimateZoom(false);
        viewer.setZoom(4f, 2f, -1f);
        assertEquals(4f, viewer.getZoom());
        assertEquals(1f, getPrivateField(viewer, "panPositionX", Float.class));
        assertEquals(0f, getPrivateField(viewer, "panPositionY", Float.class));
    }

    @FormTest
    void propertyAccessorsExposeConfiguration() {
        Image placeholder = Image.createImage(5, 5, 0xffabcdef);
        Image first = Image.createImage(6, 6, 0xffaabbcc);
        Image second = Image.createImage(7, 7, 0xffddeeff);
        ListModel<Image> model = new DefaultListModel<>(first, second);
        ImageViewer viewer = new ImageViewer();
        viewer.setAnimateZoom(false);
        viewer.setSwipePlaceholder(placeholder);
        viewer.setImageList(model);
        viewer.setEagerLock(false);
        viewer.setCycleLeft(false);
        viewer.setCycleRight(false);
        viewer.setSwipeThreshold(0.6f);

        assertArrayEquals(new String[]{"eagerLock", "image", "imageList", "swipePlaceholder"}, viewer.getPropertyNames());
        assertSame(placeholder, viewer.getPropertyValue("swipePlaceholder"));
        assertFalse(viewer.isEagerLock());
        assertFalse(viewer.isCycleLeft());
        assertFalse(viewer.isCycleRight());
        assertEquals(0.6f, viewer.getSwipeThreshold());
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(target);
        if (type == Float.class && value instanceof Number) {
            return (T) Float.valueOf(((Number) value).floatValue());
        }
        return (T) value;
    }
}
