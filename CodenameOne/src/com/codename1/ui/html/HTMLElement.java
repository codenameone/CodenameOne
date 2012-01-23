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

import com.codename1.xml.Element;
import com.codename1.ui.Component;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The HTMLElement class defines a single HTML element with its attributes and children.
 * Due to its hierarchial nature, this class can be used for a single "leaf" Element, for more complex elements (with child elements), and up to describing the entire document.
 *
 * @author Ofir Leitner
 */
public class HTMLElement extends Element {

//////////////////////////////////
// Tags                         //
//////////////////////////////////

 //HTML Tag ID codes:
 public static final int TAG_CSS_ILLEGAL_SELECTOR = -3; // If this is the tag ID it means this selector was malformed and as such will never match (For example p[] or a[=] - empty/meaningless attribute selection). Note that we can't simply discard this selector for the case of descendants: for example 'h1 a[] h2' - should never be matched - if we would discard the a[] which is illegal we would get 'h1 h2' which may be matched
 public static final int TAG_CSS_SELECTOR = -2;
 public static final int TAG_UNSUPPORTED = -1;

//Structure Module
 public static final int TAG_BODY = 0;
 public static final int TAG_HEAD = 1;
 public static final int TAG_HTML = 2;
 public static final int TAG_TITLE = 3;

//Text Module -
 public static final int TAG_ABBR = 4;    // No visual effect
 public static final int TAG_ACRONYM = 5; // No visual effect
 public static final int TAG_ADDRESS = 6; // No visual effect
 public static final int TAG_BLOCKQUOTE = 7;
 public static final int TAG_BR = 8;
 public static final int TAG_CITE = 9;
 public static final int TAG_CODE = 10;
 public static final int TAG_DFN = 11;
 public static final int TAG_DIV = 12;
 public static final int TAG_EM = 13;
 public static final int TAG_H1 = 14;
 public static final int TAG_H2 = 15;
 public static final int TAG_H3 = 16;
 public static final int TAG_H4 = 17;
 public static final int TAG_H5 = 18;
 public static final int TAG_H6 = 19;
 public static final int TAG_KBD = 20;
 public static final int TAG_P = 21;
 public static final int TAG_PRE = 22;
 public static final int TAG_Q = 23;
 public static final int TAG_SAMP = 24;
 public static final int TAG_SPAN = 25;   // While this is not parsed, it will be kept in the DOM and as such CSS will affect its class/ID
 public static final int TAG_STRONG = 26;
 public static final int TAG_VAR = 27;

 //Hypertext Module -
 public static final int TAG_A = 28;

 //List Module -
 public static final int TAG_DL = 29;
 public static final int TAG_DT = 30;
 public static final int TAG_DD = 31;
 public static final int TAG_OL = 32;
 public static final int TAG_UL = 33;
 public static final int TAG_LI = 34;

//Basic Forms Module -

 public static final int TAG_FORM = 35;
 public static final int TAG_INPUT = 36;
 public static final int TAG_LABEL = 37;
 public static final int TAG_SELECT = 38;
 public static final int TAG_OPTION = 39;
 public static final int TAG_TEXTAREA = 40;

 //Basic Tables Module -
 public static final int TAG_CAPTION = 41;
 public static final int TAG_TABLE = 42;
 public static final int TAG_TD = 43;
 public static final int TAG_TH = 44;
 public static final int TAG_TR = 45;

 //Image Module -
 public static final int TAG_IMG = 46;

 //Object Module -
 public static final int TAG_OBJECT = 47; // Not supported
 public static final int TAG_PARAM = 48;  // Not supported

 //Metainformation Module
 public static final int TAG_META = 49;

 //Link Module -
 public static final int TAG_LINK = 50;

 //Base Module
 public static final int TAG_BASE = 51;

 //XHTML Mobile Profile additons
 public static final int TAG_HR = 52;
 public static final int TAG_OPTGROUP = 53; // Currently still not supported as Codename One's ComboBox can't display option groups, but will display as a regular ComboBox
 public static final int TAG_STYLE = 54;
 public static final int TAG_B = 55;
 public static final int TAG_I = 56;
 public static final int TAG_BIG = 57;
 public static final int TAG_SMALL = 58;
 public static final int TAG_FIELDSET = 59;

 // HTML 4 tags - the following tags are not part of the XHTML MP 1.0 standard:

 public static final int TAG_U = 60; // Underline
 public static final int TAG_FONT = 61;
 public static final int TAG_DEL = 62; // Rendered same as S
 public static final int TAG_INS = 63; // Rendered same as U
 public static final int TAG_TT = 64;
 public static final int TAG_BASEFONT = 65;
 public static final int TAG_MENU = 66; // Same as UL
 public static final int TAG_S = 67; // Strike through
 public static final int TAG_STRIKE = 68; // Strike through
 public static final int TAG_CENTER = 69; // shorthand for DIV align=center
 public static final int TAG_DIR = 70;// Same as UL
 public static final int TAG_MAP = 71;
 public static final int TAG_AREA = 72;
 public static final int TAG_LEGEND = 73;
 public static final int TAG_SUB = 74;
 public static final int TAG_SUP = 75;
 public static final int TAG_NOSCRIPT = 76; // Since we don't support scripts this tag will always be evaluated
 public static final int TAG_NOFRAMES = 77; // Since we don't support frames this tag will always be evaluated
 public static final int TAG_THEAD = 78;
 public static final int TAG_TBODY = 79;
 public static final int TAG_TFOOT = 80;

 //Text nodes (not an actual tag - text segments are added by the parser as the 'text' tag
 public static final int TAG_TEXT = 81;

 private static int LAST_TAG_INDEX = HTMLComponent.PROCESS_HTML_MP1_ONLY?TAG_FIELDSET:TAG_TFOOT; // In any case we exclude TAG_TEXT, which is given only on text element creation


/**
 * Defines the tag names, these are specified according to the tag constants numbering.
 */
static final String[] TAG_NAMES = {
    "body","head","html","title"
    ,"abbr","acronym","address","blockquote","br","cite","code","dfn","div","em","h1","h2","h3","h4","h5","h6","kbd","p","pre","q","samp","span","strong","var"
    ,"a"
    ,"dl","dt","dd","ol","ul","li"
    ,"form","input","label","select","option","textarea"
    ,"caption","table","td","th","tr"
    ,"img"
    ,"object","param"
    ,"meta"
    ,"link"
    ,"base"
    ,"hr","optgroup","style","b","i","big","small","fieldset"
    //html4 tags
    ,"u","font","del","ins","tt","basefont","menu","s","strike","center","dir","map","area","legend","sub","sup","noscript","noframes"
    ,"thead","tbody","tfoot"
    ,"text"
};


//////////////////////////////////
// Attributes                   //
//////////////////////////////////

//Tag attributes:
 public static final int ATTR_CLASS = 0;
 public static final int ATTR_ID = 1;
 public static final int ATTR_STYLE = 2;
 public static final int ATTR_TITLE = 3;
 public static final int ATTR_XMLNS = 4;
 public static final int ATTR_XMLLANG = 5;
 public static final int ATTR_ALIGN = 6;
 public static final int ATTR_BGCOLOR = 7;
 public static final int ATTR_LINK = 8;
 public static final int ATTR_TEXT = 9;
 public static final int ATTR_VERSION = 10;
 public static final int ATTR_CITE = 11;
 public static final int ATTR_ACCESSKEY = 12;
 public static final int ATTR_CHARSET = 13;
 public static final int ATTR_HREF = 14;
 public static final int ATTR_HREFLANG = 15;
 public static final int ATTR_REL = 16;
 public static final int ATTR_REV = 17;
 public static final int ATTR_TABINDEX = 18;
 public static final int ATTR_TYPE = 19;
 public static final int ATTR_ACTION = 20;
 public static final int ATTR_ENCTYPE = 21;
 public static final int ATTR_METHOD = 22;
 public static final int ATTR_WIDTH = 23;
 public static final int ATTR_HEIGHT = 24;
 public static final int ATTR_ALT = 25;
 public static final int ATTR_HSPACE = 26;
 public static final int ATTR_VSPACE = 27;
 public static final int ATTR_LONGDESC = 28;
 public static final int ATTR_LOCALSRC = 29;
 public static final int ATTR_SRC = 30;
 public static final int ATTR_SIZE = 31;
 public static final int ATTR_CHECKED = 32;
 public static final int ATTR_EMPTYOK = 33;
 public static final int ATTR_FORMAT = 34;
 public static final int ATTR_ISTYLE = 35;
 public static final int ATTR_MAXLENGTH = 36;
 public static final int ATTR_NAME = 37;
 public static final int ATTR_VALUE = 38;
 public static final int ATTR_FOR = 39;
 public static final int ATTR_XMLSPACE = 40;
 public static final int ATTR_MULTIPLE = 41;
 public static final int ATTR_SELECTED = 42;
 public static final int ATTR_ABBR = 43;
 public static final int ATTR_AXIS = 44;
 public static final int ATTR_COLSPAN = 45;
 public static final int ATTR_HEADERS = 46;
 public static final int ATTR_ROWSPAN = 47;
 public static final int ATTR_SCOPE = 48;
 public static final int ATTR_VALIGN = 49;
 public static final int ATTR_START = 50;
 public static final int ATTR_MEDIA = 51;
 public static final int ATTR_LABEL = 52;
 public static final int ATTR_SUMMARY = 53;
 public static final int ATTR_CONTENT = 54;
 public static final int ATTR_HTTPEQUIV = 55;
 public static final int ATTR_SCHEME = 56;
 public static final int ATTR_COLS = 57;
 public static final int ATTR_ROWS = 58;

 // Unsupported attributes in XHTML-MP that we DO support
 public static final int ATTR_DIR = 59; //Currently only supported in the html tag
 public static final int ATTR_BORDER = 60;

 // Required by HTML4 tags we support:
 public static final int ATTR_COLOR = 61;
 public static final int ATTR_FACE = 62;
 public static final int ATTR_SHAPE = 63;
 public static final int ATTR_COORDS = 64;
 public static final int ATTR_USEMAP = 65;

 // HTML4 attributes
 public static final int ATTR_LANG = 66;
 public static final int ATTR_CELLSPACING = 67;
 public static final int ATTR_CELLPADDING = 68;
 public static final int ATTR_FRAME = 69;
 public static final int ATTR_RULES = 70;
 public static final int ATTR_DISABLED = 71;
 public static final int ATTR_READONLY = 72;
 public static final int ATTR_ISMAP = 73;

  /**
  * Defines the allowed attribute names, these are specified according to the ATTR_* constants numbering.
  */
 static final String[] ATTRIBUTE_NAMES = {
    "class", "id", "style", "title", "xmlns", "xml:lang", "align", "bgcolor", "link", "text", "version", "cite",
    "accesskey", "charset", "href", "hreflang", "rel", "rev", "tabindex", "type", "action", "enctype", "method",
    "width", "height", "alt", "hspace", "vspace", "longdesc", "localsrc", "src", "size", "checked", "emptyok",
    "format", "istyle", "maxlength", "name", "value", "for", "xml:space", "multiple", "selected","abbr","axis",
    "colspan","headers","rowspan","scope","valign","start","media","label","summary","content","http-equiv","scheme",
    "cols","rows","dir","border",
    "color","face","shape","coords","usemap",
    "lang","cellspacing","cellpadding","frame","rules",
    "disabled","readonly","ismap"
 };

 /**
  * This array defines the 6 common attributes that each tag has
  */
 private static final int[] COMMON_ATTRIBUTES = {ATTR_CLASS,ATTR_ID,ATTR_STYLE,ATTR_TITLE,ATTR_XMLNS,ATTR_XMLLANG,ATTR_LANG};

 /**
  * This array defines the allowed attributes for each tag, according to the XHTML-MP 1.0 spec
  */
 private static final int[][] TAG_ATTRIBUTES = {
    //Structure Module
     {
        ATTR_BGCOLOR, // #rrggbb | colors  // Deprecated but supported
        ATTR_LINK, // #rrggbb | colors     // Deprecated but supported
        ATTR_TEXT //#rrggbb | colors       // Deprecated but supported
     }, // BODY = 0;
     {}, // HEAD = 1;
     {
        //ATTR_VERSION, // = //WAPFORUM//DTD XHTML Mobile 1.0//EN // We don't use the version attribute
        ATTR_DIR

     }, // HTML = 2;
     {}, // TITLE = 3;

    //Text Module -
     {}, // ABBR = 4;
     {}, // ACRONYM = 5;
     {}, // ADDRESS = 6;
     {
         //ATTR_CITE // URL // Not supported by any of the major browsers
     }, // BLOCKQUOTE = 7;
     {}, // BR = 8;
     {}, // CITE = 9;
     {}, // CODE = 10;
     {}, // DFN = 11;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // DIV = 12;
     {}, // EM = 13;
     {
        ATTR_ALIGN // top/bottom/left/right
     }, // H1 = 14;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H2 = 15;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H3 = 16;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H4 = 17;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H5 = 18;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H6 = 19;
     {}, // KBD = 20;
     {
        ATTR_ALIGN // left | center | right | justify
     }, // P = 21;
     {
        //ATTR_XMLSPACE, // preserve // We don't use this attribute
     }, // PRE = 22;
     {
        //ATTR_CITE // URL // The cite attribute is not supported by any of the major browsers.
     }, // Q = 23;
     {}, // SAMP = 24;
     {}, // SPAN = 25;
     {}, // STRONG = 26;
     {}, // VAR = 27;

     //Hypertext Module -
     {
        ATTR_ACCESSKEY, // character
        //ATTR_CHARSET, // cdata // The charset attribute is not supported in any of the major browsers.
        ATTR_HREF, // URL
        //ATTR_HREFLANG, // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)? // The hreflang attribute is not supported in any of the major browsers.
        //ATTR_REL, // nmtokens  // Not used by browsers
        //ATTR_REV, // nmtokens  // Not used by browsers
        //ATTR_TABINDEX, // number 
        //ATTR_TYPE, // cdata // Should specify the MIME type of the document, but we don't use it anyway
        ATTR_NAME // Note: Name on the a tag (anchor) is not supported on XHTML-MP 1.0, but we support it
     }, // A = 28;

     //List Module -
     {}, // DL = 29;
     {}, // DT = 30;
     {}, // DD = 31;
     {
        ATTR_START, // number  // Deprecated but supported
        ATTR_TYPE // cdata     // Deprecated but supported
     }, // OL = 32;
     {}, // UL = 33;
     {
        ATTR_TYPE, // cdata    // Deprecated but supported
        ATTR_VALUE // number   // Deprecated but supported
     }, // LI = 34;
    //Basic Forms Module -
     {
        ATTR_ACTION, // URL
        ATTR_ENCTYPE, // cdata
        ATTR_METHOD, // get | post

     }, // FORM = 35;
     {
        ATTR_ACCESSKEY, // character
        ATTR_CHECKED, // checked
        ATTR_EMPTYOK, // true | false    // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        ATTR_FORMAT, // cdata            // Deprecated but still supported
        //ATTR_ISTYLE, // cdata            // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        //ATTR_LOCALSRC, // cdata          // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        ATTR_MAXLENGTH, // number
        ATTR_NAME, // cdata
        ATTR_SIZE, // cdata
        ATTR_SRC, // URL
        ATTR_TABINDEX, // number
        ATTR_TYPE, // text | password | checkbox | radio | submit | reset | hidden
        ATTR_VALUE, // cdata
        ATTR_DISABLED,
        ATTR_READONLY
     }, // INPUT = 36;
     {
        ATTR_ACCESSKEY,
        ATTR_FOR
     }, // LABEL = 37;
     {
        ATTR_MULTIPLE, // multiple
        ATTR_NAME, // cdata
        ATTR_SIZE, // number
        ATTR_TABINDEX, // number
        ATTR_DISABLED,
     }, // SELECT = 38;
     {
        ATTR_SELECTED, // selected
        ATTR_VALUE, // cdata
        ATTR_DISABLED,
     }, // OPTION = 39;
     {
        ATTR_ACCESSKEY, // character
        ATTR_COLS, // number
        ATTR_NAME, // cdata
        ATTR_ROWS, // number
        ATTR_TABINDEX, // number
        ATTR_DISABLED,
        ATTR_READONLY
     }, // TEXTAREA = 40;

     //Basic Tables Module -
     {
        ATTR_ALIGN // top/bottom/left/right
     }, // CAPTION = 41;
     {
        //ATTR_SUMMARY, // cdata  // The summary attribute makes no visual difference in ordinary web browsers.
        ATTR_BORDER,
        ATTR_CELLSPACING,
        ATTR_CELLPADDING,
        ATTR_FRAME,
        ATTR_RULES,
     }, // TABLE = 42;
     {
        //ATTR_ABBR, // cdata  // The abbr attribute makes no visual difference in ordinary web browsers.
        ATTR_ALIGN, // left | center | right
        //ATTR_AXIS, // cdata  // The axis attribute is not supported by any of the major browsers
        ATTR_COLSPAN, // number
        //ATTR_HEADERS, // IDREFS // The headers attribute makes no visual difference in ordinary web browsers.
        ATTR_ROWSPAN, // number
        //ATTR_SCOPE, // row | col // // The scope attribute makes no visual difference in ordinary web browsers.
        ATTR_VALIGN, // top | middle | bottom
        ATTR_WIDTH,  // number or % - deprecated but still supported
        ATTR_HEIGHT, // number or % - deprecated but still supported
     }, // TD = 43;
     {
        //ATTR_ABBR, // cdata  // The abbr attribute makes no visual difference in ordinary web browsers.
        ATTR_ALIGN, // left | center | right
        //ATTR_AXIS, // cdata  // The axis attribute is not supported by any of the major browsers
        ATTR_COLSPAN, // number
        //ATTR_HEADERS, // IDREFS // The headers attribute makes no visual difference in ordinary web browsers.
        ATTR_ROWSPAN, // number
        //ATTR_SCOPE, // row | col // // The scope attribute makes no visual difference in ordinary web browsers.
        ATTR_VALIGN, // top | middle | bottom
        ATTR_WIDTH,  // number or % - deprecated but still supported
        ATTR_HEIGHT, // number or % - deprecated but still supported

     }, // TH = 44;
     {
        ATTR_ALIGN, // = left | center | right
        ATTR_VALIGN // top | middle | bottom
     }, // TR = 45;

     //Image Module -
     {
        ATTR_ALIGN, // top | middle | bottom | left | right
        ATTR_ALT, // cdata
        ATTR_HEIGHT, // number[%]
        ATTR_HSPACE, // number
        //ATTR_LOCALSRC, // cdata          // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        //ATTR_LONGDESC, // URL //The longdesc attribute is not supported by any of the major browsers.
        ATTR_SRC, // URL
        ATTR_VSPACE, // number
        ATTR_WIDTH, // number[%]
        ATTR_USEMAP, // for HTML4
        ATTR_BORDER, // for HTML4
        ATTR_ISMAP // for HTML4
     }, // IMG = 46;

     //Object Module -
     {
    /*    ATTR_ARCHIVE, // &URLs
        ATTR_CLASSID, // URL
        ATTR_CODEBASE, // URL
        ATTR_CODETYPE, // cdata
        ATTR_DATA, // URL
        ATTR_DECLARE, // declare
        ATTR_HEIGHT, // number[%]
        ATTR_NAME, // cdata
        ATTR_STANDBY, // cdata
        ATTR_TABINDEX, // number
        ATTR_TYPE, // cdata
        ATTR_WIDTH, // number[%] */
     }, // OBJECT = 47;
     {
       /* ATTR_NAME, //  cdata
        ATTR_TYPE, //  cdata
        ATTR_VALUE, //  cdata
        ATTR_VALUETYPE // data | ref | object */

     }, // PARAM = 48;

     //Metainformation Module
     {
        ATTR_CONTENT, // cdata
        ATTR_HTTPEQUIV, // nmtoken
        ATTR_NAME, // nmtoken  // We do not make any use of this attribute
        //ATTR_SCHEME, // cdata  // We do not make any use of this attribute
     }, // META = 49;

     //Link Module -
     {
        //ATTR_CHARSET, // cdata  //The charset attribute is not supported by any of the major browsers.
        ATTR_HREF, // URL
        //ATTR_HREFLANG, // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)? // The hreflang attribute is not supported in any of the major browsers.
        ATTR_MEDIA, // cdata
        ATTR_REL, // nmtokens
        //ATTR_REV, // nmtokens // The rev attribute is not supported in any of the major browsers.
        ATTR_TYPE, // cdata
     }, // LINK = 50;

     //Base Module -
     {
        ATTR_HREF // URL
     }, // BASE = 51;

     //XHTML-MP
     {
        ATTR_ALIGN, // left | center | right
        ATTR_SIZE, // number     // Deprecated but still supported
        ATTR_WIDTH, // number[%] // Deprecated but still supported

     }, // HR = 52;
     {
        ATTR_LABEL
     }, // OPTGROUP = 53
     {
        ATTR_MEDIA, // cdata
        ATTR_TYPE, // cdata
        //ATTR_XMLSPACE, // preserve  // We don't use this attribute

     }, // STYLE = 54
     {}, //B = 55;
     {}, //I = 56;
     {}, //BIG = 57;
     {}, //SMALL = 58;
     {}, //FIELDSET = 59;
     {}, //U = 60; // Underline
     {
         ATTR_COLOR,
         ATTR_SIZE,
         ATTR_FACE,
     }, //FONT = 61;
     {}, //DEL = 62;
     {}, //INS = 63;
     {}, //TT = 64;
     {
         ATTR_COLOR,
         ATTR_SIZE,
         ATTR_FACE,
     }, //BASEFONT = 65;
     {}, //MENU = 66; // Same as UL
     {}, //S = 67; // Strike through
     {}, //STRIKE = 68; // Strike through
     {}, //CENTER = 69; // shorthand for DIV align=center
     {}, //DIR = 70;// Same as UL
     {
        ATTR_NAME
     }, //MAP = 71;
     {
        ATTR_SHAPE,
        ATTR_COORDS,
        ATTR_HREF,
        ATTR_ALT
     }, //AREA = 72;
     {}, //LEGEND = 73;
     {}, //SUB = 74;
     {}, //SUP = 75;
     {}, //NOSCRIPT = 76;
     {}, //NOFRAMES = 77;
     {
        ATTR_ALIGN,
        ATTR_VALIGN
     }, //TAG_THEAD = 78;
     {
        ATTR_ALIGN,
        ATTR_VALIGN
     }, //TAG_TBODY = 79;
     {
        ATTR_ALIGN,
        ATTR_VALIGN
     }, //TAG_TFOOT = 80;
     {}, //TEXT = 81;

 };




//////////////////////////////////
// Types                        //
//////////////////////////////////

// These are the possible types for an attribute. Types define what values are acceptable to the attribute.
 static final int TYPE_NUMBER = 0;
 static final int TYPE_PIXELS_OR_PERCENTAGE = 1;
 static final int TYPE_COLOR = 2;
 static final int TYPE_ALIGN = 3; //note: there are different types of align - TD allows only left,center,right DIV allows also justify, CAPTION allows top,bottom,left,right - however we don't get to that resolution
 static final int TYPE_CHAR = 4;
 static final int TYPE_URL = 5;
 static final int TYPE_CDATA = 6;
 static final int TYPE_NMTOKENS = 7;
 static final int TYPE_ID = 8;
 static final int TYPE_XMLNS = 9;
 static final int TYPE_LANG_CODE = 10; // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)?
 static final int TYPE_VERSION = 11;
 static final int TYPE_HTTP_METHOD = 12; // get / post
 static final int TYPE_BOOLEAN = 13;
 static final int TYPE_CHECKED = 14;
 static final int TYPE_IDREF = 15;
 static final int TYPE_PRESERVE = 16;
 static final int TYPE_MULTIPLE = 17;
 static final int TYPE_SELECTED = 18;
 static final int TYPE_IDREFS = 19;
 static final int TYPE_SCOPE = 20; // row / col
 static final int TYPE_VALIGN = 21; // top/middle/bottom
 static final int TYPE_NMTOKEN = 22; // top/middle/bottom
 static final int TYPE_DIRECTION = 23; // ltr/rtl

 static final int TYPE_CSS_LENGTH = 24; // values with CSS suffixes (px/em/ex etc.)
 static final int TYPE_CSS_LENGTH_OR_PERCENTAGE = 25; // values with CSS suffixes (px/em/ex etc.) or percentages
 static final int TYPE_CSS_URL = 26;
 static final int TYPE_CSS_LENGTH_OR_PERCENTAGE_OR_MULTIPLIER = 27; // values with CSS suffixes (px/em/ex etc.) or percentages or a number without unit that represents multiply by (i.e. 1 is 100% of the font size, 2 is 200% and so on)

 /**
  * This array assigns a type to each of the attributes.
  */
 private static final int[] ATTRIBUTE_TYPES = {
    TYPE_NMTOKENS, //"class",
    TYPE_ID, //"id",
    TYPE_CDATA, //"style",
    TYPE_CDATA, //"title",
    TYPE_XMLNS, //"xmlns",
    TYPE_LANG_CODE, //"xml:lang",
    TYPE_ALIGN, //"align",
    TYPE_COLOR, //"bgcolor",
    TYPE_COLOR, //"link",
    TYPE_COLOR, //"text",
    TYPE_VERSION, //"version",
    TYPE_URL, //"cite",
    TYPE_CHAR, //"accesskey",
    TYPE_CDATA, //"charset",
    TYPE_URL, //"href",
    TYPE_LANG_CODE, //"hreflang",
    TYPE_NMTOKENS, //"rel",
    TYPE_NMTOKENS, //"rev",
    TYPE_NUMBER, //"tabindex",
    TYPE_CDATA, //"type",
    TYPE_URL, //"action",
    TYPE_CDATA, //"enctype",
    TYPE_HTTP_METHOD, //"method",
    TYPE_PIXELS_OR_PERCENTAGE, //"width",
    TYPE_PIXELS_OR_PERCENTAGE, //"height",
    TYPE_CDATA, //"alt",
    TYPE_NUMBER, //"hspace",
    TYPE_NUMBER, //"vspace",
    TYPE_URL, //"longdesc",
    TYPE_CDATA, //"localsrc",
    TYPE_URL, //"src",
    TYPE_CDATA, //"size",
    TYPE_CHECKED, //"checked",
    TYPE_BOOLEAN, //"emptyok",
    TYPE_CDATA, //"format",
    TYPE_CDATA, //"istyle",
    TYPE_NUMBER, //"maxlength",
    TYPE_CDATA, //"name",
    TYPE_CDATA, //"value",
    TYPE_IDREF, //"for",
    TYPE_PRESERVE, //"xml:space",
    TYPE_MULTIPLE, //"multiple",
    TYPE_SELECTED, //"selected",
    TYPE_CDATA, //"abbr",
    TYPE_CDATA, //"axis",
    TYPE_NUMBER, //"colspan",
    TYPE_IDREFS, //"headers",
    TYPE_NUMBER, //"rowspan",
    TYPE_SCOPE, //"scope",
    TYPE_VALIGN, //"valign",
    TYPE_NUMBER, //"start",
    TYPE_CDATA, //"media",
    TYPE_CDATA, //"label",
    TYPE_CDATA, //"summary",
    TYPE_CDATA, //"content",
    TYPE_NMTOKEN, //"http-equiv",
    TYPE_CDATA, //"scheme",
    TYPE_NUMBER, //"cols",
    TYPE_NUMBER, //"rows"
    TYPE_DIRECTION, //"dir"
    TYPE_NUMBER, // "border"
    TYPE_COLOR, // "color"
    TYPE_NMTOKEN, // "face"
    TYPE_NMTOKEN, // "shape"
    TYPE_NMTOKEN, // "coords"
    TYPE_NMTOKEN, // "usemap"
    TYPE_NMTOKEN, // "lang"
    TYPE_NUMBER, // "cellspacing"
    TYPE_NUMBER, // "cellpadding"
    TYPE_NMTOKEN, // "frame"
    TYPE_NMTOKEN, // "rules"
    TYPE_NMTOKEN, //ATTR_DISABLED = 71;
    TYPE_NMTOKEN, //ATTR_READONLY = 72;
    TYPE_NMTOKEN, //ATTR_ISMAP = 73;
 };

/**
 * Some types accept only a specific set of strings. For these this array defines the allowed strings.
 * If the value is null it means that the type has another rule set (for example numbers only).
 * This is checked against in the DOM building process.
 */
 private static String[][] ALLOWED_STRINGS = {
    null, // TYPE_NUMBER = 0;
    null, // TYPE_PIXELS_OR_PERCENTAGE = 1;
    null, // TYPE_COLOR = 2;
    {"left","right","top","bottom","center","middle","justify"}, // TYPE_ALIGN = 3;
    null, // TYPE_CHAR = 4;
    null, // TYPE_URL = 5;
    null, // TYPE_CDATA = 6;
    null, // TYPE_NMTOKENS = 7;
    null, // TYPE_ID = 8;
    null, // TYPE_XMLNS = 9;
    null, // TYPE_LANG_CODE = 10; // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)?
    null, // TYPE_VERSION = 11;
    {"get","post"}, // TYPE_HTTP_METHOD = 12; // get / post
    {"true","false"}, // TYPE_BOOLEAN = 13;
    {"checked"}, // TYPE_CHECKED = 14;
    null, // TYPE_IDREF = 15;
    {"default","preserve"}, // TYPE_PRESERVE = 16;
    {"multiple"}, // TYPE_MULTIPLE = 17;
    {"selected"}, // TYPE_SELECTED = 18;
    null, // TYPE_IDREFS = 19;
    {"row","col"}, // TYPE_SCOPE = 20; // row / col
    {"top","bottom","middle"}, // TYPE_VALIGN = 21; // top/middle/bottom
    null, // TYPE_NMTOKEN = 22; // top/middle/bottom
    {"ltr","rtl"}, // TYPE_DIRECTION
};


 /**
  * The allowed values for the 'frame' attribute in the 'table' tag
  * (This was not placed in the ALLOWED_STRINGS in order not to add more data types, since it is very specific for this attribute)
  */
 static final String[] ALLOWED_TABLE_FRAME_STRINGS =
            {"void","above","below","hsides","vsides","lhs","rhs","box","border"};

 /**
  * The values each string of ALLOWED_TABLE_FRAME_STRINGS represents
  */
 static final int[][] ALLOWED_TABLE_FRAME_VALS =
            {
                {}, // void
                {Component.TOP}, // above
                {Component.BOTTOM}, //below
                {Component.TOP, Component.BOTTOM}, //hsides
                {Component.LEFT, Component.RIGHT}, //vsides
                {Component.LEFT}, //lhs
                {Component.RIGHT}, // rhs
                {Component.LEFT,Component.RIGHT,Component.TOP,Component.BOTTOM}, //box
                {Component.LEFT,Component.RIGHT,Component.TOP,Component.BOTTOM}, //border
        };

 /**
  * The allowed values for the 'rules' attribute in the 'table' tag
  * (This was not placed in the ALLOWED_STRINGS in order not to add more data types, since it is very specific for this attribute)
  */
 static final String[] ALLOWED_TABLE_RULES_STRINGS = 
            {"none","rows","cols","all","groups"};


// Additional constants used to define allowed characters for specific types
private static final int DIGITS = 1;
private static final int HEX = 2;
private static final int ABC = 4;


//////////////////////////////////
// Colors                       //
//////////////////////////////////

//HTML-MP colors
public static final int COLOR_AQUA = 0x00ffff;
public static final int COLOR_BLACK = 0x000000;
public static final int COLOR_BLUE = 0x0000ff;
public static final int COLOR_FUCHSIA = 0xff00ff;
public static final int COLOR_GRAY = 0x808080;
public static final int COLOR_GREEN = 0x008000;
public static final int COLOR_LIME = 0x00ff00;
public static final int COLOR_MAROON = 0x800000;
public static final int COLOR_NAVY = 0x000080;
public static final int COLOR_OLIVE = 0x808000;
public static final int COLOR_PURPLE = 0x800080;
public static final int COLOR_RED = 0xff0000;
public static final int COLOR_SILVER = 0xc0c0c0;
public static final int COLOR_TEAL = 0x008080;
public static final int COLOR_WHITE = 0xffffff;
public static final int COLOR_YELLOW = 0xffff00;
public static final int COLOR_ORANGE = 0xffa500; // Orange is added in CSS 2.1


/**
 * Defines the allowed color string that are acceptable as a value to color attributes in HTML-MP1
 */
static final String[] COLOR_STRINGS = {
    "aqua","black","blue","fuchsia","gray","green","lime","maroon",
    "navy","olive","purple","red","silver","teal","white","yellow",
    "orange","grey"
};

/**
 * Assigns a color constant to each of the colors defined in COLOR_STRINGS
 */
static final int[] COLOR_VALS = {
    COLOR_AQUA,COLOR_BLACK,COLOR_BLUE,COLOR_FUCHSIA,COLOR_GRAY,COLOR_GREEN,COLOR_LIME,COLOR_MAROON,
    COLOR_NAVY,COLOR_OLIVE,COLOR_PURPLE,COLOR_RED,COLOR_SILVER,COLOR_TEAL,COLOR_WHITE,COLOR_YELLOW,
    COLOR_ORANGE,COLOR_GRAY
};

/**
  * Defines additional allowed color string that are acceptable as a value to color attributes in HTML4
  */
static final String[] MORE_COLOR_STRINGS = {
  "AliceBlue","AntiqueWhite","Aqua","Aquamarine","Azure","Beige","Bisque","Black","BlanchedAlmond","Blue","BlueViolet","Brown","BurlyWood",
  "CadetBlue","Chartreuse","Chocolate","Coral","CornflowerBlue","Cornsilk","Crimson","Cyan","DarkBlue","DarkCyan","DarkGoldenRod","DarkGray",
  "DarkGrey","DarkGreen","DarkKhaki","DarkMagenta","DarkOliveGreen","Darkorange","DarkOrchid","DarkRed","DarkSalmon","DarkSeaGreen",
  "DarkSlateBlue","DarkSlateGray","DarkSlateGrey","DarkTurquoise","DarkViolet","DeepPink","DeepSkyBlue","DimGray","DimGrey","DodgerBlue",
  "FireBrick","FloralWhite","ForestGreen","Fuchsia","Gainsboro","GhostWhite","Gold","GoldenRod","Gray","Grey","Green","GreenYellow","HoneyDew",
  "HotPink","IndianRed ","Indigo ","Ivory","Khaki","Lavender","LavenderBlush","LawnGreen","LemonChiffon","LightBlue","LightCoral","LightCyan",
  "LightGoldenRodYellow","LightGray","LightGrey","LightGreen","LightPink","LightSalmon","LightSeaGreen","LightSkyBlue","LightSlateGray",
  "LightSlateGrey","LightSteelBlue","LightYellow","Lime","LimeGreen","Linen","Magenta","Maroon","MediumAquaMarine","MediumBlue","MediumOrchid",
  "MediumPurple","MediumSeaGreen","MediumSlateBlue","MediumSpringGreen","MediumTurquoise","MediumVioletRed","MidnightBlue","MintCream",
  "MistyRose","Moccasin","NavajoWhite","Navy","OldLace","Olive","OliveDrab","Orange","OrangeRed","Orchid","PaleGoldenRod","PaleGreen",
  "PaleTurquoise","PaleVioletRed","PapayaWhip","PeachPuff","Peru","Pink","Plum","PowderBlue","Purple","Red","RosyBrown","RoyalBlue",
  "SaddleBrown","Salmon","SandyBrown","SeaGreen","SeaShell","Sienna","Silver","SkyBlue","SlateBlue","SlateGray","SlateGrey","Snow","SpringGreen",
  "SteelBlue","Tan","Teal","Thistle","Tomato","Turquoise","Violet","Wheat","White","WhiteSmoke","Yellow","YellowGreen"
};

/**
 * Assigns a color constant to each of the colors defined in MORE_COLOR_STRINGS
 */
static final int[] MORE_COLOR_VALS = {
    0xF0F8FF,0xFAEBD7,0x00FFFF,0x7FFFD4,0xF0FFFF,0xF5F5DC,0xFFE4C4,0x000000,0xFFEBCD,0x0000FF,0x8A2BE2,0xA52A2A,0xDEB887,0x5F9EA0,0x7FFF00,0xD2691E,
    0xFF7F50,0x6495ED,0xFFF8DC,0xDC143C,0x00FFFF,0x00008B,0x008B8B,0xB8860B,0xA9A9A9,0xA9A9A9,0x006400,0xBDB76B,0x8B008B,0x556B2F,0xFF8C00,0x9932CC,
    0x8B0000,0xE9967A,0x8FBC8F,0x483D8B,0x2F4F4F,0x2F4F4F,0x00CED1,0x9400D3,0xFF1493,0x00BFFF,0x696969,0x696969,0x1E90FF,0xB22222,0xFFFAF0,0x228B22,
    0xFF00FF,0xDCDCDC,0xF8F8FF,0xFFD700,0xDAA520,0x808080,0x808080,0x008000,0xADFF2F,0xF0FFF0,0xFF69B4,0xCD5C5C,0x4B0082,0xFFFFF0,0xF0E68C,0xE6E6FA,
    0xFFF0F5,0x7CFC00,0xFFFACD,0xADD8E6,0xF08080,0xE0FFFF,0xFAFAD2,0xD3D3D3,0xD3D3D3,0x90EE90,0xFFB6C1,0xFFA07A,0x20B2AA,0x87CEFA,0x778899,0x778899,
    0xB0C4DE,0xFFFFE0,0x00FF00,0x32CD32,0xFAF0E6,0xFF00FF,0x800000,0x66CDAA,0x0000CD,0xBA55D3,0x9370D8,0x3CB371,0x7B68EE,0x00FA9A,0x48D1CC,0xC71585,
    0x191970,0xF5FFFA,0xFFE4E1,0xFFE4B5,0xFFDEAD,0x000080,0xFDF5E6,0x808000,0x6B8E23,0xFFA500,0xFF4500,0xDA70D6,0xEEE8AA,0x98FB98,0xAFEEEE,0xD87093,
    0xFFEFD5,0xFFDAB9,0xCD853F,0xFFC0CB,0xDDA0DD,0xB0E0E6,0x800080,0xFF0000,0xBC8F8F,0x4169E1,0x8B4513,0xFA8072,0xF4A460,0x2E8B57,0xFFF5EE,0xA0522D,
    0xC0C0C0,0x87CEEB,0x6A5ACD,0x708090,0x708090,0xFFFAFA,0x00FF7F,0x4682B4,0xD2B48C,0x008080,0xD8BFD8,0xFF6347,0x40E0D0,0xEE82EE,0xF5DEB3,0xFFFFFF,
    0xF5F5F5,0xFFFF00,0x9ACD32
};

/**
 * Converts a color string into an int value.
 * This method supports color denoted in hex (with a leading #), named colors (from the standard HTML colors) and rgb(x,y,z)
 * 
 * @param colorStr The string representing the color
 * @param defaultColor Default color if color parsing can't be done
 * @return The int value of the parsed color
 */
static int getColor(String colorStr,int defaultColor) {
    if ((colorStr==null) || (colorStr.equals(""))) {
        return defaultColor;
    }
    if (colorStr.charAt(0)!='#') {

        if (colorStr.startsWith("rgb(")) {
            colorStr=colorStr.substring(4);
            char[] tokens= {',',',',')'};
            int weight=256*256;
            int color=0;
            for(int i=0;i<3;i++) {
                int index=colorStr.indexOf(tokens[i]);
                if (index==-1) {
                    return defaultColor; // Unparsed color
                }
                String channelStr=colorStr.substring(0, index).trim();

                int channel=HTMLComponent.calcSize(255, channelStr, 0,true);
                channel=Math.min(channel, 255); // Set to 255 if over 255
                channel=Math.max(channel, 0); // Set to 0 if negative

                color+=channel*weight;
                colorStr=colorStr.substring(index+1);
                weight/=256;
            }
            return color;

        } else {
            for(int i=0;i<COLOR_STRINGS.length;i++) {
                if (colorStr.equalsIgnoreCase(COLOR_STRINGS[i])) {
                    return COLOR_VALS[i];
                }
            }
            if (!HTMLComponent.PROCESS_HTML_MP1_ONLY) {
                for(int i=0;i<MORE_COLOR_STRINGS.length;i++) {
                    if (colorStr.equalsIgnoreCase(MORE_COLOR_STRINGS[i])) {
                        return MORE_COLOR_VALS[i];
                    }
                }
            }

        }
    } else {
        colorStr=colorStr.substring(1);
    }

    if (colorStr.length()==3) { // shortened format rgb - translated to rrggbb
        String newColStr="";
        for(int i=0;i<3;i++) {
            newColStr+=colorStr.charAt(i)+""+colorStr.charAt(i);
        }
        colorStr=newColStr;
    }

    try {
        int color=Integer.parseInt(colorStr,16);
        return color;
    } catch (NumberFormatException nfe) {
        return defaultColor;
    }
}


// Member variables:

    /**
     * The tag ID. Upon construction of tag with a name, a lookup is performed to assign it an ID accordign to the TAG_NAMES array.
     * THe ID is used to find what are the allowed attributes, and also prevents the need for further string parsing later on.
     */
    private int id=TAG_UNSUPPORTED;

    /**
     * A vector holding all associate components
     */
     private Vector comps;

     /**
      * If true than the UI components where calculated automatically
      */
     private boolean calculatedUi = false;


    /**
     * Constructs and HTMLElement without specifying a name.
     * This can be used by subclasses that do not require name assigments.
     */
    protected HTMLElement() {
    }

    /**
     * Constructor for HTMLElement. This mostly sets up the element's ID.
     * 
     * @param tagName The HTMLElement's name
     */
    public HTMLElement(String tagName) {
        init(tagName);
    }

    private void init(String tagName) {
        int i=0;
        int tagId=-1;

        while((tagId==-1) && (i<=LAST_TAG_INDEX)) {   // -1 to exclude TAG_TEXT, which is given only on text element creation
            if (TAG_NAMES[i].equals(tagName)) {
                tagId=i;
            } else {
                i++;
            }
        }
        id=tagId;
        if (id==TAG_UNSUPPORTED) {
            setTagName(tagName);
        }
    }

    /**
     * Constructor for HTMLElement. This mostly sets up the element's ID.
     *
     * @param tagName The HTMLElement's name, or the text for text elements
     * @param isTextElement true for a text element, false otherwise
     */
    public HTMLElement(String tagName,boolean isTextElement) {
        setTextElement(isTextElement);
        if (isTextElement) {
            setTagName(tagName);
            id=TAG_TEXT;
        } else {
            init(tagName);
        }
    }

    /**
     * Sets the given component or Vector of components to be associated with this element.
     * This is used internally to apply CSS styling.
     * 
     * @param obj The component (or vector of components) representing this HTMLElement
     */
    void setAssociatedComponents(Object obj) {
        if (obj instanceof Vector) {
            comps=(Vector)obj;
        } else {
            comps=new Vector();
            comps.addElement(obj);
        }
    }

    /**
     * Clears the associated components object
     */
    void clearAssociatedComponents() {
        comps=null;
    }

    /**
     * Adds the given component to be associated with this element.
     * This is used internally to apply CSS styling.
     * 
     * @param cmp The component to add
     */
    void addAssociatedComponent(Component cmp) {
        if (comps==null) {
            comps=new Vector();
        }
        comps.addElement(cmp);
    }

    /**
     * Adds the given component to be associated with this element.
     * This is used internally to apply CSS styling.
     *
     * @param index The index to insert the component to
     * @param cmp The component to add
     */
    void addAssociatedComponentAt(int index,Component cmp) {
        if (comps==null) {
            comps=new Vector();
        }
        comps.insertElementAt(cmp, index);
    }


    public void addChild(Element childElement) {
        //if (((HTMLElement)childElement).getId()!=TAG_UNSUPPORTED) {
            super.addChild(childElement);
        //}

    }

    /**
     * Returns whether this element supports the common core attributes.
     * These are attributes most HTML tags support, with a few exceptions that are checked here.
     * Note that to be exact the common atributes are divided to 2 groups: core attributes (class,id,title,style) and language attributes (xmlns,xml:lang)
     * The tags checked here all don't support the core attributes but in fact may support the language attributes.
     * Since the language attributes are not implemented anyway, this is not critical.
     * For reference, tags that do not support the language attributes in XHTML-MP1 are: param, hr, base, br
     * 
     * @return true if core attributes are supported, false otherwise
     */
    private boolean supportsCoreAttributes() {
        return ((id!=TAG_STYLE) && (id!=TAG_META) && (id!=TAG_HEAD) && (id!=TAG_HTML) && (id!=TAG_TITLE) && (id!=TAG_PARAM) && (id!=TAG_BASE));
    }

    /**
     * Adds the specified attribute and value to this Element if it is supported for the Element and has a valid value.
     *
     * @param attribute The attribute's name
     * @param value The attribute's value
     *
     * @return a positive error code or -1 if attribute is supported and valid
     */
    public int setAttribute(String attribute,String value) {
        if (id==TAG_UNSUPPORTED) {
            return -1; //No error code for this case since tag not supported error is already notified before
        }

        int attrId=-1;
        int i=0;
        if (supportsCoreAttributes()) {
            while ((attrId==-1) && (i<COMMON_ATTRIBUTES.length)) {
                if (ATTRIBUTE_NAMES[COMMON_ATTRIBUTES[i]].equals(attribute)) {
                    attrId=COMMON_ATTRIBUTES[i];
                } else {
                    i++;
                }
            }
        }

        i=0;
        while ((attrId==-1) && (i<TAG_ATTRIBUTES[id].length)) {
            if (ATTRIBUTE_NAMES[TAG_ATTRIBUTES[id][i]].equals(attribute)) {
                attrId=TAG_ATTRIBUTES[id][i];
            } else {
                i++;
            }
        }

        if (attrId==-1) {
            return HTMLCallback.ERROR_ATTRIBUTE_NOT_SUPPORTED;

        } else {
            if (isValid(ATTRIBUTE_TYPES[attrId], value)) {
                setAttribute(new Integer(attrId), value);
            } else {
                return HTMLCallback.ERROR_ATTIBUTE_VALUE_INVALID;
            }
        }
        
        return -1;
    }

    /**
     * Allows setting an attribute with an attribute id
     * 
     * @param attrId The attribute Id (One of the ATTR_ constants)
     * @param value The value to set to the attribute
     */
    public void setAttributeById(int attrId,String value) {
        if ((attrId<0) || (attrId>=ATTRIBUTE_NAMES.length)) {
            throw new IllegalArgumentException("Attribute Id must be in the range of 0-"+(ATTRIBUTE_NAMES.length-1));
        }
        if (isValid(ATTRIBUTE_TYPES[attrId], value)) {
            setAttribute(new Integer(attrId), value);
        } else {
            throw new IllegalArgumentException(value+" is not a valid value for attribute "+ATTRIBUTE_NAMES[attrId]);
        }
    }

    /**
     * Removes the specified attribute
     * 
     * @param attrId The attribute Id (One of the ATTR_ constants)
     */
    public void removeAttributeById(int attrId) {
        if ((attrId<0) || (attrId>=ATTRIBUTE_NAMES.length)) {
            throw new IllegalArgumentException("Attribute Id must be in the range of 0-"+(ATTRIBUTE_NAMES.length-1));
        }
        removeAttribute(new Integer(attrId));
    }

    /**
     * Returns a list of supported attributes for this tag. Note that the list does not include the core attributes that are supported on almost all tags
     * 
     * @return a list of supported attributes for this tag
     */
    public String getSupportedAttributesList() {
        if ((id<0) || (id>=TAG_ATTRIBUTES.length)) {
            return "Unknown";
        }
        String list="";
        for (int a=0;a<TAG_ATTRIBUTES[id].length;a++) {
            list+=ATTRIBUTE_NAMES[TAG_ATTRIBUTES[id][a]]+",";
        }
        if (supportsCoreAttributes()) {
            for (int a=0;a<COMMON_ATTRIBUTES.length;a++) {
                list+=ATTRIBUTE_NAMES[COMMON_ATTRIBUTES[a]]+",";
            }
        }

        if (list.endsWith(",")) {
            list=list.substring(0, list.length()-1);
        }
        if (list.equals("")) {
            list="None";
        }
        return list;
    }

    /**
     * Verifies that the specified value conforms with the attribute's type restrictions.
     * This basically checks the attribute type and according to that checks the value.
     *
     * @param attrId The attribute ID
     * @param value The value to be checked
     * @return true if the value is valid for this attribute, false otherwise
     */
    private boolean isValid(int type,String value) {
        if (value==null) { // a null value is invalid for all attributes
            return false;
        }
        if (ALLOWED_STRINGS[type]!=null) {
            return verifyStringGroup(value, ALLOWED_STRINGS[type]);
        }

        switch(type) {
            case TYPE_NUMBER:
                return verify(value, DIGITS, null);
            case TYPE_PIXELS_OR_PERCENTAGE:
                if (value.endsWith("%")) { //percentage
                    value=value.substring(0,value.length()-1);
                } else if (value.endsWith("px")) { //pixels
                    value=value.substring(0,value.length()-2);
                }
                return verify(value, DIGITS, null);
            case TYPE_CHAR:
                return verify(value, DIGITS|ABC, null, 1, 1);
            case TYPE_COLOR:
                if (value.length()==0) {
                    return false;
                }
                if (value.charAt(0)!='#') {
                    return verifyStringGroup(value, COLOR_STRINGS);
                } else {
                    return verify(value.substring(1), HEX, null, 3, 6); //Color can also be #rgb which means #rrggbb (as for 4,5 chars these will also be tolerated)
                }

            default:
                return true;
        }
    }

    /**
     * A convenience method for verifying strings with no length restrictions
     * 
     * @param value The string to be checked
     * @param allowedMask DIGITS or HEX or ABC or a combination of those
     * @param allowedChars Characters that are allowed even if they don't conform to the mask
     * @return true if the string is valid, false otherwise.
     */
    private boolean verify(String value,int allowedMask,char[] allowedChars) {
        return verify(value,allowedMask,allowedChars,-1,-1);
    }
    
    /**
     * Verifies that the specified string conforms with the specified restrictions.
     *
     * @param value The string to be checked
     * @param allowedMask DIGITS or HEX or ABC or a combination of those
     * @param allowedChars Characters that are allowed even if they don't conform to the mask
     * @param minLength Minimum length
     * @param maxLength Maximum length
     * @return true if the string is valid, false otherwise.
     */
    private boolean verify(String value,int allowedMask,char[] allowedChars,int minLength,int maxLength) {
            if ((minLength!=-1) && (value.length()<minLength)) {
                return false;
            }
            if ((maxLength!=-1) && (value.length()>maxLength)) {
                return false;
            }


            int i=0;
            while (i<value.length()) {
                boolean found=false;
                char ch=value.charAt(i);
                if ((allowedMask & HEX)!=0) {
                    if (((ch>='0') && (ch<='9')) ||
                        ((ch>='A') && (ch<='F')) ||
                        ((ch>='a') && (ch<='f'))) {
                        found=true;
                    }
                }

                if ((allowedMask & DIGITS)!=0) {
                    if (((ch>='0') && (ch<='9'))) {
                        found=true;
                    } else if ((i==0) && ((ch=='-') || (ch=='+'))) { // Sign is allowed as the first character
                        found=true;
                    }
                }

                if ((!found) && ((allowedMask & ABC)!=0)) {
                    if (((ch>='a') && (ch<='z')) ||
                        ((ch>='A') && (ch<='Z')))
                    {
                        found=true;
                    }
                }

                if ((!found) && (allowedChars!=null)) {
                    int c=0;
                    while ((!found) && (c<allowedChars.length)) {
                        if (ch==allowedChars[c]) {
                            found=true;
                        } else {
                            c++;
                        }
                    }
                }

                if (!found) {
                    return false;
                }
                i++;

            }
            return true;
    }

    /**
     * Verifies that the specified string equals to one of the allowed strings
     * 
     * @param value The string to be checked
     * @param allowed The list of allowed strings
     * @return true if the string equals to one of the allowed, false otherwise
     */
    boolean verifyStringGroup(String value,String[] allowed) {
        for(int i=0;i<allowed.length;i++) {
            if (value.equalsIgnoreCase(allowed[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns this HTMLElement's tag name
     * 
     * @return the HTMLElement's tag name
     */
    public String getTagName() {
        String name=super.getTagName();

        if (name!=null) { //unsupported tags
            return name;
        }
//        if ((id<0) || (id>=TAG_NAMES.length)) {
//            return "Unsupported";
//        }
        return TAG_NAMES[id];
    }

    /**
     * Returns this HTMLElement's ID
     *
     * @return the HTMLELement's ID
     */
    public int getTagId() {
        return id;
    }

    /**
     * Sets this HTMLElement's ID
     * 
     * @param tagId The tag ID to set, one of the TAG_* constants (Not to be confused with the id attribute)
     */
    protected void setTagId(int tagId) {
        this.id=tagId;
    }

    /**
     * Returns an HTMLElement's child by a tag ID (One of the TAG_* constants)
     * 
     * @param tagId The child's tag ID, one of the TAG_* constants (Not to be confused with the id attribute)
     * @return the first child with the specified ID, or null if not found
     */
    public HTMLElement getFirstChildByTagId(int tagId) {
        Vector children=getChildren();
        if (children==null) {
            return null;
        }
        int i=0;
        HTMLElement found=null;
        while ((found==null) && (i<children.size())) {
            HTMLElement child=(HTMLElement)children.elementAt(i);
            if (child.getTagId()==tagId) {
                found=child;
            } else {
                i++;
            }
        }
        return found;

    }

    /**
     * Returns an HTMLElement's attribute by the attribute's ID (One of the ATTR_* constants)
     *
     * @param id The attribute's ID
     * @return the attribute with the specified ID, or null if not found
     */
    public String getAttributeById(int id) {
        Hashtable attributes=getAttributes();
        if (attributes==null) {
            return null;
        }
        return (String)attributes.get(new Integer(id));
    }

    /**
     *
     * {@inheritDoc}
     */
    public String toString() {
        return toString("");
    }

    
    /**
     * Returns the attribute name of the requested attribute
     * 
     * @param attrKey The attribute key, which is typically an Integer object made of its int attrId
     * @return the attribute name of the requested attribute
     */
    public String getAttributeName(Integer attrKey) { // This method is not static since it needs to be overriden in CSSElement
        return ATTRIBUTE_NAMES[attrKey.intValue()];
    }

    public String getAttribute(String name) {
        Hashtable attributes=getAttributes();
        if (attributes!=null) {
            for(int i=0;i<ATTRIBUTE_NAMES.length;i++) {
                if (name.equalsIgnoreCase(ATTRIBUTE_NAMES[i])) {
                    return getAttributeById(i);
                }
            }
        }
        return null;
    }

    /**
     * A recursive method for creating a printout of a full tag with its entire hierarchy.
     * This is used by the public method toString().
     *
     * @param spacing Increased by one in each recursion phase to provide with indentation
     * @return the printout of this tag
     */
    private String toString(String spacing) {
        String str=spacing;
        if (!isTextElement()) {
            str+="<"+getTagName();
            Hashtable attributes=getAttributes();
            if (attributes!=null) {
                for(Enumeration e=attributes.keys();e.hasMoreElements();) {
                    Integer attrKey=(Integer)e.nextElement();
                    String attrStr=getAttributeName(attrKey);

                    String val=(String)attributes.get(attrKey);
                    str+=" "+attrStr+"='"+val+"' ("+attrKey+")";
                }
            }
            str+=">\n";

            Vector children=getChildren();
            if (children!=null) {
                for(int i=0;i<children.size();i++) {
                    str+=((HTMLElement)children.elementAt(i)).toString(spacing+' ');
                }
            }
            str+=spacing+"</"+getTagName()+">\n";
        } else {
            str+="'"+getText()+"'\n";
        }
        return str;
   }

    /**
     * Returns a vector of Components associated with this HTMLElement
     * 
     * @return a vector of Components associated with this HTMLElement
     */
    Vector getUi() {
        if (comps==null) { // If no UI exists this may be a tag with children that do have UI, such as TAG_A
            comps=new Vector();
            Vector children=getChildren();
            if (children!=null) {
                for (Enumeration e=children.elements();e.hasMoreElements();) {
                    HTMLElement child = (HTMLElement)e.nextElement();
                    Vector childUI=child.getUi();
                    for (Enumeration e2=childUI.elements();e2.hasMoreElements();) {
                        comps.addElement(e2.nextElement());
                    }
                }
            }
            calculatedUi=true;
        }
        return comps;
    }

    /**
     * Causes a recalculation of the UI, if the UI of this element was deduced from children components
     */
    void recalcUi() {
        if (calculatedUi) {
            comps=null;
            calculatedUi=false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeChildAt(int index) { // Overiding is done to clear the associated components vector of the child
        HTMLElement child=(HTMLElement)getChildAt(index);
        child.clearAssociatedComponents();
        super.removeChildAt(index);
    }

    private void getDescendantsByTagIdInternal(Vector v,int tagId,int depth) {
        int i=0;
        Vector children=getChildren();
        if (children!=null) {
            while (i<children.size()) {
                HTMLElement child=(HTMLElement)children.elementAt(i);
                if (depth>0) {
                    child.getDescendantsByTagIdInternal(v, tagId, depth-1);
                }
                if (child.getTagId()==tagId) {
                    v.addElement(child);
                }
                i++;
            }
        }

    }

    /**
     *  Returns all descendants with the specified tag id
     *
     * @param tagId The tag ID to look for, one of the TAG_* constants (Not to be confused with the id attribute)
     * @param depth The search depth (1 - children, 2 - grandchildren .... DEPTH_INFINITE - for all descendants)
     * @return A vector containing descendants with the specified tag id
     */
    public Vector getDescendantsByTagId(int tagId,int depth) {
        if (depth<1) {
            throw new IllegalArgumentException("Depth must be 1 or higher");
        }
        if (getChildren()==null) {
            return null;
        }
        Vector v=new Vector();
        getDescendantsByTagIdInternal(v, tagId,depth);
        return v;
    }

    /**
     *  Returns all descendants with the specified tag id
     *
     * @param tagId The tag ID to look for, one of the TAG_* constants (Not to be confused with the id attribute)
     * @return A vector containing descendants with the specified tag id
     */
    public Vector getDescendantsByTagId(int tagId) {
        return getDescendantsByTagId(tagId, DEPTH_INFINITE);
    }

    /**
     * Returns true if this element is the first non-text child of its parent
     * This is used internally for the :first-child pseudo class
     * 
     * @return true if this element is the first non-text child of its parent
     */
    boolean isFirstChild() {
        if ((HTMLComponent.PROCESS_HTML_MP1_ONLY) || (isTextElement())) { // :first-child is not supported in HTML-MP1
            return false;
        }
        HTMLElement parent = (HTMLElement)getParent();
        if (parent!=null) {
            Vector v=parent.getChildren();
            for(int i=0;i<v.size();i++) {
                HTMLElement elem = (HTMLElement)v.elementAt(i);
                if (elem==this) {
                    return true;
                }
                if (!elem.isTextElement()) {
                    return false;
                }
            }
        }
        return false;
    }

}

