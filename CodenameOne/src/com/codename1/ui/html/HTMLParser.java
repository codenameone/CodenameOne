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
import com.codename1.xml.XMLParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * The HTMLParser class is used to parse an XHTML-MP 1.0 document into a DOM object (Element).
 * Unsupported tags and attributes as well as comments are dropped in the parsing process.
 * The parser is also makes use of CSSParser for external CSS files, embedded CSS segments and CSS within the 'style' attribute.
 *
 * @author Ofir Leitner
 */
public class HTMLParser extends XMLParser {

    HTMLComponent htmlC; // The HTMLComponent that uses this Parser

    /**
     * The list of empty tags (tags that naturally don't have any children).
     * This is used to enable empty tags to be closed also in a non-strict way (i.e. &lt;br&gt; instead of &lt;br&gt/;)
     * some of these tags are not a part of the XHTML-MP 1.0 standard, but including them here allows a more smooth parsing if the document is not strictly XHTML-MP 1.0
     */
    private static String[] EMPTY_TAGS = {"br","link","meta","base","area","basefont","col","frame","hr","img","input","isindex","param"};

    /**
     * Constructs a new instance of HTMLParser
     */
    public HTMLParser() {
        // Add common char entities that are above the HTML 2.0 char entities range
        addCharEntity("bull", 8226);
        addCharEntity("euro", 8364);
        setIncludeWhitespacesBetweenTags(true);
    }

    /**
     * Pair this HTMLParser with the HTMLComponent that uses it.
     * This pairing is necessary to allow access to the htmlC in parseTagContent upon finding a CSS embedded segment
     * 
     * @param htmlC The HTMLComponent that uses this parser
     */
    void setHTMLComponent(HTMLComponent htmlC) {
        if ((htmlC!=null) && (this.htmlC!=null)) {
            throw new IllegalStateException("This HTMLParser is already paired with an HTMLComponent");
        }
        this.htmlC=htmlC;
    }
    
    /**
     * Overrides XMLParser.parseTagContent to enable embedded CSS segments (Style tags)
     * 
     * @param element The current parent element
     * @param is The reader containing the XML
     * @throws IOException if an I/O error in the stream is encountered
     */
    protected void parseTagContent(Element element, Reader is) throws IOException {
        if ((HTMLComponent.SUPPORT_CSS) && (htmlC.loadCSS) && (((HTMLElement)element).getTagId() == HTMLElement.TAG_STYLE)) { // We aren't strict and don't require text/css in a style tag // && "text/css".equals(element.getAttributeById(Element.ATTR_TYPE)))) {
            CSSElement addTo = CSSParser.getInstance().parseCSSSegment(is,null,htmlC,null);
            htmlC.addToEmebeddedCSS(addTo);
            return;
        }

        super.parseTagContent(element, is);
    }

    /**
     * Overrides XMLParser.createNewElement to return an HTMLElement instance
     * 
     * @param name The HTMLElement's name
     * @return a new instance of the names HTMLElement
     */
    protected Element createNewElement(String name) {
        HTMLElement elem=new HTMLElement(name);
        return elem;
    }

    /**
     * Overrides XMLParser.createNewTextElement to return an HTMLElement instance
     *
     * @param text The HTMLElement's text
     * @return a new instance of the HTMLElement
     */
    protected Element createNewTextElement(String text) {
        HTMLElement elem=new HTMLElement(text,true);
        return elem;
    }

    /**
     * Overrides XMLParser.convertCharEntity to add in HTML char entities
     *
     * @param charEntity The char entity to convert
     * @return A string containing a single char, or the original char entity string (with & and ;) if the char entity couldn't be resolved
     */
    protected String convertCharEntity(String charEntity) {
        try {
            return HTMLUtils.convertCharEntity(charEntity, true, null);
        } catch (IllegalArgumentException iae) {
            return super.convertCharEntity(charEntity);
        }
    }

    /**
     * This method translates between an HTML char entity string to the according char code.
     * It first tries to find it using its super method.
     * If not found, the search continues to a wider string array of char codes 160-255 which are supported in ISO-8859-1 / HTML 2.0
     *
     * @param symbol The symbol to lookup
     * @return The char code of the symbol, or -1 if none found
     *
    protected int getCharEntityCode(String symbol) {
        int val=super.getCharEntityCode(symbol);
        if (val==-1) {
            // Not one of the most popular char codes, proceed to check the ISO-8859-1 symbols array
            val=CSSElement.getStringVal(symbol, CHAR_ENTITY_STRINGS);
            if (val!=-1) {
                return val+160;
            }
        }
        return val;
    }*/



    /**
     * Checks whether the specified tag is an empty tag as defined in EMPTY_TAGS
     *
     * @param tagName The tag name to check
     * @return true if that tag is defined as an empty tag, false otherwise
     */
    protected boolean isEmptyTag(String tagName) {
        int i=0;
        boolean found=false;
        while ((i<EMPTY_TAGS.length) && (!found)) {
            if (tagName.equals(EMPTY_TAGS[i])) {
                found=true;
            }
            i++;
        }
        return found;
    }

    /**
     * A convenience method that casts the returned type of the parse method to HTMLElement.
     * Basically calling this method is simlar to calling parse and casting to HTMLElement.
     * 
     * @param isr The input stream containing the HTML
     * @return The HTML document
     */
    public HTMLElement parseHTML(InputStreamReader isr) {
        return (HTMLElement)super.parse(isr);
    }

    /**
     * {{@inheritDoc}}
     */
    protected String getSupportedStandardName() {
        return "XHTML-MP 1.0";
    }

    /**
     * Overrides the Element.isSupported to let the parser know which tags are supported in XHTML-MP 1.0
     *
     * @return true if the tag is a supported XHTML Mobile Profile 1.0 tag, false otherwise
     */
    protected boolean isSupported(Element element) {
        return (((HTMLElement)element).getTagId()!=HTMLElement.TAG_UNSUPPORTED);
    }

    /**
     * Overrides the Element.shouldEvaluate method to return false on the script tag.
     * The script tag should be skipped entirely, since it may contain characters like greater-than and lesser-than which may break the HTML
     * All other tags are evaluated (i.e. added including all their children to the tree), even if not supported (But of course their functionality is ignored by HTMLComponent)
     *
     * @return false if this is the SCRIPT tag, true otherwise
     */
    protected boolean shouldEvaluate(Element element) {
        return ((((HTMLElement)element).getTagId()!=HTMLElement.TAG_UNSUPPORTED) || (!element.getTagName().equalsIgnoreCase("script")));
    }



}
