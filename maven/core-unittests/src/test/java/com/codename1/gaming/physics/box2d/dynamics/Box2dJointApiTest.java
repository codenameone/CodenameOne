package com.codename1.gaming.physics.box2d.dynamics;

import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint;
import com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the getters/setters/motor/limit controls on each motorised joint, which
/// the plain "create and step" tests never reach.
class Box2dJointApiTest {

    private final World w = new World(new Vec2(0, -10));
    private final Body a = box(BodyType.STATIC, 0, 10);
    private final Body b = box(BodyType.DYNAMIC, 1, 10);

    private Body box(BodyType type, float x, float y) {
        BodyDef bd = new BodyDef();
        bd.type = type;
        bd.position.set(x, y);
        Body body = w.createBody(bd);
        PolygonShape s = new PolygonShape();
        s.setAsBox(0.5f, 0.5f);
        body.createFixture(s, 1f);
        return body;
    }

    private void step() {
        w.step(1f / 60f, 8, 3);
    }

    @Test
    void revoluteJointApi() {
        RevoluteJointDef def = new RevoluteJointDef();
        def.initialize(a, b, new Vec2(0, 10));
        def.enableMotor = true;
        def.motorSpeed = 1f;
        def.maxMotorTorque = 10f;
        def.enableLimit = true;
        def.lowerAngle = -1f;
        def.upperAngle = 1f;
        RevoluteJoint j = (RevoluteJoint) w.createJoint(def);
        step();
        assertNotNull(j.getLocalAnchorA());
        assertNotNull(j.getLocalAnchorB());
        j.getReferenceAngle();
        j.getAnchorA(new Vec2());
        j.getAnchorB(new Vec2());
        j.getReactionForce(60f, new Vec2());
        j.getReactionTorque(60f);
        j.getJointAngle();
        j.getJointSpeed();
        j.getMotorTorque(60f);
        assertTrue(j.isMotorEnabled());
        j.enableMotor(false);
        j.enableMotor(true);
        j.setMotorSpeed(2f);
        assertEquals(2f, j.getMotorSpeed(), 1e-3);
        j.setMaxMotorTorque(20f);
        assertEquals(20f, j.getMaxMotorTorque(), 1e-3);
        assertTrue(j.isLimitEnabled());
        j.enableLimit(false);
        j.enableLimit(true);
        j.getLowerLimit();
        j.getUpperLimit();
        j.setLimits(-2f, 2f);
        step();
    }

    @Test
    void prismaticJointApi() {
        PrismaticJointDef def = new PrismaticJointDef();
        def.initialize(a, b, new Vec2(0, 10), new Vec2(1, 0));
        def.enableMotor = true;
        def.motorSpeed = 1f;
        def.maxMotorForce = 10f;
        def.enableLimit = true;
        def.lowerTranslation = -5f;
        def.upperTranslation = 5f;
        PrismaticJoint j = (PrismaticJoint) w.createJoint(def);
        step();
        assertNotNull(j.getLocalAnchorA());
        j.getAnchorA(new Vec2());
        j.getReactionForce(60f, new Vec2());
        j.getReactionTorque(60f);
        j.getJointSpeed();
        j.getJointTranslation();
        assertTrue(j.isMotorEnabled());
        j.enableMotor(false);
        j.setMotorSpeed(2f);
        assertEquals(2f, j.getMotorSpeed(), 1e-3);
        j.setMaxMotorForce(20f);
        assertTrue(j.isLimitEnabled());
        j.enableLimit(false);
        j.getLowerLimit();
        j.getUpperLimit();
        j.setLimits(-3f, 3f);
        step();
    }

    @Test
    void wheelJointApi() {
        WheelJointDef def = new WheelJointDef();
        def.initialize(a, b, b.getPosition(), new Vec2(0, 1));
        def.enableMotor = true;
        def.motorSpeed = 2f;
        def.maxMotorTorque = 10f;
        def.frequencyHz = 4f;
        def.dampingRatio = 0.7f;
        WheelJoint j = (WheelJoint) w.createJoint(def);
        step();
        assertNotNull(j.getLocalAxisA());
        j.getAnchorA(new Vec2());
        j.getReactionForce(60f, new Vec2());
        j.getReactionTorque(60f);
        j.getJointTranslation();
        j.getJointSpeed();
        assertTrue(j.isMotorEnabled());
        j.enableMotor(false);
        j.setMotorSpeed(3f);
        assertEquals(3f, j.getMotorSpeed(), 1e-3);
        j.setMaxMotorTorque(20f);
        j.getMaxMotorTorque();
        j.getMotorTorque(60f);
        j.setSpringFrequencyHz(5f);
        assertEquals(5f, j.getSpringFrequencyHz(), 1e-3);
        step();
    }

    @Test
    void weldAndDistanceJointApi() {
        Body c = box(BodyType.DYNAMIC, 2, 10);
        WeldJointDef wd = new WeldJointDef();
        wd.initialize(a, c, new Vec2(1.5f, 10));
        wd.frequencyHz = 4f;
        wd.dampingRatio = 0.5f;
        WeldJoint wj = (WeldJoint) w.createJoint(wd);
        step();
        wj.getReferenceAngle();
        wj.getReactionForce(60f, new Vec2());
        wj.getReactionTorque(60f);
        wj.setFrequency(5f);
        assertEquals(5f, wj.getFrequency(), 1e-3);
        wj.setDampingRatio(0.8f);
        assertEquals(0.8f, wj.getDampingRatio(), 1e-3);

        Body d = box(BodyType.DYNAMIC, 0, 6);
        DistanceJointDef dd = new DistanceJointDef();
        dd.initialize(a, d, a.getPosition(), d.getPosition());
        DistanceJoint dj = (DistanceJoint) w.createJoint(dd);
        step();
        dj.setLength(3f);
        assertEquals(3f, dj.getLength(), 1e-3);
        dj.setFrequency(2f);
        assertEquals(2f, dj.getFrequency(), 1e-3);
        dj.setDampingRatio(0.4f);
        assertEquals(0.4f, dj.getDampingRatio(), 1e-3);
        dj.getReactionForce(60f, new Vec2());
        dj.getReactionTorque(60f);
    }
}
