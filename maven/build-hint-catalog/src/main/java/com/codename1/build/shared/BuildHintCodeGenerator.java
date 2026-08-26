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
package com.codename1.build.shared;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Generates the {@code com.codename1.annotations.build} annotation types from
 * {@link BuildHints}, plus the binding table the annotation processor reads
 * back.
 *
 * <p>The generated sources are checked in rather than produced into
 * {@code target/}. {@code CodenameOne/src} is compiled by four independent
 * front ends -- the Maven core module, the Ant/NetBeans project, IntelliJ and
 * {@code ant core} -- and generating into {@code target/} reaches exactly one
 * of them. The failure mode is not a build error but a jar-identity split,
 * where {@code mvn install} and {@code ant core} produce different
 * {@code codenameone-core.jar}s. Checked-in sources also mean {@code @Ios(}
 * autocompletes in every IDE, which is the entire point of the feature.</p>
 *
 * <p>Run through {@code scripts/gen-build-hint-annotations.sh}; CI re-runs it
 * with {@code --check} and fails on any diff.</p>
 */
public final class BuildHintCodeGenerator {

    private static final String LICENSE =
        "/*\n"
      + " * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.\n"
      + " * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n"
      + " * This code is free software; you can redistribute it and/or modify it\n"
      + " * under the terms of the GNU General Public License version 2 only, as\n"
      + " * published by the Free Software Foundation.  Codename One designates this\n"
      + " * particular file as subject to the \"Classpath\" exception as provided\n"
      + " * by Oracle in the LICENSE file that accompanied this code.\n"
      + " *\n"
      + " * This code is distributed in the hope that it will be useful, but WITHOUT\n"
      + " * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n"
      + " * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n"
      + " * version 2 for more details (a copy is included in the LICENSE file that\n"
      + " * accompanied this code).\n"
      + " *\n"
      + " * You should have received a copy of the GNU General Public License version\n"
      + " * 2 along with this work; if not, write to the Free Software Foundation,\n"
      + " * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.\n"
      + " *\n"
      + " * Please contact Codename One through http://www.codenameone.com/ if you\n"
      + " * need additional information or have any questions.\n"
      + " */\n";

    private static final String GENERATED_NOTE =
        "/// Generated from com.codename1.build.shared.BuildHints by\n"
      + "/// BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and\n"
      + "/// re-run scripts/gen-build-hint-annotations.sh.\n";

    // NOT ...annotations.build: .gitignore carries a repo-wide **/build/* rule,
    // which would silently make every generated source uncommittable and leave
    // the CI drift gate with nothing to compare.
    private static final String PKG = "com.codename1.annotations.buildhints";
    private static final String PKG_PATH = "com/codename1/annotations/buildhints";

    private BuildHintCodeGenerator() {
    }

    /**
     * @param args annotation source root, the catalog source root for the
     *             generated binding table, then any number of further outputs:
     *             a directory receives the simulator schema, an {@code .adoc}
     *             file receives the developer guide's table
     */
    public static void main(String[] args) throws IOException {
        // The developer guide's table is not checked in: it is rendered from the
        // catalog every time the guide is built, so it cannot drift and there is
        // nothing for a hand edit to survive in. This mode writes that one file
        // and nothing else, so a documentation build does not also rewrite source
        // trees it has no business touching.
        if (args.length == 3 && "--table-only".equals(args[0])) {
            write(new File(args[2]),
                    asciidocTable(everything(BuildHintAnnotationReader.readFromSources(
                            new File(args[1], PKG_PATH)))));
            return;
        }
        if (args.length < 2) {
            System.err.println("usage: BuildHintCodeGenerator <annotation-src-root> "
                    + "<catalog-src-root> [output...]");
            System.exit(2);
        }
        File annRoot = new File(args[0], PKG_PATH);
        File catalogRoot = new File(args[1], "com/codename1/build/shared");
        if (!annRoot.isDirectory()) {
            throw new IOException("No build hint annotations at " + annRoot);
        }

        // The annotations are the source of truth for the hints they expose, so
        // they are READ here rather than written. Everything below is a view of
        // them; nothing restates them.
        List<BuildHints.Hint> annotated = BuildHintAnnotationReader.readFromSources(annRoot);

        // By the enum's own order rather than whichever group a hint name sorts
        // into first, so the views below do not reshuffle when a hint is added.
        Map<HintGroup, List<BuildHints.Hint>> byGroup =
                new TreeMap<HintGroup, List<BuildHints.Hint>>();
        Map<String, BuildHints.Hint> enums = new TreeMap<String, BuildHints.Hint>();
        for (BuildHints.Hint h : annotated) {
            List<BuildHints.Hint> list = byGroup.get(h.group());
            if (list == null) {
                list = new ArrayList<BuildHints.Hint>();
                byGroup.put(h.group(), list);
            }
            list.add(h);
            if (h.type() == HintType.ENUM) {
                BuildHints.Hint previous = enums.put(h.enumName(), h);
                if (previous != null && !previous.values().equals(h.values())) {
                    throw new IllegalStateException("Enum " + h.enumName()
                            + " is declared with two different domains: "
                            + previous.name() + " and " + h.name());
                }
            }
        }

        // Sorted by attribute, so every view below is byte-stable whatever order
        // the catalog happens to list a group's hints in. This used to sit
        // inside the annotation writer, and removing that took the ordering
        // with it -- the binding table's entries stayed the same and simply
        // moved, which is drift with no meaning.
        for (Map.Entry<HintGroup, List<BuildHints.Hint>> e : byGroup.entrySet()) {
            Collections.sort(e.getValue(), new Comparator<BuildHints.Hint>() {
                public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                    return a.attr().compareTo(b.attr());
                }
            });
        }

        // The annotations are NOT generated. They are the source of truth for the
        // hints they expose -- hand-written, and read back by
        // BuildHintAnnotationReader -- so nothing here writes into
        // CodenameOne/src.
        write(new File(catalogRoot, "BuildHintsFromAnnotations.java"),
                catalogViewSource(annotated));
        write(new File(catalogRoot, "BuildHintAnnotationBinding.java"), bindingSource(byGroup, enums));
        for (int i = 2; i < args.length; i++) {
            File target = new File(args[i]);
            if (target.getName().endsWith(".adoc") || target.getName().endsWith(".asciidoc")) {
                write(target, asciidocTable(everything(annotated)));
            } else {
                write(new File(target, "com/codename1/impl/javase/BuildHintCatalogDefaults.java"),
                        simulatorSchemaSource(byGroup));
            }
        }
        System.out.println("cn1: generated the build hint views");
    }

    private static String javaType(BuildHints.Hint h) {
        switch (h.type()) {
            case BOOLEAN: return "boolean";
            case INT: return "int";
            case ENUM: return h.enumName();
            case STRING_LIST: return "String[]";
            default: return "String";
        }
    }

    private static String defaultClause(BuildHints.Hint h) {
        String d = h.def();
        switch (h.type()) {
            case BOOLEAN:
                return "true".equals(d) ? "true" : "false";
            case INT:
                if (d != null && d.length() > 0) {
                    try {
                        return String.valueOf(Integer.parseInt(d.trim()));
                    } catch (NumberFormatException ignored) {
                        // fall through to zero
                    }
                }
                return "0";
            case ENUM:
                String constant = d != null && h.values().contains(d)
                        ? enumConstant(d) : enumConstant(h.values().get(0));
                return h.enumName() + "." + constant;
            case STRING_LIST:
                return "{}";
            default:
                // A literal IP address in generated source reads to a static analyser
                // as hardcoded configuration (PMD AvoidUsingHardCodedIP), and it is not
                // load-bearing here: the annotation's default clause is documentation
                // only, since the processor emits a hint solely for members the
                // developer actually wrote. The real default stays in the javadoc.
                if (d != null && LOOKS_LIKE_IP.matcher(d).matches()) {
                    return "\"\"";
                }
                return "\"" + esc(d == null ? "" : d) + "\"";
        }
    }

    private static final java.util.regex.Pattern LOOKS_LIKE_IP =
            java.util.regex.Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}|::1|[0-9a-fA-F:]*:[0-9a-fA-F:]+");

    /** Wire value to Java enum constant: {@code internalOnly} to INTERNAL_ONLY. */
    static String enumConstant(String wire) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wire.length(); i++) {
            char c = wire.charAt(i);
            if (Character.isUpperCase(c) && sb.length() > 0
                    && Character.isLowerCase(wire.charAt(i - 1))) {
                sb.append('_');
            }
            sb.append(Character.isLetterOrDigit(c) ? Character.toUpperCase(c) : '_');
        }
        String out = sb.toString();
        return Character.isDigit(out.charAt(0)) ? "V" + out : out;
    }

    private static String annotationSource(HintGroup group, List<BuildHints.Hint> hints) {
        StringBuilder sb = new StringBuilder(LICENSE);
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("import java.lang.annotation.ElementType;\n");
        sb.append("import java.lang.annotation.Retention;\n");
        sb.append("import java.lang.annotation.RetentionPolicy;\n");
        sb.append("import java.lang.annotation.Target;\n\n");
        sb.append(doc(groupBlurb(group), ""));
        sb.append("///\n");
        sb.append(doc("Place this on your application's main class -- the class named by "
                + "`codename1.mainName`. An attribute you do not set is not written at all, "
                + "so the builder's own default applies. Each attribute's `@Hint(def)` "
                + "records what that default is; the `default` clause below it is a neutral "
                + "placeholder with no meaning at runtime.", ""));
        sb.append("@Retention(RetentionPolicy.CLASS)\n");
        sb.append("@Target(ElementType.TYPE)\n");
        sb.append("public @interface ").append(group.annotationSimpleName()).append(" {\n");
        for (int i = 0; i < hints.size(); i++) {
            BuildHints.Hint h = hints.get(i);
            sb.append("\n");
            String text = h.doc();
            if (text == null || text.length() == 0) {
                text = group == HintGroup.IOS_PRIVACY
                        ? "The text iOS shows when the app first asks for "
                          + plistSubject(h.name()) + ". It becomes the `"
                          + h.name().substring("ios.".length())
                          + "` key in `Info.plist`. The App Store rejects an app that "
                          + "touches this resource without one."
                        : "Sets the `" + h.name() + "` build hint.";
            }
            if (h.deprecated() != null) {
                sb.append("    @Deprecated\n");
            }
            sb.append(hintAnnotation(group, h, text));
            sb.append("    ").append(javaType(h)).append(" ").append(h.attr())
              .append("() default ").append(neutralDefault(h)).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /// The `@Hint` carrying what the attribute's signature cannot say.
    ///
    /// Only what differs from the convention: a name the group prefix does not
    /// produce, a separator, a documented default, and the handful of facts the
    /// guide's table shows. Everything javac already knows is left out.
    private static String hintAnnotation(HintGroup group, BuildHints.Hint h, String doc) {
        List<String> members = new ArrayList<String>();
        String prefix = group.keyPrefix() == null ? "" : group.keyPrefix();
        if (!(prefix + h.attr()).equals(h.name())) {
            members.add("name = \"" + esc(h.name()) + "\"");
        }
        String kind = kindOf(h.type());
        if (kind != null) {
            members.add("kind = HintKind." + kind);
        }
        if (h.def() != null && h.def().length() > 0) {
            members.add("def = \"" + esc(h.def()) + "\"");
        }
        if (h.separator() != null) {
            members.add("appendable = true");
            if (h.separator().length() > 0) {
                members.add("separator = \"" + esc(h.separator()) + "\"");
            }
        }
        if (h.platform() != null && !"general".equals(h.platform())) {
            members.add("platform = \"" + esc(h.platform()) + "\"");
        }
        if (h.aliasOf() != null) {
            members.add("aliasOf = \"" + esc(h.aliasOf()) + "\"");
        }
        if (h.deprecated() != null) {
            members.add("deprecated = \"" + esc(h.deprecated()) + "\"");
        }
        if (h.isExternal()) {
            members.add("external = true");
        }
        if (h.isEnterpriseOnly()) {
            members.add("enterpriseOnly = true");
        }
        if (h.link() != null && h.link().length() > 0) {
            members.add("link = \"" + esc(h.link()) + "\"");
        }
        if (doc != null && doc.length() > 0) {
            // Through the same ASCII conversion the javadoc path uses: the doc
            // is a string literal in a source file now, and the Ant javac step
            // rejects an unmappable character wherever it appears.
            members.add("doc = \"" + esc(toAscii(doc)) + "\"");
        }
        if (!h.consumedBy().isEmpty()) {
            StringBuilder by = new StringBuilder("consumedBy = {");
            for (int i = 0; i < h.consumedBy().size(); i++) {
                by.append(i == 0 ? "" : ", ").append("\"").append(esc(h.consumedBy().get(i)))
                  .append("\"");
            }
            members.add(by.append("}").toString());
        }
        if (members.isEmpty()) {
            return "    @Hint\n";
        }
        StringBuilder sb = new StringBuilder("    @Hint(");
        for (int i = 0; i < members.size(); i++) {
            sb.append(i == 0 ? "" : ",\n            ").append(members.get(i));
        }
        return sb.append(")\n").toString();
    }

    /// The `HintKind` naming a string hint's real shape, or null where the
    /// attribute's Java type already says everything.
    private static String kindOf(HintType t) {
        switch (t) {
            case TEXT_BLOCK: return "TEXT_BLOCK";
            case XML: return "XML";
            case PATH: return "PATH";
            case URL: return "URL";
            case VERSION: return "VERSION";
            case SECRET: return "SECRET";
            default: return null;
        }
    }

    /// A default clause with no meaning, because the attribute's default has
    /// none: a hint is written only where the developer set it. The builder's
    /// real default is in `@Hint(def)`, which -- unlike this -- survives being
    /// read back.
    private static String neutralDefault(BuildHints.Hint h) {
        switch (h.type()) {
            case BOOLEAN: return "false";
            case INT: return "0";
            case ENUM: return h.enumName() + "." + enumConstant(h.values().get(0));
            case STRING_LIST: return "{}";
            default: return "\"\"";
        }
    }

    /// Turns ios.NSCameraUsageDescription into "the camera", so the generated
    /// sentence reads naturally rather than repeating the plist key.
    private static String plistSubject(String hintName) {
        String body = hintName.substring("ios.NS".length());
        if (body.endsWith("UsageDescription")) {
            body = body.substring(0, body.length() - "UsageDescription".length());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(body.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(i == 0 ? Character.toLowerCase(c) : c);
        }
        return "the " + sb.toString().toLowerCase();
    }

    private static String groupBlurb(HintGroup g) {
        switch (g) {
            case IOS: return "iOS build hints, checked by the compiler.";
            case ANDROID: return "Android build hints, checked by the compiler.";
            case DESKTOP: return "Desktop build hints, checked by the compiler.";
            case HARDENING: return "App hardening build hints, checked by the compiler.";
            case ON_DEVICE_DEBUG: return "On-device debugging build hints for iOS and Android.";
            case IOS_PRIVACY: return "iOS `Info.plist` privacy usage descriptions. Set the one "
                    + "for every protected resource your app touches: the build server accepts "
                    + "an app without them, and the App Store rejects it.";
            case GENERAL: return "Build hints that are not specific to one platform.";
            default: return g.annotationSimpleName() + " build hints.";
        }
    }

    private static String enumSource(String name, BuildHints.Hint origin) {
        StringBuilder sb = new StringBuilder(LICENSE);
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append(doc("Accepted values of the `" + origin.name() + "` build hint.", ""));
        sb.append("///\n");
        sb.append(doc("Each constant's `@HintValue` carries the string the build actually "
                + "receives, which is not always the constant's own name.", ""));
        sb.append("public enum ").append(name).append(" {\n");
        List<String> values = origin.values();
        List<String> labels = origin.valueLabels();
        for (int i = 0; i < values.size(); i++) {
            String v = values.get(i);
            sb.append(i == 0 ? "" : "\n");
            if (i < labels.size()) {
                sb.append(doc(labels.get(i), "    "));
            }
            sb.append("    @HintValue(\"").append(esc(v)).append("\"");
            if (i < labels.size() && labels.get(i) != null && labels.get(i).length() > 0) {
                sb.append(", label = \"").append(esc(labels.get(i))).append("\"");
            }
            sb.append(")\n");
            sb.append("    ").append(enumConstant(v));
            sb.append(i == values.size() - 1 ? ";\n" : ",\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String packageInfoSource(Map<HintGroup, List<BuildHints.Hint>> byGroup) {
        StringBuilder sb = new StringBuilder(LICENSE);
        sb.append(doc("Build hints expressed as annotations, so the compiler checks them.", ""));
        sb.append("///\n");
        sb.append(doc("A build hint used to be a `codename1.arg.<name>=<value>` line in "
                + "`codenameone_settings.properties`. Nothing validated it, so a misspelled "
                + "name was copied into the build request, never read, and silently dropped: "
                + "the build stayed green and the setting simply did nothing. Written as an "
                + "annotation the same mistake is an unknown symbol, a wrong value type is a "
                + "type error, and a value outside a hint's supported set is an unknown enum "
                + "constant.", ""));
        sb.append("///\n");
        sb.append(doc("Put the annotations on your application's main class:", ""));
        sb.append("///\n");
        sb.append("/// ```java\n");
        sb.append("/// @Ios(newStorageLocation = true, themeMode = IosThemeMode.MODERN)\n");
        sb.append("/// @Android(themeMode = AndroidThemeMode.MODERN)\n");
        sb.append("/// @DesktopBuild(titleBar = DesktopTitleBar.NATIVE)\n");
        sb.append("/// public class MyApplication {\n");
        sb.append("/// }\n");
        sb.append("/// ```\n");
        sb.append("///\n");
        sb.append(doc("These annotations cover the hints most applications set. The rest, and "
                + "the open-ended families such as `android.permission.<NAME>` that an "
                + "annotation cannot express, are still set in "
                + "`codenameone_settings.properties`, which continues to work exactly as "
                + "before. Setting the same hint in both places is a build error.", ""));
        sb.append("///\n");
        sb.append(doc("A project generated recently already runs the goal that turns these into "
                + "build hints. An older one may not: a goal's default phase does not add an "
                + "execution to a project, so the annotations would compile and then be ignored. "
                + "The build refuses rather than shipping without them, and the module that "
                + "compiles the main class needs:", ""));
        sb.append("///\n");
        sb.append("/// ```xml\n");
        sb.append("/// <execution>\n");
        sb.append("///   <id>cn1-process-classes</id>\n");
        sb.append("///   <phase>process-classes</phase>\n");
        sb.append("///   <goals>\n");
        sb.append("///     <goal>process-annotations</goal>\n");
        sb.append("///   </goals>\n");
        sb.append("/// </execution>\n");
        sb.append("/// ```\n");
        sb.append("///\n");
        sb.append(GENERATED_NOTE);
        sb.append("package ").append(PKG).append(";\n");
        return sb.toString();
    }

    private static String bindingSource(Map<HintGroup, List<BuildHints.Hint>> byGroup,
                                        Map<String, BuildHints.Hint> enums) {
        StringBuilder sb = new StringBuilder(LICENSE);
        sb.append("package com.codename1.build.shared;\n\n");
        sb.append("import java.util.Collections;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n");
        sb.append(" * Maps a build hint annotation back to the hint it sets.\n");
        sb.append(" *\n");
        sb.append(" * <p>The annotation processor reads bytecode, where an annotation member is\n");
        sb.append(" * just a name and an enum value is just a constant name. It must not\n");
        sb.append(" * re-derive the hint name or the wire value from those strings: the folding\n");
        sb.append(" * rule would then exist in two places, and a builder silently falls back to\n");
        sb.append(" * its default on a value it does not recognise, so a mismatch would be\n");
        sb.append(" * invisible. This table is generated from the same catalog as the\n");
        sb.append(" * annotations, so the two cannot drift.</p>\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated by BuildHintCodeGenerator. Do not edit by hand.</p>\n");
        sb.append(" */\n");
        sb.append("public final class BuildHintAnnotationBinding {\n\n");
        sb.append("    /** JVM descriptor of an annotation type, by its simple name. */\n");
        sb.append("    private static final Map<String, String> DESCRIPTORS =\n");
        sb.append("            new HashMap<String, String>();\n");
        sb.append("    /** \"<descriptor>#<member>\" to hint name. */\n");
        sb.append("    private static final Map<String, String> HINTS = new HashMap<String, String>();\n");
        sb.append("    /** \"<enumSimpleName>#<CONSTANT>\" to the value the build receives. */\n");
        sb.append("    private static final Map<String, String> WIRE = new HashMap<String, String>();\n\n");
        sb.append("    static {\n");
        for (Map.Entry<HintGroup, List<BuildHints.Hint>> e : byGroup.entrySet()) {
            String simple = e.getKey().annotationSimpleName();
            String desc = "L" + PKG_PATH + "/" + simple + ";";
            sb.append("        DESCRIPTORS.put(\"").append(simple).append("\", \"")
              .append(desc).append("\");\n");
            for (BuildHints.Hint h : e.getValue()) {
                sb.append("        HINTS.put(\"").append(desc).append("#").append(h.attr())
                  .append("\", \"").append(esc(h.name())).append("\");\n");
            }
        }
        sb.append("\n");
        for (Map.Entry<String, BuildHints.Hint> e : enums.entrySet()) {
            for (String v : e.getValue().values()) {
                sb.append("        WIRE.put(\"").append(e.getKey()).append("#")
                  .append(enumConstant(v)).append("\", \"").append(esc(v)).append("\");\n");
            }
        }
        sb.append("    }\n\n");
        sb.append("    private BuildHintAnnotationBinding() {\n    }\n\n");
        sb.append("    /** Every build hint annotation descriptor, in JVM internal form. */\n");
        sb.append("    public static java.util.Collection<String> descriptors() {\n");
        sb.append("        return Collections.unmodifiableCollection(DESCRIPTORS.values());\n    }\n\n");
        sb.append("    /**\n");
        sb.append("     * The hint an annotation member sets.\n");
        sb.append("     *\n");
        sb.append("     * @param descriptor the annotation's JVM descriptor\n");
        sb.append("     * @param member the annotation member name\n");
        sb.append("     * @return the bare hint name, or null when the pair is not a build hint\n");
        sb.append("     */\n");
        sb.append("    public static String hintFor(String descriptor, String member) {\n");
        sb.append("        return HINTS.get(descriptor + \"#\" + member);\n    }\n\n");
        sb.append("    /**\n");
        sb.append("     * The value the build receives for an enum constant.\n");
        sb.append("     *\n");
        sb.append("     * @param enumDescriptorOrName the enum type, as a descriptor or a simple name\n");
        sb.append("     * @param constant the constant name as it appears in the class file\n");
        sb.append("     * @return the wire value, or null when the constant is unknown\n");
        sb.append("     */\n");
        sb.append("    public static String wireValue(String enumDescriptorOrName, String constant) {\n");
        sb.append("        String simple = enumDescriptorOrName;\n");
        sb.append("        int slash = simple.lastIndexOf('/');\n");
        sb.append("        if (slash >= 0) {\n");
        sb.append("            simple = simple.substring(slash + 1);\n        }\n");
        sb.append("        if (simple.endsWith(\";\")) {\n");
        sb.append("            simple = simple.substring(0, simple.length() - 1);\n        }\n");
        sb.append("        return WIRE.get(simple + \"#\" + constant);\n    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * The simulator's Build Hint editor schema for every annotated hint.
     *
     * <p>Emitted as a companion to the hand-written BuildHintSchemaDefaults
     * rather than replacing it: that file carries carefully written labels and
     * group descriptions for fifteen hints, and regenerating it would trade real
     * prose for mechanical text. Its registrations run first and {@code set} does
     * not overwrite, so anything it describes by hand wins and this fills in the
     * rest.</p>
     *
     * <p>Generated as source rather than read from the catalog jar at runtime
     * because Ports/JavaSE is built by Ant as well as Maven, and the Ant build
     * has a hand-maintained classpath that a new jar would have to be added to.</p>
     */
    private static String simulatorSchemaSource(Map<HintGroup, List<BuildHints.Hint>> byGroup) {
        StringBuilder sb = new StringBuilder(LICENSE);
        sb.append("package com.codename1.impl.javase;\n\n");
        sb.append("/**\n");
        sb.append(" * Build Hint editor schema for every hint that has a build hint annotation.\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated from com.codename1.build.shared.BuildHints by\n");
        sb.append(" * BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and re-run\n");
        sb.append(" * scripts/gen-build-hint-annotations.sh.</p>\n");
        sb.append(" *\n");
        sb.append(" * <p>Registered after {@link BuildHintSchemaDefaults} and skipping every hint\n");
        sb.append(" * that class already describes. Precedence cannot be left to the setter:\n");
        sb.append(" * the group name is part of the property key, so registering harden.level\n");
        sb.append(" * under both `hardening` and `Hardening` does not overwrite anything -- it\n");
        sb.append(" * makes a second group, and the editor renders both, giving the user\n");
        sb.append(" * duplicate controls for one setting.</p>\n");
        sb.append(" */\n");
        sb.append("final class BuildHintCatalogDefaults {\n\n");
        sb.append("    private BuildHintCatalogDefaults() {\n    }\n\n");
        sb.append("    static void register() {\n");
        sb.append("        java.util.Set<String> handWritten = BuildHintSchemaDefaults.declaredHints();\n");
        for (Map.Entry<HintGroup, List<BuildHints.Hint>> e : byGroup.entrySet()) {
            String group = e.getKey().annotationSimpleName();
            sb.append("\n        set(\"{{@").append(group).append("}}.label\", ")
              .append(quote(toAscii(groupLabel(e.getKey())))).append(");\n");
            for (BuildHints.Hint h : e.getValue()) {
                String key = "{{#" + group + "#" + h.name() + "}}";
                sb.append("        if (!handWritten.contains(\"").append(esc(h.name()))
                  .append("\")) {\n");
                sb.append("        set(\"").append(key).append(".label\", ")
                  .append(quote(humanize(h.attr()))).append(");\n");
                sb.append("        set(\"").append(key).append(".type\", \"")
                  .append(BuildHints.editorWidget(h.type())).append("\");\n");
                if (h.type() == HintType.ENUM) {
                    StringBuilder values = new StringBuilder();
                    for (String v : h.values()) {
                        if (values.length() > 0) {
                            values.append(',');
                        }
                        values.append(v);
                    }
                    sb.append("        set(\"").append(key).append(".values\", \"")
                      .append(values).append("\");\n");
                }
                if (h.doc() != null && h.doc().length() > 0) {
                    sb.append("        set(\"").append(key).append(".description\", ")
                      .append(quote(toAscii(h.doc()))).append(");\n");
                }
                sb.append("        }\n");
            }
        }
        sb.append("    }\n\n");
        sb.append("    /** Idempotent setter: does not overwrite user or project-level metadata. */\n");
        sb.append("    private static void set(String suffix, String value) {\n");
        sb.append("        String key = \"codename1.arg.\" + suffix;\n");
        sb.append("        if (System.getProperty(key) == null) {\n");
        sb.append("            System.setProperty(key, value);\n        }\n    }\n}\n");
        return sb.toString();
    }

    /**
     * The developer guide's build hint table.
     *
     * <p>The hand-written table it replaces had a Name and a Description column
     * and nothing else, so the Settings tool had to guess each hint's type by
     * string-matching the description prose. Generating it adds the type and the
     * default the builders actually use, and means a hint added to a builder can
     * no longer be missing from the guide.</p>
     */
    /// The annotated hints as catalog entries, for the consumers that cannot run
    /// ASM.
    ///
    /// The Settings editor reads the catalog, and it is a Codename One app: it
    /// has no bytecode reader and no business gaining one. So the annotations
    /// are rendered into the catalog the same way the binding table is -- a
    /// generated VIEW of the source of truth, checked in and drift-gated, not a
    /// second statement of it.
    private static String catalogViewSource(List<BuildHints.Hint> annotated) {
        StringBuilder sb = new StringBuilder(LICENSE);
        sb.append("package com.codename1.build.shared;\n\n");
        sb.append("import java.util.List;\n\n");
        sb.append(doc("The build hints the annotations in "
                + "`com.codename1.annotations.buildhints` expose.", ""));
        sb.append("///\n");
        sb.append(doc("A view, not a source: every fact here is read back out of those "
                + "annotations, which are where it is stated. Edit the annotation and "
                + "re-run scripts/gen-build-hint-annotations.sh.", ""));
        sb.append("final class BuildHintsFromAnnotations {\n\n");
        sb.append("    private BuildHintsFromAnnotations() {\n    }\n\n");
        sb.append("    static void register(List<BuildHints.Hint> h) {\n");
        for (BuildHints.Hint hint : annotated) {
            sb.append(entrySource(hint));
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private static String entrySource(BuildHints.Hint h) {
        StringBuilder sb = new StringBuilder();
        sb.append("        h.add(new BuildHints.Hint(").append(quote(h.name())).append(")\n");
        sb.append("                .annotatedAs(HintGroup.").append(h.group().name())
          .append(", ").append(quote(h.attr())).append(")\n");
        if (h.type() == HintType.ENUM) {
            sb.append("                .values(").append(quote(h.enumName()));
            for (String v : h.values()) {
                sb.append(", ").append(quote(v));
            }
            sb.append(")\n");
            if (!h.valueAliases().isEmpty()) {
                sb.append("                .valueAliases(");
                boolean first = true;
                for (Map.Entry<String, String> e : h.valueAliases().entrySet()) {
                    sb.append(first ? "" : ", ").append(quote(e.getKey())).append(", ")
                      .append(quote(e.getValue()));
                    first = false;
                }
                sb.append(")\n");
            }
            if (!h.valueLabels().isEmpty()) {
                sb.append("                .valueLabels(");
                for (int i = 0; i < h.valueLabels().size(); i++) {
                    sb.append(i == 0 ? "" : ", ").append(quote(h.valueLabels().get(i)));
                }
                sb.append(")\n");
            }
        } else {
            sb.append("                .type(HintType.").append(h.type().name()).append(")\n");
        }
        if (h.def() != null && h.def().length() > 0) {
            sb.append("                .def(").append(quote(h.def())).append(")\n");
        }
        if (h.separator() != null) {
            sb.append("                .separator(").append(quote(h.separator())).append(")\n");
        }
        if (h.aliasOf() != null) {
            sb.append("                .aliasOf(").append(quote(h.aliasOf())).append(")\n");
        }
        if (h.deprecated() != null) {
            sb.append("                .deprecated(").append(quote(h.deprecated())).append(")\n");
        }
        if (h.isExternal()) {
            sb.append("                .external()\n");
        }
        if (h.isEnterpriseOnly()) {
            sb.append("                .enterpriseOnly()\n");
        }
        if (h.link() != null && h.link().length() > 0) {
            sb.append("                .link(").append(quote(h.link())).append(")\n");
        }
        if (!h.consumedBy().isEmpty()) {
            sb.append("                .consumedBy(");
            for (int i = 0; i < h.consumedBy().size(); i++) {
                sb.append(i == 0 ? "" : ", ").append(quote(h.consumedBy().get(i)));
            }
            sb.append(")\n");
        }
        sb.append("                .platform(").append(quote(h.platform())).append(")\n");
        sb.append("                .doc(").append(quote(h.doc())).append("));\n");
        return sb.toString();
    }

    /// The hints the catalog still describes, plus the annotated ones read from
    /// source.
    ///
    /// Deliberately not BuildHints.entries(): that includes the generated view
    /// this same run is about to rewrite, so a stale one would render itself
    /// back into the guide.
    private static List<BuildHints.Hint> everything(List<BuildHints.Hint> annotated) {
        List<BuildHints.Hint> all = new ArrayList<BuildHints.Hint>();
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (!h.isAnnotated()) {
                all.add(h);
            }
        }
        all.addAll(annotated);
        Collections.sort(all, new Comparator<BuildHints.Hint>() {
            public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                return a.name().compareTo(b.name());
            }
        });
        return all;
    }

    private static String asciidocTable(List<BuildHints.Hint> everything) {
        List<BuildHints.Hint> all = new ArrayList<BuildHints.Hint>(everything);
        Collections.sort(all, new Comparator<BuildHints.Hint>() {
            public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                int byPlatform = a.platform().compareTo(b.platform());
                return byPlatform != 0 ? byPlatform : a.name().compareTo(b.name());
            }
        });
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated from com.codename1.build.shared.BuildHints by\n");
        sb.append("// BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and re-run\n");
        sb.append("// scripts/gen-build-hint-annotations.sh.\n");
        sb.append("//\n");
        sb.append("// The Annotation column names the compiler-checked form where one exists;\n");
        sb.append("// those hints can be written on the application's main class instead of in\n");
        sb.append("// codenameone_settings.properties.\n\n");
        sb.append("[cols=\"2,1,1,2,4\"]\n");
        sb.append("|===\n");
        sb.append("|Name |Type |Default |Annotation |Description\n\n");
        for (BuildHints.Hint h : all) {
            // Dynamic families are listed too: their names are patterns rather than
            // keys, but they are real settings a reader needs to find.
            sb.append('|').append(cell(h.name())).append('\n');
            sb.append('|').append(cell(adocType(h))).append('\n');
            // A default is a literal value, not prose. One containing a quote --
            // android.file_paths defaults to an XML fragment -- trips Vale's
            // Microsoft.Quotes rule, which the developer guide enforces as an
            // error. Protect just that line using the mechanism .vale.ini
            // documents for individual false positives.
            if (h.def() != null && h.def().indexOf('"') >= 0) {
                sb.append("// vale-skip: Microsoft.Quotes: this is a literal default value, ")
                  .append("not prose -- the quotes belong to the value.\n");
            }
            sb.append('|').append(h.def() == null || h.def().length() == 0
                    ? "_(none)_" : "`" + cell(h.def()) + "`").append('\n');
            sb.append('|').append(h.isAnnotated()
                    ? "`@" + h.group().annotationSimpleName() + "(" + h.attr() + ")`"
                    : (h.isDynamic() ? "_(properties file only)_" : "_(none)_")).append('\n');
            String doc = h.doc();
            if (doc == null || doc.length() == 0) {
                doc = h.isExternal()
                        ? "Consumed by the build service. Not read by anything in the framework "
                          + "repository, so there is no in-repo reference for it."
                        : "";
            }
            sb.append('|').append(cell(doc)).append("\n\n");
        }
        sb.append("|===\n");
        return sb.toString();
    }

    /**
     * Escapes a value for an AsciiDoc table cell.
     *
     * <p>A bare {@code |} starts a new cell, so a hint whose documentation
     * contains one -- {@code ios.spm.packages} is written
     * {@code identity|url|requirement} -- silently shifts every following column
     * and asciidoctor reports "dropping cells from incomplete row" for the whole
     * table.</p>
     */
    private static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|");
    }

    private static String adocType(BuildHints.Hint h) {
        if (h.type() == HintType.ENUM) {
            StringBuilder sb = new StringBuilder();
            for (String v : h.values()) {
                sb.append(sb.length() == 0 ? "" : ", ").append('`').append(v).append('`');
            }
            return sb.toString();
        }
        if (h.type() == HintType.STRING_LIST) {
            String sep = "\n".equals(h.separator()) ? "newline" : "`" + h.separator() + "`";
            return "list (" + sep + " delimited)";
        }
        return h.type().name().toLowerCase();
    }

    private static String groupLabel(HintGroup g) {
        switch (g) {
            case IOS: return "iOS";
            case ANDROID: return "Android";
            case DESKTOP: return "Desktop";
            case HARDENING: return "App Hardening";
            case ON_DEVICE_DEBUG: return "On-Device Debugging";
            case IOS_PRIVACY: return "iOS Privacy Strings";
            case GENERAL: return "General";
            default: return g.annotationSimpleName();
        }
    }

    /** newStorageLocation -> "New storage location". */
    private static String humanize(String attr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attr.length(); i++) {
            char c = attr.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(attr.charAt(i - 1))) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(i == 0 ? Character.toUpperCase(c) : c);
            }
        }
        return sb.toString();
    }

    /** Java string literal, wrapped so the generated line stays readable. */
    private static String quote(String s) {
        return "\"" + esc(s) + "\"";
    }


    /**
     * Folds text to ASCII.
     *
     * <p>The prose is imported from the developer guide, which uses typographic
     * punctuation. {@code CodenameOne/src} is also compiled by an Ant javac step
     * with ASCII encoding, where a single em dash is
     * {@code error: unmappable character for encoding ASCII} -- a build failure,
     * not a warning. A Unicode escape would not help: javac processes
     * {@code \\uXXXX} before it strips comments, so the character would simply
     * reappear.</p>
     */
    static String toAscii(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\u2014': sb.append("--"); break;      // em dash
                case '\u2013': sb.append('-'); break;       // en dash
                case '\u2018':
                case '\u2019': sb.append('\''); break;      // curly single quotes
                case '\u201c':
                case '\u201d': sb.append('"'); break;       // curly double quotes
                case '\u2026': sb.append("..."); break;     // ellipsis
                case '\u2192': sb.append("->"); break;      // right arrow
                case '\u00d7': sb.append('x'); break;       // multiplication sign
                case '\u00a0': sb.append(' '); break;       // non-breaking space
                default:
                    if (c < 0x80) {
                        sb.append(c);
                        break;
                    }
                    // Refuse rather than drop it. Silently deleting a character
                    // from a hint's documentation is a worse outcome than telling
                    // whoever edited the catalog to add a mapping here.
                    throw new IllegalArgumentException("Build hint documentation contains '"
                            + c + "' (U+" + Integer.toHexString(c).toUpperCase()
                            + "), which has no ASCII equivalent in toAscii(). The Ant javac step "
                            + "compiles CodenameOne/src as ASCII and rejects it as unmappable. "
                            + "Add a mapping, or reword the text.");
            }
        }
        return sb.toString();
    }

    /** Wraps text as /// markdown doc comment lines. */
    private static String doc(String text, String indent) {
        String clean = toAscii(text).replace("@since", "since").replaceAll("\\s+", " ").trim();
        StringBuilder sb = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : clean.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > 76) {
                sb.append(indent).append("/// ").append(line).append("\n");
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            sb.append(indent).append("/// ").append(line).append("\n");
        }
        return sb.toString();
    }

    private static String visible(String sep) {
        if ("\n".equals(sep)) {
            return "\\n";
        }
        return sep;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\t", "\\t").replace("\r", "");
    }

    private static void write(File f, String content) throws IOException {
        if (f.getName().endsWith(".java")) {
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) >= 0x80) {
                    throw new IOException(f.getName() + " would contain the non-ASCII character '"
                            + content.charAt(i) + "' (U+"
                            + Integer.toHexString(content.charAt(i)).toUpperCase()
                            + "), which the Ant javac step rejects as unmappable. Add it to "
                            + "toAscii().");
                }
            }
        }
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }
}
