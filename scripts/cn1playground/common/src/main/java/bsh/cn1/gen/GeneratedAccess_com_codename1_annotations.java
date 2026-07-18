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

public final class GeneratedAccess_com_codename1_annotations {
    private GeneratedAccess_com_codename1_annotations() {
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
        if ("Async".equals(simpleName)) {
            return com.codename1.annotations.Async.class;
        }
        if ("Execute".equals(simpleName)) {
            return com.codename1.annotations.Async.Execute.class;
        }
        if ("Schedule".equals(simpleName)) {
            return com.codename1.annotations.Async.Schedule.class;
        }
        if ("Bind".equals(simpleName)) {
            return com.codename1.annotations.Bind.class;
        }
        if ("Bindable".equals(simpleName)) {
            return com.codename1.annotations.Bindable.class;
        }
        if ("Column".equals(simpleName)) {
            return com.codename1.annotations.Column.class;
        }
        if ("Concrete".equals(simpleName)) {
            return com.codename1.annotations.Concrete.class;
        }
        if ("DbTransient".equals(simpleName)) {
            return com.codename1.annotations.DbTransient.class;
        }
        if ("DisableDebugInfo".equals(simpleName)) {
            return com.codename1.annotations.DisableDebugInfo.class;
        }
        if ("DisableNullChecksAndArrayBoundsChecks".equals(simpleName)) {
            return com.codename1.annotations.DisableNullChecksAndArrayBoundsChecks.class;
        }
        if ("Email".equals(simpleName)) {
            return com.codename1.annotations.Email.class;
        }
        if ("Entity".equals(simpleName)) {
            return com.codename1.annotations.Entity.class;
        }
        if ("ExistIn".equals(simpleName)) {
            return com.codename1.annotations.ExistIn.class;
        }
        if ("Fused".equals(simpleName)) {
            return com.codename1.annotations.Fused.class;
        }
        if ("Id".equals(simpleName)) {
            return com.codename1.annotations.Id.class;
        }
        if ("JsonIgnore".equals(simpleName)) {
            return com.codename1.annotations.JsonIgnore.class;
        }
        if ("JsonProperty".equals(simpleName)) {
            return com.codename1.annotations.JsonProperty.class;
        }
        if ("Length".equals(simpleName)) {
            return com.codename1.annotations.Length.class;
        }
        if ("Mapped".equals(simpleName)) {
            return com.codename1.annotations.Mapped.class;
        }
        if ("Numeric".equals(simpleName)) {
            return com.codename1.annotations.Numeric.class;
        }
        if ("Regex".equals(simpleName)) {
            return com.codename1.annotations.Regex.class;
        }
        if ("Required".equals(simpleName)) {
            return com.codename1.annotations.Required.class;
        }
        if ("Route".equals(simpleName)) {
            return com.codename1.annotations.Route.class;
        }
        if ("Routes".equals(simpleName)) {
            return com.codename1.annotations.Route.Routes.class;
        }
        if ("RouteParam".equals(simpleName)) {
            return com.codename1.annotations.RouteParam.class;
        }
        if ("Url".equals(simpleName)) {
            return com.codename1.annotations.Url.class;
        }
        if ("Validate".equals(simpleName)) {
            return com.codename1.annotations.Validate.class;
        }
        if ("XmlAttribute".equals(simpleName)) {
            return com.codename1.annotations.XmlAttribute.class;
        }
        if ("XmlElement".equals(simpleName)) {
            return com.codename1.annotations.XmlElement.class;
        }
        if ("XmlRoot".equals(simpleName)) {
            return com.codename1.annotations.XmlRoot.class;
        }
        if ("XmlTransient".equals(simpleName)) {
            return com.codename1.annotations.XmlTransient.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedStatic(type, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.annotations.Bind) {
            try {
                return invoke0((com.codename1.annotations.Bind) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Column) {
            try {
                return invoke1((com.codename1.annotations.Column) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Concrete) {
            try {
                return invoke2((com.codename1.annotations.Concrete) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Email) {
            try {
                return invoke3((com.codename1.annotations.Email) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Entity) {
            try {
                return invoke4((com.codename1.annotations.Entity) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.ExistIn) {
            try {
                return invoke5((com.codename1.annotations.ExistIn) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Id) {
            try {
                return invoke6((com.codename1.annotations.Id) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.JsonProperty) {
            try {
                return invoke7((com.codename1.annotations.JsonProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Length) {
            try {
                return invoke8((com.codename1.annotations.Length) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Numeric) {
            try {
                return invoke9((com.codename1.annotations.Numeric) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Regex) {
            try {
                return invoke10((com.codename1.annotations.Regex) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Required) {
            try {
                return invoke11((com.codename1.annotations.Required) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Route) {
            try {
                return invoke12((com.codename1.annotations.Route) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Route.Routes) {
            try {
                return invoke13((com.codename1.annotations.Route.Routes) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.RouteParam) {
            try {
                return invoke14((com.codename1.annotations.RouteParam) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Url) {
            try {
                return invoke15((com.codename1.annotations.Url) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.Validate) {
            try {
                return invoke16((com.codename1.annotations.Validate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.XmlAttribute) {
            try {
                return invoke17((com.codename1.annotations.XmlAttribute) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.XmlElement) {
            try {
                return invoke18((com.codename1.annotations.XmlElement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.annotations.XmlRoot) {
            try {
                return invoke19((com.codename1.annotations.XmlRoot) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.annotations.Bind typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("attr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.attr();
            }
        }
        if ("getter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getter();
            }
        }
        if ("name".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.name();
            }
        }
        if ("setter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.setter();
            }
        }
        if ("twoWay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.twoWay();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.annotations.Column typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("name".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.name();
            }
        }
        if ("nullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nullable();
            }
        }
        if ("type".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.type();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.annotations.Concrete typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("linux".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.linux();
            }
        }
        if ("name".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.name();
            }
        }
        if ("win".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.win();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.annotations.Email typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.annotations.Entity typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("table".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.table();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.annotations.ExistIn typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("caseSensitive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.caseSensitive();
            }
        }
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.annotations.Id typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("autoIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.autoIncrement();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.annotations.JsonProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.annotations.Length typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        if ("min".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.min();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.annotations.Numeric typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("decimal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.decimal();
            }
        }
        if ("max".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.max();
            }
        }
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        if ("min".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.min();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.annotations.Regex typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        if ("pattern".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pattern();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.annotations.Required typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.annotations.Route typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.annotations.Route.Routes typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.annotations.RouteParam typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("required".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.required();
            }
        }
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.annotations.Url typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("message".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.message();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.annotations.Validate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.annotations.XmlAttribute typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.annotations.XmlElement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.annotations.XmlRoot typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("value".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.value();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        throw unsupportedStaticField(type, name);
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
