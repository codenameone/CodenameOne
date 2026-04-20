package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_transitions {
    private GeneratedAccess_com_codename1_charts_transitions() {
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
        if ("SeriesTransition".equals(simpleName)) {
            return com.codename1.charts.transitions.SeriesTransition.class;
        }
        if ("XYMultiSeriesTransition".equals(simpleName)) {
            return com.codename1.charts.transitions.XYMultiSeriesTransition.class;
        }
        if ("XYSeriesTransition".equals(simpleName)) {
            return com.codename1.charts.transitions.XYSeriesTransition.class;
        }
        if ("XYValueSeriesTransition".equals(simpleName)) {
            return com.codename1.charts.transitions.XYValueSeriesTransition.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.transitions.XYMultiSeriesTransition.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class, com.codename1.charts.models.XYMultipleSeriesDataset.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class, com.codename1.charts.models.XYMultipleSeriesDataset.class}, false);
                return new com.codename1.charts.transitions.XYMultiSeriesTransition((com.codename1.charts.ChartComponent) adaptedArgs[0], (com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.transitions.XYSeriesTransition.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class, com.codename1.charts.models.XYSeries.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class, com.codename1.charts.models.XYSeries.class}, false);
                return new com.codename1.charts.transitions.XYSeriesTransition((com.codename1.charts.ChartComponent) adaptedArgs[0], (com.codename1.charts.models.XYSeries) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.transitions.XYValueSeriesTransition.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class, com.codename1.charts.models.XYValueSeries.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class, com.codename1.charts.models.XYValueSeries.class}, false);
                return new com.codename1.charts.transitions.XYValueSeriesTransition((com.codename1.charts.ChartComponent) adaptedArgs[0], (com.codename1.charts.models.XYValueSeries) adaptedArgs[1]);
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
        if (target instanceof com.codename1.charts.transitions.XYMultiSeriesTransition) {
            try {
                return invoke0((com.codename1.charts.transitions.XYMultiSeriesTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.transitions.XYSeriesTransition) {
            try {
                return invoke1((com.codename1.charts.transitions.XYSeriesTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.transitions.XYValueSeriesTransition) {
            try {
                return invoke2((com.codename1.charts.transitions.XYValueSeriesTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.transitions.SeriesTransition) {
            try {
                return invoke3((com.codename1.charts.transitions.SeriesTransition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.charts.transitions.XYMultiSeriesTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.animateChart(); return null;
            }
        }
        if ("getBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBuffer();
            }
        }
        if ("getChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChart();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getEasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEasing();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false);
                typedTarget.setChart((com.codename1.charts.ChartComponent) adaptedArgs[0]); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setEasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setEasing(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("updateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.updateChart(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.transitions.XYSeriesTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.animateChart(); return null;
            }
        }
        if ("getBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBuffer();
            }
        }
        if ("getChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChart();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getEasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEasing();
            }
        }
        if ("getSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeries();
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false);
                typedTarget.setChart((com.codename1.charts.ChartComponent) adaptedArgs[0]); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setEasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setEasing(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("updateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.updateChart(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.transitions.XYValueSeriesTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.animateChart(); return null;
            }
        }
        if ("getBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBuffer();
            }
        }
        if ("getChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChart();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getEasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEasing();
            }
        }
        if ("getSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeries();
            }
        }
        if ("initTransition".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initTransition(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false);
                typedTarget.setChart((com.codename1.charts.ChartComponent) adaptedArgs[0]); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setEasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setEasing(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("updateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.updateChart(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.transitions.SeriesTransition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.animateChart(); return null;
            }
        }
        if ("getChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChart();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getEasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEasing();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.ChartComponent.class}, false);
                typedTarget.setChart((com.codename1.charts.ChartComponent) adaptedArgs[0]); return null;
            }
        }
        if ("setDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setEasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setEasing(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("updateChart".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.updateChart(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.charts.transitions.SeriesTransition.class) return getStaticField0(name);
        if (type == com.codename1.charts.transitions.XYMultiSeriesTransition.class) return getStaticField1(name);
        if (type == com.codename1.charts.transitions.XYSeriesTransition.class) return getStaticField2(name);
        if (type == com.codename1.charts.transitions.XYValueSeriesTransition.class) return getStaticField3(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("EASING_IN".equals(name)) return com.codename1.charts.transitions.SeriesTransition.EASING_IN;
        if ("EASING_IN_OUT".equals(name)) return com.codename1.charts.transitions.SeriesTransition.EASING_IN_OUT;
        if ("EASING_LINEAR".equals(name)) return com.codename1.charts.transitions.SeriesTransition.EASING_LINEAR;
        if ("EASING_OUT".equals(name)) return com.codename1.charts.transitions.SeriesTransition.EASING_OUT;
        throw unsupportedStaticField(com.codename1.charts.transitions.SeriesTransition.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("EASING_IN".equals(name)) return com.codename1.charts.transitions.XYMultiSeriesTransition.EASING_IN;
        if ("EASING_IN_OUT".equals(name)) return com.codename1.charts.transitions.XYMultiSeriesTransition.EASING_IN_OUT;
        if ("EASING_LINEAR".equals(name)) return com.codename1.charts.transitions.XYMultiSeriesTransition.EASING_LINEAR;
        if ("EASING_OUT".equals(name)) return com.codename1.charts.transitions.XYMultiSeriesTransition.EASING_OUT;
        throw unsupportedStaticField(com.codename1.charts.transitions.XYMultiSeriesTransition.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("EASING_IN".equals(name)) return com.codename1.charts.transitions.XYSeriesTransition.EASING_IN;
        if ("EASING_IN_OUT".equals(name)) return com.codename1.charts.transitions.XYSeriesTransition.EASING_IN_OUT;
        if ("EASING_LINEAR".equals(name)) return com.codename1.charts.transitions.XYSeriesTransition.EASING_LINEAR;
        if ("EASING_OUT".equals(name)) return com.codename1.charts.transitions.XYSeriesTransition.EASING_OUT;
        throw unsupportedStaticField(com.codename1.charts.transitions.XYSeriesTransition.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("EASING_IN".equals(name)) return com.codename1.charts.transitions.XYValueSeriesTransition.EASING_IN;
        if ("EASING_IN_OUT".equals(name)) return com.codename1.charts.transitions.XYValueSeriesTransition.EASING_IN_OUT;
        if ("EASING_LINEAR".equals(name)) return com.codename1.charts.transitions.XYValueSeriesTransition.EASING_LINEAR;
        if ("EASING_OUT".equals(name)) return com.codename1.charts.transitions.XYValueSeriesTransition.EASING_OUT;
        throw unsupportedStaticField(com.codename1.charts.transitions.XYValueSeriesTransition.class, name);
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
