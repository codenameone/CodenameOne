package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics {
    private GeneratedAccess_com_codename1_gaming_physics() {
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
        if ("BodyType".equals(simpleName)) {
            return com.codename1.gaming.physics.BodyType.class;
        }
        if ("ContactListener".equals(simpleName)) {
            return com.codename1.gaming.physics.ContactListener.class;
        }
        if ("PhysicsBody".equals(simpleName)) {
            return com.codename1.gaming.physics.PhysicsBody.class;
        }
        if ("PhysicsContact".equals(simpleName)) {
            return com.codename1.gaming.physics.PhysicsContact.class;
        }
        if ("PhysicsJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.PhysicsJoint.class;
        }
        if ("PhysicsLinkable".equals(simpleName)) {
            return com.codename1.gaming.physics.PhysicsLinkable.class;
        }
        if ("PhysicsWorld".equals(simpleName)) {
            return com.codename1.gaming.physics.PhysicsWorld.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.PhysicsWorld.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.physics.PhysicsWorld(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
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
        if (target instanceof com.codename1.gaming.physics.PhysicsBody) {
            try {
                return invoke0((com.codename1.gaming.physics.PhysicsBody) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.PhysicsContact) {
            try {
                return invoke1((com.codename1.gaming.physics.PhysicsContact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.PhysicsJoint) {
            try {
                return invoke2((com.codename1.gaming.physics.PhysicsJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.PhysicsWorld) {
            try {
                return invoke3((com.codename1.gaming.physics.PhysicsWorld) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.ContactListener) {
            try {
                return invoke4((com.codename1.gaming.physics.ContactListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.PhysicsLinkable) {
            try {
                return invoke5((com.codename1.gaming.physics.PhysicsLinkable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.PhysicsBody typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("applyForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.applyForce(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.applyForce(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("applyLinearImpulse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.applyLinearImpulse(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("applyTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.applyTorque(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("getAngularVelocity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngularVelocity();
            }
        }
        if ("getLinearVelocityX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinearVelocityX();
            }
        }
        if ("getLinearVelocityY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinearVelocityY();
            }
        }
        if ("getLinkedSprite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkedSprite();
            }
        }
        if ("getNativeBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeBody();
            }
        }
        if ("getRotation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRotation();
            }
        }
        if ("getUserData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserData();
            }
        }
        if ("getX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getY();
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
        if ("setBullet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBullet(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDensity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setDensity(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setFixedRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFixedRotation(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFriction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setFriction(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLinearDamping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLinearDamping(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLinearVelocity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setLinearVelocity(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setLinkedSprite".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setLinkedSprite((java.lang.Object) adaptedArgs[0]); return null;
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
        if ("setTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransform(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.PhysicsContact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBodyA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyA();
            }
        }
        if ("getBodyB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyB();
            }
        }
        if ("getSpriteA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpriteA();
            }
        }
        if ("getSpriteB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpriteB();
            }
        }
        if ("isTouching".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouching();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.PhysicsJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destroy".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destroy(); return null;
            }
        }
        if ("getBodyA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyA();
            }
        }
        if ("getBodyB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyB();
            }
        }
        if ("getNativeJoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeJoint();
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTarget(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.PhysicsWorld typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addContactListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.ContactListener.class}, false);
                typedTarget.addContactListener((com.codename1.gaming.physics.ContactListener) adaptedArgs[0]); return null;
            }
        }
        if ("createBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.gaming.physics.BodyType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.gaming.physics.BodyType.class}, false);
                return typedTarget.createBox(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.gaming.physics.BodyType) adaptedArgs[4]);
            }
        }
        if ("createCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.gaming.physics.BodyType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.gaming.physics.BodyType.class}, false);
                return typedTarget.createCircle(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), (com.codename1.gaming.physics.BodyType) adaptedArgs[3]);
            }
        }
        if ("createDistanceJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.createDistanceJoint((com.codename1.gaming.physics.PhysicsBody) adaptedArgs[0], (com.codename1.gaming.physics.PhysicsBody) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue(), ((Number) adaptedArgs[7]).floatValue());
            }
        }
        if ("createMouseJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.createMouseJoint((com.codename1.gaming.physics.PhysicsBody) adaptedArgs[0], (com.codename1.gaming.physics.PhysicsBody) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue());
            }
        }
        if ("createPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, float[].class, com.codename1.gaming.physics.BodyType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, float[].class, com.codename1.gaming.physics.BodyType.class}, false);
                return typedTarget.createPolygon(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), (float[]) adaptedArgs[2], (com.codename1.gaming.physics.BodyType) adaptedArgs[3]);
            }
        }
        if ("createPrismaticJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.createPrismaticJoint((com.codename1.gaming.physics.PhysicsBody) adaptedArgs[0], (com.codename1.gaming.physics.PhysicsBody) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue());
            }
        }
        if ("createRevoluteJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.createRevoluteJoint((com.codename1.gaming.physics.PhysicsBody) adaptedArgs[0], (com.codename1.gaming.physics.PhysicsBody) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("createShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, com.codename1.ui.geom.Shape.class, com.codename1.gaming.physics.BodyType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, com.codename1.ui.geom.Shape.class, com.codename1.gaming.physics.BodyType.class}, false);
                return typedTarget.createShape(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), (com.codename1.ui.geom.Shape) adaptedArgs[2], (com.codename1.gaming.physics.BodyType) adaptedArgs[3]);
            }
        }
        if ("createWeldJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class, com.codename1.gaming.physics.PhysicsBody.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.createWeldJoint((com.codename1.gaming.physics.PhysicsBody) adaptedArgs[0], (com.codename1.gaming.physics.PhysicsBody) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("debugDraw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.debugDraw((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("destroyJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsJoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsJoint.class}, false);
                typedTarget.destroyJoint((com.codename1.gaming.physics.PhysicsJoint) adaptedArgs[0]); return null;
            }
        }
        if ("getNativeWorld".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeWorld();
            }
        }
        if ("getPixelsPerMeter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPixelsPerMeter();
            }
        }
        if ("removeBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsBody.class}, false);
                typedTarget.removeBody((com.codename1.gaming.physics.PhysicsBody) adaptedArgs[0]); return null;
            }
        }
        if ("removeContactListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.ContactListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.ContactListener.class}, false);
                typedTarget.removeContactListener((com.codename1.gaming.physics.ContactListener) adaptedArgs[0]); return null;
            }
        }
        if ("setDebugDrawFlags".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setDebugDrawFlags(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("setDebugFillAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDebugFillAlpha(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setGravity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setGravity(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setPixelsPerMeter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPixelsPerMeter(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setPositionIterations".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPositionIterations(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setVelocityIterations".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setVelocityIterations(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("step".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.step(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("syncSprites".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.syncSprites(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.ContactListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("beginContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsContact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsContact.class}, false);
                typedTarget.beginContact((com.codename1.gaming.physics.PhysicsContact) adaptedArgs[0]); return null;
            }
        }
        if ("endContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsContact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.PhysicsContact.class}, false);
                typedTarget.endContact((com.codename1.gaming.physics.PhysicsContact) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.PhysicsLinkable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("setPhysicsPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setPhysicsPosition(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setPhysicsRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPhysicsRotation(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.BodyType.class) return getStaticField0(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DYNAMIC".equals(name)) return com.codename1.gaming.physics.BodyType.DYNAMIC;
        if ("KINEMATIC".equals(name)) return com.codename1.gaming.physics.BodyType.KINEMATIC;
        if ("STATIC".equals(name)) return com.codename1.gaming.physics.BodyType.STATIC;
        throw unsupportedStaticField(com.codename1.gaming.physics.BodyType.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
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
