package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_events {
    private GeneratedAccess_com_codename1_ui_events() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.ui.events.ActionEvent".equals(name)) return com.codename1.ui.events.ActionEvent.class;
        if ("com.codename1.ui.events.ActionListener".equals(name)) return com.codename1.ui.events.ActionListener.class;
        if ("com.codename1.ui.events.ActionSource".equals(name)) return com.codename1.ui.events.ActionSource.class;
        if ("com.codename1.ui.events.BrowserNavigationCallback".equals(name)) return com.codename1.ui.events.BrowserNavigationCallback.class;
        if ("com.codename1.ui.events.ComponentStateChangeEvent".equals(name)) return com.codename1.ui.events.ComponentStateChangeEvent.class;
        if ("com.codename1.ui.events.DataChangedListener".equals(name)) return com.codename1.ui.events.DataChangedListener.class;
        if ("com.codename1.ui.events.FocusListener".equals(name)) return com.codename1.ui.events.FocusListener.class;
        if ("com.codename1.ui.events.MessageEvent".equals(name)) return com.codename1.ui.events.MessageEvent.class;
        if ("com.codename1.ui.events.ScrollListener".equals(name)) return com.codename1.ui.events.ScrollListener.class;
        if ("com.codename1.ui.events.SelectionListener".equals(name)) return com.codename1.ui.events.SelectionListener.class;
        if ("com.codename1.ui.events.StyleListener".equals(name)) return com.codename1.ui.events.StyleListener.class;
        if ("com.codename1.ui.events.WindowEvent".equals(name)) return com.codename1.ui.events.WindowEvent.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.events.ActionEvent.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], (com.codename1.ui.events.ActionEvent.Type) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], (com.codename1.ui.events.ActionEvent.Type) safeArgs[1], ((Number) safeArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Command.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Command) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionEvent.Type.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], (com.codename1.ui.events.ActionEvent.Type) safeArgs[1], ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Command.class, com.codename1.ui.events.ActionEvent.Type.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Command) safeArgs[0], (com.codename1.ui.events.ActionEvent.Type) safeArgs[1], (com.codename1.ui.Component) safeArgs[2], ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.events.ActionEvent.Type.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.ActionEvent((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.events.ActionEvent.Type) safeArgs[1], (com.codename1.ui.Component) safeArgs[2], ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue());
            }
        }
        if (type == com.codename1.ui.events.ComponentStateChangeEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.ui.events.ComponentStateChangeEvent((com.codename1.ui.Component) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.ui.events.MessageEvent.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.events.MessageEvent((java.lang.Object) safeArgs[0], (java.lang.String) safeArgs[1], ((Number) safeArgs[2]).intValue());
            }
        }
        if (type == com.codename1.ui.events.WindowEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Display.class, com.codename1.ui.events.WindowEvent.Type.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                return new com.codename1.ui.events.WindowEvent((com.codename1.ui.Display) safeArgs[0], (com.codename1.ui.events.WindowEvent.Type) safeArgs[1], (com.codename1.ui.geom.Rectangle) safeArgs[2]);
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
        if (target instanceof com.codename1.ui.events.ActionSource) {
            try {
                return invoke4((com.codename1.ui.events.ActionSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.BrowserNavigationCallback) {
            try {
                return invoke5((com.codename1.ui.events.BrowserNavigationCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.DataChangedListener) {
            try {
                return invoke6((com.codename1.ui.events.DataChangedListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.FocusListener) {
            try {
                return invoke7((com.codename1.ui.events.FocusListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.ScrollListener) {
            try {
                return invoke8((com.codename1.ui.events.ScrollListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.SelectionListener) {
            try {
                return invoke9((com.codename1.ui.events.SelectionListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.events.StyleListener) {
            try {
                return invoke10((com.codename1.ui.events.StyleListener) target, name, safeArgs);
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgress();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getY();
            }
        }
        if ("isConsumed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isConsumed();
            }
        }
        if ("isInitialized".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInitialized();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPointerPressedDuringDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.events.MessageEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCode();
            }
        }
        if ("getCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgress();
            }
        }
        if ("getPromptPromise".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPromptPromise();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getY();
            }
        }
        if ("isConsumed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("isPromptForAudioPlayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPromptForAudioPlayer();
            }
        }
        if ("isPromptForAudioRecorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPromptForAudioRecorder();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPointerPressedDuringDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.events.WindowEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds();
            }
        }
        if ("getCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgress();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("getType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getType();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getY();
            }
        }
        if ("isConsumed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPointerPressedDuringDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.events.ActionEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponent();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getEventType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgress();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getY();
            }
        }
        if ("isConsumed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPointerPressedDuringDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.events.ActionSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.events.BrowserNavigationCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("shouldNavigate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.shouldNavigate((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.events.DataChangedListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("dataChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.dataChanged(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.events.FocusListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("focusGained".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.focusGained((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("focusLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.focusLost((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.events.ScrollListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("scrollChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.scrollChanged(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.events.SelectionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("selectionChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.selectionChanged(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.events.StyleListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.styleChanged((java.lang.String) safeArgs[0], (com.codename1.ui.plaf.Style) safeArgs[1]); return null;
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
