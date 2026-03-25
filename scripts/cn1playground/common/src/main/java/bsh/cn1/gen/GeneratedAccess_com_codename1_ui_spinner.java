package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_spinner {
    private GeneratedAccess_com_codename1_ui_spinner() {
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
        if ("BaseSpinner".equals(simpleName)) {
            return com.codename1.ui.spinner.BaseSpinner.class;
        }
        if ("DateSpinner".equals(simpleName)) {
            return com.codename1.ui.spinner.DateSpinner.class;
        }
        if ("DateTimeSpinner".equals(simpleName)) {
            return com.codename1.ui.spinner.DateTimeSpinner.class;
        }
        if ("GenericSpinner".equals(simpleName)) {
            return com.codename1.ui.spinner.GenericSpinner.class;
        }
        if ("NumericSpinner".equals(simpleName)) {
            return com.codename1.ui.spinner.NumericSpinner.class;
        }
        if ("Picker".equals(simpleName)) {
            return com.codename1.ui.spinner.Picker.class;
        }
        if ("TimeSpinner".equals(simpleName)) {
            return com.codename1.ui.spinner.TimeSpinner.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.spinner.DateSpinner.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.spinner.DateSpinner();
            }
        }
        if (type == com.codename1.ui.spinner.DateTimeSpinner.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.spinner.DateTimeSpinner();
            }
        }
        if (type == com.codename1.ui.spinner.GenericSpinner.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.spinner.GenericSpinner();
            }
        }
        if (type == com.codename1.ui.spinner.NumericSpinner.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.spinner.NumericSpinner();
            }
        }
        if (type == com.codename1.ui.spinner.Picker.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.spinner.Picker();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.spinner.Picker.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("isDefaultUseLightweightPopup".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.spinner.Picker.isDefaultUseLightweightPopup();
            }
        }
        if ("setDefaultUseLightweightPopup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.ui.spinner.Picker.setDefaultUseLightweightPopup(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.spinner.Picker.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.spinner.DateTimeSpinner) {
            try {
                return invoke0((com.codename1.ui.spinner.DateTimeSpinner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.spinner.DateSpinner) {
            try {
                return invoke1((com.codename1.ui.spinner.DateSpinner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.spinner.GenericSpinner) {
            try {
                return invoke2((com.codename1.ui.spinner.GenericSpinner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.spinner.NumericSpinner) {
            try {
                return invoke3((com.codename1.ui.spinner.NumericSpinner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.spinner.TimeSpinner) {
            try {
                return invoke4((com.codename1.ui.spinner.TimeSpinner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.spinner.BaseSpinner) {
            try {
                return invoke5((com.codename1.ui.spinner.BaseSpinner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.spinner.Picker) {
            try {
                return invoke6((com.codename1.ui.spinner.Picker) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.spinner.DateTimeSpinner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCurrentDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentDate();
            }
        }
        if ("getCurrentHour".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentHour();
            }
        }
        if ("getCurrentMinute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentMinute();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getEndDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndDate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getMinuteStep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinuteStep();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStartDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartDate();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isCurrentMeridiem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCurrentMeridiem();
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isDurationMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDurationMode();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isIncludeYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIncludeYear();
            }
        }
        if ("isMarkToday".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMarkToday();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isShowMeridiem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowMeridiem();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCurrentDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setCurrentDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setCurrentHour".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentHour(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCurrentMeridiem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCurrentMeridiem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCurrentMinute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentMinute(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDurationMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDurationMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEndDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setEndDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHoursVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHoursVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIncludeYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIncludeYear(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setMarkToday".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setMarkToday(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMinuteStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMinuteStep(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setMinutesVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setMinutesVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowMeridiem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowMeridiem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setStartDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.spinner.DateSpinner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCurrentDay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentDay();
            }
        }
        if ("getCurrentMonth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentMonth();
            }
        }
        if ("getCurrentYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentYear();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getEndYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndYear();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStartYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartYear();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isMonthDayYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMonthDayYear();
            }
        }
        if ("isNumericMonths".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNumericMonths();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCurrentDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentDay(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCurrentMonth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentMonth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCurrentYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentYear(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEndYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setEndYear(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setMonthDayYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setMonthDayYear(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMonthRenderingPrototype".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setMonthRenderingPrototype((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNumericMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNumericMonths(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStartYear(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.spinner.GenericSpinner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getColumns".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColumns();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModel();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getModel(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getRenderer(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getRenderingPrototype".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderingPrototype();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getRenderingPrototype(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setColumns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setColumns(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setModel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.ListModel.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.list.ListModel.class}, false);
                typedTarget.setModel((com.codename1.ui.list.ListModel) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.list.ListModel.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.list.ListModel.class}, false);
                typedTarget.setModel(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.list.ListModel) adaptedArgs[1]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false);
                typedTarget.setRenderer((com.codename1.ui.list.ListCellRenderer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.list.ListCellRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.list.ListCellRenderer.class}, false);
                typedTarget.setRenderer(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.list.ListCellRenderer) adaptedArgs[1]); return null;
            }
        }
        if ("setRenderingPrototype".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRenderingPrototype((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.setRenderingPrototype(((Number) adaptedArgs[0]).intValue(), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setValue((java.lang.Object) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.setValue(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.spinner.NumericSpinner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getMax".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMax();
            }
        }
        if ("getMin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMin();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStep();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setMax(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setMin(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setStep(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setValue(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.spinner.TimeSpinner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCurrentHour".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentHour();
            }
        }
        if ("getCurrentMinute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentMinute();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getMinuteStep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinuteStep();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isCurrentMeridiem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCurrentMeridiem();
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isDurationMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDurationMode();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isShowMeridiem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowMeridiem();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCurrentHour".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentHour(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCurrentMeridiem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCurrentMeridiem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCurrentMinute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCurrentMinute(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDurationMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDurationMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHoursVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHoursVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setMinuteStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMinuteStep(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setMinutesVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setMinutesVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowMeridiem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowMeridiem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.spinner.BaseSpinner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.spinner.Picker typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("bindStateTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                typedTarget.bindStateTo((com.codename1.ui.Button) adaptedArgs[0]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getActionListeners".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionListeners();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBadgeStyleComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBadgeStyleComponent();
            }
        }
        if ("getBadgeText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBadgeText();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDate();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationHours".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationHours();
            }
        }
        if ("getDurationMinutes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMinutes();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getEndDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndDate();
            }
        }
        if ("getFontIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontIcon();
            }
        }
        if ("getFontIconSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontIconSize();
            }
        }
        if ("getFormatter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormatter();
            }
        }
        if ("getGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGap();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconFromState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconFromState();
            }
        }
        if ("getIconStyleComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconStyleComponent();
            }
        }
        if ("getIconUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconUIID();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getListeners".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getListeners();
            }
        }
        if ("getMask".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMask();
            }
        }
        if ("getMaskName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaskName();
            }
        }
        if ("getMaskedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaskedIcon();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getMaxAutoSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxAutoSize();
            }
        }
        if ("getMaxHour".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxHour();
            }
        }
        if ("getMinAutoSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinAutoSize();
            }
        }
        if ("getMinHour".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinHour();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredPopupHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredPopupHeight();
            }
        }
        if ("getPreferredPopupWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredPopupWidth();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getReleaseRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReleaseRadius();
            }
        }
        if ("getRenderingPrototype".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderingPrototype();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("getRolloverPressedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRolloverPressedIcon();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedString();
            }
        }
        if ("getSelectedStringIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStringIndex();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getShiftMillimeters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShiftMillimeters();
            }
        }
        if ("getShiftMillimetersF".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShiftMillimetersF();
            }
        }
        if ("getShiftText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShiftText();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStartDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartDate();
            }
        }
        if ("getState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getState();
            }
        }
        if ("getStringWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                return typedTarget.getStringWidth((com.codename1.ui.Font) adaptedArgs[0]);
            }
        }
        if ("getStrings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrings();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("getTextPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextPosition();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getVerticalAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalAlignment();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isAutoRelease".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoRelease();
            }
        }
        if ("isAutoSizeMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoSizeMode();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCapsText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCapsText();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isEndsWith3Points".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEndsWith3Points();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isLegacyRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLegacyRenderer();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOppositeSide".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOppositeSide();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSelected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSelected();
            }
        }
        if ("isShouldLocalize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShouldLocalize();
            }
        }
        if ("isShowEvenIfBlank".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowEvenIfBlank();
            }
        }
        if ("isShowMeridiem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowMeridiem();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTextSelectionEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTextSelectionEnabled();
            }
        }
        if ("isTickerEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTickerEnabled();
            }
        }
        if ("isTickerRunning".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTickerRunning();
            }
        }
        if ("isToggle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isToggle();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isUseLightweightPopup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseLightweightPopup();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pressed".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pressed(); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("released".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.released(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.released(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAlignment(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAutoRelease".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutoRelease(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAutoSizeMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutoSizeMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBadgeText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setBadgeText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setBadgeUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setBadgeUIID((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCapsText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCapsText(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Command.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Command.class}, false);
                typedTarget.setCommand((com.codename1.ui.Command) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setDisabledIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setDuration(((Number) adaptedArgs[0]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setDuration(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEndDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setEndDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEndsWith3Points(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFontIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setFontIcon(((Character) adaptedArgs[0]).charValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Character.class}, false);
                typedTarget.setFontIcon((com.codename1.ui.Font) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Character.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Character.class, java.lang.Float.class}, false);
                typedTarget.setFontIcon((com.codename1.ui.Font) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setFormatter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.l10n.SimpleDateFormat.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.l10n.SimpleDateFormat.class}, false);
                typedTarget.setFormatter((com.codename1.l10n.SimpleDateFormat) adaptedArgs[0]); return null;
            }
        }
        if ("setGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setGap(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHourRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setHourRange(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setIconUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setIconUIID((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLegacyRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setLegacyRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setMask((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setMaskName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setMaskName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setMaterialIcon(((Character) adaptedArgs[0]).charValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Float.class}, false);
                typedTarget.setMaterialIcon(((Character) adaptedArgs[0]).charValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setMaxAutoSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaxAutoSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMinAutoSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMinAutoSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMinuteStep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMinuteStep(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredPopupHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredPopupHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredPopupWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredPopupWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPressedIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReleaseRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setReleaseRadius(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setReleased".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.setReleased(); return null;
            }
        }
        if ("setRenderingPrototype".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setRenderingPrototype((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setRolloverIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setRolloverPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setRolloverPressedIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectedString((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStringIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSelectedStringIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShiftMillimeters".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setShiftMillimeters(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setShiftMillimeters(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setShiftText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setShiftText(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShouldLocalize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldLocalize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowEvenIfBlank".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowEvenIfBlank(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowMeridiem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowMeridiem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setStartDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.setStrings(varArgs); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTextPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTextPosition(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTextSelectionEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTextSelectionEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTickerEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTickerEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTime(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setTime(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setToggle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setToggle(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setType(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setUseLightweightPopup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseLightweightPopup(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setVerticalAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setVerticalAlignment(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("shouldTickerStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shouldTickerStart();
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("startTicker".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startTicker(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Boolean.class}, false);
                typedTarget.startTicker(((Number) adaptedArgs[0]).longValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stopTicker".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopTicker(); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("unbindStateFrom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                typedTarget.unbindStateFrom((com.codename1.ui.Button) adaptedArgs[0]); return null;
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
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
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
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
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            return isSamInterface(type);
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
