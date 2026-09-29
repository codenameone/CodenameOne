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

    public static void main(String[] args) {
        System.out.println("RESULT=" + (calculate() + lazySemantics() + fusionSemantics()));
    }
}
