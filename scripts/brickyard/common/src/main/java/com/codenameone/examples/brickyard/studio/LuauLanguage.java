/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeCompletionProvider;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.editor.SyntaxHighlightResult;
import com.codename1.ui.editor.SyntaxHighlighter;
import com.codename1.ui.editor.SyntaxToken;
import com.codename1.util.SuccessCallback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Luau for the studio's script editor: a line highlighter registered with
/// `CodeEditor` under `"luau"`, and completion that knows the engine API the
/// templates use -- the globals, the services `game:GetService` hands back
/// and the members of a part, a player and the datatypes.
final class LuauLanguage implements SyntaxHighlighter, CodeCompletionProvider {
    static final String ID = "luau";

    private static final int IN_COMMENT = 1;
    private static final int IN_STRING = 2;

    private static final String[] KEYWORDS = {
        "and", "break", "continue", "do", "else", "elseif", "end", "false", "for", "function", "if", "in",
        "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while", "export", "type"
    };
    private static final String[] GLOBALS = {
        "game", "workspace", "script", "Instance", "Vector3", "CFrame", "Color3", "BrickColor", "UDim2",
        "Enum", "TweenInfo", "Ray", "task", "math", "string", "table", "print", "warn", "error", "pairs",
        "ipairs", "tostring", "tonumber", "typeof", "require", "pcall", "os", "coroutine", "wait"
    };
    private static final Set<String> KEYWORD_SET = set(KEYWORDS);
    private static final Set<String> GLOBAL_SET = set(GLOBALS);

    /// Members offered after `receiver.` or `receiver:`; an unknown receiver
    /// is taken for an instance and gets [#INSTANCE].
    private static final Map<String, String[]> MEMBERS = new HashMap<String, String[]>();
    private static final String[] INSTANCE = {
        "Name", "Parent", "Position", "Size", "CFrame", "Color", "Anchored", "CanCollide", "Transparency",
        "Material", "Touched", "TouchEnded", "Destroy", "Clone", "FindFirstChild", "WaitForChild",
        "GetChildren", "GetDescendants", "IsA", "SetAttribute", "GetAttribute", "Changed",
        "GetPropertyChangedSignal", "Humanoid", "Character", "UserId", "DisplayName", "leaderstats", "Value"
    };

    static {
        MEMBERS.put("game", new String[] {"GetService", "Workspace", "Players", "Lighting",
            "ReplicatedStorage", "ServerStorage", "PlaceId"});
        MEMBERS.put("Players", new String[] {"PlayerAdded", "PlayerRemoving", "GetPlayers",
            "GetPlayerFromCharacter", "LocalPlayer", "MaxPlayers"});
        MEMBERS.put("task", new String[] {"wait", "spawn", "delay", "defer", "cancel"});
        MEMBERS.put("math", new String[] {"random", "floor", "ceil", "abs", "clamp", "sin", "cos", "sqrt",
            "max", "min", "huge", "pi", "noise", "round", "sign"});
        MEMBERS.put("string", new String[] {"format", "sub", "len", "upper", "lower", "find", "gsub",
            "match", "gmatch", "rep", "split"});
        MEMBERS.put("table", new String[] {"insert", "remove", "find", "sort", "concat", "clear", "clone",
            "freeze"});
        MEMBERS.put("Instance", new String[] {"new"});
        MEMBERS.put("Vector3", new String[] {"new", "zero", "one", "xAxis", "yAxis", "zAxis"});
        MEMBERS.put("CFrame", new String[] {"new", "Angles", "lookAt", "fromEulerAnglesXYZ", "identity"});
        MEMBERS.put("Color3", new String[] {"new", "fromRGB", "fromHSV", "fromHex"});
        MEMBERS.put("TweenService", new String[] {"Create"});
        MEMBERS.put("RunService", new String[] {"Heartbeat", "Stepped", "RenderStepped", "IsServer",
            "IsClient"});
        MEMBERS.put("Humanoid", new String[] {"Health", "MaxHealth", "WalkSpeed", "JumpPower", "Died",
            "MoveTo", "TakeDamage", "Jump"});
        MEMBERS.put("Enum", new String[] {"Material", "KeyCode", "EasingStyle", "EasingDirection",
            "PartType", "HumanoidStateType"});
        MEMBERS.put("Material", new String[] {"Plastic", "Neon", "Grass", "Wood", "Slate", "Concrete",
            "Metal", "Ice", "Sand", "Brick", "SmoothPlastic", "Glass"});
        MEMBERS.put("workspace", new String[] {"Gravity", "CurrentCamera", "Raycast", "FindFirstChild",
            "WaitForChild", "GetChildren"});
    }

    private static Set<String> set(String[] words) {
        Set<String> s = new HashSet<String>();
        for (String w : words) {
            s.add(w);
        }
        return s;
    }

    /// Registers the highlighter once and returns an editor set up for Luau.
    static CodeEditor editor() {
        LuauLanguage lang = new LuauLanguage();
        CodeEditor.registerSyntaxHighlighter(ID, lang);
        CodeEditor e = new CodeEditor();
        e.setLanguage(ID);
        e.setTheme("dark");
        e.setTabSize(4);
        e.setShowLineNumbers(true);
        e.setCompletionProvider(lang);
        return e;
    }

    @Override
    public SyntaxHighlightResult tokenize(String line, int startState) {
        List<SyntaxToken> out = new ArrayList<SyntaxToken>();
        int n = line.length();
        int i = 0;
        if (startState == IN_COMMENT || startState == IN_STRING) {
            int close = line.indexOf("]]");
            int kind = startState == IN_COMMENT ? SyntaxToken.COMMENT : SyntaxToken.STRING;
            if (close < 0) {
                out.add(new SyntaxToken(0, n, kind));
                return new SyntaxHighlightResult(out, startState);
            }
            out.add(new SyntaxToken(0, close + 2, kind));
            i = close + 2;
        }
        while (i < n) {
            char c = line.charAt(i);
            if (c == '-' && i + 1 < n && line.charAt(i + 1) == '-') {
                if (line.startsWith("[[", i + 2)) {
                    int close = line.indexOf("]]", i + 4);
                    if (close < 0) {
                        out.add(new SyntaxToken(i, n - i, SyntaxToken.COMMENT));
                        return new SyntaxHighlightResult(out, IN_COMMENT);
                    }
                    out.add(new SyntaxToken(i, close + 2 - i, SyntaxToken.COMMENT));
                    i = close + 2;
                    continue;
                }
                out.add(new SyntaxToken(i, n - i, SyntaxToken.COMMENT));
                break;
            }
            if (c == '[' && i + 1 < n && line.charAt(i + 1) == '[') {
                int close = line.indexOf("]]", i + 2);
                if (close < 0) {
                    out.add(new SyntaxToken(i, n - i, SyntaxToken.STRING));
                    return new SyntaxHighlightResult(out, IN_STRING);
                }
                out.add(new SyntaxToken(i, close + 2 - i, SyntaxToken.STRING));
                i = close + 2;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                int j = i + 1;
                while (j < n && line.charAt(j) != c) {
                    j += line.charAt(j) == '\\' ? 2 : 1;
                }
                j = Math.min(n, j + 1);
                out.add(new SyntaxToken(i, j - i, SyntaxToken.STRING));
                i = j;
                continue;
            }
            if (c >= '0' && c <= '9') {
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '.'
                        || line.charAt(j) == '_')) {
                    j++;
                }
                out.add(new SyntaxToken(i, j - i, SyntaxToken.NUMBER));
                i = j;
                continue;
            }
            if (identStart(c)) {
                int j = i + 1;
                while (j < n && identPart(line.charAt(j))) {
                    j++;
                }
                String w = line.substring(i, j);
                boolean member = i > 0 && (line.charAt(i - 1) == '.' || line.charAt(i - 1) == ':');
                if (!member && KEYWORD_SET.contains(w)) {
                    out.add(new SyntaxToken(i, j - i, SyntaxToken.KEYWORD));
                } else if (!member && GLOBAL_SET.contains(w)) {
                    out.add(new SyntaxToken(i, j - i, SyntaxToken.TYPE));
                } else if (member) {
                    out.add(new SyntaxToken(i, j - i, SyntaxToken.PROPERTY));
                }
                i = j;
                continue;
            }
            i++;
        }
        return new SyntaxHighlightResult(out, 0);
    }

    private static boolean identStart(char c) {
        return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean identPart(char c) {
        return identStart(c) || (c >= '0' && c <= '9');
    }

    @Override
    public void getCompletions(CodeEditor editor, String code, int cursor,
            SuccessCallback<List<CodeCompletion>> results) {
        results.onSucess(complete(code, cursor));
    }

    /// The proposals at `cursor`; the editor narrows them by the typed prefix.
    static List<CodeCompletion> complete(String code, int cursor) {
        int c = Math.max(0, Math.min(cursor, code.length()));
        int start = c;
        while (start > 0 && identPart(code.charAt(start - 1))) {
            start--;
        }
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        if (start > 0 && (code.charAt(start - 1) == '.' || code.charAt(start - 1) == ':')) {
            int r = start - 1;
            while (r > 0 && identPart(code.charAt(r - 1))) {
                r--;
            }
            String receiver = code.substring(r, start - 1);
            String[] members = MEMBERS.get(receiver);
            if (members == null) {
                members = MEMBERS.get(serviceOf(code, receiver));
            }
            if (members == null) {
                members = INSTANCE;
            }
            for (String m : members) {
                out.add(new CodeCompletion(m).setType(Character.isUpperCase(m.charAt(0)) ? "property" : "method")
                        .setDetail(receiver));
            }
            return out;
        }
        Set<String> seen = new HashSet<String>();
        for (String k : KEYWORDS) {
            seen.add(k);
            out.add(new CodeCompletion(k).setType("keyword"));
        }
        for (String g : GLOBALS) {
            seen.add(g);
            out.add(new CodeCompletion(g).setType("class"));
        }
        // The script's own locals, so a name declared once is one tap away.
        int i = 0;
        int n = code.length();
        while (i < n) {
            if (identStart(code.charAt(i)) && (i == 0 || !identPart(code.charAt(i - 1)))) {
                int j = i + 1;
                while (j < n && identPart(code.charAt(j))) {
                    j++;
                }
                String w = code.substring(i, j);
                if (j - i > 2 && i != start && seen.add(w)) {
                    out.add(new CodeCompletion(w).setType("variable"));
                }
                i = j;
            } else {
                i++;
            }
        }
        return out;
    }

    /// `local Players = game:GetService("Players")` makes `Players` that
    /// service: the name of the service a local was assigned, or null.
    private static String serviceOf(String code, String local) {
        String key = "local " + local + " = game:GetService(\"";
        int at = code.indexOf(key);
        if (at < 0) {
            return null;
        }
        int s = at + key.length();
        int e = code.indexOf('"', s);
        return e < 0 ? null : code.substring(s, e);
    }
}
