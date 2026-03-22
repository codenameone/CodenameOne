package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_maps_layers {
    private GeneratedAccess_com_codename1_maps_layers() {
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
        if ("AbstractLayer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.layers -> com.codename1.maps.layers.AbstractLayer");
            }
            return com.codename1.maps.layers.AbstractLayer.class;
        }
        if ("ArrowLinesLayer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.layers -> com.codename1.maps.layers.ArrowLinesLayer");
            }
            return com.codename1.maps.layers.ArrowLinesLayer.class;
        }
        if ("Layer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.layers -> com.codename1.maps.layers.Layer");
            }
            return com.codename1.maps.layers.Layer.class;
        }
        if ("LinesLayer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.layers -> com.codename1.maps.layers.LinesLayer");
            }
            return com.codename1.maps.layers.LinesLayer.class;
        }
        if ("PointLayer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.layers -> com.codename1.maps.layers.PointLayer");
            }
            return com.codename1.maps.layers.PointLayer.class;
        }
        if ("PointsLayer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.maps.layers -> com.codename1.maps.layers.PointsLayer");
            }
            return com.codename1.maps.layers.PointsLayer.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.layers.ArrowLinesLayer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.maps.layers.ArrowLinesLayer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.maps.layers.ArrowLinesLayer((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false)) {
                return new com.codename1.maps.layers.ArrowLinesLayer((com.codename1.maps.Projection) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.maps.layers.LinesLayer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.maps.layers.LinesLayer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.maps.layers.LinesLayer((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false)) {
                return new com.codename1.maps.layers.LinesLayer((com.codename1.maps.Projection) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.maps.layers.PointLayer.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                return new com.codename1.maps.layers.PointLayer((com.codename1.maps.Coord) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.ui.Image) safeArgs[2]);
            }
        }
        if (type == com.codename1.maps.layers.PointsLayer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.maps.layers.PointsLayer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.maps.layers.PointsLayer((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false)) {
                return new com.codename1.maps.layers.PointsLayer((com.codename1.maps.Projection) safeArgs[0], (java.lang.String) safeArgs[1]);
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
        if (target instanceof com.codename1.maps.layers.ArrowLinesLayer) {
            try {
                return invoke0((com.codename1.maps.layers.ArrowLinesLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.layers.LinesLayer) {
            try {
                return invoke1((com.codename1.maps.layers.LinesLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.layers.PointsLayer) {
            try {
                return invoke2((com.codename1.maps.layers.PointsLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.layers.AbstractLayer) {
            try {
                return invoke3((com.codename1.maps.layers.AbstractLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.layers.PointLayer) {
            try {
                return invoke4((com.codename1.maps.layers.PointLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.layers.Layer) {
            try {
                return invoke5((com.codename1.maps.layers.Layer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.maps.layers.ArrowLinesLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLineSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false)) {
                typedTarget.addLineSegment((com.codename1.maps.Coord[]) safeArgs[0]); return null;
            }
        }
        if ("boundingBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.boundingBox();
            }
        }
        if ("getArrowHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getArrowHeight();
            }
        }
        if ("getArrowSegmentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getArrowSegmentLength();
            }
        }
        if ("getArrowWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getArrowWidth();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProjection();
            }
        }
        if ("lineColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.lineColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.maps.Tile) safeArgs[1]); return null;
            }
        }
        if ("setArrowHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setArrowHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setArrowSegmentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setArrowSegmentLength(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setArrowWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setArrowWidth(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.maps.layers.LinesLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLineSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false)) {
                typedTarget.addLineSegment((com.codename1.maps.Coord[]) safeArgs[0]); return null;
            }
        }
        if ("boundingBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.boundingBox();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProjection();
            }
        }
        if ("lineColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.lineColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.maps.Tile) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.maps.layers.PointsLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.PointLayer.class}, false)) {
                typedTarget.addPoint((com.codename1.maps.layers.PointLayer) safeArgs[0]); return null;
            }
        }
        if ("boundingBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.boundingBox();
            }
        }
        if ("fireActionEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                typedTarget.fireActionEvent((com.codename1.maps.BoundingBox) safeArgs[0]); return null;
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProjection();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.maps.Tile) safeArgs[1]); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.PointLayer.class}, false)) {
                typedTarget.removePoint((com.codename1.maps.layers.PointLayer) safeArgs[0]); return null;
            }
        }
        if ("setPointIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPointIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.maps.layers.AbstractLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("boundingBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.boundingBox();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProjection();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.maps.Tile) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.maps.layers.PointLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("boundingBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.boundingBox();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIcon();
            }
        }
        if ("getLatitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLatitude();
            }
        }
        if ("getLongitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLongitude();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isProjected".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isProjected();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.maps.Tile) safeArgs[1]); return null;
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisplayName(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setLatitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setLatitude(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setLongitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setLongitude(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setProjected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setProjected(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                return typedTarget.translate((com.codename1.maps.Coord) safeArgs[0]);
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return typedTarget.translate(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.maps.layers.Layer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("boundingBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.boundingBox();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.maps.Tile) safeArgs[1]); return null;
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
