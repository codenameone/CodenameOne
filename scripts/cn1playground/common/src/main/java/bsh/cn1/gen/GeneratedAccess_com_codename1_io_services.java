package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_services {
    private GeneratedAccess_com_codename1_io_services() {
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
        if ("CachedData".equals(simpleName)) {
            return com.codename1.io.services.CachedData.class;
        }
        if ("CachedDataService".equals(simpleName)) {
            return com.codename1.io.services.CachedDataService.class;
        }
        if ("ImageDownloadService".equals(simpleName)) {
            return com.codename1.io.services.ImageDownloadService.class;
        }
        if ("RSSService".equals(simpleName)) {
            return com.codename1.io.services.RSSService.class;
        }
        if ("TwitterRESTService".equals(simpleName)) {
            return com.codename1.io.services.TwitterRESTService.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.services.ImageDownloadService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class}, false);
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.io.services.RSSService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.services.RSSService((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.io.services.RSSService((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.io.services.RSSService((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if (type == com.codename1.io.services.TwitterRESTService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.services.TwitterRESTService((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.io.services.TwitterRESTService((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.io.services.TwitterRESTService((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.services.CachedDataService.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.services.ImageDownloadService.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.io.services.TwitterRESTService.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("register".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.services.CachedDataService.register(); return null;
            }
        }
        if ("updateData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.services.CachedData.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.services.CachedData.class, com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.io.services.CachedDataService.updateData((com.codename1.io.services.CachedData) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.services.CachedDataService.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("addErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.io.services.ImageDownloadService.addErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("createImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (com.codename1.ui.geom.Dimension) adaptedArgs[5]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (com.codename1.ui.geom.Dimension) adaptedArgs[5], ((Number) adaptedArgs[6]).byteValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (com.codename1.ui.Image) adaptedArgs[5], ((Number) adaptedArgs[6]).byteValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, com.codename1.ui.list.ListModel.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, com.codename1.ui.list.ListModel.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.list.ListModel) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue(), (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (com.codename1.ui.Image) adaptedArgs[6], ((Number) adaptedArgs[7]).byteValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ui.geom.Dimension) adaptedArgs[3]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class, java.lang.Boolean.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ui.geom.Dimension) adaptedArgs[3], ((Number) adaptedArgs[4]).byteValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], ((Number) adaptedArgs[4]).byteValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (com.codename1.ui.geom.Dimension) adaptedArgs[5]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (com.codename1.ui.geom.Dimension) adaptedArgs[5], ((Number) adaptedArgs[6]).byteValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (com.codename1.ui.Image) adaptedArgs[5], ((Number) adaptedArgs[6]).byteValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, com.codename1.ui.list.ListModel.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, com.codename1.ui.list.ListModel.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false);
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.list.ListModel) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue(), (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (com.codename1.ui.Image) adaptedArgs[6], ((Number) adaptedArgs[7]).byteValue()); return null;
            }
        }
        if ("getDefaultTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.services.ImageDownloadService.getDefaultTimeout();
            }
        }
        if ("isAlwaysRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.services.ImageDownloadService.isAlwaysRevalidate();
            }
        }
        if ("isDefaultMaintainAspectRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.services.ImageDownloadService.isDefaultMaintainAspectRatio();
            }
        }
        if ("isFastScale".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.services.ImageDownloadService.isFastScale();
            }
        }
        if ("removeErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.io.services.ImageDownloadService.removeErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysRevalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.services.ImageDownloadService.setAlwaysRevalidate(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultMaintainAspectRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.services.ImageDownloadService.setDefaultMaintainAspectRatio(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.io.services.ImageDownloadService.setDefaultTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFastScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.services.ImageDownloadService.setFastScale(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.services.ImageDownloadService.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("initToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.services.TwitterRESTService.initToken((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.services.TwitterRESTService.setToken((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.services.TwitterRESTService.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.services.CachedData) {
            try {
                return invoke0((com.codename1.io.services.CachedData) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.services.CachedDataService) {
            try {
                return invoke1((com.codename1.io.services.CachedDataService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.services.ImageDownloadService) {
            try {
                return invoke2((com.codename1.io.services.ImageDownloadService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.services.RSSService) {
            try {
                return invoke3((com.codename1.io.services.RSSService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.services.TwitterRESTService) {
            try {
                return invoke4((com.codename1.io.services.TwitterRESTService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.services.CachedData typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getData();
            }
        }
        if ("getObjectId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObjectId();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("setData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setData((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.services.CachedDataService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArguments((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addRequestHeader((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.onRedirect((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeArgument((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCheckSSLCertificates(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setChunkedStreamingMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setContentType((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCookiesEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationStorage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDuplicateSupported(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFailSilently(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFollowRedirects(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setHttpMethod((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInsecure(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPost(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPriority(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadResponseForErrors(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setReadTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false);
                typedTarget.setRequestBody((com.codename1.io.Data) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRequestBody((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSilentRetryCount(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserAgent((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWriteRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.services.ImageDownloadService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArguments((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addRequestHeader((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getResult".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResult();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDownloadToStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDownloadToStyles();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInsecure();
            }
        }
        if ("isMaintainAspectRatio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMaintainAspectRatio();
            }
        }
        if ("isPost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.onRedirect((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeArgument((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCheckSSLCertificates(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setChunkedStreamingMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setContentType((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCookiesEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationStorage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setDownloadToStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDownloadToStyles(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDuplicateSupported(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFailSilently(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFollowRedirects(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setHttpMethod((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInsecure(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMaintainAspectRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setMaintainAspectRatio(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPost(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPriority(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadResponseForErrors(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setReadTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false);
                typedTarget.setRequestBody((com.codename1.io.Data) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRequestBody((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSilentRetryCount(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserAgent((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWriteRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.services.RSSService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArguments((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addRequestHeader((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getIconPlaceholder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconPlaceholder();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getResults".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResults();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hasMore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasMore();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isCreatePlainTextDetails".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCreatePlainTextDetails();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.onRedirect((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parsingError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.parsingError(((Number) adaptedArgs[0]).intValue(), (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4]);
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeArgument((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCheckSSLCertificates(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setChunkedStreamingMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setContentType((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCookiesEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCreatePlainTextDetails".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCreatePlainTextDetails(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationStorage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDuplicateSupported(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFailSilently(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFollowRedirects(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setHttpMethod((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIconPlaceholder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIconPlaceholder((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInsecure(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPost(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPriority(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadResponseForErrors(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setReadTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false);
                typedTarget.setRequestBody((com.codename1.io.Data) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRequestBody((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSilentRetryCount(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserAgent((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWriteRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.services.TwitterRESTService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArguments((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addRequestHeader((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getIdStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdStr();
            }
        }
        if ("getParseTree".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParseTree();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getStatus(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getStatusesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatusesCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.onRedirect((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeArgument((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCheckSSLCertificates(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setChunkedStreamingMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setContentType((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCookiesEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationStorage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDuplicateSupported(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFailSilently(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFollowRedirects(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setHttpMethod((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInsecure(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPost(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPriority(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadResponseForErrors(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setReadTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false);
                typedTarget.setRequestBody((com.codename1.io.Data) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRequestBody((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSilentRetryCount(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserAgent((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWriteRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.services.TwitterRESTService.class) {
            if ("METHOD_TWEETS".equals(name)) return com.codename1.io.services.TwitterRESTService.METHOD_TWEETS;
            if ("METHOD_USER_TIMELINE".equals(name)) return com.codename1.io.services.TwitterRESTService.METHOD_USER_TIMELINE;
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
