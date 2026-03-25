package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_events {
    private GeneratedAccess_com_codename1_ui_events() {
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
        if ("ActionEvent".equals(simpleName)) {
            return com.codename1.ui.events.ActionEvent.class;
        }
        if ("ActionListener".equals(simpleName)) {
            return com.codename1.ui.events.ActionListener.class;
        }
        if ("ActionSource".equals(simpleName)) {
            return com.codename1.ui.events.ActionSource.class;
        }
        if ("BrowserNavigationCallback".equals(simpleName)) {
            return com.codename1.ui.events.BrowserNavigationCallback.class;
        }
        if ("ComponentStateChangeEvent".equals(simpleName)) {
            return com.codename1.ui.events.ComponentStateChangeEvent.class;
        }
        if ("DataChangedListener".equals(simpleName)) {
            return com.codename1.ui.events.DataChangedListener.class;
        }
        if ("FocusListener".equals(simpleName)) {
            return com.codename1.ui.events.FocusListener.class;
        }
        if ("MessageEvent".equals(simpleName)) {
            return com.codename1.ui.events.MessageEvent.class;
        }
        if ("ScrollListener".equals(simpleName)) {
            return com.codename1.ui.events.ScrollListener.class;
        }
        if ("SelectionListener".equals(simpleName)) {
            return com.codename1.ui.events.SelectionListener.class;
        }
        if ("StyleListener".equals(simpleName)) {
            return com.codename1.ui.events.StyleListener.class;
        }
        if ("WindowEvent".equals(simpleName)) {
            return com.codename1.ui.events.WindowEvent.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.events.ActionEvent.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionEvent.Type) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionEvent.Type) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Command.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Command.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Command) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionEvent.Type) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Boolean) adaptedArgs[3]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Command.class, com.codename1.ui.events.ActionEvent.Type.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Command.class, com.codename1.ui.events.ActionEvent.Type.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Command) adaptedArgs[0], (com.codename1.ui.events.ActionEvent.Type) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.events.ActionEvent.Type.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.events.ActionEvent.Type.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.events.ActionEvent.Type) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue());
            }
        }
        if (type == com.codename1.ui.events.ComponentStateChangeEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false);
                return new com.codename1.ui.events.ComponentStateChangeEvent((com.codename1.ui.Component) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.ui.events.MessageEvent.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.events.MessageEvent((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue());
            }
        }
        if (type == com.codename1.ui.events.WindowEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Display.class, com.codename1.ui.events.WindowEvent.Type.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Display.class, com.codename1.ui.events.WindowEvent.Type.class, com.codename1.ui.geom.Rectangle.class}, false);
                return new com.codename1.ui.events.WindowEvent((com.codename1.ui.Display) adaptedArgs[0], (com.codename1.ui.events.WindowEvent.Type) adaptedArgs[1], (com.codename1.ui.geom.Rectangle) adaptedArgs[2]);
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
        if (target instanceof com.codename1.ui.events.ComponentStateChangeEvent) {
            try {
                return invoke0((com.codename1.ui.events.ComponentStateChangeEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.MessageEvent) {
            try {
                return invoke1((com.codename1.ui.events.MessageEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.WindowEvent) {
            try {
                return invoke2((com.codename1.ui.events.WindowEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.ActionEvent) {
            try {
                return invoke3((com.codename1.ui.events.ActionEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.ActionListener) {
            try {
                return invoke4((com.codename1.ui.events.ActionListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.ActionSource) {
            try {
                return invoke5((com.codename1.ui.events.ActionSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.BrowserNavigationCallback) {
            try {
                return invoke6((com.codename1.ui.events.BrowserNavigationCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.DataChangedListener) {
            try {
                return invoke7((com.codename1.ui.events.DataChangedListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.FocusListener) {
            try {
                return invoke8((com.codename1.ui.events.FocusListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.ScrollListener) {
            try {
                return invoke9((com.codename1.ui.events.ScrollListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.SelectionListener) {
            try {
                return invoke10((com.codename1.ui.events.SelectionListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.StyleListener) {
            try {
                return invoke11((com.codename1.ui.events.StyleListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.events.ComponentStateChangeEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgress();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
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
        if ("isConsumed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConsumed();
            }
        }
        if ("isInitialized".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInitialized();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPointerPressedDuringDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.events.MessageEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCode();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgress();
            }
        }
        if ("getPromptPromise".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPromptPromise();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
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
        if ("isConsumed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("isPromptForAudioPlayer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPromptForAudioPlayer();
            }
        }
        if ("isPromptForAudioRecorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPromptForAudioRecorder();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPointerPressedDuringDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.events.WindowEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgress();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
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
        if ("isConsumed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPointerPressedDuringDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.events.ActionEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgress();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
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
        if ("isConsumed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPointerPressedDuringDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.events.ActionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false);
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.events.ActionSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.events.BrowserNavigationCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("shouldNavigate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.shouldNavigate((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.events.DataChangedListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("dataChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.dataChanged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.events.FocusListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("focusGained".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.focusGained((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("focusLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.focusLost((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.events.ScrollListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("scrollChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.scrollChanged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.events.SelectionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("selectionChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.selectionChanged(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ui.events.StyleListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            if ("ADDED".equals(name)) return com.codename1.ui.events.DataChangedListener.ADDED;
            if ("CHANGED".equals(name)) return com.codename1.ui.events.DataChangedListener.CHANGED;
            if ("REMOVED".equals(name)) return com.codename1.ui.events.DataChangedListener.REMOVED;
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
