package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_charts_models {
    private GeneratedAccess_com_codename1_charts_models() {
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
        if ("AreaSeries".equals(simpleName)) {
            return com.codename1.charts.models.AreaSeries.class;
        }
        if ("CategorySeries".equals(simpleName)) {
            return com.codename1.charts.models.CategorySeries.class;
        }
        if ("MultipleCategorySeries".equals(simpleName)) {
            return com.codename1.charts.models.MultipleCategorySeries.class;
        }
        if ("Point".equals(simpleName)) {
            return com.codename1.charts.models.Point.class;
        }
        if ("RangeCategorySeries".equals(simpleName)) {
            return com.codename1.charts.models.RangeCategorySeries.class;
        }
        if ("SeriesSelection".equals(simpleName)) {
            return com.codename1.charts.models.SeriesSelection.class;
        }
        if ("TimeSeries".equals(simpleName)) {
            return com.codename1.charts.models.TimeSeries.class;
        }
        if ("XYMultipleSeriesDataset".equals(simpleName)) {
            return com.codename1.charts.models.XYMultipleSeriesDataset.class;
        }
        if ("XYSeries".equals(simpleName)) {
            return com.codename1.charts.models.XYSeries.class;
        }
        if ("XYValueSeries".equals(simpleName)) {
            return com.codename1.charts.models.XYValueSeries.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.charts.models.CategorySeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.charts.models.CategorySeries((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.MultipleCategorySeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.charts.models.MultipleCategorySeries((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.Point.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.charts.models.Point();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.charts.models.Point(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == com.codename1.charts.models.RangeCategorySeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.charts.models.RangeCategorySeries((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.SeriesSelection.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.charts.models.SeriesSelection(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue());
            }
        }
        if (type == com.codename1.charts.models.TimeSeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.charts.models.TimeSeries((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.charts.models.XYSeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.charts.models.XYSeries((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.charts.models.XYSeries((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if (type == com.codename1.charts.models.XYValueSeries.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.charts.models.XYValueSeries((java.lang.String) adaptedArgs[0]);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                typedTarget.add((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCategory(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaximumValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMaximumValue(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMinimumValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMinimumValue(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.remove(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Double.class}, false);
                typedTarget.set(((Number) adaptedArgs[0]).intValue(), (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("toXYSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toXYSeries();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.charts.models.TimeSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.lang.Double.class}, false);
                typedTarget.add((java.util.Date) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addAnnotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addAnnotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("clearAnnotations".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearAnnotations(); return null;
            }
        }
        if ("clearSeriesValues".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearSeriesValues(); return null;
            }
        }
        if ("getAnnotationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getAnnotationCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnnotationCount();
            }
        }
        if ("getAnnotationX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getAnnotationY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getIndexForKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.getIndexForKey(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaxX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxX();
            }
        }
        if ("getMaxY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxY();
            }
        }
        if ("getMinX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinX();
            }
        }
        if ("getMinY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinY();
            }
        }
        if ("getRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false);
                return typedTarget.getRange(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("getScaleNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleNumber();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.remove(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("removeAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeAnnotation(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.charts.models.XYValueSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addAnnotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addAnnotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("clearAnnotations".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearAnnotations(); return null;
            }
        }
        if ("clearSeriesValues".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearSeriesValues(); return null;
            }
        }
        if ("getAnnotationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getAnnotationCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnnotationCount();
            }
        }
        if ("getAnnotationX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getAnnotationY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getIndexForKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.getIndexForKey(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaxValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxValue();
            }
        }
        if ("getMaxX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxX();
            }
        }
        if ("getMaxY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxY();
            }
        }
        if ("getMinValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinValue();
            }
        }
        if ("getMinX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinX();
            }
        }
        if ("getMinY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinY();
            }
        }
        if ("getRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false);
                return typedTarget.getRange(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("getScaleNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleNumber();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.remove(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("removeAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeAnnotation(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.charts.models.AreaSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.CategorySeries.class}, false);
                typedTarget.addSeries((com.codename1.charts.models.CategorySeries) adaptedArgs[0]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategories".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategories();
            }
        }
        if ("getCategoriesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategoriesCount();
            }
        }
        if ("getSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeries();
            }
        }
        if ("getSeriesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesCount();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return typedTarget.getValue(((Number) adaptedArgs[0]).intValue(), (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.charts.models.CategorySeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                typedTarget.add((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCategory(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItemCount();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.remove(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Double.class}, false);
                typedTarget.set(((Number) adaptedArgs[0]).intValue(), (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("toXYSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toXYSeries();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.charts.models.MultipleCategorySeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, double[].class}, false);
                typedTarget.add((java.lang.String[]) adaptedArgs[0], (double[]) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class, double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class, double[].class}, false);
                typedTarget.add((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1], (double[]) adaptedArgs[2]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getCategoriesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategoriesCount();
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getCategory(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getItemCount(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getTitles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getTitles(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValues(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.remove(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("toXYSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toXYSeries();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.charts.models.Point typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setX(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setY(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.charts.models.SeriesSelection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPointIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPointIndex();
            }
        }
        if ("getSeriesIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesIndex();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getXValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.charts.models.XYMultipleSeriesDataset typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAllSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                typedTarget.addAllSeries((java.util.List) adaptedArgs[0]); return null;
            }
        }
        if ("addSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYSeries.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYSeries.class}, false);
                typedTarget.addSeries((com.codename1.charts.models.XYSeries) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.models.XYSeries.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.charts.models.XYSeries.class}, false);
                typedTarget.addSeries(((Number) adaptedArgs[0]).intValue(), (com.codename1.charts.models.XYSeries) adaptedArgs[1]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeries();
            }
        }
        if ("getSeriesAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSeriesAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getSeriesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeriesCount();
            }
        }
        if ("removeSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeSeries(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.charts.models.XYSeries.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.charts.models.XYSeries.class}, false);
                typedTarget.removeSeries((com.codename1.charts.models.XYSeries) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.charts.models.XYSeries typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.add(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("addAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addAnnotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addAnnotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("clearAnnotations".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearAnnotations(); return null;
            }
        }
        if ("clearSeriesValues".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearSeriesValues(); return null;
            }
        }
        if ("getAnnotationAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getAnnotationCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnnotationCount();
            }
        }
        if ("getAnnotationX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getAnnotationY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getAnnotationY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getIndexForKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.getIndexForKey(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("getItemCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItemCount();
            }
        }
        if ("getMaxX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxX();
            }
        }
        if ("getMaxY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxY();
            }
        }
        if ("getMinX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinX();
            }
        }
        if ("getMinY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinY();
            }
        }
        if ("getRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false);
                return typedTarget.getRange(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("getScaleNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleNumber();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.remove(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("removeAnnotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeAnnotation(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
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
