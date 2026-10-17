/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ui.css;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.MutableResource;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/// Compiles a subset of Codename One CSS into theme properties stored in a {@link MutableResource}.
///
/// ## Supported selector syntax
///
/// - `UIID`
/// - `UIID:selected`
/// - `UIID:pressed`
/// - `UIID:disabled`
/// - `UIID:hover`
/// - `*` (mapped to `Component`)
/// - `:root` (for constants only)
///
/// ## Supported declarations
///
/// - `color`
/// - `background-color`
/// - `padding`, `margin`: one to four values in CSS top/right/bottom/left order, stored in the
///   theme's top/bottom/left/right order together with per-side `padUnit`/`marUnit` metadata.
///   `px`, `pt` and unitless values are pixels (rounded, as the build-time compiler does), `mm`
///   and `cm` are millimetres, and `rem`, `vw`, `vh`, `vmin`, `vmax` keep their own unit. A
///   percentage is rejected, as the build-time compiler rejects it.
/// - `font-family` and `font-size`: resolved to a {@link Font} with the build-time compiler's
///   semantics. The family defaults to `native:MainRegular` and the size to the medium system
///   font; `font-size` accepts `px`, `pt`, `mm`, `cm`, `in`, `rem`, `vw`, `vh`, `vmin`, `vmax`,
///   `%` and the `small`/`medium`/`large` keywords.
/// - `cn1-derive`
/// - `cn1-image-id`
/// - `cn1-mutable-image`
/// - border-related properties: `border`, `border-*`, `background-image`, `background-position`, `background-repeat`
///
/// ## Theme constants
///
/// - CSS custom property definitions in `:root`, e.g. `--primary: #ff00ff;`
/// - `#Constants { name: value; }`, the block the build-time compiler reads, and the older
///   `@constants { name: value; other: value; }` spelling.
/// - `var(--name)` dereferencing in declaration values.
///
/// A constant keeps the case of its name (`includeNativeBool` becomes `@includeNativeBool`) and
/// a quoted value is stored without its quotes, so `'true'` and `"true"` both become `true`.
public class CSSThemeCompiler {

    public static class CSSSyntaxException extends IllegalArgumentException {
        public CSSSyntaxException(String message) {
            super(message);
        }

        public CSSSyntaxException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final String DEFERRED_IMAGE = "image";
    private static final String DEFERRED_VAR = "var";
    private static final String DEFERRED_LITERAL = "literal";
    /// Declared by this compile and needing no further handling beyond var() resolution.
    private static final String DEFERRED_PLAIN = "plain";
    /// Resolved like any var() constant, but kept as text: it aliases a quoted constant.
    private static final String DEFERRED_TEXT = "text";

    /// The `deferred` table of the compile in progress; resolveVars reads which constants are literal.
    private Hashtable<String, String> deferredConstants = new Hashtable<String, String>();

    /// The `cn1-derive` each UIID (with its `$Dark` prefix) declared last in any state, in source
    /// order: the build-time compiler sets every such derive on the owning UIID, so the last one wins.
    private Hashtable<String, String> ownerDerives = new Hashtable<String, String>();

    public void compile(String css, MutableResource resources, String themeName) {
        Hashtable theme = resources.getTheme(themeName);
        if (theme == null) {
            theme = new Hashtable();
        }

        String strippedCss = stripComments(css);
        Rule[] rules = parseRulesWithMedia(strippedCss);
        // Constants whose final value is only known after every rule ran: an image constant,
        // whose image a rule may still create (cn1-mutable-image), and one written as var(--x).
        // Keyed by "@name"; only the declaration that wins for a name decides its entry. Every
        // constant this compile declares has an entry, so constants a previous compile left in
        // the same theme are never re-resolved without the typing they were declared with.
        Hashtable<String, String> deferred = new Hashtable<String, String>();
        deferredConstants = deferred;
        // #Constants entries first, wherever their block sits, so every rule sees their final
        // values -- the build-time compiler reads that block before it compiles any rule.
        Hashtable<String, Declaration> finalConstants = new Hashtable<String, Declaration>();
        for (Rule rule : rules) {
            if (rule.constants) {
                compileConstants(rule.declarations, theme, deferred);
                for (Declaration declaration : rule.declarations) {
                    Declaration previous = finalConstants.put(declaration.name, declaration);
                    if (previous != null) {
                        previous.winner = false;
                    }
                    declaration.winner = true;
                }
            }
        }
        // Then source order. A `:root` or in-rule custom property takes effect where it is
        // declared, as the build-time parser's callbacks do: a rule above it does not see it.
        // Only the winning declaration of a #Constants name is applied again here, so it still
        // overrides a custom property declared before it.
        Hashtable<String, String[]> fonts = new Hashtable<String, String[]>();
        ownerDerives = new Hashtable<String, String>();
        for (Rule rule : rules) {
            if (rule.constants) {
                ArrayList<Declaration> winners = new ArrayList<Declaration>();
                for (Declaration declaration : rule.declarations) {
                    if (declaration.winner) {
                        winners.add(declaration);
                    }
                }
                compileConstants(winners.toArray(new Declaration[winners.size()]), theme, deferred);
                continue;
            }
            applyRule(theme, resources, rule, fonts, deferred);
        }
        markLiteralAliases(theme, deferred);
        compileFonts(theme, fonts);
        inheritHoverDerivations(theme);
        resolveThemeConstantVars(theme, deferred);
        for (Map.Entry<String, String> entry : deferred.entrySet()) {
            String key = entry.getKey();
            Object value = theme.get(key);
            if (!(value instanceof String)) {
                continue;
            }
            String resolved = unquote((String) value);
            if (DEFERRED_IMAGE.equals(entry.getValue())) {
                theme.put(key, imageConstant(resources, key.substring(1), resolved));
            } else if (DEFERRED_VAR.equals(entry.getValue()) && isColorLiteral(resolved)) {
                // A constant written as var(--x) is only a color once x is resolved.
                theme.put(key, constantColor(resolved));
            }
        }
        resources.setTheme(themeName, theme);
    }

    /// `alias: var(--literal)` where `literal` is quoted resolves to that text, not to a color:
    /// such an alias, followed through any chain of plain aliases, is not reclassified.
    private void markLiteralAliases(Hashtable theme, Hashtable<String, String> deferred) {
        ArrayList<String> aliases = new ArrayList<String>();
        for (Map.Entry<String, String> entry : deferred.entrySet()) {
            if (DEFERRED_VAR.equals(entry.getValue())) {
                aliases.add(entry.getKey());
            }
        }
        for (String key : aliases) {
            ArrayList<String> seen = new ArrayList<String>();
            Object value = theme.get(key);
            String target = key;
            while (value instanceof String && !seen.contains(target)) {
                seen.add(target);
                String v = ((String) value).trim();
                if (!v.startsWith("var(--") || v.indexOf(')') != v.length() - 1) {
                    break;
                }
                target = "@" + v.substring("var(--".length(), v.length() - 1).trim();
                if (DEFERRED_LITERAL.equals(deferred.get(target))) {
                    deferred.put(key, DEFERRED_TEXT);
                    break;
                }
                value = theme.get(target);
            }
        }
    }

    private void inheritHoverDerivations(Hashtable theme) {
        ArrayList<String> ids = new ArrayList<String>();
        for (Object keyObj : theme.keySet()) {
            String key = String.valueOf(keyObj);
            int dot = key.indexOf('.');
            if (key.startsWith("@") || dot < 0) {
                continue;
            }
            String id = key.substring(0, dot);
            if (id.startsWith("$Dark")) {
                id = id.substring(5);
            }
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        // Prefixed style lookup does not follow the normal derive key. Materialize
        // only hover derivations whose base actually declares that state.
        for (int appearance = 0; appearance < 2; appearance++) {
            boolean dark = appearance == 1;
            String prefix = dark ? "$Dark" : "";
            // Each pass can expose another link in a derive chain; this is a
            // convergence bound, not an iteration over individual component IDs.
            int remainingPasses = ids.size();
            while (remainingPasses-- > 0) {
                boolean changed = false;
                for (String id : ids) {
                    String base = hoverBase(theme, id, dark);
                    String key = prefix + id + ".hover#derive";
                    if (base == null || theme.containsKey(key) || cyclicDerivation(theme, id, dark)) {
                        continue;
                    }
                    boolean baseHasHover = hasHoverDefinition(theme, prefix + base);
                    if (dark && theme.containsKey("$Dark" + id + ".derive")) {
                        baseHasHover |= hasHoverDefinition(theme, base);
                    }
                    if (baseHasHover) {
                        if (dark) {
                            // An explicit dark derive bypasses UIManager's light-style
                            // fallback, so retain the child's own light hover overrides.
                            ArrayList<Object> keys = new ArrayList<Object>(theme.keySet());
                            String lightPrefix = id + ".hover#";
                            for (Object property : keys) {
                                String lightKey = String.valueOf(property);
                                if (lightKey.startsWith(lightPrefix) && !lightKey.endsWith("#derive")
                                        && !theme.containsKey("$Dark" + lightKey)) {
                                    theme.put("$Dark" + lightKey, theme.get(property));
                                }
                            }
                        }
                        theme.put(key, base + ".hover");
                        changed = true;
                    }
                }
                if (!changed) {
                    break;
                }
            }
        }
    }

    private String hoverBase(Hashtable theme, String id, boolean dark) {
        Object base = dark ? theme.get("$Dark" + id + ".derive") : null;
        if (base == null) {
            base = theme.get(id + ".derive");
        }
        return base instanceof String ? (String) base : null;
    }

    private boolean cyclicDerivation(Hashtable theme, String id, boolean dark) {
        ArrayList<String> seen = new ArrayList<String>();
        while (id != null) {
            if (seen.contains(id)) {
                return true;
            }
            seen.add(id);
            id = hoverBase(theme, id, dark);
        }
        return false;
    }

    private boolean hasHoverDefinition(Hashtable theme, String id) {
        String prefix = id + ".hover#";
        for (Object key : theme.keySet()) {
            if (String.valueOf(key).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void resolveThemeConstantVars(Hashtable theme, Hashtable<String, String> deferred) {
        for (Map.Entry<String, String> entry : deferred.entrySet()) {
            String key = entry.getKey();
            // A quoted value is a string: `"var(--name)"` is text, not a reference.
            if (DEFERRED_LITERAL.equals(entry.getValue())) {
                continue;
            }
            Object value = theme.get(key);
            if (!(value instanceof String)) {
                continue;
            }
            theme.put(key, resolveVars(theme, (String) value));
        }
    }

    private void compileConstants(Declaration[] declarations, Hashtable theme,
                                  Hashtable<String, String> deferred) {
        for (Declaration declaration : declarations) {
            String name = declaration.name;
            // `#Constants { --accent: ...; }` is how the build-time compiler exports a
            // palette variable as the constant `@accent`.
            if (name.startsWith("--")) {
                name = name.substring(2);
            }
            String key = "@" + name;
            String raw = declaration.value.trim();
            String value = unquote(raw);
            deferred.put(key, DEFERRED_PLAIN);
            if (name.endsWith("Image")) {
                deferred.put(key, DEFERRED_IMAGE);
            } else if (!raw.equals(value)) {
                deferred.put(key, DEFERRED_LITERAL);
            } else if (isColorLiteral(value)) {
                // Stored as plain hex, as the build-time compiler stores it: color constants
                // are read with Integer.parseInt(value, 16), which a leading '#' would break.
                value = constantColor(value);
            } else if (value.indexOf("var(--") > -1) {
                deferred.put(key, DEFERRED_VAR);
            }
            theme.put(key, value);
        }
    }

    /// UIManager.getThemeImageConstant casts an image constant to Image, and on iOS that cast is
    /// unchecked: a String left in its place would be read as an Image and crash natively. The
    /// build-time compiler fails the build on a missing image too.
    private static Image imageConstant(MutableResource resources, String name, String imageName) {
        Image image = resources.getImage(imageName);
        if (image == null) {
            throw new CSSSyntaxException("Constant " + name + " names no image in the resource: " + imageName);
        }
        return image;
    }

    private boolean isColorLiteral(String value) {
        String v = cssColorFunction(value);
        if (v.regionMatches(true, 0, "rgb(", 0, 4) || v.regionMatches(true, 0, "rgba(", 0, 5)) {
            return true;
        }
        int len = value.length();
        return value.startsWith("#") && (len == 4 || len == 7) && isHexColor(value.substring(1));
    }

    /// A color constant as plain hex. A color constant has no alpha, so `rgba()` keeps its RGB
    /// part, as the build-time compiler stores it.
    private String constantColor(String value) {
        value = cssColorFunction(value);
        if (value.regionMatches(true, 0, "rgba(", 0, 5) && value.endsWith(")")) {
            String[] parts = splitOnComma(value.substring(5, value.length() - 1));
            if (parts.length != 4) {
                throw new CSSSyntaxException("rgba() must have exactly 4 components: " + value);
            }
            return normalizeHexColor("rgb(" + parts[0] + "," + parts[1] + "," + parts[2] + ")");
        }
        return normalizeHexColor(value);
    }

    /// `cn1rgb()` and `cn1rgba()` are the build-time compiler's spellings of `rgb()` and `rgba()`.
    private static String cssColorFunction(String value) {
        return value.regionMatches(true, 0, "cn1rgb", 0, 6) ? value.substring(3) : value;
    }

    private static boolean isConstantsSelector(String selector) {
        return "@constants".equalsIgnoreCase(selector) || "#Constants".equalsIgnoreCase(selector);
    }

    /// Strips one pair of matching quotes, decoding backslash escapes inside them. Anything
    /// else is returned as it is.
    private static String unquote(String value) {
        String v = value.trim();
        int len = v.length();
        if (len < 2) {
            return v;
        }
        char quote = v.charAt(0);
        if ((quote != '"' && quote != '\'') || v.charAt(len - 1) != quote) {
            return v;
        }
        StringBuilder out = new StringBuilder(len - 2);
        int end = len - 1;
        for (int i = 1; i < end; i++) {
            char c = v.charAt(i);
            if (c != '\\' || i + 1 >= end) {
                out.append(c);
                continue;
            }
            i++;
            c = v.charAt(i);
            if (c == '\n' || c == '\r' || c == '\f') {
                // An escaped newline (LF, CRLF, CR or FF) continues the string.
                if (c == '\r' && i + 1 < end && v.charAt(i + 1) == '\n') {
                    i++;
                }
                continue;
            }
            int digits = 0;
            int code = 0;
            while (digits < 6 && i + digits < end && Character.digit(v.charAt(i + digits), 16) > -1) {
                code = code * 16 + Character.digit(v.charAt(i + digits), 16);
                digits++;
            }
            if (digits == 0) {
                out.append(c);
                continue;
            }
            // CSS: one to six hex digits name a code point, and one whitespace after them ends it.
            i += digits - 1;
            if (i + 1 < end && Character.isWhitespace(v.charAt(i + 1))) {
                i++;
            }
            if (code == 0 || code > 0x10FFFF || code >= 0xD800 && code <= 0xDFFF) {
                code = 0xFFFD;
            }
            if (code >= 0x10000) {
                code -= 0x10000;
                out.append((char) (0xD800 + (code >> 10))).append((char) (0xDC00 + (code & 0x3FF)));
            } else {
                out.append((char) code);
            }
        }
        return out.toString();
    }

    private void applyRule(Hashtable theme, MutableResource resources, Rule rule, Hashtable<String, String[]> fonts,
                           Hashtable<String, String> deferred) {
        if (":root".equals(rule.selector)) {
            for (Declaration declaration : rule.declarations) {
                applyThemeConstantProperty(theme, declaration.name, declaration.value, deferred);
            }
            return;
        }

        String[] selectorParts = selector(rule.selector);
        String uiid = selectorParts[0];
        String statePrefix = selectorParts[1];
        StringBuilder borderCss = new StringBuilder();

        for (int i = 0; i < rule.declarations.length; i++) {
            Declaration declaration = rule.declarations[i];
            String property = declaration.property;
            // A custom property takes effect where it is declared, also within a rule: the
            // build-time parser resolves each declaration as it reads it.
            if (applyThemeConstantProperty(theme, declaration.name, declaration.value, deferred)) {
                continue;
            }
            String value = resolveVars(theme, declaration.value);
            if ("cn1-derive".equals(property) && statePrefix.length() > 0) {
                ownerDerives.put(uiid, value.trim());
            }
            if (applyFontProperty(fonts, uiid + "." + statePrefix, property, value)) {
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

    private boolean applyThemeConstantProperty(Hashtable theme, String property, String value,
                                               Hashtable<String, String> deferred) {
        if (!property.startsWith("--")) {
            return false;
        }
        String key = "@" + property.substring(2);
        String raw = value.trim();
        String unquoted = unquote(raw);
        // A later custom property replaces a #Constants entry outright, deferred handling included.
        // An unquoted var() in it is resolved where it is used, against the final values.
        deferred.put(key, DEFERRED_PLAIN);
        if (!raw.equals(unquoted)) {
            deferred.put(key, DEFERRED_LITERAL);
        }
        theme.put(key, unquoted);
        return true;
    }

    /// Records a `font-family` or `font-size` for one UIID state; `compileFonts` builds the fonts
    /// once every rule has been read, so the two may come from different rules.
    private boolean applyFontProperty(Hashtable<String, String[]> fonts, String keyPrefix,
                                      String property, String value) {
        int slot;
        if ("font-family".equals(property)) {
            slot = 0;
        } else if ("font-size".equals(property)) {
            slot = 1;
            // Validated here so the error is reported while the rule is being read.
            parseFontSize(value);
        } else {
            return false;
        }
        String[] spec = fonts.get(keyPrefix);
        if (spec == null) {
            spec = new String[2];
            fonts.put(keyPrefix, spec);
        }
        spec[slot] = value;
        return true;
    }

    /// Builds the font of every UIID state that declared a font property. A state that sets only
    /// one of family and size takes the other from what applies in that state, as the build-time
    /// compiler's flattening does: the matching state of the UIID, each `cn1-derive` parent and
    /// finally `*` in turn, then their unselected styles in the same order. A dark style looks at the dark keys
    /// of each UIID before the light ones.
    private void compileFonts(Hashtable theme, Hashtable<String, String[]> fonts) {
        // A font is a live platform object, so it can only be made with a running Display.
        // Without one -- a tool that only checks that a stylesheet compiles -- every
        // declaration has still been validated as it was read.
        if (!Display.isInitialized()) {
            return;
        }
        for (Map.Entry<String, String[]> entry : fonts.entrySet()) {
            String keyPrefix = entry.getKey();
            String[] spec = entry.getValue();
            String family = spec[0];
            String size = spec[1];
            int dot = keyPrefix.indexOf('.');
            String uiid = keyPrefix.substring(0, dot);
            String state = keyPrefix.substring(dot + 1);
            boolean dark = uiid.startsWith("$Dark");
            // The matching state along the whole derive chain comes before any unselected
            // style: the build-time flattener lays a parent's selected style over the child's
            // unselected one, so `Base:selected` beats `Child`.
            // `*` compiles to Component, the implicit parent of every UIID in the build-time
            // compiler, so it comes after the explicit derive chain.
            String own = dark ? uiid.substring(5) : uiid;
            // A font a previous compile into the same theme built, when this compile has no
            // spec for that key: its family and size stand in for the missing half.
            Font inherited = null;
            for (int pass = 0; pass < 2 && (family == null || size == null); pass++) {
                String lookup = pass == 0 ? state : "";
                if (pass == 1 && state.length() == 0) {
                    break;
                }
                // The state's own derive applies in both passes: its parent's unselected style
                // is what a state derived from it falls back to.
                ArrayList<String> chain = fontChain(theme, own, state, dark);
                for (int i = 0; i < chain.size() && (family == null || size == null); i++) {
                    String id = chain.get(i);
                    String[] fallbacks = dark
                            ? new String[]{"$Dark" + id + "." + lookup, id + "." + lookup}
                            : new String[]{id + "." + lookup};
                    for (String fallback : fallbacks) {
                        String[] other = fonts.get(fallback);
                        if (other != null) {
                            family = family == null ? other[0] : family;
                            size = size == null ? other[1] : size;
                        } else if (inherited == null) {
                            Object existing = theme.get(fallback + Style.FONT);
                            if (existing instanceof Font && ((Font) existing).isTTFNativeFont()) {
                                inherited = (Font) existing;
                            }
                        }
                    }
                }
            }
            FontSize parsed = size == null ? null : parseFontSize(size);
            Font font;
            if (inherited != null && family == null && parsed != null && parsed.unit >= 0) {
                font = inherited.derive(fontSizeInPixels(parsed), inherited.getStyle());
            } else {
                if (inherited != null && family != null && parsed == null) {
                    parsed = new FontSize();
                    parsed.unit = Style.UNIT_TYPE_PIXELS;
                    parsed.value = inherited.getPixelSize();
                }
                font = createFont(family, parsed);
            }
            theme.put(keyPrefix + Style.FONT, font);
        }
    }

    /// The UIIDs a font in `state` inherits from, nearest first. At every hop a state that
    /// declares its own `cn1-derive` (`Child:selected{cn1-derive:Base}`) follows that instead of
    /// the UIID's. `*` is not part of it: the build-time compiler ignores universal rules.
    private ArrayList<String> fontChain(Hashtable theme, String id, String state, boolean dark) {
        ArrayList<String> chain = new ArrayList<String>();
        while (id != null && !chain.contains(id)) {
            chain.add(id);
            Object stateDerive = null;
            if (state.length() > 0) {
                stateDerive = dark ? theme.get("$Dark" + id + "." + state + "derive") : null;
                if (stateDerive == null) {
                    stateDerive = theme.get(id + "." + state + "derive");
                }
            }
            if (stateDerive instanceof String) {
                id = (String) stateDerive;
            } else {
                id = hoverBase(theme, id, dark);
                if (id == null) {
                    id = anyStateDerive(theme, chain.get(chain.size() - 1), dark);
                }
            }
        }
        return chain;
    }

    /// The build-time compiler applies a `cn1-derive` declared in any state to the owning UIID,
    /// so `Child:selected{cn1-derive:Base}` is also where `Child:pressed` inherits from.
    private String anyStateDerive(Hashtable theme, String id, boolean dark) {
        String recorded = dark ? ownerDerives.get("$Dark" + id) : null;
        if (recorded == null) {
            recorded = ownerDerives.get(id);
        }
        if (recorded != null) {
            return recorded;
        }
        // A derive a previous compile into the same theme stored: its order is gone.
        String[] states = {"sel#", "press#", "dis#", "hover#"};
        for (int pass = dark ? 0 : 1; pass < 2; pass++) {
            String prefix = pass == 0 ? "$Dark" + id + "." : id + ".";
            for (String state : states) {
                Object derive = theme.get(prefix + state + "derive");
                if (derive instanceof String) {
                    return (String) derive;
                }
            }
        }
        return null;
    }

    private Font createFont(String familyList, FontSize size) {
        String ttfName = "native:MainRegular";
        boolean monospace = false;
        if (familyList != null) {
            String[] families = splitOnComma(familyList);
            for (String entry : families) {
                String family = unquote(entry);
                if (family.startsWith("native:")) {
                    ttfName = family;
                    break;
                }
                if ("monospace".equalsIgnoreCase(family)) {
                    monospace = true;
                    break;
                }
                if (isGenericFamily(family)) {
                    break;
                }
                // Any other name has no @font-face here to resolve it, so it is passed over
                // the way the build-time compiler passes over a family it cannot find.
            }
        }
        int systemSize = size == null ? Font.SIZE_MEDIUM : size.systemSize;
        if (monospace) {
            return Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, systemSize);
        }
        Font fallback = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, systemSize);
        float pixels = size == null || size.unit < 0 ? fallback.getHeight() : fontSizeInPixels(size);
        Font ttf;
        try {
            ttf = Font.createTrueTypeFont(ttfName, ttfName);
        } catch (RuntimeException err) {
            ttf = null;
        }
        // The same fallback a resource file takes for a truetype font the platform cannot load.
        return ttf == null ? fallback : ttf.derive(pixels, Font.STYLE_PLAIN);
    }

    private static boolean isGenericFamily(String family) {
        String[] generic = {"sans-serif", "serif", "times", "courier", "arial", "cursive", "fantasy", "system-ui"};
        for (String g : generic) {
            if (g.equalsIgnoreCase(family)) {
                return true;
            }
        }
        return false;
    }

    /// Converts the way `Resources` converts a truetype size read from a resource file.
    private float fontSizeInPixels(FontSize size) {
        switch (size.unit) {
            case Style.UNIT_TYPE_DIPS:
                return Display.getInstance().convertToPixels((int) (size.value * 10), true) / 10.0f;
            case Style.UNIT_TYPE_REM:
                return Font.getDefaultFont().getHeight() * size.value;
            case Style.UNIT_TYPE_VW:
                return CN.getDisplayWidth() * size.value / 100f;
            case Style.UNIT_TYPE_VH:
                return CN.getDisplayHeight() * size.value / 100f;
            case Style.UNIT_TYPE_VMIN:
                return Math.min(CN.getDisplayWidth(), CN.getDisplayHeight()) * size.value / 100f;
            case Style.UNIT_TYPE_VMAX:
                return Math.max(CN.getDisplayWidth(), CN.getDisplayHeight()) * size.value / 100f;
            default:
                return size.value;
        }
    }

    /// Parses a `font-size` with the build-time compiler's conversions: `pt`, `cm` and `in` are
    /// millimetres, a percentage scales the medium (3mm), small (2mm) or large (4mm) size, and
    /// a keyword selects that system font size.
    private FontSize parseFontSize(String value) {
        String v = value.trim();
        FontSize out = new FontSize();
        // `smaller` and `larger` are relative to the inherited size, which the build-time
        // compiler does not implement; they fall through to the unsupported-size error.
        if ("small".equalsIgnoreCase(v) || "x-small".equalsIgnoreCase(v) || "xx-small".equalsIgnoreCase(v)) {
            out.systemSize = Font.SIZE_SMALL;
            return out;
        }
        if ("large".equalsIgnoreCase(v) || "x-large".equalsIgnoreCase(v) || "xx-large".equalsIgnoreCase(v)) {
            out.systemSize = Font.SIZE_LARGE;
            return out;
        }
        if ("medium".equalsIgnoreCase(v)) {
            return out;
        }
        if (v.endsWith("%")) {
            out.value = 3f * number(v.substring(0, v.length() - 1), value) / 100f;
            out.unit = Style.UNIT_TYPE_DIPS;
            return out;
        }
        // vmin before in, so the longer suffix wins.
        String unit = unitSuffix(v, new String[]{"px", "pt", "mm", "cm", "vmin", "in", "rem", "vw", "vh", "vmax"});
        if (unit == null) {
            throw new CSSSyntaxException("Unsupported font-size value: " + value);
        }
        float n = number(v.substring(0, v.length() - unit.length()), value);
        out.value = n;
        if ("px".equals(unit)) {
            out.unit = Style.UNIT_TYPE_PIXELS;
        } else if ("pt".equals(unit)) {
            out.unit = Style.UNIT_TYPE_DIPS;
            out.value = n / 72f * 25.4f;
        } else if ("mm".equals(unit)) {
            out.unit = Style.UNIT_TYPE_DIPS;
        } else if ("cm".equals(unit)) {
            out.unit = Style.UNIT_TYPE_DIPS;
            out.value = n * 10f;
        } else if ("in".equals(unit)) {
            out.unit = Style.UNIT_TYPE_DIPS;
            out.value = n * 25.4f;
        } else {
            out.unit = viewportUnit(unit);
        }
        return out;
    }

    /// The first of `units` that `value` ends with, ignoring case, or null.
    private static String unitSuffix(String value, String[] units) {
        for (String unit : units) {
            if (endsWithIgnoreCase(value, unit)) {
                return unit;
            }
        }
        return null;
    }

    private static byte viewportUnit(String unit) {
        if ("rem".equals(unit)) {
            return Style.UNIT_TYPE_REM;
        }
        if ("vw".equals(unit)) {
            return Style.UNIT_TYPE_VW;
        }
        if ("vh".equals(unit)) {
            return Style.UNIT_TYPE_VH;
        }
        if ("vmin".equals(unit)) {
            return Style.UNIT_TYPE_VMIN;
        }
        return Style.UNIT_TYPE_VMAX;
    }

    private static float number(String text, String original) {
        String t = text.trim();
        if (t.length() == 0) {
            throw new CSSSyntaxException("Missing number in: " + original);
        }
        try {
            return Float.parseFloat(t);
        } catch (NumberFormatException err) {
            throw new CSSSyntaxException("Invalid number in: " + original, err);
        }
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        int start = value.length() - suffix.length();
        return start >= 0 && value.regionMatches(true, start, suffix, 0, suffix.length());
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
            applyBox(theme, uiid + "." + statePrefix, property, value);
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
        if ("border".equals(property)) {
            String expanded = expandBorderShorthand(value);
            if (expanded.length() == 0) {
                return true;
            }
            if (borderCss.length() > 0) {
                borderCss.append(';');
            }
            borderCss.append(expanded);
            return true;
        }
        if (borderCss.length() > 0) {
            borderCss.append(';');
        }
        borderCss.append(property).append(':').append(value);
        return true;
    }

    private String expandBorderShorthand(String value) {
        String[] parts = splitOnWhitespace(value);
        if (parts.length == 0) {
            throw new CSSSyntaxException("border shorthand is missing value");
        }
        String width = null;
        String style = null;
        String color = null;
        for (String part : parts) {
            String token = part.trim().toLowerCase();
            if (token.length() == 0) {
                continue;
            }
            if (width == null && (token.endsWith("px") || token.endsWith("mm") || token.endsWith("pt") || token.endsWith("%") || "0".equals(token))) {
                width = part;
                continue;
            }
            if (style == null && ("none".equals(token) || "solid".equals(token) || "dashed".equals(token) || "dotted".equals(token))) {
                style = token;
                continue;
            }
            if (color == null) {
                normalizeHexColor(part);
                color = part;
                continue;
            }
            throw new CSSSyntaxException("Unsupported border shorthand token: " + part);
        }
        StringBuilder out = new StringBuilder();
        if (width != null) {
            out.append("border-width:").append(width);
        }
        if (style != null) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append("border-style:").append(style);
        }
        if (color != null) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append("border-color:").append(color);
        }
        return out.toString();
    }

    private String resolveVars(Hashtable theme, String value) {
        return resolveVars(theme, value, new ArrayList<String>());
    }

    /// Expands every `var(--x)`, including those inside the values it substitutes. `resolving`
    /// holds the names being expanded, so `a: var(--b); b: var(--a)` is an error instead of a
    /// compile that never returns.
    private String resolveVars(Hashtable theme, String value, ArrayList<String> resolving) {
        int varPos = value.indexOf("var(--");
        if (varPos < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder();
        int pos = 0;
        while (varPos > -1) {
            int end = value.indexOf(')', varPos);
            if (end < 0) {
                break;
            }
            String key = value.substring(varPos + "var(--".length(), end).trim();
            if (resolving.contains(key)) {
                throw new CSSSyntaxException("Cyclic variable reference: var(--" + key + ")");
            }
            Object replacement = theme.get("@" + key);
            String replaceValue = replacement == null ? "" : replacement.toString();
            // A quoted constant is text, also when it is reached through another constant.
            if (!DEFERRED_LITERAL.equals(deferredConstants.get("@" + key))) {
                resolving.add(key);
                replaceValue = resolveVars(theme, replaceValue, resolving);
                resolving.remove(resolving.size() - 1);
            }
            out.append(value.substring(pos, varPos)).append(replaceValue);
            pos = end + 1;
            varPos = value.indexOf("var(--", pos);
        }
        out.append(value.substring(pos));
        return out.toString();
    }

    private String[] selector(String selector) {
        String statePrefix = "";
        String uiid = selector.trim();

        int pseudoPos = uiid.indexOf(':');
        int classStatePos = uiid.indexOf('.');
        int statePos = -1;
        if (pseudoPos > -1 && classStatePos > -1) {
            statePos = Math.min(pseudoPos, classStatePos);
        } else if (pseudoPos > -1) {
            statePos = pseudoPos;
        } else if (classStatePos > -1) {
            statePos = classStatePos;
        }

        if (statePos > -1) {
            String pseudo = uiid.substring(statePos + 1).trim();
            uiid = uiid.substring(0, statePos).trim();
            statePrefix = statePrefix(pseudo);
        }
        if ("*".equals(uiid) || uiid.length() == 0) {
            uiid = "Component";
        }
        return new String[]{uiid, statePrefix};
    }

    private String statePrefix(String pseudo) {
        if ("unselected".equals(pseudo)) {
            return "";
        }
        if ("selected".equals(pseudo)) {
            return "sel#";
        }
        if ("pressed".equals(pseudo)) {
            return "press#";
        }
        if ("disabled".equals(pseudo)) {
            return "dis#";
        }
        // The desktop state. This runtime compiler is a separate implementation from the
        // build-time one in maven/css-compiler and shares none of its code, so a sheet using
        // .hover compiled at run time -- CSS live reload, a theme built by an application --
        // threw "Unsupported pseudo state" until it was taught the same prefix.
        if ("hover".equals(pseudo)) {
            return "hover#";
        }
        throw new CSSSyntaxException("Unsupported pseudo state: " + pseudo);
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

    /// Expands a one to four value `padding`/`margin` from CSS top/right/bottom/left order into
    /// the theme's top/bottom/left/right value order, and its unit array, which `Style` indexes
    /// by `Component.TOP`, `LEFT`, `BOTTOM`, `RIGHT`.
    private void applyBox(Hashtable theme, String keyPrefix, String property, String cssValue) {
        String[] parts = splitOnWhitespace(cssValue.trim());
        if (parts.length == 0 || parts.length > 4) {
            throw new CSSSyntaxException(property + " takes one to four values: " + cssValue);
        }
        Length top = length(parts[0]);
        Length right = parts.length > 1 ? length(parts[1]) : top;
        Length bottom = parts.length > 2 ? length(parts[2]) : top;
        Length left = parts.length > 3 ? length(parts[3]) : right;
        theme.put(keyPrefix + property, top.text + "," + bottom.text + "," + left.text + "," + right.text);
        String unitKey = "padding".equals(property) ? Style.PADDING_UNIT : Style.MARGIN_UNIT;
        theme.put(keyPrefix + unitKey, new byte[]{top.unit, left.unit, bottom.unit, right.unit});
    }

    /// One side of a box. `px`, `pt` and a bare number are pixels rounded to a whole number,
    /// which is what the build-time compiler stores: its `pt` is a density independent pixel,
    /// not a CSS point.
    private Length length(String value) {
        String v = value.trim();
        Length out = new Length();
        // No `%`: the build-time compiler rejects a percentage inset, so the preview must too.
        if (v.endsWith("%")) {
            throw new CSSSyntaxException("Unsupported unit for padding or margin: " + value);
        }
        String unit = unitSuffix(v, new String[]{"px", "pt", "mm", "cm", "rem", "vw", "vh", "vmin", "vmax"});
        // A bare number must be whole: the build-time compiler takes `1.5` for a dimension
        // missing its unit and fails the build.
        if (unit == null && v.indexOf('.') > -1) {
            throw new CSSSyntaxException("A unitless padding or margin must be a whole number: " + value);
        }
        float n = number(unit == null ? v : v.substring(0, v.length() - unit.length()), value);
        if (unit == null || "px".equals(unit) || "pt".equals(unit)) {
            out.unit = Style.UNIT_TYPE_PIXELS;
            n = Math.round(n);
        } else if ("mm".equals(unit)) {
            out.unit = Style.UNIT_TYPE_DIPS;
        } else if ("cm".equals(unit)) {
            out.unit = Style.UNIT_TYPE_DIPS;
            n = n * 10f;
        } else {
            out.unit = viewportUnit(unit);
        }
        String text = String.valueOf(n);
        out.text = text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        return out;
    }

    private Rule[] parseRulesWithMedia(String css) {
        ArrayList<Rule> out = new ArrayList<Rule>();
        parseRulesInto(css, out, false);
        return out.toArray(new Rule[out.size()]);
    }

    private void parseRulesInto(String css, ArrayList<Rule> out, boolean darkContext) {
        int pos = 0;
        int len = css.length();
        while (pos < len) {
            while (pos < len && Character.isWhitespace(css.charAt(pos))) {
                pos++;
            }
            if (pos >= len) {
                break;
            }
            int open = css.indexOf('{', pos);
            if (open < 0) {
                throw new CSSSyntaxException("Missing '{' in CSS rule near: " + css.substring(pos));
            }

            String selectors = css.substring(pos, open).trim();
            int close = findMatchingBrace(css, open);
            if (close < 0) {
                throw new CSSSyntaxException("Missing '}' for CSS rule: " + selectors);
            }

            if (isConstantsSelector(selectors)) {
                Rule constants = new Rule();
                constants.selector = selectors;
                constants.constants = true;
                constants.declarations = parseDeclarations(css.substring(open + 1, close));
                out.add(constants);
                pos = close + 1;
                continue;
            }
            if (selectors.startsWith("@media")) {
                String mediaQuery = selectors.substring("@media".length()).trim();
                boolean nextDarkContext = darkContext || isDarkModeMediaQuery(mediaQuery);
                parseRulesInto(css.substring(open + 1, close), out, nextDarkContext);
                pos = close + 1;
                continue;
            }
            if (selectors.length() == 0) {
                throw new CSSSyntaxException("Missing selector before '{'");
            }

            String body = css.substring(open + 1, close).trim();
            Declaration[] declarations = parseDeclarations(body);
            String[] selectorsList = splitOnChar(selectors, ',');
            for (String selectorEntry : selectorsList) {
                String selector = selectorEntry.trim();
                if (selector.length() == 0) {
                    throw new CSSSyntaxException("Empty selector in selector list: " + selectors);
                }
                Rule rule = new Rule();
                rule.selector = darkContext ? toDarkSelector(selector) : selector;
                rule.declarations = declarations;
                out.add(rule);
            }

            pos = close + 1;
        }
    }

    private int findMatchingBrace(String css, int openPos) {
        int depth = 0;
        int len = css.length();
        char quote = 0;
        for (int i = openPos; i < len; i++) {
            char c = css.charAt(i);
            // A brace inside a quoted value, such as a #Constants string, is text.
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isDarkModeMediaQuery(String mediaQuery) {
        String normalized = mediaQuery == null ? "" : mediaQuery.toLowerCase();
        return normalized.indexOf("prefers-color-scheme") > -1 && normalized.indexOf("dark") > -1;
    }

    private String toDarkSelector(String selector) {
        String trimmed = selector == null ? "" : selector.trim();
        if (trimmed.length() == 0 || ":root".equals(trimmed)) {
            return trimmed;
        }
        if (trimmed.startsWith("$Dark")) {
            return trimmed;
        }

        int pseudoPos = trimmed.indexOf(':');
        int classStatePos = trimmed.indexOf('.');
        int statePos = -1;
        if (pseudoPos > -1 && classStatePos > -1) {
            statePos = Math.min(pseudoPos, classStatePos);
        } else if (pseudoPos > -1) {
            statePos = pseudoPos;
        } else if (classStatePos > -1) {
            statePos = classStatePos;
        }
        String baseSelector = statePos > -1 ? trimmed.substring(0, statePos) : trimmed;
        String stateSelector = statePos > -1 ? trimmed.substring(statePos) : "";
        if ("*".equals(baseSelector) || baseSelector.length() == 0) {
            baseSelector = "Component";
        }
        return "$Dark" + baseSelector + stateSelector;
    }

    private String stripComments(String css) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        char quote = 0;
        while (i < css.length()) {
            char c = css.charAt(i);
            // Comment delimiters inside a quoted string are text.
            if (quote != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < css.length()) {
                    out.append(css.charAt(i + 1));
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                i++;
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '/' && i + 1 < css.length() && css.charAt(i + 1) == '*') {
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
        String[] segments = splitDeclarations(body);
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
            dec.name = trimmed.substring(0, colon).trim();
            dec.property = dec.name.toLowerCase();
            dec.value = trimmed.substring(colon + 1).trim();
            if (dec.property.length() == 0 || dec.value.length() == 0) {
                throw new CSSSyntaxException("Malformed declaration: " + trimmed);
            }
            out.add(dec);
        }
        return out.toArray(new Declaration[out.size()]);
    }

    /// Splits a rule body on `;`, except inside a quoted value such as `"a;b"`.
    private String[] splitDeclarations(String input) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ';') {
                out.add(input.substring(start, i));
                start = i + 1;
            }
        }
        out.add(input.substring(start));
        return out.toArray(new String[out.size()]);
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
        boolean constants;
    }

    private static class Declaration {
        /// The name as written; constants and custom properties are case sensitive.
        String name;
        /// The lower case name ordinary CSS properties are matched by.
        String property;
        String value;
        /// The last declaration of its name across every #Constants block.
        boolean winner;
    }

    private static class Length {
        String text;
        byte unit;
    }

    private static class FontSize {
        /// A `Style.UNIT_TYPE_*`, or -1 for a size given only as a system size.
        byte unit = -1;
        float value;
        int systemSize = Font.SIZE_MEDIUM;
    }
}
