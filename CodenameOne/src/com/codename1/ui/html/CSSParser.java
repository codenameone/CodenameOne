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

import com.codename1.xml.ParserCallback;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A parser for CSS segments or files.
 * Note that this class is not derived from XMLParser or HTMLParser as CSS format is significantly different than XML/HTML
 *
 * @author Ofir Leitner
 */
class CSSParser {

   /**
    * The supported CSS media types, this is relevant for CSS at-rules (i.e. @import and @media)
    * The default values according to the WCSS specs the default one is "handheld" and "all" (Which is always accepted)
    */
   private static String[] SUPPORTED_MEDIA_TYPES = {"all","handheld"};

   private static CSSParser instance;

   private CSSParserCallback parserCallback;
   
    /**
     * Returns or creates the Parser's single instance
     *
     * @return the Parser's instance
     */
    static CSSParser getInstance() {
        if (instance==null) {
            instance=new CSSParser();
        }
        return instance;
    }

    /**
     * Sets the supported CSS media types to the given strings.
     * Usually the default media types ("all","handheld") should be suitable, but in case this runs on a device that matches another profile, the developer can specify it here.
     *
     * @param supportedMediaTypes A string array containing the media types that should be supported
     */
    static void setCSSSupportedMediaTypes(String[] supportedMediaTypes) {
        SUPPORTED_MEDIA_TYPES=supportedMediaTypes;
    }


    // ***********
    // CSS Parsing methods from here onward
    // ***********

    /**
     * Checks if the specified character is a white space or not.
     * Exposed to packaage since used by HTMLComponent as well
     *
     * @param ch The character to check
     * @return true if the character is a white space, false otherwise
     */
    static boolean isWhiteSpace(char ch) {
        return ((ch==' ') || (ch=='\n') || (ch=='\t') || (ch==10) || (ch==13));
    }


    /**
     * Handles a CSS comment segment
     *
     * @param r The stream reader
     * @return The next char after the comment
     * @throws IOException
     */
    private char handleCSSComment(ExtInputStreamReader r) throws IOException {
        char c= r.readCharFromReader();
        if (c=='*') {
            char lastC='\0';
            while ((c!='/') || (lastC!='*')) {
                lastC=c;
                c= r.readCharFromReader();
            }
            c= r.readCharFromReader();
            while(((byte)c) != -1 && isWhiteSpace(c)) { //skip white spaces
                c= r.readCharFromReader();
            }
        } else {
            r.unreadChar(c);
            return '/';
        }
        return c;
    }

    /**
     * Reads the next CSS token from the reader
     *
     * @param r The stream reader
     * @param readNewline true to read new lines and not break when they're found, false otherwise
     * @param ignoreCommas true to ignore commas and not break when they're found, false otherwise
     * @param ignoreColons true to ignore colons and not break when they're found, false otherwise
     * @param ignoreWhiteSpaces true to ignore white spaces and not break when they're found, false otherwise
     * @return The next CSS token
     * @throws IOException
     */
    private String nextToken(ExtInputStreamReader r, boolean readNewline,boolean ignoreCommas,boolean ignoreColons,boolean ignoreWhiteSpaces) throws IOException {
        boolean newline = false;
        StringBuilder currentToken = new StringBuilder();
        char c= r.readCharFromReader();

        // read the next token from the CSS stream
        while(((byte)c) != -1 && isWhiteSpace(c)) {
            newline = newline || (c == 10 || c == 13 || c == ';' || ((c == ',') && (!ignoreCommas)) || (c == '>') || (c == '+'));
            if(!readNewline && newline) {
                return null;
            }
            c= r.readCharFromReader();
        }
        if (c==';' && readNewline) { //leftover from compound operation
            c= r.readCharFromReader();
            while(((byte)c) != -1 && isWhiteSpace(c)) { // This was added since after reading ; there might be some more white spaces. However there needs to be a way to combine this with the previous white spaces code or with the revised newline detection and unreading char below
                newline = newline || (c == 10 || c == 13 || c == ';' || ((c == ',') && (!ignoreCommas))  || (c == '>') || (c == '+'));
                c= r.readCharFromReader();
            }


        }
        char segment='\0'; // segment of (...) or "..." or '...'
        while(((byte)c) != -1 && ((!isWhiteSpace(c)) || (segment != '\0') || (ignoreWhiteSpaces)) && c != ';' && ((c != ':') ||
               (segment!='\0') || (ignoreColons))  && ((c != ',') || (segment != '\0') ||
               (ignoreCommas)) && (((c != '>') && (c != '+')) || (segment != '\0'))) { //- : denotes pseudo-classes, would like to keep them as one token

            if ((segment=='\0') && (c=='/')) { //comment start perhaps, if inside brackets - ignore
                c=handleCSSComment(r);
            }

            if ((c == '}' || c == '{' || c == '*' ) && (segment=='\0')) { //enter only if not in the middle of a segment. i.e. '*N'
                newline = true;
                if(currentToken.length() == 0) {
                    if(!readNewline) {
                        r.unreadChar(c);
                        return null;
                    }
                    return "" + c;
                }
                r.unreadChar(c);
                break;
            }
            currentToken.append(c);

            if (c=='(') {
                segment=')';
            } else if ((segment=='\0') && ((c=='\"') || (c=='\''))) { //Note - This keeps track of one segment only, while in fact there can be "nested" segments - i.e. ("...") which is common in URLs, though not sure it is critical as such pattern works correctly even now
                segment=c;
            } else if (c==segment) {
                segment='\0';
            }
            c= r.readCharFromReader();
        }
        if (((c==',') && (!ignoreCommas)) || (c=='>') || (c=='+')) {
            currentToken.append(c);
        }

        if((!readNewline) && (c==';')  && (currentToken.length() != 0) ) {
            r.unreadChar(c);
        }

        if(currentToken.length() == 0) {
            return null;
        }
        return currentToken.toString();
    }

    /**
     * Copies all attributes from
     *
     * @param element The element to copy from
     * @param selectors A vector containing grouped selectors to copy the attributes to
     * @param addTo The main element to add the grouped selectors to
     */
    private void copyAttributes(CSSElement element,Vector selectors,HTMLElement addTo) {
        if (selectors==null) {
            return;
        }
        for(Enumeration e=selectors.elements();e.hasMoreElements();) {
            CSSElement selector=(CSSElement)e.nextElement();
            addTo.addChild(selector);
            while (selector.getNumChildren()>0) { // This makes sure we get the last nested selector
                selector=selector.getCSSChildAt(0);
            }
            element.copyAttributesTo(selector);
        }
    }

    /**
     * Returns true if the specified CSS media type is unsupported, false otherwise
     *
     * @param media A string identifying the media type (i.e. "handheld")
     * @return true if the specified CSS media type is uspported, false otherwise
     */
    private boolean isMediaTypeSupported(String media) {
        for(int i=0;i<SUPPORTED_MEDIA_TYPES.length;i++) {
            if (media.equalsIgnoreCase(SUPPORTED_MEDIA_TYPES[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an at-media rule applies to the supported media types
     *
     * @param mediaTypes A string containing all media types the at-media rule allows
     * @return true if one of the supported media types is denoted, false otherwise
     */
    boolean mediaTypeMatches(String mediaTypes) {
        if ((mediaTypes==null) || (mediaTypes.equals(""))) {
            return true;
        }
        int comma=mediaTypes.indexOf(',');
        while (comma!=-1) {
            if (isMediaTypeSupported(mediaTypes.substring(0,comma).trim())) {
                return true;
            }
            mediaTypes=mediaTypes.substring(comma+1);
            comma=mediaTypes.indexOf(',');
        }
        return isMediaTypeSupported(mediaTypes.trim());
    }

    /**
     * Returns the import URL if the specified media matches, or null otherwise
     *
     * @param token The string including the url and media of the import at-rule (example: url("mycss.css") handheld,tv;
     * @return the import URL if the specified media matches, or null otherwise
     */
    private String getImportURLByMediaType(String token) {
        String url=token;
        boolean mediaMatches=true;
        int space=token.indexOf(' ');
        if (space!=-1) {
            url=token.substring(0, space);
            token=token.substring(space+1);
            mediaMatches=mediaTypeMatches(token);
        }
        if (mediaMatches) {
            if (url.startsWith("url(")) {
                url=CSSEngine.getCSSUrl(url);
            }
            return url;
        } else {
            return null;
        }

    }

    /**
     * Handles a media at-rule segment.
     * This method checks if the media type specified in the media at-rule is supported, if it does
     * it returns only the media segment as a separate stream, otherwsie it returns null
     *
     * @param isr The stream representing the CSS
     * @param encoding The encoding string
     * @param htmlC The HTMLComponent
     * @return An input stream with the relevant media segment or null if the media is not supported
     * @throws IOException on input stream failure
     */
    private ExtInputStreamReader getMediaSegment(ExtInputStreamReader r,String encoding,HTMLComponent htmlC) throws IOException {
        String token = nextToken(r,true,true,true,true);
        char c= r.readCharFromReader();

        while ((((byte)c) != -1) && (c!='{')) { // Find the first { that marks the start of the media segment
            c= r.readCharFromReader();
        }

        StringBuilder segment=new StringBuilder();
        boolean match=mediaTypeMatches(token);

        int count=1; // counts the number of opened curly brackets
        while (count>0) {
            c= r.readCharFromReader();
            if ((((byte)c)==-1)) {
                break; //end of file
            }
            if (match) {
                segment.append(c);
            }
            if (c=='{') {
                count++;
            } else if (c=='}') {
                count--;
            }
        }

        if (match) {
            ExtInputStreamReader segmentReader=null;
            if (encoding!=null) {
                try {
                    segmentReader=new ExtInputStreamReader(new InputStreamReader(new ByteArrayInputStream(segment.toString().getBytes()),encoding));
                } catch (UnsupportedEncodingException uee) {
                    notifyError(ParserCallback.ERROR_ENCODING, "@media", null, encoding, "Encoding '"+encoding+"' failed for media segment. "+uee.getMessage());
                }
            }
            if (segmentReader==null) { //either no encoding, or encoding failed
                segmentReader=new ExtInputStreamReader(new InputStreamReader(new ByteArrayInputStream(segment.toString().getBytes())));
            }
            return segmentReader;
        } else {
            return null;
        }

    }

    /**
     * Reads a CSS file/stream and returns the tokenized CSS as a single level element tree with the
     * root appearing as a "style".
     * This method is called upon finding linked/external CSS and embedded CSS segments.
     * It handles at-rules such as import/charset/media and forwards relevant segments to the parseCSS method
     *
     * @param isr The Reader representing the stream
     * @param is The InputStream representing the stream (We need it too, in case encoding changes and we need to create another InputStreamReader)
     * @param htmlC The HTMLComponent
     * @param  pageURL For external CSS the URL of the CSS, for embedded - null
     * @return A CSSElement containing all selectors found in the stream as its children
     * @throws IOException on input stream failure
     */
    CSSElement parseCSSSegment(Reader isr,InputStream is,HTMLComponent htmlC,String pageURL) throws IOException {
        CSSElement addTo = new CSSElement("style");
        ExtInputStreamReader r = new ExtInputStreamReader(isr);
        DocumentInfo docInfo=null;
        String encoding=htmlC.getDocumentInfo()!=null?htmlC.getDocumentInfo().getEncoding():null;
        String token = nextToken(r,true,false,true,false);
        while(token.startsWith("@")) {
            if (token.equals("@import")) {
                token = nextToken(r,true,true,true,true);
                String url=getImportURLByMediaType(token);
                if (url!=null) {
                    if (docInfo==null) {
                        docInfo=pageURL==null?htmlC.getDocumentInfo():new DocumentInfo(pageURL);
                    }
                    if (docInfo!=null) {
                        htmlC.getThreadQueue().addCSS(docInfo.convertURL(url),encoding); // Referred CSS "inherit" charset from the referring document
                    } else {
                        if (DocumentInfo.isAbsoluteURL(url)) {
                            htmlC.getThreadQueue().addCSS(url,encoding); // Referred CSS "inherit" charset from the referring document
                        } else {
                            notifyError(CSSParserCallback.ERROR_CSS_NO_BASE_URL, "@import", null, url, "Ignoring CSS file referred in an @import rule ("+url+"), since page was set by setBody/setHTML/setDOM so there's no way to access relative URLs");
                        }
                    }
                }
            } else if (token.equals("@media")) {
                ExtInputStreamReader mediaReader = getMediaSegment(r,encoding,htmlC);
                if (mediaReader!=null) {
                    parseCSS(mediaReader, htmlC, addTo,null);
                }
            } else if (token.equals("@charset")) {
                token = CSSEngine.omitQuotesIfExist(nextToken(r,true,false,true,false));
                if (is!=null) { // @charset applies only to external style sheet, and the inputstream is null for embedded CSS segments
                    try {
                        ExtInputStreamReader encodedReader=new ExtInputStreamReader(new InputStreamReader(is, token));
                        r=encodedReader;
                        encoding=token;
                    } catch (UnsupportedEncodingException uee) {
                        notifyError(ParserCallback.ERROR_ENCODING, "@charset", null, token, "External CSS encoding @charset "+token+" directive failed: "+uee.getMessage());
                    }
                }
            }
            token = nextToken(r,true,false,true,false);
        }

        return parseCSS(r, htmlC,  addTo,token);
    }

    /**
     * Reads a CSS file/stream and returns the tokenized CSS as a single level element tree with the
     * root appearing as a "style".
     * This method is called either directly on style attributes.
     *
     * @param r The stream reader containing the CSS segment
     * @param htmlC The HTMLComponent
     * @return A CSSElement containing all selectors found in the stream as its children
     * @throws IOException on input stream failure
     */
    CSSElement parseCSS(InputStreamReader r,HTMLComponent htmlC) throws IOException {
        ExtInputStreamReader er=new ExtInputStreamReader(r);
        return parseCSS(er, htmlC, null,null);
    }

    /**
     * Reads a CSS file/stream and returns the tokenized CSS as a single level element tree with the
     * root appearing as a "style".
     *
     * @param r The stream reader containing the CSS segment
     * @param htmlC The HTMLComponent
     * @param addTo the master CSSElement to add the selectors to (or null to open a new one_
     * @param firstToken A first toekn to process, or null if none
     * @return A CSSElement containing all selectors found in the stream as its children
     * @throws IOException on input stream failure
     */
    CSSElement parseCSS(ExtInputStreamReader r,HTMLComponent htmlC,CSSElement addTo,String firstToken) throws IOException {
        if (addTo==null) {
            addTo = new CSSElement("style");
        }
        CSSElement parent = addTo;
        Vector selectors = new Vector();
        CSSElement lastGroupedParent=null;

        boolean selectorMode = true;
        boolean grouping=false; // Grouping is when selector are grouped, i.e. h1,h2,h3 { ... }
        boolean childSelector=false; // when 'a > b' appears it is a child seelctor, meaning only 'b' that is a direct child of 'a' (unlike 'a b' which is a descendant selector)
        boolean siblingSelector=false; // when 'a + b' appears it is a sibling seelctor, meaning only 'b' that is adjacent to 'a'
        
        String token = "";

        //TODO - detect BOM for UTF8 etc.
        while(true) {

            if (firstToken!=null) {
                token=firstToken;
                firstToken=null;
            } else {
                token = nextToken(r,true,false,selectorMode,false);
            }
            if(token == null || token.indexOf("</style") > -1) {
                break;
            }

            if("{".equals(token)) {
                selectorMode = false;
                grouping=false;
                continue;
            }
            if("}".equals(token)) {
                selectorMode = true;
                copyAttributes(parent, selectors,addTo);
                parent = addTo;
                selectors = new Vector();
                lastGroupedParent=null;
                continue;
            }

            if(selectorMode) {
                // Checks for grouped selectors, note that due to spacing the comma can either appear as a separate token, or at the start of a token or at its end
                // All these scenarios are checked in the following lines of code.
                if (",".equals(token)) {
                    grouping=true;
                    continue;
                }

                if (">".equals(token)) {
                    childSelector=true;
                    continue;
                }

                if ("+".equals(token)) {
                    siblingSelector=true;
                    continue;
                }

                if (token.startsWith(",")) {
                    token=token.substring(1);
                    grouping=true;
                } else if (token.startsWith(">")) {
                    token=token.substring(1);
                    childSelector=true;
                } else if (token.startsWith("+")) {
                    token=token.substring(1);
                    siblingSelector=true;
                }

                boolean nextIsChildSelector=false;
                boolean nextIsSiblingSelector=false;

                if (token.endsWith(">")) {
                    nextIsChildSelector=true;
                    token=token.substring(0, token.length()-1);
                } else if (token.endsWith("+")) {
                    nextIsSiblingSelector=true;
                    token=token.substring(0, token.length()-1);
                }


                if (grouping) {
                    if (token.endsWith(",")) {
                        token=token.substring(0, token.length()-1);
                    } else {
                        grouping=false; //  there was no comma at the end, so next time it is not a grouped element (unless a comma will be detected as the next token or the start of the next token)
                    }
                    CSSElement entry = new CSSElement(token);
                    selectors.addElement(entry);
                    lastGroupedParent=entry;

                } else {
                    if (token.endsWith(",")) {
                        grouping=true;
                        token=token.substring(0, token.length()-1);
                    } 
                    CSSElement entry = new CSSElement(token);

                    
                    entry.descendantSelector=!childSelector;
                    entry.siblingSelector=siblingSelector;
                    if (lastGroupedParent==null) {
                        parent.addChild(entry);
                        parent = entry;
                    } else {
                        lastGroupedParent.addChild(entry);
                        lastGroupedParent=entry;
                    }
                    
                }
                childSelector=nextIsChildSelector;
                siblingSelector=nextIsSiblingSelector;

            } else {
                boolean compoundToken = false;

                for(int iter = 0 ; iter < CSSElement.CSS_SHORTHAND_ATTRIBUTE_LIST.length ; iter++) {
                    if(CSSElement.CSS_SHORTHAND_ATTRIBUTE_LIST[iter].equals(token)) {
                            compoundToken = true;
                            boolean collattable=CSSElement.CSS_IS_SHORTHAND_ATTRIBUTE_COLLATABLE[iter];
                            int valsAdded=0;
                            token = nextToken(r, false,false,false,false);

                            // This array is used for collatable attributes - the values can't be set as they are read, first we need to see how many values appear and set accordingly
                            String[] tokens = new String[4];
                            while(token!=null) {
                                if (collattable) {
                                    if (valsAdded<tokens.length) {
                                        tokens[valsAdded]=token;
                                        valsAdded++;
                                    }
                                } else {
                                    addShorthandAttribute(token, iter, parent);
                                }

                                token = nextToken(r, false,false,false,false);
                            }

                            // The following assigns the collatable attributes according to CSSElement.CSS_COLLATABLE_ORDER
                            if  ((collattable) && (valsAdded>0)) {
                                for(int i=0;i<CSSElement.CSS_COLLATABLE_ORDER[valsAdded-1].length;i++) {
                                    for(int j=0;j<CSSElement.CSS_COLLATABLE_ORDER[valsAdded-1][i].length;j++) {
                                        int side=CSSElement.CSS_COLLATABLE_ORDER[valsAdded-1][i][j];
                                        addAttributeTo(parent, CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[iter][side], tokens[i], htmlC);
                                    }
                                }
                            }
                            break;
                    }
                }

                // if this is a "regular" css attribute is it one of the supported attributes
                if(!compoundToken) {
                    // We ignore commas when collecting a value, since it can be for example: font-family:arial,tahoma,sans-serif etc.
                    // We also ignore spaces in font-family / access key since the value can be: arial, tahoma / send * , #
                    int result=addAttributeTo(parent, token, nextToken(r,false,true,false,
                            (token.equalsIgnoreCase("-wap-access-key") || (token.equalsIgnoreCase("font-family")) ||
                            (token.equalsIgnoreCase("quotes")) || (token.equalsIgnoreCase("border-spacing")) ||
                            (token.equalsIgnoreCase("content")) || (token.equalsIgnoreCase("counter-reset")) || (token.equalsIgnoreCase("counter-increment")))), htmlC);
                    if(result!=-1) {
                        // unsupported token we need to read until the newline
                        //while(nextToken(r, false, false,false) != null && !newline) {}
                        while(nextToken(r, false, false,false,false) != null) {} //TODO - is newline truly unnecessary ? + what if that happens in the end of the file - do we get into an infinite loop?
                    }
                }
            }
        }
        return addTo;
    }

    /**
     * Adds the specified value to the specified selector as a value to the shorthand attribute whose index is specified
     * This methods deals with the complexity of adding values for shorthand attributes, since they can be specified in any order
     * It also handles multiple-shorthand levels such as the 'border' attribute
     *
     * @param value The attribute's value
     * @param shorthandAttr The attribute's index
     * @param selector The selector to add the attribuet to
     * @return true if succeeded to add, false otherwise (for example invalid value)
     */
    private boolean addShorthandAttribute(String value,int shorthandAttr,CSSElement selector) {
        if (CSSElement.CSS_IS_SHORTHAND_ATTRIBUTE_COLLATABLE[shorthandAttr]) {
            return addCollatableAttribute(value, shorthandAttr, selector);
        }
        for(int i=0;i<CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr].length;i++) {
            int attrIndex=CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr][i];

            if (attrIndex>=CSSElement.CSS_STYLE_ID_OFFSET) {

                if (!selector.isAttributeAssigned(attrIndex)) { // Only check if the attribute wasn't set yet
                    int result=selector.addAttribute(attrIndex, value);
                    if (result==-1) { //no error code return - success
                        return true;
                    }
                }
            } else {
                boolean success=addShorthandAttribute(value, attrIndex, selector);
                if (success) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds the specified value to the specified selector as a value to the shorthand and collatable attribute whose index is specified
     * This is called from addShorthandAttribute when a shorthand attribute maps to a collatable attribute
     * Note that while usually collatable attributes can have 1-4 values, and are mapped according to CSSElement.CSS_COLLATABLE_ORDER
     * When they are specified as part of a top shorthand attribute, only one value can be specified and it is copied to all base attributes.
     * For example, While the definition 'border-width: 5px 10px' will set the vertical border width to 5 and the horizontal to 10,
     * One cannot specify: 'border: 5px 10px solid red' - but rather has to specify only one value that will be set as the width for all sides.
     *
     * @param value The attribute's value
     * @param shorthandAttr The attribute's index
     * @param selector The selector to add the attribuet to
     * @return true if succeeded to add, false otherwise (for example invalid value)
     */
    private boolean addCollatableAttribute(String value,int shorthandAttr,CSSElement selector) {
        int attrIndex=CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr][0];
        int result=selector.addAttribute(attrIndex, value);
        if (result==-1) {
            for(int i=1;i<CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr].length;i++) {
                attrIndex=CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr][i];
                selector.addAttribute(attrIndex, value);
            }
            return true;
        }
        return false;
    }

    /**
     * Adds the specified attribute and value pair to the specified selector
     *
     * @param selector The selector we're working on
     * @param attrId The attribute's id
     * @param value The attribute value
     * @param htmlC The HTMLComponent (To obtain the ParserCallback)
     * @return a positive value if an error occured, or -1 otherwise
     */
    private int addAttributeTo(CSSElement selector,int attrId,String value,HTMLComponent htmlC) {
            int error=selector.addAttribute(attrId, value);
            reportAddAttributeError(error,selector, selector.getAttributeName(new Integer(attrId)), value, htmlC);
            return error;
    }

    /**
     * Adds the specified attribute and value pair to the specified selector
     *
     * @param selector The selector we're working on
     * @param attributeName The attribute's name
     * @param value The attribute value
     * @param htmlC The HTMLComponent (To obtain the ParserCallback)
     * @return a positive value if an error occured, or -1 otherwise
     */
    private int addAttributeTo(CSSElement selector,String attributeName,String value,HTMLComponent htmlC) {
            int error=selector.setAttribute(attributeName, value);
            reportAddAttributeError(error,selector, attributeName, value, htmlC);
            return error;
    }


    /**
     * A helper method that handles reporting of CSS errors to the ParserCallback (if available)
     *
     * @param errorCode The error code as returned by the CSSElement.addAttribute methods
     * @param selector The selector we're working on
     * @param attributeName The attribute's name
     * @param value The attribute value
     * @param htmlC The HTMLComponent (To obtain the ParserCallback)
     */
    private void reportAddAttributeError(int errorCode,CSSElement selector,String attributeName,String value,HTMLComponent htmlC) {
        if (errorCode!=-1) {
            if (errorCode==CSSParserCallback.ERROR_CSS_ATTRIBUTE_NOT_SUPPORTED) {
                notifyError(errorCode, selector.getTagName(), attributeName, value, "CSS Attribute '"+attributeName+"' (Appeared in selector '"+selector.getTagName()+"') is not supported in WCSS.");
            } else if (errorCode==CSSParserCallback.ERROR_CSS_ATTIBUTE_VALUE_INVALID) {
                notifyError(errorCode, selector.getTagName(), attributeName, value, "CSS Attribute '"+attributeName+"' (Appeared in selector '"+selector.getTagName()+"') has an invalid value ("+value+")");
            }
        }
    }

    /**
     * A utility method used to notify an error to the ParserCallback and throw an IllegalArgumentException if parsingError returned false
     *
     * @param callback The ParserCallback
     * @param errorId The error ID, one of the ERROR_* constants in ParserCallback
     * @param tag The tag in which the error occured (Can be null for non-tag related errors)
     * @param attribute The attribute in which the error occured (Can be null for non-attribute related errors)
     * @param value The value in which the error occured (Can be null for non-value related errors)
     * @param description A verbal description of the error
     */
    void notifyError(int errorId,String tag, String attribute,String value,String description) {
        if (parserCallback!=null) {
            boolean cont=parserCallback.parsingError(errorId,tag,attribute,value,description);
            if (!cont) {
                throw new IllegalArgumentException(description);
            }
        }
    }

    public void setCSSParserCallback(CSSParserCallback parserCallback) {
        this.parserCallback=parserCallback;
    }

    /**
     *  A decorator for Reader that adds the ability to "unread" a character
     *  This makes parsing easier, and is used for CSS parsing.
     *
     * @author Ofir Leitner
     */
    class ExtInputStreamReader  {

        char lastCharRead = (char)-1;
        Reader internalReader;

        ExtInputStreamReader(Reader isr) {
            internalReader=isr;
        }

        /**
         * "Unreads" a character from the stream by placing it in a member variable to be later retreived by readCharFromReader, used by the CSS Parser
         *
         * @param c The character to unread
         */
        void unreadChar(char c) {
            lastCharRead = c;
        }

        /**
         * Reads the next character from the input stream, used by the CSS Parser
         * If there's an "unread" character in the buffer it is returned (and no reading is done to the actual stream)
         *
         * @param r The stream reader
         * @return the next character
         * @throws IOException
         */
        char readCharFromReader() throws IOException {
            if(lastCharRead != (char)-1) {
                char c = lastCharRead;
                lastCharRead = (char)-1;
                return c;
            }
            return (char)internalReader.read();
        }

    }

}
