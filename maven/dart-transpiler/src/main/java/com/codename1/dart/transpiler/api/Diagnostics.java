package com.codename1.dart.transpiler.api;

import com.codename1.dart.transpiler.ast.Ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable diagnostic collector threaded through the pipeline.
 */
public final class Diagnostics {

    private final List<Diagnostic> all = new ArrayList<Diagnostic>();

    public void error(Ast.Node node, String code, String message) {
        all.add(new Diagnostic(node == null ? "?" : node.file, node == null ? 0 : node.line,
                node == null ? 0 : node.col, Diagnostic.Severity.ERROR, code, message));
    }

    public void error(String file, int line, int col, String code, String message) {
        all.add(new Diagnostic(file, line, col, Diagnostic.Severity.ERROR, code, message));
    }

    public void warn(Ast.Node node, String code, String message) {
        all.add(new Diagnostic(node == null ? "?" : node.file, node == null ? 0 : node.line,
                node == null ? 0 : node.col, Diagnostic.Severity.WARNING, code, message));
    }

    public boolean hasErrors() {
        for (Diagnostic d : all) {
            if (d.severity == Diagnostic.Severity.ERROR) {
                return true;
            }
        }
        return false;
    }

    public List<Diagnostic> asList() {
        return all;
    }
}
