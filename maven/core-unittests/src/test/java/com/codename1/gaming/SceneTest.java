package com.codename1.gaming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link Scene}: membership, the camera offset, z-order sorting and
/// per-frame {@code onUpdate} propagation.
class SceneTest {

    /// A sprite that counts and remembers the delta from its last onUpdate call.
    private static class CountingSprite extends Sprite {
        int updates;
        double lastDt;
        @Override
        protected void onUpdate(double deltaSeconds) {
            updates++;
            lastDt = deltaSeconds;
        }
    }

    @Test
    void addRemoveClearSize() {
        Scene scene = new Scene();
        assertEquals(0, scene.size());
        Sprite a = new Sprite();
        Sprite b = new Sprite();
        scene.add(a);
        scene.add(b);
        assertEquals(2, scene.size());
        assertSame(a, scene.get(0));
        assertSame(b, scene.get(1));
        scene.remove(a);
        assertEquals(1, scene.size());
        assertSame(b, scene.get(0));
        scene.clear();
        assertEquals(0, scene.size());
    }

    @Test
    void cameraOffset() {
        Scene scene = new Scene();
        assertEquals(0, scene.getCameraX());
        assertEquals(0, scene.getCameraY());
        scene.setCamera(120, -30);
        assertEquals(120, scene.getCameraX());
        assertEquals(-30, scene.getCameraY());
    }

    @Test
    void updatePropagatesToSprites() {
        Scene scene = new Scene();
        CountingSprite a = new CountingSprite();
        CountingSprite b = new CountingSprite();
        scene.add(a);
        scene.add(b);
        scene.update(0.25);
        assertEquals(1, a.updates);
        assertEquals(1, b.updates);
        assertEquals(0.25, a.lastDt, 0.001);
        scene.update(0.1);
        assertEquals(2, a.updates);
        assertEquals(0.1, b.lastDt, 0.001);
    }

    @Test
    void zOrderSorting() {
        Scene scene = new Scene();
        Sprite mid = new Sprite();
        mid.setZOrder(3);
        Sprite low = new Sprite();
        low.setZOrder(1);
        Sprite high = new Sprite();
        high.setZOrder(5);
        scene.add(mid);
        scene.add(low);
        scene.add(high);
        scene.ensureSorted();
        assertSame(low, scene.get(0));    // lowest z first (drawn first/behind)
        assertSame(mid, scene.get(1));
        assertSame(high, scene.get(2));
    }

    @Test
    void markSortDirtyReSortsAfterZChange() {
        Scene scene = new Scene();
        Sprite a = new Sprite();
        a.setZOrder(1);
        Sprite b = new Sprite();
        b.setZOrder(2);
        scene.add(a);
        scene.add(b);
        scene.ensureSorted();
        assertSame(a, scene.get(0));
        // raise a above b and request a re-sort
        a.setZOrder(9);
        scene.markSortDirty();
        scene.ensureSorted();
        assertSame(b, scene.get(0));
        assertSame(a, scene.get(1));
    }
}
