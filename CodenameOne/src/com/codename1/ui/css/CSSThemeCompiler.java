/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.ui.css;

import com.codename1.ui.Component;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.util.MutableResource;
import java.util.ArrayList;
import java.util.Hashtable;

/// Compiles a subset of Codename One CSS into theme properties stored in a {@link MutableResource}.
///
/// ## Supported selector syntax
///
/// - `UIID`
/// - `UIID:selected`
/// - `UIID:pressed`
/// - `UIID:disabled`
/// - `*` (mapped to `Component`)
/// - `:root` (for constants only)
///
/// ## Supported declarations
///
/// - `color`
/// - `background-color`
/// - `padding`
/// - `margin`
/// - `font-family` (mapped to `font` string for later resolution)
/// - `cn1-derive`
/// - `cn1-image-id`
/// - `cn1-mutable-image`
/// - border-related properties: `border`, `border-*`, `background-image`, `background-position`, `background-repeat`
///
/// ## Theme constants
///
/// - CSS custom property definitions in `:root`, e.g. `--primary: #ff00ff;`
/// - `@constants { name: value; other: value; }`
/// - `var(--name)` dereferencing in declaration values.
public class CSSThemeCompiler {

    public static class CSSSyntaxException extends IllegalArgumentException {
        public CSSSyntaxException(String message) {
            super(message);
        }

        public CSSSyntaxException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void compile(String css, MutableResource resources, String themeName) {
        Hashtable theme = resources.getTheme(themeName);
        if (theme == null) {
            theme = new Hashtable();
        }

        compileConstants(css, theme);
        Rule[] rules = parseRules(css);
        for (Rule rule : rules) {
            applyRule(theme, resources, rule);
        }
        resolveThemeConstantVars(theme);
        resources.setTheme(themeName, theme);
    }

    private void resolveThemeConstantVars(Hashtable theme) {
        for (Object keyObj : theme.keySet()) {
            String key = String.valueOf(keyObj);
            if (!key.startsWith("@")) {
                continue;
            }
            Object value = theme.get(key);
            if (!(value instanceof String)) {
                continue;
            }
            theme.put(key, resolveVars(theme, (String) value));
        }
    }

    private void compileConstants(String css, Hashtable theme) {
        String stripped = stripComments(css);
        int constantsStart = stripped.indexOf("@constants");
        if (constantsStart < 0) {
            return;
        }
        int open = stripped.indexOf('{', constantsStart);
        if (open < 0) {
            return;
        }
        int close = stripped.indexOf('}', open + 1);
        if (close <= open) {
            throw new CSSSyntaxException("Unterminated @constants block");
        }
        Declaration[] declarations = parseDeclarations(stripped.substring(open + 1, close));
        for (Declaration declaration : declarations) {
            theme.put("@" + declaration.property, declaration.value);
        }
    }

    private void applyRule(Hashtable theme, MutableResource resources, Rule rule) {
        if (":root".equals(rule.selector)) {
            applyRootDeclarations(theme, rule.declarations);
            return;
        }

        String[] selectorParts = selector(rule.selector);
        String uiid = selectorParts[0];
        String statePrefix = selectorParts[1];
        StringBuilder borderCss = new StringBuilder();

        for (int i = 0; i < rule.declarations.length; i++) {
            Declaration declaration = rule.declarations[i];
            String property = declaration.property;
            String value = resolveVars(theme, declaration.value);

            if (applyThemeConstantProperty(theme, property, value)) {
                continue;
            }
            if (applySimpleThemeProperty(theme, uiid, statePrefix, property, value)) {
                continue;
            }
            if (applyImageProperty(theme, resources, uiid, statePrefix, property, value)) {
                continue;
            }
            if (appendBorderProperty(borderCss, property, value)) {
                continue;
            }
        }

        if (borderCss.length() == 0) {
            return;
        }
        theme.put(uiid + "." + statePrefix + "border", new CSSBorder(null, borderCss.toString()));
    }

    private void applyRootDeclarations(Hashtable theme, Declaration[] declarations) {
        for (Declaration declaration : declarations) {
            if (!declaration.property.startsWith("--")) {
                continue;
            }
            theme.put("@" + declaration.property.substring(2), resolveVars(theme, declaration.value));
        }
    }

    private boolean applyThemeConstantProperty(Hashtable theme, String property, String value) {
        if (!property.startsWith("--")) {
            return false;
        }
        theme.put("@" + property.substring(2), value);
        return true;
    }

    private boolean applySimpleThemeProperty(Hashtable theme, String uiid, String statePrefix, String property, String value) {
        if ("color".equals(property)) {
            theme.put(uiid + "." + statePrefix + "fgColor", normalizeHexColor(value));
            return true;
        }
        if ("background-color".equals(property)) {
            theme.put(uiid + "." + statePrefix + "bgColor", normalizeHexColor(value));
            if (!"transparent".equalsIgnoreCase(value)) {
                theme.put(uiid + "." + statePrefix + "transparency", "255");
            }
            return true;
        }
        if ("padding".equals(property) || "margin".equals(property)) {
            theme.put(uiid + "." + statePrefix + property, normalizeBox(value));
            return true;
        }
        if ("cn1-derive".equals(property)) {
            theme.put(uiid + "." + statePrefix + "derive", value);
            return true;
        }
        if ("font-family".equals(property)) {
            theme.put(uiid + "." + statePrefix + "font", value);
            return true;
        }
        if ("text-align".equals(property)) {
            Integer align = normalizeAlignment(value);
            theme.put(uiid + "." + statePrefix + "align", align);
            return true;
        }
        return false;
    }

    private boolean applyImageProperty(Hashtable theme, MutableResource resources, String uiid, String statePrefix, String property, String value) {
        if ("cn1-image-id".equals(property)) {
            Image image = resources.getImage(value);
            if (image == null) {
                return true;
            }
            theme.put(uiid + "." + statePrefix + "bgImage", image);
            return true;
        }
        if (!"cn1-mutable-image".equals(property)) {
            return false;
        }
        String[] parts = splitOnWhitespace(value);
        if (parts.length < 2) {
            return true;
        }
        String imageId = parts[0];
        Image image = createSolidImage(parts[1]);
        resources.setImage(imageId, image);
        theme.put(uiid + "." + statePrefix + "bgImage", image);
        return true;
    }

    private boolean appendBorderProperty(StringBuilder borderCss, String property, String value) {
        if (!isBorderProperty(property)) {
            return false;
        }
        if (borderCss.length() > 0) {
            borderCss.append(';');
        }
        borderCss.append(property).append(':').append(value);
        return true;
    }

    private String resolveVars(Hashtable theme, String value) {
        String out = value;
        int varPos = out.indexOf("var(--");
        while (varPos > -1) {
            int end = out.indexOf(')', varPos);
            if (end < 0) {
                break;
            }
            String key = out.substring(varPos + "var(--".length(), end).trim();
            Object replacement = theme.get("@" + key);
            String replaceValue = replacement == null ? "" : replacement.toString();
            out = out.substring(0, varPos) + replaceValue + out.substring(end + 1);
            varPos = out.indexOf("var(--");
        }
        return out;
    }

    private String[] selector(String selector) {
        String statePrefix = "";
        String uiid = selector.trim();
        int pseudoPos = uiid.indexOf(':');
        if (pseudoPos > -1) {
            String pseudo = uiid.substring(pseudoPos + 1).trim();
            uiid = uiid.substring(0, pseudoPos).trim();
            statePrefix = statePrefix(pseudo);
        }
        if ("*".equals(uiid) || uiid.length() == 0) {
            uiid = "Component";
        }
        return new String[]{uiid, statePrefix};
    }

    private String statePrefix(String pseudo) {
        if ("selected".equals(pseudo)) {
            return "sel#";
        }
        if ("pressed".equals(pseudo)) {
            return "press#";
        }
        if ("disabled".equals(pseudo)) {
            return "dis#";
        }
        return "";
    }

    private Image createSolidImage(String color) {
        int rgb = parseColor(color);
        return EncodedImage.createFromRGB(new int[]{rgb}, 1, 1, false);
    }

    private int parseColor(String cssColor) {
        String hex = normalizeHexColor(cssColor);
        return Integer.parseInt(hex, 16) | 0xff000000;
    }

    private boolean isBorderProperty(String property) {
        return "border".equals(property)
                || property.startsWith("border-")
                || property.startsWith("background-image")
                || property.startsWith("background-position")
                || property.startsWith("background-repeat");
    }

    private String normalizeHexColor(String cssColor) {
        String value = cssColor == null ? "" : cssColor.trim().toLowerCase();
        if (value.length() == 0) {
            throw new CSSSyntaxException("Color value cannot be empty");
        }
        if ("transparent".equals(value)) {
            return "000000";
        }

        if (value.startsWith("rgb(")) {
            if (!value.endsWith(")")) {
                throw new CSSSyntaxException("Malformed rgb() color: " + cssColor);
            }
            String[] parts = splitOnComma(value.substring(4, value.length() - 1));
            if (parts.length != 3) {
                throw new CSSSyntaxException("rgb() must have exactly 3 components: " + cssColor);
            }
            int r = parseRgbChannel(parts[0], cssColor);
            int g = parseRgbChannel(parts[1], cssColor);
            int b = parseRgbChannel(parts[2], cssColor);
            return toHexColor((r << 16) | (g << 8) | b);
        }

        String keyword = cssColorKeyword(value);
        if (keyword != null) {
            return keyword;
        }

        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 3) {
            value = "" + value.charAt(0) + value.charAt(0)
                    + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2);
        }
        if (value.length() != 6 || !isHexColor(value)) {
            throw new CSSSyntaxException("Unsupported color value: " + cssColor);
        }
        return value;
    }

    private Integer normalizeAlignment(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if ("left".equals(v) || "start".equals(v)) {
            return Integer.valueOf(Component.LEFT);
        }
        if ("center".equals(v)) {
            return Integer.valueOf(Component.CENTER);
        }
        if ("right".equals(v) || "end".equals(v)) {
            return Integer.valueOf(Component.RIGHT);
        }
        throw new CSSSyntaxException("Unsupported text-align value: " + value);
    }

    private String cssColorKeyword(String value) {
        if ("black".equals(value)) {
            return "000000";
        }
        if ("white".equals(value)) {
            return "ffffff";
        }
        if ("red".equals(value)) {
            return "ff0000";
        }
        if ("green".equals(value)) {
            return "008000";
        }
        if ("blue".equals(value)) {
            return "0000ff";
        }
        if ("pink".equals(value)) {
            return "ffc0cb";
        }
        if ("orange".equals(value)) {
            return "ffa500";
        }
        if ("yellow".equals(value)) {
            return "ffff00";
        }
        if ("purple".equals(value)) {
            return "800080";
        }
        if ("gray".equals(value) || "grey".equals(value)) {
            return "808080";
        }
        return null;
    }

    private int parseRgbChannel(String value, String originalColor) {
        int out;
        try {
            out = Integer.parseInt(value.trim());
        } catch (RuntimeException err) {
            throw new CSSSyntaxException("Invalid rgb() channel value in " + originalColor + ": " + value, err);
        }
        if (out < 0 || out > 255) {
            throw new CSSSyntaxException("rgb() channel out of range in " + originalColor + ": " + value);
        }
        return out;
    }

    private boolean isHexColor(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private String toHexColor(int color) {
        String hex = Integer.toHexString(color & 0xffffff);
        while (hex.length() < 6) {
            hex = "0" + hex;
        }
        return hex;
    }

    private String normalizeBox(String cssValue) {
        String[] parts = splitOnWhitespace(cssValue.trim());
        if (parts.length == 1) {
            return scalar(parts[0]) + "," + scalar(parts[0]) + "," + scalar(parts[0]) + "," + scalar(parts[0]);
        }
        if (parts.length == 2) {
            return scalar(parts[0]) + "," + scalar(parts[1]) + "," + scalar(parts[0]) + "," + scalar(parts[1]);
        }
        if (parts.length == 3) {
            return scalar(parts[0]) + "," + scalar(parts[1]) + "," + scalar(parts[2]) + "," + scalar(parts[1]);
        }
        if (parts.length >= 4) {
            return scalar(parts[0]) + "," + scalar(parts[1]) + "," + scalar(parts[2]) + "," + scalar(parts[3]);
        }
        return "0,0,0,0";
    }

    private String scalar(String value) {
        String out = value.trim();
        if (out.endsWith("px")) {
            out = out.substring(0, out.length() - 2);
        }
        return out;
    }

    private Rule[] parseRules(String css) {
        String stripped = stripComments(css);
        ArrayList<Rule> out = new ArrayList<Rule>();
        int pos = 0;
        while (pos < stripped.length()) {
            while (pos < stripped.length() && Character.isWhitespace(stripped.charAt(pos))) {
                pos++;
            }
            if (pos >= stripped.length()) {
                break;
            }
            int open = stripped.indexOf('{', pos);
            if (open < 0) {
                throw new CSSSyntaxException("Missing '{' in CSS rule near: " + stripped.substring(pos));
            }
            int close = stripped.indexOf('}', open + 1);
            if (close < 0) {
                throw new CSSSyntaxException("Missing '}' for CSS rule: " + stripped.substring(pos, open).trim());
            }
            if (stripped.indexOf('{', open + 1) > -1 && stripped.indexOf('{', open + 1) < close) {
                throw new CSSSyntaxException("Nested '{' is not supported in CSS block: " + stripped.substring(pos, open).trim());
            }

            String selectors = stripped.substring(pos, open).trim();
            if (selectors.startsWith("@constants")) {
                pos = close + 1;
                continue;
            }
            if (selectors.length() == 0) {
                throw new CSSSyntaxException("Missing selector before '{'");
            }

            String body = stripped.substring(open + 1, close).trim();
            Declaration[] declarations = parseDeclarations(body);
            String[] selectorsList = splitOnChar(selectors, ',');
            for (String selectorEntry : selectorsList) {
                String selector = selectorEntry.trim();
                if (selector.length() == 0) {
                    throw new CSSSyntaxException("Empty selector in selector list: " + selectors);
                }
                Rule rule = new Rule();
                rule.selector = selector;
                rule.declarations = declarations;
                out.add(rule);
            }

            pos = close + 1;
        }
        return out.toArray(new Rule[out.size()]);
    }

    private String stripComments(String css) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < css.length()) {
            char c = css.charAt(i);
            if (c == '/' && i + 1 < css.length() && css.charAt(i + 1) == '*') {
                i += 2;
                boolean closed = false;
                while (i + 1 < css.length()) {
                    if (css.charAt(i) == '*' && css.charAt(i + 1) == '/') {
                        i += 2;
                        closed = true;
                        break;
                    }
                    i++;
                }
                if (!closed) {
                    throw new CSSSyntaxException("Unterminated CSS comment");
                }
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private Declaration[] parseDeclarations(String body) {
        ArrayList<Declaration> out = new ArrayList<Declaration>();
        String[] segments = splitOnChar(body, ';');
        for (String line : segments) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon == trimmed.length() - 1) {
                throw new CSSSyntaxException("Malformed declaration: " + trimmed);
            }
            Declaration dec = new Declaration();
            dec.property = trimmed.substring(0, colon).trim().toLowerCase();
            dec.value = trimmed.substring(colon + 1).trim();
            if (dec.property.length() == 0 || dec.value.length() == 0) {
                throw new CSSSyntaxException("Malformed declaration: " + trimmed);
            }
            out.add(dec);
        }
        return out.toArray(new Declaration[out.size()]);
    }

    private String[] splitOnChar(String input, char delimiter) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) != delimiter) {
                continue;
            }
            out.add(input.substring(start, i));
            start = i + 1;
        }
        out.add(input.substring(start));
        return out.toArray(new String[out.size()]);
    }

    private String[] splitOnComma(String input) {
        ArrayList<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == ',') {
                String token = input.substring(start, i).trim();
                if (token.length() > 0) {
                    parts.add(token);
                }
                start = i + 1;
            }
        }
        String tail = input.substring(start).trim();
        if (tail.length() > 0) {
            parts.add(tail);
        }
        return parts.toArray(new String[parts.size()]);
    }

    private String[] splitOnWhitespace(String input) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    out.add(token.toString());
                    token.setLength(0);
                }
                continue;
            }
            token.append(c);
        }
        if (token.length() > 0) {
            out.add(token.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    private static class Rule {
        String selector;
        Declaration[] declarations;
    }

    private static class Declaration {
        String property;
        String value;
    }
}
