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

public final class GeneratedAccess_com_codename1_io_grpc {
    private GeneratedAccess_com_codename1_io_grpc() {
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
        if ("GrpcClients".equals(simpleName)) {
            return com.codename1.io.grpc.GrpcClients.class;
        }
        if ("Factory".equals(simpleName)) {
            return com.codename1.io.grpc.GrpcClients.Factory.class;
        }
        if ("GrpcException".equals(simpleName)) {
            return com.codename1.io.grpc.GrpcException.class;
        }
        if ("GrpcResponse".equals(simpleName)) {
            return com.codename1.io.grpc.GrpcResponse.class;
        }
        if ("GrpcWeb".equals(simpleName)) {
            return com.codename1.io.grpc.GrpcWeb.class;
        }
        if ("ProtoCodec".equals(simpleName)) {
            return com.codename1.io.grpc.ProtoCodec.class;
        }
        if ("ProtoCodecs".equals(simpleName)) {
            return com.codename1.io.grpc.ProtoCodecs.class;
        }
        if ("ProtoReader".equals(simpleName)) {
            return com.codename1.io.grpc.ProtoReader.class;
        }
        if ("PackedReader".equals(simpleName)) {
            return com.codename1.io.grpc.ProtoReader.PackedReader.class;
        }
        if ("ProtoWriter".equals(simpleName)) {
            return com.codename1.io.grpc.ProtoWriter.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.grpc.GrpcException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.io.grpc.GrpcException(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.io.grpc.GrpcResponse.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.lang.String.class}, false);
                return new com.codename1.io.grpc.GrpcResponse(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Object) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.io.grpc.ProtoReader.class) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return new com.codename1.io.grpc.ProtoReader((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.io.grpc.ProtoReader((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.grpc.GrpcClients.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.grpc.GrpcWeb.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.io.grpc.ProtoCodecs.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.io.grpc.ProtoReader.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.io.grpc.ProtoWriter.class) return invokeStatic4(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false);
                return com.codename1.io.grpc.GrpcClients.create((java.lang.Class) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.io.grpc.GrpcClients.Factory.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.io.grpc.GrpcClients.Factory.class}, false);
                com.codename1.io.grpc.GrpcClients.register((java.lang.Class) adaptedArgs[0], (com.codename1.io.grpc.GrpcClients.Factory) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.grpc.GrpcClients.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, com.codename1.io.grpc.ProtoCodec.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, com.codename1.io.grpc.ProtoCodec.class}, false);
                return com.codename1.io.grpc.GrpcWeb.decode((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.io.grpc.ProtoCodec) adaptedArgs[2]);
            }
        }
        if ("frame".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.io.grpc.ProtoCodec.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.io.grpc.ProtoCodec.class}, false);
                return com.codename1.io.grpc.GrpcWeb.frame((java.lang.Object) adaptedArgs[0], (com.codename1.io.grpc.ProtoCodec) adaptedArgs[1]);
            }
        }
        if ("invokeUnary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, com.codename1.io.grpc.ProtoCodec.class, com.codename1.io.grpc.ProtoCodec.class, com.codename1.util.OnComplete.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, com.codename1.io.grpc.ProtoCodec.class, com.codename1.io.grpc.ProtoCodec.class, com.codename1.util.OnComplete.class}, false);
                com.codename1.io.grpc.GrpcWeb.invokeUnary((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.Object) adaptedArgs[4], (com.codename1.io.grpc.ProtoCodec) adaptedArgs[5], (com.codename1.io.grpc.ProtoCodec) adaptedArgs[6], (com.codename1.util.OnComplete) adaptedArgs[7]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.grpc.GrpcWeb.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("lookup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                return com.codename1.io.grpc.ProtoCodecs.lookup((java.lang.Class) adaptedArgs[0]);
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.io.grpc.ProtoCodec.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.io.grpc.ProtoCodec.class}, false);
                com.codename1.io.grpc.ProtoCodecs.register((java.lang.Class) adaptedArgs[0], (com.codename1.io.grpc.ProtoCodec) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.io.grpc.ProtoCodecs.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.io.grpc.ProtoReader.of((byte[]) adaptedArgs[0]);
            }
        }
        if ("zagZig32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.io.grpc.ProtoReader.zagZig32(toIntValue(adaptedArgs[0]));
            }
        }
        if ("zagZig64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.io.grpc.ProtoReader.zagZig64(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.io.grpc.ProtoReader.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("zigZag32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.io.grpc.ProtoWriter.zigZag32(toIntValue(adaptedArgs[0]));
            }
        }
        if ("zigZag64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.io.grpc.ProtoWriter.zigZag64(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.io.grpc.ProtoWriter.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.grpc.GrpcException) {
            try {
                return invoke0((com.codename1.io.grpc.GrpcException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.grpc.GrpcResponse) {
            try {
                return invoke1((com.codename1.io.grpc.GrpcResponse) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.grpc.ProtoReader) {
            try {
                return invoke2((com.codename1.io.grpc.ProtoReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.grpc.ProtoWriter) {
            try {
                return invoke3((com.codename1.io.grpc.ProtoWriter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.grpc.GrpcClients.Factory) {
            try {
                return invoke4((com.codename1.io.grpc.GrpcClients.Factory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.grpc.ProtoCodec) {
            try {
                return invoke5((com.codename1.io.grpc.ProtoCodec) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.grpc.ProtoReader.PackedReader) {
            try {
                return invoke6((com.codename1.io.grpc.ProtoReader.PackedReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.grpc.GrpcException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getHttpCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpCode();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.grpc.GrpcResponse typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getHttpCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpCode();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
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
        if ("isOk".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOk();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.grpc.ProtoReader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isAtEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAtEnd();
            }
        }
        if ("readBool".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readBool();
            }
        }
        if ("readBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readBytes();
            }
        }
        if ("readDouble".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readDouble();
            }
        }
        if ("readFixed32".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readFixed32();
            }
        }
        if ("readFixed64".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readFixed64();
            }
        }
        if ("readFloat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readFloat();
            }
        }
        if ("readMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoCodec.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoCodec.class}, false);
                return typedTarget.readMessage((com.codename1.io.grpc.ProtoCodec) adaptedArgs[0]);
            }
        }
        if ("readPacked".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, com.codename1.io.grpc.ProtoReader.PackedReader.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, com.codename1.io.grpc.ProtoReader.PackedReader.class}, false);
                typedTarget.readPacked((java.util.List) adaptedArgs[0], (com.codename1.io.grpc.ProtoReader.PackedReader) adaptedArgs[1]); return null;
            }
        }
        if ("readSInt32".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readSInt32();
            }
        }
        if ("readSInt64".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readSInt64();
            }
        }
        if ("readString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readString();
            }
        }
        if ("readTag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readTag();
            }
        }
        if ("readVarint32".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readVarint32();
            }
        }
        if ("readVarint64".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readVarint64();
            }
        }
        if ("remaining".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remaining();
            }
        }
        if ("skipField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.skipField(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.grpc.ProtoWriter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("writeBool".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.writeBool(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("writeBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                typedTarget.writeBytes(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]); return null;
            }
        }
        if ("writeDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false);
                typedTarget.writeDouble(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
        }
        if ("writeFixed32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.writeFixed32(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("writeFixed32Field".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.writeFixed32Field(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("writeFixed64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.writeFixed64(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("writeFixed64Field".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.writeFixed64Field(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue()); return null;
            }
        }
        if ("writeFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.writeFloat(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("writeInt32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.writeInt32(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("writeInt64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.writeInt64(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue()); return null;
            }
        }
        if ("writeMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.io.grpc.ProtoCodec.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.io.grpc.ProtoCodec.class}, false);
                typedTarget.writeMessage(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (com.codename1.io.grpc.ProtoCodec) adaptedArgs[2]); return null;
            }
        }
        if ("writeMessageList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class, com.codename1.io.grpc.ProtoCodec.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class, com.codename1.io.grpc.ProtoCodec.class}, false);
                typedTarget.writeMessageList(toIntValue(adaptedArgs[0]), (java.util.List) adaptedArgs[1], (com.codename1.io.grpc.ProtoCodec) adaptedArgs[2]); return null;
            }
        }
        if ("writePackedInt32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class}, false);
                typedTarget.writePackedInt32(toIntValue(adaptedArgs[0]), (java.util.List) adaptedArgs[1]); return null;
            }
        }
        if ("writePackedInt64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class}, false);
                typedTarget.writePackedInt64(toIntValue(adaptedArgs[0]), (java.util.List) adaptedArgs[1]); return null;
            }
        }
        if ("writeSInt32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.writeSInt32(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("writeSInt64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.writeSInt64(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue()); return null;
            }
        }
        if ("writeString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.writeString(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("writeStringList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.List.class}, false);
                typedTarget.writeStringList(toIntValue(adaptedArgs[0]), (java.util.List) adaptedArgs[1]); return null;
            }
        }
        if ("writeTag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.writeTag(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("writeUInt32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.writeUInt32(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("writeUInt64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.writeUInt64(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue()); return null;
            }
        }
        if ("writeVarint32".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.writeVarint32(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("writeVarint64".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.writeVarint64(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.grpc.GrpcClients.Factory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.create((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.grpc.ProtoCodec typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoReader.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoReader.class}, false);
                return typedTarget.read((com.codename1.io.grpc.ProtoReader) adaptedArgs[0]);
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoWriter.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoWriter.class, java.lang.Object.class}, false);
                typedTarget.write((com.codename1.io.grpc.ProtoWriter) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.grpc.ProtoReader.PackedReader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoReader.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.grpc.ProtoReader.class}, false);
                return typedTarget.read((com.codename1.io.grpc.ProtoReader) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.grpc.GrpcResponse.class) return getStaticField0(name);
        if (type == com.codename1.io.grpc.GrpcWeb.class) return getStaticField1(name);
        if (type == com.codename1.io.grpc.ProtoWriter.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("STATUS_ABORTED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_ABORTED;
        if ("STATUS_ALREADY_EXISTS".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_ALREADY_EXISTS;
        if ("STATUS_CANCELLED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_CANCELLED;
        if ("STATUS_DATA_LOSS".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_DATA_LOSS;
        if ("STATUS_DEADLINE_EXCEEDED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_DEADLINE_EXCEEDED;
        if ("STATUS_FAILED_PRECONDITION".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_FAILED_PRECONDITION;
        if ("STATUS_INTERNAL".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_INTERNAL;
        if ("STATUS_INVALID_ARGUMENT".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_INVALID_ARGUMENT;
        if ("STATUS_NOT_FOUND".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_NOT_FOUND;
        if ("STATUS_OK".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_OK;
        if ("STATUS_OUT_OF_RANGE".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_OUT_OF_RANGE;
        if ("STATUS_PERMISSION_DENIED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_PERMISSION_DENIED;
        if ("STATUS_RESOURCE_EXHAUSTED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_RESOURCE_EXHAUSTED;
        if ("STATUS_TRANSPORT_FAILURE".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_TRANSPORT_FAILURE;
        if ("STATUS_UNAUTHENTICATED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_UNAUTHENTICATED;
        if ("STATUS_UNAVAILABLE".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_UNAVAILABLE;
        if ("STATUS_UNIMPLEMENTED".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_UNIMPLEMENTED;
        if ("STATUS_UNKNOWN".equals(name)) return com.codename1.io.grpc.GrpcResponse.STATUS_UNKNOWN;
        throw unsupportedStaticField(com.codename1.io.grpc.GrpcResponse.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CONTENT_TYPE".equals(name)) return com.codename1.io.grpc.GrpcWeb.CONTENT_TYPE;
        if ("FLAG_TRAILER".equals(name)) return com.codename1.io.grpc.GrpcWeb.FLAG_TRAILER;
        throw unsupportedStaticField(com.codename1.io.grpc.GrpcWeb.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("WIRE_I32".equals(name)) return com.codename1.io.grpc.ProtoWriter.WIRE_I32;
        if ("WIRE_I64".equals(name)) return com.codename1.io.grpc.ProtoWriter.WIRE_I64;
        if ("WIRE_LEN".equals(name)) return com.codename1.io.grpc.ProtoWriter.WIRE_LEN;
        if ("WIRE_VARINT".equals(name)) return com.codename1.io.grpc.ProtoWriter.WIRE_VARINT;
        throw unsupportedStaticField(com.codename1.io.grpc.ProtoWriter.class, name);
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
