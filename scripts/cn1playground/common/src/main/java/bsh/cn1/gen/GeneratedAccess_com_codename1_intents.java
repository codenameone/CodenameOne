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

public final class GeneratedAccess_com_codename1_intents {
    private GeneratedAccess_com_codename1_intents() {
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
        if ("AppEntity".equals(simpleName)) {
            return com.codename1.intents.AppEntity.class;
        }
        if ("DynamicIntent".equals(simpleName)) {
            return com.codename1.intents.DynamicIntent.class;
        }
        if ("EntitySelectionHandler".equals(simpleName)) {
            return com.codename1.intents.EntitySelectionHandler.class;
        }
        if ("Exposure".equals(simpleName)) {
            return com.codename1.intents.Exposure.class;
        }
        if ("IntentCompletion".equals(simpleName)) {
            return com.codename1.intents.IntentCompletion.class;
        }
        if ("IntentContext".equals(simpleName)) {
            return com.codename1.intents.IntentContext.class;
        }
        if ("IntentDates".equals(simpleName)) {
            return com.codename1.intents.IntentDates.class;
        }
        if ("IntentDeclaration".equals(simpleName)) {
            return com.codename1.intents.IntentDeclaration.class;
        }
        if ("IntentDispatcher".equals(simpleName)) {
            return com.codename1.intents.IntentDispatcher.class;
        }
        if ("IntentParameterInfo".equals(simpleName)) {
            return com.codename1.intents.IntentParameterInfo.class;
        }
        if ("IntentParameterType".equals(simpleName)) {
            return com.codename1.intents.IntentParameterType.class;
        }
        if ("IntentResult".equals(simpleName)) {
            return com.codename1.intents.IntentResult.class;
        }
        if ("IntentSerializer".equals(simpleName)) {
            return com.codename1.intents.IntentSerializer.class;
        }
        if ("IntentSource".equals(simpleName)) {
            return com.codename1.intents.IntentSource.class;
        }
        if ("Intents".equals(simpleName)) {
            return com.codename1.intents.Intents.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.intents.AppEntity.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.intents.AppEntity((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.intents.DynamicIntent.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.intents.DynamicIntent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.intents.IntentContext.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.IntentSource.class, java.lang.Boolean.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.IntentSource.class, java.lang.Boolean.class, java.lang.Long.class}, false);
                return new com.codename1.intents.IntentContext((com.codename1.intents.IntentSource) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        if (type == com.codename1.intents.IntentDeclaration.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.util.List.class, java.util.List.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.util.List.class, java.util.List.class, java.util.List.class}, false);
                return new com.codename1.intents.IntentDeclaration((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue(), (java.lang.String) adaptedArgs[6], toIntValue(adaptedArgs[7]), (java.util.List) adaptedArgs[8], (java.util.List) adaptedArgs[9], (java.util.List) adaptedArgs[10]);
            }
        }
        if (type == com.codename1.intents.IntentParameterInfo.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.intents.IntentParameterType.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.intents.IntentParameterType.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.util.List.class}, false);
                return new com.codename1.intents.IntentParameterInfo((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.intents.IntentParameterType) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (java.util.List) adaptedArgs[6]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.intents.IntentParameterType.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.util.List.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.intents.IntentParameterType.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.util.List.class, java.lang.Integer.class}, false);
                return new com.codename1.intents.IntentParameterInfo((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.intents.IntentParameterType) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (java.util.List) adaptedArgs[6], toIntValue(adaptedArgs[7]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.intents.IntentDates.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.intents.IntentResult.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.intents.IntentSerializer.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.intents.Intents.class) return invokeStatic3(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.intents.IntentDates.parse((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.intents.IntentDates.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("entity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.AppEntity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.AppEntity.class}, false);
                return com.codename1.intents.IntentResult.entity((com.codename1.intents.AppEntity) adaptedArgs[0]);
            }
        }
        if ("failed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.intents.IntentResult.failed((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("ok".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.IntentResult.ok();
            }
        }
        if ("opens".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.intents.IntentResult.opens((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("spoken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.intents.IntentResult.spoken((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("value".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return com.codename1.intents.IntentResult.value((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.intents.IntentResult.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("mergeParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.String.class}, false);
                return com.codename1.intents.IntentSerializer.mergeParams((java.util.Map) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("parsePayload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.intents.IntentSerializer.parsePayload((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("serializeDeclarations".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return com.codename1.intents.IntentSerializer.serializeDeclarations((java.util.List) adaptedArgs[0]);
            }
        }
        if ("serializeEntities".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.Map.class}, false);
                return com.codename1.intents.IntentSerializer.serializeEntities((java.util.List) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.Map.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.Map.class, java.lang.Boolean.class}, false);
                return com.codename1.intents.IntentSerializer.serializeEntities((java.util.List) adaptedArgs[0], (java.util.Map) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("serializeEntityRef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.intents.IntentSerializer.serializeEntityRef((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("serializeParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.intents.IntentSerializer.serializeParams((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("serializeResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.IntentResult.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.IntentResult.class, java.util.Map.class}, false);
                return com.codename1.intents.IntentSerializer.serializeResult((com.codename1.intents.IntentResult) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.intents.IntentSerializer.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("areIntentsSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.areIntentsSupported();
            }
        }
        if ("asTools".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.asTools();
            }
        }
        if ("clearIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.intents.Intents.clearIndex((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("dispatchInvocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class, com.codename1.intents.IntentSource.class, java.lang.Boolean.class, com.codename1.intents.IntentCompletion.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class, com.codename1.intents.IntentSource.class, java.lang.Boolean.class, com.codename1.intents.IntentCompletion.class}, false);
                com.codename1.intents.Intents.dispatchInvocation((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1], (com.codename1.intents.IntentSource) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), (com.codename1.intents.IntentCompletion) adaptedArgs[4]); return null;
            }
        }
        if ("dispatchSpotlightSelection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.intents.Intents.dispatchSpotlightSelection((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("dispatchUserActivity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return com.codename1.intents.Intents.dispatchUserActivity((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("donate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                com.codename1.intents.Intents.donate((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]); return null;
            }
        }
        if ("getDeclaration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.intents.Intents.getDeclaration((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getDeclarations".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.getDeclarations();
            }
        }
        if ("getDefaultTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.getDefaultTimeout();
            }
        }
        if ("getDynamicIntent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.intents.Intents.getDynamicIntent((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("index".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.AppEntity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.AppEntity.class}, false);
                com.codename1.intents.Intents.index((com.codename1.intents.AppEntity) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                com.codename1.intents.Intents.index((java.util.List) adaptedArgs[0]); return null;
            }
        }
        if ("invoke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return com.codename1.intents.Intents.invoke((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if ("isHeadlessExecutionSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.isHeadlessExecutionSupported();
            }
        }
        if ("isIndexingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.isIndexingSupported();
            }
        }
        if ("isVoiceInvocationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.intents.Intents.isVoiceInvocationSupported();
            }
        }
        if ("publishPendingDeclarations".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.intents.Intents.publishPendingDeclarations(); return null;
            }
        }
        if ("queryEntities".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.intents.Intents.queryEntities((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("registerDynamicIntent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.DynamicIntent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.DynamicIntent.class}, false);
                com.codename1.intents.Intents.registerDynamicIntent((com.codename1.intents.DynamicIntent) adaptedArgs[0]); return null;
            }
        }
        if ("removeFromIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.intents.Intents.removeFromIndex((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setBridge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.spi.IntentBridge.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.spi.IntentBridge.class}, false);
                com.codename1.intents.Intents.setBridge((com.codename1.intents.spi.IntentBridge) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.intents.Intents.setDefaultTimeout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDispatcher".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.IntentDispatcher.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.IntentDispatcher.class}, false);
                com.codename1.intents.Intents.setDispatcher((com.codename1.intents.IntentDispatcher) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.EntitySelectionHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.EntitySelectionHandler.class}, false);
                com.codename1.intents.Intents.setSelectionHandler((com.codename1.intents.EntitySelectionHandler) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.intents.Intents.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.intents.AppEntity) {
            try {
                return invoke0((com.codename1.intents.AppEntity) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.DynamicIntent) {
            try {
                return invoke1((com.codename1.intents.DynamicIntent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.IntentContext) {
            try {
                return invoke2((com.codename1.intents.IntentContext) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.IntentDeclaration) {
            try {
                return invoke3((com.codename1.intents.IntentDeclaration) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.IntentParameterInfo) {
            try {
                return invoke4((com.codename1.intents.IntentParameterInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.IntentResult) {
            try {
                return invoke5((com.codename1.intents.IntentResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.EntitySelectionHandler) {
            try {
                return invoke6((com.codename1.intents.EntitySelectionHandler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.IntentCompletion) {
            try {
                return invoke7((com.codename1.intents.IntentCompletion) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.intents.IntentDispatcher) {
            try {
                return invoke8((com.codename1.intents.IntentDispatcher) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.intents.AppEntity typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addKeywords".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.addKeywords(varArgs);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImage();
            }
        }
        if ("getKeywords".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeywords();
            }
        }
        if ("getSubtitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubtitle();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.EncodedImage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.EncodedImage.class}, false);
                return typedTarget.setImage((com.codename1.ui.EncodedImage) adaptedArgs[0]);
            }
        }
        if ("setSubtitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSubtitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.intents.DynamicIntent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return typedTarget.bind((java.util.Map) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.bind((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("getBaseIntentId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaseIntentId();
            }
        }
        if ("getBoundParameters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBoundParameters();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.intents.IntentContext typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cancel(); return null;
            }
        }
        if ("getDeadline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeadline();
            }
        }
        if ("getRemainingTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRemainingTime();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("isCancelled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCancelled();
            }
        }
        if ("isHeadless".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHeadless();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.intents.IntentDeclaration typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getExposure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExposure();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getOpensRoute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpensRoute();
            }
        }
        if ("getParameter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getParameter((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getParameters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParameters();
            }
        }
        if ("getPhrases".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPhrases();
            }
        }
        if ("getTimeoutSeconds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeoutSeconds();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isDestructive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDestructive();
            }
        }
        if ("isDiscoverable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDiscoverable();
            }
        }
        if ("isExposedTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.Exposure.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.Exposure.class}, false);
                return typedTarget.isExposedTo((com.codename1.intents.Exposure) adaptedArgs[0]);
            }
        }
        if ("isHeadless".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHeadless();
            }
        }
        if ("runsHeadless".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.runsHeadless();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.intents.IntentParameterInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultValue();
            }
        }
        if ("getEntityType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEntityType();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNumericWidthBits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNumericWidthBits();
            }
        }
        if ("getOptions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOptions();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("isRequired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRequired();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.intents.IntentResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDialog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDialog();
            }
        }
        if ("getEntity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEntity();
            }
        }
        if ("getErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrorMessage();
            }
        }
        if ("getOpenUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpenUrl();
            }
        }
        if ("getSnippet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSnippet();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("isFailed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailed();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("withDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.withDialog((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("withOpenUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.withOpenUrl((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("withSnippet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.surfaces.SurfaceNode.class}, false);
                return typedTarget.withSnippet((com.codename1.surfaces.SurfaceNode) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.intents.EntitySelectionHandler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onEntitySelected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.AppEntity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.AppEntity.class}, false);
                typedTarget.onEntitySelected((com.codename1.intents.AppEntity) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.intents.IntentCompletion typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onIntentResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.intents.IntentResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.intents.IntentResult.class}, false);
                typedTarget.onIntentResult((com.codename1.intents.IntentResult) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.intents.IntentDispatcher typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("describe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.describe();
            }
        }
        if ("invoke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class, com.codename1.intents.IntentContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class, com.codename1.intents.IntentContext.class}, false);
                return typedTarget.invoke((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1], (com.codename1.intents.IntentContext) adaptedArgs[2]);
            }
        }
        if ("queryEntities".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.queryEntities((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.intents.Exposure.class) return getStaticField0(name);
        if (type == com.codename1.intents.IntentParameterType.class) return getStaticField1(name);
        if (type == com.codename1.intents.IntentSource.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ASSISTANT".equals(name)) return com.codename1.intents.Exposure.ASSISTANT;
        if ("MODEL".equals(name)) return com.codename1.intents.Exposure.MODEL;
        throw unsupportedStaticField(com.codename1.intents.Exposure.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("BOOLEAN".equals(name)) return com.codename1.intents.IntentParameterType.BOOLEAN;
        if ("DATE".equals(name)) return com.codename1.intents.IntentParameterType.DATE;
        if ("ENTITY".equals(name)) return com.codename1.intents.IntentParameterType.ENTITY;
        if ("INTEGER".equals(name)) return com.codename1.intents.IntentParameterType.INTEGER;
        if ("NUMBER".equals(name)) return com.codename1.intents.IntentParameterType.NUMBER;
        if ("STRING".equals(name)) return com.codename1.intents.IntentParameterType.STRING;
        throw unsupportedStaticField(com.codename1.intents.IntentParameterType.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("IN_APP".equals(name)) return com.codename1.intents.IntentSource.IN_APP;
        if ("MODEL".equals(name)) return com.codename1.intents.IntentSource.MODEL;
        if ("SHORTCUT".equals(name)) return com.codename1.intents.IntentSource.SHORTCUT;
        if ("SPOTLIGHT".equals(name)) return com.codename1.intents.IntentSource.SPOTLIGHT;
        if ("UNKNOWN".equals(name)) return com.codename1.intents.IntentSource.UNKNOWN;
        if ("VOICE".equals(name)) return com.codename1.intents.IntentSource.VOICE;
        if ("WIDGET".equals(name)) return com.codename1.intents.IntentSource.WIDGET;
        throw unsupportedStaticField(com.codename1.intents.IntentSource.class, name);
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
