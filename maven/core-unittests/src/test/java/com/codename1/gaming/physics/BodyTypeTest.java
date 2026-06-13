package com.codename1.gaming.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for the {@link BodyType} enum.
class BodyTypeTest {

    @Test
    void hasThreeKinds() {
        assertEquals(3, BodyType.values().length);
        assertNotNull(BodyType.STATIC);
        assertNotNull(BodyType.KINEMATIC);
        assertNotNull(BodyType.DYNAMIC);
    }

    @Test
    void valueOfRoundTrips() {
        assertSame(BodyType.DYNAMIC, BodyType.valueOf("DYNAMIC"));
        assertSame(BodyType.STATIC, BodyType.valueOf("STATIC"));
        assertSame(BodyType.KINEMATIC, BodyType.valueOf("KINEMATIC"));
    }
}
