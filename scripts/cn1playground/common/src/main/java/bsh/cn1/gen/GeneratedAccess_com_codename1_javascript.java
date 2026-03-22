package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_javascript {
    private GeneratedAccess_com_codename1_javascript() {
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
        if ("JSFunction".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.javascript -> com.codename1.javascript.JSFunction");
            }
            return com.codename1.javascript.JSFunction.class;
        }
        if ("JSObject".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.javascript -> com.codename1.javascript.JSObject");
            }
            return com.codename1.javascript.JSObject.class;
        }
        if ("JavascriptContext".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.javascript -> com.codename1.javascript.JavascriptContext");
            }
            return com.codename1.javascript.JavascriptContext.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.javascript.JSObject.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.javascript.JavascriptContext.class, java.lang.String.class}, false)) {
                return new com.codename1.javascript.JSObject((com.codename1.javascript.JavascriptContext) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.javascript.JavascriptContext.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.BrowserComponent.class}, false)) {
                return new com.codename1.javascript.JavascriptContext((com.codename1.ui.BrowserComponent) safeArgs[0]);
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
        if (target instanceof com.codename1.javascript.JSObject) {
            try {
                return invoke0((com.codename1.javascript.JSObject) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.javascript.JavascriptContext) {
            try {
                return invoke1((com.codename1.javascript.JavascriptContext) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.javascript.JSFunction) {
            try {
                return invoke2((com.codename1.javascript.JSFunction) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.javascript.JSObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.Object) safeArgs[i];
                }
                return typedTarget.call(varArgs);
            }
        }
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.call((java.lang.String) safeArgs[0]);
            }
        }
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, false)) {
                return typedTarget.call((java.lang.String) safeArgs[0], (java.lang.Object[]) safeArgs[1]);
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callAsync((java.lang.Object[]) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callAsync((java.lang.Object[]) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callAsync((java.lang.String) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callAsync((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callAsync((java.lang.String) safeArgs[0], (java.lang.Object[]) safeArgs[1], (com.codename1.util.Callback) safeArgs[2]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callAsync((java.lang.String) safeArgs[0], (java.lang.Object[]) safeArgs[1], (com.codename1.util.SuccessCallback) safeArgs[2]); return null;
            }
        }
        if ("callDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.callDouble((java.lang.String) safeArgs[0]);
            }
        }
        if ("callDoubleAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callDoubleAsync((java.lang.String) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("callDoubleAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callDoubleAsync((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("callInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.callInt((java.lang.String) safeArgs[0]);
            }
        }
        if ("callIntAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callIntAsync((java.lang.String) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("callIntAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callIntAsync((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("callObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.callObject((java.lang.String) safeArgs[0]);
            }
        }
        if ("callObjectAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callObjectAsync((java.lang.String) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("callObjectAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callObjectAsync((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("callString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.callString((java.lang.String) safeArgs[0]);
            }
        }
        if ("callStringAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callStringAsync((java.lang.String) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("callStringAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callStringAsync((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.get(((Number) safeArgs[0]).intValue());
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.get((java.lang.String) safeArgs[0]);
            }
        }
        if ("getBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getBoolean(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getBoolean((java.lang.String) safeArgs[0]);
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getDouble(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getDouble((java.lang.String) safeArgs[0]);
            }
        }
        if ("getInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getInt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getInt((java.lang.String) safeArgs[0]);
            }
        }
        if ("getObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getObject(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getObject((java.lang.String) safeArgs[0]);
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getString(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getString((java.lang.String) safeArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.set((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), (java.lang.Object) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                typedTarget.set((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBoolean(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBoolean((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBoolean(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBoolean((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false)) {
                typedTarget.setDouble(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("setDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                typedTarget.setDouble((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("setDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                typedTarget.setDouble(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).doubleValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                typedTarget.setDouble((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setInt(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                typedTarget.setInt((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setInt(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setInt((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("toJSPointer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toJSPointer();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.javascript.JavascriptContext typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.javascript.JSObject.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class}, false)) {
                return typedTarget.call((com.codename1.javascript.JSObject) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2]);
            }
        }
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class}, false)) {
                return typedTarget.call((java.lang.String) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2]);
            }
        }
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class, java.lang.Boolean.class, com.codename1.util.Callback.class}, false)) {
                return typedTarget.call((java.lang.String) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue(), (com.codename1.util.Callback) safeArgs[4]);
            }
        }
        if ("call".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class, java.lang.Boolean.class, com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.call((java.lang.String) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue(), (com.codename1.util.SuccessCallback) safeArgs[4]);
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.javascript.JSObject.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callAsync((com.codename1.javascript.JSObject) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2], (com.codename1.util.Callback) safeArgs[3]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.javascript.JSObject.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callAsync((com.codename1.javascript.JSObject) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2], (com.codename1.util.SuccessCallback) safeArgs[3]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class, com.codename1.util.Callback.class}, false)) {
                typedTarget.callAsync((java.lang.String) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2], (com.codename1.util.Callback) safeArgs[3]); return null;
            }
        }
        if ("callAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.javascript.JSObject.class, java.lang.Object[].class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.callAsync((java.lang.String) safeArgs[0], (com.codename1.javascript.JSObject) safeArgs[1], (java.lang.Object[]) safeArgs[2], (com.codename1.util.SuccessCallback) safeArgs[3]); return null;
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.get((java.lang.String) safeArgs[0]);
            }
        }
        if ("getAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.getAsync((java.lang.String) safeArgs[0], (com.codename1.util.Callback) safeArgs[1]); return null;
            }
        }
        if ("getAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.getAsync((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("getWindow".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWindow();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.set((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                typedTarget.set((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.setAsync((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setBrowserComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.BrowserComponent.class}, false)) {
                typedTarget.setBrowserComponent((com.codename1.ui.BrowserComponent) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.javascript.JSFunction typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("apply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.javascript.JSObject.class, java.lang.Object[].class}, false)) {
                typedTarget.apply((com.codename1.javascript.JSObject) safeArgs[0], (java.lang.Object[]) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.javascript.JavascriptContext.class) {
            if ("DEBUG".equals(name)) return com.codename1.javascript.JavascriptContext.DEBUG;
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
