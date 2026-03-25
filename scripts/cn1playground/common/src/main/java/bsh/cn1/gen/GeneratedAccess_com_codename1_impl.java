package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_impl {
    private GeneratedAccess_com_codename1_impl() {
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
        if ("CodenameOneImplementation".equals(simpleName)) {
            return com.codename1.impl.CodenameOneImplementation.class;
        }
        if ("CodenameOneThread".equals(simpleName)) {
            return com.codename1.impl.CodenameOneThread.class;
        }
        if ("FullScreenAdService".equals(simpleName)) {
            return com.codename1.impl.FullScreenAdService.class;
        }
        if ("VServAds".equals(simpleName)) {
            return com.codename1.impl.VServAds.class;
        }
        if ("VirtualKeyboardInterface".equals(simpleName)) {
            return com.codename1.impl.VirtualKeyboardInterface.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.impl.CodenameOneThread.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class, java.lang.String.class}, false);
                return new com.codename1.impl.CodenameOneThread((java.lang.Runnable) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.impl.CodenameOneImplementation.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.impl.CodenameOneThread.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("deregisterPushFromServer".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.impl.CodenameOneImplementation.deregisterPushFromServer(); return null;
            }
        }
        if ("getImageArrayClass".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.impl.CodenameOneImplementation.getImageArrayClass();
            }
        }
        if ("getObjectArrayClass".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.impl.CodenameOneImplementation.getObjectArrayClass();
            }
        }
        if ("getPurchaseCallback".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.impl.CodenameOneImplementation.getPurchaseCallback();
            }
        }
        if ("getStringArray2DClass".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.impl.CodenameOneImplementation.getStringArray2DClass();
            }
        }
        if ("getStringArrayClass".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.impl.CodenameOneImplementation.getStringArrayClass();
            }
        }
        if ("registerPushOnServer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Byte.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Byte.class, java.lang.String.class, java.lang.String.class}, false);
                com.codename1.impl.CodenameOneImplementation.registerPushOnServer((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).byteValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4]); return null;
            }
        }
        if ("registerServerPush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Byte.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Byte.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.impl.CodenameOneImplementation.registerServerPush((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).byteValue(), (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4]);
            }
        }
        if ("setOnExit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                com.codename1.impl.CodenameOneImplementation.setOnExit((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setPurchaseCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.payment.PurchaseCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.payment.PurchaseCallback.class}, false);
                com.codename1.impl.CodenameOneImplementation.setPurchaseCallback((com.codename1.payment.PurchaseCallback) adaptedArgs[0]); return null;
            }
        }
        if ("setPushCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.push.PushCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.push.PushCallback.class}, false);
                com.codename1.impl.CodenameOneImplementation.setPushCallback((com.codename1.push.PushCallback) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.impl.CodenameOneImplementation.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("handleException".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                com.codename1.impl.CodenameOneThread.handleException((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("pop".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.impl.CodenameOneThread.pop(); return null;
            }
        }
        if ("push".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.impl.CodenameOneThread.push(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("storeStack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false);
                com.codename1.impl.CodenameOneThread.storeStack((java.lang.Throwable) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.impl.CodenameOneThread.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.impl.VServAds) {
            try {
                return invoke0((com.codename1.impl.VServAds) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.impl.CodenameOneImplementation) {
            try {
                return invoke1((com.codename1.impl.CodenameOneImplementation) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.impl.CodenameOneThread) {
            try {
                return invoke2((com.codename1.impl.CodenameOneThread) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.impl.FullScreenAdService) {
            try {
                return invoke3((com.codename1.impl.FullScreenAdService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.impl.VirtualKeyboardInterface) {
            try {
                return invoke4((com.codename1.impl.VirtualKeyboardInterface) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.impl.VServAds typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bindTransitionAd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.bindTransitionAd(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("getAdDisplayTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdDisplayTime();
            }
        }
        if ("getCategory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategory();
            }
        }
        if ("getCountryCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCountryCode();
            }
        }
        if ("getLocale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocale();
            }
        }
        if ("getNetworkCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNetworkCode();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getZoneId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoneId();
            }
        }
        if ("isAllowSkipping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowSkipping();
            }
        }
        if ("isAllowWithoutNetwork".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowWithoutNetwork();
            }
        }
        if ("isScaleMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScaleMode();
            }
        }
        if ("setAdDisplayTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAdDisplayTime(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAllowSkipping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAllowSkipping(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAllowWithoutNetwork".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAllowWithoutNetwork(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCategory(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setCountryCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCountryCode((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setLocale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLocale((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNetworkCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNetworkCode((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setScaleMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScaleMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setZoneId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setZoneId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("showWelcomeAd".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.showWelcomeAd(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.impl.CodenameOneImplementation typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addCompletionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false);
                typedTarget.addCompletionHandler((com.codename1.media.Media) adaptedArgs[0], (java.lang.Runnable) adaptedArgs[1]); return null;
            }
        }
        if ("addConnectionToQueue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false);
                typedTarget.addConnectionToQueue((com.codename1.io.ConnectionRequest) adaptedArgs[0]); return null;
            }
        }
        if ("addCookie".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Cookie.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Cookie.class}, false);
                typedTarget.addCookie((com.codename1.io.Cookie) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Cookie[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Cookie[].class}, false);
                typedTarget.addCookie((com.codename1.io.Cookie[]) adaptedArgs[0]); return null;
            }
        }
        if ("addHeavyActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addHeavyActionListener((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("addNativeBrowserWindowOnLoadListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addNativeBrowserWindowOnLoadListener((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("afterComponentPaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class}, false);
                typedTarget.afterComponentPaint((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Graphics) adaptedArgs[1]); return null;
            }
        }
        if ("animateImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Long.class}, false);
                return typedTarget.animateImage((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                typedTarget.announceForAccessibility((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("areMutableImagesFast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.areMutableImagesFast();
            }
        }
        if ("beforeComponentPaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class}, false);
                typedTarget.beforeComponentPaint((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Graphics) adaptedArgs[1]); return null;
            }
        }
        if ("blockCopyPaste".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.blockCopyPaste(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("browserBack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                typedTarget.browserBack((com.codename1.ui.PeerComponent) adaptedArgs[0]); return null;
            }
        }
        if ("browserClearHistory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                typedTarget.browserClearHistory((com.codename1.ui.PeerComponent) adaptedArgs[0]); return null;
            }
        }
        if ("browserDestroy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                typedTarget.browserDestroy((com.codename1.ui.PeerComponent) adaptedArgs[0]); return null;
            }
        }
        if ("browserExecute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false);
                typedTarget.browserExecute((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("browserExecuteAndReturnString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false);
                return typedTarget.browserExecuteAndReturnString((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("browserExposeInJavaScript".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.Object.class, java.lang.String.class}, false);
                typedTarget.browserExposeInJavaScript((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("browserForward".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                typedTarget.browserForward((com.codename1.ui.PeerComponent) adaptedArgs[0]); return null;
            }
        }
        if ("browserHasBack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                return typedTarget.browserHasBack((com.codename1.ui.PeerComponent) adaptedArgs[0]);
            }
        }
        if ("browserHasForward".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                return typedTarget.browserHasForward((com.codename1.ui.PeerComponent) adaptedArgs[0]);
            }
        }
        if ("browserReload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                typedTarget.browserReload((com.codename1.ui.PeerComponent) adaptedArgs[0]); return null;
            }
        }
        if ("browserStop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                typedTarget.browserStop((com.codename1.ui.PeerComponent) adaptedArgs[0]); return null;
            }
        }
        if ("canDial".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canDial();
            }
        }
        if ("canExecute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.canExecute((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("canForceOrientation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canForceOrientation();
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("canInstallOnHomescreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canInstallOnHomescreen();
            }
        }
        if ("cancelLocalNotification".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.cancelLocalNotification((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("cancelRepaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Animation.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Animation.class}, false);
                typedTarget.cancelRepaint((com.codename1.ui.animations.Animation) adaptedArgs[0]); return null;
            }
        }
        if ("captureAudio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.captureAudio((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.captureAudio((com.codename1.media.MediaRecorderBuilder) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("captureBrowserScreenshot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                return typedTarget.captureBrowserScreenshot((com.codename1.ui.PeerComponent) adaptedArgs[0]);
            }
        }
        if ("capturePhoto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.capturePhoto((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("captureScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.captureScreen();
            }
        }
        if ("captureVideo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.captureVideo((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.capture.VideoCaptureConstraints.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.captureVideo((com.codename1.capture.VideoCaptureConstraints) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("charWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Character.class}, false);
                return typedTarget.charWidth((java.lang.Object) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue());
            }
        }
        if ("charsWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.charsWidth((java.lang.Object) adaptedArgs[0], (char[]) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
        }
        if ("checkSSLCertificatesRequiresCallbackFromNative".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.checkSSLCertificatesRequiresCallbackFromNative();
            }
        }
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.cleanup((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("clearNativeCookies".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearNativeCookies(); return null;
            }
        }
        if ("clearRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.clearRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("clearStorage".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearStorage(); return null;
            }
        }
        if ("clipRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.clipRect((java.lang.Object) adaptedArgs[0], (com.codename1.ui.geom.Rectangle) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.clipRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("componentRemoved".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.componentRemoved((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("concatenateAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return typedTarget.concatenateAlpha((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("concatenateTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                typedTarget.concatenateTransform((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("confirmControlView".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.confirmControlView(); return null;
            }
        }
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return typedTarget.connect((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.connect((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Number) adaptedArgs[3]).intValue());
            }
        }
        if ("connectSocket".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.connectSocket((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.connectSocket((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("convertBidiLogicalToVisual".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.convertBidiLogicalToVisual((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("convertToPixels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.convertToPixels(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("copySelectionToClipboard".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class}, false);
                typedTarget.copySelectionToClipboard((com.codename1.ui.TextSelection) adaptedArgs[0]); return null;
            }
        }
        if ("copyToClipboard".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.copyToClipboard((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("copyTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                typedTarget.copyTransform((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("createBackgroundMedia".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.createBackgroundMedia((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("createBackgroundMediaAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.createBackgroundMediaAsync((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("createBrowserComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.createBrowserComponent((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("createContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.createContact((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5]);
            }
        }
        if ("createFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createFont(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("createHeavyButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.createHeavyButton((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("createImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.createImage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, com.codename1.ui.Stroke.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Shape.class, com.codename1.ui.Stroke.class, java.lang.Integer.class}, false);
                return typedTarget.createImage((com.codename1.ui.geom.Shape) adaptedArgs[0], (com.codename1.ui.Stroke) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createImage((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createImage((int[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("createMedia".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false);
                return typedTarget.createMedia((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createMediaAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false);
                return typedTarget.createMediaAsync((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createMediaRecorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class}, false);
                return typedTarget.createMediaRecorder((com.codename1.media.MediaRecorderBuilder) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.createMediaRecorder((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("createMutableImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createMutableImage(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("createNativeBrowserWindow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.createNativeBrowserWindow((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("createNativeIndexed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.createNativeIndexed((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("createNativeOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.createNativeOverlay((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("createNativePeer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.createNativePeer((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("createSVGImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return typedTarget.createSVGImage((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("createSoftWeakRef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.createSoftWeakRef((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("deinitialize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.deinitialize(); return null;
            }
        }
        if ("deinitializeHeavyButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.deinitializeHeavyButton((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("deinitializeTextSelection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class}, false);
                typedTarget.deinitializeTextSelection((com.codename1.ui.TextSelection) adaptedArgs[0]); return null;
            }
        }
        if ("deleteContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteContact((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteDB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.deleteDB((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deleteFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.deleteFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deleteStorageFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.deleteStorageFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deregisterPush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.deregisterPush(); return null;
            }
        }
        if ("deriveTrueTypeFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.deriveTrueTypeFont((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("dial".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.dial((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("disconnectSocket".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.disconnectSocket((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("dismissNotification".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.dismissNotification((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("disposeGraphics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.disposeGraphics((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("downloadImageToCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToCache((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.SuccessCallback) adaptedArgs[2], (com.codename1.util.FailureCallback) adaptedArgs[3]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.SuccessCallback) adaptedArgs[2], (com.codename1.util.FailureCallback) adaptedArgs[3]); return null;
            }
        }
        if ("drawArc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawArc((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("drawImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawImage((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawImage((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue()); return null;
            }
        }
        if ("drawImageArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawImageArea((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue(), ((Number) adaptedArgs[7]).intValue()); return null;
            }
        }
        if ("drawImageRotated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawImageRotated((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("drawLabelComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.plaf.Style.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.plaf.Style.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                typedTarget.drawLabelComponent((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), (com.codename1.ui.plaf.Style) adaptedArgs[5], (java.lang.String) adaptedArgs[6], (java.lang.Object) adaptedArgs[7], (java.lang.Object) adaptedArgs[8], ((Number) adaptedArgs[9]).intValue(), ((Number) adaptedArgs[10]).intValue(), ((Boolean) adaptedArgs[11]).booleanValue(), ((Boolean) adaptedArgs[12]).booleanValue(), ((Number) adaptedArgs[13]).intValue(), ((Number) adaptedArgs[14]).intValue(), ((Boolean) adaptedArgs[15]).booleanValue(), ((Number) adaptedArgs[16]).intValue(), ((Boolean) adaptedArgs[17]).booleanValue(), ((Number) adaptedArgs[18]).intValue()); return null;
            }
        }
        if ("drawLine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawLine((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("drawPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, int[].class, java.lang.Integer.class}, false);
                typedTarget.drawPolygon((java.lang.Object) adaptedArgs[0], (int[]) adaptedArgs[1], (int[]) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("drawRGB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.drawRGB((java.lang.Object) adaptedArgs[0], (int[]) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue(), ((Boolean) adaptedArgs[7]).booleanValue()); return null;
            }
        }
        if ("drawRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue()); return null;
            }
        }
        if ("drawRoundRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawRoundRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("drawShadow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.drawShadow((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue(), ((Number) adaptedArgs[7]).intValue(), ((Number) adaptedArgs[8]).intValue(), ((Number) adaptedArgs[9]).floatValue()); return null;
            }
        }
        if ("drawShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Shape.class, com.codename1.ui.Stroke.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Shape.class, com.codename1.ui.Stroke.class}, false);
                typedTarget.drawShape((java.lang.Object) adaptedArgs[0], (com.codename1.ui.geom.Shape) adaptedArgs[1], (com.codename1.ui.Stroke) adaptedArgs[2]); return null;
            }
        }
        if ("drawString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawString((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drawString((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue()); return null;
            }
        }
        if ("drawingEncodedImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.EncodedImage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.EncodedImage.class}, false);
                typedTarget.drawingEncodedImage((com.codename1.ui.EncodedImage) adaptedArgs[0]); return null;
            }
        }
        if ("editString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class}, false);
                typedTarget.editString((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("editStringImpl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class}, false);
                typedTarget.editStringImpl((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("edtIdle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.edtIdle(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.execute((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.execute((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.exists((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("existsDB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.existsDB((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("exit".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.exit(); return null;
            }
        }
        if ("exitApplication".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.exitApplication(); return null;
            }
        }
        if ("exitFullScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.exitFullScreen();
            }
        }
        if ("extractHardRef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.extractHardRef((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("fillArc".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.fillArc((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("fillLinearGradient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.fillLinearGradient((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue(), ((Boolean) adaptedArgs[7]).booleanValue()); return null;
            }
        }
        if ("fillPolygon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, int[].class, java.lang.Integer.class}, false);
                typedTarget.fillPolygon((java.lang.Object) adaptedArgs[0], (int[]) adaptedArgs[1], (int[]) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("fillRadialGradient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.fillRadialGradient((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.fillRadialGradient((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue(), ((Number) adaptedArgs[7]).intValue(), ((Number) adaptedArgs[8]).intValue()); return null;
            }
        }
        if ("fillRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.fillRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false);
                typedTarget.fillRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).byteValue()); return null;
            }
        }
        if ("fillRectRadialGradient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.fillRectRadialGradient((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue(), ((Number) adaptedArgs[7]).floatValue(), ((Number) adaptedArgs[8]).floatValue(), ((Number) adaptedArgs[9]).floatValue()); return null;
            }
        }
        if ("fillRoundRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.fillRoundRect((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("fillShape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Shape.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Shape.class}, false);
                typedTarget.fillShape((java.lang.Object) adaptedArgs[0], (com.codename1.ui.geom.Shape) adaptedArgs[1]); return null;
            }
        }
        if ("fillTriangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.fillTriangle((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("flashBacklight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.flashBacklight(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("flipImageHorizontally".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false);
                return typedTarget.flipImageHorizontally((com.codename1.ui.Image) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("flipImageVertically".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false);
                return typedTarget.flipImageVertically((com.codename1.ui.Image) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("flushGraphics".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushGraphics(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.flushGraphics(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("flushStorageCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushStorageCache(); return null;
            }
        }
        if ("gaussianBlurImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class}, false);
                return typedTarget.gaussianBlurImage((com.codename1.ui.Image) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("getAPIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAPIds();
            }
        }
        if ("getAPName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getAPName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getAPType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getAPType((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getActualDisplayHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActualDisplayHeight();
            }
        }
        if ("getAllContacts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getAllContacts(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return typedTarget.getAllContacts(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue());
            }
        }
        if ("getAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getAlpha((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAppArg".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAppArg();
            }
        }
        if ("getAppHomePath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAppHomePath();
            }
        }
        if ("getApplicationIconImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getApplicationIconImage();
            }
        }
        if ("getAvailableRecordingMimeTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailableRecordingMimeTypes();
            }
        }
        if ("getBackKeyCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackKeyCode();
            }
        }
        if ("getBackspaceKeyCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackspaceKeyCode();
            }
        }
        if ("getBrowserTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                return typedTarget.getBrowserTitle((com.codename1.ui.PeerComponent) adaptedArgs[0]);
            }
        }
        if ("getBrowserURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                return typedTarget.getBrowserURL((com.codename1.ui.PeerComponent) adaptedArgs[0]);
            }
        }
        if ("getCachesDir".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCachesDir();
            }
        }
        if ("getCharLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.getCharLocation((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getClearKeyCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClearKeyCode();
            }
        }
        if ("getClipHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getClipHeight((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClipRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getClipRect((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClipWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getClipWidth((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClipX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getClipX((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClipY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getClipY((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCodeScanner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCodeScanner();
            }
        }
        if ("getColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getColor((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCommandBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommandBehavior();
            }
        }
        if ("getComponentScreenGraphics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class}, false);
                return typedTarget.getComponentScreenGraphics((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Graphics) adaptedArgs[1]);
            }
        }
        if ("getContactById".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getContactById((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return typedTarget.getContactById((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue());
            }
        }
        if ("getContentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getContentLength((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCookiesForURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getCookiesForURL((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCurrentAccessPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentAccessPoint();
            }
        }
        if ("getCurrentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentForm();
            }
        }
        if ("getDatabasePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getDatabasePath((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getDefaultFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFont();
            }
        }
        if ("getDesktopSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopSize();
            }
        }
        if ("getDeviceDensity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeviceDensity();
            }
        }
        if ("getDisplayHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayHeight();
            }
        }
        if ("getDisplaySafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getDisplaySafeArea((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getDisplayWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayWidth();
            }
        }
        if ("getDragPathLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragPathLength();
            }
        }
        if ("getDragPathTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragPathTime();
            }
        }
        if ("getDragSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, long[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, long[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getDragSpeed((float[]) adaptedArgs[0], (long[]) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
        }
        if ("getDragStartPercentage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragStartPercentage();
            }
        }
        if ("getEDTThreadPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEDTThreadPriority();
            }
        }
        if ("getEditingText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingText();
            }
        }
        if ("getFace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getFace((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getFileLastModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getFileLastModified((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getFileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getFileLength((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getFileSystemSeparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFileSystemSeparator();
            }
        }
        if ("getFontAscent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getFontAscent((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getFontDescent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getFontDescent((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getFontPlatformNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFontPlatformNames();
            }
        }
        if ("getGameAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getGameAction(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getHeaderField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.getHeaderField((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("getHeaderFieldNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getHeaderFieldNames((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getHeaderFields".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.getHeaderFields((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("getHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getHeight((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getHostOrIP".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHostOrIP();
            }
        }
        if ("getImageHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getImageHeight((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getImageIO".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImageIO();
            }
        }
        if ("getImageWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getImageWidth((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getInAppPurchase".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInAppPurchase();
            }
        }
        if ("getInitialWindowSizeHintPercent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInitialWindowSizeHintPercent();
            }
        }
        if ("getInvisibleAreaUnderVKB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInvisibleAreaUnderVKB();
            }
        }
        if ("getKeyCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getKeyCode(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getKeyboardType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyboardType();
            }
        }
        if ("getLargerTextScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLargerTextScale();
            }
        }
        if ("getLineSeparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLineSeparator();
            }
        }
        if ("getLinkedContactIds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.contacts.Contact.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.contacts.Contact.class}, false);
                return typedTarget.getLinkedContactIds((com.codename1.contacts.Contact) adaptedArgs[0]);
            }
        }
        if ("getLocalizationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizationManager();
            }
        }
        if ("getLocationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocationManager();
            }
        }
        if ("getMsisdn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMsisdn();
            }
        }
        if ("getNativeGraphics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeGraphics();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getNativeGraphics((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getNativeTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.getNativeTransition((com.codename1.ui.animations.Transition) adaptedArgs[0]);
            }
        }
        if ("getPasteDataFromClipboard".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPasteDataFromClipboard();
            }
        }
        if ("getPlatformName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlatformName();
            }
        }
        if ("getPlatformOverrides".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlatformOverrides();
            }
        }
        if ("getPreferredBackgroundFetchInterval".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredBackgroundFetchInterval();
            }
        }
        if ("getProjectBuildHints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProjectBuildHints();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getRGB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.getRGB((java.lang.Object) adaptedArgs[0], (int[]) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("getRenderingHints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getRenderingHints((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getResponseCode((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getResponseMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getResponseMessage((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getRootAvailableSpace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getRootAvailableSpace((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getRootSizeBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getRootSizeBytes((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getRootType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getRootType((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getSMSSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSMSSupport();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.getSSLCertificates((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getSVGDocument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getSVGDocument((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getSharedJavscriptContext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSharedJavscriptContext();
            }
        }
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getSize((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getSocketAvailableInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getSocketAvailableInput((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getSocketErrorCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getSocketErrorCode((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getSocketErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getSocketErrorMessage((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getSoftkeyCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSoftkeyCode(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getSoftkeyCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSoftkeyCount();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Thread.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Thread.class, java.lang.Throwable.class}, false);
                return typedTarget.getStackTrace((java.lang.Thread) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if ("getStorageData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStorageData();
            }
        }
        if ("getStorageEntrySize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getStorageEntrySize((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getStyle((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getTransform((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Transform.class}, false);
                typedTarget.getTransform((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Transform) adaptedArgs[1]); return null;
            }
        }
        if ("getTranslateX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getTranslateX((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getTranslateY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.getTranslateY((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getURLDomain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getURLDomain((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getURLPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getURLPath((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getUdid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUdid();
            }
        }
        if ("getWindowBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWindowBounds();
            }
        }
        if ("handleEDTException".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.handleEDTException((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("hasCachesDir".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasCachesDir();
            }
        }
        if ("hasCamera".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasCamera();
            }
        }
        if ("hasNativeTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasNativeTheme();
            }
        }
        if ("hasPendingPaints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasPendingPaints();
            }
        }
        if ("hideNativeOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false);
                typedTarget.hideNativeOverlay((com.codename1.ui.Component) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("hideTextEditor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.hideTextEditor(); return null;
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.init((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("initEDT".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.initEDT(); return null;
            }
        }
        if ("initHeavyButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.initHeavyButton((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("initImpl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.initImpl((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("initializeTextSelection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class}, false);
                typedTarget.initializeTextSelection((com.codename1.ui.TextSelection) adaptedArgs[0]); return null;
            }
        }
        if ("installMessageListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.installMessageListener((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("installNativeTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.installNativeTheme(); return null;
            }
        }
        if ("installTar".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.installTar(); return null;
            }
        }
        if ("instanceofByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofByteArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofDoubleArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofDoubleArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofFloatArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofFloatArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofIntArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofIntArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofLongArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofLongArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofObjArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofObjArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofShortArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.instanceofShortArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isAPSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAPSupported();
            }
        }
        if ("isAffineSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAffineSupported();
            }
        }
        if ("isAlphaGlobal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlphaGlobal();
            }
        }
        if ("isAlphaMutableImageSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlphaMutableImageSupported();
            }
        }
        if ("isAltGraphKeyDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAltGraphKeyDown();
            }
        }
        if ("isAltKeyDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAltKeyDown();
            }
        }
        if ("isAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isAnimation((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isAntiAliased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isAntiAliased((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isAntiAliasedText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isAntiAliasedText((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isAntiAliasedTextSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAntiAliasedTextSupported();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isAntiAliasedTextSupported((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isAntiAliasingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAntiAliasingSupported();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isAntiAliasingSupported((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isAsyncEditMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAsyncEditMode();
            }
        }
        if ("isBackgroundFetchSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundFetchSupported();
            }
        }
        if ("isBadgingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBadgingSupported();
            }
        }
        if ("isBaselineTextSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBaselineTextSupported();
            }
        }
        if ("isBidiAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBidiAlgorithm();
            }
        }
        if ("isBuiltinSoundAvailable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isBuiltinSoundAvailable((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isBuiltinSoundsEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBuiltinSoundsEnabled();
            }
        }
        if ("isCallDetectionSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCallDetectionSupported();
            }
        }
        if ("isClickTouchScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isClickTouchScreen();
            }
        }
        if ("isContactsPermissionGranted".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isContactsPermissionGranted();
            }
        }
        if ("isControlKeyDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isControlKeyDown();
            }
        }
        if ("isDarkMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDarkMode();
            }
        }
        if ("isDatabaseCustomPathSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDatabaseCustomPathSupported();
            }
        }
        if ("isDesktop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDesktop();
            }
        }
        if ("isDirectory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isDirectory((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isDrawShadowFast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDrawShadowFast();
            }
        }
        if ("isDrawShadowSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDrawShadowSupported();
            }
        }
        if ("isEditingText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditingText();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isEditingText((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isFullScreenSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFullScreenSupported();
            }
        }
        if ("isGalleryTypeSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isGalleryTypeSupported(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isGaussianBlurSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGaussianBlurSupported();
            }
        }
        if ("isGetAllContactsFast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGetAllContactsFast();
            }
        }
        if ("isHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isHidden((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isInCall".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInCall();
            }
        }
        if ("isInFullScreenMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInFullScreenMode();
            }
        }
        if ("isInitialized".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInitialized();
            }
        }
        if ("isJailbrokenDevice".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isJailbrokenDevice();
            }
        }
        if ("isLargerTextEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLargerTextEnabled();
            }
        }
        if ("isLookupFontSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLookupFontSupported();
            }
        }
        if ("isMetaKeyDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMetaKeyDown();
            }
        }
        if ("isMinimized".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMinimized();
            }
        }
        if ("isMultiTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMultiTouch();
            }
        }
        if ("isNativeBrowserComponentSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeBrowserComponentSupported();
            }
        }
        if ("isNativeCookieSharingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeCookieSharingSupported();
            }
        }
        if ("isNativeEditorVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isNativeEditorVisible((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isNativeFontSchemeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeFontSchemeSupported();
            }
        }
        if ("isNativeIndexed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeIndexed();
            }
        }
        if ("isNativeInputImmediate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeInputImmediate();
            }
        }
        if ("isNativeInputSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeInputSupported();
            }
        }
        if ("isNativePickerTypeSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isNativePickerTypeSupported(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isNativeShareSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeShareSupported();
            }
        }
        if ("isNativeTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeTitle();
            }
        }
        if ("isNativeVideoPlayerControlsIncluded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeVideoPlayerControlsIncluded();
            }
        }
        if ("isNotificationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNotificationSupported();
            }
        }
        if ("isOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Object.class}, false);
                return typedTarget.isOpaque((com.codename1.ui.Image) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("isOpenNativeNavigationAppSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpenNativeNavigationAppSupported();
            }
        }
        if ("isPerspectiveTransformSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPerspectiveTransformSupported();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isPerspectiveTransformSupported((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPortrait();
            }
        }
        if ("isRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.isRTL(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isRTLOrWhitespace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.isRTLOrWhitespace(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isReadTimeoutSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadTimeoutSupported();
            }
        }
        if ("isRightMouseButtonDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRightMouseButtonDown();
            }
        }
        if ("isRotationDrawingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRotationDrawingSupported();
            }
        }
        if ("isSVGSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSVGSupported();
            }
        }
        if ("isScaledImageDrawingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScaledImageDrawingSupported();
            }
        }
        if ("isScreenLockSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScreenLockSupported();
            }
        }
        if ("isScrollWheeling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollWheeling();
            }
        }
        if ("isServerSocketAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isServerSocketAvailable();
            }
        }
        if ("isSetCursorSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSetCursorSupported();
            }
        }
        if ("isShapeClipSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isShapeClipSupported((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isShapeSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isShapeSupported((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isShiftKeyDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShiftKeyDown();
            }
        }
        if ("isSimulator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSimulator();
            }
        }
        if ("isSocketAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSocketAvailable();
            }
        }
        if ("isSocketConnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isSocketConnected((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isTablet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTablet();
            }
        }
        if ("isThirdSoftButton".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isThirdSoftButton();
            }
        }
        if ("isTimeoutSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTimeoutSupported();
            }
        }
        if ("isTouchDevice".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouchDevice();
            }
        }
        if ("isTransformSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTransformSupported();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isTransformSupported((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isTranslationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTranslationSupported();
            }
        }
        if ("isTrueTypeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTrueTypeSupported();
            }
        }
        if ("isURLWithCustomHeadersSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isURLWithCustomHeadersSupported();
            }
        }
        if ("isUseNativeCookieStore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseNativeCookieStore();
            }
        }
        if ("isVPNActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVPNActive();
            }
        }
        if ("isVPNDetectionSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVPNDetectionSupported();
            }
        }
        if ("listFiles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.listFiles((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("listFilesystemRoots".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listFilesystemRoots();
            }
        }
        if ("listStorageEntries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listStorageEntries();
            }
        }
        if ("listenSocket".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listenSocket(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("loadNativeFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.loadNativeFont((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("loadTrueTypeFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.loadTrueTypeFont((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("lockOrientation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.lockOrientation(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("lockScreen".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.lockScreen(); return null;
            }
        }
        if ("logStreamClose".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                typedTarget.logStreamClose((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("logStreamCreate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                typedTarget.logStreamCreate((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("logStreamDoubleClose".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                typedTarget.logStreamDoubleClose((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("makeTransformAffine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return typedTarget.makeTransformAffine(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue(), ((Number) adaptedArgs[4]).doubleValue(), ((Number) adaptedArgs[5]).doubleValue());
            }
        }
        if ("makeTransformCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.makeTransformCamera(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue(), ((Number) adaptedArgs[7]).floatValue(), ((Number) adaptedArgs[8]).floatValue());
            }
        }
        if ("makeTransformIdentity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.makeTransformIdentity();
            }
        }
        if ("makeTransformInverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.makeTransformInverse((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("makeTransformOrtho".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.makeTransformOrtho(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue());
            }
        }
        if ("makeTransformPerspective".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.makeTransformPerspective(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("makeTransformRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.makeTransformRotation(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("makeTransformScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.makeTransformScale(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("makeTransformTranslation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.makeTransformTranslation(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("minimizeApplication".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.minimizeApplication();
            }
        }
        if ("mkdir".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.mkdir((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("nativeBrowserWindowAddCloseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.nativeBrowserWindowAddCloseListener((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("nativeBrowserWindowCleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.nativeBrowserWindowCleanup((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("nativeBrowserWindowEval".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.BrowserWindow.EvalRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.BrowserWindow.EvalRequest.class}, false);
                typedTarget.nativeBrowserWindowEval((java.lang.Object) adaptedArgs[0], (com.codename1.ui.BrowserWindow.EvalRequest) adaptedArgs[1]); return null;
            }
        }
        if ("nativeBrowserWindowHide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.nativeBrowserWindowHide((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("nativeBrowserWindowRemoveCloseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.nativeBrowserWindowRemoveCloseListener((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("nativeBrowserWindowSetSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.nativeBrowserWindowSetSize((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("nativeBrowserWindowSetTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                typedTarget.nativeBrowserWindowSetTitle((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("nativeBrowserWindowShow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.nativeBrowserWindowShow((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("nativeEditorPaintsHint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nativeEditorPaintsHint();
            }
        }
        if ("nothingWithinComponentPaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.nothingWithinComponentPaint((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("notifyCommandBehavior".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.notifyCommandBehavior(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("notifyPushCompletion".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.notifyPushCompletion(); return null;
            }
        }
        if ("notifyStatusBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.util.Hashtable.class}, false);
                return typedTarget.notifyStatusBar((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), (java.util.Hashtable) adaptedArgs[5]);
            }
        }
        if ("numAlphaLevels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.numAlphaLevels();
            }
        }
        if ("numColors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.numColors();
            }
        }
        if ("onCanInstallOnHomescreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.onCanInstallOnHomescreen((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("onShow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                typedTarget.onShow((com.codename1.ui.Form) adaptedArgs[0]); return null;
            }
        }
        if ("openGallery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class, java.lang.Integer.class}, false);
                typedTarget.openGallery((com.codename1.ui.events.ActionListener) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("openImageGallery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.openImageGallery((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("openNativeNavigationApp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.openNativeNavigationApp((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.openNativeNavigationApp(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("openOrCreateDB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.openOrCreateDB((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.paintComponentBackground((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), (com.codename1.ui.plaf.Style) adaptedArgs[5]); return null;
            }
        }
        if ("paintDirty".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintDirty(); return null;
            }
        }
        if ("paintNativePeersBehind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.paintNativePeersBehind();
            }
        }
        if ("platformUsesInputMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.platformUsesInputMode();
            }
        }
        if ("playBuiltinSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.playBuiltinSound((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("playDialogSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.playDialogSound(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("popClip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.popClip((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("postInit".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.postInit(); return null;
            }
        }
        if ("postMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.MessageEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.MessageEvent.class}, false);
                typedTarget.postMessage((com.codename1.ui.events.MessageEvent) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.postMessage((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("promptInstallOnHomescreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.promptInstallOnHomescreen();
            }
        }
        if ("pushClip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.pushClip((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("readFromSocketStream".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.readFromSocketStream((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("refreshContacts".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshContacts(); return null;
            }
        }
        if ("refreshNativeTitle".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshNativeTitle(); return null;
            }
        }
        if ("registerPush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class, java.lang.Boolean.class}, false);
                typedTarget.registerPush((java.util.Hashtable) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("releaseImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.releaseImage((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("removeCompletionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false);
                typedTarget.removeCompletionHandler((com.codename1.media.Media) adaptedArgs[0], (java.lang.Runnable) adaptedArgs[1]); return null;
            }
        }
        if ("removeHeavyActionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeHeavyActionListener((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("removeNativeBrowserWindowOnLoadListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeNativeBrowserWindowOnLoadListener((java.lang.Object) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("rename".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.rename((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Animation.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Animation.class}, false);
                typedTarget.repaint((com.codename1.ui.animations.Animation) adaptedArgs[0]); return null;
            }
        }
        if ("requestFullScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.requestFullScreen();
            }
        }
        if ("requiresHeavyButtonForCopyToClipboard".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.requiresHeavyButtonForCopyToClipboard();
            }
        }
        if ("resetAffine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.resetAffine((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("restoreMinimizedApplication".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.restoreMinimizedApplication(); return null;
            }
        }
        if ("rotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return typedTarget.rotate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class}, false);
                typedTarget.rotate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.rotate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("rotate180Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false);
                return typedTarget.rotate180Degrees((com.codename1.ui.Image) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("rotate270Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false);
                return typedTarget.rotate270Degrees((com.codename1.ui.Image) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("rotate90Degrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false);
                return typedTarget.rotate90Degrees((com.codename1.ui.Image) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("saveTextEditingState".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.saveTextEditingState(); return null;
            }
        }
        if ("scale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.scale((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.scale((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("scalePoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, float[].class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, float[].class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.scalePoints(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (float[]) adaptedArgs[4], ((Number) adaptedArgs[5]).intValue(), (float[]) adaptedArgs[6], ((Number) adaptedArgs[7]).intValue(), ((Number) adaptedArgs[8]).intValue()); return null;
            }
        }
        if ("scheduleLocalNotification".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.notifications.LocalNotification.class, java.lang.Long.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.notifications.LocalNotification.class, java.lang.Long.class, java.lang.Integer.class}, false);
                typedTarget.scheduleLocalNotification((com.codename1.notifications.LocalNotification) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("screenshot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                typedTarget.screenshot((com.codename1.util.SuccessCallback) adaptedArgs[0]); return null;
            }
        }
        if ("sendMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String.class, com.codename1.messaging.Message.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String.class, com.codename1.messaging.Message.class}, false);
                typedTarget.sendMessage((java.lang.String[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.messaging.Message) adaptedArgs[2]); return null;
            }
        }
        if ("sendSMS".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                typedTarget.sendSMS((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("setAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setAlpha((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setAntiAliased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                typedTarget.setAntiAliased((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setAntiAliasedText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                typedTarget.setAntiAliasedText((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setAppArg".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAppArg((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setBadgeNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBadgeNumber(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setBidiAlgorithm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBidiAlgorithm(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBrowserPage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setBrowserPage((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("setBrowserPageInHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false);
                typedTarget.setBrowserPageInHierarchy((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setBrowserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBrowserProperty((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Object) adaptedArgs[2]); return null;
            }
        }
        if ("setBrowserURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class}, false);
                typedTarget.setBrowserURL((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.String.class, java.util.Map.class}, false);
                typedTarget.setBrowserURL((com.codename1.ui.PeerComponent) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.Map) adaptedArgs[2]); return null;
            }
        }
        if ("setBuiltinSoundsEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBuiltinSoundsEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setChunkedStreamingMode((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setClip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Shape.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Shape.class}, false);
                typedTarget.setClip((java.lang.Object) adaptedArgs[0], (com.codename1.ui.geom.Shape) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setClip((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("setClipRect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setClipRect((java.lang.Object) adaptedArgs[0], (com.codename1.ui.geom.Rectangle) adaptedArgs[1]); return null;
            }
        }
        if ("setCodenameOneGraphics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.setCodenameOneGraphics((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setColor((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setCommandBehavior".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCommandBehavior(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setConnectionId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setConnectionId((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setCurrentAccessPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCurrentAccessPoint((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCurrentForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                typedTarget.setCurrentForm((com.codename1.ui.Form) adaptedArgs[0]); return null;
            }
        }
        if ("setDisplayLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setDisplayLock((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setDragStartPercentage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDragStartPercentage(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFocusedEditingText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setFocusedEditingText((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setHeader((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                typedTarget.setHttpMethod((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setImageName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                typedTarget.setImageName((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setInitialWindowSizeHintPercent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setInitialWindowSizeHintPercent((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                typedTarget.setInsecure((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setLogListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.setLogListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setNativeBrowserScrollingEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.Boolean.class}, false);
                typedTarget.setNativeBrowserScrollingEnabled((com.codename1.ui.PeerComponent) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setNativeCommands".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Vector.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Vector.class}, false);
                typedTarget.setNativeCommands((java.util.Vector) adaptedArgs[0]); return null;
            }
        }
        if ("setNativeFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                typedTarget.setNativeFont((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setPinchToZoomEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class, java.lang.Boolean.class}, false);
                typedTarget.setPinchToZoomEnabled((com.codename1.ui.PeerComponent) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setPlatformHint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setPlatformHint((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setPollingFrequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPollingFrequency(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setPostRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                typedTarget.setPostRequest((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setPreferredBackgroundFetchInterval".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredBackgroundFetchInterval(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setProjectBuildHint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setProjectBuildHint((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setReadTimeout((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setRenderingHints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setRenderingHints((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setStorageData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setStorageData((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setThreadPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Thread.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Thread.class, java.lang.Integer.class}, false);
                typedTarget.setThreadPriority((java.lang.Thread) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTransform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Transform.class}, false);
                typedTarget.setTransform((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Transform) adaptedArgs[1]); return null;
            }
        }
        if ("setTransformAffine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                typedTarget.setTransformAffine((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue(), ((Number) adaptedArgs[4]).doubleValue(), ((Number) adaptedArgs[5]).doubleValue(), ((Number) adaptedArgs[6]).doubleValue()); return null;
            }
        }
        if ("setTransformCamera".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransformCamera((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue(), ((Number) adaptedArgs[7]).floatValue(), ((Number) adaptedArgs[8]).floatValue(), ((Number) adaptedArgs[9]).floatValue()); return null;
            }
        }
        if ("setTransformIdentity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setTransformIdentity((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setTransformInverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setTransformInverse((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setTransformOrtho".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransformOrtho((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue()); return null;
            }
        }
        if ("setTransformPerspective".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransformPerspective((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue()); return null;
            }
        }
        if ("setTransformRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransformRotation((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue()); return null;
            }
        }
        if ("setTransformScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransformScale((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("setTransformTranslation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setTransformTranslation((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("setUseNativeCookieStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseNativeCookieStore(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWindowSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setWindowSize(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ui.geom.Rectangle) adaptedArgs[3]); return null;
            }
        }
        if ("shear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.shear((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue()); return null;
            }
        }
        if ("shouldAutoDetectAccessPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shouldAutoDetectAccessPoint();
            }
        }
        if ("shouldPaintBackground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shouldPaintBackground();
            }
        }
        if ("shouldWriteUTFAsGetBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shouldWriteUTFAsGetBytes();
            }
        }
        if ("showNativePicker".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class, java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class, java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.showNativePicker(((Number) adaptedArgs[0]).intValue(), (com.codename1.ui.Component) adaptedArgs[1], (java.lang.Object) adaptedArgs[2], (java.lang.Object) adaptedArgs[3]);
            }
        }
        if ("showNativeScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.showNativeScreen((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("splitString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class, java.util.ArrayList.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class, java.util.ArrayList.class}, false);
                typedTarget.splitString((java.lang.String) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue(), (java.util.ArrayList) adaptedArgs[2]); return null;
            }
        }
        if ("startRemoteControl".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startRemoteControl(); return null;
            }
        }
        if ("startThread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Runnable.class}, false);
                typedTarget.startThread((java.lang.String) adaptedArgs[0], (java.lang.Runnable) adaptedArgs[1]); return null;
            }
        }
        if ("stopRemoteControl".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopRemoteControl(); return null;
            }
        }
        if ("stopTextEditing".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopTextEditing(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopTextEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("storageFileExists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.storageFileExists((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("stringWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.stringWidth((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("supportsBrowserExecuteAndReturnString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.PeerComponent.class}, false);
                return typedTarget.supportsBrowserExecuteAndReturnString((com.codename1.ui.PeerComponent) adaptedArgs[0]);
            }
        }
        if ("supportsNativeImageCache".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.supportsNativeImageCache();
            }
        }
        if ("supportsNativeTextAreaVerticalAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.supportsNativeTextAreaVerticalAlignment();
            }
        }
        if ("systemOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.systemOut((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("tileImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.tileImage((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue(), ((Number) adaptedArgs[5]).intValue()); return null;
            }
        }
        if ("toNativePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.toNativePath((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("transformEqualsImpl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class, com.codename1.ui.Transform.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Transform.class, com.codename1.ui.Transform.class}, false);
                return typedTarget.transformEqualsImpl((com.codename1.ui.Transform) adaptedArgs[0], (com.codename1.ui.Transform) adaptedArgs[1]);
            }
        }
        if ("transformNativeEqualsImpl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.transformNativeEqualsImpl((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("transformPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, float[].class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, float[].class, float[].class}, false);
                typedTarget.transformPoint((java.lang.Object) adaptedArgs[0], (float[]) adaptedArgs[1], (float[]) adaptedArgs[2]); return null;
            }
        }
        if ("transformPoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, float[].class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, float[].class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.transformPoints((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), (float[]) adaptedArgs[2], ((Number) adaptedArgs[3]).intValue(), (float[]) adaptedArgs[4], ((Number) adaptedArgs[5]).intValue(), ((Number) adaptedArgs[6]).intValue()); return null;
            }
        }
        if ("transformRotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.transformRotate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue()); return null;
            }
        }
        if ("transformScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.transformScale((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("transformTranslate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.transformTranslate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("translate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.translate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("translatePoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, float[].class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, float[].class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.translatePoints(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), (float[]) adaptedArgs[4], ((Number) adaptedArgs[5]).intValue(), (float[]) adaptedArgs[6], ((Number) adaptedArgs[7]).intValue(), ((Number) adaptedArgs[8]).intValue()); return null;
            }
        }
        if ("uninstallMessageListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.uninstallMessageListener((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("unlockOrientation".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlockOrientation(); return null;
            }
        }
        if ("unlockScreen".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlockScreen(); return null;
            }
        }
        if ("updateHeavyButtonBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.updateHeavyButtonBounds((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue()); return null;
            }
        }
        if ("updateNativeEditorText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                typedTarget.updateNativeEditorText((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("updateNativeOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false);
                typedTarget.updateNativeOverlay((com.codename1.ui.Component) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("usesInvokeAndBlockForEditString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.usesInvokeAndBlockForEditString();
            }
        }
        if ("vibrate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.vibrate(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("writeToSocketStream".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, byte[].class}, false);
                typedTarget.writeToSocketStream((java.lang.Object) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.impl.CodenameOneThread typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getStack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.getStack((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("hasStackFrame".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasStackFrame();
            }
        }
        if ("popStack".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.popStack(); return null;
            }
        }
        if ("pushStack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.pushStack(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("run".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.run(); return null;
            }
        }
        if ("storeStackForException".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false);
                typedTarget.storeStackForException((java.lang.Throwable) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.impl.FullScreenAdService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bindTransitionAd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.bindTransitionAd(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("getAdDisplayTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdDisplayTime();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("isAllowSkipping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowSkipping();
            }
        }
        if ("isAllowWithoutNetwork".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowWithoutNetwork();
            }
        }
        if ("isScaleMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScaleMode();
            }
        }
        if ("setAdDisplayTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAdDisplayTime(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAllowSkipping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAllowSkipping(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAllowWithoutNetwork".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAllowWithoutNetwork(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScaleMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScaleMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("showWelcomeAd".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.showWelcomeAd(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.impl.VirtualKeyboardInterface typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getVirtualKeyboardName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVirtualKeyboardName();
            }
        }
        if ("isVirtualKeyboardShowing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVirtualKeyboardShowing();
            }
        }
        if ("setInputType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setInputType(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("showKeyboard".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.showKeyboard(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.impl.VServAds.class) {
            if ("CAT_ID_ACTION_ADVENTURE".equals(name)) return com.codename1.impl.VServAds.CAT_ID_ACTION_ADVENTURE;
            if ("CAT_ID_ADULT".equals(name)) return com.codename1.impl.VServAds.CAT_ID_ADULT;
            if ("CAT_ID_ARCADE".equals(name)) return com.codename1.impl.VServAds.CAT_ID_ARCADE;
            if ("CAT_ID_EDUCATION".equals(name)) return com.codename1.impl.VServAds.CAT_ID_EDUCATION;
            if ("CAT_ID_ENTERTAINMENT".equals(name)) return com.codename1.impl.VServAds.CAT_ID_ENTERTAINMENT;
            if ("CAT_ID_HEALTH".equals(name)) return com.codename1.impl.VServAds.CAT_ID_HEALTH;
            if ("CAT_ID_KIDS".equals(name)) return com.codename1.impl.VServAds.CAT_ID_KIDS;
            if ("CAT_ID_MOVIES".equals(name)) return com.codename1.impl.VServAds.CAT_ID_MOVIES;
            if ("CAT_ID_MULTIMEDIA".equals(name)) return com.codename1.impl.VServAds.CAT_ID_MULTIMEDIA;
            if ("CAT_ID_OTHERS".equals(name)) return com.codename1.impl.VServAds.CAT_ID_OTHERS;
            if ("CAT_ID_PRODUCTIVITY".equals(name)) return com.codename1.impl.VServAds.CAT_ID_PRODUCTIVITY;
            if ("CAT_ID_RACING".equals(name)) return com.codename1.impl.VServAds.CAT_ID_RACING;
            if ("CAT_ID_SOCIAL_NETWORKING".equals(name)) return com.codename1.impl.VServAds.CAT_ID_SOCIAL_NETWORKING;
            if ("CAT_ID_SPIRITUAL".equals(name)) return com.codename1.impl.VServAds.CAT_ID_SPIRITUAL;
            if ("CAT_ID_SPORTS".equals(name)) return com.codename1.impl.VServAds.CAT_ID_SPORTS;
            if ("CAT_ID_STRATEGY".equals(name)) return com.codename1.impl.VServAds.CAT_ID_STRATEGY;
            if ("CAT_ID_TRAVEL".equals(name)) return com.codename1.impl.VServAds.CAT_ID_TRAVEL;
            if ("CAT_ID_UTILITY".equals(name)) return com.codename1.impl.VServAds.CAT_ID_UTILITY;
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
