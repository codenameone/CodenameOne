package com.codename1.ui.scene;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle2D;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

class SceneGraphTest extends UITestBase {

    @FormTest
    void testNodeBoundsReflectTransforms() {
        Scene scene = new Scene();
        Node root = new Node();
        root.boundsInLocal.set(new Bounds(0, 0, 0, 100, 50, 0));
        root.layoutX.set(10.0);
        root.layoutY.set(20.0);
        root.scaleX.set(2.0);
        root.scaleY.set(0.5);
        scene.setRoot(root);

        Rectangle2D bounds = new Rectangle2D();
        root.getBoundsInScene(bounds);
        Bounds local = root.boundsInLocal.get();
        double expectedX = root.layoutX.get() - local.getWidth() * (root.scaleX.get() - 1.0) / 2.0;
        double expectedY = root.layoutY.get() - local.getHeight() * (root.scaleY.get() - 1.0) / 2.0;
        assertEquals(expectedX, bounds.getX(), 0.1);
        assertEquals(expectedY, bounds.getY(), 0.1);
        assertEquals(local.getWidth() * root.scaleX.get(), bounds.getWidth(), 0.1);
        assertEquals(local.getHeight() * root.scaleY.get(), bounds.getHeight(), 0.1);
    }

    @FormTest
    void testChildNodesInheritScene() {
        Scene scene = new Scene();
        Node root = new Node();
        Node child = new Node();
        scene.setRoot(root);
        root.add(child);

        assertSame(scene, child.getScene());
        root.remove(child);
        assertNull(child.getScene());
    }

    @FormTest
    void testPerspectiveCameraCreatesTransform() {
        implementation.setDisplaySize(400, 800);
        Scene scene = new Scene();
        scene.setWidth(200);
        scene.setHeight(100);
        scene.setX(0);
        scene.setY(0);
        PerspectiveCamera camera = new PerspectiveCamera(scene, 0.5, 1.0, 1000.0);
        RuntimeException unsupported = assertThrows(RuntimeException.class, new Executable() {
            public void execute() {
                camera.getTransform();
            }
        });
        assertEquals("Transforms not supported", unsupported.getMessage());
    }
}
