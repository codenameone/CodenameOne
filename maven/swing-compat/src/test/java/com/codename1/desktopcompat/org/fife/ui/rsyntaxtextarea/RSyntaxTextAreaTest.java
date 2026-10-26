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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatDarkLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatLightLaf;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.LookAndFeel;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.Gutter;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.RTextScrollPane;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.SearchContext;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.SearchEngine;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.SearchResult;
import com.codename1.desktopcompat.rt.LafTheme;
import java.io.ByteArrayInputStream;
import org.junit.After;
import org.junit.Test;

/// The reduced RSyntaxTextArea: what it paints, where its lines are, and
/// what its search finds.
public class RSyntaxTextAreaTest extends KernelTestBase {

    private static final String TEXT = "first line\nsecond line\n\tthird\nfourth\nfifth\nsixth\nseventh\n"
            + "eighth\nninth\ntenth\neleventh\ntwelfth\nthirteenth\nfourteenth\nfifteenth\nsixteenth\n"
            + "seventeenth\neighteenth\nnineteenth\ntwentieth";

    private RSyntaxTextArea area;
    private RTextScrollPane pane;
    private JFrame frame;

    @After
    public void forget() throws Exception {
        UIManager.setLookAndFeel((LookAndFeel) null);
        HeadlessImplementation.darkMode = false;
        LafTheme.reset();
    }

    private void open(String text) {
        area = new RSyntaxTextArea(text);
        area.setEditable(false);
        pane = new RTextScrollPane(area);
        frame = new JFrame();
        frame.add(pane, BorderLayout.CENTER);
        frame.setSize(400, 200);
        show(frame);
    }

    private static int rgb(Color c) {
        return c.getRGB() & 0xffffff;
    }

    /// How many pixels of a band of a component are not `background`.
    private int inked(int[][] rows, Component c, int y, int height, int background) {
        int n = 0;
        for (int yy = y; yy < y + height; yy++) {
            for (int x = 0; x < c.getWidth() - 1; x++) {
                if ((pixel(rows, c, x, yy) & 0xffffff) != background) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    public void aLineHighlightIsPaintedBehindItsLineAndGoesWhenRemoved() throws Exception {
        open(TEXT);
        area.setHighlightCurrentLine(false);
        Color red = new Color(200, 30, 30);
        Color green = new Color(30, 160, 60);
        Object first = area.addLineHighlight(1, red);
        area.addLineHighlight(3, green);
        int h = area.getLineHeight();
        int x = area.getWidth() - 3;
        int[][] rows = raster(frame);
        assertEquals(rgb(red), pixel(rows, area, x, area.yForLine(1) + h / 2) & 0xffffff);
        assertEquals(rgb(red), pixel(rows, area, x, area.yForLine(1) + 1) & 0xffffff);
        assertEquals(rgb(green), pixel(rows, area, x, area.yForLine(3) + h / 2) & 0xffffff);
        int plain = pixel(rows, area, x, area.yForLine(2) + h / 2) & 0xffffff;
        assertEquals(rgb(area.getBackground()), plain);

        area.removeLineHighlight(first);
        rows = raster(frame);
        assertEquals(plain, pixel(rows, area, x, area.yForLine(1) + h / 2) & 0xffffff);
        assertEquals(rgb(green), pixel(rows, area, x, area.yForLine(3) + h / 2) & 0xffffff);

        area.removeAllLineHighlights();
        rows = raster(frame);
        assertEquals(plain, pixel(rows, area, x, area.yForLine(3) + h / 2) & 0xffffff);
        try {
            area.addLineHighlight(500, red);
            fail("no such line");
        } catch (com.codename1.desktopcompat.javax.swing.text.BadLocationException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void theCurrentLineIsHighlightedAndFollowsTheCaret() throws Exception {
        open(TEXT);
        Color mark = new Color(250, 240, 120);
        area.setCurrentLineHighlightColor(mark);
        assertTrue(area.getHighlightCurrentLine());
        area.setCaretPosition(area.getLineStartOffset(2));
        int h = area.getLineHeight();
        int x = area.getWidth() - 3;
        int[][] rows = raster(frame);
        assertEquals(rgb(mark), pixel(rows, area, x, area.yForLine(2) + h / 2) & 0xffffff);
        area.setCaretPosition(area.getLineStartOffset(4));
        rows = raster(frame);
        assertEquals(rgb(mark), pixel(rows, area, x, area.yForLine(4) + h / 2) & 0xffffff);
        assertFalse(rgb(mark) == (pixel(rows, area, x, area.yForLine(2) + h / 2) & 0xffffff));
        assertEquals(4, area.getCaretLineNumber());
    }

    @Test
    public void aCharacterIsWhereItsLineAndColumnPutIt() throws Exception {
        open(TEXT);
        int h = area.getLineHeight();
        assertTrue(h > 0);
        Rectangle start = area.modelToView(0);
        Rectangle third = area.modelToView(area.getLineStartOffset(2));
        assertEquals(area.yForLine(2), third.y);
        assertEquals(start.y + 2 * h, third.y);
        assertEquals(start.x, third.x);
        assertEquals(h, third.height);
        // A tab reaches the next tab stop: four columns.
        Rectangle afterTab = area.modelToView(area.getLineStartOffset(2) + 1);
        Rectangle fourIn = area.modelToView(4);
        assertEquals(fourIn.x, afterTab.x);
        assertTrue(afterTab.x > third.x);
        assertEquals(area.yForLine(2), area.yForLineContaining(area.getLineStartOffset(2) + 3));
        // And back again.
        assertEquals(4, area.viewToModel(new Point(fourIn.x + 1, fourIn.y + 1)));
        assertEquals(area.getLineStartOffset(2) + 1, area.viewToModel(new Point(afterTab.x + 1, afterTab.y + 1)));
        assertEquals(TEXT.length(), area.viewToModel(new Point(5000, 50000)));
        // Every line has a row, so the text is as high as its lines.
        assertEquals(20, area.getLineCount());
        assertTrue(area.getPreferredSize().height >= 20 * h);
        assertEquals("Monospaced", area.getFont().getFamily());
    }

    @Test
    public void theGutterNumbersEveryLineAndScrollsWithTheText() throws Exception {
        StringBuilder many = new StringBuilder(TEXT);
        for (int i = 0; i < 100; i++) {
            many.append("\nline ").append(i);
        }
        open(many.toString());
        Gutter gutter = pane.getGutter();
        assertSame(area, pane.getTextArea());
        assertTrue(pane.getLineNumbersEnabled());
        assertTrue(gutter.getWidth() > 0);
        int h = area.getLineHeight();
        int[][] rows = raster(frame);
        int bg = pixel(rows, gutter, 1, 1) & 0xffffff;
        assertTrue("line 1 has a number", inked(rows, gutter, 0, h, bg) > 0);
        assertTrue("line 3 has a number", inked(rows, gutter, 2 * h, h, bg) > 0);

        // Scrolled, the gutter is at the same place as the text.
        assertTrue(area.getSize() + " in " + pane.getViewport().getSize() + " pref " + area.getPreferredSize()
                + " line " + h + " gutter " + gutter.getSize() + " pane " + pane.getSize(),
                area.getHeight() > pane.getViewport().getHeight());
        pane.getVerticalScrollBar().setValue(5 * h);
        raster(frame);
        assertEquals(5 * h, pane.getViewport().getViewPosition().y);
        assertEquals(5 * h, pane.getRowHeader().getViewPosition().y);

        // Without line numbers the text takes the room.
        int with = area.getWidth();
        pane.setLineNumbersEnabled(false);
        raster(frame);
        assertFalse(pane.getLineNumbersEnabled());
        assertTrue(pane.getViewport().getWidth() > 0);
        assertTrue(pane.getViewport().getWidth() >= with || area.getWidth() >= with);
        pane.setLineNumbersEnabled(true);
        pane.setFoldIndicatorEnabled(true);
        assertTrue(pane.isFoldIndicatorEnabled());
        raster(frame);
        assertTrue(gutter.getWidth() > 0);
    }

    @Test
    public void aShortTextHasNoNumbersBelowItsLastLine() throws Exception {
        open("one\ntwo");
        Gutter gutter = pane.getGutter();
        int h = area.getLineHeight();
        int[][] rows = raster(frame);
        int bg = pixel(rows, gutter, 1, 1) & 0xffffff;
        assertTrue(inked(rows, gutter, h, h, bg) > 0);
        assertEquals(0, inked(rows, gutter, 3 * h, h, bg));
    }

    @Test
    public void plainSearchFindsSelectsAndWraps() {
        open("one two One twofold two");
        area.setCaretPosition(0);
        SearchContext context = new SearchContext("two");
        context.setMarkAll(false);
        SearchResult r = SearchEngine.find(area, context);
        assertTrue(r.wasFound());
        assertEquals(1, r.getCount());
        assertEquals(4, r.getMatchRange().getStartOffset());
        assertEquals(7, r.getMatchRange().getEndOffset());
        assertEquals(4, area.getSelectionStart());
        assertEquals(7, area.getSelectionEnd());
        assertEquals(12, SearchEngine.find(area, context).getMatchRange().getStartOffset());

        context.setWholeWord(true);
        r = SearchEngine.find(area, context);
        assertEquals(20, r.getMatchRange().getStartOffset());
        assertFalse(SearchEngine.find(area, context).wasFound());
        context.setSearchWrap(true);
        r = SearchEngine.find(area, context);
        assertEquals(4, r.getMatchRange().getStartOffset());
        assertTrue(r.isWrapped());

        context.setSearchForward(false);
        context.setSearchWrap(false);
        assertFalse(SearchEngine.find(area, context).wasFound());

        SearchContext one = new SearchContext("one");
        one.setMarkAll(false);
        area.setCaretPosition(1);
        assertEquals(8, SearchEngine.find(area, one).getMatchRange().getStartOffset());
        one.setMatchCase(true);
        area.setCaretPosition(1);
        assertFalse(SearchEngine.find(area, one).wasFound());
        one.setSearchForward(false);
        area.setCaretPosition(6);
        assertEquals(0, SearchEngine.find(area, one).getMatchRange().getStartOffset());

        assertEquals(4, SearchEngine.getNextMatchPos("two", "one two two", true, true, false));
        assertEquals(8, SearchEngine.getNextMatchPos("two", "one two two", false, true, false));
        assertEquals(-1, SearchEngine.getNextMatchPos("TWO", "one two two", true, true, false));
    }

    @Test
    public void aRegularExpressionSearchFindsNothing() {
        open("one two");
        area.setCaretPosition(0);
        SearchContext context = new SearchContext("t.o");
        context.setRegularExpression(true);
        SearchResult r = SearchEngine.find(area, context);
        assertFalse(r.wasFound());
        assertNull(r.getMatchRange());
        assertEquals(0, SearchEngine.markAll(area, context).getMarkedCount());
        assertEquals(0, SearchEngine.replaceAll(area, context).getCount());
        assertEquals("one two", area.getText());
    }

    @Test
    public void markAllMarksEveryOccurrenceAndPaintsIt() throws Exception {
        open("alpha beta\nbeta gamma\nBETA");
        area.setHighlightCurrentLine(false);
        Color mark = new Color(255, 200, 0);
        area.setMarkAllHighlightColor(mark);
        SearchContext context = new SearchContext("beta");
        SearchResult r = SearchEngine.markAll(area, context);
        assertEquals(3, r.getMarkedCount());
        assertFalse(r.wasFound());
        assertEquals(3, area.getMarkAllHighlightRanges().size());

        // Marked blanks show the mark itself, with no letter over it:
        // from the first marked character to the last, on its line only.
        // (The test font draws a run of text as a block, and a colored
        // text draws its blanks not at all, so the style is set.)
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        SearchContext blanks = new SearchContext("   ");
        area.setText("ab\ncd   ef  gh");
        assertEquals(1, SearchEngine.markAll(area, blanks).getMarkedCount());
        int h = area.getLineHeight();
        int[][] rows = raster(frame);
        Rectangle from = area.modelToView(5);
        Rectangle to = area.modelToView(8);
        Rectangle two = area.modelToView(10);
        assertEquals(h, from.y);
        assertEquals(rgb(mark), pixel(rows, area, from.x + 1, from.y + h / 2) & 0xffffff);
        assertEquals(rgb(mark), pixel(rows, area, to.x - 1, to.y + 1) & 0xffffff);
        assertEquals(rgb(mark), pixel(rows, area, to.x - 1, to.y + h - 1) & 0xffffff);
        assertEquals(rgb(area.getBackground()), pixel(rows, area, two.x + 2, two.y + h / 2) & 0xffffff);
        assertEquals(rgb(area.getBackground()), pixel(rows, area, from.x + 1, h + h + h / 2) & 0xffffff);
        area.setText("alpha beta\nbeta gamma\nBETA");
        assertEquals(3, SearchEngine.markAll(area, context).getMarkedCount());

        context.setMatchCase(true);
        assertEquals(2, SearchEngine.markAll(area, context).getMarkedCount());
        // Finding marks as well, unless the context says not to.
        area.setCaretPosition(0);
        context.setMatchCase(false);
        r = SearchEngine.find(area, context);
        assertEquals(3, r.getMarkedCount());
        assertTrue(r.wasFound());
        context.setMarkAll(false);
        assertEquals(0, SearchEngine.markAll(area, context).getMarkedCount());
        assertEquals(0, area.getMarkAllHighlightRanges().size());
    }

    @Test
    public void replaceAndReplaceAll() {
        open("cat dog cat catalog cat");
        area.setCaretPosition(0);
        SearchContext context = new SearchContext("cat");
        context.setReplaceWith("bird");
        context.setWholeWord(true);
        SearchResult r = SearchEngine.replace(area, context);
        assertEquals(1, r.getCount());
        assertEquals("bird dog cat catalog cat", area.getText());
        // The next occurrence is selected.
        assertEquals(9, area.getSelectionStart());
        assertEquals(12, area.getSelectionEnd());
        r = SearchEngine.replaceAll(area, context);
        assertEquals(2, r.getCount());
        assertEquals("bird dog bird catalog bird", area.getText());
        assertEquals(0, SearchEngine.replaceAll(area, context).getCount());
        SearchContext copy = context.clone();
        assertEquals("cat", copy.getSearchFor());
        assertEquals("bird", copy.getReplaceWith());
        assertTrue(copy.getWholeWord());
    }

    @Test
    public void theColorsFollowThePaletteAndAThemeReplacesThem() throws Exception {
        open("int x = 1; // note");
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        assertEquals("text/java", area.getSyntaxEditingStyle());
        assertTrue(FlatLightLaf.setup());
        int[][] rows = raster(frame);
        int light = pixel(rows, area, area.getWidth() - 3, area.getHeight() - 3);
        Color lightComment = area.getForegroundForTokenType(TokenTypes.COMMENT_EOL);
        assertTrue(FlatDarkLaf.setup());
        rows = raster(frame);
        int dark = pixel(rows, area, area.getWidth() - 3, area.getHeight() - 3);
        assertTrue("light " + Integer.toHexString(light), (light >> 8 & 0xff) >= 128);
        assertTrue("dark " + Integer.toHexString(dark), (dark >> 8 & 0xff) < 128);
        assertFalse(lightComment.equals(area.getForegroundForTokenType(TokenTypes.COMMENT_EOL)));
        int gutter = pixel(rows, pane.getGutter(), 1, 1);
        assertTrue("gutter " + Integer.toHexString(gutter), (gutter >> 8 & 0xff) < 128);

        String xml = "<?xml version=\"1.0\"?>\n<RSyntaxTheme version=\"1.0\">\n"
                + "  <baseFont size=\"15\"/>\n  <background color=\"102030\"/>\n  <caret color=\"ffffff\"/>\n"
                + "  <selection useFG=\"false\" bg=\"$405060\" roundedEdges=\"false\"/>\n"
                + "  <currentLineHighlight color=\"203040\" fade=\"false\"/>\n"
                + "  <markAllHighlight color=\"708090\"/>\n  <gutterBackground color=\"0a0b0c\"/>\n"
                + "  <gutterBorder color=\"default\"/>\n  <lineNumbers fg=\"aabbcc\" fontSize=\"11\"/>\n"
                + "  <tokenStyles>\n    <style token=\"COMMENT_EOL\" fg=\"112233\" italic=\"true\"/>\n"
                + "    <style token=\"IDENTIFIER\" fg=\"ddeeff\"/>\n    <style token=\"NO_SUCH\" fg=\"010101\"/>\n"
                + "  </tokenStyles>\n</RSyntaxTheme>\n";
        Theme theme = Theme.load(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        assertEquals(0x102030, rgb(theme.bgColor));
        assertEquals(0x405060, rgb(theme.selectionBG));
        assertNull(theme.gutterBorderColor);
        assertEquals(15, theme.baseFont.getSize());
        theme.apply(area);
        assertEquals(0x102030, rgb(area.getBackground()));
        assertEquals(0x203040, rgb(area.getCurrentLineHighlightColor()));
        assertEquals(0x708090, rgb(area.getMarkAllHighlightColor()));
        assertEquals(0x112233, rgb(area.getForegroundForTokenType(TokenTypes.COMMENT_EOL)));
        assertEquals(0xddeeff, rgb(area.getForeground()));
        assertEquals(0xaabbcc, rgb(pane.getGutter().getLineNumberColor()));
        rows = raster(frame);
        assertEquals(0x102030, pixel(rows, area, area.getWidth() - 3, area.getHeight() - 3) & 0xffffff);
        assertEquals(0x0a0b0c, pixel(rows, pane.getGutter(), 1, pane.getGutter().getHeight() - 2) & 0xffffff);
        assertEquals(0x102030, rgb(new Theme(area).bgColor));
        try {
            Theme.load(null);
            fail("a theme that is not there");
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void theLexerTellsCommentsStringsNumbersAndKeywordsApart() {
        Lexer lexer = new Lexer();
        assertEquals(Lexer.C_LIKE, Lexer.family(SyntaxConstants.SYNTAX_STYLE_JAVA));
        assertEquals(Lexer.HASH, Lexer.family(SyntaxConstants.SYNTAX_STYLE_PYTHON));
        assertEquals(Lexer.SQL, Lexer.family(SyntaxConstants.SYNTAX_STYLE_SQL));
        assertEquals(Lexer.MARKUP, Lexer.family(SyntaxConstants.SYNTAX_STYLE_XML));
        assertEquals(Lexer.NONE, Lexer.family(SyntaxConstants.SYNTAX_STYLE_NONE));
        assertEquals(Lexer.NONE, Lexer.family(null));

        String line = "int n = 0x1F; String s = \"a // b\"; // done";
        assertEquals(0, lexer.scan(line, Lexer.C_LIKE, 0));
        assertEquals(TokenTypes.DATA_TYPE, typeAt(lexer, line.indexOf("int")));
        assertEquals(TokenTypes.IDENTIFIER, typeAt(lexer, line.indexOf("n =")));
        assertEquals(TokenTypes.LITERAL_NUMBER_HEXADECIMAL, typeAt(lexer, line.indexOf("0x1F")));
        assertEquals(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, typeAt(lexer, line.indexOf("// b")));
        assertEquals(TokenTypes.COMMENT_EOL, typeAt(lexer, line.indexOf("// done") + 3));

        assertEquals(1, lexer.scan("return x; /* open", Lexer.C_LIKE, 0));
        assertEquals(TokenTypes.RESERVED_WORD, typeAt(lexer, 0));
        assertEquals(TokenTypes.COMMENT_MULTILINE, typeAt(lexer, 14));
        assertEquals(1, lexer.scan("still inside", Lexer.C_LIKE, 1));
        assertEquals(TokenTypes.COMMENT_MULTILINE, typeAt(lexer, 3));
        assertEquals(0, lexer.scan("end */ if (true)", Lexer.C_LIKE, 1));
        assertEquals(TokenTypes.COMMENT_MULTILINE, typeAt(lexer, 1));
        assertEquals(TokenTypes.RESERVED_WORD, typeAt(lexer, 7));
        assertEquals(TokenTypes.LITERAL_BOOLEAN, typeAt(lexer, 11));

        lexer.scan("def f(): # note", Lexer.HASH, 0);
        assertEquals(TokenTypes.RESERVED_WORD, typeAt(lexer, 0));
        assertEquals(TokenTypes.COMMENT_EOL, typeAt(lexer, 12));
        lexer.scan("select a from t -- all", Lexer.SQL, 0);
        assertEquals(TokenTypes.RESERVED_WORD, typeAt(lexer, 0));
        assertEquals(TokenTypes.IDENTIFIER, typeAt(lexer, 7));
        assertEquals(TokenTypes.COMMENT_EOL, typeAt(lexer, 18));
        String tag = "<a href=\"x\">text</a> <!-- c";
        assertEquals(1, lexer.scan(tag, Lexer.MARKUP, 0));
        assertEquals(TokenTypes.MARKUP_TAG_NAME, typeAt(lexer, 1));
        assertEquals(TokenTypes.MARKUP_TAG_ATTRIBUTE, typeAt(lexer, 3));
        assertEquals(TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE, typeAt(lexer, 9));
        assertEquals(TokenTypes.IDENTIFIER, typeAt(lexer, 13));
        assertEquals(TokenTypes.MARKUP_COMMENT, typeAt(lexer, tag.length() - 1));
    }

    private static int typeAt(Lexer lexer, int index) {
        for (int i = 0; i < lexer.count; i++) {
            if (index < lexer.ends[i]) {
                return lexer.types[i];
            }
        }
        return -1;
    }

    @Test
    public void theEditorSwitchesAreRecorded() {
        open("x");
        area.setAntiAliasingEnabled(false);
        area.setCodeFoldingEnabled(true);
        area.setBracketMatchingEnabled(false);
        area.setAnimateBracketMatching(false);
        area.setLineWrap(true);
        area.forceReparsing(0);
        assertFalse(area.getAntiAliasingEnabled());
        assertTrue(area.isCodeFoldingEnabled());
        assertFalse(area.isBracketMatchingEnabled());
        assertFalse(area.getAnimateBracketMatching());
        assertEquals(0, area.getParserCount());
        assertFalse(area.canUndo());
        assertEquals(4, area.getTabSize());
    }
}
