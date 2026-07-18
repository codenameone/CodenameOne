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

public final class GeneratedAccess_com_codename1_nfc {
    private GeneratedAccess_com_codename1_nfc() {
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
        if ("ApduResponse".equals(simpleName)) {
            return com.codename1.nfc.ApduResponse.class;
        }
        if ("HostCardEmulationService".equals(simpleName)) {
            return com.codename1.nfc.HostCardEmulationService.class;
        }
        if ("IsoDep".equals(simpleName)) {
            return com.codename1.nfc.IsoDep.class;
        }
        if ("MifareClassic".equals(simpleName)) {
            return com.codename1.nfc.MifareClassic.class;
        }
        if ("MifareUltralight".equals(simpleName)) {
            return com.codename1.nfc.MifareUltralight.class;
        }
        if ("NdefMessage".equals(simpleName)) {
            return com.codename1.nfc.NdefMessage.class;
        }
        if ("NdefRecord".equals(simpleName)) {
            return com.codename1.nfc.NdefRecord.class;
        }
        if ("Nfc".equals(simpleName)) {
            return com.codename1.nfc.Nfc.class;
        }
        if ("NfcA".equals(simpleName)) {
            return com.codename1.nfc.NfcA.class;
        }
        if ("NfcB".equals(simpleName)) {
            return com.codename1.nfc.NfcB.class;
        }
        if ("NfcError".equals(simpleName)) {
            return com.codename1.nfc.NfcError.class;
        }
        if ("NfcException".equals(simpleName)) {
            return com.codename1.nfc.NfcException.class;
        }
        if ("NfcF".equals(simpleName)) {
            return com.codename1.nfc.NfcF.class;
        }
        if ("NfcListener".equals(simpleName)) {
            return com.codename1.nfc.NfcListener.class;
        }
        if ("NfcReadOptions".equals(simpleName)) {
            return com.codename1.nfc.NfcReadOptions.class;
        }
        if ("NfcV".equals(simpleName)) {
            return com.codename1.nfc.NfcV.class;
        }
        if ("Tag".equals(simpleName)) {
            return com.codename1.nfc.Tag.class;
        }
        if ("TagTechnology".equals(simpleName)) {
            return com.codename1.nfc.TagTechnology.class;
        }
        if ("TagType".equals(simpleName)) {
            return com.codename1.nfc.TagType.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.nfc.IsoDep.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.IsoDep();
            }
        }
        if (type == com.codename1.nfc.MifareClassic.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.MifareClassic();
            }
        }
        if (type == com.codename1.nfc.MifareUltralight.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.MifareUltralight();
            }
        }
        if (type == com.codename1.nfc.NdefMessage.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return new com.codename1.nfc.NdefMessage((java.util.List) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NdefRecord[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NdefRecord[].class}, true);
                com.codename1.nfc.NdefRecord[] varArgs = new com.codename1.nfc.NdefRecord[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.nfc.NdefRecord) adaptedArgs[i];
                }
                return new com.codename1.nfc.NdefMessage(varArgs);
            }
        }
        if (type == com.codename1.nfc.NdefRecord.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, byte[].class, byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class, byte[].class, byte[].class, byte[].class}, false);
                return new com.codename1.nfc.NdefRecord((byte) toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1], (byte[]) adaptedArgs[2], (byte[]) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.nfc.NfcA.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.NfcA();
            }
        }
        if (type == com.codename1.nfc.NfcB.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.NfcB();
            }
        }
        if (type == com.codename1.nfc.NfcException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcError.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcError.class}, false);
                return new com.codename1.nfc.NfcException((com.codename1.nfc.NfcError) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcError.class, java.lang.String.class}, false);
                return new com.codename1.nfc.NfcException((com.codename1.nfc.NfcError) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcError.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcError.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.nfc.NfcException((com.codename1.nfc.NfcError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.nfc.NfcF.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.NfcF();
            }
        }
        if (type == com.codename1.nfc.NfcReadOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.NfcReadOptions();
            }
        }
        if (type == com.codename1.nfc.NfcV.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.nfc.NfcV();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.nfc.ApduResponse.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.nfc.IsoDep.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.nfc.MifareClassic.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.nfc.NdefMessage.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.nfc.NdefRecord.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.nfc.Nfc.class) return invokeStatic5(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("body".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.nfc.ApduResponse.body((byte[]) adaptedArgs[0]);
            }
        }
        if ("isSuccess".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.nfc.ApduResponse.isSuccess((byte[]) adaptedArgs[0]);
            }
        }
        if ("statusWord".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.nfc.ApduResponse.statusWord((byte[]) adaptedArgs[0]);
            }
        }
        if ("sw".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.nfc.ApduResponse.sw(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("swClaNotSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swClaNotSupported();
            }
        }
        if ("swFileNotFound".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swFileNotFound();
            }
        }
        if ("swInsNotSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swInsNotSupported();
            }
        }
        if ("swSecurityNotSatisfied".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swSecurityNotSatisfied();
            }
        }
        if ("swSuccess".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swSuccess();
            }
        }
        if ("swUnknownError".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swUnknownError();
            }
        }
        if ("swWrongLength".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.ApduResponse.swWrongLength();
            }
        }
        if ("withStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.nfc.ApduResponse.withStatus((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.nfc.ApduResponse.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("isSuccess".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.nfc.IsoDep.isSuccess((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.nfc.IsoDep.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("keyDefault".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.MifareClassic.keyDefault();
            }
        }
        if ("keyMifareApplicationDirectory".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.MifareClassic.keyMifareApplicationDirectory();
            }
        }
        if ("keyNfcForum".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.MifareClassic.keyNfcForum();
            }
        }
        throw unsupportedStatic(com.codename1.nfc.MifareClassic.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.nfc.NdefMessage.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.nfc.NdefMessage.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("createApplicationRecord".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.nfc.NdefRecord.createApplicationRecord((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("createExternal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class}, false);
                return com.codename1.nfc.NdefRecord.createExternal((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (byte[]) adaptedArgs[2]);
            }
        }
        if ("createMime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return com.codename1.nfc.NdefRecord.createMime((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("createText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.nfc.NdefRecord.createText((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("createUri".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.nfc.NdefRecord.createUri((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("rtdAndroidApp".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.NdefRecord.rtdAndroidApp();
            }
        }
        if ("rtdSmartPoster".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.NdefRecord.rtdSmartPoster();
            }
        }
        if ("rtdText".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.NdefRecord.rtdText();
            }
        }
        if ("rtdUri".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.NdefRecord.rtdUri();
            }
        }
        throw unsupportedStatic(com.codename1.nfc.NdefRecord.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.nfc.Nfc.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.nfc.Nfc.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.nfc.IsoDep) {
            try {
                return invoke0((com.codename1.nfc.IsoDep) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.MifareClassic) {
            try {
                return invoke1((com.codename1.nfc.MifareClassic) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.MifareUltralight) {
            try {
                return invoke2((com.codename1.nfc.MifareUltralight) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcA) {
            try {
                return invoke3((com.codename1.nfc.NfcA) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcB) {
            try {
                return invoke4((com.codename1.nfc.NfcB) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcF) {
            try {
                return invoke5((com.codename1.nfc.NfcF) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcV) {
            try {
                return invoke6((com.codename1.nfc.NfcV) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.HostCardEmulationService) {
            try {
                return invoke7((com.codename1.nfc.HostCardEmulationService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NdefMessage) {
            try {
                return invoke8((com.codename1.nfc.NdefMessage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NdefRecord) {
            try {
                return invoke9((com.codename1.nfc.NdefRecord) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.Nfc) {
            try {
                return invoke10((com.codename1.nfc.Nfc) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcException) {
            try {
                return invoke11((com.codename1.nfc.NfcException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcReadOptions) {
            try {
                return invoke12((com.codename1.nfc.NfcReadOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.Tag) {
            try {
                return invoke13((com.codename1.nfc.Tag) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.TagTechnology) {
            try {
                return invoke14((com.codename1.nfc.TagTechnology) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.nfc.NfcListener) {
            try {
                return invoke15((com.codename1.nfc.NfcListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.nfc.IsoDep typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getHistoricalBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHistoricalBytes();
            }
        }
        if ("getMaxTransceiveLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxTransceiveLength();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("isExtendedLengthSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExtendedLengthSupported();
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.nfc.MifareClassic typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authenticateSectorWithKeyA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                return typedTarget.authenticateSectorWithKeyA(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]);
            }
        }
        if ("authenticateSectorWithKeyB".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                return typedTarget.authenticateSectorWithKeyB(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]);
            }
        }
        if ("getBlockCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBlockCount();
            }
        }
        if ("getSectorCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSectorCount();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("readBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.readBlock(toIntValue(adaptedArgs[0]));
            }
        }
        if ("sectorToBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.sectorToBlock(toIntValue(adaptedArgs[0]));
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        if ("writeBlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                return typedTarget.writeBlock(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.nfc.MifareUltralight typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPageCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPageCount();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("readPages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.readPages(toIntValue(adaptedArgs[0]));
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        if ("writePage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                return typedTarget.writePage(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.nfc.NfcA typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAtqa".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAtqa();
            }
        }
        if ("getSak".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSak();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.nfc.NfcB typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getApplicationData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getApplicationData();
            }
        }
        if ("getProtocolInfo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProtocolInfo();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.nfc.NfcF typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getIdm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdm();
            }
        }
        if ("getPmm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPmm();
            }
        }
        if ("getSystemCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSystemCode();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.nfc.NfcV typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDsfid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDsfid();
            }
        }
        if ("getResponseFlags".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseFlags();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.nfc.HostCardEmulationService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAids".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAids();
            }
        }
        if ("getCategory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategory();
            }
        }
        if ("getServiceDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceDescription();
            }
        }
        if ("onDeactivated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.onDeactivated(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("processCommand".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.processCommand((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.nfc.NdefMessage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFirstRecord".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirstRecord();
            }
        }
        if ("getRecords".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecords();
            }
        }
        if ("toByteArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toByteArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.nfc.NdefRecord typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getPayload".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPayload();
            }
        }
        if ("getTextPayload".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextPayload();
            }
        }
        if ("getTnf".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTnf();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUriPayload".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUriPayload();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.nfc.Nfc typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addTagListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcListener.class}, false);
                typedTarget.addTagListener((com.codename1.nfc.NfcListener) adaptedArgs[0]); return null;
            }
        }
        if ("canHostEmulate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canHostEmulate();
            }
        }
        if ("canRead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canRead();
            }
        }
        if ("canWrite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canWrite();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("readNdef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcReadOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcReadOptions.class}, false);
                return typedTarget.readNdef((com.codename1.nfc.NfcReadOptions) adaptedArgs[0]);
            }
        }
        if ("readTag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcReadOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcReadOptions.class}, false);
                return typedTarget.readTag((com.codename1.nfc.NfcReadOptions) adaptedArgs[0]);
            }
        }
        if ("registerHostCardEmulationService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.HostCardEmulationService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.HostCardEmulationService.class}, false);
                typedTarget.registerHostCardEmulationService((com.codename1.nfc.HostCardEmulationService) adaptedArgs[0]); return null;
            }
        }
        if ("removeTagListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcListener.class}, false);
                typedTarget.removeTagListener((com.codename1.nfc.NfcListener) adaptedArgs[0]); return null;
            }
        }
        if ("stopRead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stopRead();
            }
        }
        if ("unregisterHostCardEmulationService".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unregisterHostCardEmulationService(); return null;
            }
        }
        if ("writeNdef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcReadOptions.class, com.codename1.nfc.NdefMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcReadOptions.class, com.codename1.nfc.NdefMessage.class}, false);
                return typedTarget.writeNdef((com.codename1.nfc.NfcReadOptions) adaptedArgs[0], (com.codename1.nfc.NdefMessage) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.nfc.NfcException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.nfc.NfcReadOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlertMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlertMessage();
            }
        }
        if ("getFelicaSystemCodes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFelicaSystemCodes();
            }
        }
        if ("getInvalidatedMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInvalidatedMessage();
            }
        }
        if ("getIsoSelectAids".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIsoSelectAids();
            }
        }
        if ("getTechFilter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTechFilter();
            }
        }
        if ("getTimeoutMs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeoutMs();
            }
        }
        if ("isNdefOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNdefOnly();
            }
        }
        if ("setAlertMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAlertMessage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setFelicaSystemCodes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.setFelicaSystemCodes(varArgs);
            }
        }
        if ("setInvalidatedMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setInvalidatedMessage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setIsoSelectAids".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[][].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[][].class}, true);
                byte[][] varArgs = new byte[adaptedArgs.length - 0][];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (byte[]) adaptedArgs[i];
                }
                return typedTarget.setIsoSelectAids(varArgs);
            }
        }
        if ("setNdefOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setNdefOnly(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTechFilter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.TagType[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.TagType[].class}, true);
                com.codename1.nfc.TagType[] varArgs = new com.codename1.nfc.TagType[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.nfc.TagType) adaptedArgs[i];
                }
                return typedTarget.setTechFilter(varArgs);
            }
        }
        if ("setTimeoutMs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.setTimeoutMs(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.nfc.Tag typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getIsoDep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIsoDep();
            }
        }
        if ("getMaxNdefSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxNdefSize();
            }
        }
        if ("getMifareClassic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMifareClassic();
            }
        }
        if ("getMifareUltralight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMifareUltralight();
            }
        }
        if ("getNfcA".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNfcA();
            }
        }
        if ("getNfcB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNfcB();
            }
        }
        if ("getNfcF".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNfcF();
            }
        }
        if ("getNfcV".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNfcV();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
            }
        }
        if ("hasNdef".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasNdef();
            }
        }
        if ("isWritable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWritable();
            }
        }
        if ("makeReadOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.makeReadOnly();
            }
        }
        if ("readNdef".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readNdef();
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.TagType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.TagType.class}, false);
                return typedTarget.supports((com.codename1.nfc.TagType) adaptedArgs[0]);
            }
        }
        if ("writeNdef".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NdefMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NdefMessage.class}, false);
                return typedTarget.writeNdef((com.codename1.nfc.NdefMessage) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.nfc.TagTechnology typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("transceive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.transceive((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.nfc.NfcListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("sessionFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.NfcException.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.NfcException.class}, false);
                typedTarget.sessionFailed((com.codename1.nfc.NfcException) adaptedArgs[0]); return null;
            }
        }
        if ("tagDiscovered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.nfc.Tag.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.nfc.Tag.class}, false);
                typedTarget.tagDiscovered((com.codename1.nfc.Tag) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.nfc.HostCardEmulationService.class) return getStaticField0(name);
        if (type == com.codename1.nfc.NdefRecord.class) return getStaticField1(name);
        if (type == com.codename1.nfc.NfcError.class) return getStaticField2(name);
        if (type == com.codename1.nfc.TagType.class) return getStaticField3(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("CATEGORY_OTHER".equals(name)) return com.codename1.nfc.HostCardEmulationService.CATEGORY_OTHER;
        if ("CATEGORY_PAYMENT".equals(name)) return com.codename1.nfc.HostCardEmulationService.CATEGORY_PAYMENT;
        if ("DEACTIVATION_DESELECTED".equals(name)) return com.codename1.nfc.HostCardEmulationService.DEACTIVATION_DESELECTED;
        if ("DEACTIVATION_LINK_LOSS".equals(name)) return com.codename1.nfc.HostCardEmulationService.DEACTIVATION_LINK_LOSS;
        throw unsupportedStaticField(com.codename1.nfc.HostCardEmulationService.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("TNF_ABSOLUTE_URI".equals(name)) return com.codename1.nfc.NdefRecord.TNF_ABSOLUTE_URI;
        if ("TNF_EMPTY".equals(name)) return com.codename1.nfc.NdefRecord.TNF_EMPTY;
        if ("TNF_EXTERNAL_TYPE".equals(name)) return com.codename1.nfc.NdefRecord.TNF_EXTERNAL_TYPE;
        if ("TNF_MIME_MEDIA".equals(name)) return com.codename1.nfc.NdefRecord.TNF_MIME_MEDIA;
        if ("TNF_UNCHANGED".equals(name)) return com.codename1.nfc.NdefRecord.TNF_UNCHANGED;
        if ("TNF_UNKNOWN".equals(name)) return com.codename1.nfc.NdefRecord.TNF_UNKNOWN;
        if ("TNF_WELL_KNOWN".equals(name)) return com.codename1.nfc.NdefRecord.TNF_WELL_KNOWN;
        throw unsupportedStaticField(com.codename1.nfc.NdefRecord.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("CAPACITY_EXCEEDED".equals(name)) return com.codename1.nfc.NfcError.CAPACITY_EXCEEDED;
        if ("DISABLED".equals(name)) return com.codename1.nfc.NfcError.DISABLED;
        if ("INVALID_NDEF".equals(name)) return com.codename1.nfc.NfcError.INVALID_NDEF;
        if ("IO_ERROR".equals(name)) return com.codename1.nfc.NfcError.IO_ERROR;
        if ("NOT_AUTHORIZED".equals(name)) return com.codename1.nfc.NfcError.NOT_AUTHORIZED;
        if ("NOT_AVAILABLE".equals(name)) return com.codename1.nfc.NfcError.NOT_AVAILABLE;
        if ("READ_ONLY".equals(name)) return com.codename1.nfc.NfcError.READ_ONLY;
        if ("SYSTEM_CANCELED".equals(name)) return com.codename1.nfc.NfcError.SYSTEM_CANCELED;
        if ("TAG_LOST".equals(name)) return com.codename1.nfc.NfcError.TAG_LOST;
        if ("UNKNOWN".equals(name)) return com.codename1.nfc.NfcError.UNKNOWN;
        if ("UNKNOWN_AID".equals(name)) return com.codename1.nfc.NfcError.UNKNOWN_AID;
        if ("UNSUPPORTED_TAG".equals(name)) return com.codename1.nfc.NfcError.UNSUPPORTED_TAG;
        if ("USER_CANCELED".equals(name)) return com.codename1.nfc.NfcError.USER_CANCELED;
        throw unsupportedStaticField(com.codename1.nfc.NfcError.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("ISO_DEP".equals(name)) return com.codename1.nfc.TagType.ISO_DEP;
        if ("MIFARE_CLASSIC".equals(name)) return com.codename1.nfc.TagType.MIFARE_CLASSIC;
        if ("MIFARE_ULTRALIGHT".equals(name)) return com.codename1.nfc.TagType.MIFARE_ULTRALIGHT;
        if ("NDEF".equals(name)) return com.codename1.nfc.TagType.NDEF;
        if ("NFC_A".equals(name)) return com.codename1.nfc.TagType.NFC_A;
        if ("NFC_B".equals(name)) return com.codename1.nfc.TagType.NFC_B;
        if ("NFC_F".equals(name)) return com.codename1.nfc.TagType.NFC_F;
        if ("NFC_V".equals(name)) return com.codename1.nfc.TagType.NFC_V;
        throw unsupportedStaticField(com.codename1.nfc.TagType.class, name);
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
