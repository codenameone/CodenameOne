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

public final class GeneratedAccess_com_codename1_mcp {
    private GeneratedAccess_com_codename1_mcp() {
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
        if ("MCP".equals(simpleName)) {
            return com.codename1.mcp.MCP.class;
        }
        if ("SocketTransportFactory".equals(simpleName)) {
            return com.codename1.mcp.MCP.SocketTransportFactory.class;
        }
        if ("StdioTransportFactory".equals(simpleName)) {
            return com.codename1.mcp.MCP.StdioTransportFactory.class;
        }
        if ("MCPClientDescriptor".equals(simpleName)) {
            return com.codename1.mcp.MCPClientDescriptor.class;
        }
        if ("MCPClientRegistrar".equals(simpleName)) {
            return com.codename1.mcp.MCPClientRegistrar.class;
        }
        if ("MCPClient".equals(simpleName)) {
            return com.codename1.mcp.MCPClientRegistrar.MCPClient.class;
        }
        if ("MCPServer".equals(simpleName)) {
            return com.codename1.mcp.MCPServer.class;
        }
        if ("MCPTransport".equals(simpleName)) {
            return com.codename1.mcp.MCPTransport.class;
        }
        if ("MCPVerbosity".equals(simpleName)) {
            return com.codename1.mcp.MCPVerbosity.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.mcp.MCPClientDescriptor.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.List.class}, false);
                return new com.codename1.mcp.MCPClientDescriptor((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.List) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.List.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.List.class, java.util.Map.class}, false);
                return new com.codename1.mcp.MCPClientDescriptor((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.List) adaptedArgs[2], (java.util.Map) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.mcp.MCPServer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.mcp.MCPServer();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.mcp.MCP.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.mcp.MCPClientRegistrar.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addTool".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.Tool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.Tool.class}, false);
                com.codename1.mcp.MCP.addTool((com.codename1.ai.Tool) adaptedArgs[0]); return null;
            }
        }
        if ("getServer".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCP.getServer();
            }
        }
        if ("getVerbosity".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCP.getVerbosity();
            }
        }
        if ("isRunning".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCP.isRunning();
            }
        }
        if ("isSocketSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCP.isSocketSupported();
            }
        }
        if ("isStdioSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCP.isStdioSupported();
            }
        }
        if ("setSocketTransportFactory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCP.SocketTransportFactory.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCP.SocketTransportFactory.class}, false);
                com.codename1.mcp.MCP.setSocketTransportFactory((com.codename1.mcp.MCP.SocketTransportFactory) adaptedArgs[0]); return null;
            }
        }
        if ("setStdioTransportFactory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCP.StdioTransportFactory.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCP.StdioTransportFactory.class}, false);
                com.codename1.mcp.MCP.setStdioTransportFactory((com.codename1.mcp.MCP.StdioTransportFactory) adaptedArgs[0]); return null;
            }
        }
        if ("setVerbosity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCPVerbosity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCPVerbosity.class}, false);
                com.codename1.mcp.MCP.setVerbosity((com.codename1.mcp.MCPVerbosity) adaptedArgs[0]); return null;
            }
        }
        if ("startSocketServer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.mcp.MCP.startSocketServer(toIntValue(adaptedArgs[0]));
            }
        }
        if ("startStdioServer".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCP.startStdioServer();
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.mcp.MCP.stop(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.mcp.MCP.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.mcp.MCPClientRegistrar.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.mcp.MCPClientRegistrar.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.mcp.MCPClientDescriptor) {
            try {
                return invoke0((com.codename1.mcp.MCPClientDescriptor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCPClientRegistrar) {
            try {
                return invoke1((com.codename1.mcp.MCPClientRegistrar) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCPClientRegistrar.MCPClient) {
            try {
                return invoke2((com.codename1.mcp.MCPClientRegistrar.MCPClient) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCPServer) {
            try {
                return invoke3((com.codename1.mcp.MCPServer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCPVerbosity) {
            try {
                return invoke4((com.codename1.mcp.MCPVerbosity) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCP.SocketTransportFactory) {
            try {
                return invoke5((com.codename1.mcp.MCP.SocketTransportFactory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCP.StdioTransportFactory) {
            try {
                return invoke6((com.codename1.mcp.MCP.StdioTransportFactory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.mcp.MCPTransport) {
            try {
                return invoke7((com.codename1.mcp.MCPTransport) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.mcp.MCPClientDescriptor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getArgs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArgs();
            }
        }
        if ("getCommand".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommand();
            }
        }
        if ("getEnv".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnv();
            }
        }
        if ("getServerName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServerName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.mcp.MCPClientRegistrar typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("detectClients".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.detectClients();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("register".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCPClientDescriptor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCPClientDescriptor.class}, false);
                return typedTarget.register((com.codename1.mcp.MCPClientDescriptor) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCPClientDescriptor.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCPClientDescriptor.class, java.util.List.class}, false);
                return typedTarget.register((com.codename1.mcp.MCPClientDescriptor) adaptedArgs[0], (java.util.List) adaptedArgs[1]);
            }
        }
        if ("unregister".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.unregister((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.mcp.MCPClientRegistrar.MCPClient typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getConfigPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfigPath();
            }
        }
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
        if ("isWritable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWritable();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.mcp.MCPServer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addTool".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.Tool.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.Tool.class}, false);
                typedTarget.addTool((com.codename1.ai.Tool) adaptedArgs[0]); return null;
            }
        }
        if ("getVerbosity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerbosity();
            }
        }
        if ("handleMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.handleMessage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isRunning".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRunning();
            }
        }
        if ("removeTool".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeTool((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setScreenshotEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScreenshotEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setServerInfo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setServerInfo((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setVerbosity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCPVerbosity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCPVerbosity.class}, false);
                typedTarget.setVerbosity((com.codename1.mcp.MCPVerbosity) adaptedArgs[0]); return null;
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCPTransport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCPTransport.class}, false);
                typedTarget.start((com.codename1.mcp.MCPTransport) adaptedArgs[0]); return null;
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.mcp.MCPVerbosity typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("includes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.mcp.MCPVerbosity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.mcp.MCPVerbosity.class}, false);
                return typedTarget.includes((com.codename1.mcp.MCPVerbosity) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.mcp.MCP.SocketTransportFactory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createSocketTransport".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createSocketTransport(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.mcp.MCP.StdioTransportFactory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createStdioTransport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createStdioTransport();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.mcp.MCPTransport typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("open".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.open(); return null;
            }
        }
        if ("readMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readMessage();
            }
        }
        if ("writeMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.writeMessage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.mcp.MCPServer.class) return getStaticField0(name);
        if (type == com.codename1.mcp.MCPVerbosity.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DEFAULT_PROTOCOL_VERSION".equals(name)) return com.codename1.mcp.MCPServer.DEFAULT_PROTOCOL_VERSION;
        if ("INTERNAL_ERROR".equals(name)) return com.codename1.mcp.MCPServer.INTERNAL_ERROR;
        if ("INVALID_PARAMS".equals(name)) return com.codename1.mcp.MCPServer.INVALID_PARAMS;
        if ("INVALID_REQUEST".equals(name)) return com.codename1.mcp.MCPServer.INVALID_REQUEST;
        if ("METHOD_NOT_FOUND".equals(name)) return com.codename1.mcp.MCPServer.METHOD_NOT_FOUND;
        if ("PARSE_ERROR".equals(name)) return com.codename1.mcp.MCPServer.PARSE_ERROR;
        if ("RESOURCE_NOT_FOUND".equals(name)) return com.codename1.mcp.MCPServer.RESOURCE_NOT_FOUND;
        throw unsupportedStaticField(com.codename1.mcp.MCPServer.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ERRORS".equals(name)) return com.codename1.mcp.MCPVerbosity.ERRORS;
        if ("FULL".equals(name)) return com.codename1.mcp.MCPVerbosity.FULL;
        if ("OFF".equals(name)) return com.codename1.mcp.MCPVerbosity.OFF;
        if ("SUMMARY".equals(name)) return com.codename1.mcp.MCPVerbosity.SUMMARY;
        throw unsupportedStaticField(com.codename1.mcp.MCPVerbosity.class, name);
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
