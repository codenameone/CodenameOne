package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_text {
    private GeneratedAccess_java_text() {
    }

    public static Class<?> findClass(String name) {
        if ("java.text.DateFormat".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit java.text -> java.text.DateFormat");
            }
            return java.text.DateFormat.class;
        }
        if ("java.text.DateFormatSymbols".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit java.text -> java.text.DateFormatSymbols");
            }
            return java.text.DateFormatSymbols.class;
        }
        if ("java.text.Format".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit java.text -> java.text.Format");
            }
            return java.text.Format.class;
        }
        if ("java.text.ParseException".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit java.text -> java.text.ParseException");
            }
            return java.text.ParseException.class;
        }
        if ("java.text.SimpleDateFormat".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit java.text -> java.text.SimpleDateFormat");
            }
            return java.text.SimpleDateFormat.class;
        }
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.text.ParseException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return new java.text.ParseException((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if (type == java.text.SimpleDateFormat.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.text.SimpleDateFormat();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.text.SimpleDateFormat((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.text.DateFormat.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getDateInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.text.DateFormat.getDateInstance();
            }
        }
        if ("getDateInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return java.text.DateFormat.getDateInstance(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getDateTimeInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.text.DateFormat.getDateTimeInstance(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.text.DateFormat.getInstance();
            }
        }
        if ("getTimeInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.text.DateFormat.getTimeInstance();
            }
        }
        if ("getTimeInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return java.text.DateFormat.getTimeInstance(((Number) safeArgs[0]).intValue());
            }
        }
        throw unsupportedStatic(java.text.DateFormat.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof java.text.SimpleDateFormat) {
            try {
                return invoke0((java.text.SimpleDateFormat) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.text.DateFormat) {
            try {
                return invoke1((java.text.DateFormat) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.text.DateFormatSymbols) {
            try {
                return invoke2((java.text.DateFormatSymbols) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.text.Format) {
            try {
                return invoke3((java.text.Format) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.text.ParseException) {
            try {
                return invoke4((java.text.ParseException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.text.SimpleDateFormat typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("applyPattern".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.applyPattern((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.format((java.lang.Object) safeArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                return typedTarget.format((java.util.Date) safeArgs[0]);
            }
        }
        if ("getDateFormatSymbols".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDateFormatSymbols();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.parse((java.lang.String) safeArgs[0]);
            }
        }
        if ("parseObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.parseObject((java.lang.String) safeArgs[0]);
            }
        }
        if ("setDateFormatSymbols".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.text.DateFormatSymbols.class}, false)) {
                typedTarget.setDateFormatSymbols((java.text.DateFormatSymbols) safeArgs[0]); return null;
            }
        }
        if ("toPattern".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toPattern();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(java.text.DateFormat typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.format((java.lang.Object) safeArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                return typedTarget.format((java.util.Date) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.parse((java.lang.String) safeArgs[0]);
            }
        }
        if ("parseObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.parseObject((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(java.text.DateFormatSymbols typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAmPmStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAmPmStrings();
            }
        }
        if ("getEras".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEras();
            }
        }
        if ("getMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMonths();
            }
        }
        if ("getShortMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShortMonths();
            }
        }
        if ("getShortWeekdays".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShortWeekdays();
            }
        }
        if ("getWeekdays".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWeekdays();
            }
        }
        if ("getZoneStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoneStrings();
            }
        }
        if ("setAmPmStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setAmPmStrings((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        if ("setEras".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setEras((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        if ("setMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setMonths((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        if ("setShortMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setShortMonths((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        if ("setShortWeekdays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setShortWeekdays((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        if ("setWeekdays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setWeekdays((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        if ("setZoneStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[][].class}, false)) {
                typedTarget.setZoneStrings((java.lang.String[][]) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(java.text.Format typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.format((java.lang.Object) safeArgs[0]);
            }
        }
        if ("parseObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.parseObject((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(java.text.ParseException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getErrorOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getErrorOffset();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == java.text.DateFormat.class) {
            if ("DEFAULT".equals(name)) return java.text.DateFormat.DEFAULT;
            if ("FULL".equals(name)) return java.text.DateFormat.FULL;
            if ("LONG".equals(name)) return java.text.DateFormat.LONG;
            if ("MEDIUM".equals(name)) return java.text.DateFormat.MEDIUM;
            if ("SHORT".equals(name)) return java.text.DateFormat.SHORT;
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
