package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics_box2d_common {
    private GeneratedAccess_com_codename1_gaming_physics_box2d_common() {
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
        if ("Color3f".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Color3f.class;
        }
        if ("IViewportTransform".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.IViewportTransform.class;
        }
        if ("Mat22".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Mat22.class;
        }
        if ("Mat33".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Mat33.class;
        }
        if ("MathUtils".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.MathUtils.class;
        }
        if ("OBBViewportTransform".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.OBBViewportTransform.class;
        }
        if ("OBB".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.OBBViewportTransform.OBB.class;
        }
        if ("RaycastResult".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.RaycastResult.class;
        }
        if ("Rot".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Rot.class;
        }
        if ("Settings".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Settings.class;
        }
        if ("Sweep".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Sweep.class;
        }
        if ("Transform".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Transform.class;
        }
        if ("Vec2".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Vec2.class;
        }
        if ("Vec3".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.common.Vec3.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.common.Color3f.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Color3f();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Color3f(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Mat22.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Mat22();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Mat22((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Mat22(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Mat33.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Mat33();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Mat33((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.MathUtils.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.MathUtils();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.OBBViewportTransform.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.OBBViewportTransform();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.OBBViewportTransform.OBB.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.OBBViewportTransform.OBB();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.RaycastResult.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.RaycastResult();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Rot.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Rot();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Rot(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Settings.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Settings();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Sweep.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Sweep();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Transform.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Transform();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Transform((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Transform((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Vec2.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Vec2();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Vec2((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Vec2(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == com.codename1.gaming.physics.box2d.common.Vec3.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.common.Vec3();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Vec3((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.physics.box2d.common.Vec3(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.common.Mat22.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.Mat33.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.MathUtils.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.Rot.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.Settings.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.Transform.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.Vec2.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.gaming.physics.box2d.common.Vec3.class) return invokeStatic7(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.abs((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
        }
        if ("absToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.absToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
        }
        if ("createRotationalTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.createRotationalTransform(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.createRotationalTransform(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
        }
        if ("createScaleTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.createScaleTransform(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.createScaleTransform(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
        }
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.mul((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.mul((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]);
            }
        }
        if ("mulToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[2]); return null;
            }
        }
        if ("mulToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[2]); return null;
            }
        }
        if ("mulTrans".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.mulTrans((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat22.mulTrans((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]);
            }
        }
        if ("mulTransToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulTransToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulTransToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[2]); return null;
            }
        }
        if ("mulTransToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulTransToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat22.mulTransToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Mat22.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat33.mul((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1]);
            }
        }
        if ("mul22".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Mat33.mul22((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("mul22ToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat33.mul22ToOut((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("mul22ToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat33.mul22ToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("mulToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat33.mulToOut((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[2]); return null;
            }
        }
        if ("mulToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                com.codename1.gaming.physics.box2d.common.Mat33.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Mat33.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.abs(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.abs(toIntValue(adaptedArgs[0]));
            }
        }
        if ("atan2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.atan2(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("ceil".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.ceil(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("ceilPowerOf2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.ceilPowerOf2(toIntValue(adaptedArgs[0]));
            }
        }
        if ("clamp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.clamp(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.clamp((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]);
            }
        }
        if ("clampToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.MathUtils.clampToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[3]); return null;
            }
        }
        if ("cos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.cos(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("distance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.distance((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("distanceSquared".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.distanceSquared((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("fastAtan2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.fastAtan2(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.floor(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("isPowerOfTwo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.isPowerOfTwo(toIntValue(adaptedArgs[0]));
            }
        }
        if ("map".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.map(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue());
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.max(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.max(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.min(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.min(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("nextPowerOfTwo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.nextPowerOfTwo(toIntValue(adaptedArgs[0]));
            }
        }
        if ("randomFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.randomFloat(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Random.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Random.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.randomFloat((java.util.Random) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("reduceAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.reduceAngle(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("round".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.round(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("sin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.sin(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("sinLUT".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.sinLUT(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("sqrt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.MathUtils.sqrt(((Number) adaptedArgs[0]).floatValue());
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.MathUtils.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mul((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[2]); return null;
            }
        }
        if ("mulToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulToOut((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("mulToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("mulTrans".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulTrans((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulTrans((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("mulTransUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulTransUnsafe((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulTransUnsafe((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("mulUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class, com.codename1.gaming.physics.box2d.common.Rot.class}, false);
                com.codename1.gaming.physics.box2d.common.Rot.mulUnsafe((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Rot.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("mixFriction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.Settings.mixFriction(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("mixRestitution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.Settings.mixRestitution(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Settings.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Transform.mul((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return com.codename1.gaming.physics.box2d.common.Transform.mul((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1]);
            }
        }
        if ("mulToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulToOut((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulToOut((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("mulToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("mulTrans".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Transform.mulTrans((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return com.codename1.gaming.physics.box2d.common.Transform.mulTrans((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1]);
            }
        }
        if ("mulTransToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulTransToOut((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulTransToOut((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        if ("mulTransToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulTransToOutUnsafe((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class, com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                com.codename1.gaming.physics.box2d.common.Transform.mulTransToOutUnsafe((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Transform.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.abs((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("absToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.absToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("cross".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.cross(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.cross((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.cross((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("crossToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.crossToOut(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.crossToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("crossToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.crossToOutUnsafe(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.crossToOutUnsafe((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("dot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.dot((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.max((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("maxToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.maxToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec2.min((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]);
            }
        }
        if ("minToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.minToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        if ("negateToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec2.negateToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Vec2.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("cross".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec3.cross((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1]);
            }
        }
        if ("crossToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec3.crossToOut((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[2]); return null;
            }
        }
        if ("crossToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                com.codename1.gaming.physics.box2d.common.Vec3.crossToOutUnsafe((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[2]); return null;
            }
        }
        if ("dot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return com.codename1.gaming.physics.box2d.common.Vec3.dot((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.common.Vec3.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.gaming.physics.box2d.common.Color3f) {
            try {
                return invoke0((com.codename1.gaming.physics.box2d.common.Color3f) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Mat22) {
            try {
                return invoke1((com.codename1.gaming.physics.box2d.common.Mat22) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Mat33) {
            try {
                return invoke2((com.codename1.gaming.physics.box2d.common.Mat33) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.OBBViewportTransform) {
            try {
                return invoke3((com.codename1.gaming.physics.box2d.common.OBBViewportTransform) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.RaycastResult) {
            try {
                return invoke4((com.codename1.gaming.physics.box2d.common.RaycastResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Rot) {
            try {
                return invoke5((com.codename1.gaming.physics.box2d.common.Rot) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Sweep) {
            try {
                return invoke6((com.codename1.gaming.physics.box2d.common.Sweep) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Transform) {
            try {
                return invoke7((com.codename1.gaming.physics.box2d.common.Transform) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Vec2) {
            try {
                return invoke8((com.codename1.gaming.physics.box2d.common.Vec2) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Vec3) {
            try {
                return invoke9((com.codename1.gaming.physics.box2d.common.Vec3) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.IViewportTransform) {
            try {
                return invoke10((com.codename1.gaming.physics.box2d.common.IViewportTransform) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.box2d.common.Color3f typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Color3f.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Color3f.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.Color3f) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.set(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.box2d.common.Mat22 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.abs();
            }
        }
        if ("absLocal".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.absLocal(); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.add((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
        }
        if ("addLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.addLocal((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngle();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("invert".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.invert();
            }
        }
        if ("invertLocal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.invertLocal();
            }
        }
        if ("invertToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.invertToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]); return null;
            }
        }
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.mul((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.mul((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("mulLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.mulLocal((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
        }
        if ("mulToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.mulToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.mulToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("mulToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.mulToOutUnsafe((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("mulTrans".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.mulTrans((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.mulTrans((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("mulTransLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.mulTransLocal((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
        }
        if ("mulTransToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.mulTransToOut((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.mulTransToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("mulTransToOutUnsafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class, com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.mulTransToOutUnsafe((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[1]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.set(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.set(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("setIdentity".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setIdentity(); return null;
            }
        }
        if ("setZero".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setZero(); return null;
            }
        }
        if ("solve".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.solve((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("solveToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.solveToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.box2d.common.Mat33 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getInverse22".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class}, false);
                typedTarget.getInverse22((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0]); return null;
            }
        }
        if ("getSymInverse33".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat33.class}, false);
                typedTarget.getSymInverse33((com.codename1.gaming.physics.box2d.common.Mat33) adaptedArgs[0]); return null;
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setZero".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setZero(); return null;
            }
        }
        if ("solve22".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.solve22((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("solve22ToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.solve22ToOut((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("solve33".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return typedTarget.solve33((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
        }
        if ("solve33ToOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class, com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                typedTarget.solve33ToOut((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.box2d.common.OBBViewportTransform typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getExtents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtents();
            }
        }
        if ("getScreenToWorld".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getScreenToWorld((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getScreenVectorToWorld".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getScreenVectorToWorld((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getTransform".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTransform();
            }
        }
        if ("getWorldToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldToScreen((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getWorldVectorToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldVectorToScreen((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("isYFlip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isYFlip();
            }
        }
        if ("mulByTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.mulByTransform((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.OBBViewportTransform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.OBBViewportTransform.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.OBBViewportTransform) adaptedArgs[0]); return null;
            }
        }
        if ("setCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setCamera(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setCenter((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setCenter(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setExtents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setExtents((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setExtents(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Mat22.class}, false);
                typedTarget.setTransform((com.codename1.gaming.physics.box2d.common.Mat22) adaptedArgs[0]); return null;
            }
        }
        if ("setYFlip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setYFlip(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.box2d.common.RaycastResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.RaycastResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.RaycastResult.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.RaycastResult) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.box2d.common.Rot typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngle();
            }
        }
        if ("getCos".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCos();
            }
        }
        if ("getSin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSin();
            }
        }
        if ("getXAxis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getXAxis((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getYAxis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getYAxis((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.set(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Rot.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.Rot) adaptedArgs[0]);
            }
        }
        if ("setIdentity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.setIdentity();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.gaming.physics.box2d.common.Sweep typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("advance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.advance(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("getTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class, java.lang.Float.class}, false);
                typedTarget.getTransform((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("normalize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.normalize(); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Sweep.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Sweep.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.Sweep) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.gaming.physics.box2d.common.Transform typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Transform.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.Transform) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false);
                typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setIdentity".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setIdentity(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.gaming.physics.box2d.common.Vec2 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.abs();
            }
        }
        if ("absLocal".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.absLocal(); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.add((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("addLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.addLocal((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.addLocal(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isValid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValid();
            }
        }
        if ("length".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.length();
            }
        }
        if ("lengthSquared".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lengthSquared();
            }
        }
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.mul(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("mulLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.mulLocal(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("negate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.negate();
            }
        }
        if ("negateLocal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.negateLocal();
            }
        }
        if ("normalize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.normalize();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.set(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setZero".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setZero(); return null;
            }
        }
        if ("skew".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.skew();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.skew((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("sub".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.sub((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("subLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                return typedTarget.subLocal((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.gaming.physics.box2d.common.Vec3 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return typedTarget.add((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
        }
        if ("addLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return typedTarget.addLocal((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("mul".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.mul(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("mulLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.mulLocal(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("negate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.negate();
            }
        }
        if ("negateLocal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.negateLocal();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return typedTarget.set((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.set(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setZero".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setZero(); return null;
            }
        }
        if ("sub".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return typedTarget.sub((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
        }
        if ("subLocal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec3.class}, false);
                return typedTarget.subLocal((com.codename1.gaming.physics.box2d.common.Vec3) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.gaming.physics.box2d.common.IViewportTransform typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getExtents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtents();
            }
        }
        if ("getScreenToWorld".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getScreenToWorld((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getScreenVectorToWorld".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getScreenVectorToWorld((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getWorldToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldToScreen((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getWorldVectorToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getWorldVectorToScreen((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("isYFlip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isYFlip();
            }
        }
        if ("setCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setCamera(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setCenter((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setCenter(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setExtents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setExtents((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setExtents(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setYFlip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setYFlip(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.common.Color3f.class) return getStaticField0(name);
        if (type == com.codename1.gaming.physics.box2d.common.Mat33.class) return getStaticField1(name);
        if (type == com.codename1.gaming.physics.box2d.common.MathUtils.class) return getStaticField2(name);
        if (type == com.codename1.gaming.physics.box2d.common.Settings.class) return getStaticField3(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BLACK".equals(name)) return com.codename1.gaming.physics.box2d.common.Color3f.BLACK;
        if ("BLUE".equals(name)) return com.codename1.gaming.physics.box2d.common.Color3f.BLUE;
        if ("GREEN".equals(name)) return com.codename1.gaming.physics.box2d.common.Color3f.GREEN;
        if ("RED".equals(name)) return com.codename1.gaming.physics.box2d.common.Color3f.RED;
        if ("WHITE".equals(name)) return com.codename1.gaming.physics.box2d.common.Color3f.WHITE;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.common.Color3f.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("IDENTITY".equals(name)) return com.codename1.gaming.physics.box2d.common.Mat33.IDENTITY;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.common.Mat33.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("DEG2RAD".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.DEG2RAD;
        if ("HALF_PI".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.HALF_PI;
        if ("INV_PI".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.INV_PI;
        if ("PI".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.PI;
        if ("QUARTER_PI".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.QUARTER_PI;
        if ("RAD2DEG".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.RAD2DEG;
        if ("THREE_HALVES_PI".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.THREE_HALVES_PI;
        if ("TWOPI".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.TWOPI;
        if ("sinLUT".equals(name)) return com.codename1.gaming.physics.box2d.common.MathUtils.sinLUT;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.common.MathUtils.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("CONTACT_STACK_INIT_SIZE".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.CONTACT_STACK_INIT_SIZE;
        if ("EPSILON".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.EPSILON;
        if ("FAST_ABS".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.FAST_ABS;
        if ("FAST_ATAN2".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.FAST_ATAN2;
        if ("FAST_CEIL".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.FAST_CEIL;
        if ("FAST_FLOOR".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.FAST_FLOOR;
        if ("FAST_ROUND".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.FAST_ROUND;
        if ("PI".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.PI;
        if ("SINCOS_LUT_ENABLED".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.SINCOS_LUT_ENABLED;
        if ("SINCOS_LUT_LENGTH".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.SINCOS_LUT_LENGTH;
        if ("SINCOS_LUT_LERP".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.SINCOS_LUT_LERP;
        if ("SINCOS_LUT_PRECISION".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.SINCOS_LUT_PRECISION;
        if ("aabbExtension".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.aabbExtension;
        if ("aabbMultiplier".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.aabbMultiplier;
        if ("angularSleepTolerance".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.angularSleepTolerance;
        if ("angularSlop".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.angularSlop;
        if ("baumgarte".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.baumgarte;
        if ("linearSleepTolerance".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.linearSleepTolerance;
        if ("linearSlop".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.linearSlop;
        if ("maxAngularCorrection".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxAngularCorrection;
        if ("maxLinearCorrection".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxLinearCorrection;
        if ("maxManifoldPoints".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxManifoldPoints;
        if ("maxPolygonVertices".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxPolygonVertices;
        if ("maxRotation".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxRotation;
        if ("maxRotationSquared".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxRotationSquared;
        if ("maxSubSteps".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxSubSteps;
        if ("maxTOIContacts".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxTOIContacts;
        if ("maxTranslation".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxTranslation;
        if ("maxTranslationSquared".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.maxTranslationSquared;
        if ("polygonRadius".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.polygonRadius;
        if ("timeToSleep".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.timeToSleep;
        if ("toiBaugarte".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.toiBaugarte;
        if ("velocityThreshold".equals(name)) return com.codename1.gaming.physics.box2d.common.Settings.velocityThreshold;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.common.Settings.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.common.Color3f) {
            com.codename1.gaming.physics.box2d.common.Color3f typedTarget = (com.codename1.gaming.physics.box2d.common.Color3f) target;
            if ("x".equals(name)) return typedTarget.x;
            if ("y".equals(name)) return typedTarget.y;
            if ("z".equals(name)) return typedTarget.z;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Mat22) {
            com.codename1.gaming.physics.box2d.common.Mat22 typedTarget = (com.codename1.gaming.physics.box2d.common.Mat22) target;
            if ("ex".equals(name)) return typedTarget.ex;
            if ("ey".equals(name)) return typedTarget.ey;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Mat33) {
            com.codename1.gaming.physics.box2d.common.Mat33 typedTarget = (com.codename1.gaming.physics.box2d.common.Mat33) target;
            if ("ex".equals(name)) return typedTarget.ex;
            if ("ey".equals(name)) return typedTarget.ey;
            if ("ez".equals(name)) return typedTarget.ez;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.OBBViewportTransform.OBB) {
            com.codename1.gaming.physics.box2d.common.OBBViewportTransform.OBB typedTarget = (com.codename1.gaming.physics.box2d.common.OBBViewportTransform.OBB) target;
            if ("R".equals(name)) return typedTarget.R;
            if ("center".equals(name)) return typedTarget.center;
            if ("extents".equals(name)) return typedTarget.extents;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.RaycastResult) {
            com.codename1.gaming.physics.box2d.common.RaycastResult typedTarget = (com.codename1.gaming.physics.box2d.common.RaycastResult) target;
            if ("lambda".equals(name)) return typedTarget.lambda;
            if ("normal".equals(name)) return typedTarget.normal;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Rot) {
            com.codename1.gaming.physics.box2d.common.Rot typedTarget = (com.codename1.gaming.physics.box2d.common.Rot) target;
            if ("c".equals(name)) return typedTarget.c;
            if ("s".equals(name)) return typedTarget.s;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Sweep) {
            com.codename1.gaming.physics.box2d.common.Sweep typedTarget = (com.codename1.gaming.physics.box2d.common.Sweep) target;
            if ("a".equals(name)) return typedTarget.a;
            if ("a0".equals(name)) return typedTarget.a0;
            if ("alpha0".equals(name)) return typedTarget.alpha0;
            if ("c".equals(name)) return typedTarget.c;
            if ("c0".equals(name)) return typedTarget.c0;
            if ("localCenter".equals(name)) return typedTarget.localCenter;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Transform) {
            com.codename1.gaming.physics.box2d.common.Transform typedTarget = (com.codename1.gaming.physics.box2d.common.Transform) target;
            if ("p".equals(name)) return typedTarget.p;
            if ("q".equals(name)) return typedTarget.q;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Vec2) {
            com.codename1.gaming.physics.box2d.common.Vec2 typedTarget = (com.codename1.gaming.physics.box2d.common.Vec2) target;
            if ("x".equals(name)) return typedTarget.x;
            if ("y".equals(name)) return typedTarget.y;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Vec3) {
            com.codename1.gaming.physics.box2d.common.Vec3 typedTarget = (com.codename1.gaming.physics.box2d.common.Vec3) target;
            if ("x".equals(name)) return typedTarget.x;
            if ("y".equals(name)) return typedTarget.y;
            if ("z".equals(name)) return typedTarget.z;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.common.Settings.class) {
            if ("CONTACT_STACK_INIT_SIZE".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.CONTACT_STACK_INIT_SIZE = toIntValue(value);
                return;
            }
            if ("FAST_ABS".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.FAST_ABS = ((Boolean) value).booleanValue();
                return;
            }
            if ("FAST_ATAN2".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.FAST_ATAN2 = ((Boolean) value).booleanValue();
                return;
            }
            if ("FAST_CEIL".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.FAST_CEIL = ((Boolean) value).booleanValue();
                return;
            }
            if ("FAST_FLOOR".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.FAST_FLOOR = ((Boolean) value).booleanValue();
                return;
            }
            if ("FAST_ROUND".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.FAST_ROUND = ((Boolean) value).booleanValue();
                return;
            }
            if ("SINCOS_LUT_ENABLED".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.SINCOS_LUT_ENABLED = ((Boolean) value).booleanValue();
                return;
            }
            if ("SINCOS_LUT_LERP".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.SINCOS_LUT_LERP = ((Boolean) value).booleanValue();
                return;
            }
            if ("maxRotationSquared".equals(name)) {
                com.codename1.gaming.physics.box2d.common.Settings.maxRotationSquared = ((Number) value).floatValue();
                return;
            }
        }
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.common.Color3f) {
            com.codename1.gaming.physics.box2d.common.Color3f typedTarget = (com.codename1.gaming.physics.box2d.common.Color3f) target;
            if ("x".equals(name)) {
                typedTarget.x = ((Number) value).floatValue();
                return;
            }
            if ("y".equals(name)) {
                typedTarget.y = ((Number) value).floatValue();
                return;
            }
            if ("z".equals(name)) {
                typedTarget.z = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.RaycastResult) {
            com.codename1.gaming.physics.box2d.common.RaycastResult typedTarget = (com.codename1.gaming.physics.box2d.common.RaycastResult) target;
            if ("lambda".equals(name)) {
                typedTarget.lambda = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Rot) {
            com.codename1.gaming.physics.box2d.common.Rot typedTarget = (com.codename1.gaming.physics.box2d.common.Rot) target;
            if ("c".equals(name)) {
                typedTarget.c = ((Number) value).floatValue();
                return;
            }
            if ("s".equals(name)) {
                typedTarget.s = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Sweep) {
            com.codename1.gaming.physics.box2d.common.Sweep typedTarget = (com.codename1.gaming.physics.box2d.common.Sweep) target;
            if ("a".equals(name)) {
                typedTarget.a = ((Number) value).floatValue();
                return;
            }
            if ("a0".equals(name)) {
                typedTarget.a0 = ((Number) value).floatValue();
                return;
            }
            if ("alpha0".equals(name)) {
                typedTarget.alpha0 = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Vec2) {
            com.codename1.gaming.physics.box2d.common.Vec2 typedTarget = (com.codename1.gaming.physics.box2d.common.Vec2) target;
            if ("x".equals(name)) {
                typedTarget.x = ((Number) value).floatValue();
                return;
            }
            if ("y".equals(name)) {
                typedTarget.y = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.common.Vec3) {
            com.codename1.gaming.physics.box2d.common.Vec3 typedTarget = (com.codename1.gaming.physics.box2d.common.Vec3) target;
            if ("x".equals(name)) {
                typedTarget.x = ((Number) value).floatValue();
                return;
            }
            if ("y".equals(name)) {
                typedTarget.y = ((Number) value).floatValue();
                return;
            }
            if ("z".equals(name)) {
                typedTarget.z = ((Number) value).floatValue();
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
