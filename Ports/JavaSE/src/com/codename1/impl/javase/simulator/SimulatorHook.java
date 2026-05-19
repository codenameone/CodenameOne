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
 * One menu item contributed by a cn1lib (or the app) to the simulator. The UX
 * shell decides how to render it (a JMenuItem today, possibly something else
 * after the simulator UX is rewritten); contributors never reference any
 * Swing types.
 */
public final class SimulatorHook {
    private final String menuName;
    private final String label;
    private final Runnable invoke;

    public SimulatorHook(String menuName, String label, Runnable invoke) {
        this.menuName = menuName;
        this.label = label;
        this.invoke = invoke;
    }

    /** The grouping title (one per properties file). */
    public String getMenuName() { return menuName; }

    /** The display label for this item. */
    public String getLabel() { return label; }

    /** Invokes the configured static action on the CN1 EDT. */
    public Runnable getInvoke() { return invoke; }
}
