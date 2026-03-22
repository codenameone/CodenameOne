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
                return new com.codename1.ui.animations.BubbleTransition();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.ui.animations.BubbleTransition(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.animations.BubbleTransition(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.ui.animations.FlipTransition.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.animations.FlipTransition();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.ui.animations.FlipTransition(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.animations.FlipTransition(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
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
                return com.codename1.ui.animations.AnimationObject.createAnimationImage((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createAnimationImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.util.Resources.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.AnimationObject.createAnimationImage((java.lang.String) safeArgs[0], (com.codename1.ui.util.Resources) safeArgs[1], ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.AnimationObject.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("createCover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createCover(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createDialogPulsate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.animations.CommonTransitions.createDialogPulsate();
            }
        }
        if ("createEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.animations.CommonTransitions.createEmpty();
            }
        }
        if ("createFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createFade(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createFastSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createFastSlide(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createFastSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createFastSlide(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue());
            }
        }
        if ("createSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createSlide(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createSlide(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue());
            }
        }
        if ("createSlideFadeTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createSlideFadeTitle(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createTimeline((com.codename1.ui.Image) safeArgs[0]);
            }
        }
        if ("createUncover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.CommonTransitions.createUncover(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("isDefaultLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.animations.CommonTransitions.isDefaultLinearMotion();
            }
        }
        if ("setDefaultLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.animations.CommonTransitions.setDefaultLinearMotion(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.CommonTransitions.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("compoundAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.ComponentAnimation[].class}, true)) {
                com.codename1.ui.animations.ComponentAnimation[] varArgs = new com.codename1.ui.animations.ComponentAnimation[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.animations.ComponentAnimation) safeArgs[i];
                }
                return com.codename1.ui.animations.ComponentAnimation.compoundAnimation(varArgs);
            }
        }
        if ("sequentialAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.ComponentAnimation[].class}, true)) {
                com.codename1.ui.animations.ComponentAnimation[] varArgs = new com.codename1.ui.animations.ComponentAnimation[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.animations.ComponentAnimation) safeArgs[i];
                }
                return com.codename1.ui.animations.ComponentAnimation.sequentialAnimation(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.ComponentAnimation.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.MorphTransition.create(((Number) safeArgs[0]).intValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.MorphTransition.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("createCubicBezierMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                return com.codename1.ui.animations.Motion.createCubicBezierMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).floatValue(), ((Number) safeArgs[6]).floatValue());
            }
        }
        if ("createDecelerationMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createDecelerationMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createDecelerationMotionFrom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Motion.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createDecelerationMotionFrom((com.codename1.ui.animations.Motion) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createEaseInMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createEaseInMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createEaseInOutMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createEaseInOutMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createEaseMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createEaseMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createEaseOutMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createEaseOutMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createExponentialDecayMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.ui.animations.Motion.createExponentialDecayMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        if ("createFrictionMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                return com.codename1.ui.animations.Motion.createFrictionMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue());
            }
        }
        if ("createLinearColorMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createLinearColorMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createLinearMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createSplineMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.animations.Motion.createSplineMotion(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("isSlowMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.animations.Motion.isSlowMotion();
            }
        }
        if ("setSlowMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.animations.Motion.setSlowMotion(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.animations.Motion.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("createTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.animations.AnimationObject[].class, com.codename1.ui.geom.Dimension.class}, false)) {
                return com.codename1.ui.animations.Timeline.createTimeline(((Number) safeArgs[0]).intValue(), (com.codename1.ui.animations.AnimationObject[]) safeArgs[1], (com.codename1.ui.geom.Dimension) safeArgs[2]);
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
        if (target instanceof com.codename1.ui.animations.Motion) {
            try {
                return invoke6((com.codename1.ui.animations.Motion) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Timeline) {
            try {
                return invoke7((com.codename1.ui.animations.Timeline) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Transition) {
            try {
                return invoke8((com.codename1.ui.animations.Transition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.animations.Animation) {
            try {
                return invoke9((com.codename1.ui.animations.Animation) target, name, safeArgs);
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.copy(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestination();
            }
        }
        if ("getDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDuration();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.init((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("setComponentName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setComponentName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDuration(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setRoundBubble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setRoundBubble(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.animations.CommonTransitions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.copy(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestination();
            }
        }
        if ("getMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMotion();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("getTransitionSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTransitionSpeed();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.init((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("isForwardSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isForwardSlide();
            }
        }
        if ("isHorizontalCover".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHorizontalCover();
            }
        }
        if ("isHorizontalSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHorizontalSlide();
            }
        }
        if ("isLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLinearMotion();
            }
        }
        if ("isVerticalCover".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isVerticalCover();
            }
        }
        if ("isVerticalSlide".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isVerticalSlide();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("setLinearMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setLinearMotion(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Motion.class}, false)) {
                typedTarget.setMotion((com.codename1.ui.animations.Motion) safeArgs[0]); return null;
            }
        }
        if ("setMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.LazyValue.class}, false)) {
                typedTarget.setMotion((com.codename1.util.LazyValue) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.animations.FlipTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.copy(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBgColor();
            }
        }
        if ("getDestination".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestination();
            }
        }
        if ("getDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDuration();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.init((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("setBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBgColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDuration(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.animations.MorphTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.copy(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestination();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.init((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.morph((java.lang.String) safeArgs[0]);
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.morph((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.animations.AnimationObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.copy();
            }
        }
        if ("defineFrames".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineFrames(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("defineHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineHeight(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("defineMotionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineMotionX(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("defineMotionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineMotionY(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("defineOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineOpacity(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("defineOrientation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineOrientation(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("defineWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.defineWidth(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("getEndTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEndTime();
            }
        }
        if ("getStartTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStartTime();
            }
        }
        if ("setEndTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setEndTime(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setStartTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setStartTime(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.animations.ComponentAnimation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOnCompleteCall".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                typedTarget.addOnCompleteCall((java.lang.Runnable) safeArgs[0]); return null;
            }
        }
        if ("flush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flush(); return null;
            }
        }
        if ("getMaxSteps".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxSteps();
            }
        }
        if ("getStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStep();
            }
        }
        if ("isInProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInProgress();
            }
        }
        if ("isStepModeSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isStepModeSupported();
            }
        }
        if ("setNotifyLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.setNotifyLock((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("setOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                typedTarget.setOnCompletion((java.lang.Runnable) safeArgs[0]); return null;
            }
        }
        if ("setStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setStep(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("updateAnimationState".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.updateAnimationState(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.animations.Motion typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("countAvailableVelocitySamplingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.countAvailableVelocitySamplingPoints();
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("getCurrentMotionTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCurrentMotionTime();
            }
        }
        if ("getDestinationValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationValue();
            }
        }
        if ("getDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDuration();
            }
        }
        if ("getSourceValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSourceValue();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValue();
            }
        }
        if ("getVelocity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVelocity();
            }
        }
        if ("isDecayMotion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDecayMotion();
            }
        }
        if ("isFinished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFinished();
            }
        }
        if ("setCurrentMotionTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setCurrentMotionTime(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setSourceValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSourceValue(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setStartTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setStartTime(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.start(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.animations.Timeline typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.AnimationObject.class}, false)) {
                typedTarget.addAnimation((com.codename1.ui.animations.AnimationObject) safeArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("applyMask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.applyMask((java.lang.Object) safeArgs[0]);
            }
        }
        if ("applyMask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.applyMask((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("applyMaskAutoScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.applyMaskAutoScale((java.lang.Object) safeArgs[0]);
            }
        }
        if ("asyncLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.asyncLock((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("createMask".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.createMask();
            }
        }
        if ("dispose".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.dispose(); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.fill(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("fireChangedEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.fireChangedEvent(); return null;
            }
        }
        if ("flipHorizontally".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.flipHorizontally(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("flipVertically".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.flipVertically(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnimation(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnimationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.getAnimationAt(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getAnimationCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnimationCount();
            }
        }
        if ("getAnimationDelay".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnimationDelay();
            }
        }
        if ("getDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDuration();
            }
        }
        if ("getGraphics".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGraphics();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHeight();
            }
        }
        if ("getImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getImage();
            }
        }
        if ("getImageName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getImageName();
            }
        }
        if ("getRGB".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRGB();
            }
        }
        if ("getRGB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                typedTarget.getRGB((int[]) safeArgs[0]); return null;
            }
        }
        if ("getRGBCached".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRGBCached();
            }
        }
        if ("getSVGDocument".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSVGDocument();
            }
        }
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSize();
            }
        }
        if ("getTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTime();
            }
        }
        if ("getWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWidth();
            }
        }
        if ("isAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAnimation();
            }
        }
        if ("isLocked".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLocked();
            }
        }
        if ("isLoop".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLoop();
            }
        }
        if ("isOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isOpaque();
            }
        }
        if ("isPause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPause();
            }
        }
        if ("isSVG".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSVG();
            }
        }
        if ("lock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirror".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mirror();
            }
        }
        if ("modifyAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                return typedTarget.modifyAlpha(((Number) safeArgs[0]).byteValue());
            }
        }
        if ("modifyAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Integer.class}, false)) {
                return typedTarget.modifyAlpha(((Number) safeArgs[0]).byteValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("modifyAlphaWithTranslucency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                return typedTarget.modifyAlphaWithTranslucency(((Number) safeArgs[0]).byteValue());
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.geom.Rectangle) safeArgs[1]); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("requiresDrawImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.requiresDrawImage();
            }
        }
        if ("rotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.rotate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("rotate180Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.rotate180Degrees(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("rotate270Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.rotate270Degrees(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("rotate90Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.rotate90Degrees(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.scale(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("scaled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.scaled(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("scaledHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.scaledHeight(((Number) safeArgs[0]).intValue());
            }
        }
        if ("scaledLargerRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.scaledLargerRatio(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("scaledSmallerRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.scaledSmallerRatio(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("scaledWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.scaledWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("setAnimationDelay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAnimationDelay(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setImageName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setImageName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setLoop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setLoop(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPause(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTime(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("subImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.subImage(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Boolean) safeArgs[4]).booleanValue());
            }
        }
        if ("toRGB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.RGBImage.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.toRGB((com.codename1.ui.RGBImage) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("unlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.animations.Transition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.copy(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getDestination".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestination();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.init((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("initTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.animations.Animation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.animations.AnimationObject.class) {
            if ("MOTION_TYPE_LINEAR".equals(name)) return com.codename1.ui.animations.AnimationObject.MOTION_TYPE_LINEAR;
            if ("MOTION_TYPE_SPLINE".equals(name)) return com.codename1.ui.animations.AnimationObject.MOTION_TYPE_SPLINE;
        }
        if (type == com.codename1.ui.animations.CommonTransitions.class) {
            if ("SLIDE_HORIZONTAL".equals(name)) return com.codename1.ui.animations.CommonTransitions.SLIDE_HORIZONTAL;
            if ("SLIDE_VERTICAL".equals(name)) return com.codename1.ui.animations.CommonTransitions.SLIDE_VERTICAL;
        }
        throw unsupportedStaticField(type, name);
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
            return value instanceof Number;
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
