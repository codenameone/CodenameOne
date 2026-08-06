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

public final class GeneratedAccess_com_codename1_bluetooth_le_server {
    private GeneratedAccess_com_codename1_bluetooth_le_server() {
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
        if ("AdvertiseData".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.AdvertiseData.class;
        }
        if ("AdvertiseMode".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.AdvertiseMode.class;
        }
        if ("AdvertiseSettings".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.AdvertiseSettings.class;
        }
        if ("BleAdvertisement".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.BleAdvertisement.class;
        }
        if ("BleCentral".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.BleCentral.class;
        }
        if ("GattLocalCharacteristic".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattLocalCharacteristic.class;
        }
        if ("GattLocalDescriptor".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattLocalDescriptor.class;
        }
        if ("GattLocalService".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattLocalService.class;
        }
        if ("GattReadRequest".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattReadRequest.class;
        }
        if ("GattServer".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattServer.class;
        }
        if ("GattServerAdapter".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattServerAdapter.class;
        }
        if ("GattServerListener".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattServerListener.class;
        }
        if ("GattWriteRequest".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.GattWriteRequest.class;
        }
        if ("TxPowerLevel".equals(simpleName)) {
            return com.codename1.bluetooth.le.server.TxPowerLevel.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.bluetooth.le.server.AdvertiseData.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.server.AdvertiseData();
            }
        }
        if (type == com.codename1.bluetooth.le.server.AdvertiseSettings.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.server.AdvertiseSettings();
            }
        }
        if (type == com.codename1.bluetooth.le.server.GattLocalCharacteristic.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.bluetooth.le.server.GattLocalCharacteristic((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if (type == com.codename1.bluetooth.le.server.GattLocalDescriptor.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, java.lang.Integer.class}, false);
                return new com.codename1.bluetooth.le.server.GattLocalDescriptor((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.bluetooth.le.server.GattLocalService.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                return new com.codename1.bluetooth.le.server.GattLocalService((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, java.lang.Boolean.class}, false);
                return new com.codename1.bluetooth.le.server.GattLocalService((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.bluetooth.le.server.GattServerAdapter.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.server.GattServerAdapter();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedStatic(type, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.bluetooth.le.server.AdvertiseData) {
            try {
                return invoke0((com.codename1.bluetooth.le.server.AdvertiseData) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.AdvertiseSettings) {
            try {
                return invoke1((com.codename1.bluetooth.le.server.AdvertiseSettings) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.BleAdvertisement) {
            try {
                return invoke2((com.codename1.bluetooth.le.server.BleAdvertisement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.BleCentral) {
            try {
                return invoke3((com.codename1.bluetooth.le.server.BleCentral) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattLocalCharacteristic) {
            try {
                return invoke4((com.codename1.bluetooth.le.server.GattLocalCharacteristic) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattLocalDescriptor) {
            try {
                return invoke5((com.codename1.bluetooth.le.server.GattLocalDescriptor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattLocalService) {
            try {
                return invoke6((com.codename1.bluetooth.le.server.GattLocalService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattReadRequest) {
            try {
                return invoke7((com.codename1.bluetooth.le.server.GattReadRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattServer) {
            try {
                return invoke8((com.codename1.bluetooth.le.server.GattServer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattServerAdapter) {
            try {
                return invoke9((com.codename1.bluetooth.le.server.GattServerAdapter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattWriteRequest) {
            try {
                return invoke10((com.codename1.bluetooth.le.server.GattWriteRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.server.GattServerListener) {
            try {
                return invoke11((com.codename1.bluetooth.le.server.GattServerListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.bluetooth.le.server.AdvertiseData typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addManufacturerData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                return typedTarget.addManufacturerData(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]);
            }
        }
        if ("addServiceData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, byte[].class}, false);
                return typedTarget.addServiceData((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("addServiceUuid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                return typedTarget.addServiceUuid((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]);
            }
        }
        if ("getManufacturerData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManufacturerData();
            }
        }
        if ("getServiceData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceData();
            }
        }
        if ("getServiceUuids".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceUuids();
            }
        }
        if ("isIncludeDeviceName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIncludeDeviceName();
            }
        }
        if ("isIncludeTxPower".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIncludeTxPower();
            }
        }
        if ("setIncludeDeviceName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setIncludeDeviceName(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setIncludeTxPower".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setIncludeTxPower(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.bluetooth.le.server.AdvertiseSettings typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMode();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getTxPower".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTxPower();
            }
        }
        if ("isConnectable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConnectable();
            }
        }
        if ("setConnectable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setConnectable(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.AdvertiseMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.AdvertiseMode.class}, false);
                return typedTarget.setMode((com.codename1.bluetooth.le.server.AdvertiseMode) adaptedArgs[0]);
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setTimeout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setTxPower".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.TxPowerLevel.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.TxPowerLevel.class}, false);
                return typedTarget.setTxPower((com.codename1.bluetooth.le.server.TxPowerLevel) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.bluetooth.le.server.BleAdvertisement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.bluetooth.le.server.BleCentral typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAddress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAddress();
            }
        }
        if ("getMtu".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMtu();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.bluetooth.le.server.GattLocalCharacteristic typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addDescriptor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalDescriptor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalDescriptor.class}, false);
                return typedTarget.addDescriptor((com.codename1.bluetooth.le.server.GattLocalDescriptor) adaptedArgs[0]);
            }
        }
        if ("getDescriptors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescriptors();
            }
        }
        if ("getPermissions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPermissions();
            }
        }
        if ("getProperties".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProperties();
            }
        }
        if ("getUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUuid();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.setValue((byte[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.bluetooth.le.server.GattLocalDescriptor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPermissions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPermissions();
            }
        }
        if ("getUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUuid();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.setValue((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.bluetooth.le.server.GattLocalService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addCharacteristic".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalCharacteristic.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalCharacteristic.class}, false);
                return typedTarget.addCharacteristic((com.codename1.bluetooth.le.server.GattLocalCharacteristic) adaptedArgs[0]);
            }
        }
        if ("getCharacteristics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCharacteristics();
            }
        }
        if ("getUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUuid();
            }
        }
        if ("isPrimary".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrimary();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.bluetooth.le.server.GattReadRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCentral".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCentral();
            }
        }
        if ("getCharacteristic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCharacteristic();
            }
        }
        if ("getDescriptor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescriptor();
            }
        }
        if ("getOffset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOffset();
            }
        }
        if ("reject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattStatus.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattStatus.class}, false);
                typedTarget.reject((com.codename1.bluetooth.gatt.GattStatus) adaptedArgs[0]); return null;
            }
        }
        if ("respond".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.respond((byte[]) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.bluetooth.le.server.GattServer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalService.class}, false);
                return typedTarget.addService((com.codename1.bluetooth.le.server.GattLocalService) adaptedArgs[0]);
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getConnectedCentrals".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnectedCentrals();
            }
        }
        if ("notifyCentral".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class, com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, byte[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class, com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, byte[].class, java.lang.Boolean.class}, false);
                return typedTarget.notifyCentral((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0], (com.codename1.bluetooth.le.server.GattLocalCharacteristic) adaptedArgs[1], (byte[]) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("notifyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, byte[].class}, false);
                return typedTarget.notifyValue((com.codename1.bluetooth.le.server.GattLocalCharacteristic) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("removeService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattLocalService.class}, false);
                typedTarget.removeService((com.codename1.bluetooth.le.server.GattLocalService) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.bluetooth.le.server.GattServerAdapter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("centralConnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false);
                typedTarget.centralConnected((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0]); return null;
            }
        }
        if ("centralDisconnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false);
                typedTarget.centralDisconnected((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0]); return null;
            }
        }
        if ("characteristicReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false);
                typedTarget.characteristicReadRequest((com.codename1.bluetooth.le.server.GattReadRequest) adaptedArgs[0]); return null;
            }
        }
        if ("characteristicWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false);
                typedTarget.characteristicWriteRequest((com.codename1.bluetooth.le.server.GattWriteRequest) adaptedArgs[0]); return null;
            }
        }
        if ("descriptorReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false);
                typedTarget.descriptorReadRequest((com.codename1.bluetooth.le.server.GattReadRequest) adaptedArgs[0]); return null;
            }
        }
        if ("descriptorWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false);
                typedTarget.descriptorWriteRequest((com.codename1.bluetooth.le.server.GattWriteRequest) adaptedArgs[0]); return null;
            }
        }
        if ("subscriptionChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class, com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class, com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, java.lang.Boolean.class}, false);
                typedTarget.subscriptionChanged((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0], (com.codename1.bluetooth.le.server.GattLocalCharacteristic) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.bluetooth.le.server.GattWriteRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCentral".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCentral();
            }
        }
        if ("getCharacteristic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCharacteristic();
            }
        }
        if ("getDescriptor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescriptor();
            }
        }
        if ("getOffset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOffset();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("isResponseRequired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isResponseRequired();
            }
        }
        if ("reject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattStatus.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattStatus.class}, false);
                typedTarget.reject((com.codename1.bluetooth.gatt.GattStatus) adaptedArgs[0]); return null;
            }
        }
        if ("respond".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.respond(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.bluetooth.le.server.GattServerListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("centralConnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false);
                typedTarget.centralConnected((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0]); return null;
            }
        }
        if ("centralDisconnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class}, false);
                typedTarget.centralDisconnected((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0]); return null;
            }
        }
        if ("characteristicReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false);
                typedTarget.characteristicReadRequest((com.codename1.bluetooth.le.server.GattReadRequest) adaptedArgs[0]); return null;
            }
        }
        if ("characteristicWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false);
                typedTarget.characteristicWriteRequest((com.codename1.bluetooth.le.server.GattWriteRequest) adaptedArgs[0]); return null;
            }
        }
        if ("descriptorReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattReadRequest.class}, false);
                typedTarget.descriptorReadRequest((com.codename1.bluetooth.le.server.GattReadRequest) adaptedArgs[0]); return null;
            }
        }
        if ("descriptorWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattWriteRequest.class}, false);
                typedTarget.descriptorWriteRequest((com.codename1.bluetooth.le.server.GattWriteRequest) adaptedArgs[0]); return null;
            }
        }
        if ("subscriptionChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class, com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.BleCentral.class, com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, java.lang.Boolean.class}, false);
                typedTarget.subscriptionChanged((com.codename1.bluetooth.le.server.BleCentral) adaptedArgs[0], (com.codename1.bluetooth.le.server.GattLocalCharacteristic) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.bluetooth.le.server.AdvertiseMode.class) return getStaticField0(name);
        if (type == com.codename1.bluetooth.le.server.GattLocalCharacteristic.class) return getStaticField1(name);
        if (type == com.codename1.bluetooth.le.server.TxPowerLevel.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BALANCED".equals(name)) return com.codename1.bluetooth.le.server.AdvertiseMode.BALANCED;
        if ("LOW_LATENCY".equals(name)) return com.codename1.bluetooth.le.server.AdvertiseMode.LOW_LATENCY;
        if ("LOW_POWER".equals(name)) return com.codename1.bluetooth.le.server.AdvertiseMode.LOW_POWER;
        throw unsupportedStaticField(com.codename1.bluetooth.le.server.AdvertiseMode.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("PERMISSION_READ".equals(name)) return com.codename1.bluetooth.le.server.GattLocalCharacteristic.PERMISSION_READ;
        if ("PERMISSION_READ_ENCRYPTED".equals(name)) return com.codename1.bluetooth.le.server.GattLocalCharacteristic.PERMISSION_READ_ENCRYPTED;
        if ("PERMISSION_WRITE".equals(name)) return com.codename1.bluetooth.le.server.GattLocalCharacteristic.PERMISSION_WRITE;
        if ("PERMISSION_WRITE_ENCRYPTED".equals(name)) return com.codename1.bluetooth.le.server.GattLocalCharacteristic.PERMISSION_WRITE_ENCRYPTED;
        throw unsupportedStaticField(com.codename1.bluetooth.le.server.GattLocalCharacteristic.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("HIGH".equals(name)) return com.codename1.bluetooth.le.server.TxPowerLevel.HIGH;
        if ("LOW".equals(name)) return com.codename1.bluetooth.le.server.TxPowerLevel.LOW;
        if ("MEDIUM".equals(name)) return com.codename1.bluetooth.le.server.TxPowerLevel.MEDIUM;
        if ("ULTRA_LOW".equals(name)) return com.codename1.bluetooth.le.server.TxPowerLevel.ULTRA_LOW;
        throw unsupportedStaticField(com.codename1.bluetooth.le.server.TxPowerLevel.class, name);
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
