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

import com.codename1.ui.html.HTMLUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

/**
 * <p>Parser class used to parse an XML document into a DOM object (Element). This code was originally
 * developed to parse HTML and as a result isn't as strict as most XML parsers and can parse many HTML documents
 * out of the box.  The parser is mostly stateful (although it does have an event callback API as well), its modeled closely
 * to the Java DOM API's.
 * </p>
 * 
 * <p>
 * In this sample an XML hierarchy is displayed using a {@link com.codename1.ui.tree.Tree}:
 * </p>
 * <script src="https://gist.github.com/codenameone/5361ad7339c1ae26e0b8.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tree-xml.png" alt="Tree with XML data" />
 * 
 * @author Ofir Leitner
 */
public class XMLParser {
    private static char[] buffer;
    private static int buffOffset;
    private static int buffSize = -1;

    private static final Element END_TAG = new Element();
    private boolean eventParser;
    private boolean caseSensitive;
    ParserCallback parserCallback;
    boolean includeWhitespacesBetweenTags; // For HTML white spaces between tags are significant to seperate words, in XML less so and it mostly creates garbage elements (Text with one space)

    /**
     * The char entities strings supported in XML. When a char entity is found these will be compared against first.
     *
    private static final String[] XML_CHAR_ENTITIES = {
                                                          "lt", // lesser-than
                                                          "gt", // greater-than
                                                          "amp", // ampersand
                                                          "quot", //quotation mark
                                                          "apos", // apostrophe
                                                          //"bull", //bullet
                                                          //"euro" //euro
                                                            };

    /**
     * The numericals value of char entities strings above.
     *
    private static final int[] XML_CHAR_ENTITIES_VALS = {
                                                            60, // "lt", // lesser-than
                                                            62, // "gt", // greater-than
                                                            38, // "amp", // ampersand
                                                            34, // "quot", //quotation mark
                                                            39, // "apos", // apostrophe
                                                            //8226, // "bull", //bullet
                                                            //8364 // "euro"}; //euro
                                                            };*/

   /**
    * This hashtable contains user defined char entities 
    */
   private Hashtable userDefinedCharEntities;

   /**
    * A constant containing the CDATA tag identifier (minus the C which is read anyway first)
    * CDATA nodes will be converted to text nodes
    */
   private static final String CDATA_STR = "DATA[";

    private static int read(Reader is) throws IOException {
        int c = -1;
        if(buffer == null) {
            buffer = new char[8192];
        }
        
        if(buffSize < 0 || buffOffset >= buffSize) {
            buffSize = is.read(buffer, 0, buffer.length);
            if(buffSize < 0) {
                return -1;
            }
            buffOffset = 0;
        }
        c = buffer[buffOffset];
        buffOffset ++;
        
        return c;
    }


   /**
    * Constructs the XMLParser
    */
   public XMLParser() {
    }




    /**
     * Returns a string identifying the document type this parser supports.
     * This should be overriden by subclassing parsers.
     * 
     * @return a string identifying the document type this parser supports.
     */
    protected String getSupportedStandardName() {
        return "XML";
    }

    /**
     * Adds the given symbol and code to the user defined char entities table
     * http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
     * 
     * @param symbol The symbol to add
     * @param code The symbol's code
     */
    public void addCharEntity(String symbol,int code) {
        if (userDefinedCharEntities==null) {
            userDefinedCharEntities=new Hashtable();
        }
        userDefinedCharEntities.put(trimCharEntity(symbol),new Integer(code));
    }

    /**
     * Adds the given symbols array  to the user defined char entities table with the startcode provided as the code of the first string, startcode+1 for the second etc.
     * Some strings in the symbols array may be null thus skipping code numbers.
     *
     * @param symbols The symbols to add
     * @param startcode The symbol's code
     */
    public void addCharEntitiesRange(String[] symbols,int startcode) {
        if (userDefinedCharEntities==null) {
            userDefinedCharEntities=new Hashtable();
        }
        int slen = symbols.length;
        for(int i=0;i<slen;i++) {
            if (symbols[i]!=null) {
                userDefinedCharEntities.put(trimCharEntity(symbols[i]),new Integer(startcode+i));
            }
        }
    }

    /**
     * Trims unneeded & and ; from the symbol if exist
     *
     * @param symbol The char entity symbol
     * @return A trimmed char entity without & and ;
     */
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


    /**
     * This method translates between a XML char entity string to the according char code.
     * The string is first compared to the 5 XML supported strings: quot,apos,amp,lt and gt.
     * If still not found it goes to look in the user defined char entities hashtable
     * 
     * @param symbol The symbol to lookup
     * @return The char code of the symbol, or -1 if none found
     *
    protected int getCharEntityCode(String symbol,Hashtable userDefined) {
        // First tries the XML basic char entities
        int val=-1;
        for (int i=0;i<XML_CHAR_ENTITIES.length;i++) {
            if (symbol.equalsIgnoreCase(XML_CHAR_ENTITIES[i])) {
                return XML_CHAR_ENTITIES_VALS[i];
            }
        }
        if (val!=-1) {
            return val;
        } else {

            // Not found in the standard symbol table, see if it is in the user defined symbols table
            if (userDefined!=null) {
                Object charObj=userDefined.get(symbol);
                if (charObj!=null) {
                    return ((Integer)charObj).intValue();
                }
            }

            // Not found anywhere
            return -1;
        }
    }*/


    /**
     * Converts a char entity to the matching character.
     * This handles both numbered and symbol char entities (The latter is done via getCharEntityCode)
     *
     * @param charEntity The char entity to convert
     * @return A string containing a single char, or the original char entity string (with & and ;) if the char entity couldn't be resolved
     */
    protected String convertCharEntity(String charEntity) {
        try {
            return HTMLUtils.convertCharEntity(charEntity, false, userDefinedCharEntities);
        } catch (IllegalArgumentException iae) {
            notifyError(ParserCallback.ERROR_UNRECOGNIZED_CHAR_ENTITY,null,null,null, "Unrecognized char entity: "+charEntity);
            // Another option is to return an empty string, but returning the entity will unravel bugs and will also allow ignoring common mistakes such as using the & char (instead of &apos;)
            return new StringBuilder().append('&').append(charEntity).append(';').toString();
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




    /**
     * This is the entry point for parsing a document and the only non-private member method in this class
     *
     * @param is The InputStream containing the XML
     * @return an Element object describing the parsed document (Basically its DOM)
     */
    public Element parse(Reader is) {
        buffOffset = 0;
        buffSize = -1;
        eventParser = false;
        Element rootElement=createNewElement("ROOT"); // ROOT is a "dummy" element that all other document elements are added to
        try {
            parseTagContent(rootElement, is);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (rootElement.getNumChildren()==0) {
            notifyError(ParserCallback.ERROR_NO_ROOTS, null, null, null, "XML document contains no root element.");
            return null;
        } else if (rootElement.getNumChildren()>1) {
            String roots="";
            for(int i=1;i<rootElement.getNumChildren();i++) {
                Element elem=rootElement.getChildAt(i);
                if (elem.isTextElement()) {
                    roots+="Text ("+elem.getText()+"),";
                } else {
                    roots+=elem.getTagName()+",";
                }
            }
            if (roots.endsWith(",")) {
                roots=roots.substring(0, roots.length()-1);
            }

            Element firstRoot=rootElement.getChildAt(0);
            String str=null;
            if (firstRoot.isTextElement()) {
                str="TEXT:"+firstRoot.getText();
            } else {
                str=firstRoot.getTagName();
            }
            notifyError(ParserCallback.ERROR_MULTIPLE_ROOTS, null, null, null, "XML document contains multiple root elements, only the first root ("+str+") will be used. Excessive roots: "+roots);
        }
        rootElement=rootElement.getChildAt(0);
        return rootElement;
    }

    /**
     * Creates a new element. This should be overriden by parsers that use a subclass of Element.
     * 
     * @param name The new element's name
     * @return a new instance of the element
     */
    protected Element createNewElement(String name) {
        Element e = new Element(name);
        e.caseSensitive = caseSensitive;
        return e;
    }

    /**
     * Creates a new text element. This should be overriden by parsers that use a subclass of Element.
     *
     * @param text The new element's text
     * @return a new instance of the element
     */
    protected Element createNewTextElement(String text) {
        Element e = new Element(text,true);
        e.caseSensitive = caseSensitive;
        return e;
    }

    public void setIncludeWhitespacesBetweenTags(boolean include) {
        includeWhitespacesBetweenTags=include;
    }

    /**
     * The event parser requires deriving this class and overriding callback
     * methods to work effectively. To stop the event parser in mid way a
     * callback can simply throw an IOException on purpose.
     * 
     * @param r the reader from which the data should be parsed
     * @throws java.io.IOException if an exception is thrown by the reader
     */
    public void eventParser(Reader r) throws IOException {
        eventParser = true;
        parseTagContent(null, r);
    }

    /**
     * Invoked when the event parser encounters a text element.
     * This callback method is invoked only on the eventParser.
     *
     * @param text the text encountered
     */
    protected void textElement(String text) {
    }

    /**
     * Invoked when a tag is opened, this method should return true to process
     * the tag or return false to skip the tag.
     * This callback method is invoked only on the eventParser.
     *
     * @param tag the tag name
     * @return true to process the tag, false to skip the tag
     */
    protected boolean startTag(String tag) {
        return true;
    }

    /**
     * Invoked when a tag ends
     * This callback method is invoked only on the eventParser.
     *
     * @param tag the tag name
     */
    protected void endTag(String tag) {
    }

    /**
     * Invoked for every attribute value of the givne tag
     * This callback method is invoked only on the eventParser.
     *
     * @param tag the tag name
     */
    protected void attribute(String tag, String attributeName, String value) {
    }


    /**
     * Checks if this character is a legal character for char entities
     * 
     * @param c The character to check
     * @return true if legal, false otherwise
     */
    private boolean isLegalCharEntityCharacter(char c) {
        return (((c>='a') && (c<='z')) || ((c>='A') && (c<='Z')) || ((c>='0') && (c<='9')) || (c=='#'));
    }

    /**
     * Parses tags content, accumulating text and child elements .
     * Upon bumping a start tag character it calls the parseTag method.
     * This method is called at first from the parse method, and later on from parseTag (which creates the recursion).
     *
     * @param element The current parent element
     * @param is The InputStream containing the XML
     * @throws IOException if an I/O error in the stream is encountered
     */
    protected void parseTagContent(Element element,Reader is) throws IOException {
        StringBuilder text=null;
        boolean leadingSpace=false;
        int val = read(is);
        char c=(char)val;
        StringBuilder charEntity = null;

        while(val!=-1) {
            if (c=='<') {
                if ((includeWhitespacesBetweenTags) && (leadingSpace) && (text==null) && (element!=null) && (element.getNumChildren()>0)) { 
                    leadingSpace=false;
					text = new StringBuilder().append(' ');
                }
                    
                if (text!=null) {
                    // Mistakenly "collected" something that is not a char entity, perhaps
                    // misuse of the & character (instead of using &apos;)
                    if (charEntity!=null) { 
						text.append('&').append(charEntity.toString());
                        charEntity=null;
                    }
                    if (leadingSpace) {
                        text.insert(0, ' ');
                    }
                    if(element != null) {
                        Element textElement=createNewTextElement(text.toString());
                        element.addChild(textElement);
                    } else {
                        textElement(text.toString());
                    }
                    text=null;
                    leadingSpace=false;
                    
                }


                Element childElement=parseTag(is);
                if (childElement==END_TAG) { 
                    //was actually an ending tag
                    StringBuilder closingTag = new StringBuilder();
                    int i = read(is);

                    // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                    if(i == -1) {
                        childElement = END_TAG;
                    } else {
                        c=(char)i;
                        while ((c!='>')) {
                            closingTag.append(c);
                            i = read(is);
                            c=(char)i;
                            if(i == -1) {
                                childElement = END_TAG;
                                break;
                            }
                        }
                    }
                    
                    String ct = closingTag.toString();
                    if(eventParser) {
                        endTag(ct);
			if (!isEmptyTag(ct)) {
			    // patch from http://code.google.com/p/codenameone/issues/detail?id=428 
                            // not really sure if this is the best approach but if it solves a bug...
			    return;
			}
                    }

                    if(element != null) {
                        if (ct.equalsIgnoreCase(element.getTagName())) {
                            return;
                        } else if (isEmptyTag(ct)) {
                            // do nothing, someone chose to close an empty tag i.e. <img ....></img> or <br></br>
                        } else {
                            notifyError(ParserCallback.ERROR_NO_CLOSE_TAG, element.getTagName(), null, null, "Malformed XML - no appropriate closing tag for " + element.getTagName());
                        }
                    }
                } else {
                    if (element != null && !childElement.isComment) {
                        element.addChild(childElement);
                    }
                }
            } else if (text!=null) {
                if (charEntity!=null) {
                    if (c==';') { //end
                        text.append(convertCharEntity(charEntity.toString()));
                        charEntity=null;
                    } else if (isLegalCharEntityCharacter(c)) {
                        charEntity.append(c);
                    } else {
                        text.append('&').append(charEntity).append(c);
                        charEntity=null;
                    }
                } else if (c=='&') { //start char entity
                    charEntity = new StringBuilder();
                } else {
                    text.append(c);
                }
            } else if (!isWhiteSpace(c)) {
                if (c=='&') { //text starts with a character entity (i.e. &nbsp;)
                    charEntity = new StringBuilder(); // The & is not included in the string we accumulate
					text = new StringBuilder(); //Initalize text so it won't be null
                } else {
                    text = new StringBuilder().append(c);
                }
            } else { // leading space is relevant also for newline and other whitespaces //if (c==' ') {
                leadingSpace=true;
            }
            val = read(is);
            c=(char)val;
        }
    }

    /**
     * Checks if the specified character is a white space or not.
     * Exposed to packaage since used by HTMLComponent as well
     *
     * @param ch The character to check
     * @return true if the character is a white space, false otherwise
     */
    protected boolean isWhiteSpace(char ch) { 
        return ((ch==' ') || (ch=='\n') || (ch=='\t') || (ch==10) || (ch==13));
    }

    /**
     * This method collects the tag name and all of its attributes.
     * For comments and XML declarations this will call the parseCommentOrXMLDeclaration method.
     * Note that this method returns an Element with a name and attrbutes, but not its content/children which will be done by parseTagContent
     *
     * @param is The InputStream containing the XML
     * @return The parsed element 
     * @throws IOException if an I/O error in the stream is encountered
     */
    protected Element parseTag(Reader is) throws IOException {
        StringBuilder tagName = new StringBuilder();
        StringBuilder curAttribute = new StringBuilder();
        StringBuilder curValue = new StringBuilder();
        //boolean procInst=false; // Support for the styleshhet processing instruction was removed, as it is not supported in most browsers, and it causes problems by adding tags before the HTML element (Makign the document with multiple roots)

        char c=(char)read(is);
        if (c=='/') {
            return END_TAG; //end tag
        } else if (c=='!') {
            c=(char)read(is);
            char c2=(char)read(is);
            if ((c=='-') && (c2=='-')) { //comment
                return parseCommentOrXMLDeclaration(is,"-->");
            } else if ((c=='[') && (c2=='C')) { // CDATA?
                c=(char)read(is);
                int idx=0;
                int clen = CDATA_STR.length();
                while ((idx<clen) && (c==CDATA_STR.charAt(idx))) {
                    idx++;
                    if (idx<clen) {
                        c=(char)read(is);
                    }
                }
                if (idx==clen) { //found CDATA
                    return parseCommentOrXMLDeclaration(is,"]]>"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
                } else { // some other unknown tag
                    return parseCommentOrXMLDeclaration(is,">"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
                }

            } else {
                return parseCommentOrXMLDeclaration(is,">"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
            }
        } else if (c=='?') {
            //procInst=true;
            //c=(char)read(is);
            return parseCommentOrXMLDeclaration(is,">"); //parse XML declaration i.e. <?xml version="1.0" encoding="ISO-8859-1"?> as comments as well - i.e. ignore them
        }

         //read and ignore any whitespaces before tag name
        while (isWhiteSpace(c)) {
            c=(char)read(is);
        }

        //collect tag name
        while ((!isWhiteSpace(c)) && (c!='>') && (c!='/')) {
        	if (eventParser == false) {
                    if(!caseSensitive) {
        		c = Character.toLowerCase(c);
                    }
        	}
        	tagName.append(c);
                int i = read(is);
                
                // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                if(i == -1) {
                    return END_TAG;
                }
                c=(char)i;
        }

         //read and ignore any whitespaces after tag name
        while (isWhiteSpace(c)) {
            c=(char)read(is);
        }

        boolean processTag = true;
		String tn = tagName.toString();
		if (eventParser) {
			processTag = startTag(tn);
		}
        // We do not support any processing instructions
        /*if (procInst) {
            if (tagName.equals("xml-stylesheet")) { // The XML processing instruction <?xml-stylesheet ... ?> has the same parameters as <link .. > and behaves the same way
                tagName="link";
            } else { // Processing instruction not supported - read till its end
                c=(char)read(is);
                while (c!='>') {
                    c=(char)read(is);
                }
                Element procInstElem=createNewElement("unsupported");
                procInstElem.isComment=true;
                return procInstElem;
            }
        }*/
        Element element = null;
        if(!eventParser) {
            element=createNewElement(tn);
		}

		if (!processTag || !isSupported(element)) {
			notifyError(ParserCallback.ERROR_TAG_NOT_SUPPORTED, tn, null, null, "The tag '" + tn + "' is not supported in " + getSupportedStandardName());
            if (!processTag || !shouldEvaluate(element)) {
                // If tag is not supported we skip it all till the closing tag.
                // This is especially important for the script tag which may contain '<' and '>' which might confuse the parser
                char lastChar=c;
                while (c!='>') { // Read till the end of the tag
                    lastChar=c;
                    int i = read(is);
                    
                    // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                    if(i == -1) {
                        return END_TAG;
                    }
                    c=(char)i;
                }
                if (lastChar!='/') { // If this is an empty tag, no need to search for its closing tag as there's none...
                    String endTag = new StringBuilder().append('<').append('/').append(tagName).append('>').toString();
                    int index=0;
                    int elen = endTag.length();
                    while(index<elen) {
                        int i = read(is);

                        // Fix for http://code.google.com/p/codenameone/issues/detail?id=900
                        if(i == -1) {
                            return END_TAG;
                        }
                        c=(char)i;

                        if ((c>='A') && (c<='Z')) {
                            c=(char)(c-'A'+'a');
                        }
                        if (c==endTag.charAt(index)) {
                            index++;
                        } else {
                            index=0;
                        }
                    }
                }

                return element;
            }
        }

        if (c=='>') { //tag declartion ended, process content
            if (!isEmptyTag(tn)) {
                parseTagContent(element, is);
            }
            return element;
        } else if (c=='/') { // || ((procInst) && (c=='?'))) { //closed tag - no content
            c=(char)read(is);
            if (c=='>') {
                if(eventParser) {
                    endTag(tn);
                }
                return element;
            } else {
                notifyError(ParserCallback.ERROR_UNEXPECTED_CHARACTER, tn, null, null, "XML malformed - no > after /");
            }
        }


        while(true) {
			curAttribute.delete(0, curAttribute.length()).append(c);
            c=(char)read(is);
            while ((!isWhiteSpace(c)) && (c!='=') && (c!='>')) {
            	if (eventParser == false) {
                    if(!caseSensitive) {
            		c = Character.toLowerCase(c);
                    }
            	}
                curAttribute.append(c);
                c=(char)read(is);
            }

            if (c=='>') { // tag close char shouldn't be found here, but if the XML is slightly malformed we return the element
                notifyError(ParserCallback.ERROR_UNEXPECTED_TAG_CLOSING, tn, curAttribute.toString(), null, "Unexpected tag closing in tag " + tagName + ", attribute=" + curAttribute);
				if (!isEmptyTag(tn)) {
                    parseTagContent(element, is);
                }
                return element;
            }

             //read and ignore any whitespaces after attribute name
            while (isWhiteSpace(c)) {
                c=(char)read(is);
            }

            if (c!='=') {
                notifyError(ParserCallback.ERROR_UNEXPECTED_CHARACTER, tn, curAttribute.toString(), null, "Unexpected character " + c + ", expected '=' after attribute " + curAttribute.toString() + " in tag " + tagName);
                if (c=='>') { // tag close char shouldn't be found here, but if the XML is slightly malformed we return the element
                    if (!isEmptyTag(tn)) {
                        parseTagContent(element, is);
                    }
                    return element;
                }


                continue; //if attribute is not followed by = then process the next attribute
            }

            c=(char)read(is);
             //read and ignore any whitespaces before attribute value
            while (isWhiteSpace(c)) {
                c=(char)read(is);
            }

            char quote=' ';


            if ((c=='"') || (c=='\'')) {
                quote=c;
            } else {
				curValue.append(c);
            }

            StringBuilder charEntity = null;
            boolean ended=false;
            while (!ended) {
                int i = read(is);
                if(i < 0) {
                    return END_TAG;
                }
                c=(char)i;
                if (c==quote) {
                    ended=true;
                    i = read(is);
                    if(i < 0) {
                        return END_TAG;
                    }
                    c=(char)i;
                } else if ((quote==' ') && ((c=='/') || (c=='>') || (isWhiteSpace(c)))) {
                    ended=true;
                } else if (c=='&') {
                    if (charEntity!=null) {
						curValue.append('&').append(charEntity); // Wasn't a char entit, probably a url as a parameter : i.e. param="/test?p=val&pw=val2&p3=val3
                    }
                    charEntity = new StringBuilder();
                } else {
                    if (charEntity!=null) {
                        if (c==';') {
                            curValue.append(convertCharEntity(charEntity.toString()));
                            charEntity=null;
                        } else if (isLegalCharEntityCharacter(c)) {
                            charEntity.append(c);
                        } else {
                            curValue.append('&').append(charEntity).append(c);
                            charEntity=null;
                        }
                    } else {
                        curValue.append(c);
                    }
                }
            }

            if (charEntity!=null) { // Mistaken something else for a char entity - for example an action which is action="http://domain/test.html?param1=val1&param2=val2"
                curValue.append('&').append(charEntity);
                charEntity=null;
            }

            if(eventParser) {
                attribute(tn, curAttribute.toString(), curValue.toString());
			} else {
                int error=element.setAttribute(curAttribute.toString(), curValue.toString());

                if (error==ParserCallback.ERROR_ATTRIBUTE_NOT_SUPPORTED) {
                    notifyError(error, tn, curAttribute.toString(), curValue.toString(), "Attribute '" + curAttribute + "' is not supported for tag '" + tagName + "'.");
                    //notifyError(error, tagName, curAttribute, curValue, "Attribute '"+curAttribute+"' is not supported for tag '"+tagName+"'. Supported attributes: "+element.getSupportedAttributesList());
                } else if (error==ParserCallback.ERROR_ATTIBUTE_VALUE_INVALID) {
                    notifyError(error, tn, curAttribute.toString(), curValue.toString(), "Attribute '" + curAttribute + "' in tag '" + tn + "' has an invalid value (" + curValue.toString() + ")");
                }
            }

             //read and ignore any whitespaces after attribute/value pair
            while (isWhiteSpace(c)) {
                int i = read(is);
                if(i < 0) {
                    return END_TAG;
                }
                c=(char)i;
            }

            if (c=='>') { //tag declartion ended, process content
                if (!isEmptyTag(tn)) {
                    parseTagContent(element, is);
                }
                return element;
            } else if (c=='/') { // || ((procInst) && (c=='?'))) { //closed tag - no content
                int i = read(is);
                if(i < 0) {
                    return END_TAG;
                }
                c=(char)i;
                if (c=='>') {
                    // Solves the case of <a p="s"/> endTag("a") would not be called related to
                    // http://code.google.com/p/codenameone/issues/detail?id=896
                    if(eventParser) {
                        endTag(tn);
                    }
                    return element;
                } else {
                    notifyError(ParserCallback.ERROR_UNEXPECTED_CHARACTER, tn, curAttribute.toString(), curValue.toString(), "XML malformed - no > after /");
                }
            }
			curValue = curValue.delete(0,curValue.length());
        }
        
    }

    /**
     * This utility method is used to parse comments and XML declarations in the XML.
     * The comment/declaration is returned as an Element, but is flagged as a comment since both comments and XML declarations are not part of the XML DOM.
     * This method can be overridden to process specific XML declarations
     *
     * @param is The inputstream
     * @param endTag The endtag to look for
     * @return An Element representing the comment or XML declartaion
     * @throws IOException
     */
    protected Element parseCommentOrXMLDeclaration(Reader is,String endTag) throws IOException {
		char endTagChars[] = endTag.toCharArray();
        int endTagPos=0;
        StringBuilder text = new StringBuilder();
        boolean ended=false;
        while (!ended) {
            int in = read(is);
            if(in == -1) {
                // input stream ended abruptly
                break;
            }
            char c=(char)in;
            if (c==endTagChars[endTagPos]) {
                endTagPos++;
                if (endTagPos==endTagChars.length) {
                    ended=true;
                }
            } else {
                if (endTagPos!=0) { //add - or -- if it wasn't an end tag eventually
                    // with cdata it might end with ]]> but we might have a string such as ]]]>
                    // which is valid and the first ] is just a part of the string... So we need to check
                    // for that special case http://code.google.com/p/codenameone/issues/detail?id=865
                    if(c == endTagChars[0] && endTagPos > 1) {
                        text.append(c);
                        continue;
                    }
                    
                    text.append(endTagChars, 0, endTagPos);
                    endTagPos=0;
                }
                text.append(c);
            }
        }

        String elementName=null;
        if (endTag.equals("-->")) {
            elementName="comment";
        } else if (endTag.equals(">")) {
            elementName="XML declaration";
        } else { //CDATA
            if(eventParser) {
                textElement(text.toString());
                return null;
            }
            return createNewTextElement(text.toString());
        }

        if(eventParser) {
            return null;
        }
        Element comment = createNewElement(elementName);
        comment.setAttribute("content", text.toString());
        comment.isComment=true;
        return comment;
    }

    /**
     * Checks whether the specified tag is an empty tag
     *
     * @param tagName The tag name to check
     * @return true if that tag is defined as an empty tag, false otherwise
     */
    protected boolean isEmptyTag(String tagName) {
        return false;
    }

    /**
     * A utility method used to notify an error to the ParserCallback and throw an IllegalArgumentException if parsingError returned false
     *
     * @param errorId The error ID, one of the ERROR_* constants in ParserCallback
     * @param tag The tag in which the error occured (Can be null for non-tag related errors)
     * @param attribute The attribute in which the error occured (Can be null for non-attribute related errors)
     * @param value The value in which the error occured (Can be null for non-value related errors)
     * @param description A verbal description of the error
     * @throws IllegalArgumentException If the parser callback returned false on this error
     */
    protected void notifyError(int errorId,String tag, String attribute,String value,String description) {
        if (parserCallback!=null) {
            boolean cont=parserCallback.parsingError(errorId,tag,attribute,value,description);
            if (!cont) {
                throw new IllegalArgumentException(description);
            }
        } 
    }

    /**
     * Returns true if this element is supported, false otherwise
     * In XMLParser this always returns true, but subclasses can determine if an element is supported in their context according to its name etc.
     * Unsupported elements will be skipped by the parser and excluded from the resulting DOM object
     *
     * @param element The element to check
     *
     * @return true if the element is supported, false otherwise
     */
    protected boolean isSupported(Element element) {
        return true;
    }

    /**
     * Checks if this element should be evaluated by the parser
     * This can be overriden by subclasses to skip certain elements
     * 
     * @param element The element to check
     *
     * @return true if this element should be evaluated by the parser, false to skip it completely
     */
    protected boolean shouldEvaluate(Element element) {
        return true;
    }


    /**
     * Sets the specified callback to serve as the callback for parsing errors
     * 
     * @param parserCallback The callback to use for parsing errors
     */
    public void setParserCallback(ParserCallback parserCallback) {
        this.parserCallback=parserCallback;
    }

    /**
     * Sets the parser to be case sensitive and retain case, otherwise it will convert all data to lower case
     * @return the caseSensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Sets the parser to be case sensitive and retain case, otherwise it will convert all data to lower case
     * @param caseSensitive the caseSensitive to set
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

}