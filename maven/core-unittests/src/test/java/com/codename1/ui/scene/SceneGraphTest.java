package com.codename1.ui.scene;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle2D;

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
        assertEquals(10.0, bounds.getX(), 0.1);
        assertEquals(20.0, bounds.getY(), 0.1);
        assertEquals(200.0, bounds.getWidth(), 0.1);
        assertEquals(25.0, bounds.getHeight(), 0.1);
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
        Transform transform = camera.getTransform();
        assertNotNull(transform);
        float[] point = transform.transformPoint(new float[]{0, 0, 1});
        assertEquals(3, point.length);
    }
}
