package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_lang_annotation {
    private GeneratedAccess_java_lang_annotation() {
    }

    public static Class<?> findClass(String name) {
        if ("java.lang.annotation.Annotation".equals(name)) return java.lang.annotation.Annotation.class;
        if ("java.lang.annotation.AnnotationFormatError".equals(name)) return java.lang.annotation.AnnotationFormatError.class;
        if ("java.lang.annotation.Documented".equals(name)) return java.lang.annotation.Documented.class;
        if ("java.lang.annotation.ElementType".equals(name)) return java.lang.annotation.ElementType.class;
        if ("java.lang.annotation.IncompleteAnnotationException".equals(name)) return java.lang.annotation.IncompleteAnnotationException.class;
        if ("java.lang.annotation.Inherited".equals(name)) return java.lang.annotation.Inherited.class;
        if ("java.lang.annotation.Retention".equals(name)) return java.lang.annotation.Retention.class;
        if ("java.lang.annotation.RetentionPolicy".equals(name)) return java.lang.annotation.RetentionPolicy.class;
        if ("java.lang.annotation.Target".equals(name)) return java.lang.annotation.Target.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.lang.annotation.AnnotationFormatError.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.lang.annotation.AnnotationFormatError((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return new java.lang.annotation.AnnotationFormatError((java.lang.Throwable) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                return new java.lang.annotation.AnnotationFormatError((java.lang.String) safeArgs[0], (java.lang.Throwable) safeArgs[1]);
            }
        }
        if (type == java.lang.annotation.IncompleteAnnotationException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false)) {
                return new java.lang.annotation.IncompleteAnnotationException((java.lang.Class) safeArgs[0], (java.lang.String) safeArgs[1]);
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
        if (target instanceof java.lang.annotation.IncompleteAnnotationException) {
            try {
                return invoke0((java.lang.annotation.IncompleteAnnotationException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.annotation.Annotation) {
            try {
                return invoke1((java.lang.annotation.Annotation) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.annotation.Retention) {
            try {
                return invoke2((java.lang.annotation.Retention) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.annotation.Target) {
            try {
                return invoke3((java.lang.annotation.Target) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.lang.annotation.IncompleteAnnotationException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("annotationType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.annotationType();
            }
        }
        if ("elementName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.elementName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(java.lang.annotation.Annotation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("annotationType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.annotationType();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(java.lang.annotation.Retention typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(java.lang.annotation.Target typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == java.lang.annotation.ElementType.class) {
            if ("ANNOTATION_TYPE".equals(name)) return java.lang.annotation.ElementType.ANNOTATION_TYPE;
            if ("CONSTRUCTOR".equals(name)) return java.lang.annotation.ElementType.CONSTRUCTOR;
            if ("FIELD".equals(name)) return java.lang.annotation.ElementType.FIELD;
            if ("LOCAL_VARIABLE".equals(name)) return java.lang.annotation.ElementType.LOCAL_VARIABLE;
            if ("METHOD".equals(name)) return java.lang.annotation.ElementType.METHOD;
            if ("PACKAGE".equals(name)) return java.lang.annotation.ElementType.PACKAGE;
            if ("PARAMETER".equals(name)) return java.lang.annotation.ElementType.PARAMETER;
        }
        if (type == java.lang.annotation.RetentionPolicy.class) {
            if ("CLASS".equals(name)) return java.lang.annotation.RetentionPolicy.CLASS;
            if ("RUNTIME".equals(name)) return java.lang.annotation.RetentionPolicy.RUNTIME;
            if ("SOURCE".equals(name)) return java.lang.annotation.RetentionPolicy.SOURCE;
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
