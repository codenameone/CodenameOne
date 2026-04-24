package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_animations {
    private GeneratedAccess_com_codename1_ui_animations() {
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
        if ("Animation".equals(simpleName)) {
            return com.codename1.ui.animations.Animation.class;
        }
        if ("AnimationObject".equals(simpleName)) {
            return com.codename1.ui.animations.AnimationObject.class;
        }
        if ("BubbleTransition".equals(simpleName)) {
            return com.codename1.ui.animations.BubbleTransition.class;
        }
        if ("CommonTransitions".equals(simpleName)) {
            return com.codename1.ui.animations.CommonTransitions.class;
        }
        if ("ComponentAnimation".equals(simpleName)) {
            return com.codename1.ui.animations.ComponentAnimation.class;
        }
        if ("UIMutation".equals(simpleName)) {
            return com.codename1.ui.animations.ComponentAnimation.UIMutation.class;
        }
        if ("FlipTransition".equals(simpleName)) {
            return com.codename1.ui.animations.FlipTransition.class;
        }
        if ("MorphTransition".equals(simpleName)) {
            return com.codename1.ui.animations.MorphTransition.class;
        }
        if ("Motion".equals(simpleName)) {
            return com.codename1.ui.animations.Motion.class;
        }
        if ("Timeline".equals(simpleName)) {
            return com.codename1.ui.animations.Timeline.class;
        }
        if ("Transition".equals(simpleName)) {
            return com.codename1.ui.animations.Transition.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.animations.BubbleTransition.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.animations.BubbleTransition();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.animations.BubbleTransition(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.ui.animations.BubbleTransition(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ui.animations.ComponentAnimation.UIMutation.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, com.codename1.ui.animations.ComponentAnimation.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, com.codename1.ui.animations.ComponentAnimation.class}, false);
                return new com.codename1.ui.animations.ComponentAnimation.UIMutation((com.codename1.ui.Container) adaptedArgs[0], (com.codename1.ui.animations.ComponentAnimation) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ui.animations.FlipTransition.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.animations.FlipTransition();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.animations.FlipTransition(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.animations.FlipTransition(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.animations.AnimationObject.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.animations.CommonTransitions.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.animations.ComponentAnimation.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ui.animations.MorphTransition.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ui.animations.Motion.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.ui.animations.Timeline.class) return invokeStatic5(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("createAnimationImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.AnimationObject.createAnimationImage((com.codename1.ui.Image) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.util.Resources.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.util.Resources.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.AnimationObject.createAnimationImage((java.lang.String) adaptedArgs[0], (com.codename1.ui.util.Resources) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.AnimationObject.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("createCover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createCover(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createDialogPulsate".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.animations.CommonTransitions.createDialogPulsate();
            }
        }
        if ("createEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.animations.CommonTransitions.createEmpty();
            }
        }
        if ("createFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createFade(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createFastSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createFastSlide(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createFastSlide(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]), ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("createSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createSlide(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createSlide(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]), ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("createSlideFadeTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createSlideFadeTitle(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createTimeline((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("createUncover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.CommonTransitions.createUncover(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("isDefaultLinearMotion".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.animations.CommonTransitions.isDefaultLinearMotion();
            }
        }
        if ("setDefaultLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.ui.animations.CommonTransitions.setDefaultLinearMotion(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.CommonTransitions.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("compoundAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.ComponentAnimation[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.ComponentAnimation[].class}, true);
                com.codename1.ui.animations.ComponentAnimation[] varArgs = new com.codename1.ui.animations.ComponentAnimation[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.animations.ComponentAnimation) adaptedArgs[i];
                }
                return com.codename1.ui.animations.ComponentAnimation.compoundAnimation(varArgs);
            }
        }
        if ("sequentialAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.ComponentAnimation[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.ComponentAnimation[].class}, true);
                com.codename1.ui.animations.ComponentAnimation[] varArgs = new com.codename1.ui.animations.ComponentAnimation[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.animations.ComponentAnimation) adaptedArgs[i];
                }
                return com.codename1.ui.animations.ComponentAnimation.sequentialAnimation(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.ComponentAnimation.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.animations.MorphTransition.create(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.MorphTransition.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("createCubicBezierMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.ui.animations.Motion.createCubicBezierMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue());
            }
        }
        if ("createDecelerationMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createDecelerationMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createDecelerationMotionFrom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Motion.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Motion.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createDecelerationMotionFrom((com.codename1.ui.animations.Motion) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createEaseInMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createEaseInMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createEaseInOutMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createEaseInOutMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createEaseMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createEaseMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createEaseOutMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createEaseOutMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createExponentialDecayMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.ui.animations.Motion.createExponentialDecayMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue());
            }
        }
        if ("createFrictionMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.ui.animations.Motion.createFrictionMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("createLinearColorMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createLinearColorMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createLinearMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("createSplineMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.animations.Motion.createSplineMotion(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("isSlowMotion".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.animations.Motion.isSlowMotion();
            }
        }
        if ("setSlowMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.ui.animations.Motion.setSlowMotion(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.Motion.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("createTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.animations.AnimationObject[].class, com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.animations.AnimationObject[].class, com.codename1.ui.geom.Dimension.class}, false);
                return com.codename1.ui.animations.Timeline.createTimeline(toIntValue(adaptedArgs[0]), (com.codename1.ui.animations.AnimationObject[]) adaptedArgs[1], (com.codename1.ui.geom.Dimension) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.Timeline.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.animations.BubbleTransition) {
            try {
                return invoke0((com.codename1.ui.animations.BubbleTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.CommonTransitions) {
            try {
                return invoke1((com.codename1.ui.animations.CommonTransitions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.FlipTransition) {
            try {
                return invoke2((com.codename1.ui.animations.FlipTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.MorphTransition) {
            try {
                return invoke3((com.codename1.ui.animations.MorphTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.AnimationObject) {
            try {
                return invoke4((com.codename1.ui.animations.AnimationObject) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.ComponentAnimation) {
            try {
                return invoke5((com.codename1.ui.animations.ComponentAnimation) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.ComponentAnimation.UIMutation) {
            try {
                return invoke6((com.codename1.ui.animations.ComponentAnimation.UIMutation) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Motion) {
            try {
                return invoke7((com.codename1.ui.animations.Motion) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Timeline) {
            try {
                return invoke8((com.codename1.ui.animations.Timeline) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Transition) {
            try {
                return invoke9((com.codename1.ui.animations.Transition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Animation) {
            try {
                return invoke10((com.codename1.ui.animations.Animation) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.animations.BubbleTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.copy(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestination();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                typedTarget.init((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setComponentName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setRoundBubble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRoundBubble(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.animations.CommonTransitions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.copy(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestination();
            }
        }
        if ("getMotion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMotion();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getTransitionSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTransitionSpeed();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                typedTarget.init((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("isForwardSlide".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isForwardSlide();
            }
        }
        if ("isHorizontalCover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHorizontalCover();
            }
        }
        if ("isHorizontalSlide".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHorizontalSlide();
            }
        }
        if ("isLinearMotion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLinearMotion();
            }
        }
        if ("isVerticalCover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVerticalCover();
            }
        }
        if ("isVerticalSlide".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVerticalSlide();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setLinearMotion(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Motion.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Motion.class}, false);
                typedTarget.setMotion((com.codename1.ui.animations.Motion) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.LazyValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.LazyValue.class}, false);
                typedTarget.setMotion((com.codename1.util.LazyValue) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.animations.FlipTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.copy(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getBgColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgColor();
            }
        }
        if ("getDestination".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestination();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                typedTarget.init((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBgColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.animations.MorphTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.copy(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestination();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                typedTarget.init((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.morph((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.morph((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.animations.AnimationObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.copy();
            }
        }
        if ("defineFrames".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineFrames(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("defineHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("defineMotionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineMotionX(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("defineMotionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineMotionY(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("defineOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineOpacity(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("defineOrientation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineOrientation(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("defineWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.defineWidth(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("getEndTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndTime();
            }
        }
        if ("getStartTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartTime();
            }
        }
        if ("setEndTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setEndTime(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setStartTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStartTime(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.animations.ComponentAnimation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOnCompleteCall".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addOnCompleteCall((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("completeAnimation".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.completeAnimation(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getMaxSteps".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxSteps();
            }
        }
        if ("getStep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStep();
            }
        }
        if ("isInProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInProgress();
            }
        }
        if ("isStepModeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStepModeSupported();
            }
        }
        if ("setNotifyLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setNotifyLock((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setOnCompletion((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStep(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("updateAnimationState".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.updateAnimationState(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.animations.ComponentAnimation.UIMutation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, com.codename1.ui.animations.ComponentAnimation.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, com.codename1.ui.animations.ComponentAnimation.class}, false);
                return typedTarget.add((com.codename1.ui.Container) adaptedArgs[0], (com.codename1.ui.animations.ComponentAnimation) adaptedArgs[1]);
            }
        }
        if ("isLocked".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLocked();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.animations.Motion typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("countAvailableVelocitySamplingPoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.countAvailableVelocitySamplingPoints();
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getCurrentMotionTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentMotionTime();
            }
        }
        if ("getDestinationValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationValue();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getSourceValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSourceValue();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getVelocity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVelocity();
            }
        }
        if ("isDecayMotion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDecayMotion();
            }
        }
        if ("isFinished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFinished();
            }
        }
        if ("setCurrentMotionTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setCurrentMotionTime(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setSourceValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSourceValue(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setStartTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setStartTime(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("start".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.start(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.animations.Timeline typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.AnimationObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.AnimationObject.class}, false);
                typedTarget.addAnimation((com.codename1.ui.animations.AnimationObject) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("applyMask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.applyMask((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.applyMask((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("applyMaskAutoScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.applyMaskAutoScale((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("asyncLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.asyncLock((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("createMask".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createMask();
            }
        }
        if ("dispose".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dispose(); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.fill(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("fireChangedEvent".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.fireChangedEvent(); return null;
            }
        }
        if ("flipHorizontally".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.flipHorizontally(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("flipVertically".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.flipVertically(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnimation(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getAnimationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getAnimationAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getAnimationCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationCount();
            }
        }
        if ("getAnimationDelay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationDelay();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getGraphics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGraphics();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImage();
            }
        }
        if ("getImageName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImageName();
            }
        }
        if ("getRGB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRGB();
            }
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                typedTarget.getRGB((int[]) adaptedArgs[0]); return null;
            }
        }
        if ("getRGBCached".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRGBCached();
            }
        }
        if ("getSVGDocument".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSVGDocument();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("isAnimation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAnimation();
            }
        }
        if ("isLocked".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLocked();
            }
        }
        if ("isLoop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoop();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isPause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPause();
            }
        }
        if ("isSVG".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSVG();
            }
        }
        if ("lock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirror".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mirror();
            }
        }
        if ("modifyAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                return typedTarget.modifyAlpha((byte) toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Integer.class}, false);
                return typedTarget.modifyAlpha((byte) toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("modifyAlphaWithTranslucency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                return typedTarget.modifyAlphaWithTranslucency((byte) toIntValue(adaptedArgs[0]));
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.geom.Rectangle) adaptedArgs[1]); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("requiresDrawImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.requiresDrawImage();
            }
        }
        if ("rotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.rotate(toIntValue(adaptedArgs[0]));
            }
        }
        if ("rotate180Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.rotate180Degrees(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("rotate270Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.rotate270Degrees(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("rotate90Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.rotate90Degrees(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.scale(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("scaled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.scaled(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("scaledHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.scaledHeight(toIntValue(adaptedArgs[0]));
            }
        }
        if ("scaledLargerRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.scaledLargerRatio(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("scaledSmallerRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.scaledSmallerRatio(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("scaledWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.scaledWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setAnimationDelay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAnimationDelay(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setImageName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setImageName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setLoop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setLoop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPause(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTime(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("subImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.subImage(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), ((Boolean) adaptedArgs[4]).booleanValue());
            }
        }
        if ("toRGB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.RGBImage.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.RGBImage.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.toRGB((com.codename1.ui.RGBImage) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("unlock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.animations.Transition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.copy(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestination();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                typedTarget.init((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.animations.Animation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.animations.AnimationObject.class) return getStaticField0(name);
        if (type == com.codename1.ui.animations.CommonTransitions.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("MOTION_TYPE_LINEAR".equals(name)) return com.codename1.ui.animations.AnimationObject.MOTION_TYPE_LINEAR;
        if ("MOTION_TYPE_SPLINE".equals(name)) return com.codename1.ui.animations.AnimationObject.MOTION_TYPE_SPLINE;
        throw unsupportedStaticField(com.codename1.ui.animations.AnimationObject.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("SLIDE_HORIZONTAL".equals(name)) return com.codename1.ui.animations.CommonTransitions.SLIDE_HORIZONTAL;
        if ("SLIDE_VERTICAL".equals(name)) return com.codename1.ui.animations.CommonTransitions.SLIDE_VERTICAL;
        throw unsupportedStaticField(com.codename1.ui.animations.CommonTransitions.class, name);
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
