package com.codenameone.playground;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeCompletionProvider;
import com.codename1.ui.CodeEditor;
import com.codename1.util.SuccessCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata-driven {@link CodeCompletionProvider} for the Playground's {@link CodeEditor}. It reuses the
 * same Codename One API metadata that the Monaco integration consumes (types, members, globals) to offer
 * member completion after {@code .}, plus global/type/keyword completion for Java, and UIID/state/property
 * completion for CSS. Completion runs synchronously in memory off the parsed metadata.
 */
final class PlaygroundCompletion implements CodeCompletionProvider {
    enum Mode { JAVA, CSS }

    private static final String[] JAVA_KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue",
            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "new", "package", "private",
            "protected", "public", "return", "short", "static", "super", "switch", "this", "throw", "throws",
            "try", "void", "volatile", "while", "true", "false", "null"
    };

    private static final String[] CSS_PROPERTIES = {
            "color", "background-color", "background", "font", "font-family", "font-size", "font-weight",
            "border", "border-radius", "padding", "margin", "width", "height", "min-width", "min-height",
            "max-width", "max-height", "text-align", "opacity", "cn1-border-type", "cn1-background-type",
            "cn1-derive", "display", "align-items", "justify-content"
    };

    private static final String[] CSS_STATES = {"pressed", "selected", "disabled", "unselected"};

    private final Mode mode;
    private final Map<String, String> globals = new HashMap<String, String>();
    private final Map<String, Map<String, Object>> types = new HashMap<String, Map<String, Object>>();
    private final Map<String, String> simpleToQualified = new HashMap<String, String>();
    private final List<String> simpleNames = new ArrayList<String>();
    private List<String> uiids = new ArrayList<String>();

    PlaygroundCompletion(Mode mode, String metadataJson, List<String> uiids) {
        this.mode = mode == null ? Mode.JAVA : mode;
        if (uiids != null) {
            this.uiids = new ArrayList<String>(uiids);
        }
        parse(metadataJson);
    }

    void setUiids(List<String> uiids) {
        this.uiids = uiids == null ? new ArrayList<String>() : new ArrayList<String>(uiids);
    }

    @SuppressWarnings("unchecked")
    private void parse(String metadataJson) {
        if (metadataJson == null || metadataJson.length() == 0) {
            return;
        }
        try {
            Map<String, Object> root = new JSONParser().parseJSON(new CharArrayReader(metadataJson.toCharArray()));
            Object g = root.get("globals");
            if (g instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) g).entrySet()) {
                    globals.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            Object t = root.get("types");
            if (t instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) t).entrySet()) {
                    String qualified = e.getKey();
                    if (!(e.getValue() instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> info = (Map<String, Object>) e.getValue();
                    types.put(qualified, info);
                    String simple = String.valueOf(info.get("simple"));
                    if (simple.length() > 0 && !"null".equals(simple)) {
                        if (!simpleToQualified.containsKey(simple)) {
                            simpleToQualified.put(simple, qualified);
                            simpleNames.add(simple);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void getCompletions(CodeEditor editor, String code, int cursor, SuccessCallback<List<CodeCompletion>> results) {
        if (code == null) {
            code = "";
        }
        if (cursor < 0) {
            cursor = 0;
        }
        if (cursor > code.length()) {
            cursor = code.length();
        }
        if (mode == Mode.CSS) {
            results.onSucess(cssCompletions(code, cursor));
        } else {
            results.onSucess(javaCompletions(code, cursor));
        }
    }

    private List<CodeCompletion> javaCompletions(String code, int cursor) {
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        int start = cursor;
        while (start > 0 && isIdChar(code.charAt(start - 1))) {
            start--;
        }
        String prefix = code.substring(start, cursor);
        // member access? the char just before the identifier is a dot
        if (start > 0 && code.charAt(start - 1) == '.') {
            String receiver = receiverToken(code, start - 1);
            String qualified = inferType(receiver, code);
            if (qualified != null) {
                addMembers(out, qualified, prefix);
            }
            return out;
        }
        String lower = prefix.toLowerCase();
        for (String name : globals.keySet()) {
            if (matches(name, lower)) {
                out.add(new CodeCompletion(name).setType("variable").setDetail(simpleOf(globals.get(name))));
            }
        }
        for (int i = 0; i < simpleNames.size() && out.size() < 60; i++) {
            String simple = simpleNames.get(i);
            if (matches(simple, lower)) {
                out.add(new CodeCompletion(simple).setType("class").setDetail(simpleToQualified.get(simple)));
            }
        }
        for (String kw : JAVA_KEYWORDS) {
            if (matches(kw, lower)) {
                out.add(new CodeCompletion(kw).setType("keyword"));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private void addMembers(List<CodeCompletion> out, String qualified, String prefix) {
        Map<String, Object> info = types.get(qualified);
        if (info == null) {
            return;
        }
        String lower = prefix.toLowerCase();
        Object methods = info.get("methods");
        if (methods instanceof List) {
            for (Object m : (List<Object>) methods) {
                String sig = String.valueOf(m);
                String name = methodName(sig);
                if (matches(name, lower) && out.size() < 80) {
                    boolean hasArgs = sig.indexOf('(') >= 0 && sig.indexOf("()") < 0;
                    out.add(new CodeCompletion(sig, name + "(").setType("method"));
                }
            }
        }
        Object fields = info.get("fields");
        if (fields instanceof List) {
            for (Object f : (List<Object>) fields) {
                String name = String.valueOf(f);
                if (matches(name, lower) && out.size() < 80) {
                    out.add(new CodeCompletion(name).setType("field"));
                }
            }
        }
    }

    /// Infers the qualified type of a receiver token: a known global, a static type reference (simple
    /// name), or a local variable declared earlier as "Type receiver" / "receiver = new Type(".
    private String inferType(String receiver, String code) {
        if (receiver == null || receiver.length() == 0) {
            return null;
        }
        if (globals.containsKey(receiver)) {
            return globals.get(receiver);
        }
        if (simpleToQualified.containsKey(receiver)) {
            return simpleToQualified.get(receiver);
        }
        // "Type receiver" declaration
        String declared = findDeclaredType(code, receiver);
        if (declared != null && simpleToQualified.containsKey(declared)) {
            return simpleToQualified.get(declared);
        }
        return null;
    }

    private String findDeclaredType(String code, String receiver) {
        // look for "<SimpleType> <receiver>" or "<receiver> = new <SimpleType>("
        int idx = code.indexOf(receiver);
        while (idx >= 0) {
            // case: receiver = new Type(
            int eq = skipSpaces(code, idx + receiver.length());
            if (eq < code.length() && code.charAt(eq) == '=') {
                int afterEq = skipSpaces(code, eq + 1);
                if (code.startsWith("new ", afterEq)) {
                    int ts = skipSpaces(code, afterEq + 4);
                    String type = readIdentifier(code, ts);
                    if (type.length() > 0) {
                        return type;
                    }
                }
            }
            // case: Type receiver  (token immediately before receiver is a simple type)
            int before = idx - 1;
            while (before >= 0 && code.charAt(before) == ' ') {
                before--;
            }
            int endTok = before + 1;
            while (before >= 0 && isIdChar(code.charAt(before))) {
                before--;
            }
            String type = code.substring(before + 1, endTok);
            if (type.length() > 0 && isUpper(type.charAt(0))
                    && simpleToQualified.containsKey(type)) {
                return type;
            }
            idx = code.indexOf(receiver, idx + 1);
        }
        return null;
    }

    private List<CodeCompletion> cssCompletions(String code, int cursor) {
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        int start = cursor;
        while (start > 0 && isIdChar(code.charAt(start - 1))) {
            start--;
        }
        String prefix = code.substring(start, cursor).toLowerCase();
        boolean afterDot = start > 0 && code.charAt(start - 1) == '.';
        if (afterDot) {
            for (String state : CSS_STATES) {
                if (matches(state, prefix)) {
                    out.add(new CodeCompletion(state).setType("state"));
                }
            }
            return out;
        }
        for (int i = 0; i < uiids.size(); i++) {
            String uiid = uiids.get(i);
            if (uiid != null && matches(uiid, prefix)) {
                out.add(new CodeCompletion(uiid).setType("uiid"));
            }
        }
        for (String prop : CSS_PROPERTIES) {
            if (matches(prop, prefix)) {
                out.add(new CodeCompletion(prop, prop + ": ").setType("property"));
            }
        }
        return out;
    }

    private static String receiverToken(String code, int dotIndex) {
        int end = dotIndex;
        int i = dotIndex - 1;
        while (i >= 0 && isIdChar(code.charAt(i))) {
            i--;
        }
        return code.substring(i + 1, end);
    }

    private static String methodName(String signature) {
        int paren = signature.indexOf('(');
        String name = paren >= 0 ? signature.substring(0, paren) : signature;
        int sp = name.lastIndexOf(' ');
        return sp >= 0 ? name.substring(sp + 1) : name;
    }

    private String simpleOf(String qualified) {
        if (qualified == null) {
            return "";
        }
        int dot = qualified.lastIndexOf('.');
        return dot >= 0 ? qualified.substring(dot + 1) : qualified;
    }

    private static boolean matches(String candidate, String lowerPrefix) {
        if (candidate == null) {
            return false;
        }
        if (lowerPrefix.length() == 0) {
            return true;
        }
        return candidate.toLowerCase().indexOf(lowerPrefix) >= 0;
    }

    private static boolean isIdChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '$';
    }

    private static boolean isUpper(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static int skipSpaces(String code, int i) {
        while (i < code.length() && (code.charAt(i) == ' ' || code.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private static String readIdentifier(String code, int i) {
        int s = i;
        while (i < code.length() && isIdChar(code.charAt(i))) {
            i++;
        }
        return code.substring(s, i);
    }
}
