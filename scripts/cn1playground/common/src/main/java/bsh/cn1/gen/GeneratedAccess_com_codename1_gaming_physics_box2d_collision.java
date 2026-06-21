package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics_box2d_collision {
    private GeneratedAccess_com_codename1_gaming_physics_box2d_collision() {
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
        if ("AABB".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.AABB.class;
        }
        if ("Collision".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Collision.class;
        }
        if ("ClipVertex".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex.class;
        }
        if ("PointState".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Collision.PointState.class;
        }
        if ("ContactID".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.ContactID.class;
        }
        if ("Type".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.ContactID.Type.class;
        }
        if ("Distance".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Distance.class;
        }
        if ("DistanceProxy".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy.class;
        }
        if ("SimplexCache".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache.class;
        }
        if ("DistanceInput".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.DistanceInput.class;
        }
        if ("DistanceOutput".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.DistanceOutput.class;
        }
        if ("Manifold".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Manifold.class;
        }
        if ("ManifoldType".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType.class;
        }
        if ("ManifoldPoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.ManifoldPoint.class;
        }
        if ("RayCastInput".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.RayCastInput.class;
        }
        if ("RayCastOutput".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.RayCastOutput.class;
        }
        if ("TimeOfImpact".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.class;
        }
        if ("TOIInput".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput.class;
        }
        if ("TOIOutput".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput.class;
        }
        if ("TOIOutputState".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.class;
        }
        if ("WorldManifold".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.WorldManifold.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.collision.AABB.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.AABB();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.AABB((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.AABB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.Collision.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.Collision((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.ContactID.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.ContactID();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.ContactID((com.codename1.gaming.physics.box2d.collision.ContactID) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.Distance.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.Distance();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.DistanceInput.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.DistanceInput();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.DistanceOutput.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.DistanceOutput();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.Manifold.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.Manifold();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.Manifold((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.ManifoldPoint.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.ManifoldPoint();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ManifoldPoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ManifoldPoint.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.ManifoldPoint((com.codename1.gaming.physics.box2d.collision.ManifoldPoint) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.RayCastInput.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.RayCastInput();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.RayCastOutput.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.RayCastOutput();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.TimeOfImpact.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.TimeOfImpact((com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.WorldManifold.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.WorldManifold();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.collision.AABB.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.collision.Collision.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("testOverlap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                return com.codename1.gaming.physics.box2d.collision.AABB.testOverlap((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.collision.AABB.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("clipSegmentToLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[].class, com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[].class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[].class, com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[].class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.collision.Collision.clipSegmentToLine((com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[]) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[]) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]));
            }
        }
        if ("getPointStates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.PointState[].class, com.codename1.gaming.physics.box2d.collision.Collision.PointState[].class, com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.Manifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.PointState[].class, com.codename1.gaming.physics.box2d.collision.Collision.PointState[].class, com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.Manifold.class}, false);
                com.codename1.gaming.physics.box2d.collision.Collision.getPointStates((com.codename1.gaming.physics.box2d.collision.Collision.PointState[]) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.Collision.PointState[]) adaptedArgs[1], (com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[2], (com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.collision.Collision.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.gaming.physics.box2d.collision.AABB) {
            try {
                return invoke0((com.codename1.gaming.physics.box2d.collision.AABB) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Collision) {
            try {
                return invoke1((com.codename1.gaming.physics.box2d.collision.Collision) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex) {
            try {
                return invoke2((com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.ContactID) {
            try {
                return invoke3((com.codename1.gaming.physics.box2d.collision.ContactID) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance) {
            try {
                return invoke4((com.codename1.gaming.physics.box2d.collision.Distance) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) {
            try {
                return invoke5((com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) {
            try {
                return invoke6((com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Manifold) {
            try {
                return invoke7((com.codename1.gaming.physics.box2d.collision.Manifold) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.ManifoldPoint) {
            try {
                return invoke8((com.codename1.gaming.physics.box2d.collision.ManifoldPoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.RayCastInput) {
            try {
                return invoke9((com.codename1.gaming.physics.box2d.collision.RayCastInput) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.RayCastOutput) {
            try {
                return invoke10((com.codename1.gaming.physics.box2d.collision.RayCastOutput) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.TimeOfImpact) {
            try {
                return invoke11((com.codename1.gaming.physics.box2d.collision.TimeOfImpact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.WorldManifold) {
            try {
                return invoke12((com.codename1.gaming.physics.box2d.collision.WorldManifold) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.box2d.collision.AABB typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                typedTarget.combine((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                typedTarget.combine((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[1]); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                return typedTarget.contains((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0]);
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getCenterToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getCenterToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getExtents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtents();
            }
        }
        if ("getExtentsToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getExtentsToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getPerimeter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPerimeter();
            }
        }
        if ("getVertices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class}, false);
                typedTarget.getVertices((com.codename1.gaming.physics.box2d.common.Vec2[]) adaptedArgs[0]); return null;
            }
        }
        if ("isValid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValid();
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.pooling.IWorldPool.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], (com.codename1.gaming.physics.box2d.pooling.IWorldPool) adaptedArgs[2]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.box2d.collision.Collision typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("collideCircles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.collideCircles((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], (com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4]); return null;
            }
        }
        if ("collideEdgeAndCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.collideEdgeAndCircle((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], (com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4]); return null;
            }
        }
        if ("collideEdgeAndPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.collideEdgeAndPolygon((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4]); return null;
            }
        }
        if ("collidePolygonAndCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.collidePolygonAndCircle((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], (com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4]); return null;
            }
        }
        if ("collidePolygons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.collidePolygons((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4]); return null;
            }
        }
        if ("edgeSeparation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return typedTarget.edgeSeparation((com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], toIntValue(adaptedArgs[2]), (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4]);
            }
        }
        if ("findIncidentEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[].class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[].class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                typedTarget.findIncidentEdge((com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex[]) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], toIntValue(adaptedArgs[3]), (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) adaptedArgs[4], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[5]); return null;
            }
        }
        if ("testOverlap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return typedTarget.testOverlap((com.codename1.gaming.physics.box2d.collision.shapes.Shape) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.collision.shapes.Shape) adaptedArgs[2], toIntValue(adaptedArgs[3]), (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[4], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[5]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.box2d.collision.ContactID typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false);
                return typedTarget.compareTo((com.codename1.gaming.physics.box2d.collision.ContactID) adaptedArgs[0]);
            }
        }
        if ("flip".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flip(); return null;
            }
        }
        if ("getKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKey();
            }
        }
        if ("isEqual".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false);
                return typedTarget.isEqual((com.codename1.gaming.physics.box2d.collision.ContactID) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ContactID.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.ContactID) adaptedArgs[0]); return null;
            }
        }
        if ("zero".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.zero(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.box2d.collision.Distance typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("distance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.DistanceOutput.class, com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache.class, com.codename1.gaming.physics.box2d.collision.DistanceInput.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.DistanceOutput.class, com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache.class, com.codename1.gaming.physics.box2d.collision.DistanceInput.class}, false);
                typedTarget.distance((com.codename1.gaming.physics.box2d.collision.DistanceOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) adaptedArgs[1], (com.codename1.gaming.physics.box2d.collision.DistanceInput) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getSupport".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getSupport((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getSupportVertex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.getSupportVertex((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("getVertex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getVertex(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getVertexCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVertexCount();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.Shape.class, java.lang.Integer.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.shapes.Shape) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.gaming.physics.box2d.collision.Manifold typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.gaming.physics.box2d.collision.ManifoldPoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ManifoldPoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.ManifoldPoint.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.ManifoldPoint) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.gaming.physics.box2d.collision.RayCastInput typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastInput.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastInput.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.gaming.physics.box2d.collision.RayCastOutput typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.gaming.physics.box2d.collision.TimeOfImpact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("timeOfImpact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput.class, com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput.class, com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput.class}, false);
                typedTarget.timeOfImpact((com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.gaming.physics.box2d.collision.WorldManifold typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.Manifold.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Float.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.collision.Manifold) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.collision.Collision.class) return getStaticField0(name);
        if (type == com.codename1.gaming.physics.box2d.collision.Collision.PointState.class) return getStaticField1(name);
        if (type == com.codename1.gaming.physics.box2d.collision.ContactID.Type.class) return getStaticField2(name);
        if (type == com.codename1.gaming.physics.box2d.collision.Distance.class) return getStaticField3(name);
        if (type == com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType.class) return getStaticField4(name);
        if (type == com.codename1.gaming.physics.box2d.collision.TimeOfImpact.class) return getStaticField5(name);
        if (type == com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.class) return getStaticField6(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("NULL_FEATURE".equals(name)) return com.codename1.gaming.physics.box2d.collision.Collision.NULL_FEATURE;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.Collision.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ADD_STATE".equals(name)) return com.codename1.gaming.physics.box2d.collision.Collision.PointState.ADD_STATE;
        if ("NULL_STATE".equals(name)) return com.codename1.gaming.physics.box2d.collision.Collision.PointState.NULL_STATE;
        if ("PERSIST_STATE".equals(name)) return com.codename1.gaming.physics.box2d.collision.Collision.PointState.PERSIST_STATE;
        if ("REMOVE_STATE".equals(name)) return com.codename1.gaming.physics.box2d.collision.Collision.PointState.REMOVE_STATE;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.Collision.PointState.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("FACE".equals(name)) return com.codename1.gaming.physics.box2d.collision.ContactID.Type.FACE;
        if ("VERTEX".equals(name)) return com.codename1.gaming.physics.box2d.collision.ContactID.Type.VERTEX;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.ContactID.Type.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("GJK_CALLS".equals(name)) return com.codename1.gaming.physics.box2d.collision.Distance.GJK_CALLS;
        if ("GJK_ITERS".equals(name)) return com.codename1.gaming.physics.box2d.collision.Distance.GJK_ITERS;
        if ("GJK_MAX_ITERS".equals(name)) return com.codename1.gaming.physics.box2d.collision.Distance.GJK_MAX_ITERS;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.Distance.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("CIRCLES".equals(name)) return com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType.CIRCLES;
        if ("FACE_A".equals(name)) return com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType.FACE_A;
        if ("FACE_B".equals(name)) return com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType.FACE_B;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("MAX_ITERATIONS".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.MAX_ITERATIONS;
        if ("toiCalls".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiCalls;
        if ("toiIters".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiIters;
        if ("toiMaxIters".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiMaxIters;
        if ("toiMaxRootIters".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiMaxRootIters;
        if ("toiRootIters".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiRootIters;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.TimeOfImpact.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("FAILED".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.FAILED;
        if ("OVERLAPPED".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.OVERLAPPED;
        if ("SEPARATED".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.SEPARATED;
        if ("TOUCHING".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.TOUCHING;
        if ("UNKNOWN".equals(name)) return com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.UNKNOWN;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.collision.AABB) {
            com.codename1.gaming.physics.box2d.collision.AABB typedTarget = (com.codename1.gaming.physics.box2d.collision.AABB) target;
            if ("lowerBound".equals(name)) return typedTarget.lowerBound;
            if ("upperBound".equals(name)) return typedTarget.upperBound;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex) {
            com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex typedTarget = (com.codename1.gaming.physics.box2d.collision.Collision.ClipVertex) target;
            if ("id".equals(name)) return typedTarget.id;
            if ("v".equals(name)) return typedTarget.v;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.ContactID) {
            com.codename1.gaming.physics.box2d.collision.ContactID typedTarget = (com.codename1.gaming.physics.box2d.collision.ContactID) target;
            if ("indexA".equals(name)) return typedTarget.indexA;
            if ("indexB".equals(name)) return typedTarget.indexB;
            if ("typeA".equals(name)) return typedTarget.typeA;
            if ("typeB".equals(name)) return typedTarget.typeB;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) {
            com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy typedTarget = (com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) target;
            if ("m_buffer".equals(name)) return typedTarget.m_buffer;
            if ("m_count".equals(name)) return typedTarget.m_count;
            if ("m_radius".equals(name)) return typedTarget.m_radius;
            if ("m_vertices".equals(name)) return typedTarget.m_vertices;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) {
            com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache typedTarget = (com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) target;
            if ("count".equals(name)) return typedTarget.count;
            if ("indexA".equals(name)) return typedTarget.indexA;
            if ("indexB".equals(name)) return typedTarget.indexB;
            if ("metric".equals(name)) return typedTarget.metric;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.DistanceInput) {
            com.codename1.gaming.physics.box2d.collision.DistanceInput typedTarget = (com.codename1.gaming.physics.box2d.collision.DistanceInput) target;
            if ("proxyA".equals(name)) return typedTarget.proxyA;
            if ("proxyB".equals(name)) return typedTarget.proxyB;
            if ("transformA".equals(name)) return typedTarget.transformA;
            if ("transformB".equals(name)) return typedTarget.transformB;
            if ("useRadii".equals(name)) return typedTarget.useRadii;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.DistanceOutput) {
            com.codename1.gaming.physics.box2d.collision.DistanceOutput typedTarget = (com.codename1.gaming.physics.box2d.collision.DistanceOutput) target;
            if ("distance".equals(name)) return typedTarget.distance;
            if ("iterations".equals(name)) return typedTarget.iterations;
            if ("pointA".equals(name)) return typedTarget.pointA;
            if ("pointB".equals(name)) return typedTarget.pointB;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Manifold) {
            com.codename1.gaming.physics.box2d.collision.Manifold typedTarget = (com.codename1.gaming.physics.box2d.collision.Manifold) target;
            if ("localNormal".equals(name)) return typedTarget.localNormal;
            if ("localPoint".equals(name)) return typedTarget.localPoint;
            if ("pointCount".equals(name)) return typedTarget.pointCount;
            if ("points".equals(name)) return typedTarget.points;
            if ("type".equals(name)) return typedTarget.type;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.ManifoldPoint) {
            com.codename1.gaming.physics.box2d.collision.ManifoldPoint typedTarget = (com.codename1.gaming.physics.box2d.collision.ManifoldPoint) target;
            if ("id".equals(name)) return typedTarget.id;
            if ("localPoint".equals(name)) return typedTarget.localPoint;
            if ("normalImpulse".equals(name)) return typedTarget.normalImpulse;
            if ("tangentImpulse".equals(name)) return typedTarget.tangentImpulse;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.RayCastInput) {
            com.codename1.gaming.physics.box2d.collision.RayCastInput typedTarget = (com.codename1.gaming.physics.box2d.collision.RayCastInput) target;
            if ("maxFraction".equals(name)) return typedTarget.maxFraction;
            if ("p1".equals(name)) return typedTarget.p1;
            if ("p2".equals(name)) return typedTarget.p2;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.RayCastOutput) {
            com.codename1.gaming.physics.box2d.collision.RayCastOutput typedTarget = (com.codename1.gaming.physics.box2d.collision.RayCastOutput) target;
            if ("fraction".equals(name)) return typedTarget.fraction;
            if ("normal".equals(name)) return typedTarget.normal;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput) {
            com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput typedTarget = (com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput) target;
            if ("proxyA".equals(name)) return typedTarget.proxyA;
            if ("proxyB".equals(name)) return typedTarget.proxyB;
            if ("sweepA".equals(name)) return typedTarget.sweepA;
            if ("sweepB".equals(name)) return typedTarget.sweepB;
            if ("tMax".equals(name)) return typedTarget.tMax;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput) {
            com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput typedTarget = (com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput) target;
            if ("state".equals(name)) return typedTarget.state;
            if ("t".equals(name)) return typedTarget.t;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.WorldManifold) {
            com.codename1.gaming.physics.box2d.collision.WorldManifold typedTarget = (com.codename1.gaming.physics.box2d.collision.WorldManifold) target;
            if ("normal".equals(name)) return typedTarget.normal;
            if ("points".equals(name)) return typedTarget.points;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.collision.Distance.class) {
            if ("GJK_CALLS".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.Distance.GJK_CALLS = toIntValue(value);
                return;
            }
            if ("GJK_ITERS".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.Distance.GJK_ITERS = toIntValue(value);
                return;
            }
            if ("GJK_MAX_ITERS".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.Distance.GJK_MAX_ITERS = toIntValue(value);
                return;
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.TimeOfImpact.class) {
            if ("toiCalls".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiCalls = toIntValue(value);
                return;
            }
            if ("toiIters".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiIters = toIntValue(value);
                return;
            }
            if ("toiMaxIters".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiMaxIters = toIntValue(value);
                return;
            }
            if ("toiMaxRootIters".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiMaxRootIters = toIntValue(value);
                return;
            }
            if ("toiRootIters".equals(name)) {
                com.codename1.gaming.physics.box2d.collision.TimeOfImpact.toiRootIters = toIntValue(value);
                return;
            }
        }
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.collision.ContactID) {
            com.codename1.gaming.physics.box2d.collision.ContactID typedTarget = (com.codename1.gaming.physics.box2d.collision.ContactID) target;
            if ("indexA".equals(name)) {
                typedTarget.indexA = (byte) toIntValue(value);
                return;
            }
            if ("indexB".equals(name)) {
                typedTarget.indexB = (byte) toIntValue(value);
                return;
            }
            if ("typeA".equals(name)) {
                typedTarget.typeA = (byte) toIntValue(value);
                return;
            }
            if ("typeB".equals(name)) {
                typedTarget.typeB = (byte) toIntValue(value);
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) {
            com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy typedTarget = (com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) target;
            if ("m_count".equals(name)) {
                typedTarget.m_count = toIntValue(value);
                return;
            }
            if ("m_radius".equals(name)) {
                typedTarget.m_radius = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) {
            com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache typedTarget = (com.codename1.gaming.physics.box2d.collision.Distance.SimplexCache) target;
            if ("count".equals(name)) {
                typedTarget.count = toIntValue(value);
                return;
            }
            if ("metric".equals(name)) {
                typedTarget.metric = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.DistanceInput) {
            com.codename1.gaming.physics.box2d.collision.DistanceInput typedTarget = (com.codename1.gaming.physics.box2d.collision.DistanceInput) target;
            if ("proxyA".equals(name)) {
                typedTarget.proxyA = (com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) value;
                return;
            }
            if ("proxyB".equals(name)) {
                typedTarget.proxyB = (com.codename1.gaming.physics.box2d.collision.Distance.DistanceProxy) value;
                return;
            }
            if ("transformA".equals(name)) {
                typedTarget.transformA = (com.codename1.gaming.physics.box2d.common.Transform) value;
                return;
            }
            if ("transformB".equals(name)) {
                typedTarget.transformB = (com.codename1.gaming.physics.box2d.common.Transform) value;
                return;
            }
            if ("useRadii".equals(name)) {
                typedTarget.useRadii = ((Boolean) value).booleanValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.DistanceOutput) {
            com.codename1.gaming.physics.box2d.collision.DistanceOutput typedTarget = (com.codename1.gaming.physics.box2d.collision.DistanceOutput) target;
            if ("distance".equals(name)) {
                typedTarget.distance = ((Number) value).floatValue();
                return;
            }
            if ("iterations".equals(name)) {
                typedTarget.iterations = toIntValue(value);
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.Manifold) {
            com.codename1.gaming.physics.box2d.collision.Manifold typedTarget = (com.codename1.gaming.physics.box2d.collision.Manifold) target;
            if ("pointCount".equals(name)) {
                typedTarget.pointCount = toIntValue(value);
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.collision.Manifold.ManifoldType) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.ManifoldPoint) {
            com.codename1.gaming.physics.box2d.collision.ManifoldPoint typedTarget = (com.codename1.gaming.physics.box2d.collision.ManifoldPoint) target;
            if ("normalImpulse".equals(name)) {
                typedTarget.normalImpulse = ((Number) value).floatValue();
                return;
            }
            if ("tangentImpulse".equals(name)) {
                typedTarget.tangentImpulse = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.RayCastInput) {
            com.codename1.gaming.physics.box2d.collision.RayCastInput typedTarget = (com.codename1.gaming.physics.box2d.collision.RayCastInput) target;
            if ("maxFraction".equals(name)) {
                typedTarget.maxFraction = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.RayCastOutput) {
            com.codename1.gaming.physics.box2d.collision.RayCastOutput typedTarget = (com.codename1.gaming.physics.box2d.collision.RayCastOutput) target;
            if ("fraction".equals(name)) {
                typedTarget.fraction = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput) {
            com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput typedTarget = (com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIInput) target;
            if ("tMax".equals(name)) {
                typedTarget.tMax = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput) {
            com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput typedTarget = (com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutput) target;
            if ("state".equals(name)) {
                typedTarget.state = (com.codename1.gaming.physics.box2d.collision.TimeOfImpact.TOIOutputState) value;
                return;
            }
            if ("t".equals(name)) {
                typedTarget.t = ((Number) value).floatValue();
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
