package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_rest {
    private GeneratedAccess_com_codename1_io_rest() {
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
        if ("ErrorCodeHandler".equals(simpleName)) {
            return com.codename1.io.rest.ErrorCodeHandler.class;
        }
        if ("RequestBuilder".equals(simpleName)) {
            return com.codename1.io.rest.RequestBuilder.class;
        }
        if ("Response".equals(simpleName)) {
            return com.codename1.io.rest.Response.class;
        }
        if ("Rest".equals(simpleName)) {
            return com.codename1.io.rest.Rest.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.rest.Rest.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.delete((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.get((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("head".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.head((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("options".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.options((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("patch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.patch((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("post".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.post((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.rest.Rest.put((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.rest.Rest.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.rest.RequestBuilder) {
            try {
                return invoke0((com.codename1.io.rest.RequestBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.rest.Response) {
            try {
                return invoke1((com.codename1.io.rest.Response) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.rest.ErrorCodeHandler) {
            try {
                return invoke2((com.codename1.io.rest.ErrorCodeHandler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.rest.RequestBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("acceptJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.acceptJson();
            }
        }
        if ("basicAuth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.basicAuth((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("bearer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.bearer((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("body".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false);
                return typedTarget.body((com.codename1.io.Data) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                return typedTarget.body((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.body((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("cacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                return typedTarget.cacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]);
            }
        }
        if ("contentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.contentType((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("cookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.cookiesEnabled(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("fetchAsBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class}, false);
                return typedTarget.fetchAsBytes((com.codename1.util.OnComplete) adaptedArgs[0]);
            }
        }
        if ("fetchAsJsonMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class}, false);
                return typedTarget.fetchAsJsonMap((com.codename1.util.OnComplete) adaptedArgs[0]);
            }
        }
        if ("fetchAsProperties".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class, java.lang.Class.class}, false);
                return typedTarget.fetchAsProperties((com.codename1.util.OnComplete) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
        }
        if ("fetchAsPropertyList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class, java.lang.Class.class}, false);
                return typedTarget.fetchAsPropertyList((com.codename1.util.OnComplete) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class, java.lang.Class.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class, java.lang.Class.class, java.lang.String.class}, false);
                return typedTarget.fetchAsPropertyList((com.codename1.util.OnComplete) adaptedArgs[0], (java.lang.Class) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("fetchAsString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.OnComplete.class}, false);
                return typedTarget.fetchAsString((com.codename1.util.OnComplete) adaptedArgs[0]);
            }
        }
        if ("getAsBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAsBytes();
            }
        }
        if ("getAsBytesAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.Callback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.Callback.class}, false);
                typedTarget.getAsBytesAsync((com.codename1.util.Callback) adaptedArgs[0]); return null;
            }
        }
        if ("getAsJsonMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAsJsonMap();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.getAsJsonMap((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                return typedTarget.getAsJsonMap((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.FailureCallback) adaptedArgs[1]);
            }
        }
        if ("getAsJsonMapAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.Callback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.Callback.class}, false);
                typedTarget.getAsJsonMapAsync((com.codename1.util.Callback) adaptedArgs[0]); return null;
            }
        }
        if ("getAsProperties".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                return typedTarget.getAsProperties((java.lang.Class) adaptedArgs[0]);
            }
        }
        if ("getAsPropertyList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                return typedTarget.getAsPropertyList((java.lang.Class) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false);
                return typedTarget.getAsPropertyList((java.lang.Class) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getAsString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAsString();
            }
        }
        if ("getAsStringAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.Callback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.Callback.class}, false);
                typedTarget.getAsStringAsync((com.codename1.util.Callback) adaptedArgs[0]); return null;
            }
        }
        if ("getRequestUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestUrl();
            }
        }
        if ("gzip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.gzip();
            }
        }
        if ("header".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.header((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("insecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.insecure(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("jsonContent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.jsonContent();
            }
        }
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                return typedTarget.onError((com.codename1.ui.events.ActionListener) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class, java.lang.Boolean.class}, false);
                return typedTarget.onError((com.codename1.ui.events.ActionListener) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("onErrorCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class, java.lang.Class.class}, false);
                return typedTarget.onErrorCode((com.codename1.io.rest.ErrorCodeHandler) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
        }
        if ("onErrorCodeBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class}, false);
                return typedTarget.onErrorCodeBytes((com.codename1.io.rest.ErrorCodeHandler) adaptedArgs[0]);
            }
        }
        if ("onErrorCodeJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class}, false);
                return typedTarget.onErrorCodeJSON((com.codename1.io.rest.ErrorCodeHandler) adaptedArgs[0]);
            }
        }
        if ("onErrorCodeString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.rest.ErrorCodeHandler.class}, false);
                return typedTarget.onErrorCodeString((com.codename1.io.rest.ErrorCodeHandler) adaptedArgs[0]);
            }
        }
        if ("pathParam".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.pathParam((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("postParameters".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.postParameters(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("priority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                return typedTarget.priority((byte) toIntValue(adaptedArgs[0]));
            }
        }
        if ("queryParam".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.queryParam((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                return typedTarget.queryParam((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]);
            }
        }
        if ("readTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.readTimeout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("timeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.timeout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("useBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.useBoolean(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("useLongs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.useLongs(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.rest.Response typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.rest.ErrorCodeHandler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.rest.Response.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.rest.Response.class}, false);
                typedTarget.onError((com.codename1.io.rest.Response) adaptedArgs[0]); return null;
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
