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
 */

package com.codename1.processing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import com.codename1.xml.Element;

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
 * <code>
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
 * </code>
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

	/**
	 * Internal method, do not use.
	 * 
	 * Create an evaluator object from a StructuredContent element.
	 * 
	 * @param content
	 *            a parsed dom
	 * @return Result a result evaluator object
	 * @throws IllegalArgumentException
	 *             thrown if null content is passed.
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
	 * @param content
	 *            a parsed dom
	 * @return Result a result evaluator object
	 * @throws IllegalArgumentException
	 *             thrown if null content is passed.
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
	 * @param content
	 *            structured content document as a string.
	 * @param format
	 *            an identifier for the type of content passed (ie. xml, json,
	 *            etc).
	 * @return Result a result evaluator object
	 * @throws IllegalArgumentException
	 *             thrown if null content or format is passed.
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
			return fromContent(new ByteArrayInputStream(content.getBytes()),
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
	 * <code>
	 * ConnectionRequest request = new ConnectionRequest() {
	 *    protected void readResponse(InputStream input) throws IOException {
	 *         Result evaluator = Result.fromContent(input, Result.JSON);
	 *         // ... evaluate the result here
	 *    }
	 *    // ... etc
	 * };
	 * </code>
	 * 
	 * 
	 * 
	 * @param content
	 *            structured content document as a string.
	 * @param format
	 *            an identifier for the type of content passed (ie. xml, json,
	 *            etc).
	 * @return Result a result evaluator object
	 * @throws IllegalArgumentException
	 *             thrown if null content or format is passed.
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
	 * Create an evaluator object from a parsed XML DOM.
	 * 
	 * @param content
	 *            a parsed XML DOM.
	 * @return Result a result evaluator object
	 * @throws IllegalArgumentException
	 *             thrown if null content is passed.
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
	 * @param content
	 *            JSON content input stream
	 * @return Result a result evaluator object
	 * @throws IOException
	 *             thrown if null content is passed.
	 */
	public static Result fromContent(Hashtable content)
			throws IllegalArgumentException {
		if (content == null) {
			throw new IllegalArgumentException("content cannot be null");
		}
		return fromContent(new HashtableContent(content));
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
	 * For example: <b>JSON</b> <code>
	 * {
	 * "settings" : [
	 * {
	 *     "toggle" : "true",
	 *     ... etc
	 * } 
	 * </code>
	 * 
	 * <b>Expression</b>
	 * 
	 * <code>
	 * boolean value = result.getAsBoolean("/settings[0]/toggle"); 
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public boolean getAsBoolean(final String path)
			throws IllegalArgumentException {
		String s = getAsString(path);
		if (s == null) {
			return false;
		}
		if ("true".equals(s)) {
			return true;
		} else if ("0".equals(s)) {
			return true;
		}
		return false;
	}

	/**
	 * Get an integer value from the requested path.
	 * 
	 * For example: <b>JSON</b>
	 * 
	 * <code>
	 * {
	 * "settings" 
	 * {
	 *     "connection"
	 *     {
	 *          "max_retries" : "20",
	 *          ... etc
	 *     }
	 * } 
	 * </code>
	 * 
	 * <b>Expression</b>
	 * 
	 * <code>
	 * int value = result.getAsInteger("//connection/max_retries"); 
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public int getAsInteger(final String path) throws IllegalArgumentException {
		String s = getAsString(path);
		if (s == null) {
			return 0;
		}
		return Integer.parseInt(s);
	}

	/**
	 * Get an long value from the requested path.
	 * 
	 * For example: <b>JSON</b>
	 * 
	 * <code>
	 * {
	 * "settings" 
	 * {
	 *     "connection"
	 *     {
	 *          "timeout_milliseconds" : "100000",
	 *          ... etc
	 *     }
	 * } 
	 * </code>
	 * 
	 * <b>Expression</b>
	 * 
	 * <code>
	 * long value = result.getAsLong("/settings/connection/timeout_milliseconds"); 
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
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
	 * <code>
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
	 * </code>
	 * 
	 * <b>Expression</b>
	 * 
	 * <code>
	 * double neBoundsLat = result.getAsDouble("//bounds/northeast/lat"); 
	 * double neBoundsLong = result.getAsDouble("//bounds/northeast/lng"); 
	 * double swBoundsLat = result.getAsDouble("//bounds/southwest/lat"); 
	 * double swBoundsLong = result.getAsDouble("//bounds/southwest/lng");
	 * 
	 * double memberDiscount = result.getAsDouble("pricing.members.members");
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
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
	 * <code>
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
	 * </code>
	 * 
	 * <b>Expression</b>
	 * 
	 * <code>
	 * String city = result.getAsDouble("//city"); 
	 * String province = result.getAsDouble("//location//region"); 
	 * String country = result.getAsDouble("profile//location//country"); 
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public String getAsString(final String path)
			throws IllegalArgumentException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		final StructuredContent obj = apply(root, tokens, 0);
		if (obj == null) {
			return null;
		}
		String tagName = (String) tokens.lastElement();
		if (tagName.startsWith("@")) {
			return obj.getAttribute(tagName.substring(1));
		} else {
			Vector result = obj.getChildren(tagName);
			if (result.size() == 0) {
				return null;
			}
			Object o = result.elementAt(0);
			if (o instanceof String) {
				return (String) o;
			}
			StructuredContent element = (StructuredContent) o;
			return element.getText(); // element.getChild(0).getText().trim();
		}
	}

	/**
	 * Get the size of an array at the requested path.
	 * 
	 * For example: <b>JSON</b> <code>
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
	 * </code>
	 * 
	 * <b>Expression</b>
	 * 
	 * <code>
	 *  int size = result.getSizeOfArray("/results[0]/address_components"); 
	 *  int size2 = result.getSizeOfArray("results"); 
	 *  int size3 = result.getSizeOfArray("/results[0]/address_components[2]/types"); 
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public int getSizeOfArray(final String path)
			throws IllegalArgumentException {
		final Vector array = getAsArray(path);
		return array == null ? 0 : array.size();
	}

	// TODO: add array accessors for other types, or parameterize by type

	/**
	 * Get an array of string values from the requested path.
	 * 
	 * For example: <b>JSON</b> <code>
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
	 * </code>
	 * 
	 * <b>Expression</b> 
	 * 
	 * <code>
	 * String types[] = result.getAsStringArray("/results[0]/address_components[2]/types");
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public String[] getAsStringArray(final String path)
			throws IllegalArgumentException {
		final Vector jarr = getAsArray(path);
		final String[] arr = new String[jarr == null ? 0 : jarr.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = (String) jarr.elementAt(i);
		}
		return arr;
	}

	/**
	 * Get an array of values from the requested path.
	 * 
	 * For example: <b>JSON</b> <code>
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
	 * </code>
	 * 
	 * <b>Expression</b> 
	 * 
	 * <code>
	 * String types[] = result.getAsStringArray("/results[0]/address_components[2]/types");
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public int[] getAsIntegerArray(final String path)
			throws IllegalArgumentException {
		final Vector jarr = getAsArray(path);
		final int[] arr = new int[jarr == null ? 0 : jarr.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = ((Integer) jarr.elementAt(i)).intValue();
		}
		return arr;
	}

	/**
	 * Get a vector of values from the requested path.
	 * 
	 * For example: <b>JSON</b> <code>
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
	 * </code>
	 * 
	 * <b>Expression</b> 
	 * 
	 * <code>
	 * Vector addressComponents = result.getAsVector("/results[0]/address_components"); 
	 * result = Result.fromContent(addressComponents); 
	 * String longName = result.getAsString("[1]/long_name"); 
	 * </code>
	 * 
	 * @param path
	 *            Path expression to evaluate
	 * @return the value at the requested path
	 * @throws IllegalArgumentException
	 *             on error traversing the document, ie. traversing into an
	 *             array without using subscripts.
	 */
	public Vector getAsArray(final String path) throws IllegalArgumentException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		if (tokens.isEmpty()) {
			return new Vector(); // return root;
		}
		final StructuredContent obj = apply(root, tokens, 0);
		return obj == null ? null : obj.getChildren((String) tokens
				.lastElement());
	}

	/**
	 * Internal worker utility method, traverses dom based on path
	 * tokens
	 * 
	 * @param start
	 * @param tokens
	 * @param firstToken
	 * @return
	 * @throws IllegalArgumentException
	 */
	private StructuredContent apply(final StructuredContent start,
			final Vector tokens, final int firstToken)
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
			final String tok1 = (String) tokens.elementAt(i);
			if (tok1.length() == 1
					&& ResultTokenizer.isDelimiter(tok1.charAt(0))) {
				if (root.equals(start)) {
					continue;
				}
				return apply(root, tokens, i + 1);
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
			final String tok2 = (String) tokens.elementAt(i + 1);
			final char t2 = tok2.charAt(0);
			switch (t2) {
			case SEPARATOR:
				Vector children;
				if (glob) {
					children = start.getDescendants(tok1);
				} else {
					children = start.getChildren(tok1);
				}
				if (children.size() > 0) {
					return apply((StructuredContent) children.elementAt(0),
							tokens, i + 2);
				}
				return null;
			case ARRAY_START:
				if (i + 2 >= nTokens) {
					throw new IllegalArgumentException(
							"Syntax error: array must be followed by a dimension: "
									+ tok1);
				}
				final String tok3 = (String) tokens.elementAt(i + 2);
				// TODO: here, allow us to select by attributes
				Evaluator evaluator = EvaluatorFactory.createEvaluator(tok3);

				if (i + 3 >= nTokens) {
					throw new IllegalArgumentException(
							"Syntax error: array dimension must be closed: "
									+ tok3);
				}
				final String tok4 = (String) tokens.elementAt(i + 3);
				if (tok4.length() != 1 && tok4.charAt(0) != ARRAY_END) {
					throw new IllegalArgumentException(
							"Syntax error: illegal close of array dimension: "
									+ tok4);
				}
				if (i + 4 >= nTokens) {
					throw new IllegalArgumentException(
							"Syntax error: array close must be followed by a separator: "
									+ tok1);
				}
				final String tok5 = (String) tokens.elementAt(i + 4);
				if (tok5.length() != 1 && tok5.charAt(0) != SEPARATOR) {
					throw new IllegalArgumentException(
							"Syntax error: illegal separator after array: "
									+ tok4);
				}
				i += 4;
				final Vector array;
				if (glob) {
					array = start.getDescendants(tok1);
				} else {
					array = start.getChildren(tok1);
				}

				Object selected = evaluator.evaluate(array);

				if (selected instanceof StructuredContent) {
					return apply((StructuredContent) selected, tokens, i + 1);
				} else {
					// TODO: need to fix complete this,
					// return apply((Vector)selected, tokens, i + 1);
					// For now only processing the first result:
					if (((Vector)selected).size() > 0) {
						return apply((StructuredContent)((Vector)selected).elementAt(0), tokens, i + 1);
					}
				}
			}
		}

		return start;
	}
}
