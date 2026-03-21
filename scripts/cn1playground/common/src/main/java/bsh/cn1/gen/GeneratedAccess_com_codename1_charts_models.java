package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_models {
    private GeneratedAccess_com_codename1_charts_models() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.charts.models.AreaSeries".equals(name)) return com.codename1.charts.models.AreaSeries.class;
        if ("com.codename1.charts.models.CategorySeries".equals(name)) return com.codename1.charts.models.CategorySeries.class;
        if ("com.codename1.charts.models.MultipleCategorySeries".equals(name)) return com.codename1.charts.models.MultipleCategorySeries.class;
        if ("com.codename1.charts.models.Point".equals(name)) return com.codename1.charts.models.Point.class;
        if ("com.codename1.charts.models.RangeCategorySeries".equals(name)) return com.codename1.charts.models.RangeCategorySeries.class;
        if ("com.codename1.charts.models.SeriesSelection".equals(name)) return com.codename1.charts.models.SeriesSelection.class;
        if ("com.codename1.charts.models.TimeSeries".equals(name)) return com.codename1.charts.models.TimeSeries.class;
        if ("com.codename1.charts.models.XYMultipleSeriesDataset".equals(name)) return com.codename1.charts.models.XYMultipleSeriesDataset.class;
        if ("com.codename1.charts.models.XYSeries".equals(name)) return com.codename1.charts.models.XYSeries.class;
        if ("com.codename1.charts.models.XYValueSeries".equals(name)) return com.codename1.charts.models.XYValueSeries.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.models.CategorySeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.charts.models.CategorySeries((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.MultipleCategorySeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.charts.models.MultipleCategorySeries((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.Point.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.charts.models.Point();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return new com.codename1.charts.models.Point(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if (type == com.codename1.charts.models.RangeCategorySeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.charts.models.RangeCategorySeries((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.SeriesSelection.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return new com.codename1.charts.models.SeriesSelection(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        if (type == com.codename1.charts.models.TimeSeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.charts.models.TimeSeries((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.XYSeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.charts.models.XYSeries((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return new com.codename1.charts.models.XYSeries((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if (type == com.codename1.charts.models.XYValueSeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.charts.models.XYValueSeries((java.lang.String) safeArgs[0]);
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
        if (target instanceof com.codename1.charts.models.RangeCategorySeries) {
            try {
                return invoke0((com.codename1.charts.models.RangeCategorySeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.TimeSeries) {
            try {
                return invoke1((com.codename1.charts.models.TimeSeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.XYValueSeries) {
            try {
                return invoke2((com.codename1.charts.models.XYValueSeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.AreaSeries) {
            try {
                return invoke3((com.codename1.charts.models.AreaSeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.CategorySeries) {
            try {
                return invoke4((com.codename1.charts.models.CategorySeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.MultipleCategorySeries) {
            try {
                return invoke5((com.codename1.charts.models.MultipleCategorySeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.Point) {
            try {
                return invoke6((com.codename1.charts.models.Point) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.SeriesSelection) {
            try {
                return invoke7((com.codename1.charts.models.SeriesSelection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.XYMultipleSeriesDataset) {
            try {
                return invoke8((com.codename1.charts.models.XYMultipleSeriesDataset) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.charts.models.XYSeries) {
            try {
                return invoke9((com.codename1.charts.models.XYSeries) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.charts.models.RangeCategorySeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                typedTarget.add((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCategory(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaximumValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getMaximumValue(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getMinimumValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getMinimumValue(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getValue(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.remove(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Double.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1], ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("toXYSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toXYSeries();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.models.TimeSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.lang.Double.class}, false)) {
                typedTarget.add((java.util.Date) safeArgs[0], ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.addAnnotation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.addAnnotation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("clearAnnotations".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearAnnotations(); return null;
            }
        }
        if ("clearSeriesValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearSeriesValues(); return null;
            }
        }
        if ("getAnnotationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnnotationCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnnotationCount();
            }
        }
        if ("getAnnotationX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationX(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnnotationY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationY(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getIndexForKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.getIndexForKey(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaxX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxX();
            }
        }
        if ("getMaxY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxY();
            }
        }
        if ("getMinX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinX();
            }
        }
        if ("getMinY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinY();
            }
        }
        if ("getRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                return typedTarget.getRange(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("getScaleNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScaleNumber();
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getX(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getY(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.remove(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("removeAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeAnnotation(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.models.XYValueSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.addAnnotation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.addAnnotation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("clearAnnotations".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearAnnotations(); return null;
            }
        }
        if ("clearSeriesValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearSeriesValues(); return null;
            }
        }
        if ("getAnnotationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnnotationCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnnotationCount();
            }
        }
        if ("getAnnotationX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationX(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnnotationY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationY(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getIndexForKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.getIndexForKey(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaxValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxValue();
            }
        }
        if ("getMaxX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxX();
            }
        }
        if ("getMaxY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxY();
            }
        }
        if ("getMinValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinValue();
            }
        }
        if ("getMinX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinX();
            }
        }
        if ("getMinY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinY();
            }
        }
        if ("getRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                return typedTarget.getRange(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("getScaleNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScaleNumber();
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getValue(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getX(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getY(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.remove(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("removeAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeAnnotation(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.models.AreaSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class}, false)) {
                typedTarget.addSeries((com.codename1.charts.models.CategorySeries) safeArgs[0]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategories".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCategories();
            }
        }
        if ("getCategoriesCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCategoriesCount();
            }
        }
        if ("getSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeries();
            }
        }
        if ("getSeriesCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesCount();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                return typedTarget.getValue(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.charts.models.CategorySeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                typedTarget.add((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCategory(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getItemCount();
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getValue(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.remove(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Double.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1], ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("toXYSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toXYSeries();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.charts.models.MultipleCategorySeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, double[].class}, false)) {
                typedTarget.add((java.lang.String[]) safeArgs[0], (double[]) safeArgs[1]); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class, double[].class}, false)) {
                typedTarget.add((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1], (double[]) safeArgs[2]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategoriesCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCategoriesCount();
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getCategory(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getItemCount(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getTitles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getTitles(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getValues(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.remove(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("toXYSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toXYSeries();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.charts.models.Point typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setX(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setY(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.charts.models.SeriesSelection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPointIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointIndex();
            }
        }
        if ("getSeriesIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesIndex();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValue();
            }
        }
        if ("getXValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getXValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.charts.models.XYMultipleSeriesDataset typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAllSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                typedTarget.addAllSeries((java.util.List) safeArgs[0]); return null;
            }
        }
        if ("addSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYSeries.class}, false)) {
                typedTarget.addSeries((com.codename1.charts.models.XYSeries) safeArgs[0]); return null;
            }
        }
        if ("addSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.models.XYSeries.class}, false)) {
                typedTarget.addSeries(((Number) safeArgs[0]).intValue(), (com.codename1.charts.models.XYSeries) safeArgs[1]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("getSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeries();
            }
        }
        if ("getSeriesAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getSeriesAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getSeriesCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSeriesCount();
            }
        }
        if ("removeSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYSeries.class}, false)) {
                typedTarget.removeSeries((com.codename1.charts.models.XYSeries) safeArgs[0]); return null;
            }
        }
        if ("removeSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeSeries(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.charts.models.XYSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.addAnnotation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.addAnnotation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("clearAnnotations".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearAnnotations(); return null;
            }
        }
        if ("clearSeriesValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearSeriesValues(); return null;
            }
        }
        if ("getAnnotationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnnotationCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnnotationCount();
            }
        }
        if ("getAnnotationX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationX(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getAnnotationY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getAnnotationY(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getIndexForKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.getIndexForKey(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaxX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxX();
            }
        }
        if ("getMaxY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaxY();
            }
        }
        if ("getMinX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinX();
            }
        }
        if ("getMinY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinY();
            }
        }
        if ("getRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                return typedTarget.getRange(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("getScaleNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScaleNumber();
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getX(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getY(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.remove(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("removeAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeAnnotation(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setTitle((java.lang.String) safeArgs[0]); return null;
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
