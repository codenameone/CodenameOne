/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *****************************************************************************/
package bsh;

import bsh.cn1.CN1AccessRegistry;
import bsh.cn1.CN1LambdaSupport;
import com.codenameone.playground.PlaygroundContext;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static bsh.Capabilities.haveAccessibility;

/**
 * Reduced CN1-safe reflection facade backed by {@link CN1AccessRegistry}.
 */
public final class Reflect {
    public static final Object[] ZERO_ARGS = {};
    public static final Class<?>[] ZERO_TYPES = {};
    static final String GET_PREFIX = "get";
    static final String SET_PREFIX = "set";
    static final String IS_PREFIX = "is";
    static final Map<Class<?>, Object> instanceCache = new Hashtable<Class<?>, Object>();

    private Reflect() {
    }

    public static Object invokeObjectMethod(Object object, String methodName, Object[] args,
            Interpreter interpreter, CallStack callstack, Node callerInfo) throws EvalError {
        if (object instanceof This && !This.isExposedThisMethod(methodName)) {
            return ((This) object).invokeMethod(methodName, args, interpreter, callstack, callerInfo, false);
        }
        if (object == Primitive.NULL) {
            throw new UtilTargetError(new NullPointerException(
                    "Attempt to invoke method " + methodName + " on null value")).toEvalError(callerInfo, callstack);
        }
        if (object instanceof Primitive) {
            Primitive primitive = (Primitive) object;
            if ("equals".equals(methodName) && args != null && args.length == 1) {
                return Boolean.valueOf(primitive.equals(args[0]));
            }
            if ("getType".equals(methodName) || "getClass".equals(methodName)) {
                return primitive.getType();
            }
            if (object != Primitive.VOID && object != Primitive.NULL) {
                object = Primitive.unwrap(object);
            }
        }
        if (object instanceof Class) {
            Class<?> classObject = (Class<?>) object;
            if ("getName".equals(methodName) && (args == null || args.length == 0)) {
                return classObject.getName();
            }
            if ("getSimpleName".equals(methodName) && (args == null || args.length == 0)) {
                return classObject.getSimpleName();
            }
            try {
                return invokeStaticMethod(interpreter == null ? null : interpreter.getClassManager(),
                        classObject, methodName, args, callerInfo, callstack);
            } catch (ReflectError e) {
                throw new EvalError(e.getMessage(), callerInfo, callstack, e);
            } catch (UtilEvalError e) {
                throw e.toEvalError(callerInfo, callstack);
            }
        }
        if ("getClass".equals(methodName) && (args == null || args.length == 0)) {
            return object.getClass();
        }
        if ("hashCode".equals(methodName) && (args == null || args.length == 0)) {
            return Integer.valueOf(object.hashCode());
        }
        if ("toString".equals(methodName) && (args == null || args.length == 0)) {
            return object.toString();
        }
        if ("equals".equals(methodName) && args != null && args.length == 1) {
            return Boolean.valueOf(object.equals(args[0]));
        }
        if (PlaygroundContext.interceptMethodInvocation(object, methodName, unwrapArgs(args))) {
            return Primitive.VOID;
        }
        NameSpace prevNs = CN1LambdaSupport.getCurrentNameSpace();
        try {
            CN1LambdaSupport.setCurrentNameSpace(callstack.top());
            try {
                return Primitive.wrap(CN1AccessRegistry.getInstance().invoke(object, methodName, unwrapArgs(args)),
                        null);
            } catch (Exception ex) {
                throw new EvalError("Error invoking method " + methodName + ": " + ex.getMessage(),
                        callerInfo, callstack, ex);
            }
        } finally {
            CN1LambdaSupport.setCurrentNameSpace(prevNs);
        }
    }

    static TargetError targetErrorFromTargetException(Throwable e,
            String methodName, CallStack callstack, Node callerInfo) {
        Throwable target = e.getCause() == null ? e : e.getCause();
        return new TargetError("Method Invocation " + methodName, target, callerInfo, callstack, true);
    }

    public static Object invokeStaticMethod(BshClassManager bcm, Class<?> clas, String methodName,
            Object[] args, Node callerInfo) throws ReflectError, UtilEvalError {
        try {
            return Primitive.wrap(CN1AccessRegistry.getInstance().invokeStatic(clas, methodName, unwrapArgs(args)),
                    null);
        } catch (Exception ex) {
            throw new ReflectError("Static method " + StringUtil.methodString(methodName, Types.getTypes(args))
                    + " not found in class '" + clas.getName() + "'", ex);
        }
    }

    public static Object invokeStaticMethod(BshClassManager bcm, Class<?> clas, String methodName,
            Object[] args, Node callerInfo, CallStack callstack) throws ReflectError, UtilEvalError {
        NameSpace prevNs = CN1LambdaSupport.getCurrentNameSpace();
        try {
            if (callstack != null) {
                CN1LambdaSupport.setCurrentNameSpace(callstack.top());
            }
            try {
                return Primitive.wrap(CN1AccessRegistry.getInstance().invokeStatic(clas, methodName, unwrapArgs(args)),
                        null);
            } catch (Exception ex) {
                throw new ReflectError("Static method " + StringUtil.methodString(methodName, Types.getTypes(args))
                        + " not found in class '" + clas.getName() + "'", ex);
            }
        } finally {
            CN1LambdaSupport.setCurrentNameSpace(prevNs);
        }
    }

    public static Object getStaticFieldValue(Class<?> clas, String fieldName)
            throws UtilEvalError, ReflectError {
        try {
            return Primitive.wrap(CN1AccessRegistry.getInstance().getStaticField(clas, fieldName), null);
        } catch (Exception ex) {
            throw new ReflectError("No such field: " + fieldName + " for class: " + clas.getName(), ex);
        }
    }

    public static Object getObjectFieldValue(Object object, String fieldName)
            throws UtilEvalError, ReflectError {
        if (object instanceof Class) {
            return getStaticFieldValue((Class<?>) object, fieldName);
        }
        if (object instanceof This) {
            return ((This) object).namespace.getVariable(fieldName);
        }
        if (object == Primitive.NULL) {
            throw new UtilTargetError(new NullPointerException(
                    "Attempt to access field '" + fieldName + "' on null value"));
        }
        try {
            return Primitive.wrap(CN1AccessRegistry.getInstance().getField(object, fieldName), null);
        } catch (Exception ex) {
            if (hasObjectPropertyGetter(object.getClass(), fieldName)) {
                return getObjectProperty(object, fieldName);
            }
            throw new ReflectError("No such field: " + fieldName + " for class: " + object.getClass().getName(), ex);
        }
    }

    static LHS getLHSStaticField(Class<?> clas, String fieldName) throws UtilEvalError, ReflectError {
        if (isGeneratedClass(clas)) {
            NameSpace ns = getThisNS(clas);
            if (ns != null && ns.isClass) {
                Variable var = ns.getVariableImpl(fieldName, true);
                if (var != null && (!var.hasModifier("private") || haveAccessibility())) {
                    return new LHS(ns, fieldName);
                }
            }
        }
        return new LHS(clas, fieldName);
    }

    static LHS getLHSObjectField(Object object, String fieldName) throws UtilEvalError, ReflectError {
        if (object instanceof This) {
            return new LHS(((This) object).namespace, fieldName, false);
        }
        if (isGeneratedClass(object.getClass())) {
            NameSpace ns = getThisNS(object);
            if (ns != null && ns.isClass) {
                Variable var = ns.getVariableImpl(fieldName, true);
                if (var != null && (!var.hasModifier("private") || haveAccessibility())) {
                    return new LHS(ns, fieldName);
                }
            }
        }
        return new LHS(object, fieldName);
    }

    protected static Invocable resolveJavaField(Class<?> clas, String fieldName, boolean staticOnly)
            throws UtilEvalError {
        return null;
    }

    protected static Invocable resolveExpectedJavaField(Class<?> clas, String fieldName, boolean staticOnly)
            throws UtilEvalError, ReflectError {
        throw new ReflectError("Direct reflective field access is not supported in the Codename One BeanShell runtime.");
    }

    protected static Invocable resolveExpectedJavaMethod(BshClassManager bcm, Class<?> clas, Object object,
            String name, Object[] args, boolean staticOnly) throws ReflectError, UtilEvalError {
        throw new ReflectError("Direct reflective method resolution is not supported in the Codename One BeanShell runtime.");
    }

    protected static Invocable resolveJavaMethod(Class<?> clas, String name, Class<?>[] types, boolean staticOnly)
            throws UtilEvalError {
        return null;
    }

    static BshMethod staticMethodImport(Class<?> baseClass, String methodName) {
        return null;
    }

    static Object constructObject(Class<?> clas, Object[] args)
            throws ReflectError {
        return constructObject(clas, null, args);
    }

    static Object constructObject(Class<?> clas, Object object, Object[] args)
            throws ReflectError {
        if (clas == null) {
            return Primitive.NULL;
        }
        if (object != null) {
            throw new ReflectError("Inner class construction is not supported in the Codename One BeanShell runtime.");
        }
        if (clas.isInterface()) {
            throw new ReflectError("Can't create instance of an interface: " + clas);
        }
        try {
            return CN1AccessRegistry.getInstance().construct(clas, unwrapArgs(args));
        } catch (Exception ex) {
            throw new ReflectError("Constructor error: " + ex.getMessage(), ex);
        }
    }

    public static BshMethod findMostSpecificBshMethod(Class<?>[] idealMatch, List<BshMethod> methods) {
        int index = findMostSpecificBshMethodIndex(idealMatch, methods);
        return index < 0 ? null : methods.get(index);
    }

    public static int findMostSpecificBshMethodIndex(Class<?>[] idealMatch, List<BshMethod> methods) {
        int bestIndex = -1;
        Class<?>[] bestMatch = null;
        for (int i = 0; i < methods.size(); i++) {
            BshMethod method = methods.get(i);
            Class<?>[] target = method.getParameterTypes();
            if (!Types.isSignatureAssignable(idealMatch, target, Types.JAVA_BASE_ASSIGNABLE)) {
                continue;
            }
            if (bestIndex == -1
                    || Types.isSignatureAssignable(target, bestMatch, Types.JAVA_BASE_ASSIGNABLE)
                    && !Types.areSignaturesEqual(target, bestMatch)) {
                bestIndex = i;
                bestMatch = target;
            }
        }
        return bestIndex;
    }

    public static Invocable findMostSpecificInvocable(Class<?>[] idealMatch, List<Invocable> methods) {
        int index = findMostSpecificInvocableIndex(idealMatch, methods);
        return index < 0 ? null : methods.get(index);
    }

    public static int findMostSpecificInvocableIndex(Class<?>[] idealMatch, List<Invocable> methods) {
        int bestIndex = -1;
        Class<?>[] bestMatch = null;
        for (int i = 0; i < methods.size(); i++) {
            Invocable method = methods.get(i);
            Class<?>[] target = method.getParameterTypes();
            if (!Types.isSignatureAssignable(idealMatch, target, Types.JAVA_BASE_ASSIGNABLE)) {
                continue;
            }
            if (bestIndex == -1
                    || Types.isSignatureAssignable(target, bestMatch, Types.JAVA_BASE_ASSIGNABLE)
                    && !Types.areSignaturesEqual(target, bestMatch)) {
                bestIndex = i;
                bestMatch = target;
            }
        }
        return bestIndex;
    }

    static String accessorName(String prefix, String propName) {
        if (propName == null || propName.length() == 0) {
            return prefix;
        }
        char[] chars = propName.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return prefix + new String(chars);
    }

    public static Object getObjectProperty(Object obj, Object propName) throws ReflectError {
        if (Entry.class.isAssignableFrom(obj.getClass())) {
            Entry entry = (Entry) obj;
            if ("key".equals(propName)) {
                return entry.getKey();
            }
            if ("value".equals(propName)) {
                return entry.getValue();
            }
        }
        if (obj instanceof Entry[]) {
            Entry entry = getEntryForKey(propName, (Entry[]) obj);
            if (entry != null) {
                return entry.getValue();
            }
        }
        String name = String.valueOf(propName);
        try {
            return invokeObjectMethod(obj, accessorName(GET_PREFIX, name), ZERO_ARGS, null, null, null);
        } catch (EvalError ex) {
            if (Boolean.TRUE.equals(Boolean.FALSE)) {
                // unreachable placeholder to preserve structure
            }
        }
        try {
            return invokeObjectMethod(obj, accessorName(IS_PREFIX, name), ZERO_ARGS, null, null, null);
        } catch (EvalError ex) {
            // ignore and try direct field fallback
        }
        try {
            return getObjectFieldValue(obj, name);
        } catch (UtilEvalError e) {
            throw new ReflectError(e.getMessage(), e);
        }
    }

    public static Entry getEntryForKey(Object key, Entry[] entries) {
        if (entries == null) {
            return null;
        }
        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            Object entryKey = entry == null ? null : entry.getKey();
            if (key == null ? entryKey == null : key.equals(entryKey)) {
                return entry;
            }
        }
        return null;
    }

    public static Object setObjectProperty(Object obj, String propName, Object value)
            throws ReflectError {
        return setObjectProperty(obj, (Object) propName, value);
    }

    public static Object setObjectProperty(Object obj, Object propName, Object value)
            throws ReflectError {
        if (Entry.class.isAssignableFrom(obj.getClass())) {
            throw new ReflectError("Map.Entry values are read only in the Codename One BeanShell runtime.");
        }
        if (obj instanceof Class) {
            try {
                CN1AccessRegistry.getInstance().setStaticField((Class<?>) obj, String.valueOf(propName), Primitive.unwrap(value));
                return value;
            } catch (Exception ex) {
                throw new ReflectError("Unable to set static field " + propName + ": " + ex.getMessage(), ex);
            }
        }
        try {
            CN1AccessRegistry.getInstance().setField(obj, String.valueOf(propName), Primitive.unwrap(value));
            return value;
        } catch (Exception ex) {
            String setter = accessorName(SET_PREFIX, String.valueOf(propName));
            try {
                invokeObjectMethod(obj, setter, new Object[]{value}, null, null, null);
                return value;
            } catch (EvalError ee) {
                throw new ReflectError("Unable to set property " + propName + ": " + ex.getMessage(), ex);
            }
        }
    }

    static void logInvokeMethod(String msg, Invocable method, List<Object> params) {
        Interpreter.debug(msg, method, " with args ", params);
    }

    static void logInvokeMethod(String msg, Invocable method, Object[] args) {
        Interpreter.debug(msg, method, " with args ", StringUtil.typeString(args));
    }

    static void checkFoundStaticMethod(Invocable method, boolean staticOnly, Class<?> clas) throws UtilEvalError {
        if (staticOnly && method != null && !method.isStatic()) {
            throw new UtilEvalError("Cannot reach instance method from static context: "
                    + method.getName() + " in " + clas.getName());
        }
    }

    public static boolean isGeneratedClass(Class<?> type) {
        return false;
    }

    public static This getClassStaticThis(Class<?> clas, String className) {
        return null;
    }

    public static This getClassInstanceThis(Object instance, String className) {
        return null;
    }

    public static NameSpace getThisNS(Class<?> type) {
        return null;
    }

    public static NameSpace getThisNS(Object object) {
        if (object instanceof This) {
            return ((This) object).getNameSpace();
        }
        return null;
    }

    public static Variable getVariable(Class<?> type, String name) {
        return null;
    }

    public static Variable getVariable(Object object, String name) {
        return null;
    }

    public static Variable getVariable(NameSpace ns, String name) {
        if (ns == null) {
            return null;
        }
        try {
            return ns.getVariableImpl(name, true);
        } catch (UtilEvalError e) {
            return null;
        }
    }

    public static Variable[] getVariables(Class<?> type) {
        return new Variable[0];
    }

    public static Variable[] getVariables(Object object) {
        return new Variable[0];
    }

    public static Variable[] getVariables(NameSpace ns) {
        return ns == null ? new Variable[0] : ns.getVariables();
    }

    public static Variable[] getVariables(NameSpace ns, String[] names) {
        if (ns == null || names == null) {
            return new Variable[0];
        }
        Variable[] out = new Variable[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                out[i] = ns.getVariableImpl(names[i], true);
            } catch (UtilEvalError e) {
                out[i] = null;
            }
        }
        return out;
    }

    public static Variable[] getDeclaredVariables(Class<?> type) {
        return new Variable[0];
    }

    public static BshMethod[] getDeclaredMethods(Class<?> type) {
        return new BshMethod[0];
    }

    public static BshMethod getDeclaredMethod(Class<?> type, String name, Class<?>[] sig) {
        return null;
    }

    public static Modifiers getClassModifiers(Class<?> type) {
        return new Modifiers(Modifiers.CLASS);
    }

    public static boolean isPublic(Class<?> type) {
        return true;
    }

    public static boolean isPublic(Invocable member) {
        return member != null && (member.getModifiers() & 1) != 0;
    }

    public static boolean isPublic(Object member) {
        return true;
    }

    public static boolean isPrivate(Class<?> type) {
        return false;
    }

    public static boolean isPrivate(Invocable member) {
        return member != null && (member.getModifiers() & 2) != 0;
    }

    public static boolean isPrivate(Object member) {
        return false;
    }

    public static boolean isStatic(Class<?> type) {
        return false;
    }

    public static boolean isStatic(Invocable member) {
        return member != null && member.isStatic();
    }

    public static boolean isStatic(Object member) {
        return false;
    }

    public static boolean isPackageScope(Class<?> type) {
        return false;
    }

    public static boolean isPackageAccessible(Class<?> type) {
        return true;
    }

    public static boolean hasModifier(String name, int modifiers) {
        if ("public".equals(name)) {
            return (modifiers & 1) != 0;
        }
        if ("private".equals(name)) {
            return (modifiers & 2) != 0;
        }
        if ("protected".equals(name)) {
            return (modifiers & 4) != 0;
        }
        if ("static".equals(name)) {
            return (modifiers & 8) != 0;
        }
        if ("final".equals(name)) {
            return (modifiers & 16) != 0;
        }
        if ("synchronized".equals(name)) {
            return (modifiers & 32) != 0;
        }
        if ("abstract".equals(name)) {
            return (modifiers & 1024) != 0;
        }
        return false;
    }

    public static Object[] getEnumConstants(Class<?> enm) {
        return new Object[0];
    }

    public static Object invokeCompiledCommand(Class<?> commandClass, Object[] args, Interpreter interpreter,
            CallStack callstack, Node callerInfo, boolean forceClass) throws EvalError, UtilEvalError {
        throw new EvalError("Compiled BeanShell commands are not supported in the Codename One BeanShell runtime.",
                callerInfo, callstack);
    }

    public static Object invokeCompiledCommand(Class<?> commandClass, Object[] args, Interpreter interpreter,
            CallStack callstack, Node callerInfo) throws EvalError, UtilEvalError {
        return invokeCompiledCommand(commandClass, args, interpreter, callstack, callerInfo, false);
    }

    public static boolean hasObjectPropertyGetter(Class<?> cls, String propName) {
        return false;
    }

    public static boolean hasObjectPropertySetter(Class<?> cls, String propName) {
        return false;
    }

    public static String accessorName(String prefix, Object propName) {
        return accessorName(prefix, String.valueOf(propName));
    }

    public static BshMethod getMethod(NameSpace namespace, String name, Class<?>[] sig) {
        return getMethod(namespace, name, sig, false);
    }

    public static BshMethod getMethod(NameSpace namespace, String name, Class<?>[] sig, boolean declaredOnly) {
        if (namespace == null) {
            return null;
        }
        try {
            return namespace.getMethod(name, sig, declaredOnly);
        } catch (UtilEvalError e) {
            return null;
        }
    }

    public static String typeString(Object[] args) {
        return StringUtil.typeString(args);
    }

    private static Object[] unwrapArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return ZERO_ARGS;
        }
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = Primitive.unwrap(args[i]);
        }
        return out;
    }
}
