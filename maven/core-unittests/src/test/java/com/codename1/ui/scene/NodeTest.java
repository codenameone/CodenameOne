package com.codename1.ui.scene;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Assertions;

public class NodeTest extends UITestBase {

    @FormTest
    public void testNodeProperties() {
        Node node = new Node();
        Assertions.assertEquals(1.0, node.scaleX.get(), 0.001);
        Assertions.assertEquals(0.0, node.translateX.get(), 0.001);

        node.translateX.set(10.0);
        Assertions.assertEquals(10.0, node.translateX.get(), 0.001);

        node.addTags("tag1", "tag2");
        Assertions.assertTrue(node.hasTag("tag1"));

        node.removeTags("tag1");
        Assertions.assertFalse(node.hasTag("tag1"));
        Assertions.assertTrue(node.hasTag("tag2"));
    }

    @FormTest
    public void testRenderableContainerCoverage() throws Exception {
        // Access private inner class RenderableContainer to improve coverage as requested
        Class<?>[] declaredClasses = Node.class.getDeclaredClasses();
        for (Class<?> cls : declaredClasses) {
            if (cls.getSimpleName().equals("RenderableContainer")) {
                java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object instance = ctor.newInstance();

                // Invoke render method
                java.lang.reflect.Method render = cls.getDeclaredMethod("render", Graphics.class);
                render.setAccessible(true);

                Image img = Image.createImage(100, 100, 0);
                render.invoke(instance, img.getGraphics());

                // Also verify it is a Container
                Assertions.assertTrue(instance instanceof com.codename1.ui.Container);
            }
        }
    }
}
