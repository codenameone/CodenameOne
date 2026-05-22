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
package com.codename1.impl.javase.simulator;

/**
 * One positional action contributed by a cn1lib (or the app) to the simulator.
 *
 * <p>Hooks are positional within each {@code simulator-hooks.properties}
 * file: {@code item1} is the first menu entry, {@code item2} the second,
 * etc. A hook with a non-empty {@link #getLabel() label} renders as a menu
 * item; a label-less hook is API-only -- invisible in the menu but still
 * callable via {@code CN.execute("namespace:itemN")} for test scaffolding.</p>
 */
public final class SimulatorHook {
    private final String namespace;
    private final int index;
    private final String menuName;
    private final String label;
    private final Runnable invoke;

    public SimulatorHook(String namespace, int index, String menuName, String label, Runnable invoke) {
        this.namespace = namespace;
        this.index = index;
        this.menuName = menuName;
        this.label = label;
        this.invoke = invoke;
    }

    /** Stable namespace token (one per properties file). */
    public String getNamespace() { return namespace; }

    /** 1-based position of this item within its properties file. */
    public int getIndex() { return index; }

    /** URL passed to {@code CN.execute} to trigger this hook -- {@code namespace + ":item" + index}. */
    public String getExecutorKey() { return namespace + ":item" + index; }

    /** Display title of the menu this hook belongs to (one per properties file). */
    public String getMenuName() { return menuName; }

    /**
     * Display label for the menu item, or {@code null}/empty if this hook is
     * API-only (callable through {@link #getExecutorKey()} / {@code CN.execute}
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
