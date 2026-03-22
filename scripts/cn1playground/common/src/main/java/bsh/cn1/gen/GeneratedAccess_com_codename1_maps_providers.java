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
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.providers -> com.codename1.maps.providers.GoogleMapsProvider");
            }
            return com.codename1.maps.providers.GoogleMapsProvider.class;
        }
        if ("MapProvider".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.providers -> com.codename1.maps.providers.MapProvider");
            }
            return com.codename1.maps.providers.MapProvider.class;
        }
        if ("OpenStreetMapProvider".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.providers -> com.codename1.maps.providers.OpenStreetMapProvider");
            }
            return com.codename1.maps.providers.OpenStreetMapProvider.class;
        }
        if ("TiledProvider".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.providers -> com.codename1.maps.providers.TiledProvider");
            }
            return com.codename1.maps.providers.TiledProvider.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.providers.GoogleMapsProvider.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.maps.providers.GoogleMapsProvider((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.maps.providers.OpenStreetMapProvider.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.maps.providers.GoogleMapsProvider.getTileSize();
            }
        }
        if ("setTileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.maps.providers.GoogleMapsProvider.setTileSize(((Number) safeArgs[0]).intValue()); return null;
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                return typedTarget.bboxFor((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getLanguage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLanguage();
            }
        }
        if ("isSensor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSensor();
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) safeArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.scale(((Number) safeArgs[0]).intValue());
            }
        }
        if ("setLanguage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setLanguage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setMapType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setMapType(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setSensor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSensor(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) safeArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.tileSize();
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.translate((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.maps.providers.OpenStreetMapProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                return typedTarget.bboxFor((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) safeArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.scale(((Number) safeArgs[0]).intValue());
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) safeArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.tileSize();
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.translate((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.maps.providers.TiledProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                return typedTarget.bboxFor((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) safeArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.scale(((Number) safeArgs[0]).intValue());
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) safeArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.tileSize();
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.translate((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.maps.providers.MapProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.attribution();
            }
        }
        if ("bboxFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                return typedTarget.bboxFor((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("maxZoomFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Tile.class}, false)) {
                return typedTarget.maxZoomFor((com.codename1.maps.Tile) safeArgs[0]);
            }
        }
        if ("maxZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.maxZoomLevel();
            }
        }
        if ("minZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.minZoomLevel();
            }
        }
        if ("projection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.projection();
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.scale(((Number) safeArgs[0]).intValue());
            }
        }
        if ("tileFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                return typedTarget.tileFor((com.codename1.maps.BoundingBox) safeArgs[0]);
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.tileSize();
            }
        }
        if ("tileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.tileSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.translate((com.codename1.maps.Coord) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.maps.providers.GoogleMapsProvider.class) {
            if ("HYBRID".equals(name)) return com.codename1.maps.providers.GoogleMapsProvider.HYBRID;
            if ("REGULAR".equals(name)) return com.codename1.maps.providers.GoogleMapsProvider.REGULAR;
            if ("SATELLITE".equals(name)) return com.codename1.maps.providers.GoogleMapsProvider.SATELLITE;
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
