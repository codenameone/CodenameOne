/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.javase.simulator;

/**
 * One named action contributed by a cn1lib (or the app) to the simulator.
 *
 * <p>A hook is always callable by id via
 * {@link com.codename1.system.SimulatorHookExecutor#execute} (and from the
 * cross-platform {@code CN.executeHook}). A hook with a non-empty label
 * also appears in the simulator's menu bar; hooks without a label are
 * API-only — useful for test scaffolding the user wouldn't click.</p>
 */
public final class SimulatorHook {
    private final String namespace;
    private final String id;
    private final String menuName;
    private final String label;
    private final Runnable invoke;

    public SimulatorHook(String namespace, String id, String menuName, String label, Runnable invoke) {
        this.namespace = namespace;
        this.id = id;
        this.menuName = menuName;
        this.label = label;
        this.invoke = invoke;
    }

    /** Stable namespace token (one per properties file). */
    public String getNamespace() { return namespace; }

    /** Stable id within the namespace; the executor key is {@code namespace:id}. */
    public String getId() { return id; }

    /** Fully-qualified executor key — {@code namespace + ":" + id}. */
    public String getExecutorKey() { return namespace + ":" + id; }

    /** Display title of the menu this hook belongs to (one per properties file). */
    public String getMenuName() { return menuName; }

    /**
     * Display label for the menu item, or {@code null}/empty if this hook is
     * API-only (callable through {@link #getExecutorKey()} / {@code CN.executeHook}
     * but invisible in the simulator menu).
     */
    public String getLabel() { return label; }

    /** Invokes the configured static action on the CN1 EDT. */
    public Runnable getInvoke() { return invoke; }

    /** True if this hook should render as a menu item (label is non-empty). */
    public boolean hasMenuLabel() {
        return label != null && label.trim().length() > 0;
    }
}
