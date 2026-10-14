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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CleanTargetCommandTest {
    @TempDir
    Path directory;

    private ProcessBuilder child(String mode) {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", executable).toString(),
                "-cp", System.getProperty("java.class.path"), Child.class.getName(), mode)
                .directory(directory.toFile());
    }

    @Test
    void suppliesEofAndCapturesOutputLargerThanAPipeBuffer() throws Exception {
        String output = CleanTargetIntegrationTest.runCommand(child("output"), 15, TimeUnit.SECONDS);
        assertTrue(output.contains("STDIN_EOF"));
        assertTrue(output.contains("STDERR_CAPTURED"));
        assertTrue(output.endsWith("OUTPUT_COMPLETE"));
        assertTrue(output.length() > 1024 * 1024);
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.count(), "Successful commands should remove their temporary log");
        }
    }

    @Test
    void reportsNonzeroExitAndRetainsOutput() throws Exception {
        AssertionError failure = assertThrows(AssertionError.class,
                () -> CleanTargetIntegrationTest.runCommand(child("failure"), 15, TimeUnit.SECONDS));
        assertTrue(failure.getMessage().contains("EXIT_FAILURE"));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(1, files.count(), "Failed commands should retain their complete log");
        }
    }

    @Test
    void timesOutEvenWhenChildKeepsOutputOpen() throws Exception {
        long started = System.nanoTime();
        AssertionError failure = assertThrows(AssertionError.class,
                () -> CleanTargetIntegrationTest.runCommand(child("hang"), 5, TimeUnit.SECONDS));
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started) < 20);
        assertTrue(failure.getMessage().contains("Command timed out"));
        assertTrue(failure.getMessage().contains("CHILD_STARTED"));
    }

    public static class Child {
        public static void main(String[] args) throws Exception {
            System.out.println("CHILD_STARTED");
            System.out.flush();
            if ("hang".equals(args[0])) {
                Thread.sleep(60000);
            } else if ("failure".equals(args[0])) {
                System.err.println("EXIT_FAILURE");
                System.exit(7);
            } else {
                if (System.in.read() == -1) {
                    System.out.println("STDIN_EOF");
                }
                System.err.println("STDERR_CAPTURED");
                byte[] block = new byte[8192];
                java.util.Arrays.fill(block, (byte) 'x');
                for (int i = 0; i < 256; i++) {
                    System.out.write(block);
                }
                System.out.print("OUTPUT_COMPLETE");
            }
        }
    }
}
