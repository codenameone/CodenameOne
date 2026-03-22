package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_util_promise {
    private GeneratedAccess_com_codename1_util_promise() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.util.promise.ExecutorFunction".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.util.promise -> com.codename1.util.promise.ExecutorFunction");
            }
            return com.codename1.util.promise.ExecutorFunction.class;
        }
        if ("com.codename1.util.promise.Functor".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.util.promise -> com.codename1.util.promise.Functor");
            }
            return com.codename1.util.promise.Functor.class;
        }
        if ("com.codename1.util.promise.Promise".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.util.promise -> com.codename1.util.promise.Promise");
            }
            return com.codename1.util.promise.Promise.class;
        }
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.promise.Promise.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.ExecutorFunction.class}, false)) {
                return new com.codename1.util.promise.Promise((com.codename1.util.promise.ExecutorFunction) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.promise.Promise.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("all".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Promise[].class}, true)) {
                com.codename1.util.promise.Promise[] varArgs = new com.codename1.util.promise.Promise[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.util.promise.Promise) safeArgs[i];
                }
                return com.codename1.util.promise.Promise.all(varArgs);
            }
        }
        if ("allSettled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Promise[].class}, true)) {
                com.codename1.util.promise.Promise[] varArgs = new com.codename1.util.promise.Promise[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.util.promise.Promise) safeArgs[i];
                }
                return com.codename1.util.promise.Promise.allSettled(varArgs);
            }
        }
        if ("promisify".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false)) {
                return com.codename1.util.promise.Promise.promisify((com.codename1.util.AsyncResource) safeArgs[0]);
            }
        }
        if ("reject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return com.codename1.util.promise.Promise.reject((java.lang.Throwable) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.promise.Promise.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.util.promise.Promise) {
            try {
                return invoke0((com.codename1.util.promise.Promise) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.promise.ExecutorFunction) {
            try {
                return invoke1((com.codename1.util.promise.ExecutorFunction) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.util.promise.Promise typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("always".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Functor.class}, false)) {
                return typedTarget.always((com.codename1.util.promise.Functor) safeArgs[0]);
            }
        }
        if ("asAsyncResource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.asAsyncResource();
            }
        }
        if ("except".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Functor.class}, false)) {
                return typedTarget.except((com.codename1.util.promise.Functor) safeArgs[0]);
            }
        }
        if ("getState".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getState();
            }
        }
        if ("onComplete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.onComplete((com.codename1.util.SuccessCallback) safeArgs[0]);
            }
        }
        if ("onFail".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.onFail((com.codename1.util.SuccessCallback) safeArgs[0]);
            }
        }
        if ("onSuccess".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.onSuccess((com.codename1.util.SuccessCallback) safeArgs[0]);
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.ready((com.codename1.util.SuccessCallback) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]);
            }
        }
        if ("then".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Functor.class}, false)) {
                return typedTarget.then((com.codename1.util.promise.Functor) safeArgs[0]);
            }
        }
        if ("then".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Functor.class, com.codename1.util.promise.Functor.class}, false)) {
                return typedTarget.then((com.codename1.util.promise.Functor) safeArgs[0], (com.codename1.util.promise.Functor) safeArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.util.promise.ExecutorFunction typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.promise.Functor.class, com.codename1.util.promise.Functor.class}, false)) {
                typedTarget.call((com.codename1.util.promise.Functor) safeArgs[0], (com.codename1.util.promise.Functor) safeArgs[1]); return null;
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
