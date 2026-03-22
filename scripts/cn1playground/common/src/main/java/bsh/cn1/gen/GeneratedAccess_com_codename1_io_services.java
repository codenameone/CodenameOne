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
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io.services -> com.codename1.io.services.CachedData");
            }
            return com.codename1.io.services.CachedData.class;
        }
        if ("CachedDataService".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io.services -> com.codename1.io.services.CachedDataService");
            }
            return com.codename1.io.services.CachedDataService.class;
        }
        if ("ImageDownloadService".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io.services -> com.codename1.io.services.ImageDownloadService");
            }
            return com.codename1.io.services.ImageDownloadService.class;
        }
        if ("RSSService".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io.services -> com.codename1.io.services.RSSService");
            }
            return com.codename1.io.services.RSSService.class;
        }
        if ("TwitterRESTService".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io.services -> com.codename1.io.services.TwitterRESTService");
            }
            return com.codename1.io.services.TwitterRESTService.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.services.ImageDownloadService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class}, false)) {
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) safeArgs[0], (com.codename1.ui.Label) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                return new com.codename1.io.services.ImageDownloadService((java.lang.String) safeArgs[0], (com.codename1.ui.List) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3]);
            }
        }
        if (type == com.codename1.io.services.RSSService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.services.RSSService((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return new com.codename1.io.services.RSSService((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.io.services.RSSService((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if (type == com.codename1.io.services.TwitterRESTService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.services.TwitterRESTService((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.io.services.TwitterRESTService((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.io.services.TwitterRESTService((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.services.CachedDataService.register(); return null;
            }
        }
        if ("updateData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.services.CachedData.class, com.codename1.ui.events.ActionListener.class}, false)) {
                com.codename1.io.services.CachedDataService.updateData((com.codename1.io.services.CachedData) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.services.CachedDataService.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("addErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                com.codename1.io.services.ImageDownloadService.addErrorListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("createImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("createImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.ui.List) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (com.codename1.ui.geom.Dimension) safeArgs[5]); return null;
            }
        }
        if ("createImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (com.codename1.ui.Image) safeArgs[5], ((Number) safeArgs[6]).byteValue()); return null;
            }
        }
        if ("createImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (com.codename1.ui.geom.Dimension) safeArgs[5], ((Number) safeArgs[6]).byteValue()); return null;
            }
        }
        if ("createImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, com.codename1.ui.list.ListModel.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.list.ListModel) safeArgs[2], ((Number) safeArgs[3]).intValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (com.codename1.ui.Image) safeArgs[6], ((Number) safeArgs[7]).byteValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.Label) safeArgs[1], (java.lang.String) safeArgs[2], (com.codename1.ui.geom.Dimension) safeArgs[3]); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1], (java.lang.String) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.Label) safeArgs[1], (java.lang.String) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], ((Number) safeArgs[4]).byteValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.Label) safeArgs[1], (java.lang.String) safeArgs[2], (com.codename1.ui.geom.Dimension) safeArgs[3], ((Number) safeArgs[4]).byteValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.List.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.List) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (com.codename1.ui.geom.Dimension) safeArgs[5]); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (com.codename1.ui.Image) safeArgs[5], ((Number) safeArgs[6]).byteValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (com.codename1.ui.geom.Dimension) safeArgs[5], ((Number) safeArgs[6]).byteValue()); return null;
            }
        }
        if ("createImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, com.codename1.ui.list.ListModel.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                com.codename1.io.services.ImageDownloadService.createImageToStorage((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.list.ListModel) safeArgs[2], ((Number) safeArgs[3]).intValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (com.codename1.ui.Image) safeArgs[6], ((Number) safeArgs[7]).byteValue()); return null;
            }
        }
        if ("getDefaultTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.services.ImageDownloadService.getDefaultTimeout();
            }
        }
        if ("isAlwaysRevalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.services.ImageDownloadService.isAlwaysRevalidate();
            }
        }
        if ("isDefaultMaintainAspectRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.services.ImageDownloadService.isDefaultMaintainAspectRatio();
            }
        }
        if ("isFastScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.services.ImageDownloadService.isFastScale();
            }
        }
        if ("removeErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                com.codename1.io.services.ImageDownloadService.removeErrorListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("setAlwaysRevalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.services.ImageDownloadService.setAlwaysRevalidate(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultMaintainAspectRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.services.ImageDownloadService.setDefaultMaintainAspectRatio(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.io.services.ImageDownloadService.setDefaultTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFastScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.services.ImageDownloadService.setFastScale(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.services.ImageDownloadService.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("initToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.services.TwitterRESTService.initToken((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("setToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.services.TwitterRESTService.setToken((java.lang.String) safeArgs[0]); return null;
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getData();
            }
        }
        if ("getObjectId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getObjectId();
            }
        }
        if ("getUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrl();
            }
        }
        if ("getVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVersion();
            }
        }
        if ("setData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setData((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.services.CachedDataService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArguments((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addRequestHeader((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                typedTarget.ioStreamUpdate((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.onRedirect((java.lang.String) safeArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.removeArgument((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) safeArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCheckSSLCertificates(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setChunkedStreamingMode(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setContentType((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCookiesEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationFile((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationStorage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDuplicateSupported(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFailSilently(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFollowRedirects(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setHttpMethod((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInsecure(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPost(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPriority(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadResponseForErrors(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setReadTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                typedTarget.setRequestBody((com.codename1.io.Data) safeArgs[0]); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRequestBody((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSilentRetryCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUserAgent((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setWriteRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.services.ImageDownloadService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArguments((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addRequestHeader((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResult();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                typedTarget.ioStreamUpdate((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDownloadToStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDownloadToStyles();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInsecure();
            }
        }
        if ("isMaintainAspectRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMaintainAspectRatio();
            }
        }
        if ("isPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.onRedirect((java.lang.String) safeArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.removeArgument((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) safeArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCheckSSLCertificates(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setChunkedStreamingMode(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setContentType((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCookiesEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationFile((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationStorage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setDownloadToStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDownloadToStyles(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDuplicateSupported(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFailSilently(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFollowRedirects(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setHttpMethod((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInsecure(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMaintainAspectRatio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setMaintainAspectRatio(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPost(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPriority(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadResponseForErrors(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setReadTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                typedTarget.setRequestBody((com.codename1.io.Data) safeArgs[0]); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRequestBody((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSilentRetryCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUserAgent((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setWriteRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.services.RSSService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArguments((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addRequestHeader((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getIconPlaceholder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconPlaceholder();
            }
        }
        if ("getPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getResults".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResults();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hasMore".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasMore();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                typedTarget.ioStreamUpdate((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isCreatePlainTextDetails".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCreatePlainTextDetails();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.onRedirect((java.lang.String) safeArgs[0]);
            }
        }
        if ("parsingError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.parsingError(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4]);
            }
        }
        if ("pause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.removeArgument((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) safeArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCheckSSLCertificates(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setChunkedStreamingMode(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setContentType((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCookiesEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCreatePlainTextDetails".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCreatePlainTextDetails(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationFile((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationStorage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDuplicateSupported(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFailSilently(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFollowRedirects(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setHttpMethod((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setIconPlaceholder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setIconPlaceholder((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInsecure(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPost(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPriority(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadResponseForErrors(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setReadTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                typedTarget.setRequestBody((com.codename1.io.Data) safeArgs[0]); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRequestBody((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSilentRetryCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUserAgent((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setWriteRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.services.TwitterRESTService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArguments((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addRequestHeader((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getIdStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIdStr();
            }
        }
        if ("getParseTree".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getParseTree();
            }
        }
        if ("getPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getStatus(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getStatusesCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStatusesCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                typedTarget.ioStreamUpdate((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.onRedirect((java.lang.String) safeArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.removeArgument((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) safeArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCheckSSLCertificates(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setChunkedStreamingMode(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setContentType((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCookiesEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationFile((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationStorage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDuplicateSupported(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFailSilently(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFollowRedirects(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setHttpMethod((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInsecure(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPost(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPriority(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadResponseForErrors(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setReadTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                typedTarget.setRequestBody((com.codename1.io.Data) safeArgs[0]); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRequestBody((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSilentRetryCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUserAgent((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setWriteRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
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
