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

public final class GeneratedAccess_com_codename1_health {
    private GeneratedAccess_com_codename1_health() {
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
        if ("AggregateMetric".equals(simpleName)) {
            return com.codename1.health.AggregateMetric.class;
        }
        if ("AggregateQuery".equals(simpleName)) {
            return com.codename1.health.AggregateQuery.class;
        }
        if ("AggregateResult".equals(simpleName)) {
            return com.codename1.health.AggregateResult.class;
        }
        if ("BloodPressureSample".equals(simpleName)) {
            return com.codename1.health.BloodPressureSample.class;
        }
        if ("CategorySample".equals(simpleName)) {
            return com.codename1.health.CategorySample.class;
        }
        if ("Health".equals(simpleName)) {
            return com.codename1.health.Health.class;
        }
        if ("HealthAccess".equals(simpleName)) {
            return com.codename1.health.HealthAccess.class;
        }
        if ("HealthAggregationStyle".equals(simpleName)) {
            return com.codename1.health.HealthAggregationStyle.class;
        }
        if ("HealthAnchor".equals(simpleName)) {
            return com.codename1.health.HealthAnchor.class;
        }
        if ("HealthAuthorizationStatus".equals(simpleName)) {
            return com.codename1.health.HealthAuthorizationStatus.class;
        }
        if ("HealthAvailability".equals(simpleName)) {
            return com.codename1.health.HealthAvailability.class;
        }
        if ("HealthBackgroundListener".equals(simpleName)) {
            return com.codename1.health.HealthBackgroundListener.class;
        }
        if ("HealthBackgroundListenerFactory".equals(simpleName)) {
            return com.codename1.health.HealthBackgroundListenerFactory.class;
        }
        if ("HealthChangeBatch".equals(simpleName)) {
            return com.codename1.health.HealthChangeBatch.class;
        }
        if ("HealthChangeListener".equals(simpleName)) {
            return com.codename1.health.HealthChangeListener.class;
        }
        if ("HealthConfigurationException".equals(simpleName)) {
            return com.codename1.health.HealthConfigurationException.class;
        }
        if ("HealthDataKind".equals(simpleName)) {
            return com.codename1.health.HealthDataKind.class;
        }
        if ("HealthDataType".equals(simpleName)) {
            return com.codename1.health.HealthDataType.class;
        }
        if ("HealthDeleteRequest".equals(simpleName)) {
            return com.codename1.health.HealthDeleteRequest.class;
        }
        if ("HealthError".equals(simpleName)) {
            return com.codename1.health.HealthError.class;
        }
        if ("HealthException".equals(simpleName)) {
            return com.codename1.health.HealthException.class;
        }
        if ("HealthInterval".equals(simpleName)) {
            return com.codename1.health.HealthInterval.class;
        }
        if ("HealthQuantity".equals(simpleName)) {
            return com.codename1.health.HealthQuantity.class;
        }
        if ("HealthRequestStatus".equals(simpleName)) {
            return com.codename1.health.HealthRequestStatus.class;
        }
        if ("HealthSample".equals(simpleName)) {
            return com.codename1.health.HealthSample.class;
        }
        if ("HealthSource".equals(simpleName)) {
            return com.codename1.health.HealthSource.class;
        }
        if ("HealthStore".equals(simpleName)) {
            return com.codename1.health.HealthStore.class;
        }
        if ("HealthSubscription".equals(simpleName)) {
            return com.codename1.health.HealthSubscription.class;
        }
        if ("HealthTimeRange".equals(simpleName)) {
            return com.codename1.health.HealthTimeRange.class;
        }
        if ("HealthUnit".equals(simpleName)) {
            return com.codename1.health.HealthUnit.class;
        }
        if ("HealthUnitDimension".equals(simpleName)) {
            return com.codename1.health.HealthUnitDimension.class;
        }
        if ("HealthWriteResult".equals(simpleName)) {
            return com.codename1.health.HealthWriteResult.class;
        }
        if ("QuantitySample".equals(simpleName)) {
            return com.codename1.health.QuantitySample.class;
        }
        if ("RecordingMethod".equals(simpleName)) {
            return com.codename1.health.RecordingMethod.class;
        }
        if ("SamplePage".equals(simpleName)) {
            return com.codename1.health.SamplePage.class;
        }
        if ("SampleQuery".equals(simpleName)) {
            return com.codename1.health.SampleQuery.class;
        }
        if ("SeriesSample".equals(simpleName)) {
            return com.codename1.health.SeriesSample.class;
        }
        if ("SessionSample".equals(simpleName)) {
            return com.codename1.health.SessionSample.class;
        }
        if ("SleepSample".equals(simpleName)) {
            return com.codename1.health.SleepSample.class;
        }
        if ("SleepStage".equals(simpleName)) {
            return com.codename1.health.SleepStage.class;
        }
        if ("SleepStageInterval".equals(simpleName)) {
            return com.codename1.health.SleepStageInterval.class;
        }
        if ("SleepStageSupport".equals(simpleName)) {
            return com.codename1.health.SleepStageSupport.class;
        }
        if ("SubscriptionRequest".equals(simpleName)) {
            return com.codename1.health.SubscriptionRequest.class;
        }
        if ("WorkoutActivityType".equals(simpleName)) {
            return com.codename1.health.WorkoutActivityType.class;
        }
        if ("WorkoutSample".equals(simpleName)) {
            return com.codename1.health.WorkoutSample.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.health.AggregateQuery.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.health.AggregateQuery();
            }
        }
        if (type == com.codename1.health.AggregateResult.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return new com.codename1.health.AggregateResult(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if (type == com.codename1.health.HealthChangeBatch.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class, java.util.List.class, java.util.List.class, java.lang.Boolean.class, com.codename1.health.HealthAnchor.class, java.lang.Long.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class, java.util.List.class, java.util.List.class, java.lang.Boolean.class, com.codename1.health.HealthAnchor.class, java.lang.Long.class, java.lang.Boolean.class}, false);
                return new com.codename1.health.HealthChangeBatch((java.lang.String) adaptedArgs[0], (java.util.List) adaptedArgs[1], (java.util.List) adaptedArgs[2], (java.util.List) adaptedArgs[3], ((Boolean) adaptedArgs[4]).booleanValue(), (com.codename1.health.HealthAnchor) adaptedArgs[5], ((Number) adaptedArgs[6]).longValue(), ((Boolean) adaptedArgs[7]).booleanValue());
            }
        }
        if (type == com.codename1.health.HealthConfigurationException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.health.HealthConfigurationException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.health.HealthException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthError.class, java.lang.String.class}, false);
                return new com.codename1.health.HealthException((com.codename1.health.HealthError) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthError.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthError.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.health.HealthException((com.codename1.health.HealthError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.health.HealthQuantity.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.health.HealthUnit.class}, false);
                return new com.codename1.health.HealthQuantity(((Number) adaptedArgs[0]).doubleValue(), (com.codename1.health.HealthUnit) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.health.HealthSource.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.health.HealthSource((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.health.HealthWriteResult.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.health.HealthWriteResult();
            }
        }
        if (type == com.codename1.health.SamplePage.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.health.SamplePage((java.util.List) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if (type == com.codename1.health.SampleQuery.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.health.SampleQuery();
            }
        }
        if (type == com.codename1.health.SleepStageInterval.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SleepStageInterval.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SleepStageInterval.class}, false);
                return new com.codename1.health.SleepStageInterval((com.codename1.health.SleepStageInterval) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SleepStage.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SleepStage.class, java.lang.Long.class, java.lang.Long.class}, false);
                return new com.codename1.health.SleepStageInterval((com.codename1.health.SleepStage) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        if (type == com.codename1.health.SubscriptionRequest.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.health.SubscriptionRequest((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.health.AggregateQuery.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.health.BloodPressureSample.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.health.CategorySample.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.health.Health.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.health.HealthAccess.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.health.HealthAnchor.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.health.HealthDataType.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.health.HealthDeleteRequest.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.health.HealthInterval.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.health.HealthStore.class) return invokeStatic9(name, safeArgs);
        if (type == com.codename1.health.HealthTimeRange.class) return invokeStatic10(name, safeArgs);
        if (type == com.codename1.health.HealthUnit.class) return invokeStatic11(name, safeArgs);
        if (type == com.codename1.health.QuantitySample.class) return invokeStatic12(name, safeArgs);
        if (type == com.codename1.health.SeriesSample.class) return invokeStatic13(name, safeArgs);
        if (type == com.codename1.health.SleepSample.class) return invokeStatic14(name, safeArgs);
        if (type == com.codename1.health.WorkoutSample.class) return invokeStatic15(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("isMeaningful".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.AggregateMetric.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.AggregateMetric.class}, false);
                return com.codename1.health.AggregateQuery.isMeaningful((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.AggregateMetric) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.health.AggregateQuery.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Long.class}, false);
                return com.codename1.health.BloodPressureSample.create(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class, com.codename1.health.HealthQuantity.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class, com.codename1.health.HealthQuantity.class, java.lang.Long.class}, false);
                return com.codename1.health.BloodPressureSample.create((com.codename1.health.HealthQuantity) adaptedArgs[0], (com.codename1.health.HealthQuantity) adaptedArgs[1], ((Number) adaptedArgs[2]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.health.BloodPressureSample.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Integer.class, java.lang.Long.class}, false);
                return com.codename1.health.CategorySample.create((com.codename1.health.HealthDataType) adaptedArgs[0], toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.health.CategorySample.create((com.codename1.health.HealthDataType) adaptedArgs[0], toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).longValue(), ((Number) adaptedArgs[3]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.health.CategorySample.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.Health.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.health.Health.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return com.codename1.health.HealthAccess.read((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("readWrite".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return com.codename1.health.HealthAccess.readWrite((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return com.codename1.health.HealthAccess.write((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthAccess.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("fromStorableString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.health.HealthAnchor.fromStorableString((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.health.HealthAnchor.of((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthAnchor.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("forId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.health.HealthDataType.forId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.HealthDataType.values();
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthDataType.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("byId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.String.class}, false);
                return com.codename1.health.HealthDeleteRequest.byId((com.codename1.health.HealthDataType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("byIds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.util.List.class}, false);
                return com.codename1.health.HealthDeleteRequest.byIds((com.codename1.health.HealthDataType) adaptedArgs[0], (java.util.List) adaptedArgs[1]);
            }
        }
        if ("byRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthTimeRange.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthTimeRange.class}, false);
                return com.codename1.health.HealthDeleteRequest.byRange((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.HealthTimeRange) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthDeleteRequest.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("calendarDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class}, false);
                return com.codename1.health.HealthInterval.calendarDays(toIntValue(adaptedArgs[0]), (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("calendarMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class}, false);
                return com.codename1.health.HealthInterval.calendarMonths(toIntValue(adaptedArgs[0]), (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("calendarWeeks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class, java.lang.Integer.class}, false);
                return com.codename1.health.HealthInterval.calendarWeeks(toIntValue(adaptedArgs[0]), (java.time.ZoneId) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
        }
        if ("hours".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.HealthInterval.hours(toIntValue(adaptedArgs[0]));
            }
        }
        if ("millis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.health.HealthInterval.millis(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("minutes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.HealthInterval.minutes(toIntValue(adaptedArgs[0]));
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return com.codename1.health.HealthInterval.of((java.time.Duration) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthInterval.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("setBackgroundListenerFactory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthBackgroundListenerFactory.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthBackgroundListenerFactory.class}, false);
                com.codename1.health.HealthStore.setBackgroundListenerFactory((com.codename1.health.HealthBackgroundListenerFactory) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthStore.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("at".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.health.HealthTimeRange.at(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return com.codename1.health.HealthTimeRange.at((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("between".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.health.HealthTimeRange.between(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.Instant.class}, false);
                return com.codename1.health.HealthTimeRange.between((java.time.Instant) adaptedArgs[0], (java.time.Instant) adaptedArgs[1]);
            }
        }
        if ("calendarDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.time.ZoneId.class}, false);
                return com.codename1.health.HealthTimeRange.calendarDays(toIntValue(adaptedArgs[0]), (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("last".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return com.codename1.health.HealthTimeRange.last((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("lastDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.HealthTimeRange.lastDays(toIntValue(adaptedArgs[0]));
            }
        }
        if ("lastHours".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.health.HealthTimeRange.lastHours(toIntValue(adaptedArgs[0]));
            }
        }
        if ("lastMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.health.HealthTimeRange.lastMillis(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("since".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.health.HealthTimeRange.since(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return com.codename1.health.HealthTimeRange.since((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("today".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.ZoneId.class}, false);
                return com.codename1.health.HealthTimeRange.today((java.time.ZoneId) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthTimeRange.class, name, safeArgs);
    }

    private static Object invokeStatic11(String name, Object[] safeArgs) throws Exception {
        if ("convert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.health.HealthUnit.class, com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, com.codename1.health.HealthUnit.class, com.codename1.health.HealthUnit.class}, false);
                return com.codename1.health.HealthUnit.convert(((Number) adaptedArgs[0]).doubleValue(), (com.codename1.health.HealthUnit) adaptedArgs[1], (com.codename1.health.HealthUnit) adaptedArgs[2]);
            }
        }
        if ("forSymbol".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.health.HealthUnit.forSymbol((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.HealthUnit.values();
            }
        }
        throw unsupportedStatic(com.codename1.health.HealthUnit.class, name, safeArgs);
    }

    private static Object invokeStatic12(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthQuantity.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthQuantity.class, java.lang.Long.class}, false);
                return com.codename1.health.QuantitySample.create((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.HealthQuantity) adaptedArgs[1], ((Number) adaptedArgs[2]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthQuantity.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthQuantity.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.health.QuantitySample.create((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.HealthQuantity) adaptedArgs[1], ((Number) adaptedArgs[2]).longValue(), ((Number) adaptedArgs[3]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.health.QuantitySample.class, name, safeArgs);
    }

    private static Object invokeStatic13(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Long.class, java.lang.Long.class, long[].class, long[].class, double[].class, com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Long.class, java.lang.Long.class, long[].class, long[].class, double[].class, com.codename1.health.HealthUnit.class}, false);
                return com.codename1.health.SeriesSample.create((com.codename1.health.HealthDataType) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue(), (long[]) adaptedArgs[3], (long[]) adaptedArgs[4], (double[]) adaptedArgs[5], (com.codename1.health.HealthUnit) adaptedArgs[6]);
            }
        }
        throw unsupportedStatic(com.codename1.health.SeriesSample.class, name, safeArgs);
    }

    private static Object invokeStatic14(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.health.SleepSample.create(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.util.List.class}, false);
                return com.codename1.health.SleepSample.create(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue(), (java.util.List) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.health.SleepSample.class, name, safeArgs);
    }

    private static Object invokeStatic15(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.WorkoutActivityType.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.WorkoutActivityType.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.health.WorkoutSample.create((com.codename1.health.WorkoutActivityType) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.health.WorkoutSample.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.health.SleepSample) {
            try {
                return invoke0((com.codename1.health.SleepSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.WorkoutSample) {
            try {
                return invoke1((com.codename1.health.WorkoutSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.BloodPressureSample) {
            try {
                return invoke2((com.codename1.health.BloodPressureSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.CategorySample) {
            try {
                return invoke3((com.codename1.health.CategorySample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.QuantitySample) {
            try {
                return invoke4((com.codename1.health.QuantitySample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.SeriesSample) {
            try {
                return invoke5((com.codename1.health.SeriesSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.SessionSample) {
            try {
                return invoke6((com.codename1.health.SessionSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.AggregateQuery) {
            try {
                return invoke7((com.codename1.health.AggregateQuery) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.AggregateResult) {
            try {
                return invoke8((com.codename1.health.AggregateResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.Health) {
            try {
                return invoke9((com.codename1.health.Health) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthAccess) {
            try {
                return invoke10((com.codename1.health.HealthAccess) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthAnchor) {
            try {
                return invoke11((com.codename1.health.HealthAnchor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthChangeBatch) {
            try {
                return invoke12((com.codename1.health.HealthChangeBatch) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthDataType) {
            try {
                return invoke13((com.codename1.health.HealthDataType) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthDeleteRequest) {
            try {
                return invoke14((com.codename1.health.HealthDeleteRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthException) {
            try {
                return invoke15((com.codename1.health.HealthException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthInterval) {
            try {
                return invoke16((com.codename1.health.HealthInterval) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthQuantity) {
            try {
                return invoke17((com.codename1.health.HealthQuantity) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthSample) {
            try {
                return invoke18((com.codename1.health.HealthSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthSource) {
            try {
                return invoke19((com.codename1.health.HealthSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthStore) {
            try {
                return invoke20((com.codename1.health.HealthStore) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthSubscription) {
            try {
                return invoke21((com.codename1.health.HealthSubscription) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthTimeRange) {
            try {
                return invoke22((com.codename1.health.HealthTimeRange) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthUnit) {
            try {
                return invoke23((com.codename1.health.HealthUnit) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthWriteResult) {
            try {
                return invoke24((com.codename1.health.HealthWriteResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.SamplePage) {
            try {
                return invoke25((com.codename1.health.SamplePage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.SampleQuery) {
            try {
                return invoke26((com.codename1.health.SampleQuery) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.SleepStageInterval) {
            try {
                return invoke27((com.codename1.health.SleepStageInterval) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.SubscriptionRequest) {
            try {
                return invoke28((com.codename1.health.SubscriptionRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthBackgroundListener) {
            try {
                return invoke29((com.codename1.health.HealthBackgroundListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthBackgroundListenerFactory) {
            try {
                return invoke30((com.codename1.health.HealthBackgroundListenerFactory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.HealthChangeListener) {
            try {
                return invoke31((com.codename1.health.HealthChangeListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.health.SleepSample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addStage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SleepStageInterval.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SleepStageInterval.class}, false);
                typedTarget.addStage((com.codename1.health.SleepStageInterval) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAsleepDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAsleepDuration();
            }
        }
        if ("getAsleepDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAsleepDurationMillis();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SleepStage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SleepStage.class}, false);
                return typedTarget.getDurationMillis((com.codename1.health.SleepStage) adaptedArgs[0]);
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getNotes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNotes();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStageSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStageSupport();
            }
        }
        if ("getStages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStages();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("hasStageDetail".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasStageDetail();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNotes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNotes((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.health.WorkoutSample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getActiveDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActiveDuration();
            }
        }
        if ("getActiveDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActiveDurationMillis();
            }
        }
        if ("getActivityType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActivityType();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getNotes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNotes();
            }
        }
        if ("getPlatformCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlatformCode();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getTotalDistance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalDistance();
            }
        }
        if ("getTotalEnergy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalEnergy();
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
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setActiveDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                typedTarget.setActiveDuration((java.time.Duration) adaptedArgs[0]); return null;
            }
        }
        if ("setActiveDurationMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setActiveDurationMillis(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNotes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNotes((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPlatformCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPlatformCode(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTotalDistance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class}, false);
                typedTarget.setTotalDistance((com.codename1.health.HealthQuantity) adaptedArgs[0]); return null;
            }
        }
        if ("setTotalEnergy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class}, false);
                typedTarget.setTotalEnergy((com.codename1.health.HealthQuantity) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.health.BloodPressureSample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getBodyPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBodyPosition();
            }
        }
        if ("getDiastolic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDiastolic();
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMeasurementLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMeasurementLocation();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getPulse".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPulse();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getSystolic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSystolic();
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
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setBodyPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBodyPosition(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setMeasurementLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMeasurementLocation(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPulse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthQuantity.class}, false);
                typedTarget.setPulse((com.codename1.health.HealthQuantity) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.health.CategorySample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.health.QuantitySample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getQuantity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQuantity();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false);
                return typedTarget.getValue((com.codename1.health.HealthUnit) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.health.SeriesSample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSampleEndMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSampleEndMillis(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getSampleStartMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getSampleStartMillis(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getSampleValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.health.HealthUnit.class}, false);
                return typedTarget.getSampleValue(toIntValue(adaptedArgs[0]), (com.codename1.health.HealthUnit) adaptedArgs[1]);
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toQuantitySample".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toQuantitySample(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.health.SessionSample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getNotes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNotes();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
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
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNotes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNotes((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.health.AggregateQuery typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addMetric".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.AggregateMetric.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.AggregateMetric.class}, false);
                return typedTarget.addMetric((com.codename1.health.AggregateMetric) adaptedArgs[0]);
            }
        }
        if ("addSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.addSource((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("addType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.addType((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getBucket".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBucket();
            }
        }
        if ("getMetrics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetrics();
            }
        }
        if ("getSources".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSources();
            }
        }
        if ("getTimeRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeRange();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("isDeduplicateSources".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDeduplicateSources();
            }
        }
        if ("setBucket".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthInterval.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthInterval.class}, false);
                return typedTarget.setBucket((com.codename1.health.HealthInterval) adaptedArgs[0]);
            }
        }
        if ("setDeduplicateSources".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setDeduplicateSources(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTimeRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthTimeRange.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthTimeRange.class}, false);
                return typedTarget.setTimeRange((com.codename1.health.HealthTimeRange) adaptedArgs[0]);
            }
        }
        if ("setUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false);
                return typedTarget.setUnit((com.codename1.health.HealthUnit) adaptedArgs[0]);
            }
        }
        if ("validate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validate(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.health.AggregateResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.AggregateMetric.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.AggregateMetric.class}, false);
                return typedTarget.get((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.AggregateMetric) adaptedArgs[1]);
            }
        }
        if ("getBucketEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBucketEndMillis();
            }
        }
        if ("getBucketStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBucketStartMillis();
            }
        }
        if ("getSampleCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.getSampleCount((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.AggregateMetric.class, com.codename1.health.HealthQuantity.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.AggregateMetric.class, com.codename1.health.HealthQuantity.class}, false);
                typedTarget.put((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.AggregateMetric) adaptedArgs[1], (com.codename1.health.HealthQuantity) adaptedArgs[2]); return null;
            }
        }
        if ("setSampleCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, java.lang.Integer.class}, false);
                typedTarget.setSampleCount((com.codename1.health.HealthDataType) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.health.Health typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAvailability".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailability();
            }
        }
        if ("getConfigurationProblems".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfigurationProblems();
            }
        }
        if ("getSensors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSensors();
            }
        }
        if ("getStore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStore();
            }
        }
        if ("getWorkouts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWorkouts();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("openHealthSettings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.openHealthSettings();
            }
        }
        if ("openProviderSetup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.openProviderSetup();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.health.HealthAccess typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
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
        if ("isRead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRead();
            }
        }
        if ("isWrite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWrite();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.health.HealthAnchor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toStorableString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toStorableString();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.health.HealthChangeBatch typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAdded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdded();
            }
        }
        if ("getAnchor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnchor();
            }
        }
        if ("getDeadlineMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeadlineMillis();
            }
        }
        if ("getDeletedSampleIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeletedSampleIds();
            }
        }
        if ("getSubscriptionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubscriptionId();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
            }
        }
        if ("hasMore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasMore();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
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

    private static Object invoke13(com.codename1.health.HealthDataType typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAggregationStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAggregationStyle();
            }
        }
        if ("getCanonicalUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCanonicalUnit();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("isIntervalOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIntervalOnly();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.health.HealthDeleteRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.addType((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getSampleIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleIds();
            }
        }
        if ("getTimeRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeRange();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
            }
        }
        if ("isById".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isById();
            }
        }
        if ("validate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validate(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.health.HealthException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getPartialResult".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPartialResult();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.health.HealthInterval typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bucketStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return typedTarget.bucketStart(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getFixedMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFixedMillis();
            }
        }
        if ("getZone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZone();
            }
        }
        if ("isCalendarBased".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCalendarBased();
            }
        }
        if ("nextBoundary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.nextBoundary(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.health.HealthQuantity typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getRawValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawValue();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false);
                return typedTarget.getValue((com.codename1.health.HealthUnit) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("in".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false);
                return typedTarget.in((com.codename1.health.HealthUnit) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.health.HealthSample typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getRecordingMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecordingMethod();
            }
        }
        if ("getSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSource();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
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
        if ("isInstantaneous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInstantaneous();
            }
        }
        if ("putMetadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.putMetadata((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRecordingMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.RecordingMethod.class}, false);
                typedTarget.setRecordingMethod((com.codename1.health.RecordingMethod) adaptedArgs[0]); return null;
            }
        }
        if ("setSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSource.class}, false);
                typedTarget.setSource((com.codename1.health.HealthSource) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.health.HealthSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getBundleId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBundleId();
            }
        }
        if ("getDeviceManufacturer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeviceManufacturer();
            }
        }
        if ("getDeviceModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeviceModel();
            }
        }
        if ("getDeviceName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeviceName();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
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

    private static Object invoke20(com.codename1.health.HealthStore typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("aggregate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.AggregateQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.AggregateQuery.class}, false);
                return typedTarget.aggregate((com.codename1.health.AggregateQuery) adaptedArgs[0]);
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDeleteRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDeleteRequest.class}, false);
                return typedTarget.delete((com.codename1.health.HealthDeleteRequest) adaptedArgs[0]);
            }
        }
        if ("drainChanges".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.drainChanges();
            }
        }
        if ("getAuthorizationRequestStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthAccess[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthAccess[].class}, true);
                com.codename1.health.HealthAccess[] varArgs = new com.codename1.health.HealthAccess[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.health.HealthAccess) adaptedArgs[i];
                }
                return typedTarget.getAuthorizationRequestStatus(varArgs);
            }
        }
        if ("getMaxWriteBatchSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxWriteBatchSize();
            }
        }
        if ("getPreferredWriteUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.getPreferredWriteUnit((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getReadAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.getReadAuthorizationStatus((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getSubscriptions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubscriptions();
            }
        }
        if ("getSupportedMetrics".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.getSupportedMetrics((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getSupportedTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSupportedTypes();
            }
        }
        if ("getWriteAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.getWriteAuthorizationStatus((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("hasAnyData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthTimeRange.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class, com.codename1.health.HealthTimeRange.class}, false);
                return typedTarget.hasAnyData((com.codename1.health.HealthDataType) adaptedArgs[0], (com.codename1.health.HealthTimeRange) adaptedArgs[1]);
            }
        }
        if ("isBackgroundDeliverySupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundDeliverySupported();
            }
        }
        if ("isDeletable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.isDeletable((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("isPushDelivery".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPushDelivery();
            }
        }
        if ("isSourceDeduplicationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSourceDeduplicationSupported();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("isTypeSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.isTypeSupported((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("isWritable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.isWritable((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("readSamplePage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SampleQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SampleQuery.class}, false);
                return typedTarget.readSamplePage((com.codename1.health.SampleQuery) adaptedArgs[0]);
            }
        }
        if ("readSamples".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SampleQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SampleQuery.class}, false);
                return typedTarget.readSamples((com.codename1.health.SampleQuery) adaptedArgs[0]);
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthAccess[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthAccess[].class}, true);
                com.codename1.health.HealthAccess[] varArgs = new com.codename1.health.HealthAccess[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.health.HealthAccess) adaptedArgs[i];
                }
                return typedTarget.requestAuthorization(varArgs);
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SubscriptionRequest.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SubscriptionRequest.class, java.lang.Class.class}, false);
                return typedTarget.subscribe((com.codename1.health.SubscriptionRequest) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.SubscriptionRequest.class, com.codename1.health.HealthChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.SubscriptionRequest.class, com.codename1.health.HealthChangeListener.class}, false);
                return typedTarget.subscribe((com.codename1.health.SubscriptionRequest) adaptedArgs[0], (com.codename1.health.HealthChangeListener) adaptedArgs[1]);
            }
        }
        if ("unsubscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.unsubscribe((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthSample.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthSample.class}, false);
                return typedTarget.write((com.codename1.health.HealthSample) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.write((java.util.List) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.health.HealthSubscription typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAnchor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnchor();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLastDeliveryMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastDeliveryMillis();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
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

    private static Object invoke22(com.codename1.health.HealthTimeRange typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.contains(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("isOpenEnded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpenEnded();
            }
        }
        if ("resolve".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.resolve(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.health.HealthUnit typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fromCanonical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.fromCanonical(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("getDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDimension();
            }
        }
        if ("getSymbol".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSymbol();
            }
        }
        if ("isCompatibleWith".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false);
                return typedTarget.isCompatibleWith((com.codename1.health.HealthUnit) adaptedArgs[0]);
            }
        }
        if ("toCanonical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.toCanonical(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.health.HealthWriteResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addRejection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.addRejection((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("addSampleId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.addSampleId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("getRejections".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRejections();
            }
        }
        if ("getSampleIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleIds();
            }
        }
        if ("getWrittenCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWrittenCount();
            }
        }
        if ("hasRejections".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasRejections();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(com.codename1.health.SamplePage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getNextPageToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextPageToken();
            }
        }
        if ("getSamples".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSamples();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("isTruncated".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTruncated();
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(com.codename1.health.SampleQuery typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.addSource((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("addType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.addType((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLimit();
            }
        }
        if ("getPageToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPageToken();
            }
        }
        if ("getSleepSessionGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSleepSessionGap();
            }
        }
        if ("getSleepSessionGapMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSleepSessionGapMillis();
            }
        }
        if ("getSources".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSources();
            }
        }
        if ("getTimeRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeRange();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("isFlattenSeries".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlattenSeries();
            }
        }
        if ("isSortDescending".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSortDescending();
            }
        }
        if ("setFlattenSeries".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setFlattenSeries(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setLimit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setLimit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPageToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setPageToken((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSleepSessionGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.setSleepSessionGap((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("setSleepSessionGapMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.setSleepSessionGapMillis(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("setSortDescending".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setSortDescending(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTimeRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthTimeRange.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthTimeRange.class}, false);
                return typedTarget.setTimeRange((com.codename1.health.HealthTimeRange) adaptedArgs[0]);
            }
        }
        if ("setUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthUnit.class}, false);
                return typedTarget.setUnit((com.codename1.health.HealthUnit) adaptedArgs[0]);
            }
        }
        if ("validate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validate(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke27(com.codename1.health.SleepStageInterval typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getEndMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndMillis();
            }
        }
        if ("getPlatformCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlatformCode();
            }
        }
        if ("getStage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStage();
            }
        }
        if ("getStartMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartMillis();
            }
        }
        if ("setPlatformCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPlatformCode(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke28(com.codename1.health.SubscriptionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthDataType.class}, false);
                return typedTarget.addType((com.codename1.health.HealthDataType) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaxSamplesPerBatch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxSamplesPerBatch();
            }
        }
        if ("getTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTypes();
            }
        }
        if ("isDeliverSamples".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDeliverSamples();
            }
        }
        if ("isIncludeDeletions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIncludeDeletions();
            }
        }
        if ("setDeliverSamples".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setDeliverSamples(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setIncludeDeletions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setIncludeDeletions(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setMaxSamplesPerBatch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setMaxSamplesPerBatch(toIntValue(adaptedArgs[0]));
            }
        }
        if ("validate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.validate(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke29(com.codename1.health.HealthBackgroundListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("healthDataChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthChangeBatch.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthChangeBatch.class}, false);
                typedTarget.healthDataChanged((com.codename1.health.HealthChangeBatch) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke30(com.codename1.health.HealthBackgroundListenerFactory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.create((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke31(com.codename1.health.HealthChangeListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("healthDataChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.HealthChangeBatch.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.HealthChangeBatch.class}, false);
                typedTarget.healthDataChanged((com.codename1.health.HealthChangeBatch) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.health.AggregateMetric.class) return getStaticField0(name);
        if (type == com.codename1.health.BloodPressureSample.class) return getStaticField1(name);
        if (type == com.codename1.health.CategorySample.class) return getStaticField2(name);
        if (type == com.codename1.health.HealthAggregationStyle.class) return getStaticField3(name);
        if (type == com.codename1.health.HealthAuthorizationStatus.class) return getStaticField4(name);
        if (type == com.codename1.health.HealthAvailability.class) return getStaticField5(name);
        if (type == com.codename1.health.HealthDataKind.class) return getStaticField6(name);
        if (type == com.codename1.health.HealthDataType.class) return getStaticField7(name);
        if (type == com.codename1.health.HealthError.class) return getStaticField8(name);
        if (type == com.codename1.health.HealthRequestStatus.class) return getStaticField9(name);
        if (type == com.codename1.health.HealthUnit.class) return getStaticField10(name);
        if (type == com.codename1.health.HealthUnitDimension.class) return getStaticField11(name);
        if (type == com.codename1.health.RecordingMethod.class) return getStaticField12(name);
        if (type == com.codename1.health.SampleQuery.class) return getStaticField13(name);
        if (type == com.codename1.health.SleepStage.class) return getStaticField14(name);
        if (type == com.codename1.health.SleepStageInterval.class) return getStaticField15(name);
        if (type == com.codename1.health.SleepStageSupport.class) return getStaticField16(name);
        if (type == com.codename1.health.SubscriptionRequest.class) return getStaticField17(name);
        if (type == com.codename1.health.WorkoutActivityType.class) return getStaticField18(name);
        if (type == com.codename1.health.WorkoutSample.class) return getStaticField19(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("AVERAGE".equals(name)) return com.codename1.health.AggregateMetric.AVERAGE;
        if ("COUNT".equals(name)) return com.codename1.health.AggregateMetric.COUNT;
        if ("DURATION".equals(name)) return com.codename1.health.AggregateMetric.DURATION;
        if ("LATEST".equals(name)) return com.codename1.health.AggregateMetric.LATEST;
        if ("MAXIMUM".equals(name)) return com.codename1.health.AggregateMetric.MAXIMUM;
        if ("MINIMUM".equals(name)) return com.codename1.health.AggregateMetric.MINIMUM;
        if ("TOTAL".equals(name)) return com.codename1.health.AggregateMetric.TOTAL;
        throw unsupportedStaticField(com.codename1.health.AggregateMetric.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("LOCATION_LEFT_UPPER_ARM".equals(name)) return com.codename1.health.BloodPressureSample.LOCATION_LEFT_UPPER_ARM;
        if ("LOCATION_LEFT_WRIST".equals(name)) return com.codename1.health.BloodPressureSample.LOCATION_LEFT_WRIST;
        if ("LOCATION_RIGHT_UPPER_ARM".equals(name)) return com.codename1.health.BloodPressureSample.LOCATION_RIGHT_UPPER_ARM;
        if ("LOCATION_RIGHT_WRIST".equals(name)) return com.codename1.health.BloodPressureSample.LOCATION_RIGHT_WRIST;
        if ("LOCATION_UNKNOWN".equals(name)) return com.codename1.health.BloodPressureSample.LOCATION_UNKNOWN;
        if ("POSITION_LYING_DOWN".equals(name)) return com.codename1.health.BloodPressureSample.POSITION_LYING_DOWN;
        if ("POSITION_RECLINING".equals(name)) return com.codename1.health.BloodPressureSample.POSITION_RECLINING;
        if ("POSITION_SITTING".equals(name)) return com.codename1.health.BloodPressureSample.POSITION_SITTING;
        if ("POSITION_STANDING".equals(name)) return com.codename1.health.BloodPressureSample.POSITION_STANDING;
        if ("POSITION_UNKNOWN".equals(name)) return com.codename1.health.BloodPressureSample.POSITION_UNKNOWN;
        throw unsupportedStaticField(com.codename1.health.BloodPressureSample.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("FLOW_HEAVY".equals(name)) return com.codename1.health.CategorySample.FLOW_HEAVY;
        if ("FLOW_LIGHT".equals(name)) return com.codename1.health.CategorySample.FLOW_LIGHT;
        if ("FLOW_MEDIUM".equals(name)) return com.codename1.health.CategorySample.FLOW_MEDIUM;
        if ("FLOW_UNSPECIFIED".equals(name)) return com.codename1.health.CategorySample.FLOW_UNSPECIFIED;
        throw unsupportedStaticField(com.codename1.health.CategorySample.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("CUMULATIVE".equals(name)) return com.codename1.health.HealthAggregationStyle.CUMULATIVE;
        if ("DISCRETE".equals(name)) return com.codename1.health.HealthAggregationStyle.DISCRETE;
        if ("NONE".equals(name)) return com.codename1.health.HealthAggregationStyle.NONE;
        throw unsupportedStaticField(com.codename1.health.HealthAggregationStyle.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("AUTHORIZED".equals(name)) return com.codename1.health.HealthAuthorizationStatus.AUTHORIZED;
        if ("DENIED".equals(name)) return com.codename1.health.HealthAuthorizationStatus.DENIED;
        if ("NOT_DETERMINED".equals(name)) return com.codename1.health.HealthAuthorizationStatus.NOT_DETERMINED;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.health.HealthAuthorizationStatus.NOT_SUPPORTED;
        if ("RESTRICTED".equals(name)) return com.codename1.health.HealthAuthorizationStatus.RESTRICTED;
        if ("UNKNOWN".equals(name)) return com.codename1.health.HealthAuthorizationStatus.UNKNOWN;
        throw unsupportedStaticField(com.codename1.health.HealthAuthorizationStatus.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("AVAILABLE".equals(name)) return com.codename1.health.HealthAvailability.AVAILABLE;
        if ("LOCAL_ONLY".equals(name)) return com.codename1.health.HealthAvailability.LOCAL_ONLY;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.health.HealthAvailability.NOT_SUPPORTED;
        if ("PROVIDER_NOT_INSTALLED".equals(name)) return com.codename1.health.HealthAvailability.PROVIDER_NOT_INSTALLED;
        if ("PROVIDER_UPDATE_REQUIRED".equals(name)) return com.codename1.health.HealthAvailability.PROVIDER_UPDATE_REQUIRED;
        throw unsupportedStaticField(com.codename1.health.HealthAvailability.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("CATEGORY".equals(name)) return com.codename1.health.HealthDataKind.CATEGORY;
        if ("COMPOSITE".equals(name)) return com.codename1.health.HealthDataKind.COMPOSITE;
        if ("QUANTITY".equals(name)) return com.codename1.health.HealthDataKind.QUANTITY;
        if ("SERIES".equals(name)) return com.codename1.health.HealthDataKind.SERIES;
        if ("SESSION".equals(name)) return com.codename1.health.HealthDataKind.SESSION;
        throw unsupportedStaticField(com.codename1.health.HealthDataKind.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("ACTIVE_ENERGY".equals(name)) return com.codename1.health.HealthDataType.ACTIVE_ENERGY;
        if ("BASAL_BODY_TEMPERATURE".equals(name)) return com.codename1.health.HealthDataType.BASAL_BODY_TEMPERATURE;
        if ("BASAL_ENERGY".equals(name)) return com.codename1.health.HealthDataType.BASAL_ENERGY;
        if ("BLOOD_GLUCOSE".equals(name)) return com.codename1.health.HealthDataType.BLOOD_GLUCOSE;
        if ("BLOOD_PRESSURE".equals(name)) return com.codename1.health.HealthDataType.BLOOD_PRESSURE;
        if ("BODY_FAT_PERCENTAGE".equals(name)) return com.codename1.health.HealthDataType.BODY_FAT_PERCENTAGE;
        if ("BODY_MASS".equals(name)) return com.codename1.health.HealthDataType.BODY_MASS;
        if ("BODY_MASS_INDEX".equals(name)) return com.codename1.health.HealthDataType.BODY_MASS_INDEX;
        if ("BODY_TEMPERATURE".equals(name)) return com.codename1.health.HealthDataType.BODY_TEMPERATURE;
        if ("BONE_MASS".equals(name)) return com.codename1.health.HealthDataType.BONE_MASS;
        if ("CYCLING_CADENCE".equals(name)) return com.codename1.health.HealthDataType.CYCLING_CADENCE;
        if ("DIETARY_ENERGY".equals(name)) return com.codename1.health.HealthDataType.DIETARY_ENERGY;
        if ("DISTANCE_CYCLING".equals(name)) return com.codename1.health.HealthDataType.DISTANCE_CYCLING;
        if ("DISTANCE_SWIMMING".equals(name)) return com.codename1.health.HealthDataType.DISTANCE_SWIMMING;
        if ("DISTANCE_WALKING_RUNNING".equals(name)) return com.codename1.health.HealthDataType.DISTANCE_WALKING_RUNNING;
        if ("ELEVATION_GAINED".equals(name)) return com.codename1.health.HealthDataType.ELEVATION_GAINED;
        if ("EXERCISE_TIME".equals(name)) return com.codename1.health.HealthDataType.EXERCISE_TIME;
        if ("FLIGHTS_CLIMBED".equals(name)) return com.codename1.health.HealthDataType.FLIGHTS_CLIMBED;
        if ("HEART_RATE".equals(name)) return com.codename1.health.HealthDataType.HEART_RATE;
        if ("HEART_RATE_VARIABILITY_SDNN".equals(name)) return com.codename1.health.HealthDataType.HEART_RATE_VARIABILITY_SDNN;
        if ("HEIGHT".equals(name)) return com.codename1.health.HealthDataType.HEIGHT;
        if ("HYDRATION".equals(name)) return com.codename1.health.HealthDataType.HYDRATION;
        if ("INTERMENSTRUAL_BLEEDING".equals(name)) return com.codename1.health.HealthDataType.INTERMENSTRUAL_BLEEDING;
        if ("LEAN_BODY_MASS".equals(name)) return com.codename1.health.HealthDataType.LEAN_BODY_MASS;
        if ("MENSTRUATION_FLOW".equals(name)) return com.codename1.health.HealthDataType.MENSTRUATION_FLOW;
        if ("MINDFUL_SESSION".equals(name)) return com.codename1.health.HealthDataType.MINDFUL_SESSION;
        if ("NUTRITION".equals(name)) return com.codename1.health.HealthDataType.NUTRITION;
        if ("OXYGEN_SATURATION".equals(name)) return com.codename1.health.HealthDataType.OXYGEN_SATURATION;
        if ("POWER".equals(name)) return com.codename1.health.HealthDataType.POWER;
        if ("RESPIRATORY_RATE".equals(name)) return com.codename1.health.HealthDataType.RESPIRATORY_RATE;
        if ("RESTING_HEART_RATE".equals(name)) return com.codename1.health.HealthDataType.RESTING_HEART_RATE;
        if ("RUNNING_CADENCE".equals(name)) return com.codename1.health.HealthDataType.RUNNING_CADENCE;
        if ("SLEEP".equals(name)) return com.codename1.health.HealthDataType.SLEEP;
        if ("SPEED".equals(name)) return com.codename1.health.HealthDataType.SPEED;
        if ("STEPS".equals(name)) return com.codename1.health.HealthDataType.STEPS;
        if ("VO2_MAX".equals(name)) return com.codename1.health.HealthDataType.VO2_MAX;
        if ("WAIST_CIRCUMFERENCE".equals(name)) return com.codename1.health.HealthDataType.WAIST_CIRCUMFERENCE;
        if ("WALKING_HEART_RATE_AVERAGE".equals(name)) return com.codename1.health.HealthDataType.WALKING_HEART_RATE_AVERAGE;
        if ("WHEELCHAIR_PUSHES".equals(name)) return com.codename1.health.HealthDataType.WHEELCHAIR_PUSHES;
        if ("WORKOUT".equals(name)) return com.codename1.health.HealthDataType.WORKOUT;
        throw unsupportedStaticField(com.codename1.health.HealthDataType.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("ANCHOR_EXPIRED".equals(name)) return com.codename1.health.HealthError.ANCHOR_EXPIRED;
        if ("DATABASE_INACCESSIBLE".equals(name)) return com.codename1.health.HealthError.DATABASE_INACCESSIBLE;
        if ("INVALID_ARGUMENT".equals(name)) return com.codename1.health.HealthError.INVALID_ARGUMENT;
        if ("INVALID_DATA".equals(name)) return com.codename1.health.HealthError.INVALID_DATA;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.health.HealthError.NOT_SUPPORTED;
        if ("PROVIDER_UNAVAILABLE".equals(name)) return com.codename1.health.HealthError.PROVIDER_UNAVAILABLE;
        if ("PROVIDER_UPDATE_REQUIRED".equals(name)) return com.codename1.health.HealthError.PROVIDER_UPDATE_REQUIRED;
        if ("QUOTA_EXCEEDED".equals(name)) return com.codename1.health.HealthError.QUOTA_EXCEEDED;
        if ("RATE_LIMITED".equals(name)) return com.codename1.health.HealthError.RATE_LIMITED;
        if ("SENSOR_DISCONNECTED".equals(name)) return com.codename1.health.HealthError.SENSOR_DISCONNECTED;
        if ("SESSION_STATE".equals(name)) return com.codename1.health.HealthError.SESSION_STATE;
        if ("TIMEOUT".equals(name)) return com.codename1.health.HealthError.TIMEOUT;
        if ("TYPE_NOT_SUPPORTED".equals(name)) return com.codename1.health.HealthError.TYPE_NOT_SUPPORTED;
        if ("UNAUTHORIZED".equals(name)) return com.codename1.health.HealthError.UNAUTHORIZED;
        if ("UNIT_MISMATCH".equals(name)) return com.codename1.health.HealthError.UNIT_MISMATCH;
        if ("UNKNOWN".equals(name)) return com.codename1.health.HealthError.UNKNOWN;
        if ("USER_CANCELED".equals(name)) return com.codename1.health.HealthError.USER_CANCELED;
        throw unsupportedStaticField(com.codename1.health.HealthError.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("SHOULD_REQUEST".equals(name)) return com.codename1.health.HealthRequestStatus.SHOULD_REQUEST;
        if ("UNKNOWN".equals(name)) return com.codename1.health.HealthRequestStatus.UNKNOWN;
        if ("UNNECESSARY".equals(name)) return com.codename1.health.HealthRequestStatus.UNNECESSARY;
        throw unsupportedStaticField(com.codename1.health.HealthRequestStatus.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
        if ("CENTIMETER".equals(name)) return com.codename1.health.HealthUnit.CENTIMETER;
        if ("COUNT".equals(name)) return com.codename1.health.HealthUnit.COUNT;
        if ("COUNT_PER_MINUTE".equals(name)) return com.codename1.health.HealthUnit.COUNT_PER_MINUTE;
        if ("COUNT_PER_SECOND".equals(name)) return com.codename1.health.HealthUnit.COUNT_PER_SECOND;
        if ("CUP_US".equals(name)) return com.codename1.health.HealthUnit.CUP_US;
        if ("DEGREE_CELSIUS".equals(name)) return com.codename1.health.HealthUnit.DEGREE_CELSIUS;
        if ("DEGREE_FAHRENHEIT".equals(name)) return com.codename1.health.HealthUnit.DEGREE_FAHRENHEIT;
        if ("FLUID_OUNCE_US".equals(name)) return com.codename1.health.HealthUnit.FLUID_OUNCE_US;
        if ("FOOT".equals(name)) return com.codename1.health.HealthUnit.FOOT;
        if ("GRAM".equals(name)) return com.codename1.health.HealthUnit.GRAM;
        if ("HOUR".equals(name)) return com.codename1.health.HealthUnit.HOUR;
        if ("INCH".equals(name)) return com.codename1.health.HealthUnit.INCH;
        if ("JOULE".equals(name)) return com.codename1.health.HealthUnit.JOULE;
        if ("KILOCALORIE".equals(name)) return com.codename1.health.HealthUnit.KILOCALORIE;
        if ("KILOGRAM".equals(name)) return com.codename1.health.HealthUnit.KILOGRAM;
        if ("KILOJOULE".equals(name)) return com.codename1.health.HealthUnit.KILOJOULE;
        if ("KILOMETER".equals(name)) return com.codename1.health.HealthUnit.KILOMETER;
        if ("KILOMETER_PER_HOUR".equals(name)) return com.codename1.health.HealthUnit.KILOMETER_PER_HOUR;
        if ("KILOPASCAL".equals(name)) return com.codename1.health.HealthUnit.KILOPASCAL;
        if ("LITER".equals(name)) return com.codename1.health.HealthUnit.LITER;
        if ("METER".equals(name)) return com.codename1.health.HealthUnit.METER;
        if ("METER_PER_SECOND".equals(name)) return com.codename1.health.HealthUnit.METER_PER_SECOND;
        if ("MICROGRAM".equals(name)) return com.codename1.health.HealthUnit.MICROGRAM;
        if ("MILE".equals(name)) return com.codename1.health.HealthUnit.MILE;
        if ("MILE_PER_HOUR".equals(name)) return com.codename1.health.HealthUnit.MILE_PER_HOUR;
        if ("MILLIGRAM".equals(name)) return com.codename1.health.HealthUnit.MILLIGRAM;
        if ("MILLIGRAM_PER_DECILITER".equals(name)) return com.codename1.health.HealthUnit.MILLIGRAM_PER_DECILITER;
        if ("MILLILITER".equals(name)) return com.codename1.health.HealthUnit.MILLILITER;
        if ("MILLIMETER_OF_MERCURY".equals(name)) return com.codename1.health.HealthUnit.MILLIMETER_OF_MERCURY;
        if ("MILLIMOLE_PER_LITER".equals(name)) return com.codename1.health.HealthUnit.MILLIMOLE_PER_LITER;
        if ("MILLISECOND".equals(name)) return com.codename1.health.HealthUnit.MILLISECOND;
        if ("MINUTE".equals(name)) return com.codename1.health.HealthUnit.MINUTE;
        if ("ML_PER_KG_PER_MINUTE".equals(name)) return com.codename1.health.HealthUnit.ML_PER_KG_PER_MINUTE;
        if ("OUNCE".equals(name)) return com.codename1.health.HealthUnit.OUNCE;
        if ("PERCENT".equals(name)) return com.codename1.health.HealthUnit.PERCENT;
        if ("POUND".equals(name)) return com.codename1.health.HealthUnit.POUND;
        if ("SECOND".equals(name)) return com.codename1.health.HealthUnit.SECOND;
        if ("STONE".equals(name)) return com.codename1.health.HealthUnit.STONE;
        if ("WATT".equals(name)) return com.codename1.health.HealthUnit.WATT;
        if ("YARD".equals(name)) return com.codename1.health.HealthUnit.YARD;
        throw unsupportedStaticField(com.codename1.health.HealthUnit.class, name);
    }

    private static Object getStaticField11(String name) throws Exception {
        if ("COUNT".equals(name)) return com.codename1.health.HealthUnitDimension.COUNT;
        if ("ENERGY".equals(name)) return com.codename1.health.HealthUnitDimension.ENERGY;
        if ("FREQUENCY".equals(name)) return com.codename1.health.HealthUnitDimension.FREQUENCY;
        if ("GLUCOSE_CONCENTRATION".equals(name)) return com.codename1.health.HealthUnitDimension.GLUCOSE_CONCENTRATION;
        if ("LENGTH".equals(name)) return com.codename1.health.HealthUnitDimension.LENGTH;
        if ("MASS".equals(name)) return com.codename1.health.HealthUnitDimension.MASS;
        if ("OXYGEN_UPTAKE".equals(name)) return com.codename1.health.HealthUnitDimension.OXYGEN_UPTAKE;
        if ("PERCENT".equals(name)) return com.codename1.health.HealthUnitDimension.PERCENT;
        if ("POWER".equals(name)) return com.codename1.health.HealthUnitDimension.POWER;
        if ("PRESSURE".equals(name)) return com.codename1.health.HealthUnitDimension.PRESSURE;
        if ("TEMPERATURE".equals(name)) return com.codename1.health.HealthUnitDimension.TEMPERATURE;
        if ("TIME".equals(name)) return com.codename1.health.HealthUnitDimension.TIME;
        if ("VELOCITY".equals(name)) return com.codename1.health.HealthUnitDimension.VELOCITY;
        if ("VOLUME".equals(name)) return com.codename1.health.HealthUnitDimension.VOLUME;
        throw unsupportedStaticField(com.codename1.health.HealthUnitDimension.class, name);
    }

    private static Object getStaticField12(String name) throws Exception {
        if ("ACTIVELY_RECORDED".equals(name)) return com.codename1.health.RecordingMethod.ACTIVELY_RECORDED;
        if ("AUTOMATIC".equals(name)) return com.codename1.health.RecordingMethod.AUTOMATIC;
        if ("MANUAL_ENTRY".equals(name)) return com.codename1.health.RecordingMethod.MANUAL_ENTRY;
        if ("UNKNOWN".equals(name)) return com.codename1.health.RecordingMethod.UNKNOWN;
        throw unsupportedStaticField(com.codename1.health.RecordingMethod.class, name);
    }

    private static Object getStaticField13(String name) throws Exception {
        if ("DEFAULT_LIMIT".equals(name)) return com.codename1.health.SampleQuery.DEFAULT_LIMIT;
        throw unsupportedStaticField(com.codename1.health.SampleQuery.class, name);
    }

    private static Object getStaticField14(String name) throws Exception {
        if ("ASLEEP_UNSPECIFIED".equals(name)) return com.codename1.health.SleepStage.ASLEEP_UNSPECIFIED;
        if ("AWAKE".equals(name)) return com.codename1.health.SleepStage.AWAKE;
        if ("AWAKE_IN_BED".equals(name)) return com.codename1.health.SleepStage.AWAKE_IN_BED;
        if ("CORE".equals(name)) return com.codename1.health.SleepStage.CORE;
        if ("DEEP".equals(name)) return com.codename1.health.SleepStage.DEEP;
        if ("LIGHT".equals(name)) return com.codename1.health.SleepStage.LIGHT;
        if ("OUT_OF_BED".equals(name)) return com.codename1.health.SleepStage.OUT_OF_BED;
        if ("REM".equals(name)) return com.codename1.health.SleepStage.REM;
        if ("UNKNOWN".equals(name)) return com.codename1.health.SleepStage.UNKNOWN;
        throw unsupportedStaticField(com.codename1.health.SleepStage.class, name);
    }

    private static Object getStaticField15(String name) throws Exception {
        if ("NO_PLATFORM_CODE".equals(name)) return com.codename1.health.SleepStageInterval.NO_PLATFORM_CODE;
        throw unsupportedStaticField(com.codename1.health.SleepStageInterval.class, name);
    }

    private static Object getStaticField16(String name) throws Exception {
        if ("ASLEEP_AWAKE".equals(name)) return com.codename1.health.SleepStageSupport.ASLEEP_AWAKE;
        if ("NONE".equals(name)) return com.codename1.health.SleepStageSupport.NONE;
        if ("STAGED".equals(name)) return com.codename1.health.SleepStageSupport.STAGED;
        throw unsupportedStaticField(com.codename1.health.SleepStageSupport.class, name);
    }

    private static Object getStaticField17(String name) throws Exception {
        if ("DEFAULT_MAX_SAMPLES_PER_BATCH".equals(name)) return com.codename1.health.SubscriptionRequest.DEFAULT_MAX_SAMPLES_PER_BATCH;
        throw unsupportedStaticField(com.codename1.health.SubscriptionRequest.class, name);
    }

    private static Object getStaticField18(String name) throws Exception {
        if ("AMERICAN_FOOTBALL".equals(name)) return com.codename1.health.WorkoutActivityType.AMERICAN_FOOTBALL;
        if ("BADMINTON".equals(name)) return com.codename1.health.WorkoutActivityType.BADMINTON;
        if ("BASEBALL".equals(name)) return com.codename1.health.WorkoutActivityType.BASEBALL;
        if ("BASKETBALL".equals(name)) return com.codename1.health.WorkoutActivityType.BASKETBALL;
        if ("BOXING".equals(name)) return com.codename1.health.WorkoutActivityType.BOXING;
        if ("CLIMBING".equals(name)) return com.codename1.health.WorkoutActivityType.CLIMBING;
        if ("COOLDOWN".equals(name)) return com.codename1.health.WorkoutActivityType.COOLDOWN;
        if ("CORE_TRAINING".equals(name)) return com.codename1.health.WorkoutActivityType.CORE_TRAINING;
        if ("CRICKET".equals(name)) return com.codename1.health.WorkoutActivityType.CRICKET;
        if ("CYCLING".equals(name)) return com.codename1.health.WorkoutActivityType.CYCLING;
        if ("DANCE".equals(name)) return com.codename1.health.WorkoutActivityType.DANCE;
        if ("ELLIPTICAL".equals(name)) return com.codename1.health.WorkoutActivityType.ELLIPTICAL;
        if ("FOOTBALL".equals(name)) return com.codename1.health.WorkoutActivityType.FOOTBALL;
        if ("FUNCTIONAL_TRAINING".equals(name)) return com.codename1.health.WorkoutActivityType.FUNCTIONAL_TRAINING;
        if ("GOLF".equals(name)) return com.codename1.health.WorkoutActivityType.GOLF;
        if ("HIGH_INTENSITY_INTERVAL_TRAINING".equals(name)) return com.codename1.health.WorkoutActivityType.HIGH_INTENSITY_INTERVAL_TRAINING;
        if ("HIKING".equals(name)) return com.codename1.health.WorkoutActivityType.HIKING;
        if ("MARTIAL_ARTS".equals(name)) return com.codename1.health.WorkoutActivityType.MARTIAL_ARTS;
        if ("MOUNTAIN_BIKING".equals(name)) return com.codename1.health.WorkoutActivityType.MOUNTAIN_BIKING;
        if ("OTHER".equals(name)) return com.codename1.health.WorkoutActivityType.OTHER;
        if ("PADDLING".equals(name)) return com.codename1.health.WorkoutActivityType.PADDLING;
        if ("PILATES".equals(name)) return com.codename1.health.WorkoutActivityType.PILATES;
        if ("ROWING".equals(name)) return com.codename1.health.WorkoutActivityType.ROWING;
        if ("RUGBY".equals(name)) return com.codename1.health.WorkoutActivityType.RUGBY;
        if ("RUNNING".equals(name)) return com.codename1.health.WorkoutActivityType.RUNNING;
        if ("SKATING".equals(name)) return com.codename1.health.WorkoutActivityType.SKATING;
        if ("SKIING".equals(name)) return com.codename1.health.WorkoutActivityType.SKIING;
        if ("SNOWBOARDING".equals(name)) return com.codename1.health.WorkoutActivityType.SNOWBOARDING;
        if ("SQUASH".equals(name)) return com.codename1.health.WorkoutActivityType.SQUASH;
        if ("STAIR_CLIMBING".equals(name)) return com.codename1.health.WorkoutActivityType.STAIR_CLIMBING;
        if ("STRENGTH_TRAINING".equals(name)) return com.codename1.health.WorkoutActivityType.STRENGTH_TRAINING;
        if ("STRETCHING".equals(name)) return com.codename1.health.WorkoutActivityType.STRETCHING;
        if ("SURFING".equals(name)) return com.codename1.health.WorkoutActivityType.SURFING;
        if ("SWIMMING".equals(name)) return com.codename1.health.WorkoutActivityType.SWIMMING;
        if ("TABLE_TENNIS".equals(name)) return com.codename1.health.WorkoutActivityType.TABLE_TENNIS;
        if ("TENNIS".equals(name)) return com.codename1.health.WorkoutActivityType.TENNIS;
        if ("VOLLEYBALL".equals(name)) return com.codename1.health.WorkoutActivityType.VOLLEYBALL;
        if ("WALKING".equals(name)) return com.codename1.health.WorkoutActivityType.WALKING;
        if ("WHEELCHAIR".equals(name)) return com.codename1.health.WorkoutActivityType.WHEELCHAIR;
        if ("YOGA".equals(name)) return com.codename1.health.WorkoutActivityType.YOGA;
        throw unsupportedStaticField(com.codename1.health.WorkoutActivityType.class, name);
    }

    private static Object getStaticField19(String name) throws Exception {
        if ("SAMPLES_NOT_PERSISTED".equals(name)) return com.codename1.health.WorkoutSample.SAMPLES_NOT_PERSISTED;
        if ("WORKOUT_NOT_PERSISTED".equals(name)) return com.codename1.health.WorkoutSample.WORKOUT_NOT_PERSISTED;
        throw unsupportedStaticField(com.codename1.health.WorkoutSample.class, name);
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
