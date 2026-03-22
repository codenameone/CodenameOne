package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_system {
    private GeneratedAccess_com_codename1_system() {
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
        if ("CrashReport".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.system -> com.codename1.system.CrashReport");
            }
            return com.codename1.system.CrashReport.class;
        }
        if ("DefaultCrashReporter".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.system -> com.codename1.system.DefaultCrashReporter");
            }
            return com.codename1.system.DefaultCrashReporter.class;
        }
        if ("Lifecycle".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.system -> com.codename1.system.Lifecycle");
            }
            return com.codename1.system.Lifecycle.class;
        }
        if ("NativeInterface".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.system -> com.codename1.system.NativeInterface");
            }
            return com.codename1.system.NativeInterface.class;
        }
        if ("NativeLookup".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.system -> com.codename1.system.NativeLookup");
            }
            return com.codename1.system.NativeLookup.class;
        }
        if ("URLCallback".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.system -> com.codename1.system.URLCallback");
            }
            return com.codename1.system.URLCallback.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.system.DefaultCrashReporter.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.system.NativeLookup.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getCheckboxText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.system.DefaultCrashReporter.getCheckboxText();
            }
        }
        if ("getDontSendButtonText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.system.DefaultCrashReporter.getDontSendButtonText();
            }
        }
        if ("getErrorText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.system.DefaultCrashReporter.getErrorText();
            }
        }
        if ("getSendButtonText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.system.DefaultCrashReporter.getSendButtonText();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                com.codename1.system.DefaultCrashReporter.init(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setCheckboxText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.system.DefaultCrashReporter.setCheckboxText((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDontSendButtonText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.system.DefaultCrashReporter.setDontSendButtonText((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setErrorText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.system.DefaultCrashReporter.setErrorText((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setSendButtonText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.system.DefaultCrashReporter.setSendButtonText((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.system.DefaultCrashReporter.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("isVerbose".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.system.NativeLookup.isVerbose();
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.Class.class}, false)) {
                com.codename1.system.NativeLookup.register((java.lang.Class) safeArgs[0], (java.lang.Class) safeArgs[1]); return null;
            }
        }
        if ("setVerbose".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.system.NativeLookup.setVerbose(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.system.NativeLookup.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.system.DefaultCrashReporter) {
            try {
                return invoke0((com.codename1.system.DefaultCrashReporter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.system.Lifecycle) {
            try {
                return invoke1((com.codename1.system.Lifecycle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.system.CrashReport) {
            try {
                return invoke2((com.codename1.system.CrashReport) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.system.NativeInterface) {
            try {
                return invoke3((com.codename1.system.NativeInterface) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.system.URLCallback) {
            try {
                return invoke4((com.codename1.system.URLCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.system.DefaultCrashReporter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("exception".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.exception((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.system.Lifecycle typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke2(com.codename1.system.CrashReport typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("exception".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.exception((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.system.NativeInterface typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSupported();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.system.URLCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("shouldApplicationHandleURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.shouldApplicationHandleURL((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
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
