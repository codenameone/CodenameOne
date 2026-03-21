package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_geom {
    private GeneratedAccess_com_codename1_ui_geom() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.ui.geom.AffineTransform".equals(name)) return com.codename1.ui.geom.AffineTransform.class;
        if ("com.codename1.ui.geom.Dimension".equals(name)) return com.codename1.ui.geom.Dimension.class;
        if ("com.codename1.ui.geom.Dimension2D".equals(name)) return com.codename1.ui.geom.Dimension2D.class;
        if ("com.codename1.ui.geom.GeneralPath".equals(name)) return com.codename1.ui.geom.GeneralPath.class;
        if ("com.codename1.ui.geom.PathIterator".equals(name)) return com.codename1.ui.geom.PathIterator.class;
        if ("com.codename1.ui.geom.Point".equals(name)) return com.codename1.ui.geom.Point.class;
        if ("com.codename1.ui.geom.Point2D".equals(name)) return com.codename1.ui.geom.Point2D.class;
        if ("com.codename1.ui.geom.Rectangle".equals(name)) return com.codename1.ui.geom.Rectangle.class;
        if ("com.codename1.ui.geom.Rectangle2D".equals(name)) return com.codename1.ui.geom.Rectangle2D.class;
        if ("com.codename1.ui.geom.Shape".equals(name)) return com.codename1.ui.geom.Shape.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.geom.AffineTransform.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.geom.AffineTransform();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.AffineTransform.class}, false)) {
                return new com.codename1.ui.geom.AffineTransform((com.codename1.ui.geom.AffineTransform) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return new com.codename1.ui.geom.AffineTransform((double[]) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                return new com.codename1.ui.geom.AffineTransform((float[]) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return new com.codename1.ui.geom.AffineTransform(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                return new com.codename1.ui.geom.AffineTransform(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).floatValue());
            }
        }
        if (type == com.codename1.ui.geom.Dimension.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.geom.Dimension();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                return new com.codename1.ui.geom.Dimension((com.codename1.ui.geom.Dimension) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.geom.Dimension(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if (type == com.codename1.ui.geom.Dimension2D.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.geom.Dimension2D();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension2D.class}, false)) {
                return new com.codename1.ui.geom.Dimension2D((com.codename1.ui.geom.Dimension2D) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return new com.codename1.ui.geom.Dimension2D(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if (type == com.codename1.ui.geom.GeneralPath.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.geom.GeneralPath();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class}, false)) {
                return new com.codename1.ui.geom.GeneralPath((com.codename1.ui.geom.Shape) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.ui.geom.GeneralPath(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.geom.GeneralPath(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if (type == com.codename1.ui.geom.Point.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.geom.Point(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if (type == com.codename1.ui.geom.Point2D.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return new com.codename1.ui.geom.Point2D(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if (type == com.codename1.ui.geom.Rectangle.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.geom.Rectangle();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return new com.codename1.ui.geom.Rectangle((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.geom.Dimension.class}, false)) {
                return new com.codename1.ui.geom.Rectangle(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (com.codename1.ui.geom.Dimension) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.ui.geom.Rectangle(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if (type == com.codename1.ui.geom.Rectangle2D.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.geom.Rectangle2D();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class}, false)) {
                return new com.codename1.ui.geom.Rectangle2D((com.codename1.ui.geom.Rectangle2D) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, com.codename1.ui.geom.Dimension2D.class}, false)) {
                return new com.codename1.ui.geom.Rectangle2D(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), (com.codename1.ui.geom.Dimension2D) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return new com.codename1.ui.geom.Rectangle2D(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.geom.AffineTransform.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.geom.GeneralPath.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.geom.Rectangle.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ui.geom.Rectangle2D.class) return invokeStatic3(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getRotateInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.ui.geom.AffineTransform.getRotateInstance(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("getRotateInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.ui.geom.AffineTransform.getRotateInstance(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.geom.AffineTransform.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("createFromPool".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.geom.GeneralPath.createFromPool();
            }
        }
        if ("isConvexPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, float[].class}, false)) {
                return com.codename1.ui.geom.GeneralPath.isConvexPolygon((float[]) safeArgs[0], (float[]) safeArgs[1]);
            }
        }
        if ("isConvexPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                return com.codename1.ui.geom.GeneralPath.isConvexPolygon((int[]) safeArgs[0], (int[]) safeArgs[1]);
            }
        }
        if ("recycle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.GeneralPath.class}, false)) {
                com.codename1.ui.geom.GeneralPath.recycle((com.codename1.ui.geom.GeneralPath) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.geom.GeneralPath.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.geom.Rectangle.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue(), ((Number) safeArgs[7]).intValue());
            }
        }
        if ("createFromPool".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.geom.Rectangle.createFromPool(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                com.codename1.ui.geom.Rectangle.intersection(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue(), ((Number) safeArgs[7]).intValue(), (com.codename1.ui.geom.Rectangle) safeArgs[8]); return null;
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.geom.Rectangle.intersects(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).intValue(), ((Number) safeArgs[6]).intValue(), ((Number) safeArgs[7]).intValue());
            }
        }
        if ("recycle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                com.codename1.ui.geom.Rectangle.recycle((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.geom.Rectangle.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.ui.geom.Rectangle2D.contains(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue(), ((Number) safeArgs[6]).doubleValue(), ((Number) safeArgs[7]).doubleValue());
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, com.codename1.ui.geom.Rectangle2D.class}, false)) {
                com.codename1.ui.geom.Rectangle2D.intersection(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue(), ((Number) safeArgs[6]).doubleValue(), ((Number) safeArgs[7]).doubleValue(), (com.codename1.ui.geom.Rectangle2D) safeArgs[8]); return null;
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.ui.geom.Rectangle2D.intersects(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue(), ((Number) safeArgs[6]).doubleValue(), ((Number) safeArgs[7]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.geom.Rectangle2D.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.geom.AffineTransform) {
            try {
                return invoke0((com.codename1.ui.geom.AffineTransform) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Dimension) {
            try {
                return invoke1((com.codename1.ui.geom.Dimension) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Dimension2D) {
            try {
                return invoke2((com.codename1.ui.geom.Dimension2D) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.GeneralPath) {
            try {
                return invoke3((com.codename1.ui.geom.GeneralPath) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Point) {
            try {
                return invoke4((com.codename1.ui.geom.Point) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Point2D) {
            try {
                return invoke5((com.codename1.ui.geom.Point2D) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Rectangle) {
            try {
                return invoke6((com.codename1.ui.geom.Rectangle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Rectangle2D) {
            try {
                return invoke7((com.codename1.ui.geom.Rectangle2D) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.PathIterator) {
            try {
                return invoke8((com.codename1.ui.geom.PathIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.geom.Shape) {
            try {
                return invoke9((com.codename1.ui.geom.Shape) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.geom.AffineTransform typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("setToIdentity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.setToIdentity(); return null;
            }
        }
        if ("setToRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setToRotation(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setToRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setToRotation(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("setToRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setToRotation(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue()); return null;
            }
        }
        if ("setToRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setToRotation(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("setToScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setToScale(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("setToShear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setToShear(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("setToTranslation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setToTranslation(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("setTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setTransform(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("toTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toTransform();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.geom.Dimension typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHeight();
            }
        }
        if ("getWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWidth();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setWidth(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.geom.Dimension2D typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHeight();
            }
        }
        if ("getWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWidth();
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setHeight(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setWidth(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.geom.GeneralPath typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.PathIterator.class, java.lang.Boolean.class}, false)) {
                typedTarget.append((com.codename1.ui.geom.PathIterator) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, java.lang.Boolean.class}, false)) {
                typedTarget.append((com.codename1.ui.geom.Shape) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("arc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.arc(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue()); return null;
            }
        }
        if ("arc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.arc(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).floatValue()); return null;
            }
        }
        if ("arc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                typedTarget.arc(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue(), ((Boolean) safeArgs[6]).booleanValue()); return null;
            }
        }
        if ("arc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.arc(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).floatValue(), ((Boolean) safeArgs[6]).booleanValue()); return null;
            }
        }
        if ("arcTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.arcTo(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("arcTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.arcTo(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("arcTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                typedTarget.arcTo(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Boolean) safeArgs[4]).booleanValue()); return null;
            }
        }
        if ("arcTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.arcTo(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Boolean) safeArgs[4]).booleanValue()); return null;
            }
        }
        if ("closePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.closePath(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createTransformedShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class}, false)) {
                return typedTarget.createTransformedShape((com.codename1.ui.Transform) safeArgs[0]);
            }
        }
        if ("curveTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.curveTo(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue(), ((Number) safeArgs[5]).doubleValue()); return null;
            }
        }
        if ("curveTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.curveTo(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue(), ((Number) safeArgs[4]).floatValue(), ((Number) safeArgs[5]).floatValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, com.codename1.ui.Transform.class}, false)) {
                return typedTarget.equals((com.codename1.ui.geom.Shape) safeArgs[0], (com.codename1.ui.Transform) safeArgs[1]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds();
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.getBounds((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("getBounds2D".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds2D();
            }
        }
        if ("getBounds2D".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                typedTarget.getBounds2D((float[]) safeArgs[0]); return null;
            }
        }
        if ("getCurrentPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCurrentPoint();
            }
        }
        if ("getCurrentPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                typedTarget.getCurrentPoint((float[]) safeArgs[0]); return null;
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPathIterator();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class}, false)) {
                return typedTarget.getPathIterator((com.codename1.ui.Transform) safeArgs[0]);
            }
        }
        if ("getPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                typedTarget.getPoints((float[]) safeArgs[0]); return null;
            }
        }
        if ("getPointsSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPointsSize();
            }
        }
        if ("getTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.getTypes((byte[]) safeArgs[0]); return null;
            }
        }
        if ("getTypesSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTypesSize();
            }
        }
        if ("getWindingRule".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWindingRule();
            }
        }
        if ("intersect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.intersect((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("intersect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class}, false)) {
                typedTarget.intersect((com.codename1.ui.geom.Shape) safeArgs[0]); return null;
            }
        }
        if ("intersect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.intersect(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.intersection((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("isPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPolygon();
            }
        }
        if ("isRectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangle();
            }
        }
        if ("lineTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.lineTo(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("lineTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.lineTo(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue()); return null;
            }
        }
        if ("moveTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.moveTo(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("moveTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.moveTo(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue()); return null;
            }
        }
        if ("quadTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.quadTo(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("quadTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.quadTo(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("setPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.GeneralPath.class, com.codename1.ui.Transform.class}, false)) {
                typedTarget.setPath((com.codename1.ui.geom.GeneralPath) safeArgs[0], (com.codename1.ui.Transform) safeArgs[1]); return null;
            }
        }
        if ("setRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class, com.codename1.ui.Transform.class}, false)) {
                typedTarget.setRect((com.codename1.ui.geom.Rectangle) safeArgs[0], (com.codename1.ui.Transform) safeArgs[1]); return null;
            }
        }
        if ("setShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, com.codename1.ui.Transform.class}, false)) {
                typedTarget.setShape((com.codename1.ui.geom.Shape) safeArgs[0], (com.codename1.ui.Transform) safeArgs[1]); return null;
            }
        }
        if ("setWindingRule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setWindingRule(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("transform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class}, false)) {
                typedTarget.transform((com.codename1.ui.Transform) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.geom.Point typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.geom.Point2D typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setX(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setY(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.geom.Rectangle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.contains((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds();
            }
        }
        if ("getBounds2D".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds2D();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHeight();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPathIterator();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class}, false)) {
                return typedTarget.getPathIterator((com.codename1.ui.Transform) safeArgs[0]);
            }
        }
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSize();
            }
        }
        if ("getWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWidth();
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
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.intersection((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.intersection((com.codename1.ui.geom.Rectangle) safeArgs[0], (com.codename1.ui.geom.Rectangle) safeArgs[1]); return null;
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.intersection(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.intersects((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.intersects(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("isRectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangle();
            }
        }
        if ("setBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setBounds((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("setBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setBounds(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setWidth(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.geom.Rectangle2D typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class}, false)) {
                return typedTarget.contains((com.codename1.ui.geom.Rectangle2D) safeArgs[0]);
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds();
            }
        }
        if ("getBounds2D".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds2D();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHeight();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPathIterator();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class}, false)) {
                return typedTarget.getPathIterator((com.codename1.ui.Transform) safeArgs[0]);
            }
        }
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSize();
            }
        }
        if ("getWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWidth();
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
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.intersection((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class}, false)) {
                return typedTarget.intersection((com.codename1.ui.geom.Rectangle2D) safeArgs[0]);
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return typedTarget.intersection(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle2D.class}, false)) {
                return typedTarget.intersects((com.codename1.ui.geom.Rectangle2D) safeArgs[0]);
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return typedTarget.intersects(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        if ("isRectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangle();
            }
        }
        if ("setBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.setBounds(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setHeight(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setWidth(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setX(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setX(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.setY(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setY(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                typedTarget.translate(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.geom.PathIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("currentSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return typedTarget.currentSegment((double[]) safeArgs[0]);
            }
        }
        if ("currentSegment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                return typedTarget.currentSegment((float[]) safeArgs[0]);
            }
        }
        if ("getWindingRule".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getWindingRule();
            }
        }
        if ("isDone".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDone();
            }
        }
        if ("next".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.next(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.geom.Shape typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds();
            }
        }
        if ("getBounds2D".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBounds2D();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPathIterator();
            }
        }
        if ("getPathIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class}, false)) {
                return typedTarget.getPathIterator((com.codename1.ui.Transform) safeArgs[0]);
            }
        }
        if ("intersection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.intersection((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("isRectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangle();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.geom.GeneralPath.class) {
            if ("WIND_EVEN_ODD".equals(name)) return com.codename1.ui.geom.GeneralPath.WIND_EVEN_ODD;
            if ("WIND_NON_ZERO".equals(name)) return com.codename1.ui.geom.GeneralPath.WIND_NON_ZERO;
        }
        if (type == com.codename1.ui.geom.PathIterator.class) {
            if ("SEG_CLOSE".equals(name)) return com.codename1.ui.geom.PathIterator.SEG_CLOSE;
            if ("SEG_CUBICTO".equals(name)) return com.codename1.ui.geom.PathIterator.SEG_CUBICTO;
            if ("SEG_LINETO".equals(name)) return com.codename1.ui.geom.PathIterator.SEG_LINETO;
            if ("SEG_MOVETO".equals(name)) return com.codename1.ui.geom.PathIterator.SEG_MOVETO;
            if ("SEG_QUADTO".equals(name)) return com.codename1.ui.geom.PathIterator.SEG_QUADTO;
            if ("WIND_EVEN_ODD".equals(name)) return com.codename1.ui.geom.PathIterator.WIND_EVEN_ODD;
            if ("WIND_NON_ZERO".equals(name)) return com.codename1.ui.geom.PathIterator.WIND_NON_ZERO;
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
