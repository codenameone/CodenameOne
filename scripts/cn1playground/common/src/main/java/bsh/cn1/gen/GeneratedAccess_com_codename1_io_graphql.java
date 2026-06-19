package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_graphql {
    private GeneratedAccess_com_codename1_io_graphql() {
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
        if ("GraphQL".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQL.class;
        }
        if ("GraphQLClients".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQLClients.class;
        }
        if ("Factory".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQLClients.Factory.class;
        }
        if ("GraphQLError".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQLError.class;
        }
        if ("GraphQLResponse".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQLResponse.class;
        }
        if ("GraphQLSubscription".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQLSubscription.class;
        }
        if ("Handler".equals(simpleName)) {
            return com.codename1.io.graphql.GraphQLSubscription.Handler.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.graphql.GraphQLError.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class, java.util.List.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class, java.util.List.class, java.util.Map.class}, false);
                return new com.codename1.io.graphql.GraphQLError((java.lang.String) adaptedArgs[0], (java.util.List) adaptedArgs[1], (java.util.List) adaptedArgs[2], (java.util.Map) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.io.graphql.GraphQLResponse.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, java.util.List.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, java.util.List.class, java.lang.String.class}, false);
                return new com.codename1.io.graphql.GraphQLResponse(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (java.util.List) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.graphql.GraphQL.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.graphql.GraphQLClients.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("buildRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false);
                return com.codename1.io.graphql.GraphQL.buildRequestBody((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.Map) adaptedArgs[2]);
            }
        }
        if ("decodeJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Class.class}, false);
                return com.codename1.io.graphql.GraphQL.decodeJson((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.Class) adaptedArgs[2]);
            }
        }
        if ("encodeVariables".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.io.graphql.GraphQL.encodeVariables((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Map.class, java.lang.Class.class, com.codename1.util.OnComplete.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Map.class, java.lang.Class.class, com.codename1.util.OnComplete.class}, false);
                com.codename1.io.graphql.GraphQL.execute((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.util.Map) adaptedArgs[4], (java.lang.Class) adaptedArgs[5], (com.codename1.util.OnComplete) adaptedArgs[6]); return null;
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Map.class, java.lang.Class.class, com.codename1.io.graphql.GraphQLSubscription.Handler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Map.class, java.lang.Class.class, com.codename1.io.graphql.GraphQLSubscription.Handler.class}, false);
                return com.codename1.io.graphql.GraphQL.subscribe((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.util.Map) adaptedArgs[4], (java.lang.Class) adaptedArgs[5], (com.codename1.io.graphql.GraphQLSubscription.Handler) adaptedArgs[6]);
            }
        }
        throw unsupportedStatic(com.codename1.io.graphql.GraphQL.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false);
                return com.codename1.io.graphql.GraphQLClients.create((java.lang.Class) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.io.graphql.GraphQLClients.Factory.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.io.graphql.GraphQLClients.Factory.class}, false);
                com.codename1.io.graphql.GraphQLClients.register((java.lang.Class) adaptedArgs[0], (com.codename1.io.graphql.GraphQLClients.Factory) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.graphql.GraphQLClients.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.graphql.GraphQLError) {
            try {
                return invoke0((com.codename1.io.graphql.GraphQLError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.graphql.GraphQLResponse) {
            try {
                return invoke1((com.codename1.io.graphql.GraphQLResponse) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.graphql.GraphQLSubscription) {
            try {
                return invoke2((com.codename1.io.graphql.GraphQLSubscription) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.graphql.GraphQLClients.Factory) {
            try {
                return invoke3((com.codename1.io.graphql.GraphQLClients.Factory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.graphql.GraphQLSubscription.Handler) {
            try {
                return invoke4((com.codename1.io.graphql.GraphQLSubscription.Handler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.graphql.GraphQLError typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getExtensions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtensions();
            }
        }
        if ("getLocations".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocations();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.graphql.GraphQLResponse typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getData();
            }
        }
        if ("getErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrors();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("hasErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasErrors();
            }
        }
        if ("isOk".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOk();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.graphql.GraphQLSubscription typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cancel(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.graphql.GraphQLClients.Factory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.create((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.graphql.GraphQLSubscription.Handler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onComplete".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.onComplete(); return null;
            }
        }
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.graphql.GraphQLResponse.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.graphql.GraphQLResponse.class}, false);
                typedTarget.onError((com.codename1.io.graphql.GraphQLResponse) adaptedArgs[0]); return null;
            }
        }
        if ("onNext".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.graphql.GraphQLResponse.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.graphql.GraphQLResponse.class}, false);
                typedTarget.onNext((com.codename1.io.graphql.GraphQLResponse) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.graphql.GraphQL.class) return getStaticField0(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("CONTENT_TYPE".equals(name)) return com.codename1.io.graphql.GraphQL.CONTENT_TYPE;
        throw unsupportedStaticField(com.codename1.io.graphql.GraphQL.class, name);
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
