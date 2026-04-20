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
            return com.codename1.io.AccessToken.class;
        }
        if ("BufferedInputStream".equals(simpleName)) {
            return com.codename1.io.BufferedInputStream.class;
        }
        if ("BufferedOutputStream".equals(simpleName)) {
            return com.codename1.io.BufferedOutputStream.class;
        }
        if ("CSVParser".equals(simpleName)) {
            return com.codename1.io.CSVParser.class;
        }
        if ("CacheMap".equals(simpleName)) {
            return com.codename1.io.CacheMap.class;
        }
        if ("CharArrayReader".equals(simpleName)) {
            return com.codename1.io.CharArrayReader.class;
        }
        if ("ConnectionRequest".equals(simpleName)) {
            return com.codename1.io.ConnectionRequest.class;
        }
        if ("CachingMode".equals(simpleName)) {
            return com.codename1.io.ConnectionRequest.CachingMode.class;
        }
        if ("SSLCertificate".equals(simpleName)) {
            return com.codename1.io.ConnectionRequest.SSLCertificate.class;
        }
        if ("Cookie".equals(simpleName)) {
            return com.codename1.io.Cookie.class;
        }
        if ("Data".equals(simpleName)) {
            return com.codename1.io.Data.class;
        }
        if ("Externalizable".equals(simpleName)) {
            return com.codename1.io.Externalizable.class;
        }
        if ("File".equals(simpleName)) {
            return com.codename1.io.File.class;
        }
        if ("FileFilter".equals(simpleName)) {
            return com.codename1.io.File.FileFilter.class;
        }
        if ("FilenameFilter".equals(simpleName)) {
            return com.codename1.io.File.FilenameFilter.class;
        }
        if ("FileSystemStorage".equals(simpleName)) {
            return com.codename1.io.FileSystemStorage.class;
        }
        if ("IOProgressListener".equals(simpleName)) {
            return com.codename1.io.IOProgressListener.class;
        }
        if ("JSONParseCallback".equals(simpleName)) {
            return com.codename1.io.JSONParseCallback.class;
        }
        if ("JSONParser".equals(simpleName)) {
            return com.codename1.io.JSONParser.class;
        }
        if ("Log".equals(simpleName)) {
            return com.codename1.io.Log.class;
        }
        if ("MalformedURLException".equals(simpleName)) {
            return com.codename1.io.MalformedURLException.class;
        }
        if ("MultipartRequest".equals(simpleName)) {
            return com.codename1.io.MultipartRequest.class;
        }
        if ("NetworkEvent".equals(simpleName)) {
            return com.codename1.io.NetworkEvent.class;
        }
        if ("NetworkManager".equals(simpleName)) {
            return com.codename1.io.NetworkManager.class;
        }
        if ("Oauth2".equals(simpleName)) {
            return com.codename1.io.Oauth2.class;
        }
        if ("RefreshTokenRequest".equals(simpleName)) {
            return com.codename1.io.Oauth2.RefreshTokenRequest.class;
        }
        if ("PreferenceListener".equals(simpleName)) {
            return com.codename1.io.PreferenceListener.class;
        }
        if ("Preferences".equals(simpleName)) {
            return com.codename1.io.Preferences.class;
        }
        if ("Properties".equals(simpleName)) {
            return com.codename1.io.Properties.class;
        }
        if ("Socket".equals(simpleName)) {
            return com.codename1.io.Socket.class;
        }
        if ("StopListening".equals(simpleName)) {
            return com.codename1.io.Socket.StopListening.class;
        }
        if ("SocketConnection".equals(simpleName)) {
            return com.codename1.io.SocketConnection.class;
        }
        if ("Storage".equals(simpleName)) {
            return com.codename1.io.Storage.class;
        }
        if ("URL".equals(simpleName)) {
            return com.codename1.io.URL.class;
        }
        if ("Util".equals(simpleName)) {
            return com.codename1.io.Util.class;
        }
        if ("WebServiceProxyCall".equals(simpleName)) {
            return com.codename1.io.WebServiceProxyCall.class;
        }
        if ("WSDefinition".equals(simpleName)) {
            return com.codename1.io.WebServiceProxyCall.WSDefinition.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.AccessToken.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.AccessToken();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.AccessToken((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.AccessToken((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.AccessToken((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.io.CSVParser.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.CSVParser();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return new com.codename1.io.CSVParser(((Character) adaptedArgs[0]).charValue());
            }
        }
        if (type == com.codename1.io.CacheMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.CacheMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.CacheMap((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.io.CharArrayReader.class) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return new com.codename1.io.CharArrayReader((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.io.CharArrayReader((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if (type == com.codename1.io.ConnectionRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.ConnectionRequest();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.ConnectionRequest((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.io.ConnectionRequest((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.io.File.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.File((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false);
                return new com.codename1.io.File((com.codename1.io.File) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.File((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.io.MalformedURLException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.MalformedURLException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.MalformedURLException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.io.MultipartRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.MultipartRequest();
            }
        }
        if (type == com.codename1.io.NetworkEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Exception.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Exception.class}, false);
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) adaptedArgs[0], (java.lang.Exception) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Integer.class}, false);
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Object.class}, false);
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.io.NetworkEvent((com.codename1.io.ConnectionRequest) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.io.Oauth2.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.Oauth2((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.Oauth2((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.Oauth2((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.util.Hashtable.class}, false);
                return new com.codename1.io.Oauth2((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (java.util.Hashtable) adaptedArgs[6]);
            }
        }
        if (type == com.codename1.io.Properties.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.Properties();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Properties.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Properties.class}, false);
                return new com.codename1.io.Properties((com.codename1.io.Properties) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.io.URL.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.URL((java.lang.String) adaptedArgs[0]);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Date.class}, false);
                return com.codename1.io.AccessToken.createWithExpiryDate((java.lang.String) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.io.AccessToken.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getDefaultBufferSize".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.BufferedInputStream.getDefaultBufferSize();
            }
        }
        if ("setDefaultBufferSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.io.BufferedInputStream.setDefaultBufferSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.BufferedInputStream.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getDefaultBufferSize".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.BufferedOutputStream.getDefaultBufferSize();
            }
        }
        if ("setDefaultBufferSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.io.BufferedOutputStream.setDefaultBufferSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.BufferedOutputStream.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("fetchJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.ConnectionRequest.fetchJSON((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("fetchJSONAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.ConnectionRequest.fetchJSONAsync((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCookieHeader".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.getCookieHeader();
            }
        }
        if ("getDefaultCacheMode".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.getDefaultCacheMode();
            }
        }
        if ("getDefaultUserAgent".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.getDefaultUserAgent();
            }
        }
        if ("isCookiesEnabledDefault".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.isCookiesEnabledDefault();
            }
        }
        if ("isDefaultFollowRedirects".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.isDefaultFollowRedirects();
            }
        }
        if ("isHandleErrorCodesInGlobalErrorHandler".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.isHandleErrorCodesInGlobalErrorHandler();
            }
        }
        if ("isNativeCookieSharingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.isNativeCookieSharingSupported();
            }
        }
        if ("isReadResponseForErrorsDefault".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.isReadResponseForErrorsDefault();
            }
        }
        if ("isReadTimeoutSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.ConnectionRequest.isReadTimeoutSupported();
            }
        }
        if ("purgeCacheDirectory".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.ConnectionRequest.purgeCacheDirectory(); return null;
            }
        }
        if ("setCookieHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.ConnectionRequest.setCookieHeader((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCookiesEnabledDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.ConnectionRequest.setCookiesEnabledDefault(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                com.codename1.io.ConnectionRequest.setDefaultCacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.ConnectionRequest.setDefaultFollowRedirects(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.ConnectionRequest.setDefaultUserAgent((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setHandleErrorCodesInGlobalErrorHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.ConnectionRequest.setHandleErrorCodesInGlobalErrorHandler(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrorsDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.ConnectionRequest.setReadResponseForErrorsDefault(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseNativeCookieStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.ConnectionRequest.setUseNativeCookieStore(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.ConnectionRequest.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("clearCookiesFromStorage".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.Cookie.clearCookiesFromStorage(); return null;
            }
        }
        if ("isAutoStored".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Cookie.isAutoStored();
            }
        }
        if ("setAutoStored".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.Cookie.setAutoStored(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Cookie.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("createTempFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.File.createTempFile((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("listRoots".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.File.listRoots();
            }
        }
        throw unsupportedStatic(com.codename1.io.File.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.FileSystemStorage.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.io.FileSystemStorage.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("isIncludeNulls".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.JSONParser.isIncludeNulls();
            }
        }
        if ("isUseBoolean".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.JSONParser.isUseBoolean();
            }
        }
        if ("isUseLongs".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.JSONParser.isUseLongs();
            }
        }
        if ("mapToJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.io.JSONParser.mapToJson((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("setIncludeNulls".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.JSONParser.setIncludeNulls(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.JSONParser.setUseBoolean(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseLongs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.JSONParser.setUseLongs(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.JSONParser.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("bindCrashProtection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.Log.bindCrashProtection(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("deleteLog".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.Log.deleteLog(); return null;
            }
        }
        if ("e".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                com.codename1.io.Log.e((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.getInstance();
            }
        }
        if ("getLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.getLevel();
            }
        }
        if ("getLogContent".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.getLogContent();
            }
        }
        if ("getReportingLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.getReportingLevel();
            }
        }
        if ("getUniqueDeviceId".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.getUniqueDeviceId();
            }
        }
        if ("getUniqueDeviceKey".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.getUniqueDeviceKey();
            }
        }
        if ("install".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Log.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Log.class}, false);
                com.codename1.io.Log.install((com.codename1.io.Log) adaptedArgs[0]); return null;
            }
        }
        if ("isCrashBound".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Log.isCrashBound();
            }
        }
        if ("p".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.Log.p((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                com.codename1.io.Log.p((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("sendLog".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.Log.sendLog(); return null;
            }
        }
        if ("sendLogAsync".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.Log.sendLogAsync(); return null;
            }
        }
        if ("setLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.io.Log.setLevel(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setReportingLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.io.Log.setReportingLevel(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("showLog".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.Log.showLog(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Log.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("isLeaveInputStreamsOpen".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.MultipartRequest.isLeaveInputStreamsOpen();
            }
        }
        if ("setCanFlushStream".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.MultipartRequest.setCanFlushStream(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLeaveInputStreamsOpen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.MultipartRequest.setLeaveInputStreamsOpen(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.MultipartRequest.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("getAutoDetectURL".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.NetworkManager.getAutoDetectURL();
            }
        }
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.NetworkManager.getInstance();
            }
        }
        if ("setAutoDetectURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.NetworkManager.setAutoDetectURL((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.NetworkManager.class, name, safeArgs);
    }

    private static Object invokeStatic11(String name, Object[] safeArgs) throws Exception {
        if ("fetchSerializedOauth2Request".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Oauth2.fetchSerializedOauth2Request();
            }
        }
        if ("getExpires".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Oauth2.getExpires();
            }
        }
        if ("handleRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                return com.codename1.io.Oauth2.handleRedirect((com.codename1.ui.events.ActionListener) adaptedArgs[0]);
            }
        }
        if ("isBackToParent".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Oauth2.isBackToParent();
            }
        }
        if ("setBackToParent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.io.Oauth2.setBackToParent(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Oauth2.class, name, safeArgs);
    }

    private static Object invokeStatic12(String name, Object[] safeArgs) throws Exception {
        if ("addPreferenceListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.PreferenceListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.PreferenceListener.class}, false);
                com.codename1.io.Preferences.addPreferenceListener((java.lang.String) adaptedArgs[0], (com.codename1.io.PreferenceListener) adaptedArgs[1]); return null;
            }
        }
        if ("clearAll".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.io.Preferences.clearAll(); return null;
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.Preferences.delete((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Preferences.get((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return com.codename1.io.Preferences.get((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false);
                return com.codename1.io.Preferences.get((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return com.codename1.io.Preferences.get((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false);
                return com.codename1.io.Preferences.get((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.io.Preferences.get((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getAndSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Preferences.getAndSet((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return com.codename1.io.Preferences.getAndSet((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false);
                return com.codename1.io.Preferences.getAndSet((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return com.codename1.io.Preferences.getAndSet((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false);
                return com.codename1.io.Preferences.getAndSet((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.io.Preferences.getAndSet((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getPreferencesLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Preferences.getPreferencesLocation();
            }
        }
        if ("removePreferenceListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.PreferenceListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.PreferenceListener.class}, false);
                return com.codename1.io.Preferences.removePreferenceListener((java.lang.String) adaptedArgs[0], (com.codename1.io.PreferenceListener) adaptedArgs[1]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                com.codename1.io.Preferences.set((java.util.Map) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.io.Preferences.set((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                com.codename1.io.Preferences.set((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false);
                com.codename1.io.Preferences.set((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                com.codename1.io.Preferences.set((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false);
                com.codename1.io.Preferences.set((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                com.codename1.io.Preferences.set((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setPreferencesLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.Preferences.setPreferencesLocation((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Preferences.class, name, safeArgs);
    }

    private static Object invokeStatic13(String name, Object[] safeArgs) throws Exception {
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, com.codename1.io.SocketConnection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, com.codename1.io.SocketConnection.class}, false);
                com.codename1.io.Socket.connect((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.io.SocketConnection) adaptedArgs[2]); return null;
            }
        }
        if ("getHostOrIP".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Socket.getHostOrIP();
            }
        }
        if ("isServerSocketSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Socket.isServerSocketSupported();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Socket.isSupported();
            }
        }
        if ("listen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Class.class}, false);
                return com.codename1.io.Socket.listen(toIntValue(adaptedArgs[0]), (java.lang.Class) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.io.Socket.class, name, safeArgs);
    }

    private static Object invokeStatic14(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Storage.getInstance();
            }
        }
        if ("isInitialized".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Storage.isInitialized();
            }
        }
        if ("setStorageInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Storage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Storage.class}, false);
                com.codename1.io.Storage.setStorageInstance((com.codename1.io.Storage) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.Storage.class, name, safeArgs);
    }

    private static Object invokeStatic15(String name, Object[] safeArgs) throws Exception {
        if ("cleanup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                com.codename1.io.Util.cleanup((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.io.Util.decode((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("downloadImageToCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.downloadImageToCache((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                com.codename1.io.Util.downloadImageToCache((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Util.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                com.codename1.io.Util.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.SuccessCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                com.codename1.io.Util.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.SuccessCallback) adaptedArgs[2], (com.codename1.util.FailureCallback) adaptedArgs[3]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Util.downloadImageToStorage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                com.codename1.io.Util.downloadImageToStorage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.SuccessCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                com.codename1.io.Util.downloadImageToStorage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.SuccessCallback) adaptedArgs[2], (com.codename1.util.FailureCallback) adaptedArgs[3]); return null;
            }
        }
        if ("downloadUrlSafely".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.OnComplete.class, com.codename1.util.OnComplete.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.util.OnComplete.class, com.codename1.util.OnComplete.class}, false);
                com.codename1.io.Util.downloadUrlSafely((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.util.OnComplete) adaptedArgs[2], (com.codename1.util.OnComplete) adaptedArgs[3]); return null;
            }
        }
        if ("downloadUrlToFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.io.Util.downloadUrlToFile((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("downloadUrlToFileSystemInBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.io.Util.downloadUrlToFileSystemInBackground((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.io.Util.downloadUrlToFileSystemInBackground((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("downloadUrlToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.io.Util.downloadUrlToStorage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("downloadUrlToStorageInBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.io.Util.downloadUrlToStorageInBackground((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                com.codename1.io.Util.downloadUrlToStorageInBackground((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("encodeBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.encodeBody((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.io.Util.encodeBody((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return com.codename1.io.Util.encodeBody((char[]) adaptedArgs[0]);
            }
        }
        if ("encodeUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.encodeUrl((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.io.Util.encodeUrl((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return com.codename1.io.Util.encodeUrl((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Util.encodeUrl((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getFileSizeWithoutDownload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.getFileSizeWithoutDownload((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.io.Util.getFileSizeWithoutDownload((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getIgnorCharsWhileEncoding".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Util.getIgnorCharsWhileEncoding();
            }
        }
        if ("getURLBasePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.getURLBasePath((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getURLHost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.getURLHost((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getURLPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.getURLPath((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getURLProtocol".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.getURLProtocol((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getUUID".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.Util.getUUID();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.io.Util.getUUID(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("guessMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.guessMimeType((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.io.Util.guessMimeType((byte[]) adaptedArgs[0]);
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false);
                return com.codename1.io.Util.indexOf((java.lang.Object[]) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("insertObjectAtOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Integer.class, java.lang.Object.class}, false);
                com.codename1.io.Util.insertObjectAtOffset((java.lang.Object[]) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Object) adaptedArgs[3]); return null;
            }
        }
        if ("instanceofByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofByteArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofDoubleArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofDoubleArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofFloatArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofFloatArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofIntArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofIntArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofLongArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofLongArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofObjArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofObjArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("instanceofShortArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.instanceofShortArray((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("mergeArrays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Object[].class}, false);
                com.codename1.io.Util.mergeArrays((java.lang.Object[]) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1], (java.lang.Object[]) adaptedArgs[2]); return null;
            }
        }
        if ("readToString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false);
                return com.codename1.io.Util.readToString((com.codename1.io.File) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false);
                return com.codename1.io.Util.readToString((com.codename1.io.File) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Externalizable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Externalizable.class}, false);
                com.codename1.io.Util.register((com.codename1.io.Externalizable) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class}, false);
                com.codename1.io.Util.register((java.lang.String) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]); return null;
            }
        }
        if ("relativeToAbsolute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Util.relativeToAbsolute((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("removeObjectAtOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Integer.class}, false);
                com.codename1.io.Util.removeObjectAtOffset((java.lang.Object[]) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class, java.lang.Object.class}, false);
                com.codename1.io.Util.removeObjectAtOffset((java.lang.Object[]) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1], (java.lang.Object) adaptedArgs[2]); return null;
            }
        }
        if ("setDateFormatter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.l10n.SimpleDateFormat.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.l10n.SimpleDateFormat.class}, false);
                com.codename1.io.Util.setDateFormatter((com.codename1.l10n.SimpleDateFormat) adaptedArgs[0]); return null;
            }
        }
        if ("setIgnorCharsWhileEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.io.Util.setIgnorCharsWhileEncoding((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("sleep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.io.Util.sleep(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("split".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.Util.split((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("toBooleanValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.toBooleanValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toCharArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.toCharArray((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toDateValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.toDateValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toDoubleValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.toDoubleValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toFloatValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.toFloatValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toIntValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.toIntValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toLongValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.io.Util.toLongValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("writeStringToFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false);
                com.codename1.io.Util.writeStringToFile((com.codename1.io.File) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class, java.lang.String.class}, false);
                com.codename1.io.Util.writeStringToFile((com.codename1.io.File) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("xorDecode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.xorDecode((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("xorEncode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.Util.xorEncode((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.Util.class, name, safeArgs);
    }

    private static Object invokeStatic16(String name, Object[] safeArgs) throws Exception {
        if ("defineWebService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class, int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 3];
                for (int i = 3; i < adaptedArgs.length; i++) {
                    varArgs[i - 3] = toIntValue(adaptedArgs[i]);
                }
                return com.codename1.io.WebServiceProxyCall.defineWebService((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), varArgs);
            }
        }
        if ("invokeWebserviceASync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, com.codename1.util.Callback.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, com.codename1.util.Callback.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 2];
                for (int i = 2; i < adaptedArgs.length; i++) {
                    varArgs[i - 2] = (java.lang.Object) adaptedArgs[i];
                }
                com.codename1.io.WebServiceProxyCall.invokeWebserviceASync((com.codename1.io.WebServiceProxyCall.WSDefinition) adaptedArgs[0], (com.codename1.util.Callback) adaptedArgs[1], varArgs); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 3];
                for (int i = 3; i < adaptedArgs.length; i++) {
                    varArgs[i - 3] = (java.lang.Object) adaptedArgs[i];
                }
                com.codename1.io.WebServiceProxyCall.invokeWebserviceASync((com.codename1.io.WebServiceProxyCall.WSDefinition) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], varArgs); return null;
            }
        }
        if ("invokeWebserviceSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.WebServiceProxyCall.WSDefinition.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) adaptedArgs[i];
                }
                return com.codename1.io.WebServiceProxyCall.invokeWebserviceSync((com.codename1.io.WebServiceProxyCall.WSDefinition) adaptedArgs[0], varArgs);
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
        if (target instanceof com.codename1.io.ConnectionRequest.SSLCertificate) {
            try {
                return invoke7((com.codename1.io.ConnectionRequest.SSLCertificate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Cookie) {
            try {
                return invoke8((com.codename1.io.Cookie) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.File) {
            try {
                return invoke9((com.codename1.io.File) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.FileSystemStorage) {
            try {
                return invoke10((com.codename1.io.FileSystemStorage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.JSONParser) {
            try {
                return invoke11((com.codename1.io.JSONParser) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Log) {
            try {
                return invoke12((com.codename1.io.Log) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.MalformedURLException) {
            try {
                return invoke13((com.codename1.io.MalformedURLException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.NetworkEvent) {
            try {
                return invoke14((com.codename1.io.NetworkEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.NetworkManager) {
            try {
                return invoke15((com.codename1.io.NetworkManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Oauth2) {
            try {
                return invoke16((com.codename1.io.Oauth2) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Oauth2.RefreshTokenRequest) {
            try {
                return invoke17((com.codename1.io.Oauth2.RefreshTokenRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Properties) {
            try {
                return invoke18((com.codename1.io.Properties) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.SocketConnection) {
            try {
                return invoke19((com.codename1.io.SocketConnection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Storage) {
            try {
                return invoke20((com.codename1.io.Storage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.URL) {
            try {
                return invoke21((com.codename1.io.URL) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Data) {
            try {
                return invoke22((com.codename1.io.Data) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Externalizable) {
            try {
                return invoke23((com.codename1.io.Externalizable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.File.FileFilter) {
            try {
                return invoke24((com.codename1.io.File.FileFilter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.File.FilenameFilter) {
            try {
                return invoke25((com.codename1.io.File.FilenameFilter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.IOProgressListener) {
            try {
                return invoke26((com.codename1.io.IOProgressListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.JSONParseCallback) {
            try {
                return invoke27((com.codename1.io.JSONParseCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.PreferenceListener) {
            try {
                return invoke28((com.codename1.io.PreferenceListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.Socket.StopListening) {
            try {
                return invoke29((com.codename1.io.Socket.StopListening) target, name, safeArgs);
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
        if ("addData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.String.class}, false);
                typedTarget.addData((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addData((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
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
        if ("getBoundary".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBoundary();
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
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("isBase64Binaries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBase64Binaries();
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
        if ("isManualRedirect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isManualRedirect();
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
        if ("setBase64Binaries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBase64Binaries(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setBoundary((java.lang.String) adaptedArgs[0]); return null;
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
                typedTarget.setChunkedStreamingMode(toIntValue(adaptedArgs[0])); return null;
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
        if ("setFilename".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setFilename((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
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
        if ("setManualRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setManualRedirect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
                typedTarget.setPriority((byte) toIntValue(adaptedArgs[0])); return null;
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
                typedTarget.setReadTimeout(toIntValue(adaptedArgs[0])); return null;
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
                typedTarget.setSilentRetryCount(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(toIntValue(adaptedArgs[0])); return null;
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

    private static Object invoke1(com.codename1.io.AccessToken typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getExpires".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpires();
            }
        }
        if ("getExpiryDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpiryDate();
            }
        }
        if ("getIdentityToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdentityToken();
            }
        }
        if ("getObjectId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObjectId();
            }
        }
        if ("getRefreshToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRefreshToken();
            }
        }
        if ("getToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToken();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isExpired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExpired();
            }
        }
        if ("setExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setExpiryDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setIdentityToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setIdentityToken((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRefreshToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRefreshToken((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.BufferedInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getConnection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnection();
            }
        }
        if ("getLastActivityTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastActivityTime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getTotalBytesRead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalBytesRead();
            }
        }
        if ("getYield".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYield();
            }
        }
        if ("isDisableBuffering".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisableBuffering();
            }
        }
        if ("isPrintInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrintInput();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.read();
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("setConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setConnection((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setDisableBuffering".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisableBuffering(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPrintInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPrintInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false);
                typedTarget.setProgressListener((com.codename1.io.IOProgressListener) adaptedArgs[0]); return null;
            }
        }
        if ("setYield".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYield(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.BufferedOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("flushBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushBuffer(); return null;
            }
        }
        if ("getConnection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnection();
            }
        }
        if ("getLastActivityTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastActivityTime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getTotalBytesWritten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalBytesWritten();
            }
        }
        if ("setConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setConnection((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false);
                typedTarget.setProgressListener((com.codename1.io.IOProgressListener) adaptedArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.write(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.write((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.write((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.CacheMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearAllCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearAllCache(); return null;
            }
        }
        if ("clearMemoryCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearMemoryCache(); return null;
            }
        }
        if ("clearStorageCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearStorageCache(); return null;
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.delete((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCachePrefix".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCachePrefix();
            }
        }
        if ("getCacheSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheSize();
            }
        }
        if ("getKeysInCache".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeysInCache();
            }
        }
        if ("getStorageCacheSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStorageCacheSize();
            }
        }
        if ("isAlwaysStore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysStore();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setAlwaysStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysStore(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCachePrefix".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCachePrefix((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCacheSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setStorageCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStorageCacheSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.CharArrayReader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.read();
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return typedTarget.read((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.read((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("ready".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.ready();
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.ConnectionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
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
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
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
                typedTarget.setChunkedStreamingMode(toIntValue(adaptedArgs[0])); return null;
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
                typedTarget.setPriority((byte) toIntValue(adaptedArgs[0])); return null;
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
                typedTarget.setReadTimeout(toIntValue(adaptedArgs[0])); return null;
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
                typedTarget.setSilentRetryCount(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(toIntValue(adaptedArgs[0])); return null;
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

    private static Object invoke7(com.codename1.io.ConnectionRequest.SSLCertificate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCertificteAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCertificteAlgorithm();
            }
        }
        if ("getCertificteUniqueKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCertificteUniqueKey();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.io.Cookie typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDomain".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDomain();
            }
        }
        if ("getExpires".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpires();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getObjectId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObjectId();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("isHttpOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHttpOnly();
            }
        }
        if ("isSecure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSecure();
            }
        }
        if ("setDomain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDomain((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setExpires".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setExpires(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setHttpOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHttpOnly(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPath((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSecure(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setValue((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.io.File typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("canExecute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canExecute();
            }
        }
        if ("createNewFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createNewFile();
            }
        }
        if ("delete".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.delete();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("exists".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.exists();
            }
        }
        if ("getAbsoluteFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteFile();
            }
        }
        if ("getAbsolutePath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsolutePath();
            }
        }
        if ("getFreeSpace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFreeSpace();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getParentFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParentFile();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getTotalSpace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalSpace();
            }
        }
        if ("getUsableSpace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUsableSpace();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isAbsolute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAbsolute();
            }
        }
        if ("isDirectory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDirectory();
            }
        }
        if ("isFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFile();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
        }
        if ("lastModified".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastModified();
            }
        }
        if ("length".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.length();
            }
        }
        if ("list".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.list();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.FilenameFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.FilenameFilter.class}, false);
                return typedTarget.list((com.codename1.io.File.FilenameFilter) adaptedArgs[0]);
            }
        }
        if ("listFiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listFiles();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.FileFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.FileFilter.class}, false);
                return typedTarget.listFiles((com.codename1.io.File.FileFilter) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.FilenameFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.FilenameFilter.class}, false);
                return typedTarget.listFiles((com.codename1.io.File.FilenameFilter) adaptedArgs[0]);
            }
        }
        if ("mkdir".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mkdir();
            }
        }
        if ("mkdirs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mkdirs();
            }
        }
        if ("renameTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false);
                return typedTarget.renameTo((com.codename1.io.File) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("toURL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toURL();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.io.FileSystemStorage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.delete((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deleteRetry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                typedTarget.deleteRetry((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.exists((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getAppHomePath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAppHomePath();
            }
        }
        if ("getCachesDir".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCachesDir();
            }
        }
        if ("getFileSystemSeparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFileSystemSeparator();
            }
        }
        if ("getLastModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLastModified((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLength((java.lang.String) adaptedArgs[0]);
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
        if ("getRoots".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoots();
            }
        }
        if ("hasCachesDir".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasCachesDir();
            }
        }
        if ("isDirectory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isDirectory((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isHidden((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("listFiles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.listFiles((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("mkdir".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.mkdir((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("rename".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.rename((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("toNativePath".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.toNativePath((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.io.JSONParser typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("booleanToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.booleanToken(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("endArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.endArray((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("endBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.endBlock((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("isAlive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlive();
            }
        }
        if ("isIncludeNullsInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIncludeNullsInstance();
            }
        }
        if ("isStrict".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStrict();
            }
        }
        if ("isUseBooleanInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseBooleanInstance();
            }
        }
        if ("isUseLongsInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseLongsInstance();
            }
        }
        if ("keyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.keyValue((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("longToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.longToken(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("numericToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.numericToken(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setIncludeNullsInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIncludeNullsInstance(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setStrict".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setStrict(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseBooleanInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseBooleanInstance(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseLongsInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseLongsInstance(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("startArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.startArray((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("startBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.startBlock((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("stringToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.stringToken((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.io.Log typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFileURL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFileURL();
            }
        }
        if ("isFileWriteEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFileWriteEnabled();
            }
        }
        if ("setFileURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setFileURL((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setFileWriteEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFileWriteEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("trackFileSystem".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.trackFileSystem(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.io.MalformedURLException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.addSuppressed((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.initCause((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false);
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.io.NetworkEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("consume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.consume(); return null;
            }
        }
        if ("getActualComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActualComponent();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getConnectionRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnectionRequest();
            }
        }
        if ("getDraggedComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedComponent();
            }
        }
        if ("getDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDropTarget();
            }
        }
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getEventType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEventType();
            }
        }
        if ("getKeyEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyEvent();
            }
        }
        if ("getLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLength();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getMetaData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetaData();
            }
        }
        if ("getProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgress();
            }
        }
        if ("getProgressPercentage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgressPercentage();
            }
        }
        if ("getProgressType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgressType();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getSentReceived".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSentReceived();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
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
        if ("isConsumed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConsumed();
            }
        }
        if ("isLongEvent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLongEvent();
            }
        }
        if ("isPointerPressedDuringDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPointerPressedDuringDrag();
            }
        }
        if ("setError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Exception.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Exception.class}, false);
                typedTarget.setError((java.lang.Exception) adaptedArgs[0]); return null;
            }
        }
        if ("setPointerPressedDuringDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPointerPressedDuringDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.io.NetworkManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addDefaultHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addDefaultHeader((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addProgressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addToQueue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false);
                typedTarget.addToQueue((com.codename1.io.ConnectionRequest) adaptedArgs[0]); return null;
            }
        }
        if ("addToQueueAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false);
                typedTarget.addToQueueAndWait((com.codename1.io.ConnectionRequest) adaptedArgs[0]); return null;
            }
        }
        if ("addToQueueAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false);
                return typedTarget.addToQueueAsync((com.codename1.io.ConnectionRequest) adaptedArgs[0]);
            }
        }
        if ("assignToThread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.Integer.class}, false);
                typedTarget.assignToThread((java.lang.Class) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("enumurateQueue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.enumurateQueue();
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
        if ("getCurrentAccessPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentAccessPoint();
            }
        }
        if ("getThreadCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThreadCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("isAPSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAPSupported();
            }
        }
        if ("isQueueIdle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isQueueIdle();
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
        if ("killAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false);
                typedTarget.killAndWait((com.codename1.io.ConnectionRequest) adaptedArgs[0]); return null;
            }
        }
        if ("removeErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeProgressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setCurrentAccessPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCurrentAccessPoint((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setThreadCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setThreadCount(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("shutdown".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.shutdown(); return null;
            }
        }
        if ("shutdownSync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.shutdownSync(); return null;
            }
        }
        if ("start".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.start(); return null;
            }
        }
        if ("updateThreadCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.updateThreadCount(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.io.Oauth2 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authenticate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.authenticate();
            }
        }
        if ("createAuthComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                return typedTarget.createAuthComponent((com.codename1.ui.events.ActionListener) adaptedArgs[0]);
            }
        }
        if ("isUseBrowserWindow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseBrowserWindow();
            }
        }
        if ("isUseRedirectForWeb".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseRedirectForWeb();
            }
        }
        if ("refreshToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.refreshToken((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setUseBrowserWindow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseBrowserWindow(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUseRedirectForWeb".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseRedirectForWeb(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("showAuthentication".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.showAuthentication((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.io.Oauth2.RefreshTokenRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false);
                typedTarget.addListener((com.codename1.util.AsyncResource) adaptedArgs[0]); return null;
            }
        }
        if ("addObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.addObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("asPromise".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asPromise();
            }
        }
        if ("await".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.await(); return null;
            }
        }
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.cancel(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("countObservers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.countObservers();
            }
        }
        if ("deleteObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.deleteObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("deleteObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.deleteObservers(); return null;
            }
        }
        if ("error".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.error((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("except".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.except((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false);
                return typedTarget.except((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.EasyThread) adaptedArgs[1]);
            }
        }
        if ("get".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.get();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hasChanged".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasChanged();
            }
        }
        if ("isCancelled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCancelled();
            }
        }
        if ("isDone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDone();
            }
        }
        if ("isReady".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReady();
            }
        }
        if ("notifyObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.notifyObservers(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.notifyObservers((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false);
                typedTarget.onResult((com.codename1.util.AsyncResult) adaptedArgs[0]); return null;
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.ready((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false);
                return typedTarget.ready((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.EasyThread) adaptedArgs[1]);
            }
        }
        if ("waitFor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.waitFor(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.io.Properties typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("propertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.propertyNames();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.setProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("stringPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stringPropertyNames();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.io.SocketConnection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connectionError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.connectionError(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("getConnectTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnectTimeout();
            }
        }
        if ("isConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConnected();
            }
        }
        if ("setConnectTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setConnectTimeout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.io.Storage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearCache(); return null;
            }
        }
        if ("clearStorage".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearStorage(); return null;
            }
        }
        if ("deleteStorageFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.deleteStorageFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("entrySize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.entrySize((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.exists((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("flushStorageCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushStorageCache(); return null;
            }
        }
        if ("isNormalizeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNormalizeNames();
            }
        }
        if ("listEntries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listEntries();
            }
        }
        if ("readObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.readObject((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.readObject((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("setHardCacheSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHardCacheSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setNormalizeNames".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNormalizeNames(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("writeObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.writeObject((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.writeObject((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.io.URL typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAuthority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthority();
            }
        }
        if ("getDefaultPort".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultPort();
            }
        }
        if ("getFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFile();
            }
        }
        if ("getHost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHost();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getPort".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPort();
            }
        }
        if ("getProtocol".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProtocol();
            }
        }
        if ("getQuery".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQuery();
            }
        }
        if ("getUserInfo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserInfo();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("openConnection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.openConnection();
            }
        }
        if ("sameFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.URL.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.URL.class}, false);
                return typedTarget.sameFile((com.codename1.io.URL) adaptedArgs[0]);
            }
        }
        if ("toExternalForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toExternalForm();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.io.Data typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.io.Externalizable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getObjectId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObjectId();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.io.File.FileFilter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accept".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class}, false);
                return typedTarget.accept((com.codename1.io.File) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(com.codename1.io.File.FilenameFilter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accept".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.String.class}, false);
                return typedTarget.accept((com.codename1.io.File) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(com.codename1.io.IOProgressListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke27(com.codename1.io.JSONParseCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("booleanToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.booleanToken(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("endArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.endArray((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("endBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.endBlock((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("isAlive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlive();
            }
        }
        if ("keyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.keyValue((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("longToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.longToken(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("numericToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.numericToken(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("startArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.startArray((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("startBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.startBlock((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("stringToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.stringToken((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke28(com.codename1.io.PreferenceListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("preferenceChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class, java.lang.Object.class}, false);
                typedTarget.preferenceChanged((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.lang.Object) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke29(com.codename1.io.Socket.StopListening typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.ConnectionRequest.class) return getStaticField0(name);
        if (type == com.codename1.io.ConnectionRequest.CachingMode.class) return getStaticField1(name);
        if (type == com.codename1.io.Cookie.class) return getStaticField2(name);
        if (type == com.codename1.io.File.class) return getStaticField3(name);
        if (type == com.codename1.io.FileSystemStorage.class) return getStaticField4(name);
        if (type == com.codename1.io.Log.class) return getStaticField5(name);
        if (type == com.codename1.io.MultipartRequest.class) return getStaticField6(name);
        if (type == com.codename1.io.NetworkEvent.class) return getStaticField7(name);
        if (type == com.codename1.io.NetworkManager.class) return getStaticField8(name);
        if (type == com.codename1.io.Oauth2.class) return getStaticField9(name);
        if (type == com.codename1.io.WebServiceProxyCall.class) return getStaticField10(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("PRIORITY_CRITICAL".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_CRITICAL;
        if ("PRIORITY_HIGH".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_HIGH;
        if ("PRIORITY_LOW".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_LOW;
        if ("PRIORITY_NORMAL".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_NORMAL;
        if ("PRIORITY_REDUNDANT".equals(name)) return com.codename1.io.ConnectionRequest.PRIORITY_REDUNDANT;
        throw unsupportedStaticField(com.codename1.io.ConnectionRequest.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("MANUAL".equals(name)) return com.codename1.io.ConnectionRequest.CachingMode.MANUAL;
        if ("OFF".equals(name)) return com.codename1.io.ConnectionRequest.CachingMode.OFF;
        if ("OFFLINE".equals(name)) return com.codename1.io.ConnectionRequest.CachingMode.OFFLINE;
        if ("OFFLINE_FIRST".equals(name)) return com.codename1.io.ConnectionRequest.CachingMode.OFFLINE_FIRST;
        if ("SMART".equals(name)) return com.codename1.io.ConnectionRequest.CachingMode.SMART;
        throw unsupportedStaticField(com.codename1.io.ConnectionRequest.CachingMode.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("STORAGE_NAME".equals(name)) return com.codename1.io.Cookie.STORAGE_NAME;
        throw unsupportedStaticField(com.codename1.io.Cookie.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("separator".equals(name)) return com.codename1.io.File.separator;
        if ("separatorChar".equals(name)) return com.codename1.io.File.separatorChar;
        throw unsupportedStaticField(com.codename1.io.File.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("ROOT_TYPE_MAINSTORAGE".equals(name)) return com.codename1.io.FileSystemStorage.ROOT_TYPE_MAINSTORAGE;
        if ("ROOT_TYPE_SDCARD".equals(name)) return com.codename1.io.FileSystemStorage.ROOT_TYPE_SDCARD;
        if ("ROOT_TYPE_UNKNOWN".equals(name)) return com.codename1.io.FileSystemStorage.ROOT_TYPE_UNKNOWN;
        throw unsupportedStaticField(com.codename1.io.FileSystemStorage.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("DEBUG".equals(name)) return com.codename1.io.Log.DEBUG;
        if ("ERROR".equals(name)) return com.codename1.io.Log.ERROR;
        if ("INFO".equals(name)) return com.codename1.io.Log.INFO;
        if ("REPORTING_DEBUG".equals(name)) return com.codename1.io.Log.REPORTING_DEBUG;
        if ("REPORTING_NONE".equals(name)) return com.codename1.io.Log.REPORTING_NONE;
        if ("REPORTING_PRODUCTION".equals(name)) return com.codename1.io.Log.REPORTING_PRODUCTION;
        if ("WARNING".equals(name)) return com.codename1.io.Log.WARNING;
        throw unsupportedStaticField(com.codename1.io.Log.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("PRIORITY_CRITICAL".equals(name)) return com.codename1.io.MultipartRequest.PRIORITY_CRITICAL;
        if ("PRIORITY_HIGH".equals(name)) return com.codename1.io.MultipartRequest.PRIORITY_HIGH;
        if ("PRIORITY_LOW".equals(name)) return com.codename1.io.MultipartRequest.PRIORITY_LOW;
        if ("PRIORITY_NORMAL".equals(name)) return com.codename1.io.MultipartRequest.PRIORITY_NORMAL;
        if ("PRIORITY_REDUNDANT".equals(name)) return com.codename1.io.MultipartRequest.PRIORITY_REDUNDANT;
        throw unsupportedStaticField(com.codename1.io.MultipartRequest.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("PROGRESS_TYPE_COMPLETED".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_COMPLETED;
        if ("PROGRESS_TYPE_INITIALIZING".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_INITIALIZING;
        if ("PROGRESS_TYPE_INPUT".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_INPUT;
        if ("PROGRESS_TYPE_OUTPUT".equals(name)) return com.codename1.io.NetworkEvent.PROGRESS_TYPE_OUTPUT;
        throw unsupportedStaticField(com.codename1.io.NetworkEvent.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("ACCESS_POINT_TYPE_CABLE".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_CABLE;
        if ("ACCESS_POINT_TYPE_CORPORATE".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_CORPORATE;
        if ("ACCESS_POINT_TYPE_NETWORK2G".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_NETWORK2G;
        if ("ACCESS_POINT_TYPE_NETWORK3G".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_NETWORK3G;
        if ("ACCESS_POINT_TYPE_UNKNOWN".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
        if ("ACCESS_POINT_TYPE_WLAN".equals(name)) return com.codename1.io.NetworkManager.ACCESS_POINT_TYPE_WLAN;
        throw unsupportedStaticField(com.codename1.io.NetworkManager.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("TOKEN".equals(name)) return com.codename1.io.Oauth2.TOKEN;
        throw unsupportedStaticField(com.codename1.io.Oauth2.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
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
        throw unsupportedStaticField(com.codename1.io.WebServiceProxyCall.class, name);
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
