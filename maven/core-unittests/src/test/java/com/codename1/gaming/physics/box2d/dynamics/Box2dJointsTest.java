package com.codename1.gaming.physics.box2d.dynamics;

import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.Joint;
import com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Creates each Box2D joint type and steps the simulation so the joint's
/// initialization, velocity solve and position solve all run. The wrapper never
/// exposes joints, so without these the whole joints package is dead coverage.
class Box2dJointsTest {

    private World newWorld() {
        return new World(new Vec2(0, -10));   // box2d is y-up; gravity points down
    }

    private Body box(World w, BodyType type, float x, float y) {
        BodyDef bd = new BodyDef();
        bd.type = type;
        bd.position.set(x, y);
        Body b = w.createBody(bd);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);
        b.createFixture(shape, 1f);
        return b;
    }

    private static void steps(World w, int n) {
        for (int i = 0; i < n; i++) {
            w.step(1f / 60f, 8, 3);
        }
    }

    private void assertCommonJointApi(World w, Joint j, Body a, Body b) {
        assertNotNull(j);
        assertSame(a, j.getBodyA());
        assertSame(b, j.getBodyB());
        assertNotNull(j.getType());
        Vec2 anchorA = new Vec2();
        j.getAnchorA(anchorA);   // must not throw
        steps(w, 30);            // drive solveVelocity/solvePosition
        w.destroyJoint(j);       // and exercise teardown
    }

    @Test
    void revoluteJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 1, 10);
        RevoluteJointDef def = new RevoluteJointDef();
        def.initialize(a, b, new Vec2(0, 10));
        def.enableLimit = true;
        def.lowerAngle = -1f;
        def.upperAngle = 1f;
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }

    @Test
    void distanceJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 0, 6);
        DistanceJointDef def = new DistanceJointDef();
        def.initialize(a, b, a.getPosition(), b.getPosition());
        def.frequencyHz = 4f;
        def.dampingRatio = 0.5f;
        Joint j = w.createJoint(def);
        assertTrue(j instanceof DistanceJoint);
        assertCommonJointApi(w, j, a, b);
    }

    @Test
    void prismaticJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 0, 10);
        PrismaticJointDef def = new PrismaticJointDef();
        def.initialize(a, b, new Vec2(0, 10), new Vec2(1, 0));
        def.enableLimit = true;
        def.lowerTranslation = -5f;
        def.upperTranslation = 5f;
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }

    @Test
    void weldJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 1, 10);
        WeldJointDef def = new WeldJointDef();
        def.initialize(a, b, new Vec2(0.5f, 10));
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }

    @Test
    void wheelJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 0, 9);
        WheelJointDef def = new WheelJointDef();
        def.initialize(a, b, b.getPosition(), new Vec2(0, 1));
        def.enableMotor = true;
        def.motorSpeed = 2f;
        def.maxMotorTorque = 10f;
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }

    @Test
    void frictionJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 0, 9);
        FrictionJointDef def = new FrictionJointDef();
        def.initialize(a, b, new Vec2(0, 9));
        def.maxForce = 10f;
        def.maxTorque = 5f;
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }

    @Test
    void mouseJoint() {
        World w = newWorld();
        Body ground = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 0, 6);
        MouseJointDef def = new MouseJointDef();
        def.bodyA = ground;
        def.bodyB = b;
        def.target.set(b.getPosition());
        def.maxForce = 100f * b.getMass();
        Joint j = w.createJoint(def);
        steps(w, 20);
        w.destroyJoint(j);
    }

    @Test
    void ropeJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.STATIC, 0, 10);
        Body b = box(w, BodyType.DYNAMIC, 0, 6);
        RopeJointDef def = new RopeJointDef();
        def.bodyA = a;
        def.bodyB = b;
        def.localAnchorA.set(0, 0);
        def.localAnchorB.set(0, 0);
        def.maxLength = 4f;
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }

    @Test
    void gearJoint() {
        World w = newWorld();
        Body ground = box(w, BodyType.STATIC, 0, 10);
        Body b1 = box(w, BodyType.DYNAMIC, -2, 10);
        Body b2 = box(w, BodyType.DYNAMIC, 2, 10);
        RevoluteJointDef r1 = new RevoluteJointDef();
        r1.initialize(ground, b1, b1.getPosition());
        RevoluteJoint j1 = (RevoluteJoint) w.createJoint(r1);
        RevoluteJointDef r2 = new RevoluteJointDef();
        r2.initialize(ground, b2, b2.getPosition());
        RevoluteJoint j2 = (RevoluteJoint) w.createJoint(r2);
        GearJointDef def = new GearJointDef();
        def.bodyA = b1;
        def.bodyB = b2;
        def.joint1 = j1;
        def.joint2 = j2;
        def.ratio = 1f;
        Joint gear = w.createJoint(def);
        assertNotNull(gear);
        steps(w, 30);
        w.destroyJoint(gear);
    }

    @Test
    void constantVolumeJoint() {
        World w = newWorld();
        ConstantVolumeJointDef def = new ConstantVolumeJointDef();
        def.frequencyHz = 10f;
        def.dampingRatio = 1f;
        for (int i = 0; i < 6; i++) {
            double a = i * 2 * Math.PI / 6;
            def.addBody(box(w, BodyType.DYNAMIC,
                    (float) (Math.cos(a) * 2), (float) (10 + Math.sin(a) * 2)));
        }
        Joint cv = w.createJoint(def);
        assertNotNull(cv);
        steps(w, 30);
        w.destroyJoint(cv);
    }

    @Test
    void pulleyJoint() {
        World w = newWorld();
        Body a = box(w, BodyType.DYNAMIC, -2, 6);
        Body b = box(w, BodyType.DYNAMIC, 2, 6);
        PulleyJointDef def = new PulleyJointDef();
        def.initialize(a, b, new Vec2(-2, 12), new Vec2(2, 12),
                a.getPosition(), b.getPosition(), 1f);
        assertCommonJointApi(w, w.createJoint(def), a, b);
    }
}
