package com.codename1.gaming;

import com.codename1.gpu.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link Model}: transform bookkeeping and the model matrix math.
/// The mesh is irrelevant to these (it is only used at draw time), so a null mesh is
/// fine here.
class ModelTest {

    @Test
    void defaultsAreIdentity() {
        Model m = new Model(null);
        assertEquals(0f, m.getX(), 0.001);
        assertEquals(0f, m.getY(), 0.001);
        assertEquals(0f, m.getZ(), 0.001);
        assertEquals(0f, m.getRotationX(), 0.001);
        assertEquals(0f, m.getRotationY(), 0.001);
        assertEquals(0f, m.getRotationZ(), 0.001);
        assertTrue(m.isVisible());

        float[] mat = m.modelMatrix();
        assertEquals(16, mat.length);
        assertMatrixEquals(identity(), mat);
    }

    @Test
    void positionFillsTranslationColumn() {
        Model m = new Model(null).setPosition(2f, 3f, 4f);
        assertEquals(2f, m.getX(), 0.001);
        float[] mat = m.modelMatrix();
        // column-major: translation lives at indices 12,13,14
        assertEquals(2f, mat[12], 0.001);
        assertEquals(3f, mat[13], 0.001);
        assertEquals(4f, mat[14], 0.001);
        // no scaling -> diagonal still 1
        assertEquals(1f, mat[0], 0.001);
        assertEquals(1f, mat[5], 0.001);
    }

    @Test
    void uniformScaleFillsDiagonal() {
        Model m = new Model(null).setScale(2f);
        float[] mat = m.modelMatrix();
        assertEquals(2f, mat[0], 0.001);
        assertEquals(2f, mat[5], 0.001);
        assertEquals(2f, mat[10], 0.001);
        assertEquals(1f, mat[15], 0.001);
    }

    @Test
    void nonUniformScale() {
        Model m = new Model(null).setScale(2f, 3f, 4f);
        float[] mat = m.modelMatrix();
        assertEquals(2f, mat[0], 0.001);
        assertEquals(3f, mat[5], 0.001);
        assertEquals(4f, mat[10], 0.001);
    }

    @Test
    void rotationChangesTheMatrix() {
        Model spun = new Model(null).setRotation(0f, 90f, 0f);
        assertEquals(90f, spun.getRotationY(), 0.001);
        float[] mat = spun.modelMatrix();
        // a 90-degree Y rotation must move the matrix off identity
        assertFalse(approxEqual(identity(), mat));
    }

    @Test
    void transformSettersAreChainable() {
        Model m = new Model(null);
        assertSame(m, m.setPosition(1, 1, 1));
        assertSame(m, m.setRotation(10, 20, 30));
        assertSame(m, m.setScale(2));
        assertSame(m, m.setScale(2, 2, 2));
    }

    @Test
    void meshMaterialVisibleAndUserData() {
        Material mat = new Material(Material.Type.PHONG);
        Model m = new Model(null, mat);
        assertSame(mat, m.getMaterial());
        Material lambert = new Material(Material.Type.LAMBERT);
        m.setMaterial(lambert);
        assertSame(lambert, m.getMaterial());
        assertNull(m.getMesh());

        m.setVisible(false);
        assertFalse(m.isVisible());

        Object tag = new Object();
        m.setUserData(tag);
        assertSame(tag, m.getUserData());
    }

    private static float[] identity() {
        return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static boolean approxEqual(float[] a, float[] b) {
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 0.001) {
                return false;
            }
        }
        return true;
    }

    private static void assertMatrixEquals(float[] expected, float[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], 0.001, "element " + i);
        }
    }
}
