package com.codename1.dart.transpiler.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Output of {@link DartTranspiler#transpile}.
 */
public final class TranspileResult {

    private final List<Diagnostic> diagnostics;
    private final List<GeneratedFile> generatedFiles;
    private final boolean upToDate;

    public TranspileResult(List<Diagnostic> diagnostics, List<GeneratedFile> generatedFiles, boolean upToDate) {
        this.diagnostics = diagnostics;
        this.generatedFiles = generatedFiles;
        this.upToDate = upToDate;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public List<GeneratedFile> getGeneratedFiles() {
        return generatedFiles;
    }

    public boolean isUpToDate() {
        return upToDate;
    }

    public boolean hasErrors() {
        for (Diagnostic d : diagnostics) {
            if (d.severity == Diagnostic.Severity.ERROR) {
                return true;
            }
        }
        return false;
    }

    public List<Diagnostic> errors() {
        List<Diagnostic> out = new ArrayList<Diagnostic>();
        for (Diagnostic d : diagnostics) {
            if (d.severity == Diagnostic.Severity.ERROR) {
                out.add(d);
            }
        }
        return out;
    }
}
