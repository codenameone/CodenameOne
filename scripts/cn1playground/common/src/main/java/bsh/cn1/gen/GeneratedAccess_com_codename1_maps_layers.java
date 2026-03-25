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
            return com.codename1.maps.layers.AbstractLayer.class;
        }
        if ("ArrowLinesLayer".equals(simpleName)) {
            return com.codename1.maps.layers.ArrowLinesLayer.class;
        }
        if ("Layer".equals(simpleName)) {
            return com.codename1.maps.layers.Layer.class;
        }
        if ("LinesLayer".equals(simpleName)) {
            return com.codename1.maps.layers.LinesLayer.class;
        }
        if ("PointLayer".equals(simpleName)) {
            return com.codename1.maps.layers.PointLayer.class;
        }
        if ("PointsLayer".equals(simpleName)) {
            return com.codename1.maps.layers.PointsLayer.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.layers.ArrowLinesLayer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.layers.ArrowLinesLayer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.maps.layers.ArrowLinesLayer((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false);
                return new com.codename1.maps.layers.ArrowLinesLayer((com.codename1.maps.Projection) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.maps.layers.LinesLayer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.layers.LinesLayer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.maps.layers.LinesLayer((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false);
                return new com.codename1.maps.layers.LinesLayer((com.codename1.maps.Projection) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.maps.layers.PointLayer.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.String.class, com.codename1.ui.Image.class}, false);
                return new com.codename1.maps.layers.PointLayer((com.codename1.maps.Coord) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.maps.layers.PointsLayer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.layers.PointsLayer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.maps.layers.PointsLayer((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Projection.class, java.lang.String.class}, false);
                return new com.codename1.maps.layers.PointsLayer((com.codename1.maps.Projection) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false);
                typedTarget.addLineSegment((com.codename1.maps.Coord[]) adaptedArgs[0]); return null;
            }
        }
        if ("boundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.boundingBox();
            }
        }
        if ("getArrowHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArrowHeight();
            }
        }
        if ("getArrowSegmentLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArrowSegmentLength();
            }
        }
        if ("getArrowWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArrowWidth();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProjection();
            }
        }
        if ("lineColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.lineColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.maps.Tile) adaptedArgs[1]); return null;
            }
        }
        if ("setArrowHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setArrowHeight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setArrowSegmentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setArrowSegmentLength(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setArrowWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setArrowWidth(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.maps.layers.LinesLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLineSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false);
                typedTarget.addLineSegment((com.codename1.maps.Coord[]) adaptedArgs[0]); return null;
            }
        }
        if ("boundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.boundingBox();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProjection();
            }
        }
        if ("lineColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.lineColor(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.maps.Tile) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.maps.layers.PointsLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.PointLayer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.layers.PointLayer.class}, false);
                typedTarget.addPoint((com.codename1.maps.layers.PointLayer) adaptedArgs[0]); return null;
            }
        }
        if ("boundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.boundingBox();
            }
        }
        if ("fireActionEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                typedTarget.fireActionEvent((com.codename1.maps.BoundingBox) adaptedArgs[0]); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProjection();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.maps.Tile) adaptedArgs[1]); return null;
            }
        }
        if ("removeActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeActionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.PointLayer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.layers.PointLayer.class}, false);
                typedTarget.removePoint((com.codename1.maps.layers.PointLayer) adaptedArgs[0]); return null;
            }
        }
        if ("setPointIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPointIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.maps.layers.AbstractLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("boundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.boundingBox();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getProjection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProjection();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.maps.Tile) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.maps.layers.PointLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("boundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.boundingBox();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getLatitude".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLatitude();
            }
        }
        if ("getLongitude".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLongitude();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isProjected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isProjected();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.maps.Tile) adaptedArgs[1]); return null;
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisplayName(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setLatitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setLatitude(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setLongitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setLongitude(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setProjected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setProjected(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.translate((com.codename1.maps.Coord) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.translate(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.maps.layers.Layer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("boundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.boundingBox();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.maps.Tile.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.maps.Tile) adaptedArgs[1]); return null;
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
