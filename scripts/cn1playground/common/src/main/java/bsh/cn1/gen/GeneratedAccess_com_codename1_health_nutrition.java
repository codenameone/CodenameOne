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

public final class GeneratedAccess_com_codename1_health_nutrition {
    private GeneratedAccess_com_codename1_health_nutrition() {
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
        if ("Nutrient".equals(simpleName)) {
            return com.codename1.health.nutrition.Nutrient.class;
        }
        if ("NutritionSample".equals(simpleName)) {
            return com.codename1.health.nutrition.NutritionSample.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.health.nutrition.Nutrient.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.health.nutrition.NutritionSample.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("forId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.health.nutrition.Nutrient.forId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.health.nutrition.Nutrient.values();
            }
        }
        throw unsupportedStatic(com.codename1.health.nutrition.Nutrient.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.health.nutrition.NutritionSample.create(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.health.nutrition.NutritionSample.create(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.health.nutrition.NutritionSample.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.health.nutrition.Nutrient) {
            try {
                return invoke0((com.codename1.health.nutrition.Nutrient) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.health.nutrition.NutritionSample) {
            try {
                return invoke1((com.codename1.health.nutrition.NutritionSample) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.health.nutrition.Nutrient typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.health.nutrition.NutritionSample typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getFoodName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFoodName();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMealType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMealType();
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
        if ("getNutrient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class}, false);
                return typedTarget.getNutrient((com.codename1.health.nutrition.Nutrient) adaptedArgs[0]);
            }
        }
        if ("getNutrientCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNutrientCount();
            }
        }
        if ("getNutrients".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNutrients();
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
        if ("hasNutrient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class}, false);
                return typedTarget.hasNutrient((com.codename1.health.nutrition.Nutrient) adaptedArgs[0]);
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
        if ("setFoodName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setFoodName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setMealType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setMealType(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setNotes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNotes((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNutrient".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class, java.lang.Double.class}, false);
                return typedTarget.setNutrient((com.codename1.health.nutrition.Nutrient) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class, java.lang.Double.class, com.codename1.health.HealthUnit.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.health.nutrition.Nutrient.class, java.lang.Double.class, com.codename1.health.HealthUnit.class}, false);
                return typedTarget.setNutrient((com.codename1.health.nutrition.Nutrient) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), (com.codename1.health.HealthUnit) adaptedArgs[2]);
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

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.health.nutrition.Nutrient.class) return getStaticField0(name);
        if (type == com.codename1.health.nutrition.NutritionSample.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BIOTIN".equals(name)) return com.codename1.health.nutrition.Nutrient.BIOTIN;
        if ("CAFFEINE".equals(name)) return com.codename1.health.nutrition.Nutrient.CAFFEINE;
        if ("CALCIUM".equals(name)) return com.codename1.health.nutrition.Nutrient.CALCIUM;
        if ("CHLORIDE".equals(name)) return com.codename1.health.nutrition.Nutrient.CHLORIDE;
        if ("CHOLESTEROL".equals(name)) return com.codename1.health.nutrition.Nutrient.CHOLESTEROL;
        if ("CHROMIUM".equals(name)) return com.codename1.health.nutrition.Nutrient.CHROMIUM;
        if ("COPPER".equals(name)) return com.codename1.health.nutrition.Nutrient.COPPER;
        if ("DIETARY_FIBER".equals(name)) return com.codename1.health.nutrition.Nutrient.DIETARY_FIBER;
        if ("ENERGY".equals(name)) return com.codename1.health.nutrition.Nutrient.ENERGY;
        if ("FOLATE".equals(name)) return com.codename1.health.nutrition.Nutrient.FOLATE;
        if ("IODINE".equals(name)) return com.codename1.health.nutrition.Nutrient.IODINE;
        if ("IRON".equals(name)) return com.codename1.health.nutrition.Nutrient.IRON;
        if ("MAGNESIUM".equals(name)) return com.codename1.health.nutrition.Nutrient.MAGNESIUM;
        if ("MANGANESE".equals(name)) return com.codename1.health.nutrition.Nutrient.MANGANESE;
        if ("MOLYBDENUM".equals(name)) return com.codename1.health.nutrition.Nutrient.MOLYBDENUM;
        if ("MONOUNSATURATED_FAT".equals(name)) return com.codename1.health.nutrition.Nutrient.MONOUNSATURATED_FAT;
        if ("NIACIN".equals(name)) return com.codename1.health.nutrition.Nutrient.NIACIN;
        if ("PANTOTHENIC_ACID".equals(name)) return com.codename1.health.nutrition.Nutrient.PANTOTHENIC_ACID;
        if ("PHOSPHORUS".equals(name)) return com.codename1.health.nutrition.Nutrient.PHOSPHORUS;
        if ("POLYUNSATURATED_FAT".equals(name)) return com.codename1.health.nutrition.Nutrient.POLYUNSATURATED_FAT;
        if ("POTASSIUM".equals(name)) return com.codename1.health.nutrition.Nutrient.POTASSIUM;
        if ("PROTEIN".equals(name)) return com.codename1.health.nutrition.Nutrient.PROTEIN;
        if ("RIBOFLAVIN".equals(name)) return com.codename1.health.nutrition.Nutrient.RIBOFLAVIN;
        if ("SATURATED_FAT".equals(name)) return com.codename1.health.nutrition.Nutrient.SATURATED_FAT;
        if ("SELENIUM".equals(name)) return com.codename1.health.nutrition.Nutrient.SELENIUM;
        if ("SODIUM".equals(name)) return com.codename1.health.nutrition.Nutrient.SODIUM;
        if ("SUGAR".equals(name)) return com.codename1.health.nutrition.Nutrient.SUGAR;
        if ("THIAMIN".equals(name)) return com.codename1.health.nutrition.Nutrient.THIAMIN;
        if ("TOTAL_CARBOHYDRATE".equals(name)) return com.codename1.health.nutrition.Nutrient.TOTAL_CARBOHYDRATE;
        if ("TOTAL_FAT".equals(name)) return com.codename1.health.nutrition.Nutrient.TOTAL_FAT;
        if ("TRANS_FAT".equals(name)) return com.codename1.health.nutrition.Nutrient.TRANS_FAT;
        if ("VITAMIN_A".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_A;
        if ("VITAMIN_B12".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_B12;
        if ("VITAMIN_B6".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_B6;
        if ("VITAMIN_C".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_C;
        if ("VITAMIN_D".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_D;
        if ("VITAMIN_E".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_E;
        if ("VITAMIN_K".equals(name)) return com.codename1.health.nutrition.Nutrient.VITAMIN_K;
        if ("WATER".equals(name)) return com.codename1.health.nutrition.Nutrient.WATER;
        if ("ZINC".equals(name)) return com.codename1.health.nutrition.Nutrient.ZINC;
        throw unsupportedStaticField(com.codename1.health.nutrition.Nutrient.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("MEAL_BREAKFAST".equals(name)) return com.codename1.health.nutrition.NutritionSample.MEAL_BREAKFAST;
        if ("MEAL_DINNER".equals(name)) return com.codename1.health.nutrition.NutritionSample.MEAL_DINNER;
        if ("MEAL_LUNCH".equals(name)) return com.codename1.health.nutrition.NutritionSample.MEAL_LUNCH;
        if ("MEAL_SNACK".equals(name)) return com.codename1.health.nutrition.NutritionSample.MEAL_SNACK;
        if ("MEAL_UNKNOWN".equals(name)) return com.codename1.health.nutrition.NutritionSample.MEAL_UNKNOWN;
        throw unsupportedStaticField(com.codename1.health.nutrition.NutritionSample.class, name);
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
