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
            try {
                Object result = interpreter.eval(adaptScript(script));
                Component component = resolveComponent(interpreter, result, context);
                inlineMessages.add(new InlineMessage(0, "Preview updated.", "success"));
                return new RunResult(component, Collections.<Diagnostic>emptyList(), inlineMessages);
            } finally {
                PlaygroundContext.clearCurrent();
            }
        } catch (ParseException ex) {
            return failure("Parse error: " + safeMessage(ex), ex.getErrorLineNumber(), extractColumn(ex.getMessage()), inlineMessages);
        } catch (TokenMgrException ex) {
            return failure("Lexer error: " + safeMessage(ex), extractLine(ex.getMessage()), extractColumn(ex.getMessage()), inlineMessages);
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
        interpreter.set("theme", context.getTheme());
        interpreter.set("hostForm", context.getHostForm());
        interpreter.set("previewRoot", context.getPreviewRoot());
        interpreter.set("Display", Display.getInstance());
        interpreter.set("UIManager", UIManager.getInstance());
        interpreter.set("FontImage", FontImage.class);
        interpreter.set("CN", com.codename1.ui.CN.class);
        interpreter.set("BoxLayout", BoxLayout.class);
        interpreter.set("BorderLayout", BorderLayout.class);
        interpreter.set("FlowLayout", FlowLayout.class);
        interpreter.set("GridLayout", GridLayout.class);
        interpreter.set("LayeredLayout", LayeredLayout.class);
        interpreter.set("Style", Style.class);
        namespace.importPackage("com.codename1.ui");
        namespace.importPackage("com.codename1.ui.layouts");
        namespace.importPackage("com.codename1.components");
        namespace.importPackage("com.codename1.ui.geom");
        namespace.importClass("com.codenameone.playground.PlaygroundContext");
    }

    private String adaptScript(String script) {
        String adapted = unwrapSingleTopLevelClass(script);
        return adapted == null ? script : adapted;
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
        String prefix = script.substring(0, classBlock.classStart);
        String suffix = script.substring(classBlock.bodyEnd + 1);
        if (containsTopLevelTypeDeclaration(classBlock.body)) {
            return null;
        }
        if (containsNonWhitespace(suffix)) {
            return null;
        }
        return prefix + classBlock.body;
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
        int line = ex.getErrorLineNumber();
        String raw = ex.getMessage();
        StringBuilder out = new StringBuilder();
        out.append("Syntax error");
        if (line > 0) {
            out.append(" at line ").append(line);
        }
        String detail = extractLocationDetail(raw);
        if (detail != null) {
            out.append(", ").append(detail);
        }
        String hint = parseHint(raw);
        if (hint != null) {
            out.append(". ").append(hint);
        }
        return out.toString();
    }

    private String formatLexerMessage(TokenMgrException ex) {
        String raw = ex.getMessage();
        StringBuilder out = new StringBuilder("Syntax error");
        String detail = extractLexerLocation(raw);
        if (detail != null) {
            out.append(" ").append(detail);
        }
        String hint = parseHint(raw);
        if (hint != null) {
            out.append(". ").append(hint);
        }
        return out.toString();
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
}
