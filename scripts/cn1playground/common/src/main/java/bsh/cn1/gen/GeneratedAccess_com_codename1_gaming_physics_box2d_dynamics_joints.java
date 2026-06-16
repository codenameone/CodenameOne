package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_physics_box2d_dynamics_joints {
    private GeneratedAccess_com_codename1_gaming_physics_box2d_dynamics_joints() {
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
        if ("ConstantVolumeJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint.class;
        }
        if ("ConstantVolumeJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef.class;
        }
        if ("DistanceJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint.class;
        }
        if ("DistanceJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef.class;
        }
        if ("FrictionJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint.class;
        }
        if ("FrictionJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef.class;
        }
        if ("GearJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint.class;
        }
        if ("GearJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef.class;
        }
        if ("Jacobian".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian.class;
        }
        if ("Joint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class;
        }
        if ("JointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.JointDef.class;
        }
        if ("JointEdge".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge.class;
        }
        if ("JointType".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.class;
        }
        if ("LimitState".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.class;
        }
        if ("MouseJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint.class;
        }
        if ("MouseJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef.class;
        }
        if ("PrismaticJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint.class;
        }
        if ("PrismaticJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef.class;
        }
        if ("PulleyJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint.class;
        }
        if ("PulleyJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef.class;
        }
        if ("RevoluteJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint.class;
        }
        if ("RevoluteJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef.class;
        }
        if ("RopeJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint.class;
        }
        if ("RopeJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef.class;
        }
        if ("WeldJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint.class;
        }
        if ("WeldJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef.class;
        }
        if ("WheelJoint".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint.class;
        }
        if ("WheelJointDef".equals(simpleName)) {
            return com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.World.class, com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.World.class, com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef.class}, false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint((com.codename1.gaming.physics.box2d.dynamics.World) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.JointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.JointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef();
            }
        }
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.World.class, com.codename1.gaming.physics.box2d.dynamics.joints.JointDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.World.class, com.codename1.gaming.physics.box2d.dynamics.joints.JointDef.class}, false);
                return com.codename1.gaming.physics.box2d.dynamics.joints.Joint.create((com.codename1.gaming.physics.box2d.dynamics.World) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.joints.JointDef) adaptedArgs[1]);
            }
        }
        if ("destroy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class}, false);
                com.codename1.gaming.physics.box2d.dynamics.joints.Joint.destroy((com.codename1.gaming.physics.box2d.dynamics.joints.Joint) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.physics.box2d.dynamics.joints.Joint.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint) {
            try {
                return invoke0((com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) {
            try {
                return invoke1((com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) {
            try {
                return invoke2((com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef) {
            try {
                return invoke3((com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint) {
            try {
                return invoke4((com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef) {
            try {
                return invoke5((com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint) {
            try {
                return invoke6((com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint) {
            try {
                return invoke7((com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint) {
            try {
                return invoke8((com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef) {
            try {
                return invoke9((com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint) {
            try {
                return invoke10((com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef) {
            try {
                return invoke11((com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint) {
            try {
                return invoke12((com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef) {
            try {
                return invoke13((com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint) {
            try {
                return invoke14((com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint) {
            try {
                return invoke15((com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef) {
            try {
                return invoke16((com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint) {
            try {
                return invoke17((com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef) {
            try {
                return invoke18((com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.Joint) {
            try {
                return invoke19((com.codename1.gaming.physics.box2d.dynamics.joints.Joint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getBodies".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodies();
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getJoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJoints();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.inflate(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class}, false);
                typedTarget.addBody((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0]); return null;
            }
        }
        if ("addBodyAndJoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint.class}, false);
                typedTarget.addBodyAndJoint((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getDampingRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDampingRatio();
            }
        }
        if ("getFrequency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrequency();
            }
        }
        if ("getLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLength();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setDampingRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setDampingRatio(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setFrequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setFrequency(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLength(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getMaxForce".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxForce();
            }
        }
        if ("getMaxTorque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxTorque();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setMaxForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxForce(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMaxTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxTorque(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getJoint1".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJoint1();
            }
        }
        if ("getJoint2".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJoint2();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRatio();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setRatio(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getDampingRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDampingRatio();
            }
        }
        if ("getFrequency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrequency();
            }
        }
        if ("getMaxForce".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxForce();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTarget();
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setDampingRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setDampingRatio(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setFrequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setFrequency(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMaxForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxForce(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.setTarget((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("enableLimit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.enableLimit(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("enableMotor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.enableMotor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getJointSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointSpeed();
            }
        }
        if ("getJointTranslation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointTranslation();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getLocalAxisA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAxisA();
            }
        }
        if ("getLowerLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLowerLimit();
            }
        }
        if ("getMaxMotorForce".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxMotorForce();
            }
        }
        if ("getMotorForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getMotorForce(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getMotorSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMotorSpeed();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getReferenceAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReferenceAngle();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUpperLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUpperLimit();
            }
        }
        if ("getUserData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserData();
            }
        }
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("isLimitEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLimitEnabled();
            }
        }
        if ("isMotorEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMotorEnabled();
            }
        }
        if ("setLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setLimits(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setMaxMotorForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxMotorForce(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMotorSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMotorSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getCurrentLengthA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentLengthA();
            }
        }
        if ("getCurrentLengthB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentLengthB();
            }
        }
        if ("getGroundAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGroundAnchorA();
            }
        }
        if ("getGroundAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGroundAnchorB();
            }
        }
        if ("getLength1".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLength1();
            }
        }
        if ("getLength2".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLength2();
            }
        }
        if ("getLengthA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLengthA();
            }
        }
        if ("getLengthB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLengthB();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRatio();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class, java.lang.Float.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[3], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[4], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[5], ((Number) adaptedArgs[6]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("enableLimit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.enableLimit(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("enableMotor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.enableMotor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getJointAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointAngle();
            }
        }
        if ("getJointSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointSpeed();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getLowerLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLowerLimit();
            }
        }
        if ("getMaxMotorTorque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxMotorTorque();
            }
        }
        if ("getMotorSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMotorSpeed();
            }
        }
        if ("getMotorTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getMotorTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getReferenceAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReferenceAngle();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUpperLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUpperLimit();
            }
        }
        if ("getUserData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserData();
            }
        }
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("isLimitEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLimitEnabled();
            }
        }
        if ("isMotorEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMotorEnabled();
            }
        }
        if ("setLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setLimits(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setMaxMotorTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxMotorTorque(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMotorSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMotorSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getLimitState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLimitState();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getMaxLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxLength();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setMaxLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxLength(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getDampingRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDampingRatio();
            }
        }
        if ("getFrequency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrequency();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getReferenceAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReferenceAngle();
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setDampingRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setDampingRatio(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setFrequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setFrequency(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("enableMotor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.enableMotor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getJointSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointSpeed();
            }
        }
        if ("getJointTranslation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJointTranslation();
            }
        }
        if ("getLocalAnchorA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorA();
            }
        }
        if ("getLocalAnchorB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAnchorB();
            }
        }
        if ("getLocalAxisA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalAxisA();
            }
        }
        if ("getMaxMotorTorque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxMotorTorque();
            }
        }
        if ("getMotorSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMotorSpeed();
            }
        }
        if ("getMotorTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getMotorTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getSpringDampingRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpringDampingRatio();
            }
        }
        if ("getSpringFrequencyHz".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpringFrequencyHz();
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("isMotorEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMotorEnabled();
            }
        }
        if ("setMaxMotorTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxMotorTorque(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMotorSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMotorSpeed(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setSpringDampingRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setSpringDampingRatio(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setSpringFrequencyHz".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setSpringFrequencyHz(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("initialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.dynamics.Body.class, com.codename1.gaming.physics.box2d.common.Vec2.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.initialize((com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[0], (com.codename1.gaming.physics.box2d.dynamics.Body) adaptedArgs[1], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[2], (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.gaming.physics.box2d.dynamics.joints.Joint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destructor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.destructor(); return null;
            }
        }
        if ("getAnchorA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorA((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
            }
        }
        if ("getAnchorB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getAnchorB((com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[0]); return null;
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
        if ("getCollideConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollideConnected();
            }
        }
        if ("getNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNext();
            }
        }
        if ("getReactionForce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.gaming.physics.box2d.common.Vec2.class}, false);
                typedTarget.getReactionForce(((Number) adaptedArgs[0]).floatValue(), (com.codename1.gaming.physics.box2d.common.Vec2) adaptedArgs[1]); return null;
            }
        }
        if ("getReactionTorque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.getReactionTorque(((Number) adaptedArgs[0]).floatValue());
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
        if ("initVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.initVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("setUserData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setUserData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("solvePositionConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                return typedTarget.solvePositionConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]);
            }
        }
        if ("solveVelocityConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.physics.box2d.dynamics.SolverData.class}, false);
                typedTarget.solveVelocityConstraints((com.codename1.gaming.physics.box2d.dynamics.SolverData) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.JointType.class) return getStaticField0(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.class) return getStaticField1(name);
        if (type == com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("CONSTANT_VOLUME".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.CONSTANT_VOLUME;
        if ("DISTANCE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.DISTANCE;
        if ("FRICTION".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.FRICTION;
        if ("GEAR".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.GEAR;
        if ("MOUSE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.MOUSE;
        if ("PRISMATIC".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.PRISMATIC;
        if ("PULLEY".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.PULLEY;
        if ("REVOLUTE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.REVOLUTE;
        if ("ROPE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.ROPE;
        if ("UNKNOWN".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.UNKNOWN;
        if ("WELD".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.WELD;
        if ("WHEEL".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.JointType.WHEEL;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.joints.JointType.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("AT_LOWER".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.AT_LOWER;
        if ("AT_UPPER".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.AT_UPPER;
        if ("EQUAL".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.EQUAL;
        if ("INACTIVE".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.INACTIVE;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.joints.LimitState.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("MIN_PULLEY_LENGTH".equals(name)) return com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint.MIN_PULLEY_LENGTH;
        throw unsupportedStaticField(com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("dampingRatio".equals(name)) return typedTarget.dampingRatio;
            if ("frequencyHz".equals(name)) return typedTarget.frequencyHz;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("dampingRatio".equals(name)) return typedTarget.dampingRatio;
            if ("frequencyHz".equals(name)) return typedTarget.frequencyHz;
            if ("length".equals(name)) return typedTarget.length;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("maxForce".equals(name)) return typedTarget.maxForce;
            if ("maxTorque".equals(name)) return typedTarget.maxTorque;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("joint1".equals(name)) return typedTarget.joint1;
            if ("joint2".equals(name)) return typedTarget.joint2;
            if ("ratio".equals(name)) return typedTarget.ratio;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("dampingRatio".equals(name)) return typedTarget.dampingRatio;
            if ("frequencyHz".equals(name)) return typedTarget.frequencyHz;
            if ("maxForce".equals(name)) return typedTarget.maxForce;
            if ("target".equals(name)) return typedTarget.target;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("enableLimit".equals(name)) return typedTarget.enableLimit;
            if ("enableMotor".equals(name)) return typedTarget.enableMotor;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("localAxisA".equals(name)) return typedTarget.localAxisA;
            if ("lowerTranslation".equals(name)) return typedTarget.lowerTranslation;
            if ("maxMotorForce".equals(name)) return typedTarget.maxMotorForce;
            if ("motorSpeed".equals(name)) return typedTarget.motorSpeed;
            if ("referenceAngle".equals(name)) return typedTarget.referenceAngle;
            if ("type".equals(name)) return typedTarget.type;
            if ("upperTranslation".equals(name)) return typedTarget.upperTranslation;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("groundAnchorA".equals(name)) return typedTarget.groundAnchorA;
            if ("groundAnchorB".equals(name)) return typedTarget.groundAnchorB;
            if ("lengthA".equals(name)) return typedTarget.lengthA;
            if ("lengthB".equals(name)) return typedTarget.lengthB;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("ratio".equals(name)) return typedTarget.ratio;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("enableLimit".equals(name)) return typedTarget.enableLimit;
            if ("enableMotor".equals(name)) return typedTarget.enableMotor;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("lowerAngle".equals(name)) return typedTarget.lowerAngle;
            if ("maxMotorTorque".equals(name)) return typedTarget.maxMotorTorque;
            if ("motorSpeed".equals(name)) return typedTarget.motorSpeed;
            if ("referenceAngle".equals(name)) return typedTarget.referenceAngle;
            if ("type".equals(name)) return typedTarget.type;
            if ("upperAngle".equals(name)) return typedTarget.upperAngle;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("maxLength".equals(name)) return typedTarget.maxLength;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("dampingRatio".equals(name)) return typedTarget.dampingRatio;
            if ("frequencyHz".equals(name)) return typedTarget.frequencyHz;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("referenceAngle".equals(name)) return typedTarget.referenceAngle;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("dampingRatio".equals(name)) return typedTarget.dampingRatio;
            if ("enableMotor".equals(name)) return typedTarget.enableMotor;
            if ("frequencyHz".equals(name)) return typedTarget.frequencyHz;
            if ("localAnchorA".equals(name)) return typedTarget.localAnchorA;
            if ("localAnchorB".equals(name)) return typedTarget.localAnchorB;
            if ("localAxisA".equals(name)) return typedTarget.localAxisA;
            if ("maxMotorTorque".equals(name)) return typedTarget.maxMotorTorque;
            if ("motorSpeed".equals(name)) return typedTarget.motorSpeed;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian) {
            com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian) target;
            if ("angularA".equals(name)) return typedTarget.angularA;
            if ("angularB".equals(name)) return typedTarget.angularB;
            if ("linearA".equals(name)) return typedTarget.linearA;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.Joint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.Joint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) target;
            if ("m_edgeA".equals(name)) return typedTarget.m_edgeA;
            if ("m_edgeB".equals(name)) return typedTarget.m_edgeB;
            if ("m_islandFlag".equals(name)) return typedTarget.m_islandFlag;
            if ("m_next".equals(name)) return typedTarget.m_next;
            if ("m_prev".equals(name)) return typedTarget.m_prev;
            if ("m_userData".equals(name)) return typedTarget.m_userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.JointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.JointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.JointDef) target;
            if ("bodyA".equals(name)) return typedTarget.bodyA;
            if ("bodyB".equals(name)) return typedTarget.bodyB;
            if ("collideConnected".equals(name)) return typedTarget.collideConnected;
            if ("type".equals(name)) return typedTarget.type;
            if ("userData".equals(name)) return typedTarget.userData;
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) {
            com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) target;
            if ("joint".equals(name)) return typedTarget.joint;
            if ("next".equals(name)) return typedTarget.next;
            if ("other".equals(name)) return typedTarget.other;
            if ("prev".equals(name)) return typedTarget.prev;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.ConstantVolumeJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("dampingRatio".equals(name)) {
                typedTarget.dampingRatio = ((Number) value).floatValue();
                return;
            }
            if ("frequencyHz".equals(name)) {
                typedTarget.frequencyHz = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("dampingRatio".equals(name)) {
                typedTarget.dampingRatio = ((Number) value).floatValue();
                return;
            }
            if ("frequencyHz".equals(name)) {
                typedTarget.frequencyHz = ((Number) value).floatValue();
                return;
            }
            if ("length".equals(name)) {
                typedTarget.length = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.FrictionJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("maxForce".equals(name)) {
                typedTarget.maxForce = ((Number) value).floatValue();
                return;
            }
            if ("maxTorque".equals(name)) {
                typedTarget.maxTorque = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.GearJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.GearJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("joint1".equals(name)) {
                typedTarget.joint1 = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("joint2".equals(name)) {
                typedTarget.joint2 = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("ratio".equals(name)) {
                typedTarget.ratio = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("dampingRatio".equals(name)) {
                typedTarget.dampingRatio = ((Number) value).floatValue();
                return;
            }
            if ("frequencyHz".equals(name)) {
                typedTarget.frequencyHz = ((Number) value).floatValue();
                return;
            }
            if ("maxForce".equals(name)) {
                typedTarget.maxForce = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("enableLimit".equals(name)) {
                typedTarget.enableLimit = ((Boolean) value).booleanValue();
                return;
            }
            if ("enableMotor".equals(name)) {
                typedTarget.enableMotor = ((Boolean) value).booleanValue();
                return;
            }
            if ("lowerTranslation".equals(name)) {
                typedTarget.lowerTranslation = ((Number) value).floatValue();
                return;
            }
            if ("maxMotorForce".equals(name)) {
                typedTarget.maxMotorForce = ((Number) value).floatValue();
                return;
            }
            if ("motorSpeed".equals(name)) {
                typedTarget.motorSpeed = ((Number) value).floatValue();
                return;
            }
            if ("referenceAngle".equals(name)) {
                typedTarget.referenceAngle = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("upperTranslation".equals(name)) {
                typedTarget.upperTranslation = ((Number) value).floatValue();
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.PulleyJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("groundAnchorA".equals(name)) {
                typedTarget.groundAnchorA = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("groundAnchorB".equals(name)) {
                typedTarget.groundAnchorB = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("lengthA".equals(name)) {
                typedTarget.lengthA = ((Number) value).floatValue();
                return;
            }
            if ("lengthB".equals(name)) {
                typedTarget.lengthB = ((Number) value).floatValue();
                return;
            }
            if ("localAnchorA".equals(name)) {
                typedTarget.localAnchorA = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("localAnchorB".equals(name)) {
                typedTarget.localAnchorB = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("ratio".equals(name)) {
                typedTarget.ratio = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("enableLimit".equals(name)) {
                typedTarget.enableLimit = ((Boolean) value).booleanValue();
                return;
            }
            if ("enableMotor".equals(name)) {
                typedTarget.enableMotor = ((Boolean) value).booleanValue();
                return;
            }
            if ("localAnchorA".equals(name)) {
                typedTarget.localAnchorA = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("localAnchorB".equals(name)) {
                typedTarget.localAnchorB = (com.codename1.gaming.physics.box2d.common.Vec2) value;
                return;
            }
            if ("lowerAngle".equals(name)) {
                typedTarget.lowerAngle = ((Number) value).floatValue();
                return;
            }
            if ("maxMotorTorque".equals(name)) {
                typedTarget.maxMotorTorque = ((Number) value).floatValue();
                return;
            }
            if ("motorSpeed".equals(name)) {
                typedTarget.motorSpeed = ((Number) value).floatValue();
                return;
            }
            if ("referenceAngle".equals(name)) {
                typedTarget.referenceAngle = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("upperAngle".equals(name)) {
                typedTarget.upperAngle = ((Number) value).floatValue();
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RopeJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.RopeJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("maxLength".equals(name)) {
                typedTarget.maxLength = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WeldJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("dampingRatio".equals(name)) {
                typedTarget.dampingRatio = ((Number) value).floatValue();
                return;
            }
            if ("frequencyHz".equals(name)) {
                typedTarget.frequencyHz = ((Number) value).floatValue();
                return;
            }
            if ("referenceAngle".equals(name)) {
                typedTarget.referenceAngle = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WheelJoint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.WheelJointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("dampingRatio".equals(name)) {
                typedTarget.dampingRatio = ((Number) value).floatValue();
                return;
            }
            if ("enableMotor".equals(name)) {
                typedTarget.enableMotor = ((Boolean) value).booleanValue();
                return;
            }
            if ("frequencyHz".equals(name)) {
                typedTarget.frequencyHz = ((Number) value).floatValue();
                return;
            }
            if ("maxMotorTorque".equals(name)) {
                typedTarget.maxMotorTorque = ((Number) value).floatValue();
                return;
            }
            if ("motorSpeed".equals(name)) {
                typedTarget.motorSpeed = ((Number) value).floatValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian) {
            com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.Jacobian) target;
            if ("angularA".equals(name)) {
                typedTarget.angularA = ((Number) value).floatValue();
                return;
            }
            if ("angularB".equals(name)) {
                typedTarget.angularB = ((Number) value).floatValue();
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.Joint) {
            com.codename1.gaming.physics.box2d.dynamics.joints.Joint typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) target;
            if ("m_edgeA".equals(name)) {
                typedTarget.m_edgeA = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_edgeB".equals(name)) {
                typedTarget.m_edgeB = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("m_islandFlag".equals(name)) {
                typedTarget.m_islandFlag = ((Boolean) value).booleanValue();
                return;
            }
            if ("m_next".equals(name)) {
                typedTarget.m_next = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_prev".equals(name)) {
                typedTarget.m_prev = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("m_userData".equals(name)) {
                typedTarget.m_userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.JointDef) {
            com.codename1.gaming.physics.box2d.dynamics.joints.JointDef typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.JointDef) target;
            if ("bodyA".equals(name)) {
                typedTarget.bodyA = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("bodyB".equals(name)) {
                typedTarget.bodyB = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("collideConnected".equals(name)) {
                typedTarget.collideConnected = ((Boolean) value).booleanValue();
                return;
            }
            if ("type".equals(name)) {
                typedTarget.type = (com.codename1.gaming.physics.box2d.dynamics.joints.JointType) value;
                return;
            }
            if ("userData".equals(name)) {
                typedTarget.userData = (java.lang.Object) value;
                return;
            }
        }
        if (target instanceof com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) {
            com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge typedTarget = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) target;
            if ("joint".equals(name)) {
                typedTarget.joint = (com.codename1.gaming.physics.box2d.dynamics.joints.Joint) value;
                return;
            }
            if ("next".equals(name)) {
                typedTarget.next = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
                return;
            }
            if ("other".equals(name)) {
                typedTarget.other = (com.codename1.gaming.physics.box2d.dynamics.Body) value;
                return;
            }
            if ("prev".equals(name)) {
                typedTarget.prev = (com.codename1.gaming.physics.box2d.dynamics.joints.JointEdge) value;
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
