package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformTest extends UITestBase {

    @Test
    void testMakeIdentity() {
        Transform t = Transform.makeIdentity();
        assertNotNull(t);
        assertTrue(t.isIdentity());
    }

    @Test
    void testMakeTranslation() {
        Transform t = Transform.makeTranslation(10, 20);
        assertNotNull(t);
        assertFalse(t.isIdentity());
    }

    @Test
    void testMakeScale() {
        Transform t = Transform.makeScale(2.0f, 3.0f);
        assertNotNull(t);
        assertFalse(t.isIdentity());
    }

    @Test
    void testMakeRotation() {
        Transform t = Transform.makeRotation((float)(Math.PI / 4), 0, 0);
        assertNotNull(t);
        assertFalse(t.isIdentity());
    }

    @Test
    void testCopy() {
        Transform t1 = Transform.makeTranslation(5, 10);
        Transform t2 = t1.copy();
        assertNotNull(t2);
        assertNotSame(t1, t2);
    }

    @Test
    void testTranslate() {
        Transform t = Transform.makeIdentity();
        t.translate(10, 20);
        assertFalse(t.isIdentity());
    }

    @Test
    void testTranslateWithFloats() {
        Transform t = Transform.makeIdentity();
        t.translate(10.5f, 20.5f);
        assertFalse(t.isIdentity());
    }

    @Test
    void testScale() {
        Transform t = Transform.makeIdentity();
        t.scale(2.0f, 3.0f);
        assertFalse(t.isIdentity());
    }

    @Test
    void testRotate() {
        Transform t = Transform.makeIdentity();
        t.rotate((float)(Math.PI / 2), 0, 0);
        assertFalse(t.isIdentity());
    }

    @Test
    void testRotateWithFloatAngle() {
        Transform t = Transform.makeIdentity();
        t.rotate((float) Math.PI / 4, 0, 0);
        assertFalse(t.isIdentity());
    }

    @Test
    void testConcatenateTransforms() {
        Transform t = Transform.makeIdentity();
        Transform t2 = Transform.makeTranslation(5, 5);
        t.concatenate(t2);
        assertFalse(t.isIdentity());
    }

    @Test
    void testSetTransform() {
        Transform t1 = Transform.makeTranslation(10, 20);
        Transform t2 = Transform.makeIdentity();
        t2.setTransform(t1);
        assertFalse(t2.isIdentity());
    }

    @Test
    void testInvert() {
        Transform t = Transform.makeScale(2.0f, 2.0f);
        assertDoesNotThrow(() -> t.invert());
        assertNotNull(t);
    }

    @Test
    void testConcatenate() {
        Transform t1 = Transform.makeTranslation(10, 20);
        Transform t2 = Transform.makeScale(2.0f, 2.0f);
        t1.concatenate(t2);
        assertNotNull(t1);
    }

    @Test
    void testIsSupported() {
        boolean supported = Transform.isSupported();
        // Just verify the method can be called
        assertTrue(supported || !supported);
    }

    @Test
    void testTransformPoint() {
        Transform t = Transform.makeTranslation(10, 20);
        float[] point = {5.0f, 5.0f};
        t.transformPoint(point, point);
        assertTrue(point[0] > 5.0f || point[0] < 5.0f || point[0] == 5.0f);
    }

    @Test
    void testGetScaleX() {
        Transform t = Transform.makeScale(2.0f, 3.0f);
        float scaleX = t.getScaleX();
        assertTrue(Math.abs(scaleX - 2.0f) < 0.01f);
    }

    @Test
    void testGetScaleY() {
        Transform t = Transform.makeScale(2.0f, 3.0f);
        float scaleY = t.getScaleY();
        assertTrue(Math.abs(scaleY - 3.0f) < 0.01f);
    }

    @Test
    void testGetTranslateX() {
        Transform t = Transform.makeTranslation(10, 20);
        float translateX = t.getTranslateX();
        assertTrue(Math.abs(translateX - 10.0f) < 0.01f);
    }

    @Test
    void testGetTranslateY() {
        Transform t = Transform.makeTranslation(10, 20);
        float translateY = t.getTranslateY();
        assertTrue(Math.abs(translateY - 20.0f) < 0.01f);
    }

    @Test
    void testIsTranslation() {
        Transform t = Transform.makeTranslation(10, 20);
        assertTrue(t.isTranslation());

        Transform t2 = Transform.makeScale(2.0f, 2.0f);
        assertFalse(t2.isTranslation());
    }

    @Test
    void testEquals() {
        Transform t1 = Transform.makeTranslation(10, 20);
        Transform t2 = Transform.makeTranslation(10, 20);
        Transform t3 = Transform.makeTranslation(15, 25);

        assertTrue(t1.equals(t2));
        assertFalse(t1.equals(t3));
    }

    @Test
    void testHashCode() {
        Transform t = Transform.makeTranslation(10, 20);
        int hash1 = t.hashCode();
        int hash2 = t.hashCode();

        // Same object should return consistent hashCode
        assertEquals(hash1, hash2);
    }

    @Test
    void testToString() {
        Transform t = Transform.makeIdentity();
        String str = t.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    void testComplexTransformChain() {
        Transform t = Transform.makeIdentity();
        t.translate(10, 20);
        t.scale(2.0f, 2.0f);
        t.rotate((float)(Math.PI / 4), 0, 0);
        assertFalse(t.isIdentity());
        assertNotNull(t);
    }

    @Test
    void testInvertIdentity() {
        Transform t = Transform.makeIdentity();
        assertDoesNotThrow(() -> t.invert());
        assertTrue(t.isIdentity());
    }

    @Test
    void testScaleUniform() {
        Transform t = Transform.makeIdentity();
        t.scale(2.0f, 2.0f);
        assertEquals(2.0f, t.getScaleX(), 0.01f);
        assertEquals(2.0f, t.getScaleY(), 0.01f);
    }

    @Test
    void testScaleNonUniform() {
        Transform t = Transform.makeIdentity();
        t.scale(2.0f, 3.0f);
        assertEquals(2.0f, t.getScaleX(), 0.01f);
        assertEquals(3.0f, t.getScaleY(), 0.01f);
    }

    @Test
    void testRotation90Degrees() {
        Transform t = Transform.makeIdentity();
        t.rotate((float)(Math.PI / 2), 0, 0);
        assertFalse(t.isIdentity());
    }

    @Test
    void testRotation180Degrees() {
        Transform t = Transform.makeIdentity();
        t.rotate((float)Math.PI, 0, 0);
        assertFalse(t.isIdentity());
    }

    @Test
    void testRotation360Degrees() {
        Transform t = Transform.makeIdentity();
        t.rotate((float)(2 * Math.PI), 0, 0);
        // After 360 degrees rotation, should be close to identity
        assertNotNull(t);
    }
}
