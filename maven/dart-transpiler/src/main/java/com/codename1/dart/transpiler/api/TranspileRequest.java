package com.codename1.dart.transpiler.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Input to {@link DartTranspiler#transpile}.
 */
public final class TranspileRequest {

    public final List<File> sourceRoots = new ArrayList<File>();
    /**
     * Jars/directories scanned for META-INF/dart/*.dart signature stubs
     * (the runtime API as seen from Dart). When none contribute stubs the
     * embedded copy is used.
     */
    public final List<File> stubClasspath = new ArrayList<File>();
    public File outputDir;
    /** Java package for generated sources. */
    public String packageName = "com.codename1.generated.flutter";
    /** Fast-skip digest state file; null disables the check. */
    public File stateFile;

    public TranspileRequest sourceRoot(File root) {
        sourceRoots.add(root);
        return this;
    }

    public TranspileRequest stubClasspathEntry(File jarOrDir) {
        stubClasspath.add(jarOrDir);
        return this;
    }

    public TranspileRequest outputDir(File dir) {
        this.outputDir = dir;
        return this;
    }

    public TranspileRequest packageName(String pkg) {
        this.packageName = pkg;
        return this;
    }

    public TranspileRequest stateFile(File f) {
        this.stateFile = f;
        return this;
    }
}
