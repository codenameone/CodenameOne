package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io {
    private GeneratedAccess_com_codename1_io() {
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
        if ("AccessToken".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.AccessToken");
            }
            return com.codename1.io.AccessToken.class;
        }
        if ("BufferedInputStream".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.BufferedInputStream");
            }
            return com.codename1.io.BufferedInputStream.class;
        }
        if ("BufferedOutputStream".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.BufferedOutputStream");
            }
            return com.codename1.io.BufferedOutputStream.class;
        }
        if ("CSVParser".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.CSVParser");
            }
            return com.codename1.io.CSVParser.class;
        }
        if ("CacheMap".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.CacheMap");
            }
            return com.codename1.io.CacheMap.class;
        }
        if ("CharArrayReader".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.CharArrayReader");
            }
            return com.codename1.io.CharArrayReader.class;
        }
        if ("ConnectionRequest".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.ConnectionRequest");
            }
            return com.codename1.io.ConnectionRequest.class;
        }
        if ("Cookie".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Cookie");
            }
            return com.codename1.io.Cookie.class;
        }
        if ("Data".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Data");
            }
            return com.codename1.io.Data.class;
        }
        if ("Externalizable".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Externalizable");
            }
            return com.codename1.io.Externalizable.class;
        }
        if ("File".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.File");
            }
            return com.codename1.io.File.class;
        }
        if ("FileSystemStorage".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.FileSystemStorage");
            }
            return com.codename1.io.FileSystemStorage.class;
        }
        if ("IOProgressListener".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.IOProgressListener");
            }
            return com.codename1.io.IOProgressListener.class;
        }
        if ("JSONParseCallback".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.JSONParseCallback");
            }
            return com.codename1.io.JSONParseCallback.class;
        }
        if ("JSONParser".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.JSONParser");
            }
            return com.codename1.io.JSONParser.class;
        }
        if ("Log".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Log");
            }
            return com.codename1.io.Log.class;
        }
        if ("MalformedURLException".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.MalformedURLException");
            }
            return com.codename1.io.MalformedURLException.class;
        }
        if ("MultipartRequest".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.MultipartRequest");
            }
            return com.codename1.io.MultipartRequest.class;
        }
        if ("NetworkEvent".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.NetworkEvent");
            }
            return com.codename1.io.NetworkEvent.class;
        }
        if ("NetworkManager".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.NetworkManager");
            }
            return com.codename1.io.NetworkManager.class;
        }
        if ("Oauth2".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Oauth2");
            }
            return com.codename1.io.Oauth2.class;
        }
        if ("PreferenceListener".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.PreferenceListener");
            }
            return com.codename1.io.PreferenceListener.class;
        }
        if ("Preferences".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Preferences");
            }
            return com.codename1.io.Preferences.class;
        }
        if ("Properties".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Properties");
            }
            return com.codename1.io.Properties.class;
        }
        if ("Socket".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Socket");
            }
            return com.codename1.io.Socket.class;
        }
        if ("SocketConnection".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.SocketConnection");
            }
            return com.codename1.io.SocketConnection.class;
        }
        if ("Storage".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Storage");
            }
            return com.codename1.io.Storage.class;
        }
        if ("URL".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.URL");
            }
            return com.codename1.io.URL.class;
        }
        if ("Util".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.Util");
            }
            return com.codename1.io.Util.class;
        }
        if ("WebServiceProxyCall".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.io -> com.codename1.io.WebServiceProxyCall");
            }
            return com.codename1.io.WebServiceProxyCall.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.AccessToken.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.AccessToken();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.AccessToken((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.AccessToken((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.AccessToken((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3]);
            }
        }
        if (type == com.codename1.io.CSVParser.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.CSVParser();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                return new com.codename1.io.CSVParser(((Character) safeArgs[0]).charValue());
            }
        }
        if (type == com.codename1.io.CacheMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.CacheMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.CacheMap((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.io.CharArrayReader.class) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return new com.codename1.io.CharArrayReader((char[]) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.io.CharArrayReader((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if (type == com.codename1.io.ConnectionRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.ConnectionRequest();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.ConnectionRequest((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.io.ConnectionRequest((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.io.File.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.File((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                return new com.codename1.io.File((com.codename1.io.File) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.File((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.io.MalformedURLException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.MalformedURLException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.MalformedURLException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.io.MultipartRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.MultipartRequest();
            }
        }
        if (type == com.codename1.io.NetworkEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Integer.class}, false)) {
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Exception.class}, false)) {
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) safeArgs[0], (java.lang.Exception) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Object.class}, false)) {
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) safeArgs[0], ((Number) safeArgs[1]).intValue(), (java.lang.String) safeArgs[2]);
            }
        }
        if (type == com.codename1.io.Oauth2.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.Oauth2((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.Oauth2((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.io.Oauth2((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Hashtable.class}, false)) {
                return new com.codename1.io.Oauth2((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.util.Hashtable) safeArgs[6]);
            }
        }
        if (type == com.codename1.io.Properties.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.Properties();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Properties.class}, false)) {
                return new com.codename1.io.Properties((com.codename1.io.Properties) safeArgs[0]);
            }
        }
        if (type == com.codename1.io.URL.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.URL((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.AccessToken.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.BufferedInputStream.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.io.BufferedOutputStream.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.io.ConnectionRequest.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.io.Cookie.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.io.File.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.io.FileSystemStorage.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.io.JSONParser.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.io.Log.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.io.MultipartRequest.class) return invokeStatic9(name, safeArgs);
        if (type == com.codename1.io.NetworkManager.class) return invokeStatic10(name, safeArgs);
        if (type == com.codename1.io.Oauth2.class) return invokeStatic11(name, safeArgs);
        if (type == com.codename1.io.Preferences.class) return invokeStatic12(name, safeArgs);
        if (type == com.codename1.io.Socket.class) return invokeStatic13(name, safeArgs);
        if (type == com.codename1.io.Storage.class) return invokeStatic14(name, safeArgs);
        if (type == com.codename1.io.Util.class) return invokeStatic15(name, safeArgs);
        if (type == com.codename1.io.WebServiceProxyCall.class) return invokeStatic16(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("createWithExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Date.class}, false)) {
                return com.codename1.io.AccessToken.createWithExpiryDate((java.lang.String) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.io.AccessToken.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getDefaultBufferSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.BufferedInputStream.getDefaultBufferSize();
            }
        }
        if ("setDefaultBufferSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.io.BufferedInputStream.setDefaultBufferSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.BufferedInputStream.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getDefaultBufferSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.BufferedOutputStream.getDefaultBufferSize();
            }
        }
        if ("setDefaultBufferSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.io.BufferedOutputStream.setDefaultBufferSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.BufferedOutputStream.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("fetchJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.ConnectionRequest.fetchJSON((java.lang.String) safeArgs[0]);
            }
        }
        if ("fetchJSONAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.ConnectionRequest.fetchJSONAsync((java.lang.String) safeArgs[0]);
            }
        }
        if ("getCookieHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.getCookieHeader();
            }
        }
        if ("getDefaultCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.getDefaultCacheMode();
            }
        }
        if ("getDefaultUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.getDefaultUserAgent();
            }
        }
        if ("isCookiesEnabledDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.isCookiesEnabledDefault();
            }
        }
        if ("isDefaultFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.isDefaultFollowRedirects();
            }
        }
        if ("isHandleErrorCodesInGlobalErrorHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.isHandleErrorCodesInGlobalErrorHandler();
            }
        }
        if ("isNativeCookieSharingSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.isNativeCookieSharingSupported();
            }
        }
        if ("isReadResponseForErrorsDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.isReadResponseForErrorsDefault();
            }
        }
        if ("isReadTimeoutSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.ConnectionRequest.isReadTimeoutSupported();
            }
        }
        if ("purgeCacheDirectory".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.ConnectionRequest.purgeCacheDirectory(); return null;
            }
        }
        if ("setCookieHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.ConnectionRequest.setCookieHeader((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCookiesEnabledDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.ConnectionRequest.setCookiesEnabledDefault(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                com.codename1.io.ConnectionRequest.setDefaultCacheMode((com.codename1.io.ConnectionRequest.CachingMode) safeArgs[0]); return null;
            }
        }
        if ("setDefaultFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.ConnectionRequest.setDefaultFollowRedirects(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.ConnectionRequest.setDefaultUserAgent((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setHandleErrorCodesInGlobalErrorHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.ConnectionRequest.setHandleErrorCodesInGlobalErrorHandler(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrorsDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.ConnectionRequest.setReadResponseForErrorsDefault(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseNativeCookieStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.ConnectionRequest.setUseNativeCookieStore(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.ConnectionRequest.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("clearCookiesFromStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.Cookie.clearCookiesFromStorage(); return null;
            }
        }
        if ("isAutoStored".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Cookie.isAutoStored();
            }
        }
        if ("setAutoStored".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.Cookie.setAutoStored(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Cookie.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("createTempFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.File.createTempFile((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("listRoots".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.File.listRoots();
            }
        }
        throw unsupportedStatic(com.codename1.io.File.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.FileSystemStorage.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.io.FileSystemStorage.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("isIncludeNulls".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.JSONParser.isIncludeNulls();
            }
        }
        if ("isUseBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.JSONParser.isUseBoolean();
            }
        }
        if ("isUseLongs".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.JSONParser.isUseLongs();
            }
        }
        if ("mapToJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return com.codename1.io.JSONParser.mapToJson((java.util.Map) safeArgs[0]);
            }
        }
        if ("setIncludeNulls".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.JSONParser.setIncludeNulls(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.JSONParser.setUseBoolean(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseLongs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.JSONParser.setUseLongs(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.JSONParser.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("bindCrashProtection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.Log.bindCrashProtection(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("deleteLog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.Log.deleteLog(); return null;
            }
        }
        if ("e".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                com.codename1.io.Log.e((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.getInstance();
            }
        }
        if ("getLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.getLevel();
            }
        }
        if ("getLogContent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.getLogContent();
            }
        }
        if ("getReportingLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.getReportingLevel();
            }
        }
        if ("getUniqueDeviceId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.getUniqueDeviceId();
            }
        }
        if ("getUniqueDeviceKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.getUniqueDeviceKey();
            }
        }
        if ("install".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Log.class}, false)) {
                com.codename1.io.Log.install((com.codename1.io.Log) safeArgs[0]); return null;
            }
        }
        if ("isCrashBound".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Log.isCrashBound();
            }
        }
        if ("p".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.Log.p((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("p".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                com.codename1.io.Log.p((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("sendLog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.Log.sendLog(); return null;
            }
        }
        if ("sendLogAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.Log.sendLogAsync(); return null;
            }
        }
        if ("setLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.io.Log.setLevel(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setReportingLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.io.Log.setReportingLevel(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("showLog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.Log.showLog(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Log.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("isLeaveInputStreamsOpen".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.MultipartRequest.isLeaveInputStreamsOpen();
            }
        }
        if ("setCanFlushStream".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.MultipartRequest.setCanFlushStream(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLeaveInputStreamsOpen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.MultipartRequest.setLeaveInputStreamsOpen(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.MultipartRequest.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("getAutoDetectURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.NetworkManager.getAutoDetectURL();
            }
        }
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.NetworkManager.getInstance();
            }
        }
        if ("setAutoDetectURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.NetworkManager.setAutoDetectURL((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.NetworkManager.class, name, safeArgs);
    }

    private static Object invokeStatic11(String name, Object[] safeArgs) throws Exception {
        if ("fetchSerializedOauth2Request".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Oauth2.fetchSerializedOauth2Request();
            }
        }
        if ("getExpires".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Oauth2.getExpires();
            }
        }
        if ("handleRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                return com.codename1.io.Oauth2.handleRedirect((com.codename1.ui.events.ActionListener) safeArgs[0]);
            }
        }
        if ("isBackToParent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Oauth2.isBackToParent();
            }
        }
        if ("setBackToParent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.io.Oauth2.setBackToParent(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Oauth2.class, name, safeArgs);
    }

    private static Object invokeStatic12(String name, Object[] safeArgs) throws Exception {
        if ("addPreferenceListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.PreferenceListener.class}, false)) {
                com.codename1.io.Preferences.addPreferenceListener((java.lang.String) safeArgs[0], (com.codename1.io.PreferenceListener) safeArgs[1]); return null;
            }
        }
        if ("clearAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.io.Preferences.clearAll(); return null;
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.Preferences.delete((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return com.codename1.io.Preferences.get((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                return com.codename1.io.Preferences.get((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                return com.codename1.io.Preferences.get((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return com.codename1.io.Preferences.get((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Preferences.get((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                return com.codename1.io.Preferences.get((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).longValue());
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return com.codename1.io.Preferences.getAndSet((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                return com.codename1.io.Preferences.getAndSet((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                return com.codename1.io.Preferences.getAndSet((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return com.codename1.io.Preferences.getAndSet((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Preferences.getAndSet((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                return com.codename1.io.Preferences.getAndSet((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).longValue());
            }
        }
        if ("getPreferencesLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Preferences.getPreferencesLocation();
            }
        }
        if ("removePreferenceListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.PreferenceListener.class}, false)) {
                return com.codename1.io.Preferences.removePreferenceListener((java.lang.String) safeArgs[0], (com.codename1.io.PreferenceListener) safeArgs[1]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                com.codename1.io.Preferences.set((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                com.codename1.io.Preferences.set((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                com.codename1.io.Preferences.set((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                com.codename1.io.Preferences.set((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).floatValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                com.codename1.io.Preferences.set((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.io.Preferences.set((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                com.codename1.io.Preferences.set((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).longValue()); return null;
            }
        }
        if ("setPreferencesLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.Preferences.setPreferencesLocation((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Preferences.class, name, safeArgs);
    }

    private static Object invokeStatic13(String name, Object[] safeArgs) throws Exception {
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, com.codename1.io.SocketConnection.class}, false)) {
                com.codename1.io.Socket.connect((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), (com.codename1.io.SocketConnection) safeArgs[2]); return null;
            }
        }
        if ("getHostOrIP".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Socket.getHostOrIP();
            }
        }
        if ("isServerSocketSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Socket.isServerSocketSupported();
            }
        }
        if ("isSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Socket.isSupported();
            }
        }
        if ("listen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Class.class}, false)) {
                return com.codename1.io.Socket.listen(((Number) safeArgs[0]).intValue(), (java.lang.Class) safeArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.io.Socket.class, name, safeArgs);
    }

    private static Object invokeStatic14(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Storage.getInstance();
            }
        }
        if ("isInitialized".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Storage.isInitialized();
            }
        }
        if ("setStorageInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Storage.class}, false)) {
                com.codename1.io.Storage.setStorageInstance((com.codename1.io.Storage) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Storage.class, name, safeArgs);
    }

    private static Object invokeStatic15(String name, Object[] safeArgs) throws Exception {
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                com.codename1.io.Util.cleanup((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                return com.codename1.io.Util.decode((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("downloadImageToCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.downloadImageToCache((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                com.codename1.io.Util.downloadImageToCache((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Util.downloadImageToFileSystem((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                com.codename1.io.Util.downloadImageToFileSystem((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.util.SuccessCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                com.codename1.io.Util.downloadImageToFileSystem((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.util.SuccessCallback) safeArgs[2], (com.codename1.util.FailureCallback) safeArgs[3]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Util.downloadImageToStorage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                com.codename1.io.Util.downloadImageToStorage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.util.SuccessCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                com.codename1.io.Util.downloadImageToStorage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.util.SuccessCallback) safeArgs[2], (com.codename1.util.FailureCallback) safeArgs[3]); return null;
            }
        }
        if ("downloadUrlSafely".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.OnComplete.class, com.codename1.util.OnComplete.class}, false)) {
                com.codename1.io.Util.downloadUrlSafely((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.util.OnComplete) safeArgs[2], (com.codename1.util.OnComplete) safeArgs[3]); return null;
            }
        }
        if ("downloadUrlToFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                return com.codename1.io.Util.downloadUrlToFile((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("downloadUrlToFileSystemInBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.io.Util.downloadUrlToFileSystemInBackground((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("downloadUrlToFileSystemInBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                com.codename1.io.Util.downloadUrlToFileSystemInBackground((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.ui.events.ActionListener) safeArgs[2]); return null;
            }
        }
        if ("downloadUrlToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                return com.codename1.io.Util.downloadUrlToStorage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("downloadUrlToStorageInBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.io.Util.downloadUrlToStorageInBackground((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("downloadUrlToStorageInBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                com.codename1.io.Util.downloadUrlToStorageInBackground((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (com.codename1.ui.events.ActionListener) safeArgs[2]); return null;
            }
        }
        if ("encodeBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.io.Util.encodeBody((byte[]) safeArgs[0]);
            }
        }
        if ("encodeBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return com.codename1.io.Util.encodeBody((char[]) safeArgs[0]);
            }
        }
        if ("encodeBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.encodeBody((java.lang.String) safeArgs[0]);
            }
        }
        if ("encodeUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.io.Util.encodeUrl((byte[]) safeArgs[0]);
            }
        }
        if ("encodeUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return com.codename1.io.Util.encodeUrl((char[]) safeArgs[0]);
            }
        }
        if ("encodeUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.encodeUrl((java.lang.String) safeArgs[0]);
            }
        }
        if ("encodeUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Util.encodeUrl((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getFileSizeWithoutDownload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.getFileSizeWithoutDownload((java.lang.String) safeArgs[0]);
            }
        }
        if ("getFileSizeWithoutDownload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return com.codename1.io.Util.getFileSizeWithoutDownload((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("getIgnorCharsWhileEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Util.getIgnorCharsWhileEncoding();
            }
        }
        if ("getURLBasePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.getURLBasePath((java.lang.String) safeArgs[0]);
            }
        }
        if ("getURLHost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.getURLHost((java.lang.String) safeArgs[0]);
            }
        }
        if ("getURLPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.getURLPath((java.lang.String) safeArgs[0]);
            }
        }
        if ("getURLProtocol".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.getURLProtocol((java.lang.String) safeArgs[0]);
            }
        }
        if ("getUUID".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.Util.getUUID();
            }
        }
        if ("getUUID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                return com.codename1.io.Util.getUUID(((Number) safeArgs[0]).longValue(), ((Number) safeArgs[1]).longValue());
            }
        }
        if ("guessMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.io.Util.guessMimeType((byte[]) safeArgs[0]);
            }
        }
        if ("guessMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.guessMimeType((java.lang.String) safeArgs[0]);
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false)) {
                return com.codename1.io.Util.indexOf((java.lang.Object[]) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("insertObjectAtOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Integer.class, java.lang.Object.class}, false)) {
                com.codename1.io.Util.insertObjectAtOffset((java.lang.Object[]) safeArgs[0], (java.lang.Object[]) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.Object) safeArgs[3]); return null;
            }
        }
        if ("instanceofByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofByteArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("instanceofDoubleArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofDoubleArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("instanceofFloatArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofFloatArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("instanceofIntArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofIntArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("instanceofLongArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofLongArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("instanceofObjArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofObjArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("instanceofShortArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.instanceofShortArray((java.lang.Object) safeArgs[0]);
            }
        }
        if ("mergeArrays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Object[].class}, false)) {
                com.codename1.io.Util.mergeArrays((java.lang.Object[]) safeArgs[0], (java.lang.Object[]) safeArgs[1], (java.lang.Object[]) safeArgs[2]); return null;
            }
        }
        if ("readToString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false)) {
                return com.codename1.io.Util.readToString((com.codename1.io.File) safeArgs[0]);
            }
        }
        if ("readToString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                return com.codename1.io.Util.readToString((com.codename1.io.File) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Externalizable.class}, false)) {
                com.codename1.io.Util.register((com.codename1.io.Externalizable) safeArgs[0]); return null;
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class}, false)) {
                com.codename1.io.Util.register((java.lang.String) safeArgs[0], (java.lang.Class) safeArgs[1]); return null;
            }
        }
        if ("relativeToAbsolute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Util.relativeToAbsolute((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("removeObjectAtOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Integer.class}, false)) {
                com.codename1.io.Util.removeObjectAtOffset((java.lang.Object[]) safeArgs[0], (java.lang.Object[]) safeArgs[1], ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("removeObjectAtOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Object.class}, false)) {
                com.codename1.io.Util.removeObjectAtOffset((java.lang.Object[]) safeArgs[0], (java.lang.Object[]) safeArgs[1], (java.lang.Object) safeArgs[2]); return null;
            }
        }
        if ("setDateFormatter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.l10n.SimpleDateFormat.class}, false)) {
                com.codename1.io.Util.setDateFormatter((com.codename1.l10n.SimpleDateFormat) safeArgs[0]); return null;
            }
        }
        if ("setIgnorCharsWhileEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.io.Util.setIgnorCharsWhileEncoding((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setImplementation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.impl.CodenameOneImplementation.class}, false)) {
                com.codename1.io.Util.setImplementation((com.codename1.impl.CodenameOneImplementation) safeArgs[0]); return null;
            }
        }
        if ("sleep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.io.Util.sleep(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("split".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.io.Util.split((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("toBooleanValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.toBooleanValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toCharArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.toCharArray((java.lang.String) safeArgs[0]);
            }
        }
        if ("toDateValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.toDateValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toDoubleValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.toDoubleValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toFloatValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.toFloatValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toIntValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.toIntValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toLongValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return com.codename1.io.Util.toLongValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("writeStringToFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                com.codename1.io.Util.writeStringToFile((com.codename1.io.File) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("writeStringToFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.io.Util.writeStringToFile((com.codename1.io.File) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("xorDecode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.xorDecode((java.lang.String) safeArgs[0]);
            }
        }
        if ("xorEncode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.io.Util.xorEncode((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.Util.class, name, safeArgs);
    }

    private static Object invokeStatic16(String name, Object[] safeArgs) throws Exception {
        if ("defineWebService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, int[].class}, true)) {
                int[] varArgs = new int[safeArgs.length - 3];
                for (int i = 3; i < safeArgs.length; i++) {
                    varArgs[i - 3] = ((Number) safeArgs[i]).intValue();
                }
                return com.codename1.io.WebServiceProxyCall.defineWebService((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Number) safeArgs[2]).intValue(), varArgs);
            }
        }
        if ("invokeWebserviceASync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, com.codename1.util.Callback.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 2];
                for (int i = 2; i < safeArgs.length; i++) {
                    varArgs[i - 2] = (java.lang.Object) safeArgs[i];
                }
                com.codename1.io.WebServiceProxyCall.invokeWebserviceASync((com.codename1.io.WebServiceProxyCall.WSDefinition) safeArgs[0], (com.codename1.util.Callback) safeArgs[1], varArgs); return null;
            }
        }
        if ("invokeWebserviceASync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 3];
                for (int i = 3; i < safeArgs.length; i++) {
                    varArgs[i - 3] = (java.lang.Object) safeArgs[i];
                }
                com.codename1.io.WebServiceProxyCall.invokeWebserviceASync((com.codename1.io.WebServiceProxyCall.WSDefinition) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], varArgs); return null;
            }
        }
        if ("invokeWebserviceSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) safeArgs[i];
                }
                return com.codename1.io.WebServiceProxyCall.invokeWebserviceSync((com.codename1.io.WebServiceProxyCall.WSDefinition) safeArgs[0], varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.io.WebServiceProxyCall.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.MultipartRequest) {
            try {
                return invoke0((com.codename1.io.MultipartRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.AccessToken) {
            try {
                return invoke1((com.codename1.io.AccessToken) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.BufferedInputStream) {
            try {
                return invoke2((com.codename1.io.BufferedInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.BufferedOutputStream) {
            try {
                return invoke3((com.codename1.io.BufferedOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.CacheMap) {
            try {
                return invoke4((com.codename1.io.CacheMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.CharArrayReader) {
            try {
                return invoke5((com.codename1.io.CharArrayReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.ConnectionRequest) {
            try {
                return invoke6((com.codename1.io.ConnectionRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Cookie) {
            try {
                return invoke7((com.codename1.io.Cookie) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.File) {
            try {
                return invoke8((com.codename1.io.File) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.FileSystemStorage) {
            try {
                return invoke9((com.codename1.io.FileSystemStorage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.JSONParser) {
            try {
                return invoke10((com.codename1.io.JSONParser) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Log) {
            try {
                return invoke11((com.codename1.io.Log) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.MalformedURLException) {
            try {
                return invoke12((com.codename1.io.MalformedURLException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.NetworkEvent) {
            try {
                return invoke13((com.codename1.io.NetworkEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.NetworkManager) {
            try {
                return invoke14((com.codename1.io.NetworkManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Oauth2) {
            try {
                return invoke15((com.codename1.io.Oauth2) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Properties) {
            try {
                return invoke16((com.codename1.io.Properties) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.SocketConnection) {
            try {
                return invoke17((com.codename1.io.SocketConnection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Storage) {
            try {
                return invoke18((com.codename1.io.Storage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.URL) {
            try {
                return invoke19((com.codename1.io.URL) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Data) {
            try {
                return invoke20((com.codename1.io.Data) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Externalizable) {
            try {
                return invoke21((com.codename1.io.Externalizable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.IOProgressListener) {
            try {
                return invoke22((com.codename1.io.IOProgressListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.JSONParseCallback) {
            try {
                return invoke23((com.codename1.io.JSONParseCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.PreferenceListener) {
            try {
                return invoke24((com.codename1.io.PreferenceListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.MultipartRequest typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("addData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.String.class}, false)) {
                typedTarget.addData((java.lang.String) safeArgs[0], (byte[]) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("addData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addData((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
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
        if ("getBoundary".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBoundary();
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
        if ("isBase64Binaries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBase64Binaries();
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
        if ("isManualRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isManualRedirect();
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
        if ("setBase64Binaries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setBase64Binaries(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setBoundary((java.lang.String) safeArgs[0]); return null;
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
        if ("setFilename".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.setFilename((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
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
        if ("setManualRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setManualRedirect(((Boolean) safeArgs[0]).booleanValue()); return null;
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

    private static Object invoke1(com.codename1.io.AccessToken typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getExpires".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getExpires();
            }
        }
        if ("getExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getExpiryDate();
            }
        }
        if ("getIdentityToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIdentityToken();
            }
        }
        if ("getObjectId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getObjectId();
            }
        }
        if ("getRefreshToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRefreshToken();
            }
        }
        if ("getToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getToken();
            }
        }
        if ("getVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVersion();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isExpired".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isExpired();
            }
        }
        if ("setExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                typedTarget.setExpiryDate((java.util.Date) safeArgs[0]); return null;
            }
        }
        if ("setIdentityToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setIdentityToken((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setRefreshToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRefreshToken((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.BufferedInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("getConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getConnection();
            }
        }
        if ("getLastActivityTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLastActivityTime();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getTotalBytesRead".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalBytesRead();
            }
        }
        if ("getYield".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getYield();
            }
        }
        if ("isDisableBuffering".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisableBuffering();
            }
        }
        if ("isPrintInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPrintInput();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.mark(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.read();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("setConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.setConnection((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("setDisableBuffering".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisableBuffering(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPrintInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPrintInput(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false)) {
                typedTarget.setProgressListener((com.codename1.io.IOProgressListener) safeArgs[0]); return null;
            }
        }
        if ("setYield".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setYield(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.skip(((Number) safeArgs[0]).longValue());
            }
        }
        if ("stop".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.BufferedOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flush(); return null;
            }
        }
        if ("flushBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flushBuffer(); return null;
            }
        }
        if ("getConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getConnection();
            }
        }
        if ("getLastActivityTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLastActivityTime();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getTotalBytesWritten".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalBytesWritten();
            }
        }
        if ("setConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.setConnection((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("setProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false)) {
                typedTarget.setProgressListener((com.codename1.io.IOProgressListener) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.write((byte[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.CacheMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearAllCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearAllCache(); return null;
            }
        }
        if ("clearMemoryCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearMemoryCache(); return null;
            }
        }
        if ("clearStorageCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearStorageCache(); return null;
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.delete((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.get((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCachePrefix".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCachePrefix();
            }
        }
        if ("getCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCacheSize();
            }
        }
        if ("getKeysInCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeysInCache();
            }
        }
        if ("getStorageCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStorageCacheSize();
            }
        }
        if ("isAlwaysStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAlwaysStore();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                typedTarget.put((java.lang.Object) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setAlwaysStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setAlwaysStore(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCachePrefix".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCachePrefix((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setCacheSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setStorageCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setStorageCacheSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.CharArrayReader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.mark(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.read();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return typedTarget.read((char[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.ready();
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.skip(((Number) safeArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.ConnectionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke7(com.codename1.io.Cookie typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDomain".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDomain();
            }
        }
        if ("getExpires".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getExpires();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getObjectId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getObjectId();
            }
        }
        if ("getPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPath();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValue();
            }
        }
        if ("getVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVersion();
            }
        }
        if ("isHttpOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHttpOnly();
            }
        }
        if ("isSecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSecure();
            }
        }
        if ("setDomain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDomain((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setExpires".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setExpires(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setHttpOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setHttpOnly(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setPath((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setSecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSecure(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setValue((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.io.File typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("canExecute".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canExecute();
            }
        }
        if ("createNewFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.createNewFile();
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.delete();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.exists();
            }
        }
        if ("getAbsoluteFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAbsoluteFile();
            }
        }
        if ("getAbsolutePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAbsolutePath();
            }
        }
        if ("getFreeSpace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFreeSpace();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getParent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getParent();
            }
        }
        if ("getParentFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getParentFile();
            }
        }
        if ("getPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPath();
            }
        }
        if ("getTotalSpace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalSpace();
            }
        }
        if ("getUsableSpace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUsableSpace();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isAbsolute".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAbsolute();
            }
        }
        if ("isDirectory".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDirectory();
            }
        }
        if ("isFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFile();
            }
        }
        if ("isHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isHidden();
            }
        }
        if ("lastModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.lastModified();
            }
        }
        if ("length".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.length();
            }
        }
        if ("list".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.list();
            }
        }
        if ("list".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.FilenameFilter.class}, false)) {
                return typedTarget.list((com.codename1.io.File.FilenameFilter) safeArgs[0]);
            }
        }
        if ("listFiles".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listFiles();
            }
        }
        if ("listFiles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.FileFilter.class}, false)) {
                return typedTarget.listFiles((com.codename1.io.File.FileFilter) safeArgs[0]);
            }
        }
        if ("listFiles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.FilenameFilter.class}, false)) {
                return typedTarget.listFiles((com.codename1.io.File.FilenameFilter) safeArgs[0]);
            }
        }
        if ("mkdir".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mkdir();
            }
        }
        if ("mkdirs".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mkdirs();
            }
        }
        if ("renameTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false)) {
                return typedTarget.renameTo((com.codename1.io.File) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("toURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toURL();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.io.FileSystemStorage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.delete((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("deleteRetry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                typedTarget.deleteRetry((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.exists((java.lang.String) safeArgs[0]);
            }
        }
        if ("getAppHomePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAppHomePath();
            }
        }
        if ("getCachesDir".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCachesDir();
            }
        }
        if ("getFileSystemSeparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFileSystemSeparator();
            }
        }
        if ("getLastModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getLastModified((java.lang.String) safeArgs[0]);
            }
        }
        if ("getLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getLength((java.lang.String) safeArgs[0]);
            }
        }
        if ("getRootAvailableSpace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getRootAvailableSpace((java.lang.String) safeArgs[0]);
            }
        }
        if ("getRootSizeBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getRootSizeBytes((java.lang.String) safeArgs[0]);
            }
        }
        if ("getRootType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getRootType((java.lang.String) safeArgs[0]);
            }
        }
        if ("getRoots".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRoots();
            }
        }
        if ("hasCachesDir".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasCachesDir();
            }
        }
        if ("isDirectory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isDirectory((java.lang.String) safeArgs[0]);
            }
        }
        if ("isHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isHidden((java.lang.String) safeArgs[0]);
            }
        }
        if ("listFiles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.listFiles((java.lang.String) safeArgs[0]);
            }
        }
        if ("mkdir".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.mkdir((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("rename".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.rename((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                typedTarget.setHidden((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("toNativePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.toNativePath((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.io.JSONParser typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("booleanToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.booleanToken(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("endArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.endArray((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("endBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.endBlock((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("isAlive".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAlive();
            }
        }
        if ("isIncludeNullsInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isIncludeNullsInstance();
            }
        }
        if ("isStrict".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isStrict();
            }
        }
        if ("isUseBooleanInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUseBooleanInstance();
            }
        }
        if ("isUseLongsInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUseLongsInstance();
            }
        }
        if ("keyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.keyValue((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("longToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.longToken(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("numericToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.numericToken(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("setIncludeNullsInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setIncludeNullsInstance(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStrict".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setStrict(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseBooleanInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setUseBooleanInstance(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseLongsInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setUseLongsInstance(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("startArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.startArray((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("startBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.startBlock((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("stringToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.stringToken((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.io.Log typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFileURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFileURL();
            }
        }
        if ("isFileWriteEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFileWriteEnabled();
            }
        }
        if ("setFileURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setFileURL((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setFileWriteEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFileWriteEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("trackFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.trackFileSystem(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.io.MalformedURLException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.addSuppressed((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return typedTarget.initCause((java.lang.Throwable) safeArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.io.NetworkEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComponent();
            }
        }
        if ("getConnectionRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getConnectionRequest();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getError".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getError();
            }
        }
        if ("getEventType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLength();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getMetaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMetaData();
            }
        }
        if ("getProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgress();
            }
        }
        if ("getProgressPercentage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgressPercentage();
            }
        }
        if ("getProgressType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProgressType();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getSentReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSentReceived();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSource();
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
        if ("isConsumed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Exception.class}, false)) {
                typedTarget.setError((java.lang.Exception) safeArgs[0]); return null;
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPointerPressedDuringDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.io.NetworkManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addDefaultHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addDefaultHeader((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addErrorListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addProgressListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addToQueue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                typedTarget.addToQueue((com.codename1.io.ConnectionRequest) safeArgs[0]); return null;
            }
        }
        if ("addToQueueAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                typedTarget.addToQueueAndWait((com.codename1.io.ConnectionRequest) safeArgs[0]); return null;
            }
        }
        if ("addToQueueAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                return typedTarget.addToQueueAsync((com.codename1.io.ConnectionRequest) safeArgs[0]);
            }
        }
        if ("assignToThread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.Integer.class}, false)) {
                typedTarget.assignToThread((java.lang.Class) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("enumurateQueue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.enumurateQueue();
            }
        }
        if ("getAPIds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAPIds();
            }
        }
        if ("getAPName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getAPName((java.lang.String) safeArgs[0]);
            }
        }
        if ("getAPType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getAPType((java.lang.String) safeArgs[0]);
            }
        }
        if ("getCurrentAccessPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCurrentAccessPoint();
            }
        }
        if ("getThreadCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThreadCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeout();
            }
        }
        if ("isAPSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAPSupported();
            }
        }
        if ("isQueueIdle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isQueueIdle();
            }
        }
        if ("isVPNActive".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isVPNActive();
            }
        }
        if ("isVPNDetectionSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isVPNDetectionSupported();
            }
        }
        if ("killAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                typedTarget.killAndWait((com.codename1.io.ConnectionRequest) safeArgs[0]); return null;
            }
        }
        if ("removeErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeErrorListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeProgressListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("setCurrentAccessPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCurrentAccessPoint((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setThreadCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setThreadCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("shutdown".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.shutdown(); return null;
            }
        }
        if ("shutdownSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.shutdownSync(); return null;
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.start(); return null;
            }
        }
        if ("updateThreadCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.updateThreadCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.io.Oauth2 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authenticate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.authenticate();
            }
        }
        if ("createAuthComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                return typedTarget.createAuthComponent((com.codename1.ui.events.ActionListener) safeArgs[0]);
            }
        }
        if ("isUseBrowserWindow".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUseBrowserWindow();
            }
        }
        if ("isUseRedirectForWeb".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUseRedirectForWeb();
            }
        }
        if ("refreshToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.refreshToken((java.lang.String) safeArgs[0]);
            }
        }
        if ("setUseBrowserWindow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setUseBrowserWindow(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseRedirectForWeb".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setUseRedirectForWeb(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("showAuthentication".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.showAuthentication((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.io.Properties typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.getProperty((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
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
        if ("propertyNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.propertyNames();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("setProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.setProperty((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("stringPropertyNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.stringPropertyNames();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.io.SocketConnection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connectionError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                typedTarget.connectionError(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("getConnectTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getConnectTimeout();
            }
        }
        if ("isConnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isConnected();
            }
        }
        if ("setConnectTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setConnectTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.io.Storage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearCache(); return null;
            }
        }
        if ("clearStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearStorage(); return null;
            }
        }
        if ("deleteStorageFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.deleteStorageFile((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("entrySize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.entrySize((java.lang.String) safeArgs[0]);
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.exists((java.lang.String) safeArgs[0]);
            }
        }
        if ("flushStorageCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flushStorageCache(); return null;
            }
        }
        if ("isNormalizeNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isNormalizeNames();
            }
        }
        if ("listEntries".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listEntries();
            }
        }
        if ("readObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.readObject((java.lang.String) safeArgs[0]);
            }
        }
        if ("readObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.readObject((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("setHardCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setHardCacheSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setNormalizeNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setNormalizeNames(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("writeObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                return typedTarget.writeObject((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("writeObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                return typedTarget.writeObject((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.io.URL typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getAuthority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAuthority();
            }
        }
        if ("getDefaultPort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultPort();
            }
        }
        if ("getFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFile();
            }
        }
        if ("getHost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHost();
            }
        }
        if ("getPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPath();
            }
        }
        if ("getPort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPort();
            }
        }
        if ("getProtocol".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getProtocol();
            }
        }
        if ("getQuery".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getQuery();
            }
        }
        if ("getUserInfo".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserInfo();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("openConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.openConnection();
            }
        }
        if ("sameFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.URL.class}, false)) {
                return typedTarget.sameFile((com.codename1.io.URL) safeArgs[0]);
            }
        }
        if ("toExternalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toExternalForm();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.io.Data typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSize();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.io.Externalizable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getObjectId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getObjectId();
            }
        }
        if ("getVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVersion();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.io.IOProgressListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                typedTarget.ioStreamUpdate((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.io.JSONParseCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("booleanToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.booleanToken(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("endArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.endArray((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("endBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.endBlock((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("isAlive".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isAlive();
            }
        }
        if ("keyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.keyValue((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("longToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.longToken(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("numericToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.numericToken(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("startArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.startArray((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("startBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.startBlock((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("stringToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.stringToken((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.io.PreferenceListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("preferenceChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Object.class}, false)) {
                typedTarget.preferenceChanged((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1], (java.lang.Object) safeArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.ConnectionRequest.class) {
            if ("PRIORITY_CRITICAL".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_CRITICAL;
            if ("PRIORITY_HIGH".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_HIGH;
            if ("PRIORITY_LOW".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_LOW;
            if ("PRIORITY_NORMAL".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_NORMAL;
            if ("PRIORITY_REDUNDANT".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_REDUNDANT;
        }
        if (type == com.codename1.io.Cookie.class) {
            if ("STORAGE_NAME".equals(name)) return com.codename1.io.Cookie.STORAGE_NAME;
        }
        if (type == com.codename1.io.File.class) {
            if ("separator".equals(name)) return com.codename1.io.File.separator;
            if ("separatorChar".equals(name)) return com.codename1.io.File.separatorChar;
        }
        if (type == com.codename1.io.FileSystemStorage.class) {
            if ("ROOT_TYPE_MAINSTORAGE".equals(name)) return com.codename1.io.FileSystemStorage.ROOT_TYPE_MAINSTORAGE;
            if ("ROOT_TYPE_SDCARD".equals(name)) return com.codename1.io.FileSystemStorage.ROOT_TYPE_SDCARD;
            if ("ROOT_TYPE_UNKNOWN".equals(name)) return com.codename1.io.FileSystemStorage.ROOT_TYPE_UNKNOWN;
        }
        if (type == com.codename1.io.Log.class) {
            if ("DEBUG".equals(name)) return com.codename1.io.Log.DEBUG;
            if ("ERROR".equals(name)) return com.codename1.io.Log.ERROR;
            if ("INFO".equals(name)) return com.codename1.io.Log.INFO;
            if ("REPORTING_DEBUG".equals(name)) return com.codename1.io.Log.REPORTING_DEBUG;
            if ("REPORTING_NONE".equals(name)) return com.codename1.io.Log.REPORTING_NONE;
            if ("REPORTING_PRODUCTION".equals(name)) return com.codename1.io.Log.REPORTING_PRODUCTION;
            if ("WARNING".equals(name)) return com.codename1.io.Log.WARNING;
        }
        if (type == com.codename1.io.NetworkEvent.class) {
            if ("PROGRESS_TYPE_COMPLETED".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_COMPLETED;
            if ("PROGRESS_TYPE_INITIALIZING".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_INITIALIZING;
            if ("PROGRESS_TYPE_INPUT".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_INPUT;
            if ("PROGRESS_TYPE_OUTPUT".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_OUTPUT;
        }
        if (type == com.codename1.io.NetworkManager.class) {
            if ("ACCESS_POINT_TYPE_CABLE".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_CABLE;
            if ("ACCESS_POINT_TYPE_CORPORATE".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_CORPORATE;
            if ("ACCESS_POINT_TYPE_NETWORK2G".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_NETWORK2G;
            if ("ACCESS_POINT_TYPE_NETWORK3G".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_NETWORK3G;
            if ("ACCESS_POINT_TYPE_UNKNOWN".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
            if ("ACCESS_POINT_TYPE_WLAN".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_WLAN;
        }
        if (type == com.codename1.io.Oauth2.class) {
            if ("TOKEN".equals(name)) return com.codename1.io.Oauth2.TOKEN;
        }
        if (type == com.codename1.io.WebServiceProxyCall.class) {
            if ("TYPE_BOOLEAN".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_BOOLEAN;
            if ("TYPE_BOOLEAN_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_BOOLEAN_ARRAY;
            if ("TYPE_BOOLEAN_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_BOOLEAN_OBJECT;
            if ("TYPE_BYTE".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_BYTE;
            if ("TYPE_BYTE_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_BYTE_ARRAY;
            if ("TYPE_BYTE_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_BYTE_OBJECT;
            if ("TYPE_CHAR".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_CHAR;
            if ("TYPE_CHARACTER_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_CHARACTER_OBJECT;
            if ("TYPE_CHAR_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_CHAR_ARRAY;
            if ("TYPE_DOUBLE".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_DOUBLE;
            if ("TYPE_DOUBLE_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_DOUBLE_ARRAY;
            if ("TYPE_DOUBLE_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_DOUBLE_OBJECT;
            if ("TYPE_EXTERNALIABLE".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_EXTERNALIABLE;
            if ("TYPE_FLOAT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_FLOAT;
            if ("TYPE_FLOAT_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_FLOAT_ARRAY;
            if ("TYPE_FLOAT_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_FLOAT_OBJECT;
            if ("TYPE_INT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_INT;
            if ("TYPE_INTEGER_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_INTEGER_OBJECT;
            if ("TYPE_INT_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_INT_ARRAY;
            if ("TYPE_LONG".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_LONG;
            if ("TYPE_LONG_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_LONG_ARRAY;
            if ("TYPE_LONG_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_LONG_OBJECT;
            if ("TYPE_SHORT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_SHORT;
            if ("TYPE_SHORT_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_SHORT_ARRAY;
            if ("TYPE_SHORT_OBJECT".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_SHORT_OBJECT;
            if ("TYPE_STRING".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_STRING;
            if ("TYPE_STRING_ARRAY".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_STRING_ARRAY;
            if ("TYPE_VOID".equals(name)) return com.codename1.io.WebServiceProxyCall.TYPE_VOID;
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
