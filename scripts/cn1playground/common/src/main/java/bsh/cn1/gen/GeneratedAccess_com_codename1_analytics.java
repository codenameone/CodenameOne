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

public final class GeneratedAccess_com_codename1_analytics {
    private GeneratedAccess_com_codename1_analytics() {
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
        if ("AbstractAnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.AbstractAnalyticsProvider.class;
        }
        if ("Analytics".equals(simpleName)) {
            return com.codename1.analytics.Analytics.class;
        }
        if ("AnalyticsCapability".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsCapability.class;
        }
        if ("AnalyticsConsent".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsConsent.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsConsent.Builder.class;
        }
        if ("AnalyticsContext".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsContext.class;
        }
        if ("AnalyticsCrashReport".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsCrashReport.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsCrashReport.Builder.class;
        }
        if ("AnalyticsEvent".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsEvent.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsEvent.Builder.class;
        }
        if ("AnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsProvider.class;
        }
        if ("AnalyticsService".equals(simpleName)) {
            return com.codename1.analytics.AnalyticsService.class;
        }
        if ("CodenameOneAnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.CodenameOneAnalyticsProvider.class;
        }
        if ("ConsentMode".equals(simpleName)) {
            return com.codename1.analytics.ConsentMode.class;
        }
        if ("FirebaseAnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.FirebaseAnalyticsProvider.class;
        }
        if ("Bridge".equals(simpleName)) {
            return com.codename1.analytics.FirebaseAnalyticsProvider.Bridge.class;
        }
        if ("GoogleAnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.GoogleAnalyticsProvider.class;
        }
        if ("LegacyAnalyticsProviderAdapter".equals(simpleName)) {
            return com.codename1.analytics.LegacyAnalyticsProviderAdapter.class;
        }
        if ("LoggingAnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.LoggingAnalyticsProvider.class;
        }
        if ("MatomoAnalyticsProvider".equals(simpleName)) {
            return com.codename1.analytics.MatomoAnalyticsProvider.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.analytics.AnalyticsConsent.Builder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.analytics.AnalyticsConsent.Builder();
            }
        }
        if (type == com.codename1.analytics.AnalyticsService.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.analytics.AnalyticsService();
            }
        }
        if (type == com.codename1.analytics.CodenameOneAnalyticsProvider.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.analytics.CodenameOneAnalyticsProvider();
            }
        }
        if (type == com.codename1.analytics.FirebaseAnalyticsProvider.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.analytics.FirebaseAnalyticsProvider();
            }
        }
        if (type == com.codename1.analytics.GoogleAnalyticsProvider.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.analytics.GoogleAnalyticsProvider((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.analytics.LegacyAnalyticsProviderAdapter.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsService.class}, false);
                return new com.codename1.analytics.LegacyAnalyticsProviderAdapter((com.codename1.analytics.AnalyticsService) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.analytics.LoggingAnalyticsProvider.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.analytics.LoggingAnalyticsProvider();
            }
        }
        if (type == com.codename1.analytics.MatomoAnalyticsProvider.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.analytics.MatomoAnalyticsProvider((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.analytics.Analytics.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.analytics.AnalyticsConsent.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.analytics.AnalyticsCrashReport.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.analytics.AnalyticsEvent.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.analytics.AnalyticsService.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.analytics.FirebaseAnalyticsProvider.class) return invokeStatic5(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsProvider.class}, false);
                com.codename1.analytics.Analytics.addProvider((com.codename1.analytics.AnalyticsProvider) adaptedArgs[0]); return null;
            }
        }
        if ("autoEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.Map.class}, false);
                com.codename1.analytics.Analytics.autoEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.Map) adaptedArgs[2]); return null;
            }
        }
        if ("clearDimension".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.analytics.Analytics.clearDimension((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("clearDimensions".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.analytics.Analytics.clearDimensions(); return null;
            }
        }
        if ("clearProviders".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.analytics.Analytics.clearProviders(); return null;
            }
        }
        if ("clientId".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.Analytics.clientId();
            }
        }
        if ("crash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                com.codename1.analytics.Analytics.crash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.String.class, java.lang.Boolean.class}, false);
                com.codename1.analytics.Analytics.crash((java.lang.Throwable) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("event".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                com.codename1.analytics.Analytics.event((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.analytics.Analytics.flush(); return null;
            }
        }
        if ("getConsent".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.Analytics.getConsent();
            }
        }
        if ("getConsentMode".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.Analytics.getConsentMode();
            }
        }
        if ("getDimensions".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.Analytics.getDimensions();
            }
        }
        if ("getProviders".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.Analytics.getProviders();
            }
        }
        if ("removeProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsProvider.class}, false);
                com.codename1.analytics.Analytics.removeProvider((com.codename1.analytics.AnalyticsProvider) adaptedArgs[0]); return null;
            }
        }
        if ("resetClientId".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.Analytics.resetClientId();
            }
        }
        if ("screen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.analytics.Analytics.screen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setConsent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                com.codename1.analytics.Analytics.setConsent((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("setConsentMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.ConsentMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.ConsentMode.class}, false);
                com.codename1.analytics.Analytics.setConsentMode((com.codename1.analytics.ConsentMode) adaptedArgs[0]); return null;
            }
        }
        if ("setDimension".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.analytics.Analytics.setDimension((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.analytics.Analytics.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.analytics.Analytics.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.analytics.Analytics.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("all".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsConsent.all();
            }
        }
        if ("builder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsConsent.builder();
            }
        }
        if ("denied".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsConsent.denied();
            }
        }
        if ("granted".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsConsent.granted();
            }
        }
        if ("none".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsConsent.none();
            }
        }
        throw unsupportedStatic(com.codename1.analytics.AnalyticsConsent.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.analytics.AnalyticsCrashReport.create((java.lang.Throwable) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        throw unsupportedStatic(com.codename1.analytics.AnalyticsCrashReport.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.analytics.AnalyticsEvent.create((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.analytics.AnalyticsEvent.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsService.class}, false);
                com.codename1.analytics.AnalyticsService.init((com.codename1.analytics.AnalyticsService) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.analytics.AnalyticsService.init((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("isAppsMode".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsService.isAppsMode();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsService.isEnabled();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.analytics.AnalyticsService.isFailSilently();
            }
        }
        if ("sendCrashReport".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.String.class, java.lang.Boolean.class}, false);
                com.codename1.analytics.AnalyticsService.sendCrashReport((java.lang.Throwable) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("setAppsMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.analytics.AnalyticsService.setAppsMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.analytics.AnalyticsService.setFailSilently(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.analytics.AnalyticsService.setReadTimeout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.analytics.AnalyticsService.setTimeout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("visit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.analytics.AnalyticsService.visit((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.analytics.AnalyticsService.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("registerBridge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.FirebaseAnalyticsProvider.Bridge.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.FirebaseAnalyticsProvider.Bridge.class}, false);
                com.codename1.analytics.FirebaseAnalyticsProvider.registerBridge((com.codename1.analytics.FirebaseAnalyticsProvider.Bridge) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.analytics.FirebaseAnalyticsProvider.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.analytics.CodenameOneAnalyticsProvider) {
            try {
                return invoke0((com.codename1.analytics.CodenameOneAnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.FirebaseAnalyticsProvider) {
            try {
                return invoke1((com.codename1.analytics.FirebaseAnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.GoogleAnalyticsProvider) {
            try {
                return invoke2((com.codename1.analytics.GoogleAnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.LegacyAnalyticsProviderAdapter) {
            try {
                return invoke3((com.codename1.analytics.LegacyAnalyticsProviderAdapter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.LoggingAnalyticsProvider) {
            try {
                return invoke4((com.codename1.analytics.LoggingAnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.MatomoAnalyticsProvider) {
            try {
                return invoke5((com.codename1.analytics.MatomoAnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AbstractAnalyticsProvider) {
            try {
                return invoke6((com.codename1.analytics.AbstractAnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsConsent) {
            try {
                return invoke7((com.codename1.analytics.AnalyticsConsent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsConsent.Builder) {
            try {
                return invoke8((com.codename1.analytics.AnalyticsConsent.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsContext) {
            try {
                return invoke9((com.codename1.analytics.AnalyticsContext) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsCrashReport) {
            try {
                return invoke10((com.codename1.analytics.AnalyticsCrashReport) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsCrashReport.Builder) {
            try {
                return invoke11((com.codename1.analytics.AnalyticsCrashReport.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsEvent) {
            try {
                return invoke12((com.codename1.analytics.AnalyticsEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsEvent.Builder) {
            try {
                return invoke13((com.codename1.analytics.AnalyticsEvent.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.AnalyticsProvider) {
            try {
                return invoke14((com.codename1.analytics.AnalyticsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.analytics.FirebaseAnalyticsProvider.Bridge) {
            try {
                return invoke15((com.codename1.analytics.FirebaseAnalyticsProvider.Bridge) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.analytics.CodenameOneAnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setBatchSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBatchSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setEndpoint((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.analytics.FirebaseAnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.analytics.GoogleAnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setEndpoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setEndpoint((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.analytics.LegacyAnalyticsProviderAdapter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.analytics.LoggingAnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clearLog".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearLog(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getLog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLog();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.analytics.MatomoAnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.analytics.AbstractAnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.analytics.AnalyticsConsent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asBuilder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asBuilder();
            }
        }
        if ("isAdStorage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAdStorage();
            }
        }
        if ("isAnalytics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAnalytics();
            }
        }
        if ("isCrashReporting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCrashReporting();
            }
        }
        if ("isPersonalization".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPersonalization();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.analytics.AnalyticsConsent.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("adStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.adStorage(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("all".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.all();
            }
        }
        if ("analytics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.analytics(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("crashReporting".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.crashReporting(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("none".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.none();
            }
        }
        if ("personalization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.personalization(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.analytics.AnalyticsContext typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAppName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAppName();
            }
        }
        if ("getAppVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAppVersion();
            }
        }
        if ("getClientId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClientId();
            }
        }
        if ("getLocale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocale();
            }
        }
        if ("getPlatform".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlatform();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.analytics.AnalyticsCrashReport typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asBuilder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asBuilder();
            }
        }
        if ("getCustomKeys".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCustomKeys();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getThrowable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThrowable();
            }
        }
        if ("isFatal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFatal();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.analytics.AnalyticsCrashReport.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("customKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.customKey((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.analytics.AnalyticsEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCategory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategory();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getParameters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParameters();
            }
        }
        if ("getTimestamp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestamp();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.analytics.AnalyticsEvent.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("category".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.category((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("param".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.param((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("timestamp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.timestamp(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.analytics.AnalyticsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsContext.class}, false);
                typedTarget.init((com.codename1.analytics.AnalyticsContext) adaptedArgs[0]); return null;
            }
        }
        if ("onConsentChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsConsent.class}, false);
                typedTarget.onConsentChanged((com.codename1.analytics.AnalyticsConsent) adaptedArgs[0]); return null;
            }
        }
        if ("reportCrash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCrashReport.class}, false);
                typedTarget.reportCrash((com.codename1.analytics.AnalyticsCrashReport) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsCapability.class}, false);
                return typedTarget.supports((com.codename1.analytics.AnalyticsCapability) adaptedArgs[0]);
            }
        }
        if ("trackEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.analytics.AnalyticsEvent.class}, false);
                typedTarget.trackEvent((com.codename1.analytics.AnalyticsEvent) adaptedArgs[0]); return null;
            }
        }
        if ("trackScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.trackScreen((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.analytics.FirebaseAnalyticsProvider.Bridge typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("logEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.logEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("logScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.logScreen((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUserProperty((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.analytics.AnalyticsCapability.class) return getStaticField0(name);
        if (type == com.codename1.analytics.ConsentMode.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("CRASH_REPORTING".equals(name)) return com.codename1.analytics.AnalyticsCapability.CRASH_REPORTING;
        if ("EVENTS".equals(name)) return com.codename1.analytics.AnalyticsCapability.EVENTS;
        if ("FUNNELS".equals(name)) return com.codename1.analytics.AnalyticsCapability.FUNNELS;
        if ("RAW_EXPORT".equals(name)) return com.codename1.analytics.AnalyticsCapability.RAW_EXPORT;
        if ("REAL_TIME".equals(name)) return com.codename1.analytics.AnalyticsCapability.REAL_TIME;
        if ("SCREEN_VIEWS".equals(name)) return com.codename1.analytics.AnalyticsCapability.SCREEN_VIEWS;
        if ("USER_PROPERTIES".equals(name)) return com.codename1.analytics.AnalyticsCapability.USER_PROPERTIES;
        throw unsupportedStaticField(com.codename1.analytics.AnalyticsCapability.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("OPT_IN".equals(name)) return com.codename1.analytics.ConsentMode.OPT_IN;
        if ("OPT_OUT".equals(name)) return com.codename1.analytics.ConsentMode.OPT_OUT;
        throw unsupportedStaticField(com.codename1.analytics.ConsentMode.class, name);
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
