package com.codename1.gaming.physics.box2d.common;

import com.codename1.gaming.physics.box2d.collision.AABB;
import com.codename1.gaming.physics.box2d.collision.RayCastInput;
import com.codename1.gaming.physics.box2d.collision.RayCastOutput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Exhaustive coverage of the Mat22 / Mat33 / AABB helper math. These have many
/// small methods (in-place, *ToOut and *Unsafe variants) that are otherwise barely
/// touched, so calling them all recovers a lot of line coverage.
class Box2dMatrixAabbTest {

    private static final float EPS = 1e-4f;

    private static void assertVec(Vec2 v, float x, float y) {
        assertEquals(x, v.x, 1e-3);
        assertEquals(y, v.y, 1e-3);
    }

    // ---- Mat22 ---------------------------------------------------------------

    @Test
    void constructionAndSetters() {
        Mat22 m = new Mat22(new Vec2(1, 2), new Vec2(3, 4));   // columns ex,ey
        Mat22 b = new Mat22(1, 3, 2, 4);                        // exx,col2x,exy,col2y -> same
        assertEquals(m.ex.x, b.ex.x, EPS);
        assertEquals(m.ey.y, b.ey.y, EPS);

        Mat22 c = new Mat22();
        c.set(m);
        assertEquals(1f, c.ex.x, EPS);
        c.set(5, 6, 7, 8);
        assertEquals(5f, c.ex.x, EPS);
        c.set(new Vec2(9, 10), new Vec2(11, 12));
        assertEquals(9f, c.ex.x, EPS);
        c.setZero();
        assertEquals(0f, c.ex.x, EPS);
        c.setIdentity();
        assertEquals(1f, c.ex.x, EPS);
        assertEquals(1f, c.ey.y, EPS);

        Mat22 clone = m.clone();
        assertNotSame(m, clone);
        assertEquals(m.ex.x, clone.ex.x, EPS);
        assertEquals(m, m.clone());
        m.hashCode();
    }

    @Test
    void invertAndSolveAgree() {
        Mat22 m = new Mat22(2, 1, 1, 3);     // [[2,1],[1,3]]
        Vec2 solved = m.solve(new Vec2(5, 5));
        assertVec(solved, 2, 1);             // 2x+y=5, x+3y=5

        Mat22 inv = m.invert();
        assertVec(inv.mul(new Vec2(5, 5)), 2, 1);

        Vec2 outSolve = new Vec2();
        m.solveToOut(new Vec2(5, 5), outSolve);
        assertVec(outSolve, 2, 1);

        Mat22 invOut = new Mat22();
        m.invertToOut(invOut);
        assertVec(invOut.mul(new Vec2(5, 5)), 2, 1);

        Mat22 m2 = new Mat22(2, 1, 1, 3);
        m2.invertLocal();
        assertVec(m2.mul(new Vec2(5, 5)), 2, 1);
    }

    @Test
    void rotationScaleAndAngle() {
        Mat22 rot = Mat22.createRotationalTransform((float) (Math.PI / 2));
        assertVec(rot.mul(new Vec2(1, 0)), 0, 1);
        assertEquals((float) (Math.PI / 2), rot.getAngle(), EPS);
        Mat22 rotOut = new Mat22();
        Mat22.createRotationalTransform((float) (Math.PI / 2), rotOut);
        assertVec(rotOut.mul(new Vec2(1, 0)), 0, 1);

        Mat22 scale = Mat22.createScaleTransform(3f);
        assertVec(scale.mul(new Vec2(2, 5)), 6, 15);
        Mat22 scaleOut = new Mat22();
        Mat22.createScaleTransform(3f, scaleOut);
        assertVec(scaleOut.mul(new Vec2(2, 5)), 6, 15);
    }

    @Test
    void absAddMul() {
        Mat22 m = new Mat22(-1, -2, -3, -4);
        Mat22 a = m.abs();
        assertEquals(1f, a.ex.x, EPS);
        assertEquals(4f, a.ey.y, EPS);
        assertEquals(2f, Mat22.abs(m).ey.x, EPS);
        Mat22 absOut = new Mat22();
        Mat22.absToOut(m, absOut);
        assertEquals(1f, absOut.ex.x, EPS);
        m.absLocal();
        assertEquals(1f, m.ex.x, EPS);

        Mat22 p = new Mat22(1, 2, 3, 4);
        Mat22 q = new Mat22(1, 0, 0, 1);     // identity
        Mat22 sum = p.add(q);
        assertEquals(2f, sum.ex.x, EPS);
        Mat22 product = p.mul(q);            // p * I == p
        assertEquals(1f, product.ex.x, EPS);
        assertEquals(4f, product.ey.y, EPS);
    }

    @Test
    void matrixVectorOutVariants() {
        Mat22 rot = new Mat22();
        rot.set((float) (Math.PI / 2));
        Vec2 ref = rot.mul(new Vec2(1, 0));
        Vec2 out1 = new Vec2();
        rot.mulToOut(new Vec2(1, 0), out1);
        assertVec(out1, ref.x, ref.y);
        Vec2 out2 = new Vec2();
        rot.mulToOutUnsafe(new Vec2(1, 0), out2);
        assertVec(out2, ref.x, ref.y);
        // mulTrans = inverse rotation for an orthonormal matrix
        assertVec(rot.mulTrans(ref), 1, 0);
        Vec2 tOut = new Vec2();
        rot.mulTransToOut(ref, tOut);
        assertVec(tOut, 1, 0);
        assertVec(Mat22.mul(rot, new Vec2(1, 0)), ref.x, ref.y);
        assertVec(Mat22.mulTrans(rot, ref), 1, 0);
    }

    @Test
    void matrixMatrixVariants() {
        Mat22 a = new Mat22(1, 2, 3, 4);
        Mat22 id = new Mat22();
        id.setIdentity();
        Mat22 out = new Mat22();
        a.mulToOut(id, out);
        assertEquals(a.ex.x, out.ex.x, EPS);
        a.mulToOutUnsafe(id, new Mat22());
        Mat22 trans = a.mulTrans(id);
        assertNotNull(trans);
        a.mulTransToOut(id, new Mat22());
        Mat22 staticMul = Mat22.mul(a, id);
        assertEquals(a.ex.x, staticMul.ex.x, EPS);
        Mat22.mulToOut(a, id, new Mat22());
        Mat22.mulTransToOut(a, id, new Mat22());
        a.mulLocal(id);
        a.mulTransLocal(id);
        a.addLocal(new Mat22(0, 0, 0, 0));
    }

    // ---- Mat33 ---------------------------------------------------------------

    @Test
    void mat33Inverses() {
        Mat33 m = new Mat33();
        m.ex.set(2, 0, 0);
        m.ey.set(0, 4, 0);
        m.ez.set(0, 0, 8);
        Mat33 inv22 = new Mat33();
        m.getInverse22(inv22);
        assertEquals(0.5f, inv22.ex.x, EPS);
        assertEquals(0.25f, inv22.ey.y, EPS);
        Mat33 symInv = new Mat33();
        m.getSymInverse33(symInv);
        assertNotNull(symInv);
        Vec2 out2 = new Vec2();
        m.solve22ToOut(new Vec2(4, 8), out2);
        assertVec(out2, 2, 2);
        Vec3 out3 = new Vec3();
        m.solve33ToOut(new Vec3(2, 8, 8), out3);
        assertEquals(1f, out3.x, EPS);
        m.setZero();
        assertEquals(0f, m.ex.x, EPS);
    }

    // ---- AABB ----------------------------------------------------------------

    @Test
    void aabbCombinePerimeterSet() {
        AABB a = new AABB(new Vec2(0, 0), new Vec2(4, 4));
        AABB b = new AABB(new Vec2(2, 2), new Vec2(8, 6));
        a.combine(b);
        assertEquals(8f, a.upperBound.x, EPS);
        assertEquals(6f, a.upperBound.y, EPS);
        // perimeter of a 8x6 box = 2*(8+6) = 28
        assertEquals(28f, a.getPerimeter(), EPS);

        AABB copy = new AABB();
        copy.set(a);
        assertEquals(a.upperBound.x, copy.upperBound.x, EPS);

        AABB centered = new AABB(new Vec2(-2, -2), new Vec2(2, 2));
        Vec2 c = new Vec2();
        centered.getCenterToOut(c);
        assertVec(c, 0, 0);
        Vec2 e = new Vec2();
        centered.getExtentsToOut(e);
        assertVec(e, 2, 2);
        Vec2[] verts = {new Vec2(), new Vec2(), new Vec2(), new Vec2()};
        centered.getVertices(verts);
        assertEquals(-2f, verts[0].x, EPS);
    }

    @Test
    void aabbRaycast() {
        AABB box = new AABB(new Vec2(-1, -1), new Vec2(1, 1));
        RayCastInput in = new RayCastInput();
        in.p1.set(-5, 0);
        in.p2.set(5, 0);
        in.maxFraction = 1f;
        RayCastOutput out = new RayCastOutput();
        assertTrue(box.raycast(out, in));     // ray passes straight through the box
        assertTrue(out.fraction >= 0 && out.fraction <= 1f);

        RayCastInput miss = new RayCastInput();
        miss.p1.set(-5, 50);
        miss.p2.set(5, 50);
        miss.maxFraction = 1f;
        assertFalse(box.raycast(new RayCastOutput(), miss));
    }
}
