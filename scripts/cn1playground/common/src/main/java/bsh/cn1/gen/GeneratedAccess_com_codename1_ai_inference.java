/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ai_inference {
    private GeneratedAccess_com_codename1_ai_inference() {
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
        if ("InferenceException".equals(simpleName)) {
            return com.codename1.ai.inference.InferenceException.class;
        }
        if ("InferenceOptions".equals(simpleName)) {
            return com.codename1.ai.inference.InferenceOptions.class;
        }
        if ("Accelerator".equals(simpleName)) {
            return com.codename1.ai.inference.InferenceOptions.Accelerator.class;
        }
        if ("InferenceSession".equals(simpleName)) {
            return com.codename1.ai.inference.InferenceSession.class;
        }
        if ("ModelCache".equals(simpleName)) {
            return com.codename1.ai.inference.ModelCache.class;
        }
        if ("ModelSource".equals(simpleName)) {
            return com.codename1.ai.inference.ModelSource.class;
        }
        if ("Tensor".equals(simpleName)) {
            return com.codename1.ai.inference.Tensor.class;
        }
        if ("TensorInfo".equals(simpleName)) {
            return com.codename1.ai.inference.TensorInfo.class;
        }
        if ("TensorType".equals(simpleName)) {
            return com.codename1.ai.inference.TensorType.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ai.inference.InferenceException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.inference.InferenceException((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.ai.inference.InferenceException((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ai.inference.InferenceOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.inference.InferenceOptions();
            }
        }
        if (type == com.codename1.ai.inference.Tensor.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.inference.TensorType.class, int[].class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.inference.TensorType.class, int[].class, java.lang.Object.class}, false);
                return new com.codename1.ai.inference.Tensor((java.lang.String) adaptedArgs[0], (com.codename1.ai.inference.TensorType) adaptedArgs[1], (int[]) adaptedArgs[2], (java.lang.Object) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.ai.inference.TensorInfo.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.inference.TensorType.class, int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.inference.TensorType.class, int[].class, java.lang.Integer.class}, false);
                return new com.codename1.ai.inference.TensorInfo((java.lang.String) adaptedArgs[0], (com.codename1.ai.inference.TensorType) adaptedArgs[1], (int[]) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ai.inference.InferenceSession.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ai.inference.ModelCache.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ai.inference.ModelSource.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ai.inference.Tensor.class) return invokeStatic3(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.inference.InferenceSession.isSupported();
            }
        }
        if ("open".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.inference.ModelSource.class, com.codename1.ai.inference.InferenceOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.inference.ModelSource.class, com.codename1.ai.inference.InferenceOptions.class}, false);
                return com.codename1.ai.inference.InferenceSession.open((com.codename1.ai.inference.ModelSource) adaptedArgs[0], (com.codename1.ai.inference.InferenceOptions) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.inference.InferenceSession.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fetch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ai.inference.ModelCache.fetch((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ai.inference.ModelCache.fetch((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.inference.ModelCache.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("bytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.ai.inference.ModelSource.bytes((byte[]) adaptedArgs[0]);
            }
        }
        if ("file".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.inference.ModelSource.file((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("resource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.inference.ModelSource.resource((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.inference.ModelSource.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("bytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.inference.TensorType.class, int[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.inference.TensorType.class, int[].class, byte[].class}, false);
                return com.codename1.ai.inference.Tensor.bytes((java.lang.String) adaptedArgs[0], (com.codename1.ai.inference.TensorType) adaptedArgs[1], (int[]) adaptedArgs[2], (byte[]) adaptedArgs[3]);
            }
        }
        if ("floats".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class, float[].class}, false);
                return com.codename1.ai.inference.Tensor.floats((java.lang.String) adaptedArgs[0], (int[]) adaptedArgs[1], (float[]) adaptedArgs[2]);
            }
        }
        if ("ints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class, int[].class}, false);
                return com.codename1.ai.inference.Tensor.ints((java.lang.String) adaptedArgs[0], (int[]) adaptedArgs[1], (int[]) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.inference.Tensor.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ai.inference.InferenceOptions) {
            try {
                return invoke0((com.codename1.ai.inference.InferenceOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.inference.InferenceSession) {
            try {
                return invoke1((com.codename1.ai.inference.InferenceSession) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.inference.ModelSource) {
            try {
                return invoke2((com.codename1.ai.inference.ModelSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.inference.Tensor) {
            try {
                return invoke3((com.codename1.ai.inference.Tensor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.inference.TensorInfo) {
            try {
                return invoke4((com.codename1.ai.inference.TensorInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ai.inference.InferenceOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accelerator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.inference.InferenceOptions.Accelerator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.inference.InferenceOptions.Accelerator.class}, false);
                return typedTarget.accelerator((com.codename1.ai.inference.InferenceOptions.Accelerator) adaptedArgs[0]);
            }
        }
        if ("allowFallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.allowFallback(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getAccelerator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccelerator();
            }
        }
        if ("getThreads".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThreads();
            }
        }
        if ("isFallbackAllowed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFallbackAllowed();
            }
        }
        if ("threads".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.threads(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ai.inference.InferenceSession typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getInputs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInputs();
            }
        }
        if ("getOutputs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOutputs();
            }
        }
        if ("resizeInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, false);
                typedTarget.resizeInput((java.lang.String) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.inference.Tensor[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.inference.Tensor[].class}, false);
                return typedTarget.run((com.codename1.ai.inference.Tensor[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ai.inference.ModelSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBytes();
            }
        }
        if ("getBytesUnsafe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBytesUnsafe();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ai.inference.Tensor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getData();
            }
        }
        if ("getDataUnsafe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataUnsafe();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getShape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShape();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ai.inference.TensorInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIndex();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getShape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShape();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ai.inference.InferenceOptions.Accelerator.class) return getStaticField0(name);
        if (type == com.codename1.ai.inference.ModelSource.class) return getStaticField1(name);
        if (type == com.codename1.ai.inference.TensorType.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.ai.inference.InferenceOptions.Accelerator.AUTO;
        if ("CORE_ML".equals(name)) return com.codename1.ai.inference.InferenceOptions.Accelerator.CORE_ML;
        if ("CPU".equals(name)) return com.codename1.ai.inference.InferenceOptions.Accelerator.CPU;
        if ("GPU".equals(name)) return com.codename1.ai.inference.InferenceOptions.Accelerator.GPU;
        if ("NPU".equals(name)) return com.codename1.ai.inference.InferenceOptions.Accelerator.NPU;
        throw unsupportedStaticField(com.codename1.ai.inference.InferenceOptions.Accelerator.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("BYTES".equals(name)) return com.codename1.ai.inference.ModelSource.BYTES;
        if ("FILE".equals(name)) return com.codename1.ai.inference.ModelSource.FILE;
        if ("RESOURCE".equals(name)) return com.codename1.ai.inference.ModelSource.RESOURCE;
        throw unsupportedStaticField(com.codename1.ai.inference.ModelSource.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("BOOL".equals(name)) return com.codename1.ai.inference.TensorType.BOOL;
        if ("FLOAT32".equals(name)) return com.codename1.ai.inference.TensorType.FLOAT32;
        if ("INT32".equals(name)) return com.codename1.ai.inference.TensorType.INT32;
        if ("INT64".equals(name)) return com.codename1.ai.inference.TensorType.INT64;
        if ("INT8".equals(name)) return com.codename1.ai.inference.TensorType.INT8;
        if ("UINT8".equals(name)) return com.codename1.ai.inference.TensorType.UINT8;
        throw unsupportedStaticField(com.codename1.ai.inference.TensorType.class, name);
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
        if (type == com.codename1.printing.PrintResultListener.class) {
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
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
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
