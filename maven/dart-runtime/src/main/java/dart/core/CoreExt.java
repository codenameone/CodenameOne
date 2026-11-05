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
package dart.core;

import dart.runtime.Funcs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

/// dart:core instance members the transpiler has no inline lowering for, reached
/// through the stub extensions in `META-INF/dart/dart_core_ext.dart`: each is a
/// static method taking the receiver first. Erased generics make every `cast` an
/// unchecked view of the same object, which is what Dart's lazy cast is until an
/// element is read.
public final class CoreExt {

    private CoreExt() {
    }

    private static Random random;

    // ---------------------------------------------------------------- casts

    @SuppressWarnings("unchecked")
    public static <R> DartList<R> cast(DartList<?> l) {
        return (DartList<R>) l;
    }

    @SuppressWarnings("unchecked")
    public static <R> java.util.List<R> cast(java.util.List<?> l) {
        return (java.util.List<R>) l;
    }

    @SuppressWarnings("unchecked")
    public static <R> DartSet<R> cast(DartSet<?> s) {
        return (DartSet<R>) s;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DartMap<K, V> cast(DartMap<?, ?> m) {
        return (DartMap<K, V>) m;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> cast(Map<?, ?> m) {
        return (Map<K, V>) m;
    }

    /// `list.cast<R>()` with R's witness: a view whose reads are checked and whose
    /// reified element type is R. The witness-less overloads above are for callers that
    /// cannot name R (generic code over an erased type variable).
    @SuppressWarnings("unchecked")
    public static <R> DartList<R> cast(DartList<?> l, dart.runtime.TypeWitness r) {
        return ElementViews.castList((DartList<Object>) l, r);
    }

    @SuppressWarnings("unchecked")
    public static <R> DartSet<R> cast(DartSet<?> s, dart.runtime.TypeWitness r) {
        return ElementViews.castSet((DartSet<Object>) s, r);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DartMap<K, V> cast(DartMap<?, ?> m, dart.runtime.TypeWitness k, dart.runtime.TypeWitness v) {
        return ElementViews.castMap((DartMap<Object, Object>) m, k, v);
    }

    @SuppressWarnings("unchecked")
    public static <R> DartIterable<R> cast(Iterable<?> it, dart.runtime.TypeWitness r) {
        if (it instanceof DartList) {
            return DartIterable.wrap(ElementViews.<Object, R>castList((DartList<Object>) it, r));
        }
        if (it instanceof DartSet) {
            return DartIterable.wrap(ElementViews.<Object, R>castSet((DartSet<Object>) it, r));
        }
        return ElementViews.castIterable((Iterable<Object>) it, r);
    }

    @SuppressWarnings("unchecked")
    public static <R> DartIterable<R> cast(Iterable<?> it) {
        if (it instanceof DartIterable) {
            return (DartIterable<R>) it;
        }
        return new DartIterable<R>((Iterable<R>) it);
    }

    // ---------------------------------------------------------------- List

    private static void checkRange(java.util.List<?> l, long start, long end) {
        RangeError.checkValidRange(start, end, l.size());
    }

    public static <E> void setRange(java.util.List<E> l, long start, long end, Iterable<? extends E> from) {
        setRange(l, start, end, from, 0);
    }

    public static <E> void setRange(java.util.List<E> l, long start, long end, Iterable<? extends E> from,
            long skipCount) {
        if (l instanceof MixinView && ((MixinView) l).source() instanceof dart.collection.ListMixin) {
            // A List-typed call on a program ListMixin class reaches the class's own
            // setRange, as Dart's dynamic dispatch would.
            @SuppressWarnings("unchecked")
            dart.collection.ListMixin<E> m = (dart.collection.ListMixin<E>) ((MixinView) l).source();
            m.setRange(start, end, from, skipCount);
            return;
        }
        checkRange(l, start, end);
        ArrayList<E> src = new ArrayList<E>();
        for (E e : DartList.checkedSource(l, from)) {
            src.add(e);
        }
        int len = (int) (end - start);
        if (skipCount < 0 || skipCount + len > src.size()) {
            throw new StateError("Too few elements");
        }
        for (int i = 0; i < len; i++) {
            l.set((int) start + i, src.get((int) skipCount + i));
        }
    }

    public static void removeRange(java.util.List<?> l, long start, long end) {
        checkRange(l, start, end);
        l.subList((int) start, (int) end).clear();
    }

    public static <E> void fillRange(java.util.List<E> l, long start, long end) {
        fillRange(l, start, end, null);
    }

    public static <E> void fillRange(java.util.List<E> l, long start, long end, E fill) {
        checkRange(l, start, end);
        for (int i = (int) start; i < end; i++) {
            l.set(i, fill);
        }
    }

    public static <E> void replaceRange(java.util.List<E> l, long start, long end, Iterable<? extends E> with) {
        checkRange(l, start, end);
        ArrayList<E> src = new ArrayList<E>();
        for (E e : with) {
            src.add(e);
        }
        l.subList((int) start, (int) end).clear();
        l.addAll((int) start, src);
    }

    public static <E> void setAll(java.util.List<E> l, long index, Iterable<? extends E> from) {
        ArrayList<E> src = new ArrayList<E>();
        for (E e : DartList.checkedSource(l, from)) {
            src.add(e);
        }
        RangeError.checkValueInInterval(index, 0, l.size(), "index");
        if (index + src.size() > l.size()) {
            throw new RangeError("RangeError: Invalid value: Not in inclusive range 0.." + l.size()
                    + ": " + (index + src.size()));
        }
        for (int i = 0; i < src.size(); i++) {
            l.set((int) index + i, src.get(i));
        }
    }

    public static void shuffle(java.util.List<?> l) {
        if (random == null) {
            random = new Random();
        }
        Collections.shuffle(l, random);
    }

    public static void shuffle(java.util.List<?> l, Object rnd) {
        shuffle(l);
    }

    // ---------------------------------------------------------------- Iterable

    public static <E> E single(Iterable<E> it) {
        java.util.Iterator<E> i = it.iterator();
        if (!i.hasNext()) {
            throw new StateError("No element");
        }
        E e = i.next();
        if (i.hasNext()) {
            throw new StateError("Too many elements");
        }
        return e;
    }

    /// Dart's `iterator`: `moveNext()` then `current`, over Java's iterator. The element
    /// type comes from the receiver, so `var it = xs.iterator` in an extension on
    /// `Iterable<double>` reads `it.current` as a double rather than an Object.
    public static <E> dart.collection.Iterator<E> iterator(final Iterable<? extends E> it) {
        final java.util.Iterator<? extends E> j = it.iterator();
        return new dart.collection.Iterator<E>() {
            private E cur;

            @Override
            public boolean moveNext() {
                if (j.hasNext()) {
                    cur = j.next();
                    return true;
                }
                cur = null;
                return false;
            }

            @Override
            public E current() {
                return cur;
            }

            @Override
            public boolean hasNext() {
                return j.hasNext();
            }

            @Override
            public E next() {
                cur = j.next();
                return cur;
            }
        };
    }

    public static <E> boolean moveNext(dart.collection.Iterator<E> it) {
        return it.moveNext();
    }

    public static <E> E current(dart.collection.Iterator<E> it) {
        return it.current();
    }

    public static <E> DartIterable<E> nonNulls(final Iterable<E> it) {
        ArrayList<E> out = new ArrayList<E>();
        for (E e : it) {
            if (e != null) {
                out.add(e);
            }
        }
        return new DartIterable<E>(out);
    }

    // ---------------------------------------------------------------- Map / Set

    public static <K, V, K2, V2> DartMap<K2, V2> map(Map<K, V> m, Funcs.Func2<K, V, MapEntry<K2, V2>> f) {
        DartMap<K2, V2> out = new DartMap<K2, V2>();
        for (Map.Entry<K, V> e : m.entrySet()) {
            MapEntry<K2, V2> r = f.call(e.getKey(), e.getValue());
            out.put(r.key(), r.value());
        }
        return out;
    }

    public static <K, V> void updateAll(Map<K, V> m, Funcs.Func2<K, V, V> f) {
        for (Map.Entry<K, V> e : m.entrySet()) {
            e.setValue(f.call(e.getKey(), e.getValue()));
        }
    }

    public static <E> E lookup(java.util.Set<E> s, Object o) {
        for (E e : s) {
            if (e == null ? o == null : e.equals(o)) {
                return e;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- String

    /// {@code String.codeUnits}: a List<int>, so the primitive DartLongList every other
    /// List<int> is (a local typed by it is declared as one).
    public static DartLongList codeUnits(String s) {
        DartLongList out = new DartLongList();
        for (int i = 0; i < s.length(); i++) {
            out.addLong(s.charAt(i));
        }
        return out;
    }

    /// {@code String.runes}: Dart's {@code Runes}, an {@code Iterable<int>} (not a List) of
    /// the Unicode code points, a well-formed surrogate pair read as one code point and a
    /// lone surrogate as itself. Lazy like Dart's: each iteration walks the string afresh,
    /// so {@code runes.first} decodes one code point, and it prints as an Iterable does.
    public static DartIterable<Long> runes(final String s) {
        return new DartIterable<Long>(() -> new java.util.Iterator<Long>() {
            private int i;

            @Override
            public boolean hasNext() {
                return i < s.length();
            }

            @Override
            public Long next() {
                if (i >= s.length()) {
                    throw new java.util.NoSuchElementException();
                }
                char c = s.charAt(i++);
                if (Character.isHighSurrogate(c) && i < s.length() && Character.isLowSurrogate(s.charAt(i))) {
                    return Long.valueOf(Character.toCodePoint(c, s.charAt(i++)));
                }
                return Long.valueOf(c);
            }
        });
    }

    public static String trimLeft(String s) {
        int start = 0;
        while (start < s.length() && DString.isDartWhitespace(s.charAt(start))) {
            start++;
        }
        return start == 0 ? s : s.substring(start);
    }

    public static String trimRight(String s) {
        int end = s.length();
        while (end > 0 && DString.isDartWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return end == s.length() ? s : s.substring(0, end);
    }

    public static String replaceRange(String s, long start, Long end, String with) {
        int e = end == null ? s.length() : (int) end.longValue();
        RangeError.checkValidRange(start, e, s.length());
        return s.substring(0, (int) start) + with + s.substring(e);
    }

    public static String replaceRange(String s, long start, long end, String with) {
        return replaceRange(s, start, Long.valueOf(end), with);
    }

    /// The matches of a pattern: a RegExp's own, or every non-overlapping
    /// occurrence of a literal String.
    public static DartIterable<RegExpMatch> allMatches(Object pattern, String input) {
        return allMatches(pattern, input, 0);
    }

    public static DartIterable<RegExpMatch> allMatches(Object pattern, String input, long start) {
        if (pattern instanceof RegExp) {
            return ((RegExp) pattern).allMatches(input, (int) start);
        }
        String p = (String) String.valueOf(pattern);
        ArrayList<RegExpMatch> out = new ArrayList<RegExpMatch>();
        int from = (int) start;
        while (from <= input.length()) {
            int i = input.indexOf(p, from);
            if (i < 0) {
                break;
            }
            out.add(literal(p, i, input));
            from = p.isEmpty() ? i + 1 : i + p.length();
        }
        return new DartIterable<RegExpMatch>(out);
    }

    private static RegExpMatch literal(String p, int at, String input) {
        RegExpMatch m = new RegExpMatch(new String[] {p}, new int[] {at}, new int[] {at + p.length()}, input);
        m.pattern = p;
        return m;
    }

    public static RegExpMatch matchAsPrefix(Object pattern, String input) {
        return matchAsPrefix(pattern, input, 0);
    }

    public static RegExpMatch matchAsPrefix(Object pattern, String input, long start) {
        RangeError.checkValueInInterval(start, 0, input.length(), "start");
        if (pattern instanceof RegExp) {
            RegExpMatch m = ((RegExp) pattern).matchFrom(input, (int) start);
            return m != null && m.start() == start ? m : null;
        }
        String p = String.valueOf(pattern);
        return input.startsWith(p, (int) start) ? literal(p, (int) start, input) : null;
    }

    public static String splitMapJoin(String s, Object pattern, Funcs.Func1<RegExpMatch, String> onMatch,
            Funcs.Func1<String, String> onNonMatch) {
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (RegExpMatch m : allMatches(pattern, s)) {
            int st = (int) m.start();
            String gap = s.substring(last, st);
            sb.append(onNonMatch == null ? gap : onNonMatch.call(gap));
            sb.append(onMatch == null ? m.group(0) : onMatch.call(m));
            last = (int) m.end();
        }
        String tail = s.substring(last);
        sb.append(onNonMatch == null ? tail : onNonMatch.call(tail));
        return sb.toString();
    }

    public static String replaceAllMapped(String s, Object pattern, Funcs.Func1<RegExpMatch, String> replace) {
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (RegExpMatch m : allMatches(pattern, s)) {
            sb.append(s, last, (int) m.start()).append(replace.call(m));
            last = (int) m.end();
        }
        return sb.append(s.substring(last)).toString();
    }

    public static String replaceFirstMapped(String s, Object pattern, Funcs.Func1<RegExpMatch, String> replace) {
        return replaceFirstMapped(s, pattern, replace, 0);
    }

    public static String replaceFirstMapped(String s, Object pattern, Funcs.Func1<RegExpMatch, String> replace,
            long start) {
        for (RegExpMatch m : allMatches(pattern, s, start)) {
            return s.substring(0, (int) m.start()) + replace.call(m) + s.substring((int) m.end());
        }
        return s;
    }

    // ---------------------------------------------------------------- int

    public static long sign(long v) {
        return v > 0 ? 1 : v < 0 ? -1 : 0;
    }

    public static boolean isNegative(long v) {
        return v < 0;
    }

    public static long remainder(long v, long other) {
        if (other == 0) {
            throw new IntegerDivisionByZeroException();
        }
        return v % other;
    }

    public static double remainder(long v, double other) {
        return v % other;
    }
}
