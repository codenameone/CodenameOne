package com.codename1.ui.geom;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Transform;

import static org.junit.jupiter.api.Assertions.*;

class AffineTransformFormTest extends UITestBase {

    @FormTest
    void testAffineTransformMatchesRotationMatrix() {
        double centerX = 150d;
        double centerY = 200d;
        double angle = Math.PI / 4d;

        AffineTransform affine = new AffineTransform();
        affine.setToRotation(angle, centerX, centerY);
        Transform fromAffine = affine.toTransform();

        Transform rotation = Transform.makeRotation((float) angle, (float) centerX, (float) centerY);

        float[][] points = new float[][]{
                new float[]{100f, 140f},
                new float[]{(float) centerX, (float) centerY},
                new float[]{0f, 0f},
                new float[]{(float) (centerX + 50), (float) (centerY - 30)}
        };

        for (float[] point : points) {
            float[] affineResult = new float[3];
            float[] rotationResult = new float[3];
            fromAffine.transformPoint(point, affineResult);
            rotation.transformPoint(point, rotationResult);

            assertEquals(rotationResult[0], affineResult[0], 0.0001f,
                    "X should match for point " + point[0] + "," + point[1]);
            assertEquals(rotationResult[1], affineResult[1], 0.0001f,
                    "Y should match for point " + point[0] + "," + point[1]);
        }
    }

    @FormTest
    void testAffineTransformRotatesPointAroundCenter() {
        double centerX = 75d;
        double centerY = 100d;
        double topY = 25d;
        double angle = Math.PI / 4d;

        AffineTransform affine = new AffineTransform();
        affine.setToRotation(angle, centerX, centerY);
        Transform transform = affine.toTransform();

        float[] input = new float[]{(float) centerX, (float) topY};
        float[] output = transform.transformPoint(input);

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double expectedX = centerX - (topY - centerY) * sin;
        double expectedY = centerY + (topY - centerY) * cos;

        assertEquals(expectedX, output[0], 0.0001f);
        assertEquals(expectedY, output[1], 0.0001f);
    }
}
