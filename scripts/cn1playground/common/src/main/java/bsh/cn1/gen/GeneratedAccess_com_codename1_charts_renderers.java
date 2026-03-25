package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_renderers {
    private GeneratedAccess_com_codename1_charts_renderers() {
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
        if ("BasicStroke".equals(simpleName)) {
            return com.codename1.charts.renderers.BasicStroke.class;
        }
        if ("DefaultRenderer".equals(simpleName)) {
            return com.codename1.charts.renderers.DefaultRenderer.class;
        }
        if ("DialRenderer".equals(simpleName)) {
            return com.codename1.charts.renderers.DialRenderer.class;
        }
        if ("SimpleSeriesRenderer".equals(simpleName)) {
            return com.codename1.charts.renderers.SimpleSeriesRenderer.class;
        }
        if ("XYMultipleSeriesRenderer".equals(simpleName)) {
            return com.codename1.charts.renderers.XYMultipleSeriesRenderer.class;
        }
        if ("XYSeriesRenderer".equals(simpleName)) {
            return com.codename1.charts.renderers.XYSeriesRenderer.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.renderers.BasicStroke.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, float[].class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, float[].class, java.lang.Float.class}, false);
                return new com.codename1.charts.renderers.BasicStroke(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).floatValue(), (float[]) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue());
            }
        }
        if (type == com.codename1.charts.renderers.XYMultipleSeriesRenderer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.charts.renderers.XYMultipleSeriesRenderer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.charts.renderers.XYMultipleSeriesRenderer(((Number) adaptedArgs[0]).intValue());
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
        if (target instanceof com.codename1.charts.renderers.DialRenderer) {
            try {
                return invoke0((com.codename1.charts.renderers.DialRenderer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.renderers.XYMultipleSeriesRenderer) {
            try {
                return invoke1((com.codename1.charts.renderers.XYMultipleSeriesRenderer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.renderers.XYSeriesRenderer) {
            try {
                return invoke2((com.codename1.charts.renderers.XYSeriesRenderer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.renderers.BasicStroke) {
            try {
                return invoke3((com.codename1.charts.renderers.BasicStroke) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.renderers.DefaultRenderer) {
            try {
                return invoke4((com.codename1.charts.renderers.DefaultRenderer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.renderers.SimpleSeriesRenderer) {
            try {
                return invoke5((com.codename1.charts.renderers.SimpleSeriesRenderer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.charts.renderers.DialRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.addSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.addSeriesRenderer(((Number) adaptedArgs[0]).intValue(), (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1]); return null;
            }
        }
        if ("getAngleMax".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngleMax();
            }
        }
        if ("getAngleMin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAngleMin();
            }
        }
        if ("getAxesColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAxesColor();
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getChartTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartTitle();
            }
        }
        if ("getChartTitleTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartTitleTextSize();
            }
        }
        if ("getLabelsColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelsColor();
            }
        }
        if ("getLabelsTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelsTextSize();
            }
        }
        if ("getLegendHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLegendHeight();
            }
        }
        if ("getLegendTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLegendTextSize();
            }
        }
        if ("getMajorTicksSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMajorTicksSpacing();
            }
        }
        if ("getMargins".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMargins();
            }
        }
        if ("getMaxValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxValue();
            }
        }
        if ("getMinValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinValue();
            }
        }
        if ("getMinorTicksSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinorTicksSpacing();
            }
        }
        if ("getOriginalScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginalScale();
            }
        }
        if ("getScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScale();
            }
        }
        if ("getSelectableBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectableBuffer();
            }
        }
        if ("getSeriesRendererAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSeriesRendererAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getSeriesRendererCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesRendererCount();
            }
        }
        if ("getSeriesRenderers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesRenderers();
            }
        }
        if ("getStartAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartAngle();
            }
        }
        if ("getTextTypeface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypeface();
            }
        }
        if ("getTextTypefaceName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypefaceName();
            }
        }
        if ("getTextTypefaceStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypefaceStyle();
            }
        }
        if ("getVisualTypeForIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getVisualTypeForIndex(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getXAxisColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXAxisColor();
            }
        }
        if ("getYAxisColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYAxisColor();
            }
        }
        if ("getZoomRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomRate();
            }
        }
        if ("isAntialiasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAntialiasing();
            }
        }
        if ("isApplyBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isApplyBackgroundColor();
            }
        }
        if ("isClickEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isClickEnabled();
            }
        }
        if ("isDisplayValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisplayValues();
            }
        }
        if ("isExternalZoomEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExternalZoomEnabled();
            }
        }
        if ("isFitLegend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFitLegend();
            }
        }
        if ("isInScroll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInScroll();
            }
        }
        if ("isMaxValueSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMaxValueSet();
            }
        }
        if ("isMinValueSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMinValueSet();
            }
        }
        if ("isPanEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPanEnabled();
            }
        }
        if ("isShowAxes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowAxes();
            }
        }
        if ("isShowCustomTextGridX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowCustomTextGridX();
            }
        }
        if ("isShowCustomTextGridY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowCustomTextGridY();
            }
        }
        if ("isShowGridX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowGridX();
            }
        }
        if ("isShowGridY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowGridY();
            }
        }
        if ("isShowLabels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLabels();
            }
        }
        if ("isShowLegend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLegend();
            }
        }
        if ("isShowTickMarks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowTickMarks();
            }
        }
        if ("isZoomButtonsVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomButtonsVisible();
            }
        }
        if ("isZoomEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomEnabled();
            }
        }
        if ("removeAllRenderers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllRenderers(); return null;
            }
        }
        if ("removeSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.removeSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]); return null;
            }
        }
        if ("setAngleMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setAngleMax(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setAngleMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setAngleMin(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAntialiasing(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setApplyBackgroundColor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAxesColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBackgroundColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setChartTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setChartTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setChartTitleTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setChartTitleTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setClickEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayValues(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setExternalZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFitLegend(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInScroll(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLabelsColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setLabelsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setLabelsTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLabelsTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLegendHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setLegendTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setLegendTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLegendTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMajorTicksSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setMajorTicksSpacing(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                typedTarget.setMargins((int[]) adaptedArgs[0]); return null;
            }
        }
        if ("setMaxValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setMaxValue(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMinValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setMinValue(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMinorTicksSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setMinorTicksSpacing(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPanEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setScale(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSelectableBuffer(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowAxes(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGridX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGridY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGridX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGridY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLabels(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLegend(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowTickMarks(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setStartAngle(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setTextTypeface((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setTextTypeface(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setVisualTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DialRenderer.Type[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DialRenderer.Type[].class}, false);
                typedTarget.setVisualTypes((com.codename1.charts.renderers.DialRenderer.Type[]) adaptedArgs[0]); return null;
            }
        }
        if ("setXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setXAxisColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYAxisColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setZoomButtonsVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setZoomRate(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.renderers.XYMultipleSeriesRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.addSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.addSeriesRenderer(((Number) adaptedArgs[0]).intValue(), (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1]); return null;
            }
        }
        if ("addTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false);
                typedTarget.addTextLabel(((Number) adaptedArgs[0]).doubleValue(), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addXTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false);
                typedTarget.addXTextLabel(((Number) adaptedArgs[0]).doubleValue(), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false);
                typedTarget.addYTextLabel(((Number) adaptedArgs[0]).doubleValue(), (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class, java.lang.Integer.class}, false);
                typedTarget.addYTextLabel(((Number) adaptedArgs[0]).doubleValue(), (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("clearTextLabels".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearTextLabels(); return null;
            }
        }
        if ("clearXTextLabels".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearXTextLabels(); return null;
            }
        }
        if ("clearYTextLabels".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearYTextLabels(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.clearYTextLabels(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("getAxesColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAxesColor();
            }
        }
        if ("getAxisTitleTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAxisTitleTextSize();
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getBarSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBarSpacing();
            }
        }
        if ("getBarWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBarWidth();
            }
        }
        if ("getBarsSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBarsSpacing();
            }
        }
        if ("getChartTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartTitle();
            }
        }
        if ("getChartTitleTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartTitleTextSize();
            }
        }
        if ("getGridColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getGridColor(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getInitialRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInitialRange();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getInitialRange(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getLabelFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelFormat();
            }
        }
        if ("getLabelsColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelsColor();
            }
        }
        if ("getLabelsTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelsTextSize();
            }
        }
        if ("getLegendHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLegendHeight();
            }
        }
        if ("getLegendTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLegendTextSize();
            }
        }
        if ("getMargins".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMargins();
            }
        }
        if ("getMarginsColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMarginsColor();
            }
        }
        if ("getOrientation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOrientation();
            }
        }
        if ("getOriginalScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginalScale();
            }
        }
        if ("getPanLimits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPanLimits();
            }
        }
        if ("getPointSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointSize();
            }
        }
        if ("getScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScale();
            }
        }
        if ("getScalesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScalesCount();
            }
        }
        if ("getSelectableBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectableBuffer();
            }
        }
        if ("getSeriesRendererAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSeriesRendererAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getSeriesRendererCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesRendererCount();
            }
        }
        if ("getSeriesRenderers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesRenderers();
            }
        }
        if ("getStartAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartAngle();
            }
        }
        if ("getTextTypeface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypeface();
            }
        }
        if ("getTextTypefaceName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypefaceName();
            }
        }
        if ("getTextTypefaceStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypefaceStyle();
            }
        }
        if ("getXAxisColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXAxisColor();
            }
        }
        if ("getXAxisMax".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXAxisMax();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getXAxisMax(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getXAxisMin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXAxisMin();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getXAxisMin(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getXLabelFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXLabelFormat();
            }
        }
        if ("getXLabels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXLabels();
            }
        }
        if ("getXLabelsAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXLabelsAlign();
            }
        }
        if ("getXLabelsAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXLabelsAngle();
            }
        }
        if ("getXLabelsColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXLabelsColor();
            }
        }
        if ("getXLabelsPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXLabelsPadding();
            }
        }
        if ("getXTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.getXTextLabel(Double.valueOf(((Number) adaptedArgs[0]).doubleValue()));
            }
        }
        if ("getXTextLabelLocations".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXTextLabelLocations();
            }
        }
        if ("getXTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXTitle();
            }
        }
        if ("getYAxisAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYAxisAlign(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYAxisColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYAxisColor();
            }
        }
        if ("getYAxisMax".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYAxisMax();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYAxisMax(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYAxisMin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYAxisMin();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYAxisMin(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYLabelFormat(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYLabels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYLabels();
            }
        }
        if ("getYLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYLabelsAlign(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYLabelsAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYLabelsAngle();
            }
        }
        if ("getYLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYLabelsColor(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYLabelsPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYLabelsPadding();
            }
        }
        if ("getYLabelsVerticalPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYLabelsVerticalPadding();
            }
        }
        if ("getYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.getYTextLabel(Double.valueOf(((Number) adaptedArgs[0]).doubleValue()));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                return typedTarget.getYTextLabel(Double.valueOf(((Number) adaptedArgs[0]).doubleValue()), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getYTextLabelLocations".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYTextLabelLocations();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYTextLabelLocations(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getYTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYTitle();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getYTitle(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getZoomInLimitX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomInLimitX();
            }
        }
        if ("getZoomInLimitY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomInLimitY();
            }
        }
        if ("getZoomLimits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomLimits();
            }
        }
        if ("getZoomRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomRate();
            }
        }
        if ("initAxesRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.initAxesRange(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("initAxesRangeForScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.initAxesRangeForScale(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("isAntialiasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAntialiasing();
            }
        }
        if ("isApplyBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isApplyBackgroundColor();
            }
        }
        if ("isClickEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isClickEnabled();
            }
        }
        if ("isDisplayValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisplayValues();
            }
        }
        if ("isExternalZoomEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExternalZoomEnabled();
            }
        }
        if ("isFitLegend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFitLegend();
            }
        }
        if ("isInScroll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInScroll();
            }
        }
        if ("isInitialRangeSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInitialRangeSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isInitialRangeSet(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isMaxXSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMaxXSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isMaxXSet(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isMaxYSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMaxYSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isMaxYSet(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isMinXSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMinXSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isMinXSet(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isMinYSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMinYSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isMinYSet(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isPanEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPanEnabled();
            }
        }
        if ("isPanXEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPanXEnabled();
            }
        }
        if ("isPanYEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPanYEnabled();
            }
        }
        if ("isShowAxes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowAxes();
            }
        }
        if ("isShowCustomTextGridX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowCustomTextGridX();
            }
        }
        if ("isShowCustomTextGridY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowCustomTextGridY();
            }
        }
        if ("isShowGridX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowGridX();
            }
        }
        if ("isShowGridY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowGridY();
            }
        }
        if ("isShowLabels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLabels();
            }
        }
        if ("isShowLegend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLegend();
            }
        }
        if ("isShowTickMarks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowTickMarks();
            }
        }
        if ("isXRoundedLabels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isXRoundedLabels();
            }
        }
        if ("isZoomButtonsVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomButtonsVisible();
            }
        }
        if ("isZoomEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomEnabled();
            }
        }
        if ("isZoomXEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomXEnabled();
            }
        }
        if ("isZoomYEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomYEnabled();
            }
        }
        if ("removeAllRenderers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllRenderers(); return null;
            }
        }
        if ("removeSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.removeSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]); return null;
            }
        }
        if ("removeXTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.removeXTextLabel(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("removeYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.removeYTextLabel(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.removeYTextLabel(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAntialiasing(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setApplyBackgroundColor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAxesColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAxisTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setAxisTitleTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setAxisTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setAxisTitleTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBackgroundColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setBarSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setBarSpacing(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setBarWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setBarWidth(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setChartTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setChartTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setChartTitleTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setChartTitleTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setClickEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayValues(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setExternalZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFitLegend(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGridColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setGridColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setGridColor(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInScroll(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInitialRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                typedTarget.setInitialRange((double[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setInitialRange((double[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLabelsColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setLabelsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setLabelsTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLabelsTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLegendHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setLegendTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setLegendTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLegendTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                typedTarget.setMargins((int[]) adaptedArgs[0]); return null;
            }
        }
        if ("setMarginsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMarginsColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setOrientation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation.class}, false);
                typedTarget.setOrientation((com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation) adaptedArgs[0]); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPanEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setPanEnabled(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setPanLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                typedTarget.setPanLimits((double[]) adaptedArgs[0]); return null;
            }
        }
        if ("setPointSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPointSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                typedTarget.setRange((double[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setRange((double[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setScale(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSelectableBuffer(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowAxes(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGridX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGridY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGridX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGridY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLabels(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLegend(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowTickMarks(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setStartAngle(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setTextTypeface((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setTextTypeface(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setXAxisColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setXAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setXAxisMax(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setXAxisMax(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setXAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setXAxisMin(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setXAxisMin(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setXLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false);
                typedTarget.setXLabelFormat((com.codename1.charts.util.NumberFormat) adaptedArgs[0]); return null;
            }
        }
        if ("setXLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setXLabels(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setXLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setXLabelsAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setXLabelsAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setXLabelsAngle(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setXLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setXLabelsColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setXLabelsPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setXLabelsPadding(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setXRoundedLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setXRoundedLabels(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setXTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setXTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setYAxisAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setYAxisAlign(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYAxisColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setYAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setYAxisMax(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setYAxisMax(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setYAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setYAxisMin(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setYAxisMin(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class, java.lang.Integer.class}, false);
                typedTarget.setYLabelFormat((com.codename1.charts.util.NumberFormat) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYLabels(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setYLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYLabelsAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setYLabelsAlign(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabelsAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setYLabelsAngle(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setYLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setYLabelsColor(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabelsPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setYLabelsPadding(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setYLabelsVerticalPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setYLabelsVerticalPadding(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setYTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setYTitle((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                typedTarget.setYTitle((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setZoomButtonsVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setZoomInLimitX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setZoomInLimitX(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setZoomInLimitY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setZoomInLimitY(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setZoomLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                typedTarget.setZoomLimits((double[]) adaptedArgs[0]); return null;
            }
        }
        if ("setZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setZoomRate(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.renderers.XYSeriesRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addFillOutsideLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine.class}, false);
                typedTarget.addFillOutsideLine((com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine) adaptedArgs[0]); return null;
            }
        }
        if ("getAnnotationsColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnnotationsColor();
            }
        }
        if ("getAnnotationsTextAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnnotationsTextAlign();
            }
        }
        if ("getAnnotationsTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnnotationsTextSize();
            }
        }
        if ("getChartValuesFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartValuesFormat();
            }
        }
        if ("getChartValuesSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartValuesSpacing();
            }
        }
        if ("getChartValuesTextAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartValuesTextAlign();
            }
        }
        if ("getChartValuesTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartValuesTextSize();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getDisplayChartValuesDistance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayChartValuesDistance();
            }
        }
        if ("getFillOutsideLine".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFillOutsideLine();
            }
        }
        if ("getGradientStartColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStartColor();
            }
        }
        if ("getGradientStartValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStartValue();
            }
        }
        if ("getGradientStopColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStopColor();
            }
        }
        if ("getGradientStopValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStopValue();
            }
        }
        if ("getLineWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLineWidth();
            }
        }
        if ("getPointStrokeWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointStrokeWidth();
            }
        }
        if ("getPointStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointStyle();
            }
        }
        if ("getStroke".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStroke();
            }
        }
        if ("isDisplayBoundingPoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisplayBoundingPoints();
            }
        }
        if ("isDisplayChartValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisplayChartValues();
            }
        }
        if ("isFillBelowLine".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFillBelowLine();
            }
        }
        if ("isFillPoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFillPoints();
            }
        }
        if ("isGradientEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGradientEnabled();
            }
        }
        if ("isHighlighted".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHighlighted();
            }
        }
        if ("isShowLegendItem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLegendItem();
            }
        }
        if ("setAnnotationsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAnnotationsColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAnnotationsTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAnnotationsTextAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAnnotationsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setAnnotationsTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setAnnotationsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setAnnotationsTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setChartValuesFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false);
                typedTarget.setChartValuesFormat((com.codename1.charts.util.NumberFormat) adaptedArgs[0]); return null;
            }
        }
        if ("setChartValuesSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setChartValuesSpacing(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setChartValuesTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setChartValuesTextAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setChartValuesTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setChartValuesTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setChartValuesTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setChartValuesTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDisplayBoundingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayBoundingPoints(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayChartValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayChartValues(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayChartValuesDistance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDisplayChartValuesDistance(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFillBelowLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFillBelowLine(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFillBelowLineColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFillBelowLineColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFillPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFillPoints(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGradientEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setGradientStart(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setGradientStop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setGradientStop(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setHighlighted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHighlighted(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLineWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLineWidth(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setPointStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPointStrokeWidth(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setPointStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.views.PointStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.views.PointStyle.class}, false);
                typedTarget.setPointStyle((com.codename1.charts.views.PointStyle) adaptedArgs[0]); return null;
            }
        }
        if ("setShowLegendItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLegendItem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.BasicStroke.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.BasicStroke.class}, false);
                typedTarget.setStroke((com.codename1.charts.renderers.BasicStroke) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.renderers.BasicStroke typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCap();
            }
        }
        if ("getIntervals".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIntervals();
            }
        }
        if ("getJoin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJoin();
            }
        }
        if ("getMiter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMiter();
            }
        }
        if ("getPhase".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPhase();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.charts.renderers.DefaultRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.addSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.addSeriesRenderer(((Number) adaptedArgs[0]).intValue(), (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1]); return null;
            }
        }
        if ("getAxesColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAxesColor();
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getChartTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartTitle();
            }
        }
        if ("getChartTitleTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartTitleTextSize();
            }
        }
        if ("getLabelsColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelsColor();
            }
        }
        if ("getLabelsTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelsTextSize();
            }
        }
        if ("getLegendHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLegendHeight();
            }
        }
        if ("getLegendTextSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLegendTextSize();
            }
        }
        if ("getMargins".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMargins();
            }
        }
        if ("getOriginalScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginalScale();
            }
        }
        if ("getScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScale();
            }
        }
        if ("getSelectableBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectableBuffer();
            }
        }
        if ("getSeriesRendererAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSeriesRendererAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getSeriesRendererCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesRendererCount();
            }
        }
        if ("getSeriesRenderers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesRenderers();
            }
        }
        if ("getStartAngle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartAngle();
            }
        }
        if ("getTextTypeface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypeface();
            }
        }
        if ("getTextTypefaceName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypefaceName();
            }
        }
        if ("getTextTypefaceStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextTypefaceStyle();
            }
        }
        if ("getXAxisColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXAxisColor();
            }
        }
        if ("getYAxisColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYAxisColor();
            }
        }
        if ("getZoomRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomRate();
            }
        }
        if ("isAntialiasing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAntialiasing();
            }
        }
        if ("isApplyBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isApplyBackgroundColor();
            }
        }
        if ("isClickEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isClickEnabled();
            }
        }
        if ("isDisplayValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisplayValues();
            }
        }
        if ("isExternalZoomEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExternalZoomEnabled();
            }
        }
        if ("isFitLegend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFitLegend();
            }
        }
        if ("isInScroll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInScroll();
            }
        }
        if ("isPanEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPanEnabled();
            }
        }
        if ("isShowAxes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowAxes();
            }
        }
        if ("isShowCustomTextGridX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowCustomTextGridX();
            }
        }
        if ("isShowCustomTextGridY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowCustomTextGridY();
            }
        }
        if ("isShowGridX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowGridX();
            }
        }
        if ("isShowGridY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowGridY();
            }
        }
        if ("isShowLabels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLabels();
            }
        }
        if ("isShowLegend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLegend();
            }
        }
        if ("isShowTickMarks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowTickMarks();
            }
        }
        if ("isZoomButtonsVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomButtonsVisible();
            }
        }
        if ("isZoomEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isZoomEnabled();
            }
        }
        if ("removeAllRenderers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllRenderers(); return null;
            }
        }
        if ("removeSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                typedTarget.removeSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]); return null;
            }
        }
        if ("setAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAntialiasing(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setApplyBackgroundColor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAxesColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBackgroundColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setChartTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setChartTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setChartTitleTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setChartTitleTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setClickEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayValues(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setExternalZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFitLegend(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInScroll(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLabelsColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setLabelsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setLabelsTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLabelsTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLegendHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setLegendTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setLegendTextFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setLegendTextSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                typedTarget.setMargins((int[]) adaptedArgs[0]); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPanEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setScale(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSelectableBuffer(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowAxes(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGridX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowCustomTextGridY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGridX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowGridY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLabels(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLegend(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowTickMarks(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setStartAngle(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setTextTypeface((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setTextTypeface(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setXAxisColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYAxisColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setZoomButtonsVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setZoomEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setZoomRate(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.charts.renderers.SimpleSeriesRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getChartValuesFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartValuesFormat();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getGradientStartColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStartColor();
            }
        }
        if ("getGradientStartValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStartValue();
            }
        }
        if ("getGradientStopColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStopColor();
            }
        }
        if ("getGradientStopValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGradientStopValue();
            }
        }
        if ("getStroke".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStroke();
            }
        }
        if ("isDisplayBoundingPoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisplayBoundingPoints();
            }
        }
        if ("isGradientEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGradientEnabled();
            }
        }
        if ("isHighlighted".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHighlighted();
            }
        }
        if ("isShowLegendItem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowLegendItem();
            }
        }
        if ("setChartValuesFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false);
                typedTarget.setChartValuesFormat((com.codename1.charts.util.NumberFormat) adaptedArgs[0]); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDisplayBoundingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayBoundingPoints(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGradientEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setGradientStart(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setGradientStop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                typedTarget.setGradientStop(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setHighlighted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHighlighted(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegendItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowLegendItem(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.BasicStroke.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.BasicStroke.class}, false);
                typedTarget.setStroke((com.codename1.charts.renderers.BasicStroke) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.charts.renderers.BasicStroke.class) {
            if ("DASHED".equals(name)) return com.codename1.charts.renderers.BasicStroke.DASHED;
            if ("DOTTED".equals(name)) return com.codename1.charts.renderers.BasicStroke.DOTTED;
            if ("SOLID".equals(name)) return com.codename1.charts.renderers.BasicStroke.SOLID;
        }
        if (type == com.codename1.charts.renderers.DefaultRenderer.class) {
            if ("BACKGROUND_COLOR".equals(name)) return com.codename1.charts.renderers.DefaultRenderer.BACKGROUND_COLOR;
            if ("NO_COLOR".equals(name)) return com.codename1.charts.renderers.DefaultRenderer.NO_COLOR;
            if ("TEXT_COLOR".equals(name)) return com.codename1.charts.renderers.DefaultRenderer.TEXT_COLOR;
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
