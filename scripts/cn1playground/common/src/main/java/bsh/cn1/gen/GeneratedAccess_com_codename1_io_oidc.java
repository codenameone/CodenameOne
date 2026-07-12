package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_oidc {
    private GeneratedAccess_com_codename1_io_oidc() {
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
        if ("OidcBrowserNative".equals(simpleName)) {
            return com.codename1.io.oidc.OidcBrowserNative.class;
        }
        if ("OidcClient".equals(simpleName)) {
            return com.codename1.io.oidc.OidcClient.class;
        }
        if ("OidcConfiguration".equals(simpleName)) {
            return com.codename1.io.oidc.OidcConfiguration.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.io.oidc.OidcConfiguration.Builder.class;
        }
        if ("OidcException".equals(simpleName)) {
            return com.codename1.io.oidc.OidcException.class;
        }
        if ("OidcTokens".equals(simpleName)) {
            return com.codename1.io.oidc.OidcTokens.class;
        }
        if ("PkceChallenge".equals(simpleName)) {
            return com.codename1.io.oidc.PkceChallenge.class;
        }
        if ("SystemBrowser".equals(simpleName)) {
            return com.codename1.io.oidc.SystemBrowser.class;
        }
        if ("TokenStore".equals(simpleName)) {
            return com.codename1.io.oidc.TokenStore.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.oidc.OidcConfiguration.Builder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.oidc.OidcConfiguration.Builder();
            }
        }
        if (type == com.codename1.io.oidc.OidcException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.oidc.OidcException((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.io.oidc.OidcException((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.oidc.OidcClient.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.oidc.OidcConfiguration.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.io.oidc.OidcTokens.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.io.oidc.PkceChallenge.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.io.oidc.SystemBrowser.class) return invokeStatic4(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcConfiguration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcConfiguration.class}, false);
                return com.codename1.io.oidc.OidcClient.create((com.codename1.io.oidc.OidcConfiguration) adaptedArgs[0]);
            }
        }
        if ("discover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.oidc.OidcClient.discover((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.oidc.OidcClient.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromDiscoveryJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.io.oidc.OidcConfiguration.fromDiscoveryJson((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("newBuilder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.oidc.OidcConfiguration.newBuilder();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcConfiguration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcConfiguration.class}, false);
                return com.codename1.io.oidc.OidcConfiguration.newBuilder((com.codename1.io.oidc.OidcConfiguration) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.oidc.OidcConfiguration.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("decodeIdTokenClaims".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.oidc.OidcTokens.decodeIdTokenClaims((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("fromTokenResponse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.String.class}, false);
                return com.codename1.io.oidc.OidcTokens.fromTokenResponse((java.util.Map) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.io.oidc.OidcTokens.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("generate".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.oidc.PkceChallenge.generate();
            }
        }
        throw unsupportedStatic(com.codename1.io.oidc.PkceChallenge.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("authenticate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.io.oidc.SystemBrowser.authenticate((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isNativeAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.oidc.SystemBrowser.isNativeAvailable();
            }
        }
        if ("setProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcBrowserNative.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcBrowserNative.class}, false);
                com.codename1.io.oidc.SystemBrowser.setProvider((com.codename1.io.oidc.OidcBrowserNative) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.oidc.SystemBrowser.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.oidc.OidcClient) {
            try {
                return invoke0((com.codename1.io.oidc.OidcClient) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.OidcConfiguration) {
            try {
                return invoke1((com.codename1.io.oidc.OidcConfiguration) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.OidcConfiguration.Builder) {
            try {
                return invoke2((com.codename1.io.oidc.OidcConfiguration.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.OidcException) {
            try {
                return invoke3((com.codename1.io.oidc.OidcException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.OidcTokens) {
            try {
                return invoke4((com.codename1.io.oidc.OidcTokens) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.PkceChallenge) {
            try {
                return invoke5((com.codename1.io.oidc.PkceChallenge) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.OidcBrowserNative) {
            try {
                return invoke6((com.codename1.io.oidc.OidcBrowserNative) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.oidc.TokenStore) {
            try {
                return invoke7((com.codename1.io.oidc.TokenStore) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.oidc.OidcClient typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authorize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.authorize();
            }
        }
        if ("clearStoredTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearStoredTokens();
            }
        }
        if ("getConfiguration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfiguration();
            }
        }
        if ("loadStoredTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.loadStoredTokens();
            }
        }
        if ("refresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.refresh((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("refreshIfExpired".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.refreshIfExpired(toIntValue(adaptedArgs[0]));
            }
        }
        if ("revoke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.revoke((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setAuthorizationParameters".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.setAuthorizationParameters(varArgs);
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setClientId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setClientSecret((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setEnforceNonce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setEnforceNonce(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setRedirectUri".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setRedirectUri((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setResponseMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setResponseMode((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setScopes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.setScopes((java.util.List) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.setScopes(varArgs);
            }
        }
        if ("setStoreKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setStoreKey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTokenParameters".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.setTokenParameters(varArgs);
            }
        }
        if ("setTokenStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.TokenStore.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.TokenStore.class}, false);
                return typedTarget.setTokenStore((com.codename1.io.oidc.TokenStore) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.oidc.OidcConfiguration typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAuthorizationEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthorizationEndpoint();
            }
        }
        if ("getEndSessionEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndSessionEndpoint();
            }
        }
        if ("getIssuer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIssuer();
            }
        }
        if ("getJwksUri".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJwksUri();
            }
        }
        if ("getRevocationEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRevocationEndpoint();
            }
        }
        if ("getTokenEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTokenEndpoint();
            }
        }
        if ("getUserInfoEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserInfoEndpoint();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.oidc.OidcConfiguration.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authorizationEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.authorizationEndpoint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("endSessionEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.endSessionEndpoint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("issuer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.issuer((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("jwksUri".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.jwksUri((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("revocationEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.revocationEndpoint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("tokenEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.tokenEndpoint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("userInfoEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.userInfoEndpoint((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.oidc.OidcException typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getErrorDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrorDescription();
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

    private static Object invoke4(com.codename1.io.oidc.OidcTokens typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessToken();
            }
        }
        if ("getClaim".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClaim((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getEmail".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEmail();
            }
        }
        if ("getExpiresAt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpiresAt();
            }
        }
        if ("getIdToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdToken();
            }
        }
        if ("getIdTokenClaims".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdTokenClaims();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getRawResponse".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawResponse();
            }
        }
        if ("getRefreshToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRefreshToken();
            }
        }
        if ("getScope".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScope();
            }
        }
        if ("getStringClaim".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getStringClaim((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getSubject".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubject();
            }
        }
        if ("getTokenType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTokenType();
            }
        }
        if ("isExpired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExpired();
            }
        }
        if ("isExpiringWithin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isExpiringWithin(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toAccessToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toAccessToken();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.oidc.PkceChallenge typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getChallenge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChallenge();
            }
        }
        if ("getMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMethod();
            }
        }
        if ("getVerifier".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerifier();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.oidc.OidcBrowserNative typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("startAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.startAuthorization((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.io.oidc.TokenStore typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.clear((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("load".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.load((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("save".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.oidc.OidcTokens.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.io.oidc.OidcTokens.class}, false);
                return typedTarget.save((java.lang.String) adaptedArgs[0], (com.codename1.io.oidc.OidcTokens) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.oidc.OidcException.class) return getStaticField0(name);
        if (type == com.codename1.io.oidc.PkceChallenge.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ACCESS_DENIED".equals(name)) return com.codename1.io.oidc.OidcException.ACCESS_DENIED;
        if ("DISCOVERY_FAILED".equals(name)) return com.codename1.io.oidc.OidcException.DISCOVERY_FAILED;
        if ("INVALID_GRANT".equals(name)) return com.codename1.io.oidc.OidcException.INVALID_GRANT;
        if ("INVALID_ID_TOKEN".equals(name)) return com.codename1.io.oidc.OidcException.INVALID_ID_TOKEN;
        if ("NONCE_MISMATCH".equals(name)) return com.codename1.io.oidc.OidcException.NONCE_MISMATCH;
        if ("STATE_MISMATCH".equals(name)) return com.codename1.io.oidc.OidcException.STATE_MISMATCH;
        if ("TRANSPORT_ERROR".equals(name)) return com.codename1.io.oidc.OidcException.TRANSPORT_ERROR;
        if ("USER_CANCELLED".equals(name)) return com.codename1.io.oidc.OidcException.USER_CANCELLED;
        throw unsupportedStaticField(com.codename1.io.oidc.OidcException.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("METHOD_S256".equals(name)) return com.codename1.io.oidc.PkceChallenge.METHOD_S256;
        throw unsupportedStaticField(com.codename1.io.oidc.PkceChallenge.class, name);
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
