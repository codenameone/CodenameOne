package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_views {
    private GeneratedAccess_com_codename1_charts_views() {
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
        if ("AbstractChart".equals(simpleName)) {
            return com.codename1.charts.views.AbstractChart.class;
        }
        if ("BarChart".equals(simpleName)) {
            return com.codename1.charts.views.BarChart.class;
        }
        if ("Type".equals(simpleName)) {
            return com.codename1.charts.views.BarChart.Type.class;
        }
        if ("BubbleChart".equals(simpleName)) {
            return com.codename1.charts.views.BubbleChart.class;
        }
        if ("ClickableArea".equals(simpleName)) {
            return com.codename1.charts.views.ClickableArea.class;
        }
        if ("CombinedXYChart".equals(simpleName)) {
            return com.codename1.charts.views.CombinedXYChart.class;
        }
        if ("XYCombinedChartDef".equals(simpleName)) {
            return com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef.class;
        }
        if ("CubicLineChart".equals(simpleName)) {
            return com.codename1.charts.views.CubicLineChart.class;
        }
        if ("DialChart".equals(simpleName)) {
            return com.codename1.charts.views.DialChart.class;
        }
        if ("DoughnutChart".equals(simpleName)) {
            return com.codename1.charts.views.DoughnutChart.class;
        }
        if ("LineChart".equals(simpleName)) {
            return com.codename1.charts.views.LineChart.class;
        }
        if ("PieChart".equals(simpleName)) {
            return com.codename1.charts.views.PieChart.class;
        }
        if ("PieMapper".equals(simpleName)) {
            return com.codename1.charts.views.PieMapper.class;
        }
        if ("PieSegment".equals(simpleName)) {
            return com.codename1.charts.views.PieSegment.class;
        }
        if ("PointStyle".equals(simpleName)) {
            return com.codename1.charts.views.PointStyle.class;
        }
        if ("RadarChart".equals(simpleName)) {
            return com.codename1.charts.views.RadarChart.class;
        }
        if ("RangeBarChart".equals(simpleName)) {
            return com.codename1.charts.views.RangeBarChart.class;
        }
        if ("RangeStackedBarChart".equals(simpleName)) {
            return com.codename1.charts.views.RangeStackedBarChart.class;
        }
        if ("RoundChart".equals(simpleName)) {
            return com.codename1.charts.views.RoundChart.class;
        }
        if ("ScatterChart".equals(simpleName)) {
            return com.codename1.charts.views.ScatterChart.class;
        }
        if ("TimeChart".equals(simpleName)) {
            return com.codename1.charts.views.TimeChart.class;
        }
        if ("XYChart".equals(simpleName)) {
            return com.codename1.charts.views.XYChart.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.views.BarChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, com.codename1.charts.views.BarChart.Type.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, com.codename1.charts.views.BarChart.Type.class}, false);
                return new com.codename1.charts.views.BarChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1], (com.codename1.charts.views.BarChart.Type) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.charts.views.BubbleChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false);
                return new com.codename1.charts.views.BubbleChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.ClickableArea.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class, java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.charts.views.ClickableArea((com.codename1.ui.geom.Rectangle2D) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue());
            }
        }
        if (type == com.codename1.charts.views.CombinedXYChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef[].class}, false);
                return new com.codename1.charts.views.CombinedXYChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1], (com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef[]) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = toIntValue(adaptedArgs[i]);
                }
                return new com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if (type == com.codename1.charts.views.CubicLineChart.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.charts.views.CubicLineChart();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class, java.lang.Float.class}, false);
                return new com.codename1.charts.views.CubicLineChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if (type == com.codename1.charts.views.DialChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class, com.codename1.charts.renderers.DialRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class, com.codename1.charts.renderers.DialRenderer.class}, false);
                return new com.codename1.charts.views.DialChart((com.codename1.charts.models.CategorySeries) adaptedArgs[0], (com.codename1.charts.renderers.DialRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.DoughnutChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.MultipleCategorySeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.MultipleCategorySeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return new com.codename1.charts.views.DoughnutChart((com.codename1.charts.models.MultipleCategorySeries) adaptedArgs[0], (com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.LineChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false);
                return new com.codename1.charts.views.LineChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.PieChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return new com.codename1.charts.views.PieChart((com.codename1.charts.models.CategorySeries) adaptedArgs[0], (com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.PieSegment.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.charts.views.PieSegment(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if (type == com.codename1.charts.views.RadarChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.AreaSeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.AreaSeries.class, com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return new com.codename1.charts.views.RadarChart((com.codename1.charts.models.AreaSeries) adaptedArgs[0], (com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.ScatterChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false);
                return new com.codename1.charts.views.ScatterChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.charts.views.TimeChart.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYMultipleSeriesDataset.class, com.codename1.charts.renderers.XYMultipleSeriesRenderer.class}, false);
                return new com.codename1.charts.views.TimeChart((com.codename1.charts.models.XYMultipleSeriesDataset) adaptedArgs[0], (com.codename1.charts.renderers.XYMultipleSeriesRenderer) adaptedArgs[1]);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.charts.views.PointStyle.getIndexForName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getPointStyleForName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.charts.views.PointStyle.getPointStyleForName((java.lang.String) adaptedArgs[0]);
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
        if (target instanceof com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef) {
            try {
                return invoke17((com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PieMapper) {
            try {
                return invoke18((com.codename1.charts.views.PieMapper) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PieSegment) {
            try {
                return invoke19((com.codename1.charts.views.PieSegment) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.views.PointStyle) {
            try {
                return invoke20((com.codename1.charts.views.PointStyle) target, name, safeArgs);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.views.CubicLineChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.views.RangeBarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.views.TimeChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDateFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDateFormat();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setDateFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDateFormat((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.charts.views.BarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.charts.views.BubbleChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.charts.views.CombinedXYChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.charts.views.DialChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocalculateCenter(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.charts.views.DoughnutChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocalculateCenter(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.charts.views.LineChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.charts.views.PieChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSegmentShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSegmentShape(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocalculateCenter(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.charts.views.RadarChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocalculateCenter(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.charts.views.ScatterChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.charts.views.RoundChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawTitle((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.charts.compat.Paint) adaptedArgs[4]); return null;
            }
        }
        if ("getCenterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterX();
            }
        }
        if ("getCenterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterY();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isAutocalculateCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutocalculateCenter();
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setAutocalculateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocalculateCenter(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.charts.views.XYChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.compat.Paint.class, java.util.List.class, com.codename1.charts.renderers.XYSeriesRenderer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawSeries((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.compat.Paint) adaptedArgs[1], (java.util.List) adaptedArgs[2], (com.codename1.charts.renderers.XYSeriesRenderer) adaptedArgs[3], ((Number) adaptedArgs[4]).floatValue(), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6])); return null;
            }
        }
        if ("getCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCalcRange(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChartType();
            }
        }
        if ("getDataset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDataset();
            }
        }
        if ("getDefaultMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMinimum();
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPointsChart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointsChart();
            }
        }
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isRenderPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.SimpleSeriesRenderer.class}, false);
                return typedTarget.isRenderPoints((com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[0]);
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        if ("setCalcRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setCalcRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toRealPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.toRealPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScreenPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return typedTarget.toScreenPoint((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.charts.views.AbstractChart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("draw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.draw((com.codename1.charts.compat.Canvas) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("drawLegendShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.compat.Canvas.class, com.codename1.charts.renderers.SimpleSeriesRenderer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.charts.compat.Paint.class}, false);
                typedTarget.drawLegendShape((com.codename1.charts.compat.Canvas) adaptedArgs[0], (com.codename1.charts.renderers.SimpleSeriesRenderer) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]), (com.codename1.charts.compat.Paint) adaptedArgs[5]); return null;
            }
        }
        if ("getLegendShapeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLegendShapeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isNullValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isNullValue(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.renderers.DefaultRenderer.class}, false);
                return typedTarget.isVertical((com.codename1.charts.renderers.DefaultRenderer) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.charts.views.ClickableArea typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRect();
            }
        }
        if ("getX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getY();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.charts.views.CombinedXYChart.XYCombinedChartDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("containsSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.containsSeries(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChartSeriesIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getChartSeriesIndex(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getSeriesIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesIndex();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.charts.views.PieMapper typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPieSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.addPieSegment(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("areAllSegmentPresent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.areAllSegmentPresent(toIntValue(adaptedArgs[0]));
            }
        }
        if ("clearPieSegments".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearPieSegments(); return null;
            }
        }
        if ("getAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getAngle((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("getSegmentShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSegmentShape(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getSeriesAndPointForScreenCoordinate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.getSeriesAndPointForScreenCoordinate((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("isOnPieChart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.Point.class}, false);
                return typedTarget.isOnPieChart((com.codename1.charts.models.Point) adaptedArgs[0]);
            }
        }
        if ("setDimensions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setDimensions(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.charts.views.PieSegment typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.getShape(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("isInSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.isInSegment(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.charts.views.PointStyle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
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
        if (type == com.codename1.charts.views.BarChart.Type.class) return getStaticField0(name);
        if (type == com.codename1.charts.views.PointStyle.class) return getStaticField1(name);
        if (type == com.codename1.charts.views.TimeChart.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DEFAULT".equals(name)) return com.codename1.charts.views.BarChart.Type.DEFAULT;
        if ("HEAPED".equals(name)) return com.codename1.charts.views.BarChart.Type.HEAPED;
        if ("STACKED".equals(name)) return com.codename1.charts.views.BarChart.Type.STACKED;
        throw unsupportedStaticField(com.codename1.charts.views.BarChart.Type.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CIRCLE".equals(name)) return com.codename1.charts.views.PointStyle.CIRCLE;
        if ("DIAMOND".equals(name)) return com.codename1.charts.views.PointStyle.DIAMOND;
        if ("POINT".equals(name)) return com.codename1.charts.views.PointStyle.POINT;
        if ("SQUARE".equals(name)) return com.codename1.charts.views.PointStyle.SQUARE;
        if ("TRIANGLE".equals(name)) return com.codename1.charts.views.PointStyle.TRIANGLE;
        if ("X".equals(name)) return com.codename1.charts.views.PointStyle.X;
        throw unsupportedStaticField(com.codename1.charts.views.PointStyle.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("DAY".equals(name)) return com.codename1.charts.views.TimeChart.DAY;
        throw unsupportedStaticField(com.codename1.charts.views.TimeChart.class, name);
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
