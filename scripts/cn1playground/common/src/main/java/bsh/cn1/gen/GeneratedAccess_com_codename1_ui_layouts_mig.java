package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_layouts_mig {
    private GeneratedAccess_com_codename1_ui_layouts_mig() {
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
        if ("AC".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.AC.class;
        }
        if ("BoundSize".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.BoundSize.class;
        }
        if ("CC".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.CC.class;
        }
        if ("ComponentWrapper".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.ComponentWrapper.class;
        }
        if ("ConstraintParser".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.ConstraintParser.class;
        }
        if ("ContainerWrapper".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.ContainerWrapper.class;
        }
        if ("DimConstraint".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.DimConstraint.class;
        }
        if ("Grid".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.Grid.class;
        }
        if ("InCellGapProvider".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.InCellGapProvider.class;
        }
        if ("LC".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.LC.class;
        }
        if ("LayoutCallback".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.LayoutCallback.class;
        }
        if ("LayoutUtil".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.LayoutUtil.class;
        }
        if ("LinkHandler".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.LinkHandler.class;
        }
        if ("MigLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.MigLayout.class;
        }
        if ("PlatformDefaults".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.PlatformDefaults.class;
        }
        if ("UnitConverter".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.UnitConverter.class;
        }
        if ("UnitValue".equals(simpleName)) {
            return com.codename1.ui.layouts.mig.UnitValue.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.layouts.mig.AC.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.mig.AC();
            }
        }
        if (type == com.codename1.ui.layouts.mig.BoundSize.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.BoundSize((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.BoundSize((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.Boolean.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.Boolean.class, java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.BoundSize((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), (java.lang.String) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.ui.layouts.mig.Grid.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.AC.class, com.codename1.ui.layouts.mig.AC.class, java.util.Map.class, java.util.ArrayList.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.AC.class, com.codename1.ui.layouts.mig.AC.class, java.util.Map.class, java.util.ArrayList.class}, false);
                return new com.codename1.ui.layouts.mig.Grid((com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[0], (com.codename1.ui.layouts.mig.LC) adaptedArgs[1], (com.codename1.ui.layouts.mig.AC) adaptedArgs[2], (com.codename1.ui.layouts.mig.AC) adaptedArgs[3], (java.util.Map) adaptedArgs[4], (java.util.ArrayList) adaptedArgs[5]);
            }
        }
        if (type == com.codename1.ui.layouts.mig.MigLayout.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.mig.MigLayout();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class}, false);
                return new com.codename1.ui.layouts.mig.MigLayout((com.codename1.ui.layouts.mig.LC) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.MigLayout((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.AC.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.AC.class}, false);
                return new com.codename1.ui.layouts.mig.MigLayout((com.codename1.ui.layouts.mig.LC) adaptedArgs[0], (com.codename1.ui.layouts.mig.AC) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.MigLayout((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.AC.class, com.codename1.ui.layouts.mig.AC.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.AC.class, com.codename1.ui.layouts.mig.AC.class}, false);
                return new com.codename1.ui.layouts.mig.MigLayout((com.codename1.ui.layouts.mig.LC) adaptedArgs[0], (com.codename1.ui.layouts.mig.AC) adaptedArgs[1], (com.codename1.ui.layouts.mig.AC) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.MigLayout((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ui.layouts.mig.UnitValue.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return new com.codename1.ui.layouts.mig.UnitValue(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.ui.layouts.mig.UnitValue(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).intValue(), (java.lang.String) adaptedArgs[2]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.layouts.mig.ConstraintParser.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.layouts.mig.LayoutUtil.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.layouts.mig.LinkHandler.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ui.layouts.mig.PlatformDefaults.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ui.layouts.mig.UnitValue.class) return invokeStatic4(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("parseBoundSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseBoundSize((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("parseColumnConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseColumnConstraints((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseComponentConstraint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseComponentConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseComponentConstraints((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("parseInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseInsets((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("parseLayoutConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseLayoutConstraint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseRowConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseRowConstraints((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseUnitValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseUnitValue((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("parseUnitValueOrAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.parseUnitValueOrAlign((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2]);
            }
        }
        if ("prepare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.ConstraintParser.prepare((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.mig.ConstraintParser.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getDesignTimeEmptySize".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.LayoutUtil.getDesignTimeEmptySize();
            }
        }
        if ("getGlobalDebugMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.LayoutUtil.getGlobalDebugMillis();
            }
        }
        if ("getSerializedObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.ui.layouts.mig.LayoutUtil.getSerializedObject((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getSizeSafe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false);
                return com.codename1.ui.layouts.mig.LayoutUtil.getSizeSafe((int[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.LayoutUtil.getVersion();
            }
        }
        if ("isDesignTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ContainerWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ContainerWrapper.class}, false);
                return com.codename1.ui.layouts.mig.LayoutUtil.isDesignTime((com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[0]);
            }
        }
        if ("isLeftToRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.ContainerWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LC.class, com.codename1.ui.layouts.mig.ContainerWrapper.class}, false);
                return com.codename1.ui.layouts.mig.LayoutUtil.isLeftToRight((com.codename1.ui.layouts.mig.LC) adaptedArgs[0], (com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[1]);
            }
        }
        if ("setDesignTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ContainerWrapper.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ContainerWrapper.class, java.lang.Boolean.class}, false);
                com.codename1.ui.layouts.mig.LayoutUtil.setDesignTime((com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setDesignTimeEmptySize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.LayoutUtil.setDesignTimeEmptySize(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setGlobalDebugMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.LayoutUtil.setGlobalDebugMillis(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSerializedObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                com.codename1.ui.layouts.mig.LayoutUtil.setSerializedObject((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.mig.LayoutUtil.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("clearBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.LinkHandler.clearBounds((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("clearWeakReferencesNow".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.ui.layouts.mig.LinkHandler.clearWeakReferencesNow(); return null;
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class}, false);
                return com.codename1.ui.layouts.mig.LinkHandler.getValue((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("setBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.layouts.mig.LinkHandler.setBounds((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.mig.LinkHandler.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getButtonOrder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getButtonOrder();
            }
        }
        if ("getCurrentPlatform".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getCurrentPlatform();
            }
        }
        if ("getDefaultDPI".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getDefaultDPI();
            }
        }
        if ("getDefaultHorizontalUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getDefaultHorizontalUnit();
            }
        }
        if ("getDefaultRowAlignmentBaseline".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getDefaultRowAlignmentBaseline();
            }
        }
        if ("getDefaultVerticalUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getDefaultVerticalUnit();
            }
        }
        if ("getDefaultVisualPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.PlatformDefaults.getDefaultVisualPadding((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getDialogInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.layouts.mig.PlatformDefaults.getDialogInsets(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getGapProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getGapProvider();
            }
        }
        if ("getGridGapX".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getGridGapX();
            }
        }
        if ("getGridGapY".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getGridGapY();
            }
        }
        if ("getHorizontalScaleFactor".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getHorizontalScaleFactor();
            }
        }
        if ("getLabelAlignPercentage".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getLabelAlignPercentage();
            }
        }
        if ("getLogicalPixelBase".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getLogicalPixelBase();
            }
        }
        if ("getMinimumButtonWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getMinimumButtonWidth();
            }
        }
        if ("getModCount".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getModCount();
            }
        }
        if ("getPanelInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.layouts.mig.PlatformDefaults.getPanelInsets(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getPlatform".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getPlatform();
            }
        }
        if ("getPlatformDPI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.layouts.mig.PlatformDefaults.getPlatformDPI(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getUnitValueX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.PlatformDefaults.getUnitValueX((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getUnitValueY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.layouts.mig.PlatformDefaults.getUnitValueY((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getVerticalScaleFactor".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.PlatformDefaults.getVerticalScaleFactor();
            }
        }
        if ("setButtonOrder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setButtonOrder((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultDPI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setDefaultDPI(Integer.valueOf(((Number) adaptedArgs[0]).intValue())); return null;
            }
        }
        if ("setDefaultHorizontalUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setDefaultHorizontalUnit(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDefaultRowAlignmentBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setDefaultRowAlignmentBaseline(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultVerticalUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setDefaultVerticalUnit(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDefaultVisualPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setDefaultVisualPadding((java.lang.String) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("setDialogInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setDialogInsets((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[3]); return null;
            }
        }
        if ("setGapProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.InCellGapProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.InCellGapProvider.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setGapProvider((com.codename1.ui.layouts.mig.InCellGapProvider) adaptedArgs[0]); return null;
            }
        }
        if ("setGridCellGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setGridCellGap((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1]); return null;
            }
        }
        if ("setHorizontalScaleFactor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setHorizontalScaleFactor(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setIndentGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setIndentGap((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1]); return null;
            }
        }
        if ("setLogicalPixelBase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setLogicalPixelBase(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setMinimumButtonWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setMinimumButtonWidth((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0]); return null;
            }
        }
        if ("setPanelInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setPanelInsets((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[3]); return null;
            }
        }
        if ("setParagraphGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setParagraphGap((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1]); return null;
            }
        }
        if ("setPlatform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setPlatform(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setRelatedGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setRelatedGap((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1]); return null;
            }
        }
        if ("setUnitValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setUnitValue((java.lang.String[]) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2]); return null;
            }
        }
        if ("setUnrelatedGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setUnrelatedGap((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1]); return null;
            }
        }
        if ("setVerticalScaleFactor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                com.codename1.ui.layouts.mig.PlatformDefaults.setVerticalScaleFactor(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.mig.PlatformDefaults.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("addGlobalUnitConverter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitConverter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitConverter.class}, false);
                com.codename1.ui.layouts.mig.UnitValue.addGlobalUnitConverter((com.codename1.ui.layouts.mig.UnitConverter) adaptedArgs[0]); return null;
            }
        }
        if ("getDefaultUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.UnitValue.getDefaultUnit();
            }
        }
        if ("getGlobalUnitConverters".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.mig.UnitValue.getGlobalUnitConverters();
            }
        }
        if ("removeGlobalUnitConverter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitConverter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitConverter.class}, false);
                return com.codename1.ui.layouts.mig.UnitValue.removeGlobalUnitConverter((com.codename1.ui.layouts.mig.UnitConverter) adaptedArgs[0]);
            }
        }
        if ("setDefaultUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.ui.layouts.mig.UnitValue.setDefaultUnit(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.mig.UnitValue.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.layouts.mig.AC) {
            try {
                return invoke0((com.codename1.ui.layouts.mig.AC) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.BoundSize) {
            try {
                return invoke1((com.codename1.ui.layouts.mig.BoundSize) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.CC) {
            try {
                return invoke2((com.codename1.ui.layouts.mig.CC) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.DimConstraint) {
            try {
                return invoke3((com.codename1.ui.layouts.mig.DimConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.Grid) {
            try {
                return invoke4((com.codename1.ui.layouts.mig.Grid) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.LC) {
            try {
                return invoke5((com.codename1.ui.layouts.mig.LC) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.LayoutCallback) {
            try {
                return invoke6((com.codename1.ui.layouts.mig.LayoutCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.MigLayout) {
            try {
                return invoke7((com.codename1.ui.layouts.mig.MigLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.PlatformDefaults) {
            try {
                return invoke8((com.codename1.ui.layouts.mig.PlatformDefaults) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.UnitConverter) {
            try {
                return invoke9((com.codename1.ui.layouts.mig.UnitConverter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.UnitValue) {
            try {
                return invoke10((com.codename1.ui.layouts.mig.UnitValue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.ComponentWrapper) {
            try {
                return invoke11((com.codename1.ui.layouts.mig.ComponentWrapper) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.ContainerWrapper) {
            try {
                return invoke12((com.codename1.ui.layouts.mig.ContainerWrapper) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.mig.InCellGapProvider) {
            try {
                return invoke13((com.codename1.ui.layouts.mig.InCellGapProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.layouts.mig.AC typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("align".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.align((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.align((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if ("count".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.count(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("fill".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.fill();
            }
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.fill(varArgs);
            }
        }
        if ("gap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.gap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gap((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.gap((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if ("getConstaints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConstaints();
            }
        }
        if ("getCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCount();
            }
        }
        if ("grow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.grow();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.grow(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.grow(((Number) adaptedArgs[0]).floatValue(), varArgs);
            }
        }
        if ("growPrio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.growPrio(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.growPrio(((Number) adaptedArgs[0]).intValue(), varArgs);
            }
        }
        if ("index".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.index(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("noGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.noGrid();
            }
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.noGrid(varArgs);
            }
        }
        if ("setConstaints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.DimConstraint[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.DimConstraint[].class}, false);
                typedTarget.setConstaints((com.codename1.ui.layouts.mig.DimConstraint[]) adaptedArgs[0]); return null;
            }
        }
        if ("shrink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shrink();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shrink(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.shrink(((Number) adaptedArgs[0]).floatValue(), varArgs);
            }
        }
        if ("shrinkPrio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shrinkPrio(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.shrinkPrio(((Number) adaptedArgs[0]).intValue(), varArgs);
            }
        }
        if ("shrinkWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shrinkWeight(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.shrinkWeight(((Number) adaptedArgs[0]).floatValue(), varArgs);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.size((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.size((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if ("sizeGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.sizeGroup();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.sizeGroup((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.sizeGroup((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.layouts.mig.BoundSize typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("constrain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class}, false);
                return typedTarget.constrain(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).floatValue(), (com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[2]);
            }
        }
        if ("getGapPush".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGapPush();
            }
        }
        if ("getMax".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMax();
            }
        }
        if ("getMin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMin();
            }
        }
        if ("getPreferred".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferred();
            }
        }
        if ("isUnset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUnset();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.layouts.mig.CC typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("alignX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.alignX((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("alignY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.alignY((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("cell".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.cell(varArgs);
            }
        }
        if ("dockEast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dockEast();
            }
        }
        if ("dockNorth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dockNorth();
            }
        }
        if ("dockSouth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dockSouth();
            }
        }
        if ("dockWest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dockWest();
            }
        }
        if ("endGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.endGroup(varArgs);
            }
        }
        if ("endGroupX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.endGroupX((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("endGroupY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.endGroupY((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("external".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.external();
            }
        }
        if ("flowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.flowX();
            }
        }
        if ("flowY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.flowY();
            }
        }
        if ("gap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.gap(varArgs);
            }
        }
        if ("gapAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gapAfter((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gapBefore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gapBefore((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gapBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gapBottom((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gapLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gapLeft((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gapRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gapRight((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gapTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gapTop((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gapX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.gapX((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("gapY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.gapY((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getCellX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCellX();
            }
        }
        if ("getCellY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCellY();
            }
        }
        if ("getDimConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getDimConstraint(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getDockSide".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDockSide();
            }
        }
        if ("getFlowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFlowX();
            }
        }
        if ("getHideMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHideMode();
            }
        }
        if ("getHorizontal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontal();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getNewlineGapSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNewlineGapSize();
            }
        }
        if ("getPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPadding();
            }
        }
        if ("getPos".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPos();
            }
        }
        if ("getPushX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPushX();
            }
        }
        if ("getPushY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPushY();
            }
        }
        if ("getSkip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSkip();
            }
        }
        if ("getSpanX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpanX();
            }
        }
        if ("getSpanY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpanY();
            }
        }
        if ("getSplit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSplit();
            }
        }
        if ("getTag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTag();
            }
        }
        if ("getVertical".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVertical();
            }
        }
        if ("getVisualPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisualPadding();
            }
        }
        if ("getWrapGapSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWrapGapSize();
            }
        }
        if ("grow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.grow();
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, true);
                float[] varArgs = new float[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).floatValue();
                }
                return typedTarget.grow(varArgs);
            }
        }
        if ("growPrio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.growPrio(varArgs);
            }
        }
        if ("growPrioX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.growPrioX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("growPrioY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.growPrioY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("growX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.growX();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.growX(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("growY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.growY();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.growY(Float.valueOf(((Number) adaptedArgs[0]).floatValue()));
            }
        }
        if ("height".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.height((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("hideMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.hideMode(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("id".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.id((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isBoundsInGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBoundsInGrid();
            }
        }
        if ("isExternal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExternal();
            }
        }
        if ("isNewline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNewline();
            }
        }
        if ("isWrap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWrap();
            }
        }
        if ("maxHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.maxHeight((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("maxWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.maxWidth((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("minHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.minHeight((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("minWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.minWidth((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("newline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.newline();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.newline((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pad".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.pad((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.pad(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
        }
        if ("pos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.pos((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.pos((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("push".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.push();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.push(Float.valueOf(((Number) adaptedArgs[0]).floatValue()), Float.valueOf(((Number) adaptedArgs[1]).floatValue()));
            }
        }
        if ("pushX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pushX();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.pushX(Float.valueOf(((Number) adaptedArgs[0]).floatValue()));
            }
        }
        if ("pushY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pushY();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.pushY(Float.valueOf(((Number) adaptedArgs[0]).floatValue()));
            }
        }
        if ("setCellX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCellX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCellY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCellY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDockSide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDockSide(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setExternal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setExternal(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlowX(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setHideMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHideMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setHorizontal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.DimConstraint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.DimConstraint.class}, false);
                typedTarget.setHorizontal((com.codename1.ui.layouts.mig.DimConstraint) adaptedArgs[0]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNewline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNewline(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setNewlineGapSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setNewlineGapSize((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false);
                typedTarget.setPadding((com.codename1.ui.layouts.mig.UnitValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("setPos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false);
                typedTarget.setPos((com.codename1.ui.layouts.mig.UnitValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("setPushX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPushX(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setPushY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPushY(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setSkip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSkip(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSpanX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSpanX(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSpanY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSpanY(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSplit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSplit(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTag((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.DimConstraint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.DimConstraint.class}, false);
                typedTarget.setVertical((com.codename1.ui.layouts.mig.DimConstraint) adaptedArgs[0]); return null;
            }
        }
        if ("setVisualPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false);
                typedTarget.setVisualPadding((com.codename1.ui.layouts.mig.UnitValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("setWrap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWrap(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWrapGapSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setWrapGapSize((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("shrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, true);
                float[] varArgs = new float[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).floatValue();
                }
                return typedTarget.shrink(varArgs);
            }
        }
        if ("shrinkPrio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.shrinkPrio(varArgs);
            }
        }
        if ("shrinkPrioX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shrinkPrioX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("shrinkPrioY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shrinkPrioY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("shrinkX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shrinkX(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("shrinkY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shrinkY(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("sizeGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.sizeGroup(varArgs);
            }
        }
        if ("sizeGroupX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.sizeGroupX((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("sizeGroupY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.sizeGroupY((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("skip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.skip();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("span".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = ((Number) adaptedArgs[i]).intValue();
                }
                return typedTarget.span(varArgs);
            }
        }
        if ("spanX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.spanX();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.spanX(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("spanY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.spanY();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.spanY(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("split".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.split();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.split(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("tag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.tag((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("width".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.width((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("wrap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wrap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.wrap((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("x".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.x((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("x2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.x2((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("y".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.y((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("y2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.y2((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.layouts.mig.DimConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlign();
            }
        }
        if ("getAlignOrDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getAlignOrDefault(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getEndGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndGroup();
            }
        }
        if ("getGapAfter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGapAfter();
            }
        }
        if ("getGapBefore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGapBefore();
            }
        }
        if ("getGrow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGrow();
            }
        }
        if ("getGrowPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGrowPriority();
            }
        }
        if ("getShrink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShrink();
            }
        }
        if ("getShrinkPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShrinkPriority();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("getSizeGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSizeGroup();
            }
        }
        if ("isFill".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFill();
            }
        }
        if ("isNoGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNoGrid();
            }
        }
        if ("setAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false);
                typedTarget.setAlign((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0]); return null;
            }
        }
        if ("setEndGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setEndGroup((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setFill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFill(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGapAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setGapAfter((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setGapBefore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setGapBefore((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setGrow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setGrow(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setGrowPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setGrowPriority(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setNoGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNoGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setShrink(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setShrinkPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setShrinkPriority(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setSize((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setSizeGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSizeGroup((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.layouts.mig.Grid typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getContainer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContainer();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("invalidateContainerSize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidateContainerSize(); return null;
            }
        }
        if ("layout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.Boolean.class}, false);
                return typedTarget.layout((int[]) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, com.codename1.ui.layouts.mig.UnitValue.class, com.codename1.ui.layouts.mig.UnitValue.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return typedTarget.layout((int[]) adaptedArgs[0], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[1], (com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue());
            }
        }
        if ("paintDebug".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintDebug(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.layouts.mig.LC typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("align".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.align((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("alignX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.alignX((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("alignY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.alignY((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("bottomToTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.bottomToTop();
            }
        }
        if ("debug".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.debug();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.debug(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("fill".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.fill();
            }
        }
        if ("fillX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.fillX();
            }
        }
        if ("fillY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.fillY();
            }
        }
        if ("flowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.flowX();
            }
        }
        if ("flowY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.flowY();
            }
        }
        if ("getAlignX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignX();
            }
        }
        if ("getAlignY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignY();
            }
        }
        if ("getDebugMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDebugMillis();
            }
        }
        if ("getGridGapX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGridGapX();
            }
        }
        if ("getGridGapY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGridGapY();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getHideMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHideMode();
            }
        }
        if ("getInsets".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInsets();
            }
        }
        if ("getLeftToRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeftToRight();
            }
        }
        if ("getPackHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPackHeight();
            }
        }
        if ("getPackHeightAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPackHeightAlign();
            }
        }
        if ("getPackWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPackWidth();
            }
        }
        if ("getPackWidthAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPackWidthAlign();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("getWrapAfter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWrapAfter();
            }
        }
        if ("gridGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.gridGap((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("gridGapX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gridGapX((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gridGapY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.gridGapY((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("height".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.height((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("hideMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.hideMode(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("insets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.insets((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.insets((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("insetsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.insetsAll((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isFillX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFillX();
            }
        }
        if ("isFillY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFillY();
            }
        }
        if ("isFlowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlowX();
            }
        }
        if ("isNoCache".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNoCache();
            }
        }
        if ("isNoGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNoGrid();
            }
        }
        if ("isTopToBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTopToBottom();
            }
        }
        if ("isVisualPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisualPadding();
            }
        }
        if ("leftToRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.leftToRight(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("maxHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.maxHeight((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("maxWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.maxWidth((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("minHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.minHeight((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("minWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.minWidth((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("noCache".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.noCache();
            }
        }
        if ("noGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.noGrid();
            }
        }
        if ("noVisualPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.noVisualPadding();
            }
        }
        if ("pack".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pack();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.pack((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("packAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.packAlign(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("rightToLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.rightToLeft();
            }
        }
        if ("setAlignX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false);
                typedTarget.setAlignX((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0]); return null;
            }
        }
        if ("setAlignY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue.class}, false);
                typedTarget.setAlignY((com.codename1.ui.layouts.mig.UnitValue) adaptedArgs[0]); return null;
            }
        }
        if ("setDebugMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDebugMillis(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFillX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFillX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFillY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFillY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlowX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGridGapX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setGridGapX((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setGridGapY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setGridGapY((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setHeight((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setHideMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHideMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.UnitValue[].class}, false);
                typedTarget.setInsets((com.codename1.ui.layouts.mig.UnitValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("setLeftToRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setLeftToRight(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setNoCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNoCache(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setNoGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNoGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPackHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setPackHeight((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setPackHeightAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPackHeightAlign(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setPackWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setPackWidth((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setPackWidthAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPackWidthAlign(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setTopToBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTopToBottom(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setVisualPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisualPadding(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.BoundSize.class}, false);
                typedTarget.setWidth((com.codename1.ui.layouts.mig.BoundSize) adaptedArgs[0]); return null;
            }
        }
        if ("setWrapAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWrapAfter(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("topToBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.topToBottom();
            }
        }
        if ("width".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.width((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("wrap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wrap();
            }
        }
        if ("wrapAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.wrapAfter(((Number) adaptedArgs[0]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.layouts.mig.LayoutCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("correctBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class}, false);
                typedTarget.correctBounds((com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[0]); return null;
            }
        }
        if ("getPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class}, false);
                return typedTarget.getPosition((com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[0]);
            }
        }
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class}, false);
                return typedTarget.getSize((com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.layouts.mig.MigLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LayoutCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LayoutCallback.class}, false);
                typedTarget.addLayoutCallback((com.codename1.ui.layouts.mig.LayoutCallback) adaptedArgs[0]); return null;
            }
        }
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false);
                typedTarget.addLayoutComponent((com.codename1.ui.Component) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getColumnConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColumnConstraints();
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraints((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getConstraintMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConstraintMap();
            }
        }
        if ("getLayoutAlignmentX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getLayoutAlignmentX((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getLayoutAlignmentY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getLayoutAlignmentY((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getLayoutConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutConstraints();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getRowConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRowConstraints();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("invalidateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.invalidateLayout((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isManagingComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isManagingComponent((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("maximumLayoutSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.maximumLayoutSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("minimumLayoutSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.minimumLayoutSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("preferredLayoutSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.preferredLayoutSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LayoutCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.LayoutCallback.class}, false);
                typedTarget.removeLayoutCallback((com.codename1.ui.layouts.mig.LayoutCallback) adaptedArgs[0]); return null;
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setColumnConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setColumnConstraints((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false);
                typedTarget.setComponentConstraints((com.codename1.ui.Component) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setConstraintMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.setConstraintMap((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("setLayoutConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setLayoutConstraints((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setRowConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setRowConstraints((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.layouts.mig.PlatformDefaults typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.layouts.mig.UnitConverter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("convertToPixels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class}, false);
                return typedTarget.convertToPixels(((Number) adaptedArgs[0]).floatValue(), (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[4], (com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[5]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.layouts.mig.UnitValue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getConstraintString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConstraintString();
            }
        }
        if ("getOperation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOperation();
            }
        }
        if ("getPixels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class}, false);
                return typedTarget.getPixels(((Number) adaptedArgs[0]).floatValue(), (com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[1], (com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[2]);
            }
        }
        if ("getPixelsExact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.ui.layouts.mig.ContainerWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class}, false);
                return typedTarget.getPixelsExact(((Number) adaptedArgs[0]).floatValue(), (com.codename1.ui.layouts.mig.ContainerWrapper) adaptedArgs[1], (com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[2]);
            }
        }
        if ("getSubUnits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubUnits();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("getUnitString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnitString();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isHorizontal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHorizontal();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ui.layouts.mig.ComponentWrapper typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getComponentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getComponentType(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getContentBias".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentBias();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getHorizontalScreenDPI".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalScreenDPI();
            }
        }
        if ("getLayoutHashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHashCode();
            }
        }
        if ("getLinkId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkId();
            }
        }
        if ("getMaximumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMaximumHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMaximumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMaximumWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMinimumHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMinimumWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPixelUnitFactor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getPixelUnitFactor(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getPreferredHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getPreferredHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getPreferredWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getPreferredWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getScreenHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenHeight();
            }
        }
        if ("getScreenLocationX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenLocationX();
            }
        }
        if ("getScreenLocationY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenLocationY();
            }
        }
        if ("getScreenWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenWidth();
            }
        }
        if ("getVerticalScreenDPI".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalScreenDPI();
            }
        }
        if ("getVisualPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisualPadding();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("hasBaseline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasBaseline();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("paintDebugOutline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.paintDebugOutline(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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

    private static Object invoke12(com.codename1.ui.layouts.mig.ContainerWrapper typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getComponentType(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getComponents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponents();
            }
        }
        if ("getContentBias".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentBias();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getHorizontalScreenDPI".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalScreenDPI();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHashCode();
            }
        }
        if ("getLinkId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkId();
            }
        }
        if ("getMaximumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMaximumHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMaximumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMaximumWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMinimumHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMinimumWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPixelUnitFactor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getPixelUnitFactor(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getPreferredHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getPreferredHeight(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getPreferredWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getPreferredWidth(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getScreenHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenHeight();
            }
        }
        if ("getScreenLocationX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenLocationX();
            }
        }
        if ("getScreenLocationY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenLocationY();
            }
        }
        if ("getScreenWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScreenWidth();
            }
        }
        if ("getVerticalScreenDPI".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalScreenDPI();
            }
        }
        if ("getVisualPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisualPadding();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
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
        if ("hasBaseline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasBaseline();
            }
        }
        if ("isLeftToRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLeftToRight();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("paintDebugCell".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintDebugCell(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("paintDebugOutline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.paintDebugOutline(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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

    private static Object invoke13(com.codename1.ui.layouts.mig.InCellGapProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class, java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.mig.ComponentWrapper.class, com.codename1.ui.layouts.mig.ComponentWrapper.class, java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.getDefaultGap((com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[0], (com.codename1.ui.layouts.mig.ComponentWrapper) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], ((Boolean) adaptedArgs[4]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.layouts.mig.BoundSize.class) {
            if ("NULL_SIZE".equals(name)) return com.codename1.ui.layouts.mig.BoundSize.NULL_SIZE;
            if ("ZERO_PIXEL".equals(name)) return com.codename1.ui.layouts.mig.BoundSize.ZERO_PIXEL;
        }
        if (type == com.codename1.ui.layouts.mig.ComponentWrapper.class) {
            if ("TYPE_BUTTON".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_BUTTON;
            if ("TYPE_CHECK_BOX".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_CHECK_BOX;
            if ("TYPE_COMBO_BOX".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_COMBO_BOX;
            if ("TYPE_CONTAINER".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_CONTAINER;
            if ("TYPE_IMAGE".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_IMAGE;
            if ("TYPE_LABEL".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_LABEL;
            if ("TYPE_LIST".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_LIST;
            if ("TYPE_PANEL".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_PANEL;
            if ("TYPE_PROGRESS_BAR".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_PROGRESS_BAR;
            if ("TYPE_SCROLL_BAR".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_SCROLL_BAR;
            if ("TYPE_SCROLL_PANE".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_SCROLL_PANE;
            if ("TYPE_SEPARATOR".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_SEPARATOR;
            if ("TYPE_SLIDER".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_SLIDER;
            if ("TYPE_SPINNER".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_SPINNER;
            if ("TYPE_TABBED_PANE".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_TABBED_PANE;
            if ("TYPE_TABLE".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_TABLE;
            if ("TYPE_TEXT_AREA".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_TEXT_AREA;
            if ("TYPE_TEXT_FIELD".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_TEXT_FIELD;
            if ("TYPE_TREE".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_TREE;
            if ("TYPE_UNKNOWN".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_UNKNOWN;
            if ("TYPE_UNSET".equals(name)) return com.codename1.ui.layouts.mig.ComponentWrapper.TYPE_UNSET;
        }
        if (type == com.codename1.ui.layouts.mig.Grid.class) {
            if ("TEST_GAPS".equals(name)) return com.codename1.ui.layouts.mig.Grid.TEST_GAPS;
        }
        if (type == com.codename1.ui.layouts.mig.LayoutUtil.class) {
            if ("HAS_BEANS".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.HAS_BEANS;
            if ("HORIZONTAL".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.HORIZONTAL;
            if ("INF".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.INF;
            if ("MAX".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.MAX;
            if ("MIN".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.MIN;
            if ("PREF".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.PREF;
            if ("VERTICAL".equals(name)) return com.codename1.ui.layouts.mig.LayoutUtil.VERTICAL;
        }
        if (type == com.codename1.ui.layouts.mig.LinkHandler.class) {
            if ("HEIGHT".equals(name)) return com.codename1.ui.layouts.mig.LinkHandler.HEIGHT;
            if ("WIDTH".equals(name)) return com.codename1.ui.layouts.mig.LinkHandler.WIDTH;
            if ("X".equals(name)) return com.codename1.ui.layouts.mig.LinkHandler.X;
            if ("X2".equals(name)) return com.codename1.ui.layouts.mig.LinkHandler.X2;
            if ("Y".equals(name)) return com.codename1.ui.layouts.mig.LinkHandler.Y;
            if ("Y2".equals(name)) return com.codename1.ui.layouts.mig.LinkHandler.Y2;
        }
        if (type == com.codename1.ui.layouts.mig.PlatformDefaults.class) {
            if ("BASE_FONT_SIZE".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.BASE_FONT_SIZE;
            if ("BASE_REAL_PIXEL".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.BASE_REAL_PIXEL;
            if ("BASE_SCALE_FACTOR".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.BASE_SCALE_FACTOR;
            if ("GNOME".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.GNOME;
            if ("MAC_OSX".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.MAC_OSX;
            if ("VISUAL_PADDING_PROPERTY".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.VISUAL_PADDING_PROPERTY;
            if ("WINDOWS_XP".equals(name)) return com.codename1.ui.layouts.mig.PlatformDefaults.WINDOWS_XP;
        }
        if (type == com.codename1.ui.layouts.mig.UnitConverter.class) {
            if ("UNABLE".equals(name)) return com.codename1.ui.layouts.mig.UnitConverter.UNABLE;
        }
        if (type == com.codename1.ui.layouts.mig.UnitValue.class) {
            if ("ADD".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.ADD;
            if ("ALIGN".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.ALIGN;
            if ("BUTTON".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.BUTTON;
            if ("CM".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.CM;
            if ("DIV".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.DIV;
            if ("INCH".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.INCH;
            if ("LABEL_ALIGN".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LABEL_ALIGN;
            if ("LINK_H".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_H;
            if ("LINK_W".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_W;
            if ("LINK_X".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_X;
            if ("LINK_X2".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_X2;
            if ("LINK_XPOS".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_XPOS;
            if ("LINK_Y".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_Y;
            if ("LINK_Y2".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_Y2;
            if ("LINK_YPOS".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LINK_YPOS;
            if ("LOOKUP".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LOOKUP;
            if ("LPX".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LPX;
            if ("LPY".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.LPY;
            if ("MAX".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MAX;
            if ("MAX_SIZE".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MAX_SIZE;
            if ("MID".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MID;
            if ("MIN".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MIN;
            if ("MIN_SIZE".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MIN_SIZE;
            if ("MM".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MM;
            if ("MUL".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.MUL;
            if ("PERCENT".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.PERCENT;
            if ("PIXEL".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.PIXEL;
            if ("PREF_SIZE".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.PREF_SIZE;
            if ("PT".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.PT;
            if ("SPX".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.SPX;
            if ("SPY".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.SPY;
            if ("STATIC".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.STATIC;
            if ("SUB".equals(name)) return com.codename1.ui.layouts.mig.UnitValue.SUB;
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
