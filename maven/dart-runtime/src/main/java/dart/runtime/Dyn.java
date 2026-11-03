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
        if (o instanceof Enum && name.equals("index")) {
            // Dart's Enum.index, a real instance member of every enum value, so a dynamic
            // receiver answers it. `name` is not: it is the EnumName extension, which
            // Dart resolves statically, so a dynamic `.name` is a NoSuchMethodError there too.
            return Long.valueOf(((Enum<?>) o).ordinal());
        }
        Object core = coreTypeGet(o, name);
        if (core != NO_MEMBER) {
            return core;
        }
        if (name.equals("hashCode")) {
            return Long.valueOf(o == null ? 0 : o.hashCode());
        }
        if (name.equals("runtimeType")) {
            // The reified type (`List<dynamic>`, a generic class's arguments), as a typed
            // receiver's `runtimeType` answers it; a bare class token loses the arguments.
            // `null.runtimeType` is Null, a Type like any other.
            return TypeWitness.runtimeType(o);
        }
        if (o instanceof DynamicObject) {
            Object v = ((DynamicObject) o).noSuchMethod(dart.core.Invocation.getter(name));
            if (v != NO_MEMBER) {
                return v;
            }
        }
        throw noSuch(o, name);
    }

    /**
     * Getters of the dart:core value classes the runtime implements as plain Java classes
     * (no DynamicObject table): DateTime, Duration and Uri. Reached when such a value's
     * static type is dynamic, e.g. {@code (clock ?? DateTime.now)().year}.
     */
    private static Object coreTypeGet(Object o, String name) {
        if (o instanceof dart.core.RegExpMatch) {
            // allMatches/firstMatch answer these; a dynamic receiver reads Match's getters.
            dart.core.RegExpMatch m = (dart.core.RegExpMatch) o;
            switch (name) {
                case "start": return Long.valueOf(m.start());
                case "end": return Long.valueOf(m.end());
                case "groupCount": return Long.valueOf(m.groupCount());
                case "input": return m.input();
                case "pattern": return m.pattern();
                default: break;
            }
        }
        if (o instanceof dart.core.DateTime) {
            dart.core.DateTime d = (dart.core.DateTime) o;
            switch (name) {
                case "year": return Long.valueOf(d.year());
                case "month": return Long.valueOf(d.month());
                case "day": return Long.valueOf(d.day());
                case "hour": return Long.valueOf(d.hour());
                case "minute": return Long.valueOf(d.minute());
                case "second": return Long.valueOf(d.second());
                case "millisecond": return Long.valueOf(d.millisecond());
                case "microsecond": return Long.valueOf(d.microsecond());
                case "weekday": return Long.valueOf(d.weekday());
                case "millisecondsSinceEpoch": return Long.valueOf(d.millisecondsSinceEpoch());
                case "microsecondsSinceEpoch": return Long.valueOf(d.microsecondsSinceEpoch());
                case "isUtc": return Boolean.valueOf(d.isUtc());
                default: return NO_MEMBER;
            }
        }
        if (o instanceof dart.core.Duration) {
            dart.core.Duration d = (dart.core.Duration) o;
            switch (name) {
                case "inMicroseconds": return Long.valueOf(d.inMicroseconds());
                case "inMilliseconds": return Long.valueOf(d.inMilliseconds());
                case "inSeconds": return Long.valueOf(d.inSeconds());
                case "inMinutes": return Long.valueOf(d.inMinutes());
                case "inHours": return Long.valueOf(d.inHours());
                case "inDays": return Long.valueOf(d.inDays());
                case "isNegative": return Boolean.valueOf(d.isNegative());
                default: return NO_MEMBER;
            }
        }
        if (o instanceof dart.core.DartUri) {
            dart.core.DartUri u = (dart.core.DartUri) o;
            switch (name) {
                case "scheme": return u.scheme();
                case "host": return u.host();
                case "port": return Long.valueOf(u.port());
                case "path": return u.path();
                case "query": return u.query();
                case "fragment": return u.fragment();
                case "pathSegments": return u.pathSegments();
                case "hasScheme": return Boolean.valueOf(u.hasScheme());
                case "queryParameters": return u.queryParameters();
                default: return NO_MEMBER;
            }
        }
        if (o instanceof dart.core.StringBuffer) {
            dart.core.StringBuffer b = (dart.core.StringBuffer) o;
            switch (name) {
                case "length": return Long.valueOf(b.length());
                default: return NO_MEMBER;
            }
        }
        return NO_MEMBER;
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
        DartList<Object> l = new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC);
        for (Object o : c) {
            l.add(o);
        }
        return l;
    }

    private static DartList<Object> reversed(Iterable<Object> it) {
        DartList<Object> l = new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC);
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
        if (o instanceof DynamicObject
                && ((DynamicObject) o).noSuchMethod(dart.core.Invocation.setter(name, value)) != NO_MEMBER) {
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
            if (!(args.length == 0 && name.equals("toString")) && !(args.length == 1 && name.equals("=="))) {
                // No member: Dart hands the call to noSuchMethod before throwing.
                Object r = ((DynamicObject) o).noSuchMethod(dart.core.Invocation.method(name, args, named));
                if (r != NO_MEMBER) {
                    return r;
                }
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
        } else if (o instanceof dart.core.DateTime || o instanceof dart.core.RegExp
                || o instanceof dart.core.RegExpMatch
                || o instanceof dart.core.StringBuffer) {
            Object v = coreTypeCall(o, name, args);
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
            case "reduce": case "expand": case "join": case "elementAt": case "skip": case "take": case "endsWith":
            case "split": case "codeUnitAt": case "compareTo": case "toStringAsFixed": case "remainder":
                return 1;
            case "putIfAbsent": case "insert": case "indexOf": case "sublist": case "firstWhere": case "fold":
            case "substring": case "replaceAll": case "padLeft": case "clamp": case "contains": case "startsWith":
            case "lastIndexOf": case "allMatches":
                return 2;
            case "replaceFirst":
                return 3;
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
                Object f = fnArg(arg(a, 0), "(dynamic, dynamic) => void");
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
                        final Object cmp = fnArg(a[0], "(dynamic, dynamic) => int");
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
        if (o instanceof dart.core.DartSet) {
            // Set's own mutators; add checks the element against the set's witness.
            dart.core.DartSet<Object> st = (dart.core.DartSet<Object>) o;
            switch (name) {
                case "add":
                    return Boolean.valueOf(st.add(arg(a, 0)));
                case "addAll":
                    st.addAllIterable(asIterable(arg(a, 0)));
                    return null;
                case "remove":
                    return Boolean.valueOf(st.remove(arg(a, 0)));
                case "clear":
                    st.clear();
                    return null;
                default:
                    break;
            }
        }
        switch (name) {
            case "map":
                return it.map(fn1(fnArg(arg(a, 0), "(dynamic) => dynamic")));
            case "where":
                return it.where(pred(fnArg(arg(a, 0), "(dynamic) => bool")));
            case "any":
                return Boolean.valueOf(it.any(pred(fnArg(arg(a, 0), "(dynamic) => bool"))));
            case "every":
                return Boolean.valueOf(it.every(pred(fnArg(arg(a, 0), "(dynamic) => bool"))));
            case "contains":
                return Boolean.valueOf(it.contains(arg(a, 0)));
            case "toList":
                return it.toList();
            case "toSet":
                return it.toSet();
            case "join":
                return a.length == 0 || a[0] == null ? it.join() : it.join(DartRuntime.as(a[0], String.class, false, "String"));
            case "forEach": {
                Object f = fnArg(arg(a, 0), "(dynamic) => void");
                for (Object e : it.toList()) {
                    invoke(f, new Object[] {e});
                }
                return null;
            }
            case "elementAt":
                return it.elementAt(num(arg(a, 0)).longValue());
            case "firstWhere": {
                fnArg(arg(a, 0), "(dynamic) => bool");
                final Object orElse = a.length > 1 && a[1] != null ? fnArg(a[1], "() => dynamic") : null;
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
                fnArg(arg(a, 1), "(dynamic, dynamic) => dynamic");
                Object acc = arg(a, 0);
                for (Object e : it) {
                    acc = invoke(arg(a, 1), new Object[] {acc, e});
                }
                return acc;
            }
            case "reduce": {
                fnArg(arg(a, 0), "(dynamic, dynamic) => dynamic");
                // Missing, a dynamic list's reduce -- a top-level `final xs = ws.map(...)
                // .toList()` is one -- threw NoSuchMethodError though fold worked.
                java.util.Iterator<?> i = it.iterator();
                if (!i.hasNext()) {
                    throw new dart.core.StateError("No element");
                }
                Object acc = i.next();
                while (i.hasNext()) {
                    acc = invoke(arg(a, 0), new Object[] {acc, i.next()});
                }
                return acc;
            }
            case "expand": {
                final Object f = fnArg(arg(a, 0), "(dynamic) => Iterable<dynamic>");
                return it.expand(new Funcs.Func1<Object, Iterable<Object>>() {
                    @Override
                    public Iterable<Object> call(Object x) {
                        return asIterable(invoke(f, new Object[] {x}));
                    }
                });
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

    /// A value already checked to be a Number (or null), boxed as the Java type an
    /// {@code int?} or {@code double?} parameter takes: Long or Double.
    public static Object numAs(Number n, boolean isInt) {
        if (n == null) {
            return null;
        }
        if (isInt) {
            return n instanceof Long ? n : Long.valueOf(n.longValue());
        }
        return n instanceof Double ? n : Double.valueOf(n.doubleValue());
    }

    /// Methods of the dart:core classes the runtime implements as plain Java classes,
    /// called on a value whose static type is dynamic. Each argument is checked against
    /// the parameter type the SDK declares, as Dart's dynamic invocation does: a mismatch
    /// is a TypeError, not whatever the Java method would make of it.
    private static Object coreTypeCall(Object o, String name, Object[] a) {
        if (o instanceof dart.core.DateTime) {
            dart.core.DateTime d = (dart.core.DateTime) o;
            switch (name) {
                case "add": return d.add(typed(arg(a, 0), dart.core.Duration.class, "Duration"));
                case "subtract": return d.subtract(typed(arg(a, 0), dart.core.Duration.class, "Duration"));
                case "difference": return d.difference(typed(arg(a, 0), dart.core.DateTime.class, "DateTime"));
                case "isBefore":
                    return Boolean.valueOf(d.isBefore(typed(arg(a, 0), dart.core.DateTime.class, "DateTime")));
                case "compareTo":
                    return Long.valueOf(d.compareTo(typed(arg(a, 0), dart.core.DateTime.class, "DateTime")));
                default: return NO_MEMBER;
            }
        }
        if (o instanceof dart.core.RegExpMatch) {
            dart.core.RegExpMatch m = (dart.core.RegExpMatch) o;
            switch (name) {
                case "group": case "[]": return m.group(typed(arg(a, 0), Long.class, "int").longValue());
                case "namedGroup": return m.namedGroup(typed(arg(a, 0), String.class, "String"));
                default: return NO_MEMBER;
            }
        }
        if (o instanceof dart.core.RegExp) {
            dart.core.RegExp r = (dart.core.RegExp) o;
            switch (name) {
                case "hasMatch": return Boolean.valueOf(r.hasMatch(typed(arg(a, 0), String.class, "String")));
                case "firstMatch": return r.firstMatch(typed(arg(a, 0), String.class, "String"));
                case "stringMatch": return r.stringMatch(typed(arg(a, 0), String.class, "String"));
                case "allMatches":
                    return r.allMatches(typed(arg(a, 0), String.class, "String"),
                            a.length > 1 && a[1] != null ? typed(a[1], Long.class, "int").longValue() : 0L);
                default: return NO_MEMBER;
            }
        }
        if (o instanceof dart.core.StringBuffer) {
            dart.core.StringBuffer b = (dart.core.StringBuffer) o;
            switch (name) {
                case "write":
                    b.write(arg(a, 0));
                    return null;
                case "writeln":
                    if (a.length == 0) {
                        b.writeln();
                    } else {
                        b.writeln(a[0]);
                    }
                    return null;
                case "writeCharCode":
                    b.writeCharCode(typed(arg(a, 0), Long.class, "int").longValue());
                    return null;
                case "clear":
                    b.clear();
                    return null;
                default: return NO_MEMBER;
            }
        }
        return NO_MEMBER;
    }

    /// Dart's implicit downcast of a dynamic argument to its declared parameter type.
    private static <T> T typed(Object v, Class<T> type, String dartType) {
        if (type.isInstance(v)) {
            return type.cast(v);
        }
        throw new dart.core.TypeError("type '" + DartRuntime.typeName(v)
                + "' is not a subtype of type '" + dartType + "'");
    }

    private static Object stringCall(String s, String name, Object[] a) {
        switch (name) {
            case "toLowerCase":
                return DString.toLowerCase(s);
            case "toUpperCase":
                return DString.toUpperCase(s);
            case "trim":
                return DString.trim(s);
            // Every Pattern parameter takes a String or a RegExp, and anything else is the
            // TypeError Dart's implicit downcast raises: the DString Object overloads check.
            case "contains":
                return Boolean.valueOf(a.length > 1 && a[1] != null
                        ? DString.contains(s, a[0], typed(a[1], Long.class, "int").longValue())
                        : DString.contains(s, arg(a, 0)));
            case "startsWith":
                return Boolean.valueOf(DString.startsWith(s, arg(a, 0),
                        a.length > 1 && a[1] != null ? typed(a[1], Long.class, "int").longValue() : 0L));
            case "endsWith":
                return Boolean.valueOf(s.endsWith(typed(arg(a, 0), String.class, "String")));
            case "split":
                return DString.split(s, arg(a, 0));
            case "substring":
                return a.length > 1 && a[1] != null ? DString.substring(s, num(a[0]).longValue(), num(a[1]).longValue())
                        : DString.substring(s, num(arg(a, 0)).longValue());
            case "replaceAll":
                return DString.replaceAll(s, arg(a, 0), typed(arg(a, 1), String.class, "String"));
            case "replaceFirst":
                return DString.replaceFirst(s, arg(a, 0), typed(arg(a, 1), String.class, "String"),
                        a.length > 2 && a[2] != null ? typed(a[2], Long.class, "int").longValue() : 0L);
            case "indexOf":
                return Long.valueOf(a.length > 1 && a[1] != null
                        ? DString.indexOf(s, a[0], typed(a[1], Long.class, "int").longValue())
                        : DString.indexOf(s, arg(a, 0)));
            case "lastIndexOf":
                return Long.valueOf(a.length > 1 && a[1] != null
                        ? DString.lastIndexOf(s, a[0], typed(a[1], Long.class, "int").longValue())
                        : DString.lastIndexOf(s, arg(a, 0)));
            case "codeUnitAt":
                return Long.valueOf(DString.codeUnitAt(s, num(arg(a, 0)).longValue()));
            case "padLeft":
                return a.length > 1 && a[1] != null ? DString.padLeft(s, num(a[0]).longValue(), DartRuntime.as(a[1], String.class, false, "String"))
                        : DString.padLeft(s, num(arg(a, 0)).longValue());
            case "compareTo":
                return Long.valueOf(DString.compareTo(s, DartRuntime.as(arg(a, 0), String.class, false, "String")));
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
            case "toRadixString":
                if (dbl) {
                    return NO_MEMBER;
                }
                return DartRuntime.toRadixString(n.longValue(), typed(arg(a, 0), Long.class, "int").longValue());
            case "compareTo":
                // Dart's total order: NaN after everything, -0.0 before 0.0
                return Long.valueOf(dart.core.DartComparable.compare(n, num(arg(a, 0))));
            case "remainder": {
                // Java's % is Dart's remainder (truncated, the dividend's sign); an int
                // divisor of zero throws, as the int operator does.
                Number o = num(arg(a, 0));
                if (!dbl && !(o instanceof Double || o instanceof Float)) {
                    long r = o.longValue();
                    if (r == 0) {
                        throw new dart.core.IntegerDivisionByZeroException();
                    }
                    return Long.valueOf(n.longValue() % r);
                }
                return Double.valueOf(d % o.doubleValue());
            }
            case "clamp": {
                Number lo = num(arg(a, 0));
                Number hi = num(arg(a, 1));
                // num.clamp: ordered by compareTo (NaN greatest), and the operand that wins
                // is answered as it is -- 1.5.clamp(2, 3) is the int 2.
                // Compared exactly, as num.compareTo does: through doubleValue() an int
                // past 2^53 rounds and 9007199254740993.clamp(0, 9007199254740992.0)
                // answered the receiver instead of the upper bound.
                if (numCompare(lo, hi) > 0) {
                    throw new dart.core.ArgumentError(DartRuntime.str(lo));
                }
                if (numCompare(n, lo) < 0) {
                    return lo;
                }
                return numCompare(n, hi) > 0 ? hi : n;
            }
            case "toString":
                return DartRuntime.str(n);
            default:
                return NO_MEMBER;
        }
    }

    /**
     * Dart's {@code num.compareTo}: exact for two ints and for an int against a
     * double, NaN greatest, and -0.0 below 0 (so {@code 0.compareTo(-0.0)} is 1).
     */
    static int numCompare(Number a, Number b) {
        boolean ai = DartRuntime.isIntegral(a);
        boolean bi = DartRuntime.isIntegral(b);
        if (ai && bi) {
            return Long.compare(a.longValue(), b.longValue());
        }
        if (!ai && !bi) {
            return Double.compare(a.doubleValue(), b.doubleValue());
        }
        return ai ? intCompareDouble(a.longValue(), b.doubleValue())
                : -intCompareDouble(b.longValue(), a.doubleValue());
    }

    private static int intCompareDouble(long l, double d) {
        if (d != d) {
            return -1;
        }
        if (d >= 9.223372036854775807E18) {
            return -1;
        }
        if (d < -9.223372036854775807E18) {
            return 1;
        }
        if (d == 0 && l == 0) {
            return 1 / d < 0 ? 1 : 0;
        }
        double ld = (double) l;
        if (ld != d) {
            return ld < d ? -1 : 1;
        }
        // Equal as doubles, so d is integral and inside the long range: exact on longs.
        return Long.compare(l, (long) d);
    }

    // ------------------------------------------------------------------ functions

    public static boolean isFunction(Object f) {
        return f instanceof Funcs.Func0 || f instanceof Funcs.Func1 || f instanceof Funcs.Func2
                || f instanceof Funcs.Func3 || f instanceof Funcs.Func4 || f instanceof Funcs.Func5
                || f instanceof Funcs.VoidFunc0 || f instanceof Funcs.VoidFunc1 || f instanceof Funcs.VoidFunc2
                || f instanceof Funcs.VoidFunc3 || f instanceof Funcs.VoidFunc4 || f instanceof Funcs.VoidFunc5;
    }

    /// Dart's {@code Function.apply(f, positional, named)}. The positional list is spread
    /// over the function as {@link #invoke} does. Function values are held in their
    /// canonical positional shape, which keeps no parameter names, so named arguments
    /// cannot be matched to anything: a non-empty map is refused rather than dropped.
    @SuppressWarnings("unchecked")
    public static Object apply(Object f, Object positional, Object named) {
        if (named instanceof Map && !((Map<Object, Object>) named).isEmpty()) {
            throw new dart.core.UnsupportedError(
                    "Function.apply with named arguments is not supported: function values keep no parameter names");
        }
        Object[] a = NONE;
        if (positional instanceof java.util.Collection) {
            a = ((java.util.Collection<Object>) positional).toArray();
        } else if (positional instanceof Iterable) {
            java.util.ArrayList<Object> l = new java.util.ArrayList<Object>();
            for (Object o : (Iterable<Object>) positional) {
                l.add(o);
            }
            a = l.toArray();
        }
        if (f instanceof DynamicObject) {
            // A callable object: Function.apply reaches its call method.
            return call(f, "call", a, null);
        }
        return invoke(f, a);
    }

    /// A dynamic call with named arguments (`dynamic f = ({required int a}) => ..;
    /// f(a: 1)`). A closure with named parameters is a positional SAM whose named
    /// parameters follow the positional ones in the order its signature names them
    /// ($named), so each argument is placed by name, an omitted one passed as the
    /// omitted-argument marker for the adapter's default, and each checked against its
    /// reified parameter type: a mismatch is a TypeError and an unknown name or a
    /// missing required one a NoSuchMethodError, as in Dart.
    public static Object invokeNamed(Object f, Object[] positional, String[] names, Object[] values) {
        // A narrowed view (SignedFns.narrowNamed) answers the wide closure's signature, so
        // its arguments are placed for the wide closure, which is the one called.
        f = SignedFns.unwrap(f);
        if (f instanceof DynamicObject && !isFunction(f)) {
            // A callable object (`dynamic p = P(); p(a: 1)`): Dart calls its call method,
            // named arguments and all.
            DartMap<String, Object> named = new DartMap<String, Object>();
            for (int k = 0; k < names.length; k++) {
                named.put(names[k], values[k]);
            }
            return call(f, "call", positional, named);
        }
        if (!(f instanceof Signed) || ((Signed) f).$named() == null || ((Signed) f).$generic()) {
            throw noSuch(f, "call");
        }
        Signed sf = (Signed) f;
        String[] declared = sf.$named();
        TypeWitness[] sig = sf.$sig();
        int posCount = sig.length - 1 - declared.length;
        int req = sf.$required();
        if (positional.length > posCount || positional.length < (req >= 0 ? Math.min(req, posCount) : posCount)) {
            throw noSuch(f, "call");
        }
        Object[] a = new Object[sig.length - 1];
        for (int i = 0; i < a.length; i++) {
            a[i] = i < positional.length ? positional[i] : DartRuntime.absent();
        }
        for (int k = 0; k < names.length; k++) {
            int at = -1;
            for (int j = 0; j < declared.length; j++) {
                if (bareName(declared[j]).equals(names[k])) {
                    at = posCount + j;
                    break;
                }
            }
            if (at < 0) {
                throw noSuch(f, "call");
            }
            a[at] = values[k];
        }
        for (int j = 0; j < declared.length; j++) {
            // A required named parameter is recorded as "!name" ($named).
            if (declared[j].startsWith("!") && a[posCount + j] == DartRuntime.absent()) {
                throw noSuch(f, "call");
            }
        }
        for (int i = 0; i < a.length; i++) {
            TypeWitness pw = sig[i + 1];
            if (pw != null && a[i] != DartRuntime.absent() && !pw.test(a[i])) {
                throw new dart.core.TypeError("type '" + DartRuntime.typeName(a[i])
                        + "' is not a subtype of the parameter type of this closure");
            }
        }
        return invokePlaced(f, a);
    }

    private static String bareName(String declared) {
        return declared.startsWith("!") ? declared.substring(1) : declared;
    }

    /** Calls a function value whose static type is dynamic or Function. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object invoke(Object f, Object... a) {
        if (a == null) {
            a = NONE;
        }
        if (f instanceof Signed && ((Signed) f).$named() != null && !((Signed) f).$generic()) {
            // A closure with named parameters called with positionals only: every named
            // one is omitted, which invokeNamed checks against the signature (a missing
            // required one is a NoSuchMethodError) before padding with the defaults.
            return invokeNamed(f, a, new String[0], new Object[0]);
        }
        return invokePlaced(f, a);
    }

    /// Invokes a function value with its arguments already in the SAM's positional slots.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object invokePlaced(Object f, Object[] a) {
        f = SignedFns.unwrap(f);
        if (f instanceof DynamicObject && !isFunction(f)) {
            // A callable object (`dynamic p = P(); p(21)`): Dart calls its call method.
            return call(f, "call", a, null);
        }
        if (f instanceof Signed && ((Signed) f).$named() == null && !((Signed) f).$generic()) {
            // A dynamic call checks each argument against the closure's own parameter
            // type, as Dart does: `(f as dynamic)('s')` on an `(int x) => ..` is a
            // TypeError, not a ClassCastException from inside the body.
            TypeWitness[] sig = ((Signed) f).$sig();
            for (int i = 0; sig != null && i < a.length && i + 1 < sig.length; i++) {
                TypeWitness pw = sig[i + 1];
                if (pw != null && !pw.test(a[i])) {
                    throw new dart.core.TypeError("type '" + DartRuntime.typeName(a[i])
                            + "' is not a subtype of the parameter type of this closure");
                }
            }
            // Omitted optional positionals (`(int a, [int b = 2])` called with one
            // argument) arrive as the omitted-argument marker, which the closure's adapter
            // replaces with the default. Fewer than the required count still falls through
            // to the arity mismatch below.
            int req = ((Signed) f).$required();
            if (req >= 0 && sig != null && a.length >= req && a.length < sig.length - 1) {
                Object[] padded = new Object[sig.length - 1];
                System.arraycopy(a, 0, padded, 0, a.length);
                for (int i = a.length; i < padded.length; i++) {
                    padded[i] = DartRuntime.absent();
                }
                a = padded;
            }
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

    /// Dart's implicit downcast of a dynamic argument to a callback parameter: a value
    /// that is not a function is a TypeError at the call, before anything runs (a lazy
    /// `where(5)` would otherwise fail only when iterated, or never).
    private static Object fnArg(Object f, String dartType) {
        if (isFunction(f)) {
            return f;
        }
        throw new dart.core.TypeError("type '" + DartRuntime.typeName(f) + "' is not a subtype of type '"
                + dartType + "'");
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
