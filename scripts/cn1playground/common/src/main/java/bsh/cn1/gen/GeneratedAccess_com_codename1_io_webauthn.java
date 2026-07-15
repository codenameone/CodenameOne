/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_webauthn {
    private GeneratedAccess_com_codename1_io_webauthn() {
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
        if ("PublicKeyCredential".equals(simpleName)) {
            return com.codename1.io.webauthn.PublicKeyCredential.class;
        }
        if ("PublicKeyCredentialCreationOptions".equals(simpleName)) {
            return com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.Builder.class;
        }
        if ("PublicKeyCredentialRequestOptions".equals(simpleName)) {
            return com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.Builder.class;
        }
        if ("WebAuthnClient".equals(simpleName)) {
            return com.codename1.io.webauthn.WebAuthnClient.class;
        }
        if ("WebAuthnException".equals(simpleName)) {
            return com.codename1.io.webauthn.WebAuthnException.class;
        }
        if ("WebAuthnNative".equals(simpleName)) {
            return com.codename1.io.webauthn.WebAuthnNative.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.Builder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.Builder();
            }
        }
        if (type == com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.Builder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.Builder();
            }
        }
        if (type == com.codename1.io.webauthn.WebAuthnException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.webauthn.WebAuthnException((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.io.webauthn.WebAuthnException((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.webauthn.PublicKeyCredential.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.io.webauthn.WebAuthnClient.class) return invokeStatic3(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("fromJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.webauthn.PublicKeyCredential.fromJson((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.webauthn.PublicKeyCredential.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.fromJson((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("newBuilder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.newBuilder();
            }
        }
        throw unsupportedStatic(com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("fromJson".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.fromJson((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("newBuilder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.newBuilder();
            }
        }
        throw unsupportedStatic(com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.webauthn.WebAuthnClient.getInstance();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.webauthn.WebAuthnClient.isSupported();
            }
        }
        if ("setProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.webauthn.WebAuthnNative.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.webauthn.WebAuthnNative.class}, false);
                com.codename1.io.webauthn.WebAuthnClient.setProvider((com.codename1.io.webauthn.WebAuthnNative) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.webauthn.WebAuthnClient.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.webauthn.PublicKeyCredential) {
            try {
                return invoke0((com.codename1.io.webauthn.PublicKeyCredential) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.PublicKeyCredentialCreationOptions) {
            try {
                return invoke1((com.codename1.io.webauthn.PublicKeyCredentialCreationOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.Builder) {
            try {
                return invoke2((com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.PublicKeyCredentialRequestOptions) {
            try {
                return invoke3((com.codename1.io.webauthn.PublicKeyCredentialRequestOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.Builder) {
            try {
                return invoke4((com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.WebAuthnClient) {
            try {
                return invoke5((com.codename1.io.webauthn.WebAuthnClient) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.WebAuthnException) {
            try {
                return invoke6((com.codename1.io.webauthn.WebAuthnException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.webauthn.WebAuthnNative) {
            try {
                return invoke7((com.codename1.io.webauthn.WebAuthnNative) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.webauthn.PublicKeyCredential typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asMap();
            }
        }
        if ("getAttestationObject".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttestationObject();
            }
        }
        if ("getAuthenticatorAttachment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthenticatorAttachment();
            }
        }
        if ("getClientDataJSON".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClientDataJSON();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getRawId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawId();
            }
        }
        if ("getSignature".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSignature();
            }
        }
        if ("getUserHandle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserHandle();
            }
        }
        if ("isRegistration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRegistration();
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.webauthn.PublicKeyCredentialCreationOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asMap();
            }
        }
        if ("getChallenge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChallenge();
            }
        }
        if ("getRpId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRpId();
            }
        }
        if ("getRpName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRpName();
            }
        }
        if ("getUserDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserDisplayName();
            }
        }
        if ("getUserId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserId();
            }
        }
        if ("getUserName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserName();
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authenticatorAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.authenticatorAttachment((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("challenge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.challenge((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("residentKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.residentKey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("rp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.rp((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("user".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.user((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("userVerification".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.userVerification((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.webauthn.PublicKeyCredentialRequestOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asMap();
            }
        }
        if ("getChallenge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChallenge();
            }
        }
        if ("getRpId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRpId();
            }
        }
        if ("getUserVerification".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserVerification();
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("challenge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.challenge((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("rpId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.rpId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("userVerification".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.userVerification((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.webauthn.WebAuthnClient typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.webauthn.PublicKeyCredentialCreationOptions.class}, false);
                return typedTarget.create((com.codename1.io.webauthn.PublicKeyCredentialCreationOptions) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.webauthn.PublicKeyCredentialRequestOptions.class}, false);
                return typedTarget.get((com.codename1.io.webauthn.PublicKeyCredentialRequestOptions) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.webauthn.WebAuthnException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke7(com.codename1.io.webauthn.WebAuthnNative typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createPasskey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.createPasskey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getPasskey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPasskey((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.webauthn.PublicKeyCredential.class) return getStaticField0(name);
        if (type == com.codename1.io.webauthn.WebAuthnException.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("TYPE_PUBLIC_KEY".equals(name)) return com.codename1.io.webauthn.PublicKeyCredential.TYPE_PUBLIC_KEY;
        throw unsupportedStaticField(com.codename1.io.webauthn.PublicKeyCredential.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ABORTED".equals(name)) return com.codename1.io.webauthn.WebAuthnException.ABORTED;
        if ("CONSTRAINT_ERROR".equals(name)) return com.codename1.io.webauthn.WebAuthnException.CONSTRAINT_ERROR;
        if ("INVALID_OPTIONS".equals(name)) return com.codename1.io.webauthn.WebAuthnException.INVALID_OPTIONS;
        if ("INVALID_RESPONSE".equals(name)) return com.codename1.io.webauthn.WebAuthnException.INVALID_RESPONSE;
        if ("INVALID_STATE".equals(name)) return com.codename1.io.webauthn.WebAuthnException.INVALID_STATE;
        if ("NOT_ALLOWED".equals(name)) return com.codename1.io.webauthn.WebAuthnException.NOT_ALLOWED;
        if ("NOT_IMPLEMENTED".equals(name)) return com.codename1.io.webauthn.WebAuthnException.NOT_IMPLEMENTED;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.io.webauthn.WebAuthnException.NOT_SUPPORTED;
        if ("SECURITY_ERROR".equals(name)) return com.codename1.io.webauthn.WebAuthnException.SECURITY_ERROR;
        if ("TRANSPORT_ERROR".equals(name)) return com.codename1.io.webauthn.WebAuthnException.TRANSPORT_ERROR;
        throw unsupportedStaticField(com.codename1.io.webauthn.WebAuthnException.class, name);
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
