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

public final class GeneratedAccess_com_codename1_home {
    private GeneratedAccess_com_codename1_home() {
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
        if ("Accessory".equals(simpleName)) {
            return com.codename1.home.Accessory.class;
        }
        if ("AccessoryCategory".equals(simpleName)) {
            return com.codename1.home.AccessoryCategory.class;
        }
        if ("AccessoryService".equals(simpleName)) {
            return com.codename1.home.AccessoryService.class;
        }
        if ("AirQualityLevel".equals(simpleName)) {
            return com.codename1.home.AirQualityLevel.class;
        }
        if ("AlarmState".equals(simpleName)) {
            return com.codename1.home.AlarmState.class;
        }
        if ("ChargingState".equals(simpleName)) {
            return com.codename1.home.ChargingState.class;
        }
        if ("DoorState".equals(simpleName)) {
            return com.codename1.home.DoorState.class;
        }
        if ("FanMode".equals(simpleName)) {
            return com.codename1.home.FanMode.class;
        }
        if ("HeatingCoolingMode".equals(simpleName)) {
            return com.codename1.home.HeatingCoolingMode.class;
        }
        if ("HomeAuthorizationStatus".equals(simpleName)) {
            return com.codename1.home.HomeAuthorizationStatus.class;
        }
        if ("HomeAvailability".equals(simpleName)) {
            return com.codename1.home.HomeAvailability.class;
        }
        if ("HomeBackend".equals(simpleName)) {
            return com.codename1.home.HomeBackend.class;
        }
        if ("HomeChangeListener".equals(simpleName)) {
            return com.codename1.home.HomeChangeListener.class;
        }
        if ("HomeConfigurationException".equals(simpleName)) {
            return com.codename1.home.HomeConfigurationException.class;
        }
        if ("HomeError".equals(simpleName)) {
            return com.codename1.home.HomeError.class;
        }
        if ("HomeException".equals(simpleName)) {
            return com.codename1.home.HomeException.class;
        }
        if ("HomeRoom".equals(simpleName)) {
            return com.codename1.home.HomeRoom.class;
        }
        if ("HomeStructure".equals(simpleName)) {
            return com.codename1.home.HomeStructure.class;
        }
        if ("HomeStructureEvent".equals(simpleName)) {
            return com.codename1.home.HomeStructureEvent.class;
        }
        if ("HomeStructureListener".equals(simpleName)) {
            return com.codename1.home.HomeStructureListener.class;
        }
        if ("HomeZone".equals(simpleName)) {
            return com.codename1.home.HomeZone.class;
        }
        if ("LockState".equals(simpleName)) {
            return com.codename1.home.LockState.class;
        }
        if ("PositionState".equals(simpleName)) {
            return com.codename1.home.PositionState.class;
        }
        if ("Scene".equals(simpleName)) {
            return com.codename1.home.Scene.class;
        }
        if ("SceneAction".equals(simpleName)) {
            return com.codename1.home.SceneAction.class;
        }
        if ("SceneType".equals(simpleName)) {
            return com.codename1.home.SceneType.class;
        }
        if ("ServiceType".equals(simpleName)) {
            return com.codename1.home.ServiceType.class;
        }
        if ("SmartHome".equals(simpleName)) {
            return com.codename1.home.SmartHome.class;
        }
        if ("StructureChangeKind".equals(simpleName)) {
            return com.codename1.home.StructureChangeKind.class;
        }
        if ("SubscriptionRequest".equals(simpleName)) {
            return com.codename1.home.SubscriptionRequest.class;
        }
        if ("Trait".equals(simpleName)) {
            return com.codename1.home.Trait.class;
        }
        if ("TraitChangeBatch".equals(simpleName)) {
            return com.codename1.home.TraitChangeBatch.class;
        }
        if ("TraitConstraint".equals(simpleName)) {
            return com.codename1.home.TraitConstraint.class;
        }
        if ("TraitReadRequest".equals(simpleName)) {
            return com.codename1.home.TraitReadRequest.class;
        }
        if ("TraitReading".equals(simpleName)) {
            return com.codename1.home.TraitReading.class;
        }
        if ("TraitSubscription".equals(simpleName)) {
            return com.codename1.home.TraitSubscription.class;
        }
        if ("TraitUnit".equals(simpleName)) {
            return com.codename1.home.TraitUnit.class;
        }
        if ("TraitUnitDimension".equals(simpleName)) {
            return com.codename1.home.TraitUnitDimension.class;
        }
        if ("TraitValue".equals(simpleName)) {
            return com.codename1.home.TraitValue.class;
        }
        if ("TraitValueKind".equals(simpleName)) {
            return com.codename1.home.TraitValueKind.class;
        }
        if ("TraitWrite".equals(simpleName)) {
            return com.codename1.home.TraitWrite.class;
        }
        if ("TraitWriteResult".equals(simpleName)) {
            return com.codename1.home.TraitWriteResult.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.home.Accessory.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.home.AccessoryCategory.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.home.AccessoryCategory.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.util.List.class}, false);
                return new com.codename1.home.Accessory((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.home.AccessoryCategory) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (java.lang.String) adaptedArgs[6], ((Boolean) adaptedArgs[7]).booleanValue(), (java.lang.String) adaptedArgs[8], (java.util.List) adaptedArgs[9]);
            }
        }
        if (type == com.codename1.home.AccessoryService.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.ServiceType.class, java.lang.Boolean.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.ServiceType.class, java.lang.Boolean.class, java.util.List.class}, false);
                return new com.codename1.home.AccessoryService((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.ServiceType) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), (java.util.List) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.home.HomeConfigurationException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.home.HomeConfigurationException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.home.HomeException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeError.class, java.lang.String.class}, false);
                return new com.codename1.home.HomeException((com.codename1.home.HomeError) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeError.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeError.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.home.HomeException((com.codename1.home.HomeError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeError.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeError.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.home.HomeException((com.codename1.home.HomeError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.Throwable) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.home.HomeRoom.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.home.HomeRoom((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.home.HomeStructure.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.util.List.class, java.util.List.class, java.util.List.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.util.List.class, java.util.List.class, java.util.List.class, java.util.List.class}, false);
                return new com.codename1.home.HomeStructure((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), (java.util.List) adaptedArgs[5], (java.util.List) adaptedArgs[6], (java.util.List) adaptedArgs[7], (java.util.List) adaptedArgs[8]);
            }
        }
        if (type == com.codename1.home.HomeStructureEvent.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.StructureChangeKind.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.StructureChangeKind.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.home.HomeStructureEvent((com.codename1.home.StructureChangeKind) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.home.HomeZone.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.util.List.class}, false);
                return new com.codename1.home.HomeZone((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.List) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.home.Scene.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.home.SceneType.class, java.lang.Boolean.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.home.SceneType.class, java.lang.Boolean.class, java.util.List.class}, false);
                return new com.codename1.home.Scene((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.home.SceneType) adaptedArgs[3], ((Boolean) adaptedArgs[4]).booleanValue(), (java.util.List) adaptedArgs[5]);
            }
        }
        if (type == com.codename1.home.SceneAction.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class}, false);
                return new com.codename1.home.SceneAction((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2], (com.codename1.home.TraitValue) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.home.SubscriptionRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.home.SubscriptionRequest();
            }
        }
        if (type == com.codename1.home.TraitChangeBatch.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return new com.codename1.home.TraitChangeBatch((java.lang.String) adaptedArgs[0], (java.util.List) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if (type == com.codename1.home.TraitReadRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.home.TraitReadRequest();
            }
        }
        if (type == com.codename1.home.TraitWrite.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class}, false);
                return new com.codename1.home.TraitWrite((com.codename1.home.Accessory) adaptedArgs[0], (com.codename1.home.AccessoryService) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2], (com.codename1.home.TraitValue) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class}, false);
                return new com.codename1.home.TraitWrite((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2], (com.codename1.home.TraitValue) adaptedArgs[3]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.home.AirQualityLevel.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.home.AlarmState.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.home.ChargingState.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.home.DoorState.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.home.FanMode.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.home.HeatingCoolingMode.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.home.HomeError.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.home.LockState.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.home.PositionState.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.home.SmartHome.class) return invokeStatic9(name, safeArgs);
        if (type == com.codename1.home.Trait.class) return invokeStatic10(name, safeArgs);
        if (type == com.codename1.home.TraitConstraint.class) return invokeStatic11(name, safeArgs);
        if (type == com.codename1.home.TraitReading.class) return invokeStatic12(name, safeArgs);
        if (type == com.codename1.home.TraitUnit.class) return invokeStatic13(name, safeArgs);
        if (type == com.codename1.home.TraitValue.class) return invokeStatic14(name, safeArgs);
        if (type == com.codename1.home.TraitWriteResult.class) return invokeStatic15(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.AirQualityLevel.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.AirQualityLevel.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.AlarmState.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.AlarmState.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.ChargingState.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.ChargingState.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.DoorState.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.DoorState.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.FanMode.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.FanMode.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.HeatingCoolingMode.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.HeatingCoolingMode.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("forName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.home.HomeError.forName((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.HomeError.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.LockState.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.LockState.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return com.codename1.home.PositionState.of((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.PositionState.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("deliverAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverAuthorization(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("deliverChanges".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                com.codename1.home.SmartHome.deliverChanges((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("deliverCommissioningResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverCommissioningResult(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], toIntValue(adaptedArgs[4]), (java.lang.String) adaptedArgs[5]); return null;
            }
        }
        if ("deliverDrained".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverDrained(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("deliverIdentifyResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverIdentifyResult(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("deliverReadings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String[].class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverReadings(toIntValue(adaptedArgs[0]), (java.lang.String[]) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("deliverRefreshed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverRefreshed(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("deliverResyncRequired".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverResyncRequired((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("deliverSceneResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverSceneResult(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]); return null;
            }
        }
        if ("deliverStarted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverStarted(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("deliverWriteResults".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String[].class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.deliverWriteResults(toIntValue(adaptedArgs[0]), (java.lang.String[]) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.home.SmartHome.getInstance();
            }
        }
        if ("notifyStructureChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class}, false);
                com.codename1.home.SmartHome.notifyStructureChanged(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.home.SmartHome.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("all".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.home.Trait.all();
            }
        }
        if ("forId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.home.Trait.forId((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.home.Trait.class, name, safeArgs);
    }

    private static Object invokeStatic11(String name, Object[] safeArgs) throws Exception {
        if ("choices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, int[].class}, false);
                return com.codename1.home.TraitConstraint.choices((com.codename1.home.Trait) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), (int[]) adaptedArgs[4]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return com.codename1.home.TraitConstraint.of((com.codename1.home.Trait) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("ranged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.home.TraitConstraint.ranged((com.codename1.home.Trait) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Number) adaptedArgs[4]).doubleValue(), ((Number) adaptedArgs[5]).doubleValue(), ((Number) adaptedArgs[6]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.home.TraitConstraint.class, name, safeArgs);
    }

    private static Object invokeStatic12(String name, Object[] safeArgs) throws Exception {
        if ("absent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class}, false);
                return com.codename1.home.TraitReading.absent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2]);
            }
        }
        if ("failed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.HomeError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.HomeError.class, java.lang.String.class}, false);
                return com.codename1.home.TraitReading.failed((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2], (com.codename1.home.HomeError) adaptedArgs[3], (java.lang.String) adaptedArgs[4]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class, com.codename1.home.TraitValue.class, java.lang.Long.class}, false);
                return com.codename1.home.TraitReading.of((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2], (com.codename1.home.TraitValue) adaptedArgs[3], ((Number) adaptedArgs[4]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.home.TraitReading.class, name, safeArgs);
    }

    private static Object invokeStatic13(String name, Object[] safeArgs) throws Exception {
        if ("convert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.home.TraitUnit.class, com.codename1.home.TraitUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.home.TraitUnit.class, com.codename1.home.TraitUnit.class}, false);
                return com.codename1.home.TraitUnit.convert(((Number) adaptedArgs[0]).doubleValue(), (com.codename1.home.TraitUnit) adaptedArgs[1], (com.codename1.home.TraitUnit) adaptedArgs[2]);
            }
        }
        if ("forWireId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.home.TraitUnit.forWireId(toIntValue(adaptedArgs[0]));
            }
        }
        if ("kelvinToMired".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.home.TraitUnit.kelvinToMired(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("miredToKelvin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.home.TraitUnit.miredToKelvin(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.home.TraitUnit.class, name, safeArgs);
    }

    private static Object invokeStatic14(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return com.codename1.home.TraitValue.of(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.home.TraitValue.of(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.home.TraitValue.of((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.home.TraitUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.home.TraitUnit.class}, false);
                return com.codename1.home.TraitValue.of(((Number) adaptedArgs[0]).doubleValue(), (com.codename1.home.TraitUnit) adaptedArgs[1]);
            }
        }
        if ("ofEnum".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Enum.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Enum.class}, false);
                return com.codename1.home.TraitValue.ofEnum((java.lang.Enum) adaptedArgs[0]);
            }
        }
        if ("ofEnumOrdinal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.home.TraitValue.ofEnumOrdinal(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(com.codename1.home.TraitValue.class, name, safeArgs);
    }

    private static Object invokeStatic15(String name, Object[] safeArgs) throws Exception {
        if ("applied".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitWrite.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitWrite.class}, false);
                return com.codename1.home.TraitWriteResult.applied((com.codename1.home.TraitWrite) adaptedArgs[0]);
            }
        }
        if ("failed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitWrite.class, com.codename1.home.HomeError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitWrite.class, com.codename1.home.HomeError.class, java.lang.String.class}, false);
                return com.codename1.home.TraitWriteResult.failed((com.codename1.home.TraitWrite) adaptedArgs[0], (com.codename1.home.HomeError) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.home.TraitWriteResult.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.home.HomeConfigurationException) {
            try {
                return invoke0((com.codename1.home.HomeConfigurationException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.Accessory) {
            try {
                return invoke1((com.codename1.home.Accessory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.AccessoryService) {
            try {
                return invoke2((com.codename1.home.AccessoryService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.DoorState) {
            try {
                return invoke3((com.codename1.home.DoorState) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HeatingCoolingMode) {
            try {
                return invoke4((com.codename1.home.HeatingCoolingMode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeException) {
            try {
                return invoke5((com.codename1.home.HomeException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeRoom) {
            try {
                return invoke6((com.codename1.home.HomeRoom) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeStructure) {
            try {
                return invoke7((com.codename1.home.HomeStructure) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeStructureEvent) {
            try {
                return invoke8((com.codename1.home.HomeStructureEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeZone) {
            try {
                return invoke9((com.codename1.home.HomeZone) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.LockState) {
            try {
                return invoke10((com.codename1.home.LockState) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.Scene) {
            try {
                return invoke11((com.codename1.home.Scene) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.SceneAction) {
            try {
                return invoke12((com.codename1.home.SceneAction) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.SmartHome) {
            try {
                return invoke13((com.codename1.home.SmartHome) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.SubscriptionRequest) {
            try {
                return invoke14((com.codename1.home.SubscriptionRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.Trait) {
            try {
                return invoke15((com.codename1.home.Trait) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitChangeBatch) {
            try {
                return invoke16((com.codename1.home.TraitChangeBatch) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitConstraint) {
            try {
                return invoke17((com.codename1.home.TraitConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitReadRequest) {
            try {
                return invoke18((com.codename1.home.TraitReadRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitReading) {
            try {
                return invoke19((com.codename1.home.TraitReading) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitSubscription) {
            try {
                return invoke20((com.codename1.home.TraitSubscription) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitUnit) {
            try {
                return invoke21((com.codename1.home.TraitUnit) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitValue) {
            try {
                return invoke22((com.codename1.home.TraitValue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitWrite) {
            try {
                return invoke23((com.codename1.home.TraitWrite) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.TraitWriteResult) {
            try {
                return invoke24((com.codename1.home.TraitWriteResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeChangeListener) {
            try {
                return invoke25((com.codename1.home.HomeChangeListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.home.HomeStructureListener) {
            try {
                return invoke26((com.codename1.home.HomeStructureListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.home.HomeConfigurationException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.home.Accessory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBridgeAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBridgeAccessoryId();
            }
        }
        if ("getCategory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategory();
            }
        }
        if ("getFirmwareVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirmwareVersion();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getManufacturer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getManufacturer();
            }
        }
        if ("getModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPrimaryService".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrimaryService();
            }
        }
        if ("getRoomId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoomId();
            }
        }
        if ("getService".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getService((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getServices".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServices();
            }
        }
        if ("getServicesSupporting".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false);
                return typedTarget.getServicesSupporting((com.codename1.home.Trait) adaptedArgs[0]);
            }
        }
        if ("isBridged".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBridged();
            }
        }
        if ("isReachable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReachable();
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false);
                return typedTarget.supports((com.codename1.home.Trait) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.home.AccessoryService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false);
                return typedTarget.getConstraint((com.codename1.home.Trait) adaptedArgs[0]);
            }
        }
        if ("getConstraints".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConstraints();
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
        if ("getTraits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTraits();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("isPrimary".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrimary();
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false);
                return typedTarget.supports((com.codename1.home.Trait) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.home.DoorState typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isWritable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWritable();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.home.HeatingCoolingMode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isWritable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWritable();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.home.HomeException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.home.HomeRoom typedTarget, String name, Object[] safeArgs) throws Exception {
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.home.HomeStructure typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessories".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessories();
            }
        }
        if ("getAccessoriesInRoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getAccessoriesInRoom((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getAccessoriesSupporting".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Trait.class}, false);
                return typedTarget.getAccessoriesSupporting((com.codename1.home.Trait) adaptedArgs[0]);
            }
        }
        if ("getAccessory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getAccessory((java.lang.String) adaptedArgs[0]);
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
        if ("getRoom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getRoom((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getRooms".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRooms();
            }
        }
        if ("getScenes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScenes();
            }
        }
        if ("getZones".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZones();
            }
        }
        if ("isOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOwner();
            }
        }
        if ("isPrimary".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrimary();
            }
        }
        if ("isSceneAuthoringSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSceneAuthoringSupported();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.home.HomeStructureEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.home.HomeZone typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getRoomIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoomIds();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.home.LockState typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isWritable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWritable();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.home.Scene typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
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
        if ("getStructureId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStructureId();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("isExecutable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExecutable();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.home.SceneAction typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getServiceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceId();
            }
        }
        if ("getTrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrait();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.home.SmartHome typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addStructureListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeStructureListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeStructureListener.class}, false);
                typedTarget.addStructureListener((com.codename1.home.HomeStructureListener) adaptedArgs[0]); return null;
            }
        }
        if ("areIdsPersistent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.areIdsPersistent();
            }
        }
        if ("createScene".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeStructure.class, java.lang.String.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeStructure.class, java.lang.String.class, java.util.List.class}, false);
                return typedTarget.createScene((com.codename1.home.HomeStructure) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.util.List) adaptedArgs[2]);
            }
        }
        if ("deleteScene".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Scene.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Scene.class}, false);
                return typedTarget.deleteScene((com.codename1.home.Scene) adaptedArgs[0]);
            }
        }
        if ("drainChanges".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.drainChanges();
            }
        }
        if ("executeScene".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Scene.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Scene.class}, false);
                return typedTarget.executeScene((com.codename1.home.Scene) adaptedArgs[0]);
            }
        }
        if ("findAccessory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.findAccessory((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthorizationStatus();
            }
        }
        if ("getAvailability".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailability();
            }
        }
        if ("getBackend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackend();
            }
        }
        if ("getCommissioner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommissioner();
            }
        }
        if ("getConfigurationProblems".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfigurationProblems();
            }
        }
        if ("getMaxReadBatchSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxReadBatchSize();
            }
        }
        if ("getMaxWriteBatchSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxWriteBatchSize();
            }
        }
        if ("getPrimaryStructure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrimaryStructure();
            }
        }
        if ("getStructures".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStructures();
            }
        }
        if ("identify".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class}, false);
                return typedTarget.identify((com.codename1.home.Accessory) adaptedArgs[0]);
            }
        }
        if ("isAutomationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutomationSupported();
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
        if ("openHomeSettings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.openHomeSettings();
            }
        }
        if ("openProviderSetup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.openProviderSetup();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitReadRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitReadRequest.class}, false);
                return typedTarget.read((com.codename1.home.TraitReadRequest) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class}, false);
                return typedTarget.read((com.codename1.home.Accessory) adaptedArgs[0], (com.codename1.home.AccessoryService) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2]);
            }
        }
        if ("refresh".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.refresh();
            }
        }
        if ("removeStructureListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeStructureListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeStructureListener.class}, false);
                typedTarget.removeStructureListener((com.codename1.home.HomeStructureListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.requestAuthorization();
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.SubscriptionRequest.class, com.codename1.home.HomeChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.SubscriptionRequest.class, com.codename1.home.HomeChangeListener.class}, false);
                return typedTarget.subscribe((com.codename1.home.SubscriptionRequest) adaptedArgs[0], (com.codename1.home.HomeChangeListener) adaptedArgs[1]);
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitWrite.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitWrite.class}, false);
                return typedTarget.write((com.codename1.home.TraitWrite) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.write((java.util.List) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.home.SubscriptionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class}, false);
                return typedTarget.add((com.codename1.home.Accessory) adaptedArgs[0], (com.codename1.home.AccessoryService) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2]);
            }
        }
        if ("getAccessoryIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryIds();
            }
        }
        if ("getMinIntervalMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinIntervalMillis();
            }
        }
        if ("getServiceIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceIds();
            }
        }
        if ("getTraits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTraits();
            }
        }
        if ("isDeliverInitialValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDeliverInitialValues();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("setDeliverInitialValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setDeliverInitialValues(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setMinIntervalMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setMinIntervalMillis(toIntValue(adaptedArgs[0]));
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.home.Trait typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("acceptsEnumValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return typedTarget.acceptsEnumValue((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        if ("acceptsEnumWrite".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return typedTarget.acceptsEnumWrite((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        if ("acceptsUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitUnit.class}, false);
                return typedTarget.acceptsUnit((com.codename1.home.TraitUnit) adaptedArgs[0]);
            }
        }
        if ("enumValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.enumValue(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getNominalMaximum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNominalMaximum();
            }
        }
        if ("getNominalMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNominalMinimum();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("getValueKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValueKind();
            }
        }
        if ("hasNominalRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasNominalRange();
            }
        }
        if ("isReadOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadOnly();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.home.TraitChangeBatch typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getReadings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadings();
            }
        }
        if ("getSubscriptionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubscriptionId();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("isInitialDelivery".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInitialDelivery();
            }
        }
        if ("isResyncRequired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isResyncRequired();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.home.TraitConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accepts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitValue.class}, false);
                return typedTarget.accepts((com.codename1.home.TraitValue) adaptedArgs[0]);
            }
        }
        if ("getMaximum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaximum();
            }
        }
        if ("getMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimum();
            }
        }
        if ("getStep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStep();
            }
        }
        if ("getTrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrait();
            }
        }
        if ("getValidOrdinals".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidOrdinals();
            }
        }
        if ("hasRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasRange();
            }
        }
        if ("isReadable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadable();
            }
        }
        if ("isWritable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWritable();
            }
        }
        if ("notifiesOnChange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.notifiesOnChange();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.home.TraitReadRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class, com.codename1.home.Trait.class}, false);
                return typedTarget.add((com.codename1.home.Accessory) adaptedArgs[0], (com.codename1.home.AccessoryService) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.home.Trait.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.home.Trait) adaptedArgs[2]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.Accessory.class, com.codename1.home.AccessoryService.class}, false);
                return typedTarget.addAll((com.codename1.home.Accessory) adaptedArgs[0], (com.codename1.home.AccessoryService) adaptedArgs[1]);
            }
        }
        if ("getAccessoryIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryIds();
            }
        }
        if ("getServiceIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceIds();
            }
        }
        if ("getTraits".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTraits();
            }
        }
        if ("isAllowCached".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllowCached();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("setAllowCached".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setAllowCached(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.home.TraitReading typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrorMessage();
            }
        }
        if ("getServiceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceId();
            }
        }
        if ("getTimestampMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampMillis();
            }
        }
        if ("getTrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrait();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("hasValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasValue();
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.home.TraitSubscription typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("isActive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isActive();
            }
        }
        if ("isPushDelivery".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPushDelivery();
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.home.TraitUnit typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDimension();
            }
        }
        if ("getWireId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWireId();
            }
        }
        if ("isCompatibleWith".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitUnit.class}, false);
                return typedTarget.isCompatibleWith((com.codename1.home.TraitUnit) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.home.TraitValue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getBoolean".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBoolean();
            }
        }
        if ("getColorTemperatureKelvin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColorTemperatureKelvin();
            }
        }
        if ("getDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitUnit.class}, false);
                return typedTarget.getDouble((com.codename1.home.TraitUnit) adaptedArgs[0]);
            }
        }
        if ("getEnumName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnumName();
            }
        }
        if ("getEnumOrdinal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnumOrdinal();
            }
        }
        if ("getInt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInt();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getRawDouble".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawDouble();
            }
        }
        if ("getRawPlatformValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawPlatformValue();
            }
        }
        if ("getString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getString();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("hasRawPlatformValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasRawPlatformValue();
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
        if ("withRawPlatformValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.withRawPlatformValue(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.home.TraitWrite typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessoryId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessoryId();
            }
        }
        if ("getAuthorizationData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthorizationData();
            }
        }
        if ("getServiceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceId();
            }
        }
        if ("getTrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrait();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("setAuthorizationData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAuthorizationData((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toSceneAction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toSceneAction();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.home.TraitWriteResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrorMessage();
            }
        }
        if ("getWrite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWrite();
            }
        }
        if ("isApplied".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isApplied();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(com.codename1.home.HomeChangeListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("traitsChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.TraitChangeBatch.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.TraitChangeBatch.class}, false);
                typedTarget.traitsChanged((com.codename1.home.TraitChangeBatch) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(com.codename1.home.HomeStructureListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("structureChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.home.HomeStructureEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.home.HomeStructureEvent.class}, false);
                typedTarget.structureChanged((com.codename1.home.HomeStructureEvent) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.home.AccessoryCategory.class) return getStaticField0(name);
        if (type == com.codename1.home.AirQualityLevel.class) return getStaticField1(name);
        if (type == com.codename1.home.AlarmState.class) return getStaticField2(name);
        if (type == com.codename1.home.ChargingState.class) return getStaticField3(name);
        if (type == com.codename1.home.DoorState.class) return getStaticField4(name);
        if (type == com.codename1.home.FanMode.class) return getStaticField5(name);
        if (type == com.codename1.home.HeatingCoolingMode.class) return getStaticField6(name);
        if (type == com.codename1.home.HomeAuthorizationStatus.class) return getStaticField7(name);
        if (type == com.codename1.home.HomeAvailability.class) return getStaticField8(name);
        if (type == com.codename1.home.HomeBackend.class) return getStaticField9(name);
        if (type == com.codename1.home.HomeError.class) return getStaticField10(name);
        if (type == com.codename1.home.LockState.class) return getStaticField11(name);
        if (type == com.codename1.home.PositionState.class) return getStaticField12(name);
        if (type == com.codename1.home.SceneType.class) return getStaticField13(name);
        if (type == com.codename1.home.ServiceType.class) return getStaticField14(name);
        if (type == com.codename1.home.StructureChangeKind.class) return getStaticField15(name);
        if (type == com.codename1.home.SubscriptionRequest.class) return getStaticField16(name);
        if (type == com.codename1.home.Trait.class) return getStaticField17(name);
        if (type == com.codename1.home.TraitUnit.class) return getStaticField18(name);
        if (type == com.codename1.home.TraitUnitDimension.class) return getStaticField19(name);
        if (type == com.codename1.home.TraitValueKind.class) return getStaticField20(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("AIR_PURIFIER".equals(name)) return com.codename1.home.AccessoryCategory.AIR_PURIFIER;
        if ("BRIDGE".equals(name)) return com.codename1.home.AccessoryCategory.BRIDGE;
        if ("CAMERA".equals(name)) return com.codename1.home.AccessoryCategory.CAMERA;
        if ("DOORBELL".equals(name)) return com.codename1.home.AccessoryCategory.DOORBELL;
        if ("FAN".equals(name)) return com.codename1.home.AccessoryCategory.FAN;
        if ("GARAGE_DOOR_OPENER".equals(name)) return com.codename1.home.AccessoryCategory.GARAGE_DOOR_OPENER;
        if ("LIGHT".equals(name)) return com.codename1.home.AccessoryCategory.LIGHT;
        if ("LOCK".equals(name)) return com.codename1.home.AccessoryCategory.LOCK;
        if ("OTHER".equals(name)) return com.codename1.home.AccessoryCategory.OTHER;
        if ("OUTLET".equals(name)) return com.codename1.home.AccessoryCategory.OUTLET;
        if ("SECURITY_SYSTEM".equals(name)) return com.codename1.home.AccessoryCategory.SECURITY_SYSTEM;
        if ("SENSOR".equals(name)) return com.codename1.home.AccessoryCategory.SENSOR;
        if ("SPEAKER".equals(name)) return com.codename1.home.AccessoryCategory.SPEAKER;
        if ("SWITCH".equals(name)) return com.codename1.home.AccessoryCategory.SWITCH;
        if ("TELEVISION".equals(name)) return com.codename1.home.AccessoryCategory.TELEVISION;
        if ("THERMOSTAT".equals(name)) return com.codename1.home.AccessoryCategory.THERMOSTAT;
        if ("WINDOW_COVERING".equals(name)) return com.codename1.home.AccessoryCategory.WINDOW_COVERING;
        throw unsupportedStaticField(com.codename1.home.AccessoryCategory.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("EXTREMELY_POOR".equals(name)) return com.codename1.home.AirQualityLevel.EXTREMELY_POOR;
        if ("FAIR".equals(name)) return com.codename1.home.AirQualityLevel.FAIR;
        if ("GOOD".equals(name)) return com.codename1.home.AirQualityLevel.GOOD;
        if ("MODERATE".equals(name)) return com.codename1.home.AirQualityLevel.MODERATE;
        if ("POOR".equals(name)) return com.codename1.home.AirQualityLevel.POOR;
        if ("UNKNOWN".equals(name)) return com.codename1.home.AirQualityLevel.UNKNOWN;
        if ("VERY_POOR".equals(name)) return com.codename1.home.AirQualityLevel.VERY_POOR;
        throw unsupportedStaticField(com.codename1.home.AirQualityLevel.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("CRITICAL".equals(name)) return com.codename1.home.AlarmState.CRITICAL;
        if ("NORMAL".equals(name)) return com.codename1.home.AlarmState.NORMAL;
        if ("UNKNOWN".equals(name)) return com.codename1.home.AlarmState.UNKNOWN;
        if ("WARNING".equals(name)) return com.codename1.home.AlarmState.WARNING;
        throw unsupportedStaticField(com.codename1.home.AlarmState.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("CHARGING".equals(name)) return com.codename1.home.ChargingState.CHARGING;
        if ("FULL".equals(name)) return com.codename1.home.ChargingState.FULL;
        if ("NOT_CHARGEABLE".equals(name)) return com.codename1.home.ChargingState.NOT_CHARGEABLE;
        if ("NOT_CHARGING".equals(name)) return com.codename1.home.ChargingState.NOT_CHARGING;
        if ("UNKNOWN".equals(name)) return com.codename1.home.ChargingState.UNKNOWN;
        throw unsupportedStaticField(com.codename1.home.ChargingState.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("CLOSED".equals(name)) return com.codename1.home.DoorState.CLOSED;
        if ("CLOSING".equals(name)) return com.codename1.home.DoorState.CLOSING;
        if ("OPEN".equals(name)) return com.codename1.home.DoorState.OPEN;
        if ("OPENING".equals(name)) return com.codename1.home.DoorState.OPENING;
        if ("STOPPED".equals(name)) return com.codename1.home.DoorState.STOPPED;
        if ("UNKNOWN".equals(name)) return com.codename1.home.DoorState.UNKNOWN;
        throw unsupportedStaticField(com.codename1.home.DoorState.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.home.FanMode.AUTO;
        if ("HIGH".equals(name)) return com.codename1.home.FanMode.HIGH;
        if ("LOW".equals(name)) return com.codename1.home.FanMode.LOW;
        if ("MEDIUM".equals(name)) return com.codename1.home.FanMode.MEDIUM;
        if ("OFF".equals(name)) return com.codename1.home.FanMode.OFF;
        if ("ON".equals(name)) return com.codename1.home.FanMode.ON;
        if ("SMART".equals(name)) return com.codename1.home.FanMode.SMART;
        throw unsupportedStaticField(com.codename1.home.FanMode.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.home.HeatingCoolingMode.AUTO;
        if ("COOL".equals(name)) return com.codename1.home.HeatingCoolingMode.COOL;
        if ("HEAT".equals(name)) return com.codename1.home.HeatingCoolingMode.HEAT;
        if ("OFF".equals(name)) return com.codename1.home.HeatingCoolingMode.OFF;
        if ("OTHER".equals(name)) return com.codename1.home.HeatingCoolingMode.OTHER;
        throw unsupportedStaticField(com.codename1.home.HeatingCoolingMode.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("AUTHORIZED".equals(name)) return com.codename1.home.HomeAuthorizationStatus.AUTHORIZED;
        if ("DENIED".equals(name)) return com.codename1.home.HomeAuthorizationStatus.DENIED;
        if ("NOT_DETERMINED".equals(name)) return com.codename1.home.HomeAuthorizationStatus.NOT_DETERMINED;
        if ("RESTRICTED".equals(name)) return com.codename1.home.HomeAuthorizationStatus.RESTRICTED;
        if ("UNKNOWN".equals(name)) return com.codename1.home.HomeAuthorizationStatus.UNKNOWN;
        throw unsupportedStaticField(com.codename1.home.HomeAuthorizationStatus.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("AVAILABLE".equals(name)) return com.codename1.home.HomeAvailability.AVAILABLE;
        if ("COMMISSIONING_ONLY".equals(name)) return com.codename1.home.HomeAvailability.COMMISSIONING_ONLY;
        if ("LOCAL_ONLY".equals(name)) return com.codename1.home.HomeAvailability.LOCAL_ONLY;
        if ("NOT_CONFIGURED".equals(name)) return com.codename1.home.HomeAvailability.NOT_CONFIGURED;
        if ("NOT_STARTED".equals(name)) return com.codename1.home.HomeAvailability.NOT_STARTED;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.home.HomeAvailability.NOT_SUPPORTED;
        if ("PERMISSION_DENIED".equals(name)) return com.codename1.home.HomeAvailability.PERMISSION_DENIED;
        if ("PERMISSION_REQUIRED".equals(name)) return com.codename1.home.HomeAvailability.PERMISSION_REQUIRED;
        if ("PROVIDER_NOT_INSTALLED".equals(name)) return com.codename1.home.HomeAvailability.PROVIDER_NOT_INSTALLED;
        if ("PROVIDER_UPDATE_REQUIRED".equals(name)) return com.codename1.home.HomeAvailability.PROVIDER_UPDATE_REQUIRED;
        if ("RESTRICTED".equals(name)) return com.codename1.home.HomeAvailability.RESTRICTED;
        if ("SIGN_IN_REQUIRED".equals(name)) return com.codename1.home.HomeAvailability.SIGN_IN_REQUIRED;
        throw unsupportedStaticField(com.codename1.home.HomeAvailability.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("GOOGLE_HOME".equals(name)) return com.codename1.home.HomeBackend.GOOGLE_HOME;
        if ("HOMEKIT".equals(name)) return com.codename1.home.HomeBackend.HOMEKIT;
        if ("LOCAL".equals(name)) return com.codename1.home.HomeBackend.LOCAL;
        if ("MATTER_COMMISSIONING_ONLY".equals(name)) return com.codename1.home.HomeBackend.MATTER_COMMISSIONING_ONLY;
        if ("NONE".equals(name)) return com.codename1.home.HomeBackend.NONE;
        throw unsupportedStaticField(com.codename1.home.HomeBackend.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
        if ("ACCESSORY_NOT_FOUND".equals(name)) return com.codename1.home.HomeError.ACCESSORY_NOT_FOUND;
        if ("ACCESSORY_UNREACHABLE".equals(name)) return com.codename1.home.HomeError.ACCESSORY_UNREACHABLE;
        if ("AUTHORIZATION_REQUIRED".equals(name)) return com.codename1.home.HomeError.AUTHORIZATION_REQUIRED;
        if ("BUSY".equals(name)) return com.codename1.home.HomeError.BUSY;
        if ("COMMISSIONING_FAILED".equals(name)) return com.codename1.home.HomeError.COMMISSIONING_FAILED;
        if ("COMMISSIONING_UNAVAILABLE".equals(name)) return com.codename1.home.HomeError.COMMISSIONING_UNAVAILABLE;
        if ("ECOSYSTEM_APP_MISSING".equals(name)) return com.codename1.home.HomeError.ECOSYSTEM_APP_MISSING;
        if ("INVALID_ARGUMENT".equals(name)) return com.codename1.home.HomeError.INVALID_ARGUMENT;
        if ("INVALID_DATA".equals(name)) return com.codename1.home.HomeError.INVALID_DATA;
        if ("NOT_CONFIGURED".equals(name)) return com.codename1.home.HomeError.NOT_CONFIGURED;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.home.HomeError.NOT_SUPPORTED;
        if ("PIN_REJECTED".equals(name)) return com.codename1.home.HomeError.PIN_REJECTED;
        if ("PIN_REQUIRED".equals(name)) return com.codename1.home.HomeError.PIN_REQUIRED;
        if ("PROVIDER_UNAVAILABLE".equals(name)) return com.codename1.home.HomeError.PROVIDER_UNAVAILABLE;
        if ("PROVIDER_UPDATE_REQUIRED".equals(name)) return com.codename1.home.HomeError.PROVIDER_UPDATE_REQUIRED;
        if ("RATE_LIMITED".equals(name)) return com.codename1.home.HomeError.RATE_LIMITED;
        if ("READ_ONLY_TRAIT".equals(name)) return com.codename1.home.HomeError.READ_ONLY_TRAIT;
        if ("RESTRICTED".equals(name)) return com.codename1.home.HomeError.RESTRICTED;
        if ("SIGN_IN_REQUIRED".equals(name)) return com.codename1.home.HomeError.SIGN_IN_REQUIRED;
        if ("TIMEOUT".equals(name)) return com.codename1.home.HomeError.TIMEOUT;
        if ("TRAIT_NOT_SUPPORTED".equals(name)) return com.codename1.home.HomeError.TRAIT_NOT_SUPPORTED;
        if ("UNAUTHORIZED".equals(name)) return com.codename1.home.HomeError.UNAUTHORIZED;
        if ("UNIT_MISMATCH".equals(name)) return com.codename1.home.HomeError.UNIT_MISMATCH;
        if ("UNKNOWN".equals(name)) return com.codename1.home.HomeError.UNKNOWN;
        if ("USER_CANCELED".equals(name)) return com.codename1.home.HomeError.USER_CANCELED;
        if ("VALUE_OUT_OF_RANGE".equals(name)) return com.codename1.home.HomeError.VALUE_OUT_OF_RANGE;
        if ("WRITE_ONLY_TRAIT".equals(name)) return com.codename1.home.HomeError.WRITE_ONLY_TRAIT;
        throw unsupportedStaticField(com.codename1.home.HomeError.class, name);
    }

    private static Object getStaticField11(String name) throws Exception {
        if ("JAMMED".equals(name)) return com.codename1.home.LockState.JAMMED;
        if ("PARTIALLY_LOCKED".equals(name)) return com.codename1.home.LockState.PARTIALLY_LOCKED;
        if ("SECURED".equals(name)) return com.codename1.home.LockState.SECURED;
        if ("UNKNOWN".equals(name)) return com.codename1.home.LockState.UNKNOWN;
        if ("UNSECURED".equals(name)) return com.codename1.home.LockState.UNSECURED;
        throw unsupportedStaticField(com.codename1.home.LockState.class, name);
    }

    private static Object getStaticField12(String name) throws Exception {
        if ("CLOSING".equals(name)) return com.codename1.home.PositionState.CLOSING;
        if ("OPENING".equals(name)) return com.codename1.home.PositionState.OPENING;
        if ("STOPPED".equals(name)) return com.codename1.home.PositionState.STOPPED;
        if ("UNKNOWN".equals(name)) return com.codename1.home.PositionState.UNKNOWN;
        throw unsupportedStaticField(com.codename1.home.PositionState.class, name);
    }

    private static Object getStaticField13(String name) throws Exception {
        if ("ARRIVAL".equals(name)) return com.codename1.home.SceneType.ARRIVAL;
        if ("DEPARTURE".equals(name)) return com.codename1.home.SceneType.DEPARTURE;
        if ("SLEEP".equals(name)) return com.codename1.home.SceneType.SLEEP;
        if ("TRIGGER_OWNED".equals(name)) return com.codename1.home.SceneType.TRIGGER_OWNED;
        if ("USER_DEFINED".equals(name)) return com.codename1.home.SceneType.USER_DEFINED;
        if ("WAKE_UP".equals(name)) return com.codename1.home.SceneType.WAKE_UP;
        throw unsupportedStaticField(com.codename1.home.SceneType.class, name);
    }

    private static Object getStaticField14(String name) throws Exception {
        if ("AIR_PURIFIER".equals(name)) return com.codename1.home.ServiceType.AIR_PURIFIER;
        if ("AIR_QUALITY_SENSOR".equals(name)) return com.codename1.home.ServiceType.AIR_QUALITY_SENSOR;
        if ("BATTERY".equals(name)) return com.codename1.home.ServiceType.BATTERY;
        if ("CARBON_MONOXIDE_SENSOR".equals(name)) return com.codename1.home.ServiceType.CARBON_MONOXIDE_SENSOR;
        if ("CONTACT_SENSOR".equals(name)) return com.codename1.home.ServiceType.CONTACT_SENSOR;
        if ("DOOR".equals(name)) return com.codename1.home.ServiceType.DOOR;
        if ("FAN".equals(name)) return com.codename1.home.ServiceType.FAN;
        if ("GARAGE_DOOR_OPENER".equals(name)) return com.codename1.home.ServiceType.GARAGE_DOOR_OPENER;
        if ("HUMIDITY_SENSOR".equals(name)) return com.codename1.home.ServiceType.HUMIDITY_SENSOR;
        if ("LEAK_SENSOR".equals(name)) return com.codename1.home.ServiceType.LEAK_SENSOR;
        if ("LIGHTBULB".equals(name)) return com.codename1.home.ServiceType.LIGHTBULB;
        if ("LIGHT_SENSOR".equals(name)) return com.codename1.home.ServiceType.LIGHT_SENSOR;
        if ("LOCK_MECHANISM".equals(name)) return com.codename1.home.ServiceType.LOCK_MECHANISM;
        if ("MOTION_SENSOR".equals(name)) return com.codename1.home.ServiceType.MOTION_SENSOR;
        if ("OCCUPANCY_SENSOR".equals(name)) return com.codename1.home.ServiceType.OCCUPANCY_SENSOR;
        if ("OTHER".equals(name)) return com.codename1.home.ServiceType.OTHER;
        if ("OUTLET".equals(name)) return com.codename1.home.ServiceType.OUTLET;
        if ("SMOKE_SENSOR".equals(name)) return com.codename1.home.ServiceType.SMOKE_SENSOR;
        if ("SPEAKER".equals(name)) return com.codename1.home.ServiceType.SPEAKER;
        if ("SWITCH".equals(name)) return com.codename1.home.ServiceType.SWITCH;
        if ("TEMPERATURE_SENSOR".equals(name)) return com.codename1.home.ServiceType.TEMPERATURE_SENSOR;
        if ("THERMOSTAT".equals(name)) return com.codename1.home.ServiceType.THERMOSTAT;
        if ("WINDOW_COVERING".equals(name)) return com.codename1.home.ServiceType.WINDOW_COVERING;
        throw unsupportedStaticField(com.codename1.home.ServiceType.class, name);
    }

    private static Object getStaticField15(String name) throws Exception {
        if ("ACCESSORY_ADDED".equals(name)) return com.codename1.home.StructureChangeKind.ACCESSORY_ADDED;
        if ("ACCESSORY_MOVED".equals(name)) return com.codename1.home.StructureChangeKind.ACCESSORY_MOVED;
        if ("ACCESSORY_REMOVED".equals(name)) return com.codename1.home.StructureChangeKind.ACCESSORY_REMOVED;
        if ("ACCESSORY_RENAMED".equals(name)) return com.codename1.home.StructureChangeKind.ACCESSORY_RENAMED;
        if ("AVAILABILITY_CHANGED".equals(name)) return com.codename1.home.StructureChangeKind.AVAILABILITY_CHANGED;
        if ("REACHABILITY_CHANGED".equals(name)) return com.codename1.home.StructureChangeKind.REACHABILITY_CHANGED;
        if ("SCENES_CHANGED".equals(name)) return com.codename1.home.StructureChangeKind.SCENES_CHANGED;
        if ("STRUCTURES_CHANGED".equals(name)) return com.codename1.home.StructureChangeKind.STRUCTURES_CHANGED;
        throw unsupportedStaticField(com.codename1.home.StructureChangeKind.class, name);
    }

    private static Object getStaticField16(String name) throws Exception {
        if ("DEFAULT_MIN_INTERVAL_MILLIS".equals(name)) return com.codename1.home.SubscriptionRequest.DEFAULT_MIN_INTERVAL_MILLIS;
        throw unsupportedStaticField(com.codename1.home.SubscriptionRequest.class, name);
    }

    private static Object getStaticField17(String name) throws Exception {
        if ("AIR_QUALITY".equals(name)) return com.codename1.home.Trait.AIR_QUALITY;
        if ("BATTERY_CHARGING".equals(name)) return com.codename1.home.Trait.BATTERY_CHARGING;
        if ("BATTERY_LEVEL".equals(name)) return com.codename1.home.Trait.BATTERY_LEVEL;
        if ("BATTERY_LOW".equals(name)) return com.codename1.home.Trait.BATTERY_LOW;
        if ("BRIGHTNESS".equals(name)) return com.codename1.home.Trait.BRIGHTNESS;
        if ("CO2_LEVEL".equals(name)) return com.codename1.home.Trait.CO2_LEVEL;
        if ("COLOR_TEMPERATURE".equals(name)) return com.codename1.home.Trait.COLOR_TEMPERATURE;
        if ("CONTACT_DETECTED".equals(name)) return com.codename1.home.Trait.CONTACT_DETECTED;
        if ("COVERING_MOTION".equals(name)) return com.codename1.home.Trait.COVERING_MOTION;
        if ("COVERING_POSITION".equals(name)) return com.codename1.home.Trait.COVERING_POSITION;
        if ("COVERING_TILT".equals(name)) return com.codename1.home.Trait.COVERING_TILT;
        if ("CO_DETECTED".equals(name)) return com.codename1.home.Trait.CO_DETECTED;
        if ("CO_LEVEL".equals(name)) return com.codename1.home.Trait.CO_LEVEL;
        if ("CURRENT_HEATING_COOLING".equals(name)) return com.codename1.home.Trait.CURRENT_HEATING_COOLING;
        if ("CURRENT_HUMIDITY".equals(name)) return com.codename1.home.Trait.CURRENT_HUMIDITY;
        if ("CURRENT_LIGHT_LEVEL".equals(name)) return com.codename1.home.Trait.CURRENT_LIGHT_LEVEL;
        if ("CURRENT_TEMPERATURE".equals(name)) return com.codename1.home.Trait.CURRENT_TEMPERATURE;
        if ("DOOR_STATE".equals(name)) return com.codename1.home.Trait.DOOR_STATE;
        if ("FAN_MODE".equals(name)) return com.codename1.home.Trait.FAN_MODE;
        if ("FAN_SPEED".equals(name)) return com.codename1.home.Trait.FAN_SPEED;
        if ("HUE".equals(name)) return com.codename1.home.Trait.HUE;
        if ("LEAK_DETECTED".equals(name)) return com.codename1.home.Trait.LEAK_DETECTED;
        if ("LOCK_STATE".equals(name)) return com.codename1.home.Trait.LOCK_STATE;
        if ("MOTION_DETECTED".equals(name)) return com.codename1.home.Trait.MOTION_DETECTED;
        if ("MUTE".equals(name)) return com.codename1.home.Trait.MUTE;
        if ("OBSTRUCTION_DETECTED".equals(name)) return com.codename1.home.Trait.OBSTRUCTION_DETECTED;
        if ("OCCUPANCY_DETECTED".equals(name)) return com.codename1.home.Trait.OCCUPANCY_DETECTED;
        if ("ON_OFF".equals(name)) return com.codename1.home.Trait.ON_OFF;
        if ("OUTLET_IN_USE".equals(name)) return com.codename1.home.Trait.OUTLET_IN_USE;
        if ("PM10_DENSITY".equals(name)) return com.codename1.home.Trait.PM10_DENSITY;
        if ("PM2_5_DENSITY".equals(name)) return com.codename1.home.Trait.PM2_5_DENSITY;
        if ("SATURATION".equals(name)) return com.codename1.home.Trait.SATURATION;
        if ("SMOKE_DETECTED".equals(name)) return com.codename1.home.Trait.SMOKE_DETECTED;
        if ("TARGET_COOLING_TEMPERATURE".equals(name)) return com.codename1.home.Trait.TARGET_COOLING_TEMPERATURE;
        if ("TARGET_COVERING_POSITION".equals(name)) return com.codename1.home.Trait.TARGET_COVERING_POSITION;
        if ("TARGET_COVERING_TILT".equals(name)) return com.codename1.home.Trait.TARGET_COVERING_TILT;
        if ("TARGET_DOOR_STATE".equals(name)) return com.codename1.home.Trait.TARGET_DOOR_STATE;
        if ("TARGET_HEATING_COOLING".equals(name)) return com.codename1.home.Trait.TARGET_HEATING_COOLING;
        if ("TARGET_HEATING_TEMPERATURE".equals(name)) return com.codename1.home.Trait.TARGET_HEATING_TEMPERATURE;
        if ("TARGET_HUMIDITY".equals(name)) return com.codename1.home.Trait.TARGET_HUMIDITY;
        if ("TARGET_LOCK_STATE".equals(name)) return com.codename1.home.Trait.TARGET_LOCK_STATE;
        if ("TARGET_TEMPERATURE".equals(name)) return com.codename1.home.Trait.TARGET_TEMPERATURE;
        if ("VOC_DENSITY".equals(name)) return com.codename1.home.Trait.VOC_DENSITY;
        if ("VOLUME".equals(name)) return com.codename1.home.Trait.VOLUME;
        throw unsupportedStaticField(com.codename1.home.Trait.class, name);
    }

    private static Object getStaticField18(String name) throws Exception {
        if ("ARC_DEGREE".equals(name)) return com.codename1.home.TraitUnit.ARC_DEGREE;
        if ("CELSIUS".equals(name)) return com.codename1.home.TraitUnit.CELSIUS;
        if ("FAHRENHEIT".equals(name)) return com.codename1.home.TraitUnit.FAHRENHEIT;
        if ("LUX".equals(name)) return com.codename1.home.TraitUnit.LUX;
        if ("MICROGRAM_PER_CUBIC_METER".equals(name)) return com.codename1.home.TraitUnit.MICROGRAM_PER_CUBIC_METER;
        if ("MIRED".equals(name)) return com.codename1.home.TraitUnit.MIRED;
        if ("NONE".equals(name)) return com.codename1.home.TraitUnit.NONE;
        if ("PERCENT".equals(name)) return com.codename1.home.TraitUnit.PERCENT;
        if ("PPB".equals(name)) return com.codename1.home.TraitUnit.PPB;
        if ("PPM".equals(name)) return com.codename1.home.TraitUnit.PPM;
        throw unsupportedStaticField(com.codename1.home.TraitUnit.class, name);
    }

    private static Object getStaticField19(String name) throws Exception {
        if ("ANGLE".equals(name)) return com.codename1.home.TraitUnitDimension.ANGLE;
        if ("COLOR_TEMPERATURE".equals(name)) return com.codename1.home.TraitUnitDimension.COLOR_TEMPERATURE;
        if ("CONCENTRATION_MASS".equals(name)) return com.codename1.home.TraitUnitDimension.CONCENTRATION_MASS;
        if ("CONCENTRATION_PARTS".equals(name)) return com.codename1.home.TraitUnitDimension.CONCENTRATION_PARTS;
        if ("DIMENSIONLESS".equals(name)) return com.codename1.home.TraitUnitDimension.DIMENSIONLESS;
        if ("ILLUMINANCE".equals(name)) return com.codename1.home.TraitUnitDimension.ILLUMINANCE;
        if ("RATIO".equals(name)) return com.codename1.home.TraitUnitDimension.RATIO;
        if ("TEMPERATURE".equals(name)) return com.codename1.home.TraitUnitDimension.TEMPERATURE;
        throw unsupportedStaticField(com.codename1.home.TraitUnitDimension.class, name);
    }

    private static Object getStaticField20(String name) throws Exception {
        if ("BOOLEAN".equals(name)) return com.codename1.home.TraitValueKind.BOOLEAN;
        if ("DOUBLE".equals(name)) return com.codename1.home.TraitValueKind.DOUBLE;
        if ("ENUM".equals(name)) return com.codename1.home.TraitValueKind.ENUM;
        if ("INT".equals(name)) return com.codename1.home.TraitValueKind.INT;
        if ("STRING".equals(name)) return com.codename1.home.TraitValueKind.STRING;
        throw unsupportedStaticField(com.codename1.home.TraitValueKind.class, name);
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
