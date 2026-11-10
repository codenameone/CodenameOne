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
package com.codename1.cli;

import java.util.ArrayList;
import java.util.List;

/// A hierarchy rendered the way `tree` prints a directory:
///
/// ```java
/// Tree root = new Tree("app");
/// Tree src = root.add("src");
/// src.add("Main.java");
/// root.add("pom.xml");
/// Console.print(root.render());
/// ```
public final class Tree {
    private final String label;
    private final List<Tree> children = new ArrayList<Tree>();

    /// A node showing `label`, which may be styled.
    public Tree(String label) {
        this.label = label == null ? "" : label;
    }

    /// Adds a child showing `label` and answers it, to add grandchildren to.
    public Tree add(String label) {
        Tree child = new Tree(label);
        children.add(child);
        return child;
    }

    /// Adds `child` and answers this node.
    public Tree add(Tree child) {
        children.add(child);
        return this;
    }

    /// The node's label.
    public String label() {
        return label;
    }

    /// The tree as text, with box-drawing guides when [Console#unicode] says
    /// they are safe and ASCII ones otherwise.
    public String render() {
        return render(Console.unicode());
    }

    /// The tree as text, with box-drawing guides when `unicode`.
    public String render(boolean unicode) {
        StringBuilder sb = new StringBuilder();
        appendLabel(sb, "", label);
        renderChildren(sb, "", unicode);
        return sb.toString();
    }

    @Override
    public String toString() {
        return render();
    }

    private void renderChildren(StringBuilder sb, String prefix, boolean unicode) {
        String branch = unicode ? "\u251c\u2500\u2500 " : "|-- ";
        String last = unicode ? "\u2514\u2500\u2500 " : "`-- ";
        String pipe = unicode ? "\u2502   " : "|   ";
        for (int i = 0; i < children.size(); i++) {
            Tree child = children.get(i);
            boolean isLast = i == children.size() - 1;
            String guide = prefix + (isLast ? last : branch);
            String continuation = prefix + (isLast ? "    " : pipe);
            String[] lines = TextWidth.splitLines(child.label);
            sb.append(guide).append(lines[0]).append('\n');
            for (int l = 1; l < lines.length; l++) {
                sb.append(continuation).append(lines[l]).append('\n');
            }
            child.renderChildren(sb, continuation, unicode);
        }
    }

    private static void appendLabel(StringBuilder sb, String prefix, String label) {
        for (String line : TextWidth.splitLines(label)) {
            sb.append(prefix).append(line).append('\n');
        }
    }
}
