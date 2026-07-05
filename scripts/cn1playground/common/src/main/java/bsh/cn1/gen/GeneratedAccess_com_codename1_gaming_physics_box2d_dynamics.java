package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics_box2d_dynamics {
    private GeneratedAccess_com_codename1_gaming_physics_box2d_dynamics() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("Body".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.Body.class;
        }
        if ("BodyDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.BodyDef.class;
        }
        if ("BodyType".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.BodyType.class;
        }
        if ("ContactManager".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.ContactManager.class;
        }
        if ("Filter".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.Filter.class;
        }
        if ("Fixture".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.Fixture.class;
        }
        if ("FixtureDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.FixtureDef.class;
        }
        if ("FixtureProxy".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.FixtureProxy.class;
        }
        if ("Island".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.Island.class;
        }
        if ("Profile".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.Profile.class;
        }
        if ("SolverData".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.SolverData.class;
        }
        if ("TimeStep".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.TimeStep.class;
        }
        if ("World".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.World.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.dynamics.Body.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.BodyDef.class, com.codename1.gaming.physics.box2d.dynamics.World.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.BodyDef.class, com.codename1.gaming.physics.box2d.dynamics.World.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.Body((com.codename1.gaming.physics.box2d.dynamics.BodyDef) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.World) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.BodyDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.BodyDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.ContactManager.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.World.class, com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhaseStrategy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.World.class, com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhaseStrategy.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.ContactManager((com.codename1.gaming.physics.box2d.dynamics.World) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhaseStrategy) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.Filter.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.Filter();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.Fixture.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.Fixture();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.FixtureDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.FixtureDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.FixtureProxy.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.FixtureProxy();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.Island.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.Island();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.Profile.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.Profile();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.SolverData.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.SolverData();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.TimeStep.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.TimeStep();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.World.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.World((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.World((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.pooling.IWorldPool.class, com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhaseStrategy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.pooling.IWorldPool.class, com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhaseStrategy.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.World((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[1], (com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhaseStrategy) adaptedArgs[2]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedStatic(type, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Body) {
            try {
                return invoke0((com.codename1.gaming.physics.box2d.dynamics.Body) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.ContactManager) {
            try {
                return invoke1((com.codename1.gaming.physics.box2d.dynamics.ContactManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Filter) {
            try {
                return invoke2((com.codename1.gaming.physics.box2d.dynamics.Filter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Fixture) {
            try {
                return invoke3((com.codename1.gaming.physics.box2d.dynamics.Fixture) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Island) {
            try {
                return invoke4((com.codename1.gaming.physics.box2d.dynamics.Island) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Profile) {
            try {
                return invoke5((com.codename1.gaming.physics.box2d.dynamics.Profile) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.World) {
            try {
                return invoke6((com.codename1.gaming.physics.box2d.dynamics.World) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.box2d.dynamics.Body typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("applyAngularImpulse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.applyAngularImpulse(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("applyForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.applyForce((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("applyForceToCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.applyForceToCenter((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("applyLinearImpulse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.applyLinearImpulse((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("applyTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.applyTorque(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("createFixture".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.FixtureDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.FixtureDef.class}, false);
                return typedTarget.createFixture((com.codename1.gaming.physics.box2d.dynamics.FixtureDef) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Float.class}, false);
                return typedTarget.createFixture((com.codename1.gaming.physics.box2d.collision.shapes.Shape) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("destroyFixture".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false);
                typedTarget.destroyFixture((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0]); return null;
            }
        }
        if ("getAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngle();
            }
        }
        if ("getAngularDamping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngularDamping();
            }
        }
        if ("getAngularVelocity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngularVelocity();
            }
        }
        if ("getContactList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContactList();
            }
        }
        if ("getFixtureList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureList();
            }
        }
        if ("getGravityScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGravityScale();
            }
        }
        if ("getInertia".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInertia();
            }
        }
        if ("getJointList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointList();
            }
        }
        if ("getLinearDamping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinearDamping();
            }
        }
        if ("getLinearVelocity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinearVelocity();
            }
        }
        if ("getLinearVelocityFromLocalPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getLinearVelocityFromLocalPoint((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getLinearVelocityFromLocalPointToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getLinearVelocityFromLocalPointToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getLinearVelocityFromWorldPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getLinearVelocityFromWorldPoint((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getLinearVelocityFromWorldPointToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getLinearVelocityFromWorldPointToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getLocalCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalCenter();
            }
        }
        if ("getLocalPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getLocalPoint((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getLocalPointToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getLocalPointToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getLocalVector".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getLocalVector((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getLocalVectorToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getLocalVectorToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getLocalVectorToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getLocalVectorToOutUnsafe((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getMass".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMass();
            }
        }
        if ("getMassData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false);
                typedTarget.getMassData((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0]); return null;
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPosition();
            }
        }
        if ("getTransform".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTransform();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUserData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserData();
            }
        }
        if ("getWorld".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWorld();
            }
        }
        if ("getWorldCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWorldCenter();
            }
        }
        if ("getWorldPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getWorldPoint((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getWorldPointToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldPointToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getWorldVector".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getWorldVector((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getWorldVectorToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldVectorToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getWorldVectorToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldVectorToOutUnsafe((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("isAwake".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAwake();
            }
        }
        if ("isBullet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBullet();
            }
        }
        if ("isFixedRotation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFixedRotation();
            }
        }
        if ("isSleepingAllowed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSleepingAllowed();
            }
        }
        if ("resetMassData".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetMassData(); return null;
            }
        }
        if ("setActive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setActive(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAngularDamping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setAngularDamping(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setAngularVelocity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setAngularVelocity(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setAwake".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAwake(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBullet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBullet(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFixedRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFixedRotation(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGravityScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setGravityScale(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLinearDamping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLinearDamping(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLinearVelocity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setLinearVelocity((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("setMassData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false);
                typedTarget.setMassData((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0]); return null;
            }
        }
        if ("setSleepingAllowed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSleepingAllowed(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false);
                typedTarget.setTransform((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.BodyType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.BodyType.class}, false);
                typedTarget.setType((com.codename1.gaming.physics.box2d.dynamics.BodyType) adaptedArgs[0]); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("shouldCollide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false);
                return typedTarget.shouldCollide((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0]);
            }
        }
        if ("synchronizeTransform".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.synchronizeTransform(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.box2d.dynamics.ContactManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPair".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                typedTarget.addPair((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("collide".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.collide(); return null;
            }
        }
        if ("destroy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false);
                typedTarget.destroy((com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) adaptedArgs[0]); return null;
            }
        }
        if ("findNewContacts".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.findNewContacts(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.box2d.dynamics.Filter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Filter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Filter.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.dynamics.Filter) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.box2d.dynamics.Fixture typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.FixtureDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.FixtureDef.class}, false);
                typedTarget.create((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.FixtureDef) adaptedArgs[1]); return null;
            }
        }
        if ("createProxies".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.createProxies((com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1]); return null;
            }
        }
        if ("destroy".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destroy(); return null;
            }
        }
        if ("destroyProxies".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase.class}, false);
                typedTarget.destroyProxies((com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase) adaptedArgs[0]); return null;
            }
        }
        if ("dump".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.dump(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("getAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAABB(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBody();
            }
        }
        if ("getDensity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDensity();
            }
        }
        if ("getFilterData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFilterData();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getMassData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false);
                typedTarget.getMassData((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0]); return null;
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRestitution();
            }
        }
        if ("getShape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShape();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUserData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserData();
            }
        }
        if ("isSensor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSensor();
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, java.lang.Integer.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
        }
        if ("refilter".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refilter(); return null;
            }
        }
        if ("setDensity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setDensity(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setFilterData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Filter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Filter.class}, false);
                typedTarget.setFilterData((com.codename1.gaming.physics.box2d.dynamics.Filter) adaptedArgs[0]); return null;
            }
        }
        if ("setFriction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setFriction(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setRestitution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRestitution(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setSensor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSensor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("testPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.testPoint((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.box2d.dynamics.Island typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false);
                typedTarget.add((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false);
                typedTarget.add((com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class}, false);
                typedTarget.add((com.codename1.gaming.physics.box2d.dynamics.joints.Joint) adaptedArgs[0]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.init(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[3]); return null;
            }
        }
        if ("report".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint[].class}, false);
                typedTarget.report((com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint[]) adaptedArgs[0]); return null;
            }
        }
        if ("solve".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Profile.class, com.codename1.gaming.physics.box2d.dynamics.TimeStep.class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Profile.class, com.codename1.gaming.physics.box2d.dynamics.TimeStep.class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Boolean.class}, false);
                typedTarget.solve((com.codename1.gaming.physics.box2d.dynamics.Profile) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.TimeStep) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("solveTOI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.TimeStep.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.TimeStep.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.solveTOI((com.codename1.gaming.physics.box2d.dynamics.TimeStep) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.box2d.dynamics.Profile typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("toDebugStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                typedTarget.toDebugStrings((java.util.List) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.gaming.physics.box2d.dynamics.World typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearForces".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearForces(); return null;
            }
        }
        if ("createBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.BodyDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.BodyDef.class}, false);
                return typedTarget.createBody((com.codename1.gaming.physics.box2d.dynamics.BodyDef) adaptedArgs[0]);
            }
        }
        if ("createJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.JointDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.JointDef.class}, false);
                return typedTarget.createJoint((com.codename1.gaming.physics.box2d.dynamics.joints.JointDef) adaptedArgs[0]);
            }
        }
        if ("destroyBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false);
                typedTarget.destroyBody((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0]); return null;
            }
        }
        if ("destroyJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class}, false);
                typedTarget.destroyJoint((com.codename1.gaming.physics.box2d.dynamics.joints.Joint) adaptedArgs[0]); return null;
            }
        }
        if ("drawDebugData".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.drawDebugData(); return null;
            }
        }
        if ("getAutoClearForces".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAutoClearForces();
            }
        }
        if ("getBodyCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyCount();
            }
        }
        if ("getBodyList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyList();
            }
        }
        if ("getContactCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContactCount();
            }
        }
        if ("getContactList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContactList();
            }
        }
        if ("getContactManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContactManager();
            }
        }
        if ("getGravity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGravity();
            }
        }
        if ("getJointCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointCount();
            }
        }
        if ("getJointList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointList();
            }
        }
        if ("getPool".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPool();
            }
        }
        if ("getProfile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProfile();
            }
        }
        if ("getProxyCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProxyCount();
            }
        }
        if ("getTreeBalance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTreeBalance();
            }
        }
        if ("getTreeHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTreeHeight();
            }
        }
        if ("getTreeQuality".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTreeQuality();
            }
        }
        if ("isAllowSleep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowSleep();
            }
        }
        if ("isContinuousPhysics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isContinuousPhysics();
            }
        }
        if ("isLocked".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLocked();
            }
        }
        if ("isSleepingAllowed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSleepingAllowed();
            }
        }
        if ("isSubStepping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSubStepping();
            }
        }
        if ("isWarmStarting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWarmStarting();
            }
        }
        if ("popContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                return typedTarget.popContact((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if ("pushContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false);
                typedTarget.pushContact((com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) adaptedArgs[0]); return null;
            }
        }
        if ("queryAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.QueryCallback.class, com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.QueryCallback.class, com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                typedTarget.queryAABB((com.codename1.gaming.physics.box2d.callbacks.QueryCallback) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[1]); return null;
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.RayCastCallback.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.RayCastCallback.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.raycast((com.codename1.gaming.physics.box2d.callbacks.RayCastCallback) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("setAllowSleep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAllowSleep(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAutoClearForces".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutoClearForces(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setContactFilter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactFilter.class}, false);
                typedTarget.setContactFilter((com.codename1.gaming.physics.box2d.callbacks.ContactFilter) adaptedArgs[0]); return null;
            }
        }
        if ("setContactListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.setContactListener((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        if ("setContinuousPhysics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setContinuousPhysics(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDebugDraw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.DebugDraw.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.DebugDraw.class}, false);
                typedTarget.setDebugDraw((com.codename1.gaming.physics.box2d.callbacks.DebugDraw) adaptedArgs[0]); return null;
            }
        }
        if ("setDestructionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.DestructionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.DestructionListener.class}, false);
                typedTarget.setDestructionListener((com.codename1.gaming.physics.box2d.callbacks.DestructionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setGravity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setGravity((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("setSleepingAllowed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSleepingAllowed(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSubStepping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSubStepping(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWarmStarting".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWarmStarting(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("step".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.step(((Number) adaptedArgs[0]).floatValue(), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.dynamics.Body.class) return getStaticField0(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.BodyType.class) return getStaticField1(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.World.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("e_activeFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_activeFlag;
        if ("e_autoSleepFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_autoSleepFlag;
        if ("e_awakeFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_awakeFlag;
        if ("e_bulletFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_bulletFlag;
        if ("e_fixedRotationFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_fixedRotationFlag;
        if ("e_islandFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_islandFlag;
        if ("e_toiFlag".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.Body.e_toiFlag;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.Body.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("DYNAMIC".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.BodyType.DYNAMIC;
        if ("KINEMATIC".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.BodyType.KINEMATIC;
        if ("STATIC".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.BodyType.STATIC;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.BodyType.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("CLEAR_FORCES".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.World.CLEAR_FORCES;
        if ("LOCKED".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.World.LOCKED;
        if ("NEW_FIXTURE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.World.NEW_FIXTURE;
        if ("WORLD_POOL_CONTAINER_SIZE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.World.WORLD_POOL_CONTAINER_SIZE;
        if ("WORLD_POOL_SIZE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.World.WORLD_POOL_SIZE;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.World.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Body) {
            com.codename1.gaming.physics.box2d.dynamics.Body typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Body) target;
            if ("m_I".equals(name)) return typedTarget.m_I;
            if ("m_angularDamping".equals(name)) return typedTarget.m_angularDamping;
            if ("m_angularVelocity".equals(name)) return typedTarget.m_angularVelocity;
            if ("m_contactList".equals(name)) return typedTarget.m_contactList;
            if ("m_fixtureCount".equals(name)) return typedTarget.m_fixtureCount;
            if ("m_fixtureList".equals(name)) return typedTarget.m_fixtureList;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_force".equals(name)) return typedTarget.m_force;
            if ("m_gravityScale".equals(name)) return typedTarget.m_gravityScale;
            if ("m_invI".equals(name)) return typedTarget.m_invI;
            if ("m_invMass".equals(name)) return typedTarget.m_invMass;
            if ("m_islandIndex".equals(name)) return typedTarget.m_islandIndex;
            if ("m_jointList".equals(name)) return typedTarget.m_jointList;
            if ("m_linearDamping".equals(name)) return typedTarget.m_linearDamping;
            if ("m_linearVelocity".equals(name)) return typedTarget.m_linearVelocity;
            if ("m_mass".equals(name)) return typedTarget.m_mass;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_sleepTime".equals(name)) return typedTarget.m_sleepTime;
            if ("m_sweep".equals(name)) return typedTarget.m_sweep;
            if ("m_torque".equals(name)) return typedTarget.m_torque;
            if ("m_type".equals(name)) return typedTarget.m_type;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
            if ("m_world".equals(name)) return typedTarget.m_world;
            if ("m_xf".equals(name)) return typedTarget.m_xf;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.BodyDef) {
            com.codename1.gaming.physics.box2d.dynamics.BodyDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.BodyDef) target;
            if ("active".equals(name)) return typedTarget.active;
            if ("allowSleep".equals(name)) return typedTarget.allowSleep;
            if ("angle".equals(name)) return typedTarget.angle;
            if ("angularDamping".equals(name)) return typedTarget.angularDamping;
            if ("angularVelocity".equals(name)) return typedTarget.angularVelocity;
            if ("awake".equals(name)) return typedTarget.awake;
            if ("bullet".equals(name)) return typedTarget.bullet;
            if ("fixedRotation".equals(name)) return typedTarget.fixedRotation;
            if ("gravityScale".equals(name)) return typedTarget.gravityScale;
            if ("linearDamping".equals(name)) return typedTarget.linearDamping;
            if ("linearVelocity".equals(name)) return typedTarget.linearVelocity;
            if ("position".equals(name)) return typedTarget.position;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.ContactManager) {
            com.codename1.gaming.physics.box2d.dynamics.ContactManager typedTarget = (com.codename1.gaming.physics.box2d.dynamics.ContactManager) target;
            if ("m_broadPhase".equals(name)) return typedTarget.m_broadPhase;
            if ("m_contactCount".equals(name)) return typedTarget.m_contactCount;
            if ("m_contactFilter".equals(name)) return typedTarget.m_contactFilter;
            if ("m_contactList".equals(name)) return typedTarget.m_contactList;
            if ("m_contactListener".equals(name)) return typedTarget.m_contactListener;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Filter) {
            com.codename1.gaming.physics.box2d.dynamics.Filter typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Filter) target;
            if ("categoryBits".equals(name)) return typedTarget.categoryBits;
            if ("groupIndex".equals(name)) return typedTarget.groupIndex;
            if ("maskBits".equals(name)) return typedTarget.maskBits;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Fixture) {
            com.codename1.gaming.physics.box2d.dynamics.Fixture typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Fixture) target;
            if ("m_body".equals(name)) return typedTarget.m_body;
            if ("m_density".equals(name)) return typedTarget.m_density;
            if ("m_filter".equals(name)) return typedTarget.m_filter;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_isSensor".equals(name)) return typedTarget.m_isSensor;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_proxies".equals(name)) return typedTarget.m_proxies;
            if ("m_proxyCount".equals(name)) return typedTarget.m_proxyCount;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_shape".equals(name)) return typedTarget.m_shape;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.FixtureDef) {
            com.codename1.gaming.physics.box2d.dynamics.FixtureDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.FixtureDef) target;
            if ("density".equals(name)) return typedTarget.density;
            if ("filter".equals(name)) return typedTarget.filter;
            if ("friction".equals(name)) return typedTarget.friction;
            if ("isSensor".equals(name)) return typedTarget.isSensor;
            if ("restitution".equals(name)) return typedTarget.restitution;
            if ("shape".equals(name)) return typedTarget.shape;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Island) {
            com.codename1.gaming.physics.box2d.dynamics.Island typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Island) target;
            if ("m_bodies".equals(name)) return typedTarget.m_bodies;
            if ("m_bodyCapacity".equals(name)) return typedTarget.m_bodyCapacity;
            if ("m_bodyCount".equals(name)) return typedTarget.m_bodyCount;
            if ("m_contactCapacity".equals(name)) return typedTarget.m_contactCapacity;
            if ("m_contactCount".equals(name)) return typedTarget.m_contactCount;
            if ("m_contacts".equals(name)) return typedTarget.m_contacts;
            if ("m_jointCapacity".equals(name)) return typedTarget.m_jointCapacity;
            if ("m_jointCount".equals(name)) return typedTarget.m_jointCount;
            if ("m_joints".equals(name)) return typedTarget.m_joints;
            if ("m_listener".equals(name)) return typedTarget.m_listener;
            if ("m_positions".equals(name)) return typedTarget.m_positions;
            if ("m_velocities".equals(name)) return typedTarget.m_velocities;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Profile) {
            com.codename1.gaming.physics.box2d.dynamics.Profile typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Profile) target;
            if ("broadphase".equals(name)) return typedTarget.broadphase;
            if ("collide".equals(name)) return typedTarget.collide;
            if ("solve".equals(name)) return typedTarget.solve;
            if ("solveInit".equals(name)) return typedTarget.solveInit;
            if ("solvePosition".equals(name)) return typedTarget.solvePosition;
            if ("solveTOI".equals(name)) return typedTarget.solveTOI;
            if ("solveVelocity".equals(name)) return typedTarget.solveVelocity;
            if ("step".equals(name)) return typedTarget.step;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.SolverData) {
            com.codename1.gaming.physics.box2d.dynamics.SolverData typedTarget = (com.codename1.gaming.physics.box2d.dynamics.SolverData) target;
            if ("positions".equals(name)) return typedTarget.positions;
            if ("step".equals(name)) return typedTarget.step;
            if ("velocities".equals(name)) return typedTarget.velocities;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.TimeStep) {
            com.codename1.gaming.physics.box2d.dynamics.TimeStep typedTarget = (com.codename1.gaming.physics.box2d.dynamics.TimeStep) target;
            if ("dt".equals(name)) return typedTarget.dt;
            if ("dtRatio".equals(name)) return typedTarget.dtRatio;
            if ("inv_dt".equals(name)) return typedTarget.inv_dt;
            if ("positionIterations".equals(name)) return typedTarget.positionIterations;
            if ("velocityIterations".equals(name)) return typedTarget.velocityIterations;
            if ("warmStarting".equals(name)) return typedTarget.warmStarting;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.World) {
            com.codename1.gaming.physics.box2d.dynamics.World typedTarget = (com.codename1.gaming.physics.box2d.dynamics.World) target;
            if ("activeContacts".equals(name)) return typedTarget.activeContacts;
            if ("contactPoolCount".equals(name)) return typedTarget.contactPoolCount;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Body) {
            com.codename1.gaming.physics.box2d.dynamics.Body typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Body) target;
            if ("m_I".equals(name)) {
                typedTarget.m_I = ((Number) value).floatValue();
                return;
            }
            if ("m_angularDamping".equals(name)) {
                typedTarget.m_angularDamping = ((Number) value).floatValue();
                return;
            }
            if ("m_angularVelocity".equals(name)) {
                typedTarget.m_angularVelocity = ((Number) value).floatValue();
                return;
            }
            if ("m_contactList".equals(name)) {
                typedTarget.m_contactList = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_fixtureCount".equals(name)) {
                typedTarget.m_fixtureCount = toIntValue(value);
                return;
            }
            if ("m_fixtureList".equals(name)) {
                typedTarget.m_fixtureList = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_gravityScale".equals(name)) {
                typedTarget.m_gravityScale = ((Number) value).floatValue();
                return;
            }
            if ("m_invI".equals(name)) {
                typedTarget.m_invI = ((Number) value).floatValue();
                return;
            }
            if ("m_invMass".equals(name)) {
                typedTarget.m_invMass = ((Number) value).floatValue();
                return;
            }
            if ("m_islandIndex".equals(name)) {
                typedTarget.m_islandIndex = toIntValue(value);
                return;
            }
            if ("m_jointList".equals(name)) {
                typedTarget.m_jointList = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_linearDamping".equals(name)) {
                typedTarget.m_linearDamping = ((Number) value).floatValue();
                return;
            }
            if ("m_mass".equals(name)) {
                typedTarget.m_mass = ((Number) value).floatValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("m_sleepTime".equals(name)) {
                typedTarget.m_sleepTime = ((Number) value).floatValue();
                return;
            }
            if ("m_torque".equals(name)) {
                typedTarget.m_torque = ((Number) value).floatValue();
                return;
            }
            if ("m_type".equals(name)) {
                typedTarget.m_type = (com.codename1.gaming.physics.box2d.dynamics.BodyType) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
            if ("m_world".equals(name)) {
                typedTarget.m_world = (com.codename1.gaming.physics.box2d.dynamics.World) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.BodyDef) {
            com.codename1.gaming.physics.box2d.dynamics.BodyDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.BodyDef) target;
            if ("active".equals(name)) {
                typedTarget.active = ((Boolean) value).booleanValue();
                return;
            }
            if ("allowSleep".equals(name)) {
                typedTarget.allowSleep = ((Boolean) value).booleanValue();
                return;
            }
            if ("angle".equals(name)) {
                typedTarget.angle = ((Number) value).floatValue();
                return;
            }
            if ("angularDamping".equals(name)) {
                typedTarget.angularDamping = ((Number) value).floatValue();
                return;
            }
            if ("angularVelocity".equals(name)) {
                typedTarget.angularVelocity = ((Number) value).floatValue();
                return;
            }
            if ("awake".equals(name)) {
                typedTarget.awake = ((Boolean) value).booleanValue();
                return;
            }
            if ("bullet".equals(name)) {
                typedTarget.bullet = ((Boolean) value).booleanValue();
                return;
            }
            if ("fixedRotation".equals(name)) {
                typedTarget.fixedRotation = ((Boolean) value).booleanValue();
                return;
            }
            if ("gravityScale".equals(name)) {
                typedTarget.gravityScale = ((Number) value).floatValue();
                return;
            }
            if ("linearDamping".equals(name)) {
                typedTarget.linearDamping = ((Number) value).floatValue();
                return;
            }
            if ("linearVelocity".equals(name)) {
                typedTarget.linearVelocity = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("position".equals(name)) {
                typedTarget.position = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.BodyType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.ContactManager) {
            com.codename1.gaming.physics.box2d.dynamics.ContactManager typedTarget = (com.codename1.gaming.physics.box2d.dynamics.ContactManager) target;
            if ("m_broadPhase".equals(name)) {
                typedTarget.m_broadPhase = (com.codename1.gaming.physics.box2d.collision.broadphase.BroadPhase) value;
                return;
            }
            if ("m_contactCount".equals(name)) {
                typedTarget.m_contactCount = toIntValue(value);
                return;
            }
            if ("m_contactFilter".equals(name)) {
                typedTarget.m_contactFilter = (com.codename1.gaming.physics.box2d.callbacks.ContactFilter) value;
                return;
            }
            if ("m_contactList".equals(name)) {
                typedTarget.m_contactList = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_contactListener".equals(name)) {
                typedTarget.m_contactListener = (com.codename1.gaming.physics.box2d.callbacks.ContactListener) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Filter) {
            com.codename1.gaming.physics.box2d.dynamics.Filter typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Filter) target;
            if ("categoryBits".equals(name)) {
                typedTarget.categoryBits = toIntValue(value);
                return;
            }
            if ("groupIndex".equals(name)) {
                typedTarget.groupIndex = toIntValue(value);
                return;
            }
            if ("maskBits".equals(name)) {
                typedTarget.maskBits = toIntValue(value);
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Fixture) {
            com.codename1.gaming.physics.box2d.dynamics.Fixture typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Fixture) target;
            if ("m_body".equals(name)) {
                typedTarget.m_body = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("m_density".equals(name)) {
                typedTarget.m_density = ((Number) value).floatValue();
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_isSensor".equals(name)) {
                typedTarget.m_isSensor = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_proxies".equals(name)) {
                typedTarget.m_proxies = (com.codename1.gaming.physics.box2d.dynamics.FixtureProxy[]) value;
                return;
            }
            if ("m_proxyCount".equals(name)) {
                typedTarget.m_proxyCount = toIntValue(value);
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_shape".equals(name)) {
                typedTarget.m_shape = (com.codename1.gaming.physics.box2d.collision.shapes.Shape) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.FixtureDef) {
            com.codename1.gaming.physics.box2d.dynamics.FixtureDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.FixtureDef) target;
            if ("density".equals(name)) {
                typedTarget.density = ((Number) value).floatValue();
                return;
            }
            if ("filter".equals(name)) {
                typedTarget.filter = (com.codename1.gaming.physics.box2d.dynamics.Filter) value;
                return;
            }
            if ("friction".equals(name)) {
                typedTarget.friction = ((Number) value).floatValue();
                return;
            }
            if ("isSensor".equals(name)) {
                typedTarget.isSensor = ((Boolean) value).booleanValue();
                return;
            }
            if ("restitution".equals(name)) {
                typedTarget.restitution = ((Number) value).floatValue();
                return;
            }
            if ("shape".equals(name)) {
                typedTarget.shape = (com.codename1.gaming.physics.box2d.collision.shapes.Shape) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Island) {
            com.codename1.gaming.physics.box2d.dynamics.Island typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Island) target;
            if ("m_bodies".equals(name)) {
                typedTarget.m_bodies = (com.codename1.gaming.physics.box2d.dynamics.Body[]) value;
                return;
            }
            if ("m_bodyCapacity".equals(name)) {
                typedTarget.m_bodyCapacity = toIntValue(value);
                return;
            }
            if ("m_bodyCount".equals(name)) {
                typedTarget.m_bodyCount = toIntValue(value);
                return;
            }
            if ("m_contactCapacity".equals(name)) {
                typedTarget.m_contactCapacity = toIntValue(value);
                return;
            }
            if ("m_contactCount".equals(name)) {
                typedTarget.m_contactCount = toIntValue(value);
                return;
            }
            if ("m_contacts".equals(name)) {
                typedTarget.m_contacts = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact[]) value;
                return;
            }
            if ("m_jointCapacity".equals(name)) {
                typedTarget.m_jointCapacity = toIntValue(value);
                return;
            }
            if ("m_jointCount".equals(name)) {
                typedTarget.m_jointCount = toIntValue(value);
                return;
            }
            if ("m_joints".equals(name)) {
                typedTarget.m_joints = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint[]) value;
                return;
            }
            if ("m_listener".equals(name)) {
                typedTarget.m_listener = (com.codename1.gaming.physics.box2d.callbacks.ContactListener) value;
                return;
            }
            if ("m_positions".equals(name)) {
                typedTarget.m_positions = (com.codename1.gaming.physics.box2d.dynamics.contacts.Position[]) value;
                return;
            }
            if ("m_velocities".equals(name)) {
                typedTarget.m_velocities = (com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity[]) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.Profile) {
            com.codename1.gaming.physics.box2d.dynamics.Profile typedTarget = (com.codename1.gaming.physics.box2d.dynamics.Profile) target;
            if ("broadphase".equals(name)) {
                typedTarget.broadphase = ((Number) value).floatValue();
                return;
            }
            if ("collide".equals(name)) {
                typedTarget.collide = ((Number) value).floatValue();
                return;
            }
            if ("solve".equals(name)) {
                typedTarget.solve = ((Number) value).floatValue();
                return;
            }
            if ("solveInit".equals(name)) {
                typedTarget.solveInit = ((Number) value).floatValue();
                return;
            }
            if ("solvePosition".equals(name)) {
                typedTarget.solvePosition = ((Number) value).floatValue();
                return;
            }
            if ("solveTOI".equals(name)) {
                typedTarget.solveTOI = ((Number) value).floatValue();
                return;
            }
            if ("solveVelocity".equals(name)) {
                typedTarget.solveVelocity = ((Number) value).floatValue();
                return;
            }
            if ("step".equals(name)) {
                typedTarget.step = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.SolverData) {
            com.codename1.gaming.physics.box2d.dynamics.SolverData typedTarget = (com.codename1.gaming.physics.box2d.dynamics.SolverData) target;
            if ("positions".equals(name)) {
                typedTarget.positions = (com.codename1.gaming.physics.box2d.dynamics.contacts.Position[]) value;
                return;
            }
            if ("step".equals(name)) {
                typedTarget.step = (com.codename1.gaming.physics.box2d.dynamics.TimeStep) value;
                return;
            }
            if ("velocities".equals(name)) {
                typedTarget.velocities = (com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity[]) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.TimeStep) {
            com.codename1.gaming.physics.box2d.dynamics.TimeStep typedTarget = (com.codename1.gaming.physics.box2d.dynamics.TimeStep) target;
            if ("dt".equals(name)) {
                typedTarget.dt = ((Number) value).floatValue();
                return;
            }
            if ("dtRatio".equals(name)) {
                typedTarget.dtRatio = ((Number) value).floatValue();
                return;
            }
            if ("inv_dt".equals(name)) {
                typedTarget.inv_dt = ((Number) value).floatValue();
                return;
            }
            if ("positionIterations".equals(name)) {
                typedTarget.positionIterations = toIntValue(value);
                return;
            }
            if ("velocityIterations".equals(name)) {
                typedTarget.velocityIterations = toIntValue(value);
                return;
            }
            if ("warmStarting".equals(name)) {
                typedTarget.warmStarting = ((Boolean) value).booleanValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.World) {
            com.codename1.gaming.physics.box2d.dynamics.World typedTarget = (com.codename1.gaming.physics.box2d.dynamics.World) target;
            if ("activeContacts".equals(name)) {
                typedTarget.activeContacts = toIntValue(value);
                return;
            }
            if ("contactPoolCount".equals(name)) {
                typedTarget.contactPoolCount = toIntValue(value);
                return;
            }
        }
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
