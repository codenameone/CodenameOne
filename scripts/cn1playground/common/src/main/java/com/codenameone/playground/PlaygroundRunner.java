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
                String adapted = adaptScript(script);
                Object result = interpreter.eval(adapted);
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
            return failure("Parse error: " + safeMessage(ex), errorLine, extractColumn(ex.getMessage()), inlineMessages);
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

    private String adaptScript(String script) {
        String adapted = unwrapSingleTopLevelClass(script);
        String normalized = adapted == null ? script : adapted;
        normalized = rewriteKnownSamCalls(normalized);
        normalized = rewriteLambdaArguments(normalized);
        String wrapped = wrapLooseScript(normalized);
        return wrapped == null ? normalized : wrapped;
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
        int line = -1;
        try {
            line = ex.getErrorLineNumber();
        } catch (NullPointerException e) {
            // currentToken can be null in some parse error cases
        }
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
