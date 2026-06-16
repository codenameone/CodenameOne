package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics_box2d_dynamics_contacts {
    private GeneratedAccess_com_codename1_gaming_physics_box2d_dynamics_contacts() {
    }

    public static Class<?> findClass(String name) {
        int lastDot = name == null ? -1 : name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(lastDot + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("ChainAndCircleContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.class;
        }
        if ("ChainAndPolygonContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.class;
        }
        if ("CircleContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.class;
        }
        if ("Contact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class;
        }
        if ("ContactCreator".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactCreator.class;
        }
        if ("ContactEdge".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge.class;
        }
        if ("ContactPositionConstraint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactPositionConstraint.class;
        }
        if ("ContactRegister".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister.class;
        }
        if ("ContactSolver".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.class;
        }
        if ("ContactSolverDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef.class;
        }
        if ("ContactVelocityConstraint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.class;
        }
        if ("VelocityConstraintPoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint.class;
        }
        if ("EdgeAndCircleContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.class;
        }
        if ("EdgeAndPolygonContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.class;
        }
        if ("PolygonAndCircleContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.class;
        }
        if ("PolygonContact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.class;
        }
        if ("Position".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.Position.class;
        }
        if ("Velocity".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ContactPositionConstraint.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.ContactPositionConstraint();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("mixFriction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.mixFriction(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("mixRestitution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.mixRestitution(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact) {
            try {
                return invoke0((com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact) {
            try {
                return invoke1((com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact) {
            try {
                return invoke2((com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact) {
            try {
                return invoke3((com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact) {
            try {
                return invoke4((com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact) {
            try {
                return invoke5((com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact) {
            try {
                return invoke6((com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) {
            try {
                return invoke7((com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver) {
            try {
                return invoke8((com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactCreator) {
            try {
                return invoke9((com.codename1.gaming.physics.box2d.dynamics.contacts.ContactCreator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("evaluate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.evaluate((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("flagForFiltering".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flagForFiltering(); return null;
            }
        }
        if ("getChildIndexA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexA();
            }
        }
        if ("getChildIndexB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIndexB();
            }
        }
        if ("getFixtureA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureA();
            }
        }
        if ("getFixtureB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixtureB();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getManifold".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManifold();
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
        if ("getTangentSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTangentSpeed();
            }
        }
        if ("getWorldManifold".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.WorldManifold.class}, false);
                typedTarget.getWorldManifold((com.codename1.gaming.physics.box2d.collision.WorldManifold) adaptedArgs[0]); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, java.lang.Integer.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        if ("resetFriction".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetFriction(); return null;
            }
        }
        if ("resetRestitution".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resetRestitution(); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
        if ("setTangentSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTangentSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.callbacks.ContactListener.class}, false);
                typedTarget.update((com.codename1.gaming.physics.box2d.callbacks.ContactListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef.class}, false);
                typedTarget.init((com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef) adaptedArgs[0]); return null;
            }
        }
        if ("initializeVelocityConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initializeVelocityConstraints(); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.solvePositionConstraints();
            }
        }
        if ("solveTOIPositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.solveTOIPositionConstraints(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.solveVelocityConstraints(); return null;
            }
        }
        if ("storeImpulses".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.storeImpulses(); return null;
            }
        }
        if ("warmStart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.warmStart(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.gaming.physics.box2d.dynamics.contacts.ContactCreator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contactCreateFcn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class, com.codename1.gaming.physics.box2d.dynamics.Fixture.class}, false);
                return typedTarget.contactCreateFcn((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[1], (com.codename1.gaming.physics.box2d.dynamics.Fixture) adaptedArgs[2]);
            }
        }
        if ("contactDestroyFcn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class, com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class, com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class}, false);
                typedTarget.contactDestroyFcn((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.class) return getStaticField0(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.class) return getStaticField1(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.class) return getStaticField2(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class) return getStaticField3(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.class) return getStaticField4(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.class) return getStaticField5(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.class) return getStaticField6(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.class) return getStaticField7(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.class) return getStaticField8(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("DEBUG_SOLVER".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.DEBUG_SOLVER;
        if ("INITIAL_NUM_CONSTRAINTS".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.INITIAL_NUM_CONSTRAINTS;
        if ("k_errorTol".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.k_errorTol;
        if ("k_maxConditionNumber".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.k_maxConditionNumber;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("BULLET_HIT_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.BULLET_HIT_FLAG;
        if ("ENABLED_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.ENABLED_FLAG;
        if ("FILTER_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.FILTER_FLAG;
        if ("ISLAND_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.ISLAND_FLAG;
        if ("TOI_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.TOI_FLAG;
        if ("TOUCHING_FLAG".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.TOUCHING_FLAG;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.Contact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) target;
            if ("m_fixtureA".equals(name)) return typedTarget.m_fixtureA;
            if ("m_fixtureB".equals(name)) return typedTarget.m_fixtureB;
            if ("m_flags".equals(name)) return typedTarget.m_flags;
            if ("m_friction".equals(name)) return typedTarget.m_friction;
            if ("m_indexA".equals(name)) return typedTarget.m_indexA;
            if ("m_indexB".equals(name)) return typedTarget.m_indexB;
            if ("m_manifold".equals(name)) return typedTarget.m_manifold;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_nodeA".equals(name)) return typedTarget.m_nodeA;
            if ("m_nodeB".equals(name)) return typedTarget.m_nodeB;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_restitution".equals(name)) return typedTarget.m_restitution;
            if ("m_tangentSpeed".equals(name)) return typedTarget.m_tangentSpeed;
            if ("m_toi".equals(name)) return typedTarget.m_toi;
            if ("m_toiCount".equals(name)) return typedTarget.m_toiCount;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) target;
            if ("contact".equals(name)) return typedTarget.contact;
            if ("next".equals(name)) return typedTarget.next;
            if ("other".equals(name)) return typedTarget.other;
            if ("prev".equals(name)) return typedTarget.prev;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister) target;
            if ("creator".equals(name)) return typedTarget.creator;
            if ("primary".equals(name)) return typedTarget.primary;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver) target;
            if ("m_contacts".equals(name)) return typedTarget.m_contacts;
            if ("m_count".equals(name)) return typedTarget.m_count;
            if ("m_positionConstraints".equals(name)) return typedTarget.m_positionConstraints;
            if ("m_positions".equals(name)) return typedTarget.m_positions;
            if ("m_step".equals(name)) return typedTarget.m_step;
            if ("m_velocities".equals(name)) return typedTarget.m_velocities;
            if ("m_velocityConstraints".equals(name)) return typedTarget.m_velocityConstraints;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef) target;
            if ("contacts".equals(name)) return typedTarget.contacts;
            if ("count".equals(name)) return typedTarget.count;
            if ("positions".equals(name)) return typedTarget.positions;
            if ("step".equals(name)) return typedTarget.step;
            if ("velocities".equals(name)) return typedTarget.velocities;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint) target;
            if ("K".equals(name)) return typedTarget.K;
            if ("contactIndex".equals(name)) return typedTarget.contactIndex;
            if ("friction".equals(name)) return typedTarget.friction;
            if ("indexA".equals(name)) return typedTarget.indexA;
            if ("indexB".equals(name)) return typedTarget.indexB;
            if ("invIA".equals(name)) return typedTarget.invIA;
            if ("invIB".equals(name)) return typedTarget.invIB;
            if ("invMassA".equals(name)) return typedTarget.invMassA;
            if ("invMassB".equals(name)) return typedTarget.invMassB;
            if ("normal".equals(name)) return typedTarget.normal;
            if ("normalMass".equals(name)) return typedTarget.normalMass;
            if ("pointCount".equals(name)) return typedTarget.pointCount;
            if ("points".equals(name)) return typedTarget.points;
            if ("restitution".equals(name)) return typedTarget.restitution;
            if ("tangentSpeed".equals(name)) return typedTarget.tangentSpeed;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint) target;
            if ("normalImpulse".equals(name)) return typedTarget.normalImpulse;
            if ("normalMass".equals(name)) return typedTarget.normalMass;
            if ("rA".equals(name)) return typedTarget.rA;
            if ("rB".equals(name)) return typedTarget.rB;
            if ("tangentImpulse".equals(name)) return typedTarget.tangentImpulse;
            if ("tangentMass".equals(name)) return typedTarget.tangentMass;
            if ("velocityBias".equals(name)) return typedTarget.velocityBias;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Position) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.Position typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.Position) target;
            if ("a".equals(name)) return typedTarget.a;
            if ("c".equals(name)) return typedTarget.c;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity) target;
            if ("v".equals(name)) return typedTarget.v;
            if ("w".equals(name)) return typedTarget.w;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndCircleContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ChainAndPolygonContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.CircleContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndCircleContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.EdgeAndPolygonContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonAndCircleContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.PolygonContact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.Contact typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) target;
            if ("m_fixtureA".equals(name)) {
                typedTarget.m_fixtureA = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_fixtureB".equals(name)) {
                typedTarget.m_fixtureB = (com.codename1.gaming.physics.box2d.dynamics.Fixture) value;
                return;
            }
            if ("m_flags".equals(name)) {
                typedTarget.m_flags = toIntValue(value);
                return;
            }
            if ("m_friction".equals(name)) {
                typedTarget.m_friction = ((Number) value).floatValue();
                return;
            }
            if ("m_indexA".equals(name)) {
                typedTarget.m_indexA = toIntValue(value);
                return;
            }
            if ("m_indexB".equals(name)) {
                typedTarget.m_indexB = toIntValue(value);
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_nodeA".equals(name)) {
                typedTarget.m_nodeA = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_nodeB".equals(name)) {
                typedTarget.m_nodeB = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("m_restitution".equals(name)) {
                typedTarget.m_restitution = ((Number) value).floatValue();
                return;
            }
            if ("m_tangentSpeed".equals(name)) {
                typedTarget.m_tangentSpeed = ((Number) value).floatValue();
                return;
            }
            if ("m_toi".equals(name)) {
                typedTarget.m_toi = ((Number) value).floatValue();
                return;
            }
            if ("m_toiCount".equals(name)) {
                typedTarget.m_toiCount = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) target;
            if ("contact".equals(name)) {
                typedTarget.contact = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact) value;
                return;
            }
            if ("next".equals(name)) {
                typedTarget.next = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
            if ("other".equals(name)) {
                typedTarget.other = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("prev".equals(name)) {
                typedTarget.prev = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactEdge) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactRegister) target;
            if ("creator".equals(name)) {
                typedTarget.creator = (com.codename1.gaming.physics.box2d.pooling.IDynamicStack) value;
                return;
            }
            if ("primary".equals(name)) {
                typedTarget.primary = ((Boolean) value).booleanValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver) target;
            if ("m_contacts".equals(name)) {
                typedTarget.m_contacts = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact[]) value;
                return;
            }
            if ("m_count".equals(name)) {
                typedTarget.m_count = toIntValue(value);
                return;
            }
            if ("m_positionConstraints".equals(name)) {
                typedTarget.m_positionConstraints = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactPositionConstraint[]) value;
                return;
            }
            if ("m_positions".equals(name)) {
                typedTarget.m_positions = (com.codename1.gaming.physics.box2d.dynamics.contacts.Position[]) value;
                return;
            }
            if ("m_step".equals(name)) {
                typedTarget.m_step = (com.codename1.gaming.physics.box2d.dynamics.TimeStep) value;
                return;
            }
            if ("m_velocities".equals(name)) {
                typedTarget.m_velocities = (com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity[]) value;
                return;
            }
            if ("m_velocityConstraints".equals(name)) {
                typedTarget.m_velocityConstraints = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint[]) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactSolver.ContactSolverDef) target;
            if ("contacts".equals(name)) {
                typedTarget.contacts = (com.codename1.gaming.physics.box2d.dynamics.contacts.Contact[]) value;
                return;
            }
            if ("count".equals(name)) {
                typedTarget.count = toIntValue(value);
                return;
            }
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
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint) target;
            if ("contactIndex".equals(name)) {
                typedTarget.contactIndex = toIntValue(value);
                return;
            }
            if ("friction".equals(name)) {
                typedTarget.friction = ((Number) value).floatValue();
                return;
            }
            if ("indexA".equals(name)) {
                typedTarget.indexA = toIntValue(value);
                return;
            }
            if ("indexB".equals(name)) {
                typedTarget.indexB = toIntValue(value);
                return;
            }
            if ("invIA".equals(name)) {
                typedTarget.invIA = ((Number) value).floatValue();
                return;
            }
            if ("invIB".equals(name)) {
                typedTarget.invIB = ((Number) value).floatValue();
                return;
            }
            if ("invMassA".equals(name)) {
                typedTarget.invMassA = ((Number) value).floatValue();
                return;
            }
            if ("invMassB".equals(name)) {
                typedTarget.invMassB = ((Number) value).floatValue();
                return;
            }
            if ("pointCount".equals(name)) {
                typedTarget.pointCount = toIntValue(value);
                return;
            }
            if ("points".equals(name)) {
                typedTarget.points = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint[]) value;
                return;
            }
            if ("restitution".equals(name)) {
                typedTarget.restitution = ((Number) value).floatValue();
                return;
            }
            if ("tangentSpeed".equals(name)) {
                typedTarget.tangentSpeed = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint) target;
            if ("normalImpulse".equals(name)) {
                typedTarget.normalImpulse = ((Number) value).floatValue();
                return;
            }
            if ("normalMass".equals(name)) {
                typedTarget.normalMass = ((Number) value).floatValue();
                return;
            }
            if ("tangentImpulse".equals(name)) {
                typedTarget.tangentImpulse = ((Number) value).floatValue();
                return;
            }
            if ("tangentMass".equals(name)) {
                typedTarget.tangentMass = ((Number) value).floatValue();
                return;
            }
            if ("velocityBias".equals(name)) {
                typedTarget.velocityBias = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Position) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.Position typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.Position) target;
            if ("a".equals(name)) {
                typedTarget.a = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity) {
            com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity typedTarget = (com.codename1.gaming.physics.box2d.dynamics.contacts.Velocity) target;
            if ("w".equals(name)) {
                typedTarget.w = ((Number) value).floatValue();
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
