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
package com.codename1.spellcheck;

import com.codename1.ui.Display;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Checks the spelling of words and text. A checker reads either a Hunspell dictionary the
/// application supplies (`create`) or the platform's own checker where the port has one
/// (`platform`); either way it adds a user dictionary and a list of ignored words, and skips
/// what is not a word to check: numbers, single letters, acronyms in capitals, URLs and email
/// addresses.
///
/// The build adds a Hunspell dictionary to an application that uses this class, for each
/// language named by the `spellcheck.languages` build hint (`en_US` when it is not set), and
/// `forLanguage` reads it. An application that brings its own dictionary sets
/// `spellcheck.dictionaries=false` and calls `create`. The simulator runs the application
/// unbuilt, so there `forLanguage` finds a dictionary only when the project depends on
/// `com.codenameone:codenameone-spellcheck-dictionaries`, and otherwise falls back to the
/// platform checker.
///
/// ```java
/// SpellChecker spell = SpellChecker.forLanguage("en_US");
/// if (spell == null) {
///     spell = SpellChecker.create(HunspellDictionary.fromBytes(aff, dic));
/// }
/// for (ProofIssue i : spell.check(text)) {
///     List<String> fixes = spell.suggest(text.substring(i.start, i.end), 5);
/// }
/// ```
public abstract class SpellChecker {
    private final Set<String> userWords = new HashSet<String>();
    private final Set<String> ignored = new HashSet<String>();

    /// Is this word, already trimmed and not empty, in the underlying dictionary?
    ///
    /// #### Parameters
    ///
    /// - `word`: the word as written
    ///
    /// #### Returns
    ///
    /// true when it is spelled correctly
    protected abstract boolean lookup(String word);

    /// Suggestions from the underlying dictionary.
    ///
    /// #### Parameters
    ///
    /// - `word`: the misspelled word
    ///
    /// - `max`: the most to return
    ///
    /// #### Returns
    ///
    /// the suggestions, best first
    protected abstract List<String> suggestions(String word, int max);

    /// A checker over a Hunspell dictionary.
    ///
    /// #### Parameters
    ///
    /// - `d`: the dictionary
    ///
    /// #### Returns
    ///
    /// the checker
    public static SpellChecker create(HunspellDictionary d) {
        return new Hunspell(d);
    }

    /// The resource name prefix of the dictionaries the build adds: `cn1dict_en_US.aff` and
    /// `cn1dict_en_US.dic` for `en_US`. Names are flat because some ports flatten resource
    /// folders.
    public static final String DICTIONARY_PREFIX = "cn1dict_";

    /// The resource listing the languages of the dictionaries the build added, one per line.
    public static final String LANGUAGES_RESOURCE = "/cn1dict_languages.txt";

    private static final Map<String, SpellChecker> LOADED = new HashMap<String, SpellChecker>();

    /// A checker for a language: the dictionary the build added for it, or else the
    /// platform's own checker. A dictionary is read once and shared; reading one takes a
    /// moment, so call this off the EDT when a delay would show.
    ///
    /// #### Parameters
    ///
    /// - `language`: a language tag such as `en_US`; `en-US` is the same language
    ///
    /// #### Returns
    ///
    /// the checker, or null when there is neither a dictionary nor a platform checker
    public static SpellChecker forLanguage(String language) {
        String lang = language == null ? "" : language.replace('-', '_');
        synchronized (LOADED) {
            SpellChecker s = LOADED.get(lang);
            if (s == null && !LOADED.containsKey(lang)) {
                HunspellDictionary d = bundled(lang);
                s = d != null ? create(d) : platform(language);
                LOADED.put(lang, s);
            }
            return s;
        }
    }

    /// The languages of the dictionaries the build added.
    ///
    /// #### Returns
    ///
    /// the language tags, empty when there are none
    public static List<String> getBundledLanguages() {
        List<String> out = new ArrayList<String>();
        byte[] b = resource(LANGUAGES_RESOURCE);
        if (b == null) {
            return out;
        }
        for (String line : HunspellDictionary.split(HunspellDictionary.decode(b, "UTF-8"), '\n')) {
            String t = line.trim();
            if (t.length() > 0 && t.charAt(0) != '#') {
                out.add(t);
            }
        }
        return out;
    }

    private static HunspellDictionary bundled(String lang) {
        if (lang.length() == 0) {
            return null;
        }
        String base = "/" + DICTIONARY_PREFIX + lang;
        byte[] aff = resource(base + ".aff");
        if (aff == null) {
            // The simulator and desktop runs are not built by the builder: there the
            // codenameone-spellcheck-dictionaries artifact, when the project depends on it,
            // is on the class path in its own layout.
            base = "/cn1dict/" + lang;
            aff = resource(base + ".aff");
        }
        byte[] dic = aff == null ? null : resource(base + ".dic");
        return dic == null ? null : HunspellDictionary.fromBytes(aff, dic);
    }

    private static byte[] resource(String name) {
        InputStream in = Display.isInitialized()
                ? Display.getInstance().getResourceAsStream(SpellChecker.class, name) : null;
        if (in == null) {
            in = SpellChecker.class.getResourceAsStream(name);
        }
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // nothing left to read
            }
        }
    }

    /// The platform's own checker for a language, where the port has one.
    ///
    /// #### Parameters
    ///
    /// - `language`: a language tag such as `en_US`, or null for the device's language
    ///
    /// #### Returns
    ///
    /// the checker, or null when the platform cannot check that language
    public static SpellChecker platform(String language) {
        if (!Display.isInitialized()
                || !Display.getInstance().isNativeSpellCheckSupported(language)) {
            return null;
        }
        return new Platform(language);
    }

    private static final class Hunspell extends SpellChecker {
        private final HunspellDictionary dictionary;

        Hunspell(HunspellDictionary d) {
            dictionary = d;
        }

        @Override
        protected boolean lookup(String word) {
            return dictionary.check(word);
        }

        @Override
        protected List<String> suggestions(String word, int max) {
            return dictionary.suggest(word, max);
        }
    }

    private static final class Platform extends SpellChecker {
        private final String language;

        Platform(String language) {
            this.language = language;
        }

        @Override
        protected boolean lookup(String word) {
            return Display.getInstance().isNativeSpelledCorrectly(word, language);
        }

        @Override
        protected List<String> suggestions(String word, int max) {
            String[] s = Display.getInstance().getNativeSpellingSuggestions(word, language, max);
            return s == null ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(s));
        }
    }

    /// Adds a word to the user dictionary, so it is spelled correctly in any case.
    ///
    /// #### Parameters
    ///
    /// - `word`: the word
    public void addWord(String word) {
        userWords.add(HunspellDictionary.lower(normalize(word)));
    }

    /// Removes a word from the user dictionary.
    ///
    /// #### Parameters
    ///
    /// - `word`: the word
    public void removeWord(String word) {
        userWords.remove(HunspellDictionary.lower(normalize(word)));
    }

    /// The words of the user dictionary, in lower case.
    ///
    /// #### Returns
    ///
    /// a copy of the set
    public Set<String> getUserWords() {
        return new HashSet<String>(userWords);
    }

    /// Ignores a word, exactly as written, until `clearIgnored`.
    ///
    /// #### Parameters
    ///
    /// - `word`: the word
    public void ignore(String word) {
        ignored.add(normalize(word));
    }

    /// Forgets every ignored word.
    public void clearIgnored() {
        ignored.clear();
    }

    private static String normalize(String w) {
        return trimmed(w).replace('\u2019', '\'');
    }

    static boolean letter(char c) {
        return Character.isLetter(c);
    }

    private static String trimmed(String word) {
        int a = 0;
        int b = word.length();
        while (a < b && !Character.isLetterOrDigit(word.charAt(a))) {
            a++;
        }
        while (b > a && !Character.isLetterOrDigit(word.charAt(b - 1))) {
            b--;
        }
        return word.substring(a, b);
    }

    /// Is the word spelled correctly? Numbers, words with digits, single letters and words in
    /// capitals are; each part of a hyphenated word is checked on its own.
    ///
    /// #### Parameters
    ///
    /// - `word`: the word
    ///
    /// #### Returns
    ///
    /// true when it is
    public boolean isCorrect(String word) {
        String w = normalize(word);
        if (w.length() <= 1 || ignored.contains(w)) {
            return true;
        }
        boolean letters = false;
        boolean lowerCase = false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (Character.isDigit(c)) {
                return true;
            }
            letters |= letter(c);
            lowerCase |= Character.isLowerCase(c);
        }
        if (!letters || !lowerCase) {
            return true;
        }
        if (known(w)) {
            return true;
        }
        if (w.indexOf('-') < 0) {
            return false;
        }
        for (String part : HunspellDictionary.split(w, '-')) {
            if (part.length() > 1 && !known(part)) {
                return false;
            }
        }
        return true;
    }

    private boolean known(String w) {
        if (userWords.contains(HunspellDictionary.lower(w)) || lookup(w)) {
            return true;
        }
        // a possessive of a correct word
        return w.endsWith("'s") && w.length() > 3 && lookup(w.substring(0, w.length() - 2));
    }

    /// Suggestions for a misspelled word, best first, user dictionary words included.
    ///
    /// #### Parameters
    ///
    /// - `word`: the word
    ///
    /// - `max`: the most to return
    ///
    /// #### Returns
    ///
    /// the suggestions
    public List<String> suggest(String word, int max) {
        List<String> out = new ArrayList<String>();
        String w = normalize(word);
        String l = HunspellDictionary.lower(w);
        for (String u : userWords) {
            if (out.size() < max && !u.equals(l) && closeTo(l, u)) {
                out.add(Character.isUpperCase(w.length() > 0 ? w.charAt(0) : 'a')
                        ? HunspellDictionary.capitalize(u) : u);
            }
        }
        for (String s : suggestions(w, max)) {
            if (out.size() >= max) {
                break;
            }
            if (!out.contains(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean closeTo(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) {
            return false;
        }
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int sub = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j], cur[j - 1]) + 1);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()] <= 1;
    }

    private static boolean wordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '\'' || c == '\u2019' || c == '-';
    }

    /// The words of a text as `{start, end}` pairs, apostrophes and hyphens at either end
    /// left out.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    ///
    /// #### Returns
    ///
    /// the word ranges in order
    public static List<int[]> words(String text) {
        List<int[]> out = new ArrayList<int[]>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            if (!wordChar(text.charAt(i))) {
                i++;
                continue;
            }
            int j = i;
            while (j < n && wordChar(text.charAt(j))) {
                j++;
            }
            int a = i;
            int b = j;
            while (a < b && !Character.isLetterOrDigit(text.charAt(a))) {
                a++;
            }
            while (b > a && !Character.isLetterOrDigit(text.charAt(b - 1))) {
                b--;
            }
            if (a < b) {
                out.add(new int[] {a, b});
            }
            i = j;
        }
        return out;
    }

    /// The misspelled words of a text, without suggestions (ask `suggest`, which is far
    /// slower than a check).
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, typically a paragraph
    ///
    /// #### Returns
    ///
    /// the issues in order
    public List<ProofIssue> check(String text) {
        List<ProofIssue> out = new ArrayList<ProofIssue>();
        for (int[] w : words(text)) {
            int ts = w[0];
            int te = w[1];
            while (ts > 0 && !Character.isWhitespace(text.charAt(ts - 1))) {
                ts--;
            }
            while (te < text.length() && !Character.isWhitespace(text.charAt(te))) {
                te++;
            }
            String token = text.substring(ts, te);
            if (token.indexOf("://") >= 0 || token.indexOf('@') >= 0
                    || token.startsWith("www.")) {
                continue;
            }
            if (!isCorrect(text.substring(w[0], w[1]))) {
                out.add(new ProofIssue(w[0], w[1], ProofIssue.SPELLING,
                        "Possible spelling mistake", null));
            }
        }
        return out;
    }
}
