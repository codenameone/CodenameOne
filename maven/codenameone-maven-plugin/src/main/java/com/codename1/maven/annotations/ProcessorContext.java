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
package com.codename1.maven.annotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

/// Shared state passed to every `AnnotationProcessor`.
///
/// Exposes:
/// - A read-only **class index** of every non-synthetic class found in the
///   project's compiled output, keyed by JVM internal name. Processors use it
///   to traverse superclass chains, check interface implementations, etc.
/// - The **output class directory** so emitted bytecode lands in the same
///   tree the rest of the build references.
/// - An **error sink** (`#error`) processors call when a class fails validation.
///   Errors accumulate rather than throwing immediately, so a single run can
///   surface every offending class.
/// - A **stub source directory** in `target/generated-sources/cn1-annotations`
///   used by the GENERATE_SOURCES Mojo; the PROCESS_CLASSES path doesn't write
///   to it but the directory may exist either way.
public final class ProcessorContext {

    private final File outputClassDir;
    private final File stubSourceDir;
    private final Map<String, AnnotatedClass> classIndex;
    private final Log log;
    private final List<ProcessingError> errors = new ArrayList<ProcessingError>();
    private final Map<String, byte[]> emittedClasses = new LinkedHashMap<String, byte[]>();
    private final Map<String, String> emittedStubSources = new LinkedHashMap<String, String>();

    public ProcessorContext(File outputClassDir, File stubSourceDir,
                             Map<String, AnnotatedClass> classIndex, Log log) {
        this.outputClassDir = outputClassDir;
        this.stubSourceDir = stubSourceDir;
        this.classIndex = classIndex == null
                ? Collections.<String, AnnotatedClass>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, AnnotatedClass>(classIndex));
        this.log = log;
    }

    /// `target/classes` for the project, or the equivalent output directory.
    public File getOutputClassDir() { return outputClassDir; }

    /// `target/generated-sources/cn1-annotations` (or the configured stub dir).
    /// Always present even during PROCESS_CLASSES so processors can probe for
    /// previously generated stubs.
    public File getStubSourceDir() { return stubSourceDir; }

    /// All non-synthetic classes from `target/classes`, keyed by internal name.
    public Map<String, AnnotatedClass> getClassIndex() { return classIndex; }

    /// Looks up a class by internal name (`com/example/Foo`). Returns null when
    /// the class is not in the project's compiled output (e.g. it's a JDK or
    /// dependency class).
    public AnnotatedClass lookup(String internalName) { return classIndex.get(internalName); }

    public Log getLog() { return log; }

    /// Reports a validation error attributed to `source`. Continues processing.
    public void error(AnnotatedClass source, String message) {
        errors.add(new ProcessingError(source, message));
    }

    /// Reports a global (non-class-bound) error.
    public void error(String message) {
        errors.add(new ProcessingError(null, message));
    }

    /// Queues a generated class for write-out. Path comes from the internal
    /// name. Subsequent calls with the same name overwrite — useful for
    /// processors that update existing stubs.
    public void emitClass(String internalName, byte[] bytecode) {
        emittedClasses.put(internalName, bytecode);
    }

    /// Queues a generated Java source for write-out under the stub source
    /// directory. Used by GENERATE_SOURCES phase. `internalName` follows the
    /// same `com/example/Foo` convention as #emitClass.
    public void emitStubSource(String internalName, String javaSource) {
        emittedStubSources.put(internalName, javaSource);
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public List<ProcessingError> getErrors() { return Collections.unmodifiableList(errors); }
    public Map<String, byte[]> getEmittedClasses() { return Collections.unmodifiableMap(emittedClasses); }
    public Map<String, String> getEmittedStubSources() { return Collections.unmodifiableMap(emittedStubSources); }

    /// One validation error.
    public static final class ProcessingError {
        private final AnnotatedClass source;
        private final String message;

        ProcessingError(AnnotatedClass source, String message) {
            this.source = source;
            this.message = message;
        }

        public AnnotatedClass getSource() { return source; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            if (source == null) return message;
            return source.getBinaryName() + ": " + message;
        }
    }
}
