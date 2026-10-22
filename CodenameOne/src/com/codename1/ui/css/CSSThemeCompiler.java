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
import java.util.HashSet;
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

    /// The quoted source of each string constant this compile declared. A style property that
    /// uses one gets the string token, not its text: the build-time compiler keeps the token's
    /// type, so `color: var(--accent)` with `--accent: "#f00"` is not a color there either.
    private Hashtable<String, String> literalTokens = new Hashtable<String, String>();
    /// `#Constants` entries this compile declared without the `--` prefix. The build-time
    /// compiler adds only `--name` declarations to its variables, so `var(--name)` does not see
    /// these.
    private HashSet<String> plainConstants = new HashSet<String>();
    /// Every custom property this compile has read so far, keyed by "@name", as written: the
    /// build-time compiler keeps its variables apart from the constants it exports. A variable
    /// holds its CSS spelling (`#f00`, not the exported `ff0000`, so a border can use it) and an
    /// alias holds `var(--base)` itself, resolved where it is used, so a later `--base` changes
    /// it while the exported `@alias` keeps the value it had where it was declared. A later
    /// plain `#Constants` entry or `:root` property of the same name changes only its own side.
    private Hashtable<String, String> cssVars = new Hashtable<String, String>();
    /// "@name" of every entry a `#Constants` block of this compile exported. The build-time
    /// compiler adds a custom property to its constants only inside `#Constants`, so a later
    /// `:root { --name: ... }` changes what `var(--name)` reads and leaves `@name` alone.
    private HashSet<String> exportedConstants = new HashSet<String>();

    /// `padding-left` and its kin, per `uiid.state#padding` key, top/right/bottom/left; null is
    /// a side not declared. Applied once every rule is read, over the box shorthand.
    private Hashtable<String, Length[]> boxSides = new Hashtable<String, Length[]>();

    /// The UIIDs (`$Dark` ones included) a rule of this compile styled, in first-seen order.
    private ArrayList<String> styledUiids = new ArrayList<String>();

    /// The style keys the build-time compiler carries from a UIID's unselected style into its
    /// selected, pressed and disabled ones. The font has its own pass in compileFonts.
    private static final String[] STATE_INHERITED_KEYS = {
        "fgColor", "fgAlpha", "bgColor", "transparency", "opacity", "padding", "padUnit", "margin",
        "marUnit", "align", "border", "bgImage"
    };
    /// While set, resolveVars does not see a constant this compile declares unless it holds it:
    /// one declared further down is not defined yet. A constant only an earlier compile into the
    /// same theme declared is visible.
    private Hashtable<String, String> declaredOnly;
    /// Every constant and custom property this compile declares, as "@name".
    private HashSet<String> declaredThisCompile = new HashSet<String>();
    /// Set by resolveVars when it met a var() naming an undefined variable with no fallback.
    private boolean unresolvedVar;

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
        literalTokens = new Hashtable<String, String>();
        plainConstants = new HashSet<String>();
        cssVars = new Hashtable<String, String>();
        exportedConstants = new HashSet<String>();
        declaredThisCompile = new HashSet<String>();
        for (Rule rule : rules) {
            for (Declaration declaration : rule.declarations) {
                if (rule.constants || rule.legacyConstants) {
                    String n = declaration.name;
                    if (rule.constants) {
                        // Decoded, as compileConstants keys it: `--a\62 c` declares `@abc`.
                        String decoded = decodeIdentifier(n);
                        if (decoded != null) {
                            n = decoded;
                        }
                    }
                    declaredThisCompile.add("@" + (n.startsWith("--") ? n.substring(2) : n));
                } else if (declaration.name.startsWith("--")) {
                    declaredThisCompile.add("@" + customPropertyName(declaration.name));
                }
            }
        }
        // Everything in source order, #Constants blocks included: the build-time parser resolves
        // var() in each declaration as it reads it, and a `--name` inside #Constants enters its
        // variable map only when that block is reached. A rule above the block does not see it.
        // The legacy `@constants` block is this compiler's own syntax, which the build-time
        // compiler does not read; it has always been applied before every rule, so a rule may
        // use it from above.
        for (Rule rule : rules) {
            if (rule.legacyConstants) {
                compileConstants(rule.declarations, theme, deferred, true);
            }
        }
        Hashtable<String, String[]> fonts = new Hashtable<String, String[]>();
        ownerDerives = new Hashtable<String, String>();
        styledUiids = new ArrayList<String>();
        boxSides = new Hashtable<String, Length[]>();
        for (Rule rule : rules) {
            if (rule.legacyConstants) {
                continue;
            }
            if (rule.constants) {
                compileConstants(rule.declarations, theme, deferred, false);
                continue;
            }
            applyRule(theme, resources, rule, fonts, deferred);
        }
        markLiteralAliases(theme, deferred);
        compileFonts(theme, fonts);
        applyBoxSides(theme);
        inheritUnselectedIntoStates(theme);
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
        // The build-time compiler also stores every `Default.*` property unprefixed: the
        // theme's root keys, which UIManager's default style reads.
        ArrayList<String> defaults = new ArrayList<String>();
        for (Object k : theme.keySet()) {
            if (k instanceof String && ((String) k).startsWith("Default.")) {
                defaults.add((String) k);
            }
        }
        for (String k : defaults) {
            theme.put(k.substring("Default.".length()), theme.get(k));
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
                String next = aliasTarget((String) value);
                if (next == null) {
                    break;
                }
                target = next;
                // Across separate compile() calls the theme keeps only final values, and a quoted
                // "#abc" is stored exactly like an unquoted #abc custom property, so whether an
                // earlier value was quoted is not recoverable from it. Compiling the stylesheet
                // whole, as live reload and the GUI builder do, keeps the exact typing.
                // A constant a previous compile stored is a final value: a var() still in it was
                // quoted text there, and must not be expanded through the alias. Any other earlier
                // value is classified like a fresh one, so `#abc` still becomes a color.
                Object earlierValue = deferred.get(target) == null ? theme.get(target) : null;
                boolean earlier = earlierValue instanceof String && ((String) earlierValue).indexOf("var(--") > -1;
                if (DEFERRED_LITERAL.equals(deferred.get(target)) || earlier) {
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
            unresolvedVar = false;
            String resolved = resolveVars(theme, (String) value);
            if (unresolvedVar) {
                // The build-time compiler keeps an undefined var() with no fallback as a function
                // its constants emitter has no branch for, so the built theme has no such
                // constant: the caller's own default applies, not an empty string.
                theme.remove(key);
            } else {
                theme.put(key, typedAlias(key, (String) value, resolved));
            }
        }
    }

    private void compileConstants(Declaration[] declarations, Hashtable theme,
                                  Hashtable<String, String> deferred, boolean legacy) {
        for (Declaration declaration : declarations) {
            String declName = declaration.name;
            if (!legacy) {
                // The build-time parser takes a declaration name only as an identifier, and
                // decoded: `1message` is not one, and `a\62 c` is `abc`.
                declName = decodeIdentifier(declaration.name);
                if (declName == null) {
                    throw new CSSSyntaxException("Invalid #Constants name: " + declaration.name);
                }
            }
            if (!legacy && declName.startsWith("--")) {
                // The build-time compiler adds the raw variable before it evaluates it, so
                // `--accent: var(--accent, #f00)` is a cycle there, which fails the build.
                cssVars.put("@" + declName.substring(2), declaration.value.trim());
            }
            String name = declName;
            // `#Constants { --accent: ...; }` is how the build-time compiler exports a
            // palette variable as the constant `@accent`.
            if (name.startsWith("--")) {
                name = name.substring(2);
            }
            String key = "@" + name;
            if (!legacy) {
                // Only #Constants is what the build-time compiler exports; a later :root still
                // replaces a legacy @constants entry, as it always has.
                exportedConstants.add(key);
            }
            if (legacy || declName.startsWith("--")) {
                plainConstants.remove(key);
            } else {
                plainConstants.add(key);
            }
            String raw = declaration.value.trim();
            String value = unquote(raw);
            deferred.put(key, DEFERRED_PLAIN);
            literalTokens.remove(key);
            boolean alias = raw.equals(value) && value.indexOf("var(--") > -1;
            // Of a chain such as `#f00 extra` the build-time compiler types the leading unit, so
            // that is what decides whether an `...Image` name is looked up as an image.
            String typed = !legacy && raw.equals(value) && !alias ? leadingToken(value) : value;
            if (name.endsWith("Image") && (!raw.equals(value) || (!isColorLiteral(typed) && !isCssNumber(typed)))) {
                // The build-time compiler looks an image up only for a string or identifier
                // token; a color or number named `...Image` is stored as a color or number. An alias is
                // resolved where it is declared, as below, so a later redefinition of its
                // target does not change the image.
                if (alias && !legacy && declaration.snapshot == null && !declaration.dropped) {
                    declaration.snapshot = resolveAtDeclaration(theme, key, value, deferred);
                    declaration.dropped = declaration.snapshot == null;
                    declaration.snapshotKind = DEFERRED_IMAGE;
                }
                if (declaration.dropped) {
                    theme.remove(key);
                    deferred.remove(key);
                    continue;
                }
                boolean fromText = false;
                if (alias && declaration.snapshot != null) {
                    value = declaration.snapshot;
                    // An alias of a quoted constant names the image by text, as written, and so
                    // does a quoted fallback: `var(--missing, "my icon")` is `my icon`.
                    fromText = aliasesQuotedText(raw) || quotedFallbackApplies(raw);
                }
                if (alias && declaration.snapshot != null && !fromText
                        && (isColorLiteral(value) || isCssNumber(value))) {
                    // The alias resolved to a color or a number: the build-time compiler types
                    // the evaluated value, so it is that, not an image named `#f00`.
                    theme.put(key, isColorLiteral(value) ? constantColor(value) : buildConstant(name, value, value));
                    continue;
                }
                if (!fromText && raw.equals(value) || alias && !fromText) {
                    if (!legacy) {
                        // Of a chain such as `icon extra` the build-time compiler looks up the
                        // leading unit alone.
                        value = leadingToken(value);
                        if (isUriOrDimension(value)) {
                            // The build-time constants emitter looks an image up only for a
                            // string or identifier; `url(icon.png)` or `2mm` is silently left out.
                            theme.remove(key);
                            deferred.remove(key);
                            continue;
                        }
                    }
                    // An identifier image name is looked up decoded: `my\ icon` is `my icon`.
                    String decoded = decodeIdentifier(value);
                    if (decoded != null) {
                        value = decoded;
                    }
                }
                deferred.put(key, DEFERRED_IMAGE);
                if (!raw.equals(value)) {
                    // A quoted image name stays a string token, so an alias of it names the
                    // whole `"my icon"`, not the leading `my` of a chain.
                    literalTokens.put(key, raw);
                }
            } else if (!raw.equals(value)) {
                deferred.put(key, DEFERRED_LITERAL);
                literalTokens.put(key, raw);
            } else if (isColorLiteral(value) || !legacy && isColorLiteral(leadingToken(value))) {
                if (!legacy) {
                    // `rgb(255, 0, 0) extra` is a chain whose leading unit is the color, and the
                    // build-time constants emitter writes only that unit.
                    value = leadingToken(value);
                }
                // Stored as plain hex, as the build-time compiler stores it: color constants
                // are read with Integer.parseInt(value, 16), which a leading '#' would break.
                // Only `#hex` and `rgb()`/`rgba()` are color tokens here; a keyword such as `red`
                // or `transparent` is an identifier and is stored as written, as in the build.
                // testColorKeywordConstantStaysAnIdentifier holds that.
                value = constantColor(value);
            } else if (value.indexOf("var(--") > -1) {
                String kind = DEFERRED_VAR;
                if (!legacy && declaration.snapshot == null && !declaration.dropped) {
                    String target = aliasTarget(value);
                    String targetKind = target == null ? null : deferred.get(target);
                    // Text when the target is, through aliases or a redefinition of an exported
                    // constant, or when an undefined target's fallback is a quoted string.
                    declaration.snapshotKind = DEFERRED_LITERAL.equals(targetKind) || DEFERRED_TEXT.equals(targetKind)
                            || aliasesQuotedText(value) || quotedFallbackApplies(value)
                            ? DEFERRED_LITERAL : DEFERRED_VAR;
                    declaration.snapshot = resolveAtDeclaration(theme, key, value, deferred);
                    declaration.dropped = declaration.snapshot == null;
                    if (!declaration.dropped && declaration.snapshot.indexOf("var(--") > -1) {
                        // Fully resolved here, so a var() still in it came from text.
                        declaration.snapshotKind = DEFERRED_LITERAL;
                    }
                }
                if (declaration.dropped) {
                    theme.remove(key);
                    deferred.remove(key);
                    continue;
                }
                // A legacy `@constants` alias is not frozen here: the build-time compiler has no
                // `@constants` block to match, so it keeps resolving after every rule, as it
                // always has, and `:root { --base: blue; }` later still reaches `alias`.
                if (declaration.snapshot != null) {
                    value = declaration.snapshot;
                    kind = declaration.snapshotKind;
                }
                deferred.put(key, kind);
            } else if (!legacy) {
                // The build-time compiler writes string, identifier, number and color constants
                // only; anything else (`50%`, `2mm`, `url(a.png)`) is dropped from the built
                // theme without a word, so a preview that showed it would show a constant the
                // application never gets. Of a chain such as `foo bar` it writes the leading unit
                // alone. The legacy `@constants` block never reaches the build-time compiler and
                // keeps such values.
                String first = leadingToken(value);
                String unquotedFirst = unquote(first);
                if (first.length() < value.length() && !unquotedFirst.equals(first)) {
                    value = unquotedFirst;
                } else if (first.length() < value.length() && isColorLiteral(first)) {
                    value = constantColor(first);
                } else {
                    value = buildConstant(name, first, first);
                }
            }
            theme.put(key, value);
        }
    }

    /// The first whitespace- or comma-separated token of `value`, skipping quoted text and
    /// parentheses: `foo bar` is `foo`, `rgb(1, 2, 3) x` is `rgb(1, 2, 3)`.
    private static String leadingToken(String value) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\\') {
                // An escaped character, `my\ icon`'s space among them, is part of the token, and
                // so is the one whitespace ending a hex escape (`\41 x` is `Ax`).
                int j = i + 1;
                while (j < value.length() && j - i <= 6 && hexDigit(value.charAt(j)) >= 0) {
                    j++;
                }
                if (j > i + 1 && skipEscapeTerminator(value, j) > j) {
                    i = skipEscapeTerminator(value, j) - 1;
                } else {
                    i = j > i + 1 ? j - 1 : i + 1;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && (c == ',' || c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f')) {
                return value.substring(0, i);
            }
        }
        return value;
    }

    /// A constant that is one var() takes its value's token type, a fallback's too: the
    /// build-time compiler stores the string `"hello"` as hello and the number 1.50 as 1.5, as
    /// it would written directly.
    private String typedAlias(String key, String original, String resolved) {
        String sole = original.trim();
        if (sole.startsWith("var(") && closingParen(sole, "var(".length()) == sole.length() - 1) {
            if (aliasesQuotedText(sole)) {
                // The target is a string token whose text resolveVars() already decoded:
                // `--code: "001"` is copied as 001, not read again as the number 1, and a
                // quote character inside it is part of the text.
                return resolved;
            }
            String text = resolved.trim();
            String unquoted = unquote(text);
            if (!unquoted.equals(text)) {
                return unquoted;
            }
            if (isCssNumber(text)) {
                return buildConstant(key.substring(1), text, text);
            }
        }
        return resolved;
    }

    /// Resolves a `#Constants` alias where it is declared, as the build-time compiler does: only
    /// names declared above it are seen, so `early: var(--later)` is undefined however `later` is
    /// declared afterwards. Returns null, and the constant is left out, when a reference stays
    /// undefined without a fallback: the build-time compiler keeps such a var() as a function its
    /// constants emitter drops.
    private String resolveAtDeclaration(Hashtable theme, String key, String value, Hashtable<String, String> deferred) {
        String own = deferred.remove(key);
        declaredOnly = deferred;
        unresolvedVar = false;
        try {
            String resolved = resolveVars(theme, value);
            if (unresolvedVar) {
                return null;
            }
            String text = resolved.trim();
            String first = leadingToken(text);
            if (first.length() < text.length() && !aliasesQuotedText(value)) {
                // `pair: var(--x) bar` is a chain of lexical units, and the build-time
                // constants emitter writes only the leading one: `@pair` is `foo`.
                String unquoted = unquote(first);
                if (!unquoted.equals(first)) {
                    return unquoted;
                }
                return isCssNumber(first) ? buildConstant(key.substring(1), first, first) : first;
            }
            String typed = typedAlias(key, value, resolved);
            if (typed.equals(text) && !isCssNumber(text) && !isColorLiteral(text) && isUriOrDimension(text)) {
                // `var(--missing, 50%)` is the percentage token itself, which the build-time
                // emitter leaves out; it is rejected here exactly as `progress: 50%` written
                // directly is, so the preview never shows a constant the application lacks.
                // An `...Image` constant is the exception: a direct `navImage: url(icon.png)` is
                // left out silently, so its fallback is too.
                if (key.endsWith("Image")) {
                    return null;
                }
                buildConstant(key.substring(1), text, text);
            }
            return typed;
        } finally {
            declaredOnly = null;
            if (own != null) {
                deferred.put(key, own);
            }
        }
    }

    /// A `url()` token or a number with a unit: neither is a string or identifier token.
    private static boolean isUriOrDimension(String token) {
        if (token.regionMatches(true, 0, "url(", 0, 4)) {
            return true;
        }
        int i = token.startsWith("+") || token.startsWith("-") ? 1 : 0;
        return i < token.length() && (token.charAt(i) >= '0' && token.charAt(i) <= '9'
                || token.charAt(i) == '.' && i + 1 < token.length()
                && token.charAt(i + 1) >= '0' && token.charAt(i + 1) <= '9');
    }

    /// `--a\62 c` as the name `abc`, decoded as the build-time parser reads it.
    private static String customPropertyName(String property) {
        String decoded = decodeIdentifier(property);
        return (decoded != null ? decoded : property).substring(2);
    }

    /// Whether `value` is `var(--x, "text")` with `x` not declared so far: the fallback is then
    /// the value, and it keeps its string type.
    private boolean quotedFallbackApplies(String value) {
        String v = value.trim();
        String target = aliasTarget(v);
        if (target == null || cssVars.get(target) != null) {
            return false;
        }
        String inner = v.substring("var(".length(), v.length() - 1);
        int comma = topLevelComma(inner);
        if (comma < 0) {
            return false;
        }
        String fallback = inner.substring(comma + 1).trim();
        // The fallback that applies keeps its lexical type through any nesting:
        // `var(--missing, var(--text))` is text when `--text` is.
        return !unquote(fallback).equals(fallback) || aliasesQuotedText(fallback)
                || quotedFallbackApplies(fallback);
    }

    /// Whether `value` is an alias that ends, through every intermediate alias, at a quoted
    /// string: `--a: var(--name)` keeps the string `--name` was, as the build-time compiler keeps
    /// the lexical unit, so `navImage: var(--a)` names the image `my icon`, not `my`.
    private boolean aliasesQuotedText(String value) {
        String target = aliasTarget(value);
        ArrayList<String> seen = new ArrayList<String>();
        while (target != null && literalTokens.get(target) == null && !seen.contains(target)) {
            seen.add(target);
            String next = cssVars.get(target);
            target = next == null ? null : aliasTarget(next);
        }
        return target != null && literalTokens.get(target) != null;
    }

    /// Past the one whitespace that may end a hex escape: any CSS whitespace, with CR LF as one.
    private static int skipEscapeTerminator(String value, int j) {
        if (j >= value.length()) {
            return j;
        }
        char c = value.charAt(j);
        if (c == '\r' && j + 1 < value.length() && value.charAt(j + 1) == '\n') {
            return j + 2;
        }
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' ? j + 1 : j;
    }

    /// `@x` for a value that is exactly `var(--x)` or `var(--x, fallback)`, else null.
    private static String aliasTarget(String value) {
        String v = value.trim();
        if (!v.startsWith("var(--") || closingParen(v, "var(".length()) != v.length() - 1) {
            return null;
        }
        String inner = v.substring("var(--".length(), v.length() - 1);
        int comma = topLevelComma(inner);
        String name = (comma < 0 ? inner : inner.substring(0, comma)).trim();
        // Decoded, as resolveVars looks it up: `var(--a\62 c)` is `@abc`.
        String decoded = decodeIdentifier("--" + name);
        return "@" + (decoded != null ? decoded.substring(2) : name);
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
            String[] parts = colorArguments(value.substring(5, value.length() - 1), value);
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

    private static String derivedUiid(String value) {
        String unquoted = unquote(value);
        if (!unquoted.equals(value)) {
            return unquoted;
        }
        String decoded = decodeIdentifier(value);
        return decoded != null ? decoded : value;
    }

    private static boolean isConstantsSelector(String selector) {
        if ("@constants".equalsIgnoreCase(selector)) {
            return true;
        }
        if (!selector.startsWith("#")) {
            return false;
        }
        // The build-time parser decodes the ID before its special-selector check, so
        // `#\\43 onstants` is `#Constants` too.
        String decoded = decodeIdentifier(selector.substring(1));
        return "Constants".equalsIgnoreCase(decoded != null ? decoded : selector.substring(1));
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
            if (c == '\n' || c == '\r' || c == '\f') {
                // A CSS string cannot hold an unescaped newline: the build-time parser rejects
                // the declaration, so the built theme would not have it.
                throw new CSSSyntaxException("Unescaped newline in string: " + value);
            }
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
            while (digits < 6 && i + digits < end && hexDigit(v.charAt(i + digits)) > -1) {
                code = code * 16 + hexDigit(v.charAt(i + digits));
                digits++;
            }
            if (digits == 0) {
                out.append(c);
                continue;
            }
            // CSS: one to six hex digits name a code point, and one CSS whitespace after them
            // (space, tab, LF, FF, CR or CR LF) ends it; an em space (U+2003) after `\\41` is kept.
            i += digits - 1;
            if (i + 1 < end) {
                i = Math.min(end, skipEscapeTerminator(v, i + 1)) - 1;
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
        boolean universal = rule.universal;
        String uiid = selectorParts[0];
        String statePrefix = selectorParts[1];
        if (!universal) {
            styledUiids.add(uiid);
        }
        StringBuilder borderCss = new StringBuilder();

        for (int i = 0; i < rule.declarations.length; i++) {
            Declaration declaration = rule.declarations[i];
            String property = declaration.property;
            // A custom property takes effect where it is declared, also within a rule: the
            // build-time parser resolves each declaration as it reads it.
            if (applyThemeConstantProperty(theme, declaration.name, declaration.value, deferred)) {
                continue;
            }
            String value = resolveVars(theme, declaration.value, new ArrayList<String>(), true);
            if ("cn1-derive".equals(property)) {
                // The build-time compiler reads the target with getStringValue(): `"Base"` and
                // `B\61se` both name the UIID `Base`.
                value = derivedUiid(value.trim());
            }
            if (universal) {
                // The build-time compiler ignores universal rules entirely (CSSTheme.apply()
                // returns before any element), so `*` sets no style property and no derive
                // there; its custom properties, handled above, still count. Nothing is validated
                // either: `* { font-size: smaller; }` builds.
                continue;
            }
            if ("cn1-derive".equals(property) && !"press#".equals(statePrefix)) {
                // The build-time compiler's Element.setParent() promotes a derive to the owning
                // UIID from every state but pressed.
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
        String key = "@" + customPropertyName(property);
        String raw = value.trim();
        String unquoted = unquote(raw);
        cssVars.put(key, raw);
        if (exportedConstants.contains(key)) {
            // Only the variable changes; the constant #Constants exported keeps its value. Its
            // token type changes with it, so a later alias of a now quoted `--x` is text.
            if (!raw.equals(unquoted)) {
                literalTokens.put(key, raw);
            } else {
                literalTokens.remove(key);
            }
            return true;
        }
        plainConstants.remove(key);
        // An unquoted var() in it is resolved where it is used, against the final values.
        deferred.put(key, DEFERRED_PLAIN);
        literalTokens.remove(key);
        if (!raw.equals(unquoted)) {
            deferred.put(key, DEFERRED_LITERAL);
            literalTokens.put(key, raw);
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
            if (value.trim().length() == 0) {
                // `var(--optional-size)` left undefined: the build-time compiler keeps the
                // unresolved function and its font falls back to the default size, so the
                // declaration is passed over rather than rejecting the stylesheet.
                return true;
            }
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
    ///
    /// Only the states this compile declares a font property for are built. A font an earlier
    /// compile into the same theme built is a finished Font, and which parent its family came
    /// from is not stored, so a later compile that only changes a `cn1-derive` cannot tell which
    /// earlier fonts depended on it. Compiling the stylesheet whole, as live reload and the GUI
    /// builder do, rebuilds every font.
    private void compileFonts(Hashtable theme, Hashtable<String, String[]> fonts) {
        // A font is a live platform object, so it can only be made with a running Display.
        // Without one -- a tool that only checks that a stylesheet compiles -- every
        // declaration has still been validated as it was read.
        if (!Display.isInitialized()) {
            return;
        }
        // The build-time compiler writes a UIID's font into every state, and UIManager does not
        // fall back from `sel#font` to `font`: `Label { font-family: monospace; }` alone would
        // be monospace unselected and the system face once selected. A state with no font of
        // its own gets an empty spec, which the unselected pass below fills from `Label`.
        String[] states = {"sel#", "press#", "dis#"};
        for (String keyPrefix : new ArrayList<String>(fonts.keySet())) {
            if (!keyPrefix.endsWith(".") || keyPrefix.indexOf('.') != keyPrefix.length() - 1) {
                continue;
            }
            for (String state : states) {
                if (!fonts.containsKey(keyPrefix + state)) {
                    fonts.put(keyPrefix + state, new String[2]);
                }
            }
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
                int[] normalFrom = {Integer.MAX_VALUE, Integer.MAX_VALUE};
                ArrayList<String> chain = fontChain(theme, own, state, dark, normalFrom);
                for (int i = 0; i < chain.size() && (family == null || size == null); i++) {
                    String id = chain.get(i);
                    String lookupHere = i >= normalFrom[0] ? "" : lookup;
                    // A dark rule is its own `$Dark` element in the build-time compiler, with no
                    // link to the light UIID, so a dark font never takes the light one's half.
                    String[] fallbacks = dark && i < normalFrom[1]
                            ? new String[]{"$Dark" + id + "." + lookupHere}
                            : new String[]{id + "." + lookupHere};
                    for (String fallback : fallbacks) {
                        String[] other = fonts.get(fallback);
                        if (other != null) {
                            family = family == null ? other[0] : family;
                            size = size == null ? other[1] : size;
                        }
                        // Also when this compile has a spec for the key: a recompile of the same
                        // state that sets only one field keeps the other from the earlier font.
                        if (inherited == null) {
                            Object existing = theme.get(fallback + Style.FONT);
                            if (existing instanceof Font) {
                                inherited = (Font) existing;
                            }
                        }
                    }
                }
            }
            // A system font keeps only what it can say: monospace, its system size and its style,
            // so a size-only rule over a bold system font stays bold.
            int style = Font.STYLE_PLAIN;
            if (inherited != null && !inherited.isTTFNativeFont()) {
                style = inherited.getStyle();
                if (family == null && inherited.getFace() == Font.FACE_MONOSPACE) {
                    family = "monospace";
                }
                if (size == null) {
                    size = inherited.getSize() == Font.SIZE_SMALL ? "small"
                            : inherited.getSize() == Font.SIZE_LARGE ? "large" : "medium";
                }
                inherited = null;
            }
            FontSize parsed = size == null ? null : parseFontSize(size);
            Font font;
            if (inherited != null && family == null && parsed != null && parsed.unit >= 0) {
                font = inherited.derive(fontSizeInPixels(parsed), inherited.getStyle());
            } else if (inherited != null && family == null && parsed != null) {
                // A keyword size (`small`, `medium`, `large`) keeps the inherited family too, at
                // the height of the system font of that size.
                font = inherited.derive(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,
                        parsed.systemSize).getHeight(), inherited.getStyle());
            } else {
                if (inherited != null && family != null && parsed == null) {
                    parsed = new FontSize();
                    parsed.unit = Style.UNIT_TYPE_PIXELS;
                    parsed.value = inherited.getPixelSize();
                }
                font = createFont(family, parsed, style);
            }
            theme.put(keyPrefix + Style.FONT, font);
        }
    }

    /// The build-time compiler's selected, pressed and disabled styles start from the UIID's
    /// unselected one, and the built theme holds every state's value. UIManager does not fall
    /// back from `sel#bgColor` to `bgColor`, so without this a selected `Label{color:#333}`
    /// would take the default selected color. A state keeps its own value, and one a derive
    /// target declares for that state: the build lays `Base:selected` over `Child`'s unselected
    /// style, and UIManager reaches `Base.sel#` through the derive by itself.
    private void inheritUnselectedIntoStates(Hashtable theme) {
        String[] states = {"sel#", "press#", "dis#"};
        String[] baseStates = {"sel", "press", "dis"};
        // What the stylesheet itself declared; a value this pass copies into a state is not a
        // declaration a derived UIID's state should stop at.
        Hashtable declared = new Hashtable(theme);
        // A derived UIID's states derive from the target's matching states, as the build-time
        // compiler writes them (`Child.sel#derive=Base.sel`): UIManager reads only the state's
        // own derive key, so without it a selected Child would start from the default style.
        for (Map.Entry<String, String> entry : ownerDerives.entrySet()) {
            String uiid = entry.getKey();
            for (int i = 0; i < states.length; i++) {
                if (theme.get(uiid + "." + states[i] + "derive") == null) {
                    theme.put(uiid + "." + states[i] + "derive", entry.getValue() + "." + baseStates[i]);
                }
            }
        }
        for (String uiid : styledUiids) {
            for (String key : STATE_INHERITED_KEYS) {
                Object value = theme.get(uiid + "." + key);
                if (value == null) {
                    continue;
                }
                for (String state : states) {
                    if (theme.get(uiid + "." + state + key) != null
                            || declared.get(uiid + "." + state + "derive") != null
                            || derivedStateDeclares(declared, uiid, state, key)) {
                        continue;
                    }
                    theme.put(uiid + "." + state + key, value);
                }
            }
        }
    }

    /// Whether a UIID `uiid` derives from, at any depth, declares `key` for `state`.
    private boolean derivedStateDeclares(Hashtable theme, String uiid, String state, String key) {
        boolean dark = uiid.startsWith("$Dark");
        ArrayList<String> seen = new ArrayList<String>();
        String id = uiid;
        while (!seen.contains(id)) {
            seen.add(id);
            String next = ownerDerives.get(id);
            if (next == null) {
                Object stored = theme.get(id + ".derive");
                next = stored instanceof String ? (String) stored : null;
            }
            if (next == null) {
                return false;
            }
            id = dark && !next.startsWith("$Dark") && theme.get("$Dark" + next + "." + state + key) != null
                    ? "$Dark" + next : next;
            if (theme.get(id + "." + state + key) != null) {
                return true;
            }
        }
        return false;
    }

    /// The UIIDs a font in `state` inherits from, nearest first. At every hop a state that
    /// declares its own `cn1-derive` (`Child:selected{cn1-derive:Base}`) follows that instead of
    /// the UIID's. `*` is not part of it: the build-time compiler ignores universal rules.
    /// `normalFrom[0]` is set to the first index past a pressed-only derive: the build-time
    /// compiler attaches `Child:pressed{cn1-derive:Base}` to `Base` itself, not `Base:pressed`,
    /// so from there on the chain is read in the unselected state. `normalFrom[1]` is the first
    /// index past a dark element's derive: `$DarkChild{cn1-derive:Base}` is attached to the
    /// light `Base` by name, so from there on the chain is light.
    private ArrayList<String> fontChain(Hashtable theme, String id, String state, boolean dark, int[] normalFrom) {
        ArrayList<String> chain = new ArrayList<String>();
        while (id != null && !chain.contains(id)) {
            chain.add(id);
            if (dark && chain.size() > 1) {
                dark = false;
                normalFrom[1] = chain.size() - 1;
            }
            // A derive this compile declared, in any state: the last one in source order is the
            // owning UIID's only parent in the build-time compiler, for every state.
            // The build-time compiler makes `$DarkChild` an element of its own with no link to
            // the light `Child`'s derive, so only a dark derive extends the dark chain.
            // Except a pressed state's own derive: `Child:pressed{cn1-derive:Base1}` is attached
            // to `Base1` directly, whatever the owner's normal derive says.
            String prefix = dark ? "$Dark" + id + "." : id + ".";
            if ("press#".equals(state) && normalFrom[0] == Integer.MAX_VALUE) {
                Object pressDerive = theme.get(prefix + "press#derive");
                if (pressDerive instanceof String) {
                    id = (String) pressDerive;
                    normalFrom[0] = chain.size();
                    continue;
                }
            }
            String owner = ownerDerives.get(dark ? "$Dark" + id : id);
            if (owner != null) {
                id = owner;
                continue;
            }
            // Otherwise a derive a previous compile into the same theme stored. For a dark
            // element only its own `$Dark` keys: a retained light derive is no link either.
            Object stateDerive = state.length() > 0 ? theme.get(prefix + state + "derive") : null;
            if (stateDerive instanceof String) {
                id = (String) stateDerive;
                if ("press#".equals(state) && normalFrom[0] == Integer.MAX_VALUE) {
                    normalFrom[0] = chain.size();
                }
            } else {
                Object base = theme.get(prefix + "derive");
                id = base instanceof String ? (String) base : null;
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
        String recorded = ownerDerives.get(dark ? "$Dark" + id : id);
        if (recorded != null) {
            return recorded;
        }
        // A derive a previous compile into the same theme stored: its order is gone. Pressed is
        // not among them: the build-time compiler does not promote a pressed derive.
        String[] states = {"sel#", "dis#", "hover#"};
        for (int pass = dark ? 0 : 1; pass < (dark ? 1 : 2); pass++) {
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

    private Font createFont(String familyList, FontSize size, int style) {
        String ttfName = "native:MainRegular";
        boolean monospace = false;
        if (familyList != null) {
            String[] families = splitOnComma(familyList);
            for (String entry : families) {
                String family = unquote(entry);
                if (family.equals(entry.trim())) {
                    // An identifier is read decoded, as the build-time parser reads it:
                    // `native\:MainBold` is `native:MainBold`.
                    String decoded = decodeIdentifier(family);
                    if (decoded != null) {
                        family = decoded;
                    }
                }
                // The build-time compiler walks on past a native font, so a later one, or a later
                // monospace, wins; only a generic family or monospace ends the walk.
                if (family.startsWith("native:")) {
                    ttfName = family;
                    continue;
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
            return Font.createSystemFont(Font.FACE_MONOSPACE, style, systemSize);
        }
        Font fallback = Font.createSystemFont(Font.FACE_SYSTEM, style, systemSize);
        float pixels = size == null || size.unit < 0 ? fallback.getHeight() : fontSizeInPixels(size);
        Font ttf;
        try {
            ttf = Font.createTrueTypeFont(ttfName, ttfName);
        } catch (RuntimeException err) {
            ttf = null;
        }
        // The same fallback a resource file takes for a truetype font the platform cannot load.
        return ttf == null ? fallback : ttf.derive(pixels, style);
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
    /// `digits` unchanged, unless it uses exponent notation: `1e2px` is a dimension with the
    /// unit `e2px` to the build-time parser, which it does not recognise as a size.
    private static String fontDigits(String digits, String value) {
        if (digits.indexOf('e') > -1 || digits.indexOf('E') > -1) {
            throw new CSSSyntaxException("Exponent notation is not supported for font-size: " + value);
        }
        return digits;
    }

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
            out.value = 3f * number(fontDigits(v.substring(0, v.length() - 1), value), value) / 100f;
            out.unit = Style.UNIT_TYPE_DIPS;
            return out;
        }
        if (isCssNumber(v) && Float.parseFloat(v) == 0f) {
            // `0` is a valid length; the build-time compiler reads the bare number as no size
            // and keeps its default medium sizing.
            return out;
        }
        // vmin before in, so the longer suffix wins.
        String unit = unitSuffix(v, new String[]{"px", "pt", "mm", "cm", "vmin", "in", "rem", "vw", "vh", "vmax"});
        if (unit == null) {
            throw new CSSSyntaxException("Unsupported font-size value: " + value);
        }
        float n = number(fontDigits(v.substring(0, v.length() - unit.length()), value), value);
        out.value = n;
        if ("px".equals(unit)) {
            out.unit = Style.UNIT_TYPE_PIXELS;
            // The build-time compiler takes the integer part of a pixel size (12.5px is 12),
            // and the fraction only when the integer part is 0.
            if ((int) n != 0) {
                out.value = (int) n;
            }
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
        // CSS number syntax only: Float.parseFloat would also take 1f, 1d, 0x1p3, NaN and
        // Infinity, which the build-time parser reads as a number followed by a unit.
        if (!isCssNumber(t)) {
            throw new CSSSyntaxException("Invalid number in: " + original);
        }
        try {
            return Float.parseFloat(t);
        } catch (NumberFormatException err) {
            throw new CSSSyntaxException("Invalid number in: " + original, err);
        }
    }

    /// The value the build-time compiler stores for an unquoted `#Constants` token: a number
    /// as it serializes the parsed value (`1.50` is `1.5`, `001` is `1`), an identifier as
    /// written. Every other token type is one it drops, so it is rejected here.
    private static String buildConstant(String name, String raw, String value) {
        if (isCssNumber(value) && (value.indexOf('e') > -1 || value.indexOf('E') > -1)) {
            // The build-time parser reads `1e2` as a dimension, a token its constants emitter
            // drops, so the built theme has no such constant.
            throw new CSSSyntaxException("#Constants does not support exponent notation: "
                    + name + ": " + raw + " (write the number out, or quote it to store it as text)");
        }
        if (isCssNumber(value)) {
            boolean integer = value.indexOf('.') < 0 && value.indexOf('e') < 0 && value.indexOf('E') < 0;
            try {
                if (integer) {
                    return String.valueOf(Integer.parseInt(value.charAt(0) == '+' ? value.substring(1) : value));
                }
                String real = String.valueOf(Float.parseFloat(value));
                return real.endsWith(".0") ? real.substring(0, real.length() - 2) : real;
            } catch (NumberFormatException err) {
                throw new CSSSyntaxException("Invalid number in #Constants: " + name + ": " + raw, err);
            }
        }
        String ident = decodeIdentifier(value);
        if (ident != null) {
            return ident;
        }
        throw new CSSSyntaxException("#Constants supports strings, identifiers, numbers and colors only: "
                + name + ": " + raw + " (quote it to store it as text)");
    }

    /// The decoded value of a CSS identifier token, or null when `t` is not one: `-`? then a
    /// name-start character (a letter, `_`, `-`, any non-ASCII character or an escape), then name
    /// characters. An escape is a backslash and 1-6 hex digits (plus one optional whitespace) or
    /// a backslash and any other character, decoded as the build-time parser stores it.
    private static String decodeIdentifier(String t) {
        int n = t.length();
        StringBuilder out = new StringBuilder(n);
        int i = 0;
        boolean first = true;
        if (t.startsWith("--")) {
            // A custom property: anything may follow the double hyphen, `--1accent` included.
            out.append("--");
            i = 2;
            first = false;
        }
        while (i < n) {
            char c = t.charAt(i);
            if (c == '\\') {
                if (i + 1 >= n || t.charAt(i + 1) == '\n' || t.charAt(i + 1) == '\r' || t.charAt(i + 1) == '\f') {
                    return null;
                }
                int j = i + 1;
                int code = 0;
                while (j < n && j - i <= 6 && hexDigit(t.charAt(j)) >= 0) {
                    code = code * 16 + hexDigit(t.charAt(j));
                    j++;
                }
                if (j == i + 1) {
                    out.append(t.charAt(j));
                    i = j + 1;
                } else {
                    if (code == 0 || code > 0x10ffff || (code >= 0xd800 && code <= 0xdfff)) {
                        code = 0xfffd;
                    }
                    if (code > 0xffff) {
                        code -= 0x10000;
                        out.append((char) (0xd800 + (code >> 10)));
                        out.append((char) (0xdc00 + (code & 0x3ff)));
                    } else {
                        out.append((char) code);
                    }
                    j = skipEscapeTerminator(t, j);
                    i = j;
                }
                first = false;
                continue;
            }
            boolean digit = c >= '0' && c <= '9';
            if (!isNameChar(c) || (first && digit)) {
                return null;
            }
            if (first && c == '-' && out.length() == 0) {
                // A leading '-' is followed by the real name start.
                out.append(c);
                i++;
                if (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                    return null;
                }
                continue;
            }
            out.append(c);
            first = false;
            i++;
        }
        return first ? null : out.toString();
    }

    /// `[+-]? digits`.
    private static boolean isIntegerSpelling(String t) {
        int i = t.length() > 0 && (t.charAt(0) == '+' || t.charAt(0) == '-') ? 1 : 0;
        if (i >= t.length()) {
            return false;
        }
        for (; i < t.length(); i++) {
            if (t.charAt(i) < '0' || t.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /// `[+-]? (digits | digits? . digits) ([eE] [+-]? digits)?`, the CSS number token.
    private static boolean isCssNumber(String t) {
        int i = 0;
        int n = t.length();
        if (i < n && (t.charAt(i) == '+' || t.charAt(i) == '-')) {
            i++;
        }
        int intDigits = 0;
        while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i++;
            intDigits++;
        }
        int fracDigits = 0;
        if (i < n && t.charAt(i) == '.') {
            i++;
            while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                i++;
                fracDigits++;
            }
            if (fracDigits == 0) {
                return false;
            }
        }
        if (intDigits + fracDigits == 0) {
            return false;
        }
        if (i < n && (t.charAt(i) == 'e' || t.charAt(i) == 'E')) {
            i++;
            if (i < n && (t.charAt(i) == '+' || t.charAt(i) == '-')) {
                i++;
            }
            int expDigits = 0;
            while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                i++;
                expDigits++;
            }
            if (expDigits == 0) {
                return false;
            }
        }
        return i == n;
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        int start = value.length() - suffix.length();
        return start >= 0 && value.regionMatches(true, start, suffix, 0, suffix.length());
    }

    private boolean applySimpleThemeProperty(Hashtable theme, String uiid, String statePrefix, String property, String value) {
        if ("color".equals(property)) {
            theme.put(uiid + "." + statePrefix + "fgColor", normalizeHexColor(value));
            // As the build-time compiler's getThemeFgAlpha(): `transparent` and an rgba() alpha
            // become fgAlpha, any other color leaves it unset.
            int alpha = colorAlpha(value);
            if (alpha >= 0) {
                theme.put(uiid + "." + statePrefix + "fgAlpha", Integer.valueOf(alpha));
            }
            return true;
        }
        if ("background-color".equals(property)) {
            theme.put(uiid + "." + statePrefix + "bgColor", normalizeHexColor(value));
            // As getThemeTransparency(): `transparent` is 0, an rgba() its alpha, anything else
            // opaque. A transparent background has to say so: the UIID's default may be opaque.
            int alpha = colorAlpha(value);
            theme.put(uiid + "." + statePrefix + "transparency", String.valueOf(alpha >= 0 ? alpha : 255));
            return true;
        }
        if ("opacity".equals(property)) {
            float opacity;
            try {
                opacity = Float.parseFloat(value.trim());
            } catch (NumberFormatException err) {
                throw new CSSSyntaxException("Unsupported opacity value: " + value, err);
            }
            // As getThemeOpacity(): the fraction scaled to 0-255 and truncated.
            theme.put(uiid + "." + statePrefix + "opacity", String.valueOf((int) (opacity * 255.0)));
            return true;
        }
        if ("padding".equals(property) || "margin".equals(property)) {
            applyBox(theme, uiid + "." + statePrefix, property, value);
            // A later shorthand replaces the sides declared before it.
            boxSides.remove(uiid + "." + statePrefix + property);
            return true;
        }
        int side = boxSide(property);
        if (side >= 0) {
            String box = property.startsWith("padding") ? "padding" : "margin";
            String key = uiid + "." + statePrefix + box;
            Length[] sides = boxSides.get(key);
            if (sides == null) {
                sides = new Length[4];
                boxSides.put(key, sides);
            }
            sides[side] = length(value);
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
        if ("background-image".equals(property) && "none".equalsIgnoreCase(value.trim())) {
            // No image, as in the build-time compiler; CSSBorder has no `none` to parse.
            return true;
        }
        // CSSBorder reads only hex and rgb() colors; a keyword such as `red` is written as the
        // hex the build-time compiler resolves it to.
        value = namedColorsAsHex(value);
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
            String token = asciiLower(part.trim());
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
        return resolveVars(theme, value, new ArrayList<String>(), false);
    }

    /// Expands every `var(--x)`, including those inside the values it substitutes. `resolving`
    /// holds the names being expanded, so `a: var(--b); b: var(--a)` is an error instead of a
    /// compile that never returns.
    private String resolveVars(Hashtable theme, String value, ArrayList<String> resolving, boolean tokens) {
        int varPos = value.indexOf("var(--");
        if (varPos < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder();
        int pos = 0;
        while (varPos > -1) {
            int end = closingParen(value, varPos + "var(".length());
            if (end < 0) {
                // The build-time parser rejects the unmatched parenthesis.
                throw new CSSSyntaxException("Unterminated var() in: " + value);
            }
            // `var(--name, fallback)`: the build-time compiler takes the fallback when the
            // variable is not defined.
            String inner = value.substring(varPos + "var(--".length(), end);
            int comma = topLevelComma(inner);
            String key = (comma < 0 ? inner : inner.substring(0, comma)).trim();
            // Decoded, as the declaration was: `var(--a\62 c)` is `--abc`.
            String decodedKey = decodeIdentifier("--" + key);
            if (decodedKey != null) {
                key = decodedKey.substring(2);
            }
            String fallback = comma < 0 ? null : inner.substring(comma + 1).trim();
            if (resolving.contains(key)) {
                throw new CSSSyntaxException("Cyclic variable reference: var(--" + key + ")");
            }
            String variable = cssVars.get("@" + key);
            boolean own = variable != null;
            if (own) {
                // A quoted variable is text: its token in a style property, its text elsewhere.
                String text = unquote(variable);
                String spelling = tokens || text.equals(variable) ? variable : text;
                if (!text.equals(variable)) {
                    out.append(value.substring(pos, varPos)).append(spelling);
                } else {
                    resolving.add(key);
                    out.append(value.substring(pos, varPos)).append(resolveVars(theme, variable, resolving, tokens));
                    resolving.remove(resolving.size() - 1);
                }
                pos = end + 1;
                varPos = value.indexOf("var(--", pos);
                continue;
            }
            boolean shadowed = plainConstants.contains("@" + key);
            Object replacement = shadowed ? null
                    : (declaredOnly != null && !declaredOnly.containsKey("@" + key)
                            && declaredThisCompile.contains("@" + key)) ? null : theme.get("@" + key);
            if (replacement == null && fallback != null) {
                out.append(value.substring(pos, varPos)).append(resolveVars(theme, fallback, resolving, tokens));
                pos = end + 1;
                varPos = value.indexOf("var(--", pos);
                continue;
            }
            if (replacement == null) {
                unresolvedVar = true;
            }
            String replaceValue = replacement == null ? "" : replacement.toString();
            String token = tokens ? literalTokens.get("@" + key) : null;
            if (token != null) {
                replaceValue = token;
            }
            // A quoted constant is text, also when it is reached through another constant. So
            // is one a previous compile into the same theme left: it is already resolved, and
            // any var() still in it was quoted text there.
            String kind = deferredConstants.get("@" + key);
            // A quoted token is text whatever its kind (a quoted image name is one).
            if (kind != null && !DEFERRED_LITERAL.equals(kind) && token == null) {
                resolving.add(key);
                replaceValue = resolveVars(theme, replaceValue, resolving, tokens);
                resolving.remove(resolving.size() - 1);
            }
            out.append(value.substring(pos, varPos)).append(replaceValue);
            pos = end + 1;
            varPos = value.indexOf("var(--", pos);
        }
        out.append(value.substring(pos));
        return out.toString();
    }

    /// The index of the `)` closing the parenthesis opened just before `from`, skipping nested
    /// parentheses and quoted text; -1 when it is not closed.
    private static int closingParen(String value, int from) {
        int depth = 0;
        char quote = 0;
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\\') {
                // An escape outside a string: `foo\"bar` is one identifier, not a string start.
                i++;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;
    }

    /// The first comma outside parentheses and quotes, or -1.
    private static int topLevelComma(String value) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\\') {
                // An escape outside a string: `foo\"bar` is one identifier, not a string start.
                i++;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                return i;
            }
        }
        return -1;
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
        String value = cssColor == null ? "" : asciiLower(cssColor.trim());
        if (value.length() == 0) {
            throw new CSSSyntaxException("Color value cannot be empty");
        }
        if ("transparent".equals(value)) {
            return "000000";
        }

        if (value.startsWith("rgb(") || value.startsWith("rgba(")) {
            boolean alpha = value.startsWith("rgba(");
            if (!value.endsWith(")")) {
                throw new CSSSyntaxException("Malformed rgb() color: " + cssColor);
            }
            String[] parts = colorArguments(value.substring(alpha ? 5 : 4, value.length() - 1), cssColor);
            if (parts.length != (alpha ? 4 : 3)) {
                throw new CSSSyntaxException((alpha ? "rgba() must have exactly 4" : "rgb() must have exactly 3")
                        + " components: " + cssColor);
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
        String v = value == null ? "" : asciiLower(value.trim());
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

    /// The CSS named colors as the build-time compiler's Color table resolves them, name then
    /// hex. `transparent` is handled by the callers, which give it its alpha.
    private static final String[] NAMED_COLORS = {
        "aliceblue", "f0f8ff", "antiquewhite", "faebd7", "aqua", "00ffff",
        "aquamarine", "7fffd4", "azure", "f0ffff", "beige", "f5f5dc",
        "bisque", "ffe4c4", "black", "000000", "blanchedalmond", "ffebcd",
        "blue", "0000ff", "blueviolet", "8a2be2", "brown", "a52a2a",
        "burlywood", "deb887", "cadetblue", "5f9ea0", "chartreuse", "7fff00",
        "chocolate", "d2691e", "coral", "ff7f50", "cornflowerblue", "6495ed",
        "cornsilk", "fff8dc", "crimson", "dc143c", "cyan", "00ffff",
        "darkblue", "00008b", "darkcyan", "008b8b", "darkgoldenrod", "b8860b",
        "darkgray", "a9a9a9", "darkgreen", "006400", "darkgrey", "a9a9a9",
        "darkkhaki", "bdb76b", "darkmagenta", "8b008b", "darkolivegreen", "556b2f",
        "darkorange", "ff8c00", "darkorchid", "9932cc", "darkred", "8b0000",
        "darksalmon", "e9967a", "darkseagreen", "8fbc8f", "darkslateblue", "483d8b",
        "darkslategray", "2f4f4f", "darkslategrey", "2f4f4f", "darkturquoise", "00ced1",
        "darkviolet", "9400d3", "deeppink", "ff1493", "deepskyblue", "00bfff",
        "dimgray", "696969", "dimgrey", "696969", "dodgerblue", "1e90ff",
        "firebrick", "b22222", "floralwhite", "fffaf0", "forestgreen", "228b22",
        "fuchsia", "ff00ff", "gainsboro", "dcdcdc", "ghostwhite", "f8f8ff",
        "gold", "ffd700", "goldenrod", "daa520", "gray", "808080",
        "green", "008000", "greenyellow", "adff2f", "grey", "808080",
        "honeydew", "f0fff0", "hotpink", "ff69b4", "indianred", "cd5c5c",
        "indigo", "4b0082", "ivory", "fffff0", "khaki", "f0e68c",
        "lavender", "e6e6fa", "lavenderblush", "fff0f5", "lawngreen", "7cfc00",
        "lemonchiffon", "fffacd", "lightblue", "add8e6", "lightcoral", "f08080",
        "lightcyan", "e0ffff", "lightgoldenrodyellow", "fafad2", "lightgray", "d3d3d3",
        "lightgreen", "90ee90", "lightgrey", "d3d3d3", "lightpink", "ffb6c1",
        "lightsalmon", "ffa07a", "lightseagreen", "20b2aa", "lightskyblue", "87cefa",
        "lightslategray", "778899", "lightslategrey", "778899", "lightsteelblue", "b0c4de",
        "lightyellow", "ffffe0", "lime", "00ff00", "limegreen", "32cd32",
        "linen", "faf0e6", "magenta", "ff00ff", "maroon", "800000",
        "mediumaquamarine", "66cdaa", "mediumblue", "0000cd", "mediumorchid", "ba55d3",
        "mediumpurple", "9370db", "mediumseagreen", "3cb371", "mediumslateblue", "7b68ee",
        "mediumspringgreen", "00fa9a", "mediumturquoise", "48d1cc", "mediumvioletred", "c71585",
        "midnightblue", "191970", "mintcream", "f5fffa", "mistyrose", "ffe4e1",
        "moccasin", "ffe4b5", "navajowhite", "ffdead", "navy", "000080",
        "oldlace", "fdf5e6", "olive", "808000", "olivedrab", "6b8e23",
        "orange", "ffa500", "orangered", "ff4500", "orchid", "da70d6",
        "palegoldenrod", "eee8aa", "palegreen", "98fb98", "paleturquoise", "afeeee",
        "palevioletred", "db7093", "papayawhip", "ffefd5", "peachpuff", "ffdab9",
        "peru", "cd853f", "pink", "ffc0cb", "plum", "dda0dd",
        "powderblue", "b0e0e6", "purple", "800080", "red", "ff0000",
        "rosybrown", "bc8f8f", "royalblue", "4169e1", "saddlebrown", "8b4513",
        "salmon", "fa8072", "sandybrown", "f4a460", "seagreen", "2e8b57",
        "seashell", "fff5ee", "sienna", "a0522d", "silver", "c0c0c0",
        "skyblue", "87ceeb", "slateblue", "6a5acd", "slategray", "708090",
        "slategrey", "708090", "snow", "fffafa", "springgreen", "00ff7f",
        "steelblue", "4682b4", "tan", "d2b48c", "teal", "008080",
        "thistle", "d8bfd8", "tomato", "ff6347", "turquoise", "40e0d0",
        "violet", "ee82ee", "wheat", "f5deb3", "white", "ffffff",
        "whitesmoke", "f5f5f5", "yellow", "ffff00", "yellowgreen", "9acd32",
    };

    private String cssColorKeyword(String value) {
        for (int i = 0; i < NAMED_COLORS.length; i += 2) {
            if (NAMED_COLORS[i].equals(value)) {
                return NAMED_COLORS[i + 1];
            }
        }
        return null;
    }

    /// `1px solid red` as `1px solid #ff0000`: every whitespace-separated CSS color keyword
    /// replaced by its hex.
    private String namedColorsAsHex(String value) {
        StringBuilder out = new StringBuilder(value.length());
        int i = 0;
        int n = value.length();
        while (i < n) {
            char c = value.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                out.append(c);
                i++;
                continue;
            }
            int end = i;
            while (end < n && " \t\n\r\f".indexOf(value.charAt(end)) < 0) {
                end++;
            }
            String token = value.substring(i, end);
            String hex = "transparent".equalsIgnoreCase(token) ? null : cssColorKeyword(asciiLower(token));
            out.append(hex != null ? "#" + hex : token);
            i = end;
        }
        return out.toString();
    }

    /// The alpha a color carries, 0-255, or -1 for an opaque color that states none:
    /// `transparent` is 0 and `rgba(0, 0, 0, .5)` is 127, as the build-time compiler truncates it.
    private int colorAlpha(String cssColor) {
        String value = asciiLower(cssColor.trim());
        if ("transparent".equals(value)) {
            return 0;
        }
        if (!value.startsWith("rgba(") || !value.endsWith(")")) {
            return -1;
        }
        String[] parts = colorArguments(value.substring(5, value.length() - 1), cssColor);
        if (parts.length != 4) {
            throw new CSSSyntaxException("rgba() must have exactly 4 components: " + cssColor);
        }
        String a = parts[3].trim();
        float fraction;
        try {
            fraction = a.endsWith("%") ? Float.parseFloat(a.substring(0, a.length() - 1)) / 100f : Float.parseFloat(a);
        } catch (NumberFormatException err) {
            throw new CSSSyntaxException("Invalid rgba() alpha in " + cssColor + ": " + a, err);
        }
        return Math.max(0, Math.min(255, (int) (fraction * 255.0)));
    }

    private int parseRgbChannel(String value, String originalColor) {
        int out;
        String v = value.trim();
        try {
            if (v.endsWith("%")) {
                out = Math.round(Float.parseFloat(v.substring(0, v.length() - 1)) * 255f / 100f);
            } else {
                out = Integer.parseInt(v);
            }
        } catch (RuntimeException err) {
            throw new CSSSyntaxException("Invalid rgb() channel value in " + originalColor + ": " + value, err);
        }
        // Out of range is clamped, as CSS specifies and as the build-time compiler does.
        return Math.max(0, Math.min(255, out));
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

    /// 0-3 for `padding-top`/`-right`/`-bottom`/`-left` (and `margin-`), else -1.
    private static int boxSide(String property) {
        String rest;
        if (property.startsWith("padding-")) {
            rest = property.substring(8);
        } else if (property.startsWith("margin-")) {
            rest = property.substring(7);
        } else {
            return -1;
        }
        if ("top".equals(rest)) {
            return 0;
        }
        if ("right".equals(rest)) {
            return 1;
        }
        if ("bottom".equals(rest)) {
            return 2;
        }
        return "left".equals(rest) ? 3 : -1;
    }

    /// Writes the declared sides over the box they belong to: the state's own box when one was
    /// declared, else (for a state) the UIID's unselected box, which the build-time compiler's
    /// state style starts from, else zero. Unselected keys go first so a state can start from
    /// a box that itself had sides applied.
    private void applyBoxSides(Hashtable theme) {
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<String, Length[]> entry : boxSides.entrySet()) {
                String key = entry.getKey();
                int dot = key.indexOf('.');
                int hash = key.indexOf('#', dot);
                if ((pass == 0) != (hash < 0)) {
                    continue;
                }
                String box = key.endsWith("padding") ? "padding" : "margin";
                String unitSuffix = "padding".equals(box) ? Style.PADDING_UNIT : Style.MARGIN_UNIT;
                String prefix = key.substring(0, key.length() - box.length());
                String source = theme.get(key) != null || hash < 0 ? prefix : key.substring(0, dot + 1);
                Length[] out = readBox(theme.get(source + box), theme.get(source + unitSuffix));
                Length[] sides = entry.getValue();
                for (int i = 0; i < 4; i++) {
                    if (sides[i] != null) {
                        out[i] = sides[i];
                    }
                }
                theme.put(key, out[0].text + "," + out[2].text + "," + out[3].text + "," + out[1].text);
                theme.put(prefix + unitSuffix, new byte[]{out[0].unit, out[3].unit, out[2].unit, out[1].unit});
            }
        }
    }

    /// A stored box (`top,bottom,left,right` with units `[top, left, bottom, right]`) as
    /// top/right/bottom/left lengths; zero pixels where nothing is stored.
    private static Length[] readBox(Object values, Object units) {
        Length[] out = new Length[4];
        String[] parts = values instanceof String ? splitCommas((String) values) : null;
        byte[] unit = units instanceof byte[] ? (byte[]) units : null;
        // Stored order to top/right/bottom/left.
        int[] textIndex = {0, 3, 1, 2};
        int[] unitIndex = {0, 3, 2, 1};
        for (int i = 0; i < 4; i++) {
            Length l = new Length();
            l.text = parts != null && parts.length == 4 ? parts[textIndex[i]].trim() : "0";
            l.unit = unit != null && unit.length == 4 ? unit[unitIndex[i]] : Style.UNIT_TYPE_PIXELS;
            out[i] = l;
        }
        return out;
    }

    private static String[] splitCommas(String value) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == ',') {
                out.add(value.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
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
        // A bare number must be an integer spelling: the build-time compiler takes `1.5` or
        // `1e2` for a dimension missing its unit and fails the build.
        if (unit == null && !isIntegerSpelling(v)) {
            throw new CSSSyntaxException("A unitless padding or margin must be a whole number: " + value);
        }
        String digits = unit == null ? v : v.substring(0, v.length() - unit.length());
        if (digits.indexOf('e') > -1 || digits.indexOf('E') > -1) {
            // `1e2px` is a dimension with the unit `e2px` to the build-time parser, which the
            // inset conversion rejects.
            throw new CSSSyntaxException("Exponent notation is not supported for padding or margin: " + value);
        }
        float n = number(digits, value);
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
                constants.legacyConstants = selectors.startsWith("@");
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
                if (isConstantsSelector(selector)) {
                    // `#Constants, Other { ... }`: the build-time compiler still exports the
                    // constants, and applies the declarations to the other selectors as usual.
                    Rule constants = new Rule();
                    constants.selector = selector;
                    constants.constants = true;
                    constants.declarations = parseDeclarations(body);
                    out.add(constants);
                    continue;
                }
                Rule rule = new Rule();
                rule.selector = darkContext ? toDarkSelector(selector) : selector;
                String base = selector.trim();
                rule.universal = base.startsWith("*") || base.startsWith(":") && !":root".equals(base);
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
            } else if (c == '\\') {
                // An escape outside a string: `foo\"bar` is one identifier, not a string start.
                i++;
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

    /// A dark media block's rules land on the same `$Dark` UIID as a `$DarkFoo` rule and merge
    /// with it in source order. The build-time compiler differs in one case on purpose left
    /// unmatched: a `$DarkFoo` rule BEFORE a `@media (prefers-color-scheme: dark) { Foo {...} }`
    /// block loses its other declarations there (the media rule replaces the element instead of
    /// merging), so `background-color` set in `$DarkFoo` silently vanishes from the build. That
    /// is a defect of the build, not a behaviour a stylesheet relies on, so the preview keeps
    /// what the author wrote.
    private boolean isDarkModeMediaQuery(String mediaQuery) {
        String normalized = mediaQuery == null ? "" : asciiLower(mediaQuery);
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
            if (c == '\\' && i + 1 < css.length()) {
                // An escaped quote outside a string, as in `foo\"bar`, opens no string.
                out.append(c).append(css.charAt(i + 1));
                i += 2;
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

    /// Drops a trailing `!important`: the build-time SAC parser reports priority apart from the
    /// value and the compiler ignores it, so `includeNativeBool: true !important` stores `true`.
    /// `var( --x )` is `var(--x)`: the build-time parser tokenizes function arguments without
    /// regard to whitespace, while every lookup here searches for `var(--`. Quoted text is left
    /// as written.
    private static String tightenVarCalls(String value) {
        if (value.indexOf("var(") < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        char open = 0;
        int n = value.length();
        for (int i = 0; i < n; i++) {
            char c = value.charAt(i);
            out.append(c);
            if (c == '\\' && i + 1 < n) {
                out.append(value.charAt(++i));
            } else if (open == 0 && (c == '"' || c == '\'')) {
                open = c;
            } else if (c == open) {
                open = 0;
            } else if (open == 0 && c == '(' && i >= 3 && value.regionMatches(true, i - 3, "var", 0, 3)
                    && (i == 3 || !isNameChar(value.charAt(i - 4)))) {
                while (i + 1 < n && value.charAt(i + 1) <= ' ') {
                    i++;
                }
            }
        }
        return out.toString();
    }

    private static boolean isNameChar(char c) {
        return c == '-' || c == '_' || c > 127 || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9');
    }

    private static String stripPriority(String value) {
        // `!` then optional whitespace then `important`, as the CSS grammar has it.
        String marker = "important";
        int at = value.length() - marker.length();
        if (at < 0 || !value.regionMatches(true, at, marker, 0, marker.length())) {
            return value;
        }
        while (at > 0 && value.charAt(at - 1) <= ' ') {
            at--;
        }
        if (at == 0 || value.charAt(at - 1) != '!') {
            return value;
        }
        int bang = at - 1;
        // `foo\!important` is an identifier with an escaped bang, not a priority.
        int slashes = 0;
        while (bang - 1 - slashes >= 0 && value.charAt(bang - 1 - slashes) == '\\') {
            slashes++;
        }
        if ((slashes & 1) != 0) {
            return value;
        }
        while (bang > 0 && value.charAt(bang - 1) <= ' ') {
            bang--;
        }
        // Inside an unterminated quote the marker is text, not a priority. The open delimiter
        // is tracked, since the other quote character inside a string (`"don't"`) is text.
        char open = 0;
        for (int i = 0; i < bang; i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (open == 0 && (c == '"' || c == '\'')) {
                open = c;
            } else if (c == open) {
                open = 0;
            }
        }
        return open == 0 ? value.substring(0, bang).trim() : value;
    }

    /// The first `:` not escaped by a backslash: `a\:b: hello` names the constant `a:b`.
    private static int unescapedColon(String declaration) {
        for (int i = 0; i < declaration.length(); i++) {
            char c = declaration.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == ':') {
                return i;
            }
        }
        return -1;
    }

    private Declaration[] parseDeclarations(String body) {
        ArrayList<Declaration> out = new ArrayList<Declaration>();
        String[] segments = splitDeclarations(body);
        for (String line : segments) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            int colon = unescapedColon(trimmed);
            if (colon <= 0 || colon == trimmed.length() - 1) {
                throw new CSSSyntaxException("Malformed declaration: " + trimmed);
            }
            Declaration dec = new Declaration();
            dec.name = trimmed.substring(0, colon).trim();
            dec.property = asciiLower(dec.name);
            dec.value = tightenVarCalls(stripPriority(trimmed.substring(colon + 1).trim()));
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
            } else if (c == '\\') {
                // `foo\;bar` is one identifier, as the build-time parser reads it.
                i++;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if ((c == 'u' || c == 'U') && input.regionMatches(true, i, "url(", 0, 4)
                    && (i == 0 || !isNameChar(input.charAt(i - 1)))) {
                // An unquoted url() is one token, so `url(data:image/png;base64,AAAA)` keeps
                // its semicolon, as the build-time parser reads it.
                int close = input.indexOf(')', i + 4);
                if (close > 0) {
                    i = close;
                }
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

    /// The arguments of `rgb()` or `rgba()`. Unlike a font family list, an empty one is an error:
    /// the build-time parser rejects `rgb(255,,0,0)` rather than reading it as red.
    private static String[] colorArguments(String input, String color) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= input.length(); i++) {
            if (i == input.length() || input.charAt(i) == ',') {
                String part = input.substring(start, i).trim();
                if (part.length() == 0) {
                    throw new CSSSyntaxException("Empty component in color: " + color);
                }
                out.add(part);
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /// Lower-cases ASCII letters only. CSS keywords, property names and colors are ASCII by
    /// specification, and String.toLowerCase() follows the device locale: on a Turkish device
    /// `I` would fold to a dotless i and `PADDING-LEFT`-style names would never match.
    private static String asciiLower(String s) {
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) {
                    out = new StringBuilder(s);
                }
                out.setCharAt(i, (char) (c + 32));
            }
        }
        return out == null ? s : out.toString();
    }

    /// The value of an ASCII hex digit, or -1. CSS escapes take only `0-9a-fA-F`;
    /// `Character.digit` would also accept fullwidth and other Unicode digits.
    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /// Splits a family list on its commas, except an escaped one (`foo\,monospace` is one
    /// identifier) or one inside a quoted name.
    private String[] splitOnComma(String input) {
        ArrayList<String> parts = new ArrayList<String>();
        int start = 0;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ',') {
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
        /// An `@constants` block rather than `#Constants`.
        boolean legacyConstants;
        /// A `*` selector (or a bare state), which the build-time compiler ignores.
        boolean universal;
    }

    private static class Declaration {
        /// A `#Constants` alias whose reference was undefined where it was declared.
        boolean dropped;
        /// The name as written; constants and custom properties are case sensitive.
        String name;
        /// The lower case name ordinary CSS properties are matched by.
        String property;
        String value;
        /// A #Constants alias resolved where it was declared, and how to type it (an alias of
        /// text is final text, so it is typed like the quoted constant itself); null when it
        /// names something not declared yet and is resolved against the final values instead.
        String snapshot;
        String snapshotKind;
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
