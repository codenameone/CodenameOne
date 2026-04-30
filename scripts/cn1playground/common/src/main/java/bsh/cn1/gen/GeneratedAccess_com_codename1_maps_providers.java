package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_maps_providers {
    private GeneratedAccess_com_codename1_maps_providers() {
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
        if ("GoogleMapsProvider".equals(simpleName)) {
            return com.codename1.maps.providers.GoogleMapsProvider.class;
        }
        if ("MapProvider".equals(simpleName)) {
            return com.codename1.maps.providers.MapProvider.class;
        }
        if ("OpenStreetMapProvider".equals(simpleName)) {
            return com.codename1.maps.providers.OpenStreetMapProvider.class;
        }
        if ("TiledProvider".equals(simpleName)) {
            return com.codename1.maps.providers.TiledProvider.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.providers.GoogleMapsProvider.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.maps.providers.GoogleMapsProvider((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.maps.providers.OpenStreetMapProvider.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.providers.OpenStreetMapProvider();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.providers.GoogleMapsProvider.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.maps.providers.GoogleMapsProvider.getTileSize();
            }
        }
        if ("setTileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.maps.providers.GoogleMapsProvider.setTileSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedStatic(com.codename1.maps.providers.GoogleMapsProvider.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.maps.providers.GoogleMapsProvider) {
            try {
                return invoke0((com.codename1.maps.providers.GoogleMapsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.providers.OpenStreetMapProvider) {
            try {
                return invoke1((com.codename1.maps.providers.OpenStreetMapProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.providers.TiledProvider) {
            try {
                return invoke2((com.codename1.maps.providers.TiledProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.providers.MapProvider) {
            try {
                return invoke3((com.codename1.maps.providers.MapProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.maps.providers.GoogleMapsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false);
                return typedTarget.bboxFor((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("getLanguage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguage();
            }
        }
        if ("isSensor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSensor();
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false);
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) adaptedArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.scale(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setLanguage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLanguage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setMapType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMapType(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setSensor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSensor(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.tileSize();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.translate((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.maps.providers.OpenStreetMapProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false);
                return typedTarget.bboxFor((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false);
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) adaptedArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.scale(toIntValue(adaptedArgs[0]));
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.tileSize();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.translate((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.maps.providers.TiledProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false);
                return typedTarget.bboxFor((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false);
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) adaptedArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.scale(toIntValue(adaptedArgs[0]));
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.tileSize();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.translate((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.maps.providers.MapProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false);
                return typedTarget.bboxFor((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false);
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) adaptedArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.scale(toIntValue(adaptedArgs[0]));
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.tileSize();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.translate((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.maps.providers.GoogleMapsProvider.class) return getStaticField0(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("HYBRID".equals(name)) return com.codename1.maps.providers.GoogleMapsProvider.HYBRID;
        if ("REGULAR".equals(name)) return com.codename1.maps.providers.GoogleMapsProvider.REGULAR;
        if ("SATELLITE".equals(name)) return com.codename1.maps.providers.GoogleMapsProvider.SATELLITE;
        throw unsupportedStaticField(com.codename1.maps.providers.GoogleMapsProvider.class, name);
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
