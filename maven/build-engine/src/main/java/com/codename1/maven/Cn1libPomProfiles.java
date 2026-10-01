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
package com.codename1.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Reads which artifacts a Maven-published cn1lib contributes to each platform.
///
/// A cn1lib's `-lib` pom selects its per-platform jars with profiles activated
/// by `-Dcodename1.platform=<platform>` (see `cn1lib-archetype`'s `lib/pom.xml`).
/// Maven honours those; Gradle does not activate profiles on a property at all.
/// This reads the same profiles so the Gradle plugin can add exactly what Maven
/// would, from the same metadata, without guessing artifact names.
///
/// A deployed pom is not interpolated, so its profile dependencies still say
/// `${project.groupId}`, `${cn1lib.name}-android` and `${project.version}`. They
/// are resolved against the pom's own coordinates and properties and those of
/// its parent, which is where the cn1lib archetype declares `cn1lib.name`.
public final class Cn1libPomProfiles {
    /// Fetches a parent pom's text, or returns null when it cannot.
    public interface ParentResolver {
        /// The pom text of `groupId:artifactId:version`, or null.
        String pom(String groupId, String artifactId, String version);
    }

    /// One dependency a profile adds.
    public static final class Coordinate {
        /// The group id.
        public final String groupId;
        /// The artifact id.
        public final String artifactId;
        /// The version, or null when the pom leaves it to dependency management.
        public final String version;
        /// The classifier, or null.
        public final String classifier;
        /// The type (`jar` unless the pom says otherwise).
        public final String type;
        private final List<String[]> exclusions;

        Coordinate(String groupId, String artifactId, String version, String classifier, String type) {
            this(groupId, artifactId, version, classifier, type, null);
        }

        Coordinate(String groupId, String artifactId, String version, String classifier, String type,
                   List<String[]> exclusions) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type == null ? "jar" : type;
            this.exclusions = exclusions == null ? java.util.Collections.<String[]>emptyList()
                    : java.util.Collections.unmodifiableList(new ArrayList<String[]>(exclusions));
        }

        /// The dependency's `<exclusions>`, each {groupId, artifactId}, either
        /// of which may be Maven's `*` wildcard.
        public List<String[]> exclusions() {
            return exclusions;
        }

        /// `group:artifact:version[:classifier][@type]`, Gradle's dependency
        /// notation.
        public String toNotation() {
            StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId);
            if (version != null) {
                sb.append(':').append(version);
            }
            if (classifier != null) {
                sb.append(':').append(classifier);
            }
            if (!"jar".equals(type)) {
                sb.append('@').append(type);
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return toNotation();
        }
    }

    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)\\}");

    private Cn1libPomProfiles() {
    }

    /// The dependencies each `codename1.platform` profile of `pomText` adds,
    /// keyed by platform (`android`, `ios`, `javase`, ...). Empty when the pom
    /// is not a cn1lib's or cannot be parsed.
    public static Map<String, List<Coordinate>> read(String pomText, ParentResolver parents) {
        Map<String, List<Coordinate>> out = new LinkedHashMap<String, List<Coordinate>>();
        Element project = parse(pomText);
        if (project == null) {
            return out;
        }
        Map<String, String> props = properties(project, parents, 0);
        Map<String, String> managed = managedVersions(project, parents, 0, props);
        for (Element profile : children(child(project, "profiles"), "profile")) {
            Element activation = child(profile, "activation");
            Element property = child(activation, "property");
            if (property == null || !"codename1.platform".equals(text(child(property, "name")))) {
                continue;
            }
            String platform = text(child(property, "value"));
            if (platform == null) {
                continue;
            }
            List<Coordinate> deps = shipped(child(profile, "dependencies"), props, managed);
            List<Coordinate> existing = out.get(platform);
            if (existing == null) {
                out.put(platform, deps);
            } else {
                existing.addAll(deps);
            }
        }
        return out;
    }

    /// The dependencies `pomText` declares outside any profile, in the scopes
    /// that reach a build (not test, provided or system). A cn1lib's common
    /// module lists the cn1libs it uses here, as `pom`-type dependencies on
    /// their `-lib` artifacts -- which is how a consumer finds the libraries it
    /// uses indirectly, whose platform profiles Maven activates too.
    public static List<Coordinate> dependencies(String pomText, ParentResolver parents) {
        Element project = parse(pomText);
        if (project == null) {
            return new ArrayList<Coordinate>();
        }
        Map<String, String> props = properties(project, parents, 0);
        return shipped(child(project, "dependencies"), props, managedVersions(project, parents, 0, props));
    }

    /// The versions `<dependencyManagement>` gives, in the pom and its parents
    /// (nearest wins), keyed group:artifact. A dependency the pom declares
    /// without a version takes its version from here, as Maven does; without it
    /// a third-party platform dependency would reach Gradle versionless.
    private static Map<String, String> managedVersions(Element project, ParentResolver parents, int depth,
                                                       Map<String, String> props) {
        Map<String, String> out = new HashMap<String, String>();
        Element parent = child(project, "parent");
        String parentGroup = text(child(parent, "groupId"));
        String parentArtifact = text(child(parent, "artifactId"));
        String parentVersion = text(child(parent, "version"));
        if (parent != null && parents != null && depth < 8 && parentGroup != null && parentArtifact != null
                && parentVersion != null) {
            Element parentProject = parse(parents.pom(parentGroup, parentArtifact, parentVersion));
            if (parentProject != null) {
                out.putAll(managedVersions(parentProject, parents, depth + 1, props));
            }
        }
        for (Element dep : children(child(child(project, "dependencyManagement"), "dependencies"), "dependency")) {
            String g = interpolate(text(child(dep, "groupId")), props);
            String a = interpolate(text(child(dep, "artifactId")), props);
            String v = interpolate(text(child(dep, "version")), props);
            if (g != null && a != null && v != null) {
                out.put(g + ":" + a, v);
            }
        }
        return out;
    }

    private static List<Coordinate> shipped(Element dependencies, Map<String, String> props,
                                            Map<String, String> managed) {
        List<Coordinate> deps = new ArrayList<Coordinate>();
        for (Element dep : children(dependencies, "dependency")) {
            String scope = interpolate(text(child(dep, "scope")), props);
            if ("test".equals(scope) || "provided".equals(scope) || "system".equals(scope)) {
                continue;
            }
            List<String[]> exclusions = new ArrayList<String[]>();
            for (Element ex : children(child(dep, "exclusions"), "exclusion")) {
                exclusions.add(new String[] {interpolate(text(child(ex, "groupId")), props),
                        interpolate(text(child(ex, "artifactId")), props)});
            }
            String group = interpolate(text(child(dep, "groupId")), props);
            String artifact = interpolate(text(child(dep, "artifactId")), props);
            String version = interpolate(text(child(dep, "version")), props);
            if (version == null) {
                version = managed.get(group + ":" + artifact);
            }
            deps.add(new Coordinate(group, artifact, version,
                    interpolate(text(child(dep, "classifier")), props),
                    interpolate(text(child(dep, "type")), props), exclusions));
        }
        return deps;
    }

    /// The properties `${...}` expressions in the pom can name: its own
    /// `<properties>`, its parent's (recursively, nearest wins), and the
    /// `project.*` coordinates.
    private static Map<String, String> properties(Element project, ParentResolver parents, int depth) {
        Map<String, String> props = new HashMap<String, String>();
        Element parent = child(project, "parent");
        String parentGroup = text(child(parent, "groupId"));
        String parentArtifact = text(child(parent, "artifactId"));
        String parentVersion = text(child(parent, "version"));
        if (parent != null && parents != null && depth < 8 && parentGroup != null && parentArtifact != null
                && parentVersion != null) {
            Element parentProject = parse(parents.pom(parentGroup, parentArtifact, parentVersion));
            if (parentProject != null) {
                props.putAll(properties(parentProject, parents, depth + 1));
            }
        }
        for (Element p : children(child(project, "properties"), null)) {
            String value = text(p);
            props.put(p.getTagName(), value == null ? "" : value);
        }
        String groupId = text(child(project, "groupId"));
        String version = text(child(project, "version"));
        props.put("project.groupId", groupId != null ? groupId : parentGroup);
        props.put("project.version", version != null ? version : parentVersion);
        props.put("project.artifactId", text(child(project, "artifactId")));
        if (parentGroup != null) {
            props.put("project.parent.groupId", parentGroup);
        }
        if (parentVersion != null) {
            props.put("project.parent.version", parentVersion);
        }
        props.put("groupId", props.get("project.groupId"));
        props.put("version", props.get("project.version"));
        return props;
    }

    static String interpolate(String value, Map<String, String> props) {
        if (value == null) {
            return null;
        }
        String current = value;
        // A few passes, so a property defined in terms of another resolves; bounded
        // so a self-referencing one cannot loop.
        for (int pass = 0; pass < 5 && current.indexOf("${") >= 0; pass++) {
            Matcher m = PROPERTY.matcher(current);
            StringBuffer sb = new StringBuffer();
            boolean changed = false;
            while (m.find()) {
                String replacement = props.get(m.group(1));
                if (replacement == null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                } else {
                    changed = true;
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
            }
            m.appendTail(sb);
            current = sb.toString();
            if (!changed) {
                break;
            }
        }
        return current.trim();
    }

    private static Element parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document d = b.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
            return d.getDocumentElement();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && ((Element) n).getTagName().equals(name)) {
                return (Element) n;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        if (parent == null) {
            return Collections.emptyList();
        }
        List<Element> out = new ArrayList<Element>();
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n instanceof Element && (name == null || ((Element) n).getTagName().equals(name))) {
                out.add((Element) n);
            }
        }
        return out;
    }

    private static String text(Element e) {
        if (e == null) {
            return null;
        }
        String t = e.getTextContent();
        return t == null ? null : t.trim();
    }
}
