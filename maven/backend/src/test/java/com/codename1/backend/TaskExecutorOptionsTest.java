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
package com.codename1.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TaskExecutorOptionsTest {
    @Test
    void decoratorPrefixAndCapacityApply() throws Exception {
        TaskExecutor executor = new TaskExecutor("opts", false, 1, null);
        executor.configure(1, "mail-");
        final AtomicReference<String> seen = new AtomicReference<String>();
        executor.setTaskDecorator(new TaskDecorator() {
            @Override
            public Runnable decorate(final Runnable task) {
                return new Runnable() {
                    @Override
                    public void run() {
                        seen.set("decorated on " + Thread.currentThread().getName());
                        task.run();
                    }
                };
            }
        });
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                started.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));
        assertEquals("decorated on mail-0", seen.get());
        Runnable nothing = new Runnable() {
            @Override
            public void run() {
            }
        };
        executor.execute(nothing);
        assertThrows(TaskRejectedException.class, () -> executor.execute(nothing));
        release.countDown();
        executor.shutdown(5000);
    }

    @Test
    void argumentsParseAsSpringBootDoes() {
        ApplicationArguments a = new ApplicationArguments("--port=8080", "--debug", "in.txt", "--port=9", "--");
        assertEquals(java.util.Arrays.asList("8080", "9"), a.getOptionValues("port"));
        assertTrue(a.containsOption("debug"));
        assertTrue(a.getOptionValues("debug").isEmpty());
        assertEquals(java.util.Arrays.asList("in.txt", "--"), a.getNonOptionArgs());
        assertEquals(null, a.getOptionValues("missing"));
    }
}
