package com.codenameone.playground;

import com.codename1.ui.Component;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.ParseException;
import bsh.Primitive;
import bsh.TargetError;
import bsh.TokenMgrException;
import bsh.UtilEvalError;
import bsh.cn1.CN1LambdaSupport;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.regex.RE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PlaygroundRunner {
    static final class Diagnostic {
        final int line;
        final int column;
        final int endLine;
        final int endColumn;
        final String message;
        final String severity;

        Diagnostic(int line, int column, int endLine, int endColumn, String message, String severity) {
            this.line = line;
            this.column = column;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.message = message;
            this.severity = severity;
        }
    }

    static final class InlineMessage {
        final int line;
        final String text;
        final String kind;

        InlineMessage(int line, String text, String kind) {
            this.line = line;
            this.text = text;
            this.kind = kind;
        }
    }

    static final class RunResult {
        private final Component component;
        private final List<Diagnostic> diagnostics;
        private final List<InlineMessage> messages;

        RunResult(Component component, List<Diagnostic> diagnostics, List<InlineMessage> messages) {
            this.component = component;
            this.diagnostics = diagnostics;
            this.messages = messages;
        }

        Component getComponent() {
            return component;
        }

        List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        List<InlineMessage> getMessages() {
            return messages;
        }
    }

    RunResult run(String script, PlaygroundContext context) {
        List<InlineMessage> inlineMessages = new ArrayList<InlineMessage>();
        try {
            Interpreter interpreter = new Interpreter();
            bindGlobals(interpreter, context);
            PlaygroundContext.pushCurrent(context);
            CN1LambdaSupport.pushInterpreter(interpreter);
            try {
                ScriptPlan plan = adaptScript(script);
                for (int i = 0; i < plan.typeDeclarations.size(); i++) {
                    interpreter.eval(plan.typeDeclarations.get(i));
                }
                Object result = interpreter.eval(plan.executableScript);
                Component component = resolveComponent(interpreter, result, context);
                inlineMessages.add(new InlineMessage(0, "Preview updated.", "success"));
                return new RunResult(component, Collections.<Diagnostic>emptyList(), inlineMessages);
            } finally {
                CN1LambdaSupport.clearInterpreter();
                PlaygroundContext.clearCurrent();
            }
        } catch (ParseException ex) {
            int errorLine = 1;
            try {
                errorLine = ex.getErrorLineNumber();
            } catch (NullPointerException e) {
                // currentToken can be null in some parse error cases
            }
            return failure("Parse error: " + formatParseMessageWithSource(ex, script),
                    errorLine, extractColumn(ex.getMessage()), inlineMessages);
        } catch (TokenMgrException ex) {
            return failure("Lexer error: " + formatLexerMessageWithSource(ex, script),
                    extractLine(ex.getMessage()), extractColumn(ex.getMessage()), inlineMessages);
        } catch (TargetError ex) {
            return failure("Evaluation error: " + safeMessage(ex), extractTargetLine(ex), 1, inlineMessages);
        } catch (EvalError ex) {
            return failure("Evaluation error: " + safeMessage(ex), ex.getErrorLineNumber(), 1, inlineMessages);
        } catch (RuntimeException ex) {
            return failure("Unexpected error: " + safeMessage(ex), 1, 1, inlineMessages);
        }
    }

    private void bindGlobals(Interpreter interpreter, PlaygroundContext context) throws EvalError {
        NameSpace namespace = interpreter.getNameSpace();
        interpreter.set("ctx", context);
        interpreter.set("__lambdaSupport", new PlaygroundLambdaBridge());
        interpreter.set("__listenerSupport", new PlaygroundListenerBridge());
        interpreter.set("theme", context.getTheme());
        interpreter.set("hostForm", context.getHostForm());
        interpreter.set("previewRoot", context.getPreviewRoot());
        interpreter.set("display", Display.getInstance());
        interpreter.set("uiManager", UIManager.getInstance());
        interpreter.set("FontImage", FontImage.class);
        interpreter.set("CN", com.codename1.ui.CN.class);
        interpreter.set("BoxLayout", BoxLayout.class);
        interpreter.set("BorderLayout", BorderLayout.class);
        interpreter.set("FlowLayout", FlowLayout.class);
        interpreter.set("GridLayout", GridLayout.class);
        interpreter.set("LayeredLayout", LayeredLayout.class);
        interpreter.set("Style", Style.class);
        interpreter.set("Component", Component.class);
        namespace.importPackage("com.codename1.ui");
        namespace.importPackage("com.codename1.ui.layouts");
        namespace.importPackage("com.codename1.components");
        namespace.importPackage("com.codename1.ui.geom");
        namespace.importClass("com.codename1.ui.Component");
        namespace.importClass("com.codenameone.playground.PlaygroundContext");
    }

    private ScriptPlan adaptScript(String script) {
        String adapted = unwrapSingleTopLevelClass(script);
        String normalized = adapted == null ? script : adapted;
        normalized = rewriteClassModel(normalized);
        int packageEnd = skipPackageDeclaration(normalized, 0);
        int importEnd = skipImports(normalized, packageEnd);
        List<TypeDeclarationBlock> declarations = findTopLevelTypeDeclarations(normalized, importEnd);
        if (declarations.isEmpty()) {
            String wrapped = wrapLooseScript(normalized);
            return new ScriptPlan(Collections.<String>emptyList(), wrapped == null ? normalized : wrapped);
        }

        String importSection = normalized.substring(packageEnd, importEnd);
        List<String> declarationScripts = new ArrayList<String>();
        StringBuilder remainingBody = new StringBuilder();
        int cursor = importEnd;
        for (int i = 0; i < declarations.size(); i++) {
            TypeDeclarationBlock block = declarations.get(i);
            declarationScripts.add(importSection + normalized.substring(block.start, block.end + 1));
            if (cursor < block.start) {
                remainingBody.append(normalized.substring(cursor, block.start));
            }
            cursor = block.end + 1;
        }
        if (cursor < normalized.length()) {
            remainingBody.append(normalized.substring(cursor));
        }

        String rewritten = normalized.substring(0, importEnd) + remainingBody.toString();
        String wrapped = wrapLooseScript(rewritten);
        return new ScriptPlan(declarationScripts, wrapped == null ? rewritten : wrapped);
    }

    private String rewriteClassModel(String script) {
        // sealed/non-sealed/permits are stripped first so the underlying
        // class declaration parses cleanly. enforceSealedPermits runs on
        // the original script (sealed modifiers still visible) to
        // collect permit lists, then replaces any class declaration
        // whose parent is sealed and whose name isn't in the parent's
        // permit list with an immediate `throw`.
        String rewritten = enforceSealedPermits(script);
        rewritten = rewriteSealedModifiers(rewritten);
        // Records desugar into a class declaration before any other pass —
        // downstream rewrites then treat them as regular scripted classes.
        rewritten = rewriteRecords(rewritten);
        rewritten = rewriteInlineAutoCloseableClasses(rewritten);
        // Method references must be rewritten BEFORE the lambda passes so the
        // resulting lambdas get the SAM-binding treatment.
        rewritten = rewriteMethodReferences(rewritten);
        rewritten = rewriteKnownSamCalls(rewritten);
        rewritten = rewriteLambdaArguments(rewritten);
        rewritten = rewriteTopLevelLambdas(rewritten);
        rewritten = rewriteTopLevelAnonSams(rewritten);
        rewritten = rewriteSwitchExpressions(rewritten);
        // Pattern-switch arrow statements (case Type name -> ...) don't
        // fit BSH's case-label grammar. Rewrite them into explicit
        // instanceof / bind / if-else chains. Constant-label switches go
        // through rewriteArrowSwitchStatements below.
        rewritten = rewritePatternSwitchStatements(rewritten);
        rewritten = rewriteArrowSwitchStatements(rewritten);
        return rewritten;
    }

    /** Rewrites a {@code switch (x) { case Foo f -> ...; default -> ...; }}
     * statement that contains at least one pattern label into an
     * equivalent if-else chain. Constant-only switches are left alone
     * so the existing arrow-to-colon pass can handle them. */
    private String rewritePatternSwitchStatements(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "switch")) { i++; continue; }
            int parenStart = skipWhitespace(script, i + "switch".length());
            if (parenStart >= script.length() || script.charAt(parenStart) != '(') {
                i += "switch".length(); continue;
            }
            int parenEnd = findMatchingParen(script, parenStart);
            if (parenEnd < 0) break;
            int braceStart = skipWhitespace(script, parenEnd + 1);
            if (braceStart >= script.length() || script.charAt(braceStart) != '{') {
                i += "switch".length(); continue;
            }
            int braceEnd = findMatchingBrace(script, braceStart);
            if (braceEnd < 0) break;
            String scrutinee = script.substring(parenStart + 1, parenEnd).trim();
            String body = script.substring(braceStart + 1, braceEnd);
            if (!containsPatternCase(body)) { i = braceEnd + 1; continue; }
            String rewritten = rewritePatternSwitchBody(scrutinee, body);
            if (rewritten == null) { i = braceEnd + 1; continue; }
            out.append(script, last, i).append(rewritten);
            last = braceEnd + 1;
            i = last;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private boolean containsPatternCase(String body) {
        int i = 0;
        int depth = 0;
        int n = body.length();
        while (i < n) {
            char ch = body.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(body, i) + 1; continue; }
            if (startsLineComment(body, i)) { i = skipLineComment(body, i) + 1; continue; }
            if (startsBlockComment(body, i)) { i = skipBlockComment(body, i) + 1; continue; }
            if (ch == '(' || ch == '[' || ch == '{') { depth++; i++; continue; }
            if (ch == ')' || ch == ']' || ch == '}') { depth--; i++; continue; }
            if (depth == 0 && startsWithWord(body, i, "case")) {
                int labelStart = skipWhitespace(body, i + "case".length());
                // Find arrow at depth 0 from here.
                int j = labelStart;
                int localDepth = 0;
                while (j + 1 < n) {
                    char k = body.charAt(j);
                    if (k == '(' || k == '[' || k == '{') localDepth++;
                    else if (k == ')' || k == ']' || k == '}') localDepth--;
                    else if (localDepth == 0 && k == '-' && body.charAt(j + 1) == '>') break;
                    j++;
                }
                if (j + 1 >= n) { i++; continue; }
                String label = body.substring(labelStart, j).trim();
                if (looksLikePatternLabel(label)) return true;
                i = j + 2;
                continue;
            }
            i++;
        }
        return false;
    }

    /** A pattern label looks like {@code Type ident} — a Java-identifier
     * (possibly dotted or with type args) followed by a bare identifier
     * binding. Constant labels (a number, a dotted-name constant, a
     * string literal) don't match because they have no trailing
     * identifier at top level. */
    private boolean looksLikePatternLabel(String label) {
        if (label.isEmpty() || label.charAt(0) == '"' || label.charAt(0) == '\'') return false;
        if (!isIdentifierStart(label.charAt(0))) return false;
        int i = 0;
        int n = label.length();
        int depth = 0;
        int lastNonWsAtDepth0 = -1;
        int firstWsAtDepth0 = -1;
        while (i < n) {
            char ch = label.charAt(i);
            if (ch == '<') depth++;
            else if (ch == '>') depth--;
            else if (depth == 0 && Character.isWhitespace(ch)) {
                if (firstWsAtDepth0 < 0 && lastNonWsAtDepth0 >= 0) firstWsAtDepth0 = i;
            } else if (depth == 0) {
                lastNonWsAtDepth0 = i;
            }
            i++;
        }
        if (firstWsAtDepth0 < 0) return false;
        String head = label.substring(0, firstWsAtDepth0).trim();
        String tail = label.substring(firstWsAtDepth0).trim();
        // Head must look like a type reference (identifiers and dots);
        // tail must be a single identifier.
        for (int j = 0; j < head.length(); j++) {
            char c = head.charAt(j);
            if (!isIdentifierPart(c) && c != '.' && c != '<' && c != '>' && c != ',' && c != '?'
                    && !Character.isWhitespace(c)) return false;
        }
        for (int j = 0; j < tail.length(); j++) {
            char c = tail.charAt(j);
            if (!isIdentifierPart(c)) return false;
        }
        return true;
    }

    private static int PATTERN_SWITCH_COUNTER = 0;

    private String rewritePatternSwitchBody(String scrutinee, String body) {
        String scrVar = "__patternScrutinee" + (++PATTERN_SWITCH_COUNTER);
        StringBuilder out = new StringBuilder();
        out.append("{ Object ").append(scrVar).append(" = ").append(scrutinee).append(";");
        int i = 0;
        int n = body.length();
        boolean firstBranch = true;
        while (i < n) {
            i = skipWhitespace(body, i);
            if (i >= n) break;
            String kw;
            if (startsWithWord(body, i, "case")) kw = "case";
            else if (startsWithWord(body, i, "default")) kw = "default";
            else return null;
            int afterKw = i + kw.length();
            int arrow = -1;
            int depth = 0;
            for (int j = afterKw; j + 1 < n; j++) {
                char ch = body.charAt(j);
                if (ch == '"' || ch == '\'') { j = skipQuoted(body, j); continue; }
                if (ch == '(' || ch == '[' || ch == '{') depth++;
                else if (ch == ')' || ch == ']' || ch == '}') depth--;
                else if (depth == 0 && ch == '-' && body.charAt(j + 1) == '>') {
                    arrow = j;
                    break;
                }
            }
            if (arrow < 0) return null;
            String label = body.substring(afterKw, arrow).trim();
            int bodyStart = skipWhitespace(body, arrow + 2);
            String caseBody;
            int bodyEnd;
            if (bodyStart < n && body.charAt(bodyStart) == '{') {
                int close = findMatchingBrace(body, bodyStart);
                if (close < 0) return null;
                caseBody = body.substring(bodyStart + 1, close);
                bodyEnd = close + 1;
                int semi = skipWhitespace(body, bodyEnd);
                if (semi < n && body.charAt(semi) == ';') bodyEnd = semi + 1;
            } else {
                int semi = findTopLevelSemicolon(body, bodyStart);
                if (semi < 0) return null;
                caseBody = body.substring(bodyStart, semi);
                bodyEnd = semi + 1;
            }
            if ("default".equals(kw)) {
                if (!firstBranch) out.append(" else ");
                out.append("{ ").append(caseBody);
                if (!caseBody.trim().endsWith(";") && !caseBody.trim().endsWith("}")) out.append(';');
                out.append(" }");
            } else {
                // Extract Type and variable name from label.
                int lastWs = -1;
                int depth2 = 0;
                for (int j = 0; j < label.length(); j++) {
                    char k = label.charAt(j);
                    if (k == '<') depth2++;
                    else if (k == '>') depth2--;
                    else if (depth2 == 0 && Character.isWhitespace(k)) lastWs = j;
                }
                String type;
                String binding;
                if (lastWs < 0) {
                    // Constant-label inside a pattern switch — compare by equals.
                    type = null;
                    binding = null;
                }
                else {
                    type = label.substring(0, lastWs).trim();
                    binding = label.substring(lastWs + 1).trim();
                }
                if (!firstBranch) out.append(" else ");
                if (type != null) {
                    out.append("if (").append(scrVar).append(" instanceof ").append(type).append(") { ")
                            .append(type).append(' ').append(binding).append(" = (").append(type).append(") ")
                            .append(scrVar).append("; ").append(caseBody);
                } else {
                    out.append("if (").append(scrVar).append(" != null && ").append(scrVar).append(".equals(")
                            .append(label).append(")) { ").append(caseBody);
                }
                if (!caseBody.trim().endsWith(";") && !caseBody.trim().endsWith("}")) out.append(';');
                out.append(" }");
            }
            firstBranch = false;
            i = bodyEnd;
        }
        out.append(" }");
        return out.toString();
    }

    /**
     * Collect permit lists from {@code sealed class X permits A, B} and
     * then replace any class declaration whose parent is in the map
     * but whose own name isn't in the parent's permit set with an
     * immediate {@code throw}. Runs ahead of
     * {@link #rewriteSealedModifiers} so the sealed keyword is still
     * visible. Enforcement is best-effort across a single snippet —
     * sealed hierarchies that span multiple source roots aren't
     * supported (documented as out of scope).
     */
    private String enforceSealedPermits(String script) {
        java.util.Map<String, java.util.Set<String>> permitsByParent = collectSealedPermits(script);
        if (permitsByParent.isEmpty()) return script;
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        int n = script.length();
        while (i < n) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "class")) { i++; continue; }
            int nameStart = skipWhitespace(script, i + "class".length());
            int nameEnd = nameStart;
            while (nameEnd < n && isIdentifierPart(script.charAt(nameEnd))) nameEnd++;
            if (nameStart == nameEnd) { i++; continue; }
            String className = script.substring(nameStart, nameEnd);
            int cursor = skipWhitespace(script, nameEnd);
            if (cursor >= n || !startsWithWord(script, cursor, "extends")) { i = nameEnd; continue; }
            int parentStart = skipWhitespace(script, cursor + "extends".length());
            int parentEnd = parentStart;
            while (parentEnd < n && (isIdentifierPart(script.charAt(parentEnd)) || script.charAt(parentEnd) == '.')) {
                parentEnd++;
            }
            if (parentStart == parentEnd) { i = nameEnd; continue; }
            String parentName = script.substring(parentStart, parentEnd);
            java.util.Set<String> permits = permitsByParent.get(parentName);
            if (permits == null || permits.contains(className)) { i = parentEnd; continue; }
            int braceStart = findClassBodyBraceStart(script, parentEnd);
            if (braceStart < 0) { i = parentEnd; continue; }
            int braceEnd = findMatchingBrace(script, braceStart);
            if (braceEnd < 0) { i = parentEnd; continue; }
            // Include any preceding modifiers (final/public/abstract/...) in
            // the replacement so the emitted `throw` isn't prefixed with a
            // dangling keyword like `final throw new ...`.
            int replaceStart = backUpOverClassModifiers(script, i);
            out.append(script, last, replaceStart);
            out.append("throw new RuntimeException(\"class '").append(className)
                    .append("' is not permitted to extend sealed class '").append(parentName)
                    .append("'\");");
            last = braceEnd + 1;
            i = last;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    /** Back up from a {@code class} keyword over any contiguous modifier
     * keywords (public/private/protected/abstract/static/final/strictfp)
     * so a replacement can subsume them. */
    private int backUpOverClassModifiers(String script, int classPos) {
        int pos = classPos;
        while (pos > 0) {
            int end = pos;
            int cursor = end - 1;
            while (cursor >= 0 && Character.isWhitespace(script.charAt(cursor))) cursor--;
            int wordEnd = cursor + 1;
            int wordStart = wordEnd;
            while (wordStart > 0 && isIdentifierPart(script.charAt(wordStart - 1))) wordStart--;
            if (wordStart == wordEnd) return pos;
            String word = script.substring(wordStart, wordEnd);
            if (!isClassModifier(word)) return pos;
            pos = wordStart;
        }
        return pos;
    }

    /** Scan forward from a position just after the parent type for the
     * opening brace of the class body, skipping an {@code implements}
     * clause if present. Returns {@code -1} if structure doesn't match. */
    private int findClassBodyBraceStart(String script, int from) {
        int i = from;
        int n = script.length();
        while (i < n) {
            char ch = script.charAt(i);
            if (ch == '{') return i;
            if (ch == ';') return -1;
            i++;
        }
        return -1;
    }

    /** Build a {@code parent -> permits} map from the script's
     * {@code sealed class X [extends ...] permits A, B} declarations. */
    private java.util.Map<String, java.util.Set<String>> collectSealedPermits(String script) {
        java.util.Map<String, java.util.Set<String>> out = new java.util.LinkedHashMap<String, java.util.Set<String>>();
        int i = 0;
        int n = script.length();
        while (i < n) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "permits")) { i++; continue; }
            String parent = findPrecedingClassName(script, i);
            int end = i + "permits".length();
            int j = end;
            while (j < n && script.charAt(j) != '{' && script.charAt(j) != ';') j++;
            if (parent != null) {
                java.util.Set<String> permits = parseCommaList(script.substring(end, j));
                out.put(parent, permits);
            }
            i = j;
        }
        return out;
    }

    private java.util.Set<String> parseCommaList(String input) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        int from = 0;
        int n = input.length();
        for (int i = 0; i <= n; i++) {
            if (i == n || input.charAt(i) == ',') {
                String part = input.substring(from, i).trim();
                if (!part.isEmpty()) out.add(part);
                from = i + 1;
            }
        }
        return out;
    }

    private String findPrecedingClassName(String script, int permitsPos) {
        int i = permitsPos - 1;
        while (i >= 0 && Character.isWhitespace(script.charAt(i))) i--;
        for (int pass = 0; pass < 2; pass++) {
            int end = i + 1;
            int start = end;
            while (start > 0 && isIdentifierPart(script.charAt(start - 1))) start--;
            if (start == end) return null;
            String token = script.substring(start, end);
            i = start - 1;
            while (i >= 0 && Character.isWhitespace(script.charAt(i))) i--;
            if ("extends".equals(token)) continue;
            int prevEnd = i + 1;
            int prevStart = prevEnd;
            while (prevStart > 0 && isIdentifierPart(script.charAt(prevStart - 1))) prevStart--;
            if (prevStart < prevEnd && "class".equals(script.substring(prevStart, prevEnd))) {
                return token;
            }
            return null;
        }
        return null;
    }

    /**
     * Strips Java 17 sealed/non-sealed/permits modifiers from class
     * declarations so they parse with the existing grammar. The permit
     * enforcement happens earlier in {@link #enforceSealedPermits}.
     */
    private String rewriteSealedModifiers(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (startsWithWord(script, i, "sealed")) {
                out.append(script, last, i);
                last = i + "sealed".length();
                i = last;
                continue;
            }
            if (startsWithWord(script, i, "non-sealed")) {
                out.append(script, last, i);
                last = i + "non-sealed".length();
                i = last;
                continue;
            }
            if (startsWithWord(script, i, "permits")) {
                int end = i + "permits".length();
                // Consume identifiers and commas up to '{' or end-of-input.
                int j = end;
                while (j < script.length()) {
                    char k = script.charAt(j);
                    if (k == '{' || k == ';') break;
                    j++;
                }
                out.append(script, last, i);
                last = j;
                i = j;
                continue;
            }
            i++;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    /**
     * Desugars Java 14+ records of the form {@code record Name(t1 a, t2 b) {
     * ... }} into an equivalent class with final fields, a canonical
     * constructor, and accessor methods named after each component. The
     * optional body block is included verbatim after the synthetic members,
     * so users can declare extra static methods or override an accessor.
     */
    private String rewriteRecords(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "record")) { i++; continue; }
            int nameStart = skipWhitespace(script, i + "record".length());
            int nameEnd = nameStart;
            while (nameEnd < script.length() && isIdentifierPart(script.charAt(nameEnd))) nameEnd++;
            if (nameStart == nameEnd) { i += "record".length(); continue; }
            int parenStart = skipWhitespace(script, nameEnd);
            if (parenStart >= script.length() || script.charAt(parenStart) != '(') {
                i += "record".length();
                continue;
            }
            int parenEnd = findMatchingParen(script, parenStart);
            if (parenEnd < 0) break;
            int braceStart = skipWhitespace(script, parenEnd + 1);
            if (braceStart >= script.length() || script.charAt(braceStart) != '{') {
                i += "record".length();
                continue;
            }
            int braceEnd = findMatchingBrace(script, braceStart);
            if (braceEnd < 0) break;
            String recordName = script.substring(nameStart, nameEnd);
            String params = script.substring(parenStart + 1, parenEnd).trim();
            String body = script.substring(braceStart + 1, braceEnd).trim();
            String desugar = desugarRecord(recordName, params, body);
            if (desugar == null) { i = braceEnd + 1; continue; }
            out.append(script, last, i).append(desugar);
            last = braceEnd + 1;
            i = last;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private String desugarRecord(String name, String params, String body) {
        String[] componentDecls = splitTopLevel(params, ',');
        StringBuilder fields = new StringBuilder();
        StringBuilder ctorAssigns = new StringBuilder();
        StringBuilder accessors = new StringBuilder();
        StringBuilder ctorParams = new StringBuilder();
        for (int i = 0; i < componentDecls.length; i++) {
            String c = componentDecls[i].trim();
            if (c.isEmpty()) continue;
            // Each component is "<type> <name>". Split on last whitespace
            // boundary that isn't inside angle brackets (for generic types).
            int split = lastTopLevelWhitespace(c);
            if (split < 0) return null;
            String type = c.substring(0, split).trim();
            String comp = c.substring(split).trim();
            if (type.isEmpty() || comp.isEmpty()) return null;
            // We deliberately drop `private final` here: the playground's
            // BSH layer treats `final` as immutable and would reject the
            // canonical ctor's `this.<comp> = <comp>` assignment, leaving the
            // field at its default value. Records are still effectively
            // immutable from the user's perspective because the desugar emits
            // no setters.
            fields.append(type).append(' ').append(comp).append(';');
            ctorAssigns.append("this.").append(comp).append('=').append(comp).append(';');
            accessors.append("public ").append(type).append(' ').append(comp)
                    .append("(){return this.").append(comp).append(";}");
            if (ctorParams.length() > 0) ctorParams.append(',');
            ctorParams.append(type).append(' ').append(comp);
        }
        // Compact-ctor form: `<RecordName> { ... }` inside the body runs
        // validation/normalization before the implicit field assignments.
        // Extract it and inline the statements ahead of the assignments.
        String compactBody = extractCompactRecordCtor(name, body);
        String trimmedBody;
        String prefixStmts;
        if (compactBody == null) {
            trimmedBody = body;
            prefixStmts = "";
        } else {
            trimmedBody = compactBody.substring(0, compactBody.indexOf('\u0000'));
            prefixStmts = compactBody.substring(compactBody.indexOf('\u0000') + 1);
        }
        StringBuilder out = new StringBuilder();
        out.append("class ").append(name).append('{');
        out.append(fields);
        out.append(name).append('(').append(ctorParams).append("){");
        if (!prefixStmts.isEmpty()) out.append(prefixStmts).append(';');
        out.append(ctorAssigns).append('}');
        out.append(accessors);
        if (!trimmedBody.isEmpty()) out.append(trimmedBody);
        out.append('}');
        return out.toString();
    }

    /** Looks for a compact record constructor inside the body: the
     * bare form {@code <RecordName> { stmts }} with no parameter list.
     * Returns {@code null} if none present, otherwise a string of the
     * form {@code <bodyWithoutCompact>\u0000<stmts>} so the caller can
     * get both halves without allocating a pair. */
    private String extractCompactRecordCtor(String recordName, String body) {
        int i = 0;
        while (i < body.length()) {
            char ch = body.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(body, i) + 1; continue; }
            if (startsLineComment(body, i)) { i = skipLineComment(body, i) + 1; continue; }
            if (startsBlockComment(body, i)) { i = skipBlockComment(body, i) + 1; continue; }
            if (!startsWithWord(body, i, recordName)) { i++; continue; }
            int after = skipWhitespace(body, i + recordName.length());
            if (after >= body.length() || body.charAt(after) != '{') { i += recordName.length(); continue; }
            int braceEnd = findMatchingBrace(body, after);
            if (braceEnd < 0) return null;
            String stmts = body.substring(after + 1, braceEnd).trim();
            String stripped = body.substring(0, i) + body.substring(braceEnd + 1);
            return stripped + "\u0000" + stmts;
        }
        return null;
    }

    private int lastTopLevelWhitespace(String s) {
        int depth = 0;
        int last = -1;
        for (int j = 0; j < s.length(); j++) {
            char ch = s.charAt(j);
            if (ch == '<') depth++;
            else if (ch == '>') depth--;
            else if (depth == 0 && Character.isWhitespace(ch)) last = j;
        }
        return last;
    }

    /**
     * Rewrites Java 8 method references to equivalent single-arg lambdas
     * before BSH parses the script. The lexer does not tokenise {@code ::}
     * so without this rewrite any method-reference site parse-errors.
     *
     * <p>We can't infer the target SAM's arity from syntax alone (e.g.
     * {@code Supplier<X> s = X::new} wants zero args, {@code Function<X,Y>}
     * wants one). We emit a one-arg lambda which suits the common cases —
     * {@code addActionListener(obj::method)}, {@code stream.forEach(obj::m)},
     * etc. Zero-arg targets like {@code Supplier} still parse-error today.
     */
    private String rewriteMethodReferences(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i + 1 < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!(ch == ':' && script.charAt(i + 1) == ':')) { i++; continue; }
            int receiverStart = findMethodRefReceiverStart(script, i);
            if (receiverStart < 0) { i += 2; continue; }
            int targetStart = i + 2;
            int targetEnd = targetStart;
            while (targetEnd < script.length() && isIdentifierPart(script.charAt(targetEnd))) targetEnd++;
            if (targetStart == targetEnd) { i += 2; continue; }
            String receiver = script.substring(receiverStart, i).trim();
            String target = script.substring(targetStart, targetEnd);
            if (receiver.isEmpty()) { i += 2; continue; }
            String replacement;
            if ("new".equals(target)) {
                // Both zero- and one-arg constructor refs are common; emit a
                // zero-arg lambda since most are passed to Supplier-style
                // SAMs. One-arg constructor refs (Consumer<T> applying new
                // Wrapper(T)) remain a documented limitation.
                replacement = "(() -> new " + receiver + "())";
            } else if (looksLikeClassName(receiver)) {
                // Class-like receiver: could be either static (`Math::abs`)
                // or unbound instance (`String::length`). Java disambiguates
                // via target SAM info; we emit the unbound-instance shape
                // because the method name is more often an instance method.
                // Static refs in known-SAM call sites (System.out::println)
                // are unaffected — `System.out` is not a single uppercase
                // identifier, so this branch doesn't fire there.
                replacement = "((__mref_a) -> __mref_a." + target + "())";
            } else {
                replacement = "((__mref_a) -> " + receiver + "." + target + "(__mref_a))";
            }
            out.append(script, last, receiverStart).append(replacement);
            last = targetEnd;
            i = last;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private static final java.util.Set<String> KNOWN_LAMBDA_SAM_TYPES;
    static {
        java.util.Set<String> s = new java.util.HashSet<String>();
        // Mirror the list of SAMs implemented by CN1LambdaSupport.LambdaValue.
        s.add("Runnable");
        s.add("Supplier");
        s.add("Consumer");
        s.add("BiConsumer");
        s.add("Function");
        s.add("Predicate");
        s.add("Comparator");
        KNOWN_LAMBDA_SAM_TYPES = java.util.Collections.unmodifiableSet(s);
    }

    /**
     * Rewrites statement-level {@code new SamType() { method bodies }}
     * expressions to {@code __lambdaSupport.lambda(params, body)} so the
     * resulting LambdaValue (which implements common SAMs directly) can be
     * assigned to {@code Runnable}, {@code Function}, etc. Without this,
     * `Runnable r = new Runnable() { public void run() {} }` hits BSH's
     * legacy "Anonymous interface implementations are not supported" path.
     */
    private String rewriteTopLevelAnonSams(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i + 4 < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "new")) { i++; continue; }
            int afterNew = i + 3;
            int typeEnd = afterNew;
            while (typeEnd < script.length() && Character.isWhitespace(script.charAt(typeEnd))) typeEnd++;
            int parenStart = findTopLevelChar(script, '(', typeEnd);
            if (parenStart < 0) { i++; continue; }
            // Only consider single-identifier types here (no dots/generics)
            // to avoid colliding with regular constructor calls.
            String typeName = script.substring(typeEnd, parenStart).trim();
            if (typeName.isEmpty() || !isSimpleIdentifier(typeName)) { i++; continue; }
            // Restrict to types LambdaValue actually implements. For
            // user-declared scripted interfaces, the anon-class path on
            // BSHAllocationExpression handles the body; we mustn't intercept
            // those here or the resulting LambdaValue won't have the user's
            // method.
            if (!KNOWN_LAMBDA_SAM_TYPES.contains(typeName)) { i++; continue; }
            int closeParen = findMatchingParen(script, parenStart);
            if (closeParen < 0) break;
            // Args inside `()` must be empty for anon-SAM construction.
            if (containsNonWhitespace(script.substring(parenStart + 1, closeParen))) {
                i++; continue;
            }
            int braceStart = skipWhitespace(script, closeParen + 1);
            if (braceStart >= script.length() || script.charAt(braceStart) != '{') {
                i++; continue;
            }
            int braceEnd = findMatchingBrace(script, braceStart);
            if (braceEnd < 0) break;
            String segment = script.substring(i, braceEnd + 1);
            String rewritten = rewriteAnonymousSamExpression(segment);
            if (rewritten == null) { i++; continue; }
            out.append(script, last, i).append(rewritten);
            last = braceEnd + 1;
            i = last;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private boolean isSimpleIdentifier(String s) {
        if (s.isEmpty() || !isIdentifierStart(s.charAt(0))) return false;
        for (int j = 1; j < s.length(); j++) {
            if (!isIdentifierPart(s.charAt(j))) return false;
        }
        return true;
    }

    /** Heuristic: a receiver is "class-like" if it's a single identifier
     * starting with an uppercase letter — likely a Java class name rather
     * than a field/local variable. */
    private boolean looksLikeClassName(String receiver) {
        if (receiver == null || receiver.isEmpty()) return false;
        if (receiver.indexOf('.') >= 0) return false;
        char c = receiver.charAt(0);
        return Character.isUpperCase(c);
    }

    /** Walks back from a {@code ::} position to find the receiver expression
     * start. A receiver may be a dotted class name (System.out), a single
     * identifier, or — eventually — a parenthesised expression. */
    private int findMethodRefReceiverStart(String script, int doubleColonPos) {
        int j = doubleColonPos - 1;
        while (j >= 0 && Character.isWhitespace(script.charAt(j))) j--;
        if (j < 0) return -1;
        // Receiver is a chain of identifiers separated by dots, or a single
        // identifier. We don't yet support parenthesised receivers.
        if (!isIdentifierPart(script.charAt(j))) return -1;
        int end = j + 1;
        while (j >= 0) {
            while (j >= 0 && isIdentifierPart(script.charAt(j))) j--;
            int wordStart = j + 1;
            if (wordStart >= end) return -1;
            if (j < 0 || script.charAt(j) != '.') {
                // boundary — receiver starts at wordStart
                if (!isIdentifierStart(script.charAt(wordStart))) return -1;
                return wordStart;
            }
            // include the dot and continue back
            j--;
        }
        return -1;
    }

    /**
     * Rewrites Java 14+ switch expressions of the form
     * {@code <Type> <name> = switch (<expr>) { case <v> -> <r>; ...; default -> <rd>; };}
     * into a declaration plus a traditional switch statement. Runs
     * iteratively so that nested switch expressions (a switch produced as
     * the right-hand-side of a case body from an outer rewrite) are also
     * handled. Switch expressions used inline (as a method argument) still
     * parse-error.
     */
    private String rewriteSwitchExpressions(String script) {
        String prev = null;
        String cur = script;
        int iterations = 0;
        while (!cur.equals(prev) && iterations < 16) {
            prev = cur;
            cur = rewriteSwitchExpressionsOnce(cur);
            iterations++;
        }
        return cur;
    }

    private String rewriteSwitchExpressionsOnce(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "switch")) { i++; continue; }
            // Walk back to see if this is an expression-context switch
            // (preceded by `=`). If not, leave it for the statement rewrite.
            int back = i - 1;
            while (back >= 0 && Character.isWhitespace(script.charAt(back))) back--;
            if (back < 0 || script.charAt(back) != '=') { i += "switch".length(); continue; }
            // Find the lhs (declaration or bare name). Walk back to the
            // statement start (top-level `;` or `{` or beginning).
            int stmtStart = findSwitchStmtStart(script, back);
            String lhs = script.substring(stmtStart, back).trim();
            if (lhs.length() == 0) { i += "switch".length(); continue; }
            String varName = extractVarName(lhs);
            if (varName == null) { i += "switch".length(); continue; }
            int parenStart = skipWhitespace(script, i + "switch".length());
            if (parenStart >= script.length() || script.charAt(parenStart) != '(') {
                i += "switch".length();
                continue;
            }
            int parenEnd = findMatchingParen(script, parenStart);
            if (parenEnd < 0) break;
            int braceStart = skipWhitespace(script, parenEnd + 1);
            if (braceStart >= script.length() || script.charAt(braceStart) != '{') {
                i += "switch".length();
                continue;
            }
            int braceEnd = findMatchingBrace(script, braceStart);
            if (braceEnd < 0) break;
            int semi = skipWhitespace(script, braceEnd + 1);
            if (semi >= script.length() || script.charAt(semi) != ';') {
                i += "switch".length();
                continue;
            }
            String discriminant = script.substring(parenStart + 1, parenEnd);
            String body = script.substring(braceStart + 1, braceEnd);
            String rewrittenBody = rewriteSwitchExprBodyToAssignments(body, varName);
            if (rewrittenBody == null) { i += "switch".length(); continue; }
            out.append(script, last, stmtStart);
            out.append(lhs).append(';');
            out.append("switch(").append(discriminant).append(") {")
                    .append(rewrittenBody).append('}');
            last = semi + 1;
            i = last;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    /**
     * Rewrites arrow-form switch *statements* (no result value):
     * {@code switch (x) { case 1 -> doA(); default -> doB(); }} into
     * traditional case-label form with explicit breaks.
     */
    private String rewriteArrowSwitchStatements(String script) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        while (i < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(script, i) + 1; continue; }
            if (startsLineComment(script, i)) { i = skipLineComment(script, i) + 1; continue; }
            if (startsBlockComment(script, i)) { i = skipBlockComment(script, i) + 1; continue; }
            if (!startsWithWord(script, i, "switch")) { i++; continue; }
            int parenStart = skipWhitespace(script, i + "switch".length());
            if (parenStart >= script.length() || script.charAt(parenStart) != '(') {
                i += "switch".length(); continue;
            }
            int parenEnd = findMatchingParen(script, parenStart);
            if (parenEnd < 0) break;
            int braceStart = skipWhitespace(script, parenEnd + 1);
            if (braceStart >= script.length() || script.charAt(braceStart) != '{') {
                i += "switch".length(); continue;
            }
            int braceEnd = findMatchingBrace(script, braceStart);
            if (braceEnd < 0) break;
            String body = script.substring(braceStart + 1, braceEnd);
            if (!containsArrowCase(body)) { i = braceEnd + 1; continue; }
            String rewrittenBody = rewriteSwitchStmtBodyArrowToColon(body);
            if (rewrittenBody == null) { i = braceEnd + 1; continue; }
            out.append(script, last, braceStart + 1);
            out.append(rewrittenBody);
            last = braceEnd;
            i = braceEnd;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private int findSwitchStmtStart(String script, int from) {
        int depth = 0;
        for (int j = from; j >= 0; j--) {
            char ch = script.charAt(j);
            if (ch == ')' || ch == ']' || ch == '}') depth++;
            else if (ch == '(' || ch == '[' || ch == '{') {
                if (depth == 0) return j + 1;
                depth--;
            } else if (depth == 0 && ch == ';') {
                return j + 1;
            }
        }
        return 0;
    }

    /** Extract the variable name from an LHS like {@code int x} or {@code x}. */
    private String extractVarName(String lhs) {
        String s = lhs.trim();
        int end = s.length();
        int start = end;
        while (start > 0 && (isIdentifierPart(s.charAt(start - 1)))) start--;
        if (start >= end) return null;
        if (!isIdentifierStart(s.charAt(start))) return null;
        return s.substring(start, end);
    }

    /** Body is "case A -> X; case B -> Y; default -> Z;" — produce
     * "case A: var = X; break; case B: var = Y; break; default: var = Z; break;"
     * Also handles the yield form: "case A: yield X; default: yield Z;" by
     * substituting `yield X;` with `var = X; break;`. */
    private String rewriteSwitchExprBodyToAssignments(String body, String varName) {
        if (containsArrowCase(body)) {
            return rewriteSwitchBody(body, varName);
        }
        // Yield form: leave the case-label structure intact and rewrite each
        // `yield <expr>;` into `<varName> = <expr>; break;`.
        return rewriteYieldStatements(body, varName);
    }

    private String rewriteYieldStatements(String body, String varName) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int last = 0;
        int depth = 0;
        while (i < body.length()) {
            char ch = body.charAt(i);
            if (ch == '"' || ch == '\'') { i = skipQuoted(body, i) + 1; continue; }
            if (startsLineComment(body, i)) { i = skipLineComment(body, i) + 1; continue; }
            if (startsBlockComment(body, i)) { i = skipBlockComment(body, i) + 1; continue; }
            if (ch == '(' || ch == '[' || ch == '{') { depth++; i++; continue; }
            if (ch == ')' || ch == ']' || ch == '}') { depth--; i++; continue; }
            if (depth == 0 && startsWithWord(body, i, "yield")) {
                int exprStart = skipWhitespace(body, i + "yield".length());
                int semi = findTopLevelSemicolon(body, exprStart);
                if (semi < 0) { i++; continue; }
                String expr = body.substring(exprStart, semi).trim();
                out.append(body, last, i);
                out.append(varName).append(" = ").append(expr).append("; break;");
                last = semi + 1;
                i = last;
                continue;
            }
            i++;
        }
        out.append(body.substring(last));
        return out.toString();
    }

    private String rewriteSwitchStmtBodyArrowToColon(String body) {
        return rewriteSwitchBody(body, null);
    }

    private boolean containsArrowCase(String body) {
        // Crude but sufficient: look for `->` at depth 0 within this body.
        int depth = 0;
        for (int j = 0; j < body.length(); j++) {
            char ch = body.charAt(j);
            if (ch == '"' || ch == '\'') { j = skipQuoted(body, j); continue; }
            if (startsLineComment(body, j)) { j = skipLineComment(body, j); continue; }
            if (startsBlockComment(body, j)) { j = skipBlockComment(body, j); continue; }
            if (ch == '(' || ch == '[' || ch == '{') depth++;
            else if (ch == ')' || ch == ']' || ch == '}') depth--;
            else if (depth == 0 && ch == '-' && j + 1 < body.length() && body.charAt(j + 1) == '>') {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the switch body and rewrites each {@code case <e> -> <body>;}
     * into {@code case <e>: <body or assignment>; break;}. When {@code
     * varName} is non-null, single-expression bodies become assignments to
     * that variable; otherwise they stay as expression statements.
     * Returns null if any case shape is unrecognised.
     */
    private String rewriteSwitchBody(String body, String varName) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = body.length();
        while (i < n) {
            i = skipWhitespace(body, i);
            if (i >= n) break;
            String labelKw;
            if (startsWithWord(body, i, "case")) labelKw = "case";
            else if (startsWithWord(body, i, "default")) labelKw = "default";
            else return null;
            int afterKw = i + labelKw.length();
            int arrow = -1;
            int depth = 0;
            int labelEnd = -1;
            // Find the arrow `->` at depth 0.
            for (int j = afterKw; j + 1 < n; j++) {
                char ch = body.charAt(j);
                if (ch == '"' || ch == '\'') { j = skipQuoted(body, j); continue; }
                if (ch == '(' || ch == '[' || ch == '{') depth++;
                else if (ch == ')' || ch == ']' || ch == '}') depth--;
                else if (depth == 0 && ch == '-' && body.charAt(j + 1) == '>') {
                    arrow = j;
                    labelEnd = j;
                    break;
                }
            }
            if (arrow < 0) return null;
            String label = body.substring(afterKw, labelEnd).trim();
            int bodyStart = skipWhitespace(body, arrow + 2);
            int bodyEnd;
            String caseBody;
            if (bodyStart < n && body.charAt(bodyStart) == '{') {
                int close = findMatchingBrace(body, bodyStart);
                if (close < 0) return null;
                caseBody = body.substring(bodyStart + 1, close);
                bodyEnd = close + 1;
                int semi = skipWhitespace(body, bodyEnd);
                if (semi < n && body.charAt(semi) == ';') bodyEnd = semi + 1;
            } else {
                int semi = findTopLevelSemicolon(body, bodyStart);
                if (semi < 0) return null;
                caseBody = body.substring(bodyStart, semi);
                bodyEnd = semi + 1;
            }
            out.append(' ').append(labelKw);
            if ("case".equals(labelKw)) {
                out.append(' ').append(label).append(':');
            } else {
                out.append(':');
            }
            out.append(' ');
            String trimmedBody = caseBody.trim();
            if (varName != null && !trimmedBody.endsWith(";")) {
                out.append(varName).append(" = ").append(trimmedBody).append(';');
            } else if (varName != null) {
                // block body — assume last expression-statement is the
                // implicit yield. Keep body as-is.
                out.append(caseBody).append(';');
            } else {
                out.append(caseBody);
                if (!caseBody.trim().endsWith(";") && !caseBody.trim().endsWith("}")) out.append(';');
            }
            out.append(" break;");
            i = bodyEnd;
        }
        return out.toString();
    }

    private int findTopLevelSemicolon(String text, int from) {
        int depth = 0;
        for (int j = from; j < text.length(); j++) {
            char ch = text.charAt(j);
            if (ch == '"' || ch == '\'') { j = skipQuoted(text, j); continue; }
            if (startsLineComment(text, j)) { j = skipLineComment(text, j); continue; }
            if (startsBlockComment(text, j)) { j = skipBlockComment(text, j); continue; }
            if (ch == '(' || ch == '[' || ch == '{') depth++;
            else if (ch == ')' || ch == ']' || ch == '}') depth--;
            else if (depth == 0 && ch == ';') return j;
        }
        return -1;
    }

    private String rewriteInlineAutoCloseableClasses(String script) {
        RE declarationPattern = new RE(
                "class\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:extends\\s+[A-Za-z_$][A-Za-z0-9_$.]*\\s*)?implements\\s+[^\\{]*AutoCloseable[^\\{]*\\{\\s*public\\s+void\\s+close\\s*\\(\\s*\\)\\s*\\{\\s*\\}\\s*\\}");
        List<String> helperClassNames = new ArrayList<String>();
        int searchFrom = 0;
        while (searchFrom < script.length() && declarationPattern.match(script, searchFrom)) {
            helperClassNames.add(declarationPattern.getParen(1));
            int next = declarationPattern.getParenEnd(0);
            if (next <= searchFrom) {
                break;
            }
            searchFrom = next;
        }
        if (helperClassNames.isEmpty()) {
            return script;
        }

        String rewritten = script;
        for (int i = 0; i < helperClassNames.size(); i++) {
            String className = helperClassNames.get(i);
            String classToken = escapeRegexLiteral(className);
            RE resourceTypePattern = new RE("([\\(;]\\s*)" + classToken + "(\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*=)");
            RE ctorPattern = new RE("\\bnew\\s+" + classToken + "\\s*\\(\\s*\\)");
            rewritten = resourceTypePattern.subst(rewritten, "$1java.io.StringReader$2",
                    RE.REPLACE_ALL | RE.REPLACE_BACKREFERENCES);
            rewritten = ctorPattern.subst(rewritten,
                    "new java.io.StringReader(\"\")", RE.REPLACE_ALL);
        }
        rewritten = declarationPattern.subst(rewritten, "", RE.REPLACE_ALL);
        return rewritten;
    }

    private String escapeRegexLiteral(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '$' || ch == '.' || ch == '[' || ch == ']' || ch == '(' || ch == ')'
                    || ch == '{' || ch == '}' || ch == '+' || ch == '*' || ch == '?' || ch == '^'
                    || ch == '|') {
                out.append('\\');
            }
            out.append(ch);
        }
        return out.toString();
    }

    private List<TypeDeclarationBlock> findTopLevelTypeDeclarations(String script, int start) {
        List<TypeDeclarationBlock> out = new ArrayList<TypeDeclarationBlock>();
        int depth = 0;
        for (int i = start; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
                continue;
            }
            if (ch == '{') {
                depth++;
                continue;
            }
            if (ch == '}') {
                depth--;
                continue;
            }
            if (depth != 0) {
                continue;
            }
            if (startsWithWord(script, i, "class")
                    || startsWithWord(script, i, "interface")
                    || startsWithWord(script, i, "enum")) {
                int declarationStart = findClassModifiersStart(script, i);
                int openingBrace = findOpeningBrace(script, i);
                if (openingBrace < 0) {
                    continue;
                }
                int end = findMatchingBrace(script, openingBrace);
                if (end < 0) {
                    continue;
                }
                out.add(new TypeDeclarationBlock(declarationStart, end));
                i = end;
            }
        }
        return out;
    }

    private RunResult failure(String message, int line, int column, List<InlineMessage> inlineMessages) {
        int safeLine = Math.max(1, line);
        int safeColumn = Math.max(1, column);
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(new Diagnostic(safeLine, safeColumn, safeLine, safeColumn + 1, message, "error"));
        inlineMessages.add(new InlineMessage(safeLine, message, "error"));
        return new RunResult(null, diagnostics, inlineMessages);
    }

    private int extractTargetLine(TargetError ex) {
        if (ex.getTarget() instanceof EvalError) {
            return ((EvalError) ex.getTarget()).getErrorLineNumber();
        }
        return 1;
    }

    private int extractLine(String raw) {
        if (raw == null) {
            return 1;
        }
        int at = raw.indexOf("line ");
        if (at < 0) {
            return 1;
        }
        return parseNumber(raw, at + 5);
    }

    private int extractColumn(String raw) {
        if (raw == null) {
            return 1;
        }
        int at = raw.indexOf("column ");
        if (at < 0) {
            return 1;
        }
        return parseNumber(raw, at + 7);
    }

    private int parseNumber(String text, int start) {
        int i = start;
        while (i < text.length() && !Character.isDigit(text.charAt(i))) {
            i++;
        }
        int from = i;
        while (i < text.length() && Character.isDigit(text.charAt(i))) {
            i++;
        }
        if (from == i) {
            return 1;
        }
        try {
            return Integer.parseInt(text.substring(from, i));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private String unwrapSingleTopLevelClass(String script) {
        int packageEnd = skipPackageDeclaration(script, 0);
        int searchFrom = skipImports(script, packageEnd);
        ClassBlock classBlock = findSingleTopLevelClass(script, searchFrom);
        if (classBlock == null) {
            return null;
        }
        if (findSingleTopLevelClass(script, classBlock.bodyEnd + 1) != null) {
            return null;
        }
        int classModifiersStart = findClassModifiersStart(script, classBlock.classStart);
        String prefix = script.substring(0, classModifiersStart);
        String suffix = script.substring(classBlock.bodyEnd + 1);
        if (containsTopLevelTypeDeclaration(classBlock.body)) {
            return null;
        }
        if (containsNonWhitespace(suffix)) {
            return null;
        }
        String body = classBlock.body;
        if (containsFieldDeclaration(body)) {
            body = transformFieldDeclarations(body);
        }
        return prefix + body;
    }

    private int findClassModifiersStart(String script, int classKeywordPos) {
        int i = classKeywordPos - 1;
        while (i >= 0 && Character.isWhitespace(script.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return 0;
        }
        int modifiersEnd = i + 1;
        int modifiersStart = modifiersEnd;
        while (i >= 0) {
            while (i >= 0 && Character.isWhitespace(script.charAt(i))) {
                i--;
            }
            if (i < 0) {
                break;
            }
            int wordEnd = i + 1;
            while (i >= 0 && isIdentifierPart(script.charAt(i))) {
                i--;
            }
            int wordStart = i + 1;
            String word = script.substring(wordStart, wordEnd);
            if (!isClassModifier(word)) {
                break;
            }
            modifiersStart = wordStart;
            i--;
        }
        return modifiersStart;
    }

    private boolean isClassModifier(String word) {
        return word.equals("public") || word.equals("private") || word.equals("protected")
                || word.equals("static") || word.equals("final") || word.equals("abstract")
                || word.equals("strictfp");
    }

    private String transformFieldDeclarations(String body) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < body.length()) {
            int stmtStart = i;
            int depth = 0;
            while (i < body.length()) {
                char ch = body.charAt(i);
                if (ch == '"' || ch == '\'') {
                    i = skipQuoted(body, i) + 1;
                    continue;
                }
                if (startsLineComment(body, i)) {
                    i = skipLineComment(body, i) + 1;
                    continue;
                }
                if (startsBlockComment(body, i)) {
                    i = skipBlockComment(body, i) + 1;
                    continue;
                }
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                } else if (depth == 0 && ch == ';') {
                    int end = i;
                    String stmt = body.substring(stmtStart, end + 1).trim();
                    String transformed = transformFieldStatement(stmt);
                    if (transformed != null) {
                        out.append(transformed);
                    } else {
                        out.append(stmt);
                    }
                    i++;
                    stmtStart = i;
                    continue;
                }
                i++;
            }
            if (stmtStart < i) {
                out.append(body.substring(stmtStart));
            }
            break;
        }
        return out.toString();
    }

    private String transformFieldStatement(String stmt) {
        if (!startsWithAnyWord(stmt, 0, "public", "private", "protected", "static", "final")) {
            return null;
        }
        int next = skipModifiers(stmt, 0);
        if (next >= stmt.length()) {
            return null;
        }
        if (isMethodDeclaration(stmt, next)) {
            return null;
        }
        String afterModifiers = stmt.substring(next).trim();
        if (afterModifiers.startsWith("class ")
                || afterModifiers.startsWith("interface ")
                || afterModifiers.startsWith("enum ")) {
            return null;
        }
        int typeEnd = findTypeEnd(afterModifiers);
        if (typeEnd < 0) {
            return null;
        }
        String rest = afterModifiers.substring(typeEnd).trim();
        int eqPos = rest.indexOf('=');
        String varName;
        String initValue = "null";
        if (eqPos >= 0) {
            varName = rest.substring(0, eqPos).trim();
            initValue = rest.substring(eqPos + 1, rest.length() - 1).trim();
        } else {
            varName = rest.substring(0, rest.length() - 1).trim();
        }
        int bracketPos = varName.indexOf('[');
        if (bracketPos >= 0) {
            varName = varName.substring(0, bracketPos).trim();
        }
        if (varName.isEmpty() || !isIdentifierStart(varName.charAt(0))) {
            return null;
        }
        return varName + " = " + initValue + ";\n";
    }

    private int findTypeEnd(String text) {
        int i = skipWhitespace(text, 0);
        if (i >= text.length() || !isIdentifierStart(text.charAt(i))) {
            return -1;
        }
        while (i < text.length() && isIdentifierPart(text.charAt(i))) {
            i++;
        }
        int typeEnd = i;
        int afterType = skipWhitespace(text, i);
        while (afterType < text.length() && (text.charAt(afterType) == '[' || text.charAt(afterType) == ']')) {
            if (text.charAt(afterType) == '[') {
                int j = afterType + 1;
                while (j < text.length() && Character.isWhitespace(text.charAt(j))) {
                    j++;
                }
                if (j < text.length() && text.charAt(j) == ']') {
                    afterType = j + 1;
                } else {
                    break;
                }
            } else {
                afterType++;
            }
            afterType = skipWhitespace(text, afterType);
        }
        int nameStart = afterType;
        if (nameStart >= text.length() || !isIdentifierStart(text.charAt(nameStart))) {
            return -1;
        }
        int nameEnd = nameStart;
        while (nameEnd < text.length() && isIdentifierPart(text.charAt(nameEnd))) {
            nameEnd++;
        }
        int afterName = skipWhitespace(text, nameEnd);
        while (afterName < text.length() && (text.charAt(afterName) == '[' || text.charAt(afterName) == ']')) {
            afterName++;
            afterName = skipWhitespace(text, afterName);
        }
        if (afterName < text.length() && (text.charAt(afterName) == '=' || text.charAt(afterName) == ';')) {
            return typeEnd;
        }
        return -1;
    }

    private int skipPackageDeclaration(String script, int start) {
        int i = skipWhitespace(script, start);
        if (!startsWithWord(script, i, "package")) {
            return start;
        }
        int semi = findStatementEnd(script, i);
        return semi < 0 ? start : semi + 1;
    }

    private int skipImports(String script, int start) {
        int i = start;
        while (true) {
            i = skipWhitespace(script, i);
            if (!startsWithWord(script, i, "import")) {
                return i;
            }
            int semi = findStatementEnd(script, i);
            if (semi < 0) {
                return i;
            }
            i = semi + 1;
        }
    }

    private int findStatementEnd(String script, int start) {
        int len = script.length();
        for (int i = start; i < len; i++) {
            char ch = script.charAt(i);
            if (ch == ';') {
                return i;
            }
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
            } else if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
            } else if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
            }
        }
        return -1;
    }

    private ClassBlock findSingleTopLevelClass(String script, int start) {
        int depth = 0;
        int len = script.length();
        for (int i = start; i < len; i++) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
                continue;
            }
            if (ch == '{') {
                depth++;
                continue;
            }
            if (ch == '}') {
                depth--;
                continue;
            }
            if (depth == 0 && startsWithWord(script, i, "class")) {
                return extractClassBlock(script, i);
            }
        }
        return null;
    }

    private ClassBlock extractClassBlock(String script, int classKeyword) {
        int headerEnd = findOpeningBrace(script, classKeyword);
        if (headerEnd < 0) {
            return null;
        }
        int bodyEnd = findMatchingBrace(script, headerEnd);
        if (bodyEnd < 0) {
            return null;
        }
        return new ClassBlock(classKeyword, headerEnd + 1, bodyEnd, script.substring(headerEnd + 1, bodyEnd));
    }

    private int findOpeningBrace(String script, int start) {
        int len = script.length();
        for (int i = start; i < len; i++) {
            char ch = script.charAt(i);
            if (ch == '{') {
                return i;
            }
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
            } else if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
            } else if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
            }
        }
        return -1;
    }

    private int findMatchingBrace(String script, int openBrace) {
        int depth = 1;
        int len = script.length();
        for (int i = openBrace + 1; i < len; i++) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean containsTopLevelTypeDeclaration(String body) {
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(body, i);
                continue;
            }
            if (startsLineComment(body, i)) {
                i = skipLineComment(body, i);
                continue;
            }
            if (startsBlockComment(body, i)) {
                i = skipBlockComment(body, i);
                continue;
            }
            if (ch == '{') {
                depth++;
                continue;
            }
            if (ch == '}') {
                depth--;
                continue;
            }
            if (depth == 0 && (startsWithWord(body, i, "class")
                    || startsWithWord(body, i, "interface")
                    || startsWithWord(body, i, "enum"))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFieldDeclaration(String body) {
        int depth = 0;
        int i = 0;
        while (i < body.length()) {
            char ch = body.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(body, i) + 1;
                continue;
            }
            if (startsLineComment(body, i)) {
                i = skipLineComment(body, i) + 1;
                continue;
            }
            if (startsBlockComment(body, i)) {
                i = skipBlockComment(body, i) + 1;
                continue;
            }
            if (ch == '{') {
                depth++;
                i++;
                continue;
            }
            if (ch == '}') {
                depth--;
                i++;
                continue;
            }
            if (depth == 0 && ch == ';') {
                int stmtStart = findStatementStartBackwards(body, i);
                if (isFieldDeclaration(body, stmtStart, i)) {
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    private int findStatementStartBackwards(String text, int semiPos) {
        int depth = 0;
        for (int i = semiPos - 1; i >= 0; i--) {
            char ch = text.charAt(i);
            if (ch == '}') {
                depth++;
            } else if (ch == '{') {
                if (depth == 0) {
                    return i + 1;
                }
                depth--;
            } else if (ch == ')') {
                depth++;
            } else if (ch == '(') {
                depth--;
            } else if (ch == '"' || ch == '\'') {
                i = skipQuotedBackwards(text, i);
            } else if (depth == 0 && ch == ';') {
                return i + 1;
            }
        }
        return 0;
    }

    private int skipQuotedBackwards(String text, int endQuotePos) {
        char quote = text.charAt(endQuotePos);
        int i = endQuotePos - 1;
        while (i >= 0) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                i--;
            } else if (ch == quote) {
                return i;
            }
            i--;
        }
        return 0;
    }

    private boolean isFieldDeclaration(String text, int start, int end) {
        String stmt = text.substring(start, end + 1).trim();
        if (stmt.length() == 0) {
            return false;
        }
        if (startsWithAnyWord(stmt, 0, "public", "private", "protected", "static", "final")) {
            int next = skipModifiers(stmt, 0);
            if (next >= stmt.length()) {
                return false;
            }
            if (isMethodDeclaration(stmt, next)) {
                return false;
            }
            if (stmt.substring(next).trim().startsWith("class ")
                    || stmt.substring(next).trim().startsWith("interface ")
                    || stmt.substring(next).trim().startsWith("enum ")) {
                return false;
            }
            return true;
        }
        return false;
    }

    private int skipModifiers(String text, int start) {
        int i = skipWhitespace(text, start);
        while (i < text.length()) {
            if (startsWithWord(text, i, "public")
                    || startsWithWord(text, i, "private")
                    || startsWithWord(text, i, "protected")
                    || startsWithWord(text, i, "static")
                    || startsWithWord(text, i, "final")
                    || startsWithWord(text, i, "abstract")
                    || startsWithWord(text, i, "synchronized")
                    || startsWithWord(text, i, "volatile")
                    || startsWithWord(text, i, "transient")
                    || startsWithWord(text, i, "native")
                    || startsWithWord(text, i, "strictfp")) {
                i = skipWhitespace(text, i + indexOfWordEnd(text, i));
            } else {
                break;
            }
        }
        return i;
    }

    private int indexOfWordEnd(String text, int start) {
        int i = start;
        while (i < text.length() && isIdentifierPart(text.charAt(i))) {
            i++;
        }
        return i - start;
    }

    private boolean isMethodDeclaration(String text, int start) {
        int i = skipWhitespace(text, start);
        while (i < text.length() && isIdentifierPart(text.charAt(i))) {
            i++;
        }
        i = skipWhitespace(text, i);
        while (i < text.length() && (text.charAt(i) =='['|| text.charAt(i) == ']')) {
            i++;
            i = skipWhitespace(text, i);
        }
        if (i >= text.length() || !isIdentifierStart(text.charAt(i))) {
            return false;
        }
        while (i < text.length() && isIdentifierPart(text.charAt(i))) {
            i++;
        }
        i = skipWhitespace(text, i);
        while (i < text.length() && (text.charAt(i) == '[' || text.charAt(i) == ']')) {
            i++;
            i = skipWhitespace(text, i);
        }
        if (i >= text.length() || text.charAt(i) != '(') {
            return false;
        }
        return true;
    }

    private boolean startsWithWord(String text, int index, String word) {
        int end = index + word.length();
        if (index < 0 || end > text.length() || !text.regionMatches(index, word, 0, word.length())) {
            return false;
        }
        return (index == 0 || !isIdentifierPart(text.charAt(index - 1)))
                && (end == text.length() || !isIdentifierPart(text.charAt(end)));
    }

    private int skipWhitespace(String text, int index) {
        int i = index;
        while (i < text.length()) {
            if (Character.isWhitespace(text.charAt(i))) {
                i++;
            } else if (startsLineComment(text, i)) {
                i = skipLineComment(text, i) + 1;
            } else if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i) + 1;
            } else {
                break;
            }
        }
        return i;
    }

    private boolean startsLineComment(String text, int index) {
        return index + 1 < text.length() && text.charAt(index) == '/' && text.charAt(index + 1) == '/';
    }

    private boolean startsBlockComment(String text, int index) {
        return index + 1 < text.length() && text.charAt(index) == '/' && text.charAt(index + 1) == '*';
    }

    private int skipLineComment(String text, int index) {
        int i = index + 2;
        while (i < text.length() && text.charAt(i) != '\n') {
            i++;
        }
        return i;
    }

    private int skipBlockComment(String text, int index) {
        int i = index + 2;
        while (i + 1 < text.length()) {
            if (text.charAt(i) == '*' && text.charAt(i + 1) == '/') {
                return i + 1;
            }
            i++;
        }
        return text.length() - 1;
    }

    private int skipQuoted(String text, int index) {
        char quote = text.charAt(index);
        int i = index + 1;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                i += 2;
                continue;
            }
            if (ch == quote) {
                return i;
            }
            i++;
        }
        return text.length() - 1;
    }

    private boolean containsNonWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isIdentifierPart(char ch) {
        return (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9')
                || ch == '_'
                || ch == '$';
    }

    private String wrapLooseScript(String script) {
        int packageEnd = skipPackageDeclaration(script, 0);
        int bodyStart = skipImports(script, packageEnd);
        if (findSingleTopLevelClass(script, bodyStart) != null || containsTopLevelCallableDeclaration(script, bodyStart)) {
            return null;
        }
        String prefix = script.substring(0, bodyStart);
        String body = script.substring(bodyStart);
        String rewrittenBody = rewriteLooseScriptBody(body);
        return prefix
                + "Component build(PlaygroundContext ctx) {\n"
                + rewrittenBody
                + "\n}\n"
                + "build(ctx);";
    }

    private boolean containsTopLevelCallableDeclaration(String script, int start) {
        int depth = 0;
        for (int i = start; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
                continue;
            }
            if (ch == '{') {
                depth++;
                continue;
            }
            if (ch == '}') {
                depth--;
                continue;
            }
            if (depth != 0 || !isIdentifierStart(ch)) {
                continue;
            }
            int openParen = findNextNonWhitespace(script, i);
            while (openParen < script.length() && script.charAt(openParen) != '(' && script.charAt(openParen) != ';'
                    && script.charAt(openParen) != '{' && script.charAt(openParen) != '}') {
                if (script.charAt(openParen) == '"' || script.charAt(openParen) == '\'') {
                    openParen = skipQuoted(script, openParen) + 1;
                    continue;
                }
                openParen++;
            }
            if (openParen >= script.length() || script.charAt(openParen) != '(') {
                continue;
            }
            int closeParen = findMatchingParen(script, openParen);
            if (closeParen < 0) {
                return false;
            }
            int next = skipWhitespace(script, closeParen + 1);
            if (next < script.length() && script.charAt(next) == '{') {
                return true;
            }
        }
        return false;
    }

    private String rewriteLooseScriptBody(String body) {
        int lastSemi = findLastTopLevelSemicolon(body);
        if (lastSemi < 0) {
            String trimmed = body.trim();
            if (trimmed.length() == 0) {
                return "return null;";
            }
            if (looksLikeReturnStatement(trimmed)) {
                return body;
            }
            return "return " + trimmed + ";";
        }
        int statementStart = findStatementStart(body, lastSemi);
        String leading = body.substring(0, statementStart);
        String statement = body.substring(statementStart, lastSemi).trim();
        String trailing = body.substring(lastSemi + 1);
        if (statement.length() == 0) {
            return body + "\nreturn null;";
        }
        if (looksLikeReturnStatement(statement)) {
            return body;
        }
        if (looksLikeReturnableExpression(statement)) {
            return leading + "return " + statement + ";" + trailing;
        }
        return body + "\nreturn null;";
    }

    private int findLastTopLevelSemicolon(String text) {
        int depth = 0;
        int last = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            } else if (ch == ';' && depth == 0) {
                last = i;
            }
        }
        return last;
    }

    private int findStatementStart(String text, int endExclusive) {
        int depth = 0;
        int last = 0;
        for (int i = 0; i < endExclusive; i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            } else if (ch == ';' && depth == 0) {
                last = i + 1;
            }
        }
        return last;
    }

    private boolean looksLikeReturnStatement(String statement) {
        return startsWithWord(statement, skipWhitespace(statement, 0), "return");
    }

    private boolean looksLikeReturnableExpression(String statement) {
        int start = skipWhitespace(statement, 0);
        if (start >= statement.length()) {
            return false;
        }
        if (startsWithAnyWord(statement, start, "if", "for", "while", "switch", "try", "catch", "finally",
                "do", "class", "interface", "enum", "throw", "break", "continue", "public", "private",
                "protected", "static", "final", "abstract", "synchronized")) {
            return false;
        }
        return !containsTopLevelAssignment(statement);
    }

    private boolean containsTopLevelAssignment(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
                continue;
            }
            if (ch == ')' || ch == ']' || ch == '}') {
                depth--;
                continue;
            }
            if (depth == 0 && ch == '=') {
                char before = i > 0 ? text.charAt(i - 1) : 0;
                char after = i + 1 < text.length() ? text.charAt(i + 1) : 0;
                if (before != '=' && before != '!' && before != '<' && before != '>' && after != '=') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean startsWithAnyWord(String text, int index, String... words) {
        for (int i = 0; i < words.length; i++) {
            if (startsWithWord(text, index, words[i])) {
                return true;
            }
        }
        return false;
    }

    private String rewriteLambdaArguments(String script) {
        StringBuilder out = new StringBuilder();
        int last = 0;
        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
                continue;
            }
            if (ch != '(') {
                continue;
            }
            int close = findMatchingParen(script, i);
            if (close < 0) {
                break;
            }
            String inner = script.substring(i + 1, close);
            String rewrittenInner = rewriteLambdaSegments(rewriteLambdaArguments(inner));
            out.append(script, last, i + 1);
            out.append(rewrittenInner);
            last = close;
            i = close;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private String rewriteKnownSamCalls(String script) {
        String rewritten = rewriteKnownSamCalls(script,
                "addActionListener",
                "actionListener");
        rewritten = rewriteKnownSamCalls(rewritten,
                "addResponseListener",
                "networkListener");
        rewritten = rewriteKnownSamCalls(rewritten,
                "callSerially",
                "runnable");
        rewritten = rewriteKnownSamCalls(rewritten,
                "callSeriallyAndWait",
                "runnable");
        rewritten = rewriteKnownSamCalls(rewritten,
                "fetchAsString",
                "onComplete");
        return rewritten;
    }

    private String rewriteKnownSamCalls(String script, String methodName, String factoryMethod) {
        String marker = "." + methodName + "(";
        StringBuilder out = new StringBuilder();
        int last = 0;
        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i);
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i);
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i);
                continue;
            }
            if (!script.startsWith(marker, i)) {
                continue;
            }
            int open = i + marker.length() - 1;
            int close = findMatchingParen(script, open);
            if (close < 0) {
                break;
            }
            String args = script.substring(open + 1, close);
            String rewrittenArgs = rewriteKnownSamArgument(args, factoryMethod);
            out.append(script, last, open + 1);
            out.append(rewrittenArgs == null ? args : rewrittenArgs);
            last = close;
            i = close;
        }
        out.append(script.substring(last));
        return out.toString();
    }

    private String rewriteKnownSamArgument(String args, String factoryMethod) {
        String rewrittenLambda = rewriteLambdaToAnonymousSam(args, factoryMethod);
        if (rewrittenLambda != null) {
            return rewrittenLambda;
        }
        return rewriteAnonymousSamToAnonymousSam(args, factoryMethod);
    }

    private String rewriteLambdaToAnonymousSam(String segment, String factoryMethod) {
        int arrow = findTopLevelArrow(segment);
        if (arrow < 0) {
            return null;
        }
        String[] params = parseLambdaParameters(segment.substring(0, arrow));
        if (params == null) {
            return null;
        }
        String body = normalizeLambdaBody(segment.substring(arrow + 2));
        if (body == null) {
            return null;
        }
        body = rewriteKnownSamCalls(rewriteLambdaSegments(rewriteLambdaArguments(body)));
        return listenerFactoryExpression(factoryMethod, params, body);
    }

    private String rewriteAnonymousSamToAnonymousSam(String segment, String factoryMethod) {
        String trimmed = segment.trim();
        if (!trimmed.startsWith("new ")) {
            return null;
        }
        int openParen = findTopLevelChar(trimmed, '(', 4);
        if (openParen < 0) {
            return null;
        }
        int closeParen = findMatchingParen(trimmed, openParen);
        if (closeParen < 0 || containsNonWhitespace(trimmed.substring(openParen + 1, closeParen))) {
            return null;
        }
        int bodyStart = skipWhitespace(trimmed, closeParen + 1);
        if (bodyStart >= trimmed.length() || trimmed.charAt(bodyStart) != '{') {
            return null;
        }
        int bodyEnd = findMatchingBrace(trimmed, bodyStart);
        if (bodyEnd != trimmed.length() - 1) {
            return null;
        }
        AnonymousSam anonymousSam = parseAnonymousSamBody(trimmed.substring(bodyStart + 1, bodyEnd));
        if (anonymousSam == null) {
            return null;
        }
        String rewrittenBody = rewriteKnownSamCalls(rewriteLambdaSegments(rewriteLambdaArguments(anonymousSam.bodySource)));
        return listenerFactoryExpression(factoryMethod, anonymousSam.parameterNames, rewrittenBody);
    }

    private String listenerFactoryExpression(String factoryMethod, String[] params, String body) {
        return "__listenerSupport." + factoryMethod + "(" + lambdaPlaceholder(params, body) + ")";
    }

    private String rewriteLambdaSegments(String text) {
        StringBuilder out = new StringBuilder();
        int start = 0;
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == ']' || ch == '}') {
                depth--;
            } else if (ch == ',' && depth == 0) {
                appendLambdaSegment(out, text.substring(start, i));
                out.append(',');
                start = i + 1;
            }
        }
        appendLambdaSegment(out, text.substring(start));
        return out.toString();
    }

    private void appendLambdaSegment(StringBuilder out, String segment) {
        String rewritten = rewriteLambdaExpression(segment);
        out.append(rewritten == null ? segment : rewritten);
    }

    private String rewriteLambdaExpression(String segment) {
        int arrow = findTopLevelArrow(segment);
        if (arrow < 0) {
            return null;
        }
        String[] params = parseLambdaParameters(segment.substring(0, arrow));
        if (params == null) {
            return null;
        }
        String body = normalizeLambdaBody(segment.substring(arrow + 2));
        if (body == null) {
            return null;
        }
        body = rewriteKnownSamCalls(rewriteLambdaSegments(rewriteLambdaArguments(body)));
        return lambdaPlaceholder(params, body);
    }

    private String rewriteAnonymousSamExpression(String segment) {
        String trimmed = segment.trim();
        if (!trimmed.startsWith("new ")) {
            return null;
        }
        int typeStart = 4;
        int openParen = findTopLevelChar(trimmed, '(', typeStart);
        if (openParen < 0) {
            return null;
        }
        int closeParen = findMatchingParen(trimmed, openParen);
        if (closeParen < 0 || containsNonWhitespace(trimmed.substring(openParen + 1, closeParen))) {
            return null;
        }
        int bodyStart = skipWhitespace(trimmed, closeParen + 1);
        if (bodyStart >= trimmed.length() || trimmed.charAt(bodyStart) != '{') {
            return null;
        }
        int bodyEnd = findMatchingBrace(trimmed, bodyStart);
        if (bodyEnd != trimmed.length() - 1) {
            return null;
        }
        AnonymousSam anonymousSam = parseAnonymousSamBody(trimmed.substring(bodyStart + 1, bodyEnd));
        if (anonymousSam == null) {
            return null;
        }
        return lambdaPlaceholder(anonymousSam.parameterNames, anonymousSam.bodySource);
    }

    private AnonymousSam parseAnonymousSamBody(String body) {
        int i = skipWhitespace(body, 0);
        if (!startsWithWord(body, i, "public")) {
            return null;
        }
        i = skipWhitespace(body, i + "public".length());
        int openParen = findTopLevelChar(body, '(', i);
        if (openParen < 0) {
            return null;
        }
        int closeParen = findMatchingParen(body, openParen);
        if (closeParen < 0) {
            return null;
        }
        int nameEnd = openParen;
        int nameStart = nameEnd - 1;
        while (nameStart >= i && isIdentifierPart(body.charAt(nameStart))) {
            nameStart--;
        }
        nameStart++;
        if (nameStart >= nameEnd) {
            return null;
        }
        int methodBodyStart = skipWhitespace(body, closeParen + 1);
        if (methodBodyStart >= body.length() || body.charAt(methodBodyStart) != '{') {
            return null;
        }
        int methodBodyEnd = findMatchingBrace(body, methodBodyStart);
        if (methodBodyEnd < 0 || containsNonWhitespace(body.substring(methodBodyEnd + 1))) {
            return null;
        }
        String[] params = parseAnonymousMethodParameters(body.substring(openParen + 1, closeParen));
        if (params == null) {
            return null;
        }
        return new AnonymousSam(params, body.substring(methodBodyStart + 1, methodBodyEnd).trim());
    }

    private String[] parseAnonymousMethodParameters(String raw) {
        String parameterList = raw.trim();
        if (parameterList.length() == 0) {
            return new String[0];
        }
        String[] rawParts = splitTopLevel(parameterList, ',');
        String[] out = new String[rawParts.length];
        for (int i = 0; i < rawParts.length; i++) {
            String candidate = rawParts[i].trim();
            if (candidate.length() == 0) {
                return null;
            }
            int end = candidate.length() - 1;
            while (end >= 0 && Character.isWhitespace(candidate.charAt(end))) {
                end--;
            }
            int start = end;
            while (start >= 0 && isIdentifierPart(candidate.charAt(start))) {
                start--;
            }
            start++;
            if (start > end || !isIdentifierStart(candidate.charAt(start))) {
                return null;
            }
            out[i] = candidate.substring(start, end + 1);
        }
        return out;
    }

    private String lambdaPlaceholder(String[] params, String body) {
        return "__lambdaSupport.lambda(" + toStringArrayLiteral(params) + ", " + toJavaStringLiteral(body) + ")";
    }

    /**
     * Rewrites lambdas that appear outside a method-argument list (e.g.
     * {@code Runnable r = () -> {};} or {@code return x -> x + 1;}). BeanShell's
     * parser does not recognise the lambda syntax itself, so any lambda we
     * don't pre-rewrite produces a parse error. {@link #rewriteLambdaArguments}
     * handles lambdas inside {@code (...)} call sites; this pass covers the
     * remaining statement-level occurrences.
     */
    private String rewriteTopLevelLambdas(String script) {
        while (true) {
            int arrow = findTopLevelArrow(script);
            if (arrow < 0) {
                return script;
            }
            int paramStart = findLambdaParamStart(script, arrow);
            if (paramStart < 0) {
                return script;
            }
            int bodyEnd = findLambdaBodyEnd(script, arrow + 2);
            if (bodyEnd < 0) {
                return script;
            }
            String params = script.substring(paramStart, arrow);
            String body = script.substring(arrow + 2, bodyEnd);
            String[] paramNames = parseLambdaParameters(params);
            if (paramNames == null) {
                return script;
            }
            // Rewrite lambdas that appear in the body BEFORE normalization
            // (which prepends `return`, creating non-param prefix text that
            // would confuse rewriteLambdaExpression). This handles nested
            // top-level lambdas like `a -> b -> a + b`.
            String recursedBody = rewriteTopLevelLambdas(body);
            // Rewrite switch expressions / arrow switches inside the
            // lambda body too — the body is eval'd as a standalone script
            // at invocation time and otherwise wouldn't get those passes.
            recursedBody = rewriteSwitchExpressions(recursedBody);
            recursedBody = rewriteArrowSwitchStatements(recursedBody);
            String bodyText = normalizeLambdaBody(recursedBody);
            if (bodyText == null) {
                return script;
            }
            String rewrittenBody = rewriteKnownSamCalls(
                    rewriteLambdaSegments(rewriteLambdaArguments(bodyText)));
            String placeholder = lambdaPlaceholder(paramNames, rewrittenBody);
            script = script.substring(0, paramStart) + placeholder + script.substring(bodyEnd);
        }
    }

    private int findLambdaParamStart(String script, int arrow) {
        int i = arrow - 1;
        while (i >= 0 && Character.isWhitespace(script.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return -1;
        }
        if (script.charAt(i) == ')') {
            int depth = 1;
            int j = i - 1;
            while (j >= 0 && depth > 0) {
                char ch = script.charAt(j);
                if (ch == '"' || ch == '\'') {
                    int openQuote = findOpeningQuote(script, j, ch);
                    if (openQuote < 0) {
                        return -1;
                    }
                    j = openQuote - 1;
                    continue;
                }
                if (ch == ')') {
                    depth++;
                } else if (ch == '(') {
                    depth--;
                    if (depth == 0) {
                        return j;
                    }
                }
                j--;
            }
            return -1;
        }
        int end = i + 1;
        int start = i;
        while (start > 0 && isIdentifierPart(script.charAt(start - 1))) {
            start--;
        }
        if (start >= end) {
            return -1;
        }
        if (!isIdentifierStart(script.charAt(start))) {
            return -1;
        }
        return start;
    }

    /** Body terminates at the next top-level statement/expression boundary. */
    private int findLambdaBodyEnd(String script, int bodyStart) {
        int i = skipWhitespace(script, bodyStart);
        if (i >= script.length()) {
            return -1;
        }
        if (script.charAt(i) == '{') {
            int close = findMatchingBrace(script, i);
            return close < 0 ? -1 : close + 1;
        }
        int depth = 0;
        while (i < script.length()) {
            char ch = script.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(script, i) + 1;
                continue;
            }
            if (startsLineComment(script, i)) {
                i = skipLineComment(script, i) + 1;
                continue;
            }
            if (startsBlockComment(script, i)) {
                i = skipBlockComment(script, i) + 1;
                continue;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == ']' || ch == '}') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            } else if (depth == 0 && (ch == ';' || ch == ',')) {
                return i;
            }
            i++;
        }
        return i;
    }

    private int findOpeningQuote(String script, int closeIndex, char quote) {
        int i = closeIndex - 1;
        while (i >= 0) {
            if (script.charAt(i) == quote) {
                int escapes = 0;
                int k = i - 1;
                while (k >= 0 && script.charAt(k) == '\\') {
                    escapes++;
                    k--;
                }
                if ((escapes % 2) == 0) {
                    return i;
                }
            }
            i--;
        }
        return -1;
    }

    private int findTopLevelArrow(String text) {
        int depth = 0;
        for (int i = 0; i + 1 < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
                continue;
            }
            if (ch == ')' || ch == ']' || ch == '}') {
                depth--;
                continue;
            }
            if (depth == 0 && ch == '-' && text.charAt(i + 1) == '>') {
                return i;
            }
        }
        return -1;
    }

    private int findTopLevelChar(String text, char expected, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (depth == 0 && ch == expected) {
                return i;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == ']' || ch == '}') {
                depth--;
            }
        }
        return -1;
    }

    private String[] parseLambdaParameters(String raw) {
        String parameterList = raw.trim();
        if (parameterList.length() == 0) {
            return new String[0];
        }
        if (parameterList.startsWith("(") && parameterList.endsWith(")")) {
            parameterList = parameterList.substring(1, parameterList.length() - 1).trim();
        }
        if (parameterList.length() == 0) {
            return new String[0];
        }
        String[] rawParts = splitTopLevel(parameterList, ',');
        String[] out = new String[rawParts.length];
        for (int i = 0; i < rawParts.length; i++) {
            String candidate = rawParts[i].trim();
            if (candidate.length() == 0 || containsWhitespace(candidate) || !isIdentifierStart(candidate.charAt(0))) {
                return null;
            }
            for (int j = 1; j < candidate.length(); j++) {
                if (!isIdentifierPart(candidate.charAt(j))) {
                    return null;
                }
            }
            out[i] = candidate;
        }
        return out;
    }

    private String normalizeLambdaBody(String rawBody) {
        String body = rawBody.trim();
        if (body.length() == 0) {
            return null;
        }
        if (body.startsWith("{") && body.endsWith("}")) {
            return body.substring(1, body.length() - 1).trim();
        }
        return "return " + body + ";";
    }

    private String[] splitTopLevel(String text, char separator) {
        int depth = 0;
        int start = 0;
        java.util.List<String> values = new java.util.ArrayList<String>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == ']' || ch == '}') {
                depth--;
            } else if (depth == 0 && ch == separator) {
                values.add(text.substring(start, i));
                start = i + 1;
            }
        }
        values.add(text.substring(start));
        return values.toArray(new String[values.size()]);
    }

    private String toStringArrayLiteral(String[] values) {
        if (values == null || values.length == 0) {
            return "new String[0]";
        }
        StringBuilder out = new StringBuilder("new String[]{");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(toJavaStringLiteral(values[i]));
        }
        out.append('}');
        return out.toString();
    }

    private String toJavaStringLiteral(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
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
                    out.append(ch);
                    break;
            }
        }
        out.append('"');
        return out.toString();
    }

    private boolean containsWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private int findMatchingParen(String text, int openParen) {
        int depth = 1;
        for (int i = openParen + 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                i = skipQuoted(text, i);
                continue;
            }
            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i);
                continue;
            }
            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i);
                continue;
            }
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findNextNonWhitespace(String text, int index) {
        int i = index;
        while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
            if (text.charAt(i) == '"' || text.charAt(i) == '\'') {
                return i;
            }
            i++;
        }
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private boolean isIdentifierStart(char ch) {
        return (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || ch == '_'
                || ch == '$';
    }

    private static final class AnonymousSam {
        final String[] parameterNames;
        final String bodySource;

        AnonymousSam(String[] parameterNames, String bodySource) {
            this.parameterNames = parameterNames;
            this.bodySource = bodySource;
        }
    }

    private Component resolveComponent(Interpreter interpreter, Object value, PlaygroundContext context) throws EvalError {
        if (context != null && context.getShownForm() != null) {
            return context.getShownForm();
        }
        if (value != null && value != Primitive.VOID && value != Primitive.NULL) {
            return coerceToComponent(value, context);
        }

        NameSpace namespace = interpreter.getNameSpace();
        if (hasBuildMethod(namespace, context)) {
            return coerceToComponent(namespace.invokeMethod("build", new Object[]{context}, interpreter), context);
        }

        if (hasLifecycleMethods(namespace, context)) {
            return runLifecycle(namespace, interpreter, context);
        }

        throw new EvalError(
                "Script must return a com.codename1.ui.Component, define build(ctx), or define lifecycle methods such as init(Object) and start().",
                null, null);
    }

    private boolean hasLifecycleMethods(NameSpace namespace, PlaygroundContext context) {
        return hasMethod(namespace, "start")
                || hasInitMethod(namespace, context);
    }

    private Component runLifecycle(NameSpace namespace, Interpreter interpreter, PlaygroundContext context) throws EvalError {
        if (context != null) {
            context.clearShownForm();
        }
        if (hasMethod(namespace, "init", PlaygroundContext.class)) {
            namespace.invokeMethod("init", new Object[]{context}, interpreter);
        } else if (hasMethod(namespace, "init", Object.class)) {
            namespace.invokeMethod("init", new Object[]{context}, interpreter);
        }

        if (!hasMethod(namespace, "start")) {
            throw new EvalError("Lifecycle script defines init(Object) but is missing start().", null, null);
        }

        Form previousForm = Display.getInstance().getCurrent();
        Object startResult = namespace.invokeMethod("start", new Object[0], interpreter);
        Form shownForm = context == null ? null : context.getShownForm();
        if (shownForm == null) {
            shownForm = Display.getInstance().getCurrent();
        }
        if (shownForm != null && shownForm != previousForm
                && (context == null || shownForm != context.getHostForm())) {
            return shownForm;
        }

        if (startResult != null && startResult != Primitive.VOID && startResult != Primitive.NULL) {
            return coerceToComponent(startResult, context);
        }

        if (shownForm != null && (context == null || shownForm != context.getHostForm())) {
            return shownForm;
        }

        throw new EvalError(
                "Lifecycle start() did not show a Form and did not return a Component. Call form.show() in start() or return a Component.",
                null, null);
    }

    private boolean hasBuildMethod(NameSpace namespace, PlaygroundContext context) {
        return hasMethod(namespace, "build", PlaygroundContext.class)
                || hasMethod(namespace, "build", Object.class);
    }

    private boolean hasInitMethod(NameSpace namespace, PlaygroundContext context) {
        return hasMethod(namespace, "init", PlaygroundContext.class)
                || hasMethod(namespace, "init", Object.class);
    }

    private boolean hasMethod(NameSpace namespace, String name, Class<?>... signature) {
        try {
            return namespace.getMethod(name, signature, false) != null;
        } catch (bsh.UtilEvalError ex) {
            return false;
        }
    }

    private Component coerceToComponent(Object resolved, PlaygroundContext context) throws EvalError {
        if (resolved == null || resolved == Primitive.NULL || resolved == Primitive.VOID) {
            throw new EvalError("Script did not produce a previewable Component.", null, null);
        }
        if (resolved instanceof Form) {
            return (Form) resolved;
        }
        if (resolved instanceof Component) {
            return (Component) resolved;
        }
        throw new EvalError("Script produced " + resolved.getClass().getName() + " instead of a previewable Component.", null, null);
    }

    private String safeMessage(Throwable throwable) {
        if (throwable instanceof ParseException) {
            return formatParseMessage((ParseException) throwable);
        }
        if (throwable instanceof TokenMgrException) {
            return formatLexerMessage((TokenMgrException) throwable);
        }
        String best = null;
        Throwable current = throwable;
        while (current != null) {
            String candidate = current.getMessage();
            if (candidate != null && candidate.length() > 0) {
                candidate = simplifyMessage(candidate);
                if (candidate.indexOf("Generated ") >= 0
                        || candidate.indexOf("not implemented") >= 0
                        || candidate.indexOf("Class or variable not found:") >= 0
                        || candidate.indexOf("Script must return") >= 0
                        || candidate.indexOf("Lifecycle script") >= 0
                        || candidate.indexOf("did not return a Component") >= 0
                        || candidate.indexOf("Script must produce") >= 0
                        || candidate.indexOf("Script did not produce a previewable Component") >= 0
                        || candidate.indexOf("instead of a previewable Component") >= 0) {
                    return candidate;
                }
                best = candidate;
            }
            current = current.getCause();
        }
        if (best != null) {
            return best;
        }
        return throwable.getClass().getSimpleName();
    }

    private String simplifyMessage(String message) {
        int generated = message.indexOf("Generated ");
        if (generated >= 0) {
            return message.substring(generated);
        }
        int classMissing = message.indexOf("Class or variable not found:");
        if (classMissing >= 0) {
            return message.substring(classMissing);
        }
        return message;
    }

    private String formatParseMessage(ParseException ex) {
        return formatParseMessageWithSource(ex, null);
    }

    private String formatLexerMessage(TokenMgrException ex) {
        return formatLexerMessageWithSource(ex, null);
    }

    /** Build an enriched parse-error message: precise location,
     * offending token, a source-line excerpt with a caret under the
     * bad column, and a specific hint when we can infer one. Falls
     * back to BSH's own "Unable to parse code syntax" message when
     * the exception doesn't carry a {@code currentToken}. */
    private String formatParseMessageWithSource(ParseException ex, String source) {
        int line = -1;
        int column = -1;
        String encountered = null;
        try {
            if (ex.currentToken != null && ex.currentToken.next != null) {
                line = ex.currentToken.next.beginLine;
                column = ex.currentToken.next.beginColumn;
                encountered = tokenImage(ex.currentToken.next);
            } else {
                line = ex.getErrorLineNumber();
            }
        } catch (NullPointerException e) {
            // currentToken can be null on some parse paths
        }
        String raw = ex.getMessage();
        if (encountered == null) encountered = inferEncounteredToken(raw);

        StringBuilder out = new StringBuilder();
        out.append("Syntax error");
        if (line > 0) out.append(" at line ").append(line);
        if (column > 0) out.append(", column ").append(column);
        if (encountered != null && !encountered.isEmpty()) {
            out.append(" (unexpected ").append(encountered).append(")");
        }
        out.append('.');

        String excerpt = sourceExcerpt(source, line, column);
        if (excerpt != null) out.append('\n').append(excerpt);

        String hint = tokenAwareHint(encountered, raw);
        if (hint != null) out.append('\n').append(hint);
        return out.toString();
    }

    private String formatLexerMessageWithSource(TokenMgrException ex, String source) {
        String raw = ex.getMessage();
        int line = extractLine(raw);
        int column = extractColumn(raw);
        StringBuilder out = new StringBuilder("Lexer error");
        if (line > 0) out.append(" at line ").append(line);
        if (column > 0) out.append(", column ").append(column);
        out.append('.');
        String excerpt = sourceExcerpt(source, line, column);
        if (excerpt != null) out.append('\n').append(excerpt);
        String hint = tokenAwareHint(null, raw);
        if (hint != null) out.append('\n').append(hint);
        return out.toString();
    }

    /** Render an excerpt of the offending source line with a caret
     * marking the bad column. Returns {@code null} when no useful
     * line can be extracted. */
    private String sourceExcerpt(String source, int line, int column) {
        if (source == null || line <= 0) return null;
        String lineText = readLine(source, line);
        if (lineText == null) return null;
        // Trim only leading whitespace we won't need (keep alignment).
        StringBuilder out = new StringBuilder();
        out.append("  ").append(lineText);
        if (column > 0 && column <= lineText.length() + 1) {
            out.append('\n').append("  ");
            for (int i = 1; i < column; i++) out.append(' ');
            out.append('^');
        }
        return out.toString();
    }

    private String readLine(String source, int line) {
        int start = 0;
        int current = 1;
        while (current < line) {
            int nl = source.indexOf('\n', start);
            if (nl < 0) return null;
            start = nl + 1;
            current++;
        }
        int end = source.indexOf('\n', start);
        String raw = end < 0 ? source.substring(start) : source.substring(start, end);
        if (raw.endsWith("\r")) raw = raw.substring(0, raw.length() - 1);
        // Clamp overlong lines so the diagnostic stays readable.
        if (raw.length() > 200) raw = raw.substring(0, 200) + "…";
        return raw;
    }

    /** Pretty-print a Token's image. Strips leading/trailing
     * whitespace and wraps in double-quotes. */
    private String tokenImage(bsh.Token tok) {
        if (tok == null || tok.image == null) return null;
        String img = tok.image;
        if (img.isEmpty()) return "end of input";
        return "\"" + img + "\"";
    }

    /** Pull out the token from BSH's "Encountered: X at line ..." message. */
    private String inferEncounteredToken(String raw) {
        if (raw == null) return null;
        int key = raw.indexOf("Encountered:");
        if (key < 0) return null;
        int start = key + "Encountered:".length();
        int at = raw.indexOf(" at line", start);
        String span = (at > start ? raw.substring(start, at) : raw.substring(start)).trim();
        if (span.isEmpty()) return null;
        return "\"" + span + "\"";
    }

    /** Produce a targeted hint based on the token or raw message. */
    private String tokenAwareHint(String encountered, String raw) {
        if (raw != null && raw.indexOf("Encountered:  at line") >= 0) {
            return "  Hint: parser reached end-of-input. Check for a missing closing brace or bracket.";
        }
        if (encountered != null) {
            String tok = stripQuotes(encountered);
            if ("(".equals(tok) || ")".equals(tok)) {
                return "  Hint: mismatched parentheses. Count the '(' and ')' on this line.";
            }
            if ("{".equals(tok) || "}".equals(tok)) {
                return "  Hint: mismatched braces. Check the preceding block's opening/closing '{' and '}'.";
            }
            if (";".equals(tok)) {
                return "  Hint: unexpected semicolon — the previous expression may be incomplete.";
            }
            if (",".equals(tok)) {
                return "  Hint: unexpected comma — check argument lists or variable declarations.";
            }
            if (":".equals(tok)) {
                return "  Hint: a ':' typically appears in switch labels, ternary expressions, or labels.";
            }
            if ("else".equals(tok)) {
                return "  Hint: 'else' without a matching 'if' — check the previous block.";
            }
            if ("catch".equals(tok) || "finally".equals(tok)) {
                return "  Hint: '" + tok + "' must follow a complete 'try' block.";
            }
            if (tok.length() == 1 && !isLetterOrDigitAscii(tok.charAt(0))) {
                return "  Hint: '" + tok + "' isn't valid here. Check the preceding expression for a missing operator or keyword.";
            }
        }
        return "  Hint: check for mismatched braces, missing semicolons, or unsupported class syntax.";
    }

    private static String stripQuotes(String s) {
        if (s == null || s.length() == 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '"') out.append(c);
        }
        return out.toString();
    }

    private static boolean isLetterOrDigitAscii(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
    }

    private String extractLocationDetail(String raw) {
        if (raw == null) {
            return null;
        }
        int at = raw.indexOf("at line ");
        if (at < 0) {
            return null;
        }
        int in = raw.indexOf(" in:", at);
        String location = in >= 0 ? raw.substring(at, in) : raw.substring(at);
        int comma = location.indexOf(", column ");
        if (comma >= 0) {
            return "column " + location.substring(comma + 9).trim();
        }
        return null;
    }

    private String extractLexerLocation(String raw) {
        if (raw == null) {
            return null;
        }
        int at = raw.indexOf("at line ");
        if (at < 0) {
            return null;
        }
        return raw.substring(at);
    }

    private String parseHint(String raw) {
        if (raw == null) {
            return "Check for mismatched braces or unsupported syntax.";
        }
        if (raw.indexOf("Encountered:  at line") >= 0) {
            return "The parser hit end-of-file unexpectedly. Check for a missing closing brace or a malformed class wrapper.";
        }
        if (raw.indexOf("public class") >= 0 || raw.indexOf("class ") >= 0) {
            return "If you pasted a single app class wrapper, it should now be unwrapped automatically. Check for extra code outside the class body.";
        }
        return "Check for mismatched braces, missing semicolons, or unsupported class syntax.";
    }

    private static final class ClassBlock {
        final int classStart;
        final int bodyStart;
        final int bodyEnd;
        final String body;

        ClassBlock(int classStart, int bodyStart, int bodyEnd, String body) {
            this.classStart = classStart;
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
            this.body = body;
        }
    }

    private static final class TypeDeclarationBlock {
        final int start;
        final int end;

        TypeDeclarationBlock(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class ScriptPlan {
        final List<String> typeDeclarations;
        final String executableScript;

        ScriptPlan(List<String> typeDeclarations, String executableScript) {
            this.typeDeclarations = typeDeclarations;
            this.executableScript = executableScript;
        }
    }
}
