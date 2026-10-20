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
package com.codename1.xml;

import com.codename1.io.Log;
import com.codename1.ui.html.HTMLUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

/// Parser class used to parse an XML document into a DOM object (Element). This code was originally
/// developed to parse HTML and as a result isn't as strict as most XML parsers and can parse many HTML documents
/// out of the box.  The parser is mostly stateful (although it does have an event callback API as well), its modeled closely
/// to the Java DOM API's.
///
/// In this sample an XML hierarchy is displayed using a `com.codename1.ui.tree.Tree`:
///
/// ```java
/// class XMLTreeModel implements TreeModel {
///     private Element root;
///     public XMLTreeModel(Element e) {
///         root = e;
///     }
///
///     public Vector getChildren(Object parent) {
///         if(parent == null) {
///             Vector c = new Vector();
///             c.addElement(root);
///             return c;
///         }
///         Vector result = new Vector();
///         Element e = (Element)parent;
///         for(int iter = 0 ; iter
public class XMLParser {
    private static final Element END_TAG = new Element();
    /// A constant containing the CDATA tag identifier (minus the C which is read anyway first)
    /// CDATA nodes will be converted to text nodes
    private static final String CDATA_STR = "DATA[";
    // The read-ahead buffer belongs to the parser, and both entry points empty
    // it. It used to be static and only parse() emptied it, so an eventParser
    // run that a callback ended early -- RSSService stops at its item limit by
    // throwing -- left the rest of that document in the buffer, and the next
    // eventParser call in the process read it before its own input: the next
    // feed came back with the previous feed's leftover items. A static buffer
    // was also shared by parsers running on different threads, and by a parse
    // started from inside another parser's callback.
    private char[] buffer;
    private int buffOffset;
    private int buffSize = -1;
    ParserCallback parserCallback;
    boolean includeWhitespacesBetweenTags; // For HTML white spaces between tags are significant to seperate words, in XML less so and it mostly creates garbage elements (Text with one space)
    private boolean eventParser;
    private boolean strict;
    private boolean stripNamespacePrefixes;
    // How many parseTagContent calls are active. 1 is document level: the
    // synthetic ROOT of parse(), or no element at all for eventParser.
    private int contentDepth;
    // Scratch buffers shared by every frame of the recursive descent. Each is
    // turned into a String before parseTag or parseTagContent recurses, so a
    // nested call always finds them free. They replace the three
    // StringBuilders parseTag allocated per tag and the one per text node.
    private final CharBuf nameBuf = new CharBuf();
    private final CharBuf attrBuf = new CharBuf();
    private final CharBuf valueBuf = new CharBuf();
    private final CharBuf textBuf = new CharBuf();
    private final CharBuf entityBuf = new CharBuf();
    // Names and short values repeat endlessly in a document (w:p, w:r, w:val
    // ...); the pool hands back the instance it already made instead of a new
    // String each time. Emptied by both entry points.
    private static final int POOL_SIZE = 1024;
    private static final int POOL_MAX_LEN = 32;
    private String[] pool;
    private char[][] poolChars;
    private int poolCount;

    /// The char entities strings supported in XML. When a char entity is found these will be compared against first.
    ///
    /// private static final String[] XML_CHAR_ENTITIES = {
    /// "lt", // lesser-than
    /// "gt", // greater-than
    /// "amp", // ampersand
    /// "quot", //quotation mark
    /// "apos", // apostrophe
    /// //"bull", //bullet
    /// //"euro" //euro
    /// };
    ///
    /// /**
    /// The numericals value of char entities strings above.
    ///
    /// private static final int[] XML_CHAR_ENTITIES_VALS = {
    /// 60, // "lt", // lesser-than
    /// 62, // "gt", // greater-than
    /// 38, // "amp", // ampersand
    /// 34, // "quot", //quotation mark
    /// 39, // "apos", // apostrophe
    /// //8226, // "bull", //bullet
    /// //8364 // "euro"}; //euro
    /// };
    private boolean caseSensitive;
    /// This hashtable contains user defined char entities
    private Hashtable userDefinedCharEntities;

    private int read(Reader is) throws IOException {
        int c = -1;
        if (buffSize < 0 || buffOffset >= buffSize) {
            if (buffer == null) {
                // A subclass may call parseTagContent without an entry point.
                buffer = new char[8192];
            }
            buffSize = is.read(buffer, 0, buffer.length);
            if (buffSize < 0) {
                return -1;
            }
            buffOffset = 0;
        }
        c = buffer[buffOffset];
        buffOffset++;

        return c;
    }

    private void resetBuffer() {
        buffOffset = 0;
        buffSize = -1;
        contentDepth = 0;
        if (pool != null) {
            for (int i = 0; i < POOL_SIZE; i++) {
                pool[i] = null;
                poolChars[i] = null;
            }
            poolCount = 0;
        }
    }

    /// A growable char array. Final, so every call on it is a direct call.
    private static final class CharBuf {
        char[] chars = new char[64];
        int len;

        void clear() {
            len = 0;
        }

        void add(char c) {
            if (len == chars.length) {
                grow(len + 1);
            }
            chars[len++] = c;
        }

        void add(char[] src, int off, int count) {
            if (len + count > chars.length) {
                grow(len + count);
            }
            System.arraycopy(src, off, chars, len, count);
            len += count;
        }

        void add(CharBuf other) {
            add(other.chars, 0, other.len);
        }

        void add(String s) {
            int count = s.length();
            if (len + count > chars.length) {
                grow(len + count);
            }
            for (int i = 0; i < count; i++) {
                chars[len++] = s.charAt(i);
            }
        }

        private void grow(int min) {
            int n = chars.length * 2;
            if (n < min) {
                n = min;
            }
            char[] a = new char[n];
            System.arraycopy(chars, 0, a, 0, len);
            chars = a;
        }

        @Override
        public String toString() {
            return new String(chars, 0, len);
        }
    }

    /// Whitespace as the parser sees it. Strict mode never consults the
    /// overridable isWhiteSpace, so the check is a direct call ParparVM can
    /// keep cheap.
    private boolean ws(char ch) {
        if (strict) {
            return ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r';
        }
        return isWhiteSpace(ch);
    }

    /// Returns a pooled String for chars[off, off+len).
    private String pooled(char[] chars, int off, int len) {
        if (len > POOL_MAX_LEN) {
            return new String(chars, off, len);
        }
        int h = 0;
        for (int i = 0; i < len; i++) {
            h = 31 * h + chars[off + i];
        }
        if (pool == null) {
            pool = new String[POOL_SIZE];
            poolChars = new char[POOL_SIZE][];
        }
        int mask = POOL_SIZE - 1;
        int idx = (h ^ (h >>> 16)) & mask;
        for (int probe = 0; probe < 8; probe++) {
            char[] p = poolChars[idx];
            if (p == null) {
                String s = new String(chars, off, len);
                if (poolCount < POOL_SIZE * 3 / 4) {
                    char[] copy = new char[len];
                    System.arraycopy(chars, off, copy, 0, len);
                    poolChars[idx] = copy;
                    pool[idx] = s;
                    poolCount++;
                }
                return s;
            }
            if (p.length == len) {
                int i = 0;
                while (i < len && p[i] == chars[off + i]) {
                    i++;
                }
                if (i == len) {
                    return pool[idx];
                }
            }
            idx = (idx + 1) & mask;
        }
        return new String(chars, off, len);
    }

    private String name(CharBuf b) {
        int off = 0;
        if (stripNamespacePrefixes) {
            for (int i = b.len - 1; i >= 0; i--) {
                if (b.chars[i] == ':') {
                    off = i + 1;
                    break;
                }
            }
        }
        return pooled(b.chars, off, b.len - off);
    }

    private String attributeName(CharBuf b) {
        // xmlns:w names the namespace w itself; without its prefix it would
        // collide with the default namespace declaration
        if (stripNamespacePrefixes && b.len > 5 && b.chars[0] == 'x' && b.chars[1] == 'm'
                && b.chars[2] == 'l' && b.chars[3] == 'n' && b.chars[4] == 's' && b.chars[5] == ':') {
            return pooled(b.chars, 0, b.len);
        }
        return name(b);
    }

    private String value(CharBuf b) {
        return pooled(b.chars, 0, b.len);
    }

    /// Trims unneeded & and ; from the symbol if exist
    ///
    /// #### Parameters
    ///
    /// - `symbol`: The char entity symbol
    ///
    /// #### Returns
    ///
    /// A trimmed char entity without & and ;
    private static String trimCharEntity(String symbol) {
        int start = 0;
        int end = symbol.length();
        if (symbol.charAt(0) == '&') {
            start = 1;
        }
        if (symbol.charAt(end - 1) == ';') {
            end--;
        }
        return symbol.substring(start, end);
    }

    /// Returns a string identifying the document type this parser supports.
    /// This should be overriden by subclassing parsers.
    ///
    /// #### Returns
    ///
    /// a string identifying the document type this parser supports.
    protected String getSupportedStandardName() {
        return "XML";
    }

    /// Adds the given symbol and code to the user defined char entities table
    /// [http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references](http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references)
    ///
    /// #### Parameters
    ///
    /// - `symbol`: The symbol to add
    ///
    /// - `code`: The symbol's code
    public void addCharEntity(String symbol, int code) {
        if (userDefinedCharEntities == null) {
            userDefinedCharEntities = new Hashtable();
        }
        userDefinedCharEntities.put(trimCharEntity(symbol), Integer.valueOf(code));
    }

    /// Adds the given symbols array  to the user defined char entities table with the startcode provided as the code of the first string, startcode+1 for the second etc.
    /// Some strings in the symbols array may be null thus skipping code numbers.
    ///
    /// #### Parameters
    ///
    /// - `symbols`: The symbols to add
    ///
    /// - `startcode`: The symbol's code
    public void addCharEntitiesRange(String[] symbols, int startcode) {
        if (userDefinedCharEntities == null) {
            userDefinedCharEntities = new Hashtable();
        }
        int slen = symbols.length;
        for (int i = 0; i < slen; i++) {
            if (symbols[i] != null) {
                userDefinedCharEntities.put(trimCharEntity(symbols[i]), Integer.valueOf(startcode + i));
            }
        }
    }


    /// This method translates between a XML char entity string to the according char code.
    /// The string is first compared to the 5 XML supported strings: quot,apos,amp,lt and gt.
    /// If still not found it goes to look in the user defined char entities hashtable
    ///
    /// #### Parameters
    ///
    /// - `symbol`: The symbol to lookup
    ///
    /// #### Returns
    ///
    /// @return The char code of the symbol, or -1 if none found
    ///
    ///     protected int getCharEntityCode(String symbol,Hashtable userDefined) {
    ///     // First tries the XML basic char entities
    ///     int val=-1;
    ///     for (int i=0;i<XML_CHAR_ENTITIES.length;i++) {
    ///     if (symbol.equalsIgnoreCase(XML_CHAR_ENTITIES[i])) {
    ///     return XML_CHAR_ENTITIES_VALS[i];
    ///     }
    ///     }
    ///     if (val!=-1) {
    ///     return val;
    ///     } else {
    ///
    ///     // Not found in the standard symbol table, see if it is in the user defined symbols table
    ///     if (userDefined!=null) {
    ///     Object charObj=userDefined.get(symbol);
    ///     if (charObj!=null) {
    ///     return ((Integer)charObj).intValue();
    ///     }
    ///     }
    ///
    ///     // Not found anywhere
    ///     return -1;
    ///     }
    ///     }

    /// Converts a char entity to the matching character.
    /// This handles both numbered and symbol char entities (The latter is done via getCharEntityCode)
    ///
    /// #### Parameters
    ///
    /// - `charEntity`: The char entity to convert
    ///
    /// #### Returns
    ///
    /// A string containing a single char, or the original char entity string (with & and ;) if the char entity couldn't be resolved
    protected String convertCharEntity(String charEntity) {
        try {
            return HTMLUtils.convertCharEntity(charEntity, false, userDefinedCharEntities);
        } catch (IllegalArgumentException iae) {
            notifyError(ParserCallback.ERROR_UNRECOGNIZED_CHAR_ENTITY, null, null, null, "Unrecognized char entity: " + charEntity);
            // Another option is to return an empty string, but returning the entity will unravel bugs and will also allow ignoring common mistakes such as using the & char (instead of &apos;)
            return '&' + charEntity + ';';
        }

        /*int charCode=-1;
        if (charEntity.startsWith("#")) { //numbered char entity
            if (charEntity.startsWith("#x")) { //hex
                try {
                    charCode=Integer.parseInt(charEntity.substring(2),16);
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            } else {
                try {
                    charCode=Integer.parseInt(charEntity.substring(1));
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            }
        } else { //not numbered, rather a symbol
            charCode=getCharEntityCode(charEntity,userDefinedCharEntities);
        }

        if (charCode!=-1) {
            return ""+(char)charCode;
        } else {
            notifyError(ParserCallback.ERROR_UNRECOGNIZED_CHAR_ENTITY,null,null,null, "Unrecognized char entity: "+charEntity);
            return "&"+charEntity+";"; // Another option is to return an empty string, but returning the entity will unravel bugs and will also allow ignoring common mistakes such as using the & char (instead of &apos;)
        }*/

    }


    /// This is the entry point for parsing a document and the only non-private member method in this class
    ///
    /// #### Parameters
    ///
    /// - `is`: The InputStream containing the XML
    ///
    /// #### Returns
    ///
    /// an Element object describing the parsed document (Basically its DOM)
    public Element parse(Reader is) {
        resetBuffer();
        eventParser = false;
        Element rootElement = createNewElement("ROOT"); // ROOT is a "dummy" element that all other document elements are added to
        try {
            parseTagContent(rootElement, is);
        } catch (IOException ioe) {
            Log.e(ioe);
        }
        if (rootElement.getNumChildren() == 0) {
            notifyError(ParserCallback.ERROR_NO_ROOTS, null, null, null, "XML document contains no root element.");
            return null;
        } else if (rootElement.getNumChildren() > 1) {
            StringBuilder roots = new StringBuilder();
            for (int i = 1; i < rootElement.getNumChildren(); i++) {
                Element elem = rootElement.getChildAt(i);
                if (elem.isTextElement()) {
                    roots.append("Text (").append(elem.getText()).append("),");
                } else {
                    roots.append(elem.getTagName()).append(",");
                }
            }
            if (roots.length() > 0 && roots.charAt(roots.length() - 1) == ',') {
                roots.setLength(roots.length() - 1);
            }

            Element firstRoot = rootElement.getChildAt(0);
            String str = null;
            if (firstRoot.isTextElement()) {
                str = "TEXT:" + firstRoot.getText();
            } else {
                str = firstRoot.getTagName();
            }
            notifyError(ParserCallback.ERROR_MULTIPLE_ROOTS, null, null, null, "XML document contains multiple root elements, only the first root (" + str + ") will be used. Excessive roots: " + roots);
        }
        rootElement = rootElement.getChildAt(0);
        return rootElement;
    }

    /// Creates a new element. This should be overriden by parsers that use a subclass of Element.
    ///
    /// #### Parameters
    ///
    /// - `name`: The new element's name
    ///
    /// #### Returns
    ///
    /// a new instance of the element
    protected Element createNewElement(String name) {
        Element e = new Element(name);
        e.caseSensitive = caseSensitive || strict;
        e.exactTagNames = strict;
        return e;
    }

    /// Creates a new text element. This should be overriden by parsers that use a subclass of Element.
    ///
    /// #### Parameters
    ///
    /// - `text`: The new element's text
    ///
    /// #### Returns
    ///
    /// a new instance of the element
    protected Element createNewTextElement(String text) {
        Element e = new Element(text, true);
        e.caseSensitive = caseSensitive || strict;
        e.exactTagNames = strict;
        return e;
    }

    public void setIncludeWhitespacesBetweenTags(boolean include) {
        includeWhitespacesBetweenTags = include;
    }

    /// The event parser requires deriving this class and overriding callback
    /// methods to work effectively. To stop the event parser in mid way a
    /// callback can simply throw an IOException on purpose.
    ///
    /// #### Parameters
    ///
    /// - `r`: the reader from which the data should be parsed
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if an exception is thrown by the reader
    public void eventParser(Reader r) throws IOException {
        resetBuffer();
        eventParser = true;
        parseTagContent(null, r);
    }

    /// Invoked when the event parser encounters a text element.
    /// This callback method is invoked only on the eventParser.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text encountered
    protected void textElement(String text) {
    }

    /// Invoked when a tag is opened, this method should return true to process
    /// the tag or return false to skip the tag.
    /// This callback method is invoked only on the eventParser.
    ///
    /// #### Parameters
    ///
    /// - `tag`: the tag name
    ///
    /// #### Returns
    ///
    /// true to process the tag, false to skip the tag
    protected boolean startTag(String tag) {
        return true;
    }

    /// Invoked when a tag ends
    /// This callback method is invoked only on the eventParser.
    ///
    /// #### Parameters
    ///
    /// - `tag`: the tag name
    protected void endTag(String tag) {
    }

    /// Invoked for every attribute value of the givne tag
    /// This callback method is invoked only on the eventParser.
    ///
    /// #### Parameters
    ///
    /// - `tag`: the tag name
    protected void attribute(String tag, String attributeName, String value) {
    }


    /// Checks if this character is a legal character for char entities
    ///
    /// #### Parameters
    ///
    /// - `c`: The character to check
    ///
    /// #### Returns
    ///
    /// true if legal, false otherwise
    private boolean isLegalCharEntityCharacter(char c) {
        return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || (c == '#'));
    }

    /// Parses tags content, accumulating text and child elements .
    /// Upon bumping a start tag character it calls the parseTag method.
    /// This method is called at first from the parse method, and later on from parseTag (which creates the recursion).
    ///
    /// #### Parameters
    ///
    /// - `element`: The current parent element
    ///
    /// - `is`: The InputStream containing the XML
    ///
    /// #### Throws
    ///
    /// - `IOException`: if an I/O error in the stream is encountered
    protected void parseTagContent(Element element, Reader is) throws IOException {
        contentDepth++;
        try {
            parseTagContentImpl(element, is);
        } finally {
            contentDepth--;
        }
    }

    private void parseTagContentImpl(Element element, Reader is) throws IOException {
        // The text and entity buffers belong to the parser, not to this frame:
        // both are flushed before parseTag recurses, so a nested call always
        // finds them empty.
        CharBuf text = textBuf;
        CharBuf charEntity = entityBuf;
        boolean inText = false;
        boolean inEntity = false;
        boolean leadingSpace = false;
        // Whitespace around the root element (after <?xml ...?>, a trailing
        // newline) is not content; kept, it would become a second root.
        final boolean keepAllText = strict && contentDepth > 1;
        int val = read(is);
        char c = (char) val;

        while (val != -1) {
            if (c == '<') {
                if ((includeWhitespacesBetweenTags) && (leadingSpace) && (!inText) && (element != null) && (element.getNumChildren() > 0)) {
                    leadingSpace = false;
                    text.clear();
                    text.add(' ');
                    inText = true;
                }

                if (inText) {
                    // Mistakenly "collected" something that is not a char entity, perhaps
                    // misuse of the & character (instead of using &apos;)
                    if (inEntity) {
                        text.add('&');
                        text.add(charEntity);
                        inEntity = false;
                    }
                    String str = text.toString();
                    if (element != null) {
                        Element textElement = createNewTextElement(str);
                        element.addChild(textElement);
                    } else {
                        textElement(str);
                    }
                    inText = false;
                    leadingSpace = false;
                }


                Element childElement = parseTag(is);
                if (childElement == END_TAG) {
                    //was actually an ending tag
                    CharBuf closingTag = nameBuf;
                    closingTag.clear();
                    int i = read(is);

                    // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                    if (i == -1) {
                        childElement = END_TAG;
                    } else {
                        c = (char) i;
                        while ((c != '>')) {
                            closingTag.add(c);
                            i = read(is);
                            c = (char) i;
                            if (i == -1) {
                                childElement = END_TAG;
                                break;
                            }
                        }
                    }

                    String ct = name(closingTag);
                    if (eventParser) {
                        endTag(ct);
                        if (!isEmptyTag(ct)) {
                            // patch from http://code.google.com/p/codenameone/issues/detail?id=428
                            // not really sure if this is the best approach but if it solves a bug...
                            return;
                        }
                    }

                    if (element != null) {
                        String tagName = element.getTagName();
                        if (strict ? ct.equals(tagName) : ct.equalsIgnoreCase(tagName)) {
                            return;
                        } else if (!isEmptyTag(ct)) {
                            notifyError(ParserCallback.ERROR_NO_CLOSE_TAG, tagName, null, null, "Malformed XML - no appropriate closing tag for " + tagName);
                        }
                    }
                } else {
                    if (element != null && !childElement.isComment) {
                        element.addChild(childElement);
                    }
                }
            } else if (inText) {
                if (inEntity) {
                    if (c == ';') { //end
                        text.add(convertCharEntity(charEntity.toString()));
                        inEntity = false;
                    } else if (isLegalCharEntityCharacter(c)) {
                        charEntity.add(c);
                    } else {
                        text.add('&');
                        text.add(charEntity);
                        text.add(c);
                        inEntity = false;
                    }
                } else if (c == '&') { //start char entity
                    charEntity.clear();
                    inEntity = true;
                } else {
                    text.add(c);
                    // Plain text runs to the next markup or entity; copy the
                    // rest of it out of the read buffer in one go rather than
                    // a character at a time.
                    char[] b = buffer;
                    int start = buffOffset;
                    int end = start;
                    int lim = buffSize;
                    while (end < lim) {
                        char d = b[end];
                        if (d == '<' || d == '&') {
                            break;
                        }
                        end++;
                    }
                    if (end > start) {
                        text.add(b, start, end - start);
                        buffOffset = end;
                    }
                }
            } else if (keepAllText || !isWhiteSpace(c)) {
                text.clear();
                inText = true;
                if (leadingSpace) {
                    text.add(' ');
                    leadingSpace = false;
                }
                if (c == '&') { //text starts with a character entity (i.e. &nbsp;)
                    // The & is not included in the string we accumulate
                    charEntity.clear();
                    inEntity = true;
                } else {
                    // re-dispatch through the plain text branch so the bulk copy applies
                    continue;
                }
            } else { // leading space is relevant also for newline and other whitespaces //if (c==' ') {
                leadingSpace = true;
            }
            val = read(is);
            c = (char) val;
        }
    }

    /// Checks if the specified character is a white space or not.
    /// Exposed to packaage since used by HTMLComponent as well
    ///
    /// #### Parameters
    ///
    /// - `ch`: The character to check
    ///
    /// #### Returns
    ///
    /// true if the character is a white space, false otherwise
    protected boolean isWhiteSpace(char ch) {
        return ((ch == ' ') || (ch == '\n') || (ch == '\t') || (ch == '\r'));
    }

    /// This method collects the tag name and all of its attributes.
    /// For comments and XML declarations this will call the parseCommentOrXMLDeclaration method.
    /// Note that this method returns an Element with a name and attrbutes, but not its content/children which will be done by parseTagContent
    ///
    /// #### Parameters
    ///
    /// - `is`: The InputStream containing the XML
    ///
    /// #### Returns
    ///
    /// The parsed element
    ///
    /// #### Throws
    ///
    /// - `IOException`: if an I/O error in the stream is encountered
    protected Element parseTag(Reader is) throws IOException {
        CharBuf tagName = nameBuf;
        CharBuf curAttribute = attrBuf;
        CharBuf curValue = valueBuf;
        tagName.clear();
        curValue.clear();
        final boolean fold = !eventParser && !caseSensitive && !strict;
        //boolean procInst=false; // Support for the styleshhet processing instruction was removed, as it is not supported in most browsers, and it causes problems by adding tags before the HTML element (Makign the document with multiple roots)

        char c = (char) read(is);
        if (c == '/') {
            return END_TAG; //end tag
        } else if (c == '!') {
            c = (char) read(is);
            char c2 = (char) read(is);
            if ((c == '-') && (c2 == '-')) { //comment
                return parseCommentOrXMLDeclaration(is, "-->");
            } else if ((c == '[') && (c2 == 'C')) { // CDATA?
                c = (char) read(is);
                int idx = 0;
                int clen = CDATA_STR.length();
                while ((idx < clen) && (c == CDATA_STR.charAt(idx))) {
                    idx++;
                    if (idx < clen) {
                        c = (char) read(is);
                    }
                }
                if (idx == clen) { //found CDATA
                    return parseCommentOrXMLDeclaration(is, "]]>"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
                } else { // some other unknown tag
                    return parseCommentOrXMLDeclaration(is, ">"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
                }

            } else {
                return parseCommentOrXMLDeclaration(is, ">"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
            }
        } else if (c == '?') {
            return parseCommentOrXMLDeclaration(is, ">"); //parse XML declaration i.e. <?xml version="1.0" encoding="ISO-8859-1"?> as comments as well - i.e. ignore them
        }

        //read and ignore any whitespaces before tag name
        while (ws(c)) {
            c = (char) read(is);
        }

        //collect tag name
        while ((!ws(c)) && (c != '>') && (c != '/')) {
            if (fold) {
                c = Character.toLowerCase(c);
            }
            tagName.add(c);
            int i = read(is);

            // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
            if (i == -1) {
                return END_TAG;
            }
            c = (char) i;
        }

        //read and ignore any whitespaces after tag name
        while (ws(c)) {
            c = (char) read(is);
        }

        boolean processTag = true;
        String tn = name(tagName);
        if (eventParser) {
            processTag = startTag(tn);
        }
        Element element = null;
        if (!eventParser) {
            element = createNewElement(tn);
        }

        if (!processTag || !isSupported(element)) {
            notifyError(ParserCallback.ERROR_TAG_NOT_SUPPORTED, tn, null, null, "The tag '" + tn + "' is not supported in " + getSupportedStandardName());
            if (!processTag || !shouldEvaluate(element)) {
                // If tag is not supported we skip it all till the closing tag.
                // This is especially important for the script tag which may contain '<' and '>' which might confuse the parser
                char lastChar = c;
                while (c != '>') { // Read till the end of the tag
                    lastChar = c;
                    int i = read(is);

                    // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                    if (i == -1) {
                        return END_TAG;
                    }
                    c = (char) i;
                }
                if (lastChar != '/') { // If this is an empty tag, no need to search for its closing tag as there's none...
                    // the name as read, before any namespace prefix was stripped
                    String endTag = new StringBuilder().append('<').append('/').append(tagName.toString()).append('>').toString();
                    int index = 0;
                    int elen = endTag.length();
                    while (index < elen) {
                        int i = read(is);

                        // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                        if (i == -1) {
                            return END_TAG;
                        }
                        c = (char) i;

                        if (!strict) {
                            c = Character.toLowerCase(c);
                        }
                        if (c == endTag.charAt(index)) {
                            index++;
                        } else {
                            index = 0;
                        }
                    }
                }

                return element;
            }
        }

        if (c == '>') { //tag declartion ended, process content
            if (!isEmptyTag(tn)) {
                parseTagContent(element, is);
            }
            return element;
        } else if (c == '/') { //closed tag - no content
            c = (char) read(is);
            if (c == '>') {
                if (eventParser) {
                    endTag(tn);
                }
                return element;
            } else {
                notifyError(ParserCallback.ERROR_UNEXPECTED_CHARACTER, tn, null, null, "XML malformed - no > after /");
            }
        }


        while (true) {
            curAttribute.clear();
            curAttribute.add(c);
            c = (char) read(is);
            while ((!ws(c)) && (c != '=') && (c != '>')) {
                if (fold) {
                    c = Character.toLowerCase(c);
                }
                curAttribute.add(c);
                c = (char) read(is);
            }

            if (c == '>') { // tag close char shouldn't be found here, but if the XML is slightly malformed we return the element
                notifyError(ParserCallback.ERROR_UNEXPECTED_TAG_CLOSING, tn, curAttribute.toString(), null, "Unexpected tag closing in tag " + tn + ", attribute=" + curAttribute);
                if (!isEmptyTag(tn)) {
                    parseTagContent(element, is);
                }
                return element;
            }

            //read and ignore any whitespaces after attribute name
            while (ws(c)) {
                c = (char) read(is);
            }

            if (c != '=') {
                notifyError(ParserCallback.ERROR_UNEXPECTED_CHARACTER, tn, curAttribute.toString(), null, "Unexpected character " + c + ", expected '=' after attribute " + curAttribute + " in tag " + tn);
                if (c == '>') { // tag close char shouldn't be found here, but if the XML is slightly malformed we return the element
                    if (!isEmptyTag(tn)) {
                        parseTagContent(element, is);
                    }
                    return element;
                }


                continue; //if attribute is not followed by = then process the next attribute
            }

            c = (char) read(is);
            //read and ignore any whitespaces before attribute value
            while (ws(c)) {
                c = (char) read(is);
            }

            char quote = ' ';


            if ((c == '"') || (c == '\'')) {
                quote = c;
            } else {
                curValue.add(c);
            }

            CharBuf charEntity = entityBuf;
            boolean inEntity = false;
            boolean ended = false;
            while (!ended) {
                int i = read(is);
                if (i < 0) {
                    return END_TAG;
                }
                c = (char) i;
                if (c == quote) {
                    ended = true;
                    i = read(is);
                    if (i < 0) {
                        return END_TAG;
                    }
                    c = (char) i;
                } else if ((quote == ' ') && ((c == '/') || (c == '>') || (ws(c)))) {
                    ended = true;
                } else if (c == '&') {
                    if (inEntity) {
                        curValue.add('&'); // Wasn't a char entit, probably a url as a parameter : i.e. param="/test?p=val&pw=val2&p3=val3
                        curValue.add(charEntity);
                    }
                    charEntity.clear();
                    inEntity = true;
                } else {
                    if (inEntity) {
                        if (c == ';') {
                            curValue.add(convertCharEntity(charEntity.toString()));
                            inEntity = false;
                        } else if (isLegalCharEntityCharacter(c)) {
                            charEntity.add(c);
                        } else {
                            curValue.add('&');
                            curValue.add(charEntity);
                            curValue.add(c);
                            inEntity = false;
                        }
                    } else {
                        curValue.add(c);
                    }
                }
            }

            if (inEntity) { // Mistaken something else for a char entity - for example an action which is action="http://domain/test.html?param1=val1&param2=val2"
                curValue.add('&');
                curValue.add(charEntity);
            }

            String an = attributeName(curAttribute);
            String av = value(curValue);
            if (eventParser) {
                attribute(tn, an, av);
            } else {
                int error = element.setAttribute(an, av);

                if (error == ParserCallback.ERROR_ATTRIBUTE_NOT_SUPPORTED) {
                    notifyError(error, tn, an, av, "Attribute '" + an + "' is not supported for tag '" + tn + "'.");
                } else if (error == ParserCallback.ERROR_ATTIBUTE_VALUE_INVALID) {
                    notifyError(error, tn, an, av, "Attribute '" + an + "' in tag '" + tn + "' has an invalid value (" + av + ")");
                }
            }

            //read and ignore any whitespaces after attribute/value pair
            while (ws(c)) {
                int i = read(is);
                if (i < 0) {
                    return END_TAG;
                }
                c = (char) i;
            }

            if (c == '>') { //tag declartion ended, process content
                if (!isEmptyTag(tn)) {
                    parseTagContent(element, is);
                }
                return element;
            } else if (c == '/') { //closed tag - no content
                int i = read(is);
                if (i < 0) {
                    return END_TAG;
                }
                c = (char) i;
                if (c == '>') {
                    // Solves the case of <a p="s"/> endTag("a") would not be called related to
                    // http://code.google.com/p/codenameone/issues/detail?id=896
                    if (eventParser) {
                        endTag(tn);
                    }
                    return element;
                } else {
                    notifyError(ParserCallback.ERROR_UNEXPECTED_CHARACTER, tn, an, av, "XML malformed - no > after /");
                }
            }
            curValue.clear();
        }

    }

    /// This utility method is used to parse comments and XML declarations in the XML.
    /// The comment/declaration is returned as an Element, but is flagged as a comment since both comments and XML declarations are not part of the XML DOM.
    /// This method can be overridden to process specific XML declarations
    ///
    /// #### Parameters
    ///
    /// - `is`: The inputstream
    ///
    /// - `endTag`: The endtag to look for
    ///
    /// #### Returns
    ///
    /// An Element representing the comment or XML declartaion
    ///
    /// #### Throws
    ///
    /// - `IOException`
    protected Element parseCommentOrXMLDeclaration(Reader is, String endTag) throws IOException {
        char[] endTagChars = endTag.toCharArray();
        int endTagPos = 0;
        StringBuilder text = new StringBuilder();
        boolean ended = false;
        while (!ended) {
            int in = read(is);
            if (in == -1) {
                // input stream ended abruptly
                break;
            }
            char c = (char) in;
            if (c == endTagChars[endTagPos]) {
                endTagPos++;
                if (endTagPos == endTagChars.length) {
                    ended = true;
                }
            } else {
                if (endTagPos != 0) { //add - or -- if it wasn't an end tag eventually
                    // with cdata it might end with ]]> but we might have a string such as ]]]>
                    // which is valid and the first ] is just a part of the string... So we need to check
                    // for that special case http://code.google.com/p/codenameone/issues/detail?id=865
                    if (c == endTagChars[0] && endTagPos > 1) {
                        text.append(c);
                        continue;
                    }

                    text.append(endTagChars, 0, endTagPos);
                    endTagPos = 0;
                }
                text.append(c);
            }
        }

        String elementName = null;
        if ("-->".equals(endTag)) {
            elementName = "comment";
        } else if (">".equals(endTag)) {
            elementName = "XML declaration";
        } else { //CDATA
            if (eventParser) {
                textElement(text.toString());
                return null;
            }
            return createNewTextElement(text.toString());
        }

        if (eventParser) {
            return null;
        }
        Element comment = createNewElement(elementName);
        comment.setAttribute("content", text.toString());
        comment.isComment = true;
        return comment;
    }

    /// Checks whether the specified tag is an empty tag
    ///
    /// #### Parameters
    ///
    /// - `tagName`: The tag name to check
    ///
    /// #### Returns
    ///
    /// true if that tag is defined as an empty tag, false otherwise
    protected boolean isEmptyTag(String tagName) {
        return false;
    }

    /// A utility method used to notify an error to the ParserCallback and throw an IllegalArgumentException if parsingError returned false
    ///
    /// #### Parameters
    ///
    /// - `errorId`: The error ID, one of the ERROR_* constants in ParserCallback
    ///
    /// - `tag`: The tag in which the error occured (Can be null for non-tag related errors)
    ///
    /// - `attribute`: The attribute in which the error occured (Can be null for non-attribute related errors)
    ///
    /// - `value`: The value in which the error occured (Can be null for non-value related errors)
    ///
    /// - `description`: A verbal description of the error
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: If the parser callback returned false on this error
    protected void notifyError(int errorId, String tag, String attribute, String value, String description) {
        if (parserCallback != null) {
            boolean cont = parserCallback.parsingError(errorId, tag, attribute, value, description);
            if (!cont) {
                throw new IllegalArgumentException(description);
            }
        }
    }

    /// Returns true if this element is supported, false otherwise
    /// In XMLParser this always returns true, but subclasses can determine if an element is supported in their context according to its name etc.
    /// Unsupported elements will be skipped by the parser and excluded from the resulting DOM object
    ///
    /// #### Parameters
    ///
    /// - `element`: The element to check
    ///
    /// #### Returns
    ///
    /// true if the element is supported, false otherwise
    protected boolean isSupported(Element element) {
        return true;
    }

    /// Checks if this element should be evaluated by the parser
    /// This can be overriden by subclasses to skip certain elements
    ///
    /// #### Parameters
    ///
    /// - `element`: The element to check
    ///
    /// #### Returns
    ///
    /// true if this element should be evaluated by the parser, false to skip it completely
    protected boolean shouldEvaluate(Element element) {
        return true;
    }


    /// Sets the specified callback to serve as the callback for parsing errors
    ///
    /// #### Parameters
    ///
    /// - `parserCallback`: The callback to use for parsing errors
    public void setParserCallback(ParserCallback parserCallback) {
        this.parserCallback = parserCallback;
    }

    /// Sets the parser to be case sensitive and retain case, otherwise it will convert all data to lower case
    ///
    /// #### Returns
    ///
    /// the caseSensitive
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /// Sets the parser to be case sensitive and retain case, otherwise it will convert all data to lower case
    ///
    /// #### Parameters
    ///
    /// - `caseSensitive`: the caseSensitive to set
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /// Turns strict XML parsing on or off (off by default). The default mode
    /// keeps the lenient, HTML-flavoured behaviour existing code relies on.
    /// Strict mode is for well-formed XML such as OOXML, and differs in that:
    ///
    /// - names keep their case and are matched exactly, both by the parser
    ///   (closing tags) and by `Element.getAttribute`, as if
    ///   `setCaseSensitive(true)` were set;
    ///
    /// - text is kept exactly as written: leading whitespace is not collapsed
    ///   and whitespace-only text between tags is kept as a text element, so
    ///   `<w:t xml:space="preserve"> </w:t>` keeps its space;
    ///
    /// - whitespace is the four XML whitespace characters and `isWhiteSpace`
    ///   is not consulted.
    ///
    /// #### Parameters
    ///
    /// - `strict`: true for strict XML parsing
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /// Returns true if strict XML parsing is on, see `setStrict(boolean)`.
    ///
    /// #### Returns
    ///
    /// true for strict XML parsing
    public boolean isStrict() {
        return strict;
    }

    /// When on, the namespace prefix is removed from tag and attribute names as
    /// they are read, so `w:p` is reported as `p` and `w:val` as `val`. Off by
    /// default because it discards information: `w:t` and `m:t` both become
    /// `t`. Namespace declarations (`xmlns:w`) keep their name. Works in
    /// either mode.
    ///
    /// #### Parameters
    ///
    /// - `strip`: true to remove namespace prefixes from names
    public void setStripNamespacePrefixes(boolean strip) {
        this.stripNamespacePrefixes = strip;
    }

    /// Returns true if namespace prefixes are removed from names, see
    /// `setStripNamespacePrefixes(boolean)`.
    ///
    /// #### Returns
    ///
    /// true if namespace prefixes are removed
    public boolean isStripNamespacePrefixes() {
        return stripNamespacePrefixes;
    }

}
