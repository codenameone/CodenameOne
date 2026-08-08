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

public final class GeneratedAccess_com_codename1_bluetooth_le {
    private GeneratedAccess_com_codename1_bluetooth_le() {
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
        if ("AdvertisementData".equals(simpleName)) {
            return com.codename1.bluetooth.le.AdvertisementData.class;
        }
        if ("BlePeripheral".equals(simpleName)) {
            return com.codename1.bluetooth.le.BlePeripheral.class;
        }
        if ("BleScan".equals(simpleName)) {
            return com.codename1.bluetooth.le.BleScan.class;
        }
        if ("BluetoothLE".equals(simpleName)) {
            return com.codename1.bluetooth.le.BluetoothLE.class;
        }
        if ("ConnectionEvent".equals(simpleName)) {
            return com.codename1.bluetooth.le.ConnectionEvent.class;
        }
        if ("ConnectionListener".equals(simpleName)) {
            return com.codename1.bluetooth.le.ConnectionListener.class;
        }
        if ("ConnectionOptions".equals(simpleName)) {
            return com.codename1.bluetooth.le.ConnectionOptions.class;
        }
        if ("ConnectionPriority".equals(simpleName)) {
            return com.codename1.bluetooth.le.ConnectionPriority.class;
        }
        if ("ConnectionState".equals(simpleName)) {
            return com.codename1.bluetooth.le.ConnectionState.class;
        }
        if ("L2capChannel".equals(simpleName)) {
            return com.codename1.bluetooth.le.L2capChannel.class;
        }
        if ("L2capServer".equals(simpleName)) {
            return com.codename1.bluetooth.le.L2capServer.class;
        }
        if ("ScanFilter".equals(simpleName)) {
            return com.codename1.bluetooth.le.ScanFilter.class;
        }
        if ("ScanListener".equals(simpleName)) {
            return com.codename1.bluetooth.le.ScanListener.class;
        }
        if ("ScanMode".equals(simpleName)) {
            return com.codename1.bluetooth.le.ScanMode.class;
        }
        if ("ScanResult".equals(simpleName)) {
            return com.codename1.bluetooth.le.ScanResult.class;
        }
        if ("ScanSettings".equals(simpleName)) {
            return com.codename1.bluetooth.le.ScanSettings.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.bluetooth.le.AdvertisementData.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.AdvertisementData();
            }
        }
        if (type == com.codename1.bluetooth.le.ConnectionOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.ConnectionOptions();
            }
        }
        if (type == com.codename1.bluetooth.le.ScanFilter.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.ScanFilter();
            }
        }
        if (type == com.codename1.bluetooth.le.ScanResult.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.BlePeripheral.class, java.lang.Integer.class, com.codename1.bluetooth.le.AdvertisementData.class, java.lang.Boolean.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.BlePeripheral.class, java.lang.Integer.class, com.codename1.bluetooth.le.AdvertisementData.class, java.lang.Boolean.class, java.lang.Long.class}, false);
                return new com.codename1.bluetooth.le.ScanResult((com.codename1.bluetooth.le.BlePeripheral) adaptedArgs[0], toIntValue(adaptedArgs[1]), (com.codename1.bluetooth.le.AdvertisementData) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), ((Number) adaptedArgs[4]).longValue());
            }
        }
        if (type == com.codename1.bluetooth.le.ScanSettings.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.bluetooth.le.ScanSettings();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.bluetooth.le.AdvertisementData.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.bluetooth.le.AdvertisementData.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.bluetooth.le.AdvertisementData.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.bluetooth.le.AdvertisementData) {
            try {
                return invoke0((com.codename1.bluetooth.le.AdvertisementData) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.BlePeripheral) {
            try {
                return invoke1((com.codename1.bluetooth.le.BlePeripheral) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.BleScan) {
            try {
                return invoke2((com.codename1.bluetooth.le.BleScan) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.BluetoothLE) {
            try {
                return invoke3((com.codename1.bluetooth.le.BluetoothLE) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ConnectionEvent) {
            try {
                return invoke4((com.codename1.bluetooth.le.ConnectionEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ConnectionOptions) {
            try {
                return invoke5((com.codename1.bluetooth.le.ConnectionOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.L2capChannel) {
            try {
                return invoke6((com.codename1.bluetooth.le.L2capChannel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.L2capServer) {
            try {
                return invoke7((com.codename1.bluetooth.le.L2capServer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ScanFilter) {
            try {
                return invoke8((com.codename1.bluetooth.le.ScanFilter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ScanResult) {
            try {
                return invoke9((com.codename1.bluetooth.le.ScanResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ScanSettings) {
            try {
                return invoke10((com.codename1.bluetooth.le.ScanSettings) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ConnectionListener) {
            try {
                return invoke11((com.codename1.bluetooth.le.ConnectionListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.bluetooth.le.ScanListener) {
            try {
                return invoke12((com.codename1.bluetooth.le.ScanListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.bluetooth.le.AdvertisementData typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addManufacturerData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                typedTarget.addManufacturerData(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]); return null;
            }
        }
        if ("addServiceData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, byte[].class}, false);
                typedTarget.addServiceData((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
        }
        if ("addServiceUuid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                typedTarget.addServiceUuid((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]); return null;
            }
        }
        if ("getLocalName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalName();
            }
        }
        if ("getManufacturerData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getManufacturerData(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getManufacturerIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManufacturerIds();
            }
        }
        if ("getRawBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawBytes();
            }
        }
        if ("getServiceData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                return typedTarget.getServiceData((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]);
            }
        }
        if ("getServiceDataUuids".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceDataUuids();
            }
        }
        if ("getServiceUuids".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceUuids();
            }
        }
        if ("getTxPowerLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTxPowerLevel();
            }
        }
        if ("setLocalName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLocalName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTxPowerLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTxPowerLevel(Integer.valueOf(toIntValue(adaptedArgs[0]))); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.bluetooth.le.BlePeripheral typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addConnectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionListener.class}, false);
                typedTarget.addConnectionListener((com.codename1.bluetooth.le.ConnectionListener) adaptedArgs[0]); return null;
            }
        }
        if ("connect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.connect();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionOptions.class}, false);
                return typedTarget.connect((com.codename1.bluetooth.le.ConnectionOptions) adaptedArgs[0]);
            }
        }
        if ("createBond".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createBond();
            }
        }
        if ("disconnect".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.disconnect(); return null;
            }
        }
        if ("discoverServices".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.discoverServices();
            }
        }
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
        if ("getCharacteristic".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class, com.codename1.bluetooth.BluetoothUuid.class}, false);
                return typedTarget.getCharacteristic((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0], (com.codename1.bluetooth.BluetoothUuid) adaptedArgs[1]);
            }
        }
        if ("getConnectionState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnectionState();
            }
        }
        if ("getMtu".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMtu();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                return typedTarget.getService((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]);
            }
        }
        if ("getServices".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServices();
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
        if ("isSubscribed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class}, false);
                return typedTarget.isSubscribed((com.codename1.bluetooth.gatt.GattCharacteristic) adaptedArgs[0]);
            }
        }
        if ("openL2capChannel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.openL2capChannel(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("readCharacteristic".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class}, false);
                return typedTarget.readCharacteristic((com.codename1.bluetooth.gatt.GattCharacteristic) adaptedArgs[0]);
            }
        }
        if ("readDescriptor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattDescriptor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattDescriptor.class}, false);
                return typedTarget.readDescriptor((com.codename1.bluetooth.gatt.GattDescriptor) adaptedArgs[0]);
            }
        }
        if ("readRssi".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readRssi();
            }
        }
        if ("removeConnectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionListener.class}, false);
                typedTarget.removeConnectionListener((com.codename1.bluetooth.le.ConnectionListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestConnectionPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionPriority.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionPriority.class}, false);
                return typedTarget.requestConnectionPriority((com.codename1.bluetooth.le.ConnectionPriority) adaptedArgs[0]);
            }
        }
        if ("requestMtu".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.requestMtu(toIntValue(adaptedArgs[0]));
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class, com.codename1.bluetooth.gatt.GattNotificationListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class, com.codename1.bluetooth.gatt.GattNotificationListener.class}, false);
                return typedTarget.subscribe((com.codename1.bluetooth.gatt.GattCharacteristic) adaptedArgs[0], (com.codename1.bluetooth.gatt.GattNotificationListener) adaptedArgs[1]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unsubscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class, com.codename1.bluetooth.gatt.GattNotificationListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class, com.codename1.bluetooth.gatt.GattNotificationListener.class}, false);
                return typedTarget.unsubscribe((com.codename1.bluetooth.gatt.GattCharacteristic) adaptedArgs[0], (com.codename1.bluetooth.gatt.GattNotificationListener) adaptedArgs[1]);
            }
        }
        if ("writeCharacteristic".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class, byte[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattCharacteristic.class, byte[].class, java.lang.Boolean.class}, false);
                return typedTarget.writeCharacteristic((com.codename1.bluetooth.gatt.GattCharacteristic) adaptedArgs[0], (byte[]) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if ("writeDescriptor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattDescriptor.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.gatt.GattDescriptor.class, byte[].class}, false);
                return typedTarget.writeDescriptor((com.codename1.bluetooth.gatt.GattDescriptor) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.bluetooth.le.BleScan typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false);
                typedTarget.addListener((com.codename1.util.AsyncResource) adaptedArgs[0]); return null;
            }
        }
        if ("addObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.addObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("asPromise".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asPromise();
            }
        }
        if ("await".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.await(); return null;
            }
        }
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.cancel(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("countObservers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.countObservers();
            }
        }
        if ("deleteObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.deleteObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("deleteObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.deleteObservers(); return null;
            }
        }
        if ("error".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.error((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("except".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.except((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false);
                return typedTarget.except((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.EasyThread) adaptedArgs[1]);
            }
        }
        if ("get".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.get();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hasChanged".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasChanged();
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("isCancelled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCancelled();
            }
        }
        if ("isDone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDone();
            }
        }
        if ("isReady".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReady();
            }
        }
        if ("notifyObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.notifyObservers(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.notifyObservers((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false);
                typedTarget.onResult((com.codename1.util.AsyncResult) adaptedArgs[0]); return null;
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.ready((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false);
                return typedTarget.ready((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.EasyThread) adaptedArgs[1]);
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        if ("waitFor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.waitFor(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.bluetooth.le.BluetoothLE typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBondedPeripherals".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBondedPeripherals();
            }
        }
        if ("getConnectedPeripherals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                return typedTarget.getConnectedPeripherals((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]);
            }
        }
        if ("getPeripheral".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPeripheral((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("openGattServer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattServerListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.GattServerListener.class}, false);
                return typedTarget.openGattServer((com.codename1.bluetooth.le.server.GattServerListener) adaptedArgs[0]);
            }
        }
        if ("openL2capServer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.openL2capServer(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("startAdvertising".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.AdvertiseSettings.class, com.codename1.bluetooth.le.server.AdvertiseData.class, com.codename1.bluetooth.le.server.AdvertiseData.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.server.AdvertiseSettings.class, com.codename1.bluetooth.le.server.AdvertiseData.class, com.codename1.bluetooth.le.server.AdvertiseData.class}, false);
                return typedTarget.startAdvertising((com.codename1.bluetooth.le.server.AdvertiseSettings) adaptedArgs[0], (com.codename1.bluetooth.le.server.AdvertiseData) adaptedArgs[1], (com.codename1.bluetooth.le.server.AdvertiseData) adaptedArgs[2]);
            }
        }
        if ("startScan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanSettings.class, com.codename1.bluetooth.le.ScanListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanSettings.class, com.codename1.bluetooth.le.ScanListener.class}, false);
                return typedTarget.startScan((com.codename1.bluetooth.le.ScanSettings) adaptedArgs[0], (com.codename1.bluetooth.le.ScanListener) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.bluetooth.le.ConnectionEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPeripheral".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPeripheral();
            }
        }
        if ("getReason".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReason();
            }
        }
        if ("getState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getState();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.bluetooth.le.ConnectionOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("isAutoConnect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoConnect();
            }
        }
        if ("setAutoConnect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setAutoConnect(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setTimeout(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.bluetooth.le.L2capChannel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getPsm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPsm();
            }
        }
        if ("isOpen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpen();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.bluetooth.le.L2capServer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accept".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.accept();
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getPsm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPsm();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.bluetooth.le.ScanFilter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAddress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAddress();
            }
        }
        if ("getManufacturerData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManufacturerData();
            }
        }
        if ("getManufacturerDataMask".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManufacturerDataMask();
            }
        }
        if ("getManufacturerId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManufacturerId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNamePrefix".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNamePrefix();
            }
        }
        if ("getServiceUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceUuid();
            }
        }
        if ("matches".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanResult.class}, false);
                return typedTarget.matches((com.codename1.bluetooth.le.ScanResult) adaptedArgs[0]);
            }
        }
        if ("setAddress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAddress((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setManufacturerData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class, byte[].class}, false);
                return typedTarget.setManufacturerData(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1], (byte[]) adaptedArgs[2]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setNamePrefix".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setNamePrefix((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setServiceUuid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.BluetoothUuid.class}, false);
                return typedTarget.setServiceUuid((com.codename1.bluetooth.BluetoothUuid) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.bluetooth.le.ScanResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAdvertisementData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdvertisementData();
            }
        }
        if ("getPeripheral".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPeripheral();
            }
        }
        if ("getRssi".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRssi();
            }
        }
        if ("getTimestamp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestamp();
            }
        }
        if ("isConnectable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConnectable();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.bluetooth.le.ScanSettings typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addFilter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanFilter.class}, false);
                return typedTarget.addFilter((com.codename1.bluetooth.le.ScanFilter) adaptedArgs[0]);
            }
        }
        if ("getFilters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFilters();
            }
        }
        if ("getScanMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScanMode();
            }
        }
        if ("isAllowDuplicates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowDuplicates();
            }
        }
        if ("matches".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanResult.class}, false);
                return typedTarget.matches((com.codename1.bluetooth.le.ScanResult) adaptedArgs[0]);
            }
        }
        if ("setAllowDuplicates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setAllowDuplicates(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setScanMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanMode.class}, false);
                return typedTarget.setScanMode((com.codename1.bluetooth.le.ScanMode) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.bluetooth.le.ConnectionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connectionStateChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ConnectionEvent.class}, false);
                typedTarget.connectionStateChanged((com.codename1.bluetooth.le.ConnectionEvent) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.bluetooth.le.ScanListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("peripheralDiscovered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.bluetooth.le.ScanResult.class}, false);
                typedTarget.peripheralDiscovered((com.codename1.bluetooth.le.ScanResult) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.bluetooth.le.ConnectionPriority.class) return getStaticField0(name);
        if (type == com.codename1.bluetooth.le.ConnectionState.class) return getStaticField1(name);
        if (type == com.codename1.bluetooth.le.ScanMode.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BALANCED".equals(name)) return com.codename1.bluetooth.le.ConnectionPriority.BALANCED;
        if ("HIGH".equals(name)) return com.codename1.bluetooth.le.ConnectionPriority.HIGH;
        if ("LOW_POWER".equals(name)) return com.codename1.bluetooth.le.ConnectionPriority.LOW_POWER;
        throw unsupportedStaticField(com.codename1.bluetooth.le.ConnectionPriority.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CONNECTED".equals(name)) return com.codename1.bluetooth.le.ConnectionState.CONNECTED;
        if ("CONNECTING".equals(name)) return com.codename1.bluetooth.le.ConnectionState.CONNECTING;
        if ("DISCONNECTED".equals(name)) return com.codename1.bluetooth.le.ConnectionState.DISCONNECTED;
        if ("DISCONNECTING".equals(name)) return com.codename1.bluetooth.le.ConnectionState.DISCONNECTING;
        throw unsupportedStaticField(com.codename1.bluetooth.le.ConnectionState.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("BALANCED".equals(name)) return com.codename1.bluetooth.le.ScanMode.BALANCED;
        if ("LOW_LATENCY".equals(name)) return com.codename1.bluetooth.le.ScanMode.LOW_LATENCY;
        if ("LOW_POWER".equals(name)) return com.codename1.bluetooth.le.ScanMode.LOW_POWER;
        if ("OPPORTUNISTIC".equals(name)) return com.codename1.bluetooth.le.ScanMode.OPPORTUNISTIC;
        throw unsupportedStaticField(com.codename1.bluetooth.le.ScanMode.class, name);
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
