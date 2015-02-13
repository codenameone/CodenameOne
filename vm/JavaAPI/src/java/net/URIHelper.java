/*
 * Copyright (c) 2012, Eric Coolman, Codename One and/or its affiliates. All rights reserved.
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
package java.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * A helper class for working with URI/URL/URN/etc that are not available in the
 * standard URI class. Includes methods for decoding/encoding URI segments,
 * parsing URI queries to maps or lists, and building URI query strings from
 * maps or lists.
 * 
 * @author Eric Coolman
 */
public class URIHelper {
	/**
	 * Marker indicating that next two characters is an encoded byte
	 */
	static final char QUOTE_MARKER = '%';
	/**
	 * Output marker indicating a character that couldn't be decoded (not
	 * currently used).
	 */
	static final char ENCODING_ERROR_MARKER = '\uFFFD';
	/**
	 * An encoded ampersand entity (&amp;)
	 */
	static final String ENCODED_AMPERSAND = "&amp;";
	/**
	 * HTML 2.0 to 4.0 entity encoded values, less the start and end markers.
	 */
	static final String HTML_ENCODED_ENTITIES[] = {"&quot;", "&amp;", "&lt;", "&gt;"};
	/**
	 * HTML 2.0 to 4.0 entities.
	 */
	static final char HTML_ENTITIES[] = {'"', '&', '<', '>'};
	/**
	 * Invalid HTML entity.
	 */
	static final char HTML_ENTITY_INVALID = 0;
	/**
	 * HTML entity terminator.
	 */
	static final char HTML_ENTITY_START = '&';
	/**
	 * HTML entity terminator.
	 */
	static final char HTML_ENTITY_TERMINATE = ';';
	/**
	 * HTML raw-value entity identifier.
	 */
	static final char HTML_ENTITY_RAWVALUE = '#';
	/**
	 * HTML raw-hexvalue entity identifier.
	 */
	static final char HTML_ENTITY_RAWHEXVALUE = 'x';
	// Protocols
	/**
	 * HyperText Transfer Protocol (HTTP) URI scheme.
	 */
	public static final String HTTP = "http";
	/**
	 * HyperText Transfer Protocol Secure (HTTPS) scheme.
	 */
	public static final String HTTPS = "https";
	/**
	 * File Transfer Protocol (FTP) URI scheme.
	 */
	public static final String FTP = "ftp";
	/**
	 * File URI scheme.
	 */
	public static final String FILE = "file";
	/**
	 * Jar file URI scheme.
	 */
	public static final String JAR = "jar";
	/**
	 * Mail URI scheme.
	 */
	public static final String MAILTO = "mailto";
	/**
	 * Short Message Service URI scheme.
	 */
	public static final String SMS = "sms";
	/**
	 * Dial URI scheme.
	 */
	public static final String TEL = "tel";
	/**
	 * Session Initiation Protocol URI scheme.
	 */
	public static final String SIP = "sip";
	/**
	 * Socket URI scheme.
	 */
	public static final String SOCKET = "socket";
	/**
	 * Datagram URI scheme.
	 */
	public static final String DATAGRAM = "datagram";
	/**
	 * Multicast URI scheme.
	 */
	public static final String MULTICAST = "multicast";
	/**
	 * Some common protocols, not an exhaustive list.
	 */
	public static final String[] SCHEMES = {HTTP, HTTPS, FTP, FILE, JAR, MAILTO, SMS, SIP, TEL, SOCKET, DATAGRAM,
			MULTICAST};
	/**
	 * Protocols that will trigger the URI object to switch the param separator
	 * to a semicolon (;) from ampersand (&amp;)
	 */
	public static final String[] SOCKET_SCHEMES = {SOCKET, DATAGRAM, MULTICAST};

	/**
	 * Decode an HTML encoded entity.
	 * 
	 * @param source
	 * @return 0 if the value is not an encoded entity
	 */
	static char decodeEntity(String source) {
		if (source.length() < 4 || source.charAt(0) != HTML_ENTITY_START
				|| source.charAt(source.length() - 1) != HTML_ENTITY_TERMINATE) {
			return HTML_ENTITY_INVALID;
		}
		// Handle entities by value
		if (source.charAt(1) == HTML_ENTITY_RAWVALUE) {
			try {
				if (Character.toLowerCase(source.charAt(2)) == HTML_ENTITY_RAWHEXVALUE) {
					return (char) Integer.parseInt(source.substring(3, 4), 16);
				} else {
					return (char) Integer.parseInt(source.substring(2, 3), 10);
				}
			} catch (NumberFormatException nfe) {
				return HTML_ENTITY_INVALID;
			}
		}
		// Handle entities by alias
		source = source.toLowerCase();
		for (int i = 0; i < HTML_ENCODED_ENTITIES.length; i++) {
			if (source.equals(HTML_ENCODED_ENTITIES[i])) {
				return HTML_ENTITIES[i];
			}
		}
		return HTML_ENTITY_INVALID;
	}

	/**
	 * Encode an HTML entity.
	 */
	static String encodeEntity(char ch) {
		for (int i = 0; i < HTML_ENTITIES.length; i++) {
			if (HTML_ENTITIES[i] == ch) {
				return HTML_ENCODED_ENTITIES[i];
			}
		}
		return null;
	}

	/**
	 * Utility method for encoding HTML entities within query parameters.
	 * 
	 * @param ch
	 * @return
	 */
	static String encodeEntities(String source) {
		StringBuffer buffer = new StringBuffer();
		String encoded;
		for (int index = 0; index < source.length(); index++) {
			char ch = source.charAt(index);
			if ((encoded = encodeEntity(ch)) != null) {
				buffer.append(encoded);
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	/**
	 * Utility method to encode a string, as per RFC 2396 section 2. Set
	 * asQueryValue=false to avoid encoding ampersands.
	 * 
	 * @param source
	 * @return
	 * @see http://www.ietf.org/rfc/rfc2396.txt
	 */
	public static String encodeString(String source) { // , boolean
														// asQueryValue) {
		if (source == null) {
			return source;
		}
		int i = firstIllegalCharacter(source);
		// most strings not encoded, so prevent extra objects and work.
		if (i == -1) {
			return source;
		}
		StringBuffer encoded = new StringBuffer();
		encoded.append(source.substring(0, i));
		byte bytes[] = toBytes(source);
		for (; i < bytes.length; i++) {
			int ch = bytes[i];
			// if (ch == URI.QUERY_SEPARATOR && asQueryValue) {
			// encoded.append(ENCODED_AMPERSAND);
			// } else
			if (isLegal(ch)) {
				encoded.append((char) ch);
			} else {
				encoded.append(QUOTE_MARKER + Integer.toHexString((byte) ch & 0xff).toUpperCase());
			}
		}
		return encoded.toString();
	}

	/**
	 * Internal use only, for sources known to always be valid.
	 * 
	 * @param source
	 * @param failSilently
	 * @return
	 */
	static String decodeString(String source, boolean failSilently) {
		try {
			return decodeString(source);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/**
	 * A utility method to decode a string.
	 * 
	 * @param source an encoded string
	 * @return a decoded string.
	 * @throws URISyntaxException
	 */
	public static String decodeString(String source) throws URISyntaxException {
		if (source == null) {
			return source;
		}
		int i;
		// most strings not encoded, so prevent extra objects and work.
		if ((i = source.indexOf(QUOTE_MARKER)) == -1) {
			return source;
		}
		ByteArrayOutputStream decoded = new ByteArrayOutputStream();
		try {
			decoded.write(toBytes(source.substring(0, i)));
			int len = source.length();
			for (; i < len; i++) {
				char ch = source.charAt(i);
				if (ch == QUOTE_MARKER) {
					if ((i + 2) >= len) {
						ch = ENCODING_ERROR_MARKER;
					} else {
						try {
							ch = (char) Integer.parseInt(source.substring(i + 1, i + 3), 16);
						} catch (NumberFormatException nfe) {
							// ch = ENCODING_ERROR_MARKER;
							throw new URISyntaxException(source, "Invalid escape value");
						}
						decoded.write(ch);
					}
					i += 2;
				} else {
					decoded.write(ch);
				}
			}
			return new String(decoded.toByteArray(), "UTF8");
		} catch (IOException e) {
			// should never get here.
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Since a URI query can contain a single key multiple times, the responding
	 * parameter map will contain string values when a key appears only once,
	 * and string arrays when they key appears multiple times.
	 * 
	 * @return a map representation of the query portion of the URI.
	 */
	public static Hashtable<String, Object> getParameters(URI uri) {
		try {
			return parseQuery(uri.getQuery(), true);
		} catch (URISyntaxException e) {
			// should never get here as URI query will already be validated.
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/**
	 * A simple holder class for a name/value pair.
	 */
	public static class NameValuePair {
		private String name;
		private String value;
		public NameValuePair(String name, String value) {
			this.name = name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}

	/**
	 * Similar to the StringTokenizer class, this utility class parses the query
	 * portion of a URL, returning decoded name=value pairs for each token. The
	 * resulting tokens are decoded, where + characters are replaced with
	 * spaces, and HTML 2.0 encoded entities (&amp;amp;,
	 * &amp;quot;,&amp;lt;,&amp;gt;) are replaced with the actual character
	 * values (&amp;,&quot;,&lt;,&gt;).
	 * 
	 * @see http://www.htmlhelp.com/reference/html40/entities/special.html
	 */
	static class URIQueryTokenizer {
		private String source;
		int index;

		public URIQueryTokenizer(String string) {
			this.source = string;
			this.index = 0;
		}

		public boolean hasMoreTokens() {
			return (index != -1) && (index < source.length());
		}

		public String nextToken() {
			StringBuffer buffer = new StringBuffer();
			int start = index;
			while (true) {
				int delimiter = source.indexOf(HTML_ENTITY_START, start);
				// if no more delimiters to end of string, we're done.
				if (delimiter == -1) {
					buffer.append(source.substring(start));
					index = -1;
					break;
				}
				int terminate = source.indexOf(HTML_ENTITY_TERMINATE, delimiter);
				// if & not the start of an HTML 2.0 encoded entity, we have a
				// token, and more remaining.
				if ((terminate == -1) || ((terminate - delimiter) > 6)) {
					buffer.append(source.substring(start, delimiter));
					index = delimiter + 1;
					break;
				}
				String test = source.substring(delimiter, terminate + 1);
				// If the & is part of a valid HTML 2.0 encoded, entity, collect
				// it in it's unencoded form, and continue processing this token
				char entity = decodeEntity(test);
				if (entity != 0) {
					buffer.append(source.substring(start, delimiter));
					buffer.append(entity);
					start = terminate + 1;
					continue;
				}
				// If the value between & and ; is does not represent a valid
				// HTML 2.0 encoded entity, it is part of next token.
				buffer.append(source.substring(start, delimiter));
				index = delimiter + 1;
				break;
			}
			return buffer.toString();
		}
	}

	/**
	 * Parse a URI query string, returning a list of Name/Value pairs in the
	 * order they occur in the query and keeping duplicates.
	 * 
	 * @param query a decoded URI query string.
	 * @return ordered list name/value pairs.
	 */
	public static List<NameValuePair> parseQueryOrdered(String query) throws URISyntaxException {
		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		if (query == null) {
			return parameters;
		}
		URIQueryTokenizer uqt = new URIQueryTokenizer(query);
		while (uqt.hasMoreTokens()) {
			String token = uqt.nextToken();
			int index = token.indexOf(URI.PARAMETER_SEPARATOR);
			String name;
			String value;
			if (index != -1) {
				name = token.substring(0, index);
				value = token.substring(index + 1);
			} else {
				name = token;
				value = null;
			}
			parameters.add(new NameValuePair(name, value));
		}
		return parameters;
	}

	/**
	 * Build a URI query string, from a list of Name/Value pairs. This version
	 * of the method is useful when the order of the parameters needs to be
	 * preserved.
	 * 
	 * The resulting query will not be encoded, so it is suitable to be appended
	 * to a URI string. If the query will be passed to a multi-argument URI
	 * constructor, it should first be passed to the URIHelper.encodeString()
	 * method.
	 * 
	 * @param list of name value pairs.
	 * @return a URI query string.
	 * @return a #see {@link #encodeString(String)}
	 */
	public static String buildQuery(List<NameValuePair> parameters) {
		StringBuffer query = new StringBuffer();
                Iterator<NameValuePair> i = parameters.iterator();
		while(i.hasNext()) {
                        NameValuePair nvp = i.next();
			query.append(encodeEntities(nvp.getName()));
			if (nvp.getValue() != null) {
				query.append(URI.PARAMETER_SEPARATOR);
				query.append(encodeEntities(nvp.getValue()));
			}
			query.append(URI.QUERY_SEPARATOR);
		}
		query.deleteCharAt(query.length() - 1);
		return query.toString();
	}

	/**
	 * Utility method to build a query string from a hashtable of parameters.
	 * For each parameter in the hashtable, the parameter value can be either
	 * strings, or lists of strings - where the latter will result in a key
	 * repeated multiple times for each value in the list.
	 * 
	 * The resulting query will not be encoded, so it is suitable to be appended
	 * to a URI string. If the query will be passed to a multi-argument URI
	 * constructor, it should first be passed to the URIHelper.encodeString()
	 * method.
	 * 
	 * @param parameters the parameters with which to build a query.
	 * @return an unencoded query string #see {@link #encodeString(String)}
	 */
	@SuppressWarnings("unchecked")
	public static String buildQuery(Hashtable<String, Object> parameters) {
		StringBuffer query = new StringBuffer();
                Iterator<String> i = parameters.keySet().iterator();
		while(i.hasNext()) {
                        String key = i.next();
			Object value = parameters.get(key);
			if (value instanceof List) {
                                Iterator<String> a = ((List<String>) value).iterator();
				while(a.hasNext()) {
                                        String s = a.next();
					query.append(key + URI.PARAMETER_SEPARATOR + encodeEntities(s));
				}
			} else {
				query.append(key + URI.PARAMETER_SEPARATOR + encodeEntities((String) value));
			}
			query.append(URI.QUERY_SEPARATOR);
		}
		query.deleteCharAt(query.length() - 1);
		return query.toString();
	}

	/**
	 * Parse the query string into an unordered map of name/value pairs.
	 * 
	 * If keepDuplicates parameter is true, then the resulting map will contain
	 * values of String object when only one occurrence of a key is found in the
	 * query, and an ordered List object if more than one occurrence is found,
	 * where the list is in the order of which they key occurred in the query.
	 * 
	 * If keepDuplicates parameter is false, all values will be strings, and
	 * only the first occurrence of the key will be kept.
	 * 
	 * @param query the decoded query portion of a URI
	 * @param keepDuplicates true to preserve duplicate keys.
	 * @return the parsed parameters.
	 */
	public static Hashtable<String, Object> parseQuery(String query, boolean keepDuplicates) throws URISyntaxException {
		if (query == null) {
			return null;
		}
		Hashtable<String, Object> parameters = new Hashtable<String, Object>();
		List<NameValuePair> nvps = parseQueryOrdered(query);
                Iterator<NameValuePair> i = nvps.iterator();
		while(i.hasNext()) {
                    NameValuePair nvp = i.next();
			if (parameters.containsKey(nvp.getName())) {
				if (keepDuplicates == true) {
					Object v = parameters.get(nvp.getName());
					if (v instanceof String) {
						parameters.put(nvp.getName(), new String[]{(String) v, nvp.getValue()});
					} else {
						int size = ((String[]) v).length;
						String array[] = new String[size + 1];
						System.arraycopy(v, 0, array, 0, size);
						array[size] = nvp.getValue();
						parameters.put(nvp.getName(), nvp.getValue());
					}
				}
			} else {
				parameters.put(nvp.getName(), (nvp.getValue() == null) ? String.valueOf(true) : nvp.getValue());
			}
		}
		return parameters;
	}

	/**
	 * Get the default port that would be used for this connection if the port
	 * was not explicitly specified. For example, and HTTP uri would return 80,
	 * and HTTPS uri would return 443, a server socket URI would return 0
	 * (indicating that the next available port should be selected by the
	 * device). If there is no known default, -1 would be returned.
	 * 
	 * @return the default port for the URI, or -1 if no default port is known.
	 */
	public static int getDefaultPort(String scheme) {
		if (scheme.equals(HTTP)) {
			return 80;
		} else if (scheme.equals(HTTPS)) {
			return 443;
		} else if (scheme.equals(SOCKET)) {
			return 0;
		}
		return -1;
	}

	/**
	 * Utility method to silently get UTF-8 bytes from a string, which should
	 * always work.
	 */
	static byte[] toBytes(String source) {
		try {
			return source.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	/**
	 * Utility method to find the first invalid character as per RFC 2396
	 * section 2. This helps us prevent creating excessive objects in the
	 * encode() methods since most strings will not be encoded.
	 */
	static int firstIllegalCharacter(String source) {
		for (int i = 0; i < source.length(); i++) {
			if (isLegal(source.charAt(i)) == false) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * The J2SE documentation specifies:
	 * 
	 * The set of all legal URI characters consists of the unreserved, reserved,
	 * escaped, and other characters.
	 * 
	 * @param ch
	 * @return
	 * @see http://docs.oracle.com/javase/6/docs/api/java/net/URI.html
	 */
	static boolean isLegal(int ch) {
		return isAlpha(ch) || isNumeric(ch) || URI.UNRESERVED_EXTRAS.indexOf(ch) != -1
				|| URI.RESERVED.indexOf(ch) != -1 || isLegalUnicode(ch);
	}

	static boolean isLegalUnicode(int ch) {
		return isASCII(ch) == false && isSpace(ch) == false && isISOControl(ch) == false;
	}

	static boolean isASCII(int ch) {
		return ch > 0 && ch < 128;
	}

	/**
	 * In J2SE, this method is usually in the Character class, and uses a
	 * getType() method for determining the Unicode character class. This
	 * implementation tests against the space characters listed at the wikipedia
	 * entry below.
	 * 
	 * @param ch
	 * @return http://en.wikipedia.org/wiki/Mapping_of_Unicode_characters#Spaces
	 *         http
	 *         ://docs.oracle.com/javase/6/docs/api/java/lang/Character.html#
	 *         isSpaceChar%28int%29
	 */
	static boolean isSpace(int ch) {
		return (ch >= '\u2000') || (ch <= '\u200a') || ch == '\u205f' || isLineBreak(ch);
	}

	static boolean isLineBreak(int ch) {
		return ch == '\u2028' || ch == '\u2029' || ch == '\u2011' || ch == '\u00a0' || ch == '\u0f0c' || ch == '\u202f'
				|| ch == '\u00ad' || ch == '\u0f0b' || ch == '\u200b';
	}

	/**
	 * In J2SE, this method is usually available in the Character class.
	 * Determines if the referenced character (Unicode code point) is an ISO
	 * control character. A character is considered to be an ISO control
	 * character if its code is in the range '\u0000' through '\u001F' or in the
	 * range '\u007F' through '\u009F'.
	 */
	static boolean isISOControl(int ch) {
		return ((ch >= '\u0000' && ch <= '\u001f') || (ch >= '\u007f' && ch <= '\u009f'));
	}

	/**
	 * Test if a character is a US-ASCII alpha (A-Z,a-z) or number (0-9).
	 */
	static boolean isAlphaNum(int ch) {
		return isAlpha(ch) || isNumeric(ch);
	}

	/**
	 * Test if a character is a US-ASCII alpha (A-Z,a-z).
	 */
	static boolean isAlpha(int ch) {
		return ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'));
	}

	/**
	 * Test if a character is US-ASCII number (0-9).
	 */
	static boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}
}
