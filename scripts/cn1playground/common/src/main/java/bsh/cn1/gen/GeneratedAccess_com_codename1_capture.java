package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_capture {
    private GeneratedAccess_com_codename1_capture() {
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
        if ("Capture".equals(simpleName)) {
            return com.codename1.capture.Capture.class;
        }
        if ("VideoCaptureConstraints".equals(simpleName)) {
            return com.codename1.capture.VideoCaptureConstraints.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.capture.VideoCaptureConstraints.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.capture.VideoCaptureConstraints();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.capture.VideoCaptureConstraints(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class}, false);
                return new com.codename1.capture.VideoCaptureConstraints((com.codename1.capture.VideoCaptureConstraints) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.capture.VideoCaptureConstraints(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.capture.Capture.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.capture.VideoCaptureConstraints.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("captureAudio".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.capture.Capture.captureAudio();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class}, false);
                return com.codename1.capture.Capture.captureAudio((com.codename1.media.MediaRecorderBuilder) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.capture.Capture.captureAudio((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class, com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.capture.Capture.captureAudio((com.codename1.media.MediaRecorderBuilder) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("capturePhoto".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.capture.Capture.capturePhoto();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.capture.Capture.capturePhoto((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.capture.Capture.capturePhoto(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("captureVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.capture.Capture.captureVideo();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class}, false);
                return com.codename1.capture.Capture.captureVideo((com.codename1.capture.VideoCaptureConstraints) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.capture.Capture.captureVideo((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class, com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.capture.Capture.captureVideo((com.codename1.capture.VideoCaptureConstraints) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("hasCamera".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.capture.Capture.hasCamera();
            }
        }
        throw unsupportedStatic(com.codename1.capture.Capture.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.Compiler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.Compiler.class}, false);
                com.codename1.capture.VideoCaptureConstraints.init((com.codename1.capture.VideoCaptureConstraints.Compiler) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.capture.VideoCaptureConstraints.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.capture.VideoCaptureConstraints) {
            try {
                return invoke0((com.codename1.capture.VideoCaptureConstraints) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.capture.VideoCaptureConstraints typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getMaxFileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxFileSize();
            }
        }
        if ("getMaxLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxLength();
            }
        }
        if ("getPreferredHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredHeight();
            }
        }
        if ("getPreferredMaxFileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredMaxFileSize();
            }
        }
        if ("getPreferredMaxLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredMaxLength();
            }
        }
        if ("getPreferredQuality".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredQuality();
            }
        }
        if ("getPreferredWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredWidth();
            }
        }
        if ("getQuality".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQuality();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isMaxFileSizeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMaxFileSizeSupported();
            }
        }
        if ("isMaxLengthSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMaxLengthSupported();
            }
        }
        if ("isNullConstraint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullConstraint();
            }
        }
        if ("isQualitySupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isQualitySupported();
            }
        }
        if ("isSizeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSizeSupported();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("preferredHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.preferredHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("preferredMaxFileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.preferredMaxFileSize(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("preferredMaxLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.preferredMaxLength(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("preferredQuality".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.preferredQuality(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("preferredWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.preferredWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.capture.VideoCaptureConstraints.class) {
            if ("QUALITY_HIGH".equals(name)) return com.codename1.capture.VideoCaptureConstraints.QUALITY_HIGH;
            if ("QUALITY_LOW".equals(name)) return com.codename1.capture.VideoCaptureConstraints.QUALITY_LOW;
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
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
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
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            return isSamInterface(type);
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
