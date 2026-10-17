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
package com.codename1.settings.extensions;

/// The Maven [DependencyEditor]: declares dependencies in a `pom.xml`.
public final class PomEditor implements DependencyEditor {
    /// The shared instance [DependencyEditor#forBuildSystem(String)] returns.
    public static final PomEditor INSTANCE = new PomEditor();

    private PomEditor() {
    }

    @Override
    public boolean contains(String text, MavenDependency dependency) {
        return containsDependency(text, dependency);
    }

    @Override
    public String add(String text, MavenDependency dependency) {
        return addDependency(text, dependency);
    }

    @Override
    public String remove(String text, MavenDependency dependency) {
        return removeDependency(text, dependency);
    }

    @Override
    public String fileLabel() {
        return "common/pom.xml";
    }

    public static boolean containsDependency(String pom, MavenDependency dependency) {
        if (pom == null || dependency == null) {
            return false;
        }
        Element deps = child(parse(pom), "dependencies");
        if (deps != null) {
            for (Element entry : deps.children) {
                if ("dependency".equals(entry.name) && matches(pom, entry, dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String addDependency(String pom, MavenDependency dependency) {
        if (pom == null) {
            pom = "";
        }
        if (dependency == null || !dependency.isValid() || containsDependency(pom, dependency)) {
            return pom;
        }
        String xml = "        <dependency>\n"
                + "            <groupId>" + xml(dependency.groupId()) + "</groupId>\n"
                + "            <artifactId>" + xml(dependency.artifactId()) + "</artifactId>\n"
                + "            <version>" + xml(dependency.version()) + "</version>\n"
                + (dependency.type().length() == 0 ? "" : "            <type>" + xml(dependency.type()) + "</type>\n")
                + "        </dependency>\n";
        Element project = parse(pom);
        Element deps = child(project, "dependencies");
        if (deps != null) {
            return insert(pom, deps, xml);
        }
        if (project != null) {
            return insert(pom, project, "    <dependencies>\n" + xml + "    </dependencies>\n");
        }
        throw new IllegalArgumentException("Cannot find the Maven project element");
    }

    public static String removeDependency(String pom, MavenDependency dependency) {
        if (pom == null || dependency == null) {
            return pom;
        }
        Element deps = child(parse(pom), "dependencies");
        if (deps == null) {
            return pom;
        }
        for (Element entry : deps.children) {
            if ("dependency".equals(entry.name) && matches(pom, entry, dependency)) {
                int start = entry.start;
                while (start > 0 && (pom.charAt(start - 1) == ' ' || pom.charAt(start - 1) == '\t')) {
                    start--;
                }
                int end = entry.end;
                while (end < pom.length() && (pom.charAt(end) == ' ' || pom.charAt(end) == '\t')) {
                    end++;
                }
                if (end < pom.length() && pom.charAt(end) == '\n') {
                    end++;
                }
                return pom.substring(0, start) + pom.substring(end);
            }
        }
        return pom;
    }

    private static String insert(String text, Element element, String xml) {
        if (element.end == element.openEnd) { // Self-closing element.
            return text.substring(0, element.openEnd - 2) + ">\n" + xml
                    + "</" + element.name + ">" + text.substring(element.end);
        }
        return text.substring(0, element.close) + xml + text.substring(element.close);
    }

    private static boolean matches(String pom, Element entry, MavenDependency dependency) {
        return value(pom, child(entry, "groupId")).equals(xml(dependency.groupId()))
                && value(pom, child(entry, "artifactId")).equals(xml(dependency.artifactId()));
    }

    private static String value(String text, Element element) {
        return element == null ? "" : text.substring(element.openEnd, element.close).trim();
    }

    private static Element child(Element parent, String name) {
        if (parent != null) {
            for (Element child : parent.children) {
                if (name.equals(child.name)) {
                    return child;
                }
            }
        }
        return null;
    }

    // Keep source offsets rather than serializing an XML tree: unrelated POM
    // formatting and comments must survive edits. Only direct project children
    // are eligible; plugin, managed and profile dependencies are separate scopes.
    private static final class Element {
        String name;
        int start, openEnd, close, end;
        java.util.List<Element> children = new java.util.ArrayList<>();
    }

    private static Element parse(String text) {
        java.util.List<Element> stack = new java.util.ArrayList<>();
        Element root = null;
        for (int pos = 0; (pos = text.indexOf('<', pos)) >= 0;) {
            String terminator = text.startsWith("<!--", pos) ? "-->"
                    : text.startsWith("<![CDATA[", pos) ? "]]>"
                    : text.startsWith("<?", pos) ? "?>" : null;
            if (terminator != null) {
                int end = text.indexOf(terminator, pos + 2);
                if (end < 0) throw new IllegalArgumentException("Incomplete XML markup");
                pos = end + terminator.length();
                continue;
            }
            if (text.startsWith("<!DOCTYPE", pos) && pos + 9 < text.length()
                    && Character.isWhitespace(text.charAt(pos + 9))) {
                pos = doctypeEnd(text, pos + 9);
                continue;
            }
            if (text.startsWith("<!", pos)) {
                throw new IllegalArgumentException("Unsupported XML declaration in POM");
            }
            int end = pos + 1;
            char quote = 0;
            for (; end < text.length(); end++) {
                char ch = text.charAt(end);
                if (quote != 0) {
                    if (ch == quote) quote = 0;
                } else if (ch == '\'' || ch == '"') {
                    quote = ch;
                } else if (ch == '>') {
                    break;
                }
            }
            if (end == text.length()) throw new IllegalArgumentException("Incomplete XML tag");
            boolean closing = text.charAt(pos + 1) == '/';
            int nameStart = pos + (closing ? 2 : 1);
            int nameEnd = nameStart;
            while (nameEnd < end && !Character.isWhitespace(text.charAt(nameEnd))
                    && text.charAt(nameEnd) != '/') nameEnd++;
            String name = text.substring(nameStart, nameEnd);
            if (closing) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Unexpected XML closing tag");
                Element element = stack.remove(stack.size() - 1);
                if (!element.name.equals(name)) throw new IllegalArgumentException("Mismatched XML tag");
                element.close = pos;
                element.end = end + 1;
            } else {
                Element element = new Element();
                element.name = name;
                element.start = pos;
                element.openEnd = end + 1;
                if (stack.isEmpty()) {
                    if (root != null || !"project".equals(name)) {
                        throw new IllegalArgumentException("Expected one Maven project element");
                    }
                    root = element;
                } else {
                    stack.get(stack.size() - 1).children.add(element);
                }
                if (text.charAt(end - 1) == '/') {
                    element.close = element.end = end + 1;
                } else {
                    stack.add(element);
                }
            }
            pos = end + 1;
        }
        if (!stack.isEmpty()) throw new IllegalArgumentException("Unclosed XML element");
        return root;
    }

    // Lexically skip the declaration, including any internal subset. Never
    // resolve a SYSTEM/PUBLIC identifier or expand a declared entity.
    private static int doctypeEnd(String text, int pos) {
        int subset = 0;
        char quote = 0;
        for (; pos < text.length(); pos++) {
            char ch = text.charAt(pos);
            if (quote != 0) {
                if (ch == quote) quote = 0;
            } else if (text.startsWith("<!--", pos) || text.startsWith("<?", pos)) {
                String terminator = text.startsWith("<!--", pos) ? "-->" : "?>";
                int end = text.indexOf(terminator, pos + 2);
                if (end < 0) throw new IllegalArgumentException("Incomplete POM DOCTYPE");
                pos = end + terminator.length() - 1;
            } else if (ch == '\'' || ch == '"') {
                quote = ch;
            } else if (ch == '[') {
                subset++;
            } else if (ch == ']') {
                subset--;
            } else if (ch == '>' && subset == 0) {
                return pos + 1;
            }
        }
        throw new IllegalArgumentException("Incomplete POM DOCTYPE");
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
