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

public final class GeneratedAccess_com_codename1_nearby_transport {
    private GeneratedAccess_com_codename1_nearby_transport() {
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
        if ("Endpoint".equals(simpleName)) {
            return com.codename1.nearby.transport.Endpoint.class;
        }
        if ("IncomingConnection".equals(simpleName)) {
            return com.codename1.nearby.transport.IncomingConnection.class;
        }
        if ("NearbyTransport".equals(simpleName)) {
            return com.codename1.nearby.transport.NearbyTransport.class;
        }
        if ("Payload".equals(simpleName)) {
            return com.codename1.nearby.transport.Payload.class;
        }
        if ("PayloadStatus".equals(simpleName)) {
            return com.codename1.nearby.transport.PayloadStatus.class;
        }
        if ("PayloadTransferUpdate".equals(simpleName)) {
            return com.codename1.nearby.transport.PayloadTransferUpdate.class;
        }
        if ("TransportAdapter".equals(simpleName)) {
            return com.codename1.nearby.transport.TransportAdapter.class;
        }
        if ("TransportListener".equals(simpleName)) {
            return com.codename1.nearby.transport.TransportListener.class;
        }
        if ("TransportStrategy".equals(simpleName)) {
            return com.codename1.nearby.transport.TransportStrategy.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.nearby.transport.Endpoint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.nearby.transport.Endpoint((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.nearby.transport.IncomingConnection.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, java.lang.String.class}, false);
                return new com.codename1.nearby.transport.IncomingConnection((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.nearby.transport.PayloadTransferUpdate.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class, com.codename1.nearby.transport.PayloadStatus.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class, com.codename1.nearby.transport.PayloadStatus.class}, false);
                return new com.codename1.nearby.transport.PayloadTransferUpdate(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue(), (com.codename1.nearby.transport.PayloadStatus) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.nearby.transport.TransportAdapter.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nearby.transport.TransportAdapter();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.nearby.transport.NearbyTransport.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.nearby.transport.Payload.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addTransportListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.TransportListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.TransportListener.class}, false);
                com.codename1.nearby.transport.NearbyTransport.addTransportListener((com.codename1.nearby.transport.TransportListener) adaptedArgs[0]); return null;
            }
        }
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.nearby.transport.NearbyTransport.cancel(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("deliverConnectionRequested".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverConnectionRequested((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("deliverConnectionResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverConnectionResult((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3]); return null;
            }
        }
        if ("deliverDisconnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverDisconnected((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deliverEndpointFound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverEndpointFound((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("deliverPayloadProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class, java.lang.Integer.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverPayloadProgress((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).longValue(), ((Number) adaptedArgs[3]).longValue(), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("deliverPayloadReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, byte[].class, java.lang.String.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverPayloadReceived((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (byte[]) adaptedArgs[3], (java.lang.String) adaptedArgs[4]); return null;
            }
        }
        if ("deliverRequestFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverRequestFailed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("deliverRequestOk".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                com.codename1.nearby.transport.NearbyTransport.deliverRequestOk(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("disconnect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                com.codename1.nearby.transport.NearbyTransport.disconnect((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("getAvailability".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nearby.transport.NearbyTransport.getAvailability();
            }
        }
        if ("getMaxPayloadSize".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nearby.transport.NearbyTransport.getMaxPayloadSize();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nearby.transport.NearbyTransport.isSupported();
            }
        }
        if ("removeTransportListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.TransportListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.TransportListener.class}, false);
                com.codename1.nearby.transport.NearbyTransport.removeTransportListener((com.codename1.nearby.transport.TransportListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, java.lang.String.class}, false);
                return com.codename1.nearby.transport.NearbyTransport.requestConnection((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("requestPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.NearbyPermission[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.NearbyPermission[].class}, true);
                com.codename1.nearby.NearbyPermission[] varArgs = new com.codename1.nearby.NearbyPermission[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.nearby.NearbyPermission) adaptedArgs[i];
                }
                return com.codename1.nearby.transport.NearbyTransport.requestPermissions(varArgs);
            }
        }
        if ("resetForTest".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.nearby.transport.NearbyTransport.resetForTest(); return null;
            }
        }
        if ("send".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.Payload.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.Payload.class}, false);
                return com.codename1.nearby.transport.NearbyTransport.send((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.transport.Payload) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint[].class, com.codename1.nearby.transport.Payload.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint[].class, com.codename1.nearby.transport.Payload.class}, false);
                return com.codename1.nearby.transport.NearbyTransport.send((com.codename1.nearby.transport.Endpoint[]) adaptedArgs[0], (com.codename1.nearby.transport.Payload) adaptedArgs[1]);
            }
        }
        if ("startAdvertising".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.nearby.transport.TransportStrategy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.nearby.transport.TransportStrategy.class}, false);
                return com.codename1.nearby.transport.NearbyTransport.startAdvertising((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.nearby.transport.TransportStrategy) adaptedArgs[2]);
            }
        }
        if ("startDiscovery".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.nearby.transport.TransportStrategy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.nearby.transport.TransportStrategy.class}, false);
                return com.codename1.nearby.transport.NearbyTransport.startDiscovery((java.lang.String) adaptedArgs[0], (com.codename1.nearby.transport.TransportStrategy) adaptedArgs[1]);
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.nearby.transport.NearbyTransport.stop(); return null;
            }
        }
        if ("stopAdvertising".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.nearby.transport.NearbyTransport.stopAdvertising(); return null;
            }
        }
        if ("stopDiscovery".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.nearby.transport.NearbyTransport.stopDiscovery(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.nearby.transport.NearbyTransport.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.nearby.transport.Payload.fromBytes((byte[]) adaptedArgs[0]);
            }
        }
        if ("fromFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.nearby.transport.Payload.fromFile((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("received".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, byte[].class, java.lang.String.class}, false);
                return com.codename1.nearby.transport.Payload.received(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (byte[]) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        throw unsupportedStatic(com.codename1.nearby.transport.Payload.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.nearby.transport.Endpoint) {
            try {
                return invoke0((com.codename1.nearby.transport.Endpoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nearby.transport.IncomingConnection) {
            try {
                return invoke1((com.codename1.nearby.transport.IncomingConnection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nearby.transport.Payload) {
            try {
                return invoke2((com.codename1.nearby.transport.Payload) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nearby.transport.PayloadTransferUpdate) {
            try {
                return invoke3((com.codename1.nearby.transport.PayloadTransferUpdate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nearby.transport.TransportAdapter) {
            try {
                return invoke4((com.codename1.nearby.transport.TransportAdapter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nearby.transport.TransportListener) {
            try {
                return invoke5((com.codename1.nearby.transport.TransportListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.nearby.transport.Endpoint typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getServiceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceId();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.nearby.transport.IncomingConnection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accept".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.accept(); return null;
            }
        }
        if ("getAuthenticationToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthenticationToken();
            }
        }
        if ("getEndpoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndpoint();
            }
        }
        if ("isAnswered".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAnswered();
            }
        }
        if ("reject".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reject(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.nearby.transport.Payload typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBytes();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.nearby.transport.PayloadTransferUpdate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBytesTransferred".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBytesTransferred();
            }
        }
        if ("getPayloadId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPayloadId();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        if ("getTotalBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalBytes();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.nearby.transport.TransportAdapter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.connected((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("connectionFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.NearbyException.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.NearbyException.class}, false);
                typedTarget.connectionFailed((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.NearbyException) adaptedArgs[1]); return null;
            }
        }
        if ("connectionRequested".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.IncomingConnection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.IncomingConnection.class}, false);
                typedTarget.connectionRequested((com.codename1.nearby.transport.IncomingConnection) adaptedArgs[0]); return null;
            }
        }
        if ("disconnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.disconnected((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("endpointFound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.endpointFound((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("endpointLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.endpointLost((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("payloadProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.PayloadTransferUpdate.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.PayloadTransferUpdate.class}, false);
                typedTarget.payloadProgress((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.transport.PayloadTransferUpdate) adaptedArgs[1]); return null;
            }
        }
        if ("payloadReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.Payload.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.Payload.class}, false);
                typedTarget.payloadReceived((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.transport.Payload) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.nearby.transport.TransportListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.connected((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("connectionFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.NearbyException.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.NearbyException.class}, false);
                typedTarget.connectionFailed((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.NearbyException) adaptedArgs[1]); return null;
            }
        }
        if ("connectionRequested".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.IncomingConnection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.IncomingConnection.class}, false);
                typedTarget.connectionRequested((com.codename1.nearby.transport.IncomingConnection) adaptedArgs[0]); return null;
            }
        }
        if ("disconnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.disconnected((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("endpointFound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.endpointFound((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("endpointLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class}, false);
                typedTarget.endpointLost((com.codename1.nearby.transport.Endpoint) adaptedArgs[0]); return null;
            }
        }
        if ("payloadProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.PayloadTransferUpdate.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.PayloadTransferUpdate.class}, false);
                typedTarget.payloadProgress((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.transport.PayloadTransferUpdate) adaptedArgs[1]); return null;
            }
        }
        if ("payloadReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.Payload.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nearby.transport.Endpoint.class, com.codename1.nearby.transport.Payload.class}, false);
                typedTarget.payloadReceived((com.codename1.nearby.transport.Endpoint) adaptedArgs[0], (com.codename1.nearby.transport.Payload) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.nearby.transport.Payload.class) return getStaticField0(name);
        if (type == com.codename1.nearby.transport.PayloadStatus.class) return getStaticField1(name);
        if (type == com.codename1.nearby.transport.TransportStrategy.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("TYPE_BYTES".equals(name)) return com.codename1.nearby.transport.Payload.TYPE_BYTES;
        if ("TYPE_FILE".equals(name)) return com.codename1.nearby.transport.Payload.TYPE_FILE;
        throw unsupportedStaticField(com.codename1.nearby.transport.Payload.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CANCELED".equals(name)) return com.codename1.nearby.transport.PayloadStatus.CANCELED;
        if ("FAILURE".equals(name)) return com.codename1.nearby.transport.PayloadStatus.FAILURE;
        if ("IN_PROGRESS".equals(name)) return com.codename1.nearby.transport.PayloadStatus.IN_PROGRESS;
        if ("SUCCESS".equals(name)) return com.codename1.nearby.transport.PayloadStatus.SUCCESS;
        throw unsupportedStaticField(com.codename1.nearby.transport.PayloadStatus.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("CLUSTER".equals(name)) return com.codename1.nearby.transport.TransportStrategy.CLUSTER;
        if ("POINT_TO_POINT".equals(name)) return com.codename1.nearby.transport.TransportStrategy.POINT_TO_POINT;
        if ("STAR".equals(name)) return com.codename1.nearby.transport.TransportStrategy.STAR;
        throw unsupportedStaticField(com.codename1.nearby.transport.TransportStrategy.class, name);
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
