package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_social {
    private GeneratedAccess_com_codename1_social() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.social.FacebookConnect".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.social -> com.codename1.social.FacebookConnect");
            }
            return com.codename1.social.FacebookConnect.class;
        }
        if ("com.codename1.social.GoogleConnect".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.social -> com.codename1.social.GoogleConnect");
            }
            return com.codename1.social.GoogleConnect.class;
        }
        if ("com.codename1.social.Login".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.social -> com.codename1.social.Login");
            }
            return com.codename1.social.Login.class;
        }
        if ("com.codename1.social.LoginCallback".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.social -> com.codename1.social.LoginCallback");
            }
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.social.FacebookConnect.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.social.FacebookConnect.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
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
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.addScopes(varArgs);
            }
        }
        if ("askPublishPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.askPublishPermissions((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.connect();
            }
        }
        if ("doLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.doLogin(); return null;
            }
        }
        if ("doLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.doLogin((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("doLogout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.doLogout(); return null;
            }
        }
        if ("getAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAccessToken();
            }
        }
        if ("getToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getToken();
            }
        }
        if ("hasPublishPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasPublishPermissions();
            }
        }
        if ("inviteFriends".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.inviteFriends((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("inviteFriends".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.Callback.class}, false)) {
                typedTarget.inviteFriends((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.util.Callback) safeArgs[2]); return null;
            }
        }
        if ("isFacebookSDKSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFacebookSDKSupported();
            }
        }
        if ("isInviteFriendsSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInviteFriendsSupported();
            }
        }
        if ("isLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLoggedIn();
            }
        }
        if ("isNativeLoginSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isNativeLoginSupported();
            }
        }
        if ("isPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPreferRedirectPrompt();
            }
        }
        if ("isUserLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUserLoggedIn();
            }
        }
        if ("login".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.login(); return null;
            }
        }
        if ("logout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.logout(); return null;
            }
        }
        if ("nativeIsLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nativeIsLoggedIn();
            }
        }
        if ("nativeLogout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.nativeLogout(); return null;
            }
        }
        if ("nativelogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.nativelogin(); return null;
            }
        }
        if ("setAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false)) {
                typedTarget.setAccessToken((com.codename1.io.AccessToken) safeArgs[0]); return null;
            }
        }
        if ("setCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.setCallback((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setClientId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setClientSecret((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOauth2URL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setOauth2URL((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPreferRedirectPrompt(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRedirectURI((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setScope".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setScope((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("validateToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.validateToken(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.social.GoogleConnect typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addScopes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.addScopes(varArgs);
            }
        }
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.connect();
            }
        }
        if ("doLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.doLogin(); return null;
            }
        }
        if ("doLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.doLogin((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("doLogout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.doLogout(); return null;
            }
        }
        if ("getAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAccessToken();
            }
        }
        if ("isNativeLoginSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isNativeLoginSupported();
            }
        }
        if ("isPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPreferRedirectPrompt();
            }
        }
        if ("isUserLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUserLoggedIn();
            }
        }
        if ("nativeIsLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nativeIsLoggedIn();
            }
        }
        if ("nativeLogout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.nativeLogout(); return null;
            }
        }
        if ("nativelogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.nativelogin(); return null;
            }
        }
        if ("setAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false)) {
                typedTarget.setAccessToken((com.codename1.io.AccessToken) safeArgs[0]); return null;
            }
        }
        if ("setCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.setCallback((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setClientId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setClientSecret((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOauth2URL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setOauth2URL((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPreferRedirectPrompt(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRedirectURI((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setScope".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setScope((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("validateToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.validateToken(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.social.Login typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addScopes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.addScopes(varArgs);
            }
        }
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.connect();
            }
        }
        if ("doLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.doLogin(); return null;
            }
        }
        if ("doLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.doLogin((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("doLogout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.doLogout(); return null;
            }
        }
        if ("getAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAccessToken();
            }
        }
        if ("isNativeLoginSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isNativeLoginSupported();
            }
        }
        if ("isPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPreferRedirectPrompt();
            }
        }
        if ("isUserLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUserLoggedIn();
            }
        }
        if ("nativeIsLoggedIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nativeIsLoggedIn();
            }
        }
        if ("nativeLogout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.nativeLogout(); return null;
            }
        }
        if ("nativelogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.nativelogin(); return null;
            }
        }
        if ("setAccessToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.AccessToken.class}, false)) {
                typedTarget.setAccessToken((com.codename1.io.AccessToken) safeArgs[0]); return null;
            }
        }
        if ("setCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.social.LoginCallback.class}, false)) {
                typedTarget.setCallback((com.codename1.social.LoginCallback) safeArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setClientId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setClientSecret((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOauth2URL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setOauth2URL((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPreferRedirectPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPreferRedirectPrompt(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRedirectURI((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setScope".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setScope((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("validateToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.validateToken(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.social.LoginCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("loginFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.loginFailed((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("loginSuccessful".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
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
