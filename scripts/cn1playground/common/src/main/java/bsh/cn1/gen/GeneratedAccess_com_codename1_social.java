package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_social {
    private GeneratedAccess_com_codename1_social() {
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
        if ("FacebookConnect".equals(simpleName)) {
            return com.codename1.social.FacebookConnect.class;
        }
        if ("GoogleConnect".equals(simpleName)) {
            return com.codename1.social.GoogleConnect.class;
        }
        if ("Login".equals(simpleName)) {
            return com.codename1.social.Login.class;
        }
        if ("LoginCallback".equals(simpleName)) {
            return com.codename1.social.LoginCallback.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.social.FacebookConnect.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.social.GoogleConnect.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.social.FacebookConnect.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.social.FacebookConnect.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.social.GoogleConnect.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.social.GoogleConnect.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.social.FacebookConnect) {
            try {
                return invoke0((com.codename1.social.FacebookConnect) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.social.GoogleConnect) {
            try {
                return invoke1((com.codename1.social.GoogleConnect) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.social.Login) {
            try {
                return invoke2((com.codename1.social.Login) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.social.LoginCallback) {
            try {
                return invoke3((com.codename1.social.LoginCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.social.FacebookConnect typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addScopes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.addScopes(varArgs);
            }
        }
        if ("askPublishPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.askPublishPermissions((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("connect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.connect();
            }
        }
        if ("doLogin".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.doLogin(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.doLogin((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("doLogout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.doLogout(); return null;
            }
        }
        if ("getAccessToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessToken();
            }
        }
        if ("getToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToken();
            }
        }
        if ("hasPublishPermissions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasPublishPermissions();
            }
        }
        if ("inviteFriends".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.inviteFriends((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.Callback.class}, false);
                typedTarget.inviteFriends((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.Callback) adaptedArgs[2]); return null;
            }
        }
        if ("isFacebookSDKSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFacebookSDKSupported();
            }
        }
        if ("isInviteFriendsSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInviteFriendsSupported();
            }
        }
        if ("isLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoggedIn();
            }
        }
        if ("isNativeLoginSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeLoginSupported();
            }
        }
        if ("isPreferRedirectPrompt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPreferRedirectPrompt();
            }
        }
        if ("isUserLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUserLoggedIn();
            }
        }
        if ("login".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.login(); return null;
            }
        }
        if ("logout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.logout(); return null;
            }
        }
        if ("nativeIsLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nativeIsLoggedIn();
            }
        }
        if ("nativeLogout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.nativeLogout(); return null;
            }
        }
        if ("nativelogin".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.nativelogin(); return null;
            }
        }
        if ("setAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false);
                typedTarget.setAccessToken((com.codename1.io.AccessToken) adaptedArgs[0]); return null;
            }
        }
        if ("setCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.setCallback((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setClientId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setClientSecret((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOauth2URL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setOauth2URL((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPreferRedirectPrompt(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRedirectURI((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setScope".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setScope((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("validateToken".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validateToken(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.social.GoogleConnect typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addScopes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.addScopes(varArgs);
            }
        }
        if ("connect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.connect();
            }
        }
        if ("doLogin".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.doLogin(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.doLogin((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("doLogout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.doLogout(); return null;
            }
        }
        if ("getAccessToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessToken();
            }
        }
        if ("isNativeLoginSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeLoginSupported();
            }
        }
        if ("isPreferRedirectPrompt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPreferRedirectPrompt();
            }
        }
        if ("isUserLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUserLoggedIn();
            }
        }
        if ("nativeIsLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nativeIsLoggedIn();
            }
        }
        if ("nativeLogout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.nativeLogout(); return null;
            }
        }
        if ("nativelogin".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.nativelogin(); return null;
            }
        }
        if ("setAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false);
                typedTarget.setAccessToken((com.codename1.io.AccessToken) adaptedArgs[0]); return null;
            }
        }
        if ("setCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.setCallback((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setClientId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setClientSecret((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOauth2URL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setOauth2URL((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPreferRedirectPrompt(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRedirectURI((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setScope".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setScope((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("validateToken".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validateToken(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.social.Login typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addScopes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.addScopes(varArgs);
            }
        }
        if ("connect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.connect();
            }
        }
        if ("doLogin".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.doLogin(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.doLogin((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("doLogout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.doLogout(); return null;
            }
        }
        if ("getAccessToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessToken();
            }
        }
        if ("isNativeLoginSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeLoginSupported();
            }
        }
        if ("isPreferRedirectPrompt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPreferRedirectPrompt();
            }
        }
        if ("isUserLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUserLoggedIn();
            }
        }
        if ("nativeIsLoggedIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nativeIsLoggedIn();
            }
        }
        if ("nativeLogout".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.nativeLogout(); return null;
            }
        }
        if ("nativelogin".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.nativelogin(); return null;
            }
        }
        if ("setAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false);
                typedTarget.setAccessToken((com.codename1.io.AccessToken) adaptedArgs[0]); return null;
            }
        }
        if ("setCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false);
                typedTarget.setCallback((com.codename1.social.LoginCallback) adaptedArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setClientId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setClientSecret((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOauth2URL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setOauth2URL((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPreferRedirectPrompt(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRedirectURI((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setScope".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setScope((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("validateToken".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validateToken(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.social.LoginCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("loginFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.loginFailed((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("loginSuccessful".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.loginSuccessful(); return null;
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
