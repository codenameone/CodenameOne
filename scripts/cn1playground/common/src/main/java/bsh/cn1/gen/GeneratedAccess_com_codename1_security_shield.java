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

public final class GeneratedAccess_com_codename1_security_shield {
    private GeneratedAccess_com_codename1_security_shield() {
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
        if ("AppShield".equals(simpleName)) {
            return com.codename1.security.shield.AppShield.class;
        }
        if ("FailureMode".equals(simpleName)) {
            return com.codename1.security.shield.FailureMode.class;
        }
        if ("HostPolicy".equals(simpleName)) {
            return com.codename1.security.shield.HostPolicy.class;
        }
        if ("PinSet".equals(simpleName)) {
            return com.codename1.security.shield.PinSet.class;
        }
        if ("ShieldConfig".equals(simpleName)) {
            return com.codename1.security.shield.ShieldConfig.class;
        }
        if ("ShieldException".equals(simpleName)) {
            return com.codename1.security.shield.ShieldException.class;
        }
        if ("ShieldListener".equals(simpleName)) {
            return com.codename1.security.shield.ShieldListener.class;
        }
        if ("ShieldSignal".equals(simpleName)) {
            return com.codename1.security.shield.ShieldSignal.class;
        }
        if ("ShieldSignals".equals(simpleName)) {
            return com.codename1.security.shield.ShieldSignals.class;
        }
        if ("ShieldStatus".equals(simpleName)) {
            return com.codename1.security.shield.ShieldStatus.class;
        }
        if ("ShieldToken".equals(simpleName)) {
            return com.codename1.security.shield.ShieldToken.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.security.shield.HostPolicy.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, com.codename1.security.shield.FailureMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, com.codename1.security.shield.FailureMode.class}, false);
                return new com.codename1.security.shield.HostPolicy(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue(), (com.codename1.security.shield.FailureMode) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.security.shield.PinSet.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class}, false);
                return new com.codename1.security.shield.PinSet((java.util.Hashtable) adaptedArgs[0], toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).longValue(), ((Number) adaptedArgs[3]).longValue());
            }
        }
        if (type == com.codename1.security.shield.ShieldConfig.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.security.shield.ShieldConfig();
            }
        }
        if (type == com.codename1.security.shield.ShieldException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldStatus.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldStatus.class, java.lang.String.class}, false);
                return new com.codename1.security.shield.ShieldException((com.codename1.security.shield.ShieldStatus) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.security.shield.ShieldSignal.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.security.shield.ShieldSignal((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.security.shield.ShieldToken.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.shield.ShieldStatus.class, java.lang.Long.class, java.lang.Long.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.shield.ShieldStatus.class, java.lang.Long.class, java.lang.Long.class, java.lang.String.class}, false);
                return new com.codename1.security.shield.ShieldToken((java.lang.String) adaptedArgs[0], (com.codename1.security.shield.ShieldStatus) adaptedArgs[1], ((Number) adaptedArgs[2]).longValue(), ((Number) adaptedArgs[3]).longValue(), (java.lang.String) adaptedArgs[4]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.security.shield.AppShield.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.security.shield.ShieldSignals.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.security.shield.ShieldStatus.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldListener.class}, false);
                com.codename1.security.shield.AppShield.addListener((com.codename1.security.shield.ShieldListener) adaptedArgs[0]); return null;
            }
        }
        if ("addProtectedHost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.security.shield.AppShield.addProtectedHost((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.shield.HostPolicy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.shield.HostPolicy.class}, false);
                com.codename1.security.shield.AppShield.addProtectedHost((java.lang.String) adaptedArgs[0], (com.codename1.security.shield.HostPolicy) adaptedArgs[1]); return null;
            }
        }
        if ("attach".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.class}, false);
                com.codename1.security.shield.AppShield.attach((com.codename1.io.ConnectionRequest) adaptedArgs[0]); return null;
            }
        }
        if ("fetchToken".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.fetchToken();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.shield.AppShield.fetchToken((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCachedToken".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getCachedToken();
            }
        }
        if ("getConfig".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getConfig();
            }
        }
        if ("getEngineName".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getEngineName();
            }
        }
        if ("getNetworkGuard".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getNetworkGuard();
            }
        }
        if ("getPinSet".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getPinSet();
            }
        }
        if ("getSignals".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getSignals();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.getStatus();
            }
        }
        if ("headersFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.shield.AppShield.headersFor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldConfig.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldConfig.class}, false);
                com.codename1.security.shield.AppShield.init((com.codename1.security.shield.ShieldConfig) adaptedArgs[0]); return null;
            }
        }
        if ("invalidateToken".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.security.shield.AppShield.invalidateToken(); return null;
            }
        }
        if ("isProtected".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.isProtected();
            }
        }
        if ("policyFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.shield.AppShield.policyFor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("protectedHosts".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.AppShield.protectedHosts();
            }
        }
        if ("removeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldListener.class}, false);
                com.codename1.security.shield.AppShield.removeListener((com.codename1.security.shield.ShieldListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.security.shield.AppShield.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldSignal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldSignal.class}, false);
                com.codename1.security.shield.ShieldSignals.add((com.codename1.security.shield.ShieldSignal) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.security.shield.ShieldSignals.add((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.security.shield.ShieldSignals.clear(); return null;
            }
        }
        if ("hasSignalAtLeast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.security.shield.ShieldSignals.hasSignalAtLeast(toIntValue(adaptedArgs[0]));
            }
        }
        if ("snapshot".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.shield.ShieldSignals.snapshot();
            }
        }
        throw unsupportedStatic(com.codename1.security.shield.ShieldSignals.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("forId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.shield.ShieldStatus.forId((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.security.shield.ShieldStatus.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.security.shield.HostPolicy) {
            try {
                return invoke0((com.codename1.security.shield.HostPolicy) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.PinSet) {
            try {
                return invoke1((com.codename1.security.shield.PinSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.ShieldConfig) {
            try {
                return invoke2((com.codename1.security.shield.ShieldConfig) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.ShieldException) {
            try {
                return invoke3((com.codename1.security.shield.ShieldException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.ShieldSignal) {
            try {
                return invoke4((com.codename1.security.shield.ShieldSignal) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.ShieldStatus) {
            try {
                return invoke5((com.codename1.security.shield.ShieldStatus) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.ShieldToken) {
            try {
                return invoke6((com.codename1.security.shield.ShieldToken) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.shield.ShieldListener) {
            try {
                return invoke7((com.codename1.security.shield.ShieldListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.security.shield.HostPolicy typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFailureMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFailureMode();
            }
        }
        if ("isAttachToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAttachToken();
            }
        }
        if ("isEnforcePins".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnforcePins();
            }
        }
        if ("isNoOp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNoOp();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.security.shield.PinSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("hostCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hostCount();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("isEnforcedFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isEnforcedFor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isExpired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExpired();
            }
        }
        if ("isStale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStale();
            }
        }
        if ("matches".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                return typedTarget.matches((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]);
            }
        }
        if ("pinsFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.pinsFor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.security.shield.ShieldConfig typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("collectSignals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.collectSignals(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("defaultFailureMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.FailureMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.FailureMode.class}, false);
                return typedTarget.defaultFailureMode((com.codename1.security.shield.FailureMode) adaptedArgs[0]);
            }
        }
        if ("endpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.endpoint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getDefaultFailureMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailureMode();
            }
        }
        if ("getEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndpoint();
            }
        }
        if ("getRefreshThresholdPercent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRefreshThresholdPercent();
            }
        }
        if ("getTokenHeader".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTokenHeader();
            }
        }
        if ("hasProtectedHosts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasProtectedHosts();
            }
        }
        if ("isCollectSignals".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCollectSignals();
            }
        }
        if ("policyFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.policyFor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("protect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.protect((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.shield.HostPolicy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.shield.HostPolicy.class}, false);
                return typedTarget.protect((java.lang.String) adaptedArgs[0], (com.codename1.security.shield.HostPolicy) adaptedArgs[1]);
            }
        }
        if ("protectedHosts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.protectedHosts();
            }
        }
        if ("refreshThresholdPercent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.refreshThresholdPercent(toIntValue(adaptedArgs[0]));
            }
        }
        if ("tokenHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.tokenHeader((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.security.shield.ShieldException typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
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

    private static Object invoke4(com.codename1.security.shield.ShieldSignal typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDetail".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDetail();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getSeverity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeverity();
            }
        }
        if ("getTimestamp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestamp();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.security.shield.ShieldStatus typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isSuccess".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSuccess();
            }
        }
        if ("isTransient".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTransient();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.security.shield.ShieldToken typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBinding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBinding();
            }
        }
        if ("getFetchedAt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFetchedAt();
            }
        }
        if ("getMillisUntilExpiry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMillisUntilExpiry();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("isBoundTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isBoundTo((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isValid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValid();
            }
        }
        if ("shouldRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shouldRefresh(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.security.shield.ShieldListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("signalRaised".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldSignal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldSignal.class}, false);
                typedTarget.signalRaised((com.codename1.security.shield.ShieldSignal) adaptedArgs[0]); return null;
            }
        }
        if ("statusChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldStatus.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.shield.ShieldStatus.class}, false);
                typedTarget.statusChanged((com.codename1.security.shield.ShieldStatus) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.security.shield.AppShield.class) return getStaticField0(name);
        if (type == com.codename1.security.shield.FailureMode.class) return getStaticField1(name);
        if (type == com.codename1.security.shield.HostPolicy.class) return getStaticField2(name);
        if (type == com.codename1.security.shield.PinSet.class) return getStaticField3(name);
        if (type == com.codename1.security.shield.ShieldConfig.class) return getStaticField4(name);
        if (type == com.codename1.security.shield.ShieldSignal.class) return getStaticField5(name);
        if (type == com.codename1.security.shield.ShieldStatus.class) return getStaticField6(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("REJECT_HEADER".equals(name)) return com.codename1.security.shield.AppShield.REJECT_HEADER;
        throw unsupportedStaticField(com.codename1.security.shield.AppShield.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CLOSED".equals(name)) return com.codename1.security.shield.FailureMode.CLOSED;
        if ("OPEN".equals(name)) return com.codename1.security.shield.FailureMode.OPEN;
        throw unsupportedStaticField(com.codename1.security.shield.FailureMode.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("ENFORCED".equals(name)) return com.codename1.security.shield.HostPolicy.ENFORCED;
        if ("PROTECTED".equals(name)) return com.codename1.security.shield.HostPolicy.PROTECTED;
        if ("UNPROTECTED".equals(name)) return com.codename1.security.shield.HostPolicy.UNPROTECTED;
        throw unsupportedStaticField(com.codename1.security.shield.HostPolicy.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("EMPTY".equals(name)) return com.codename1.security.shield.PinSet.EMPTY;
        throw unsupportedStaticField(com.codename1.security.shield.PinSet.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("DEFAULT_TOKEN_HEADER".equals(name)) return com.codename1.security.shield.ShieldConfig.DEFAULT_TOKEN_HEADER;
        throw unsupportedStaticField(com.codename1.security.shield.ShieldConfig.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("ACCESSIBILITY".equals(name)) return com.codename1.security.shield.ShieldSignal.ACCESSIBILITY;
        if ("DEBUGGER".equals(name)) return com.codename1.security.shield.ShieldSignal.DEBUGGER;
        if ("EMULATOR".equals(name)) return com.codename1.security.shield.ShieldSignal.EMULATOR;
        if ("HOOK".equals(name)) return com.codename1.security.shield.ShieldSignal.HOOK;
        if ("JAILBREAK".equals(name)) return com.codename1.security.shield.ShieldSignal.JAILBREAK;
        if ("REPACKAGED".equals(name)) return com.codename1.security.shield.ShieldSignal.REPACKAGED;
        if ("ROOT".equals(name)) return com.codename1.security.shield.ShieldSignal.ROOT;
        throw unsupportedStaticField(com.codename1.security.shield.ShieldSignal.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("NOT_INITIALIZED".equals(name)) return com.codename1.security.shield.ShieldStatus.NOT_INITIALIZED;
        if ("NO_NETWORK".equals(name)) return com.codename1.security.shield.ShieldStatus.NO_NETWORK;
        if ("OK".equals(name)) return com.codename1.security.shield.ShieldStatus.OK;
        if ("PIN_MISMATCH".equals(name)) return com.codename1.security.shield.ShieldStatus.PIN_MISMATCH;
        if ("POOR_NETWORK".equals(name)) return com.codename1.security.shield.ShieldStatus.POOR_NETWORK;
        if ("RATE_LIMITED".equals(name)) return com.codename1.security.shield.ShieldStatus.RATE_LIMITED;
        if ("REJECTED".equals(name)) return com.codename1.security.shield.ShieldStatus.REJECTED;
        if ("SERVICE_DOWN".equals(name)) return com.codename1.security.shield.ShieldStatus.SERVICE_DOWN;
        if ("UNPROTECTED".equals(name)) return com.codename1.security.shield.ShieldStatus.UNPROTECTED;
        throw unsupportedStaticField(com.codename1.security.shield.ShieldStatus.class, name);
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
