package com.codenameone.playground;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
            if (!"PARSE_ERROR".equals(errorType) && !"LEXER_ERROR".equals(errorType)) {
                System.out.println(PREFIX + encodeLikePlayground(source));
                return;
            }
            emitError(errorType, message, line, column);
        } catch (Throwable ex) {
            String message = ex.getMessage();
            if (message == null || message.length() == 0) {
                message = ex.getClass().getName();
            }
            emitError("UNEXPECTED_ERROR", message, 1, 1);
        }
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
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

    private static String encodeLikePlayground(String source)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CN1Playground playground = new CN1Playground();
        Method method = CN1Playground.class.getDeclaredMethod("encodeSharedScript", String.class);
        method.setAccessible(true);
        Object encoded = method.invoke(playground, source);
        return encoded == null ? "" : encoded.toString();
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
