/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.builders;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Picks the maximum heap for a forked ByteCodeTranslator JVM.
 *
 * <p>The translator holds the whole parsed class model plus the emitted output in
 * memory, so its peak scales with the size of the app being built. The heap used
 * to be a hard-coded constant per target -- one tuned against the sample apps --
 * which meant a genuinely large app died with OutOfMemoryError partway through
 * translation and could only be built by setting {@code CN1_TRANSLATOR_OPTS} by
 * hand (issue #5511). Sizing the ceiling from the machine instead lets those apps
 * build unmodified.
 *
 * <p>Raising the ceiling is close to free for ordinary apps: {@code -Xmx} is a
 * reservation, not a commitment, so a small app still runs at its natural
 * footprint. The bound is deliberately conservative rather than "as much as the
 * box has" -- half the detected budget, never more than {@link #CEILING_MB} --
 * so a translator running inside a build container stays well clear of the
 * container's own limit and a runaway build gets a clean Java OutOfMemoryError
 * (which we can report) instead of a kernel OOM-kill (which we cannot).
 */
final class TranslatorHeap {
    /**
     * Absolute cap on the auto-sized heap, in MB. Large real-world apps have been
     * observed needing ~1.6GB; this leaves headroom above that without ever
     * approaching the memory ceiling of a build container.
     */
    static final int CEILING_MB = 4096;

    /** Fraction of the detected memory budget the translator may claim. */
    private static final double BUDGET_FRACTION = 0.5;

    /**
     * Per-box escape hatch: set to an explicit MB value to pin the heap. Honoured
     * above the auto-sizing (but still below a {@code -Xmx} the caller passed in
     * {@code CN1_TRANSLATOR_OPTS}, which wins over everything).
     */
    static final String HEAP_ENV = "CN1_TRANSLATOR_MAX_HEAP_MB";

    private TranslatorHeap() {
    }

    /**
     * Resolves the {@code -Xmx} value, in MB, for a forked translator.
     *
     * @param floorMB the target's historical hard-coded heap. The result is never
     *                below this, so a machine (or container) too small to profit
     *                from auto-sizing behaves exactly as it did before.
     * @return the heap size in MB
     */
    static int maxHeapMB(int floorMB) {
        return maxHeapMB(floorMB, System.getenv(HEAP_ENV), detectBudgetMB());
    }

    /**
     * Testable core of {@link #maxHeapMB(int)}.
     *
     * @param floorMB   the target's historical hard-coded heap
     * @param envValue  raw value of {@link #HEAP_ENV}, or null when unset
     * @param budgetMB  detected memory budget in MB, or -1 when unknown
     * @return the heap size in MB
     */
    static int maxHeapMB(int floorMB, String envValue, long budgetMB) {
        if (floorMB < 1) {
            floorMB = 1;
        }
        int override = parseMB(envValue);
        if (override > 0) {
            // An explicit override is a deliberate act by whoever configured the
            // box, so it may go below the historical floor (a memory-starved box
            // may need that) AND above CEILING_MB, which only bounds the value we
            // pick automatically. Clamping it to the ceiling would make the knob
            // useless anyway: the operator would just switch to
            // CN1_TRANSLATOR_OPTS=-Xmx, which no ceiling applies to.
            //
            // What it may NOT do is exceed the memory the machine actually has --
            // that is the setting that OOM-kills a build box rather than failing
            // the one build, and it is the likely shape of a typo (an extra digit).
            if (budgetMB > 0 && override > budgetMB) {
                return (int) budgetMB;
            }
            return override;
        }
        if (budgetMB <= 0) {
            return floorMB;
        }
        long heap = (long) (budgetMB * BUDGET_FRACTION);
        if (heap > CEILING_MB) {
            heap = CEILING_MB;
        }
        if (heap < floorMB) {
            heap = floorMB;
        }
        return (int) heap;
    }

    /** The {@code -Xmx} argument for a forked translator, e.g. {@code -Xmx4096m}. */
    static String maxHeapArg(int floorMB) {
        return "-Xmx" + maxHeapMB(floorMB) + "m";
    }

    /**
     * Whether a failed translator run failed for lack of memory.
     *
     * <p>A translator that dies this way otherwise just returns a non-zero exit
     * code, and the build reports nothing more useful than "the translator
     * failed" -- which is what sent the reporter of issue #5511 reading the build
     * plugin's source to discover {@code CN1_TRANSLATOR_OPTS}. Recognising the
     * failure lets us say what actually happened.
     *
     * @param processOutput the translator's captured stdout/stderr
     * @return true when the output carries an out-of-memory signature
     */
    static boolean looksOutOfMemory(CharSequence processOutput) {
        if (processOutput == null) {
            return false;
        }
        String s = processOutput.toString();
        return s.contains("java.lang.OutOfMemoryError")
                || s.contains("GC overhead limit exceeded")
                || s.contains("There is insufficient memory for the Java Runtime Environment");
    }

    /**
     * The message to show when {@link #looksOutOfMemory} matches.
     *
     * @param heapMB                   the heap the translator was actually given, in MB
     * @param canConfigureEnvironment  true for a build on the developer's own
     *                                 machine, where they can raise the ceiling
     *                                 themselves; false for a cloud build, where
     *                                 the environment is not theirs to change and
     *                                 pointing them at an env var would be useless
     * @return an error message naming the heap in use and what can be done about it
     */
    static String outOfMemoryAdvice(int heapMB, boolean canConfigureEnvironment) {
        String head = "The ByteCodeTranslator ran out of memory (it was given -Xmx" + heapMB + "m). "
                + "This app needs a larger translation heap than the build machine's memory budget allows. ";
        if (canConfigureEnvironment) {
            return head + "Raise it by setting " + HEAP_ENV + " to a larger value in MB, or pass an "
                    + "explicit heap via CN1_TRANSLATOR_OPTS (e.g. CN1_TRANSLATOR_OPTS=-Xmx6g), and "
                    + "build again.";
        }
        return head + "Building locally lets you raise the ceiling via " + HEAP_ENV + "; otherwise "
                + "reducing the amount of code in the app (dropping unused libraries) brings it back "
                + "under the limit. Please report the app size on the issue tracker if neither applies.";
    }

    private static int parseMB(String value) {
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Memory this process is actually allowed to use, in MB, or -1 when it cannot
     * be determined. A cgroup limit wins over physical RAM: inside a container the
     * host's RAM is visible but irrelevant, and sizing against it is how a build
     * ends up OOM-killed by the kernel.
     */
    static long detectBudgetMB() {
        long cgroup = cgroupLimitMB();
        if (cgroup > 0) {
            return cgroup;
        }
        return physicalMemoryMB();
    }

    /**
     * Reads the memory limit imposed on this process by its cgroup, in MB, or -1
     * when unlimited / unavailable. Handles cgroup v2 ({@code memory.max}, which
     * reads "max" when unlimited) and v1 ({@code memory.limit_in_bytes}, which
     * reports a sentinel near Long.MAX_VALUE when unlimited).
     */
    private static long cgroupLimitMB() {
        long v2 = readLimitFile(new File("/sys/fs/cgroup/memory.max"));
        if (v2 > 0) {
            return v2;
        }
        return readLimitFile(new File("/sys/fs/cgroup/memory/memory.limit_in_bytes"));
    }

    private static long readLimitFile(File f) {
        try {
            if (!f.isFile()) {
                return -1;
            }
            String raw = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            if (raw.isEmpty() || "max".equals(raw)) {
                return -1;
            }
            long bytes = Long.parseLong(raw);
            // An "unlimited" v1 cgroup reports a huge sentinel (typically
            // LONG_MAX rounded down to the page size). Anything at or above a
            // petabyte is that sentinel, not a real limit.
            if (bytes <= 0 || bytes >= (1024L * 1024L * 1024L * 1024L * 1024L)) {
                return -1;
            }
            return bytes / (1024L * 1024L);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Total physical RAM in MB, or -1 when unavailable.
     *
     * <p>Reflective on purpose. The accessor lives on
     * {@code com.sun.management.OperatingSystemMXBean}, which is not guaranteed to
     * exist on every JVM, and it was renamed from {@code getTotalPhysicalMemorySize}
     * to {@code getTotalMemorySize} in Java 14 while this code still compiles at
     * source level 8. Resolving the method against the public *interface* (rather
     * than the non-public implementation class) means no setAccessible call and no
     * illegal-access warning under JPMS.
     */
    private static long physicalMemoryMB() {
        Object bean;
        Class<?> sunOsBean;
        try {
            bean = ManagementFactory.getOperatingSystemMXBean();
            sunOsBean = Class.forName("com.sun.management.OperatingSystemMXBean");
        } catch (Exception ex) {
            return -1;
        } catch (LinkageError err) {
            return -1;
        }
        if (bean == null || !sunOsBean.isInstance(bean)) {
            return -1;
        }
        // Java 14+ name first, then the Java 8..13 spelling.
        String[] candidates = {"getTotalMemorySize", "getTotalPhysicalMemorySize"};
        for (String name : candidates) {
            long bytes = invokeLongAccessor(sunOsBean, bean, name);
            if (bytes > 0) {
                return bytes / (1024L * 1024L);
            }
        }
        return -1;
    }

    /**
     * Invokes a no-arg accessor by name, returning -1 when it is absent on this
     * JVM (the accessor was renamed in Java 14) or fails for any reason.
     */
    private static long invokeLongAccessor(Class<?> owner, Object target, String name) {
        try {
            Method m = owner.getMethod(name);
            Object result = m.invoke(target);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
            return -1;
        } catch (Exception ex) {
            return -1;
        }
    }
}
