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

public final class GeneratedAccess_com_codename1_home_commissioning {
    private GeneratedAccess_com_codename1_home_commissioning() {
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
        if ("Commissioner".equals(simpleName)) {
            return com.codename1.home.commissioning.Commissioner.class;
        }
        if ("CommissioningRequest".equals(simpleName)) {
            return com.codename1.home.commissioning.CommissioningRequest.class;
        }
        if ("CommissioningResult".equals(simpleName)) {
            return com.codename1.home.commissioning.CommissioningResult.class;
        }
        if ("CommissioningStyle".equals(simpleName)) {
            return com.codename1.home.commissioning.CommissioningStyle.class;
        }
        if ("SetupPayload".equals(simpleName)) {
            return com.codename1.home.commissioning.SetupPayload.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.home.commissioning.CommissioningRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.home.commissioning.CommissioningRequest();
            }
        }
        if (type == com.codename1.home.commissioning.CommissioningResult.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.home.commissioning.CommissioningResult((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.home.commissioning.SetupPayload.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.home.commissioning.SetupPayload.isValid((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.home.commissioning.SetupPayload.parse((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.commissioning.SetupPayload.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.home.commissioning.Commissioner) {
            try {
                return invoke0((com.codename1.home.commissioning.Commissioner) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.commissioning.CommissioningRequest) {
            try {
                return invoke1((com.codename1.home.commissioning.CommissioningRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.commissioning.CommissioningResult) {
            try {
                return invoke2((com.codename1.home.commissioning.CommissioningResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.commissioning.SetupPayload) {
            try {
                return invoke3((com.codename1.home.commissioning.SetupPayload) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.home.commissioning.Commissioner typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("commission".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.commissioning.CommissioningRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.commissioning.CommissioningRequest.class}, false);
                return typedTarget.commission((com.codename1.home.commissioning.CommissioningRequest) adaptedArgs[0]);
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("openEcosystemApp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.openEcosystemApp();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.home.commissioning.CommissioningRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getRawSetupPayload".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawSetupPayload();
            }
        }
        if ("getRoomId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoomId();
            }
        }
        if ("getSetupPayload".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSetupPayload();
            }
        }
        if ("getStructureId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStructureId();
            }
        }
        if ("getSuggestedName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSuggestedName();
            }
        }
        if ("getTimeoutMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeoutMillis();
            }
        }
        if ("isCommissionToThisApp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCommissionToThisApp();
            }
        }
        if ("setCommissionToThisApp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setCommissionToThisApp(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setRawSetupPayload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setRawSetupPayload((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setRoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeRoom.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeRoom.class}, false);
                return typedTarget.setRoom((com.codename1.home.HomeRoom) adaptedArgs[0]);
            }
        }
        if ("setRoomId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setRoomId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSetupPayload".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.commissioning.SetupPayload.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.commissioning.SetupPayload.class}, false);
                return typedTarget.setSetupPayload((com.codename1.home.commissioning.SetupPayload) adaptedArgs[0]);
            }
        }
        if ("setStructure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeStructure.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeStructure.class}, false);
                return typedTarget.setStructure((com.codename1.home.HomeStructure) adaptedArgs[0]);
            }
        }
        if ("setStructureId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setStructureId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSuggestedName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSuggestedName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTimeoutMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setTimeoutMillis(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.home.commissioning.CommissioningResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getAccessoryName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryName();
            }
        }
        if ("getStructureId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStructureId();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("wasCommissionedToThisApp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wasCommissionedToThisApp();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.home.commissioning.SetupPayload typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCustomFlow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCustomFlow();
            }
        }
        if ("getDiscoveryCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDiscoveryCapabilities();
            }
        }
        if ("getDiscriminator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDiscriminator();
            }
        }
        if ("getPasscode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPasscode();
            }
        }
        if ("getProductId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProductId();
            }
        }
        if ("getRaw".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRaw();
            }
        }
        if ("getVendorId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVendorId();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("isFromQrCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFromQrCode();
            }
        }
        if ("isShortDiscriminator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShortDiscriminator();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.home.commissioning.CommissioningStyle.class) return getStaticField0(name);
        if (type == com.codename1.home.commissioning.SetupPayload.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ECOSYSTEM_APP_HANDOFF".equals(name)) return com.codename1.home.commissioning.CommissioningStyle.ECOSYSTEM_APP_HANDOFF;
        if ("NONE".equals(name)) return com.codename1.home.commissioning.CommissioningStyle.NONE;
        if ("OS_OWNED_UI".equals(name)) return com.codename1.home.commissioning.CommissioningStyle.OS_OWNED_UI;
        throw unsupportedStaticField(com.codename1.home.commissioning.CommissioningStyle.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("DISCOVERY_BLE".equals(name)) return com.codename1.home.commissioning.SetupPayload.DISCOVERY_BLE;
        if ("DISCOVERY_ON_NETWORK".equals(name)) return com.codename1.home.commissioning.SetupPayload.DISCOVERY_ON_NETWORK;
        if ("DISCOVERY_SOFT_AP".equals(name)) return com.codename1.home.commissioning.SetupPayload.DISCOVERY_SOFT_AP;
        throw unsupportedStaticField(com.codename1.home.commissioning.SetupPayload.class, name);
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
