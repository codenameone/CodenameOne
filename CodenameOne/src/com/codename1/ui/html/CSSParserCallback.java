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

/**
 * A callback used to dispatch errors encountered while parsing CSS resources
 *
 * @author Ofir Leitner
 */
interface CSSParserCallback {

    /**
     * Error code denoting that an unsupported CSS attribute (by XHTML-MP 1.0 standards) was found in the HTML or external CSS files
     */
    public static int ERROR_CSS_ATTRIBUTE_NOT_SUPPORTED = 200;

    /**
     * Error code denoting that an invalid attribute value was found in the CSS
     */
    public static int ERROR_CSS_ATTIBUTE_VALUE_INVALID = 201;

    /**
     * Error code denoting that a CSS file referenced from the HTML or from another external CSS file was not found
     */
    public static int ERROR_CSS_NOT_FOUND = 202;

    /**
     * Error code denoting that a relative URL was referenced from a document with no base URL (A document that was loaded via setBody/setHTML/setDOM and not via setPage)
     * In this case the return value of parsingError is not considered - parsing continues and the resource at the URL (CSS file/image) is ignored
     */
    public static int ERROR_CSS_NO_BASE_URL = 203;

    /**
     *  Called when encountering an error while parsing the HTML document.
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
