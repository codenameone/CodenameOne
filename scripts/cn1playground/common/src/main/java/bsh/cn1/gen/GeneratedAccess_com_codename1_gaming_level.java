package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_gaming_level {
    private GeneratedAccess_com_codename1_gaming_level() {
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
        if ("AssetCatalog".equals(simpleName)) {
            return com.codename1.gaming.level.AssetCatalog.class;
        }
        if ("AssetDef".equals(simpleName)) {
            return com.codename1.gaming.level.AssetDef.class;
        }
        if ("Kind".equals(simpleName)) {
            return com.codename1.gaming.level.AssetDef.Kind.class;
        }
        if ("Type".equals(simpleName)) {
            return com.codename1.gaming.level.AssetDef.Type.class;
        }
        if ("AssetPack".equals(simpleName)) {
            return com.codename1.gaming.level.AssetPack.class;
        }
        if ("ChunkProvider".equals(simpleName)) {
            return com.codename1.gaming.level.ChunkProvider.class;
        }
        if ("GameElement".equals(simpleName)) {
            return com.codename1.gaming.level.GameElement.class;
        }
        if ("GameLevel".equals(simpleName)) {
            return com.codename1.gaming.level.GameLevel.class;
        }
        if ("Mode".equals(simpleName)) {
            return com.codename1.gaming.level.GameLevel.Mode.class;
        }
        if ("GameSceneView".equals(simpleName)) {
            return com.codename1.gaming.level.GameSceneView.class;
        }
        if ("GameWorld".equals(simpleName)) {
            return com.codename1.gaming.level.GameWorld.class;
        }
        if ("IsoProjection".equals(simpleName)) {
            return com.codename1.gaming.level.IsoProjection.class;
        }
        if ("Layer".equals(simpleName)) {
            return com.codename1.gaming.level.Layer.class;
        }
        if ("Kind".equals(simpleName)) {
            return com.codename1.gaming.level.Layer.Kind.class;
        }
        if ("LevelLight".equals(simpleName)) {
            return com.codename1.gaming.level.LevelLight.class;
        }
        if ("Material".equals(simpleName)) {
            return com.codename1.gaming.level.Material.class;
        }
        if ("MaterialRegistry".equals(simpleName)) {
            return com.codename1.gaming.level.MaterialRegistry.class;
        }
        if ("Region".equals(simpleName)) {
            return com.codename1.gaming.level.Region.class;
        }
        if ("RegionProvider".equals(simpleName)) {
            return com.codename1.gaming.level.RegionProvider.class;
        }
        if ("StreamingTerrain".equals(simpleName)) {
            return com.codename1.gaming.level.StreamingTerrain.class;
        }
        if ("MemoryChunkProvider".equals(simpleName)) {
            return com.codename1.gaming.level.StreamingTerrain.MemoryChunkProvider.class;
        }
        if ("Terrain".equals(simpleName)) {
            return com.codename1.gaming.level.Terrain.class;
        }
        if ("TerrainChunk".equals(simpleName)) {
            return com.codename1.gaming.level.TerrainChunk.class;
        }
        if ("TerrainFeature".equals(simpleName)) {
            return com.codename1.gaming.level.TerrainFeature.class;
        }
        if ("TerrainGrid".equals(simpleName)) {
            return com.codename1.gaming.level.TerrainGrid.class;
        }
        if ("TileLayer".equals(simpleName)) {
            return com.codename1.gaming.level.TileLayer.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.level.AssetCatalog.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.AssetCatalog();
            }
        }
        if (type == com.codename1.gaming.level.AssetDef.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.AssetDef();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.gaming.level.AssetDef.Kind.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.gaming.level.AssetDef.Kind.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.gaming.level.AssetDef((java.lang.String) adaptedArgs[0], (com.codename1.gaming.level.AssetDef.Kind) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]));
            }
        }
        if (type == com.codename1.gaming.level.AssetPack.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.AssetPack();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.gaming.level.AssetPack((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.level.GameElement.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.GameElement();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.gaming.level.GameElement((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.level.GameLevel.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.GameLevel();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameLevel.Mode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameLevel.Mode.class}, false);
                return new com.codename1.gaming.level.GameLevel((com.codename1.gaming.level.GameLevel.Mode) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.level.GameSceneView.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameLevel.class, com.codename1.gaming.level.AssetCatalog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameLevel.class, com.codename1.gaming.level.AssetCatalog.class}, false);
                return new com.codename1.gaming.level.GameSceneView((com.codename1.gaming.level.GameLevel) adaptedArgs[0], (com.codename1.gaming.level.AssetCatalog) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.level.GameWorld.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.GameWorld();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.RegionProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.RegionProvider.class}, false);
                return new com.codename1.gaming.level.GameWorld((com.codename1.gaming.level.RegionProvider) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.gaming.level.IsoProjection.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.IsoProjection();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.level.IsoProjection(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if (type == com.codename1.gaming.level.Layer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.Layer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.gaming.level.Layer.Kind.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.gaming.level.Layer.Kind.class}, false);
                return new com.codename1.gaming.level.Layer((java.lang.String) adaptedArgs[0], (com.codename1.gaming.level.Layer.Kind) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.level.LevelLight.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.LevelLight();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.gaming.level.LevelLight(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]));
            }
        }
        if (type == com.codename1.gaming.level.Material.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.Material();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.gaming.level.Material((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
        }
        if (type == com.codename1.gaming.level.Region.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.Region();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.gaming.level.Region((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.level.StreamingTerrain.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.StreamingTerrain();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.ChunkProvider.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.ChunkProvider.class, java.lang.Integer.class}, false);
                return new com.codename1.gaming.level.StreamingTerrain((com.codename1.gaming.level.ChunkProvider) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.gaming.level.StreamingTerrain.MemoryChunkProvider.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.StreamingTerrain.MemoryChunkProvider();
            }
        }
        if (type == com.codename1.gaming.level.TerrainChunk.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.gaming.level.TerrainChunk(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.gaming.level.TerrainFeature.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.TerrainFeature();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.gaming.level.TerrainFeature((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.gaming.level.TerrainGrid.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.gaming.level.TerrainGrid();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                return new com.codename1.gaming.level.TerrainGrid(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if (type == com.codename1.gaming.level.TileLayer.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.SpriteSheet.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.SpriteSheet.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.gaming.level.TileLayer((com.codename1.gaming.SpriteSheet) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.gaming.level.AssetCatalog.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.gaming.level.AssetDef.Kind.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.gaming.level.AssetDef.Type.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.gaming.level.GameLevel.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.gaming.level.GameLevel.Mode.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.gaming.level.GameWorld.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.gaming.level.Layer.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.gaming.level.Layer.Kind.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.gaming.level.MaterialRegistry.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.gaming.level.Region.class) return invokeStatic9(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("load".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.AssetCatalog.load((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.AssetCatalog.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromWire".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.AssetDef.Kind.fromWire((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.AssetDef.Kind.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("fromWire".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.AssetDef.Type.fromWire((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.AssetDef.Type.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("load".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.GameLevel.load((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.GameLevel.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("fromWire".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.GameLevel.Mode.fromWire((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.GameLevel.Mode.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("fromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.gaming.level.GameWorld.fromMap((java.util.Map) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.GameWorld.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("cellKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.gaming.level.Layer.cellKey(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.Layer.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("fromWire".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.Layer.Kind.fromWire((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.Layer.Kind.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("all".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.gaming.level.MaterialRegistry.all();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.MaterialRegistry.contains((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.MaterialRegistry.get((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("ids".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.gaming.level.MaterialRegistry.ids();
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.Material.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.Material.class}, false);
                com.codename1.gaming.level.MaterialRegistry.register((com.codename1.gaming.level.Material) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.MaterialRegistry.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("fromJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.gaming.level.Region.fromJson((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.gaming.level.Region.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.gaming.level.AssetCatalog) {
            try {
                return invoke0((com.codename1.gaming.level.AssetCatalog) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.AssetDef) {
            try {
                return invoke1((com.codename1.gaming.level.AssetDef) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.AssetDef.Kind) {
            try {
                return invoke2((com.codename1.gaming.level.AssetDef.Kind) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.AssetDef.Type) {
            try {
                return invoke3((com.codename1.gaming.level.AssetDef.Type) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.AssetPack) {
            try {
                return invoke4((com.codename1.gaming.level.AssetPack) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.GameElement) {
            try {
                return invoke5((com.codename1.gaming.level.GameElement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.GameLevel) {
            try {
                return invoke6((com.codename1.gaming.level.GameLevel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.GameLevel.Mode) {
            try {
                return invoke7((com.codename1.gaming.level.GameLevel.Mode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.GameSceneView) {
            try {
                return invoke8((com.codename1.gaming.level.GameSceneView) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.GameWorld) {
            try {
                return invoke9((com.codename1.gaming.level.GameWorld) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.IsoProjection) {
            try {
                return invoke10((com.codename1.gaming.level.IsoProjection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.Layer) {
            try {
                return invoke11((com.codename1.gaming.level.Layer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.Layer.Kind) {
            try {
                return invoke12((com.codename1.gaming.level.Layer.Kind) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.LevelLight) {
            try {
                return invoke13((com.codename1.gaming.level.LevelLight) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.Material) {
            try {
                return invoke14((com.codename1.gaming.level.Material) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.Region) {
            try {
                return invoke15((com.codename1.gaming.level.Region) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.StreamingTerrain) {
            try {
                return invoke16((com.codename1.gaming.level.StreamingTerrain) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.StreamingTerrain.MemoryChunkProvider) {
            try {
                return invoke17((com.codename1.gaming.level.StreamingTerrain.MemoryChunkProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.TerrainChunk) {
            try {
                return invoke18((com.codename1.gaming.level.TerrainChunk) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.TerrainFeature) {
            try {
                return invoke19((com.codename1.gaming.level.TerrainFeature) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.TerrainGrid) {
            try {
                return invoke20((com.codename1.gaming.level.TerrainGrid) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.TileLayer) {
            try {
                return invoke21((com.codename1.gaming.level.TileLayer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.ChunkProvider) {
            try {
                return invoke22((com.codename1.gaming.level.ChunkProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.RegionProvider) {
            try {
                return invoke23((com.codename1.gaming.level.RegionProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.gaming.level.Terrain) {
            try {
                return invoke24((com.codename1.gaming.level.Terrain) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.gaming.level.AssetCatalog typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetPack.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetPack.class}, false);
                return typedTarget.addPack((com.codename1.gaming.level.AssetPack) adaptedArgs[0]);
            }
        }
        if ("def".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.def((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getPack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPack((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("hasImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.hasImage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("image".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.image((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("meshData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.meshData((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("packs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.packs();
            }
        }
        if ("resolveArt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resolveArt();
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false);
                return typedTarget.setImage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("setMeshData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return typedTarget.setMeshData((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("setSheet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.gaming.SpriteSheet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.gaming.SpriteSheet.class}, false);
                return typedTarget.setSheet((java.lang.String) adaptedArgs[0], (com.codename1.gaming.SpriteSheet) adaptedArgs[1]);
            }
        }
        if ("sheet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.sheet((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.gaming.level.AssetDef typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("defaultProperties".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.defaultProperties();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getFps".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFps();
            }
        }
        if ("getFrameCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrameCount();
            }
        }
        if ("getFrameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrameHeight();
            }
        }
        if ("getFrameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrameWidth();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("isMesh".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMesh();
            }
        }
        if ("isSheet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSheet();
            }
        }
        if ("isTile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTile();
            }
        }
        if ("isUnique".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUnique();
            }
        }
        if ("putDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.putDefault((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setKind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetDef.Kind.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetDef.Kind.class}, false);
                return typedTarget.setKind((com.codename1.gaming.level.AssetDef.Kind) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSheet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false);
                return typedTarget.setSheet(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).doubleValue());
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setSize(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSource((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetDef.Type.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetDef.Type.class}, false);
                return typedTarget.setType((com.codename1.gaming.level.AssetDef.Type) adaptedArgs[0]);
            }
        }
        if ("setUnique".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setUnique(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.gaming.level.AssetDef.Kind typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.gaming.level.AssetDef.Type typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.gaming.level.AssetPack typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetDef.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetDef.class}, false);
                return typedTarget.add((com.codename1.gaming.level.AssetDef) adaptedArgs[0]);
            }
        }
        if ("assets".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.assets();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.contains((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.get((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.gaming.level.GameElement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAssetId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAssetId();
            }
        }
        if ("getBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.getBoolean((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return typedTarget.getDouble((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.getInt((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("getLayer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayer();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getRotation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRotation();
            }
        }
        if ("getScaleX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleX();
            }
        }
        if ("getScaleY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleY();
            }
        }
        if ("getScaleZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleZ();
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getString((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
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
        if ("getZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZ();
            }
        }
        if ("hasProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.hasProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("properties".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.properties();
            }
        }
        if ("resolveDef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetCatalog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.AssetCatalog.class}, false);
                return typedTarget.resolveDef((com.codename1.gaming.level.AssetCatalog) adaptedArgs[0]);
            }
        }
        if ("setAssetId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAssetId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setLayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLayer((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.setPosition(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.setPosition(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue());
            }
        }
        if ("setProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setRotation(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("setScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setScale(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setScale(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setX(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setY(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("setZ".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setZ(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.gaming.level.GameLevel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameElement.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameElement.class}, false);
                return typedTarget.addElement((com.codename1.gaming.level.GameElement) adaptedArgs[0]);
            }
        }
        if ("addLayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.Layer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.Layer.class}, false);
                return typedTarget.addLayer((com.codename1.gaming.level.Layer) adaptedArgs[0]);
            }
        }
        if ("elements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.elements();
            }
        }
        if ("getAssetPack".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAssetPack();
            }
        }
        if ("getCols".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCols();
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return typedTarget.getDouble((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("getEyeX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEyeX();
            }
        }
        if ("getEyeY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEyeY();
            }
        }
        if ("getEyeZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEyeZ();
            }
        }
        if ("getFar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFar();
            }
        }
        if ("getFov".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFov();
            }
        }
        if ("getInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.getInt((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("getLayer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLayer((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMode();
            }
        }
        if ("getNear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNear();
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getString((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getTargetX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTargetX();
            }
        }
        if ("getTargetY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTargetY();
            }
        }
        if ("getTargetZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTargetZ();
            }
        }
        if ("getTerrain".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTerrain();
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("getWorld".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWorld();
            }
        }
        if ("is2D".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.is2D();
            }
        }
        if ("is3D".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.is3D();
            }
        }
        if ("isBoard".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBoard();
            }
        }
        if ("isLargeWorld".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLargeWorld();
            }
        }
        if ("layers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.layers();
            }
        }
        if ("lights".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lights();
            }
        }
        if ("props".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.props();
            }
        }
        if ("realizeSprites".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.Scene.class, com.codename1.gaming.level.AssetCatalog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.Scene.class, com.codename1.gaming.level.AssetCatalog.class}, false);
                typedTarget.realizeSprites((com.codename1.gaming.Scene) adaptedArgs[0], (com.codename1.gaming.level.AssetCatalog) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.Scene.class, com.codename1.gaming.level.AssetCatalog.class, com.codename1.gaming.level.IsoProjection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.Scene.class, com.codename1.gaming.level.AssetCatalog.class, com.codename1.gaming.level.IsoProjection.class}, false);
                typedTarget.realizeSprites((com.codename1.gaming.Scene) adaptedArgs[0], (com.codename1.gaming.level.AssetCatalog) adaptedArgs[1], (com.codename1.gaming.level.IsoProjection) adaptedArgs[2]); return null;
            }
        }
        if ("setAssetPack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAssetPack((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setCamera(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue());
            }
        }
        if ("setGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setGrid(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("setLens".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setLens(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameLevel.Mode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameLevel.Mode.class}, false);
                return typedTarget.setMode((com.codename1.gaming.level.GameLevel.Mode) adaptedArgs[0]);
            }
        }
        if ("setTerrain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainGrid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainGrid.class}, false);
                return typedTarget.setTerrain((com.codename1.gaming.level.TerrainGrid) adaptedArgs[0]);
            }
        }
        if ("setWorld".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameWorld.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.GameWorld.class}, false);
                return typedTarget.setWorld((com.codename1.gaming.level.GameWorld) adaptedArgs[0]);
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.gaming.level.GameLevel.Mode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("label".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.label();
            }
        }
        if ("wireName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.gaming.level.GameSceneView typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addModel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.Model.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.Model.class}, false);
                typedTarget.addModel((com.codename1.gaming.Model) adaptedArgs[0]); return null;
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
        if ("addScore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.addScore(toIntValue(adaptedArgs[0]));
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
        if ("frame".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.frame(((Number) adaptedArgs[0]).doubleValue()); return null;
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
        if ("getCamera".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCamera();
            }
        }
        if ("getCatalog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCatalog();
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
        if ("getControls".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getControls();
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
        if ("getFixedTimestep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixedTimestep();
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
        if ("getInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInput();
            }
        }
        if ("getInterpolationAlpha".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInterpolationAlpha();
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
        if ("getLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLevel();
            }
        }
        if ("getLight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLight();
            }
        }
        if ("getLives".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLives();
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
        if ("getPeer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPeer();
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
        if ("getProjection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProjection();
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
        if ("getRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRenderer();
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
        if ("getScene".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScene();
            }
        }
        if ("getScore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScore();
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
        if ("isArcadeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isArcadeBehavior();
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
        if ("isContinuous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isContinuous();
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
        if ("isGameOver".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGameOver();
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
        if ("isPaused".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPaused();
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
        if ("isRunning".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRunning();
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
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
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
        if ("loseLife".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.loseLife();
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
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pause(); return null;
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
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeModel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.Model.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.Model.class}, false);
                typedTarget.removeModel((com.codename1.gaming.Model) adaptedArgs[0]); return null;
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
        if ("requestRender".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestRender(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resume(); return null;
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
        if ("setArcadeBehavior".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setArcadeBehavior(((Boolean) adaptedArgs[0]).booleanValue());
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
        if ("setClearColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setClearColor(toIntValue(adaptedArgs[0])); return null;
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
        if ("setContinuous".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setContinuous(((Boolean) adaptedArgs[0]).booleanValue());
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
        if ("setFixedTimestep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setFixedTimestep(((Number) adaptedArgs[0]).doubleValue()); return null;
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
        if ("setFollowCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setFollowCamera(((Boolean) adaptedArgs[0]).booleanValue());
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
        if ("setLives".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLives(toIntValue(adaptedArgs[0])); return null;
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
        if ("setScore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScore(toIntValue(adaptedArgs[0])); return null;
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
        if ("setup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gpu.GraphicsDevice.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gpu.GraphicsDevice.class}, false);
                typedTarget.setup((com.codename1.gpu.GraphicsDevice) adaptedArgs[0]); return null;
            }
        }
        if ("start".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.start(); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
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

    private static Object invoke9(com.codename1.gaming.level.GameWorld typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.Region.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.Region.class}, false);
                return typedTarget.addRegion((com.codename1.gaming.level.Region) adaptedArgs[0]);
            }
        }
        if ("getActiveRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActiveRegion();
            }
        }
        if ("getKeepRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeepRadius();
            }
        }
        if ("getProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProvider();
            }
        }
        if ("getRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getRegion((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("residentRegions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.residentRegions();
            }
        }
        if ("setActiveRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setActiveRegion((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setKeepRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setKeepRadius(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.RegionProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.RegionProvider.class}, false);
                return typedTarget.setProvider((com.codename1.gaming.level.RegionProvider) adaptedArgs[0]);
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.update(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.gaming.level.IsoProjection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.fit(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("getOriginX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginX();
            }
        }
        if ("getOriginY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginY();
            }
        }
        if ("getTileHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileHeight();
            }
        }
        if ("getTileWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileWidth();
            }
        }
        if ("pick".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.pick(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setOrigin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setOrigin(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setTileSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setTileSize(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("tileCenterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.tileCenterX(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("tileCenterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.tileCenterY(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.gaming.level.Layer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBand();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getParallaxX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParallaxX();
            }
        }
        if ("getParallaxY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParallaxY();
            }
        }
        if ("getTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("isLocked".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLocked();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("putTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return typedTarget.putTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("removeTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.removeTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setBand".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setBand(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setKind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.Layer.Kind.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.Layer.Kind.class}, false);
                return typedTarget.setKind((com.codename1.gaming.level.Layer.Kind) adaptedArgs[0]);
            }
        }
        if ("setLocked".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setLocked(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setParallax".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setParallax(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("tiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.tiles();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.gaming.level.Layer.Kind typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("wireName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wireName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.gaming.level.LevelLight typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAmbientColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAmbientColor();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getDirectionX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirectionX();
            }
        }
        if ("getDirectionY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirectionY();
            }
        }
        if ("getDirectionZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirectionZ();
            }
        }
        if ("setAmbientColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setAmbientColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setDirection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setDirection(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.gaming.level.Material typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getArtId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArtId();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getFriction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFriction();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("isSolid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSolid();
            }
        }
        if ("props".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.props();
            }
        }
        if ("setArtId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setArtId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setFriction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setFriction(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSolid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setSolid(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.gaming.level.Region typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("elements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.elements();
            }
        }
        if ("getDepth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDepth();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getOriginX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginX();
            }
        }
        if ("getOriginZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginZ();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("link".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.link((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("neighbors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.neighbors();
            }
        }
        if ("props".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.props();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setOrigin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.setOrigin(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("setSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.setSpan(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("setTerrain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.StreamingTerrain.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.StreamingTerrain.class}, false);
                return typedTarget.setTerrain((com.codename1.gaming.level.StreamingTerrain) adaptedArgs[0]);
            }
        }
        if ("terrain".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.terrain();
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.gaming.level.StreamingTerrain typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addFeature".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainFeature.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainFeature.class}, false);
                typedTarget.addFeature((com.codename1.gaming.level.TerrainFeature) adaptedArgs[0]); return null;
            }
        }
        if ("chunk".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.chunk(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("features".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.features();
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getCacheSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheSize();
            }
        }
        if ("getCols".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCols();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProvider();
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("hasGround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.hasGround(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("isBounded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBounded();
            }
        }
        if ("loadedChunkCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.loadedChunkCount();
            }
        }
        if ("residentChunks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.residentChunks();
            }
        }
        if ("setCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCacheSize(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.setMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("streamAround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.streamAround(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.gaming.level.StreamingTerrain.MemoryChunkProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("loadChunk".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.loadChunk(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("saveChunk".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainChunk.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainChunk.class}, false);
                typedTarget.saveChunk((com.codename1.gaming.level.TerrainChunk) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.gaming.level.TerrainChunk typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearDirty".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearDirty(); return null;
            }
        }
        if ("features".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.features();
            }
        }
        if ("getChunkX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChunkX();
            }
        }
        if ("getChunkZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChunkZ();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("hasGround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.hasGround(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("isDirty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDirty();
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.setMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("touch".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.touch(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.gaming.level.TerrainFeature typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDepth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDepth();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaterial".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterial();
            }
        }
        if ("getRotation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRotation();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
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
        if ("getZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZ();
            }
        }
        if ("props".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.props();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setMaterial((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.setPosition(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue());
            }
        }
        if ("setRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setRotation(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.setSize(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue());
            }
        }
        if ("setType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setType((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.gaming.level.TerrainGrid typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCellSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCellSize();
            }
        }
        if ("getCols".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCols();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("getWall".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getWall(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("hasGround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.hasGround(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("heights".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.heights();
            }
        }
        if ("materials".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.materials();
            }
        }
        if ("setCellSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setCellSize(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("setGround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.setGround(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                return typedTarget.setHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return typedTarget.setMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("setWall".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                return typedTarget.setWall(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("walls".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.walls();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.gaming.level.TileLayer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCols".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCols();
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("getSheet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSheet();
            }
        }
        if ("getTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getTileSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTileSize();
            }
        }
        if ("setTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setTile(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("toScene".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.Scene.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.Scene.class, java.lang.Integer.class}, false);
                typedTarget.toScene((com.codename1.gaming.Scene) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.gaming.level.ChunkProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("loadChunk".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.loadChunk(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("saveChunk".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainChunk.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainChunk.class}, false);
                typedTarget.saveChunk((com.codename1.gaming.level.TerrainChunk) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.gaming.level.RegionProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("loadRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.loadRegion((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("saveRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.Region.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.Region.class}, false);
                typedTarget.saveRegion((com.codename1.gaming.level.Region) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.gaming.level.Terrain typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addFeature".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainFeature.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gaming.level.TerrainFeature.class}, false);
                typedTarget.addFeature((com.codename1.gaming.level.TerrainFeature) adaptedArgs[0]); return null;
            }
        }
        if ("features".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.features();
            }
        }
        if ("getCols".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCols();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("hasGround".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.hasGround(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("isBounded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBounded();
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("setMaterial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.setMaterial(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.gaming.level.AssetDef.Kind.class) return getStaticField0(name);
        if (type == com.codename1.gaming.level.AssetDef.Type.class) return getStaticField1(name);
        if (type == com.codename1.gaming.level.GameLevel.Mode.class) return getStaticField2(name);
        if (type == com.codename1.gaming.level.GameSceneView.class) return getStaticField3(name);
        if (type == com.codename1.gaming.level.Layer.Kind.class) return getStaticField4(name);
        if (type == com.codename1.gaming.level.MaterialRegistry.class) return getStaticField5(name);
        if (type == com.codename1.gaming.level.StreamingTerrain.class) return getStaticField6(name);
        if (type == com.codename1.gaming.level.Terrain.class) return getStaticField7(name);
        if (type == com.codename1.gaming.level.TerrainChunk.class) return getStaticField8(name);
        if (type == com.codename1.gaming.level.TerrainFeature.class) return getStaticField9(name);
        if (type == com.codename1.gaming.level.TerrainGrid.class) return getStaticField10(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ACTOR".equals(name)) return com.codename1.gaming.level.AssetDef.Kind.ACTOR;
        if ("TILE".equals(name)) return com.codename1.gaming.level.AssetDef.Kind.TILE;
        throw unsupportedStaticField(com.codename1.gaming.level.AssetDef.Kind.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("IMAGE".equals(name)) return com.codename1.gaming.level.AssetDef.Type.IMAGE;
        if ("MESH".equals(name)) return com.codename1.gaming.level.AssetDef.Type.MESH;
        if ("SHEET".equals(name)) return com.codename1.gaming.level.AssetDef.Type.SHEET;
        throw unsupportedStaticField(com.codename1.gaming.level.AssetDef.Type.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("BOARD".equals(name)) return com.codename1.gaming.level.GameLevel.Mode.BOARD;
        if ("THREE_D".equals(name)) return com.codename1.gaming.level.GameLevel.Mode.THREE_D;
        if ("TWO_D".equals(name)) return com.codename1.gaming.level.GameLevel.Mode.TWO_D;
        throw unsupportedStaticField(com.codename1.gaming.level.GameLevel.Mode.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("BASELINE".equals(name)) return com.codename1.gaming.level.GameSceneView.BASELINE;
        if ("BOTTOM".equals(name)) return com.codename1.gaming.level.GameSceneView.BOTTOM;
        if ("BRB_CENTER_OFFSET".equals(name)) return com.codename1.gaming.level.GameSceneView.BRB_CENTER_OFFSET;
        if ("BRB_CONSTANT_ASCENT".equals(name)) return com.codename1.gaming.level.GameSceneView.BRB_CONSTANT_ASCENT;
        if ("BRB_CONSTANT_DESCENT".equals(name)) return com.codename1.gaming.level.GameSceneView.BRB_CONSTANT_DESCENT;
        if ("BRB_OTHER".equals(name)) return com.codename1.gaming.level.GameSceneView.BRB_OTHER;
        if ("CENTER".equals(name)) return com.codename1.gaming.level.GameSceneView.CENTER;
        if ("CROSSHAIR_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.CROSSHAIR_CURSOR;
        if ("DEFAULT_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.DEFAULT_CURSOR;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_X".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_IMMEDIATELY_DRAG_X;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_XY".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_IMMEDIATELY_DRAG_XY;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_Y".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_IMMEDIATELY_DRAG_Y;
        if ("DRAG_REGION_LIKELY_DRAG_X".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_LIKELY_DRAG_X;
        if ("DRAG_REGION_LIKELY_DRAG_XY".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_LIKELY_DRAG_XY;
        if ("DRAG_REGION_LIKELY_DRAG_Y".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_LIKELY_DRAG_Y;
        if ("DRAG_REGION_NOT_DRAGGABLE".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_NOT_DRAGGABLE;
        if ("DRAG_REGION_POSSIBLE_DRAG_X".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_POSSIBLE_DRAG_X;
        if ("DRAG_REGION_POSSIBLE_DRAG_XY".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_POSSIBLE_DRAG_XY;
        if ("DRAG_REGION_POSSIBLE_DRAG_Y".equals(name)) return com.codename1.gaming.level.GameSceneView.DRAG_REGION_POSSIBLE_DRAG_Y;
        if ("E_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.E_RESIZE_CURSOR;
        if ("HAND_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.HAND_CURSOR;
        if ("LEFT".equals(name)) return com.codename1.gaming.level.GameSceneView.LEFT;
        if ("MOVE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.MOVE_CURSOR;
        if ("NE_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.NE_RESIZE_CURSOR;
        if ("NW_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.NW_RESIZE_CURSOR;
        if ("N_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.N_RESIZE_CURSOR;
        if ("RIGHT".equals(name)) return com.codename1.gaming.level.GameSceneView.RIGHT;
        if ("SE_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.SE_RESIZE_CURSOR;
        if ("SW_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.SW_RESIZE_CURSOR;
        if ("S_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.S_RESIZE_CURSOR;
        if ("TEXT_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.TEXT_CURSOR;
        if ("TOP".equals(name)) return com.codename1.gaming.level.GameSceneView.TOP;
        if ("WAIT_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.WAIT_CURSOR;
        if ("W_RESIZE_CURSOR".equals(name)) return com.codename1.gaming.level.GameSceneView.W_RESIZE_CURSOR;
        throw unsupportedStaticField(com.codename1.gaming.level.GameSceneView.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("ENTITY".equals(name)) return com.codename1.gaming.level.Layer.Kind.ENTITY;
        if ("MODEL".equals(name)) return com.codename1.gaming.level.Layer.Kind.MODEL;
        if ("TILE".equals(name)) return com.codename1.gaming.level.Layer.Kind.TILE;
        throw unsupportedStaticField(com.codename1.gaming.level.Layer.Kind.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("DIRT".equals(name)) return com.codename1.gaming.level.MaterialRegistry.DIRT;
        if ("GRASS".equals(name)) return com.codename1.gaming.level.MaterialRegistry.GRASS;
        if ("ROAD".equals(name)) return com.codename1.gaming.level.MaterialRegistry.ROAD;
        if ("SAND".equals(name)) return com.codename1.gaming.level.MaterialRegistry.SAND;
        if ("STONE".equals(name)) return com.codename1.gaming.level.MaterialRegistry.STONE;
        if ("WATER".equals(name)) return com.codename1.gaming.level.MaterialRegistry.WATER;
        throw unsupportedStaticField(com.codename1.gaming.level.MaterialRegistry.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("NO_GROUND".equals(name)) return com.codename1.gaming.level.StreamingTerrain.NO_GROUND;
        throw unsupportedStaticField(com.codename1.gaming.level.StreamingTerrain.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("NO_GROUND".equals(name)) return com.codename1.gaming.level.Terrain.NO_GROUND;
        throw unsupportedStaticField(com.codename1.gaming.level.Terrain.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("SIZE".equals(name)) return com.codename1.gaming.level.TerrainChunk.SIZE;
        throw unsupportedStaticField(com.codename1.gaming.level.TerrainChunk.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("TYPE_PLATFORM".equals(name)) return com.codename1.gaming.level.TerrainFeature.TYPE_PLATFORM;
        if ("TYPE_RAMP".equals(name)) return com.codename1.gaming.level.TerrainFeature.TYPE_RAMP;
        if ("TYPE_WALL".equals(name)) return com.codename1.gaming.level.TerrainFeature.TYPE_WALL;
        throw unsupportedStaticField(com.codename1.gaming.level.TerrainFeature.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
        if ("NO_GROUND".equals(name)) return com.codename1.gaming.level.TerrainGrid.NO_GROUND;
        throw unsupportedStaticField(com.codename1.gaming.level.TerrainGrid.class, name);
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
