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
package com.codename1.spreadsheet;

import java.util.ArrayList;

/// A number format as spreadsheets write it -- `#,##0.00`, `0%`, `yyyy-mm-dd`,
/// `[Red]-0.0;"none"` -- compiled once and applied to values.
///
/// These codes are not the patterns of a Java formatter: a section per sign, digit
/// placeholders that differ in what they show for a missing digit, a comma that divides by
/// a thousand, and an `m` that means month or minute by what stands next to it.
final class CellFormat {
    static final String GENERAL = "General";

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December"};
    private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday"};

    // token kinds
    private static final int LITERAL = 0;
    private static final int DIGIT = 1;
    private static final int POINT = 2;
    private static final int PERCENT = 3;
    private static final int EXPONENT = 4;
    private static final int TEXT = 5;
    private static final int DATE = 6;
    private static final int AMPM = 7;
    private static final int GENERAL_TOKEN = 8;
    private static final int ELAPSED = 9;
    private static final int SLASH = 10;
    private static final int SECOND_FRACTION = 11;

    private static final class Token {
        final int kind;
        final String text;

        Token(int kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private static final class Section {
        final ArrayList<Token> tokens = new ArrayList<Token>();
        boolean date;
        boolean general;
        boolean hasText;
        boolean grouping;
        boolean exponent;
        boolean fraction;
        boolean twelveHour;
        int percents;
        int thousands;
        int decimals;
        int integerDigits;
        // a condition in brackets: 0 none, then the operator
        char condition;
        boolean conditionOrEqual;
        double conditionValue;
    }

    static final int MAX_CODE = 1024;
    private final String code;
    /// Refuses a format code no file can hold: more than four sections, or a quote, a
    /// bracket or an escape left open.
    static void check(String code) {
        if (code == null) {
            return;
        }
        int sections = 1;
        int i = 0;
        while (i < code.length()) {
            char ch = code.charAt(i);
            if (ch == '"' || ch == '[') {
                int end = code.indexOf(ch == '"' ? '"' : ']', i + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("A format code with " + ch + " left open: " + code);
                }
                i = end + 1;
                continue;
            }
            if (ch == '\\' || ch == '_' || ch == '*') {
                if (i + 1 >= code.length()) {
                    throw new IllegalArgumentException("A format code ending in " + ch + ": " + code);
                }
                i += 2;
                continue;
            }
            if (ch == ';') {
                sections++;
            }
            i++;
        }
        if (sections > 4) {
            throw new IllegalArgumentException("A format code of more than four sections: " + code);
        }
    }

    private final Section[] sections;

    CellFormat(String code) {
        // a format code is 255 characters at most in the programs that write them; one far
        // longer is not a format, and is not given the work of being read as one
        this.code = code == null || code.length() == 0 || code.length() > MAX_CODE ? GENERAL : code;
        ArrayList<Section> list = new ArrayList<Section>();
        int start = 0;
        boolean quoted = false;
        boolean bracket = false;
        String c = this.code;
        for (int i = 0; i <= c.length(); i++) {
            char ch = i < c.length() ? c.charAt(i) : ';';
            if (i < c.length()) {
                if (quoted) {
                    quoted = ch != '"';
                    continue;
                }
                if (ch == '"') {
                    quoted = true;
                    continue;
                }
                if (ch == '\\' || ch == '_' || ch == '*') {
                    // the character it applies to is skipped, and at the end of the code
                    // there is none: the section still has to be closed
                    if (i + 1 < c.length()) {
                        i++;
                    }
                    continue;
                }
                if (ch == '[') {
                    bracket = true;
                } else if (ch == ']') {
                    bracket = false;
                }
                if (bracket || ch != ';') {
                    continue;
                }
            }
            list.add(compile(c.substring(start, Math.min(i, c.length()))));
            start = i + 1;
        }
        sections = new Section[list.size()];
        list.toArray(sections);
    }

    String code() {
        return code;
    }

    /// The format code of one of the numbered formats every spreadsheet file knows without
    /// being told, null for a number that is not one.
    static String builtin(int id) {
        switch (id) {
            case 0:
                return GENERAL;
            case 1:
                return "0";
            case 2:
                return "0.00";
            case 3:
                return "#,##0";
            case 4:
                return "#,##0.00";
            case 5:
            case 37:
                return "#,##0;-#,##0";
            case 6:
            case 38:
                return "#,##0;[Red]-#,##0";
            case 7:
            case 39:
                return "#,##0.00;-#,##0.00";
            case 8:
            case 40:
                return "#,##0.00;[Red]-#,##0.00";
            case 9:
                return "0%";
            case 10:
                return "0.00%";
            case 11:
                return "0.00E+00";
            case 12:
                return "# ?/?";
            case 13:
                return "# ??/??";
            case 14:
                return "m/d/yyyy";
            case 15:
                return "d-mmm-yy";
            case 16:
                return "d-mmm";
            case 17:
                return "mmm-yy";
            case 18:
                return "h:mm AM/PM";
            case 19:
                return "h:mm:ss AM/PM";
            case 20:
                return "h:mm";
            case 21:
                return "h:mm:ss";
            case 22:
                return "m/d/yyyy h:mm";
            case 45:
                return "mm:ss";
            case 46:
                return "[h]:mm:ss";
            case 47:
                return "mm:ss.0";
            case 48:
                return "##0.0E+0";
            case 49:
                return "@";
            default:
                return null;
        }
    }

    /// The number of the built-in format with a code, -1 when the code is not one of them.
    static int builtinId(String code) {
        for (int i = 0; i < 50; i++) {
            String b = builtin(i);
            // 5 to 8 and 37 to 40 are the locale's: in en-US 5 to 8 are currency, so the codes
            // they are read as here are written out in full and keep their meaning
            if (b != null && b.equals(code) && (i < 5 || i > 8) && (i < 37 || i > 40)) {
                return i;
            }
        }
        return -1;
    }

    /// Whether numbers shown with this format are dates or times.
    boolean isDate() {
        return sections.length > 0 && sections[0].date;
    }

    /// Whether the format shows a time of day and no date.
    boolean isTimeOnly() {
        if (!isDate()) {
            return false;
        }
        ArrayList<Token> tokens = sections[0].tokens;
        for (Token t : tokens) {
            if (t.kind == DATE) {
                char c = t.text.charAt(0);
                if (c == 'y' || c == 'd' || c == 'M') {
                    return false;
                }
            }
        }
        return true;
    }

    boolean isPercent() {
        return sections.length > 0 && sections[0].percents > 0;
    }

    boolean isText() {
        return sections.length == 1 && sections[0].hasText && sections[0].tokens.size() == 1;
    }

    boolean isGeneral() {
        return sections.length == 1 && sections[0].general && sections[0].tokens.size() == 1;
    }

    /// The count of decimals the format shows, for a column that has to pick an alignment.
    int decimals() {
        return sections.length == 0 ? 0 : sections[0].decimals;
    }

    static String format(String code, Object value, boolean date1904) {
        return new CellFormat(code).format(value, date1904);
    }

    private static Section compile(String s) {
        Section sec = new Section();
        ArrayList<Token> out = sec.tokens;
        int n = s.length();
        boolean afterPoint = false;
        boolean afterSlash = false;
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"') {
                int end = s.indexOf('"', i + 1);
                if (end < 0) {
                    end = n;
                }
                out.add(new Token(LITERAL, s.substring(i + 1, end)));
                i = end + 1;
                continue;
            }
            if (c == '\\') {
                if (i + 1 < n) {
                    out.add(new Token(LITERAL, s.substring(i + 1, i + 2)));
                }
                i += 2;
                continue;
            }
            if (c == '_') {
                // a space as wide as the next character
                out.add(new Token(LITERAL, " "));
                i += 2;
                continue;
            }
            if (c == '*') {
                // repeats the next character to fill the cell: there is no width to fill here
                i += 2;
                continue;
            }
            if (c == '[') {
                int end = s.indexOf(']', i);
                if (end < 0) {
                    end = n;
                }
                bracket(sec, s.substring(i + 1, end));
                i = end + 1;
                continue;
            }
            if (c == 'G' || c == 'g') {
                if (s.regionMatches(true, i, GENERAL, 0, 7)) {
                    out.add(new Token(GENERAL_TOKEN, ""));
                    sec.general = true;
                    i += 7;
                    continue;
                }
            }
            if (c == '@') {
                out.add(new Token(TEXT, ""));
                sec.hasText = true;
                i++;
                continue;
            }
            if (c == '0' || c == '#' || c == '?') {
                if (sec.date && c == '0' && afterPoint) {
                    // fractions of a second
                    int end = i;
                    while (end < n && s.charAt(end) == '0') {
                        end++;
                    }
                    out.remove(out.size() - 1);
                    out.add(new Token(SECOND_FRACTION, s.substring(i, end)));
                    i = end;
                    continue;
                }
                out.add(new Token(DIGIT, afterSlash ? "/" + c : afterPoint ? "." + c : String.valueOf(c)));
                if (afterPoint) {
                    sec.decimals++;
                } else if (!afterSlash && !sec.exponent) {
                    sec.integerDigits++;
                }
                i++;
                continue;
            }
            if (c == '.') {
                out.add(new Token(POINT, "."));
                afterPoint = true;
                i++;
                continue;
            }
            if (c == ',') {
                // between digits a comma groups them; after the last digit it divides by 1000
                boolean digitsAfter = false;
                for (int k = i + 1; k < n; k++) {
                    char d = s.charAt(k);
                    if (d == '0' || d == '#' || d == '?') {
                        digitsAfter = true;
                        break;
                    }
                    if (d != ',') {
                        break;
                    }
                }
                boolean digitBefore = !out.isEmpty() && out.get(out.size() - 1).kind == DIGIT;
                if (digitBefore && digitsAfter && !afterPoint) {
                    sec.grouping = true;
                } else if (digitBefore || !out.isEmpty() && out.get(out.size() - 1).kind == POINT) {
                    sec.thousands++;
                } else {
                    out.add(new Token(LITERAL, ","));
                }
                i++;
                continue;
            }
            if (c == '%') {
                out.add(new Token(PERCENT, "%"));
                sec.percents++;
                i++;
                continue;
            }
            if ((c == 'E' || c == 'e') && i + 1 < n && (s.charAt(i + 1) == '+' || s.charAt(i + 1) == '-')
                    && !sec.date) {
                out.add(new Token(EXPONENT, s.substring(i, i + 2)));
                sec.exponent = true;
                afterPoint = false;
                i += 2;
                continue;
            }
            if (c == '/' && !sec.date && sec.integerDigits > 0) {
                out.add(new Token(SLASH, "/"));
                sec.fraction = true;
                afterSlash = true;
                i++;
                continue;
            }
            if (s.regionMatches(true, i, "AM/PM", 0, 5)) {
                out.add(new Token(AMPM, s.substring(i, i + 5)));
                sec.twelveHour = true;
                sec.date = true;
                i += 5;
                continue;
            }
            if (s.regionMatches(true, i, "A/P", 0, 3)) {
                out.add(new Token(AMPM, s.substring(i, i + 3)));
                sec.twelveHour = true;
                sec.date = true;
                i += 3;
                continue;
            }
            char lower = c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
            if (lower == 'y' || lower == 'm' || lower == 'd' || lower == 'h' || lower == 's') {
                int end = i;
                while (end < n && (s.charAt(end) == c || s.charAt(end) == lower
                        || s.charAt(end) == (char) (lower - 32))) {
                    end++;
                }
                StringBuilder run = new StringBuilder();
                for (int k = i; k < end; k++) {
                    run.append(lower);
                }
                out.add(new Token(DATE, run.toString()));
                sec.date = true;
                afterPoint = false;
                i = end;
                continue;
            }
            out.add(new Token(LITERAL, String.valueOf(c)));
            i++;
        }
        if (sec.date) {
            resolveMinutes(out);
        }
        return sec;
    }

    /// An `m` run is minutes when it follows hours or comes before seconds, months otherwise.
    /// Months are renamed to `M` so that the formatter need not look around again.
    private static void resolveMinutes(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.kind != DATE || t.text.charAt(0) != 'm') {
                continue;
            }
            boolean minutes = false;
            if (t.text.length() <= 2) {
                for (int k = i - 1; k >= 0; k--) {
                    Token p = tokens.get(k);
                    if (p.kind == DATE || p.kind == ELAPSED) {
                        minutes = p.text.charAt(0) == 'h';
                        break;
                    }
                }
                if (!minutes) {
                    for (int k = i + 1; k < tokens.size(); k++) {
                        Token p = tokens.get(k);
                        if (p.kind == DATE || p.kind == ELAPSED) {
                            minutes = p.text.charAt(0) == 's';
                            break;
                        }
                    }
                }
            }
            if (!minutes) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < t.text.length(); k++) {
                    sb.append('M');
                }
                tokens.set(i, new Token(DATE, sb.toString()));
            }
        }
    }

    private static void bracket(Section sec, String body) {
        if (body.length() == 0) {
            return;
        }
        char c = body.charAt(0);
        if (c == '<' || c == '>' || c == '=') {
            int skip = 1;
            sec.condition = c;
            if (body.length() > 1 && body.charAt(1) == '=') {
                sec.conditionOrEqual = true;
                skip = 2;
            } else if (c == '<' && body.length() > 1 && body.charAt(1) == '>') {
                sec.condition = '!';
                skip = 2;
            }
            try {
                sec.conditionValue = Double.parseDouble(body.substring(skip));
            } catch (NumberFormatException e) {
                sec.condition = 0;
            }
            return;
        }
        if (c == '$') {
            // a currency symbol and a locale: [$EUR-407] shows EUR
            int dash = body.indexOf('-');
            String symbol = body.substring(1, dash < 0 ? body.length() : dash);
            if (symbol.length() > 0) {
                sec.tokens.add(new Token(LITERAL, symbol));
            }
            return;
        }
        // folded by hand: the tokens are ASCII, and a Turkish device would fold an I elsewhere
        StringBuilder folded = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            folded.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + 32) : ch);
        }
        String lower = folded.toString();
        if (lower.length() > 0 && (lower.charAt(0) == 'h' || lower.charAt(0) == 'm' || lower.charAt(0) == 's')) {
            boolean run = true;
            for (int i = 1; i < lower.length(); i++) {
                run &= lower.charAt(i) == lower.charAt(0);
            }
            if (run) {
                sec.tokens.add(new Token(ELAPSED, lower));
                sec.date = true;
            }
        }
        // anything else is a colour or a locale, which change nothing in the text
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return s.length() > 0;
    }

    /// Writes the format as an OpenDocument data style, which describes in elements what a
    /// format code spells. A code of several sections is one style per section, and the
    /// last of them maps a value to the others by the condition of each, which is how
    /// OpenDocument spells sections. Answers false, and writes nothing, for a format that
    /// needs no style.
    boolean writeOds(XmlOut x, String name) throws java.io.IOException {
        // an empty first section with others after it is a format (;;; hides everything)
        if (isGeneral() || sections[0].tokens.isEmpty() && sections.length == 1) {
            return false;
        }
        // the number sections; a fourth is for text. A section may be empty (the "hide"
        // idiom, 0;;;) or a date; OpenDocument maps a value to any data style.
        int count = Math.min(sections.length, 3);
        boolean explicit = sections[0].condition != 0;
        if (sections.length == 4) {
            // A fourth section is for text. OpenDocument spells that as a text style whose
            // conditions send a number to the number styles, as LibreOffice writes it. The
            // third section takes every number the first two did not, which two maps say.
            StringBuilder textMaps = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                String sub = name + "P" + i;
                writeOds(x, sub, sections[i], "");
                String[] conditions;
                if (i == 2) {
                    conditions = explicit ? new String[] {"value()>=0", "value()<0"} : new String[] {"value()=0"};
                } else if (sections[i].condition != 0) {
                    conditions = new String[] {conditionText(sections[i])};
                } else if (explicit) {
                    conditions = new String[] {"value()<0"};
                } else {
                    conditions = new String[] {i == 0 ? "value()>0" : "value()<0"};
                }
                for (String condition : conditions) {
                    textMaps.append("<style:map style:condition=\"").append(xmlAttr(condition))
                            .append("\" style:apply-style-name=\"").append(sub).append("\"/>");
                }
            }
            writeOdsText(x, name, sections[3], textMaps.toString());
            return true;
        }
        if (count < 2) {
            return writeOds(x, name, sections[0], "");
        }
        StringBuilder maps = new StringBuilder();
        for (int i = 0; i < count - 1; i++) {
            String sub = name + "P" + i;
            writeOds(x, sub, sections[i], "");
            String condition;
            if (sections[i].condition != 0) {
                condition = conditionText(sections[i]);
            } else if (explicit) {
                // after a condition of its own, the second section takes what is left
                // below; the last takes everything else
                condition = "value()<0";
            } else if (count == 2) {
                condition = "value()>=0";
            } else {
                condition = i == 0 ? "value()>0" : "value()<0";
            }
            maps.append("<style:map style:condition=\"").append(xmlAttr(condition))
                    .append("\" style:apply-style-name=\"").append(sub).append("\"/>");
        }
        return writeOds(x, name, sections[count - 1], maps.toString());
    }

    private static void writeOdsText(XmlOut x, String name, Section s, String maps) throws java.io.IOException {
        x.raw("<number:text-style").attr("style:name", name).raw(">");
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == TEXT) {
                x.raw("<number:text-content/>");
            } else if (t.kind == LITERAL) {
                x.raw("<number:text>").text(t.text, false).raw("</number:text>");
            }
        }
        x.raw(maps).raw("</number:text-style>");
    }

    private static String conditionText(Section s) {
        String op;
        switch (s.condition) {
            case '<':
                op = s.conditionOrEqual ? "<=" : "<";
                break;
            case '>':
                op = s.conditionOrEqual ? ">=" : ">";
                break;
            case '!':
                op = "!=";
                break;
            default:
                op = "=";
                break;
        }
        return "value()" + op + FormulaParser.numberText(s.conditionValue);
    }

    private static String xmlAttr(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean writeOds(XmlOut x, String name, Section s, String maps) throws java.io.IOException {
        if (s.hasText && !s.date && s.integerDigits == 0 && s.decimals == 0) {
            writeOdsText(x, name, s, maps);
            return true;
        }
        if (s.date) {
            writeOdsDate(x, name, s, maps);
            return true;
        }
        String tag = s.percents > 0 ? "number:percentage-style" : "number:number-style";
        x.raw("<").raw(tag).attr("style:name", name).raw(">");
        boolean numberWritten = false;
        boolean pastExponent = false;
        int minInteger = 0;
        int minDecimals = 0;
        int exponentDigits = 0;
        int denominatorDigits = 0;
        // What a fraction is made of: the digits of a whole number and a space when it has
        // one, the numerator, the slash, and a denominator of digits or written out.
        int slash = -1;
        int wholeEnd = -1;
        int fractionEnd = -1;
        String fixedDenominator = null;
        for (int i = 0; i < s.tokens.size() && s.fraction; i++) {
            Token t = s.tokens.get(i);
            if (t.kind == SLASH) {
                slash = i;
                fractionEnd = i;
                int[] fixed = fixedDenominator(s.tokens, i);
                if (fixed != null) {
                    fixedDenominator = String.valueOf(fixed[0]);
                    fractionEnd = fixed[1];
                    i = fixed[1];
                }
            } else if (t.kind == DIGIT && slash >= 0) {
                fractionEnd = i;
            } else if (t.kind == LITERAL && slash < 0 && " ".equals(t.text) && i > 0 && s.tokens.get(i - 1).kind == DIGIT) {
                wholeEnd = i;
            }
        }
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == EXPONENT) {
                pastExponent = true;
            } else if (t.kind == DIGIT) {
                if (pastExponent) {
                    exponentDigits++;
                } else if (slash >= 0 && i > slash) {
                    denominatorDigits++;
                } else if (".0".equals(t.text)) {
                    minDecimals++;
                } else if ("0".equals(t.text)) {
                    minInteger++;
                }
            }
        }
        // Text between the digits of the whole part, 000-00-0000, is text embedded in the
        // number, placed by how many digits follow it.
        int firstInteger = -1;
        int lastInteger = -1;
        for (int i = 0; i < s.tokens.size() && !s.exponent && !s.fraction; i++) {
            Token t = s.tokens.get(i);
            if (t.kind == POINT) {
                break;
            }
            if (t.kind == DIGIT && t.text.length() == 1) {
                if (firstInteger < 0) {
                    firstInteger = i;
                }
                lastInteger = i;
            }
        }
        ArrayList<String> embedded = new ArrayList<String>();
        for (int i = firstInteger + 1; firstInteger >= 0 && i < lastInteger; i++) {
            Token t = s.tokens.get(i);
            if (t.kind == LITERAL) {
                int after = 0;
                for (int k = i + 1; k <= lastInteger; k++) {
                    Token d = s.tokens.get(k);
                    after += d.kind == DIGIT && d.text.length() == 1 ? 1 : 0;
                }
                embedded.add(String.valueOf(after));
                embedded.add(t.text);
            }
        }
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (numberWritten && i <= fractionEnd) {
                // the space, the slash and the denominator are the fraction element's own
                continue;
            }
            if (t.kind == LITERAL && i > firstInteger && i < lastInteger && firstInteger >= 0) {
                // written inside the number element
                continue;
            }
            if (t.kind == LITERAL || t.kind == PERCENT) {
                x.raw("<number:text>").text(t.text, false).raw("</number:text>");
            } else if (t.kind == GENERAL_TOKEN && !numberWritten) {
                numberWritten = true;
                x.raw("<number:number number:min-integer-digits=\"1\"/>");
            } else if (t.kind == DIGIT && !numberWritten) {
                numberWritten = true;
                if (s.exponent) {
                    x.raw("<number:scientific-number").attr("number:decimal-places", s.decimals)
                            .attr("number:min-integer-digits", Math.max(1, minInteger))
                            .attr("number:min-exponent-digits", Math.max(1, exponentDigits));
                    if (s.integerDigits > 1) {
                        // ##0.0E+0: the exponent moves in steps of the integer places, which
                        // OpenDocument says as an interval
                        x.attr("number:exponent-interval", s.integerDigits);
                    }
                    x.raw("/>");
                } else if (s.fraction) {
                    x.raw("<number:fraction");
                    if (wholeEnd >= 0) {
                        // there, even at zero, it says a whole number is written first
                        x.attr("number:min-integer-digits", 0);
                    }
                    x.attr("number:min-numerator-digits", 1);
                    if (fixedDenominator != null) {
                        x.attr("number:denominator-value", fixedDenominator);
                    } else {
                        x.attr("number:min-denominator-digits", Math.max(1, denominatorDigits));
                    }
                    x.raw("/>");
                } else {
                    x.raw("<number:number").attr("number:decimal-places", s.decimals)
                            .attr("number:min-decimal-places", minDecimals)
                            .attr("number:min-integer-digits", minInteger);
                    if (s.grouping) {
                        x.raw(" number:grouping=\"true\"");
                    }
                    if (s.thousands > 0) {
                        x.attr("number:display-factor", FormulaParser.numberText(Values.pow10(3 * s.thousands)));
                    }
                    if (!embedded.isEmpty()) {
                        x.raw(">");
                        for (int k = 0; k < embedded.size(); k += 2) {
                            x.raw("<number:embedded-text").attr("number:position", embedded.get(k)).raw(">")
                                    .text(embedded.get(k + 1), false).raw("</number:embedded-text>");
                        }
                        x.raw("</number:number>");
                    } else {
                        x.raw("/>");
                    }
                }
            }
        }
        x.raw(maps).raw("</").raw(tag).raw(">");
        return true;
    }

    private void writeOdsDate(XmlOut x, String name, Section s, String maps) throws java.io.IOException {
        boolean elapsed = false;
        for (int i = 0; i < s.tokens.size(); i++) {
            elapsed |= s.tokens.get(i).kind == ELAPSED;
        }
        String tag = isTimeOnly() ? "number:time-style" : "number:date-style";
        x.raw("<").raw(tag).attr("style:name", name);
        if (elapsed) {
            x.raw(" number:truncate-on-overflow=\"false\"");
        }
        x.raw(">");
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            int len = t.text.length();
            String part = null;
            boolean textual = false;
            if (t.kind == DATE || t.kind == ELAPSED) {
                switch (t.text.charAt(0)) {
                    case 'y':
                        part = "number:year";
                        len = len > 2 ? 2 : 1;
                        break;
                    case 'M':
                        part = "number:month";
                        // OpenDocument has no month initial: mmmmm is written as the month's
                        // whole name, the nearest the format holds, and reads back as mmmm
                        textual = len > 2;
                        len = len > 2 ? len - 2 : len;
                        break;
                    case 'd':
                        part = len > 2 ? "number:day-of-week" : "number:day";
                        len = len > 2 ? len - 2 : len;
                        break;
                    case 'h':
                        part = "number:hours";
                        break;
                    case 'm':
                        part = "number:minutes";
                        break;
                    default:
                        part = "number:seconds";
                        break;
                }
                x.raw("<").raw(part);
                if (len > 1) {
                    x.raw(" number:style=\"long\"");
                }
                if (textual) {
                    x.raw(" number:textual=\"true\"");
                }
                if (i + 1 < s.tokens.size() && s.tokens.get(i + 1).kind == SECOND_FRACTION) {
                    x.attr("number:decimal-places", s.tokens.get(i + 1).text.length());
                }
                x.raw("/>");
            } else if (t.kind == AMPM) {
                x.raw("<number:am-pm/>");
            } else if (t.kind == LITERAL || t.kind == POINT) {
                x.raw("<number:text>").text(t.kind == POINT ? "." : t.text, false).raw("</number:text>");
            }
        }
        x.raw(maps).raw("</").raw(tag).raw(">");
    }

    private Section pick(double v) {
        boolean conditional = false;
        for (int i = 0; i < sections.length && i < 2; i++) {
            Section s = sections[i];
            if (s.condition != 0) {
                conditional = true;
                boolean hit;
                switch (s.condition) {
                    case '<':
                        hit = s.conditionOrEqual ? v <= s.conditionValue : v < s.conditionValue;
                        break;
                    case '>':
                        hit = s.conditionOrEqual ? v >= s.conditionValue : v > s.conditionValue;
                        break;
                    case '=':
                        hit = v <= s.conditionValue && v >= s.conditionValue;
                        break;
                    default:
                        hit = v < s.conditionValue || v > s.conditionValue;
                        break;
                }
                if (hit) {
                    return s;
                }
            }
        }
        if (conditional) {
            for (int i = 0; i < sections.length && i < 3; i++) {
                if (sections[i].condition == 0) {
                    return sections[i];
                }
            }
            return sections[sections.length - 1];
        }
        if (sections.length >= 3 && v == 0) {
            return sections[2];
        }
        if (sections.length >= 2 && v < 0) {
            return sections[1];
        }
        return sections[0];
    }

    /// The text a value is shown as.
    String format(Object value, boolean date1904) {
        try {
            return formatValue(value, date1904);
        } catch (EvalError e) {
            // The text of a cell is never an exception: whatever a code cannot write a number
            // in, the number is shown as General shows it.
            return value instanceof Double ? Values.general(((Double) value).doubleValue()) : String.valueOf(value);
        }
    }

    private String formatValue(Object value, boolean date1904) {
        if (value == null) {
            return "";
        }
        if (value instanceof CellError) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? "TRUE" : "FALSE";
        }
        if (value instanceof String) {
            Section text = null;
            if (sections.length >= 4) {
                text = sections[3];
            } else if (sections[sections.length - 1].hasText) {
                text = sections[sections.length - 1];
            }
            if (text == null) {
                return (String) value;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.tokens.size(); i++) {
                Token t = text.tokens.get(i);
                if (t.kind == TEXT) {
                    sb.append((String) value);
                } else if (t.kind == LITERAL) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }
        if (!(value instanceof Double)) {
            return value.toString();
        }
        double v = ((Double) value).doubleValue();
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return CellError.NUM.toString();
        }
        Section s = pick(v);
        if (s.date) {
            if (v < 0 || v >= DateFunctions.LAST_DAY + 1 - (date1904 ? 1462 : 0)) {
                // what a column too narrow for its date shows, and a date before the first
                // day or after 9999-12-31 always
                return "########";
            }
            return formatDate(s, v, date1904);
        }
        // a section of its own for negatives shows the sign itself, or leaves it out
        boolean ownSign = s != sections[0] || s.condition != 0; //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
        if (s.tokens.isEmpty()) {
            return "";
        }
        return formatNumber(s, ownSign ? Math.abs(v) : v);
    }

    private static String formatNumber(Section s, double v) {
        boolean negative = v < 0;
        double abs = Math.abs(v);
        for (int i = 0; i < s.percents; i++) {
            abs *= 100;
        }
        for (int i = 0; i < s.thousands; i++) {
            abs /= 1000;
        }
        boolean hasDigits = s.integerDigits > 0 || s.decimals > 0;
        if (s.general || !hasDigits) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.tokens.size(); i++) {
                Token t = s.tokens.get(i);
                if (t.kind == GENERAL_TOKEN || t.kind == TEXT) {
                    // "@" is the place of whatever the cell holds, a number included
                    sb.append(Values.general(abs));
                } else if (t.kind == LITERAL || t.kind == PERCENT) {
                    sb.append(t.text);
                }
            }
            return negative && abs != 0 ? "-" + sb : sb.toString();
        }
        if (s.fraction) {
            return formatFraction(s, abs, negative);
        }
        int exponent = 0;
        if (s.exponent && abs != 0) {
            // the exponent is a multiple of the count of integer placeholders
            int step = Math.max(1, s.integerDigits);
            exponent = (int) Math.floor(com.codename1.util.MathUtil.log10(abs));
            exponent = (int) (Math.floor((double) exponent / step) * step);
            // a subnormal number's power of ten is not a double, so it is divided out in two
            if (exponent < -300) {
                abs = abs * 1e300 / Values.pow10signed(exponent + 300);
            } else {
                abs = abs / Values.pow10signed(exponent);
            }
            if (Double.isNaN(abs) || Double.isInfinite(abs)) {
                return Values.general(negative ? -abs : abs);
            }
            if (Values.round(abs, s.decimals) >= Values.pow10(step)) {
                exponent += step;
                abs /= Values.pow10(step);
            }
        }
        // more decimal places than a number has digits are zeros, written by the placeholders
        String digits = TextFunctions.fixed(abs, Math.min(s.decimals, 30), false);
        int point = digits.indexOf('.');
        String whole = point < 0 ? digits : digits.substring(0, point);
        String frac = point < 0 ? "" : digits.substring(point + 1);
        if ("0".equals(whole)) {
            whole = "";
        }
        boolean zero = whole.length() == 0 && isZeros(frac);
        StringBuilder sb = new StringBuilder();
        if (negative && !zero) {
            sb.append('-');
        }
        // integer placeholders take the digits right to left; the first one takes any surplus
        int placeholders = s.integerDigits;
        int seen = 0;
        int fracAt = 0;
        boolean inExponent = false;
        int lastFraction = frac.length();
        while (lastFraction > 0 && frac.charAt(lastFraction - 1) == '0') {
            lastFraction--;
        }
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            switch (t.kind) {
                case DIGIT: {
                    char p = t.text.charAt(t.text.length() - 1);
                    if (inExponent) {
                        // every placeholder of the exponent was written with the exponent
                        break;
                    }
                    if (t.text.length() == 2) {
                        char digit = fracAt < frac.length() ? frac.charAt(fracAt) : '0';
                        if (fracAt < lastFraction || p == '0') {
                            sb.append(digit);
                        } else if (p == '?') {
                            sb.append(' ');
                        }
                        fracAt++;
                        break;
                    }
                    // the digits this placeholder shows: those left of the later placeholders
                    int remaining = placeholders - seen - 1;
                    int end = whole.length() - remaining;
                    int from = seen == 0 ? 0 : end - 1;
                    if (seen == 0) {
                        for (int k = 0; k < end; k++) {
                            appendDigit(sb, whole, k, s.grouping);
                        }
                    }
                    if (end <= 0 || seen > 0 && from < 0) {
                        if (end <= 0) {
                            if (p == '0') {
                                sb.append('0');
                                if (s.grouping && remaining > 0 && remaining % 3 == 0) {
                                    sb.append(',');
                                }
                            } else if (p == '?') {
                                sb.append(' ');
                            }
                        }
                    } else if (seen > 0) {
                        appendDigit(sb, whole, from, s.grouping);
                    }
                    seen++;
                    break;
                }
                case POINT:
                    sb.append('.');
                    break;
                case EXPONENT: {
                    sb.append(t.text.charAt(0));
                    if (exponent < 0) {
                        sb.append('-');
                    } else if (t.text.charAt(1) == '+') {
                        sb.append('+');
                    }
                    int width = 0;
                    for (int k = i + 1; k < s.tokens.size() && s.tokens.get(k).kind == DIGIT; k++) {
                        width++;
                    }
                    String e = Integer.toString(Math.abs(exponent));
                    for (int k = e.length(); k < width; k++) {
                        sb.append('0');
                    }
                    sb.append(e);
                    inExponent = true;
                    break;
                }
                case LITERAL:
                case PERCENT:
                    sb.append(t.text);
                    inExponent = false;
                    break;
                default:
                    break;
            }
        }
        return sb.toString();
    }

    private static void appendDigit(StringBuilder sb, String whole, int index, boolean grouping) {
        if (index < 0 || index >= whole.length()) {
            // a placeholder where no format has one, after the exponent: it has no digit
            return;
        }
        sb.append(whole.charAt(index));
        int after = whole.length() - index - 1;
        if (grouping && after > 0 && after % 3 == 0) {
            sb.append(',');
        }
    }

    private static boolean isZeros(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    /// Writes `n/d`, padding each part to its placeholders the way Excel lines fractions up:
    /// a `?` with no digit is a space, a `0` a zero and a `#` nothing. The numerator pads on
    /// the left, the denominator on the right.
    private static void appendFraction(StringBuilder sb, long n, long d, CharSequence numeratorMarks,
            CharSequence denominatorMarks) {
        String num = String.valueOf(n);
        for (int i = 0; i < numeratorMarks.length() - num.length(); i++) {
            pad(sb, numeratorMarks.charAt(i));
        }
        sb.append(num).append('/');
        String den = String.valueOf(d);
        sb.append(den);
        if (denominatorMarks != null) {
            // a zero written after the digits would read as another digit, so only ? pads here
            for (int i = den.length(); i < denominatorMarks.length(); i++) {
                if (denominatorMarks.charAt(i) == '?') {
                    sb.append(' ');
                }
            }
        }
    }

    /// The digits of a whole number, written out however large it is: 1E20 is a 1 and twenty
    /// zeros. Past the 17 digits a double holds, the rest are zeros, as Excel shows them.
    private static String wholeDigits(double whole) {
        if (whole < 9.2E18) {
            return Long.toString((long) whole);
        }
        String s = Double.toString(whole);
        int e = s.indexOf('E');
        if (e < 0) {
            return s;
        }
        int exponent = Integer.parseInt(s.substring(e + 1));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < e; i++) {
            char c = s.charAt(i);
            if (c != '.') {
                sb.append(c);
            }
        }
        // the mantissa is one digit before the point, the rest after it
        while (sb.length() < exponent + 1) {
            sb.append('0');
        }
        if (sb.length() > exponent + 1) {
            sb.setLength(exponent + 1);
        }
        return sb.toString();
    }

    /// Digits with a thousands separator every three, when the format asks for one.
    private static String grouped(String digits, boolean grouping) {
        if (!grouping || digits.length() <= 3) {
            return digits;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                sb.append(',');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private static void pad(StringBuilder sb, char mark) {
        if (mark == '?') {
            sb.append(' ');
        } else if (mark == '0') {
            sb.append('0');
        }
    }

    /// `# ?/?`: a whole part and the closest fraction whose denominator fits the placeholders.
    /// A fraction with the text its format writes around it: `$# ?/2` shows 2.5 as
    /// `$2 1/2`, and a section for negatives such as `-# ?/2` brings its own sign.
    private static String formatFraction(Section s, double abs, boolean negative) {
        int first = -1;
        int last = -1;
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == DIGIT || t.kind == SLASH) {
                if (first < 0) {
                    first = i;
                }
                last = i;
                if (t.kind == SLASH) {
                    int[] fixed = fixedDenominator(s.tokens, i);
                    if (fixed != null) {
                        last = fixed[1];
                        i = fixed[1];
                    }
                }
            }
        }
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == LITERAL && (i < first || i > last)) {
                (i < first ? prefix : suffix).append(t.text);
            }
        }
        String body = fractionBody(s, abs, negative);
        if (prefix.length() == 0 && suffix.length() == 0) {
            return body;
        }
        boolean signed = body.startsWith("-");
        return (signed ? "-" : "") + prefix + (signed ? body.substring(1) : body) + suffix;
    }

    private static String fractionBody(Section s, double abs, boolean negative) {
        int denominatorDigits = 0;
        int numeratorDigits = 0;
        int wholeDigits = 0;
        boolean pastSlash = false;
        boolean spaceSeen = false;
        int fixedDenominator = 0;
        StringBuilder wholeMarks = new StringBuilder();
        StringBuilder numeratorMarks = new StringBuilder();
        StringBuilder denominatorMarks = new StringBuilder();
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == SLASH) {
                pastSlash = true;
                int[] fixed = fixedDenominator(s.tokens, i);
                if (fixed != null) {
                    fixedDenominator = fixed[0];
                    i = fixed[1];
                }
            } else if (t.kind == DIGIT) {
                char mark = t.text.charAt(t.text.length() - 1);
                if (pastSlash) {
                    denominatorDigits++;
                    denominatorMarks.append(mark);
                } else if (spaceSeen) {
                    numeratorDigits++;
                    numeratorMarks.append(mark);
                } else {
                    wholeDigits++;
                    wholeMarks.append(mark);
                }
            } else if (t.kind == LITERAL && !pastSlash) {
                if (" ".equals(t.text) && wholeDigits > 0) {
                    spaceSeen = true;
                }
            }
        }
        boolean mixed = spaceSeen && numeratorDigits > 0;
        double whole = mixed ? Math.floor(abs) : 0;
        double rest = abs - whole;
        long bestN = 0;
        long bestD = 1;
        if (fixedDenominator > 0) {
            bestD = fixedDenominator;
            bestN = (long) Math.floor(rest * bestD + 0.5);
        } else {
            long max = (long) Values.pow10(Math.max(1, Math.min(denominatorDigits, 7))) - 1;
            double bestError = Double.MAX_VALUE;
            for (long den = 1; den <= max; den++) {
                long num = (long) Math.floor(rest * den + 0.5);
                double error = Math.abs(rest - (double) num / den);
                if (error < bestError - 1e-15) {
                    bestError = error;
                    bestN = num;
                    bestD = den;
                    if (error == 0) {
                        break;
                    }
                }
            }
        }
        if (mixed && bestN >= bestD) {
            whole += 1;
            bestN = 0;
        }
        StringBuilder sb = new StringBuilder();
        if (!mixed && abs * bestD >= 9.0E15) {
            // past the integers a double holds exactly there is no fraction left, and a long
            // cannot hold the numerator: every digit of it, over the denominator
            if (negative) {
                sb.append('-');
            }
            sb.append(wholeDigits(Math.floor(abs * bestD + 0.5))).append('/').append(bestD);
            for (int i = 1; fixedDenominator <= 0 && i < denominatorMarks.length(); i++) {
                if (denominatorMarks.charAt(i) == '?') {
                    sb.append(' ');
                }
            }
            return sb.toString();
        }
        if (negative && (whole != 0 || bestN != 0)) {
            sb.append('-');
        }
        boolean zeroShown = bestN == 0 && numeratorMarks.indexOf("0") >= 0;
        if (mixed) {
            boolean marked = wholeMarks.indexOf("0") >= 0 || wholeMarks.indexOf("?") >= 0;
            if (whole != 0 || bestN == 0 || marked) {
                // the whole part pads to its placeholders too: 00 shows 2 as 02, and 0 shows a
                // whole part of nothing as 0
                // every digit of the whole part, never an exponent: a fraction format shows 1E15
                // in full, as Excel does
                String digits = whole == 0 && bestN != 0 && wholeMarks.indexOf("0") < 0 ? ""
                        : wholeDigits(whole);
                StringBuilder padded = new StringBuilder();
                int blanks = 0;
                for (int i = 0; i < wholeMarks.length() - digits.length(); i++) {
                    char mark = wholeMarks.charAt(i);
                    if (mark == '0') {
                        padded.append('0');
                    } else if (mark == '?') {
                        blanks++;
                    }
                }
                padded.append(digits);
                for (int i = 0; i < blanks; i++) {
                    sb.append(' ');
                }
                // the zeros the placeholders add are grouped with the digits: 0,000 shows 5 as 0,005
                sb.append(grouped(padded.toString(), s.grouping));
            }
            if (bestN == 0 && !zeroShown) {
                // no fraction to show: blanks where it would be keep the column lined up
                int width = 1 + numeratorMarks.length() + 1 + (fixedDenominator > 0 ? 0 : denominatorMarks.length());
                for (int i = 0; i < width; i++) {
                    sb.append(' ');
                }
            }
            if (bestN != 0 || zeroShown) {
                // the space between the parts stays when the whole part shows nothing, as in
                // LibreOffice and Excel: # ?/10 shows 0.37 as " 4/10"
                sb.append(' ');
                appendFraction(sb, bestN, bestD, numeratorMarks, fixedDenominator > 0 ? null : denominatorMarks);
            }
        } else {
            appendFraction(sb, bestN, bestD, wholeMarks, fixedDenominator > 0 ? null : denominatorMarks);
        }
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == PERCENT) {
                sb.append('%');
            }
        }
        return sb.toString();
    }

    /// The denominator a fraction writes out, as the 16 of `# ??/16` or the 100 of
    /// `# ??/100`, and the index of its last token; null when the denominator is made of
    /// placeholders. A written denominator starts with a digit 1 to 9, and every digit after
    /// it, a 0 included, belongs to it.
    private static int[] fixedDenominator(ArrayList<Token> tokens, int slash) {
        StringBuilder digits = new StringBuilder();
        int last = slash;
        for (int i = slash + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            // a placeholder's text carries the slash or point before it: its digit is the last
            String text = t.kind == LITERAL ? t.text.trim()
                    : t.kind == DIGIT ? t.text.substring(t.text.length() - 1) : null;
            if (text == null || text.length() == 0 || !isDigits(text) || digits.length() == 0 && t.kind != LITERAL) {
                break;
            }
            if (digits.length() == 0 && text.charAt(0) == '0') {
                break;
            }
            digits.append(text);
            last = i;
        }
        if (digits.length() == 0 || digits.length() > 9) {
            return null;
        }
        return new int[] {Integer.parseInt(digits.toString()), last};
    }

    private static void pad(StringBuilder sb, long v, int width) {
        String t = Long.toString(v);
        for (int i = t.length(); i < width; i++) {
            sb.append('0');
        }
        sb.append(t);
    }

    private static String formatDate(Section s, double serial, boolean date1904) {
        // the smallest unit shown decides what the value is rounded to
        int fractionDigits = 0;
        boolean showsSeconds = false;
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            if (t.kind == SECOND_FRACTION) {
                fractionDigits = Math.min(3, t.text.length());
            } else if ((t.kind == DATE || t.kind == ELAPSED) && t.text.charAt(0) == 's') {
                showsSeconds = true;
            }
        }
        double unit = Values.pow10(fractionDigits);
        double totalUnits = Math.floor(serial * 86400 * unit + 0.5);
        if (!showsSeconds && fractionDigits == 0) {
            // minutes are not rounded up by the seconds that are not shown
            totalUnits = Math.floor(serial * 86400 + 1e-6);
        }
        double totalSeconds = Math.floor(totalUnits / unit);
        long fraction = (long) (totalUnits - totalSeconds * unit);
        double day = Math.floor(totalSeconds / 86400);
        long secondOfDay = (long) (totalSeconds - day * 86400);
        int[] f = DateUtil.fields(day, date1904);
        int hour = (int) (secondOfDay / 3600);
        int minute = (int) (secondOfDay / 60 % 60);
        int second = (int) (secondOfDay % 60);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.tokens.size(); i++) {
            Token t = s.tokens.get(i);
            switch (t.kind) {
                case LITERAL:
                case PERCENT:
                    sb.append(t.text);
                    break;
                case POINT:
                    sb.append('.');
                    break;
                case SECOND_FRACTION: {
                    sb.append('.');
                    pad(sb, fraction, fractionDigits);
                    break;
                }
                case ELAPSED: {
                    char c = t.text.charAt(0);
                    double total = c == 'h' ? Math.floor(totalSeconds / 3600)
                            : c == 'm' ? Math.floor(totalSeconds / 60) : totalSeconds;
                    pad(sb, (long) total, t.text.length());
                    break;
                }
                case AMPM: {
                    boolean pm = hour >= 12;
                    if (t.text.length() == 3) {
                        char a = t.text.charAt(0);
                        char p = t.text.charAt(2);
                        sb.append(pm ? p : a);
                    } else {
                        sb.append(pm ? "PM" : "AM");
                    }
                    break;
                }
                case DATE: {
                    char c = t.text.charAt(0);
                    int len = t.text.length();
                    switch (c) {
                        case 'y':
                            if (len <= 2) {
                                pad(sb, f[0] % 100, 2);
                            } else {
                                pad(sb, f[0], 4);
                            }
                            break;
                        case 'M':
                            if (len <= 2) {
                                pad(sb, f[1], len);
                            } else if (len == 3) {
                                sb.append(MONTHS[f[1] - 1].substring(0, 3));
                            } else if (len == 5) {
                                sb.append(MONTHS[f[1] - 1].charAt(0));
                            } else {
                                sb.append(MONTHS[f[1] - 1]);
                            }
                            break;
                        case 'd':
                            if (len <= 2) {
                                pad(sb, f[2], len);
                            } else {
                                String name = DAYS[DateUtil.weekday(day, date1904) - 1];
                                if (len == 3) {
                                    sb.append(name.substring(0, 3));
                                } else {
                                    sb.append(name);
                                }
                            }
                            break;
                        case 'h': {
                            int h = hour;
                            if (s.twelveHour) {
                                h = h % 12;
                                if (h == 0) {
                                    h = 12;
                                }
                            }
                            pad(sb, h, Math.min(len, 2));
                            break;
                        }
                        case 'm':
                            pad(sb, minute, Math.min(len, 2));
                            break;
                        default:
                            pad(sb, second, Math.min(len, 2));
                            break;
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return sb.toString();
    }
}
