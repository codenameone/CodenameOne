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

/**
 * A callback used to dispatch errors encountered while parsing XML resources
 *
 * @author Ofir Leitner
 */
public interface ParserCallback {

    /**
     * Error code denoting that an unsupported tag was found in the XML
     */
    public static int ERROR_TAG_NOT_SUPPORTED = 0;

    /**
     * Error code denoting that an unsupported attribute was found in the XML
     */
    public static int ERROR_ATTRIBUTE_NOT_SUPPORTED = 1;

    /**
     * Error code denoting that an  invalid attribute value was found in the XML
     */
    public static int ERROR_ATTIBUTE_VALUE_INVALID = 2;

    /**
     * Error code denoting that a tag was not closed properly in the XML
     */
    public static int ERROR_NO_CLOSE_TAG = 3;

    /**
     * Error code denoting that an  invalid character entity was found
     * A character entity is XML codes that start with an ampersand and end with semicolon and denote special/reserved chars
     */
    public static int ERROR_UNRECOGNIZED_CHAR_ENTITY = 4;

    /**
     * Error code denoting that a tag was not closed  prematurely
     */
    public static int ERROR_UNEXPECTED_TAG_CLOSING = 5;

    /**
     * Error code denoting that the parser bumped into an unexpected character
     */
    public static int ERROR_UNEXPECTED_CHARACTER = 6;

    /**
     * Error code denoting that the document had more than one root element
     */
    public static int ERROR_MULTIPLE_ROOTS = 7;

    /**
     * Error code denoting that the document had no root element at all (empty document or seriously malformed XML)
     */
    public static int ERROR_NO_ROOTS = 8;

     /**
     * Error code denoting that the encoding the page needed according to its charset (usually specified in the content-type response header) is unsupported in the device
     */
    public static int ERROR_ENCODING = 9;

    /**
     *  Called when encountering an error while parsing the XML document.
     *  When implementing this, the developer should return true if the error should be ignored and the document needs to be further parsed, or false to stop parsing and issue an error to the user
     *  Note that this method is always called NOT on the EDT thread.
     *
     * @param errorId The error ID, one of the ERROR_* constants
     * @param tag The tag in which the error occured (Can be null for non-tag related errors)
     * @param attribute The attribute in which the error occured (Can be null for non-attribute related errors)
     * @param value The value in which the error occured (Can be null for non-value related errors)
     * @param description A verbal description of the error
     * @return true to continue parsing, false to stop
     */
    public boolean parsingError(int errorId,String tag,String attribute,String value,String description);


}
