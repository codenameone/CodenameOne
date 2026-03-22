package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codenameone_playground {
    private GeneratedAccess_com_codenameone_playground() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codenameone.playground.CN1Playground".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codenameone.playground -> com.codenameone.playground.CN1Playground");
            }
            return com.codenameone.playground.CN1Playground.class;
        }
        if ("com.codenameone.playground.PlaygroundContext".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codenameone.playground -> com.codenameone.playground.PlaygroundContext");
            }
            return com.codenameone.playground.PlaygroundContext.class;
        }
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codenameone.playground.PlaygroundContext.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class, com.codename1.ui.Container.class, com.codename1.ui.util.Resources.class, com.codenameone.playground.PlaygroundContext.Logger.class}, false)) {
                return new com.codenameone.playground.PlaygroundContext((com.codename1.ui.Form) safeArgs[0], (com.codename1.ui.Container) safeArgs[1], (com.codename1.ui.util.Resources) safeArgs[2], (com.codenameone.playground.PlaygroundContext.Logger) safeArgs[3]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codenameone.playground.PlaygroundContext.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("debug".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codenameone.playground.PlaygroundContext.debug((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("getCurrent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codenameone.playground.PlaygroundContext.getCurrent();
            }
        }
        if ("interceptMethodInvocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Object[].class}, false)) {
                return com.codenameone.playground.PlaygroundContext.interceptMethodInvocation((java.lang.Object) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.Object[]) safeArgs[2]);
            }
        }
        throw unsupportedStatic(com.codenameone.playground.PlaygroundContext.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codenameone.playground.CN1Playground) {
            try {
                return invoke0((com.codenameone.playground.CN1Playground) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codenameone.playground.PlaygroundContext) {
            try {
                return invoke1((com.codenameone.playground.PlaygroundContext) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codenameone.playground.CN1Playground typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("destroy".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.destroy(); return null;
            }
        }
        if ("getTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTheme();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.init((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("runApp".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.runApp(); return null;
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.start(); return null;
            }
        }
        if ("stop".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codenameone.playground.PlaygroundContext typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("captureShownForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                typedTarget.captureShownForm((com.codename1.ui.Form) safeArgs[0]); return null;
            }
        }
        if ("clearPreview".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearPreview(); return null;
            }
        }
        if ("clearShownForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearShownForm(); return null;
            }
        }
        if ("getHostForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHostForm();
            }
        }
        if ("getPreviewRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPreviewRoot();
            }
        }
        if ("getShownForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShownForm();
            }
        }
        if ("getTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTheme();
            }
        }
        if ("log".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.log((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("refreshPreview".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.refreshPreview(); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setTitle((java.lang.String) safeArgs[0]); return null;
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
