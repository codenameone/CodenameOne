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
package com.codename1.gradle;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Kotlin's classes were relocated by an action of `compileJava`, which never
/// runs when javac is up to date -- and it is whenever Kotlin's ABI is
/// unchanged, including on the build right after a relocation. Relocation is
/// now a task of its own that `compileJava` is finalized by and `classes`
/// depends on. The Gradle API jar cannot build a real Project here, so the
/// task container is a recording fake.
class KotlinRelocationWiringTest {

    /// What the wiring did to one task.
    static final class Recorded {
        final List<Object> dependsOn = new ArrayList<Object>();
        final List<Object> mustRunAfter = new ArrayList<Object>();
        final List<Object> finalizedBy = new ArrayList<Object>();
        final List<Object> actions = new ArrayList<Object>();
    }

    private final Map<String, Recorded> recorded = new HashMap<String, Recorded>();
    private final Map<String, Task> taskProxies = new HashMap<String, Task>();

    private Task task(String name) {
        Task existing = taskProxies.get(name);
        if (existing != null) {
            return existing;
        }
        final Recorded r = new Recorded();
        recorded.put(name, r);
        final Task[] self = new Task[1];
        InvocationHandler h = (proxy, m, args) -> {
            switch (m.getName()) {
                case "getName":
                    return name;
                case "dependsOn":
                    r.dependsOn.addAll(Arrays.asList((Object[]) args[0]));
                    return self[0];
                case "mustRunAfter":
                    r.mustRunAfter.addAll(Arrays.asList((Object[]) args[0]));
                    return self[0];
                case "finalizedBy":
                    r.finalizedBy.addAll(Arrays.asList((Object[]) args[0]));
                    return self[0];
                case "doLast":
                case "doFirst":
                    r.actions.add(args[args.length - 1]);
                    return self[0];
                case "setDescription":
                case "setGroup":
                    return null;
                default:
                    throw new UnsupportedOperationException(m.getName());
            }
        };
        self[0] = (Task) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Task.class}, h);
        taskProxies.put(name, self[0]);
        return self[0];
    }

    @SuppressWarnings("unchecked")
    private TaskProvider<Task> provider(String name) {
        InvocationHandler h = (proxy, m, args) -> {
            switch (m.getName()) {
                case "getName":
                    return name;
                case "get":
                    return task(name);
                case "configure":
                    ((Action<Task>) args[0]).execute(task(name));
                    return null;
                case "toString":
                    return "provider(" + name + ")";
                case "hashCode":
                    return name.hashCode();
                case "equals":
                    return proxy == args[0];
                default:
                    throw new UnsupportedOperationException(m.getName());
            }
        };
        return (TaskProvider<Task>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {TaskProvider.class}, h);
    }

    @SuppressWarnings("unchecked")
    private TaskContainer container() {
        InvocationHandler h = (proxy, m, args) -> {
            String name = (String) args[0];
            if (m.getName().equals("register") && args.length == 2 && args[1] instanceof Action) {
                ((Action<Task>) args[1]).execute(task(name));
                return provider(name);
            }
            if (m.getName().equals("named") && args.length == 1) {
                return provider(name);
            }
            throw new UnsupportedOperationException(m.getName());
        };
        return (TaskContainer) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {TaskContainer.class}, h);
    }

    @Test
    void kotlinIsRelocatedByATaskThatDoesNotDependOnJavacRunning() {
        Action<Task> remap = t -> { };
        TaskProvider<Task> relocate = AppSupport.registerKotlinRelocation(container(), "compileJava", "classes",
                remap);

        Recorded javac = recorded.get("compileJava");
        assertTrue(javac.actions.isEmpty(), "an up-to-date compileJava runs no actions");
        assertEquals(1, javac.finalizedBy.size());
        assertSame(relocate, javac.finalizedBy.get(0), "compileJava, even up to date, is followed by relocation");

        Recorded r = recorded.get(relocate.getName());
        assertTrue(r.dependsOn.contains("compileKotlin"));
        assertTrue(r.mustRunAfter.contains("compileJava"), "javac must see Kotlin's classes unrelocated");
        assertEquals(1, r.actions.size());
        assertSame(remap, r.actions.get(0));

        assertTrue(recorded.get("classes").dependsOn.contains(relocate),
                "every Codename One task depends on classes, so it waits for relocation");
    }

    /// The Swing runtime is written under the names it ships with, so
    /// Kotlin's classes can only be checked once they are relocated. The
    /// relocation task then does both, in that order, and a module that
    /// checks Kotlin where it is compiled passes no check here at all.
    @Test
    void complianceFollowsRelocationInTheSameTask() {
        final List<String> ran = new ArrayList<String>();
        Action<Task> remap = t -> ran.add("remap");
        Action<Task> compliance = t -> ran.add("compliance");

        AppSupport.kotlinRelocation(remap, compliance).execute(task("cn1RemapAndroidKotlin"));
        assertEquals(Arrays.asList("remap", "compliance"), ran);

        ran.clear();
        AppSupport.kotlinRelocation(remap, null).execute(task("cn1RemapAndroidKotlin"));
        assertEquals(Arrays.asList("remap"), ran);
    }

    /// A failed relocation leaves classes the check would misreport.
    @Test
    void aFailedRelocationSkipsTheCheck() {
        final List<String> ran = new ArrayList<String>();
        Action<Task> remap = t -> {
            throw new IllegalStateException("remap");
        };
        Action<Task> compliance = t -> ran.add("compliance");
        try {
            AppSupport.kotlinRelocation(remap, compliance).execute(task("cn1RemapAndroidKotlin"));
            throw new AssertionError("the failure must reach Gradle");
        } catch (IllegalStateException expected) {
            assertTrue(ran.isEmpty());
        }
    }
}
