package com.codename1.dart.transpiler.api;

/**
 * A transpiler diagnostic carrying the Dart source position.
 */
public final class Diagnostic {

    public enum Severity {
        ERROR, WARNING, INFO
    }

    public final String file;
    public final int line;
    public final int col;
    public final Severity severity;
    public final String code;      // e.g. "E0101"
    public final String message;

    public Diagnostic(String file, int line, int col, Severity severity, String code, String message) {
        this.file = file;
        this.line = line;
        this.col = col;
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return file + ":[" + line + "," + col + "] " + message + " (dart2java:" + code + ")";
    }
}
