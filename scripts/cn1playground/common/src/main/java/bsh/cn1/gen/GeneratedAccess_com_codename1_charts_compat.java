package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_compat {
    private GeneratedAccess_com_codename1_charts_compat() {
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
        if ("Canvas".equals(simpleName)) {
            return com.codename1.charts.compat.Canvas.class;
        }
        if ("GradientDrawable".equals(simpleName)) {
            return com.codename1.charts.compat.GradientDrawable.class;
        }
        if ("Paint".equals(simpleName)) {
            return com.codename1.charts.compat.Paint.class;
        }
        if ("PathMeasure".equals(simpleName)) {
            return com.codename1.charts.compat.PathMeasure.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.compat.Canvas.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.charts.compat.Canvas();
            }
        }
        if (type == com.codename1.charts.compat.GradientDrawable.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.GradientDrawable.Orientation.class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.GradientDrawable.Orientation.class, int[].class}, false);
                return new com.codename1.charts.compat.GradientDrawable((com.codename1.charts.compat.GradientDrawable.Orientation) adaptedArgs[0], (int[]) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.compat.PathMeasure.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.GeneralPath.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.GeneralPath.class, java.lang.Boolean.class}, false);
                return new com.codename1.charts.compat.PathMeasure((com.codename1.ui.geom.GeneralPath) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
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
        if (target instanceof com.codename1.charts.compat.Canvas) {
            try {
                return invoke0((com.codename1.charts.compat.Canvas) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.compat.GradientDrawable) {
            try {
                return invoke1((com.codename1.charts.compat.GradientDrawable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.compat.Paint) {
            try {
                return invoke2((com.codename1.charts.compat.Paint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.compat.PathMeasure) {
            try {
                return invoke3((com.codename1.charts.compat.PathMeasure) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.charts.compat.Canvas typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("drawArc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Float.class, java.lang.Float.class, java.lang.Boolean.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Float.class, java.lang.Float.class, java.lang.Boolean.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawArc((com.codename1.ui.geom.Rectangle2D) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Boolean) adaptedArgs[3]).booleanValue(), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("drawArcWithGradient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Float.class, java.lang.Float.class, java.lang.Boolean.class, com.codename1.charts.compat.Paint.class, com.codename1.charts.compat.GradientDrawable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Float.class, java.lang.Float.class, java.lang.Boolean.class, com.codename1.charts.compat.Paint.class, com.codename1.charts.compat.GradientDrawable.class}, false);
                typedTarget.drawArcWithGradient((com.codename1.ui.geom.Rectangle2D) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Boolean) adaptedArgs[3]).booleanValue(), (com.codename1.charts.compat.Paint) adaptedArgs[4], (com.codename1.charts.compat.GradientDrawable) adaptedArgs[5]); return null;
            }
        }
        if ("drawBitmap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawBitmap((com.codename1.ui.Image) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), (com.codename1.charts.compat.Paint) adaptedArgs[3]); return null;
            }
        }
        if ("drawCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawCircle(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), (com.codename1.charts.compat.Paint) adaptedArgs[3]); return null;
            }
        }
        if ("drawLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLine(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("drawPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawPath((com.codename1.ui.geom.Shape) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1]); return null;
            }
        }
        if ("drawPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawPoint(Float.valueOf(((Number) adaptedArgs[0]).floatValue()), Float.valueOf(((Number) adaptedArgs[1]).floatValue()), (com.codename1.charts.compat.Paint) adaptedArgs[2]); return null;
            }
        }
        if ("drawRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawRect(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("drawRoundRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawRoundRect((com.codename1.ui.geom.Rectangle2D) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), (com.codename1.charts.compat.Paint) adaptedArgs[3]); return null;
            }
        }
        if ("drawText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Float.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawText((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), (com.codename1.charts.compat.Paint) adaptedArgs[3]); return null;
            }
        }
        if ("getClipBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.getClipBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("isShapeClipSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShapeClipSupported();
            }
        }
        if ("rotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.rotate(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.scale(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.translate(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.compat.GradientDrawable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0]); return null;
            }
        }
        if ("setBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setBounds(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.compat.Paint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("breakText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Float.class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Float.class, float[].class}, false);
                return typedTarget.breakText((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Number) adaptedArgs[2]).floatValue(), (float[]) adaptedArgs[3]);
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getStrokeCap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeCap();
            }
        }
        if ("getStrokeJoin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeJoin();
            }
        }
        if ("getStrokeMiter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeMiter();
            }
        }
        if ("getStrokeWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeWidth();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTextAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextAlign();
            }
        }
        if ("getTextBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.geom.Rectangle2D.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.geom.Rectangle2D.class}, false);
                typedTarget.getTextBounds((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), (com.codename1.ui.geom.Rectangle2D) adaptedArgs[3]); return null;
            }
        }
        if ("getTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSize();
            }
        }
        if ("getTextWidths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, float[].class}, false);
                typedTarget.getTextWidths((java.lang.String) adaptedArgs[0], (float[]) adaptedArgs[1]); return null;
            }
        }
        if ("getTypeface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypeface();
            }
        }
        if ("measureText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.measureText((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.measureText((char[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("setAntiAlias".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAntiAlias(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setStrokeCap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStrokeCap(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setStrokeJoin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStrokeJoin(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setStrokeMiter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setStrokeMiter(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setStrokeWidth(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Paint.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Paint.Style.class}, false);
                typedTarget.setStyle((com.codename1.charts.compat.Paint.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTextAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setTypeface((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.compat.PathMeasure typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLength();
            }
        }
        if ("getPosTan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, float[].class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, float[].class, float[].class}, false);
                typedTarget.getPosTan(((Number) adaptedArgs[0]).intValue(), (float[]) adaptedArgs[1], (float[]) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.charts.compat.Canvas) {
            com.codename1.charts.compat.Canvas typedTarget = (com.codename1.charts.compat.Canvas) target;
            if ("absoluteX".equals(name)) return typedTarget.absoluteX;
            if ("absoluteY".equals(name)) return typedTarget.absoluteY;
            if ("bounds".equals(name)) return typedTarget.bounds;
            if ("g".equals(name)) return typedTarget.g;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.charts.compat.Canvas) {
            com.codename1.charts.compat.Canvas typedTarget = (com.codename1.charts.compat.Canvas) target;
            if ("absoluteX".equals(name)) {
                typedTarget.absoluteX = ((Number) value).intValue();
                return;
            }
            if ("absoluteY".equals(name)) {
                typedTarget.absoluteY = ((Number) value).intValue();
                return;
            }
            if ("bounds".equals(name)) {
                typedTarget.bounds = (com.codename1.ui.geom.Rectangle) value;
                return;
            }
            if ("g".equals(name)) {
                typedTarget.g = (com.codename1.ui.Graphics) value;
                return;
            }
        }
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
