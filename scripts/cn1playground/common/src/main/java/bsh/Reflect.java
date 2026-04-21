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
                Object fallback = invokeWellKnownInterfaceMethod(object, methodName, args);
                if (fallback != FALLBACK_MISS) return fallback;
                throw new EvalError("Error invoking method " + methodName + ": " + ex.getMessage(),
                        callerInfo, callstack, ex);
            }
        } finally {
            CN1LambdaSupport.setCurrentNameSpace(prevNs);
        }
    }

    private static final Object FALLBACK_MISS = new Object();

    /** Last-resort dispatch for well-known Java interface methods that
     * don't have a registry entry on the concrete type. Covers the
     * common case of java.util.Map.Entry (implementation is
     * HashMap$Node / TreeMap$Entry / etc., none of which are in the
     * registry) and Collection.stream() (CN1's Collection backport
     * doesn't expose stream — we route to a minimal CN1StreamBridge
     * shim). Only uses direct virtual calls — no reflection. */
    @SuppressWarnings("unchecked")
    private static Object invokeWellKnownInterfaceMethod(
            Object object, String methodName, Object[] args) {
        if (object instanceof java.util.Map.Entry) {
            java.util.Map.Entry entry = (java.util.Map.Entry) object;
            if (args == null || args.length == 0) {
                if ("getKey".equals(methodName)) return entry.getKey();
                if ("getValue".equals(methodName)) return entry.getValue();
            } else if (args.length == 1 && "setValue".equals(methodName)) {
                return entry.setValue(unwrapArgs(args)[0]);
            }
        }
        if (object instanceof java.util.Collection
                && "stream".equals(methodName)
                && (args == null || args.length == 0)) {
            return new bsh.cn1.CN1StreamBridge((java.util.Collection<?>) object);
        }
        if (object instanceof bsh.cn1.CN1StreamBridge) {
            Object dispatched = dispatchStreamBridge((bsh.cn1.CN1StreamBridge) object, methodName, unwrapArgs(args));
            if (dispatched != FALLBACK_MISS) return dispatched;
        }
        return FALLBACK_MISS;
    }

    @SuppressWarnings("unchecked")
    private static Object dispatchStreamBridge(bsh.cn1.CN1StreamBridge sb, String methodName, Object[] unwrapped) {
        int n = unwrapped.length;
        if ("filter".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Predicate) {
            return sb.filter((java.util.function.Predicate<Object>) unwrapped[0]);
        }
        if ("map".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Function) {
            return sb.map((java.util.function.Function<Object, Object>) unwrapped[0]);
        }
        if ("flatMap".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Function) {
            return sb.flatMap((java.util.function.Function<Object, Object>) unwrapped[0]);
        }
        if ("peek".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Consumer) {
            return sb.peek((java.util.function.Consumer<Object>) unwrapped[0]);
        }
        if ("sorted".equals(methodName)) {
            if (n == 0) return sb.sorted();
            if (n == 1 && unwrapped[0] instanceof java.util.Comparator) {
                return sb.sorted((java.util.Comparator<Object>) unwrapped[0]);
            }
        }
        if ("distinct".equals(methodName) && n == 0) return sb.distinct();
        if ("limit".equals(methodName) && n == 1 && unwrapped[0] instanceof Number) {
            return sb.limit(((Number) unwrapped[0]).longValue());
        }
        if ("skip".equals(methodName) && n == 1 && unwrapped[0] instanceof Number) {
            return sb.skip(((Number) unwrapped[0]).longValue());
        }
        if ("forEach".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Consumer) {
            sb.forEach((java.util.function.Consumer<Object>) unwrapped[0]);
            return null;
        }
        if ("count".equals(methodName) && n == 0) return Long.valueOf(sb.count());
        if ("anyMatch".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Predicate) {
            return Boolean.valueOf(sb.anyMatch((java.util.function.Predicate<Object>) unwrapped[0]));
        }
        if ("allMatch".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Predicate) {
            return Boolean.valueOf(sb.allMatch((java.util.function.Predicate<Object>) unwrapped[0]));
        }
        if ("noneMatch".equals(methodName) && n == 1
                && unwrapped[0] instanceof java.util.function.Predicate) {
            return Boolean.valueOf(sb.noneMatch((java.util.function.Predicate<Object>) unwrapped[0]));
        }
        if ("findFirst".equals(methodName) && n == 0) return sb.findFirst();
        if ("findAny".equals(methodName) && n == 0) return sb.findAny();
        if ("min".equals(methodName)) {
            if (n == 0) return sb.min();
            if (n == 1 && unwrapped[0] instanceof java.util.Comparator) {
                return sb.min((java.util.Comparator<Object>) unwrapped[0]);
            }
        }
        if ("max".equals(methodName)) {
            if (n == 0) return sb.max();
            if (n == 1 && unwrapped[0] instanceof java.util.Comparator) {
                return sb.max((java.util.Comparator<Object>) unwrapped[0]);
            }
        }
        if ("reduce".equals(methodName)) {
            java.util.function.BinaryOperator<Object> binaryOp = null;
            int opIndex = -1;
            if (n == 1) opIndex = 0;
            else if (n == 2) opIndex = 1;
            if (opIndex >= 0) {
                if (unwrapped[opIndex] instanceof java.util.function.BinaryOperator) {
                    binaryOp = (java.util.function.BinaryOperator<Object>) unwrapped[opIndex];
                } else if (unwrapped[opIndex] instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
                    binaryOp = bsh.cn1.CN1LambdaSupport.asBinaryOperator(
                            (bsh.cn1.CN1LambdaSupport.LambdaValue) unwrapped[opIndex]);
                }
            }
            if (binaryOp != null) {
                if (n == 1) return sb.reduce(binaryOp);
                return sb.reduce(unwrapped[0], binaryOp);
            }
        }
        if ("toArray".equals(methodName) && n == 0) return sb.toArray();
        if ("toList".equals(methodName) && n == 0) return sb.toList();
        if ("collect".equals(methodName) && n == 1) return sb.collect(unwrapped[0]);
        if ("iterator".equals(methodName) && n == 0) return sb.iterator();
        return FALLBACK_MISS;
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
                String hint = suggestNearestMethod(clas, methodName);
                String msg = "Static method " + StringUtil.methodString(methodName, Types.getTypes(args))
                        + " not found in class '" + clas.getName() + "'";
                if (hint != null) msg += " (did you mean: " + hint + "?)";
                throw new ReflectError(msg, ex);
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
            // Nested class/interface fallback: treat `Outer.Inner` as a
            // reference to the nested Java class `Outer$Inner`. The CN1
            // registry indexes nested types by their full dotted name; try
            // that first, then fall back to a JVM-form class lookup.
            Class<?> nested = lookupNestedJavaClass(clas, fieldName);
            if (nested != null) {
                return new ClassIdentifier(nested);
            }
            // Repackage the registry's terse exception with a "did you
            // mean" suggestion. We re-throw an unchecked RuntimeException
            // so the suggestion-bearing message is what surfaces through
            // BSH's evaluation chain (which would otherwise swallow our
            // wrapped ReflectError on the static-field property fallback
            // path and surface the registry's original message instead).
            String hint = suggestNearestField(clas, fieldName);
            String original = ex.getMessage() == null ? "" : ex.getMessage();
            String msg = original.isEmpty() ? "No such field: " + fieldName : original;
            if (hint != null) msg += " (did you mean: " + hint + "?)";
            throw new RuntimeException(msg, ex);
        }
    }

    /** Compare the requested name against the registry's known field
     * names for this class and return a comma-separated list of the
     * closest matches (case-insensitive prefix or short edit
     * distance). Returns {@code null} when no known names are available
     * or no name is plausibly close. */
    static String suggestNearestField(Class<?> clas, String requested) {
        if (clas == null || requested == null) return null;
        bsh.cn1.CN1Access registry = CN1AccessRegistry.getInstance();
        if (!(registry instanceof bsh.cn1.GeneratedCN1Access)) return null;
        try {
            String[] names = ((bsh.cn1.GeneratedCN1Access) registry).getFieldNames(clas.getName());
            return pickSuggestions(names, requested, 3);
        } catch (Throwable ignore) {
            return null;
        }
    }

    static String suggestNearestMethod(Class<?> clas, String requested) {
        if (clas == null || requested == null) return null;
        bsh.cn1.CN1Access registry = CN1AccessRegistry.getInstance();
        if (!(registry instanceof bsh.cn1.GeneratedCN1Access)) return null;
        try {
            String[] names = ((bsh.cn1.GeneratedCN1Access) registry).getMethodSignatures(clas.getName());
            return pickSuggestions(names, requested, 3);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static String pickSuggestions(String[] candidates, String requested, int max) {
        if (candidates == null || candidates.length == 0) return null;
        String lowerReq = requested.toLowerCase();
        java.util.List<String> exact = new java.util.ArrayList<String>();
        java.util.List<String> close = new java.util.ArrayList<String>();
        for (String c : candidates) {
            if (c == null) continue;
            String lc = c.toLowerCase();
            if (lc.equals(lowerReq) || lc.startsWith(lowerReq) || lowerReq.startsWith(lc)) {
                exact.add(c);
            } else if (editDistance(lc, lowerReq) <= Math.max(2, requested.length() / 3)) {
                close.add(c);
            }
        }
        java.util.List<String> picks = exact.isEmpty() ? close : exact;
        if (picks.isEmpty()) return null;
        StringBuilder out = new StringBuilder();
        int n = Math.min(max, picks.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) out.append(", ");
            out.append(picks.get(i));
        }
        return out.toString();
    }

    /** Cheap iterative Levenshtein. The candidate sets are bounded by
     * the registry size for one class (typically dozens), so the
     * O(m·n) cost per name is fine. */
    private static int editDistance(String a, String b) {
        int la = a.length();
        int lb = b.length();
        if (la == 0) return lb;
        if (lb == 0) return la;
        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[lb];
    }

    private static Class<?> lookupNestedJavaClass(Class<?> outer, String nestedName) {
        // Try the CN1 registry first (uses dotted nested names).
        try {
            Class<?> viaRegistry = CN1AccessRegistry.getInstance()
                    .findClass(outer.getName() + "." + nestedName);
            if (viaRegistry != null) return viaRegistry;
        } catch (Exception ignore) {
        }
        // Fall back to the JVM's `$`-separated form, which works for any
        // public nested type regardless of registry coverage.
        try {
            return Class.forName(outer.getName() + "$" + nestedName);
        } catch (ClassNotFoundException ignore) {
            return null;
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
