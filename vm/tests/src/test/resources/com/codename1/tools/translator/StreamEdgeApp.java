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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamEdgeApp {
    private static int calculate() {
        Object[] transformed = Stream.of(4, 2, 9, 2, 7, 4)
                .distinct()
                .sorted()
                .skip(1)
                .limit(3)
                .toArray();
        int transformedSum = ((Integer) transformed[0]).intValue()
                + ((Integer) transformed[1]).intValue()
                + ((Integer) transformed[2]).intValue();

        int reduce = Stream.of(1, 2, 3, 4).reduce(10, (a, b) -> a + b);

        final int[] forEachCode = new int[] { 0 };
        Stream.of(3, 1, 2).sorted().forEach(i -> forEachCode[0] = forEachCode[0] * 10 + i);

        Object[] arr = Stream.of(5, 6).skip(1).toArray();
        long emptyCount = Stream.<Integer>empty().count();

        int matchScore = 0;
        if (!Stream.<Integer>empty().anyMatch(v -> true)) {
            matchScore += 1;
        }
        if (Stream.<Integer>empty().allMatch(v -> false)) {
            matchScore += 10;
        }
        if (Stream.<Integer>empty().noneMatch(v -> true)) {
            matchScore += 100;
        }

        long clampCount = Stream.of(1, 2, 3).skip(5).limit(2).count();
        long distinctCount = Stream.of(1, 1, 2, 3, 3).distinct().count();

        int checksum = 0;
        checksum += transformedSum * 2;
        checksum += reduce;
        checksum += forEachCode[0];
        checksum += ((Integer) arr[0]).intValue() * 7;
        checksum += (int) emptyCount * 11;
        checksum += matchScore;
        checksum += (int) clampCount;
        checksum += (int) distinctCount * 13;
        return checksum;
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("lazy stream contract");
    }

    private static int lazySemantics() {
        final StringBuilder trace = new StringBuilder();
        Stream<Integer> pipeline = Stream.of(1, 2, 3, 4)
                .filter(v -> { trace.append('f').append(v); return v % 2 == 0; })
                .map(v -> { trace.append('m').append(v); return v * 10; });
        check(trace.length() == 0);
        check(pipeline.anyMatch(v -> { trace.append('t').append(v); return true; }));
        check("f1f2m2t20".equals(trace.toString()));
        try { pipeline.count(); throw new AssertionError(); }
        catch (IllegalStateException expected) { }

        final int[] calls = { 0 };
        check(Stream.of(3, 2, 1).map(v -> { calls[0]++; return v; })
                .limit(0).toArray().length == 0);
        check(calls[0] == 0);
        Object[] limited = Stream.of(1, 2, 3).filter(v -> { calls[0]++; return true; })
                .limit(1).toArray();
        check(limited.length == 1 && calls[0] == 1);
        check(Stream.of(1, 2).skip(Long.MAX_VALUE).count() == 0);
        check(Stream.of(1, 2).limit(Long.MAX_VALUE).count() == 2);

        Integer[] backing = { 1, 2 };
        Stream<Integer> late = Stream.of(backing);
        backing[0] = 9;
        check(((Integer) late.toArray()[0]).intValue() == 9);
        Stream<Integer> head = Stream.of(new Integer[] { 1 });
        Stream<Integer> tail = head.map(v -> v);
        try { head.count(); throw new AssertionError(); }
        catch (IllegalStateException expected) { }
        head.close();
        try { tail.count(); throw new AssertionError(); }
        catch (IllegalStateException expected) { }
        tail.close();

        try { Stream.of(new Integer[] { 1 }).limit(-1); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        try { Stream.of(new Integer[] { 1 }).skip(-1); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        try { Stream.empty().filter(null); throw new AssertionError(); }
        catch (NullPointerException expected) { }
        try { Stream.empty().map(null); throw new AssertionError(); }
        catch (NullPointerException expected) { }
        try { Stream.empty().anyMatch(null); throw new AssertionError(); }
        catch (NullPointerException expected) { }
        try { Stream.<Integer>of((Integer[]) null); throw new AssertionError(); }
        catch (NullPointerException expected) { }

        java.util.Iterator<Integer> iterator = Stream.of(2, 2, null, null, 1).distinct().iterator();
        check(iterator.hasNext() && iterator.hasNext() && iterator.next() == 2);
        check(iterator.next() == null && iterator.next() == 1 && !iterator.hasNext());
        try { iterator.next(); throw new AssertionError(); }
        catch (java.util.NoSuchElementException expected) { }
        try { iterator.remove(); throw new AssertionError(); }
        catch (UnsupportedOperationException expected) { }
        return 10000;
    }

    private static boolean fusedMatch(Integer[] input, StringBuilder trace, int threshold, long skip) {
        return Stream.of(input)
                .filter(v -> { trace.append('f').append(v); return v > threshold; })
                .skip(skip).limit(2)
                .map(v -> { trace.append('m').append(v); return v * 10; })
                .anyMatch(v -> { trace.append('t').append(v); return v == 40; });
    }
    private static long fusedCount(Integer[] input, long skip, long limit) {
        return Stream.of(input).skip(skip).limit(limit).count();
    }
    private static void fusedForEach(String[] input, String suffix, StringBuilder result) {
        Stream.of(input).map(v -> v + suffix).forEach(v -> result.append(v));
    }
    private static boolean fusedAll(Integer[] input, double threshold) {
        return Stream.of(input).allMatch(v -> v < threshold);
    }
    private static boolean fusedNone(Integer[] input, float threshold) {
        return Stream.of(input).noneMatch(v -> v > threshold);
    }
    private static boolean fusedZero(Integer[] input, int[] calls) {
        return Stream.of(input).filter(v -> { calls[0]++; return true; }).limit(0).anyMatch(v -> true);
    }
    private static boolean fusedThrow(Integer[] input) {
        return Stream.of(input).anyMatch(v -> { if (v == 2) throw new IllegalStateException("callback"); return false; });
    }
    private static void fusedMutation(String[] input, StringBuilder trace) {
        Stream.of(input).forEach(v -> { trace.append(v); input[1] = "changed"; System.gc(); });
    }
    private static int fusionSemantics() {
        Integer[] input = {1, 2, 3, 4, 5};
        StringBuilder trace = new StringBuilder();
        check(fusedMatch(input, trace, 1, 1));
        check("f1f2f3m3t30f4m4t40".equals(trace.toString()));
        check(fusedCount(input, 1, 2) == 2);
        check(fusedCount(input, Long.MAX_VALUE, 2) == 0);
        check(fusedCount(input, 0, Long.MAX_VALUE) == 5);
        check(fusedAll(input, 6.0) && !fusedAll(input, 5.0));
        check(fusedNone(input, 6.0f) && !fusedNone(input, 4.0f));
        check(fusedAll(new Integer[0], 0) && fusedNone(new Integer[0], 0));
        int[] calls = {0};
        check(!fusedZero(input, calls) && calls[0] == 0);
        StringBuilder joined = new StringBuilder();
        fusedForEach(new String[] {"a", "b", "c"}, "!", joined);
        check("a!b!c!".equals(joined.toString()));
        try { fusedCount(null, -1, -1); throw new AssertionError(); }
        catch (NullPointerException expected) { }
        try { fusedCount(input, -1, 1); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        try { fusedCount(input, 0, -1); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        try { fusedThrow(input); throw new AssertionError(); }
        catch (IllegalStateException expected) { check("callback".equals(expected.getMessage())); }
        StringBuilder mutation = new StringBuilder();
        fusedMutation(new String[] {new String("first"), new String("second")}, mutation);
        check("firstchanged".equals(mutation.toString()));
        return 20000;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneaky(Throwable t) throws E {
        throw (E) t;
    }

    /** flatMap closes every stream its mapper returns; sorted(null) is an error, not natural order. */
    private static int closeSemantics() {
        final StringBuilder log = new StringBuilder();
        List<Integer> all = Stream.of(1, 2, 3)
                .flatMap(v -> Stream.of(v, v * 10).onClose(() -> log.append("close-s").append(v).append(' ')))
                .collect(Collectors.toList());
        check(all.size() == 6 && all.get(5).intValue() == 30);
        check("close-s1 close-s2 close-s3 ".equals(log.toString()));
        log.setLength(0);
        Optional<Integer> first = Stream.of(1, 2, 3)
                .flatMap(v -> Stream.of(v, v * 10).onClose(() -> log.append("close-t").append(v).append(' ')))
                .filter(v -> v.intValue() >= 10)
                .findFirst();
        check(first.get().intValue() == 10);
        check("close-t1 ".equals(log.toString()));
        log.setLength(0);
        check(Stream.of(1, 2, 3)
                .flatMap(v -> Stream.of(v).onClose(() -> log.append("close-u").append(v).append(' ')))
                .anyMatch(v -> v.intValue() == 2));
        check("close-u1 close-u2 ".equals(log.toString()));
        log.setLength(0);
        Stream<Integer> closing = Stream.of(1).onClose(() -> log.append("a")).onClose(() -> log.append("b"));
        closing.close();
        closing.close();
        check("ab".equals(log.toString()));
        log.setLength(0);
        try {
            Stream.of(1, 2)
                    .flatMap(v -> Stream.of(v, v).onClose(() -> log.append("close-p").append(v).append(' ')))
                    .anyMatch(v -> { throw new IllegalStateException("predicate"); });
            throw new AssertionError("predicate exception swallowed");
        } catch (IllegalStateException expected) {
        }
        check("close-p1 ".equals(log.toString()));
        log.setLength(0);
        try {
            Stream.of(1)
                    .flatMap(v -> Stream.of(v).onClose(() -> {
                        log.append("outer ");
                        throw new IllegalStateException("close");
                    }))
                    .flatMap(w -> Stream.of(w).onClose(() -> log.append("inner ")))
                    .anyMatch(v -> { throw new IllegalArgumentException("predicate"); });
            throw new AssertionError("predicate exception swallowed");
        } catch (IllegalArgumentException e) {
            // The close failure rides along as suppressed; it does not replace the original.
            check(e.getSuppressed().length == 1 && "close".equals(e.getSuppressed()[0].getMessage()));
        }
        check("inner outer ".equals(log.toString()));
        try {
            java.util.Comparator.<String, Integer>comparing(String::length, null);
            throw new AssertionError("comparing(f, null)");
        } catch (NullPointerException expected) {
        }
        try {
            java.util.Comparator.<String>naturalOrder().thenComparing((java.util.Comparator<String>) null);
            throw new AssertionError("thenComparing(null)");
        } catch (NullPointerException expected) {
        }
        try {
            Stream.of(1).collect(() -> new StringBuilder(), (sb, v) -> sb.append(v), null);
            throw new AssertionError("collect(.., null)");
        } catch (NullPointerException expected) {
        }
        try {
            Optional.of(1).or(null);
            throw new AssertionError("or(null)");
        } catch (NullPointerException expected) {
        }
        Stream<Integer> failing = Stream.of(1).onClose(() -> sneaky(new java.io.IOException("io")))
                .onClose(() -> log.append("second-ran"));
        log.setLength(0);
        try {
            failing.close();
            throw new AssertionError("close swallowed a checked failure");
        } catch (Throwable t) {
            check(t instanceof java.io.IOException && "io".equals(t.getMessage()));
        }
        check("second-ran".equals(log.toString()));
        try {
            Stream.of(2, 1).sorted(null).collect(Collectors.toList());
            throw new AssertionError("sorted(null)");
        } catch (NullPointerException expected) {
        }
        return 0;
    }

    public static void main(String[] args) {
        System.out.println("RESULT=" + (calculate() + lazySemantics() + fusionSemantics() + closeSemantics()));
    }
}
