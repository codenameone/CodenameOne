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
package com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.RTextArea;
import com.codename1.desktopcompat.rt.LafTheme;
import java.util.ArrayList;
import java.util.List;

/// RSyntaxTextArea in a reduced form: a code view, not a code editor.
///
/// #### What is provided
///
///  - The text in a monospaced font, tabs expanded, one row per line, so
///    line highlights, line numbers and `modelToView` are exact (see
///    [com.codename1.desktopcompat.org.fife.ui.rtextarea.RTextAreaBase]).
///  - Colors for comments, strings, numbers, keywords and markup tags,
///    by the shape of the language the syntax style names: the C family,
///    languages with hash comments, SQL and markup. Other styles are
///    shown as plain text.
///  - Line highlights, the current line highlight and marked occurrences.
///  - Colors that follow the light or dark palette, or a [Theme].
///
/// #### What is recorded only
///
/// Every switch of the editor the library is: anti-aliasing, code
/// folding, bracket matching and its animation, auto indent, closing of
/// braces and tags, mark occurrences, visible whitespace and line ends,
/// tab lines, hyperlinks, templates and the parser delay. Each setter
/// keeps its value for its getter and changes nothing on screen.
///
/// #### What is absent
///
/// Parsers and their notices, folds, tokens as objects, auto completion,
/// the syntax scheme class, the popup menu, and undo. [#forceReparsing]
/// does nothing and [#getParserCount] is zero.
public class RSyntaxTextArea extends RTextArea implements SyntaxConstants {

    public static final String ANIMATE_BRACKET_MATCHING_PROPERTY = "RSTA.animateBracketMatching";
    public static final String ANTIALIAS_PROPERTY = "RSTA.antiAlias";
    public static final String AUTO_INDENT_PROPERTY = "RSTA.autoIndent";
    public static final String BRACKET_MATCHING_PROPERTY = "RSTA.bracketMatching";
    public static final String CLEAR_WHITESPACE_LINES_PROPERTY = "RSTA.clearWhitespaceLines";
    public static final String CLOSE_CURLY_BRACES_PROPERTY = "RSTA.closeCurlyBraces";
    public static final String CLOSE_MARKUP_TAGS_PROPERTY = "RSTA.closeMarkupTags";
    public static final String CODE_FOLDING_PROPERTY = "RSTA.codeFolding";
    public static final String EOL_VISIBLE_PROPERTY = "RSTA.eolMarkersVisible";
    public static final String FOCUSABLE_TIPS_PROPERTY = "RSTA.focusableTips";
    public static final String FRACTIONAL_FONTMETRICS_PROPERTY = "RSTA.fractionalFontMetrics";
    public static final String HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY = "RSTA.highlightSecondaryLanguages";
    public static final String HYPERLINKS_ENABLED_PROPERTY = "RSTA.hyperlinksEnabled";
    public static final String INSERT_PAIRED_CHARS_PROPERTY = "RSTA.insertPairedChars";
    public static final String MARK_OCCURRENCES_PROPERTY = "RSTA.markOccurrences";
    public static final String MARKED_OCCURRENCES_CHANGED_PROPERTY = "RSTA.markedOccurrencesChanged";
    public static final String PAINT_MATCHED_BRACKET_PAIR_PROPERTY = "RSTA.paintMatchedBracketPair";
    public static final String PARSER_NOTICES_PROPERTY = "RSTA.parserNotices";
    public static final String SYNTAX_SCHEME_PROPERTY = "RSTA.syntaxScheme";
    public static final String SYNTAX_STYLE_PROPERTY = "RSTA.syntaxStyle";
    public static final String TAB_LINE_COLOR_PROPERTY = "RSTA.tabLineColor";
    public static final String TAB_LINES_PROPERTY = "RSTA.tabLines";
    public static final String USE_SELECTED_TEXT_COLOR_PROPERTY = "RSTA.useSelectedTextColor";
    public static final String VISIBLE_WHITESPACE_PROPERTY = "RSTA.visibleWhitespace";

    /// The colors of the token types in a light and in a dark palette;
    /// zero is the color of plain text.
    private static final int[] LIGHT = new int[TokenTypes.DEFAULT_NUM_TOKEN_TYPES];
    private static final int[] DARK = new int[TokenTypes.DEFAULT_NUM_TOKEN_TYPES];

    static {
        both(TokenTypes.COMMENT_EOL, 0x4f7f4f, 0x808080);
        both(TokenTypes.COMMENT_MULTILINE, 0x4f7f4f, 0x808080);
        both(TokenTypes.COMMENT_DOCUMENTATION, 0x4f7f4f, 0x629755);
        both(TokenTypes.MARKUP_COMMENT, 0x4f7f4f, 0x808080);
        both(TokenTypes.RESERVED_WORD, 0x0033b3, 0xcc7832);
        both(TokenTypes.RESERVED_WORD_2, 0x0033b3, 0xcc7832);
        both(TokenTypes.LITERAL_BOOLEAN, 0x0033b3, 0xcc7832);
        both(TokenTypes.DATA_TYPE, 0x007a8a, 0xffc66d);
        both(TokenTypes.LITERAL_NUMBER_DECIMAL_INT, 0x1750eb, 0x6897bb);
        both(TokenTypes.LITERAL_NUMBER_FLOAT, 0x1750eb, 0x6897bb);
        both(TokenTypes.LITERAL_NUMBER_HEXADECIMAL, 0x1750eb, 0x6897bb);
        both(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, 0xa31515, 0x6a8759);
        both(TokenTypes.LITERAL_CHAR, 0xa31515, 0x6a8759);
        both(TokenTypes.LITERAL_BACKQUOTE, 0xa31515, 0x6a8759);
        both(TokenTypes.ANNOTATION, 0x808000, 0xbbb529);
        both(TokenTypes.PREPROCESSOR, 0x808000, 0xbbb529);
        both(TokenTypes.MARKUP_TAG_DELIMITER, 0x7f0055, 0xe8bf6a);
        both(TokenTypes.MARKUP_TAG_NAME, 0x7f0055, 0xe8bf6a);
        both(TokenTypes.MARKUP_TAG_ATTRIBUTE, 0x174ad4, 0xbababa);
        both(TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE, 0xa31515, 0x6a8759);
    }

    private final Lexer lexer = new Lexer();
    /// A color per token type from a [Theme]; null entries follow the
    /// palette.
    private Color[] tokenColors;
    private String syntaxStyle = SYNTAX_STYLE_NONE;
    private int family;
    /// For each line, whether it starts inside a block comment.
    private boolean[] inComment;
    private String scanned;

    private boolean animateBracketMatching = true;
    private boolean antiAliasing = true;
    private boolean autoIndent = true;
    private boolean bracketMatching = true;
    private boolean clearWhitespaceLines = true;
    private boolean closeCurlyBraces = true;
    private boolean closeMarkupTags = true;
    private boolean codeFolding;
    private boolean eolMarkersVisible;
    private boolean fractionalFontMetrics;
    private boolean highlightSecondaryLanguages = true;
    private boolean hyperlinksEnabled = true;
    private boolean insertPairedCharacters = true;
    private boolean markOccurrences;
    private boolean paintMarkOccurrencesBorder;
    private boolean paintMatchedBracketPair;
    private boolean paintTabLines;
    private boolean showMatchedBracketPopup = true;
    private boolean useFocusableTips = true;
    private boolean useSelectedTextColor;
    private boolean whitespaceVisible;
    private int markOccurrencesDelay = 1000;
    private int parserDelay = 1250;
    private int rightHandSideCorrection;
    private Color hyperlinkForeground = Color.BLUE;
    private Color markOccurrencesColor;
    private Color matchedBracketBGColor = getDefaultBracketMatchBGColor();
    private Color matchedBracketBorderColor = getDefaultBracketMatchBorderColor();
    private Color tabLineColor = Color.GRAY;

    public RSyntaxTextArea() {
        super();
    }

    public RSyntaxTextArea(String text) {
        super(text);
    }

    public RSyntaxTextArea(int rows, int cols) {
        super(rows, cols);
    }

    public RSyntaxTextArea(String text, int rows, int cols) {
        super(text, rows, cols);
    }

    public RSyntaxTextArea(int textMode) {
        super(textMode);
    }

    private static void both(int type, int light, int dark) {
        LIGHT[type] = light;
        DARK[type] = dark;
    }

    // ------------------------------------------------------------ syntax

    public String getSyntaxEditingStyle() {
        return syntaxStyle;
    }

    /// Chooses how the text is colored, by one of the names in
    /// [SyntaxConstants]. Null means plain text.
    public void setSyntaxEditingStyle(String styleKey) {
        String now = styleKey == null ? SYNTAX_STYLE_NONE : styleKey;
        if (!now.equals(syntaxStyle)) {
            String old = syntaxStyle;
            syntaxStyle = now;
            family = Lexer.family(now);
            scanned = null;
            firePropertyChange(SYNTAX_STYLE_PROPERTY, old, now);
            repaint();
        }
    }

    /// The color tokens of this type are painted in.
    public Color getForegroundForTokenType(int type) {
        if (tokenColors != null && type >= 0 && type < tokenColors.length && tokenColors[type] != null) {
            return tokenColors[type];
        }
        int[] palette = LafTheme.isDark() ? DARK : LIGHT;
        if (type > 0 && type < palette.length && palette[type] != 0) {
            return new Color(palette[type]);
        }
        return getForeground();
    }

    /// The colors a [Theme] gives the token types; null entries, and a
    /// null array, follow the palette.
    void cn1TokenColors(Color[] colors) {
        tokenColors = colors;
        repaint();
    }

    Color[] cn1TokenColors() {
        return tokenColors;
    }

    /// Finds, for every line, whether it starts inside a block comment.
    private void scanStates(String all) {
        int lines = Math.max(1, getLineCount());
        boolean[] states = new boolean[lines];
        int state = 0;
        if (family != Lexer.NONE && family != Lexer.HASH) {
            for (int line = 0; line < lines; line++) {
                states[line] = state == 1;
                state = lexer.scan(cn1LineText(all, line), family, state);
            }
        }
        inComment = states;
        scanned = all;
    }

    @Override
    protected void cn1PaintLine(Graphics g, String shown, int line, int x, int baseline) {
        if (family == Lexer.NONE || !isEnabled()) {
            super.cn1PaintLine(g, shown, line, x, baseline);
            return;
        }
        String all = getText();
        if (all == null) {
            all = "";
        }
        if (inComment == null || scanned == null || !(scanned == all || scanned.equals(all))) {
            scanStates(all);
        }
        boolean[] states = inComment;
        lexer.scan(shown, family, states != null && line < states.length && states[line] ? 1 : 0);
        FontMetrics fm = getFontMetrics(getFont());
        int from = 0;
        int at = x;
        for (int i = 0; i < lexer.count; i++) {
            int end = Math.min(shown.length(), lexer.ends[i]);
            if (end <= from) {
                continue;
            }
            String run = shown.substring(from, end);
            if (lexer.types[i] != TokenTypes.WHITESPACE) {
                g.setColor(getForegroundForTokenType(lexer.types[i]));
                g.drawString(run, at, baseline);
            }
            at += fm.stringWidth(run);
            from = end;
        }
    }

    // ------------------------------------------------------------ parsing: absent

    /// Does nothing: there are no parsers.
    public void forceReparsing(int parser) {
    }

    /// Always zero: there are no parsers.
    public int getParserCount() {
        return 0;
    }

    /// Does nothing: there are no parsers.
    public void clearParsers() {
    }

    public int getParserDelay() {
        return parserDelay;
    }

    /// Recorded only.
    public void setParserDelay(int millis) {
        parserDelay = millis;
    }

    /// The marked occurrences: the ranges of
    /// [com.codename1.desktopcompat.org.fife.ui.rtextarea.SearchEngine#markAll].
    public List<DocumentRange> getMarkAllHighlightRanges() {
        return new ArrayList<DocumentRange>(cn1Marked());
    }

    /// Always empty: occurrences of the word at the caret are not marked.
    public List<DocumentRange> getMarkedOccurrences() {
        return new ArrayList<DocumentRange>();
    }

    // ------------------------------------------------------------ recorded only

    public static Color getDefaultBracketMatchBGColor() {
        return new Color(234, 234, 255);
    }

    public static Color getDefaultBracketMatchBorderColor() {
        return new Color(0, 0, 128);
    }

    public static Color getDefaultSelectionColor() {
        return new Color(200, 200, 255);
    }

    public boolean getAnimateBracketMatching() {
        return animateBracketMatching;
    }

    /// Recorded only.
    public void setAnimateBracketMatching(boolean animate) {
        boolean old = animateBracketMatching;
        animateBracketMatching = animate;
        firePropertyChange(ANIMATE_BRACKET_MATCHING_PROPERTY, old, animate);
    }

    public boolean getAntiAliasingEnabled() {
        return antiAliasing;
    }

    /// Recorded only: text is drawn as the platform draws it.
    public void setAntiAliasingEnabled(boolean enabled) {
        boolean old = antiAliasing;
        antiAliasing = enabled;
        firePropertyChange(ANTIALIAS_PROPERTY, old, enabled);
    }

    public boolean isAutoIndentEnabled() {
        return autoIndent;
    }

    /// Recorded only.
    public void setAutoIndentEnabled(boolean enabled) {
        boolean old = autoIndent;
        autoIndent = enabled;
        firePropertyChange(AUTO_INDENT_PROPERTY, old, enabled);
    }

    public final boolean isBracketMatchingEnabled() {
        return bracketMatching;
    }

    /// Recorded only.
    public void setBracketMatchingEnabled(boolean enabled) {
        boolean old = bracketMatching;
        bracketMatching = enabled;
        firePropertyChange(BRACKET_MATCHING_PROPERTY, old, enabled);
    }

    public boolean isClearWhitespaceLinesEnabled() {
        return clearWhitespaceLines;
    }

    /// Recorded only.
    public void setClearWhitespaceLinesEnabled(boolean enabled) {
        boolean old = clearWhitespaceLines;
        clearWhitespaceLines = enabled;
        firePropertyChange(CLEAR_WHITESPACE_LINES_PROPERTY, old, enabled);
    }

    public boolean getCloseCurlyBraces() {
        return closeCurlyBraces;
    }

    /// Recorded only.
    public void setCloseCurlyBraces(boolean close) {
        boolean old = closeCurlyBraces;
        closeCurlyBraces = close;
        firePropertyChange(CLOSE_CURLY_BRACES_PROPERTY, old, close);
    }

    public boolean getCloseMarkupTags() {
        return closeMarkupTags;
    }

    /// Recorded only.
    public void setCloseMarkupTags(boolean close) {
        boolean old = closeMarkupTags;
        closeMarkupTags = close;
        firePropertyChange(CLOSE_MARKUP_TAGS_PROPERTY, old, close);
    }

    public boolean isCodeFoldingEnabled() {
        return codeFolding;
    }

    /// Recorded only: there is no code folding.
    public void setCodeFoldingEnabled(boolean enabled) {
        boolean old = codeFolding;
        codeFolding = enabled;
        firePropertyChange(CODE_FOLDING_PROPERTY, old, enabled);
    }

    public boolean getEOLMarkersVisible() {
        return eolMarkersVisible;
    }

    /// Recorded only.
    public void setEOLMarkersVisible(boolean visible) {
        boolean old = eolMarkersVisible;
        eolMarkersVisible = visible;
        firePropertyChange(EOL_VISIBLE_PROPERTY, old, visible);
    }

    public boolean getFractionalFontMetricsEnabled() {
        return fractionalFontMetrics;
    }

    /// Recorded only.
    public void setFractionalFontMetricsEnabled(boolean enabled) {
        boolean old = fractionalFontMetrics;
        fractionalFontMetrics = enabled;
        firePropertyChange(FRACTIONAL_FONTMETRICS_PROPERTY, old, enabled);
    }

    public boolean getHighlightSecondaryLanguages() {
        return highlightSecondaryLanguages;
    }

    /// Recorded only.
    public void setHighlightSecondaryLanguages(boolean highlight) {
        boolean old = highlightSecondaryLanguages;
        highlightSecondaryLanguages = highlight;
        firePropertyChange(HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY, old, highlight);
    }

    public Color getHyperlinkForeground() {
        return hyperlinkForeground;
    }

    /// Recorded only.
    public void setHyperlinkForeground(Color fg) {
        if (fg == null) {
            throw new NullPointerException("fg cannot be null");
        }
        hyperlinkForeground = fg;
    }

    public boolean getHyperlinksEnabled() {
        return hyperlinksEnabled;
    }

    /// Recorded only.
    public void setHyperlinksEnabled(boolean enabled) {
        boolean old = hyperlinksEnabled;
        hyperlinksEnabled = enabled;
        firePropertyChange(HYPERLINKS_ENABLED_PROPERTY, old, enabled);
    }

    public boolean getInsertPairedCharacters() {
        return insertPairedCharacters;
    }

    /// Recorded only.
    public void setInsertPairedCharacters(boolean insert) {
        boolean old = insertPairedCharacters;
        insertPairedCharacters = insert;
        firePropertyChange(INSERT_PAIRED_CHARS_PROPERTY, old, insert);
    }

    public boolean getMarkOccurrences() {
        return markOccurrences;
    }

    /// Recorded only.
    public void setMarkOccurrences(boolean mark) {
        boolean old = markOccurrences;
        markOccurrences = mark;
        firePropertyChange(MARK_OCCURRENCES_PROPERTY, old, mark);
    }

    public Color getMarkOccurrencesColor() {
        return markOccurrencesColor != null ? markOccurrencesColor : getMarkAllHighlightColor();
    }

    /// Recorded only.
    public void setMarkOccurrencesColor(Color color) {
        markOccurrencesColor = color;
    }

    public int getMarkOccurrencesDelay() {
        return markOccurrencesDelay;
    }

    /// Recorded only.
    public void setMarkOccurrencesDelay(int millis) {
        markOccurrencesDelay = millis;
    }

    public boolean getPaintMarkOccurrencesBorder() {
        return paintMarkOccurrencesBorder;
    }

    /// Recorded only.
    public void setPaintMarkOccurrencesBorder(boolean paint) {
        paintMarkOccurrencesBorder = paint;
    }

    public Color getMatchedBracketBGColor() {
        return matchedBracketBGColor;
    }

    /// Recorded only.
    public void setMatchedBracketBGColor(Color color) {
        matchedBracketBGColor = color;
    }

    public Color getMatchedBracketBorderColor() {
        return matchedBracketBorderColor;
    }

    /// Recorded only.
    public void setMatchedBracketBorderColor(Color color) {
        matchedBracketBorderColor = color;
    }

    public boolean getPaintMatchedBracketPair() {
        return paintMatchedBracketPair;
    }

    /// Recorded only.
    public void setPaintMatchedBracketPair(boolean paint) {
        boolean old = paintMatchedBracketPair;
        paintMatchedBracketPair = paint;
        firePropertyChange(PAINT_MATCHED_BRACKET_PAIR_PROPERTY, old, paint);
    }

    public boolean getPaintTabLines() {
        return paintTabLines;
    }

    /// Recorded only.
    public void setPaintTabLines(boolean paint) {
        boolean old = paintTabLines;
        paintTabLines = paint;
        firePropertyChange(TAB_LINES_PROPERTY, old, paint);
    }

    public Color getTabLineColor() {
        return tabLineColor;
    }

    /// Recorded only.
    public void setTabLineColor(Color color) {
        Color old = tabLineColor;
        tabLineColor = color;
        firePropertyChange(TAB_LINE_COLOR_PROPERTY, old, color);
    }

    public int getRightHandSideCorrection() {
        return rightHandSideCorrection;
    }

    /// Recorded only.
    public void setRightHandSideCorrection(int correction) {
        rightHandSideCorrection = correction;
    }

    public boolean getShowMatchedBracketPopup() {
        return showMatchedBracketPopup;
    }

    /// Recorded only.
    public void setShowMatchedBracketPopup(boolean show) {
        showMatchedBracketPopup = show;
    }

    public boolean getUseFocusableTips() {
        return useFocusableTips;
    }

    /// Recorded only.
    public void setUseFocusableTips(boolean use) {
        boolean old = useFocusableTips;
        useFocusableTips = use;
        firePropertyChange(FOCUSABLE_TIPS_PROPERTY, old, use);
    }

    public boolean getUseSelectedTextColor() {
        return useSelectedTextColor;
    }

    /// Recorded only: selected text keeps its colors.
    public void setUseSelectedTextColor(boolean use) {
        boolean old = useSelectedTextColor;
        useSelectedTextColor = use;
        firePropertyChange(USE_SELECTED_TEXT_COLOR_PROPERTY, old, use);
    }

    public boolean isWhitespaceVisible() {
        return whitespaceVisible;
    }

    /// Recorded only.
    public void setWhitespaceVisible(boolean visible) {
        boolean old = whitespaceVisible;
        whitespaceVisible = visible;
        firePropertyChange(VISIBLE_WHITESPACE_PROPERTY, old, visible);
    }

    /// Does nothing: there is no line range to show in the gutter.
    public void setActiveLineRange(int min, int max) {
    }

    /// Drops the token colors a theme gave, so they follow the palette.
    public void restoreDefaultSyntaxScheme() {
        cn1TokenColors(null);
    }
}
