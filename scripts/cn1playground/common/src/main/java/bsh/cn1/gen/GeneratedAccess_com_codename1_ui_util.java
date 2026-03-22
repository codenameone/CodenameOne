package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_util {
    private GeneratedAccess_com_codename1_ui_util() {
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
        if ("Effects".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.Effects");
            }
            return com.codename1.ui.util.Effects.class;
        }
        if ("EmbeddedContainer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.EmbeddedContainer");
            }
            return com.codename1.ui.util.EmbeddedContainer.class;
        }
        if ("EventDispatcher".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.EventDispatcher");
            }
            return com.codename1.ui.util.EventDispatcher.class;
        }
        if ("GlassTutorial".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.GlassTutorial");
            }
            return com.codename1.ui.util.GlassTutorial.class;
        }
        if ("ImageIO".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.ImageIO");
            }
            return com.codename1.ui.util.ImageIO.class;
        }
        if ("MutableResouce".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.MutableResouce");
            }
            return com.codename1.ui.util.MutableResouce.class;
        }
        if ("MutableResource".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.MutableResource");
            }
            return com.codename1.ui.util.MutableResource.class;
        }
        if ("Resources".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.Resources");
            }
            return com.codename1.ui.util.Resources.class;
        }
        if ("SwipeBackSupport".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.SwipeBackSupport");
            }
            return com.codename1.ui.util.SwipeBackSupport.class;
        }
        if ("UIBuilder".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.UIBuilder");
            }
            return com.codename1.ui.util.UIBuilder.class;
        }
        if ("UITimer".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.UITimer");
            }
            return com.codename1.ui.util.UITimer.class;
        }
        if ("WeakHashMap".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.util -> com.codename1.ui.util.WeakHashMap");
            }
            return com.codename1.ui.util.WeakHashMap.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.util.EmbeddedContainer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.util.EmbeddedContainer();
            }
        }
        if (type == com.codename1.ui.util.MutableResouce.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.util.MutableResouce();
            }
        }
        if (type == com.codename1.ui.util.MutableResource.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.util.MutableResource();
            }
        }
        if (type == com.codename1.ui.util.UITimer.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                return new com.codename1.ui.util.UITimer((java.lang.Runnable) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.util.Effects.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.util.EventDispatcher.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.util.ImageIO.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ui.util.Resources.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ui.util.SwipeBackSupport.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.ui.util.UIBuilder.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.ui.util.UITimer.class) return invokeStatic6(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("dropshadow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                return com.codename1.ui.util.Effects.dropshadow((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).floatValue());
            }
        }
        if ("dropshadow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.util.Effects.dropshadow((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue());
            }
        }
        if ("gaussianBlurImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class}, false)) {
                return com.codename1.ui.util.Effects.gaussianBlurImage((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                com.codename1.ui.util.Effects.growShrink((com.codename1.ui.Component) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("isGaussianBlurSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.Effects.isGaussianBlurSupported();
            }
        }
        if ("reflectionImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.util.Effects.reflectionImage((com.codename1.ui.Image) safeArgs[0]);
            }
        }
        if ("reflectionImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.util.Effects.reflectionImage((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("reflectionImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.util.Effects.reflectionImage((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("squareShadow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                return com.codename1.ui.util.Effects.squareShadow(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).floatValue());
            }
        }
        if ("verticalPerspective".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                return com.codename1.ui.util.Effects.verticalPerspective((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.Effects.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("setFireStyleEventsOnNonEDT".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.util.EventDispatcher.setFireStyleEventsOnNonEDT(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.EventDispatcher.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getImageIO".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.ImageIO.getImageIO();
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.ImageIO.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getGlobalResources".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.Resources.getGlobalResources();
            }
        }
        if ("getSystemResource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.Resources.getSystemResource();
            }
        }
        if ("isEnableMediaQueries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.Resources.isEnableMediaQueries();
            }
        }
        if ("isFailOnMissingTruetype".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.Resources.isFailOnMissingTruetype();
            }
        }
        if ("open".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.util.Resources.open((java.lang.String) safeArgs[0]);
            }
        }
        if ("open".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.util.Resources.open((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("openLayered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.util.Resources.openLayered((java.lang.String) safeArgs[0]);
            }
        }
        if ("openLayered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.util.Resources.openLayered((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("setEnableMediaQueries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.util.Resources.setEnableMediaQueries(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailOnMissingTruetype".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.util.Resources.setFailOnMissingTruetype(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGlobalResources".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                com.codename1.ui.util.Resources.setGlobalResources((com.codename1.ui.util.Resources) safeArgs[0]); return null;
            }
        }
        if ("setPassword".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.ui.util.Resources.setPassword((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setRuntimeMultiImageEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.util.Resources.setRuntimeMultiImageEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.Resources.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("bindBack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.LazyValue.class}, false)) {
                com.codename1.ui.util.SwipeBackSupport.bindBack((com.codename1.util.LazyValue) safeArgs[0]); return null;
            }
        }
        if ("bindBack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class, com.codename1.util.LazyValue.class}, false)) {
                com.codename1.ui.util.SwipeBackSupport.bindBack((com.codename1.ui.Form) safeArgs[0], (com.codename1.util.LazyValue) safeArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.SwipeBackSupport.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("isBlockAnalytics".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.util.UIBuilder.isBlockAnalytics();
            }
        }
        if ("registerCustomComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class}, false)) {
                com.codename1.ui.util.UIBuilder.registerCustomComponent((java.lang.String) safeArgs[0], (java.lang.Class) safeArgs[1]); return null;
            }
        }
        if ("setBlockAnalytics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.util.UIBuilder.setBlockAnalytics(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.UIBuilder.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("timer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, java.lang.Runnable.class}, false)) {
                return com.codename1.ui.util.UITimer.timer(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), (java.lang.Runnable) safeArgs[2]);
            }
        }
        if ("timer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, com.codename1.ui.Form.class, java.lang.Runnable.class}, false)) {
                return com.codename1.ui.util.UITimer.timer(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), (com.codename1.ui.Form) safeArgs[2], (java.lang.Runnable) safeArgs[3]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.util.UITimer.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.util.MutableResouce) {
            try {
                return invoke0((com.codename1.ui.util.MutableResouce) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.MutableResource) {
            try {
                return invoke1((com.codename1.ui.util.MutableResource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.EmbeddedContainer) {
            try {
                return invoke2((com.codename1.ui.util.EmbeddedContainer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.EventDispatcher) {
            try {
                return invoke3((com.codename1.ui.util.EventDispatcher) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.GlassTutorial) {
            try {
                return invoke4((com.codename1.ui.util.GlassTutorial) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.ImageIO) {
            try {
                return invoke5((com.codename1.ui.util.ImageIO) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.Resources) {
            try {
                return invoke6((com.codename1.ui.util.Resources) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.UIBuilder) {
            try {
                return invoke7((com.codename1.ui.util.UIBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.UITimer) {
            try {
                return invoke8((com.codename1.ui.util.UITimer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.util.WeakHashMap) {
            try {
                return invoke9((com.codename1.ui.util.WeakHashMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.util.MutableResouce typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsResource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.containsResource((java.lang.String) safeArgs[0]);
            }
        }
        if ("getDataByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getDataByteArray((java.lang.String) safeArgs[0]);
            }
        }
        if ("getDataResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataResourceNames();
            }
        }
        if ("getFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getFont((java.lang.String) safeArgs[0]);
            }
        }
        if ("getFontResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFontResourceNames();
            }
        }
        if ("getImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("getImageResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getImageResourceNames();
            }
        }
        if ("getL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.getL10N((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getL10NResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getL10NResourceNames();
            }
        }
        if ("getMajorVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMajorVersion();
            }
        }
        if ("getMetaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMetaData();
            }
        }
        if ("getMinorVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinorVersion();
            }
        }
        if ("getResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResourceNames();
            }
        }
        if ("getTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThemeResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThemeResourceNames();
            }
        }
        if ("getUIResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUIResourceNames();
            }
        }
        if ("isAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isAnimation((java.lang.String) safeArgs[0]);
            }
        }
        if ("isData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isData((java.lang.String) safeArgs[0]);
            }
        }
        if ("isFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isFont((java.lang.String) safeArgs[0]);
            }
        }
        if ("isImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("isL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isL10N((java.lang.String) safeArgs[0]);
            }
        }
        if ("isModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isModified();
            }
        }
        if ("isTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("isUI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isUI((java.lang.String) safeArgs[0]);
            }
        }
        if ("l10NLocaleSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.l10NLocaleSet((java.lang.String) safeArgs[0]);
            }
        }
        if ("listL10NLocales".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.listL10NLocales((java.lang.String) safeArgs[0]);
            }
        }
        if ("setData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.setData((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setImage((java.lang.String) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setIndexedImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setIndexedImage((java.lang.String) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Hashtable.class}, false)) {
                typedTarget.setL10N((java.lang.String) safeArgs[0], (java.util.Hashtable) safeArgs[1]); return null;
            }
        }
        if ("setSVG".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setSVG((java.lang.String) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Hashtable.class}, false)) {
                typedTarget.setTheme((java.lang.String) safeArgs[0], (java.util.Hashtable) safeArgs[1]); return null;
            }
        }
        if ("setThemeProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.setThemeProperty((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.Object) safeArgs[2]); return null;
            }
        }
        if ("setTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.animations.Timeline.class}, false)) {
                typedTarget.setTimeline((java.lang.String) safeArgs[0], (com.codename1.ui.animations.Timeline) safeArgs[1]); return null;
            }
        }
        if ("setUi".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.setUi((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.util.MutableResource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsResource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.containsResource((java.lang.String) safeArgs[0]);
            }
        }
        if ("getDataByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getDataByteArray((java.lang.String) safeArgs[0]);
            }
        }
        if ("getDataResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataResourceNames();
            }
        }
        if ("getFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getFont((java.lang.String) safeArgs[0]);
            }
        }
        if ("getFontResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFontResourceNames();
            }
        }
        if ("getImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("getImageResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getImageResourceNames();
            }
        }
        if ("getL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.getL10N((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getL10NResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getL10NResourceNames();
            }
        }
        if ("getMajorVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMajorVersion();
            }
        }
        if ("getMetaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMetaData();
            }
        }
        if ("getMinorVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinorVersion();
            }
        }
        if ("getResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResourceNames();
            }
        }
        if ("getTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThemeResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThemeResourceNames();
            }
        }
        if ("getUIResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUIResourceNames();
            }
        }
        if ("isAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isAnimation((java.lang.String) safeArgs[0]);
            }
        }
        if ("isData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isData((java.lang.String) safeArgs[0]);
            }
        }
        if ("isFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isFont((java.lang.String) safeArgs[0]);
            }
        }
        if ("isImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("isL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isL10N((java.lang.String) safeArgs[0]);
            }
        }
        if ("isModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isModified();
            }
        }
        if ("isTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("isUI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isUI((java.lang.String) safeArgs[0]);
            }
        }
        if ("l10NLocaleSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.l10NLocaleSet((java.lang.String) safeArgs[0]);
            }
        }
        if ("listL10NLocales".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.listL10NLocales((java.lang.String) safeArgs[0]);
            }
        }
        if ("setData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.setData((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setImage((java.lang.String) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setIndexedImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setIndexedImage((java.lang.String) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Hashtable.class}, false)) {
                typedTarget.setL10N((java.lang.String) safeArgs[0], (java.util.Hashtable) safeArgs[1]); return null;
            }
        }
        if ("setSVG".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setSVG((java.lang.String) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Hashtable.class}, false)) {
                typedTarget.setTheme((java.lang.String) safeArgs[0], (java.util.Hashtable) safeArgs[1]); return null;
            }
        }
        if ("setThemeProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.setThemeProperty((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.Object) safeArgs[2]); return null;
            }
        }
        if ("setTimeline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.animations.Timeline.class}, false)) {
                typedTarget.setTimeline((java.lang.String) safeArgs[0], (com.codename1.ui.animations.Timeline) safeArgs[1]); return null;
            }
        }
        if ("setUi".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.setUi((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.util.EmbeddedContainer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                return typedTarget.add((com.codename1.ui.Component) safeArgs[0]);
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                return typedTarget.add((com.codename1.ui.Image) safeArgs[0]);
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.add((java.lang.String) safeArgs[0]);
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                return typedTarget.add((java.lang.Object) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]);
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                return typedTarget.add((java.lang.Object) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]);
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                return typedTarget.add((java.lang.Object) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) safeArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.addComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.addComponent(((Number) safeArgs[0]).intValue(), (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.addComponent((java.lang.Object) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.addComponent(((Number) safeArgs[0]).intValue(), (java.lang.Object) safeArgs[1], (com.codename1.ui.Component) safeArgs[2]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) safeArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                typedTarget.addPullToRefresh((java.lang.Runnable) safeArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) safeArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.animateHierarchy(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.animateHierarchyAndWait(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.animateHierarchyFade(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.animateHierarchyFadeAndWait(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.animateLayout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.animateLayoutAndWait(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.animateLayoutFade(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.animateLayoutFadeAndWait(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                typedTarget.animateUnlayout(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (java.lang.Runnable) safeArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.animateUnlayoutAndWait(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.announceForAccessibility((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.applyRTL(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                typedTarget.bindProperty((java.lang.String) safeArgs[0], (com.codename1.cloud.BindTarget) safeArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                return typedTarget.contains((com.codename1.ui.Component) safeArgs[0]);
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.contains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.containsOrOwns(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.createAnimateHierarchy(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.createAnimateHierarchyFade(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.createAnimateLayout(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.createAnimateLayoutFade(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.createAnimateLayoutFadeAndWait(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                return typedTarget.createAnimateUnlayout(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (java.lang.Runnable) safeArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.animations.Transition) safeArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return typedTarget.createStyleAnimation((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.drop((com.codename1.ui.Component) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.findDropTargetAt(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.getBaseline(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getBoundPropertyValue((java.lang.String) safeArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) safeArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.getChildrenAsList(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getClientProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.getClosestComponentTo(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getComponentAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.getComponentAt(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getComponentCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                return typedTarget.getComponentIndex((com.codename1.ui.Component) safeArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getEmbed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEmbed();
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getPropertyValue((java.lang.String) safeArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.getResponderAt(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSideGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) safeArgs[0]);
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
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.growShrink(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                return typedTarget.isChildOf((com.codename1.ui.Container) safeArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHidden();
            }
        }
        if ("isHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.isHidden(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                return typedTarget.isOwnedBy((com.codename1.ui.Component) safeArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.iterator(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.keyPressed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.keyReleased(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.keyRepeated(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.longPointerPress(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                typedTarget.morph((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.Runnable) safeArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                typedTarget.morphAndWait((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paintComponent((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                typedTarget.paintComponent((com.codename1.ui.Graphics) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) safeArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.paintLock(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.paintShadows((com.codename1.ui.Graphics) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.pointerDragged(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                typedTarget.pointerDragged((int[]) safeArgs[0], (int[]) safeArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                typedTarget.pointerHover((int[]) safeArgs[0], (int[]) safeArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                typedTarget.pointerHoverPressed((int[]) safeArgs[0], (int[]) safeArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                typedTarget.pointerHoverReleased((int[]) safeArgs[0], (int[]) safeArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.pointerPressed(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                typedTarget.pointerPressed((int[]) safeArgs[0], (int[]) safeArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.pointerReleased(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                typedTarget.pointerReleased((int[]) safeArgs[0], (int[]) safeArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.putClientProperty((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.refreshTheme(); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.refreshTheme(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.removeComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) safeArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) safeArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.repaint(); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.repaint(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.replace((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.animations.Transition) safeArgs[2]); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                typedTarget.replace((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.animations.Transition) safeArgs[2], (java.lang.Runnable) safeArgs[3], ((Number) safeArgs[4]).intValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.replaceAndWait((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.animations.Transition) safeArgs[2]); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                typedTarget.replaceAndWait((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.animations.Transition) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                typedTarget.replaceAndWait((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (com.codename1.ui.animations.Transition) safeArgs[2], ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.scrollRectToVisible(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), (com.codename1.ui.Component) safeArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setAccessibilityText((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAlwaysTensile(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setBlockLead(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.setBoundPropertyValue((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCellRenderer(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCloudBoundProperty((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCloudDestinationProperty((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.setComponentState((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCursor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) safeArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setDragTransparency(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDraggable(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDropTarget(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) safeArgs[0]); return null;
            }
        }
        if ("setEmbed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setEmbed((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFlatten(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFocus(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFocusable(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setGrabsPointerEvents(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHandlesInput(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setHeight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHidden(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.setHidden(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHideInLandscape(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHideInPortrait(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setIgnorePointerEvents(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setInlineAllStyles((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setInlineDisabledStyles((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setInlinePressedStyles((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setInlineSelectedStyles((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) safeArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setInlineUnselectedStyles((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setIsScrollVisible(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                typedTarget.setLabelForComponent((com.codename1.ui.Label) safeArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) safeArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setLeadComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setNextFocusDown((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setNextFocusRight((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setNextFocusUp((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setOpaque(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setOwner((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPreferredH(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setPreferredSizeStr((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPreferredTabIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPreferredW(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) safeArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                return typedTarget.setPropertyValue((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setRTL(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setRippleEffect(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSafeArea(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSafeAreaRoot(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setScrollAnimationSpeed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setScrollIncrement(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setScrollOpacityChangeSpeed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setScrollVisible(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setScrollable(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setScrollableX(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setScrollableY(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setSelectCommandText((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) safeArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShouldCalcPreferredSize(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                typedTarget.setSize((com.codename1.ui.geom.Dimension) safeArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSmoothScrolling(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSnapToGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTabIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setTactileTouch(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setTensileDragEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTensileLength(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setTooltip((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setTraversable(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUIID((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.setUIID((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) safeArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) safeArgs[0]); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setVisible(((Boolean) safeArgs[0]).booleanValue()); return null;
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
        if ("startEditingAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                typedTarget.stopEditing((java.lang.Runnable) safeArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.styleChanged((java.lang.String) safeArgs[0], (com.codename1.ui.plaf.Style) safeArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                typedTarget.unbindProperty((java.lang.String) safeArgs[0], (com.codename1.cloud.BindTarget) safeArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.updateTabIndices(((Number) safeArgs[0]).intValue());
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.visibleBoundsContains(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.util.EventDispatcher typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.addListener((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("fireActionEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                typedTarget.fireActionEvent((com.codename1.ui.events.ActionEvent) safeArgs[0]); return null;
            }
        }
        if ("fireBindTargetChange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class}, false)) {
                typedTarget.fireBindTargetChange((com.codename1.ui.Component) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.Object) safeArgs[2], (java.lang.Object) safeArgs[3]); return null;
            }
        }
        if ("fireDataChangeEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.fireDataChangeEvent(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("fireFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.fireFocus((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("fireScrollEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.fireScrollEvent(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("fireSelectionEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.fireSelectionEvent(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("fireStyleChangeEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.fireStyleChangeEvent((java.lang.String) safeArgs[0], (com.codename1.ui.plaf.Style) safeArgs[1]); return null;
            }
        }
        if ("getListenerCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getListenerCollection();
            }
        }
        if ("getListenerVector".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getListenerVector();
            }
        }
        if ("hasListeners".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasListeners();
            }
        }
        if ("isBlocking".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBlocking();
            }
        }
        if ("removeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.removeListener((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("setBlocking".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setBlocking(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.util.GlassTutorial typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addHint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                typedTarget.addHint((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.geom.Rectangle) safeArgs[1]); return null;
            }
        }
        if ("showOn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                typedTarget.showOn((com.codename1.ui.Form) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.util.ImageIO typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getImageSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getImageSize((java.lang.String) safeArgs[0]);
            }
        }
        if ("isFormatSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isFormatSupported((java.lang.String) safeArgs[0]);
            }
        }
        if ("saveAndKeepAspect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                return typedTarget.saveAndKeepAspect((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).floatValue(), ((Boolean) safeArgs[6]).booleanValue(), ((Boolean) safeArgs[7]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.util.Resources typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDataResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDataResourceNames();
            }
        }
        if ("getFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getFont((java.lang.String) safeArgs[0]);
            }
        }
        if ("getFontResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFontResourceNames();
            }
        }
        if ("getImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("getImageResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getImageResourceNames();
            }
        }
        if ("getL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.getL10N((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getL10NResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getL10NResourceNames();
            }
        }
        if ("getMajorVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMajorVersion();
            }
        }
        if ("getMetaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMetaData();
            }
        }
        if ("getMinorVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinorVersion();
            }
        }
        if ("getResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResourceNames();
            }
        }
        if ("getTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThemeResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThemeResourceNames();
            }
        }
        if ("getUIResourceNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUIResourceNames();
            }
        }
        if ("isAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isAnimation((java.lang.String) safeArgs[0]);
            }
        }
        if ("isData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isData((java.lang.String) safeArgs[0]);
            }
        }
        if ("isFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isFont((java.lang.String) safeArgs[0]);
            }
        }
        if ("isImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("isL10N".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isL10N((java.lang.String) safeArgs[0]);
            }
        }
        if ("isTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("isUI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isUI((java.lang.String) safeArgs[0]);
            }
        }
        if ("l10NLocaleSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.l10NLocaleSet((java.lang.String) safeArgs[0]);
            }
        }
        if ("listL10NLocales".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.listL10NLocales((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.util.UIBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addCommandListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addCommandListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addCommandListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addCommandListener((java.lang.String) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1]); return null;
            }
        }
        if ("addComponentListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.addComponentListener((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.Object) safeArgs[2]); return null;
            }
        }
        if ("back".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.back(); return null;
            }
        }
        if ("back".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.back((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("createBackLazyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                return typedTarget.createBackLazyValue((com.codename1.ui.Form) safeArgs[0]);
            }
        }
        if ("createContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class}, false)) {
                return typedTarget.createContainer((com.codename1.ui.util.Resources) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("createContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.createContainer((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("findByName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class}, false)) {
                return typedTarget.findByName((java.lang.String) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]);
            }
        }
        if ("findByName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Container.class}, false)) {
                return typedTarget.findByName((java.lang.String) safeArgs[0], (com.codename1.ui.Container) safeArgs[1]);
            }
        }
        if ("getHomeForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHomeForm();
            }
        }
        if ("getResourceFilePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResourceFilePath();
            }
        }
        if ("isBackCommandEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackCommandEnabled();
            }
        }
        if ("isKeepResourcesInRam".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isKeepResourcesInRam();
            }
        }
        if ("reloadContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.reloadContainer((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("reloadForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reloadForm(); return null;
            }
        }
        if ("removeCommandListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeCommandListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeCommandListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeCommandListener((java.lang.String) safeArgs[0], (com.codename1.ui.events.ActionListener) safeArgs[1]); return null;
            }
        }
        if ("removeComponentListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.removeComponentListener((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.Object) safeArgs[2]); return null;
            }
        }
        if ("setBackCommandEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setBackCommandEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHomeForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setHomeForm((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setKeepResourcesInRam".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setKeepResourcesInRam(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setResourceFilePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setResourceFilePath((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("showContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Command.class, com.codename1.ui.Component.class}, false)) {
                return typedTarget.showContainer((java.lang.String) safeArgs[0], (com.codename1.ui.Command) safeArgs[1], (com.codename1.ui.Component) safeArgs[2]);
            }
        }
        if ("showForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Command.class}, false)) {
                return typedTarget.showForm((java.lang.String) safeArgs[0], (com.codename1.ui.Command) safeArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.util.UITimer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cancel(); return null;
            }
        }
        if ("schedule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class, com.codename1.ui.Form.class}, false)) {
                typedTarget.schedule(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue(), (com.codename1.ui.Form) safeArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.util.WeakHashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.util.ImageIO.class) {
            if ("FORMAT_JPEG".equals(name)) return com.codename1.ui.util.ImageIO.FORMAT_JPEG;
            if ("FORMAT_PNG".equals(name)) return com.codename1.ui.util.ImageIO.FORMAT_PNG;
        }
        if (type == com.codename1.ui.util.UIBuilder.class) {
            if ("BACK_COMMAND_ID".equals(name)) return com.codename1.ui.util.UIBuilder.BACK_COMMAND_ID;
            if ("FORM_STATE_KEY_FOCUS".equals(name)) return com.codename1.ui.util.UIBuilder.FORM_STATE_KEY_FOCUS;
            if ("FORM_STATE_KEY_NAME".equals(name)) return com.codename1.ui.util.UIBuilder.FORM_STATE_KEY_NAME;
            if ("FORM_STATE_KEY_SELECTION".equals(name)) return com.codename1.ui.util.UIBuilder.FORM_STATE_KEY_SELECTION;
            if ("FORM_STATE_KEY_TITLE".equals(name)) return com.codename1.ui.util.UIBuilder.FORM_STATE_KEY_TITLE;
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
