/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import dart.runtime.Funcs;

/**
 * Base class for elements that compose exactly one child by calling a build
 * method (Flutter's ComponentElement): {@link StatelessElement} and
 * {@link StatefulElement}.
 */
public abstract class ComposedElement extends Element {

    private Element child;

    protected ComposedElement(Widget widget) {
        super(widget);
    }

    public Element child() {
        return child;
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        firstBuild();
    }

    protected void firstBuild() {
        dirty = true;
        performRebuild();
    }

    @Override
    public void update(Widget newWidget) {
        super.update(newWidget);
        dirty = true;
        performRebuild();
    }

    private static long diagMs;
    private static long buildMs;
    private static long updateMs;
    private static int builds;

    /// Build cost per widget class, keyed by the Class itself so the hot path
    /// never formats a name. "115 builds cost 462ms" is not actionable; knowing
    /// WHICH build method owns them is.
    private static final java.util.Map<Class<?>, long[]> BY_CLASS =
            new java.util.HashMap<Class<?>, long[]>();

    /** Where a composed element's rebuild time goes, worst build methods first. */
    public static String rebuildCost() {
        java.util.List<java.util.Map.Entry<Class<?>, long[]>> rows =
                new java.util.ArrayList<java.util.Map.Entry<Class<?>, long[]>>(BY_CLASS.entrySet());
        java.util.Collections.sort(rows, new java.util.Comparator<java.util.Map.Entry<Class<?>, long[]>>() {
            @Override
            public int compare(java.util.Map.Entry<Class<?>, long[]> a,
                    java.util.Map.Entry<Class<?>, long[]> b) {
                return Long.compare(b.getValue()[0], a.getValue()[0]);
            }
        });
        StringBuilder sb = new StringBuilder();
        sb.append(builds).append(" build(s) diag=").append(diagMs).append("ms build=")
                .append(buildMs).append("ms; hottest:");
        for (int i = 0; i < rows.size() && i < 6; i++) {
            java.util.Map.Entry<Class<?>, long[]> e = rows.get(i);
            String n = e.getKey().getName();
            int dot = n.lastIndexOf('.');
            sb.append(' ').append(dot < 0 ? n : n.substring(dot + 1))
                    .append('=').append(e.getValue()[0]).append("ms/")
                    .append(e.getValue()[1]).append('x');
        }
        return sb.toString();
    }

    @Override
    protected void performRebuild() {
        dirty = false;
        // Name the widget being built, so a failure inside it (a Dart `!` on
        // something that turned out null, most often) reports where it
        // happened. The transpiled build methods are inlined into the
        // framework's frame on some backends, so the stack trace alone shows
        // nothing but this class's own recursion. The WIDGET is handed over
        // rather than a description of it: describing costs a String per
        // build, and nothing reads the description unless a build throws.
        Object previous = dart.runtime.DartRuntime.diagnosticContextValue();
        dart.runtime.DartRuntime.diagnosticContext(widget);
        if (!Trace.on()) {
            try {
                child = updateChild(child, build(), 0);
            } finally {
                dart.runtime.DartRuntime.diagnosticContext(previous);
            }
            return;
        }
        builds++;
        long d1 = System.currentTimeMillis();
        Widget built;
        try {
            built = build();
        } finally {
            dart.runtime.DartRuntime.diagnosticContext(previous);
        }
        long d2 = System.currentTimeMillis();
        buildMs += d2 - d1;
        if (widget != null) {
            long[] row = BY_CLASS.get(widget.getClass());
            if (row == null) {
                row = new long[2];
                BY_CLASS.put(widget.getClass(), row);
            }
            row[0] += d2 - d1;
            row[1]++;
        }
        child = updateChild(child, built, 0);
        updateMs += System.currentTimeMillis() - d2;
    }

    /**
     * Calls the widget's (or state's) build method.
     */
    protected abstract Widget build();

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (child != null) {
            visitor.call(child);
        }
    }
}
