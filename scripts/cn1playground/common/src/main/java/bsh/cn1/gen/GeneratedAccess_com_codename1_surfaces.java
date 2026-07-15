package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_surfaces {
    private GeneratedAccess_com_codename1_surfaces() {
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
        if ("LiveActivity".equals(simpleName)) {
            return com.codename1.surfaces.LiveActivity.class;
        }
        if ("LiveActivityDescriptor".equals(simpleName)) {
            return com.codename1.surfaces.LiveActivityDescriptor.class;
        }
        if ("SurfaceActionEvent".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceActionEvent.class;
        }
        if ("SurfaceActionHandler".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceActionHandler.class;
        }
        if ("SurfaceAlignment".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceAlignment.class;
        }
        if ("SurfaceBox".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceBox.class;
        }
        if ("SurfaceColor".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceColor.class;
        }
        if ("SurfaceColumn".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceColumn.class;
        }
        if ("SurfaceContainer".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceContainer.class;
        }
        if ("SurfaceDynamicText".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceDynamicText.class;
        }
        if ("SurfaceFontWeight".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceFontWeight.class;
        }
        if ("SurfaceImage".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceImage.class;
        }
        if ("SurfaceNode".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceNode.class;
        }
        if ("SurfaceProgress".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceProgress.class;
        }
        if ("SurfaceRasterizer".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceRasterizer.class;
        }
        if ("ActionRect".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceRasterizer.ActionRect.class;
        }
        if ("Result".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceRasterizer.Result.class;
        }
        if ("SurfaceRow".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceRow.class;
        }
        if ("SurfaceSerializer".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceSerializer.class;
        }
        if ("SurfaceSpacer".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceSpacer.class;
        }
        if ("SurfaceText".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceText.class;
        }
        if ("SurfaceVector".equals(simpleName)) {
            return com.codename1.surfaces.SurfaceVector.class;
        }
        if ("Surfaces".equals(simpleName)) {
            return com.codename1.surfaces.Surfaces.class;
        }
        if ("WidgetKind".equals(simpleName)) {
            return com.codename1.surfaces.WidgetKind.class;
        }
        if ("WidgetSize".equals(simpleName)) {
            return com.codename1.surfaces.WidgetSize.class;
        }
        if ("WidgetTimeline".equals(simpleName)) {
            return com.codename1.surfaces.WidgetTimeline.class;
        }
        if ("Entry".equals(simpleName)) {
            return com.codename1.surfaces.WidgetTimeline.Entry.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.surfaces.LiveActivityDescriptor.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.surfaces.LiveActivityDescriptor((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.surfaces.SurfaceActionEvent.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false);
                return new com.codename1.surfaces.SurfaceActionEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.Map) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.surfaces.SurfaceBox.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.surfaces.SurfaceBox();
            }
        }
        if (type == com.codename1.surfaces.SurfaceColumn.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.surfaces.SurfaceColumn();
            }
        }
        if (type == com.codename1.surfaces.SurfaceDynamicText.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.surfaces.SurfaceDynamicText(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Date.class}, false);
                return new com.codename1.surfaces.SurfaceDynamicText(toIntValue(adaptedArgs[0]), (java.util.Date) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.surfaces.SurfaceImage.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return new com.codename1.surfaces.SurfaceImage((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.surfaces.SurfaceImage((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.surfaces.SurfaceProgress.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.surfaces.SurfaceProgress(toIntValue(adaptedArgs[0]));
            }
        }
        if (type == com.codename1.surfaces.SurfaceRow.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.surfaces.SurfaceRow();
            }
        }
        if (type == com.codename1.surfaces.SurfaceSpacer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.surfaces.SurfaceSpacer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.surfaces.SurfaceSpacer(toIntValue(adaptedArgs[0]));
            }
        }
        if (type == com.codename1.surfaces.SurfaceText.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.surfaces.SurfaceText((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.surfaces.SurfaceVector.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.surfaces.SurfaceVector(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.surfaces.WidgetKind.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.surfaces.WidgetKind((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.surfaces.WidgetTimeline.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.surfaces.WidgetTimeline();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.surfaces.LiveActivity.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.surfaces.SurfaceColor.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.surfaces.SurfaceRasterizer.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.surfaces.SurfaceSerializer.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.surfaces.Surfaces.class) return invokeStatic4(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.surfaces.LiveActivity.isSupported();
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.LiveActivityDescriptor.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.LiveActivityDescriptor.class, java.util.Map.class}, false);
                return com.codename1.surfaces.LiveActivity.start((com.codename1.surfaces.LiveActivityDescriptor) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.surfaces.LiveActivity.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("rgb".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.surfaces.SurfaceColor.rgb(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.surfaces.SurfaceColor.rgb(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedStatic(com.codename1.surfaces.SurfaceColor.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("currentEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Long.class}, false);
                return com.codename1.surfaces.SurfaceRasterizer.currentEntry((java.util.Map) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("layoutForSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.String.class}, false);
                return com.codename1.surfaces.SurfaceRasterizer.layoutForSize((java.util.Map) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("nextEntryFlip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Long.class}, false);
                return com.codename1.surfaces.SurfaceRasterizer.nextEntryFlip((java.util.Map) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("rasterize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.util.Map.class, java.util.Map.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.util.Map.class, java.util.Map.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Long.class}, false);
                return com.codename1.surfaces.SurfaceRasterizer.rasterize((java.util.Map) adaptedArgs[0], (java.util.Map) adaptedArgs[1], (java.util.Map) adaptedArgs[2], toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), ((Boolean) adaptedArgs[5]).booleanValue(), ((Number) adaptedArgs[6]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.surfaces.SurfaceRasterizer.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("serializeKind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetKind.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetKind.class}, false);
                return com.codename1.surfaces.SurfaceSerializer.serializeKind((com.codename1.surfaces.WidgetKind) adaptedArgs[0]);
            }
        }
        if ("serializeLiveActivity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.LiveActivityDescriptor.class, java.util.Map.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.LiveActivityDescriptor.class, java.util.Map.class, java.util.Map.class}, false);
                return com.codename1.surfaces.SurfaceSerializer.serializeLiveActivity((com.codename1.surfaces.LiveActivityDescriptor) adaptedArgs[0], (java.util.Map) adaptedArgs[1], (java.util.Map) adaptedArgs[2]);
            }
        }
        if ("serializeState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.surfaces.SurfaceSerializer.serializeState((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("serializeTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.surfaces.WidgetTimeline.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.surfaces.WidgetTimeline.class, java.util.Map.class}, false);
                return com.codename1.surfaces.SurfaceSerializer.serializeTimeline((java.lang.String) adaptedArgs[0], (com.codename1.surfaces.WidgetTimeline) adaptedArgs[1], (java.util.Map) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.surfaces.SurfaceSerializer.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("areWidgetsSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.surfaces.Surfaces.areWidgetsSupported();
            }
        }
        if ("dispatchAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false);
                com.codename1.surfaces.Surfaces.dispatchAction((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.Map) adaptedArgs[2]); return null;
            }
        }
        if ("getInstalledWidgetCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.surfaces.Surfaces.getInstalledWidgetCount((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getRegisteredKinds".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.surfaces.Surfaces.getRegisteredKinds();
            }
        }
        if ("publish".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.surfaces.WidgetTimeline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.surfaces.WidgetTimeline.class}, false);
                com.codename1.surfaces.Surfaces.publish((java.lang.String) adaptedArgs[0], (com.codename1.surfaces.WidgetTimeline) adaptedArgs[1]); return null;
            }
        }
        if ("registerWidgetKind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetKind.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetKind.class}, false);
                com.codename1.surfaces.Surfaces.registerWidgetKind((com.codename1.surfaces.WidgetKind) adaptedArgs[0]); return null;
            }
        }
        if ("reloadWidgets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.surfaces.Surfaces.reloadWidgets((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setActionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceActionHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceActionHandler.class}, false);
                com.codename1.surfaces.Surfaces.setActionHandler((com.codename1.surfaces.SurfaceActionHandler) adaptedArgs[0]); return null;
            }
        }
        if ("setBridge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.spi.SurfaceBridge.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.spi.SurfaceBridge.class}, false);
                com.codename1.surfaces.Surfaces.setBridge((com.codename1.surfaces.spi.SurfaceBridge) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.surfaces.Surfaces.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.surfaces.SurfaceBox) {
            try {
                return invoke0((com.codename1.surfaces.SurfaceBox) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceColumn) {
            try {
                return invoke1((com.codename1.surfaces.SurfaceColumn) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceRow) {
            try {
                return invoke2((com.codename1.surfaces.SurfaceRow) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceContainer) {
            try {
                return invoke3((com.codename1.surfaces.SurfaceContainer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceDynamicText) {
            try {
                return invoke4((com.codename1.surfaces.SurfaceDynamicText) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceImage) {
            try {
                return invoke5((com.codename1.surfaces.SurfaceImage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceProgress) {
            try {
                return invoke6((com.codename1.surfaces.SurfaceProgress) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceSpacer) {
            try {
                return invoke7((com.codename1.surfaces.SurfaceSpacer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceText) {
            try {
                return invoke8((com.codename1.surfaces.SurfaceText) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceVector) {
            try {
                return invoke9((com.codename1.surfaces.SurfaceVector) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.LiveActivity) {
            try {
                return invoke10((com.codename1.surfaces.LiveActivity) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.LiveActivityDescriptor) {
            try {
                return invoke11((com.codename1.surfaces.LiveActivityDescriptor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceActionEvent) {
            try {
                return invoke12((com.codename1.surfaces.SurfaceActionEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceAlignment) {
            try {
                return invoke13((com.codename1.surfaces.SurfaceAlignment) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceColor) {
            try {
                return invoke14((com.codename1.surfaces.SurfaceColor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceFontWeight) {
            try {
                return invoke15((com.codename1.surfaces.SurfaceFontWeight) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceNode) {
            try {
                return invoke16((com.codename1.surfaces.SurfaceNode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceRasterizer.ActionRect) {
            try {
                return invoke17((com.codename1.surfaces.SurfaceRasterizer.ActionRect) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceRasterizer.Result) {
            try {
                return invoke18((com.codename1.surfaces.SurfaceRasterizer.Result) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.WidgetKind) {
            try {
                return invoke19((com.codename1.surfaces.WidgetKind) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.WidgetSize) {
            try {
                return invoke20((com.codename1.surfaces.WidgetSize) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.WidgetTimeline) {
            try {
                return invoke21((com.codename1.surfaces.WidgetTimeline) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.WidgetTimeline.Entry) {
            try {
                return invoke22((com.codename1.surfaces.WidgetTimeline.Entry) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.surfaces.SurfaceActionHandler) {
            try {
                return invoke23((com.codename1.surfaces.SurfaceActionHandler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.surfaces.SurfaceBox typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.add((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getChildren".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildren();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.surfaces.SurfaceColumn typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.add((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getChildren".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildren();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpacing();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setSpacing(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.surfaces.SurfaceRow typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.add((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getChildren".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildren();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getSpacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpacing();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setSpacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setSpacing(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.surfaces.SurfaceContainer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.add((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getChildren".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildren();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.surfaces.SurfaceDynamicText typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDate();
            }
        }
        if ("getDateKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDateKey();
            }
        }
        if ("getFontSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontSize();
            }
        }
        if ("getFontWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontWeight();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setColor((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setFontSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setFontSize(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setFontWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceFontWeight.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceFontWeight.class}, false);
                return typedTarget.setFontWeight((com.codename1.surfaces.SurfaceFontWeight) adaptedArgs[0]);
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.surfaces.SurfaceImage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImage();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getRegisteredName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRegisteredName();
            }
        }
        if ("getScaleMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleMode();
            }
        }
        if ("getTint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTint();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setScaleMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setScaleMode(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setTint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setTint((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.surfaces.SurfaceProgress typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getIntervalEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIntervalEnd();
            }
        }
        if ("getIntervalStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIntervalStart();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getValueKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValueKey();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setColor((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setDateInterval".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.setDateInterval((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setValue(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("setValueState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setValueState((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.surfaces.SurfaceSpacer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getMinDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.surfaces.SurfaceText typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getFontSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontSize();
            }
        }
        if ("getFontWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontWeight();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getMaxLines".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxLines();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setColor((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setFontSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setFontSize(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setFontWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceFontWeight.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceFontWeight.class}, false);
                return typedTarget.setFontWeight((com.codename1.surfaces.SurfaceFontWeight) adaptedArgs[0]);
            }
        }
        if ("setMaxLines".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setMaxLines(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.surfaces.SurfaceVector typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("beginRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.beginRotation(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.beginRotation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("endRotation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.endRotation();
            }
        }
        if ("fillArc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.fillArc(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[6]);
            }
        }
        if ("fillEllipse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.fillEllipse(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[4]);
            }
        }
        if ("fillPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Boolean.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Boolean.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.fillPath((float[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[2]);
            }
        }
        if ("fillRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.fillRect(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[4]);
            }
        }
        if ("fillRoundRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.fillRoundRect(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[5]);
            }
        }
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getOpCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpCount();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getViewBoxHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getViewBoxHeight();
            }
        }
        if ("getViewBoxWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getViewBoxWidth();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("line".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.line(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[5]);
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        if ("strokeArc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.strokeArc(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[7]);
            }
        }
        if ("strokeEllipse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.strokeEllipse(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[5]);
            }
        }
        if ("strokePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Boolean.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Boolean.class, java.lang.Float.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.strokePath((float[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Number) adaptedArgs[2]).floatValue(), (com.codename1.surfaces.SurfaceColor) adaptedArgs[3]);
            }
        }
        if ("text".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceFontWeight.class, com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, com.codename1.surfaces.SurfaceFontWeight.class, com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.text((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (com.codename1.surfaces.SurfaceFontWeight) adaptedArgs[4], (com.codename1.surfaces.SurfaceColor) adaptedArgs[5]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.surfaces.LiveActivity typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("end".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.end((java.util.Map) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Boolean.class}, false);
                typedTarget.end((java.util.Map) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.update((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.surfaces.LiveActivityDescriptor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActivityType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActivityType();
            }
        }
        if ("getAndroidChannelId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAndroidChannelId();
            }
        }
        if ("getCompactLeading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompactLeading();
            }
        }
        if ("getCompactTrailing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompactTrailing();
            }
        }
        if ("getContent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContent();
            }
        }
        if ("getExpandedBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpandedBottom();
            }
        }
        if ("getExpandedCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpandedCenter();
            }
        }
        if ("getExpandedLeading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpandedLeading();
            }
        }
        if ("getExpandedTrailing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpandedTrailing();
            }
        }
        if ("getMinimal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimal();
            }
        }
        if ("getTint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTint();
            }
        }
        if ("setAndroidChannelId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAndroidChannelId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setCompactLeading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setCompactLeading((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setCompactTrailing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setCompactTrailing((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setContent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setContent((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setExpandedBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setExpandedBottom((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setExpandedCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setExpandedCenter((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setExpandedLeading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setExpandedLeading((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setExpandedTrailing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setExpandedTrailing((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setMinimal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setMinimal((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        if ("setTint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setTint((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.surfaces.SurfaceActionEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParams();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("isColdStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isColdStart();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.surfaces.SurfaceAlignment typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getJsonName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJsonName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.surfaces.SurfaceColor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDark".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDark();
            }
        }
        if ("getLight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLight();
            }
        }
        if ("getRole".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRole();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.surfaces.SurfaceFontWeight typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getJsonName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJsonName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.surfaces.SurfaceNode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getActionParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionParams();
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackground();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getHeightDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightDips();
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeft();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRight();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getWeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeight();
            }
        }
        if ("getWidthDips".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthDips();
            }
        }
        if ("setAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return typedTarget.setAction((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceAlignment.class}, false);
                return typedTarget.setAlignment((com.codename1.surfaces.SurfaceAlignment) adaptedArgs[0]);
            }
        }
        if ("setBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceColor.class}, false);
                return typedTarget.setBackground((com.codename1.surfaces.SurfaceColor) adaptedArgs[0]);
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCornerRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setWeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setWeight(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.surfaces.SurfaceRasterizer.ActionRect typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActionId();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getParams".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParams();
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.surfaces.SurfaceRasterizer.Result typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getArgb".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArgb();
            }
        }
        if ("getImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImage();
            }
        }
        if ("getNextTickMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextTickMillis();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.surfaces.WidgetKind typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSupportedSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetSize.class}, false);
                return typedTarget.addSupportedSize((com.codename1.surfaces.WidgetSize) adaptedArgs[0]);
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getSupportedSizes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSupportedSizes();
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDescription((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDisplayName((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.surfaces.WidgetSize typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getJsonName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJsonName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.surfaces.WidgetTimeline typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Map.class}, false);
                return typedTarget.addEntry((java.util.Date) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("getContent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetSize.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetSize.class}, false);
                return typedTarget.getContent((com.codename1.surfaces.WidgetSize) adaptedArgs[0]);
            }
        }
        if ("getDefaultContent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultContent();
            }
        }
        if ("getEntries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEntries();
            }
        }
        if ("getReloadPolicy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReloadPolicy();
            }
        }
        if ("setContent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setContent((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetSize.class, com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.WidgetSize.class, com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.setContent((com.codename1.surfaces.WidgetSize) adaptedArgs[0], (com.codename1.surfaces.SurfaceNode) adaptedArgs[1]);
            }
        }
        if ("setReloadPolicy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setReloadPolicy(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.surfaces.WidgetTimeline.Entry typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDate();
            }
        }
        if ("getState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getState();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.surfaces.SurfaceActionHandler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onSurfaceAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceActionEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceActionEvent.class}, false);
                typedTarget.onSurfaceAction((com.codename1.surfaces.SurfaceActionEvent) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.surfaces.SurfaceAlignment.class) return getStaticField0(name);
        if (type == com.codename1.surfaces.SurfaceColor.class) return getStaticField1(name);
        if (type == com.codename1.surfaces.SurfaceDynamicText.class) return getStaticField2(name);
        if (type == com.codename1.surfaces.SurfaceFontWeight.class) return getStaticField3(name);
        if (type == com.codename1.surfaces.SurfaceImage.class) return getStaticField4(name);
        if (type == com.codename1.surfaces.SurfaceProgress.class) return getStaticField5(name);
        if (type == com.codename1.surfaces.WidgetSize.class) return getStaticField6(name);
        if (type == com.codename1.surfaces.WidgetTimeline.class) return getStaticField7(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BOTTOM".equals(name)) return com.codename1.surfaces.SurfaceAlignment.BOTTOM;
        if ("BOTTOM_LEADING".equals(name)) return com.codename1.surfaces.SurfaceAlignment.BOTTOM_LEADING;
        if ("BOTTOM_TRAILING".equals(name)) return com.codename1.surfaces.SurfaceAlignment.BOTTOM_TRAILING;
        if ("CENTER".equals(name)) return com.codename1.surfaces.SurfaceAlignment.CENTER;
        if ("LEADING".equals(name)) return com.codename1.surfaces.SurfaceAlignment.LEADING;
        if ("TOP".equals(name)) return com.codename1.surfaces.SurfaceAlignment.TOP;
        if ("TOP_LEADING".equals(name)) return com.codename1.surfaces.SurfaceAlignment.TOP_LEADING;
        if ("TOP_TRAILING".equals(name)) return com.codename1.surfaces.SurfaceAlignment.TOP_TRAILING;
        if ("TRAILING".equals(name)) return com.codename1.surfaces.SurfaceAlignment.TRAILING;
        throw unsupportedStaticField(com.codename1.surfaces.SurfaceAlignment.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ACCENT".equals(name)) return com.codename1.surfaces.SurfaceColor.ACCENT;
        if ("BACKGROUND".equals(name)) return com.codename1.surfaces.SurfaceColor.BACKGROUND;
        if ("LABEL".equals(name)) return com.codename1.surfaces.SurfaceColor.LABEL;
        if ("SECONDARY_LABEL".equals(name)) return com.codename1.surfaces.SurfaceColor.SECONDARY_LABEL;
        throw unsupportedStaticField(com.codename1.surfaces.SurfaceColor.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("STYLE_DATE".equals(name)) return com.codename1.surfaces.SurfaceDynamicText.STYLE_DATE;
        if ("STYLE_RELATIVE".equals(name)) return com.codename1.surfaces.SurfaceDynamicText.STYLE_RELATIVE;
        if ("STYLE_TIME".equals(name)) return com.codename1.surfaces.SurfaceDynamicText.STYLE_TIME;
        if ("STYLE_TIMER_DOWN".equals(name)) return com.codename1.surfaces.SurfaceDynamicText.STYLE_TIMER_DOWN;
        if ("STYLE_TIMER_UP".equals(name)) return com.codename1.surfaces.SurfaceDynamicText.STYLE_TIMER_UP;
        throw unsupportedStaticField(com.codename1.surfaces.SurfaceDynamicText.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("BOLD".equals(name)) return com.codename1.surfaces.SurfaceFontWeight.BOLD;
        if ("LIGHT".equals(name)) return com.codename1.surfaces.SurfaceFontWeight.LIGHT;
        if ("MEDIUM".equals(name)) return com.codename1.surfaces.SurfaceFontWeight.MEDIUM;
        if ("REGULAR".equals(name)) return com.codename1.surfaces.SurfaceFontWeight.REGULAR;
        if ("SEMIBOLD".equals(name)) return com.codename1.surfaces.SurfaceFontWeight.SEMIBOLD;
        throw unsupportedStaticField(com.codename1.surfaces.SurfaceFontWeight.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("SCALE_CENTER".equals(name)) return com.codename1.surfaces.SurfaceImage.SCALE_CENTER;
        if ("SCALE_FILL".equals(name)) return com.codename1.surfaces.SurfaceImage.SCALE_FILL;
        if ("SCALE_FIT".equals(name)) return com.codename1.surfaces.SurfaceImage.SCALE_FIT;
        throw unsupportedStaticField(com.codename1.surfaces.SurfaceImage.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("STYLE_CIRCULAR".equals(name)) return com.codename1.surfaces.SurfaceProgress.STYLE_CIRCULAR;
        if ("STYLE_LINEAR".equals(name)) return com.codename1.surfaces.SurfaceProgress.STYLE_LINEAR;
        throw unsupportedStaticField(com.codename1.surfaces.SurfaceProgress.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("LARGE".equals(name)) return com.codename1.surfaces.WidgetSize.LARGE;
        if ("LOCKSCREEN".equals(name)) return com.codename1.surfaces.WidgetSize.LOCKSCREEN;
        if ("MEDIUM".equals(name)) return com.codename1.surfaces.WidgetSize.MEDIUM;
        if ("SMALL".equals(name)) return com.codename1.surfaces.WidgetSize.SMALL;
        throw unsupportedStaticField(com.codename1.surfaces.WidgetSize.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("RELOAD_AT_END".equals(name)) return com.codename1.surfaces.WidgetTimeline.RELOAD_AT_END;
        if ("RELOAD_NEVER".equals(name)) return com.codename1.surfaces.WidgetTimeline.RELOAD_NEVER;
        throw unsupportedStaticField(com.codename1.surfaces.WidgetTimeline.class, name);
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
