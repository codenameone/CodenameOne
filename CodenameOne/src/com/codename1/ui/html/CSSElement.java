/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.html;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.plaf.Style;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The CSSElement class defines a single CSS element with its attributes and children.
 * It extends Element and adds to it certain CSS-specific methods.
 * Each CSSElement object is in fact a CSS selector.
 *
 * @author Ofir Leitner
 */
class CSSElement extends HTMLElement {

    /**
     * Defines the Dots-per-Inch - used when CSS length values are denoted in units like inches (in), cm, mm, points (pt) etc.
     * This can be customized per device in build time. However since these units are not mandatory in WCSS so even if this is not accurate no harm done.
     * In any case, always prefer using the supported length units: px, em, ex
     */
    private static final int DPI=72;

    /**
     * Possible suffix strings for values of the CSS length type
     */
    private final static String[] CSS_LENGTH_SUFFIX = {"px","em","ex","in","pt","pc","mm","cm"};

    /**
     * The multiplier values for the suffix strings
     */
    private final static int CSS_LENGTH_FACTORS[] = {1,2,1,DPI,DPI/72,DPI/6,(int)(DPI/2.54/10),(int)(DPI/2.54)}; // unit factors for {"px","em","ex","in","pt","pc","mm","cm"};

    private final static int LENGTH_SUFFIX_PX = 0;
    private final static int LENGTH_SUFFIX_EM = 1;
    private final static int LENGTH_SUFFIX_EX = 2;



    private final static String CENTER_STR = "center"; // Since this value is used in multiple CSS properties it is defined as a constant
    private static final String CSS_NONE = "none"; // Since this value is used in multiple CSS properties it is defined as a constant

    /**
     * A constant representing both the attribute value of 'font-variant' and the font name that should be given to small-caps fonts
     * Since J2ME doesn't support small-caps fonts, when a small-caps font varinat is requested
     * the font-family is changed to "smallcaps" which should be loaded to HTMLComponent and the theme as a bitmap font
     * If no smallcaps font is found at all, then the family stays the same, but if even only one is found - the best match will be used.
     */
    final static String SMALL_CAPS_STRING = "small-caps";

    // Background repeat strings and their corresponding values
    static final String[] BG_REPEAT_STRINGS = {"repeat","repeat-x","repeat-y","no-repeat"};
    private static final int[] BG_REPEAT_VALS = {Style.BACKGROUND_IMAGE_TILE_BOTH,Style.BACKGROUND_IMAGE_TILE_HORIZONTAL,Style.BACKGROUND_IMAGE_TILE_VERTICAL,0};

    // Horizontal alignment strings and their corresponding values
    private static final String[] TEXT_ALIGN_STRINGS = {"left","right",CENTER_STR};
    private static final int[] TEXT_ALIGN_VALS = {Component.LEFT,Component.RIGHT,Component.CENTER};

    // Vertical alignment strings and their corresponding values
    private static final String[] VERTICAL_ALIGN_STRINGS = {"top","middle","bottom","baseline","sub","super"};
    private static final int[] VERTICAL_ALIGN_VALS = {Component.TOP,Component.CENTER,Component.BOTTOM,-1,-1,-1};

    // Border style strings
    private static final String[] BORDER_STYLE_STRINGS = {CSS_NONE,"solid","dotted","dashed","double","groove","ridge","inset","outset"};

    // Border width strings and their corresponding values
    private static final String[] BORDER_WIDTH_STRINGS = {"thin","medium","thick"};
    private static final int[] BORDER_WIDTH_VALS = {1,3,5};
    
    /**
     * The default border width (when not specified)
     */
    static final int BORDER_DEFAULT_WIDTH = BORDER_WIDTH_VALS[1]; // medium

    //
    // Font strings and values constants
    //
    static final int FONT_SIZE_SMALLER = -3;
    static final int FONT_SIZE_LARGER = -2;

    // Constants defining the approximate sizes of a small/medium/large font (This can be tweaked per platform)
    static final int FONT_SIZE_SMALL = 12;
    static final int FONT_SIZE_MEDIUM = 15;
    static final int FONT_SIZE_LARGE = 19;

    // Font size strings and their corresponding values
    private static final String[] FONT_SIZE_STRINGS = {"xx-small","x-small","small","medium","large","x-large","xx-large","smaller","larger"};
    private static final int[] FONT_SIZE_VALS = {FONT_SIZE_SMALL-4,FONT_SIZE_SMALL-2,FONT_SIZE_SMALL,FONT_SIZE_MEDIUM,FONT_SIZE_LARGE,FONT_SIZE_LARGE+2,FONT_SIZE_LARGE+4,FONT_SIZE_SMALLER,FONT_SIZE_LARGER};

    // Font style strings and their corresponding values, note that oblique is translated to italic
    private static final String[] FONT_STYLE_STRINGS = {"normal","italic","oblique"};
    private static final int[] FONT_STYLE_VALS = {Font.STYLE_PLAIN,Font.STYLE_ITALIC,Font.STYLE_ITALIC};

    // Font weight strings and their corresponding values
    // Note that since we have only two levels of boldness (plain, bold) - bolder always means bold, and lighter always means plain
    private static final String[] FONT_WEIGHT_STRINGS = {"normal","bold","bolder","lighter","100","200","300","400","500","600","700","800","900"};
    private static final int[] FONT_WEIGHT_VALS = {Font.STYLE_PLAIN,Font.STYLE_BOLD,Font.STYLE_BOLD,Font.STYLE_PLAIN,Font.STYLE_PLAIN,Font.STYLE_PLAIN,Font.STYLE_PLAIN,Font.STYLE_PLAIN,Font.STYLE_PLAIN,Font.STYLE_BOLD,Font.STYLE_BOLD,Font.STYLE_BOLD,Font.STYLE_BOLD};

    /**
     * Since the other tags are 0 based the CSS tags should start at this point
     */
    static final int CSS_STYLE_ID_OFFSET = 500;

    // CSS Attributes
    static final int CSS_BACKGROUND_COLOR = CSS_STYLE_ID_OFFSET;
    static final int CSS_BACKGROUND_IMAGE = CSS_STYLE_ID_OFFSET + 1;
    static final int CSS_BACKGROUND_REPEAT = CSS_STYLE_ID_OFFSET + 2;
    static final int CSS_BACKGROUND_ATTACHMENT = CSS_STYLE_ID_OFFSET + 3;
    static final int CSS_BACKGROUND_POSITION_X = CSS_STYLE_ID_OFFSET + 4;
    static final int CSS_BACKGROUND_POSITION_Y = CSS_STYLE_ID_OFFSET + 5;
    static final int CSS_BORDER_TOP_WIDTH = CSS_STYLE_ID_OFFSET + 6;
    static final int CSS_BORDER_LEFT_WIDTH = CSS_STYLE_ID_OFFSET + 7;
    static final int CSS_BORDER_BOTTOM_WIDTH = CSS_STYLE_ID_OFFSET + 8;
    static final int CSS_BORDER_RIGHT_WIDTH = CSS_STYLE_ID_OFFSET + 9;
    static final int CSS_BORDER_TOP_STYLE = CSS_STYLE_ID_OFFSET + 10;
    static final int CSS_BORDER_LEFT_STYLE = CSS_STYLE_ID_OFFSET + 11;
    static final int CSS_BORDER_BOTTOM_STYLE = CSS_STYLE_ID_OFFSET + 12;
    static final int CSS_BORDER_RIGHT_STYLE = CSS_STYLE_ID_OFFSET + 13;
    static final int CSS_BORDER_TOP_COLOR= CSS_STYLE_ID_OFFSET + 14;
    static final int CSS_BORDER_LEFT_COLOR = CSS_STYLE_ID_OFFSET + 15;
    static final int CSS_BORDER_BOTTOM_COLOR = CSS_STYLE_ID_OFFSET + 16;
    static final int CSS_BORDER_RIGHT_COLOR = CSS_STYLE_ID_OFFSET + 17;
    static final int CSS_CLEAR = CSS_STYLE_ID_OFFSET + 18;
    static final int CSS_COLOR = CSS_STYLE_ID_OFFSET + 19;
    static final int CSS_VERTICAL_ALIGN = CSS_STYLE_ID_OFFSET + 20;
    static final int CSS_DISPLAY = CSS_STYLE_ID_OFFSET + 21;
    static final int CSS_FLOAT = CSS_STYLE_ID_OFFSET + 22;
    static final int CSS_FONT_FAMILY = CSS_STYLE_ID_OFFSET + 23;
    static final int CSS_FONT_SIZE = CSS_STYLE_ID_OFFSET + 24;
    static final int CSS_FONT_STYLE = CSS_STYLE_ID_OFFSET + 25;
    static final int CSS_FONT_WEIGHT = CSS_STYLE_ID_OFFSET + 26;
    static final int CSS_FONT_VARIANT = CSS_STYLE_ID_OFFSET + 27;
    static final int CSS_HEIGHT = CSS_STYLE_ID_OFFSET + 28;
    static final int CSS_WIDTH = CSS_STYLE_ID_OFFSET + 29;
    static final int CSS_VISIBILITY = CSS_STYLE_ID_OFFSET + 30;
    static final int CSS_WHITE_SPACE = CSS_STYLE_ID_OFFSET + 31;
    static final int CSS_LIST_STYLE_IMAGE = CSS_STYLE_ID_OFFSET + 32;
    static final int CSS_LIST_STYLE_POSITION = CSS_STYLE_ID_OFFSET + 33;
    static final int CSS_LIST_STYLE_TYPE = CSS_STYLE_ID_OFFSET + 34;
    static final int CSS_MARGIN_TOP = CSS_STYLE_ID_OFFSET + 35;
    static final int CSS_MARGIN_LEFT = CSS_STYLE_ID_OFFSET + 36;
    static final int CSS_MARGIN_BOTTOM = CSS_STYLE_ID_OFFSET + 37;
    static final int CSS_MARGIN_RIGHT = CSS_STYLE_ID_OFFSET + 38;
    static final int CSS_PADDING_TOP = CSS_STYLE_ID_OFFSET + 39;
    static final int CSS_PADDING_LEFT = CSS_STYLE_ID_OFFSET + 40;
    static final int CSS_PADDING_BOTTOM = CSS_STYLE_ID_OFFSET + 41;
    static final int CSS_PADDING_RIGHT = CSS_STYLE_ID_OFFSET + 42;
    static final int CSS_TEXT_ALIGN = CSS_STYLE_ID_OFFSET + 43;
    static final int CSS_TEXT_DECORATION = CSS_STYLE_ID_OFFSET + 44;
    static final int CSS_TEXT_INDENT = CSS_STYLE_ID_OFFSET + 45;
    static final int CSS_TEXT_TRANSFORM = CSS_STYLE_ID_OFFSET + 46;
    static final int CSS_WAP_ACCESSKEY = CSS_STYLE_ID_OFFSET + 47;
    static final int CSS_WAP_INPUT_FORMAT = CSS_STYLE_ID_OFFSET + 48;
    static final int CSS_WAP_INPUT_REQUIRED = CSS_STYLE_ID_OFFSET + 49;
    static final int CSS_PAGEURL = CSS_STYLE_ID_OFFSET + 50; // This attribute is not a CSS attribute, but rather for internal CodenameOne usage, to identify the stylesheet's base url
    static final int CSS_BORDER_COLLAPSE = CSS_STYLE_ID_OFFSET + 51;
    static final int CSS_EMPTY_CELLS = CSS_STYLE_ID_OFFSET + 52;
    static final int CSS_BORDER_SPACING = CSS_STYLE_ID_OFFSET + 53;
    static final int CSS_CAPTION_SIDE = CSS_STYLE_ID_OFFSET + 54;
    static final int CSS_WORD_SPACING = CSS_STYLE_ID_OFFSET + 55;
    static final int CSS_LINE_HEIGHT = CSS_STYLE_ID_OFFSET + 56;
    static final int CSS_MIN_WIDTH = CSS_STYLE_ID_OFFSET + 57;
    static final int CSS_MAX_WIDTH = CSS_STYLE_ID_OFFSET + 58;
    static final int CSS_MIN_HEIGHT = CSS_STYLE_ID_OFFSET + 59;
    static final int CSS_MAX_HEIGHT = CSS_STYLE_ID_OFFSET + 60;
    static final int CSS_QUOTES = CSS_STYLE_ID_OFFSET + 61;
    static final int CSS_OUTLINE_WIDTH = CSS_STYLE_ID_OFFSET + 62;
    static final int CSS_OUTLINE_STYLE = CSS_STYLE_ID_OFFSET + 63;
    static final int CSS_OUTLINE_COLOR = CSS_STYLE_ID_OFFSET + 64;
    static final int CSS_CONTENT = CSS_STYLE_ID_OFFSET + 65;
    static final int CSS_COUNTER_RESET = CSS_STYLE_ID_OFFSET + 66;
    static final int CSS_COUNTER_INCREMENT = CSS_STYLE_ID_OFFSET + 67;
    static final int CSS_DIRECTION = CSS_STYLE_ID_OFFSET + 68;


     private static int LAST_CSS_PROPERTY_INDEX = (HTMLComponent.PROCESS_HTML_MP1_ONLY?CSS_PAGEURL:CSS_DIRECTION)-CSS_STYLE_ID_OFFSET;

    /**
     * The types of the attribute
     */
    static final int[] CSS_ATTRIBUTE_TYPES = {
        TYPE_COLOR, // CSS_BACKGROUND_COLOR
        TYPE_CSS_URL, //CSS_BACKGROUND_IMAGE
        TYPE_NMTOKENS, //CSS_BACKGROUND_REPEAT
        TYPE_NMTOKENS, //CSS_BACKGROUND_ATTACHMENT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_BACKGROUND_POSITION_X
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_BACKGROUND_POSITION_Y
        TYPE_CSS_LENGTH, //CSS_BORDER_TOP_WIDTH
        TYPE_CSS_LENGTH, //CSS_BORDER_RIGHT_WIDTH
        TYPE_CSS_LENGTH, //CSS_BORDER_BOTTOM_WIDTH
        TYPE_CSS_LENGTH, //CSS_BORDER_LEFT_WIDTH
        TYPE_NMTOKENS, //CSS_BORDER_TOP_STYLE
        TYPE_NMTOKENS, //CSS_BORDER_RIGHT_STYLE
        TYPE_NMTOKENS, //CSS_BORDER_BOTTOM_STYLE
        TYPE_NMTOKENS, //CSS_BORDER_LEFT_STYLE
        TYPE_COLOR, //CSS_BORDER_TOP_COLOR
        TYPE_COLOR, // CSS_BORDER_RIGHT_COLOR
        TYPE_COLOR, // CSS_BORDER_BOTTOM_COLOR
        TYPE_COLOR, // CSS_BORDER_LEFT_COLOR
        TYPE_NMTOKENS, //CSS_CLEAR
        TYPE_COLOR, // CSS_COLOR
        TYPE_NMTOKENS, //CSS_VERTICAL_ALIGN
        TYPE_NMTOKENS, //CSS_DISPLAY
        TYPE_NMTOKENS, //CSS_FLOAT
        TYPE_NMTOKENS, //CSS_FONT_FAMILY
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_FONT_SIZE
        TYPE_NMTOKENS, //CSS_FONT_STYLE
        TYPE_NMTOKENS, //CSS_FONT_WEIGHT
        TYPE_NMTOKENS, //CSS_FONT_VARIANT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_HEIGHT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_WIDTH
        TYPE_NMTOKENS, // CSS_VISIBILITY
        TYPE_NMTOKENS, //CSS_WHITE_SPACE
        TYPE_CSS_URL, //CSS_LIST_STYLE_IMAGE
        TYPE_NMTOKENS, //CSS_LIST_STYLE_POSITION
        TYPE_NMTOKENS, //CSS_LIST_STYLE_TYPE
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_TOP
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_RIGHT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_BOTTOM
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_LEFT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_TOP
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_RIGHT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_BOTTOM
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_LEFT
        TYPE_ALIGN, //CSS_TEXT_ALIGN
        TYPE_NMTOKENS, //CSS_TEXT_DECORATION
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_TEXT_INDENT
        TYPE_NMTOKENS, //CSS_TEXT_TRANSFORM
        TYPE_NMTOKENS, // CSS_WAP_ACCESSKEY
        TYPE_NMTOKENS, // CSS_WAP_INPUT_FORMAT
        TYPE_BOOLEAN, // CSS_WAP_INPUT_REQUIRED
        TYPE_NMTOKENS, // CSS_PAGE_URL (internal usage)
        TYPE_NMTOKENS, // CSS_BORDER_COLLAPSE
        TYPE_NMTOKENS, // CSS_EMPTY_CELLS
        TYPE_NMTOKENS,// CSS_BORDER_SPACING
        TYPE_NMTOKENS,// CSS_CAPTION_SIDE
        TYPE_CSS_LENGTH_OR_PERCENTAGE, // CSS_WORD_SPACING
        TYPE_CSS_LENGTH_OR_PERCENTAGE_OR_MULTIPLIER, //CSS_LINE_HEIGHT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MIN_WIDTH
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MAX_WIDTH
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MIN_HEIGHT
        TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MAX_HEIGHT
        TYPE_NMTOKENS, //CSS_QUOTES
        TYPE_CSS_LENGTH, //CSS_OUTLINE_WIDTH
        TYPE_NMTOKENS, //CSS_OUTLINE_STYLE
        TYPE_COLOR, //CSS_OUTLINE_COLOR
        TYPE_NMTOKENS, //CSS_CONTENT
        TYPE_NMTOKENS, //CSS_COUNTER_RESET
        TYPE_NMTOKENS, //CSS_COUNTER_INCREMENT
        TYPE_NMTOKENS, //CSS_DIRECTION
     };

    /**
     * An array containing the allowed strings for CSS attributes.
     * Note that these strings are allowed in addition for the allowed values according to the attribute types.
     * Also, unlike the allowed string in Element that are matched per type, here in CSSElement each line matches an attribute (and not its type).
     * This is because of the great variance of allowed strings in CSS that are really per attribute and not per type.
     */
    private static final String[][] CSS_ALLOWED_STRINGS = {
        null, //TYPE_COLOR, // CSS_BACKGROUND_COLOR
        null, //TYPE_CSS_URL, //CSS_BACKGROUND_IMAGE
        BG_REPEAT_STRINGS, //TYPE_NMTOKENS, //CSS_BACKGROUND_REPEAT
        {"fixed","scroll"}, //TYPE_NMTOKENS, //CSS_BACKGROUND_ATTACHMENT
        {"left",CENTER_STR,"right"}, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_BACKGROUND_POSITION_X
        {"top",CENTER_STR,"bottom"}, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_BACKGROUND_POSITION_Y
        BORDER_WIDTH_STRINGS, //TYPE_CSS_LENGTH, //CSS_BORDER_TOP_WIDTH
        BORDER_WIDTH_STRINGS, //TYPE_CSS_LENGTH, //CSS_BORDER_RIGHT_WIDTH
        BORDER_WIDTH_STRINGS, //TYPE_CSS_LENGTH, //CSS_BORDER_BOTTOM_WIDTH
        BORDER_WIDTH_STRINGS, //TYPE_CSS_LENGTH, //CSS_BORDER_LEFT_WIDTH
        BORDER_STYLE_STRINGS, //TYPE_NMTOKENS, //CSS_BORDER_TOP_STYLE
        BORDER_STYLE_STRINGS, //TYPE_NMTOKENS, //CSS_BORDER_RIGHT_STYLE
        BORDER_STYLE_STRINGS, //TYPE_NMTOKENS, //CSS_BORDER_BOTTOM_STYLE
        BORDER_STYLE_STRINGS, //TYPE_NMTOKENS, //CSS_BORDER_LEFT_STYLE
        null, //TYPE_COLOR, //CSS_BORDER_TOP_COLOR
        null, //TYPE_COLOR, // CSS_BORDER_RIGHT_COLOR
        null, //TYPE_COLOR, // CSS_BORDER_BOTTOM_COLOR
        null, //TYPE_COLOR, // CSS_BORDER_LEFT_COLOR
        {"left","right",CSS_NONE,"both"}, //TYPE_NMTOKENS, //CSS_CLEAR
        null, //TYPE_COLOR, // CSS_COLOR
        VERTICAL_ALIGN_STRINGS, //TYPE_NMTOKENS, //CSS_VERTICAL_ALIGN
        {"inline","block","list-item",CSS_NONE,"-wap-marquee"}, //TYPE_NMTOKENS, //CSS_DISPLAY
        {"left","right",CSS_NONE}, //TYPE_NMTOKENS, //CSS_FLOAT
        null, //TYPE_NMTOKENS, //CSS_FONT_FAMILY
        FONT_SIZE_STRINGS, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_FONT_SIZE
        FONT_STYLE_STRINGS, //TYPE_NMTOKENS, //CSS_FONT_STYLE
        FONT_WEIGHT_STRINGS, //TYPE_NMTOKENS, //CSS_FONT_WEIGHT
        {"normal",SMALL_CAPS_STRING}, //TYPE_NMTOKENS, //CSS_FONT_VARIANT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_HEIGHT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_WIDTH
        {"hidden","visible","collapse"}, //TYPE_NMTOKENS, // CSS_VISIBILITY
        {"normal","pre","nowrap"}, //TYPE_NMTOKENS, //CSS_WHITE_SPACE
        null, //TYPE_CSS_URL, //CSS_LIST_STYLE_IMAGE
        {"inside","outside"}, //TYPE_NMTOKENS, //CSS_LIST_STYLE_POSITION
        {CSS_NONE,"disc","circle","square","decimal","upper-alpha","lower-alpha","upper-roman","lower-roman"}, //TYPE_NMTOKENS, //CSS_LIST_STYLE_TYPE
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_TOP
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_RIGHT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_BOTTOM
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_LEFT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_TOP
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_RIGHT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_BOTTOM
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_LEFT
        TEXT_ALIGN_STRINGS, //TYPE_ALIGN, //CSS_TEXT_ALIGN
        {"underline","line-through",CSS_NONE,"overline"}, //TYPE_NMTOKENS, //CSS_TEXT_DECORATION // 'blink' is not supported, as it is not supported in any major browser
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_TEXT_INDENT
        {CSS_NONE,"uppercase","lowercase","capitalize"}, //TYPE_NMTOKENS, //CSS_TEXT_TRANSFORM
        null, //TYPE_NMTOKENS, // CSS_WAP_ACCESSKEY
        null, //TYPE_NMTOKENS, // CSS_WAP_INPUT_FORMAT
        {"true","false"}, //TYPE_BOOLEAN, // CSS_WAP_INPUT_REQUIRED
        null, //TYPE_NMTOKENS, // baseurl (internal usage)
        {"collapse","separate"}, // CSS_BORDER_COLLAPSE
        {"hide","show"}, // CSS_EMPTY_CELLS
        null,// CSS_BORDER_SPACING
        {"bottom","top"},// CSS_CAPTION_SIDE
        null, // CSS_WORD_SPACING
        {"normal"}, //CSS_LINE_HEIGHT
        null, //CSS_MIN_WIDTH
        null, //CSS_MAX_WIDTH
        null, //CSS_MIN_HEIGHT
        null, //CSS_MAX_HEIGHT
        null, //CSS_QUOTES
        BORDER_WIDTH_STRINGS, //CSS_OUTLINE_WIDTH
        BORDER_STYLE_STRINGS, //CSS_OUTLINE_STYLE
        null, //CSS_OUTLINE_COLOR
        null, //CSS_CONTENT
        null, //CSS_COUNTER_RESET
        null, //CSS_COUNTER_INCREMENT
        {"rtl","ltr"},   //CSS_DIRECTION
    };


    /**
     * This very high integer value is added to a numeric value stored in attrVals to denote that it is a percentage value
     */
    static final int VAL_PERCENTAGE = 1<<20;

    /**
     * This very high integer value is added to a numeric value stored in attrVals to denote that it is a value in the CSS 'ex' unit (EX means half of the font size)
     */
    static final int VAL_EX = 1<<21;

   /**
    * Values associated with BG_POS_STRINGS
    */
    private final static int[] BG_POS_PERCENTAGE = {0+VAL_PERCENTAGE,50+VAL_PERCENTAGE,100+VAL_PERCENTAGE};

    /**
     * The values of each of the allowed strings.
     * When adding an attribute which its value is an allowed string, the attribute value is set according to this array.
     * For attributes that have no allowed strings (i.e. accpeting numeric values solely) this is not reelvant.
     * For attributes that do have allowed strings and 'null' as the values, it means that the first string in the allowed list will be translated to 0, the second to 1 etc.
     * The purpose of converting strings in strings that have non-null values, is to convert them to a value representing the essence of the string (FOr example thin in border is 1)
     * And for those with null values, is to avoid having to parse strings all the time (so they are conerted to 0,1...,n)
     */
    private static final int[][] CSS_ALLOWED_STRINGS_VALS = {
        null, //TYPE_COLOR, // CSS_BACKGROUND_COLOR
        null, //TYPE_CSS_URL, //CSS_BACKGROUND_IMAGE
        BG_REPEAT_VALS, //TYPE_NMTOKENS, //CSS_BACKGROUND_REPEAT
        null, //{"fixed","scroll"}, //TYPE_NMTOKENS, //CSS_BACKGROUND_ATTACHMENT
        BG_POS_PERCENTAGE, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_BACKGROUND_POSITION_X
        BG_POS_PERCENTAGE, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_BACKGROUND_POSITION_Y
        BORDER_WIDTH_VALS, //TYPE_CSS_LENGTH, //CSS_BORDER_TOP_WIDTH
        BORDER_WIDTH_VALS, //TYPE_CSS_LENGTH, //CSS_BORDER_RIGHT_WIDTH
        BORDER_WIDTH_VALS, //TYPE_CSS_LENGTH, //CSS_BORDER_BOTTOM_WIDTH
        BORDER_WIDTH_VALS, //TYPE_CSS_LENGTH, //CSS_BORDER_LEFT_WIDTH
        null, //BORDER_STYLE_VALS, //TYPE_NMTOKENS, //CSS_BORDER_TOP_STYLE
        null, //BORDER_STYLE_VALS, //TYPE_NMTOKENS, //CSS_BORDER_RIGHT_STYLE
        null, //BORDER_STYLE_VALS, //TYPE_NMTOKENS, //CSS_BORDER_BOTTOM_STYLE
        null, //BORDER_STYLE_VALS, //TYPE_NMTOKENS, //CSS_BORDER_LEFT_STYLE
        null, //TYPE_COLOR, //CSS_BORDER_TOP_COLOR
        null, //TYPE_COLOR, // CSS_BORDER_RIGHT_COLOR
        null, //TYPE_COLOR, // CSS_BORDER_BOTTOM_COLOR
        null, //TYPE_COLOR, // CSS_BORDER_LEFT_COLOR
        null, //CLEAR_VALS, //TYPE_NMTOKENS, //CSS_CLEAR
        null, //TYPE_COLOR, // CSS_COLOR
        VERTICAL_ALIGN_VALS, //TYPE_NMTOKENS, //CSS_VERTICAL_ALIGN
        null, //DISPLAY_VALS, //TYPE_NMTOKENS, //CSS_DISPLAY
        null, //FLOAT_VALS, //TYPE_NMTOKENS, //CSS_FLOAT
        null, //TYPE_NMTOKENS, //CSS_FONT_FAMILY
        FONT_SIZE_VALS, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_FONT_SIZE
        FONT_STYLE_VALS, //TYPE_NMTOKENS, //CSS_FONT_STYLE
        FONT_WEIGHT_VALS, //TYPE_NMTOKENS, //CSS_FONT_WEIGHT
        null, //FONT_VARIANT_VALS, //TYPE_NMTOKENS, //CSS_FONT_VARIANT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_HEIGHT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_WIDTH
        null, //VISIBILITY_VALS, //TYPE_NMTOKENS, // CSS_VISIBILITY
        null, //WHITE_SPACE_VALS, //TYPE_NMTOKENS, //CSS_WHITE_SPACE
        null, //TYPE_CSS_URL, //CSS_LIST_STYLE_IMAGE
        null, //LIST_STYLE_POSITION_VALS, //TYPE_NMTOKENS, //CSS_LIST_STYLE_POSITION
        null, //LIST_STYLE_TYPE_VALS, //TYPE_NMTOKENS, //CSS_LIST_STYLE_TYPE
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_TOP
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_RIGHT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_BOTTOM
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_MARGIN_LEFT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_TOP
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_RIGHT
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_BOTTOM
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_PADDING_LEFT
        TEXT_ALIGN_VALS, //TYPE_ALIGN, //CSS_TEXT_ALIGN
        null, //{CSS_NONE}, //TYPE_NMTOKENS, //CSS_TEXT_DECORATION
        null, //TYPE_CSS_LENGTH_OR_PERCENTAGE, //CSS_TEXT_INDENT
        null, //TEXT_TRANSFORM_VALS, //TYPE_NMTOKENS, //CSS_TEXT_TRANSFORM
        null, //TYPE_NMTOKENS, // CSS_WAP_ACCESSKEY
        null, //TYPE_NMTOKENS, // CSS_WAP_INPUT_FORMAT
        null, //TYPE_BOOLEAN, // CSS_WAP_INPUT_REQUIRED
        null, //TYPE_NMTOKENS, // baseurl (internal usage)
        null, // CSS_BORDER_COLLAPSE
        null, // CSS_EMPTY_CELLS
        null,// CSS_BORDER_SPACING
        null,// CSS_CAPTION_SIDE
        null, // CSS_WORD_SPACING
        {1}, //CSS_LINE_HEIGHT //normal means 100% of the font height (or 1 * font height)
        null, //CSS_MIN_WIDTH
        null, //CSS_MAX_WIDTH
        null, //CSS_MIN_HEIGHT
        null, //CSS_MAX_HEIGHT
        null, //CSS_QUOTES
        BORDER_WIDTH_VALS, //CSS_OUTLINE_WIDTH
        null, //CSS_OUTLINE_STYLE
        null, //CSS_OUTLINE_COLOR
        null, //CSS_CONTENT
        null, //CSS_COUNTER_RESET
        null, //CSS_COUNTER_INCREMENT
        null,   //CSS_DIRECTION
    };

    /**
     * The attrVals array holds the numeric values of most of the attributes of this element.
     * This is used for quick access, instead of parsing strings in the attributes Hashtable
     * Values are set during addAttribute.
     */
    int[] attrVals = new int[LAST_CSS_PROPERTY_INDEX+1];

    /**
     * A string array with the attributes supported by the WCSS spec
     * Note that shorthand attributes appear in a different array
     */
    static final String[] CSS_ATTRIBUTE_ROOTS = {
        "background-color", "background-image", "background-repeat", "background-attachment",
        "background-position-x", "background-position-y",
        "border-top-width", "border-left-width", "border-bottom-width", "border-right-width",
        "border-top-style", "border-left-style", "border-bottom-style", "border-right-style",
        "border-top-color", "border-left-color", "border-bottom-color", "border-right-color",
        "clear", "color", "vertical-align", "display", "float",
        "font-family", "font-size", "font-style", "font-weight", "font-variant",
        "height", "width", "visibility", "white-space",
        "list-style-image", "list-style-position", "list-style-type",
        "margin-top", "margin-left", "margin-bottom", "margin-right",
        "padding-top", "padding-left", "padding-bottom", "padding-right",
        "text-align", "text-decoration", "text-indent", "text-transform", 
        "-wap-access-key","-wap-input-format","-wap-input-required",
        "pageurl", // an attribute used for internal CodenameOne uses
        "border-collapse", "empty-cells", "border-spacing", "caption-side", "word-spacing", // css2
        "line-height","min-width","max-width","min-height","max-height","quotes", //css2
        "outline-width","outline-style","outline-color", // css2
        "content","counter-reset","counter-increment", //css2
        "direction" //css2

    };

    /**
     * A string array containing all supported shorthand attributes
     * Shorthand attrributes are mapped to multiple CSS base attributes are used to define several CSS attributes at once
     */
    static final String[] CSS_SHORTHAND_ATTRIBUTE_LIST = {
        "background",
        "background-position",
        "border-width",
        "border-style",
        "border-color",
        "border-top",
        "border-right",
        "border-bottom",
        "border-left",
        "border",
        "font",
        "margin",
        "padding",
        "list-style",
        "outline"
    };

    /**
     * A boolean array defining which of the CSS shorthand attributes when defined with less than the expected number of values copy the values defined to the remaining undefined attributes
     * For example, margin expects 4 values (top/right/bottom/left), but can also be defined with 1 value that will be used for all 4, or with 2 values which will be used one for the vertical margins and the other for the horizontal etc.
     */
    static final boolean[] CSS_IS_SHORTHAND_ATTRIBUTE_COLLATABLE = new boolean[]{
        false, // "background",
        false, // "background-position",
        true,  // "border-width",
        true,  // "border-style",
        true,  // "border-color",
        false, // "border-top",
        false, // "border-right",
        false, // "border-bottom",
        false, // "border-left",
        false, // "border",
        false, // "font",
        true,  // "margin",
        true,  // "padding",
        false,  // "list-style"
        false   // "outline"
    };

    /**
     * A constant defining the offset in which the base CSS attribute relating to the TOP appears in the shorthand attribute index
     */
    private static final int CSS_TOP=0;

    /**
     * A constant defining the offset in which the base CSS attribute relating to the RIGHT appears in the shorthand attribute index
     */
    private static final int CSS_RIGHT=1;

    /**
     * A constant defining the offset in which the base CSS attribute relating to the BOTTOM appears in the shorthand attribute index
     */
    private static final int CSS_BOTTOM=2;

    /**
     * A constant defining the offset in which the base CSS attribute relating to the LEFT appears in the shorthand attribute index
     */
    private static final int CSS_LEFT=3;

    /**
     * A map defining the rules by which values of collatable shorthand attributes are assigned to the base attributes
     * This map is according to the CSS specs
     */
    static final int[][][] CSS_COLLATABLE_ORDER = {
        { // When one value is specified it is set to all sides
            {CSS_TOP,CSS_RIGHT,CSS_BOTTOM,CSS_LEFT}
        },
        { // When two value are specified:
            {CSS_TOP,CSS_BOTTOM}, // The first value is set to the top and bottom
            {CSS_LEFT,CSS_RIGHT}  // The second value is set to the left and right
        },
        { // When three values are specified:
            {CSS_TOP}, // The first is set to the top
            {CSS_LEFT,CSS_RIGHT}, // The second is set to the left and right
            {CSS_BOTTOM} // THe third is set to the bottom
        },
        { // When four values are specified - each is set to the appropriate side
            {CSS_TOP},
            {CSS_RIGHT},
            {CSS_BOTTOM},
            {CSS_LEFT}
        }
    };

    // The following constants are used in CSS_SHORTHAND_ATTRIBUTE_INDEX when a shorthand attribute translates to a second level of shorthand attributes
    // For example 'border' which translated to width/style/color. The values of the constants denote their location in the CSS_SHORTHAND_ATTRIBUTE_INDEX map
    private static final int CSS_SHORTHAND_BACKGROUND_POSITION = 1;
    private static final int CSS_SHORTHAND_BORDER_WIDTH = 2;
    private static final int CSS_SHORTHAND_BORDER_STYLE = 3;
    private static final int CSS_SHORTHAND_BORDER_COLOR = 4;

    /**
     * A map of CSS shorthand attributes to their corresponding base attributes (or to other shorthand attributes)
     */
    static final int[][] CSS_SHORTHAND_ATTRIBUTE_INDEX = {
        {CSS_BACKGROUND_COLOR,CSS_BACKGROUND_IMAGE,CSS_BACKGROUND_REPEAT, CSS_BACKGROUND_ATTACHMENT, CSS_SHORTHAND_BACKGROUND_POSITION},
        { CSS_BACKGROUND_POSITION_X, CSS_BACKGROUND_POSITION_Y},
        {CSS_BORDER_TOP_WIDTH,CSS_BORDER_RIGHT_WIDTH,CSS_BORDER_BOTTOM_WIDTH,CSS_BORDER_LEFT_WIDTH},
        {CSS_BORDER_TOP_STYLE,CSS_BORDER_RIGHT_STYLE,CSS_BORDER_BOTTOM_STYLE,CSS_BORDER_LEFT_STYLE},
        {CSS_BORDER_TOP_COLOR,CSS_BORDER_RIGHT_COLOR,CSS_BORDER_BOTTOM_COLOR,CSS_BORDER_LEFT_COLOR},
        {CSS_BORDER_TOP_WIDTH,CSS_BORDER_TOP_STYLE,CSS_BORDER_TOP_COLOR},
        {CSS_BORDER_RIGHT_WIDTH,CSS_BORDER_RIGHT_STYLE,CSS_BORDER_RIGHT_COLOR},
        {CSS_BORDER_BOTTOM_WIDTH,CSS_BORDER_BOTTOM_STYLE,CSS_BORDER_BOTTOM_COLOR},
        {CSS_BORDER_LEFT_WIDTH,CSS_BORDER_LEFT_STYLE,CSS_BORDER_LEFT_COLOR},
        {CSS_SHORTHAND_BORDER_WIDTH,CSS_SHORTHAND_BORDER_STYLE,CSS_SHORTHAND_BORDER_COLOR},
        {CSS_FONT_STYLE,CSS_FONT_VARIANT,CSS_FONT_WEIGHT,CSS_FONT_SIZE,CSS_FONT_FAMILY},
        {CSS_MARGIN_TOP,CSS_MARGIN_RIGHT,CSS_MARGIN_BOTTOM,CSS_MARGIN_LEFT},
        {CSS_PADDING_TOP,CSS_PADDING_RIGHT,CSS_PADDING_BOTTOM,CSS_PADDING_LEFT},
        {CSS_LIST_STYLE_TYPE,CSS_LIST_STYLE_POSITION,CSS_LIST_STYLE_IMAGE},
        {CSS_OUTLINE_WIDTH,CSS_OUTLINE_STYLE,CSS_OUTLINE_COLOR}
    };

    private int selectorSpecificity = -1; // A value used to determine how specific this selector is - the more the selector is specific (i.e. id > class > tag) the more it overrides other less specific selectors
    private String selectorId=null; // The selector's ID (if it's an ID selector, i.e. '#someid')
    private String selectorClass=null; // The selector's class (if it's a class selector i.e. '.someclass')
    private String selectorTag=null; // The selector's tag (if it's a tag selector - i.e. 'div')

    private int selectorPseudoClass=0;

    /**
     * This is true if this selector is a descendant selector - and false if it is a child selector.
     * For example is this selector is h2, and was derived from a 'h1 h2' combo it will be true - but if derived from 'h1 > h2' will be false.
     * Note - for the first selector in the combo (h1 in the above example) this value is not significant and never read.
     */
    boolean descendantSelector;

    /**
     * This is true if this selector is a sibling (adjacent) selector - and false otherwise
     */
    boolean siblingSelector;

    /**
     * A vector holding any attribute selections (For example p[att=value])
     * This will be null in non-attribute selectors.
     */
    Vector attributeSelections;

    /**
     * A constant representing the focus pseudo-class
     */
    static final int PC_FOCUS = 1;

    /**
     * A constant representing the active pseudo-class
     */
    static final int PC_ACTIVE = 2;

    /**
     * A constant representing the link pseudo-class
     */
    static final int PC_LINK = 4;

    /**
     * A constant representing the visited pseudo-class
     */
    static final int PC_VISITED = 8;

    /**
     * A constant representing the 'before' pseudo-class
     */
    static final int PC_BEFORE = 16;

    /**
     * A constant representing the 'after' pseudo-class
     */
    static final int PC_AFTER = 32;

    /**
     * A constant representing the 'first-child' pseudo-class
     */
    static final int PC_FIRST_CHILD = 64;
    

    /**
     * The list of strings representing the various CSS pseudo-classes
     */
    final static String[] PSEUDO_CLASSES_STRINGS = {"hover","focus","active","link","visited","before","after","first-child"};

    /**
     * The values per each string in PSEUDO_CLASSES_STRINGS
     * Note that 'hover' is mapped to PC_FOCUS since we don't support hover on mobile.
     */
    final static int[] PSEUDO_CLASSES_VALS = {PC_FOCUS,PC_FOCUS,PC_ACTIVE,PC_LINK,PC_VISITED,PC_BEFORE,PC_AFTER,PC_FIRST_CHILD}; // Since we don't have hover in most/all devices we convert hover to focus as well

    /**
     * Convenience method with defaultSuffix == LENGTH_SUFFIX_PX
     *
     * @param units The string with CSS units (can be either percentage or px/em/ex and other sufixes from CSS_LENGTH_SUFFIX)
     * @return The value in pixels/font-multipliers/percentage
     */
    static int convertUnitsOrPercentage(String units) {
        return convertUnitsOrPercentage(units, LENGTH_SUFFIX_PX);
    }

    /**
     * Converts the given CSS length/percentage string to pixels
     *
     * @param units The string with CSS units (can be either percentage or px/em/ex and other sufixes from CSS_LENGTH_SUFFIX)
     * @param defaultSuffix The suffix to use as default if none is specified
     *
     * @return The value in pixels/font-multipliers/percentage
     */
    static int convertUnitsOrPercentage(String units,int defaultSuffix) {
        if (units==null) {
            return -1;
        }

        boolean percentage=false;
        if (units.charAt(units.length()-1)=='%') {
            percentage=true;
            defaultSuffix=LENGTH_SUFFIX_PX; // for percentage, the number without the % is sent and needs to stay as is (factor=1)
            units=units.substring(0, units.length()-1);
        }

        int val=convertUnits(units,defaultSuffix);
        if (percentage) {
            val+=VAL_PERCENTAGE;
        }
        return val;
    }


    /**
     * Converts the given CSS length string to pixels
     *
     * @param units The string with CSS units (can be px/em/ex and other  sufixes from CSS_LENGTH_SUFFIX)
     * @param defaultSuffix The suffix to use as default if none is specified
     *
     * @return the CSS length in pixels
     */
    static int convertUnits(String units,int defaultSuffix) {
        if (units==null) {
            return -1;
        }
        int factor=1;

        int i=0;
        boolean suffixFound=false;
        for(;i<CSSElement.CSS_LENGTH_SUFFIX.length;i++) {
            if (units.endsWith(CSSElement.CSS_LENGTH_SUFFIX[i])) {
                factor=CSS_LENGTH_FACTORS[i];
                units=units.substring(0, units.length()-2);
                suffixFound=true;
                break;
            }
        }

        if (!suffixFound) {
            i=defaultSuffix;
            factor=CSS_LENGTH_FACTORS[i];
        }


        try {
            int result=(int)(Float.parseFloat(units)*factor);
            if ((i==LENGTH_SUFFIX_EM) || (i==LENGTH_SUFFIX_EX)) { // em & ex
                result+=VAL_EX;
            }

            return result;
        } catch (NumberFormatException nfe) {
            return -1;
        }

    }


    /**
     * Adds the given attribute selection
     * 
     * @param exp The expression inside the square brackets (for example p[title=test][class] will be parsed in the construvtor and be sent here twice as 'title=test' and then 'class')
     */
    void addAttributeSelection(String exp) {
        int index=exp.indexOf('=');
        String value=null;
        int constraint=AttString.EQUALS;
        if (index!=-1) {
            if (index==0) { // something lie [=] which is illegal
                setTagId(TAG_CSS_ILLEGAL_SELECTOR);
                return;
            }
            int opIndex=index;
            char c=exp.charAt(index-1);
            if (c=='~') {
                constraint=AttString.CONTAINS_WORD;
                opIndex--;
            } else if (c=='|') {
                constraint=AttString.BEGINS_WITH;
                opIndex--;
            }
            value=exp.substring(index+1);
            if (((value.startsWith("\"")) && (value.endsWith("\""))) ||
                ((value.startsWith("'")) && (value.endsWith("'")))) {
                    value=value.substring(1, value.length()-1);
            }

            exp=exp.substring(0, opIndex);
        }
        if (attributeSelections==null) {
            attributeSelections=new Vector();
        }
        attributeSelections.addElement(new AttString(exp,constraint,value));
    }


    /**
     * Returns the language code of the specified element (or one of its ancestors)
     * This implementation is optimized for minimal storage but not for performance - each element checks its ancestors for any lang definition
     * 
     * @param element The element to check
     * @return The language code of the element, or null if none specified
     */
    private String getLang(HTMLElement element) {
        String lang=element.getAttributeById(HTMLElement.ATTR_LANG);
        if (lang!=null) {
            return lang;
        } else {
            HTMLElement parent=(HTMLElement)element.getParent();
            if (parent!=null) {
                return getLang(parent);
            } else {
                return null;
            }

        }
    }

    /**
     * Matches the attribute selections of this selector (if any) to the provided element
     * 
     * @param element The element to match to
     * @return true for a match, false otherwise
     */
    boolean matchAttributeSelections(HTMLElement element) {
        if (getTagId()==TAG_CSS_ILLEGAL_SELECTOR) {
            return false;
        }
        if (attributeSelections==null) {
            return true;
        }
        for(Enumeration e=attributeSelections.elements();e.hasMoreElements();) {
            AttString attStr = (AttString)e.nextElement();
            if (attStr.constraint==AttString.LANG) {
                String lang=attStr.value;
                String elemLang=getLang(element);
                if ((elemLang==null) || ((!lang.equals(elemLang)) && (!elemLang.startsWith(lang+"-")))) {
                    return false;
                }
            } else {
                String elementVal=element.getAttribute(attStr.attribute);
                if (elementVal==null) {
                    return false;
                }
                if (attStr.value!=null) { //null means if defined it suffices
                    switch(attStr.constraint) {
                        case AttString.EQUALS:
                            if (!elementVal.equals(attStr.value)) {
                                return false;
                            }
                            break;
                        case AttString.BEGINS_WITH:
                            if ((!elementVal.equals(attStr.value)) && (!elementVal.startsWith(attStr.value+"-"))) {
                                return false;
                            }
                            break;
                        case AttString.CONTAINS_WORD:
                            String str=" "+elementVal+" "; //adding leading and trailing space
                            if (str.indexOf(" "+attStr.value+" ")==-1) { // adding spaces to check for the word and not subword (when we search 'val' we don't want to find 'interval')
                                return false;
                            }
                            break;
                    }
                }
            }

        }
        return true;
    }

    /**
     * Constructs a CSSElement, This basically sets the name and ID and resets the attrVals array.
     * 
     * @param name
     */
    CSSElement(String name) {
        setTagId(TAG_CSS_SELECTOR);
        int index=name.indexOf('[');
        String tagName=name;
        if (index!=-1) {
            tagName=name.substring(0, index);
            if (HTMLComponent.PROCESS_HTML_MP1_ONLY) { // HTML-MP1 does not supports attribute selectors, so this selector will be ignored
                setTagId(TAG_CSS_ILLEGAL_SELECTOR);
            } else {
                while (index!=-1) {
                    int endIndex=name.indexOf(']');
                    if ((endIndex!=-1) && (endIndex>index+1)) { // non empty (i.e. not [])
                        String str=name.substring(index+1,endIndex);
                        addAttributeSelection(str);
                    } else {
                        setTagId(TAG_CSS_ILLEGAL_SELECTOR);
                        break;
                    }
                    name=name.substring(endIndex+1);
                    index=name.indexOf('[');
                }
            }
        }

        setTagName(tagName);
        for(int i=0;i<=LAST_CSS_PROPERTY_INDEX;i++) {
            attrVals[i]=-1;
        }
    }

    /**
     * Overrides Element.getName to return the name string of this CSSElement.
     * Unlike Element which discards the name string and uses the id to identify the name, the CSSElement retains the name and simply returns it here.
     * This is because a CSSElement name is in fact the selector string which is any combination of tags, classes and IDs and as such cannot be converted to a simple int.
     * 
     * @return the selector's name
     *
    public String getName() {
        return name;
    }*/

    /**
     * Checks if the specified attribute is assigned (i.e. was set with a legal value)
     * 
     * @param attrId The attribute ID (Should be one of the CSS attributes, i.e. >= CSS_STYLE_ID_OFFSET)
     * @return true if this attribute is assigned, false otherwise
     */
    boolean isAttributeAssigned(int attrId) {
        return ((attrVals[attrId-CSS_STYLE_ID_OFFSET]!=-1) || ((getAttributes()!=null) && (getAttributes().get(new Integer(attrId))!=null)));
    }

    /**
     * Returns the CSSElement's child positioned at the specified index.
     * This is a convenience method that is very similar to Element.getChildAt, but returns an object of the CSSElement class.
     * Since all of the children of a CSSElement are CSSElements as well, this prevents redundant casting.
     * 
     * @param index The requested child position
     * @return The child at the requested position 
     * @throws ArrayIndexOutOfBoundsException if the index is bigger than the children's count or smaller than 0
     */
    CSSElement getCSSChildAt(int index) {
        Vector children=getChildren();
        if ((index<0) || (children==null) || (index>=children.size())) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (CSSElement)children.elementAt(index);
    }


    /**
     * Adds the specified attribute and value to this CSSElement if it is supported and has a valid value.
     * This method overrides Element.addAttribute to provide with specific CSSElement functionality.
     * Unlike Element which retains all the value strings in the attributes hashtable, In CSSElement the value is immediately converted to a numeric value if possible and placed in the attrVals array.
     * The string is retained only for values that can't be converted to int (such as URLs).
     *
     * @param attribute The attribute's name
     * @param value The attribute's value
     *
     * @return a positive error code or -1 if attribute is supported and valid
     */
    public int setAttribute(String attribute,String value) {
        int attrId=-1;
        int i=0;
        //while ((attrId==-1) && (i<CSS_ATTRIBUTE_ROOTS.length)) {
        while ((attrId==-1) && (i<=LAST_CSS_PROPERTY_INDEX)) {
            if(CSS_ATTRIBUTE_ROOTS[i].equals(attribute)) {
                attrId=CSS_STYLE_ID_OFFSET + i;
                break;
            } else {
                i++;
            }
        }

        if (attrId==-1) {
            return HTMLCallback.ERROR_CSS_ATTRIBUTE_NOT_SUPPORTED;
        } else {
            return addAttribute(attrId, value);

        }
    }

    /**
     * Adds the specified attribute and value to this CSSElement if it is supported and has a valid value.
     * This method is used by addAttribute(String,String) and also when we already know the attribute's id (Such as in shorthand attributes)
     *
     * @param attrId The attribute's id
     * @param value The attribute's value
     *
     * @return a positive error code or -1 if attribute is supported and valid
     */
    int addAttribute(int attrId,String value) {
         
        if (value==null) {
            return HTMLCallback.ERROR_ATTIBUTE_VALUE_INVALID;
        }
        int i=attrId-CSS_STYLE_ID_OFFSET;

        boolean knownType=true;
        int val=-1;

        switch(CSS_ATTRIBUTE_TYPES[i]) {
            case TYPE_COLOR:
                val=getColor(value, -1);
                break;
            case TYPE_CSS_LENGTH:
                val=convertUnits(value,LENGTH_SUFFIX_PX);
                break;
            case TYPE_CSS_LENGTH_OR_PERCENTAGE:
                val=convertUnitsOrPercentage(value,LENGTH_SUFFIX_PX);
                break;
            case TYPE_CSS_LENGTH_OR_PERCENTAGE_OR_MULTIPLIER:
                val=convertUnitsOrPercentage(value,LENGTH_SUFFIX_EM);
                break;
            default:
                knownType=false;
        }
        if (val==-1) { // Some attributes can be either a number, or a string. For example border-width may be a number/percentage but can also be "thin"/"medium"/"thick" which is translated by the strings
            if (CSS_ALLOWED_STRINGS[i]!=null) {
                val=HTMLUtils.getStringVal(value, CSS_ALLOWED_STRINGS[i], CSS_ALLOWED_STRINGS_VALS[i]);
                if (val==-1) {
                   return HTMLCallback.ERROR_ATTIBUTE_VALUE_INVALID;
                } else {
                    attrVals[i]=val;
                    fixBackgroundPositionDefaults(attrId);
                }
            } else {
                if (knownType) {
                               return HTMLCallback.ERROR_ATTIBUTE_VALUE_INVALID;
                } else {
                    setAttribute(new Integer(attrId), value);
                }
            }
        } else {
            attrVals[i]=val;
            fixBackgroundPositionDefaults(attrId);
        }

        return -1; // No error code - attribute addition succeeded
    }

    /**
     *  Checks if the attribute is one of CSS_BACKGROUND_POSITION_X or CSS_BACKGROUND_POSITION_Y and if so fixes the default of the other attribute accordingly.
     *  Background position is a very special case, since when it is not specified the default is TOP, LEFT, but when one of the background positions is specified, the other's default turns to CENTER
     * 
     * @param attrId The attributeID
     */
    private void fixBackgroundPositionDefaults(int attrId) {
                if ((attrId==CSS_BACKGROUND_POSITION_X) && (!isAttributeAssigned(CSS_BACKGROUND_POSITION_Y))) {
                    addAttribute(CSS_BACKGROUND_POSITION_Y, CENTER_STR);
                } else if ((attrId==CSS_BACKGROUND_POSITION_Y) && (!isAttributeAssigned(CSS_BACKGROUND_POSITION_X))) {
                    addAttribute(CSS_BACKGROUND_POSITION_X, CENTER_STR);
                }
    }


    /**
     * {@inheritDoc}
     */
    public String getAttributeName(Integer attrKey) {
        return CSS_ATTRIBUTE_ROOTS[attrKey.intValue()-CSS_STYLE_ID_OFFSET];
    }

    /**
     * Returns this selector's specificity. A specificity of a selector determines the order in which it should be applied.
     * The bigger the specificty, the later the selector will be applied (Which means it will overdride previously applied selectors)
     * This lazily invokes calcSelectorSpecificity if it wasn't invoked before.
     * 
     * @return this selector's specificity.
     */
    int getSelectorSpecificity() {
        if (selectorSpecificity==-1) {
            selectorSpecificity=calcSelectorSpecificity();
        }
        return selectorSpecificity;

    }

    /**
     * Returns the value of the requested attribute
     *
     * @param attrId The attribute's id
     * @return the value of the requested attribute
     */
    int getAttrVal(int attrId) {
        return attrVals[attrId-CSS_STYLE_ID_OFFSET];
    }

    /**
     * Returns the length value of the requested attribute.
     * A CSS length value can be denoted in various units, or percentages. This method calculates the final value in pixels.
     *
     * @param attrId The attribute's id
     * @param cmp The component relevant to the length calculation (Used in case of length values based on font size)
     * @param origDimension The original dimension to take into account if the value is specified as a percentage value
     * @return the calculated value
     */
    int getAttrLengthVal(int attrId,Component cmp,int origDimension) {
        int val=getAttrVal(attrId);
        return convertLengthVal(val, cmp, origDimension);
    }

    static int convertLengthVal(int val,Component cmp,int origDimension) {
        if (val>=0) { // !=-1 is not enough, since some values as FONT_SMALLER/LARGER are negative and then & VAL_* may be true...
            if ((val & VAL_PERCENTAGE)!=0) {
                val-=VAL_PERCENTAGE;
                val=val*origDimension/100;
            } else if ((val & VAL_EX)!=0) { // 'ex' means half of the font size
                val-=VAL_EX;
                val=val*cmp.getStyle().getFont().getHeight()/2;
            }
        }
        return val;
    }

    /**
     * Calculates this selector's specificity
     * 
     * @return this selector's specificity
     */
    int calcSelectorSpecificity() {
        int spec=0;
        if (attributeSelections!=null) {
            spec+=attributeSelections.size(); // attribute selectors get extra specificity points, same as pseudo classes
        }

        String nameStr=getTagName();
        if (nameStr.startsWith("*")) {
            nameStr=nameStr.substring(1); //ignore universal selector
        }

        int index=nameStr.lastIndexOf(':');
        while (index!=-1) {
            String property=nameStr.substring(index+1);
            nameStr=nameStr.substring(0, index);
            int propNum=HTMLUtils.getStringVal(property, PSEUDO_CLASSES_STRINGS, PSEUDO_CLASSES_VALS);
            if (propNum!=-1) {
                selectorPseudoClass+=propNum;
                spec++; // Psuedo-classes get extra specificity points
            } else if ((!HTMLComponent.PROCESS_HTML_MP1_ONLY) && (property.startsWith("lang("))) { // Though :lang is a pseudo-class, we implement it internally as an attribute selection, since it needs to hold a string value (the langauge string)
                String lang=property.substring(5,property.length()-1);
                if (attributeSelections==null) {
                    attributeSelections=new Vector();
                }
                attributeSelections.addElement(new AttString(null,AttString.LANG, lang));
                spec++;

            }
            index=nameStr.lastIndexOf(':');
        }
        if (((selectorPseudoClass & (PC_FOCUS|PC_ACTIVE|PC_LINK|PC_VISITED))!=0)
            && (nameStr.length()==0)) { // :link is the same as a:link
            nameStr="a";
        }
        //name=nameStr; // The tag name should not contain the pseudo classes
        setTagName(nameStr); // The tag name should not contain the pseudo classes

        index=nameStr.indexOf('#');
        if (index!=-1) {
            spec+=100;
            selectorId=nameStr.substring(index+1);
            if (index!=0) {
                spec+=1;
                selectorTag=nameStr.substring(0, index);
            }
        } else {
            index=nameStr.indexOf('.');
            if (index!=-1) {
                spec+=10;
                selectorClass=nameStr.substring(index+1);
                //selectorClass=selectorClass.replace('.', ' ');

                if (index!=0) {
                    spec+=1;
                    selectorTag=nameStr.substring(0, index);
                }
            } else {
                if (nameStr.length()>0) {
                    spec+=1;
                    selectorTag=nameStr;
                }
            }
        }

        for(int i=0;i<getNumChildren();i++) { // There should be usually just one child
            spec+=(getCSSChildAt(i)).getSelectorSpecificity();
        }

        return spec;
    }

    /**
     * Returns this selector's id, or null if none
     * This method assumes that calcSelectorSpecificity was invoked before.
     *
     * @return this selector's id, or null if none
     */
    String getSelectorId() {
        return selectorId;
    }

    /**
     * Returns this selector's class, or null if none
     * This method assumes that calcSelectorSpecificity was invoked before.
     *
     * @return this selector's class, or null if none
     */
    String getSelectorClass() {
        return selectorClass;
    }

    /**
     * Returns this selector's tag, or null if none
     * This method assumes that calcSelectorSpecificity was invoked before.
     * 
     * @return this selector's tag, or null if none
     */
    String getSelectorTag() {
        return selectorTag;
    }

    /**
     * This method assumes that calcSelectorSpecificity was invoked before.
     *
     * @return the selectorPseudoClass
     */
    int getSelectorPseudoClass() {
        return selectorPseudoClass;
    }

    /**
     * Copies all properties, both string and numeric values to the destination element
     * This is used for grouped selectors
     * 
     * @param dest The destination selector
     */
    void copyAttributesTo(CSSElement dest) {
        for (int i=0;i<attrVals.length;i++) {
            dest.attrVals[i]=attrVals[i];
        }
        Hashtable attributes = getAttributes();
        if (attributes!=null) {
            for(Enumeration e=attributes.keys();e.hasMoreElements();) {
                Integer key=(Integer)e.nextElement();
                String value=(String)attributes.get(key);
                dest.setAttribute(key, value);
            }
        }
    }

    /**
     * Creates this CSSElement as a copy of the given selector
     * 
     * @param selector The selector to copy
     */
    CSSElement(CSSElement selector) {
        setTagId(selector.getTagId());
        setTagName(selector.getTagName());
        selector.copyAttributesTo(this);
        descendantSelector=selector.descendantSelector;
        attributeSelections=selector.attributeSelections; // This assumes that there are no modifications to this vector after the copy time
        for(int i=0;i<getNumChildren();i++) {
            addChild(new CSSElement(selector.getCSSChildAt(i)));
        }
    }

    /**
     * Simple data class to hold a data to be used for attribute selections
     *
     * @author Ofir Leitner
     */
    class AttString {

        static final int EQUALS = 0;
        static final int BEGINS_WITH = 1;
        static final int CONTAINS_WORD = 2;
        static final int LANG = 3;

        String attribute;
        String value;
        int constraint;

        AttString(String attribute,int constraint,String value) {
            this.attribute=attribute;
            this.constraint=constraint;
            this.value=value;
        }

    }

}

