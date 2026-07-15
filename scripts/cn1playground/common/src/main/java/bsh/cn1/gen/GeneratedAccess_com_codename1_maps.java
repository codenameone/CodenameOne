package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_maps {
    private GeneratedAccess_com_codename1_maps() {
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
        if ("BoundingBox".equals(simpleName)) {
            return com.codename1.maps.BoundingBox.class;
        }
        if ("CameraChangeListener".equals(simpleName)) {
            return com.codename1.maps.CameraChangeListener.class;
        }
        if ("CameraPosition".equals(simpleName)) {
            return com.codename1.maps.CameraPosition.class;
        }
        if ("Circle".equals(simpleName)) {
            return com.codename1.maps.Circle.class;
        }
        if ("Coord".equals(simpleName)) {
            return com.codename1.maps.Coord.class;
        }
        if ("LatLng".equals(simpleName)) {
            return com.codename1.maps.LatLng.class;
        }
        if ("MapBounds".equals(simpleName)) {
            return com.codename1.maps.MapBounds.class;
        }
        if ("MapComponent".equals(simpleName)) {
            return com.codename1.maps.MapComponent.class;
        }
        if ("MapListener".equals(simpleName)) {
            return com.codename1.maps.MapListener.class;
        }
        if ("MapObject".equals(simpleName)) {
            return com.codename1.maps.MapObject.class;
        }
        if ("MapSurface".equals(simpleName)) {
            return com.codename1.maps.MapSurface.class;
        }
        if ("MapTapListener".equals(simpleName)) {
            return com.codename1.maps.MapTapListener.class;
        }
        if ("MapView".equals(simpleName)) {
            return com.codename1.maps.MapView.class;
        }
        if ("Marker".equals(simpleName)) {
            return com.codename1.maps.Marker.class;
        }
        if ("MarkerOptions".equals(simpleName)) {
            return com.codename1.maps.MarkerOptions.class;
        }
        if ("Mercator".equals(simpleName)) {
            return com.codename1.maps.Mercator.class;
        }
        if ("NativeMap".equals(simpleName)) {
            return com.codename1.maps.NativeMap.class;
        }
        if ("Polygon".equals(simpleName)) {
            return com.codename1.maps.Polygon.class;
        }
        if ("Polyline".equals(simpleName)) {
            return com.codename1.maps.Polyline.class;
        }
        if ("Projection".equals(simpleName)) {
            return com.codename1.maps.Projection.class;
        }
        if ("ProxyHttpTile".equals(simpleName)) {
            return com.codename1.maps.ProxyHttpTile.class;
        }
        if ("Tile".equals(simpleName)) {
            return com.codename1.maps.Tile.class;
        }
        if ("WebMapProvider".equals(simpleName)) {
            return com.codename1.maps.WebMapProvider.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.BoundingBox.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, com.codename1.maps.Coord.class}, false);
                return new com.codename1.maps.BoundingBox((com.codename1.maps.Coord) adaptedArgs[0], (com.codename1.maps.Coord) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.maps.BoundingBox((com.codename1.maps.Coord) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue());
            }
        }
        if (type == com.codename1.maps.CameraPosition.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false);
                return new com.codename1.maps.CameraPosition((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.maps.CameraPosition((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue());
            }
        }
        if (type == com.codename1.maps.Circle.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false);
                return new com.codename1.maps.Circle((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if (type == com.codename1.maps.Coord.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return new com.codename1.maps.Coord((com.codename1.maps.Coord) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.maps.Coord(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class}, false);
                return new com.codename1.maps.Coord(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if (type == com.codename1.maps.LatLng.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.maps.LatLng(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if (type == com.codename1.maps.MapBounds.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, com.codename1.maps.LatLng.class}, false);
                return new com.codename1.maps.MapBounds((com.codename1.maps.LatLng) adaptedArgs[0], (com.codename1.maps.LatLng) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.maps.MapComponent.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.MapComponent();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class}, false);
                return new com.codename1.maps.MapComponent((com.codename1.maps.providers.MapProvider) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.location.Location.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.location.Location.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.MapComponent((com.codename1.maps.providers.MapProvider) adaptedArgs[0], (com.codename1.location.Location) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.maps.Coord.class, java.lang.Integer.class}, false);
                return new com.codename1.maps.MapComponent((com.codename1.maps.providers.MapProvider) adaptedArgs[0], (com.codename1.maps.Coord) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.location.Location.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.location.Location.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.maps.MapComponent((com.codename1.maps.providers.MapProvider) adaptedArgs[0], (com.codename1.location.Location) adaptedArgs[1], toIntValue(adaptedArgs[2]), ((Boolean) adaptedArgs[3]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.providers.MapProvider.class, com.codename1.maps.Coord.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.maps.MapComponent((com.codename1.maps.providers.MapProvider) adaptedArgs[0], (com.codename1.maps.Coord) adaptedArgs[1], toIntValue(adaptedArgs[2]), ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if (type == com.codename1.maps.MapView.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.MapView();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class}, false);
                return new com.codename1.maps.MapView((com.codename1.maps.vector.TileSource) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class, com.codename1.maps.vector.MapStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class, com.codename1.maps.vector.MapStyle.class}, false);
                return new com.codename1.maps.MapView((com.codename1.maps.vector.TileSource) adaptedArgs[0], (com.codename1.maps.vector.MapStyle) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.maps.MarkerOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.MarkerOptions();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return new com.codename1.maps.MarkerOptions((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.maps.Mercator.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.Mercator();
            }
        }
        if (type == com.codename1.maps.NativeMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.NativeMap();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false);
                return new com.codename1.maps.NativeMap((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class, com.codename1.maps.vector.TileSource.class, com.codename1.maps.vector.MapStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class, com.codename1.maps.vector.TileSource.class, com.codename1.maps.vector.MapStyle.class}, false);
                return new com.codename1.maps.NativeMap((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), (com.codename1.maps.vector.TileSource) adaptedArgs[2], (com.codename1.maps.vector.MapStyle) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.maps.Polygon.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.Polygon();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng[].class}, false);
                return new com.codename1.maps.Polygon((com.codename1.maps.LatLng[]) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.maps.Polyline.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.maps.Polyline();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng[].class}, false);
                return new com.codename1.maps.Polyline((com.codename1.maps.LatLng[]) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.maps.ProxyHttpTile.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class, com.codename1.maps.BoundingBox.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class, com.codename1.maps.BoundingBox.class, java.lang.String.class}, false);
                return new com.codename1.maps.ProxyHttpTile((com.codename1.ui.geom.Dimension) adaptedArgs[0], (com.codename1.maps.BoundingBox) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.maps.Tile.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class, com.codename1.maps.BoundingBox.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class, com.codename1.maps.BoundingBox.class, com.codename1.ui.Image.class}, false);
                return new com.codename1.maps.Tile((com.codename1.ui.geom.Dimension) adaptedArgs[0], (com.codename1.maps.BoundingBox) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.maps.WebMapProvider.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.maps.WebMapProvider((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.maps.BoundingBox.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.maps.LatLng.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.maps.MapBounds.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.maps.MapComponent.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.maps.Mercator.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.maps.NativeMap.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.maps.Tile.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.maps.WebMapProvider.class) return invokeStatic7(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Vector.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Vector.class}, false);
                return com.codename1.maps.BoundingBox.create((java.util.Vector) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false);
                return com.codename1.maps.BoundingBox.create((com.codename1.maps.Coord[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.maps.BoundingBox.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.LatLng.create(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("fromCoord".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return com.codename1.maps.LatLng.fromCoord((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.maps.LatLng.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("fromCoordinates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return com.codename1.maps.MapBounds.fromCoordinates((java.util.List) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.maps.MapBounds.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("distance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.MapComponent.distance(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.maps.MapComponent.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("forwardMercator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.Mercator.forwardMercator(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("inverseMercator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.maps.Mercator.inverseMercator(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.maps.Mercator.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("fireCameraChange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.maps.NativeMap.fireCameraChange(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("fireLongPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                com.codename1.maps.NativeMap.fireLongPress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("fireMarkerClick".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                com.codename1.maps.NativeMap.fireMarkerClick(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue()); return null;
            }
        }
        if ("fireTap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                com.codename1.maps.NativeMap.fireTap(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedStatic(com.codename1.maps.NativeMap.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("setPaintLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.maps.Tile.setPaintLoading(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTileLoadingImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                com.codename1.maps.Tile.setTileLoadingImage((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setTileLoadingText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.maps.Tile.setTileLoadingText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.maps.Tile.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("google".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.maps.WebMapProvider.google((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.maps.WebMapProvider.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.maps.Circle) {
            try {
                return invoke0((com.codename1.maps.Circle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Marker) {
            try {
                return invoke1((com.codename1.maps.Marker) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Mercator) {
            try {
                return invoke2((com.codename1.maps.Mercator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Polygon) {
            try {
                return invoke3((com.codename1.maps.Polygon) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Polyline) {
            try {
                return invoke4((com.codename1.maps.Polyline) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.ProxyHttpTile) {
            try {
                return invoke5((com.codename1.maps.ProxyHttpTile) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.BoundingBox) {
            try {
                return invoke6((com.codename1.maps.BoundingBox) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.CameraPosition) {
            try {
                return invoke7((com.codename1.maps.CameraPosition) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Coord) {
            try {
                return invoke8((com.codename1.maps.Coord) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.LatLng) {
            try {
                return invoke9((com.codename1.maps.LatLng) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapBounds) {
            try {
                return invoke10((com.codename1.maps.MapBounds) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapComponent) {
            try {
                return invoke11((com.codename1.maps.MapComponent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapObject) {
            try {
                return invoke12((com.codename1.maps.MapObject) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapView) {
            try {
                return invoke13((com.codename1.maps.MapView) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MarkerOptions) {
            try {
                return invoke14((com.codename1.maps.MarkerOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.NativeMap) {
            try {
                return invoke15((com.codename1.maps.NativeMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Projection) {
            try {
                return invoke16((com.codename1.maps.Projection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.Tile) {
            try {
                return invoke17((com.codename1.maps.Tile) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.WebMapProvider) {
            try {
                return invoke18((com.codename1.maps.WebMapProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.CameraChangeListener) {
            try {
                return invoke19((com.codename1.maps.CameraChangeListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapListener) {
            try {
                return invoke20((com.codename1.maps.MapListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapSurface) {
            try {
                return invoke21((com.codename1.maps.MapSurface) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.maps.MapTapListener) {
            try {
                return invoke22((com.codename1.maps.MapTapListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.maps.Circle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getFillColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFillColor();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getRadiusMeters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadiusMeters();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeWidth();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.setCenter((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("setFillColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setFillColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setRadiusMeters".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setRadiusMeters(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("setStrokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.maps.Marker typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAnchorU".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnchorU();
            }
        }
        if ("getAnchorV".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnchorV();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getOnClick".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOnClick();
            }
        }
        if ("getPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPosition();
            }
        }
        if ("getSnippet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSnippet();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("setPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                typedTarget.setPosition((com.codename1.maps.LatLng) adaptedArgs[0]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.maps.Mercator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("extent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.extent();
            }
        }
        if ("fromWGS84".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.fromWGS84((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.fromWGS84((com.codename1.maps.Coord) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false);
                return typedTarget.fromWGS84((com.codename1.maps.Coord[]) adaptedArgs[0]);
            }
        }
        if ("toWGS84".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.toWGS84((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.toWGS84((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.maps.Polygon typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.addPoint((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getFillColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFillColor();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getPoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPoints();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeWidth();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("setFillColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setFillColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setStrokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.maps.Polyline typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.addPoint((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getPoints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPoints();
            }
        }
        if ("getStrokeAlpha".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeAlpha();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeWidth();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("setStrokeAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeAlpha(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setStrokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setStrokeWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStrokeWidth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.maps.ProxyHttpTile typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("dimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dimension();
            }
        }
        if ("getBoundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBoundingBox();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                return typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("paintTileLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintTileLoading((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("pointPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.pointPosition((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        if ("position".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.position(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setsTileReadyListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.setsTileReadyListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.maps.BoundingBox typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.contains((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("extend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.extend((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
        }
        if ("getNorthEast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNorthEast();
            }
        }
        if ("getSouthWest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSouthWest();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("latitudeDifference".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.latitudeDifference();
            }
        }
        if ("longitudeDifference".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longitudeDifference();
            }
        }
        if ("projected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.projected();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.maps.CameraPosition typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBearing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBearing();
            }
        }
        if ("getTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTarget();
            }
        }
        if ("getTilt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTilt();
            }
        }
        if ("getZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoom();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("withTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.withTarget((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("withZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.withZoom(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.maps.Coord typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
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

    private static Object invoke9(com.codename1.maps.LatLng typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("distanceTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.distanceTo((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
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
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toCoord".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toCoord();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.maps.MapBounds typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.contains((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("extend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.extend((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getLatitudeSpan".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLatitudeSpan();
            }
        }
        if ("getLongitudeSpan".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLongitudeSpan();
            }
        }
        if ("getNorthEast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNorthEast();
            }
        }
        if ("getSouthWest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSouthWest();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.maps.MapComponent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accessibilityChanged".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.accessibilityChanged(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.accessibilityChanged(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.Layer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.layers.Layer.class}, false);
                typedTarget.addLayer((com.codename1.maps.layers.Layer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.Layer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.layers.Layer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.addLayer((com.codename1.maps.layers.Layer) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapListener.class}, false);
                typedTarget.addMapListener((com.codename1.maps.MapListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("center".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.center();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityNode();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCoordFromPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getCoordFromPosition(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayerAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLayerAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getLayersConut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayersConut();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getMaxZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxZoomLevel();
            }
        }
        if ("getMinZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinZoomLevel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPointFromCoord".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.getPointFromCoord((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProvider();
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSemantics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSemantics();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
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
        if ("getZoomLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoomLevel();
            }
        }
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbGrabbed();
            }
        }
        if ("isHScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbHover();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbGrabbed();
            }
        }
        if ("isVScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbHover();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("moveDown".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.moveDown(); return null;
            }
        }
        if ("moveLeft".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.moveLeft(); return null;
            }
        }
        if ("moveRight".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.moveRight(); return null;
            }
        }
        if ("moveUp".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.moveUp(); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeAllLayers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllLayers(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.layers.Layer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.layers.Layer.class}, false);
                typedTarget.removeLayer((com.codename1.maps.layers.Layer) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapListener.class}, false);
                typedTarget.removeMapListener((com.codename1.maps.MapListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHorizontalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setHorizontalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVerticalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setVerticalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setZoomLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setZoomLevel(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(toIntValue(adaptedArgs[0]));
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("zoomIn".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.zoomIn(); return null;
            }
        }
        if ("zoomOut".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.zoomOut(); return null;
            }
        }
        if ("zoomTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                typedTarget.zoomTo((com.codename1.maps.BoundingBox) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class, java.lang.Integer.class}, false);
                typedTarget.zoomTo((com.codename1.maps.Coord) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("zoomToLayers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.zoomToLayers(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.maps.MapObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.maps.MapView typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accessibilityChanged".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.accessibilityChanged(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.accessibilityChanged(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addCameraChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false);
                typedTarget.addCameraChangeListener((com.codename1.maps.CameraChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("addCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false);
                return typedTarget.addCircle((com.codename1.maps.Circle) adaptedArgs[0]);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MarkerOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MarkerOptions.class}, false);
                return typedTarget.addMarker((com.codename1.maps.MarkerOptions) adaptedArgs[0]);
            }
        }
        if ("addMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false);
                return typedTarget.addPolygon((com.codename1.maps.Polygon) adaptedArgs[0]);
            }
        }
        if ("addPolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false);
                return typedTarget.addPolyline((com.codename1.maps.Polyline) adaptedArgs[0]);
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addTapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.addTapListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("asComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asComponent();
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("clearMapObjects".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearMapObjects(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("fitBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false);
                typedTarget.fitBounds((com.codename1.maps.MapBounds) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityNode();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getCameraPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCameraPosition();
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getEngine".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEngine();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
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
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSemantics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSemantics();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getVisibleRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisibleRegion();
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
        if ("getZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoom();
            }
        }
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbGrabbed();
            }
        }
        if ("isHScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbHover();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isLoadingTiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoadingTiles();
            }
        }
        if ("isMapReady".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMapReady();
            }
        }
        if ("isNativeMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeMap();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbGrabbed();
            }
        }
        if ("isVScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbHover();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("latLngToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.latLngToScreen((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("moveCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false);
                typedTarget.moveCamera((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeCameraChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false);
                typedTarget.removeCameraChangeListener((com.codename1.maps.CameraChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false);
                typedTarget.removeCircle((com.codename1.maps.Circle) adaptedArgs[0]); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Marker.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Marker.class}, false);
                typedTarget.removeMarker((com.codename1.maps.Marker) adaptedArgs[0]); return null;
            }
        }
        if ("removeMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false);
                typedTarget.removePolygon((com.codename1.maps.Polygon) adaptedArgs[0]); return null;
            }
        }
        if ("removePolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false);
                typedTarget.removePolyline((com.codename1.maps.Polyline) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeTapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.removeTapListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("screenToLatLng".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.screenToLatLng(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCameraPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraPosition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraPosition.class}, false);
                typedTarget.setCameraPosition((com.codename1.maps.CameraPosition) adaptedArgs[0]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                typedTarget.setCenter((com.codename1.maps.LatLng) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHorizontalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setHorizontalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.MapStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.MapStyle.class}, false);
                return typedTarget.setStyle((com.codename1.maps.vector.MapStyle) adaptedArgs[0]);
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTileSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.vector.TileSource.class}, false);
                return typedTarget.setTileSource((com.codename1.maps.vector.TileSource) adaptedArgs[0]);
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVerticalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setVerticalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setZoom(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(toIntValue(adaptedArgs[0]));
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.maps.MarkerOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("anchor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.anchor(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("draggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.draggable(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("icon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.EncodedImage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.EncodedImage.class}, false);
                return typedTarget.icon((com.codename1.ui.EncodedImage) adaptedArgs[0]);
            }
        }
        if ("onClick".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                return typedTarget.onClick((com.codename1.ui.events.ActionListener) adaptedArgs[0]);
            }
        }
        if ("position".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.position((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("snippet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.snippet((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("title".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.title((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.maps.NativeMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accessibilityChanged".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.accessibilityChanged(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.accessibilityChanged(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addCameraChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false);
                typedTarget.addCameraChangeListener((com.codename1.maps.CameraChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("addCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false);
                return typedTarget.addCircle((com.codename1.maps.Circle) adaptedArgs[0]);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MarkerOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MarkerOptions.class}, false);
                return typedTarget.addMarker((com.codename1.maps.MarkerOptions) adaptedArgs[0]);
            }
        }
        if ("addMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false);
                return typedTarget.addPolygon((com.codename1.maps.Polygon) adaptedArgs[0]);
            }
        }
        if ("addPolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false);
                return typedTarget.addPolyline((com.codename1.maps.Polyline) adaptedArgs[0]);
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addTapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.addTapListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("asComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asComponent();
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("clearMapObjects".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearMapObjects(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("dispose".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dispose(); return null;
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("fitBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false);
                typedTarget.fitBounds((com.codename1.maps.MapBounds) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityNode();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getCameraPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCameraPosition();
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
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
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSemantics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSemantics();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getVisibleRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisibleRegion();
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
        if ("getZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoom();
            }
        }
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbGrabbed();
            }
        }
        if ("isHScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbHover();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isLoadingTiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoadingTiles();
            }
        }
        if ("isMapReady".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMapReady();
            }
        }
        if ("isNativeMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeMap();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbGrabbed();
            }
        }
        if ("isVScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbHover();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("latLngToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.latLngToScreen((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("moveCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false);
                typedTarget.moveCamera((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeCameraChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false);
                typedTarget.removeCameraChangeListener((com.codename1.maps.CameraChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false);
                typedTarget.removeCircle((com.codename1.maps.Circle) adaptedArgs[0]); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Marker.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Marker.class}, false);
                typedTarget.removeMarker((com.codename1.maps.Marker) adaptedArgs[0]); return null;
            }
        }
        if ("removeMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false);
                typedTarget.removePolygon((com.codename1.maps.Polygon) adaptedArgs[0]); return null;
            }
        }
        if ("removePolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false);
                typedTarget.removePolyline((com.codename1.maps.Polyline) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeTapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.removeTapListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("screenToLatLng".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.screenToLatLng(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCameraPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraPosition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraPosition.class}, false);
                typedTarget.setCameraPosition((com.codename1.maps.CameraPosition) adaptedArgs[0]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                typedTarget.setCenter((com.codename1.maps.LatLng) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHorizontalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setHorizontalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVerticalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setVerticalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setZoom(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(toIntValue(adaptedArgs[0]));
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.maps.Projection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("extent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.extent();
            }
        }
        if ("fromWGS84".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.fromWGS84((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.fromWGS84((com.codename1.maps.Coord) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord[].class}, false);
                return typedTarget.fromWGS84((com.codename1.maps.Coord[]) adaptedArgs[0]);
            }
        }
        if ("toWGS84".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.BoundingBox.class}, false);
                return typedTarget.toWGS84((com.codename1.maps.BoundingBox) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.toWGS84((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.maps.Tile typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("dimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dimension();
            }
        }
        if ("getBoundingBox".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBoundingBox();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                return typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("paintTileLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintTileLoading((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("pointPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Coord.class}, false);
                return typedTarget.pointPosition((com.codename1.maps.Coord) adaptedArgs[0]);
            }
        }
        if ("position".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.position(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setsTileReadyListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.setsTileReadyListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.maps.WebMapProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.addCircle(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue(), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]));
            }
        }
        if ("addMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.addMarker(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1], ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue(), (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], ((Number) adaptedArgs[6]).floatValue(), ((Number) adaptedArgs[7]).floatValue());
            }
        }
        if ("addToPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.addToPath(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue()); return null;
            }
        }
        if ("beginPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.beginPath(toIntValue(adaptedArgs[0]));
            }
        }
        if ("calcLatLongPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.calcLatLongPosition(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("calcScreenPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.calcScreenPosition(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue()); return null;
            }
        }
        if ("createPeer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.NativeMap.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.NativeMap.class, java.lang.Integer.class}, false);
                return typedTarget.createPeer((com.codename1.maps.NativeMap) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("deinitialize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.deinitialize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("finishPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.finishPolygon(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue(), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]));
            }
        }
        if ("finishPolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.finishPolyline(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue(), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLatitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLatitude(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getLongitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getLongitude(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getMaxZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMaxZoom(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getMinZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMinZoom(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getScreenLat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getScreenLat(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getScreenLon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getScreenLon(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getScreenX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getScreenX(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getScreenY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getScreenY(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getZoom(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("isReady".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isReady(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAllElements".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeAllElements(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("removeElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.removeElement(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue()); return null;
            }
        }
        if ("setCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setCamera(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue()); return null;
            }
        }
        if ("setMapType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setMapType(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setRotateGestureEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setRotateGestureEnabled(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setShowMyLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setShowMyLocation(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.maps.CameraChangeListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cameraChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapSurface.class, com.codename1.maps.CameraPosition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapSurface.class, com.codename1.maps.CameraPosition.class}, false);
                typedTarget.cameraChanged((com.codename1.maps.MapSurface) adaptedArgs[0], (com.codename1.maps.CameraPosition) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.maps.MapListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("mapPositionUpdated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, com.codename1.maps.Coord.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, com.codename1.maps.Coord.class}, false);
                typedTarget.mapPositionUpdated((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.maps.Coord) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.maps.MapSurface typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addCameraChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false);
                typedTarget.addCameraChangeListener((com.codename1.maps.CameraChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("addCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false);
                return typedTarget.addCircle((com.codename1.maps.Circle) adaptedArgs[0]);
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MarkerOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MarkerOptions.class}, false);
                return typedTarget.addMarker((com.codename1.maps.MarkerOptions) adaptedArgs[0]);
            }
        }
        if ("addPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false);
                return typedTarget.addPolygon((com.codename1.maps.Polygon) adaptedArgs[0]);
            }
        }
        if ("addPolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false);
                return typedTarget.addPolyline((com.codename1.maps.Polyline) adaptedArgs[0]);
            }
        }
        if ("addTapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.addTapListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("asComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asComponent();
            }
        }
        if ("clearMapObjects".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearMapObjects(); return null;
            }
        }
        if ("fitBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapBounds.class, java.lang.Integer.class}, false);
                typedTarget.fitBounds((com.codename1.maps.MapBounds) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("getCameraPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCameraPosition();
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
        if ("getVisibleRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisibleRegion();
            }
        }
        if ("getZoom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoom();
            }
        }
        if ("isLoadingTiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoadingTiles();
            }
        }
        if ("isNativeMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeMap();
            }
        }
        if ("latLngToScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                return typedTarget.latLngToScreen((com.codename1.maps.LatLng) adaptedArgs[0]);
            }
        }
        if ("moveCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class, java.lang.Double.class}, false);
                typedTarget.moveCamera((com.codename1.maps.LatLng) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("removeCameraChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraChangeListener.class}, false);
                typedTarget.removeCameraChangeListener((com.codename1.maps.CameraChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeCircle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Circle.class}, false);
                typedTarget.removeCircle((com.codename1.maps.Circle) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMarker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Marker.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Marker.class}, false);
                typedTarget.removeMarker((com.codename1.maps.Marker) adaptedArgs[0]); return null;
            }
        }
        if ("removePolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polygon.class}, false);
                typedTarget.removePolygon((com.codename1.maps.Polygon) adaptedArgs[0]); return null;
            }
        }
        if ("removePolyline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.Polyline.class}, false);
                typedTarget.removePolyline((com.codename1.maps.Polyline) adaptedArgs[0]); return null;
            }
        }
        if ("removeTapListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapTapListener.class}, false);
                typedTarget.removeTapListener((com.codename1.maps.MapTapListener) adaptedArgs[0]); return null;
            }
        }
        if ("screenToLatLng".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.screenToLatLng(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setCameraPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.CameraPosition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.CameraPosition.class}, false);
                typedTarget.setCameraPosition((com.codename1.maps.CameraPosition) adaptedArgs[0]); return null;
            }
        }
        if ("setCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.LatLng.class}, false);
                typedTarget.setCenter((com.codename1.maps.LatLng) adaptedArgs[0]); return null;
            }
        }
        if ("setZoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setZoom(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.maps.MapTapListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("mapTapped".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.maps.MapSurface.class, com.codename1.maps.LatLng.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.maps.MapSurface.class, com.codename1.maps.LatLng.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.mapTapped((com.codename1.maps.MapSurface) adaptedArgs[0], (com.codename1.maps.LatLng) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.maps.MapComponent.class) return getStaticField0(name);
        if (type == com.codename1.maps.MapView.class) return getStaticField1(name);
        if (type == com.codename1.maps.NativeMap.class) return getStaticField2(name);
        if (type == com.codename1.maps.WebMapProvider.class) return getStaticField3(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BASELINE".equals(name)) return com.codename1.maps.MapComponent.BASELINE;
        if ("BOTTOM".equals(name)) return com.codename1.maps.MapComponent.BOTTOM;
        if ("BRB_CENTER_OFFSET".equals(name)) return com.codename1.maps.MapComponent.BRB_CENTER_OFFSET;
        if ("BRB_CONSTANT_ASCENT".equals(name)) return com.codename1.maps.MapComponent.BRB_CONSTANT_ASCENT;
        if ("BRB_CONSTANT_DESCENT".equals(name)) return com.codename1.maps.MapComponent.BRB_CONSTANT_DESCENT;
        if ("BRB_OTHER".equals(name)) return com.codename1.maps.MapComponent.BRB_OTHER;
        if ("CENTER".equals(name)) return com.codename1.maps.MapComponent.CENTER;
        if ("CROSSHAIR_CURSOR".equals(name)) return com.codename1.maps.MapComponent.CROSSHAIR_CURSOR;
        if ("DEFAULT_CURSOR".equals(name)) return com.codename1.maps.MapComponent.DEFAULT_CURSOR;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_X".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_IMMEDIATELY_DRAG_X;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_XY".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_IMMEDIATELY_DRAG_XY;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_Y".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_IMMEDIATELY_DRAG_Y;
        if ("DRAG_REGION_LIKELY_DRAG_X".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_LIKELY_DRAG_X;
        if ("DRAG_REGION_LIKELY_DRAG_XY".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_LIKELY_DRAG_XY;
        if ("DRAG_REGION_LIKELY_DRAG_Y".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_LIKELY_DRAG_Y;
        if ("DRAG_REGION_NOT_DRAGGABLE".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_NOT_DRAGGABLE;
        if ("DRAG_REGION_POSSIBLE_DRAG_X".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_POSSIBLE_DRAG_X;
        if ("DRAG_REGION_POSSIBLE_DRAG_XY".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_POSSIBLE_DRAG_XY;
        if ("DRAG_REGION_POSSIBLE_DRAG_Y".equals(name)) return com.codename1.maps.MapComponent.DRAG_REGION_POSSIBLE_DRAG_Y;
        if ("E_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.E_RESIZE_CURSOR;
        if ("HAND_CURSOR".equals(name)) return com.codename1.maps.MapComponent.HAND_CURSOR;
        if ("LEFT".equals(name)) return com.codename1.maps.MapComponent.LEFT;
        if ("MOVE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.MOVE_CURSOR;
        if ("NE_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.NE_RESIZE_CURSOR;
        if ("NW_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.NW_RESIZE_CURSOR;
        if ("N_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.N_RESIZE_CURSOR;
        if ("RIGHT".equals(name)) return com.codename1.maps.MapComponent.RIGHT;
        if ("SE_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.SE_RESIZE_CURSOR;
        if ("SW_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.SW_RESIZE_CURSOR;
        if ("S_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.S_RESIZE_CURSOR;
        if ("TEXT_CURSOR".equals(name)) return com.codename1.maps.MapComponent.TEXT_CURSOR;
        if ("TOP".equals(name)) return com.codename1.maps.MapComponent.TOP;
        if ("WAIT_CURSOR".equals(name)) return com.codename1.maps.MapComponent.WAIT_CURSOR;
        if ("W_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapComponent.W_RESIZE_CURSOR;
        throw unsupportedStaticField(com.codename1.maps.MapComponent.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("BASELINE".equals(name)) return com.codename1.maps.MapView.BASELINE;
        if ("BOTTOM".equals(name)) return com.codename1.maps.MapView.BOTTOM;
        if ("BRB_CENTER_OFFSET".equals(name)) return com.codename1.maps.MapView.BRB_CENTER_OFFSET;
        if ("BRB_CONSTANT_ASCENT".equals(name)) return com.codename1.maps.MapView.BRB_CONSTANT_ASCENT;
        if ("BRB_CONSTANT_DESCENT".equals(name)) return com.codename1.maps.MapView.BRB_CONSTANT_DESCENT;
        if ("BRB_OTHER".equals(name)) return com.codename1.maps.MapView.BRB_OTHER;
        if ("CENTER".equals(name)) return com.codename1.maps.MapView.CENTER;
        if ("CROSSHAIR_CURSOR".equals(name)) return com.codename1.maps.MapView.CROSSHAIR_CURSOR;
        if ("DEFAULT_CURSOR".equals(name)) return com.codename1.maps.MapView.DEFAULT_CURSOR;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_X".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_IMMEDIATELY_DRAG_X;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_XY".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_IMMEDIATELY_DRAG_XY;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_Y".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_IMMEDIATELY_DRAG_Y;
        if ("DRAG_REGION_LIKELY_DRAG_X".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_LIKELY_DRAG_X;
        if ("DRAG_REGION_LIKELY_DRAG_XY".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_LIKELY_DRAG_XY;
        if ("DRAG_REGION_LIKELY_DRAG_Y".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_LIKELY_DRAG_Y;
        if ("DRAG_REGION_NOT_DRAGGABLE".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_NOT_DRAGGABLE;
        if ("DRAG_REGION_POSSIBLE_DRAG_X".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_POSSIBLE_DRAG_X;
        if ("DRAG_REGION_POSSIBLE_DRAG_XY".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_POSSIBLE_DRAG_XY;
        if ("DRAG_REGION_POSSIBLE_DRAG_Y".equals(name)) return com.codename1.maps.MapView.DRAG_REGION_POSSIBLE_DRAG_Y;
        if ("E_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.E_RESIZE_CURSOR;
        if ("HAND_CURSOR".equals(name)) return com.codename1.maps.MapView.HAND_CURSOR;
        if ("LEFT".equals(name)) return com.codename1.maps.MapView.LEFT;
        if ("MOVE_CURSOR".equals(name)) return com.codename1.maps.MapView.MOVE_CURSOR;
        if ("NE_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.NE_RESIZE_CURSOR;
        if ("NW_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.NW_RESIZE_CURSOR;
        if ("N_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.N_RESIZE_CURSOR;
        if ("RIGHT".equals(name)) return com.codename1.maps.MapView.RIGHT;
        if ("SE_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.SE_RESIZE_CURSOR;
        if ("SW_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.SW_RESIZE_CURSOR;
        if ("S_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.S_RESIZE_CURSOR;
        if ("TEXT_CURSOR".equals(name)) return com.codename1.maps.MapView.TEXT_CURSOR;
        if ("TOP".equals(name)) return com.codename1.maps.MapView.TOP;
        if ("WAIT_CURSOR".equals(name)) return com.codename1.maps.MapView.WAIT_CURSOR;
        if ("W_RESIZE_CURSOR".equals(name)) return com.codename1.maps.MapView.W_RESIZE_CURSOR;
        throw unsupportedStaticField(com.codename1.maps.MapView.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("BASELINE".equals(name)) return com.codename1.maps.NativeMap.BASELINE;
        if ("BOTTOM".equals(name)) return com.codename1.maps.NativeMap.BOTTOM;
        if ("BRB_CENTER_OFFSET".equals(name)) return com.codename1.maps.NativeMap.BRB_CENTER_OFFSET;
        if ("BRB_CONSTANT_ASCENT".equals(name)) return com.codename1.maps.NativeMap.BRB_CONSTANT_ASCENT;
        if ("BRB_CONSTANT_DESCENT".equals(name)) return com.codename1.maps.NativeMap.BRB_CONSTANT_DESCENT;
        if ("BRB_OTHER".equals(name)) return com.codename1.maps.NativeMap.BRB_OTHER;
        if ("CENTER".equals(name)) return com.codename1.maps.NativeMap.CENTER;
        if ("CROSSHAIR_CURSOR".equals(name)) return com.codename1.maps.NativeMap.CROSSHAIR_CURSOR;
        if ("DEFAULT_CURSOR".equals(name)) return com.codename1.maps.NativeMap.DEFAULT_CURSOR;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_X".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_IMMEDIATELY_DRAG_X;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_XY".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_IMMEDIATELY_DRAG_XY;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_Y".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_IMMEDIATELY_DRAG_Y;
        if ("DRAG_REGION_LIKELY_DRAG_X".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_LIKELY_DRAG_X;
        if ("DRAG_REGION_LIKELY_DRAG_XY".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_LIKELY_DRAG_XY;
        if ("DRAG_REGION_LIKELY_DRAG_Y".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_LIKELY_DRAG_Y;
        if ("DRAG_REGION_NOT_DRAGGABLE".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_NOT_DRAGGABLE;
        if ("DRAG_REGION_POSSIBLE_DRAG_X".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_POSSIBLE_DRAG_X;
        if ("DRAG_REGION_POSSIBLE_DRAG_XY".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_POSSIBLE_DRAG_XY;
        if ("DRAG_REGION_POSSIBLE_DRAG_Y".equals(name)) return com.codename1.maps.NativeMap.DRAG_REGION_POSSIBLE_DRAG_Y;
        if ("E_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.E_RESIZE_CURSOR;
        if ("HAND_CURSOR".equals(name)) return com.codename1.maps.NativeMap.HAND_CURSOR;
        if ("LEFT".equals(name)) return com.codename1.maps.NativeMap.LEFT;
        if ("MOVE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.MOVE_CURSOR;
        if ("NE_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.NE_RESIZE_CURSOR;
        if ("NW_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.NW_RESIZE_CURSOR;
        if ("N_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.N_RESIZE_CURSOR;
        if ("RIGHT".equals(name)) return com.codename1.maps.NativeMap.RIGHT;
        if ("SE_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.SE_RESIZE_CURSOR;
        if ("SW_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.SW_RESIZE_CURSOR;
        if ("S_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.S_RESIZE_CURSOR;
        if ("TEXT_CURSOR".equals(name)) return com.codename1.maps.NativeMap.TEXT_CURSOR;
        if ("TOP".equals(name)) return com.codename1.maps.NativeMap.TOP;
        if ("WAIT_CURSOR".equals(name)) return com.codename1.maps.NativeMap.WAIT_CURSOR;
        if ("W_RESIZE_CURSOR".equals(name)) return com.codename1.maps.NativeMap.W_RESIZE_CURSOR;
        throw unsupportedStaticField(com.codename1.maps.NativeMap.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("MAP_TYPE_HYBRID".equals(name)) return com.codename1.maps.WebMapProvider.MAP_TYPE_HYBRID;
        if ("MAP_TYPE_SATELLITE".equals(name)) return com.codename1.maps.WebMapProvider.MAP_TYPE_SATELLITE;
        if ("MAP_TYPE_STANDARD".equals(name)) return com.codename1.maps.WebMapProvider.MAP_TYPE_STANDARD;
        if ("MAP_TYPE_TERRAIN".equals(name)) return com.codename1.maps.WebMapProvider.MAP_TYPE_TERRAIN;
        throw unsupportedStaticField(com.codename1.maps.WebMapProvider.class, name);
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
