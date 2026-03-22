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
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.charts.renderers -> com.codename1.charts.renderers.BasicStroke");
            }
            return com.codename1.charts.renderers.BasicStroke.class;
        }
        if ("DefaultRenderer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.charts.renderers -> com.codename1.charts.renderers.DefaultRenderer");
            }
            return com.codename1.charts.renderers.DefaultRenderer.class;
        }
        if ("DialRenderer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.charts.renderers -> com.codename1.charts.renderers.DialRenderer");
            }
            return com.codename1.charts.renderers.DialRenderer.class;
        }
        if ("SimpleSeriesRenderer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.charts.renderers -> com.codename1.charts.renderers.SimpleSeriesRenderer");
            }
            return com.codename1.charts.renderers.SimpleSeriesRenderer.class;
        }
        if ("XYMultipleSeriesRenderer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.charts.renderers -> com.codename1.charts.renderers.XYMultipleSeriesRenderer");
            }
            return com.codename1.charts.renderers.XYMultipleSeriesRenderer.class;
        }
        if ("XYSeriesRenderer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.charts.renderers -> com.codename1.charts.renderers.XYSeriesRenderer");
            }
            return com.codename1.charts.renderers.XYSeriesRenderer.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.renderers.BasicStroke.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, float[].class, java.lang.Float.class}, false)) {
                return new com.codename1.charts.renderers.BasicStroke(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).floatValue(), (float[]) safeArgs[3], ((Number) safeArgs[4]).floatValue());
            }
        }
        if (type == com.codename1.charts.renderers.XYMultipleSeriesRenderer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.charts.renderers.XYMultipleSeriesRenderer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.charts.renderers.XYMultipleSeriesRenderer(((Number) safeArgs[0]).intValue());
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
                typedTarget.addSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]); return null;
            }
        }
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.addSeriesRenderer(((Number) safeArgs[0]).intValue(), (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1]); return null;
            }
        }
        if ("getAngleMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAngleMax();
            }
        }
        if ("getAngleMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAngleMin();
            }
        }
        if ("getAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAxesColor();
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartTitle();
            }
        }
        if ("getChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartTitleTextSize();
            }
        }
        if ("getLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelsColor();
            }
        }
        if ("getLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelsTextSize();
            }
        }
        if ("getLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLegendHeight();
            }
        }
        if ("getLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLegendTextSize();
            }
        }
        if ("getMajorTicksSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMajorTicksSpacing();
            }
        }
        if ("getMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMargins();
            }
        }
        if ("getMaxValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxValue();
            }
        }
        if ("getMinValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinValue();
            }
        }
        if ("getMinorTicksSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinorTicksSpacing();
            }
        }
        if ("getOriginalScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginalScale();
            }
        }
        if ("getScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScale();
            }
        }
        if ("getSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectableBuffer();
            }
        }
        if ("getSeriesRendererAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getSeriesRendererAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesRendererCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesRendererCount();
            }
        }
        if ("getSeriesRenderers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesRenderers();
            }
        }
        if ("getStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStartAngle();
            }
        }
        if ("getTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypeface();
            }
        }
        if ("getTextTypefaceName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypefaceName();
            }
        }
        if ("getTextTypefaceStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypefaceStyle();
            }
        }
        if ("getVisualTypeForIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getVisualTypeForIndex(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXAxisColor();
            }
        }
        if ("getYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYAxisColor();
            }
        }
        if ("getZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoomRate();
            }
        }
        if ("isAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAntialiasing();
            }
        }
        if ("isApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isApplyBackgroundColor();
            }
        }
        if ("isClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isClickEnabled();
            }
        }
        if ("isDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisplayValues();
            }
        }
        if ("isExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isExternalZoomEnabled();
            }
        }
        if ("isFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFitLegend();
            }
        }
        if ("isInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInScroll();
            }
        }
        if ("isMaxValueSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMaxValueSet();
            }
        }
        if ("isMinValueSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMinValueSet();
            }
        }
        if ("isPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPanEnabled();
            }
        }
        if ("isShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowAxes();
            }
        }
        if ("isShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowCustomTextGridX();
            }
        }
        if ("isShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowCustomTextGridY();
            }
        }
        if ("isShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowGridX();
            }
        }
        if ("isShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowGridY();
            }
        }
        if ("isShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLabels();
            }
        }
        if ("isShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLegend();
            }
        }
        if ("isShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowTickMarks();
            }
        }
        if ("isZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomButtonsVisible();
            }
        }
        if ("isZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomEnabled();
            }
        }
        if ("removeAllRenderers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllRenderers(); return null;
            }
        }
        if ("removeSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.removeSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]); return null;
            }
        }
        if ("setAngleMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setAngleMax(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setAngleMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setAngleMin(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAntialiasing(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setApplyBackgroundColor(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAxesColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBackgroundColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setChartTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setChartTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setChartTitleTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setChartTitleTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setClickEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayValues(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setExternalZoomEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFitLegend(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInScroll(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setLabelsColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setLabelsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setLabelsTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLabelsTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setLegendHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setLegendTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setLegendTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLegendTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMajorTicksSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setMajorTicksSpacing(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                typedTarget.setMargins((int[]) safeArgs[0]); return null;
            }
        }
        if ("setMaxValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setMaxValue(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMinValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setMinValue(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setMinorTicksSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setMinorTicksSpacing(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPanEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setScale(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSelectableBuffer(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowAxes(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGridX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGridY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGridX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGridY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLabels(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLegend(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowTickMarks(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setStartAngle(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setTextTypeface((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setTextTypeface(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setVisualTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DialRenderer.Type[].class}, false)) {
                typedTarget.setVisualTypes((com.codename1.charts.renderers.DialRenderer.Type[]) safeArgs[0]); return null;
            }
        }
        if ("setXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setXAxisColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setYAxisColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setZoomButtonsVisible(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setZoomEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setZoomRate(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.renderers.XYMultipleSeriesRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.addSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]); return null;
            }
        }
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.addSeriesRenderer(((Number) safeArgs[0]).intValue(), (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1]); return null;
            }
        }
        if ("addTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                typedTarget.addTextLabel(((Number) safeArgs[0]).doubleValue(), (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addXTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                typedTarget.addXTextLabel(((Number) safeArgs[0]).doubleValue(), (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                typedTarget.addYTextLabel(((Number) safeArgs[0]).doubleValue(), (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                typedTarget.addYTextLabel(((Number) safeArgs[0]).doubleValue(), (java.lang.String) safeArgs[1], ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("clearTextLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearTextLabels(); return null;
            }
        }
        if ("clearXTextLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearXTextLabels(); return null;
            }
        }
        if ("clearYTextLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearYTextLabels(); return null;
            }
        }
        if ("clearYTextLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.clearYTextLabels(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("getAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAxesColor();
            }
        }
        if ("getAxisTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAxisTitleTextSize();
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getBarSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBarSpacing();
            }
        }
        if ("getBarWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBarWidth();
            }
        }
        if ("getBarsSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBarsSpacing();
            }
        }
        if ("getChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartTitle();
            }
        }
        if ("getChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartTitleTextSize();
            }
        }
        if ("getGridColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getGridColor(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getInitialRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInitialRange();
            }
        }
        if ("getInitialRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getInitialRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelFormat();
            }
        }
        if ("getLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelsColor();
            }
        }
        if ("getLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelsTextSize();
            }
        }
        if ("getLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLegendHeight();
            }
        }
        if ("getLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLegendTextSize();
            }
        }
        if ("getMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMargins();
            }
        }
        if ("getMarginsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMarginsColor();
            }
        }
        if ("getOrientation".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOrientation();
            }
        }
        if ("getOriginalScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginalScale();
            }
        }
        if ("getPanLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPanLimits();
            }
        }
        if ("getPointSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointSize();
            }
        }
        if ("getScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScale();
            }
        }
        if ("getScalesCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScalesCount();
            }
        }
        if ("getSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectableBuffer();
            }
        }
        if ("getSeriesRendererAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getSeriesRendererAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesRendererCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesRendererCount();
            }
        }
        if ("getSeriesRenderers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesRenderers();
            }
        }
        if ("getStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStartAngle();
            }
        }
        if ("getTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypeface();
            }
        }
        if ("getTextTypefaceName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypefaceName();
            }
        }
        if ("getTextTypefaceStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypefaceStyle();
            }
        }
        if ("getXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXAxisColor();
            }
        }
        if ("getXAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXAxisMax();
            }
        }
        if ("getXAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getXAxisMax(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getXAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXAxisMin();
            }
        }
        if ("getXAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getXAxisMin(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getXLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXLabelFormat();
            }
        }
        if ("getXLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXLabels();
            }
        }
        if ("getXLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXLabelsAlign();
            }
        }
        if ("getXLabelsAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXLabelsAngle();
            }
        }
        if ("getXLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXLabelsColor();
            }
        }
        if ("getXLabelsPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXLabelsPadding();
            }
        }
        if ("getXTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.getXTextLabel(Double.valueOf(((Number) safeArgs[0]).doubleValue()));
            }
        }
        if ("getXTextLabelLocations".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXTextLabelLocations();
            }
        }
        if ("getXTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXTitle();
            }
        }
        if ("getYAxisAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYAxisAlign(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYAxisColor();
            }
        }
        if ("getYAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYAxisMax();
            }
        }
        if ("getYAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYAxisMax(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYAxisMin();
            }
        }
        if ("getYAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYAxisMin(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYLabelFormat(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYLabels();
            }
        }
        if ("getYLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYLabelsAlign(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYLabelsAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYLabelsAngle();
            }
        }
        if ("getYLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYLabelsColor(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYLabelsPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYLabelsPadding();
            }
        }
        if ("getYLabelsVerticalPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYLabelsVerticalPadding();
            }
        }
        if ("getYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.getYTextLabel(Double.valueOf(((Number) safeArgs[0]).doubleValue()));
            }
        }
        if ("getYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                return typedTarget.getYTextLabel(Double.valueOf(((Number) safeArgs[0]).doubleValue()), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getYTextLabelLocations".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYTextLabelLocations();
            }
        }
        if ("getYTextLabelLocations".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYTextLabelLocations(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getYTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYTitle();
            }
        }
        if ("getYTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getYTitle(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getZoomInLimitX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoomInLimitX();
            }
        }
        if ("getZoomInLimitY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoomInLimitY();
            }
        }
        if ("getZoomLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoomLimits();
            }
        }
        if ("getZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoomRate();
            }
        }
        if ("initAxesRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.initAxesRange(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("initAxesRangeForScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.initAxesRangeForScale(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("isAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAntialiasing();
            }
        }
        if ("isApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isApplyBackgroundColor();
            }
        }
        if ("isClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isClickEnabled();
            }
        }
        if ("isDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisplayValues();
            }
        }
        if ("isExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isExternalZoomEnabled();
            }
        }
        if ("isFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFitLegend();
            }
        }
        if ("isInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInScroll();
            }
        }
        if ("isInitialRangeSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInitialRangeSet();
            }
        }
        if ("isInitialRangeSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.isInitialRangeSet(((Number) safeArgs[0]).intValue());
            }
        }
        if ("isMaxXSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMaxXSet();
            }
        }
        if ("isMaxXSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.isMaxXSet(((Number) safeArgs[0]).intValue());
            }
        }
        if ("isMaxYSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMaxYSet();
            }
        }
        if ("isMaxYSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.isMaxYSet(((Number) safeArgs[0]).intValue());
            }
        }
        if ("isMinXSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMinXSet();
            }
        }
        if ("isMinXSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.isMinXSet(((Number) safeArgs[0]).intValue());
            }
        }
        if ("isMinYSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMinYSet();
            }
        }
        if ("isMinYSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.isMinYSet(((Number) safeArgs[0]).intValue());
            }
        }
        if ("isPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPanEnabled();
            }
        }
        if ("isPanXEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPanXEnabled();
            }
        }
        if ("isPanYEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPanYEnabled();
            }
        }
        if ("isShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowAxes();
            }
        }
        if ("isShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowCustomTextGridX();
            }
        }
        if ("isShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowCustomTextGridY();
            }
        }
        if ("isShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowGridX();
            }
        }
        if ("isShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowGridY();
            }
        }
        if ("isShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLabels();
            }
        }
        if ("isShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLegend();
            }
        }
        if ("isShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowTickMarks();
            }
        }
        if ("isXRoundedLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isXRoundedLabels();
            }
        }
        if ("isZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomButtonsVisible();
            }
        }
        if ("isZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomEnabled();
            }
        }
        if ("isZoomXEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomXEnabled();
            }
        }
        if ("isZoomYEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomYEnabled();
            }
        }
        if ("removeAllRenderers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllRenderers(); return null;
            }
        }
        if ("removeSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.removeSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]); return null;
            }
        }
        if ("removeXTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.removeXTextLabel(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("removeYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.removeYTextLabel(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("removeYTextLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.removeYTextLabel(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAntialiasing(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setApplyBackgroundColor(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAxesColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAxisTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setAxisTitleTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setAxisTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setAxisTitleTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBackgroundColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBarSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setBarSpacing(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setBarWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setBarWidth(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setChartTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setChartTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setChartTitleTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setChartTitleTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setClickEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayValues(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setExternalZoomEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFitLegend(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGridColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setGridColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setGridColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setGridColor(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInScroll(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInitialRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                typedTarget.setInitialRange((double[]) safeArgs[0]); return null;
            }
        }
        if ("setInitialRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setInitialRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setLabelsColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setLabelsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setLabelsTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLabelsTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setLegendHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setLegendTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setLegendTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLegendTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                typedTarget.setMargins((int[]) safeArgs[0]); return null;
            }
        }
        if ("setMarginsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setMarginsColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setOrientation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation.class}, false)) {
                typedTarget.setOrientation((com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation) safeArgs[0]); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPanEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.setPanEnabled(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setPanLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                typedTarget.setPanLimits((double[]) safeArgs[0]); return null;
            }
        }
        if ("setPointSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setPointSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                typedTarget.setRange((double[]) safeArgs[0]); return null;
            }
        }
        if ("setRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setScale(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSelectableBuffer(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowAxes(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGridX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGridY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGridX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGridY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLabels(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLegend(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowTickMarks(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setStartAngle(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setTextTypeface((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setTextTypeface(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setXAxisColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setXAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setXAxisMax(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setXAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setXAxisMax(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setXAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setXAxisMin(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setXAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setXAxisMin(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setXLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false)) {
                typedTarget.setXLabelFormat((com.codename1.charts.util.NumberFormat) safeArgs[0]); return null;
            }
        }
        if ("setXLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setXLabels(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setXLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setXLabelsAlign(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setXLabelsAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setXLabelsAngle(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setXLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setXLabelsColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setXLabelsPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setXLabelsPadding(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setXRoundedLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setXRoundedLabels(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setXTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setXTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setYAxisAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setYAxisAlign(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setYAxisColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setYAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setYAxisMax(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setYAxisMax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setYAxisMax(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setYAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setYAxisMin(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setYAxisMin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setYAxisMin(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabelFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class, java.lang.Integer.class}, false)) {
                typedTarget.setYLabelFormat((com.codename1.charts.util.NumberFormat) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setYLabels(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setYLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setYLabelsAlign(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setYLabelsAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setYLabelsAlign(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabelsAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setYLabelsAngle(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setYLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setYLabelsColor(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setYLabelsPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setYLabelsPadding(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setYLabelsVerticalPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setYLabelsVerticalPadding(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setYTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setYTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setYTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                typedTarget.setYTitle((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setZoomButtonsVisible(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setZoomEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.setZoomEnabled(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setZoomInLimitX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setZoomInLimitX(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setZoomInLimitY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setZoomInLimitY(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setZoomLimits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                typedTarget.setZoomLimits((double[]) safeArgs[0]); return null;
            }
        }
        if ("setZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setZoomRate(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.renderers.XYSeriesRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addFillOutsideLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine.class}, false)) {
                typedTarget.addFillOutsideLine((com.codename1.charts.renderers.XYSeriesRenderer.FillOutsideLine) safeArgs[0]); return null;
            }
        }
        if ("getAnnotationsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnnotationsColor();
            }
        }
        if ("getAnnotationsTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnnotationsTextAlign();
            }
        }
        if ("getAnnotationsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnnotationsTextSize();
            }
        }
        if ("getChartValuesFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartValuesFormat();
            }
        }
        if ("getChartValuesSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartValuesSpacing();
            }
        }
        if ("getChartValuesTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartValuesTextAlign();
            }
        }
        if ("getChartValuesTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartValuesTextSize();
            }
        }
        if ("getColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getColor();
            }
        }
        if ("getDisplayChartValuesDistance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisplayChartValuesDistance();
            }
        }
        if ("getFillOutsideLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFillOutsideLine();
            }
        }
        if ("getGradientStartColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStartColor();
            }
        }
        if ("getGradientStartValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStartValue();
            }
        }
        if ("getGradientStopColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStopColor();
            }
        }
        if ("getGradientStopValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStopValue();
            }
        }
        if ("getLineWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLineWidth();
            }
        }
        if ("getPointStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointStrokeWidth();
            }
        }
        if ("getPointStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointStyle();
            }
        }
        if ("getStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStroke();
            }
        }
        if ("isDisplayBoundingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisplayBoundingPoints();
            }
        }
        if ("isDisplayChartValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisplayChartValues();
            }
        }
        if ("isFillBelowLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFillBelowLine();
            }
        }
        if ("isFillPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFillPoints();
            }
        }
        if ("isGradientEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isGradientEnabled();
            }
        }
        if ("isHighlighted".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHighlighted();
            }
        }
        if ("isShowLegendItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLegendItem();
            }
        }
        if ("setAnnotationsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAnnotationsColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAnnotationsTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAnnotationsTextAlign(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAnnotationsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setAnnotationsTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setAnnotationsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setAnnotationsTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setChartValuesFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false)) {
                typedTarget.setChartValuesFormat((com.codename1.charts.util.NumberFormat) safeArgs[0]); return null;
            }
        }
        if ("setChartValuesSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setChartValuesSpacing(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setChartValuesTextAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setChartValuesTextAlign(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setChartValuesTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setChartValuesTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setChartValuesTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setChartValuesTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDisplayBoundingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayBoundingPoints(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayChartValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayChartValues(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayChartValuesDistance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDisplayChartValuesDistance(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFillBelowLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFillBelowLine(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFillBelowLineColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFillBelowLineColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFillPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFillPoints(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setGradientEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setGradientStart(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setGradientStop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setGradientStop(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setHighlighted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHighlighted(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLineWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLineWidth(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setPointStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setPointStrokeWidth(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setPointStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.views.PointStyle.class}, false)) {
                typedTarget.setPointStyle((com.codename1.charts.views.PointStyle) safeArgs[0]); return null;
            }
        }
        if ("setShowLegendItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLegendItem(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.BasicStroke.class}, false)) {
                typedTarget.setStroke((com.codename1.charts.renderers.BasicStroke) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.renderers.BasicStroke typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCap();
            }
        }
        if ("getIntervals".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIntervals();
            }
        }
        if ("getJoin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getJoin();
            }
        }
        if ("getMiter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMiter();
            }
        }
        if ("getPhase".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPhase();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.charts.renderers.DefaultRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.addSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]); return null;
            }
        }
        if ("addSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.addSeriesRenderer(((Number) safeArgs[0]).intValue(), (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1]); return null;
            }
        }
        if ("getAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAxesColor();
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartTitle();
            }
        }
        if ("getChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartTitleTextSize();
            }
        }
        if ("getLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelsColor();
            }
        }
        if ("getLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelsTextSize();
            }
        }
        if ("getLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLegendHeight();
            }
        }
        if ("getLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLegendTextSize();
            }
        }
        if ("getMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMargins();
            }
        }
        if ("getOriginalScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginalScale();
            }
        }
        if ("getScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScale();
            }
        }
        if ("getSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectableBuffer();
            }
        }
        if ("getSeriesRendererAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getSeriesRendererAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesRendererCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesRendererCount();
            }
        }
        if ("getSeriesRenderers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesRenderers();
            }
        }
        if ("getStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStartAngle();
            }
        }
        if ("getTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypeface();
            }
        }
        if ("getTextTypefaceName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypefaceName();
            }
        }
        if ("getTextTypefaceStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextTypefaceStyle();
            }
        }
        if ("getXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXAxisColor();
            }
        }
        if ("getYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYAxisColor();
            }
        }
        if ("getZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getZoomRate();
            }
        }
        if ("isAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAntialiasing();
            }
        }
        if ("isApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isApplyBackgroundColor();
            }
        }
        if ("isClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isClickEnabled();
            }
        }
        if ("isDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisplayValues();
            }
        }
        if ("isExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isExternalZoomEnabled();
            }
        }
        if ("isFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFitLegend();
            }
        }
        if ("isInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInScroll();
            }
        }
        if ("isPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPanEnabled();
            }
        }
        if ("isShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowAxes();
            }
        }
        if ("isShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowCustomTextGridX();
            }
        }
        if ("isShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowCustomTextGridY();
            }
        }
        if ("isShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowGridX();
            }
        }
        if ("isShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowGridY();
            }
        }
        if ("isShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLabels();
            }
        }
        if ("isShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLegend();
            }
        }
        if ("isShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowTickMarks();
            }
        }
        if ("isZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomButtonsVisible();
            }
        }
        if ("isZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isZoomEnabled();
            }
        }
        if ("removeAllRenderers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllRenderers(); return null;
            }
        }
        if ("removeSeriesRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                typedTarget.removeSeriesRenderer((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]); return null;
            }
        }
        if ("setAntialiasing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAntialiasing(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setApplyBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setApplyBackgroundColor(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAxesColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAxesColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBackgroundColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setChartTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setChartTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setChartTitleTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setChartTitleTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setChartTitleTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setChartTitleTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setClickEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setClickEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisplayValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayValues(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setExternalZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setExternalZoomEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFitLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFitLegend(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInScroll(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelsColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setLabelsColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setLabelsTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setLabelsTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setLabelsTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLabelsTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setLegendHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setLegendHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setLegendTextFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setLegendTextFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setLegendTextSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setLegendTextSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                typedTarget.setMargins((int[]) safeArgs[0]); return null;
            }
        }
        if ("setPanEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPanEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setScale(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setSelectableBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSelectableBuffer(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setShowAxes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowAxes(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGridX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowCustomTextGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowCustomTextGridY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGridX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowGridY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowGridY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLabels(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLegend(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowTickMarks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowTickMarks(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStartAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setStartAngle(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setTextTypeface((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setTextTypeface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setTextTypeface(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setXAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setXAxisColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setYAxisColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setYAxisColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setZoomButtonsVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setZoomButtonsVisible(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setZoomEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setZoomRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setZoomRate(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.charts.renderers.SimpleSeriesRenderer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getChartValuesFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartValuesFormat();
            }
        }
        if ("getColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getColor();
            }
        }
        if ("getGradientStartColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStartColor();
            }
        }
        if ("getGradientStartValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStartValue();
            }
        }
        if ("getGradientStopColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStopColor();
            }
        }
        if ("getGradientStopValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getGradientStopValue();
            }
        }
        if ("getStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStroke();
            }
        }
        if ("isDisplayBoundingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisplayBoundingPoints();
            }
        }
        if ("isGradientEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isGradientEnabled();
            }
        }
        if ("isHighlighted".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHighlighted();
            }
        }
        if ("isShowLegendItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowLegendItem();
            }
        }
        if ("setChartValuesFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.util.NumberFormat.class}, false)) {
                typedTarget.setChartValuesFormat((com.codename1.charts.util.NumberFormat) safeArgs[0]); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDisplayBoundingPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayBoundingPoints(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setGradientEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGradientStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setGradientStart(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setGradientStop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                typedTarget.setGradientStop(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setHighlighted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHighlighted(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShowLegendItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowLegendItem(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.BasicStroke.class}, false)) {
                typedTarget.setStroke((com.codename1.charts.renderers.BasicStroke) safeArgs[0]); return null;
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
