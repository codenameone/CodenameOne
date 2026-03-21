package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_messaging {
    private GeneratedAccess_com_codename1_messaging() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.messaging.Message".equals(name)) return com.codename1.messaging.Message.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.messaging.Message.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.messaging.Message((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.messaging.Message.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("sendMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String.class, com.codename1.messaging.Message.class}, false)) {
                com.codename1.messaging.Message.sendMessage((java.lang.String[]) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.messaging.Message) safeArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.messaging.Message.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.messaging.Message) {
            try {
                return invoke0((com.codename1.messaging.Message) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.messaging.Message typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAttachment();
            }
        }
        if ("getAttachmentMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAttachmentMimeType();
            }
        }
        if ("getAttachments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAttachments();
            }
        }
        if ("getContent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContent();
            }
        }
        if ("getMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMimeType();
            }
        }
        if ("isCloudMessageFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCloudMessageFailSilently();
            }
        }
        if ("sendMessageViaCloud".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.sendMessageViaCloud((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4]); return null;
            }
        }
        if ("sendMessageViaCloudSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.sendMessageViaCloudSync((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4]);
            }
        }
        if ("setAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setAttachment((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setAttachmentMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setAttachmentMimeType((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCloudMessageFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCloudMessageFailSilently(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setMimeType((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.messaging.Message.class) {
            if ("MIME_HTML".equals(name)) return com.codename1.messaging.Message.MIME_HTML;
            if ("MIME_IMAGE_JPG".equals(name)) return com.codename1.messaging.Message.MIME_IMAGE_JPG;
            if ("MIME_IMAGE_PNG".equals(name)) return com.codename1.messaging.Message.MIME_IMAGE_PNG;
            if ("MIME_TEXT".equals(name)) return com.codename1.messaging.Message.MIME_TEXT;
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
