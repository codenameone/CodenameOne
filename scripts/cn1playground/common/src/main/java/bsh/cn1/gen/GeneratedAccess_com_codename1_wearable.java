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

public final class GeneratedAccess_com_codename1_wearable {
    private GeneratedAccess_com_codename1_wearable() {
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
        if ("WearableConnection".equals(simpleName)) {
            return com.codename1.wearable.WearableConnection.class;
        }
        if ("WearableDataListener".equals(simpleName)) {
            return com.codename1.wearable.WearableDataListener.class;
        }
        if ("WearableMessage".equals(simpleName)) {
            return com.codename1.wearable.WearableMessage.class;
        }
        if ("WearableMessageListener".equals(simpleName)) {
            return com.codename1.wearable.WearableMessageListener.class;
        }
        if ("WearableNode".equals(simpleName)) {
            return com.codename1.wearable.WearableNode.class;
        }
        if ("WearableReplyHandler".equals(simpleName)) {
            return com.codename1.wearable.WearableReplyHandler.class;
        }
        if ("WearableStateListener".equals(simpleName)) {
            return com.codename1.wearable.WearableStateListener.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.wearable.WearableMessage.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.wearable.WearableMessage((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.wearable.WearableNode.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.wearable.WearableNode((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.wearable.WearableConnection.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.wearable.WearableMessage.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addDataListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableDataListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableDataListener.class}, false);
                com.codename1.wearable.WearableConnection.addDataListener((com.codename1.wearable.WearableDataListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMessageListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessageListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessageListener.class}, false);
                com.codename1.wearable.WearableConnection.addMessageListener((com.codename1.wearable.WearableMessageListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableStateListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableStateListener.class}, false);
                com.codename1.wearable.WearableConnection.addStateListener((com.codename1.wearable.WearableStateListener) adaptedArgs[0]); return null;
            }
        }
        if ("deliverDataChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                com.codename1.wearable.WearableConnection.deliverDataChanged((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
        }
        if ("deliverDataChangedTracked".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return com.codename1.wearable.WearableConnection.deliverDataChangedTracked((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.Runnable.class}, false);
                return com.codename1.wearable.WearableConnection.deliverDataChangedTracked((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1], (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("deliverDataRemoved".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.wearable.WearableConnection.deliverDataRemoved((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deliverMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.Integer.class}, false);
                com.codename1.wearable.WearableConnection.deliverMessage((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("deliverReply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class, java.lang.String.class}, false);
                com.codename1.wearable.WearableConnection.deliverReply(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("getConnectedNodes".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.wearable.WearableConnection.getConnectedNodes();
            }
        }
        if ("getData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.wearable.WearableConnection.getData((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getDataPaths".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.wearable.WearableConnection.getDataPaths();
            }
        }
        if ("isCompanionAppInstalled".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.wearable.WearableConnection.isCompanionAppInstalled();
            }
        }
        if ("isPaired".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.wearable.WearableConnection.isPaired();
            }
        }
        if ("isReachable".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.wearable.WearableConnection.isReachable();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.wearable.WearableConnection.isSupported();
            }
        }
        if ("notifyStateChanged".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.wearable.WearableConnection.notifyStateChanged(); return null;
            }
        }
        if ("putData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false);
                com.codename1.wearable.WearableConnection.putData((com.codename1.wearable.WearableMessage) adaptedArgs[0]); return null;
            }
        }
        if ("removeData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.wearable.WearableConnection.removeData((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeDataListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableDataListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableDataListener.class}, false);
                com.codename1.wearable.WearableConnection.removeDataListener((com.codename1.wearable.WearableDataListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMessageListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessageListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessageListener.class}, false);
                com.codename1.wearable.WearableConnection.removeMessageListener((com.codename1.wearable.WearableMessageListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableStateListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableStateListener.class}, false);
                com.codename1.wearable.WearableConnection.removeStateListener((com.codename1.wearable.WearableStateListener) adaptedArgs[0]); return null;
            }
        }
        if ("sendMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false);
                com.codename1.wearable.WearableConnection.sendMessage((com.codename1.wearable.WearableMessage) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class, com.codename1.wearable.WearableReplyHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class, com.codename1.wearable.WearableReplyHandler.class}, false);
                com.codename1.wearable.WearableConnection.sendMessage((com.codename1.wearable.WearableMessage) adaptedArgs[0], (com.codename1.wearable.WearableReplyHandler) adaptedArgs[1]); return null;
            }
        }
        if ("transferFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class}, false);
                com.codename1.wearable.WearableConnection.transferFile((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (byte[]) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.wearable.WearableConnection.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return com.codename1.wearable.WearableMessage.fromByteArray((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.wearable.WearableMessage.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.wearable.WearableMessage) {
            try {
                return invoke0((com.codename1.wearable.WearableMessage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.wearable.WearableNode) {
            try {
                return invoke1((com.codename1.wearable.WearableNode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.wearable.WearableDataListener) {
            try {
                return invoke2((com.codename1.wearable.WearableDataListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.wearable.WearableMessageListener) {
            try {
                return invoke3((com.codename1.wearable.WearableMessageListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.wearable.WearableReplyHandler) {
            try {
                return invoke4((com.codename1.wearable.WearableReplyHandler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.wearable.WearableStateListener) {
            try {
                return invoke5((com.codename1.wearable.WearableStateListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.wearable.WearableMessage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.contains((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.getBoolean((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return typedTarget.getBytes((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return typedTarget.getDouble((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("getInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.getInt((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("getKeys".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeys();
            }
        }
        if ("getLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false);
                return typedTarget.getLong((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getString((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("toByteArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toByteArray();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.wearable.WearableNode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("isNearby".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNearby();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.wearable.WearableDataListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("dataChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false);
                typedTarget.dataChanged((com.codename1.wearable.WearableMessage) adaptedArgs[0]); return null;
            }
        }
        if ("dataRemoved".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.dataRemoved((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.wearable.WearableMessageListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("messageReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class, java.lang.Boolean.class}, false);
                return typedTarget.messageReceived((com.codename1.wearable.WearableMessage) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.wearable.WearableReplyHandler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("replyFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.replyFailed((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("replyReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.wearable.WearableMessage.class}, false);
                typedTarget.replyReceived((com.codename1.wearable.WearableMessage) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.wearable.WearableStateListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connectionStateChanged".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.connectionStateChanged(); return null;
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
