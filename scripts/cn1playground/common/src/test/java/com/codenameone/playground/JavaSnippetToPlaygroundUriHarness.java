package com.codenameone.playground;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JavaSnippetToPlaygroundUriHarness {
    private static final String PREFIX = "/playground/?code=";

    private JavaSnippetToPlaygroundUriHarness() {
    }

    public static void main(String[] args) {
        try {
            String source = loadSource(args);
            if (source == null) {
                emitError("UNEXPECTED_ERROR", "No snippet source provided", 1, 1);
                return;
            }
            currentSource = source;

            // Best-effort: populate Display.impl so BeanShell can report undefined
            // identifiers (e.g. "icon" in "setIcon(icon)") instead of NPE-ing inside
            // a CN1 constructor. initImpl will throw HeadlessException in CI, but
            // Display.impl is assigned before that point, which is enough for name
            // resolution. If no port is on the classpath we silently fall back to
            // parse-only validation. We mute stdout during init because JavaSEPort
            // prints "Retina Scale: ..." which would corrupt the harness output.
            java.io.PrintStream savedOut = System.out;
            try {
                System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
                    public void write(int b) {
                    }
                }));
                try {
                    com.codename1.ui.Display.init(null);
                } catch (Throwable ignored) {
                }
            } finally {
                System.setOut(savedOut);
            }

            PlaygroundContext context = new PlaygroundContext(null, null, null, new PlaygroundContext.Logger() {
                public void log(String message) {
                }
            });

            PlaygroundRunner runner = new PlaygroundRunner();
            PlaygroundRunner.RunResult result = runner.run(source, context);
            if (result.getComponent() != null) {
                System.out.println(PREFIX + encodeLikePlayground(source));
                return;
            }

            PlaygroundRunner.Diagnostic diagnostic = result.getDiagnostics().isEmpty() ? null : result.getDiagnostics().get(0);
            int line = diagnostic == null ? 1 : Math.max(1, diagnostic.line);
            int column = diagnostic == null ? 1 : Math.max(1, diagnostic.column);
            String message = diagnostic == null ? "Script execution failed" : diagnostic.message;
            String errorType = classifyErrorType(message);
            // Treat undefined-identifier eval errors as failures. CN1 runtime
            // failures under the headless harness (NPE because Display.impl is
            // null, HeadlessException, etc.) are not the snippet's fault and
            // pass; an "Undefined argument" / "Typed variable declaration" eval
            // error is a real symbol problem and fails.
            if ("EVAL_ERROR".equals(errorType) && isUndefinedSymbolError(message)) {
                emitError(errorType, message, line, column);
                return;
            }
            if (!"PARSE_ERROR".equals(errorType) && !"LEXER_ERROR".equals(errorType)) {
                System.out.println(PREFIX + encodeLikePlayground(source));
                return;
            }
            emitError(errorType, message, line, column);
        } catch (Throwable ex) {
            // BeanShell parse/lex errors arrive as exceptions and are caught by
            // PlaygroundRunner; anything reaching this catch is a runtime/init
            // failure that did NOT trip the parser. CN1 classes have static
            // initializers that touch Display.impl and blow up under the
            // headless harness, but those snippets are still valid playground
            // input - emit the URI and let the live playground decide.
            if (isHeadlessRuntimeFailure(ex)) {
                try {
                    System.out.println(PREFIX + encodeLikePlayground(currentSource));
                    return;
                } catch (Throwable ignored) {
                    // fall through to error emission
                }
            }
            String message = ex.getMessage();
            if (message == null || message.length() == 0) {
                message = ex.getClass().getName();
            }
            emitError("UNEXPECTED_ERROR", message, 1, 1);
        }
    }

    /// Heuristic: any Error subclass (or a wrapper carrying one as its cause)
    /// is treated as a non-parse failure. ParseException/TokenMgrException are
    /// already handled inside PlaygroundRunner.run, so anything reaching the
    /// outer catch is by definition a runtime issue.
    private static boolean isHeadlessRuntimeFailure(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof Error) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static String currentSource = "";

    /// BeanShell phrases unresolved identifiers a few different ways depending
    /// on context (argument position, lhs of assignment, method call target).
    /// We deliberately limit the match to "Undefined argument/variable" - those
    /// fire for identifiers like the unbound 'icon' in SpanLabel's old sample.
    /// "Class X not found in namespace" is intentionally ignored here: that
    /// signals a missing import (e.g. Style, ConnectionRequest), which is the
    /// snippet's responsibility to declare at the call site, and not a bug to
    /// flag in the JavaDoc source.
    private static boolean isUndefinedSymbolError(String message) {
        if (message == null) {
            return false;
        }
        return message.indexOf("Undefined argument") >= 0
                || message.indexOf("Undefined variable") >= 0;
    }

    private static String loadSource(String[] args) throws IOException {
        if (args.length == 0) {
            return slurp(System.in);
        }
        if (args.length == 2 && "--file".equals(args[0])) {
            InputStream input = new FileInputStream(args[1]);
            try {
                return slurp(input);
            } finally {
                input.close();
            }
        }
        throw new IOException("Unsupported arguments. Expected --file <path> or stdin.");
    }

    private static String slurp(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (!first) {
                out.append('\n');
            }
            out.append(line);
            first = false;
        }
        return out.toString();
    }

    // Mirrors CN1Playground.encodeSharedScript: URL-safe Base64 without padding.
    // Implemented with java.util.Base64 so the headless harness does not require
    // an initialized CN1 Display (CN1's Base64 transitively touches Display.impl).
    private static String encodeLikePlayground(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(source.getBytes(StandardCharsets.UTF_8));
    }

    private static String classifyErrorType(String message) {
        if (message == null) {
            return "UNEXPECTED_ERROR";
        }
        if (message.startsWith("Parse error:")) {
            return "PARSE_ERROR";
        }
        if (message.startsWith("Lexer error:")) {
            return "LEXER_ERROR";
        }
        if (message.startsWith("Unexpected error:")) {
            return "UNEXPECTED_ERROR";
        }
        if (message.indexOf("Lifecycle script defines init(Object) but is missing start().") >= 0
                || message.indexOf("Lifecycle start() did not show a Form and did not return a Component.") >= 0) {
            return "LIFECYCLE_CONTRACT_ERROR";
        }
        if (message.indexOf("Script did not produce a previewable Component.") >= 0
                || message.indexOf("instead of a previewable Component.") >= 0
                || message.indexOf("Script must return a com.codename1.ui.Component") >= 0) {
            return "NO_PREVIEWABLE_COMPONENT";
        }
        return "EVAL_ERROR";
    }

    private static void emitError(String errorType, String message, int line, int column) {
        System.out.println("{\"ok\":false,\"errorType\":\"" + jsonEscape(errorType)
                + "\",\"message\":\"" + jsonEscape(message)
                + "\",\"line\":" + Math.max(1, line)
                + ",\"column\":" + Math.max(1, column) + "}");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (ch < 32) {
                        out.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int j = hex.length(); j < 4; j++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(ch);
                    }
            }
        }
        return out.toString();
    }
}
