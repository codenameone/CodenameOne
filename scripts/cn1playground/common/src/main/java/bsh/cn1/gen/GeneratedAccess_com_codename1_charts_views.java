package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_views {
    private GeneratedAccess_com_codename1_charts_views() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.charts.views.AbstractChart".equals(name)) return com.codename1.charts.views.AbstractChart.class;
        if ("com.codename1.charts.views.BarChart".equals(name)) return com.codename1.charts.views.BarChart.class;
        if ("com.codename1.charts.views.BubbleChart".equals(name)) return com.codename1.charts.views.BubbleChart.class;
        if ("com.codename1.charts.views.ClickableArea".equals(name)) return com.codename1.charts.views.ClickableArea.class;
        if ("com.codename1.charts.views.CombinedXYChart".equals(name)) return com.codename1.charts.views.CombinedXYChart.class;
        if ("com.codename1.charts.views.CubicLineChart".equals(name)) return com.codename1.charts.views.CubicLineChart.class;
        if ("com.codename1.charts.views.DialChart".equals(name)) return com.codename1.charts.views.DialChart.class;
        if ("com.codename1.charts.views.DoughnutChart".equals(name)) return com.codename1.charts.views.DoughnutChart.class;
        if ("com.codename1.charts.views.LineChart".equals(name)) return com.codename1.charts.views.LineChart.class;
        if ("com.codename1.charts.views.PieChart".equals(name)) return com.codename1.charts.views.PieChart.class;
        if ("com.codename1.charts.views.PieMapper".equals(name)) return com.codename1.charts.views.PieMapper.class;
        if ("com.codename1.charts.views.PieSegment".equals(name)) return com.codename1.charts.views.PieSegment.class;
        if ("com.codename1.charts.views.PointStyle".equals(name)) return com.codename1.charts.views.PointStyle.class;
        if ("com.codename1.charts.views.RadarChart".equals(name)) return com.codename1.charts.views.RadarChart.class;
        if ("com.codename1.charts.views.RangeBarChart".equals(name)) return com.codename1.charts.views.RangeBarChart.class;
        if ("com.codename1.charts.views.RangeStackedBarChart".equals(name)) return com.codename1.charts.views.RangeStackedBarChart.class;
        if ("com.codename1.charts.views.RoundChart".equals(name)) return com.codename1.charts.views.RoundChart.class;
        if ("com.codename1.charts.views.ScatterChart".equals(name)) return com.codename1.charts.views.ScatterChart.class;
        if ("com.codename1.charts.views.TimeChart".equals(name)) return com.codename1.charts.views.TimeChart.class;
        if ("com.codename1.charts.views.XYChart".equals(name)) return com.codename1.charts.views.XYChart.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.views.BarChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, com.codename1.charts.views.BarChart.Type.class}, false)) {
                return new com.codename1.charts.views.BarChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1], (com.codename1.charts.views.BarChart.Type) safeArgs[2]);
            }
        }
        if (type == com.codename1.charts.views.BubbleChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                return new com.codename1.charts.views.BubbleChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.ClickableArea.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return new com.codename1.charts.views.ClickableArea((com.codename1.ui.geom.Rectangle2D) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue());
            }
        }
        if (type == com.codename1.charts.views.CombinedXYChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef[].class}, false)) {
                return new com.codename1.charts.views.CombinedXYChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1], (com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef[]) safeArgs[2]);
            }
        }
        if (type == com.codename1.charts.views.CubicLineChart.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.charts.views.CubicLineChart();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, java.lang.Float.class}, false)) {
                return new com.codename1.charts.views.CubicLineChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue());
            }
        }
        if (type == com.codename1.charts.views.DialChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class, com.codename1.charts.renderers.DialRenderer.class}, false)) {
                return new com.codename1.charts.views.DialChart((com.codename1.charts.models.CategorySeries) safeArgs[0], (com.codename1.charts.renderers.DialRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.DoughnutChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.MultipleCategorySeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return new com.codename1.charts.views.DoughnutChart((com.codename1.charts.models.MultipleCategorySeries) safeArgs[0], (com.codename1.charts.renderers.DefaultRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.LineChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                return new com.codename1.charts.views.LineChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.PieChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return new com.codename1.charts.views.PieChart((com.codename1.charts.models.CategorySeries) safeArgs[0], (com.codename1.charts.renderers.DefaultRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.PieSegment.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                return new com.codename1.charts.views.PieSegment(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue());
            }
        }
        if (type == com.codename1.charts.views.RadarChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.AreaSeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return new com.codename1.charts.views.RadarChart((com.codename1.charts.models.AreaSeries) safeArgs[0], (com.codename1.charts.renderers.DefaultRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.ScatterChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                return new com.codename1.charts.views.ScatterChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.TimeChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                return new com.codename1.charts.views.TimeChart((com.codename1.charts.models.XYMultipleSeriesDataset) safeArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) safeArgs[1]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.views.PointStyle.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getIndexForName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.charts.views.PointStyle.getIndexForName((java.lang.String) safeArgs[0]);
            }
        }
        if ("getPointStyleForName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.charts.views.PointStyle.getPointStyleForName((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.charts.views.PointStyle.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.charts.views.RangeStackedBarChart) {
            try {
                return invoke0((com.codename1.charts.views.RangeStackedBarChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.CubicLineChart) {
            try {
                return invoke1((com.codename1.charts.views.CubicLineChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.RangeBarChart) {
            try {
                return invoke2((com.codename1.charts.views.RangeBarChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.TimeChart) {
            try {
                return invoke3((com.codename1.charts.views.TimeChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.BarChart) {
            try {
                return invoke4((com.codename1.charts.views.BarChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.BubbleChart) {
            try {
                return invoke5((com.codename1.charts.views.BubbleChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.CombinedXYChart) {
            try {
                return invoke6((com.codename1.charts.views.CombinedXYChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.DialChart) {
            try {
                return invoke7((com.codename1.charts.views.DialChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.DoughnutChart) {
            try {
                return invoke8((com.codename1.charts.views.DoughnutChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.LineChart) {
            try {
                return invoke9((com.codename1.charts.views.LineChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PieChart) {
            try {
                return invoke10((com.codename1.charts.views.PieChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.RadarChart) {
            try {
                return invoke11((com.codename1.charts.views.RadarChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.ScatterChart) {
            try {
                return invoke12((com.codename1.charts.views.ScatterChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.RoundChart) {
            try {
                return invoke13((com.codename1.charts.views.RoundChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.XYChart) {
            try {
                return invoke14((com.codename1.charts.views.XYChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.AbstractChart) {
            try {
                return invoke15((com.codename1.charts.views.AbstractChart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.ClickableArea) {
            try {
                return invoke16((com.codename1.charts.views.ClickableArea) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PieMapper) {
            try {
                return invoke17((com.codename1.charts.views.PieMapper) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PieSegment) {
            try {
                return invoke18((com.codename1.charts.views.PieSegment) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PointStyle) {
            try {
                return invoke19((com.codename1.charts.views.PointStyle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.charts.views.RangeStackedBarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.views.CubicLineChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.views.RangeBarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.views.TimeChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDateFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDateFormat();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setDateFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDateFormat((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.charts.views.BarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.charts.views.BubbleChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.charts.views.CombinedXYChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.charts.views.DialChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAutocalculateCenter(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.charts.views.DoughnutChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAutocalculateCenter(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.charts.views.LineChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.charts.views.PieChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSegmentShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getSegmentShape(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAutocalculateCenter(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.charts.views.RadarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAutocalculateCenter(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.charts.views.ScatterChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.charts.views.RoundChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAutocalculateCenter(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCenterY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.charts.views.XYChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.compat.Paint) safeArgs[1], (java.util.List) safeArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) safeArgs[3], ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue()); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCalcRange(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getChartType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPointsChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                typedTarget.setCalcRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return typedTarget.toRealPoint(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0]);
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return typedTarget.toScreenPoint((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.charts.views.AbstractChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.draw((com.codename1.charts.compat.Canvas) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) safeArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).intValue(), (com.codename1.charts.compat.Paint) safeArgs[5]); return null;
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getLegendShapeWidth(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isNullValue(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.charts.views.ClickableArea typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRect();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getY();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.charts.views.PieMapper typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPieSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.addPieSegment(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("areAllSegmentPresent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.areAllSegmentPresent(((Number) safeArgs[0]).intValue());
            }
        }
        if ("clearPieSegments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearPieSegments(); return null;
            }
        }
        if ("getAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getAngle((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("getSegmentShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getSegmentShape(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("isOnPieChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                return typedTarget.isOnPieChart((com.codename1.charts.models.Point) safeArgs[0]);
            }
        }
        if ("setDimensions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setDimensions(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.charts.views.PieSegment typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.getShape(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue());
            }
        }
        if ("isInSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.isInSegment(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.charts.views.PointStyle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.charts.views.PointStyle.class) {
            if ("CIRCLE".equals(name)) return com.codename1.charts.views.PointStyle.CIRCLE;
            if ("DIAMOND".equals(name)) return com.codename1.charts.views.PointStyle.DIAMOND;
            if ("POINT".equals(name)) return com.codename1.charts.views.PointStyle.POINT;
            if ("SQUARE".equals(name)) return com.codename1.charts.views.PointStyle.SQUARE;
            if ("TRIANGLE".equals(name)) return com.codename1.charts.views.PointStyle.TRIANGLE;
            if ("X".equals(name)) return com.codename1.charts.views.PointStyle.X;
        }
        if (type == com.codename1.charts.views.TimeChart.class) {
            if ("DAY".equals(name)) return com.codename1.charts.views.TimeChart.DAY;
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
