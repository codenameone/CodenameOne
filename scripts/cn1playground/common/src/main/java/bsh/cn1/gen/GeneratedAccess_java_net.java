package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_net {
    private GeneratedAccess_java_net() {
    }

    public static Class<?> findClass(String name) {
        if ("java.net.URI".equals(name)) return java.net.URI.class;
        if ("java.net.URISyntaxException".equals(name)) return java.net.URISyntaxException.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.net.URI.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.net.URI((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new java.net.URI((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new java.net.URI((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new java.net.URI((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Number) safeArgs[3]).intValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6]);
            }
        }
        if (type == java.net.URISyntaxException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new java.net.URISyntaxException((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                return new java.net.URISyntaxException((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Number) safeArgs[2]).intValue());
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
        if (target instanceof java.net.URI) {
            try {
                return invoke0((java.net.URI) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.net.URISyntaxException) {
            try {
                return invoke1((java.net.URISyntaxException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.net.URI typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getAuthority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAuthority();
            }
        }
        if ("getFragment".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFragment();
            }
        }
        if ("getHost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHost();
            }
        }
        if ("getPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPath();
            }
        }
        if ("getPort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPort();
            }
        }
        if ("getQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getQuery();
            }
        }
        if ("getRawAuthority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawAuthority();
            }
        }
        if ("getRawFragment".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawFragment();
            }
        }
        if ("getRawPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawPath();
            }
        }
        if ("getRawQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawQuery();
            }
        }
        if ("getRawSchemeSpecificPart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawSchemeSpecificPart();
            }
        }
        if ("getRawUserInfo".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawUserInfo();
            }
        }
        if ("getScheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScheme();
            }
        }
        if ("getSchemeSpecificPart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSchemeSpecificPart();
            }
        }
        if ("getUserInfo".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserInfo();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isAbsolute".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAbsolute();
            }
        }
        if ("isOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isOpaque();
            }
        }
        if ("toASCIIString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toASCIIString();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(java.net.URISyntaxException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIndex();
            }
        }
        if ("getInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInput();
            }
        }
        if ("getReason".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReason();
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
