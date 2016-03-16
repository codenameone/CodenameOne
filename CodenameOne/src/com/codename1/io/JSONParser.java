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
package com.codename1.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * <p>Fast and dirty parser for JSON content on the web, it essentially returns
 * a {@link java.util.Map} object containing the object fields mapped to their values. If the value is
 * a nested object a nested {@link java.util.Map}/{@link java.util.List} is returned. The sample
 * code below fetches a page of data from the nestoria housing listing API as a list of Map elements.
 * You can see instructions on how to display the data in the {@link com.codename1.components.InfiniteScrollAdapter}
 * class.</p>
 * <script src="https://gist.github.com/codenameone/22efe9e04e2b8986dfc3.js"></script>
 *
 * @author Shai Almog
 */
public class JSONParser implements JSONParseCallback {

    /**
     * Indicates that the parser will generate long objects and not just doubles for numeric values
     * @return the useLongsDefault
     */
    public static boolean isUseLongs() {
        return useLongsDefault;
    }

    /**
     * Indicates that the parser will generate long objects and not just doubles for numeric values
     * @param aUseLongsDefault the useLongsDefault to set
     */
    public static void setUseLongs(boolean aUseLongsDefault) {
        useLongsDefault = aUseLongsDefault;
    }

    static class ReaderClass {
        char[] buffer;
        int buffOffset;
        int buffSize = -1;
        int read(Reader is) throws IOException {
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

    }

    private static boolean useLongsDefault;
    private boolean modern;
    private Map<String, Object> state;
    private java.util.List<Object> parseStack;
    private String currentKey;
    static class KeyStack extends Vector {
		protected String peek() {
			return (String)elementAt(0);
		}

		protected void push(String key) {
			insertElementAt(key, 0);
		}
		
		protected String pop() {
			if (isEmpty()) {
				return null;
			}
			String key = peek();
			removeElementAt(0);
			return key;
		}
	};
    
    /**
     * Static method! Parses the given input stream and fires the data into the given callback.
     *
     * @param i the reader
     * @param callback a generic callback to receive the parse events
     * @throws IOException if thrown by the stream
     */
    public static void parse(Reader i, JSONParseCallback callback) throws IOException {
        boolean quoteMode = false;
        ReaderClass rc = new ReaderClass();
        rc.buffOffset = 0;
        rc.buffSize = -1;
        StringBuilder currentToken = new StringBuilder();
        KeyStack blocks = new KeyStack();
        String currentBlock = "";
        String lastKey = null;
        try {
            while (callback.isAlive()) {
                int currentChar = rc.read(i);
                if (currentChar < 0) {
                    return;
                }
                char c = (char) currentChar;

                if (quoteMode) {
                    switch (c) {
                        case '"':
                            String v = currentToken.toString();
                            callback.stringToken(v);
                            if (lastKey != null) {
                                callback.keyValue(lastKey, v);
                                lastKey = null;
                            } else {
                                lastKey = v;
                            }
                            currentToken.setLength(0);
                            quoteMode = false;
                            continue;
                        case '\\':
                            c = (char) rc.read(i);
                            if (c == 'u') {
                                String unicode = "" + ((char) rc.read(i)) + ((char) rc.read(i)) + ((char) rc.read(i)) + ((char) rc.read(i));
                                try {
                                    c = (char) Integer.parseInt(unicode, 16);
                                } catch (NumberFormatException err) {
                                    // problem in parsing the u notation!
                                    err.printStackTrace();
                                    System.out.println("Error in parsing \\u" + unicode);
                                }
                            } else {
                                switch(c) {
                                    case 'n':
                                        currentToken.append('\n');
                                        continue;
                                    case 't':
                                        currentToken.append('\t');
                                        continue;
                                    case 'r':
                                        currentToken.append('\r');
                                        continue;
                                }
                            }
                            currentToken.append(c);
                            continue;
                    }
                    currentToken.append(c);
                } else {
                    switch (c) {
                        case 'n':
                            // check for null
                            char u = (char) rc.read(i);
                            char l = (char) rc.read(i);
                            char l2 = (char) rc.read(i);
                            if (u == 'u' && l == 'l' && l2 == 'l') {
                                // this is null
                                callback.stringToken(null);
                                if (lastKey != null) {
                                    callback.keyValue(lastKey, null);
                                    lastKey = null;
                                }
                            } else {
                                // parsing error....
                                System.out.println("Expected null for key value!");
                            }

                            continue;
                        case 't':
                            // check for true
                            char a1 = (char) rc.read(i);
                            char a2 = (char) rc.read(i);
                            char a3 = (char) rc.read(i);
                            if (a1 == 'r' && a2 == 'u' && a3 == 'e') {
                                callback.stringToken("true");
                                if (lastKey != null) {
                                    callback.keyValue(lastKey, "true");
                                    lastKey = null;
                                }
                            } else {
                                // parsing error....
                                System.out.println("Expected true for key value!");
                            }

                            continue;
                        case 'f':
                            // this can either be the start of "false" or the end of a
                            // fraction number...
                            if (currentToken.length() > 0) {
                                currentToken.append('f');
                                continue;
                            }
                            // check for false
                            char b1 = (char) rc.read(i);
                            char b2 = (char) rc.read(i);
                            char b3 = (char) rc.read(i);
                            char b4 = (char) rc.read(i);
                            if (b1 == 'a' && b2 == 'l' && b3 == 's' && b4 == 'e') {
                                callback.stringToken("false");
                                if (lastKey != null) {
                                    callback.keyValue(lastKey, "false");
                                    lastKey = null;
                                }
                            } else {
                                // parsing error....
                                System.out.println("Expected false for key value!");
                            }

                            continue;
                        case '{':
                            if (lastKey == null) {
                            	if (blocks.size() == 0) {
                            		lastKey = "root";
                            	} else {
                            		lastKey = blocks.peek();
                            	}
                            }
                        	blocks.push(lastKey);
                            callback.startBlock(lastKey);
                            lastKey = null;
                            continue;
                        case '}':
                            if (currentToken.length() > 0) {
                                try {
                                    String ct = currentToken.toString();
                                    if(useLongsDefault) {
                                        if(ct.indexOf('.') > -1) {
                                            callback.numericToken(Double.parseDouble(ct));
                                        } else {
                                            callback.longToken(Long.parseLong(ct));
                                        }
                                    } else {
                                        callback.numericToken(Double.parseDouble(ct));
                                    }
                                    if (lastKey != null) {
                                        callback.keyValue(lastKey, currentToken.toString());
                                        lastKey = null;
                                        currentToken.setLength(0);
                                    }
                                    
                                } catch (NumberFormatException err) {
                                    err.printStackTrace();
                                    // this isn't a number!
                                }
                            }
                            currentBlock = blocks.pop();
                            callback.endBlock(currentBlock);
                            lastKey = null;
                            continue;
                        case '[':
                        	blocks.push(lastKey);

                            callback.startArray(lastKey);
                            lastKey = null;
                            continue;
                        case ']':
                            if (currentToken.length() > 0) {
                                try {
                                    String ct = currentToken.toString();
                                    if(useLongsDefault) {
                                        if(ct.indexOf('.') > -1) {
                                            callback.numericToken(Double.parseDouble(ct));
                                        } else {
                                            callback.longToken(Long.parseLong(ct));
                                        }
                                    } else {
                                        callback.numericToken(Double.parseDouble(ct));
                                    }
                                    if (lastKey != null) {
                                        callback.keyValue(lastKey, currentToken.toString());
                                        lastKey = null;
                                    }
                                } catch (NumberFormatException err) {
                                    // this isn't a number!
                                }
                            }
                            currentToken.setLength(0);

                            currentBlock = blocks.pop();
                            callback.endArray(currentBlock);
                            lastKey = null;
                            continue;
                        case ' ':
                        case '\r':
                        case '\t':
                        case '\n':
                            // whitespace
                            continue;

                        case '"':
                            quoteMode = true;
                            continue;
                        case ':':
                        case ',':
                            if (currentToken.length() > 0) {
                                try {
                                    String ct = currentToken.toString();
                                    if(useLongsDefault) {
                                        if(ct.indexOf('.') > -1) {
                                            callback.numericToken(Double.parseDouble(ct));
                                        } else {
                                            callback.longToken(Long.parseLong(ct));
                                        }
                                    } else {
                                        callback.numericToken(Double.parseDouble(ct));
                                    }
                                    if (lastKey != null) {
                                        callback.keyValue(lastKey, currentToken.toString());
                                        lastKey = null;
                                    }
                                } catch (NumberFormatException err) {
                                    // this isn't a number!
                                }
                            }
                            currentToken.setLength(0);
                            continue;
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case '-':
                        case '.':
                        case 'x':
                        case 'd':
                        case 'l':
                        case 'e':
                        case 'E':
                            currentToken.append(c);
                            continue;
                    }
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
            /*System.out.println();
            int current = i.read();
            while(current >= 0) {
            System.out.print((char)current);
            current = i.read();
            }*/
            i.close();
        }
    }
    /**
     * Parses the given input stream into this object and returns the parse tree
     *
     * @param i the reader
     * @return the parse tree as a hashtable
     * @throws IOException if thrown by the stream
     */
    public Map<String, Object> parseJSON(Reader i) throws IOException {
        modern = true;
        state = new LinkedHashMap<String, Object>();
        parseStack = new ArrayList<Object>();
        currentKey = null;
        parse(i, this);
        return state;
    }

    /**
     * Parses the given input stream into this object and returns the parse tree
     *
     * @param i the reader
     * @return the parse tree as a hashtable
     * @throws IOException if thrown by the stream
     * @deprecated use the new parseJSON instead
     */
    public Hashtable<String, Object> parse(Reader i) throws IOException {
        modern = false;
        state = new Hashtable();
        parseStack = new Vector();
        currentKey = null;
        parse(i, this);
        return (Hashtable<String, Object>)state;
    }

    private boolean isStackHash() {
        return parseStack.get(parseStack.size() - 1) instanceof Map;
    }

    private Map getStackHash() {
        return (Map) parseStack.get(parseStack.size() - 1);
    }

    private java.util.List<Object> getStackVec() {
        return (java.util.List<Object>) parseStack.get(parseStack.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public void startBlock(String blockName) {
        if (parseStack.size() == 0) {
            parseStack.add(state);
        } else {
            Map newOne;
            if(modern) {
                newOne = new LinkedHashMap();
            } else {
                newOne = new Hashtable();
            }
            if (isStackHash()) {
                getStackHash().put(currentKey, newOne);
                currentKey = null;
            } else {
                getStackVec().add(newOne);
            }
            parseStack.add(newOne);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endBlock(String blockName) {
        parseStack.remove(parseStack.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public void startArray(String arrayName) {
        java.util.List<Object> currentVector;
        Map newOne;
        if(modern) {
            currentVector = new ArrayList<Object>();
        } else {
            currentVector = new Vector<Object>();
        }

        // the root of the JSON is an array, we need to wrap it in an assignment
        if (parseStack.size() == 0) {
            parseStack.add(state);
            currentKey = "root";
        }
        if (isStackHash()) {
            getStackHash().put(currentKey, currentVector);
            currentKey = null;
        } else {
            getStackVec().add(currentVector);
        }
        parseStack.add(currentVector);
    }

    /**
     * {@inheritDoc}
     */
    public void endArray(String arrayName) {
        parseStack.remove(parseStack.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public void stringToken(String tok) {
        if (isStackHash()) {
            if (currentKey == null) {
                currentKey = tok;
            } else {
                if (tok != null) {
                    getStackHash().put(currentKey, tok);
                }
                currentKey = null;
            }
        } else {
            getStackVec().add(tok);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void numericToken(double tok) {
        if (isStackHash()) {
            getStackHash().put(currentKey, new Double(tok));
            currentKey = null;
        } else {
            getStackVec().add(new Double(tok));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void longToken(long tok) {
        if (isStackHash()) {
            getStackHash().put(currentKey, new Double(tok));
            currentKey = null;
        } else {
            getStackVec().add(new Long(tok));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void keyValue(String key, String value) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAlive() {
        return true;
    }
}
