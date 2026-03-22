package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_db {
    private GeneratedAccess_com_codename1_db() {
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
        if ("Cursor".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.db -> com.codename1.db.Cursor");
            }
            return com.codename1.db.Cursor.class;
        }
        if ("Database".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.db -> com.codename1.db.Database");
            }
            return com.codename1.db.Database.class;
        }
        if ("Row".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.db -> com.codename1.db.Row");
            }
            return com.codename1.db.Row.class;
        }
        if ("RowExt".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.db -> com.codename1.db.RowExt");
            }
            return com.codename1.db.RowExt.class;
        }
        if ("ThreadSafeDatabase".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.db -> com.codename1.db.ThreadSafeDatabase");
            }
            return com.codename1.db.ThreadSafeDatabase.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.db.ThreadSafeDatabase.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.db.Database.class}, false)) {
                return new com.codename1.db.ThreadSafeDatabase((com.codename1.db.Database) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.db.Database.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.db.Database.delete((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.db.Database.exists((java.lang.String) safeArgs[0]);
            }
        }
        if ("getDatabasePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.db.Database.getDatabasePath((java.lang.String) safeArgs[0]);
            }
        }
        if ("isCustomPathSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.db.Database.isCustomPathSupported();
            }
        }
        if ("openOrCreate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.db.Database.openOrCreate((java.lang.String) safeArgs[0]);
            }
        }
        if ("supportsWasNull".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.db.Row.class}, false)) {
                return com.codename1.db.Database.supportsWasNull((com.codename1.db.Row) safeArgs[0]);
            }
        }
        if ("wasNull".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.db.Row.class}, false)) {
                return com.codename1.db.Database.wasNull((com.codename1.db.Row) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.db.Database.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.db.ThreadSafeDatabase) {
            try {
                return invoke0((com.codename1.db.ThreadSafeDatabase) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.db.Database) {
            try {
                return invoke1((com.codename1.db.Database) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.db.Cursor) {
            try {
                return invoke2((com.codename1.db.Cursor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.db.Row) {
            try {
                return invoke3((com.codename1.db.Row) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.db.RowExt) {
            try {
                return invoke4((com.codename1.db.RowExt) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.db.ThreadSafeDatabase typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("beginTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.beginTransaction(); return null;
            }
        }
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("commitTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.commitTransaction(); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.execute((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) safeArgs[i];
                }
                typedTarget.execute((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.execute((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("executeQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.executeQuery((java.lang.String) safeArgs[0]);
            }
        }
        if ("executeQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) safeArgs[i];
                }
                return typedTarget.executeQuery((java.lang.String) safeArgs[0], varArgs);
            }
        }
        if ("executeQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                return typedTarget.executeQuery((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]);
            }
        }
        if ("getThread".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThread();
            }
        }
        if ("rollbackTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.rollbackTransaction(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.db.Database typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("beginTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.beginTransaction(); return null;
            }
        }
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("commitTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.commitTransaction(); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.execute((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) safeArgs[i];
                }
                typedTarget.execute((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.execute((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("executeQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.executeQuery((java.lang.String) safeArgs[0]);
            }
        }
        if ("executeQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) safeArgs[i];
                }
                return typedTarget.executeQuery((java.lang.String) safeArgs[0], varArgs);
            }
        }
        if ("executeQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                return typedTarget.executeQuery((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]);
            }
        }
        if ("rollbackTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.rollbackTransaction(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.db.Cursor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("first".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.first();
            }
        }
        if ("getColumnCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getColumnCount();
            }
        }
        if ("getColumnIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getColumnIndex((java.lang.String) safeArgs[0]);
            }
        }
        if ("getColumnName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getColumnName(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPosition();
            }
        }
        if ("getRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRow();
            }
        }
        if ("last".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.last();
            }
        }
        if ("next".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.next();
            }
        }
        if ("position".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.position(((Number) safeArgs[0]).intValue());
            }
        }
        if ("prev".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.prev();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.db.Row typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBlob".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getBlob(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getDouble(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getFloat(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getInteger".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getInteger(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLong(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getShort(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getString(((Number) safeArgs[0]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.db.RowExt typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBlob".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getBlob(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getDouble(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getFloat(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getInteger".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getInteger(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLong(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getShort(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getString(((Number) safeArgs[0]).intValue());
            }
        }
        if ("wasNull".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.wasNull();
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
