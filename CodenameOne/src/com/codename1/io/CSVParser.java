/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

/**
 * Simple CSV parser very useful for importing data into applications quickly from a CSV source
 *
 * @author Shai Almog
 */
public class CSVParser {
    private char separatorChar;
    private Reader currentReader;
    private char[] buffer = new char[8192];
    private int bufferSize = -1;
    private int bufferOffset;
    
    /**
     * Initializes a parser with the default comma (',') separator char 
     */
    public CSVParser() {
        this(',');
    }

    /**
     * Allows creating a parser with a custom separator char
     * 
     * @param separatorChar custom separator character such as semi-colon (';') etc.
     */
    public CSVParser(char separatorChar) {
        this.separatorChar = separatorChar;
    }
    
    /**
     * Parses input from the given stream and returns the tokens broken into rows and columns
     * 
     * @param r the input stream
     * @return array of rows and columns
     */
    public String[][] parse(InputStream r) throws IOException {
        return parse(new InputStreamReader(r));
    }
    
    /**
     * Parses input from the given stream and returns the tokens broken into rows and columns
     * 
     * @param r the input stream
     * @param encoding the encoding of the stream
     * @return array of rows and columns
     */
    public String[][] parse(InputStream r, String encoding) throws IOException {
        return parse(new InputStreamReader(r, encoding));
    }
    
    private int nextChar() throws IOException {
        int response = peekNextChar();
        bufferOffset++;
        return response;
    }
    
    private int peekNextChar() throws IOException {
        if(bufferOffset >= bufferSize) {
            bufferSize = currentReader.read(buffer);
            if(bufferSize == -1) {
                return -1;
            }
            bufferOffset = 0;
        }
        int response = buffer[bufferOffset];
        return response;        
    }
    
    /**
     * Parses input from the given reader and returns the tokens broken into rows and columns
     * 
     * @param r the reader stream
     * @return array of rows and columns
     */
    public String[][] parse(Reader r) throws IOException {
        currentReader = r;
        StringBuffer stringBuf = new StringBuffer();
        boolean isQuoteMode = false;
        Vector returnValue = new Vector();
        Vector currentVector = new Vector();
        returnValue.addElement(currentVector);
        int currentChar = nextChar();
        while(currentChar > -1) {
            if(isQuoteMode) {
                if(currentChar == '"') {
                    int next = peekNextChar();
                    if(next == '"') {
                        stringBuf.append('"');
                    } else {
                        if(next == -1) {
                            stringBuf.append('"');
                        } else {
                            isQuoteMode = false;
                        }
                    }
                } else {
                    stringBuf.append((char)currentChar);
                }
            } else {
                if(stringBuf.length() == 0) {
                    if(currentChar == '"') {
                        isQuoteMode = true;
                        currentChar = nextChar();
                        continue;
                    }
                }
                if(currentChar == separatorChar) {
                    currentVector.addElement(stringBuf.toString());
                    stringBuf.setLength(0);
                    currentChar = nextChar();
                    continue;
                } 
                if(currentChar == 10 || currentChar == 13) {
                    while(currentChar == 10 || currentChar == 13) {
                        currentChar = nextChar();
                    }
                    currentVector.addElement(stringBuf.toString());
                    stringBuf.setLength(0);
                    currentVector = new Vector();
                    returnValue.addElement(currentVector);
                    continue;
                }
                stringBuf.append((char)currentChar);
            }
            currentChar = nextChar();
        }
        if(stringBuf.length() > 0) {
            currentVector.addElement(stringBuf.toString());
        }
        String[][] actualReturnValue = new String[returnValue.size()][];
        for(int iter = 0 ; iter < actualReturnValue.length ; iter++) {
            Vector e = (Vector)returnValue.elementAt(iter);
            actualReturnValue[iter] = new String[e.size()];
            for(int i = 0 ; i < actualReturnValue[iter].length ; i++) {
                actualReturnValue[iter][i] = (String)e.elementAt(i);
            }
        }
        currentReader.close();
        currentReader = null;
        return actualReturnValue;
    }
}
