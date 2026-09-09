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
package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Records where every file in the generated project's source directory came from.
 *
 * <p><b>Why this has to exist.</b> The translator drops four completely different
 * kinds of C into one flat directory: the code it generated itself, the ParparVM
 * runtime it copied out of its own jar, the hand-written port natives, and vendored
 * third-party sources such as the bundled SQLite amalgamation. After
 * {@link ByteCodeTranslator#execute} has run they are siblings with nothing to tell
 * them apart -- {@code CN1Vision.m} and {@code com_codename1_ui_Form.m} sit in the
 * same directory and differ only in who wrote them. A compiler warning in the first
 * is ours to fix today; one in the second is a codegen defect; one in the third is
 * somebody else's code we cannot touch. Without a record written at copy time that
 * distinction is simply gone, and a warning report over the build log can only
 * lump them together.</p>
 *
 * <p>The translator is the only component that ever knows, because it is the thing
 * doing the copying, so it writes what it knows to {@value #FILE_NAME} beside the
 * generated project.</p>
 *
 * <p><b>The file must not live in the source directory it describes.</b>
 * {@code handleAppleOutput} lists that directory and puts everything it finds into
 * the Xcode project; {@code getFileType} has no case for {@code .txt}, so an entry
 * would fall through to the resources build phase and the manifest would be copied
 * inside the shipped {@code .app}. It is written to the project root instead --
 * the parent -- which nothing enumerates.</p>
 *
 * <p>Entries are keyed by file name rather than by path on purpose: the name is what
 * survives into the generated project, into the compiler's command line, and into
 * the diagnostics in the build log, which is where this data is consumed. Recording
 * the same name twice is not an error -- a later copy legitimately overwrites an
 * earlier one -- and the last writer wins, matching what the filesystem did.</p>
 */
public class SourceManifest {
    /** Name of the manifest, written to the generated project's root directory. */
    public static final String FILE_NAME = "cn1-source-manifest.txt";

    /**
     * Who wrote a file. Ordered from "ours and generated" to "not ours at all",
     * which is also the order of how much a warning in one of them means.
     */
    public enum Origin {
        /**
         * Emitted by the translator from Java bytecode. A warning here is a defect in
         * an emitter, and fixing one emitter fixes every file it wrote.
         */
        GENERATED("generated"),

        /**
         * The ParparVM runtime, copied verbatim out of the translator's own jar
         * ({@code cn1_globals.m}, {@code nativeMethods.m} and friends). Hand-written
         * and ours.
         */
        RUNTIME("runtime"),

        /**
         * A hand-written native from one of the ports, or from a cn1lib. Ours to fix
         * when it is a port file; the consumer resolves which by looking the name up
         * in the checked-out tree.
         */
        PORT("port"),

        /**
         * Third-party code we bundle but do not maintain, such as the SQLite
         * amalgamation. Reported, never gated.
         */
        VENDORED("vendored");

        private final String token;

        Origin(String token) {
            this.token = token;
        }

        /**
         * How this origin is spelled in the manifest.
         *
         * <p>A literal, not {@code name().toLowerCase()}. Case folding is locale
         * sensitive and Codename One has no {@code java.util.Locale} to ask for the
         * root one, so the fold a device performs depends on who is holding it -- and
         * this is a protocol token another program parses back, which is exactly the
         * case that must never be produced by folding. Writing it out also means the
         * wire format is stated here rather than being an accident of the Java
         * identifier, so renaming the constant cannot silently change the file.</p>
         *
         * @return the lower-case ASCII token, identical on every device
         */
        public String token() {
            return token;
        }
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Insertion-ordered so the manifest reads in the order the build produced it,
     * which makes a diff between two builds meaningful.
     */
    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

    /** One recorded file. */
    public static final class Entry {
        private final String name;
        private final Origin origin;
        private final String source;

        Entry(String name, Origin origin, String source) {
            this.name = name;
            this.origin = origin;
            this.source = source;
        }

        public String getName() {
            return name;
        }

        public Origin getOrigin() {
            return origin;
        }

        /**
         * Where the file came from -- an absolute path for a copied file, a
         * {@code resource:} URL for one unpacked from the translator jar, or the empty
         * string for generated code, which has no prior existence.
         */
        public String getSource() {
            return source;
        }
    }

    /**
     * Records a file the translator generated. {@code name} is the file name as it
     * appears in the project directory.
     */
    public void recordGenerated(String name) {
        record(name, Origin.GENERATED, "");
    }

    /** Records a runtime file unpacked from the translator's own jar. */
    public void recordRuntime(String name, String resourceName) {
        record(name, Origin.RUNTIME, "resource:" + resourceName);
    }

    /** Records a vendored third-party file unpacked from the translator's own jar. */
    public void recordVendored(String name, String resourceName) {
        record(name, Origin.VENDORED, "resource:" + resourceName);
    }

    /**
     * Records a hand-written native copied in from the application's or a port's
     * source tree.
     */
    public void recordPort(String name, File origin) {
        record(name, Origin.PORT, origin == null ? "" : origin.getAbsolutePath());
    }

    private void record(String name, Origin origin, String source) {
        if (name == null || name.isEmpty()) {
            return;
        }
        entries.put(name, new Entry(name, origin, source));
    }

    /**
     * Re-records an entry under a new file name, keeping its origin.
     *
     * <p>For the case where a later stage renames a file the translator already wrote --
     * the clean target copies {@code cn1_class_method_index.m} to {@code .c} and deletes
     * the original, because it compiles C rather than Objective-C. The manifest has to
     * name the file that reaches the compiler, since that is the name a diagnostic will
     * carry; leaving the old name behind would describe a file nothing builds and hide
     * the one that is built.</p>
     *
     * <p>Does nothing when {@code fromName} was never recorded, so a caller does not have
     * to know whether the rename it is reporting actually happened.</p>
     *
     * @param fromName the name the file was recorded under
     * @param toName the name it now has on disk
     */
    public void renameGenerated(String fromName, String toName) {
        Entry existing = entries.remove(fromName);
        if (existing == null) {
            return;
        }
        entries.put(toName, new Entry(toName, existing.origin, existing.source));
    }

    /** Every entry, in the order it was recorded. */
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<Entry>(entries.values()));
    }

    /** The recorded origin of {@code name}, or null when it was never recorded. */
    public Origin originOf(String name) {
        Entry e = entries.get(name);
        return e == null ? null : e.origin;
    }

    public int size() {
        return entries.size();
    }

    /**
     * Writes the manifest to {@link #FILE_NAME} in {@code projectRoot}.
     *
     * <p>Pass the project root, never the source directory: see the note on the
     * class about the manifest otherwise shipping inside the application bundle.</p>
     */
    public void write(File projectRoot) throws IOException {
        File out = new File(projectRoot, FILE_NAME);
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(out.toPath()), UTF8)) {
            w.write("# Provenance of every file in the generated project's source directory.\n");
            w.write("# Written by the ParparVM translator; consumed by\n");
            w.write("# scripts/check-native-warnings.py to decide who owns a compiler warning.\n");
            w.write("#\n");
            w.write("# Format: <file-name>|<origin>|<where-it-came-from>\n");
            w.write("# origin is one of: generated, runtime, port, vendored\n");
            w.write("#\n");
            w.write("# generated has no source: it did not exist before this build.\n");
            w.write("\n");
            for (Entry e : entries.values()) {
                w.write(e.name);
                w.write('|');
                w.write(e.origin.token());
                w.write('|');
                w.write(e.source);
                w.write('\n');
            }
        }
    }
}
