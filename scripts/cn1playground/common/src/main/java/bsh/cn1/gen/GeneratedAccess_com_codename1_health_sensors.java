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

public final class GeneratedAccess_com_codename1_health_sensors {
    private GeneratedAccess_com_codename1_health_sensors() {
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
        if ("BloodPressureMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.BloodPressureMeasurement.class;
        }
        if ("BodySensorLocation".equals(simpleName)) {
            return com.codename1.health.sensors.BodySensorLocation.class;
        }
        if ("CscMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.CscMeasurement.class;
        }
        if ("CyclingPowerMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.CyclingPowerMeasurement.class;
        }
        if ("GlucoseMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.GlucoseMeasurement.class;
        }
        if ("GlucoseRecordFilter".equals(simpleName)) {
            return com.codename1.health.sensors.GlucoseRecordFilter.class;
        }
        if ("Kind".equals(simpleName)) {
            return com.codename1.health.sensors.GlucoseRecordFilter.Kind.class;
        }
        if ("HealthSensor".equals(simpleName)) {
            return com.codename1.health.sensors.HealthSensor.class;
        }
        if ("HealthSensorProfile".equals(simpleName)) {
            return com.codename1.health.sensors.HealthSensorProfile.class;
        }
        if ("HealthSensors".equals(simpleName)) {
            return com.codename1.health.sensors.HealthSensors.class;
        }
        if ("HeartRateMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.HeartRateMeasurement.class;
        }
        if ("RscMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.RscMeasurement.class;
        }
        if ("SensorDiscoveryListener".equals(simpleName)) {
            return com.codename1.health.sensors.SensorDiscoveryListener.class;
        }
        if ("SensorSampleListener".equals(simpleName)) {
            return com.codename1.health.sensors.SensorSampleListener.class;
        }
        if ("SensorScan".equals(simpleName)) {
            return com.codename1.health.sensors.SensorScan.class;
        }
        if ("SensorScanSettings".equals(simpleName)) {
            return com.codename1.health.sensors.SensorScanSettings.class;
        }
        if ("SensorSession".equals(simpleName)) {
            return com.codename1.health.sensors.SensorSession.class;
        }
        if ("SensorSessionOptions".equals(simpleName)) {
            return com.codename1.health.sensors.SensorSessionOptions.class;
        }
        if ("SensorSessionState".equals(simpleName)) {
            return com.codename1.health.sensors.SensorSessionState.class;
        }
        if ("TemperatureMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.TemperatureMeasurement.class;
        }
        if ("WeightMeasurement".equals(simpleName)) {
            return com.codename1.health.sensors.WeightMeasurement.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.health.sensors.HealthSensors.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.health.sensors.HealthSensors();
            }
        }
        if (type == com.codename1.health.sensors.SensorScanSettings.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.health.sensors.SensorScanSettings();
            }
        }
        if (type == com.codename1.health.sensors.SensorSessionOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.health.sensors.SensorSessionOptions();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.health.sensors.BloodPressureMeasurement.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.health.sensors.BodySensorLocation.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.health.sensors.CscMeasurement.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.health.sensors.CyclingPowerMeasurement.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.health.sensors.GlucoseMeasurement.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.health.sensors.GlucoseRecordFilter.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.health.sensors.HealthSensorProfile.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.health.sensors.HeartRateMeasurement.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.health.sensors.RscMeasurement.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.health.sensors.TemperatureMeasurement.class) return invokeStatic9(name, safeArgs);
        if (type == com.codename1.health.sensors.WeightMeasurement.class) return invokeStatic10(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.BloodPressureMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.BloodPressureMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("describe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.sensors.BodySensorLocation.describe(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isDefined".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.sensors.BodySensorLocation.isDefined(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.BodySensorLocation.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.CscMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.CscMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.CyclingPowerMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.CyclingPowerMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.GlucoseMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.GlucoseMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("all".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.sensors.GlucoseRecordFilter.all();
            }
        }
        if ("first".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.sensors.GlucoseRecordFilter.first();
            }
        }
        if ("last".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.sensors.GlucoseRecordFilter.last();
            }
        }
        if ("sequenceRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.health.sensors.GlucoseRecordFilter.sequenceRange(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("sinceSequence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.sensors.GlucoseRecordFilter.sinceSequence(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.GlucoseRecordFilter.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.sensors.HealthSensorProfile.values();
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.HealthSensorProfile.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.HeartRateMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.HeartRateMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.RscMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.RscMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.TemperatureMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.TemperatureMeasurement.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.health.sensors.WeightMeasurement.parse((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.sensors.WeightMeasurement.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.health.sensors.BloodPressureMeasurement) {
            try {
                return invoke0((com.codename1.health.sensors.BloodPressureMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.CscMeasurement) {
            try {
                return invoke1((com.codename1.health.sensors.CscMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.CyclingPowerMeasurement) {
            try {
                return invoke2((com.codename1.health.sensors.CyclingPowerMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.GlucoseMeasurement) {
            try {
                return invoke3((com.codename1.health.sensors.GlucoseMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.GlucoseRecordFilter) {
            try {
                return invoke4((com.codename1.health.sensors.GlucoseRecordFilter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.HealthSensor) {
            try {
                return invoke5((com.codename1.health.sensors.HealthSensor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.HealthSensorProfile) {
            try {
                return invoke6((com.codename1.health.sensors.HealthSensorProfile) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.HealthSensors) {
            try {
                return invoke7((com.codename1.health.sensors.HealthSensors) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.HeartRateMeasurement) {
            try {
                return invoke8((com.codename1.health.sensors.HeartRateMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.RscMeasurement) {
            try {
                return invoke9((com.codename1.health.sensors.RscMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.SensorScan) {
            try {
                return invoke10((com.codename1.health.sensors.SensorScan) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.SensorScanSettings) {
            try {
                return invoke11((com.codename1.health.sensors.SensorScanSettings) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.SensorSession) {
            try {
                return invoke12((com.codename1.health.sensors.SensorSession) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.SensorSessionOptions) {
            try {
                return invoke13((com.codename1.health.sensors.SensorSessionOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.TemperatureMeasurement) {
            try {
                return invoke14((com.codename1.health.sensors.TemperatureMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.WeightMeasurement) {
            try {
                return invoke15((com.codename1.health.sensors.WeightMeasurement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.SensorDiscoveryListener) {
            try {
                return invoke16((com.codename1.health.sensors.SensorDiscoveryListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.sensors.SensorSampleListener) {
            try {
                return invoke17((com.codename1.health.sensors.SensorSampleListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.health.sensors.BloodPressureMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDiastolicMmHg".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDiastolicMmHg();
            }
        }
        if ("getMeanArterialMmHg".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMeanArterialMmHg();
            }
        }
        if ("getPulseBpm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPulseBpm();
            }
        }
        if ("getSystolicMmHg".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSystolicMmHg();
            }
        }
        if ("getTimestampMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampMillis();
            }
        }
        if ("getUserId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserId();
            }
        }
        if ("hasPulse".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasPulse();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.health.sensors.CscMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCrankRevolutions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCrankRevolutions();
            }
        }
        if ("getLastCrankEventTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastCrankEventTime();
            }
        }
        if ("getLastWheelEventTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastWheelEventTime();
            }
        }
        if ("getWheelRevolutions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWheelRevolutions();
            }
        }
        if ("hasCrankData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasCrankData();
            }
        }
        if ("hasWheelData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasWheelData();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.health.sensors.CyclingPowerMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccumulatedEnergyKilojoules".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccumulatedEnergyKilojoules();
            }
        }
        if ("getCrankRevolutions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCrankRevolutions();
            }
        }
        if ("getInstantaneousPowerWatts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInstantaneousPowerWatts();
            }
        }
        if ("getLastCrankEventTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastCrankEventTime();
            }
        }
        if ("getLastWheelEventTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastWheelEventTime();
            }
        }
        if ("getPedalPowerBalancePercent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPedalPowerBalancePercent();
            }
        }
        if ("getWheelRevolutions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWheelRevolutions();
            }
        }
        if ("hasCrankData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasCrankData();
            }
        }
        if ("isPedalPowerBalanceLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPedalPowerBalanceLeft();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.health.sensors.GlucoseMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getMilligramsPerDeciliter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMilligramsPerDeciliter();
            }
        }
        if ("getMillimolesPerLiter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMillimolesPerLiter();
            }
        }
        if ("getSampleLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleLocation();
            }
        }
        if ("getSampleType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleType();
            }
        }
        if ("getSequenceNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSequenceNumber();
            }
        }
        if ("getTimestampMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampMillis();
            }
        }
        if ("hasConcentration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasConcentration();
            }
        }
        if ("isContextFollowing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isContextFollowing();
            }
        }
        if ("isControlSolution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isControlSolution();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.health.sensors.GlucoseRecordFilter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFromSequence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFromSequence();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getToSequence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToSequence();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.health.sensors.HealthSensor typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getProfiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProfiles();
            }
        }
        if ("getRssi".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRssi();
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensorProfile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensorProfile.class}, false);
                return typedTarget.supports((com.codename1.health.sensors.HealthSensorProfile) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.health.sensors.HealthSensorProfile typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getMeasurementUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMeasurementUuid();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getProducedTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProducedTypes();
            }
        }
        if ("getServiceUuid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getServiceUuid();
            }
        }
        if ("isStreaming".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStreaming();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.health.sensors.HealthSensors typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("connect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensor.class, com.codename1.health.sensors.HealthSensorProfile.class, com.codename1.health.sensors.SensorSessionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensor.class, com.codename1.health.sensors.HealthSensorProfile.class, com.codename1.health.sensors.SensorSessionOptions.class}, false);
                return typedTarget.connect((com.codename1.health.sensors.HealthSensor) adaptedArgs[0], (com.codename1.health.sensors.HealthSensorProfile) adaptedArgs[1], (com.codename1.health.sensors.SensorSessionOptions) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.health.sensors.HealthSensorProfile.class, com.codename1.health.sensors.SensorSessionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.health.sensors.HealthSensorProfile.class, com.codename1.health.sensors.SensorSessionOptions.class}, false);
                return typedTarget.connect((java.lang.String) adaptedArgs[0], (com.codename1.health.sensors.HealthSensorProfile) adaptedArgs[1], (com.codename1.health.sensors.SensorSessionOptions) adaptedArgs[2]);
            }
        }
        if ("getActiveSessions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActiveSessions();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("startScan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorScanSettings.class, com.codename1.health.sensors.SensorDiscoveryListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorScanSettings.class, com.codename1.health.sensors.SensorDiscoveryListener.class}, false);
                return typedTarget.startScan((com.codename1.health.sensors.SensorScanSettings) adaptedArgs[0], (com.codename1.health.sensors.SensorDiscoveryListener) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.health.sensors.HeartRateMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getEnergyExpendedKilocalories".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnergyExpendedKilocalories();
            }
        }
        if ("getEnergyExpendedKilojoules".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnergyExpendedKilojoules();
            }
        }
        if ("getHeartRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeartRate();
            }
        }
        if ("getRrIntervalCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRrIntervalCount();
            }
        }
        if ("getRrIntervalMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getRrIntervalMillis(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hasEnergyExpended".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasEnergyExpended();
            }
        }
        if ("isSensorContactDetected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSensorContactDetected();
            }
        }
        if ("isSensorContactSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSensorContactSupported();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.health.sensors.RscMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCadenceStepsPerMinute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCadenceStepsPerMinute();
            }
        }
        if ("getSpeedMetersPerSecond".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpeedMetersPerSecond();
            }
        }
        if ("getStrideLengthMeters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrideLengthMeters();
            }
        }
        if ("getStrideRatePerMinute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrideRatePerMinute();
            }
        }
        if ("getTotalDistanceMeters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalDistanceMeters();
            }
        }
        if ("isRunning".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRunning();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.health.sensors.SensorScan typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isScanning".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScanning();
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.health.sensors.SensorScanSettings typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addProfile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensorProfile.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensorProfile.class}, false);
                return typedTarget.addProfile((com.codename1.health.sensors.HealthSensorProfile) adaptedArgs[0]);
            }
        }
        if ("getProfiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProfiles();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getTimeoutMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeoutMillis();
            }
        }
        if ("isLowPower".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLowPower();
            }
        }
        if ("setLowPower".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setLowPower(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.setTimeout((java.time.Duration) adaptedArgs[0]);
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

    private static Object invoke12(com.codename1.health.sensors.SensorSession typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSampleListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSampleListener.class}, false);
                typedTarget.addListener((com.codename1.health.sensors.SensorSampleListener) adaptedArgs[0]); return null;
            }
        }
        if ("getBatteryPercent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBatteryPercent();
            }
        }
        if ("getBodySensorLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodySensorLocation();
            }
        }
        if ("getLatest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.getLatest((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getOptions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOptions();
            }
        }
        if ("getProfile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProfile();
            }
        }
        if ("getSensorId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSensorId();
            }
        }
        if ("getState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getState();
            }
        }
        if ("removeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSampleListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSampleListener.class}, false);
                typedTarget.removeListener((com.codename1.health.sensors.SensorSampleListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestStoredRecords".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.GlucoseRecordFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.GlucoseRecordFilter.class}, false);
                return typedTarget.requestStoredRecords((com.codename1.health.sensors.GlucoseRecordFilter) adaptedArgs[0]);
            }
        }
        if ("resetEnergyExpended".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resetEnergyExpended();
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

    private static Object invoke13(com.codename1.health.sensors.SensorSessionOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getStaleSample".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStaleSample();
            }
        }
        if ("getStaleSampleMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStaleSampleMillis();
            }
        }
        if ("getStoreBatch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStoreBatch();
            }
        }
        if ("getStoreBatchMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStoreBatchMillis();
            }
        }
        if ("getWorkoutSession".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWorkoutSession();
            }
        }
        if ("isAutoReconnect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoReconnect();
            }
        }
        if ("isWriteToStore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWriteToStore();
            }
        }
        if ("setAutoReconnect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setAutoReconnect(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setStaleSample".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.setStaleSample((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("setStaleSampleMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStaleSampleMillis(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setStoreBatch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.setStoreBatch((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("setStoreBatchMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setStoreBatchMillis(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setWorkoutSession".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.workout.WorkoutSession.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.workout.WorkoutSession.class}, false);
                return typedTarget.setWorkoutSession((com.codename1.health.workout.WorkoutSession) adaptedArgs[0]);
            }
        }
        if ("setWriteToStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setWriteToStore(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.health.sensors.TemperatureMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCelsius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCelsius();
            }
        }
        if ("getFahrenheit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFahrenheit();
            }
        }
        if ("getSite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSite();
            }
        }
        if ("getTimestampMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampMillis();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.health.sensors.WeightMeasurement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBmi".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBmi();
            }
        }
        if ("getHeightMeters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeightMeters();
            }
        }
        if ("getTimestampMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampMillis();
            }
        }
        if ("getUserId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserId();
            }
        }
        if ("getWeightKg".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeightKg();
            }
        }
        if ("hasBmiAndHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasBmiAndHeight();
            }
        }
        if ("isImperial".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isImperial();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.health.sensors.SensorDiscoveryListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("scanFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthException.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthException.class}, false);
                typedTarget.scanFailed((com.codename1.health.HealthException) adaptedArgs[0]); return null;
            }
        }
        if ("sensorDiscovered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.HealthSensor.class}, false);
                typedTarget.sensorDiscovered((com.codename1.health.sensors.HealthSensor) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.health.sensors.SensorSampleListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("sensorError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSession.class, com.codename1.health.HealthException.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSession.class, com.codename1.health.HealthException.class}, false);
                typedTarget.sensorError((com.codename1.health.sensors.SensorSession) adaptedArgs[0], (com.codename1.health.HealthException) adaptedArgs[1]); return null;
            }
        }
        if ("sensorSample".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSession.class, com.codename1.health.HealthSample.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSession.class, com.codename1.health.HealthSample.class}, false);
                typedTarget.sensorSample((com.codename1.health.sensors.SensorSession) adaptedArgs[0], (com.codename1.health.HealthSample) adaptedArgs[1]); return null;
            }
        }
        if ("sensorStateChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSession.class, com.codename1.health.sensors.SensorSessionState.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.sensors.SensorSession.class, com.codename1.health.sensors.SensorSessionState.class}, false);
                typedTarget.sensorStateChanged((com.codename1.health.sensors.SensorSession) adaptedArgs[0], (com.codename1.health.sensors.SensorSessionState) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.health.sensors.BodySensorLocation.class) return getStaticField0(name);
        if (type == com.codename1.health.sensors.GlucoseMeasurement.class) return getStaticField1(name);
        if (type == com.codename1.health.sensors.GlucoseRecordFilter.Kind.class) return getStaticField2(name);
        if (type == com.codename1.health.sensors.HealthSensorProfile.class) return getStaticField3(name);
        if (type == com.codename1.health.sensors.SensorSessionState.class) return getStaticField4(name);
        if (type == com.codename1.health.sensors.TemperatureMeasurement.class) return getStaticField5(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("CHEST".equals(name)) return com.codename1.health.sensors.BodySensorLocation.CHEST;
        if ("EAR_LOBE".equals(name)) return com.codename1.health.sensors.BodySensorLocation.EAR_LOBE;
        if ("FINGER".equals(name)) return com.codename1.health.sensors.BodySensorLocation.FINGER;
        if ("FOOT".equals(name)) return com.codename1.health.sensors.BodySensorLocation.FOOT;
        if ("HAND".equals(name)) return com.codename1.health.sensors.BodySensorLocation.HAND;
        if ("OTHER".equals(name)) return com.codename1.health.sensors.BodySensorLocation.OTHER;
        if ("WRIST".equals(name)) return com.codename1.health.sensors.BodySensorLocation.WRIST;
        throw unsupportedStaticField(com.codename1.health.sensors.BodySensorLocation.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("SAMPLE_LOCATION_ALTERNATE_SITE".equals(name)) return com.codename1.health.sensors.GlucoseMeasurement.SAMPLE_LOCATION_ALTERNATE_SITE;
        if ("SAMPLE_LOCATION_CONTROL_SOLUTION".equals(name)) return com.codename1.health.sensors.GlucoseMeasurement.SAMPLE_LOCATION_CONTROL_SOLUTION;
        if ("SAMPLE_LOCATION_EARLOBE".equals(name)) return com.codename1.health.sensors.GlucoseMeasurement.SAMPLE_LOCATION_EARLOBE;
        if ("SAMPLE_LOCATION_FINGER".equals(name)) return com.codename1.health.sensors.GlucoseMeasurement.SAMPLE_LOCATION_FINGER;
        if ("SAMPLE_LOCATION_UNKNOWN".equals(name)) return com.codename1.health.sensors.GlucoseMeasurement.SAMPLE_LOCATION_UNKNOWN;
        if ("TYPE_CONTROL_SOLUTION".equals(name)) return com.codename1.health.sensors.GlucoseMeasurement.TYPE_CONTROL_SOLUTION;
        throw unsupportedStaticField(com.codename1.health.sensors.GlucoseMeasurement.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("ALL".equals(name)) return com.codename1.health.sensors.GlucoseRecordFilter.Kind.ALL;
        if ("FIRST".equals(name)) return com.codename1.health.sensors.GlucoseRecordFilter.Kind.FIRST;
        if ("LAST".equals(name)) return com.codename1.health.sensors.GlucoseRecordFilter.Kind.LAST;
        if ("SEQUENCE_GREATER_OR_EQUAL".equals(name)) return com.codename1.health.sensors.GlucoseRecordFilter.Kind.SEQUENCE_GREATER_OR_EQUAL;
        if ("SEQUENCE_RANGE".equals(name)) return com.codename1.health.sensors.GlucoseRecordFilter.Kind.SEQUENCE_RANGE;
        throw unsupportedStaticField(com.codename1.health.sensors.GlucoseRecordFilter.Kind.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("BLOOD_PRESSURE".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.BLOOD_PRESSURE;
        if ("CYCLING_POWER".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.CYCLING_POWER;
        if ("CYCLING_SPEED_CADENCE".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.CYCLING_SPEED_CADENCE;
        if ("GLUCOSE".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.GLUCOSE;
        if ("HEALTH_THERMOMETER".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.HEALTH_THERMOMETER;
        if ("HEART_RATE".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.HEART_RATE;
        if ("RUNNING_SPEED_CADENCE".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.RUNNING_SPEED_CADENCE;
        if ("WEIGHT_SCALE".equals(name)) return com.codename1.health.sensors.HealthSensorProfile.WEIGHT_SCALE;
        throw unsupportedStaticField(com.codename1.health.sensors.HealthSensorProfile.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("CONNECTING".equals(name)) return com.codename1.health.sensors.SensorSessionState.CONNECTING;
        if ("FAILED".equals(name)) return com.codename1.health.sensors.SensorSessionState.FAILED;
        if ("RECONNECTING".equals(name)) return com.codename1.health.sensors.SensorSessionState.RECONNECTING;
        if ("STOPPED".equals(name)) return com.codename1.health.sensors.SensorSessionState.STOPPED;
        if ("STREAMING".equals(name)) return com.codename1.health.sensors.SensorSessionState.STREAMING;
        throw unsupportedStaticField(com.codename1.health.sensors.SensorSessionState.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("SITE_ARMPIT".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_ARMPIT;
        if ("SITE_BODY".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_BODY;
        if ("SITE_EAR".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_EAR;
        if ("SITE_FINGER".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_FINGER;
        if ("SITE_GASTROINTESTINAL".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_GASTROINTESTINAL;
        if ("SITE_MOUTH".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_MOUTH;
        if ("SITE_RECTUM".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_RECTUM;
        if ("SITE_TOE".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_TOE;
        if ("SITE_TYMPANUM".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_TYMPANUM;
        if ("SITE_UNKNOWN".equals(name)) return com.codename1.health.sensors.TemperatureMeasurement.SITE_UNKNOWN;
        throw unsupportedStaticField(com.codename1.health.sensors.TemperatureMeasurement.class, name);
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
