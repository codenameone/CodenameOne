package com.codename1.gaming.physics.box2d.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Direct unit tests for the shaded Box2D linear-algebra primitives (Vec2, Vec3,
/// Mat22, Mat33, Rot, Transform, Sweep, MathUtils). These pure-math classes make up a
/// large part of the engine yet are otherwise only reached indirectly, so testing
/// them directly recovers a lot of line coverage.
class Box2dMathTest {

    private static final float EPS = 1e-4f;

    // ---- Vec2 ----------------------------------------------------------------

    @Test
    void vec2LengthAndNormalize() {
        Vec2 v = new Vec2(3, 4);
        assertEquals(25f, v.lengthSquared(), EPS);
        assertEquals(5f, v.length(), EPS);
        float len = v.normalize();
        assertEquals(5f, len, EPS);
        assertEquals(1f, v.length(), EPS);
        assertTrue(v.isValid());
    }

    @Test
    void vec2Arithmetic() {
        Vec2 a = new Vec2(1, 2);
        Vec2 b = new Vec2(3, 5);
        Vec2 sum = a.add(b);
        assertEquals(4f, sum.x, EPS);
        assertEquals(7f, sum.y, EPS);
        Vec2 diff = b.sub(a);
        assertEquals(2f, diff.x, EPS);
        assertEquals(3f, diff.y, EPS);
        Vec2 scaled = a.mul(3);
        assertEquals(3f, scaled.x, EPS);
        assertEquals(6f, scaled.y, EPS);
        Vec2 neg = a.negate();
        assertEquals(-1f, neg.x, EPS);
        // local (mutating) variants
        a.addLocal(b);
        assertEquals(4f, a.x, EPS);
        a.subLocal(b);
        assertEquals(1f, a.x, EPS);
        a.mulLocal(2);
        assertEquals(2f, a.x, EPS);
    }

    @Test
    void vec2SetSkewAbs() {
        Vec2 v = new Vec2();
        v.set(3, -4);
        assertEquals(3f, v.x, EPS);
        v.setZero();
        assertEquals(0f, v.x, EPS);
        Vec2 skew = new Vec2(2, 3).skew();   // (-y, x)
        assertEquals(-3f, skew.x, EPS);
        assertEquals(2f, skew.y, EPS);
        Vec2 abs = new Vec2(-2, -3).abs();
        assertEquals(2f, abs.x, EPS);
        assertEquals(3f, abs.y, EPS);
    }

    @Test
    void vec2DotAndCross() {
        Vec2 a = new Vec2(1, 0);
        Vec2 b = new Vec2(0, 1);
        assertEquals(0f, Vec2.dot(a, b), EPS);
        assertEquals(1f, Vec2.cross(a, b), EPS);           // ax*by - ay*bx
        assertEquals(2f, Vec2.dot(new Vec2(1, 1), new Vec2(1, 1)), EPS);
        Vec2 cvs = Vec2.cross(a, 2f);                       // (s*y, -s*x)
        assertEquals(0f, cvs.x, EPS);
        assertEquals(-2f, cvs.y, EPS);
        Vec2 csv = Vec2.cross(2f, a);                       // (-s*y, s*x)
        assertEquals(0f, csv.x, EPS);
        assertEquals(2f, csv.y, EPS);
    }

    @Test
    void vec2CloneAndEquals() {
        Vec2 a = new Vec2(5, 6);
        Vec2 c = a.clone();
        assertEquals(a.x, c.x, EPS);
        assertNotSame(a, c);
        assertEquals(new Vec2(5, 6), new Vec2(5, 6));
        assertNotEquals(new Vec2(5, 6), new Vec2(5, 7));
    }

    // ---- Vec3 ----------------------------------------------------------------

    @Test
    void vec3Operations() {
        Vec3 a = new Vec3(1, 2, 3);
        Vec3 b = new Vec3(4, 5, 6);
        Vec3 sum = a.add(b);
        assertEquals(5f, sum.x, EPS);
        assertEquals(9f, sum.z, EPS);
        assertEquals(32f, Vec3.dot(a, b), EPS);            // 4+10+18
        Vec3 cross = Vec3.cross(a, b);                      // (-3, 6, -3)
        assertEquals(-3f, cross.x, EPS);
        assertEquals(6f, cross.y, EPS);
        assertEquals(-3f, cross.z, EPS);
        Vec3 neg = a.negate();
        assertEquals(-1f, neg.x, EPS);
        a.setZero();
        assertEquals(0f, a.x, EPS);
    }

    // ---- Mat22 ---------------------------------------------------------------

    @Test
    void mat22RotationAndIdentity() {
        Mat22 id = new Mat22();
        id.setIdentity();
        Vec2 v = id.mul(new Vec2(7, 9));
        assertEquals(7f, v.x, EPS);
        assertEquals(9f, v.y, EPS);

        Mat22 rot = new Mat22();
        rot.set((float) (Math.PI / 2));                    // 90 deg CCW
        Vec2 r = rot.mul(new Vec2(1, 0));
        assertEquals(0f, r.x, EPS);
        assertEquals(1f, r.y, EPS);
        assertEquals((float) (Math.PI / 2), rot.getAngle(), EPS);
    }

    @Test
    void mat22InvertUndoesRotation() {
        Mat22 rot = new Mat22();
        rot.set(0.7f);
        Mat22 inv = rot.invert();
        Vec2 back = inv.mul(rot.mul(new Vec2(3, -2)));
        assertEquals(3f, back.x, 1e-3);
        assertEquals(-2f, back.y, 1e-3);
    }

    // ---- Mat33 ---------------------------------------------------------------

    @Test
    void mat33Solve22() {
        // [[2,0],[0,4]] x = (4,8)  ->  x = (2,2)
        Mat33 m = new Mat33();
        m.ex.set(2, 0, 0);
        m.ey.set(0, 4, 0);
        Vec2 x = m.solve22(new Vec2(4, 8));
        assertEquals(2f, x.x, EPS);
        assertEquals(2f, x.y, EPS);
    }

    @Test
    void mat33Solve33() {
        Mat33 m = new Mat33();
        m.ex.set(2, 0, 0);
        m.ey.set(0, 3, 0);
        m.ez.set(0, 0, 5);
        Vec3 x = m.solve33(new Vec3(2, 6, 10));
        assertEquals(1f, x.x, EPS);
        assertEquals(2f, x.y, EPS);
        assertEquals(2f, x.z, EPS);
    }

    // ---- Rot -----------------------------------------------------------------

    @Test
    void rotTrigAndAxes() {
        Rot r = new Rot((float) (Math.PI / 2));
        assertEquals(1f, r.getSin(), EPS);
        assertEquals(0f, r.getCos(), EPS);
        assertEquals((float) (Math.PI / 2), r.getAngle(), EPS);
        Vec2 xAxis = new Vec2();
        r.getXAxis(xAxis);
        assertEquals(0f, xAxis.x, EPS);
        assertEquals(1f, xAxis.y, EPS);
        r.setIdentity();
        assertEquals(0f, r.getSin(), EPS);
        assertEquals(1f, r.getCos(), EPS);
    }

    // ---- Transform -----------------------------------------------------------

    @Test
    void transformMapsAndInverts() {
        Transform xf = new Transform();
        xf.set(new Vec2(10, 20), (float) (Math.PI / 2));
        Vec2 p = Transform.mul(xf, new Vec2(1, 0));        // rotate then translate
        assertEquals(10f, p.x, 1e-3);
        assertEquals(21f, p.y, 1e-3);
        Vec2 back = Transform.mulTrans(xf, p);             // inverse transform
        assertEquals(1f, back.x, 1e-3);
        assertEquals(0f, back.y, 1e-3);
        Transform id = new Transform();
        id.setIdentity();
        Vec2 same = Transform.mul(id, new Vec2(5, 6));
        assertEquals(5f, same.x, EPS);
    }

    // ---- Sweep ---------------------------------------------------------------

    @Test
    void sweepInterpolatesTransform() {
        Sweep s = new Sweep();
        s.c0.set(0, 0);
        s.c.set(10, 0);
        s.a0 = 0;
        s.a = 0;
        s.localCenter.setZero();
        Transform xf = new Transform();
        s.getTransform(xf, 0f);
        assertEquals(0f, xf.p.x, EPS);
        s.getTransform(xf, 1f);
        assertEquals(10f, xf.p.x, EPS);
        s.getTransform(xf, 0.5f);
        assertEquals(5f, xf.p.x, EPS);
        s.advance(0.5f);   // must not throw
    }

    // ---- MathUtils -----------------------------------------------------------

    @Test
    void mathUtilsClampMinMax() {
        assertEquals(3f, MathUtils.clamp(5f, 0f, 3f), EPS);
        assertEquals(0f, MathUtils.clamp(-1f, 0f, 3f), EPS);
        assertEquals(2f, MathUtils.clamp(2f, 0f, 3f), EPS);
        assertEquals(5f, MathUtils.max(5f, 3f), EPS);
        assertEquals(3, MathUtils.max(2, 3));
        assertEquals(3f, MathUtils.min(5f, 3f), EPS);
        assertEquals(2, MathUtils.min(2, 3));
    }

    @Test
    void mathUtilsDistanceAndPowers() {
        assertEquals(5f, MathUtils.distance(new Vec2(0, 0), new Vec2(3, 4)), EPS);
        assertEquals(25f, MathUtils.distanceSquared(new Vec2(0, 0), new Vec2(3, 4)), EPS);
        assertEquals(8, MathUtils.nextPowerOfTwo(5));
        assertEquals(32, MathUtils.nextPowerOfTwo(17));
        assertTrue(MathUtils.isPowerOfTwo(16));
        assertFalse(MathUtils.isPowerOfTwo(17));
    }

    @Test
    void mathUtilsTrigApproximations() {
        assertEquals((float) Math.sin(0.6), MathUtils.sin(0.6f), 1e-2);
        assertEquals((float) Math.cos(0.6), MathUtils.cos(0.6f), 1e-2);
        assertEquals(3.14159f, MathUtils.PI, 1e-4);
        assertEquals((float) (Math.PI / 180), MathUtils.DEG2RAD, 1e-5);
    }

    @Test
    void mathUtilsClampVec() {
        Vec2 clamped = MathUtils.clamp(new Vec2(5, -5), new Vec2(0, 0), new Vec2(3, 3));
        assertEquals(3f, clamped.x, EPS);
        assertEquals(0f, clamped.y, EPS);
    }
}
