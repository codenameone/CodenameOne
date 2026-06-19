package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics_box2d_collision_shapes {
    private GeneratedAccess_com_codename1_gaming_physics_box2d_collision_shapes() {
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
        if ("ChainShape".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.ChainShape.class;
        }
        if ("CircleShape".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class;
        }
        if ("EdgeShape".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class;
        }
        if ("MassData".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.MassData.class;
        }
        if ("PolygonShape".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class;
        }
        if ("Shape".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.Shape.class;
        }
        if ("ShapeType".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.collision.shapes.ChainShape.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.shapes.ChainShape();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.shapes.CircleShape.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.shapes.CircleShape();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.shapes.MassData.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.shapes.MassData();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false);
                return new com.codename1.gaming.physics.box2d.collision.shapes.MassData((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape();
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
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.ChainShape) {
            try {
                return invoke0((com.codename1.gaming.physics.box2d.collision.shapes.ChainShape) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) {
            try {
                return invoke1((com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) {
            try {
                return invoke2((com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) {
            try {
                return invoke3((com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.MassData) {
            try {
                return invoke4((com.codename1.gaming.physics.box2d.collision.shapes.MassData) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.Shape) {
            try {
                return invoke5((com.codename1.gaming.physics.box2d.collision.shapes.Shape) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.box2d.collision.shapes.ChainShape typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("computeAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                typedTarget.computeAABB((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("computeMass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false);
                typedTarget.computeMass((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("createChain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class}, false);
                typedTarget.createChain((com.codename1.gaming.physics.box2d.common.Vec2[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("createLoop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class}, false);
                typedTarget.createLoop((com.codename1.gaming.physics.box2d.common.Vec2[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("getChildCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildCount();
            }
        }
        if ("getChildEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape.class, java.lang.Integer.class}, false);
                typedTarget.getChildEdge((com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("getRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadius();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if ("setNextVertex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setNextVertex((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("setPrevVertex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setPrevVertex((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("setRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRadius(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("testPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.testPoint((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.box2d.collision.shapes.CircleShape typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("computeAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                typedTarget.computeAABB((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("computeMass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false);
                typedTarget.computeMass((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("getChildCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildCount();
            }
        }
        if ("getRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadius();
            }
        }
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
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
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
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if ("setRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRadius(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("testPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.testPoint((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("computeAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                typedTarget.computeAABB((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("computeMass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false);
                typedTarget.computeMass((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("getChildCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildCount();
            }
        }
        if ("getRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadius();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("setRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRadius(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("testPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.testPoint((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("centroid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return typedTarget.centroid((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0]);
            }
        }
        if ("centroidToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.centroidToOut((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("computeAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                typedTarget.computeAABB((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("computeCentroidToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.computeCentroidToOut((com.codename1.gaming.physics.box2d.common.Vec2[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("computeMass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false);
                typedTarget.computeMass((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("getChildCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildCount();
            }
        }
        if ("getNormals".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNormals();
            }
        }
        if ("getRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadius();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
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
        if ("getVertices".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVertices();
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec2[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.pooling.arrays.Vec2Array.class, com.codename1.gaming.physics.box2d.pooling.arrays.IntArray.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2[].class, java.lang.Integer.class, com.codename1.gaming.physics.box2d.pooling.arrays.Vec2Array.class, com.codename1.gaming.physics.box2d.pooling.arrays.IntArray.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec2[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.gaming.physics.box2d.pooling.arrays.Vec2Array) adaptedArgs[2], (com.codename1.gaming.physics.box2d.pooling.arrays.IntArray) adaptedArgs[3]); return null;
            }
        }
        if ("setAsBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setAsBox(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false);
                typedTarget.setAsBox(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("setRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRadius(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("testPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.testPoint((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("validate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.validate();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.box2d.collision.shapes.MassData typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.box2d.collision.shapes.Shape typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("computeAABB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.AABB.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                typedTarget.computeAABB((com.codename1.gaming.physics.box2d.collision.AABB) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("computeMass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.shapes.MassData.class, java.lang.Float.class}, false);
                typedTarget.computeMass((com.codename1.gaming.physics.box2d.collision.shapes.MassData) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("getChildCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildCount();
            }
        }
        if ("getRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadius();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("raycast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.collision.RayCastOutput.class, com.codename1.gaming.physics.box2d.collision.RayCastInput.class, com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Integer.class}, false);
                return typedTarget.raycast((com.codename1.gaming.physics.box2d.collision.RayCastOutput) adaptedArgs[0], (com.codename1.gaming.physics.box2d.collision.RayCastInput) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if ("setRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRadius(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("testPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.testPoint((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.class) return getStaticField0(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("CHAIN".equals(name)) return com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.CHAIN;
        if ("CIRCLE".equals(name)) return com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.CIRCLE;
        if ("EDGE".equals(name)) return com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.EDGE;
        if ("POLYGON".equals(name)) return com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.POLYGON;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.collision.shapes.ShapeType.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.ChainShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.ChainShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.ChainShape) target;
            if ("m_count".equals(name)) return typedTarget.m_count;
            if ("m_hasNextVertex".equals(name)) return typedTarget.m_hasNextVertex;
            if ("m_hasPrevVertex".equals(name)) return typedTarget.m_hasPrevVertex;
            if ("m_nextVertex".equals(name)) return typedTarget.m_nextVertex;
            if ("m_prevVertex".equals(name)) return typedTarget.m_prevVertex;
            if ("m_radius".equals(name)) return typedTarget.m_radius;
            if ("m_type".equals(name)) return typedTarget.m_type;
            if ("m_vertices".equals(name)) return typedTarget.m_vertices;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.CircleShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) target;
            if ("m_p".equals(name)) return typedTarget.m_p;
            if ("m_radius".equals(name)) return typedTarget.m_radius;
            if ("m_type".equals(name)) return typedTarget.m_type;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) target;
            if ("m_hasVertex0".equals(name)) return typedTarget.m_hasVertex0;
            if ("m_hasVertex3".equals(name)) return typedTarget.m_hasVertex3;
            if ("m_radius".equals(name)) return typedTarget.m_radius;
            if ("m_type".equals(name)) return typedTarget.m_type;
            if ("m_vertex0".equals(name)) return typedTarget.m_vertex0;
            if ("m_vertex1".equals(name)) return typedTarget.m_vertex1;
            if ("m_vertex2".equals(name)) return typedTarget.m_vertex2;
            if ("m_vertex3".equals(name)) return typedTarget.m_vertex3;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) target;
            if ("m_centroid".equals(name)) return typedTarget.m_centroid;
            if ("m_count".equals(name)) return typedTarget.m_count;
            if ("m_normals".equals(name)) return typedTarget.m_normals;
            if ("m_radius".equals(name)) return typedTarget.m_radius;
            if ("m_type".equals(name)) return typedTarget.m_type;
            if ("m_vertices".equals(name)) return typedTarget.m_vertices;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.MassData) {
            com.codename1.gaming.physics.box2d.collision.shapes.MassData typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.MassData) target;
            if ("I".equals(name)) return typedTarget.I;
            if ("center".equals(name)) return typedTarget.center;
            if ("mass".equals(name)) return typedTarget.mass;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.Shape) {
            com.codename1.gaming.physics.box2d.collision.shapes.Shape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.Shape) target;
            if ("m_radius".equals(name)) return typedTarget.m_radius;
            if ("m_type".equals(name)) return typedTarget.m_type;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.ChainShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.ChainShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.ChainShape) target;
            if ("m_count".equals(name)) {
                typedTarget.m_count = toIntValue(value);
                return;
            }
            if ("m_hasNextVertex".equals(name)) {
                typedTarget.m_hasNextVertex = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_hasPrevVertex".equals(name)) {
                typedTarget.m_hasPrevVertex = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_radius".equals(name)) {
                typedTarget.m_radius = ((Number) value).floatValue();
                return;
            }
            if ("m_vertices".equals(name)) {
                typedTarget.m_vertices = (com.codename1.gaming.physics.box2d.common.Vec2[]) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.CircleShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.CircleShape) target;
            if ("m_radius".equals(name)) {
                typedTarget.m_radius = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape) target;
            if ("m_hasVertex0".equals(name)) {
                typedTarget.m_hasVertex0 = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_hasVertex3".equals(name)) {
                typedTarget.m_hasVertex3 = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_radius".equals(name)) {
                typedTarget.m_radius = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) {
            com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape) target;
            if ("m_count".equals(name)) {
                typedTarget.m_count = toIntValue(value);
                return;
            }
            if ("m_radius".equals(name)) {
                typedTarget.m_radius = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.MassData) {
            com.codename1.gaming.physics.box2d.collision.shapes.MassData typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.MassData) target;
            if ("I".equals(name)) {
                typedTarget.I = ((Number) value).floatValue();
                return;
            }
            if ("mass".equals(name)) {
                typedTarget.mass = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.collision.shapes.Shape) {
            com.codename1.gaming.physics.box2d.collision.shapes.Shape typedTarget = (com.codename1.gaming.physics.box2d.collision.shapes.Shape) target;
            if ("m_radius".equals(name)) {
                typedTarget.m_radius = ((Number) value).floatValue();
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
