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

/**
 * The event based parser allows parsing without creating an object tree by
 * receiving callbacks to this class.
 *
 * @author Shai Almog
 */
public interface JSONParseCallback {
    /**
     * Indicates that the parser ran into an opening bracket event {
     */
    public void startBlock(String blockName);

    /**
     * Indicates that the parser ran into an ending bracket event }
     */
    public void endBlock(String blockName);

    /**
     * Indicates that the parser ran into an opening bracket event [
     */
    public void startArray(String arrayName);

    /**
     * Indicates that the parser ran into an ending bracket event ]
     */
    public void endArray(String arrayName);

    /**
     * Submits a token from the JSON data as a java string, this token is always a string value
     */
    public void stringToken(String tok);

    /**
     * Submits a numeric token from the JSON data
     * @param tok the token value
     */
    public void numericToken(double tok);

    /**
     * Submits a boolean token from the JSON data
     * @param tok the token value
     */
    public void booleanToken(boolean tok);
    
    /**
     * Submits a numeric token from the JSON data
     */
    public void longToken(long tok);

    /**
     * This method is called when a string key/value pair is detected within the json
     * it is essentially redundant when following string/numeric token.
     *
     * @param key the key
     * @param value a string value
     */
    public void keyValue(String key, String value);

    /**
     * This method indicates to the Parser if this Callback is still alive
     * 
     * @return true if the Callback is still interested to get the JSON parse
     * events from the JSONParser
     */
    public boolean isAlive();
}
