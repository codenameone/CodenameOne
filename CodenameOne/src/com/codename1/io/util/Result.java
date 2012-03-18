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

package com.codename1.io.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

import com.codename1.io.JSONParser;

/**
 * An evaluator for a very small expression language to extract primitive 
 * types from structured information. This implementation is layered over 
 * the com.codename1.io.JSONParser class.  This expression language 
 * allows applications to extract information from structured data 
 * returned by web services with minimal effort.
 * 
 * The expression language works a lot like a very small subset of XPath 
 * - the expression syntax uses the dot character for sub-elements and 
 * square brackets for arrays. 
 * 
 * Some sample expressions:
 * 
 * <code>
 * photos.photo[1].title
 * 
 * [0].location
 * 
 * results[1].geometry.bounds.northeast.lat
 * 
 * results[0].formatted_address
 * 
 * etc
 * </code> 

 */
public class Result {
	public static final char SEPARATOR = '.';
	public static final char ARRAY_START = '[';
	public static final char ARRAY_END = ']';

	private final Hashtable json;
	private final Vector array;
	private final boolean isArray;

	/**
	 * Create an evaluator object from JSON content as a string. 
	 * 
	 * @param content JSON content as a string
	 * @return Result a result evaluator object
	 * @throws ResultException thrown if null content is passed. 
	 */
	public static Result fromContent(String content) throws IOException {
        if (content == null) {
        	throw new IllegalArgumentException("content cannot be null");
        }
			return fromContent(new ByteArrayInputStream(content.getBytes()));
	}

	/**
	 * Create an evaluator object from JSON content input stream. Normally you would use
	 * this method within a content request implementation, for example:
	 * 
	 * <code>
	 * ConnectionRequest request = new ConnectionRequest() {
     *    protected void readResponse(InputStream input) throws IOException {
     *         Result evaluator = Result.fromContent(input);
     *         // ... evaluate the result here
     *    }
     *    // ... etc
     * };
	 * </code>
	 *
	 * 
	 * @param content JSON content input stream
	 * @return Result a result evaluator object
	 * @throws ResultException thrown if null content is passed. 
	 */
	public static Result fromContent(InputStream content) throws IOException {
        if (content == null) {
        	throw new IllegalArgumentException("content cannot be null");
        }
        JSONParser parser = new JSONParser();
        return fromContent(parser.parse(new InputStreamReader(content)));
	}

	/**
	 * Create an evaluator object from the result of JSONParser. For example:
	 * 
	 * <code>
	 * ConnectionRequest request = new ConnectionRequest() {
     *    protected void readResponse(InputStream input) throws IOException {
     *      JSONParser parser = new JSONParser();
     *      Hashtable parsed = parser.parse(new InputStreamReader(input));
     *      Result evaluator = Result.fromContent(parsed);
     *      // ... evaluate the result here
     *    }
     *    // ... etc
     * };
	 * </code>
	 * 
	 * @param content JSON content as returned from JSONParser
	 * @return Result a result evaluator object
	 * @throws ResultException thrown if null content is passed. 
	 * 
	 * @param content
	 * @return
	 */
	public static Result fromContent(Hashtable content)  {
        if (content == null) {
        	throw new IllegalArgumentException("content cannot be null");
        }
        return new Result(content);
    }	
	
	/**
	 * Create an evaluator object from a vector. This is useful for creating
	 * evaluators against fragments of a JSON result.
	 * 
	 * For example:
	 * 
	 * <code>
     *      JSONParser parser = new JSONParser();
     *      Hashtable parsed = parser.parse(new InputStreamReader(input));
     *      Vector array = (Vector)parsed.get("results");
     *      Result evaluator = Result.fromContent(array);
     *      // ... evaluate the result here
	 * </code>
	 * 
	 * @param content JSON content as a Vector object
	 * @return Result a result evaluator object
	 * @throws ResultException thrown if null content is passed. 
	 * 
	 * @param content
	 * @return
	 */
	public static Result fromContent(Vector content) {
        if (content == null) {
        	throw new IllegalArgumentException("content cannot be null");
        }
        return new Result(content);
    }	

	// TODO: add a cache mapping subpaths to objects to improve performance

	/**
	 * Constructor
	 * 
	 * @param obj JSON content as a Hashtable object, as returned by JSONParser
	 */
	private Result(final Hashtable obj) {
		if (obj == null) {
			throw new IllegalArgumentException("json object cannot be null");
		}
		isArray = false;
		this.json = obj;
		this.array = null;
	}

	/**
	 * Constructor
	 * 
	 * @param obj JSON content as a Vector object
	 */
	private Result(final Vector  obj) {
		if (obj == null) {
			throw new IllegalArgumentException("json object cannot be null");
		}
		isArray = true;
		this.json = null;
		this.array = obj;
	}

	/**
	 * Returns a hashcode value for the object.
	 * 
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return isArray ? array.hashCode() : json.hashCode();
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one. 
	 * 
	 * @see Object#equals(Object)
	 */
	public boolean equals(final Object other) {
		return isArray ? array.equals(other) : json.equals(other);
	}

	/**
	 * Convert the JSON object back to a JSON string.
	 * 
	 * @return the JSON object as a string
	 */
	public String toString() {
		try {
			if (isArray) {
				return ResultUtil.prettyPrint(array);
			} else {
				return ResultUtil.prettyPrint(json);
			}
		} catch (Exception jx) {
			return json.toString();
		}
	}

	/**
	 * Get a boolean value from the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
	 * <code>
	 * {
     * "settings" : [
     * {
     *     "toggle" : "true",
     *     ... etc
     * } 
     * </code>
     * 
     * <b>Expression</b>
	 * boolean value = result.getAsBoolean("settings[0].toggle"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public boolean getAsBoolean(final String path) throws IOException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		final Hashtable obj = isArray ? apply(array, tokens, 0) : apply(json,
				tokens, 0);
		return obj == null ? false : ResultUtil.optBoolean(obj,
				(String) tokens.lastElement());
	}


	/**
	 * Get an integer value from the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
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
	 * int value = result.getAsInteger("settings.connection.max_retries"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public int getAsInteger(final String path) throws IOException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		final Hashtable obj = isArray ? apply(array, tokens, 0) : apply(json,
				tokens, 0);
		return obj == null ? 0 : ResultUtil.optInt(obj, (String) tokens.lastElement());
	}

	/**
	 * Get an long value from the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
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
	 * long value = result.getAsLong("settings.connection.timeout_milliseconds"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public long getAsLong(final String path) throws IOException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		final Hashtable obj = isArray ? apply(array, tokens, 0) : apply(json,
				tokens, 0);
		return obj == null ? 0 : ResultUtil.optLong(obj, (String) tokens.lastElement());
	}

	/**
	 * Get a double value from the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
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
	 * double neBoundsLat = result.getAsDouble("geometry.bounds.northeast.lat"); 
	 * double neBoundsLong = result.getAsDouble("geometry.bounds.northeast.lng"); 
	 * double swBoundsLat = result.getAsDouble("geometry.bounds.southwest.lat"); 
	 * double swBoundsLong = result.getAsDouble("geometry.bounds.southwest.lng"); 
	 * double memberDiscount = result.getAsDouble("pricing.members.members"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public double getAsDouble(final String path) throws IOException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		final Hashtable obj = isArray ? apply(array, tokens, 0) : apply(json,
				tokens, 0);
		return obj == null ? 0 : ResultUtil.optDouble(obj, (String) tokens.lastElement());
	}

	/**
	 * Get a string value from the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
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
	 * String city = result.getAsDouble("profile.location.city"); 
	 * String province = result.getAsDouble("profile.location.region"); 
	 * String country = result.getAsDouble("profile.location.country"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public String getAsString(final String path) throws IOException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		final Hashtable obj = isArray ? apply(array, tokens, 0) : apply(json,
				tokens, 0);
		return obj == null ? null : ResultUtil.optString(obj,
				(String) tokens.lastElement());
	}

	/**
	 * Get the size of an array at the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
	 * <code>
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
	 * int size = result.getSizeOfArray("results[0].address_components"); 
	 * int size2 = result.getSizeOfArray("results"); 
	 * int size3 = result.getSizeOfArray("results[0].address_components[2].types"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public int getSizeOfArray(final String path) throws IOException {
		final Vector array = getAsArray(path);
		return array == null ? 0 : array.size();
	}

	// TODO: add array accessors for other types, or parameterize by type

	/**
	 * Get an array of string values from the requested path.
	 * 
	 * For example:
	 * <b>JSON</b> 
	 * <code>
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
	 * String types[] = result.getAsStringArray("results[0].address_components[2].types"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public String[] getAsStringArray(final String path) throws IOException {
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
	 * For example:
	 * <b>JSON</b> 
	 * <code>
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
	 * String types[] = result.getAsStringArray("results[0].address_components[2].types"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public int[] getAsIntegerArray(final String path) throws IOException {
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
	 * For example:
	 * <b>JSON</b> 
	 * <code>
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
	 * Vector addressComponents = result.getAsVector("results[0].address_components"); 
	 * result = Result.fromContent(addressComponents);
	 * String longName = result.getAsString("[1].long_name"); 
	 * </code>
	 * 
	 * @param path Path expression to evaluate
	 * @return the value at the requested path
	 * @throws ResultException on error traversing the document, ie. traversing into an array without using subscripts.
	 */
	public Vector getAsArray(final String path) throws IOException {
		final Vector tokens = new ResultTokenizer(path).tokenize();
		if (isArray && tokens.isEmpty()) {
			return array;
		}
		final Hashtable obj = isArray ? apply(array, tokens, 0) : apply(json,
				tokens, 0);
		return obj == null ? null : ResultUtil.optJSONArray(obj,
				(String) tokens.lastElement());
	}

	/**
	 * Internal worker utility method, traverses JSON object based on path tokens
	 * 
	 * @param start
	 * @param tokens
	 * @param firstToken
	 * @return
	 * @throws ResultException
	 */
	private Hashtable apply(final Vector start, final Vector tokens,
			final int firstToken) throws IOException {

		if (start == null) {
			return null;
		}

		final int nTokens = tokens.size();
		for (int i = firstToken; i < nTokens; i++) {
			final String tok1 = (String) tokens.elementAt(i);
			final char t1 = tok1.charAt(0);
			switch (t1) {
			case SEPARATOR:
				throw new IOException(
						"Syntax error: must start with an array: " + tok1);

			case ARRAY_START:
				if (i + 1 >= nTokens) {
					throw new IOException(
							"Syntax error: array must be followed by a dimension: "
									+ tok1);
				}
				final String tok2 = (String) tokens.elementAt(i + 1);
				int dim = 0;
				try {
					dim = Integer.parseInt(tok2);
				} catch (NumberFormatException nx) {
					throw new IOException(
							"Syntax error: illegal array dimension: " + tok2);
				}
				if (i + 2 >= nTokens) {
					throw new IOException(
							"Syntax error: array dimension must be closed: "
									+ tok2);
				}
				final String tok3 = (String) tokens.elementAt(i + 2);
				if (tok3.length() != 1 && tok3.charAt(0) != ARRAY_END) {
					throw new IOException(
							"Syntax error: illegal close of array dimension: "
									+ tok3);
				}
				if (i + 3 >= nTokens) {
					throw new IOException(
							"Syntax error: array close must be followed by a separator or array open: "
									+ tok3);
				}
				final String tok4 = (String) tokens.elementAt(i + 3);
				if (tok4.length() != 1 && tok4.charAt(0) != SEPARATOR
						&& tok4.charAt(0) != ARRAY_START) {
					throw new IOException(
							"Syntax error: illegal separator after array: "
									+ tok4);
				}
				i += 4;
				if (tok4.charAt(0) == SEPARATOR) {
					if (dim >= 0 && dim < start.size()) {
						return apply((Hashtable) start.elementAt(dim), tokens, i);
					}
				} else if (tok4.charAt(0) == ARRAY_START) {
					if (dim >= 0 && dim < start.size()) {
						return apply((Vector) start.elementAt(dim), tokens, i);
					}
				}
				throw new IOException(
						"Syntax error: illegal token after array: " + tok4);

			default:
				throw new IOException("Syntax error: unknown delimiter: "
						+ tok1);
			}
		}

		return null;
	}

	/**
	 * Internal worker utility method, traverses JSON object based on path tokens
	 * 
	 * @param start
	 * @param tokens
	 * @param firstToken
	 * @return
	 * @throws ResultException
	 */
	private Hashtable apply(final Hashtable start, final Vector tokens,
			final int firstToken) throws IOException {

		if (start == null) {
			return null;
		}

		final int nTokens = tokens.size();
		if (firstToken >= nTokens) {
			return start;
		}

		for (int i = firstToken; i < nTokens; i++) {
			final String tok1 = (String) tokens.elementAt(i);
			if (tok1.length() == 1
					&& ResultTokenizer.isDelimiter(tok1.charAt(0))) {
				throw new IOException(
						"Syntax error: path cannot start with a delimiter: "
								+ tok1);
			}

			if (i + 1 >= nTokens) {
				return start;
			}
			final String tok2 = (String) tokens.elementAt(i + 1);
			final char t2 = tok2.charAt(0);
			switch (t2) {
			case SEPARATOR:
				return apply(ResultUtil.optJSONObject(start, tok1), tokens, i + 2);

			case ARRAY_START:
				if (i + 2 >= nTokens) {
					throw new IOException(
							"Syntax error: array must be followed by a dimension: "
									+ tok1);
				}
				final String tok3 = (String) tokens.elementAt(i + 2);
				int dim = 0;
				try {
					dim = Integer.parseInt(tok3);
				} catch (NumberFormatException nx) {
					throw new IOException(
							"Syntax error: illegal array dimension: " + tok3);
				}
				if (i + 3 >= nTokens) {
					throw new IOException(
							"Syntax error: array dimension must be closed: "
									+ tok3);
				}
				final String tok4 = (String) tokens.elementAt(i + 3);
				if (tok4.length() != 1 && tok4.charAt(0) != ARRAY_END) {
					throw new IOException(
							"Syntax error: illegal close of array dimension: "
									+ tok4);
				}
				if (i + 4 >= nTokens) {
					throw new IOException(
							"Syntax error: array close must be followed by a separator: "
									+ tok1);
				}
				final String tok5 = (String) tokens.elementAt(i + 4);
				if (tok5.length() != 1 && tok5.charAt(0) != SEPARATOR) {
					throw new IOException(
							"Syntax error: illegal separator after array: "
									+ tok4);
				}
				i += 4;
				final Vector array = ResultUtil.optJSONArray(start, tok1);
				if ((array != null) && (dim >= 0) && (dim < array.size())) {
					return array == null ? null : apply((Hashtable) array.elementAt(dim),
							tokens, i + 1);
				} else {
					return null;
				}
			}
		}

		return start;
	}
}
