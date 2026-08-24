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
package com.codename1.maven.processors;

import com.codename1.build.shared.BuildHintAnnotationBinding;
import com.codename1.build.shared.BuildHints;
import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/// Turns the `com.codename1.annotations.buildhints` annotations into the
/// `codename1.arg.*` key/value pairs the builders already consume.
///
/// A build hint used to be a properties line that nothing checked, so a
/// misspelled name reached the build request, was never read, and was silently
/// dropped -- a green build with the setting simply not applied. Written as an
/// annotation the compiler catches the same mistake, and this processor is what
/// turns the checked form back into the wire form.
///
/// The result is written to `META-INF/codenameone/build-hints.properties` in
/// `target/classes`, which puts it both on the simulator's classpath and inside
/// the jar uploaded to the build server.
public class BuildHintAnnotationProcessor extends AbstractAnnotationProcessor {

    /// Where the emitted hints land. Read by `CN1BuildMojo` before it writes the
    /// build request, and by `Simulator` on startup.
    public static final String MANIFEST_RESOURCE = "META-INF/codenameone/build-hints.properties";

    /// Records which annotation attribute supplied each hint, so a later stage
    /// -- the conflict message, the simulator's hint editor -- can name it.
    private static final String ORIGIN_PREFIX = "cn1.buildHints.origin.";

    /// Other names for the same setting, for a consumer that cannot reach the
    /// catalog to resolve an alias itself.
    private static final String ALIAS_PREFIX = "cn1.buildHints.alias.";

    /// Stamps the emitted file with the main class it came from, so a stale or
    /// foreign copy on the classpath can be recognised rather than merged.
    private static final String MAIN_CLASS_KEY = "cn1.buildHints.mainClass";

    /// Digest of the annotations this file was generated from.
    ///
    /// The main-class stamp only says *which* class produced it, which is the
    /// same class an out-of-date copy names. Nothing removes `target/classes`
    /// between builds, so a project that ran this processor once and then stopped
    /// -- the goal unbound, skipped, or bound to a phase that no longer runs --
    /// keeps a manifest that looks entirely valid while the annotations beside it
    /// have moved on. Recording what it was built from lets the consumer compare
    /// it against the class file actually on the classpath and refuse instead of
    /// shipping last week's configuration.
    public static final String SOURCE_DIGEST_KEY = "cn1.buildHints.sourceDigest";

    /// hint name to value, sorted so the emitted bytes are stable.
    private final Map<String, String> hints = new TreeMap<String, String>();
    /// hint name to "@Ios(pods)".
    private final Map<String, String> origins = new TreeMap<String, String>();
    /// Classes carrying a build hint annotation, in discovery order.
    private final List<AnnotatedClass> annotated = new ArrayList<AnnotatedClass>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return new LinkedHashSet<String>(BuildHintAnnotationBinding.descriptors());
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        hints.clear();
        origins.clear();
        annotated.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        Set<String> descriptors = getAnnotationDescriptors();

        // @Target(TYPE) already rejects a method or field placement at compile
        // time, but @Target is a front-end check and this reads bytecode: a
        // class produced another way could still carry one, and silently
        // ignoring it would be the exact failure this feature removes.
        for (String d : cls.getAllAnnotationDescriptors()) {
            if (descriptors.contains(d) && !cls.getClassAnnotations().containsKey(d)) {
                ctx.error(cls, "@" + simpleName(d) + " is a build hint annotation and belongs on "
                        + "the class itself, not on one of its members.");
            }
        }

        boolean carriesAny = false;
        for (Map.Entry<String, AnnotationValues> e : cls.getClassAnnotations().entrySet()) {
            if (descriptors.contains(e.getKey())) {
                carriesAny = true;
            }
        }
        if (!carriesAny) {
            return;
        }
        // An output directory keeps class files whose source is gone. Rename the
        // main class, update codename1.mainName and skip the clean, and the old
        // annotated .class is still sitting there -- so every incremental build
        // failed with a placement error naming a class the developer had already
        // deleted, and the orphan's hints were merged in besides.
        //
        // Only a class that is NOT the main one is dropped this way. The main
        // class is processed whatever its source layout, because failing to find
        // its source would otherwise mean silently applying none of its hints,
        // which is worse than any placement message.
        if (!isMainClass(cls, ctx) && !hasBackingSource(cls, ctx)) {
            ctx.getLog().debug("cn1: ignoring " + cls.getBinaryName()
                    + " -- annotated, but no source for it; stale output from an earlier build");
            return;
        }
        annotated.add(cls);

        for (Map.Entry<String, AnnotationValues> e : cls.getClassAnnotations().entrySet()) {
            String descriptor = e.getKey();
            if (!descriptors.contains(descriptor)) {
                continue;
            }
            AnnotationValues values = e.getValue();
            // Only what the developer actually wrote: javac omits a member left
            // at its default from the class file, and that absence is how an
            // unset attribute is distinguished from one set to the default
            // value. Reading through a getXxxOrDefault here would write a hint
            // for every attribute of every annotation used.
            for (Map.Entry<String, Object> member : values.all().entrySet()) {
                String hint = BuildHintAnnotationBinding.hintFor(descriptor, member.getKey());
                if (hint == null) {
                    ctx.error(cls, "@" + simpleName(descriptor) + "(" + member.getKey()
                            + ") is not a known build hint. The catalog and the annotation "
                            + "have drifted; regenerate with "
                            + "scripts/gen-build-hint-annotations.sh.");
                    continue;
                }
                String value = wireValue(cls, descriptor, member.getKey(), member.getValue(),
                        hint, ctx);
                if (value == null) {
                    continue;
                }
                String origin = "@" + simpleName(descriptor) + "(" + member.getKey() + ")";
                String previous = hints.put(hint, value);
                if (previous != null && !previous.equals(value)) {
                    ctx.error(cls, "Build hint " + hint + " is set twice with different values: "
                            + origins.get(hint) + " and " + origin + ".");
                }
                origins.put(hint, origin);
            }
        }
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (annotated.isEmpty()) {
            // The last annotation was removed. The Mojo only writes emitted
            // resources, it never deletes ones a processor stopped emitting, so
            // without this yesterday's hints would stay in target/classes and
            // ship inside the jar.
            deleteGenerated(ctx);
            return;
        }
        checkPlacement(ctx);
        checkConflicts(ctx);
        if (ctx.hasErrors()) {
            return;
        }
        ctx.emitResource(MANIFEST_RESOURCE, serialize(ctx));
        ctx.getLog().info("cn1: " + hints.size() + " build hint(s) from annotations on "
                + annotated.get(0).getBinaryName());
    }

    private static boolean isMainClass(AnnotatedClass cls, ProcessorContext ctx) {
        String main = ctx.getMainClassBinaryName();
        return main != null && main.equals(cls.getBinaryName());
    }

    /// Whether a source file for `cls` still exists under the project.
    ///
    /// Answered "yes" whenever the question cannot actually be put: no project
    /// directory, or no source tree under it to search. Absence of a source tree
    /// is not evidence that a class is orphaned -- it means this is a layout the
    /// lookup does not know, and dropping annotations on that basis would apply
    /// none of a project's hints while reporting nothing.
    private static boolean hasBackingSource(AnnotatedClass cls, ProcessorContext ctx) {
        File dir = ctx.getProjectDir();
        if (dir == null) {
            return true;
        }
        // The outermost class owns the file: Outer$Inner lives in Outer.java.
        String binary = cls.getBinaryName();
        int nested = binary.indexOf('$');
        if (nested >= 0) {
            binary = binary.substring(0, nested);
        }
        String rel = binary.replace('.', File.separatorChar);
        String[] roots = {"src" + File.separator + "main" + File.separator + "java",
                          "src" + File.separator + "main" + File.separator + "kotlin",
                          "src"};
        String[] extensions = {".java", ".kt"};
        boolean sawARoot = false;
        for (String root : roots) {
            if (!new File(dir, root).isDirectory()) {
                continue;
            }
            sawARoot = true;
            for (String ext : extensions) {
                if (new File(dir, root + File.separator + rel + ext).isFile()) {
                    return true;
                }
            }
        }
        return !sawARoot;
    }

    /// Build hints configure the application, so they belong on the class the    /// Build hints configure the application, so they belong on the class the
    /// project already names as its entry point.
    ///
    /// Accepting them anywhere would mean two classes could set the same hint
    /// and the winner would depend on the order `File.listFiles` happened to
    /// return -- and it would scatter the effective build configuration across
    /// the source tree, which is the problem the properties file already had.
    private void checkPlacement(ProcessorContext ctx) {
        String main = ctx.getMainClassBinaryName();
        if (main == null) {
            ctx.error(annotated.get(0),
                    "Build hint annotations are only supported in a Codename One application, "
                            + "and this module declares no codename1.mainName.");
            return;
        }
        for (AnnotatedClass cls : annotated) {
            if (!main.equals(cls.getBinaryName())) {
                ctx.error(cls, "Build hint annotations belong on the application's main class, "
                        + main + ", but this one carries them. Move them there, or set the hint "
                        + "in codenameone_settings.properties.");
            }
        }
    }

    /// A hint has one source of truth. Setting it in both places means the two
    /// can disagree, and nothing would say which won.
    private void checkConflicts(ProcessorContext ctx) {
        Properties settings = ctx.getProjectSettings();
        if (settings == null) {
            return;
        }
        Map<String, Integer> lines = propertyLines(ctx);
        for (Map.Entry<String, String> e : hints.entrySet()) {
            Set<String> names = spellingsOf(e.getKey());
            for (String name : names) {
                String key = BuildHints.ARG_PREFIX + name;
                if (settings.getProperty(key) == null) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(key).append(" is declared twice.\n");
                sb.append("    annotation : ").append(origins.get(e.getKey()))
                  .append(" on ").append(annotated.get(0).getBinaryName()).append('\n');
                sb.append("    properties : ");
                File f = settingsFile(ctx);
                sb.append(f == null ? "codenameone_settings.properties" : f.getPath());
                Integer line = lines.get(key);
                if (line != null) {
                    sb.append(':').append(line);
                }
                sb.append('\n');
                sb.append("                 ").append(key).append('=')
                  .append(settings.getProperty(key)).append('\n');
                sb.append("    A build hint has one source of truth. Delete the properties line "
                        + "and keep the annotation, or delete the annotation attribute and keep "
                        + "the line. (-D").append(key).append("=... overrides either and is not "
                        + "a conflict.)");
                ctx.error(annotated.get(0), sb.toString());
            }
        }
    }

    private File settingsFile(ProcessorContext ctx) {
        File dir = ctx.getProjectDir();
        if (dir == null) {
            return null;
        }
        File f = new File(dir, "codenameone_settings.properties");
        return f.exists() ? f : null;
    }

    /// Best-effort key to line number, so the conflict message can point at the
    /// offending line. Properties escaping means an exotic key may not match;
    /// the message then names the file only rather than guessing.
    private Map<String, Integer> propertyLines(ProcessorContext ctx) {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        File f = settingsFile(ctx);
        if (f == null) {
            return out;
        }
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
            String line;
            int n = 0;
            while ((line = r.readLine()) != null) {
                n++;
                String t = line.trim();
                if (t.length() == 0 || t.charAt(0) == '#' || t.charAt(0) == '!') {
                    continue;
                }
                int eq = t.indexOf('=');
                int colon = t.indexOf(':');
                int split = eq < 0 ? colon : (colon < 0 ? eq : Math.min(eq, colon));
                if (split <= 0) {
                    continue;
                }
                String key = t.substring(0, split).trim();
                if (!out.containsKey(key)) {
                    out.put(key, Integer.valueOf(n));
                }
            }
        } catch (IOException ex) {
            ctx.getLog().debug("cn1: could not read " + f + " for line numbers: " + ex.getMessage());
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                    // read-only stream; nothing useful to do
                }
            }
        }
        return out;
    }

    /// Converts one annotation member value to the string the build receives.
    ///
    /// Returns null when the value could not be converted, having reported it.
    private String wireValue(AnnotatedClass cls, String descriptor, String member, Object raw,
                             String hint, ProcessorContext ctx) {
        if (raw instanceof Boolean || raw instanceof Number || raw instanceof Character) {
            return String.valueOf(raw);
        }
        if (raw instanceof String) {
            return (String) raw;
        }
        // ASM reports an enum constant as { descriptor, CONSTANT_NAME }. The
        // constant name is not the value the builder compares against, and a
        // builder silently falls back to its default on a value it does not
        // recognise, so guessing here would fail invisibly.
        if (raw instanceof String[]) {
            String[] pair = (String[]) raw;
            if (pair.length == 2) {
                String wire = BuildHintAnnotationBinding.wireValue(pair[0], pair[1]);
                if (wire == null) {
                    ctx.error(cls, "@" + simpleName(descriptor) + "(" + member + ") uses the "
                            + "constant " + pair[1] + ", which the build hint catalog does not "
                            + "map to a value. Regenerate with "
                            + "scripts/gen-build-hint-annotations.sh.");
                    return null;
                }
                return wire;
            }
        }
        if (raw instanceof List) {
            String separator = BuildHints.separatorFor(hint);
            if (separator.length() == 0) {
                ctx.error(cls, "@" + simpleName(descriptor) + "(" + member + ") is a list but the "
                        + "catalog gives " + hint + " no separator, so its values would run "
                        + "together.");
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object item : (List<?>) raw) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                String itemValue = wireValue(cls, descriptor, member, item, hint, ctx);
                if (itemValue == null) {
                    return null;
                }
                sb.append(itemValue);
            }
            return sb.toString();
        }
        ctx.error(cls, "@" + simpleName(descriptor) + "(" + member + ") has a value this "
                + "processor cannot convert: " + raw);
        return null;
    }

    /// Serializes deterministically.
    ///
    /// Not `Properties.store`: it writes a timestamp comment, so the bytes would
    /// differ on every build. That churns the resource in every incremental
    /// build and defeats the staged-jar staleness comparison in `CN1BuildMojo`.
    /// Every name that denotes the same setting as `hint`, itself included.
    ///
    /// An alias and its target are one setting -- the builder reads
    /// `android.captureRecord` and then lets `and.captureRecord` override it --
    /// so declaring either in the properties file collides with the annotation.
    static Set<String> spellingsOf(String hint) {
        Set<String> names = new LinkedHashSet<String>();
        names.add(hint);
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (hint.equals(h.aliasOf()) || hint.equals(BuildHints.canonicalName(h.name()))) {
                names.add(h.name());
            }
        }
        return names;
    }

    /// A stable fingerprint of every build hint annotation on `cls`.    /// A stable fingerprint of every build hint annotation on `cls`.
    ///
    /// Taken over the raw annotation members rather than over the hints they
    /// convert into, so it changes for anything the developer can change: a
    /// different value, an added or removed attribute, a whole annotation
    /// gained or lost. Two builds of the same source produce the same string;
    /// there is no timestamp or path in it.
    public static String sourceDigest(AnnotatedClass cls) throws ProcessingException {
        StringBuilder sb = new StringBuilder();
        Set<String> known = new HashSet<String>(BuildHintAnnotationBinding.descriptors());
        // Sorted, because the class file's annotation order is the source's and a
        // reordering is not a change.
        for (String descriptor : new TreeMap<String, AnnotationValues>(
                cls.getClassAnnotations()).keySet()) {
            if (!known.contains(descriptor)) {
                continue;
            }
            sb.append(descriptor).append('{');
            AnnotationValues values = cls.getClassAnnotation(descriptor);
            for (Map.Entry<String, Object> e
                    : new TreeMap<String, Object>(values.all()).entrySet()) {
                sb.append(e.getKey()).append('=');
                renderForDigest(e.getValue(), sb);
                sb.append(';');
            }
            sb.append('}');
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16));
                hex.append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new ProcessingException("Could not fingerprint the build hint annotations", ex);
        }
    }

    /// The type is part of the rendering, so an int 1 and the string "1" -- which
    /// print alike but are different annotations -- do not fingerprint alike.
    private static void renderForDigest(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String[]) {
            // How ASM delivers an enum member: {descriptor, CONSTANT_NAME}.
            String[] e = (String[]) value;
            sb.append("enum:").append(e.length > 0 ? e[0] : "")
              .append('.').append(e.length > 1 ? e[1] : "");
        } else if (value instanceof List) {
            sb.append('[');
            for (Object item : (List<?>) value) {
                renderForDigest(item, sb);
                sb.append(',');
            }
            sb.append(']');
        } else if (value instanceof AnnotationValues) {
            AnnotationValues nested = (AnnotationValues) value;
            sb.append(nested.getDescriptor()).append('{');
            for (Map.Entry<String, Object> e
                    : new TreeMap<String, Object>(nested.all()).entrySet()) {
                sb.append(e.getKey()).append('=');
                renderForDigest(e.getValue(), sb);
                sb.append(';');
            }
            sb.append('}');
        } else {
            sb.append(value.getClass().getName()).append(':').append(value);
        }
    }

    private byte[] serialize(ProcessorContext ctx) throws ProcessingException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated from build hint annotations by the Codename One Maven plugin.\n");
        sb.append("# Edit the annotations on the main class, not this file.\n");
        String main = ctx.getMainClassBinaryName();
        if (main != null) {
            sb.append(MAIN_CLASS_KEY).append('=').append(escape(main)).append('\n');
        }
        sb.append(SOURCE_DIGEST_KEY).append('=')
          .append(sourceDigest(annotated.get(0))).append('\n');
        for (Map.Entry<String, String> e : hints.entrySet()) {
            sb.append(escape(BuildHints.ARG_PREFIX + e.getKey())).append('=')
              .append(escape(e.getValue())).append('\n');
        }
        for (Map.Entry<String, String> e : origins.entrySet()) {
            sb.append(escape(ORIGIN_PREFIX + e.getKey())).append('=')
              .append(escape(e.getValue())).append('\n');
        }
        // The other spellings of each hint, for a consumer that has to collapse
        // them and cannot reach the catalog -- the simulator, which lives in the
        // JavaSE port. Written only where there is more than one, so the common
        // hint costs nothing.
        for (String hint : hints.keySet()) {
            Set<String> spellings = spellingsOf(hint);
            spellings.remove(hint);
            if (spellings.isEmpty()) {
                continue;
            }
            StringBuilder joined = new StringBuilder();
            for (String name : spellings) {
                if (joined.length() > 0) {
                    joined.append(',');
                }
                joined.append(name);
            }
            sb.append(escape(ALIAS_PREFIX + hint)).append('=')
              .append(escape(joined.toString())).append('\n');
        }
        try {
            return sb.toString().getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            throw new ProcessingException("ISO-8859-1 is unavailable", ex);
        }
    }

    /// Applies the escaping `java.util.Properties` expects, so a value holding a
    /// newline -- `gradleDependencies` legitimately does -- survives the round
    /// trip.
    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '=': sb.append("\\="); break;
                case ':': sb.append("\\:"); break;
                case '#': sb.append("\\#"); break;
                case '!': sb.append("\\!"); break;
                case ' ': sb.append(i == 0 ? "\\ " : " "); break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append(String.format("\\u%04x", Integer.valueOf(c)));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private void deleteGenerated(ProcessorContext ctx) {
        File f = new File(ctx.getOutputClassDir(), MANIFEST_RESOURCE);
        if (f.exists() && !f.delete()) {
            ctx.getLog().warn("cn1: could not remove stale " + f + "; it would be packaged "
                    + "with hints the project no longer declares");
        }
    }

    private static String simpleName(String descriptor) {
        String s = descriptor;
        if (s.startsWith("L") && s.endsWith(";")) {
            s = s.substring(1, s.length() - 1);
        }
        int slash = s.lastIndexOf('/');
        return slash >= 0 ? s.substring(slash + 1) : s;
    }
}
