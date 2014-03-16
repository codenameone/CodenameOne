/*
 Copyright (c) 2007, Sun Microsystems, Inc.
 
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.
 * Neither the name of Sun Microsystems, Inc. nor the names of its
 contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 Derivative Revision History:
 
 2012-03 - derivative work from original Sun source, removed references
 to Sun's JSON parser, support for any structured document that implements
 a StructuredSource interface.  Added globbing and backtracking support 
 (backed by structured document impl), support for predicate expressions,
 nested expressions, and various XPath style features.

 */
package com.codename1.processing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;

import com.codename1.xml.Element;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * An evaluator for a very small expression language to extract primitive types
 * from structured information. This implementation is layered over the
 * com.codename1.io.JSONParser and com.codename1.xml.XMLParser classes. This
 * expression language allows applications to extract information from
 * structured data returned by web services with minimal effort.
 *
 * The expression language works a lot like a very small subset of XPath - the
 * expression syntax uses the / character for sub-elements and square brackets
 * for arrays.
 *
 * Some sample expressions:
 *
 * <pre>
 *  Simple expression, get the title of the first photo element.
 *
 *  /photos/photo[1]/title
 *
 *  Globally find the first name of a person with a last name of 'Coolman'.
 *
 *  //person[lastname='Coolman']/firstName
 *
 *  Get the latitude value of the second last result element.
 *
 *  /results[last()-1]/geometry/bounds/northeast/lat
 *
 *  Get the names of players from Germany
 *
 *  /tournament/player[@nationality='Germany']/name
 *
 *  Get the purchase order numbers of any order with a lineitem worth over $5
 *
 *  //order/lineitem[price > 5]/../@ponum
 * etc
 * </pre>
 *
 * @author Eric Coolman (2012-03 - derivative work from original Sun source).
 *
 */
public class Result {

    public static final String JSON = "json";
    public static final String XML = "xml";
    public static final char SEPARATOR = '/';
    public static final char ARRAY_START = '[';
    public static final char ARRAY_END = ']';
    private static final Object SELECT_GLOB = "//";
    private static final Object SELECT_PARENT = "..";

    private StructuredContent root;
    private Map namespaceAliases;

    /**
     * Internal method, do not use.
     *
     * Create an evaluator object from a StructuredContent element.
     *
     * @param content a parsed dom
     * @return Result a result evaluator object
     * @throws IllegalArgumentException thrown if null content is passed.
     */
    static Result fromContent(StructuredContent content)
            throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        return new Result(content);
    }

	// TODO: add a cache mapping subpaths to objects to improve performance
    /**
     * Internal method, do not use.
     *
     * Construct an evaluator object from a StructuredContent element.
     *
     * @param content a parsed dom
     * @return Result a result evaluator object
     * @throws IllegalArgumentException thrown if null content is passed.
     */
    private Result(final StructuredContent obj) throws IllegalArgumentException {
        if (obj == null) {
            throw new IllegalArgumentException("dom object cannot be null");
        }
        this.root = obj;
        if (root.getParent() != null) {
            root = root.getParent();
        }
    }

    /**
     * Create an evaluator object from a structured content document (XML, JSON,
     * etc) as a string.
     *
     * @param content structured content document as a string.
     * @param format an identifier for the type of content passed (ie. xml,
     * json, etc).
     * @return Result a result evaluator object
     * @throws IllegalArgumentException thrown if null content or format is
     * passed.
     */
    public static Result fromContent(String content, String format)
            throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        try {
            return fromContent(new InputStreamReader(new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8"),
                    format);
        } catch (IOException e) {
            // should never get here with a string
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Create an evaluator object from a structured content document (XML, JSON,
     * etc) input stream. Normally you would use this method within a content
     * request implementation, for example:
     *
     * <pre>
     * ConnectionRequest request = new ConnectionRequest() {
     * 	protected void readResponse(InputStream input) throws IOException {
     * 		Result evaluator = Result.fromContent(input, Result.JSON);
     * 		// ... evaluate the result here
     * 	}
     * 	// ... etc
     * };
     * </pre>
     *
     *
     *
     * @param content structured content document as a string.
     * @param format an identifier for the type of content passed (ie. xml,
     * json, etc).
     * @return Result a result evaluator object
     * @throws IllegalArgumentException thrown if null content or format is
     * passed.
     */
    public static Result fromContent(InputStream content, String format)
            throws IllegalArgumentException, IOException {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        StructuredContent sc;
        if ("xml".equals(format)) {
            sc = new XMLContent(content);
        } else if ("json".equals(format)) {
            sc = new JSONContent(content);
        } else {
            throw new IllegalArgumentException("Unrecognized format: " + format);
        }
        return fromContent(sc);
    }

    /**
     * Create an evaluator object from a structured content document (XML, JSON,
     * etc) input stream. Normally you would use this method within a content
     * request implementation, for example:
     *
     * <pre>
     * ConnectionRequest request = new ConnectionRequest() {
     * 	protected void readResponse(InputStream input) throws IOException {
     * 		Result evaluator = Result.fromContent(input, Result.JSON);
     * 		// ... evaluate the result here
     * 	}
     * 	// ... etc
     * };
     * </pre>
     *
     *
     *
     * @param content structured content document as a string.
     * @param format an identifier for the type of content passed (ie. xml,
     * json, etc).
     * @return Result a result evaluator object
     * @throws IllegalArgumentException thrown if null content or format is
     * passed.
     */
    public static Result fromContent(Reader content, String format)
            throws IllegalArgumentException, IOException {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        StructuredContent sc;
        if ("xml".equals(format)) {
            sc = new XMLContent(content);
        } else if ("json".equals(format)) {
            sc = new JSONContent(content);
        } else {
            throw new IllegalArgumentException("Unrecognized format: " + format);
        }
        return fromContent(sc);
    }

    /**
     * Create an evaluator object from a parsed XML DOM.
     *
     * @param content a parsed XML DOM.
     * @return Result a result evaluator object
     * @throws IllegalArgumentException thrown if null content is passed.
     */
    public static Result fromContent(Element content)
            throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        return fromContent(new XMLContent(content));
    }

    /**
     * Create an evaluator object from parsed JSON content DOM.
     *
     * @param content JSON content input stream
     * @return Result a result evaluator object
     */
    public static Result fromContent(Map content)
            throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        return fromContent(new MapContent(content));
    }

    /**
     * Returns a hashcode value for the object.
     *
     * @see Object#hashCode()
     */
    public int hashCode() {
        return root.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @see Object#equals(Object)
     */
    public boolean equals(final Object other) {
        return root.equals(other);
    }

    /**
     * Convert the object to a formatted structured content document. For
     * example, an XML or JSON document.
     *
     * @return a structured content document as a string
     */
    public String toString() {
        return root.toString();
    }

    /**
     * Get a boolean value from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     * "settings" : [
     * {
     *     "toggle" : "true",
     *     ... etc
     * }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * boolean value = result.getAsBoolean(&quot;/settings[0]/toggle&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public boolean getAsBoolean(final String path)
            throws IllegalArgumentException {
        String s = getAsString(path);
        if (s == null) {
            return false;
        }
        if ("true".equals(s)) {
            return true;
        } else if ("1".equals(s)) {
            return true;
        }
        return false;
    }

    /**
     * Get an integer value from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     * "settings"
     * {
     *     "connection"
     *     {
     *          "max_retries" : "20",
     *          ... etc
     *     }
     * }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * int value = result.getAsInteger(&quot;//connection/max_retries&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalException on error traversing the document, ie. traversing
     * into an array without using subscripts.
     */
    public int getAsInteger(final String path) throws IllegalArgumentException {
        String s = getAsString(path);
        if (s == null) {
            return 0;
        }
        return (int) Double.parseDouble(s);
    }

    /**
     * Get an long value from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     * "settings"
     * {
     *     "connection"
     *     {
     *          "timeout_milliseconds" : "100000",
     *          ... etc
     *     }
     * }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * long value = result.getAsLong(&quot;/settings/connection/timeout_milliseconds&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public long getAsLong(final String path) throws IllegalArgumentException {
        String s = getAsString(path);
        if (s == null) {
            return 0;
        }
        return Long.parseLong(s);
    }

    /**
     * Get a double value from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     *  "geometry" : {
     *    "bounds" : {
     *      "northeast" : {
     *        "lat" : 42.94959820,
     *        "lng" : -81.24873959999999
     *       },
     *       "southwest" : {
     *         "lat" : 42.94830,
     *         "lng" : -81.24901740000001
     *       }
     *    },
     *    "location" : {
     *      "lat" : 42.94886990,
     *      "lng" : -81.24876030
     *    },
     *    "location_type" : "RANGE_INTERPOLATED",
     *    "viewport" : {
     *      "northeast" : {
     *         "lat" : 42.95029808029150,
     *         "lng" : -81.24752951970851
     *      },
     *      "southwest" : {
     *         "lat" : 42.94760011970850,
     *          "lng" : -81.25022748029151
     *      }
     *   }
     *   // etc
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * double neBoundsLat = result.getAsDouble(&quot;//bounds/northeast/lat&quot;);
     * double neBoundsLong = result.getAsDouble(&quot;//bounds/northeast/lng&quot;);
     * double swBoundsLat = result.getAsDouble(&quot;//bounds/southwest/lat&quot;);
     * double swBoundsLong = result.getAsDouble(&quot;//bounds/southwest/lng&quot;);
     *
     * double memberDiscount = result.getAsDouble(&quot;pricing.members.members&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public double getAsDouble(final String path)
            throws IllegalArgumentException {
        String s = getAsString(path);
        if (s == null) {
            return 0;
        }
        return Double.parseDouble(s);
    }

    /**
     * Get a string value from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     * "profile"
     * {
     *     "location"
     *     {
     *          "city" : "London",
     *          "region" : "Ontario",
     *          "country" : "Canada",
     *          ... etc
     *     },
     * }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * String city = result.getAsDouble(&quot;//city&quot;);
     * String province = result.getAsDouble(&quot;//location//region&quot;);
     * String country = result.getAsDouble(&quot;profile//location//country&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public String getAsString(final String path)
            throws IllegalArgumentException {
        Object o = _internalGet(path);
        if (o instanceof StructuredContent) {
            return ((StructuredContent) o).getText();
        }
        return (String) o;
    }

    /**
     * Get the object value from the requested path. This method may return a
     * Map, List, String, or null.
     *
     * @param path
     * @return the object at the given path, or null.
     * @throws IllegalArgumentException
     */
    public Object get(final String path)
            throws IllegalArgumentException {
        Object o = _internalGet(path);
        if (o instanceof StructuredContent) {
            return ((StructuredContent) o).getNativeRoot();
        }
        return o;
    }

    /**
     * Internal function, do not use. This version does not convert the
     * structured content nodes, so not to be called by end user.
     *
     * @param path
     * @return a StructuredContent node, a String, or null
     * @throws IllegalArgumentException
     */
    private Object _internalGet(final String path) throws IllegalArgumentException {
        List v = _internalGetAsArray(path);
        if (v == null || v.size() == 0) {
            return null;
        }
        return v.get(0);
    }

    /**
     * Get the size of an array at the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     *    "results" : [
     *       {
     *         "address_components" : [
     *           {
     *             "long_name" : "921-989",
     *             "short_name" : "921-989",
     *             "types" : [ "street_number" ]
     *           },
     *           {
     *             "long_name" : "Country Club Crescent",
     *             "short_name" : "Country Club Crescent",
     *             "types" : [ "route" ]
     *           },
     *           {
     *             "long_name" : "Ontario",
     *             "short_name" : "ON",
     *             "types" : [ "administrative_area_level_1", "political" ]
     *           },
     *           ... etc
     *       }
     *  }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * int size = result.getSizeOfArray(&quot;/results[0]/address_components&quot;);
     * int size2 = result.getSizeOfArray(&quot;results&quot;);
     * int size3 = result.getSizeOfArray(&quot;/results[0]/address_components[2]/types&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public int getSizeOfArray(final String path)
            throws IllegalArgumentException {
        final List array = _internalGetAsArray(path);
        return array == null ? 0 : array.size();
    }

	// TODO: add array accessors for other types, or parameterize by type
    /**
     * Get an array of string values from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     *    "results" : [
     *       {
     *         "address_components" : [
     *           {
     *             "long_name" : "921-989",
     *             "short_name" : "921-989",
     *             "types" : [ "street_number" ]
     *           },
     *           {
     *             "long_name" : "Country Club Crescent",
     *             "short_name" : "Country Club Crescent",
     *             "types" : [ "route" ]
     *           },
     *           {
     *             "long_name" : "Ontario",
     *             "short_name" : "ON",
     *             "types" : [ "administrative_area_level_1", "political" ]
     *           },
     *           ... etc
     *       }
     *  }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * String types[] = result
     * 		.getAsStringArray(&quot;/results[0]/address_components[2]/types&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public String[] getAsStringArray(final String path)
            throws IllegalArgumentException {
        final List jarr = _internalGetAsArray(path);
        final String[] arr = new String[jarr == null ? 0 : jarr.size()];
        for (int i = 0; i < arr.length; i++) {
            StructuredContent element = (StructuredContent) jarr.get(i);
            arr[i] = (String) element.getText();
        }
        return arr;
    }

    /**
     * Get an array of values from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     *    "results" : [
     *       {
     *         "address_components" : [
     *           {
     *             "long_name" : "921-989",
     *             "short_name" : "921-989",
     *             "types" : [ "street_number" ]
     *           },
     *           {
     *             "long_name" : "Country Club Crescent",
     *             "short_name" : "Country Club Crescent",
     *             "types" : [ "route" ]
     *           },
     *           {
     *             "long_name" : "Ontario",
     *             "short_name" : "ON",
     *             "types" : [ "administrative_area_level_1", "political" ]
     *           },
     *           ... etc
     *       }
     *  }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * String types[] = result
     * 		.getAsStringArray(&quot;/results[0]/address_components[2]/types&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     * @throws NumberFormatException if the value at path can not be converted
     * to an integer.
     */
    public int[] getAsIntegerArray(final String path)
            throws IllegalArgumentException {
        final List jarr = _internalGetAsArray(path);
        final int[] arr = new int[jarr == null ? 0 : jarr.size()];
        for (int i = 0; i < arr.length; i++) {
            StructuredContent element = (StructuredContent) jarr.get(i);
            String s = (String) element.getText();
            arr[i] = Integer.parseInt(s);
        }
        return arr;
    }

    /**
     * Get an array of values from the requested path.
     * <pre>
     * String types[] = result
     * 		.getAsStringArray(&quot;/results[0]/address_components[2]/types&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     * @throws NumberFormatException if the value at path can not be converted
     * to an long.
     */
    public long[] getAsLongArray(final String path)
            throws IllegalArgumentException {
        final List jarr = _internalGetAsArray(path);
        final long[] arr = new long[jarr == null ? 0 : jarr.size()];
        for (int i = 0; i < arr.length; i++) {
            StructuredContent element = (StructuredContent) jarr.get(i);
            String s = (String) element.getText();
            arr[i] = Long.parseLong(s);
        }
        return arr;
    }

    /**
     * Get an array of values from the requested path.
     * <pre>
     * String types[] = result
     * 		.getAsStringArray(&quot;/results[0]/address_components[2]/types&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     * @throws NumberFormatException if the value at path can not be converted
     * to an double.
     */
    public double[] getAsDoubleArray(final String path)
            throws IllegalArgumentException {
        final List jarr = _internalGetAsArray(path);
        final double[] arr = new double[jarr == null ? 0 : jarr.size()];
        for (int i = 0; i < arr.length; i++) {
            StructuredContent element = (StructuredContent) jarr.get(i);
            String s = (String) element.getText();
            arr[i] = Double.parseDouble(s);
        }
        return arr;
    }

    /**
     * Get an array of values from the requested path.
     * <pre>
     * String types[] = result
     * 		.getAsStringArray(&quot;/results[0]/address_components[2]/types&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public boolean[] getAsBooleanArray(final String path)
            throws IllegalArgumentException {
        final List jarr = _internalGetAsArray(path);
        final boolean[] arr = new boolean[jarr == null ? 0 : jarr.size()];
        for (int i = 0; i < arr.length; i++) {
            StructuredContent element = (StructuredContent) jarr.get(i);
            String s = (String) element.getText();
            boolean b = false;
            if ("true".equals(s)) {
                b = true;
            } else if ("1".equals(s)) {
                b = true;
            }
            arr[i] = b;
        }
        return arr;
    }

    /**
     * Get a List of values from the requested path.
     *
     * For example: <b>JSON</b>
     *
     * <pre>
     * {
     *    "results" : [
     *       {
     *         "address_components" : [
     *           {
     *             "long_name" : "921-989",
     *             "short_name" : "921-989",
     *             "types" : [ "street_number" ]
     *           },
     *           {
     *             "long_name" : "Country Club Crescent",
     *             "short_name" : "Country Club Crescent",
     *             "types" : [ "route" ]
     *           },
     *           ... etc
     *       }
     *  }
     * </pre>
     *
     * <b>Expression</b>
     *
     * <pre>
     * List addressComponents = result.getAsList(&quot;/results[0]/address_components&quot;);
     * result = Result.fromContent(addressComponents);
     * String longName = result.getAsString(&quot;[1]/long_name&quot;);
     * </pre>
     *
     * @param path Path expression to evaluate
     * @return the value at the requested path
     * @throws IllegalArgumentException on error traversing the document, ie.
     * traversing into an array without using subscripts.
     */
    public List getAsArray(final String path) throws IllegalArgumentException {
        List array = _internalGetAsArray(path);
        for (int i = 0; i < array.size(); i++) {
            array.set(i, ((StructuredContent) array.get(i)).getNativeRoot());
        }
        return array;
    }

    /**
     * Internal function, do not use. This version does not convert the
     * structured content nodes in the array, so not to be called by end user.
     *
     * @param path
     * @return
     * @throws IllegalArgumentException
     */
    private List _internalGetAsArray(final String path)
            throws IllegalArgumentException {
        final List tokens = new ResultTokenizer(path).tokenize(namespaceAliases);
        if (tokens.isEmpty()) {
            return tokens;
        }
        final StructuredContent obj = apply(root, tokens, 0);
        if (obj == null) {
            return new Vector();
        }
        String key = (String) tokens.get(tokens.size() - 1);
        // if the last element of expression is a glob, handle it here
        if ((tokens.size() > 1)
                && SELECT_GLOB.equals(tokens.get(tokens.size() - 2))) {
            return obj.getDescendants(key);
        }
        // if the last element of expression is an attribute, handle it here
        if (key.startsWith("@")) {
            key = key.substring(1);
            String v = obj.getAttribute(key);
            List array = new Vector();
            if (v != null) {
				// this will allow caller to get parent of an attribute if
                // needed
                array.add(new MapContent(v, obj));
            }
            return array;
        } else if (key.charAt(0) == Result.ARRAY_END && tokens.size() >= 4) {
			// Handle path ending with a predicate instead of a key
            //key = (String)tokens.get(tokens.size() - 4);
            List array = new Vector();
            //array.add(new MapContent(key, obj));
            array.add(obj);
            return array;
        }
        // otherwise, last element of expression selects a child node.
        return obj.getChildren(key);
    }

    /**
     * Internal worker utility method, traverses dom based on path tokens
     *
     * @param start
     * @param tokens
     * @param firstToken
     * @return
     * @throws IllegalArgumentException
     */
    private StructuredContent apply(final StructuredContent start,
            final List tokens, final int firstToken)
            throws IllegalArgumentException {

        if (start == null) {
            return null;
        }

        final int nTokens = tokens.size();
        if (firstToken >= nTokens) {
            return start;
        }
        boolean glob = false;
        for (int i = firstToken; i < nTokens; i++) {
            final String tok1 = (String) tokens.get(i);
            if (tok1.length() == 1
                    && ResultTokenizer.isDelimiter(tok1.charAt(0))) {
                continue;
            }
            if (tok1.length() == 2) {
                if (tok1.equals(SELECT_GLOB)) {
                    glob = true;
                    continue;
                } else if (tok1.equals(SELECT_PARENT)) {
                    return apply(start.getParent(), tokens, i + 1);
                }
            }

            if (i + 1 >= nTokens) {
                return start;
            }
            final String tok2 = (String) tokens.get(i + 1);
            final char t2 = tok2.charAt(0);
            switch (t2) {
                case SEPARATOR:
                    List children;
                    if (glob) {
                        children = start.getDescendants(tok1);
                    } else {
                        children = start.getChildren(tok1);
                    }
                    if (children.size() > 0) {
                        return apply(new SubContent(children, start),
                                tokens, i + 2);
                    }
                    return null;
                case ARRAY_START:
                    if (i + 2 >= nTokens) {
                        throw new IllegalArgumentException(
                                "Syntax error: array must be followed by a dimension: "
                                + tok1);
                    }
                    final String tok3 = (String) tokens.get(i + 2);

                    Evaluator evaluator = EvaluatorFactory
                            .createEvaluator(tok3);

                    if (i + 3 >= nTokens) {
                        throw new IllegalArgumentException(
                                "Syntax error: array dimension must be closed: "
                                + tok3);
                    }
                    final String tok4 = (String) tokens.get(i + 3);
                    if (tok4.length() != 1 && tok4.charAt(0) != ARRAY_END) {
                        throw new IllegalArgumentException(
                                "Syntax error: illegal close of array dimension: "
                                + tok4);
                    }
                    i += 4;
                    if (i < nTokens) {
                        final String tok5 = (String) tokens.get(i);
                        if (tok5.length() != 1 && tok5.charAt(0) != SEPARATOR) {
                            throw new IllegalArgumentException(
                                    "Syntax error: illegal separator after array: "
                                    + tok4);
                        }
                    }
                    final List array;
                    if (glob) {
                        array = start.getDescendants(tok1);
                    } else {
                        array = start.getChildren(tok1);
                    }

                    Object selected = evaluator.evaluate(array);

                    if (selected instanceof StructuredContent) {
                        return apply((StructuredContent) selected, tokens,
                                i + 1);
                    } else {
                        if (selected != null && ((List) selected).size() > 0) {
                            List v = new Vector();
                            for (Object o : (List) selected) {
                                StructuredContent sc = apply((StructuredContent) o, tokens, i + 1);
                                v.add(sc);
                            }
                            return new SubContent(v, start);
                        }
                    }
            }
        }

        return start;
    }

    public void mapNamespaceAlias(String namespaceURI, String alias) {
        Map attributes = root.getChild(0).getAttributes();
        if (attributes == null) {
            return;
        }
        Iterator e = attributes.keySet().iterator();
        while (e.hasNext()) {
            String key = (String) e.next();
            if (key.startsWith("xmlns:") == false) {
                continue;
            }
            if (namespaceURI.equals(attributes.get(key))) {
                if (namespaceAliases == null) {
                    namespaceAliases = new Hashtable();
                }
                namespaceAliases.put(alias, key.substring(6));
                break;
            }
        }
    }
}
