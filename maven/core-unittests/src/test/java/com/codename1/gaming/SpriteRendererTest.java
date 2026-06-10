package com.codename1.gaming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for the device-independent parts of {@link SpriteRenderer}: its scene,
/// camera, light, 3D model list and clear color. The actual GPU drawing path needs a
/// device and is exercised by the on-device screenshot tests instead.
class SpriteRendererTest {

    @Test
    void freshRendererHasEmptyScene() {
        SpriteRenderer r = new SpriteRenderer();
        assertNotNull(r.getScene());
        assertEquals(0, r.getScene().size());
        assertNotNull(r.getCamera());
        assertNotNull(r.getLight());
        assertEquals(0, r.getModelCount());
    }

    @Test
    void rendersTheGivenScene() {
        Scene scene = new Scene();
        scene.add(new Sprite());
        SpriteRenderer r = new SpriteRenderer(scene);
        assertSame(scene, r.getScene());
        assertEquals(1, r.getScene().size());
    }

    @Test
    void modelListAddRemove() {
        SpriteRenderer r = new SpriteRenderer();
        Model a = new Model(null);
        Model b = new Model(null);
        r.addModel(a);
        r.addModel(b);
        assertEquals(2, r.getModelCount());
        r.removeModel(a);
        assertEquals(1, r.getModelCount());
    }

    @Test
    void clearColor() {
        SpriteRenderer r = new SpriteRenderer();
        r.setClearColor(0xff123456);
        assertEquals(0xff123456, r.getClearColor());
    }
}
