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

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The heap policy behind issue #5511: a hard-coded {@code -Xmx} made large apps
 * untranslatable. These pin the properties the builders rely on -- most
 * importantly that the change can never hand a target LESS heap than the
 * constant it replaced.
 */
public class TranslatorHeapTest {

    @Test
    public void sizesFromHalfTheDetectedBudget() {
        // 6GB budget -> 3GB, below the ceiling.
        assertEquals(3072, TranslatorHeap.maxHeapMB(512, null, 6144));
    }

    @Test
    public void neverExceedsTheCeiling() {
        // A 64GB workstation must not hand the translator 32GB.
        assertEquals(TranslatorHeap.CEILING_MB, TranslatorHeap.maxHeapMB(512, null, 65536));
    }

    @Test
    public void neverGoesBelowTheTargetsHistoricalHeap() {
        // A 1GB box would compute 512m for the iOS target, whose builder has
        // always used 1024m. Auto-sizing must not be a downgrade.
        assertEquals(1024, TranslatorHeap.maxHeapMB(1024, null, 1024));
        // ... and the JS target keeps its own 512m floor on the same box.
        assertEquals(512, TranslatorHeap.maxHeapMB(512, null, 1024));
    }

    @Test
    public void fallsBackToTheHistoricalHeapWhenMemoryIsUndetectable() {
        assertEquals(768, TranslatorHeap.maxHeapMB(768, null, -1));
        assertEquals(768, TranslatorHeap.maxHeapMB(768, null, 0));
    }

    @Test
    public void environmentOverrideWinsOverAutoSizing() {
        // Auto-sizing would pick 4096 here (half of 8192, at the ceiling); the
        // explicit setting takes precedence in both directions.
        assertEquals(2500, TranslatorHeap.maxHeapMB(512, "2500", 8192));
        // Whitespace is tolerated -- these get set by hand in a systemd unit.
        assertEquals(2500, TranslatorHeap.maxHeapMB(512, "  2500 ", 8192));
    }

    @Test
    public void environmentOverrideMayGoBelowTheFloorAndAboveTheCeiling() {
        // Deliberately starving a small box is allowed...
        assertEquals(256, TranslatorHeap.maxHeapMB(1024, "256", 8192));
        // ...and so is going past the ceiling, which only bounds the automatic
        // choice. A box with 32GB may legitimately want 6GB for a huge app.
        assertEquals(6144, TranslatorHeap.maxHeapMB(512, "6144", 32768));
    }

    @Test
    public void environmentOverrideCannotExceedTheMachinesMemory() {
        // The shape of a typo (an extra digit) is also the setting that OOM-kills
        // the build box instead of failing the one build.
        assertEquals(8192, TranslatorHeap.maxHeapMB(512, "999999", 8192));
        // With no detectable budget there is nothing to clamp against, so the
        // operator's number stands.
        assertEquals(6144, TranslatorHeap.maxHeapMB(512, "6144", -1));
    }

    @Test
    public void malformedEnvironmentOverrideIsIgnored() {
        assertEquals(3072, TranslatorHeap.maxHeapMB(512, "not-a-number", 6144));
        assertEquals(3072, TranslatorHeap.maxHeapMB(512, "", 6144));
        // A non-positive value is meaningless as a heap; fall through to auto-sizing.
        assertEquals(3072, TranslatorHeap.maxHeapMB(512, "0", 6144));
        assertEquals(3072, TranslatorHeap.maxHeapMB(512, "-1", 6144));
    }

    @Test
    public void recognisesAnOutOfMemoryDeath() {
        assertTrue(TranslatorHeap.looksOutOfMemory(
                "Exception in thread \"main\" java.lang.OutOfMemoryError: Java heap space"));
        assertTrue(TranslatorHeap.looksOutOfMemory(
                "java.lang.OutOfMemoryError: GC overhead limit exceeded"));
        assertTrue(TranslatorHeap.looksOutOfMemory(
                "There is insufficient memory for the Java Runtime Environment to continue."));
    }

    @Test
    public void doesNotMistakeAnOrdinaryFailureForOutOfMemory() {
        assertFalse(TranslatorHeap.looksOutOfMemory(null));
        assertFalse(TranslatorHeap.looksOutOfMemory(""));
        assertFalse(TranslatorHeap.looksOutOfMemory(
                "Exception in thread \"main\" java.lang.NullPointerException"));
    }

    @Test
    public void adviceNamesTheHeapInUse() {
        String local = TranslatorHeap.outOfMemoryAdvice(2048, true);
        assertTrue(local.contains("-Xmx2048m"));
        assertTrue(local.contains(TranslatorHeap.HEAP_ENV));
        assertTrue(local.contains("CN1_TRANSLATOR_OPTS"));
    }

    @Test
    public void cloudAdviceDoesNotTellTheDeveloperToSetAServerSideVariable() {
        // On a cloud build the environment is not the developer's to change, so
        // "export CN1_TRANSLATOR_OPTS" would be advice they cannot act on.
        String cloud = TranslatorHeap.outOfMemoryAdvice(2048, false);
        assertTrue(cloud.contains("-Xmx2048m"));
        assertFalse(cloud.contains("CN1_TRANSLATOR_OPTS"));
    }

    @Test
    public void detectsAMemoryBudgetWhereverTheJvmExposesOne() {
        // The whole policy degrades to the old constants if detection returns -1,
        // so this guards against that regression going unnoticed. Asserting it
        // unconditionally would make the test environment-dependent -- detection
        // legitimately returns -1 on a JVM without the com.sun accessor -- so the
        // assertion is tied to the exact condition that makes it obligatory.
        boolean accessorAvailable;
        try {
            Class<?> sunOsBean = Class.forName("com.sun.management.OperatingSystemMXBean");
            accessorAvailable = sunOsBean.isInstance(
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean());
        } catch (Throwable ex) {
            accessorAvailable = false;
        }
        if (accessorAvailable) {
            assertTrue("physical memory is readable on this JVM, so detection must succeed",
                    TranslatorHeap.detectBudgetMB() > 0);
        }
    }

    @Test
    public void readsTheCgroupLimitFromThisProcessesOwnCgroupNotTheMountRoot() throws Exception {
        // A systemd unit with MemoryMax=, or a container sharing the host cgroup
        // namespace: the mount root is unlimited and the real limit sits at the
        // path named in /proc/self/cgroup. Reading only the root here would fall
        // back to host RAM and pick a heap the cgroup cannot honour.
        File root = tempDir();
        write(new File(root, "memory.max"), "max");
        File leaf = new File(root, "system.slice/cn1-daemon.service");
        assertTrue(leaf.mkdirs());
        write(new File(leaf, "memory.max"), String.valueOf(2048L * 1024 * 1024));

        assertEquals(2048, TranslatorHeap.cgroupLimitMB(root, "0::/system.slice/cn1-daemon.service\n"));
    }

    @Test
    public void takesTheTightestLimitOnThePath() throws Exception {
        // A limit set anywhere on the path applies, so an ancestor's tighter
        // limit is the one that will actually kill the build.
        File root = tempDir();
        write(new File(root, "memory.max"), "max");
        File mid = new File(root, "system.slice");
        assertTrue(mid.mkdirs());
        write(new File(mid, "memory.max"), String.valueOf(1024L * 1024 * 1024));
        File leaf = new File(mid, "cn1-daemon.service");
        assertTrue(leaf.mkdirs());
        write(new File(leaf, "memory.max"), String.valueOf(4096L * 1024 * 1024));

        assertEquals(1024, TranslatorHeap.cgroupLimitMB(root, "0::/system.slice/cn1-daemon.service\n"));
    }

    @Test
    public void fallsBackToTheMountRootWhenTheProcessCgroupIsUnknown() throws Exception {
        // The container-with-its-own-namespace case: the root files ARE the
        // container's limit and /proc/self/cgroup may be unreadable.
        File root = tempDir();
        write(new File(root, "memory.max"), String.valueOf(3072L * 1024 * 1024));
        assertEquals(3072, TranslatorHeap.cgroupLimitMB(root, null));
    }

    @Test
    public void readsACgroupV1Limit() throws Exception {
        File root = tempDir();
        File leaf = new File(root, "memory/docker/abc123");
        assertTrue(leaf.mkdirs());
        write(new File(leaf, "memory.limit_in_bytes"), String.valueOf(1536L * 1024 * 1024));

        assertEquals(1536, TranslatorHeap.cgroupLimitMB(root,
                "8:memory:/docker/abc123\n4:cpu,cpuacct:/docker/abc123\n"));
    }

    @Test
    public void treatsAnUnlimitedCgroupAsNoLimit() throws Exception {
        File root = tempDir();
        write(new File(root, "memory.max"), "max");
        File v1 = new File(root, "memory");
        assertTrue(v1.mkdirs());
        // The v1 "unlimited" sentinel, not a real 8-exabyte budget.
        write(new File(v1, "memory.limit_in_bytes"), "9223372036854771712");

        assertEquals(-1, TranslatorHeap.cgroupLimitMB(root, "0::/\n"));
    }

    @Test
    public void parsesCgroupPathsIncludingOnesContainingColons() {
        assertEquals("/system.slice/cn1.service",
                TranslatorHeap.cgroupPath("0::/system.slice/cn1.service\n", ""));
        assertEquals("/docker/abc",
                TranslatorHeap.cgroupPath("8:memory:/docker/abc\n", "memory"));
        // "memory" must not match "memory_hugetlb" or similar in a controller list.
        assertNull(TranslatorHeap.cgroupPath("8:memoryfoo:/docker/abc\n", "memory"));
        // systemd scope names legitimately contain ':'.
        assertEquals("/user.slice/run-r:12.scope",
                TranslatorHeap.cgroupPath("0::/user.slice/run-r:12.scope\n", ""));
        assertNull(TranslatorHeap.cgroupPath(null, ""));
    }

    private static File tempDir() throws Exception {
        File d = File.createTempFile("cn1-cgroup-test", "");
        assertTrue(d.delete());
        assertTrue(d.mkdirs());
        d.deleteOnExit();
        return d;
    }

    private static void write(File f, String contents) throws Exception {
        Files.write(f.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
