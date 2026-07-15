/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_maps_vector {
    private GeneratedAccess_com_codename1_maps_vector() {
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
        if ("BundledTileSource".equals(simpleName)) {
            return com.codename1.maps.vector.BundledTileSource.class;
        }
        if ("DemoTileSource".equals(simpleName)) {
            return com.codename1.maps.vector.DemoTileSource.class;
        }
        if ("HttpTileSource".equals(simpleName)) {
            return com.codename1.maps.vector.HttpTileSource.class;
        }
        if ("MapStyle".equals(simpleName)) {
            return com.codename1.maps.vector.MapStyle.class;
        }
        if ("MvtDecoder".equals(simpleName)) {
            return com.codename1.maps.vector.MvtDecoder.class;
        }
        if ("MvtTileSource".equals(simpleName)) {
            return com.codename1.maps.vector.MvtTileSource.class;
        }
        if ("RasterTileSource".equals(simpleName)) {
            return com.codename1.maps.vector.RasterTileSource.class;
        }
        if ("StyleLayer".equals(simpleName)) {
            return com.codename1.maps.vector.StyleLayer.class;
        }
        if ("TileCallback".equals(simpleName)) {
            return com.codename1.maps.vector.TileCallback.class;
        }
        if ("TileSource".equals(simpleName)) {
            return com.codename1.maps.vector.TileSource.class;
        }
        if ("VectorFeature".equals(simpleName)) {
            return com.codename1.maps.vector.VectorFeature.class;
        }
        if ("VectorLayer".equals(simpleName)) {
            return com.codename1.maps.vector.VectorLayer.class;
        }
        if ("VectorMapEngine".equals(simpleName)) {
            return com.codename1.maps.vector.VectorMapEngine.class;
        }
        if ("VectorTile".equals(simpleName)) {
            return com.codename1.maps.vector.VectorTile.class;
        }
        if ("WebMercator".equals(simpleName)) {
            return com.codename1.maps.vector.WebMercator.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.vector.BundledTileSource.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.vector.BundledTileSource((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if (type == com.codename1.maps.vector.DemoTileSource.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.vector.DemoTileSource();
            }
        }
        if (type == com.codename1.maps.vector.HttpTileSource.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.vector.HttpTileSource((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if (type == com.codename1.maps.vector.MapStyle.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.vector.MapStyle((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.maps.vector.MvtTileSource.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.vector.MvtTileSource((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if (type == com.codename1.maps.vector.RasterTileSource.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.vector.RasterTileSource((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if (type == com.codename1.maps.vector.VectorMapEngine.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class, com.codename1.maps.vector.MapStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class, com.codename1.maps.vector.MapStyle.class}, false);
                return new com.codename1.maps.vector.VectorMapEngine((com.codename1.maps.vector.TileSource) adaptedArgs[0], (com.codename1.maps.vector.MapStyle) adaptedArgs[1]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.vector.DemoTileSource.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.maps.vector.MapStyle.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.maps.vector.MvtDecoder.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.maps.vector.MvtTileSource.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.maps.vector.RasterTileSource.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.maps.vector.WebMercator.class) return invokeStatic5(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("buildTile".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.maps.vector.DemoTileSource.buildTile();
            }
        }
        throw unsupportedStatic(com.codename1.maps.vector.DemoTileSource.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("dark".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.maps.vector.MapStyle.dark();
            }
        }
        if ("fromJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.maps.vector.MapStyle.fromJson((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("light".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.maps.vector.MapStyle.light();
            }
        }
        throw unsupportedStatic(com.codename1.maps.vector.MapStyle.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.maps.vector.MvtDecoder.decode((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.maps.vector.MvtDecoder.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("openFreeMap".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.maps.vector.MvtTileSource.openFreeMap();
            }
        }
        throw unsupportedStatic(com.codename1.maps.vector.MvtTileSource.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("openStreetMap".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.maps.vector.RasterTileSource.openStreetMap();
            }
        }
        throw unsupportedStatic(com.codename1.maps.vector.RasterTileSource.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("latToWorldY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.vector.WebMercator.latToWorldY(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("lonToWorldX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.vector.WebMercator.lonToWorldX(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("sinh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.maps.vector.WebMercator.sinh(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("worldSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.maps.vector.WebMercator.worldSize(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("worldXToLon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.vector.WebMercator.worldXToLon(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("worldYToLat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.vector.WebMercator.worldYToLat(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.maps.vector.WebMercator.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.maps.vector.MvtTileSource) {
            try {
                return invoke0((com.codename1.maps.vector.MvtTileSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.RasterTileSource) {
            try {
                return invoke1((com.codename1.maps.vector.RasterTileSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.BundledTileSource) {
            try {
                return invoke2((com.codename1.maps.vector.BundledTileSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.DemoTileSource) {
            try {
                return invoke3((com.codename1.maps.vector.DemoTileSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.HttpTileSource) {
            try {
                return invoke4((com.codename1.maps.vector.HttpTileSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.MapStyle) {
            try {
                return invoke5((com.codename1.maps.vector.MapStyle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.VectorFeature) {
            try {
                return invoke6((com.codename1.maps.vector.VectorFeature) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.VectorLayer) {
            try {
                return invoke7((com.codename1.maps.vector.VectorLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.VectorMapEngine) {
            try {
                return invoke8((com.codename1.maps.vector.VectorMapEngine) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.VectorTile) {
            try {
                return invoke9((com.codename1.maps.vector.VectorTile) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.TileCallback) {
            try {
                return invoke10((com.codename1.maps.vector.TileCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.vector.TileSource) {
            try {
                return invoke11((com.codename1.maps.vector.TileSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.maps.vector.MvtTileSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false);
                typedTarget.fetchTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.maps.vector.TileCallback) adaptedArgs[3]); return null;
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("isVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVector();
            }
        }
        if ("setApiKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setApiKey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setAttribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAttribution((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.maps.vector.RasterTileSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false);
                typedTarget.fetchTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.maps.vector.TileCallback) adaptedArgs[3]); return null;
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("isVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVector();
            }
        }
        if ("setApiKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setApiKey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setAttribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAttribution((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.maps.vector.BundledTileSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false);
                typedTarget.fetchTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.maps.vector.TileCallback) adaptedArgs[3]); return null;
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("isVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVector();
            }
        }
        if ("setAttribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAttribution((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.maps.vector.DemoTileSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false);
                typedTarget.fetchTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.maps.vector.TileCallback) adaptedArgs[3]); return null;
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("isVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVector();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.maps.vector.HttpTileSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false);
                typedTarget.fetchTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.maps.vector.TileCallback) adaptedArgs[3]); return null;
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("isVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVector();
            }
        }
        if ("setApiKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setApiKey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setAttribution".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAttribution((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.maps.vector.MapStyle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.StyleLayer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.StyleLayer.class}, false);
                return typedTarget.add((com.codename1.maps.vector.StyleLayer) adaptedArgs[0]);
            }
        }
        if ("getBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.maps.vector.VectorFeature typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAttribute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getAttribute((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getAttributes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttributes();
            }
        }
        if ("getGeometryType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGeometryType();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getParts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParts();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.maps.vector.VectorLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getExtent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtent();
            }
        }
        if ("getFeatures".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFeatures();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.maps.vector.VectorMapEngine typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearCache(); return null;
            }
        }
        if ("fitBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false);
                typedTarget.fitBounds((com.codename1.maps.MapBounds) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getPixelRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPixelRatio();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisibleBounds();
            }
        }
        if ("getZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoom();
            }
        }
        if ("hasPendingTiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasPendingTiles();
            }
        }
        if ("hasRenderedVisibleTiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasRenderedVisibleTiles();
            }
        }
        if ("latLngToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.latLngToScreen((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("panPixels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.panPixels(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("screenToLatLng".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.screenToLatLng(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                typedTarget.setCenter((com.codename1.maps.LatLng) adaptedArgs[0]); return null;
            }
        }
        if ("setPixelRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setPixelRatio(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setRepaintCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setRepaintCallback((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class}, false);
                typedTarget.setSource((com.codename1.maps.vector.TileSource) adaptedArgs[0]); return null;
            }
        }
        if ("setStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.MapStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.MapStyle.class}, false);
                typedTarget.setStyle((com.codename1.maps.vector.MapStyle) adaptedArgs[0]); return null;
            }
        }
        if ("setViewport".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setViewport(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setZoom(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("zoomAround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.zoomAround(((Number) adaptedArgs[0]).doubleValue(), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.maps.vector.VectorTile typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLayer((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getLayers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayers();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.maps.vector.TileCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("tileFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.tileFailed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("tileLoaded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, byte[].class}, false);
                typedTarget.tileLoaded(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (byte[]) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.maps.vector.TileSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.maps.vector.TileCallback.class}, false);
                typedTarget.fetchTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.maps.vector.TileCallback) adaptedArgs[3]); return null;
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoom();
            }
        }
        if ("getMinZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoom();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("isVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVector();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.maps.vector.StyleLayer.class) return getStaticField0(name);
        if (type == com.codename1.maps.vector.VectorFeature.class) return getStaticField1(name);
        if (type == com.codename1.maps.vector.WebMercator.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("TYPE_BACKGROUND".equals(name)) return com.codename1.maps.vector.StyleLayer.TYPE_BACKGROUND;
        if ("TYPE_FILL".equals(name)) return com.codename1.maps.vector.StyleLayer.TYPE_FILL;
        if ("TYPE_LINE".equals(name)) return com.codename1.maps.vector.StyleLayer.TYPE_LINE;
        if ("TYPE_SYMBOL".equals(name)) return com.codename1.maps.vector.StyleLayer.TYPE_SYMBOL;
        throw unsupportedStaticField(com.codename1.maps.vector.StyleLayer.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("GEOM_LINESTRING".equals(name)) return com.codename1.maps.vector.VectorFeature.GEOM_LINESTRING;
        if ("GEOM_POINT".equals(name)) return com.codename1.maps.vector.VectorFeature.GEOM_POINT;
        if ("GEOM_POLYGON".equals(name)) return com.codename1.maps.vector.VectorFeature.GEOM_POLYGON;
        if ("GEOM_UNKNOWN".equals(name)) return com.codename1.maps.vector.VectorFeature.GEOM_UNKNOWN;
        throw unsupportedStaticField(com.codename1.maps.vector.VectorFeature.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("TILE_SIZE".equals(name)) return com.codename1.maps.vector.WebMercator.TILE_SIZE;
        throw unsupportedStaticField(com.codename1.maps.vector.WebMercator.class, name);
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
