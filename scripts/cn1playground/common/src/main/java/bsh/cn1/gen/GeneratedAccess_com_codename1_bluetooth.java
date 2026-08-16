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

public final class GeneratedAccess_com_codename1_bluetooth {
    private GeneratedAccess_com_codename1_bluetooth() {
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
        if ("AdapterState".equals(simpleName)) {
            return com.codename1.bluetooth.AdapterState.class;
        }
        if ("AdapterStateListener".equals(simpleName)) {
            return com.codename1.bluetooth.AdapterStateListener.class;
        }
        if ("Bluetooth".equals(simpleName)) {
            return com.codename1.bluetooth.Bluetooth.class;
        }
        if ("BluetoothDevice".equals(simpleName)) {
            return com.codename1.bluetooth.BluetoothDevice.class;
        }
        if ("BluetoothError".equals(simpleName)) {
            return com.codename1.bluetooth.BluetoothError.class;
        }
        if ("BluetoothException".equals(simpleName)) {
            return com.codename1.bluetooth.BluetoothException.class;
        }
        if ("BluetoothPermission".equals(simpleName)) {
            return com.codename1.bluetooth.BluetoothPermission.class;
        }
        if ("BluetoothUuid".equals(simpleName)) {
            return com.codename1.bluetooth.BluetoothUuid.class;
        }
        if ("BondState".equals(simpleName)) {
            return com.codename1.bluetooth.BondState.class;
        }
        if ("DeviceType".equals(simpleName)) {
            return com.codename1.bluetooth.DeviceType.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.bluetooth.BluetoothException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class}, false);
                return new com.codename1.bluetooth.BluetoothException((com.codename1.bluetooth.BluetoothError) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class, java.lang.String.class}, false);
                return new com.codename1.bluetooth.BluetoothException((com.codename1.bluetooth.BluetoothError) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.bluetooth.BluetoothException((com.codename1.bluetooth.BluetoothError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothError.class, java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.bluetooth.BluetoothException((com.codename1.bluetooth.BluetoothError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
        }
        if (type == com.codename1.bluetooth.BluetoothUuid.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return new com.codename1.bluetooth.BluetoothUuid(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.bluetooth.Bluetooth.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.bluetooth.BluetoothUuid.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.bluetooth.Bluetooth.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.bluetooth.Bluetooth.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.bluetooth.BluetoothUuid.fromShort(toIntValue(adaptedArgs[0]));
            }
        }
        if ("fromString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.bluetooth.BluetoothUuid.fromString((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.bluetooth.BluetoothUuid.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.bluetooth.Bluetooth) {
            try {
                return invoke0((com.codename1.bluetooth.Bluetooth) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.BluetoothDevice) {
            try {
                return invoke1((com.codename1.bluetooth.BluetoothDevice) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.BluetoothException) {
            try {
                return invoke2((com.codename1.bluetooth.BluetoothException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.BluetoothUuid) {
            try {
                return invoke3((com.codename1.bluetooth.BluetoothUuid) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.AdapterStateListener) {
            try {
                return invoke4((com.codename1.bluetooth.AdapterStateListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.bluetooth.Bluetooth typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAdapterStateListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.AdapterStateListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.AdapterStateListener.class}, false);
                typedTarget.addAdapterStateListener((com.codename1.bluetooth.AdapterStateListener) adaptedArgs[0]); return null;
            }
        }
        if ("getAdapterState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdapterState();
            }
        }
        if ("getClassic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClassic();
            }
        }
        if ("getLE".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLE();
            }
        }
        if ("hasPermission".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothPermission.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothPermission.class}, false);
                return typedTarget.hasPermission((com.codename1.bluetooth.BluetoothPermission) adaptedArgs[0]);
            }
        }
        if ("isClassicSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isClassicSupported();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isL2capSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isL2capSupported();
            }
        }
        if ("isLeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLeSupported();
            }
        }
        if ("isPeripheralModeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPeripheralModeSupported();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("removeAdapterStateListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.AdapterStateListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.AdapterStateListener.class}, false);
                typedTarget.removeAdapterStateListener((com.codename1.bluetooth.AdapterStateListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestEnable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.requestEnable();
            }
        }
        if ("requestPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothPermission[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothPermission[].class}, true);
                com.codename1.bluetooth.BluetoothPermission[] varArgs = new com.codename1.bluetooth.BluetoothPermission[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.bluetooth.BluetoothPermission) adaptedArgs[i];
                }
                return typedTarget.requestPermissions(varArgs);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.bluetooth.BluetoothDevice typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAddress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAddress();
            }
        }
        if ("getBondState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBondState();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
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

    private static Object invoke2(com.codename1.bluetooth.BluetoothException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getGattStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGattStatus();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.bluetooth.BluetoothUuid typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getLeastSignificantBits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeastSignificantBits();
            }
        }
        if ("getMostSignificantBits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMostSignificantBits();
            }
        }
        if ("getShortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShortValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isShortUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShortUuid();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.bluetooth.AdapterStateListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("adapterStateChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.AdapterState.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.AdapterState.class}, false);
                typedTarget.adapterStateChanged((com.codename1.bluetooth.AdapterState) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.bluetooth.AdapterState.class) return getStaticField0(name);
        if (type == com.codename1.bluetooth.BluetoothError.class) return getStaticField1(name);
        if (type == com.codename1.bluetooth.BluetoothPermission.class) return getStaticField2(name);
        if (type == com.codename1.bluetooth.BluetoothUuid.class) return getStaticField3(name);
        if (type == com.codename1.bluetooth.BondState.class) return getStaticField4(name);
        if (type == com.codename1.bluetooth.DeviceType.class) return getStaticField5(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("POWERED_OFF".equals(name)) return com.codename1.bluetooth.AdapterState.POWERED_OFF;
        if ("POWERED_ON".equals(name)) return com.codename1.bluetooth.AdapterState.POWERED_ON;
        if ("TURNING_OFF".equals(name)) return com.codename1.bluetooth.AdapterState.TURNING_OFF;
        if ("TURNING_ON".equals(name)) return com.codename1.bluetooth.AdapterState.TURNING_ON;
        if ("UNAUTHORIZED".equals(name)) return com.codename1.bluetooth.AdapterState.UNAUTHORIZED;
        if ("UNKNOWN".equals(name)) return com.codename1.bluetooth.AdapterState.UNKNOWN;
        if ("UNSUPPORTED".equals(name)) return com.codename1.bluetooth.AdapterState.UNSUPPORTED;
        throw unsupportedStaticField(com.codename1.bluetooth.AdapterState.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ADVERTISE_FAILED".equals(name)) return com.codename1.bluetooth.BluetoothError.ADVERTISE_FAILED;
        if ("BOND_FAILED".equals(name)) return com.codename1.bluetooth.BluetoothError.BOND_FAILED;
        if ("BUSY".equals(name)) return com.codename1.bluetooth.BluetoothError.BUSY;
        if ("CONNECTION_FAILED".equals(name)) return com.codename1.bluetooth.BluetoothError.CONNECTION_FAILED;
        if ("CONNECTION_LOST".equals(name)) return com.codename1.bluetooth.BluetoothError.CONNECTION_LOST;
        if ("GATT_ERROR".equals(name)) return com.codename1.bluetooth.BluetoothError.GATT_ERROR;
        if ("IO_ERROR".equals(name)) return com.codename1.bluetooth.BluetoothError.IO_ERROR;
        if ("NOT_CONNECTED".equals(name)) return com.codename1.bluetooth.BluetoothError.NOT_CONNECTED;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.bluetooth.BluetoothError.NOT_SUPPORTED;
        if ("POWERED_OFF".equals(name)) return com.codename1.bluetooth.BluetoothError.POWERED_OFF;
        if ("SCAN_FAILED".equals(name)) return com.codename1.bluetooth.BluetoothError.SCAN_FAILED;
        if ("TIMEOUT".equals(name)) return com.codename1.bluetooth.BluetoothError.TIMEOUT;
        if ("UNAUTHORIZED".equals(name)) return com.codename1.bluetooth.BluetoothError.UNAUTHORIZED;
        if ("UNKNOWN".equals(name)) return com.codename1.bluetooth.BluetoothError.UNKNOWN;
        if ("USER_CANCELED".equals(name)) return com.codename1.bluetooth.BluetoothError.USER_CANCELED;
        throw unsupportedStaticField(com.codename1.bluetooth.BluetoothError.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("ADVERTISE".equals(name)) return com.codename1.bluetooth.BluetoothPermission.ADVERTISE;
        if ("CONNECT".equals(name)) return com.codename1.bluetooth.BluetoothPermission.CONNECT;
        if ("SCAN".equals(name)) return com.codename1.bluetooth.BluetoothPermission.SCAN;
        throw unsupportedStaticField(com.codename1.bluetooth.BluetoothPermission.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("BASE".equals(name)) return com.codename1.bluetooth.BluetoothUuid.BASE;
        if ("CCCD".equals(name)) return com.codename1.bluetooth.BluetoothUuid.CCCD;
        if ("SPP".equals(name)) return com.codename1.bluetooth.BluetoothUuid.SPP;
        throw unsupportedStaticField(com.codename1.bluetooth.BluetoothUuid.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("BONDED".equals(name)) return com.codename1.bluetooth.BondState.BONDED;
        if ("BONDING".equals(name)) return com.codename1.bluetooth.BondState.BONDING;
        if ("NONE".equals(name)) return com.codename1.bluetooth.BondState.NONE;
        throw unsupportedStaticField(com.codename1.bluetooth.BondState.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("CLASSIC".equals(name)) return com.codename1.bluetooth.DeviceType.CLASSIC;
        if ("DUAL".equals(name)) return com.codename1.bluetooth.DeviceType.DUAL;
        if ("LE".equals(name)) return com.codename1.bluetooth.DeviceType.LE;
        if ("UNKNOWN".equals(name)) return com.codename1.bluetooth.DeviceType.UNKNOWN;
        throw unsupportedStaticField(com.codename1.bluetooth.DeviceType.class, name);
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
