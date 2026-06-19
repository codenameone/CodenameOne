package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_bonjour {
    private GeneratedAccess_com_codename1_io_bonjour() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("BonjourBrowser".equals(simpleName)) {
            return com.codename1.io.bonjour.BonjourBrowser.class;
        }
        if ("BonjourPlatform".equals(simpleName)) {
            return com.codename1.io.bonjour.BonjourPlatform.class;
        }
        if ("BonjourPublisher".equals(simpleName)) {
            return com.codename1.io.bonjour.BonjourPublisher.class;
        }
        if ("BonjourService".equals(simpleName)) {
            return com.codename1.io.bonjour.BonjourService.class;
        }
        if ("BonjourServiceListener".equals(simpleName)) {
            return com.codename1.io.bonjour.BonjourServiceListener.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.bonjour.BonjourPlatform.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.bonjour.BonjourPlatform();
            }
        }
        if (type == com.codename1.io.bonjour.BonjourService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.util.Map.class}, false);
                return new com.codename1.io.bonjour.BonjourService((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], toIntValue(adaptedArgs[3]), (java.util.Map) adaptedArgs[4]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.bonjour.BonjourBrowser.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.bonjour.BonjourPublisher.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("browse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.bonjour.BonjourServiceListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.bonjour.BonjourServiceListener.class}, false);
                return com.codename1.io.bonjour.BonjourBrowser.browse((java.lang.String) adaptedArgs[0], (com.codename1.io.bonjour.BonjourServiceListener) adaptedArgs[1]);
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.bonjour.BonjourBrowser.isSupported();
            }
        }
        throw unsupportedStatic(com.codename1.io.bonjour.BonjourBrowser.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("publish".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.util.Map.class}, false);
                return com.codename1.io.bonjour.BonjourPublisher.publish((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.util.Map) adaptedArgs[3]);
            }
        }
        throw unsupportedStatic(com.codename1.io.bonjour.BonjourPublisher.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.bonjour.BonjourBrowser) {
            try {
                return invoke0((com.codename1.io.bonjour.BonjourBrowser) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.bonjour.BonjourPlatform) {
            try {
                return invoke1((com.codename1.io.bonjour.BonjourPlatform) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.bonjour.BonjourPublisher) {
            try {
                return invoke2((com.codename1.io.bonjour.BonjourPublisher) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.bonjour.BonjourService) {
            try {
                return invoke3((com.codename1.io.bonjour.BonjourService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.bonjour.BonjourServiceListener) {
            try {
                return invoke4((com.codename1.io.bonjour.BonjourServiceListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.bonjour.BonjourBrowser typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.bonjour.BonjourPlatform typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("startBrowse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.bonjour.BonjourServiceListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.bonjour.BonjourServiceListener.class}, false);
                return typedTarget.startBrowse((java.lang.String) adaptedArgs[0], (com.codename1.io.bonjour.BonjourServiceListener) adaptedArgs[1]);
            }
        }
        if ("startPublish".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.util.Map.class}, false);
                return typedTarget.startPublish((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.util.Map) adaptedArgs[3]);
            }
        }
        if ("stopBrowse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.stopBrowse((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("stopPublish".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.stopPublish((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.bonjour.BonjourPublisher typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPort".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPort();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("unpublish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unpublish(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.bonjour.BonjourService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getHost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHost();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPort".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPort();
            }
        }
        if ("getTxt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTxt();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.bonjour.BonjourServiceListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onBrowseError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.onBrowseError((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("onServiceLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.bonjour.BonjourService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.bonjour.BonjourService.class}, false);
                typedTarget.onServiceLost((com.codename1.io.bonjour.BonjourService) adaptedArgs[0]); return null;
            }
        }
        if ("onServiceResolved".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.bonjour.BonjourService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.bonjour.BonjourService.class}, false);
                typedTarget.onServiceResolved((com.codename1.io.bonjour.BonjourService) adaptedArgs[0]); return null;
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
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
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
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
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
