/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.runtime;

import dart.core.DString;
import dart.core.DartIterable;
import dart.core.DartList;
import dart.core.DartMap;
import dart.core.NoSuchMethodError;
import dart.typed_data.Uint8List;

import java.util.Map;

/**
 * Member access through a {@code dynamic} receiver -- the code Dart runs for
 * {@code json['user']['name']}, {@code data.length} or {@code obj.save(x)} when the
 * static type says nothing.
 *
 * <p>A transpiled class answers for itself through {@link DynamicObject}. The runtime's
 * own representations of Dart's built-in values -- maps, lists, iterables, strings,
 * numbers, booleans and functions -- are dispatched here by hand, covering the members
 * decoded-JSON code reaches for. Anything else is Dart's NoSuchMethodError.</p>
 *
 * <p>No reflection is used: ParparVM has none.</p>
 */
public final class Dyn {

    /** Returned by a {@link DynamicObject} for a member it does not have. */
    public static final Object NO_MEMBER = new Object();

    private static final Object[] NONE = new Object[0];

    private Dyn() {
    }

    // ------------------------------------------------------------------ get

    public static Object get(Object o, String name) {
        if (o instanceof DynamicObject) {
            Object v = ((DynamicObject) o).$dget(name);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Map) {
            Object v = mapGet((Map<?, ?>) o, name);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Iterable) {
            Object v = iterableGet(DartIterable.wrap(asIterable(o)), name);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof String) {
            Object v = stringGet((String) o, name);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Number) {
            Object v = numberGet((Number) o, name);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Uint8List) {
            Uint8List b = (Uint8List) o;
            if (name.equals("length")) {
                return Long.valueOf(b.length());
            }
            if (name.equals("isEmpty")) {
                return Boolean.valueOf(b.isEmpty());
            }
            if (name.equals("isNotEmpty")) {
                return Boolean.valueOf(b.isNotEmpty());
            }
        }
        if (name.equals("hashCode")) {
            return Long.valueOf(o == null ? 0 : o.hashCode());
        }
        if (name.equals("runtimeType")) {
            return o == null ? null : o.getClass();
        }
        throw noSuch(o, name);
    }

    private static Object mapGet(Map<?, ?> m, String name) {
        switch (name) {
            case "length":
                return Long.valueOf(m.size());
            case "isEmpty":
                return Boolean.valueOf(m.isEmpty());
            case "isNotEmpty":
                return Boolean.valueOf(!m.isEmpty());
            case "keys":
                return DartIterable.wrap(copy(m.keySet()));
            case "values":
                return DartIterable.wrap(copy(m.values()));
            case "entries":
                if (m instanceof DartMap) {
                    return ((DartMap<?, ?>) m).entries();
                }
                return NO_MEMBER;
            default:
                return NO_MEMBER;
        }
    }

    private static Object iterableGet(DartIterable<Object> it, String name) {
        switch (name) {
            case "length":
                return Long.valueOf(it.length());
            case "isEmpty":
                return Boolean.valueOf(it.isEmpty());
            case "isNotEmpty":
                return Boolean.valueOf(it.isNotEmpty());
            case "first":
                return it.first();
            case "last":
                return it.last();
            case "reversed":
                return DartIterable.wrap(reversed(it));
            default:
                return NO_MEMBER;
        }
    }

    private static DartList<Object> copy(java.util.Collection<?> c) {
        DartList<Object> l = new DartList<Object>();
        for (Object o : c) {
            l.add(o);
        }
        return l;
    }

    private static DartList<Object> reversed(Iterable<Object> it) {
        DartList<Object> l = new DartList<Object>();
        for (Object o : it) {
            l.add(0, o);
        }
        return l;
    }

    private static Object stringGet(String s, String name) {
        switch (name) {
            case "length":
                return Long.valueOf(s.length());
            case "isEmpty":
                return Boolean.valueOf(s.isEmpty());
            case "isNotEmpty":
                return Boolean.valueOf(!s.isEmpty());
            default:
                return NO_MEMBER;
        }
    }

    private static Object numberGet(Number n, String name) {
        boolean dbl = n instanceof Double || n instanceof Float;
        switch (name) {
            case "isNaN":
                return Boolean.valueOf(dbl && Double.isNaN(n.doubleValue()));
            case "isNegative":
                return Boolean.valueOf(n.doubleValue() < 0);
            case "isFinite":
                return Boolean.valueOf(!dbl || !(Double.isNaN(n.doubleValue()) || Double.isInfinite(n.doubleValue())));
            case "isInfinite":
                return Boolean.valueOf(dbl && Double.isInfinite(n.doubleValue()));
            case "isEven":
                return Boolean.valueOf(n.longValue() % 2 == 0);
            case "isOdd":
                return Boolean.valueOf(n.longValue() % 2 != 0);
            case "sign":
                // Hand-written: ParparVM's class library (vm/JavaAPI) has neither
                // Math.signum nor Long.signum, and the native builds failed to link.
                // d itself is returned for 0.0, -0.0 and NaN, as Dart's sign does.
                if (dbl) {
                    double d = n.doubleValue();
                    return Double.valueOf(d > 0 ? 1.0 : d < 0 ? -1.0 : d);
                }
                long l = n.longValue();
                return Long.valueOf(l > 0 ? 1L : l < 0 ? -1L : 0L);
            default:
                return NO_MEMBER;
        }
    }

    // ------------------------------------------------------------------ set

    @SuppressWarnings("unchecked")
    public static Object set(Object o, String name, Object value) {
        if (o instanceof DynamicObject && ((DynamicObject) o).$dset(name, value)) {
            return value;
        }
        throw noSuch(o, name + "=");
    }

    // ------------------------------------------------------------------ index

    @SuppressWarnings("unchecked")
    public static Object index(Object o, Object key) {
        if (o instanceof Map) {
            return ((Map<Object, Object>) o).get(key);
        }
        if (o instanceof java.util.List && key instanceof Number) {
            java.util.List<Object> l = (java.util.List<Object>) o;
            long i = ((Number) key).longValue();
            dart.core.RangeError.checkValidIndex(i, l.size());
            return l.get((int) i);
        }
        if (o instanceof String && key instanceof Number) {
            return DString.idx((String) o, ((Number) key).longValue());
        }
        if (o instanceof Uint8List && key instanceof Number) {
            return Long.valueOf(((Uint8List) o).$index(((Number) key).longValue()));
        }
        if (o instanceof DynamicObject) {
            return call(o, "[]", new Object[] {key}, null);
        }
        throw noSuch(o, "[]");
    }

    @SuppressWarnings("unchecked")
    public static Object indexSet(Object o, Object key, Object value) {
        if (o instanceof Map) {
            ((Map<Object, Object>) o).put(key, value);
            return value;
        }
        if (o instanceof java.util.List && key instanceof Number) {
            java.util.List<Object> l = (java.util.List<Object>) o;
            long i = ((Number) key).longValue();
            dart.core.RangeError.checkValidIndex(i, l.size());
            l.set((int) i, value);
            return value;
        }
        if (o instanceof Uint8List && key instanceof Number && value instanceof Number) {
            ((Uint8List) o).$indexSet(((Number) key).longValue(), ((Number) value).longValue());
            return value;
        }
        if (o instanceof DynamicObject) {
            call(o, "[]=", new Object[] {key, value}, null);
            return value;
        }
        throw noSuch(o, "[]=");
    }

    // ------------------------------------------------------------------ call

    public static Object call(Object o, String name, Object[] args) {
        return call(o, name, args, null);
    }

    @SuppressWarnings("unchecked")
    public static Object call(Object o, String name, Object[] args, DartMap<String, Object> named) {
        if (args == null) {
            args = NONE;
        }
        if (o instanceof DynamicObject) {
            Object v = ((DynamicObject) o).$dcall(name, args, named);
            if (v != NO_MEMBER) {
                return v;
            }
            // A field holding a function: `obj.callback(x)`.
            Object f = ((DynamicObject) o).$dget(name);
            if (f != NO_MEMBER && f != null && isFunction(f)) {
                return invoke(f, args);
            }
        } else if (name.equals("call") && isFunction(o)) {
            return invoke(o, args);
        } else if (named != null && !named.isEmpty()
                && (o instanceof Map || o instanceof Iterable || o instanceof String || o instanceof Number)) {
            // Named arguments arrive apart from the positional ones; the built-ins that
            // take any are handled here, and any other name is a mismatched call.
            if (o instanceof Iterable && named.size() == 1 && args.length == 0 && name.equals("toList")
                    && named.containsKey("growable")) {
                return DartList.from((Iterable<Object>) o, !Boolean.FALSE.equals(named.get("growable")));
            }
            if (o instanceof Iterable && named.size() == 1 && args.length == 1 && named.containsKey("orElse")
                    && name.equals("firstWhere")) {
                return call(o, name, new Object[] {args[0], named.get("orElse")}, null);
            }
            throw noSuch(o, name + "()");
        } else if (args.length > builtinMaxArgs(name)
                && (o instanceof Map || o instanceof Iterable || o instanceof String || o instanceof Number)) {
            // Dart rejects a mismatched invocation before running anything: m.clear(42)
            // on a dynamic map is a NoSuchMethodError, not a cleared map.
            throw noSuch(o, name + "()");
        } else if (o instanceof Map) {
            Object v = mapCall((Map<Object, Object>) o, name, args);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Iterable) {
            Object v = iterableCall(o, name, args);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof String) {
            Object v = stringCall((String) o, name, args);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Number) {
            Object v = numberCall((Number) o, name, args);
            if (v != NO_MEMBER) {
                return v;
            }
        } else if (o instanceof Uint8List && name.equals("toList")) {
            return ((Uint8List) o).toList();
        }
        if (args.length == 0 && name.equals("toString")) {
            return DartRuntime.str(o);
        }
        if (args.length == 1 && name.equals("==")) {
            return Boolean.valueOf(o == null ? args[0] == null : o.equals(args[0]));
        }
        throw noSuch(o, name + "()");
    }

    /// The most positional arguments the built-in member {@code name} accepts on a Map,
    /// Iterable, String or num; the dispatchers below only check that required ones
    /// are present. Unknown names answer Integer.MAX_VALUE and reach NO_MEMBER as before.
    private static int builtinMaxArgs(String name) {
        switch (name) {
            case "clear": case "removeLast": case "toList": case "toSet": case "cast": case "toLowerCase":
            case "toUpperCase": case "trim": case "toString": case "toInt": case "toDouble": case "round":
            case "floor": case "ceil": case "abs":
                return 0;
            case "containsKey": case "containsValue": case "remove": case "addAll": case "forEach": case "add":
            case "removeAt": case "sort": case "map": case "where": case "any": case "every":
            case "join": case "elementAt": case "skip": case "take": case "endsWith":
            case "split": case "codeUnitAt": case "compareTo": case "toStringAsFixed":
                return 1;
            case "putIfAbsent": case "insert": case "indexOf": case "sublist": case "firstWhere": case "fold":
            case "substring": case "replaceAll": case "padLeft": case "clamp": case "contains": case "startsWith":
                return 2;
            default:
                return Integer.MAX_VALUE;
        }
    }

    private static Object mapCall(Map<Object, Object> m, String name, Object[] a) {
        switch (name) {
            case "containsKey":
                return Boolean.valueOf(m.containsKey(arg(a, 0)));
            case "containsValue":
                return Boolean.valueOf(m.containsValue(arg(a, 0)));
            case "remove":
                return m.remove(arg(a, 0));
            case "clear":
                m.clear();
                return null;
            case "putIfAbsent": {
                Object k = arg(a, 0);
                if (!m.containsKey(k)) {
                    m.put(k, invoke(arg(a, 1), NONE));
                }
                return m.get(k);
            }
            case "addAll":
                if (arg(a, 0) instanceof Map) {
                    m.putAll((Map<?, ?>) arg(a, 0));
                }
                return null;
            case "forEach": {
                Object f = arg(a, 0);
                for (Object o : copy(m.entrySet())) {
                    Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                    invoke(f, new Object[] {e.getKey(), e.getValue()});
                }
                return null;
            }
            case "cast":
                return m;
            default:
                return NO_MEMBER;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object iterableCall(Object o, String name, Object[] a) {
        DartIterable<Object> it = DartIterable.wrap(asIterable(o));
        if (o instanceof DartList) {
            DartList<Object> l = (DartList<Object>) o;
            switch (name) {
                case "add":
                    l.add(arg(a, 0));
                    return null;
                case "addAll":
                    l.addAllIterable(asIterable(arg(a, 0)));
                    return null;
                case "remove":
                    return Boolean.valueOf(l.removeValue(arg(a, 0)));
                case "removeAt":
                    return l.removeAt(num(arg(a, 0)).longValue());
                case "removeLast":
                    return l.removeLast();
                case "insert":
                    l.insert(num(arg(a, 0)).longValue(), arg(a, 1));
                    return null;
                case "clear":
                    l.clear();
                    return null;
                case "indexOf":
                    return Long.valueOf(l.indexOfDart(arg(a, 0)));
                case "sublist":
                    return a.length > 1 && a[1] != null ? l.sublist(num(a[0]).longValue(), num(a[1]).longValue())
                            : l.sublist(num(arg(a, 0)).longValue());
                case "sort":
                    if (a.length == 0 || a[0] == null) {
                        l.sortDefault();
                    } else {
                        final Object cmp = a[0];
                        l.sort(new Funcs.Func2<Object, Object, Long>() {
                            @Override
                            public Long call(Object x, Object y) {
                                return Long.valueOf(num(invoke(cmp, new Object[] {x, y})).longValue());
                            }
                        });
                    }
                    return null;
                default:
                    break;
            }
        }
        switch (name) {
            case "map":
                return it.map(fn1(arg(a, 0)));
            case "where":
                return it.where(pred(arg(a, 0)));
            case "any":
                return Boolean.valueOf(it.any(pred(arg(a, 0))));
            case "every":
                return Boolean.valueOf(it.every(pred(arg(a, 0))));
            case "contains":
                return Boolean.valueOf(it.contains(arg(a, 0)));
            case "toList":
                return it.toList();
            case "toSet":
                return it.toSet();
            case "join":
                return a.length == 0 || a[0] == null ? it.join() : it.join((String) a[0]);
            case "forEach": {
                Object f = arg(a, 0);
                for (Object e : it.toList()) {
                    invoke(f, new Object[] {e});
                }
                return null;
            }
            case "elementAt":
                return it.elementAt(num(arg(a, 0)).longValue());
            case "firstWhere": {
                final Object orElse = a.length > 1 ? a[1] : null;
                for (Object e : it) {
                    if (truthy(invoke(arg(a, 0), new Object[] {e}))) {
                        return e;
                    }
                }
                if (orElse != null) {
                    return invoke(orElse, NONE);
                }
                throw new dart.core.StateError("No element");
            }
            case "fold": {
                Object acc = arg(a, 0);
                for (Object e : it) {
                    acc = invoke(arg(a, 1), new Object[] {acc, e});
                }
                return acc;
            }
            case "skip":
                return it.skip(num(arg(a, 0)).longValue());
            case "take":
                return it.take(num(arg(a, 0)).longValue());
            case "cast":
                return o;
            default:
                return NO_MEMBER;
        }
    }

    private static Object stringCall(String s, String name, Object[] a) {
        switch (name) {
            case "toLowerCase":
                return DString.toLowerCase(s);
            case "toUpperCase":
                return DString.toUpperCase(s);
            case "trim":
                return DString.trim(s);
            case "contains":
                return Boolean.valueOf(s.contains(String.valueOf(arg(a, 0))));
            case "startsWith":
                return Boolean.valueOf(s.startsWith(String.valueOf(arg(a, 0))));
            case "endsWith":
                return Boolean.valueOf(s.endsWith(String.valueOf(arg(a, 0))));
            case "split":
                return DString.split(s, String.valueOf(arg(a, 0)));
            case "substring":
                return a.length > 1 && a[1] != null ? DString.substring(s, num(a[0]).longValue(), num(a[1]).longValue())
                        : DString.substring(s, num(arg(a, 0)).longValue());
            case "replaceAll":
                return DString.replaceAll(s, String.valueOf(arg(a, 0)), String.valueOf(arg(a, 1)));
            case "indexOf":
                return Long.valueOf(DString.indexOf(s, String.valueOf(arg(a, 0))));
            case "codeUnitAt":
                return Long.valueOf(DString.codeUnitAt(s, num(arg(a, 0)).longValue()));
            case "padLeft":
                return a.length > 1 && a[1] != null ? DString.padLeft(s, num(a[0]).longValue(), (String) a[1])
                        : DString.padLeft(s, num(arg(a, 0)).longValue());
            case "compareTo":
                return Long.valueOf(DString.compareTo(s, (String) arg(a, 0)));
            case "toString":
                return s;
            default:
                return NO_MEMBER;
        }
    }

    private static Object numberCall(Number n, String name, Object[] a) {
        boolean dbl = n instanceof Double || n instanceof Float;
        double d = n.doubleValue();
        switch (name) {
            // The DartRuntime helpers refuse NaN and the infinities with UnsupportedError,
            // as Dart does; a bare (long) cast fabricated 0 or a saturated long.
            case "toInt":
                return Long.valueOf(dbl ? DartRuntime.toInt(d) : n.longValue());
            case "toDouble":
                return Double.valueOf(d);
            case "round":
                return Long.valueOf(dbl ? DartRuntime.round(d) : n.longValue());
            case "floor":
                return Long.valueOf(dbl ? DartRuntime.floor(d) : n.longValue());
            case "ceil":
                return Long.valueOf(dbl ? DartRuntime.ceil(d) : n.longValue());
            case "abs":
                return dbl ? (Object) Double.valueOf(Math.abs(d)) : (Object) Long.valueOf(Math.abs(n.longValue()));
            case "toStringAsFixed":
                return DartRuntime.toStringAsFixed(d, num(arg(a, 0)).longValue());
            case "compareTo":
                // Dart's total order: NaN after everything, -0.0 before 0.0
                return Long.valueOf(dart.core.DartComparable.compare(n, num(arg(a, 0))));
            case "clamp": {
                Number lo = num(arg(a, 0));
                Number hi = num(arg(a, 1));
                if (d < lo.doubleValue()) {
                    return lo;
                }
                return d > hi.doubleValue() ? hi : n;
            }
            case "toString":
                return DartRuntime.str(n);
            default:
                return NO_MEMBER;
        }
    }

    // ------------------------------------------------------------------ functions

    public static boolean isFunction(Object f) {
        return f instanceof Funcs.Func0 || f instanceof Funcs.Func1 || f instanceof Funcs.Func2
                || f instanceof Funcs.Func3 || f instanceof Funcs.Func4 || f instanceof Funcs.Func5
                || f instanceof Funcs.VoidFunc0 || f instanceof Funcs.VoidFunc1 || f instanceof Funcs.VoidFunc2
                || f instanceof Funcs.VoidFunc3 || f instanceof Funcs.VoidFunc4 || f instanceof Funcs.VoidFunc5;
    }

    /** Calls a function value whose static type is dynamic or Function. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object invoke(Object f, Object... a) {
        if (a == null) {
            a = NONE;
        }
        switch (a.length) {
            case 0:
                if (f instanceof Funcs.Func0) {
                    return ((Funcs.Func0) f).call();
                }
                if (f instanceof Funcs.VoidFunc0) {
                    ((Funcs.VoidFunc0) f).call();
                    return null;
                }
                break;
            case 1:
                if (f instanceof Funcs.Func1) {
                    return ((Funcs.Func1) f).call(a[0]);
                }
                if (f instanceof Funcs.VoidFunc1) {
                    ((Funcs.VoidFunc1) f).call(a[0]);
                    return null;
                }
                break;
            case 2:
                if (f instanceof Funcs.Func2) {
                    return ((Funcs.Func2) f).call(a[0], a[1]);
                }
                if (f instanceof Funcs.VoidFunc2) {
                    ((Funcs.VoidFunc2) f).call(a[0], a[1]);
                    return null;
                }
                break;
            case 3:
                if (f instanceof Funcs.Func3) {
                    return ((Funcs.Func3) f).call(a[0], a[1], a[2]);
                }
                if (f instanceof Funcs.VoidFunc3) {
                    ((Funcs.VoidFunc3) f).call(a[0], a[1], a[2]);
                    return null;
                }
                break;
            case 4:
                if (f instanceof Funcs.Func4) {
                    return ((Funcs.Func4) f).call(a[0], a[1], a[2], a[3]);
                }
                if (f instanceof Funcs.VoidFunc4) {
                    ((Funcs.VoidFunc4) f).call(a[0], a[1], a[2], a[3]);
                    return null;
                }
                break;
            case 5:
                if (f instanceof Funcs.Func5) {
                    return ((Funcs.Func5) f).call(a[0], a[1], a[2], a[3], a[4]);
                }
                if (f instanceof Funcs.VoidFunc5) {
                    ((Funcs.VoidFunc5) f).call(a[0], a[1], a[2], a[3], a[4]);
                    return null;
                }
                break;
            default:
                break;
        }
        throw new NoSuchMethodError("Closure call with mismatched arguments: " + a.length + " argument(s)");
    }

    private static Funcs.Func1<Object, Object> fn1(final Object f) {
        return new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object x) {
                return invoke(f, new Object[] {x});
            }
        };
    }

    private static Funcs.Func1<Object, Boolean> pred(final Object f) {
        return new Funcs.Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object x) {
                return Boolean.valueOf(truthy(invoke(f, new Object[] {x})));
            }
        };
    }

    // ------------------------------------------------------------------ helpers

    private static boolean truthy(Object v) {
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        throw new dart.core.TypeError("type '" + typeName(v) + "' is not a subtype of type 'bool'");
    }

    private static Number num(Object v) {
        if (v instanceof Number) {
            return (Number) v;
        }
        throw new dart.core.TypeError("type '" + typeName(v) + "' is not a subtype of type 'num'");
    }

    private static Object arg(Object[] a, int i) {
        if (i >= a.length) {
            throw new NoSuchMethodError("Missing positional argument " + (i + 1));
        }
        return a[i];
    }

    @SuppressWarnings("unchecked")
    private static Iterable<Object> asIterable(Object o) {
        if (o instanceof Iterable) {
            return (Iterable<Object>) o;
        }
        throw new dart.core.TypeError("type '" + typeName(o) + "' is not a subtype of type 'Iterable<dynamic>'");
    }

    private static String typeName(Object o) {
        return o == null ? "Null" : o.getClass().getName();
    }

    /**
     * Validates a dynamic call to a transpiled method against its declaration, as
     * Dart does before invoking: {@code minPos..maxPos} positional arguments, only the
     * {@code named} parameters, and every one of {@code requiredNamed}. Generated
     * {@code $dcall} code calls this; a null array means none.
     */
    public static void checkArgs(Object o, String member, Object[] a, java.util.Map<String, Object> n,
            int minPos, int maxPos, String[] named, String[] requiredNamed) {
        int count = a == null ? 0 : a.length;
        if (count < minPos || count > maxPos) {
            throw noSuch(o, member);
        }
        if (n != null) {
            for (String k : n.keySet()) {
                if (!contains(named, k)) {
                    throw noSuch(o, member);
                }
            }
        }
        if (requiredNamed != null) {
            for (String k : requiredNamed) {
                if (n == null || !n.containsKey(k)) {
                    throw noSuch(o, member);
                }
            }
        }
    }

    private static boolean contains(String[] names, String k) {
        if (names == null) {
            return false;
        }
        for (String s : names) {
            if (s.equals(k)) {
                return true;
            }
        }
        return false;
    }

    private static NoSuchMethodError noSuch(Object o, String member) {
        if (o == null) {
            return new NoSuchMethodError("The getter or method '" + member + "' was called on null.");
        }
        return new NoSuchMethodError("Class '" + typeName(o) + "' has no instance member '" + member + "'.");
    }
}
